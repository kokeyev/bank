CREATE TABLE deposit_types (
deposit_type_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
name VARCHAR(100) NOT NULL,
rate NUMERIC(5, 2) NOT NULL,
duration INT NOT NULL,
withdrawal BOOLEAN DEFAULT FALSE,
minimum_amount NUMERIC(18, 2),
currency_id BIGINT NOT NULL,
CONSTRAINT fk_deposit_types_currency
FOREIGN KEY (currency_id)
REFERENCES currencies(currency_id)
);