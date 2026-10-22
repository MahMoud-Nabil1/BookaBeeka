# ==========================================
# Multi-stage Dockerfile for Spring Boot
# ==========================================

# Stage 1: Build the JAR
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and package
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Production JRE runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as non-privileged user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copy built JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Dynamic PORT assigned by Render (default fallback 8080)
ENV PORT=8080
# Optimize JVM for Render container memory limits (512MB free tier)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Run application with Render PORT injection
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
