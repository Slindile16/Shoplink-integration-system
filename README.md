# ShopLink — Online Store Integration System

## The Heirloom Verse website

ShopLink now also powers **The Heirloom Verse**, a luxury fine jewellery portfolio
storefront. Start the complete stack with Docker Compose, then visit
**http://localhost:3000** to browse 101 designs for women, men and unisex collections,
including matching ring sets and black-and-white bracelet pairs, select sizes, manage a shopping bag,
place a simulated order and track payment/delivery. See the
[storefront guide](storefront/README.md) for direct Node setup and demo boundaries.
Accounts, administration and durable Java repositories are the next milestone.

ShopLink is a Systems Integration portfolio project that connects six independent
Java services through REST, JSON, and ActiveMQ/JMS. It validates customer orders,
reserves stock, simulates payments, creates deliveries, and records notifications.

## Run the complete system

Install Docker Desktop with Linux containers and Docker Compose v2, then run:

```powershell
docker compose up --build -d --wait --wait-timeout 240
./scripts/demo.ps1
```

The demo creates an order, waits for payment and delivery, checks notifications,
and verifies the stock deduction. Allow about 4 GB for Docker and the builds.
All published ports bind to localhost. See the [operations runbook](docs/operations.md)
for declines, outages, clean resets, and troubleshooting.

The generated narrated walkthrough is available locally at
`demo/generated/ShopLink-demo.mp4` (8 minutes 32 seconds). See the
[demo guide](docs/demo.md) to regenerate it from new API evidence.

