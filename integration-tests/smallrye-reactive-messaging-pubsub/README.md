# Quarkiverse - Google Cloud Services - Integration Tests - Smallrye Reactive Messaging PubSub

**WARNING: this extension is experimental and rely on the experimental 
[smallrye-reactive-messaging-gcp-pubsub](https://github.com/smallrye/smallrye-reactive-messaging/tree/master/smallrye-reactive-messaging-gcp-pubsub) 
extension.**

To test it you first need to create a topic named `test-topic`

You can create one with `gcloud`:

```
gcloud pubsub topics create test-topic
```

As PubSub mandates the usage of the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to define its credentials, 
you need to set this one instead of relying on the `quarkus.google.cloud.service-account-location` property. 

```
export GOOGLE_APPLICATION_CREDENTIALS=<your-service-account-file>
```

You can then use `gcloud` to send a message to the topic:

```
gcloud pubsub topics publish test-topic --message SGVsbG8gV29ybGQ=
```

NOTE: `SGVsbG8gV29ybGQ=` is `Hello World` in BASE64.