#!/usr/bin/env bash
# Fail fast on errors, undefined vars, and failed pipes
set -euo pipefail
# Load environment variables from the project's .env file so that Stripe CLI
# and any downstream processes can use them. Resolve the project root
# relative to this script's location so no absolute paths are needed.
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
  echo "[init-stripe-listen] Loading environment from: $ENV_FILE"
  # Export all variables defined in the .env file
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
else
  echo "[init-stripe-listen] Warning: .env file not found. Looked for: ${repo_root}/.env and up the directory tree from $script_dir" >&2
fi
# Normalize Stripe env for the Stripe CLI
# If STRIPE_API_KEY is not set but STRIPE_SECRET_KEY exists, map it.
if [ "${STRIPE_API_KEY:-}" = "" ] && [ "${STRIPE_SECRET_KEY:-}" != "" ]; then
  export STRIPE_API_KEY="$STRIPE_SECRET_KEY"
fi
# For completeness, some tooling uses STRIPE_WEBHOOK_SECRET env name.
if [ "${STRIPE_WEBHOOK_SECRET:-}" = "" ] && [ "${STRIPE_ENDPOINT_SECRET:-}" != "" ]; then
  export STRIPE_WEBHOOK_SECRET="$STRIPE_ENDPOINT_SECRET"
fi
# Determine forward URL (respect SERVER_PORT_LISTENING if present)
# Note: The application maps the Stripe webhook at /api/v1/invoice/stripe_webhooks
# so we forward to that exact path to avoid 403/404 issues.
forward_port="${SERVER_PORT_LISTENING:-3120}"
forward_url="http://localhost:${forward_port}/api/v1/invoice/stripe_webhooks"
# Start Stripe CLI listener with explicit API key if available to avoid using cached, possibly expired auth
if [ -n "${STRIPE_API_KEY:-}" ]; then
  echo "[init-stripe-listen] Starting Stripe listener forwarding to ${forward_url} using STRIPE_API_KEY from environment"
  exec stripe listen --api-key "$STRIPE_API_KEY" --forward-to "$forward_url"
else
  echo "[init-stripe-listen] Notice: STRIPE_API_KEY not found in environment. Stripe CLI will use its local login/session (may be expired)." >&2
  exec stripe listen --forward-to "$forward_url"
fi
