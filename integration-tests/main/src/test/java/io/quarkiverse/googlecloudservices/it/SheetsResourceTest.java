package io.quarkiverse.googlecloudservices.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SheetsResourceTest {
    @Test
    public void testSheets() {
        given()
                .when().get("/sheets?spreadsheetId=test&range=A1:B2")
                .then()
                .statusCode(200)
                .body(equalTo("Hello,from;the,stub"));
    }
}
