CREATE TABLE locked_users(
    index BIGSERIAL,
    user_id UUID NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    unlock_code VARCHAR(64) NOT NULL,
    unlocked_at TIMESTAMP,
    PRIMARY KEY (user_id, unlock_code),

    CONSTRAINT fk_locked_users_user_id FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX locked_users_by_user_id ON locked_users (user_id);
