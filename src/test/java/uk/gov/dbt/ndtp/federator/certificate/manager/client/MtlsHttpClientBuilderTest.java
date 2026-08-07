/*
 * SPDX-License-Identifier: Apache-2.0
 * © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally
 * attributed to the Department for Business and Trade (UK) as the governing entity.
 */

package uk.gov.dbt.ndtp.federator.certificate.manager.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dbt.ndtp.federator.certificate.manager.config.CertificateProperties;
import uk.gov.dbt.ndtp.federator.certificate.manager.exception.RestClientConfigurationException;
import uk.gov.dbt.ndtp.federator.certificate.manager.model.dto.CreateKeyResponseDTO;
import uk.gov.dbt.ndtp.federator.certificate.manager.service.pki.KeyStoreService;
import uk.gov.dbt.ndtp.federator.certificate.manager.service.pki.VaultSecretProvider;
import uk.gov.dbt.ndtp.federator.certificate.manager.service.pki.cryptography.PemUtil;

/**
 * Verifies that the mTLS client is assembled entirely from Vault-held certificate material with
 * no keystore/truststore files on disk (the Azure SMB file share has been removed).
 */
@ExtendWith(MockitoExtension.class)
class MtlsHttpClientBuilderTest {

    @Mock
    VaultSecretProvider vaultSecretProvider;

    @Mock
    CertificateProperties certificateProperties;

    MtlsHttpClientBuilder builder;

    private String caPem;
    private String leafPem;
    private String privateKeyPem;
    private String publicKeyPem;

    @BeforeEach
    void setUp() throws Exception {
        builder = new MtlsHttpClientBuilder(vaultSecretProvider, new KeyStoreService(), certificateProperties);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair caKeyPair = kpg.generateKeyPair();
        KeyPair leafKeyPair = kpg.generateKeyPair();

        X500Name caName = new X500Name("CN=CA");
        X500Name leafName = new X500Name("CN=federator.dpn.local");
        X509Certificate caCert = createCert(caName, caName, caKeyPair.getPublic(), caKeyPair.getPrivate());
        X509Certificate leafCert = createCert(leafName, caName, leafKeyPair.getPublic(), caKeyPair.getPrivate());

        caPem = PemUtil.toPem("CERTIFICATE", caCert.getEncoded());
        leafPem = PemUtil.toPem("CERTIFICATE", leafCert.getEncoded());
        privateKeyPem = PemUtil.toPem("PRIVATE KEY", leafKeyPair.getPrivate().getEncoded());
        publicKeyPem = PemUtil.toPem("PUBLIC KEY", leafKeyPair.getPublic().getEncoded());
    }

    private void stubVault() {
        when(certificateProperties.getIdentity()).thenReturn(identityWithAlias("federator"));
        when(vaultSecretProvider.getCertificate()).thenReturn(leafPem);
        when(vaultSecretProvider.getKeyPair())
                .thenReturn(CreateKeyResponseDTO.builder()
                        .privateKeyPem(privateKeyPem)
                        .publicKeyPem(publicKeyPem)
                        .build());
        when(vaultSecretProvider.getCaChain()).thenReturn(List.of(caPem));
    }

    @Test
    void buildConnectionManager_fromVaultMaterial() {
        stubVault();
        BasicHttpClientConnectionManager connectionManager = builder.buildConnectionManager();
        assertNotNull(connectionManager);
    }

    @Test
    void buildHttpClient_fromVaultMaterial() {
        stubVault();
        CloseableHttpClient httpClient = builder.buildHttpClient();
        assertNotNull(httpClient);
    }

    @Test
    void buildConnectionManager_failsWhenCertificateMissing() {
        when(vaultSecretProvider.getCertificate()).thenReturn(null);
        when(vaultSecretProvider.getKeyPair())
                .thenReturn(CreateKeyResponseDTO.builder()
                        .privateKeyPem(privateKeyPem)
                        .build());
        when(vaultSecretProvider.getCaChain()).thenReturn(List.of(caPem));
        assertThrows(RestClientConfigurationException.class, () -> builder.buildConnectionManager());
    }

    private static CertificateProperties.Identity identityWithAlias(String alias) {
        CertificateProperties.Identity identity = new CertificateProperties.Identity();
        identity.setKeystoreAlias(alias);
        return identity;
    }

    private X509Certificate createCert(X500Name subject, X500Name issuer, PublicKey pubKey, PrivateKey privKey)
            throws Exception {
        long now = System.currentTimeMillis();
        Date start = new Date(now);
        Date end = new Date(now + 1000000);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(privKey);
        X509v3CertificateBuilder certBuilder =
                new JcaX509v3CertificateBuilder(issuer, BigInteger.valueOf(now), start, end, subject, pubKey);
        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
    }
}
