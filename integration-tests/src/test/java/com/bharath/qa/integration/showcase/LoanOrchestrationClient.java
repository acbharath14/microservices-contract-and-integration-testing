package com.bharath.qa.integration.showcase;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * Minimal consumer for the loan-orchestration services, used only by the
 * WireMock showcase tests in this package.
 *
 * <p>It is test-scoped support code, not production code: it exists so the
 * showcase has a real consumer to point at the virtualized services. It
 * uses only {@code java.net.http.HttpClient}, so it needs no new
 * dependencies.</p>
 */
public class LoanOrchestrationClient {

    /**
     * Fallback underwriting payload, identical to
     * {@code UNDERWRITING_FALLBACK_PAYLOAD} in {@code demo/contract_demo.py}.
     * Returned when the underwriting call times out.
     */
    public static final String UNDERWRITING_FALLBACK_BODY =
            "{\"orderId\":123,\"decision\":\"REVIEW\",\"riskBand\":\"HIGH\"}";

    /** One response from a provider service: HTTP status plus the raw body. */
    public record ServiceResponse(int status, String body) {
    }

    private final HttpClient httpClient;
    private final String baseUri;
    private final int timeoutSeconds;

    /**
     * @param baseUri base URI of the virtualized services, for example
     *                {@code http://localhost:8080}
     * @param timeoutSeconds per-request timeout in seconds
     */
    public LoanOrchestrationClient(String baseUri, int timeoutSeconds) {
        this.baseUri = baseUri;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /** GET {@code /orders/123} with an {@code Accept: application/json} header. */
    public ServiceResponse getOrder() throws IOException, InterruptedException {
        return get("/orders/123");
    }

    /** GET {@code /notifications/123} with an {@code Accept: application/json} header. */
    public ServiceResponse getNotification() throws IOException, InterruptedException {
        return get("/notifications/123");
    }

    /**
     * GET {@code /underwriting/123}. If the call times out, return the
     * fallback payload instead of failing, mirroring the demo's fallback
     * policy. Any other failure propagates to the caller.
     */
    public ServiceResponse getUnderwritingWithFallback() throws IOException, InterruptedException {
        try {
            return get("/underwriting/123");
        } catch (HttpTimeoutException e) {
            return new ServiceResponse(200, UNDERWRITING_FALLBACK_BODY);
        }
    }

    private ServiceResponse get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + path))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new ServiceResponse(response.statusCode(), response.body());
    }
}
