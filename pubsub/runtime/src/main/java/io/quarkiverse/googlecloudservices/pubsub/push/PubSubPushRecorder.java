package io.quarkiverse.googlecloudservices.pubsub.push;

import java.util.function.Consumer;

import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Recorder for creating a routeFunction, used to create a build item which handles the creation of the programmatic
 * endpoint for the push-endpoints.
 */
@Recorder
public class PubSubPushRecorder {

    public Consumer<Route> routeFunction() {
        var endpointHandler = CDI.current().select(PubSubPushEndpointHandler.class).get();

        return (route) -> route.method(HttpMethod.POST)
                .consumes("application/json")
                .handler(BodyHandler.create())
                .handler(endpointHandler);
    }

    public RuntimeValue<GooglePubSubAuthenticationHandler> authenticationHandler(String endpoint,
            PubSubPushBuildTimeConfig config) {
        var tokenVerifier = CDI.current().select(TokenVerifier.class).get();

        return new RuntimeValue<>(new GooglePubSubAuthenticationHandler(
                endpoint,
                config.verificationToken(),
                config.serviceAccountEmail().orElseThrow(),
                tokenVerifier));
    }

    public Handler<RoutingContext> authHandlerInstance(RuntimeValue<GooglePubSubAuthenticationHandler> authHandler) {
        return authHandler.getValue();
    }

}
