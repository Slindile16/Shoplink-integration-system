package za.co.shoplink.order.repository;

import org.springframework.stereotype.Repository;
import za.co.shoplink.order.model.Order;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class OrderRepository {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public Order save(Order order) {
        orders.put(order.id(), order);
        return order;
    }

    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }
}
