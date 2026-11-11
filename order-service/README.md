# ShopLink Order Service

Runs on port 8081. `POST /orders` accepts `customerId` and an `items` list of `productId`/positive `quantity` values. Products are checked over HTTP, stock is reserved, and the saved order is published to ActiveMQ for payment. Returns `201` with a generated ID, trusted product details, and `PAYMENT_PENDING` status.

`GET /orders/{id}` returns the current order. Payment results arrive asynchronously over JMS: approval sets `PAID`; a decline releases stock and sets `PAYMENT_FAILED`. A failed release leaves `STOCK_RELEASE_PENDING` and retries automatically. Failed publications stay in a process-local buffer and retry every two seconds, including order-status and delivery-request events.

Invalid requests return `400`, unknown products `422`, insufficient stock `409`, unknown orders `404`, and upstream product/inventory errors `502`. Mixed currencies are rejected with `422`. All order state is in memory.

See [messaging contracts](../docs/messaging.md) and the [operations runbook](../docs/operations.md) for setup and recovery limits. Product and Inventory URLs can be configured with `PRODUCT_SERVICE_URL` and `INVENTORY_SERVICE_URL`. Dependency HTTP calls have two-second connection and three-second read timeouts. State and retry buffers reset on restart.
