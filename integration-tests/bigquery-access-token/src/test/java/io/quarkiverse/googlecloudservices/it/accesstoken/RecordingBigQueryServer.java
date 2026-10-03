package io.quarkiverse.googlecloudservices.it.accesstoken;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class RecordingBigQueryServer implements QuarkusTestResourceLifecycleManager {

    private HttpServer server;
    private final List<String> authHeaders = new CopyOnWriteArrayList<>();

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String body;
                if (exchange.getRequestURI().getPath().equals("/_captured")) {
                    body = String.join("\n", authHeaders);
                } else {
                    authHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
                    body = "{}";
                }

                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
            server.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start the recording BigQuery server", e);
        }

        String url = "http://localhost:" + server.getAddress().getPort();
        return Map.of("quarkus.google.cloud.bigquery.host-override", url);
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
