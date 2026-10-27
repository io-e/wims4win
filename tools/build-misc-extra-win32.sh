#!/bin/bash
# Cross-build the remaining src/Misc programs (the 17 of Misc/Makefile.in
# "all" not covered by build-misc-win32.sh) for native Windows, mingw-w64,
# from the unmodified sources (compat headers/wrappers in src/Platform):
# clickzone wrap voronoi units-filter chemeq
# whirlgif mathexp symtext bioinfo(4) checkmol(+matchmol) lceb crossword
# moneyprint toascii scienceprint sigdigits canvasdraw.
# Usage: tools/build-misc-extra-win32.sh [OUTDIR]  (default build/misc-win)
#   OUTDIR/{public_html/bin,other/bin}/*.exe, mirroring the runtime.
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT=${1:-$ROOT/build/misc-win}; W=$ROOT/build/win32
CC=${MINGW:-x86_64-w64-mingw32-gcc}; CXX=${MINGWXX:-x86_64-w64-mingw32-g++}
[ -f "$W/libwims-w32.a" ] || bash "$ROOT/tools/build-win32.sh" >/dev/null
S=$W/stage; M=$S/Misc; G=$ROOT/build/deps/mingw
mkdir -p "$OUT/public_html/bin" "$OUT/other/bin" "$OUT/obj"
CF="-O2 -w -static -I$S -DGNU_SOURCE -DVERSION_DATE=\"$(date +%Y-%m-%d)\""
[ -f "$OUT/obj/binmode.o" ] || $CC -O2 -c "$ROOT/src/Platform/tools/binmode_win.c" -o "$OUT/obj/binmode.o"
B="$OUT/obj/binmode.o"; L="$B $W/libwims-w32.a -lm -lpthread"
ok=(); ko=()
res() { if [ $1 = 0 ]; then ok+=("$2"); else ko+=("$2"); fi; }
mk()  { local d=$1 n=$2; shift 2
  $CC $CF -o "$OUT/$d/$n.exe" "$@" $L 2>"$OUT/obj/$n.err"; res $? "$d/$n"; }
mkx() { local d=$1 n=$2; shift 2
  $CXX -O2 -w -static -include "$ROOT/src/Platform/win32/compat/bsd_index.h" -I"$OUT/obj/flex" -I"$X/include" -o "$OUT/$d/$n.exe" "$@" $B -lm 2>"$OUT/obj/$n.err"; res $? "$d/$n"; }
set +e
P=$ROOT/src/Platform; X=$ROOT/build/deps/msys2-mingw64/mingw64
[ -f "$X/lib/libgmpxx.a" ] || bash "$ROOT/tools/fetch-mingw-deps.sh" >/dev/null
# wrap..exec: no setuid on Windows, runs argv[1..] (#! via PATH interpreter)
$CC -O2 -Wall -static -o "$OUT/public_html/bin/wrap..exec.exe" "$P/tools/wrap_win.c" 2>"$OUT/obj/wrap.err"; res $? public_html/bin/wrap..exec
[ -f "$G/lib/libgd.a" ] || bash "$ROOT/tools/build-libgd-mingw.sh" >/dev/null
# clickzone: system("magick ...") -> sh -c, never cmd.exe
mk public_html/bin clickzone -DNONDLL -DBGDWIN32 -I"$G/include" "$M/clickzone.c" "$P/tools/system_sh_win.c" -Wl,--wrap=system "$G/lib/libgd.a"
mk public_html/bin voronoi "$M"/voronoiW/*.c
mk public_html/bin mathexp -I"$X/include" "$M"/mathexp/*.c "$X/lib/libregex.a"
mk public_html/bin symtext "$M"/symtext/*.c
# canvasdraw: keyword enum vs windows.h names (SIZE, RECT...), see the header
mk other/bin canvasdraw -include "$P/tools/canvasdraw_win.h" "$M"/canvasdraw/*.c "$P/tools/nofork_win.c"
mk other/bin whirlgif "$M"/whirlgif/*.c
for p in C_etat_civil tab1d tab2d test_cond; do mk other/bin $p "$M/bioinfo/$p.c"; done
mk other/bin checkmol "$M/checkmol/checkmolc.c"
cp -f "$OUT/other/bin/checkmol.exe" "$OUT/other/bin/matchmol.exe" 2>/dev/null
mk other/bin lceb_dynopt "$M/lceb/lceb_dynopt.c"
mk other/bin crossword -I"$P/win32/compat" "$M/crossword/crossword.c"
for p in moneyprint toascii scienceprint sigdigits; do mk other/bin $p "$M/$p/$p.c"; done
mkdir -p "$OUT/obj/flex"; cp -f /usr/include/FlexLexer.h "$OUT/obj/flex/"   # flex C++ header
# chemeq, units-filter: C++ + flex/bison, generated in a scratch copy
T=$OUT/obj/gen; rm -rf "$T"; mkdir -p "$T"; cp -r "$M/chemeq/src" "$T/chemeq"; cp -r "$M/units-filter/src" "$T/units"
( cd "$T/chemeq" && { [ -f chemeq.h ] || { echo '// generated'; sed 's/VERSION.*/VERSION "wims"/' chemeq.h.in; } > chemeq.h; } \
  && flex -ochemlex.cc chemeq.l && bison -o chemparser.cc chemeq.y ) >"$OUT/obj/chemeq.gen" 2>&1
mkx other/bin chemeq "$T/chemeq/chemeq.cc" "$T/chemeq/chemparser.cc" "$T/chemeq/mendeleiev.cc"
( cd "$T/units" && flex -ouniteslex.cc unites.l && bison -o unitesparser.cc unites.y ) >"$OUT/obj/units.gen" 2>&1
mkx other/bin units-filter "$T/units/unitesparser.cc" "$T/units/decimal.cc" "$X/lib/libgmpxx.a" "$X/lib/libgmp.a" "$X/lib/libregex.a"
set -e
echo "built (${#ok[@]}): ${ok[*]}"
[ ${#ko[@]} -eq 0 ] || { echo "FAILED (${#ko[@]}): ${ko[*]} (see $OUT/obj/*.err)"; exit 1; }
