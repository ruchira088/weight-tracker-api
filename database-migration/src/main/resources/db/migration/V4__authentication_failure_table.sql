create table authentication_failure(
    index bigserial,
    user_id uuid not null,
    failed_at timestamp not null,
    primary key (user_id, failed_at),

    constraint fk_authentication_failure_user_id
        foreign key (user_id) references `user`(id)
);
