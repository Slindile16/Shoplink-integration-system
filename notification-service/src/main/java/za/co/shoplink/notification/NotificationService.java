package za.co.shoplink.notification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import za.co.shoplink.common.events.OrderPaidEvent;
import za.co.shoplink.common.events.OrderPaymentFailedEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;

@Service
@RestController
@RequestMapping("/notifications")
public class NotificationService {

    public record Notification(
            String orderId,
            String customerId,
            String message
    ) {}

    private final Map<String, Notification> notifications =
            new ConcurrentHashMap<>();

    @JmsListener(destination = MessagingConfiguration.NOTIFICATION_PAID)
    public void onPaid(OrderPaidEvent event) {

        notifications.putIfAbsent(
                event.orderId(),
                new Notification(
                        event.orderId(),
                        event.customerId(),
                        "Payment approved; order preparing for delivery"
                )
        );
    }

    @JmsListener(destination = MessagingConfiguration.ORDER_PAYMENT_FAILED)
    public void onFailed(OrderPaymentFailedEvent event) {

        notifications.putIfAbsent(
                event.orderId(),
                new Notification(
                        event.orderId(),
                        event.customerId(),
                        "Payment declined; order cancelled"
                )
        );
    }

    @GetMapping("/{orderId}")
    public Notification find(@PathVariable String orderId) {
        return notifications.get(orderId);
    }
}