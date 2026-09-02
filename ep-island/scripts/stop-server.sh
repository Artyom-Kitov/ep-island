#!/bin/sh
set -eu

APP_DIR="${APP_DIR:-$HOME/ep-island}"
PID_FILE="$APP_DIR/.ep-island.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "EP Island is not running"
    exit 0
fi

APP_PID="$(sed -n '1p' "$PID_FILE")"
if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID"
    for _ in 1 2 3 4 5; do
        kill -0 "$APP_PID" 2>/dev/null || break
        sleep 1
    done
fi
rm -f "$PID_FILE"
echo "EP Island stopped"
