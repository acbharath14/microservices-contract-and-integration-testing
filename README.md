# Microservices Contract and Integration Testing

Contract testing for microservices, demonstrated end to end: a loan-orchestration consumer calls three local services (order, underwriting, notification), every response is validated against a consumer-owned contract, and schema drift or timeouts surface as explicit release decisions instead of silent failures.

Dependencies are virtualized with WireMock so the whole thing runs locally with no external services.

## Architecture

```mermaid
flowchart LR
	Consumer[Loan Orchestration Consumer] --> Order[Order Decision Service]
	Consumer --> Underwriting[Underwriting Service]
	Consumer --> Notification[Notification Service]
	Order --> Validator[Contract Validator]
	Underwriting --> Validator
	Notification --> Validator
	Underwriting --> Fallback[Fallback Policy]
	Fallback --> Result[Release Decision]
	Validator --> Result
```

## Quick Start

```bash
python demo/contract_demo.py --scenario happy-path
```

PowerShell:

```powershell
.\run-demo.ps1 --scenario happy-path
```

## Key Scenarios

1. **Happy path** — all three services honor their contracts: `python demo/contract_demo.py --scenario happy-path`
2. **Schema drift** — the order service changes its payload and the run fails fast: `--scenario schema-drift`
3. **Timeout fallback** — underwriting times out, fallback policy kicks in, release decision degrades to warn: `--scenario timeout-fallback`

Run everything and generate the aggregated report:

```bash
python demo/run_release_suite.py
# writes reports/integration-suite-report.json
```

The report is machine-readable on purpose — it's designed to feed the release-governance evaluator in `cicd-quality-gates-reference-implementation`.

## What's Inside

- `contracts/` — consumer-owned JSON contracts for all three services
- `demo/` — runnable demo (`contract_demo.py`), contract validator, suite runner
- `integration-tests/` — Java integration tests (Maven): contract verification, dependency virtualization, provider API tests
- `virtual-services/` — WireMock stubs for the three dependencies
- `ci/` — GitLab CI, Jenkins, and GitHub Actions pipeline templates (the live GitHub workflow is `.github/workflows/ci.yml`; `ci/github-actions.yml` is its reference copy)
- `docs/` — architecture, risk map, and evidence from real runs

## What's implemented

- **Pact consumer-driven contracts** — `OrderConsumerPactTest` generates the pact, `OrderProviderPactTest` verifies the provider against it (Pact JVM 4.6.2)
- **OpenAPI schema validation** — `OpenApiValidationTest` validates provider responses against `openapi.yaml`
- **CI gate** — `.github/workflows/ci.yml` runs all contract tests on every push/PR; the Python release suite reports PASS/WARN/BLOCK per scenario

## Roadmap

- Contract version diffing with breaking-change classification

## License

MIT — see [LICENSE](LICENSE).
