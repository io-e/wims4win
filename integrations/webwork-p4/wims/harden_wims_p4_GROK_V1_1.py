#!/usr/bin/env python3
"""GROK_V1_1 P4 WIMS hardening (A.2), idempotent, for a WIMS runtime tree you own. Python stdlib only.
V1 behaviour kept:
  - every connection except p4grok: ident_password=* (disabled), myself included;
  - p4grok: ident_site=127.0.0.1, ident_agent=P4-OEF-GROK, ident_allow=gettime listmodules getmodule,
    password = 32 hex from SECRETS_DIR/wims_p4grok_password (created chmod 600 if missing; never printed).
V1_1 additions:
  - P4 shared secret for the WIMS side (job=p4render and the p4_json hook, see p4_wims_GROK_V1_1.diff):
    SECRETS_DIR/wims_p4_token (64 hex, chmod 600, created if missing) is copied into
    WIMS_RUNTIME/log/p4_auth/p4_secret.proc (dir 700, file 600; log/ is never served by wims.cgi);
  - --rotate-p4grok: replace the p4grok password by a new random one (old value never printed);
  - --dev-wims-conf: DEBIAN DEV INSTANCE ONLY. Writes the GROK_V1_1 block of log/wims.conf
    (threshold1=2000, threshold2=4000, priority=127.0.0.1; session cleanup idle_time3=120). Never use on the Windows instance without Arnaud's OK.
Usage: harden_wims_p4_GROK_V1_1.py WIMS_RUNTIME SECRETS_DIR [--rotate-p4grok] [--dev-wims-conf]"""
import os, re, secrets, sys
from pathlib import Path

args = [a for a in sys.argv[1:] if not a.startswith("--")]
opts = {a for a in sys.argv[1:] if a.startswith("--")}
assert len(args) == 2 and opts <= {"--rotate-p4grok", "--dev-wims-conf"}, __doc__
rt, sd = Path(args[0]), Path(args[1])
conn = rt / "log/classes/.connections"
assert conn.is_dir(), f"no {conn}"
sd.mkdir(mode=0o700, exist_ok=True)


def write600(path: Path, data: str) -> None:
    tmp = path.with_name(path.name + ".tmp")
    fd = os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    os.write(fd, data.encode()); os.close(fd)
    os.replace(tmp, path); os.chmod(path, 0o600)


def secret_file(path: Path, nbytes: int, rotate: bool) -> str:
    if rotate or not path.exists():
        write600(path, secrets.token_hex(nbytes))
    v = path.read_text().strip()
    assert re.fullmatch(r"[0-9a-f]{%d}" % (2 * nbytes), v), f"{path.name} must be {2 * nbytes} hex"
    return v


changed = []
pw = secret_file(sd / "wims_p4grok_password", 16, "--rotate-p4grok" in opts)
if "--rotate-p4grok" in opts:
    changed.append("p4grok password rotated")
for f in sorted(conn.iterdir()):
    if f.name.startswith(".") or f.name == "p4grok" or not f.is_file():
        continue
    raw = f.read_bytes().decode("latin-1")
    new = re.sub(r"(?m)^ident_password=.*$", "ident_password=*", raw)
    if "ident_password=" not in new:
        new += "\nident_password=*\n"
    if new != raw:
        f.write_bytes(new.encode("latin-1")); changed.append(f.name)
p4 = conn / "p4grok"
body = ("# GROK_V1_1 P4 connection (loopback only). Password: wims_p4grok_password in the secrets dir (32 hex).\n"
        "# P4 renders through job=p4render (shared secret, not this ident); this connection only serves health checks.\n"
        "ident_site=127.0.0.1\nident_desc=P4 OEF worker (GROK_V1_1)\nident_agent=P4-OEF-GROK\nident_protocol=http\n"
        f"ident_password={pw}\nident_type=json\nident_allow=gettime listmodules getmodule\nencoding=utf-8\n")
if not p4.exists() or p4.read_text(errors="replace") != body:
    write600(p4, body); changed.append("p4grok")
os.chmod(p4, 0o600)

tok = secret_file(sd / "wims_p4_token", 32, False)
ad = rt / "log/p4_auth"
ad.mkdir(mode=0o700, exist_ok=True); os.chmod(ad, 0o700)
sp = ad / "p4_secret.proc"
want = f"!set p4_secret={tok}\n"
if not sp.exists() or sp.read_text() != want:
    write600(sp, want); changed.append("log/p4_auth/p4_secret.proc")

if "--dev-wims-conf" in opts:
    wc = rt / "log/wims.conf"
    raw = wc.read_text(encoding="latin-1") if wc.exists() else ""
    raw = re.sub(r"(?s)# BEGIN GROK_V1_1 P4 dev.*?# END GROK_V1_1 P4 dev\n", "", raw)
    block = ("# BEGIN GROK_V1_1 P4 dev (Debian dev WIMS only; Windows needs Arnaud's explicit OK)\n"
             "# load thresholds (defaults 150/300): refusals ('threshold', HTTP 450) start at 1-min load > threshold1/200\n"
             "# for anonymous new sessions and > threshold2/50 for all; runnable processes > threshold1/20 refuse too.\n"
             "threshold1=2000\nthreshold2=4000\n"
             "# loopback (the P4 worker and the gates) is a priority site: never refused by threshold1\n"
             "priority=127.0.0.1\n"
             "# session cleanup (wimslogd, every 300 s): idle anonymous/popup sessions (the worker's) are removed after\n"
             "# idle_time3 s (default 900); restart wimslogd to apply (it reads the values when it starts)\n"
             "idle_time3=120\n"
             "# END GROK_V1_1 P4 dev\n")
    new = raw + ("" if raw.endswith("\n") or not raw else "\n") + block
    if new != raw:
        wc.write_text(new, encoding="latin-1"); changed.append("log/wims.conf (dev block)")
print("disabled/updated:", ", ".join(changed) or "nothing (already hardened)")
