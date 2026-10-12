package za.co.shoplink.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record StockReservationRequest(
        @NotBlank(message = "orderId is required") String orderId,
        @NotEmpty(message = "items must contain at least one item") List<@NotNull @Valid Item> items) {
    public record Item(@NotBlank(message = "productId is required") String productId,
                       @Positive(message = "quantity must be greater than zero") int quantity) {
    }
}
