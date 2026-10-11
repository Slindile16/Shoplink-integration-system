package za.co.shoplink.order.model;

import java.time.Instant;
import java.util.List;

public record Order(String id, String customerId, OrderStatus status, List<OrderItem> items, Instant createdAt) {
    public Order {
        items = List.copyOf(items);
    }
}
