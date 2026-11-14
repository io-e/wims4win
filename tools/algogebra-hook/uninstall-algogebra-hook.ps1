# Copyright (C) 2026 wims4win contributors. GNU GPL v2 (see NOTICE.md).
# Remove the AlgoGebra JSON hook from a WIMS Windows runtime tree.
# Restores answer.phtml from answer.phtml.bak_pre_algogebra when present;
# otherwise strips the injected block.
#
# Usage:
#   powershell -File tools/algogebra-hook/uninstall-algogebra-hook.ps1 -RuntimeRoot D:\path\to\wims-runtime

param(
    [Parameter(Mandatory = $true)]
    [string]$RuntimeRoot
)

$ErrorActionPreference = "Stop"
$oef = Join-Path $RuntimeRoot "public_html\scripts\oef"
$answer = Join-Path $oef "answer.phtml"
$hook = Join-Path $oef "algogebra_json.phtml"
$bak = "$answer.bak_pre_algogebra"

if (-not (Test-Path $answer)) { throw "answer.phtml not found: $answer" }

if (Test-Path $bak) {
    Copy-Item $bak $answer -Force
    Write-Host "Restored $answer from answer.phtml.bak_pre_algogebra"
} else {
    $text = Get-Content -Raw -Encoding Default $answer
    $pattern = '(?s)!! GROK_V1 AlgoGebra JSON hook \(special_parm=algogebra_json\)\r?\n!if \$special_parm=algogebra_json\r?\n<!-- ALGOGEBRA_JSON_BEGIN\r?\n!read oef/algogebra_json.phtml\r?\nALGOGEBRA_JSON_END -->\r?\n!endif\r?\n'
    $newText = [regex]::Replace($text, $pattern, '', 1)
    if ($newText -eq $text -and $text -match 'GROK_V1 AlgoGebra JSON hook') {
        throw "could not strip hook block from answer.phtml"
    }
    Set-Content -Path $answer -Value $newText -Encoding Default -NoNewline
    Write-Host "Stripped hook block from $answer"
}

if (Test-Path $hook) { Remove-Item $hook -Force; Write-Host "Removed $hook" }
