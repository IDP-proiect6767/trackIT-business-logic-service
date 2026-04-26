# ── Builder stage ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Cache dependencies first (layer caching optimisation)
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source and build (skip tests — they ran in CI)
COPY src/ src/
RUN mvn -B -DskipTests package

# ── Runtime stage ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1

USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
