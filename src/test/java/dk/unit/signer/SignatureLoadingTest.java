package dk.unit.signer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import dk.mada.action.ActionArguments;
import dk.mada.action.sign.GpgSigner;

class SignatureLoadingTest {
    @Test
    void canLoadSignatures() {
        var aa = new ActionArguments(readResource("/gpg-testkey.txt"), readResource("/gpg-testkey-password.txt"));
        new GpgSigner().go(aa);
    }
    
    private String readResource(String path) {
        try (InputStream is = SignatureLoadingTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Failed to find resource from '" + path + "'");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read resource " + path, e);
        }
    }
}
