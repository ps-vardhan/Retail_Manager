-- Initial Products
INSERT INTO product (name, sku, price, description) 
VALUES ('Laptop Pro', 'TECH-001', 1200.00, 'High performance laptop')
ON CONFLICT DO NOTHING;

INSERT INTO product (name, sku, price, description) 
VALUES ('Wireless Mouse', 'TECH-002', 25.00, 'Ergonomic mouse')
ON CONFLICT DO NOTHING;


INSERT INTO inventory (product_id,quantity,location)
values ((SELECT id FROM product WHERE sku='TECH-001'),50,'WareHouse A') ON CONFLICT DO NOTHING;