# Handoff: wims4win work by GBot (Migration_Wims) — for Claude

**Document:** `handoff_Wims4Claude_GBOT_v1.md` (v1)  
**Audience:** Claude, the other assistant working on Arnaud Lefrancois’s wims4win project (typically under `Reprise_Wims_Claude` on the lab PC).  
**Author:** GBot (Migration_Wims).  
**Written:** 2026-11-13, early afternoon, Europe/Paris.  
**Language:** English, as requested.

This is a snapshot for coordination. It is **not** a commit, **not** a PR, and **not** permission to merge, cut over Laragon, or change live trees.

## How this document was built

Facts below were checked on 2026-11-13 (Europe/Paris) against:

- Box git checkout `/workspace/wims-port` (origin `https://github.com/io-e/wims4win.git`). At the snapshot, HEAD was `docs/setup-class-backup` @ `b1ef0d2`, in sync with `origin/docs/setup-class-backup`. The only untracked path in that checkout was `P1` (unrelated; not part of the open PRs).
- GitHub `io-e/wims4win`, open pull requests **#10–#16** (all **open**, not draft, none merged). `master` tip: `f65e803` (`docs: LOGD-12 daemon spawn FAIL (&> share); fix proposed, not applied`, 2026-11-09 16:31:57 +0100).
- Lab PC ShadowPC (`SHADOW-TB64BO9R`, machineId `6c8920c9-5fca-47fb-aa1f-37caac205ab2`): folder listings and SHA-256 of CGI / `wimslogd` binaries under `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok`.
- Repo docs on the setup-class-backup branch: `docs/08_Test_Plan_Windows_Migration.md` §7 (updated 2026-11-12 12:37, Paris time) and `docs/Suivi_Developpement_Portage.md`.

GitHub timestamps in `Z` are converted to Europe/Paris (UTC+1 on these dates). Items not verified in this snapshot are marked **unknown**.

---

## 1. Context and standing constraints

**Project.** Native Windows port of WIMS 4.33a (upstream SVN r19040) in the private repo `io-e/wims4win`. Arnaud works in Europe/Paris. GBot’s Windows lab machine is ShadowPC. Deliverables from GBot go under:

- `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok`
- Documentation copies also under `D:\ALF_ZGit\02-wims4win\docs`

**Do not violate these standing rules** (they apply to GBot; Claude should treat them as binding on shared trees and the repo unless Arnaud says otherwise):

| Rule | Detail |
|---|---|
| Leave Claude’s tree alone | **Never modify** `Reprise_Wims_Claude` or Claude’s files. The inverse is requested below: please do not modify `Reprise_Wims_Grok` or GBot sibling runtimes without coordination. |
| PowerShell only | No `.bat` / `cmd.exe`. Windows automation and test scripts are PowerShell. |
| Laragon config | Do **not** edit the live Laragon vhost. Lines may be *given* (docs/11 §8); they are **not applied**. Cutover of `WIMS_RT` remains on hold (SETUP-08 WAITING). Class login now works on a HAVE_CRYPT sibling with aligned CGI names, but the **live** tree still lacks PR #12 and #13. |
| Credentials | Test passwords **only** from `C:\Users\Shadow\wims_test_creds.txt`. Do not commit, paste, or echo secrets. Automation uses `--creds` / `WIMS_CREDS_FILE` or env vars; `creds.example` has no real secrets. |
| Sibling runtimes | New runtimes are **sibling folders**. Ask before overwrite. **Never overwrite live.** |
| GitHub | GBot pushes branches and opens PRs. **Arnaud merges.** No merges, no force-push, no GitHub comments without his approval. |
| Load tests | No load tests and no 200-module runs. ADM-06 is NOT RUN for that reason. |
| SBX-14 | Not run (no `--purge-profiles`; would delete leftover HKCU AppContainer profiles). |
| SEC-02 / ACL-06 | Reserved for Arnaud (needs a second local Windows user). Script: `tests-win_ALF/SEC-02_pipe_acl_steps.ps1`. |
| Issue #9 | The wimsdev / `calc.c:241` report must **not** be sent without Arnaud’s review. |
| Shared box | GBot folders include `/workspace/wims-port`, `/workspace/mw-wims-debian/`, `/workspace/mw-wims-setup-debian/`, `/workspace/mw-wims-setup-debian-incoming/`, `/workspace/wims-port-merge-setup`, and this file under `/workspace/mw-handoff/`. Do not delete box content you did not create. Another GBot line uses `/workspace/grok-g1/`. |
| Verify Linux first | Prefer Debian/box verification before changing the PC. |

