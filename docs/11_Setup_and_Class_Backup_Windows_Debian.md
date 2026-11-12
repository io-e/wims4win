# 11 — Setup and per-class backup / restore (Windows and Debian)

**Language:** English (procedure).  
**Date of this run:** 2026-11-12, Europe/Paris (CET, UTC+1).  
**Branch:** `docs/setup-class-backup`.  
**Companion documents:** [docs/04](04_Guide_Installation_et_CGI_Laragon.md) (Windows install / Laragon), [docs/10](10_Server_Administration_Windows_Debian.md) (passwords, rights, `wimslogd`, site-level backup). This document is the step-by-step **from-scratch setup** plus a **per-class** backup and restore drill. It does not replace docs/10.

Passwords and other secrets are never written in this file. Use a local credentials file (on the lab PC: `C:\Users\Shadow\wims_test_creds.txt`) and never commit it.

---

## 1. Purpose and scope

This procedure tells an operator how to:

1. Set up a **new** WIMS 4.33a (SVN r19040) instance on **native Windows (wims4win)** and on **Debian 13**.
2. Back up **one class** (example: `9001`) with WIMS’s own archive (`adm/class/config` → `arch/pack.sh`) and, as a fallback, a filesystem archive of `log/classes/<id>/`.
3. Restore that class into a **freshly set-up** instance and verify that the class exists, supervisor and student logins work, and scores / Notes are present.

