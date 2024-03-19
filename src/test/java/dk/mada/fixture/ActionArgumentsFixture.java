package dk.mada.fixture;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;

import dk.mada.action.ActionArguments;
import dk.mada.action.util.LoggerConfig;

/**
 * Fixture for creating action arguments for tests.
 */
public final class ActionArgumentsFixture {
    private ActionArgumentsFixture() {
    }

    /**
     * {@return action arguments based on test certificate}
     * 
     * @throws
     */
    public static ActionArguments withGpg() {
        LoggerConfig.loadConfig("/test-logging.properties");

        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        List<String> emptySuffixes = List.of();
        OssrhCreds ossrhCreds = readOssrhCreds();
        return new ActionArguments(readResource("/gpg-testkey.txt"), readResource("/gpg-testkey-password.txt"), tmpDir, emptySuffixes,
                Level.FINEST, ossrhCreds.user(), ossrhCreds.token());
    }

    private record OssrhCreds(String user, String token) {
    }

    /**
     * Reads OSSRH user:token from $XDG_RUNTIME_DIR/ossrh-creds.txt if
     * the file is available.
     *
     * @return the loaded OSSRH credentials or dummy credentials
     */
    private static OssrhCreds readOssrhCreds() {
        try {
            String runtimePath = System.getenv("XDG_RUNTIME_DIR");
            if (runtimePath != null) {
                Path credsFile = Paths.get(runtimePath).resolve("ossrh-creds.txt");
                if (Files.isRegularFile(credsFile)) {
                    String creds = Files.readString(credsFile).trim();
                    return new OssrhCreds(creds.replaceFirst(":.*", ""), creds.replaceFirst("[^:]*:", ""));
                }
            }
            return new OssrhCreds("no_ossrh_user", "no_ossrh_token");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read OSSRH creds", e);
        }
    }

    private static String readResource(String path) {
        try (InputStream is = ActionArgumentsFixture.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Failed to find resource from '" + path + "'");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read resource " + path, e);
        }
    }
}
