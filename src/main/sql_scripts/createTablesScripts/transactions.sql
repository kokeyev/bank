CREATE TABLE transactions (
transaction_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
sender_account_id BIGINT,
receiver_account_id BIGINT,
transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
amount NUMERIC(18, 2) NOT NULL,
currency_id BIGINT NOT NULL,
fee NUMERIC(18, 2) NOT NULL DEFAULT 0,
message VARCHAR(255),
transaction_type VARCHAR(50) NOT NULL,
CONSTRAINT fk_transactions_sender_account
FOREIGN KEY (sender_account_id)
REFERENCES accounts(account_id),
CONSTRAINT fk_transactions_receiver_account
FOREIGN KEY (receiver_account_id)
REFERENCES accounts(account_id),
CONSTRAINT fk_transactions_currency
FOREIGN KEY (currency_id)
REFERENCES currencies(currency_id)
);
