package dk.unit.signer;

import org.junit.jupiter.api.Test;

import dk.fixture.ActionArgumentsFixture;
import dk.mada.action.signer.GpgSigner;

/**
 * Signer tests.
 */
class SignerTest {
    /**
     * Tests that the signatures can be loaded (from test resources).
     */
    @Test
    void canLoadSignatures() {
        new GpgSigner().loadSigningCertificate(ActionArgumentsFixture.withGpg());
    }
}
