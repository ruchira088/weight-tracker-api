create table reset_password_tokens(
    index bigserial,
    secret varchar(255),
    user_id uuid,
    created_at timestamp not null,
    expires_at timestamp not null,
    password_set_at timestamp,
    primary key (user_id, secret),

    constraint fk_reset_password_tokens_user_id
        foreign key (user_id) references users(id)
);
