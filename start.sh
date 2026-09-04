#!/bin/bash
# Single-process startup script for development and small deployments
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting Summa (single-process mode)..."

# Check for Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java 21+ is required"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "ERROR: Java 21+ is required, found $JAVA_VERSION"
    exit 1
fi

# Check for JWT secret
if [ -z "$SUMMA_JWT_SECRET" ]; then
    echo "ERROR: SUMMA_JWT_SECRET environment variable is required"
    exit 1
fi

echo "Java version: $(java -version 2>&1 | head -n 1)"

# Find the backend JAR
JAR_FILE=$(ls backend/target/summa-backend-*.jar 2>/dev/null | grep -v sources | grep -v plain | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Backend JAR not found. Run 'npm run build:backend' (or 'cd backend && mvn package') first."
    exit 1
fi
echo "Using JAR: $JAR_FILE"

# Create data directories
mkdir -p ~/.summa

# Start the backend
echo "Starting backend on port 8080..."
exec java \
    -Xmx512m \
    -Xms256m \
    -Dspring.profiles.active=prod \
    -jar "$JAR_FILE"
