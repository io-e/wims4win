# set-cas-conf.ps1 - (re)write the CAS command lines of a wims4win runtime's
# log\wims.conf: pari_command, maxima_command, gap_command, octave_command.
# Idempotent; backs up wims.conf first; replaces any existing *_command line
# (other lines untouched, LF line endings kept).
#
#   .\tools\set-cas-conf.ps1 -Runtime D:\...\wims-runtime-xyz -CasRoot D:\...\tests-win\cas
#   .\tools\set-cas-conf.ps1 -Runtime ... -CasRoot ... -WhatIf     # show only
#
# Layout expected under -CasRoot (same as the hook-20261106 runtime):
#   pari\gp.exe
#   maxima\maxima-5.49.0\bin\sbcl.exe + lib\maxima\5.49.0\binary-sbcl\maxima.core
#   gap\runtime\opt\gap-4.16.1\gap.exe
#   octave\octave-11.3.0-w64\mingw64\bin\octave-cli.exe
[CmdletBinding(SupportsShouldProcess)]
param(
  [Parameter(Mandatory)][string]$Runtime,
  [Parameter(Mandatory)][string]$CasRoot
)
$ErrorActionPreference = 'Stop'
$conf = Join-Path $Runtime 'log\wims.conf'
if (-not (Test-Path $conf)) { throw "no log\wims.conf under $Runtime" }
$cr = (Resolve-Path $CasRoot).Path
$gp   = Join-Path $cr 'pari\gp.exe'
$sbcl = Join-Path $cr 'maxima\maxima-5.49.0\bin\sbcl.exe'
$core = Join-Path $cr 'maxima\maxima-5.49.0\lib\maxima\5.49.0\binary-sbcl\maxima.core'
$gap  = Join-Path $cr 'gap\runtime\opt\gap-4.16.1\gap.exe'
$oct  = Join-Path $cr 'octave\octave-11.3.0-w64\mingw64\bin\octave-cli.exe'
$lines = [ordered]@{}
if (Test-Path $gp)  { $lines.pari_command   = ($gp -replace '\\','/') + ' -f -q -p 150000' } else { Write-Warning "missing $gp" }
if (Test-Path $gap) { $lines.gap_command    = "$gap -T -n" } else { Write-Warning "missing $gap" }
if ((Test-Path $sbcl) -and (Test-Path $core)) {
  $lines.maxima_command = "$sbcl --core $core --noinform --dynamic-space-size 512 --end-runtime-options --eval (cl-user::run) --end-toplevel-options"
} else { Write-Warning "missing sbcl.exe or maxima.core under $cr\maxima" }
if (Test-Path $oct) { $lines.octave_command = "$oct -Hq --no-line-editing --no-window-system --silent" } else { Write-Warning "missing $oct" }
$old = [IO.File]::ReadAllText($conf)
$keep = @(($old -split "`r?`n") | Where-Object { $_ -ne '' -and $_ -notmatch '^(pari|maxima|gap|octave)_command=' })   # @() : a one-line conf must stay an array
$new = (($keep + ($lines.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" })) -join "`n") + "`n"
if ($PSCmdlet.ShouldProcess($conf, "write $($lines.Count) CAS command lines")) {
  Copy-Item $conf "$conf.bak-$(Get-Date -Format yyyyMMdd-HHmmss)"
  [IO.File]::WriteAllText($conf, $new, (New-Object Text.UTF8Encoding($false)))
}
$lines.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }
