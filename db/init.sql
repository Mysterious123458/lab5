-- Схема и учебные данные для варианта 26.
-- Скрипт выполняется в уже созданной базе lab5 от имени администратора.
-- Пароль lab5_app_password совпадает с .env.example и нужен только для локальной базы.
-- Приложение этот пароль из кода не читает: оно берёт DB_USERNAME и DB_PASSWORD из окружения.
-- Хеши паролей учебных учётных записей: Password123, Dispatcher123, Admin12345 (BCrypt).

CREATE USER lab5_app WITH PASSWORD 'lab5_app_password';

CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT app_users_role_chk CHECK (role IN ('RESIDENT', 'DISPATCHER', 'ADMIN')),
    CONSTRAINT app_users_status_chk CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

CREATE TABLE city_objects (
    id BIGSERIAL PRIMARY KEY,
    address VARCHAR(300) NOT NULL,
    type VARCHAR(32) NOT NULL,
    condition VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT city_objects_type_chk CHECK (type IN ('YARD', 'PARK', 'ROAD', 'PLAYGROUND', 'LIGHTING')),
    CONSTRAINT city_objects_condition_chk CHECK (condition IN ('GOOD', 'SATISFACTORY', 'NEEDS_REPAIR', 'EMERGENCY'))
);

CREATE TABLE work_crews (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    active_work_limit INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT work_crews_limit_chk CHECK (active_work_limit >= 1)
);

CREATE TABLE work_crew_supported_types (
    crew_id BIGINT NOT NULL REFERENCES work_crews (id) ON DELETE CASCADE,
    object_type VARCHAR(32) NOT NULL,
    PRIMARY KEY (crew_id, object_type),
    CONSTRAINT work_crew_supported_types_chk CHECK (object_type IN ('YARD', 'PARK', 'ROAD', 'PLAYGROUND', 'LIGHTING'))
);

CREATE TABLE city_reports (
    id BIGSERIAL PRIMARY KEY,
    city_object_id BIGINT NOT NULL REFERENCES city_objects (id),
    author_id BIGINT NOT NULL REFERENCES app_users (id),
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    rejection_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT city_reports_status_chk CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    CONSTRAINT city_reports_priority_chk CHECK (priority IN ('NORMAL', 'HIGH'))
);

CREATE TABLE work_orders (
    id BIGSERIAL PRIMARY KEY,
    city_report_id BIGINT NOT NULL REFERENCES city_reports (id),
    work_crew_id BIGINT NOT NULL REFERENCES work_crews (id),
    due_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    result_description VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT work_orders_status_chk CHECK (status IN ('ASSIGNED', 'IN_PROGRESS', 'DONE', 'CANCELLED'))
);

CREATE INDEX city_reports_object_idx ON city_reports (city_object_id);
CREATE INDEX city_reports_author_idx ON city_reports (author_id);
CREATE INDEX work_orders_report_idx ON work_orders (city_report_id);
CREATE INDEX work_orders_crew_idx ON work_orders (work_crew_id);

-- Учёт выданных пар access/refresh: одна строка — одна пара.
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    username VARCHAR(200) NOT NULL,
    device_id VARCHAR(200),
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL UNIQUE,
    access_token_expiry TIMESTAMPTZ NOT NULL,
    refresh_token_expiry TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT user_sessions_status_chk CHECK (status IN ('ACTIVE', 'USED', 'REVOKED'))
);

CREATE INDEX user_sessions_username_idx ON user_sessions (username);

