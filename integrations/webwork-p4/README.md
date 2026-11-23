# integrations/webwork-p4 — WIMS OEF exercises graded inside WeBWorK (P4, build GROK_V1_1)

GROK_V1_1. Debian-tested build (all gates PASS on the dev instance; 1,000-cycle load test NOT RUN by owner rule).
- `INSTALL.md` — how to install (delivered config: worker 127.0.0.1:3003, WeBWorK 3000).
- `Rapport_P4_Build_GROK_V1_1.md` — gate table, every change, open points.
- `worker/` OEF worker · `site-lib/`, `site-macros/` WeBWorK side · `wims/` WIMS patch + hardening ·
  `wims_modules/` the 2 pilot modules · `wrappers/` generator + pilot/menu wrappers · `install/` course creation,
  config hook, **config generator sibling (Debian + Windows)** · `start/` launchers (Linux) · `conf-snippets/` ·
  `gates/` P0–P14 · `baseline/` P9 · `catalog/` exercise search catalogue (SQLite FTS5) indexer + tests ·
  `scan_V1_1/` image scan · `selfcheck_results/` Debian results (JSON/TXT/logs; PDFs and CSV not committed).
- Windows continuation: `docs/Handoff_Windows_P4_GROK_V1.md`.
No secret, generated config, WIMS build or student data is in this folder (`MANIFEST_SHA256.txt` lists every file).
