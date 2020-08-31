create table if not exists users (
    id identity not null primary key,
    first_name varchar(255),
    last_name varchar(255),
    registration_date datetime

);

alter table users add column if not exists balance int default 100;

create table if not exists transfers (
    id identity not null primary key,
    sender_id int not null,
    receiver_id int not null,
    amount int not null,
    foreign key (sender_id) references users(id),
    foreign key (receiver_id) references users(id)
);

alter table users add column IF NOT EXISTS version int default 1;