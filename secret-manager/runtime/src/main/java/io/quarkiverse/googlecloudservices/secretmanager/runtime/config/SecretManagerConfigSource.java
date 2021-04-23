package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretVersionName;

import io.smallrye.config.common.AbstractConfigSource;

public class SecretManagerConfigSource extends AbstractConfigSource {

    /** The ordinal is set to < 100 (which is the default) so that this config source is retrieved from last. */
    private static final int SECRET_MANAGER_ORDINAL = 50;

    private static final String CONFIG_SOURCE_NAME = "io.quarkiverse.googlecloudservices.secretmanager.runtime.config";

    private final String defaultProject;

    private SecretManagerClientProvider clientProvider = new SecretManagerClientProvider();

    public SecretManagerConfigSource(String defaultProject) {
        super(CONFIG_SOURCE_NAME, SECRET_MANAGER_ORDINAL);
        this.defaultProject = defaultProject;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.emptySet();
    }

    @Override
    public String getValue(String propertyName) {
        SecretVersionName secretVersionName = SecretManagerConfigUtils.getSecretVersionName(propertyName, defaultProject);
        if (secretVersionName == null) {
            // The propertyName is not in the form "${sm//...}" so return null.
            return null;
        }

        AccessSecretVersionResponse response = clientProvider.get().accessSecretVersion(secretVersionName);
        return response.getPayload().getData().toStringUtf8();
    }
}
