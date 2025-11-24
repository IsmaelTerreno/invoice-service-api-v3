# Start with a base image containing Java runtime (JDK 22)
# Builder: use Maven with JDK 22 so we don't rely on the Maven Wrapper files
FROM maven:3.9-eclipse-temurin-22 AS builder

# Specify the working directory
WORKDIR /workspace

# Note: build-stage does not need the app ENV values; they are applied in the final runner stage.

# Copy only pom first to leverage Docker layer caching
COPY pom.xml .

# Resolve dependencies (optional but speeds up iterative builds)
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy the project source
COPY src src

# Build the application and robustly copy the produced jar (avoid brittle fixed name)
# - We still set a deterministic finalName, but in case plugins change naming, we select the
#   first non-sources/non-javadoc/non-original jar from target and copy it as plan-service-api.jar
RUN mvn -B -DskipTests -DfinalName=plan-service-api clean package \
    && echo "Built artifacts in target/:" \
    && ls -al target \
    && JAR_PATH=$(find target -maxdepth 1 -type f -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" ! -name "*original*" | head -n1) \
    && echo "Selecting jar: ${JAR_PATH}" \
    && cp "${JAR_PATH}" /workspace/plan-service-api.jar

# Runner: use lightweight JDK 22 base
FROM eclipse-temurin:22-jdk AS runner

# Re-declare build args in runner stage so they can be used as defaults for ENV
# Provide a sensible default port so EXPOSE never receives an empty value
ARG SERVER_PORT_LISTENING_ARG=3090
ARG CORS_ALLOW_ORIGINS_ARG

# --- DB config ---
ARG HOST_DB_CONFIG_ARG
ARG PORT_DB_CONFIG_ARG
ARG DATABASE_NAME_DB_CONFIG_ARG
ARG USER_NAME_DB_CONFIG_ARG
ARG USER_PASSWORD_DB_CONFIG_ARG

# --- JWT ---
ARG JWT_URL_ENDPOINT_ARG
ARG JWT_SECRET_ACCESS_ARG
ARG JWT_SECRET_REFRESH_ARG

# --- Test E2E defaults (optional) ---
ARG TEST_USER_NAME_ARG
ARG TEST_USER_EMAIL_ARG
ARG TEST_USER_PASSWORD_ARG

# --- RabbitMQ connection ---
ARG RABBITMQ_HOST_ARG
ARG RABBITMQ_PORT_ARG
ARG RABBITMQ_USERNAME_ARG
ARG RABBITMQ_PASSWORD_ARG

# --- Application queue names ---
ARG INVOICE_STATUS_ON_RELATED_PLANS_RABBITMQ_QUEUE_NAME_ARG
ARG PLANS_TO_CREATE_RABBITMQ_QUEUE_NAME_ARG

# Provide default ENV values baked into the image (can be overridden at runtime)
ENV SERVER_PORT_LISTENING=${SERVER_PORT_LISTENING_ARG}
ENV CORS_ALLOW_ORIGINS=${CORS_ALLOW_ORIGINS_ARG}

# --- DB config ---
ENV HOST_DB_CONFIG=${HOST_DB_CONFIG_ARG}
ENV PORT_DB_CONFIG=${PORT_DB_CONFIG_ARG}
ENV DATABASE_NAME_DB_CONFIG=${DATABASE_NAME_DB_CONFIG_ARG}
ENV USER_NAME_DB_CONFIG=${USER_NAME_DB_CONFIG_ARG}
ENV USER_PASSWORD_DB_CONFIG=${USER_PASSWORD_DB_CONFIG_ARG}

# --- JWT ---
ENV JWT_URL_ENDPOINT=${JWT_URL_ENDPOINT_ARG}
ENV JWT_SECRET_ACCESS=${JWT_SECRET_ACCESS_ARG}
ENV JWT_SECRET_REFRESH=${JWT_SECRET_REFRESH_ARG}

# --- Test E2E defaults (optional) ---
ENV TEST_USER_NAME=${TEST_USER_NAME_ARG}
ENV TEST_USER_EMAIL=${TEST_USER_EMAIL_ARG}
ENV TEST_USER_PASSWORD=${TEST_USER_PASSWORD_ARG}

# --- RabbitMQ connection ---
ENV RABBITMQ_HOST=${RABBITMQ_HOST_ARG}
ENV RABBITMQ_PORT=${RABBITMQ_PORT_ARG}
ENV RABBITMQ_USERNAME=${RABBITMQ_USERNAME_ARG}
ENV RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD_ARG}

# --- Application queue names ---
ENV INVOICE_STATUS_ON_RELATED_PLANS_RABBITMQ_QUEUE_NAME=${INVOICE_STATUS_ON_RELATED_PLANS_RABBITMQ_QUEUE_NAME_ARG}
ENV PLANS_TO_CREATE_RABBITMQ_QUEUE_NAME=${PLANS_TO_CREATE_RABBITMQ_QUEUE_NAME_ARG}

# Copy the jar from the builder stage
COPY --from=builder /workspace/plan-service-api.jar app.jar

# Make port SERVER_PORT_LISTENING value available to the world outside this container
EXPOSE ${SERVER_PORT_LISTENING}

# Run the jar file
ENTRYPOINT ["java","-jar","/app.jar"]