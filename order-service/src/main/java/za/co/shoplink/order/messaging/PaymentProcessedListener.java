package za.co.shoplink.order.messaging;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import za.co.shoplink.common.events.PaymentProcessedEvent;
import za.co.shoplink.common.messaging.MessagingConfiguration;
import za.co.shoplink.order.service.OrderService;

@Component
public class PaymentProcessedListener {
    private final OrderService service;
    public PaymentProcessedListener(OrderService service) { this.service = service; }
    @JmsListener(destination = MessagingConfiguration.PAYMENT_PROCESSED)
    public void process(PaymentProcessedEvent event) { service.applyPayment(event); }
}
