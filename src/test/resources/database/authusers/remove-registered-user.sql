DELETE FROM users_roles
WHERE user_id IN (SELECT id FROM users WHERE email = 'new.user@example.com');
DELETE FROM users WHERE email = 'new.user@example.com';