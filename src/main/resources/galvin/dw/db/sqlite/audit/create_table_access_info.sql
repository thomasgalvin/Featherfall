create table if not exists AccessInfo(
    userUuid text not null,
    loginType text not null,
    loginProxyUuid text,
    ipAddress text,
    timestamp bigint not null,
    resourceUuid text not null,
    resourceName text not null,
    classification text not null,
    resourceType text not null,
    accessType int not null,
    permissionGranted INT not null,
    systemInfoUuid text not null,
    uuid text not null primary key
);