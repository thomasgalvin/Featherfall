create table if not exists AccessInfo(
    userUuid TEXT not null,
    loginType TEXT not null,
    loginProxyUuid TEXT,
    timestamp BIGINT not null,
    resourceUuid TEXT not null,
    resourceName TEXT not null,
    classification TEXT not null,
    resourceType TEXT not null,
    accessType TEXT not null,
    permissionGranted INT not null,
    systemInfoUuid TEXT not null,
    uuid TEXT not null primary key
);