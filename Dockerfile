FROM maven:3.9.15-eclipse-temurin-26-alpine AS builder

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

# Install curl for healthcheck
RUN apk add --no-cache curl

COPY --from=builder /build/backend/target/summa-backend-*.jar /app/app.jar
RUN if [ ! -f /app/app.jar ]; then echo "ERROR: No JAR found in build output" >&2; exit 1; fi

# Create data directories and non-root user
RUN addgroup -g 1000 -S summa && adduser -u 1000 -S summa -G summa && \
    mkdir -p /data/dna /data/db && chown -R 1000:1000 /data

ENV SUMMA_DB_PATH=/data/db/summa.db \
    SUMMA_DNA_REPO=/data/dna \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m"

EXPOSE 8080

USER 1000
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=20s \
  CMD curl -sf http://localhost:8080/api/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java \"$JAVA_OPTS\" -jar app.jar"]