**In scope for an agent or operator on the lab PC:** new sibling runtimes under `Reprise_Wims_Grok\`, Debian trees in a **new** folder, PowerShell only on Windows (no `.bat` / `cmd.exe`).  
**Out of scope unless the owner asks:** changing Laragon config, touching the live runtime `wims-runtime-r19040-grok-hook-20261106`, touching `Reprise_Wims_Claude`, merging unmerged fix PRs to `master`, load tests, SBX-14, SEC-02.

Until [PR #12](https://github.com/io-e/wims4win/pull/12) (`HAVE_CRYPT`) and [PR #13](https://github.com/io-e/wims4win/pull/13) (`wimslogd` `DuplicateHandle` when stdout and stderr share a log) are merged, a Windows instance that must accept class passwords **and** run daily jobs has to be built from a **local merge** of those branches (do not push the merge to `master`), or assembled from binaries already built from those branches.

---

## 2. What already exists (do not reinvent)

| Source | What it covers |
|---|---|
| Official wiki, Debian auto-install | <https://wiki.wimsedu.info/doku.php?id=installation:debian:auto> — `apt-get install wims`. **To verify** the package version you actually get on Debian 13. |
| Official wiki, manual / chroot | `./compile --mathjax --jmol --modules --geogebra` (plus `--build-chroot` when used), then root `setwrapexec`, optional `setchroot`, `apache-config`. |
| Official “maintenance → Backup” | Site-level `WIMSdata-<date>.tgz` and `bin/backup` (daemon path). That is **not** a per-class archive. See docs/10 §10 and §13. |
| docs/04 | Windows Laragon, CAS, CGI vhost. |
| docs/10 §3–§4, §9 | Passwords, rights, `wimslogd` start/stop. |
| This document | End-to-end setup + **per-class** backup/restore, with a run recorded in §10. |

WIMS’s **per-class** mechanism (source of truth in the tree):

- Supervisor UI: module `adm/class/config`, `job=arch`, `job2=download`, `selection=all`, `format=zip|tgz|tar.gz` (`arch.phtml`, `arch/download.proc`).
- Underlying script: `public_html/modules/adm/class/config/arch/pack.sh`. It copies the selected class directories (see `arch/recdir`) into a session `getfile/class/` folder, writes `serial`, and builds `class.zip` or `class.tgz` / `class.tar.gz`, then renames the archive to `<classid>.<format>` (slashes in the class id become `~`).
- Restore in an **existing** class: upload the archive (`arch/upload.proc` + `scripts/adm/decompress.sh`), then `arch/restore.sh` copies the chosen record types into `log/classes/<id>/`.
- Site-manager `adm/manage/classrestore` is a **different** format (`backup/oldclasses/<year>/<id>.tgz` produced when classes expire). Do not confuse it with the supervisor `pack.sh` archive.
- `download.proc` writes `class_backup=<date>` into the class `.def` after a successful UI backup. **That modifies the source class.** For a read-only source, either run `pack.sh` directly (it only reads the class tree) or work on a **copy** of the runtime / of `log/classes/<id>/`.

`log/classes/.build-index` rebuilds the site class index (`.index`) from each class `.def`. After a filesystem-style restore into a class number that was not already in the index, run `sh .build-index <id>`.

---

## 3. Windows setup (native wims4win)

All Windows commands below are **PowerShell**. Do not use `.bat` or `cmd.exe`. `robocopy.exe` and `%SystemRoot%\System32\tar.exe` are standalone executables and may be called from PowerShell. Prefer `System32\tar.exe` for paths such as `D:\…`: GNU tar from a bundled MSYS2 `PATH` treats `D:` as a host name (`Cannot connect to D:`).

### 3.1 Prerequisites

- Windows 10/11, a writable disk with several GB free.
- For a **from-source** build: a Linux box (or WSL) with `build-essential`, `flex`, `bison`, `libgd-dev`, `mingw-w64`, as listed at the top of `tools/build-runtime-tree.sh`. On 2026-11-12 the Grok box could not install these (`deb.debian.org` returned HTTP 500; `make` / `flex` / `mingw` were absent). **To verify** when mirrors work.
- Bundled `other\msys2\usr\bin\sh.exe` must exist in the runtime (coreutils, `tar`, `gzip`; `zip` may be absent — use `tgz`).
- Apache / Laragon is optional for a CGI-direct smoke test. Vhost lines are in §8; **do not apply them** unless the owner asks.

### 3.2 Option A — Build a runtime from SVN r19040 + local fixes

On the Linux build box, in a **throwaway worktree** (never push this merge):

```bash
git fetch origin
git worktree add -b local/merge-pr12-pr13 /tmp/wims-port-merge origin/master
cd /tmp/wims-port-merge
git merge --no-edit origin/fix/win32-have-crypt-passcheck   # PR #12
git merge --no-edit origin/fix/win32-wimslogd-shared-redirect  # PR #13
# Confirm:
#   src/Platform/win32/config.h  has  #define HAVE_CRYPT 1
#   src/Platform/wp_win32.c      has  DuplicateHandle
tools/build-runtime-tree.sh /path/to/svn-r19040.tgz /tmp/wims-runtime-build
# Output: /tmp/wims-runtime-build/wims-runtime.tgz
```

Copy the tarball to the PC (for example via `C:\Users\<you>\` if a direct `D:\ALF_ZGit\…` transfer is blocked) and expand it to a **new sibling** folder, e.g.

`D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-r19040-grok-setup-test-YYYYMMDD`

Never overwrite an existing live or sibling tree. If the destination exists, stop and ask.

### 3.3 Option B — Copy a already-built runtime and drop in fixed binaries (verified 2026-11-12)

This is the path used in §10, because the box could not cross-compile that day. It is an allowed alternative in the original brief.

1. Choose a source **software** tree that already has modules, `public_html`, bundled MSYS2, and preferably a HAVE_CRYPT `wims.cgi.exe`. On the lab PC: `wims-runtime-r19040-grok-hook-class-20261106` (CGI SHA-256 `42BBAE9D…D8015C`, 933537 bytes, built 2026-11-06 17:09 Paris from PR #12).
2. `robocopy` it to a **new** sibling (ask before overwrite). Example (2026-11-12 11:59–12:01 Paris): destination `wims-runtime-r19040-grok-setup-test-20261112`, 22922 files, 432.46 MB, `robocopy` exit 1 = success.
3. Replace `bin\wimslogd.exe` with the PR #13 binary if the source still has the unfixed daemon. On the lab PC the fixed binary is in `wims-runtime-r19040-grok-hook-logd-20261109\bin\wimslogd.exe` (SHA-256 `19E7E257…C4A3C4E1`, 543754 bytes, 2026-11-09 16:44 Paris). Do **not** copy that sibling’s CGI (it has no `HAVE_CRYPT`).
4. Do **not** copy class `9001` into the “fresh” instance if you are about to restore it; or copy it, take the backup first (on the copy, not the source), then delete `log\classes\9001` before restore.

A local merge of PR #12 and PR #13 was created on the box at `/workspace/wims-port-merge-setup` (branch `local/merge-pr12-pr13`, **not pushed**). Cross-compile was blocked as noted in §3.1.

### 3.4 First-boot files, `HAVE_CRYPT`, `wimslogd`, NTFS lockdown

Working directory for the daemon is the **runtime root** (parent of `bin` and `tmp`). Put `other\msys2\usr\bin` first on `PATH` **for the daemon and for `*.sh` scripts**, not necessarily for a one-shot CGI (and never let MSYS `tar` shadow `System32\tar.exe` when you pass `D:\` paths).

```powershell
# $rt = runtime root
$log = Join-Path $rt "tmp\log"
New-Item -ItemType Directory -Force -Path $log, (Join-Path $rt "sessions"), (Join-Path $rt "s2") | Out-Null

