create table reset_password_token(
    index bigserial,
    secret varchar(255),
    user_id uuid,
    created_at timestamp not null,
    expires_at timestamp not null,
    password_set_at timestamp,

    constraint pk_reset_password primary key (user_id, secret),

    constraint fk_reset_password_user_id
        foreign key (user_id) references users(id)
);