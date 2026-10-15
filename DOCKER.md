# G-Commerce Backend API

A robust e-commerce REST API built with Spring Boot that provides comprehensive JWT authentication, role-based access control, product management, purchase tracking, and advanced security features with rate limiting.

## 🚀 Features

- **User Management**
  - User registration and authentication
  - Role-based authorization (ADMIN, USER)
  - Profile management and password updates
  - Paginated user listing for administrators

- **Product Management**
  - Complete CRUD operations
  - Product status management (active/inactive)
  - Public access to active products
  - Role-based access control for product operations
  - Stock management
  - Paginated product listing with status filtering
  - Text search across product name and description fields

- **Purchase System**
  - Shopping cart functionality
  - Order tracking
  - Purchase history tracking with pagination
  - Role-specific purchase restrictions
  - Transaction management

- **JWT Authentication & Authorization**
  - OAuth2 Resource Server with RSA-signed JWT tokens
  - Access and refresh token system with automatic token rotation
  - Token blacklisting and secure logout functionality
  - RSA key pair generation, storage, and scheduled rotation
  - OpenID Connect discovery endpoints and JWKS support

- **Advanced Security & Rate Limiting**
  - Rate limiting with token bucket algorithm
  - Role-based rate limits (Admin/User/Unauthenticated)
  - Redis-based token storage and blacklisting
  - Automatic profile detection based on available SSL certificate
  - HTTPS configuration with SSL keystore
  - Environment-based profile activation
  - RSA-based JWT token signing and validation

- **Comprehensive Documentation**
  - Javadoc for code-level documentation
  - OpenAPI (Swagger) for interactive API documentation
  - Detailed endpoint descriptions and schemas
  - JWKS and OpenID Connect discovery endpoints

- **Monitoring & Management**
  - Spring Boot Actuator integration for application monitoring
  - Health checks with readiness and liveness probes
  - Metrics collection and exposure (JVM, system, web, database)
  - Application info endpoint with environment and build details
  - Prometheus metrics support for external monitoring systems

## 🚦 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Git (optional)

### Installation

1- Create a `.env` file with the required environment variables:
```env
# Database Configuration
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_USER=your_database_user
MYSQL_PASSWORD=your_database_password

# Redis Configuration
REDIS_PASSWORD=your_redis_password

# Admin User Configuration
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=your_admin_password

# JWT Configuration
JWT_ISSUER=g-commerce-api
JWT_AUDIENCE=g-commerce-api
JWT_ACCESS_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=86400000

# Encryption of the JWT signing keys stored in Redis (required, e.g. `openssl rand -base64 32`)
OAUTH2_KEY_ENCRYPTION_SECRET=your_key_encryption_secret

# OAuth2 Key Rotation (optional)
OAUTH2_KEY_ROTATION_ENABLED=true
OAUTH2_KEY_ROTATION_CRON=0 0 2 1 * ?
```

#### HTTPS support (optional)

**If you need the application to run with HTTPS**, you must add the following environment variables in your `.env` file and provide a valid SSL certificate in a `certs/` directory.

```env
SPRING_PROFILES_ACTIVE=https
SSL_PASSWORD=your_ssl_password
```

2- Create a `docker-compose.yml`:
```yaml
# Full deployment configuration - app, database, and Redis
services:
  app:
    image: gussttaav/g-commerce-backend:latest
    ports:
      - "8080:8080"
      - "8443:8443"
    env_file:
      - .env
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/shopping
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-http}
      LOG_PATH: /app/logs
      LOG_ARCHIVE: /app/logs/archive
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    volumes:
      - ./certs:/certs
      - app-logs:/app/logs
    networks:
      - app-network

  mysql:
    image: gussttaav/g-commerce-mysql:latest
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

  redis:
    image: redis:7-alpine
    command: ["redis-server", "--appendonly", "yes", "--requirepass", "${REDIS_PASSWORD}"]
    ports:
      - "6379:6379"
    environment:
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    networks:
      - app-network

volumes:
  mysql-data:
  app-logs:

networks:
  app-network:
    driver: bridge
```

3. Start the application:
```bash
docker compose up -d
```

- If **HTTPS is enabled**, access the API at `https://localhost:8443`
- If **HTTP mode is active (default)**, access it at `http://localhost:8080`

💡 The database schema is automatically initialized - no additional setup required!

## 📚 API Documentation

Once running, access the API documentation at:
- **Swagger UI**: `http://localhost:8080/swagger-ui` or `https://localhost:8443/swagger-ui`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs` or `https://localhost:8443/v3/api-docs`

### Discovery Endpoints
- **JWKS Endpoint**: `/.well-known/jwks.json`
- **OpenID Configuration**: `/.well-known/openid-configuration`

## 🔌 Main Endpoints

### Authentication
```http
POST /api/auth/login            # Login with credentials, returns JWT tokens
POST /api/auth/refresh          # Refresh access token using refresh token
POST /api/auth/logout           # Logout and blacklist tokens
GET  /api/auth/validate         # Validate JWT token
```

### User Management
```
POST /api/users                 # Register new user (role: USER or ADMIN if caller is ADMIN)
GET  /api/users/me              # Get current user profile
PATCH /api/users/me             # Update profile or password

# With ADMIN role only:
GET    /api/users               # List all users (paginated)
PATCH  /api/users/{id}/role     # Change user role (ADMIN/USER)
```

### Product Management
```
GET /api/products               # List active products (search, pagination)

# With ADMIN role only:
GET    /api/products?status=INACTIVE|ALL   # List inactive or all products
POST   /api/products                       # Create product
PUT    /api/products/{id}                  # Update product
DELETE /api/products/{id}                  # Delete product
```

