#!/usr/bin/env bash

# Publish sample AMQP messages to local RabbitMQ (management API) to drive the
# Plan service event listeners.
#
# This script uses the RabbitMQ Management HTTP API to publish messages to the
# default exchange (amq.default) with the routing_key set to the queue name.
# It does not require rabbitmqadmin to be installed.
#
# Queues (must match your Spring properties):
#   - app.amqp.queues.invoice-status-updates (invoice-status-on-related-plans)
#   - app.amqp.queues.plans-to-create (plans-to-create)
#
# Usage examples:
#   # 1) Send a PlansToCreateEvent with generated values
#   bash scripts/publish-amqp-events.sh send_plans_to_create
#
#   # 2) Send a PlansToCreateEvent with explicit values
#   bash scripts/publish-amqp-events.sh send_plans_to_create \
#     --user-id user-123 --invoice-id 11111111-1111-1111-1111-111111111111 \
#     --description "Basic subscription plan" --is-active true --status CREATED \
#     --duration 30 --expires-at 2026-01-31T00:00:00Z
#
#   # 3) Send an InvoiceStatusUpdateEvent (to update status/active/expiry)
#   bash scripts/publish-amqp-events.sh send_invoice_status_update \
#     --user-id user-123 --invoice-id 11111111-1111-1111-1111-111111111111 \
#     --status PAID --is-active true --expires-at 2026-01-31T00:00:00Z
#
# Environment overrides (defaults shown):
#   RABBITMQ_MGMT_HOST=localhost
#   RABBITMQ_MGMT_PORT=15672
#   RABBITMQ_USERNAME=guest
#   RABBITMQ_PASSWORD=guest
#   INVOICE_STATUS_QUEUE=invoice-status-on-related-plans
#   PLANS_TO_CREATE_QUEUE=plans-to-create

set -euo pipefail

RABBITMQ_MGMT_HOST=${RABBITMQ_MGMT_HOST:-localhost}
RABBITMQ_MGMT_PORT=${RABBITMQ_MGMT_PORT:-15672}
RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-guest}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest}

# Keep defaults aligned with src/main/resources/application.properties
INVOICE_STATUS_QUEUE=${INVOICE_STATUS_QUEUE:-${INVOICE_STATUS_ON_RELATED_PLANS_RABBITMQ_QUEUE_NAME:-invoice-status-on-related-plans}}
PLANS_TO_CREATE_QUEUE=${PLANS_TO_CREATE_QUEUE:-${PLANS_TO_CREATE_RABBITMQ_QUEUE_NAME:-plans-to-create}}

color() { local c="$1"; shift; printf "\033[%sm%s\033[0m\n" "$c" "$*"; }
info()  { color 36 "[INFO]  $*"; }
ok()    { color 32 "[OK]    $*"; }
warn()  { color 33 "[WARN]  $*"; }
err()   { color 31 "[ERROR] $*"; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || { err "Missing required command: $1"; exit 1; }
}

gen_uuid() {
  if command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr 'A-Z' 'a-z'
  else
    python3 - <<'PY'
import uuid; print(uuid.uuid4())
PY
  fi
}

base64_payload() {
  # Reads JSON from stdin and outputs a single-line base64 string
  # that RabbitMQ management API will decode.
  if command -v base64 >/dev/null 2>&1; then
    base64 | tr -d '\n'
  else
    python3 - <<'PY'
import sys, base64; print(base64.b64encode(sys.stdin.buffer.read()).decode('ascii'))
PY
  fi
}

publish_to_queue() {
  local queue_name="$1"; shift
  local json_payload="$1"; shift

  # Echo the JSON payload that will be sent (for visibility/debugging)
  info "Preparing to publish JSON payload to queue '${queue_name}':"
  printf '%s\n' "$json_payload"

  local b64
  b64=$(printf '%s' "$json_payload" | base64_payload)

  local body
  body=$(cat <<JSON
{"properties":{},"routing_key":"${queue_name}","payload":"${b64}","payload_encoding":"base64"}
JSON
)

  local url="http://${RABBITMQ_MGMT_HOST}:${RABBITMQ_MGMT_PORT}/api/exchanges/%2f/amq.default/publish"
  local resp
  resp=$(curl -sS -u "${RABBITMQ_USERNAME}:${RABBITMQ_PASSWORD}" \
    -H 'content-type: application/json' \
    -X POST "$url" \
    --data-binary "$body") || { err "HTTP request failed"; exit 1; }

  # Expected JSON: {"routed":true}
  if echo "$resp" | grep -q '"routed":true'; then
    ok "Published to queue '${queue_name}'"
  else
    warn "Publish call did not confirm routing. Response: $resp"
  fi
}

