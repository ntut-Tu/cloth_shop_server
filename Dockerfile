FROM openjdk:17-jdk-alpine
VOLUME /app/uploads
ARG JAR_FILE=target/clothing-shop.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
