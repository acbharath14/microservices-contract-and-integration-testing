# Evidence

## Command

```bash
python demo/contract_demo.py --scenario happy-path
```

## Output

```text
Orchestration scenario passed
Scenario: happy-path
Order payload: {'orderId': 123, 'status': 'APPROVED', 'customerTier': 'GOLD'}
Underwriting payload: {'orderId': 123, 'decision': 'APPROVED', 'riskBand': 'LOW'}
Notification payload: {'orderId': 123, 'channel': 'EMAIL', 'deliveryState': 'QUEUED'}
```
