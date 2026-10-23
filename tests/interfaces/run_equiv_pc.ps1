# Native Windows: gap / maxima / octave vs Linux behavioural refs. Run from tests-win.
$ErrorActionPreference='Continue'
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $here
$pass=0; $fail=0; $rows=New-Object System.Collections.Generic.List[object]
function Norm([string]$s){
  if($null -eq $s){ return '' }
  $s = [regex]::Replace($s, '\x1b\[[0-9;]*m', '')
  $s = $s.Replace("`r",'')
  $s = [regex]::Replace($s, '(Start|End) line ([a-z0-9]+)( [0-9]+){4}', '$1 line $2 RND')
  return $s
}
function Rec($cas,$tag,$ok,$detail){
  $rows.Add([pscustomobject]@{cas=$cas; scenario=$tag; ok=$ok; detail=$detail}) | Out-Null
  if($ok){ $script:pass++; Write-Host "PASS $cas/$tag :: $detail" -ForegroundColor Green }
  else { $script:fail++; Write-Host "FAIL $cas/$tag :: $detail" -ForegroundColor Red }
}
function RunIface($exe, $envmap){
  $t = Join-Path $env:TEMP ("wims-eq-" + [guid]::NewGuid().ToString('n'))
  New-Item -ItemType Directory -Path $t | Out-Null
  $saved=@{}
  foreach($k in $envmap.Keys){
    $saved[$k]=[Environment]::GetEnvironmentVariable($k,'Process')
    [Environment]::SetEnvironmentVariable($k, [string]$envmap[$k], 'Process')
  }
  [Environment]::SetEnvironmentVariable('tmp_dir', $t, 'Process')
  $psi=New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName=(Join-Path $here $exe)
  $psi.WorkingDirectory=$t
  $psi.UseShellExecute=$false
  $psi.RedirectStandardOutput=$true
  $psi.RedirectStandardError=$true
  $psi.RedirectStandardInput=$true
  $psi.CreateNoWindow=$true
  $p=[Diagnostics.Process]::Start($psi)
  $p.StandardInput.Close()
  $outTask=$p.StandardOutput.ReadToEndAsync()
  $errTask=$p.StandardError.ReadToEndAsync()
  if(-not $p.WaitForExit(180000)){ try{$p.Kill()}catch{}; $rc=124 } else { $rc=$p.ExitCode }
  $out=Norm $outTask.Result
  $err=Norm $errTask.Result
  foreach($k in $saved.Keys){
    [Environment]::SetEnvironmentVariable($k, $saved[$k], 'Process')
  }
  Remove-Item -Recurse -Force $t -ErrorAction SilentlyContinue
  return @{rc=$rc; out=$out; err=$err}
}

$gapExe=Join-Path $here 'cas\gap\runtime\opt\gap-4.16.1\gap.exe'
$gapBin=Join-Path $here 'cas\gap\runtime\bin'
if(-not (Test-Path $gapExe)){ throw "GAP missing: $gapExe" }
# Prefill PATH with Cygwin runtime (DLL deps for gap.exe)
$env:PATH = "$gapBin;" + $env:PATH
$gapCmd = "$gapExe -T -n"

# Maxima: prefer direct binary under clisp tree if present; else maxima.bat (launcher)
$maxCand=@(
  'C:\maxima-5.49.0\bin\maxima.exe',
  'C:\maxima-5.49.0\lib\maxima\5.49.0\binary-sbcl\maxima.exe',
  'C:\maxima-5.49.0\bin\maxima.bat'
)
$maxCmd=$null
foreach($c in $maxCand){ if(Test-Path $c){ $maxCmd=$c; break } }
Get-ChildItem 'C:\maxima-5.49.0' -Recurse -Filter 'maxima.exe' -ErrorAction SilentlyContinue |
  Select-Object -First 3 FullName | ForEach-Object { Write-Host "found $($_.FullName)" }
if(-not $maxCmd){
  $m=Get-ChildItem 'C:\maxima-5.49.0' -Recurse -Filter 'maxima.exe' -ErrorAction SilentlyContinue | Select-Object -First 1
  if($m){ $maxCmd=$m.FullName } else { $maxCmd='C:\maxima-5.49.0\bin\maxima.bat' }
}

