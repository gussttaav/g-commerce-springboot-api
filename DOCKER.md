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

- **Security & Documentation**
  - Comprehensive security implementation
  - OpenAPI/Swagger documentation
  - Input validation
  - CORS configuration

## 🚦 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Git (optional)

### Installation

1. Create a `.env` file with the required environment variables:
```env
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_HOST=localhost:3306
MYSQL_USER=your_database_user
MYSQL_PASSWORD=your_database_password
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=your_admin_password
ADMIN_NAME=Administrator
CORS_ORIGINS=http://localhost:3000
```

2. Create a `docker-compose.yml`:
```yaml
services:
  app:
    image: gussttaav/g-commerce-backend:latest
    ports:
      - "8080:8080"
    env_file:
      - .env
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/shopping
    depends_on:
      mysql:
        condition: service_healthy
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

networks:
  spring-mysql-network:
    driver: bridge
```

3. Start the application:
```bash
docker compose up -d
```

The API will be available at `http://localhost:8080`

💡 The database schema is automatically initialized - no additional setup required!

## 📚 API Documentation

Once running, access the API documentation at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

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
| `MYSQL_HOST` | Name and port for the MySQL server | localhost:3306 | Yes |
| `MYSQL_USER` | Database user | - | Yes |
| `MYSQL_PASSWORD` | Database password | - | Yes |
| `MYSQL_CHARSET` | Database charset | utf8mb4 | No |
| `MYSQL_COLLATION` | Database collation | utf8mb4_unicode_ci | No |
| `ADMIN_EMAIL` | Admin user email | - | Yes |
| `ADMIN_PASSWORD` | Admin user password | - | Yes |
| `ADMIN_NAME` | Admin user name | - | Yes |
| `CORS_ORIGINS` | Allowed CORS origins | http://localhost:3000 | Yes |

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

- **Backend Framework**: Spring Boot 3.4.1
- **Language**: Java 17
- **Security**: Spring Security with JWT
- **Database**: MySQL 9
- **Documentation**: OpenAPI 3.0 (Swagger)
- **Containerization**: Docker & Docker Compose
- **Testing**: JUnit 5, Mockito
- **Build Tool**: Maven

## 📦 Related Images

This application consists of two Docker images:
- Frontend: `gussttaav/g-commerce-frontend`
- Backend API (this image): `gussttaav/g-commerce-backend`
- Database: `gussttaav/g-commerce-mysql`

## 💻 Source Code

The complete source code is available on [GitHub](https://github.com/gussttaav/springboot-projects/tree/gestion-tienda)

## 🤝 Support

For issues and feature requests, please use the [GitHub Issues](https://github.com/gussttaav/springboot-projects/issues) page.

## 📄 License

This project is licensed under the MIT License.