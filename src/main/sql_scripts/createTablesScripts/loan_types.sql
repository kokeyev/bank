CREATE TABLE loan_types (
loan_type_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
name VARCHAR(100) NOT NULL,
rate NUMERIC(5, 2) NOT NULL,
duration INT NOT NULL,
minimum_amount NUMERIC(18, 2) NOT NULL,
maximum_amount NUMERIC(18, 2) NOT NULL,
currency_id BIGINT NOT NULL,
CONSTRAINT uq_loan_types_product_currency UNIQUE (name, currency_id),
CONSTRAINT chk_loan_types_rate_positive CHECK (rate > 0),
CONSTRAINT chk_loan_types_duration_positive CHECK (duration > 0),
CONSTRAINT chk_loan_types_amount_range CHECK (minimum_amount > 0 AND maximum_amount >= minimum_amount),
CONSTRAINT fk_loan_types_currency
FOREIGN KEY (currency_id)
REFERENCES currencies(currency_id)
);
