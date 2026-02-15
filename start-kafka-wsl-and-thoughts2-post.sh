#!/usr/bin/env bash
set -euo pipefail

# Run this script from the repo folder (kafka-stack-docker-compose) inside WSL2.
# It will:
# - detect current WSL IP
# - update Kafka server.properties advertised.listeners to that IP
# - start Kafka (daemon)
# - wait for Kafka port to open
# - start Conduktor (docker compose)

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

KAFKA_HOME="${KAFKA_HOME:-/home/sakthivadivel/kafka}"
KAFKA_CONFIG="${KAFKA_CONFIG:-${KAFKA_HOME}/config/server.properties}"
KAFKA_START_SH="${KAFKA_START_SH:-${KAFKA_HOME}/bin/kafka-server-start.sh}"
KAFKA_PORT="${KAFKA_PORT:-9092}"

# COMPOSE_FILE no longer needed as we use default docker-compose.yml

# Log files
LOGS_DIR="${LOGS_DIR:-${REPO_DIR}/logs}"
KAFKA_LOG="${KAFKA_LOG:-${LOGS_DIR}/kafka.log}"
KAFKA_SERVER_LOG="${KAFKA_SERVER_LOG:-${KAFKA_HOME}/logs/server.log}"
KAFKA_LOG_PID_FILE="${LOGS_DIR}/kafka-log-pid.txt"
# CONDUKTOR_LOG no longer needed

# Create logs directory if it doesn't exist
mkdir -p "$LOGS_DIR"

die() { echo "ERROR: $*" >&2; exit 1; }

command -v docker >/dev/null 2>&1 || die "docker not found in PATH"
command -v python3 >/dev/null 2>&1 || die "python3 not found in PATH (needed to safely edit server.properties)"

[[ -f "$KAFKA_CONFIG" ]] || die "Kafka config not found: $KAFKA_CONFIG"
[[ -x "$KAFKA_START_SH" ]] || die "Kafka start script not found/executable: $KAFKA_START_SH"
# [[ -f "$COMPOSE_FILE" ]] || die "Compose file not found: $COMPOSE_FILE"

WSL_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
GATEWAY_IP="$(ip route show | grep default | awk '{print $3}')"
[[ -n "$WSL_IP" ]] || die "Could not determine WSL IP via 'hostname -I'"

echo "WSL_IP=$WSL_IP"
echo "GATEWAY_IP=$GATEWAY_IP"

cd "$REPO_DIR"
# Write .env so docker compose can substitute variables.
cat > .env <<EOF
WSL_IP=$WSL_IP
GATEWAY_IP=$GATEWAY_IP
EOF
echo "Wrote ${REPO_DIR}/.env"

python3 - "$KAFKA_CONFIG" "$WSL_IP" <<'PY'
import re, sys

path, wsl_ip = sys.argv[1], sys.argv[2]

with open(path, "r", encoding="utf-8") as f:
    lines = f.read().splitlines(True)

out = []
changed = False

adv_re = re.compile(r"^\s*advertised\.listeners\s*=")
for line in lines:
    if adv_re.match(line):
        # Keep controller advertised as localhost:9093 (KRaft controller is local)
        new_line = f"advertised.listeners=PLAINTEXT://{wsl_ip}:9092,CONTROLLER://localhost:9093\n"
        out.append(new_line)
        changed = True
    else:
        out.append(line)

if not changed:
    out.append(f"\nadvertised.listeners=PLAINTEXT://{wsl_ip}:9092,CONTROLLER://localhost:9093\n")

with open(path, "w", encoding="utf-8") as f:
    f.writelines(out)
PY

echo "Updated advertised.listeners in: $KAFKA_CONFIG"

KAFKA_RUNNING=false
if pgrep -f "kafka\.Kafka" >/dev/null 2>&1; then
  echo "Kafka process found. Checking if it's reachable on $WSL_IP:$KAFKA_PORT ..."
  if (echo >"/dev/tcp/${WSL_IP}/${KAFKA_PORT}") >/dev/null 2>&1; then
    echo "Kafka is already running and reachable on $WSL_IP:$KAFKA_PORT. Skipping restart."
    KAFKA_RUNNING=true
  else
    echo "Kafka is running but NOT reachable on $WSL_IP:$KAFKA_PORT (IP might have changed)."
    echo "Stopping Kafka to update configuration..."
    pkill -f "kafka\.Kafka" || true
    sleep 2
  fi
fi

