# Microservices Contract and Integration Testing

Reference repo for consumer-driven contracts, service virtualization, and integration validation across microservices.

## Business Value
- Proves a loan-orchestration consumer can block unsafe releases when upstream payloads drift.
- Demonstrates local service virtualization across multiple dependencies to keep integration checks stable and fast.
- Shows how consumer-owned contracts can reduce downstream defects in regulated workflows.
- Shows how timeout handling and fallback policy can preserve workflow continuity without silently masking risk.

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

## What This Proves
- You can model multi-service integration risk, not just test a single endpoint.
- You understand contract drift, dependency isolation, and workflow-level correctness.
- You can make failure modes explicit enough to block a release decision.
- You can separate release-blocking failures from warn-level resilience events.

## Included In This Version
- Multi-service orchestration demo for order, underwriting, and notification workflows
- Consumer-owned JSON contracts for all three services
- Contract validator that checks required fields, types, and allowed values
- Happy-path scenario, intentional schema-drift failure scenario, and timeout-fallback scenario
- Machine-readable scenario and suite reports for downstream governance tools
- CI templates for GitLab and Jenkins for future pipeline integration

## Quick Start
```bash
python demo/contract_demo.py --scenario happy-path
```

PowerShell alternative:
```powershell
.\run-demo.ps1 --scenario happy-path
```

## Demonstrable Behavior
1. Simulates a loan-orchestration consumer calling three local services.
2. Validates each service response against a consumer-owned contract.
3. Passes a realistic happy path when all contracts are honored.
4. Fails fast when the order service introduces schema drift.
5. Degrades to a warn-level release decision when underwriting times out and fallback policy is applied.

## Key Scenarios
1. Happy path: `python demo/contract_demo.py --scenario happy-path`
2. Schema drift: `python demo/contract_demo.py --scenario schema-drift`
3. Timeout fallback: `python demo/contract_demo.py --scenario timeout-fallback`

## Suite Runner
Run all scenarios and generate an aggregated report:

```bash
python demo/run_release_suite.py
```

Generated report:
- `reports/integration-suite-report.json`

## Core Files
- contracts/order_contract.json
- contracts/underwriting_contract.json
- contracts/notification_contract.json
- demo/contract_demo.py
- demo/run_release_suite.py
- demo/contract_validator.py
- docs/architecture.md
- docs/risk-map.md

## Evidence
1. Success run: docs/evidence.md
2. Failure run: docs/evidence-failure.md
3. Fallback run: docs/evidence-timeout-fallback.md
4. Suite run: docs/evidence-suite.md

## Roadmap
1. Add retry budget and circuit-breaker style behavior for repeated dependency failures.
2. Add CI gate that fails pull requests on contract drift.
3. Add contract version diffing and breaking-change classification.
4. Add release readiness output written to machine-readable report files.
