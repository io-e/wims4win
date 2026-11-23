#!/bin/bash
# GROK_V1_1 P4: run gates P0..P14 (with P5b, P10c, P12, P13, P14) in dependency order. Results in $P4_RESULTS.
# Environment (I5): every setting comes from p4env_GROK_V1.py; set at least, for an instance that is not the builder's:
#   export P4_ROOT=<extracted p4_build dir>      # code root (default: parent of gates/)
#   export P4_RESULTS=<results dir>  P4_LOGS=<logs dir of the running worker/Morbo/WIMS>   # else inside P4_ROOT
#   export P4_SECRETS=<secrets dir (chmod 700; files chmod 600)>
#   export OEF_PORT=<worker port: 3003 delivered config>   # REQUIRED, no default
#   export WW_PORT=3000 DB_PORT=3307 WIMS_PORT=<wims port>
#   export MARIADB_DEFAULTS=<my.cnf with a [client] section: socket= (or host/port), user=, password= of the webwork DB>
#   export MARIADB_DB=webwork  WIMS_RUNTIME=<wims runtime dir (contains log/, sessions/, public_html/)>
#   export OEF_WORKER_LOG=... WW_SERVER_LOG=... WIMS_ACCESS_LOG=... OEF_WORKER_PIDFILE=<run dir>/oef_worker.pid
#   export OEF_WORKER_SCRIPT=<.../worker/oef_eval_GROK_V1_1.pl>
#   export OEF_WORKER_START="<command that starts the supervisor>" OEF_WORKER_STOP="<command that stops it>"
# Course p4pilot with users p4prof/stud1..3 and sets P4pilot + P4menu (install/create_p4pilot_course_GROK_V1.py).
# P4 resets set P4pilot (status, attempts, past answers); P5b resets stud2's P4pilot rows; P13 resets set P4menu.
# P9 is separate (long): ww/p4/baseline/Grade_Baseline_GROK_V1.py (needs WW_COURSES and WEBWORK_ROOT, I7).
# P10 is the owner's reduced run (50 sequential cycles); the 1,000-cycle load test is NOT RUN by rule.
# P10c waits up to P10C_WAIT seconds (default 480) for wimslogd's session cleaning. P14 runs last (whole-run logs).
set -u
cd "$(dirname "$0")"
: "${OEF_PORT:?set OEF_PORT (3003 delivered config)}"
rc=0
run() { echo "=== $*"; out=$(python3 "$@" 2>&1); echo "$out" | tail -2; echo "$out" | tail -1 | grep -q "PASS" || rc=1; }
for g in gate_P0_P1_GROK_V1.py gate_P1b_bridge_client_GROK_V1.py gate_P4_grade_GROK_V1.py gate_P7_steps_GROK_V1.py \
         gate_P2_noleak_GROK_V1.py gate_P3_seed_GROK_V1.py gate_P6_comma_GROK_V1.py gate_P8_hardcopy_GROK_V1.py \
         gate_P13_menu_GROK_V1_1.py; do run "$g"; done
run gate_P10c_session_cleanup_GROK_V1_1.py --baseline
run gate_P10_functional50_GROK_V1.py
run gate_P10c_session_cleanup_GROK_V1_1.py
for g in gate_P10b_recycling_GROK_V1.py gate_P5_worker_down_GROK_V1.py gate_P5b_no_score_when_down_GROK_V1_1.py \
         gate_P11_wims_hardening_GROK_V1.py gate_P12_wims_auth_GROK_V1_1.py gate_P14_no_secret_in_logs_GROK_V1_1.py; do
  run "$g"; done
echo "run_all exit $rc"
exit $rc
