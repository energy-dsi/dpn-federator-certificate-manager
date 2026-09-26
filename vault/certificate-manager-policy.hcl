# Vault ACL policy for the certificate-manager's AppRole identity.
#
# VaultSecretProviderImpl (uk.gov.dbt.ndtp.federator.certificate.manager.service.pki) uses the
# Spring Cloud Vault VaultTemplate against the KV v2 mount derived from
# application.vault.secret-path. The secret-path is split on the first "/" into
# <mount>/<base-path>; with the default DPN configuration secret-path is
# "pki-client/node-net/client", i.e. mount "pki-client" and base path "node-net/client".
# KV v2 prefixes the base path with "data/" for reads/writes and "metadata/" for
# metadata/list/delete operations.
#
# ensureKvMountExists() also calls vaultTemplate.opsForSys().getMounts(), which requires read
# access to "sys/mounts".

path "sys/mounts" {
  capabilities = ["read"]
}

path "pki-client/data/node-net/client/*" {
  capabilities = ["create", "read", "update", "list"]
}

path "pki-client/metadata/node-net/client/*" {
  capabilities = ["read", "list", "delete"]
}
