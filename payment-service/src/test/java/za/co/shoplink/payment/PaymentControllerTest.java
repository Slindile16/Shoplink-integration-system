package za.co.shoplink.payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties = "spring.jms.listener.auto-startup=false")
@AutoConfigureMockMvc
class PaymentControllerTest {
    @Autowired MockMvc mvc;
    @Test void rejectsInvalidPayment() throws Exception {
        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"\",\"amount\":-1,\"currency\":\"ZAR\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void processesAndRetrievesPayment() throws Exception {
        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"O1\",\"amount\":179.98,\"currency\":\"ZAR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.id").isNotEmpty());
        mvc.perform(get("/payments/orders/O1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(179.98));
        mvc.perform(get("/payments/orders/missing")).andExpect(status().isNotFound());
    }
}
