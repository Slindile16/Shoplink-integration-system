package za.co.shoplink.delivery.messaging;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import za.co.shoplink.common.events.DeliveryCreatedEvent;
import za.co.shoplink.common.events.OrderPaidEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;
import za.co.shoplink.delivery.service.DeliveryService;

@Component
public class OrderPaidListener {
    private final DeliveryService service;
    public OrderPaidListener(DeliveryService service) { this.service = service; }

    @JmsListener(destination = MessagingConfiguration.ORDER_PAID)
    @SendTo(MessagingConfiguration.DELIVERY_CREATED)
    public DeliveryCreatedEvent process(OrderPaidEvent event) { return service.createFor(event); }
}
