CREATE TABLE weight_entries(
    id UUID,
    index BIGSERIAL,
    created_at TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    user_id UUID NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    description TEXT,
    deleted BOOLEAN NOT NULL,
    PRIMARY KEY (id),

    CONSTRAINT fk_weight_entries_user_id FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_weight_entries_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX weight_entries_by_user_id ON weight_entries (user_id);
