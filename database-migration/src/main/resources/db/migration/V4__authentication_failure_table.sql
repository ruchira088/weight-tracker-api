create table authentication_failure(
    id uuid,
    user_id uuid not null,
    failure_at timestamp not null,
    primary key (id),

    constraint fk_authentication_failure_user_id
        foreign key (user_id) references `user`(id)
);
