package za.co.shoplink.payment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@org.springframework.context.annotation.Import(za.co.shoplink.common.messaging.MessagingConfiguration.class)
@SpringBootApplication
public class PaymentApplication {
    public static void main(String[] args) { SpringApplication.run(PaymentApplication.class, args); }
}
