package com.bharath.qa.integration;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Consumer-driven contract test. Defines what
 * {@code loan-orchestration-service} expects from
 * {@code order-decision-service} and generates the pact file
 * (written to {@code target/pacts/} on a successful run).
 *
 * <p>The generated pact is verified provider-side by
 * {@link OrderProviderPactTest}.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "order-decision-service", port = "0")
class OrderConsumerPactTest {

    @Pact(consumer = "loan-orchestration-service")
    public RequestResponsePact getOrder(PactDslWithProvider builder) {
        return builder
                .given("order 123 exists")
                .uponReceiving("a request for order 123")
                    .path("/orders/123")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(new PactDslJsonBody()
                            .numberType("orderId", 123)
                            .stringType("status", "APPROVED")
                            .stringType("customerTier", "GOLD"))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getOrder")
    void consumerHonorsPact(MockServer mockServer) {
        given()
                .baseUri(mockServer.getUrl())
                .when()
                .get("/orders/123")
                .then()
                .statusCode(200)
                .body("orderId", equalTo(123))
                .body("status", equalTo("APPROVED"))
                .body("customerTier", equalTo("GOLD"));
    }
}
