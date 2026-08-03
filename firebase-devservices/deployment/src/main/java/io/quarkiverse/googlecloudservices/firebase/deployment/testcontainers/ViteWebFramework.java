package io.quarkiverse.googlecloudservices.firebase.deployment.testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Vite's default dev server port, and therefore also the default port its HMR (hot module reload) client
     * tries to reconnect on. Firebase spawns {@code vite} without any CLI flags, so unless the project's own
     * {@code vite.config} overrides it, this is the port the dev server ends up listening on.
     */
    static final int DEFAULT_HMR_PORT = 5173;

    /**
     * Name of the environment variable the resolved HMR port is exposed under inside the container (see
     * {@link #apply(DockerfileBuilder)}), so {@code vite.config.js}/{@code vite.config.ts} can read it via
     * {@code process.env.VITE_HMR_PORT} instead of hardcoding the port number. Node.js (which runs the config
     * file, as opposed to Vite's client-side bundle) exposes the full process environment via {@code process.env},
     * so this works regardless of the {@code VITE_} prefix - that prefix only governs which variables Vite injects
     * into client-side code via {@code import.meta.env}, and is used here purely as a recognizable naming
     * convention.
     */
    static final String HMR_PORT_ENV_VAR = "VITE_HMR_PORT";

    /**
     * Best-effort match of a {@code server: { ..., port: <number>, ... }} block in a vite.config file. Only
     * catches the common literal-object-config case; deliberately not a full JS/TS parser.
     */
    private static final Pattern SERVER_PORT_PATTERN = Pattern
            .compile("server\\s*:\\s*\\{[^}]*?\\bport\\s*:\\s*(\\d+)", Pattern.DOTALL);

    /**
     * Best-effort match of {@code server.port} being read from {@link #HMR_PORT_ENV_VAR} via {@code process.env}.
     * When this matches, the config is guaranteed to be aligned with {@link #hmrPort}, since we set that same
     * environment variable to that same value - no need to fall back to guessing based on a literal number.
     */
    private static final Pattern ENV_VAR_USAGE_PATTERN = Pattern
            .compile("process\\.env\\." + HMR_PORT_ENV_VAR + "\\b");

    /**
     * Best-effort match of {@code strictPort: true} anywhere in a vite.config file.
     */
    private static final Pattern STRICT_PORT_TRUE_PATTERN = Pattern
            .compile("strictPort\\s*:\\s*true");

    private final Path hostingDirectory;
    private final int hmrPort;

    public ViteWebFramework(Path hostingDirectory, int hmrPort) {
        this.hostingDirectory = hostingDirectory;
        this.hmrPort = hmrPort;
    }

    /**
     * The port Vite's dev server (and therefore its HMR client) is published on, resolved from
     * {@code quarkus.google.cloud.devservices.firebase.hosting.vite.hmr-port} or {@link #DEFAULT_HMR_PORT}.
     *
     * @return The resolved HMR port
     */
    public int hmrPort() {
        return hmrPort;
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
        // directly to "<page-host>:<hmr-port>" rather than through the Hosting emulator's proxy, so the
        // externally reachable port must match the one Vite itself reports.
        builder.expose(hmrPort);

        // Let vite.config.js/vite.config.ts read the resolved port back, e.g.
        // `server: { port: Number(process.env.VITE_HMR_PORT) }`, instead of hardcoding it - keeping the two
        // settings in sync automatically rather than requiring the project to mirror our config value by hand.
        builder.env(HMR_PORT_ENV_VAR, String.valueOf(hmrPort));

        warnIfViteConfigLooksMismatched();
    }

    @Override
    public Set<Integer> additionalExposedPorts() {
        return Set.of(hmrPort);
    }

    /**
     * Best-effort sanity check of the project's {@code vite.config.js}/{@code vite.config.ts}: warns if it looks
     * like {@code server.port} doesn't match {@link #hmrPort}, or if {@code server.strictPort} isn't set to
     * {@code true}. This is a plain-text heuristic, not a JS/TS parser, so it can't reliably handle dynamic
     * configs (functions, spreads, imported constants, ...); it only flags the common literal-object case and
     * stays silent whenever it can't confidently tell - except when the config reads {@link #HMR_PORT_ENV_VAR}
     * back via {@code process.env}, which is recognized as a confirmed match rather than an unknown case.
     */
    private void warnIfViteConfigLooksMismatched() {
        var configFile = hostingDirectory.resolve("vite.config.ts");
        if (!Files.isRegularFile(configFile)) {
            configFile = hostingDirectory.resolve("vite.config.js");
        }
        if (!Files.isRegularFile(configFile)) {
            return;
        }

        String contents;
        try {
            contents = Files.readString(configFile);
        } catch (IOException e) {
            LOGGER.debug("Failed to read {} while checking the configured Vite HMR port", configFile, e);
            return;
        }

        if (ENV_VAR_USAGE_PATTERN.matcher(contents).find()) {
            // Reads the port back from the same environment variable we set it to - guaranteed to be aligned,
            // whatever the actual value is, so there's nothing left to check here.
            LOGGER.debug("{} reads server.port from the {} environment variable; port is guaranteed to be aligned",
                    configFile, HMR_PORT_ENV_VAR);
        } else {
            Matcher portMatcher = SERVER_PORT_PATTERN.matcher(contents);
            if (portMatcher.find()) {
                int configuredPort = Integer.parseInt(portMatcher.group(1));
                if (configuredPort != hmrPort) {
                    LOGGER.warn(
                            "{} sets server.port to {}, but the Firebase Dev Service is configured to publish port "
                                    + "{} (quarkus.google.cloud.devservices.firebase.hosting.vite.hmr-port). The "
                                    + "browser's Vite HMR client will try to reconnect on the wrong port. Align the "
                                    + "two values, or read server.port from the {} environment variable instead.",
                            configFile, configuredPort, hmrPort, HMR_PORT_ENV_VAR);
                }
            } else {
                LOGGER.debug("Could not detect an explicit server.port in {}; assuming it matches port {}",
                        configFile, hmrPort);
            }
        }

        if (!STRICT_PORT_TRUE_PATTERN.matcher(contents).find()) {
            LOGGER.warn(
                    "{} does not set server.strictPort: true. Without it, Vite may silently fall back to a "
                            + "different port than the one published by the Firebase Dev Service ({}) if that "
                            + "port turns out to be unavailable inside the container, breaking the HMR client's "
                            + "reconnection.",
                    configFile, hmrPort);
        }
    }
}