A **local, unpushed** merge of PR #12 and PR #13 exists as branch `local/merge-pr12-pr13` and as `/workspace/wims-port-merge-setup`. It is **not** on GitHub and must not be treated as official `master`. Box cross-compile of that merge was blocked on 2026-11-12 by `deb.debian.org` HTTP 500 (no mingw/`make`/`flex` on the box that day).

---

## 2. Pull requests #10–#16

All seven are **open** as of this snapshot. None are merged, none are drafts. Base branch is `master`. Ahead/behind counts are `git rev-list --left-right --count origin/master...origin/<branch>` from the box checkout.

Suggested merge order is in §2.4. Arnaud merges; this is only a dependency note.

### 2.1 PR table

| PR | Title | Branch / head | Unique commits (dates Europe/Paris) | vs `master` `f65e803` | What it changes |
|---|---|---|---|---|---|
| [#10](https://github.com/io-e/wims4win/pull/10) | feat: AlgoGebra JSON grading hook in OEF runtime trees | `feat/algogebra-json-hook` `6c3750c` | Hook work from 2026-11-05 14:15 through 2026-11-06 11:04, including merges of then-`master` (`cba60cb`, `6c3750c`). Opened 2026-11-06 11:05. | **8 ahead / 8 behind** | Vendors `tools/algogebra-hook/` and installs it from `tools/build-runtime-tree.sh` (`algogebra_json.phtml` + `answer.phtml` injection). Opt-out: `ALGOGEBRA_HOOK=0`. Trigger only `cmd=reply&special_parm=algogebra_json` (never `cmd=new`). Fields `replyN` / `choiceN`. Output HTML comments `ALGOGEBRA_JSON_BEGIN`…`END`. Docs: `docs/09_Hook_JSON_AlgoGebra.md`, draft merge-readiness notes. **No C-source change** relative to the `master` it last merged (`0bb1553`). 17 files, +648 lines vs current master (includes later docs drift). |
| [#11](https://github.com/io-e/wims4win/pull/11) | fake_cas: env dump mode for SBX-18 | `feat/fake-cas-env-dump` `f548263` | `f548263` 2026-11-06 14:43. Opened 14:44. | **1 ahead / 6 behind** | `tests/interfaces/fake_cas.c` only: `fake_cas env` prints a sorted `NAME=value` environment dump, for plan 08 **SBX-18** (`ch..root` `env_rm`). Linux self-check done; Windows SBX-18 deploy under `ch..root.exe` was still unchecked in the PR body. |
| [#12](https://github.com/io-e/wims4win/pull/12) | fix(win32): HAVE_CRYPT for class login passcheck | `fix/win32-have-crypt-passcheck` `28b064b` | `28b064b` 2026-11-06 17:09. Opened 17:09. | **1 ahead / 4 behind** | One line of code: `#define HAVE_CRYPT 1` in `src/Platform/win32/config.h`. Without it, `_passcrypt` is a no-op and every class password is rejected. `wp_descrypt.c` was already linked; `crypt()` already declared in `posix_shim.h`. |
| [#13](https://github.com/io-e/wims4win/pull/13) | fix(win32): DuplicateHandle when call_ssh &> shares outf/errf | `fix/win32-wimslogd-shared-redirect` `c1690fa` | `9a981e3` 2026-11-09 16:42 (fix); `936e13a` 16:57 (housekeep PASS); `c1690fa` 2026-11-10 01:22 (modupdate PASS). Opened 2026-11-09 16:44. | **3 ahead / 0 behind** | Windows-only: `wp_spawn` in `src/Platform/wp_win32.c` opens once and `DuplicateHandle`s stderr when `outf==errf`; `open_redirect` also adds `FILE_SHARE_WRITE`; real `GetLastError` is kept across `CloseHandle`. Small `wimslogdlines.c` tweak + docs/08 and Suivi. Verified on sibling `hook-logd-20261109`. Linux not affected (see §3). |
| [#14](https://github.com/io-e/wims4win/pull/14) | docs: English WIMS server administration guide (Windows and Debian) | `docs/admin-guide-win-debian` `15afbfd` | `b582d51` 2026-11-11 10:41; `15afbfd` 2026-11-12 06:19 (PWD/ACL/ADM results). Opened 2026-11-11 10:42. | **2 ahead / 0 behind** | Adds `docs/10_Server_Administration_Windows_Debian.md`. Password management (§3) and rights (§4) come right after the inventory. §17 defines PWD-01…10, ACL-01…11, ADM-01…08. Also updates docs/08, README, Suivi. **Docs only** (no C changes). |
| [#15](https://github.com/io-e/wims4win/pull/15) | docs: setup procedure and per-class backup/restore (Windows and Debian) | `docs/setup-class-backup` `b1ef0d2` | Stacked on #14, then `e14bdde` 2026-11-12 12:22 (docs/11) and `b1ef0d2` 12:38 (BKP-06 root cause + retest PASS). Opened 12:22; updated 12:39. | **4 ahead / 0 behind** | Adds `docs/11_Setup_and_Class_Backup_Windows_Debian.md` (setup + official `pack.sh` class backup/restore + filesystem `tar` fallback). Updates docs/08 §4.17 and Suivi. **Docs only.** The unique commits vs #14 are `e14bdde` and `b1ef0d2`. **Merge #14 first.** |
| [#16](https://github.com/io-e/wims4win/pull/16) | tools: WIMS CGI, curl, and optional Playwright automation | `feat/wims-automation` `cf36e3a` | `cf36e3a` 2026-11-12 12:54. Opened 12:55. | **1 ahead / 0 behind** | Adds `tools/wims-automation/` (CGI-direct `.sh`/`.ps1`, curl HTTP + cookie jar, localhost-only `dev-cgi-server.py` bound to `127.0.0.1` and refusing the live tree name `wims-runtime-r19040-grok-hook-20261106`, optional Playwright, `creds.example`, English README). Commands: `home`, `session`, `supervisor-login`, `student-login`, `open-exercise`, `reply`, `scores`, `scenario`. Windows CGI helper prefers `wims.cgi.exe` (BKP-06 lesson). `adm/raw` is documented as a configured identifier/IP hook and is **not** used here for grading. No load tests. |

GitHub `mergeable` / `mergeable_state` were **not** populated by the list API in this snapshot (**unknown** until a direct PR fetch or a merge attempt).

### 2.2 Files touched (high level)

- **#10:** `tools/algogebra-hook/*`, `tools/build-runtime-tree.sh`, `docs/09_Hook_JSON_AlgoGebra.md`, several `docs/drafts/` and evidence files, docs/README, Suivi.
- **#11:** `tests/interfaces/fake_cas.c`.
- **#12:** `src/Platform/win32/config.h`.
- **#13:** `src/Platform/wp_win32.c`, `src/Wimslogd/wimslogdlines.c`, docs/08, Suivi.
- **#14:** `docs/10_Server_Administration_Windows_Debian.md`, docs/08, README, Suivi.
- **#15:** everything in #14 plus `docs/11_Setup_and_Class_Backup_Windows_Debian.md` and further docs/08 / Suivi updates (BKP-06).
- **#16:** `tools/wims-automation/` only (12 files, +2004 lines). Did **not** bump docs/08 totals.

### 2.3 Test evidence already recorded on those PRs

- **#10:** Linux CGI smoke 00006/00076 × wrong/right + no-leak 8/8; Debian HTTP smoke 8/8; Windows sibling `wims-runtime-r19040-grok-hook-20261106` CGI 8/8 + `ch..root --selftest` + PARI via sandbox. That sibling is also the current **live Laragon** tree (see §4). The PR’s “Laragon cutover” checkbox refers to that earlier hook runtime, **not** to a later cutover to a HAVE_CRYPT / PR #13 tree.
- **#11:** Linux `fake_cas env` self-check done; Windows SBX-18 still open in the PR body (**unknown** whether it was run later; it is not recorded as PASS in the 2026-11-12 §7 table as a new ID — SBX-18 status should be read from docs/08, not assumed).
- **#12:** `tools/check-linux-identical.sh` (calc.c IDENTICAL); cross-built `wims.exe` contains the `Nv\0` salt. Class 9001 supervisor + student CGI login and LOGD-15 were then shown on sibling `hook-class-20261106` (HAVE_CRYPT CGI). **Not** on the live tree.
- **#13:** `tools/build-wimslogd-win32.sh` builds; `make -C tests/platform test` 22/22; sibling `hook-logd-20261109` housekeep + modupdate PASS (non-empty logs, no new `exec.fail`). Live Laragon `wimslogd` was left on the **unfixed** binary (see §4).
- **#14 / #15:** Admin and setup/backup drills on 2026-11-12; BKP-06 first failed then PASS after aligning `wims.cgi` with `wims.cgi.exe` on the setup-test sibling only. See §6.
- **#16:** 2026-11-12, Debian `/workspace/mw-wims-setup-debian` and Windows sibling `wims-runtime-r19040-grok-setup-test-20261112` only: CGI scenario PASS, curl HTTP scenario PASS (localhost wrapper). Playwright on the box: student login, supervisor login, and teacher Notes PASS; the class-9001 `classes/fr` / `exi1` path still rendered the module group intro (**no OEF form**), so reply was not a full exercise submit. Test `wimslogd` stopped by path. Live tree not used.

### 2.4 Merge order and dependencies

Independent of each other relative to current `master` (but some are **behind**):

1. **#12** and **#13** — the two Windows **code** fixes needed before a production-like runtime rebuild. #13 is up to date with `master`. #12 is 4 commits behind (docs-only `master` commits after `859821f`); rebase/merge of `master` into #12 should be straightforward but was **not** done in this snapshot.
2. **#14** then **#15** — #15 is stacked on #14. Merge #14 first. Unique #15 commits: `e14bdde`, `b1ef0d2`.
3. **#16** — independent tools folder, 0 behind `master`.
4. **#11** — independent one-file change, but 6 behind `master`.
5. **#10** — 8 behind current `master`; last merged `master` was `0bb1553`. Expect doc conflicts (Suivi / plan 08 at least). Hook itself is phtml/tools. Do not assume it is merge-ready without a fresh `master` merge and CI.

Do **not** merge the unpushed `local/merge-pr12-pr13` branch.

After **#12 and #13 are on `master`:** build a **new sibling** runtime from that `master`, test it, and only then offer the Laragon `WIMS_RT` lines. Do not overwrite live `hook-20261106`.

---

## 3. Technical findings (please reuse; do not rediscover the hard way)

### 3.1 `open_redirect` sharing violation (PR #13, LOGD-12 daemon path)

`housekeep.daily` / `modupdate.auto` are started from `wimslogd` via `call_ssh(..., "bin/… &>%s/….log")`, so `outf` and `errf` are the **same path**. On Windows, `open_redirect` opened that path twice with `FILE_SHARE_READ` only. The second `CreateFile` failed with **ERROR_SHARING_VIOLATION (32)**. Logs were truncated to 0 bytes; `exec.fail` recorded «error 0» because `GetLastError` was lost across `CloseHandle`.

Fix (Windows only): open once, `DuplicateHandle` for stderr; also allow `FILE_SHARE_WRITE`; preserve the real last error.

**Debian / Linux is not affected.** Linux uses `fork` + `freopen` twice on the same path (`wimslogdlines.c` `#else`). Box reproduction: child exits 0, log written. No `CreateFile` sharing.

The live tree still has the **unfixed** `wimslogd.exe`. The daemon-path failure was reproduced on live `hook-20261106` on 2026-11-09 (PID 5132 at the time). The earlier MSYS `sh` manual run of the scripts remains the reason LOGD-12 was kept PASS in the plan; the overnight daemon path did not add successful runs until the fixed binary on the **sibling**.

### 3.2 `HAVE_CRYPT` / `_passcrypt` (PR #12)

Without `HAVE_CRYPT`, `_passcrypt` compiles as a no-op. Class login `!passcheck` fails; `.log.auth` can show `??????`. Passwords are DES `crypt()`, salt `"Nv"`, via `wp_descrypt.c`. Site manager `log/.wimspass` may start as plaintext and is hashed on first login (`*` present). Class passwords live under `log/classes/<id>/`.

PWD-07: hook-logd and live CGI (pre-HAVE_CRYPT) still reject class logins; `hook-class` (HAVE_CRYPT) accepts them.

### 3.3 Two CGI names must stay aligned

Runtimes can contain both extensionless `public_html\wims.cgi` and `public_html\wims.cgi.exe`. CGI-direct tests that do `Get-ChildItem public_html\wims.cgi* | Select-Object -First 1` pick the **extensionless** file first.

**BKP-06 (2026-11-12):** official class restore was fine. Login failed only because setup-test’s `wims.cgi` was still the pre-HAVE_CRYPT binary, while `wims.cgi.exe` was already good. After copying `wims.cgi.exe` over `wims.cgi` on **that sibling only**, supervisor + student login PASS.

On these lab trees, `public_html\wims.exe` is the **pre-HAVE_CRYPT** binary (same hash as the live CGI). **Never copy `wims.exe` onto `wims.cgi`.** After any binary drop-in, hash and align **both** CGI names.

Known SHA-256 (SHA-256, Windows `Get-FileHash`, 2026-11-13 snapshot; same values as docs/11):

| Binary | Size | SHA-256 | Meaning |
|---|---|---|---|
| pre-HAVE_CRYPT CGI | 933 025 bytes | `7C8FFCEDC7897B25C64E97CADBC544E8F1CC80695897FCC8859D2E06B9252643` | Live hook-20261106 both CGI names; hook-logd both CGI names; also `wims.exe` on these trees |
| HAVE_CRYPT CGI (PR #12) | 933 537 bytes | `42BBAE9D1F571F720B82C35B2A50DB4CFF9934C49F8B5699885CFBDF56D8015C` | hook-class both CGI names; setup-test both CGI names (aligned 2026-11-12 12:36) |
| unfixed `bin\wimslogd.exe` | 543 048 bytes | `AA9D5B0BA61542A5E1FADDADC1E516F45D7714B311E593B7AAEEA7AFB582A4B3` | Live and hook-class |
| PR #13 `bin\wimslogd.exe` | 543 754 bytes | `19E7E2579834F4D30075400B8560AB28D92F72F030B661504B414319C4A3C4E1` | hook-logd and setup-test |

### 3.4 Class backup and restore

Official path: `public_html/modules/adm/class/config/arch/pack.sh`. It produces a `tgz` whose top-level directory is `class/`. After extracting `class/*` into `log/classes/<id>/`, run `log/classes/.build-index <id>`.

UI `download.proc` writes `class_backup=<date>` into the class `.def` (modifies the source). For a read-only source, run `pack.sh` directly (it only reads the class tree) or work on a copy.

Filesystem `tar` fallback has top-level `<id>/` and may include extra files (`pack.sh` does not select all of them). Site-manager `adm/manage/classrestore` is a **different** format (expired classes under `backup/oldclasses/`). Do not confuse the two.

2026-11-12 drill archives (class 9001, source `hook-class-20261106` read-only):

| Archive | Bytes | SHA-256 | Top-level |
|---|---|---|---|
| `docs-admin/backups-20261112/9001-wims-pack-20261112.tgz` | 7 155 000 | `c94b37a68938ae7175685db17b4c6779dcb4015a4cb6b6941d4a7338dfaa408f` | `class/` |
| `docs-admin/backups-20261112/9001-filesystem-20261112.tgz` | 7 173 356 | `afc1a478afbb206a5bcfdf00544ad60f2ebba23fce1801d000fcf3b3f2d89976` | `9001/` |

Copies were also stored under `C:\Users\Shadow\wims-backups-20261112\` and `/workspace/mw-wims-setup-debian-incoming/` (presence of those extra copies was **not** re-listed in this 2026-11-13 snapshot).

### 3.5 Site backup

`bin/backup` is meant to run from `wimslogd` (env `wimslogd`). It writes `backup/WIMSdata.tgz` with daily / weekly / monthly rotation, covering `log` plus local modules, docs and themes. The site manager can also download a backup from the online admin. This is **not** a per-class archive (docs/10 §10, §13; docs/11). ADM-03 (daemon-driven site backup) was **not run** on 2026-11-12.

### 3.6 NTFS ACL and `.wimspass`

`tmp` and `log` may inherit **Authenticated Users (M)** modify rights, so `log\.wimspass` needs an `icacls` lockdown (`/inheritance:r` + grant only the service/user account). ACL-04 PASS on hook-class on 2026-11-12 (open ACEs removed). Cross-user read was **not** tested (that is SEC-02 / ACL-06, for Arnaud). Debian: `chmod 600` / `chown`; group-readable `.wimspass` yields `wims_ismanager=-2` (ACL-03 PASS).

### 3.7 No official init / reset-to-zero procedure

A web and disk search found **no official WIMS procedure** for initializing a site from scratch or resetting it to zero (beyond ordinary install + empty `log`, and the backup tools above). GBot offered to write and test a full-site backup/restore, from-scratch setup, and reset-to-zero procedure (backup first; reset would empty classes, sessions and logs but keep config and modules; tested only on a throwaway sibling and Debian — never live). **Arnaud has not answered that question yet.** Do not invent or run a reset on live.

### 3.8 Automation gap (exi1 / exercise form)

CGI and curl scenarios PASS. Playwright: logins and teacher Notes PASS; opening the class-9001 `classes/fr` / `exi1` path still showed the module **group intro**, not an OEF form, so a full student submit remains untested with that tool.

### 3.9 Already on `master` (context, not in #10–#16)

GBot work already merged (or documented) on current `master` includes, among other items visible in `git log`: Windows `wimslogd` (named pipe instead of AF_UNIX, `--task` + Job Object), `ch..root` AppContainer sandbox and later low-IL policy for listed Cygwin CAS (GAP, Maxima, Octave; `unix()` forbidden like `system`/`dos`), Misc helpers, mkindex / theme asset generation, charset conversion script, runtime r19040 installer, Laragon apache snippet in the repo (not applied from GBot to the live service), and docs/01–08 + Suivi. That history is the baseline the open PRs sit on. This handoff does not re-list every older commit.

---

## 4. PC runtimes (`Reprise_Wims_Grok`)

Root: `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok`.

Hashes below were read on ShadowPC on 2026-11-13 (Europe/Paris). **No `wimslogd.exe` process was running** at that check (pid files may still be stale). Do not start or stop daemons, and do not overwrite these trees, without Arnaud’s go-ahead.

| Folder | Role | CGI (both names, 2026-11-13) | `bin\wimslogd.exe` | Notes |
|---|---|---|---|---|
| `wims-runtime-r19040-grok-hook-20261106` | **Live** lab tree currently associated with Laragon. Created 2026-11-06 10:15. | Pre-HAVE_CRYPT `7C8FFCED…2643` (933 025 B, mtime 2026-11-06 10:10) | Unfixed `AA9D5B0B…A4B3` (543 048 B, mtime 2026-11-06 10:10) | Holds AlgoGebra hook runtime. Class login **fails** (no HAVE_CRYPT). Daemon-path housekeep/modupdate **fails** (no PR #13). Stale `tmp\log\wimslogd.pid` was present (mtime 2026-11-10 01:06). Treat as **read-only** unless Arnaud directs a change. |
| `wims-runtime-r19040-grok-hook-class-20261106` | Sibling for HAVE_CRYPT / class 9001. Created 2026-11-06 15:53. | HAVE_CRYPT `42BBAE9D…D8015C` (933 537 B, mtime 2026-11-06 17:09) | Unfixed `AA9D5B0B…A4B3` | Class supervisor/student login works. Used as **read-only source** for the 2026-11-12 class-9001 backup drill. |
| `wims-runtime-r19040-grok-hook-logd-20261109` | Sibling for PR #13 verify. Created 2026-11-09 16:43. | Pre-HAVE_CRYPT `7C8FFCED…2643` | Fixed `19E7E257…C4E1` (543 754 B, mtime 2026-11-09 16:44) | housekeep/modupdate daemon path PASS (2026-11-09/10). Class login **fails** (no HAVE_CRYPT). No `exec.fail` file present in the 2026-11-13 listing. |
| `wims-runtime-r19040-grok-setup-test-20261112` | Throwaway sibling for setup + class restore + automation. Created 2026-11-12 11:59. | HAVE_CRYPT, **aligned** `42BBAE9D…D8015C` on both names (after 12:36 copy of `.exe` → extensionless) | Fixed `19E7E257…C4E1` | Combines PR #12 CGI + PR #13 `wimslogd` (binaries copied from the siblings above; not a fresh master rebuild). BKP-06 PASS after the CGI align. Used for PR #16 Windows checks. Test daemons were stopped the same day; a pid file still existed on 2026-11-13 (mtime 12:53 on 2026-11-12) with **no** process. |
| `wims-runtime-r19040-grok` | Earlier baseline runtime (late October). | Extensionless `wims.cgi` **absent**; `wims.cgi.exe` hash `8DE3E3E6…0C78160` (933 025 B, mtime 2026-10-28 11:43) — **different** from the later pre-HAVE_CRYPT hook CGI | Older `938EBEC3…30D48F0` (543 048 B, 2026-10-27 17:10) | Not the live tree. Listed only so it is not confused with hook-20261106. |

Other folders in the same root (not runtimes to cut over): `backup-exe-*`, `backup-modules-utf8-20261028`, `backup-runtime3-20261028`, `backup-theme-quickfix-20261028`, `docs`, `docs-admin`, `oef-wims4win`, `rt with space`, `tests-win`, `tests-win_ALF`, `tools`, `wims-automation`, `wims-port`, `wims-port-docs-untracked-backup`, `wims-runtime`, `wims-runtime-oef`, `wims4win`, plus older top-level copies of docs/01–08. Those top-level markdown files are **stale** relative to the repo (October dates).

Debian isolated trees (box, not the PC): `/workspace/mw-wims-debian/` (admin tests) and `/workspace/mw-wims-setup-debian/` (setup + class restore + automation). Do not delete them.

---

## 5. Documentation and other deliverables — locations

**Canonical latest GBot docs for #14/#15** are the files on GitHub branches `docs/admin-guide-win-debian` / `docs/setup-class-backup`, and the copies dated **2026-11-12 12:39** (Europe/Paris) under `D:\ALF_ZGit\02-wims4win\docs`. Sizes there: `08_Test_Plan_Windows_Migration.md` 59 637 bytes; `11_Setup_and_Class_Backup_Windows_Debian.md` 27 916 bytes; `Suivi_Developpement_Portage.md` 51 238 bytes; `10_Server_Administration_Windows_Debian.md` 38 288 bytes (06:19 that day, then 11 updated later).

| Item | Where | Caution |
|---|---|---|
| Admin guide (docs/10) | Repo PR #14; `D:\ALF_ZGit\02-wims4win\docs\10_Server_Administration_Windows_Debian.md`; `Reprise_Wims_Grok\docs-admin\10_…md` | Use these, not older trees. |
| Setup + class backup (docs/11) | Repo PR #15; same two PC folders (`02-wims4win\docs` and `docs-admin`) | Includes BKP-06 §10.3 and CGI alignment procedure. |
| Test plan 08 + Suivi (latest) | PR #15 branch; `D:\ALF_ZGit\02-wims4win\docs\` (12:39) | **`Reprise_Wims_Grok\docs\` is stale** (08 and Suivi dated 2026-11-06 17:15–17:16). **`tests-win_ALF\08_Test_Plan_Windows_Migration.md` is also behind** (54 524 bytes, 2026-11-12 06:19 — admin tests, before BKP-06 / 158 PASS). |
| Hook doc (docs/09) | PR #10 only (not on current `master` or on the setup-class-backup checkout). Present in `D:\ALF_ZGit\02-wims4win\docs\09_Hook_JSON_AlgoGebra.md` (2026-11-06). | |
| Official / GEM source (untracked) | `D:\ALF_ZGit\02-wims4win\docs\Officiel\GEM_Documentation WIMS et Format OEF.md` (and the rest of `Officiel\`) | Referenced by docs/10; do not copy large copyrighted text into new docs. |
| Class 9001 backup archives | `Reprise_Wims_Grok\docs-admin\backups-20261112\` (`9001-wims-pack-20261112.tgz`, `9001-filesystem-20261112.tgz`, pack stdio) | See hashes in §3.4. |
| Automation copy | Repo PR #16 `tools/wims-automation/`; also `Reprise_Wims_Grok\wims-automation\` (copied 2026-11-12 12:54) | Credentials from the creds file / env only. |
| Windows test scripts + results | `Reprise_Wims_Grok\tests-win_ALF\` (scripts, `SEC-02_pipe_acl_steps.ps1`, `results\` including `admin_tests_20261112.tsv` 2026-11-12 06:15, plus older 2026-10-28 / low-IL JSON). | Results folder is **not** a complete dump of the 2026-11-12 12:36 BKP-06 retest; that write-up lives in docs/11 §10.3 and docs/08 §4.17 / §7. |
| Laragon apache snippet in repo | `docs/apache-laragon-wims-runtime.conf`; vhost lines also in docs/11 §8 | **Give the lines, do not apply.** |
| This handoff | Box: `/workspace/mw-handoff/handoff_Wims4Claude_GBOT_v1.md`. PC copies: `D:\ALF_ZGit\02-wims4win\docs\handoff_Wims4Claude_GBOT_v1.md` and `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\handoff_Wims4Claude_GBOT_v1.md` | **Not** committed or pushed. |

`docs/07_Handoffs.md` in the repo is an older handoff log (on `master`); it does not replace this v1 snapshot.

---

## 6. Test status (plan 08, 2026-11-12 12:37 Europe/Paris)

Source of truth: every status row in docs/08 §4, summarised in §7. Script-checked after the BKP-06 retest.

**158 / 180 PASS, 0 FAIL, 18 NOT RUN, 1 WAIVED, 3 WAITING, 0 BLOCKED.**  
**P0: 60 / 60 PASS.**

| Priority | Tests | PASS | NOT RUN | WAIVED | WAITING | BLOCKED | FAIL |
|---|---|---|---|---|---|---|---|
| P0 | 60 | 60 | 0 | 0 | 0 | 0 | 0 |
| P1 | 89 | 84 | 2 | 1 | 2 | 0 | 0 |
| P2 | 31 | 14 | 16 | 0 | 1 | 0 | 0 |
| **Total** | **180** | **158** | **18** | **1** | **3** | **0** | **0** |

**WAITING (blocked on Arnaud):**

- **SEC-02** (P1) — named-pipe ACL; needs a second local Windows user. Script: `tests-win_ALF/SEC-02_pipe_acl_steps.ps1`.
- **ACL-06** (P1) — maps to SEC-02; reserved for Arnaud.
- **SETUP-08** (P2) — Laragon `WIMS_RT` cutover. Lines are in docs/11 §8; **not applied**.

**WAIVED:** **LIN-03** (P1), 2026-11-06, Arnaud: identity not expected for the `mathml.c` YAML reader; behaviour covered by YAML-02.

**NOT RUN (18), as recorded in docs/08** (do not “just run” the destructive ones):

- P1: **ACL-05** (no root / `setwrapexec` on the box), **ADM-08** (no production package upgrade).
- P2: **PLT-06, PLT-07, PLT-08, JOB-07, SBX-14** (purge; do not run), **LOGD-14, CGI-03, HLP-12, CI-03, FS-02, PWD-10, ACL-11, ADM-03, ADM-06** (no load tests), **ADM-07, SETUP-07** (`deb.debian.org` HTTP 500 that day; `./compile` / apt toolchain missing).

SETUP-07 remains desirable when Debian mirrors and the box toolchain work. PR #16’s Playwright exercise-submit gap is **not** a new plan-08 ID.

Live runtime still needs #12 and #13 merged and a **new** rebuilt sibling before any cutover discussion.

---

## 7. Open items / next steps

1. **Arnaud reviews and merges PRs #10–#16**, in an order that respects §2.4 (#14 before #15; refresh #10 / #11 / #12 onto current `master` as needed). CI on #10 had an earlier apt timeout then a green rerun; **current** CI status of each PR was **not** re-checked in this snapshot.
2. **After #12 and #13 are on `master`:** cross-build and deliver a **new sibling** runtime (do not overwrite live). Re-hash and align **both** CGI names. Re-test class 9001 login and wimslogd daily jobs on that sibling. Only then **give** the Laragon `WIMS_RT` / `ScriptAlias` lines (docs/11 §8); do not apply them until Arnaud asks. SETUP-08 stays WAITING until he does.
3. **SEC-02 / ACL-06:** Arnaud runs them with a second Windows user.
4. **SETUP-07:** retry Debian from-source / toolchain when mirrors work.
5. **Automation:** optional follow-up for a real OEF exercise form (the `exi1` group-intro gap). Not required for merge of #16, but a full student submit remains untested in Playwright.
6. **Full-site backup / initialize-from-scratch / reset-to-zero procedure:** offered by GBot; **waiting for Arnaud’s yes/no**. If yes: backup first; test only on a new throwaway sibling and a Debian box folder; never on live; no Laragon change; creds only from the creds file.
7. **Issue [#9](https://github.com/io-e/wims4win/issues/9)** (open since 2026-10-23): report the `calc.c:241` multiexec timeout bug to wimsdev, without changing the default Linux path. A draft exists on branch `docs/calc-c-241-bug-report` as `docs/Bug_Report_calc_c_241_CLAUDE_V1.md` (filename suggests Claude authorship; **not claimed as GBot work**). **Do not send the report without Arnaud’s review.** That branch also has `docs/CPU_UserTime_Zero_Win11_CLAUDE_V1.md`. There was **no** open PR for that branch in the 2026-11-13 open-PR list.
8. Optional screenshots / images for the admin guide were offered earlier; **no answer recorded**.
9. This handoff is **not** in git. If it should be added to the repo, that is Arnaud’s decision (and would be a new docs commit / PR).

---

## 8. Suggested division of work — and what Claude should not touch

These are coordination suggestions, not assignments from Arnaud.

### Please do not

- Modify or delete **`Reprise_Wims_Grok`** sibling runtimes (especially live `wims-runtime-r19040-grok-hook-20261106`), `docs-admin\backups-20261112`, or the GBot copies of `wims-automation`, unless Arnaud asks and a backup exists.
- Overwrite live binaries, or copy `wims.exe` onto `wims.cgi`.
- Change **Laragon** / Apache config, or point `WIMS_RT` at another tree.
- Merge, rebase-push, force-push, or comment on GitHub PRs/issues without Arnaud’s approval.
- Commit or paste secrets, or read the creds file into a ticket / chat / commit.
- Run **SBX-14** (`--purge-profiles`), load tests, or a 200-module sweep.
- Send the **#9** wimsdev report.
- Delete shared-box folders listed in §1 (including `/workspace/mw-wims-*` and `/workspace/wims-port-merge-setup`).
- Treat `local/merge-pr12-pr13` as official `master`.
- Silently push new commits onto GBot’s open branches (`feat/algogebra-json-hook`, `feat/fake-cas-env-dump`, `fix/win32-have-crypt-passcheck`, `fix/win32-wimslogd-shared-redirect`, `docs/admin-guide-win-debian`, `docs/setup-class-backup`, `feat/wims-automation`) — that would tangle reviews. Coordinate first.
- Use `Reprise_Wims_Grok\docs\` (2026-11-06) or the 06:19 `tests-win_ALF` copy of plan 08 as if they were the latest counts (they are not; see §5).

### Natural split

- **Claude’s usual tree** `Reprise_Wims_Claude` remains Claude’s area; GBot will not modify it.
- **Arnaud:** review/merge #10–#16; SEC-02 / ACL-06; whether to authorise a full-site backup/init/reset write-up; review of the #9 report; any Laragon cutover.
- **GBot (if asked after merges):** new sibling runtime from `master` (with #12+#13), post-merge tests, Laragon *lines* only, SETUP-07 when the toolchain works, optional init/reset procedure if approved, follow-up on the Playwright exercise-form gap.

If both assistants must edit the same repo files, prefer a new branch and a short coordination note rather than both committing to an already-open PR.

---

## Document history

| Version | Date (Europe/Paris) | Notes |
|---|---|---|
| v1 | 2026-11-13 | First handoff of GBot dev and docs, focused on open PRs #10–#16, lab runtimes, and current test counts. Not committed. |
