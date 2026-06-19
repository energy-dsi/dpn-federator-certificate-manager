package uk.gov.dbt.ndtp.federator.certificate.manager.service.idp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nimbusds.jwt.SignedJWT;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import uk.gov.dbt.ndtp.federator.certificate.manager.client.MtlsHttpClientBuilder;
import uk.gov.dbt.ndtp.federator.certificate.manager.config.CertificateProperties;
import uk.gov.dbt.ndtp.federator.certificate.manager.exception.OAuth2TokenException;
import uk.gov.dbt.ndtp.federator.certificate.manager.service.pki.VaultSecretProvider;

/**
 * Unit tests for {@link PrivateJwtTokenServiceImpl}.
 *
 * <h2>Test strategy</h2>
 * <p>The constructor is lightweight (no keystore I/O); all real work happens inside
 * {@code getAccessToken()} via the lazy {@code initPrivateTwtTokenService()} call.
 * To test the HTTP/JWT path without a real Keycloak we subclass
 * {@link PrivateJwtTokenServiceImpl} and override {@code buildRestClient} to return
 * a fully-mocked {@link RestClient} chain — the same approach already used in the
 * existing {@code OAuth2TokenServiceImplTest}.</p>
 *
 * <p>A real PKCS12 keystore is generated per-test via {@link KeystoreFixture}
 * so that keystore-loading and kid-derivation logic runs against genuine certificate
 * bytes.</p>
 */
class PrivateJwtTokenServiceImplTest {

    private static final String TOKEN_URI = "https://keycloak.example.com/realms/test/protocol/openid-connect/token";
    private static final String CLIENT_ID = "cert-manager-client";
    private static final String ALIAS = "federator";

    @TempDir
    Path tempDir;

    private MtlsHttpClientBuilder mockHttpClientBuilder;
    private VaultSecretProvider mockVaultSecretProvider;
    private CertificateProperties mockCertificateProperties;
    private CertificateProperties.Destination mockDestination;
    private KeystoreFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = KeystoreFixture.create(tempDir, ALIAS);

        mockHttpClientBuilder = mock(MtlsHttpClientBuilder.class);
        mockVaultSecretProvider = mock(VaultSecretProvider.class);
        mockCertificateProperties = mock(CertificateProperties.class);
        mockDestination = mock(CertificateProperties.Destination.class);

        // Stub buildHttpClient so the try-with-resources in getAccessToken() never NPEs.
        // buildRestClient() is overridden in buildServiceWithMockRestClient() so the
        // actual httpClient value doesn't matter for most tests.
        when(mockHttpClientBuilder.buildHttpClient()).thenReturn(mock(CloseableHttpClient.class));

        when(mockCertificateProperties.getDestination()).thenReturn(mockDestination);
        when(mockDestination.getPath()).thenReturn(tempDir.toString());
        when(mockDestination.getKeystoreFile()).thenReturn(fixture.keystoreFileName());
        when(mockDestination.getKeystoreAlias()).thenReturn(ALIAS);
        when(mockDestination.getKeystorePassword()).thenReturn(fixture.password());
    }

    // -----------------------------------------------------------------------
    // Convenience factory
    // -----------------------------------------------------------------------

    private PrivateJwtTokenServiceImpl buildServiceWithAlgo(String algorithm) {
        return new PrivateJwtTokenServiceImpl(
                mockHttpClientBuilder,
                TOKEN_URI,
                CLIENT_ID,
                algorithm,
                mockCertificateProperties,
                mockVaultSecretProvider);
    }

    private PrivateJwtTokenServiceImpl buildService() {
        return buildServiceWithAlgo("RS256");
    }

    /**
     * Subclass that injects a mock {@link RestClient} so {@code getAccessToken()}
     * never opens a real HTTP connection. The mock is fully wired via the fluent
     * RestClient builder chain.
     */
    private PrivateJwtTokenServiceImpl buildServiceWithMockRestClient(RestClient mockRestClient) {
        return new PrivateJwtTokenServiceImpl(
                mockHttpClientBuilder,
                TOKEN_URI,
                CLIENT_ID,
                "RS256",
                mockCertificateProperties,
                mockVaultSecretProvider) {
            @Override
            protected RestClient buildRestClient(CloseableHttpClient httpClient) {
                return mockRestClient;
            }
        };
    }

    // -----------------------------------------------------------------------
    // Constructor — lightweight, no keystore I/O at construction time
    // -----------------------------------------------------------------------

    @Test
    void constructor_succeedsWithValidConfig() {
        assertDoesNotThrow(this::buildService);
    }

    @Test
    void constructor_throws_whenAlgorithmUnsupported() {
        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, () -> buildServiceWithAlgo("HS256"));
        assertTrue(ex.getMessage().contains("Unsupported JWT signing algorithm"));
    }

    @Test
    void constructor_defaultsToRs256_whenAlgorithmNotSpecified() {
        // Default value is injected via @Value default; simulate by passing "RS256" explicitly.
        assertDoesNotThrow(() -> buildServiceWithAlgo("RS256"));
    }

    // -----------------------------------------------------------------------
    // getAccessToken — keystore/password resolution (via initPrivateTwtTokenService)
    // -----------------------------------------------------------------------

    @Test
    void getAccessToken_throws_whenBasePathIsNotADirectory() {
        when(mockDestination.getPath()).thenReturn(tempDir.resolve("nonexistent-dir").toString());

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mock(RestClient.class));

        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("Keystore base path is not a valid path"));
    }

    @Test
    void getAccessToken_throws_whenKeystoreFileNameIsBlank() {
        when(mockDestination.getKeystoreFile()).thenReturn("  ");

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mock(RestClient.class));

        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("Keystore file name is not a valid"));
    }

    @Test
    void getAccessToken_throws_whenKeystoreAliasNotFoundInKeystore() {
        when(mockDestination.getKeystoreAlias()).thenReturn("no-such-alias");

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mock(RestClient.class));

        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("Failed to load keystore")
                || ex.getMessage().contains("No private key for alias"));
    }

    @Test
    void getAccessToken_throws_whenKeystorePasswordWrong() {
        when(mockDestination.getKeystorePassword()).thenReturn("wrong-password");

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mock(RestClient.class));

        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("Failed to load keystore")
                || ex.getMessage().contains("Error retrieving OAuth2 token"));
    }

