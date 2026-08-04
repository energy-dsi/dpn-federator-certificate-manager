#!/bin/sh
# =============================================================================
# Load Bootstrap Bundle into Vault — DPN Platform
#
# Loads a pre-issued bootstrap certificate bundle (private key, signed
# certificate, CA chain) into the cert-manager KV paths in Vault, so the
# certificate-manager can build its initial keystore/truststore and self-enrol
# (bootstrap cert carries BOOTSTRAP_OID -> immediate renewal on first run).
#
# Runs INSIDE the dpn-vault-https pod (vault CLI + openssl are available there).
# The pipeline kubectl-cp's this script + the 3 PEM files into the pod, then
# execs it.
#
# Required env:
#   VAULT_TOKEN  — Vault root/privileged token (sourced from Key Vault by the pipeline)
#
# Optional env (defaults match the pipeline's kubectl cp targets):
#   KEY_FILE     — private key PEM path     (default /tmp/bootstrap.key)
#   CERT_FILE    — signed certificate PEM   (default /tmp/certificate.pem)
#   CHAIN_FILE   — CA chain PEM             (default /tmp/ca-chain.pem)
#   KV_MOUNT     — KV v2 mount path         (default pki-client)
#   KV_BASE      — base secret path         (default node-net/client)
#   VAULT_ADDR   — Vault address            (default https://127.0.0.1:8200)
# =============================================================================
set -eu

: "${VAULT_TOKEN:?VAULT_TOKEN must be set (sourced from Key Vault by the pipeline)}"

KEY_FILE="${KEY_FILE:-/tmp/bootstrap.key}"
CERT_FILE="${CERT_FILE:-/tmp/certificate.pem}"
CHAIN_FILE="${CHAIN_FILE:-/tmp/ca-chain.pem}"
KV_MOUNT="${KV_MOUNT:-pki-client}"
KV_BASE="${KV_BASE:-node-net/client}"

export VAULT_ADDR="${VAULT_ADDR:-https://127.0.0.1:8200}"
export VAULT_SKIP_VERIFY="${VAULT_SKIP_VERIFY:-true}"
export VAULT_TOKEN

# ---- Pre-flight ------------------------------------------------------------
for f in "$KEY_FILE" "$CERT_FILE" "$CHAIN_FILE"; do
  if [ ! -s "$f" ]; then
    echo "ERROR: required bundle file missing or empty: $f" >&2
    exit 1
  fi
done

# Fail fast if Vault is sealed / unreachable
if ! vault status >/dev/null 2>&1; then
  echo "ERROR: Vault is not reachable/unsealed at $VAULT_ADDR. Ensure vault-https is deployed and unsealed." >&2
  vault status || true
  exit 1
fi

# ---- Load ------------------------------------------------------------------
echo "Loading bootstrap bundle into ${KV_MOUNT}/${KV_BASE}/* ..."

vault kv put "${KV_MOUNT}/${KV_BASE}/keypair" \
  privateKey="$(cat "$KEY_FILE")" \
  publicKey="$(openssl rsa -in "$KEY_FILE" -pubout 2>/dev/null)"

vault kv put "${KV_MOUNT}/${KV_BASE}/certificate" \
  certificate="$(cat "$CERT_FILE")"

vault kv put "${KV_MOUNT}/${KV_BASE}/ca-chain" \
  chain="$(cat "$CHAIN_FILE")"

echo "Bootstrap bundle loaded successfully:"
echo "  - ${KV_MOUNT}/${KV_BASE}/keypair      (privateKey + derived publicKey)"
echo "  - ${KV_MOUNT}/${KV_BASE}/certificate"
echo "  - ${KV_MOUNT}/${KV_BASE}/ca-chain"
