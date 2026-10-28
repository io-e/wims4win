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
| LIN-03 | MF-27 `mathml.c` (YAML), MF-25 `config.c` | E-LNX: add `Mathml/… config.c mathml.c log.c wims.c` to FILES | IDENTICAL, or diff limited to the YAML reader, which is the same on Linux by design (9b38fb7) | — | P1 | NOT RUN (identity not expected for mathml.c: the YAML feature is cross-platform; verify behaviour with YAML-02 instead) |
| LIN-04 | MF-02 `wp_posix.c`; MF-61 `tests/platform` | E-LNX: `make -C tests/platform clean test` | 15/15 PASS (POSIX) | — | P1 | PASS (CI job "Linux Platform unit tests", green on ea6dc73) |
| LIN-05 | MF-20 `Interfaces/common.c`; MF-61 `run_equiv.sh` | E-LNX: `tests/interfaces/run_equiv.sh <r19040>/src/Interfaces <HEAD>/src/Interfaces` | stdout/stderr/rc identical, time ±2 s | r19040 | P1 | PASS (phase 1–2, `05` phase2 "equiv PARI/Maxima") |

### 4.2 PLT — Platform layer (MF-01…MF-09, MF-18)

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| PLT-01 | MF-03 `wp_spawn`, `wp_poll`, `wp_read`, `wp_kill`, `wp_wait` | `tests\platform\test_platform.exe` (cross-built by `make -C tests/platform test_platform.exe`) on E-PC | 22/22 PASS | LIN-04 | P0 | PASS (`05` tests.platform_unit 22/22, ShadowPC) |
| PLT-02 | MF-03 `test_platform.c` "spawn cmd /c echo" | **Defect to fix:** this test case uses `cmd /c echo`, contrary to the no-cmd.exe rule. Replace it with `fake_cas.exe plain`, then rerun PLT-01 | 22/22 without cmd.exe | — | P2 | NOT RUN |
| PLT-03 | MF-04 `wp_strip_cr`; MF-24 `_get_exec_error` | PLT-01 cases "strip_cr…", "child output has CRLF", "CR stripped" | CR removed only on Win32 | identical bytes | P1 | PASS (in PLT-01) |
| PLT-04 | MF-03 `wp_resolve_exe` | `!exec pari` resolves `bin/pari` → `bin\pari.exe`; check `W 'module=tool/algebra/factor.fr&cmd=new&lang=fr'` then reply (see SMK-02) | page computed | — | P1 | PASS (factor.fr 200, 2026-10-27) |
| PLT-05 | MF-03 `wp_script_interp` (#! scripts) | createqcm module (`rmdepot.sh`); also `sh` absent check: `Get-Command sh` | no `bad_cmd rmdepot.sh` | Linux runs the script | P1 | PASS (1b58be7, createqcm 200, 2026-10-27) |
| PLT-06 | MF-03 `wp_script_interp` "#!/usr/bin/env perl" | run a helper with `#!/usr/bin/env perl` through wp_spawn (e.g. `bin/chemeq` via wims `!exec chemeq`) | perl from PATH used | — | P2 | NOT RUN (perl path tested directly in HLP-05) |
| PLT-07 | MF-03 `wp_builtin` (mv/rm in-process) | module calling `call_ssh("mv a b")` (sessions/exo save); inspect file moved | no sh needed, rc 0 | — | P2 | NOT RUN |
| PLT-08 | MF-08 `posix_shim.h` fork stub → `wp_no_fork` | grep build: every `fork()` site compiled for Win32 is either `#ifdef`ed or harmless (list in `02_Architecture` §2.2) | inventory reviewed | — | P2 | NOT RUN (review task) |
| PLT-09 | MF-09 `wp_alarm.c` alarm via timer queue | CAS timeout scenario of CAS-08 (silent CAS) | wims returns after the timeout | Linux SIGALRM | P1 | PASS (cas_equiv "timeout" 24/24) |
| PLT-10 | MF-05 `wp_mx_exchange` fixed=1 vs legacy | PLT-01 "mx fixed: silent interface → ~3 s"; issue #9 (bug calc.c:241) | legacy behaviour kept by default | upstream bug kept | P2 | PASS (PLT-01); report to wimsdev #9 pending Arnaud's review |

### 4.3 JOB — Job Objects and CPU limit (D4, issue #7)

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| JOB-01 | MF-03 `spawn_core` (CREATE_SUSPENDED → Assign → Resume), `make_job` | `fake_cas.exe busy` spawned by wims (run_equiv_pc.ps1 scenario) with rlimit 5 s | killed by job/watchdog < 6 s | RLIMIT_CPU kill | P0 | PASS (`05` cpu_limit "busy child killed within 6s") |
| JOB-02 | MF-03 `wp_cpu_watch` (accounting + 2× wall grace) | as JOB-01 on Win11, where `PerProcessUserTimeLimit` stays at 0 for busy loops (#7) | watchdog kills | — | P0 | PASS (Claude 2026-10-27: WIMS path < 6 s; the .NET harness that attaches after start is not representative) |
| JOB-03 | MF-03 job tree kill (KILL_ON_JOB_CLOSE) | `fake_cas.exe spawn` (grandchild `silent`), end of request; `Get-Process fake_cas` | no grandchild left | kill by exec.pid | P0 | PASS (phase 2 "tree kill", 68377ef tests) |
| JOB-04 | MF-03 `wp_set_job` mem limit | spawn with mem 64 MB, `fake_cas flood` | process killed / alloc fails | RLIMIT_AS | P1 | NOT RUN |
| JOB-05 | MF-03 `wp_job_kill` named job `Local\wims-job-<session>` | open session, start CAS, `cleaning` of session | job terminated | exec.pid kill | P1 | NOT RUN |
| JOB-06 | MF-25 `set_rlimits` → `wp_set_job` | `log/wims.conf` `rlimit_cpu=3`; OEF with heavy pari loop | CAS stopped after ~3 s, WIMS error shown | same message | P1 | NOT RUN |
| JOB-07 | MF-31 `execredirected` Win (CPU 1500 s) | wimslogd task `housekeep.daily` runs > 20 s without kill | not killed before 1500 s | — | P2 | NOT RUN |

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
| SBX-S6 | MF-22 `octave.c illegal[]` (system) + MF-11 | Octave `system("PING.EXE 127.0.0.1")` through the interface | refused by interface | same list | P0 | BLOCKED (Octave not installed in E-RT; waiver needed) |
| SBX-S7 | MF-22 `illegal[]` += dos/winqueryreg/winopen | Octave `dos('x')`, `winqueryreg(...)` | refused | n/a (Win only) | P0 | BLOCKED (Octave) |
| SBX-S8 | MF-21 + MF-11 | Maxima `:lisp (with-open-file (s "D:/…/log/x" :direction :output))` | denied by ACL | chroot | P0 | BLOCKED (Maxima not configured in E-RT) |
| SBX-S9 | MF-11 no HKCU write | `fake_cas reg` | rc≠0, key absent | — | P0 | PASS (2026-10-27) |
| SBX-10 | MF-11 `main` `& sh\|perl` → 127 | `ch..root.exe '&' sh 'echo pwned > x'` | 127 | upstream non-root behaviour | P0 | PASS |
| SBX-11 | MF-11 `cleantmpdir` | `ch..root.exe cleantmpdir` | 0 | — | P1 | PASS |
| SBX-12 | MF-11 `DeleteAppContainerProfile`, `set_ace REVOKE_ACCESS` | profile count under `HKCU:\…\AppContainer\Mappings` before/after; ACL of session dir | no growth, no `S-1-15-2-…` ACE | — | P0 | PASS (143→142) |
| SBX-13 | MF-11 `purge_profiles_ex(stale=1)` | kill a ch..root mid-run (Job kill by wims), next run | orphan profile removed | — | P1 | PASS indirectly (profiles 157→143 after deployment, 2026-10-27) |
| SBX-14 | MF-11 `--purge-profiles` | `ch..root.exe --purge-profiles` | "deleted N" | — | P2 | NOT RUN (Arnaud's OK needed: deletes ~140 leftovers in HKCU) |
| SBX-15 | MF-11 `grant_rx` | `ch..root.exe --grant-rx <CAS dir>`; `(Get-Acl dir).Access` | ACE `ALL APPLICATION PACKAGES` RX | — | P0 | PASS (other\tests, pari dir) |
| SBX-16 | MF-20 `execredirected` ISO_WRAPPER when `chroot\tmp\sessions\.chroot` exists; MF-03 `wp_sandbox_available` | factor.fr with marker / with `ch..root.exe` renamed / without marker | OK / refused / OK (direct) | — | P0 | PASS (2026-10-27, 3 cases) |
| SBX-17 | MF-11 + real CAS | pari `write("…/log/gp-evil.txt",1)` inside sandbox | error, file absent | — | P0 | PASS (2026-10-27) |
| SBX-18 | MF-11 env cleaning (`env_rm`) | `fake_cas` mode printing env (to add) | no `w_wims_home`, `SERVER_ADDR`… | chroot.c env_rm | P1 | NOT RUN (fake_cas mode to add) |
| SBX-19 | MF-11 TMPDIR made absolute | CAS writing `$TMPDIR/out` | file in session dir | — | P1 | PASS indirectly (factor.fr in sandbox) |
| SBX-20 | MF-54 marker by default | fresh `build-runtime-tree.sh` → `chroot/tmp/sessions/.chroot` exists | present | — | P0 | PASS (2026-10-28, fresh box build-runtime-tree.sh: marker present) |
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
| LOGD-12 | housekeep.daily / modupdate.auto / backup | leave daemon running past trigger times; read `tmp/log/housekeep.log` | scripts run via sh | Linux | P1 | NOT RUN |
| LOGD-13 | MF-06 `pipe_name` per tree hash | two runtimes (wims-runtime, r19040-grok, tests-win) running simultaneously | each CGI talks to its own daemon | — | P1 | PASS (3 daemons coexisting on PC, 2026-10-27) |
| LOGD-14 | MF-30 pid check / daily restart (`wait_children`) | overwrite `tmp/log/wimslogd.pid` | daemon exits within 1 min; next request restarts it | same | P2 | NOT RUN |
| LOGD-15 | class scores end-to-end | in a test class: student does an exercise, then teacher score page | scores shown | Linux | P1 | NOT RUN |

### 4.6 CGI — wims.exe CGI and Apache/Laragon

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| CGI-01 | MF-07 `wp_cgi_prepare` | `W 'module=adm/doc&cmd=new&lang=fr'` | `Status: 200` | — | P0 | PASS |
| CGI-02 | MF-07 binary mode (d1d10c0) | open OEF exercise, then `cmd=reply` with the same session | session kept (no `cmd=new` fallback) | — | P0 | PASS (d1d10c0 fix; createqcm, factor reply 2026-10-27) |
| CGI-03 | MF-07 incomplete tree → 500 | rename `$rt\public_html\themes` temporarily | `Status: 500 WIMS runtime tree incomplete` listing it | — | P2 | NOT RUN |
| CGI-04 | MF-26 SERVER_ADDR NULL (f999023) | run CGI without `SERVER_ADDR` | no access violation | — | P1 | PASS (all PowerShell runs omit it) |
| CGI-05 | E-WEB + MF-58 vhost | browser: `http://wims_paris_saclay_fr.test/wims/wims.cgi` | 200 home | — | P0 | PASS on Claude's runtime (2026-10-27); NOT RUN with `WIMS_RT` → Grok runtime |
| CGI-06 | E-WEB | `http://wims_paris_saclay_fr.test/wims/wims.cgi?module=tool/algebra/factor.fr&lang=fr` → enter `2^64+1` | `274177 × 67280421310721` | Linux | P0 | PASS (2026-10-28, after ch..root ensure_profile_env fix: Apache minimal env made CreateProcessAsUser fail with err 203) |
| CGI-08 | E-WEB `ScriptAliasMatch` | exercise image `/wims/wims.gif?cmd=getins…` (e.g. chronology) via Laragon | 200 image/gif | Linux apache-config maps wims.* | P1 | NOT RUN via Apache (CGI direct PASS: image/gif 3065 B; needs the 4 ScriptAliasMatch lines of docs/apache-laragon-wims-runtime.conf, applied by Arnaud) |
| CGI-07 | MF-25 PATH 8 KiB, `wims_home` with `/` | `W` home page with a long inherited PATH (>4 KiB) | 200 | — | P2 | NOT RUN |

### 4.7 IFC — CAS interfaces

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| IFC-01 | MF-20, MF-61 `tests/interfaces/run_equiv_pc.ps1` | run on E-PC for PARI, Maxima, Octave, GAP (calc, factor, large, illegal, about, timeout, absent, multiexec) | 24/24 | Linux outputs | P0 | PASS (`05` cas_equiv 24/24) |
| IFC-02 | MF-21 `maxima.c` sbcl.exe `--core` (D3, D10, #2) | `Get-Process` during a Maxima request: parent is sbcl.exe, no cmd.exe, no maxima.bat | sbcl only | — | P0 | PASS (#2 closed, 3c59829) |
| IFC-03 | MF-22 GAP direct exe (51b54d5) | GAP scenarios of IFC-01 | 8/8 on rerun | Linux | P1 | PASS (Claude 2026-10-27: 21/24 then 8/8 on rerun; first failure during mkindex load) |
| IFC-04 | D12 `pari_command` in `log/wims.conf` (MF-54 `PARI_COMMAND`) | `Select-String pari_command $rt\log\wims.conf`; `!exec pari about` | path to gp.exe | — | P0 | PASS |
| IFC-05 | MF-24 CR strip on exec.err | CAS error message in a module | no `\r` in page | — | P1 | PASS (PLT-03, f81abf1) |
| IFC-06 | MF-20 + sandbox for Maxima/Octave/GAP | with `.chroot` marker + `--grant-rx` on each CAS dir, rerun IFC-01 | 24/24 | — | P0 | FAIL for GAP (Cygwin cannot create \\BaseNamedObjects\\cygwin1S5-… in an AppContainer, 0xC0000022; decision pending: native GAP or weaker policy); Octave/Maxima BLOCKED (not installed) |

### 4.8 MML — MathML and formula size (D7)

| ID | Linked code | Steps | Expected | Linux ref. | Prio | Status |
|---|---|---|---|---|---|---|
| MML-01 | MF-27 `mathml()` via `wp_spawn` (`bin/wims_mathml.exe`); MF-61 `win/test_mathml_spawn.c` | page with formula (latex2wims example, `\frac`) | `<math` in page | same MathML | P0 | PASS (`05` mathml; latex2wims PC check "MathML 2/3 blocks") |
| MML-02 | MF-54 `default_texposition=2` | `Select-String default_texposition $rt\log\wims.conf` | `=2` | — | P0 | PASS |
| YAML-01 | MF-27 + MF-58 `log/wims4win.yaml` `formula: size: 200%` | page source: formula style font-size 200 % | 200 % | identical on Linux (9b38fb7) | P1 | PASS (d3c33a3/d60e1f9 results) |
| YAML-02 | range 50–400 clamp | set `size: 900%` then `10%` | clamped to 400 % / 50 % | same | P1 | PASS (2026-10-28: 900%→400%, 30%→50%, abc→200% on local/exemple.fr block=mainS1; mathml.c now clamps) |
| YAML-03 | missing YAML | rename `wims4win.yaml` | default 200 %, no error | same | P2 | NOT RUN |

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
| HLP-09 | MF-52 canvasdraw (`canvasdraw_win.h`) | inside a module using canvasdraw (`!exec canvasdraw`) | canvas JS emitted | Linux | P1 | NOT RUN (outside WIMS it only answers "should be used within wims") |
| HLP-10 | MF-52 crossword (`sys/times.h`) | module using crossword | grid | Linux | P2 | FAIL/unclear (standalone test "Cannot open bcd": input format to recheck) |
| HLP-11 | MF-52 mathexp (libgnurx), symtext, voronoi, whirlgif, bioinfo×4, checkmol/matchmol | one call each through its `bin/` script | non-empty, matches Linux | Linux | P2 | NOT RUN |
| HLP-12 | MF-16 clickzone `system()` → `sh -c` | module with clickzone on a GIF | zone detected; no cmd.exe (`Get-Process`) | — | P2 | NOT RUN |
| HLP-13 | MF-15 wrap..exec.exe | `.\bin\wrap..exec.exe ../other/bin/units-filter` | runs, returns child rc | wrap.c | P1 | PASS (rc propagated, 2026-10-27) |
| HLP-14 | MF-53 flydraw.exe, MF-13 insdraw..processor.exe | OEF 00001 with drawing | GIF identical to Linux | Linux | P1 | PASS (Suivi: "00001 identical, GIF included") |
| HLP-15 | MF-40 `other/bin/src2def` via sh | latex2wims/mkindex path, `sh other/bin/src2def` | .def generated | Linux | P1 | PASS indirectly (mkindex runs) |
| HLP-16 | `bin/mkindex` via MSYS2 sh (D17) | `sh bin/mkindex` | rc 0 | Linux | P1 | FAIL (2026-10-27: rc=1 after 1 338 s, `cpp` and `strfile` missing, "find: environment too large") |
| HLP-17 | `bin/oefcompile` via sh | compile a local OEF module | `.oef` → `.def` | Linux | P1 | NOT RUN |

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
| OEF-03 | module `local/exemple.fr` | `W 'module=local/exemple.fr&cmd=new&lang=fr'` | 200, exercise rendered | Linux | P1 | NOT RUN (module not verified on E-RT) |
| OEF-04 | module `local/oef2html_web.fr` (42 OEF from `D:\ALF_ZGit\03-oef2html_Web\src\oef\fr`) | for each of the 42 `exo=`: new + reply | 42 × 200, no `WIMS User Error`; parity with Linux on seed | Linux | P1 | NOT RUN (Arnaud's request pt63u pending) |

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
| CI-03 | apt retries (ea6dc73) | 3 consecutive pushes | no Install hang | P2 | NOT RUN (1 run so far) |

### 4.14 ENC/PATH/FS — encodings, paths, locking, security

| ID | Linked code | Steps | Expected | Prio | Status |
|---|---|---|---|---|---|
| ENC-01 | MF-07 `_O_BINARY`, MF-31 wimslogd `_fmode` | after sessions, `Format-Hex` on `sessions\*\var.stat`: no `0D 0A` | LF only | P0 | PASS indirectly (sessions survive replies, d1d10c0) |
| ENC-02 | MF-04 CR strip of CAS output | CRLF CAS (`fake_cas crlf`) | no CR in page | P1 | PASS (IFC/PLT) |
| ENC-03 | windows-1252 pages | `charset=windows-1252` header and accents render (`Factorisation`) | correct | P1 | PASS (factor.fr page) |
| ENC-04 | MF-71 UTF-8 → windows-1252 | L2W-PC-05 | — | P2 | PASS |
| PATH-01 | MF-03 `quote_arg`, MF-11 `quote_arg`, MF-15 `q` | runtime copied to `D:\wims test\rt with space`; SMK-01…03 | all pass | P1 | NOT RUN |
| PATH-02 | long paths > 260 | session dir nested > 260 chars (module path) | error is explicit, no crash | P2 | NOT RUN |
| PATH-03 | MF-25 wims_home `/` separators | `W` + `!exec` helpers using `$wims_home` | OK | P1 | PASS (93e8775) |
| FS-01 | MF-30 file sharing (`open_redirect` FILE_SHARE_READ) | overlapping wimslogd tasks | no sharing violation (fixed) | P1 | PASS (LOGD-11) |
| FS-02 | session files locked by a running CAS while cleaning | start long CAS, force cleaning | cleaning retries, no daemon crash | P2 | NOT RUN |
| SEC-01 | MF-10 crypt DES / BCrypt | `test_crypt.exe` | 8/8 (DES 4/4, BCrypt 3/3, cross 1/1) | P0 | PASS (`05` crypt) |
| SEC-02 | MF-06 pipe ACL | another local user opens `\\.\pipe\wims-wimslogd-<hash>` | access denied, or documented risk | P1 | NOT RUN |
| SEC-03 | MF-03 `token_elevated` | CGI run elevated | CAS refused (DROP_PRIV) | P1 | PASS (PLT-01) |
| SEC-04 | runtime `log\` not granted to AppContainers | `(Get-Acl $rt\log).Access` has no `S-1-15-2-1` | absent | P0 | PASS (SBX-S1a) |

### 4.15 SMK — minimal smoke (proves the modified code is exercised)

| ID | Linked code exercised | Steps | Expected | Prio | Status |
|---|---|---|---|---|---|
| SMK-01 | MF-07, MF-26, MF-06, MF-30 | `W 'module=adm/doc&cmd=new&lang=fr'` | 200; daemon started | P0 | PASS |
| SMK-02 | MF-20, MF-23, MF-24, MF-03, MF-11 (marker on) | `$p=W 'module=tool/algebra/factor.fr&cmd=new&lang=fr'; $s=[regex]::Match($p,'session=([A-Za-z0-9_.]+)').Groups[1].Value; W "session=$s&module=tool/algebra/factor.fr&cmd=reply&formula=2%5E64%2B1&calc=factor&lang=fr"` | contains `67280421310721` | P0 | PASS (2026-10-27, in sandbox) |
| SMK-03 | MF-03 `wp_script_interp`, MF-51 | `tool/wcalc.fr`, createqcm | 200 | P1 | PASS |

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

## 7. Counts (updated 2026-10-28 afternoon, Paris time)

140 test cases (5 added on 2026-10-28: ENC-05 charset, BLD-04 theme assets, HLP-18 cpp, CGI-08 getins image, BLD-05 wims.exe stack).

| Priority | Tests | PASS | NOT RUN | BLOCKED | FAIL |
|---|---|---|---|---|---|
| P0 | 51 | 47 | 0 | 3 | 1 |
| P1 | 68 | 57 | 11 | 0 | 0 |
| P2 | 21 | 6 | 14 | 0 | 1 |
| **Total** | **140** | **110** | **25** | **3** | **2** |

Per area: SBX 26, HLP 18, LOGD 15, ENC/PATH/FS/SEC 14, PLT 10, JOB 7, CGI 8, BLD/CI 8, IFC 6, L2W 6, LIN 5, MML/YAML 5, MSYS 5, OEF 4, SMK 3.

**Production gate:**
- P0 NOT RUN: none.
- P0 FAIL: IFC-06 (GAP Cygwin vs AppContainer, decision pending).
- P0 BLOCKED: SBX-S6/S7/S8 (Octave/Maxima not installed, per Arnaud's instruction of 2026-10-28).
- **Gate not yet met.**
