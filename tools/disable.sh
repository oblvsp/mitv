#!/usr/bin/env bash
set -uo pipefail

HOST="${TV_HOST:-}"
PORT="${TV_PORT:-5555}"

if [ -z "$HOST" ]; then
  echo "set TV_HOST first, e.g. export TV_HOST=192.168.1.x" >&2
  exit 2
fi

if [ $# -eq 0 ]; then
  echo "usage: $0 <package...>   (refuses protected packages, one pm call per shell)"
  exit 2
fi

PROTECTED=(
  "com.android."
  "com.google.android.gms"
  "com.google.android.gsf"
  "com.google.android.webview"
  "com.google.android.inputmethod"
  "com.google.android.tv.remote"
  "com.google.android.katniss"
  "com.google.android.tungsten.setupwraith"
)

status=0
for pkg in "$@"; do
  if [[ ! "$pkg" =~ ^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)+$ ]]; then
    echo "REFUSED (invalid package name): $pkg" >&2
    status=1
    continue
  fi
  protected=0
  for p in "${PROTECTED[@]}"; do
    case "$pkg" in
      "$p"*) protected=1 ;;
    esac
  done
  if [ "$protected" -eq 1 ]; then
    echo "REFUSED (protected): $pkg"
    status=1
    continue
  fi
  if ! adb -s "$HOST:$PORT" shell pm disable-user --user 0 "$pkg"; then
    echo "FAILED to disable: $pkg" >&2
    status=1
    continue
  fi
  if adb -s "$HOST:$PORT" shell pm list packages -d | grep -qx "package:$pkg"; then
    echo "OK disabled: $pkg"
  else
    echo "NOT LISTED DISABLED (verify manually): $pkg"
    status=1
  fi
done
exit "$status"
