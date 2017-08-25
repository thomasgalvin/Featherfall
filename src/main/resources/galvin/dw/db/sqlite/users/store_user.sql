insert or replace into Users(
    login,
    passwordHash,

    name,
    displayName,
    sortName,
    prependToName,
    appendToName,

    credential,
    serialNumber,
    distinguishedName,
    homeAgency,
    agency,
    countryCode,
    citizenship,

    created,
    active,

    uuid
)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);