# Site manager password: create log\.wimspass if missing (plaintext; WIMS hashes it
# on first successful manager login, leading '*'). Do not print or commit the value.
$wimspass = Join-Path $rt "log\.wimspass"
# ... write a generated password, then:
icacls $wimspass /inheritance:r /grant:r "${env:USERNAME}:(R,W)"

# Tighten log\ and tmp\ (remove broad Authenticated Users / Users write if present).
# Keep the operator / service account able to write sessions and logs.
icacls (Join-Path $rt "log") /grant:r "${env:USERNAME}:(OI)(CI)(F)"
icacls (Join-Path $rt "tmp") /grant:r "${env:USERNAME}:(OI)(CI)(F)"
icacls (Join-Path $rt "log") /remove:g "Authenticated Users"
icacls (Join-Path $rt "tmp") /remove:g "Authenticated Users"

# Stale pid after reboot: remove only if that PID is not running.
$pidf = Join-Path $log "wimslogd.pid"
if (Test-Path $pidf) {
  $old = (Get-Content $pidf -Raw).Trim()
  if ($old -and -not (Get-Process -Id $old -ErrorAction SilentlyContinue)) {
    Remove-Item $pidf -Force
  }
}
$msys = Join-Path $rt "other\msys2\usr\bin"
$env:PATH = "$msys;" + $env:PATH
Start-Process -FilePath (Join-Path $rt "bin\wimslogd.exe") `
  -WorkingDirectory $rt -WindowStyle Hidden
```

Stop a test sibling **by executable path**, never by image name alone (several `wimslogd.exe` can run at once; the pipe name is hashed per runtime).

`HAVE_CRYPT` is mandatory for class logins on Windows (docs/10 §3.4, PWD-07). A runtime whose `wims.cgi.exe` was built without it will reject every class password. Confirm the CGI you placed in the new sibling is a PR #12 (or later) build.

---

## 4. Debian setup (Debian 13)

### 4.1 Option A — Debian package (**to verify** the version)

As root, following the official auto-install wiki:

```bash
apt-get update
apt-get install wims
# Then: apache CGI, setwrapexec, manager_site, log/.wimspass (mode 600)
```

On 2026-11-12 this could not be exercised: `apt-get update` failed (`deb.debian.org` HTTP 500, InRelease not signed). No system-wide `wims` package was installed on the box.

### 4.2 Option B — Compile from an SVN r19040 export

```bash
# as a non-root user (the official ./compile refuses uid 0)
tar xzf svn-r19040.tgz
cd svn-r19040
rm -f .wims_maintainer_mode
chmod +x compile src/configure
yes "" | ./compile --mathjax --geogebra    # add --modules / --build-chroot as needed
# as root, when you want the official wrappers:
#   ./bin/setwrapexec
#   ./bin/apache-config
install -d -m 700 sessions s2 tmp/log tmp/sessions
chmod 600 log/wims.conf
# create log/.wimspass, chmod 600; set manager_site in log/wims.conf
```

