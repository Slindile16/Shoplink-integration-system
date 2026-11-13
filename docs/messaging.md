# Messaging contracts

All destinations are JMS queues. Events are JSON TextMessages with an `eventType`
property identifying a known contract alias. Arbitrary Java class names are
rejected. No Java object serialization is used.

| Queue | eventType | Body fields |
|---|---|---|
| `shoplink.order.created` | `ORDER_CREATED` | `orderId`, positive `amount`, `currency` |
| `shoplink.payment.processed` | `PAYMENT_PROCESSED` | `paymentId`, `orderId`, positive `amount`, `currency`, `status` (`APPROVED` or `DECLINED`) |
| `shoplink.order.paid` | `ORDER_PAID` | `orderId` |
| `shoplink.delivery.created` | `DELIVERY_CREATED` | `deliveryId`, `orderId`, `status` (`CREATED`) |
| `shoplink.order.status.changed` | `ORDER_STATUS_CHANGED` | `orderId`, `status` (`PAID` or `PAYMENT_FAILED`) |

Order creation validates product details, reserves stock, and saves PAYMENT_PENDING.
Its publication buffer sends ORDER_CREATED immediately and retries failures in the
background. Payment consumes it and replies with PAYMENT_PROCESSED in the same
JMS transaction. Order validates the reply against its calculated amount and currency.
For declines it releases stock before setting PAYMENT_FAILED; failures remain
STOCK_RELEASE_PENDING and retry automatically. Approval sets PAID.

The order transition enqueues order-status and, for approval, delivery-request
publications. A scheduler sends them independently from the payment listener's
transaction. Delivery creates one record per order and returns DELIVERY_CREATED
in its listener transaction. Notification records one notice per order and type.
Arrival order between separate queues is not guaranteed.

## Configuration

The broker defaults to `tcp://localhost:61616`, with development credentials
`admin` / `admin`. Set `ACTIVEMQ_BROKER_URL`, `ACTIVEMQ_USER`, and
`ACTIVEMQ_PASSWORD` for another local broker. Docker Compose supplies service
hostnames internally. Start the complete system with:

```powershell
docker compose up --build -d --wait --wait-timeout 240
```

Alternatively start only the broker with `docker compose up -d activemq`, install
shared modules with `mvn install`, then start each service with
`mvn -f <service>/pom.xml spring-boot:run`.

## Failures and verification

Listeners are transacted. Failed messages get three redeliveries after the initial
attempt, using exponential delays starting at 500 ms. Poison messages are moved
to `ActiveMQ.DLQ`. The order publication buffer and pending stock-release scheduler
retry every two seconds while Order Service stays running. Duplicate consumers
prevent repeated business effects. This provides process-local recovery, not
exactly-once execution or restart durability.

See the [operations runbook](operations.md) for DLQ inspection, replay precautions,
and the limitations of in-memory state. Use `mvn clean package` to build services. See the root README for an order request example.
