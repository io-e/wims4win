# GROK_V1 - Sandbox suite for ch..root.exe (wims4win #8), corrected CLI contract.
# Companion of 03_Test_Sandbox_CLAUDE_V1.ps1 (not modified). No cmd.exe (D10).
# Usage (PowerShell 7, not elevated):
#   pwsh -File 03_Test_Sandbox_GROK_V1.ps1 [-RuntimePath <runtime root>]
# Contract (src/Platform/sandbox/chroot_win.c, main):
#   ch..root.exe <prog> [args...]        run prog sandboxed; rc = prog's exit code
#   ch..root.exe --cmd <prog> [args...]  same (older form)
#   ch..root.exe & sh|perl <code>        refused, rc 127 (also after --cmd)
#   ch..root.exe --selftest | --grant-rx <dir>... | --purge-profiles | cleantmpdir
#   sandbox setup failure: rc 2 (or 127 if the program cannot be started)
# A denied operation is reported by the *child*: fake_cas returns 3 when its
# fopen/RegCreateKeyEx fails; PING.EXE returns 1. ch..root passes rc through.
# NOTE: never name a PowerShell parameter $Args (automatic variable): the
# array is then empty and ch..root.exe runs with no argument (rc 0, no output).
param([string]$RuntimePath = 'D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-r19040-grok')
$ErrorActionPreference = 'Continue'
$ch   = Join-Path $RuntimePath 'public_html\bin\ch..root.exe'
$fake = Join-Path $RuntimePath 'other\tests\fake_cas.exe'
$sb   = Join-Path $RuntimePath 'tmp\sessions'
$res  = [System.Collections.Generic.List[object]]::new()
function T($id, $ok, $detail, $prio = 'P0') {
  $s = if ($ok) { 'PASS' } else { 'FAIL' }
  Write-Host ("{0} {1} [{2}] {3}" -f $s, $id, $prio, $detail)
  $res.Add([pscustomobject]@{id = $id; status = $s; prio = $prio; detail = $detail})
}
foreach ($f in $ch, $fake) { if (-not (Test-Path $f)) { Write-Host "BAIL OUT - missing $f"; exit 2 } }
New-Item -ItemType Directory -Force $sb | Out-Null
$tmp = Join-Path $sb ('sbgrok' + [guid]::NewGuid().ToString('n').Substring(0, 8))
New-Item -ItemType Directory $tmp | Out-Null
$env:TMPDIR = $tmp
# run ch..root.exe; returns rc, stdout lines, stderr text (no cmd.exe)
function SB([string[]]$cmdArgs, [string]$stdin = $null) {
  $ef = [IO.Path]::GetTempFileName()
  if ($null -ne $stdin) { $o = @($stdin | & $ch @cmdArgs 2>$ef) } else { $o = @(& $ch @cmdArgs 2>$ef) }
  $rc = $LASTEXITCODE; $e = Get-Content $ef -Raw; Remove-Item $ef
  [pscustomobject]@{ rc = $rc; out = $o; err = "$e".Trim() }
}
# MSYS-05 watcher: every cmd.exe created during the run, with its parent
$t0 = Get-Date
$acKey = 'HKCU:\Software\Classes\Local Settings\Software\Microsoft\Windows\CurrentVersion\AppContainer\Mappings'
$ac0 = @(Get-ChildItem $acKey -EA SilentlyContinue).Count

$r = SB @('--selftest');                            T 'SBX-00-selftest' ($r.rc -eq 0) "rc=$($r.rc)"
$r = SB @($fake, 'write', "$tmp\in.txt");           T 'SBX-01-write-inside' ($r.rc -eq 0 -and (Test-Path "$tmp\in.txt")) "rc=$($r.rc)"
$r = SB @($fake, 'plain') 'hello world';            T 'SBX-02-stdio' ("$($r.out)" -eq 'ECHO:hello world') "out='$($r.out)' rc=$($r.rc)"
$r = SB @('--cmd', $fake, 'plain') 'hello world';   T 'SBX-02b-stdio-cmd-form' ("$($r.out)" -eq 'ECHO:hello world') "out='$($r.out)'"

