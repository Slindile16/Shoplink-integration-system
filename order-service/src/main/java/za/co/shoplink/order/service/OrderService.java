package za.co.shoplink.order.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import za.co.shoplink.order.client.ProductServiceClient;
import za.co.shoplink.order.client.InventoryServiceClient;
import za.co.shoplink.order.dto.CreateOrderRequest;
import za.co.shoplink.order.dto.ProductDetails;
import za.co.shoplink.order.model.Order;
import za.co.shoplink.order.model.OrderItem;
import za.co.shoplink.order.model.OrderStatus;
import za.co.shoplink.order.repository.OrderRepository;

import java.time.Instant;
import java.math.BigDecimal;
import za.co.shoplink.order.client.PaymentServiceClient;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;

    private final PaymentServiceClient paymentServiceClient;

    public OrderService(OrderRepository repository, ProductServiceClient productServiceClient,
                        InventoryServiceClient inventoryServiceClient, PaymentServiceClient paymentServiceClient) {
        this.repository = repository;
        this.paymentServiceClient = paymentServiceClient;
        this.productServiceClient = productServiceClient;
        this.inventoryServiceClient = inventoryServiceClient;
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

        if (items.stream().map(OrderItem::currency).distinct().count() != 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Order items must use one currency");
        }
        String orderId = UUID.randomUUID().toString();
        try {
            inventoryServiceClient.reserve(orderId, items.stream()
                    .collect(Collectors.groupingBy(OrderItem::productId,
                            Collectors.summingInt(OrderItem::quantity)))
                    .entrySet().stream().map(entry -> new za.co.shoplink.order.dto.StockReservationRequest.Item(
                            entry.getKey(), entry.getValue())).toList());
        } catch (HttpClientErrorException.Conflict ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock to fulfill order", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Inventory Service could not reserve stock", ex);
        }
        Instant createdAt = Instant.now();
        repository.save(new Order(orderId, request.customerId(), OrderStatus.PAYMENT_PENDING, items, createdAt));
        BigDecimal total = items.stream().map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PaymentServiceClient.Result payment;
        try {
            payment = paymentServiceClient.pay(orderId, total, items.get(0).currency());
        } catch (RestClientException ex) {
            return findById(orderId);
        }
        // An ambiguous response must keep stock held until payment can be reconciled.
        if (payment == null || !orderId.equals(payment.orderId()) || payment.amount() == null
                || total.compareTo(payment.amount()) != 0 || !items.get(0).currency().equals(payment.currency())) {
            return findById(orderId);
        }
        OrderStatus status = OrderStatus.PAYMENT_PENDING;
        if ("APPROVED".equals(payment.status())) {
            status = OrderStatus.PAID;
        } else if ("DECLINED".equals(payment.status())) {
            status = OrderStatus.STOCK_RELEASE_PENDING;
            repository.save(new Order(orderId, request.customerId(), status, items, createdAt));
            try {
                inventoryServiceClient.release(orderId);
                status = OrderStatus.PAYMENT_FAILED;
            } catch (RestClientException ex) {
                // Preserve a visible state for manual reconciliation if compensation fails.
            }
        }
        return repository.save(new Order(orderId, request.customerId(), status, items, createdAt));
    }

    public Order findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }
}
