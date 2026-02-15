#!/usr/bin/env bash
set -euo pipefail

# This script stops the application "pods" (API, Agent, UI) 
# that are managed by docker compose.
# It does NOT stop Kafka or MongoDB.

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_DIR"

echo "Stopping application services (API, Agent, UI)..."
docker compose down

echo ""
echo "=========================================="
echo "✓ Application services stopped."
echo "Note: Native Kafka and external MongoDB (if running) are still active."
echo "=========================================="
