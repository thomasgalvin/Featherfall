insert into LoginTokens
( uuid, ipAddress, tokenLifespan, createdTimestamp, expiresTimestamp, userUuid, loginType, loginProxyUuid )
values ( ?, ?, ?, ?, ?, ?, ?, ?)
on conflict(uuid) do update set
uuid = excluded.uuid,
ipAddress = excluded.ipAddress,
tokenLifespan = excluded.tokenLifespan,
createdTimestamp = excluded.createdTimestamp,
expiresTimestamp = excluded.expiresTimestamp,
userUuid = excluded.userUuid,
loginType = excluded.loginType,
loginProxyUuid = excluded.loginProxyUuid;