#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

APP_NAME="${APP_NAME:-investment}"
CONTAINER_NAME="${CONTAINER_NAME:-$APP_NAME}"
IMAGE_NAME="${IMAGE_NAME:-$APP_NAME:latest}"
DEPLOY_VARIANT="${DEPLOY_VARIANT:-native}"
HOST_PORT="${HOST_PORT:-8080}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
LOG_VOLUME="${LOG_VOLUME:-${APP_NAME}_logs}"
STOP_TIMEOUT="${STOP_TIMEOUT:-30}"

case "$DEPLOY_VARIANT" in
    native)
        DOCKERFILE_PATH="${DOCKERFILE_PATH:-native-image/Dockerfile}"
        ;;
    jvm)
        DOCKERFILE_PATH="${DOCKERFILE_PATH:-jvm-image/Dockerfile}"
        ;;
    *)
        echo "Unsupported DEPLOY_VARIANT: $DEPLOY_VARIANT (expected native or jvm)" >&2
        exit 1
        ;;
esac

if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is not installed or is not available in PATH." >&2
    exit 1
fi

if [[ ! -f "$DOCKERFILE_PATH" ]]; then
    echo "Dockerfile not found: $DOCKERFILE_PATH" >&2
    exit 1
fi

docker info >/dev/null
docker volume inspect "$LOG_VOLUME" >/dev/null 2>&1 || docker volume create "$LOG_VOLUME" >/dev/null

container_exists=false
container_was_running=false

if docker container inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
    container_exists=true
    if [[ "$(docker inspect --format '{{.State.Running}}' "$CONTAINER_NAME")" == "true" ]]; then
        container_was_running=true
        echo "Stopping container: $CONTAINER_NAME"
        docker stop --time "$STOP_TIMEOUT" "$CONTAINER_NAME" >/dev/null
    else
        echo "Container is already stopped: $CONTAINER_NAME"
    fi
fi

echo "Building image: $IMAGE_NAME"
if ! docker build --file "$DOCKERFILE_PATH" --tag "$IMAGE_NAME" "$SCRIPT_DIR"; then
    if [[ "$container_was_running" == "true" && "$container_exists" == "true" ]]; then
        echo "Image build failed; starting the previous container again." >&2
        docker start "$CONTAINER_NAME" >/dev/null || true
    fi
    exit 1
fi

if [[ "$container_exists" == "true" ]]; then
    docker rm "$CONTAINER_NAME" >/dev/null
fi

echo "Starting container: $CONTAINER_NAME"
docker run --detach \
    --name "$CONTAINER_NAME" \
    --network expense-network \
    --restart unless-stopped \
    --publish "$HOST_PORT:$CONTAINER_PORT" \
    --mount "type=volume,source=$LOG_VOLUME,target=/app/logs" \
    "$IMAGE_NAME"

if command -v curl >/dev/null 2>&1; then
    echo "Waiting for the application health endpoint..."
    for _ in {1..30}; do
        if curl --fail --silent --show-error --max-time 2 "http://127.0.0.1:$HOST_PORT/health" >/dev/null; then
            echo "Deployment completed successfully."
            exit 0
        fi

        if [[ "$(docker inspect --format '{{.State.Running}}' "$CONTAINER_NAME")" != "true" ]]; then
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
