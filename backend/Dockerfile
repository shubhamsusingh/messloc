# Multi-stage build for Spring Boot with Java 21
# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and pre-fetch dependencies to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:resolve -B

# Copy source code and build jar without running tests
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy built artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Render automatically maps the port via $PORT)
EXPOSE 8080

# Optimized for containerized environments (Render free tier 512MB RAM)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
