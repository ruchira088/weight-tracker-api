CREATE TABLE users(
    id UUID,
    index BIGSERIAL,
    created_at TIMESTAMP NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(2047) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255),
    deleted BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX users_by_email ON users (email);
