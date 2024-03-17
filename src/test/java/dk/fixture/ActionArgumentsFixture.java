package dk.fixture;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import dk.mada.action.ActionArguments;

/**
 * Fixture for creating action arguments for tests.
 */
public final class ActionArgumentsFixture {
    private ActionArgumentsFixture() {
    }

    public static ActionArguments withGpg() {
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        List<String> emptySuffixes = List.of();
        return new ActionArguments(readResource("/gpg-testkey.txt"), readResource("/gpg-testkey-password.txt"), tmpDir, emptySuffixes);
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