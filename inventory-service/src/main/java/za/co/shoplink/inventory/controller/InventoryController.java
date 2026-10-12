package za.co.shoplink.inventory.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.shoplink.inventory.dto.ReservationResponse;
import za.co.shoplink.inventory.dto.StockCheckResponse;
import za.co.shoplink.inventory.dto.StockReservationRequest;
import za.co.shoplink.inventory.service.InventoryService;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public StockCheckResponse available(@PathVariable("productId") String productId) {
        return new StockCheckResponse(productId, inventoryService.available(productId));
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@Valid @RequestBody StockReservationRequest request) {
        inventoryService.reserve(request);
        return new ReservationResponse(request.orderId(), "RESERVED");
    }
}
