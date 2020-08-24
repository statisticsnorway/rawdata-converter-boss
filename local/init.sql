-- noinspection SqlNoDataSourceInspectionForFile

CREATE USER boss WITH PASSWORD 'bossman';
CREATE DATABASE rawdata_conveter_jobs;
GRANT ALL PRIVILEGES ON DATABASE rawdata_conveter_jobs TO boss;
