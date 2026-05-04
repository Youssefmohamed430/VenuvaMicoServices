# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and other Maven configuration
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn

# Copy source code
COPY src src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Create a non-root user for security
RUN useradd -m appuser

# Copy the built JAR from builder stage
COPY --from=builder /app/target/venuva-0.0.1-SNAPSHOT.jar app.jar

# Change ownership
RUN chown appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Environment variables with defaults (non-sensitive only)
# Sensitive values like JWT_SECRET and SPRING_DATASOURCE_PASSWORD must be provided at runtime
ENV SERVER_PORT=8081 \
    SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/authdb \
    SPRING_DATASOURCE_USERNAME=root \
    LOGGING_FILE_NAME=/var/log/app/venuva.log \
    LOGGING_MAX_FILE_SIZE=10MB \
    LOGGING_MAX_HISTORY=30 \
    LOGGING_LEVEL_VENUVA=DEBUG \
    LOGGING_LEVEL_SECURITY=INFO \
    LOGGING_LEVEL_HIBERNATE=DEBUG

# Create logs directory
RUN mkdir -p /var/log/app && chown appuser:appuser /var/log/app

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
