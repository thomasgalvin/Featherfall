create table if not exists SystemInfo(
    serialNumber text not null,
    name text not null,
    version text not null,
    maximumClassification text not null,
    classificationGuide text not null,
    uuid text not null primary key
);