package za.co.shoplink.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import za.co.shoplink.common.events.OrderCreatedEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;
import za.co.shoplink.inventory.InventoryApplication;
import za.co.shoplink.inventory.service.InventoryService;
import za.co.shoplink.order.OrderApplication;
import za.co.shoplink.payment.PaymentApplication;
import za.co.shoplink.payment.PaymentService;
import za.co.shoplink.product.ProductApplication;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class ShopLinkIntegrationTest {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private BrokerService broker;
    private ConfigurableApplicationContext products, inventory, payments, orders;
    private String brokerUrl;
    private int orderPort;

    @BeforeEach void startServices() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.setBrokerName("test-" + java.util.UUID.randomUUID());
        var connector = broker.addConnector("tcp://127.0.0.1:0");
        broker.start();
        broker.waitUntilStarted();
        brokerUrl = connector.getPublishableConnectString() + "?connectionTimeout=1000&soTimeout=2000";
        products = start(ProductApplication.class);
        inventory = start(InventoryApplication.class);
        startPayments(false);
        orders = start(OrderApplication.class,
                "--product.service.base-url=http://127.0.0.1:" + port(products),
                "--inventory.service.base-url=http://127.0.0.1:" + port(inventory));
        orderPort = port(orders);
    }

    private ConfigurableApplicationContext start(Class<?> app, String... args) {
        var options = new java.util.ArrayList<String>(java.util.List.of(
                "--server.port=0", "--spring.main.banner-mode=off", "--spring.activemq.broker-url=" + brokerUrl,
                "--spring.activemq.user=", "--spring.activemq.password=", "--logging.level.root=WARN"));
        options.addAll(java.util.List.of(args));
        return new SpringApplicationBuilder(app).run(options.toArray(String[]::new));
    }
    private void startPayments(boolean decline) {
        payments = start(PaymentApplication.class, "--payment.simulation.decline=" + decline);
    }
    private int port(ConfigurableApplicationContext context) {
        return ((WebServerApplicationContext) context).getWebServer().getPort();
    }
    @AfterEach void stopServices() throws Exception {
        for (var context : new ConfigurableApplicationContext[]{orders, payments, inventory, products}) {
            if (context != null) context.close();
        }
        if (broker != null) { broker.stop(); broker.waitUntilStopped(); }
    }
    private JsonNode create() throws Exception {
        var response = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + orderPort + "/orders"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"customerId\":\"C1\",\"items\":[{\"productId\":\"P1002\",\"quantity\":3}]}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode(), response.body());
        JsonNode result = mapper.readTree(response.body());
        assertEquals("PAYMENT_PENDING", result.path("status").asText());
        return result;
    }
    private JsonNode get(String id) throws Exception {
        var response = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + orderPort + "/orders/" + id))
                .timeout(Duration.ofSeconds(3)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return mapper.readTree(response.body());
    }
    private JsonNode await(String id, String status) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        JsonNode result;
        do {
            result = get(id);
            if (status.equals(result.path("status").asText())) return result;
            Thread.sleep(50);
        } while (System.nanoTime() < deadline);
        fail("Expected " + status + " but got " + result);
        return result;
    }
    private int stock() { return inventory.getBean(InventoryService.class).available("P1002"); }

    @Test void approvesPaymentThroughRealBrokerAndPreservesPricing() throws Exception {
        JsonNode created = create();
        String id = created.path("id").asText();
        JsonNode paid = await(id, "PAID");
        assertEquals("Canvas Tote Bag", paid.path("items").get(0).path("productName").asText());
        var payment = payments.getBean(PaymentService.class).find(id);
        assertEquals(0, new BigDecimal("449.97").compareTo(payment.amount()));
        assertEquals("ZAR", payment.currency().getCurrencyCode());
        assertEquals(47, stock());
    }
    @Test void declinedPaymentReleasesStock() throws Exception {
        payments.close(); startPayments(true);
        String id = create().path("id").asText();
        await(id, "PAYMENT_FAILED");
        assertEquals(50, stock());
    }
    @Test void queuesOrderWhilePaymentServiceIsOffline() throws Exception {
        payments.close();
        String id = create().path("id").asText();
        assertEquals("PAYMENT_PENDING", get(id).path("status").asText());
        assertEquals(47, stock());
        startPayments(false);
        await(id, "PAID");
    }
    @Test void duplicateMessagesDoNotRepeatPaymentOrRelease() throws Exception {
        payments.close(); startPayments(true);
        String id = create().path("id").asText();
        await(id, "PAYMENT_FAILED");
        var payment = payments.getBean(PaymentService.class).find(id);
        var template = orders.getBean(JmsTemplate.class);
        var event = new OrderCreatedEvent(id, new BigDecimal("449.97"), Currency.getInstance("ZAR"));
        template.convertAndSend(MessagingConfiguration.ORDER_CREATED, event);
        // A second real order establishes that the preceding duplicate and reply were consumed.
        String marker = create().path("id").asText();
        await(marker, "PAYMENT_FAILED");
        assertEquals(payment, payments.getBean(PaymentService.class).find(id));
        assertEquals(50, stock());
        assertEquals("PAYMENT_FAILED", get(id).path("status").asText());
    }
    @Test void brokerOutageLeavesRetrievablePendingOrder() throws Exception {
        broker.stop(); broker.waitUntilStopped();
        String id = create().path("id").asText();
        assertEquals("PAYMENT_PENDING", get(id).path("status").asText());
        assertEquals(47, stock());
    }
}
