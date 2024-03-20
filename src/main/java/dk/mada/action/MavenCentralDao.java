package dk.mada.action;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.logging.Logger;

import dk.mada.action.util.EphemeralCookieHandler;

public class MavenCentralDao {
    private static Logger logger = Logger.getLogger(MavenCentralDao.class.getName());

    /** The action arguments, containing OSSRH credentials. */
    private final ActionArguments actionArguments;

    /**
     * Constructs new instance.
     *
     * @param actionArguments the action arguments
     */
    public MavenCentralDao(ActionArguments actionArguments) {
        this.actionArguments = actionArguments;
    }

    public void go() {

        try {
            CookieHandler cookieHandler = EphemeralCookieHandler.newAcceptAll();

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20))
                    .cookieHandler(cookieHandler)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://s01.oss.sonatype.org/service/local/authentication/login"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", actionArguments.ossrhCredentials().asBasicAuth())
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.println(response.statusCode());
            System.out.println(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("OSSHR access failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OSSHR access interrupted", e);
        }
    }
}
