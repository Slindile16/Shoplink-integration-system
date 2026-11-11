package za.co.shoplink.notification.service;

import org.springframework.stereotype.Service;
import za.co.shoplink.common.events.DeliveryCreatedEvent;
import za.co.shoplink.common.events.OrderStatusChangedEvent;
import za.co.shoplink.notification.model.Notification;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {
    private final Map<String, Notification> notifications = new ConcurrentHashMap<>();

    public void recordOrderStatus(OrderStatusChangedEvent event) {
        String type = "ORDER_" + event.status();
        notifications.putIfAbsent(type + ":" + event.orderId(), new Notification(event.orderId(), type,
                "Order " + event.orderId() + " is " + event.status(), Instant.now()));
    }

    public void recordDelivery(DeliveryCreatedEvent event) {
        notifications.putIfAbsent("DELIVERY_CREATED:" + event.orderId(), new Notification(event.orderId(),
                "DELIVERY_CREATED", "Delivery " + event.deliveryId() + " was created", Instant.now()));
    }

    public List<Notification> findByOrderId(String orderId) {
        return notifications.values().stream().filter(item -> item.orderId().equals(orderId))
                .sorted(Comparator.comparing(Notification::createdAt)).toList();
    }
}
