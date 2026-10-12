package za.co.shoplink.inventory.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void checksAvailableStock() throws Exception {
        mockMvc.perform(get("/inventory/P1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("P1001"))
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void createsAReservationAndReducesAvailableStock() throws Exception {
        mockMvc.perform(post("/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"O-100\",\"items\":[{\"productId\":\"P1003\",\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("O-100"))
                .andExpect(jsonPath("$.status").value("RESERVED"));
        mockMvc.perform(get("/inventory/P1003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(23));
    }

    @Test
    void rejectsInvalidReservationRequests() throws Exception {
        mockMvc.perform(post("/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"\",\"items\":[{\"productId\":\"P1001\",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInsufficientStockWithoutChangingOtherItems() throws Exception {
        mockMvc.perform(post("/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"O-101\",\"items\":[{\"productId\":\"P1001\",\"quantity\":1},{\"productId\":\"P1002\",\"quantity\":51}]}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/inventory/P1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }
}
