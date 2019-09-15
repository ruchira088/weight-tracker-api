create table users(
    id uuid,
    index bigserial,
    created_at timestamp not null,
    username varchar(255) not null unique,
    password varchar(2047) not null,
    email varchar(255) not null unique,
    first_name varchar(255),
    last_name varchar(255),
    primary key (id)
);
