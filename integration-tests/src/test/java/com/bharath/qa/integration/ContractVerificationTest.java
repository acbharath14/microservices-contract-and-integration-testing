package com.bharath.qa.integration;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies each provider endpoint against its consumer-owned JSON contract
 * ({@code contracts/*_contract.json}, copied to test resources).
 *
 * <p>For every contract the test asserts: HTTP 200, every
 * {@code requiredFields} entry present with the declared type, and every
 * {@code allowedValues} entry respected by the live response.
 */
class ContractVerificationTest {

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

    @ParameterizedTest(name = "provider honors {0}")
    @ValueSource(strings = {
            "contracts/order_contract.json",
            "contracts/underwriting_contract.json",
            "contracts/notification_contract.json"
    })
    void providerHonorsConsumerContract(String contractResource) {
        JsonPath contract = loadContract(contractResource);

        String endpoint = contract.getString("endpoint");
        assertEquals("GET", contract.getString("method"),
                "only GET contracts are covered by this test");

        Response response = given()
                .baseUri(baseUri)
                .when()
                .get(endpoint)
                .then()
                .statusCode(200)
                .extract()
                .response();

        JsonPath body = response.jsonPath();

        Map<String, String> requiredFields = stringMap(contract, "requiredFields");
        for (Map.Entry<String, String> field : requiredFields.entrySet()) {
            Object value = body.get(field.getKey());
            assertNotNull(value, "contract field missing in response: " + field.getKey());
            switch (field.getValue()) {
                case "number" -> assertTrue(value instanceof Number,
                        "expected a JSON number for '" + field.getKey()
                                + "' but got " + value.getClass().getSimpleName());
                case "string" -> assertTrue(value instanceof String,
                        "expected a JSON string for '" + field.getKey()
                                + "' but got " + value.getClass().getSimpleName());
                default -> fail("unknown contract type '" + field.getValue()
                        + "' for field '" + field.getKey() + "'");
            }
        }

        Map<String, List<String>> allowedValues = listMap(contract, "allowedValues");
        for (Map.Entry<String, List<String>> entry : allowedValues.entrySet()) {
            String actual = body.getString(entry.getKey());
            assertTrue(entry.getValue().contains(actual),
                    "field '" + entry.getKey() + "' returned '" + actual
                            + "' which is not in contract allowed values "
                            + entry.getValue());
        }
    }

    private static JsonPath loadContract(String resource) {
        try (InputStream in = ContractVerificationTest.class
                .getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "contract not found on classpath: " + resource);
            return new JsonPath(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AssertionError("cannot read contract: " + resource, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> stringMap(JsonPath json, String path) {
        return (Map<String, String>) (Map<?, ?>) json.getMap(path);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> listMap(JsonPath json, String path) {
        return (Map<String, List<String>>) (Map<?, ?>) json.getMap(path);
    }
}
