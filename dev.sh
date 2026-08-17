#!/bin/bash
# Development startup - starts both backend and console
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting Summa development environment..."
echo ""

# Start backend in background
echo "[1/2] Starting backend..."
cd backend
nohup mvn spring-boot:run > /tmp/summa-backend.log 2>&1 &
BACKEND_PID=$!
echo "      Backend PID: $BACKEND_PID"
cd ..

# Wait for backend to start
echo "      Waiting for backend on :8080..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/api/auth/health > /dev/null 2>&1; then
        echo "      Backend ready!"
        break
    fi
    sleep 1
done

# Start console
echo "[2/2] Starting console..."
cd console
npm run dev &
CONSOLE_PID=$!
echo "      Console PID: $CONSOLE_PID"
cd ..

echo ""
echo "Summa is running:"
echo "  Console: http://localhost:3000"
echo "  API:     http://localhost:8080/api"
echo ""
echo "Press Ctrl+C to stop all services"

trap "kill $BACKEND_PID $CONSOLE_PID 2>/dev/null" EXIT
wait
