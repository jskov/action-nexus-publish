package dk.mada.action.util;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

/**
 * A cookie handler that dies with the JVM (no external storage). All cookies are accepted.
 */
public final class EphemeralCookieHandler {
    private static Logger logger = Logger.getLogger(EphemeralCookieHandler.class.getName());

    private EphemeralCookieHandler() {
    }

    /** {@return a new cookie handler that accepts all cookies} */
    public static CookieHandler newAcceptAll() {
        return new CookieManager(new EphemeralCookieStore(), new AcceptAllCookiesPolicy());
    }

    /**
     * An ephemeral cookie store, good for the life-time of the JVM.
     */
    private static final class EphemeralCookieStore implements CookieStore {
        @Override
        public void add(URI uri, HttpCookie cookie) {
            logger.info(() -> "cookie: add url:" + uri + ", name:" + cookie.getName());
        }

        @Override
        public List<HttpCookie> get(URI uri) {
            logger.info(() -> "cookie get: " + uri);
            return List.of();
        }

        @Override
        public List<HttpCookie> getCookies() {
            logger.info("cookie: get all");
            return List.of();
        }

        @Override
        public List<URI> getURIs() {
            logger.info("cookie: get uris");
            return List.of();
        }

        @Override
        public boolean remove(URI uri, HttpCookie cookie) {
            logger.info(() -> "cookie: remove uri:" + uri + ", name:" + cookie.getName());
            return false;
        }

        @Override
        public boolean removeAll() {
            logger.info("cookie: clear");
            return false;
        }
    }

    /**
     * An accept-all cookie policy.
     */
    private static final class AcceptAllCookiesPolicy implements CookiePolicy {
        @Override
        public boolean shouldAccept(URI uri, HttpCookie cookie) {
            return true;
        }
    }
}
