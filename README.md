# Invoice Service API v3

A Spring Boot microservice for managing invoices, subscriptions, and payment processing via Stripe.

## 🎯 Project Overview

This service has been migrated from NestJS (TypeScript) to Spring Boot 3.3.3 (Java 22), providing:
- Stripe payment integration for subscriptions
- Invoice management and persistence
- RabbitMQ messaging for event-driven architecture
- RESTful API with OpenAPI documentation
- Secure JWT-based authentication

## 🏗️ Architecture

- **Framework**: Spring Boot 3.3.3
- **Language**: Java 22
- **Database**: PostgreSQL with Hibernate/JPA
- **Messaging**: RabbitMQ (AMQP)
- **Payment**: Stripe API
- **Documentation**: SpringDoc OpenAPI
- **Containerization**: Docker & Docker Compose

## 📋 Prerequisites

- Java 22 or higher
- Maven 3.9+
- Docker & Docker Compose (for containerized deployment)
- PostgreSQL 15+ (for local development)
- RabbitMQ 3+ (for local development)

## 🚀 Quick Start

### Using Docker Compose (Recommended)

```bash
# Clone the repository
cd invoice-service-api-v3

# Start all services
cd docker
docker-compose up --build

# Access the API
# API: http://localhost:3120
# Swagger UI: http://localhost:3120/doc
# RabbitMQ Management: http://localhost:15672 (guest/guest)
```

### Local Development

1. **Configure Environment Variables**

```bash
# Copy the example env file
cp .env.example .env

# Edit .env with your configuration
```

2. **Start External Services**

```bash
# Start PostgreSQL
docker run -d \
  --name invoice-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=remotejob \
  -p 5433:5432 \
  postgres:latest

# Start RabbitMQ
docker run -d \
  --name invoice-rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

3. **Build and Run the Application**

```bash
# Using Maven
./mvnw clean install
./mvnw spring-boot:run

# Or build JAR and run
./mvnw clean package
java -jar target/invoice-service-api-*.jar
```

## 🧪 Running Tests

### All Tests
```bash
./mvnw test
```

### Specific Test Class
```bash
./mvnw test -Dtest=InvoiceServiceAPIApplicationTests
```

### With Coverage Report
```bash
./mvnw test jacoco:report
# Report: target/site/jacoco/index.html
```

### Using IntelliJ IDEA
1. Open the project in IntelliJ IDEA
2. Right-click on `src/test/java` folder
3. Select "Run 'All Tests'"

### Test Details
See [TEST_SUMMARY.md](TEST_SUMMARY.md) for comprehensive test documentation including:
- 18 end-to-end tests
- Carlos's implementation verification tests
- Test configuration and expected results

## 📚 API Documentation

Once the application is running, access the interactive API documentation:

**Swagger UI**: http://localhost:3120/doc

### Available Endpoints

#### Invoice Management
- `GET /api/v1/invoice/all` - Get all invoices
- `POST /api/v1/invoice/create-subscription` - Create a new subscription
- `POST /api/v1/invoice/stripe_webhooks` - Handle Stripe webhook events

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT_LISTENING` | Server port | 3120 |
| `HOST_DB_CONFIG` | Database host | localhost |
| `PORT_DB_CONFIG` | Database port | 5433 |
| `DATABASE_NAME_DB_CONFIG` | Database name | remotejob |
| `STRIPE_SECRET_KEY` | Stripe secret key | (required) |
| `STRIPE_ENDPOINT_SECRET` | Stripe webhook secret | (required) |
| `RABBITMQ_HOST` | RabbitMQ host | localhost |
| `RABBITMQ_PORT` | RabbitMQ port | 5672 |

See `.env.example` for a complete list of configuration options.

### Application Properties

Configuration files:
- `src/main/resources/application.properties` - Main configuration
- `src/test/resources/application-test.properties` - Test configuration

## 🐳 Docker

### Build Docker Image
```bash
docker build -f docker/Dockerfile -t invoice-service-api:latest .
```

