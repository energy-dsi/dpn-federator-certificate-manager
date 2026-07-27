/*
 * SPDX-License-Identifier: Apache-2.0
 * © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally
 * attributed to the Department for Business and Trade (UK) as the governing entity.
 */

package uk.gov.dbt.ndtp.federator.certificate.manager.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VaultAuthStartupValidatorTest {

    @Test
    void validate_succeeds_whenTokenMethodWithTokenConfigured() {
        VaultAuthStartupValidator validator = new VaultAuthStartupValidator("token", "", "", "approle", "root-token");

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_logsWarning_whenTokenMethodWithoutToken() {
        VaultAuthStartupValidator validator = new VaultAuthStartupValidator("token", "", "", "approle", "");

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_succeeds_whenApproleMethodWithCredentials() {
        VaultAuthStartupValidator validator =
                new VaultAuthStartupValidator("approle", "role-id", "secret-id", "approle", "");

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_throws_whenApproleMethodMissingRoleId() {
        VaultAuthStartupValidator validator =
                new VaultAuthStartupValidator("approle", "", "secret-id", "approle", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("role-id"));
    }

    @Test
    void validate_throws_whenApproleMethodMissingSecretId() {
        VaultAuthStartupValidator validator =
                new VaultAuthStartupValidator("approle", "role-id", "", "approle", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("secret-id"));
    }

    @Test
    void validate_isCaseInsensitive_forApprole() {
        VaultAuthStartupValidator validator =
                new VaultAuthStartupValidator("APPROLE", "role-id", "secret-id", "approle", "");

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_defaultsToTokenBehaviour_whenAuthMethodBlank() {
        VaultAuthStartupValidator validator = new VaultAuthStartupValidator("", "", "", "approle", "root-token");

        assertDoesNotThrow(validator::validate);
    }
}
