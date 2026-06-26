# Timeout Fallback Evidence

## Command

```bash
python demo/contract_demo.py --scenario timeout-fallback
```

## Output

```text
Orchestration scenario passed
Scenario: timeout-fallback
Order payload: {'orderId': 123, 'status': 'APPROVED', 'customerTier': 'GOLD'}
Underwriting payload: {'orderId': 123, 'decision': 'REVIEW', 'riskBand': 'HIGH'}
Notification payload: {'orderId': 123, 'channel': 'EMAIL', 'deliveryState': 'QUEUED'}
Release decision: WARN
Decision reason: Underwriting timed out and fallback policy was used
Fallback applied: True
```
