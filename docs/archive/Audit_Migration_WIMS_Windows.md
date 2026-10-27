# Audit : migration de WIMS 4.32 vers Windows natif (sans WSL)

*Date : 23 octobre 2026 — auteur : Grok Bot pour Arnaud Lefrancois*

## 1. Contexte
L'objectif est d'étudier comment porter WIMS 4.32 sur Windows natif, sans WSL, et de lister les programmes et composants qui posent problème.

- Dépôt : `https://subversion.renater.fr/anonscm/svn/wimsdev/trunk`
- Copie locale prévue : `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims`

## 2. État de la récupération des sources
L'audit est **partiel**.

- `git svn clone -r HEAD https://subversion.renater.fr/anonscm/svn/wimsdev/trunk` a été coupé au bout de 12 minutes : le serveur Renater est très lent depuis la box.
- Une seconde tentative ciblée sur `trunk/wims-4.32/src` a été arrêtée après environ 15 minutes sans résultat.
- 101 fichiers ont été récupérés : 44 dans `src/` (fichiers `.c`/`.h` du cœur, configure, Makefile.in), 35 dans `bin/` et quelques autres.
- Les sous-dossiers `src/Wimslogd`, `Interfaces`, `Flydraw`, `Texgif` et `Module` sont **vides** dans ce clone partiel.
- La copie locale `D:\...\Reprise_Wims` était **vide** au moment de l'audit : aucune comparaison n'a été possible.
- Aucun `svn relocate` n'a été fait et rien n'a été modifié.

Le tableau ci-dessous repose donc sur ces fichiers et, pour les sous-dossiers, sur la connaissance de l'architecture de WIMS.

## 3. Stratégie recommandée
1. **Le plus réaliste : MSYS2 ou Cygwin.** WIMS repose sur `fork`/`exec`, `SIGALRM`, `setrlimit`, les sockets Unix et Apache CGI. Cygwin émule tout cela presque sans modifier le code (fork lent, mais fonctionnel). MSYS2/MinGW natif demande au contraire une réécriture de la couche processus.
2. **Pour du « vrai » natif Win32 / MinGW,** il faut une couche d'abstraction maison (`win32compat.c`) :
   - `CreateProcess` à la place de `fork`/`exec` ;
   - Job Objects à la place de `setrlimit`/`alarm`/`kill` ;
   - socket TCP sur 127.0.0.1 ou named pipe à la place de `AF_UNIX` ;
   - service Windows à la place du daemon `wimslogd`.

   Il faut aussi réécrire en Perl ou PowerShell les scripts shell d'exploitation.
3. **Logiciels externes :** PARI/GP, Maxima, Octave, gnuplot, POV-Ray, ImageMagick et TeX (MiKTeX/TeX Live) existent tous en version Windows. Ce ne sont pas eux qui bloquent : le blocage vient de la façon dont WIMS les appelle.

