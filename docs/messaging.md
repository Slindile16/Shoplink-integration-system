# Phase 5: ActiveMQ / JMS

Order creation validates products over REST, reserves inventory over REST, saves `PAYMENT_PENDING`, and publishes `ORDER_CREATED`. Payment Service consumes it and publishes `PAYMENT_PROCESSED`. Order Service consumes the result, sets `PAID`, or releases stock over REST and sets `PAYMENT_FAILED`.

## Destinations and JSON contracts

Both destinations are JMS queues. One consumer handles each message; Delivery and Notification fan-out is future Phase 6 work. The JMS `eventType` property identifies the contract, and the body is a JSON TextMessage (no Java object serialization).

| Queue | eventType | Body fields |
|---|---|---|
| `shoplink.order.created` | `ORDER_CREATED` | `orderId`, `amount`, `currency` |
| `shoplink.payment.processed` | `PAYMENT_PROCESSED` | `paymentId`, `orderId`, `amount`, `currency`, `status` (`APPROVED` or `DECLINED`) |

The amount is calculated from Product Service prices, not supplied by the customer. All order lines must have the same currency. Events validate required fields and positive amounts. The result amount and currency must match the saved order before its state changes.

## Run locally (PowerShell)

Docker Desktop must be running, or use your own ActiveMQ Classic 6 broker.

From the project root:

```powershell
docker compose up -d activemq
mvn install -DskipTests
```

The install makes the shared `common` module available when starting services individually. Start each in its own terminal:

```powershell
mvn -f product-service/pom.xml spring-boot:run
mvn -f inventory-service/pom.xml spring-boot:run
mvn -f payment-service/pom.xml spring-boot:run
mvn -f order-service/pom.xml spring-boot:run
```

The broker uses port 61616. Its local web console is http://localhost:8161/admin (default development login: admin / admin). The Compose ports bind only to localhost. Configure both messaging services with `ACTIVEMQ_BROKER_URL`, `ACTIVEMQ_USER`, and `ACTIVEMQ_PASSWORD` when using a different broker. Use credentials and broker access controls appropriate to your deployment.

Place an order:

```powershell
$body = @{ customerId = 'C001'; items = @(@{ productId = 'P1001'; quantity = 2 }) } | ConvertTo-Json -Depth 4
$order = Invoke-RestMethod -Uri 'http://localhost:8081/orders' -Method Post -ContentType 'application/json' -Body $body
$order | ConvertTo-Json -Depth 6
Invoke-RestMethod "http://localhost:8081/orders/$($order.id)" | ConvertTo-Json -Depth 6
```

Creation returns `201` with `PAYMENT_PENDING`. Poll `GET /orders/{id}` to observe the later result. Payment Service still provides its REST simulation and payment lookup endpoints; Order Service no longer calls them for payment processing.

For delayed processing, stop Payment Service, place an order, then restart Payment Service: the broker queues the work. For declines, set `$env:PAYMENT_SIMULATION_DECLINE = 'true'` in the Payment Service terminal before starting it. The setting applies to new payments; existing payment outcomes are retained until that service restarts.

## Delivery and failure behavior

Payment listeners use Spring Boot's transacted JMS sessions: consuming an order event and sending its reply commit together. Payment processing reuses the same in-memory result for duplicate order IDs. Order result handling ignores repeated terminal results and retries a declined stock release safely. Failed releases preserve `STOCK_RELEASE_PENDING` and throw so the broker can redeliver. Invalid or unknown order results also roll back; broker redelivery limits can eventually send poison messages to `ActiveMQ.DLQ`. Inspect that queue if an order remains pending.

If initial event publication fails or is uncertain, creation still returns the saved `PAYMENT_PENDING` order and keeps stock held. An error with the order ID is logged. There is no automatic retry of initial publication; reconcile these orders manually. Re-posting the customer order creates a new order and is not a safe retry.

The broker volume persists messages, but application orders, reservations, and payments remain in memory. JMS transactions do not atomically commit that application state or HTTP inventory calls. Restarting services with queued messages can therefore produce unknown-order results or lose payment deduplication history. Durable persistence, an outbox, and crash-safe reconciliation remain future work. This phase does not claim exactly-once processing.

## Verification

`mvn clean test` starts an isolated, non-persistent ActiveMQ Classic broker on a random TCP port and runs actual HTTP/JMS flows. Docker is not required for these tests. Coverage includes approval and calculated totals, decline/stock release, delayed consumers, duplicate events, failed release/retry, and broker outages.

References: [Spring Boot JMS](https://docs.spring.io/spring-boot/3.5/reference/messaging/jms.html), [Spring JMS](https://docs.spring.io/spring-framework/reference/integration/jms/using.html), [Apache ActiveMQ Classic container](https://hub.docker.com/r/apache/activemq-classic).
