/*
 * SPDX-License-Identifier: Apache-2.0
 * © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally
 * attributed to the Department for Business and Trade (UK) as the governing entity.
 */

package uk.gov.dbt.ndtp.federator.certificate.manager.service.idp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import uk.gov.dbt.ndtp.federator.certificate.manager.client.MtlsHttpClientBuilder;
import uk.gov.dbt.ndtp.federator.certificate.manager.exception.OAuth2TokenException;
import uk.gov.dbt.ndtp.federator.certificate.manager.model.dto.CreateKeyResponseDTO;
import uk.gov.dbt.ndtp.federator.certificate.manager.service.pki.VaultSecretProvider;
import uk.gov.dbt.ndtp.federator.certificate.manager.service.pki.cryptography.PemUtil;

/**
 * Unit tests for {@link PrivateJwtTokenServiceImpl}.
 *
 * <p>The private key and leaf certificate are now read directly from Vault (no keystore file on
 * disk — the Azure SMB file share has been removed). Tests stub {@link VaultSecretProvider} with
 * genuine PEM material so key-loading and kid-derivation run against real certificate bytes.</p>
 */
class PrivateJwtTokenServiceImplTest {

    private static final String TOKEN_URI = "https://keycloak.example.com/realms/test/protocol/openid-connect/token";
    private static final String CLIENT_ID = "cert-manager-client";

    private MtlsHttpClientBuilder mockHttpClientBuilder;
    private VaultSecretProvider mockVaultSecretProvider;

    private String leafPem;
    private String privateKeyPem;
    private String publicKeyPem;
    private X509Certificate leafCert;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair caKeyPair = kpg.generateKeyPair();
        KeyPair leafKeyPair = kpg.generateKeyPair();

        X500Name caName = new X500Name("CN=CA");
        X500Name leafName = new X500Name("CN=federator.dpn.local");
        leafCert = createCert(leafName, caName, leafKeyPair.getPublic(), caKeyPair.getPrivate());

        leafPem = PemUtil.toPem("CERTIFICATE", leafCert.getEncoded());
        privateKeyPem = PemUtil.toPem("PRIVATE KEY", leafKeyPair.getPrivate().getEncoded());
        publicKeyPem = PemUtil.toPem("PUBLIC KEY", leafKeyPair.getPublic().getEncoded());

        mockHttpClientBuilder = mock(MtlsHttpClientBuilder.class);
        mockVaultSecretProvider = mock(VaultSecretProvider.class);
        when(mockHttpClientBuilder.buildHttpClient()).thenReturn(mock(CloseableHttpClient.class));
    }

    private void stubVaultWithMaterial() {
        when(mockVaultSecretProvider.getCertificate()).thenReturn(leafPem);
        when(mockVaultSecretProvider.getKeyPair())
                .thenReturn(CreateKeyResponseDTO.builder()
                        .privateKeyPem(privateKeyPem)
                        .publicKeyPem(publicKeyPem)
                        .build());
    }

    private PrivateJwtTokenServiceImpl buildServiceWithAlgo(String algorithm) {
        return new PrivateJwtTokenServiceImpl(
                mockHttpClientBuilder, TOKEN_URI, CLIENT_ID, algorithm, mockVaultSecretProvider);
    }

    private PrivateJwtTokenServiceImpl buildServiceWithMockRestClient(RestClient mockRestClient) {
        return new PrivateJwtTokenServiceImpl(
                mockHttpClientBuilder, TOKEN_URI, CLIENT_ID, "RS256", mockVaultSecretProvider) {
            @Override
            protected RestClient buildRestClient(CloseableHttpClient httpClient) {
                return mockRestClient;
            }
        };
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @Test
    void constructor_succeedsWithValidConfig() {
        assertDoesNotThrow(() -> buildServiceWithAlgo("RS256"));
    }

    @Test
    void constructor_throws_whenAlgorithmUnsupported() {
        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, () -> buildServiceWithAlgo("HS256"));
        assertTrue(ex.getMessage().contains("Unsupported JWT signing algorithm"));
    }

    // -----------------------------------------------------------------------
    // Vault material loading
    // -----------------------------------------------------------------------

    @Test
    void getAccessToken_throws_whenCertificateMissingInVault() {
        when(mockVaultSecretProvider.getCertificate()).thenReturn(null);

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mock(RestClient.class));

        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("No certificate found in Vault")
                || ex.getMessage().contains("Error retrieving OAuth2 token"));
    }

    @Test
    void getAccessToken_throws_whenPrivateKeyMissingInVault() {
        when(mockVaultSecretProvider.getCertificate()).thenReturn(leafPem);
        when(mockVaultSecretProvider.getKeyPair())
                .thenReturn(CreateKeyResponseDTO.builder().build());

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mock(RestClient.class));

        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("No private key found in Vault")
                || ex.getMessage().contains("Error retrieving OAuth2 token"));
    }

    @Test
    void loadKeystoreContentsFromVault_succeeds() {
        stubVaultWithMaterial();
        PrivateJwtTokenServiceImpl service = buildServiceWithAlgo("RS256");

        PrivateJwtTokenServiceImpl.KeystoreContents contents = service.loadKeystoreContentsFromVault();

        assertNotNull(contents.privateKey());
        assertNotNull(contents.kid());
        assertFalse(contents.kid().isBlank());
    }

    // -----------------------------------------------------------------------
    // getAccessToken — response handling
    // -----------------------------------------------------------------------

    @Test
    void getAccessToken_returnsToken_onSuccess() {
        stubVaultWithMaterial();
        RestClient mockRestClient = buildSuccessRestClientWithBody(Map.of("access_token", "tok-123", "expires_in", 300));

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mockRestClient);

        TokenResponse response = service.getAccessToken();
        assertNotNull(response);
        assertEquals("tok-123", response.getAccessToken());
        assertEquals(300, response.getExpiresIn());
    }

    @Test
    void getAccessToken_throws_whenResponseMissingAccessToken() {
        stubVaultWithMaterial();
        RestClient mockRestClient = buildSuccessRestClientWithBody(Map.of("token_type", "Bearer"));

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mockRestClient);

        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("Missing access_token in response")
                || ex.getMessage().contains("Error retrieving OAuth2 token via private_key_jwt"));
    }

    @Test
    void getAccessToken_throws_whenRestClientThrows() {
        stubVaultWithMaterial();
        RestClient mockRestClient = buildThrowingRestClient(
                new org.springframework.web.client.RestClientException("connection refused"));

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mockRestClient);

        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("Error retrieving OAuth2 token via private_key_jwt"));
    }

    // -----------------------------------------------------------------------
    // deriveKidFromCertificate
    // -----------------------------------------------------------------------

    @Test
    void deriveKidFromCertificate_matchesManualSha256Thumbprint() throws Exception {
        String expected = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(leafCert.getEncoded()));

        assertEquals(expected, PrivateJwtTokenServiceImpl.deriveKidFromCertificate(leafCert));
    }

    @Test
    void deriveKidFromCertificate_isDeterministic() {
        assertEquals(
                PrivateJwtTokenServiceImpl.deriveKidFromCertificate(leafCert),
                PrivateJwtTokenServiceImpl.deriveKidFromCertificate(leafCert));
    }

    // -----------------------------------------------------------------------
    // Mock RestClient helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private RestClient buildSuccessRestClientWithBody(Object body) {
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mockRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.body(any(org.springframework.util.MultiValueMap.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(body);

        return mockRestClient;
    }

    @SuppressWarnings("unchecked")
    private RestClient buildThrowingRestClient(RuntimeException exception) {
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mockRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenThrow(exception);

        return mockRestClient;
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
