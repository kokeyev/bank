CREATE TABLE accounts (
account_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
user_id BIGINT NOT NULL,
card_number VARCHAR(30) NOT NULL,
cvv VARCHAR(3) NOT NULL,
expiry_date DATE NOT NULL,
balance NUMERIC(18, 2) NOT NULL DEFAULT 0,
currency_id BIGINT NOT NULL,
status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
transaction_limit NUMERIC(18, 2) NOT NULL,
name VARCHAR(100) NOT NULL,
is_main BOOLEAN NOT NULL DEFAULT FALSE,
CONSTRAINT uq_accounts_card_number UNIQUE (card_number),
CONSTRAINT chk_accounts_status CHECK (status IN ('PENDING', 'ACTIVE', 'DEACTIVATED', 'EXPIRED', 'DELETED', 'REJECTED')),
CONSTRAINT fk_accounts_user
FOREIGN KEY (user_id)
REFERENCES users(user_id),
CONSTRAINT fk_accounts_currency
FOREIGN KEY (currency_id)
REFERENCES currencies(currency_id)
);
