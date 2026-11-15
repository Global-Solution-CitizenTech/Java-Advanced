# =============================================================================
# Stage 1 — BUILD
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Wrapper e configurações do Gradle Multi-Project
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew

# Código-fonte e recursos da API
COPY zelafix-api/build.gradle zelafix-api/build.gradle
COPY zelafix-api/src/ zelafix-api/src/
RUN mkdir -p zelafix-worker

# Geração das chaves RSA para JWT (produção)
RUN apk add --no-cache openssl && \
    mkdir -p zelafix-api/src/main/resources/keys && \
    openssl genpkey -algorithm RSA -out zelafix-api/src/main/resources/keys/private_key.pem -pkeyopt rsa_keygen_bits:2048 && \
    openssl rsa -pubout -in zelafix-api/src/main/resources/keys/private_key.pem -out zelafix-api/src/main/resources/keys/public_key.pem

RUN ./gradlew :zelafix-api:bootJar --no-daemon -x test

# =============================================================================
# Stage 2 — RUNTIME
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && \
    echo "America/Sao_Paulo" > /etc/timezone

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/zelafix-api/build/libs/*.jar app.jar
RUN chown -R appuser:appgroup /app

ENV TZ=America/Sao_Paulo
USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
