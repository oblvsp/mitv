#!/usr/bin/env bash
set -uo pipefail

HOST="${TV_HOST:-}"
PORT="${TV_PORT:-5555}"
DIR="$(cd "$(dirname "$0")" && pwd)"
LIST="$DIR/packages-disabled-final.txt"

if [ -z "$HOST" ]; then
  echo "set TV_HOST first, e.g. export TV_HOST=192.168.1.x" >&2
  exit 2
fi
if [ ! -f "$LIST" ]; then
  echo "missing $LIST" >&2
  exit 2
fi

echo "Re-enabling every package from $(basename "$LIST") on $HOST:$PORT"
status=0
while IFS= read -r pkg; do
  [[ -n "$pkg" ]] || continue
  if [[ ! "$pkg" =~ ^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)+$ ]]; then
    echo "REFUSED (invalid package name): $pkg" >&2
    status=1
    continue
  fi
  if adb -s "$HOST:$PORT" shell pm enable "$pkg"; then
    echo "enabled: $pkg"
  else
    echo "FAILED to enable: $pkg" >&2
    status=1
  fi
done < <(sed -n 's/^package://p' "$LIST")
echo "Verify: adb -s \"$HOST:$PORT\" shell pm list packages -d"
echo "Unsuspending com.google.android.tvrecommendations (UPDATED_SYSTEM_APP, disabled-user does not survive boot)..."
if ! adb -s "$HOST:$PORT" shell cmd package unsuspend com.google.android.tvrecommendations; then
  echo "FAILED to unsuspend com.google.android.tvrecommendations" >&2
  status=1
fi
echo "To restore stock HOME: adb -s \"$HOST:$PORT\" shell pm enable com.google.android.tvlauncher"
if [ "$status" -eq 0 ]; then
  echo "Restore commands completed. Review the output and verify the disabled package list."
else
  echo "Restore completed with errors; review the failed packages above." >&2
fi
exit "$status"
