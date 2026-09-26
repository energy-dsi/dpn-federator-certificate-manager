# DPN Vault — AppRole Setup & Secret Handling (Certificate Manager + Federator)

This document describes how the **DPN Vault** is configured for **AppRole** authentication, how
the **certificate manager** and **federator** (server + client) authenticate with it, and
**how the AppRole `secret_id` is kept** in local‑dev vs. Kubernetes.

> Scope: this is the auth/secret model for the DPN vault that stores the federator's certificate
> material (leaf certificate, key pair, CA chain, intermediate CA). It is separate from the
> management‑node's own Vault PKI engine (`pki_int`) that *signs* the certificates.

---

## 1. What the DPN Vault holds

A **KV v2** secrets engine mounted at **`pki-client`**, with the federator's material under
`node-net/client`:

| Path (KV v2) | Fields | Written by | Read by |
|---|---|---|---|
| `pki-client/data/node-net/client/certificate` | `certificate` | cert‑manager | cert‑manager, federator |
| `pki-client/data/node-net/client/keypair` | `publicKey`, `privateKey` | cert‑manager | cert‑manager, federator |
| `pki-client/data/node-net/client/ca-chain` | `chain` | cert‑manager | cert‑manager, federator |
| `pki-client/data/node-net/client/intermediate-ca` | `certificate` | cert‑manager | cert‑manager |

`VAULT_SECRET_PATH` is `pki-client/node-net/client` — the app splits it on the first `/` into
the KV **mount** (`pki-client`) and the **base path** (`node-net/client`).

---

## 2. AppRole configuration on the DPN Vault

AppRole is Vault's machine‑to‑machine auth method: a non‑secret **`role_id`** (like a username)
plus a secret **`secret_id`** (like a password) are exchanged at `auth/approle/login` for a
short‑lived, auto‑renewed Vault **client token**.

### 2.1 Enable AppRole + write least‑privilege policies

```sh
export VAULT_ADDR=http://<dpn-vault>:8200
export VAULT_TOKEN=<admin-token>          # a token allowed to manage auth methods/policies/roles

vault auth enable approle                 # no-op if already enabled

# certificate-manager: read/write its KV material (KV v2 => data/ + metadata/), read sys/mounts
vault policy write certificate-manager-policy - <<'HCL'
path "sys/mounts"                             { capabilities = ["read"] }
path "pki-client/data/node-net/client/*"      { capabilities = ["create","read","update","list"] }
path "pki-client/metadata/node-net/client/*"  { capabilities = ["read","list","delete"] }
HCL

# federator: read-only access to the certificate material
vault policy write federator-policy - <<'HCL'
path "pki-client/*"                           { capabilities = ["read","list"] }
HCL
```

These policies also ship in each repo under `vault/certificate-manager-policy.hcl` and
`vault/federator-policy.hcl` (see `vault/setup-approle.sh`).

### 2.2 Create the AppRole roles

```sh
vault write auth/approle/role/certificate-manager \
    token_policies="certificate-manager-policy" \
    token_ttl=1h token_max_ttl=4h \
    secret_id_ttl=0 secret_id_num_uses=0     # 0 = non-expiring, unlimited-use (local dev)

vault write auth/approle/role/federator \
    token_policies="federator-policy" \
    token_ttl=1h token_max_ttl=4h \
    secret_id_ttl=0 secret_id_num_uses=0
```

> **Production hardening:** set a finite `secret_id_ttl` (e.g. `24h`), `secret_id_num_uses`,
> `token_ttl`/`token_max_ttl`, and bind with `secret_id_bound_cidrs` / `token_bound_cidrs`.
> Rotate `secret_id`s on a schedule.

### 2.3 Read the `role_id` and issue a `secret_id`

```sh
# role_id is NOT sensitive — it can live in config/env/ConfigMap
vault read -field=role_id  auth/approle/role/certificate-manager/role-id
vault read -field=role_id  auth/approle/role/federator/role-id

# secret_id IS sensitive — treat like a password
vault write -f -field=secret_id auth/approle/role/certificate-manager/secret-id
vault write -f -field=secret_id auth/approle/role/federator/secret-id
```

---

## 3. How each app consumes AppRole

### 3.1 Certificate Manager (Spring Cloud Vault)

Auth method and credentials are config‑driven (`application.yml` → env):

