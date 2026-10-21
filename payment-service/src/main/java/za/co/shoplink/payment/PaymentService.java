package za.co.shoplink.payment;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class PaymentService {
    private final Map<String, Payment> payments = new HashMap<>();
    private final boolean decline;
    public PaymentService(@Value("${payment.simulation.decline:false}") boolean decline) { this.decline = decline; }
    public synchronized Payment process(PaymentRequest request) {
        Payment existing = payments.get(request.orderId());
        if (existing != null) {
            if (existing.amount().compareTo(request.amount()) != 0 || !existing.currency().equals(request.currency()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already has a different payment");
            return existing;
        }
        Payment payment = new Payment(UUID.randomUUID().toString(), request.orderId(), request.amount(),
                request.currency(), decline ? Payment.Status.DECLINED : Payment.Status.APPROVED, Instant.now());
        payments.put(request.orderId(), payment);
        return payment;
    }
    public synchronized Payment find(String orderId) {
        Payment payment = payments.get(orderId);
        if (payment == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        return payment;
    }
}
