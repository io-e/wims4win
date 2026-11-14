# shellcheck shell=bash
# Load WIMS test credentials from a file and/or environment. Never print values.

wims_creds_load() {
  local f="${WIMS_CREDS_FILE:-}"
  local k v line
  if [[ -n "$f" ]]; then
    if [[ ! -r "$f" ]]; then
      echo "wims-automation: cannot read creds file (path from WIMS_CREDS_FILE / --creds)" >&2
      return 1
    fi
    while IFS= read -r line || [[ -n "$line" ]]; do
      [[ "$line" =~ ^[[:space:]]*# ]] && continue
      [[ "$line" =~ ^[[:space:]]*$ ]] && continue
      k="${line%%=*}"
      v="${line#*=}"
      k="${k%"${k##*[![:space:]]}"}"
      k="${k#"${k%%[![:space:]]*}"}"
      case "$k" in
        class) WIMS_CLASS="${WIMS_CLASS:-$v}" ;;
        supervisor_login) WIMS_SUPERVISOR_LOGIN="${WIMS_SUPERVISOR_LOGIN:-$v}" ;;
        supervisor_password) WIMS_SUPERVISOR_PASSWORD="${WIMS_SUPERVISOR_PASSWORD:-$v}" ;;
        participant_login) WIMS_PARTICIPANT_LOGIN="${WIMS_PARTICIPANT_LOGIN:-$v}" ;;
        participant_password) WIMS_PARTICIPANT_PASSWORD="${WIMS_PARTICIPANT_PASSWORD:-$v}" ;;
        exercise_module) WIMS_EXERCISE_MODULE="${WIMS_EXERCISE_MODULE:-$v}" ;;
        exercise_exo) WIMS_EXERCISE_EXO="${WIMS_EXERCISE_EXO:-$v}" ;;
        exercise_sheet) WIMS_EXERCISE_SHEET="${WIMS_EXERCISE_SHEET:-$v}" ;;
      esac
    done < "$f"
  fi
  WIMS_EXERCISE_MODULE="${WIMS_EXERCISE_MODULE:-classes/fr}"
  WIMS_EXERCISE_EXO="${WIMS_EXERCISE_EXO:-exi1}"
  WIMS_EXERCISE_SHEET="${WIMS_EXERCISE_SHEET:-1}"
  if [[ -z "${WIMS_CLASS:-}" || -z "${WIMS_SUPERVISOR_LOGIN:-}" || -z "${WIMS_SUPERVISOR_PASSWORD:-}" || -z "${WIMS_PARTICIPANT_LOGIN:-}" || -z "${WIMS_PARTICIPANT_PASSWORD:-}" ]]; then
    echo "wims-automation: missing class or login fields (file and/or env)" >&2
    return 1
  fi
  return 0
}
