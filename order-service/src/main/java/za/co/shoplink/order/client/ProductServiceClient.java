package za.co.shoplink.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import za.co.shoplink.order.dto.ProductDetails;

@Component
public class ProductServiceClient {
    private final RestClient restClient;

    public ProductServiceClient(RestClient.Builder builder,
                                @Value("${product.service.base-url:http://localhost:8080}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ProductDetails findById(String id) {
        return restClient.get().uri("/products/{id}", id).retrieve().body(ProductDetails.class);
    }
}
