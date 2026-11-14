#!/usr/bin/env bash
# curl-based WIMS HTTP client with a cookie jar. Credentials from --creds / env only.
set -euo pipefail

HERE="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=../lib/load_creds.sh
source "$HERE/lib/load_creds.sh"

usage() {
  cat <<'U'
Usage: wims-http.sh --base-url URL [--creds FILE] [--state-dir DIR] [--answer TXT] COMMAND

--base-url is the CGI URL, e.g. http://127.0.0.1:8765/wims/wims.cgi
Commands: home, session, supervisor-login, student-login, open-exercise, reply, scores, scenario

Uses curl (curl.exe on Windows if that is what the environment provides).
U
}

BASE_URL="${WIMS_BASE_URL:-}"
STATE_DIR="${WIMS_STATE_DIR:-}"
ANSWER="1"
CMD=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url) BASE_URL="$2"; shift 2 ;;
    --creds) WIMS_CREDS_FILE="$2"; shift 2 ;;
    --state-dir) STATE_DIR="$2"; shift 2 ;;
    --answer) ANSWER="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    home|session|supervisor-login|student-login|open-exercise|reply|scores|scenario)
      CMD="$1"; shift ;;
    *) echo "wims-http.sh: unknown argument: $1" >&2; usage; exit 2 ;;
  esac
done

if [[ -z "$BASE_URL" || -z "$CMD" ]]; then usage; exit 2; fi
wims_creds_load

if [[ -z "$STATE_DIR" ]]; then STATE_DIR="${TMPDIR:-/tmp}/wims-http-$$"; fi
mkdir -p "$STATE_DIR"
chmod 700 "$STATE_DIR" 2>/dev/null || true
JAR="$STATE_DIR/cookies.txt"
: >"$JAR"

CURL="curl"
command -v curl.exe >/dev/null 2>&1 && CURL="curl.exe"

# write body to last.out; status to last.status; do not follow redirects automatically
# (we extract the session from Location ourselves).
http() {
  local method="$1" data="$2"
  local out="$STATE_DIR/last.out" hdr="$STATE_DIR/last.hdr"
  if [[ "$method" == POST ]]; then
    "$CURL" -sS -D "$hdr" -o "$out" -b "$JAR" -c "$JAR" \
      -X POST -H "Content-Type: application/x-www-form-urlencoded" \
      --data "$data" \
      "$BASE_URL" || true
  else
    local url="$BASE_URL"
    [[ -n "$data" ]] && url="${BASE_URL}?${data}"
    "$CURL" -sS -D "$hdr" -o "$out" -b "$JAR" -c "$JAR" \
      "$url" || true
  fi
}

