#!/usr/bin/env python3
"""Drive an interface in multiexec mode like wims does (calc.c:229-258):
send '<parm><marker>', read stdout until the marker comes back, next request.
Prints a transcript; exits after the last answer (kills the interface)."""
import os, sys, subprocess, select, time
binary, marker = sys.argv[1], sys.argv[2]
reqs = sys.argv[3:]
env = {"PATH": "/usr/bin:/bin", "tmp_dir": os.getcwd(),
       "wims_exec_parm": marker, "multiexec_random": marker}
# MXDRV_WRAP=wine: run a Windows build; wine's own variables are passed through
wrap = os.environ.get("MXDRV_WRAP", "").split()
for k in ("HOME", "WINEPREFIX", "WINEDEBUG", "WINEPATH", "pari_command"):
    if wrap and k in os.environ: env[k] = os.environ[k]
p = subprocess.Popen(wrap + [binary], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                     stderr=subprocess.PIPE, env=env)
for r in reqs:
    p.stdin.write((r + marker).encode()); p.stdin.flush()
    buf = b""; t0 = time.time()
    while marker.encode() not in buf and time.time() - t0 < 20:
        rd, _, _ = select.select([p.stdout], [], [], 0.5)
        if rd:
            chunk = os.read(p.stdout.fileno(), 65536)
            if not chunk: break
            buf += chunk
    ans = buf.split(marker.encode())[0].decode(errors="replace").strip()
    print(f"REQ {r!r} -> {ans!r}")
p.kill(); p.wait()
