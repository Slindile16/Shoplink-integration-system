package za.co.shoplink.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import za.co.shoplink.order.dto.StockReservationRequest;

import java.util.List;

@Component
public class InventoryServiceClient {
    private final RestClient restClient;

    public InventoryServiceClient(RestClient.Builder builder,
                                  @Value("${inventory.service.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public void release(String orderId) {
        restClient.post().uri("/inventory/reservations/{orderId}/release", orderId)
                .retrieve().toBodilessEntity();
    }

    public void reserve(String orderId, List<StockReservationRequest.Item> items) {
        restClient.post().uri("/inventory/reservations")
                .body(new StockReservationRequest(orderId, items))
                .retrieve().toBodilessEntity();
    }
}
