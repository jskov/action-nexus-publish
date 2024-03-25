package dk.mada.action;

import java.util.List;
import java.util.logging.Logger;

import dk.mada.action.BundleCollector.Bundle;
import dk.mada.action.util.LoggerConfig;

class ActionNexusPublisher {
    private static Logger logger = Logger.getLogger(ActionNexusPublisher.class.getName());

    private void run() {
        ActionArguments args = ActionArguments.fromEnv();
        LoggerConfig.loadDefaultConfig(args.logLevel());
        logger.config(() -> args.toString());

        GpgSigner signer = new GpgSigner(args.gpgCertificate());
        BundleCollector bundleBuilder = new BundleCollector(signer);
        OssrhProxy proxy = new OssrhProxy(args.ossrhCredentials());
        BundlePublisher bundlePublisher = new BundlePublisher(proxy);
        try {
            signer.loadSigningCertificate();

            List<Bundle> bundles = bundleBuilder.collectBundles(args.searchDir(), args.companionSuffixes());
            bundlePublisher.publish(bundles, args.targetAction());
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
        new ActionNexusPublisher().run();
    }
}
