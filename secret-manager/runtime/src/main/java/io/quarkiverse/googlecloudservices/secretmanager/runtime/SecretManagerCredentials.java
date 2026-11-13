package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Qualifies the {@link com.google.auth.Credentials} bean used by the Secret Manager client.
 * <p>
 * This allows Secret Manager to use its own authentication configuration without affecting the global
 * credentials shared by the other Google Cloud services.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, TYPE })
public @interface SecretManagerCredentials {
}
