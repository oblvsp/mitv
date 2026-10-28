#!/usr/bin/env python3
import os
import pathlib
import subprocess
import sys
import time

HOST = os.environ.get("TV_HOST", "").strip()
PORT = os.environ.get("TV_PORT", "5555").strip()

KEYMAP = {
    "UP": "KEYCODE_DPAD_UP",
    "DOWN": "KEYCODE_DPAD_DOWN",
    "LEFT": "KEYCODE_DPAD_LEFT",
    "RIGHT": "KEYCODE_DPAD_RIGHT",
    "OK": "KEYCODE_DPAD_CENTER",
    "CENTER": "KEYCODE_DPAD_CENTER",
    "SELECT": "KEYCODE_DPAD_CENTER",
    "ENTER": "KEYCODE_ENTER",
    "BACK": "KEYCODE_BACK",
    "HOME": "KEYCODE_HOME",
    "MENU": "KEYCODE_MENU",
    "VOLUP": "KEYCODE_VOLUME_UP",
    "VOLDOWN": "KEYCODE_VOLUME_DOWN",
    "MUTE": "KEYCODE_VOLUME_MUTE",
    "POWER": "KEYCODE_POWER",
    "WAKEUP": "KEYCODE_WAKEUP",
    "SLEEP": "KEYCODE_SLEEP",
    "MEDIA_PLAY": "KEYCODE_MEDIA_PLAY",
    "MEDIA_PAUSE": "KEYCODE_MEDIA_PAUSE",
}

PNG_MAGIC = b"\x89PNG\r\n\x1a\n"


def die(msg, code=2):
    print(msg, file=sys.stderr)
    sys.exit(code)


def adb(*args):
    return subprocess.run(
        ["adb", "-s", f"{HOST}:{PORT}", *args],
        capture_output=True,
    )


def connect():
    r = subprocess.run(
        ["adb", "connect", f"{HOST}:{PORT}"],
        capture_output=True,
        text=True,
    )
    out = (r.stdout + r.stderr).strip()
    if r.returncode != 0 or "connected to" not in out.lower():
        die(f"adb connect failed: {out}")
    print(out)


def keyname(k):
    return KEYMAP.get(k.upper(), k)


def inject(keys):
    for k in keys:
        r = adb("shell", "input", "keyevent", keyname(k))
        if r.returncode != 0:
            die(f"keyevent {k} failed: {r.stderr.decode(errors='replace').strip()}")
        time.sleep(0.35)


def screenshot(out_path, keys):
    if keys:
        inject(keys)
        time.sleep(0.6)
    r = adb("exec-out", "screencap", "-p")
    data = r.stdout
    valid = data.startswith(PNG_MAGIC) and len(data) > 64
    if valid:
        path = pathlib.Path(out_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        print(f"saved {out_path} ({len(data)} bytes)")
    else:
        print(f"WARNING: {out_path} is {len(data)} bytes, not a valid PNG")
        print("Empty screenshots are normal for Live TV / HDMI / Netflix (DRM surface).")
        print("Check the foreground app instead:")
        print(f"  TV_HOST={HOST} tools/tv.py sh 'dumpsys activity activities | grep topResumedActivity'")
        sys.exit(1)


def main():
    if len(sys.argv) < 2:
        die("usage: tv.py connect | sh <cmd...> | keys <KEY...> | shot <out.png> [KEY...]")
    if not HOST:
        die("set TV_HOST first, e.g. export TV_HOST=192.168.1.x (TV_PORT defaults to 5555)")
    mode, rest = sys.argv[1], sys.argv[2:]
    if mode == "connect":
        connect()
    elif mode == "sh":
        if not rest:
            die("usage: tv.py sh <command...>")
        connect()
        r = adb("shell", " ".join(rest))
        sys.stdout.write(r.stdout.decode(errors="replace"))
        err = r.stderr.decode(errors="replace").strip()
        if err:
            print(err, file=sys.stderr)
        sys.exit(r.returncode)
    elif mode == "keys":
        if not rest:
            die("usage: tv.py keys <KEY...>")
        connect()
        inject(rest)
        print(f"sent {len(rest)} keys")
    elif mode == "shot":
        if not rest:
            die("usage: tv.py shot <out.png> [KEY...]")
        connect()
        screenshot(rest[0], rest[1:])
    else:
        die(f"unknown mode: {mode}")


if __name__ == "__main__":
    main()
