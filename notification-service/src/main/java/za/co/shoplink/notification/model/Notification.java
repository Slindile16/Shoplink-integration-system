package za.co.shoplink.notification.model;

import java.time.Instant;

public record Notification(String orderId, String type, String message, Instant createdAt) {
}
