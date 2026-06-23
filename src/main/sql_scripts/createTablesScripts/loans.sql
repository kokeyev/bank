CREATE TABLE loans (
loan_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
user_id BIGINT NOT NULL,
loan_type_id BIGINT NOT NULL,
parent_loan_id BIGINT,
account_id BIGINT,
remaining_amount NUMERIC(18, 2) NOT NULL,
rate NUMERIC(5, 2),
duration INT,
status VARCHAR(50) NOT NULL,
start_date DATE,
monthly_payment NUMERIC(18, 2),
CONSTRAINT chk_loans_status CHECK (status IN ('PENDING', 'OFFERED', 'REFUSED', 'ACTIVE', 'CLOSED', 'REJECTED')),
CONSTRAINT fk_loans_user
FOREIGN KEY (user_id)
REFERENCES users(user_id),
CONSTRAINT fk_loans_loan_type
FOREIGN KEY (loan_type_id)
REFERENCES loan_types(loan_type_id),
CONSTRAINT fk_loans_parent_loan
FOREIGN KEY (parent_loan_id)
REFERENCES loans(loan_id),
CONSTRAINT fk_loans_account
FOREIGN KEY (account_id)
REFERENCES accounts(account_id)
);
