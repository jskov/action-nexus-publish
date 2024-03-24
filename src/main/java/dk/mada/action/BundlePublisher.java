package dk.mada.action;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.List;

import dk.mada.action.BundleCollector.Bundle;
import dk.mada.action.util.XmlExtractor;

/**
 * Publishes bundles and follow their state until stable.
 */
public class BundlePublisher {
    /** The expected prefix in response when creating new repository. */
    private static final String RESPONSE_REPO_URI_PREFIX = "{\"repositoryUris\":[\"https://s01.oss.sonatype.org/content/repositories/";
    /** Dummy id for unassigned repository. */
    private static final String REPO_ID_UNASSIGNED = "_unassigned_";
    /** The OSSRH proxy. */
    private final OssrhProxy proxy;

    public BundlePublisher(OssrhProxy proxy) {
        this.proxy = proxy;
    }

    public enum TargetAction {
        DROP,
        LEAVE,
        PUBLISH
    }

    public void publish(List<Bundle> bundles, TargetAction action) {
        List<BundleRepositoryState> initialBundleStates = bundles.stream()
                .map(this::uploadBundle)
                .toList();

        waitForRepositoriesToSettle(initialBundleStates);
    }

    private BundleRepositoryState uploadBundle(Bundle bundle) {
        HttpResponse<String> response = proxy.uploadBundle(bundle);
        return extractRepoId(bundle, response);
    }

    private void waitForRepositoriesToSettle(List<BundleRepositoryState> bundleStates) {
        List<BundleRepositoryState> updatedStates = bundleStates;
        do {
            // TODO: initial time by number of bundles
            // TODO: reduce wait for bundles with notifications (nearing conclusion)
            sleep(5000);
            updatedStates = updatedStates.stream()
                    .map(this::updateRepoState)
                    .toList();

        } while (updatedStates.stream().anyMatch(rs -> rs.status().isTransitioning()));
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
        if (!currentState.status.isTransitioning()) {
            return currentState;
        }

        // curl -v -H 'Accept: application/json' /tmp/cookies.txt
        // https://s01.oss.sonatype.org/service/local/staging/repository/dkmada-1104
        String repoId = currentState.assignedId;
        HttpResponse<String> response = proxy.get("/service/local/staging/repository/" + repoId);
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
        return new BundleRepositoryState(currentState.bundle(), newStatus, currentState.assignedId(), currentState.info());
    }

    private record RepositoryStateInfo(int notifications, boolean transitioning, String info) {
    }

    private RepositoryStateInfo parseRepositoryState(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body();
        if (status != HttpURLConnection.HTTP_OK) {
            return new RepositoryStateInfo(-1, false, "Failed repository probe; status: " + status + ", message: " + body);
        }
        XmlExtractor xe = new XmlExtractor(body);
        return new RepositoryStateInfo(xe.getInt("notifications"), xe.getBool("transitioning"), body);
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
     * Crudely extracts the assigned repository id from returned JSON.
     *
     * @param bundle   the bundle that was uploaded
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
            return new BundleRepositoryState(bundle, Status.FAILED_UPLOAD, REPO_ID_UNASSIGNED,
                    "Upload status: " + status + ", message: " + body);
        }
    }

}
