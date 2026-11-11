package za.co.shoplink.common.events;

public record DeliveryCreatedEvent(String deliveryId, String orderId, String status) {
    public DeliveryCreatedEvent {
        if (deliveryId == null || deliveryId.isBlank()) throw new IllegalArgumentException("deliveryId is required");
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId is required");
        if (!"CREATED".equals(status)) throw new IllegalArgumentException("status must be CREATED");
    }
}
