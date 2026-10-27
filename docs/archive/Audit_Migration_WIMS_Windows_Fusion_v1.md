# Audit fusionné : migration de WIMS 4.32 vers Windows natif (sans WSL)

*Date : 23 octobre 2026. Fusion de l'audit Grok (`Audit_Migration_WIMS_Windows.md`) et de l'audit Claude (`Reprise_Wims_Claude\Audit_Migration_WIMS_Windows_CLAUDE_V1.md`, r19040), avec un recoupement sur `D:\ALF_ZGit\43-wims-paris-saclay_fr\wims-4.32`.*

## 1. Contexte et état des sources
- **Dépôt :** `https://subversion.renater.fr/anonscm/svn/wimsdev/trunk`.
- **Grok :** clone `git svn` partiel (101 fichiers). Seul le cœur `src/*.c` et `bin/` ont été analysés.
- **Claude :** annonce r19040 et l'analyse de `Wimsd`, `Interfaces/common.c`, `Misc`, `Lib`. **Aucune copie de ces sources n'est présente** dans `Reprise_Wims_Claude`, qui ne contient que le rapport.
- **Recoupement local :** dans `wims-4.32\src`, les sous-dossiers `Wimsd`, `Wimslogd`, `Interfaces`, `Flydraw`, `Texgif`, `Lib` et `Misc\*` sont **vides**. Seuls les fichiers C du cœur sont présents. Les affirmations sur ces sous-dossiers restent donc **non vérifiées**, quelle que soit la source.

## 2. Stratégie consolidée
Trois options sont possibles, selon le but recherché :

1. **Faire tourner un serveur WIMS complet sous Windows le plus vite possible : Cygwin (ou MSYS2).** Le code C reste quasi inchangé, au prix d'un runtime POSIX et d'un `fork` lent. Effort : 2 à 4 semaines (estimation Claude, cohérente avec Grok).
2. **Windows natif pur (MSVC ou MinGW + Win32) :**
   - couche `win32compat` : `CreateProcess`/`CreatePipe` à la place de `fork`/`exec`/`pipe`/`dup2`, Job Objects pour `setrlimit`/`alarm`/`kill`/`chroot`, TCP ou named pipe à la place de `AF_UNIX`, `CreateFileMapping` pour la mémoire partagée POSIX, service Windows pour `wimsd`/`wimslogd` ;
   - build CMake ;
   - scripts réécrits en Python ou PowerShell.

   Effort : 3 à 6 mois (Claude). Grok parlait de « plusieurs semaines » : l'estimation de Claude est plus réaliste au vu de l'inventaire complet.
3. **Portage partiel (options C + D de Claude, absentes chez Grok) :** on ne porte pas `wimsd`/`wims`. On continue l'interpréteur OEF Python (WA-OEF2IPYNB : MathJax, MathLive, SVG, notation côté client), on porte flydraw/texgif et les outils `Misc`, et on utilise Maxima/PARI/Octave en natif Windows.

**Recommandation fusionnée :**
- Si l'objectif est un WIMS complet sans WSL : **prototype Cygwin d'abord**, pour valider vite et mesurer les écarts.
- Si l'objectif est d'exploiter les contenus OEF sous Windows : **option C + D de Claude**.
- Le natif Win32 complet ne se justifie que pour un besoin durable de serveur Windows sans couche POSIX.

## 3. Tableau unifié des composants problématiques
Légende de la colonne « Vérif. locale » : ✔ trouvé dans `wims-4.32\src` ; ✖ contredit ; ? sous-dossier absent.

