#!/bin/bash
# Equivalence tests: Interfaces/<prog> built from r19040 (REF) vs refactored (NEW).
# Usage: run_equiv.sh REF_INTERFACES_DIR NEW_INTERFACES_DIR
# Compares stdout, stderr and exit status (random start/end markers normalised).
REF=$1; NEW=$2; HERE=$(cd "$(dirname "$0")" && pwd)
pass=0; fail=0
norm(){ sed -E 's/(Start|End) line ([a-z0-9]+)( [0-9]+){4}/\1 line \2 RND/g; s/_(input|output)\.[0-9]+/_\1.PID/g'; }
run_one(){ # dir name env... -- stdin-file
  local dir=$1 tag=$2 stdin=$3; shift 3
  local t=$(mktemp -d); local t0=$(date +%s.%N)
  ( cd "$t" && env -i PATH=/usr/bin:/bin tmp_dir="$t" "$@" timeout 40 "$dir/pari" <"$stdin" >"$t/out" 2>"$t/err"; echo $? >"$t/rc" )
  local t1=$(date +%s.%N)
  printf '%s\n' "$(norm <"$t/out")" >"$RES/$tag.out"
  printf '%s\n' "$(norm <"$t/err")" >"$RES/$tag.err"
  cat "$t/rc" >"$RES/$tag.rc"
  awk -v a=$t0 -v b=$t1 'BEGIN{printf "%d\n", b-a}' >"$RES/$tag.sec"
  rm -rf "$t"
}
check(){ # name, env..., stdin via $STDIN
  local name=$1; shift
  RES=$(mktemp -d); run_one "$REF" ref "${STDIN:-/dev/null}" "$@"; run_one "$NEW" new "${STDIN:-/dev/null}" "$@"
  local ok=1
  for k in out err rc; do cmp -s "$RES/ref.$k" "$RES/new.$k" || ok=0; done
  local dt=$(( $(cat $RES/ref.sec) - $(cat $RES/new.sec) )); [ ${dt#-} -le 2 ] || ok=0
  if [ $ok = 1 ]; then pass=$((pass+1)); echo "PASS $name (rc=$(cat $RES/new.rc), ${#}env, $(cat $RES/new.sec)s, out=$(wc -c <$RES/new.out)B)";
  else fail=$((fail+1)); echo "FAIL $name"; for k in out err rc sec; do diff <(head -c 600 $RES/ref.$k) <(head -c 600 $RES/new.$k) | head -8; done; fi
  [ -n "$SHOW" ] && { echo "  --- out:"; head -c 300 "$RES/new.out"; echo; }
  rm -rf "$RES"
}
GP=$(command -v gp)
echo "== platform layer equivalence (pari interface) =="
SHOW=1 check "gp: 2+3"                wims_exec_parm='2+3'
SHOW=1 check "gp: factor(x^4-1)"      wims_exec_parm='factor(x^4-1)'
check "gp: large output vector(200000)" wims_exec_parm='vector(200000,i,i)'
check "gp: illegal command filtered"  wims_exec_parm='system("id")'
check "about"                         wims_exec_parm='about'
SHOW=1 check "fake CAS: plain"        wims_exec_parm='1+1' pari_command="$HERE/fake_cas.py plain"
SHOW=1 check "fake CAS: CRLF output (Linux keeps \\r)" wims_exec_parm='1+1' pari_command="$HERE/fake_cas.py crlf"
SHOW=1 check "fake CAS: 20MB flood (fsizelim bound)" wims_exec_parm='1+1' pari_command="$HERE/fake_cas.py flood"
SHOW=1 check "timeout: CAS never answers" wims_exec_parm='1+1' pari_command="sleep 30"
SHOW=1 check "CAS not installed"      wims_exec_parm='1+1' pari_command="/nonexistent/gp"
# multiexec: persistent session driven like wims (one request at a time)
for v in REF NEW; do
  d=$(mktemp -d); (cd "$d" && python3 "$HERE/multiexec_driver.py" "${!v}/pari" MXRND42 'a=5' 'a+1' 'a*10' 'factor(12)') >"/tmp/mx_$v.txt" 2>&1; rm -rf "$d"
done
if cmp -s /tmp/mx_REF.txt /tmp/mx_NEW.txt && grep -q "'a+1' -> '6'" /tmp/mx_NEW.txt; then pass=$((pass+1)); echo "PASS multiexec: same session across requests"; sed 's/^/  /' /tmp/mx_NEW.txt
else fail=$((fail+1)); echo "FAIL multiexec"; diff /tmp/mx_REF.txt /tmp/mx_NEW.txt; cat /tmp/mx_NEW.txt; fi
echo "== result: $pass passed, $fail failed =="
[ $fail = 0 ]
