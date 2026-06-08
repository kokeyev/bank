CREATE TABLE accounts (
account_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
user_id BIGINT NOT NULL,
card_number VARCHAR(30),
expiry_date DATE,
balance NUMERIC(18, 2) DEFAULT 0,
currency_id BIGINT NOT NULL,
status VARCHAR(50),
transaction_limit NUMERIC(18, 2),
name VARCHAR(100),
CONSTRAINT fk_accounts_user
FOREIGN KEY (user_id)
REFERENCES users(user_id),
CONSTRAINT fk_accounts_currency
FOREIGN KEY (currency_id)
REFERENCES currencies(currency_id)
);