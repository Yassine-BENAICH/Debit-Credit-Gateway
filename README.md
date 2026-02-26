# Debit-Credit-Gateway

Debit-Credit-Gateway is a Spring Boot–based ISO 8583 payment gateway for processing debit and credit transactions over TCP.  
It exposes a RESTful API that accepts high-level JSON transaction requests, converts them into ISO 8583 messages, sends them to a remote host over a configurable TCP connection, and maps the ISO 8583 responses back into JSON.

The project is designed as a reference implementation or starting point for building real-time card transaction gateways, reversals, and transaction status queries.

---

## Features

- RESTful API for:
  - Processing debit and credit transactions (sync and async)
  - Reversing previous transactions
  - Checking transaction status
  - Health check endpoint
- ISO 8583 message construction using jPOS and a configurable packager
- TCP client with connection pooling, retries, exponential backoff, and configurable timeouts
- Validation of incoming requests with detailed constraints
- Structured transaction logging, including ISO 8583 message logging
- Error handling with standardized response codes
- Ready to integrate with PostgreSQL (data source preconfigured, persistence hooks can be added)

---

## Technology Stack

- **Language:** Java 21
- **Framework:** Spring Boot 2.7.x
- **Messaging:** ISO 8583 via jPOS
- **Build Tool:** Maven
- **Database :** Oracle
- **Logging:** Log4j2

---

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 21 installed and on your PATH
- Maven 3.6+ installed
- Oracle
  - Default URL: `jdbc:postgresql://localhost:5432/gateway`
  - Default credentials: `Admin` / `Admin123`
- An ISO 8583 host or simulator reachable over TCP (default `127.0.0.1:5000`)

### Clone the Repository

```bash
git clone <your-repo-url>.git
cd Debit-Credit-Gateway/Debit-Credit-Gateway
```

### Build the Project

```bash
mvn clean install
```

This command compiles the project, runs tests, and builds the JAR artifact.

### Run the Application

```bash
mvn spring-boot:run
```

The application starts on port **8080** by default.

Alternatively, you can run the generated JAR:

```bash
java -jar target/debit-credit-gateway-1.0.0.jar
```

---

## Configuration