urlencode() {
  python3 -c 'import sys,urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$1"
}
encode_qs() {
  local out="" k v
  while [[ $# -gt 0 ]]; do
    k="$1"; v="$2"; shift 2
    [[ -n "$out" ]] && out+="&"
    out+="$(urlencode "$k")=$(urlencode "$v")"
  done
  printf '%s' "$out"
}

parse_result() {
  local hdr="$STATE_DIR/last.hdr" out="$STATE_DIR/last.out"
  HTTP_STATUS="$(awk 'toupper($1) ~ /^HTTP\// {s=$2} END{print s+0}' "$hdr" 2>/dev/null || echo 0)"
  LOCATION="$(awk 'BEGIN{IGNORECASE=1} /^Location:/{print $2}' "$hdr" | tr -d '\r' | tail -1)"
  SESSION_ID=""
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
  [[ -n "$LOCATION" ]] || return 0
  local qs="${LOCATION#*\?}"
  if [[ "$qs" == "$LOCATION" ]]; then
    # absolute without query — still GET it
    "$CURL" -sS -D "$STATE_DIR/last.hdr" -o "$STATE_DIR/last.out" -b "$JAR" -c "$JAR" "$LOCATION" || true
    parse_result
    return 0
  fi
  qs="$(python3 -c '
import sys,urllib.parse
pairs=urllib.parse.parse_qsl(sys.argv[1], keep_blank_values=True)
deny={"auth","auth_password","pw1","pw2","old","devpasswd","passwd","password"}
out=[(k.lstrip("+"),v) for k,v in pairs if k.lstrip("+") not in deny]
print(urllib.parse.urlencode(out))
' "$qs")"
  http GET "$qs"
  parse_result
}

save_kv() { printf '%s' "$2" > "$STATE_DIR/$1"; }
load_kv() { [[ -f "$STATE_DIR/$1" ]] && cat "$STATE_DIR/$1" || true; }
log() { echo "$*"; }

cmd_home() {
  http GET "$(encode_qs module home cmd intro lang en)"
  parse_result
  if [[ -z "$SESSION_ID" ]]; then
    http GET "$(encode_qs module home cmd new lang en)"
    parse_result
    follow_redirect
  fi
  log "HOME status=$HTTP_STATUS session=${SESSION_ID:-none} bytes=$BODY_LEN"
  [[ -n "$SESSION_ID" ]] && save_kv session_anon "$SESSION_ID"
  [[ "$HTTP_STATUS" == "200" || "$HTTP_STATUS" == "302" ]]
}

cmd_session() {
  http GET "$(encode_qs module home cmd new lang en)"
  parse_result
  follow_redirect
  log "SESSION id=${SESSION_ID:-FAIL} status=$HTTP_STATUS bytes=$BODY_LEN"
  [[ -n "$SESSION_ID" ]] || return 1
  save_kv session_anon "$SESSION_ID"
  printf '%s\n' "$SESSION_ID"
}

class_login() {
  local typ="$1" user="$2" pass="$3" label="$4" store="$5"
  http GET "$(encode_qs module adm/class/classes cmd new lang fr class "$WIMS_CLASS" type "$typ")"
  parse_result
  local sid0="$SESSION_ID"
  http POST "$(encode_qs module adm/class/classes cmd reply lang fr class "$WIMS_CLASS" type "$typ" session "$sid0" auth_user "$user" auth_password "$pass" agreecgu yes)"
  parse_result
  follow_redirect
  log "$label status=$HTTP_STATUS session=${SESSION_ID:-none} bytes=$BODY_LEN (user not read from disk in HTTP mode)"
  # HTTP success: 302 or a page that is not the login form
  if [[ "$HTTP_STATUS" == "302" || ( "$HTTP_STATUS" == "200" && "$BODY_LEN" -lt 8000 && -n "$SESSION_ID" ) ]]; then
    save_kv "$store" "${SESSION_ID:-$sid0}"
    return 0
  fi
  # 200 with a large login form is a fail; 200 after redirect-followed home is ok if session changed
  if [[ -n "$SESSION_ID" && "$SESSION_ID" != "$sid0" ]]; then
    save_kv "$store" "$SESSION_ID"
    return 0
  fi
  return 1
}

cmd_supervisor_login() { class_login authsupervisor "$WIMS_SUPERVISOR_LOGIN" "$WIMS_SUPERVISOR_PASSWORD" SUPERVISOR session_sup; }
cmd_student_login() { class_login authparticipant "$WIMS_PARTICIPANT_LOGIN" "$WIMS_PARTICIPANT_PASSWORD" STUDENT session_stu; }

cmd_open_exercise() {
  local sid; sid="$(load_kv session_stu)"
  if [[ -z "$sid" ]]; then
    cmd_student_login
    sid="$(load_kv session_stu)"
  fi
  # Encode plus-prefixed WIMS params as %2B so HTTP does not treat + as space.
  local qs="session=${sid}&%2Bcmd=new&%2Bworksheet=${WIMS_EXERCISE_SHEET}&%2Bmodule=$(urlencode "$WIMS_EXERCISE_MODULE")&%2Bexo=$(urlencode "$WIMS_EXERCISE_EXO")&%2Blang=fr&qnum=1"
  http GET "$qs"
  parse_result
  follow_redirect
  local forms=0
  forms="$(grep -ci '<form' "$STATE_DIR/last.out" || true)"
  log "EXERCISE status=$HTTP_STATUS session=${SESSION_ID:-$sid} bytes=$BODY_LEN forms=$forms module=$WIMS_EXERCISE_MODULE exo=$WIMS_EXERCISE_EXO worksheet=$WIMS_EXERCISE_SHEET"
  save_kv session_stu "${SESSION_ID:-$sid}"
  cp -f "$STATE_DIR/last.out" "$STATE_DIR/exercise.out"
  [[ "$BODY_LEN" -gt 200 && "$HTTP_STATUS" != "500" ]]
}

cmd_reply() {
  local sid; sid="$(load_kv session_stu)"
  [[ -n "$sid" ]] || { echo "reply: no student session" >&2; return 1; }
  [[ -f "$STATE_DIR/exercise.out" ]] || cmd_open_exercise || true
  local body
  body="$(python3 - "$STATE_DIR/exercise.out" "$sid" "$ANSWER" "$WIMS_EXERCISE_MODULE" "$WIMS_EXERCISE_EXO" "$WIMS_EXERCISE_SHEET" <<'PY'
import sys,re,urllib.parse
html=open(sys.argv[1],'rb').read().decode('latin-1','replace')
sid,answer,mod,exo,sheet=sys.argv[2:7]
html=re.sub(r'(?is)<script[^>]*>.*?</script>','',html)
fields=[]
forms=re.findall(r'(?is)<form[^>]*>.*?</form>', html)
chosen=forms[0] if forms else html
for m in re.finditer(r'(?is)<input\b[^>]*>', chosen):
    tag=m.group(0)
    typ=re.search(r'(?i)\btype=["\']?(\w+)', tag)
    typ=typ.group(1).lower() if typ else 'text'
    if typ in ('file','image','button','submit','reset'):
        continue
    name=re.search(r'(?i)\bname=["\']?([^"\'\s>]+)', tag)
    if not name: continue
    name=name.group(1)
    if re.search(r'(?i)password|passwd', name): continue
    val=re.search(r'(?i)\bvalue=["\']([^"\']*)', tag)
    val=val.group(1) if val else ''
    fields.append((name,val))
d={}
for k,v in fields:
    if k not in d: d[k]=v
d['cmd']='reply'
d['session']=d.get('session') or sid
d['module']=d.get('module') or mod
d['exo']=d.get('exo') or exo
d['sheet']=d.get('sheet') or sheet
for key in ('reply','wims_answer','oefanswer','rep','answer'):
    if key in d and not d[key]:
        d[key]=answer
if not any(k.lower() in ('reply','wims_answer','oefanswer','rep','answer') for k in d):
    d['reply']=answer
pairs=[(k,v) for k,v in d.items() if not re.search(r'(?i)password|passwd', k)]
print(urllib.parse.urlencode(pairs))
PY
)"
  http POST "$body"
  parse_result
  follow_redirect
  log "REPLY status=$HTTP_STATUS session=${SESSION_ID:-$sid} bytes=$BODY_LEN"
  save_kv session_stu "${SESSION_ID:-$sid}"
  [[ "$HTTP_STATUS" == "200" || "$HTTP_STATUS" == "302" ]]
}

cmd_scores() {
  local sid; sid="$(load_kv session_sup)"
  if [[ -z "$sid" ]]; then
    cmd_supervisor_login
    sid="$(load_kv session_sup)"
  fi
  http GET "$(encode_qs module adm/class/userscore cmd new lang fr session "$sid" class "$WIMS_CLASS")"
  parse_result
  follow_redirect
  local notes="no"
  grep -qiE 'notes|feuille|sheet|score|utilisateur|userscore' "$STATE_DIR/last.out" && notes="yes"
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
