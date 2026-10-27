#! /bin/sh
# Tests Linux de latex2wims (séquentiels). usage : sh tests/run_tests.sh WIMS_HOME [BIN]
#  1. parité mkindex sh amont / portage C (octet par octet)
#  2. corpus : conversion + empreintes, comparées à parity/ref_linux.tsv (reproductibilité)
#  3. wims.cgi : install + check de chaque document du corpus (200, titre, contenu, MathML)
#  4. codes de sortie (usage, runtime absent, --strict)
wh=${1:?usage: sh tests/run_tests.sh WIMS_HOME [latex2wims]}
here=`cd "\`dirname "$0"\`/.." && pwd`
bin=${2:-$here/build/latex2wims}
out=${OUT:-$here/build/tests}
rm -rf "$out"; mkdir -p "$out"
fail=0; total=0
step() { total=$((total+1)); if [ "$1" = 0 ]; then echo "PASS $2"; else echo "FAIL $2"; fail=$((fail+1)); fi; }

sh "$here/tests/mkindex_parity.sh" "$wh" "$bin" >"$out/mkindex.txt" 2>&1; r=$?
cat "$out/mkindex.txt" | tail -1; step $r "parité mkindex sh/C"

PERL5LIB=$here/tests/perl PERL5OPT=-ML2wFixTime "$bin" corpus --wims-home "$wh" --corpus "$here/corpus" \
  --out "$out/corpus/linux.tsv" --ref "$here/parity/ref_linux.tsv" --log "$out/corpus.log" >/dev/null 2>"$out/corpus.err"; r=$?
tail -1 "$out/corpus.err"; step $r "corpus : empreintes identiques à parity/ref_linux.tsv"

for d in "$here"/corpus/*/; do
  n=`basename "$d"`
  set -- ; case $n in
    exemple) set -- --expect Euler --expect mfrac ;;
    environnements) set -- --expect Pythagore --expect Courbes ;;
    sections_maths) set -- --expect Limites --expect mfrac ;;
    fichier_test) set -- --expect Coloriage ;;
  esac
  "$bin" all --wims-home "$wh" --tex "$d/$n.tex" --module "local/l2w-test-$n.fr" "$@" --log "$out/check-$n.log" >"$out/check-$n.txt" 2>&1; r=$?
  grep '== check' "$out/check-$n.txt"; step $r "wims.cgi $n"
  rm -rf "$wh/public_html/modules/local/l2w-test-$n.fr"
done

"$bin" install >/dev/null 2>&1; step `[ $? = 2 ] && echo 0 || echo 1` "code 2 : usage"
"$bin" install --wims-home /inexistant --tex "$here/exemple.tex" >/dev/null 2>&1; step `[ $? = 3 ] && echo 0 || echo 1` "code 3 : runtime absent"
"$bin" convert --wims-home "$wh" --tex "$here/corpus/accents_cp1252/accents_cp1252.tex" --out "$out/strict" --strict >/dev/null 2>"$out/strict.err"; r=$?
grep -q ':11:45: caractère U+03B1' "$out/strict.err"; d=$?
step `[ $r = 4 ] && [ $d = 0 ] && echo 0 || echo 1` "code 4 : --strict, diagnostic ligne:colonne"
"$bin" check --wims-home "$wh" --module local/inexistant.fr >/dev/null 2>&1; step `[ $? = 1 ] && echo 0 || echo 1` "code 1 : check d'un module absent"

echo "== latex2wims : $((total-fail))/$total tests OK =="
[ $fail = 0 ]
