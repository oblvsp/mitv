# Troubleshooting

## connect: refused / timeout
- Network ADB toggled off, TV asleep (not just screen off — deep sleep kills adbd), or IP changed.
- Rescan for port 5555; check the current IP in TV settings → Network.
- Consider a DHCP reservation on the router so the IP stays stable.

## device unauthorized
- The RSA prompt is waiting on the TV screen. Press OK (tick "Always allow").
- Or: `adb disconnect`, revoke USB debugging authorizations on the TV, reconnect fresh.

## Screenshot looks stale (old content)
- The panel did not redraw after the keys. Re-run with an extra key or longer pause:
  `TV_HOST=$TV_HOST tools/tv.py shot shots/x.png OK` then re-shoot without keys.
- Check what is actually focused: `tools/tv.py sh 'dumpsys activity activities | grep mCurrentFocus'`.
- Force-stop the foreground app (`pm`/`am force-stop <pkg>`) and recapture.

## Screenshot empty / 0 bytes / not a valid PNG
- Normal for Live TV, HDMI inputs, and Netflix (DRM-protected surfaces return nothing).
- Exit code 1 is expected. Identify the app instead:
  `tools/tv.py sh 'dumpsys activity activities | grep topResumedActivity'`.

## adb install hangs (Android 14 / MIUI TV)
- `adb push app.apk /data/local/tmp/` then `tools/tv.py sh 'pm install -r /data/local/tmp/app.apk'`.

## Disabled packages came back after reboot
- Some firmware re-enables packages at boot. Compare before/after:
  `tools/tv.py sh 'pm list packages -d' | sort > before.txt` … reboot … after.txt; `diff`.
- Re-disable the survivors and note them in the backup list header.

## pm commands silently stop midway
- Never chain multiple `pm` calls in one `adb shell`. One command per shell call —
  this is what tools/disable.sh does.

## com.google.android.tungsten.setupwraith
- Holds Wi-Fi setup state. Before disabling, check `tools/tv.py sh 'dumpsys wifi | grep -o "cname=[^ ]*"'`.
- If a cname= entry exists, leave setupwraith enabled or Wi-Fi may need re-setup after factory events.
