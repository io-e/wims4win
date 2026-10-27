# Evasion tests for ch..root.exe (run on real Windows after --selftest passes).
# Expect: network/registry/file-outside-session denied; session dir writable.
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
$ch=Join-Path $here '..\..\build\tests-win\ch..root.exe'
if(-not (Test-Path $ch)){ $ch=Join-Path (Get-Location) 'ch..root.exe' }
$tmp=Join-Path $env:TEMP ("wims-sb-"+[guid]::NewGuid().ToString('n'))
New-Item -ItemType Directory $tmp | Out-Null
$env:TMPDIR=$tmp
# no cmd.exe: fake_cas.exe "write <file>" and ping.exe are run directly
# ($args is a PowerShell automatic variable: never use it as a parameter name)
$fake=Join-Path $here '..\..\build\tests-win\fake_cas.exe'
if(-not (Test-Path $fake)){ $fake=Join-Path (Get-Location) 'fake_cas.exe' }
function Run([string[]]$cmdArgs){ & $ch --cmd @cmdArgs 2>$null | Out-Null; return $LASTEXITCODE }
$pass=0;$fail=0
function T($n,$ok){ if($ok){Write-Host "PASS $n";$script:pass++}else{Write-Host "FAIL $n";$script:fail++} }
# write inside session
T "write session file" ((Run @($fake,'write',"$tmp\in.txt")) -eq 0 -and (Test-Path "$tmp\in.txt"))
# write outside session (Windows\Temp sibling) should fail or be invisible to us as denial
$out=Join-Path $env:SystemRoot 'Temp\wims-evasion-should-fail.txt'
Remove-Item $out -EA SilentlyContinue
$rc=Run @($fake,'write',$out)
T "cannot write outside session (file absent)" (-not (Test-Path $out))
# network: ping should fail inside AppContainer without network capability
$rc=Run @((Join-Path $env:SystemRoot 'System32\PING.EXE'),'-n','1','-w','500','1.1.1.1')
T "no network (ping fails)" ($rc -ne 0)
Write-Host "== $pass passed, $fail failed =="
Remove-Item -Recurse -Force $tmp -EA SilentlyContinue
exit $(if($fail){1}else{0})
