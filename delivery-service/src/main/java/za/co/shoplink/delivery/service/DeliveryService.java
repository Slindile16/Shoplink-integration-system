package za.co.shoplink.delivery.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import za.co.shoplink.common.events.DeliveryCreatedEvent;
import za.co.shoplink.common.events.OrderPaidEvent;
import za.co.shoplink.delivery.model.Delivery;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeliveryService {
    private final Map<String, Delivery> deliveries = new ConcurrentHashMap<>();

    public DeliveryCreatedEvent createFor(OrderPaidEvent event) {
        Delivery delivery = deliveries.computeIfAbsent(event.orderId(), orderId ->
                new Delivery(UUID.randomUUID().toString(), orderId, "CREATED", Instant.now()));
        return new DeliveryCreatedEvent(delivery.deliveryId(), delivery.orderId(), delivery.status());
    }

    public Delivery findByOrderId(String orderId) {
        Delivery delivery = deliveries.get(orderId);
        if (delivery == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found for order: " + orderId);
        return delivery;
    }
}
