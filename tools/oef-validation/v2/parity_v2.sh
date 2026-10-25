#!/bin/bash
# Parité oef2wims-v2.exe (enveloppe) vs oef2wims.exe direct : .def + code de retour (wine).
# Usage : bash parity_v2.sh [CORPUS]   (après ../oef2wims/build.sh et build.sh ; oef2wims.exe doit être à côté de v2)
H=$(cd "$(dirname "$0")" && pwd); REPO=$(cd "$H/../../.." && pwd)
OUT=${OUT:-$REPO/build/oef-validation}; CORPUS=$(readlink -f "${1:-corpus_oef}")
V=$OUT/win/oef2wims-v2.exe; W=$OUT/win/oef2wims.exe; O=$OUT/pv; R=$OUT/pr; rm -rf $O $R; mkdir -p $O $R
same=0; diff=0; T=$OUT/parity_v2.tsv; : > $T
for f in "$CORPUS"/*.oef; do b=$(basename "$f" .oef); cp "$f" $O/; cp "$f" $R/
 (cd $R; WINEDEBUG=-all wine $W $b.oef out_$b.def >/dev/null 2>&1; echo $? > rc_$b)
 (cd $O; WINEDEBUG=-all wine $V --quiet --json $b.oef out_$b.def >/dev/null 2>&1; echo $? > rc_$b)
 r=""; cmp -s $O/rc_$b $R/rc_$b || r="$r RC"; if [ -e $R/out_$b.def ] || [ -e $O/out_$b.def ]; then cmp -s $R/out_$b.def $O/out_$b.def || r="$r DEF"; fi
 [ -z "$r" ] && same=$((same+1)) || diff=$((diff+1)); echo -e "$b\t$(cat $R/rc_$b)\t$(cat $O/rc_$b)\t${r:-SAME}" >> $T; done
echo "total=$((same+diff)) same=$same diff=$diff  ($T)"
