"""Smoke test of the release jar on a real dedicated server, driven over RCON.

The server (./gradlew runProductionServer) must run with enable-rcon=true, rcon.port=25575 and
rcon.password=upgrades. Checks that the /upgrades command works and that the mod's entities are registered.
"""
import re
import socket
import struct
import sys
import time

HOST, PORT, PASSWORD = "127.0.0.1", 25575, "upgrades"


def recv_exact(sock, size):
    data = b""
    while len(data) < size:
        chunk = sock.recv(size - len(data))
        if not chunk:
            raise ConnectionError("RCON connection closed")
        data += chunk
    return data


def send(sock, request_id, kind, body):
    payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\0\0"
    sock.sendall(struct.pack("<i", len(payload)) + payload)
    length = struct.unpack("<i", recv_exact(sock, 4))[0]
    data = recv_exact(sock, length)
    response_id = struct.unpack("<i", data[:4])[0]
    return response_id, data[8:-2].decode("utf-8", "replace")


def connect(timeout_seconds=900):
    deadline = time.time() + timeout_seconds
    while True:
        try:
            sock = socket.create_connection((HOST, PORT), timeout=30)
            response_id, _ = send(sock, 1, 3, PASSWORD)
            if response_id == -1:
                sys.exit("RCON login failed")
            return sock
        except (ConnectionError, OSError):
            if time.time() > deadline:
                sys.exit("Server did not open RCON in time")
            time.sleep(5)


def main():
    sock = connect()
    request_id = 10
    failures = []

    def command(text, expect=None):
        nonlocal request_id
        request_id += 1
        _, response = send(sock, request_id, 2, text)
        print(f"> /{text}\n{response.strip() or '(no output)'}\n", flush=True)
        if expect is not None and not re.search(expect, response):
            failures.append(f"/{text}: expected {expect!r}, got {response!r}")
        return response

    command("upgrades settings", r"\[Ulepszenia\].*Vein Miner")
    command("upgrades powers all", r"naraz")
    command("upgrades powers select", r"Jedna moc")
    command("upgrades veinminer sizes 8 16 32 64", r"8 / 16 / 32 / 64")
    command("upgrades veinminer sneak false", r"WYŁ")
    command("upgrades drops 3 4", r"×3–×4")
    command("upgrades clones max 20", r"20")
    command("upgrades disable nap", r"Wyłączono")
    command("upgrades enable nap", r"Włączono")
    command("upgrades give @a all")
    command("forceload add 0 0")
    command("summon upgrades:combat_clone 0 -60 0", r"Summoned")
    time.sleep(2)
    command("upgrades clones clear", r"Usunięto klonów: 1")
    command("upgrades portals clear", r"Usunięto portale")
    command("upgrades reset", r"domyślne")
    command("upgrades settings", r"16 / 48 / 128 / 512")

    try:
        send(sock, 99, 2, "stop")
    except ConnectionError:
        pass

    if failures:
        print("FAILURES:\n" + "\n".join(failures))
        sys.exit(1)

    print("Smoke test passed")


if __name__ == "__main__":
    main()
