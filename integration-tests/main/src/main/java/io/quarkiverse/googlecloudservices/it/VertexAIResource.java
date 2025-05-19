package io.quarkiverse.googlecloudservices.it;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

@Path("/vertexai")
public class VertexAIResource {
    @Inject
    VertexAI vertexAI;

    @GET
    public String predict(@QueryParam("prompt") String prompt) throws IOException {
        var model = new GenerativeModel("gemini-2.0-flash-001", vertexAI);
        var response = model.generateContent(prompt);

        return response.toString();
    }
}
