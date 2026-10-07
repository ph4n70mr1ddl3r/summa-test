#!/bin/bash
# Single-process startup script for development and small deployments
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Load .env file if present (for local development convenience)
if [ -f ".env" ]; then
    set -a
    ./.env
    set +a
fi

echo "Starting Summa (single-process mode)..."

# Check for Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java 21+ is required"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d. -f1)
if [ -z "$JAVA_VERSION" ] || [ "$JAVA_VERSION" -lt 21 ]; then
    echo "ERROR: Java 21+ is required"
    exit 1
fi

# Check for JWT secret
if [ -z "$SUMMA_JWT_SECRET" ]; then
    echo "ERROR: SUMMA_JWT_SECRET environment variable is required"
    exit 1
fi
if [ "${#SUMMA_JWT_SECRET}" -lt 32 ]; then
    echo "ERROR: SUMMA_JWT_SECRET must be at least 32 characters"
    exit 1
fi

echo "Java version: $(java -version 2>&1 | head -n 1)"

# Find the backend JAR
if [ ! -d "backend/target" ]; then
    echo "ERROR: backend/target/ not found. Run 'npm run build:backend' (or 'cd backend && mvn package') first."
    exit 1
fi
JAR_FILE=$(find backend/target -maxdepth 1 -name 'summa-backend-*.jar' ! -name '*-sources.jar' ! -name '*-plain.jar' -print -quit)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Backend JAR not found in backend/target/. Run 'npm run build:backend' (or 'cd backend && mvn package') first."
    exit 1
fi
echo "Using JAR: $JAR_FILE"

# Create data directories
mkdir -p ~/.summa
SUMMA_DNA_REPO="${SUMMA_DNA_REPO:-$HOME/.summa/dna}"
SUMMA_DB_PATH="${SUMMA_DB_PATH:-$HOME/.summa/summa.db}"
SUMMA_LOG_DIR="${SUMMA_LOG_DIR:-$HOME/.summa/logs}"
export SUMMA_LOG_DIR
mkdir -p "$SUMMA_DNA_REPO" "$(dirname "$SUMMA_DB_PATH")" "$SUMMA_LOG_DIR"

# Verify write permissions
for dir in "$SUMMA_DNA_REPO" "$(dirname "$SUMMA_DB_PATH")" "$SUMMA_LOG_DIR"; do
  if [ ! -w "$dir" ]; then
    echo "ERROR: Cannot write to $dir"
    exit 1
  fi
done

# Start the backend
echo "Starting backend on port 8080..."
JAVA_OPTS="${JAVA_OPTS:--Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m}"
export SUMMA_LOG_DIR
exec java "$JAVA_OPTS" \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
    -Dsumma.auth.local-auth-enabled=${SUMMA_LOCAL_AUTH_ENABLED:-true} \
    -jar "$JAR_FILE"
