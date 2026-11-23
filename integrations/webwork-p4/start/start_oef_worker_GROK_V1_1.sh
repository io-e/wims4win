#!/bin/bash
# GROK_V1_1 P4 OEF worker supervisor (V1_1: also exports OEF_WIMS_TOKEN_FILE): start|stop|status. The worker is a Mojolicious pre-forking server: its manager
# replaces each worker process after OEF_MAX_REQUESTS connections without closing the listening socket (no gap).
# This loop only restarts the manager if it dies. A non-zero exit 5 times within 60 s stops the loop (bad config, port taken).
# Required env: OEF_WORKER (path to oef_eval_GROK_V1_1.pl), OEF_MODULES (F1: exact module allowlist), OEF_TOKEN_FILE, OEF_WIMS_TOKEN_FILE (WIMS p4 shared secret), OEF_MODULE_ROOTS, OEF_WIMS_BASE,
#               OEF_LISTEN (127.0.0.1:3003), OEF_ALLOWED_ORIGIN (WeBWorK origin, e.g. http://127.0.0.1:3000),
#               OEF_RUN_DIR (pid files + log). No secret is passed on the command line.
set -uo pipefail
: "${OEF_WORKER:?}" "${OEF_MODULES:?}" "${OEF_TOKEN_FILE:?}" "${OEF_MODULE_ROOTS:?}" "${OEF_WIMS_BASE:?}" "${OEF_LISTEN:?}" "${OEF_RUN_DIR:?}"
set +u; export OEF_MODULES OEF_TOKEN_FILE OEF_WIMS_TOKEN_FILE OEF_MODULE_ROOTS OEF_WIMS_BASE OEF_LISTEN OEF_ALLOWED_ORIGIN OEF_MAX_REQUESTS OEF_RATE OEF_TIMEOUT OEF_WORKERS
export OEF_PREFORK_PIDFILE=${OEF_PREFORK_PIDFILE:-$OEF_RUN_DIR/oef_prefork.pid}; set -u
SUP=$OEF_RUN_DIR/oef_worker_supervisor.pid; CHILD=$OEF_RUN_DIR/oef_worker.pid; LOG=$OEF_RUN_DIR/oef_worker.log
mkdir -p "$OEF_RUN_DIR"
running() { [ -f "$SUP" ] && kill -0 "$(cat "$SUP")" 2>/dev/null; }
case "${1:-start}" in
start)
  running && { echo "already running (supervisor $(cat "$SUP"))"; exit 0; }
  nohup bash -c '
    child=; trap "[ -n \"\$child\" ] && kill \$child 2>/dev/null; rm -f \"$1\" \"$2\"; exit 0" TERM INT
    fails=(); while :; do
      perl "$3" & child=$!; echo $child > "$2"; wait $child; rc=$?; child=
      if [ $rc -ne 0 ]; then now=$(date +%s); fails+=($now); recent=0
        for t in "${fails[@]}"; do [ $((now - t)) -lt 60 ] && recent=$((recent + 1)); done
        [ $recent -ge 5 ] && { echo "supervisor: worker failed 5 times in 60 s (rc=$rc), giving up" >&2; rm -f "$1" "$2"; exit 1; }
      fi
      sleep 0.5
    done' oef-supervisor "$SUP" "$CHILD" "$OEF_WORKER" >> "$LOG" 2>&1 &
  echo $! > "$SUP"
  for i in $(seq 1 40); do (exec 3<>"/dev/tcp/${OEF_LISTEN%:*}/${OEF_LISTEN#*:}") 2>/dev/null && break; sleep 0.25; done
  echo "started supervisor $(cat "$SUP") worker $(cat "$CHILD" 2>/dev/null)";;
stop)
  running && kill "$(cat "$SUP")"; for i in $(seq 1 40); do running || break; sleep 0.25; done; rm -f "$SUP" "$CHILD"; echo stopped;;
status)
  running && echo "running supervisor $(cat "$SUP") worker $(cat "$CHILD" 2>/dev/null)" || { echo "not running"; exit 1; };;
*) echo "usage: $0 start|stop|status"; exit 2;;
esac