| Env var | Property | Meaning |
|---|---|---|
| `VAULT_AUTHENTICATION=approle` | `spring.cloud.vault.authentication` | select AppRole |
| `VAULT_APPROLE_ROLE_ID` | `spring.cloud.vault.app-role.role-id` | role_id (not sensitive) |
| `VAULT_APPROLE_SECRET_ID` | `spring.cloud.vault.app-role.secret-id` | **secret_id (sensitive)** |
| `VAULT_APPROLE_ROLE` | `spring.cloud.vault.app-role.role` | role name (`certificate-manager`) |
| `VAULT_APPROLE_MOUNT_PATH` | `spring.cloud.vault.app-role.app-role-path` | AppRole mount (`approle`) |

Spring Cloud Vault's `AppRoleClientAuthenticationProvider` performs `auth/approle/login` and the
`LifecycleAwareSessionManager` renews the resulting token automatically. `VaultAuthStartupValidator`
fails fast at startup if `authentication=approle` but `role-id`/`secret-id` are missing.

### 3.2 Federator (custom `PropertyUtil` + BetterCloud Vault client)

| Env var | Property | Meaning |
|---|---|---|
| — | `vault.auth.method=approle` | select AppRole |
| `VAULT_ROLE_ID` | `vault.approle.role-id` | role_id (env takes precedence) |
| `VAULT_SECRET_ID` | `vault.approle.secret-id` | **secret_id (sensitive)** |
| — | `vault.approle.secret-id-path` | file path to read the secret_id from (preferred over the property) |
| — | `vault.approle.mount-path=approle` | AppRole mount |

`VaultClient` performs `loginByAppRole(...)` and `VaultTokenRenewalManager` keeps the token alive
(renew‑self, re‑login on failure).

---

## 4. How the `secret_id` is kept (this is the sensitive part)

**Never** commit a `secret_id` to git, bake it into an image, or log it. Resolution precedence and
storage by environment:

### Local dev (this setup)
- The `role_id`/`secret_id` for both roles are captured once by the provisioning script into a
  session‑local file **outside the repos**
  (`…/scratchpad/approle-creds.json`) and injected as container env at `podman run`:
  - cert‑manager: `VAULT_APPROLE_ROLE_ID` / `VAULT_APPROLE_SECRET_ID`
  - federator: `VAULT_ROLE_ID` / `VAULT_SECRET_ID`
- This file is ephemeral and is regenerated whenever the dev vault is re‑provisioned.

### Kubernetes (recommended)
- Store the `secret_id` in a **Kubernetes `Secret`** (not a ConfigMap); put the non‑sensitive
  `role_id` in a ConfigMap or values file.
  - cert‑manager chart: `secrets.vaultAppRoleSecretId` → Secret key `VAULT_APPROLE_SECRET_ID`,
    injected via `envFrom: secretRef`. `role_id` is `vault.appRole.roleId` in values → ConfigMap
    `VAULT_APPROLE_ROLE_ID`.
  - federator chart: `VAULT_SECRET_ID` from `secretKeyRef` (`certificate-manager-secrets`),
    `VAULT_ROLE_ID` from `vaultConfig.appRole.roleId`.
- **Preferred over env:** mount the Secret as a **file** and point the app at it:
  - federator: `vault.approle.secret-id-path=/vault/secrets/secret-id`
  - cert‑manager: Spring Boot config‑tree —
    `spring.config.import=optional:configtree:/vault/secrets/` with a file at
    `/vault/secrets/spring.cloud.vault.app-role.secret-id`.
  A mounted file avoids the `secret_id` appearing in `kubectl describe pod` / process env.
- **Best practice:** don't store a long‑lived `secret_id` at all — use the **AppRole pull model**:
  a trusted orchestrator (Vault Agent, `vault-k8s`/CSI, or an init container with a Vault
  Kubernetes‑auth token) requests a short‑TTL, limited‑use `secret_id` (a "response‑wrapped" token)
  and delivers it to the pod at start. Combine with a finite `secret_id_ttl`.

### Rotation
- `role_id` is stable. Rotate `secret_id`s by issuing a new one
  (`vault write -f auth/approle/role/<role>/secret-id`) and updating the Secret; optionally revoke
  the old one (`auth/approle/role/<role>/secret-id/destroy`).

---

## 5. Quick verification

```sh
# AppRole login works and returns a token with the expected policy
vault write auth/approle/login role_id=<role_id> secret_id=<secret_id>

# cert-manager logs on startup:
#   "Vault authentication method: APPROLE (mount path 'approle')"
# federator logs on startup:
#   "Vault AppRole login succeeded, lease duration: 3600s, renewable: ..."
```

© Crown Copyright 2026. National Digital Twin Programme.
