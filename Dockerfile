FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY backend/pom.xml backend/pom.xml
# Pre-fetch dependencies for better layer caching
RUN cd backend && mvn -q -B dependency:go-offline

COPY backend/src backend/src
RUN cd backend && mvn -q -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="summa-team"
LABEL org.opencontainers.image.source="https://github.com/summa-org/summa"

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

COPY --from=builder /build/backend/target/summa-backend-*.jar app.jar

# Create data directories and non-root user
RUN addgroup -g 1000 -S summa && adduser -u 1000 -S summa -G summa && \
    mkdir -p /data/dna /data/db && chown -R 1000:1000 /data

ENV SUMMA_DB_PATH=/data/db/summa.db \
    SUMMA_DNA_REPO=/data/dna \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xmx512m -Xms256m"

EXPOSE 8080

USER 1000
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
