# Estágio de Build
FROM amazoncorretto:25 AS builder
WORKDIR /app
COPY ../gradlew ../gradle ./
COPY ../settings.gradle ../build.gradle ./
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew && ./gradlew :zelafix-worker:bootJar -x test --no-daemon

# Estágio de Execução
FROM amazoncorretto:25-alpine
WORKDIR /app
COPY --from=builder /app/zelafix-worker/build/libs/*.jar app.jar

ENV WORKER_PORT=8081
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]

