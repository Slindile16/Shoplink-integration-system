package za.co.shoplink.payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
public record Payment(String id, String orderId, BigDecimal amount, Currency currency, Status status, Instant createdAt) {
    public enum Status { APPROVED, DECLINED }
}
