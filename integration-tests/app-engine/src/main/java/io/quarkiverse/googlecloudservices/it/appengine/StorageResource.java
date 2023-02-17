package io.quarkiverse.googlecloudservices.it.appengine;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

@Path("/storage")
public class StorageResource {

    @Inject
    Storage storage;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String storage() {
        Bucket bucket = storage.get("quarkus-hello");
        Blob blob = bucket.get("hello.txt");
        return new String(blob.getContent());
    }

}
