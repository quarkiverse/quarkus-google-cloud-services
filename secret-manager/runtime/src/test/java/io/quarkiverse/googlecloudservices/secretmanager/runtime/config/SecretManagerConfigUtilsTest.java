package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.google.cloud.secretmanager.v1.SecretVersionName;

public class SecretManagerConfigUtilsTest {

    private static final String DEFAULT_PROJECT = "defaultProject";

    @Test
    public void testNonSecret() {
        String property = "some.non.secret.property.name";
        SecretVersionName secretIdentifier = SecretManagerConfigUtils.getSecretVersionName(property, DEFAULT_PROJECT);

        assertThat(secretIdentifier).isNull();
    }

    @Test
    public void testInvalidSecretFormat_missingSecretId() {
        String property = "sm//";

        assertThatThrownBy(() -> SecretManagerConfigUtils.getSecretVersionName(property, DEFAULT_PROJECT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testShortProperty_secretId() {
        String property = "sm//the-secret";
        SecretVersionName secretIdentifier = SecretManagerConfigUtils.getSecretVersionName(property, DEFAULT_PROJECT);

        assertThat(secretIdentifier.getProject()).isEqualTo("defaultProject");
        assertThat(secretIdentifier.getSecret()).isEqualTo("the-secret");
        assertThat(secretIdentifier.getSecretVersion()).isEqualTo("latest");
    }

    @Test
    public void testShortProperty_projectSecretId() {
        String property = "sm//the-secret/the-version";
        SecretVersionName secretIdentifier = SecretManagerConfigUtils.getSecretVersionName(property, DEFAULT_PROJECT);

        assertThat(secretIdentifier.getProject()).isEqualTo("defaultProject");
        assertThat(secretIdentifier.getSecret()).isEqualTo("the-secret");
        assertThat(secretIdentifier.getSecretVersion()).isEqualTo("the-version");
    }

    @Test
    public void testShortProperty_projectSecretIdVersion() {
        String property = "sm//my-project/the-secret/2";
        SecretVersionName secretIdentifier = SecretManagerConfigUtils.getSecretVersionName(property, DEFAULT_PROJECT);

        assertThat(secretIdentifier.getProject()).isEqualTo("my-project");
        assertThat(secretIdentifier.getSecret()).isEqualTo("the-secret");
        assertThat(secretIdentifier.getSecretVersion()).isEqualTo("2");
    }

    @Test
    public void testLongProperty_projectSecret() {
        String property = "sm//projects/my-project/secrets/the-secret";
        SecretVersionName secretIdentifier = SecretManagerConfigUtils.getSecretVersionName(property, DEFAULT_PROJECT);

        assertThat(secretIdentifier.getProject()).isEqualTo("my-project");
        assertThat(secretIdentifier.getSecret()).isEqualTo("the-secret");
        assertThat(secretIdentifier.getSecretVersion()).isEqualTo("latest");
    }

    @Test
    public void testLongProperty_projectSecretVersion() {
        String property = "sm//projects/my-project/secrets/the-secret/versions/3";
        SecretVersionName secretIdentifier = SecretManagerConfigUtils.getSecretVersionName(property, DEFAULT_PROJECT);

        assertThat(secretIdentifier.getProject()).isEqualTo("my-project");
        assertThat(secretIdentifier.getSecret()).isEqualTo("the-secret");
        assertThat(secretIdentifier.getSecretVersion()).isEqualTo("3");
    }

    @Test
    public void testRegionalSecret_projectLocationSecret() {
        String property = "sm//projects/my-project/locations/the-location/secrets/the-secret";
        SecretVersionName secretIdentifier = SecretManagerConfigUtils.getSecretVersionName(property, DEFAULT_PROJECT);

        assertThat(secretIdentifier.getProject()).isEqualTo("my-project");
        assertThat(secretIdentifier.getLocation()).isEqualTo("the-location");
        assertThat(secretIdentifier.getSecret()).isEqualTo("the-secret");
        assertThat(secretIdentifier.getSecretVersion()).isEqualTo("latest");
    }

    @Test
    public void testRegionalSecret_projectLocationSecretVersion() {
        String property = "sm//projects/my-project/locations/the-location/secrets/the-secret/versions/4";
        SecretVersionName secretIdentifier = SecretManagerConfigUtils.getSecretVersionName(property, DEFAULT_PROJECT);

        assertThat(secretIdentifier.getProject()).isEqualTo("my-project");
        assertThat(secretIdentifier.getLocation()).isEqualTo("the-location");
        assertThat(secretIdentifier.getSecret()).isEqualTo("the-secret");
        assertThat(secretIdentifier.getSecretVersion()).isEqualTo("4");
    }
}