| Composant | Fichier(s) | Problème sous Windows | Gravité | Piste | Source | Vérif. locale |
|---|---|---|---|---|---|---|
| Serveur HTTP `wimsd` | `src/Wimsd/wimsd.c` | fork/exec/dup2/accept par requête ; setreuid | Bloquant | Apache/IIS + CGI, ou réécriture CreateProcess + Winsock | Claude | ? |
| Lancement des programmes externes (cœur) | `lines.c:927-972`, `exec.c` ; selon Claude aussi `Lib/libwims.c`, `Interfaces/common.c` (`execredirected`) | fork, execve/execvp, waitpid, pipe, dup2, select | Bloquant | CreateProcess + CreatePipe + WaitForSingleObject, ou Cygwin | Les deux | ✔ lines.c, log.c, mathml.c |
| Rendu MathML | `mathml.c:42,132` | fork + pipe + execv `wims_mathml` | Bloquant | CreateProcess avec redirection | Grok | ✔ |
| Délais d'exécution | `lines.c:853-877`, `wims.c`, `config.c` | SIGALRM / alarm | Bloquant | Timer thread / Job Object | Les deux | ✔ |
| Limites CPU | `wims.c:591`, `auth.c:71`, `config.c:509` | setrlimit | Moyen (Grok) / bloquant implicite (Claude) | Job Objects | Les deux | ✔ |
| Arrêt des processus | `cleaning.c:31`, `lines.c:843` | kill SIGKILL | Moyen | TerminateProcess | Les deux | ✔ |
| Changement d'identité | `lines.c`, `log.c` (setreuid/setregid) | Pas d'uid/gid | Moyen | Compte de service dédié, ACL NTFS | Claude (Grok via chmod/uid) | ✔ |
| Daemon `wimslogd` + socket Unix | `src/Wimslogd/`, `lines.c:1346-1355`, `log.c`, `wims.h:357` | PF_UNIX, double fork, setsid | Bloquant | Service Windows + TCP localhost / named pipe | Grok | ✔ (cœur) / ? (Wimslogd) |
| Mémoire partagée | `wimsshm.h` (`SHM_NAME "/wims.shm"`) | Mémoire partagée **POSIX** (shm_open/mmap), pas System V | Bloquant | CreateFileMapping/MapViewOfFile | Grok | ✔ (corrigé : POSIX, pas SysV) |
| Socket réseau / `/proc/net/tcp` | `wims.c:553` | `/proc` absent | Moyen | GetExtendedTcpTable | Claude | ✔ |
| Interfaces CAS (maxima, gap, octave, pari, scilab, yacas) | `src/Interfaces/*.c`, `public_html/bin/ch..*` | Pipes bidirectionnels, wrappers sh | Bloquant (Claude) / moyen (Grok) | Binaires CAS Windows + wrappers réécrits | Les deux | ? |
| Sandbox chroot | `bin/setchroot`, `ch..root`, `variables.c` | Pas de chroot | Bloquant (sécurité) | AppContainer / Job Object / compte restreint | Les deux | ✔ (référence dans variables.c, wimsshm.h) |
| Liens symboliques | `calc.c:1964`, `files.c`, `variables.c`, configure | symlink/lstat/readlink | Moyen | Copie ou CreateSymbolicLink / jonctions | Les deux | ✔ |
| Permissions | `wims.c`, `log.c`, `exec.c`, `cleaning.c` ; `bin/wrapuid`, `setwrapexec`, `changepermissions` | chmod/umask/getuid | Moyen | _umask partiel, ACL | Les deux | ✔ |
| Chemins en dur | `wims.h`, `config.c`, `exec.c`, `variables.c`, `log.c` ; texgif, Misc | `/bin/sh`, `/usr`, `/tmp` | Moyen | Configuration, %TEMP% | Les deux | ✔ |
| Appels shell | `exec.c`, `lines.c`, `wims_modules.pl` | popen/system → sh | Moyen | _popen ou sh embarqué | Grok | ✔ |
| Envoi de mail | `exec.c` | sendmail | Difficile | SMTP (blat, bibliothèque SMTP) | Claude | ✔ |
| Authentification | `calc.c` (`crypt()`) | libcrypt absente | Moyen | bcrypt.lib / OpenSSL | Claude | ✔ |
| Verrous de fichiers | `includes.h`, `configure.ac` | flock/fcntl | Moyen | LockFileEx | Grok | ✔ |
| Compatibilité libc | `src/Lib/*.c`, partout | random/srandom, strcasecmp, setenv, unlink | Moyen | Macros `#ifdef _WIN32` | Claude | ? |
| Build | `configure.ac`, `Makefile.in`, `compile`, `define.sh`, `mkmathfonts` | Autotools + sh | Moyen (Grok) / difficile (Claude) | MSYS2 à court terme, CMake à terme | Les deux | ✔ |
| Scripts d'exploitation | `bin/` : oefcompile, src2def, mkindex, modupdate, housekeep.daily, backup, account.sh, apache-config…, `update.sh` | sh + coreutils + cron | Moyen (Grok) / bloquant (Claude) | Bash MSYS2 + Planificateur de tâches, ou réécriture Python/PowerShell | Les deux | ✔ |
| flydraw | `src/Flydraw` | libgd, autotools, strcasecmp | Difficile (Claude) / faible-moyen (Grok) | libgd via vcpkg + CMake, ou SVG | Les deux | ? |
| texgif | `src/Texgif` | Pipeline TeX/DVI, /tmp | Difficile | MiKTeX, %TEMP% | Les deux | ? |
| Misc/chemeq, units-filter | `src/Misc/...` | flex + g++ | Difficile / moyen | win_flex, MinGW | Claude | ? (dossiers vides) |
| Misc/canvasdraw | `src/Misc/canvasdraw` | Scripts doc sh | Difficile (doc) | Python | Claude | ? |
| Autres outils Misc (symtext, crossword, checkmol, lceb, voronoiW, whirlgif…) | `src/Misc/*` | C quasi pur, /tmp, strcasecmp | Moyen | MSVC/MinGW | Claude | ? |
| Apache CGI | `bin/apache-config` | Configuration Unix | Faible | Apache Lounge / IIS | Grok | ✔ |
| Scripts Perl | css.pl, refer.pl, oefdoc.pl, rss.pl, wims_modules.pl | Chemins | Faible | Strawberry Perl | Les deux | ✔ |
| Logiciels externes (Maxima, PARI, Octave, GAP, gnuplot, POV-Ray, ImageMagick, TeX, wget) | — | Versions Windows existantes (GAP plus délicat) | Faible à moyen | Installeurs officiels | Les deux | — |
| Front-end (MathJax, JSmol, GeoGebra, public_html, contenus .oef/.def) | — | Aucun problème | Aucune | — | Claude | — |

