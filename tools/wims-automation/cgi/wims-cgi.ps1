# Simulate WIMS CGI requests directly (REQUEST_METHOD / QUERY_STRING + wims.cgi).
# Credentials from --creds or WIMS_CREDS_FILE / env; never hardcoded, never printed.
# Prefer wims.cgi.exe on Windows (do not glob wims.cgi* first; see docs/11 BKP-06).
[CmdletBinding()]
param(
  [string]$Runtime,
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
Usage: wims-cgi.ps1 -Runtime DIR [-Creds FILE] [-StateDir DIR] [-Answer TXT] COMMAND

Commands:
  home, session, supervisor-login, student-login,
  open-exercise, reply, scores, scenario
"@
}
if ($Help -or -not $Command) { Show-Usage; if ($Help) { exit 0 } else { exit 2 } }
if (-not $Runtime) { $Runtime = $env:WIMS_RUNTIME }
if (-not $Runtime) { Show-Usage; exit 2 }
$Runtime = [IO.Path]::GetFullPath($Runtime)
$Cgi = Get-WimsCgiPath $Runtime
$CredsObj = Get-WimsCreds -CredsFile $Creds
if (-not $StateDir) { $StateDir = $env:WIMS_STATE_DIR }
if (-not $StateDir) { $StateDir = Join-Path $env:TEMP ("wims-auto-" + [guid]::NewGuid().ToString('N')) }
New-Item -ItemType Directory -Force -Path $StateDir | Out-Null

function Save-Kv([string]$k,[string]$v){ Set-Content -LiteralPath (Join-Path $StateDir $k) -Value $v -Encoding ASCII -NoNewline }
function Load-Kv([string]$k){ $p=Join-Path $StateDir $k; if(Test-Path -LiteralPath $p){ return (Get-Content -LiteralPath $p -Raw) } else { return '' } }

function Invoke-WimsCgi {
  param([hashtable]$Params, [string]$Method = 'GET')
  $pub = Join-Path $Runtime 'public_html'
  $pairs = @()
  foreach ($k in $Params.Keys) {
    $pairs += ([uri]::EscapeDataString([string]$k) + '=' + [uri]::EscapeDataString([string]$Params[$k]))
  }
  $body = [string]::Join('&', $pairs)
  $saved = @{}
  $names = @('REQUEST_METHOD','QUERY_STRING','CONTENT_TYPE','CONTENT_LENGTH','SERVER_SOFTWARE','SERVER_NAME','SERVER_PORT','SERVER_PROTOCOL','GATEWAY_INTERFACE','SCRIPT_NAME','SCRIPT_FILENAME','REMOTE_ADDR','REMOTE_PORT','SERVER_ADDR','HTTP_HOST','HTTP_USER_AGENT','WIMS_HOME')
  foreach ($n in $names) { $saved[$n] = [Environment]::GetEnvironmentVariable($n, 'Process') }
  try {
    [Environment]::SetEnvironmentVariable('SERVER_SOFTWARE','wims-automation','Process')
    [Environment]::SetEnvironmentVariable('SERVER_NAME','localhost','Process')
    [Environment]::SetEnvironmentVariable('SERVER_PORT','80','Process')
    [Environment]::SetEnvironmentVariable('SERVER_PROTOCOL','HTTP/1.0','Process')
    [Environment]::SetEnvironmentVariable('GATEWAY_INTERFACE','CGI/1.1','Process')
    [Environment]::SetEnvironmentVariable('SCRIPT_NAME','/wims/wims.cgi','Process')
    [Environment]::SetEnvironmentVariable('SCRIPT_FILENAME',$Cgi,'Process')
    [Environment]::SetEnvironmentVariable('REMOTE_ADDR','127.0.0.1','Process')
    [Environment]::SetEnvironmentVariable('REMOTE_PORT','54321','Process')
    [Environment]::SetEnvironmentVariable('SERVER_ADDR','127.0.0.1','Process')
    [Environment]::SetEnvironmentVariable('HTTP_HOST','localhost','Process')
    [Environment]::SetEnvironmentVariable('HTTP_USER_AGENT','wims-automation/cgi','Process')
    [Environment]::SetEnvironmentVariable('WIMS_HOME',$null,'Process')
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $Cgi
    $psi.WorkingDirectory = $pub
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow = $true
    $psi.StandardOutputEncoding = [Text.Encoding]::GetEncoding(1252)
    $psi.StandardErrorEncoding = [Text.Encoding]::GetEncoding(1252)
    $bytes = $null
    if ($Method -eq 'POST') {
      [Environment]::SetEnvironmentVariable('REQUEST_METHOD','POST','Process')
      [Environment]::SetEnvironmentVariable('QUERY_STRING','','Process')
      [Environment]::SetEnvironmentVariable('CONTENT_TYPE','application/x-www-form-urlencoded','Process')
      $bytes = [Text.Encoding]::ASCII.GetBytes($body)
      [Environment]::SetEnvironmentVariable('CONTENT_LENGTH', "$($bytes.Length)", 'Process')
    } else {
      [Environment]::SetEnvironmentVariable('REQUEST_METHOD','GET','Process')
      [Environment]::SetEnvironmentVariable('QUERY_STRING',$body,'Process')
      [Environment]::SetEnvironmentVariable('CONTENT_TYPE',$null,'Process')
      [Environment]::SetEnvironmentVariable('CONTENT_LENGTH',$null,'Process')
    }
    $p = [Diagnostics.Process]::Start($psi)
    if ($bytes) { $p.StandardInput.BaseStream.Write($bytes,0,$bytes.Length) }
    $p.StandardInput.Close()
    $outTask = $p.StandardOutput.ReadToEndAsync()
    $errTask = $p.StandardError.ReadToEndAsync()
    if (-not $p.WaitForExit(30000)) { try { $p.Kill() } catch {}; $rc = 124 } else { $rc = $p.ExitCode }
    $out = $outTask.Result
    $err = $errTask.Result
  } finally {
    foreach ($n in $names) { [Environment]::SetEnvironmentVariable($n, $saved[$n], 'Process') }
  }
  $st = '200'; if ($out -match '(?m)^Status:\s*(\S+)') { $st = $Matches[1] }
  elseif ($out -match '(?m)^Location:') { $st = '302' }
  $sess = ''; if ($out -match 'session=([A-Za-z0-9_.]+)') { $sess = $Matches[1].Split('.')[0] }
  $loc = ''; if ($out -match '(?m)^Location:\s*(\S+)') { $loc = $Matches[1].Trim() }
  Set-Content -LiteralPath (Join-Path $StateDir 'last.out') -Value $out -Encoding Default
  return @{ rc=$rc; out=$out; err=$err; status=$st; session=$sess; location=$loc; len=$out.Length }
}

