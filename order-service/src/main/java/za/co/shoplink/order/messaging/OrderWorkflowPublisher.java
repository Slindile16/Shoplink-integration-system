package za.co.shoplink.order.messaging;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import za.co.shoplink.common.events.OrderPaidEvent;
import za.co.shoplink.common.events.OrderStatusChangedEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;

@Component
public class OrderWorkflowPublisher {
    private final JmsTemplate jmsTemplate;
    public OrderWorkflowPublisher(JmsTemplate jmsTemplate) { this.jmsTemplate = jmsTemplate; }

    public void publishStatus(OrderStatusChangedEvent event) {
        jmsTemplate.convertAndSend(MessagingConfiguration.ORDER_STATUS_CHANGED, event);
    }

    public void publishPaid(OrderPaidEvent event) {
        jmsTemplate.convertAndSend(MessagingConfiguration.ORDER_PAID, event);
    }
}
