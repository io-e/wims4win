# 10 — WIMS server administration (Windows and Debian)

*English operations guide for running a WIMS server on native Windows (wims4win)
and on Debian. Written 2026-11-11 (Europe/Paris). It takes into account the
GEM expertise report on WIMS administration and the OEF format (see §2), official
WIMS EDU documentation, and the wims4win repository. It does not replace those
sources.*

**Password management and rights/permissions are in §3 and §4**, immediately
after the document inventory, because they are the first things an operator
must get right.

Commands in this file are taken from the code, official docs, or procedures
already run on ShadowPC / the Linux box. Anything not verified in this project
is marked **to verify**. This document never contains passwords or other
secrets; do not add any.

---

## 1. Purpose and scope

This guide is for **administering a live WIMS server**: accounts and passwords,
who may do what, filesystem and service rights, starting and stopping
`wimslogd`, daily housekeeping and module updates, logs, sandbox integrity,
classes, backups, upgrades, and teardown.

It covers two platforms:

| Platform | What it is |
|---|---|
| **Windows (wims4win)** | Native port (no WSL/Cygwin as the server). Typical front-end: Laragon Apache, `WIMS_RT` pointing at a runtime tree built by `tools/build-runtime-tree.sh`. |
| **Debian** | Upstream WIMS: either `apt install wims` (often under `/var/lib/wims`) or a manual compile as user `wims` (often `/home/wims`), plus Apache and optional `wims-chroot`. |

**Out of scope** (pointed to, not copied):

- OEF authoring, Createxo/Modtool, SLIB, Flydraw/Canvasdraw — see the GEM report and the WIMS EDU OEF wiki.
- The Windows *first-install* walkthrough — see [04_Guide_Installation_et_CGI_Laragon.md](04_Guide_Installation_et_CGI_Laragon.md).
- The migration test catalogue — see [08_Test_Plan_Windows_Migration.md](08_Test_Plan_Windows_Migration.md).
- AlgoGebra hook install/uninstall details — see `docs/09_Hook_JSON_AlgoGebra.md` on branch `feat/algogebra-json-hook` (not yet on `master` at the time of writing).

---

## 2. Does this document already exist?

**Short answer:** no existing document covers *native Windows + Debian* WIMS
*operations* in English with passwords, rights, wimslogd, and a test map.
This file is new. It references the sources below instead of duplicating them.

### 2.1 Official documentation on the Web

These describe **Unix/Linux (and macOS) WIMS**, almost all in **French**, except
the technical help and README which have English versions. **None of them
document the native Windows port** (wims4win). A bootable “KNOWIMS” CD is
mentioned for trying WIMS from Windows; that is not this port.

Do not confuse educational WIMS with **Hach WIMS** (a water-industry product).
Web searches for “WIMS Administrator Guide Windows” often hit that unrelated
software.

