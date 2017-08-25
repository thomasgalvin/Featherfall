create table if not exists RolePermission(
    roleName text not null,
    permissionName text not null,
    ordinal int not null
)