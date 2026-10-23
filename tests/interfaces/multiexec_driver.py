#!/usr/bin/env python3
"""Drive an interface in multiexec mode like wims (calc.c:229-258).
Linux + Windows (thread reader; no select() on pipes)."""
import os, sys, subprocess, time, threading, queue, re
binary, marker = sys.argv[1], sys.argv[2]
reqs = sys.argv[3:]
# Inherit the full parent environment (required on Windows for .bat / SBCL /
# Cygwin CAS launchers: SystemRoot, COMSPEC, TEMP, ...). Override WIMS keys.
env = os.environ.copy()
env["tmp_dir"] = env.get("tmp_dir") or os.getcwd()
env["wims_exec_parm"] = marker
env["multiexec_random"] = marker
wrap = os.environ.get("MXDRV_WRAP", "").split()
p = subprocess.Popen(wrap + [binary], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                     stderr=subprocess.PIPE, env=env, bufsize=0)
q = queue.Queue()
def _reader():
    try:
        while True:
            chunk = os.read(p.stdout.fileno(), 65536)
            if not chunk:
                q.put(None); break
            q.put(chunk)
    except Exception:
        q.put(None)
threading.Thread(target=_reader, daemon=True).start()
# Maxima via bat/sbcl on Windows needs a short warm-up before the first write.
if os.name == "nt":
    time.sleep(float(os.environ.get("MXDRV_WARMUP", "2.5")))
mb = marker.encode()
for r in reqs:
    p.stdin.write((r + marker).encode()); p.stdin.flush()
    buf = b""; t0 = time.time()
    while mb not in buf and time.time() - t0 < 90:
        try:
            chunk = q.get(timeout=0.5)
        except queue.Empty:
            continue
        if chunk is None: break
        buf += chunk
    ans = buf.split(mb)[0].decode(errors="replace").strip()
    ans = re.sub(r'\x1b\[[0-9;]*m', '', ans)
    print(f"REQ {r!r} -> {ans}")
try:
    p.kill()
except Exception:
    pass
p.wait()
