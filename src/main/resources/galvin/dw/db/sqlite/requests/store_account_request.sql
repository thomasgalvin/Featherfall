insert or replace into AccountRequests(
    password,

    reasonForAccount,
    vouchName,
    vouchContactInfo,

    approved,
    approvedByUuid,
    approvedTimestamp,

    rejected,
    rejectedByUuid,
    rejectedTimestamp,
    rejectedReason,

    uuid
)
values(?,?,?,?,?,?,?,?,?,?,?,?);