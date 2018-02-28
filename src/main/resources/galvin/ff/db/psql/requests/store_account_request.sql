insert into AccountRequests(
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
values(?,?,?,?,?,?,?,?,?,?,?,?)
on conflict(uuid) do update set
password = excluded.password,

reasonForAccount = excluded.reasonForAccount,
vouchName = excluded.vouchName,
vouchContactInfo = excluded.vouchContactInfo,

approved = excluded.approved,
approvedByUuid = excluded.approvedByUuid,
approvedTimestamp = excluded.approvedTimestamp,

rejected = excluded.rejected,
rejectedByUuid = excluded.rejectedByUuid,
rejectedTimestamp = excluded.rejectedTimestamp,
rejectedReason = excluded.rejectedReason,

uuid = excluded.uuid;