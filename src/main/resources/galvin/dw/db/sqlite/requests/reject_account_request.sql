update AccountRequests
    set rejected = 1, rejectedByUuid = ?, rejectedTimestamp = ?
where uuid = ?