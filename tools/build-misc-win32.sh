#!/bin/bash
# Cross-build the WIMS C helpers of src/Misc and src/OefMsg2wims for native
# Windows (mingw-w64), from the unmodified sources, linked with the Win32
# libwims (tools/build-win32.sh) and a binary-stdio constructor
# (src/Platform/tools/binmode_win.c). webget uses a Windows-only rewrite
# (src/Platform/tools/webget_win.c, WinHTTP) instead of BSD sockets+openssl.
# Usage: tools/build-misc-win32.sh [OUTDIR]  (default build/misc-win)
#   OUTDIR/public_html/bin/*.exe and OUTDIR/bin/*.exe, mirroring the runtime.
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT=${1:-$ROOT/build/misc-win}; W=$ROOT/build/win32
CC=${MINGW:-x86_64-w64-mingw32-gcc}
[ -f "$W/libwims-w32.a" ] || bash "$ROOT/tools/build-win32.sh" >/dev/null
S=$W/stage; M=$S/Misc
rm -rf "$OUT"; mkdir -p "$OUT/public_html/bin" "$OUT/bin" "$OUT/obj"
CF="-O2 -w -static -I$S -DGNU_SOURCE -DVERSION_DATE=\"$(date +%Y-%m-%d)\""
$CC -O2 -c "$ROOT/src/Platform/tools/binmode_win.c" -o "$OUT/obj/binmode.o"
L="$OUT/obj/binmode.o $W/libwims-w32.a -lm -lpthread"
ok=(); ko=()
mk() { # dest name sources...
  local d=$1 n=$2; shift 2
  if $CC $CF -o "$OUT/$d/$n.exe" "$@" $L 2>"$OUT/obj/$n.err"; then ok+=("$d/$n"); else ko+=("$d/$n"); fi
}
for p in curvecomp cyclicode ftype oncechar shortpath Plouffe html2msg huffman; do   # clickzone: libgd + system() -> to do
  mk public_html/bin $p "$M/$p.c"; done
mk public_html/bin translator "$M/translator.c" "$M/suffix.c"
for p in exostat modstat ccsum phtmltext; do mk bin $p "$M/$p.c"; done
mk bin dicsort "$M/dicsort.c" "$M/suffix.c"
mk bin modind "$M/modind.c" "$M/suffix.c" "$M/translator_.c"
O=$S/OefMsg2wims
mk bin oef2wims $O/oeflines.c $O/compare.c $O/outexec.c $O/process.c $O/sp.c $O/oef2wims.c
mk bin msg2wims $O/msglines.c $O/compare.c $O/def.c $O/doc.c $O/sp.c $O/msg2wims.c
cp "$OUT/bin/msg2wims.exe" "$OUT/public_html/bin/" 2>/dev/null || true
if $CC -O2 -Wall -static -o "$OUT/public_html/bin/webget.exe" "$ROOT/src/Platform/tools/webget_win.c" \
     "$OUT/obj/binmode.o" -lwinhttp -lws2_32 2>"$OUT/obj/webget.err"; then ok+=(public_html/bin/webget); else ko+=(public_html/bin/webget); fi
echo "built (${#ok[@]}): ${ok[*]}"
[ ${#ko[@]} -eq 0 ] || { echo "FAILED (${#ko[@]}): ${ko[*]} (see $OUT/obj/*.err)"; exit 1; }
