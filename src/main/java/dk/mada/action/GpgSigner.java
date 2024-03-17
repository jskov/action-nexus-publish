package dk.mada.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;

import dk.mada.action.util.DirectoryDeleter;
import dk.mada.action.util.ExternalCmdRunner;
import dk.mada.action.util.ExternalCmdRunner.CmdInput;
import dk.mada.action.util.ExternalCmdRunner.CmdResult;

public final class GpgSigner {
    private static final int GPG_DEFAULT_TIMEOUT_SECONDS = 3;

    private final Path gnupghomeDir;
    private final Map<String, String> gpgEnv;

    public GpgSigner() {
        try {
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
     * @param actionArgs the action arguments
     * @return the signature fingerprint
     */
    public String loadSigningCertificate(ActionArguments actionArgs) {
        try {
            // Import the certificate
            Path keyFile = gnupghomeDir.resolve("private.txt");
            Files.writeString(keyFile, actionArgs.gpgPrivateKey());
            runCmd("gpg", "--import", "--batch", keyFile.toAbsolutePath().toString());

            // Extract fingerprint of the certificate
            CmdResult idResult = runCmd("gpg", "-K", "--with-colons");
            String fingerprint = GpgDetailType.FINGERPRINT.extractFrom(idResult.output()).replace(":", "");

            // Mark the certificate as ultimately trusted
            Path ownerTrustFile = gnupghomeDir.resolve("otrust.txt");
            Files.writeString(ownerTrustFile, fingerprint + ":6:\n");
            runCmd("gpg", "--import-ownertrust", ownerTrustFile.toAbsolutePath().toString());

            return fingerprint;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load private GPG key", e);
        }
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

    private CmdResult runCmd(String... args) {
        return runCmd(null, List.of(args), GPG_DEFAULT_TIMEOUT_SECONDS);
    }

    private CmdResult runCmd(String stdin, List<String> command, int timeoutSeconds) {
        var input = new CmdInput(command, gnupghomeDir, stdin, gpgEnv, timeoutSeconds);
        CmdResult res = ExternalCmdRunner.runCmd(input);
        return res;
    }
}
