FROM maven:3.9.5-eclipse-temurin-21 AS build

WORKDIR /app

# Copy all source files
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage with Java 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/garmin-data-fetch-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=default
ENV SERVER_PORT=8080

# Add JVM options for container support
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
