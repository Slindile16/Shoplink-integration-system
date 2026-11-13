package za.co.shoplink.common.events;

public record OrderPaidEvent(String orderId) {
    public OrderPaidEvent {
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId is required");
    }
}
