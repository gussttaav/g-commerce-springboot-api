# Spring Boot E-commerce API

A RESTful API built with Spring Boot that implements a basic e-commerce system with user authentication, product management, and purchase tracking.

## 🚀 Features

- **User Management**
  - User registration and authentication
  - Role-based authorization (ADMIN, USER)
  - Profile management and password updates

- **Product Management**
  - Product creation, update, delete and listing
  - Product status management (active/inactive)
  - Role-based access control for product operations

- **Purchase System**
  - Shopping cart functionality
  - Purchase history tracking
  - Role-specific purchase restrictions

- **Comprehensive Documentation**
  - Javadoc for code-level documentation
  - OpenAPI (Swagger) for interactive API documentation
  - Detailed endpoint descriptions and schemas

- **Comprehensive Testing**
  - Unit tests for all layers
  - Security tests
  - Mock MVC tests for controllers
  - In-memory H2 database for testing

## 🛠️ Technologies

- Java 17
- Spring Boot 3.4.1
- Spring Security
- OpenAPI/Swagger
- Spring Data JPA
- MySQL 9
- H2 Database (for testing)
- JUnit 5
- Mockito
- Docker & Docker Compose
- Maven
- Lombok

## 📋 Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Maven
- Git

## 🔧 Installation

1. Clone the repository:
   ```bash
   git clone --branch gestion-tienda --single-branch https://github.com/gussttaav/springboot-projects.git
   cd gestion-tienda
   ```

2. Create a `.env` file in the root directory with the following variables:
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
   CORS_ORIGINS=http://localhost:3000,other_allowed_origins
   ```

3. Start the MySQL database using Docker Compose:
   ```bash
   docker-compose up -d
   ```

4. Build and run the application:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

The application will be available at `http://localhost:8080`

## 📚 API Documentation

### OpenAPI (Swagger) Documentation

The project uses SpringDoc OpenAPI for interactive API documentation:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
  - Interactive documentation of all API endpoints
  - Ability to test endpoints directly from the browser
  - Detailed request and response schemas

- **API Docs JSON**: `http://localhost:8080/v3/api-docs`
  - Machine-readable OpenAPI specification
  - Can be used for client code generation

### Javadoc

Comprehensive Javadoc documentation is available for all classes.

To generate Javadoc:
```bash
mvn javadoc:javadoc
```

## 🧪 Testing

### Test Configuration
The project uses H2 in-memory database for testing.

### Test Categories
- **Controller Tests**: Using MockMvc to test HTTP endpoints
- **Service Tests**: Unit tests for business logic
- **Repository Tests**: Database operation tests
- **Security Tests**: Authentication and authorization tests
- **Integration Tests**: End-to-end functionality tests

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UsuarioControllerTest

# Run tests with coverage report
mvn verify
```

## 🔑 API Endpoints

### User Management
```
POST /api/usuarios/registro          # Register a new user
POST /api/usuarios/login             # Login a user
POST /api/usuarios/admin/registro    # Register a new admin (requires ADMIN role)
PUT  /api/usuarios/perfil           # Update user profile
PUT  /api/usuarios/password         # Change password
```

### Product Management
```
GET    /api/productos/listar               # List all active products

# As ADMIN user:
GET /api/productos/listar?status=INACTIVE  # Returns inactive products
GET /api/productos/listar?status=ALL       # Returns all products

POST   /api/productos/crear                # Create a new product (ADMIN only)
DELETE /api/productos/eliminar/{id}        # Delete a product (ADMIN only)
PUT /api/productos/actualizar/{id}         # Update a product information (ADMIN only)
```

### Purchase Management
```
POST /api/compras/nueva              # Create a new purchase
GET  /api/compras/listar             # List user purchases
```

## 🔒 Security

- Basic authentication is implemented using Spring Security
- Passwords are encrypted using BCrypt
- Role-based access control for different endpoints
- Input validation for all endpoints
- Comprehensive security documentation in OpenAPI spec

## 📝 Example Requests

### Register a User
```bash
POST /api/usuarios/registro
Content-Type: application/json

{
    "nombre": "John Doe",
    "email": "john@example.com",
    "contraseña": "password123"
}
```

### Create a Product (Admin only)
```bash
POST /api/productos/crear
Authorization: Basic base64(admin@example.com:password)
Content-Type: application/json

{
    "nombre": "Product Name",
    "descripcion": "Product Description",
    "precio": 99.99
}
```

### Make a Purchase
```bash
POST /api/compras/nueva
Authorization: Basic base64(user@example.com:password)
Content-Type: application/json

{
    "productos": [
        {
            "productoId": 1,
            "cantidad": 2
        }
    ]
}
```

## 📁 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/mitienda/gestion_tienda/
│   │       ├── configs/
│   │       ├── controllers/
│   │       ├── dtos/
│   │       ├── entities/
│   │       ├── exceptions/
│   │       ├── repositories/
│   │       ├── services/
│   │       └── utilities/
│   └── resources/
│       └── application.yml
├── test/
│   ├── java/
│   │   └── com/mitienda/gestion_tienda/
│   │       ├── controllers/
│   │       ├── repositories/
│   │       ├── services/
│   │       ├── security/
│   │       └── integration/
│   └── resources/
│       └── application-test.properties
├── docker-compose.yml
├── mysql-init/
│   └── shopping_db.sql
└── pom.xml
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.