$secret = Join-Path $RuntimePath 'log\sbtest-secret.txt'; 'SECRET_DATA_42' | Set-Content $secret
$r = SB @($fake, 'read', $secret);                  T 'SBX-S1a-read-log-denied' ($r.rc -eq 3) "rc=$($r.rc) (3 = fopen denied in child)"
Remove-Item $secret -EA SilentlyContinue
$ctl = Join-Path $tmp 'ctl.txt'; 'x' | Set-Content $ctl
$r = SB @($fake, 'read', $ctl);                     T 'SBX-S1-control-read-session' ($r.rc -eq 0) "rc=$($r.rc) (control: own session readable)"
$r = SB @($fake, 'read', (Join-Path $env:USERPROFILE 'Documents\desktop.ini'))
T 'SBX-S1b-read-profile-denied' ($r.rc -eq 3) "rc=$($r.rc) (Documents\desktop.ini: readable by the user, not by the container)"
$r = SB @($fake, 'read', (Join-Path $env:USERPROFILE 'NTUSER.DAT')); T 'SBX-S1c-read-ntuser-denied' ($r.rc -eq 3) "rc=$($r.rc)"

$i = 0
foreach ($evil in (Join-Path $RuntimePath 'log\sbtest-evil.txt'), (Join-Path $env:TEMP 'wims-sbtest-evil.txt'), (Join-Path $sb 'sibling-evil.txt')) {
  $i++; Remove-Item $evil -EA SilentlyContinue
  $r = SB @($fake, 'write', $evil); T ("SBX-S2{0}-write-outside-denied" -f [char](96 + $i)) ($r.rc -eq 3 -and -not (Test-Path $evil)) "rc=$($r.rc) $evil"
}
$r = SB @((Join-Path $env:SystemRoot 'System32\PING.EXE'), '-n', '1', '-w', '500', '1.1.1.1')
T 'SBX-S3-no-network' ($r.rc -ne 0) "rc=$($r.rc) (PING.EXE exit code)"

$before = @(Get-Process fake_cas -EA SilentlyContinue).Count
$r = SB @($fake, 'bomb')
$n = if ("$($r.out)" -match 'children=(\d+)') { [int]$matches[1] } else { -1 }
T 'SBX-S4a-fork-bomb' ($n -ge 0 -and $n -le 14) "children=$n (Job ActiveProcessLimit=15 incl. parent), rc=$($r.rc) (0 by design)"
Start-Sleep 1; $after = @(Get-Process fake_cas -EA SilentlyContinue).Count
T 'SBX-S4b-cleanup' ($after -le $before) "fake_cas processes $before -> $after"

$a1 = 'a" & PING.EXE -n 1 127.0.0.1 & "b'; $a2 = 'x\" y'; $a3 = 'c:\dir with space\'
$r = SB @($fake, 'args', $a1, $a2, $a3)
T 'SBX-S5-quote-arg' ($r.out.Count -eq 3 -and $r.out[0] -eq "[$a1]" -and $r.out[1] -eq "[$a2]" -and $r.out[2] -eq "[$a3]") "lines=$($r.out.Count): $($r.out -join ' | ')"

Remove-Item 'HKCU:\Software\wims-sandbox-test' -Recurse -EA SilentlyContinue
$r = SB @($fake, 'reg'); T 'SBX-S9-no-registry' ($r.rc -eq 3 -and -not (Test-Path 'HKCU:\Software\wims-sandbox-test')) "rc=$($r.rc)"

