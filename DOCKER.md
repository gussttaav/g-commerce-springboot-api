# Spring Boot E-commerce Backend

A robust e-commerce REST API built with Spring Boot that provides user authentication, product management, and purchase tracking functionality.

## Features

- User Management (registration, authentication, role-based access)
- Product Management (CRUD operations)
- Purchase System with shopping cart functionality
- OpenAPI/Swagger documentation
- Role-based authorization (ADMIN, USER)
- Comprehensive security implementation
- MySQL database with automatic schema initialization

## Quick Start

1. Create a `.env` file with the following variables:
```env
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_DATABASE=your_database_name
MYSQL_USER=your_database_user
MYSQL_PASSWORD=your_database_password
MYSQL_CHARSET=utf8mb4
MYSQL_COLLATION=utf8mb4_unicode_ci
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
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - spring-mysql-network

  mysql:
    image: gussttaav/g-commerce-mysql:latest
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_CHARSET: ${MYSQL_CHARSET}
      MYSQL_COLLATION: ${MYSQL_COLLATION}
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

The application will be available at `http://localhost:8080`

That's it! The database schema is automatically included in the MySQL image, so no additional setup is required.

## API Documentation

Once the application is running, you can access:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

## Main Endpoints

### User Management
```
POST /api/usuarios/registro         # Register a new user
POST /api/usuarios/login           # Login a user
PUT  /api/usuarios/perfil          # Update user profile
GET  /api/usuarios/perfil          # Get user profile
PUT  /api/usuarios/password        # Change password
```

### Product Management
```
GET    /api/productos/listar       # List all active products
POST   /api/productos/crear        # Create a new product (ADMIN)
DELETE /api/productos/eliminar/{id} # Delete a product (ADMIN)
PUT    /api/productos/actualizar/{id} # Update product (ADMIN)
```

### Purchase Management
```
POST /api/compras/nueva           # Create a new purchase
GET  /api/compras/listar          # List user purchases
```

## Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| MYSQL_ROOT_PASSWORD | MySQL root password | Yes |
| MYSQL_DATABASE | Database name | Yes |
| MYSQL_USER | Database user | Yes |
| MYSQL_PASSWORD | Database password | Yes |
| MYSQL_CHARSET | Database charset | Yes |
| MYSQL_COLLATION | Database collation | Yes |
| ADMIN_EMAIL | Admin user email | Yes |
| ADMIN_PASSWORD | Admin user password | Yes |
| ADMIN_NAME | Admin user name | Yes |
| CORS_ORIGINS | Allowed CORS origins | Yes |

## Container Management

```bash
# Start containers
docker compose up -d

# View logs
docker compose logs -f

# Stop containers
docker compose down

# Stop and remove volumes (clears database)
docker compose down -v
```

## Technologies

- Java 17
- Spring Boot 3.4.1
- Spring Security
- MySQL 9
- Docker & Docker Compose
- OpenAPI/Swagger

## Source Code

The complete source code is available on [GitHub](https://github.com/gussttaav/springboot-projects/tree/gestion-tienda)

## Support

For issues and feature requests, please use the [GitHub Issues](https://github.com/gussttaav/springboot-projects/issues) page.

## License

This project is licensed under the MIT License.
