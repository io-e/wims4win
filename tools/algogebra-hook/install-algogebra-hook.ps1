# Copyright (C) 2026 wims4win contributors. GNU GPL v2 (see NOTICE.md).
# Install the AlgoGebra JSON hook into a WIMS Windows runtime tree.
# Idempotent; keeps answer.phtml.bak_pre_algogebra for uninstall.
#
# Usage:
#   powershell -File tools/algogebra-hook/install-algogebra-hook.ps1 -RuntimeRoot D:\path\to\wims-runtime

param(
    [Parameter(Mandatory = $true)]
    [string]$RuntimeRoot
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$oef = Join-Path $RuntimeRoot "public_html\scripts\oef"
$answer = Join-Path $oef "answer.phtml"
$hookSrc = Join-Path $here "algogebra_json.phtml"
$hookDst = Join-Path $oef "algogebra_json.phtml"

if (-not (Test-Path $answer)) { throw "answer.phtml not found: $answer" }
if (-not (Test-Path $hookSrc)) { throw "missing $hookSrc" }

Copy-Item $hookSrc $hookDst -Force

$marker = "GROK_V1 AlgoGebra JSON hook"
$text = Get-Content -Raw -Encoding Default $answer
if ($text -match [regex]::Escape($marker)) {
    Write-Host "answer.phtml already contains AlgoGebra hook"
    Write-Host "Installed $hookDst"
    exit 0
}

$bak = "$answer.bak_pre_algogebra"
if (-not (Test-Path $bak)) { Copy-Item $answer $bak }

$pattern = '(?s)(!!end of \.answer_analysis\r?\n</div>\r?\n)(!!presentsol=1 deprecated)'
$repl = "`$1!! GROK_V1 AlgoGebra JSON hook (special_parm=algogebra_json)`r`n!if `$special_parm=algogebra_json`r`n<!-- ALGOGEBRA_JSON_BEGIN`r`n!read oef/algogebra_json.phtml`r`nALGOGEBRA_JSON_END -->`r`n!endif`r`n`$2"
$newText = [regex]::Replace($text, $pattern, $repl, 1)
if ($newText -eq $text) { throw "injection pattern did not match in answer.phtml" }
Set-Content -Path $answer -Value $newText -Encoding Default -NoNewline
Write-Host "Patched $answer"
Write-Host "Installed $hookDst"
