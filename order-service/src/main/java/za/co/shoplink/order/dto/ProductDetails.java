package za.co.shoplink.order.dto;

import java.math.BigDecimal;
import java.util.Currency;

public record ProductDetails(String id, String name, BigDecimal price, Currency currency) {
}
