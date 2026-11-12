# curl.exe-based WIMS HTTP client with a cookie jar. Credentials from -Creds / env only.
[CmdletBinding()]
param(
  [string]$BaseUrl,
  [string]$Creds,
  [string]$StateDir,
  [string]$Answer = '1',
  [Parameter(Position=0)][string]$Command,
  [switch]$Help
)
$ErrorActionPreference = 'Stop'
$Here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $Here '..\lib\load_creds.ps1')

function Show-Usage {
  @"
Usage: wims-http.ps1 -BaseUrl URL [-Creds FILE] [-StateDir DIR] [-Answer TXT] COMMAND

-BaseUrl is the CGI URL, e.g. http://127.0.0.1:8765/wims/wims.cgi
Commands: home, session, supervisor-login, student-login, open-exercise, reply, scores, scenario
Uses curl.exe when present, otherwise curl.
"@
}
if ($Help -or -not $Command) { Show-Usage; if ($Help) { exit 0 } else { exit 2 } }
if (-not $BaseUrl) { $BaseUrl = $env:WIMS_BASE_URL }
if (-not $BaseUrl) { Show-Usage; exit 2 }
$CredsObj = Get-WimsCreds -CredsFile $Creds
if (-not $StateDir) { $StateDir = $env:WIMS_STATE_DIR }
if (-not $StateDir) { $StateDir = Join-Path $env:TEMP ("wims-http-" + [guid]::NewGuid().ToString('N')) }
New-Item -ItemType Directory -Force -Path $StateDir | Out-Null
$Jar = Join-Path $StateDir 'cookies.txt'
if (-not (Test-Path -LiteralPath $Jar)) { New-Item -ItemType File -Path $Jar | Out-Null }

$Curl = 'curl.exe'
if (-not (Get-Command $Curl -ErrorAction SilentlyContinue)) {
  if (Get-Command curl -ErrorAction SilentlyContinue) { $Curl = 'curl' }
  else { throw 'curl.exe / curl not found' }
}

function Save-Kv([string]$k,[string]$v){ Set-Content -LiteralPath (Join-Path $StateDir $k) -Value $v -Encoding ASCII -NoNewline }
function Load-Kv([string]$k){ $p=Join-Path $StateDir $k; if(Test-Path -LiteralPath $p){ return (Get-Content -LiteralPath $p -Raw) } else { return '' } }

function Encode-Qs([hashtable]$Params) {
  $pairs = @()
  foreach ($k in $Params.Keys) {
    $pairs += ([uri]::EscapeDataString([string]$k) + '=' + [uri]::EscapeDataString([string]$Params[$k]))
  }
  return [string]::Join('&', $pairs)
}

function Invoke-Http {
  param([string]$Method, [string]$Data)
  $out = Join-Path $StateDir 'last.out'
  $hdr = Join-Path $StateDir 'last.hdr'
  if ($Method -eq 'POST') {
    & $Curl -sS -D $hdr -o $out -b $Jar -c $Jar -X POST `
      -H 'Content-Type: application/x-www-form-urlencoded' --data $Data $BaseUrl | Out-Null
  } else {
    $url = $BaseUrl
    if ($Data) { $url = "$BaseUrl`?$Data" }
    & $Curl -sS -D $hdr -o $out -b $Jar -c $Jar $url | Out-Null
  }
}

function Parse-Result {
  $hdr = Get-Content -LiteralPath (Join-Path $StateDir 'last.hdr') -ErrorAction SilentlyContinue
  $st = '0'
  $loc = ''
  foreach ($line in $hdr) {
    if ($line -match '^HTTP/\S+\s+(\d+)') { $st = $Matches[1] }
    if ($line -match '(?i)^Location:\s*(\S+)') { $loc = $Matches[1].Trim() }
  }
  $body = ''
  $bp = Join-Path $StateDir 'last.out'
  if (Test-Path -LiteralPath $bp) { $body = Get-Content -LiteralPath $bp -Raw -Encoding Default }
  $sess = ''
  if ($loc -match 'session=([A-Za-z0-9_.]+)') { $sess = $Matches[1].Split('.')[0] }
  elseif ($body -match 'session=([A-Za-z0-9_.]+)') { $sess = $Matches[1].Split('.')[0] }
  return @{ status=$st; location=$loc; session=$sess; out=$body; len=$(if($body){$body.Length}else{0}) }
}

