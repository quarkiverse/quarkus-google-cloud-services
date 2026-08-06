package io.quarkiverse.googlecloudservices.secretmanager.runtime.config;

import com.google.cloud.secretmanager.v1.SecretVersionName;

import java.util.Arrays;

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

        if (input.contains("locations")) {
            return parseRegionalSecret(input, defaultProjectId);
        } else {
            return parseGlobalSecret(input, defaultProjectId);
        }
    }

    static private SecretVersionName parseRegionalSecret(String input, String defaultProjectId) {
        String projectId = defaultProjectId;
        String location = null;
        String secretId = null;
        String version = "latest";
        String[] tokens = tokenize(input);
        if ((tokens.length == 6 || tokens.length == 8)
                && tokens[0].equals("projects")
                && tokens[2].equals("locations")
                && tokens[4].equals("secrets")) {
            // property is form "sm//projects/<project-id>/locations/<location>/secrets/<secret>"
            projectId = tokens[1];
            location = tokens[3];
            secretId = tokens[5];
            if (tokens.length == 8 && tokens[6].equals("versions")) {
                // property is form "sm//projects/<project-id>/locations/<location>/secrets/<secret>/versions/<version>"
                version = tokens[7];
            }
        } else {
            illegalState(input);
        }
        return SecretVersionName.ofProjectLocationSecretSecretVersionName(
                projectId, location, secretId, version
        );
    }

    static private SecretVersionName parseGlobalSecret(String input, String defaultProjectId) {
        String projectId = defaultProjectId;
        String secretId = null;
        String version = "latest";
        String[] tokens = tokenize(input);
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
            illegalState(input);
        }
        assertURI(secretId, projectId, version, input);
        return SecretVersionName.ofProjectSecretSecretVersionName(projectId, secretId, version);
    }

    static private void assertURI(String secretId, String projectId, String version, String input) {
        if (secretId.isEmpty() || projectId.isEmpty() || version.isEmpty()) {
            throw new IllegalArgumentException("The provided secret manager URI is invalid: " + input);
        }
    }

    static private String[] tokenize(String input) {
        String resourcePath = input.substring(GCP_SECRET_PREFIX.length());
        return resourcePath.split("/");
    }

    static private void illegalState(String input) {
        throw new IllegalArgumentException(
                "Unrecognized format for specifying a GCP Secret Manager secret: " + input);
    }
}
