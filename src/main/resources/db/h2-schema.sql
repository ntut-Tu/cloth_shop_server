CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    account VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL,
    email VARCHAR(200) NOT NULL,
    is_disable BOOLEAN NOT NULL DEFAULT FALSE
);


ALTER TABLE users
ADD COLUMN user_type VARCHAR(20) NOT NULL
    CHECK (user_type IN ('admin', 'customer', 'vendor'));

ALTER TABLE users
ALTER COLUMN is_disable SET DEFAULT FALSE;
