#!/usr/bin/env python3
"""GROK_V1 P4 OEF worker client (stdlib). Token read from ../secrets/oef_token (never printed)."""
import json, os, urllib.request
from pathlib import Path
P4 = Path(__file__).resolve().parents[1]
from p4env_GROK_V1 import OEF_URL as BASE, SECRETS
TOKEN = (SECRETS / "oef_token").read_text().strip()

def call(path, body=None, token=TOKEN, headers=None, raw=None, ctype="application/json"):
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    req = urllib.request.Request(BASE + path, data=data, method="POST" if data is not None else "GET")
    if token is not None:
        req.add_header("X-LumaPlus-Token", token)
    if data is not None and ctype:
        req.add_header("Content-Type", ctype)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    import time
    for attempt in range(16):  # the worker recycles itself every OEF_MAX_REQUESTS calls (~0.8 s restart gap)
        try:
            r = urllib.request.urlopen(req, timeout=60)
            code, txt, hdrs = r.status, r.read().decode(), dict(r.headers)
            break
        except urllib.error.HTTPError as e:
            code, txt, hdrs = e.code, e.read().decode(), dict(e.headers)
            break
        except urllib.error.URLError as e:
            if attempt == 15 or not isinstance(e.reason, ConnectionRefusedError):
                raise
            time.sleep(0.25)
    try:
        j = json.loads(txt)
    except Exception:
        j = None
    return code, j, txt, hdrs
