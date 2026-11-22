--liquibase formatted sql

--changeset sgorski00:002-segments-fallback
ALTER TABLE segments
    ADD COLUMN is_fallback         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN geometry_simplified JSONB;
