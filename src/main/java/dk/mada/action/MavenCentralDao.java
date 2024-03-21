package dk.mada.action;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.logging.Logger;

import dk.mada.action.util.EphemeralCookieHandler;

public class MavenCentralDao {
    private static Logger logger = Logger.getLogger(MavenCentralDao.class.getName());
    private static final String OSSRH_BASE_URL = "https://s01.oss.sonatype.org";
    /** The action arguments, containing OSSRH credentials. */
    private final ActionArguments actionArguments;
    /** The http client. */
//    private final HttpClient client;
    private CookieHandler cookieHandler;

    /**
     * Constructs new instance.
     *
     * @param actionArguments the action arguments
     */
    public MavenCentralDao(ActionArguments actionArguments) {
        this.actionArguments = actionArguments;
        System.setProperty("javax.net.debug", "plaintext");

        cookieHandler = EphemeralCookieHandler.newAcceptAll();
        
    }

    private HttpClient newClient() {
        return HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(cookieHandler)
                .build();
    }
    
    public void go() {
        try {
            HttpResponse<String> response = get(OSSRH_BASE_URL + "/service/local/authentication/login");
            System.out.println(response.statusCode());
            System.out.println(response.body());
            System.out.println("----");

            HttpResponse<String> r2 = uploadBundle(Paths.get("src/test/data/bundle.jar"));
            System.out.println(r2.statusCode());
            System.out.println(r2.body());
        } catch (IOException e) {
            throw new IllegalStateException("OSSHR access failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OSSHR access interrupted", e);
        }
    }

    private HttpResponse<String> get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", actionArguments.ossrhCredentials().asBasicAuth())
                .GET()
                .build();
        return newClient().send(request, BodyHandlers.ofString());
    }

    private HttpResponse<String> uploadBundle(Path jar) throws IOException, InterruptedException {
        // As per https://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.2, the marker should be ASCII
        // and not match anything in the encapsulated sections. Just using a random string (partly from the spec).
        String boundaryMarker = "AaB03xyz30BaA";
        String mimeBoundaryMarker = "\r\n";
        BodyPublisher formStartMarker = BodyPublishers.ofString("--" + boundaryMarker + mimeBoundaryMarker);
        BodyPublisher formEndMarker = BodyPublishers.ofString("--" + boundaryMarker + "--" + mimeBoundaryMarker);
        BodyPublisher formDisposition = BodyPublishers
                .ofString("Content-Disposition: form-data; name=\"file\"; filename=\"" + jar.getFileName() + "\"" + mimeBoundaryMarker);
//        String fileContentType = Files.probeContentType(jar);
        String fileContentType = "application/octet-stream";
        BodyPublisher formType = BodyPublishers.ofString("Content-Type: " + fileContentType + mimeBoundaryMarker);
        BodyPublisher boundary = BodyPublishers.ofString(mimeBoundaryMarker);

        BodyPublisher formData = BodyPublishers.ofFile(jar);
        BodyPublisher formComplete = BodyPublishers.concat(formStartMarker, formDisposition, formType, boundary, formData, boundary, formEndMarker);
        //BodyPublisher oneSection = BodyPublishers.fromPublisher(formComplete);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OSSRH_BASE_URL + "/service/local/staging/bundle_upload"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundaryMarker)
                .POST(formComplete)
                .build();
        return newClient().send(request, BodyHandlers.ofString());
    }
}
