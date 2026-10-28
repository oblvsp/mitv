#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

usage() {
  echo "usage: $0 <project-dir> <output.apk>" >&2
  echo "env: ANDROID_SDK_ROOT, JAVA_HOME, MITV_KEYSTORE" >&2
  exit 2
}

[ "$#" -eq 2 ] || usage
PROJECT_ARG="$1"
OUTPUT_ARG="$2"
[ -d "$PROJECT_ARG" ] || { echo "project directory not found: $PROJECT_ARG" >&2; exit 2; }
PROJ="$(cd "$PROJECT_ARG" && pwd)"
OUT="$OUTPUT_ARG"
case "$OUT" in /*) ;; *) OUT="$(pwd)/$OUT" ;; esac

SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$ROOT/tools/sdk}}"
BT="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34/android.jar"

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
  JAVAC="$JAVA_HOME/bin/javac"
elif [ -x "$ROOT/tools/jdk/bin/javac" ]; then
  JAVA_HOME="$ROOT/tools/jdk"
  JAVAC="$JAVA_HOME/bin/javac"
else
  LOCAL_JAVAC="$(find "$ROOT/tools/jdk" -type f -path '*/bin/javac' -print -quit 2>/dev/null || true)"
  if [ -n "$LOCAL_JAVAC" ]; then
    JAVAC="$LOCAL_JAVAC"
    JAVA_HOME="$(dirname "$(dirname "$JAVAC")")"
  else
    JAVAC="$(command -v javac || true)"
    if [ -n "$JAVAC" ]; then
      JAVA_HOME="$(cd "$(dirname "$JAVAC")/.." && pwd)"
    fi
  fi
fi

if [ -z "${JAVAC:-}" ] || [ ! -x "$JAVAC" ]; then
  echo "JDK 21 javac not found; install JDK 21 or set JAVA_HOME" >&2
  exit 2
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

for tool in aapt2 d8 zipalign apksigner; do
  [ -x "$BT/$tool" ] || { echo "missing $BT/$tool; install Android build-tools 34.0.0" >&2; exit 2; }
done
[ -f "$PLATFORM" ] || { echo "missing $PLATFORM; install Android platform 34" >&2; exit 2; }
[ -x "$JAVA_HOME/bin/keytool" ] || { echo "missing $JAVA_HOME/bin/keytool; install a full JDK" >&2; exit 2; }
[ -f "$PROJ/AndroidManifest.xml" ] || { echo "missing $PROJ/AndroidManifest.xml" >&2; exit 2; }
[ -d "$PROJ/res" ] && [ -d "$PROJ/src" ] || { echo "project must contain src/ and res/" >&2; exit 2; }

PKGDIR="$ROOT/build/$(basename "$PROJ")"
mkdir -p "$(dirname "$OUT")"
rm -rf "$PKGDIR"
mkdir -p "$PKGDIR/gen" "$PKGDIR/classes" "$PKGDIR/dex" "$PKGDIR/empty-assets"

"$BT/aapt2" compile --dir "$PROJ/res" -o "$PKGDIR/res.zip"
ASSETS="$PROJ/assets"
[ -d "$ASSETS" ] || ASSETS="$PKGDIR/empty-assets"
"$BT/aapt2" link \
  -o "$PKGDIR/base.apk" \
  -I "$PLATFORM" \
  --manifest "$PROJ/AndroidManifest.xml" \
  -R "$PKGDIR/res.zip" \
  -A "$ASSETS" \
  --java "$PKGDIR/gen" \
  --auto-add-overlay

find "$PROJ/src" "$PKGDIR/gen" -name '*.java' -print > "$PKGDIR/sources.txt"
"$JAVAC" -source 8 -target 8 -Xlint:-options -classpath "$PLATFORM" -d "$PKGDIR/classes" @"$PKGDIR/sources.txt"

find "$PKGDIR/classes" -name '*.class' -print > "$PKGDIR/classlist.txt"
"$BT/d8" --release --min-api 26 --lib "$PLATFORM" --output "$PKGDIR/dex" @"$PKGDIR/classlist.txt"

(cd "$PKGDIR/dex" && zip -q -j "$PKGDIR/base.apk" classes.dex)
"$BT/zipalign" -f 4 "$PKGDIR/base.apk" "$PKGDIR/aligned.apk"

KS="${MITV_KEYSTORE:-$ROOT/tools/debug.keystore}"
KS_ALIAS="${MITV_KEY_ALIAS:-androiddebugkey}"
KS_STOREPASS="${MITV_KEYSTORE_PASS:-android}"
KS_KEYPASS="${MITV_KEY_PASS:-$KS_STOREPASS}"
if [ ! -f "$KS" ]; then
  mkdir -p "$(dirname "$KS")"
  "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$KS" -alias "$KS_ALIAS" \
    -storepass "$KS_STOREPASS" -keypass "$KS_KEYPASS" -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  echo "Created a local debug signing key at $KS. Keep it to install future APK updates." >&2
fi

"$BT/apksigner" sign --ks "$KS" --ks-key-alias "$KS_ALIAS" \
  --ks-pass "pass:$KS_STOREPASS" --key-pass "pass:$KS_KEYPASS" \
  --out "$OUT" "$PKGDIR/aligned.apk"
echo "built $OUT"
