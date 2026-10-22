# Spring Boot G-commerce API

[![Build Status](https://img.shields.io/github/actions/workflow/status/gussttaav/g-commerce-springboot-api/docker-build.yml?branch=main&logo=github&logoColor=white)](https://github.com/gussttaav/g-commerce-springboot-api/actions/workflows/docker-build.yml)
[![Tests](https://img.shields.io/github/actions/workflow/status/gussttaav/g-commerce-springboot-api/docker-build.yml?branch=main&logo=checkmarx&logoColor=white&label=tests)](https://github.com/gussttaav/g-commerce-springboot-api/actions/workflows/docker-build.yml)
[![Deploy Status](https://img.shields.io/github/actions/workflow/status/gussttaav/g-commerce-springboot-api/aws-deploy.yml?branch=main&logo=amazonaws&logoColor=white&label=deploy)](https://github.com/gussttaav/g-commerce-springboot-api/actions/workflows/aws-deploy.yml)
[![Health Check](https://img.shields.io/github/actions/workflow/status/gussttaav/g-commerce-springboot-api/health-check.yml?branch=main&logo=heart&logoColor=white&label=health)](https://github.com/gussttaav/g-commerce-springboot-api/actions/workflows/health-check.yml)
[![API Documentation](https://img.shields.io/badge/API%20Docs-Swagger-85EA2D?logo=swagger&logoColor=white)](https://g-commerce.duckdns.org/swagger-ui/index.html)
[![Code Quality](https://img.shields.io/badge/Code%20Quality-Checkstyle%20%7C%20PMD-blue?logo=codeclimate&logoColor=white)](https://github.com/gussttaav/g-commerce-springboot-api)
[![Backend Docker Image](https://img.shields.io/docker/pulls/gussttaav/g-commerce-backend?logo=docker&logoColor=white&label=pulls)](https://hub.docker.com/r/gussttaav/g-commerce-backend)

The **G-Commerce API** is a robust, secure, and scalable backend for e-commerce applications, built with **Spring Boot**. It provides essential features such as **JWT authentication with refresh tokens, role-based access control, product management, purchases and history tracking with pagination**. Designed for seamless integration with various front-end projects, this API ensures a smooth and efficient e-commerce experience.

## 🚀 Features

- **User Management**
  - User registration and authentication
  - Role-based authorization (ADMIN, USER)
  - Profile management and password updates
  - Admin-only user listing with pagination

- **Product Management**
  - Full CRUD operations on `/api/products`
  - Product status management (active/inactive)
  - Public access to active products
  - Role-based access control for product operations
  - Paginated product listing with status filtering
  - Text search across product name and description fields
  
- **Purchase System**
  - Shopping cart functionality
  - Purchase history tracking with pagination
  - Role-specific purchase restrictions

- **JWT Authentication & Authorization**
  - OAuth2 Resource Server with RSA-signed JWT tokens
  - Access tokens and single-use refresh tokens
  - Token revocation on logout
  - RSA key pair generation, encrypted storage, and scheduled rotation without downtime
  - OpenID Connect discovery endpoints and JWKS support

- **Security & SSL Support**
  - Rate limiting with token bucket algorithm
  - Role-based rate limits for users, and per IP address limits for anonymous requests and login attempts
  - HTTPS profile selected automatically when a keystore is available, unless a profile is set explicitly
  - HTTPS configuration with SSL keystore
  - RSA-based JWT token signing and validation
  - Redis-based token storage and blacklisting

- **Comprehensive Documentation**
  - Javadoc for code-level documentation
  - OpenAPI (Swagger) for interactive API documentation
  - Detailed endpoint descriptions and schemas

- **Comprehensive Testing**
  - Unit tests for all layers
  - Security tests
  - Mock MVC tests for controllers
  - Integration tests with an in-memory H2 database and an embedded Redis server

- **Continuous Integration via GitHub Actions**
  - Code quality checks and tests on every pull request and push to `main`
  - Automated Docker image build and publish to Docker Hub
  - Environment-based configuration management
  - Automated deploy to an AWS EC2 instance pulling the created images
  - Automated availability monitoring after deployment

- **Monitoring & Management**
  - Spring Boot Actuator integration for application monitoring
  - Health checks with readiness and liveness probes
  - Metrics collection and exposure (JVM, system, web, database)
  - Application info endpoint with environment and build details
  - Prometheus metrics support for external monitoring systems

## 🛠️ Technologies

- Java 21
- Spring Boot 3.5.9
- Spring Security (OAuth2 Resource Server)
- NimbusDS JOSE+JWT library
- Redis (for token storage and blacklisting)
- Bucket4j (for rate limiting)
- OpenAPI/Swagger
- Spring Data JPA
- MySQL 9
- H2 Database (for testing)
- JUnit 5
- Mockito
- Docker & Docker Compose
- Maven
- Lombok
- GitHub Actions
- Spring Boot Actuator
- Prometheus (metrics)
- Checkstyle (code style enforcement)
- PMD (static code analysis)

## 📋 Prerequisites

- Java 21 or higher
- Docker and Docker Compose
- Git

Maven is optional: the repository includes the Maven Wrapper (`./mvnw`).

## 🔧 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/gussttaav/g-commerce-springboot-api.git
   cd g-commerce-springboot-api
   ```

2. Create a `.env` file in the root directory with the following variables:
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
   JWT_ACCESS_EXPIRATION_MS=3600000
   JWT_REFRESH_EXPIRATION_MS=86400000
   
   # Encryption of the JWT signing keys stored in Redis (required, e.g. `openssl rand -base64 32`)
   OAUTH2_KEY_ENCRYPTION_SECRET=your_key_encryption_secret
   
   # OAuth2 Key Rotation (optional)
   OAUTH2_KEY_ROTATION_ENABLED=true
   OAUTH2_KEY_ROTATION_CRON="0 0 2 1 * ?"

   # Allowed CORS origins (optional, defaults to http://localhost:3000)
   CORS_ALLOWED_ORIGINS=http://localhost:3000
   ```

   #### HTTPS support (optional)
   Unless a profile is set explicitly, the application chooses it on startup: `https` (port 8443) if it finds a PKCS12 keystore at `/certs/keystore.p12` or on the classpath (e.g. `src/main/resources/keystore.p12`), and `http` (port 8080) otherwise. The keystore must contain the certificate under the alias `springboot`, and its password goes in your `.env` file:

   ```env
   SSL_PASSWORD=your_keystore_password
   ```

   Set `SPRING_PROFILES_ACTIVE=http` or `SPRING_PROFILES_ACTIVE=https` to choose the profile yourself, and `SSL_KEY_STORE` (e.g. `file:/path/to/keystore.p12`) to use a keystore stored elsewhere.

3. Start the MySQL database and Redis using Docker Compose:
   ```bash
   docker compose -f docker-compose.db.yml up -d
   ```

4. Build and run the application. Maven does not read the `.env` file, so export its variables first:
   ```bash
   set -a; source .env; set +a
   ./mvnw spring-boot:run
   ```

The application will be available at `http://localhost:8080`, or at `https://localhost:8443` with the `https` profile.

## 🐳 Docker Images

The application is split into two Docker images:

1. **Backend Application** (`gussttaav/g-commerce-backend`)
   - Spring Boot application
   - API endpoints and business logic
   - Available tags: `latest`

2. **Database** (`gussttaav/g-commerce-mysql`)
   - MySQL 9 with a pre-configured schema
   - Includes all necessary tables and initial data
   - Available tags: `latest`

Both images are automatically built and pushed to Docker Hub on every push to this branch using GitHub Actions. Refer to the [repository documentation](https://hub.docker.com/repository/docker/gussttaav/g-commerce-backend/general) if you want to deploy the full application with docker compose.

After the image is pushed to Docker Hub the application is automatically deployed to an AWS EC2 instance pulling the created images and running the associated containers using Docker Compose.

## 📚 API Documentation

### OpenAPI (Swagger) Documentation

The project uses SpringDoc OpenAPI for interactive API documentation:

- **Swagger UI**: `http://localhost:8080/swagger-ui`
  - Interactive documentation of all API endpoints
  - Ability to test endpoints directly from the browser
  - Detailed request and response schemas

- **API Docs JSON**: `http://localhost:8080/v3/api-docs`
  - Machine-readable OpenAPI specification
  - Can be used for client code generation

The document covers the `/api/**` and `/.well-known/**` endpoints, with an overview of authentication, errors,
pagination and rate limits, every error response with an example body, and request examples based on the demo data.

#### Published OpenAPI document

A copy of the document is committed at [`docs-site/public/openapi.json`](docs-site/public/openapi.json), so API
changes show up as a diff in their pull request. `OpenApiSpecTest` fails if the copy is out of date; after changing
the API, regenerate it and commit it:

```bash
./mvnw test -Dtest=OpenApiSpecTest -Dopenapi.update=true
```

CI also lints it with [Redocly CLI](https://redocly.com/docs/cli/) (rules in `redocly.yaml`):

```bash
npx @redocly/cli@2.58.2 lint docs-site/public/openapi.json
```

### JWKS and OpenID Connect Discovery

The API exposes standard discovery endpoints for OAuth2 clients:

- **JWKS Endpoint**: `/.well-known/jwks.json`
  - JSON Web Key Set containing public keys for JWT verification
  - Used by external services to validate JWT signatures

- **OpenID Configuration**: `/.well-known/openid-configuration`
  - OpenID Provider metadata following RFC 8414
  - Configuration information for OAuth2 clients

### Javadoc

Comprehensive Javadoc documentation is available for all classes.

To generate Javadoc:
```bash
mvn javadoc:javadoc
```

### Health and Monitoring Endpoints

Spring Boot Actuator provides several monitoring endpoints:

- **Health Check**: `/actuator/health`
  - Basic health status available to all users
  - Detailed health information available to authenticated users
  - Includes database connectivity and disk space checks

- **Application Info**: `/actuator/info`
  - Application metadata including version, build info, and environment details
  - Git information and Java/OS details

- **Metrics**: `/actuator/metrics`
  - JVM, system, web, and database metrics
  - Custom application metrics

- **Prometheus**: `/actuator/prometheus`
  - Prometheus-formatted metrics for external monitoring systems

## 🧪 Testing

### Test Configuration
The tests need no external services: they use an H2 in-memory database and an embedded Redis server on port 6379, so stop any local Redis bound to that port before running them.

### Test Categories
- **Controller Tests**: Using MockMvc to test HTTP endpoints
- **Service Tests**: Unit tests for business logic
- **Repository Tests**: Database operation tests
- **Security Tests**: Authentication and authorization tests
- **Integration Tests**: End-to-end functionality tests

### Running Tests
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UserControllerTest

# Run the tests and the PMD and CPD checks
./mvnw clean verify
```

## ✅ Code Quality

The project uses Checkstyle and PMD for code quality analysis:

- **Checkstyle**: Enforces coding standards (`checkstyle-rules.xml`)
- **PMD**: Static code analysis for best practices (`pmd-ruleset.xml`)

Checkstyle runs on every build, and PMD and CPD run in the `verify` phase. Run them on their own with:
```bash
# Checkstyle only
./mvnw checkstyle:check

# PMD only
./mvnw pmd:check
```

## 🐞 Known Issues

The defects found in a code review, and the phased plan that fixed them, are tracked in [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md).

## 🔑 API Endpoints

### Authentication
```
POST /api/auth/login            # Login → access + refresh tokens
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

## 🔒 Security

- JWT authentication using OAuth2 Resource Server with RSA-signed tokens, validated against issuer and audience
- Tokens identify users by ID, so a session survives an email change
- Single-use refresh tokens: each refresh issues a new pair and invalidates the token used
- Logout revokes the access token and the refresh token
- RSA key pairs stored in Redis with their private keys encrypted with AES-256-GCM, under a key derived from `OAUTH2_KEY_ENCRYPTION_SECRET` that is never stored
- Scheduled key rotation: retired keys stay published in the JWKS until the tokens they signed expire
- Rate limiting implemented using token bucket algorithm, with a stricter limit on login attempts
- Login answers unknown emails and wrong passwords in the same way, to avoid revealing registered emails
- HTTPS support to encrypt communication between clients and the server
- Passwords are encrypted using BCrypt
- Role-based access control for different endpoints
- Input validation for all endpoints
- Comprehensive security documentation in OpenAPI spec

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
    "user": {
        "id": 1,
        "name": "John Doe",
        "email": "user@example.com",
        "role": "USER",
        "creationDate": "2026-10-15T10:30:00"
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

## 📁 Project Structure

```
├── .github/workflows/          # CI (quality checks, tests, images), deployment and health check
├── docs/
│   └── KNOWN_ISSUES.md         # Defects found in a code review and the plan that fixed them
├── mysql-init/
│   └── shopping_db.sql         # Database schema and seed data
├── src/
│   ├── main/
│   │   ├── java/com/gplanet/commerce/api/
│   │   │   ├── configs/
│   │   │   │   ├── app/
│   │   │   │   │   ├── data/
│   │   │   │   │   ├── docs/
│   │   │   │   │   ├── health/
│   │   │   │   │   └── props/
│   │   │   │   └── security/
│   │   │   │       └── ratelimiting/
│   │   │   ├── controllers/
│   │   │   ├── dtos/
│   │   │   │   ├── api/
│   │   │   │   ├── auth/
│   │   │   │   ├── jwks/
│   │   │   │   ├── product/
│   │   │   │   ├── purchase/
│   │   │   │   └── user/
│   │   │   ├── entities/
│   │   │   ├── exceptions/
│   │   │   ├── repositories/
│   │   │   ├── services/
│   │   │   └── utilities/
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── java/com/gplanet/commerce/api/
│       │   ├── configs/
│       │   ├── controllers/
│       │   ├── exceptions/
│       │   ├── integration/
│       │   ├── repositories/
│       │   ├── security/
│       │   ├── services/
│       │   └── testutils/
│       └── resources/
│           └── application-test.yml
├── docker-compose.yml          # Full stack with the published images
├── docker-compose.db.yml       # MySQL and Redis for local development
├── docker-entry.sh
├── Dockerfile
├── Dockerfile.db
└── pom.xml
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.