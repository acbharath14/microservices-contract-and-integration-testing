# Integration Risk Map

## High Risk
1. Order payload drift can break downstream decisioning and audit workflows.
2. Underwriting decision mismatches can cause incorrect customer outcomes.
3. Notification contract issues can hide operational failures after approval.
4. Underwriting timeouts can create ambiguous release confidence if fallback policy is not explicit.

## Quality Strategy
1. Consumer-owned contracts define required fields and allowed values.
2. Local virtualization keeps validation deterministic and runnable in CI.
3. Failure scenarios are explicit so contract breaks are visible and testable.
4. Resilience scenarios distinguish warn-level degradation from release-blocking failures.