function Follow-Redirect($r) {
  if (-not $r.location) { return $r }
  $q = $r.location
  if ($q.Contains('?')) { $q = $q.Split('?',2)[1] } else {
    & $Curl -sS -D (Join-Path $StateDir 'last.hdr') -o (Join-Path $StateDir 'last.out') -b $Jar -c $Jar $r.location | Out-Null
    return (Parse-Result)
  }
  $ht = @{}
  foreach ($part in ($q -split '&')) {
    $pp = $part.TrimStart('+')
    if ($pp -match '^([^=]+)=(.*)$') {
      $k = $Matches[1].TrimStart('+')
      if ($k -in @('auth','auth_password','pw1','pw2','old','devpasswd','passwd','password')) { return $r }
      $ht[$k] = [uri]::UnescapeDataString($Matches[2])
    }
  }
  Invoke-Http -Method GET -Data (Encode-Qs $ht)
  return (Parse-Result)
}

function Invoke-Home {
  Invoke-Http GET (Encode-Qs @{ module='home'; cmd='intro'; lang='en' })
  $r = Parse-Result
  if (-not $r.session) {
    Invoke-Http GET (Encode-Qs @{ module='home'; cmd='new'; lang='en' })
    $r = Follow-Redirect (Parse-Result)
  }
  Write-Output "HOME status=$($r.status) session=$(if($r.session){$r.session}else{'none'}) bytes=$($r.len)"
  if ($r.session) { Save-Kv 'session_anon' $r.session }
  return ($r.status -eq '200' -or $r.status -eq '302')
}

function Invoke-Session {
  Invoke-Http GET (Encode-Qs @{ module='home'; cmd='new'; lang='en' })
  $r = Follow-Redirect (Parse-Result)
  Write-Output "SESSION id=$(if($r.session){$r.session}else{'FAIL'}) status=$($r.status) bytes=$($r.len)"
  if (-not $r.session) { return $false }
  Save-Kv 'session_anon' $r.session
  Write-Output $r.session
  return $true
}

function Invoke-ClassLogin([string]$typ,[string]$user,[string]$pass,[string]$label,[string]$store) {
  Invoke-Http GET (Encode-Qs @{ module='adm/class/classes'; cmd='new'; lang='fr'; class=$CredsObj.Class; type=$typ })
  $r0 = Parse-Result
  Invoke-Http POST (Encode-Qs @{ module='adm/class/classes'; cmd='reply'; lang='fr'; class=$CredsObj.Class; type=$typ; session=$r0.session; auth_user=$user; auth_password=$pass; agreecgu='yes' })
  $r1 = Follow-Redirect (Parse-Result)
  $sid = if ($r1.session) { $r1.session } else { $r0.session }
  Write-Output "$label status=$($r1.status) session=$(if($sid){$sid}else{'none'}) bytes=$($r1.len) (user not read from disk in HTTP mode)"
  if ($r1.status -eq '302' -or ($r1.session -and $r1.session -ne $r0.session) -or ($r1.status -eq '200' -and $r1.len -lt 8000 -and $r1.session)) {
    Save-Kv $store $sid
    return $true
  }
  return $false
}

function Invoke-OpenExercise {
  $sid = Load-Kv 'session_stu'
  if (-not $sid) {
    if (-not (Invoke-ClassLogin 'authparticipant' $CredsObj.ParticipantLogin $CredsObj.ParticipantPassword 'STUDENT' 'session_stu')) { return $false }
    $sid = Load-Kv 'session_stu'
  }
  Invoke-Http GET (Encode-Qs @{ module=$CredsObj.ExerciseModule; cmd='new'; lang='fr'; session=$sid; exo=$CredsObj.ExerciseExo; sheet=$CredsObj.ExerciseSheet; qnum='1' })
  $r = Follow-Redirect (Parse-Result)
  $sid2 = if ($r.session) { $r.session } else { $sid }
  Write-Output "EXERCISE status=$($r.status) session=$sid2 bytes=$($r.len) module=$($CredsObj.ExerciseModule) exo=$($CredsObj.ExerciseExo)"
  Save-Kv 'session_stu' $sid2
  Copy-Item -LiteralPath (Join-Path $StateDir 'last.out') -Destination (Join-Path $StateDir 'exercise.out') -Force
  return ($r.len -gt 200 -and $r.status -ne '500')
}

