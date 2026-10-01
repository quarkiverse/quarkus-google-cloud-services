package io.quarkiverse.googlecloudservices.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import com.google.genai.Client;

@Path("/genai")
public class GenAIResource {
    @Inject
    Client client;

    @GET
    public String generate(@QueryParam("prompt") String prompt) {
        return client.models.generateContent("gemini-2.5-flash", prompt, null).text();
    }
}
