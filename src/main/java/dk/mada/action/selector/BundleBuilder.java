package dk.mada.action.selector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Builds bundles for later signing/publishing.
 */
public final class BundleBuilder {
    private BundleBuilder() {
    }

    /**
     * Finds bundles in and below the search directory.
     * 
     * @param searchDir         the search directory
     * @param companionSuffixes the suffixes to use for picking bundle assets
     * @return the found bundles
     */
    public static List<Bundle> findBundles(Path searchDir, List<String> companionSuffixes) {
        try (Stream<Path> files = Files.walk(searchDir)) {
            // First find the POMs
            List<Path> poms = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".pom"))
                    .toList();

            // Then make bundles with the companions
            return poms.stream()
                    .map(p -> makeBundle(p, companionSuffixes))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build bundles in " + searchDir, e);
        }
    }

    /**
     * A bundle to be uploaded.
     *
     * @param pom    the main POM file
     * @param assets a list of additional assets (may be empty)
     */
    public record Bundle(Path pom, List<Path> assets) {
    }

    /**
     * Makes a bundle based on the main POM file.
     *
     * Finds companion files for the bundle based on the provided suffixes.
     *
     * @param pomFile           the main POM file
     * @param companionSuffixes the suffixes to find companion assets from
     * @return the resulting bundle
     */
    private static Bundle makeBundle(Path pomFile, List<String> companionSuffixes) {
        Path dir = pomFile.getParent();
        String basename = pomFile.getFileName().toString().replace(".pom", "");
        List<Path> companions = companionSuffixes.stream()
                .map(suffix -> dir.resolve(basename + suffix))
                .filter(Files::isRegularFile)
                .toList();
        return new Bundle(pomFile, companions);
    }
}
