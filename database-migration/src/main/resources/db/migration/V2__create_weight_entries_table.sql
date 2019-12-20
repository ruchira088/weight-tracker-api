create table weight_entries(
    id uuid,
    index bigserial,
    created_at timestamp not null,
    created_by uuid not null,
    user_id uuid not null,
    timestamp timestamp not null,
    weight double precision not null,
    description text,
    deleted boolean not null,
    primary key (id),

    constraint fk_weight_entries_user_id
        foreign key (user_id) references users(id),

    constraint fk_weight_entries_created_by
        foreign key (created_by) references users(id)
);
