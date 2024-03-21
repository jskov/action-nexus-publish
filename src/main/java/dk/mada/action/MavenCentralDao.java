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
    private final HttpClient client;

    /**
     * Constructs new instance.
     *
     * @param actionArguments the action arguments
     */
    public MavenCentralDao(ActionArguments actionArguments) {
        this.actionArguments = actionArguments;
        System.setProperty("javax.net.debug", "plaintext");

        CookieHandler cookieHandler = EphemeralCookieHandler.newAcceptAll();
        client = HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(cookieHandler)
                .build();
    }

    public void go() {
        authenticate();

        HttpResponse<String> r2 = uploadBundle(Paths.get("src/test/data/bundle.jar"));
        System.out.println(r2.statusCode());
        System.out.println(r2.body());
    }

    private void authenticate() {
        try {
            HttpResponse<String> response = get(OSSRH_BASE_URL + "/service/local/authentication/login",
                    "Content-Type", "application/json",
                    "Authorization", actionArguments.ossrhCredentials().asBasicAuth());
            System.out.println(response.statusCode());
            System.out.println(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while authenticating", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed while authenticating", e);
        }
    }

    /**
     * Uploads bundle.
     *
     * @param bundle the bundle to upload
     */
    private HttpResponse<String> uploadBundle(Path bundle) {
        try {
            return uploadFile(bundle);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while uploading bundle " + bundle, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed while uploading bundle " + bundle, e);
        }
    }

    /**
     * Gets data from a URL
     *
     * @param url the url to read from
     * @param headers headers to use (paired values)
     * @return the http response
     * @throws IOException          if the call failed
     * @throws InterruptedException if the call was interrupted
     */
    private HttpResponse<String> get(String url, String... headers) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .headers(headers)
                .GET()
                .build();
        return client.send(request, BodyHandlers.ofString());
    }

    /**
     * Uploads file using POST multipart/form-data.
     *
     * @see https://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.2
     *
     * @param jar the file to upload
     * @return the http response
     * @throws IOException          if the access to data or upload failed
     * @throws InterruptedException if the upload was interrupted
     */
    private HttpResponse<String> uploadFile(Path jar) throws IOException, InterruptedException {
        // As per the MIME spec, the marker should be ASCII and not match anything in the encapsulated sections.
        // Just using a random string (similar to the forms spec).
        String boundaryMarker = "AaB03xyz30BaA";
        String mimeNewline = "\r\n";
        String formIntro = ""
                + "--" + boundaryMarker + mimeNewline
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + jar.getFileName() + "\"" + mimeNewline
                + "Content-Type: " + Files.probeContentType(jar) + mimeNewline
                + mimeNewline; // (empty line between form instructions and the data)

        String formOutro = ""
                + mimeNewline // for the binary data
                + "--" + boundaryMarker + "--" + mimeNewline;

        BodyPublisher body = BodyPublishers.concat(
                BodyPublishers.ofString(formIntro),
                BodyPublishers.ofFile(jar),
                BodyPublishers.ofString(formOutro));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OSSRH_BASE_URL + "/service/local/staging/bundle_upload"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundaryMarker)
                .POST(body)
                .build();
        return client.send(request, BodyHandlers.ofString());
    }
}
