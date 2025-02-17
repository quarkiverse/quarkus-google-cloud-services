package io.quarkiverse.googlecloudservices.firebase.admin.runtime.authentication.http;

import static io.vertx.ext.web.handler.impl.HTTPAuthorizationHandler.Type.BEARER;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkiverse.googlecloudservices.firebase.admin.runtime.FirebaseSessionCookieManager;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * This class is an implementation of HttpAuthenticationMechanism for Firebase authentication.
 * It provides methods to authenticate a user based on the provided token in the Authorization header.
 */
@ApplicationScoped
public class FirebaseSecurityAuthMechanism implements HttpAuthenticationMechanism {

    // The set of supported credential types
    static final Set<Class<? extends AuthenticationRequest>> credentialTypes = Set.of(FirebaseAuthenticationRequest.class);

    // The prefix of the Authorization header
    private static final String BEARER_PREFIX = BEARER + " ";

    // The lowercase version of the prefix of the Authorization header
    private static final String LOWERCASE_BASIC_PREFIX = BEARER_PREFIX.toLowerCase(Locale.ENGLISH);

    // The length of the prefix of the Authorization header
    private static final int PREFIX_LENGTH = BEARER_PREFIX.length();

    @Inject
    Instance<FirebaseSessionCookieManager> cookieManagerInstance;

    @ConfigProperty(name = "quarkus.google.cloud.firebase.auth.session-cookie.enabled")
    boolean sessionCookiesEnabled;

    /**
     * Authenticates the user using the provided token in the Authorization header of the request.
     *
     * @param context The RoutingContext of the request.
     * @param identityProviderManager The IdentityProviderManager to authenticate the request.
     * @return A Uni of SecurityIdentity representing the authenticated user or a null item if authentication fails.
     */
    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        List<String> authHeaders = context.request().headers().getAll(HttpHeaderNames.AUTHORIZATION);

        // Try to extract the token from the Authorization header
        if (authHeaders != null) {
            for (String current : authHeaders) {
                if (current.toLowerCase(Locale.ENGLISH).startsWith(LOWERCASE_BASIC_PREFIX)) {

                    // Extract the token from the header
                    String tokenChallenge = current.substring(PREFIX_LENGTH);

                    // We have found a suitable header, so try to authenticate
                    return identityProviderManager.authenticate(new FirebaseAuthenticationRequest(tokenChallenge, false));
                }
            }
        }

        if (sessionCookiesEnabled) {
            var manager = cookieManagerInstance.get();
            var cookie = manager.getSessionCookie(context);
            return cookie.map(c -> identityProviderManager
                    .authenticate(new FirebaseAuthenticationRequest(c, true))).orElse(Uni.createFrom().nullItem());
        }

        // No suitable header has been found in this request,
        return Uni.createFrom().nullItem();
    }

    /**
     * Sends an authentication challenge, if required.
     *
     * @param context The RoutingContext of the request.
     * @return A Uni of Boolean that is always false, as Firebase does not require an explicit challenge.
     */
    @Override
    public Uni<Boolean> sendChallenge(RoutingContext context) {
        return Uni.createFrom().item(false);
    }

    /**
     * Retrieves the challenge data, if any.
     *
     * @param context The RoutingContext of the request.
     * @return A Uni of ChallengeData that is always a null item, as Firebase does not require any challenge data.
     */
    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().nullItem();
    }

    /**
     * Retrieves the set of credential types supported by this mechanism.
     *
     * @return A Set of classes representing the supported credential types.
     */
    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return credentialTypes;
    }
}
