package dk.mada.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Collects bundles from disk for later signing/publishing.
 */
public final class BundleCollector {
    /** The GPG signer. */
    private final GpgSigner signer;

    /**
     * Creates a new instance.
     *
     * @param signer the signer to use when creating the bundles
     */
    public BundleCollector(GpgSigner signer) {
        this.signer = signer;
    }

    /**
     * Collects bundles for publishing.
     *
     * @param searchDir         the search directory
     * @param companionSuffixes the suffixes to use for finding bundle assets
     * @return the collected bundles
     */
    public List<Bundle> collectBundles(Path searchDir, List<String> companionSuffixes) {
        return findBundles(searchDir, companionSuffixes).stream()
                .map(this::signBundleFiles)
                .toList();
    }

    /**
     * Collects bundles in and below the search directory.
     *
     * @param searchDir         the search directory
     * @param companionSuffixes the suffixes to use for finding bundle assets
     * @return the collected bundles
     */
    public List<Bundle> findBundles(Path searchDir, List<String> companionSuffixes) {
        try (Stream<Path> files = Files.walk(searchDir)) {
            // First find the POMs
            List<Path> poms = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".pom"))
                    .toList();

            // Then make bundles with the companions
            return poms.stream()
                    .map(p -> makeBaseBundle(p, companionSuffixes))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build bundles in " + searchDir, e);
        }
    }

    /**
     * A bundle to be uploaded.
     *
     * @param pom        the main POM file
     * @param assets     a list of additional assets (may be empty)
     * @param signatures a list of created signatures for the assets
     */
    public record Bundle(Path pom, List<Path> assets, List<Path> signatures) {
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
    private Bundle makeBaseBundle(Path pomFile, List<String> companionSuffixes) {
        Path dir = pomFile.getParent();
        String basename = pomFile.getFileName().toString().replace(".pom", "");
        List<Path> companions = companionSuffixes.stream()
                .map(suffix -> dir.resolve(basename + suffix))
                .filter(Files::isRegularFile)
                .toList();
        return new Bundle(pomFile, companions, List.of());
    }

    private Bundle signBundleFiles(Bundle bundle) {
        List<Path> signatures = Stream.concat(Stream.of(bundle.pom()), bundle.assets().stream())
                .map(signer::sign)
                .toList();
        return new Bundle(bundle.pom(), bundle.assets(), signatures);
    }
}
