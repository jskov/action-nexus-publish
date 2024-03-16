package dk.mada.action.signer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;

import dk.mada.action.ActionArguments;
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
                    "GNUPGHOME", gnupghomeDir.toAbsolutePath().toString(),
                    "GPG_OPTS", "--pinentry-mode loopback"
                    );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create GNUPGHOME directory", e);
        }
    }

    /**
     * Loads the provided signing certificate.
     *
     * After loading, the private certificate is tweaked to be ultimately trusted.
     *
     * @param actionArgs the action arguments
     */
    public void loadSigningCertificate(ActionArguments actionArgs) {
        try {
            System.out.println("TTY");
            CmdResult ttyResult = runCmd(List.of("tty"));
            System.out.println("See TTY: " + ttyResult.output());
            
            Path keyFile = gnupghomeDir.resolve("private.txt");
            Files.writeString(keyFile, actionArgs.gpgPrivateKey());
            System.out.println(">import");

            List<String> importKeyCmd = List.of("gpg", "--import", "--batch", keyFile.toAbsolutePath().toString());
            runCmd(importKeyCmd);

            System.out.println(">list private");
            runCmd(List.of("gpg", "-K"));

            System.out.println("-----------------");
            
            List<String> idKeyCmd = List.of("gpg", "-K", "--with-colons");
            CmdResult idResult = runCmd(idKeyCmd);

            String fingerprint = GpgDetailType.FINGERPRINT.extractFrom(idResult.output()).replace(":", "");
            System.out.println("Fingerprint: " + fingerprint);

            // Mark the certificate as ultimately trusted
            Path ownerTrustFile = gnupghomeDir.resolve("otrust.txt");
            String ownerTrust = fingerprint + ":6:\n";
            Files.writeString(ownerTrustFile, ownerTrust);
            CmdResult newTrustResult = runCmd(List.of("gpg", "--import-ownertrust", ownerTrustFile.toAbsolutePath().toString()));
            System.out.println("NEW: " + newTrustResult.output());
            
            System.out.println("-----------------");

            System.out.println(">list private");
            runCmd(List.of("gpg", "-K"));
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
        FINGERPRINT("fpr");

        private final String prefix;

        private GpgDetailType(String prefix) {
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
    
    private CmdResult runCmd(List<String> args) {
        return runCmd(null, args, GPG_DEFAULT_TIMEOUT_SECONDS);
    }

    private CmdResult runCmd(String stdin, List<String> command, int timeoutSeconds) {
        System.out.println("CMD: " + command);
        var input = new CmdInput(command, gnupghomeDir, stdin, gpgEnv, timeoutSeconds);
        CmdResult res = ExternalCmdRunner.runCmd(input);
        System.out.println(res.output());
        return res;
    }
}
