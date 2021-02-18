package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import com.google.cloud.secretmanager.v1.SecretVersionName;

/**
 * Utilities for parsing the secret manager URI string.
 */
public class SecretManagerConfigUtils {
    private static final String GCP_SECRET_PREFIX = "sm//";

    private SecretManagerConfigUtils() {
    }

    static SecretVersionName getSecretVersionName(String input, String defaultProjectId) {
        if (!input.startsWith(GCP_SECRET_PREFIX)) {
            return null;
        }

        String resourcePath = input.substring(GCP_SECRET_PREFIX.length());
        String[] tokens = resourcePath.split("/");

        String projectId = defaultProjectId;
        String secretId = null;
        String version = "latest";

        if (tokens.length == 1) {
            // property is form "sm//<secret-id>"
            secretId = tokens[0];
        } else if (tokens.length == 2) {
            // property is form "sm//<secret-id>/<version>"
            secretId = tokens[0];
            version = tokens[1];
        } else if (tokens.length == 3) {
            // property is form "sm//<project-id>/<secret-id>/<version-id>"
            projectId = tokens[0];
            secretId = tokens[1];
            version = tokens[2];
        } else if (tokens.length == 4
                && tokens[0].equals("projects")
                && tokens[2].equals("secrets")) {
            // property is form "sm//projects/<project-id>/secrets/<secret-id>"
            projectId = tokens[1];
            secretId = tokens[3];
        } else if (tokens.length == 6
                && tokens[0].equals("projects")
                && tokens[2].equals("secrets")
                && tokens[4].equals("versions")) {
            // property is form "sm//projects/<project-id>/secrets/<secret-id>/versions/<version>"
            projectId = tokens[1];
            secretId = tokens[3];
            version = tokens[5];
        } else {
            throw new IllegalArgumentException(
                    "Unrecognized format for specifying a GCP Secret Manager secret: " + input);
        }

        if (secretId.isEmpty() || projectId.isEmpty() || version.isEmpty()) {
            throw new IllegalArgumentException("The provided secret manager URI is invalid: " + input);
        }

        return SecretVersionName.newBuilder()
                .setProject(projectId)
                .setSecret(secretId)
                .setSecretVersion(version)
                .build();
    }
}
