CREATE TABLE currencies (
currency_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
name VARCHAR(100) NOT NULL,
rate_to_kzt NUMERIC(18, 6) NOT NULL,
CONSTRAINT uq_currencies_name UNIQUE (name),
CONSTRAINT chk_currencies_rate_positive CHECK (rate_to_kzt > 0)
);
