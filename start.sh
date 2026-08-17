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

echo "Java version: $(java -version 2>&1 | head -n 1)"

# Create data directories
mkdir -p ~/.summa/db ~/.summa/dna

# Start the backend
echo "Starting backend on port 8080..."
exec java \
    -Xmx512m \
    -Xms256m \
    -Dspring.profiles.active=default \
    -jar backend/target/summa-backend-*.jar
