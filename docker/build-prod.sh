#!/bin/bash
set -euo pipefail

# Constants
ENV_FILE="docker-prod.env"
REGISTRY_URL="registry.digitalocean.com/remote-job"
# Match compose service/image naming
SERVICE_NAME="plan-service-api"
DOCKERFILE="Dockerfile"
TAG="latest"
FULL_IMAGE_NAME="${REGISTRY_URL}/${SERVICE_NAME}:${TAG}"

# Load environment variables from file
load_env_variables() {
  export $(grep -v '^#' "$ENV_FILE" | xargs)
}

# Build the Docker image
build_docker_image() {
  docker build \
      --build-arg SERVER_PORT_LISTENING_ARG="$SERVER_PORT_LISTENING" \
      --build-arg CORS_ALLOW_ORIGINS_ARG="$CORS_ALLOW_ORIGINS" \
      --build-arg HOST_DB_CONFIG_ARG="$HOST_DB_CONFIG" \
      --build-arg PORT_DB_CONFIG_ARG="$PORT_DB_CONFIG" \
      --build-arg DATABASE_NAME_DB_CONFIG_ARG="$DATABASE_NAME_DB_CONFIG" \
      --build-arg USER_NAME_DB_CONFIG_ARG="$USER_NAME_DB_CONFIG" \
      --build-arg USER_PASSWORD_DB_CONFIG_ARG="$USER_PASSWORD_DB_CONFIG" \
      --build-arg JWT_SECRET_ACCESS_ARG="$JWT_SECRET_ACCESS" \
      --build-arg JWT_SECRET_REFRESH_ARG="$JWT_SECRET_REFRESH" \
      --build-arg JWT_URL_ENDPOINT_ARG="$JWT_URL_ENDPOINT" \
      --build-arg TEST_USER_NAME_ARG="$TEST_USER_NAME" \
      --build-arg TEST_USER_EMAIL_ARG="$TEST_USER_EMAIL" \
      --build-arg TEST_USER_PASSWORD_ARG="$TEST_USER_PASSWORD" \
      --build-arg RABBITMQ_HOST_ARG="$RABBITMQ_HOST" \
      --build-arg RABBITMQ_PORT_ARG="$RABBITMQ_PORT" \
      --build-arg RABBITMQ_USERNAME_ARG="$RABBITMQ_USERNAME" \
      --build-arg RABBITMQ_PASSWORD_ARG="$RABBITMQ_PASSWORD" \
      --build-arg INVOICE_STATUS_ON_RELATED_PLANS_RABBITMQ_QUEUE_NAME_ARG="$INVOICE_STATUS_ON_RELATED_PLANS_RABBITMQ_QUEUE_NAME" \
      --build-arg PLANS_TO_CREATE_RABBITMQ_QUEUE_NAME_ARG="$PLANS_TO_CREATE_RABBITMQ_QUEUE_NAME" \
      -t "$FULL_IMAGE_NAME" -f "$DOCKERFILE" ..
}

# Push the Docker image
push_docker_image() {
  docker push "$FULL_IMAGE_NAME"
}

# Main script execution
load_env_variables
build_docker_image
push_docker_image