## 4. Divergences entre les deux audits
1. **Gravité des scripts shell et des interfaces CAS :** bloquante pour Claude, moyenne pour Grok. Avec Cygwin/MSYS2, ils tournent tels quels, donc « moyen » ; sans couche POSIX, ils sont bloquants. Les deux avis se valent, selon l'option choisie.
2. **Effort du natif complet :** 3 à 6 mois pour Claude, « plusieurs semaines » pour Grok. **On retient l'estimation de Claude.**
3. **Stratégie :** Grok recommande Cygwin pour obtenir un WIMS complet. Claude recommande de ne pas porter le moteur et de poursuivre l'interpréteur OEF Python (C + D). Ces deux recommandations répondent à deux objectifs différents, que l'utilisateur doit trancher.
4. **Mémoire partagée :** Grok la décrivait comme System V. La vérification locale de `wimsshm.h` (`SHM_NAME "/wims.shm"`) montre de la **mémoire partagée POSIX**. Grok est corrigé ; Claude ne la mentionnait pas.
5. **Localisation de `fork` :** Claude le situe dans `wimsd.c` / `Interfaces/common.c`. Localement, `fork` est confirmé dans `lines.c`, `log.c` et `mathml.c`, et les sous-dossiers ne sont pas vérifiables.
6. **Oublis de chaque audit :**
   - Claude omet `wimslogd`/`AF_UNIX`, MathML, la mémoire partagée, flock et popen.
   - Grok omet `wimsd`, `/proc/net/tcp`, sendmail, `crypt`, `select`, setreuid, les outils `Misc` et la compatibilité libc.

## 5. Apports de Claude
- Le serveur `wimsd`.
- L'inventaire de `Misc` (chemeq, canvasdraw, units-filter…).
- `/proc/net/tcp`, sendmail, libcrypt.
- Le tableau des équivalents POSIX → Win32.
- Le tableau des dépendances externes (GAP, flex, libgd via vcpkg).
- Les options C/D (portage partiel, WA-OEF2IPYNB).
- Les estimations d'effort et un schéma d'architecture.

## 6. Prochaines étapes
1. **Récupérer les sous-dossiers manquants** dans `wims-4.32\src` (`git svn clone` ou `svn export` de `trunk/wims-4.32/src`) et re-vérifier les lignes marquées « ? ».
2. **Décider de l'objectif :** serveur WIMS complet, ou exploitation des contenus OEF.
3. **Si serveur complet :** prototype Cygwin (`./compile` + Apache), puis journal des échecs.
4. **Si contenus OEF :** poursuivre WA-OEF2IPYNB, porter flydraw (ou passer en SVG) et texgif (MiKTeX).
5. **Dans les deux cas :** réécrire `src2def`, `mkindex` et `oefcompile` en Python.

## 7. Non vérifié
- Le contenu de `Wimsd`, `Wimslogd`, `Interfaces`, `Flydraw`, `Texgif`, `Lib` et `Misc` : vides localement et non récupérés.
- Toute compilation réelle : rien n'a été compilé.
- La révision r19040 citée par Claude.