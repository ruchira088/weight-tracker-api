create table locked_users(
    index bigserial,
    user_id uuid not null,
    locked_at timestamp not null,
    unlock_code varchar(64) not null,
    unlocked_at timestamp,
    primary key (user_id, unlock_code),

    constraint fk_locked_users_user_id
        foreign key (user_id) references users(id)
);
