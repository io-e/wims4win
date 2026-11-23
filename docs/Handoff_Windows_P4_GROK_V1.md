# Handoff — P4 (WIMS OEF exercises graded inside WeBWorK) to Windows — GROK_V1

Audience: Grok, continuing on Arnaud's Windows PC. Written 2026-11-23 (Europe/Paris) by Grok Bot after build
GROK_V1_1 passed every gate on the Debian dev box. Code and evidence: `integrations/webwork-p4/`
(start with its `INSTALL.md` and `Rapport_P4_Build_GROK_V1_1.md`). Study: `docs/Etude_MariaDB_WeBWorK_OEF_GROK_V1_1.md`.

## 1. Current state
- **Design.** A WeBWorK `.pg` wrapper per OEF exercise calls macro `oefRemote.pl` → trusted module
  `WeBWorK::OEFBridge` (outside Safe, builtin HTTP client) → **OEF worker** (`worker/oef_eval_GROK_V1_1.pl`,
  Mojolicious, 127.0.0.1 only, token) → **WIMS** on 127.0.0.1 (authenticated adm/raw job `p4render` + hook
  `p4_json`, patch `wims/p4_wims_GROK_V1_1.diff`). WIMS grades; WeBWorK records the per-blank average.
  Expected answers never leave WIMS (gate P2). Formulas come back as TeX (MathJax on screen, LaTeX in hardcopy).
- **Tested on Debian only** (dev copy: WeBWorK 3020, MariaDB 3308, WIMS 8892, worker 3013). LW_WEBWORK ran the
  official V1 gates on the Debian reference instance (3000/3307); V1_1 fixes all items of its review.
- **Delivered config:** worker on **127.0.0.1:3003**, WeBWorK origin http://127.0.0.1:3000.
- **Nothing has been run on Windows.**

## 2. Gate table (Debian dev, final run `run_all_gates_GROK_V1_1.sh`, exit 0)
| Gate | Result |
|---|---|
| P0 ports / loopback | PASS |
| P1 worker security (35 cases incl. F1 allowlist, 5 start refusals) | PASS |
| P1b bridge HTTP client (builtin default) | PASS |
| P2 no leak (8 surfaces + secrets in logs) | PASS |
| P3 seed reproducibility | PASS |
| P4 grading 22/22 | PASS |
| P5 worker down / P5b no score > 0 during outage | PASS / PASS |
| P6 decimal comma | PASS |
| P7 `\steps` | PASS |
| P8 hardcopy (formulas as TeX, no `[figure]`) / P8b MathJax on the web page | PASS / PASS |
| P9 BPL regression, 530 rows, 0 differing | PASS |
| P10 50 sequential cycles / **1,000-cycle load test** | PASS / **NOT RUN (owner rule)** |
| P10b worker recycling / P10c WIMS session cleanup | PASS / PASS |
| P11 WIMS hardening / P12 WIMS auth patch | PASS / PASS |
| P13 OEF drop-down (outside the pilot) | PASS |
| P14 no secret in any log | PASS |
| I3 config generator sibling (Windows + Debian generators, on Linux) | PASS on Linux, **NOT RUN on Windows** |

## 3. Rules on Windows (from Arnaud; do not relax them)
- **PowerShell only** (`pwsh`); no `.bat`/`.cmd` files.
- **Laragon and the live WIMS runtime (`WIMS_RT`) stay untouched** without Arnaud's explicit OK. WIMS patches go on a
  **NEW sibling runtime**, never the live one.
- **Test credentials only from `C:\Users\Shadow\wims_test_creds.txt`.** Never print them, never commit them.
- **WIMS limits (`wims.conf`: threshold1/threshold2/priority/idle_time3) on Windows need Arnaud's OK.**
- **The 1,000-cycle P10 needs Arnaud's OK** (no load test otherwise).
- Secrets in gitignored chmod-600-equivalent files (ACL: owner only), never on a command line, never in a URL or log.
- New files carry `GROK_V<x>` in the name and a header marker; existing generator/start scripts are not edited
  (siblings instead).

## 4. Ordered checklist
1. **Prerequisite — MariaDB switch at 127.0.0.1:3307, done by Codex.** Do not start P4 on Windows before WeBWorK
   runs on MariaDB 3307 (as on Debian) and its own tests pass. Record the commit/report Codex leaves.
2. **Read** `integrations/webwork-p4/INSTALL.md` and the V1_1 report; `sha256sum`-check files if you copy the archive.
3. **Sibling WIMS runtime.** Make a NEW WIMS runtime next to the live one (copy or `tools/install-runtime-r19040.ps1`
   into a new folder), served on a new loopback port. Never patch the live runtime.
