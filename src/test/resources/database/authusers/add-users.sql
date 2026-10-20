INSERT INTO users (id, email, password, first_name, last_name, is_deleted)
VALUES (1000, 'customer@example.com', 'BCRYPT_HASH1', 'Customer', 'User', false),
       (1001, 'admin@example.com',    'BCRYPT_HASH2', 'Admin',    'User', false),
       (1002, 'other@example.com',    'BCRYPT_HASH3', 'Other',    'User', false);

INSERT INTO users_roles (user_id, role_id)
VALUES (1000, (SELECT id FROM roles WHERE name = 'CUSTOMER')),
       (1001, (SELECT id FROM roles WHERE name = 'ADMIN')),
       (1002, (SELECT id FROM roles WHERE name = 'CUSTOMER'));