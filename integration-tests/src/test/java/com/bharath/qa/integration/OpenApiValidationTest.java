package com.bharath.qa.integration;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.restassured.RestAssured.given;

/**
 * Validates the provider's live responses against
 * {@code src/test/resources/openapi.yaml}. The filter fails the test on
 * any request/response that drifts from the spec: missing fields, wrong
 * types, unexpected enum values, or wrong status codes.
 */
class OpenApiValidationTest {

    private static final OpenApiValidationFilter VALIDATION_FILTER =
            new OpenApiValidationFilter("openapi.yaml");

    private static ProviderServer provider;
    private static String baseUri;

    @BeforeAll
    static void startProvider() throws IOException {
        provider = ProviderServer.start();
        baseUri = "http://localhost:" + provider.port();
    }

    @AfterAll
    static void stopProvider() {
        if (provider != null) {
            provider.stop();
        }
    }

    @Test
    void orderResponseMatchesOpenApiSpec() {
        given()
                .filter(VALIDATION_FILTER)
                .baseUri(baseUri)
                .when()
                .get("/orders/123")
                .then()
                .statusCode(200);
    }
}
