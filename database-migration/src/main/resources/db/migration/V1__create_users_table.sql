create table users(
    id uuid,
    index bigserial,
    created_at timestamp not null,
    email varchar(255) not null unique,
    password varchar(2047) not null,
    first_name varchar(255) not null,
    last_name varchar(255),
    deleted boolean not null,
    primary key (id)
);
