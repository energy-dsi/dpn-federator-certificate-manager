# Changelog 

**Repository:** `dpn-federator-certificate-manager`  
**Description:** `Tracks all notable changes, version history, and roadmap toward 1.0.0 following Semantic Versioning.`  

---

All notable changes to this repository will be documented in this file.

This project follows **Semantic Versioning (SemVer)** ([semver.org](https://semver.org/)), using the format:

`[MAJOR].[MINOR].[PATCH]`
- **MAJOR** (`X.0.0`) – Incompatible API/feature changes that break backward compatibility.
- **MINOR** (`0.X.0`) – Backward-compatible new features, enhancements, or functionality changes.
- **PATCH** (`0.0.X`) – Backward-compatible bug fixes, security updates, or minor corrections.
- **Pre-release versions** – Use suffixes such as `-alpha`, `-beta`, `-rc.1` (e.g., `2.1.0-beta.1`).
- **Build metadata** – If needed, use `+build` (e.g., `2.1.0+20260314`).

---

## How to Update This Changelog

1. When making changes, update this file under the **Unreleased** section.
2. Before a new release, move changes from **Unreleased** to a new dated section with a version number.
3. Follow **Semantic Versioning** rules to categorise changes correctly.
4. If pre-release versions are used, clearly mark them as `-alpha`, `-beta`, or `-rc.X`.

---

## Release 1.0.0

- Automated DPN certificate renewal from DSM
- Signed certificate generation from DSM provided bootstrap package
- Mutual TLS authentication using signed certificate between DPN and DSM
- Storage of certificates in Hashicorp Vault
- SMB Based file share for storing keystore and truststore certificate files
- Private JWT Assertion Authentication between DPN and DSM
- Centralized logging to Open telemetry platform

---

## Maintained by the National Energy System Operator (NESO)

Copyright 2026 NESO and the Crown.  This work is licensed under the Open Government Licence 3.0 (OGL). This work has been developed by NESO using content licensed by the Department for Business and Trade (UK) under the OGL.   
 
Licensed under the Open Government Licence v3.0.

For full licensing terms, [OGL_LICENSE.md](./OGL_LICENSE.md)