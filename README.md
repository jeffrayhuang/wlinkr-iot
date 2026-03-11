# WLinkr IoT Platform

Full-stack IoT data processing and device control platform built with **Spring Boot 3** + **React 18** + **PostgreSQL** + **MQTT**.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Configuration Reference](#configuration-reference)
- [Authentication](#authentication)
- [REST API Reference](#rest-api-reference)
- [MQTT Integration](#mqtt-integration)
- [WebSocket (Real-Time)](#websocket-real-time)
- [Caching Strategy](#caching-strategy)
- [Database Schema](#database-schema)
- [Project Structure](#project-structure)
- [Frontend](#frontend)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

---

## Architecture

```
                         ┌───────────────────────────────────────────────────────┐
                         │                    Spring Boot 3.3                    │
┌─────────────┐   REST   │  ┌──────────┐  ┌──────────┐  ┌──────────────┐       │  ┌────────────┐
│  React SPA  │─────────▶│  │   REST   │  │ Security │  │   Caffeine   │       │─▶│ PostgreSQL │
│  Vite + TS  │          │  │ Contrlrs │  │ JWT/OAuth│  │    Cache     │       │  │  Port 5432 │
│  Port 5173  │◀─WebSkt──│  └──────────┘  └──────────┘  └──────────────┘       │  └────────────┘
└─────────────┘   STOMP  │  ┌──────────────────────────────────────────┐       │
                         │  │          MQTT Subsystem                   │       │
 ┌────────────┐   MQTT   │  │  MqttService ◀──▶ Moquette (embedded)   │       │
 │ IoT Device │─────────▶│  │  Handlers ──▶ DB + WebSocket bridge     │       │
 │ (Sensor /  │◀─────────│  │  AutoRegistrar (creates new devices)    │       │
 │  Actuator) │  Commands│  └──────────────────────────────────────────┘       │
 └────────────┘          │                    Port 8080                         │
                         └───────────────────────────────────────────────────────┘
```

**Data flows:**
1. **Telemetry (device → server):** Device publishes to MQTT → handler saves to DB → WebSocket pushes to frontend
2. **Commands (server → device):** User creates command via REST → published over MQTT → device responds → status updated in DB
3. **Status tracking:** Device publishes online/offline → handler updates device record → frontend notified via WebSocket
4. **Auto-registration:** Unknown device serial number → new `Device` record created automatically

---

## Tech Stack

| Layer         | Technology                                          |
|---------------|-----------------------------------------------------|
| **Backend**   | Spring Boot 3.3, Java 21, Spring Security 6         |
| **Database**  | PostgreSQL 16, Flyway migrations                    |
| **Caching**   | Caffeine (in-process, 5-min TTL)                    |
| **Auth**      | OAuth2 (Google, Facebook) + Local email/password + JWT |
| **MQTT**      | Moquette (embedded broker) + Eclipse Paho client    |
| **WebSocket** | Spring WebSocket, STOMP over SockJS                 |
| **API Docs**  | SpringDoc OpenAPI 2.6 / Swagger UI                  |
| **Frontend**  | React 18, TypeScript, Vite, Tailwind CSS, Recharts  |
| **Container** | Docker, Docker Compose                              |

---

## Quick Start

### Prerequisites

- **Java 21+** (set `JAVA_HOME`)
- **Node.js 20+**
- **PostgreSQL 15+** (or Docker)
- **Maven 3.6+** (or use the included wrapper)

### Option A: Local Development

#### 1. Database

```bash
# Using Docker:
docker compose up postgres -d

# Or connect to an existing instance and update application.yml datasource
```

#### 2. Backend

```bash
cd backend

# Copy and configure environment
cp ../.env.example ../.env
# Edit .env with your credentials

# Start (Windows)
mvnw.cmd spring-boot:run

# Start (macOS/Linux)
./mvnw spring-boot:run
```

The backend starts at **http://localhost:8080**.

- Swagger UI: **http://localhost:8080/swagger-ui.html**
- Actuator health: **http://localhost:8080/actuator/health**

#### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts at **http://localhost:5173**.

### Option B: Full Docker Compose

```bash
cp .env.example .env
# Edit .env with OAuth credentials and JWT secret
docker compose up --build
```

| Service    | URL                         |
|------------|-----------------------------|
| Frontend   | http://localhost:5173        |
| Backend    | http://localhost:8080        |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MQTT       | tcp://localhost:1883         |
| MQTT WS    | ws://localhost:9883          |

---

## Configuration Reference

All configuration is in `backend/src/main/resources/application.yml`. Key sections:

### Environment Variables

| Variable                  | Default                        | Description                          |
|---------------------------|--------------------------------|--------------------------------------|
| `DB_USERNAME`             | `postgres`                     | Database username                    |
| `DB_PASSWORD`             | `postgres`                     | Database password                    |
| `GOOGLE_CLIENT_ID`        | —                              | Google OAuth2 client ID              |
| `GOOGLE_CLIENT_SECRET`    | —                              | Google OAuth2 client secret          |
| `FACEBOOK_CLIENT_ID`      | —                              | Facebook OAuth2 client ID            |
| `FACEBOOK_CLIENT_SECRET`  | —                              | Facebook OAuth2 client secret        |
| `JWT_SECRET`              | (dev default)                  | HMAC-SHA256 signing key (≥256 bits)  |
| `MQTT_EMBEDDED_ENABLED`   | `true`                         | Start embedded Moquette broker       |
| `MQTT_EMBEDDED_PORT`      | `1883`                         | Embedded broker MQTT port            |
| `MQTT_EMBEDDED_WS_PORT`   | `9883`                         | Embedded broker WebSocket port       |
| `MQTT_BROKER_URL`         | `tcp://localhost:1883`         | External broker URL (when embedded=false) |
| `MQTT_USERNAME`           | (empty)                        | MQTT broker username                 |
| `MQTT_PASSWORD`           | (empty)                        | MQTT broker password                 |
| `MQTT_CLIENT_ID`          | `wlinkr-server`                | MQTT client identifier               |

---

## Authentication

WLinkr supports three authentication methods:

### 1. Local Login (Email / Password)

**Register:**
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "mypassword123"
}
```
- Password must be at least 6 characters
- No email verification required

**Login:**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "mypassword123"
}
```

**Response:**
```json
{ "token": "eyJhbGciOiJIUzI1NiIs..." }
```

### 2. Google OAuth2

- Redirect to: `http://localhost:8080/oauth2/authorization/google`
- Callback: `http://localhost:8080/login/oauth2/code/google`
- After login, redirects to frontend with JWT token

### 3. Facebook OAuth2

- Redirect to: `http://localhost:8080/oauth2/authorization/facebook`
- Callback: `http://localhost:8080/login/oauth2/code/facebook`

### Using JWT Tokens

Include the token in the `Authorization` header for all authenticated requests:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

Token expiration: **24 hours**.

---

## REST API Reference

Base URL: `http://localhost:8080`

### Auth

| Method | Endpoint                | Description              | Auth |
|--------|-------------------------|--------------------------|------|
| POST   | `/api/v1/auth/register` | Register local user      | No   |
| POST   | `/api/v1/auth/login`    | Login (returns JWT)      | No   |
| GET    | `/api/v1/auth/me`       | Current user profile     | Yes  |

### Dashboard

| Method | Endpoint              | Description                | Auth |
|--------|-----------------------|----------------------------|------|
| GET    | `/api/v1/dashboard`   | Aggregated IoT dashboard   | Yes  |

**Response: `DashboardDto`**
```json
{
  "totalDevices": 15,
  "onlineDevices": 10,
  "offlineDevices": 3,
  "errorDevices": 2,
  "totalSensorReadings": 45230,
  "pendingCommands": 5,
  "devicesByType": {
    "SENSOR": 8,
    "ACTUATOR": 4,
    "GATEWAY": 2,
    "CONTROLLER": 1
  }
}
```

### Devices

| Method | Endpoint                  | Description              | Auth |
|--------|---------------------------|--------------------------|------|
| GET    | `/api/v1/devices`         | List devices (paginated) | Yes  |
| GET    | `/api/v1/devices/{id}`    | Get device detail        | Yes  |
| POST   | `/api/v1/devices`         | Register new device      | Yes  |
| PUT    | `/api/v1/devices/{id}`    | Update device            | Yes  |
| DELETE | `/api/v1/devices/{id}`    | Delete device            | Yes  |

**Query Parameters (GET /devices):** `page`, `size`, `status` (ONLINE/OFFLINE/MAINTENANCE/ERROR), `type` (SENSOR/ACTUATOR/GATEWAY/CONTROLLER)

**Create Device Request:**
```json
{
  "name": "Temperature Sensor A1",
  "deviceType": "SENSOR",
  "serialNumber": "SN-2024-001",
  "firmwareVersion": "1.0.0",
  "location": "Building A, Floor 3",
  "description": "Ambient temperature sensor"
}
```

**Device Response: `DeviceDto`**
```json
{
  "id": 1,
  "name": "Temperature Sensor A1",
  "deviceType": "SENSOR",
  "status": "ONLINE",
  "serialNumber": "SN-2024-001",
  "firmwareVersion": "1.0.0",
  "location": "Building A, Floor 3",
  "description": "Ambient temperature sensor",
  "ownerId": 1,
  "createdAt": "2026-03-11T10:00:00Z",
  "updatedAt": "2026-03-11T10:00:00Z"
}
```

**Update Device Request** (all fields optional):
```json
{
  "name": "Updated Name",
  "status": "MAINTENANCE",
  "firmwareVersion": "1.1.0",
  "location": "Building B",
  "description": "Moved to new location"
}
```

### Sensor Data

| Method | Endpoint                              | Description                  | Auth |
|--------|---------------------------------------|------------------------------|------|
| GET    | `/api/v1/devices/{id}/data`           | Sensor data (paginated)      | Yes  |
| GET    | `/api/v1/devices/{id}/data/range`     | Sensor data in time range    | Yes  |
| GET    | `/api/v1/devices/{id}/data/latest`    | Latest N readings for metric | Yes  |
| POST   | `/api/v1/devices/{id}/data`           | Ingest sensor reading        | Yes  |

**Query Parameters:**
- `/data`: `page`, `size`
- `/data/range`: `from` (ISO instant), `to` (ISO instant)
- `/data/latest`: `metricName`, `limit`

**Create Sensor Data Request:**
```json
{
  "metricName": "temperature",
  "metricValue": 23.5,
  "unit": "°C"
}
```

**Sensor Data Response: `SensorDataDto`**
```json
{
  "id": 1,
  "deviceId": 1,
  "metricName": "temperature",
  "metricValue": 23.5,
  "unit": "°C",
  "recordedAt": "2026-03-11T10:30:00Z"
}
```

### Device Commands

| Method | Endpoint                           | Description               | Auth |
|--------|------------------------------------|---------------------------|------|
| GET    | `/api/v1/devices/{id}/commands`    | Command history (paginated)| Yes  |
| POST   | `/api/v1/devices/{id}/commands`    | Send command to device     | Yes  |

**Create Command Request:**
```json
{
  "commandName": "setLed",
  "payload": {
    "color": "red",
    "brightness": 100
  }
}
```

**Command Response: `DeviceCommandDto`**
```json
{
  "id": 1,
  "deviceId": 1,
  "issuedById": 1,
  "commandName": "setLed",
  "payload": { "color": "red", "brightness": 100 },
  "status": "SENT",
  "response": null,
  "createdAt": "2026-03-11T10:30:00Z",
  "executedAt": null
}
```

**Command Status Lifecycle:** `PENDING` → `SENT` → `ACKNOWLEDGED` / `FAILED` / `EXPIRED`

### MQTT Management

| Method | Endpoint               | Description                      | Auth |
|--------|------------------------|----------------------------------|------|
| GET    | `/api/v1/mqtt/status`  | MQTT connection status           | Yes  |
| POST   | `/api/v1/mqtt/publish` | Publish to MQTT topic (debug)    | Yes  |

---

## MQTT Integration

WLinkr includes a full MQTT subsystem for real-time IoT communication.

### Architecture

```
IoT Device                    WLinkr Server                    PostgreSQL
    │                              │                               │
    │── telemetry ──────────────▶  │── SensorDataMqttHandler ───▶  │ sensor_data
    │── status ─────────────────▶  │── DeviceStatusMqttHandler ──▶ │ devices
    │                              │                               │
    │◀── commands ──────────────── │◀── MqttCommandPublisher ───── │ device_commands
    │── commands/response ──────▶  │── CommandResponseHandler ───▶ │ device_commands
```

### Broker

By default, WLinkr starts an **embedded Moquette broker** on port 1883 (MQTT) and 9883 (WebSocket). To use an external broker:

```yaml
mqtt:
  embedded:
    enabled: false
  broker:
    url: tcp://your-broker:1883
    username: your-user
    password: your-pass
```

### Topic Structure

| Topic Pattern                                     | Direction       | Description                        |
|---------------------------------------------------|-----------------|------------------------------------|
| `wlinkr/devices/{serialNumber}/telemetry`         | Device → Server | Sensor data (temperature, etc.)    |
| `wlinkr/devices/{serialNumber}/status`            | Device → Server | Online/offline/error status        |
| `wlinkr/devices/{serialNumber}/commands`           | Server → Device | Commands sent to device            |
| `wlinkr/devices/{serialNumber}/commands/response`  | Device → Server | Command execution results          |
| `wlinkr/server/status`                             | Server (LWT)    | Server online/offline (Last Will)  |

### Telemetry Payload Format

Devices publish JSON to `wlinkr/devices/{serialNumber}/telemetry`:

**Multiple metrics:**
```json
{
  "metrics": [
    { "name": "temperature", "value": 23.5, "unit": "°C" },
    { "name": "humidity", "value": 60.2, "unit": "%" }
  ],
  "timestamp": "2026-03-11T10:30:00Z"
}
```

**Single metric:**
```json
{
  "name": "temperature",
  "value": 23.5,
  "unit": "°C"
}
```

### Status Payload Format

```json
{
  "status": "ONLINE"
}
```

Valid statuses: `ONLINE`, `OFFLINE`, `MAINTENANCE`, `ERROR`

### Command Payload Format

Commands published by the server to `wlinkr/devices/{serialNumber}/commands`:
```json
{
  "commandId": 123,
  "commandName": "setLed",
  "payload": { "color": "red", "brightness": 100 }
}
```

Device response to `wlinkr/devices/{serialNumber}/commands/response`:
```json
{
  "commandId": 123,
  "status": "ACKNOWLEDGED",
  "response": { "result": "ok", "details": "LED turned on" }
}
```

### Device Auto-Registration

When an unknown `serialNumber` publishes telemetry or status, WLinkr **automatically creates a new device** record:

- **Name:** `Auto-{serialNumber}`
- **Type:** `SENSOR` (configurable via `mqtt.auto-register.default-device-type`)
- **Status:** `ONLINE`
- **Owner:** System user (`system@wlinkr.local`, auto-created)

To disable:
```yaml
mqtt:
  auto-register:
    enabled: false
```

### Connecting a Device (Example: Python)

```python
import paho.mqtt.client as mqtt
import json, time

SERIAL = "my-sensor-001"
client = mqtt.Client()
client.connect("localhost", 1883)

# Publish telemetry
while True:
    client.publish(
        f"wlinkr/devices/{SERIAL}/telemetry",
        json.dumps({
            "metrics": [
                {"name": "temperature", "value": 22.5, "unit": "°C"},
                {"name": "humidity", "value": 55.0, "unit": "%"}
            ]
        })
    )
    time.sleep(10)
```

### Connecting a Device (Example: Arduino/ESP32)

```cpp
#include <PubSubClient.h>

const char* mqtt_server = "your-server-ip";
const char* serial_number = "esp32-001";

void publishTelemetry() {
  String topic = "wlinkr/devices/" + String(serial_number) + "/telemetry";
  String payload = "{\"name\":\"temperature\",\"value\":" + String(readTemp()) + ",\"unit\":\"°C\"}";
  client.publish(topic.c_str(), payload.c_str());
}
```

---

## WebSocket (Real-Time)

The backend bridges MQTT messages to the frontend via STOMP over SockJS.

### Connection

```
WebSocket endpoint: ws://localhost:8080/ws   (SockJS fallback)
```

### Topics

Subscribe to receive real-time updates:

| STOMP Topic                                 | Event                          |
|---------------------------------------------|--------------------------------|
| `/topic/devices/{deviceId}/telemetry`       | New sensor data for a device   |
| `/topic/devices/{deviceId}/status`          | Status change for a device     |
| `/topic/devices/{deviceId}/commands`        | Command response from device   |
| `/topic/devices/status`                     | All device status changes      |

### Usage (JavaScript)

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  onConnect: () => {
    // Subscribe to a device's telemetry
    client.subscribe('/topic/devices/1/telemetry', (message) => {
      const data = JSON.parse(message.body);
      console.log('Telemetry:', data);
    });

    // Subscribe to all device status changes
    client.subscribe('/topic/devices/status', (message) => {
      const event = JSON.parse(message.body);
      console.log(`Device ${event.serialNumber}: ${event.oldStatus} → ${event.newStatus}`);
    });
  }
});

client.activate();
```

---

## Caching Strategy

Server-side caching uses **Caffeine** (in-process) with automatic eviction on write operations.

| Cache Name       | TTL     | Max Size | Evicted On                    |
|------------------|---------|----------|-------------------------------|
| `devices`        | 5 min   | 1,000    | Create / Update / Delete      |
| `device-detail`  | 5 min   | 1,000    | Create / Update / Delete      |
| `sensor-data`    | 5 min   | 1,000    | New sensor data ingested      |
| `dashboard`      | 5 min   | 1,000    | Any write operation           |

MQTT handlers also evict the relevant caches when processing incoming messages.

---

## Database Schema

Schema name: **`wlinkr`**. Managed by Flyway migrations.

### Entity Relationship

```
┌──────────────┐       ┌──────────────┐       ┌─────────────────┐
│    users     │       │   devices    │       │  sensor_data    │
│──────────────│       │──────────────│       │─────────────────│
│ id (PK)      │◀──┐   │ id (PK)      │◀──┐   │ id (PK)         │
│ email        │   │   │ name         │   │   │ device_id (FK)  │──▶ devices.id
│ name         │   │   │ device_type  │   │   │ metric_name     │
│ password     │   │   │ status       │   │   │ metric_value    │
│ avatar_url   │   │   │ serial_number│   │   │ unit            │
│ provider     │   ├──▶│ owner_id(FK) │   │   │ recorded_at     │
│ provider_id  │   │   │ firmware_ver │   │   └─────────────────┘
│ created_at   │   │   │ location     │   │
│ updated_at   │   │   │ description  │   │   ┌─────────────────┐
└──────────────┘   │   │ created_at   │   │   │ device_commands  │
                   │   │ updated_at   │   │   │─────────────────│
                   │   └──────────────┘   │   │ id (PK)         │
                   │                      └──▶│ device_id (FK)  │──▶ devices.id
                   └─────────────────────────▶│ issued_by (FK)  │──▶ users.id
                                              │ command_name    │
                                              │ payload (JSONB) │
                                              │ status          │
                                              │ response (JSONB)│
                                              │ created_at      │
                                              │ executed_at     │
                                              └─────────────────┘
```

### Enum Types

| Type              | Values                                            |
|-------------------|---------------------------------------------------|
| `device_status`   | `ONLINE`, `OFFLINE`, `MAINTENANCE`, `ERROR`       |
| `device_type`     | `SENSOR`, `ACTUATOR`, `GATEWAY`, `CONTROLLER`     |
| `command_status`  | `PENDING`, `SENT`, `ACKNOWLEDGED`, `FAILED`, `EXPIRED` |
| `auth_provider`   | `GOOGLE`, `FACEBOOK`, `LOCAL`                     |

---

## Project Structure

```
wlinkr/
├── .env.example                          # Environment template
├── docker-compose.yml                    # Full stack Docker setup
├── backend/
│   ├── pom.xml                           # Maven dependencies
│   └── src/main/
│       ├── java/com/wlinkr/iot/
│       │   ├── WlinkrApplication.java    # Spring Boot entry point
│       │   ├── config/
│       │   │   ├── CacheConfig.java      # Caffeine cache setup
│       │   │   ├── EmbeddedMqttBrokerConfig.java  # Moquette broker
│       │   │   ├── OpenApiConfig.java    # Swagger JWT auth scheme
│       │   │   └── WebSocketConfig.java  # STOMP/SockJS config
│       │   ├── controller/
│       │   │   ├── AuthController.java   # Register, login, profile
│       │   │   ├── DashboardController.java
│       │   │   ├── DeviceController.java
│       │   │   ├── DeviceCommandController.java
│       │   │   ├── MqttController.java   # MQTT status & publish
│       │   │   └── SensorDataController.java
│       │   ├── exception/                # Global error handling
│       │   ├── model/
│       │   │   ├── dto/                  # Request/response records
│       │   │   ├── entity/               # JPA entities
│       │   │   └── enums/                # Status/type enums
│       │   ├── mqtt/
│       │   │   ├── MqttService.java      # Core MQTT client (pub/sub)
│       │   │   ├── MqttMessageRouter.java    # Routes messages to handlers
│       │   │   ├── SensorDataMqttHandler.java # Ingests telemetry
│       │   │   ├── DeviceStatusMqttHandler.java  # Updates device status
│       │   │   ├── CommandResponseMqttHandler.java # Processes cmd responses
│       │   │   ├── MqttCommandPublisher.java      # Sends commands via MQTT
│       │   │   └── DeviceAutoRegistrar.java       # Auto-creates devices
│       │   ├── repository/               # Spring Data JPA
│       │   ├── security/
│       │   │   ├── SecurityConfig.java   # HTTP security rules
│       │   │   ├── JwtTokenProvider.java # JWT generation/validation
│       │   │   ├── JwtAuthenticationFilter.java   # Request filter
│       │   │   ├── UserPrincipal.java    # Auth principal
│       │   │   └── OAuth2AuthenticationSuccessHandler.java
│       │   └── service/                  # Business logic + caching
│       └── resources/
│           ├── application.yml           # Full config
│           └── db/migration/
│               └── V1__init_schema.sql   # Flyway migration
├── frontend/
│   ├── package.json
│   └── src/
│       ├── api/                          # Axios API clients
│       │   ├── client.ts                 # Shared Axios instance + JWT interceptor
│       │   ├── auth.ts                   # Login/register API
│       │   ├── devices.ts               # Device CRUD API
│       │   ├── sensorData.ts            # Sensor data API
│       │   ├── commands.ts              # Device commands API
│       │   └── dashboard.ts             # Dashboard API
│       ├── context/
│       │   └── AuthContext.tsx           # Auth state (token, user, login/logout)
│       ├── pages/
│       │   ├── LoginPage.tsx            # Local + OAuth login/register
│       │   ├── DashboardPage.tsx        # IoT dashboard with charts
│       │   ├── DevicesPage.tsx          # Device list with filters
│       │   └── DeviceDetailPage.tsx     # Device detail + sensor data + commands
│       ├── components/                  # Reusable UI components
│       ├── hooks/                       # Custom React hooks
│       └── types/                       # TypeScript interfaces
```

---

## Frontend

The frontend is a React 18 SPA built with Vite and styled with Tailwind CSS.

### Pages

| Page             | Route               | Description                                   |
|------------------|----------------------|-----------------------------------------------|
| Login            | `/login`             | Email/password login + registration + OAuth    |
| Dashboard        | `/`                  | Aggregated stats, device status charts         |
| Devices          | `/devices`           | Paginated device list with status/type filters |
| Device Detail    | `/devices/:id`       | Device info, live sensor charts, command panel |

### Key Libraries

| Library         | Purpose                          |
|-----------------|----------------------------------|
| `axios`         | HTTP client with JWT interceptor |
| `react-router`  | Client-side routing              |
| `recharts`      | Sensor data charts               |
| `lucide-react`  | Icons                            |
| `tailwindcss`   | Utility-first CSS                |

---

## Deployment

### Docker Compose (Production)

```bash
cp .env.example .env
# Set real credentials in .env:
#   GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
#   FACEBOOK_CLIENT_ID, FACEBOOK_CLIENT_SECRET
#   JWT_SECRET (strong random string)

docker compose up --build -d
```

### Environment Checklist

- [ ] Set a strong `JWT_SECRET` (≥256 bits)
- [ ] Configure OAuth2 credentials (Google and/or Facebook)
- [ ] Set PostgreSQL credentials (`DB_USERNAME`, `DB_PASSWORD`)
- [ ] For external MQTT broker: set `MQTT_EMBEDDED_ENABLED=false` + `MQTT_BROKER_URL`
- [ ] Review `mqtt.auto-register.enabled` for production
- [ ] Set `logging.level.com.wlinkr` to `INFO` or `WARN` in production

---

## Troubleshooting

### Common Issues

| Problem | Solution |
|---------|----------|
| `Address already in use: bind` on startup | Another process is using port 1883 or 9883. Change `MQTT_EMBEDDED_PORT` / `MQTT_EMBEDDED_WS_PORT` or kill the conflicting process. |
| `release version 21 not supported` | Set `JAVA_HOME` to a JDK 21 installation. |
| Flyway checksum mismatch | Run `mvn flyway:repair` or drop and recreate the `wlinkr` schema. |
| `stringtype=unspecified` error | Ensure the JDBC URL includes `?stringtype=unspecified` for PostgreSQL enum support. |
| Flyway `non-empty schema` error | Set `spring.flyway.baselineOnMigrate=true` in application.yml. |
| MQTT `Unable to connect to server` | The MQTT client is starting before the embedded broker. Ensure `MqttService` depends on the `embeddedMqttBroker` bean (already handled in code). |
| 401 on Swagger UI | Use the "Authorize" button and enter `Bearer {your-jwt-token}`. |
| OAuth `invalid_client` | Verify OAuth credentials in `.env` match your Google/Facebook app configuration. |
| Frontend 401 on API calls | Token may be expired (24h). Log out and log back in. |

### Useful Endpoints

| URL | Description |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | Interactive API docs |
| `http://localhost:8080/actuator/health` | Application health |
| `http://localhost:8080/actuator/caches` | Active cache stats |
| `http://localhost:8080/api/v1/mqtt/status` | MQTT connection status |

---

## License

Private project.
