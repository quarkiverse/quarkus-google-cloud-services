package io.quarkiverse.googlecloudservices.common;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.google.cloud.PlatformInformation;
import com.google.cloud.ServiceDefaults;
import com.google.cloud.ServiceOptions;
import com.google.common.annotations.VisibleForTesting;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.PropertiesConfigSource;

public class GcpDefaultsConfigSourceFactory implements ConfigSourceFactory {

    private final Supplier<String> defaultProjectIdSupplier;

    public GcpDefaultsConfigSourceFactory() {
        this(ServiceOptionsHelper::getDefaultProjectId);
    }

    @VisibleForTesting
    GcpDefaultsConfigSourceFactory(Supplier<String> defaultProjectIdSupplier) {
        this.defaultProjectIdSupplier = defaultProjectIdSupplier;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        ConfigValue enableMetadataServer = context.getValue("quarkus.google.cloud.enable-metadata-server");
        if (enableMetadataServer.getValue() != null) {
            if (Converters.getImplicitConverter(Boolean.class).convert(enableMetadataServer.getValue())) {
                String defaultProjectId = defaultProjectIdSupplier.get();
                if (defaultProjectId != null) {
                    return singletonList(
                            new PropertiesConfigSource(Map.of("quarkus.google.cloud.project-id", defaultProjectId),
                                    "GcpDefaultsConfigSource",
                                    -Integer.MAX_VALUE));
                }
            }
        }
        return emptyList();
    }

    /**
     * This is a partial copy of {@link ServiceOptions} to prevent the use of Google's HTTP client, which causes
     * static initialization trouble via OpenCensus-shim with OpenTelemetry.
     *
     * <p>
     * This helper class is only intended to not use the Google HTTP client but not change any other aspects of
     * how the default project ID is retrieved.
     *
     * <p>
     * (The {@link ServiceOptions} class is licensed using ASL2.)
     */
    @SuppressWarnings("rawtypes")
    static class ServiceOptionsHelper extends ServiceOptions {
        @SuppressWarnings("unchecked")
        protected ServiceOptionsHelper(Class serviceFactoryClass, Class rpcFactoryClass, Builder builder,
                ServiceDefaults serviceDefaults) {
            super(serviceFactoryClass, rpcFactoryClass, builder, serviceDefaults);
            throw new UnsupportedOperationException();
        }

        @Override
        protected Set<String> getScopes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Builder toBuilder() {
            throw new UnsupportedOperationException();
        }

        public static String getDefaultProjectId() {
            // As in the original `ServiceOptions` class
            String projectId = System.getProperty("GOOGLE_CLOUD_PROJECT", System.getenv("GOOGLE_CLOUD_PROJECT"));
            if (projectId == null) {
                projectId = System.getProperty("GCLOUD_PROJECT", System.getenv("GCLOUD_PROJECT"));
            }

            if (projectId == null) {
                projectId = getAppEngineProjectId();
            }

            if (projectId == null) {
                projectId = getServiceAccountProjectId();
            }

            return projectId != null ? projectId : getGoogleCloudProjectId();
        }

        protected static String getAppEngineProjectId() {
            // As in the original `ServiceOptions` class
            String projectId;
            if (PlatformInformation.isOnGAEStandard7()) {
                projectId = getAppEngineProjectIdFromAppId();
            } else {
                projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
                if (projectId == null) {
                    projectId = System.getenv("GCLOUD_PROJECT");
                }

                if (projectId == null) {
                    projectId = getAppEngineProjectIdFromAppId();
                }

                if (projectId == null) {
                    try {
                        projectId = getAppEngineProjectIdFromMetadataServer();
                    } catch (IOException var2) {
                        // projectId = null;
                    }
                }
            }

            return projectId;
        }

        /**
         * This function has been changed to use the (new) Java HTTP client.
         */
        private static String getAppEngineProjectIdFromMetadataServer() throws IOException {
            String metadata = "http://metadata.google.internal";
            String projectIdURL = "/computeMetadata/v1/project/project-id";

            try {
                URI uri = new URI(metadata + projectIdURL);
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build();
                HttpRequest request = HttpRequest.newBuilder().timeout(Duration.ofMillis(500)).GET().uri(uri)
                        .header("Metadata-Flavor", "Google").build();
                HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
                HttpResponse<String> response = client.send(request, bodyHandler);
                return headerContainsMetadataFlavor(response) ? response.body() : null;
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
        }

        /**
         * This function has been adopted for the Java HTTP client.
         */
        private static boolean headerContainsMetadataFlavor(HttpResponse<?> response) {
            String metadataFlavorValue = response.headers().firstValue("Metadata-Flavor").orElse("");
            return "Google".equals(metadataFlavorValue);
        }
    }
}
