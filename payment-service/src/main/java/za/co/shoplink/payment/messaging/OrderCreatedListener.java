package za.co.shoplink.payment.messaging;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import za.co.shoplink.common.events.OrderCreatedEvent;
import za.co.shoplink.common.events.PaymentProcessedEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;
import za.co.shoplink.payment.PaymentRequest;
import za.co.shoplink.payment.PaymentService;

@Component
public class OrderCreatedListener {
    private final PaymentService service;
    public OrderCreatedListener(PaymentService service) { this.service = service; }

    // Boot's transacted listener session commits consumption and the reply together.
    @JmsListener(destination = MessagingConfiguration.ORDER_CREATED)
    @SendTo(MessagingConfiguration.PAYMENT_PROCESSED)
    public PaymentProcessedEvent process(OrderCreatedEvent event) {
        var payment = service.process(new PaymentRequest(event.orderId(), event.amount(), event.currency()));
        return new PaymentProcessedEvent(payment.id(), payment.orderId(), payment.amount(), payment.currency(),
                PaymentProcessedEvent.Status.valueOf(payment.status().name()));
    }
}
