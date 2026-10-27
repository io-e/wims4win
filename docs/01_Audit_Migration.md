# 01 — Audit de la migration de WIMS vers Windows natif (document consolidé)

*Document consolidé le 27/10/2026 (heure de Paris). Il fusionne les audits de Grok et de Claude. Les originaux sont conservés dans `docs/archive/` (historique git préservé par `git mv`).*

## Sources fusionnées

| Source (dans `archive/`) | Auteur | Statut dans ce document |
|---|---|---|
| `Audit_Migration_WIMS_Windows_Fusion.md` (audit fusionné v2 + remarques Grok + étude critique MSVC) | Grok | Repris intégralement (partie A) |
| `Audit_Migration_WIMS_Windows_complement.md` | Grok | Repris intégralement (partie B) |
| `Audit_Migration_WIMS_Windows_Fusion_v1.md` | Grok | Seules les sections non reprises en v2 (divergences initiales, apports de Claude) (partie C) |
| `Audit_Migration_WIMS_Windows_CLAUDE_V2.md` | Claude | Repris intégralement (partie D) |
| `Audit_Migration_WIMS_Windows.md` (audit initial) et `Audit_Migration_WIMS_Windows_Fusion_v2.md` | Grok | Non recopiés : contenu entièrement couvert par la v2 (`Fusion.md`) ; consultables dans `archive/` |

## État réel à la date de consolidation (prime sur le texte des audits)

Les audits datent du 23/10/2026, avant le code. La vérité est le code du dépôt (`master`) et les résultats mesurés, détaillés dans `Suivi_Developpement_Portage.md` et `05_Etat_et_Decisions.yaml`.

