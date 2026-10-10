package za.co.shoplink.product.service;

import org.springframework.stereotype.Service;
import za.co.shoplink.product.model.Product;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private static final Currency CURRENCY = Currency.getInstance("ZAR");

    private final List<Product> products = List.of(
            new Product("P1001", "ShopLink Starter Mug", new BigDecimal("89.99"), CURRENCY),
            new Product("P1002", "Canvas Tote Bag", new BigDecimal("149.99"), CURRENCY),
            new Product("P1003", "Insulated Water Bottle", new BigDecimal("219.50"), CURRENCY)
    );

    public List<Product> findAll() {
        return products;
    }

    public Optional<Product> findById(String id) {
        return products.stream().filter(product -> product.id().equals(id)).findFirst();
    }
}
