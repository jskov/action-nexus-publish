package dk.mada.action;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * These are the arguments accepted by the action. Arguments are provided via environment variables.
 *
 * @param gpgPrivateKey       the private GPG key used for signing
 * @param gpgPrivateKeySecret the secret for the private GPG key
 * @param searchDir           the directory to search for POM files
 * @param companionSuffixes   the companion suffixes to include when finding a POM file
 */
public record ActionArguments(String gpgPrivateKey, String gpgPrivateKeySecret, Path searchDir, List<String> companionSuffixes) {
    /** The PGP header expected to be in the GPG key. */
    private static final String BEGIN_PGP_PRIVATE_KEY_BLOCK = "-----BEGIN PGP PRIVATE KEY BLOCK-----";

    /**
     * Argument validation.
     */
    public ActionArguments {
        Objects.requireNonNull(gpgPrivateKey, "The private GPG key must be specified");
        Objects.requireNonNull(gpgPrivateKeySecret, "The private GPG secret must be specified");
        Objects.requireNonNull(searchDir, "The search directory must be specified");
        Objects.requireNonNull(companionSuffixes, "The companion suffixes must not be null");

        if (!gpgPrivateKey.contains(BEGIN_PGP_PRIVATE_KEY_BLOCK)) {
            throw new IllegalArgumentException("Provided GPG key does not contain private header: " + BEGIN_PGP_PRIVATE_KEY_BLOCK);
        }
        if (!Files.isDirectory(searchDir)) {
            throw new IllegalArgumentException("Not a directory: " + searchDir);
        }
    }

    /**
     * Extracts action arguments from the environment.
     *
     * @return the environment-specified action arguments
     */
    public static ActionArguments fromEnv() {
        String suffixesStr = getRequiredEnv("COMPANION_SUFFIXES");
        List<String> suffixes = Stream.of(suffixesStr.split(",", -1))
                .map(String::trim)
                .toList();
        Path searchDir = Paths.get(getRequiredEnv("SEARCH_DIR"));
        return new ActionArguments(getRequiredEnv("SIGNING_KEY"), getRequiredEnv("SIGNING_KEY_SECRET"), searchDir, suffixes);
    }

    /**
     * {@return the value of a required environment variable}
     *
     * @param envName the name of the environment variable
     */
    private static String getRequiredEnv(String envName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Needs environment variable '" + envName + "' to be defined and non-blank. See readme!");
        }
        return value;
    }

    @Override
    public String toString() {
        return "ActionArguments [searchDir=" + searchDir + ", companionSuffixes=" + companionSuffixes + "]";
    }
}
