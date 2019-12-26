CREATE TABLE reset_password_tokens(
    index BIGSERIAL,
    secret VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    password_set_at TIMESTAMP,
    PRIMARY KEY (user_id, secret),

    CONSTRAINT fk_reset_password_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);
