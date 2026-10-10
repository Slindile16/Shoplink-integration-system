# ShopLink Product Service

The Product Service will provide product information, including product names and prices, to other ShopLink services and clients over HTTP.

## Current setup

This module inherits shared Java and Spring Boot settings from the root `pom.xml`. Its Spring Boot web dependency provides the foundation for REST endpoints and JSON request and response handling.

The service does not have an application class or REST endpoints yet. Those will be added incrementally as Phase 2 continues.

## Planned API

The initial API will provide endpoints to:

- List available products.
- Retrieve a product by its ID.

Request and response formats, endpoint paths, and error responses will be documented as each endpoint is implemented.

## Planned structure

```text
product-service/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/za/co/shoplink/product/
        └── resources/
```
