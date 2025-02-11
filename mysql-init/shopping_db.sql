DROP DATABASE IF EXISTS shopping;

CREATE DATABASE shopping
CHARACTER SET utf8mb4
COLLATE utf8mb4_spanish_ci;

USE shopping;

-- Tabla Usuario
CREATE TABLE usuario (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol ENUM('ADMIN', 'USER') NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla Productos
CREATE TABLE productos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(200) UNIQUE NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10, 2) NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activo BOOLEAN DEFAULT TRUE
);

-- Tabla Compras
CREATE TABLE compras (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

-- Tabla Compra_Productos
CREATE TABLE compra_productos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    compra_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (compra_id) REFERENCES compras(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Insert users with BCrypt encoded passwords
INSERT INTO usuario (nombre, email, password, rol) VALUES
('Admin User', 'admin@example.com', '$2a$10$rHGJINmgN1UjmMy2NMO8OuhsODwYQl6QBZuQAm90AbtoYj2uIV4TW', 'ADMIN'), -- Admin123!
('John Doe', 'john@example.com', '$2a$10$bXhXNQz7GQg0sV3fzfzQR.1rVfw.puYm6VGJ8JNcmsz3KydUGHWzy', 'USER'), -- JohnDoe2024
('Jane Smith', 'jane@example.com', '$2a$10$PQ0AH2gV8rHF7g1ylL/9TOPh0w7dCJMmK8Ec6WsqQAu7Y9WKcXenim', 'USER'), -- JaneSmith2024
('Bob Wilson', 'bob@example.com', '$2a$10$D/IX7DvYVxB3qZKiLBxtR.vEHgjL0HII4h2SqVr5/P7kJFuGD.wtK', 'USER'), -- BobWilson2024
('Alice Brown', 'alice@example.com', '$2a$10$zG2Z4NkqHQz0LY3FnQXJWekQ8ULwwj3gmFAJD1tOjkuxXYH0.9mBG', 'USER'); -- AliceBrown2024

-- Insert products
INSERT INTO productos (nombre, descripcion, precio, activo) VALUES
('Laptop Pro', 'High-performance laptop with 16GB RAM and 512GB SSD', 1299.99, true),
('Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, true),
('4K Monitor', '27-inch 4K Ultra HD Monitor', 399.99, true),
('Mechanical Keyboard', 'RGB mechanical keyboard with Cherry MX switches', 149.99, true),
('USB-C Hub', '7-in-1 USB-C hub with HDMI and card reader', 49.99, true),
('Wireless Headphones', 'Noise-cancelling wireless headphones', 199.99, true),
('Webcam HD', '1080p webcam with built-in microphone', 79.99, true),
('Gaming Mouse', 'High-precision gaming mouse with adjustable DPI', 89.99, false),
('Tablet Pro', '10-inch tablet with stylus support', 599.99, true),
('External SSD', '1TB portable SSD with USB 3.0', 159.99, true);

-- Insert purchases
INSERT INTO compras (usuario_id, total, fecha) VALUES
(2, 1329.98, '2024-01-15 10:30:00'),
(3, 449.98, '2024-01-16 14:45:00'),
(4, 279.98, '2024-01-17 16:20:00'),
(2, 599.99, '2024-01-18 09:15:00'),
(5, 229.98, '2024-01-19 11:30:00'),
(3, 1499.98, '2024-01-20 13:45:00'),
(4, 159.99, '2024-01-21 15:00:00'),
(5, 349.98, '2024-01-22 17:30:00');

-- Insert purchase details
INSERT INTO compra_productos (compra_id, producto_id, cantidad, subtotal) VALUES
(1, 1, 1, 1299.99),
(1, 2, 1, 29.99),
(2, 3, 1, 399.99),
(2, 2, 1, 49.99),
(3, 6, 1, 199.99),
(3, 7, 1, 79.99),
(4, 9, 1, 599.99),
(5, 6, 1, 199.99),
(5, 2, 1, 29.99),
(6, 1, 1, 1299.99),
(6, 4, 1, 199.99),
(7, 10, 1, 159.99),
(8, 7, 1, 79.99),
(8, 5, 1, 269.99);