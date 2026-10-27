# 04 — Guide d'installation et CGI sous Laragon (document consolidé)

*Document consolidé le 27/10/2026 (heure de Paris). Il fusionne le guide d'installation et le guide CGI Laragon de Claude avec les procédures de Grok.*

## Sources fusionnées

| Source | Auteur | Partie |
|---|---|---|
| `archive/Guide_Installation_Wims4Win_CLAUDE_V1.md` | Claude | A — installation complète |
| `archive/Guide_CGI_Laragon_WIMS_CLAUDE_V1_1.md` | Claude | B — CGI sous Laragon |
| `apache-laragon-wims-runtime.conf`, `tools/build-runtime-tree.sh`, `tools/install-cas-msys2.md`, `Suivi_Developpement_Portage.md` | Grok | Corrections ci-dessous (fichiers restés en place, utilisés par le build) |

## Corrections à appliquer en lisant les parties A et B

1. **Runtime à utiliser :** celui de Grok, construit par `tools/build-runtime-tree.sh` (à partir de `wims-4.32.tgz`, liens symboliques déréférencés, `default_texposition=2`, `log/wims4win.yaml`, `flydraw.exe`, `insdraw..processor.exe`). C'est lui que sert le vhost Laragon `wims_paris_saclay_fr.test`. Le runtime extrait du `.deb` n'est plus utilisé.
2. **Maxima :** `maxima_command` lance `sbcl.exe --core …\maxima.core` directement ; `maxima.bat` ne doit jamais être appelé. `SBCL_HOME` et `MAXIMA_PREFIX` sont déduits du chemin de `sbcl.exe`.
3. **GAP :** validé (« non encore validé » en A §4.4 est périmé) : `gap.exe` + runtime Cygwin dans le `PATH`.
4. **PARI/GP :** `pari_command=<chemin>/gp.exe -f -q -p 150000` dans `log/wims.conf` des runtimes `wims-runtime` et `wims-runtime-oef`.
5. **Taille des formules (DYS) :** `log/wims4win.yaml`, `formula: size: 200%` (50 % à 400 %).
6. **MSYS2 `sh.exe` :** décrit en A §2.2 comme prérequis ; le moteur actuel n'en dépend pas pour `mv`/`rm`. Les modules qui lancent des scripts sh peuvent encore échouer.
7. **wimslogd :** démarré à la volée par `wims.exe` ; un named pipe par arbre runtime, plus de collision entre instances.
8. **Sécurité :** n'ouvrir le serveur aux élèves qu'après les tests S1–S9 (issue #8).
9. **Mot de passe webmaster** (`log/.wimspass`) : toujours vide, à définir.


---

## Partie A — Guide d'installation (Claude V1)

> Source : `archive/Guide_Installation_Wims4Win_CLAUDE_V1.md`

## Guide d'installation WIMS 4.33a sur Windows (Wims4Win)

**Date :** 2026-10-23
**Auteur :** Claude
**Portage :** Grok Bot (code) / Claude (architecture, audit)
**Source :** wims-port HEAD `ad7a415` (22 commits, Phases 1-4)
**Prérequis :** Windows 10 1803+ ou Windows 11

---

### Table des matieres

1. [Vue d'ensemble](#1-vue-densemble)
2. [Prerequis systeme](#2-prerequis-systeme)
3. [Installation des binaires WIMS](#3-installation-des-binaires-wims)
4. [Installation des CAS](#4-installation-des-cas)
5. [Configuration Apache](#5-configuration-apache)
6. [Premier demarrage](#6-premier-demarrage)
7. [Verification](#7-verification)
8. [Securite](#8-securite)
9. [Depannage](#9-depannage)
10. [Limites connues](#10-limites-connues)

---

### 1. Vue d'ensemble

#### Architecture

WIMS est un serveur educatif CGI (C, ~50k lignes) qui s'execute derriere Apache.
Le portage Windows natif (mingw-w64) remplace les primitives POSIX (fork/exec,
pipes, chroot, mmap, sockets) par des equivalents Win32 :

```
Navigateur → Apache (mod_cgi) → wims.exe (CGI)
                                    ├── wimslogd.exe (daemon log, named pipe IPC)
                                    ├── ch..root.exe  (sandbox AppContainer)
                                    ├── pari.exe      (interface → gp.exe)
                                    ├── maxima.exe    (interface → sbcl.exe)
                                    ├── octave.exe    (interface → octave-cli.exe)
                                    ├── gap.exe       (interface → gap.exe)
                                    └── wims_mathml.exe (MathML)
```

#### Arborescence cible

```
C:\wims\                         ← WIMS_HOME (racine d'installation)
├── bin\
│   ├── wims.exe                 ← moteur CGI
│   ├── wimslogd.exe             ← daemon log
│   ├── wims_mathml.exe          ← MathML helper
│   ├── ch..root.exe             ← sandbox
│   ├── pari.exe                 ← interfaces CAS (7 fichiers)
│   ├── maxima.exe
│   ├── octave.exe
│   ├── gap.exe
│   ├── m2.exe
│   ├── scilab.exe
│   └── yacas.exe
├── public_html\                 ← contenu web (modules, scripts, html, CSS)
│   ├── modules\                 ← modules OEF (adm, H1, etc.)
│   ├── scripts\                 ← JS, images
│   ├── html\                    ← templates
│   └── msg\                     ← messages d'erreur i18n
├── sessions\                    ← sessions utilisateur (cree au runtime)
├── tmp\                         ← temporaire
│   └── log\                     ← logs wimslogd
│       └── cquota\              ← quotas
├── log\
│   └── cquota\
├── s2\                          ← donnees internes
└── other\                       ← sh.exe + coreutils (MSYS2)
    └── bin\
        ├── sh.exe
        ├── cat.exe
        ├── sed.exe
        ├── awk.exe
        ├── grep.exe
        └── ...
```

---

### 2. Prerequis systeme

#### 2.1 Windows

| Composant | Minimum | Recommande | Note |
|-----------|---------|------------|------|
| Windows | 10 1803 (RS4) | 11 | AppContainer requis |
| RAM | 2 Go | 4 Go+ | Selon nombre de CAS |
| Disque | 500 Mo (base) | 2 Go+ (avec CAS) | public_html = ~200 Mo |

#### 2.2 Logiciels requis

##### Apache HTTP Server

**Option A — Laragon (recommande pour tester)**

Laragon inclut Apache + PHP + MySQL et se gere via une UI.
Telecharger : https://laragon.org/download/

**Option B — Apache Lounge (production)**

Telecharger Apache 2.4 Win64 depuis https://www.apachelounge.com/download/
Installer en tant que service Windows.

##### MSYS2 (pour sh.exe et les coreutils)

WIMS utilise des scripts shell (.sh) pour compiler les OEF, generer des index, etc.
Ces scripts ont besoin d'un `sh.exe` POSIX.

```powershell
# Installer MSYS2
winget install MSYS2.MSYS2

# Dans le terminal MSYS2 (ucrt64) :
pacman -S coreutils sed gawk grep findutils
```

Le chemin par defaut est `C:\msys64\ucrt64\bin\` et `C:\msys64\usr\bin\`.

---

### 3. Installation des binaires WIMS

#### 3.1 Option A — Depuis les binaires pre-compiles (tests-win)

Les binaires de la Phase 4 sont dans `Reprise_Wims_Grok/tests-win/`.

```powershell
# Creer l'arborescence
$WIMS = "C:\wims"
New-Item -ItemType Directory -Force `
  "$WIMS\bin", "$WIMS\sessions", "$WIMS\tmp\log\cquota", `
  "$WIMS\log\cquota", "$WIMS\s2"

# Copier les binaires
$SRC = "D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\tests-win"
Copy-Item "$SRC\wims-home\bin\wims.exe"      "$WIMS\bin\"
Copy-Item "$SRC\wims-home\bin\wimslogd.exe"  "$WIMS\bin\"
Copy-Item "$SRC\wims_mathml.exe"             "$WIMS\bin\"
Copy-Item "$SRC\ch..root.exe"                "$WIMS\bin\"

# Interfaces CAS
foreach ($cas in @("pari","maxima","gap","octave","m2","scilab","yacas")) {
    Copy-Item "$SRC\$cas.exe" "$WIMS\bin\" -ErrorAction SilentlyContinue
}

# Copier public_html (contenu web complet)
Copy-Item "$SRC\wims-home\public_html" "$WIMS\public_html" -Recurse
```

#### 3.2 Option B — Compiler depuis les sources (cross-build)

Necessite un hote Linux avec `mingw-w64` installe (ou MSYS2 sur Windows).

```bash
cd wims-port/

# 1. Interfaces CAS (7 .exe)
tools/build-win32.sh build/win32

# 2. wims.exe (moteur CGI)
tools/build-wims-win32.sh build/wims-win

# 3. wimslogd.exe
tools/build-wimslogd-win32.sh build/wims-win

# 4. ch..root.exe (sandbox)
tools/build-chroot-win32.sh build/tests-win

# 5. wims_mathml.exe (necessite flex + bison + mingw-g++)
tools/build-wims-mathml-win32.sh

# Resultats dans build/win32/ et build/wims-win/
```

#### 3.3 public_html — d'ou vient le contenu web ?

Le repertoire `public_html/` provient du checkout SVN de WIMS (r19040).
Il contient ~1400 fichiers : modules OEF, templates HTML, JS, CSS, images.

```bash
# Sur le VPS Debian (Grok) :
svn co svn://svn.code.sf.net/p/wims/wims/trunk wims-svn
cp -a wims-svn/public_html/ /chemin/vers/wims4win/public_html/
```

Ce contenu est **identique** Linux/Windows — pas de compilation, juste une copie.

---

### 4. Installation des CAS

Les CAS (Computer Algebra Systems) sont des logiciels externes que WIMS appelle
via ses interfaces (.exe). Chaque interface lance le binaire CAS correspondant.

#### 4.1 PARI/GP (valide sur ShadowPC)

```powershell
# Via MSYS2 (recommande — binaire autonome)
# Dans le terminal MSYS2 ucrt64 :
pacman -S mingw-w64-ucrt-x86_64-pari

# Le binaire est C:\msys64\ucrt64\bin\gp.exe
# Copier ou configurer le chemin dans wims
```

**Test :**
```powershell
echo "2+3" | C:\msys64\ucrt64\bin\gp.exe -q
# Attendu : 5
```

#### 4.2 Maxima (valide sur ShadowPC)

```powershell
winget install MaximaTeam.Maxima
# Installe dans C:\maxima-5.49.0\
# Binaire : C:\maxima-5.49.0\bin\maxima.bat
# IMPORTANT : WIMS doit appeler sbcl.exe directement, PAS maxima.bat
#   (risque d'injection cmd.exe via .bat)
```

**Test :**
```powershell
echo "2+3;" | C:\maxima-5.49.0\bin\maxima.bat --very-quiet
# Attendu : 5
```

#### 4.3 Octave (valide sur ShadowPC)

```powershell
winget install GNU.Octave
# Installe dans C:\Program Files\GNU Octave\Octave-11.3.0\
# Binaire : ...\mingw64\bin\octave-cli.exe
```

**Test :**
```powershell
echo "1+1" | & "C:\Program Files\GNU Octave\Octave-11.3.0\mingw64\bin\octave-cli.exe" --no-gui --quiet
# Attendu : ans = 2
```

#### 4.4 GAP (non encore valide)

Pas de paquet winget. Installer depuis le site officiel :
https://www.gap-system.org/Releases/

Telecharger le .exe Windows, installer, noter le chemin vers `gap.exe`.

#### 4.5 Autres CAS (optionnels)

| CAS | Installation | Priorite |
|-----|-------------|----------|
| gnuplot | `winget install gnuplot.gnuplot` | Haute (graphiques) |
| POV-Ray | Installeur Windows | Basse |
| Scilab | `winget install Scilab.Scilab` | Basse |
| Yacas | Build from source | Tres basse |
| Macaulay2 | Builds experimentaux | Tres basse |

#### 4.6 Configuration des chemins CAS

WIMS cherche les CAS via des variables dans sa configuration.
Editer `public_html/wims.conf` (ou `other/wims.conf.local`) :

```
# Chemins des CAS (adapter selon votre installation)
pari_command=C:/msys64/ucrt64/bin/gp.exe
maxima_command=C:/maxima-5.49.0/bin/sbcl.exe
octave_command=C:/Program Files/GNU Octave/Octave-11.3.0/mingw64/bin/octave-cli.exe
gap_command=C:/Program Files/GAP/gap-4.13.1/gap.exe
gnuplot_command=C:/Program Files/gnuplot/bin/gnuplot.exe
```

**Regle de securite : toujours pointer vers le binaire `.exe` directement, jamais
vers un `.bat` ou `.cmd`** (risque d'injection via l'interpreteur cmd.exe).

---

### 5. Configuration Apache

#### 5.1 Avec Laragon

Laragon facilite la configuration. Ajouter un alias WIMS :

1. Menu Laragon → Apache → `sites-enabled/`
2. Creer `wims.conf` :

```apache
# C:\laragon\etc\apache2\sites-enabled\wims.conf
ScriptAlias /wims/wims.cgi "C:/wims/bin/wims.exe"
Alias /wims "C:/wims/public_html"

<Directory "C:/wims/bin">
    Options +ExecCGI
    SetHandler cgi-script
    Require all granted
</Directory>

<Directory "C:/wims/public_html">
    Options -Indexes +FollowSymLinks
    Require all granted
</Directory>

# Variables d'environnement pour wims.exe
SetEnv wims_home "C:/wims"
```

3. Redemarrer Apache (bouton Laragon ou `httpd -k restart`)

**Piege Laragon :** Apache sous Laragon n'est PAS un service Windows. Apres un
changement de config, il faut arreter/redemarrer via l'UI Laragon, pas `net stop`.

#### 5.2 Avec Apache Lounge (standalone)

Ajouter dans `httpd.conf` :

```apache
LoadModule cgi_module modules/mod_cgi.so

ScriptAlias /wims/wims.cgi "C:/wims/bin/wims.exe"
Alias /wims "C:/wims/public_html"

<Directory "C:/wims/bin">
    Options +ExecCGI
    SetHandler cgi-script
    Require all granted
</Directory>

<Directory "C:/wims/public_html">
    Options -Indexes +FollowSymLinks
    Require all granted
</Directory>
```

#### 5.3 Securite Apache

```apache
# Ecouter UNIQUEMENT en local (ne PAS ouvrir aux eleves avant
# que les tests de securite sandbox S1-S9 soient tous verts)
Listen 127.0.0.1:80
```

**CRITIQUE : ne jamais binder 0.0.0.0 tant que la sandbox n'est pas validee.**

---

### 6. Premier demarrage

#### 6.1 Creer les repertoires runtime

```powershell
$WIMS = "C:\wims"
New-Item -ItemType Directory -Force `
  "$WIMS\sessions", `
  "$WIMS\tmp\log\cquota", `
  "$WIMS\log\cquota", `
  "$WIMS\s2"
```

#### 6.2 Demarrer wimslogd

wimslogd doit tourner avant wims.exe. Il communique par named pipe.

```powershell
# Demarrer en arriere-plan
Start-Process -FilePath "C:\wims\bin\wimslogd.exe" `
  -WorkingDirectory "C:\wims" -WindowStyle Hidden

# Verifier qu'il tourne
Get-Process wimslogd
```

#### 6.3 Demarrer Apache

```powershell
# Laragon : cliquer "Start All"
# Apache Lounge : 
httpd -k start
```

#### 6.4 Tester en CGI direct (sans Apache)

Pour valider que wims.exe fonctionne avant de configurer Apache :

```powershell
cd C:\wims

# Simuler un appel CGI
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

Set-Location public_html
..\bin\wims.exe > ..\tmp\cgi.out 2> ..\tmp\cgi.err

# Verifier
Get-Content ..\tmp\cgi.out | Select-Object -First 5
# Attendu : "Status: 200 OK" dans les premieres lignes
```

---

### 7. Verification

#### 7.1 Smoke test navigateur

Ouvrir : http://localhost/wims/wims.cgi?module=adm/doc&lang=en

Resultat attendu : la page de documentation WIMS s'affiche.

#### 7.2 Test CAS

```
http://localhost/wims/wims.cgi?module=adm/raw&cmd=reply&job=pari&rep=2+3
```

Resultat attendu : `5`

#### 7.3 Tests platform (optionnel)

```powershell
# Depuis tests-win/ :
.\test_platform.exe
# Attendu : 22/22 passed
```

#### 7.4 Checklist

| # | Verification | Commande/URL | Attendu |
|---|-------------|--------------|---------|
| 1 | wimslogd tourne | `Get-Process wimslogd` | Process present |
| 2 | Apache tourne | `curl http://localhost/` | Apache default page |
| 3 | wims.exe repond | `curl http://localhost/wims/wims.cgi?module=adm/doc` | Status 200 |
| 4 | PARI repond | `echo "2+3" \| gp.exe -q` | 5 |
| 5 | Maxima repond | `echo "2+3;" \| maxima --very-quiet` | 5 |
| 6 | Sandbox active | `.\ch..root.exe --selftest` | Exit 0 |
| 7 | Reseau sandbox bloque | `ping` depuis sandbox | rc=1 |
| 8 | MathML | `.\test_mathml_spawn.exe` | PASS |
| 9 | crypt | `.\test_crypt.exe` | 8/8 PASS |

---

### 8. Securite

#### 8.1 Modele de sandbox

WIMS execute du code arbitraire (exercices OEF appellent des CAS). Sur Linux,
WIMS utilise chroot + setuid. Sur Windows, le portage utilise :

1. **Restricted Token** — DISABLE_MAX_PRIVILEGE + LUA_TOKEN (privileges minimaux)
2. **AppContainer** — isolation processus sans capability SIDs (pas de reseau, pas de registre, pas de clipboard)
3. **ACL** — le processus sandbox ne peut ecrire que dans son repertoire de session
4. **Job Object** — limite CPU (watchdog thread), memoire (32 Mo), nombre de processus
5. **Fail-closed** — si `ch..root.exe` ne peut pas creer le sandbox, il quitte avec exit code 2 (pas de fallback non-sandboxe)

#### 8.2 Regles imperatives

| Regle | Raison |
|-------|--------|
| **Ne PAS ouvrir Apache sur 0.0.0.0** | Sandbox pas encore 100% valide (S1-S9) |
| **Ne PAS utiliser de .bat pour les CAS** | cmd.exe est un vecteur d'injection |
| **Ne PAS desactiver AppContainer** | C'est la seule isolation reseau |
| **Garder wimslogd en local** | Named pipe, pas de socket reseau |

#### 8.3 Tests de securite restants

Les tests S1-S9 ne sont PAS tous passes. Avant d'ouvrir a des utilisateurs :

| Test | Statut | Description |
|------|:------:|-------------|
| S1 | 🔲 | Lecture fichier hors session |
| S2 | 🔲 | Ecriture fichier hors session |
| S3 | ✅ | Acces reseau (ping bloque) |
| S4 | 🔲 | Fork bomb (Job Object kill) |
| S5 | 🔲 | Injection ligne de commande |
| S6 | 🔲 | Octave system() |
| S7 | 🔲 | Octave dos/winqueryreg |
| S8 | 🔲 | Maxima :lisp |
| S9 | 🔲 | Ecriture registre HKCU |

**Tant que S1-S9 ne sont pas tous verts, WIMS ne doit servir qu'en local (127.0.0.1).**

---

### 9. Depannage

#### 9.1 "cannot_create_session_directory"

```powershell
New-Item -ItemType Directory -Force "C:\wims\sessions"
# Verifier les permissions NTFS : l'utilisateur Apache doit pouvoir ecrire
```

#### 9.2 wimslogd ne demarre pas

```powershell
# Verifier qu'un autre wimslogd ne tourne pas deja
Get-Process wimslogd -EA SilentlyContinue | Stop-Process -Force
Start-Sleep 1
Start-Process -FilePath "C:\wims\bin\wimslogd.exe" -WorkingDirectory "C:\wims"
```

#### 9.3 CAS "not found"

wims.exe cherche les CAS via les chemins dans sa config. Verifier :
- Le chemin pointe vers un `.exe`, pas un `.bat`
- Le `.exe` est accessible depuis le repertoire de travail de wims.exe
- Les DLL dependantes sont presentes (surtout pour PARI/MSYS2)

```powershell
# Tester un CAS manuellement
echo "2+3" | C:\msys64\ucrt64\bin\gp.exe -q
```

#### 9.4 Apache ne lance pas wims.exe

Verifier dans les logs Apache (`C:\laragon\logs\error.log` ou equivalent) :
- `mod_cgi` est charge
- Le `ScriptAlias` pointe vers le bon chemin
- `Options +ExecCGI` est present
- `Require all granted` autorise l'acces

#### 9.5 CPU 100% / processus zombies

Le watchdog thread de Job Object devrait tuer les processus en boucle infinie.
Si ce n'est pas le cas :

```powershell
# Lister les processus WIMS
Get-Process wims*, gp, sbcl, octave-cli -EA SilentlyContinue

# Tuer tout
Get-Process wims*, gp, sbcl, octave-cli -EA SilentlyContinue | Stop-Process -Force
```

---

### 10. Limites connues

| Limitation | Impact | Contournement |
|------------|--------|---------------|
| wimslogd : ping/loadavg seulement | Les classes ne peuvent pas noter | Phase 5 portera scores/housekeeping |
| GAP non installe | CAS algebre manquant | Installer manuellement |
| Pas de Docker/WSL2 sur ShadowPC | Pas de conteneurisation | Natif uniquement |
| MSYS2 sh.exe requis | Scripts shell (.sh) | Embarquer coreutils dans `other/bin/` |
| Sandbox S1-S9 incomplets | Pas de mise en production | Tester avant d'ouvrir |
| Wine : 18/22 vs natif 22/22 | 4 tests echouent sous wine | Non bloquant (wine != Windows reel) |

---

### Annexe A — Script d'installation rapide

```powershell
# install-wims4win.ps1
# Execute depuis le repertoire contenant tests-win/

param(
    [string]$WimsHome = "C:\wims",
    [string]$Source   = ".\tests-win"
)

$ErrorActionPreference = "Stop"

Write-Host "=== Installation Wims4Win ===" -ForegroundColor Cyan

# 1. Arborescence
$dirs = @("bin","sessions","tmp\log\cquota","log\cquota","s2")
foreach ($d in $dirs) {
    New-Item -ItemType Directory -Force "$WimsHome\$d" | Out-Null
}
Write-Host "[OK] Arborescence creee"

# 2. Binaires
Copy-Item "$Source\wims-home\bin\wims.exe"      "$WimsHome\bin\" -Force
Copy-Item "$Source\wims-home\bin\wimslogd.exe"  "$WimsHome\bin\" -Force
Copy-Item "$Source\wims_mathml.exe"             "$WimsHome\bin\" -Force
Copy-Item "$Source\ch..root.exe"                "$WimsHome\bin\" -Force
foreach ($cas in @("pari","maxima","gap","octave","m2","scilab","yacas")) {
    if (Test-Path "$Source\$cas.exe") {
        Copy-Item "$Source\$cas.exe" "$WimsHome\bin\" -Force
    }
}
Write-Host "[OK] Binaires copies"

# 3. public_html
if (Test-Path "$Source\wims-home\public_html") {
    Copy-Item "$Source\wims-home\public_html" "$WimsHome\public_html" -Recurse -Force
    Write-Host "[OK] public_html copie"
} else {
    Write-Host "[WARN] public_html non trouve dans $Source\wims-home\" -ForegroundColor Yellow
}

# 4. Verification
Write-Host ""
Write-Host "=== Verification ===" -ForegroundColor Cyan
$bins = @("wims.exe","wimslogd.exe","wims_mathml.exe","ch..root.exe")
foreach ($b in $bins) {
    if (Test-Path "$WimsHome\bin\$b") {
        Write-Host "[OK] $b" -ForegroundColor Green
    } else {
        Write-Host "[MISSING] $b" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Installation terminee dans $WimsHome" -ForegroundColor Cyan
Write-Host "Prochaines etapes :"
Write-Host "  1. Installer les CAS (PARI, Maxima, Octave)"
Write-Host "  2. Configurer Apache (voir section 5 du guide)"
Write-Host "  3. Demarrer wimslogd puis Apache"
Write-Host "  4. Tester : http://localhost/wims/wims.cgi?module=adm/doc"
```

---

### Annexe B — Matrice de compatibilite

| Composant | Source | Build | Status |
|-----------|--------|-------|:------:|
| wims.exe | wims-port/src/ (37 .c) | mingw-w64 static | ✅ CGI 200 |
| wimslogd.exe | wims-port/src/Wimslogd/win/ | mingw-w64 static | ✅ Ping OK |
| ch..root.exe | wims-port/src/Platform/sandbox/ | mingw-w64 static | ✅ AppContainer |
| wims_mathml.exe | wims-port/src/Mathml/ | mingw-g++ + flex/bison | ✅ MathML |
| 7 interfaces CAS | wims-port/src/Interfaces/ | mingw-w64 static | ✅ |
| public_html | SVN r19040 | Copie directe | ✅ |
| PARI 2.19 | MSYS2 ucrt64 | Pre-compile | ✅ PC |
| Maxima 5.49 | winget | Pre-compile | ✅ PC |
| Octave 11.3 | winget | Pre-compile | ✅ PC |
| GAP 4.13 | Installeur officiel | Pre-compile | 🔲 |

---

### Annexe C — Glossaire

| Terme | Definition |
|-------|-----------|
| **CAS** | Computer Algebra System (PARI, Maxima, Octave, GAP...) |
| **CGI** | Common Gateway Interface — wims.exe est appele par Apache pour chaque requete |
| **OEF** | Online Exercise Format — format des exercices WIMS |
| **AppContainer** | Mecanisme d'isolation Windows (equivalent leger de chroot) |
| **Job Object** | Groupe de processus Windows avec limites CPU/memoire/kill-on-close |
| **Named pipe** | IPC Windows (`\\.\pipe\wims-*`) pour wimslogd |
| **wimslogd** | Daemon qui gere le log, les sessions, les scores WIMS |
| **ch..root.exe** | Le sandbox Windows (nom original Linux : ch..root) |


---

## Partie B — Guide CGI Laragon (Claude V1_1)

> Source : `archive/Guide_CGI_Laragon_WIMS_CLAUDE_V1_1.md`

## Guide : WIMS CGI sous Laragon Apache (Windows)

### Résumé

WIMS fonctionne en mode CGI sous Laragon Apache 2.4.54 sur ShadowPC
(Windows 11). Deux runtime trees testés :

| Runtime | Source | Binaire | Modules | Status |
|---------|--------|---------|---------|--------|
| Debian .deb extract | `wims_4.31b+dfsg1-5_amd64.deb` | wims.exe 522 KB (notre build) | Core only (4766 fichiers) | ✅ Home 200 |
| **Grok wims-runtime** | Compilé depuis wims-4.32.tgz sur Debian | wims.cgi.exe 929 KB (fixes inclus) | Complet (5173 fichiers) | **✅ Home + modules 200** |

**Configuration active** : Grok's `wims-runtime` (commit `d820b2d`).

### Changements V1 → V1_1

- Intégré les 4 nouveaux commits Grok (`f999023..d820b2d`) :
  - Fix NULL `SERVER_ADDR` (`log.c:521`) + CGI prepare guard (`wp_cgi_win32.c`)
  - Per-tree wimslogd pipe name (plus de collision entre instances)
  - wimslogd ne bloque plus la sortie CGI (handles non hérités)
  - Build script `build-runtime-tree.sh` + Apache config template
- VHost Laragon pointe vers `wims-runtime/public_html` (Grok)
- Modules `adm/doc`, `adm/class/classes` fonctionnent (200 OK)

### Architecture de l'arbre runtime (Grok)

```
D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime\
├── public_html\
│   ├── wims.cgi.exe      ← CGI binaire (929 KB, avec fixes)
│   ├── wims.exe           ← idem (ScriptAlias pointe sur .cgi.exe)
│   ├── html\              ← 435 fichiers (templates complets)
│   ├── msg\               ← 37 fichiers langue
│   ├── modules\           ← 5173 fichiers (core + admin)
│   ├── scripts\           ← 10018 fichiers
│   ├── gifs\              ← 5818 images
│   ├── themes\            ← 304 fichiers
│   ├── bases\             ← 2365 fichiers
│   └── bin\               ← scripts internes WIMS
├── bin\
│   └── wimslogd.exe       ← daemon IPC (257 KB)
├── log\                   ← wims.conf, .wimspass
├── sessions\              ← sessions utilisateur
├── s2\                    ← WIMS secondaire
└── tmp\                   ← tmp/log pour wimslogd
```

### Configuration Apache (Laragon)

Fichier : `C:\laragon\etc\apache2\sites-enabled\auto.wims_paris_saclay_fr.test.conf`

```apache
Define WIMS_RT "D:/ALF_ZGit/43-wims-paris-saclay_fr/Reprise_Wims_Grok/wims-runtime/public_html"
ScriptAliasMatch "^/wims/wims\.cgi$" "${WIMS_RT}/wims.cgi.exe"
Alias /wims "${WIMS_RT}"
```

Voir aussi `docs/apache-laragon-wims-runtime.conf` dans le repo wims4win.

### Bugs corrigés (Grok commits f999023 + 9c3628d)

#### 1. NULL dereference `log.c:521`
- `getenv("SERVER_ADDR")` peut retourner NULL → segfault 0xC0000005
- Fix : `#ifdef _WIN32` guard + `wp_cgi_prepare()` pré-remplit la variable

#### 2. Collision wimslogd instances
- Le pipe name était global → tous les wims.exe du même PC partageaient un seul wimslogd
- Fix : pipe name dérivé du chemin de l'arbre runtime

#### 3. wimslogd bloquait la sortie CGI
- `wp_daemon_spawn()` passait les handles au processus enfant → Apache attendait indéfiniment
- Fix : `bInheritHandles=FALSE` dans `CreateProcess`

### Tests validés

| Test | Runtime Debian | Runtime Grok |
|------|:-:|:-:|
| `wims.exe version` → 4.33a | ✅ | ✅ |
| CGI `module=home` (CLI) | ✅ 2593 chars | ✅ 26492 chars |
| CGI `module=home` (Apache) | ✅ | ✅ |
| `module=adm/doc` | ❌ wrong_module | ✅ 14474 chars |
| `module=adm/class/classes` | ❌ | ✅ 15239 chars |
| Assets CSS | ✅ | ✅ |
| Session IDs | ✅ | ✅ |

### Limitations actuelles

1. **`module=help`** → 450 (module incomplet dans cette build)
2. **`module=adm/manage`** → 550 (nécessite authentification)
3. **Modules exercices OEF** → nécessitent `modupdate` pour installer
4. **Audio Shtooka** → absent (site de téléchargement indisponible)
5. **Mot de passe webmaster** → vide (`log/.wimspass` à définir)
6. **Version mismatch** → runtime 4.32, binaire 4.33a (trunk)

### Prochaines étapes

1. Installer des modules exercices via `modupdate`
2. Tester un exercice OEF dans le navigateur
3. Définir le mot de passe webmaster
4. Tester les CAS via un exercice interactif
5. Valider la version croisée VPS ↔ Windows