On 2026-11-12 this could not be exercised on the box (`make`, `flex`, `bison` were not installed and could not be installed; see §3.1).

### 4.3 Option C — Deploy a pre-built r19040 tree into a new folder (verified 2026-11-12)

Used for the drill in §10. Never delete someone else’s tree.

```bash
# Example destination (must not already exist):
DST=/workspace/mw-wims-setup-debian
# Copy a compiled r19040 tree (software + default log), then:
rm -rf "$DST/sessions"/* "$DST/s2"/* "$DST/tmp/sessions"/* \
       "$DST/tmp/log/wimslogd.pid" "$DST/log/classes/9001"
mkdir -p "$DST/sessions" "$DST/s2" "$DST/tmp/sessions" "$DST/tmp/log"
chmod 600 "$DST/log/wims.conf" "$DST/log/.wimspass"
# Drop 9001 from log/classes/.index if you stripped the example class:
#   delete the line starting with :9001,
```

The first request to `public_html/wims` (CGI) starts `wimslogd`. Stop it by the pid in `tmp/log/wimslogd.pid` when the test is over.

Debian `config.h` already defines `HAVE_CRYPT`; class password hashes (`*` + DES crypt) are portable to a Windows CGI that also has `HAVE_CRYPT`.

---

## 5. Per-class backup

Source used in §10: class **9001** in `wims-runtime-r19040-grok-hook-class-20261106`. Treat that tree as **read-only**. If a step would write into it (UI backup writes `class_backup` in `.def`), copy the class or the whole sibling first.

### 5.1 Official archive (`pack.sh`) — preferred

From a **copy** of the runtime, or with `pack.sh` pointed at a copy of `log/classes/9001` (the script itself does not write the class tree):

```powershell
# $copy = runtime copy; $msys = $copy\other\msys2\usr\bin
# Convert D:\foo\bar to /d/foo/bar for MSYS sh.
$env:w_wims_home   = "<msys path of the runtime root>"
$env:w_wims_sesdir = "tmp/sessions/bkp_<id>"
$env:w_wims_class  = "9001"
# recdir entries + version + .def (download.proc adds .def when 'def' is selected)
$env:w_dirs = "version .def .security css access.conf doc exams src forum freeworks livret .log .grades .motd motd neighbors score noscore seq sheets supervisor tool .users .userlist photoboard vote cdt def"
$env:w_format = "tgz"          # zip needs a zip binary; bundled MSYS2 may not have one
$env:w_fname  = "9001"
$env:w_class_superclass = ""
$env:w_tmpnum_class = "9001"
New-Item -ItemType Directory -Force -Path (Join-Path $copy "tmp\sessions\bkp_<id>") | Out-Null
& (Join-Path $msys "sh.exe") "$($env:w_wims_home)/public_html/modules/adm/class/config/arch/pack.sh"
# Archive: $copy\tmp\sessions\bkp_<id>\getfile\9001.tgz
```

The official UI path is the same script, after a supervisor login: `cmd=reply&job=arch&job2=download&selection=all&format=tgz`. Prefer a copy: `download.proc` updates `.def`.

**Format:** gzip-compressed tar. Top-level directory is `class/` (not `9001/`). It contains at least `.def`, `serial`, `version`, `supervisor`, `.users/`, `.userlist`, `score/` (and other selected record types). Store the file **off the runtime**, with a SHA-256.

### 5.2 Filesystem fallback

```powershell
# Read-only on the source; use System32\tar.exe
& "$env:SystemRoot\System32\tar.exe" -C "<source>\log\classes" -czf "9001-filesystem-YYYYMMDD.tgz" 9001
```

**Format:** gzip-compressed tar. Top-level directory is `9001/`. This includes extra files that `pack.sh` does not select (for example `.du`, `.log.auth`, `supervisor.bak*`, `content.html`). Linux `tar` may warn about Windows `SCHILY.fflags` extended headers; the content still extracts. Store off the runtime, with a SHA-256.

