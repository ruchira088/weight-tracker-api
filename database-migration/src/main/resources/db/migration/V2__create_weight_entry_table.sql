create table weight_entry(
    id uuid,
    index bigserial,
    created_at timestamp not null,
    created_by uuid not null,
    user_id uuid not null,
    weight double precision not null,
    description text,
    primary key (id),

    constraint fk_weight_entry_user_id
        foreign key (user_id) references users(id),

    constraint fk_weight_entry_created_by
        foreign key (created_by) references users(id)
);
