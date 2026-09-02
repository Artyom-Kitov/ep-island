#!/bin/sh
set -eu

BASE_URL="${1:-http://127.0.0.1:25074}"
CURL="${CURL:-curl}"
OFFICER_COOKIE="$(mktemp "${TMPDIR:-/tmp}/ep-island-officer.XXXXXX")"
REGISTRAR_COOKIE="$(mktemp "${TMPDIR:-/tmp}/ep-island-registrar.XXXXXX")"
EVENTS_FILE="$(mktemp "${TMPDIR:-/tmp}/ep-island-events.XXXXXX")"
ERROR_FILE="$(mktemp "${TMPDIR:-/tmp}/ep-island-error.XXXXXX")"
EVENTS_PID=""

cleanup() {
    if [ -n "$EVENTS_PID" ]; then
        kill "$EVENTS_PID" 2>/dev/null || true
    fi
    rm -f "$OFFICER_COOKIE" "$REGISTRAR_COOKIE" "$EVENTS_FILE" "$ERROR_FILE"
}
trap cleanup EXIT INT TERM

login() {
    cookie="$1"
    username="$2"
    "$CURL" -fsS -b "$cookie" -c "$cookie" \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"$username\",\"password\":\"$username\"}" \
        "$BASE_URL/api/session/login"
}

wait_for_event() {
    expected="$1"
    attempts=0
    while [ "$attempts" -lt 40 ]; do
        if grep -q "$expected" "$EVENTS_FILE"; then
            return 0
        fi
        attempts=$((attempts + 1))
        sleep 0.25
    done
    echo "FAIL: SSE event not received: $expected" >&2
    sed -n '1,80p' "$EVENTS_FILE" >&2
    exit 1
}

login "$OFFICER_COOKIE" officer >/dev/null
login "$REGISTRAR_COOKIE" registrar >/dev/null

"$CURL" -fsSN -b "$REGISTRAR_COOKIE" "$BASE_URL/api/events" >"$EVENTS_FILE" &
EVENTS_PID=$!
wait_for_event '"scope":"ALL"'

RUN_TOKEN="$(date +%Y%m%d%H%M%S)-$$"
FULL_NAME="Синхронизация $RUN_TOKEN"
REFERRAL="$($CURL -fsS -b "$OFFICER_COOKIE" -c "$OFFICER_COOKIE" \
    -H 'Content-Type: application/json' -H "Idempotency-Key: realtime-$RUN_TOKEN" \
    -d "{\"fullName\":\"$FULL_NAME\",\"debtAmount\":100,\"reason\":\"Межсессионный тест\",\"documents\":\"SSE\"}" \
    "$BASE_URL/api/referrals")"
REFERRAL_ID="$(printf '%s' "$REFERRAL" | grep -o '"id":[0-9]*' | head -n 1 | cut -d: -f2)"
[ -n "$REFERRAL_ID" ] || { echo 'FAIL: referral id' >&2; exit 1; }
wait_for_event "\"entityId\":\"$REFERRAL_ID\""

SEARCH="$($CURL -fsS -b "$REGISTRAR_COOKIE" -G \
    --data-urlencode "fullName=$FULL_NAME" --data-urlencode 'limit=12' \
    "$BASE_URL/api/referrals/search")"
printf '%s' "$SEARCH" | grep -q "\"id\":$REFERRAL_ID" || {
    echo 'FAIL: created referral is not visible to registrar' >&2
    exit 1
}
printf '%s' "$SEARCH" | grep -q '"status":"CREATED"' || {
    echo 'FAIL: new referral status' >&2
    exit 1
}

EARLY_STATUS="$($CURL -sS -o "$ERROR_FILE" -w '%{http_code}' -b "$REGISTRAR_COOKIE" \
    -H 'Content-Type: application/json' -d "{\"referralId\":$REFERRAL_ID}" \
    "$BASE_URL/api/residents")"
[ "$EARLY_STATUS" = '409' ] || {
    echo "FAIL: arrival before convoy handoff returned HTTP $EARLY_STATUS" >&2
    exit 1
}

"$CURL" -fsS -X PATCH -b "$OFFICER_COOKIE" \
    -H 'Content-Type: application/json' -d '{"status":"HANDED_TO_CONVOY"}' \
    "$BASE_URL/api/referrals/$REFERRAL_ID/status" >/dev/null

READY="$($CURL -fsS -b "$REGISTRAR_COOKIE" -G \
    --data-urlencode "fullName=$FULL_NAME" "$BASE_URL/api/referrals/search")"
printf '%s' "$READY" | grep -q '"status":"HANDED_TO_CONVOY"' || {
    echo 'FAIL: handoff is not visible to registrar' >&2
    exit 1
}

RESIDENT="$($CURL -fsS -b "$REGISTRAR_COOKIE" \
    -H 'Content-Type: application/json' -d "{\"referralId\":$REFERRAL_ID}" \
    "$BASE_URL/api/residents")"
RESIDENT_ID="$(printf '%s' "$RESIDENT" | grep -o '"id":"[^"]*"' | head -n 1 | cut -d\" -f4)"
[ "${#RESIDENT_ID}" -eq 10 ] || { echo 'FAIL: resident id' >&2; exit 1; }
wait_for_event '"scope":"RESIDENTS"'

AFTER_ARRIVAL="$($CURL -fsS -b "$REGISTRAR_COOKIE" -G \
    --data-urlencode "fullName=$FULL_NAME" "$BASE_URL/api/referrals/search")"
if printf '%s' "$AFTER_ARRIVAL" | grep -q "\"id\":$REFERRAL_ID"; then
    echo 'FAIL: registered referral is still pending arrival' >&2
    exit 1
fi

printf '{"status":"PASS","referralId":%s,"residentId":"%s","sessions":2}\n' \
    "$REFERRAL_ID" "$RESIDENT_ID"