---

## 6. Per-class restore into a fresh instance

The official `restore.sh` copies selected records from a session `upload/class/` directory into an **existing** class. For a brand-new instance you can either:

- create an empty class (site manager / supervisor UI — **to verify** end-to-end as a CGI-only flow), upload the `pack.sh` archive, and restore; or
- extract the official archive and copy `class/*` into `log/classes/<id>/`, then run `.build-index` (filesystem apply of the official format). This is what §10 used.

```bash
# Official pack.sh archive → new class 9001 (Debian or Windows with System32 tar)
mkdir -p "$WIMS_HOME/log/classes/9001" /tmp/wims-restore-9001
tar -xzf 9001-wims-pack.tgz -C /tmp/wims-restore-9001
cp -a /tmp/wims-restore-9001/class/. "$WIMS_HOME/log/classes/9001/"
cd "$WIMS_HOME/log/classes" && sh ./.build-index 9001
```

On Windows, extract with `System32\tar.exe`, then copy (including hidden files) with `robocopy /E`, then `sh ./.build-index 9001` via bundled `sh.exe`.

Filesystem-fallback restore: `tar -xzf 9001-filesystem.tgz -C "$WIMS_HOME/log/classes"` (top-level `9001/`), then `.build-index 9001`.

Do not extract over a live class without an explicit replace/merge decision. The official UI default is `merge`.

---

## 7. Verification

After restore, without printing passwords:

1. `log/classes/9001/` exists, with `supervisor`, `.users/`, `.def` (`class_defined=yes`), and `score/` files you care about (hash-compare to the source when possible).
2. `.index` contains a `:9001,…` line.
3. Start `wimslogd` if it is not running; issue a first CGI (`module=home`) and confirm a session is created.
4. Supervisor login: `adm/class/classes`, `type=authsupervisor`, POST `auth_user` / `auth_password` / `agreecgu=yes` (not a field named `login`). Expect `wims_user=supervisor` in the session (often after a 302 to a new session; `var.class.prep` may hold the identity).
5. Student / participant login: `type=authparticipant`, same POST fields. Expect `wims_user` equal to the login or `anonymous` / `anonymousN`.
6. Scores / Notes: the restored `score/<login>` file is non-empty (and any `.bin` worksheet data you expect). Opening the supervisor userscore UI is optional if the file hashes already match.

Then **stop** any `wimslogd` you started in a test sibling, checking the executable path (Windows) or pid file (Debian).

---

## 8. Laragon vhost lines (not applied)

Give these to the owner. Do **not** edit Laragon config unless the owner asked for that exact change.

```apache
# Cutover to the setup-test sibling (owner action only):
Define WIMS_RT "D:/ALF_ZGit/43-wims-paris-saclay_fr/Reprise_Wims_Grok/wims-runtime-r19040-grok-setup-test-20261112/public_html"
ScriptAliasMatch "^/wims/wims\.(.*)$" "${WIMS_RT}/wims.cgi.exe"
```

The live line today still points at `wims-runtime-r19040-grok-hook-20261106`. Cut over only after class logins work end-to-end on the new sibling (see BKP-06 in §10).

---

## 9. Security reminders (short)

