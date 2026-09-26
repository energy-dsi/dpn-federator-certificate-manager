# GitHub Actions deployment reference

Reference copy of this component's GitHub Actions deployment tooling, sourced from `dpn-containerised-deployment-service` (branch `feature/merge-azure-aws`, where this component's directory is named `dpn-federator-cert-manager`) on 2026-09-22. Replaces a previous, incorrectly-shaped copy of this folder.

**This is a reference copy, not a runnable pipeline from this location, and does not affect the existing Azure DevOps pipelines under `.pipelines/azure-pipelines/`.** The actual GitHub Actions CD pipeline still runs centrally from `dpn-containerised-deployment-service`, which checks out this repo's charts directly — GitHub Actions only auto-discovers workflows under a repo's own `.github/workflows/`, so nothing here is triggerable from this repo as-is. This folder exists so this repo's own history reflects its GitHub Actions deployment configuration.

- `actions/cloud-login/` — the shared composite action every workflow here uses to authenticate to whichever cloud (azure/aws/gcp) is selected at dispatch time
- `config/{aws,azure}/*.json` — per-environment config, one set per cloud (shared across all DPN components deployed to that environment; each workflow reads only the keys it needs)
- `config/scripts/create-vault-tls-bootstrap.sh`, `load-bootstrap-bundle.sh`, `vault-init.sh`, `create-dpn-tls.sh` — supporting scripts run by the Vault/TLS workflows below (not chart installs themselves)
- `workflows/dpn-gha-certificate-manager-cd.yaml` — installs the Certificate Manager chart
- `workflows/dpn-gha-vault-cd.yaml` — installs the basic Vault chart
- `workflows/dpn-gha-vault-https-cd.yaml` — installs the Vault-HTTPS chart
- `workflows/dpn-gha-vault-tls-bootstrap-cd.yaml` — generates Vault's TLS material and the Certificate Manager's truststore (runs `create-vault-tls-bootstrap.sh` against a live Vault, not a chart install)
- `workflows/dpn-gha-vault-load-bundle-cd.yaml` — loads the signed certificate bundle into Vault (runs `load-bootstrap-bundle.sh`)
- `workflows/dpn-gha-certificate-manager-role-id-creation.yaml` — provisions a Vault AppRole identity; **shared with `dpn-rest-federator-gateway`** via this same workflow's `component` input (`certificate-manager` or `rest-federator`)
- `workflows/dpn-gha-tls-cd.yml` — copy of source's `dpn-tls-cd.yaml` (source name: "DPN CD - Prerequisite Pipeline (AWS Universal TLS) #1"), renamed here as `dpn-gha-tls-cd.yml` per naming convention. **Cross-cutting, not certificate-manager-specific**: generates ONE self-signed DPN Root CA + universal server cert (via `create-dpn-tls.sh`) and publishes the resulting secrets/K8s objects consumed by Vault, Certificate Manager, Keycloak, Kafka, the Federator Gateway, and Health Monitoring's oauth2-proxy/Airflow TLS all in a single run. It doesn't map to one component in the source repo (no single `CHART_DIR`) — placed here because it's the TLS prerequisite step ahead of this repo's own Vault/cert-manager workflows above, and because `create-dpn-tls.sh` sits alongside the other Vault-adjacent scripts. Consumers in other repos (Keycloak, Kafka, Federator Gateway, Health Monitoring oauth2-proxy/Airflow) do not have their own copy of this workflow — it is intentionally not duplicated.
- All workflows handle all three clouds via their own `cloud` input (not three separate files).

The Helm values for this repo's three charts on each cloud now live where they belong — alongside each chart, under `values/<cloud>/<environment>-<cluster>.yaml` (e.g. `charts/vault-https/values/aws/dev-dpn01.yaml`) — not in this folder. This mirrors the `values/{aws,azure,gcp}/<environment>-<cluster>.yaml` folder structure used in the source repo `dpn-containerised-deployment-service` (GCP excluded here). It sits alongside, and does not touch, the existing flat `values-<environment>-<cluster>.yaml` files used by the Azure DevOps pipelines.

No uninstall or rollback workflow exists yet for any of these.
