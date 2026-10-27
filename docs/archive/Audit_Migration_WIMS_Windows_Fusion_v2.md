# Audit fusionné : migration de WIMS vers Windows natif (sans WSL) — v2

*Mise à jour du 23 octobre 2026, 14 h (heure de Paris). Fusion de l'audit Grok et de l'audit Claude (`Audit_Migration_WIMS_Windows_CLAUDE_V1.md`), revérifiés sur un checkout SVN complet. La version précédente est conservée dans `Audit_Migration_WIMS_Windows_Fusion_v1.md`.*

## 1. Sources utilisées
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

## 2. Vérification des points ouverts de la v1
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

## 3. Nouveaux problèmes trouvés
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

## 4. Tableau unifié des composants (v2)
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

## 5. Divergences entre les audits (mises à jour)
1. **`fork` :** Claude avait raison pour `wimsd.c` et `Interfaces/common.c`. Grok avait raison pour `lines.c`, `mathml.c`, `log.c` et `wimslogd`. Les deux listes sont complémentaires.
2. **Socket de `wimsd` :** c'est du TCP (AF_INET) et non une socket Unix, ce qui le rend portable via Winsock. Le seul socket Unix est celui de `wimslogd`.
3. **Texgif :** Claude parlait de « chemins à adapter », Grok de « faible ». Il est en fait **bloquant en natif** (fork/exec/setreuid).
4. **Gravité des scripts et des CAS, effort du natif :** inchangé par rapport à la v1. On retient l'estimation de Claude, **3 à 6 mois**, qui se confirme : on trouve du fork/exec dans au moins 8 programmes distincts.

## 6. Stratégie consolidée (inchangée sur le fond, renforcée)
Le checkout complet confirme que **fork/exec, pipes + select, setreuid et chroot sont omniprésents**. On les trouve dans `wims`, `wimsd`, `wimslogd`, `Interfaces`, `Texgif`, `ch..root`, `canvasdraw` et `wrap`. D'où trois recommandations :

1. **WIMS complet sans WSL : Cygwin**, avec `wimsd` remplacé par Apache (Cygwin ou natif) en CGI. Le sandbox `ch..root` est désactivé ; on accepte la perte d'isolation, ou on fait tourner le service sous un compte restreint.
2. **Contenus OEF sous Windows :** l'option C + D de Claude (interpréteur Python WA-OEF2IPYNB, flydraw/SVG, CAS Windows natifs) reste la plus économique.
3. **Natif Win32 pur :** seulement si c'est un impératif. Il faut alors une couche `win32compat` couvrant processus, pipes avec select, timers, mémoire partagée et service, plus le remplacement de `ch..root`. Effort : 3 à 6 mois.

## 7. Prochaines étapes
1. Récupérer le tag correspondant exactement à 4.32 (`svn ls https://subversion.renater.fr/anonscm/svn/wimsdev/tags`) et compléter `public_html` (`svn update`).
2. Faire un prototype de compilation sous Cygwin (`./compile`) et noter les erreurs.
3. Choisir l'objectif : serveur complet ou contenus OEF.
4. Vérifier les scripts `public_html/scripts` et `bin/` une fois présents.

## 8. Non vérifié
- Les scripts de `public_html` (dossier vide dans le checkout).
- La correspondance exacte avec la 4.32 : le trunk analysé est en 4.33a.
- Toute compilation.
- Les occurrences de `random`/`strcasecmp` dans `Lib`.