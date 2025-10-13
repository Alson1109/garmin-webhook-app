# =============================================
# BUILD STAGE
# =============================================
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# =============================================
# RUNTIME STAGE
# =============================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/garmin-data-fetch-1.0.0.jar app.jar

# Expose the default port (Railway overrides this dynamically)
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
