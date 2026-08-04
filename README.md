# README

**Repository:** `dpn-federator-certificate-manager`  
**Description:** `The Federator Certificate Manager is a non-interactive Spring Boot service that automates X.509 certificate lifecycle management for federator components within the DPN network.`  
**SPDX-License-Identifier:** `Apache-2.0 AND OGL-UK-3.0 `

---

## Overview

This repository contributes to the development of **secure, scalable, and interoperable data-sharing infrastructure**. It supports DSI's mission to enable **trusted, federated, and decentralised** data-sharing across organisations.

This repository is one of several open-source components that underpin DSI's **Data Preparation Node (DPN)**—a framework designed to allow organisations to manage and exchange data securely while maintaining control over their own information. The DPN is actively deployed and tested across multiple sectors, ensuring its adaptability and alignment with real-world needs.

The Federator Certificate Manager is a non-interactive Spring Boot service that automates X.509 certificate lifecycle management for federator components within the **Data Sharing Infrastructure (DSI)** DPN network. It operates as a headless daemon — no HTTP endpoints are exposed — running two scheduled jobs that handle certificate renewal and filesystem synchronisation.

The service integrates with **HashiCorp Vault** (KV v2) for secret persistence, an external **Management Node** API for PKI operations (intermediate CA retrieval and CSR signing), and an **OAuth2 Identity Provider** for token-based authentication. All external HTTP communication is secured via mutual TLS (mTLS).

---

## Prerequisites

| Dependency | Version | Purpose |
|------------|---------|---------|
| **JDK** | 21+ | Runtime and compilation |
| **Apache Maven** | 3.9+ | Build tool |
| **HashiCorp Vault** | 1.15+ | Secret storage (KV v2 engine) |
| **Management Node Endpoint** | — | External PKI API (intermediate CA, CSR signing from DSM) |
| **Auth Endpoint** | — | Identity Provider for client credentials from DSM |
| **JKS Keystores** | — | Client keystore and truststore for mTLS |

### System Requirements

- **OS:** Linux, macOS, or Windows with Java 21+
- **Memory:** 256 MB minimum heap (512 MB recommended)
- **Disk:** Writable path for PKCS#12 output files
- **Network:** Outbound HTTPS to Vault, Management Node, and IdP

---
## Configuration & Installation

Detailed configuration and installation instructions for this repository are present in **[dpn-integration-playbook](https://github.com/energy-dsi/dpn-integration-playbook)**

This includes producer/consumer setup, CI/CD pipeline configuration and execution, and deployment validation. Refer to the guide matching your deployment target:

### AWS Deployment

Refer [aws-manual-beta](https://github.com/energy-dsi/dpn-integration-playbook/tree/main/Docs/03-dpn-application-deployment/aws-manual-beta) for AWS specific deployment 

**Note** AWS Manual deployment is an interim solution and GitHub Actions based deployment to replace the manual deployment in future release

### Azure Deployment

Refer [azure-ado-beta](https://github.com/energy-dsi/dpn-integration-playbook/tree/main/Docs/03-dpn-application-deployment/azure-ado-beta) for Azure specific deployment

---
## Features

- **Automated Certificate Renewal** — Monitors certificate validity against a configurable threshold and triggers renewal when approaching expiry
- **Intermediate CA Management** — Automatically fetches and refreshes the intermediate CA from the Management Node before it expires
- **PKCS#12 KeyStore/TrustStore Generation** — Produces `keystore.p12` and `truststore.p12` from PEM artifacts stored in Vault
- **Atomic Filesystem Writes** — Uses temp-file-then-rename to prevent partial writes on crash
- **HashiCorp Vault Integration** — Persists all cryptographic material (key pairs, certificates, CA chains, passwords) to Vault KV v2
- **mTLS-Secured Communication** — All outbound HTTP calls use mutual TLS with configurable JKS keystores
- **OAuth2 Client Credentials Flow** — Authenticates against an IdP with Caffeine-cached tokens and automatic refresh
- **SBOM Generation** — CycloneDX Maven plugin produces a Software Bill of Materials at build time

---

## Technology Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Runtime | Java (JDK) | 21 |
| Framework | Spring Boot | 3.5.5 |
| Cloud | Spring Cloud (Vault) | 2025.0.0 |
| Cryptography | Bouncy Castle | 1.83 |
| HTTP Client | Apache HttpClient 5 | managed |
| Caching | Caffeine | managed |
| DTO Mapping | ModelMapper | 3.2.0 |
| Build | Apache Maven | 3.9+ |
| Formatting | Spotless (Palantir) | 2.46.1 |
| Coverage | JaCoCo | 0.8.13 |
| SBOM | CycloneDX | 2.9.1 |
| Testing | JUnit 5 + Mockito | 5.10.0 |

---

## Public Funding Acknowledgment

This repository has been developed with public funding as part of the Data Sharing Infrastructure (DSI), a UK Government initiative. DSI, alongside its partners, has invested in this work to advance open, secure, and reusable digital twin technologies for any organisation, whether from the public or private sector, irrespective of size.

## Licensing

This repository contains both source code and documentation, which are covered by different licenses:  
- **Code:** Licensed under the [Apache License 2.0](./LICENSE.md).
- **Documentation:** Licensed under the [Open Government Licence v3.0](./OGL_LICENSE.md).

By contributing to this repository, you agree that your contributions will be licenced under these terms.

See [`LICENSE.md`](./LICENSE.md), [`OGL_LICENSE.md`](./OGL_LICENSE.md) and [`NOTICE.md`](./NOTICE.md) for details.

## Security and Responsible Disclosure

We take security seriously. If you believe you have found a security vulnerability in this repository, please follow our responsible disclosure process outlined in [SECURITY.md](./SECURITY.md).

## Contributing

We welcome contributions that align with the Programme's objectives. Please read our [CONTRIBUTING.md](./CONTRIBUTING.md) guidelines before submitting pull requests.

## Acknowledgements  
This repository has benefited from collaboration with various organisations. For a list of acknowledgments, see [ACKNOWLEDGEMENTS.md](./ACKNOWLEDGEMENTS.md).  

## Support and Contact

For questions, feedback, or support requests:

- Contact DSI team via email to [dsi@neso.energy](mailto:dsi@neso.energy)

## Maintained by the National Energy System Operator (NESO)

Copyright 2026 NESO and the Crown.  This work is licensed under the Open Government Licence 3.0 (OGL). This work has been developed by NESO using content licensed by the Department for Business and Trade (UK) under the OGL.   
 
Licensed under the Open Government Licence v3.0.

For full licensing terms, [OGL_LICENSE.md](./OGL_LICENSE.md)