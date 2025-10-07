FROM maven:3.8.4-openjdk-17# Multi-stage build to avoid large files in GitHub

FROM maven:3.8.4-openjdk-17 AS build

WORKDIR /app

# Set working directory

# Copy all source filesWORKDIR /app

COPY . .

# Copy Maven files

# Build the applicationCOPY pom.xml .

RUN mvn clean package -DskipTests

# Download dependencies (cached layer)

# Expose portRUN mvn dependency:go-offline -B

EXPOSE 8080

# Copy source code

# Run the applicationCOPY src ./src

CMD ["java", "-jar", "target/garmin-data-fetch-1.0.0.jar"]
# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/target/garmin-data-fetch-1.0.0.jar app.jar

# Create data directory for H2 database
RUN mkdir -p /app/data

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8080
ENV DATABASE_PATH=./data/garmindb

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]