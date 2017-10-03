insert into AccessInfo(
    userUuid, loginType, loginProxyUuid, ipAddress,
    timestamp, resourceUuid, resourceName,
    classification, resourceType, accessType,
    permissionGranted, systemInfoUuid, uuid
) values(
    ?,?,?,?,
    ?,?,?,
    ?,?,?,
    ?,?,?
)