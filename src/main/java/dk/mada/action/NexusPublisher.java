package dk.mada.action;

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

    /**
     * Action main method.
     *
     * @param args the arguments from the command line, ignored
     */
    public static void main(String[] args) {
        new NexusPublisher().run();
    }
}
