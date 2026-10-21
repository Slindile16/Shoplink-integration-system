# ShopLink Order Service

Runs on port 8081. `POST /orders` accepts `customerId` and an `items` list of `productId`/positive `quantity` values. Products are checked over HTTP, stock is reserved, and the saved order is published to ActiveMQ for payment. Returns `201` with a generated ID, trusted product details, and `PAYMENT_PENDING` status.

`GET /orders/{id}` returns the current order. Payment results arrive asynchronously over JMS: approval sets `PAID`; a decline releases stock and sets `PAYMENT_FAILED`. A failed release leaves `STOCK_RELEASE_PENDING` while broker redelivery can retry. If publishing the initial event fails, the order remains pending with stock held and needs manual reconciliation.

Invalid requests return `400`, unknown products `422`, insufficient stock `409`, unknown orders `404`, and upstream product/inventory errors `502`. Mixed currencies are rejected with `422`. All order state is in memory.

See [Phase 5 setup, PowerShell examples, and failure behavior](../docs/messaging.md) for the broker and startup commands. Product and Inventory URLs can be configured with `PRODUCT_SERVICE_URL` and `INVENTORY_SERVICE_URL`. Payment processing no longer uses `PAYMENT_SERVICE_URL`.
