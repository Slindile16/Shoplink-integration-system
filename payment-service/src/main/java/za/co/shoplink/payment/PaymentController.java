package za.co.shoplink.payment;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }
    @PostMapping
    public Payment process(@Valid @RequestBody PaymentRequest request) { return service.process(request); }
    @GetMapping("/orders/{orderId}")
    public Payment find(@PathVariable("orderId") String orderId) { return service.find(orderId); }
}
