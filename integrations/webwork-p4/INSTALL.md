# INSTALL — P4 build GROK_V1_1 (WeBWorK with WIMS OEF exercises)

GROK_V1_1 (replaces the V1 INSTALL; the V1 archive `p4_build_GROK_V1.tar.gz` is unchanged). For LW_WEBWORK's
reference instance (webwork2 47b53d4 / pg 6a4fd3a, MariaDB, 127.0.0.1:3000/3307). **Delivered config: OEF worker on
127.0.0.1:3003.** The builder's self-check ran on a dev copy (WeBWorK 3020, MariaDB 3308, WIMS 8892, worker **3013**;
see `Rapport_P4_Build_GROK_V1_1.md`). **No secret is in this archive**: you create the tokens and passwords on your side
(chmod 600, gitignored). No upstream webwork2/pg/BPL file is edited; WIMS changes come as one patch file.

Paths: `$B` = where you extract this archive; `$SECRETS` = your chmod-700 secrets dir; `$WW` = webwork2 root
(`$WEBWORK_ROOT`), `$PG` = `$PG_ROOT`; `$WIMS_RT` = WIMS runtime dir (holds `public_html/`, `log/`, `sessions/`).
Random hex values (I2: no `xxd` needed): `python3 -c "import secrets;print(secrets.token_hex(32))"` (64 hex).

## 0. Check the archive
```
cd $B && sha256sum -c MANIFEST_SHA256.txt
```

## 1. WIMS (loopback only)
1. A WIMS runtime built from io-e/wims4win master b21702a (svn r19040): `wims/build_linux_wims_GROK_V1.sh`
   (reference). Debian packages for the pilot exercises: `maxima pari-gp gnuplot-nox`.
2. Serve it on **127.0.0.1 only** (dev: `tools/wims-automation/http/dev-cgi-server.py --runtime $WIMS_RT --port <PORT>`).
   The `wimslogd` daemon of this WIMS must run (it cleans sessions; gate P10c).
3. **WIMS patch V1_1** (cumulative; replaces `p4_json_GROK_V1.diff`). If V1 is applied, reverse it first:
   ```
   cd $WIMS_RT/public_html
   patch -p1 -R --dry-run < <V1>/wims/p4_json_GROK_V1.diff && patch -p1 -R < <V1>/wims/p4_json_GROK_V1.diff   # only if V1 applied
   patch -p1 --dry-run < $B/wims/p4_wims_GROK_V1_1.diff && patch -p1 < $B/wims/p4_wims_GROK_V1_1.diff
   ```
   It adds: `scripts/oef/p4_json.phtml` (the hook; answers only in sessions marked `wims_p4_auth=yes`, else
   `{"p4":1,"error":"auth"}`; never emits an expected or given answer), `scripts/p4/p4_auth.proc` (constant-time
   compare of the POST field `p4_auth` with the secret file), `modules/adm/raw/jobs/p4render.proc` (authenticated copy
   of `lightpopup` for the worker: refuses without the secret, refuses `adm/` and `..` modules, marks the session);
   patches `adm/raw/var.proc` + `var.def` (job `p4render`, variable `p4_auth`), `scripts/oef/answer.phtml` and
   `Main.phtml` (hook call). `lightpopup` and `algogebra_json.phtml` are not changed. Normal WIMS use is unchanged (P12).
4. Pilot modules **not in an r19040 tree** (I1): both are shipped (public WIMS modules):
   ```
   tar -C $WIMS_RT/public_html/modules -xzf $B/wims_modules/U2_arithmetic_oefrational.fr_GROK_V1.tar.gz
   tar -C $WIMS_RT/public_html/modules -xzf $B/wims_modules/H3_analysis_oeflinf.fr_GROK_V1_1.tar.gz
   ```
   (oeflinf.fr: copy of `mw-fr-modules-20261114/sanity-linux/public_html/modules/H3/analysis/oeflinf.fr`, unchanged.)
