#!/usr/bin/env python3
"""Localhost-only CGI wrapper so curl/Playwright can talk HTTP to a WIMS runtime
without changing Apache/Laragon. Binds 127.0.0.1 only. Refuses the live lab tree.
"""
from __future__ import annotations

import argparse
import os
import posixpath
import subprocess
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

LIVE_BASENAME = "wims-runtime-r19040-grok-hook-20261106"


def resolve_cgi(runtime: Path) -> Path:
    ph = runtime / "public_html"
    for name in ("wims.cgi.exe", "wims.cgi", "wims"):
        p = ph / name
        if p.is_file():
            return p
    raise SystemExit(f"dev-cgi-server: no wims.cgi under {ph}")


def refuse_live(runtime: Path) -> None:
    for part in runtime.resolve().parts:
        if part == LIVE_BASENAME:
            raise SystemExit(
                f"dev-cgi-server: refusing live runtime {LIVE_BASENAME}"
            )


class Handler(BaseHTTPRequestHandler):
    runtime: Path
    cgi: Path

    def log_message(self, fmt: str, *args) -> None:
        sys.stderr.write("dev-cgi-server: " + (fmt % args) + "\n")

    def _run_cgi(self) -> None:
        parsed = urlsplit(self.path)
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length else b""
        env = os.environ.copy()
        # CGI variables only; do not inherit caller query leftovers
        for k in list(env):
            if k in (
                "REQUEST_METHOD",
                "QUERY_STRING",
                "CONTENT_TYPE",
                "CONTENT_LENGTH",
                "SCRIPT_NAME",
                "SCRIPT_FILENAME",
                "PATH_INFO",
                "GATEWAY_INTERFACE",
                "SERVER_SOFTWARE",
                "SERVER_NAME",
                "SERVER_PORT",
                "SERVER_PROTOCOL",
                "REMOTE_ADDR",
                "REMOTE_PORT",
                "HTTP_HOST",
                "HTTP_COOKIE",
                "HTTP_USER_AGENT",
                "WIMS_HOME",
            ):
                env.pop(k, None)
        env.update(
            {
                "REQUEST_METHOD": self.command,
                # Pass the query raw. Unquoting the whole string turns %20 into a
                # literal space (and would break values like "3, 5"); the CGI decodes
                # percent-escapes per field, including %2B -> +.
                "QUERY_STRING": parsed.query,
                "CONTENT_TYPE": self.headers.get("Content-Type", ""),
                "CONTENT_LENGTH": str(len(body)),
                "GATEWAY_INTERFACE": "CGI/1.1",
                "SERVER_SOFTWARE": "wims-automation-dev/1.0",
                "SERVER_NAME": "127.0.0.1",
                "SERVER_PORT": str(self.server.server_address[1]),
                "SERVER_PROTOCOL": "HTTP/1.0",
                "SCRIPT_NAME": "/wims/wims.cgi",
                "SCRIPT_FILENAME": str(self.cgi),
                "PATH_INFO": "",
                "REMOTE_ADDR": self.client_address[0],
                "REMOTE_PORT": str(self.client_address[1]),
                "HTTP_HOST": self.headers.get("Host", "127.0.0.1"),
                "HTTP_USER_AGENT": self.headers.get("User-Agent", "wims-automation"),
            }
        )
        cookie = self.headers.get("Cookie")
        if cookie:
            env["HTTP_COOKIE"] = cookie
        try:
            proc = subprocess.run(
                [str(self.cgi)],
                input=body,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                cwd=str(self.runtime / "public_html"),
                env=env,
                timeout=60,
            )
        except subprocess.TimeoutExpired:
            self.send_error(504, "CGI timeout")
            return
        raw = proc.stdout
        # Split CGI headers / body on first blank line
        if b"\r\n\r\n" in raw:
            header_blob, body_out = raw.split(b"\r\n\r\n", 1)
            sep = b"\r\n"
        elif b"\n\n" in raw:
            header_blob, body_out = raw.split(b"\n\n", 1)
            sep = b"\n"
        else:
            header_blob, body_out, sep = raw, b"", b"\n"
        headers = []
        status = 200
        reason = "OK"
        for line in header_blob.split(sep):
            if not line.strip():
                continue
            try:
                text = line.decode("latin-1")
            except Exception:
                continue
            if text.lower().startswith("status:"):
                rest = text.split(":", 1)[1].strip()
                parts = rest.split(None, 1)
                try:
                    status = int(parts[0])
                except ValueError:
                    status = 200
                if len(parts) > 1:
                    reason = parts[1]
            else:
                headers.append(text)
        self.send_response(status, reason)
        sent_ct = False
        for h in headers:
            if ":" not in h:
                continue
            k, v = h.split(":", 1)
            if k.lower() == "status":
                continue
            if k.lower() == "content-type":
                sent_ct = True
            self.send_header(k, v.strip())
        if not sent_ct:
            self.send_header("Content-Type", "text/html; charset=ISO-8859-1")
        self.send_header("Content-Length", str(len(body_out)))
        self.end_headers()
        self.wfile.write(body_out)
        if proc.stderr:
            sys.stderr.write(proc.stderr.decode("latin-1", "replace"))

    def do_GET(self) -> None:  # noqa: N802
        self._dispatch()

    def do_POST(self) -> None:  # noqa: N802
        self._dispatch()

    def do_HEAD(self) -> None:  # noqa: N802
        self._dispatch()

    def _dispatch(self) -> None:
        path = urlsplit(self.path).path
        path = posixpath.normpath(path)
        if path in ("/wims/wims.cgi", "/wims/wims.cgi.exe", "/wims/wims", "/wims.cgi"):
            self._run_cgi()
            return
        # tiny static: only files under public_html, no path escape
        rel = path.lstrip("/")
        if rel.startswith("wims/"):
            rel = rel[5:]
        target = (self.runtime / "public_html" / rel).resolve()
        root = (self.runtime / "public_html").resolve()
        if root not in target.parents and target != root:
            self.send_error(403, "forbidden")
            return
        if target.is_file():
            data = target.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "application/octet-stream")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            if self.command != "HEAD":
                self.wfile.write(data)
            return
        self.send_error(404, "not found")


def main() -> int:
    ap = argparse.ArgumentParser(description="Localhost CGI front-end for one WIMS runtime")
    ap.add_argument("--runtime", required=True, help="WIMS runtime root")
    ap.add_argument("--port", type=int, default=8765)
    ap.add_argument("--pid-file", default="")
    args = ap.parse_args()
    runtime = Path(args.runtime).resolve()
    if not runtime.is_dir():
        raise SystemExit(f"dev-cgi-server: runtime not found: {runtime}")
    refuse_live(runtime)
    cgi = resolve_cgi(runtime)
    Handler.runtime = runtime
    Handler.cgi = cgi
    # 127.0.0.1 only — never 0.0.0.0
    httpd = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    if args.pid_file:
        Path(args.pid_file).write_text(str(os.getpid()), encoding="ascii")
    print(f"dev-cgi-server listening on http://127.0.0.1:{args.port}/wims/wims.cgi", flush=True)
    print(f"runtime={runtime} cgi={cgi.name}", flush=True)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        httpd.server_close()
        if args.pid_file:
            try:
                os.remove(args.pid_file)
            except OSError:
                pass
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