function Follow-Redirect($r) {
  if (-not $r.location) { return $r }
  $q = $r.location
  if ($q.Contains('?')) { $q = $q.Split('?',2)[1] } else { return $r }
  $ht = @{}
  foreach ($part in ($q -split '&')) {
    $pp = $part.TrimStart('+')
    if ($pp -match '^([^=]+)=(.*)$') {
      $k = $Matches[1].TrimStart('+')
      if ($k -in @('auth','auth_password','pw1','pw2','old','devpasswd','passwd','password')) { return $r }
      $ht[$k] = [uri]::UnescapeDataString($Matches[2])
    }
  }
  return Invoke-WimsCgi -Params $ht -Method GET
}

function Read-SessUser([string]$sess) {
  if (-not $sess) { return '' }
  $sid = $sess.Split('.')[0]
  foreach ($name in @('var.stat','var','var.class.prep')) {
    $vf = Join-Path $Runtime ("sessions\$sid\$name")
    if (-not (Test-Path -LiteralPath $vf)) { continue }
    foreach ($line in [IO.File]::ReadAllLines($vf, [Text.Encoding]::GetEncoding(1252))) {
      if ($line -match '(?i)password|passwd') { continue }
      if ($line -match '^(?:w_)?wims_user=(.*)$') { return $Matches[1] }
    }
  }
  return ''
}

