package io.quarkiverse.googlecloudservices.it;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

@Path("/storage")
public class StorageResource {
    private static final String BUCKET = "quarkus-hello";

    @Inject
    Storage storage;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String storage() {
        BlobInfo blobInfo = BlobInfo.newBuilder(BUCKET, "test-upload").build();
        storage.create(blobInfo, "test".getBytes());

        Bucket bucket = storage.get(BUCKET);
        Blob blob = bucket.get("hello.txt");
        return new String(blob.getContent());
    }

}
