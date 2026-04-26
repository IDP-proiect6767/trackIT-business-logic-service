# Changelog

All notable changes to the TrackIT Business Logic Service will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-04-26

### Added

- **Phase 0 — Project bootstrap**: `pom.xml` (Spring Boot 3.4.1, Java 21) with Web, WebFlux (for `WebClient`), Security, Validation, Actuator, Cache + Caffeine, Resilience4j (`spring-boot3` + `reactor`), Micrometer Prometheus, Lombok, springdoc-openapi, and test deps (`spring-boot-starter-test`, `spring-security-test`, `reactor-test`, `wiremock-standalone`). `BusinessLogicServiceApplication`, minimal `application.yml` (port 8082, app name `business-logic-service`), `.gitignore`, Maven toolchain locked to Java 21 (Bellsoft).
- **Phase 1 — Configuration & WebClient setup**: `TrackitProperties` (`@ConfigurationProperties` for `trackit.auth.*`, `trackit.data.*`, `trackit.http.*` with validation), `HttpClientConfig` exposing two qualified `WebClient` beans (`authServiceWebClient`, `dataServiceWebClient`) wired with Reactor Netty connect timeout 2s and response timeout 5s, `CacheConfig` (`@EnableCaching` + Caffeine `CacheManager` with `tokenValidations` cache, TTL 60s configurable, max size 10000). `application.yml` extended with `trackit.*` and `resilience4j.retry.instances.{authService,dataService}` blocks (3 attempts, exponential backoff 200ms→800ms, retry only on `IOException`/503/504).
- **Phase 2 — DTOs**: Full set of request/response records under `dto.*` — `PageResponse<T>`, `ErrorResponse` (with stable error codes), `AuthValidationResponse`, `AuthenticatedUser`, workout/exercise/workout-log/food/meal request + response DTOs with Jakarta validation annotations, goal DTOs, statistics DTOs (`DailyNutritionStats`, `MacroDelta`, `RangeNutritionStats`, `WorkoutSummary`, `TopExercise`, `ExerciseProgressPoint`, `ExerciseProgress`, `DashboardResponse`).
- **Phase 3 — Authentication**: `AuthServiceRemoteClient` (Resilience4j-wrapped `POST /validate`), `AuthServiceClient` (Caffeine-cached token validation with expiry check), `JwtAuthenticationFilter` (extracts Bearer token, validates via Auth Service, sets `SecurityContextHolder`), `SecurityConfig` (stateless, CSRF disabled, CORS permissive, actuator/swagger paths open, all else `authenticated()`), `RestAuthenticationEntryPoint` + `RestAccessDeniedHandler` (JSON error responses), `@CurrentUser` argument resolver.
- **Phase 4 — Data Service client**: `DataServiceClient` with typed methods for all resources (exercises, workouts, workout-logs, foods, meals), `X-User-Id` header on user-scoped calls, error translation (404 → `ResourceNotFoundException`, 409 → `ConflictException`, 5xx → `DataServiceUnavailableException`), Resilience4j `@Retry(name="dataService")`, and timing instrumentation via `BusinessMetrics`.
- **Phase 5 — Exception handling**: `GlobalExceptionHandler` (`@RestControllerAdvice`) mapping all custom exceptions + validation errors to consistent `ErrorResponse` JSON with stable error codes.
- **Phase 6 — Workout & exercise endpoints**: `WorkoutService` + `WorkoutController` (CRUD pass-through), `ExerciseService` + `ExerciseController` (read = any role, write = `@PreAuthorize("hasRole('ADMIN')")`).
- **Phase 7 — Workout logs, foods, meals**: `WorkoutLogService` + `WorkoutLogController`, `FoodService` + `FoodController` (admin-gated writes), `MealService` + `MealController` — all proxy pattern.
- **Phase 8 — Calorie & macro calculations**: `NutritionStatsService` aggregating per-meal macros into daily/range totals with `BigDecimal` arithmetic, `MacroDelta` (vs goals), configurable timezone via `trackit.default-timezone`, 90-day range cap. `NutritionStatsController` exposing `GET /stats/nutrition/daily` and `GET /stats/nutrition/range`.
- **Phase 9 — User goals**: In-memory `GoalsService` (ConcurrentHashMap, default fallback 2000 kcal / 150g protein / 250g carbs / 65g fat). `GoalsController` at `/api/v1/users/me/goals` (GET + PUT). Documented as known limitation: goals reset on restart.
- **Phase 10 — Workout statistics**: `WorkoutStatsService` computing total volume (`reps × weightKg`), session count, duration, top-5 exercises by volume, and exercise progress timelines (Epley 1RM formula). `WorkoutStatsController` at `/api/v1/stats/workouts/summary` and `/exercise-progress/{exerciseId}`. 365-day range cap.
- **Phase 11 — Dashboard endpoint**: `DashboardService` aggregating today's nutrition, recent logs (5), weekly workout summary, and weekly meal count in parallel via `CompletableFuture`. `DashboardController` at `GET /api/v1/dashboard`.
- **Phase 12 — Observability**: `BusinessMetrics` component with custom Prometheus counters/timers (`business_auth_validations_total`, `business_data_calls_total`, `business_data_call_duration_seconds`, `business_stats_computed_total`, `business_goals_updated_total`). Actuator endpoints exposed: health, info, prometheus, metrics. Kubernetes health probes enabled.
- **Phase 13 — Tests**: Unit tests (`GoalsServiceTest`, `NutritionStatsServiceTest`, `WorkoutStatsServiceTest`, `DashboardServiceTest`, `WorkoutServiceTest`, `MealServiceTest`) + integration tests with WireMock (`AuthFilterIT`, `WorkoutFlowIT`, `AdminGuardIT`, `NutritionStatsIT`, `DashboardIT`, `ResilienceIT`). Context-loads test verifying WebClient beans and cache manager.
- **Phase 14 — Docker**: Multi-stage `Dockerfile` (Maven builder + `eclipse-temurin:21-jre-alpine`), non-root user, `HEALTHCHECK`, `.dockerignore`, `docker-compose.yml` for local dev with all three services and Postgres.
- **Phase 15 — CI/CD (GitHub Actions)**: `ci.yml` (build + test on every push, upload surefire/failsafe reports on failure), `release.yml` (build JAR, build + push Docker image to `florinionita/trackit-business-logic-service:latest` and `:sha` on push to `main`).
- **Phase 16 — Documentation**: Full `README.md` with architecture diagram, quick start, API reference, configuration table, deployment notes, custom metrics, known limitations, CI secrets. `CHANGELOG.md` finalized.

<!--
Section types you can use under each release (omit empty ones):

  ### Added       — for new features.
  ### Changed     — for changes in existing functionality.
  ### Deprecated  — for soon-to-be removed features.
  ### Removed     — for now removed features.
  ### Fixed       — for any bug fixes.
  ### Security    — in case of vulnerabilities.

Workflow:
  1. Add entries under [Unreleased] as you work.
  2. When releasing, replace [Unreleased] heading with [X.Y.Z] - YYYY-MM-DD
     and start a fresh empty [Unreleased] section above it.
-->