if [ "$KAFKA_RUNNING" = false ]; then
  echo "Initializing Kafka storage (if needed)..."
  # Generate a random UUID, but silence errors if the storage is already formatted
  KAFKA_CLUSTER_ID="$($KAFKA_HOME/bin/kafka-storage.sh random-uuid 2>/dev/null || echo "reusing-existing-id")"
  # Try to format, but redirect stderr to null to avoid noisy Java exceptions if already formatted
  $KAFKA_HOME/bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID -c $KAFKA_HOME/config/server.properties >/dev/null 2>&1 || {
    echo "Note: Kafka storage is already formatted or using an existing cluster ID."
  }

  echo "Starting Kafka..."
  echo "Kafka server log: $KAFKA_SERVER_LOG"
  "$KAFKA_START_SH" -daemon "$KAFKA_CONFIG" || {
    echo "ERROR: Failed to start Kafka. Check Kafka logs under: $KAFKA_HOME/logs" >&2
    exit 1
  }

  echo "Waiting for Kafka to listen on $WSL_IP:$KAFKA_PORT ..."
  for i in {1..60}; do
    # bash TCP check
    if (echo >"/dev/tcp/${WSL_IP}/${KAFKA_PORT}") >/dev/null 2>&1; then
      echo "Kafka is reachable on $WSL_IP:$KAFKA_PORT"
      break
    fi
    if [[ "$i" == "60" ]]; then
      die "Kafka did not become reachable on $WSL_IP:$KAFKA_PORT within 60 seconds"
    fi
    sleep 1
  done
fi

# Capture Kafka logs into project logs directory
if [[ -f "$KAFKA_SERVER_LOG" ]]; then
  echo "Capturing Kafka server logs into: $KAFKA_LOG"
  # Initial snapshot
  tail -n 200 "$KAFKA_SERVER_LOG" > "$KAFKA_LOG" 2>/dev/null || true
  # Background tail to keep kafka.log updated
  (
    tail -F "$KAFKA_SERVER_LOG" >> "$KAFKA_LOG" 2>&1
  ) &
  KAFKA_LOG_PID=$!
  echo "$KAFKA_LOG_PID" > "$KAFKA_LOG_PID_FILE"
  echo "Started Kafka log capture (PID: $KAFKA_LOG_PID)"
else
  echo "WARNING: Kafka server log file not found at $KAFKA_SERVER_LOG; kafka.log will remain empty."
fi

echo "----------------------------------------------------------"
read -p "Do you want to rebuild Docker images from source code? (y/n, default: n): " rebuild_choice
rebuild_choice=${rebuild_choice:-n}

BUILD_FLAG=""
if [[ "$rebuild_choice" =~ ^[Yy]$ ]]; then
    echo "Rebuilding images from source code..."
    BUILD_FLAG="--build"
else
    echo "Checking for existing local images..."
    # Get all service names, filtering out potential warnings or empty lines
    # Only keep lines that look like service names (alphanumeric, -, _)
    SERVICES=$(docker compose config --services 2>/dev/null | grep -E '^[a-zA-Z0-9_-]+$' || echo "")
    
    if [[ -z "$SERVICES" ]]; then
        echo "Warning: Could not detect services from docker-compose.yml."
        echo "This might happen if docker compose is not responding or if .env is missing."
        echo "Falling back to rebuild from source..."
        MISSING_IMAGE=true
    else
        MISSING_IMAGE=false
        for service in $SERVICES; do
            echo "Checking image for service: $service"
            # docker compose images -q can return non-zero if service is not found 
            # or if there's a connectivity issue. We use || true to stay alive.
            IMAGE_ID=$(docker compose images -q "$service" 2>/dev/null || echo "")
            if [[ -z "$IMAGE_ID" ]]; then
                echo "  -> MISSING: Local image for '$service' not found."
                MISSING_IMAGE=true
            fi
        done
    fi

    if [ "$MISSING_IMAGE" = true ]; then
        echo "Notice: Falling back to rebuild from source to ensure all images are available."
        BUILD_FLAG="--build"
    else
        echo "All necessary images found in local repository. Reusing existing images."
        BUILD_FLAG=""
    fi
fi
echo "----------------------------------------------------------"

echo "Starting sub-projects (API, Agent, UI) via Docker Compose with WSL_IP=$WSL_IP ..."
docker compose up $BUILD_FLAG -d

echo ""
echo "=========================================="
echo "✓ Platform started successfully!"
echo "=========================================="
echo "Angular UI: http://localhost:4200"
echo "API Service: http://localhost:8080"
echo "WSL IP (for local dev): $WSL_IP"
echo "=========================================="

