create table if not exists ContactInfo(
    type text not null,
    description text not null,
    contact text not null,
    isPrimary int not null,
    userUuid text not null,
    ordinal int not null
);