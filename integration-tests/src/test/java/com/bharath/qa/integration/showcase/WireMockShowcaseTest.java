package com.bharath.qa.integration.showcase;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.restassured.path.json.JsonPath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * WireMock showcase: one test per WireMock feature, against the three
 * contracted services (order, underwriting, notification).
 *
 * <p>Each test verifies a behavior no other test in this repo verifies:
 * stubbing from files, delay-driven timeout with fallback, drift detection,
 * stateful stubs, request verification, and fault injection. Nothing here
 * re-asserts contract shape (that belongs to {@code ContractVerificationTest}),
 * OpenAPI conformance (that belongs to {@code OpenApiValidationTest}), or
 * Pact interactions.</p>
 *
 * <p>The class is tagged {@code showcase} and excluded from the default
 * suite run. Run it with {@code mvn test -Pwiremock-showcase}. It is a
 * demonstration of WireMock features, not counted coverage.</p>
 */
@Tag("showcase")
class WireMockShowcaseTest {

    private static final int CLIENT_TIMEOUT_SECONDS = 1;
    private static final int UNDERWRITING_DELAY_MILLIS = 3000;
    private static final String NOTIFICATION_SCENARIO = "notification-retry";

    /**
     * Drifted order payload, identical to {@code SCHEMA_DRIFT_SERVICES} in
     * {@code demo/contract_demo.py}: orderId is a string instead of a number,
     * and {@code customerTier} is renamed to {@code tier}.
     */
    private static final String DRIFTED_ORDER_BODY =
            "{\"orderId\":\"123\",\"status\":\"APPROVED\",\"tier\":\"GOLD\"}";

    private static WireMockServer wireMockServer;
    private static String orderApprovedJson;
    private static String underwritingApprovedJson;
    private static String notificationQueuedJson;

    private LoanOrchestrationClient client;

    @BeforeAll
    static void startWireMock() throws IOException {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        orderApprovedJson = readResource("showcase/order-approved.json");
        underwritingApprovedJson = readResource("showcase/underwriting-approved.json");
        notificationQueuedJson = readResource("showcase/notification-queued.json");
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        client = new LoanOrchestrationClient(
                "http://localhost:" + wireMockServer.port(), CLIENT_TIMEOUT_SECONDS);
    }

    @Test
    @DisplayName("WireMock serves the contracted order payload from a stub file")
    void wireMockServesOrderPayloadFromStubFile() throws IOException, InterruptedException {
        wireMockServer.stubFor(get(urlEqualTo("/orders/123"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(orderApprovedJson)));

        LoanOrchestrationClient.ServiceResponse response = client.getOrder();

        assertEquals(200, response.status());
        assertEquals(orderApprovedJson, response.body());
    }

    @Test
    @DisplayName("Underwriting timeout falls back to the review decision")
    void underwritingTimeoutFallsBackToReviewDecision() throws IOException, InterruptedException {
        wireMockServer.stubFor(get(urlEqualTo("/underwriting/123"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(underwritingApprovedJson)
                        .withFixedDelay(UNDERWRITING_DELAY_MILLIS)));

        LoanOrchestrationClient.ServiceResponse response = client.getUnderwritingWithFallback();

        assertEquals(200, response.status());
        JsonPath body = new JsonPath(response.body());
        assertEquals("REVIEW", body.getString("decision"));
        assertEquals("HIGH", body.getString("riskBand"));
    }

    @Test
    @DisplayName("Schema drift in the order payload is detected")
    void schemaDriftInOrderPayloadIsDetected() throws IOException, InterruptedException {
        wireMockServer.stubFor(get(urlEqualTo("/orders/123"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(DRIFTED_ORDER_BODY)));

        JsonPath contract = loadContract("contracts/order_contract.json");
        assertEquals("number", contract.getString("requiredFields.orderId"),
                "contract requires orderId to be a number");

        LoanOrchestrationClient.ServiceResponse response = client.getOrder();

        Object orderId = new JsonPath(response.body()).get("orderId");
        assertFalse(orderId instanceof Number,
                "drift detected: orderId should be a number per the contract, got: " + orderId);
    }

    @Test
    @DisplayName("A transient notification failure succeeds on the second call")
    void transientNotificationFailureSucceedsOnSecondCall() throws IOException, InterruptedException {
        wireMockServer.stubFor(get(urlEqualTo("/notifications/123"))
                .inScenario(NOTIFICATION_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("failed-once"));
        wireMockServer.stubFor(get(urlEqualTo("/notifications/123"))
                .inScenario(NOTIFICATION_SCENARIO)
                .whenScenarioStateIs("failed-once")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(notificationQueuedJson)));

        LoanOrchestrationClient.ServiceResponse first = client.getNotification();
        assertEquals(500, first.status());

        LoanOrchestrationClient.ServiceResponse second = client.getNotification();
        assertEquals(200, second.status());
        assertEquals(notificationQueuedJson, second.body());
    }

    @Test
    @DisplayName("The consumer sends the contracted order request")
    void consumerSendsContractedOrderRequest() throws IOException, InterruptedException {
        wireMockServer.stubFor(get(urlEqualTo("/orders/123"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(orderApprovedJson)));

        LoanOrchestrationClient.ServiceResponse response = client.getOrder();

        assertEquals(200, response.status());
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/orders/123"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    @DisplayName("A reset connection surfaces as an I/O failure")
    void resetConnectionSurfacesAsIoFailure() {
        wireMockServer.stubFor(get(urlEqualTo("/notifications/123"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThrows(IOException.class, () -> client.getNotification());
    }

    private static String readResource(String resource) throws IOException {
        try (InputStream in = WireMockShowcaseTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "resource not found on classpath: " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static JsonPath loadContract(String resource) {
        try (InputStream in = WireMockShowcaseTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "contract not found on classpath: " + resource);
            return new JsonPath(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AssertionError("cannot read contract: " + resource, e);
        }
    }
}