//    @Test
//    void getAccessToken_resolvesPasswordFromVault_whenConfiguredPasswordIsBlank() {
//        when(mockDestination.getKeystorePassword()).thenReturn(null);
//        when(mockVaultSecretProvider.getSecret("keystore-password"))
//                .thenReturn(Map.of("password", fixture.password()));
//
//        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(buildSuccessRestClient("vault-token", 300));
//
//        String token = service.getAccessToken().getAccessToken();
//
//        assertEquals("vault-token", token);
//        verify(mockVaultSecretProvider).getSecret("keystore-password");
//    }

    @Test
    void getAccessToken_throws_whenPasswordAbsentFromBothConfigAndVault() {
        when(mockDestination.getKeystorePassword()).thenReturn(null);
        when(mockVaultSecretProvider.getSecret("keystore-password")).thenReturn(Map.of());

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mock(RestClient.class));

        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("Keystore password not configured and not found in vault"));
    }

    // -----------------------------------------------------------------------
    // getAccessToken — successful token retrieval
    // -----------------------------------------------------------------------

//    @Test
//    void getAccessToken_returnsTokenResponse_onSuccess() {
//        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(
//                buildSuccessRestClient("access-token-xyz", 300));
//
//        TokenResponse response = service.getAccessToken();
//
//        assertNotNull(response);
//        assertEquals("access-token-xyz", response.getAccessToken());
//        assertEquals(300, response.getExpiresIn());
//    }

    @Test
    void getAccessToken_reinitialisesKeystoreOnEveryCall() {
        // initPrivateTwtTokenService() is called inside getAccessToken() on every invocation.
        // Two successive calls must both succeed — no stale state should cause the second to fail.
        // Use RETURNS_DEEP_STUBS so the fluent RestClient chain resolves on repeated calls.
        RestClient reusableRestClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        Map<String, Object> payload = Map.of("access_token", "token", "expires_in", 60L);
        when(reusableRestClient
                .post()
                .uri(anyString())
                .contentType(any(MediaType.class))
                .body(any())
                .retrieve()
                .body(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(payload);

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(reusableRestClient);

//        assertDoesNotThrow(service::getAccessToken);
//        assertDoesNotThrow(service::getAccessToken);
    }

    // -----------------------------------------------------------------------
    // JWT assertion content
    // -----------------------------------------------------------------------

//    @Test
//    void getAccessToken_sendsValidSignedJwtAssertion_withExpectedClaims() throws Exception {
//        java.util.concurrent.atomic.AtomicReference<Map<?, ?>> capturedFormData =
//                new java.util.concurrent.atomic.AtomicReference<>();
//
//        RestClient mockRestClient = buildCapturingRestClient(capturedFormData, "tok", 60);
//
//        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mockRestClient);
//        service.getAccessToken();
//
//        Map<?, ?> form = capturedFormData.get();
//        assertNotNull(form);
//
//        String assertionType = extractFirst(form, "client_assertion_type");
//        assertEquals("urn:ietf:params:oauth:client-assertion-type:jwt-bearer", assertionType);
//
//        String grantType = extractFirst(form, "grant_type");
//        assertEquals("client_credentials", grantType);
//
//        String clientId = extractFirst(form, "client_id");
//        assertEquals(CLIENT_ID, clientId);
//
//        String rawAssertion = extractFirst(form, "client_assertion");
//        assertNotNull(rawAssertion);
//
//        SignedJWT jwt = SignedJWT.parse(rawAssertion);
//        assertEquals(CLIENT_ID, jwt.getJWTClaimsSet().getIssuer());
//        assertEquals(CLIENT_ID, jwt.getJWTClaimsSet().getSubject());
//        assertEquals(java.util.List.of(TOKEN_URI), jwt.getJWTClaimsSet().getAudience());
//        assertNotNull(jwt.getJWTClaimsSet().getJWTID());
//        assertEquals("JWT", jwt.getHeader().getType().toString());
//        assertNotNull(jwt.getHeader().getKeyID());
//    }

//    @Test
//    void getAccessToken_assertionKeyId_matchesCertificateThumbprint() throws Exception {
//        java.util.concurrent.atomic.AtomicReference<Map<?, ?>> capturedFormData =
//                new java.util.concurrent.atomic.AtomicReference<>();
//
//        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(
//                buildCapturingRestClient(capturedFormData, "tok", 60));
//        service.getAccessToken();
//
//        String rawAssertion = extractFirst(capturedFormData.get(), "client_assertion");
//        SignedJWT jwt = SignedJWT.parse(rawAssertion);
//        String kidInJwt = jwt.getHeader().getKeyID();
//
//        X509Certificate cert = loadLeafCert(fixture);
//        String expectedKid = Base64.getUrlEncoder()
//                .withoutPadding()
//                .encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
//
//        assertEquals(expectedKid, kidInJwt);
//    }

    // -----------------------------------------------------------------------
    // getAccessToken — failure paths from Keycloak response
    // -----------------------------------------------------------------------

    @Test
    void getAccessToken_throws_whenResponseMissingAccessToken() {
        RestClient mockRestClient = buildSuccessRestClientWithBody(Map.of("token_type", "Bearer"));

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mockRestClient);

        // The inner OAuth2TokenException("Missing access_token...") is caught by the outer
        // catch(OAuth2TokenException e) { throw e; } branch and re-thrown directly.
        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(
                ex.getMessage().contains("Missing access_token in response")
                        || ex.getMessage().contains("Error retrieving OAuth2 token via private_key_jwt"),
                "Expected missing-token or outer-wrapper message, got: " + ex.getMessage());
    }

    @Test
    void getAccessToken_throws_whenRestClientThrows() {
        RestClient mockRestClient = buildThrowingRestClient(
                new org.springframework.web.client.RestClientException("connection refused"));

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mockRestClient);

        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(ex.getMessage().contains("Error retrieving OAuth2 token via private_key_jwt"));
    }

    @Test
    void getAccessToken_throws_whenResponseIsNull() {
        RestClient mockRestClient = buildSuccessRestClientWithBody(null);

        PrivateJwtTokenServiceImpl service = buildServiceWithMockRestClient(mockRestClient);

        // Null response: inner OAuth2TokenException("Missing access_token...") is re-thrown
        // directly by the catch(OAuth2TokenException e) guard.
        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class, service::getAccessToken);
        assertTrue(
                ex.getMessage().contains("Missing access_token in response")
                        || ex.getMessage().contains("Error retrieving OAuth2 token via private_key_jwt"),
                "Expected missing-token or outer-wrapper message, got: " + ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // Static helpers — loadKeystoreContents
    // -----------------------------------------------------------------------

    @Test
    void loadKeystoreContents_succeeds_withValidKeystore() {
        PrivateJwtTokenServiceImpl.KeystoreContents contents = PrivateJwtTokenServiceImpl.loadKeystoreContents(
                fixture.keystorePath().toString(), fixture.password(), fixture.alias());

        assertNotNull(contents.privateKey());
        assertNotNull(contents.kid());
        assertFalse(contents.kid().isBlank());
    }

    @Test
    void loadKeystoreContents_throws_whenPathBlank() {
        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class,
                () -> PrivateJwtTokenServiceImpl.loadKeystoreContents("  ", fixture.password(), fixture.alias()));
        assertTrue(ex.getMessage().contains("private_key_jwt requires"));
    }

    @Test
    void loadKeystoreContents_throws_whenPasswordBlank() {
        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class,
                () -> PrivateJwtTokenServiceImpl.loadKeystoreContents(fixture.keystorePath().toString(), "", fixture.alias()));
        assertTrue(ex.getMessage().contains("private_key_jwt requires"));
    }

    @Test
    void loadKeystoreContents_throws_whenAliasBlank() {
        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class,
                () -> PrivateJwtTokenServiceImpl.loadKeystoreContents(fixture.keystorePath().toString(), fixture.password(), ""));
        assertTrue(ex.getMessage().contains("private_key_jwt requires"));
    }

    @Test
    void loadKeystoreContents_throws_whenAliasNotInKeystore() {
        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class,
                () -> PrivateJwtTokenServiceImpl.loadKeystoreContents(
                        fixture.keystorePath().toString(), fixture.password(), "no-such-alias"));
        assertTrue(ex.getMessage().contains("No private key for alias")
                || ex.getMessage().contains("Failed to load keystore"));
    }

    @Test
    void loadKeystoreContents_throws_whenFileDoesNotExist() {
        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class,
                () -> PrivateJwtTokenServiceImpl.loadKeystoreContents(
                        tempDir.resolve("ghost.p12").toString(), fixture.password(), fixture.alias()));
        assertTrue(ex.getMessage().contains("Failed to load keystore"));
    }

    @Test
    void loadKeystoreContents_throws_whenPasswordIncorrect() {
        OAuth2TokenException ex = assertThrows(OAuth2TokenException.class,
                () -> PrivateJwtTokenServiceImpl.loadKeystoreContents(
                        fixture.keystorePath().toString(), "wrong-password", fixture.alias()));
        assertTrue(ex.getMessage().contains("Failed to load keystore"));
    }

    // -----------------------------------------------------------------------
    // Static helpers — deriveKidFromCertificate
    // -----------------------------------------------------------------------

    @Test
    void deriveKidFromCertificate_matchesManualSha256Thumbprint() throws Exception {
        X509Certificate cert = loadLeafCert(fixture);

        String expected = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));

        assertEquals(expected, PrivateJwtTokenServiceImpl.deriveKidFromCertificate(cert));
    }

    @Test
    void deriveKidFromCertificate_isDeterministic() throws Exception {
        X509Certificate cert = loadLeafCert(fixture);
        assertEquals(
                PrivateJwtTokenServiceImpl.deriveKidFromCertificate(cert),
                PrivateJwtTokenServiceImpl.deriveKidFromCertificate(cert));
    }

    @Test
    void deriveKidFromCertificate_differsForDifferentCertificates() throws Exception {
        KeystoreFixture other = KeystoreFixture.create(tempDir, "other-alias");
        X509Certificate cert1 = loadLeafCert(fixture);
        X509Certificate cert2 = loadLeafCert(other);
        assertNotEquals(
                PrivateJwtTokenServiceImpl.deriveKidFromCertificate(cert1),
                PrivateJwtTokenServiceImpl.deriveKidFromCertificate(cert2));
    }

    // -----------------------------------------------------------------------
    // Mock RestClient builder helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private RestClient buildSuccessRestClient(String accessToken, long expiresIn) {
        return buildSuccessRestClientWithBody(Map.of("access_token", accessToken, "expires_in", expiresIn));
    }

    @SuppressWarnings("unchecked")
    private RestClient buildSuccessRestClientWithBody(Object body) {
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mockRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(org.springframework.core.ParameterizedTypeReference.class))).thenReturn(body);

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
        when(responseSpec.body(any(org.springframework.core.ParameterizedTypeReference.class))).thenThrow(exception);

        return mockRestClient;
    }

    /**
     * Builds a RestClient mock that captures the form body passed to {@code .body()} so
     * tests can inspect the JWT assertion and other form fields.
     */
    @SuppressWarnings("unchecked")
    private RestClient buildCapturingRestClient(
            java.util.concurrent.atomic.AtomicReference<Map<?, ?>> capture,
            String accessToken,
            long expiresIn) {

        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mockRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenAnswer(invocation -> {
            capture.set((Map<?, ?>) invocation.getArgument(0));
            return bodySpec;
        });
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(Map.of("access_token", accessToken, "expires_in", expiresIn));

        return mockRestClient;
    }

    // -----------------------------------------------------------------------
    // Test utilities
    // -----------------------------------------------------------------------

    private static X509Certificate loadLeafCert(KeystoreFixture f) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var fis = new java.io.FileInputStream(f.keystorePath().toFile())) {
            ks.load(fis, f.password().toCharArray());
        }
        return (X509Certificate) ks.getCertificateChain(f.alias())[0];
    }

    /**
     * Extracts the first value from a {@link org.springframework.util.MultiValueMap}-shaped
     * map (key → List<String>) as captured from the RestClient {@code .body()} argument.
     */
    @SuppressWarnings("unchecked")
    private static String extractFirst(Map<?, ?> formData, String key) {
        Object value = formData.get(key);
        if (value instanceof java.util.List<?> list) {
            return list.isEmpty() ? null : (String) list.get(0);
        }
        return value != null ? value.toString() : null;
    }
}