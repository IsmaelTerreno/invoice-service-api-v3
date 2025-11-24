# Invoice Service API Migration - v2 to v3

This document describes the migration from the NestJS-based invoice-service-api-v2 to the Spring Boot-based invoice-service-api-v3.

## Overview

The invoice service has been successfully migrated from:
- **Source**: NestJS (TypeScript) application
- **Target**: Spring Boot 3.3.3 (Java 22) application

## Key Changes

### Architecture
- **Framework**: NestJS → Spring Boot 3.3.3
- **Language**: TypeScript → Java 22
- **ORM**: TypeORM → Spring Data JPA with Hibernate
- **Package Structure**: `com.remotejob.planservice` → `com.remotejob.invoiceservice`

### Dependencies
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter AMQP (RabbitMQ)
- PostgreSQL Driver
- Stripe Java SDK (v24.14.0)
- MapStruct (v1.5.5.Final) for DTO mapping
- Lombok for reducing boilerplate
- SpringDoc OpenAPI for API documentation

### Database Schema
The `Invoice` entity maintains the same structure:
- `id` (UUID) - Primary key
- `userId` (String) - User identifier
- `customerId` (String) - Stripe customer ID
- `customerEmail` (String) - Customer email
- `items` (JSONB) - Invoice items
- `subscriptionId` (String) - Stripe subscription ID
- `status` (String) - Invoice status
- `lastPaymentIntentId` (String) - Last payment intent ID
- `invoiceIdProvidedByStripe` (String) - Stripe invoice ID
- `createdAt` (Timestamp) - Creation timestamp
- `updatedAt` (Timestamp) - Last update timestamp

### API Endpoints

All endpoints are prefixed with `/api/v1/invoice`:

1. **GET /all** - Retrieve all invoices
2. **POST /create-subscription** - Create a new subscription in Stripe and save invoice
3. **POST /stripe_webhooks** - Handle Stripe webhook events

### Business Logic Migration

#### InvoiceService
Migrated business logic includes:
- Creating subscriptions via Stripe
- Saving/updating invoices in the database
- Processing Stripe webhook events:
  - `invoice.created`
  - `invoice.updated`
  - `payment_intent.succeeded`
- Publishing messages to RabbitMQ queues:
  - Invoice status updates
  - Plan creation events
  - User notifications

#### StripeService
Handles all Stripe operations:
- Creating customers
- Creating subscriptions
- Confirming payment intents
- Constructing and validating webhook events

#### RabbitMQService
Manages RabbitMQ messaging:
- Sending messages to queues
- Event pattern routing
- Queue configuration

### Configuration

#### Environment Variables
Key environment variables required:

**Server**:
- `SERVER_PORT_LISTENING` (default: 3120)
- `CORS_ALLOW_ORIGINS`

**Database**:
- `HOST_DB_CONFIG`
- `PORT_DB_CONFIG` (default: 5433)
- `DATABASE_NAME_DB_CONFIG`
- `USER_NAME_DB_CONFIG`
- `USER_PASSWORD_DB_CONFIG`

**Stripe**:
- `STRIPE_SECRET_KEY`
- `STRIPE_ENDPOINT_SECRET`

**RabbitMQ**:
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `INVOICE_STATUS_ON_RELATED_PLANS_RABBITMQ_QUEUE_NAME`
- `NOTIFICATION_EVENTS_RABBITMQ_QUEUE_NAME`

**JWT**:
- `JWT_SECRET_ACCESS`
- `JWT_SECRET_REFRESH`
- `JWT_URL_ENDPOINT`

### Docker Configuration

#### Docker Compose Services
1. **invoice-service-api**: Main application (port 3120)
2. **invoice-service-postgres**: PostgreSQL database (port 5433)
3. **rabbitmq**: RabbitMQ with management UI (ports 5672, 15672)

### Running the Application

#### Local Development
```bash
# Using Maven
./mvnw spring-boot:run

# Or with Docker Compose
cd docker
docker-compose up --build
```

#### Building
```bash
# Maven build
./mvnw clean package

# Docker build
docker build -f docker/Dockerfile -t invoice-service-api:latest .
```

### API Documentation

Swagger UI is available at: `http://localhost:3120/doc`

## Migration Checklist

- [x] Rename package structure from `planservice` to `invoiceservice`
- [x] Create Invoice entity with proper JPA annotations
- [x] Create DTOs (InvoiceDto, SubscriptionDto, NotificationDto)
- [x] Create InvoiceRepository interface
- [x] Migrate StripeService with Stripe SDK integration
- [x] Migrate InvoiceService with all business logic
- [x] Create InvoiceMapper using MapStruct
- [x] Create InvoiceController with REST endpoints
- [x] Update RabbitMQ configuration and event DTOs
- [x] Create RabbitMQService for messaging
- [x] Update application.properties
- [x] Update Docker configuration files
- [x] Delete Plan-related files
- [x] Rename main application class

## Testing

The application includes:
- Unit test structure in place
- Integration test configuration ready
- Docker Compose for end-to-end testing

## Notes

- Port changed from 3090 (plan service) to 3120 (invoice service)
- Database port changed from 1090 to 5433 to match v2 configuration
- All Stripe webhook handlers maintain the same functionality
- RabbitMQ event patterns remain compatible with existing services
- Security configuration inherited from the base project
