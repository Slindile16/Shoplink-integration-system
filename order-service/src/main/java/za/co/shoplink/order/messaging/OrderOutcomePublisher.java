package za.co.shoplink.order.messaging;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import za.co.shoplink.common.events.OrderPaidEvent;
import za.co.shoplink.common.events.OrderPaymentFailedEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;

@Component
public class OrderOutcomePublisher {

    private final JmsTemplate template;

    public OrderOutcomePublisher(JmsTemplate template) {
        this.template = template;
    }

    public void paid(OrderPaidEvent event) {
        template.convertAndSend(
                MessagingConfiguration.ORDER_PAID,
                event
        );

        template.convertAndSend(
                MessagingConfiguration.NOTIFICATION_PAID,
                event
        );
    }

    public void failed(OrderPaymentFailedEvent event) {
        template.convertAndSend(
                MessagingConfiguration.ORDER_PAYMENT_FAILED,
                event
        );
    }
}