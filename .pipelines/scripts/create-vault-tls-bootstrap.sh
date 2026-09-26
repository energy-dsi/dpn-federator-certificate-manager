#!/bin/sh
# =============================================================================
# Bootstrap Vault TLS + Truststore — DPN Platform
#
# Produces in one run:
#   <OUT_DIR>/ca/rootCA.key       — Root CA private key   (keep safe / discard)
#   <OUT_DIR>/ca/rootCA.crt       — Root CA cert          (CN=dpn-vault-root)
#   <OUT_DIR>/certs/vault.key     — Vault server key      (-> AKV VAULT-TLS-KEY)
#   <OUT_DIR>/certs/vault.csr     — Vault CSR
#   <OUT_DIR>/certs/vault.crt     — Vault server cert     (-> AKV VAULT-TLS-CERT)
#   <OUT_DIR>/truststore.jks      — PKCS12 truststore     (-> cert-manager-truststore)
#
# CSR format:
#   [ req ]
#   default_bits = 4096 / sha256 / no prompt
#   distinguished_name: C, O, CN=vault  (OU always empty)
#
#   [ alt_names ]
#   DNS.1 = vault-https.ns-dpn-01.svc.cluster.local  (fixed — in-cluster Vault service name)
#
# Customer inputs:
#   COUNTRY         — subject Country (C)         (default GB)
#   ORG             — subject Organisation (O)    (default DSI)
#   TRUSTSTORE_PASS — truststore password         (required)
# Optional plumbing:
#   OUT_DIR         — output base directory       (default vault)
# =============================================================================
set -eu

# ---- Fixed identity ---------------------------------------------------------
LEAF_CN="vault"
ROOT_CN="dpn-vault-root"
KEY_SIZE="4096"
ROOT_DAYS="3650"
LEAF_DAYS="825"
TRUST_ALIAS="ca"

# ---- Customer inputs --------------------------------------------------------
COUNTRY="${COUNTRY:-GB}"
ORG="${ORG:-DSI}"

# ---- Fixed SAN — in-cluster Vault service name ------------------------------
SANS="vault-https.ns-dpn-01.svc.cluster.local"

# ---- Validate ---------------------------------------------------------------
: "${TRUSTSTORE_PASS:?TRUSTSTORE_PASS must be set}"

# ---- Plumbing ---------------------------------------------------------------
OUT_DIR="${OUT_DIR:-vault}"
CA_DIR="$OUT_DIR/ca"
CERTS_DIR="$OUT_DIR/certs"
CFG="$CERTS_DIR/vault-openssl.cnf"
TRUSTSTORE="$OUT_DIR/truststore.jks"

ROOT_SUBJ="/C=$COUNTRY/O=$ORG/CN=$ROOT_CN"

echo "=========================================="
echo " Vault TLS bootstrap"
echo "   leaf CN      : $LEAF_CN       (fixed)"
echo "   Root CA CN   : $ROOT_CN       (fixed)"
echo "   key size     : ${KEY_SIZE}-bit RSA / SHA-256 (fixed)"
echo "   O (org)      : $ORG"
echo "   C (country)  : $COUNTRY"
echo "   SANs         : $SANS"
echo "=========================================="

# ---- Step 1: directory layout ----------------------------------------------
mkdir -p "$CA_DIR" "$CERTS_DIR"

# ---- Step 2: Root CA private key -------------------------------------------
echo "[1/7] Generating Root CA private key (${KEY_SIZE}-bit)..."
openssl genrsa -out "$CA_DIR/rootCA.key" "$KEY_SIZE"

# ---- Step 3: Root CA self-signed certificate --------------------------------
echo "[2/7] Generating Root CA certificate ($ROOT_CN)..."
openssl req -x509 -new -nodes -key "$CA_DIR/rootCA.key" \
  -sha256 -days "$ROOT_DAYS" \
  -out "$CA_DIR/rootCA.crt" \
  -subj "$ROOT_SUBJ"

# ---- Step 4: Vault private key ---------------------------------------------
echo "[3/7] Generating Vault private key (${KEY_SIZE}-bit)..."
openssl genrsa -out "$CERTS_DIR/vault.key" "$KEY_SIZE"

# ---- Step 5: OpenSSL config ------------------------------------------------
echo "[4/7] Writing OpenSSL config..."
cat > "$CFG" <<EOF
[ req ]
default_bits       = $KEY_SIZE
prompt             = no
default_md         = sha256
req_extensions     = req_ext
distinguished_name = dn

[ dn ]
C  = $COUNTRY
O  = $ORG
CN = $LEAF_CN

[ req_ext ]
subjectAltName = @alt_names

[ alt_names ]
EOF

# Split SANS by comma and write DNS.1, DNS.2, DNS.3...
i=1
OLD_IFS="$IFS"
IFS=','
for san in $SANS; do
  san=$(printf '%s' "$san" | tr -d ' ')
  if [ -n "$san" ]; then
    echo "DNS.$i = $san" >> "$CFG"
    i=$((i + 1))
  fi
done
IFS="$OLD_IFS"

echo "Generated SANs in config:"
grep "^DNS\." "$CFG"

# ---- Step 6: CSR for Vault -------------------------------------------------
echo "[5/7] Generating Vault CSR..."
openssl req -new \
  -key "$CERTS_DIR/vault.key" \
  -out "$CERTS_DIR/vault.csr" \
  -config "$CFG"

# ---- Step 7: sign CSR with Root CA -> vault.crt ----------------------------
echo "[6/7] Signing Vault CSR with Root CA (validity ${LEAF_DAYS} days)..."
openssl x509 -req \
  -in "$CERTS_DIR/vault.csr" \
  -CA "$CA_DIR/rootCA.crt" \
  -CAkey "$CA_DIR/rootCA.key" \
  -CAcreateserial \
  -out "$CERTS_DIR/vault.crt" \
  -days "$LEAF_DAYS" \
  -sha256 \
  -extensions req_ext \
  -extfile "$CFG"

# ---- Step 8: truststore from Root CA (trust anchor) ------------------------
echo "[7/7] Building PKCS12 truststore (imports Root CA only)..."
[ -f "$TRUSTSTORE" ] && rm -f "$TRUSTSTORE"
keytool -import -trustcacerts -noprompt \
  -alias "$TRUST_ALIAS" \
  -file "$CA_DIR/rootCA.crt" \
  -keystore "$TRUSTSTORE" \
  -storetype PKCS12 \
  -storepass "$TRUSTSTORE_PASS"

echo ""
echo "=========================================="
echo " DONE — artifacts written under $OUT_DIR/"
echo "   $CA_DIR/rootCA.crt   (root CA — trust anchor)"
echo "   $CERTS_DIR/vault.crt (Vault server cert -> AKV VAULT-TLS-CERT)"
echo "   $CERTS_DIR/vault.key (Vault server key  -> AKV VAULT-TLS-KEY)"
echo "   $TRUSTSTORE          (certificate-manager truststore)"
echo "=========================================="
echo "--- vault.crt summary ---"
openssl x509 -in "$CERTS_DIR/vault.crt" -noout -subject -issuer -dates
openssl x509 -in "$CERTS_DIR/vault.crt" -noout -text | grep -A6 "Subject Alternative Name" || true
echo "--- truststore ---"
keytool -list -keystore "$TRUSTSTORE" -storetype PKCS12 -storepass "$TRUSTSTORE_PASS"
