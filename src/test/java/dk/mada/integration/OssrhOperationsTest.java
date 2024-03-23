package dk.mada.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import dk.mada.action.BundleCollector;
import dk.mada.action.BundleCollector.Bundle;
import dk.mada.action.MavenCentralDao;
import dk.mada.fixture.TestInstances;

/**
 * The operations against OSSRH require credentials, so these can only be tested locally.
 *
 * If you want to run the tests yourself (after reviewing the code, naturally), see
 * ActionArgumentsFixture:readOssrhCreds for how to provide the credentials.
 */
public class OssrhOperationsTest {
    @TempDir(cleanup = CleanupMode.NEVER)
    Path workDir;

    @Test
    void canGo() throws IOException {
        String pomName = "action-maven-publish-test-0.0.0.pom";
        Files.copy(Paths.get("src/test/data").resolve(pomName), workDir.resolve(pomName));

        BundleCollector bundleCollector = TestInstances.bundleCollector();
        List<Bundle> x = bundleCollector.collectBundles(workDir, List.of());

        Bundle pomBundle = x.getFirst();
        MavenCentralDao sut = TestInstances.mavenCentralDao();

        System.out.println("" + pomBundle.bundleJar());

        sut.upload(pomBundle.bundleJar());
    }
}
