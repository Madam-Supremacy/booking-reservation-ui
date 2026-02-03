# Use an official Maven + Java image to build the project
FROM maven:3.9.3-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the project
RUN mvn clean package

# Use a lighter Java runtime image to run the jar
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/booking-reservation-system-1.0-SNAPSHOT.jar app.jar

# Expose the port your server runs on
EXPOSE 8080

# Run the jar
CMD ["java", "-jar", "app.jar"]
