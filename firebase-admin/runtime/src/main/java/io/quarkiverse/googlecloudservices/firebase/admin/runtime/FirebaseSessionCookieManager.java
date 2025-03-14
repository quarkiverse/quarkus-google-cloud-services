package io.quarkiverse.googlecloudservices.firebase.admin.runtime;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.quarkiverse.googlecloudservices.firebase.admin.runtime.authentication.http.DefaultFirebaseIdentityProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.SessionCookieOptions;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Manages using cookie based authentication (see <a href="https://firebase.google.com/docs/auth/admin/manage-cookies#java_2">
 */
@ApplicationScoped
public class FirebaseSessionCookieManager {

    private static final Logger LOG = Logger.getLogger(FirebaseSessionCookieManager.class.getName());

    @Inject
    FirebaseAuthConfig config;

    @Inject
    FirebaseAuth firebaseAuth;

    @Inject
    IdentityProviderManager identityProviderManager;

    /**
     * Programmatically register routes for sending the cookie and clearing the cookie if the associated config
     * properties are set.
     *
     * @param router The router
     */
    public void registerRoutes(@Observes Router router) {
        var loginPath = config.auth().sessionCookie().loginApiPath();
        var logoutPath = config.auth().sessionCookie().logoutApiPath();

        loginPath.ifPresent(path -> router.route(HttpMethod.POST, path)
                .handler(this::sendCookieResponse));

        logoutPath.ifPresent(path -> router.route(HttpMethod.POST, path)
                .handler(this::clearCookieResponse));
    }

    /**
     * Handler method to send the cookie response based on an authenticated request.
     *
     * @param rc The routingContext.
     */
    public void sendCookieResponse(RoutingContext rc) {
        var sessionCookie = config.auth().sessionCookie();

        // Set session expiration to 5 days.
        long expiresIn = sessionCookie.expirationDuration().toMillis();
        SessionCookieOptions options = SessionCookieOptions.builder()
                .setExpiresIn(expiresIn)
                .build();

        var securityIdentity = QuarkusHttpUser.getSecurityIdentityBlocking(rc, identityProviderManager);
        var tokenCredential = securityIdentity.getCredential(TokenCredential.class);
        if (tokenCredential != null &&
                DefaultFirebaseIdentityProvider.FIREBASE_TOKEN_TYPE.equals(tokenCredential.getType())) {
            String idToken = tokenCredential.getToken();

            if (sessionCookie.validateToken()) {
                if (!validateTokenValidity(rc, idToken)) {
                    rc.response().setStatusCode(401);
                    rc.end();
                    return;
                }
            }

            try {
                // Create the session cookie. This will also verify the ID token in the process.
                // The session cookie will have the same claims as the ID token.
                String cookie = firebaseAuth.createSessionCookie(idToken, options);

                rc.response().addCookie(Cookie
                        .cookie(sessionCookie.name(), cookie)
                        .setHttpOnly(true)
                        .setMaxAge(expiresIn)
                        .setSameSite(CookieSameSite.STRICT)
                        .setSecure(true)
                        .setPath("/"));
                rc.response().setStatusCode(200);
            } catch (FirebaseAuthException e) {
                rc.response().setStatusCode(401);
            }
        } else {
            rc.response().setStatusCode(400);
        }
        rc.end();
    }

    /**
     * Handler method to clear the cookie response (equivalent to a logout)
     *
     * @param rc The routing context
     */
    public void clearCookieResponse(RoutingContext rc) {
        var sessionCookie = config.auth().sessionCookie();

        rc.response().addCookie(Cookie
                .cookie(sessionCookie.name(), "")
                .setHttpOnly(true)
                .setMaxAge(0)
                .setSameSite(CookieSameSite.STRICT)
                .setSecure(true)
                .setPath("/"));
        rc.end();
    }

    private boolean validateTokenValidity(RoutingContext rc, String idToken) {
        var sessionCookie = config.auth().sessionCookie();

        if (sessionCookie.minimumTokenValidity().isEmpty()) {
            LOG.error("Minimum token validity needs to be set in case token validation is enabled");
            rc.response().setStatusCode(500);
            return false;
        }

        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            long minimumDuration = sessionCookie.minimumTokenValidity().get().toMillis();
            long authTimeMillis = TimeUnit.SECONDS.toMillis(
                    (long) decodedToken.getClaims().get("auth_time"));

            // Only process if the user signed in within the minimum duration
            if (System.currentTimeMillis() - authTimeMillis < minimumDuration) {
                return true;
            } else {
                rc.response().setStatusCode(401);
                return false;
            }
        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve the session cookie from the routing context
     *
     * @param rc The routing context
     * @return The optional cookie
     */
    public Optional<String> getSessionCookie(RoutingContext rc) {
        var sessionCookie = config.auth().sessionCookie();

        return rc.request()
                .cookies()
                .stream()
                .filter(cookie -> cookie.getName().equals(sessionCookie.name()))
                .findFirst()
                .map(Cookie::getValue);
    }

    /**
     * Verify the session cookie and return a FirebaseToken based on the cookie or null
     *
     * @param cookie THe session cookie
     * @return The firebase token or null
     */
    public Future<FirebaseToken> verifySessionToken(String cookie) {
        var sessionCookie = config.auth().sessionCookie();

        // Verify the session cookie. In this case an additional check is added to detect
        // if the user's Firebase session was revoked, user deleted/disabled, etc.
        return firebaseAuth.verifySessionCookieAsync(cookie, sessionCookie.checkRevoked());
    }
}
