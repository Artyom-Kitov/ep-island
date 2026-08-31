#!/bin/sh
set -eu

BASE_URL="${1:-http://127.0.0.1:25074}"
CURL="${CURL:-curl}"
COOKIE_FILE="$(mktemp "${TMPDIR:-/tmp}/ep-island-smoke.XXXXXX")"
trap 'rm -f "$COOKIE_FILE"' EXIT INT TERM

json_post() {
    path="$1"
    body="$2"
    "$CURL" -fsS -b "$COOKIE_FILE" -c "$COOKIE_FILE" \
        -H 'Content-Type: application/json' -d "$body" "$BASE_URL$path"
}

json_patch() {
    path="$1"
    body="$2"
    "$CURL" -fsS -X PATCH -b "$COOKIE_FILE" -c "$COOKIE_FILE" \
        -H 'Content-Type: application/json' -d "$body" "$BASE_URL$path"
}

require_contains() {
    value="$1"
    expected="$2"
    label="$3"
    printf '%s' "$value" | grep -q "$expected" || {
        echo "FAIL: $label" >&2
        exit 1
    }
}

LOGIN="$(json_post /api/session/login '{"username":"admin","password":"admin"}')"
require_contains "$LOGIN" '"role":"ADMIN"' 'demo session login'

HEALTH="$("$CURL" -fsS "$BASE_URL/api/health")"
require_contains "$HEALTH" '"status":"UP"' 'health endpoint'

RUN_KEY="acceptance-$(date +%Y%m%d%H%M%S)-$$"
REFERRAL="$("$CURL" -fsS -b "$COOKIE_FILE" -c "$COOKIE_FILE" \
    -H 'Content-Type: application/json' -H "Idempotency-Key: $RUN_KEY" \
    -d '{"fullName":"Приёмочный Коротышка","birthDate":"1990-01-01","debtAmount":12345.60,"reason":"Сквозной тест MVP","documents":"TEST-MPI"}' \
    "$BASE_URL/api/referrals")"
REFERRAL_ID="$(printf '%s' "$REFERRAL" | grep -o '"id":[0-9]*' | head -n 1 | cut -d: -f2)"
[ -n "$REFERRAL_ID" ] || { echo 'FAIL: referral id' >&2; exit 1; }

HANDED="$(json_patch "/api/referrals/$REFERRAL_ID/status" '{"status":"HANDED_TO_CONVOY"}')"
require_contains "$HANDED" '"status":"HANDED_TO_CONVOY"' 'referral handoff'

RESIDENT="$(json_post /api/residents "{\"referralId\":$REFERRAL_ID}")"
RESIDENT_ID="$(printf '%s' "$RESIDENT" | grep -o '"id":"[^"]*"' | head -n 1 | cut -d\" -f4)"
[ "${#RESIDENT_ID}" -eq 10 ] || { echo 'FAIL: ten-character resident id' >&2; exit 1; }

RECOMMENDATION="$("$CURL" -fsS -b "$COOKIE_FILE" "$BASE_URL/api/zones/recommendation/$RESIDENT_ID")"
ZONE_ID="$(printf '%s' "$RECOMMENDATION" | grep -o '"id":[0-9]*' | head -n 1 | cut -d: -f2)"
[ -n "$ZONE_ID" ] || { echo 'FAIL: zone recommendation' >&2; exit 1; }

ASSIGNMENT="$(json_post /api/zones/assignments "{\"residentId\":\"$RESIDENT_ID\",\"zoneId\":$ZONE_ID}")"
ASSIGNMENT_ID="$(printf '%s' "$ASSIGNMENT" | grep -o '"id":[0-9]*' | head -n 1 | cut -d: -f2)"
[ -n "$ASSIGNMENT_ID" ] || { echo 'FAIL: zone assignment' >&2; exit 1; }

TRANSFORMED="$(json_patch "/api/zones/assignments/$ASSIGNMENT_ID/transformation" '{"percent":100}')"
require_contains "$TRANSFORMED" '"transformationPercent":100' 'transformation completion'

SHEARING="$(json_patch "/api/energy/shearings/$RESIDENT_ID" '{"woolKg":7.2}')"
require_contains "$SHEARING" '"status":"COMPLETED"' 'shearing task'

SHIFT_CODE="SMOKE-$(date +%Y%m%d%H%M%S)-$$"
SHIFT="$(json_post /api/energy/shifts "{\"shiftCode\":\"$SHIFT_CODE\",\"actualKwh\":102.2}")"
require_contains "$SHIFT" '"deliveryStatus":"DELIVERED"' 'accounting delivery'

REPORT="$("$CURL" -fsS -b "$COOKIE_FILE" "$BASE_URL/api/analytics/report")"
require_contains "$REPORT" "$RESIDENT_ID" 'dynamic report'

CSV="$("$CURL" -fsS -b "$COOKIE_FILE" "$BASE_URL/api/analytics/report.csv")"
require_contains "$CSV" 'ID;ФИО;Направление' 'CSV export'

DASHBOARD="$("$CURL" -fsS -b "$COOKIE_FILE" "$BASE_URL/api/analytics/dashboard")"
require_contains "$DASHBOARD" '"referrals":' 'dashboard'

printf '{"status":"PASS","referralId":%s,"residentId":"%s","zoneId":%s,"assignmentId":%s,"shiftCode":"%s"}\n' \
    "$REFERRAL_ID" "$RESIDENT_ID" "$ZONE_ID" "$ASSIGNMENT_ID" "$SHIFT_CODE"
