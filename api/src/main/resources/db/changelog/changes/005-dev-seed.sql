--liquibase formatted sql

--changeset sgorski00:005-dev-seed context:dev
-- Przykładowe dane tylko dla profilu dev. Hasło wszystkich kont: haslo1234

INSERT INTO users (email, username, password_hash, is_public, blocked) VALUES
    ('anna@example.com', 'anna', '$2a$10$kV4xqwM0NFn5eMjtMwqTx.5MEl1GWd9Snfrlr.MupIS/NRw7jgUK6', TRUE, FALSE),
    ('bartek@example.com', 'bartek', '$2a$10$kV4xqwM0NFn5eMjtMwqTx.5MEl1GWd9Snfrlr.MupIS/NRw7jgUK6', TRUE, FALSE),
    ('celina@example.com', 'celina', '$2a$10$kV4xqwM0NFn5eMjtMwqTx.5MEl1GWd9Snfrlr.MupIS/NRw7jgUK6', FALSE, FALSE),
    ('dawid@example.com', 'dawid', '$2a$10$kV4xqwM0NFn5eMjtMwqTx.5MEl1GWd9Snfrlr.MupIS/NRw7jgUK6', TRUE, TRUE);

INSERT INTO trips (user_id, name, start_date, end_date, description)
SELECT id, 'Weekend w Krakowie', '2025-05-01', '2025-05-03', 'Krótki wyjazd na majówkę.' FROM users WHERE username = 'anna';
INSERT INTO points (trip_id, name, latitude, longitude, country_code, position) VALUES
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Weekend w Krakowie'), 'Warszawa', 52.2297, 21.0122, 'PL', 0),
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Weekend w Krakowie'), 'Kraków', 50.0647, 19.945, 'PL', 1);
INSERT INTO segments (trip_id, from_point_id, to_point_id, transport_type, distance_meters, geometry, geometry_simplified, is_fallback)
VALUES ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Weekend w Krakowie'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Weekend w Krakowie') AND p.name = 'Warszawa'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Weekend w Krakowie') AND p.name = 'Kraków'), 'CAR', 251977, '{"type": "LineString", "coordinates": [[21.0122, 52.2297], [19.945, 50.0647]]}'::jsonb, '{"type": "LineString", "coordinates": [[21.0122, 52.2297], [19.945, 50.0647]]}'::jsonb, FALSE);

INSERT INTO trips (user_id, name, start_date, end_date, description)
SELECT id, 'Bałkany 2026', '2026-07-01', '2026-07-14', 'Pociągiem i samochodem przez Bałkany.' FROM users WHERE username = 'anna';
INSERT INTO points (trip_id, name, latitude, longitude, country_code, position) VALUES
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026'), 'Kraków', 50.0647, 19.945, 'PL', 0),
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026'), 'Budapeszt', 47.4979, 19.0402, 'HU', 1),
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026'), 'Belgrad', 44.7866, 20.4489, 'RS', 2),
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026'), 'Sarajewo', 43.8563, 18.4131, 'BA', 3);
INSERT INTO segments (trip_id, from_point_id, to_point_id, transport_type, distance_meters, geometry, geometry_simplified, is_fallback)
VALUES ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026') AND p.name = 'Kraków'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026') AND p.name = 'Budapeszt'), 'TRAIN', 293007, '{"type": "LineString", "coordinates": [[19.945, 50.0647], [19.0402, 47.4979]]}'::jsonb, '{"type": "LineString", "coordinates": [[19.945, 50.0647], [19.0402, 47.4979]]}'::jsonb, FALSE);
INSERT INTO segments (trip_id, from_point_id, to_point_id, transport_type, distance_meters, geometry, geometry_simplified, is_fallback)
VALUES ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026') AND p.name = 'Budapeszt'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026') AND p.name = 'Belgrad'), 'CAR', 320408, '{"type": "LineString", "coordinates": [[19.0402, 47.4979], [20.4489, 44.7866]]}'::jsonb, '{"type": "LineString", "coordinates": [[19.0402, 47.4979], [20.4489, 44.7866]]}'::jsonb, FALSE);
INSERT INTO segments (trip_id, from_point_id, to_point_id, transport_type, distance_meters, geometry, geometry_simplified, is_fallback)
VALUES ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026') AND p.name = 'Belgrad'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'anna' AND t.name = 'Bałkany 2026') AND p.name = 'Sarajewo'), 'OTHER', 192161, '{"type": "LineString", "coordinates": [[20.4489, 44.7866], [18.4131, 43.8563]]}'::jsonb, '{"type": "LineString", "coordinates": [[20.4489, 44.7866], [18.4131, 43.8563]]}'::jsonb, FALSE);

