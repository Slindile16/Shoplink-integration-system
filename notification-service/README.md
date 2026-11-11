# ShopLink Notification Service

Consumes order status and delivery created JSON TextMessages from ActiveMQ and records in-memory notification events. Duplicate messages of the same type for an order are ignored. Notifications reset when the service restarts; this phase records notifications for inspection and does not send email or SMS.

`GET /notifications/{orderId}` returns the recorded notifications for an order. Start ActiveMQ, install shared modules with `mvn install`, then run `mvn -f notification-service/pom.xml spring-boot:run`. Broker settings use `ACTIVEMQ_BROKER_URL`, `ACTIVEMQ_USER`, and `ACTIVEMQ_PASSWORD`.
