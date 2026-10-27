#! /bin/sh
# Parité octet par octet : scripts/docu/mkindex (sh amont) vs « latex2wims mkindex » (portage C).
# Pour chaque document du corpus : arbre préparé sans index (convert --no-index), puis dans le
# MÊME dossier (chemins absolus identiques) : mkindex sh, sauvegarde ; mkindex C, sauvegarde ; diff -r.
# time() de perl est figé (tests/perl/L2wFixTime.pm) pour la date « datm » écrite par latex2wims.pl.
# usage : sh tests/mkindex_parity.sh WIMS_HOME [BIN] [CORPUS]
set -e
wh=${1:?usage: sh tests/mkindex_parity.sh WIMS_HOME [latex2wims] [corpus]}
here=`cd "\`dirname "$0"\`/.." && pwd`
bin=${2:-$here/build/latex2wims}
corpus=${3:-$here/corpus}
wh=`cd "$wh" && pwd`
work=${TMPDIR:-/tmp}/l2w-mkindex-parity.$$
mkdir -p "$work"
PERL5LIB=$here/tests/perl; PERL5OPT=-ML2wFixTime; export PERL5LIB PERL5OPT
export LC_ALL=C
PERL_HASH_SEED=0; PERL_PERTURB_KEYS=0; export PERL_HASH_SEED PERL_PERTURB_KEYS
pass=0; fail=0
for d in "$corpus"/*/; do
  n=`basename "$d"`; [ -f "$d/$n.tex" ] || continue
  "$bin" convert --wims-home "$wh" --tex "$d/$n.tex" --out "$work/pristine" --no-index >/dev/null 2>&1
  rm -rf "$work/m"; cp -a "$work/pristine" "$work/m"
  ( cd "$work/m" && wims_home=$wh w_wims_home=$wh w_wims_class= sh "$wh/public_html/scripts/docu/mkindex" "$wh" >"$work/sh.out" 2>&1 ) || true
  rm -rf "$work/sh"; mv "$work/m" "$work/sh"
  cp -a "$work/pristine" "$work/m"
  ( cd "$work/m" && wims_home= "$bin" mkindex --wims-home "$wh" . >"$work/c.out" 2>&1 ) || true
  rm -rf "$work/c"; mv "$work/m" "$work/c"
  nf=`find "$work/sh" -type f | wc -l`
  if diff -r "$work/sh" "$work/c" >"$work/$n.diff" 2>&1; then
    echo "PASS $n : arbres identiques octet par octet ($nf fichiers)"; pass=$((pass+1)); rm -f "$work/$n.diff"
  else
    echo "FAIL $n : voir $work/$n.diff"; head -20 "$work/$n.diff"; fail=$((fail+1))
  fi
done
echo "== mkindex : $pass identique(s), $fail différent(s) =="
[ $fail = 0 ] && rm -rf "$work"
[ $fail = 0 ]
