# CLAUDE_V1
# install-runtime-r19040.ps1 — Build the WIMS runtime from SVN r19040
#
# Usage: .\tools\install-runtime-r19040.ps1 [-WimsHome C:\wims]
#
# Prerequisites: SlikSVN (svn.exe in PATH)
# This script:
#   1. SVN-exports public_html, other/, log/ from trunk r19040
#   2. Copies binaries from tests-win/ (or build/)
#   3. Creates required runtime directories
#   4. Runs a CGI smoke test

param(
    [string]$WimsHome = "C:\wims",
    [string]$BinSource = "",
    [switch]$SkipSvn,
    [switch]$SkipTest
)

$ErrorActionPreference = "Stop"
$SVN_URL = "https://subversion.renater.fr/anonscm/svn/wimsdev/trunk/wims"
$REV = 19040

Write-Host "=== WIMS4WIN Runtime Installer (r$REV) ===" -ForegroundColor Cyan
Write-Host "Target: $WimsHome"

# ── 0. Check prerequisites ──────────────────────────────────────────

if (-not $SkipSvn) {
    try { $null = svn --version --quiet 2>$null }
    catch { throw "SlikSVN not found. Install: winget install SlikSvn.SlikSvn" }
}

# ── 1. SVN export ────────────────────────────────────────────────────

if (-not $SkipSvn) {
    Write-Host "`n[1/4] SVN export from r$REV..." -ForegroundColor Yellow

    foreach ($dir in @("public_html", "other", "log")) {
        $dest = "$WimsHome\$dir"
        if (Test-Path $dest) {
            Write-Host "  $dir already exists, skipping (use -Force to overwrite)"
            continue
        }
        Write-Host "  Exporting $dir..."
        svn export -r $REV "$SVN_URL/$dir" $dest --quiet
        $count = (Get-ChildItem -Recurse $dest -File -EA SilentlyContinue | Measure-Object).Count
        Write-Host "  $dir : $count files"
    }
} else {
    Write-Host "`n[1/4] SVN export skipped (-SkipSvn)" -ForegroundColor DarkGray
}

# ── 2. Runtime directories ───────────────────────────────────────────

Write-Host "`n[2/4] Creating runtime directories..." -ForegroundColor Yellow
foreach ($d in @("bin", "sessions", "tmp\log\cquota", "log\cquota", "s2")) {
    New-Item -ItemType Directory -Force "$WimsHome\$d" | Out-Null
}
Write-Host "  Done"

# ── 3. Binaries ─────────────────────────────────────────────────────

Write-Host "`n[3/4] Copying binaries..." -ForegroundColor Yellow

# Auto-detect bin source
if (-not $BinSource) {
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    $repoRoot = Split-Path -Parent $scriptDir
    $candidates = @(
        "$repoRoot\..\tests-win",
        "$repoRoot\build\wims-win",
        "$repoRoot\build\win32",
        "$repoRoot\build\tests-win"
    )
    foreach ($c in $candidates) {
        if (Test-Path "$c\wims-home\bin\wims.exe" -or (Test-Path "$c\wims.exe")) {
            $BinSource = (Resolve-Path $c).Path
            break
        }
    }
}

if ($BinSource -and (Test-Path $BinSource)) {
    # Core binaries
    $core = @(
        @{src="wims-home\bin\wims.exe";     dst="bin\wims.exe"},
        @{src="wims-home\bin\wimslogd.exe"; dst="bin\wimslogd.exe"},
        @{src="wims_mathml.exe";            dst="bin\wims_mathml.exe"},
        @{src="ch..root.exe";               dst="bin\ch..root.exe"}
    )
    foreach ($b in $core) {
        $s = "$BinSource\$($b.src)"
        if (Test-Path $s) {
            Copy-Item $s "$WimsHome\$($b.dst)" -Force
            Write-Host "  [OK] $($b.dst)"
        } else {
            Write-Host "  [MISS] $($b.src)" -ForegroundColor Yellow
        }
    }

    # CAS interfaces
    foreach ($cas in @("pari","maxima","gap","octave","m2","scilab","yacas")) {
        $s = "$BinSource\$cas.exe"
        if (Test-Path $s) {
            Copy-Item $s "$WimsHome\bin\" -Force
            Write-Host "  [OK] bin\$cas.exe"
        }
    }
} else {
    Write-Host "  No binary source found. Copy binaries manually to $WimsHome\bin\" -ForegroundColor Yellow
}

