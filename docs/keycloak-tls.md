# Keycloak TLS material — DPN Platform

Generates the TLS material the DPN **Keycloak** (DSM auth) deployment needs, using the same
kind of **self-signed "common DPN certificate"** approach that Vault uses. Deployment of
Keycloak itself is owned by another team; this module only **prepares the cert material and
publishes it**.

- **Script:**  `.pipelines/scripts/create-keycloak-tls.sh`
- **Pipeline:** `.pipelines/azure-pipelines/cd-pipelines/keycloak-tls-cd.yaml`

> **Status: segregated.** Kept separate from the Vault cert module for now. Once proven, the
> Root CA generation here and in `create-vault-tls-bootstrap.sh` will be **unified** so Vault
> and Keycloak chain to ONE common DPN Root CA (see *Merge plan* below).

---

## What it produces

| File | Purpose |
|---|---|
| `keystore.jks` | Keycloak HTTPS **server identity** — **PKCS12** (named `.jks`), alias `dpn-server` (server key + cert + Root CA chain) |
| `truststore.jks` | Keycloak **truststore** — **PKCS12**, `dpn-root` (server CA) + `dpn-client-ca` (to validate client certs for mTLS) |
| `dpn-client.p12` | **Client cert** (CN=`dpn-client`, signed by the Client CA) — install in a browser / service for mTLS to Keycloak (NOT consumed by Keycloak itself) |

> **Formats/alias match `dsm-management-node/charts/keycloak`:** the chart reads the files with
> `KC_SPI_TRUSTSTORE_FILE_TYPE`/client-keystore-type = `pkcs12`, so the stores are **PKCS12**.
> The key alias is `dpn-server` — the Keycloak chart must set `KC_HTTPS_KEY_ALIAS=dpn-server`
> to match (its default is `dsm-server`).

**Identities / chain (all self-signed, no external CA):**

```
dpn-root (self-signed Root CA)
  └── dpn-common       (server cert CN, WILDCARD SANs, alias dpn-server)  -> keystore.jks + dpn-tls secret

dpn-client-ca (self-signed Client CA)
  └── dpn-client        (client cert)                 -> dpn-client.p12

truststore.jks = dpn-root + dpn-client-ca
```

**Wildcard SANs** (this is what makes it a *common* DPN cert — one cert serves any service in
either namespace, Keycloak and Vault alike):

```
DNS: *.ns-dpn-01.svc.cluster.local
DNS: *.ns-dpn-health-01.svc.cluster.local
```

Everything is fixed except **Country (C)**, **Organisation (O)**, and the three store passwords.

---

## Pipeline: what it does

Mirrors `vault-tls-bootstrap-cd.yaml` (guard → approval → work). The work stage:

1. Runs `create-keycloak-tls.sh` (self-signed, wildcard SANs, PKCS12 stores).
2. Optionally stores the passwords in Key Vault under the **names the Keycloak
   SecretProviderClass expects** (so they're consumed automatically):
   - `KC-HTTPS-KEY-STORE-PASSWORD` = keystore password (also reused for the outbound client keystore/key)
   - `KC-HTTPS-TRUST-STORE-PASSWORD` = truststore password
   - `KC-SPI-TRUSTSTORE-FILE-PASSWORD` = truststore password (same `truststore.jks`)
   - `DPN-CLIENT-P12-PASSWORD` = `dpn-client.p12` password (browser client only — Keycloak doesn't read it)
3. **Creates the generic k8s secret `dpn-tls`** in the chosen namespace holding
   **`keystore.jks` + `truststore.jks`** — the Keycloak chart mounts it at `/cert` and reads
   `/cert/keystore.jks` + `/cert/truststore.jks`. Point the chart's `tlsSecretName` at `dpn-tls`
   (chart default is `dsm-tls`). (Created via delete+create, not `dry-run|apply`, so the keystore's
   private key is never printed to the pipeline log.)
4. **Publishes** `keystore.jks` / `truststore.jks` / `dpn-client.p12` as the pipeline artifact
   `keycloak-tls-<env>-<cluster>`.

Inputs: `environment`, `dpnCluster`, `targetNamespace` (`ns-dpn-01` or `ns-dpn-health-01`),
`country`, `org`, the three store passwords, `storePasswordsInKeyVault`.

> ⚠️ The published artifact and the `dpn-client.p12` contain **private keys**. Restrict who can
> download pipeline artifacts and delete the run once the material is collected.

Run it locally the same way:
```sh
KEYSTORE_PASS=… TRUSTSTORE_PASS=… CLIENT_P12_PASS=… COUNTRY=GB ORG=DSI OUT_DIR=./keycloak-tls \
  sh .pipelines/scripts/create-keycloak-tls.sh
```

---

## Mapping to the manual reference steps

The original manual procedure (NESO-signed) maps to this self-signed module as follows:

| Manual step (reference) | Here (self-signed DPN) |
|---|---|
| `cat intermediate.crt neso-rootca.crt > ca-chain.crt` | self-signed `dpn-root.crt` is the whole chain |
| `openssl pkcs12 -export -in dsm.crt … -name dsm-server` + `keytool -importkeystore … keystore.jks` | step 3 → `keystore.jks` (alias `dpn-server`) |
| `keytool -import -alias … ca-chain.crt -keystore truststore.jks` | `truststore.jks` gets `dpn-root` |
| `openssl req -x509 … CN=NESO-Client-CA … client-ca.crt` | `dpn-client-ca` (self-signed Client CA) |
| `openssl x509 -req … -CA client-ca.crt … dpn-client.crt` (v3_client EKU) | step 5 (`clientAuth` EKU) |
| `keytool -import -alias client-ca … truststore.jks` | `truststore.jks` also gets `dpn-client-ca` |
| `openssl pkcs12 -export -in dpn-client.crt … dpn-client.p12` | step 5 → `dpn-client.p12` |

Difference: **no NESO/DSM external CA** — DPN uses self-signed roots throughout, exactly like
the Vault setup.

---

## Merge plan (future — make it a common DPN certificate)

1. Extract the Root CA generation into a shared helper used by both
   `create-vault-tls-bootstrap.sh` and `create-keycloak-tls.sh`, so both chain to one
   `dpn-root`.
2. Give the Vault server cert the same **wildcard SANs**
   (`*.ns-dpn-01.svc.cluster.local`, `*.ns-dpn-health-01.svc.cluster.local`) instead of the
   single fixed `vault-https.ns-dpn-01.svc.cluster.local`, so one server cert covers Vault,
   Keycloak, and any other in-cluster service.
3. Fold the two pipelines into one "DPN certificate" pipeline that emits the Vault material,
   the Keycloak material, and the `dpn-tls` secret from the same Root CA in a single run.
