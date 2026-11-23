#!/usr/bin/env python3
"""GROK_V1 P1b fake OEF worker on 127.0.0.1:<port>: one behaviour per mode (test harness only)."""
import socket, sys, time, threading
mode, port = sys.argv[1], int(sys.argv[2])
log = open(sys.argv[3], "ab") if len(sys.argv) > 3 else None
s = socket.socket(); s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1); s.bind(("127.0.0.1", port))
s.listen(0 if mode == "backlog" else 16)
if mode == "backlog":   # never accept: queue fills, later SYNs are dropped -> connect timeout
    time.sleep(60); sys.exit()
def handle(c):
    data = b""
    c.settimeout(5)
    try:
        while b"\r\n\r\n" not in data:
            data += c.recv(65536)
        h, _, b = data.partition(b"\r\n\r\n")
        n = int(([l.split(b":")[1] for l in h.split(b"\r\n") if l.lower().startswith(b"content-length")] or [b"0"])[0])
        while len(b) < n:
            b += c.recv(65536)
    except Exception:
        pass
    if log:
        log.write(data.split(b"\r\n\r\n")[0] + b"\n---\n"); log.flush()
    J = b'{"ok":true,"statement_html":"x","fields":[]}'
    if mode == "silent":
        time.sleep(30)
    elif mode == "ok":
        c.sendall(b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: %d\r\n\r\n%s" % (len(J), J))
    elif mode == "big":
        c.sendall(b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 9437184\r\n\r\n")
        try:
            for _ in range(144): c.sendall(b"[" * 65536)
        except Exception: pass
    elif mode == "redirect":
        c.sendall(b"HTTP/1.1 302 Found\r\nLocation: http://10.0.0.5/steal\r\nContent-Length: 0\r\n\r\n")
    elif mode == "html":
        c.sendall(b"HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: 13\r\n\r\n<html></html>")
    elif mode == "badjson":
        c.sendall(b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 9\r\n\r\n{not json")
    elif mode == "garbage":
        c.sendall(b"SSH-2.0-OpenSSH\r\n\r\n")
    elif mode == "array":
        c.sendall(b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 5\r\n\r\n[1,2]")
    try: c.close()
    except Exception: pass
while True:
    c, _ = s.accept()
    threading.Thread(target=handle, args=(c,), daemon=True).start()
