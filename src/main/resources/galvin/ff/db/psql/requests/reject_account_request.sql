update AccountRequests
    set rejected = 1, rejectedByUuid = ?, rejectedTimestamp = ?, rejectedReason = ?
where uuid = ?