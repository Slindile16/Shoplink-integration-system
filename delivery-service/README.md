# ShopLink Delivery Service

Consumes `ORDER_PAID` JSON TextMessages from ActiveMQ, creates one in-memory delivery per order, and publishes `DELIVERY_CREATED` for Notification Service. Duplicate order-paid messages return the existing delivery. State resets when the service restarts.

`GET /deliveries/orders/{orderId}` returns the delivery or `404` if none exists. Start ActiveMQ, install shared modules with `mvn install`, then run `mvn -f delivery-service/pom.xml spring-boot:run`. Broker settings use `ACTIVEMQ_BROKER_URL`, `ACTIVEMQ_USER`, and `ACTIVEMQ_PASSWORD`.
