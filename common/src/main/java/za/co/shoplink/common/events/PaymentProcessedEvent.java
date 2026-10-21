package za.co.shoplink.common.events;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record PaymentProcessedEvent(String paymentId, String orderId, BigDecimal amount,
                                    Currency currency, Status status) {
    public enum Status { APPROVED, DECLINED }
    public PaymentProcessedEvent {
        if (paymentId == null || paymentId.isBlank()) throw new IllegalArgumentException("paymentId is required");
        new OrderCreatedEvent(orderId, amount, currency);
        Objects.requireNonNull(status, "status is required");
    }
}
