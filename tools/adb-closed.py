#!/usr/bin/env python3
import socket
import sys


def probe(ip, port):
    s = socket.socket()
    s.settimeout(2.0)
    try:
        s.connect((ip, port))
        return "open"
    except ConnectionRefusedError:
        return "refused (verified closed)"
    except OSError as e:
        return f"inconclusive: {e}"
    finally:
        s.close()


def main():
    if len(sys.argv) != 2:
        print("usage: adb-closed.py <tv-ip>", file=sys.stderr)
        sys.exit(2)
    ip = sys.argv[1]
    print(f"checking {ip} ...")
    ok = True
    for port in (5555, 6466):
        result = probe(ip, port)
        print(f"  {ip}:{port} -> {result}")
        if not result.startswith("refused"):
            ok = False
    if ok:
        print("ADB is fully closed on the TV (only 'Connection refused' counts).")
        sys.exit(0)
    print("ADB is NOT verified closed; a port did not refuse.")
    sys.exit(1)


if __name__ == "__main__":
    main()
