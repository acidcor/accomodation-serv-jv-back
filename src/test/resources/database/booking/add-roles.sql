INSERT INTO users_roles (user_id, role_id)
VALUES (1000, (SELECT id FROM roles WHERE name = 'CUSTOMER')),
       (1002, (SELECT id FROM roles WHERE name = 'ADMIN')),
       (1001, (SELECT id FROM roles WHERE name = 'CUSTOMER'));