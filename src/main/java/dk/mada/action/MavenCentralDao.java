package dk.mada.action;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class MavenCentralDao {
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
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(actionArguments.ossrhUser(), actionArguments.ossrhToken().toCharArray());
                }
            };

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20))
                    .authenticator(authenticator)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://s01.oss.sonatype.org/service/local/authentication/login"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.println(response.statusCode());
            System.out.println(response.body());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
