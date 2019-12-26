CREATE TABLE authentication_failures(
    id UUID NOT NULL,
    index BIGSERIAL,
    user_id UUID NOT NULL,
    failed_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL,
    PRIMARY KEY (user_id, failed_at),

    CONSTRAINT fk_authentication_failures_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);
