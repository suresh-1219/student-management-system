-- V1: baseline = the schema as it was before Flyway was introduced.
--
-- * Brand-new database  -> this script creates the two tables.
-- * Existing database   -> Flyway "baselines" at version 1 and SKIPS this
--   file (spring.flyway.baseline-on-migrate=true), because the tables were
--   already created earlier by Hibernate.

CREATE TABLE IF NOT EXISTS users (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS students (
    id     BIGINT       NOT NULL AUTO_INCREMENT,
    name   VARCHAR(255) NOT NULL,
    email  VARCHAR(255) NOT NULL,
    course VARCHAR(255) NOT NULL,
    fee    DOUBLE,
    PRIMARY KEY (id)
);
