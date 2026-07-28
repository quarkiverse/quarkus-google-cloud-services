package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Detects projects using <a href="https://vite.dev">Vite</a> for the Firebase Hosting content, using the same
 * detection signals as firebase-tools itself (see {@code discover()} in
 * <a href="https://github.com/firebase/firebase-tools/blob/master/src/frameworks/vite/index.ts">
 * src/frameworks/vite/index.ts</a>): a {@code package.json} combined with either a {@code vite.config.js}/
 * {@code vite.config.ts} file or a {@code vite} dependency.
 * <p>
 * Running the Vite dev server through the Firebase Hosting emulator inside Docker breaks hot module reloading:
 * the hosting directory is a bind-mounted volume, which doesn't reliably deliver inotify events into the
 * container, so chokidar (used by Vite) never notices source changes on the host. On top of that, the dev
 * server's own port isn't published to the host by default, and the browser's HMR client connects to it
 * directly rather than through the Hosting emulator's proxy.
 */
public class ViteWebFramework implements FirebaseEmulatorContainer.WebFramework {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViteWebFramework.class);

    /**
     * Vite's default dev server port. Firebase spawns {@code vite} without any CLI flags, so unless the
     * project's own {@code vite.config} overrides it, this is the port the dev server ends up listening on.
     */
    static final int DEV_SERVER_PORT = 5173;

    private final Path hostingDirectory;

    public ViteWebFramework(Path hostingDirectory) {
        this.hostingDirectory = hostingDirectory;
    }

    @Override
    public String name() {
        return "Vite";
    }

    @Override
    public boolean detected() {
        LOGGER.info("Starting detection of Vite in hosting directory {}", hostingDirectory);
        var packageJson = hostingDirectory.resolve("package.json");
        if (!Files.isRegularFile(packageJson)) {
            LOGGER.info("No package.json file detected for vite support");
            return false;
        }

        var hasConfigFile = Files.isRegularFile(hostingDirectory.resolve("vite.config.js"))
                || Files.isRegularFile(hostingDirectory.resolve("vite.config.ts"));

        if (!hasConfigFile) {
            LOGGER.info("No Vite config file detected");
        }

        return hasConfigFile || declaresViteDependency(packageJson);
    }

    private boolean declaresViteDependency(Path packageJson) {
        try {
            var parsed = new ObjectMapper().readValue(packageJson.toFile(), FirebaseToolsVersionReader.PackageJson.class);
            return parsed.getDependencies().containsKey("vite") || parsed.getDevDependencies().containsKey("vite");
        } catch (IOException e) {
            LOGGER.debug("Failed to read {} while detecting Vite, treating as not detected", packageJson, e);
            return false;
        }
    }

    @Override
    public void apply(DockerfileBuilder builder) {
        // Bind-mounted hosting directories don't reliably deliver filesystem change events into the
        // container, so fall back to polling for chokidar-based watchers (used by Vite).
        builder.env("CHOKIDAR_USEPOLLING", "true");

        // Published on the same port number inside and outside the container: Vite's HMR client connects
        // directly to "<page-host>:<dev-server-port>" rather than through the Hosting emulator's proxy, so the
        // externally reachable port must match the one Vite itself reports.
        builder.expose(DEV_SERVER_PORT);
    }

    @Override
    public Set<Integer> additionalExposedPorts() {
        return Set.of(DEV_SERVER_PORT);
    }
}
