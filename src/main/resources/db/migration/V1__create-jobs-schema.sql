-- noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE job
(
    id       varchar(26) PRIMARY KEY,
    status   varchar(100),
    source   varchar(100),
    document jsonb
);
