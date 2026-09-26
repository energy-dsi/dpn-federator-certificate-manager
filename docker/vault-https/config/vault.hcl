ui = true

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = 0
  tls_cert_file = "/vault/certs/vault.crt"
  tls_key_file  = "/vault/certs/vault.key"
}

storage "file" {
  path = "/vault/file"
}

# Optional but helpful: disables mlock warnings in containers
disable_mlock = true

api_addr = "https://localhost:8200"
cluster_addr = "https://localhost:8201"