### Purchase Management
```
POST /api/purchases             # Create new purchase
GET  /api/purchases             # List user purchases (paginated)
```

### Discovery Endpoints
```
GET  /.well-known/jwks.json                   # JSON Web Key Set
GET  /.well-known/openid-configuration        # OpenID Provider metadata
```

### Monitoring Endpoints
```
GET  /actuator/health              # Application health status
GET  /actuator/info                # Application information
GET  /actuator/metrics             # Application metrics
GET  /actuator/prometheus          # Prometheus-formatted metrics
```

## ⚙️ Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `MYSQL_ROOT_PASSWORD` | MySQL root password | - | Yes |
| `MYSQL_USER` | Database user | - | Yes |
| `MYSQL_PASSWORD` | Database password | - | Yes |
| `MYSQL_CHARSET` | Database charset | utf8mb4 | No |
| `MYSQL_COLLATION` | Database collation | utf8mb4_unicode_ci | No |
| `REDIS_PASSWORD` | Redis server password | - | Yes |
| `ADMIN_EMAIL` | Admin user email | - | Yes |
| `ADMIN_PASSWORD` | Admin user password | - | Yes |
| `JWT_ISSUER` | JWT token issuer | g-commerce-api | No |
| `JWT_AUDIENCE` | JWT token audience (required on incoming tokens) | g-commerce-api | No |
| `JWT_ACCESS_EXPIRATION_MS` | Access token expiration (ms) | 3600000 | No |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token expiration (ms) | 86400000 | No |
| `OAUTH2_KEY_ENCRYPTION_SECRET` | Secret (at least 32 characters) from which the key that encrypts the JWT signing keys in Redis is derived; changing it rotates the signing key | - | Yes |
| `OAUTH2_KEY_ROTATION_ENABLED` | Enable automatic key rotation | true | No |
| `OAUTH2_KEY_ROTATION_CRON` | Key rotation schedule | 0 0 2 1 * ? | No |
| `SPRING_PROFILES_ACTIVE` | Set "https" to enable SSL, "http" to disable | http | No |
| `SSL_PASSWORD` | Password for SSL keystore | - | No (Only for HTTPS) |
| `LOG_PATH` | Application log path | /app/logs | No |
| `LOG_ARCHIVE` | Archived logs path | /app/logs/archive | No |

## 📝 JWT Authentication Flow

### 1. Login
```bash
POST /api/auth/login
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "password123"
}
```

**Response:**
```json
{
    "usuario": {
        "id": 1,
        "name": "John Doe",
        "email": "user@example.com",
        "role": "USER"
    },
    "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshExpiresIn": 86400
}
```

### 2. Authenticated Requests
```bash
GET /api/users/me
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 3. Token Refresh
```bash
POST /api/auth/refresh
Authorization: Bearer <refresh_token>
```

### 4. Logout
```bash
POST /api/auth/logout
Authorization: Bearer <access_token>
Content-Type: application/json

{
    "refreshToken": "<refresh_token>"
}
```

## 📝 Example Requests

### Register a User
```bash
POST /api/users
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

### Create a Product (Admin only)
```bash
POST /api/products
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "Laptop",
  "description": "High-performance laptop",
  "price": 999.99
}
```

### Make a Purchase
```bash
POST /api/purchases
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "products": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

## � Container Management

```bash
# Start services
docker compose up -d

# View logs
docker compose logs -f

# View specific service logs
docker compose logs -f app
docker compose logs -f mysql
docker compose logs -f redis

# Stop services
docker compose down

# Stop and remove volumes (clears database and Redis data)
docker compose down -v

# Check service status
docker compose ps

# Scale services (if needed)
docker compose up -d --scale redis=1
```

## 🛠️ Technologies

- **Backend Framework**: Spring Boot 3.4.9
- **Language**: Java 21
- **Security**: Spring Security with OAuth2 Resource Server
- **Authentication**: JWT with RSA signing (NimbusDS JOSE+JWT)
- **Database**: MySQL 9
- **Caching/Storage**: Redis 7 (token storage, blacklisting, rate limiting)
- **Rate Limiting**: Bucket4j with token bucket algorithm
- **Documentation**: OpenAPI 3.0 (Swagger)
- **Containerization**: Docker & Docker Compose
- **Testing**: JUnit 5, Mockito, H2 in-memory database
- **Build Tool**: Maven
- **Monitoring**: Spring Boot Actuator, Prometheus
- **Code Quality**: Checkstyle, PMD

## 🔒 Security Features

- **JWT Authentication**: OAuth2 Resource Server with RSA-signed tokens
- **Token Management**: Access and refresh token system with automatic rotation
- **Rate Limiting**: Role-based rate limits using token bucket algorithm
- **Token Blacklisting**: Redis-based secure logout functionality
- **Key Rotation**: Scheduled RSA key pair rotation for enhanced security
- **Key Encryption**: RSA private keys are stored in Redis encrypted with AES-256-GCM, under a key derived from a secret that is never stored
- **HTTPS Support**: SSL/TLS encryption for secure communication
- **Password Encryption**: BCrypt hashing for user passwords
- **Role-based Access Control**: Granular permissions for different user roles

## 📦 Related Images

This application consists of three Docker images:
- Frontend: `gussttaav/g-commerce-frontend`
- Backend API (this image): `gussttaav/g-commerce-backend`
- Database: `gussttaav/g-commerce-mysql`

## 💻 Source Code

The complete source code is available on [GitHub](https://github.com/gussttaav/g-commerce-springboot-api)

## 🤝 Support

For issues and feature requests, please use the [GitHub Issues](https://github.com/gussttaav/g-commerce-springboot-api/issues) page.

## 📄 License

This project is licensed under the MIT License.