package za.co.shoplink.notification.messaging;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import za.co.shoplink.common.events.DeliveryCreatedEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;
import za.co.shoplink.notification.service.NotificationService;

@Component
public class DeliveryCreatedListener {
    private final NotificationService service;
    public DeliveryCreatedListener(NotificationService service) { this.service = service; }
    @JmsListener(destination = MessagingConfiguration.DELIVERY_CREATED)
    public void process(DeliveryCreatedEvent event) { service.recordDelivery(event); }
}
