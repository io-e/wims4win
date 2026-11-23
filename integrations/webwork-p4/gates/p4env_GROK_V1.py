#!/usr/bin/env python3
"""GROK_V1 P4 gate settings (V1_1: OEF_PORT must be set explicitly, no default; worker V1_1 scripts), one place, every value overridable by environment (so the same gates run on the dev
instance 3020/3308 and on LW_WEBWORK's reference instance 3000/3307). No secret values here: only file paths."""
import os
from pathlib import Path

P4 = Path(os.environ.get("P4_ROOT", Path(__file__).resolve().parents[1]))
RESULTS = Path(os.environ.get("P4_RESULTS", P4 / "p4_results"))
SECRETS = Path(os.environ.get("P4_SECRETS", P4 / "secrets"))
LOGS = Path(os.environ.get("P4_LOGS", P4 / "logs"))
WW_PORT = os.environ.get("WW_PORT", "3020")
DB_PORT = os.environ.get("DB_PORT", "3308")
if not os.environ.get("OEF_PORT"):
    raise SystemExit("set OEF_PORT (worker port: 3003 in the delivered config, 3013 on the builder's dev box)")
OEF_PORT = int(os.environ["OEF_PORT"])
WIMS_PORT = int(os.environ.get("WIMS_PORT", "8892"))
WW_BASE = os.environ.get("WW_BASE", f"http://127.0.0.1:{WW_PORT}/webwork2")
OEF_URL = os.environ.get("OEF_URL", f"http://127.0.0.1:{OEF_PORT}")
WIMS_URL = os.environ.get("WIMS_URL", f"http://127.0.0.1:{WIMS_PORT}/wims/wims.cgi")
WIMS_RUNTIME = Path(os.environ.get("WIMS_RUNTIME", P4 / "wims/build/wims"))
MARIADB_DEFAULTS = os.environ.get("MARIADB_DEFAULTS", str(P4 / "ww/mariadb/my.cnf"))
MARIADB_DB = os.environ.get("MARIADB_DB", "webwork")
WORKER_SCRIPT = Path(os.environ.get("OEF_WORKER_SCRIPT", P4 / "worker/oef_eval_GROK_V1_1.pl"))
WORKER_PIDFILE = Path(os.environ.get("OEF_WORKER_PIDFILE", LOGS / "oef_worker.pid"))
WORKER_START = os.environ.get("OEF_WORKER_START", f"bash {P4}/ww/p4/start_p4_GROK_V1_1.sh worker")
WORKER_STOP = os.environ.get("OEF_WORKER_STOP", f"bash {P4}/ww/p4/start_p4_GROK_V1_1.sh worker-stop")
WORKER_LOG = Path(os.environ.get("OEF_WORKER_LOG", LOGS / "oef_worker.log"))
MORBO_LOG = Path(os.environ.get("WW_SERVER_LOG", LOGS / "morbo.log"))
WIMS_ACCESS_LOG = Path(os.environ.get("WIMS_ACCESS_LOG", LOGS / "wims.log"))  # dev-cgi-server log (P1 no-call check)
WIMS_TOKEN_FILE = SECRETS / "wims_p4_token"
OEF_ORIGIN_OK = os.environ.get("OEF_ORIGIN", f"http://127.0.0.1:{WW_PORT}")
RESULTS.mkdir(parents=True, exist_ok=True)
