package io.quarkiverse.googlecloudservices.firebase.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Root configuration class for Google Cloud Firebase that operates at build time.
 * This class provides a nested structure for configuration, including
 * a separate group for the development service configuration.
 * <p>
 * Here is an example of how to configure these properties:
 * <p>
 *
 * <pre>
 * quarkus.google.cloud.firebase.devservice.enabled = true
 * quarkus.google.cloud.pubsub.devservice.image-name = gcr.io/google.com/cloudsdktool/google-cloud-cli # optional
 * quarkus.google.cloud.pubsub.devservice.emulatorPort = 8085 # optional
 * </pre>
 */
@ConfigMapping(prefix = "quarkus.google.cloud")
@ConfigRoot
public interface FirebaseDevServiceConfig {

    /**
     * Google Cloud project ID. The project is required to be set if you use the Firebase Auth Dev service.
     */
    Optional<String> projectId();

    /**
     * Configure the generic Firebase settings
     */
    Firebase firebase();

    /**
     * Configure the realtime database
     */
    Database database();

    /**
     * Configure the firestore
     */
    Firestore firestore();

    /**
     * Configure google cloud functions
     */
    Functions functions();

    /**
     * Configure Google Cloud Pub/Sub
     */
    PubSub pubsub();

    /**
     * Configure Google Cloud Storage
     */
    Storage storage();

    interface Firebase {

        /**
         * Configuration for the firebase emulator devservice. This is the generic configuration for the firebase
         * emulator. THe specifics are handled in each of the other dev services.
         */
        DevService devservice();

        /**
         * Configure the firebase auth settings
         */
        Auth auth();

        /**
         * Configure Firebase Hosting
         */
        Hosting hosting();

        interface DevService {

            /**
             * Indicates to use the dev service for Firebase. The default value is true. This indicator is used
             * to detect the Firebase DevService and disable the DevServices for extensions which conflict with the
             * Firebase DevService.
             */
            @WithDefault("true")
            boolean preferFirebaseDevServices();

            /**
             * Sets the Docker image name for the Google Cloud SDK.
             * This image is used to emulate the Pub/Sub service in the development environment.
             * The default value is 'node:23-alpine'.
             * <p>
             * See also the documentation on Custom Docker images for more info about this image.
             */
            @WithDefault("node:23-alpine")
            String imageName();

            /**
             * Id of the docker user to run the firebase executable. This is needed in environments where Docker
             * does not perform a mapping to the user running Docker. In a Docker Desktop setup, Docker
             * automatically performs this mapping and the data written by the emulator can be read by the user
             * running the build. This is not the case in a regular (non-Desktop) setup,
             * so you may need to set the user id and {@link #dockerGroup()}. This option is often needed in CI
             * environments.
             */
            Optional<Integer> dockerUser();

            /**
             * Id of the group to which the {@link #dockerUser()} belongs.
             */
            Optional<Integer> dockerGroup();

            /**
             * The version of the firebase tools to use. Default is to use the latest available version.
             */
            @WithDefault("latest")
            String firebaseVersion();

            /**
             * The token to use for firebase authentication. Run `firebase login:ci` locally to get a new token. This
             * option is mandatory if you use firebase hosting.
             */
            Optional<String> token();

            /**
             * Indicate to use a custom firebase.json file instead of the automatically generated one. The custom
             * firebase.json file MUST include a setting of
             *
             * <pre>
             * "host" : "0.0.0.0"
             * </pre>
             *
             * to ensure the ports of the
             * emulator are exposed correctly at the docker container level.
             * <p>
             * See the section on Custom Firebase Json in the docs for more info.
             */
            Optional<String> customFirebaseJson();

            /**
             * Sets the JAVA tool options for emulators based on the Java runtime environment like -Xmx.
             * See also
             * <a href="https://firebase.google.com/docs/emulator-suite/install_and_configure#specifying_java_options">here</a>
             */
            Optional<String> javaToolOptions();

            /**
             * Allow to import and export data. Specify a path relative to the current working directory of the executable
             * (for most unit tests, this is the root of the build directory) to be used for import and export of emulator
             * data. The data will be written to a subdirectory called "emulator-data" of this directory.
             * See also <a href=
             * "https://firebase.google.com/docs/emulator-suite/install_and_configure#export_and_import_emulator_data">here</a>
             */
            Optional<String> emulatorData();

            /**
             * Settings for the emulator UI
             */
            UI ui();

            interface UI extends GenericDevService {

                /**
                 * Indicates whether the service should be enabled or not.
                 * The default value is 'false'.
                 */
                @WithDefault("true")
                @Override
                boolean enabled();

                /**
                 * Port on which to expose the logging endpoint port. This is needed in case you want to view the logging
                 * via the Emulator UI.
                 */
                Optional<Integer> loggingPort();

                /**
                 * Port on which to expose the hub endpoint port. This is needed if you want to use the hub API of
                 * the Emulator UI.
                 */
                Optional<Integer> hubPort();
            }

        }
    }

    interface Auth {

        /**
         * Configuration for the firebase auth emulator
         */
        GenericDevService devservice();
    }

    interface Database {

        /**
         * Configuration for the realtime database emulator
         */
        GenericDevService devservice();
    }

    interface Firestore {

        /**
         * Configuration for the Firestore emulator
         */
        FirestoreDevService devservice();

        /**
         * Extension for the Firestore dev service. This service can also configure the websocket port.
         */
        interface FirestoreDevService extends GenericDevService {

            /**
             * Port on which to expose the websocket port. This is needed in case the Firestore Emulator UI needs is
             * used.
             */
            Optional<Integer> websocketPort();
        }
    }

    interface Functions {

        /**
         * Configuration for the Functions emulator
         */
        GenericDevService devservice();
    }

    interface Hosting {

        /**
         * Configuration for the hosting emulator
         */
        GenericDevService devservice();

        /**
         * Path to the hosting files.
         */
        Optional<String> hostingPath();
    }

    interface PubSub {

        /**
         * Configuration for the pubsub emulator
         */
        GenericDevService devservice();
    }

    interface Storage {

        /**
         * Configuration for the storage emulator
         */
        GenericDevService devservice();
    }

    /**
     * Internal interface representing a dev service for each of the different emulators part of the Firebase
     * platform.
     */
    interface GenericDevService {

        /**
         * Indicates whether the DevService should be enabled or not.
         * The default value is 'false'.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Specifies the emulatorPort on which the service should run in the development environment. The default
         * is to expose the service on a random port.
         */
        Optional<Integer> emulatorPort();
    }

}