$r = SB @('&', 'sh', 'echo pwned > x');          T 'SBX-10-shell-inject' ($r.rc -eq 127 -and $r.err -match 'refused') "rc=$($r.rc) err='$($r.err)'"
$r = SB @('--cmd', '&', 'sh', 'echo pwned > x'); T 'SBX-10b-shell-inject-cmd-form' ($r.rc -eq 127) "rc=$($r.rc) err='$($r.err)'"
T 'SBX-10c-no-file' (-not (Test-Path (Join-Path $tmp 'x'))) 'x not created'
$r = SB @('cleantmpdir'); T 'SBX-11-cleantmpdir' ($r.rc -eq 0) "rc=$($r.rc)" 'P1'

$ac1 = @(Get-ChildItem $acKey -EA SilentlyContinue).Count
T 'SBX-12-no-profile-left' ($ac1 -le $ac0) "AppContainer profiles $ac0 -> $ac1"
$left = (Get-Acl $tmp).GetAccessRules($true, $false, [Security.Principal.SecurityIdentifier]) | ? { $_.IdentityReference.Value -match '^S-1-15-2-\d+-' }
T 'SBX-12b-no-container-ACE' (-not $left) 'no S-1-15-2-<container> ACE on the session dir'

# SBX-15: compare SIDs, not names (French Windows: "TOUS LES PACKAGES D'APPLICATION")
$g = Join-Path $env:TEMP ('wims-grant-' + [guid]::NewGuid().ToString('n'))
New-Item -ItemType Directory $g | Out-Null
$r = SB @('--grant-rx', $g)
$ace = (Get-Acl $g).GetAccessRules($true, $false, [Security.Principal.SecurityIdentifier]) | ? { $_.IdentityReference.Value -eq 'S-1-15-2-1' -and $_.AccessControlType -eq 'Allow' }
T 'SBX-15-grant-rx' ($r.rc -eq 0 -and $ace) ("rc=$($r.rc) ACE S-1-15-2-1: " + (($ace | % { "$($_.FileSystemRights)/$($_.InheritanceFlags)" }) -join '; '))
Remove-Item -Recurse -Force $g

$gp = Join-Path (Split-Path $RuntimePath) 'tests-win\cas\pari\gp.exe'
if (Test-Path $gp) {
  $evil = Join-Path $RuntimePath 'log\gp-evil.txt'; Remove-Item $evil -EA SilentlyContinue
  $r = SB @($gp, '-f', '-q') ("print(2+2)`nwrite(`"$($evil.Replace('\','/'))`",1)`nquit")
  T 'SBX-17-pari-write-denied' ("$($r.out)" -match '4' -and -not (Test-Path $evil)) "out='$($r.out)' file absent=$(-not (Test-Path $evil))"
} else { Write-Host 'SKIP SBX-17 (gp.exe not found)' }

$bad = (Get-Acl (Join-Path $RuntimePath 'log')).GetAccessRules($true, $true, [Security.Principal.SecurityIdentifier]) | ? { $_.IdentityReference.Value -like 'S-1-15-2-*' -and $_.AccessControlType -eq 'Allow' }
T 'SEC-04-log-no-appcontainer' (-not $bad) 'no S-1-15-2-* Allow ACE on log\'

$cmds = @(Get-CimInstance Win32_Process -Filter "Name='cmd.exe'" | ? { $_.CreationDate -ge $t0 })
T 'MSYS-05-no-cmdexe' ($cmds.Count -eq 0) "cmd.exe started during the suite: $($cmds.Count)"

Remove-Item -Recurse -Force $tmp -EA SilentlyContinue
$p = @($res | ? status -eq 'PASS').Count; $f = @($res | ? status -eq 'FAIL').Count
Write-Host "== Sandbox GROK_V1: $p passed, $f failed =="
$out = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'results'
New-Item -ItemType Directory -Force $out | Out-Null
$res | ConvertTo-Json | Set-Content (Join-Path $out ("sandbox_GROK_V1_{0}.json" -f (Get-Date -f yyyyMMdd_HHmmss)))
exit $(if ($f) { 1 } else { 0 })
