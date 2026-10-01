package io.quarkiverse.googlecloudservices.it;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Minimal stub of the Gemini Developer API `generateContent` endpoint, so the Gen AI client can be tested without Google Cloud.
 */
@Path("/genai-stub")
public class GenAIStubResource {
    @POST
    @Path("{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public String generateContent(@PathParam("path") String path) {
        return """
                {
                  "candidates": [
                    {
                      "content": { "role": "model", "parts": [ { "text": "Hello from the stub" } ] },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """;
    }
}
