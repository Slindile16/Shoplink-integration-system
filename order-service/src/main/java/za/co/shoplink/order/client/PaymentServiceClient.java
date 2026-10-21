package za.co.shoplink.order.client;
import java.math.BigDecimal;
import java.util.Currency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class PaymentServiceClient {
    private final RestClient client;
    public PaymentServiceClient(RestClient.Builder builder,
            @Value("${payment.service.base-url:http://localhost:8083}") String baseUrl) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        client = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
    public record Request(String orderId, BigDecimal amount, Currency currency) {}
    public record Result(String id, String orderId, BigDecimal amount, Currency currency, String status) {}
    public Result pay(String orderId, BigDecimal amount, Currency currency) {
        return client.post().uri("/payments").body(new Request(orderId, amount, currency))
                .retrieve().body(Result.class);
    }
}
