# GROK_V1 - D21 "lowil" policy of ch..root.exe (Cygwin CAS such as GAP).
# Restricted token at LOW integrity + Job (15 processes, memory_mb, kill-on-close),
# no AppContainer, cwd/TEMP = session dir. Selection: "lowil <name>" in
# ch..root.conf next to ch..root.exe. No cmd.exe (D10). PowerShell 7, not elevated.
#   pwsh -File test-sandbox-lowil.ps1 [-RuntimePath <runtime root>] [-GapExe <gap.exe>]
param([string]$RuntimePath = 'D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-r19040-grok',
      [string]$GapExe = 'D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\tests-win\cas\gap\runtime\opt\gap-4.16.1\gap.exe',
      [string]$GapBin = 'D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\tests-win\cas\gap\runtime\bin')
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
$tmp = Join-Path $sb ('lowil' + [guid]::NewGuid().ToString('n').Substring(0, 8))
New-Item -ItemType Directory -Force $tmp | Out-Null
$env:TMPDIR = $tmp
function RUN($exe, [string[]]$cmdArgs, [string]$stdin = $null) {
  $ef = [IO.Path]::GetTempFileName()
  if ($null -ne $stdin) { $o = @($stdin | & $exe @cmdArgs 2>$ef) } else { $o = @(& $exe @cmdArgs 2>$ef) }
  $rc = $LASTEXITCODE; $e = Get-Content $ef -Raw; Remove-Item $ef
  [pscustomobject]@{ rc = $rc; out = $o; err = "$e".Trim() }
}
$t0 = Get-Date

# 1. selection is explicit: shipped conf lists gap.exe only
$conf = Join-Path (Split-Path $ch) 'ch..root.conf'
$lines = @(Get-Content $conf -EA SilentlyContinue | ? { $_ -match '^\s*lowil\s' })
T 'D21-01-conf-gap-only' ($lines.Count -eq 1 -and $lines[0] -match 'gap\.exe') ("lowil lines: " + ($lines -join '; '))
$p1 = (RUN $ch @('--policy', 'gap.exe')).out; $p2 = (RUN $ch @('--policy', $fake)).out
$p3 = (RUN $ch @('--policy', 'gp.exe')).out; $p4 = (RUN $ch @('--policy', 'maxima.exe')).out
T 'D21-02-policy-report' ("$p1" -eq 'lowil' -and "$p2" -eq 'appcontainer' -and "$p3" -eq 'appcontainer' -and "$p4" -eq 'appcontainer') "gap=$p1 fake_cas=$p2 gp=$p3 maxima=$p4"
# other CAS still in AppContainer: a write to log\ fails and no network
$evil = Join-Path $RuntimePath 'log\d21-ac-evil.txt'; Remove-Item $evil -EA SilentlyContinue
$r = RUN $ch @($fake, 'write', $evil)
$acs = @(Get-ChildItem 'HKCU:\Software\Classes\Local Settings\Software\Microsoft\Windows\CurrentVersion\AppContainer\Mappings' -EA SilentlyContinue).Count
$r2 = RUN $ch @((Join-Path $env:SystemRoot 'System32\PING.EXE'), '-n', '1', '-w', '500', '1.1.1.1')
T 'D21-03-others-appcontainer' ($r.rc -eq 3 -and $r2.rc -ne 0) "fake_cas write log rc=$($r.rc), PING rc=$($r2.rc)"

