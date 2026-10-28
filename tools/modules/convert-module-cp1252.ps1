<#
.SYNOPSIS
  Re-encode a WIMS module from UTF-8 to windows-1252 (the charset WIMS 4.33a
  serves for fr/en/es/de/it/nl/pt/ca, see src/wims.c charname[]).
.DESCRIPTION
  WIMS r19040 sends "Content-type: text/html; charset=windows-1252" for French
  modules on Linux AND Windows; upstream modules are ISO-8859-1. A module whose
  .oef/.def/INDEX files are UTF-8 therefore shows mojibake (deg sign -> "A-circumflex deg").
  This script converts only files that are valid UTF-8 and contain 8-bit bytes;
  files already in Latin-1 are left untouched. Characters outside cp1252 are
  written as HTML numeric entities (&#NNNN;). A .utf8.bak copy is kept unless -NoBackup.
.EXAMPLE
  pwsh -File convert-module-cp1252.ps1 -Module D:\...\public_html\modules\local\oef2html_web.fr
#>
param([Parameter(Mandatory)][string]$Module, [switch]$NoBackup, [switch]$WhatIf)
try { [Text.Encoding]::RegisterProvider([Text.CodePagesEncodingProvider]::Instance) } catch {}
$strict = [Text.UTF8Encoding]::new($false, $true)
$cp = [Text.Encoding]::GetEncoding(1252, [Text.EncoderFallback]::ExceptionFallback, [Text.DecoderFallback]::ExceptionFallback)
$skip = '\.(gif|png|jpe?g|svg|ico|pdf|zip|gz|tgz|mp3|ogg|wav|jar|class|bak)$'
$n = 0
Get-ChildItem -LiteralPath $Module -Recurse -File -Force | Where-Object { $_.Name -notmatch $skip } | ForEach-Object {
  try { $b = [IO.File]::ReadAllBytes($_.FullName) } catch { return }
  if (-not ($b | Where-Object { $_ -ge 0x80 } | Select-Object -First 1)) { return }
  try { $s = $strict.GetString($b) } catch { return }          # not UTF-8: already 8-bit legacy
  if ($s.Length -gt 0 -and $s[0] -eq [char]0xFEFF) { $s = $s.Substring(1) }
  $sb = [Text.StringBuilder]::new()
  foreach ($ch in $s.ToCharArray()) {
    if ([int]$ch -lt 0x80) { [void]$sb.Append($ch); continue }
    try { [void]$cp.GetBytes([string]$ch); [void]$sb.Append($ch) }
    catch { if ([char]::IsHighSurrogate($ch)) { $hs = $ch; continue }
            if ([char]::IsLowSurrogate($ch)) { [void]$sb.Append('&#' + [char]::ConvertToUtf32($hs, $ch) + ';'); continue }
            [void]$sb.Append('&#' + [int]$ch + ';') }
  }
  $out = [Text.Encoding]::GetEncoding(1252).GetBytes($sb.ToString())
  $n++
  if ($WhatIf) { "would convert $($_.FullName)"; return }
  if (-not $NoBackup) { [IO.File]::WriteAllBytes($_.FullName + '.utf8.bak', $b) }
  [IO.File]::WriteAllBytes($_.FullName, $out)
  "converted $($_.FullName)"
}
"$n file(s) converted to windows-1252"
