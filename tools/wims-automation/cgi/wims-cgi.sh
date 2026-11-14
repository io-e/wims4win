#!/usr/bin/env bash
# Simulate WIMS CGI requests directly (REQUEST_METHOD / QUERY_STRING + wims.cgi).
# Credentials from --creds or WIMS_CREDS_FILE / env; never hardcoded, never printed.
set -euo pipefail

HERE="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=../lib/load_creds.sh
source "$HERE/lib/load_creds.sh"

usage() {
  cat <<'U'
Usage: wims-cgi.sh --runtime DIR [--creds FILE] [--state-dir DIR] COMMAND

Commands:
  home                 Open the WIMS home page (module=home, cmd=intro or new)
  session              Start a session and print the session id
  supervisor-login     Log the supervisor into the class
  student-login        Log a student / participant into the class
  open-exercise        Open the configured worksheet exercise
  reply [--answer TXT] Submit cmd=reply (default answer: 1)
  scores               Show whether a score file exists; fetch supervisor userscore
  scenario             Run home → session → both logins → exercise → reply → scores

Environment: WIMS_RUNTIME, WIMS_CREDS_FILE, WIMS_CLASS, WIMS_SUPERVISOR_LOGIN,
WIMS_SUPERVISOR_PASSWORD, WIMS_PARTICIPANT_LOGIN, WIMS_PARTICIPANT_PASSWORD,
WIMS_EXERCISE_MODULE, WIMS_EXERCISE_EXO, WIMS_EXERCISE_SHEET, WIMS_STATE_DIR.
U
}

RUNTIME="${WIMS_RUNTIME:-}"
STATE_DIR="${WIMS_STATE_DIR:-}"
ANSWER="1"
CMD=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --runtime) RUNTIME="$2"; shift 2 ;;
    --creds) WIMS_CREDS_FILE="$2"; shift 2 ;;
    --state-dir) STATE_DIR="$2"; shift 2 ;;
    --answer) ANSWER="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    home|session|supervisor-login|student-login|open-exercise|reply|scores|scenario)
      CMD="$1"; shift ;;
    *) echo "wims-cgi.sh: unknown argument: $1" >&2; usage; exit 2 ;;
  esac
done

if [[ -z "$RUNTIME" || -z "$CMD" ]]; then usage; exit 2; fi
RUNTIME="$(cd "$RUNTIME" && pwd)"
PUB="$RUNTIME/public_html"
if [[ -x "$PUB/wims.cgi" ]]; then CGI="$PUB/wims.cgi"
elif [[ -x "$PUB/wims" ]]; then CGI="$PUB/wims"
else echo "wims-cgi.sh: no executable wims.cgi in $PUB" >&2; exit 1
fi

wims_creds_load

if [[ -z "$STATE_DIR" ]]; then
  STATE_DIR="${TMPDIR:-/tmp}/wims-auto-$$"
fi
mkdir -p "$STATE_DIR"
chmod 700 "$STATE_DIR" 2>/dev/null || true

urlencode() {
  python3 -c 'import sys,urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$1"
}


