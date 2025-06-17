# Use official OpenJDK base image for building
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy the Maven project and wrapper
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Make the mvnw script executable
RUN chmod +x mvnw

# Copy the source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Use a smaller runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create log directory with proper permissions
RUN mkdir -p /app/logs && chmod 755 /app/logs

# Copy the built JAR from the previous stage
COPY --from=builder /app/target/*.jar app.jar

# Define environment variables (runtime only)
ENV SPRING_PROFILES_ACTIVE=http
ENV SSL_PASSWORD=changeit
ENV LOG_PATH=/app/logs
ENV LOG_ARCHIVE=/app/logs/archive

# Create log archive directory
RUN mkdir -p ${LOG_ARCHIVE}

# Expose ports (HTTP: 8080, HTTPS: 8443)
EXPOSE 8080 8443

# Create volume for persistent logs
VOLUME /app/logs

# Entry script to handle SSL dynamically
COPY docker-entry.sh /docker-entry.sh
RUN chmod +x /docker-entry.sh

# Use the entrypoint script to configure SSL
ENTRYPOINT ["/docker-entry.sh"]