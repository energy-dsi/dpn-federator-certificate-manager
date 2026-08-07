/*
 * SPDX-License-Identifier: Apache-2.0
 * © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally
 * attributed to the Department for Business and Trade (UK) as the governing entity.
 */

package uk.gov.dbt.ndtp.federator.certificate.manager.client;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;
import uk.gov.dbt.ndtp.federator.certificate.manager.config.CertificateProperties;
import uk.gov.dbt.ndtp.federator.certificate.manager.exception.RestClientConfigurationException;
import uk.gov.dbt.ndtp.federator.certificate.manager.model.dto.CreateKeyResponseDTO;
import uk.gov.dbt.ndtp.federator.certificate.manager.service.pki.KeyStoreService;
import uk.gov.dbt.ndtp.federator.certificate.manager.service.pki.VaultSecretProvider;

/**
 * Builds an mTLS-enabled HTTP client whose keystore and truststore are constructed
 * <strong>in memory</strong> from the certificate material stored in Vault.
 * <p>
 *     No keystore/truststore files are read from disk — the Azure SMB file share previously
 *     shared between the certificate manager and the federator has been removed. The identity
 *     (leaf certificate + private key + CA chain) and the trust anchors (CA chain) are fetched
 *     from Vault on demand and assembled into transient {@link KeyStore} instances protected by
 *     an ephemeral, process-local password that never leaves memory.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MtlsHttpClientBuilder {

    private static final String PKCS_12 = "PKCS12";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VaultSecretProvider vaultSecretProvider;
    private final KeyStoreService keyStoreService;
    private final CertificateProperties certificateProperties;

    /**
     * Builds a connection manager whose SSL context is derived from Vault-held certificate
     * material assembled into in-memory key/trust stores.
     */
    public BasicHttpClientConnectionManager buildConnectionManager() {
        try {
            char[] ephemeralPassword = generateEphemeralPassword();
            String passwordStr = new String(ephemeralPassword);

            String certificatePem = vaultSecretProvider.getCertificate();
            CreateKeyResponseDTO keyPair = vaultSecretProvider.getKeyPair();
            List<String> caChain = resolveCaChain();

            if (certificatePem == null || keyPair == null || keyPair.getPrivateKeyPem() == null) {
                throw new RestClientConfigurationException(
                        "Certificate or key pair missing in Vault; cannot build mTLS client");
            }

            String alias = certificateProperties.getIdentity().getKeystoreAlias();

            byte[] keyStoreBytes = keyStoreService.createKeyStore(
                    keyPair.getPrivateKeyPem(), certificatePem, caChain, passwordStr, alias);
            KeyStore keyStore = loadInMemoryKeyStore(keyStoreBytes, ephemeralPassword);

            byte[] trustStoreBytes = keyStoreService.createTrustStore(caChain, passwordStr);
            KeyStore trustStore = loadInMemoryKeyStore(trustStoreBytes, ephemeralPassword);

            logTrustStoreEntries(trustStore);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, ephemeralPassword);

            X509ExtendedKeyManager originalKeyManager = (X509ExtendedKeyManager) kmf.getKeyManagers()[0];
            KeyManager loggingKeyManager = new LoggingKeyManager(originalKeyManager);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[] {loggingKeyManager}, tmf.getTrustManagers(), null);
            Lookup<TlsSocketStrategy> registry = RegistryBuilder.<TlsSocketStrategy>create()
                    .register("https", new DefaultClientTlsStrategy(sslContext))
                    .build();

            ConnectionConfig connectionConfig = ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.of(10, TimeUnit.SECONDS))
                    .setSocketTimeout(Timeout.of(30, TimeUnit.SECONDS))
                    .setTimeToLive(TimeValue.ofHours(1))
                    .build();

            BasicHttpClientConnectionManager connectionManager = BasicHttpClientConnectionManager.create(registry);
            connectionManager.setConnectionConfig(connectionConfig);
            return connectionManager;
        } catch (RestClientConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new RestClientConfigurationException("Failed to configure mTLS HttpClient", e);
        }
    }

    /**
     * Builds an HTTP client configured with the in-memory mTLS connection manager.
     *
     * @return an instance of {@link CloseableHttpClient}
     */
    public CloseableHttpClient buildHttpClient() {
        BasicHttpClientConnectionManager connectionManager = buildConnectionManager();
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.of(30, TimeUnit.SECONDS))
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .build();
    }

    /**
     * Resolves the CA chain used for both the keystore chain and the truststore trust anchors,
     * falling back to the signing (intermediate) CA when no explicit chain is stored.
     */
    private List<String> resolveCaChain() {
        List<String> caChain = vaultSecretProvider.getCaChain();
        if (caChain != null && !caChain.isEmpty()) {
            return caChain;
        }
        String intermediateCa = vaultSecretProvider.getIntermediateCa();
        if (intermediateCa != null && !intermediateCa.isBlank()) {
            log.info("CA chain is empty in Vault; falling back to signing CA for the mTLS truststore");
            List<String> fallback = new ArrayList<>();
            fallback.add(intermediateCa);
            return fallback;
        }
        return new ArrayList<>();
    }

    private KeyStore loadInMemoryKeyStore(byte[] bytes, char[] password) {
        try {
            KeyStore ks = KeyStore.getInstance(PKCS_12);
            ks.load(new ByteArrayInputStream(bytes), password);
            return ks;
        } catch (Exception e) {
            throw new RestClientConfigurationException("Failed to load in-memory keystore", e);
        }
    }

    private void logTrustStoreEntries(KeyStore trustStore) throws Exception {
        Enumeration<String> aliases = trustStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = trustStore.getCertificate(alias);
            if (cert instanceof X509Certificate x509) {
                log.info("Truststore entry: {} -> {}", alias, x509.getSubjectX500Principal());
            }
        }
    }

    private char[] generateEphemeralPassword() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes).toCharArray();
    }
}
