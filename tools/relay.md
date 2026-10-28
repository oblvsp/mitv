# TCP relay for ADB

`tools/relay.py` forwards a local port to the TV's ADB port over plain TCP. Use it when
the TV is not directly reachable in a way adb handles well (Tailscale, VPN, flaky route):

```
python3 tools/relay.py 192.168.1.x       # listens on 127.0.0.1:15555 -> 192.168.1.x:5555
python3 tools/relay.py 100.x.y.z 5555    # explicit target port
```

Then point the tooling at the relay:

```
export TV_HOST=127.0.0.1 TV_PORT=15555
tools/tv.py connect
```

Notes:
- The relay stays in the foreground; stop with Ctrl-C.
- `adb connect 127.0.0.1:15555` will appear in `adb devices` as the endpoint; TV tools
  keep working unchanged because they only use TV_HOST/TV_PORT.
- One relay per TV; the local port 15555 is fixed on purpose so scripts stay simple.
