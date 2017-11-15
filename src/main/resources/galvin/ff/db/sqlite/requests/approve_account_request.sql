update AccountRequests
    set rejected = 0, approved = 1, approvedByUuid = ?, approvedTimestamp = ?
where uuid = ?