function Has-Prep([string]$sess) {
  if (-not $sess) { return $false }
  return (Test-Path -LiteralPath (Join-Path $Runtime ("sessions\" + $sess.Split('.')[0] + "\var.class.prep")))
}

function Invoke-Home {
  $r = Invoke-WimsCgi @{ module='home'; cmd='intro'; lang='en' }
  if (-not $r.session) { $r = Invoke-WimsCgi @{ module='home'; cmd='new'; lang='en' } }
  $r2 = Follow-Redirect $r
  $sid = if ($r2.session) { $r2.session } else { $r.session }
  Write-Output "HOME status=$($r.status) session=$(if($sid){$sid}else{'none'}) bytes=$($r.len)"
  if ($sid) { Save-Kv 'session_anon' $sid }
  return [bool]$sid -or ($r.status -eq '200')
}

function Invoke-Session {
  $r = Invoke-WimsCgi @{ module='home'; cmd='new'; lang='en' }
  $r2 = Follow-Redirect $r
  $sid = if ($r2.session) { $r2.session } else { $r.session }
  Write-Output "SESSION id=$(if($sid){$sid}else{'FAIL'}) status=$($r.status) bytes=$($r.len)"
  if (-not $sid) { return $false }
  Save-Kv 'session_anon' $sid
  Write-Output $sid
  return $true
}

function Invoke-ClassLogin([string]$typ,[string]$user,[string]$pass,[string]$label,[string]$store) {
  $r0 = Invoke-WimsCgi @{ module='adm/class/classes'; cmd='new'; lang='fr'; class=$CredsObj.Class; type=$typ }
  $r1 = Invoke-WimsCgi -Method POST @{ module='adm/class/classes'; cmd='reply'; lang='fr'; class=$CredsObj.Class; type=$typ; session=$r0.session; auth_user=$user; auth_password=$pass; agreecgu='yes' }
  $r2 = Follow-Redirect $r1
  $u = Read-SessUser $r2.session
  if (-not $u) { $u = Read-SessUser $r1.session }
  if (-not $u) { $u = Read-SessUser $r0.session }
  $prep = (Has-Prep $r0.session) -or (Has-Prep $r2.session)
  $sid = if ($r2.session) { $r2.session } else { $r1.session }
  Write-Output "$label user=$(if($u){$u}else{'empty'}) status=$($r1.status) session=$(if($sid){$sid}else{'none'}) prep=$prep bytes=$($r1.len)"
  if (-not $u) { return $false }
  Save-Kv $store $sid
  Save-Kv ($store + '_user') $u
  return $true
}

function Invoke-OpenExercise {
  $sid = Load-Kv 'session_stu'
  if (-not $sid) {
    if (-not (Invoke-ClassLogin 'authparticipant' $CredsObj.ParticipantLogin $CredsObj.ParticipantPassword 'STUDENT' 'session_stu')) { return $false }
    $sid = Load-Kv 'session_stu'
  }
  $r = Invoke-WimsCgi @{ module=$CredsObj.ExerciseModule; cmd='new'; lang='fr'; session=$sid; exo=$CredsObj.ExerciseExo; sheet=$CredsObj.ExerciseSheet; qnum='1' }
  $r2 = Follow-Redirect $r
  $sid2 = if ($r2.session) { $r2.session } else { $sid }
  Write-Output "EXERCISE status=$($r.status) session=$sid2 bytes=$($r.len) module=$($CredsObj.ExerciseModule) exo=$($CredsObj.ExerciseExo)"
  Save-Kv 'session_stu' $sid2
  Set-Content -LiteralPath (Join-Path $StateDir 'exercise.out') -Value $(if($r2.out){$r2.out}else{$r.out}) -Encoding Default
  return ($r.len -gt 200)
}

function Invoke-Reply {
  $sid = Load-Kv 'session_stu'
  if (-not $sid) { Write-Output 'reply: no student session'; return $false }
  $exf = Join-Path $StateDir 'exercise.out'
  if (-not (Test-Path -LiteralPath $exf)) { [void](Invoke-OpenExercise) }
  $html = Get-Content -LiteralPath (Join-Path $StateDir 'exercise.out') -Raw -Encoding Default
  $fields = @{}
  $rx = [regex]'(?is)<input\b[^>]*>'
  foreach ($m in $rx.Matches($html)) {
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
  $r = Invoke-WimsCgi -Method POST $fields
  $r2 = Follow-Redirect $r
  $sid2 = if ($r2.session) { $r2.session } else { $sid }
  Write-Output "REPLY status=$($r.status) session=$sid2 bytes=$($r.len)"
  Save-Kv 'session_stu' $sid2
  return ($r.status -like '200*' -or $r.status -like '302*')
}

function Invoke-Scores {
  $stu = Load-Kv 'session_stu_user'
  $scoreDir = Join-Path $Runtime "log\classes\$($CredsObj.Class)\score"
  $found = 'none'
  if (Test-Path -LiteralPath $scoreDir) {
    $found = ((Get-ChildItem -LiteralPath $scoreDir -File).Name -join ' ')
  }
  Write-Output "SCORES files=$found student_user=$(if($stu){$stu}else{'unknown'})"
  $sid = Load-Kv 'session_sup'
  if (-not $sid) {
    [void](Invoke-ClassLogin 'authsupervisor' $CredsObj.SupervisorLogin $CredsObj.SupervisorPassword 'SUPERVISOR' 'session_sup')
    $sid = Load-Kv 'session_sup'
  }
  $r = Invoke-WimsCgi @{ module='adm/class/userscore'; cmd='new'; lang='fr'; session=$sid; class=$CredsObj.Class }
  $r2 = Follow-Redirect $r
  $notes = ($r.out -match '(?i)notes|feuille|sheet|score|utilisateur|userscore')
  Write-Output "NOTES status=$($r.status) bytes=$($r.len) markers=$notes session=$(if($r2.session){$r2.session}else{$sid})"
  return ($r.status -like '200*' -or $r.status -like '302*')
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
    if (-not (Invoke-Session | Select-Object -Last 1)) { } # session prints id; check file
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
