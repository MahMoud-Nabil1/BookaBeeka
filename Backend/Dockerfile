# ==============================================================================
# Stage 1: Build Spring Boot Application
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Download dependencies & plugins first to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins -B

# 2. Copy source code and build the executable fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Stage 2: Minimal Production Runtime
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-privileged user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled JAR with ownership set directly (no duplicate layer)
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

# Dynamic PORT assigned by Render (falls back to 8081 for local/docker-compose)
ENV PORT=8081

# JVM memory management tuned for containerized environments (e.g. Render free tier)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
