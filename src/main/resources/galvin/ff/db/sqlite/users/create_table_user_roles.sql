create table if not exists UserRoles(
    roleName text not null,
    userUuid text not null,
    ordinal int not null
)