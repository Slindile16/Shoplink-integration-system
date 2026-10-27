package za.co.shoplink.common.events;

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