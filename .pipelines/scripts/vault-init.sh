#!/bin/bash
# =============================================================================
# Vault Init Script — DPN Platform
#
# Runs after first vault-https deployment to:
#   1. Initialize Vault (if not already initialized)
#   2. Store root token in Azure Key Vault as VAULT-TOKEN
#   3. Enable KV v2 engine at pki-client
#   4. Enable audit logging to stdout
#
# Idempotent — safe to re-run on existing vaults.
# On re-runs: reads VAULT-TOKEN from AKV to enable KV v2 / audit if not yet done.
#
# NOTE: vault-https uses AKV auto-unseal — no manual unseal needed.
#       vault operator init produces Recovery Keys (not Unseal Keys).
#
# Env vars:
#   NAMESPACE          — Kubernetes namespace (default: ns-dpn-01)
#   KV_MOUNT           — Vault KV mount path (default: pki-client)
#   INIT_FILE          — Path to save init output (default: vault-init.txt)
#   STORE_ROOT_TOKEN   — "true" to push root token to AKV as VAULT-TOKEN (default: false)
#   AKV_NAME           — Azure Key Vault name (required when STORE_ROOT_TOKEN=true)
#   VAULT_POD_LABEL    — Pod label selector (default: app=dpn-vault-https)
# =============================================================================

set -euo pipefail

NAMESPACE="${NAMESPACE:-ns-dpn-01}"
KV_MOUNT="${KV_MOUNT:-pki-client}"
INIT_FILE="${INIT_FILE:-vault-init.txt}"
STORE_ROOT_TOKEN="${STORE_ROOT_TOKEN:-false}"
STORE_ROOT_TOKEN="${STORE_ROOT_TOKEN,,}"   # normalise to lowercase — ADO passes "True" not "true"
VAULT_POD_LABEL="${VAULT_POD_LABEL:-app=dpn-vault-https}"
SECRET_NAME="${SECRET_NAME:-VAULT-TOKEN}"

if [[ "$STORE_ROOT_TOKEN" == "true" ]] && [[ -z "${AKV_NAME:-}" ]]; then
  echo "##[error]AKV_NAME is required when STORE_ROOT_TOKEN=true."
  exit 1
fi

##############################################
# Helper — exec into vault pod
##############################################

VAULT_POD=$(kubectl get pod -n "$NAMESPACE" -l "$VAULT_POD_LABEL" \
  -o jsonpath='{.items[0].metadata.name}')

if [[ -z "$VAULT_POD" ]]; then
  echo "##[error]No vault pod found in namespace $NAMESPACE with label $VAULT_POD_LABEL"
  exit 1
fi

echo "Using pod: $VAULT_POD"

vault_exec() {
  kubectl exec -n "$NAMESPACE" "$VAULT_POD" -- sh -c "$1"
}

##############################################
# Step 1: Wait for Vault to be ready
##############################################

echo "Waiting for Vault to become ready..."
VAULT_READY=false
for i in $(seq 1 12); do
  VAULT_STATUS=$(vault_exec "VAULT_ADDR=https://127.0.0.1:8200 VAULT_SKIP_VERIFY=true vault status" 2>&1 || true)
  if echo "$VAULT_STATUS" | grep -q "Initialized"; then
    echo "Vault is up."
    VAULT_READY=true
    break
  fi
  echo "Attempt $i/12 — waiting 10s..."
  sleep 10
done

if [[ "$VAULT_READY" != "true" ]]; then
  echo "##[error]Vault did not become ready after 120s. Check pod logs: kubectl logs -n $NAMESPACE $VAULT_POD"
  exit 1
fi

##############################################
# Step 2: Check initialization status
##############################################

echo "Checking Vault initialization status..."

STATUS=$(vault_exec "VAULT_ADDR=https://127.0.0.1:8200 VAULT_SKIP_VERIFY=true vault status" 2>&1 || true)
INITIALIZED=$(echo "$STATUS" | grep "Initialized" | awk '{print $2}')

##############################################
# Step 3: Initialize if needed
# AKV auto-unseal: produces Recovery Keys, not Unseal Keys.
# Vault unseals itself — no vault operator unseal required.
##############################################

ROOT_TOKEN=""
TOKEN_STORED="skipped"   # skipped | stored | existing

if [[ "$INITIALIZED" != "true" ]]; then
  echo "Vault is not initialized. Initializing..."

  vault_exec "VAULT_ADDR=https://127.0.0.1:8200 VAULT_SKIP_VERIFY=true vault operator init" > "$INIT_FILE"

  echo "Initialization complete. Output saved to $INIT_FILE"

  echo "Waiting for AKV auto-unseal to complete..."
  UNSEALED=false
  for i in $(seq 1 12); do
    UNSEAL_STATUS=$(vault_exec "VAULT_ADDR=https://127.0.0.1:8200 VAULT_SKIP_VERIFY=true vault status" 2>&1 || true)
    SEALED=$(echo "$UNSEAL_STATUS" | grep "^Sealed" | awk '{print $2}')
    if [[ "$SEALED" == "false" ]]; then
      echo "Vault is unsealed."
      UNSEALED=true
      break
    fi
    echo "Attempt $i/12 — still sealed, waiting 10s..."
    sleep 10
  done

  if [[ "$UNSEALED" != "true" ]]; then
    echo "##[error]Vault did not unseal after 120s. Check AKV seal configuration and managed identity permissions."
    exit 1
  fi

  ROOT_TOKEN=$(grep 'Initial Root Token' "$INIT_FILE" | awk '{print $NF}')

