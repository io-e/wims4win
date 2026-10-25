#!/bin/bash
# Build de oef2wims-trace.exe (instrumenté, -finstrument-functions) à partir des mêmes sources WIMS 4.31b.
# Le code WIMS n'est modifié que par 4 hooks #ifdef OEF_TRACE (oef_trace.patch), appliqués sur une copie dans $OUT.
set -e
H=$(cd "$(dirname "$0")" && pwd); REPO=$(cd "$H/../../.." && pwd)
OUT=${OUT:-$REPO/build/oef-validation}; CC_WIN=${CC_WIN:-x86_64-w64-mingw32-gcc}; SH=$H/../oef2wims/shim
B=$OUT/trace; rm -rf "$B"; mkdir -p "$B/obj"; cp -r "$H/../oef2wims/src" "$B/src"; cp "$SH/config_win.h" "$B/src/config.h"
(cd "$B" && patch -s -p0 < "$H/oef_trace.patch")
SRCS="Lib/dir.c Lib/evalue.c Lib/levelcurve.c Lib/liblines.c Lib/math.c Lib/text.c Lib/mt19937ar.c Lib/libwims.c OefMsg2wims/oeflines.c OefMsg2wims/compare.c OefMsg2wims/outexec.c OefMsg2wims/process.c OefMsg2wims/sp.c OefMsg2wims/oef2wims.c"
cd "$B/src"
for f in $SRCS; do $CC_WIN -g -O2 -Wall -finstrument-functions -finstrument-functions-exclude-file-list=/usr/ -DOEF_TRACE -I"$SH" -include oef2wims_win_compat.h -include "$H/oef_trace.h" -DGNU_SOURCE '-DVERSION_DATE="2026-10-25"' -c $f -o ../obj/$(basename $f .c).o; done
cd "$B"
$CC_WIN -O2 -Wall -c "$SH/oef2wims_win_binmode.c" -o obj/zz_binmode.o
$CC_WIN -g -O2 -Wall -I"$H" -c "$H/oef_trace.c" -o obj/zz_trace.o
$CC_WIN -g -static -o obj/oef2wims-trace-dbg.exe obj/*.o -lm -Wl,-Bstatic -lpthread
${CC_WIN%gcc}strip -o obj/stripped.exe obj/oef2wims-trace-dbg.exe
python3 "$H/mksym.py" obj/oef2wims-trace-dbg.exe obj/tmp.exe >/dev/null
# overlay de symboles généré depuis l'exe debug, ajouté à l'exe strippé (mêmes adresses)
python3 - "$B" "$OUT/win/oef2wims-trace.exe" <<'P'
import struct,sys,os
b,dst=sys.argv[1],sys.argv[2]; os.makedirs(os.path.dirname(dst),exist_ok=True)
d=open(b+'/obj/tmp.exe','rb').read(); n=struct.unpack('<I',d[-12:-8])[0]; ov=d[-12-n:]
open(dst,'wb').write(open(b+'/obj/stripped.exe','rb').read()+ov)
P
sha256sum "$OUT/win/oef2wims-trace.exe"
