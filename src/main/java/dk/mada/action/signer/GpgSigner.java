package dk.mada.action.signer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;

import dk.mada.action.ActionArguments;

public final class GpgSigner {
    
    private final Path gnupghomeDir;
    private final Map<String, String> gpgEnv;

    public GpgSigner() {
        try {
            gnupghomeDir = Files.createTempDirectory("_gnupghome-",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            gpgEnv = Map.of(
                    "GNUPGHOME", gnupghomeDir.toAbsolutePath().toString()
                    );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create GNUPGHOME directory", e);
        }
    }

    public void loadSigningCertificate(ActionArguments aa) {
        try {
            List<String> args = List.of("gpg", "-k");
            ProcessBuilder pb = new ProcessBuilder()
                .command(args)
                .redirectErrorStream(true)
                .directory(gnupghomeDir.toFile());
            pb.environment().putAll(gpgEnv);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Exit " + p.exitValue());
            System.out.println(output);
        } catch (IOException e) {
            throw new IllegalStateException("Failed running GPG", e);
        }
    }
}