else
  echo "Vault already initialized. Skipping init."

  # Try to read token from AKV so KV v2 / audit setup can still run on re-deploys
  if [[ -n "${AKV_NAME:-}" ]]; then
    echo "Reading $SECRET_NAME from AKV for KV v2 / audit setup..."
    ROOT_TOKEN=$(az keyvault secret show \
      --vault-name "$AKV_NAME" \
      --name "$SECRET_NAME" \
      --query "value" -o tsv 2>/dev/null || true)
    if [[ -n "$ROOT_TOKEN" ]]; then
      echo "Token retrieved from AKV."
    else
      echo "##[warning]$SECRET_NAME not found in AKV. KV v2 and audit setup will be skipped."
    fi
  fi
fi

if [[ -z "$ROOT_TOKEN" ]]; then
  echo "##[warning]No root token available — skipping KV v2 and audit setup."
fi

##############################################
# Step 4: Store root token in AKV as VAULT-TOKEN (optional, first init only)
##############################################

if [[ "$STORE_ROOT_TOKEN" == "true" ]] && [[ -f "$INIT_FILE" ]]; then
  echo "Storing root token in Azure Key Vault: $AKV_NAME"

  # Policy requires an expiry — set to exactly 1 year from now
  SECRET_EXPIRY=$(date -u -d "+365 days" '+%Y-%m-%dT%H:%M:%SZ')

  az keyvault secret set \
    --vault-name "$AKV_NAME" \
    --name "$SECRET_NAME" \
    --value "$ROOT_TOKEN" \
    --expires "$SECRET_EXPIRY" \
    --output none

  echo "Root token stored as secret '$SECRET_NAME' in AKV: $AKV_NAME (expires: $SECRET_EXPIRY)"
  TOKEN_STORED="stored"
elif [[ "$STORE_ROOT_TOKEN" == "true" ]] && [[ ! -f "$INIT_FILE" ]]; then
  echo "Vault already initialized — token already in AKV as '$SECRET_NAME'."
  TOKEN_STORED="existing"
else
  echo "Skipping AKV root token storage (storeRootTokenInAkv=false)."
  # Still check if token already exists in AKV so the summary is accurate
  if [[ -n "${AKV_NAME:-}" ]]; then
    EXISTING=$(az keyvault secret show \
      --vault-name "$AKV_NAME" \
      --name "$SECRET_NAME" \
      --query "value" -o tsv 2>/dev/null || true)
    if [[ -n "$EXISTING" ]]; then
      TOKEN_STORED="existing"
    else
      TOKEN_STORED="skipped"
    fi
  else
    TOKEN_STORED="skipped"
  fi
fi

##############################################
# Step 5: Enable KV v2 engine
##############################################

if [[ -n "$ROOT_TOKEN" ]]; then
  echo "Enabling KV v2 engine at path: $KV_MOUNT"

  vault_exec "
  export VAULT_ADDR=https://127.0.0.1:8200
  export VAULT_SKIP_VERIFY=true
  export VAULT_TOKEN=$ROOT_TOKEN
  vault secrets enable -path=$KV_MOUNT kv-v2 2>/dev/null && echo 'KV v2 enabled' || echo 'KV v2 already enabled'
  "
else
  echo "##[warning]Skipping KV v2 setup — no token available."
fi

##############################################
# Step 6: Enable audit logs
##############################################

if [[ -n "$ROOT_TOKEN" ]]; then
  echo "Enabling audit logging to stdout..."

  vault_exec "
  export VAULT_ADDR=https://127.0.0.1:8200
  export VAULT_SKIP_VERIFY=true
  export VAULT_TOKEN=$ROOT_TOKEN
  vault audit enable file file_path=stdout 2>/dev/null && echo 'Audit logging enabled' || echo 'Audit logging already enabled'
  "
else
  echo "##[warning]Skipping audit setup — no token available."
fi

##############################################
# Step 7: Summary
##############################################

echo ""
echo "=========================================="
echo " VAULT INIT COMPLETE"
echo " Namespace : $NAMESPACE"
echo " Pod       : $VAULT_POD"
echo " KV mount  : $KV_MOUNT"
if [[ "$TOKEN_STORED" == "stored" ]]; then
  echo " Root token: stored in AKV '$AKV_NAME' as secret '$SECRET_NAME'"
elif [[ "$TOKEN_STORED" == "existing" ]]; then
  echo " Root token: already in AKV '$AKV_NAME' as secret '$SECRET_NAME' (vault was pre-initialized)"
else
  echo " Root token: not stored in AKV (storeRootTokenInAkv=false)"
fi
if [[ -f "$INIT_FILE" ]]; then
  echo " Init file : $INIT_FILE (also published as pipeline artifact)"
fi
echo "=========================================="
