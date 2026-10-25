#!/bin/bash
# Parité oef2wims.exe (référence) vs oef2wims-trace.exe sous wine, sur un corpus.
# Usage : bash parity_trace.sh [CORPUS]
H=$(cd "$(dirname "$0")" && pwd); REPO=$(cd "$H/../../.." && pwd)
OUT=${OUT:-$REPO/build/oef-validation}; CORPUS=$(readlink -f "${1:-corpus_oef}")
R=$OUT/win/oef2wims.exe; T=$OUT/win/oef2wims-trace.exe; O=$OUT/ptr; rm -rf $O; mkdir -p $O/R $O/T $O/traces
wineserver -p 2>/dev/null; files=$(ls "$CORPUS"/*.oef)
for f in $files; do cp $f $O/R/; cp $f $O/T/; done
for f in $files; do b=$(basename $f .oef); (cd $O/R; WINEDEBUG=-all wine $R $b.oef out_$b.def > so_$b.txt 2>se_$b.txt; echo $? > rc_$b); done
for f in $files; do b=$(basename $f .oef); (cd $O/T; OEF2WIMS_TRACE=$O/traces/$b.trace WINEDEBUG=-all wine $T $b.oef out_$b.def > so_$b.txt 2>se_$b.txt; echo $? > rc_$b); done
same=0; diff=0; TS=$OUT/parity_trace.tsv; : > $TS
for f in $files; do b=$(basename $f .oef); r=""
 for x in so_$b.txt se_$b.txt rc_$b out_$b.def; do if [ -e $O/R/$x ] || [ -e $O/T/$x ]; then cmp -s $O/R/$x $O/T/$x || r="$r DIFF:$x"; fi; done
 [ -z "$r" ] && same=$((same+1)) || diff=$((diff+1)); echo -e "$b\t$(cat $O/R/rc_$b)\t$(cat $O/T/rc_$b)\t${r:-SAME}" >> $TS; done
echo "total=$((same+diff)) same=$same diff=$diff  ($TS)"
