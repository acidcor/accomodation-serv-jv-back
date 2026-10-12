INSERT INTO users (id, email, password, first_name, last_name, is_deleted)
VALUES (1000, 'existing.user@example.com', '$2a$12$eM2ImUu.LZXd7TpFyj3MPu48eyhokCXJwLL.e6STdAwJWB8FcRtY6',
        'Existing', 'User', false);

INSERT INTO users_roles (user_id, role_id)
VALUES (1000, (SELECT id FROM roles WHERE name = 'CUSTOMER'));