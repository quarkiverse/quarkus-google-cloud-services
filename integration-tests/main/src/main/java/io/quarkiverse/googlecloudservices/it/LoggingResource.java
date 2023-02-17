package io.quarkiverse.googlecloudservices.it;

import java.time.Instant;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.LoggerFactory;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;

import io.quarkiverse.googlecloudservices.it.logging.KeyValueParameter;

@Path("/logging")
public class LoggingResource {

    private org.slf4j.Logger slf4j = LoggerFactory.getLogger(getClass());

    java.util.logging.Logger jul = java.util.logging.Logger.getLogger(getClass().getName());

    @Inject
    org.jboss.logging.Logger jboss;

    @Inject
    com.google.cloud.logging.Logging gcp;

    @GET
    @Path("/{payload}")
    @Produces(MediaType.TEXT_PLAIN)
    public String tryLog(@PathParam("payload") String p) {
        writeSlf4j(p);
        writeJBoss();
        writeJul(p);
        writeGcp(p);
        return "Hello " + p;
    }

    private void writeJul(String p) {
        jul.info("Mee too! Says JUL! Happily!");
    }

    private void writeGcp(String p) {
        gcp.write(List.of(LogEntry.newBuilder(Payload.StringPayload.of("Hello from GCP Logging " + p))
                .setSeverity(Severity.DEBUG)
                .setTimestamp(Instant.now())
                .build()));
    }

    private void writeJBoss() {
        try {
            throwExceptionWithCause();
        } catch (RuntimeException e) {
            jboss.error("Oh no says jboss!", e);
        }
    }

    private void writeSlf4j(String p) {
        slf4j.info("Hello from slf4j {}", p, KeyValueParameter.of("word", p));
    }

    private void throwExceptionWithCause() {
        try {
            throwOne();
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private void throwOne() {
        throw new RuntimeException("This is expected - it's a test exception");
    }
}
