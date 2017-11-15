create table if not exists CurrentSystemInfo(
    uuid text not null,
    foreign key(uuid) references SystemInfo(uuid)
);