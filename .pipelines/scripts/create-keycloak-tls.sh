#!/bin/sh
# =============================================================================
# Keycloak TLS material generator — DPN Platform
#
# Self-signed, self-contained generator for the DPN Keycloak (DSM auth) TLS
# material. Mirrors create-vault-tls-bootstrap.sh: one self-signed DPN Root CA
# is the trust anchor for the server identity, so Keycloak uses the SAME kind of
# self-signed "common DPN certificate" that Vault does.
#
# Produces, in one run (under <OUT_DIR>):
#   ca/dpn-root.crt          — self-signed DPN Root CA   (CN=dpn-root)  [trust anchor]
#   ca/dpn-root.key          — Root CA private key       (keep safe / discard)
#   ca/dpn-client-ca.crt     — self-signed Client CA     (CN=dpn-client-ca)
#   ca/dpn-client-ca.key     — Client CA private key
#   certs/server.key         — Keycloak server private key
#   certs/server.crt         — Keycloak server cert (CN=dpn-common, WILDCARD SANs, signed by Root CA)
#   keystore.jks             — PKCS12 keystore (alias dpn-server: server key+cert+chain) [1]
#   truststore.jks           — PKCS12 truststore (dpn-root + dpn-client-ca)              [2]
#   dpn-client.p12           — PKCS12 client cert (CN=dpn-client, signed by Client CA)   [3]
#
# keystore.jks / truststore.jks are PKCS12 (named .jks) because the Keycloak chart
# reads them with KC_SPI_TRUSTSTORE_FILE_TYPE / *_CLIENT_KEYSTORE_TYPE = pkcs12.
# The key alias is dpn-server — the dsm-management-node Keycloak chart MUST set
# KC_HTTPS_KEY_ALIAS=dpn-server to match (default there is dsm-server).
#
# The server cert carries WILDCARD SANs so ONE cert serves any service in the
# two DPN namespaces (Keycloak, Vault, etc.) — this is the "common DPN cert":
#   DNS: *.ns-dpn-01.svc.cluster.local
#   DNS: *.ns-dpn-health-01.svc.cluster.local
#
# The k8s secret `dpn-tls` (created by the pipeline, not here) is a GENERIC secret
# holding keystore.jks + truststore.jks — the Keycloak chart mounts it at /cert and
# reads /cert/keystore.jks + /cert/truststore.jks.
#
# Required env:
#   KEYSTORE_PASS    — keystore.jks password
#   TRUSTSTORE_PASS  — truststore.jks password
#   CLIENT_P12_PASS  — dpn-client.p12 password
# Customer inputs (the only tunable subject fields):
#   COUNTRY          — subject Country (C)      (default GB)
#   ORG              — subject Organisation (O) (default DSI)
# Optional plumbing:
#   OUT_DIR          — output base directory    (default keycloak-tls)
#
# NOTE (future work): this is intentionally SEGREGATED from create-vault-tls-
# bootstrap.sh for now. Once proven, the Root CA generation here and there should
# be unified so Vault + Keycloak chain to ONE common DPN Root CA.
# =============================================================================
set -eu

# ---- Fixed identity ---------------------------------------------------------
ROOT_CN="dpn-root"
SERVER_CN="dpn-common"
CLIENT_CA_CN="dpn-client-ca"
CLIENT_CN="dpn-client"
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
OUT_DIR="${OUT_DIR:-keycloak-tls}"
CA_DIR="$OUT_DIR/ca"
CERTS_DIR="$OUT_DIR/certs"
SRV_CFG="$CERTS_DIR/server-openssl.cnf"
CLI_CFG="$CERTS_DIR/client-openssl.cnf"
KEYSTORE="$OUT_DIR/keystore.jks"
TRUSTSTORE="$OUT_DIR/truststore.jks"
CLIENT_P12="$OUT_DIR/dpn-client.p12"
SERVER_P12="$CERTS_DIR/server.p12"

