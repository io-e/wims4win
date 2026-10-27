# Sandbox evasion tests S1-S5, S9 (+ session write, stdin/stdout, cleanup) for
# ch..root.exe on real Windows. No cmd.exe: fake_cas.exe modes and PING.EXE are
# run directly. Usage (PowerShell, not elevated):
#   .\test_sandbox_evasion.ps1 -Runtime <wims runtime root>
# fake_cas.exe must sit in <runtime>\other\tests (granted RX by --grant-rx).
# ($args is a PowerShell automatic variable: never use it as a parameter name)
param([string]$Runtime)
$ErrorActionPreference='Continue'
$ch=Join-Path $Runtime 'public_html\bin\ch..root.exe'
$fake=Join-Path $Runtime 'other\tests\fake_cas.exe'
$sb=Join-Path $Runtime 'tmp\sessions'
New-Item -ItemType Directory -Force $sb | Out-Null
$tmp=Join-Path $sb ("sbtest"+[guid]::NewGuid().ToString('n').Substring(0,8))
New-Item -ItemType Directory $tmp | Out-Null
$env:TMPDIR=$tmp
$pass=0;$fail=0
function T($n,$ok){ if($ok){Write-Host "PASS $n";$script:pass++}else{Write-Host "FAIL $n";$script:fail++} }
function Run([string[]]$cmdArgs){ & $ch @cmdArgs 2>$null | Out-Null; return $LASTEXITCODE }
$acKey='HKCU:\Software\Classes\Local Settings\Software\Microsoft\Windows\CurrentVersion\AppContainer\Mappings'
$ac0=@(Get-ChildItem $acKey -EA SilentlyContinue).Count

& $ch --selftest; T "selftest (no cmd.exe)" ($LASTEXITCODE -eq 0)
T "write inside session dir" ((Run @($fake,'write',"$tmp\in.txt")) -eq 0 -and (Test-Path "$tmp\in.txt"))
$o = 'print("hello")' | & $ch $fake plain 2>$null
T "stdin/stdout through the sandbox" ("$o" -match 'hello')

$secret=Join-Path $Runtime 'log\sbtest-secret.txt'; 'secret' | Set-Content $secret
T "S1 cannot read runtime log/ (wims.conf, passwords)" ((Run @($fake,'read',$secret)) -ne 0)
T "S1 cannot read user profile" ((Run @($fake,'read',(Join-Path $env:USERPROFILE 'NTUSER.DAT'))) -ne 0)
Remove-Item $secret -EA SilentlyContinue
$evil=Join-Path $Runtime 'log\sbtest-evil.txt'; Remove-Item $evil -EA SilentlyContinue
$null=Run @($fake,'write',$evil); T "S2 cannot write runtime log/" (-not (Test-Path $evil))
$evil2=Join-Path $env:TEMP 'wims-sbtest-evil.txt'; Remove-Item $evil2 -EA SilentlyContinue
$null=Run @($fake,'write',$evil2); T "S2 cannot write user TEMP" (-not (Test-Path $evil2))
$evil3=Join-Path $sb 'sibling-evil.txt'; Remove-Item $evil3 -EA SilentlyContinue
$null=Run @($fake,'write',$evil3); T "S2 cannot write next to the session (other sessions)" (-not (Test-Path $evil3))

$rc=Run @((Join-Path $env:SystemRoot 'System32\PING.EXE'),'-n','1','-w','500','1.1.1.1')
T "S3 no network (ping fails)" ($rc -ne 0)

$before=@(Get-Process fake_cas -EA SilentlyContinue).Count
$o = & $ch $fake bomb 2>$null
$n = if("$o" -match 'children=(\d+)'){[int]$matches[1]}else{-1}
Start-Sleep 1
$after=@(Get-Process fake_cas -EA SilentlyContinue).Count
T "S4 fork bomb capped (children=$n < 15)" ($n -ge 0 -and $n -lt 15)
T "S4 no process left after the run ($before -> $after)" ($after -le $before)

$a1='a" & PING.EXE -n 1 127.0.0.1 & "b'; $a2='x\" y'; $a3='c:\dir with space\'
$o = @(& $ch $fake args $a1 $a2 $a3 2>$null)
T "S5 arguments not split nor interpreted" ($o.Count -eq 3 -and $o[0] -eq "[$a1]" -and $o[1] -eq "[$a2]" -and $o[2] -eq "[$a3]")

Remove-Item 'HKCU:\Software\wims-sandbox-test' -Recurse -EA SilentlyContinue
$rc=Run @($fake,'reg')
T "S9 HKCU write refused" ($rc -ne 0 -and -not (Test-Path 'HKCU:\Software\wims-sandbox-test'))

$rc=Run @('&','sh','echo pwned > x')
T "untrusted '& sh' refused (127)" ($rc -eq 127)
T "cleantmpdir accepted" ((Run @('cleantmpdir')) -eq 0)

$ac1=@(Get-ChildItem $acKey -EA SilentlyContinue).Count
T "no AppContainer profile left ($ac0 -> $ac1)" ($ac1 -le $ac0)
$acl=(Get-Acl $tmp).Access | ?{ $_.IdentityReference -match 'S-1-15-2-\d+-' }
T "no container ACE left on the session dir" (-not $acl)

Write-Host "== $pass passed, $fail failed =="
Remove-Item -Recurse -Force $tmp -EA SilentlyContinue
exit $(if($fail){1}else{0})
