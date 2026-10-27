# ShopLink — Online Store Integration System

**ShopLink** is an online store integration system designed to demonstrate how independent software services communicate to process customer orders reliably. The project focuses on RESTful APIs, JSON data exchange, asynchronous messaging, and handling concurrent orders.

This project is being developed as an original **Systems Integration** portfolio project.

##  Project Objective

An online order involves several processes, including checking product availability, processing payments, managing stock, arranging delivery, and notifying customers.

ShopLink aims to connect these processes through independent services so that they can communicate efficiently without relying on one large, tightly coupled application.

The goal is to build a system that can manage multiple incoming orders, coordinate communication between services, and handle failures safely.

##  Planned Architecture

```text
                 Customer
                    |
                    v
                Order Service
                    |
          +---------+---------+
          |                   |
          v                   v
   Inventory Service     Payment Service
          |                   |
          |                   v
          |             Order-Paid Event
          |                   |
          |              ActiveMQ/JMS
          |                   |
          |          +--------+--------+
          |          |                 |
          v          v                 v
      Stock       Delivery         Notification
     Updates      Service           Service
```

The architecture will evolve as the project is developed.

##  Planned Services

| Service | Responsibility |
|---|---|
| **Product Service** | Provides product information and prices. |
| **Order Service** | Creates orders, assigns unique order IDs, and tracks order status. |
| **Inventory Service** | Tracks available stock and safely reserves products. |
| **Payment Service** | Simulates payment processing and publishes payment events. |
| **Delivery Service** | Processes delivery requests for successful orders. |
| **Notification Service** | Simulates customer notifications about order and payment updates. |

##  Systems Integration Concepts

### 1. RESTful APIs

Services will use HTTP requests to communicate when an immediate response is needed.

For example, the Order Service can request product availability from the Inventory Service.

### 2. JSON Serialization and Deserialization

Services will exchange structured JSON messages. The application will convert data between Java objects and JSON representations.

Example order payload:

```json
{
  "orderId": "ORD-1001",
  "customerId": "C001",
  "status": "PROCESSING",
  "items": [
    {
      "productId": "P100",
      "quantity": 2
    }
  ]
}
```

### 3. Asynchronous Messaging with JMS and ActiveMQ

ShopLink will use Java Message Service (JMS) interfaces with ActiveMQ to support communication that does not require the sender to wait for every downstream service.

Planned messaging patterns include:

- **Topics:** Publish events such as `ORDER_PAID` for multiple interested subscribers.
- **Queues:** Buffer work such as delivery requests until a consumer can process it.

### 4. Concurrent Order Processing

The system will be designed to handle multiple orders arriving at approximately the same time.

Planned considerations include:

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
```


Product, Inventory, Order, and Payment services provide the Phase 5 flow. Start ActiveMQ and install shared modules before starting the services individually. See [Phase 5 messaging guide](docs/messaging.md) for commands, event contracts, verification, and failure limitations. Delivery, Notification, and durable application persistence remain planned.

WTC-T8AR98NB
