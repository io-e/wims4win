#!/bin/bash
# GROK_V1_1 P4 WeBWorK launcher (I3/I4). Keeps the P4 config across the config generator and gives Morbo the P4
# environment, without editing the reference start script:
#   1. optional: run the reference start script with --no-server (it regenerates localOverrides.conf/pg_config.yml,
#      starts MariaDB, checks tables) - WW_START_SCRIPT, e.g. <...>/webwork4win-mariadb/start_debian_mariadb.sh;
#   2. p4_conf_hook_GROK_V1_1.py re-adds the two P4 lines (idempotent), then --check;
#   3. exec webwork2-morbo with PERL5LIB = P4 site lib + WW_EXTRA_PERL5LIB + webwork2/pg lib, OEF_BRIDGE_CONF exported.
# Required env: WEBWORK_ROOT, P4_SITE_LIB (dir holding WeBWorK/OEFBridge.pm), OEF_BRIDGE_CONF (chmod-600 file),
#               WW_PORT (3000 on the reference instance). Optional: PG_ROOT (default $WEBWORK_ROOT/pg), WW_START_SCRIPT,
#               WW_EXTRA_PERL5LIB (e.g. <...>/webwork4win-mariadb/lib), WW_LOG (Morbo output file).
# Windows: the same 3 steps belong in the webwork4win start script (needs Arnaud's OK; see INSTALL.md section 3.4).
set -euo pipefail
B="$(cd "$(dirname "$0")/.." && pwd)"
: "${WEBWORK_ROOT:?}" "${P4_SITE_LIB:?}" "${OEF_BRIDGE_CONF:?}" "${WW_PORT:?}"
export PG_ROOT="${PG_ROOT:-$WEBWORK_ROOT/pg}" OEF_BRIDGE_CONF
[ -f "$P4_SITE_LIB/WeBWorK/OEFBridge.pm" ] || { echo "no $P4_SITE_LIB/WeBWorK/OEFBridge.pm" >&2; exit 1; }
[ -r "$OEF_BRIDGE_CONF" ] || { echo "OEF_BRIDGE_CONF not readable" >&2; exit 1; }
if [ -n "${WW_START_SCRIPT:-}" ]; then bash "$WW_START_SCRIPT" --no-server; fi
python3 "$B/install/p4_conf_hook_GROK_V1_1.py" --webwork-root "$WEBWORK_ROOT" --pg-root "$PG_ROOT"
python3 "$B/install/p4_conf_hook_GROK_V1_1.py" --webwork-root "$WEBWORK_ROOT" --pg-root "$PG_ROOT" --check
export PERL5LIB="$P4_SITE_LIB${WW_EXTRA_PERL5LIB:+:$WW_EXTRA_PERL5LIB}:$WEBWORK_ROOT/lib:$PG_ROOT/lib"
cd "$WEBWORK_ROOT"
if [ -n "${WW_LOG:-}" ]; then exec perl "$WEBWORK_ROOT/bin/dev_scripts/webwork2-morbo" -l "http://127.0.0.1:$WW_PORT" >>"$WW_LOG" 2>&1
else exec perl "$WEBWORK_ROOT/bin/dev_scripts/webwork2-morbo" -l "http://127.0.0.1:$WW_PORT"; fi
