#!/bin/bash
# Parité oef2wims Linux (ELF) vs Windows (.exe sous wine), mêmes sources, sur un corpus.
# Usage : bash parity.sh [CORPUS=dossier de .oef]   (OUT comme build.sh ; résultat $OUT/parity.tsv)
H=$(cd "$(dirname "$0")" && pwd); REPO=$(cd "$H/../../.." && pwd)
OUT=${OUT:-$REPO/build/oef-validation}; CORPUS=$(readlink -f "${1:-corpus_oef}")
L=$OUT/linux/oef2wims; W=$OUT/win/oef2wims.exe; P=$OUT/pt; rm -rf $P; mkdir -p $P/L $P/W
wineserver -p 2>/dev/null
same=0; diff=0; T=$OUT/parity.tsv; : > $T
for f in "$CORPUS"/*.oef; do b=$(basename "$f" .oef); cp "$f" $P/L/; cp "$f" $P/W/
 (cd $P/L; $L $b.oef out_$b.def > so_$b.txt 2>se_$b.txt; echo $? > rc_$b)
 (cd $P/W; WINEDEBUG=-all wine $W $b.oef out_$b.def > so_$b.txt 2>se_$b.txt; echo $? > rc_$b)
 r=""
 for x in so_$b.txt se_$b.txt rc_$b out_$b.def; do
   if [ -e $P/L/$x ] || [ -e $P/W/$x ]; then cmp -s $P/L/$x $P/W/$x || r="$r DIFF:$x"; fi
 done
 [ -z "$r" ] && same=$((same+1)) || diff=$((diff+1))
 echo -e "$b\t$(cat $P/L/rc_$b)\t$(cat $P/W/rc_$b)\t${r:-SAME}" >> $T
done
echo "total=$((same+diff)) same=$same diff=$diff  ($T)"
