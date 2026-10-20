package io.quarkiverse.googlecloudservices.it;

import java.io.IOException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

@Path("/sheets")
public class SheetsResource {
    @Inject
    Sheets sheets;

    @GET
    public String read(@QueryParam("spreadsheetId") String spreadsheetId, @QueryParam("range") String range)
            throws IOException {
        ValueRange response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        return response.getValues().stream()
                .map(row -> row.stream().map(Object::toString).collect(Collectors.joining(",")))
                .collect(Collectors.joining(";"));
    }
}
