create table authentication_failures(
    id uuid not null,
    index bigserial,
    user_id uuid not null,
    failed_at timestamp not null,
    deleted boolean not null,
    primary key (user_id, failed_at),

    constraint fk_authentication_failures_user_id
        foreign key (user_id) references users(id)
);
