INSERT INTO users (id, email, password, first_name, last_name, is_deleted)
VALUES (3000, 'payment.user.a@example.com', 'BCRYPT_HASH1', 'User', 'A', false),
       (3001, 'payment.user.b@example.com', 'BCRYPT_HASH2', 'User', 'B', false);

INSERT INTO addresses (id, street_line_one, street_line_two, postal_code, city, region,
                       room_number, latitude, longitude)
VALUES (3000, 'Main Street 10', 'Building A', '00-001', 'Warsaw', 'Masovian', '101',
        52.2297, 21.0122);

INSERT INTO accommodations (id, type, address_id, size, daily_rate, availability, is_deleted)
VALUES (3000, 'CHALET', 3000, '120', 150.00, 5, false);

INSERT INTO bookings (id, check_in, check_out, accommodation_id, user_id, status, is_deleted)
VALUES (3000, '2099-01-10', '2099-01-15', 3000, 3000, 'AWAIT_PAYMENT', false),
       (3001, '2099-02-10', '2099-02-15', 3000, 3000, 'CONFIRMED',     false),
       (3002, '2099-03-10', '2099-03-15', 3000, 3001, 'EXPIRED',       false);

INSERT INTO payments (id, status, booking_id, session_url, session_id, amount)
VALUES (3000, 'AWAIT_PAYMENT', 3000, 'https://checkout.stripe.test/a1', 'sess_a1', 750.00),
       (3001, 'PAID',          3001, 'https://checkout.stripe.test/a2', 'sess_a2', 750.00),
       (3002, 'EXPIRED',       3002, 'https://checkout.stripe.test/b1', 'sess_b1', 750.00);