# encode_qs k1 v1 k2 v2 ...
encode_qs() {
  local out="" k v
  while [[ $# -gt 0 ]]; do
    k="$1"; v="$2"; shift 2
    [[ -n "$out" ]] && out+="&"
    out+="$(urlencode "$k")=$(urlencode "$v")"
  done
  printf '%s' "$out"
}

invoke_cgi() {
  local method="$1" data="$2"
  local out="$STATE_DIR/last.out" err="$STATE_DIR/last.err"
  local qs="" clen="0" ctype=""
  local bodyfile="$STATE_DIR/last.body"
  : >"$bodyfile"
  if [[ "$method" == "POST" ]]; then
    printf '%s' "$data" >"$bodyfile"
    clen="$(wc -c < "$bodyfile" | tr -d ' ')"
    ctype="application/x-www-form-urlencoded"
  else
    qs="$data"
  fi
  local -a envargs=(
    PATH="/usr/bin:/bin"
    REQUEST_METHOD="$method"
    QUERY_STRING="$qs"
    CONTENT_TYPE="$ctype"
    CONTENT_LENGTH="$clen"
    SERVER_SOFTWARE="wims-automation"
    SERVER_NAME="localhost"
    SERVER_PORT="80"
    SERVER_PROTOCOL="HTTP/1.0"
    GATEWAY_INTERFACE="CGI/1.1"
    SCRIPT_NAME="/wims/wims.cgi"
    SCRIPT_FILENAME="$CGI"
    REMOTE_ADDR="127.0.0.1"
    REMOTE_PORT="54321"
    SERVER_ADDR="127.0.0.1"
    HTTP_HOST="localhost"
    HTTP_USER_AGENT="wims-automation/cgi"
  )
  # Working directory must be public_html so the CGI can find ../bin/wimslogd.
  if [[ "$method" == "POST" ]]; then
    ( cd "$PUB" && env -i "${envargs[@]}" "$CGI" <"$bodyfile" >"$out" 2>"$err" ) || true
  else
    ( cd "$PUB" && env -i "${envargs[@]}" "$CGI" >"$out" 2>"$err" ) || true
  fi
}


parse_result() {
  # sets: HTTP_STATUS, SESSION_ID, LOCATION, BODY_LEN
  local out="$STATE_DIR/last.out"
  HTTP_STATUS="200"
  LOCATION=""
  SESSION_ID=""
  if grep -q '^Status:' "$out" 2>/dev/null; then
    HTTP_STATUS="$(grep -m1 '^Status:' "$out" | awk '{print $2}')"
  elif grep -q '^Location:' "$out" 2>/dev/null; then
    HTTP_STATUS="302"
  fi
  if grep -q '^Location:' "$out" 2>/dev/null; then
    LOCATION="$(grep -m1 '^Location:' "$out" | awk '{print $2}' | tr -d '\r')"
  fi
  if [[ "$LOCATION" =~ session=([A-Za-z0-9_.]+) ]]; then
    SESSION_ID="${BASH_REMATCH[1]}"
  else
    SESSION_ID="$(python3 - "$out" <<'PY'
import re,sys
t=open(sys.argv[1],'rb').read().decode('latin-1','replace')
m=re.search(r'session=([A-Za-z0-9_.]+)', t)
print(m.group(1) if m else '')
PY
)"
  fi
  SESSION_ID="${SESSION_ID%%.*}"
  BODY_LEN="$(wc -c < "$out" | tr -d ' ')"
}

follow_redirect() {
  parse_result
  if [[ -z "$LOCATION" ]]; then return 0; fi
  local qs="$LOCATION"
  qs="${qs#*\?}"
  # drop auth fields if a Location ever echoed them
  qs="$(python3 -c '
import sys,urllib.parse
q=sys.argv[1]
pairs=urllib.parse.parse_qsl(q, keep_blank_values=True)
deny={"auth","auth_password","pw1","pw2","old","devpasswd","passwd","password"}
out=[(k.lstrip("+"),v) for k,v in pairs if k.lstrip("+") not in deny]
print(urllib.parse.urlencode(out))
' "$qs")"
  invoke_cgi GET "$qs"
  parse_result
}

sess_user() {
  local sid="$1" key="${2:-wims_user}"
  local d="$RUNTIME/sessions/$sid"
  python3 - "$d" "$key" <<'PY'
import sys,os,re
d,key=sys.argv[1],sys.argv[2]
for name in ("var.stat","var","var.class.prep"):
    p=os.path.join(d,name)
    if not os.path.isfile(p):
        continue
    for line in open(p,'rb').read().decode('latin-1','replace').splitlines():
        if re.search(r'password|passwd', line, re.I):
            continue
        for cand in (key, "w_"+key, "w_w_"+key):
            if line.startswith(cand+"="):
                print(line.split("=",1)[1]); sys.exit(0)
print("")
PY
}

save_kv() { printf '%s' "$2" > "$STATE_DIR/$1"; }
load_kv() { [[ -f "$STATE_DIR/$1" ]] && cat "$STATE_DIR/$1" || true; }

log() { echo "$*"; }

cmd_home() {
  invoke_cgi GET "$(encode_qs module home cmd intro lang en)"
  parse_result
  if [[ -z "$SESSION_ID" ]]; then
    invoke_cgi GET "$(encode_qs module home cmd new lang en)"
    parse_result
  fi
  log "HOME status=$HTTP_STATUS session=${SESSION_ID:-none} bytes=$BODY_LEN"
  [[ -n "$SESSION_ID" ]] && save_kv session_anon "$SESSION_ID"
  [[ "$HTTP_STATUS" == "200" || "$HTTP_STATUS" == "302" ]]
}

cmd_session() {
  invoke_cgi GET "$(encode_qs module home cmd new lang en)"
  parse_result
  follow_redirect
  log "SESSION id=${SESSION_ID:-FAIL} status=$HTTP_STATUS bytes=$BODY_LEN"
  if [[ -z "$SESSION_ID" ]]; then return 1; fi
  save_kv session_anon "$SESSION_ID"
  printf '%s\n' "$SESSION_ID"
}

class_login() {
  local typ="$1" user="$2" pass="$3" label="$4" store="$5"
  invoke_cgi GET "$(encode_qs module adm/class/classes cmd new lang fr class "$WIMS_CLASS" type "$typ")"
  parse_result
  local sid0="$SESSION_ID"
  invoke_cgi POST "$(encode_qs module adm/class/classes cmd reply lang fr class "$WIMS_CLASS" type "$typ" session "$sid0" auth_user "$user" auth_password "$pass" agreecgu yes)"
  parse_result
  follow_redirect
  local u; u="$(sess_user "$SESSION_ID" wims_user)"
  [[ -z "$u" ]] && u="$(sess_user "$sid0" wims_user)"
  local prep="no"
  [[ -f "$RUNTIME/sessions/${sid0}/var.class.prep" || -f "$RUNTIME/sessions/${SESSION_ID}/var.class.prep" ]] && prep="yes"
  log "$label user=${u:-empty} status=$HTTP_STATUS session=${SESSION_ID:-none} prep=$prep bytes=$BODY_LEN"
  if [[ -z "$u" ]]; then return 1; fi
  save_kv "$store" "$SESSION_ID"
  save_kv "${store}_user" "$u"
  return 0
}

cmd_supervisor_login() {
  class_login authsupervisor "$WIMS_SUPERVISOR_LOGIN" "$WIMS_SUPERVISOR_PASSWORD" SUPERVISOR session_sup
}

cmd_student_login() {
  class_login authparticipant "$WIMS_PARTICIPANT_LOGIN" "$WIMS_PARTICIPANT_PASSWORD" STUDENT session_stu
}

cmd_open_exercise() {
  local sid; sid="$(load_kv session_stu)"
  if [[ -z "$sid" ]]; then
    cmd_student_login
    sid="$(load_kv session_stu)"
  fi
  # Official class-home shape: +cmd=new&+worksheet=N&+module=...&+exo=... (literal plus in QUERY_STRING).
  local qs="session=${sid}&+cmd=new&+worksheet=${WIMS_EXERCISE_SHEET}&+module=${WIMS_EXERCISE_MODULE}&+exo=${WIMS_EXERCISE_EXO}&+lang=fr&qnum=1"
  invoke_cgi GET "$qs"
  parse_result
  follow_redirect
  local forms=0
  forms="$(grep -ci '<form' "$STATE_DIR/last.out" || true)"
  log "EXERCISE status=$HTTP_STATUS session=${SESSION_ID:-$sid} bytes=$BODY_LEN forms=$forms module=$WIMS_EXERCISE_MODULE exo=$WIMS_EXERCISE_EXO worksheet=$WIMS_EXERCISE_SHEET"
  save_kv session_stu "${SESSION_ID:-$sid}"
  # keep a copy of the form page for reply
  cp -f "$STATE_DIR/last.out" "$STATE_DIR/exercise.out"
  # consider success if we got a body and no obvious login form bounce
  [[ "$BODY_LEN" -gt 200 && "$HTTP_STATUS" != "500" ]]
}

# Extract form fields from the last exercise page and POST cmd=reply.
cmd_reply() {
  local sid; sid="$(load_kv session_stu)"
  [[ -n "$sid" ]] || { echo "reply: no student session (run student-login)" >&2; return 1; }
  if [[ ! -f "$STATE_DIR/exercise.out" ]]; then
    cmd_open_exercise || true
  fi
  local body
  body="$(python3 - "$STATE_DIR/exercise.out" "$sid" "$ANSWER" "$WIMS_EXERCISE_MODULE" "$WIMS_EXERCISE_EXO" "$WIMS_EXERCISE_SHEET" <<'PY'
import sys,re,urllib.parse
html=open(sys.argv[1],'rb').read().decode('latin-1','replace')
sid,answer,mod,exo,sheet=sys.argv[2:7]
# drop script/style
html=re.sub(r'(?is)<script[^>]*>.*?</script>','',html)
html=re.sub(r'(?is)<style[^>]*>.*?</style>','',html)
fields=[]
# pick the first form that mentions reply or session
forms=re.findall(r'(?is)<form[^>]*>.*?</form>', html)
chosen=forms[0] if forms else html
for m in re.finditer(r'(?is)<input\b[^>]*>', chosen):
    tag=m.group(0)
    typ=re.search(r'(?i)\btype=["\']?(\w+)', tag)
    typ=typ.group(1).lower() if typ else 'text'
    if typ in ('file','image','button','submit','reset'):
        # keep hidden submit? skip
        if typ != 'hidden':
            continue
    name=re.search(r'(?i)\bname=["\']?([^"\'\s>]+)', tag)
    if not name: continue
    name=name.group(1)
    if re.search(r'(?i)password|passwd', name): continue
    val=re.search(r'(?i)\bvalue=["\']([^"\']*)', tag)
    val=val.group(1) if val else ''
    fields.append((name,val))
for m in re.finditer(r'(?is)<textarea\b[^>]*name=["\']?([^"\'\s>]+)[^>]*>(.*?)</textarea>', chosen):
    if re.search(r'(?i)password|passwd', m.group(1)): continue
    fields.append((m.group(1), re.sub(r'(?s)<[^>]+>','',m.group(2))))
# radio/select: keep first option if any
d={}
for k,v in fields:
    if k not in d: d[k]=v
d['cmd']='reply'
d['session']=d.get('session') or sid
d['module']=d.get('module') or mod
d['exo']=d.get('exo') or exo
d['sheet']=d.get('sheet') or sheet
# common OEF answer field names
for key in ('reply','wims_answer','oefanswer','rep','answer','reponse','réponse'):
    if key in d and not d[key]:
        d[key]=answer
# if no obvious answer field, set reply
if not any(k.lower() in ('reply','wims_answer','oefanswer','rep','answer') for k in d):
    d['reply']=answer
# also fill empty text-like fields that look like answers (q1, r1, ch1...)
for k,v in list(d.items()):
    if not v and re.match(r'(?i)^(q|r|rep|ch|choice|item)\d*$', k):
        d[k]=answer
pairs=[(k,v) for k,v in d.items() if not re.search(r'(?i)password|passwd', k)]
print(urllib.parse.urlencode(pairs))
PY
)"
  invoke_cgi POST "$body"
  parse_result
  follow_redirect
  log "REPLY status=$HTTP_STATUS session=${SESSION_ID:-$sid} bytes=$BODY_LEN"
  save_kv session_stu "${SESSION_ID:-$sid}"
  [[ "$HTTP_STATUS" == "200" || "$HTTP_STATUS" == "302" ]]
}

