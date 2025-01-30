# Use official OpenJDK base image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the Maven project file
COPY pom.xml .

# Copy the project source
COPY src ./src

# Copy Maven wrapper files
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Make the mvnw script executable
RUN chmod +x mvnw

# Build the application
RUN ./mvnw clean package -DskipTests

# Create the final image with just the JAR
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from the previous stage
COPY --from=0 /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "app.jar"]