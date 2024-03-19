package dk.mada.integration;

import org.junit.jupiter.api.Test;

import dk.mada.action.ActionArguments;
import dk.mada.action.MavenCentralDao;
import dk.mada.fixture.ActionArgumentsFixture;

/**
 * The operations against OSSRH require credentials, so these can
 * only be tested locally.
 *
 * If you want to run the tests yourself (after reviewing the code, naturally),
 * see ActionArgumentsFixture:readOssrhCreds for how to provide
 * the credentials.
 */
public class OssrhOperationsTest {
    @Test
    void canGo() {
        ActionArguments aa = ActionArgumentsFixture.withGpg();
        MavenCentralDao sut = new MavenCentralDao(aa);
        sut.go();
    }
    
}
