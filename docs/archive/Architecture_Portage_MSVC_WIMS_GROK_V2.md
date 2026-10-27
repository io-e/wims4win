# Architecture détaillée : portage natif MSVC de WIMS — V2 (révision Grok)

**Date :** 23 octobre 2026
**Base :** `Reprise_Wims_Claude\Architecture_Portage_MSVC_WIMS_CLAUDE_V1.md` (1 453 lignes), validée point par point contre le checkout `Reprise_Wims_Claude\wims`.
**Sources :** `trunk/wims` **r19040**, fichier `version` = **4.33a** (et non 4.32, voir §0.3).
**Périmètre :** portage du serveur WIMS sur Windows natif (MSVC + API Win32), sans WSL ni Cygwin à l'exécution, à part un `sh.exe` embarqué.
**Estimation révisée :** **6 à 7,5 mois** pour 1 développeur expérimenté, **4 à 5 mois** à deux (voir §11).

---

## 0. Validation de l'architecture Claude V1

### 0.1 Verdict global
**L'architecture de Claude est bonne sur le fond et bien plus solide que son étude précédente.** Elle intègre la plupart des corrections déjà signalées :
- `wimsd` supprimé au profit d'Apache en CGI ;
- mmap au lieu de shmget ;
- named pipes overlapped pour les CAS ;
- `sh.exe` MSYS2 pour `call_sh` ;
- service Windows pour `wimslogd` ;
- les 4 `execredirected` ;
- canvasdraw/OpenBabel ;
- le filtrage Octave mentionné.

Il reste **des erreurs techniques ponctuelles, mais importantes** : handlers d'alarme exécutés dans un thread, héritage de handles global, risque d'interblocage en écriture, mappage nommé non global, sandbox insuffisant. Il y a aussi **des oublis de build** (flex/bison, msg2wims, outils Misc manquants) et **un chiffrage encore optimiste** sur les tests, la sécurité et les scripts.

### 0.2 Tableau de validation point par point
Légende : ✅ validé ; ✏️ corrigé ; ➕ ajouté.

