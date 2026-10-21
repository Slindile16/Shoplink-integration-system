package za.co.shoplink.common.messaging;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import za.co.shoplink.common.events.OrderCreatedEvent;
import za.co.shoplink.common.events.PaymentProcessedEvent;

@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {
    public static final String ORDER_CREATED = "shoplink.order.created";
    public static final String PAYMENT_PROCESSED = "shoplink.payment.processed";

    @Bean
    public MappingJackson2MessageConverter eventMessageConverter() {
        var converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("eventType");
        converter.setTypeIdMappings(Map.of("ORDER_CREATED", OrderCreatedEvent.class,
                "PAYMENT_PROCESSED", PaymentProcessedEvent.class));
        return converter;
    }
}