# ── 4. Smoke test ────────────────────────────────────────────────────

if (-not $SkipTest) {
    Write-Host "`n[4/4] CGI smoke test..." -ForegroundColor Yellow

    # Start wimslogd
    try { Get-Process wimslogd -EA Stop | Stop-Process -Force -Confirm:$false } catch {}
    Start-Sleep 1

    if (Test-Path "$WimsHome\bin\wimslogd.exe") {
        Start-Process -FilePath "$WimsHome\bin\wimslogd.exe" -WorkingDirectory $WimsHome -WindowStyle Hidden
        Start-Sleep 2
    }

    if (Test-Path "$WimsHome\bin\wims.exe") {
        $env:REQUEST_METHOD = "GET"
        $env:SERVER_SOFTWARE = "wims-test"
        $env:SERVER_NAME = "localhost"
        $env:SERVER_PORT = "80"
        $env:SERVER_PROTOCOL = "HTTP/1.0"
        $env:GATEWAY_INTERFACE = "CGI/1.1"
        $env:SCRIPT_NAME = "/wims/wims.cgi"
        $env:REMOTE_ADDR = "127.0.0.1"
        $env:SERVER_ADDR = "127.0.0.1"
        $env:QUERY_STRING = "module=adm/doc&lang=en"

        New-Item -ItemType Directory -Force "$WimsHome\tmp" | Out-Null
        $p = Start-Process -FilePath "$WimsHome\bin\wims.exe" `
            -WorkingDirectory "$WimsHome\public_html" `
            -NoNewWindow -Wait -PassThru `
            -RedirectStandardOutput "$WimsHome\tmp\cgi.out" `
            -RedirectStandardError "$WimsHome\tmp\cgi.err"

        $out = Get-Content "$WimsHome\tmp\cgi.out" -Raw -EA SilentlyContinue
        if ($out -match "Status: 200") {
            Write-Host "  PASS CGI Status 200" -ForegroundColor Green
        } else {
            Write-Host "  FAIL (exit=$($p.ExitCode))" -ForegroundColor Red
            $err = Get-Content "$WimsHome\tmp\cgi.err" -Raw -EA SilentlyContinue
            if ($err) { Write-Host "  $($err.Substring(0, [Math]::Min(200, $err.Length)))" }
        }
    } else {
        Write-Host "  Skipped (wims.exe not found)" -ForegroundColor DarkGray
    }

    # Cleanup
    try { Get-Process wimslogd -EA Stop | Stop-Process -Force -Confirm:$false } catch {}
} else {
    Write-Host "`n[4/4] Smoke test skipped (-SkipTest)" -ForegroundColor DarkGray
}

# ── Summary ──────────────────────────────────────────────────────────

Write-Host "`n=== Installation complete ===" -ForegroundColor Cyan
$phCount = (Get-ChildItem -Recurse "$WimsHome\public_html" -File -EA SilentlyContinue | Measure-Object).Count
$binCount = (Get-ChildItem "$WimsHome\bin\*.exe" -EA SilentlyContinue | Measure-Object).Count
Write-Host "  public_html: $phCount files"
Write-Host "  bin: $binCount executables"
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Configure Apache/Laragon (see docs/04_Guide_Installation_et_CGI_Laragon.md)"
Write-Host "  2. Install CAS: winget install MaximaTeam.Maxima GNU.Octave"
Write-Host "  3. Start: wimslogd.exe then Apache"
Write-Host "  4. Browse: http://localhost/wims/wims.cgi?module=adm/doc"
