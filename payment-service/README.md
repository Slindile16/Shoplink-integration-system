# ShopLink Payment Service

Simulates payments in memory; no real money or payment credentials are involved. Data resets on restart.

Run `mvn -f payment-service/pom.xml spring-boot:run` from the root. Default port: 8083.

`POST /payments` accepts `{"orderId":"O1","amount":179.98,"currency":"ZAR"}` and returns a payment ID, order ID, amount, currency, status (`APPROVED` or `DECLINED`), and creation time. Amount must be positive and at least 0.01. Invalid requests return 400.

`GET /payments/orders/{orderId}` returns the recorded result, or 404. Repeating a payment for the same order and amount/currency returns the original result; conflicting details return 409.

Payments approve by default. Set `PAYMENT_SIMULATION_DECLINE=true` before starting this service to simulate declines for every payment. This is a server simulation setting, not customer input.

Order flow: product validation -> inventory reservation -> PAYMENT_PENDING -> payment -> PAID, or stock release -> PAYMENT_FAILED. Failed release leaves STOCK_RELEASE_PENDING. Payment timeouts or unavailable responses leave PAYMENT_PENDING and keep stock held because approval may already have occurred. Use the payment lookup endpoint to inspect the outcome. Automatic reconciliation and durable recovery are not implemented; state is lost on restart. Keep services running together while inspecting pending outcomes.

Run all tests with `mvn test` at the repository root. Messaging is planned for Phase 5.
