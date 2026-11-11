package za.co.shoplink.common.catalogue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** The storefront JSON is also the backend seed, keeping option IDs and prices in sync. */
public final class JewelleryCatalogue {
    public record Variant(String id, String name, BigDecimal price, int initialStock) {}

    private static final List<Variant> VARIANTS = load();

    private JewelleryCatalogue() {}

    public static List<Variant> variants() {
        return VARIANTS;
    }

    private static List<Variant> load() {
        try (InputStream input = JewelleryCatalogue.class.getResourceAsStream("/shoplink/catalogue.json")) {
            if (input == null) throw new IllegalStateException("Jewellery catalogue resource is missing");
            List<Variant> result = new ArrayList<>();
            JsonNode products = new ObjectMapper().readTree(input);
            for (JsonNode product : products) {
                for (JsonNode option : product.path("options")) {
                    result.add(new Variant(product.path("sku").asText() + "-" + option.asText(),
                            product.path("name").asText() + " (" + option.asText() + ")",
                            product.path("price").decimalValue(), 8));
                }
            }
            return List.copyOf(result);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read the jewellery catalogue", ex);
        }
    }
}
