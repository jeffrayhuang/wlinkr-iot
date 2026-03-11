
-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS wlinkr;
SET search_path TO wlinkr;

-- Enum-like types

CREATE TYPE wlinkr.device_status AS ENUM ('ONLINE', 'OFFLINE', 'MAINTENANCE', 'ERROR');
CREATE TYPE wlinkr.device_type   AS ENUM ('SENSOR', 'ACTUATOR', 'GATEWAY', 'CONTROLLER');
CREATE TYPE wlinkr.command_status AS ENUM ('PENDING', 'SENT', 'ACKNOWLEDGED', 'FAILED', 'EXPIRED');
CREATE TYPE wlinkr.auth_provider  AS ENUM ('GOOGLE', 'FACEBOOK', 'LOCAL');

-- Users (OAuth2-sourced)

CREATE TABLE wlinkr.users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    password        VARCHAR(255), -- nullable for OAuth users, required for local
    avatar_url      VARCHAR(512),
    provider        wlinkr.auth_provider NOT NULL,
    provider_id     VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_id)
);

-- Devices

CREATE TABLE wlinkr.devices (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    device_type     wlinkr.device_type  NOT NULL,
    status          wlinkr.device_status NOT NULL DEFAULT 'OFFLINE',
    serial_number   VARCHAR(100) NOT NULL UNIQUE,
    firmware_version VARCHAR(50),
    location        VARCHAR(255),
    description     TEXT,
    owner_id        BIGINT NOT NULL REFERENCES wlinkr.users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_devices_owner   ON wlinkr.devices(owner_id);
CREATE INDEX idx_devices_status  ON wlinkr.devices(status);
CREATE INDEX idx_devices_type    ON wlinkr.devices(device_type);

-- Sensor data (time-series style)

CREATE TABLE wlinkr.sensor_data (
    id              BIGSERIAL PRIMARY KEY,
    device_id       BIGINT NOT NULL REFERENCES wlinkr.devices(id) ON DELETE CASCADE,
    metric_name     VARCHAR(100) NOT NULL,
    metric_value    DOUBLE PRECISION NOT NULL,
    unit            VARCHAR(30),
    recorded_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_sensor_data_device      ON wlinkr.sensor_data(device_id);
CREATE INDEX idx_sensor_data_recorded    ON wlinkr.sensor_data(recorded_at DESC);
CREATE INDEX idx_sensor_data_device_time ON wlinkr.sensor_data(device_id, recorded_at DESC);

-- Device commands (control plane)

CREATE TABLE wlinkr.device_commands (
    id              BIGSERIAL PRIMARY KEY,
    device_id       BIGINT NOT NULL REFERENCES wlinkr.devices(id) ON DELETE CASCADE,
    issued_by       BIGINT NOT NULL REFERENCES wlinkr.users(id),
    command_name    VARCHAR(100) NOT NULL,
    payload         JSONB,
    status          wlinkr.command_status NOT NULL DEFAULT 'PENDING',
    response        JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    executed_at     TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_commands_device ON wlinkr.device_commands(device_id);
CREATE INDEX idx_commands_status ON wlinkr.device_commands(status);
