#!/usr/bin/env bash
#
# Configures HashiCorp Vault AppRole authentication for the certificate-manager component
# against the DPN vault.
#
# Prerequisites:
#   - VAULT_ADDR and VAULT_TOKEN (a token with sufficient privileges to manage auth
#     methods, policies and AppRole roles) are exported in the environment.
#   - The KV v2 secrets engine referenced by certificate-manager-policy.hcl already exists,
#     e.g.  vault secrets enable -path=pki-client -version=2 kv
#
# Usage:
#   ./setup-approle.sh
#
set -euo pipefail

ROLE_NAME="certificate-manager"
POLICY_NAME="certificate-manager-policy"
POLICY_FILE="$(dirname "$0")/certificate-manager-policy.hcl"

echo "Enabling AppRole auth method (no-op if already enabled)..."
vault auth enable approle 2>/dev/null || echo "  approle auth method already enabled"

echo "Writing policy '${POLICY_NAME}'..."
vault policy write "${POLICY_NAME}" "${POLICY_FILE}"

echo "Creating/updating AppRole role '${ROLE_NAME}'..."
vault write "auth/approle/role/${ROLE_NAME}" \
    token_policies="${POLICY_NAME}" \
    token_ttl=1h \
    token_max_ttl=4h \
    secret_id_ttl=0 \
    secret_id_num_uses=0

echo
echo "role_id (set as spring.cloud.vault.app-role.role-id / VAULT_APPROLE_ROLE_ID, not sensitive):"
vault read -field=role_id "auth/approle/role/${ROLE_NAME}/role-id"

echo
echo "secret_id (set as spring.cloud.vault.app-role.secret-id / VAULT_APPROLE_SECRET_ID, TREAT AS SENSITIVE):"
vault write -field=secret_id -f "auth/approle/role/${ROLE_NAME}/secret-id"

cat <<'EOF'

To switch the certificate manager to AppRole, set:

  VAULT_AUTHENTICATION=approle
  VAULT_APPROLE_ROLE_ID=<role_id from above>
  VAULT_APPROLE_SECRET_ID=<secret_id from above>
  VAULT_APPROLE_ROLE=certificate-manager           # optional, this is the default
  VAULT_APPROLE_MOUNT_PATH=approle                 # optional, this is the default

spring.cloud.vault.authentication defaults to "token" if VAULT_AUTHENTICATION is unset,
preserving existing VAULT_TOKEN-based deployments.
EOF
