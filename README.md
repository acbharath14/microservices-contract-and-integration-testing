# Microservices Contract and Integration Testing

Reference repo for consumer-driven contracts, service virtualization, and integration validation across microservices.

## What This Proves
- You can validate service boundaries, not just UI flows.
- You understand contract drift, resilience, and dependency isolation.
- You can operationalize microservices QA inside CI.

## Included in Day 1
- Runnable provider simulation using JDK HttpServer
- Rest Assured contract-style validation of real JSON responses
- WireMock-based dependency virtualization
- CI templates for GitLab and Jenkins

## Quick Start
```bash
python demo/contract_demo.py
```

## Demonstrable Behavior
1. Starts a local provider server and validates order payload fields.
2. Spins up a virtualized dependency endpoint and proves the consumer can isolate it.
3. Runs without external dependencies so it is immediately demonstrable.

## Roadmap
1. Consumer contract tests
2. Provider verification pipeline
3. Integration workflows with retries and failure simulation
4. CI quality gate for contract drift
