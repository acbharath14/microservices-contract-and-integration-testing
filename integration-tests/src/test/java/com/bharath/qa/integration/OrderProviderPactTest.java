package com.bharath.qa.integration;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junit5.HttpTestTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

/**
 * Verifies {@code order-decision-service} against the consumer-driven pact
 * in {@code src/test/resources/pacts/}. Every interaction in the pact
 * becomes a test case; provider states are satisfied by
 * {@link ProviderServer}, which serves the happy-path payloads.
 */
@Provider("order-decision-service")
@PactFolder("src/test/resources/pacts")
class OrderProviderPactTest {

    private ProviderServer provider;

    @BeforeEach
    void setUp(PactVerificationContext context) throws IOException {
        provider = ProviderServer.start();
        context.setTarget(new HttpTestTarget("localhost", provider.port(), "/"));
    }

    @AfterEach
    void tearDown() {
        if (provider != null) {
            provider.stop();
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("order 123 exists")
    public void order123Exists() {
        // ProviderServer serves order 123 unconditionally; nothing to set up.
    }
}
