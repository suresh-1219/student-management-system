# ---- Build stage: compiles the jar. Nothing from this stage ships. ----
FROM eclipse-temurin:21-jdk AS build

WORKDIR /src

# Cache dependencies separately from source so `docker build` only
# re-downloads them when the pom actually changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Git does not keep the Unix executable bit on mvnw when it is committed from
# Windows, so building this image on Linux (CI, a server) would fail with
# "Permission denied" without this.
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B -DskipTests package

# ---- Run stage: just a JRE and the jar, running as a non-root user. ----
FROM eclipse-temurin:21-jre

# curl is needed for HEALTHCHECK below; the base image does not include it.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --create-home appuser
WORKDIR /app
COPY --from=build /src/target/student-management-system-*.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 9090

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD curl -f http://localhost:9090/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
