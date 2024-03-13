import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Map;

class NexusPublisher {
    private Map<String, String> gpgProcessEnv;
    private Path gnupghomeDir;
    
    private NexusPublisher(String[] args) {
        try {
            System.out.println("Args: " + Arrays.toString(args));
            System.out.println("Environment:");
            System.out.println("CWD: " + Paths.get(".").toAbsolutePath());

            String signingKey = getRequiredEnv("SIGNING_KEY");
            String signingKeySecret = getRequiredEnv("SIGNING_KEY_SECRET");
    
            gnupghomeDir = Files.createTempDirectory("_gnupghome-",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
    
            System.out.println("Created GNUPGHOME: " + gnupghomeDir);
            
            gpgProcessEnv = Map.of(
                    "SIGNING_KEY_SECRET", signingKeySecret,
                    "GNUPGHOME", gnupghomeDir.toString()
                    );
         
        } catch (Exception e) {
            System.err.println("Publisher failed initialization: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            DirectoryDeleter.deleteDir(gnupghomeDir);
        }
    }
    
    private void run() {
        System.out.println("Running!");
    }
    
    public static void main(String[] args) {
        new NexusPublisher(args).run();
    }

    /**
     * {@return the value of a required environment variable}
     *
     * @param envName the name of the environment variable
     */
    private static String getRequiredEnv(String envName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Needs environment variable '" + envName + "' to be defined and non-blank. See readme!");
        }
        return value;
    }
}