$oct='C:\Users\Shadow\AppData\Local\Programs\GNU Octave\Octave-11.3.0\mingw64\bin\octave-cli.exe'
$octCmd="$oct -Hf --no-line-editing"
$fake=Join-Path $here 'fake_cas.exe'

Write-Host "GAP=$gapCmd"
Write-Host "MAXIMA=$maxCmd"
Write-Host "OCTAVE=$octCmd"

# --- scenarios ---
# calc
$r=RunIface 'gap.exe' @{ wims_exec_parm='2+3'; gap_command=$gapCmd }
Rec 'gap' 'calc' ($r.out.Trim() -eq '5') "out=$($r.out.Trim()) rc=$($r.rc)"
$r=RunIface 'maxima.exe' @{ wims_exec_parm='2+3'; maxima_command=$maxCmd }
Rec 'maxima' 'calc' ($r.out.Trim() -eq '5') "out=$($r.out.Trim()) rc=$($r.rc)"
$r=RunIface 'octave.exe' @{ wims_exec_parm='1+1'; octave_command=$octCmd }
Rec 'octave' 'calc' ($r.out.Trim() -match '^\s*2\s*$') "out=$($r.out.Trim()) rc=$($r.rc)"

# factor / nontrivial
$r=RunIface 'gap.exe' @{ wims_exec_parm='Factorial(10)'; gap_command=$gapCmd }
Rec 'gap' 'factor' ($r.out.Trim() -eq '3628800') "out=$($r.out.Trim())"
$r=RunIface 'maxima.exe' @{ wims_exec_parm='factor(x^4-1)'; maxima_command=$maxCmd }
Rec 'maxima' 'factor' ($r.out -match '\(x-1\)' -and $r.out -match '\(x\+1\)') "out=$($r.out.Trim())"
$r=RunIface 'octave.exe' @{ wims_exec_parm='factor(210)'; octave_command=$octCmd }
Rec 'octave' 'factor' ($r.out -match '2' -and $r.out -match '3' -and $r.out -match '5' -and $r.out -match '7') "out=$($r.out.Trim())"

# large / fsizelim-bounded (real CAS; expect substantial but capped output)
$r=RunIface 'gap.exe' @{ wims_exec_parm='List([1..8000],x->x)'; gap_command=$gapCmd }
Rec 'gap' 'large' ($r.out.Length -gt 10000 -and $r.out.Length -le 140000) "bytes=$($r.out.Length)"
$r=RunIface 'maxima.exe' @{ wims_exec_parm='makelist(i,i,1,20000)'; maxima_command=$maxCmd }
Rec 'maxima' 'large' ($r.out.Length -gt 10000 -and $r.out.Length -le 140000) "bytes=$($r.out.Length)"
$r=RunIface 'octave.exe' @{ wims_exec_parm='num2str(1:15000)'; octave_command=$octCmd }
Rec 'octave' 'large' ($r.out.Length -gt 10000 -and $r.out.Length -le 140000) "bytes=$($r.out.Length)"

# illegal filtered
$r=RunIface 'gap.exe' @{ wims_exec_parm='Exec("id")'; gap_command=$gapCmd }
Rec 'gap' 'illegal' ($r.out -notmatch 'uid=' -and $r.err -notmatch 'uid=') "out='$($r.out.Trim())'"
$r=RunIface 'maxima.exe' @{ wims_exec_parm='system("id")'; maxima_command=$maxCmd }
Rec 'maxima' 'illegal' ($r.out -notmatch 'uid=' -and ($r.out -match 'sjstem' -or $r.out.Trim() -eq '' -or $r.out -match 'system')) "out='$($r.out.Trim())'"
$r=RunIface 'octave.exe' @{ wims_exec_parm='system("id")'; octave_command=$octCmd }
Rec 'octave' 'illegal' ($r.out -notmatch 'uid=' -and $r.err -notmatch 'uid=') "out='$($r.out.Trim())' err_has_filter=$($r.err -match 'illegal|error|sjstem|system')"

# about (version may differ from Linux)
$r=RunIface 'gap.exe' @{ wims_exec_parm='about'; gap_command=$gapCmd }
Rec 'gap' 'about' ($r.out -match 'gap-system' -and $r.out -match 'GAP') "out=$($r.out.Trim().Substring(0,[Math]::Min(80,$r.out.Trim().Length)))"
$r=RunIface 'maxima.exe' @{ wims_exec_parm='about'; maxima_command=$maxCmd }
Rec 'maxima' 'about' ($r.out -match 'Maxima') "out=$($r.out.Trim().Substring(0,[Math]::Min(80,$r.out.Trim().Length)))"
$r=RunIface 'octave.exe' @{ wims_exec_parm='about'; octave_command=$octCmd }
Rec 'octave' 'about' ($r.out -match 'Octave') "out=$($r.out.Trim().Substring(0,[Math]::Min(80,$r.out.Trim().Length)))"

