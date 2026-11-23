# MariaDB, WeBWorK and OEF: decision, measurements and P4 design (GROK_V1_1)

Date: 2026-11-23 (Europe/Paris). Follows `Etude_MariaDB_WeBWorK_OEF_GROK_V1.md`; V1 is kept unchanged.
Everything was done on the box under `/workspace/mw-proto-webwork-20261123/`. Nothing was written to Drive, the PC or
GitHub. `/workspace/wims-port` stayed on its branch (`docs/setup-class-backup`); the WIMS copy is a detached git
worktree of origin/master `b21702a`. The LW_WEBWORK Debian instance (`/workspace/ww-debian`, 127.0.0.1:3000 / MariaDB
3307) was only read as files: no start, no stop, no DB query, and nothing was read from `ww-debian/secrets/`.

## 0. Decision and short results

**Decision (Arnaud, 2026-11-23): everything goes into WeBWorK (P4).** WeBWorK has the only gradebook (MariaDB). WIMS
does only two things: it renders an OEF exercise and grades an answer. The P3 option (Moodle with two gradebooks) is
dropped.

| Task | Result |
|---|---|
| A. Launcher | 3 GROK_V1_1 PS1 files fix L2–L7. L1 was a false alarm. All 3 parse with 0 errors (pwsh 7.4.6). Dry run on Linux only; **no end-to-end Windows run was possible** (the PC is offline). |
| B. Catalogue | SQLite FTS5 catalogue of 84,254 items: 10.6 s cold build, 1.1 s incremental, 52 MB. `/programming/search` answers in 1.2–13 ms median, against 346 ms warm (1.8 s cold) plus 130–260 ms JSON.parse for the 28.8 MB `/programming/list`. 8/8 tests pass. |
| C. adm/raw | New WIMS (master b21702a) built on the box. **32/32 probes pass**: class, user, sheet, exercise, scores, auth checks. **A single OEF can be rendered with a given seed (reproducible) and graded without any class or student session**, through `lightpopup` and `cmd=reply` with the AlgoGebra hook. That hook also returns the expected answer, so P4 must strip it. |
| D. P4 design | OEF service shaped like the Lumaplus PG worker (loopback, token, `render`/`check`, seed per request, no expected answer out). WeBWorK side: a **trusted PG module** listed in `pg_config.yml` `modules` (recommended over reusing AskSage), a wrapper `.pg` problem, and the score in `problem_user.status`. No new table. |

## 1. Corrections to V1

- **L1 was a false positive.** `/programming_3` is the static extensionless file `htdocs/programming_3`, served by the
  "SPA statique" block of `router.php`. It holds the `perl-preview` iframe (to 8117). V1_1 only checks that the
  file exists.
- **OEF seed per request exists.** `scripts/oef/var.proc` (lines 8–13) takes `seed=` from `module_init_parm`. C
  confirms it: the same seed gives the same statement in two separate sessions, and another seed gives a different
  one.
- **`lightpopup` renders without authentication** (it runs before the ident check in `adm/raw/var.proc` line 1).
- The default `.connections/myself` ships with password `toto` and `ident_type=wims` (see the security notes, 4.4).

## 2. A. Launcher (A_launcher/)

| File | Fixes |
|---|---|
| `Lancer_8097_8098_GROK_V1_1.ps1` | Requires its V1_1 siblings and never falls back to another version. `-Stop` runs every step in try/catch (`Invoke-StopStep`), reports each failure and exits 1 (L5). Stop order: native 3002, graph 8117 (L3), 8098, then Lumaplus with PG. Passes `-PgNoDebug`/`-PgDebug` through. Opens the URL through a switch. Headers cleaned (L7). |
| `Lancer_Lumaplus_GROK_V1_1.ps1` | Uses `router_lumaplus_GROK_V1_1.php` and fails clearly if it is missing. `-PgNoDebug` runs `--stop` first, then restarts the worker without `--debug` (L4). A foreign router on 8097 is reported, not reused. `$PgLog` path fixed (L6). |
| `Lancer_Programming_3_GROK_V1_1.ps1` | Never starts a Lumaplus launcher; it throws if 8097 is not listening (L2). Checks that `htdocs\programming_3` exists (L1). Writes the PID file `logs\programming3_8117.pid`. `-StopGraph` stops only `perl.exe` whose command line holds `Programming_3_Perl_CLAUDE`, found by PID file or port owner (L3). Adds `-StopNative`. |

