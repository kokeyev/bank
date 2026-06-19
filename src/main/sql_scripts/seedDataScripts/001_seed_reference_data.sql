-- Reference and demo data for local development.
-- The script is idempotent for currencies, products, users, and the demo account.

INSERT INTO currencies (name, rate_to_kzt)
VALUES
  ('KZT', 1.000000),
  ('USD', 500.000000),
  ('EUR', 540.000000),
  ('RUB', 5.500000)
ON CONFLICT (name) DO UPDATE
SET rate_to_kzt = EXCLUDED.rate_to_kzt;

INSERT INTO deposit_types (name, rate, duration, withdrawal, minimum_amount, currency_id)
VALUES
  ('Копилка', 17.00, 6, TRUE, 10000.00, (SELECT currency_id FROM currencies WHERE name = 'KZT')),
  ('Копилка', 1.00, 6, TRUE, 50.00, (SELECT currency_id FROM currencies WHERE name = 'USD')),
  ('Копилка', 0.80, 6, TRUE, 50.00, (SELECT currency_id FROM currencies WHERE name = 'EUR')),
  ('Копилка', 1.00, 6, TRUE, 5000.00, (SELECT currency_id FROM currencies WHERE name = 'RUB')),
  ('Стратегия', 19.00, 12, FALSE, 50000.00, (SELECT currency_id FROM currencies WHERE name = 'KZT')),
  ('Стратегия', 1.20, 12, FALSE, 100.00, (SELECT currency_id FROM currencies WHERE name = 'USD')),
  ('Стратегия', 1.00, 12, FALSE, 100.00, (SELECT currency_id FROM currencies WHERE name = 'EUR')),
  ('Стратегия', 1.50, 12, FALSE, 10000.00, (SELECT currency_id FROM currencies WHERE name = 'RUB')),
  ('Капитал', 20.00, 24, FALSE, 100000.00, (SELECT currency_id FROM currencies WHERE name = 'KZT')),
  ('Капитал', 1.20, 24, FALSE, 500.00, (SELECT currency_id FROM currencies WHERE name = 'USD')),
  ('Капитал', 0.90, 24, FALSE, 500.00, (SELECT currency_id FROM currencies WHERE name = 'EUR')),
  ('Капитал', 1.75, 24, FALSE, 50000.00, (SELECT currency_id FROM currencies WHERE name = 'RUB'))
ON CONFLICT (name, currency_id, duration) DO UPDATE
SET rate = EXCLUDED.rate,
    withdrawal = EXCLUDED.withdrawal,
    minimum_amount = EXCLUDED.minimum_amount;

INSERT INTO loan_types (name, rate, duration, minimum_amount, maximum_amount, currency_id)
VALUES
  ('На любые цели', 24.00, 60, 50000.00, 7000000.00, (SELECT currency_id FROM currencies WHERE name = 'KZT')),
  ('Автокредит', 18.00, 84, 500000.00, 30000000.00, (SELECT currency_id FROM currencies WHERE name = 'KZT')),
  ('Ипотека', 16.00, 240, 1000000.00, 100000000.00, (SELECT currency_id FROM currencies WHERE name = 'KZT'))
ON CONFLICT (name, currency_id) DO UPDATE
SET rate = EXCLUDED.rate,
    duration = EXCLUDED.duration,
    minimum_amount = EXCLUDED.minimum_amount,
    maximum_amount = EXCLUDED.maximum_amount;

INSERT INTO users (name, surname, phone_number, email_address, role, status, date_created, password_hash)
VALUES
  ('System', 'Admin', '+77000000001', 'admin@openbank.kz', 'ADMIN', 'ACTIVE', CURRENT_DATE, 'b3BlbmJhbmstYWRtaW4tMQ==:0znjrZRYed9lXTicAeI9JMmMZiplCl74tkRk6a7v8U0='),
  ('Aruzhan', 'Sadyk', '+77001112233', 'client@openbank.kz', 'CLIENT', 'ACTIVE', CURRENT_DATE, 'b3BlbmJhbmstY2xpZW50LTE=:SKVlGgp4gC6IM048L5wBLo4tGE1iX1a5W4CUqxn21gQ='),
  ('Dias', 'Manager', '+77002223344', 'manager@openbank.kz', 'MANAGER', 'ACTIVE', CURRENT_DATE, 'b3BlbmJhbmstbWFuYWdlcg==:es2+Dnp8UIoXvFdmfMCiav8JNZx4wqTBheMFEzw4JOo=')
ON CONFLICT (email_address) DO UPDATE
SET name = EXCLUDED.name,
    surname = EXCLUDED.surname,
    phone_number = EXCLUDED.phone_number,
    role = EXCLUDED.role,
    status = EXCLUDED.status,
    password_hash = EXCLUDED.password_hash;

INSERT INTO accounts (user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main)
SELECT
  u.user_id,
  '4000000000000002',
  '123',
  DATE '2030-01-01',
  250000.00,
  c.currency_id,
  'ACTIVE',
  1000000.00,
  'Main KZT',
  TRUE
FROM users u
JOIN currencies c ON c.name = 'KZT'
WHERE u.email_address = 'client@openbank.kz'
  AND NOT EXISTS (
    SELECT 1
    FROM accounts a
    WHERE a.card_number = '4000000000000002'
  );
