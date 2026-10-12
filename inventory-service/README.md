# ShopLink Inventory Service

The Inventory Service keeps sample product stock in memory. Its data resets to the sample quantities whenever the service restarts.

## API

### `GET /inventory/{productId}`

Returns `productId` and `availableQuantity`. The sample stock is `P1001: 100`, `P1002: 50`, and `P1003: 25`. Unknown product IDs return `404 Not Found`.

### `POST /inventory/reservations`

Request:

```json
{
  "orderId": "order-123",
  "items": [
    { "productId": "P1001", "quantity": 2 },
    { "productId": "P1002", "quantity": 1 }
  ]
}
```

A successful reservation returns `201 Created` with the order ID and `RESERVED` status. The service checks all requested quantities before changing stock. If any item is unknown or short on stock, the whole reservation is rejected (`404` for an unknown product, `409 Conflict` for insufficient quantity) and no stock is deducted. Invalid bodies return `400 Bad Request`.

## Run

From the repository root:

```shell
mvn -f inventory-service/pom.xml spring-boot:run
```

The service listens on `http://localhost:8082`.
