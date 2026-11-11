package za.co.shoplink.notification.messaging;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import za.co.shoplink.common.events.OrderStatusChangedEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;
import za.co.shoplink.notification.service.NotificationService;

@Component
public class OrderStatusListener {
    private final NotificationService service;
    public OrderStatusListener(NotificationService service) { this.service = service; }
    @JmsListener(destination = MessagingConfiguration.ORDER_STATUS_CHANGED)
    public void process(OrderStatusChangedEvent event) { service.recordOrderStatus(event); }
}
