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
    private final HttpClient client;

    /**
     * Constructs new instance.
     *
     * @param actionArguments the action arguments
     */
    public MavenCentralDao(ActionArguments actionArguments) {
        this.actionArguments = actionArguments;

        CookieHandler cookieHandler = EphemeralCookieHandler.newAcceptAll();
        client = HttpClient.newBuilder()
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

            HttpResponse<String> r2 = uploadBundle(Paths.get("./gradle/wrapper/gradle-wrapper.jar"));
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
        return client.send(request, BodyHandlers.ofString());
    }

    private HttpResponse<String> uploadBundle(Path jar) throws IOException, InterruptedException {
        String boundaryMarker = "AaB03xyz"; // arbitrary, from https://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.2
        BodyPublisher formMarker = BodyPublishers.ofString("-- " + boundaryMarker);
        BodyPublisher formDisposition = BodyPublishers
                .ofString("Content-Disposition: form-data; name=\"file\"; filename=\"" + jar.getFileName() + "\"");
        BodyPublisher formType = BodyPublishers.ofString("Content-Type: application/binary");
//        Content-Transfer-Encoding: binary
        BodyPublisher formData = BodyPublishers.ofFile(jar);

        BodyPublisher formComplete = BodyPublishers.concat(formMarker, formDisposition, formType, formData, formMarker);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OSSRH_BASE_URL + "/service/local/staging/bundle_upload"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundaryMarker)
                .POST(formComplete)
                .build();
        return client.send(request, BodyHandlers.ofString());
    }
}
