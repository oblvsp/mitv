#!/usr/bin/env python3
import asyncio
import sys

LOCAL_HOST = "127.0.0.1"
LOCAL_PORT = 15555


async def pipe(reader, writer):
    try:
        while True:
            data = await reader.read(65536)
            if not data:
                break
            writer.write(data)
            await writer.drain()
    except Exception:
        pass
    finally:
        try:
            writer.close()
        except Exception:
            pass


async def handle(client_reader, client_writer, target):
    try:
        tv_reader, tv_writer = await asyncio.open_connection(*target)
    except Exception as e:
        print(f"upstream {target} failed: {e}", file=sys.stderr)
        client_writer.close()
        return
    await asyncio.gather(
        pipe(client_reader, tv_writer),
        pipe(tv_reader, client_writer),
    )
    tv_writer.close()
    client_writer.close()


async def main():
    if len(sys.argv) < 2:
        print("usage: relay.py <tv-ip> [port]", file=sys.stderr)
        sys.exit(2)
    tv = sys.argv[1]
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 5555
    target = (tv, port)
    server = await asyncio.start_server(
        lambda r, w: handle(r, w, target), LOCAL_HOST, LOCAL_PORT
    )
    print(f"relay {LOCAL_HOST}:{LOCAL_PORT} -> {tv}:{port} (Ctrl-C to stop)")
    async with server:
        await server.serve_forever()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
