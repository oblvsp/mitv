# Testing

Run the local tests with:

```bash
tests/run.sh
```

They use only Python's standard library and a fake `adb`; they do not connect to
a TV or require the Android SDK. The suite covers ADB port-probe outcomes,
screenshot validation, key mapping, and success/failure handling in the package
scripts. GitHub Actions runs this suite on pushes and pull requests.

## TV smoke checks

After building and installing `launcher.apk`, check these cases on the target TV:

1. Copy `docs/weather.properties.example` to `launcher/assets/weather.properties`,
   fill in a test location, and rebuild. Cold start with Wi-Fi connected and
   disconnected; verify the launcher still opens and that the weather row
   tolerates an unavailable network.
2. Navigate the dock and app folder to each edge with the remote. Press keys
   rapidly in both directions and verify focus animation remains stable.
3. Install, update, and uninstall a Leanback app while the launcher is active;
   confirm the folder and dock refresh without losing the selected item.
4. Open and close the folder with BACK and MENU; launch an app, return with HOME,
   and verify focus returns to the dock.
5. Reboot after selecting the custom HOME app and confirm the launcher starts.

The smoke checks require a physical TV and are not run by the local test suite.
