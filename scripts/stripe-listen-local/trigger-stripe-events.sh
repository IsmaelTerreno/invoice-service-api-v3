#!/usr/bin/env bash

# A small interactive menu to trigger common Stripe events using the Stripe CLI.
# It loads environment variables from the project's .env (like init-stripe-listen.sh)
# and, when available, passes --api-key to avoid relying on a cached/expired CLI session.

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
ENV_FILE="${repo_root}/.env"

find_env_file() {
  local start_dir="$1"
  local cur="$start_dir"
  while :; do
    if [ -f "$cur/.env" ]; then
      echo "$cur/.env"
      return 0
    fi
    if [ "$cur" = "/" ]; then
      return 1
    fi
    cur="$(dirname "$cur")"
  done
}

# If the expected .env isn't present, search upwards from script_dir.
if [ ! -f "$ENV_FILE" ]; then
  if found_env=$(find_env_file "$script_dir"); then
    ENV_FILE="$found_env"
  fi
fi

if [ -f "$ENV_FILE" ]; then
  echo "[trigger-stripe-events] Loading environment from: $ENV_FILE"
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
else
  echo "[trigger-stripe-events] Warning: .env file not found. Looked for: ${repo_root}/.env and up the directory tree from $script_dir" >&2
fi

# Normalize Stripe env for the Stripe CLI
if [ "${STRIPE_API_KEY:-}" = "" ] && [ "${STRIPE_SECRET_KEY:-}" != "" ]; then
  export STRIPE_API_KEY="$STRIPE_SECRET_KEY"
fi
if [ "${STRIPE_WEBHOOK_SECRET:-}" = "" ] && [ "${STRIPE_ENDPOINT_SECRET:-}" != "" ]; then
  export STRIPE_WEBHOOK_SECRET="$STRIPE_ENDPOINT_SECRET"
fi

mask_key() {
  local key="$1"
  if [ -z "$key" ]; then
    echo "<none>"
    return
  fi
  # Keep prefix and last 4 chars, mask the rest
  local prefix=${key%%_*}_
  local last4=${key: -4}
  echo "${prefix}***${last4}"
}

run_trigger() {
  local event_name="$1"
  if [ -n "${STRIPE_API_KEY:-}" ]; then
    echo "[trigger] Running: stripe trigger $event_name (api-key=$(mask_key "$STRIPE_API_KEY"))"
    stripe trigger "$event_name" --api-key "$STRIPE_API_KEY"
  else
    echo "[trigger] Notice: STRIPE_API_KEY not set. Using Stripe CLI's local login/session."
    stripe trigger "$event_name"
  fi
}

echo ""
echo "Stripe Trigger Menu"
echo "--------------------"
echo "This tool will trigger common Stripe events via 'stripe trigger'."
echo "Ensure your local listener is running: ./scripts/stripe-listen-local/init-stripe-listen.sh"
echo ""

PS3=$'Select an event to trigger (or choose Exit): '

options=(
  "payment_intent.succeeded"
  "payment_intent.payment_failed"
  "charge.succeeded"
  "charge.failed"
  "invoice.paid"
  "invoice.payment_failed"
  "checkout.session.completed"
  "checkout.session.async_payment_succeeded"
  "customer.created"
  "customer.subscription.created"
  "customer.subscription.deleted"
  "Custom event (manual input)"
  "Exit"
)

select opt in "${options[@]}"; do
  case "$REPLY" in
    1) run_trigger "payment_intent.succeeded" ;;
    2) run_trigger "payment_intent.payment_failed" ;;
    3) run_trigger "charge.succeeded" ;;
    4) run_trigger "charge.failed" ;;
    5) run_trigger "invoice.paid" ;;
    6) run_trigger "invoice.payment_failed" ;;
    7) run_trigger "checkout.session.completed" ;;
    8) run_trigger "checkout.session.async_payment_succeeded" ;;
    9) run_trigger "customer.created" ;;
    10) run_trigger "customer.subscription.created" ;;
    11) run_trigger "customer.subscription.deleted" ;;
    12)
      read -r -p "Enter an event name (e.g., payment_intent.created): " custom_event
      if [ -n "$custom_event" ]; then
        run_trigger "$custom_event"
      else
        echo "[trigger] No event entered."
      fi
      ;;
    13)
      echo "Bye!"
      break
      ;;
    *)
      echo "Invalid option. Try again."
      ;;
  esac
done
