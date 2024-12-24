# Quarkus - Google Cloud Services - Integration Tests - Firebase Admin

This integration test show how to integrate with Firebase Admin SDK.

**WARNING**: It is disabled by default as it needs the emulator authenticated with a valid account to run.

## Installing gcloud CLI and Emulator Suite

1. Install the gcloud CLI following [this](https://cloud.google.com/sdk/docs/install) guide
2. Authenticate with your Google Account
```shell
$ gcloud auth login
```
3. Install the Local Emulator Suite using [this](https://firebase.google.com/docs/emulator-suite/install_and_configure#install_the_local_emulator_suite) instructions

## Running

1. Before running the integration tests, start the Local Emulator Suite using the following command:
```shell
$ firebase emulators:start --project demo-test-project-id
```

_After that, the emulator will start in the default port, which the application is configured to connect to. Since it is configured using the `demo-` prefix, all operations will be performed in the emulator environment only, without affecting any real projects. If needed, you will be able to use the Emulator UI at http://localhost:4000/auth._

2. In order to have the Admin SDK connecting to the emulator, set the following environment variable:
```shell
$ export FIREBASE_AUTH_EMULATOR_HOST="localhost:9099"
```