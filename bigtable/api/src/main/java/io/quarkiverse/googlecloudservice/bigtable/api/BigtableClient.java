package io.quarkiverse.googlecloudservice.bigtable.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Qualifies an injected Bigtable client.
 */
@Qualifier
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface BigtableClient {

    /**
     * Constant value for {@link #value()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * The name is used to configure the Bigtable client, e.g. the instance, project, etc.
     *
     * @return the client name
     */
    String value() default ELEMENT_NAME;

    final class Literal extends AnnotationLiteral<BigtableClient> implements BigtableClient {

        private static final long serialVersionUID = 1L;

        private final String value;

        /**
         * Creates a new instance of {@link Literal}.
         *
         * @param value the name of the Bigtable service, must not be {@code null}, must not be {@code blank}
         * @return the literal instance.
         */
        public static Literal of(String value) {
            return new Literal(value);
        }

        private Literal(String value) {
            this.value = value;
        }

        /**
         * @return the service name.
         */
        public String value() {
            return value;
        }
    }
}
