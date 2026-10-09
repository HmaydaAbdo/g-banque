# syntax=docker/dockerfile:1
# ================================
# Multi-stage Dockerfile — Spring Boot Backend
# Stage 1 : build + extraction des couches du JAR (layertools)
# Stage 2 : runtime JRE alpine, non-root, couches séparées
#
# Les couches (dépendances / loader / application) permettent de ne retransférer
# lors d'un déploiement que la couche "application" (quelques Mo) au lieu du JAR complet.
# ================================

# ============ STAGE 1: BUILD ============
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# pom.xml d'abord : la couche des dépendances n'est reconstruite que si le pom change
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline

COPY src ./src

# Les tests sont exécutés par la CI (workflow ci.yml), pas pendant le build de l'image
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -DskipTests \
    && java -Djarmode=layertools -jar target/*.jar extract --destination /layers

# ============ STAGE 2: RUNTIME ============
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="g-banque-backend" \
      org.opencontainers.image.description="G-Banque — API Spring Boot" \
      org.opencontainers.image.source="https://github.com/HmaydaAbdo/g-banque"

# Correctifs de sécurité de l'OS à jour ; pas de curl (le healthcheck utilise wget de busybox)
RUN apk upgrade --no-cache \
    && addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Répertoire des logs (un fichier par jour) — monté en volume nommé par docker-compose ;
# créé ici pour que le volume hérite du propriétaire "spring" (utilisateur non-root).
RUN mkdir -p /app/logs && chown spring:spring /app/logs
ENV LOG_DIR=/app/logs

# Ordre : du moins au plus souvent modifié
COPY --from=build --chown=spring:spring /layers/dependencies/ ./
COPY --from=build --chown=spring:spring /layers/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /layers/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /layers/application/ ./

USER spring

EXPOSE 8080

# SERVER_PORT et CONTEXT_PATH sont fournis par docker-compose (cf. .env)
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -q --spider "http://localhost:${SERVER_PORT:-8080}${CONTEXT_PATH}/actuator/health" || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
