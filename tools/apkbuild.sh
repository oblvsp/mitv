#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="$ROOT/tools/sdk"
BT="$SDK/build-tools/34.0.0"
PLATFORM="$SDK/platforms/android-34/android.jar"
JAVAC="$(find "$ROOT/tools/jdk" -name javac -type f | head -1)"
export JAVA_HOME="$(dirname "$(dirname "$JAVAC")")"
export PATH="$JAVA_HOME/bin:$PATH"

PROJ="$1"
OUT="$2"
PKGDIR="$ROOT/build/$(basename "$PROJ")"

rm -rf "$PKGDIR"
mkdir -p "$PKGDIR/gen" "$PKGDIR/classes" "$PKGDIR/dex"

"$BT/aapt2" compile --dir "$PROJ/res" -o "$PKGDIR/res.zip"
"$BT/aapt2" link \
  -o "$PKGDIR/base.apk" \
  -I "$PLATFORM" \
  --manifest "$PROJ/AndroidManifest.xml" \
  -R "$PKGDIR/res.zip" \
  -A "$PROJ/assets" \
  --java "$PKGDIR/gen" \
  --auto-add-overlay

find "$PROJ/src" "$PKGDIR/gen" -name '*.java' > "$PKGDIR/sources.txt"
"$JAVAC" -source 8 -target 8 -Xlint:-options -classpath "$PLATFORM" -d "$PKGDIR/classes" @"$PKGDIR/sources.txt"

find "$PKGDIR/classes" -name '*.class' > "$PKGDIR/classlist.txt"
"$BT/d8" --release --min-api 26 --lib "$PLATFORM" --output "$PKGDIR/dex" @"$PKGDIR/classlist.txt"

(cd "$PKGDIR/dex" && zip -q -j "$PKGDIR/base.apk" classes.dex)

"$BT/zipalign" -f 4 "$PKGDIR/base.apk" "$PKGDIR/aligned.apk"

KS="$ROOT/tools/debug.keystore"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias androiddebugkey \
    -storepass android -keypass android -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$OUT" "$PKGDIR/aligned.apk"

echo "built $OUT"
