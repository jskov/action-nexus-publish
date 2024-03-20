package dk.mada.action;

import java.util.List;
import java.util.logging.Logger;

import dk.mada.action.BundleCollector.Bundle;
import dk.mada.action.util.LoggerConfig;

class NexusPublisher {
    private static Logger logger = Logger.getLogger(NexusPublisher.class.getName());

    private void run() {
        ActionArguments args = ActionArguments.fromEnv();
        LoggerConfig.loadDefaultConfig(args.logLevel());
        logger.config(() -> args.toString());

        GpgSigner signer = new GpgSigner(args.gpgCertificate());
        BundleCollector bundleBuilder = new BundleCollector(signer);
        try {
            logger.info("Running!");
            signer.loadSigningCertificate();

            List<Bundle> bundles = bundleBuilder.collectBundles(args.searchDir(), args.companionSuffixes());
            logger.info("Found bundles:");
            bundles.forEach(b -> logger.info(" " + b));
        } catch (Exception e) {
            logger.warning("Publisher failed initialization: " + e.getMessage());
            System.exit(1);
        } finally {
            signer.cleanup();
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
