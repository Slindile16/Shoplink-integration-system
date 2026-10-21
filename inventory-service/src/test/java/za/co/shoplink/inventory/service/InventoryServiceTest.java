package za.co.shoplink.inventory.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import za.co.shoplink.inventory.dto.StockReservationRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryServiceTest {
    @Test void releaseAndRepeatedReservationsDoNotChangeStockTwice() {
        InventoryService service = new InventoryService();
        StockReservationRequest request = new StockReservationRequest("O1", List.of(
                new StockReservationRequest.Item("P1001", 4)));
        service.reserve(request);
        service.reserve(request);
        assertEquals(96, service.available("P1001"));
        service.release("O1");
        service.release("O1");
        assertEquals(100, service.available("P1001"));
        assertThrows(ResponseStatusException.class, () -> service.reserve(request));
    }

    @Test
    void reservesStockAndReportsTheUpdatedQuantity() {
        InventoryService service = new InventoryService();
        service.reserve(new StockReservationRequest("O1", List.of(
                new StockReservationRequest.Item("P1001", 4))));

        assertEquals(96, service.available("P1001"));
    }

    @Test
    void insufficientStockLeavesEveryProductUnchanged() {
        InventoryService service = new InventoryService();
        assertThrows(ResponseStatusException.class, () -> service.reserve(new StockReservationRequest("O2", List.of(
                new StockReservationRequest.Item("P1001", 2),
                new StockReservationRequest.Item("P1002", 51)))));

        assertEquals(100, service.available("P1001"));
        assertEquals(50, service.available("P1002"));
    }

    @Test
    void combinesRepeatedProductLinesBeforeCheckingStock() {
        InventoryService service = new InventoryService();
        assertThrows(ResponseStatusException.class, () -> service.reserve(new StockReservationRequest("O3", List.of(
                new StockReservationRequest.Item("P1003", 20),
                new StockReservationRequest.Item("P1003", 6)))));
        assertEquals(25, service.available("P1003"));
    }

    @Test
    void unknownProductsReturnNotFound() {
        InventoryService service = new InventoryService();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.available("missing"));
        assertEquals(404, exception.getStatusCode().value());
    }
}
