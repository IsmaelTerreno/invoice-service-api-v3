# Docker Configuration

This directory contains Docker-related files for building and deploying the Invoice Service API.

## Files

- **Dockerfile** - Multi-stage Docker build configuration
- **docker-compose.yml** - Local development with Docker Compose
- **build-prod.sh** - Production build and push script
- **docker.env** - Local development environment variables (for docker-compose)
- **docker-prod.env** - Production environment variables (for build-prod.sh)

## Environment Files

### docker.env (Local Development)
Used by `docker-compose.yml` for local development with Docker Compose.

```bash
# Edit docker.env with your local settings
docker-compose up
```

### docker-prod.env (Production)
Used by `build-prod.sh` for building and pushing production images.

**Setup:**
```bash
# 1. Copy the example file
cp docker-prod.env.example docker-prod.env

# 2. Edit docker-prod.env with production credentials
nano docker-prod.env

# 3. Build and push to registry
./build-prod.sh
```

**⚠️ SECURITY WARNING:**
- Never commit `docker-prod.env` to version control
- Use strong, unique passwords for all credentials
- Use production Stripe keys (sk_live_*) only in production
- Keep JWT secrets at least 256 bits (64 base64 characters)

## Environment Variables Reference

All environment variables used in the Docker build:

| Variable | Description | Example |
|----------|-------------|---------|
| `SERVER_PORT_LISTENING` | Application port | 3120 |
| `CORS_ALLOW_ORIGINS` | CORS allowed origins | https://yourdomain.com |
| `HOST_DB_CONFIG` | PostgreSQL host | db.example.com |
| `PORT_DB_CONFIG` | PostgreSQL port | 5432 |
| `DATABASE_NAME_DB_CONFIG` | Database name | remotejob |
| `USER_NAME_DB_CONFIG` | Database username | postgres |
| `USER_PASSWORD_DB_CONFIG` | Database password | secure_password |
| `JWT_SECRET_ACCESS` | JWT access token secret (base64) | (64+ chars) |
| `JWT_SECRET_REFRESH` | JWT refresh token secret (base64) | (64+ chars) |
| `JWT_URL_ENDPOINT` | Auth service URL | https://auth.example.com |
| `TEST_USER_NAME` | Test user name | testuser |
| `TEST_USER_EMAIL` | Test user email | test@example.com |
| `TEST_USER_PASSWORD` | Test user password | testpass |
| `RABBITMQ_HOST` | RabbitMQ host | rabbitmq.example.com |
| `RABBITMQ_PORT` | RabbitMQ port | 5672 |
| `RABBITMQ_USERNAME` | RabbitMQ username | admin |
| `RABBITMQ_PASSWORD` | RabbitMQ password | secure_password |
| `INVOICE_STATUS_ON_RELATED_PLANS_RABBITMQ_QUEUE_NAME` | Queue name | invoice-status-on-related-plans |
| `NOTIFICATION_EVENTS_RABBITMQ_QUEUE_NAME` | Queue name | notification-events |
| `STRIPE_SECRET_KEY` | Stripe API key | sk_live_... or sk_test_... |
| `STRIPE_ENDPOINT_SECRET` | Stripe webhook secret | whsec_... |

## Verification Checklist

Before deploying, verify all environment variables are correctly configured:

### Build Script (build-prod.sh)
✅ All variables in `build-prod.sh` match `docker-prod.env`

### Dockerfile
✅ All ARG declarations in Dockerfile match build-prod.sh build args
✅ All ENV declarations in Dockerfile match ARG declarations

### Application Properties
✅ All `${VARIABLE}` references in `application.properties` have corresponding ENV in Dockerfile

## Status: Verified ✓

All environment variables have been verified across:
- ✅ build-prod.sh
- ✅ Dockerfile (ARG and ENV declarations)
- ✅ application.properties
- ✅ docker-prod.env.example

No missing environment variables detected.

## Building for Production

```bash
# 1. Setup production environment file
cp docker-prod.env.example docker-prod.env
nano docker-prod.env

# 2. Login to Docker registry
docker login registry.digitalocean.com

# 3. Build and push
cd docker
./build-prod.sh
```

## Local Development

```bash
# Start all services (PostgreSQL, RabbitMQ, Invoice Service)
cd docker
docker-compose up

# Stop all services
docker-compose down

# View logs
docker-compose logs -f invoice-service-api
```

## Troubleshooting

### Build fails with "Could not resolve placeholder"
- Check that all variables in `docker-prod.env` are defined
- Ensure there are no typos in variable names
- Verify the env file is in the correct location

### Image won't start
- Check Docker logs: `docker logs <container-id>`
- Verify database and RabbitMQ are accessible
- Check that Stripe keys are valid

### Permission denied on build-prod.sh
```bash
chmod +x build-prod.sh
```

## Generating Secure Secrets

### JWT Secrets (base64, 256+ bits)
```bash
openssl rand -base64 64
```

### Database Password
```bash
openssl rand -base64 32
```

### RabbitMQ Password
```bash
openssl rand -base64 32
```

## Security Best Practices

1. **Never commit secrets** - Keep `docker-prod.env` out of version control
2. **Rotate secrets regularly** - Change passwords and keys periodically
3. **Use separate environments** - Different credentials for dev/staging/prod
4. **Restrict CORS** - Don't use `*` in production
5. **Monitor access** - Track who has access to production credentials
6. **Use secret management** - Consider using Vault, AWS Secrets Manager, etc.
