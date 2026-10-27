package za.co.shoplink.common.events;

public record OrderPaymentFailedEvent(
        String orderId,
        String customerId
) {
    public OrderPaymentFailedEvent {
        if (orderId == null || orderId.isBlank()
                || customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException(
                    "orderId and customerId required"
            );
        }
    }
}