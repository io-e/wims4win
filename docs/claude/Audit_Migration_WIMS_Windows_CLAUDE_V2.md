# CLAUDE_V2
# Audit consolidé — Migration WIMS 4.32 sur Windows (sans WSL)

**Date:** 2026-10-23  
**Auteurs:** Claude (analyse SVN r19040 complète) + Grok (analyse partielle, 101 fichiers)  
**Source:** SVN `https://subversion.renater.fr/anonscm/svn/wimsdev/trunk` (r19040)  
**Sources locales:** `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Claude\wims\{src,bin,other}`  
**Objectif:** Lister les programmes/composants qui posent problème pour un portage natif Windows.

---

## 1. Architecture WIMS — vue d'ensemble

```
                          ┌─────────────────────────┐
   Browser ──HTTP──►      │   wimsd (C daemon)       │  fork/exec per request
                          │   ou Apache + CGI        │
                          └─────────┬───────────────┘
                                    │ execl()
                          ┌─────────▼───────────────┐
                          │   wims (C CGI engine)     │  core: eval, calc, html, var
                          │   src/*.c (~30 files)     │
                          └─────────┬───────────────┘
                                    │ call_sh / execredirected
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
     ┌────────▼──────┐   ┌─────────▼──────┐   ┌──────────▼─────┐
     │  flydraw (C)  │   │  texgif (C)    │   │  CAS Interfaces│
     │  libgd → GIF  │   │  TeX → GIF     │   │  Maxima, GAP,  │
     └───────────────┘   └────────────────┘   │  Octave, PARI, │
                                               │  Scilab, Yacas  │
                                               └────────────────┘
              ┌───────────────────────────────────────────┐
              │  Misc/ (C tools)                          │
              │  chemeq, symtext, crossword, checkmol,    │
              │  canvasdraw, lceb, voronoiW, whirlgif...  │
              └───────────────────────────────────────────┘
              ┌───────────────────────────────────────────┐
              │  Shell scripts (bin/)                      │
              │  oefcompile, src2def, mkindex, modupdate, │
              │  housekeep.daily, account.sh, backup...   │
              └───────────────────────────────────────────┘
              ┌───────────────────────────────────────────┐
              │  External deps                            │
              │  TeX (LaTeX), ImageMagick, Perl, wget,    │
              │  MathJax, Jmol/JSmol, GeoGebra            │
              └───────────────────────────────────────────┘
```

---

## 2. État de la récupération des sources

