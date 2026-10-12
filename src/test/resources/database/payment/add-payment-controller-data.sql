INSERT INTO users (id, email, password, first_name, last_name, is_deleted)
VALUES (4000, 'payment.customer@example.com', 'BCRYPT_HASH1', 'Customer', 'User', false),
       (4001, 'payment.other@example.com',    'BCRYPT_HASH2', 'Other',    'User', false),
       (4002, 'payment.admin@example.com',    'BCRYPT_HASH3', 'Admin',    'User', false);

INSERT INTO users_roles (user_id, role_id)
VALUES (4000, (SELECT id FROM roles WHERE name = 'CUSTOMER')),
       (4001, (SELECT id FROM roles WHERE name = 'CUSTOMER')),
       (4002, (SELECT id FROM roles WHERE name = 'ADMIN'));

INSERT INTO addresses (id, street_line_one, street_line_two, postal_code, city, region,
                       room_number, latitude, longitude)
VALUES (4000, 'Main Street 10', 'Building A', '00-001', 'Warsaw', 'Masovian', '101',
        52.2297, 21.0122);

INSERT INTO accommodations (id, type, address_id, size, daily_rate, availability, is_deleted)
VALUES (4000, 'CHALET', 4000, '120', 150.00, 5, false);

INSERT INTO bookings (id, check_in, check_out, accommodation_id, user_id, status, is_deleted)
VALUES (4000, '2099-01-10', '2099-01-15', 4000, 4000, 'AWAIT_PAYMENT', false),
       (4001, '2099-02-10', '2099-02-15', 4000, 4000, 'AWAIT_PAYMENT', false),
       (4002, '2099-03-10', '2099-03-15', 4000, 4000, 'CONFIRMED',     false),
       (4003, '2099-04-10', '2099-04-15', 4000, 4000, 'EXPIRED',       false),
       (4004, '2099-05-10', '2099-05-15', 4000, 4001, 'AWAIT_PAYMENT', false);

INSERT INTO payments (id, status, booking_id, session_url, session_id, amount)
VALUES (4001, 'AWAIT_PAYMENT', 4001, 'https://checkout.stripe.test/c1', 'sess_c1', 750.00),
       (4002, 'PAID',          4002, 'https://checkout.stripe.test/c2', 'sess_c2', 750.00),
       (4003, 'EXPIRED',       4003, 'https://checkout.stripe.test/c3', 'sess_c3', 750.00),
       (4004, 'AWAIT_PAYMENT', 4004, 'https://checkout.stripe.test/o1', 'sess_o1', 750.00);