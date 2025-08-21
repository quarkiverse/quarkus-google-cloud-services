package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import static io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers.FirebaseEmulatorContainer.DEFAULT_FIREBASE_VERSION;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class reads the firebase-tools version we want to use. If a package.json file is detected,
 * which contains a dependency on firebase-tools, this version will automatically be used.
 */
public class FirebaseToolsVersionReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseToolsVersionReader.class);

    public static String getFirebaseVersion() {
        var mapper = new ObjectMapper();

        LOGGER.info("Trying to auto detect the firebase-tools version to use");

        var packageJsonFile = new File("package.json");

        LOGGER.info("Trying to determine the firebase-tools version from package.json file at {}",
                packageJsonFile.getAbsolutePath());
        try (var reader = new BufferedReader(new InputStreamReader(new FileInputStream(packageJsonFile)))) {
            var packageJson = mapper.readValue(reader, PackageJson.class);

            return packageJson.getFirebaseToolsVersion()
                    .map(FirebaseToolsVersionReader::cleanSemVer)
                    .map(v -> {
                        LOGGER.info("Auto detect the firebase-tools version to use: {}", v);
                        return v;
                    })
                    .orElseGet(() -> {
                        LOGGER.info("Auto detect the firebase-tools version to use: no version found");
                        return DEFAULT_FIREBASE_VERSION;
                    });
        } catch (IOException e) {
            LOGGER.info("package.json file was not found, defaulting to latest version of firebase-tools");
            return DEFAULT_FIREBASE_VERSION;
        }
    }

    private static String cleanSemVer(String version) {
        if (version.startsWith("^") || version.startsWith("~")) {
            version = version.substring(1);
        }
        return version;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PackageJson {
        private Map<String, String> dependencies = new HashMap<>();
        private Map<String, String> devDependencies = new HashMap<>();

        public Optional<String> getFirebaseToolsVersion() {
            return Optional
                    .ofNullable(dependencies.get("firebase-tools"))
                    .or(() -> Optional.ofNullable(devDependencies.get("firebase-tools")));
        }

        public Map<String, String> getDependencies() {
            return dependencies;
        }

        public void setDependencies(Map<String, String> dependencies) {
            this.dependencies = dependencies;
        }

        public Map<String, String> getDevDependencies() {
            return devDependencies;
        }

        public void setDevDependencies(Map<String, String> devDependencies) {
            this.devDependencies = devDependencies;
        }
    }
}
