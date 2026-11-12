# Load WIMS test credentials from a file and/or environment. Never print values.
function Get-WimsCreds {
  param([string]$CredsFile)
  $file = $CredsFile
  if (-not $file) { $file = $env:WIMS_CREDS_FILE }
  $h = @{}
  if ($file) {
    if (-not (Test-Path -LiteralPath $file)) { throw "wims-automation: cannot read creds file" }
    Get-Content -LiteralPath $file | ForEach-Object {
      if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
      if ($_ -match '^([^=]+)=(.*)$') { $h[$Matches[1].Trim()] = $Matches[2].Trim() }
    }
  }
  function Pick([string]$envName, [string]$key, [string]$default = '') {
    $ev = [Environment]::GetEnvironmentVariable($envName)
    if ($ev) { return $ev }
    if ($h.ContainsKey($key) -and $h[$key]) { return $h[$key] }
    return $default
  }
  $c = @{
    Class              = Pick 'WIMS_CLASS' 'class'
    SupervisorLogin    = Pick 'WIMS_SUPERVISOR_LOGIN' 'supervisor_login'
    SupervisorPassword = Pick 'WIMS_SUPERVISOR_PASSWORD' 'supervisor_password'
    ParticipantLogin   = Pick 'WIMS_PARTICIPANT_LOGIN' 'participant_login'
    ParticipantPassword= Pick 'WIMS_PARTICIPANT_PASSWORD' 'participant_password'
    ExerciseModule     = Pick 'WIMS_EXERCISE_MODULE' 'exercise_module' 'classes/fr'
    ExerciseExo        = Pick 'WIMS_EXERCISE_EXO' 'exercise_exo' 'exi1'
    ExerciseSheet      = Pick 'WIMS_EXERCISE_SHEET' 'exercise_sheet' '1'
  }
  if (-not $c.Class -or -not $c.SupervisorLogin -or -not $c.SupervisorPassword -or -not $c.ParticipantLogin -or -not $c.ParticipantPassword) {
    throw 'wims-automation: missing class or login fields (file and/or env)'
  }
  return $c
}

# Prefer wims.cgi.exe on Windows so a stale extensionless wims.cgi cannot be picked
# (Get-ChildItem wims.cgi* returns the no-extension name first; see docs/11 BKP-06).
function Get-WimsCgiPath {
  param([string]$RuntimePath)
  $ph = Join-Path $RuntimePath 'public_html'
  $exe = Join-Path $ph 'wims.cgi.exe'
  if (Test-Path -LiteralPath $exe) { return $exe }
  foreach ($name in @('wims.cgi','wims')) {
    $p = Join-Path $ph $name
    if (Test-Path -LiteralPath $p) { return $p }
  }
  throw "wims-automation: no wims.cgi in $ph"
}
