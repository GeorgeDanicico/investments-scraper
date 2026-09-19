#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

IMAGE_NAME="${IMAGE_NAME:-ghcr.io/georgedanicico/investments-scraper:${IMAGE_TAG:-native}}"
CONTAINER_NAME="${CONTAINER_NAME:-investment}"
HOST_PORT="${HOST_PORT:-8080}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
LOG_VOLUME="${LOG_VOLUME:-investment_logs}"
STOP_TIMEOUT="${STOP_TIMEOUT:-30}"

if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is not installed or is not available in PATH." >&2
    exit 1
fi

docker info >/dev/null

echo "Pulling image: $IMAGE_NAME"
docker pull "$IMAGE_NAME"

if docker container inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
    if [[ "$(docker inspect --format '{{.State.Running}}' "$CONTAINER_NAME")" == "true" ]]; then
        echo "Stopping container: $CONTAINER_NAME"
        docker stop --time "$STOP_TIMEOUT" "$CONTAINER_NAME" >/dev/null
    fi

    echo "Removing container: $CONTAINER_NAME"
    docker rm "$CONTAINER_NAME" >/dev/null
fi

docker volume inspect "$LOG_VOLUME" >/dev/null 2>&1 || \
    docker volume create "$LOG_VOLUME" >/dev/null

echo "Starting container: $CONTAINER_NAME"
docker run --detach \
    --name "$CONTAINER_NAME" \
    --restart unless-stopped \
    --publish "$HOST_PORT:$CONTAINER_PORT" \
    --mount "type=volume,source=$LOG_VOLUME,target=/app/logs" \
    "$IMAGE_NAME"

if command -v curl >/dev/null 2>&1; then
    echo "Waiting for the application health endpoint..."
    for _ in {1..30}; do
        if curl --fail --silent --show-error --max-time 2 \
            "http://127.0.0.1:$HOST_PORT/health" >/dev/null; then
            echo "Deployment completed successfully."
            exit 0
        fi

        if [[ "$(docker inspect --format '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null || true)" != "true" ]]; then
            echo "Container exited during startup:" >&2
            docker logs --tail 100 "$CONTAINER_NAME" >&2 || true
            exit 1
        fi

        sleep 1
    done

    echo "Application did not become healthy within 30 seconds:" >&2
    docker logs --tail 100 "$CONTAINER_NAME" >&2 || true
    exit 1
fi

echo "Deployment started. Install curl to enable the health check."