# timeout: command that never speaks the CAS protocol
$r=RunIface 'gap.exe' @{ wims_exec_parm='1+1'; gap_command="$fake silent" }
Rec 'gap' 'timeout' ($r.out.Trim() -eq '' -and $r.err -match 'time out') "err=$($r.err.Trim())"
$r=RunIface 'maxima.exe' @{ wims_exec_parm='1+1'; maxima_command="$fake silent" }
Rec 'maxima' 'timeout' ($r.out.Trim() -eq '' -and $r.err -match 'time out') "err=$($r.err.Trim())"
$r=RunIface 'octave.exe' @{ wims_exec_parm='1+1'; octave_command="$fake silent" }
Rec 'octave' 'timeout' ($r.out.Trim() -eq '' -and $r.err -match 'time out') "err=$($r.err.Trim())"

# CAS absent
$r=RunIface 'gap.exe' @{ wims_exec_parm='1+1'; gap_command='D:\nonexistent\gap.exe' }
Rec 'gap' 'absent' ($r.err -match 'not_INStalled') "err=$($r.err.Trim())"
$r=RunIface 'maxima.exe' @{ wims_exec_parm='1+1'; maxima_command='D:\nonexistent\maxima.exe' }
Rec 'maxima' 'absent' ($r.err -match 'not_INStalled') "err=$($r.err.Trim())"
$r=RunIface 'octave.exe' @{ wims_exec_parm='1+1'; octave_command='D:\nonexistent\octave.exe' }
Rec 'octave' 'absent' ($r.err -match 'not_INStalled') "err=$($r.err.Trim())"

# multiexec
$py=(Get-Command python -ErrorAction SilentlyContinue); if(-not $py){ $py=Get-Command py -ErrorAction SilentlyContinue }
$drv=Join-Path $here 'multiexec_driver.py'
function RunMx($cas,$exe,$cmdKey,$cmd,$reqs,$needle){
  $saved=[Environment]::GetEnvironmentVariable($cmdKey,'Process')
  [Environment]::SetEnvironmentVariable($cmdKey,$cmd,'Process')
  [Environment]::SetEnvironmentVariable('PATH', "$gapBin;" + [Environment]::GetEnvironmentVariable('PATH','Process'), 'Process')
  if($cas -eq 'maxima'){
    [Environment]::SetEnvironmentVariable('MAXIMA_PREFIX','C:/maxima-5.49.0','Process')
    [Environment]::SetEnvironmentVariable('SBCL_HOME','C:/maxima-5.49.0/bin','Process')
  }
  $out = & $py.Source $drv (Join-Path $here $exe) 'MXRND42' @reqs 2>&1 | Out-String
  $out = Norm $out
  [Environment]::SetEnvironmentVariable($cmdKey,$saved,'Process')
  $ok=$true
  foreach($n in $needle){ if($out -notmatch [regex]::Escape($n)){ $ok=$false } }
  Rec $cas 'multiexec' $ok ($out.Trim().Substring(0,[Math]::Min(120,$out.Trim().Length)))
}
if($py -and (Test-Path $drv)){
  RunMx 'gap' 'gap.exe' 'gap_command' $gapCmd @('a:=5;;','a+1;','Factorial(5);') @('6','120')
  RunMx 'maxima' 'maxima.exe' 'maxima_command' $maxCmd @('a:5','a+1','expand((x+1)^2)') @('6','x^2+2*x+1')
  RunMx 'octave' 'octave.exe' 'octave_command' $octCmd @('a=5','a+1','factor(12)') @('6')
} else { Write-Host 'SKIP multiexec' }

$rows | Export-Csv -NoTypeInformation (Join-Path $here 'equiv_pc_results.csv')
$rows | ConvertTo-Json | Set-Content (Join-Path $here 'equiv_pc_results.json') -Encoding UTF8
Write-Host "== result: $pass passed, $fail failed =="
if($fail -gt 0){ exit 1 } else { exit 0 }
