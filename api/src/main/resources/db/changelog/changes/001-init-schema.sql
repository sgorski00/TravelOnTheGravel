--liquibase formatted sql

--changeset sgorski00:001-init-schema
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_public     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_users_email ON users (lower(email));
CREATE UNIQUE INDEX ux_users_username ON users (lower(username));

CREATE TABLE refresh_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE password_reset_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE trips (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE points (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id      BIGINT           NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    name         VARCHAR(255)     NOT NULL,
    latitude     DOUBLE PRECISION NOT NULL,
    longitude    DOUBLE PRECISION NOT NULL,
    country_code VARCHAR(2),
    position     INTEGER          NOT NULL
);

CREATE TABLE segments (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trip_id         BIGINT      NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    from_point_id   BIGINT      NOT NULL REFERENCES points (id) ON DELETE CASCADE,
    to_point_id     BIGINT      NOT NULL REFERENCES points (id) ON DELETE CASCADE,
    transport_type  VARCHAR(20) NOT NULL CHECK (transport_type IN ('CAR', 'PLANE', 'TRAIN', 'OTHER')),
    distance_meters BIGINT,
    geometry        JSONB
);

CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX ix_password_reset_tokens_user_id ON password_reset_tokens (user_id);
CREATE INDEX ix_trips_user_id ON trips (user_id);
CREATE INDEX ix_points_trip_id ON points (trip_id);
CREATE INDEX ix_segments_trip_id ON segments (trip_id);
