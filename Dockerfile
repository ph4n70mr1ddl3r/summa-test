FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY backend/pom.xml backend/pom.xml
# Pre-fetch dependencies for better layer caching
RUN mvn -q -B -f backend/pom.xml dependency:go-offline

COPY backend/src backend/src
RUN mvn -q -B -f backend/pom.xml clean package -DskipTests

FROM eclipse-temurin:21.0.4_7-jre-alpine
LABEL maintainer="summa-team"
LABEL org.opencontainers.image.source="https://github.com/summa-org/summa"

WORKDIR /app

# wget is available in alpine by default; use it for the healthcheck to avoid
# adding the curl package and its transitive dependencies.

# Copy the executable JAR from the builder stage
COPY --from=builder /build/backend/target/summa-backend-*.jar /app/
RUN find /app -maxdepth 1 -name 'summa-backend-*.jar' ! -name '*-sources.jar' ! -name '*-plain.jar' -print -quit | xargs -I{} mv {} /app/app.jar && \
    rm -f /app/*-plain.jar /app/*-sources.jar

# Create data directories and non-root user
RUN addgroup -g 1000 -S summa && adduser -u 1000 -S summa -G summa && \
    mkdir -p /data/dna /data/db /data/logs && chown -R 1000:1000 /data

ENV SUMMA_DB_PATH=/data/db/summa.db \
    SUMMA_DNA_REPO=/data/dna \
    SUMMA_LOG_DIR=/data/logs \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m"

EXPOSE 8080

USER 1000
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=20s \
  CMD wget --spider -q http://localhost:8080/api/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java \"$JAVA_OPTS\" -jar app.jar"]
