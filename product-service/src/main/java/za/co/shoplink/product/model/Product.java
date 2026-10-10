package za.co.shoplink.product.model;

import java.math.BigDecimal;
import java.util.Currency;

public record Product(String id, String name, BigDecimal price, Currency currency) {
}
