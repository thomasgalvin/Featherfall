insert or replace into LoginTokens
( uuid, ipAddress, tokenLifespan, createdTimestamp, expiresTimestamp, userUuid, loginType, loginProxyUuid )
values ( ?, ?, ?, ?, ?, ?, ?, ?);