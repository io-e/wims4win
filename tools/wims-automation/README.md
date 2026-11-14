# WIMS automation scripts

Small helpers to drive a **local** WIMS 4.33a / wims4win instance for setup
checks: open the home page, start a session, log a supervisor and a student
into a class, open a worksheet exercise, submit `cmd=reply`, and read scores /
the teacher **Notes** page (`adm/class/userscore`).

They are meant for a lab sibling or a Debian tree you already run, not for
load tests and not against a production site you do not control.

There are three layers, in increasing dependency:

1. **CGI-direct** (`cgi/`) — sets `REQUEST_METHOD` / `QUERY_STRING` (POST body
   on stdin) and execs `wims.cgi`. No HTTP server required.
2. **HTTP + curl** (`http/`) — `curl` (on Windows, `curl.exe`) with a cookie
   jar, talking to a CGI URL. Use this when a front-end already exists, or
   with the localhost wrapper below.
3. **Playwright** (`playwright/`) — optional browser end-to-end test,
   student → exercise → teacher Notes. Install only if you want it.

Bash scripts are for Debian/Linux. PowerShell scripts are for Windows
(Windows PowerShell 5.1 is enough; do not use `.bat` / `cmd.exe`).

## Credentials

**Never hardcode passwords.** Pass a file path and/or environment variables.

- `--creds FILE` or `WIMS_CREDS_FILE`
- or: `WIMS_CLASS`, `WIMS_SUPERVISOR_LOGIN`, `WIMS_SUPERVISOR_PASSWORD`,
  `WIMS_PARTICIPANT_LOGIN`, `WIMS_PARTICIPANT_PASSWORD`

File format (see `creds.example`; comments start with `#`):

```
class=9001
supervisor_login=...
supervisor_password=...
participant_login=...
participant_password=...
```

Optional file keys / env vars for the default worksheet exercise:

- `exercise_module` / `WIMS_EXERCISE_MODULE` (default `classes/fr`)
- `exercise_exo` / `WIMS_EXERCISE_EXO` (default `exi1`)
- `exercise_sheet` / `WIMS_EXERCISE_SHEET` (default `1`)

Those defaults match class 9001 tutorial sheet 1 on the lab trees. Keep the
real creds file **outside git** (for example
`C:\Users\Shadow\wims_test_creds.txt` or a mode-600 file under `/tmp`).
The scripts never print password values.

## CGI-direct

Debian / Linux:

```bash
export WIMS_RUNTIME=/workspace/mw-wims-setup-debian
export WIMS_CREDS_FILE=/path/to/creds
# start that tree's wimslogd first (working directory = runtime root)
tools/wims-automation/cgi/wims-cgi.sh --runtime "$WIMS_RUNTIME" --creds "$WIMS_CREDS_FILE" scenario
```

Windows (PowerShell):

```powershell
$rt = 'D:\path\to\wims-runtime-r19040-grok-setup-test-20261112'
$creds = 'C:\Users\Shadow\wims_test_creds.txt'
# Start that sibling's bin\wimslogd.exe with WorkingDirectory = $rt, then:
.\cgi\wims-cgi.ps1 -Runtime $rt -Creds $creds scenario
```

On Windows the CGI helper **prefers `public_html\wims.cgi.exe`**. It does not
use `Get-ChildItem wims.cgi* | Select-Object -First 1`, because that glob
picks a stale extensionless `wims.cgi` first. A leftover pre-`HAVE_CRYPT`
binary in that name will fail every class login (see docs/11, BKP-06).

Individual commands: `home`, `session`, `supervisor-login`, `student-login`,
`open-exercise`, `reply`, `scores`. `session` prints the session id.
`--state-dir` / `WIMS_STATE_DIR` keeps last responses and session ids
(no passwords). `--answer` is the dummy reply value (default `1`; a wrong
answer is enough to exercise `cmd=reply`).

Class login uses POST fields `auth_user`, `auth_password`, and `agreecgu=yes`
(not `login=`).

Stop any **test** `wimslogd` you started **by executable path**, never by
image name alone.

## HTTP + curl

Point `--base-url` at the CGI, for example
`http://127.0.0.1:8765/wims/wims.cgi`. A cookie jar is stored in the state
directory (`cookies.txt`). WIMS itself usually carries the session in the
query string; the jar is still used so cookies are honoured if the front-end
sets any.

```bash
# Localhost-only wrapper (does not change Apache / Laragon; binds 127.0.0.1).
# Refuses the live lab tree wims-runtime-r19040-grok-hook-20261106.
python3 tools/wims-automation/http/dev-cgi-server.py \
  --runtime /workspace/mw-wims-setup-debian --port 8765

tools/wims-automation/http/wims-http.sh \
  --base-url http://127.0.0.1:8765/wims/wims.cgi \
  --creds /path/to/creds scenario
```

Windows:

```powershell
python dev-cgi-server.py --runtime $rt --port 8765
.\http\wims-http.ps1 -BaseUrl 'http://127.0.0.1:8765/wims/wims.cgi' -Creds $creds scenario
```

Do **not** point these scripts at the live Laragon vhost unless the owner has
cut `WIMS_RT` over to a test sibling. The wrapper is the safe default.

## Playwright (optional)

```bash
pip install -r tools/wims-automation/playwright/requirements.txt
playwright install chromium

python3 tools/wims-automation/playwright/test_student_exercise_notes.py \
  --base-url http://127.0.0.1:8765/wims/wims.cgi \
  --creds /path/to/creds
```

The test logs a student in, opens the configured exercise, submits a reply,
then opens the supervisor userscore / Notes page in a second page. If
Playwright is not installed, the script exits with status 2 and a skip
message. Pass `--headed` to watch the browser.


Class worksheet links use WIMS plus-prefixed parameters, for example:

```
session=SID&+cmd=new&+worksheet=1&+module=classes/fr&+exo=exi1&qnum=1
```

In a CGI `QUERY_STRING` the `+` must be a literal plus. Over HTTP, send it as
`%2B` (`+` is a space in form-urlencoded query strings). The localhost wrapper
(`http/dev-cgi-server.py`) decodes `%2B` with `unquote` (not `unquote_plus`).

On the class 9001 lab trees those parameters are accepted (`worksheet=1`,
`exo=exi1` in the session) but the module may still render the class-exercise
group intro instead of the OEF form; treat `open-exercise` / `reply` as
best-effort until that path is confirmed in a browser on a vhost. Home,
session, class logins, and teacher Notes (`adm/class/userscore`) are the
reliable checks.

## About `adm/raw`

WIMS ships an `adm/raw` module for machine-to-machine calls from a
**configured** peer (see `public_html/modules/adm/raw/protocol.md`).
Authentication is a declared identifier (`ident`) plus a password stored
under `log/classes/.connections/`, and the client IP must appear in that
peer’s `ident_site` list (a User-Agent and https constraint can also apply).

These scripts **do not** use `adm/raw`. It is a site-to-site / identifier+IP
hook, not a path used for grading, and nothing here records or changes scores
through it. Student scores in this pack go through a normal class login and
`cmd=reply` on a worksheet exercise; the teacher view is `adm/class/userscore`.

## What this pack does not do

- No load tests, no parallel floods, no production cutover.
- No credentials in the repo, in logs, or in git history.
- No changes to a live runtime, Laragon config, or a tree you did not pass
  in `--runtime` / `--base-url`.
