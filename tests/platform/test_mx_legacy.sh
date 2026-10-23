#!/bin/bash
# Demonstrates bug calc.c:241 through wp_mx_exchange(fixed=0), which mirrors
# the historical loop: with a silent interface it never returns.
cd "$(dirname "$0")"; P=../../src/Platform
cat > /tmp/mxl.c <<'C'
#include <stdio.h>
#include "wims_platform.h"
int main(int c, char**v){ wp_chan ch; wp_pid p; char r[64]; char *a[]={"sleep","30",NULL};
 wp_chan_open(&ch); wp_spawn(&ch,a,NULL,NULL,NULL,WP_ISO_NONE,"t",&p);
 wp_mx_exchange(&ch,"x","MXMARK",r,63,v[1][0]=='1'); wp_kill(&p); puts("returned"); return 0; }
C
gcc -O2 -I$P -o /tmp/mxl /tmp/mxl.c $P/wp_common.c $P/wp_posix.c $P/wp_mx.c || exit 1
timeout 8 /tmp/mxl 0 >/dev/null; rc0=$?
s=$(date +%s); timeout 8 /tmp/mxl 1 >/dev/null; rc1=$?; d=$(( $(date +%s)-s ))
[ $rc0 = 124 ] && echo "PASS legacy (calc.c:241 bug reproduced): silent interface -> still waiting after 8 s" || { echo "FAIL legacy rc=$rc0"; exit 1; }
[ $rc1 = 0 ] && echo "PASS fixed (WIMS_MX_FIX_TIMEOUT): returned after ${d}s" || { echo "FAIL fixed rc=$rc1"; exit 1; }
