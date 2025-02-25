package io.quarkiverse.googlecloudservices.pubsub;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.google.common.base.Strings;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

/**
 * The filter perform authentication checks on Google PubSub push messages. For these messages, HTTP POST calls
 * are made on a specific endpoint using Google-Specific JWTs. This filter performs the checks as documented
 * <a href="https://cloud.google.com/pubsub/docs/authenticate-push-subscriptions">here</a> to validate the
 * calls originate from a PubSub subscription.
 * <p/>
 * No checks are performed for any other requests or in case the PubSub endpoint is not active.
 */
@ApplicationScoped
public class GooglePubSubAuthenticationFilter {

    private static final Logger LOGGER = Logger.getLogger(GooglePubSubAuthenticationFilter.class);

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.push.endpoint-path")
    Optional<String> pubSubEndpoint;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.push.verification-token")
    Optional<String> verificationToken;

    @ConfigProperty(name = "quarkus.google.cloud.pubsub.push.service-account-email")
    String serviceAccountEmail;

    @Inject
    TokenVerifier verifier;

    @RouteFilter(1000)
    public void filter(RoutingContext rc) {
        // Endpoint not configured, just move on
        if (pubSubEndpoint.isEmpty()) {
            LOGGER.trace("PubSub endpoint is empty, not checking authentication");
            rc.next();
            return;
        }

        // Not a Pubsub request, so ignore it and move on
        if (!rc.normalizedPath().startsWith(pubSubEndpoint.get())) {
            LOGGER.trace("Request was not targeted at PubSub endpoint, not checking authentication");
            rc.next();
            return;
        }

        if (verificationToken.isPresent()) {
            String pubsubVerificationToken = verificationToken.get();
            var receivedToken = rc.queryParam("token");
            // Do not process message if request token does not match pubsubVerificationToken
            if (receivedToken.size() != 1 || pubsubVerificationToken.compareTo(rc.queryParam("token").get(0)) != 0) {
                LOGGER.warn("The request did not contain the required verification token, denying the pubsub message");
                rc.fail(401);
                return;
            }
        } else {
            LOGGER.debug("No verification token configured, ignoring check");
        }

        var authorizationHeader = rc.request().headers().get("Authorization");
        if (Strings.isNullOrEmpty(authorizationHeader) || authorizationHeader.split(" ").length != 2) {
            LOGGER.error("No (valid) authentication header found, denying the pubsub message");
            rc.fail(401);
            return;
        }

        var token = authorizationHeader.split(" ")[1];

        try {
            var idToken = verifier.verify(token);
            if (idToken == null) {
                LOGGER.error("No valid authentication header found, denying the pubsub message");
                rc.fail(401);
                return;
            }

            if (!Boolean.TRUE.equals(idToken.getPayload().getEmailVerified())) {
                LOGGER.error("The email verified flag in the token payload was not set, denying the pubsub message.");
                rc.fail(401);
                return;
            }

            if (!serviceAccountEmail.equals(idToken.getPayload().getEmail())) {
                LOGGER.error("Incorrect service account email detected, denying the pubsub message.");
                rc.fail(401);
                return;
            }

            LOGGER.debug("Message was correctly authorized, passing the message on for handling");

            // Remove auth header to prevent MP-JWT to handle it.
            rc.request().headers().remove("Authorization");
            rc.next();
        } catch (Exception e) {
            LOGGER.error("Unkown error handling a pubsub push request, denying the pubsub message", e);
            rc.fail(401);
        }
    }
}
