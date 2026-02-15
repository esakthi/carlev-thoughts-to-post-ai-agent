#!/usr/bin/env bash
set -euo pipefail

# Script to build and run Dockerfiles individually for testing

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WSL_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"

echo "=========================================="
echo "   Docker Individual Test Utility"
echo "=========================================="
echo "WSL IP: $WSL_IP"
echo "------------------------------------------"
echo "Select a project to test:"
echo "1) AI Agent (Python)"
echo "2) API Service (Spring Boot)"
echo "3) Angular UI (Nginx)"
echo "4) Exit"
read -p "Enter choice [1-4]: " choice

case $choice in
    1)
        PROJECT_DIR="thoughts-to-post-ai-agent"
        IMAGE_NAME="thoughts-ai-agent"
        ENV_VARS="-e KAFKA_BOOTSTRAP_SERVERS=$WSL_IP:9092 -e OLLAMA_HOST=http://$WSL_IP:11434"
        PORTS=""
        ;;
    2)
        PROJECT_DIR="thoughts-to-post-api-service"
        IMAGE_NAME="thoughts-api-service"
        GATEWAY_IP="$(ip route show | grep default | awk '{print $3}')"
        ENV_VARS="-e SPRING_KAFKA_BOOTSTRAP_SERVERS=$WSL_IP:9092 -e SPRING_DATA_MONGODB_URI=mongodb://admin:admin123@$GATEWAY_IP:27017/thoughtstopost?authSource=admin"
        PORTS="-p 8080:8080"
        ;;
    3)
        PROJECT_DIR="thoughts-to-post-angular-ui"
        IMAGE_NAME="thoughts-angular-ui"
        ENV_VARS=""
        PORTS="-p 4200:80"
        ;;
    4)
        exit 0
        ;;
    *)
        echo "Invalid choice."
        exit 1
        ;;
esac

cd "$REPO_DIR/$PROJECT_DIR"

echo ""
echo "--- Building $IMAGE_NAME ---"
echo "Note: Build logs will appear below. Check for 'Error' or 'Red' text if it fails."
docker build -t "$IMAGE_NAME" .

echo ""
read -p "Build successful. Do you want to run the container? [y/N]: " run_choice
if [[ "$run_choice" =~ ^[Yy]$ ]]; then
    echo ""
    echo "How long should the container run?"
    echo "1) 10 sec"
    echo "2) 1 minute"
    echo "3) Run indefinitely (manual stop)"
    read -p "Enter choice [1-3]: " time_choice

    case $time_choice in
        1) DURATION=0 ;;
        2) DURATION=60 ;;
        3) DURATION=0 ;;
        *) echo "Invalid choice, defaulting to manual stop."; DURATION=0 ;;
    esac

    echo "--- Running $IMAGE_NAME ---"
    
    # Remove existing container if it exists
    docker rm -f "$IMAGE_NAME" >/dev/null 2>&1 || true
    
    if [ "$DURATION" -eq 0 ]; then
        echo "Use 'docker logs -f $IMAGE_NAME' in another terminal to see output."
        echo "Press Ctrl+C to stop the container."
        docker run --name "$IMAGE_NAME" $PORTS $ENV_VARS "$IMAGE_NAME"
    else
        echo "Starting container for $DURATION seconds..."
        docker run -d --name "$IMAGE_NAME" $PORTS $ENV_VARS "$IMAGE_NAME"
        
        echo "Container started. Tailing logs for $DURATION seconds (Ctrl+C to stop tailing early)..."
        timeout $DURATION docker logs -f "$IMAGE_NAME" || true
        
        echo ""
        echo "Time's up! Stopping and removing container to free ports..."
        docker stop "$IMAGE_NAME" >/dev/null 2>&1 || true
        docker rm "$IMAGE_NAME" >/dev/null 2>&1 || true
        echo "Cleanup complete. Ports are now free."
    fi
fi
