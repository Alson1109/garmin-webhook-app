FROM maven:3.8.4-openjdk-17

WORKDIR /app

# Copy all source files
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8080
ENV DATABASE_PATH=./data/garmindb

# Run the application
CMD ["java", "-jar", "target/garmin-data-fetch-1.0.0.jar"]