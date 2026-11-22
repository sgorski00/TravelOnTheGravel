--liquibase formatted sql

--changeset sgorski00:004-user-blocked
ALTER TABLE users
    ADD COLUMN blocked BOOLEAN NOT NULL DEFAULT FALSE;
