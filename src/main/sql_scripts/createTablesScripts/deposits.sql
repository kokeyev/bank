CREATE TABLE deposits (
deposit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
user_id BIGINT NOT NULL,
deposit_type_id BIGINT NOT NULL,
reinvest_interest BOOLEAN DEFAULT FALSE,
status VARCHAR(50),
start_date DATE,
current_amount NUMERIC(18, 2),
CONSTRAINT fk_deposits_user
FOREIGN KEY (user_id)
REFERENCES users(user_id),
CONSTRAINT fk_deposits_deposit_type
FOREIGN KEY (deposit_type_id)
REFERENCES deposit_types(deposit_type_id)
);