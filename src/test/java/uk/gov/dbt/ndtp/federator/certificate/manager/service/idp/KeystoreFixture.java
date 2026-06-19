package uk.gov.dbt.ndtp.federator.certificate.manager.service.idp;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Test-only helper that generates a throwaway self-signed RSA PKCS12 keystore
 * using the JDK's bundled {@code keytool}.
 *
 * <p>Each call writes a new keystore file under the supplied {@code @TempDir},
 * keeping tests fully isolated. The keystore file sits directly in
 * {@code tempDir} so that {@link PrivateJwtTokenServiceImpl#resolveKeystorePath}
 * (which splits the path into a base directory + filename) works correctly with
 * the mocked {@code CertificateProperties.Destination}.</p>
 */
public final class KeystoreFixture {

    static final String TEST_PASSWORD = "changeit";
    private static final String DNAME = "CN=cert-manager-test,OU=NDTP,O=DBT,L=London,ST=London,C=GB";

    private final Path keystorePath;
    private final String alias;

    private KeystoreFixture(Path keystorePath, String alias) {
        this.keystorePath = keystorePath;
        this.alias = alias;
    }

    /** Full path to the generated {@code .p12} file. */
    public Path keystorePath() {
        return keystorePath;
    }

    /**
     * Just the filename (e.g. {@code "federator-keystore.p12"}).
     * This is the value returned by the mocked
     * {@code CertificateProperties.Destination#getKeystoreFile()}.
     */
    public String keystoreFileName() {
        return keystorePath.getFileName().toString();
    }

    /** The alias under which the key/cert pair is stored. */
    public String alias() {
        return alias;
    }

    /** The keystore password. */
    public String password() {
        return TEST_PASSWORD;
    }

    /**
     * Generates a PKCS12 keystore with a self-signed RSA-2048 key pair at
     * {@code <tempDir>/<alias>-keystore.p12}.
     *
     * @param tempDir JUnit {@code @TempDir}-managed directory — used as both the
     *                base path returned by the mocked
     *                {@code Destination#getPath()} AND the parent of the keystore file
     * @param alias   key entry alias (e.g. {@code "federator"})
     */
    public static KeystoreFixture create(Path tempDir, String alias) throws Exception {
        Path keystorePath = tempDir.resolve(alias + "-keystore.p12");

        ProcessBuilder pb = new ProcessBuilder(
                resolveKeytool(),
                "-genkeypair",
                "-alias", alias,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-sigalg", "SHA256withRSA",
                "-validity", "3650",
                "-dname", DNAME,
                "-keystore", keystorePath.toString(),
                "-storetype", "PKCS12",
                "-storepass", TEST_PASSWORD,
                "-keypass", TEST_PASSWORD);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("keytool timed out. Output: " + output);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(
                    "keytool failed (exit " + process.exitValue() + "). Output: " + output);
        }

        return new KeystoreFixture(keystorePath, alias);
    }

    private static String resolveKeytool() {
        String javaHome = System.getProperty("java.home");
        Path candidate = Path.of(javaHome, "bin", "keytool");
        return candidate.toFile().exists() ? candidate.toString() : "keytool";
    }
}