5. Hardening + P4 secret: `python3 $B/wims/harden_wims_p4_GROK_V1_1.py $WIMS_RT $SECRETS [--rotate-p4grok]`
   - every connection file except p4grok gets `ident_password=*` (myself included). I8: an r19040 tree has only
     `myself` and `wimsunice` (no `toto`); all are disabled;
   - `p4grok`: `ident_site=127.0.0.1`, `ident_agent=P4-OEF-GROK`, `ident_allow=gettime listmodules getmodule`,
     32-hex password in `$SECRETS/wims_p4grok_password` (created if missing; `--rotate-p4grok` replaces it);
   - `$SECRETS/wims_p4_token` (64 hex, created if missing) copied to `$WIMS_RT/log/p4_auth/p4_secret.proc`
     (dir 700, file 600; `log/` is never served). Re-run after any token change.
   - Send `ident`/`passwd` to adm/raw only as **multipart POST** to `wims.cgi?form-data<unique suffix>` (not in a
     URL: URLs are logged; the suffix must change at each request, because WIMS answers a request whose query string
     equals the previous one of the session with the previous page, `src/variables.c` `check_session`). WIMS saves adm/raw request variables in the session's `var` file; delete those sessions after admin
     calls (gate P11 does it for its own).
6. **Busy WIMS (wims.conf, Debian dev instance only)**: `--dev-wims-conf` writes in `$WIMS_RT/log/wims.conf` the 3
   key=value lines below between the BEGIN/END markers (the notes after `#` here are explanations; the script writes its own comment lines)
   ```
   # BEGIN GROK_V1_1 P4 dev ...
   threshold1=2000      # default 150. Refused (HTTP 450 "threshold"): runnable processes x20 > threshold1+2, or 5-min
                        #   load x100 > threshold1 (anonymous new sessions); CPU allowance cut from 2/3 of it
   threshold2=4000      # default 300. Refused: runnable processes x12 > threshold2+3 (even priority sites),
                        #   1-min load x50 > threshold2, 5-min load x100 > threshold2 (new sessions and anonymous)
   priority=127.0.0.1   # addresses exempt from every check except "runnable x12 > threshold2" (worker, WeBWorK)
   idle_time3=120       # default 900. wimslogd keeps every session idle < idle_time3; after it, idle popup/anonymous
                        #   sessions (the worker's lightpopup/p4render ones) are removed at the next cleaning (<= 300 s)
   # END GROK_V1_1 P4 dev
   ```
   Keys read by `src/config.c`, used in `check_load()` of `src/auth.c` (r19040). **Windows instance: do not apply without Arnaud's explicit OK.**
   Restart the WIMS server after the change, and restart **this WIMS's** `wimslogd` (it reads idle_time* at start;
   kill the pid in `$WIMS_RT/tmp/log/wimslogd.pid` after checking its cwd is `$WIMS_RT`; the next request restarts it).

## 2. OEF worker (127.0.0.1:3003)
Perl + Mojolicious (as for webwork2). Tokens:
```
python3 -c "import secrets;print(secrets.token_hex(32))" > $SECRETS/oef_token && chmod 600 $SECRETS/oef_token
# $SECRETS/wims_p4_token: created by harden_wims_p4_GROK_V1_1.py (1.5)
```
Start through the supervisor (memory recycling after `OEF_MAX_REQUESTS`=2000, no listening gap):
```
export OEF_MODULES="H3/analysis/oeflinf.fr U2/arithmetic/oefrational.fr" \
       OEF_WORKER=$B/worker/oef_eval_GROK_V1_1.pl OEF_TOKEN_FILE=$SECRETS/oef_token \
       OEF_WIMS_TOKEN_FILE=$SECRETS/wims_p4_token \
       OEF_LISTEN=127.0.0.1:3003 OEF_ALLOWED_ORIGIN=http://127.0.0.1:3000 \
       OEF_WIMS_BASE=http://127.0.0.1:<WIMS PORT>/wims/wims.cgi \
       OEF_MODULE_ROOTS=$WIMS_RT/public_html/modules OEF_RUN_DIR=<run dir for pid files + oef_worker.log>
bash $B/start/start_oef_worker_GROK_V1_1.sh start      # stop | status
```
The worker refuses a non-loopback listen address or WIMS URL, and a WIMS token that is not 64 hex.
Modules (F1): **`OEF_MODULES` is the site's exact allowlist** (required; the worker refuses to start without it or
with a malformed entry); a .pg author can only use a module from it. Each must also be `<Letter><1-2 digits>/<path>`
under `OEF_MODULE_ROOTS` with `def/` and `src/`; `adm/`, system dirs, `..`, absolute paths, empty names and symlinks
are refused; the exercise needs `def/<exo>.def`. Add a module = add it to OEF_MODULES and restart the worker. All WIMS calls that carry the secret are multipart
POSTs (not in URLs). Math is asked from WIMS as MathML (`useropts=32`) and returned **as TeX**: `\(...\)` in `statement_html` (typeset
by WeBWorK's MathJax, same delimiters as PG) and in `statement_tex` (WeBWorK's LaTeX hardcopy takes it as is).
Logs: module, exo, seed, status, ms only.

