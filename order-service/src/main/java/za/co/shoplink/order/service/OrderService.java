package za.co.shoplink.order.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import za.co.shoplink.order.client.ProductServiceClient;
import za.co.shoplink.order.dto.CreateOrderRequest;
import za.co.shoplink.order.dto.ProductDetails;
import za.co.shoplink.order.model.Order;
import za.co.shoplink.order.model.OrderItem;
import za.co.shoplink.order.model.OrderStatus;
import za.co.shoplink.order.repository.OrderRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final ProductServiceClient productServiceClient;

    public OrderService(OrderRepository repository, ProductServiceClient productServiceClient) {
        this.repository = repository;
        this.productServiceClient = productServiceClient;
    }

    public Order create(CreateOrderRequest request) {
        List<OrderItem> items = request.items().stream().map(item -> {
            ProductDetails product;
            try {
                product = productServiceClient.findById(item.productId());
            } catch (HttpClientErrorException.NotFound ex) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Unknown product: " + item.productId());
            } catch (RestClientException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Product Service could not be reached", ex);
            }
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Product Service returned no details for product: " + item.productId());
            }
            return new OrderItem(product.id(), product.name(), item.quantity(), product.price(), product.currency());
        }).toList();

        Order order = new Order(UUID.randomUUID().toString(), request.customerId(), OrderStatus.CREATED,
                items, Instant.now());
        return repository.save(order);
    }

    public Order findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }
}
