#!/bin/sh
# =============================================================================
# DPN Universal TLS material generator — DPN Platform
#
# ONE self-signed DPN Root CA (CN=dpn-root) and ONE common server certificate
# (CN=dpn-common, WILDCARD SANs) that serves EVERY in-cluster TLS endpoint in the
# two DPN namespaces. The same key+cert is emitted in the format each consumer
# reads — only the file format/extension differs:
#
#   PEM   (certs/server.key + certs/server.crt)  -> Vault HTTPS server
#                                                   (AKV VAULT-TLS-KEY / VAULT-TLS-CERT)
#   PKCS12 keystore.jks (alias dpn-server)        -> Keycloak HTTPS + Kafka broker TLS
#                                                   (k8s secret dpn-tls)
#   PKCS12 truststore.jks (dpn-root + client CA)  -> cert-manager (trust Vault),
#                                                   Keycloak, Kafka, federator
#                                                   (k8s secrets cert-manager-truststore + dpn-tls)
#   PKCS12 dsm-client.p12 (CN=dsm-client)         -> external mTLS / browser client
#
# Wildcard SANs cover every single-label service under the two DPN namespaces,
# e.g. vault-https / dpn-keycloak / dpn-kafka-src / dpn-kafka-target .ns-dpn-01…:
#   DNS: *.ns-dpn-01.svc.cluster.local
#   DNS: *.ns-dpn-health-01.svc.cluster.local
#
# The key alias is dpn-server — the Keycloak chart MUST set
# KC_HTTPS_KEY_ALIAS=dpn-server to match.
#
# This is the UNIFIED generator. The segregated create-vault-tls-bootstrap.sh and
# create-keycloak-tls.sh are kept for fallback.
#
# Required env:
#   KEYSTORE_PASS    — keystore.jks password (= key password; PKCS12 uses one)
#   TRUSTSTORE_PASS  — truststore.jks password
#   CLIENT_P12_PASS  — dsm-client.p12 password
# Customer inputs (the only tunable subject fields):
#   COUNTRY          — subject Country (C)      (default GB)
#   ORG              — subject Organisation (O) (default DSI)
# Optional plumbing:
#   OUT_DIR          — output base directory    (default dpn-tls)
# =============================================================================
set -eu

# ---- Fixed identity ---------------------------------------------------------
ROOT_CN="dpn-root"
SERVER_CN="dpn-common"
CLIENT_CA_CN="dpn-client-ca"
CLIENT_CN="dsm-client"
VAULT_CN="vault"          # subject CN of the DSM-signed Vault bootstrap CSR
KEY_SIZE="4096"
ROOT_DAYS="3650"
SERVER_DAYS="825"
CLIENT_CA_DAYS="3650"
CLIENT_DAYS="365"
SERVER_ALIAS="dpn-server"

# ---- Fixed WILDCARD SANs — the "common DPN cert" service coverage ----------
SAN_DNS_1="*.ns-dpn-01.svc.cluster.local"
SAN_DNS_2="*.ns-dpn-health-01.svc.cluster.local"

# ---- Customer inputs — C and O only ----------------------------------------
COUNTRY="${COUNTRY:-GB}"
ORG="${ORG:-DSI}"

# ---- Required passwords -----------------------------------------------------
: "${KEYSTORE_PASS:?KEYSTORE_PASS must be set}"
: "${TRUSTSTORE_PASS:?TRUSTSTORE_PASS must be set}"
: "${CLIENT_P12_PASS:?CLIENT_P12_PASS must be set}"

# ---- Plumbing ---------------------------------------------------------------
OUT_DIR="${OUT_DIR:-dpn-tls}"
CA_DIR="$OUT_DIR/ca"
CERTS_DIR="$OUT_DIR/certs"
SRV_CFG="$CERTS_DIR/server-openssl.cnf"
CLI_CFG="$CERTS_DIR/client-openssl.cnf"
VAULT_CFG="$CERTS_DIR/vault-openssl.cnf"
KEYSTORE="$OUT_DIR/keystore.jks"
TRUSTSTORE="$OUT_DIR/truststore.jks"
CLIENT_P12="$OUT_DIR/dsm-client.p12"
SERVER_P12="$CERTS_DIR/server.p12"

SUBJ_BASE="/C=$COUNTRY/O=$ORG"

echo "=========================================="
echo " DPN universal TLS generator (self-signed)"
echo "   Root CA CN   : $ROOT_CN        (fixed)"
echo "   Server CN    : $SERVER_CN      (fixed)"
echo "   Client CA CN : $CLIENT_CA_CN   (fixed)"
echo "   Client CN    : $CLIENT_CN      (fixed)"
echo "   key strength : ${KEY_SIZE}-bit RSA / SHA-256 (fixed)"
echo "   SANs         : $SAN_DNS_1, $SAN_DNS_2 (wildcard, fixed)"
echo "   O (org)      : $ORG"
echo "   C (country)  : $COUNTRY"
echo "=========================================="