INSERT INTO trips (user_id, name, start_date, end_date, description)
SELECT id, 'Berlin i Praga', '2026-03-10', '2026-03-15', NULL FROM users WHERE username = 'bartek';
INSERT INTO points (trip_id, name, latitude, longitude, country_code, position) VALUES
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Berlin i Praga'), 'Gdańsk', 54.352, 18.6466, 'PL', 0),
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Berlin i Praga'), 'Berlin', 52.52, 13.405, 'DE', 1),
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Berlin i Praga'), 'Praga', 50.0755, 14.4378, 'CZ', 2);
INSERT INTO segments (trip_id, from_point_id, to_point_id, transport_type, distance_meters, geometry, geometry_simplified, is_fallback)
VALUES ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Berlin i Praga'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Berlin i Praga') AND p.name = 'Gdańsk'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Berlin i Praga') AND p.name = 'Berlin'), 'PLANE', 402407, '{"type": "LineString", "coordinates": [[18.6466, 54.352], [13.405, 52.52]]}'::jsonb, '{"type": "LineString", "coordinates": [[18.6466, 54.352], [13.405, 52.52]]}'::jsonb, FALSE);
INSERT INTO segments (trip_id, from_point_id, to_point_id, transport_type, distance_meters, geometry, geometry_simplified, is_fallback)
VALUES ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Berlin i Praga'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Berlin i Praga') AND p.name = 'Berlin'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Berlin i Praga') AND p.name = 'Praga'), 'TRAIN', 281133, '{"type": "LineString", "coordinates": [[13.405, 52.52], [14.4378, 50.0755]]}'::jsonb, '{"type": "LineString", "coordinates": [[13.405, 52.52], [14.4378, 50.0755]]}'::jsonb, FALSE);

INSERT INTO trips (user_id, name, start_date, end_date, description)
SELECT id, 'Rejs po Bałtyku', '2026-08-01', NULL, 'Statkiem z Wrocławia nad morze.' FROM users WHERE username = 'bartek';
INSERT INTO points (trip_id, name, latitude, longitude, country_code, position) VALUES
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Rejs po Bałtyku'), 'Wrocław', 51.1079, 17.0385, 'PL', 0),
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Rejs po Bałtyku'), 'Gdynia', 54.5189, 18.5305, 'PL', 1),
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Rejs po Bałtyku'), 'Bałtyk', 55.2, 17.5, NULL, 2);
INSERT INTO segments (trip_id, from_point_id, to_point_id, transport_type, distance_meters, geometry, geometry_simplified, is_fallback)
VALUES ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Rejs po Bałtyku'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Rejs po Bałtyku') AND p.name = 'Wrocław'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Rejs po Bałtyku') AND p.name = 'Gdynia'), 'TRAIN', 392293, '{"type": "LineString", "coordinates": [[17.0385, 51.1079], [18.5305, 54.5189]]}'::jsonb, '{"type": "LineString", "coordinates": [[17.0385, 51.1079], [18.5305, 54.5189]]}'::jsonb, FALSE);
INSERT INTO segments (trip_id, from_point_id, to_point_id, transport_type, distance_meters, geometry, geometry_simplified, is_fallback)
VALUES ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Rejs po Bałtyku'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Rejs po Bałtyku') AND p.name = 'Gdynia'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'bartek' AND t.name = 'Rejs po Bałtyku') AND p.name = 'Bałtyk'), 'OTHER', 100425, '{"type": "LineString", "coordinates": [[18.5305, 54.5189], [17.5, 55.2]]}'::jsonb, '{"type": "LineString", "coordinates": [[18.5305, 54.5189], [17.5, 55.2]]}'::jsonb, FALSE);

INSERT INTO trips (user_id, name, start_date, end_date, description)
SELECT id, 'Sylwester w Wiedniu', '2025-12-30', '2026-01-02', NULL FROM users WHERE username = 'celina';
INSERT INTO points (trip_id, name, latitude, longitude, country_code, position) VALUES
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'celina' AND t.name = 'Sylwester w Wiedniu'), 'Poznań', 52.4064, 16.9252, 'PL', 0),
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'celina' AND t.name = 'Sylwester w Wiedniu'), 'Wiedeń', 48.2082, 16.3738, 'AT', 1);
INSERT INTO segments (trip_id, from_point_id, to_point_id, transport_type, distance_meters, geometry, geometry_simplified, is_fallback)
VALUES ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'celina' AND t.name = 'Sylwester w Wiedniu'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'celina' AND t.name = 'Sylwester w Wiedniu') AND p.name = 'Poznań'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'celina' AND t.name = 'Sylwester w Wiedniu') AND p.name = 'Wiedeń'), 'PLANE', 468454, '{"type": "LineString", "coordinates": [[16.9252, 52.4064], [16.3738, 48.2082]]}'::jsonb, '{"type": "LineString", "coordinates": [[16.9252, 52.4064], [16.3738, 48.2082]]}'::jsonb, FALSE);

INSERT INTO trips (user_id, name, start_date, end_date, description)
SELECT id, 'Stara podróż', '2024-09-01', NULL, NULL FROM users WHERE username = 'dawid';
INSERT INTO points (trip_id, name, latitude, longitude, country_code, position) VALUES
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'dawid' AND t.name = 'Stara podróż'), 'Warszawa', 52.2297, 21.0122, 'PL', 0),
    ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'dawid' AND t.name = 'Stara podróż'), 'Gdańsk', 54.352, 18.6466, 'PL', 1);
INSERT INTO segments (trip_id, from_point_id, to_point_id, transport_type, distance_meters, geometry, geometry_simplified, is_fallback)
VALUES ((SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'dawid' AND t.name = 'Stara podróż'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'dawid' AND t.name = 'Stara podróż') AND p.name = 'Warszawa'), (SELECT p.id FROM points p WHERE p.trip_id = (SELECT t.id FROM trips t JOIN users u ON u.id = t.user_id WHERE u.username = 'dawid' AND t.name = 'Stara podróż') AND p.name = 'Gdańsk'), 'CAR', 283537, '{"type": "LineString", "coordinates": [[21.0122, 52.2297], [18.6466, 54.352]]}'::jsonb, '{"type": "LineString", "coordinates": [[21.0122, 52.2297], [18.6466, 54.352]]}'::jsonb, FALSE);
