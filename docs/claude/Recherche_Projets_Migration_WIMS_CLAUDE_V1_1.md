# CLAUDE_V1_1
# Recherche — Projets open source pour migrer WIMS sur Windows

**Date:** 2026-10-23 (rev 2026-10-23)  
**Objectif:** Trouver des projets qui facilitent le portage de WIMS (le vrai serveur) sur Windows sans WSL.

---

## ⚠️ Contrainte environnement

**ShadowPC** = PC cloud (gaming). Pas d'accès hyperviseur.

| Technologie | Disponible ? | Raison |
|-------------|-------------|--------|
| Hyper-V | ❌ | Virtualisation imbriquée impossible |
| WSL2 | ❌ | Nécessite Hyper-V |
| Docker Desktop | ❌ | Nécessite WSL2 ou Hyper-V |
| Podman Desktop | ❌ | Nécessite WSL2 ou Hyper-V |
| MSYS2 / Cygwin | ✅ | User-space, pas de virtualisation |
| Midipix | ✅ | User-space, ntdll |
| MinGW natif | ✅ | User-space |
| Laragon | ✅ | Apache/PHP/MySQL natif Windows |

---

## 0. Résumé exécutif (mis à jour ShadowPC)

| Piste | Projet | Effort | Résultat attendu | ShadowPC |
|-------|--------|--------|-----------------|----------|
| ~~A. Docker Desktop~~ | ~~docker-wims~~ | ~~1 jour~~ | ~~WIMS complet~~ | ❌ Impossible |
| ~~B. Podman Desktop~~ | ~~docker-wims + Podman~~ | ~~1 jour~~ | ~~Idem~~ | ❌ Impossible |
| 🏆 **C. MSYS2 Cygwin** | MSYS2 msys2 runtime | **2-4 semaines** | WIMS compilé (avec msys-2.0.dll) | ✅ |
| 🥈 **D. Cygwin natif** | cygwin.com | **2-4 semaines** | WIMS compilé (avec cygwin1.dll) | ✅ |
| 🥉 **E. Midipix** | midipix.org | **1-2 mois** | fork() natif Win32 | ✅ mais risqué |
| 4. **F. Portage MSVC** | comfork + win32compat | **3-6 mois** | Binaire 100% natif | ✅ |
| 5. **G. Laragon + CGI** | laragon.org | **1-2 semaines** | Apache natif + wims.exe (MSYS2) | ✅ |

**Recommandation : Piste C (MSYS2) + G (Laragon pour Apache).**

---

## 1. ~~Piste A — Docker Desktop~~ ❌ ÉLIMINÉE

ShadowPC n'a pas Hyper-V. Docker Desktop et Podman Desktop sont impossibles.

