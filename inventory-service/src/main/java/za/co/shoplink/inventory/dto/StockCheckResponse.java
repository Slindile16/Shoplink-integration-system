package za.co.shoplink.inventory.dto;

public record StockCheckResponse(String productId, int availableQuantity) {
}