send_plans_to_create() {
  local user_id=""
  local invoice_id=""
  local description="Basic subscription plan"
  local is_active="true"
  local status="CREATED"
  local duration="30"
  local expires_at="$(date -u -v+30d +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -d '+30 days' +%Y-%m-%dT%H:%M:%SZ)"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --user-id) user_id="$2"; shift 2;;
      --invoice-id) invoice_id="$2"; shift 2;;
      --description) description="$2"; shift 2;;
      --is-active) is_active="$2"; shift 2;;
      --status) status="$2"; shift 2;;
      --duration|--duration-in-days) duration="$2"; shift 2;;
      --expires-at) expires_at="$2"; shift 2;;
      *) warn "Unknown arg: $1"; shift;;
    esac
  done

  [[ -n "$user_id" ]] || user_id="user-$(LC_ALL=C tr -dc 'a-z0-9' </dev/urandom | head -c6)"
  [[ -n "$invoice_id" ]] || invoice_id="$(gen_uuid)"

  # Minimal items payload; adjust as needed
  local items='{ "planName": "basic", "seats": 1, "features": ["support"] }'

  local payload
  payload=$(cat <<JSON
{
  "userId": "${user_id}",
  "invoiceId": "${invoice_id}",
  "description": "${description}",
  "isActive": ${is_active},
  "items": ${items},
  "status": "${status}",
  "durationInDays": ${duration},
  "expiresAt": "${expires_at}"
}
JSON
)

  info "Sending PlansToCreateEvent to '${PLANS_TO_CREATE_QUEUE}'"
  info "userId=${user_id} invoiceId=${invoice_id}"
  publish_to_queue "${PLANS_TO_CREATE_QUEUE}" "$payload"
}

send_invoice_status_update() {
  local user_id=""
  local invoice_id=""
  local status="PAID"
  local is_active="true"
  local expires_at=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --user-id) user_id="$2"; shift 2;;
      --invoice-id) invoice_id="$2"; shift 2;;
      --status) status="$2"; shift 2;;
      --is-active) is_active="$2"; shift 2;;
      --expires-at) expires_at="$2"; shift 2;;
      *) warn "Unknown arg: $1"; shift;;
    esac
  done

  if [[ -z "$user_id" || -z "$invoice_id" ]]; then
    err "--user-id and --invoice-id are required for status update"; exit 1
  fi

  local payload
  if [[ -n "$expires_at" ]]; then
    payload=$(cat <<JSON
{
  "userId": "${user_id}",
  "invoiceId": "${invoice_id}",
  "status": "${status}",
  "isActive": ${is_active},
  "expiresAt": "${expires_at}"
}
JSON
)
  else
    payload=$(cat <<JSON
{
  "userId": "${user_id}",
  "invoiceId": "${invoice_id}",
  "status": "${status}",
  "isActive": ${is_active}
}
JSON
)
  fi

  info "Sending InvoiceStatusUpdateEvent to '${INVOICE_STATUS_QUEUE}'"
  info "userId=${user_id} invoiceId=${invoice_id} status=${status}"
  publish_to_queue "${INVOICE_STATUS_QUEUE}" "$payload"
}

print_help() {
  cat <<USAGE
Usage: $0 <command> [options]

Commands:
  send_plans_to_create [--user-id ID] [--invoice-id UUID] [--description TXT] [--is-active true|false] \
                       [--status STR] [--duration N] [--expires-at ISO8601]

  send_invoice_status_update --user-id ID --invoice-id UUID [--status STR] [--is-active true|false] [--expires-at ISO8601]

Environment overrides:
  RABBITMQ_MGMT_HOST (default: ${RABBITMQ_MGMT_HOST})
  RABBITMQ_MGMT_PORT (default: ${RABBITMQ_MGMT_PORT})
  RABBITMQ_USERNAME  (default: ${RABBITMQ_USERNAME})
  RABBITMQ_PASSWORD  (default: ${RABBITMQ_PASSWORD})
  INVOICE_STATUS_QUEUE (default: ${INVOICE_STATUS_QUEUE})
  PLANS_TO_CREATE_QUEUE (default: ${PLANS_TO_CREATE_QUEUE})

Examples:
  $0 send_plans_to_create
  $0 send_invoice_status_update --user-id user-123 --invoice-id 11111111-1111-1111-1111-111111111111 --status PAID
USAGE
}

main() {
  require_cmd curl
  # base64 is optional (we fallback to python3). If neither exists, we fail when used.
  if ! command -v base64 >/dev/null 2>&1 && ! command -v python3 >/dev/null 2>&1; then
    err "Either 'base64' or 'python3' is required to encode payloads"; exit 1
  fi

  local cmd="${1:-}"; shift || true
  case "$cmd" in
    send_plans_to_create) send_plans_to_create "$@" ;;
    send_invoice_status_update) send_invoice_status_update "$@" ;;
    -h|--help|help|"") print_help ;;
    *) err "Unknown command: $cmd"; print_help; exit 1 ;;
  esac
}

main "$@"
