package dk.mada.action;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import dk.mada.action.BundleCollector.Bundle;
import dk.mada.action.util.EphemeralCookieHandler;

/**
 * A proxy for OSSRH web service.
 */
public class OssrhProxy {
    /** The base URL for OSSRH. */
    private static final String OSSRH_BASE_URL = "https://s01.oss.sonatype.org";
    /** The action arguments, containing OSSRH credentials. */
    private final ActionArguments actionArguments;
    /** The http client. */
    private final HttpClient client;
    /** Flag for successful authentication with OSSRH. */
    private boolean isAuthenticated;

    /**
     * Constructs new instance.
     *
     * @param actionArguments the action arguments
     */
    public OssrhProxy(ActionArguments actionArguments) {
        this.actionArguments = actionArguments;

        CookieHandler cookieHandler = EphemeralCookieHandler.newAcceptAll();
        client = HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(cookieHandler)
                .build();
    }

    /**
     * Gets response from OSSHR service.
     *
     * @param path    the url path to read from
     * @param headers headers to use (paired values)
     * @return the response
     */
    public HttpResponse<String> get(String path, String... headers) {
        authenticate();
        try {
            return getRaw(path, headers);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading from " + path, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed while reading from " + path, e);
        }
    }

    /**
     * Uploads bundle.
     *
     * @param bundle the bundle to upload
     */
    public HttpResponse<String> uploadBundle(Bundle bundle) {
        authenticate();
        try {
            return uploadRaw(bundle.bundleJar());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while uploading bundle " + bundle, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed while uploading bundle " + bundle, e);
        }
    }

    /**
     * Gets data from a OSSRH path
     *
     * @param path    the url path to read from
     * @param headers headers to use (paired values)
     * @return the http response
     * @throws IOException          if the call failed
     * @throws InterruptedException if the call was interrupted
     */
    public HttpResponse<String> getRaw(String path, String... headers) throws IOException, InterruptedException {
        Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(OSSRH_BASE_URL + path))
                .timeout(Duration.ofSeconds(30));
        if (headers.length > 0) {
            builder.headers(headers);
        }
        HttpRequest request = builder
                .GET()
                .build();
        return client.send(request, BodyHandlers.ofString());
    }

    /**
     * Uploads file to OSSRH using POST multipart/form-data.
     *
     * @see https://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.2
     *
     * @param jar the file to upload
     * @return the http response
     * @throws IOException          if the access to data or upload failed
     * @throws InterruptedException if the upload was interrupted
     */
    private HttpResponse<String> uploadRaw(Path jar) throws IOException, InterruptedException {
        // As per the MIME spec, the marker should be ASCII and not match anything in the encapsulated sections.
        // Just using a random string (similar to the one in the forms spec).
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

    /**
     * Authenticate with the server which will provide a cookie used in the remaining calls.
     */
    private void authenticate() {
        if (isAuthenticated) {
            return;
        }

        HttpResponse<String> response = get("/service/local/authentication/login",
                "User-Agent", "jskov_action-maven-publish",
                "Authorization", actionArguments.ossrhCredentials().asBasicAuth());
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Failed authenticating: " + response.body());
        }
        isAuthenticated = true;
    }
}
