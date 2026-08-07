/*
 * SPDX-License-Identifier: Apache-2.0
 * © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally
 * attributed to the Department for Business and Trade (UK) as the governing entity.
 */

package uk.gov.dbt.ndtp.federator.certificate.manager.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates the configured Vault authentication method at startup and logs which method is
 * active.
 * <p>
 *     {@code spring.cloud.vault.authentication} is config-driven and may be either
 *     {@code token} (a pre-issued Vault token, e.g. a root token, supplied via
 *     {@code spring.cloud.vault.token}) or {@code approle} (a {@code role_id}/{@code secret_id}
 *     pair, supplied via {@code spring.cloud.vault.app-role.role-id} and
 *     {@code spring.cloud.vault.app-role.secret-id}).
 * </p>
 * <p>
 *     Spring Cloud Vault's built-in {@code AppRoleClientAuthenticationProvider} performs the
 *     actual AppRole login and the {@code LifecycleAwareSessionManager} renews the resulting
 *     token automatically - no custom {@code ClientAuthentication} bean is required. This
 *     validator simply fails fast with a clear message if the chosen method is missing its
 *     required credentials, rather than surfacing an opaque Vault 4xx error later.
 * </p>
 */
@Slf4j
@Component
public class VaultAuthStartupValidator {

    private final String authMethod;
    private final String approleRoleId;
    private final String approleSecretId;
    private final String approlePath;
    private final String vaultToken;

    public VaultAuthStartupValidator(
            @Value("${spring.cloud.vault.authentication:token}") String authMethod,
            @Value("${spring.cloud.vault.app-role.role-id:}") String approleRoleId,
            @Value("${spring.cloud.vault.app-role.secret-id:}") String approleSecretId,
            @Value("${spring.cloud.vault.app-role.app-role-path:approle}") String approlePath,
            @Value("${spring.cloud.vault.token:}") String vaultToken) {
        this.authMethod = authMethod;
        this.approleRoleId = approleRoleId;
        this.approleSecretId = approleSecretId;
        this.approlePath = approlePath;
        this.vaultToken = vaultToken;
    }

    @PostConstruct
    void validate() {
        String normalised = authMethod == null ? "" : authMethod.trim().toUpperCase();

        switch (normalised) {
            case "APPROLE" -> validateAppRole();
            case "TOKEN", "" -> validateToken();
            default -> log.info("Vault authentication method configured as '{}'", authMethod);
        }
    }

    private void validateAppRole() {
        if (isBlank(approleRoleId) || isBlank(approleSecretId)) {
            throw new IllegalStateException(
                    "spring.cloud.vault.authentication is 'approle' but "
                            + "spring.cloud.vault.app-role.role-id and/or "
                            + "spring.cloud.vault.app-role.secret-id are not set. "
                            + "Both must be provided (e.g. via VAULT_APPROLE_ROLE_ID / VAULT_APPROLE_SECRET_ID "
                            + "environment variables, or mounted as a Kubernetes Secret via Spring Boot's "
                            + "configtree support).");
        }
        log.info("Vault authentication method: APPROLE (mount path '{}')", approlePath);
    }

    private void validateToken() {
        if (isBlank(vaultToken)) {
            log.warn(
                    "Vault authentication method is TOKEN but spring.cloud.vault.token is empty; "
                            + "Vault-backed configuration and secrets will be unavailable until a token is "
                            + "configured.");
            return;
        }
        log.info("Vault authentication method: TOKEN");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
