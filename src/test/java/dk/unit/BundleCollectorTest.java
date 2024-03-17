package dk.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import dk.fixture.ActionArgumentsFixture;
import dk.mada.action.BundleCollector;
import dk.mada.action.BundleCollector.Bundle;
import dk.mada.action.GpgSigner;

/**
 * Tests bundle collection - really the search for files.
 */
class BundleCollectorTest {
    /** Temporary test directory. */
    private @TempDir(cleanup = CleanupMode.NEVER) Path testDir;

    private final GpgSigner signer = new GpgSigner(ActionArgumentsFixture.withGpg());
    private final BundleCollector sut = new BundleCollector(signer);

    @Test
    void canSignFiles() throws IOException {
        Files.copy(Paths.get("gradle/wrapper/gradle-wrapper.jar"), testDir.resolve("bundle.jar"));
        Files.createFile(testDir.resolve("bundle.pom"));
        signer.loadSigningCertificate();
        
        List<Bundle> x = sut.buildBundles(testDir, List.of(".jar"));
        System.out.println("GOt " + x);
        
        
    }
    
    @Test
    void canCollectBundle() throws IOException {
        setupFileTree(
                "root.jar", // ignored
                "dir/a.pom",
                "dir/a.jar", // ignored
                "dir/a-sources.jar",
                "dir/a.module");

        List<Bundle> foundBundles = new BundleCollector(null).collectBundles(testDir, List.of(".module", "-sources.jar"));
        List<String> foundPaths = foundBundles.stream()
                .flatMap(b -> toPaths(b).stream())
                .toList();
        assertThat(foundPaths)
                .containsExactlyInAnyOrder("dir/a.pom", "dir/a-sources.jar", "dir/a.module");
    }

    private List<String> toPaths(Bundle b) {
        List<Path> files = new ArrayList<>();
        files.add(b.pom());
        files.addAll(b.assets());
        return files.stream()
                .map(p -> testDir.relativize(p).toString())
                .toList();
    }

    private void setupFileTree(String... files) throws IOException {
        for (String path : files) {
            Path file = testDir.resolve(path);
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        }
    }
}
