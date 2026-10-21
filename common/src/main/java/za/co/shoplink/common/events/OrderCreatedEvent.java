package za.co.shoplink.common.events;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record OrderCreatedEvent(String orderId, BigDecimal amount, Currency currency) {
    public OrderCreatedEvent {
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        if (amount.compareTo(new BigDecimal("0.01")) < 0) throw new IllegalArgumentException("Invalid amount");
    }
}
