CREATE TABLE users (
user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
name VARCHAR(100),
surname VARCHAR(100),
phone_number VARCHAR(30),
email_address VARCHAR(255),
role VARCHAR(50),
status VARCHAR(50),
date_created DATE DEFAULT CURRENT_DATE,
date_modified DATE
);

ALTER TABLE users
ADD COLUMN password_hash VARCHAR(255);