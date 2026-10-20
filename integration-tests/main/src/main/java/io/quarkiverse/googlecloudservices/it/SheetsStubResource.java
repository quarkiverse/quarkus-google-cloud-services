package io.quarkiverse.googlecloudservices.it;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Minimal stub of the Google Sheets API `spreadsheets.values.get` endpoint, so the Sheets client can be tested without
 * Google Cloud.
 */
@Path("/sheets-stub")
public class SheetsStubResource {
    @GET
    @Path("/v4/spreadsheets/{spreadsheetId}/values/{range}")
    @Produces(MediaType.APPLICATION_JSON)
    public String values(@PathParam("spreadsheetId") String spreadsheetId, @PathParam("range") String range) {
        return """
                {
                  "range": "Sheet1!A1:B2",
                  "majorDimension": "ROWS",
                  "values": [["Hello", "from"], ["the", "stub"]]
                }
                """;
    }
}