mkdir -p "$CA_DIR" "$CERTS_DIR"

# ---- openssl configs (server SAN + EKUs) -----------------------------------
cat > "$SRV_CFG" <<EOF
[ req ]
default_bits       = $KEY_SIZE
prompt             = no
default_md         = sha256
distinguished_name = dn
req_extensions     = v3_server

[ dn ]
C  = $COUNTRY
O  = $ORG
CN = $SERVER_CN

[ v3_server ]
basicConstraints = CA:FALSE
keyUsage         = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName   = @alt_names

[ alt_names ]
DNS.1 = $SAN_DNS_1
DNS.2 = $SAN_DNS_2
EOF

cat > "$CLI_CFG" <<EOF
[ v3_client ]
basicConstraints = CA:FALSE
keyUsage         = critical, digitalSignature, keyEncipherment
extendedKeyUsage = clientAuth
EOF

# ---- 1/6: DPN Root CA (self-signed) ----------------------------------------
# -addext keyUsage is required: a bare `openssl req -x509` only auto-adds
# basicConstraints=CA:TRUE, not keyUsage. Modern OpenSSL (3.x) and Python's
# ssl module reject a CA cert with no keyUsage extension at all during chain
# verification ("CA cert does not include key usage extension"), which
# breaks every SASL_SSL Kafka client here even though the cert/chain/mount
# are otherwise all correct.
echo "[1/6] Root CA ($ROOT_CN)..."
openssl genrsa -out "$CA_DIR/dpn-root.key" "$KEY_SIZE"
openssl req -x509 -new -nodes -key "$CA_DIR/dpn-root.key" -sha256 \
  -days "$ROOT_DAYS" -subj "$SUBJ_BASE/CN=$ROOT_CN" \
  -addext "keyUsage=critical,keyCertSign,cRLSign" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -out "$CA_DIR/dpn-root.crt"

# ---- 2/6: common server key + CSR + cert (wildcard SANs) -------------------
# certs/server.key + certs/server.crt ARE the Vault PEM material (PART 1 of the
# "same key, many formats" idea) — published to AKV as VAULT-TLS-KEY / VAULT-TLS-CERT.
echo "[2/6] Server cert ($SERVER_CN, wildcard SANs)..."
openssl genrsa -out "$CERTS_DIR/server.key" "$KEY_SIZE"
openssl req -new -key "$CERTS_DIR/server.key" -out "$CERTS_DIR/server.csr" \
  -config "$SRV_CFG"
openssl x509 -req -in "$CERTS_DIR/server.csr" \
  -CA "$CA_DIR/dpn-root.crt" -CAkey "$CA_DIR/dpn-root.key" -CAcreateserial \
  -days "$SERVER_DAYS" -sha256 \
  -extfile "$SRV_CFG" -extensions v3_server \
  -out "$CERTS_DIR/server.crt"

# ---- 3/6: keystore.jks (server identity, alias dpn-server) -----------------
echo "[3/6] keystore.jks (alias $SERVER_ALIAS)..."
[ -f "$KEYSTORE" ] && rm -f "$KEYSTORE"
openssl pkcs12 -export \
  -in "$CERTS_DIR/server.crt" -inkey "$CERTS_DIR/server.key" \
  -certfile "$CA_DIR/dpn-root.crt" -name "$SERVER_ALIAS" \
  -out "$SERVER_P12" -passout pass:"$KEYSTORE_PASS"
keytool -importkeystore -noprompt \
  -srckeystore "$SERVER_P12" -srcstoretype PKCS12 -srcstorepass "$KEYSTORE_PASS" \
  -destkeystore "$KEYSTORE" -deststoretype PKCS12 -deststorepass "$KEYSTORE_PASS" \
  -srcalias "$SERVER_ALIAS" -destalias "$SERVER_ALIAS"

# ---- 4/6: Client CA (self-signed) ------------------------------------------
# Same keyUsage fix as the root CA above — see that comment for why.
echo "[4/6] Client CA ($CLIENT_CA_CN)..."
openssl genrsa -out "$CA_DIR/dpn-client-ca.key" "$KEY_SIZE"
openssl req -x509 -new -nodes -key "$CA_DIR/dpn-client-ca.key" -sha256 \
  -days "$CLIENT_CA_DAYS" -subj "$SUBJ_BASE/CN=$CLIENT_CA_CN" \
  -addext "keyUsage=critical,keyCertSign,cRLSign" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -out "$CA_DIR/dpn-client-ca.crt"

