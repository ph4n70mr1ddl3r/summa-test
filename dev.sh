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

JAVA_VERSION=$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d. -f1)
if [ -z "$JAVA_VERSION" ] || [ "$JAVA_VERSION" -lt 21 ]; then
    echo "ERROR: Java 21+ is required"
    exit 1
fi

# Check for Node.js
if ! command -v node &> /dev/null; then
    echo "ERROR: Node.js is required"
    exit 1
fi

NODE_VERSION=$(node -e "console.log(process.version.replace('v','').split('.')[0])")
if [ "$NODE_VERSION" -lt 22 ]; then
    echo "ERROR: Node.js 22+ is required, found $NODE_VERSION"
    exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven 3.9+ is required"
    exit 1
fi

MAVEN_VERSION=$(mvn -v 2>&1 | sed -n 's/.*Apache Maven \([0-9.]*\).*/\1/p' | head -1)
if [ -n "$MAVEN_VERSION" ]; then
    MAVEN_MAJOR=$(echo "$MAVEN_VERSION" | cut -d'.' -f1)
    MAVEN_MINOR=$(echo "$MAVEN_VERSION" | cut -d'.' -f2)
    if [ "$MAVEN_MAJOR" -lt 3 ] || { [ "$MAVEN_MAJOR" -eq 3 ] && [ "$MAVEN_MINOR" -lt 9 ]; }; then
        echo "ERROR: Maven 3.9+ is required, found $MAVEN_VERSION"
        exit 1
    fi
fi

# Check for Python (required for spec lint tooling)
if ! command -v python3 &> /dev/null; then
    echo "WARNING: python3 not found. Spec linting will be unavailable."
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

# Create data directories
mkdir -p ~/.summa ~/.summa/logs
SUMMA_DNA_REPO="${SUMMA_DNA_REPO:-$HOME/.summa/dna}"
SUMMA_DB_PATH="${SUMMA_DB_PATH:-$HOME/.summa/summa.db}"
mkdir -p "$SUMMA_DNA_REPO" "$(dirname "$SUMMA_DB_PATH")"

# Start backend in background
echo "[1/2] Starting backend..."
pushd backend > /dev/null
nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.jvmArguments="-Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m" \
    -Dsumma.auth.local-auth-enabled=${SUMMA_LOCAL_AUTH_ENABLED:-true} \
    > ~/.summa/logs/backend.log 2>&1 &
BACKEND_PID=$!
popd > /dev/null
echo "      Backend PID: $BACKEND_PID"

# Wait for backend to start
echo "      Waiting for backend on :8080..."
BACKEND_READY=false
for i in $(seq 1 30); do
    if curl -sf http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "      Backend ready!"
        BACKEND_READY=true
        break
    fi
    sleep 1
done

if [ "$BACKEND_READY" = false ]; then
    echo "ERROR: Backend did not start within 30 seconds. Check ~/.summa/logs/backend.log"
    exit 1
fi

# Start console
echo "[2/2] Starting console..."
pushd console > /dev/null
if [ ! -d "node_modules" ]; then
    echo "      Installing console dependencies..."
    npm ci --prefer-offline 2>/dev/null || npm install
fi
npm run dev > ~/.summa/logs/console.log 2>&1 &
CONSOLE_PID=$!
echo "      Console PID: $CONSOLE_PID"
popd > /dev/null

# Wait for console to start
echo "      Waiting for console on :3000..."
CONSOLE_READY=false
for i in $(seq 1 20); do
    if curl -sf http://localhost:3000 > /dev/null 2>&1; then
        echo "      Console ready!"
        CONSOLE_READY=true
        break
    fi
    sleep 1
done

if [ "$CONSOLE_READY" = false ]; then
    echo "WARNING: Console did not start within 20 seconds. Check ~/.summa/logs/console.log"
fi

echo ""
echo "Summa is running:"
echo "  Console: http://localhost:3000"
echo "  API:     http://localhost:8080/api"
echo ""
echo "Press Ctrl+C to stop all services"

trap '[ "${BACKEND_PID:-0}" -ne 0 ] && kill "${BACKEND_PID}" 2>/dev/null; [ "${CONSOLE_PID:-0}" -ne 0 ] && kill "${CONSOLE_PID}" 2>/dev/null; pkill -P "${CONSOLE_PID}" 2>/dev/null; exit 0' INT TERM EXIT
wait
