package za.co.shoplink.order.dto;

import java.util.List;

public record StockReservationRequest(String orderId, List<Item> items) {
    public record Item(String productId, int quantity) {
    }
}
