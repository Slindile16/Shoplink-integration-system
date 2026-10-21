# ShopLink Payment Service

Simulates payments in memory; no real money or payment credentials are involved. Runs on port 8083. In Phase 5 it consumes JSON `ORDER_CREATED` events from ActiveMQ and returns `PAYMENT_PROCESSED` events to Order Service. Consumption and reply use the same JMS transaction.

Payments approve by default. Set `PAYMENT_SIMULATION_DECLINE=true` before starting to simulate declines. Repeated order IDs reuse their existing payment; conflicting amounts/currencies are rejected.

The REST API remains available for simulation and inspection:

- `POST /payments`: accepts `{"orderId":"O1","amount":179.98,"currency":"ZAR"}`. Returns the payment ID, order ID, amount, currency, status, and creation time. Invalid requests return `400`, conflicting duplicates `409`.
- `GET /payments/orders/{orderId}`: returns the payment, or `404`.

Order Service uses messaging instead of the payment REST endpoint. Data and duplicate protection reset when this service restarts.

See [Phase 5 setup and verification](../docs/messaging.md). Install shared modules first, start ActiveMQ, then run `mvn -f payment-service/pom.xml spring-boot:run`. Configure `ACTIVEMQ_BROKER_URL`, `ACTIVEMQ_USER`, and `ACTIVEMQ_PASSWORD` for a different broker.
