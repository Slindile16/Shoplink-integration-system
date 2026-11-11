package za.co.shoplink.common.events;

public record OrderStatusChangedEvent(String orderId, String status) {
    public OrderStatusChangedEvent {
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId is required");
        if (status == null || status.isBlank()) throw new IllegalArgumentException("status is required");
    }
}
