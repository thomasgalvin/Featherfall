insert into Users(
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
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
on conflict(uuid) do update set
login = excluded.login,
loginIgnoreCase = excluded.loginIgnoreCase,
passwordHash = excluded.passwordHash,

name = excluded.name,
displayName = excluded.displayName,
sortName = excluded.sortName,
prependToName = excluded.prependToName,
appendToName = excluded.appendToName,

credential = excluded.credential,
serialNumber = excluded.serialNumber,
distinguishedName= excluded.distinguishedName,
homeAgency = excluded.homeAgency,
agency = excluded.agency,
countryCode = excluded.countryCode,
citizenship = excluded.citizenship,

created = excluded.created,
active = excluded.active,
locked = excluded.locked,

uuid = excluded.uuid;