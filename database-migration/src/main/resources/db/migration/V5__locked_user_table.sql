create table locked_user(
    user_id uuid,
    locked_at timestamp not null,
    unlock_code varchar(64) not null,
    unlocked_at timestamp,
    primary key (user_id),

    constraint fk_locked_user_user_id
        foreign key (user_id) references `user`(id)
);
