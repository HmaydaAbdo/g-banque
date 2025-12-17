# Stage 1: Build the application using a Maven base image
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
# Copy the Maven pom.xml and source code
COPY pom.xml .
COPY src ./src


# Build the JAR file
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

# Stage 2: Create the final, smaller runtime image
FROM eclipse-temurin:17-jre-focal
# Create a dedicated group and user for security
RUN groupadd -r springboot && useradd -r -g springboot springboot
# Set the working directory
WORKDIR /app
# Copy the built JAR from the build stage
COPY --from=build /app/target/gBanque-0.0.1-SNAPSHOT.jar app.jar
# Make the user a non-root user
USER springboot
# Expose the port (matches the server.port in application.yml)
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]