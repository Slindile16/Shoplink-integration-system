package za.co.shoplink.inventory.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import za.co.shoplink.inventory.dto.StockReservationRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class InventoryService {
    private final Map<String, Map<String, Integer>> reservations = new HashMap<>();
    private final java.util.Set<String> released = new java.util.HashSet<>();
    private final Map<String, Integer> stock = new HashMap<>(Map.of(
            "P1001", 100, "P1002", 50, "P1003", 25));

    public synchronized void release(String orderId) {
        if (released.contains(orderId)) return;
        Map<String, Integer> items = reservations.remove(orderId);
        if (items == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
        items.forEach((productId, quantity) -> stock.compute(productId, (key, available) -> available + quantity));
        released.add(orderId);
    }

    public synchronized int available(String productId) {
        Integer quantity = stock.get(productId);
        if (quantity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown product: " + productId);
        }
        return quantity;
    }

    public synchronized void reserve(StockReservationRequest request) {
        Map<String, Integer> requested = new TreeMap<>();
        for (StockReservationRequest.Item item : request.items()) {
            requested.merge(item.productId(), item.quantity(), Math::addExact);
        }
        if (released.contains(request.orderId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation already released");
        }
        Map<String, Integer> existing = reservations.get(request.orderId());
        if (existing != null) {
            if (!existing.equals(requested)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation differs");
            return;
        }
        for (Map.Entry<String, Integer> entry : requested.entrySet()) {
            Integer available = stock.get(entry.getKey());
            if (available == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown product: " + entry.getKey());
            }
            if (available < entry.getValue()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient stock for product: " + entry.getKey());
            }
        }
        reservations.put(request.orderId(), Map.copyOf(requested));
        requested.forEach((productId, quantity) -> stock.compute(productId, (key, available) -> available - quantity));
    }
}
