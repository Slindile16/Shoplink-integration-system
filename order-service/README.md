# ShopLink Order Service

The Order Service accepts orders, checks each item against Product Service over HTTP/JSON, and keeps created orders in memory. Order data is cleared whenever this service restarts. A successful order starts in `CREATED` status.

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

Each product ID is fetched from `GET /products/{id}` before the order is stored. The response is `201 Created` with a generated ID and the product details used to price each line:

```json
{
  "id": "b1df9343-7f1e-41e9-814f-395ef22979c9",
  "customerId": "C001",
  "status": "CREATED",
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

## Run both services

From the repository root, start each command in its own terminal:

```shell
mvn -f product-service/pom.xml spring-boot:run
```

```shell
mvn -f order-service/pom.xml spring-boot:run
```

Product Service listens on `http://localhost:8080`; Order Service listens on `http://localhost:8081`. Set `PRODUCT_SERVICE_URL` when Product Service is hosted elsewhere.

Example requests:

```shell
curl -i -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"C001","items":[{"productId":"P1001","quantity":2}]}'
curl http://localhost:8081/orders/{id}
```

Run the service endpoint tests and root cross-service integration test with:

```shell
mvn -pl integration-tests -am test
```
