\connect testdb;

CREATE TABLE IF NOT EXISTS data_table (
    id SERIAL PRIMARY KEY,
    random_col1 VARCHAR(255),
    random_col2 INTEGER,
    random_col3 TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create a replication slot and publication for Debezium
SELECT * FROM pg_create_logical_replication_slot('debezium', 'pgoutput');
CREATE PUBLICATION dbserver1 FOR TABLE data_table;

INSERT INTO data_table (random_col1, random_col2) VALUES ('sample data 1', 123);
INSERT INTO data_table (random_col1, random_col2) VALUES ('sample data 2', 456);
INSERT INTO data_table (random_col1, random_col2) VALUES ('sample data 3', 412);