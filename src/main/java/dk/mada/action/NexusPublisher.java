package dk.mada.action;

import java.util.List;

import dk.mada.action.BundleCollector.Bundle;

class NexusPublisher {
    private void run() {
        ActionArguments args = ActionArguments.fromEnv();
        System.out.println(args);

        GpgSigner signer = new GpgSigner(args);
        BundleCollector bundleBuilder = new BundleCollector(signer);
        try {
            System.out.println("Running!");
            signer.loadSigningCertificate();

            List<Bundle> bundles = bundleBuilder.collectBundles(args.searchDir(), args.companionSuffixes());
            System.out.println("Found bundles:");
            bundles.forEach(b -> System.out.println(" " + b));
        } catch (Exception e) {
            System.err.println("Publisher failed initialization: " + e.getMessage());
            System.exit(1);
        } finally {
            signer.cleanup();
            if (bundleBuilder != null) {
                bundleBuilder.cleanup();
            }
        }
    }

    /**
     * Action main method.
     *
     * @param args the arguments from the command line, ignored
     */
    public static void main(String[] args) {
        new NexusPublisher().run();
    }
}
