INSERT INTO users (id, email, password, first_name, last_name, is_deleted)
VALUES (1000, 'customer@example.com', 'BCRYPT_HASH1', 'Customer', 'User', false),
       (1002, 'admin@example.com',    'BCRYPT_HASH2', 'Admin',    'User', false),
       (1001, 'other@example.com',    'BCRYPT_HASH3', 'Other',    'User', false);

INSERT INTO addresses (id, street_line_one, street_line_two, postal_code, city, region, room_number, latitude, longitude)
VALUES (1000, 'Main Street 10', 'Building A', '00-001', 'Warsaw', 'Masovian', '101', 52.2297, 21.0122);

INSERT INTO accommodations (id, type, address_id, size, daily_rate, availability, is_deleted)
VALUES (1000,'CHALET', 1000, '120', 150.00, 5, false),
       (1001,'CHALET', 1000, '120', 150.00, 5, false);

INSERT INTO bookings (id, check_in, check_out, accommodation_id, user_id, status, is_deleted)
VALUES (1000, '2026-12-10', '2026-12-15', 1000, 1000, 'AWAIT_PAYMENT',   false),
       (1001, '2026-12-20', '2026-12-25', 1000, 1000, 'CANCELED',  false),
       (1002, '2026-12-01', '2026-12-05', 1000, 1001, 'CONFIRMED', false),
       (1003, '2026-12-10', '2026-12-15', 1001, 1001, 'EXPIRED',   false),
       (1004, '2026-12-30', '2027-01-05', 1000, 1000, 'AWAIT_PAYMENT',   true);