# G-Commerce Backend API

A robust e-commerce REST API built with Spring Boot that provides comprehensive user authentication, product management, and purchase tracking functionality.

## 🚀 Features

- **User Management**
  - Secure user registration and authentication
  - Role-based authorization (ADMIN, USER)
  - Profile management and password updates
  - Basic authentication

- **Product Management**
  - Complete CRUD operations
  - Product status management (active/inactive)
  - Role-based access control
  - Stock management

- **Purchase System**
  - Shopping cart functionality
  - Order tracking
  - Purchase history
  - Transaction management

- **Security & SSL Support**
  - Automatic profile detection based on available SSL certificate
  - HTTPS configuration with SSL keystore
  - Environment-based profile activation
  - Secure authentication with Spring Security

- **Comprehensive Documentation**
  - Javadoc for code-level documentation
  - OpenAPI (Swagger) for interactive API documentation
  - Detailed endpoint descriptions and schemas

## 🚦 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Git (optional)

### Installation

1- Create a `.env` file with the required environment variables:
```env
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_USER=your_database_user
MYSQL_PASSWORD=your_database_password
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=your_admin_password
```

#### HTTPS support (optional)

**If you need the application to run with HTTPS**, you must add the following environment variables in your `.env` file and provide a valid SSL certificate in a `certs/` directory.

```env
SPRING_PROFILES_ACTIVE=https
SSL_PASSWORD=your_ssl_password
```


2- Create a `docker-compose.yml`:
```yaml
# Full deployment configuration - both app and database
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
    volumes:
      - ./certs:/certs
      - app-logs:/app/logs
    networks:
      - spring-mysql-network

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
      - spring-mysql-network

volumes:
  mysql-data:
  app-logs:

networks:
  spring-mysql-network:
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
- **Swagger UI**: `http://localhost:8080/swagger-ui.html` or `https://localhost:8443/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs` or `https://localhost:8443/v3/api-docs`

## 🔌 Main Endpoints

### Authentication & Users
```http
POST /api/usuarios/registro         # Register new user
POST /api/usuarios/login           # User login
GET  /api/usuarios/perfil          # Get user profile
PUT  /api/usuarios/perfil          # Update profile
PUT  /api/usuarios/password        # Change password
```

### Products
```http
GET    /api/productos/listar             # List products
GET    /api/productos/{id}               # Get product details
POST   /api/productos/crear              # Create product (ADMIN)
PUT    /api/productos/actualizar/{id}    # Update product (ADMIN)
DELETE /api/productos/eliminar/{id}      # Delete product (ADMIN)
```

### Purchases
```http
POST /api/compras/nueva            # Create purchase
GET  /api/compras/listar          # List user purchases
GET  /api/compras/{id}            # Get purchase details
```

## ⚙️ Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `MYSQL_ROOT_PASSWORD` | MySQL root password | - | Yes |
| `MYSQL_USER` | Database user | - | Yes |
| `MYSQL_PASSWORD` | Database password | - | Yes |
| `MYSQL_CHARSET` | Database charset | utf8mb4 | No |
| `MYSQL_COLLATION` | Database collation | utf8mb4_unicode_ci | No |
| `ADMIN_EMAIL` | Admin user email | - | Yes |
| `ADMIN_PASSWORD` | Admin user password | - | Yes |
| `SPRING_PROFILES_ACTIVE` | Set "https" to enable SSL, "http" to disable | http | No |
| `SSL_PASSWORD` | Password for SSL keystore | - | No (Only for HTTPS) |
| `LOG_PATH` | Application log path | /app/logs | No |
| `LOG_ARCHIVE` | Archived logs path | /app/logs/archive | No |

## 🐳 Container Management

```bash
# Start services
docker compose up -d

# View logs
docker compose logs -f

# Stop services
docker compose down

# Stop and remove volumes (clears database)
docker compose down -v

# Check service status
docker compose ps
```

## 🛠️ Technologies

- **Backend Framework**: Spring Boot 3.4.3
- **Language**: Java 17
- **Security**: Spring Security with Basic Authentication
- **Database**: MySQL 9
- **Documentation**: OpenAPI 3.0 (Swagger)
- **Containerization**: Docker & Docker Compose
- **Testing**: JUnit 5, Mockito
- **Build Tool**: Maven

## 📦 Related Images

This application consists of three Docker images:
- Frontend: `gussttaav/g-commerce-frontend`
- Backend API (this image): `gussttaav/g-commerce-backend`
- Database: `gussttaav/g-commerce-mysql`

## 💻 Source Code

The complete source code is available on [GitHub](https://github.com/gussttaav/springboot-projects/tree/gestion-tienda)

## 🤝 Support

For issues and feature requests, please use the [GitHub Issues](https://github.com/gussttaav/springboot-projects/issues) page.

## 📄 License

This project is licensed under the MIT License.