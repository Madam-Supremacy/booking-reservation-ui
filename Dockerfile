# Stage 1: Build the Java project with Maven
FROM maven:3.9.3-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the project
RUN mvn clean package

# Stage 2: Run the Java jar in a lightweight JDK
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/target/booking-reservation-system-1.0-SNAPSHOT.jar app.jar

# Expose the port your app listens on
EXPOSE 8080

# Command to run the jar
CMD ["java", "-jar", "app.jar"]
