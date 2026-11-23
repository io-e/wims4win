# Rapport P4 Build — GROK_V1_1 (WeBWorK + WIMS OEF, Part A, after LW_WEBWORK's V1 review)

GROK_V1_1 · 2026-11-23 (Europe/Paris) · builder self-check on the dev copy. Archive `p4_build_GROK_V1_1.tar.gz`
(the V1 archive `p4_build_GROK_V1.tar.gz`, SHA-256 8dee317f…fedc9, is unchanged). Inputs: Arnaud's "ok" (12:03),
parent steering 12:08 (F1, P8 HOLD, D2, I1–I8, D4) and 12:39 (LW review refinements 1–6), LW's report
`/workspace/codex_handoff/Rapport_P4_GROK_V1.md` (read only). LW's instance, files, logs and secrets were not read
or touched (except reading LW's report and, read only, the generator/start script sources to base the sibling on).

## 1. Instance used
Own dev copy: WeBWorK (Morbo) **127.0.0.1:3020**, MariaDB **127.0.0.1:3308**, WIMS **127.0.0.1:8892**, OEF worker
**127.0.0.1:3013** (dev port; **delivered config = 3003**, nothing of mine ran on 3003). Temporary test listeners, all
loopback and stopped: 3014 (P10b worker), 3015 (P0/P1 start-refusal tests, debug worker), 3016 (static page for the
MathJax check), 3031 (P1b fake worker).

## 2. Gates — final run `OEF_PORT=3013 P4_RESULTS=p4_results_V1_1 bash gates/run_all_gates_GROK_V1_1.sh`
Log `p4_results_V1_1/run_all_gates_GROK_V1_1.log`, `run_all exit 0`, 4 min 43 s. Earlier runs of this session that
failed or were superseded are kept in `run1_failed/`, `run2_superseded/`, `run3_superseded/` (causes in §4).

| Gate | Key output (final run) | Status | Result file |
|---|---|---|---|
| P0 ports | 3020/3013/3308/8892 on 127.0.0.1 only; text from the environment (I6) | **PASS** | `P0_ports.txt` |
| P1 worker security | 35 request cases (403/415/413/400/404, no CORS) incl. **F1**: adm/raw, adm/class, home, classes/fr, tool/wcalc.fr, area dir, real OEF module **not in the allowlist** (H2/algebra/oefcalcul.fr), empty module/exo, traversal from an allowed module, trailing slash, absolute path, missing `def/<exo>.def`, INDEX; 5 start refusals (non-loopback listen / WIMS URL, no `OEF_MODULES`, `adm/raw` or `../` in it); 0 WIMS call | **PASS** | `P1_security.json` |
| P1b bridge HTTP client | builtin client works inside Safe, HTTP::Tiny dies → builtin stays default (item 6) | **PASS** | `P1b_http_client.json` |
| P2 no leak | 10 items, 0 leak on 8 surfaces; D1 rule (values printed by the native WIMS page are data); positive control stud2 OK; **past-answers page now really read** (`/show_answers/`, V1 read a 404); **0 secret in 157 log files** (item 5) | **PASS** | `P2_noleak.json` |
| P3 seed | 10/10 same seed → same statement; reseed → different; restore → same | **PASS** | `P3_seed.json` |
| P4 grade | 22/22 (right 1.0, wrong 0.0, partial per blank); no WIMS class created (57→57) | **PASS** | `P4_grade.json` |
| P5 worker down | message « Service de correction indisponible / grading service unavailable », score 0, restart case | **PASS** | `P5_worker_down.json` |
| P5b no score when down | stud2 RIGHT answers on 10/10 while stopped: every past_answer score 0, status 0, message shown on submit and view; max_attempts −1 everywhere; 2nd outage submission: num_incorrect 1→2, 2 past_answer rows, both 0; teacher's past-answers page rows: incorrect, incorrect, correct; after restart status 1.0 (num_correct 1, num_incorrect 2) | **PASS** | `P5b_no_score_when_down.json` |
| P6 decimal comma | as V1 | **PASS** | `P6_comma.json` |
| P7 \steps | ratapprox step 1 → step 2 → 1.0 (num_correct 1, num_incorrect 1) | **PASS** | `P7_steps.json` |
| P8 hardcopy | 2 PDFs (stud1; p4prof for stud1 with answers/solutions/hints), 2 pages each: 10/10 statements, 0 expected value, 0 AcroForm, **no `[figure]`**, all 47 numbers written inside formulas present in the PDF (two-column text read left then right) | **PASS** | `P8_hardcopy.json`, `.pdf`, `.txt` |
| P8b web math | worker TeX typeset by WeBWorK's own MathJax (headless Chrome): 10/10 problems, 79 formulas sent = 79 typeset, 0 MathJax error | **PASS** | `P8b_mathjax_web_check.txt` |
| P9 regression | 530 rows vs LW baseline, **0 differing** (PASS 450 / RENDER_ERR 30 / SELF_FAIL 50 as baseline), 9 min 20 s, with OEFBridge.pm a1eaab8c… / oefRemote.pl 01ca21f8… | **PASS** | `P9_baseline_V1_1.log`, `P9_grade_baseline_P4_V1_1.csv`, `P9_V1_1_module_macro_sha256.txt` |
| P10 functional | 50 sequential cycles 50/50; view p50 251 / p95 300 ms; submit p50 258 / p95 312 ms; worker RSS 94 976→95 016 kB. **1,000-cycle load test: NOT RUN** (owner rule) | **PASS** | `P10_functional50.json` |
| P10b recycling | 23 calls, OEF_MAX_REQUESTS=5, 11 worker pids, 23×200, no client retry | **PASS** | `P10b_recycling.json` |
| P10c session cleanup | baseline 977 → 998 after P10 → **895** after 100 s (wimslogd of this WIMS, idle_time3=120) | **PASS** | `P10c_session_cleanup.json` |
| P11 WIMS hardening | 27/27; **passwd/ident now multipart POST** (no URL); WIMS-saved adm/raw sessions holding the password deleted (0 left) | **PASS** | `P11_wims_hardening.json` |
| P12 WIMS auth patch | 20/20: p4render refuses no/empty/wrong/truncated/upper-case/+1 token and GET, refuses emod adm/raw; p4_json gives only `{"error":"auth"}` outside marked sessions (lightpopup, direct, forged var); normal WIMS use OK (lightpopup, anonymous exercise, listmodules, home, wcalc); 127.0.0.1 only | **PASS** | `P12_wims_auth.json` |
| P13 drop-down | set P4menu (perden, outside the pilot): wrong/wrong 0, wrong menu/right period 0.5, right/right 1.0; independent oracle | **PASS** | `P13_menu.json` |
| P14 no secret in logs | 8 secrets × 157 log files + 1 884 WIMS session files: **0**; positive control finds the token in log/p4_auth | **PASS** | `P14_no_secret_in_logs.json` |
| I3 generator sibling | Windows generator (read-only copy, sha 068de768…) and Debian generator (bb9b20c3…): P4 lines added once per regeneration, twice in a row, `perl -c` OK | **PASS** (Linux; Windows **NOT RUN**) | `I3_generator_sibling_test.txt` |

## 3. Changes, item by item
**Images in hardcopy / P8 HOLD / item 2.** The worker opens WIMS sessions with `useropts=32`, so WIMS sends formulas
as MathML instead of GIFs. The worker converts the MathML to TeX (`ml_tex`): `\(...\)` in `statement_html`
(MathJax on the web page, same delimiters as PG) and in `statement_tex` (WeBWorK's LaTeX hardcopy uses it as is).
Scan `scan_V1_1/`: with MathML, **0 image in all 10 pilot items** → **pilot exercises removed: none**. The 3 items
that printed `[figure]` in V1 — **fnctlin2, ratapprox, approx** — now print their formulas and P8 passes, so they can
stay in printed sets. Real pictures (CORequa*, classer* in the scanned modules) are **not** embedded in the PDF: this
build prints `[figure]` for them; those exercises are not in the pilot and must stay out of printed sets.
Not done: embedding real figures in the PDF (needs image files in WeBWorK's hardcopy temp dir; not feasible in this pass).

**F1 (refined, item 1).** `OEF_MODULES` = exact allowlist in the worker environment (site config), **required**:
the worker refuses to start without it or with a malformed entry. Every request: module in the allowlist, format
`<Letter><1-2 digits>/<path>`, not absolute, no `..`, not empty, a real dir under `OEF_MODULE_ROOTS` with `def/`
and `src/`, not a symlink; exo `^[A-Za-z0-9_]+$` with `def/<exo>.def`. P1 cases above.

**Worker down / D2 (item 3).** Unlimited attempts (max_attempts −1) on OEF sets; message « Service de correction
indisponible / grading service unavailable. Réponse non corrigée, aucune note enregistrée / answer not graded, no
score recorded. » on view and submit. P5b. What the teacher sees: each outage submission is an attempt scored 0
(num_incorrect +1 each, one past_answer row each); the past-answers page shows the typed answer marked *incorrect*
(red) even when it was right, with no outage note; the status after the worker returns is the best score. No
webwork2 hook (cannot cancel an attempt without patching webwork2).

**Busy WIMS.** `harden_wims_p4_GROK_V1_1.py --dev-wims-conf` (Debian dev only): `threshold1=2000`,
`threshold2=4000`, `priority=127.0.0.1` (+ `idle_time3=120`, below). Keys and their effect (src/config.c,
`check_load()` in src/auth.c) are documented in INSTALL §1.6. **Windows: not applied, needs Arnaud's explicit OK.**

**Session cleanup.** Root cause of V1 growth: wimslogd keeps every session idle < `idle_time3` (900 s default) and
removes idle popup sessions only after it (the "50 s" popup rule comes after that check, src/Wimslogd/cleaning.c).
Dev: `idle_time3=120` in the dev wims.conf block, wimslogd of my WIMS restarted (pid checked by cwd). P10c PASS.

**WIMS auth patch.** `wims_patch/p4_wims_GROK_V1_1.diff` (cumulative, applied only in my copy): new adm/raw job
`p4render` checks the POST field `p4_auth` against `log/p4_auth/p4_secret.proc` (constant-time compare, hex only),
refuses adm/ and `..`, marks the session `wims_p4_auth=yes` in var.stat; `scripts/oef/p4_json.phtml` answers only in
marked sessions. Untrusted OEF modules cannot read wimshome files, hence the session binding. The secret comes from
`secrets/wims_p4_token` (gitignored, 600), copied by the hardening script; never in a URL or log (P14). The worker
sends it. lightpopup and algogebra_json unchanged; 127.0.0.1 binding kept. P12.

**Multipart POST detail (found this session).** WIMS answers a request whose QUERY_STRING equals the session's
previous one with the previous page (src/variables.c `check_session`, reload protection). Two `?form-data` POSTs in a
row made \steps step 2 return step 1 (P4/P7/P10 failed in run 1). Fix: unique suffix `?form-data<pid>_<n>_<rand>`
(WIMS's own forms do the same, exec.c). cmd=new stays a GET (no secret in it).

**Grades.** Grade of record = WeBWorK's per-blank average. WIMS's score formula differs: perden 1 of 2 right → WIMS
0.25, WeBWorK 0.5; prob2 → WIMS 0.44, WeBWorK 0.667. Multi-step: each step submission is a WeBWorK attempt; fixing
that needs a webwork2 patch → not done, unlimited attempts kept (documented INSTALL §4).

**Drop-down.** P13 with `wrappers/generated_menu/` (perden), set P4menu, outside the pilot; `create_p4pilot_course`
gained `--menu-wrappers`. Mojo::DOM mis-nests `<select>` → pre-parsed into placeholders.

**P10 1,000 cycles: NOT RUN** (owner rule); 50-cycle run kept.

**I1** oeflinf.fr shipped: `wims_modules/H3_analysis_oeflinf.fr_GROK_V1_1.tar.gz` (unchanged copy of the sanity-linux
module) + oefrational.fr. **I2** no xxd: `python3 -c "import secrets;print(secrets.token_hex(32))"`. **I3**
generator-integrated: NEW sibling `install/Apply_WebWork4Win_Conf_P4_GROK_V1_1.py` (runs the existing generator
unchanged; its writers also emit `push @{$pg{modules}}, ...` — append, never replace — and the pg_config.yml item);
tested on Linux against the Windows and Debian generators; my dev start script now uses it; optional
`conf-snippets/start_debian_mariadb_P4_GROK_V1_1.diff` (`P4_GENERATOR`, `P4_SITE_LIB`; dry-run OK on an identical
copy, not applied to LW's file); idempotent hook `install/p4_conf_hook_GROK_V1_1.py` + launcher
`start/start_webwork_p4_GROK_V1_1.sh` as alternative. **I4** launcher sets PERL5LIB (site lib) and OEF_BRIDGE_CONF.
**I5** run_all header documents P4_ROOT/P4_RESULTS/P4_LOGS/P4_SECRETS/OEF_PORT/MARIADB_DEFAULTS ([client] socket,
user, password). **I6** P0 text from the environment. **I7** Grade_Baseline stops unless WW_COURSES and WEBWORK_ROOT
are set, prints the paths used. **I8** documented: r19040 has only myself and wimsunice; all disabled.

**D4 security.** P11 sends passwd/ident by multipart POST (dev-cgi log shows only `POST ...?form-data…`). Dev p4grok
password rotated (new random 32 hex, `secrets/wims_p4grok_password`, 600, never printed); old value found in 39
WIMS session var files + the conn file → those sessions deleted, conn rewritten; `logs/wims.log` redacted
(68 lines, `passwd=<redacted>`), now `logs/wims_until_1212.log`. P14 (and P2) check all 8 secrets in all our logs: 0.
LW's logs not read.

## 4. Problems met this session
1. Run 1: P4/P7/P10 failed (\steps step 2) → WIMS reload protection, fixed with the unique POST suffix (§3).
2. Run 1: P8 failed (key words taken from TeX commands) → gate takes key words from text outside `\(...\)`.
3. Run 1: P10c failed (count rose for 480 s) → idle_time3 (§3).
4. Run 3: P2 CHECK → the gate now reads the real past-answers page; 1-character values matched page chrome →
   surface limited to the answer cells. Run 4 (final): all PASS.

## 5. Open points / residual risks
- `algogebra_json` (unchanged upstream hook) is unauthenticated; used only by the test oracle. Loopback bind is its
  only protection.
- cmd=new GET URLs (session id, exo, seed) appear in the dev-cgi access log; sessions still need the P4 marker.
- WIMS stores adm/raw request variables (passwd included) in session var files: delete those sessions after admin
  calls (P11 does) or avoid adm/raw passwd jobs in production.
- Real figures not embedded in hardcopy.
- Windows: generator sibling, worker service, wims.conf limits, wimslogd idle_time3 — not run on Windows.
