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
import java.util.List;
import java.util.logging.Logger;

import dk.mada.action.BundleCollector.Bundle;
import dk.mada.action.util.EphemeralCookieHandler;

public class MavenCentralDao {
//    private static final String XML_BEGIN_NOTIFICATIONS = "<notifications>";
    private static Logger logger = Logger.getLogger(MavenCentralDao.class.getName());
    /** The expected prefix in reponse when creating new repository. */
    private static final String RESPONSE_REPO_URI_PREFIX = "{\"repositoryUris\":[\"https://s01.oss.sonatype.org/content/repositories/";
    /** Dummy id for unassigned repository. */
    private static final String REPO_ID_UNASSIGNED = "_unassigned_";
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
    public MavenCentralDao(ActionArguments actionArguments) {
        this.actionArguments = actionArguments;
//        System.setProperty("javax.net.debug", "plaintext");

        CookieHandler cookieHandler = EphemeralCookieHandler.newAcceptAll();
        client = HttpClient.newBuilder()
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .cookieHandler(cookieHandler)
                .build();
    }

    public void upload(Bundle bundle) {
        authenticate();

        BundleRepositoryState rs = uploadBundle(bundle);
        waitForRepositoriesToSettle(List.of(rs));
    }
    
    public void waitForRepositoriesToSettle(List<BundleRepositoryState> bundleStates) {
        List<BundleRepositoryState> updatedStates = bundleStates;
        while (updatedStates.stream().anyMatch(rs -> rs.status().isTransitioning())) {
            updatedStates = updatedStates.stream()
                    .map(this::updateRepoState)
                    .toList();

            sleep(15000);
        }
    }
    
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for repository state change", e);
        }
    }
    
    private BundleRepositoryState updateRepoState(BundleRepositoryState currentState) {
        if (currentState.status == Status.FAILED_UPLOAD
                || currentState.status == Status.FAILED_VALIDATION) {
            return currentState;
        }
        
        // curl -v -H 'Accept: application/json' /tmp/cookies.txt https://s01.oss.sonatype.org/service/local/staging/repository/dkmada-1104
        String repoId = currentState.assignedId;
        try {
            HttpResponse<String> response = get(OSSRH_BASE_URL + "/service/local/staging/repository/" + repoId);
            System.out.println("Got: " + response.statusCode() + " : " + response.body());
            RepositoryStateInfo x = parseRepositoryState(response);
            // TODO: get status update time
            // TODO: set user agent
            Status newStatus;
            if (x.transitioning) {
                newStatus = currentState.status();
            } else {
                if (x.notifications == 0) {
                    newStatus = Status.VALIDATED;
                } else {
                    newStatus = Status.FAILED_VALIDATION;
                }
            }
            System.out.println("NEW STATUS: " + newStatus);
            // does not break loop - state handling bad
            return new BundleRepositoryState(currentState.bundle(), newStatus, currentState.assignedId(), currentState.info());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while getting status for repository " + repoId, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed while getting status for repository " + repoId, e);
        }
    }

    private record RepositoryStateInfo(int notifications, boolean transitioning, String info) {
    }
    private RepositoryStateInfo parseRepositoryState(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body();
        if (status != HttpURLConnection.HTTP_OK) {
            return new RepositoryStateInfo(-1, false, "Failed repository probe; status: " + status + ", message: " + body);
        }
        return new RepositoryStateInfo(extractNotifications(body), extractTransitioning(body), body);
    }

    private boolean extractTransitioning(String body) {
        return body.contains("<transitioning>true</transitioning>");
    }
    
    private int extractNotifications(String body) {
        try {
            String notificationsBegin = "<notifications>";
            int start = body.indexOf(notificationsBegin) + notificationsBegin.length();
            int end = body.indexOf("</notifications>");
            String notificationsTxt = body.substring(start, end);
            return Integer.parseInt(notificationsTxt);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new IllegalStateException("Failed to extract notifications: " + body, e);
        }
    }
    
    
    public enum Status {
        // Terminal states
        FAILED_UPLOAD,
        FAILED_VALIDATION,
        UPLOADED,
        VALIDATED;
        
        boolean isTransitioning() {
            return this == UPLOADED || this == VALIDATED;
        }
    }
    
    public record BundleRepositoryState(Bundle bundle, Status status, String assignedId, String info) {
    }
    
    
    /**
     * Authenticate with the server which will provide a cookie used in the remaining calls.
     */
    private void authenticate() {
        if (isAuthenticated) {
            return;
        }

        try {
            HttpResponse<String> response = get(OSSRH_BASE_URL + "/service/local/authentication/login",
                    "User-Agent", "jskov_action-maven-publish",
                    "Authorization", actionArguments.ossrhCredentials().asBasicAuth());
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("Failed authenticating: " + response.body());
            }
            isAuthenticated = true;
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
    private BundleRepositoryState uploadBundle(Bundle bundle) {
        try {
            HttpResponse<String> response = uploadFile(bundle.bundleJar());
            return extractRepoId(bundle, response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while uploading bundle " + bundle, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed while uploading bundle " + bundle, e);
        }
    }

    /**
     * Crudely extracts the assigned repository id from returned JSON.
     *
     * @param bundle the bundle that was uploaded
     * @param response the HTTP response from OSRRH
     * @return the resulting bundle repository state
     */
    private BundleRepositoryState extractRepoId(Bundle bundle, HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body();

        if (status == HttpURLConnection.HTTP_CREATED && body.startsWith(RESPONSE_REPO_URI_PREFIX)) {
            String repoId = body.substring(RESPONSE_REPO_URI_PREFIX.length()).replace("\"]}", "");
            return new BundleRepositoryState(bundle, Status.UPLOADED, repoId, "Assigned id: " + repoId);
        } else {
            return new BundleRepositoryState(bundle, Status.FAILED_UPLOAD, REPO_ID_UNASSIGNED, "Upload status: " + status + ", message: " + body);
        }
    }

    /**
     * Gets data from a URL
     *
     * @param url     the url to read from
     * @param headers headers to use (paired values)
     * @return the http response
     * @throws IOException          if the call failed
     * @throws InterruptedException if the call was interrupted
     */
    private HttpResponse<String> get(String url, String... headers) throws IOException, InterruptedException {
        Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
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
