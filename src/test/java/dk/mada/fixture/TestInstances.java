package dk.mada.fixture;

import dk.mada.action.BundleCollector;
import dk.mada.action.GpgSigner;
import dk.mada.action.MavenCentralDao;

/**
 * Provides test instances of the domain classes.
 */
public class TestInstances {
    private TestInstances() {
    }

    /** The GPG signer test instance. */
    private static GpgSigner signer;
    /** The bundle collector test instance. */
    private static BundleCollector bundleCollector;
    /** The Maven Central DAO test instance. */
    private static MavenCentralDao mavenCentralDao;

    /** @{return an initialized GPG signer instance} */
    public static GpgSigner signer() {
        if (signer == null) {
            signer = new GpgSigner(ArgumentsFixture.gpgCert());
            signer.loadSigningCertificate();
        }
        return signer;
    }

    /** @{return an initialized bundle collector instance} */
    public static BundleCollector bundleCollector() {
        if (bundleCollector == null) {
            bundleCollector = new BundleCollector(signer());
        }
        return bundleCollector;
    }
    
    public static MavenCentralDao mavenCentralDao() {
        if (mavenCentralDao == null) {
            mavenCentralDao = new MavenCentralDao(ArgumentsFixture.withGpg());
        }
        return mavenCentralDao;
    }
}
