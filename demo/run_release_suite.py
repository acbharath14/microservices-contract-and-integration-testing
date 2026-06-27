from __future__ import annotations

import json
import subprocess
from pathlib import Path

SCENARIOS = ["happy-path", "timeout-fallback", "schema-drift"]


def run_scenario(scenario: str, reports_dir: Path) -> dict[str, object]:
    report_path = reports_dir / f"scenario-{scenario}.json"
    command = [
        "python",
        "demo/contract_demo.py",
        "--scenario",
        scenario,
        "--report-file",
        str(report_path),
    ]
    process = subprocess.run(command, capture_output=True, text=True)

    report = json.loads(report_path.read_text(encoding="utf-8"))
    report["command"] = " ".join(command)
    report["exitCode"] = process.returncode
    report["stdout"] = process.stdout.strip()
    report["stderr"] = process.stderr.strip()
    return report


def summarize(reports: list[dict[str, object]]) -> dict[str, object]:
    decisions = [str(report["releaseDecision"]) for report in reports]
    if "BLOCK" in decisions:
        final = "BLOCK"
    elif "WARN" in decisions:
        final = "WARN"
    else:
        final = "PASS"

    summary = {
        "suite": "microservices-contract-suite",
        "finalDecision": final,
        "scenarios": reports,
        "counts": {
            "pass": sum(1 for d in decisions if d == "PASS"),
            "warn": sum(1 for d in decisions if d == "WARN"),
            "block": sum(1 for d in decisions if d == "BLOCK"),
        },
    }
    return summary


def main() -> int:
    reports_dir = Path("reports")
    reports_dir.mkdir(exist_ok=True)

    reports = [run_scenario(scenario, reports_dir) for scenario in SCENARIOS]
    summary = summarize(reports)

    summary_path = reports_dir / "integration-suite-report.json"
    summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")

    print(f"Suite summary written to {summary_path}")
    print(f"Final decision: {summary['finalDecision']}")
    print(f"Counts: {summary['counts']}")

    return 1 if summary["finalDecision"] == "BLOCK" else 0


if __name__ == "__main__":
    raise SystemExit(main())
