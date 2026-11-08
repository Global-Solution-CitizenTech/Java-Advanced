# Estágio de Build
FROM amazoncorretto:25 AS builder
WORKDIR /app
COPY ../gradlew ../gradle ./
COPY ../settings.gradle ../build.gradle ./
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew && ./gradlew :zelafix-api:generateRsaKeys :zelafix-api:bootJar -x test --no-daemon

# Estágio de Execução
FROM amazoncorretto:25-alpine
WORKDIR /app
COPY --from=builder /app/zelafix-api/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

