package za.co.shoplink.payment;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Currency;
public record PaymentRequest(@NotBlank String orderId, @NotNull @DecimalMin("0.01") BigDecimal amount,
                             @NotNull Currency currency) {}
