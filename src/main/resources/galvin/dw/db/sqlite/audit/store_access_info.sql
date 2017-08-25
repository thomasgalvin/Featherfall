insert into AccessInfo(
    userUuid, loginType, loginProxyUuid,
    timestamp, resourceUuid, resourceName,
    classification, resourceType, accessType,
    permissionGranted, systemInfoUuid, uuid
) values(
    ?,?,?,
    ?,?,?,
    ?,?,?,
    ?,?,?
)