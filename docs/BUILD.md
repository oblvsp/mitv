# Build requirements

The project uses a small shell build instead of Gradle. Install a JDK (21 is the
version used for development) and these Android SDK packages:

- Android SDK Platform 34
- Android SDK Build-Tools 34.0.0
- Android SDK Platform-Tools, if you want to use `adb`

You can install the packages from Android Studio's SDK Manager. The official
Android command-line tools can also install SDK packages; see the
[SDK Manager documentation](https://developer.android.com/tools/sdkmanager).

Set the paths for your machine. Keep the SDK and JDK out of this repository:

```bash
export ANDROID_SDK_ROOT="/path/to/Android/Sdk"
export JAVA_HOME="/path/to/jdk-21"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
```

Build from the repository root:

```bash
tools/apkbuild.sh launcher build/launcher.apk
tools/apkbuild.sh mediatest build/mediatest.apk
```

After installation, open **Папка приложений → Погода** and choose a location.
The choice is saved locally on the TV. `launcher/assets/weather.properties` is
still supported as an optional build-time fallback; it is ignored by Git.

The first build creates a local debug keystore at `tools/debug.keystore` unless
`MITV_KEYSTORE` points to another file. Keep that key in a private backup if you
need to install updates over an existing build; a newly generated key cannot
sign an update for an APK signed by a different key. Override the alias and
passwords with `MITV_KEY_ALIAS`, `MITV_KEYSTORE_PASS`, and `MITV_KEY_PASS` when
using a privately stored keystore. The default credentials are for development
builds only, not for publishing a production app.