| Composant de l'audit | Gravité prévue | État réel mesuré |
|---|---|---|
| `wimsd` (fork par requête) | Bloquant | Non porté, remplacé par Apache (Laragon) + `wims.exe` en CGI : HTTP 200 |
| Interfaces CAS (`common.c`, fork + select) | Bloquant | `src/Platform` (CreateProcess, pipes overlapped, Job Objects) ; PARI, Maxima, Octave, GAP : 24/24 équivalents |
| `ch..root` (chroot, setrlimit) | Bloquant (sécurité) | `ch..root.exe` AppContainer sans capability ; réseau bloqué (S3) ; S1–S2, S4–S9 non testés (issue #8) |
| SIGALRM / setrlimit CPU | Bloquant | Job créé en `CREATE_SUSPENDED` puis watchdog (commit `5f5523b`) : enfant en boucle tué en < 6 s |
| `wimslogd` (AF_UNIX, fork, mmap) | Bloquant | Version minimale par named pipe (ping, loadavg) ; scores et housekeeping à porter (issue #1) |
| MathML (fork + pipe) | Bloquant | `wp_spawn`, sans fork : OK |
| `crypt()` | Moyen | DES (musl) + BCrypt PBKDF2 : 8/8 |
| Flydraw (libgd) | Moyen | `flydraw.exe` (libgd 2.3.3 statique mingw) : image identique à Linux ; reste police TrueType (freetype) et animations |
| Texgif | Bloquant (natif) | Non porté ; MathJax + flydraw utilisés |
| Scripts `bin/` (sh) | Moyen/bloquant | Non portés ; `mv`/`rm` exécutés en interne par le moteur. Voir « Points à trancher » |
| `public_html` / modules | Non vérifiable | Runtime construit depuis `wims-4.32.tgz` (`tools/build-runtime-tree.sh`) ; modules hors distribution absents (issue #3) |

## Divergences résolues

1. **Version de référence :** les audits parlent de « WIMS 4.32 ». Le code suit le trunk **r19040 / 4.33a**, sans rebasage sur 4.31b (décision d'Arnaud du 25/10/2026 ; issue #4 fermée).
2. **Stratégie :** l'audit v1 hésitait entre Cygwin/MSYS2 (Grok) et un portage partiel ou l'interpréteur OEF Python (Claude). Le code réalisé est un **portage natif mingw-w64** via la couche `src/Platform`, avec Apache en frontal.
3. **Effort :** les estimations de 3 à 6 mois (Claude) ou 6 à 7 mois (V3) sont dépassées par le réel : phases 1–4 faites ; il reste environ 2 à 3 semaines côté Grok (voir `05_Etat_et_Decisions.yaml`).
4. **Texgif « bloquant » / « chemins à adapter » :** tranché par l'audit v2 (bloquant en natif) ; sans objet en pratique, l'affichage passe par MathJax.
5. **Mémoire partagée System V :** corrigé, c'est du `mmap` POSIX (`wimsshm.h`).


---

## Partie A — Audit fusionné v2 (Grok + Claude V1), remarques Grok et étude critique MSVC

> Source : `archive/Audit_Migration_WIMS_Windows_Fusion.md`

## Audit fusionné : migration de WIMS vers Windows natif (sans WSL) — v2

*Mise à jour du 23 octobre 2026, 14 h (heure de Paris). Fusion de l'audit Grok et de l'audit Claude (`Audit_Migration_WIMS_Windows_CLAUDE_V1.md`), revérifiés sur un checkout SVN complet. La version précédente est conservée dans `Audit_Migration_WIMS_Windows_Fusion_v1.md`.*

### 1. Sources utilisées
- **Checkout SlikSVN :** `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Claude\wims`.
  - URL : `https://subversion.renater.fr/anonscm/svn/wimsdev/trunk/wims`.
  - **Révision 19040**, ce qui confirme la référence de Claude.
- **Version :** le fichier `version` indique **4.33a**. C'est le trunk, pas 4.32 au sens strict. Pour coller à 4.32, il faudrait identifier un tag (non fait).
- **Sous-dossiers de `src` désormais présents :**

| Sous-dossier | Fichiers |
|---|---|
| `Wimsd` | 2 |
| `Wimslogd` | 15 |
| `Interfaces` | 10 |
| `Flydraw` | 15 |
| `Texgif` | 12 |
| `Lib` | 11 |
| `Misc` | 1 457 |

- **`public_html` est vide (0 fichier)** dans ce checkout : les scripts `public_html` restent **non vérifiés**.
- Analyse en lecture seule ; rien n'a été modifié dans les sources.

### 2. Vérification des points ouverts de la v1
| Point | Résultat | Preuve (fichier:ligne) |
|---|---|---|
| `fork` dans `wimsd.c` (Claude) | **Confirmé** | `Wimsd/wimsd.c:206` fork, `:219-220` dup2 sur socket, `:275` execl, `:76/210/286` alarm, `:211/312` SIGALRM |
| Socket de `wimsd` | **Précisé** : `AF_INET` (TCP), pas Unix | `wimsd.c:141` |
| setreuid dans `wimsd` | **Commenté** (inactif) | `wimsd.c:326` |
| `fork` dans `Interfaces/common.c` (Claude) | **Confirmé** | `common.c:181` fork, `:184/189` dup2 sur pipes, `:201` setreuid/setregid, `:210-211` execv/execvp, `:494/519/549` select, `:443/599` kill SIGKILL, `:45` `/tmp` |
| Wrapper `ch..root` | **Confirmé**, c'est un programme C | `Misc/chroot.c:235` chroot(`../chroot`), `:207/215/238` setrlimit FSIZE/NPROC, `:153` kill, `:242` `/tmp/sessions`, `:257` execv/execvp ; appelé par `Interfaces/common.c:20,196`, `calc.c:323`, `cleaning.c:36`, `Wimslogd/cleaning.c:65` |
| `wimslogd` + socket Unix (Grok) | **Confirmé** | `Wimslogd/socket.c:31-33` PF_UNIX, `:85` select ; `wimslogd.c:197` select, `:61` fork, `:169-170` setreuid/setregid ; `log.c:87`, `cleaning.c:146` fork ; `wimslogdlines.c:31,46-47` fork/execve/execvp ; `fork.c` gestion des enfants |
| Mémoire partagée | **Confirmé POSIX, via mmap** | `wimslogd.c:141` mmap MAP_SHARED (`shm_open` commenté `:139`) ; `wimsshm.h:22` |
| `/proc` | **Confirmé, et une occurrence en plus** | `wims.c:553` `/proc/net/tcp` ; **nouveau** `wimslogd.c:193` `/proc/loadavg` |
| Flydraw | **Confirmé**, code portable mais dépend de libgd | `flydraw.h:25` `#include <gd.h>`, `flydraw.c:78,80` gdImageGif ; `htmldoc.sh`, `nametab.sh`, `flydraw2js.pl` pour la doc |
| Texgif | **Confirmé** : plus qu'un problème de chemins, il y a aussi du fork/exec | `Texgif/basic.c:79` fork, `:88` setreuid, `:90-91` execv/execvp ; `texgif.c:127` setreuid |
| Outils Misc | **Majoritairement confirmé**, avec de **nouveaux points** | voir section 3 |
| chemeq / units-filter (flex) | **Confirmé**, et bison aussi | `Misc/chemeq/src/chemeq.l` + `.y`, `Misc/units-filter/src/unites.l` + `.y` |
| Builds Misc | **Confirmé** autotools | `Makefile.in` dans applets, bioinfo, canvasdraw, checkmol, crossword, lceb, mathexp, moneyprint, scienceprint, sigdigits, symtext, toascii, voronoiW, whirlgif |
| Scripts `public_html` | **Non vérifiable** | dossier vide dans le checkout |
| `Lib/` | **Aucun appel système bloquant** trouvé ; seuls `random`/`strcasecmp` restent à vérifier (dits par Claude) | — |

### 3. Nouveaux problèmes trouvés
- **canvasdraw :**
  - `Misc/canvasdraw/canvasdraw.c:6330-6339` : fork + execv `../bin/wims_mathml` ;
  - `:6372-6382` : fork + execvp `obabel` (Open Babel, dépendance externe non listée jusqu'ici).
- **wrap :** `Misc/wrap.c:34,38` exécute via execv/execvp. Ce wrapper d'exécution devient inutile sans setuid.
- **webget :** `Misc/webget.c:37` (`/tmp`) et `:117` (`system`).
- **clickzone :** `Misc/clickzone.c:115` (`system`).
- **lceb :** `Misc/lceb/random.c:58` `srand(getpid())`. Portable (`_getpid`), impact faible.
- **wimslogd :** `/proc/loadavg` (`wimslogd.c:193`) → `GetSystemTimes` / compteurs de performance.
- **Texgif :** il lance lui-même TeX via fork/exec et change d'uid. Sa gravité passe de « difficile/faible » à **bloquant (natif) / OK (Cygwin)**.
- **Octave :** `Interfaces/octave.c:73-77` contient une liste de fonctions interdites (`system`, `popen`…). Il s'agit d'un filtrage de sécurité qu'il faut conserver.

### 4. Tableau unifié des composants (v2)
| Composant | Fichiers:lignes | Problème Windows | Gravité | Piste | Source |
|---|---|---|---|---|---|
| Serveur `wimsd` | `Wimsd/wimsd.c:76-326` | fork par requête, dup2 sur socket, execl, SIGALRM ; socket AF_INET | Bloquant (natif) | Apache/IIS + CGI (recommandé), sinon Winsock + CreateProcess | Claude, vérifié |
| Moteur `wims`, lancement de programmes | `lines.c:927-972`, `exec.c` | fork/execve/waitpid, popen/system | Bloquant | CreateProcess, ou Cygwin | Les deux, vérifié |
| MathML | `mathml.c:42,132` ; `canvasdraw.c:6330` | fork + pipe + execv | Bloquant | CreateProcess + CreatePipe | Grok + nouveau |
| Délais | `lines.c:853-877`, `wimsd.c:76,210-211,286,312` | SIGALRM/alarm | Bloquant | Timer thread / Job Object | Les deux |
| Limites de ressources | `wims.c:591`, `auth.c:71`, `config.c:509`, `Misc/chroot.c:207-238` | setrlimit CPU/FSIZE/NPROC | Moyen | Job Objects | Les deux + nouveau |
| Arrêt des processus | `cleaning.c:31`, `lines.c:843`, `Interfaces/common.c:443,599`, `Misc/chroot.c:153` | kill SIGKILL | Moyen | TerminateProcess | Les deux |
| Changement d'identité | `Interfaces/common.c:201`, `Texgif/basic.c:88`, `texgif.c:127`, `wimslogd.c:169-170`, `lines.c`, `log.c` | setreuid/setregid | Moyen | Compte de service, suppression | Les deux, vérifié |
| Daemon `wimslogd` | `Wimslogd/*.c` : `socket.c:31-33,85`, `wimslogd.c:61,141,193,197`, `log.c:87`, `cleaning.c:146`, `wimslogdlines.c:31-47` | Daemon, fork, socket PF_UNIX, select, mmap, `/proc/loadavg` | Bloquant | Service Windows + named pipe / TCP + CreateFileMapping | Grok, vérifié |
| Interfaces CAS | `Interfaces/common.c:20-599` ; `maxima.c`, `pari.c`, `octave.c`, `gap.c`, `m2.c`, `scilab.c`, `yacas.c` | fork + pipes bidirectionnels + select sur pipes (impossible avec Winsock select) + chroot | Bloquant (natif) / OK (Cygwin) | Threads + CreatePipe + PeekNamedPipe ; binaires CAS Windows | Les deux, vérifié |
| Sandbox `ch..root` | `Misc/chroot.c:153-257` ; appelé depuis `calc.c:323`, `cleaning.c:36`, `Interfaces/common.c:196`, `Wimslogd/cleaning.c:65` | chroot, setrlimit, `/tmp/sessions` | Bloquant (sécurité) | Neutraliser (pas de chroot) + Job Object/AppContainer | Les deux, vérifié |
| Mémoire partagée | `wimsshm.h:22`, `wimslogd.c:139-143` | mmap MAP_SHARED | Bloquant | CreateFileMapping | Grok, corrigé |
| `/proc` | `wims.c:553` (net/tcp), `wimslogd.c:193` (loadavg) | Absent | Moyen | GetExtendedTcpTable, GetSystemTimes | Claude + nouveau |
| Texgif | `Texgif/basic.c:79-91`, `texgif.c:127` | fork/exec TeX, setreuid | Bloquant (natif) | CreateProcess + MiKTeX | Les deux, réévalué |
| Flydraw | `flydraw.h:25`, `flydraw.c:78-80` ; scripts doc `htmldoc.sh`, `nametab.sh` | libgd ; scripts sh | Moyen | libgd vcpkg/MSYS2 | Les deux, vérifié |
| canvasdraw | `canvasdraw.c:6330-6382` | fork/exec `wims_mathml` et `obabel` | Moyen à bloquant | CreateProcess ; Open Babel Windows | Nouveau |
| Wrapper et outils Misc | `wrap.c:34-38`, `webget.c:37,117`, `clickzone.c:115`, `lceb/random.c:58` | exec, system, `/tmp` | Moyen à faible | `_spawn`, %TEMP% | Nouveau |
| chemeq, units-filter | `chemeq/src/chemeq.l/.y`, `units-filter/src/unites.l/.y` | flex + bison, g++ | Moyen | win_flex_bison ou MSYS2 | Claude, vérifié |
| Builds (cœur + Misc) | `src/configure.ac`, `Makefile.in`, ~20 `Makefile.in` dans Misc, `compile` | Autotools, sh | Moyen (MSYS2) / difficile (CMake) | MSYS2 puis CMake | Les deux, vérifié |
| Envoi de mail | `exec.c` | sendmail | Difficile | SMTP | Claude |
| Authentification | `calc.c` | crypt() | Moyen | bcrypt / OpenSSL | Claude |
| Liens symboliques | `calc.c:1964`, `files.c`, `variables.c` | symlink | Moyen | Copie / CreateSymbolicLink | Les deux |
| Permissions / chemins / verrous / popen | `wims.c`, `log.c`, `exec.c`, `variables.c`, `includes.h` | chmod, `/tmp`, flock, popen | Moyen | ACL, %TEMP%, LockFileEx, `_popen` | Les deux |
| Compatibilité libc | `src/Lib`, partout | random, strcasecmp, setenv | Faible | Macros `_WIN32` | Claude (non revérifié) |
| Scripts d'exploitation | `bin/*` (oefcompile, src2def, mkindex, modupdate, housekeep.daily…), `update.sh` | sh + coreutils + cron | Moyen (MSYS2) / bloquant (natif) | Bash MSYS2 + Planificateur, ou Python | Les deux |
| Scripts Perl | css.pl, refer.pl, oefdoc.pl, rss.pl, `flydraw2js.pl` | Chemins | Faible | Strawberry Perl | Les deux |
| Logiciels externes | Maxima, PARI, Octave, GAP, M2, Scilab, Yacas, gnuplot, POV-Ray, ImageMagick, TeX, **Open Babel** | Versions Windows existantes (GAP, M2 plus délicats) | Faible à moyen | Installeurs officiels | Les deux + nouveau |
| Front-end | MathJax, JSmol, GeoGebra, contenus OEF | Aucun problème | Aucune | — | Claude |

### 5. Divergences entre les audits (mises à jour)
1. **`fork` :** Claude avait raison pour `wimsd.c` et `Interfaces/common.c`. Grok avait raison pour `lines.c`, `mathml.c`, `log.c` et `wimslogd`. Les deux listes sont complémentaires.
2. **Socket de `wimsd` :** c'est du TCP (AF_INET) et non une socket Unix, ce qui le rend portable via Winsock. Le seul socket Unix est celui de `wimslogd`.
3. **Texgif :** Claude parlait de « chemins à adapter », Grok de « faible ». Il est en fait **bloquant en natif** (fork/exec/setreuid).
4. **Gravité des scripts et des CAS, effort du natif :** inchangé par rapport à la v1. On retient l'estimation de Claude, **3 à 6 mois**, qui se confirme : on trouve du fork/exec dans au moins 8 programmes distincts.

### 6. Stratégie consolidée (inchangée sur le fond, renforcée)
Le checkout complet confirme que **fork/exec, pipes + select, setreuid et chroot sont omniprésents**. On les trouve dans `wims`, `wimsd`, `wimslogd`, `Interfaces`, `Texgif`, `ch..root`, `canvasdraw` et `wrap`. D'où trois recommandations :

1. **WIMS complet sans WSL : Cygwin**, avec `wimsd` remplacé par Apache (Cygwin ou natif) en CGI. Le sandbox `ch..root` est désactivé ; on accepte la perte d'isolation, ou on fait tourner le service sous un compte restreint.
2. **Contenus OEF sous Windows :** l'option C + D de Claude (interpréteur Python WA-OEF2IPYNB, flydraw/SVG, CAS Windows natifs) reste la plus économique.
3. **Natif Win32 pur :** seulement si c'est un impératif. Il faut alors une couche `win32compat` couvrant processus, pipes avec select, timers, mémoire partagée et service, plus le remplacement de `ch..root`. Effort : 3 à 6 mois.

### 7. Prochaines étapes
1. Récupérer le tag correspondant exactement à 4.32 (`svn ls https://subversion.renater.fr/anonscm/svn/wimsdev/tags`) et compléter `public_html` (`svn update`).
2. Faire un prototype de compilation sous Cygwin (`./compile`) et noter les erreurs.
3. Choisir l'objectif : serveur complet ou contenus OEF.
4. Vérifier les scripts `public_html/scripts` et `bin/` une fois présents.

### 8. Non vérifié
- Les scripts de `public_html` (dossier vide dans le checkout).
- La correspondance exacte avec la 4.32 : le trunk analysé est en 4.33a.
- Toute compilation.
- Les occurrences de `random`/`strcasecmp` dans `Lib`.
### 9. Remarques Grok
- **Version :** le checkout `trunk/wims` r19040 est en **4.33a** (fichier `version`), pas en 4.32. Il faut trouver le tag 4.32 (`svn ls https://subversion.renater.fr/anonscm/svn/wimsdev/tags`) et faire un diff avec le trunk avant toute décision de portage.
- **`public_html` est vide (0 fichier)** dans le checkout : il est peut-être incomplet. Faire un `svn update`, puis analyser `public_html/scripts` (beaucoup de shell, selon l'architecture WIMS habituelle).
- **Dépendances externes oubliées jusqu'ici :**
  - **Open Babel** (`obabel`, `Misc/canvasdraw/canvasdraw.c:6382`) ;
  - **flex + bison** (`chemeq.l/.y`, `unites.l/.y`) ;
  - **libgd** (`Flydraw/flydraw.h:25`) ;
  - **libcrypt** (`calc.c`) ;
  - **sendmail** (`exec.c`) ;
  - **un shell `sh` à l'exécution :** `Texgif/basic.c:115` appelle `execredirected("sh", …)`, et `calc.c:1181` passe à `call_sh` une syntaxe bash (`&>`).
- **Il y a 4 implémentations distinctes de `execredirected()`** à porter : `Interfaces/common.c:168`, `Texgif/basic.c:72`, `Wimslogd/wimslogdlines.c:24`, et celle du cœur appelée depuis `calc.c:261` (`lines.c`). S'y ajoutent les fork directs de `mathml.c`, `canvasdraw.c`, `wimsd.c`, `log.c`, `wimslogd.c` et `cleaning.c`.
- **Filtrage de sécurité Octave** (`Interfaces/octave.c:73-77` : `system`, `popen`, `fopen`, `mkdir`… interdits) : c'est un garde-fou à conserver tel quel, quel que soit le portage. Il devient encore plus important si `ch..root` disparaît.
- **`ch..root` est un exécutable à part** (`Misc/chroot.c`), invoqué comme préfixe de commande (`Interfaces/common.c:196-197`), et non un appel `chroot()` dans le cœur. Il fait aussi le nettoyage `cleantmpdir` (`cleaning.c:36`, `Wimslogd/cleaning.c:65`) et pose des setrlimit FSIZE/NPROC (`chroot.c:207-238`).
- **`wimslogd`** s'appuie sur une socket PF_UNIX (`socket.c:31`), `/proc/loadavg` (`wimslogd.c:193`), un mmap partagé (`wimslogd.c:141`) et du fork. C'est le composant natif le plus coûteux après les interfaces CAS.
- **`wimsd`** est un mini-serveur HTTP sur TCP qui fait fork puis `execl` de `wims` (`wimsd.c:206-275`) : c'est un lanceur CGI sans état. **Apache ou IIS en CGI le remplace entièrement**, sans rien porter.
- Dans `Lib/`, seulement 4 occurrences de `strcasecmp`/`random` : impact négligeable.

### 10. Étude critique de la proposition « portage natif MSVC » de Claude
*Document étudié : `Reprise_Wims_Claude\Etude_Portage_MSVC_Natif_CLAUDE_V1.md`, recoupé avec le checkout r19040.*

#### 10.1 Points justes
- **Le tableau POSIX → Win32 est globalement correct :** CreateProcess/CreatePipe, Job Objects pour CPU et mémoire, TerminateProcess, CreateTimerQueueTimer, CreateFileMapping, GetExtendedTcpTable, LockFileEx.
- **Couche de compatibilité séparée (`src/win32compat/`) avec `#ifdef _WIN32`**, à la manière de PostgreSQL `src/port`, nginx `src/os/win32` ou OpenSSH `contrib/win32` : bonne organisation, qui ne casse pas le code Linux.
- **Le squelette `execredirected_win32`** (pipes non hérités côté parent, `CREATE_SUSPENDED` + `AssignProcessToJobObject` + `ResumeThread`, timeout puis TerminateProcess) est le bon schéma.
- **Outils pertinents :** CMake, vcpkg (libgd), winflexbison, NSSM, AppContainer.
- **Conclusion juste :** le natif ne se justifie pas, sauf pour une distribution officielle ou de fortes charges.

#### 10.2 Erreurs
1. **Mémoire partagée :** l'étude propose de remplacer `shmget`/`shmat` (System V). WIMS n'en utilise pas : c'est du **mmap POSIX** (`wimslogd.c:139-141`, `wimsshm.h:22`). Les macros `shmget`… de `win32compat.h` sont donc inutiles. Il faut émuler `mmap`/`shm_open`.
2. **`#define chroot(x) (0)` et `setreuid(...) (0)` :** dans WIMS, le chroot n'est pas un appel du cœur mais l'exécutable **`ch..root`** (`Misc/chroot.c`). Un no-op silencieux supprime l'isolation sans le signaler. Dans un serveur qui exécute du code CAS fourni par des modules, c'est un **risque de sécurité majeur**, à remplacer explicitement par Job Object, AppContainer et un compte restreint, pas à masquer.
3. **`select()` sur pipes → `WaitForMultipleObjects` :** c'est faux tel quel. Les pipes anonymes Win32 ne sont pas attendables. Les interfaces CAS (`Interfaces/common.c:494,519,549`) multiplexent des pipes et des FIFO en dialogue continu avec le CAS. Il faut des **named pipes en mode overlapped** ou un **thread de lecture par pipe**. Le code proposé (écrire tout, puis lire tout) ne couvre pas ce mode interactif et risque un interblocage quand le buffer du pipe est plein.
4. **« Re-exec self » à la PostgreSQL pour `wimsd` :** inutile. `wimsd` ne garde aucun état : il fait seulement fork puis `execl(wims)` (`wimsd.c:206-275`). Apache ou IIS en CGI le remplace. L'analogie avec postmaster, qui partage un état complexe, surestime le besoin.
5. **« Socket Unix → TCP » attribué à `wimsd` :** `wimsd` est déjà en AF_INET (`wimsd.c:141`). La socket Unix est celle de **`wimslogd`** (`socket.c:31`) et de son client (`lines.c:1355`).
6. **« MSYS2 = fork lent » :** il y a confusion. Le fork lent est celui du runtime MSYS/Cygwin. En MSYS2 **MinGW**, on compile du natif Win32 sans fork. La piste « 2-3 semaines » correspond donc à **Cygwin ou au runtime MSYS**, pas à MinGW.
7. **`DEFAULT_SHELL "cmd.exe"` :** WIMS passe des commandes en syntaxe sh/bash (`call_sh`, `calc.c:1181` `&>`, `Texgif/basic.c:115`). Avec `cmd.exe`, elles ne fonctionneront pas : il faut embarquer un `sh` (busybox-w32 ou MSYS) ou réécrire ces appels.

#### 10.3 Oublis
- Les **4 implémentations de `execredirected`**, plus les fork directs dans 6 autres fichiers. L'étude n'en porte qu'une.
- **`wimslogd` entier :** daemon, `/proc/loadavg`, fork de tâches (`log.c:87`, `cleaning.c:146`), gestion des enfants (`fork.c`). Le service Windows n'est évoqué que pour `wimsd`.
- **Texgif** (fork/exec + `sh`), **canvasdraw** (fork, `wims_mathml`, `obabel`), `Misc/wrap.c`, `webget.c`, `clickzone.c`.
- **Dépendances :** Open Babel, libcrypt (bcrypt/OpenSSL), sendmail (SMTP).
- **Filtrage de sécurité Octave**, et plus généralement le modèle de sécurité de remplacement.
- **Signaux autres que SIGALRM** (SIGCHLD et gestion des enfants dans `wimslogd`), et le comportement de `waitpid` non bloquant.
- **Chemins :** séparateurs, `../chroot`, `/tmp/sessions`, `../bin/wims_mathml` codés en dur.
- **Tests :** il n'existe pas de jeu de non-régression. Il faudrait comparer la sortie HTML Linux et la sortie Windows module par module.
- **Version :** l'étude vise 4.32, mais le code analysé est en 4.33a.

#### 10.4 Réalisme de l'estimation (~3 mois)
L'estimation de Claude est **optimiste**. Ses postes « execredirected 2 semaines » et « chroot 1 jour » ignorent les 4 implémentations, le multiplexage interactif et le vrai remplacement du sandbox. Elle omet aussi `wimslogd`, Texgif, canvasdraw, les outils `Misc`, crypt et sendmail, et ne compte aucun temps de tests.

| Poste | Estimation Grok |
|---|---|
| Couche processus et pipes (4 × execredirected + 6 fork directs + I/O interactif) | 4-6 semaines |
| `wimslogd` en service Windows (named pipe, mapping, charge système, tâches) | 2-3 semaines |
| Sandbox de remplacement (Job Object + AppContainer/compte restreint + cleantmpdir) | 2-3 semaines |
| Timers/limites, `/proc`, crypt, sendmail, chemins | 2 semaines |
| CMake (cœur + ~20 Makefile Misc + flex/bison + libgd) | 2-3 semaines |
| Scripts `bin/` (35) + `public_html/scripts` (non chiffré) → Python/PowerShell | 3-5 semaines |
| Recette et non-régression | 3-4 semaines |
| **Total** | **≈ 4,5 à 6,5 mois pour 1 développeur expérimenté** |

Ce total est cohérent avec la fourchette haute de Claude (3-6 mois) et dépasse les « ~3 mois » de son tableau détaillé. `wimsd` n'y figure pas, puisqu'il est remplacé par Apache ou IIS.

#### 10.5 Risques
- **Sécurité :** perte de `ch..root`, exécution de code CAS non isolée. C'est le risque principal.
- **Divergence avec l'amont :** des `#ifdef` partout rendent les mises à jour du trunk coûteuses, sauf si les patchs sont reversés à l'équipe WIMS.
- **Interblocages et timeouts** sur les pipes interactifs avec les CAS.
- **Disponibilité et compatibilité** des binaires CAS Windows (GAP, M2, Yacas, Scilab) et de leurs options en ligne de commande.
- **Écart 4.32 / 4.33a.**

#### 10.6 Verdict
**La proposition est techniquement faisable et bien orientée** : couche `win32compat`, CreateProcess + Job Objects, CMake, vcpkg. En revanche, **elle n'est pas exploitable telle quelle**. Sa mémoire partagée vise la mauvaise API, ses no-op chroot/setreuid sont dangereux, son `select` sur pipes est faux, `wimsd` est mal ciblé, elle choisit `cmd.exe` comme shell, sous-estime l'effort et oublie `wimslogd`, Texgif et canvasdraw.

**Recommandation inchangée :**
1. Prototype Cygwin avec Apache en CGI pour obtenir un WIMS complet rapidement.
2. Ne lancer le natif MSVC que si un besoin de production Windows à grande échelle est avéré. Dans ce cas : commencer par `Interfaces/common.c` (I/O interactif par threads ou overlapped), traiter la sécurité comme un livrable à part entière, et reverser les patchs à l'amont.


---

## Partie B — Complément (sous-dossiers 4.31b)

> Source : `archive/Audit_Migration_WIMS_Windows_complement.md`

## Complément à l'audit : migration de WIMS vers Windows natif

*23 octobre 2026. Complète la section 7 (« Non vérifié ») de `Audit_Migration_WIMS_Windows.md`.*

### Source utilisée
Le serveur Renater reste injoignable depuis la box (`svn ls` coupé après 90 s). Les sous-dossiers manquants ont donc été lus dans les sources complètes de **WIMS 4.31b** (`wims_4.31b+dfsg1.orig.tar.gz`, déjà présent sur la box).

Proximité avec la 4.32 : sur les 24 fichiers `.c` récupérés du clone partiel 4.32, 14 sont identiques en 4.31b et 10 diffèrent. Les constats ci-dessous portent sur l'architecture et les appels système, qui changent peu d'une version à l'autre, mais ils restent à confirmer sur la 4.32.

### Sous-dossiers désormais vérifiés (4.31b)
| Composant | Fichier(s) | Appels Unix relevés | Gravité | Piste |
|---|---|---|---|---|
| Daemon `wimslogd` | `Wimslogd/wimslogd.c`, `fork.c`, `socket.c`, `wimslogdlines.c`, `cleaning.c` | `fork`, `waitpid`, `kill(SIGKILL)`, `execve`/`execvp`, `system`, `socket(PF_UNIX)` + `sockaddr_un`, `mmap(MAP_SHARED)` pour la mémoire partagée, `chroot`, `chmod` | Bloquant | Confirmé : service Windows + TCP localhost ou named pipe + `CreateFileMapping` ; ou Cygwin |
| Interfaces logiciels (pari, maxima, octave, gap, m2, scilab, yacas) | `Interfaces/common.c` (cœur commun), `octave.c`, `m2.c`, `maxima.c` | `fork`, `pipe` x3, `dup2`, `execv`/`execvp`, `waitpid`, `kill(SIGKILL)`, `chroot`, `popen`, `umask` | Bloquant | Une seule réécriture de `common.c` (CreateProcess + CreatePipe + Job Object) couvre la plupart des interfaces |
| Wrapper sandbox | `Misc/chroot.c` (programme « Change root and exec », installé en `ch..root`) | `chroot`, `setrlimit`/`getrlimit`, `execv`, `kill`, uid à partir de 10000 | Bloquant pour la sécurité | Pas d'équivalent direct : Job Object + compte restreint, ou abandon de la sandbox en usage local |
| Wrapper uid | `Misc/wrap.c` (« Wrap uid and exec ») | `execv`/`execvp`, changement d'uid/gid | Moyen | À supprimer sous Windows |
| Serveur HTTP intégré | `Wimsd/wimsd.c` (« http daemon for WIMS ») | `fork`, `waitpid`, `alarm`/`SIGALRM`, `dup2`, `execl`, sockets BSD | Moyen | Optionnel si Apache est utilisé ; sinon à réécrire (Winsock + CreateProcess) |
| Texgif | `Texgif/basic.c` | `fork`, `execv`/`execvp`, `waitpid`, `system` (appel de TeX) | Moyen | CreateProcess vers MiKTeX/TeX Live |
| Flydraw | `Flydraw/*.c`, `flydraw.h` | Dépend de **libgd** (`gd.h`). Les « kill » trouvés sont des commandes de dessin (`killbrush`, `killtile`), pas des appels système. Un seul `system` | Faible | Compilation MinGW-w64 avec libgd (paquet MSYS2) |
| Canvasdraw et utilitaires | `Misc/canvasdraw/canvasdraw.c`, `canvasreply.c`, `checkmol`, `crossword`, `scienceprint`, `webget.c` | canvasdraw : `fork` x4, `pipe` x4, `dup2`, `execv`, beaucoup de `system` (35) ; les autres surtout `system` | Moyen | Revoir les appels `system` (commandes sh) au cas par cas |
| `Module` | `src/Module/` | Aucun appel Unix relevé dans le C | Faible | Rien de particulier |
| Scripts de `public_html/bin` | 42 entrées, dont `auth..ldap`, `convert..switch`, `insdraw..processor`, `insplot..processor` | Scripts sh appelés par WIMS | Moyen | bash MSYS2 ou réécriture |
| Scripts de `public_html/scripts` | 44 fichiers exécutables ou `.sh` | Shell | Moyen | Idem |

### Ce que ça change à la synthèse
- Le tableau principal est confirmé : les points bloquants sont `fork`/`exec`, les sockets Unix, la mémoire partagée et le daemon, et ils se retrouvent aussi dans `wimslogd` et `Interfaces/common.c`.
- Bonne nouvelle pour un portage natif : les interfaces vers les logiciels de calcul passent presque toutes par `Interfaces/common.c`, donc une seule couche à réécrire.
- La sandbox (`ch..root`) n'a pas d'équivalent Windows simple. Pour un usage local ou de développement, la désactiver est le choix le plus réaliste.
- La recommandation reste la même : Cygwin pour un WIMS qui tourne vite sous Windows sans WSL, MinGW natif seulement pour un portage durable.

### Toujours non vérifié
- Ces constats sur la 4.32 exacte (faire `svn export` de `trunk/wims-4.32` depuis la machine locale).
- La compilation réelle.
- Le contenu de `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims`.


---

## Partie C — Divergences initiales et apports de Claude (audit fusionné v1)

> Source : `archive/Audit_Migration_WIMS_Windows_Fusion_v1.md` — sections non reprises en v2

### 4. Divergences entre les deux audits
1. **Gravité des scripts shell et des interfaces CAS :** bloquante pour Claude, moyenne pour Grok. Avec Cygwin/MSYS2, ils tournent tels quels, donc « moyen » ; sans couche POSIX, ils sont bloquants. Les deux avis se valent, selon l'option choisie.
2. **Effort du natif complet :** 3 à 6 mois pour Claude, « plusieurs semaines » pour Grok. **On retient l'estimation de Claude.**
3. **Stratégie :** Grok recommande Cygwin pour obtenir un WIMS complet. Claude recommande de ne pas porter le moteur et de poursuivre l'interpréteur OEF Python (C + D). Ces deux recommandations répondent à deux objectifs différents, que l'utilisateur doit trancher.
4. **Mémoire partagée :** Grok la décrivait comme System V. La vérification locale de `wimsshm.h` (`SHM_NAME "/wims.shm"`) montre de la **mémoire partagée POSIX**. Grok est corrigé ; Claude ne la mentionnait pas.
5. **Localisation de `fork` :** Claude le situe dans `wimsd.c` / `Interfaces/common.c`. Localement, `fork` est confirmé dans `lines.c`, `log.c` et `mathml.c`, et les sous-dossiers ne sont pas vérifiables.
6. **Oublis de chaque audit :**
   - Claude omet `wimslogd`/`AF_UNIX`, MathML, la mémoire partagée, flock et popen.
   - Grok omet `wimsd`, `/proc/net/tcp`, sendmail, `crypt`, `select`, setreuid, les outils `Misc` et la compatibilité libc.

### 5. Apports de Claude
- Le serveur `wimsd`.
- L'inventaire de `Misc` (chemeq, canvasdraw, units-filter…).
- `/proc/net/tcp`, sendmail, libcrypt.
- Le tableau des équivalents POSIX → Win32.
- Le tableau des dépendances externes (GAP, flex, libgd via vcpkg).
- Les options C/D (portage partiel, WA-OEF2IPYNB).
- Les estimations d'effort et un schéma d'architecture.


---

## Partie D — Audit consolidé de Claude (V2)

> Source : `archive/Audit_Migration_WIMS_Windows_CLAUDE_V2.md`

## Audit consolidé — Migration WIMS 4.32 sur Windows (sans WSL)

**Date:** 2026-10-23  
**Auteurs:** Claude (analyse SVN r19040 complète) + Grok (analyse partielle, 101 fichiers)  
**Source:** SVN `https://subversion.renater.fr/anonscm/svn/wimsdev/trunk` (r19040)  
**Sources locales:** `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Claude\wims\{src,bin,other}`  
**Objectif:** Lister les programmes/composants qui posent problème pour un portage natif Windows.

---

### 1. Architecture WIMS — vue d'ensemble

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

### 2. État de la récupération des sources

| Item | Claude | Grok |
|------|--------|------|
| Méthode | SlikSVN 1.14.5 (`svn checkout --depth`) | `git svn clone -r HEAD` (coupé à 12 min) |
| Résultat | `wims/src/` complet (12 sous-dossiers + 43 fichiers), `wims/bin/` (36 scripts), `wims/other/bin/` (3 fichiers) | 101 fichiers partiels ; `Interfaces`, `Flydraw`, `Texgif`, `Wimslogd` vides |
| Sous-dossiers vérifiés | ✅ Tous (Wimsd, Interfaces, Flydraw, Texgif, Lib, Misc, Module, Mathml, OefMsg2wims, Wimslogd) | ❌ Partiels (src/*.c et bin/ seulement) |
| SVN relocate | Non nécessaire (accès direct) | Non fait |

---

### 3. Programmes problématiques — classement par difficulté

#### Légende

| Niveau | Signification |
|--------|---------------|
| 🔴 **BLOQUANT** | Réécriture majeure ou remplacement nécessaire |
| 🟠 **DIFFICILE** | Portage possible mais effort > 1 semaine |
| 🟡 **MOYEN** | Adaptation mineure (1–5 jours) |
| 🟢 **FACILE** | Fonctionne ou quasi-prêt |

---

#### 🔴 BLOQUANT — Programmes qui ne fonctionneront PAS sur Windows

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

#### 🟠 DIFFICILE — Portage possible avec effort significatif

| # | Programme | Fichier(s) | Problème | Solution | Confirmé par |
|---|-----------|------------|----------|----------|-------------|
| D1 | **flydraw** | `src/Flydraw/flydraw.c` + 4 sources | **libgd** (GIF). `strcasecmp`. Build autotools. | CMakeLists.txt + vcpkg `gd`. `_stricmp`. | Claude |
| D2 | **texgif** | `src/Texgif/texgif.c` + 7 sources | TeX → GIF. DVI parsing custom. Chemins `/tmp`. | MiKTeX + `%TEMP%`. C portable. | Claude + Grok |
| D3 | **Misc/chemeq** | `src/Misc/chemeq/` | `flex` + `g++`. | MinGW/MSYS2 ou win_flex + MSVC. | Claude |
| D4 | **configure / Makefile** | `configure.ac`, `Makefile.in` | Autotools. Vérifie `fork`, `select`, `alarm`, `ln -s`. | CMake. Ou MSYS2 (pas "natif"). | Claude + Grok |
| D5 | **Sockets Unix** | `wims.h:357`, `lines.c:1346-1355` | `socket(PF_UNIX)`, `sockaddr_un`. | TCP localhost ou Named Pipes. `AF_UNIX` partiel depuis Win10 1803. | Grok |
| D6 | **sendmail** | `exec.c` (exec_mailto) | Pipe vers `/usr/sbin/sendmail`. | SMTP via `blat`, `Send-MailMessage`, ou lib SMTP. | Claude |

---

#### 🟡 MOYEN — Adaptations mineures

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

#### 🟢 FACILE — Fonctionne déjà ou quasi-prêt

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

### 4. Appels système POSIX bloquants — inventaire complet

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

### 5. Dépendances externes

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

### 6. Stratégies de portage

#### Option A — Cygwin (le plus rapide) — *recommandé par Grok*

```
Pro : Fork/exec/SIGALRM/chroot émulés. Code C quasi-inchangé.
      ./compile dans un terminal Cygwin devrait construire le gros.
Con : Runtime Cygwin obligatoire. fork() très lent (~100× Linux).
      Pas "natif" Windows. Performance dégradée.
Effort : ~2-4 semaines pour build + tests.
```

#### Option B — MSYS2/MinGW (pseudo-natif)

```
Pro : Binaires natifs Windows (pas de cygwin1.dll).
Con : fork() n'est PAS émulé par MinGW (seulement par Cygwin).
      Nécessite réécriture de la couche processus.
Effort : ~1-2 mois.
Risque : Intermédiaire dangereux — ni vraiment POSIX ni vraiment Win32.
```

#### Option C — Windows natif (MSVC + CreateProcess) — *couche win32compat*

```
Pro : Vraiment natif. Pas de couche POSIX.
Con : Réécriture massive : fork/exec/pipe/dup2/select/signal/alarm.
      Estimé 60-80% du code moteur à modifier.
      Couche d'abstraction « win32compat.c » : CreateProcess, Job Objects,
      socket TCP 127.0.0.1, service Windows.
Effort : ~3-6 mois pour un développeur expérimenté.
```

#### Option D — Portage partiel (ce qui est utile)

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

#### Option E — Notre approche (WA-OEF2IPYNB) — *recommandé par Claude*

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

### 7. Recommandation consolidée

**Option D + E est la voie la plus réaliste :**

1. **Ne PAS porter wimsd/wims** — le modèle fork-per-request est fondamentalement incompatible Windows natif. Cygwin le fait tourner mais avec des performances dégradées.

2. **Continuer WA-OEF2IPYNB** comme interpréteur OEF natif Windows — il couvre déjà 80% des exercices du corpus.

3. **Porter flydraw** si les GIF sont nécessaires (sinon SVG via `render_draw` suffit).

4. **Utiliser Maxima/PARI/Octave** natifs Windows si un CAS serveur est requis. La difficulté est l'interface pipe — réécrire `execredirected()` avec `CreateProcess` + `CreatePipe` pour un seul CAS (Maxima) est faisable en ~1 semaine.

5. **Prototype Cygwin** (suggestion Grok) : tenter `./compile` sous Cygwin pour voir combien échoue. C'est le test le plus rapide pour mesurer l'effort réel.

6. **Réécrire les scripts shell** critiques (`src2def`, `mkindex`) en Python ou PowerShell.

Le portage complet natif (Option C) n'est justifié que si l'objectif est un serveur WIMS complet sur Windows — effort estimé 3-6 mois.

---

### 8. Prochaines étapes

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

### 9. Non vérifié

- Compilation réelle : aucun binaire n'a été produit.
- Sous-dossiers `public_html/scripts/` (scripts OEF .def de compilation).
- `src/Module/` (contenu non inspecté).
- Performance réelle de Cygwin fork sur ce code.
- Existence du tag `wims-4.32` exact (le trunk HEAD r19040 a été utilisé).
