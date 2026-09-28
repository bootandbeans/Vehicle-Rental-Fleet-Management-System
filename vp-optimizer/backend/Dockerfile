# ---------------------------------------------------------------------------
# Build stage - Maven builds the Spring Boot jar
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Dependencies first so that a source-only change keeps the dependency layer cached.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package && cp target/*.jar /workspace/app.jar

# ---------------------------------------------------------------------------
# Runtime stage - slim JRE, non-root user
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar

# Translates the single DATABASE_URL that managed platforms hand out into the JDBC settings Spring
# needs. A no-op for docker compose / local runs, which configure DB_HOST, DB_PORT, ... themselves.
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod 755 /app/entrypoint.sh

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport" \
    SPRING_PROFILES_ACTIVE=docker

# Flyway migrates and Hibernate validates on startup, so the health probe is a meaningful readiness
# signal for docker compose. Platforms that inject PORT (Render, Railway, Cloud Run) are honoured
# through server.port: ${PORT:...}.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD wget -qO- "http://localhost:${PORT:-8080}/api/health" || exit 1

ENTRYPOINT ["/app/entrypoint.sh"]
