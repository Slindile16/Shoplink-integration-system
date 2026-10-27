package za.co.shoplink.common.messaging;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import za.co.shoplink.common.events.OrderCreatedEvent;
import za.co.shoplink.common.events.OrderPaidEvent;
import za.co.shoplink.common.events.OrderPaymentFailedEvent;
import za.co.shoplink.common.events.PaymentProcessedEvent;

@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {

    public static final String ORDER_CREATED = "shoplink.order.created";
    public static final String PAYMENT_PROCESSED = "shoplink.payment.processed";
    public static final String ORDER_PAID = "shoplink.order.paid";
    public static final String NOTIFICATION_PAID = "shoplink.notification.paid";
    public static final String ORDER_PAYMENT_FAILED = "shoplink.order.payment.failed";

    @Bean
    public MappingJackson2MessageConverter eventMessageConverter() {
        var converter = new MappingJackson2MessageConverter();

        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("eventType");

        converter.setTypeIdMappings(Map.of(
                "ORDER_CREATED", OrderCreatedEvent.class,
                "PAYMENT_PROCESSED", PaymentProcessedEvent.class,
                "ORDER_PAID", OrderPaidEvent.class,
                "ORDER_PAYMENT_FAILED", OrderPaymentFailedEvent.class
        ));

        return converter;
    }
}
