from __future__ import annotations

from json import loads
from pathlib import Path
from typing import Any


class ContractViolation(Exception):
    pass


def _validate_type(field: str, expected_type: str, value: Any) -> None:
    type_map = {
        "number": (int, float),
        "string": (str,),
        "boolean": (bool,),
    }
    if expected_type not in type_map:
        raise ContractViolation(f"Unsupported expected type '{expected_type}' for field '{field}'")
    if not isinstance(value, type_map[expected_type]):
        raise ContractViolation(
            f"Field '{field}' expected type '{expected_type}' but got '{type(value).__name__}'"
        )


def validate_payload_against_contract(payload: dict[str, Any], contract_path: str | Path) -> None:
    contract = loads(Path(contract_path).read_text(encoding="utf-8"))

    for field, expected_type in contract["requiredFields"].items():
        if field not in payload:
            raise ContractViolation(f"Missing required field '{field}' in provider payload")
        _validate_type(field, expected_type, payload[field])

    if payload["status"] not in contract["allowedStatus"]:
        raise ContractViolation("Field 'status' has unsupported value")

    if payload["customerTier"] not in contract["allowedCustomerTier"]:
        raise ContractViolation("Field 'customerTier' has unsupported value")
