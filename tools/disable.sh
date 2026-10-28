#!/usr/bin/env bash
set -u

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

for pkg in "$@"; do
  protected=0
  for p in "${PROTECTED[@]}"; do
    case "$pkg" in
      "$p"*) protected=1 ;;
    esac
  done
  if [ "$protected" -eq 1 ]; then
    echo "REFUSED (protected): $pkg"
    continue
  fi
  adb -s "$HOST:$PORT" shell pm disable-user --user 0 "$pkg"
  if adb -s "$HOST:$PORT" shell pm list packages -d | grep -qx "package:$pkg"; then
    echo "OK disabled: $pkg"
  else
    echo "NOT LISTED DISABLED (verify manually): $pkg"
  fi
done