SUBJ_BASE="/C=$COUNTRY/O=$ORG"

echo "=========================================="
echo " Keycloak TLS generator (self-signed DPN)"
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
echo "[1/6] Root CA ($ROOT_CN)..."
openssl genrsa -out "$CA_DIR/dpn-root.key" "$KEY_SIZE"
openssl req -x509 -new -nodes -key "$CA_DIR/dpn-root.key" -sha256 \
  -days "$ROOT_DAYS" -subj "$SUBJ_BASE/CN=$ROOT_CN" -out "$CA_DIR/dpn-root.crt"

# ---- 2/6: Keycloak server key + CSR + cert (wildcard SANs) ------------------
echo "[2/6] Server cert ($SERVER_CN, wildcard SANs)..."
openssl genrsa -out "$CERTS_DIR/server.key" "$KEY_SIZE"
openssl req -new -key "$CERTS_DIR/server.key" -out "$CERTS_DIR/server.csr" \
  -config "$SRV_CFG"
openssl x509 -req -in "$CERTS_DIR/server.csr" \
  -CA "$CA_DIR/dpn-root.crt" -CAkey "$CA_DIR/dpn-root.key" -CAcreateserial \
  -days "$SERVER_DAYS" -sha256 \
  -extfile "$SRV_CFG" -extensions v3_server \
  -out "$CERTS_DIR/server.crt"

# ---- 3/6: keystore.jks (server identity) -----------------------------------
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
echo "[4/6] Client CA ($CLIENT_CA_CN)..."
openssl genrsa -out "$CA_DIR/dpn-client-ca.key" "$KEY_SIZE"
openssl req -x509 -new -nodes -key "$CA_DIR/dpn-client-ca.key" -sha256 \
  -days "$CLIENT_CA_DAYS" -subj "$SUBJ_BASE/CN=$CLIENT_CA_CN" -out "$CA_DIR/dpn-client-ca.crt"

# ---- 5/6: client cert (dpn-client) -> dpn-client.p12 -----------------------
echo "[5/6] Client cert ($CLIENT_CN) + dpn-client.p12..."
openssl genrsa -out "$CERTS_DIR/dpn-client.key" "$KEY_SIZE"
openssl req -new -key "$CERTS_DIR/dpn-client.key" \
  -subj "$SUBJ_BASE/CN=$CLIENT_CN" -out "$CERTS_DIR/dpn-client.csr"
openssl x509 -req -in "$CERTS_DIR/dpn-client.csr" \
  -CA "$CA_DIR/dpn-client-ca.crt" -CAkey "$CA_DIR/dpn-client-ca.key" -CAcreateserial \
  -days "$CLIENT_DAYS" -sha256 \
  -extfile "$CLI_CFG" -extensions v3_client \
  -out "$CERTS_DIR/dpn-client.crt"
[ -f "$CLIENT_P12" ] && rm -f "$CLIENT_P12"
openssl pkcs12 -export \
  -in "$CERTS_DIR/dpn-client.crt" -inkey "$CERTS_DIR/dpn-client.key" \
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

echo ""
echo "=========================================="
echo " DONE — artifacts under $OUT_DIR/"
echo "   keystore.jks    (Keycloak server identity, alias $SERVER_ALIAS)"
echo "   truststore.jks  (dpn-root + dpn-client-ca)"
echo "   dpn-client.p12  (client cert for mTLS / browser install)"
echo "   keystore.jks + truststore.jks  -> generic k8s secret 'dpn-tls' (mounted at /cert)"
echo "=========================================="
echo "--- server.crt summary ---"
openssl x509 -in "$CERTS_DIR/server.crt" -noout -subject -issuer -dates
openssl x509 -in "$CERTS_DIR/server.crt" -noout -ext subjectAltName
echo "--- truststore entries ---"
keytool -list -keystore "$TRUSTSTORE" -storetype PKCS12 -storepass "$TRUSTSTORE_PASS" | grep -iE "trustedCertEntry|Alias"