## 4. Composants problématiques
| Composant | Fichier(s) | Problème sous Windows | Gravité | Piste |
|---|---|---|---|---|
| Lancement des programmes externes | `lines.c:927-972` (`fork`, `execve`/`execvp`, `waitpid`) | Pas de `fork` sous Win32 | Bloquant | `CreateProcess` + `WaitForSingleObject`, ou Cygwin |
| Rendu MathML | `mathml.c:42,132` (`fork`, `pipe`, `execv("../bin/wims_mathml")`) | Même problème, plus des pipes | Bloquant | `CreateProcess` avec redirection des handles (`CreatePipe`) |
| Délais d'exécution | `lines.c:853-877` (`signal(SIGALRM)`, `alarm`), `wims.c` (`initalarm`) | Pas de `SIGALRM` | Bloquant | Timer thread ou Job Object avec limite de temps |
| Limites de ressources | `setrlimit(RLIMIT_CPU)` dans `wims.c:591`, `auth.c:71`, `config.c:509` | `setrlimit` absent | Moyen | Job Objects (`JOB_OBJECT_LIMIT_PROCESS_TIME`/`MEMORY`) |
| Arrêt des processus | `cleaning.c:31`, `lines.c:843` (`kill(pid, SIGKILL)`) | Pas de `kill` | Moyen | `TerminateProcess` |
| Communication avec le daemon | `lines.c:1346-1355`, `wims.h:357` (`socket(PF_UNIX)`, `sockaddr_un`) | `AF_UNIX` seulement partiel (Windows 10 1803+) ; non testé | Bloquant à moyen | TCP sur localhost ou named pipe |
| Daemon `wimslogd` | `src/Wimslogd/` (non vérifié) ; `log.c:446-464` (double `fork` puis `execve`) | Pas de daemonisation ni de setsid | Bloquant | Service Windows (NSSM ou API service) |
| Mémoire partagée | `wims.c` / `wimsshm.h` (`shmget`/`shmat`/`mmap`) | System V IPC absente | Bloquant | `CreateFileMapping`/`MapViewOfFile`, ou Cygwin (cygserver) |
| Liens symboliques | `calc.c:1964` (`symlink`), `files.c`/`variables.c` (`lstat`/`readlink`) | Droits admin ou mode développeur requis | Moyen | Copie de fichiers ou `CreateSymbolicLink` avec repli |
| Permissions et uid | `wims.c`, `log.c`, `exec.c`, `cleaning.c` (`chmod`, `umask`, `getuid`) ; `bin/wrapuid`, `bin/setwrapexec`, `bin/changepermissions` | Modèle uid/gid et setuid sans équivalent | Moyen | Supprimer ou neutraliser ; ACL NTFS ; compte de service dédié |
| Sandbox | `bin/setchroot`, wrapper `ch..root` dans `Interfaces` (non vérifié) | Pas de `chroot` | Bloquant pour la sécurité | AppContainer, Job Object ou utilisateur restreint ; sinon pas de sandbox |
| Chemins en dur | `wims.h`, `config.c`, `exec.c`, `variables.c`, `log.c` (`/bin/sh`, `/usr/…`, `/tmp/`, séparateur `/`) | Chemins absents sous Windows | Moyen | Variables de configuration, `GetTempPath` |
| Appels shell | `exec.c`, `lines.c` (`popen`/`system`), `wims_modules.pl` | Dépendance à `sh` | Moyen | `_popen` ou shell MSYS2 embarqué |
| Verrous de fichiers | `includes.h`, `configure.ac` (`flock`/`fcntl`) | Absents en MSVC/MinGW | Moyen | `LockFileEx` |
| Build | `configure`, `configure.ac`, `Makefile.in`, `src/compile`, `define.sh`, `mkmathfonts` | Autotools et sh | Moyen | Build sous MSYS2 (gcc MinGW-w64) ; à terme CMake |
| Scripts d'exploitation | `bin/` : `housekeep.daily`, `backup`, `modupdate`, `update`, `apache-config`, `server-interrupt`, `server-resume`, `oefcompile`, `mkindex`, `account.sh`… ; `update.sh` | Scripts sh/bash, utilitaires coreutils, cron | Moyen | Planificateur de tâches + bash MSYS2, ou réécriture PowerShell/Perl |
| Scripts Perl | `css.pl`, `oefdoc.pl`, `rss.pl`, `refer.pl`, `wims_modules.pl` | Plutôt portables (chemins, `system`) | Faible | Strawberry Perl |
| Apache CGI | `bin/apache-config` | Configuration Unix | Faible | Apache Lounge ou IIS CGI ; `wims.cgi` compilé en `.exe` |
| Interfaces vers pari, maxima, octave, gnuplot, povray, ImageMagick, TeX | `src/Interfaces`, `public_html/bin/ch..*` (non vérifié) | Wrappers sh, pipes, timeout | Moyen | Versions Windows natives des logiciels + wrappers réécrits |
| Flydraw, Texgif | `src/Flydraw`, `src/Texgif` (non vérifié) | Probablement du C portable (libgd) ; Texgif appelle TeX/dvi | Faible à moyen | Compilation MinGW avec libgd ; MiKTeX |

## 5. Synthèse
Les points bloquants sont tous dans le cœur `wims.cgi` et dans `wimslogd` : `fork`/`exec`, `SIGALRM`, sockets Unix, mémoire partagée System V et daemon. Un portage natif demande donc une vraie couche d'abstraction des processus, de l'ordre de plusieurs semaines. Cygwin donne la voie la plus courte vers un WIMS qui tourne sous Windows sans WSL. MinGW natif n'a de sens que si un portage durable est voulu.

## 6. Prochaines étapes
1. **Mettre les sources sur la machine locale.** Lancer `git svn clone -r HEAD https://subversion.renater.fr/anonscm/svn/wimsdev/trunk` en local (l'accès à Renater y est sans doute plus rapide que depuis la box), ou faire un `svn export` de `trunk/wims-4.32`.
2. **Vérifier les sous-dossiers manquants** : `Wimslogd`, `Interfaces` et le wrapper `ch..root`, `Flydraw`, `Texgif`, `Module`, ainsi que les scripts de `public_html/scripts`.
3. **Prototype Cygwin** : `./compile` avec Apache Cygwin, puis noter les échecs.
4. **En parallèle, écrire la couche `win32compat`** (processus, timers, IPC) si le natif MinGW est retenu.

## 7. Non vérifié
- Les sous-dossiers de `src` et les scripts de `public_html`.
- La compilation réelle : rien n'a été compilé.
- L'existence de la révision 4.32 exacte comme tag : `trunk/wims-4.32` au HEAD a été utilisé.
- Pourquoi `D:\...\Reprise_Wims` est vide (mauvais chemin ? sources jamais récupérées ?).
