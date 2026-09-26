# DPN Vault — Certificate Manager

This directory holds the Vault ACL policy and setup script for the **certificate manager**'s
identity in the DPN Vault, and documents how the certificate material flows through Vault now
that the Azure SMB file share has been removed.

## Authentication: AppRole

The certificate manager authenticates to Vault with **AppRole** (Spring Cloud Vault's built-in
`APPROLE` method), selected via `spring.cloud.vault.authentication` (env `VAULT_AUTHENTICATION`).
`token` is still supported for backwards compatibility.

| Property | Env var | Default |
|---|---|---|
| `spring.cloud.vault.authentication` | `VAULT_AUTHENTICATION` | `token` |
| `spring.cloud.vault.app-role.role-id` | `VAULT_APPROLE_ROLE_ID` | - |
| `spring.cloud.vault.app-role.secret-id` | `VAULT_APPROLE_SECRET_ID` | - |
| `spring.cloud.vault.app-role.role` | `VAULT_APPROLE_ROLE` | `certificate-manager` |
| `spring.cloud.vault.app-role.app-role-path` | `VAULT_APPROLE_MOUNT_PATH` | `approle` |

`VaultAuthStartupValidator` fails fast at startup if `authentication=approle` but the
role-id/secret-id are missing.

Run `./setup-approle.sh` (with `VAULT_ADDR`/`VAULT_TOKEN` for an admin token) to create the
`certificate-manager` policy + role and print the `role_id`/`secret_id`.

### Automated (Azure pipeline)

`.pipelines/azure-pipelines/cd-pipelines/vault-approle-setup-cd.yaml` does this per environment:
it runs `setup-approle.sh` inside the Vault pod (admin token pulled from Key Vault `VAULT-TOKEN`),
writes the generated **secret-id** to Key Vault as **`VAULT-APPROLE-SECRET-ID`** (which the
cert-manager `SecretProviderClass` consumes when `vault.authentication=APPROLE`), and prints the
**role-id**. Then set that `role_id` in the env values file under `vault.appRole.roleId`, flip
`vault.authentication` to `APPROLE`, and redeploy.

The Key Vault secret is stamped with a **+355-day expiry** to satisfy the Key Vault governance
policy (secrets must have a maximum validity period). The AppRole secret-id itself does not
expire (`secret_id_ttl=0`), so **re-run the pipeline before the Key Vault copy expires** to
rotate it — a re-run generates a fresh secret-id, refreshes the expiry, and you redeploy to
pick it up.

## No more SMB file share

The certificate manager no longer writes `keystore.p12` / `truststore.p12` to a shared Azure
file share, and no longer reads its own mTLS keystore from disk. It builds the mTLS
keystore/truststore **in memory** from the certificate material in Vault
(`MtlsHttpClientBuilder`). The federator reads the same material directly from Vault. No PV/PVC
is created by the chart.

## Vault layout (KV v2)

The KV v2 engine is mounted at `pki-client`; the certificate material lives under
`node-net/client` (so `VAULT_SECRET_PATH=pki-client/node-net/client`).

| Path | Field(s) | Written by | Read by |
|---|---|---|---|
| `pki-client/node-net/client/certificate` | `certificate` | cert-manager | cert-manager, federator |
| `pki-client/node-net/client/keypair` | `publicKey`, `privateKey` | cert-manager | cert-manager, federator |
| `pki-client/node-net/client/ca-chain` | `chain` | cert-manager | cert-manager, federator |
| `pki-client/node-net/client/intermediate-ca` | `certificate` | cert-manager | cert-manager |

## The DPN certificate (producer + consumer)

A **single** leaf certificate serves both the federator **producer** (gRPC server, `serverAuth`)
and **consumer** (gRPC client, `clientAuth`) endpoints; its SANs cover both endpoints, e.g.
`producer.<dpn>...` and `consumer.<dpn>...` (`CERT_SUBJECT_ALT_NAMES`). The CSR is signed by the
Management Node's sign endpoint (`/api/v1/certificate/csr/sign`) against the shared DPN root CA.

## Local development (standalone Vault, no Docker)

```sh
# 1. run a dev Vault (isolated from any other Vault you have on :8200)
vault server -dev -dev-root-token-id="dpn-root-token" -dev-listen-address="127.0.0.1:8210"
export VAULT_ADDR=http://127.0.0.1:8210 VAULT_TOKEN=dpn-root-token

# 2. KV v2 engine
vault secrets enable -path=pki-client kv-v2

# 3. seed a producer+consumer leaf signed by the shared root CA (…/project-D/certs/rootCA.*)
#    (see the repo root README for the openssl commands), then:
vault kv put pki-client/node-net/client/keypair       privateKey=@fed.pkcs8.key publicKey=@fed.pub
vault kv put pki-client/node-net/client/certificate   certificate=@fed.crt
vault kv put pki-client/node-net/client/ca-chain      chain=@rootCA.crt
vault kv put pki-client/node-net/client/intermediate-ca certificate=@rootCA.crt

# 4. AppRole
./setup-approle.sh

# 5. run the certificate manager against the DPN Vault via AppRole
VAULT_URI=http://127.0.0.1:8210 \
VAULT_AUTHENTICATION=approle \
VAULT_APPROLE_ROLE_ID=<role_id> VAULT_APPROLE_SECRET_ID=<secret_id> \
VAULT_SECRET_PATH=pki-client/node-net/client \
java -jar target/federator-certificate-manager-*.jar
```
