# Step 1: Build the app using Maven
FROM maven:3.9.6-eclipse-temurin-11 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package spring-boot:repackage -DskipTests

# Step 2: Run the app
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
EXPOSE 1004

ENTRYPOINT ["java", "-jar", "app.jar"]
