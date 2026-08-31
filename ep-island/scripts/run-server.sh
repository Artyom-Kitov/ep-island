#!/bin/sh
set -eu

APP_DIR="${APP_DIR:-$HOME/ep-island}"
APP_JAR="${APP_JAR:-$APP_DIR/target/ep-island-0.9.0.jar}"
APP_PORT="${PORT:-25074}"
PID_FILE="$APP_DIR/.ep-island.pid"
LOG_FILE="$APP_DIR/ep-island.log"

if [ ! -f "$APP_JAR" ]; then
    echo "JAR not found: $APP_JAR" >&2
    exit 1
fi

if [ -f "$PID_FILE" ]; then
    OLD_PID="$(sed -n '1p' "$PID_FILE")"
    if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
        echo "EP Island is already running (PID $OLD_PID)" >&2
        exit 1
    fi
fi

DB_SECRET="$(awk -F: '$4 == "s507491" { print $5; exit }' "$HOME/.pgpass")"
if [ -z "$DB_SECRET" ]; then
    echo "PostgreSQL password was not found in ~/.pgpass" >&2
    exit 1
fi

cd "$APP_DIR"
nohup env \
    PORT="$APP_PORT" \
    DB_URL="jdbc:postgresql://pg:5432/studs?currentSchema=s507491" \
    DB_USER="s507491" \
    DB_PASSWORD="$DB_SECRET" \
    DB_POOL_SIZE="3" \
    java -Xms32m -Xmx192m -XX:MaxMetaspaceSize=128m -jar "$APP_JAR" \
    >"$LOG_FILE" 2>&1 &
APP_PID=$!
echo "$APP_PID" > "$PID_FILE"
sleep 2

if ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "EP Island failed to start. See $LOG_FILE" >&2
    exit 1
fi

echo "EP Island started: PID $APP_PID, port $APP_PORT"