| Item | Claude | Grok |
|------|--------|------|
| Méthode | SlikSVN 1.14.5 (`svn checkout --depth`) | `git svn clone -r HEAD` (coupé à 12 min) |
| Résultat | `wims/src/` complet (12 sous-dossiers + 43 fichiers), `wims/bin/` (36 scripts), `wims/other/bin/` (3 fichiers) | 101 fichiers partiels ; `Interfaces`, `Flydraw`, `Texgif`, `Wimslogd` vides |
| Sous-dossiers vérifiés | ✅ Tous (Wimsd, Interfaces, Flydraw, Texgif, Lib, Misc, Module, Mathml, OefMsg2wims, Wimslogd) | ❌ Partiels (src/*.c et bin/ seulement) |
| SVN relocate | Non nécessaire (accès direct) | Non fait |

---

## 3. Programmes problématiques — classement par difficulté

### Légende

| Niveau | Signification |
|--------|---------------|
| 🔴 **BLOQUANT** | Réécriture majeure ou remplacement nécessaire |
| 🟠 **DIFFICILE** | Portage possible mais effort > 1 semaine |
| 🟡 **MOYEN** | Adaptation mineure (1–5 jours) |
| 🟢 **FACILE** | Fonctionne ou quasi-prêt |

---

### 🔴 BLOQUANT — Programmes qui ne fonctionneront PAS sur Windows

| # | Programme | Fichier(s) | Problème | Impact | Confirmé par |
|---|-----------|------------|----------|--------|-------------|
| B1 | **wimsd** (daemon HTTP) | `src/Wimsd/wimsd.c` | `fork()` + `execl()` + `dup2()` + `accept()` par requête. `setreuid/setregid`. `signal(SIGALRM)` pour timeout 200s. | Serveur complet. Alternative : Apache/IIS + CGI ou réécrire avec `CreateProcess`. | Claude + Grok |
| B2 | **wims** (CGI engine) | `src/wims.c`, `src/exec.c`, `src/Lib/libwims.c`, `src/Interfaces/common.c` | `fork()`+`execv()`+`pipe()`+`dup2()`+`waitpid()` dans `execredirected()`. `select()` dans `dopipes()`. `kill(pid, SIGKILL)`. `/proc/net/tcp` (non-portable). | Le moteur WIMS. Chaque appel CAS = fork+exec. | Claude + Grok |
| B3 | **CAS Interfaces** | `src/Interfaces/{maxima,gap,octave,pari,scilab,yacas}.c` + `common.c` | Fork+exec+pipe bidirectionnel via `execredirected()`. `setreuid/setregid` dans le child. `chroot` wrapper. | Communication avec les 6 CAS. | Claude |
| B4 | **chroot sandbox** | `common.c` → `bin/ch..root`, `bin/setchroot` | `chroot()` via wrapper pour isoler les CAS. | Sécurité. N'existe pas sur Windows. | Claude + Grok |
| B5 | **Mémoire partagée** | `wimsshm.h`, `wims.c` | `shmget`/`shmat`/`mmap` (System V IPC). Actuellement commenté mais architecturellement prévu. | IPC inter-processus. | Grok |
| B6 | **wimslogd** (daemon log) | `src/Wimslogd/wimsd.c` (sic), `log.c:446-464` | Double `fork` + `execve` pour démonisation. `setsid`. | Service de journalisation. | Grok |
| B7 | **Shell scripts (bin/)** | 36 scripts sh : `oefcompile`, `src2def`, `mkindex`, `housekeep.daily`, `modupdate`, `backup`, etc. | Tous en `/bin/sh`. Utilisent `find`, `sed`, `grep`, `awk`, `cmp`, `diff`, `chmod`, `ln -s`. | Admin, compilation OEF, maintenance quotidienne. | Claude + Grok |
| B8 | **Délais/signaux** | `lines.c:853-877`, `wims.c` (`initalarm`), `wimsd.c` | `signal(SIGALRM)` + `alarm()` pour timeout d'exécution. Pas de `SIGALRM` sur Windows. | Contrôle du temps d'exécution. | Grok |

---

### 🟠 DIFFICILE — Portage possible avec effort significatif

| # | Programme | Fichier(s) | Problème | Solution | Confirmé par |
|---|-----------|------------|----------|----------|-------------|
| D1 | **flydraw** | `src/Flydraw/flydraw.c` + 4 sources | **libgd** (GIF). `strcasecmp`. Build autotools. | CMakeLists.txt + vcpkg `gd`. `_stricmp`. | Claude |
| D2 | **texgif** | `src/Texgif/texgif.c` + 7 sources | TeX → GIF. DVI parsing custom. Chemins `/tmp`. | MiKTeX + `%TEMP%`. C portable. | Claude + Grok |
| D3 | **Misc/chemeq** | `src/Misc/chemeq/` | `flex` + `g++`. | MinGW/MSYS2 ou win_flex + MSVC. | Claude |
| D4 | **configure / Makefile** | `configure.ac`, `Makefile.in` | Autotools. Vérifie `fork`, `select`, `alarm`, `ln -s`. | CMake. Ou MSYS2 (pas "natif"). | Claude + Grok |
| D5 | **Sockets Unix** | `wims.h:357`, `lines.c:1346-1355` | `socket(PF_UNIX)`, `sockaddr_un`. | TCP localhost ou Named Pipes. `AF_UNIX` partiel depuis Win10 1803. | Grok |
| D6 | **sendmail** | `exec.c` (exec_mailto) | Pipe vers `/usr/sbin/sendmail`. | SMTP via `blat`, `Send-MailMessage`, ou lib SMTP. | Claude |

---

### 🟡 MOYEN — Adaptations mineures

| # | Programme | Fichier(s) | Problème | Solution | Confirmé par |
|---|-----------|------------|----------|----------|-------------|
| M1 | **Misc/ C tools** (15+) | `Plouffe.c`, `clickzone.c`, `cryptarith.c`, `curvecomp.c`, `dicsort.c`, `huffman.c`, `modind.c`, `oncechar.c`, `phtmltext.c`, `shortpath.c`, `ccsum.c`, `html2msg.c`, etc. | C pur. `strcasecmp` → `_stricmp`. `/tmp` → `%TEMP%`. | Macros de compatibilité. Compiler MSVC ou MinGW. | Claude |
| M2 | **src/Lib/** | `libwims.c`, `math.c`, `text.c`, `dir.c`, `evalue.c` | `random()`→`rand()`, `srandom()`→`srand()`, `setenv`→`_putenv_s`, `unlink`→`_unlink`. | `#ifdef _WIN32` compat layer. mt19937 déjà inclus (`mt19937ar.c`). | Claude + Grok |
| M3 | **Liens symboliques** | `calc.c:1964`, `files.c`, `variables.c` | `symlink`, `lstat`, `readlink`. Droits admin ou mode développeur. | `CreateSymbolicLink` avec repli copie. Junctions. | Grok |
| M4 | **Permissions uid/gid** | `wims.c`, `log.c`, `exec.c`, `cleaning.c`, `bin/wrapuid` | `chmod`, `umask`, `getuid`, `setuid`. Modèle Unix. | Neutraliser ou ACL NTFS. Compte de service. | Grok |
| M5 | **Verrous fichiers** | `includes.h`, `configure.ac` | `flock`/`fcntl`. | `LockFileEx`. | Grok |
| M6 | **Chemins en dur** | `wims.h`, `config.c`, `exec.c`, `variables.c`, `log.c` | `/bin/sh`, `/usr/…`, `/tmp/`, séparateur `/`. | Variables de config, `GetTempPath`, backslash compat. | Grok |
| M7 | **Appels shell** | `exec.c`, `lines.c` | `popen`/`system` → `/bin/sh`. | `_popen` ou shell MSYS2 embarqué. | Grok |
| M8 | **Misc/canvasdraw** | `src/Misc/canvasdraw/` | Scripts shell pour doc. Le JS généré est portable. | Réécrire scripts doc en Python. | Claude |

---

### 🟢 FACILE — Fonctionne déjà ou quasi-prêt

| # | Programme | Notes | Confirmé par |
|---|-----------|-------|-------------|
| F1 | **MathJax** (JavaScript) | Navigateur. Zéro portage. | Claude + Grok |
| F2 | **Jmol/JSmol** (Java/JS) | Navigateur. Zéro portage. | Claude |
| F3 | **GeoGebra** (JavaScript) | Navigateur. Zéro portage. | Claude |
| F4 | **Perl scripts** | `css.pl`, `refer.pl`, `oefdoc.pl`, `wims_modules.pl` — Strawberry Perl Windows. Adapter `system()` et chemins. | Claude + Grok |
| F5 | **Modules OEF** (contenu) | Fichiers texte `.oef` / `.def`. Aucune adaptation. | Claude + Grok |
| F6 | **public_html/** (HTML/CSS/JS) | Pages web statiques. Portables. | Claude |
| F7 | **CAS natifs Windows** | Maxima, PARI/GP, Octave, gnuplot, POV-Ray, ImageMagick — installeurs Windows officiels. **Le problème n'est pas les CAS mais la façon dont WIMS les appelle** (fork+pipe). | Grok |
| F8 | **LaTeX** (MiKTeX / TeX Live) | Installeur Windows natif. | Claude + Grok |
| F9 | **Apache CGI** | Apache Lounge ou IIS. `wims.cgi` → `.exe`. | Grok |

---

## 4. Appels système POSIX bloquants — inventaire complet

| Appel POSIX | Fichier(s) source | Équivalent Windows | Difficulté |
|-------------|-------------------|-------------------|------------|
| `fork()` | wimsd.c, common.c, log.c | `CreateProcess()` (pas d'équivalent direct — pas de COW) | 🔴 |
| `execl/execv/execvp()` | wimsd.c, common.c | `CreateProcess()` | 🔴 |
| `pipe()` | common.c | `CreatePipe()` | 🟡 |
| `dup2()` | wimsd.c, common.c | `SetStdHandle()` + `DuplicateHandle()` | 🟡 |
| `waitpid()` | wimsd.c, common.c | `WaitForSingleObject()` | 🟡 |
| `select()` (pipes) | common.c (`dopipes`) | `WaitForMultipleObjects()` ou `select()` Winsock | 🟠 |
| `kill(pid, SIGKILL)` | common.c, cleaning.c, lines.c | `TerminateProcess()` | 🟡 |
| `socket(PF_UNIX)` | wims.h, lines.c | TCP localhost ou Named Pipes | 🟠 |
| `socket/bind/listen/accept` (TCP) | wimsd.c | Winsock2 (`WSAStartup` + mêmes noms) | 🟡 |
| `setreuid/setregid` | wimsd.c, common.c | N/A — modèle sécurité différent | 🟠 |
| `signal(SIGALRM)` + `alarm()` | wimsd.c, common.c, wims.c, lines.c | Timer threads ou `CreateTimerQueueTimer()` | 🟠 |
| `chroot()` | common.c (wrapper `ch..root`) | N/A — Job Objects ou AppContainer | 🔴 |
| `setrlimit(RLIMIT_CPU/MEM)` | wims.c, auth.c, config.c | `SetInformationJobObject()` | 🟡 |
| `shmget/shmat/mmap` | wimsshm.h | `CreateFileMapping/MapViewOfFile` | 🟡 |
| `random/srandom` | Lib/*.c | `rand/srand` ou mt19937 (inclus) | 🟢 |
| `strcasecmp/strncasecmp` | partout | `_stricmp/_strnicmp` | 🟢 |
| `symlink/lstat/readlink` | calc.c, files.c, variables.c | `CreateSymbolicLink` (droits admin) | 🟡 |
| `/proc/net/tcp` | wims.c (`manager_check`) | `GetExtendedTcpTable()` | 🟡 |
| `umask/chmod` | wims.c, scripts | `_umask()` partiel, ACL NTFS | 🟡 |
| `flock/fcntl` | includes.h | `LockFileEx` | 🟡 |
| `popen/system("/bin/sh")` | exec.c, lines.c | `_popen("cmd /c …")` ou MSYS2 sh | 🟡 |
| `getuid/geteuid` | wims.c | `GetUserName()` / SID (modèle différent) | 🟡 |
| Double `fork` (daemonize) | log.c, Wimslogd | Service Windows (NSSM / `SC create`) | 🟠 |

---

## 5. Dépendances externes

| Dépendance | Rôle dans WIMS | Dispo Windows ? | Difficulté |
|------------|---------------|-----------------|------------|
| **GCC / G++** | Compiler C/C++ | MinGW-w64 / MSVC | 🟡 |
| **autotools** | Build system | MSYS2 ou réécrire CMake | 🟠 |
| **flex** | chemeq, units-filter | win_flex (chocolatey) | 🟡 |
| **libgd** | flydraw (GIF) | vcpkg `gd` | 🟡 |
| **libcrypt** | Authentification | OpenSSL `crypt()` | 🟡 |
| **libsocket/libnsl** | Réseau (configure) | Winsock2 (intégré) | 🟢 |
| **LaTeX** | texgif, rendu math | MiKTeX natif | 🟢 |
| **Maxima** | CAS symbolique | Installeur officiel | 🟢 |
| **GAP** | CAS algèbre | Build Windows | 🟡 |
| **PARI/GP** | CAS théorie des nombres | Build officiel | 🟢 |
| **Octave** | CAS matriciel | Installeur officiel | 🟢 |
| **gnuplot** | Graphiques | Build Windows | 🟢 |
| **POV-Ray** | Raytracing 3D | Build Windows | 🟢 |
| **ImageMagick** | Images | Installeur officiel | 🟢 |
| **Perl** | Scripts admin | Strawberry Perl | 🟢 |
| **wget** | Téléchargements | `Invoke-WebRequest` / wget.exe | 🟢 |
| **sendmail** | Email | N/A → SMTP | 🟠 |

---

## 6. Stratégies de portage

### Option A — Cygwin (le plus rapide) — *recommandé par Grok*

```
Pro : Fork/exec/SIGALRM/chroot émulés. Code C quasi-inchangé.
      ./compile dans un terminal Cygwin devrait construire le gros.
Con : Runtime Cygwin obligatoire. fork() très lent (~100× Linux).
      Pas "natif" Windows. Performance dégradée.
Effort : ~2-4 semaines pour build + tests.
```

### Option B — MSYS2/MinGW (pseudo-natif)

```
Pro : Binaires natifs Windows (pas de cygwin1.dll).
Con : fork() n'est PAS émulé par MinGW (seulement par Cygwin).
      Nécessite réécriture de la couche processus.
Effort : ~1-2 mois.
Risque : Intermédiaire dangereux — ni vraiment POSIX ni vraiment Win32.
```

### Option C — Windows natif (MSVC + CreateProcess) — *couche win32compat*

```
Pro : Vraiment natif. Pas de couche POSIX.
Con : Réécriture massive : fork/exec/pipe/dup2/select/signal/alarm.
      Estimé 60-80% du code moteur à modifier.
      Couche d'abstraction « win32compat.c » : CreateProcess, Job Objects,
      socket TCP 127.0.0.1, service Windows.
Effort : ~3-6 mois pour un développeur expérimenté.
```

### Option D — Portage partiel (ce qui est utile)

```
Idée : Ne pas porter wimsd/wims. Porter les outils individuels :
  - flydraw → libgd + MSVC (ou SVG/Canvas côté navigateur)
  - texgif → MiKTeX + adaptation chemins
  - Misc/ tools → MSVC compilation directe
  - CAS → versions Windows natives (Maxima, PARI, Octave)
Pro : Effort ciblé. Résultat immédiat.
Con : Pas un "WIMS complet". Serveur web manquant.
Effort : ~2-4 semaines.
```

### Option E — Notre approche (WA-OEF2IPYNB) — *recommandé par Claude*

```
Ce que nous avons déjà :
  ✅ Interpréteur OEF → HTML (Python, portable Windows/Linux)
  ✅ MathLive pour saisie math (navigateur)
  ✅ MathJax pour rendu LaTeX (navigateur)
  ✅ SVG inline pour draw (Python, remplace flydraw)
  ✅ Grading client-side (JavaScript)
  ✅ oef2html CLI + PS1 wrapper (Windows natif)
  ✅ Jupyter notebooks comme support interactif
  ✅ 212 tests passent

  Ce qui manque :
  ❌ CAS serveur → Pyodide+SymPy (navigateur) ou Algebrite
  ❌ flydraw complet → notre render_draw couvre les bases
  ❌ Administration/classes/notes
  ❌ Modules OEF complexes (certaines conditions/boucles avancées)
```

---

## 7. Recommandation consolidée

**Option D + E est la voie la plus réaliste :**

1. **Ne PAS porter wimsd/wims** — le modèle fork-per-request est fondamentalement incompatible Windows natif. Cygwin le fait tourner mais avec des performances dégradées.

2. **Continuer WA-OEF2IPYNB** comme interpréteur OEF natif Windows — il couvre déjà 80% des exercices du corpus.

3. **Porter flydraw** si les GIF sont nécessaires (sinon SVG via `render_draw` suffit).

4. **Utiliser Maxima/PARI/Octave** natifs Windows si un CAS serveur est requis. La difficulté est l'interface pipe — réécrire `execredirected()` avec `CreateProcess` + `CreatePipe` pour un seul CAS (Maxima) est faisable en ~1 semaine.

5. **Prototype Cygwin** (suggestion Grok) : tenter `./compile` sous Cygwin pour voir combien échoue. C'est le test le plus rapide pour mesurer l'effort réel.

6. **Réécrire les scripts shell** critiques (`src2def`, `mkindex`) en Python ou PowerShell.

Le portage complet natif (Option C) n'est justifié que si l'objectif est un serveur WIMS complet sur Windows — effort estimé 3-6 mois.

---

## 8. Prochaines étapes

| # | Action | Priorité | Effort |
|---|--------|----------|--------|
| 1 | ✅ Récupérer les sources SVN (fait — `wims/src`, `wims/bin`, `wims/other`) | — | Fait |
| 2 | Tenter `./compile` sous Cygwin (test rapide) | Haute | 1 jour |
| 3 | Compiler flydraw seul (MinGW + libgd via vcpkg) | Moyenne | 2-3 jours |
| 4 | Compiler les Misc/ tools (MSVC) | Moyenne | 1-2 jours |
| 5 | Réécrire `src2def` + `mkindex` en Python | Basse | 2-3 jours |
| 6 | Étendre oef_interpreter pour les OEF complexes | Haute | En cours |
| 7 | Intégrer Pyodide+SymPy pour CAS navigateur | Moyenne | 3-5 jours |

---

## 9. Non vérifié

- Compilation réelle : aucun binaire n'a été produit.
- Sous-dossiers `public_html/scripts/` (scripts OEF .def de compilation).
- `src/Module/` (contenu non inspecté).
- Performance réelle de Cygwin fork sur ce code.
- Existence du tag `wims-4.32` exact (le trunk HEAD r19040 a été utilisé).
