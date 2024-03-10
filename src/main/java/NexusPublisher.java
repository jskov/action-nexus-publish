import java.nio.file.Paths;
import java.util.Arrays;

class NexusPublisher {
    public static void main(String[] args) {
        System.out.println("Args: " + Arrays.toString(args));
        System.out.println("Environment:");
//        System.getenv().entrySet().stream()
//            .map(e -> " '" + e.getKey() + "=" + e.getValue() + "'")
//            .sorted()
//            .forEach(System.out::println);
        
        System.out.println("CWD: " + Paths.get(".").toAbsolutePath());
        
    }
}