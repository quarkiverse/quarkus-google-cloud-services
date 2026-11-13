package io.quarkiverse.googlecloudservices.it.accesstoken;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/** Test-only: accepts any "Authorization: Bearer <token>" header without validating it. */
@ApplicationScoped
public class BearerTokenMechanism implements HttpAuthenticationMechanism {

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        String header = context.request().getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Uni.createFrom().nullItem();
        }

        String token = header.substring("Bearer ".length());
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                .setPrincipal(() -> "test-user")
                .addCredential(new TokenCredential(token, "bearer"))
                .build());
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate", "Bearer"));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of();
    }
}
