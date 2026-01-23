package io.quarkiverse.googlecloudservices.pubsub.push;

import java.util.Optional;

import org.jboss.logging.Logger;

import com.google.common.base.Strings;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * This filter perform authentication checks on Google PubSub push messages. For these messages, HTTP POST calls
 * are made on a specific endpoint using Google-Specific JWTs. This filter performs the checks as documented
 * <a href="https://cloud.google.com/pubsub/docs/authenticate-push-subscriptions">here</a> to validate the
 * calls originate from a PubSub subscription.
 * <p/>
 * No checks are performed for any other requests or in case the PubSub endpoint is not active.
 */
public class GooglePubSubAuthenticationHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = Logger.getLogger(GooglePubSubAuthenticationHandler.class);

    private final String pubSubEndpoint;
    private final Optional<String> verificationToken;
    private final String serviceAccountEmail;
    private final TokenVerifier verifier;

    public GooglePubSubAuthenticationHandler(String pubSubEndpoint,
            Optional<String> verificationToken,
            String serviceAccountEmail,
            TokenVerifier verifier) {
        this.pubSubEndpoint = pubSubEndpoint;
        this.verificationToken = verificationToken;
        this.serviceAccountEmail = serviceAccountEmail;
        this.verifier = verifier;
    }

    public void handle(RoutingContext rc) {
        LOGGER.debug("Checking for pubsub call");

        // Endpoint not configured, just move on
        if (pubSubEndpoint.isEmpty()) {
            LOGGER.trace("PubSub endpoint is empty, not checking authentication");
            rc.next();
            return;
        }

        // Not a Pubsub request, so ignore it and move on
        if (!rc.normalizedPath().startsWith(pubSubEndpoint)) {
            LOGGER.trace("Request was not targeted at PubSub endpoint, not checking authentication");
            rc.next();
            return;
        }

        LOGGER.debug("PubSub call detected, checking verification token");

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

        LOGGER.debug("Inspecting authorization header for pubsub request");

        var authorizationHeader = rc.request().headers().get("Authorization");
        // Remove auth header to prevent MP-JWT to handle it.
        rc.request().headers().remove("Authorization");

        if (Strings.isNullOrEmpty(authorizationHeader)) {
            LOGGER.error("No authentication header found, denying the pubsub message");
            rc.fail(401);
            return;
        }

        if (!authorizationHeader.startsWith("Bearer")) {
            LOGGER.error("Authentication header does not have required prefix, denying the pubsub message");
            rc.fail(401);
            return;
        }

        var token = authorizationHeader.substring("Bearer".length()).trim();
        LOGGER.debug("PubSub authorization header found, inspecting token");

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

            rc.next();
        } catch (Exception e) {
            LOGGER.error("Unkown error handling a pubsub push request, denying the pubsub message", e);
            rc.fail(401);
        }
    }
}
