package dk.mada.action.sign;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;

import dk.mada.action.ActionArguments;

public class GpgSigner {
    // GNUPGHOME
    public void go(ActionArguments aa) {
        try {
            Path gnupghomeDir = Files.createTempDirectory("_gnupghome-",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            Map<String, String> env = Map.of(
                    "GNUPGHOME", gnupghomeDir.toAbsolutePath().toString()
                    );
                    
            List<String> args = List.of("gpg", "--version");
            ProcessBuilder pb = new ProcessBuilder()
                .command(args)
                .directory(gnupghomeDir.toFile());
            pb.environment().putAll(env);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("See");
            System.out.println(output);
        } catch (IOException e) {
            throw new IllegalStateException("Failed running GPG", e);
        }
    }
}
