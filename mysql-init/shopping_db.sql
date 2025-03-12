
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
('Admin', 'admin@example.com', '$2a$10$vojmaj3pMOzWHVid5J9su..YSckd.RIswZ3STy5xiJkYwmAB8b67.', 'ADMIN'), -- Admin123!
('John Doe', 'john@example.com', '$2a$10$wMBR8BIcmw5CPy.b.ZGGv.F.8te07dT7rWwkYjsTsjMXCyd/J17x6', 'USER'), -- JohnDoe123!
('Jane Smith', 'jane@example.com', '$2a$10$UoAgUGPRNJ/iGghzJuhsguoBYAcp3F/7k9eC.wiS4MRWL9qBtZxsO', 'USER'), -- JaneSmith123!
('Bob Wilson', 'bob@example.com', '$2a$10$gT2Mo59GlO41xfw.p2UMEOrExZNIqRoyiCCKuLdM8ZGt9QDDaFlzu', 'USER'), -- BobWilson123!
('Alice Brown', 'alice@example.com', '$2a$10$Afi6FHFbp8EBHycNxNR2DekLBbAc00AnefRXKY5GMsa1dzbg/GyFK', 'USER'); -- AliceBrown123!

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
('External SSD', '1TB portable SSD with USB 3.0', 159.99, true),
('USB Flash Drive', '64GB USB 3.0 flash drive', 14.99, true),
('Portable Charger', '10,000mAh power bank with fast charging', 24.99, true),
('Wireless Charger', 'Fast wireless charger for smartphones', 19.99, true),
('Bluetooth Speaker', 'Compact Bluetooth speaker with deep bass', 39.99, true),
('HDMI Cable', '6ft high-speed HDMI cable', 9.99, true),
('Laptop Stand', 'Adjustable aluminum laptop stand', 34.99, true),
('Smart Light Bulb', 'Wi-Fi enabled smart bulb with voice control', 12.99, true),
('Noise Cancelling Earbuds', 'Wireless earbuds with active noise cancellation', 49.99, true),
('Phone Stand', 'Foldable phone stand for desk', 8.99, true),
('Wireless Keyboard', 'Compact wireless keyboard with silent keys', 39.99, true),
('Ethernet Cable', '10ft CAT6 Ethernet cable', 7.99, true),
('Screen Protector', 'Tempered glass screen protector for smartphones', 6.99, true),
('USB Wall Charger', 'Dual-port USB wall charger', 14.99, true),
('Car Phone Mount', 'Adjustable car phone holder', 16.99, true),
('Cooling Pad', 'Laptop cooling pad with dual fans', 29.99, true);

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