- `HAVE_CRYPT` is required on Windows for class passwords (PR #12). Debian already has it.
- `log/.wimspass` mode 600 (Debian) or a tight NTFS ACL (Windows). Plaintext is rewritten to `*` + crypt on first manager login.
- Never store secrets in the git repository.
- NTFS: lock down `.wimspass`, and avoid inherited “Authenticated Users (M)” on `log\` and `tmp\` if untrusted local users exist. See docs/10 §4 and ACL-04.
- On Windows, identify `wimslogd` by path. The live daemon must not be stopped by mistake.

---

## 10. Results of the 2026-11-12 drill (Europe/Paris)

Constraints respected: live `hook-20261106` and `Reprise_Wims_Claude` not touched; Laragon not edited; credentials only from `C:\Users\Shadow\wims_test_creds.txt` (never printed or committed); PowerShell only; no load tests; test daemons stopped.

### 10.1 Artefacts

| Artefact | Location | Size | SHA-256 | Time (Paris) |
|---|---|---|---|---|
| Official `pack.sh` archive (`tgz`, top-level `class/`, 510 entries) | `Reprise_Wims_Grok\docs-admin\backups-20261112\9001-wims-pack-20261112.tgz` (copy also under `C:\Users\Shadow\wims-backups-20261112\` and `/workspace/mw-wims-setup-debian-incoming/`) | 7 155 000 bytes | `c94b37a68938ae7175685db17b4c6779dcb4015a4cb6b6941d4a7338dfaa408f` | 12:03:41 |
| Filesystem fallback (`tgz`, top-level `9001/`, 523 entries) | same folders, `9001-filesystem-20261112.tgz` | 7 173 356 bytes | `afc1a478afbb206a5bcfdf00544ad60f2ebba23fce1801d000fcf3b3f2d89976` | 12:02:50 |
| Fresh Debian instance | `/workspace/mw-wims-setup-debian/` | — | — | tree ready 12:05:06; official pack restored 12:05:47 |
| Fresh Windows sibling | `Reprise_Wims_Grok\wims-runtime-r19040-grok-setup-test-20261112` | copy 11:59:53–12:01:01 | CGI `42BBAE9D…D8015C`, wimslogd `19E7E257…C4A3C4E1` | official pack restored 12:11:32 |
| Source class 9001 (read-only) | `wims-runtime-r19040-grok-hook-class-20261106\log\classes\9001` | — | `.def` `D4A5A47A…0EC7` (mtime 2017-06-06 18:37:37), `score/anonymous1` `19A23E2F…6692` (117 bytes, LOGD-15 row of 2026-11-06 17:14:23) | unchanged at end of run |

`pack.sh` ran against the **copy** (setup-test) before class 9001 was removed from that copy; the source `.def` mtime and hashes did not change. `pack.sh` itself did not write the copy’s `.def` either (exit 0; a harmless `cp: version specified more than once` because `version` was also listed in `w_dirs`).

### 10.2 Debian

| Step | Result | Time (Paris) |
|---|---|---|
| New tree, example 9001 stripped, conf mode 600 | PASS | 12:05:06 |
| Restore official pack + `.build-index 9001` | PASS (7 users, supervisor present, score 117 bytes, same SHA-256, index line added) | 12:05:47 |
| First CGI + `wimslogd` start | PASS (`pid` 294617, home 200 OK, 27031 bytes) | 12:07:03 |
| Supervisor login | PASS (`wims_user=supervisor`) | 12:07:03–12:07:04 |
| Student login (`anonymous`) | PASS (`wims_user=anonymous`) | 12:07:03–12:07:04 |
| Scores / Notes | PASS (`score/anonymous1` contains a `score` row) | 12:07:04 |
| Stop `wimslogd` | PASS | 12:07:04 |

Login POST must use `auth_user` (not `login`) and `agreecgu=yes`. An earlier attempt with `login=` left the student on the form; the procedure in §7 uses the correct fields.

### 10.3 Windows

| Step | Result | Time (Paris) |
|---|---|---|
| New sibling (no overwrite) + PR #12 CGI + PR #13 `wimslogd` | PASS | 11:59–12:02 |
| Create `log\.wimspass` + `icacls` lockdown | PASS (inheritance removed; only `Shadow:(R,W)`) | 12:09:21 |
| Official restore + `.build-index` | PASS (score SHA-256 matches source and Debian; 7 users; index updated; `.build-index` exit 0) | 12:11:32 |
| `wimslogd` start (pid 22400, path = setup-test) + first CGI | PASS (200 OK, session, 27031 bytes) | 12:11:32–12:11:37 |
| Supervisor + student login | **FAIL** | 12:11:37 (and retests 12:12:26–12:18:21) |
| Scores / Notes on disk | PASS (same 117-byte file / SHA-256) | 12:11:37 |
| Stop `wimslogd` by path; source + live hashes unchanged | PASS | 12:11:37 |

**BKP-06 failure (Windows class login after restore):** `log/classes/9001/.log.auth` recorded `supervisor` and `anonymous` with `??????` (`var.auth` writes that marker when `!passcheck` returns `no`). The restored `supervisor` and `.users/anonymous` files are **byte-identical** to the read-only source (`36EDBC0A…` and `260B094B…`). The same official archive plus the same credentials **succeed on Debian** (BKP-04). The same HAVE_CRYPT CGI and credentials **succeeded** on the source sibling the same morning (PWD-04/06, docs/10 §17). Retries with GET instead of POST and without MSYS2 on `PATH` still failed. CGI stderr was empty. This is a **code or environment bug to investigate**, not a corrupt archive. No code change was committed. Hypotheses to test (do not swap live binaries): interaction of the PR #12 CGI with the PR #13 `wimslogd` (that pair had not been logged-in together before); something in the fresh sibling environment (new `.wimspass`, tighter ACLs) that does not affect a direct disk read of the user files. `current_pass` is used by `!passcheck` in `var.auth` and is not obviously assigned from `auth_password` in the script; that is also worth a careful read, because a platform difference in how that variable is set would explain a `passcheck=no` with a correct stored hash.

### 10.4 Setup options that were not run

- Windows `tools/build-runtime-tree.sh` from the local merge (no mingw / no Debian mirrors).
- Debian `apt-get install wims` and `./compile` (same mirror / toolchain gap).
- Laragon vhost cutover (lines only, §8).
- Official UI upload + `restore.sh` into a class created through the site-manager UI (filesystem apply of the official `class/` tree was used instead).

---

## 11. Items to test (map to docs/08)

See docs/08 §4.17 for IDs, priorities, and the 2026-11-12 status. Summary:

| ID | What | Priority | 2026-11-12 |
|---|---|---|---|
| SETUP-01 | Debian fresh instance (pre-built tree) | P1 | PASS |
| SETUP-02 | Debian first CGI + `wimslogd` start/stop | P1 | PASS |
| SETUP-03 | Windows new sibling, no overwrite | P0 | PASS |
| SETUP-04 | Windows HAVE_CRYPT + PR #13 daemon in that sibling | P0 | PASS (prior build artefacts; box compile blocked) |
| SETUP-05 | Windows `.wimspass` + `icacls` | P1 | PASS |
| SETUP-06 | Windows `wimslogd` start, CGI, stop by path | P1 | PASS |
| SETUP-07 | Debian apt or `./compile` from source | P2 | NOT RUN |
| SETUP-08 | Laragon `WIMS_RT` cutover | P2 | WAITING (owner) |
| BKP-01 | Official `pack.sh` tgz, source unmodified | P0 | PASS |
| BKP-02 | Filesystem tgz fallback | P1 | PASS |
| BKP-03 | Restore official pack on Debian + files/scores | P0 | PASS |
| BKP-04 | Debian supervisor + student login after restore | P0 | PASS |
| BKP-05 | Restore official pack on Windows + files/scores | P0 | PASS |
| BKP-06 | Windows supervisor + student login after restore | P0 | **FAIL** (see §10.3) |
| BKP-07 | Source class + live runtime untouched | P1 | PASS |

---

## 12. References

- `public_html/modules/adm/class/config/arch/{pack.sh,download.proc,restore.sh,restore.proc,upload.proc,recdir}`
- `public_html/modules/adm/class/classes/{var.proc,var.auth}` — login fields `auth_user`, `auth_password`; `!passcheck`
- `log/classes/.build-index`
- `tools/build-runtime-tree.sh`, `tools/build-wims-win32.sh`, `tools/build-wimslogd-win32.sh`
- PRs [\#12](https://github.com/io-e/wims4win/pull/12), [\#13](https://github.com/io-e/wims4win/pull/13)
- docs/04, docs/10, official WIMS EDU install wiki
