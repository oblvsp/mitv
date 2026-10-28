#!/usr/bin/env bash
set -u

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
grep '^package:' "$LIST" | sed 's/^package://' | while IFS= read -r pkg; do
  [ -n "$pkg" ] || continue
  adb -s "$HOST:$PORT" shell pm enable "$pkg"
  echo "enabled: $pkg"
done
echo "Done. Verify: adb -s \"$HOST:$PORT\" shell pm list packages -d"
echo "Unsuspending com.google.android.tvrecommendations (UPDATED_SYSTEM_APP, disabled-user does not survive boot)..."
adb -s "$HOST:$PORT" shell cmd package unsuspend com.google.android.tvrecommendations
echo "To restore stock HOME: adb -s \"$HOST:$PORT\" shell pm enable com.google.android.tvlauncher"
