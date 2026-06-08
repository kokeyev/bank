CREATE TABLE loans (
loan_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
user_id BIGINT NOT NULL,
loan_type_id BIGINT NOT NULL,
remaining_amount NUMERIC(18, 2),
status VARCHAR(50),
start_date DATE,
monthly_payment NUMERIC(18, 2),
CONSTRAINT fk_loans_user
FOREIGN KEY (user_id)
REFERENCES users(user_id),
CONSTRAINT fk_loans_loan_type
FOREIGN KEY (loan_type_id)
REFERENCES loan_types(loan_type_id)
);