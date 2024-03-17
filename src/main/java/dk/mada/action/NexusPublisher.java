package dk.mada.action;

import dk.mada.action.signer.GpgSigner;

class NexusPublisher {
    private void run() {
        GpgSigner signer = new GpgSigner();
        try {
            ActionArguments args = ActionArguments.fromEnv();
            signer.loadSigningCertificate(args);

            System.out.println("Running!");
        } catch (Exception e) {
            System.err.println("Publisher failed initialization: " + e.getMessage());
            System.exit(1);
        } finally {
            signer.cleanup();
        }
    }

    public static void main() {
        new NexusPublisher().run();
    }
}
