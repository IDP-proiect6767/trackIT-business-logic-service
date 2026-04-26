# TrackIT Business Logic Service

> Central application-logic microservice for [TrackIT](https://github.com/IDP-proiect6767) — a fitness-tracking platform. Handles workout management, nutrition tracking, calorie/macro calculations, statistics, and dashboards. Owns no persistent state; reads/writes data via HTTP REST to **Data Service**. Authenticates every incoming request by calling **Auth Service**.

## Architecture

```
[Client / Frontend]
       │  Bearer JWT
       ▼
[Kong API Gateway]               (frontend-net)
       │
       └──► [Business Logic Service]     (frontend-net + backend-net)
                     │
                     ├──► POST /api/v1/auth/validate   →  [Auth Service]
                     │    (validates Bearer token)          (frontend-net)
                     │
                     └──► HTTP REST (CRUD + queries)    →  [Data Service]
                          X-User-Id header (no auth)        (backend-net)
                                    │
                                    ▼
                              [data-db / Postgres]          (db-net)
```

Business Logic Service sits between the external gateway and the internal Data Service. It is the **only** service that validates tokens and the **only** service that can call Data Service.

## Quick start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for containerised runs)

### Run locally (IDE)

1. Start infrastructure (Auth Service + Data Service + DB):
   ```bash
   cd trackIT-infrastructure/compose
   cp .env.example .env  # edit passwords
   docker compose up -d auth-db data-db auth-service data-service
   ```

2. Run the service:
   ```bash
   mvn spring-boot:run
   ```

3. Test an end-to-end flow:
   ```bash
   # Register a user & get token
   TOKEN=$(curl -s -X POST http://localhost:8000/api/v1/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"demo","email":"demo@trackit.local","password":"pass1234"}' \
     | jq -r .tokens.accessToken)

   # Create a workout
   curl -X POST http://localhost:8082/api/v1/workouts \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"name":"Push Day","description":"Chest + Shoulders","exercises":[]}'

   # List workouts
   curl http://localhost:8082/api/v1/workouts -H "Authorization: Bearer $TOKEN"
   ```

### Run with Docker Compose

```bash
docker compose up --build
# Service available at http://localhost:8082
```

## API reference

All endpoints require a `Bearer` access token. Admin-only endpoints are noted.

### Workouts

| Method | Path | Role | Notes |
|--------|------|------|-------|
| GET | `/api/v1/workouts` | any | List user's workout templates |
| GET | `/api/v1/workouts/{id}` | any | Get one |
| POST | `/api/v1/workouts` | any | Create |
| PATCH | `/api/v1/workouts/{id}` | any | Update |
| DELETE | `/api/v1/workouts/{id}` | any | Delete |

### Exercises

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/exercises` | any |
| GET | `/api/v1/exercises/{id}` | any |
| POST | `/api/v1/exercises` | ADMIN |
| PATCH | `/api/v1/exercises/{id}` | ADMIN |
| DELETE | `/api/v1/exercises/{id}` | ADMIN |

### Workout logs

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/workout-logs` | any |
| GET | `/api/v1/workout-logs/{id}` | any |
| POST | `/api/v1/workout-logs` | any |
| PATCH | `/api/v1/workout-logs/{id}` | any |
| DELETE | `/api/v1/workout-logs/{id}` | any |

### Foods

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/foods` | any |
| GET | `/api/v1/foods/{id}` | any |
| POST | `/api/v1/foods` | ADMIN |
| PATCH | `/api/v1/foods/{id}` | ADMIN |
| DELETE | `/api/v1/foods/{id}` | ADMIN |

### Meals

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/meals` | any |
| GET | `/api/v1/meals/{id}` | any |
| POST | `/api/v1/meals` | any |
| PATCH | `/api/v1/meals/{id}` | any |
| DELETE | `/api/v1/meals/{id}` | any |

### Goals

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/users/me/goals` | any |
| PUT | `/api/v1/users/me/goals` | any |

### Statistics & dashboard

| Method | Path | Role | Notes |
|--------|------|------|-------|
| GET | `/api/v1/stats/nutrition/daily?date=YYYY-MM-DD` | any | Sum of meals for a day, vs goals |
| GET | `/api/v1/stats/nutrition/range?from=&to=` | any | Per-day totals across a range |
| GET | `/api/v1/stats/workouts/summary?from=&to=` | any | Sessions, total volume, top exercises |
| GET | `/api/v1/stats/workouts/exercise-progress/{exerciseId}` | any | Time series of best set per session |
| GET | `/api/v1/dashboard` | any | Combined recent activity overview |

Full interactive docs at **`/swagger-ui.html`** when the service is running.

## Configuration

| Variable | Default | Required | Notes |
|----------|---------|----------|-------|
| `AUTH_SERVICE_URL` | `http://localhost:8081` | yes | Auth Service base URL |
| `DATA_SERVICE_URL` | `http://localhost:8083` | yes | Data Service base URL |
| `AUTH_VALIDATE_CACHE_TTL` | `60` | no | Cache TTL (seconds) for token validations |
| `TRACKIT_TZ` | `Europe/Bucharest` | no | Default timezone for daily stats |
| `SERVER_PORT` | `8082` | no | Server port |

## Build & test

```bash
mvn clean verify
```

Unit tests use Mockito; integration tests use WireMock to stub Auth and Data Services.

## Deployment

- **Docker Swarm** placement constraint: `node.labels.role == app`
- Networks: `frontend-net` + `backend-net` + `monitoring-net`
- Stack file lives in `trackIT-infrastructure`
- Observability: `/actuator/prometheus` (scraped by Prometheus), `/actuator/health` (Docker healthcheck)

## Custom Prometheus metrics

| Metric | Tags | Description |
|--------|------|-------------|
| `business_auth_validations_total` | `result` | Token validation outcomes (hit_cache, miss_valid, miss_invalid, error) |
| `business_data_calls_total` | `resource`, `method`, `status` | Data Service call counter |
| `business_data_call_duration_seconds` | `resource`, `method` | Data Service call latency |
| `business_stats_computed_total` | `kind` | Statistics computation counter |
| `business_goals_updated_total` | — | Goal updates counter |

## Known limitations

- **Goals are in-memory** — user nutrition goals reset on service restart. This is a documented limitation for Etapa 1. Migration to a persistent `data.user_goals` table is planned for a future release.
- **Recommendations engine** — out of scope for Etapa 1.
- **Trainer-client features** — out of scope for Etapa 1.

## GitHub Actions CI

Required repository secrets:

| Secret | Description |
|--------|-------------|
| `DOCKERHUB_USERNAME` | Docker Hub username |
| `DOCKERHUB_TOKEN` | Docker Hub access token |

## License

Internal project — no public license.
