create table if not exists Users(
    login text not null,
    loginIgnoreCase text not null,
    passwordHash text,

    name text not null,
    displayName text not null,
    sortName text not null,
    prependToName text not null,
    appendToName text not null,

    credential text,
    serialNumber text,
    distinguishedName text,
    homeAgency text,
    agency text,
    countryCode text,
    citizenship text,

    created bigint not null,
    active int not null,
    locked int not null,

    uuid text not null primary key
);