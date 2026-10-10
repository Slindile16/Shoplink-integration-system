# ShopLink Product Service

The Product Service will provide product information, including product names and prices, to other ShopLink services and clients over HTTP.

## Current setup

This module inherits shared Java and Spring Boot settings from the root `pom.xml`. Spring Boot and Jackson handle the HTTP endpoints and JSON serialization.

The first version uses three sample products held in memory. Prices are numeric rand amounts and each response includes the currency code `ZAR` (South African rand). Data resets when the service restarts.

## API

| Method | Path | Behaviour |
|---|---|---|
| `GET` | `/products` | Returns all sample products as a JSON array. |
| `GET` | `/products/{id}` | Returns the matching product as JSON, or `404 Not Found` if the ID does not exist. |

## Run the service

From the repository root, run:

```shell
mvn -f product-service/pom.xml spring-boot:run
```

The service listens on `http://localhost:8080` by default.

Example requests:

```shell
curl http://localhost:8080/products
curl http://localhost:8080/products/P1001
curl -i http://localhost:8080/products/unknown
```

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
