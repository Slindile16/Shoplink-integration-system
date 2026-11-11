package za.co.shoplink.common.messaging;

import java.util.Map;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionFactoryCustomizer;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MessageConversionException;
import jakarta.jms.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import za.co.shoplink.common.events.OrderCreatedEvent;
import za.co.shoplink.common.events.PaymentProcessedEvent;
import za.co.shoplink.common.events.OrderPaidEvent;
import za.co.shoplink.common.events.DeliveryCreatedEvent;
import za.co.shoplink.common.events.OrderStatusChangedEvent;

@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {
    private static final ObjectMapper EVENT_MAPPER = new ObjectMapper();
    public static final String ORDER_CREATED = "shoplink.order.created";
    public static final String PAYMENT_PROCESSED = "shoplink.payment.processed";
    public static final String ORDER_PAID = "shoplink.order.paid";
    public static final String DELIVERY_CREATED = "shoplink.delivery.created";
    public static final String ORDER_STATUS_CHANGED = "shoplink.order.status.changed";

    @Bean
    public MappingJackson2MessageConverter eventMessageConverter() {
        var converter = new MappingJackson2MessageConverter() {
            @Override
            protected JavaType getJavaTypeForMessage(Message message) throws JMSException {
                String type = message.getStringProperty("eventType");
                Class<?> eventClass = type == null ? null : EVENT_TYPES.get(type);
                if (eventClass == null) throw new MessageConversionException("Unsupported eventType");
                return EVENT_MAPPER.constructType(eventClass);
            }
        };
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("eventType");
        converter.setTypeIdMappings(EVENT_TYPES);
        return converter;
    }

    private static final Map<String, Class<?>> EVENT_TYPES = Map.of("ORDER_CREATED", OrderCreatedEvent.class,
                "PAYMENT_PROCESSED", PaymentProcessedEvent.class,
                "ORDER_PAID", OrderPaidEvent.class,
                "DELIVERY_CREATED", DeliveryCreatedEvent.class,
                "ORDER_STATUS_CHANGED", OrderStatusChangedEvent.class);

    @Bean
    public ActiveMQConnectionFactoryCustomizer boundedRedelivery() {
        return factory -> {
            var policy = factory.getRedeliveryPolicy();
            policy.setInitialRedeliveryDelay(500);
            policy.setRedeliveryDelay(500);
            policy.setUseExponentialBackOff(true);
            policy.setBackOffMultiplier(2);
            policy.setMaximumRedeliveryDelay(4000);
            policy.setMaximumRedeliveries(3);
        };
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            DefaultJmsListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        var factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setSessionTransacted(true);
        return factory;
    }
}