Checks:

- The PowerShell parser reports 0 errors for all three files.
- Linux dry run of `-Stop`: all 4 steps ran even though each one failed (no `Get-CimInstance` on Linux), and the
  exit code was 1. The missing-sibling check stops the launch.
- Not done: a real run on Windows (ports, processes, browser). This is the first thing to run when the PC is back.

## 3. B. SQLite catalogue and search (B_catalog/)

### 3.1 Choice

- **SQLite FTS5**, one file built by a stdlib-only Python indexer (`build_catalog_GROK_V1.py`). The endpoint is read
  by PHP `pdo_sqlite` inside the existing `php -S` router (`programming_search_api_GROK_V1.php`).
- Why not MariaDB for the catalogue:
  - the catalogue is a read-only index of files, rebuilt from disk, so it needs no server, account or port;
  - it stays on the same origin and the same process as 8097, so there is no CORS and no extra process to supervise;
  - PHP opens the DB read-only;
  - MariaDB stays for the WeBWorK gradebook only.
- Later, the same SQLite can feed the OPL tag tables of WeBWorK's MariaDB. The Library Browser currently returns 500
  because OPL is not loaded (LW_WEBWORK note).

### 3.2 What it indexes

- **PG files:**
  - `DBsubject`, `DBchapter`, `DBsection`, `KEYWORDS`, `Level`, `Author`, `Institution`;
  - the title comes from `TITLE()` or the line after `## DESCRIPTION`;
  - `zzz-` placeholders are ignored.
- **OEF files:** `\title`, `\language`, `\author`, `\keywords`, plus the module `INDEX` (chapter, keywords, level,
  language).
- **JSON, XML and Python items:** their name fields.
- **Text handling:** HTML entities are decoded, with a cp1252 fallback.
- **Incremental runs:** files are compared by size and mtime, removed files are deleted, then FTS is rebuilt.
- **Flags:** `--extra`, `--map`, `--full`, `--optimize`.
- **Endpoint:** `GET /programming/search?q=&type=&folder=&page=&per_page=`.
  - `per_page` is at most 200, `q` at most 200 bytes, and at most 12 words, each turned into a quoted prefix term.
  - Results are ranked by bm25 and return `by_type` facets.
  - It returns 503 when `pdo_sqlite` is missing or the DB is not built.

### 3.3 Measurements (box, 84,254 items: OPL 40,541 + Contrib 29,624 + WIMS OEF 14,089)

| Metric | Value |
|---|---|
| Cold full build / incremental with no change | 10.6 s / 1.1 s, 0 errors |
| DB size | 52 MB |
| Extraction coverage | PG: 58,778 with subject, 49,705 with keywords. OEF: 14,059 with keywords, 14,054 with level |
| Current `/programming/list?folder=all` | 28.8 MB; cold 1.8–2.3 s; warm median 346 ms; client JSON.parse 128–264 ms; filtering 8–60 ms; matches name/path only |
| `/programming/search` HTTP median | "eigenvalue matrix" 2.5 ms, 64 hits (current list: 0 hits). "derivative chain rule" 7.8 ms, 561 hits. "fraction" 11.6 ms, 4,974 hits. With `type=oef`: about 10 ms. No `q`, page 100: 4.8 ms. Folder filter: 6.4 ms. Responses 2–26 KB |
| Tests | `tests/test_catalog_GROK_V1.py`: 8/8 OK in 0.33 s |

Finding: with 84k items, the current `/programming/list` and `/programming/folders` die at PHP's default 128M
`memory_limit` (they work at 256M). Search works at 128M.

### 3.4 Deploy on the PC (not done; PC offline)

1. Copy these files into `poc_obfuscation`:
   - `build_catalog_GROK_V1.py`;
   - `lumaplus/server/programming_search_api_GROK_V1.php`;
   - `router_lumaplus_GROK_V1_1.php`, which is the GROK_V1 router plus one `require`.
