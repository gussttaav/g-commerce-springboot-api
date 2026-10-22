-- Demo data of the public deployment: the demo accounts, the product catalog and some purchases.
--
-- The MySQL image runs this script once, after the schema (mysql-init/shopping_db.sql), and the application
-- runs it again on the demo-reset schedule (DemoDataService), so it must leave the same data on an empty
-- database and on one that visitors have changed. Rows are deleted rather than truncated, and the demo rows
-- are inserted with fixed IDs: MySQL keeps the AUTO_INCREMENT counters, so the IDs of deleted rows are never
-- given to new ones, and a token issued to a deleted user can't act as someone who registers later.
-- Keep it valid for both MySQL and H2, where the tests run it.

DELETE FROM compra_productos;
DELETE FROM compras;
DELETE FROM productos;
DELETE FROM usuario;

-- Users with BCrypt encoded passwords; the demo accounts published in the docs are admin and john
INSERT INTO usuario (id, nombre, email, password, rol, fecha_creacion) VALUES
(1, 'Admin', 'admin@example.com', '$2a$10$vojmaj3pMOzWHVid5J9su..YSckd.RIswZ3STy5xiJkYwmAB8b67.', 'ADMIN', '2026-10-01 09:30:00'), -- Admin123!
(2, 'John Doe', 'john@example.com', '$2a$10$wMBR8BIcmw5CPy.b.ZGGv.F.8te07dT7rWwkYjsTsjMXCyd/J17x6', 'USER', '2026-10-01 09:30:00'), -- JohnDoe123!
(3, 'Jane Smith', 'jane@example.com', '$2a$10$UoAgUGPRNJ/iGghzJuhsguoBYAcp3F/7k9eC.wiS4MRWL9qBtZxsO', 'USER', '2026-10-01 09:30:00'), -- JaneSmith123!
(4, 'Bob Wilson', 'bob@example.com', '$2a$10$gT2Mo59GlO41xfw.p2UMEOrExZNIqRoyiCCKuLdM8ZGt9QDDaFlzu', 'USER', '2026-10-01 09:30:00'), -- BobWilson123!
(5, 'Alice Brown', 'alice@example.com', '$2a$10$Afi6FHFbp8EBHycNxNR2DekLBbAc00AnefRXKY5GMsa1dzbg/GyFK', 'USER', '2026-10-01 09:30:00'); -- AliceBrown123!

-- Products; Gaming Mouse is inactive
INSERT INTO productos (id, nombre, descripcion, precio, activo, fecha_creacion) VALUES
(1, 'Laptop Pro', 'High-performance laptop with 16GB RAM and 512GB SSD', 1299.99, true, '2026-10-01 09:30:00'),
(2, 'Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, true, '2026-10-01 09:30:00'),
(3, '4K Monitor', '27-inch 4K Ultra HD Monitor', 399.99, true, '2026-10-01 09:30:00'),
(4, 'Mechanical Keyboard', 'RGB mechanical keyboard with Cherry MX switches', 149.99, true, '2026-10-01 09:30:00'),
(5, 'USB-C Hub', '7-in-1 USB-C hub with HDMI and card reader', 49.99, true, '2026-10-01 09:30:00'),
(6, 'Wireless Headphones', 'Noise-cancelling wireless headphones', 199.99, true, '2026-10-01 09:30:00'),
(7, 'Webcam HD', '1080p webcam with built-in microphone', 79.99, true, '2026-10-01 09:30:00'),
(8, 'Gaming Mouse', 'High-precision gaming mouse with adjustable DPI', 89.99, false, '2026-10-01 09:30:00'),
(9, 'Tablet Pro', '10-inch tablet with stylus support', 599.99, true, '2026-10-01 09:30:00'),
(10, 'External SSD', '1TB portable SSD with USB 3.0', 159.99, true, '2026-10-01 09:30:00'),
(11, 'USB Flash Drive', '64GB USB 3.0 flash drive', 14.99, true, '2026-10-01 09:30:00'),
(12, 'Portable Charger', '10,000mAh power bank with fast charging', 24.99, true, '2026-10-01 09:30:00'),
(13, 'Wireless Charger', 'Fast wireless charger for smartphones', 19.99, true, '2026-10-01 09:30:00'),
(14, 'Bluetooth Speaker', 'Compact Bluetooth speaker with deep bass', 39.99, true, '2026-10-01 09:30:00'),
(15, 'HDMI Cable', '6ft high-speed HDMI cable', 9.99, true, '2026-10-01 09:30:00'),
(16, 'Laptop Stand', 'Adjustable aluminum laptop stand', 34.99, true, '2026-10-01 09:30:00'),
(17, 'Smart Light Bulb', 'Wi-Fi enabled smart bulb with voice control', 12.99, true, '2026-10-01 09:30:00'),
(18, 'Noise Cancelling Earbuds', 'Wireless earbuds with active noise cancellation', 49.99, true, '2026-10-01 09:30:00'),
(19, 'Phone Stand', 'Foldable phone stand for desk', 8.99, true, '2026-10-01 09:30:00'),
(20, 'Wireless Keyboard', 'Compact wireless keyboard with silent keys', 39.99, true, '2026-10-01 09:30:00'),
(21, 'Ethernet Cable', '10ft CAT6 Ethernet cable', 7.99, true, '2026-10-01 09:30:00'),
(22, 'Screen Protector', 'Tempered glass screen protector for smartphones', 6.99, true, '2026-10-01 09:30:00'),
(23, 'USB Wall Charger', 'Dual-port USB wall charger', 14.99, true, '2026-10-01 09:30:00'),
(24, 'Car Phone Mount', 'Adjustable car phone holder', 16.99, true, '2026-10-01 09:30:00'),
(25, 'Cooling Pad', 'Laptop cooling pad with dual fans', 29.99, true, '2026-10-01 09:30:00');

-- Purchases of the users john (2), jane (3), bob (4) and alice (5)
INSERT INTO compras (id, usuario_id, total, fecha) VALUES
(1, 2, 1329.98, '2026-10-02 10:30:00'),
(2, 3, 429.98, '2026-10-03 14:45:00'),
(3, 4, 279.98, '2026-10-04 16:20:00'),
(4, 2, 599.99, '2026-10-05 09:15:00'),
(5, 5, 229.98, '2026-10-06 11:30:00'),
(6, 3, 1449.98, '2026-10-07 13:45:00'),
(7, 4, 159.99, '2026-10-08 15:00:00'),
(8, 5, 129.98, '2026-10-09 17:30:00');

-- Purchase items; each subtotal is the price of the product times the quantity
INSERT INTO compra_productos (id, compra_id, producto_id, cantidad, subtotal) VALUES
(1, 1, 1, 1, 1299.99),
(2, 1, 2, 1, 29.99),
(3, 2, 3, 1, 399.99),
(4, 2, 2, 1, 29.99),
(5, 3, 6, 1, 199.99),
(6, 3, 7, 1, 79.99),
(7, 4, 9, 1, 599.99),
(8, 5, 6, 1, 199.99),
(9, 5, 2, 1, 29.99),
(10, 6, 1, 1, 1299.99),
(11, 6, 4, 1, 149.99),
(12, 7, 10, 1, 159.99),
(13, 8, 7, 1, 79.99),
(14, 8, 5, 1, 49.99);
