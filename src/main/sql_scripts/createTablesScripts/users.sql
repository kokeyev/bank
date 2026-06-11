CREATE TABLE users (
user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
name VARCHAR(100) NOT NULL,
surname VARCHAR(100) NOT NULL,
phone_number VARCHAR(30) NOT NULL,
email_address VARCHAR(255) NOT NULL,
role VARCHAR(50) NOT NULL DEFAULT 'CLIENT',
status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
date_created DATE NOT NULL DEFAULT CURRENT_DATE,
date_modified DATE,
password_hash VARCHAR(255) NOT NULL,
CONSTRAINT uq_users_phone_number UNIQUE (phone_number),
CONSTRAINT uq_users_email_address UNIQUE (email_address),
CONSTRAINT chk_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'DEACTIVATED', 'DELETED'))
);