function Invoke-Reply {
  $sid = Load-Kv 'session_stu'
  if (-not $sid) { Write-Output 'reply: no student session'; return $false }
  if (-not (Test-Path -LiteralPath (Join-Path $StateDir 'exercise.out'))) { [void](Invoke-OpenExercise) }
  $html = Get-Content -LiteralPath (Join-Path $StateDir 'exercise.out') -Raw -Encoding Default
  $fields = @{}
  foreach ($m in [regex]::Matches($html, '(?is)<input\b[^>]*>')) {
    $tag = $m.Value
    $typ = 'text'
    if ($tag -match '(?i)\btype=["'']?(\w+)') { $typ = $Matches[1].ToLower() }
    if ($typ -in @('file','image','button','submit','reset')) { continue }
    if ($tag -notmatch '(?i)\bname=["'']?([^"''\s>]+)') { continue }
    $name = $Matches[1]
    if ($name -match '(?i)password|passwd') { continue }
    $val = ''
    if ($tag -match '(?i)\bvalue=["'']([^"'']*)') { $val = $Matches[1] }
    if (-not $fields.ContainsKey($name)) { $fields[$name] = $val }
  }
  $fields['cmd'] = 'reply'
  if (-not $fields['session']) { $fields['session'] = $sid }
  if (-not $fields['module']) { $fields['module'] = $CredsObj.ExerciseModule }
  if (-not $fields['exo']) { $fields['exo'] = $CredsObj.ExerciseExo }
  if (-not $fields['sheet']) { $fields['sheet'] = $CredsObj.ExerciseSheet }
  $filled = $false
  foreach ($key in @('reply','wims_answer','oefanswer','rep','answer')) {
    if ($fields.ContainsKey($key) -and -not $fields[$key]) { $fields[$key] = $Answer; $filled = $true }
  }
  if (-not $filled -and -not ($fields.Keys | Where-Object { $_ -match '(?i)^(reply|wims_answer|oefanswer|rep|answer)$' })) {
    $fields['reply'] = $Answer
  }
  Invoke-Http POST (Encode-Qs $fields)
  $r = Follow-Redirect (Parse-Result)
  $sid2 = if ($r.session) { $r.session } else { $sid }
  Write-Output "REPLY status=$($r.status) session=$sid2 bytes=$($r.len)"
  Save-Kv 'session_stu' $sid2
  return ($r.status -eq '200' -or $r.status -eq '302')
}

function Invoke-Scores {
  $sid = Load-Kv 'session_sup'
  if (-not $sid) {
    [void](Invoke-ClassLogin 'authsupervisor' $CredsObj.SupervisorLogin $CredsObj.SupervisorPassword 'SUPERVISOR' 'session_sup')
    $sid = Load-Kv 'session_sup'
  }
  Invoke-Http GET (Encode-Qs @{ module='adm/class/userscore'; cmd='new'; lang='fr'; session=$sid; class=$CredsObj.Class })
  $r = Follow-Redirect (Parse-Result)
  $notes = ($r.out -match '(?i)notes|feuille|sheet|score|utilisateur|userscore')
  Write-Output "NOTES status=$($r.status) bytes=$($r.len) markers=$notes session=$(if($r.session){$r.session}else{$sid})"
  return ($r.status -eq '200' -or $r.status -eq '302')
}

$ok = $true
switch ($Command) {
  'home' { $ok = Invoke-Home }
  'session' { $ok = Invoke-Session }
  'supervisor-login' { $ok = Invoke-ClassLogin 'authsupervisor' $CredsObj.SupervisorLogin $CredsObj.SupervisorPassword 'SUPERVISOR' 'session_sup' }
  'student-login' { $ok = Invoke-ClassLogin 'authparticipant' $CredsObj.ParticipantLogin $CredsObj.ParticipantPassword 'STUDENT' 'session_stu' }
  'open-exercise' { $ok = Invoke-OpenExercise }
  'reply' { $ok = Invoke-Reply }
  'scores' { $ok = Invoke-Scores }
  'scenario' {
    $ok = $true
    if (-not (Invoke-Home)) { $ok = $false }
    [void](Invoke-Session)
    if (-not (Load-Kv 'session_anon')) { $ok = $false }
    if (-not (Invoke-ClassLogin 'authsupervisor' $CredsObj.SupervisorLogin $CredsObj.SupervisorPassword 'SUPERVISOR' 'session_sup')) { $ok = $false }
    if (-not (Invoke-ClassLogin 'authparticipant' $CredsObj.ParticipantLogin $CredsObj.ParticipantPassword 'STUDENT' 'session_stu')) { $ok = $false }
    if (-not (Invoke-OpenExercise)) { $ok = $false }
    if (-not (Invoke-Reply)) { $ok = $false }
    if (-not (Invoke-Scores)) { $ok = $false }
    if ($ok) { Write-Output 'SCENARIO PASS' } else { Write-Output 'SCENARIO FAIL' }
  }
  default { Show-Usage; exit 2 }
}
if ($ok) { exit 0 } else { exit 1 }
