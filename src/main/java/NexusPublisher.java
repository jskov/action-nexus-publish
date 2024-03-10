import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
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
            deleteDir(gnupghomeDir);
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

    /**
     * Deletes directory recursively.
     *
     * @param dir the directory to delete
     */
    private static void deleteDir(Path dir) {
        try {
            if (dir == null) {
                return;
            }

            Files.walkFileTree(dir, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                  return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                  Files.delete(file);
                  return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                  return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                  Files.delete(dir);
                  return FileVisitResult.CONTINUE;
                }
              });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete directory " + dir, e);
        }
    }
}