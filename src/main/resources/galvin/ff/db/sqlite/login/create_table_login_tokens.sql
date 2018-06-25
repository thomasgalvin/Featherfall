create table if not exists LoginTokens(
    uuid text primary key not null,
    ipAddress text not null,
    tokenLifespan bigint not null,
    createdTimestamp bigint not null,
    expiresTimestamp bigint not null,
    userUuid text not null,
    loginType text not null,
    loginProxyUuid text
);