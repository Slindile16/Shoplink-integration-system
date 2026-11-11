package za.co.shoplink.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
<<<<<<< HEAD
=======

>>>>>>> 06b2bff313488b39abfe5428335d6d78d87eae83
import za.co.shoplink.common.messaging.MessagingConfiguration;

@SpringBootApplication
@Import(MessagingConfiguration.class)
public class DeliveryApplication {
<<<<<<< HEAD
    public static void main(String[] args) { SpringApplication.run(DeliveryApplication.class, args); }
}
=======

    public static void main(String[] args) {
        SpringApplication.run(DeliveryApplication.class, args);
    }
}
>>>>>>> 06b2bff313488b39abfe5428335d6d78d87eae83
