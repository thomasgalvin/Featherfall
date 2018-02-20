create table if not exists AccountRequests(
    password text not null,

    reasonForAccount text,
    vouchName text,
    vouchContactInfo text,

    approved int,
    approvedByUuid text,
    approvedTimestamp bigint,

    rejected int,
    rejectedByUuid text,
    rejectedTimestamp bigint,
    rejectedReason text,

    uuid text not null primary key
);