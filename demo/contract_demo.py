from http.server import BaseHTTPRequestHandler, HTTPServer
from json import dumps, loads
from threading import Thread
from urllib.request import urlopen

from contract_validator import validate_payload_against_contract

ORDER_PAYLOAD = {"orderId": 123, "status": "APPROVED", "customerTier": "GOLD"}
DEPENDENCY_PAYLOAD = {"status": "UP"}


class DemoHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/orders/123":
            body = dumps(ORDER_PAYLOAD).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        if self.path == "/health":
            body = dumps(DEPENDENCY_PAYLOAD).encode("utf-8")
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


def start_server(port: int) -> HTTPServer:
    server = HTTPServer(("127.0.0.1", port), DemoHandler)
    thread = Thread(target=server.serve_forever, daemon=True)
    thread.start()
    return server


def main() -> None:
    provider = start_server(0)
    dependency = start_server(0)

    provider_port = provider.server_address[1]
    dependency_port = dependency.server_address[1]

    provider_response = urlopen(f"http://127.0.0.1:{provider_port}/orders/123").read().decode("utf-8")
    dependency_response = urlopen(f"http://127.0.0.1:{dependency_port}/health").read().decode("utf-8")

    provider_json = loads(provider_response)
    dependency_json = loads(dependency_response)

    validate_payload_against_contract(provider_json, "contracts/order_contract.json")

    assert provider_json["orderId"] == 123
    assert provider_json["status"] == "APPROVED"
    assert provider_json["customerTier"] == "GOLD"
    assert dependency_json["status"] == "UP"

    print("Contract verification passed")
    print("Provider payload:", provider_json)
    print("Virtualized dependency payload:", dependency_json)


if __name__ == "__main__":
    main()
