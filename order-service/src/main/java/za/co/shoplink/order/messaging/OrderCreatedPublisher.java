package za.co.shoplink.order.messaging;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import za.co.shoplink.common.events.OrderCreatedEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;

@Component
public class OrderCreatedPublisher {
    private final JmsTemplate template;
    public OrderCreatedPublisher(JmsTemplate template) { this.template = template; }
    public void publish(OrderCreatedEvent event) {
        template.convertAndSend(MessagingConfiguration.ORDER_CREATED, event);
    }
}