# 2. lowil path exercised with fake_cas: copy ch..root.exe + a test conf
$bin = Join-Path $env:TEMP ('wims-d21-' + [guid]::NewGuid().ToString('n').Substring(0, 8))
New-Item -ItemType Directory $bin | Out-Null
Copy-Item $ch $bin; $lch = Join-Path $bin 'ch..root.exe'
"lowil fake_cas.exe`nmemory_mb 256" | Set-Content (Join-Path $bin 'ch..root.conf')
T 'D21-04-test-policy' ("$((RUN $lch @('--policy', $fake)).out)" -eq 'lowil') 'test conf selects fake_cas'
$r = RUN $lch @($fake, 'write', "$tmp\in.txt")
T 'D21-05-write-session' ($r.rc -eq 0 -and (Test-Path "$tmp\in.txt")) "rc=$($r.rc)"
$r = RUN $lch @($fake, 'write', 'rel.txt')
T 'D21-06-cwd-session' ($r.rc -eq 0 -and (Test-Path "$tmp\rel.txt")) "rc=$($r.rc) (relative file lands in the session dir)"
$i = 0
foreach ($e in (Join-Path $env:USERPROFILE 'wims-d21-evil.txt'), (Join-Path $env:LOCALAPPDATA 'Temp\wims-d21-evil.txt'),
               (Join-Path $RuntimePath 'log\d21-evil.txt'), (Join-Path $sb 'd21-sibling-evil.txt')) {
  $i++; Remove-Item $e -EA SilentlyContinue
  $r = RUN $lch @($fake, 'write', $e)
  T ("D21-07{0}-no-write-outside" -f [char](96 + $i)) ($r.rc -eq 3 -and -not (Test-Path $e)) "rc=$($r.rc) $e"
}
Remove-Item 'HKCU:\Software\wims-sandbox-test' -Recurse -EA SilentlyContinue
$r = RUN $lch @($fake, 'reg'); T 'D21-08-no-hkcu-write' ($r.rc -eq 3 -and -not (Test-Path 'HKCU:\Software\wims-sandbox-test')) "rc=$($r.rc)"
$before = @(Get-Process fake_cas -EA SilentlyContinue).Count
$r = RUN $lch @($fake, 'bomb')
$n = if ("$($r.out)" -match 'children=(\d+)') { [int]$matches[1] } else { -1 }
T 'D21-09-process-limit' ($n -ge 0 -and $n -le 14) "children=$n (limit 15 incl. parent)"
Start-Sleep 1; $after = @(Get-Process fake_cas -EA SilentlyContinue).Count
T 'D21-10-kill-on-close' ($after -le $before) "fake_cas processes $before -> $after"
$r = RUN $lch @($fake, 'alloc', '64');  $ok64 = $r.rc -eq 0
$r2 = RUN $lch @($fake, 'alloc', '1024')
T 'D21-11-memory-limit' ($ok64 -and $r2.rc -ne 0) "alloc 64 MB rc=$($r.rc), alloc 1024 MB rc=$($r2.rc) (memory_mb 256)"
$r = RUN $lch @('&', 'sh', 'echo pwned > x'); T 'D21-12-shell-refused' ($r.rc -eq 127 -and $r.err -match 'refused') "rc=$($r.rc) err='$($r.err)'"
$lab = (Get-Acl $tmp).Sddl -match 'ML;[^)]*;;;LW'
T 'D21-13-label-removed' (-not $lab) 'session dir has no low label after the run'
$sec = Join-Path $RuntimePath 'log\d21-secret.txt'; 'S' | Set-Content $sec
$r = RUN $lch @($fake, 'read', $sec)
T 'D21-14-tradeoff-read-allowed' ($r.rc -eq 0) "rc=$($r.rc) (documented D21 trade-off: no read isolation)" 'INFO'
Remove-Item $sec -EA SilentlyContinue
Remove-Item -Recurse -Force $bin -EA SilentlyContinue

# 3. real GAP through the shipped conf (IFC-06 GAP part)
if (Test-Path $GapExe) {
  if (Test-Path $GapBin) { $env:PATH = "$GapBin;$env:PATH" }
  $r = RUN $ch @($GapExe, '-q', '-T', '-n') "Print(2+3, `"\n`");`nPrint(Factorial(10), `"\n`");`nQUIT;"
  T 'D21-15-gap-runs' ("$($r.out)" -match '\b5\b' -and "$($r.out)" -match '3628800') "rc=$($r.rc) out='$($r.out -join ' ')' err='$($r.err)'"
  $ge = Join-Path $env:USERPROFILE 'wims-d21-gap-evil.txt'; Remove-Item $ge -EA SilentlyContinue
  $gp = $ge.Replace('\', '/')
  $r = RUN $ch @($GapExe, '-q', '-T', '-n') "PrintTo(`"$gp`", 1);`nQUIT;"
  T 'D21-16-gap-no-profile-write' (-not (Test-Path $ge)) "file absent=$(-not (Test-Path $ge))"
} else { Write-Host "SKIP D21-15/16 (no $GapExe)" }

$cmds = @(Get-CimInstance Win32_Process -Filter "Name='cmd.exe'" | ? { $_.CreationDate -ge $t0 })
T 'D21-17-no-cmdexe' ($cmds.Count -eq 0) "cmd.exe started: $($cmds.Count)"
Remove-Item -Recurse -Force $tmp -EA SilentlyContinue
$p = @($res | ? status -eq 'PASS').Count; $f = @($res | ? status -eq 'FAIL').Count
Write-Host "== Sandbox lowil (D21): $p passed, $f failed =="
$out = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'results'
New-Item -ItemType Directory -Force $out | Out-Null
$res | ConvertTo-Json | Set-Content (Join-Path $out ("sandbox_lowil_GROK_V1_{0}.json" -f (Get-Date -f yyyyMMdd_HHmmss)))
exit $(if ($f) { 1 } else { 0 })
