#!/bin/bash
# Equivalence Linux <-> native Windows (mingw-w64 build run under wine) of the
# pari interface. LIN = Linux pari interface (refactored), WIN = pari.exe.
# Linux uses the Debian gp, Windows the MSYS2 ucrt64 gp.exe (PARI/GP Windows).
# Usage: run_equiv_wine.sh LIN_PARI WIN_PARI_EXE GPWIN_BIN_DIR FAKE_LINUX FAKE_EXE
LIN=$1; WIN=$2; GPDIR=$3; FAKEL=$4; FAKEW=$5; HERE=$(cd "$(dirname "$0")" && pwd)
export WINEDEBUG=-all
WGPDIR="Z:$(echo "$GPDIR" | tr / '\\\\')"; WFAKE="Z:$(echo "$FAKEW" | tr / '\\\\')"
pass=0; fail=0
# also: wine's host-side "XDG_RUNTIME_DIR" warning, and the gp version (Debian
# gp 2.17.2 vs Windows gp 2.19.0) are not differences of the port
norm(){ sed -E '/XDG_RUNTIME_DIR is invalid/d; s/(Start|End) line ([a-z0-9]+)( [0-9]+){4}/\1 line \2 RND/g; s/_(input|output)\.[0-9]+/_\1.PID/g; s/Version [0-9.]+ \(released\)/Version X (released)/'; }
run(){ # side tag stdin env...
  local side=$1 tag=$2 stdin=$3; shift 3; local t=$(mktemp -d)
  if [ $side = lin ]; then
    ( cd $t && env -i PATH=/usr/bin:/bin tmp_dir=$t "$@" timeout 60 "$LIN" <"$stdin" >$t/out 2>$t/err; echo $? >$t/rc )
  else
    ( cd $t && env -i HOME=$HOME PATH=/usr/bin:/bin WINEDEBUG=-all WINEPATH="$WGPDIR" \
        tmp_dir=$t "$@" timeout 60 wine "$WIN" <"$stdin" >$t/out 2>$t/err; echo $? >$t/rc )
  fi
  # CAS stderr is inherited (not piped): gp.exe writes CRLF there. wims reads
  # it back from exec.err -> \r must be stripped on the wims side (phase 3).
  norm <$t/out >$RES/$tag.out; norm <$t/err | tr -d '\r' >$RES/$tag.err; cp $t/rc $RES/$tag.rc; rm -rf $t
}
# check NAME LIN_ENV -- WIN_ENV  (same env if no --)
check(){
  local name=$1; shift; local le=() we=(); local tgt=le
  for a in "$@"; do if [ "$a" = -- ]; then tgt=we; else eval "$tgt+=(\"\$a\")"; fi; done
  [ ${#we[@]} = 0 ] && we=("${le[@]}")
  RES=$(mktemp -d); run lin lin /dev/null "${le[@]}"; run win win /dev/null "${we[@]}"
  local ok=1; for k in ${KEYS:-out err rc}; do cmp -s $RES/lin.$k $RES/win.$k || ok=0; done
  if [ $ok = 1 ]; then pass=$((pass+1)); echo "PASS $name (rc=$(cat $RES/win.rc), out=$(wc -c <$RES/win.out)B) $(head -c 80 $RES/win.out | tr '\n' '|')"
  else fail=$((fail+1)); echo "FAIL $name"; for k in out err rc; do diff <(head -c 400 $RES/lin.$k) <(head -c 400 $RES/win.$k) | head -6; done; fi
  rm -rf $RES
}
echo "== Linux vs Windows(wine) equivalence: pari interface =="
check "gp: 2+3"                 wims_exec_parm='2+3'
check "gp: factor(x^4-1)"       wims_exec_parm='factor(x^4-1)'
check "gp: 2^200"               wims_exec_parm='2^200'
check "gp: large output vector(200000) (fsizelim)" wims_exec_parm='vector(200000,i,i)'
check "gp: illegal command filtered" wims_exec_parm='system("id")'
check "about"                   wims_exec_parm='about'
check "fake CAS: plain"         wims_exec_parm='1+1' pari_command="$FAKEL plain" -- wims_exec_parm='1+1' pari_command="$WFAKE plain"
# Windows strips \r (WP_STRIP_CR_DEFAULT=1): CRLF CAS on Windows == LF CAS on Linux
check "fake CAS: CRLF on Windows == LF on Linux" wims_exec_parm='1+1' pari_command="$FAKEL plain" -- wims_exec_parm='1+1' pari_command="$WFAKE crlf"
check "fake CAS: 20MB flood (fsizelim)" wims_exec_parm='1+1' pari_command="$FAKEL flood" -- wims_exec_parm='1+1' pari_command="$WFAKE flood"
check "timeout: CAS never answers" wims_exec_parm='1+1' pari_command="$FAKEL silent" -- wims_exec_parm='1+1' pari_command="$WFAKE silent"
KEYS="out rc" check "CAS not installed (stdout+rc)" wims_exec_parm='1+1' pari_command="/nonexistent/gp" -- wims_exec_parm='1+1' pari_command='Z:\nonexistent\gp.exe'
# multiexec, persistent session
python3 "$HERE/multiexec_driver.py" "$LIN" MXRND42 'a=5' 'a+1' 'a*10' 'factor(12)' >/tmp/mxw_lin.txt 2>&1
d=$(mktemp -d); (cd $d && WINEPATH="$WGPDIR" MXDRV_WRAP=wine python3 "$HERE/multiexec_driver.py" "$WIN" MXRND42 'a=5' 'a+1' 'a*10' 'factor(12)') >/tmp/mxw_win.txt 2>&1; rm -rf $d
if cmp -s /tmp/mxw_lin.txt /tmp/mxw_win.txt && grep -q "'a+1' -> '6'" /tmp/mxw_win.txt; then pass=$((pass+1)); echo "PASS multiexec: same session across requests"; sed 's/^/  /' /tmp/mxw_win.txt
else fail=$((fail+1)); echo "FAIL multiexec"; diff /tmp/mxw_lin.txt /tmp/mxw_win.txt; fi
echo "== result: $pass passed, $fail failed =="
[ $fail = 0 ]
