package com.bharath.qa.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class ProviderApiTest {

    private static ProviderServer providerServer;

    @BeforeAll
    static void startProvider() throws IOException {
        providerServer = ProviderServer.start();
        given().baseUri("http://localhost:" + providerServer.port())
                .when().get("/orders/123")
                .then().statusCode(200);
    }

    @AfterAll
    static void stopProvider() {
        providerServer.stop();
    }

    @Test
    void returnsTheExpectedOrderRepresentation() {
        given()
                .baseUri("http://localhost:" + providerServer.port())
        .when()
                .get("/orders/123")
        .then()
                .statusCode(200)
                .body("orderId", equalTo(123))
                .body("status", equalTo("APPROVED"))
                .body("customerTier", equalTo("GOLD"));
    }
}
