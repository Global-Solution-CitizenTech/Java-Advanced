FROM amazoncorretto:25-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
