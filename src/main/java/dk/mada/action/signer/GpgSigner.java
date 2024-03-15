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
    private static final int GPG_DEFAULT_TIMEOUT_SECONDS = 30;

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

    public void loadSigningCertificate(ActionArguments aa) {
        try {
            Path keyFile = gnupghomeDir.resolve("private.txt");
            Files.writeString(keyFile, aa.gpgPrivateKey());
            System.out.println(">import");

            List<String> importKeyCmd = List.of("gpg", "--import", "--batch", keyFile.toAbsolutePath().toString());
            runCmd(importKeyCmd);

            System.out.println(">list public");
            runCmd(List.of("gpg", "-k"));
            System.out.println(">list private");
            runCmd(List.of("gpg", "-K"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load private GPG key", e);
        }
    }

    private CmdResult runCmd(List<String> args) {
        return runGpgCmd(null, args, GPG_DEFAULT_TIMEOUT_SECONDS);
    }

    private CmdResult runGpgCmd(String stdin, List<String> command, int timeoutSeconds) {
        var input = new CmdInput(command, gnupghomeDir, stdin, gpgEnv, timeoutSeconds);
        CmdResult res = ExternalCmdRunner.runCmd(input);
        System.out.println(res.output());
        return res;
    }
}
