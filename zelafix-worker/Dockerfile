FROM amazoncorretto:25-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
ENV WORKER_PORT=8081
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
