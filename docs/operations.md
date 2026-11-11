# Operations and failure recovery

## Start and inspect

Install Docker Desktop with Linux containers and Docker Compose v2. Allow about
4 GB of memory for the complete stack and builds. From the repository root:

```powershell
docker compose up --build -d --wait --wait-timeout 240
docker compose ps
./scripts/demo.ps1
docker compose logs --tail 100 order payment delivery notification
```

All published ports bind to loopback. The local ActiveMQ console is
http://localhost:8161/admin (development credentials `admin` / `admin`). Service
health endpoints are `/actuator/health` on ports 8080–8085. The four messaging
services include broker connectivity in health; they become unhealthy during a
broker outage while JMS reconnection continues automatically.

## Retry and dead-letter behavior

* A failed order-created, payment-status, or delivery-request publication stays
  in Order Service's process-local buffer and is retried every two seconds.
  `shoplink.events.retry-delay-ms` changes that delay.
* A failed declined-payment stock release remains `STOCK_RELEASE_PENDING` and
  is retried by both JMS redelivery and an independent two-second scheduler.
  It can therefore recover after the original message reaches the dead-letter queue.
* Listener failures roll back consumption. ActiveMQ redelivers up to three times
  after the initial attempt, with exponential delays starting at 500 ms and
  capped at four seconds. Persistent poison messages then go to `ActiveMQ.DLQ`.
* Missing or unrecognized `eventType` aliases, malformed JSON, invalid event
  fields, mismatched payment totals, and conflicting duplicate payments fail
  processing. They are never treated as successfully processed business events.
* HTTP dependency connections time out after two seconds and responses after
  three seconds. Error responses use Spring problem details. Do not blindly retry
  `POST /orders`: every request creates a fresh order ID.

To demonstrate recovery, stop the broker, submit an order, and restart the broker
without restarting Order Service:

```powershell
docker compose stop activemq
# POST an order: it is saved as PAYMENT_PENDING with stock reserved.
docker compose start activemq
# Poll the saved order ID; pending publications resume automatically.
```

For delayed-consumer behavior, stop `payment`, place an order, then start
`payment`. Its queued order event is processed when the consumer reconnects.

Inspect dead-letter messages in the broker console. Correct the underlying
problem before manually moving a message back to its original queue. Preserve
the JSON body and `eventType` property. Do not replay messages for orders lost
on application restart; they cannot be reconciled to the original in-memory state.
No automatic DLQ replay is configured, so permanent bad messages cannot loop forever.

## Clean demo resets

`docker compose down` retains broker data but discards application memory when
services stop. To start a completely fresh demo, remove the broker volume as well:

```powershell
# Destructive: deletes persisted demo messages.
docker compose down -v
docker compose up -d --wait --wait-timeout 240
```

Do not mix old broker messages with fresh application memory when testing a new
scenario. Avoid restarting or scaling Inventory independently: it resets stock
and forgets outstanding reservations. Use one instance of every service.

## Simulated declines

On a clean demo stack, configure Payment Service to decline:

```powershell
$env:PAYMENT_SIMULATION_DECLINE = 'true'
docker compose up -d --force-recreate payment
./scripts/demo.ps1 -ExpectedStatus PAYMENT_FAILED
Remove-Item Env:PAYMENT_SIMULATION_DECLINE
docker compose up -d --force-recreate payment
```

Use a new order for each scenario. A decline should restore its reservation and
produce one payment-failure notice, with no delivery.

## Known limits

The retry buffer, idempotency records, orders, and inventory all disappear on
restart and grow with demo activity until reset. A crash between saving state
and queuing its event is not recoverable. A timed-out reservation HTTP response
can be ambiguous: Inventory may have reserved stock even though Order Service
returns `502`. No durable reconciliation resolves that case. Production work
would need persistent state, a transactional outbox, API idempotency keys,
database stock transactions, retention policies, secrets, authentication, metrics,
and deployment-specific recovery procedures.
