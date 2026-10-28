# Enabling network ADB on the TV

1. On the TV: Settings → Device Preferences → About.
2. Click "Build" 7 times until "You are now a developer" appears.
3. Back to Settings → Device Preferences → Developer options.
4. Enable "Network ADB" (or "USB debugging (network)" / "Wireless debugging"). Note the IP:port it shows.
5. The TV keeps its screen awake during ADB sessions; leave it on the Home screen.

## First connect (RSA prompt)

The TV will show an "Allow USB debugging?" prompt with this computer's RSA key fingerprint.
Tick "Always allow from this computer" and press OK on the remote. If missed, run
`adb disconnect` from the PC and reconnect to trigger it again.

## Connecting from the PC

```
export TV_HOST=<tv-ip>          # TV_PORT defaults to 5555
tools/tv.py connect             # adb connect <tv-ip>:5555
adb devices                     # must show <tv-ip>:5555  device (not offline/unauthorized)
tools/tv.py sh getprop ro.product.model
```

If the network path is unreliable (Tailscale/VPN, flaky Wi-Fi), use the local TCP relay:
run `python3 tools/relay.py <tv-ip> [port]` on a machine that can reach the TV, then
`export TV_HOST=127.0.0.1 TV_PORT=15555`. See tools/relay.md.

## Closing ADB for good (end of project)

Shell-level (while ADB is still open):

```
tools/tv.py sh 'settings put global adb_wifi_enabled 0'
tools/tv.py sh 'settings put global adb_enabled 0'
```

`adb_enabled 0` kills the classic 5555 listener (verified: port refuses). The
wireless-debugging port 6466 is NOT closed by settings — it requires the
TV-side toggle.

TV-side (Developer options): Wireless debugging / USB debugging (network) OFF,
revoke USB debugging authorizations, reboot the TV. Then verify from the PC:

```
python3 tools/adb-closed.py <tv-ip>
```

Only "Connection refused" on both 5555 and 6466 counts as fully closed.

To use ADB again later: follow "Enabling network ADB" above (7× Build click,
enable network ADB, accept the RSA prompt on the TV).