Le Dockerfile de [amato-gianluca/docker-wims](https://github.com/amato-gianluca/docker-wims) reste une **référence précieuse** pour la liste des dépendances :

```
apache2, make, g++, texlive-base, texlive-latex-base, gnuplot, pari-gp,
units-filter, flex, bison, perl, liburi-perl, imagemagick, libgd-dev,
libfl-dev, wget, curl, yacas, gap, maxima, maxima-share, octave,
octave-statistics, graphviz, ldap-utils, scilab-cli, qrencode,
fortune, unzip, zip, libgmp-dev, openbabel, povray, macaulay2, bc,
chemeq, ssmtp, bsd-mailx, patch, locales, locales-all
```

---

## 2. 🏆 Piste C — MSYS2 mode msys2 (RECOMMANDÉE)

### Le projet
- **Site :** [msys2.org](https://www.msys2.org/)
- **Installer :** `winget install MSYS2.MSYS2`
- **Mode requis :** Terminal **MSYS2 MSYS** (pas MINGW64 — MinGW n'a pas fork)

### Pourquoi MSYS2 et pas Cygwin directement ?

| Critère | MSYS2 | Cygwin |
|---------|-------|--------|
| Package manager | `pacman` (Arch-like, rapide) | `setup.exe` (GUI lent) |
| Packages à jour | ✅ Rolling release | 🟡 Souvent en retard |
| libgd | `pacman -S libgd-devel` | `apt-cyg` ou setup.exe |
| Maxima | ❌ Pas dans pacman → installer Windows natif | ❌ Idem |
| fork() | ✅ Via msys-2.0.dll (même code que Cygwin) | ✅ Via cygwin1.dll |
| Interop Windows | ✅ Bonne (lance les .exe Windows) | 🟡 Correcte |

### Plan d'installation

```bash
# 1. Installer MSYS2
winget install MSYS2.MSYS2

# 2. Ouvrir "MSYS2 MSYS" (PAS MINGW64 !)

# 3. Installer les dépendances de compilation
pacman -Syu
pacman -S base-devel gcc make flex bison
pacman -S libgd-devel perl imagemagick
pacman -S texlive-core wget curl unzip zip
pacman -S libcrypt-devel

# 4. Compiler WIMS
cd /d/ALF_ZGit/43-wims-paris-saclay_fr/Reprise_Wims_Claude/wims
./configure
make all

# 5. Compiler avec MathJax, modules, etc.
yes "" | ./compile --mathjax --jmol --modules --geogebra
```

### Problèmes attendus et solutions

| Problème | Solution |
|----------|----------|
| `fork()` lent (~100× Linux) | Acceptable pour usage local. Pas pour production. |
| `chroot` absent | Commenter dans `common.c` ou `#ifdef _WIN32` → skip |
| Chemins `/tmp`, `/bin/sh` | MSYS2 mappe `/tmp` → `C:\msys64\tmp`, `/bin/sh` → `C:\msys64\usr\bin\sh.exe` |
| Apache | ❌ Ne pas utiliser Apache MSYS2 → utiliser **Laragon** (piste G) |
| Maxima | Installer Windows natif + créer wrapper sh dans MSYS2 PATH |
| PARI/GP | Installer Windows natif (`gp.exe`) + wrapper |
| Octave | Installer Windows natif + wrapper |
| GAP | Installer Windows natif + wrapper |
| `setreuid/setregid` | `#ifdef __MSYS__` → noop (pas de séparation uid sur Windows) |
| `/proc/net/tcp` | `#ifdef __MSYS__` → skip `manager_check` |

### CAS Windows natifs à installer séparément

| CAS | Installation Windows | Wrapper MSYS2 |
|-----|---------------------|--------------|
| Maxima | `winget install Maxima.Maxima` | `#!/bin/sh` → `"/c/Program Files/Maxima/bin/maxima.bat" "$@"` |
| PARI/GP | `winget install PARI.PARI` | `#!/bin/sh` → `"/c/Program Files/PARI/gp.exe" "$@"` |
| Octave | `winget install GNU.Octave` | `#!/bin/sh` → `"/c/Program Files/GNU Octave/bin/octave-cli.exe" "$@"` |
| GAP | Télécharger depuis gap-system.org | `#!/bin/sh` → `"/c/gap/bin/gap.exe" "$@"` |
| gnuplot | `winget install gnuplot.gnuplot` | Déjà dans PATH |
| ImageMagick | `winget install ImageMagick.ImageMagick` | Déjà dans PATH |

### Effort estimé : 2-4 semaines

- Semaine 1 : `./configure && make` sous MSYS2, corriger les erreurs de compilation
- Semaine 2 : Adapter `common.c` (skip chroot, setreuid, /proc)
- Semaine 3 : Installer CAS natifs + wrappers, tester les exercices
- Semaine 4 : Configurer Apache Laragon comme frontend CGI

---

## 3. 🥈 Piste D — Cygwin natif

### Le projet
- **Site :** [cygwin.com](https://cygwin.com/)
- **Installer :** `setup-x86_64.exe`

Même approche que MSYS2 mais avec le package manager Cygwin (plus lourd). MSYS2 est préféré car pacman est plus rapide et les packages sont plus à jour.

**Utiliser Cygwin SI** un package dont WIMS a besoin est dans Cygwin mais pas dans MSYS2 pacman.

---

## 4. 🥉 Piste E — Midipix (fork natif, expérimental)

### Le projet
- **Site :** [midipix.org](https://midipix.org)
- **Ce que c'est :** Couche POSIX utilisant Windows Native API (ntdll)
- **Killer feature :** `fork()` copy-on-write natif, pas d'émulation Cygwin

### Avantages
- ✅ fork() réel et rapide
- ✅ Pas de cygwin1.dll / msys-2.0.dll
- ✅ musl libc (léger)

### Inconvénients
- ❌ **Projet peu maintenu** (dernières docs 2016-2020)
- ❌ Pas de packages pré-compilés
- ❌ Cross-compilation nécessaire pour builder le toolchain
- ❌ API ntdll non documentée = risque de casse entre mises à jour Windows
- ❌ Pas de communauté active

### Verdict
Intéressant conceptuellement. **Trop risqué pour un projet concret.** À surveiller si le projet reprend vie.

---

## 5. Piste F — Portage natif MSVC (long terme)

### Les projets utiles
- **[comfork](https://github.com/jonnysoe/comfork)** : fork() via RtlCreateUserProcess (ntdll)
- **[winflexbison](https://github.com/lexxmark/winflexbison)** : flex/bison pour MSVC
- **[vcpkg](https://github.com/microsoft/vcpkg)** : libgd, OpenSSL, etc. pour MSVC

### Ce qu'il faut écrire
1. `win32compat.c` — CreateProcess, CreatePipe, DuplicateHandle, Job Objects, timer threads
2. `CMakeLists.txt` — remplace autotools
3. 36 scripts shell → PowerShell
4. Supprimer chroot → AppContainer ou rien

### Effort : 3-6 mois
Uniquement si l'objectif est une distribution Windows officielle de WIMS.

---

## 6. 🏆 Piste G — Laragon comme serveur web (combinaison avec C)

### Le projet
- **Site :** [laragon.org](https://laragon.org/)
- **Ce que c'est :** Apache + PHP + MySQL natif Windows, pas de virtualisation
- **Déjà installé** sur ce ShadowPC (utilisé pour Drupal/Joomla dans le projet Reprise)

### Architecture visée

```
                    ┌─────────────────────────┐
   Browser ──►     │  Laragon Apache (natif)  │  port 80/443
                    │  mod_cgi activé          │
                    └─────────┬───────────────┘
                              │ CGI
                    ┌─────────▼───────────────┐
                    │  wims.exe (MSYS2 build) │  compilé avec msys-2.0.dll
                    │  fork/exec via MSYS2    │
                    └─────────┬───────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
     ┌────────▼──────┐  ┌────▼────┐  ┌───────▼──────┐
     │ flydraw.exe   │  │ texgif  │  │ Maxima.exe   │
     │ (MSYS2+libgd) │  │ (MSYS2) │  │ (natif Win)  │
     └───────────────┘  └─────────┘  └──────────────┘
```

### Configuration Apache (Laragon)

```apache
# Dans httpd.conf ou extra/httpd-vhosts.conf
ScriptAlias /wims/ "D:/msys64/home/wims/public_html/"
<Directory "D:/msys64/home/wims/public_html">
    Options +ExecCGI
    AddHandler cgi-script .cgi
    AllowOverride None
    Require all granted
</Directory>
```

### Avantages
- ✅ Apache natif Windows (pas d'émulation)
- ✅ Laragon déjà installé et configuré
- ✅ Rechargement UI facile (pas un service)
- ✅ wims.exe (MSYS2) appelé en CGI = fork fonctionne

### Effort additionnel : 1-2 jours (au-dessus de la piste C)

---

## 7. Projets WIMS connexes (référence)

| Projet | Repo | Utilité pour le portage |
|--------|------|----------------------|
| **docker-wims** | [amato-gianluca](https://github.com/amato-gianluca/docker-wims) | **Liste des dépendances** (Dockerfile = spécification) |
| **docker-wims-minimal** | [qcoumes](https://github.com/qcoumes/docker-wims-minimal) | Version minimale pour tests |
| **Ansible-WIMS** | [stephanemouton](https://github.com/stephanemouton/Ansible-WIMS) | Scripts d'install Debian (à adapter) |
| **wims-lti** | [amato-gianluca](https://github.com/amato-gianluca/wims-lti) | Passerelle LTI (Moodle ↔ WIMS) |
| **moodle-mod_wims** | [suipnice](https://github.com/suipnice/moodle-mod_wims) | Plugin Moodle |
| **WIMS-modules** | [amato-gianluca](https://github.com/amato-gianluca/WIMS-modules) | Internationalisation modules |
| **wims (Ubuntu)** | [Launchpad](https://launchpad.net/ubuntu/+source/wims) | Package Ubuntu 4.07a |

## 8. Outils de portage

| Outil | Rôle | Installation |
|-------|------|-------------|
| **MSYS2** | Compilation C avec fork() | `winget install MSYS2.MSYS2` |
| **Laragon** | Apache natif + CGI | Déjà installé |
| **vcpkg** | libgd, OpenSSL pour MSVC | `winget install Microsoft.Vcpkg` |
| **winflexbison** | flex/bison natifs (chemeq) | `pacman -S flex bison` (MSYS2) |
| **Strawberry Perl** | Scripts Perl WIMS | `winget install StrawberryPerl.StrawberryPerl` |
| **MiKTeX** | LaTeX (texgif) | `winget install MiKTeX.MiKTeX` |
| **SlikSVN** | SVN CLI | Déjà installé (`C:\Program Files\SlikSvn\bin\svn.exe`) |
| **Maxima** | CAS symbolique | `winget install Maxima.Maxima` |
| **PARI/GP** | CAS théorie des nombres | Installeur Windows |
| **GNU Octave** | CAS matriciel | `winget install GNU.Octave` |
| **gnuplot** | Graphiques | `winget install gnuplot.gnuplot` |
| **ImageMagick** | Images | `winget install ImageMagick.ImageMagick` |

---

## 9. Plan d'action recommandé (ShadowPC)

| Étape | Action | Effort | Pré-requis |
|-------|--------|--------|-----------|
| **1** | Installer MSYS2 | 10 min | — |
| **2** | `pacman -S` dépendances de build | 15 min | Étape 1 |
| **3** | `./configure && make` dans MSYS2 MSYS | 1-3 jours | Étapes 1-2 |
| **4** | Patcher `common.c` (skip chroot, setreuid, /proc) | 1 jour | Étape 3 |
| **5** | Installer CAS natifs Windows + wrappers MSYS2 | 1 jour | — |
| **6** | Configurer Laragon Apache + CGI | 1 jour | Étapes 3-4 |
| **7** | Tester exercices OEF (compilation + exécution) | 2-3 jours | Étapes 5-6 |
| **8** | Corriger les scripts shell critiques | 3-5 jours | Étape 7 |
| **Total** | | **~2-3 semaines** | |

---

## 10. Non vérifié

- Compilation réelle WIMS sous MSYS2 (jamais testée par personne à notre connaissance)
- Compatibilité Laragon CGI + binaire MSYS2
- Performance de fork() MSYS2 sous ShadowPC (overhead réseau + fork lent)
- Que MSYS2 pacman ait tous les -devel nécessaires (libgd, libfl, etc.)