| Service | Port | Main endpoints |
|---|---|---|
| Product | 8080 | `GET /products`, `GET /products/{id}` |
| Order | 8081 | `POST /orders`, `GET /orders/{id}` |
| Inventory | 8082 | `GET /inventory/{productId}`, `POST /inventory/reservations` |
| Payment | 8083 | `POST /payments`, `GET /payments/orders/{orderId}` |
| Delivery | 8084 | `GET /deliveries/orders/{orderId}` |
| Notification | 8085 | `GET /notifications/{orderId}` |
| ActiveMQ | 61616 / 8161 | JMS / [local web console](http://localhost:8161/admin) |

Health is available at `/actuator/health` on every service. ActiveMQ uses development
credentials `admin` / `admin`; this local demo is not configured for public hosting.

## Place an order

```powershell
$body = '{"customerId":"C001","items":[{"productId":"P1002","quantity":3}]}'
$order = Invoke-RestMethod http://127.0.0.1:8081/orders -Method Post -ContentType application/json -Body $body
Invoke-RestMethod "http://127.0.0.1:8081/orders/$($order.id)"
```

Creation returns `201` with an ID and `PAYMENT_PENDING`. Poll the order until
`PAID` or `PAYMENT_FAILED`. Product prices determine the payment total; the client
does not supply a price. Successful payments create one delivery and two notices.
Declined payments restore stock and create a payment-failure notice.

## Architecture and reliability

Order Service calls Product and Inventory over HTTP. It sends `ORDER_CREATED` to
Payment over JMS. The payment result updates the order; approved orders send
`ORDER_PAID` to Delivery. Notification records order-status and delivery events.
Each event path has a separate queue with a validated JSON contract.

Implemented safeguards include atomic multi-product reservations, overflow
protection, idempotent consumers, payment amount/currency checks, bounded HTTP
timeouts, transacted JMS listeners, automatic publication/release retries, and
bounded redelivery followed by `ActiveMQ.DLQ` for poison messages.

Application state and retry buffers remain in memory. A broker volume persists
messages, but application restarts do not preserve orders or inventory. Run one
instance per service and reset the complete demo between independent scenarios.
This is a portfolio demonstration, with simulated payments and notifications;
production durability, authentication, and real payment/email providers are outside
its scope. The [runbook](docs/operations.md) explains crash and timeout limits.

## Build without Docker

Install Java 17 or newer and Maven 3.9 or newer:

<<<<<<< HEAD
```powershell
mvn clean package
=======
- Unique order identifiers and timestamps.
- Message queues to buffer incoming work.
- Multiple consumers for parallel processing where appropriate.
- Safe database operations to prevent overselling limited stock.
- Order status tracking and error handling.

A queue can help manage an incoming workload, but it does not automatically guarantee that every order will finish in arrival order when multiple consumers process messages concurrently. ShopLink will distinguish message arrival order from processing completion order.

##  Planned Technology Stack

| Technology | Purpose |
|---|---|
| Java | Main programming language |
| Maven | Dependency and build management |
| REST / HTTP | Communication between services |
| JSON | Data exchange format |
| JMS | Java messaging interfaces |
| Apache ActiveMQ | Message broker |
| JUnit | Automated testing |
| Docker | Running services and the message broker in containers |
| Git and GitHub | Version control and project documentation |

Additional technologies will be introduced when they are needed.

##  Development Roadmap

The project will be developed incrementally, with regular commits documenting implementation decisions and progress.

- [X] **Phase 1:** Create the project structure and document the architecture.
- [X] **Phase 2:** Implement basic REST endpoints and JSON communication.
- [X] **Phase 3:** Implement in-memory inventory reservations and connect the Order flow.
- [X] **Phase 4:** Add the Payment Service and order-processing workflow.
- [X] **Phase 5:** Integrate ActiveMQ/JMS messaging.
- [X] **Phase 6:** Add Delivery and Notification consumers.
- [X] **Phase 7:** Handle concurrent orders and protect stock consistency.
- [ ] **Phase 8:** Add tests, retries, error handling, and message-processing safeguards.
- [ ] **Phase 9:** Add Docker support and complete the documentation.
- [ ] **Phase 10:** Record a 5–10 minute demonstration of the completed system.

##  Testing Strategy

Testing will cover:

- REST endpoint behaviour.
- JSON request and response handling.
- Communication between services.
- Message publishing and consumption.
- Order status transitions.
- Concurrent stock reservations.
- Failure handling and duplicate message processing.

##  Learning Goals

Through ShopLink, I aim to strengthen my understanding of:

- Distributed systems and microservice-style architecture.
- RESTful API design.
- JSON serialization and deserialization.
- JMS, queues, topics, and asynchronous communication.
- Concurrent processing and data consistency.
- Integration testing and reliable message processing.

##  Project Status

**Status:** Phase 5 is implemented. Order Service reserves stock and publishes JSON order events to ActiveMQ. Payment Service consumes them and publishes payment results; Order Service updates status and releases stock for declines asynchronously. See [messaging setup and examples](docs/messaging.md).

Features listed in the roadmap are planned and may not yet be implemented.

## Planned Project Structure

The following tree shows the intended layout. We will create service files as each part is implemented; generated Maven `target/` folders are omitted.

```
shoplink/
├── README.md
├── pom.xml
├── .gitignore
├── docs/
│   ├── architecture.md
│   └── decisions/
│       └── 001-service-boundaries.md
├── common/
│   ├── README.md
│   ├── pom.xml
│   ├── docker-compose.yml
│   └── src/
│       ├── main/java/za/co/shoplink/common/
│       │   ├── events/
│       │   │   ├── OrderCreatedEvent.java
│       │   │   ├── PaymentProcessedEvent.java
│       │   │   └── DeliveryRequestedEvent.java
│       │   └── messaging/
│       │       └── EventEnvelope.java
│       └── test/java/za/co/shoplink/common/events/
│           └── EventSerializationTest.java
├── product-service/
│   ├── README.md
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/za/co/shoplink/product/
│       │   │   ├── ProductApplication.java
│       │   │   ├── controller/ProductController.java
│       │   │   ├── service/ProductService.java
│       │   │   ├── repository/ProductRepository.java
│       │   │   ├── model/Product.java
│       │   │   ├── dto/
│       │   │   │   ├── ProductRequest.java
│       │   │   │   └── ProductResponse.java
│       │   │   └── exception/ProductNotFoundException.java
│       │   └── resources/application.properties
│       └── test/java/za/co/shoplink/product/
│           ├── service/ProductServiceTest.java
│           └── controller/ProductControllerTest.java
├── inventory-service/
│   ├── README.md
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/za/co/shoplink/inventory/
│       │   │   ├── InventoryApplication.java
│       │   │   ├── controller/InventoryController.java
│       │   │   ├── service/InventoryService.java
│       │   │   ├── repository/InventoryRepository.java
│       │   │   ├── model/StockItem.java
│       │   │   ├── dto/
│       │   │   │   ├── StockCheckResponse.java
│       │   │   │   └── StockReservationRequest.java
│       │   │   └── exception/InsufficientStockException.java
│       │   └── resources/application.properties
│       └── test/java/za/co/shoplink/inventory/
│           ├── service/InventoryServiceTest.java
│           └── controller/InventoryControllerTest.java
├── order-service/
│   ├── README.md
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/za/co/shoplink/order/
│       │   │   ├── OrderApplication.java
│       │   │   ├── controller/OrderController.java
│       │   │   ├── service/OrderService.java
│       │   │   ├── repository/OrderRepository.java
│       │   │   ├── model/
│       │   │   │   ├── Order.java
│       │   │   │   ├── OrderItem.java
│       │   │   │   └── OrderStatus.java
│       │   │   ├── dto/
│       │   │   │   ├── CreateOrderRequest.java
│       │   │   │   └── OrderResponse.java
│       │   │   ├── client/
│       │   │   │   ├── ProductServiceClient.java
│       │   │   │   └── InventoryServiceClient.java
│       │   │   └── messaging/PaymentProcessedListener.java
│       │   └── resources/application.properties
│       └── test/java/za/co/shoplink/order/
│           ├── service/OrderServiceTest.java
│           └── controller/OrderControllerTest.java
├── payment-service/
│   ├── README.md
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/za/co/shoplink/payment/
│       │   │   ├── PaymentApplication.java
│       │   │   ├── service/PaymentService.java
│       │   │   ├── repository/PaymentRepository.java
│       │   │   ├── model/
│       │   │   │   ├── Payment.java
│       │   │   │   └── PaymentStatus.java
│       │   │   ├── messaging/
│       │   │   │   ├── OrderCreatedListener.java
│       │   │   │   └── PaymentProcessedPublisher.java
│       │   │   └── dto/PaymentResult.java
│       │   └── resources/application.properties
│       └── test/java/za/co/shoplink/payment/service/PaymentServiceTest.java
├── delivery-service/
│   ├── README.md
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/za/co/shoplink/delivery/
│       │   │   ├── DeliveryApplication.java
│       │   │   ├── service/DeliveryService.java
│       │   │   ├── repository/DeliveryRepository.java
│       │   │   ├── model/
│       │   │   │   ├── Delivery.java
│       │   │   │   └── DeliveryStatus.java
│       │   │   └── messaging/
│       │   │       ├── DeliveryRequestedListener.java
│       │   │       └── DeliveryCreatedPublisher.java
│       │   └── resources/application.properties
│       └── test/java/za/co/shoplink/delivery/service/DeliveryServiceTest.java
├── notification-service/
│   ├── README.md
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/za/co/shoplink/notification/
│       │   │   ├── NotificationApplication.java
│       │   │   ├── service/NotificationService.java
│       │   │   └── messaging/
│       │   │       ├── OrderStatusListener.java
│       │   │       └── PaymentProcessedListener.java
│       │   └── resources/application.properties
│       └── test/java/za/co/shoplink/notification/service/NotificationServiceTest.java
└── integration-tests/
    ├── pom.xml
    └── src/
        ├── main/java/
        └── test/java/za/co/shoplink/integration/
            └── ShopLinkIntegrationTest.java
>>>>>>> 06b2bff313488b39abfe5428335d6d78d87eae83
```

Executable jars are produced as `*-exec.jar`. To run services individually, first
run `mvn install`, start `docker compose up -d activemq`, then run
`mvn -f <service>/pom.xml spring-boot:run` in a terminal for each service.
GitHub Actions checks the Java build, storefront syntax and Compose configuration.

<<<<<<< HEAD
Service URLs and broker settings can be changed through `PRODUCT_SERVICE_URL`,
`INVENTORY_SERVICE_URL`, `ACTIVEMQ_BROKER_URL`, `ACTIVEMQ_USER`, and `ACTIVEMQ_PASSWORD`.

## Documentation

- [Architecture and service ownership](docs/architecture.md)
- [Messaging contracts and setup](docs/messaging.md)
- [Concurrency guarantees](docs/concurrency.md)
- [Operations, retries, DLQ, and reset procedures](docs/operations.md)
- [5–10 minute demo outline](docs/demo.md)
- [Completed verification](docs/verification.md)

## Development roadmap

- [X] **Phase 1:** Project structure and architecture.
- [X] **Phase 2:** REST endpoints and JSON communication.
- [X] **Phase 3:** Inventory reservations and order flow.
- [X] **Phase 4:** Payment Service and order workflow.
- [X] **Phase 5:** ActiveMQ/JMS messaging.
- [X] **Phase 6:** Delivery and Notification consumers.
- [X] **Phase 7:** Concurrent orders and stock consistency.
- [X] **Phase 8:** Retries, error handling, and message safeguards.
- [X] **Phase 9:** Docker setup, CI, and documentation.
- [X] **Phase 10:** Produce the demonstration video: an 8:32 narrated walkthrough using captured API responses, with subtitles and reproducible generation scripts.

## Repository layout

`common/` holds shared events and JMS configuration. Each `*-service/` module owns
its API and business behavior. `storefront/` contains the jewellery website;
`scripts/` contains the demo; `docs/` contains design and operation notes. The root
Dockerfile builds any service, and `docker-compose.yml` starts the full system.
=======
Product, Inventory, Order, and Payment services provide the Phase 5 flow. Start ActiveMQ and install shared modules before starting the services individually. See [Phase 5 messaging guide](docs/messaging.md) for commands, event contracts, verification, and failure limitations. Delivery, Notification, and durable application persistence remain planned.

WTC-T8AR98NB
>>>>>>> 06b2bff313488b39abfe5428335d6d78d87eae83
