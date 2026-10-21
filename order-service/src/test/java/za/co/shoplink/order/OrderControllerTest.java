package za.co.shoplink.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import za.co.shoplink.order.client.ProductServiceClient;
import za.co.shoplink.order.client.InventoryServiceClient;
import za.co.shoplink.order.dto.ProductDetails;

import java.math.BigDecimal;
import java.util.Currency;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jms.listener.auto-startup=false")
@AutoConfigureMockMvc
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductServiceClient productServiceClient;

    @MockBean
    private InventoryServiceClient inventoryServiceClient;

    @MockBean
    private za.co.shoplink.order.messaging.OrderCreatedPublisher publisher;

    @Test
    void createsAndRetrievesOrderAsJson() throws Exception {
        when(productServiceClient.findById("P1001")).thenReturn(
                new ProductDetails("P1001", "ShopLink Starter Mug", new BigDecimal("89.99"), Currency.getInstance("ZAR")));

        String response = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"C001","items":[{"productId":"P1001","quantity":2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.customerId").value("C001"))
                .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.items[0].productName").value("ShopLink Starter Mug"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(89.99))
                .andReturn().getResponse().getContentAsString();
        verify(inventoryServiceClient).reserve(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.argThat(items -> items.size() == 1
                        && items.get(0).productId().equals("P1001") && items.get(0).quantity() == 2));

        String id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get("id").asText();
        var captured = org.mockito.ArgumentCaptor.forClass(za.co.shoplink.common.events.OrderCreatedEvent.class);
        verify(publisher).publish(captured.capture());
        org.junit.jupiter.api.Assertions.assertEquals(id, captured.getValue().orderId());
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("179.98").compareTo(captured.getValue().amount()));
        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"));
    }

    @Test
    void failedCompensationLeavesVisibleRecoveryState() throws Exception {
        when(productServiceClient.findById("P1001")).thenReturn(new ProductDetails(
                "P1001", "Mug", new BigDecimal("89.99"), Currency.getInstance("ZAR")));
        org.mockito.Mockito.doThrow(new org.springframework.web.client.ResourceAccessException("offline"))
                .when(inventoryServiceClient).release(org.mockito.ArgumentMatchers.anyString());
        String body = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"C1\",\"items\":[{\"productId\":\"P1001\",\"quantity\":1}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).path("id").asText();
        var result = new za.co.shoplink.common.events.PaymentProcessedEvent("PAY1", id,
                new BigDecimal("89.99"), Currency.getInstance("ZAR"),
                za.co.shoplink.common.events.PaymentProcessedEvent.Status.DECLINED);
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.client.RestClientException.class,
                () -> service.applyPayment(result));
        mockMvc.perform(get("/orders/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOCK_RELEASE_PENDING"));
        org.mockito.Mockito.doNothing().when(inventoryServiceClient).release(id);
        service.applyPayment(result);
        service.applyPayment(result);
        mockMvc.perform(get("/orders/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
        verify(inventoryServiceClient, org.mockito.Mockito.times(2)).release(id);
    }

    @Autowired private za.co.shoplink.order.service.OrderService service;

    @Test
    void mismatchedPaymentCannotChangeOrderStatus() throws Exception {
        when(productServiceClient.findById("P1001")).thenReturn(new ProductDetails(
                "P1001", "Mug", new BigDecimal("89.99"), Currency.getInstance("ZAR")));
        String body = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"C1\",\"items\":[{\"productId\":\"P1001\",\"quantity\":1}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).path("id").asText();
        var wrong = new za.co.shoplink.common.events.PaymentProcessedEvent("PAY1", id, BigDecimal.ONE,
                Currency.getInstance("ZAR"), za.co.shoplink.common.events.PaymentProcessedEvent.Status.APPROVED);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> service.applyPayment(wrong));
        mockMvc.perform(get("/orders/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"));
        org.mockito.Mockito.verify(inventoryServiceClient, org.mockito.Mockito.never())
                .release(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsInvalidOrderData() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsClearErrorForUnknownProduct() throws Exception {
        when(productServiceClient.findById("missing")).thenThrow(HttpClientErrorException.create(
                org.springframework.http.HttpStatus.NOT_FOUND, "Not Found", org.springframework.http.HttpHeaders.EMPTY,
                new byte[0], null));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C001\",\"items\":[{\"productId\":\"missing\",\"quantity\":1}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Unknown product: missing"));
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        mockMvc.perform(get("/orders/not-an-order"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order not found: not-an-order"));
    }
}