| # | Point (section Claude) | Verdict | Justification (checkout r19040) |
|---|---|---|---|
| 1 | §1.3 `wimsd` supprimé, Apache CGI | ✅ Validé | `Wimsd/wimsd.c:206` fork puis `:275` execl de wims, sans état ; socket AF_INET `:141` |
| 2 | §1.3 fork+exec → CreateProcess | ✅ Validé | Seule voie en MSVC |
| 3 | §1.3 select sur pipes → overlapped (named pipes) | ✅ Validé, ✏️ compléter | `Interfaces/common.c:494,519,549`, `calc.c:242,255`. Il faut aussi rendre **l'écriture** non bloquante (voir #14) |
| 4 | §1.3 mmap → CreateFileMapping | ✅ Validé, ✏️ corrigé | Seul mmap actif : `Wimslogd/wimslogd.c:141`. Celui de `wims.c:1222-1224` est **commenté**. Le nom du mappage doit être `Global\...` (voir #16) |
| 5 | §1.3 SIGALRM → timer threads | ✏️ Corrigé | `lines.c:837-847` : `alarm1`/`alarm2` appellent `kill` puis `module_error("timeup")`, qui sort ou saute (longjmp) vers la boucle principale. **Interdit depuis un autre thread.** Voir §3.5 |
| 6 | §1.3 chroot → Job Objects | ✏️ Corrigé | Un Job Object **n'isole pas le système de fichiers**. `ch..root` (`Misc/chroot.c:235`) confine aussi les écritures à `../chroot` et `/tmp/sessions` (`:242`). Il faut un jeton restreint ou AppContainer et des ACL. Voir §3.3 |
| 7 | §1.3 setreuid → supprimé avec avertissement | ✏️ Corrigé | `Interfaces/common.c:201`, `Texgif/basic.c:88`, `texgif.c:127`, `wimslogd.c:169-170`. Remplacer par **un compte de service dédié et un jeton restreint pour les enfants**, pas par un no-op |
| 8 | §1.3 `call_sh` → sh.exe MSYS2 | ✅ Validé | `calc.c:1181` (`&>`), `Texgif/basic.c:115` `execredirected("sh",…)`, `wims.c:954`, `Wimslogd/log.c:89`. ➕ sh.exe dépend du runtime msys-2.0.dll : il faut aussi coreutils (rm, cp, ls, cat…) et une conversion des chemins |
| 9 | §1.3 socket Unix de wimslogd → named pipe | ✅ Validé, ✏️ compléter | `Wimslogd/socket.c:31-33,85` ; client `lines.c:1346-1364`. Le **client** utilise aussi `select()` sur la socket (S7) : avec un named pipe, ce `select` ne fonctionne plus. Il faut l'adapter (attente overlapped) |
| 10 | §2.2 « 10 sites fork() » | ✅ Validé, ➕ compléter | Liste exacte. Il faut ajouter `Misc/chroot.c:257` (exec), `Misc/wrap.c:34,38` (exec), `Misc/webget.c:117` et `Misc/clickzone.c:115` (`system`), ainsi que les **popen/system** du cœur (`exec.c`, `lines.c`) |
| 11 | §2.3 select : S1-S4 problématiques, S5-S7 OK | ✏️ Corrigé | S5-S7 ne sont « OK » que si l'on garde des sockets. Avec la décision « named pipe » (#9), S5-S7 doivent changer aussi. Alternative plus simple : **garder AF_UNIX** (pris en charge par Winsock depuis Windows 10 1803) ou TCP 127.0.0.1, ce qui préserve `select` |
| 12 | §2.4 `call_sh` : inventaire de 7 appels | ✅ Validé (échantillon) | `exec.c:1803-1812` (sendmail) confirmé. ➕ L'inventaire n'est pas exhaustif : il faut un `rg call_sh` complet, ainsi que les scripts de `public_html` (vide ici) |
| 13 | §3.2 Variante 1 `win32_exec_redirect` | ✏️ Corrigé | `bInheritHandles=TRUE` fait hériter **tous** les handles héritables du parent (pipes d'autres enfants, fichiers) : fuites et blocages. Il faut utiliser `PROC_THREAD_ATTRIBUTE_HANDLE_LIST` (STARTUPINFOEX). Il faut aussi gérer le chemin de l'exécutable : `execvp` cherche dans le PATH, `CreateProcess` aussi, mais pas `.bat` (`maxima.bat` exige `cmd /c`) |
| 14 | §3.2 Variante 2 `win32_exec_cas` | ✏️ Corrigé | Le stdin reste un **pipe anonyme en écriture bloquante**. Si le CAS remplit sa sortie pendant que le parent écrit, c'est l'**interblocage**. Les deux sens doivent être en overlapped, ou l'écriture confiée à un thread. Il manque aussi `ConnectNamedPipe` / `ERROR_PIPE_CONNECTED` et un nom de pipe aléatoire (anti-squatting) |
| 15 | §3.2 Variante 3 `win32_exec_pipe` | ✏️ Corrigé | Écrire tout, puis lire tout, puis attendre : **pas de timeout effectif** (ReadFile bloque avant `WaitForSingleObject`) et interblocage si l'entrée dépasse 4 Ko pendant que l'enfant écrit. Il faut un thread d'écriture, un timeout via le Job Object ou CancelSynchronousIo, et le message d'erreur `canvas_error` (`canvasdraw.c:6339`) |
| 16 | §3.4 `mmap.c` | ✏️ Corrigé | Le nom fixe `"WimsSharedMem"` est créé dans l'espace de **session**. Or `wimslogd` (service, session 0) et wims.exe (sous Apache) doivent partager le mappage. Il faut `Global\WimsShm`, le privilège `SeCreateGlobalPrivilege` et un descripteur de sécurité explicite. Un nom unique pour tous les mmap provoque des collisions |
| 17 | §3.3 `win32_restrict_dir` en TODO + avertissement | ✏️ Corrigé (bloquant prod) | Le sandbox est un **livrable**, pas un TODO. Voir §3.3 V2 |
| 18 | §3.5 `win32_kill` | ✅ Validé, ✏️ réserve | OpenProcess(pid) expose au **recyclage de PID**. Il vaut mieux conserver les HANDLE des enfants (`Wimslogd/fork.c` `addfork` stocke déjà les pid) |
| 19 | §3.6 service Windows | ✅ Validé | Remplace `log.c:446-448` et `wimslogd.c` ; NSSM est une alternative valable. ➕ À ajouter : `/proc/loadavg` (`wimslogd.c:193`) → `GetSystemTimes` / compteur de charge |
| 20 | §4.1 CMake : wims.exe | ✏️ Corrigé | `auth.c` est cité pour libcrypt, mais `crypt()` est dans **`calc.c:2007-2020`**, sous `#ifdef HAVE_CRYPT` (déjà optionnel). Il suffit de **ne pas définir HAVE_CRYPT** ou de fournir une implémentation compatible des hachages existants (compatibilité des mots de passe stockés !) |
| 21 | §4.1 CMake : Interfaces | ✏️ Corrigé | Noms des exécutables : le Makefile produit `m2`, `octave`… (et non `wims_m2`). Il faut les garder pour ne pas casser les appels |
| 22 | §4.1 CMake : OefMsg2wims | ✏️ Corrigé | Ce sont **deux programmes** (`oef2wims` et `msg2wims`, chacun avec son `main`), pas un seul exécutable |
| 23 | §4.1 CMake : outils Misc | ➕ Ajouté | Il manque `suffix.c`, `translator.c`, `chroot.c` et les sous-dossiers `chemeq`, `units-filter` (**flex + bison**, `.l/.y`), `symtext`, `mathexp`, `voronoiW`, `whirlgif`, `bioinfo`, `lceb`, `checkmol`, `crossword`, `sigdigits`, `moneyprint`, `scienceprint`, `toascii`, `applets` |
| 24 | §4.2 vcpkg | ➕ Ajouté | Ajouter `winflexbison` (outil) ; OpenSSL seulement si crypt est réimplémenté |
| 25 | §5.1 `wims.c` : mmap → win32_mmap | ✏️ Corrigé | mmap commenté (`wims.c:1222-1224`). Reste `/proc/net/tcp` (`wims.c:553`) → GetExtendedTcpTable |
| 26 | §6.1 `octave.c` filtrage | ✅ Validé, ➕ renforcé | `Interfaces/octave.c:73-77` (system, popen, fopen, mkdir…). À conserver et **étendre aux équivalents Windows** (`dos`, `winqueryreg`, `system` via `!`) |
| 27 | §6.2 `maxima.bat` | ✏️ Corrigé | Un `.bat` passe par cmd.exe : les arguments sont réinterprétés (injection) et TerminateProcess ne tue pas l'enfant SBCL/GCL. Il faut lancer directement le binaire Lisp de Maxima, avec Job Object en kill-on-close |
| 28 | §7 Texgif | ✅ Validé | `Texgif/basic.c:72-115`, `tfm.c:48`, `gf.c:167,235` ; MiKTeX |
| 29 | §7 canvasdraw / OpenBabel | ✅ Validé | `canvasdraw.c:6330-6382` |
| 30 | §7 `chroot.c` « remplacer ou supprimer, 1 jour » | ✏️ Corrigé | `chroot.c` gère aussi `cleantmpdir` (`cleaning.c:36`, `Wimslogd/cleaning.c:65`), `kill` (`:153`) et les limites FSIZE/NPROC (`:207-238`). Il faut un `ch..root.exe` Windows (lanceur sandboxé + nettoyage). 4-5 jours |
| 31 | §8 wimslogd / housekeep → schtasks | ✅ Validé | `Wimslogd/housekeep.c` |
| 32 | §9 scripts → PowerShell | ✏️ Nuancé | Puisque sh.exe MSYS2 est de toute façon embarqué (#8), **garder les scripts sh** pour la V1 et ne réécrire que l'indispensable. Gain de 2 à 3 semaines |
| 33 | §10 plan de test | ✅ Validé, ➕ compléter | Ajouter la **non-régression** (diff des sorties HTML Linux/Windows sur un corpus de modules), les tests de sécurité (évasion du sandbox CAS) et la charge (30 utilisateurs) |
| 34 | §11 chiffrage 5,5 mois | ✏️ Corrigé | Voir §11 V2 : 6 à 7,5 mois |
| 35 | En-tête « WIMS 4.32 » | ✏️ Corrigé | Le code analysé est en **4.33a** (r19040 trunk). Il faut identifier le tag 4.32 ou assumer le trunk |
| 36 | `sendmail` → msmtp | ✅ Validé | `exec.c:1803-1812` |
| 37 | Encodage et chemins | ➕ Ajouté | ANSI vs UTF-8 (`CreateFileA`, `fopen`) : fichiers de modules en UTF-8, chemins avec accents. Il faut `/utf-8` et le manifeste `activeCodePage=UTF-8`. Les chemins `../chroot`, `../bin/wims_mathml` (`mathml.c:132`) sont relatifs au répertoire courant de wims.exe |
| 38 | Fins de ligne et mode texte | ➕ Ajouté | Sous MSVC, `fopen` sans `b` convertit CRLF et décale `fseek`/`ftell`. Il faut forcer `_O_BINARY` (`_set_fmode`) |
| 39 | Apache CGI | ✅ Validé, ➕ compléter | Configurer `ScriptInterpreterSource` (ou `.exe` direct), les variables d'environnement transmises et le timeout CGI d'Apache, cohérent avec `WIMS_TIMEOUT` |
| 40 | Licence et amont | ➕ Ajouté | WIMS est sous GPL ; prévoir de **reverser les `#ifdef _WIN32` à l'équipe wimsdev** pour éviter une divergence |

### 0.3 Remarques préalables
- **Version :** r19040 = 4.33a. Avant de démarrer, lancer `svn ls https://subversion.renater.fr/anonscm/svn/wimsdev/tags`, puis décider : porter le trunk (recommandé, puisque le portage sera reversé) ou un tag 4.32.
- **`public_html` est vide dans le checkout** : les scripts `public_html/scripts`, les modules et le contenu ne sont pas analysés. Faire un `svn update`, puis inventorier.

---

## 1. Vue d'ensemble de l'architecture cible

### 1.1 Architecture Linux actuelle (inchangée, validée)
```
Navigateur ──HTTP──► wimsd (fork par requête) ou Apache CGI
                  │ fork() + execl()
                  ▼
                  wims (moteur C) ── socket AF_UNIX ──► wimslogd (daemon : mmap, fork, cron interne)
                  │ fork() + exec + pipes/select ; call_sh → /bin/sh
     ┌──────────┬──────────┬───────────────┬──────────────┬──────────────┐
  flydraw    texgif      Interfaces       canvasdraw      ch..root (chroot + rlimit)
  (libgd)    (TeX, sh)   maxima pari gap  (wims_mathml,   └─► CAS sandboxés
                         octave scilab    obabel)
                         yacas m2
```

### 1.2 Architecture Windows cible (corrigée)
```
Navigateur ──HTTP──► Apache Win64 (mod_cgi) — service, compte « wims-svc »
                  │ CreateProcess
                  ▼
                  wims.exe (MSVC, /utf-8, _O_BINARY)
                  │  ├─ IPC wimslogd : AF_UNIX Winsock (Win10 1803+) ou TCP 127.0.0.1  ← garde select()
                  │  ├─ Global\WimsShm (lecture)
                  │  ├─ win32_spawn() : STARTUPINFOEX + HANDLE_LIST + Job Object
                  │  └─ call_sh → wims\msys\usr\bin\sh.exe (+ coreutils)
     ┌──────────┬──────────┬───────────────┬──────────────┬──────────────────────┐
  flydraw    texgif      Interfaces       canvasdraw     ch..root.exe (nouveau)
  libgd      MiKTeX      I/O overlapped   obabel.exe     = jeton restreint/AppContainer
  (vcpkg)               2 sens + timeout                 + Job Object + ACL ../chroot/tmp
                                                          + cleantmpdir
wimslogd.exe — service Windows (SCM/NSSM), crée Global\WimsShm, GetSystemTimes (≈ loadavg)
```

### 1.3 Décisions d'architecture (révisées)
| Décision | Choix V2 | Changement / justification |
|---|---|---|
| wimsd | Supprimé, Apache CGI | = Claude |
| fork+exec | `win32_spawn()` unique (STARTUPINFOEX + `PROC_THREAD_ATTRIBUTE_HANDLE_LIST` + Job Object) | ✏️ Évite l'héritage global des handles |
| Pipes CAS | Overlapped **dans les deux sens**, ou thread d'écriture | ✏️ Évite l'interblocage en écriture |
| IPC wimslogd | **AF_UNIX Winsock** (ou TCP localhost authentifié) | ✏️ Conserve `select()` côté client et serveur ; moins de code que les named pipes |
| Mémoire partagée | `CreateFileMapping("Global\\WimsShm")` + descripteur de sécurité | ✏️ Session 0 ↔ Apache |
| SIGALRM | Thread de surveillance qui **termine l'enfant** ; le thread principal constate le timeout à son retour d'attente puis appelle `module_error` | ✏️ Pas de longjmp inter-thread |
| Sandbox | `ch..root.exe` : jeton restreint (CreateRestrictedToken) ou AppContainer + Job Object (CPU, mémoire, ActiveProcessLimit, kill-on-close) + ACL | ✏️ Le Job seul est insuffisant |
| setreuid | Compte de service dédié + jeton restreint pour les enfants | ✏️ Pas de no-op silencieux |
| Shell | sh.exe MSYS2 + coreutils, embarqués | = Claude, ➕ dépendances |
| wimslogd | Service Windows | = Claude |
| Build | CMake + vcpkg + winflexbison | ➕ flex/bison |
| crypt | Ne pas définir `HAVE_CRYPT` ou fournir un crypt compatible | ✏️ Optionnel dans le code |

---

## 2. Cartographie des sources r19040 (corrections)
La cartographie de Claude (§2.1) est **validée** dans ses grandes lignes. Corrections :
- `auth.c` n'utilise pas libcrypt : c'est **`calc.c:2007-2020`** (`#ifdef HAVE_CRYPT`).
- `OefMsg2wims` produit 2 exécutables (`oef2wims`, `msg2wims`).
- `Misc/*.c` (23 fichiers) : ajouter `suffix.c`, `translator.c` (et `translator_.c`), `test.c`.
- `Interfaces` : 7 CAS (`gap`, `m2`, `maxima`, `octave`, `pari`, `scilab`, `yacas`) + `common.c`.

### 2.2 Sites de création de processus (complétés)
| # | Fichier:ligne | Motif | Cible |
|---|---|---|---|
| F1 | `lines.c:927-972` | fork/execve/execvp/waitpid + alarm | `win32_spawn` (mode fichier) |
| F2 | `Interfaces/common.c:168-211` | fork, dup2 sur pipes, setreuid, exec, `ch..root` en préfixe (`:196`) | `win32_spawn` (mode CAS overlapped) |
| F3 | `Wimslogd/wimslogdlines.c:24-65` | fork/exec | `win32_spawn` |
| F4 | `Texgif/basic.c:72-115` | fork/setreuid/exec, appelle `sh` | `win32_spawn` + sh.exe |
| F5 | `log.c:446-464` | double fork | supprimé (service) |
| F6 | `mathml.c:42-142` | fork + pipe | `win32_spawn` (mode pipe) |
| F7 | `Wimsd/wimsd.c:206` | fork par requête | hors périmètre (Apache) |
| F8 | `Wimslogd/wimslogd.c:61` | fork | `win32_spawn` |
| F9 | `Wimslogd/cleaning.c:146`, `Wimslogd/log.c:87` | fork | `win32_spawn` |
| F10 | `Misc/canvasdraw/canvasdraw.c:6330,6372` | fork + exec `wims_mathml`, `obabel` | `win32_spawn` (mode pipe) |
| ➕ F11 | `Misc/chroot.c:235-257` | chroot + rlimit + exec | réécriture : `ch..root.exe` sandbox |
| ➕ F12 | `Misc/wrap.c:34,38` | exec | `_spawnv` ou suppression |
| ➕ F13 | `Misc/webget.c:117`, `Misc/clickzone.c:115` | `system()` | `_spawnv` / sh.exe |
| ➕ F14 | `exec.c`, `lines.c` | `popen`/`system` | `_popen` vers sh.exe |

### 2.3 select() (corrigé)
- S1-S4 (`Interfaces/common.c:494,519,549` ; `calc.c:242,255`) : pipes → overlapped.
- S5-S7 (`wimslogd.c:197`, `Wimslogd/socket.c:85`, `lines.c:1364`) : **inchangés si l'on garde des sockets** (AF_UNIX Winsock ou TCP). À réécrire si l'on passe aux named pipes.

### 2.4 call_sh
Liste de Claude validée. **À compléter par un inventaire exhaustif** (`rg -n "call_sh|call_ssh" src`) et par `public_html`.
---

## 3. Module win32compat/ (révisé)

### 3.1 Structure
Celle de Claude est conservée (`process.c`, `pipe.c`, `signal.c`, `mmap.c`, `sandbox.c`, `socket.c`, `misc.c`, `service.c`, `rlimit.c`), avec ces ajouts :
- `spawn.c` : **point d'entrée unique** `win32_spawn()`, qui factorise les 3 variantes de Claude.
- `cmdline.c` : `argv` → ligne de commande, avec les règles de quotation de MSVCRT (`\"`, antislashs). C'est critique contre l'injection.
- `fmode.c` : `_set_fmode(_O_BINARY)` et initialisation UTF-8.
- `loadavg.c` : `GetSystemTimes` → moyenne de charge glissante pour `wimslogd.c:193`.
- `chrootexe/` : nouveau `ch..root.exe` (§3.3).

### 3.2 `win32_spawn()` : les corrections clés
```c
/* Corrections par rapport aux 3 variantes de Claude V1 :
 * 1. STARTUPINFOEXW + PROC_THREAD_ATTRIBUTE_HANDLE_LIST : seuls les 3 handles std sont hérités.
 * 2. CREATE_SUSPENDED -> AssignProcessToJobObject -> ResumeThread (= Claude, validé).
 * 3. Mode CAS : stdin ET stdout en named pipes OVERLAPPED (nom aléatoire,
 *    FILE_FLAG_FIRST_PIPE_INSTANCE, ACL restreinte), ConnectNamedPipe géré.
 * 4. Mode pipe : thread d'écriture + lecture bornée + timeout réel
 *    (WaitForMultipleObjects {hProcess, evtRead} ; à l'échéance -> TerminateJobObject).
 * 5. .bat/.cmd interdits : on lance l'exécutable réel (Maxima : sbcl.exe/gcl).
 * 6. Chemins et arguments en UTF-16 (CreateProcessW) convertis depuis l'UTF-8.
 */
typedef enum { SPAWN_FILES, SPAWN_PIPE, SPAWN_CAS } spawn_mode;
int win32_spawn(const char *exe, char *const argv[], spawn_mode m,
                const char *inf, const char *outf, const char *errf,
                const spawn_limits *lim, spawn_ctx *ctx /* SPAWN_CAS */);
```

### 3.3 Sandbox : remplacement réel de `ch..root`
Le Job Object de Claude (CPU, mémoire, UI, kill-on-close) est **validé, mais insuffisant**. À ajouter :
- **`JOB_OBJECT_LIMIT_ACTIVE_PROCESS`**, l'équivalent de `RLIMIT_NPROC` (`Misc/chroot.c:238`).
- **Limite de taille de fichier** (`RLIMIT_FSIZE`, `chroot.c:207,215`) : pas d'équivalent Job. Il faut un quota NTFS sur le volume de travail, ou une vérification de taille après exécution.
- **Isolation du système de fichiers :**
  - **jeton restreint** (`CreateRestrictedToken`, SID restreint « wims-cas », niveau d'intégrité **Low**) ;
  - plus des **ACL** n'autorisant l'écriture que dans `chroot\tmp\sessions\<id>` ;
  - en option, **AppContainer** (sans capacité réseau) pour bloquer le réseau.
- **`cleantmpdir`** (`chroot.c:~199`, appelé par `cleaning.c:36` et `Wimslogd/cleaning.c:65`) : à réimplémenter dans `ch..root.exe cleantmpdir`.
- **Filtrage Octave** (`Interfaces/octave.c:73-77`) : à conserver et compléter (`dos`, `unix`, `winopen`, `winqueryreg`, `!` shell).
- **Tests d'évasion** : lecture hors de la session, écriture, réseau, création de processus.

### 3.4 Mémoire partagée
`win32_mmap` de Claude, corrigé : nom `Global\WimsShm`, descripteur de sécurité (wimslogd en lecture-écriture, compte Apache en lecture), créé par le service (`wimslogd.c:141`). Côté wims.exe, le code est **aujourd'hui commenté** (`wims.c:1222-1224`) : rien à porter tant qu'il n'est pas réactivé.

### 3.5 Timeouts (remplace SIGALRM) : correction majeure
`alarm1`/`alarm2` (`lines.c:837-847`) appellent `kill`, puis `cleantmpdir` et `module_error("timeup")`. Exécuter ces handlers dans le thread du TimerQueue (Claude §3.5) **corromprait l'état** : `module_error` écrit la sortie HTML et termine, en concurrence avec le thread principal.

**Schéma V2 :**
- Le timer **ne fait que** `TerminateJobObject(job)` et positionner `volatile LONG timed_out = 1`.
- Le thread principal, débloqué par la fin de l'enfant, teste `timed_out`, puis appelle `cleantmpdir()` et `module_error("timeup")` **dans son propre contexte**.
- Pour le timeout global du CGI (`wims.c` `initalarm`) : timer qui écrit une page d'erreur minimale puis `TerminateProcess(GetCurrentProcess())`, ce qui reproduit le comportement « sortie forcée ».

### 3.6 Service wimslogd
Celui de Claude est validé. À ajouter :
- `SERVICE_START_PENDING` avec `dwWaitHint` ;
- passage en `SERVICE_STOPPED` sur erreur ;
- `loadavg.c` ;
- l'IPC en AF_UNIX Winsock sur `wims\tmp\wimslogd.sock` (code `socket.c` presque inchangé, `select` conservé).

---

## 4. Phase 1 : build (CMake), corrections
Base de Claude conservée. Modifications :
```cmake
project(WIMS VERSION 4.33 LANGUAGES C CXX)        # trunk r19040 = 4.33a ; C++ pour chemeq
add_compile_options(/utf-8)
find_package(FLEX REQUIRED)  # winflexbison
find_package(BISON REQUIRED)
# Interfaces : garder les noms d'exécutables d'origine (m2, octave, pari, ...)
foreach(CAS gap m2 maxima octave pari scilab yacas)
  add_executable(${CAS} Interfaces/${CAS}.c Interfaces/common.c Common/setenv.c)
endforeach()
# OefMsg2wims : DEUX exécutables
add_executable(oef2wims OefMsg2wims/oef2wims.c OefMsg2wims/oeflines.c ...)
add_executable(msg2wims OefMsg2wims/msg2wims.c OefMsg2wims/msglines.c ...)
# Misc : liste complète
set(MISC_TOOLS ccsum clickzone cryptarith curvecomp cyclicode dicsort exostat ftype
    html2msg huffman modind modstat oncechar phtmltext Plouffe shortpath suffix
    translator webget wrap)
add_subdirectory(Misc/chemeq)        # flex/bison + C++
add_subdirectory(Misc/units-filter)  # flex/bison
foreach(D symtext mathexp voronoiW whirlgif bioinfo lceb checkmol crossword
          sigdigits moneyprint scienceprint toascii)
  add_subdirectory(Misc/${D})        # CMakeLists à écrire pour chacun (~20 Makefile.in)
endforeach()
add_executable(ch..root win32compat/chrootexe/chroot_win.c)   # remplace Misc/chroot.c sous WIN32
```
vcpkg : `libgd`, `zlib`, (`openssl` seulement si crypt est réimplémenté). Outils : `winflexbison`, MSYS2 (`sh`, coreutils).

---

## 5 à 8. Phases 2 à 5 : tableaux de Claude validés, avec ajustements
| Phase | Ajustements V2 | Effort Claude → V2 |
|---|---|---|
| P2 moteur | `auth.c` → retiré (crypt dans `calc.c`, `HAVE_CRYPT`) ; `wims.c` sans mmap ; ajouter `_O_BINARY`/UTF-8, `/proc/net/tcp`, popen → sh.exe, timeouts §3.5 | 16 j → **20 j** |
| P3 CAS | Overlapped dans les 2 sens, lancement sans `.bat`, sandbox via `ch..root.exe`, filtrage Octave étendu ; disponibilité réelle de yacas/m2/scilab sous Windows à vérifier | 11 j → **15 j** |
| P4 outils | `ch..root.exe` complet (4-5 j au lieu de 1), chemeq/units-filter (flex/bison), ~20 CMakeLists Misc | 11 j → **18 j** |
| P5 wimslogd | AF_UNIX conservé (moins de travail sur socket.c), mais loadavg + Global shm + service robuste + handles enfants (`fork.c`) | 10,5 j → **11 j** |

---

## 9. Phase 6 : scripts (révisée)
Puisque `sh.exe` + coreutils MSYS2 sont embarqués, **les 35 scripts de `bin/` restent en sh** pour la première version. On adapte seulement ce qui touche au système :
- `apache-config` : configuration Apache Windows ;
- `changepermissions` → `icacls` ;
- `server-interrupt` / `server-resume` → `sc stop|start WimsLogd` ;
- `setchroot` → configuration de `ch..root.exe` ;
- `setwimsd` et `wrapuid` : supprimés ;
- `housekeep.daily` → tâche planifiée appelant sh.exe.

Réécriture en PowerShell/Python **optionnelle**, plus tard. Effort : 3-4 semaines → **1,5-2 semaines**, plus `public_html/scripts`, non chiffré tant que le dossier est vide.

## 10. Phase 7 : tests (complétée)
T1-T10 de Claude validés. À ajouter :
- **T11 non-régression** : rendu HTML Linux vs Windows sur un corpus d'au moins 200 modules OEF, avec diff automatique.
- **T12 sécurité** : évasion du sandbox CAS (lecture `C:\`, écriture hors session, réseau, fork bomb), injection d'arguments (cmdline), Octave.
- **T13 charge** : 30 utilisateurs simultanés (une classe), et non 5.
- **T14 robustesse** : arrêt de wimslogd, timeouts CAS, fuites de handles (Process Explorer).

Effort : 3-4 semaines → **5-6 semaines**.

---

## 11. Chiffrage révisé
| Phase | Claude V1 | **Grok V2** | Écart et raison |
|---|---|---|---|
| P0 Cadrage : tag 4.32 vs trunk, `public_html`, inventaire `call_sh` | — | **1 sem** | Ajouté |
| P1 CMake + win32compat (spawn, cmdline, fmode) | 2 sem | **3 sem** | flex/bison, Misc, spawn unifié |
| P2 Moteur wims.exe | 3 sem | **4 sem** | Timeouts, UTF-8/binaire, popen |
| P3 Interfaces CAS | 2,5 sem | **3 sem** | Overlapped bidirectionnel, sans .bat |
| P3b **Sandbox `ch..root.exe` + sécurité** | (1 j) | **3 sem** | Jeton restreint/AppContainer, ACL, cleantmpdir, Octave |
| P4 Outils (flydraw, texgif, canvasdraw, Misc) | 2 sem | **3,5 sem** | ~20 sous-projets Misc |
| P5 wimslogd service | 2,5 sem | **2 sem** | AF_UNIX conservé |
| P6 Scripts (sh conservé) | 3-4 sem | **2 sem** | Pas de réécriture PowerShell en V1 |
| P7 Tests, non-régression, sécurité, charge | 3-4 sem | **5-6 sem** | T11-T14 |
| Marge d'intégration et de débogage (≈ 15 %) | — | **3-4 sem** | Interblocages, chemins, encodages |
| **Total (1 développeur)** | **~5,5 mois** | **≈ 30-32 semaines ≈ 6,5-7,5 mois** | |
| Total (2 développeurs, P6/P4 en parallèle) | — | **≈ 4-5 mois** | |

**Ordre de grandeur cohérent** avec mes estimations précédentes (4,5-6,5 mois hors sandbox et non-régression). L'écart vient essentiellement de la sécurité et des tests, que Claude sous-estimait.

## 12. Jalons (révisés)
| Jalon | Fin de | Critère |
|---|---|---|
| J0 | S1 | Version cible figée (tag ou trunk), `public_html` inventorié |
| J1 | S4 | Build vert (tous les exe, y compris Misc et flex/bison) |
| J2 | S8 | wims.exe sert l'accueil via Apache ; timeouts OK |
| J3 | S11 | Maxima et PARI répondent en overlapped, sans interblocage sur 10 Mo de sortie |
| J4 | S14 | `ch..root.exe` passe les tests d'évasion T12 |
| J5 | S18 | Exercice complet OEF + CAS + flydraw + texgif |
| J6 | S20 | wimslogd en service, mémoire partagée Global OK |
| J7 | S28-32 | T11-T14 verts, release candidate |

## 13. Risques
1. **Sécurité du sandbox** : c'est le risque n°1. Sans isolation équivalente, ne pas ouvrir le service aux élèves.
2. **Divergence avec l'amont** : reverser les patchs à wimsdev (GPL), sinon chaque mise à jour du trunk coûte cher.
3. **CAS Windows** : versions et options CLI différentes ; Yacas, M2 et Scilab peut-être indisponibles ou instables.
4. **Encodages et fins de ligne** : régressions silencieuses dans les modules.
5. **Dépendance à MSYS2 sh** : le portage n'est « natif » que pour le C. Assumé.

## 14. Recommandation
- L'architecture est **valide et exploitable avec les corrections ci-dessus**.
- **Avant d'engager 6 à 7 mois**, il reste recommandé de faire un **prototype Cygwin + Apache (2-4 semaines)**, qui sert aussi de **référence de non-régression** pour T11.
- Si le natif est confirmé, commencer par **P0, P1 puis P3 + P3b** (CAS et sandbox), là où se concentrent les risques techniques.

## Annexes
- **Annexe A** (projets de référence PostgreSQL, nginx, Win32-OpenSSH, Redis) : **validée telle quelle**. Ajouter `CreateRestrictedToken` et AppContainer (exemples Microsoft *Sandboxing* / Chromium sandbox `sandbox/win`) pour P3b.
- **Annexe B** (checklist) : celle de Claude, plus :
  - winflexbison ;
  - MSYS2 `pacman -S coreutils` ;
  - Apache Win64 (Apache Lounge) ou Laragon ;
  - `svn ls .../tags` ;
  - corpus de modules de référence pour T11 ;
  - compte de service `wims-svc`.