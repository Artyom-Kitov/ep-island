#!/bin/sh
set -eu

cd "$(dirname "$0")/.."

if docker compose version >/dev/null 2>&1; then
    compose() { docker compose "$@"; }
elif command -v docker-compose >/dev/null 2>&1; then
    compose() { docker-compose "$@"; }
else
    echo 'Docker Compose не найден. Установите Docker Desktop с Compose.' >&2
    exit 1
fi

compose up -d --build

application_id="$(compose ps -q application)"
attempt=0
health="starting"
while [ "$attempt" -lt 30 ]; do
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$application_id")"
    [ "$health" = "healthy" ] && break
    if [ "$health" = "unhealthy" ] || [ "$health" = "exited" ]; then
        compose logs --tail=100 application
        echo 'EP Island не запустился: проверьте лог выше.' >&2
        exit 1
    fi
    attempt=$((attempt + 1))
    sleep 1
done

compose ps

echo
if [ "$health" != "healthy" ]; then
    echo 'Контейнер не успел перейти в healthy за 30 секунд.' >&2
    echo 'Проверьте: docker-compose logs application' >&2
    exit 1
fi

echo 'EP Island готов: http://127.0.0.1:25074'
echo 'Логи приложения: docker-compose logs -f application'
