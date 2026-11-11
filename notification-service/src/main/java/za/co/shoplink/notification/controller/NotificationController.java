package za.co.shoplink.notification.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.shoplink.notification.model.Notification;
import za.co.shoplink.notification.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping("/{orderId}")
    public List<Notification> findByOrderId(@PathVariable("orderId") String orderId) {
        return service.findByOrderId(orderId);
    }
}
