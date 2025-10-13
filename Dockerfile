# ===========================
# 1️⃣ BUILD STAGE
# ===========================
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only pom.xml first (for dependency caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the entire source code
COPY src ./src

# Package the app (skip tests to speed up build)
RUN mvn clean package -DskipTests

# ===========================
# 2️⃣ RUNTIME STAGE
# ===========================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port used by Spring Boot
EXPOSE 8080

# ✅ Tell Railway how to start the app
ENTRYPOINT ["java", "-jar", "app.jar"]
