from __future__ import annotations

import argparse
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, HTTPServer
from json import dumps, loads
from threading import Thread
from urllib.request import urlopen

from contract_validator import ContractViolation, validate_payload_against_contract


@dataclass(frozen=True)
class ServiceDefinition:
    name: str
    path: str
    payload: dict[str, object]
    contract_path: str


HAPPY_PATH_SERVICES = [
    ServiceDefinition(
        name="order-decision-service",
        path="/orders/123",
        payload={"orderId": 123, "status": "APPROVED", "customerTier": "GOLD"},
        contract_path="contracts/order_contract.json",
    ),
    ServiceDefinition(
        name="underwriting-service",
        path="/underwriting/123",
        payload={"orderId": 123, "decision": "APPROVED", "riskBand": "LOW"},
        contract_path="contracts/underwriting_contract.json",
    ),
    ServiceDefinition(
        name="notification-service",
        path="/notifications/123",
        payload={"orderId": 123, "channel": "EMAIL", "deliveryState": "QUEUED"},
        contract_path="contracts/notification_contract.json",
    ),
]

SCHEMA_DRIFT_SERVICES = [
    ServiceDefinition(
        name="order-decision-service",
        path="/orders/123",
        payload={"orderId": "123", "status": "APPROVED", "tier": "GOLD"},
        contract_path="contracts/order_contract.json",
    ),
    HAPPY_PATH_SERVICES[1],
    HAPPY_PATH_SERVICES[2],
]


def build_handler(path: str, payload: dict[str, object]):
    class DemoHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path == path:
                body = dumps(payload).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)
                return

            self.send_response(404)
            self.end_headers()

        def log_message(self, *_args):
            pass

    return DemoHandler


def start_server(path: str, payload: dict[str, object], port: int = 0) -> HTTPServer:
    server = HTTPServer(("127.0.0.1", port), build_handler(path, payload))
    thread = Thread(target=server.serve_forever, daemon=True)
    thread.start()
    return server


def fetch_json(base_url: str, path: str) -> dict[str, object]:
    response = urlopen(f"{base_url}{path}").read().decode("utf-8")
    return loads(response)


def build_service_urls(services: list[ServiceDefinition]) -> tuple[list[HTTPServer], dict[str, str]]:
    servers: list[HTTPServer] = []
    urls: dict[str, str] = {}
    for service in services:
        server = start_server(service.path, service.payload)
        servers.append(server)
        urls[service.name] = f"http://127.0.0.1:{server.server_address[1]}"
    return servers, urls


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run a contract-testing orchestration demo")
    parser.add_argument(
        "--scenario",
        choices=["happy-path", "schema-drift"],
        default="happy-path",
        help="Choose whether to run a successful workflow or an intentional contract failure",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    services = HAPPY_PATH_SERVICES if args.scenario == "happy-path" else SCHEMA_DRIFT_SERVICES
    servers, urls = build_service_urls(services)

    try:
        order_service, underwriting_service, notification_service = services

        order_json = fetch_json(urls[order_service.name], order_service.path)
        validate_payload_against_contract(order_json, order_service.contract_path)

        underwriting_json = fetch_json(urls[underwriting_service.name], underwriting_service.path)
        validate_payload_against_contract(underwriting_json, underwriting_service.contract_path)

        notification_json = fetch_json(urls[notification_service.name], notification_service.path)
        validate_payload_against_contract(notification_json, notification_service.contract_path)

        print("Orchestration scenario passed")
        print("Scenario:", args.scenario)
        print("Order payload:", order_json)
        print("Underwriting payload:", underwriting_json)
        print("Notification payload:", notification_json)
    except ContractViolation as error:
        print("Orchestration scenario failed")
        print("Scenario:", args.scenario)
        print("Reason:", error)
        raise SystemExit(1) from error
    finally:
        for server in servers:
            server.shutdown()
            server.server_close()


if __name__ == "__main__":
    main()