INSERT INTO app_users (id, full_name, email, password_hash, role, status, created_at) VALUES
    (1, 'Тимошенко Влад', 'timoshenko@example.com', '$2a$10$bfKi4Ufk2lSoSD1TC4R97.ghXwZjrew7CZcY38o4RfLDO2BFlXqRS', 'RESIDENT', 'ACTIVE', TIMESTAMPTZ '2026-10-20 09:00:00+00'),
    (2, 'Диспетчер района', 'dispatcher@example.com', '$2a$10$kjIfVOBCLTKh/h6ldjNbsu7So1Es6C6aU.mR8zxoorb0z.60BgPye', 'DISPATCHER', 'ACTIVE', TIMESTAMPTZ '2026-10-20 09:05:00+00'),
    (3, 'Администратор', 'admin@example.com', '$2a$10$ZfDkoOidi6vy99RBSp9BBuAixdX/n7jG76yBNa/Zj7hwpaHqqpX52', 'ADMIN', 'ACTIVE', TIMESTAMPTZ '2026-10-20 09:10:00+00'),
    (4, 'Заблокированный житель', 'blocked@example.com', '$2a$10$bfKi4Ufk2lSoSD1TC4R97.ghXwZjrew7CZcY38o4RfLDO2BFlXqRS', 'RESIDENT', 'BLOCKED', TIMESTAMPTZ '2026-10-20 09:15:00+00');

INSERT INTO city_objects (id, address, type, condition, created_at) VALUES
    (1, 'ул. Ленина, 1, двор', 'YARD', 'NEEDS_REPAIR', TIMESTAMPTZ '2026-10-20 10:00:00+00'),
    (2, 'Парк у пруда', 'PARK', 'SATISFACTORY', TIMESTAMPTZ '2026-10-20 10:05:00+00'),
    (3, 'ул. Садовая, освещение', 'LIGHTING', 'GOOD', TIMESTAMPTZ '2026-10-20 10:10:00+00');

INSERT INTO work_crews (id, name, active_work_limit, created_at) VALUES
    (1, 'Бригада дворов', 2, TIMESTAMPTZ '2026-10-20 11:00:00+00'),
    (2, 'Парковая бригада', 1, TIMESTAMPTZ '2026-10-20 11:05:00+00');

INSERT INTO work_crew_supported_types (crew_id, object_type) VALUES
    (1, 'YARD'),
    (1, 'PLAYGROUND'),
    (2, 'PARK');

INSERT INTO city_reports (id, city_object_id, author_id, description, status, priority, rejection_reason, created_at, updated_at) VALUES
    (1, 1, 1, 'Во дворе провалилась плитка у подъезда', 'NEW', 'NORMAL', NULL, TIMESTAMPTZ '2026-10-21 08:00:00+00', TIMESTAMPTZ '2026-10-21 08:00:00+00'),
    (2, 2, 2, 'Сломана скамейка у главной аллеи', 'IN_PROGRESS', 'NORMAL', NULL, TIMESTAMPTZ '2026-10-21 08:30:00+00', TIMESTAMPTZ '2026-10-21 09:00:00+00'),
    (3, 3, 1, 'Не горит фонарь у остановки', 'NEW', 'NORMAL', NULL, TIMESTAMPTZ '2026-10-21 09:30:00+00', TIMESTAMPTZ '2026-10-21 09:30:00+00');

INSERT INTO work_orders (id, city_report_id, work_crew_id, due_date, status, result_description, created_at, updated_at) VALUES
    (1, 2, 2, DATE '2026-10-28', 'ASSIGNED', NULL, TIMESTAMPTZ '2026-10-21 09:00:00+00', TIMESTAMPTZ '2026-10-21 09:00:00+00');

SELECT setval('app_users_id_seq', (SELECT MAX(id) FROM app_users));
SELECT setval('city_objects_id_seq', (SELECT MAX(id) FROM city_objects));
SELECT setval('work_crews_id_seq', (SELECT MAX(id) FROM work_crews));
SELECT setval('city_reports_id_seq', (SELECT MAX(id) FROM city_reports));
SELECT setval('work_orders_id_seq', (SELECT MAX(id) FROM work_orders));

GRANT USAGE ON SCHEMA public TO lab5_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON
    app_users, city_objects, work_crews, work_crew_supported_types, city_reports, work_orders
    TO lab5_app;
-- Сессии только читаются, добавляются и изменяются: удалять их сервисная роль не может.
GRANT SELECT, INSERT, UPDATE ON user_sessions TO lab5_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO lab5_app;
