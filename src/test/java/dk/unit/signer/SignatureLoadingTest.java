package dk.unit.signer;

import org.junit.jupiter.api.Test;

import dk.fixture.ActionArgs;
import dk.mada.action.signer.GpgSigner;

class SignatureLoadingTest {
    @Test
    void canLoadSignatures() {
        new GpgSigner().loadSigningCertificate(ActionArgs.withGpg());
    }
}
