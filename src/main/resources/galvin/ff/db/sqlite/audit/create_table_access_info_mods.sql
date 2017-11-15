create table if not exists AccessInfoMods(
    field TEXT not null,
    oldValue TEXT not null,
    newValue TEXT not null,
    accessInfoUuid TEXT not null,
    ordinal int not null
);