2. Enable `extension=pdo_sqlite` in the php.ini used by 8097.
3. Run `python build_catalog_GROK_V1.py` against the real `programming_folders_CLAUDE_V1_4.yaml` (`--map` if
   paths differ).
4. Start the platform with `Lancer_8097_8098_GROK_V1_1.ps1`.
5. Point the front-end search box at `/programming/search` instead of filtering `/programming/list` on the
   client.

## 4. C. WIMS adm/raw on a new copy (C_admraw/)

### 4.1 Setup

- **Source:** worktree `C_admraw/wims-master-b21702a` (detached origin/master).
- **Build:** `C_admraw/build/wims`, from the r19040 tree. It runs `./compile`, mkindex, theme assets and the
  AlgoGebra hook (`build_linux_wims_GROK_V1.sh`, `resume_build_GROK_V1.sh`).
- **Build dependencies installed on the box:**
  - libgd-dev, libgmp-dev, flex, bison, libfl-dev, g++;
  - python3-jinja2 and python3-cssmin (needed by chemeq).
- **Module:** `H3/analysis/oeflinf.fr` was copied in from the sanity-linux module tree.
- **Server:** `dev-cgi-server.py` on 127.0.0.1:8891.
- **Connection `.connections/p4grok`:**
  - `ident_site=127.0.0.1`, `ident_agent=P4-WeBWorK-GROK`, `ident_type=json`;
  - `ident_allow` lists 25 jobs;
  - the password is random and stored in `C_admraw/secrets/test_creds_GROK_V1.txt` (chmod 600, never printed).
- **Second connection `p4grokip`:** the same with `ident_site=10.9.9.9`, used for the IP test.
- **Probe:** `test_admraw_GROK_V1.py` (stdlib). Results are in `results/admraw_results_GROK_V1.json`, with
  passwords redacted.

### 4.2 Results: 32/32 pass

| Area | Jobs | Result |
|---|---|---|
| Auth | bad password, bad User-Agent, IP outside `ident_site`, job outside `ident_allow` | All refused with an explicit JSON error |
| No-auth jobs | `listmodules` (also `getmodule`, `getinfoserver`, `getserverstat`, `getmodulesstat` per `defs`) | Answer without ident |
| Class / user | `addclass` (917 ms; creates class + supervisor), `getclass`, `adduser`, `getuser`, `authuser` | OK. `authuser` hands out a student session **without the student password** |
| Sheet / exercise | `addsheet`, `putexo` (module exercise into sheet), `modsheet` (`status=1`), `getsheet`, `listsheets`, `getexo` (needs `qsheet`+`qexo`=index), `addexo` (class OEF source, compiled), `listexos`, `testexo` (sandbox compile) | OK |
| Scores | `getscore`, `getscores`, `getsheetscores` (the sheet must be active) | OK (empty, nobody worked) |
| Render with seed | `job=lightpopup&emod=H3/analysis/oeflinf.fr&parm=cmd=new;exo=antecedant;qnum=1;seed=4242` | 7–9 ms. Seed 4242 twice gives the same statement ("coefficient -33 … image -330"), seed 777 a different one. **No ident, no class.** The page HTML does not contain the answer |
| Grade without a student session | the anonymous session from lightpopup + `cmd=reply&reply1=…&special_parm=algogebra_json` | Wrong answer: `{"grade":0,"seed":4242,…}`. Same seed with the right value: `grade:1`. About 10 ms. **The hook JSON contains `"expected":"10"`** |

Times are on the box with the dev CGI server; Apache on Windows will differ.

### 4.3 What P4 still lacks on the WIMS side

1. **A clean render+grade API.** lightpopup returns a full WIMS HTML page tied to an anonymous session. A P4
   service has to:
   - extract the statement and the reply fields;
   - keep the session id on the server side;
   - post `cmd=reply` back into that session.

   Grading needs the same session as the render, because WIMS keeps the drawn variables in it. A stateless "check
   seed+answer" request does not exist. The service can emulate it by doing `cmd=new;seed=S` and then `cmd=reply`
   (two calls, about 20 ms).
