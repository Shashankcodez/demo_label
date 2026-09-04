# =============================================================================
# LabelCheck Backend — Render Docker Deployment
# Spring Boot 3.4.3 / Java 21 / Maven
# =============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Copy Maven wrapper and POM first to leverage Docker layer cache for dependencies.
# Dependencies are only re-downloaded when pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download all dependencies (offline-friendly cache layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q

# Copy source code
COPY src ./src

# Build the application JAR, skipping tests (tests require Tesseract native libs)
RUN ./mvnw package -DskipTests -B -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

# Create a non-root user for security
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

WORKDIR /app

# Copy the built JAR from the build stage
# Artifact name is defined by pom.xml: artifactId=backend, version=0.0.1-SNAPSHOT
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Create directories for temporary uploads and H2 database
# These are ephemeral on Render free tier (reset on redeploy — acceptable for demo)
RUN mkdir -p /app/uploads /app/data && chown -R appuser:appgroup /app

USER appuser

# Render exposes its assigned port via the $PORT environment variable.
# We pass it to Spring Boot at runtime via --server.port so the source
# code and application.properties do not need modification.
# server.address=0.0.0.0 is already set in application.properties.
ENV PORT=8080

EXPOSE ${PORT}

# Health check using the existing /api/v1/health endpoint.
# Render also performs its own HTTP health check on $PORT.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT}/api/v1/health || exit 1

# Start the Spring Boot application.
# --server.port=${PORT} overrides the hardcoded 8080 in application.properties
# without modifying any source files.
ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT}"]
