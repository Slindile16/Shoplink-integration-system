package za.co.shoplink.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import za.co.shoplink.order.OrderApplication;
import za.co.shoplink.inventory.InventoryApplication;
import za.co.shoplink.product.ProductApplication;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShopLinkIntegrationTest {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ConfigurableApplicationContext productContext;
    private ConfigurableApplicationContext orderContext;
    private ConfigurableApplicationContext inventoryContext;
    private int orderPort;
    private ConfigurableApplicationContext paymentContext;

    @BeforeEach
    void startServices() {
        productContext = new SpringApplicationBuilder(ProductApplication.class)
                .run("--server.port=0", "--spring.main.banner-mode=off");

        int productPort = ((WebServerApplicationContext) productContext).getWebServer().getPort();
        inventoryContext = new SpringApplicationBuilder(InventoryApplication.class)
                .run("--server.port=0", "--spring.main.banner-mode=off");
        int inventoryPort = ((WebServerApplicationContext) inventoryContext).getWebServer().getPort();
        paymentContext = new SpringApplicationBuilder(za.co.shoplink.payment.PaymentApplication.class)
                .run("--server.port=0", "--spring.main.banner-mode=off");
        int paymentPort = ((WebServerApplicationContext) paymentContext).getWebServer().getPort();
        orderContext = new SpringApplicationBuilder(OrderApplication.class)
                .run("--server.port=0",
                        "--spring.main.banner-mode=off",
                        "--product.service.base-url=http://127.0.0.1:" + productPort,
                        "--inventory.service.base-url=http://127.0.0.1:" + inventoryPort,
                        "--payment.service.base-url=http://127.0.0.1:" + paymentPort);

        orderPort = ((WebServerApplicationContext) orderContext).getWebServer().getPort();
    }

    @AfterEach
    void stopServices() {
        if (paymentContext != null) paymentContext.close();
        if (orderContext != null) {
            orderContext.close();
        }
        if (productContext != null) {
            productContext.close();
        }
        if (inventoryContext != null) {
            inventoryContext.close();
        }
    }

    @Test
    void declineReleasesStock() throws Exception {
        int port = ((WebServerApplicationContext) paymentContext).getWebServer().getPort();
        paymentContext.close();
        paymentContext = new SpringApplicationBuilder(za.co.shoplink.payment.PaymentApplication.class)
                .run("--server.port=" + port, "--payment.simulation.decline=true");
        assertOrderOutcome("PAYMENT_FAILED", 50);
    }

    @Test
    void unavailablePaymentKeepsOrderPendingAndStockHeld() throws Exception {
        paymentContext.close();
        assertOrderOutcome("PAYMENT_PENDING", 49);
    }

    private void assertOrderOutcome(String status, int stock) throws Exception {
        HttpResponse<String> response = HTTP_CLIENT.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + orderPort + "/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"customerId\":\"C1\",\"items\":[{\"productId\":\"P1002\",\"quantity\":1}]}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        JsonNode order = OBJECT_MAPPER.readTree(response.body());
        assertEquals(status, order.path("status").asText());
        HttpResponse<String> retrieved = HTTP_CLIENT.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + orderPort + "/orders/" + order.path("id").asText()))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(status, OBJECT_MAPPER.readTree(retrieved.body()).path("status").asText());
        assertEquals(stock, inventoryContext.getBean(za.co.shoplink.inventory.service.InventoryService.class)
                .available("P1002"));
    }

    @Test
    void createsAndRetrievesAnOrderUsingTheRunningProductService() throws Exception {
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + orderPort + "/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"customerId\":\"C002\",\"items\":[{\"productId\":\"P1002\",\"quantity\":3}]}",
                        StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> createResponse = HTTP_CLIENT.send(
                createRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, createResponse.statusCode());
        JsonNode createdOrder = OBJECT_MAPPER.readTree(createResponse.body());
        assertEquals("C002", createdOrder.path("customerId").asText());
        assertEquals("PAID", createdOrder.path("status").asText());
        assertEquals("Canvas Tote Bag", createdOrder.path("items").get(0).path("productName").asText());
        assertEquals(149.99, createdOrder.path("items").get(0).path("unitPrice").asDouble());
        assertEquals("ZAR", createdOrder.path("items").get(0).path("currency").asText());

        String orderId = createdOrder.path("id").asText();
        assertNotNull(orderId);
        za.co.shoplink.payment.Payment payment = paymentContext.getBean(za.co.shoplink.payment.PaymentService.class)
                .find(orderId);
        assertEquals(0, new java.math.BigDecimal("449.97").compareTo(payment.amount()));
        assertEquals("ZAR", payment.currency().getCurrencyCode());
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + orderPort + "/orders/" + orderId))
                .GET()
                .build();

        HttpResponse<String> getResponse = HTTP_CLIENT.send(
                getRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResponse.statusCode());
        JsonNode retrievedOrder = OBJECT_MAPPER.readTree(getResponse.body());
        assertEquals(orderId, retrievedOrder.path("id").asText());
        assertEquals("Canvas Tote Bag", retrievedOrder.path("items").get(0).path("productName").asText());

        HttpRequest stockRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + ((WebServerApplicationContext) inventoryContext)
                        .getWebServer().getPort() + "/inventory/P1002"))
                .GET().build();
        HttpResponse<String> stockResponse = HTTP_CLIENT.send(
                stockRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, stockResponse.statusCode());
        assertEquals(47, OBJECT_MAPPER.readTree(stockResponse.body()).path("availableQuantity").asInt());
    }
}
