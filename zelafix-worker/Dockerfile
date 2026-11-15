# =============================================================================
# Stage 1 — BUILD
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Wrapper e configurações do Gradle Multi-Project
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew

# Código-fonte e recursos do Worker
COPY zelafix-worker/build.gradle zelafix-worker/build.gradle
COPY zelafix-worker/src/ zelafix-worker/src/
RUN mkdir -p zelafix-api

RUN ./gradlew :zelafix-worker:bootJar --no-daemon -x test

# =============================================================================
# Stage 2 — RUNTIME
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && \
    echo "America/Sao_Paulo" > /etc/timezone

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/zelafix-worker/build/libs/*.jar app.jar
RUN chown -R appuser:appgroup /app

ENV TZ=America/Sao_Paulo
USER appuser
EXPOSE 8081

ENTRYPOINT ["java", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
