package dk.fixture;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import dk.mada.action.ActionArguments;

/**
 * Fixture for creating action arguments for tests.
 */
public final class ActionArgs {
    private ActionArgs() {
    }
    
    public static ActionArguments withGpg() {
        return new ActionArguments(readResource("/gpg-testkey.txt"), readResource("/gpg-testkey-password.txt"));
    }
    
    private static String readResource(String path) {
        try (InputStream is = ActionArgs.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Failed to find resource from '" + path + "'");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read resource " + path, e);
        }
    }

}
