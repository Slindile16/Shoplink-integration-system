package za.co.shoplink.order.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import za.co.shoplink.common.events.OrderCreatedEvent;
import za.co.shoplink.common.events.OrderPaidEvent;
import za.co.shoplink.common.events.OrderStatusChangedEvent;
import za.co.shoplink.common.events.PaymentProcessedEvent;

import za.co.shoplink.order.client.InventoryServiceClient;
import za.co.shoplink.order.client.ProductServiceClient;
import za.co.shoplink.order.dto.CreateOrderRequest;
import za.co.shoplink.order.dto.ProductDetails;

import za.co.shoplink.order.messaging.OrderCreatedPublisher;
import za.co.shoplink.order.messaging.OrderWorkflowPublisher;
import za.co.shoplink.order.messaging.OrderEventOutbox;
import za.co.shoplink.order.model.Order;
import za.co.shoplink.order.model.OrderItem;
import za.co.shoplink.order.model.OrderStatus;
import za.co.shoplink.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(OrderService.class);
    private final Map<String, PaymentProcessedEvent> pendingReleases = new HashMap<>();
    private final OrderRepository repository;
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final OrderCreatedPublisher publisher;
    private final OrderWorkflowPublisher workflowPublisher;
    private final OrderEventOutbox outbox;

    public OrderService(OrderRepository repository, ProductServiceClient productServiceClient,
                        InventoryServiceClient inventoryServiceClient, OrderCreatedPublisher publisher,
                        OrderWorkflowPublisher workflowPublisher, OrderEventOutbox outbox) {
        this.repository = repository;
        this.publisher = publisher;
        this.workflowPublisher = workflowPublisher;
        this.outbox = outbox;
        this.productServiceClient = productServiceClient;
        this.inventoryServiceClient = inventoryServiceClient;
    }

    public Order create(CreateOrderRequest request) {

        List<OrderItem> items = request.items().stream()
                .map(item -> {

                    ProductDetails product;

                    try {
                        product = productServiceClient.findById(
                                item.productId()
                        );
                    } catch (HttpClientErrorException.NotFound ex) {
                        throw new ResponseStatusException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "Unknown product: " + item.productId()
                        );
                    } catch (RestClientException ex) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY,
                                "Product Service could not be reached",
                                ex
                        );
                    }

                    if (product == null) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY,
                                "Product Service returned no details for product: "
                                        + item.productId()
                        );
                    }

                    return new OrderItem(
                            product.id(),
                            product.name(),
                            item.quantity(),
                            product.price(),
                            product.currency()
                    );
                })
                .toList();

        if (items.stream()
                .map(OrderItem::currency)
                .distinct()
                .count() != 1) {

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Order items must use one currency"
            );
        }

        String orderId = UUID.randomUUID().toString();

        try {
            inventoryServiceClient.reserve(orderId, items.stream()
                    .collect(Collectors.groupingBy(OrderItem::productId,
                            Collectors.reducing(0, OrderItem::quantity, Math::addExact)))
                    .entrySet().stream().map(entry -> new za.co.shoplink.order.dto.StockReservationRequest.Item(
                            entry.getKey(), entry.getValue())).toList());
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Total quantity exceeds the supported maximum", ex);
        } catch (HttpClientErrorException.Conflict ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient stock to fulfill order",
                    ex
            );

        } catch (RestClientException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Inventory Service could not reserve stock",
                    ex
            );
        }

        Instant createdAt = Instant.now();
        Order pending = repository.save(new Order(orderId, request.customerId(), OrderStatus.PAYMENT_PENDING, items, createdAt));
        outbox.enqueue("created:" + orderId,
                () -> publisher.publish(new OrderCreatedEvent(orderId, total(items), items.get(0).currency())));
        outbox.retryPending();
        return pending;
    }

    private BigDecimal total(List<OrderItem> items) {

        return items.stream()
                .map(item ->
                        item.unitPrice().multiply(
                                BigDecimal.valueOf(item.quantity())
                        )
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public synchronized void applyPayment(
            PaymentProcessedEvent payment
    ) {

        Order order = findById(payment.orderId());

        if (total(order.items()).compareTo(payment.amount()) != 0
                || !order.items().get(0).currency()
                        .equals(payment.currency())) {

            throw new IllegalArgumentException(
                    "Payment does not match order " + order.id()
            );
        }

        if (order.status() != OrderStatus.PAYMENT_PENDING
                && !(order.status() == OrderStatus.STOCK_RELEASE_PENDING
                && payment.status()
                        == PaymentProcessedEvent.Status.DECLINED)) {
            return;
        }

        OrderStatus status;

        if (payment.status()
                == PaymentProcessedEvent.Status.APPROVED) {

            status = OrderStatus.PAID;

        } else {
            pendingReleases.put(order.id(), payment);
            repository.save(new Order(order.id(), order.customerId(), OrderStatus.STOCK_RELEASE_PENDING,
                    order.items(), order.createdAt()));
            // Propagate failure so the transacted listener rolls back and the broker redelivers.
            inventoryServiceClient.release(order.id());

            status = OrderStatus.PAYMENT_FAILED;
        }
        repository.save(new Order(order.id(), order.customerId(), status, order.items(), order.createdAt()));
        pendingReleases.remove(order.id());
        publishWorkflowEvents(order.id(), payment.status());
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${shoplink.events.retry-delay-ms:2000}")
    public synchronized void retryStockReleases() {
        for (var payment : List.copyOf(pendingReleases.values())) {
            try {
                applyPayment(payment);
            } catch (RestClientException ex) {
                LOG.warn("Stock release for order {} remains pending: {}", payment.orderId(), ex.getMessage());
            }
        }
    }

    private void publishWorkflowEvents(String orderId, PaymentProcessedEvent.Status paymentStatus) {
        OrderStatusChangedEvent statusEvent = new OrderStatusChangedEvent(orderId,
                paymentStatus == PaymentProcessedEvent.Status.APPROVED ? "PAID" : "PAYMENT_FAILED");
        outbox.enqueue("status:" + orderId, () -> workflowPublisher.publishStatus(statusEvent));
        if (paymentStatus == PaymentProcessedEvent.Status.APPROVED) {
            outbox.enqueue("paid:" + orderId, () -> workflowPublisher.publishPaid(new OrderPaidEvent(orderId)));
        }
        // Do not send on the listener's JMS session: its transaction may roll back
        // after this method returns. The scheduler publishes independently.
    }

    public Order findById(String id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found: " + id
                        )
                );
    }
}