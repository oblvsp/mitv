#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 -m unittest discover -s "$ROOT/tests" -v
python3 -m py_compile "$ROOT"/tools/*.py "$ROOT"/tests/*.py
for script in "$ROOT/tools/apkbuild.sh" "$ROOT/tools/disable.sh" "$ROOT/backup/restore.sh"; do
  bash -n "$script"
done
