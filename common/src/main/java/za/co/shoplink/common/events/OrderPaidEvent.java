package za.co.shoplink.common.events;

<<<<<<< HEAD
public record OrderPaidEvent(String orderId) {
    public OrderPaidEvent {
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId is required");
    }
}
=======
import java.math.BigDecimal;
import java.util.Currency;

public record OrderPaidEvent(
        String orderId,
        String customerId,
        BigDecimal amount,
        Currency currency
) {
    public OrderPaidEvent {
        new OrderCreatedEvent(orderId, amount, currency);

        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId required");
        }
    }
}
>>>>>>> 06b2bff313488b39abfe5428335d6d78d87eae83
