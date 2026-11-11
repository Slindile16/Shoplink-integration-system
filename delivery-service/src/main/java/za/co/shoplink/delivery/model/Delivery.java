package za.co.shoplink.delivery.model;

import java.time.Instant;

public record Delivery(String deliveryId, String orderId, String status, Instant createdAt) {
}