Application-level configuration is defined in  
[`src/main/resources/application.properties`](file:///c:/Users/admin/Documents/trae_projects/Debit-Credit-Gateway/Debit-Credit-Gateway/src/main/resources/application.properties).

### Spring Application & Server

```yaml
spring:
  application:
    name: direct-debit-gateway

server:
  port: 8080
```

### Database 
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gateway
    username: Admin
    password: Admin123

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

> Note: The project currently does not persist transactions to the database, but the datasource is preconfigured so you can easily add repositories and entities.

### TCP Gateway Settings

TCP settings are bound to the `TcpConfig` class via `gateway.tcp` properties:

```yaml
gateway:
  tcp:
    host: 127.0.0.1
    port: 5000
    connection-timeout: 30000
    read-timeout: 30000
    max-connections: 10
    min-connections: 2
    keep-alive: true
    tcp-no-delay: true
    retry-attempts: 3
    retry-delay: 1000
    max-retry-delay: 10000
    connection-pool-timeout: 5000
    length-header: true
    length-header-size: 2
    encoding: UTF-8
```

The default `application.properties` includes only:

```yaml
tcp:
  host: 127.0.0.1
  port: 5000
```

You can override or extend the configuration with `gateway.tcp.*` properties as above.

---

## REST API

Base path: `/api/v1/transactions`

### 1. Process Transaction (Synchronous)

- **URL:** `POST /api/v1/transactions/process`
- **Description:** Processes a debit/credit transaction and returns the response synchronously.
- **Request Body (JSON):**

```json
{
  "cardNumber": "4111111111111111",
  "transactionType": "DEBIT",
  "amount": 100.00,
  "currencyCode": "USD",
  "terminalId": "12345678",
  "merchantId": "876543210123456",
  "merchantName": "Test Merchant",
  "posEntryMode": "05",
  "merchantCategoryCode": "541",
  "cardExpiryDate": "2512",
  "cvv": "123",
  "invoiceNumber": "INV000001",
  "fromAccount": "4000000000000000",
  "toAccount": "5000000000000000",
  "description": "Sample purchase"
}
```

#### Required Fields

- `cardNumber`: 16–19 digits
- `transactionType`: one of `DEBIT`, `CREDIT`, `REFUND`, `REVERSAL`
- `amount`: between `0.01` and `999999.99`
- `currencyCode`: 3 uppercase letters (e.g., `USD`, `EUR`)
- `terminalId`: 8 characters
- `merchantId`: 15 characters
- `merchantName`: up to 40 characters
- `posEntryMode`: 2-digit string

Other fields are optional and used for ISO 8583 enrichment where applicable.

#### Response (JSON)

```json
{
  "transactionId": "b3a5...",
  "rrn": "123456789012",
  "stan": "123456",
  "authCode": "123456",
  "responseCode": "00",
  "responseMessage": "Approved",
  "amount": 100.00,
  "currencyCode": "USD",
  "terminalId": "12345678",
  "merchantId": "876543210123456",
  "cardNumber": "4111111111111111",
  "maskedCardNumber": "411111******1111",
  "transactionType": "DEBIT",
  "transactionDate": "2026-02-23T10:15:30.123",
  "approved": true,
  "status": "SUCCESS",
  "processingTime": 120,
  "hostResponseCode": "00"
}
```

- HTTP 200 when `approved == true`
- HTTP 400 when the transaction fails (validation or host response)

### 2. Process Transaction (Asynchronous)

- **URL:** `POST /api/v1/transactions/process/async`
- **Description:** Processes a transaction asynchronously. The endpoint immediately returns a `CompletableFuture<ResponseEntity<TransactionResponse>>` which resolves when processing completes.
- **Request Body:** Same as synchronous `/process`.
- **Response:** Same structure as synchronous endpoint; use for clients that can handle async workflows.

### 3. Reverse Transaction

- **URL:** `POST /api/v1/transactions/reverse`
- **Description:** Sends a reversal request for a previous transaction.
- **Query Parameters:**
  - `rrn`: original Retrieval Reference Number
  - `stan`: original STAN

Example:

```bash
curl -X POST "http://localhost:8080/api/v1/transactions/reverse?rrn=123456789012&stan=123456"
```

- HTTP 200 if reversal succeeds
- HTTP 400 if reversal fails or original transaction is not found

### 4. Get Transaction Status

- **URL:** `GET /api/v1/transactions/status/{rrn}`
- **Description:** Retrieves status for a transaction by RRN.
- **Example:**

```bash
curl "http://localhost:8080/api/v1/transactions/status/123456789012"
```

> Note: In this reference implementation, status is not persisted and a mock response is returned with `responseCode = "00"` if the request is valid.

### 5. Health Check

- **URL:** `GET /api/v1/transactions/health`
- **Description:** Simple health check endpoint.
- **Response:** `"Debit/Credit Gateway is running"`

---

## ISO 8583 Mapping Overview

The mapping logic is implemented in  
[`Iso8583Converter`](file:///c:/Users/admin/Documents/trae_projects/Debit-Credit-Gateway/Debit-Credit-Gateway/src/main/java/com/gateway/service/Iso8583Converter.java).

Key mappings include:

- Field 2 – PAN: `cardNumber`
- Field 3 – Processing Code: derived from `transactionType`, `fromAccount`, `toAccount`
- Field 4 – Amount: scaled to minor units (e.g., cents)
- Field 7 – Transmission date/time
- Field 11 – STAN
- Field 37 – RRN
- Field 41 – Terminal ID
- Field 42 – Merchant ID
- Field 43 – Merchant name/location
- Field 49 – Currency code
- Field 60 / 62 / 102 / 103 – Additional transaction data as applicable

Responses map ISO 8583 field 39 (response code) to `ResponseCode` enum and to `TransactionResponse`.

---

## TCP Communication

The TCP communication layer is implemented in  
[`IsoTcpClient`](file:///c:/Users/admin/Documents/trae_projects/Debit-Credit-Gateway/Debit-Credit-Gateway/src/main/java/com/gateway/tcp/IsoTcpClient.java) and configured via `TcpConfig`.

Highlights:

- Connection pool with configurable min/max connections
- Length-header based framing (default 2 bytes)
- Read and connect timeouts
- Exponential backoff retry on IO errors
- Graceful shutdown of connections on application stop

Make sure an ISO 8583 host or simulator is running and reachable under the configured `gateway.tcp.host` and `gateway.tcp.port`.

---

## Troubleshooting

### Build or Java Version Issues

- Ensure JDK 21 is installed and used:

```bash
java -version
```

- If Maven reports class file version mismatch:
  - Clean your local repository for conflicting libraries or adjust versions in `pom.xml`.

### TCP Connection Errors

- Check that your ISO 8583 host is running and reachable (e.g., via `telnet host port`).
- Verify `gateway.tcp.host` and `gateway.tcp.port` in `application.properties`.
- Increase `connection-timeout` and `read-timeout` if your host is slow to respond.

### ISO 8583 Format Errors

- Inspect logs for full ISO 8583 messages (logged by `Iso8583Util.logISOMsg`).
- Adjust the packager configuration in  
  [`src/main/resources/iso8583/iso-packager.xml`](file:///c:/Users/admin/Documents/trae_projects/Debit-Credit-Gateway/Debit-Credit-Gateway/src/main/resources/iso8583/iso-packager.xml).

### Validation Errors

- If the API returns HTTP 400 with validation messages, verify:
  - Card number length and characters
  - Supported transaction types
  - Currency code format
  - Terminal and merchant identifiers

---

## Contribution Guidelines

Contributions are welcome. To contribute:

1. Fork the repository.
2. Create a feature branch:

   ```bash
   git checkout -b feature/your-feature-name
   ```

3. Make your changes following existing code style and patterns.
4. Add or update tests as appropriate.
5. Ensure the build passes:

   ```bash
   mvn clean install
   ```

6. Submit a pull request with a clear description of your changes and motivation.

---

## License

This project is provided under the MIT License.  
If a `LICENSE` file is not yet present, please add one before distributing or deploying the project in production environments.

---

## Contact

For questions, issues, or feature requests, please open an issue in the repository or contact the maintainers via your preferred channel.
