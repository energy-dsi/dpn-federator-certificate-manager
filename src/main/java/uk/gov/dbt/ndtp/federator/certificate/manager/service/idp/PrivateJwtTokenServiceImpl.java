package uk.gov.dbt.ndtp.federator.certificate.manager.service.idp;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uk.gov.dbt.ndtp.federator.certificate.manager.client.MtlsHttpClientBuilder;
import uk.gov.dbt.ndtp.federator.certificate.manager.config.CertificateProperties;
import uk.gov.dbt.ndtp.federator.certificate.manager.exception.OAuth2TokenException;
import uk.gov.dbt.ndtp.federator.certificate.manager.service.pki.VaultSecretProvider;

/**
 * Service for requesting OAuth2 tokens from Keycloak using the
 * <b>private_key_jwt</b> client-authentication method (RFC 7523).
 *
 * <p>A short-lived, self-signed JWT assertion is built and signed with the
 * application's private key on every token request. Keycloak verifies the
 * assertion against the registered public certificate and issues an access
 * token — no client secret is transmitted.</p>
 *
 * <h2>Key-ID derivation</h2>
 * <p>The {@code kid} in the JWT assertion header is derived as the X.509
 * SHA-256 thumbprint ({@code x5t#S256}, RFC 7517 §4.9) of the leaf
 * certificate held in the PKCS12 keystore.  Keycloak computes the same value
 * when the certificate is uploaded, so no separate configuration property is
 * needed.  On certificate renewal cert-manager overwrites the keystore; the
 * new {@code kid} is picked up automatically on the next application start.</p>
 *
 * <h2>Required configuration ({@code application.yml})</h2>
 * <pre>
 *
 * application:
 *   oauth2:
 *     token-uri:           https://keycloak/realms/&lt;realm&gt;/protocol/openid-connect/token
 *     client-id:           &lt;client-id registered in Keycloak&gt;
 *     auth-mode:           private_key_jwt
 *     jwt-algorithm:       RS256
 *   certificate:
 *     destination:
 *       path:              Keystore file Base directory path
 *       keystore-file:     Filename of PKCS12 keystore file
 *       keystore-alias:    Alias for the identity key in the keystore
 * </pre>
 *
 * <p>Communication with Keycloak is secured via mTLS using the same
 * {@link MtlsHttpClientBuilder} as {@link OAuth2TokenServiceImpl}.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "application.oauth2.auth-mode", havingValue = "private_key_jwt")
public class PrivateJwtTokenServiceImpl implements OAuth2TokenService {

    /** RFC 7523 §2.2 — client_assertion_type form field value */
    private static final String CLIENT_ASSERTION_TYPE_VALUE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
    private static final String CLIENT_ASSERTION      = "client_assertion";
    private static final String GRANT_TYPE            = "grant_type";
    private static final String CLIENT_CREDENTIALS    = "client_credentials";
    private static final String CLIENT_ID             = "client_id";
    private static final String ACCESS_TOKEN          = "access_token";
    private static final String EXPIRES_IN            = "expires_in";
    private static final String PASSWORD = "password";
    private static final String PKCS_12 = "PKCS12";

    /** Assertion lifetime — Keycloak enforces jti-uniqueness to prevent replays. */
    private static final int ASSERTION_LIFETIME_SECONDS = 60;

    private final MtlsHttpClientBuilder httpClientBuilder;
    private final VaultSecretProvider vaultSecretProvider;
    private final String tokenUri;
    private final String clientId;
    private PrivateKey privateKey;
    private final JWSAlgorithm jwsAlgorithm;
    private final CertificateProperties.Destination config;

    /**
     * The {@code kid} derived once at startup from the keystore leaf certificate.
     * Must match the value Keycloak assigned when the certificate was registered.
     */
    private String keyId;

    /**
     * Constructs the PrivateJwtTokenServiceImpl.
     *
     * @param httpClientBuilder       builder that creates mTLS-configured {@link CloseableHttpClient} instances
     * @param tokenUri                Keycloak token endpoint URI
     * @param clientId                OAuth2 client identifier registered in Keycloak
     * @param algorithm               JWS signing algorithm (e.g. {@code RS256}, {@code ES256})
     * @param certificateProperties   certificate properties to extract destination props like keystore path, file and alias.
     * @param vaultSecretProvider     vault provider api to retrieve keystore password
     */
    public PrivateJwtTokenServiceImpl(
            MtlsHttpClientBuilder httpClientBuilder,
            @Value("${application.oauth2.token-uri}") String tokenUri,
            @Value("${application.oauth2.client-id}") String clientId,
            @Value("${application.oauth2.jwt-algorithm:RS256}") String algorithm,
            CertificateProperties certificateProperties,
            VaultSecretProvider vaultSecretProvider) {

        this.httpClientBuilder = httpClientBuilder;
        this.tokenUri  = tokenUri;
        this.clientId  = clientId;
        this.jwsAlgorithm = resolveAlgorithm(algorithm);
        this.vaultSecretProvider = vaultSecretProvider;
        this.config = certificateProperties.getDestination();

    }

    private void initPrivateTwtTokenService() throws OAuth2TokenException {
        String keystorePath = resolveKeystorePath(config);
        String keystorePassword = resolveKeystorePassword(config.getKeystorePassword(), "keystore-password");

        KeystoreContents ks = loadKeystoreContents(keystorePath, keystorePassword, config.getKeystoreAlias());
        this.privateKey = ks.privateKey();
        this.keyId      = ks.kid();

        log.info(
                "PrivateJwtTokenServiceImpl initialised. tokenUri='{}', clientId='{}', "
                        + "kid='{}' (derived from cert in keystore), algorithm='{}'",
                tokenUri, clientId, keyId, jwsAlgorithm);
    }
    /**
     * Requests a new OAuth2 access token using the private_key_jwt grant.
     *
     * @return a {@link TokenResponse} containing the access token and its lifetime
     * @throws OAuth2TokenException if the assertion cannot be built or the token request fails
     */
    @Retry(name = "oauth2")
    @CircuitBreaker(name = "oauth2")
    @Override
    public TokenResponse getAccessToken() {
        log.debug("Requesting OAuth2 token via private_key_jwt from {}", tokenUri);

        initPrivateTwtTokenService();
        String clientAssertion = buildClientAssertion();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE, CLIENT_CREDENTIALS);
        formData.add(CLIENT_ID, clientId);
        formData.add(CLIENT_ASSERTION_TYPE, CLIENT_ASSERTION_TYPE_VALUE);
        formData.add(CLIENT_ASSERTION, clientAssertion);

        try (CloseableHttpClient httpClient = httpClientBuilder.buildHttpClient()) {
            RestClient restClient = buildRestClient(httpClient);
            Map<String, Object> response = restClient
                    .post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.containsKey(ACCESS_TOKEN)) {
                String token = (String) response.get(ACCESS_TOKEN);
                long expiresIn = ((Number) response.getOrDefault(EXPIRES_IN, 0)).longValue();
                log.info("Successfully retrieved access token via private_key_jwt, expires in {} seconds", expiresIn);
                return new TokenResponse(token, expiresIn);
            } else {
                log.error(
                        "Token response did not contain access_token: {}",
                        response != null ? response.keySet() : "null");
                throw new OAuth2TokenException("Failed to retrieve access token. Missing access_token in response.");
            }
        } catch (OAuth2TokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving OAuth2 token via private_key_jwt", e);
            throw new OAuth2TokenException("Error retrieving OAuth2 token via private_key_jwt", e);
        }
    }

    // -----------------------------------------------------------------------
    // JWT assertion builder  (RFC 7523 §3)
    // -----------------------------------------------------------------------

    private String buildClientAssertion() {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(clientId)
                    .subject(clientId)
                    .audience(tokenUri)
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(ASSERTION_LIFETIME_SECONDS)))
                    .build();

            JWSHeader header = new JWSHeader.Builder(jwsAlgorithm)
                    .keyID(keyId)
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(buildSigner());
            return jwt.serialize();
        } catch (Exception e) {
            throw new OAuth2TokenException("Failed to build signed JWT client assertion", e);
        }
    }

    private JWSSigner buildSigner() throws Exception {
        if (privateKey instanceof RSAPrivateKey rsa) {
            return new RSASSASigner(rsa);
        } else if (privateKey instanceof ECPrivateKey ec) {
            return new ECDSASigner(ec);
        }
        throw new OAuth2TokenException(
                "Unsupported private key type: " + privateKey.getClass().getName());
    }

    // -----------------------------------------------------------------------
    // Keystore loading — single open, both private key and kid extracted
    // -----------------------------------------------------------------------

    /**
     * Holds the private key and the derived {@code kid} from a single keystore open.
     * Avoids reading the keystore file twice.
     */
    record KeystoreContents(PrivateKey privateKey, String kid) {}

    /**
     * Opens the PKCS12 keystore once, extracts the private key and the leaf certificate,
     * and derives the {@code kid} from the certificate's SHA-256 thumbprint (RFC 7638).
     *
     * <p>The cert-manager sidecar places the CA-signed certificate at {@code chain[0]}
     * under the configured alias.  On certificate renewal the keystore is overwritten
     * in-place; the new {@code kid} is picked up automatically on the next start.</p>
     *
     * @param keystorePath     filesystem path to the PKCS12 keystore
     * @param keystorePassword keystore password
     * @param alias            key entry alias
     * @return a {@link KeystoreContents} holding both artefacts
     * @throws OAuth2TokenException if the keystore cannot be loaded or required entries are missing
     */
    static KeystoreContents loadKeystoreContents(String keystorePath, String keystorePassword, String alias) {
        if (StringUtils.isAnyBlank(keystorePath, keystorePassword, alias)) {
            throw new OAuth2TokenException(
                    "private_key_jwt requires: application.private-jwt.keystore-path, "
                            + "application.private-jwt.keystore-password, application.private-jwt.key-alias");
        }

        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            KeyStore ks = KeyStore.getInstance(PKCS_12);
            ks.load(fis, keystorePassword.toCharArray());

            // 1. Private key
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, keystorePassword.toCharArray());
            if (privateKey == null) {
                throw new OAuth2TokenException(
                        "No private key for alias '" + alias + "' in keystore: " + keystorePath);
            }

            // 2. Leaf certificate — cert-manager places the signed cert at chain[0]
            Certificate[] chain = ks.getCertificateChain(alias);
            if (chain == null || chain.length == 0) {
                throw new OAuth2TokenException(
                        "No certificate chain for alias '" + alias + "' in keystore: " + keystorePath
                                + ". Has cert-manager completed its first sync job yet?");
            }
            X509Certificate leafCert = (X509Certificate) chain[0];

            // 3. Derive kid as SHA-256 thumbprint — identical to what Keycloak computes
            //    when the certificate is uploaded via the Admin REST API
            String kid = deriveKidFromCertificate(leafCert);

            log.info("Keystore loaded from '{}' alias '{}'. Subject: {}, kid: {}",
                    keystorePath, alias, leafCert.getSubjectX500Principal().getName(), kid);

            return new KeystoreContents(privateKey, kid);

        } catch (OAuth2TokenException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuth2TokenException("Failed to load keystore from: " + keystorePath, e);
        }
    }

    /**
     * Computes the JWT key ID as the X.509 SHA-256 thumbprint of the certificate.
     *
     * <p>Formula: {@code BASE64URL(SHA-256(DER-encoded certificate bytes))} — the
     * {@code x5t#S256} parameter defined in RFC 7517 §4.9 and RFC 7638.  Keycloak
     * uses this exact value as the {@code kid} in its JWKS endpoint when the certificate
     * is registered via the Admin REST API.</p>
     *
     * @param cert the CA-signed X.509 leaf certificate
     * @return base64url-encoded SHA-256 thumbprint
     * @throws OAuth2TokenException if the thumbprint cannot be computed
     */
    static String deriveKidFromCertificate(X509Certificate cert) {
        try {
            String kid = RSAKey.parse(cert).computeThumbprint("SHA-256").toString();
            log.debug("Derived kid from certificate SHA-256 thumbprint: '{}'", kid);
            return kid;
        } catch (Exception e) {
            throw new OAuth2TokenException(
                    "Failed to derive kid from certificate SHA-256 thumbprint. "
                            + "Ensure the keystore contains an RSA certificate at the configured alias.", e);
        }
    }

    private static JWSAlgorithm resolveAlgorithm(String alg) {
        return switch (alg.toUpperCase()) {
            case "RS256" -> JWSAlgorithm.RS256;
            case "RS384" -> JWSAlgorithm.RS384;
            case "RS512" -> JWSAlgorithm.RS512;
            case "ES256" -> JWSAlgorithm.ES256;
            case "ES384" -> JWSAlgorithm.ES384;
            case "ES512" -> JWSAlgorithm.ES512;
            default -> throw new OAuth2TokenException(
                    "Unsupported JWT signing algorithm (application.private-jwt.algorithm): " + alg);
        };
    }

    private String resolveKeystorePath(CertificateProperties.Destination config) {
        Path basepath = Paths.get(config.getPath());
        if (Objects.nonNull(basepath) && basepath.toFile().isDirectory()) {
            String keystoreFile = config.getKeystoreFile();
            if (keystoreFile != null && !keystoreFile.isBlank()) {
                return basepath.resolve(keystoreFile).toString();
            } else {
                throw new OAuth2TokenException(
                        "Keystore file name is not a valid: " + keystoreFile);
            }
        } else {
            throw new OAuth2TokenException(
                    "Keystore base path is not a valid path: " + basepath);
        }
    }

    private String resolveKeystorePassword(String configuredPassword, String vaultSuffix) {
        if (configuredPassword != null && !configuredPassword.isBlank()) {
            return configuredPassword;
        }

        Map<String, Object> secret = vaultSecretProvider.getSecret(vaultSuffix);
        if (secret.containsKey(PASSWORD)) {
            return (String) secret.get(PASSWORD);
        }

        throw new OAuth2TokenException(
                "Keystore password not configured and not found in vault: " + configuredPassword);
    }

    /**
     * Creates an instance of {@link RestClient} from a {@link CloseableHttpClient}.
     *
     * @param httpClient an mTLS-configured HTTP client
     * @return a configured {@link RestClient}
     */
    protected RestClient buildRestClient(CloseableHttpClient httpClient) {
        return RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}