4. **WIMS patch** in the sibling runtime only: `patch -p1 < wims/p4_wims_GROK_V1_1.diff` (MSYS2 `patch`, or apply the
   7 files by hand) in `public_html`; install modules `wims_modules/*.tar.gz` (oefrational.fr, oeflinf.fr).
5. **Hardening + token** on the sibling: `python harden_wims_p4_GROK_V1_1.py <SIBLING_RT> <SECRETS>`
   (no `--dev-wims-conf` on Windows unless Arnaud says OK). Tokens: `python -c "import secrets;print(secrets.token_hex(32))"`.
   Check that `log\p4_auth\` is not readable over HTTP in the Windows/Apache setup.
6. **Session cleanup:** make sure `wimslogd` runs for the sibling runtime (Windows build: `tools/build-wimslogd-win32.sh`).
   Default idle_time3=900 s keeps worker sessions 15 min; lowering it is a wims.conf change → Arnaud's OK.
7. **Worker as a Windows service on 127.0.0.1:3003** (Strawberry Perl + Mojolicious, the same Perl as webwork2).
   Environment: `OEF_MODULES` (exact allowlist), `OEF_TOKEN_FILE`, `OEF_WIMS_TOKEN_FILE`, `OEF_LISTEN=127.0.0.1:3003`,
   `OEF_ALLOWED_ORIGIN=http://127.0.0.1:3000`, `OEF_WIMS_BASE=http://127.0.0.1:<sibling port>/wims/wims.cgi`,
   `OEF_MODULE_ROOTS=<sibling>\public_html\modules`. The Linux supervisor (`start_oef_worker_GROK_V1_1.sh`) has no
   Windows twin yet: write a PowerShell supervisor/service wrapper (new file) — Mojolicious prefork does not exist on
   Windows; use `daemon` mode and restart on exit. Re-check P10b semantics (recycling) there.
8. **Config generator:** copy `install/Apply_WebWork4Win_Conf_P4_GROK_V1_1.py` next to
   `webwork4win\Apply_WebWork4Win_Conf_GROK_V1.py` and make a NEW sibling of `Start_WebWork4Win_GROK_V1.ps1` that calls
   it (both calls, incl. `--links`), prepends the P4 site lib to `$env:PERL5LIB` and sets `$env:OEF_BRIDGE_CONF`.
   Then run the generator twice and check: one `push @{$pg{modules}}, [qw(WeBWorK::OEFBridge)];` line, one
   `pg_config.yml` item, `perl -c localOverrides.conf` OK (as `install/test_generator_sibling_GROK_V1_1.py` does).
9. **WeBWorK side:** `site-lib\WeBWorK\OEFBridge.pm`, `site-macros\oefRemote.pl`, `oef_bridge.conf` (url 3003,
   token file); course `p4pilot` via `install/create_p4pilot_course_GROK_V1.py` (+ `--menu-wrappers`).
10. **Gates on Windows**, with `P4_RESULTS` outside the repo and `OEF_PORT=3003`: P0, P1, P1b, P2, P3, P4 first.
    The gate scripts use Linux tools in places (`ss`, `ip`, `pdftotext`, bash supervisor commands): port those calls to
    PowerShell equivalents in NEW `_WIN` siblings (e.g. `Get-NetTCPConnection`), keep the assertions identical.
11. **Part C only after P0–P4 pass on Windows.** Then P5, P5b, P6, P7, P8/P8b, P9 (Windows BPL baseline), P10 (50),
    P10b, P10c, P11, P12, P13, P14. P10 1,000 cycles only with Arnaud's OK.
12. Write `Rapport_P4_Windows_GROK_V<x>.md` with each gate's command, output and result file; mark anything not run
    as NOT RUN.

## 5. Open decisions (for Arnaud)
1. Lower WIMS limits / idle_time3 on Windows (needed for busy use and for P10c in reasonable time)?
2. Run the 1,000-cycle P10 on Windows?
3. Real pictures in hardcopy: not embedded (print `[figure]`); keep exercises with data-bearing pictures out of
   printed sets, or fund an embedding step?
4. `algogebra_json` (unchanged upstream hook) is unauthenticated; loopback-only is its protection. Keep, or gate it
   like `p4_json`?
5. Outage attempts are recorded as incorrect attempts (no webwork2 hook): accept with unlimited attempts, or patch
   webwork2?
6. Multi-step OEF: each step submission is a WeBWorK attempt — accept with unlimited attempts?
7. Adopt the start-script hook diff (`conf-snippets/start_debian_mariadb_P4_GROK_V1_1.diff`) on Debian?
