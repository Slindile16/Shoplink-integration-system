package za.co.shoplink.delivery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import za.co.shoplink.common.events.OrderPaidEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;

@Service
@RestController
@RequestMapping("/deliveries")
public class DeliveryService {

    public record Delivery(
            String orderId,
            String customerId,
            String status
    ) {}

    private final Map<String, Delivery> deliveries =
            new ConcurrentHashMap<>();

    @JmsListener(destination = MessagingConfiguration.ORDER_PAID)
    public void onPaid(OrderPaidEvent event) {

        deliveries.computeIfAbsent(
                event.orderId(),
                id -> new Delivery(
                        id,
                        event.customerId(),
                        "PREPARING"
                )
        );
    }

    @GetMapping("/{orderId}")
    public Delivery find(@PathVariable String orderId) {

        Delivery delivery = deliveries.get(orderId);

        if (delivery == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Delivery not found"
            );
        }

        return delivery;
    }
}