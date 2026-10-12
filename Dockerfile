# ==========================================
# SkyGuard Aviation System - Production Dockerfile
# ==========================================

# Stage 1: Build the Application
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Maven Wrapper and POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build executable JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal Runtime Environment
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy packaged JAR from builder
COPY --from=builder /app/target/skyguard-1.0.0.jar app.jar

# Expose Web Port
EXPOSE 8080

# Environment Defaults (can be overridden at runtime)
ENV DB_URL=jdbc:mysql://mysql:3306/aviation_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
ENV DB_USER=root
ENV DB_PASSWORD=root

# Launch Spring Boot Application
ENTRYPOINT ["java", "-jar", "app.jar"]
