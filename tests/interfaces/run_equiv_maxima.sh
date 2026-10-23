#!/bin/bash
# Equivalence r19040 (REF) vs refactored (NEW) for the maxima interface (Linux).
# Usage: run_equiv_maxima.sh REF_INTERFACES_DIR NEW_INTERFACES_DIR
REF=$1; NEW=$2; HERE=$(cd "$(dirname "$0")" && pwd); pass=0; fail=0
norm(){ sed -E 's/(Start|End) line ([a-z0-9]+)( [0-9]+){4}/\1 line \2 RND/g; s/_(input|output)\.[0-9]+/_\1.PID/g'; }
one(){ local dir=$1 tag=$2; shift 2; local t=$(mktemp -d)
  ( cd $t && env -i PATH=/usr/bin:/bin HOME=$t tmp_dir=$t "$@" timeout 60 "$dir/maxima" </dev/null >$t/out 2>$t/err; echo $? >$t/rc )
  norm <$t/out >$RES/$tag.out; norm <$t/err >$RES/$tag.err; cp $t/rc $RES/$tag.rc; rm -rf $t; }
check(){ local name=$1; shift; RES=$(mktemp -d); one "$REF" ref "$@"; one "$NEW" new "$@"; local ok=1
  for k in out err rc; do cmp -s $RES/ref.$k $RES/new.$k || ok=0; done
  if [ $ok = 1 ]; then pass=$((pass+1)); echo "PASS $name (rc=$(cat $RES/new.rc)) $(head -c 70 $RES/new.out | tr '\n' '|')"
  else fail=$((fail+1)); echo "FAIL $name"; for k in out err rc; do diff $RES/ref.$k $RES/new.$k | head -5; done; fi; rm -rf $RES; }
echo "== maxima interface: r19040 vs refactored =="
check "maxima: 2+3"               wims_exec_parm='2+3'
check "maxima: factor(x^4-1)"     wims_exec_parm='factor(x^4-1)'
check "maxima: integrate"         wims_exec_parm='integrate(x*exp(x),x)'
check "maxima: illegal (system)"  wims_exec_parm='system("id")'
check "maxima: about"             wims_exec_parm='about'
check "maxima: timeout (silent CAS)" wims_exec_parm='1' maxima_command="sleep 30"
for v in REF NEW; do d=$(mktemp -d); (cd $d && python3 "$HERE/multiexec_driver.py" "${!v}/maxima" MXRND42 'a:5' 'a+1' 'expand((x+1)^2)') >/tmp/mxm_$v.txt 2>&1; rm -rf $d; done
if cmp -s /tmp/mxm_REF.txt /tmp/mxm_NEW.txt && grep -q "'a+1' -> '6'" /tmp/mxm_NEW.txt; then pass=$((pass+1)); echo "PASS maxima multiexec"; sed 's/^/  /' /tmp/mxm_NEW.txt
else fail=$((fail+1)); echo "FAIL maxima multiexec"; diff /tmp/mxm_REF.txt /tmp/mxm_NEW.txt; cat /tmp/mxm_NEW.txt; fi
echo "== result: $pass passed, $fail failed =="; [ $fail = 0 ]