2. **No expected-answer stripping.** The AlgoGebra hook (`oef/algogebra_json.phtml`) always emits `expected`. P4
   needs one of two fixes:
   - a variant hook that omits `expected` (for example `special_parm=p4_json`);
   - or the service drops the field and the WIMS port is never reachable by browsers.

   Both fixes are needed for defence in depth.
3. **Class exercises (`addexo`) are not reachable through lightpopup**, which takes a module path. P4 should grade
   module exercises (public modules or a dedicated local module such as `local/p4/...`), not class exercises.
4. **Multi-step exercises, `\steps` and feedback:** the hook only lists the steps that were reached. The wrapper must
   loop until WIMS reports completion. This is not tested yet.
5. **Not tested yet:**
   - decimal-comma answers on the WIMS side;
   - images and plots (the image URL points to the WIMS session);
   - session lifetime and cleaning under load.

### 4.4 Security notes

- **The `ident_site` IP allowlist is the main barrier.** It matches `REMOTE_ADDR` exactly, with no wildcard.
  - Use 127.0.0.1 only, and keep WIMS (Apache) bound to loopback.
  - Behind a reverse proxy every caller looks like 127.0.0.1, so the proxy must not forward `/wims/` to browsers in
    P4.
- **The `ident_agent` check is cosmetic**, because the User-Agent is client-chosen.
- **The password** is alphanumeric, sent in the query string (it ends up in Apache access logs), and compared in
  clear text.
  - Use a long random value (32 hex), as done here.
  - Use `ident_protocol=https` if WIMS ever leaves loopback.
- **Use `ident_allow` (allowlist), not `ident_deny`.**
  - The default `myself` uses `ident_deny=deluser delclass`, so everything else is allowed, including
    `authuser` (sessions without a password) and `getcsv` with uncrypted passwords.
  - P4 only needs `gettime` plus, optionally, `listmodules`/`getmodule`. Render and grade go through lightpopup,
    which needs no ident.
- **Default `myself` / `toto`:** change the password or disable it (`ident_password=*`) on any WIMS used by P4.
- **lightpopup and the AlgoGebra hook need no ident.** Anyone who can reach WIMS can render any module exercise and,
  through the hook, read the expected answer. So in P4, WIMS must be reachable **only** from the OEF service on
  loopback, or the hook must be switched to the stripped variant.
- The robot check: `adm/raw` refuses sessions marked robot.

## 5. D. P4 design: WIMS as an OEF render+grade engine behind WeBWorK

### 5.1 Components

```
Browser ──> WeBWorK 2.21 (Morbo 127.0.0.1:3000, MariaDB 3307: the only gradebook)
              │  wrapper problem  OEF/<module>/<exo>.pg  (PG, runs in Safe)
              │  macro  oefRemote.pl  ──calls──>  trusted module WeBWorK::OEFBridge (outside Safe)
              │                                        │ HTTP, loopback, token
              ▼                                        ▼
         problem_user.status (0..1)          OEF worker 127.0.0.1:3003  (shaped like the Lumaplus PG worker 3001)
                                                       │ lightpopup cmd=new;seed=S / cmd=reply + p4 hook
                                                       ▼
                                             WIMS (Apache/dev CGI, loopback only)
```

### 5.2 OEF worker (new, shaped like `pg_eval_CLAUDE_V1_3.pl`)

- **Same conventions as the Lumaplus PG worker:**
  - binds 127.0.0.1;
  - bearer token from a git-ignored file;
  - JSON in and out;
  - `GET /oef/ping`;
  - paths resolved inside an allowlist of module roots (no `..`, no absolute paths);
  - debug mode is off by default.
- **`POST /oef/render {module, exo, seed, lang}`:**
  - calls `lightpopup … cmd=new;exo=…;seed=S`;
  - returns `{statement_html, fields:[{id,type,choices?}], seed, render_id}`;
  - keeps the WIMS session server-side, keyed by `render_id` (short TTL);
  - rewrites image URLs to a proxied, token-free path;
  - never returns the WIMS session id.
