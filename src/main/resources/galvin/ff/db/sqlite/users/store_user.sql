insert or replace into Users(
    login,
    loginIgnoreCase,
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
    locked,

    uuid
)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);