## 3. WeBWorK side
1. Trusted module `site-lib/WeBWorK/OEFBridge.pm` in a site lib dir on webwork2's PERL5LIB (not in `$WW/lib`/`$PG/lib`).
2. Macro: `site-macros/oefRemote.pl` into the site macros dir on `$pg{directories}{macrosPath}`.
3. Bridge config: `conf-snippets/oef_bridge.conf.example` → `$SECRETS/oef_bridge.conf` (chmod 600), `url=http://127.0.0.1:3003`,
   `token_file=$SECRETS/oef_token`.
4. **Config that survives the generator (I3) and Morbo environment (I4).** `localOverrides.conf` and `pg_config.yml`
   are regenerated by `Apply_WebWork4Win_Conf_GROK_V1.py` at every `start_debian_mariadb.sh`: do **not** hand-edit them.
   Use one of:
   - **Generator-integrated (preferred, Debian and Windows)**: `install/Apply_WebWork4Win_Conf_P4_GROK_V1_1.py`, a
     NEW sibling of `Apply_WebWork4Win_Conf_GROK_V1.py` (the existing generator is not edited). Copy it next to the
     generator (`webwork4win-mariadb/` on Debian, `webwork4win\` on Windows) and call it instead of the generator,
     same arguments. It runs the generator unchanged and makes its `write_overrides`/`write_pg` also emit
     `push @{$pg{modules}}, [qw(WeBWorK::OEFBridge)];` (append, never replace) and the `pg_config.yml` item, so every
     regeneration keeps P4. Debian: `conf-snippets/start_debian_mariadb_P4_GROK_V1_1.diff` makes the start script call
     it when `P4_GENERATOR` is set (and puts `P4_SITE_LIB` in PERL5LIB). Windows: in `Start_WebWork4Win_GROK_V1.ps1`
     the two `python ...Apply_WebWork4Win_Conf_GROK_V1.py` calls would call the sibling, `$env:PERL5LIB` would start
     with the P4 site lib dir and `$env:OEF_BRIDGE_CONF` would be set — **needs Arnaud's OK; not tested on Windows**
     (tested on Linux against the read-only Windows generator, sha256 068de768..., and the Debian one, bb9b20c3...:
     `install/test_generator_sibling_GROK_V1_1.py`, result `selfcheck_results/I3_generator_sibling_test.txt`).
   - **Launcher (no change to the reference scripts)**:
     ```
     export WEBWORK_ROOT=$WW PG_ROOT=$PG WW_PORT=3000 P4_SITE_LIB=$B/site-lib OEF_BRIDGE_CONF=$SECRETS/oef_bridge.conf \
            WW_START_SCRIPT=<...>/webwork4win-mariadb/start_debian_mariadb.sh WW_EXTRA_PERL5LIB=<...>/webwork4win-mariadb/lib
     bash $B/start/start_webwork_p4_GROK_V1_1.sh
     ```
     It runs the reference script with `--no-server` (generator, MariaDB, tables), then
     `install/p4_conf_hook_GROK_V1_1.py` (idempotent; adds `push @{$pg{modules}}, [qw(WeBWorK::OEFBridge)];` and the
     `pg_config.yml` item between markers; `--check` verifies), then execs Morbo with the P4 PERL5LIB and OEF_BRIDGE_CONF.
     Note: `WW4WIN_MODULES`/`PERL5OPT` of the reference script are not carried over; export `PERL5OPT` yourself if used.
   - **Or the optional hook in the reference script**: `conf-snippets/start_debian_mariadb_P4_GROK_V1_1.diff`
     (`P4_CONF_HOOK=$B/install/p4_conf_hook_GROK_V1_1.py`, `P4_SITE_LIB=$B/site-lib`, export `OEF_BRIDGE_CONF`).
   - Windows/webwork4win: the same three steps belong in its start script / generator — needs Arnaud's OK.
   Restart webwork2 after any change to OEFBridge.pm or oef_bridge.conf.
5. Course `p4pilot` (prof `p4prof`, students `stud1..3`, sets `P4pilot` = 10 wrappers and `P4menu` = drop-down test, P13):
   ```
   export WEBWORK_ROOT=$WW PG_ROOT=$PG WW_COURSES=<courses dir>
   python3 $B/install/create_p4pilot_course_GROK_V1.py --secrets $SECRETS --wrappers $B/wrappers/generated \
           --menu-wrappers $B/wrappers/generated_menu
   ```

## 4. Grades and attempts (read before using in a course)
- **Grade of record = WeBWorK's**: the average over the answer blanks (each blank right/wrong as WIMS judges it). It
  differs from the WIMS score formula (WIMS weights and penalises, e.g. perden 1 of 2 right: WIMS 0.25, WeBWorK 0.5;
  prob2: WIMS 0.44, WeBWorK 0.667). The WIMS score is not stored.
- **Unlimited attempts** on OEF sets (max_attempts -1). Reason: when the worker is down, WeBWorK still counts the
  submission as an attempt (score 0, message « Service de correction indisponible / grading service unavailable »,
  nothing above 0 recorded, P5b); this cannot be undone without a webwork2 patch. Multi-step OEF exercises
  (`\steps`): steps are shown one submit at a time, each submit counts as an attempt; not fixable without a webwork2
  patch, so keep unlimited attempts.
- **Worker down — what the teacher sees** (P5b): each submission made during the outage is a normal WeBWorK attempt
  scored 0 (`num_incorrect` +1 per submission, one `past_answer` row per submission with score 0). The instructor
  "past answers" page (`/<course>/show_answers/`) lists each of them with the typed answer marked **incorrect** (red),
  even when the typed answer was right, with no outage note; the student sees the « indisponible » message; when the worker is back, the next
  right submission gives 100 % and the problem status is the best score. No webwork2 hook is used.
- **Printing**: formulas are TeX in the hardcopy (P8). The 3 items that printed `[figure]` in V1 (fnctlin2, ratapprox,
  approx) now print their formulas and pass P8; keep them out of printed sets only on an install where P8 fails.
  Real **pictures** are not embedded in the PDF by this build (they print `[figure]`): exercises whose pictures carry
  data (e.g. `CORequa*`, `classer*`) are not in the pilot and must stay out of printed sets.

## 5. Gates
`gates/run_all_gates_GROK_V1_1.sh` — the header lists every environment variable (I5: `P4_ROOT`, `P4_RESULTS`,
`P4_LOGS`, `P4_SECRETS`, `OEF_PORT` (**required**, 3003), `MARIADB_DEFAULTS` = a my.cnf with a `[client]` section giving
socket (or host/port), user and password of the webwork DB, ...). Set `P4_RESULTS` outside the extracted archive.
Gate P0 prints the instance it runs against from the environment (I6). Needs Python 3 stdlib, `mariadb` client,
`pdftotext`/`pdfinfo`, `ss`, `ip`. P4 resets set P4pilot; P5b resets stud2's rows; P13 resets set P4menu. P10 is the
reduced 50-cycle run (1,000-cycle load test not run: owner rule). P10c waits for wimslogd (up to 8 min; needs idle_time3=120, section 1.6). P14 (last)
checks that no secret value is in any log.
P9 (I7: `WW_COURSES` and `WEBWORK_ROOT` are **required**, the script stops without them):
```
cd $B/baseline && WW_COURSES=<courses> WEBWORK_ROOT=$WW PG_ROOT=$PG PERL5LIB=... OEF_BRIDGE_CONF=... \
  python3 Grade_Baseline_GROK_V1.py --course bpltest --extra-list grade_extra_C_failures.txt \
  --out tmp/grade_baseline_P4.csv --compare <your grade_baseline_comma_input.csv>
```

## 6. What is never shipped
Secrets, generated config (localOverrides.conf, pg_config.yml, site.conf, wims.conf), WIMS build, course data, logs.
