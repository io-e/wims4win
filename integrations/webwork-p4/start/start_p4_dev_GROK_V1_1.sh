#!/bin/bash
# GROK_V1_1 P4 start script (dev worker port: OEF_PORT, 3013 on the dev box; delivered config 3003) (own instance; never touches LW's 3000/3307).
# Usage: start_p4_GROK_V1.sh [all|mariadb|webwork|wims|worker|stop]
set -euo pipefail
P4=/workspace/mw-p4-20261123; WW=$P4/ww; SRC=$WW/src
WW_PORT=${WW_PORT:-3020}; DB_PORT=3308; WIMS_PORT=${WIMS_PORT:-8892}; OEF_PORT=${OEF_PORT:?set OEF_PORT (3013 on the dev box)}
port_open() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }
export WEBWORK_ROOT=$SRC PG_ROOT=$SRC/pg WW_COURSES=$WW/courses
export PERL5LIB="$WW/site-lib:$SRC/lib:$SRC/pg/lib"
export OEF_BRIDGE_CONF=$P4/secrets/oef_bridge.conf
mariadb_up() { port_open $DB_PORT || { nohup /usr/sbin/mariadbd --defaults-file=$WW/mariadb/my.cnf >/dev/null 2>&1 &
  for i in $(seq 1 30); do mariadb-admin --defaults-file=$WW/mariadb/my.cnf ping >/dev/null 2>&1 && break; sleep 0.5; done; }; }
wims_up() { port_open $WIMS_PORT || { nohup python3 $P4/../mw-proto-webwork-20261123/C_admraw/wims-master-b21702a/tools/wims-automation/http/dev-cgi-server.py \
  --runtime $P4/wims/build/wims --port $WIMS_PORT --pid-file $P4/logs/wims_$WIMS_PORT.pid > $P4/logs/wims.log 2>&1 & sleep 1; }; }
worker_up() { OEF_MODULES="${OEF_MODULES:-H3/analysis/oeflinf.fr U2/arithmetic/oefrational.fr}" OEF_WORKER=$P4/worker/oef_eval_GROK_V1_1.pl OEF_TOKEN_FILE=$P4/secrets/oef_token OEF_WIMS_TOKEN_FILE=$P4/secrets/wims_p4_token OEF_LISTEN=127.0.0.1:$OEF_PORT \
  OEF_ALLOWED_ORIGIN=http://127.0.0.1:$WW_PORT OEF_WIMS_BASE=http://127.0.0.1:$WIMS_PORT/wims/wims.cgi \
  OEF_MODULE_ROOTS=$P4/wims/build/wims/public_html/modules OEF_RUN_DIR=$P4/logs bash $P4/start/start_oef_worker_GROK_V1_1.sh start; }
# V1_1: the dev config generator through the P4 sibling (install/Apply_WebWork4Win_Conf_P4_GROK_V1_1.py, base
# Apply_P4_Conf_GROK_V1.py; regenerates localOverrides.conf/pg_config.yml WITH the P4 lines) then the
# delivered launcher start/start_webwork_p4_GROK_V1_1.sh (P4 conf hook + PERL5LIB + OEF_BRIDGE_CONF + Morbo).
webwork_up() { port_open $WW_PORT || { WW_P4_PROBE=${WW_P4_PROBE:-0} P4_BASE_GENERATOR=$WW/p4/Apply_P4_Conf_GROK_V1.py python3 $P4/install/Apply_WebWork4Win_Conf_P4_GROK_V1_1.py >/dev/null
  : > $P4/logs/morbo.log; WW_PORT=$WW_PORT P4_SITE_LIB=$WW/site-lib WW_LOG=$P4/logs/morbo.log \
    nohup bash $P4/start/start_webwork_p4_GROK_V1_1.sh > $P4/logs/morbo_launcher.log 2>&1 &
  echo $! > $P4/logs/morbo.pid; for i in $(seq 1 60); do port_open $WW_PORT && break; sleep 1; done; }; }
stop_all() {
  [ -f $P4/logs/oef_worker_supervisor.pid ] && kill $(cat $P4/logs/oef_worker_supervisor.pid) 2>/dev/null || true
  [ -f $P4/logs/morbo.pid ] && pkill -P $(cat $P4/logs/morbo.pid) 2>/dev/null; [ -f $P4/logs/morbo.pid ] && kill $(cat $P4/logs/morbo.pid) 2>/dev/null || true
  [ -f $P4/logs/wims_$WIMS_PORT.pid ] && kill $(cat $P4/logs/wims_$WIMS_PORT.pid) 2>/dev/null || true
  mariadb-admin --defaults-file=$WW/mariadb/my.cnf shutdown 2>/dev/null || true; }
case "${1:-all}" in
  mariadb) mariadb_up;; webwork) mariadb_up; webwork_up;; wims) wims_up;; worker) worker_up;; worker-stop) OEF_MODULES=x OEF_WORKER=x OEF_TOKEN_FILE=x OEF_MODULE_ROOTS=x OEF_WIMS_BASE=x OEF_LISTEN=x OEF_RUN_DIR=$P4/logs bash $P4/start/start_oef_worker_GROK_V1_1.sh stop;;
  all) mariadb_up; wims_up; worker_up; webwork_up;; stop) stop_all;; *) echo "bad arg"; exit 2;;
esac
