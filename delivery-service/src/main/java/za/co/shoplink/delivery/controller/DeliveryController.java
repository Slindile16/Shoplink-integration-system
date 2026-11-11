package za.co.shoplink.delivery.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.shoplink.delivery.model.Delivery;
import za.co.shoplink.delivery.service.DeliveryService;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {
    private final DeliveryService service;
    public DeliveryController(DeliveryService service) { this.service = service; }
    @GetMapping("/orders/{orderId}")
    public Delivery findByOrderId(@PathVariable("orderId") String orderId) { return service.findByOrderId(orderId); }
}
