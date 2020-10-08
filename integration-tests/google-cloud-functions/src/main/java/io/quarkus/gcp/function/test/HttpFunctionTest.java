package io.quarkus.gcp.function.test;

import java.io.Writer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

@ApplicationScoped
public class HttpFunctionTest implements HttpFunction {
    @Inject
    Storage storage;

    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        Bucket bucket = storage.get("quarkus-hello");
        Blob blob = bucket.get("hello.txt");
        Writer writer = httpResponse.getWriter();
        writer.write(new String(blob.getContent()));
    }
}
