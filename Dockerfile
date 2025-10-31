# Step 1: Build the app using Maven
FROM maven:3.9.6-eclipse-temurin-11 AS build
WORKDIR /app

# Copy pom.xml and download dependencies first (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR
COPY src ./src
RUN mvn clean package spring-boot:repackage -DskipTests

# Step 2: Run the app
FROM openjdk:8-jdk-alpine
WORKDIR /app

# Copy built JAR from the first stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (your app runs on 1004)
EXPOSE 1004

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