| Source | Language | Coverage |
|---|---|---|
| [WIMS EDU wiki — installation hub](https://wiki.wimsedu.info/doku.php?id=installation:a) | FR | Linux, macOS, PiWIMS, first config, upgrades, NGINX, security, reinstall |
| [Debian automatic install](https://wiki.wimsedu.info/doku.php?id=installation:debian:auto) | FR | `apt-get install wims` (package versions lag upstream; path often `/var/lib/wims`) |
| [Debian manual install](https://wiki.wimsedu.info/doku.php?id=installation:debian:manual) | FR | user `wims`, compile, `setwrapexec`, `apache-config` |
| [Debian chroot](https://wiki.wimsedu.info/doku.php?id=installation:debian:debian_chroot) | FR | `setchroot`, bundled CAS inside the chroot |
| [Debian upgrade](https://wiki.wimsedu.info/doku.php?id=installation:debian:debian_upgrade) | FR | announce MOTD, backup/snapshot, `apt` or tarball + `./compile`, `setwrapexec`/`setchroot` |
| [First configuration](https://wiki.wimsedu.info/doku.php?id=installation:first_config) | FR | online maintenance, `manager_site`, load thresholds, MathML, auto module update |
| [Site management FAQ](https://wiki.wimsedu.info/doku.php?id=faq:gestion) | FR | `.wimspass`, one-time vs permanent manager password, disk-full panic, debugging, lost class passwords |
| [Security](https://wiki.wimsedu.info/doku.php?id=installation:secure) | FR | ports, intranet mode, bot harvesting, Apache examples |
| [Reinstall / backup](https://wiki.wimsedu.info/doku.php?id=installation:reinstaller_le_serveur) | FR | online Backup, `WIMSdata-<date>.tgz` |
| [Upstream README](https://wims.univ-cotedazur.fr/wims/README) | EN | compile, third-party CAS, `setwrapexec`, `apache-config`, `localwims` |
| [Technical documentation](https://wims.univ-cotedazur.fr/wims/wims.cgi?+module=help%2Fwimsdoc.en&lang=en) | EN | kernel, modules, `!exec`, not a sysadmin runbook; little on `wimslogd` |
| [User / teacher help](https://wims.univ-cotedazur.fr/wims/wims.cgi?+lang=en&+module=help%2Fmain.en) | EN | classes and worksheets, not server ops |
| [SourceSup wimsdev](https://sourcesup.renater.fr/projects/wimsdev) | — | official source (SVN) and tarballs (e.g. 4.31) |
| [Debian package `wims`](https://packages.debian.org/trixie/web/wims) | — | ships `wimslogd`, `housekeep.daily`, `modupdate.auto`, `backup`; **no documented systemd unit for `wimslogd` found** (to verify on a given release) |
| [WIMS EDU forum: server administration](https://wimsedu.info/forums/forum/gestion-dun-serveur-wims/administration-dun-serveur-wims/) | FR | operator discussion |
| [OEF wiki](https://wiki.wimsedu.info/doku.php?id=faq:oef) | FR | exercise authoring (out of scope here) |

`wimslogd` on Debian is started by the first CGI request (`bringuplogd`), not
typically as a standalone systemd service. **To verify** on a specific Debian
package whether any init hook exists.

### 2.2 Documents already on disk / in this repository

| Location | What it is | Overlap |
|---|---|---|
| `D:\ALF_ZGit\02-wims4win\docs\Officiel\GEM_Documentation WIMS et Format OEF.md` (also `.docx`) | French *expertise report* (2026-11-11), untracked in that checkout: admin *concepts* (install/isolation, `wims.conf`, sessions, LTI/Moodle) plus a long OEF engineering part | **Source to take into account**, not an operations runbook; no native Windows, no wimslogd restart, no HAVE_CRYPT, no test map |
| This repo [docs/04](04_Guide_Installation_et_CGI_Laragon.md) | Windows **installation** and Laragon CGI | Install only; mentions empty `log/.wimspass` “to define”, little on changing class passwords or daily jobs |
| [docs/08](08_Test_Plan_Windows_Migration.md) | English test plan | Status of LOGD/SEC/SBX/CGI tests; not an admin guide |
| [docs/Suivi_Developpement_Portage.md](Suivi_Developpement_Portage.md) | Development journal | Evidence (HAVE_CRYPT, LOGD-12, sibling runtimes) |
| [docs/05_Etat_et_Decisions.yaml](05_Etat_et_Decisions.yaml) | Decisions (D21/D22 lowil, etc.) | Policy, not procedures |
| `docs/09_Hook_JSON_AlgoGebra.md` (hook branch) | Hook install / opt-out / uninstall | Hook only |
| `docs/archive/Guide_Installation_Wims4Win_CLAUDE_V1.md` | Historical install draft | Superseded by docs/04 |
| `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\` | Deliverables: install/test docs, runtime siblings | Copies of repo docs; **no prior admin guide**; no `docs-admin\` folder existed when this was written |
| `D:\ALF_Scripts\43-wims-paris-saclay_fr\` | OEF tooling, not server admin | No overlap |

`D:\ALF_ZGit\02-wims4win` itself is a checkout of `https://github.com/io-e/wims4win.git` on branch `feat/algogebra-json-hook` at `00eaa91`, with `docs/Officiel/` **untracked**. That tree’s `master` was behind origin. This guide is written in `/workspace/wims-port` and must not modify `02-wims4win`.

### 2.3 Overlap verdict

Write this operations guide; **do not copy** the GEM OEF chapters, the full
docs/04 install, or the French wiki. Link them. Fill the gap: Windows+Debian
day-2 admin, passwords, rights, wimslogd, jobs, and a test map.

---

## 3. Password management (Windows and Debian)

This is the same WIMS kernel on both platforms (`!passcrypt` / `!passcheck` in
`src/calc.c`). The Windows-specific trap is **`HAVE_CRYPT`** (see §3.4).

### 3.1 What is stored, and how it is hashed

| Secret | File (under the WIMS home / runtime root) | Form |
|---|---|---|
| Site manager (webmaster) password | `log/.wimspass` | May start as **plaintext** (what `./compile` writes). On first use, `adm/manage/auth.proc` runs `!passcrypt` and rewrites the file if it does not already contain `*`. |
| One-time manager password | `tmp/log/.wimspassone` | Generated when `log/.wimspass` is absent or empty; session-scoped. |
| Class supervisor password | `log/classes/<id>/supervisor` field `user_password` | `!passcrypt` (leading `*`, DES `crypt()`). |
| Class registration password | `log/classes/<id>/.def` field `class_password` | Same. |
| Student / participant | `log/classes/<id>/.users/<login>` field `user_password` | Same. |
| Module developer | developer record via `adm/manage/developer` | `!passcrypt`. |

Hashing (`_passcrypt` in `src/calc.c`):

- Compiled only if `HAVE_CRYPT` is defined.
- Calls `crypt(plaintext, salt)`. Default salt when creating a hash is `"Nv"`
  (traditional DES crypt). `passcheck` re-hashes with the salt taken from the
  stored hash so old and new salts can coexist (see upstream changelog).
- On Windows the implementation is `src/Platform/wp_descrypt.c` (already
  linked). A bcrypt helper exists (`wp_crypt_bcrypt.c`) but **DES crypt remains
  the default** for compatibility with existing files.

**Never commit** `log/.wimspass`, class user files, or any password to git,
chat, or this document. Keep operator secrets in a private file outside the
repository.

### 3.2 Site manager password — set, change, reset

Access to “WIMS online site management” (`adm/manage`) also requires the
client IP to be listed in `manager_site` (see §4.1). The password check is
`!passcheck $auth and $pass` in `adm/manage/auth.proc`.

**Define a permanent password**

1. Write the password into `log/.wimspass` (one line).
2. Restrict the file:
   - **Debian:** `chmod 600 log/.wimspass` and `chown wims:wims` (or the
     account that runs the CGI). Official docs and `auth.phtml` require the
     file to be readable **only** by the WIMS account. `./compile` uses
     `chmod 600` when it creates the file.
   - **Windows:** equivalent NTFS rights: the file must not be readable by
     Everyone / other interactive users. The Apache / CGI identity needs
     read access. Exact `icacls` lines: **to verify** on the Laragon service
     account; the kernel’s Unix mode check (`S_IRWXO|S_IRWXG`) is what
     `manager_check()` uses (`src/wims.c`). If those bits appear set,
     `wims_ismanager` becomes `-2` and management is refused.
3. First successful load of `adm/manage` hashes a plaintext file (see
   `auth.proc`: if `*` is not in the stored value, it `!passcrypt`s and
   `setpass.sh` rewrites the file, then `chmod og-rwx`).

**Change** the permanent password the same way: replace the file contents
(plaintext is acceptable; it will be hashed on next login) or, **to verify**
whether the online maintenance UI also offers a password field after you are
already authenticated. The documented official method is the file.

**Switch to one-time passwords:** delete `log/.wimspass`. The server writes a
random password into `tmp/log/.wimspassone` for that session. Official docs
consider this more secure than a permanent file.

**Reset if locked out (same machine):** from an account that can write
`log/.wimspass`, set a new value and fix rights; or remove the file and use
the one-time file. There is no back-door in the kernel.

`setpass.sh` / `getpass.sh` are small `sh` scripts under
`modules/adm/manage/scripts/`. On Windows they need **MSYS2 / bundled
`other/msys2/usr/bin/sh.exe` on PATH**, not `cmd.exe`.

### 3.3 Class supervisor, student, and class-registration passwords

In a class, use module **`adm/class/passwd`**:

| Job in `passwd/var.proc` | Who | What |
|---|---|---|
| `job=user` | the user (supervisor must re-auth) | change own password; requires the old password (`bad_user_password` if it does not match) |
| `job=part` | supervisor | set a participant’s password (`user_forcechpwd=yes`) |
| `job=class` | supervisor only | set `class_password` in the class `.def` (used when registering) |

The site manager can also set supervisor / class passwords from
`adm/manage` class tools (`class.proc` uses `!passcrypt` and writes
`user_password` / `class_password`). Official FAQ: a lost supervisor
password can be handled by the site manager (do not paste the new secret
into tickets or git).

**Test class 9001** (this project): student `anonymous` and supervisor logins
were verified on the **sibling** runtime `wims-runtime-r19040-grok-hook-class-20261106`
after the HAVE_CRYPT fix (LOGD-15). The **live** Laragon tree was not given
that CGI. Treat 9001 as a test class only; keep its secrets out of the repo.

### 3.4 HAVE_CRYPT on Windows (PR #12) — read this before changing passwords

`_passcrypt` is an empty function when `HAVE_CRYPT` is not defined. Then
`!passcheck` never produces a matching hash and **every class (and manager)
password check fails** (`bad_password`), even if the stored hash is correct.

- Cause: `src/Platform/win32/config.h` lacked `#define HAVE_CRYPT 1`.
- Fix: pull request [https://github.com/io-e/wims4win/pull/12](https://github.com/io-e/wims4win/pull/12)
  (unmerged at the time of writing). `calc.c` stays IDENTICAL to Linux.
- **Implication:** any `wims.cgi.exe` built **before** that fix will reject
  all passwords. Deploy the fixed CGI to the runtime you actually serve
  (sibling first; do not silently overwrite the live tree). Debian/Linux
  builds already define `HAVE_CRYPT` in the usual Unix `config.h`.

### 3.5 Policy and rotation

WIMS does **not** implement a documented complexity or expiry policy in the
kernel (to verify if a given site added custom checks). Recommended
operations:

- Rotate the site manager password after staff changes and after any suspected
  leak of `log/.wimspass`.
- Prefer one-time manager passwords on internet-facing servers (official
  guidance).
- Do not use a **dynamic** IP in `manager_site`.
- After rotation, confirm login on a throwaway class account before
  announcing.
- Never store passwords in the repository, in `docs/`, or in runtime trees
  that get zipped into git.

---

## 4. Rights and permissions (Windows and Debian)

### 4.1 Who can do what inside WIMS

| Role | How it is granted | What they can do |
|---|---|---|
| **Anonymous visitor** | no login | public modules; cannot see class scores or maintenance |
| **Student / participant** | class login, file under `log/classes/<id>/.users/` | exercises, own scores; cannot open supervisor pages |
| **Class supervisor (teacher)** | `log/classes/<id>/supervisor` | class roster, sheets, scores, `adm/class/passwd`, class password |
| **Site manager (webmaster)** | IP in `manager_site` **and** password in §3.2; session flag `.manager=yes` | online maintenance: classes, modules, backups, MOTD, developers, technical topics |
| **Module developer** | account created by the site manager (`adm/manage/developer`) | Modtool / devel modules; not full site admin |
| **`wimsdev` / source project** | SourceSup developers, not a WIMS runtime role | upstream code; no special login on your server unless you grant one |

`manager_site` lives in `log/wims.conf` (parsed in `src/config.c`). Default in
`src/wims.c` is `"127.0.0.1 ::1"`. `./compile` may write `manager_site=127.0.0.1`
when you set a webmaster email. The home page shows “online site management”
only if `checkhost(manager_site)` succeeds (`manager_check()`).

`manager_check()` also:

- can require HTTPS if `manager_https` is set;
- sets `wims_ismanager=-1` if `log/wims.conf` is group- or world-accessible;
- sets `wims_ismanager=-2` if `log/.wimspass` is group- or world-accessible;
- requires a trusted module;
- after password success, writes `tmp/.../.manager` and sets `wims_ismanager=2`.

The site manager can open a class as supervisor with a special token
(`auth_password=*wims_site_manager` in `adm/manage` class lists). That is
powerful: protect the manager password accordingly.

### 4.2 Debian filesystem and process rights

- Run the tree as user **`wims`**, not as root (official install).
- After compile, as **root**: `./bin/setwrapexec` (and `./bin/setchroot` if
  you use chroot). `setwrapexec` sets `public_html/bin/wrap..exec` to owner
  `nobody` and mode **6755** (setuid/setgid wrapper). It refuses to run if
  group `nobody`/`nogroup` is missing, and removes `log/unsecure` on success.
- `./bin/setwimsd` is only for the optional `wimsd` debug server
  (`localwims`), same 6755 pattern — not required for Apache CGI.
- `log/.wimspass` and `log/wims.conf`: mode `600`, owner `wims`.
- CGI and `wimslogd` must write `tmp/`, `tmp/log/`, `sessions/`, `s2/`, and
  class data under `log/classes/`.
- Chroot installs confine CAS and helpers under `chroot/` (`setchroot`).

Exact `chown`/`chmod` of every directory after a tarball upgrade: follow the
official upgrade page, then **to verify** with the “installation check” tab
in online maintenance.

### 4.3 Windows filesystem, services, and pipes

- There is **no setuid** on Windows. Isolation is the sandbox
  `public_html/bin/ch..root.exe`: AppContainer + Job Object by default;
  **low-integrity** (`lowil`) for `gap.exe` and `octave-cli.exe` (decisions
  D21/D22 in [05_Etat_et_Decisions.yaml](05_Etat_et_Decisions.yaml)).
- The Laragon / Apache identity must be able to **read** the runtime and
  **write** `tmp\`, `tmp\log\`, sessions, `s2\`, and `log\` (including class
  files). Official install guide: if sessions cannot be created, check NTFS
  write access.
- `log\.wimspass` must not be readable by other interactive users (see §3.2).
- **Named pipe** `\\.\pipe\wims-wimslogd-<hash>` is per runtime tree (path
  hash), so two trees can run two daemons. Whether a second Windows user can
  open that pipe is **SEC-02**, still WAITING (needs a second local user;
  script `tests-win_ALF/SEC-02_pipe_acl_steps.ps1`). Until that test passes,
  treat cross-user pipe access as an undocumented risk.
- Do not run the CGI elevated (DROP_PRIV is refused if the token is
  elevated; covered by PLT-01 / a second SBX-21 row).

### 4.4 Sandbox integrity (both platforms)

- Debian: Unix chroot + wrappers (`wrap..exec` / `ch..root`).
- Windows: AppContainer (write only in the session dir; no HKCU write; no
  extra network capability in the S3 test) and lowil for GAP/Octave.
- Octave interface `illegal[]` includes `system`, `dos`, `winqueryreg`, and
  **`unix`** (rewritten to `sJstem` / `uJix`, etc.).
- Maxima is launched as `sbcl.exe --core`, never `maxima.bat` / `cmd.exe`.

See §17 for the SBX test IDs. Do not run SBX-14 (profile purge) unless the
owner explicitly allows it.

---

## 5. Architecture at a glance

```
Client  -->  Apache (CGI)  -->  wims.cgi / wims.cgi.exe
                                   |  named pipe / socket
                                   +--> wimslogd  (sessions, scores, daily jobs)
                                   +--> ch..root / wrap..exec --> CAS (PARI, Maxima, Octave, GAP, …)
```

Windows runtime root (example, Paris-Saclay lab) looks like:

- `bin\wimslogd.exe`, `bin\housekeep.daily`, `bin\modupdate.auto`, `bin\backup`
- `public_html\wims.cgi.exe`, `public_html\bin\ch..root.exe`
- `log\wims.conf`, `log\.wimspass`, `log\classes\`
- `tmp\log\` — `wimslogd.pid`, `wimslogd.out`, `housekeep.log`, `modupdate.log`, `exec.fail`, `keepdate`, `mupdate`
- `other\msys2\usr\bin\sh.exe` — required for `#!` scripts (no `cmd.exe`)

Debian: same layout under `/home/wims` or `/var/lib/wims`, with
`public_html/wims.cgi` and `/usr/lib/wims/bin/wimslogd` in the Debian package.

---

## 6. Installation (pointers)

- **Debian automatic:** [wiki auto](https://wiki.wimsedu.info/doku.php?id=installation:debian:auto) — `apt-get install wims`. **To verify** the package version you actually get.
- **Debian manual / chroot:** official wiki; `./compile --mathjax --jmol --modules --geogebra` (plus `--build-chroot` when used); then root `setwrapexec`, optional `setchroot`, `apache-config`.
- **Windows:** [docs/04](04_Guide_Installation_et_CGI_Laragon.md) and `tools/build-runtime-tree.sh` (from an SVN export). Do not invent a second install path here.
- After install, set `manager_site` and `log/.wimspass` (§3–§4) before opening the site.

---

## 7. HTTP front-end (Apache)

**Debian:** `./bin/apache-config` as root; `a2enmod cgi` as required by the
wiki; `systemctl reload apache2` (or `service apache2 restart`). HTTPS
termination and NGINX reverse proxy: official wiki; **to verify** on your
host.

**Windows (Laragon):** the vhost defines `WIMS_RT` to the **sibling or live
runtime** `public_html`. Example (illustrative, already used on the lab PC):

```apache
Define WIMS_RT "D:/ALF_ZGit/43-wims-paris-saclay_fr/Reprise_Wims_Grok/wims-runtime-r19040-grok-hook-20261106/public_html"
ScriptAliasMatch "^/wims/wims\.(.*)$" "${WIMS_RT}/wims.cgi.exe"
```

Do **not** edit Laragon config from an agent unless the owner asked for that
exact change; give the owner the `Define WIMS_RT` line instead. Cutover to
another sibling is a one-line `WIMS_RT` change, only after class logins work
end-to-end (owner decision).

Use PowerShell for checks, never `.bat` / `cmd.exe`.

---

## 8. Runtime trees, siblings, and cutover (Windows)

Deliver new runtimes as **sibling folders**; never overwrite the live tree.

Known siblings on the lab PC (names only):

- live: `wims-runtime-r19040-grok-hook-20261106`
- class modules + HAVE_CRYPT CGI: `wims-runtime-r19040-grok-hook-class-20261106`
- LOGD-12 fix verification: `wims-runtime-r19040-grok-hook-logd-20261109`

Each tree has its own `tmp\log\wimslogd.pid` and a **hashed pipe name**, so
two `wimslogd` processes can coexist (LOGD-13). Identify processes by
**path**, not by image name alone, when stopping a sibling.

---

## 9. wimslogd — start, stop, restart, reboot

`wimslogd` writes `tmp/log/wimslogd.pid` and `wimslogd.out`. On Windows the
start line looks like `wimslogd <pid> started at YYYYMMDD.HH:MM:SS (win32).`

### 9.1 Windows (verified on ShadowPC)

Working directory must be the **runtime root** (parent of `bin` and `tmp`).
Put bundled `other\msys2\usr\bin` first on `PATH`.

```powershell
# PowerShell only. $rt = runtime root (not public_html).
$log = Join-Path $rt "tmp\log"
# If the pid file is stale (process not running), remove it first:
if (Test-Path "$log\wimslogd.pid") {
  $old = (Get-Content "$log\wimslogd.pid").Trim()
  if ($old -and -not (Get-Process -Id $old -ErrorAction SilentlyContinue)) {
    Remove-Item "$log\wimslogd.pid" -Force
  }
}
$msys = Join-Path $rt "other\msys2\usr\bin"
$env:PATH = "$msys;" + $env:PATH
Start-Process -FilePath (Join-Path $rt "bin\wimslogd.exe") `
  -WorkingDirectory $rt -WindowStyle Hidden
```

Stop a **sibling** only after checking `Path` contains that folder name.
Never stop the live daemon by mistake.

After a **reboot**, both daemons were observed dead and pid files stale
(2026-11-10). First CGI *should* call `bringuplogd`; whether a stale pid
blocks that is **to verify** (related: LOGD-14, not run because it is
disruptive). Until verified, remove a stale pid and start `wimslogd` as
above, or issue a first CGI and confirm a new pid.

There is **no documented Windows service / scheduled task** that starts
`wimslogd` at boot in this project (**to verify** if the operator adds one).

### 9.2 Debian

Usually the first request to `wims.cgi` starts `wimslogd`. Pid and logs are
under `tmp/log/` of the WIMS home. A dedicated systemd unit was **not found**
in the documentation reviewed; **to verify** on the installed package
(`systemctl list-units '*wims*'`). After reboot, confirm the pid is live;
remove a stale pid if the process is gone, then hit the site once.

---

## 10. Daily jobs: housekeep, modupdate, backup

Fired by `wimslogd` (not by cron, unless you add one):

| Job | Script | Gate (from `wimslogd_win.c` / `wimslogd.c`) | Stamp file | Log |
|---|---|---|---|---|
| Housekeep | `bin/housekeep.daily` | `(thismin % 5)==0` and wall `nowmin>15`, once per calendar day | `tmp/log/keepdate` | `tmp/log/housekeep.log` |
| Module update | `bin/modupdate.auto` | `nowhr*60+nowmin > modupdatetime` (random 0–350 at start) and `(thismin % 17)==11` | `tmp/log/mupdate` | `tmp/log/modupdate.log` |
| Backup | `bin/backup` | `backup_hour` in the `wimslogd` env (often `-1` = off) | `backup/backdate` | `tmp/log/backup.log` |

`thismin` is `(time - rshift) / MINLENGTH` with `MINLENGTH=47`, not wall-clock
minutes. Clearing `keepdate` / setting `mupdate` to an old YYYYMMDD is an
accepted way to force the next window. Early morning, `modupdatetime` can
delay modupdate until you restart the daemon (new random gate).

On Windows, `call_ssh(..., "bin/… &>%s/….log")` opens the same log as stdout
and stderr. A **sharing violation** emptied the logs and wrote `exec.fail`
`error 0` until [PR #13](https://github.com/io-e/wims4win/pull/13)
(`DuplicateHandle` when `outf==errf`). That bug **does not exist on Debian**
(`fork` + `freopen`). Until PR #13 is merged and the live `wimslogd.exe` is
replaced (sibling first), the live Windows daemon can still fail to spawn
those scripts.

`bin/backup` refuses to run unless env `wimslogd` is set (i.e. launched by
the daemon). Official alternative: online maintenance → Backup
(`WIMSdata-<date>.tgz`). See the reinstall wiki for restore.

---

## 11. Logs and diagnostics

Under `tmp/log/`:

| File | Meaning |
|---|---|
| `wimslogd.out` / `.err` | daemon start / errors |
| `wimslogd.pid` | current pid; stale after a crash or reboot |
| `housekeep.log`, `modupdate.log` | daily jobs |
| `exec.fail` | spawn failures (`Failed to execute bin/…`, Win32 `error N`) |
| `lastclean` | session cleaner ran |
| `wimslogd.cleaning.err` | session delete errors (e.g. permission denied) |

HTTP / WIMS logs also live under `log/` (`access.log`, `session.log`,
`user_error.log`, …). Official FAQ: `cannot-create-session-directory` usually
means disk or inode exhaustion; keep `log/` and `tmp/` on filesystems with
room.

Debug mode (“debogging”) from online maintenance → technical topics: inspect
`tmp/sessions/<id>` while the session still exists.

---

## 12. AlgoGebra JSON hook

Installed by default by `tools/build-runtime-tree.sh`. Opt out at build time:

```bash
ALGOGEBRA_HOOK=0 tools/build-runtime-tree.sh <export-svn-r19040> [workdir]
```

The hook is triggered only by `cmd=reply` with `special_parm=algogebra_json`.
It is not a grading path via `adm/raw`. Uninstall and Debian install scripts:
`docs/09_Hook_JSON_AlgoGebra.md` on `feat/algogebra-json-hook`.

---

## 13. Backups and restore

- Daemon path: `bin/backup` when `backup_hour` is set (see §10).
- UI path: site manager Backup, then store `WIMSdata-*.tgz` **off the server**.
- Before a Debian upgrade, official wiki: announce MOTD (`log/motd.phtml.LANG`),
  snapshot the VM, then upgrade.
- Windows: copy or archive the **whole sibling** you care about (`log/`,
  especially `log/classes/`). Do not overwrite live.
- Restore: official reinstall page; **to verify** on Windows (no native
  restore drill recorded in docs/08).

---

## 14. Upgrades

**Debian package:** `apt update && apt upgrade` as on the official upgrade
page; if you use chroot, re-run `./bin/setchroot` as root and check the
installation tab.

**Debian manual:** `bin/server-interrupt`, apply upstream upgrade scripts if
required, unpack, `./compile …`, `setwrapexec`, test, remove MOTD.
Classes are not touched by the documented upgrade.

**Windows:** build a **new sibling** with `tools/build-runtime-tree.sh` and
the desired CGI (`HAVE_CRYPT`, hook, wimslogd fix), smoke-test (home, CAS,
class login), then change `WIMS_RT` (owner action). Never overwrite live
binaries in place as the first step.

---

## 15. Uninstall / teardown

**Debian package:** `apt remove --purge wims` (and `wims-moodle` if
installed) is the usual inverse of `apt install`; **to verify** that
`/var/lib/wims` class data is handled as you expect (back it up first).

**Debian manual:** stop Apache vhost, stop `wimslogd`, remove the `wims`
user tree only after backup. **To verify** leftover cron/Apache snippets.

**Windows:** stop that tree’s `wimslogd` (by path), point `WIMS_RT` elsewhere
or disable the vhost, then you may rename or archive the sibling folder.
Do not delete a tree you did not create without an explicit request. Hook
uninstall: hook doc on the hook branch.

---

## 16. Security checklist (summary)

- Passwords and file modes: §3–§4.
- `manager_site` is a static IP; HTTPS for management if you set
  `manager_https` (**to verify** in your vhost).
- Official hardening (ports, intranet mode, bot rules):
  [security wiki](https://wiki.wimsedu.info/doku.php?id=installation:secure).
- Load thresholds in online maintenance should be scaled by CPU count
  (wiki; GEM). Command on Debian: `nproc` or `grep -c processor /proc/cpuinfo`.
  Windows equivalent: **to verify**.
- No `cmd.exe` / `.bat` in the WIMS path; Maxima via `sbcl.exe`.
- Sandbox tests green (SBX-\*) before exposing the server.
- SEC-02 still open on Windows pipe ACLs.
- LTI / Moodle / `adm/raw`: see official ENT wiki; **not verified** on
  wims4win in this project (and `adm/raw` is not the AlgoGebra grade path).

---

## 17. Items to test

Statuses cited below are from [docs/08](08_Test_Plan_Windows_Migration.md) as
of 2026-11-11 (master). Re-read §7 of that plan before a campaign.

### 17.1 Password and account tests (dedicated)

| ID | Procedure | Expected | Priority | Status / note |
|---|---|---|---|---|
| **PWD-01** | Site manager login at `adm/manage` from an IP in `manager_site`, with `log/.wimspass` set | maintenance UI; `wims_ismanager=2` | P0 | **NEW** — Win + Debian |
| **PWD-02** | Replace `log/.wimspass` with a new secret; confirm old fails and new works; file becomes hashed (`*` present) and not group/world readable | only new password works | P1 | **NEW** |
| **PWD-03** | Remove `log/.wimspass`; one-time password appears in `tmp/log/.wimspassone` and works once | official one-time mode | P2 | **NEW** |
| **PWD-04** | Supervisor changes own password via `adm/class/passwd` (`job=user`) | old rejected, new accepted | P1 | **NEW** (uses same `passcrypt` as LOGD-15) |
| **PWD-05** | Supervisor sets a participant password (`job=part`); student logs in | student can enter; `user_forcechpwd` as designed | P1 | **NEW** |
| **PWD-06** | Class **9001** teacher + student login and a scored exercise | scores visible to supervisor | P1 | Map: **LOGD-15 PASS** on hook-class sibling with HAVE_CRYPT CGI; **not** on live hook tree |
| **PWD-07** | CGI **without** `HAVE_CRYPT` vs fixed CGI on the same class hashes | pre-fix rejects all; post-fix accepts | P1 | **NEW** (documents PR #12); do this only on a sibling |
| **PWD-08** | Empty and wrong passwords | `bad_password` / `wrongpass`, no session upgrade | P1 | **NEW** |
| **PWD-09** | Grep / archive check: no password files in git or shared docs | no `log/.wimspass` or class secrets in the repo | P1 | **NEW** (process) |
| **PWD-10** | Create/change a module developer password in `adm/manage/developer` | login to Modtool works | P2 | **NEW** |
| SEC-01 | platform crypt / descrypt | PASS | P0 | Existing **PASS** |

### 17.2 Rights, ACL, and sandbox tests (dedicated)

| ID | Procedure | Expected | Priority | Status / note |
|---|---|---|---|---|
| **ACL-01** | Request home / `adm/manage` from an IP **not** in `manager_site` | no working management link / cannot authenticate | P0 | **NEW** |
| **ACL-02** | (Debian) `chmod` `log/wims.conf` group-readable; retry manage | `wims_ismanager=-1`, warning | P1 | **NEW**; Windows mode bits **to verify** |
| **ACL-03** | (Debian) `chmod` `log/.wimspass` group-readable | `wims_ismanager=-2` | P1 | **NEW**; Windows NTFS **to verify** |
| **ACL-04** | Windows NTFS: other interactive user cannot read `.wimspass`; Apache can write `tmp\log`, `log\classes` | access denied vs write OK | P1 | **NEW** |
| **ACL-05** | Debian: `setwrapexec` results (`wrap..exec` 6755 `nobody`) | matches script; CGI still works | P1 | **NEW** (official script) |
| **ACL-06** | Second Windows user opens `\\.\pipe\wims-wimslogd-<hash>` | deny, or documented risk | P1 | Map: **SEC-02 WAITING** |
| **ACL-07** | Sandbox cannot read `log\` or write the profile | denied, files absent | P0 | Map: **SBX-S1a/b, S2, S9, S17 PASS** |
| **ACL-08** | GAP/Octave lowil + Octave `unix()`/`system()` refused | D21/D22 behaviour | P0 | Map: **SBX-21, SBX-S6/S7 PASS** |
| **ACL-09** | Supervisor of class A cannot open class B data | denied | P1 | **NEW** |
| **ACL-10** | Student cannot open supervisor / userscore as teacher | denied | P1 | **NEW** (complement to LOGD-15) |
| **ACL-11** | Developer account cannot perform site-manager actions | denied | P2 | **NEW** |
| SEC-03, SEC-04 | related platform / sandbox rights | PASS | P0–P1 | Existing **PASS** |
| SBX-14 | purge AppContainer profiles | only with owner approval | P2 | Existing **NOT RUN** (do not run) |

### 17.3 Other admin procedures already in docs/08

| Area | Existing IDs | Notes |
|---|---|---|
| wimslogd start / IPC | LOGD-01…11, LOGD-13 | PASS; start and per-tree pipes |
| Housekeep / modupdate / backup | LOGD-12 | Scripts PASS; daemon spawn fixed in PR #13 (sibling verify 2026-11-10); live binary may still be old until cutover |
| Pid overwrite / restart | LOGD-14 | NOT RUN (disruptive) |
| Class scores | LOGD-15 | PASS on HAVE_CRYPT sibling |
| CGI / images | CGI-01, CGI-02, CGI-05, CGI-06, CGI-08 | PASS (see rows for Laragon vs direct) |
| CAS through sandbox | IFC-01…06, JOB-06 | PASS |
| Module update helper | HLP-02 | `modupdate` check mode PASS |
| Paths with spaces | PATH-01 | PASS on a copy, live intact |

### 17.4 Other new items (not passwords/ACLs)

| ID | Procedure | Priority | Note |
|---|---|---|---|
| **ADM-01** | After Windows reboot: stale pid removed or CGI bring-up; `wimslogd` live | P1 | Observed need for manual start on 2026-11-10 |
| **ADM-02** | Live + sibling daemons after reboot, stop only sibling by path | P1 | Partly done 2026-11-10 |
| **ADM-03** | `bin/backup` via daemon (`backup_hour`) writes `backup/` and log | P2 | Overlaps LOGD-12 backup; not separately recorded |
| **ADM-04** | MOTD `log/motd.phtml.fr` appears, then removal | P2 | Official upgrade practice |
| **ADM-05** | Hook opt-out `ALGOGEBRA_HOOK=0` and uninstall script | P2 | See hook doc |
| **ADM-06** | Load-threshold change vs CPU count; site still serves | P2 | Wiki/GEM; **to verify** numbers |
| **ADM-07** | Uninstall/teardown of a sibling without touching live or Claude trees | P2 | **To verify** |
| **ADM-08** | Debian package or manual upgrade + `setwrapexec` + class data intact | P1 | Official wiki; **to verify** on the target host |

**Counts:** 10 new PWD rows + 1 mapped existing (SEC-01 / LOGD-15). 11 new ACL
rows + mapped SEC-02 / SBX / SEC-03/04. 8 new ADM rows. About 20 existing
docs/08 IDs listed as already covering start, jobs, CAS, CGI, and sandbox.

---

## 18. References

- GEM report (source to take into account, not copied):
  `D:\ALF_ZGit\02-wims4win\docs\Officiel\GEM_Documentation WIMS et Format OEF.md`
- Kernel: `src/calc.c` (`_passcrypt`, `passcheck`), `src/wims.c` (`manager_check`),
  `src/config.c` (`manager_site`), `src/Platform/wp_descrypt.c`,
  `src/Platform/win32/config.h`, `adm/manage/auth.proc`, `adm/class/passwd/var.proc`
- [docs/04](04_Guide_Installation_et_CGI_Laragon.md),
  [docs/08](08_Test_Plan_Windows_Migration.md),
  [docs/05_Etat_et_Decisions.yaml](05_Etat_et_Decisions.yaml),
  [Suivi](Suivi_Developpement_Portage.md)
- Official links in §2.1
- PRs: [#12 HAVE_CRYPT](https://github.com/io-e/wims4win/pull/12),
  [#13 wimslogd `&>` share](https://github.com/io-e/wims4win/pull/13)
