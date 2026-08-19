#!/bin/bash
# Development startup - starts both backend and console
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting Summa development environment..."
echo ""

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

# Create data directories
mkdir -p ~/.summa

# Start backend in background
echo "[1/2] Starting backend..."
cd backend
nohup mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev" > /tmp/summa-backend.log 2>&1 &
BACKEND_PID=$!
echo "      Backend PID: $BACKEND_PID"
cd ..

# Wait for backend to start
echo "      Waiting for backend on :8080..."
BACKEND_READY=false
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "      Backend ready!"
        BACKEND_READY=true
        break
    fi
    sleep 1
done

if [ "$BACKEND_READY" = false ]; then
    echo "ERROR: Backend did not start within 30 seconds. Check /tmp/summa-backend.log"
    exit 1
fi

# Start console
echo "[2/2] Starting console..."
cd console
npm run dev > /tmp/summa-console.log 2>&1 &
CONSOLE_PID=$!
echo "      Console PID: $CONSOLE_PID"
cd ..

echo ""
echo "Summa is running:"
echo "  Console: http://localhost:3000"
echo "  API:     http://localhost:8080/api"
echo ""
echo "Press Ctrl+C to stop all services"

trap '[ "${BACKEND_PID:-0}" -ne 0 ] && kill "${BACKEND_PID}" 2>/dev/null; [ "${CONSOLE_PID:-0}" -ne 0 ] && kill "${CONSOLE_PID}" 2>/dev/null; exit 0' INT TERM EXIT
wait
