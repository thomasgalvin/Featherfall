create table if not exists RolePermissions(
    roleName text not null,
    permissionName text not null,
    ordinal int not null
);