# GROK_V1
# Install AlgoGebra JSON hook into a WIMS Windows runtime tree.
# Usage:
#   powershell -File AlgoGebra_Wims_Integration/runtime_patch/install_algogebra_hook_GROK_V1.ps1
#   powershell -File ... -RuntimeRoot "D:\path\to\wims-runtime-oef"

param(
    [string]$RuntimeRoot = "D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef"
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
if ($text -notmatch [regex]::Escape($marker)) {
    $needle = "!!end of .answer_analysis`r`n</div>`r`n!!presentsol=1 deprecated"
    $needle2 = "!!end of .answer_analysis`n</div>`n!!presentsol=1 deprecated"
    $inject = @"
!!end of .answer_analysis
</div>
!! GROK_V1 AlgoGebra JSON hook (special_parm=algogebra_json)
!if `$special_parm=algogebra_json
<!-- ALGOGEBRA_JSON_BEGIN
!read oef/algogebra_json.phtml
ALGOGEBRA_JSON_END -->
!endif
!!presentsol=1 deprecated
"@
    if ($text.Contains("!!end of .answer_analysis") -and $text.Contains("!!presentsol=1 deprecated")) {
        $bak = "$answer.bak_pre_algogebra"
        if (-not (Test-Path $bak)) { Copy-Item $answer $bak }
        $pattern = '(?s)(!!end of \.answer_analysis\r?\n</div>\r?\n)(!!presentsol=1 deprecated)'
        $repl = "`$1!! GROK_V1 AlgoGebra JSON hook (special_parm=algogebra_json)`r`n!if `$special_parm=algogebra_json`r`n<!-- ALGOGEBRA_JSON_BEGIN`r`n!read oef/algogebra_json.phtml`r`nALGOGEBRA_JSON_END -->`r`n!endif`r`n`$2"
        $newText = [regex]::Replace($text, $pattern, $repl, 1)
        if ($newText -eq $text) { throw "injection pattern did not match in answer.phtml" }
        Set-Content -Path $answer -Value $newText -Encoding Default -NoNewline
        Write-Host "Patched $answer"
    } else {
        throw "Unexpected answer.phtml layout; patch manually using answer_phtml_injection_GROK_V1.snippet"
    }
} else {
    Write-Host "answer.phtml already contains AlgoGebra hook"
}

Write-Host "Installed $hookDst"
Write-Host "Done. Smoke test:"
Write-Host "  python AlgoGebra_Wims_Integration/tests/test_algogebra_json_GROK_V1.py"
