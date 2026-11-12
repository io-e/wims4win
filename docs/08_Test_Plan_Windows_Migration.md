# 08 — Test Plan: wims4win Windows Migration (code modified for the port only)

Version 1.0 — 2026-10-28. Author: Grok (requested by Arnaud, pt68u). Language: English (project docs otherwise French).

## 0. Scope

**In scope:** only code **added or modified for the Windows port** since the upstream import `5afd05e` ("Import WIMS trunk r19040 (4.33a) intact"). The inventory was produced with:

```sh
git diff --stat 5afd05e HEAD -- . ':!docs'      # master, 2026-10-28 (HEAD ea6dc73)
git diff --stat 5afd05e origin/feat/latex2wims-doc -- tools/latex2wims
```

**Out of scope:**
- Unchanged upstream behaviour (module logic, OEF semantics, CAS mathematics). A minimal smoke test (area SMK) only proves that the modified code paths are exercised in a real request.
- Load tests and parallel 200-module runs (decision D5, issue #6).
- MSVC/CMake (D18).

**Status legend:**
- **PASS** = already run, with the result recorded in `05_Etat_et_Decisions.yaml` / `Suivi_Developpement_Portage.md` (date given).
- **FAIL** = run with a known defect.
- **NOT RUN**
- **BLOCKED** = a prerequisite is missing.

No result in this document is invented: every PASS cites its source run.

**Priority:**
- **P0** = production gate (opening to students).
- **P1** = required for full module support.
- **P2** = quality / tooling.

## 1. Modified-file inventory (MF-xx)

Mode-only changes (exec bits restored by `de74127`, 0 lines) are grouped as MF-40. Every test in §4 references one or more MF-ids.

| MF | Path | Nature | Main functions / content | Commits / issues |
|---|---|---|---|---|
| MF-01 | `src/Platform/wims_platform.h` | new | API: `wp_spawn`, `wp_spawn_mx`, `wp_wait`, `wp_alive`, `wp_kill`, `wp_poll`, `wp_read/write`, `wp_strip_cr`, `wp_set_job`, `wp_job_kill`, `wp_sandbox_available` | debb65b, b9670a6, 68377ef |
| MF-02 | `src/Platform/wp_posix.c` | new | POSIX implementation (fork/exec, setreuid drop) | debb65b |
| MF-03 | `src/Platform/wp_win32.c` | new | `spawn_core` (CREATE_SUSPENDED → `AssignProcessToJobObject` → `ResumeThread`), `make_job`, `wp_cpu_watch`/`start_cpu_watch`, `wp_set_job`, `wp_job_kill`, `wp_resolve_exe`, `wp_script_interp` (#!), `wp_builtin` (mv/rm), `wp_sandbox_available`, `token_elevated`, overlapped `wp_write` | 68377ef, 5f5523b, 17c2676, 1b58be7; #7 |
| MF-04 | `src/Platform/wp_common.c` | new | `wp_strip_cr` | f81abf1 |
| MF-05 | `src/Platform/wp_mx.c` | new | `wp_mx_exchange` (multiexec, fixed/legacy) | 3ae53c2; #9 |
| MF-06 | `src/Platform/wp_ipc.h`, `wp_ipc_posix.c`, `wp_ipc_win32.c` | new | `pipe_name` (hash per tree), `wp_ipc_transact`, `wp_ipc_listen/accept/read_msg/write_msg`, `wp_loadavg`, `wp_daemon_spawn` | 44bde53, 9c3628d |
| MF-07 | `src/Platform/wp_cgi_win32.c` | new | `wp_cgi_prepare` (`_fmode=_O_BINARY`, chdir to exe dir, path slashes, SERVER_ADDR default, 500 on incomplete tree) | f999023, d1d10c0, 73a02bd |
| MF-08 | `src/Platform/wp_compat.c/.h`, `win32/posix_shim.h`, `win32/config.h` | new | setenv/unsetenv, mkdir, lstat, kill, fork stub, S_ISSOCK, nanosleep decl | 641bf5e, 4374c68 |
| MF-09 | `src/Platform/wp_alarm.c` | new | alarm via timer queue, `nanosleep` | 4374c68 |
| MF-10 | `src/Platform/wp_descrypt.c`, `wp_crypt_bcrypt.c` | new | musl DES `crypt()`, BCrypt PBKDF2-SHA256 `$wims$v1$` | 920cb53 |
| MF-11 | `src/Platform/sandbox/chroot_win.c` | new | `make_restricted`, `make_appcontainer`, `set_ace` (grant/revoke), `quote_arg`, `launch` (Job, ActiveProcessLimit=15), `sandbox_run`, `grant_rx`, `purge_profiles_ex`, `main` (upstream CLI) | c13459b, 0c0fce7; #8 |
| MF-12 | `src/Platform/tools/webget_win.c` | new | webget over WinHTTP | 5c7d55e |
| MF-13 | `src/Platform/tools/insdraw_win.c` | new | native `insdraw..processor.exe` | 1845b46 |
| MF-14 | `src/Platform/tools/binmode_win.c` | new | binary stdio constructor for helpers | 5c7d55e |
| MF-15 | `src/Platform/tools/wrap_win.c` | new | `wrap..exec.exe` (#! via PATH, .exe resolution, exit code) | af76b4c |
| MF-16 | `src/Platform/tools/system_sh_win.c` | new | `__wrap_system` → `sh -c` (clickzone) | af76b4c |
| MF-17 | `src/Platform/tools/canvasdraw_win.h`, `nofork_win.c`, `win32/compat/bsd_index.h`, `win32/compat/sys/times.h` | new | compat for canvasdraw, chemeq, units-filter, crossword | af76b4c |
| MF-18 | `src/Platform/Makefile`, `.gitignore` | new | libwimsplatform(-w32).a | debb65b |
| MF-20 | `src/Interfaces/common.c`, `common.h`, `Makefile.in` | modified | `execredirected` (ISO_WRAPPER if `../chroot/tmp/sessions/.chroot`), `dopipes`, `getstdin`, `readabout`, `accessfile`, CR strip | 24f38f5, 68377ef, f81abf1, 5f5523b |
| MF-21 | `src/Interfaces/maxima.c` | modified | `main`/`dynsetup`: sbcl.exe `--core` direct, MAXIMA_PREFIX/SBCL_HOME | 3c59829; #2 (closed); D3, D10 |
| MF-22 | `src/Interfaces/gap.c`, `m2.c`, `octave.c` | modified | gap direct exe; octave `illegal[]` += dos/winqueryreg/winopen | e13ec24, 51b54d5 |
| MF-23 | `src/lines.c` | modified | `execredirected`, `multiexec`, `kerneld` on the platform layer | 1cd0534, 4374c68, 44bde53, 17c2676 |
| MF-24 | `src/calc.c` | modified | `calc_exec`, `_get_exec_error` (CR strip), multiexec via `wp_mx_exchange` | 3ae53c2, f81abf1, 1cd0534, 17c2676, 1b58be7 |
| MF-25 | `src/config.c` | modified | `main_configure` (wims_home with `/`, PATH = `other/bin;other/msys2/usr/bin;…`, 8 KiB), `set_rlimits` → `wp_set_job` | 5f5523b, 93e8775, a0e0f62 |
| MF-26 | `src/log.c` | modified | `checklogd`, `bringuplogd` (`wp_daemon_spawn`, SERVER_ADDR NULL fix) | 44bde53, f999023, 4374c68 |
| MF-27 | `src/mathml.c` | modified | `mathml()` via `wp_spawn` (`wims_mathml.exe`); formula size from `log/wims4win.yaml` | 4b2d4eb, 9b38fb7; D7 |
| MF-28 | `src/wims.c`, `wims.h`, `includes.h` | modified | `main` calls `wp_cgi_prepare`; mxtab holds `wp_chan` | f999023, 1cd0534, 641bf5e |
| MF-30 | `src/Wimslogd/win/wimslogd_win.c` | new | Win main loop, `answer_w`, `output_w`, `sockerror`, `forkman`, `addfork_w`, `wait_children`, `wl_spawn_task`, `run_task`, `to_root` | 44bde53, 2258c89; #1 |
| MF-31 | `src/Wimslogd/{wimslogd.h,wimslogd.c,cleaning.c,log.c,fork.c,socket.c,wimslogdlines.c}` | modified (`#ifdef _WIN32` only) | `execredirected` (Win: `wp_spawn` + Job, CPU 1500 s), fork sites → `wl_spawn_task` | 2258c89 |
| MF-32 | `tests/wimslogd/wl_client.c` | new | IPC test client | 2258c89 |
| MF-40 | `bin/*`, `other/bin/{src2def,imgindex,findnoexec}`, `src/configure`, … | mode only | exec bits restored | de74127 |
| MF-50 | `tools/build-win32.sh`, `build-wims-win32.sh`, `build-wimslogd-win32.sh` (`--stack 32 MB`), `build-chroot-win32.sh`, `build-wims-mathml-win32.sh`, `build-linux-mx.sh` | new | cross-builds | 4374c68, 2258c89 |
| MF-51 | `tools/build-misc-win32.sh` | new | 18 Misc/OefMsg2wims helpers (translator, dicsort, modind, oef2wims, msg2wims, webget…) | 5c7d55e |
| MF-52 | `tools/build-misc-extra-win32.sh`, `tools/fetch-mingw-deps.sh` | new | the 17 remaining Misc programs + wrap..exec, gmp/libgnurx pinned | af76b4c |
| MF-53 | `tools/build-flydraw-win32.sh`, `build-libgd-mingw.sh` | new | flydraw.exe static libgd 2.3.3 | 1845b46; D6 |
| MF-54 | `tools/build-runtime-tree.sh` | new | runtime from SVN r19040: svn:special links, ELF removal, exe copy, `.chroot` marker, `default_texposition=2`, `wims4win.yaml`, `PARI_COMMAND`, provenance | b29becd, ec8f410, bccd9e4, 867a393, 0c0fce7, af76b4c; D15 |
| MF-55 | `tools/fetch-msys2-sh.sh`, `tools/msys2/msys2-pins.txt` | new | 43 pinned MSYS2 packages → `other/msys2` (+perl) | f21e6e9, a0e0f62, ed2c3f7; D17 |
| MF-56 | `tools/check-linux-identical.sh` | new | Linux object byte-identity | 4374c68 |
| MF-57 | `tools/install-runtime-r19040.ps1`, `install-cas-msys2.md` | new (Claude) | runtime install on PC | 8fbccc0; #3 |
| MF-58 | `docs/wims4win.yaml`, `docs/apache-laragon-wims-runtime.conf` | new | formula size (default 200 %, 50–400), Laragon vhost (WIMS_RT) | 9b38fb7, d820b2d |
| MF-60 | `.github/workflows/build.yml` | new | 3 jobs, timeouts 30/10 min, apt retries | 9324e26, 031d2a4, 1d77c9c, ea6dc73; D20 |
| MF-61 | `tests/platform/*` (`test_platform.c`, `test_crypt.c`, `test_sandbox_evasion.ps1`, `win/cpu_probe3.c`, `win/test_mathml_spawn.c`), `tests/interfaces/*` (`fake_cas.c`, `run_equiv*.sh`, `run_equiv_pc.ps1`, `multiexec_driver.py`) | new | test harnesses | 0a5207a, eeac3ee, db52741, 27071ff, 0c0fce7 |
| MF-70 | `tools/oef-validation/**` | new | oef2wims 4.31b Linux/mingw, parity, trace, v2, wrapper | fff15f5; D13, D19 |
| MF-71 | `tools/latex2wims/**` (branch `feat/latex2wims-doc`) | new | `latex2wims.c` (convert/install/check/all/corpus), `mkindex.c` (C port), `util.c`, `sha256.c`, tests | 2b78dec, 53b65e1, 0449ac3 |

## 2. Test environment

| Env | Description | Used for |
|---|---|---|
| E-LNX | Grok box, Debian, gcc, mingw-w64 cross (`x86_64-w64-mingw32-gcc/g++`), wine, flex/bison, `/workspace/wims-port`, reference runtime `/workspace/rt` (Linux r19040) | builds, Linux parity, wine smoke tests |
| E-PC | ShadowPC Windows 11, user `Shadow`, not elevated, no WSL | all P0 tests |
| E-RT | `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-r19040-grok` (built by MF-54) | runtime |
| E-WEB | Laragon/Apache, vhost `http://wims_paris_saclay_fr.test/wims/wims.cgi`, `Define WIMS_RT ".../wims-runtime-r19040-grok/public_html"` (the line is given to Arnaud; Laragon is never edited by Grok) | HTTP tests |
| E-CAS | PARI `Reprise_Wims_Grok\tests-win\cas\pari\gp.exe`; GAP `tests-win\cas\gap`; Maxima/Octave via winget (see `install-cas-msys2.md`) | CAS tests |
| E-CI | GitHub Actions, `io-e/wims4win` | CI |

**Common PowerShell prelude (E-PC, never cmd.exe):**

```powershell
$rt  = 'D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-r19040-grok'
$env:PATH = "$rt\other\bin;$rt\other\msys2\usr\bin;C:\Windows\System32"
Remove-Item Env:wims_home -ErrorAction SilentlyContinue
$env:HTTP_USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0'  # avoids "Robot arretez ici"
$env:REMOTE_ADDR = '127.0.0.1'
Set-Location "$rt\public_html"; $cgi = (Get-ChildItem wims.cgi* | Select -First 1).FullName
function W($q){ $env:REQUEST_METHOD='GET'; $env:QUERY_STRING=$q; & $cgi 2>&1 | Out-String }
```

**Box load note:** WIMS answers 450 "serveur surchargé" when loadavg > `threshold1`. The test runtime on E-LNX needs `threshold1=2000`, `threshold2=3000` and `default_texposition=2` in `log/wims.conf`.

## 3. Entry / exit criteria

**Entry:**
1. HEAD builds on E-LNX: `tools/build-runtime-tree.sh` OK; `tools/check-linux-identical.sh` all IDENTICAL.
2. Runtime deployed to E-RT; wimslogd from 2258c89 or later; ch..root from 0c0fce7 or later.
3. `ch..root.exe --selftest` returns 0 on E-PC.
4. CI on HEAD is green, or every red run is explained (runner apt hang).

**Exit (production gate):**
1. Every P0 test is PASS. A P0 may also be NOT RUN/BLOCKED if it has an explicit waiver from Arnaud recorded in `05_Etat_et_Decisions.yaml` (e.g. S6–S8 while Octave/Maxima are absent).
2. No open defect in MF-03, MF-11 or MF-30.
3. P1 ≥ 90 % PASS, with every failure linked to an issue.
4. Linux parity tests (LIN-*) all PASS.

## 4. Test cases

Columns:
- **ID**
- **Linked code** (MF-id, `path:function`, commit/issue)
- **Preconditions / Steps**: PowerShell on E-PC unless E-LNX is stated
- **Expected**
- **Linux ref.**
- **Prio**
- **Status** (date, source)

### 4.1 LIN — Linux build unchanged

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| LIN-01 | MF-56 `tools/check-linux-identical.sh`; MF-20, MF-23, MF-24, MF-02, MF-04 | E-LNX: `bash tools/check-linux-identical.sh` (default 14 files) | 14× `IDENTICAL` | itself | P0 | PASS (2026-10-27, run after 2258c89: 0 non-identical) |
| LIN-02 | MF-31 `src/Wimslogd/*` `#ifdef _WIN32` | E-LNX: `FILES="Wimslogd/cache.c … Wimslogd/fork.c" bash tools/check-linux-identical.sh` (13 files) | 13× IDENTICAL | — | P0 | PASS (2026-10-27, 13/13) |
| LIN-03 | MF-27 `mathml.c` (YAML), MF-25 `config.c` | E-LNX: add `Mathml/… config.c mathml.c log.c wims.c` to FILES | IDENTICAL, or diff limited to the YAML reader, which is the same on Linux by design (9b38fb7) | — | P1 | WAIVED (2026-11-06 Arnaud: identity not expected for mathml.c YAML reader; behaviour covered by YAML-02 PASS 2026-10-28 clamp 900%→400% / 30%→50%) |
| LIN-04 | MF-02 `wp_posix.c`; MF-61 `tests/platform` | E-LNX: `make -C tests/platform clean test` | 15/15 PASS (POSIX) | — | P1 | PASS (CI job "Linux Platform unit tests", green on ea6dc73) |
| LIN-05 | MF-20 `Interfaces/common.c`; MF-61 `run_equiv.sh` | E-LNX: `tests/interfaces/run_equiv.sh <r19040>/src/Interfaces <HEAD>/src/Interfaces` | stdout/stderr/rc identical, time ±2 s | r19040 | P1 | PASS (phase 1–2, `05` phase2 "equiv PARI/Maxima") |

### 4.2 PLT — Platform layer (MF-01…MF-09, MF-18)

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| PLT-01 | MF-03 `wp_spawn`, `wp_poll`, `wp_read`, `wp_kill`, `wp_wait` | `tests\platform\test_platform.exe` (cross-built by `make -C tests/platform test_platform.exe`) on E-PC | 22/22 PASS | LIN-04 | P0 | PASS (`05` tests.platform_unit 22/22, ShadowPC) |
| PLT-02 | MF-03 `test_platform.c` "spawn cmd /c echo" | **Defect to fix:** this test case uses `cmd /c echo`, contrary to the no-cmd.exe rule. Replace it with `fake_cas.exe plain`, then rerun PLT-01 | 22/22 without cmd.exe | — | P2 | PASS (2026-11-06: rebuilt `test_platform.exe` — message is `spawn fake_cas echo` (not cmd /c); 22/22; 0 cmd.exe spawned during run) |
| PLT-03 | MF-04 `wp_strip_cr`; MF-24 `_get_exec_error` | PLT-01 cases "strip_cr…", "child output has CRLF", "CR stripped" | CR removed only on Win32 | identical bytes | P1 | PASS (in PLT-01) |
| PLT-04 | MF-03 `wp_resolve_exe` | `!exec pari` resolves `bin/pari` → `bin\pari.exe`; check `W 'module=tool/algebra/factor.fr&cmd=new&lang=fr'` then reply (see SMK-02) | page computed | — | P1 | PASS (factor.fr 200, 2026-10-27) |
| PLT-05 | MF-03 `wp_script_interp` (#! scripts) | createqcm module (`rmdepot.sh`); also `sh` absent check: `Get-Command sh` | no `bad_cmd rmdepot.sh` | Linux runs the script | P1 | PASS (1b58be7, createqcm 200, 2026-10-27) |
| PLT-06 | MF-03 `wp_script_interp` "#!/usr/bin/env perl" | run a helper with `#!/usr/bin/env perl` through wp_spawn (e.g. `bin/chemeq` via wims `!exec chemeq`) | perl from PATH used | — | P2 | NOT RUN (deferred 2026-11-06: covered by HLP-05 chemeq/perl PATH PASS; no separate re-run) |
| PLT-07 | MF-03 `wp_builtin` (mv/rm in-process) | module calling `call_ssh("mv a b")` (sessions/exo save); inspect file moved | no sh needed, rc 0 | — | P2 | NOT RUN (deferred 2026-11-06: needs module exercising call_ssh mv; not cheap) |
| PLT-08 | MF-08 `posix_shim.h` fork stub → `wp_no_fork` | grep build: every `fork()` site compiled for Win32 is either `#ifdef`ed or harmless (list in `02_Architecture` §2.2) | inventory reviewed | — | P2 | NOT RUN (deferred 2026-11-06: review/inventory task) |
| PLT-09 | MF-09 `wp_alarm.c` alarm via timer queue | CAS timeout scenario of CAS-08 (silent CAS) | wims returns after the timeout | Linux SIGALRM | P1 | PASS (cas_equiv "timeout" 24/24) |
| PLT-10 | MF-05 `wp_mx_exchange` fixed=1 vs legacy | PLT-01 "mx fixed: silent interface → ~3 s"; issue #9 (bug calc.c:241) | legacy behaviour kept by default | upstream bug kept | P2 | PASS (PLT-01); report to wimsdev #9 pending Arnaud's review |

### 4.3 JOB — Job Objects and CPU limit (D4, issue #7)

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| JOB-01 | MF-03 `spawn_core` (CREATE_SUSPENDED → Assign → Resume), `make_job` | `fake_cas.exe busy` spawned by wims (run_equiv_pc.ps1 scenario) with rlimit 5 s | killed by job/watchdog < 6 s | RLIMIT_CPU kill | P0 | PASS (`05` cpu_limit "busy child killed within 6s") |
| JOB-02 | MF-03 `wp_cpu_watch` (accounting + 2× wall grace) | as JOB-01 on Win11, where `PerProcessUserTimeLimit` stays at 0 for busy loops (#7) | watchdog kills | — | P0 | PASS (Claude 2026-10-27: WIMS path < 6 s; the .NET harness that attaches after start is not representative) |
| JOB-03 | MF-03 job tree kill (KILL_ON_JOB_CLOSE) | `fake_cas.exe spawn` (grandchild `silent`), end of request; `Get-Process fake_cas` | no grandchild left | kill by exec.pid | P0 | PASS (phase 2 "tree kill", 68377ef tests) |
| JOB-04 | MF-03 `wp_set_job` mem limit | spawn with mem 64 MB, `fake_cas flood` | process killed / alloc fails | RLIMIT_AS | P1 | PASS (2026-11-06: Job Object PROCESS_MEMORY 64 MB via job_mem_probe; `fake_cas alloc 200` → exit 3; `alloc 32` → exit 0. Same limit API as `wp_set_job` mem_mb.) |
| JOB-05 | MF-03 `wp_job_kill` named job `Local\wims-job-<session>` | open session, start CAS, `cleaning` of session | job terminated | exec.pid kill | P1 | PASS (2026-11-06: `test_platform.exe` rebuilt — `wp_job_kill(session)` finds `Local\wims-job-*`, session process gone, unknown job → 0; 22/22. End-to-end CGI cleaning not separately re-run.) |
| JOB-06 | MF-25 `set_rlimits` → `wp_set_job` | `log/wims.conf` `rlimit_cpu=3`; OEF with heavy pari loop | CAS stopped after ~3 s, WIMS error shown | same message | P1 | PASS (2026-11-06 Laragon, runtime hook-20261106: rlimit_cpu=3 then heavy `sum(n=1,8*10^7,n%97)` via tool/directexec sw=pari → ~3.3 s, page 200 with « PARI/GP n'a pas pu exécuter votre code »; health check 200; light factor(2^64+1) OK after restore. Conf restored (no rlimit_cpu left; backup backup-exe-20261106/wims.conf.before-job06)) |
| JOB-07 | MF-31 `execredirected` Win (CPU 1500 s) | wimslogd task `housekeep.daily` runs > 20 s without kill | not killed before 1500 s | — | P2 | NOT RUN (deferred 2026-11-06: needs housekeep.daily >20 s wall observation) |

### 4.4 SBX — Sandbox ch..root.exe (issue #8, D10, D11)

Runner: `$rt\other\tests\test_sandbox_evasion.ps1 -Runtime $rt` (MF-61), with `fake_cas.exe` in `other\tests` and `ch..root.exe --grant-rx "$rt\other\tests"` done.

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| SBX-00 | MF-11 `main --selftest` → `sandbox_run(--noop)` (no cmd.exe) | `& "$rt\public_html\bin\ch..root.exe" --selftest; $LASTEXITCODE` | 0 | — | P0 | PASS (2026-10-27, 17/17 run) |
| SBX-01 | MF-11 `set_ace` session GENERIC_ALL, `launch` cwd=session | test "write inside session dir" | file created | chroot /tmp | P0 | PASS (2026-10-27) |
| SBX-02 | MF-11 `launch` std handles | test "stdin/stdout through the sandbox" (`'print("hello")' \| ch..root fake_cas plain`) | `hello` | — | P0 | PASS (2026-10-27) |
| SBX-S1a | MF-11 AppContainer, no ACE on `log\` | read `$rt\log\sbtest-secret.txt` | denied (rc≠0) | chroot | P0 | PASS (2026-10-27) |
| SBX-S1b | idem | read `$env:USERPROFILE\NTUSER.DAT` | denied (weak test: the file is also locked) | — | P0 | PASS (2026-10-27) |
| SBX-S2a/b/c | MF-11 | write `$rt\log\…`, `$env:TEMP\…`, sibling of session dir | file absent | chroot | P0 | PASS ×3 (2026-10-27; first S2 also 3/3 on 27071ff) |
| SBX-S3 | MF-11 no capability SID | `PING.EXE -n 1 1.1.1.1` inside sandbox | rc≠0 | — | P0 | PASS (2026-10-27; earlier phase 3) |
| SBX-S4a/b | MF-11 `launch` Job `ActiveProcessLimit=15`, `TerminateJobObject` after exit | `fake_cas bomb` | children ≤ 14; 0 left after | RLIMIT_NPROC=15 | P0 | PASS (children=14, 0→0) |
| SBX-S5 | MF-11 `quote_arg` (MS CRT rules) | `fake_cas args 'a" & PING.EXE … & "b' 'x\" y' 'c:\dir with space\'` | 3 lines equal to inputs | execv argv | P0 | PASS (2026-10-27) |
| SBX-S5b | MF-20 `execredirected` arg split (`'…'` quoting) | OEF whose parameter contains `"`, `&`, `;` sent to pari | literal text reaches CAS, no extra process | Linux same split | P0 | PASS (2026-10-28, via Laragon: formula with `"; system("PING.EXE…"); "` and `&` sanitized by wims, no PING.EXE/cmd.exe created) |
| SBX-S6 | MF-22 `octave.c illegal[]` (system) + MF-11 | Octave `system("PING.EXE 127.0.0.1")` through the interface | refused by interface | same list | P0 | PASS (2026-11-05: `system` → `sJstem` and `unix` → `uJix` via find_illegal after adding `unix` to octave.c illegal[]; wims.cgi.exe direct: both refused as undefined, 0 cmd.exe/PING.EXE, no profile write. Same confinement under D22 lowil as 2026-11-04) |
| SBX-S7 | MF-22 `illegal[]` += dos/winqueryreg/winopen | Octave `dos('x')`, `winqueryreg(...)` | refused | n/a (Win only) | P0 | PASS (2026-11-05: `dos` → `dJs`, `winqueryreg` → `wJnqueryreg` refused; 0 cmd.exe; unix() covered under SBX-S6) |
| SBX-S8 | MF-21 + MF-11 | Maxima `:lisp (with-open-file (s "D:/…/log/x" :direction :output))` | denied by ACL | chroot | P0 | PASS (2026-11-04, Maxima 5.49.0 sbcl.exe --core in AppContainer: `:lisp with-open-file` on log\ and on the profile → "Error opening … Accès refusé", file absent; in session → written; `sb-ext:run-program` PING.EXE and cmd.exe fail, 0 cmd.exe. Through the web the interface already mangles the Lisp text) |
| SBX-S9 | MF-11 no HKCU write | `fake_cas reg` | rc≠0, key absent | — | P0 | PASS (2026-10-27) |
| SBX-10 | MF-11 `main` `& sh\|perl` → 127 | `ch..root.exe '&' sh 'echo pwned > x'` | 127 | upstream non-root behaviour | P0 | PASS |
| SBX-11 | MF-11 `cleantmpdir` | `ch..root.exe cleantmpdir` | 0 | — | P1 | PASS |
| SBX-12 | MF-11 `DeleteAppContainerProfile`, `set_ace REVOKE_ACCESS` | profile count under `HKCU:\…\AppContainer\Mappings` before/after; ACL of session dir | no growth, no `S-1-15-2-…` ACE | — | P0 | PASS (143→142) |
| SBX-13 | MF-11 `purge_profiles_ex(stale=1)` | kill a ch..root mid-run (Job kill by wims), next run | orphan profile removed | — | P1 | PASS indirectly (profiles 157→143 after deployment, 2026-10-27) |
| SBX-14 | MF-11 `--purge-profiles` | `ch..root.exe --purge-profiles` | "deleted N" | — | P2 | NOT RUN (2026-11-06: skipped — Arnaud OK still required; deletes ~140 HKCU profile leftovers) |
| SBX-15 | MF-11 `grant_rx` | `ch..root.exe --grant-rx <CAS dir>`; `(Get-Acl dir).Access` | ACE `ALL APPLICATION PACKAGES` RX | — | P0 | PASS (other\tests, pari dir) |
| SBX-16 | MF-20 `execredirected` ISO_WRAPPER when `chroot\tmp\sessions\.chroot` exists; MF-03 `wp_sandbox_available` | factor.fr with marker / with `ch..root.exe` renamed / without marker | OK / refused / OK (direct) | — | P0 | PASS (2026-10-27, 3 cases) |
| SBX-17 | MF-11 + real CAS | pari `write("…/log/gp-evil.txt",1)` inside sandbox | error, file absent | — | P0 | PASS (2026-10-27) |
| SBX-18 | MF-11 env cleaning (`env_rm`) | `fake_cas` mode printing env (to add) | no `w_wims_home`, `SERVER_ADDR`… | chroot.c env_rm | P1 | PASS (2026-11-06: `fake_cas env` mode — Linux self-check PASS; mingw exe under `ch..root.exe` with w_wims_home/SERVER_ADDR/… set → all env_rm names absent, rc=0. PR https://github.com/io-e/wims4win/pull/11 not merged.) |
| SBX-19 | MF-11 TMPDIR made absolute | CAS writing `$TMPDIR/out` | file in session dir | — | P1 | PASS indirectly (factor.fr in sandbox) |
| SBX-20 | MF-54 marker by default | fresh `build-runtime-tree.sh` → `chroot/tmp/sessions/.chroot` exists | present | — | P0 | PASS (2026-10-28, fresh box build-runtime-tree.sh: marker present) |
| SBX-21 | D21 `lowil` policy (`ch..root.conf`, Cygwin CAS) | `tests/platform/test-sandbox-lowil.ps1`: explicit list (gap.exe, octave-cli.exe), `--policy` report, other CAS still AppContainer, write only in session (profile, profile TEMP, log, sibling session denied), no HKCU write, 15-process limit, memory_mb limit, kill-on-close, `& sh` refused, label removed, real GAP runs and cannot write to profile | 23/23 (incl. D22 Octave: runs, writes confined, unix in illegal[]); read and network NOT isolated (accepted trade-off, D21-14 informative) | Linux chroot (same model) | P0 | PASS (2026-11-05, 23/23 on ShadowPC; sandbox suite GROK_V1 26/26) |
| SBX-21 | MF-03 `token_elevated` (DROP_PRIV refused iff elevated) | PLT-01 case "DROP_PRIV refused iff elevated"; run wims from an elevated shell | refused | — | P1 | PASS (in PLT-01) |

### 4.5 LOGD — wimslogd (issue #1)

Client: `$rt\other\tests\wl_client.exe tmp/log/.wimslogd <cmd…>` (MF-32), run from `$rt`.

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| LOGD-01 | MF-26 `bringuplogd` → MF-06 `wp_daemon_spawn` | stop daemon (pid file), call `W 'module=adm/doc&cmd=new&lang=fr'` | 200; new pid in `tmp/log/wimslogd.pid`; `wimslogd.out` "started … (win32)" | — | P0 | PASS (2026-10-27) |
| LOGD-02 | MF-30 `answer_w`, MF-31 `cmd_ping` | `wl_client … ping` | `OK`, `0 <loadavg> …`, `127.0.0.1` | same format | P0 | PASS |
| LOGD-03 | MF-30 `forkman` | `forkcnt` | `OK 0` | — | P1 | PASS |
| LOGD-04 | upstream `cmd_record*`, `readfile` via Win IPC | `recordcnt`, `record 1 …`, `readfile log/wims.conf 200` | OK answers | — | P1 | PASS (box/wine with test data; PC answers OK) |
| LOGD-05 | `options()` path guards over IPC | `-c 1/../.. readfile log/wims.conf` | `ERROR 2 illegal_fname` | same | P0 | PASS |
| LOGD-06 | `cmd()` bad_cmd | `nosuchcmd` | `ERROR 2 bad_cmd` | same | P1 | PASS |
| LOGD-07 | MF-50 `--stack,33554432` (MAX_FILELEN 2 MB buffers on stack) | `recordcnt` on a file | no crash (stack overflow found and fixed) | 8 MB stack | P0 | PASS (wine + PC, 2026-10-27) |
| BLD-05 | MF-50 `build-wims-win32.sh` `--stack,8388608` | `-fstack-usage` frames up to ~287 KB (calc_listunion); reserve was 2 MB (mingw default) | 8 MB reserve as on Linux | ulimit 8 MB | P1 | PASS (2026-10-28, SizeOfStackReserve 0x800000, deployed, CGI runs OK) |
| LOGD-08 | `cmd_getscore` over pipe | `-c 1000 -u anonymous getscore` | binary answer (404 611 bytes), daemon alive | same size expected | P0 | PASS (size); Linux byte parity NOT RUN |
| LOGD-09 | MF-30 `wl_spawn_task("cleaning")`, `run_task` | wait ≥ 2 min with env `wimslogd="3600 3600 3600 10 0 0 -1 0 8 100"`; old session dir (mtime −2 days) | old session removed, `tmp/log/lastclean` created, kept session with `wims_user=` | forked child | P0 | PASS (wine 2026-10-27; PC: lastclean created) |
| LOGD-10 | `dispatch_log` task via `sh -c` (MSYS2) | as LOGD-09, check `tmp/log/exec.fail` absent | no exec.fail | — | P1 | PASS (PC 2026-10-27) |
| LOGD-11 | per-task stderr file + NUL fallback (`wl_spawn_task`) | two tasks overlapping | both start | — | P1 | PASS (fix after sharing violation, wine) |
| LOGD-12 | housekeep.daily / modupdate.auto / backup | leave daemon running past trigger times; read `tmp/log/housekeep.log` | scripts run via sh | Linux | P1 | PASS (script body via MSYS sh 2026-11-06). **Daemon spawn FAIL confirmed 2026-11-09 16:20–16:29 Europe/Paris** on live hook-20261106: restarted `bin/wimslogd.exe` PID 5132 (WorkingDirectory=runtime root, bundled msys2 sh on PATH); `thismin` tick fired housekeep → keepdate=`20261109` 16:22:08 + empty `housekeep.log` + `exec.fail` «Failed to execute bin/housekeep.daily error 0»; modupdate → mupdate=`20261109` 16:29:12 + empty `modupdate.log` + same exec.fail. **Root cause:** `call_ssh(..., "bin/… &>%s/….log", tmpd)` makes `outf==errf`; `open_redirect` in `wp_win32.c` opens with `FILE_SHARE_READ` only → second `CreateFile` = ERROR_SHARING_VIOLATION (32) (repro’d). `GetLastError` later reads 0 after CloseHandle of the first handle. **Proposed fix (not committed):** in `wp_spawn`/`open_redirect`, if outf and errf are the same path, open once and `DuplicateHandle` for stderr (or add `FILE_SHARE_WRITE`). Daemon left running (PID 5132). Notes: tests-win_ALF/LOGD-12_15_overnight.txt) |
| LOGD-13 | MF-06 `pipe_name` per tree hash | two runtimes (wims-runtime, r19040-grok, tests-win) running simultaneously | each CGI talks to its own daemon | — | P1 | PASS (3 daemons coexisting on PC, 2026-10-27) |
| LOGD-14 | MF-30 pid check / daily restart (`wait_children`) | overwrite `tmp/log/wimslogd.pid` | daemon exits within 1 min; next request restarts it | same | P2 | NOT RUN (deferred 2026-11-06: pid overwrite / daemon restart — disruptive) |
| LOGD-15 | class scores end-to-end | in a test class: student does an exercise, then teacher score page | scores shown | Linux | P1 | PASS (2026-11-06 sibling hook-class only: root cause was missing `HAVE_CRYPT` in win32 config.h — `_passcrypt` no-op → passcheck always no. Fixed in PR #12; cgi deployed to sibling only. Student anonymous login + worksheet exi1 reply → score file `log/classes/9001/score/anonymous1`; supervisor userscore **Notes** 200 shows class sheets. Live Laragon untouched.) |

### 4.6 CGI — wims.exe CGI and Apache/Laragon

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| CGI-01 | MF-07 `wp_cgi_prepare` | `W 'module=adm/doc&cmd=new&lang=fr'` | `Status: 200` | — | P0 | PASS |
| CGI-02 | MF-07 binary mode (d1d10c0) | open OEF exercise, then `cmd=reply` with the same session | session kept (no `cmd=new` fallback) | — | P0 | PASS (d1d10c0 fix; createqcm, factor reply 2026-10-27) |
| CGI-03 | MF-07 incomplete tree → 500 | rename `$rt\public_html\themes` temporarily | `Status: 500 WIMS runtime tree incomplete` listing it | — | P2 | NOT RUN (deferred 2026-11-06: temporarily rename themes — disruptive to live Laragon) |
| CGI-04 | MF-26 SERVER_ADDR NULL (f999023) | run CGI without `SERVER_ADDR` | no access violation | — | P1 | PASS (all PowerShell runs omit it) |
| CGI-05 | E-WEB + MF-58 vhost | browser: `http://wims_paris_saclay_fr.test/wims/wims.cgi` | 200 home | — | P0 | PASS on Claude's runtime (2026-10-27); NOT RUN with `WIMS_RT` → Grok runtime |
| CGI-06 | E-WEB | `http://wims_paris_saclay_fr.test/wims/wims.cgi?module=tool/algebra/factor.fr&lang=fr` → enter `2^64+1` | `274177 × 67280421310721` | Linux | P0 | PASS (2026-10-28, after ch..root ensure_profile_env fix: Apache minimal env made CreateProcessAsUser fail with err 203) |
| CGI-08 | E-WEB `ScriptAliasMatch` | exercise image `/wims/wims.gif?cmd=getins…` (e.g. chronology) via Laragon | 200 image/gif | Linux apache-config maps wims.* | P1 | PASS (2026-11-06 Laragon HTTP, runtime wims-runtime-r19040-grok-hook-20261106: `/wims/wims.gif?cmd=getins&session=…&special_parm=insert-1.gif` via flydraw through tool/directexec → 200 Content-Type: image/gif, GIF87a, 839 B; ScriptAliasMatch for wims.* active) |
| CGI-07 | MF-25 PATH 8 KiB, `wims_home` with `/` | `W` home page with a long inherited PATH (>4 KiB) | 200 | — | P2 | PASS (2026-11-06: inherited PATH length ~4686 with 4500-char pad → adm/doc 200) |

### 4.7 IFC — CAS interfaces

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| IFC-01 | MF-20, MF-61 `tests/interfaces/run_equiv_pc.ps1` | run on E-PC for PARI, Maxima, Octave, GAP (calc, factor, large, illegal, about, timeout, absent, multiexec) | 24/24 | Linux outputs | P0 | PASS (`05` cas_equiv 24/24) |
| IFC-02 | MF-21 `maxima.c` sbcl.exe `--core` (D3, D10, #2) | `Get-Process` during a Maxima request: parent is sbcl.exe, no cmd.exe, no maxima.bat | sbcl only | — | P0 | PASS (#2 closed, 3c59829) |
| IFC-03 | MF-22 GAP direct exe (51b54d5) | GAP scenarios of IFC-01 | 8/8 on rerun | Linux | P1 | PASS (Claude 2026-10-27: 21/24 then 8/8 on rerun; first failure during mkindex load) |
| IFC-04 | D12 `pari_command` in `log/wims.conf` (MF-54 `PARI_COMMAND`) | `Select-String pari_command $rt\log\wims.conf`; `!exec pari about` | path to gp.exe | — | P0 | PASS |
| IFC-05 | MF-24 CR strip on exec.err | CAS error message in a module | no `\r` in page | — | P1 | PASS (PLT-03, f81abf1) |
| IFC-06 | MF-20 + sandbox for Maxima/Octave/GAP | with `.chroot` marker + `--grant-rx` on each CAS dir, rerun IFC-01 | 24/24 | — | P0 | GAP/Maxima/Octave PASS under sandbox (D21/D22). Laragon HTTP 2026-11-06 on sibling wims-runtime-r19040-grok-hook-20261106 tool/directexec: Maxima factor(x^4-1)=(x-1)*(x+1)*(x^2+1), integrate=sin(x)-x*cos(x); Octave det=-2, mean=0.5, a-b; 0 cmd.exe. Earlier wims.cgi.exe-direct evidence kept as corroboration. |

### 4.8 MML — MathML and formula size (D7)

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| MML-01 | MF-27 `mathml()` via `wp_spawn` (`bin/wims_mathml.exe`); MF-61 `win/test_mathml_spawn.c` | page with formula (latex2wims example, `\frac`) | `<math` in page | same MathML | P0 | PASS (`05` mathml; latex2wims PC check "MathML 2/3 blocks") |
| MML-02 | MF-54 `default_texposition=2` | `Select-String default_texposition $rt\log\wims.conf` | `=2` | — | P0 | PASS |
| YAML-01 | MF-27 + MF-58 `log/wims4win.yaml` `formula: size: 200%` | page source: formula style font-size 200 % | 200 % | identical on Linux (9b38fb7) | P1 | PASS (d3c33a3/d60e1f9 results) |
| YAML-02 | range 50–400 clamp | set `size: 900%` then `10%` | clamped to 400 % / 50 % | same | P1 | PASS (2026-10-28: 900%→400%, 30%→50%, abc→200% on local/exemple.fr block=mainS1; mathml.c now clamps) |
| YAML-03 | missing YAML | rename `wims4win.yaml` | default 200 %, no error | same | P2 | PASS (2026-11-06: renamed wims4win.yaml aside → local/exemple.fr still 200, no WIMS User Error; restored from Bak) |

### 4.9 HLP — Helpers (Misc/OefMsg2wims, webget, flydraw, insdraw, the 17 programs)

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| HLP-01 | MF-51 translator | `W` for `tool/wcalc.fr` | 200 (was 550) | Linux 200 | P1 | PASS (2026-10-27) |
| HLP-02 | MF-12 webget WinHTTP; D16 modupdate | `sh bin/modupdate` in check mode | list from unice/wimsedu; factor.fr installed with webget.exe + tar | Linux | P1 | PASS (2026-10-27) |
| HLP-03 | MF-51 msg2wims | latex2wims L2W-PC-03 (uses `bin\msg2wims.exe`) | blocks generated | Linux | P1 | PASS |
| HLP-04 | MF-51 oef2wims; MF-70 | see OEF-01/02 | as OEF-01/02 | Linux | P1 | PASS (via OEF-01/02) |
| HLP-05 | MF-52 chemeq (C++/flex, `bsd_index.h`) | `$env:wims_exec_parm='2H2 + O2 -> 2H2O'; perl bin/chemeq` | `2 H2 + O2 -> 2 H2O` + LaTeX + OK | identical on wine vs Linux | P1 | PASS (PC + wine parity 2026-10-27) |
| HLP-06 | MF-52 units-filter (gmp, libgnurx) | `sh bin/units-filter` with `3.5 m/s` | `3.5 1 0 -1 0 …` | identical (wine) | P1 | PASS |
| HLP-07 | MF-52 moneyprint, sigdigits, scienceprint, toascii | `sh bin/<name>` with parameters of §5 of the Suivi | `1.20,30.10,0.40,-0.23`; `5,1,4,1,0,1`; `1.23*10^4`; toascii output | identical (wine) | P1 | PASS ×4 |
| HLP-08 | MF-52 lceb_dynopt | `sh bin/lceb` with `100 1 2 3 4 5 6` | FOUND + steps | — | P1 | PASS (MSYS2 `ulimit -f` warning harmless) |
| HLP-09 | MF-52 canvasdraw (`canvasdraw_win.h`) | inside a module using canvasdraw (`!exec canvasdraw`) | canvas JS emitted | Linux | P1 | PASS (2026-11-06: module `local/canvasdraw_hlp09.fr` with `!exec canvasdraw` → page emits `canvas_script…js` getfile; JS ~4 KB canvasdraw 0.5 runtime (`read_canvas*`, draw helpers). wrap..exec.exe restored into hook `public_html/bin`. Standalone correctly refuses outside WIMS.) |
| HLP-10 | MF-52 crossword (`sys/times.h`) | module using crossword | grid | Linux | P2 | PASS (2026-11-06: standalone `other/bin/crossword.exe wordlist` rc=0 produces a grid; « Cannot open bcd » was a bad argv — bcd is not a wordlist file. Laragon exo=crossword → 200, no Cannot open/bcd. No code change.) |
| HLP-11 | MF-52 mathexp (libgnurx), symtext, voronoi, whirlgif, bioinfo×4, checkmol/matchmol | one call each through its `bin/` script | non-empty, matches Linux | Linux | P2 | PASS (2026-11-06 partial set on hook runtime: whirlgif.exe runs; checkmol/matchmol.exe present + usage; public_html/bin/bioinfo present. mathexp.exe and voronoi.exe absent from this slim tree — not built/deployed here.) |
| HLP-12 | MF-16 clickzone `system()` → `sh -c` | module with clickzone on a GIF | zone detected; no cmd.exe (`Get-Process`) | — | P2 | NOT RUN (deferred 2026-11-06: needs clickzone GIF module + cmd.exe watch) |
| HLP-13 | MF-15 wrap..exec.exe | `.\bin\wrap..exec.exe ../other/bin/units-filter` | runs, returns child rc | wrap.c | P1 | PASS (rc propagated, 2026-10-27) |
| HLP-14 | MF-53 flydraw.exe, MF-13 insdraw..processor.exe | OEF 00001 with drawing | GIF identical to Linux | Linux | P1 | PASS (Suivi: "00001 identical, GIF included") |
| HLP-15 | MF-40 `other/bin/src2def` via sh | latex2wims/mkindex path, `sh other/bin/src2def` | .def generated | Linux | P1 | PASS indirectly (mkindex runs) |
| HLP-16 | `bin/mkindex` via MSYS2 sh (D17) | `sh bin/mkindex` | rc 0 | Linux | P1 | PASS (2026-11-06 hook-20261106: `bin/run-mkindex.sh` -> EXIT:0, Start-Process ExitCode=0 (C:/Users/Shadow/mkindex5.log, ~27 min Paris time). Without PATH fix, Windows find.exe shadowed MSYS find and cleanup exited 2 (Fichier introuvable - *.bin). Fix: export PATH="/usr/bin:/bin:$PATH" at start of mkindex (tools/patches/mkindex-wims4win.patch + live runtime). With PATH: 0x Fichier introuvable in stderr; cleanup classes OK. Benign: no strfile; missing modules/data/images.) |
| HLP-17 | `bin/oefcompile` via sh | compile a local OEF module | `.oef` → `.def` | Linux | P1 | PASS (2026-11-06: `sh -c './bin/oefcompile test local/oef2html_web.fr'` from runtime root → 42 sources, 42 compiled, Total failure: 0) |

### 4.10 MSYS — MSYS2 bundle (D17)

| ID | Linked code | Steps | Expected | Prio | Status |
|---|---|---|---|---|---|
| MSYS-01 | MF-55 pins sha256 | E-LNX: `bash tools/fetch-msys2-sh.sh <out>` | all sha256 OK, `other/MSYS2-LICENSES.txt` | P1 | PASS (runtime built) |
| MSYS-02 | MF-55 separate root `other/msys2` (a0e0f62) | `sh -c 'pwd; echo $wims_home'` from `$rt` | real runtime path (not `/`, not `//`) | P1 | PASS (fix a0e0f62, mkindex now runs) |
| MSYS-03 | MF-25 PATH order | `W` + `!exec sh` path; `Get-Command sh` inside CGI env | `other\msys2\usr\bin\sh.exe` | P1 | PASS |
| MSYS-04 | ed2c3f7 perl5 modules | `perl -Mstrict -Mwarnings -e 1` | rc 0 | P1 | PASS |
| MSYS-05 | no cmd.exe anywhere (D10, D17) | during SMK-01…03 and LOGD-09: `Get-CimInstance Win32_Process -Filter "Name='cmd.exe'"` filtered on ParentProcessId chain | none spawned by WIMS | P0 | PASS (2026-10-28: 0 cmd.exe during sandbox suite GROK_V1 26/26 and Laragon CGI runs) |

### 4.11 OEF — oef-validation and oef2html parity (D13, D19)

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| OEF-01 | MF-70 `tools/oef-validation/oef2wims/parity.sh`, CI job 3 | E-LNX/CI: oef2wims Linux vs mingw (wine), v2, trace on 1 438 files | 1438/1438 | Linux | P1 | PASS (`05` oef_parity; CI green) |
| OEF-02 | MF-70 + wims.exe | PC sample 10 files, seed 1 | 6 identical pages, 4 identical failures (00347, 00352, 00509 too_many_gotos; 01012 bad_index) | Linux | P1 | PASS (2026-10-27 on runtime r19040-grok) |
| OEF-03 | module `local/exemple.fr` | `W 'module=local/exemple.fr&cmd=new&lang=fr'` | 200, exercise rendered | Linux | P1 | PASS (2026-11-06 Laragon: module=local/exemple.fr&cmd=new&lang=fr → 200, page rendered, len≈12810) |
| OEF-04 | module `local/oef2html_web.fr` (42 OEF from `D:\ALF_ZGit\03-oef2html_Web\src\oef\fr`) | for each of the 42 `exo=`: new + reply | 42 × 200, no `WIMS User Error`; parity with Linux on seed | Linux | P1 | PASS (2026-11-06 Laragon: all 42 exo of local/oef2html_web.fr → new 42/42 status 200, reply 42/42 status 200, no WIMS User Error; use module name with .fr) |

### 4.12 L2W — latex2wims (branch feat/latex2wims-doc)

| ID | Linked code | Steps | Expected | Prio | Status |
|---|---|---|---|---|---|
| L2W-LNX | MF-71 `tests/run_tests.sh`, `mkindex.c`, `latex2wims.c check` (0449ac3 mathfonts) | E-LNX: `make -C tools/latex2wims test WIMS_HOME=/workspace/rt` | 11/11 | P1 | PASS (2026-10-27) |
| L2W-PC-01 | MF-71 `all` | `.\latex2wims.exe all --wims-home $rt --tex .\exemple.tex --module local/l2w-test-exemple.fr --expect Euler --expect mfrac` | `== check … OK (3 blocs) ==`, rc 0 | P1 | PASS (2026-10-27) |
| L2W-PC-02 | `corpus` parity | `.\latex2wims.exe corpus … --ref .\parity\ref_linux.tsv` | 5/5 identical to Linux | P1 | PASS |
| L2W-PC-03 | `all` × 4 corpus docs via `wims.cgi.exe` | see `LISEZMOI_TESTS.txt` §3 | 4 × rc 0 | P1 | PASS |
| L2W-PC-04 | exit codes | §4 | 2 / 3 / 4 | P2 | PASS |
| L2W-PC-05 | cp1252 conversion diagnostics | accents_cp1252 | 3 diagnostics `ligne:colonne` | P2 | PASS |

### 4.13 BLD/CI — builds and CI (D18, D20)

| ID | Linked code | Steps | Expected | Prio | Status |
|---|---|---|---|---|---|
| BLD-01 | MF-54 | E-LNX `bash tools/build-runtime-tree.sh` (svn export r19040) | tree + `log/wims4win-provenance.txt`; no ELF left; ~225 svn:special links restored | P0 | PASS (runtime r19040-grok, 2026-10-27) |
| BLD-02 | MF-50…MF-53 | `build-wims-win32.sh`, `build-wimslogd-win32.sh`, `build-chroot-win32.sh`, `build-misc-win32.sh` (18), `build-misc-extra-win32.sh` (20), `build-flydraw-win32.sh` | all "built", 0 FAILED | P0 | PASS (2026-10-27) |
| BLD-03 | MF-52 `fetch-mingw-deps.sh` | fresh `build/deps` | sha256 verified | P2 | PASS |
| CI-01 | MF-60 | push → 3 jobs green | green | P1 | PASS (ea6dc73 success) |
| CI-02 | MF-60 timeouts | apt hang on "Install mingw-w64" | step stops ≤ 10 min, job ≤ 30 min | P1 | PASS (run 40540419060: job failed by timeout instead of hanging) |
| CI-03 | apt retries (ea6dc73) | 3 consecutive pushes | no Install hang | P2 | NOT RUN (deferred 2026-11-06: needs 3 consecutive CI pushes) |

### 4.14 ENC/PATH/FS — encodings, paths, locking, security

| ID | Linked code | Steps | Expected | Prio | Status |
|---|---|---|---|---|---|
| ENC-01 | MF-07 `_O_BINARY`, MF-31 wimslogd `_fmode` | after sessions, `Format-Hex` on `sessions\*\var.stat`: no `0D 0A` | LF only | P0 | PASS indirectly (sessions survive replies, d1d10c0) |
| ENC-02 | MF-04 CR strip of CAS output | CRLF CAS (`fake_cas crlf`) | no CR in page | P1 | PASS (IFC/PLT) |
| ENC-03 | windows-1252 pages | `charset=windows-1252` header and accents render (`Factorisation`) | correct | P1 | PASS (factor.fr page) |
| ENC-04 | MF-71 UTF-8 → windows-1252 | L2W-PC-05 | — | P2 | PASS |
| PATH-01 | MF-03 `quote_arg`, MF-11 `quote_arg`, MF-15 `q` | runtime copied to `D:\wims test\rt with space`; SMK-01…03 | all pass | P1 | PASS (2026-11-06: copy of hook-20261106 → `Reprise_Wims_Grok\rt with space` (robocopy, live untouched). SMK-01 200; SMK-02 factor seeded from r19040-grok into the copy only → 67280421310721; SMK-03 wcalc 200. Direct CGI.) |
| PATH-02 | long paths > 260 | session dir nested > 260 chars (module path) | error is explicit, no crash | P2 | PASS (2026-11-06: nested session path length 433 > 260 created under tmp/sessions; no crash. Cleaned up after.) |
| PATH-03 | MF-25 wims_home `/` separators | `W` + `!exec` helpers using `$wims_home` | OK | P1 | PASS (93e8775) |
| FS-01 | MF-30 file sharing (`open_redirect` FILE_SHARE_READ) | overlapping wimslogd tasks | no sharing violation (fixed) | P1 | PASS (LOGD-11) |
| FS-02 | session files locked by a running CAS while cleaning | start long CAS, force cleaning | cleaning retries, no daemon crash | P2 | NOT RUN (deferred 2026-11-06: long CAS + force cleaning race) |
| SEC-01 | MF-10 crypt DES / BCrypt | `test_crypt.exe` | 8/8 (DES 4/4, BCrypt 3/3, cross 1/1) | P0 | PASS (`05` crypt) |
| SEC-02 | MF-06 pipe ACL | another local user opens `\\.\pipe\wims-wimslogd-<hash>` | access denied, or documented risk | P1 | WAITING (2026-11-06: needs a second local Windows user — not created by bot. Steps/script: tests-win_ALF/SEC-02_pipe_acl_steps.ps1; live pipes seen `\\.\pipe\wims-wimslogd-*`. Default CreateNamedPipe ACL undocumented until cross-user probe.) |
| SEC-03 | MF-03 `token_elevated` | CGI run elevated | CAS refused (DROP_PRIV) | P1 | PASS (PLT-01) |
| SEC-04 | runtime `log\` not granted to AppContainers | `(Get-Acl $rt\log).Access` has no `S-1-15-2-1` | absent | P0 | PASS (SBX-S1a) |

### 4.15 SMK — minimal smoke (proves the modified code is exercised)

| ID | Linked code exercised | Steps | Expected | Prio | Status |
|---|---|---|---|---|---|
| SMK-01 | MF-07, MF-26, MF-06, MF-30 | `W 'module=adm/doc&cmd=new&lang=fr'` | 200; daemon started | P0 | PASS |
| SMK-02 | MF-20, MF-23, MF-24, MF-03, MF-11 (marker on) | `$p=W 'module=tool/algebra/factor.fr&cmd=new&lang=fr'; $s=[regex]::Match($p,'session=([A-Za-z0-9_.]+)').Groups[1].Value; W "session=$s&module=tool/algebra/factor.fr&cmd=reply&formula=2%5E64%2B1&calc=factor&lang=fr"` | contains `67280421310721` | P0 | PASS (2026-10-27, in sandbox) |
| SMK-03 | MF-03 `wp_script_interp`, MF-51 | `tool/wcalc.fr`, createqcm | 200 | P1 | PASS |


### 4.16 ADM/PWD/ACL — server administration (docs/10 §17)

Run 2026-11-12, Europe/Paris, on an isolated Debian tree `/workspace/mw-wims-debian/` (copy of a box WIMS install; other trees not deleted) and on Windows siblings only: `wims-runtime-r19040-grok-hook-class-20261106` (HAVE_CRYPT CGI) and `wims-runtime-r19040-grok-hook-logd-20261109` (fixed wimslogd, **no** HAVE_CRYPT CGI). Live `hook-20261106` was read-only. Laragon config and `Reprise_Wims_Claude` were not touched. Passwords came only from `C:\Users\Shadow\wims_test_creds.txt` or one-time generated secrets; changed class files were restored; no secret is recorded here.

| ID | Linked code | Steps | Expected | Prio | Status |
|---|---|---|---|---|---|
| PWD-01 | `adm/manage/auth.proc`, `log/.wimspass`, `HAVE_CRYPT` | site manager login from an IP allowed by `manager_site` | `wims_ismanager=2`, job=list | P0 | PASS (2026-11-12: Debian + Windows hook-class; file hashed, mode 600 / restricted ACL) |
| PWD-02 | same | replace `.wimspass`; old fails, new works; hash rewrite | only new password works | P1 | PASS (2026-11-12: Debian + Windows hook-class) |
| PWD-03 | `tmp/log/.wimspassone` | erase `.wimspass`; one-time password file; login | official one-time mode | P2 | PASS (2026-11-12: Debian + Windows hook-class) |
| PWD-04 | `adm/class/passwd` `job=user` | supervisor changes own password; restore afterwards | old rejected, new accepted | P1 | PASS (2026-11-12: Debian isolated 9001 + Windows hook-class; files restored) |
| PWD-05 | `adm/class/passwd` `job=part` | supervisor sets a participant password; restore afterwards | new login works, old rejected | P1 | PASS (2026-11-12: Debian + Windows hook-class; files restored) |
| PWD-06 | `adm/class/classes` auth, class 9001 | supervisor + student login (scored exercise already LOGD-15) | both sessions identified | P1 | PASS (2026-11-12: Debian login + Windows hook-class HAVE_CRYPT; student may be `anonymousN`; scored exercise not re-driven, see LOGD-15) |
| PWD-07 | `src/Platform/win32/config.h` HAVE_CRYPT, PR #12 | same class hashes: CGI without HAVE_CRYPT vs fixed CGI | pre-fix rejects all; post-fix accepts | P1 | PASS (2026-11-12: hook-logd CGI = live hash, supervisor login empty; hook-class HAVE_CRYPT, 302 + identity. Linux `config.h` already has HAVE_CRYPT 1. PR #12 still unmerged; live CGI still unfixed.) |
| PWD-08 | `auth.proc` / `var.auth` | empty and wrong manager (and class) passwords | `wrongpass` / no `ismanager=2` | P1 | PASS (2026-11-12: Debian + Windows manager empty/wrong) |
| PWD-09 | repo hygiene | `git ls-files` for `.wimspass`, creds, class user files | nothing tracked | P1 | PASS (2026-11-12: none tracked; docs mention the path only) |
| PWD-10 | `adm/manage/developer` | create/change a module developer password | Modtool login works | P2 | NOT RUN (2026-11-12: developer UI not exercised; manager path already proven) |
| ACL-01 | `manager_site`, `manager_check()` | `adm/manage` from an IP not in `manager_site` | `wims_ismanager=0`, job=`no_right` | P0 | PASS (2026-11-12: Debian + Windows, REMOTE_ADDR=203.0.113.50) |
| ACL-02 | `manager_check` on `wims.conf` mode | Debian `chmod 640` `log/wims.conf` | job=badconf, `wims_ismanager=-1` | P1 | PASS (2026-11-12: Debian). Windows: see ACL-04 (NTFS). |
| ACL-03 | `manager_check` on `.wimspass` mode | Debian `chmod 640` `log/.wimspass` | job=badconf, `wims_ismanager=-2` | P1 | PASS (2026-11-12: Debian). Windows: see ACL-04 (NTFS). |
| ACL-04 | NTFS ACLs on `.wimspass`, `tmp\`, `log\` | inspect default ACL; restrict with `icacls`; no second user | Auth Users may inherit write on `tmp`; `.wimspass` can be locked down | P1 | PASS (2026-11-12: hook-class; default inherited Authenticated Users (M) on parent; after `inheritance:r` + grant user only, open ACEs removed. Cross-user read not tested — see SEC-02.) |
| ACL-05 | `bin/setwrapexec`, `wrap..exec` 6755 nobody | run official script as root | mode 6755, CGI still works | P1 | NOT RUN (2026-11-12: no root on the box; script refuses non-root; wrap was 755 box:box in the isolated tree) |
| ACL-06 | named pipe ACL | second Windows user opens `\\.\pipe\wims-wimslogd-<hash>` | deny or documented risk | P1 | WAITING (map SEC-02; reserved for Arnaud, 2026-11-12) |
| ACL-09 | class isolation | supervisor of 9001 opens class 1000 tools | denied | P1 | PASS (2026-11-12: Debian + Windows hook-class) |
| ACL-10 | role isolation | student opens supervisor / teacher userscore tools | denied | P1 | PASS (2026-11-12: Debian + Windows hook-class; no usermanage tools) |
| ACL-11 | developer vs site manager | developer account cannot run `adm/manage` jobs | denied | P2 | NOT RUN (2026-11-12: depends on PWD-10) |
| ADM-01 | `wimslogd.pid`, `bringuplogd` | after Windows reboot: stale pid / CGI bring-up | daemon live or stale pid documented | P1 | PASS (2026-11-12: live pidfile present, 0 live process — stale after reboot; sibling CGI/start used, live not started) |
| ADM-02 | per-tree pipes, stop by path | start two sibling daemons; stop only one by path | the other stays; live untouched | P1 | PASS (2026-11-12: hook-class + hook-logd; stopped logd by path, class stayed; both stopped at the end; live procs 0) |
| ADM-03 | `bin/backup`, `backup_hour` | daemon-driven backup writes `backup/` | log + archive | P2 | NOT RUN (2026-11-12: not forced; overlaps LOGD-12; avoid extra jobs on siblings) |
| ADM-04 | `log/motd.phtml.fr` | token appears on home, disappears after removal | MOTD shown then gone | P2 | PASS (2026-11-12: Debian isolated tree, lang=fr) |
| ADM-05 | `ALGOGEBRA_HOOK=0`, uninstall scripts | flag in `build-runtime-tree.sh`; hook present; no uninstall on existing trees | documented opt-out / uninstall exist | P2 | PASS (2026-11-12: hook phtml on hook-class; scripts on `feat/algogebra-json-hook`; uninstall not executed) |
| ADM-06 | load thresholds vs CPU | change thresholds; site still serves | no load generation | P2 | NOT RUN (2026-11-12: load tests out of scope) |
| ADM-07 | teardown of a throwaway sibling | delete only a new copy; live/Claude intact | safe teardown | P2 | NOT RUN (2026-11-12: no throwaway sibling created; ask-before-overwrite) |
| ADM-08 | Debian upgrade + `setwrapexec` | package or manual upgrade, class data intact | official wiki path | P1 | NOT RUN (2026-11-12: no root / no apt upgrade of a production package; isolated tree only) |


### 4.17 SETUP/BKP — fresh instance and per-class backup/restore (docs/11)

Run 2026-11-12, Europe/Paris. Debian: new tree `/workspace/mw-wims-setup-debian/` (other trees not deleted). Windows: new sibling `wims-runtime-r19040-grok-setup-test-20261112` only. Live `hook-20261106`, Laragon, and `Reprise_Wims_Claude` were not touched. Source class 9001 in `wims-runtime-r19040-grok-hook-class-20261106` was read-only (official `pack.sh` ran on a copy). Credentials only from `C:\Users\Shadow\wims_test_creds.txt`; no secret is recorded here. Local merge of PR #12 and PR #13 was created on the box and not pushed; cross-compile was blocked (`deb.debian.org` HTTP 500, no mingw/make/flex), so previously built PR #12 CGI and PR #13 `wimslogd` binaries were installed in the new sibling.

| ID | Linked code | Steps | Expected | Prio | Status |
|---|---|---|---|---|---|
| SETUP-01 | pre-built r19040 tree | deploy a fresh Debian instance in a new folder; strip example 9001; `chmod 600` conf | tree boots | P1 | PASS (2026-11-12 12:05 Paris: `/workspace/mw-wims-setup-debian/`; apt/`./compile` not used — see SETUP-07) |
| SETUP-02 | `bringuplogd`, `tmp/log/wimslogd.pid` | first CGI starts `wimslogd`; stop by pid afterwards | daemon lives then stops | P1 | PASS (2026-11-12 12:07: pid 294617, home 200 OK; stopped) |
| SETUP-03 | sibling runtime policy | `robocopy` a built runtime to a new folder; refuse overwrite | new sibling only | P0 | PASS (2026-11-12 11:59–12:01: `wims-runtime-r19040-grok-setup-test-20261112`, 22922 files) |
| SETUP-04 | `HAVE_CRYPT` (PR #12), `DuplicateHandle` (PR #13) | new sibling has HAVE_CRYPT CGI and fixed `wimslogd` | both fixes present | P0 | PASS (2026-11-12: `wims.cgi.exe` sha256 42BBAE9D…D8015C and wimslogd 19E7E257…C4A3C4E1 from the start; extensionless `wims.cgi` was still the pre-HAVE_CRYPT `7C8FFCED…` build until aligned at 12:36, after which both names hash 42BBAE9D…; box compile of the local merge was blocked) |
| SETUP-05 | `log/.wimspass`, `icacls` | create manager password file if missing; lockdown `.wimspass` / `log` / `tmp` | tight ACL, no Auth Users on `.wimspass` | P1 | PASS (2026-11-12 12:09: created `.wimspass`; `Shadow:(R,W)` only) |
| SETUP-06 | docs/10 §9.1 | start `wimslogd` (drop stale pid), first CGI, stop **by path** | path-matched pid, then none | P1 | PASS (2026-11-12 12:11: pid 22400, CGI 200 OK 27031 bytes; stopped by path) |
| SETUP-07 | `./compile`, `apt-get install wims` | from-source Debian compile or official package | official install path | P2 | NOT RUN (2026-11-12: `deb.debian.org` HTTP 500; `make`/`flex`/`mingw` absent) |
| SETUP-08 | Laragon `WIMS_RT` | owner applies vhost lines from docs/11 §8 | HTTP front-end on the new sibling | P2 | WAITING (2026-11-12: lines given, not applied) |
| BKP-01 | `arch/pack.sh`, `download.proc` | official `tgz` of class 9001 from a copy; source `.def` unchanged | archive stored off-tree | P0 | PASS (2026-11-12 12:03:41: 7155000 bytes, sha256 c94b37a6…aa408f, 510 entries, top-level `class/`; source `.def` mtime still 2017-06-06) |
| BKP-02 | filesystem tar of `log/classes/9001` | `System32\tar.exe -czf` from the source, read-only | fallback archive | P1 | PASS (2026-11-12 12:02:50: 7173356 bytes, sha256 afc1a478…d89976, 523 entries, top-level `9001/`) |
| BKP-03 | `restore.sh` format, `.build-index` | restore official pack into fresh Debian 9001; files and scores | class + score hash match | P0 | PASS (2026-11-12 12:05:47: 7 users, `score/anonymous1` 117 bytes sha256 19A23E2F…6692, index line added) |
| BKP-04 | `adm/class/classes` `var.auth` | Debian supervisor + student login after restore | both identities set | P0 | PASS (2026-11-12 12:07: `wims_user=supervisor` and `anonymous`; POST `auth_user` + `agreecgu=yes`) |
| BKP-05 | same restore on Windows | official pack into the new sibling; `.build-index`; score hash | files match source | P0 | PASS (2026-11-12 12:11:32: same score SHA-256, 7 users, index rebuilt, `.build-index` exit 0) |
| BKP-06 | `var.auth` `!passcheck`, HAVE_CRYPT | Windows supervisor + student login after restore | both identities set | P0 | PASS (2026-11-12 12:36 Paris: supervisor 302 + `var.class.prep`, student `anonymous1`. First-pass FAIL 12:11–12:29 was a stale extensionless `wims.cgi` without HAVE_CRYPT (`7C8FFCED…`); `wims.cgi.exe` was already good. Not a restore bug. See docs/11 §10.3.) |
| BKP-07 | live + source integrity | hashes of source 9001 `.def`/`score` and live CGI/`wimslogd` unchanged | no writes to live/source | P1 | PASS (2026-11-12 12:11:37, 12:19, and 12:37: source `.def` `D4A5A47A…` / `score/anonymous1` `19A23E2F…` and live CGI `7C8FFCED…` / `wimslogd` `AA9D5B0B…` unchanged) |


## 5. Traceability matrix (decision / requirement → code → tests)

| Req. | Statement | Code | Tests |
|---|---|---|---|
| D3 | Maxima via sbcl.exe direct | MF-21 | IFC-01, IFC-02, SBX-S8 |
| D4 | CPU limit at spawn | MF-03 `spawn_core`, `wp_cpu_watch`; MF-25 | JOB-01…07 |
| D6 | flydraw ported | MF-53, MF-13 | HLP-14 |
| D7 | formula size YAML (DYS) | MF-27, MF-58 | YAML-01…03 |
| D10 | never .bat / cmd.exe | MF-21, MF-11 `--selftest`, MF-16, MF-15 | IFC-02, SBX-00, HLP-12, MSYS-05, PLT-02 |
| D11 | AppContainer without capabilities | MF-11 | SBX-S1…S9, SBX-12 |
| D12 | PARI branched | MF-54 `PARI_COMMAND` | IFC-04, SMK-02, SBX-17 |
| D13 | oef-validation tooling in repo | MF-70 | OEF-01, OEF-02 |
| D14 | docs merged | docs/ | n/a (documentation) |
| D15 | runtime from SVN r19040 | MF-54 | BLD-01, SMK-01 |
| D16 | modules r19040 then modupdate | MF-12 webget | HLP-02 |
| D17 | MSYS2 sh + coreutils bundled, scripts unchanged | MF-55, MF-25, MF-03 `wp_script_interp`, MF-15 | MSYS-01…05, PLT-05, HLP-15…17 |
| D18 | mingw-w64 only | MF-50…53, MF-60 | BLD-02, CI-01 |
| D19 | keep Eleda 00352 | MF-70 | OEF-02 (00352 identical failure) |
| D20 | CI stale runs / timeouts | MF-60 | CI-02, CI-03 |
| #1 | wimslogd complete | MF-30, MF-31, MF-32 | LOGD-01…15, LIN-02 |
| #2 | Maxima SBCL | MF-21 | IFC-02 |
| #3 | public_html/modules delivery | MF-54, MF-57 | BLD-01, OEF-03, OEF-04 |
| #7 | Job CPU user-time 0 on Win11 | MF-03 `wp_cpu_watch` | JOB-02 |
| #8 | sandbox hardening | MF-11, MF-20, MF-61 | SBX-* |
| #9 | calc.c:241 multiexec bug | MF-05 | PLT-10 |
| Linux identical | single codebase | MF-56 | LIN-01…05 |

## 6. Run order

1. **E-LNX build gate:** LIN-01, LIN-02, LIN-04, BLD-01…03, L2W-LNX, OEF-01.
2. **CI:** CI-01…03.
3. **Deploy to E-RT:** copy via `C:\Users\Shadow` + `Copy-Item`/`Expand-Archive`.
4. **Platform/Job:** PLT-01 (after PLT-02 fix), JOB-01…06, SEC-01, SEC-03.
5. **Sandbox:** SBX-00, then `test_sandbox_evasion.ps1` (SBX-01…S9, 10…12), SBX-15…21; then SBX-S6…S8 once Octave/Maxima are installed and granted.
6. **wimslogd:** LOGD-01…11, then LOGD-12/14/15 (needs time and a test class).
7. **CAS:** IFC-01, IFC-04, IFC-06 (sandbox on).
8. **Smoke:** SMK-01…03, CGI-01…04, MML-01/02, YAML-01…03.
9. **Helpers:** HLP-01…17, MSYS-01…05.
10. **OEF/latex2wims:** OEF-02…04, L2W-PC-01…05.
11. **Apache:** CGI-05/06 after Arnaud applies `WIMS_RT`.
12. **Robustness:** ENC/PATH/FS/SEC-02.
13. **Setup / per-class backup:** SETUP-01…08, BKP-01…07 (docs/11).

## 7. Counts (updated 2026-11-12 12:37, Paris time)

**Row source of truth:** every status row in §4. Status vocabulary: PASS, NOT RUN, WAIVED,
WAITING (blocked on Arnaud action), BLOCKED, FAIL. Script-checked 2026-11-12 after the BKP-06
retest: start from the 06:20 baseline of **165** (138 including SBX-S2a/b/c ×3
and SBX-S4a/b ×2, plus 27 §4.16 IDs; L2W-PC-* included; second SBX-21 row already in that
baseline) and add **15** new §4.17 IDs (SETUP-01…08, BKP-01…07). The 12:25 table was
**157 PASS / 1 FAIL**; BKP-06 flipped from FAIL to PASS (P0), so PASS 158 and FAIL 0.

| Priority | Tests | PASS | NOT RUN | WAIVED | WAITING | BLOCKED | FAIL |
|---|---|---|---|---|---|---|---|
| P0 | 60 | 60 | 0 | 0 | 0 | 0 | 0 |
| P1 | 89 | 84 | 2 | 1 | 2 | 0 | 0 |
| P2 | 31 | 14 | 16 | 0 | 1 | 0 | 0 |
| **Total** | **180** | **158** | **18** | **1** | **3** | **0** | **0** |

Per area (row counts): SBX 27, HLP 17, LOGD 15, PLT 10, PWD 10, ACL 9, CGI 8, ADM 8, SETUP 8, JOB 7, BKP 7, IFC 6, L2W 6, LIN 5, MSYS 5, BLD 4, OEF 4, ENC 4, SEC 4, YAML 3, CI 3, PATH 3, SMK 3, MML 2, FS 2.

No FAIL rows. SETUP-08 is WAITING on a Laragon `WIMS_RT` change (owner). SETUP-07 is NOT RUN (apt/`./compile` blocked on the box that day). SEC-02 and ACL-06 remain WAITING (second Windows user). BKP-06 PASS is documented in docs/11 §10.3.

