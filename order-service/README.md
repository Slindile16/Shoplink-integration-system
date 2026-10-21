# ShopLink Order Service

The Order Service accepts orders, checks each item against Product Service over HTTP/JSON, reserves all requested stock through Inventory Service, then keeps created orders in memory. Order data is cleared whenever this service restarts. The order is saved as `PAYMENT_PENDING` before payment processing and becomes `PAID` after approval.

## API

### `POST /orders`

Request:

```json
{
  "customerId": "C001",
  "items": [
    { "productId": "P1001", "quantity": 2 }
  ]
}
```

Each product ID is fetched from `GET /products/{id}`, then all quantities are reserved in one Inventory Service request before the order is stored. Insufficient stock returns `409 Conflict`; Inventory Service failures return `502 Bad Gateway`. The response is `201 Created` with a generated ID and the product details used to price each line:

```json
{
  "id": "b1df9343-7f1e-41e9-814f-395ef22979c9",
  "customerId": "C001",
  "status": "PAID",
  "items": [
    {
      "productId": "P1001",
      "productName": "ShopLink Starter Mug",
      "quantity": 2,
      "unitPrice": 89.99,
      "currency": "ZAR"
    }
  ],
  "createdAt": "2026-10-11T10:15:30Z"
}
```

### `GET /orders/{id}`

Returns the saved order, including its current status. Unknown IDs return `404 Not Found`.

Invalid or empty order data returns `400 Bad Request`. An unknown product returns `422 Unprocessable Entity` with a message such as `Unknown product: P9999`. If Product Service cannot be reached, the Order Service returns `502 Bad Gateway`.

## Payment outcomes

The total is calculated from trusted product prices and quantities; mixed currencies are rejected before reservation. Confirmed declines release reserved stock and return an order in `PAYMENT_FAILED`. A failed release leaves `STOCK_RELEASE_PENDING`. Payment connection failures, timeouts, or ambiguous responses leave `PAYMENT_PENDING` and retain stock. These orders remain retrievable through `GET /orders/{id}`; creation returns `201` for all saved outcomes.

Pending outcomes require manual reconciliation; automatic retries and recovery are future work. Do not submit a new order to retry a pending payment. All state is in memory and resets on restart.

## Run services

From the repository root, start each command in its own terminal:

```shell
mvn -f product-service/pom.xml spring-boot:run
```

```shell
mvn -f inventory-service/pom.xml spring-boot:run
```

```shell
mvn -f order-service/pom.xml spring-boot:run
```

Start Payment Service using `mvn -f payment-service/pom.xml spring-boot:run`. It listens on port 8083; set `PAYMENT_SERVICE_URL` for another address.

Product Service listens on `http://localhost:8080`, Order Service on `http://localhost:8081`, and Inventory Service on `http://localhost:8082`. Set `PRODUCT_SERVICE_URL` or `INVENTORY_SERVICE_URL` when a dependency is hosted elsewhere.

Example requests:

```shell
curl -i -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P1001","quantity":2}]}'
curl http://localhost:8081/orders/{id}
```

Run the service endpoint tests and root cross-service integration test (including stock deduction) with:

```shell
mvn -pl integration-tests -am test
```
