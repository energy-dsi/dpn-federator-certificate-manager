ui = true

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = 1
}

storage "file" {
  path = "/vault/file"
}

# Optional but helpful: disables mlock warnings in containers
disable_mlock = true