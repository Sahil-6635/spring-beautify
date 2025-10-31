# Use Java 8 base image
FROM openjdk:8-jdk-alpine
WORKDIR /app

# Copy the JAR from target folder
COPY target/spring-beautify-1.0.0.jar app.jar

# Expose port 1004 (or 8080)
EXPOSE 1004

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
