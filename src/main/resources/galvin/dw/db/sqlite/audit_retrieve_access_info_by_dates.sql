select
    userUuid, loginType, loginProxyUuid,
    timestamp, resourceUuid, resourceName,
    classification, resourceType, accessType,
    permissionGranted, systemInfoUuid, uuid
from AccessInfo
    where timestamp >= ? and timestamp <= ?
    order by timestamp;