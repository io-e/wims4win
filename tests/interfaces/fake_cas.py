#!/usr/bin/env python3
"""Fake CAS speaking just enough of the pari protocol used by WIMS interfaces.
print("...") -> prints the string ; 'quit' -> exits ; other lines -> 'ECHO:<line>'.
Modes (argv[1]): plain | crlf (CRLF line endings, like a Windows CAS)
                 | flood (20 MB of output after each echo: tests fsizelim)"""
import sys, re
mode = sys.argv[1] if len(sys.argv) > 1 else "plain"
eol = "\r\n" if mode == "crlf" else "\n"
out = sys.stdout
echo = False
for line in sys.stdin:
    line = line.rstrip("\n")
    if line.strip() == "\\e1":
        echo = True; continue
    if echo and line.strip():
        out.write("? " + line + eol)          # gp echo mode: '? <input>'
    if line.strip() == "quit":
        break
    m = re.match(r'print\("(.*)"\)\s*$', line)
    if m:
        out.write(m.group(1) + eol)
    elif line.strip() and not line.startswith(("\\", "default", "alias", "setrand")) and "=" not in line:
        out.write("ECHO:" + line + eol)
        if mode == "flood":
            out.write("x" * 20_000_000 + eol)
    out.flush()
