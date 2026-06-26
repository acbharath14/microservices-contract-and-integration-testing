# Architecture

## Services
1. loan-orchestration-service: consumer that coordinates the workflow.
2. order-decision-service: source of order status and customer tier.
3. underwriting-service: virtualized dependency returning decision and risk band.
4. notification-service: virtualized dependency returning delivery status.

## Demonstrated Risks
1. Contract drift between consumer expectations and provider payload.
2. Dependency isolation using local virtualized services.
3. Workflow correctness across multiple service boundaries.
4. Dependency timeout that requires an explicit fallback policy and release warning.

## Scenarios
1. happy-path: all contracts are satisfied and the orchestration succeeds.
2. schema-drift: the provider breaks the contract and the consumer blocks the flow.
3. timeout-fallback: the underwriting dependency times out, the consumer applies fallback, and the release decision is downgraded to warn.
