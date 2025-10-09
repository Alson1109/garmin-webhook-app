# Garmin Data Fetch Service

A Spring Boot application for integrating with Garmin Connect API to fetch smartwatch data, particularly step data and daily activity summaries.

## Features

- **OAuth 2.0 Authentication**: Secure authentication with Garmin Connect
- **Step Data Retrieval**: Fetch daily step counts from Garmin devices
- **Daily Summaries**: Get comprehensive daily activity summaries including:
  - Steps, distance, calories
  - Heart rate data
  - Stress levels
  - Body battery information
  - Activity duration and intensity
- **Webhook Support**: Handle real-time data pushes from Garmin
- **User Management**: Support for multiple users and device connections

## Prerequisites

1. **Java 17+**: Make sure you have Java 17 or higher installed
2. **Maven**: For building and dependency management
3. **Garmin Developer Account**: Register at [Garmin Developer Portal](https://developer.garmin.com/)

## Setup Instructions

### 1. Register with Garmin

1. Go to [Garmin Developer Portal](https://developer.garmin.com/)
2. Create a developer account
3. Register your application to get:
   - Client ID
   - Client Secret
4. Set the redirect URI to: `http://localhost:8080/api/garmin/callback`

### 2. Configure Environment

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

2. Update the `.env` file with your Garmin credentials:
   ```env
   GARMIN_CLIENT_ID=your-actual-client-id
   GARMIN_CLIENT_SECRET=your-actual-client-secret
   GARMIN_REDIRECT_URI=http://localhost:8080/api/garmin/callback
   ```

### 3. Install Dependencies

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Authentication
- `GET /api/garmin/authorize` - Generate Garmin authorization URL
- `GET /api/garmin/callback` - Handle OAuth callback from Garmin

### Data Retrieval
- `GET /api/garmin/permissions` - Get user permissions
- `POST /api/garmin/dailies` - Handle daily summaries webhook
- `DELETE /api/garmin/deregister/{userId}` - Deregister user

### Health Check
- `GET /actuator/health` - Application health status

## API Documentation

Once the application is running, you can access:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs**: `http://localhost:8080/api-docs`

## Database

The application uses H2 in-memory database for development. You can access the H2 console at:
- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:garmindb`
- **Username**: `sa`
- **Password**: (empty)

### Production Database

For production, uncomment the appropriate database dependency in `pom.xml` and update `application.properties`:

**PostgreSQL:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/garmin_db
spring.datasource.username=your-username
spring.datasource.password=your-password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

**MySQL:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/garmin_db
spring.datasource.username=your-username
spring.datasource.password=your-password
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
```

## Testing with Bruno

Since you have Bruno installed, you can use it to test the API endpoints:

1. **Authorization Flow**:
   - GET `http://localhost:8080/api/garmin/authorize`
   - Follow the returned authorization URL
   - Complete OAuth flow with Garmin

2. **Get Permissions**:
   - GET `http://localhost:8080/api/garmin/permissions`

3. **Webhook Testing**:
   - POST `http://localhost:8080/api/garmin/dailies`
   - Send sample daily summary data

## Data Structure

The application stores the following key data:

- **User Tokens**: OAuth tokens for authenticated users
- **Daily Summaries**: Comprehensive daily activity data including:
  - Step count and goals
  - Distance traveled
  - Calories burned
  - Heart rate metrics
  - Stress levels
  - Sleep data
  - Body battery levels

## Development

### Project Structure
```
src/main/java/io/fermion/az/health/garmin/
├── config/         # Configuration classes
├── controller/     # REST controllers
├── dto/           # Data transfer objects
├── entity/        # JPA entities
├── exception/     # Custom exceptions
├── repo/          # Data repositories
└── service/       # Business logic
```

### Building
```bash
mvn clean compile
```

### Running Tests
```bash
mvn test
```

### Creating JAR
```bash
mvn clean package
```

## Troubleshooting

1. **Client ID/Secret Issues**: Verify your Garmin developer credentials
2. **Callback URL Mismatch**: Ensure redirect URI matches exactly in Garmin developer console
3. **Database Issues**: Check H2 console for data verification
4. **Port Conflicts**: Change server port in `application.properties` if needed

## Next Steps

1. Set up your Garmin developer account
2. Update the `.env` file with your credentials
3. Run the application and test the authorization flow
4. Use Bruno to test the API endpoints
5. Integrate with your frontend application

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request