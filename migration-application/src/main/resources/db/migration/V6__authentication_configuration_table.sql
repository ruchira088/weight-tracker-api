CREATE TABLE authentication_configuration(
    index BIGSERIAL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    password VARCHAR(2047) NOT NULL,
    totp_secret VARCHAR(255),
    PRIMARY KEY (user_id),

    CONSTRAINT fk_authentication_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);