- **`POST /oef/check {module, exo, seed, answers:{reply1:…}}`:**
  - re-creates the exercise with `cmd=new;seed=S` (stateless, so the result does not depend on `render_id`), then
    posts `cmd=reply&special_parm=p4_json`;
  - returns `{score:0..1, per_field:[{id,correct,score,feedback}], seed}`;
  - **never** returns `expected`. It is dropped by the stripped hook and again by the worker (allowlist of keys).
- **`seed`** comes from WeBWorK's `problemSeed`, so each student gets a reproducible draw, and a re-randomisation
  in WeBWorK changes the OEF draw.
- **Rate limiting:** limit per token, and cap the answer size.

### 5.3 WeBWorK side: trusted module (recommended) versus AskSage reuse

| | Trusted module in `pg_config.yml` `modules` | Reuse `AskSage` (`PGcore.pm:705` → `IO.pm:321` `query_sage_server`) |
|---|---|---|
| PG source edits | None. The module is a new `.pm` in a site lib dir, listed in `modules` like `[WeBWorK::OEFBridge]`, and loaded outside Safe by PG's documented mechanism | None |
| Wire format | Our JSON (`render`/`check`) | Fixed Sage Cell format: it POSTs `accepted_tos`, `user_expressions={"WEBWORK":…}` and `code=<setSeed><webworkfunc><python>`, then parses Sage's reply. The OEF worker would have to pretend to be a Sage Cell server and carry OEF requests inside a "python" string |
| Transport | Perl `HTTP::Tiny` or `Mojo::UserAgent`, fixed to the loopback URL and token from site config | `curl` subprocess per call (`-k`, so TLS is not verified), URL validated by regex only; on Windows `curl.exe` adds a process spawn per answer under Strawberry's pseudofork |
| Security | Token and URL are site config, not visible to problem authors; the module exposes only `oef_render`/`oef_check`; the answer is never in PG memory | The URL is `$args->{url}` from the problem, else site `URLs.sagecellService`, else the public sagecell.sagemath.org (`IO.pm:427`): a problem author can point the server at any URL, and a missing site setting sends the code to a public server; no auth header |
| Robustness | Explicit timeouts, explicit errors | Already fragile: the 2 BPL AskSage problems fail (LW_WEBWORK; HOLD in the spec) |
| Cost | About 150 lines of Perl plus a 20-line macro | Less WeBWorK code, more hacks in the worker |

**Recommendation: the trusted module.** AskSage is only an option for a one-day demo. Its format, its `-k` curl and
its author-chosen URL make it a poor base for grades. RserveClient is the upstream precedent for a module that calls
a network service from PG; the WeBWorK wiki warns that such modules weaken Safe. So keep the module surface to two
functions with fixed endpoints.

Checks before writing it:

1. That `modules` entries in their `pg_config.yml` are loaded on 2.21 at 6a4fd3a. The site already edits this file
   for the French macros dir.
2. That a module function is callable from a macro. Other `modules` such as `Fraction` and `Distributions` are, but
   ours must be tested under Morbo and on Windows (`Subprocess.pm` runs `run_p` in-process).

### 5.4 Wrapper problem and macro

- **One `.pg` per OEF exercise**, generated from the SQLite catalogue (the type=oef rows):
  - `loadMacros("PGstandard.pl","oefRemote.pl")`;
  - `$oef = OEF("H3/analysis/oeflinf.fr","antecedant")`;
  - `BEGIN_PGML [@ $oef->statement @]* END_PGML`;
  - `ANS($oef->grader)`.
