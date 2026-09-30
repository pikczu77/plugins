"""Smoke test of the release jar on a real dedicated server, driven over RCON.

The server (./gradlew runProductionServer) must run with enable-rcon=true, rcon.port=25575 and
rcon.password=hpsize. Checks that the mod's commands work and that mobs really get as big as their health
(20 HP = normal size).
"""
import re
import socket
import struct
import sys
import time

HOST, PORT, PASSWORD = "127.0.0.1", 25575, "hpsize"


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

    command("hpsize", r"\[HP Size\]")
    command("hpsize preset film", r"Preset film")
    command("forceload add 0 0")
    command("execute positioned 0 -60 0 run spawnsized minecraft:chicken 4", r"Summoned")
    command("execute positioned 4 -60 0 run spawnsized minecraft:cow 10", r"Summoned")
    time.sleep(4)
    # natural health, 20 HP = normal size: chicken 4 HP -> x0.2, cow 10 HP -> x0.5
    command("hpsize info @e[type=minecraft:chicken,limit=1]", r"size ×0\.20\b")
    command("hpsize info @e[type=minecraft:cow,limit=1]", r"size ×0\.50\b")
    command("mobhp @e[type=minecraft:cow] max 100", r"Max HP = 100")
    time.sleep(2)
    command("hpsize info @e[type=minecraft:cow,limit=1]", r"size ×5\b")
    command("hpsize preset fair", r"Preset fair")
    command("mobhp @e[type=minecraft:cow] 50", r"HP = 50")
    time.sleep(4)
    # fair preset: half of the health -> half of the size
    command("hpsize info @e[type=minecraft:cow,limit=1]", r"size ×2\.50\b")
    command("hpsize off", r"OFF")
    time.sleep(2)
    command("hpsize info @e[type=minecraft:cow,limit=1]", r"size ×1\b")
    command("hpsize preset smooth", r"Preset smooth")
    command("hpsize on", r"ON")
    command("countdown 3 Test", r"Countdown")
    command("go 1", r"Starting in")
    command("recmode on", r"Recording mode ON")
    command("recmode off", r"Recording mode OFF")
    command("cleanup all 64", r"Removed")
    command("announce &6Test|Podtytuł", r"Shown to")
    command("glow @e[type=minecraft:cow] 5", r"Glowing for")
    command("mute @e[type=minecraft:cow]", r"Muted")

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
