#!/bin/sh
set -eu

cd "$(dirname "$0")/.."

if docker compose version >/dev/null 2>&1; then
    docker compose down
elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose down
else
    echo 'Docker Compose не найден.' >&2
    exit 1
fi