# ---- 5/6: client cert (dsm-client) -> dsm-client.p12 -----------------------
echo "[5/6] Client cert ($CLIENT_CN) + dsm-client.p12..."
openssl genrsa -out "$CERTS_DIR/dsm-client.key" "$KEY_SIZE"
openssl req -new -key "$CERTS_DIR/dsm-client.key" \
  -subj "$SUBJ_BASE/CN=$CLIENT_CN" -out "$CERTS_DIR/dsm-client.csr"
openssl x509 -req -in "$CERTS_DIR/dsm-client.csr" \
  -CA "$CA_DIR/dpn-client-ca.crt" -CAkey "$CA_DIR/dpn-client-ca.key" -CAcreateserial \
  -days "$CLIENT_DAYS" -sha256 \
  -extfile "$CLI_CFG" -extensions v3_client \
  -out "$CERTS_DIR/dsm-client.crt"
[ -f "$CLIENT_P12" ] && rm -f "$CLIENT_P12"
openssl pkcs12 -export \
  -in "$CERTS_DIR/dsm-client.crt" -inkey "$CERTS_DIR/dsm-client.key" \
  -name "$CLIENT_CN" -out "$CLIENT_P12" -passout pass:"$CLIENT_P12_PASS"

# ---- 6/6: truststore.jks (Root CA + Client CA) -----------------------------
echo "[6/6] truststore.jks (dpn-root + dpn-client-ca)..."
[ -f "$TRUSTSTORE" ] && rm -f "$TRUSTSTORE"
keytool -import -trustcacerts -noprompt -alias dpn-root \
  -file "$CA_DIR/dpn-root.crt" -keystore "$TRUSTSTORE" \
  -storetype PKCS12 -storepass "$TRUSTSTORE_PASS"
keytool -import -trustcacerts -noprompt -alias dpn-client-ca \
  -file "$CA_DIR/dpn-client-ca.crt" -keystore "$TRUSTSTORE" \
  -storetype PKCS12 -storepass "$TRUSTSTORE_PASS"

# ---- 7/7: Vault DSM bootstrap CSR (CN=vault, wildcard SAN) — UNSIGNED -------
# vault.key + vault.csr for MANUAL signing by the DSM Vault. NOT self-signed here:
# you submit vault.csr to DSM, get back certificate.pem + ca-chain.pem, then load
# vault.key + those into the DPN Vault via vault-load-bundle-cd (unchanged). Same
# flow as the old create-vault-tls-bootstrap.sh, only the SAN is now the wildcard.
echo "[7/7] Vault DSM bootstrap CSR ($VAULT_CN, wildcard SAN) -> vault.key + vault.csr..."
cat > "$VAULT_CFG" <<EOF
[ req ]
default_bits       = $KEY_SIZE
prompt             = no
default_md         = sha256
distinguished_name = dn
req_extensions     = req_ext

[ dn ]
C  = $COUNTRY
O  = $ORG
CN = $VAULT_CN

[ req_ext ]
subjectAltName = @alt_names

[ alt_names ]
DNS.1 = $SAN_DNS_1
DNS.2 = $SAN_DNS_2
EOF
openssl genrsa -out "$CERTS_DIR/vault.key" "$KEY_SIZE"
openssl req -new -key "$CERTS_DIR/vault.key" -out "$CERTS_DIR/vault.csr" -config "$VAULT_CFG"

echo ""
echo "=========================================="
echo " DONE — artifacts under $OUT_DIR/"
echo "   certs/server.crt + certs/server.key  -> Vault HTTPS listener (AKV VAULT-TLS-CERT / VAULT-TLS-KEY, self-signed)"
echo "   keystore.jks   (alias $SERVER_ALIAS) -> Keycloak + Kafka (k8s secret dpn-tls)"
echo "   truststore.jks (dpn-root + client CA)-> cert-manager-truststore + dpn-tls"
echo "   dsm-client.p12 (client cert for mTLS / browser install)"
echo "   certs/vault.key + certs/vault.csr    -> MANUAL DSM-sign the CSR, then vault-load-bundle-cd"
echo "=========================================="
echo "--- server.crt summary (the universal cert) ---"
openssl x509 -in "$CERTS_DIR/server.crt" -noout -subject -issuer -dates
openssl x509 -in "$CERTS_DIR/server.crt" -noout -ext subjectAltName
echo "--- truststore entries ---"
keytool -list -keystore "$TRUSTSTORE" -storetype PKCS12 -storepass "$TRUSTSTORE_PASS" | grep -iE "trustedCertEntry|Alias"