- **`grader`** is a custom answer evaluator (an `AnswerEvaluator` from PG's `modules` list). It sends all blanks in
  one `check` call and returns the worker's score. With several blanks, grade per blank, or use a single
  aggregated blank with per-field feedback; to decide.
- **The statement HTML is shown as-is.** It is trusted, because it comes from our own WIMS. Inputs keep WeBWorK
  names (`AnSwEr0001` …) and are mapped to `reply1` … in the macro.
- **"Show correct answers" in WeBWorK cannot work**, because the expected answer is never sent. This is a deliberate
  P4 limitation; to be accepted by Arnaud. A teacher-only path could ask the worker with a separate teacher token.
- **Hardcopy (PDF):** show the statement text and skip the inputs; to be tested.

### 5.5 Scores, gradebook and checks

- The score from the evaluator ends up in `problem_user.status` (0..1) through WeBWorK's normal path: attempts,
  `num_correct`, and `last_answer`. The submitted answers are stored by WeBWorK, never by WIMS.
- **No new table, no WIMS class and no WIMS gradebook.** adm/raw class, sheet and score jobs are not used in P4.
- **Decimal comma:** WeBWorK's site `parserCustomization.pl` does not apply to OEF blanks, which WIMS parses. The
  worker passes the raw string; test whether OEF numeric accepts `0,39` and, if not, normalise in the worker only
  for `type=numeric` blanks.
- **Non-regression gate:** OEF wrapper problems join `Grade_Baseline_GROK_V1.py --compare`, which uses the
  multi-seed baseline (seeds 1234 1 42 2026 31415).
  - "Correct" must score 100% and "perturbed" must score below 100%.
  - The script reads `correct_ans` from the PG worker. OEF wrappers have none, so they need a teacher-token
    `expected` call (test-only), or a fixed answer file per seed.
- **PG version:** the Lumaplus PG worker `pg_eval_CLAUDE_V1_3.pl` uses the standalone PG 2.21 API
  (`WeBWorK::PG->new(...)`), the same major version as the Debian instance (pg 6a4fd3a0, webwork2 47b53d48). The
  worker is not pinned to a commit, so check the PG checkout used on the PC against 6a4fd3a.
- **LTI does not apply:** WeBWorK is an LTI tool, not a platform, so it cannot launch WIMS as an LTI tool.
- **Later: OPL tags.** Load the PG rows of the SQLite catalogue (DBsubject, DBchapter, DBsection, keywords) into
  WeBWorK's MariaDB OPL tables, so the Library Browser stops returning 500. OEF wrappers could get their own
  "OEF" subject the same way.

## 6. Open blockers and next decisions

**Blockers:**

1. The PC is offline. Still to do there:
   - a real Windows run of the A launchers;
   - the B deploy (pdo_sqlite, real YAML);
   - a WIMS adm/raw check on the Windows WIMS (Apache, `.connections`).
2. Windows WeBWorK still uses port 3307 for MariaDB; Codex is migrating it (LW_WEBWORK). The OEF worker must not
   collide with it; 3003 is proposed.
3. The stripped hook (`p4_json`) and the OEF worker are not written yet. The trusted module is not written or
   tested on 2.21 or under Morbo.

**Decisions for Arnaud:**

1. Accept "no Show correct answers" for OEF in WeBWorK (or a teacher-only token path).
2. Trusted module (recommended) or an AskSage demo first.
3. Grading granularity for exercises with several blanks: per blank or aggregated.
4. Source of OEF exercises: public modules only, or a local `local/p4` module for our own OEF.
5. Whether to bind WIMS to loopback only on the P4 machine, and disable `myself`/`toto`.

## 7. Files (box)

| Path | What |
|---|---|
| `A_launcher/Lancer_8097_8098_GROK_V1_1.ps1`, `Lancer_Lumaplus_GROK_V1_1.ps1`, `Lancer_Programming_3_GROK_V1_1.ps1` | Launchers |
| `B_catalog/build_catalog_GROK_V1.py`, `bench_search_GROK_V1.py`, `tests/test_catalog_GROK_V1.py` | Indexer, benchmark, tests |
| `B_catalog/poc_mock/router_lumaplus_GROK_V1_1.php`, `poc_mock/lumaplus/server/programming_search_api_GROK_V1.php` | Router and search endpoint |
| `B_catalog/build_full.json`, `build_incr.json`, `bench_results.json` | Measurements |
| `C_admraw/build_linux_wims_GROK_V1.sh`, `resume_build_GROK_V1.sh`, `test_admraw_GROK_V1.py`, `results/admraw_results_GROK_V1.json` | WIMS build, probe, results |
| `C_admraw/secrets/test_creds_GROK_V1.txt` | Generated test credentials (chmod 600) |
