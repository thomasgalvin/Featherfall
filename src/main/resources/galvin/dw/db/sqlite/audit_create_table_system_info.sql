create table if not exists SystemInfo(
    serialNumber TEXT NOT NULL,
    name TEXT NOT NULL,
    version TEXT NOT NULL,
    maximumClassification TEXT NOT NULL,
    classificationGuide TEXT NOT NULL,
    uuid TEXT NOT NULL PRIMARY KEY
);