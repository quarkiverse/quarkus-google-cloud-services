package io.quarkiverse.googlecloudservices.secretmanager.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SecretManagerCredentialsProviderConfig {
        /**
         * The version of the secret which should be used for this credentials provider
         */
        @ConfigItem(defaultValue = "latest")
        public String secretVersion;

        /**
         * The name of the secret that should be used for this provider
         */
        @ConfigItem
        public String secretName;

        @Override
        public String toString() {
            return "CredentialsProviderConfig{" +
                    ", secretName='" + secretVersion + '\'' +
                    ", secretVersion='" + secretName + '\'' +
                    '}';
        }
}
