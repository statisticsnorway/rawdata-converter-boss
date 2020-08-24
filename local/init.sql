-- noinspection SqlNoDataSourceInspectionForFile

CREATE USER boss WITH PASSWORD 'bossman';
CREATE DATABASE rawdata_converter_jobs;
GRANT ALL PRIVILEGES ON DATABASE rawdata_converter_jobs TO boss;
