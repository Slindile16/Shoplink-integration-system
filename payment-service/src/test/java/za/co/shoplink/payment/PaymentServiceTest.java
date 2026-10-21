package za.co.shoplink.payment;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;
class PaymentServiceTest {
    private PaymentRequest request(String amount) {
        return new PaymentRequest("O1", new BigDecimal(amount), Currency.getInstance("ZAR"));
    }
    @Test void approvesAndRetrievesPaymentWithoutDuplicateProcessing() {
        PaymentService service = new PaymentService(false);
        Payment payment = service.process(request("179.98"));
        assertEquals(Payment.Status.APPROVED, payment.status());
        assertEquals(payment, service.process(request("179.98")));
        assertEquals(payment, service.find("O1"));
        assertThrows(ResponseStatusException.class, () -> service.process(request("1.00")));
    }
    @Test void simulatesDeclinedPayment() {
        assertEquals(Payment.Status.DECLINED, new PaymentService(true).process(request("1.00")).status());
    }
}