### Run with Docker Compose
```bash
cd docker
docker-compose up -d

# View logs
docker-compose logs -f invoice-service-api

# Stop services
docker-compose down
```

## 🔄 Migration from v2

This project was migrated from NestJS (v2) to Spring Boot (v3). See [MIGRATION.md](MIGRATION.md) for:
- Detailed migration steps
- Architecture changes
- API compatibility notes
- Business logic mapping

## 📦 Project Structure

```
invoice-service-api-v3/
├── src/
│   ├── main/
│   │   ├── java/com/remotejob/invoiceservice/
│   │   │   ├── amqp/              # RabbitMQ configuration & listeners
│   │   │   ├── controller/        # REST controllers
│   │   │   ├── dto/               # Data Transfer Objects
│   │   │   ├── entity/            # JPA entities
│   │   │   ├── mapper/            # MapStruct mappers
│   │   │   ├── repository/        # Spring Data repositories
│   │   │   ├── security/          # Security configuration
│   │   │   ├── service/           # Business logic services
│   │   │   └── InvoiceServiceAPIApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/com/remotejob/invoiceservice/
│       │   └── InvoiceServiceAPIApplicationTests.java
│       └── resources/
│           └── application-test.properties
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── pom.xml
├── .env.example
├── README.md
├── MIGRATION.md
└── TEST_SUMMARY.md
```

## 🔐 Security

- JWT-based authentication
- Stripe webhook signature verification
- CORS configuration
- PostgreSQL with secure credentials
- Environment-based secrets management

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:3120/actuator/health
```

### Metrics
```bash
curl http://localhost:3120/actuator/metrics
```

## 🐛 Troubleshooting

### Application won't start
1. Check if port 3120 is available
2. Verify database connection settings
3. Ensure RabbitMQ is running
4. Check Stripe API keys are valid

### Tests failing
1. Ensure H2 dependency is present
2. Check test configuration in `application-test.properties`
3. Verify no port conflicts on random test port

### Docker issues
1. Clear Docker cache: `docker-compose down -v`
2. Rebuild: `docker-compose up --build`
3. Check logs: `docker-compose logs -f`

## 📝 Development

### Code Style
- Follow Java conventions
- Use Lombok for boilerplate reduction
- Document public APIs with JavaDoc
- Write tests for new features

### Building
```bash
# Clean build
./mvnw clean install

# Skip tests
./mvnw clean install -DskipTests

# Build Docker image
./mvnw clean package
docker build -f docker/Dockerfile -t invoice-service-api:latest .
```

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Add/update tests
4. Ensure all tests pass
5. Submit a pull request

## 📄 License

UNLICENSED - Private project

## 👤 Author

**Carlos** - Invoice Service Migration Engineer

## 📞 Support

For issues or questions:
1. Check [MIGRATION.md](MIGRATION.md) for migration-related questions
2. Check [TEST_SUMMARY.md](TEST_SUMMARY.md) for test documentation
3. Review Docker logs: `docker-compose logs`
4. Check application logs in the console

## 🎯 Key Features

✅ Stripe subscription management
✅ Invoice CRUD operations
✅ Webhook event processing
✅ RabbitMQ event publishing
✅ PostgreSQL persistence with JSONB support
✅ JWT authentication & authorization
✅ OpenAPI/Swagger documentation
✅ Docker containerization
✅ Comprehensive test suite
✅ Production-ready configuration

## 🔗 Related Services

- Plan Service API - Manages subscription plans
- Notification Service API - Handles user notifications
- User Service API - User management and authentication

## 📈 Version History

- **v3.0.0** (2025-11-24) - Spring Boot migration by Carlos
  - Migrated from NestJS to Spring Boot 3.3.3
  - Added comprehensive test suite
  - Updated Docker configuration
  - Enhanced documentation

- **v2.0.0** - NestJS implementation
  - Original TypeScript/NestJS version

---

**Last Updated**: 2025-11-24
**Version**: 3.0.0
**Spring Boot**: 3.3.3
**Java**: 22
