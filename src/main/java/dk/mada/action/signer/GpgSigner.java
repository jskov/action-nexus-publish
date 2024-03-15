package dk.mada.action.signer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import dk.mada.action.ActionArguments;

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

            runGpgCmd(List.of("--import", "--batch", "--pinentry-mode", "loopback", keyFile.toAbsolutePath().toString()));
            System.out.println(">list public");
            runGpgCmd(List.of("-k"));
            System.out.println(">list private");
            runGpgCmd(List.of("-K"));
            
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load private GPG key", e);
        }
    }

    record CmdResult(int status, String txt) {
    }

    private CmdResult runGpgCmd(List<String> args) {
        return runGpgCmd(null, args, GPG_DEFAULT_TIMEOUT_SECONDS);
    }

    private CmdResult runGpgCmd(String stdin, List<String> args, int timeoutSeconds) {
        try {
            List<String> combinedArgs = new ArrayList<>();
            combinedArgs.add("gpg");
            combinedArgs.addAll(args);

            ProcessBuilder pb = new ProcessBuilder()
                    .command(combinedArgs)
                    .redirectErrorStream(true)
                    .directory(gnupghomeDir.toFile());
            pb.environment().putAll(gpgEnv);
            Process p = pb.start();
            
            if (stdin != null) {
                p.outputWriter(StandardCharsets.UTF_8).write(stdin);
            }
            BufferedReader outputReader = p.inputReader(StandardCharsets.UTF_8);
            
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Command timed out!");
            }

            String output = outputReader.lines().collect(Collectors.joining("\n"));
            return new CmdResult(p.exitValue(), output);
        } catch (IOException e) {
            throw new IllegalStateException("Failed running GPG", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Waited for GPG to complete, but was interruped");
        }
    }
}
