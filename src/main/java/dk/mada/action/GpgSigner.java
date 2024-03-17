package dk.mada.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dk.mada.action.util.DirectoryDeleter;
import dk.mada.action.util.ExternalCmdRunner;
import dk.mada.action.util.ExternalCmdRunner.CmdInput;
import dk.mada.action.util.ExternalCmdRunner.CmdResult;

public final class GpgSigner {
    /** The GPG command timeout in seconds. */
    private static final int GPG_DEFAULT_TIMEOUT_SECONDS = 5;

    /** The action arguments provided by the user. */
    private final ActionArguments actionArgs;
    /** The GNUPG_HOME directory. */
    private final Path gnupghomeDir;
    /** The environment provided when running GPG. */
    private final Map<String, String> gpgEnv;

    /** The certificate fingerprint. Found while loading certificate. */
    private String certificateFingerprint;

    public GpgSigner(ActionArguments actionArgs) {
        try {
            this.actionArgs = actionArgs;
            gnupghomeDir = Files.createTempDirectory("_gnupghome-",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            gpgEnv = Map.of(
                    "GNUPGHOME", gnupghomeDir.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create GNUPGHOME directory", e);
        }
    }

    /**
     * Cleanup working directory.
     */
    public void cleanup() {
        DirectoryDeleter.deleteDir(gnupghomeDir);
    }

    /** {@return the created GNUPGHOME directory} */
    public Path getGnupgHome() {
        return gnupghomeDir;
    }

    /**
     * Loads the provided signing certificate.
     *
     * After loading, the private certificate is tweaked to be ultimately trusted.
     *
     * @return the signature fingerprint
     */
    public String loadSigningCertificate() {
        try {
            // Import the certificate
            Path keyFile = gnupghomeDir.resolve("private.txt");
            Files.writeString(keyFile, actionArgs.gpgPrivateKey());
            runGpg("gpg", "--import", "--batch", keyFile.toAbsolutePath().toString());

            // Extract fingerprint of the certificate
            CmdResult idResult = runGpg("gpg", "-K", "--with-colons");
            certificateFingerprint = GpgDetailType.FINGERPRINT.extractFrom(idResult.output()).replace(":", "");

            // Mark the certificate as ultimately trusted
            Path ownerTrustFile = gnupghomeDir.resolve("otrust.txt");
            Files.writeString(ownerTrustFile, certificateFingerprint + ":6:\n");
            runGpg("gpg", "--import-ownertrust", ownerTrustFile.toAbsolutePath().toString());

            return certificateFingerprint;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load private GPG key", e);
        }
    }

    /**
     * Signs the file with the loaded GPG certificate.
     *
     * @param file the file to sign
     */
    public void sign(Path file) {
        String fingerprint = Objects.requireNonNull(certificateFingerprint, "Need to load certificate!");

        System.out.println("signing " + file);
        
        // "--quiet",
        CmdResult o = runGpgWithInput(actionArgs.gpgPrivateKeySecret(),
                "gpg",
                // FIXME: when debug "-v",
                "-v",
                "--batch",
                // "--yes",
                "--pinentry-mode", "loopback",
                "--passphrase-fd", "0",
                "-u", fingerprint,
                "--detach-sign", "--armor",
                file.toAbsolutePath().toString());

        // FIXME: debug flag, logger

        System.out.println("res: " + o.output());
    }

    /**
     * The GPG Detail types
     *
     * @see https://github.com/gpg/gnupg/blob/master/doc/DETAILS
     */
    enum GpgDetailType {
        /** The certificate fingerprint. */
        FINGERPRINT("fpr");

        private final String prefix;

        GpgDetailType(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Extracts the detail information from GPG '--with-colons' output.
         *
         * @param output the GPG output
         * @return the relevant information line
         */
        public String extractFrom(String output) {
            return output.lines()
                    .filter(l -> l.startsWith(prefix))
                    .map(l -> l.substring(prefix.length() + 1))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private CmdResult runGpg(String... args) {
        return runGpgWithInput(null, args);
    }

    private CmdResult runGpgWithInput(String stdin, String... args) {
        var input = new CmdInput(List.of(args), gnupghomeDir, stdin, gpgEnv, GPG_DEFAULT_TIMEOUT_SECONDS);
        CmdResult res = ExternalCmdRunner.runCmd(input);
        return res;
    }
}
