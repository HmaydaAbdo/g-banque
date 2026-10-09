# ================================
# Multi-stage Dockerfile for Spring Boot Backend
# Stage 1: Build
# Stage 2: Runtime
# ================================

# ============ STAGE 1: BUILD ============
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# ============ STAGE 2: RUNTIME ============
FROM eclipse-temurin:17-jre-alpine

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/gBanque-0.0.1-SNAPSHOT.jar app.jar

# Répertoire des logs (un fichier par jour) — monté en volume nommé par docker-compose ;
# créé ici pour que le volume hérite du propriétaire "spring" (utilisateur non-root).
RUN mkdir -p /app/logs
ENV LOG_DIR=/app/logs

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# Health check (will wait for actuator endpoint)
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8080/gBanque/api/actuator/health || exit 1

# Run the application with optimized JVM settings
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]