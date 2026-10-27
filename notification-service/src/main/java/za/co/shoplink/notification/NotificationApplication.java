package za.co.shoplink.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import za.co.shoplink.common.messaging.MessagingConfiguration;

@SpringBootApplication
@Import(MessagingConfiguration.class)
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}