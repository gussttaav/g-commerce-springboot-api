# G-Commerce Database Image

MySQL database image pre-configured with the schema for the G-Commerce E-commerce application.

## 🚀 Features

- Pre-configured MySQL 9 database
- Complete e-commerce schema with users, products, and purchases
- Spanish collation (utf8mb4_spanish_ci)
- Automatic table creation and structure initialization
- Built-in foreign key relationships
- Default timestamps for tracking

## 🔧 Environment Variables

Required environment variables:
- `MYSQL_ROOT_PASSWORD`: Root password for MySQL
- `MYSQL_USER`: Database user
- `MYSQL_PASSWORD`: Database user password

Default environment variables:
- `MYSQL_CHARSET`: utf8mb4
- `MYSQL_COLLATION`: utf8mb4_spanish_ci
- `TZ`: UTC

## 📦 Usage

### Standalone Usage

```bash
docker run -d \
  -e MYSQL_ROOT_PASSWORD=your_root_password \
  -e MYSQL_USER=your_user \
  -e MYSQL_PASSWORD=your_password \
  -v mysql-data:/var/lib/mysql \
  -p 3306:3306 \
  gussttaav/g-commerce-mysql:latest
```

### With G-Commerce Backend

Use with docker-compose (recommended):

```yaml
services:
  mysql:
    image: gussttaav/g-commerce-mysql:latest
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"
```

## 💾 Data Persistence

The database data is stored in the `/var/lib/mysql` directory inside the container. To persist the data, mount a volume to this location as shown in the examples above.

## 📄 Schema Details

### Database Configuration
```sql
DATABASE NAME: shopping
CHARACTER SET: utf8mb4
COLLATION: utf8mb4_spanish_ci
TZ: UTC -- Time zone
```

### Tables Structure

#### usuario (Users)
```sql
CREATE TABLE usuario (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol ENUM('ADMIN', 'USER') NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
- Stores user information
- Supports two roles: ADMIN and USER
- Automatic timestamp for user creation
- Unique email constraint

#### productos (Products)
```sql
CREATE TABLE productos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(200) UNIQUE NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10, 2) NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE
);
```
- Product catalog information
- Unique product names
- Support for active/inactive products
- Price with 2 decimal places
- Automatic timestamp for product creation

#### compras (Purchases)
```sql
CREATE TABLE compras (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
```
- Tracks purchase transactions
- Links to user who made the purchase
- Automatic timestamp for purchase date
- Stores total purchase amount

#### compra_productos (Purchase Details)
```sql
CREATE TABLE compra_productos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    compra_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (compra_id) REFERENCES compras(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);
```
- Junction table for purchase-product relationship
- Tracks quantity and subtotal for each product in a purchase
- Maintains referential integrity with purchases and products

## 🔒 Security Notes

- Always use strong passwords in production
- Consider restricting port access in production environments
- The schema includes password hashing support (VARCHAR(255) for hashed passwords)
- Role-based access control is built into the user table

## 🤝 Related Images

This image is part of the G-Commerce application suite:
- Frontend: `gussttaav/g-commerce-frontend`
- Backend API: `gussttaav/g-commerce-backend`
- Database (this image): `gussttaav/g-commerce-mysql`