cmd_scores() {
  local stu_user; stu_user="$(load_kv session_stu_user)"
  local classdir="$RUNTIME/log/classes/$WIMS_CLASS/score"
  local found="none"
  if [[ -d "$classdir" ]]; then
    # list score file names only (not contents — may include IPs)
    found="$(ls -1 "$classdir" | tr '\n' ' ')"
  fi
  log "SCORES files=${found:-none} student_user=${stu_user:-unknown}"
  local sid; sid="$(load_kv session_sup)"
  if [[ -z "$sid" ]]; then
    cmd_supervisor_login
    sid="$(load_kv session_sup)"
  fi
  invoke_cgi GET "$(encode_qs module adm/class/userscore cmd new lang fr session "$sid" class "$WIMS_CLASS")"
  parse_result
  follow_redirect
  local notes="no"
  if grep -qiE 'notes|feuille|sheet|score|utilisateur|userscore' "$STATE_DIR/last.out"; then notes="yes"; fi
  log "NOTES status=$HTTP_STATUS bytes=$BODY_LEN markers=$notes session=${SESSION_ID:-$sid}"
  [[ "$HTTP_STATUS" == "200" || "$HTTP_STATUS" == "302" ]]
}

cmd_scenario() {
  local rc=0
  cmd_home || rc=1
  cmd_session >/dev/null || rc=1
  cmd_supervisor_login || rc=1
  cmd_student_login || rc=1
  cmd_open_exercise || rc=1
  cmd_reply || rc=1
  cmd_scores || rc=1
  if [[ $rc -eq 0 ]]; then log "SCENARIO PASS"; else log "SCENARIO FAIL"; fi
  return $rc
}

case "$CMD" in
  home) cmd_home ;;
  session) cmd_session ;;
  supervisor-login) cmd_supervisor_login ;;
  student-login) cmd_student_login ;;
  open-exercise) cmd_open_exercise ;;
  reply) cmd_reply ;;
  scores) cmd_scores ;;
  scenario) cmd_scenario ;;
esac
