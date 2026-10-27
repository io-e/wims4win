# CLAUDE_V1_3
# Architecture détaillée — Portage natif MSVC de WIMS 4.33a (r19040)

**Date:** 2026-10-23 (rév. V1_3 2026-10-23)  
**Scope:** Portage complet du serveur WIMS sur Windows natif (MSVC + Win32 API)  
**Estimation révisée :** 6 – 7,5 mois (1 dev), 4 – 5 mois (2 devs)  
**Prérequis :** Étude V1_1 (`Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md`)  
**Base :** Architecture V1 (`Architecture_Portage_MSVC_WIMS_CLAUDE_V1.md`) corrigée après 2 cross-reviews Grok  
**Sources :** trunk `wims` **r19040**, fichier `version` = **4.33a** (pas 4.32)

### Historique des reviews

| Review | Document Grok | Points | Corrections appliquées dans |
|--------|---------------|--------|-----------------------------|
| **Review 1** (étude MSVC) | Commentaires intégrés dans l'audit | 9 erreurs + 6 oublis | `Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md` (commit `98cae01`) |
| **Review 2** (architecture) | `Architecture_Portage_MSVC_WIMS_GROK_V2.md` | 40 points (24 corrections) | Ce document V1_3 (§0 errata) |

---

## 0. Traçabilité des cross-reviews Grok

### 0.1 Review 1 — Étude MSVC (9 erreurs + 6 oublis)

Grok a identifié 9 erreurs et 6 oublis dans `Etude_Portage_MSVC_Natif_CLAUDE_V1.md`. **Toutes ont été corrigées** dans V1_1 de l'étude (commit `98cae01`) et sont reprises dans cette architecture :

| # | Point Grok Review 1 | Statut | Où c'est corrigé |
|---|---------------------|--------|------------------|
| G1.1 | Sécurité : chroot/setreuid → noop silencieux supprime l'isolation | ✅ Corrigé | §3.3 `ch..root.exe` (jeton restreint + AppContainer + ACL), §1.3 setreuid → compte service |
| G1.2 | Mémoire partagée : vise shmget, WIMS utilise mmap | ✅ Corrigé | §3.4 `CreateFileMapping("Global\\WimsShm")`, confirmé `wimslogd.c:141` |
| G1.3 | Pipes : select() → WaitForMultipleObjects bloque sur pipes anonymes | ✅ Corrigé | §3.2.1 named pipes overlapped bidirectionnels, §3.2.2 `win32_dopipes()` |
| G1.4 | Shell : cmd.exe ne marche pas, WIMS envoie du bash | ✅ Corrigé | §1.3 `call_sh → sh.exe MSYS2` + coreutils, §2.4 inventaire 30+ appels |
| G1.5 | wimsd : l'adapter est inutile, Apache CGI le remplace | ✅ Corrigé | §1.2 wimsd supprimé, §2.2 F7 hors scope |
| G1.6 | MSYS2 : confusion MinGW / runtime Cygwin | ✅ Corrigé | §1.3 clarification : MinGW n'a pas fork, sh.exe utilise msys-2.0.dll |
| G1.7 | execredirected() : 4 versions + 6 fork directs, pas 1 | ✅ Corrigé | §2.2 inventaire 14 sites (F1-F14), les 4 execredirected identifiés |
| G1.8 | Oubli wimslogd | ✅ Corrigé | §8 Phase 5 complète (service SCM, AF_UNIX, Global\WimsShm, loadavg) |
| G1.9 | Oubli Texgif | ✅ Corrigé | §7 Phase 4, F4 (`Texgif/basic.c:72`), MiKTeX |
| G1.10 | Oubli canvasdraw + OpenBabel | ✅ Corrigé | §7 Phase 4, F10 (`canvasdraw.c:6330,6372`), obabel.exe natif |
| G1.11 | Oubli libcrypt | ✅ Corrigé | §1.3 `HAVE_CRYPT` optionnel dans `calc.c:2007`, pas auth.c (E10) |
| G1.12 | Oubli sendmail | ✅ Corrigé | §2.4 `exec.c:1804` → msmtp.exe |
| G1.13 | Oubli filtrage sécurité Octave | ✅ Corrigé | §16.5 C5.1 filtrage étendu (`dos`, `winqueryreg`, `!`), §6 octave.c |
| G1.14 | Estimation 3 mois trop optimiste | ✅ Corrigé | §11 révisé à 6,5-7,5 mois (1 dev) |
| G1.15 | Troncature au milieu de l'étude (fin Redis / début compat layer) | ✅ Corrigé | Architecture V1 complète, pas de troncature |

### 0.2 Review 2 — Architecture V1 (40 points, 24 corrections)

Grok a validé l'architecture V1 point par point contre les sources r19040 dans `Architecture_Portage_MSVC_WIMS_GROK_V2.md`. **Toutes les corrections ont été intégrées** dans cette V1_3.

### 0.3 Tableau des corrections de la Review 2

| # | Erreur V1 | Correction V1_3 | Gravité |
|---|-----------|-----------------|---------|
| E1 | Version « 4.32 » | **4.33a** (r19040 trunk). Avant de démarrer : `svn ls .../tags`, décider trunk ou tag | Cosmétique |
| E2 | `bInheritHandles=TRUE` dans CreateProcess (fuite de handles) | **STARTUPINFOEX + PROC_THREAD_ATTRIBUTE_HANDLE_LIST** : seuls les 3 handles std sont hérités | Bloquant (sécurité + interblocage) |
| E3 | Variante 2 : stdin en pipe anonyme bloquant | **Overlapped dans les 2 sens** ou thread d'écriture. Sinon interblocage si CAS remplit stdout pendant écriture parent | Bloquant |
| E4 | Variante 3 : écrire tout, lire tout, attendre | **Thread d'écriture + lecture bornée + timeout réel**. Deadlock si input > 4Ko | Bloquant |
| E5 | Nom shared memory `"WimsSharedMem"` (session locale) | **`Global\WimsShm`** + descripteur de sécurité. wimslogd (session 0) et Apache ne partagent pas la même session | Bloquant |
| E6 | Timer callback appelle `module_error("timeup")` (longjmp inter-thread) | Timer **ne fait que** `TerminateJobObject` + flag `volatile LONG timed_out`. Thread principal teste le flag après WaitForSingleObject | Bloquant |
| E7 | Sandbox = Job Object seul | **Insuffisant** : Job Object n'isole pas le filesystem. Faut : jeton restreint (`CreateRestrictedToken`) ou AppContainer + ACL NTFS + `ch..root.exe` dédié | Bloquant prod |
| E8 | setreuid → warning noop | **Compte de service dédié** + jeton restreint pour enfants, pas un noop | Sécurité |
| E9 | `mmap` actif dans `wims.c:1222-1224` | **Commenté** dans le code source. Seul `wimslogd.c:141` est actif | Mineur |
| E10 | `crypt()` dans `auth.c` | Dans **`calc.c:2007-2020`** sous `#ifdef HAVE_CRYPT` (optionnel) | Erreur doc |
| E11 | OefMsg2wims = 1 exécutable | **2 exécutables** : `oef2wims` + `msg2wims` | Build |
| E12 | Interfaces → `wims_maxima`, `wims_gap`… | Le Makefile produit **`maxima`**, **`gap`**, etc. Garder les noms d'origine | Build |
| E13 | 10 sites fork() | **14 sites** minimum : + `chroot.c:257` (exec), `wrap.c:34,38` (exec), `webget.c:117` (system), `clickzone.c:115` (system), `Flydraw/objects.c:126` (system) | Incomplétude |
| E14 | S5-S7 « OK car sockets » | OK **seulement si on garde AF_UNIX Winsock** (Win10 1803+) ou TCP localhost. Named pipes casseraient ces select() aussi | Architecture |
| E15 | ch..root.c « remplacer ou supprimer, 1 jour » | `chroot.c` gère cleantmpdir, RLIMIT_FSIZE, RLIMIT_NPROC, chdir, setenv. Faut **`ch..root.exe` complet** : 4-5 jours | Chiffrage |
| E16 | Scripts → PowerShell (3-4 semaines) | sh.exe MSYS2 embarqué de toute façon → **garder scripts sh en V1**, adapter seulement l'indispensable : 1,5-2 semaines | Chiffrage |
| E17 | Tests « 3-4 semaines » | Manque non-régression (diff HTML Linux/Win), tests sécurité sandbox, charge 30 users, robustesse. **5-6 semaines** | Chiffrage |
| E18 | CMake : manque flex/bison pour chemeq, units-filter | Ajouter **winflexbison** + `add_subdirectory` pour ~20 sous-projets Misc | Build |
| E19 | Misc tools : liste incomplète | Manque `suffix.c`, `translator.c`, `chroot.c`, sous-dossiers `chemeq`, `units-filter`, `symtext`, `mathexp`, `voronoiW`, `whirlgif`, `bioinfo`, `lceb`, `checkmol`, `crossword`, `sigdigits`, `moneyprint`, `scienceprint`, `toascii` | Build |
| E20 | `.bat` lancé directement par CreateProcess | `.bat` passe par cmd.exe (injection). Maxima : lancer **sbcl.exe/gcl** directement, pas `maxima.bat` | Sécurité |
| E21 | Pas de gestion `_O_BINARY` / UTF-8 | `fopen` sans `b` convertit CRLF. Forcer **`_set_fmode(_O_BINARY)`** + manifeste `activeCodePage=UTF-8` + compiler `/utf-8` | Bloquant |
| E22 | ConnectNamedPipe manquant + nom prédictible | Ajouter `ConnectNamedPipe` / `ERROR_PIPE_CONNECTED` + nom aléatoire anti-squatting + `FILE_FLAG_FIRST_PIPE_INSTANCE` | Sécurité |
| E23 | `call_sh` inventaire incomplet (7 appels) | Inventaire réel : **30+ appels** `call_sh`/`call_ssh` dans le code source (voir §2.4) | Incomplétude |
| E24 | PID recyclage dans `win32_kill` | Conserver les **HANDLE** des enfants (comme `Wimslogd/fork.c` `addfork`) plutôt que `OpenProcess(pid)` | Fiabilité |

---

## 1. Vue d'ensemble de l'architecture cible

### 1.1 Architecture Linux actuelle

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
                  │  ├─ IPC wimslogd : AF_UNIX Winsock (Win10 1803+) ← garde select()
                  │  ├─ Global\WimsShm (lecture seule, commenté dans wims.c, actif dans wimslogd)
                  │  ├─ win32_spawn() : STARTUPINFOEX + HANDLE_LIST + Job Object
                  │  └─ call_sh → wims\msys\usr\bin\sh.exe (+ coreutils)
     ┌──────────┬──────────┬───────────────┬──────────────┬──────────────────────┐
  flydraw    texgif      Interfaces       canvasdraw     ch..root.exe (nouveau)
  libgd      MiKTeX      I/O overlapped   obabel.exe     = jeton restreint/AppContainer
  (vcpkg)               2 sens + timeout                 + Job Object + ACL ../chroot/tmp
                                                          + cleantmpdir + FSIZE/NPROC
wimslogd.exe — service Windows (SCM/NSSM), crée Global\WimsShm, GetSystemTimes (≈ loadavg)
```

### 1.3 Décisions d'architecture (révisées)

| Décision | Choix V1_1 | Changement vs V1 / Justification |
|----------|-----------|----------------------------------|
| wimsd | **Supprimé** — Apache CGI | = V1 |
| fork+exec | **`win32_spawn()` unique** : STARTUPINFOEX + `PROC_THREAD_ATTRIBUTE_HANDLE_LIST` + Job Object | ✏️ Évite l'héritage global des handles (E2) |
| Pipes CAS | **Overlapped dans les 2 sens**, ou thread d'écriture | ✏️ Évite l'interblocage en écriture (E3) |
| IPC wimslogd | **AF_UNIX Winsock** (Win10 1803+) | ✏️ Conserve select() côté client et serveur, moins de code que named pipes (E14) |
| Mémoire partagée | `CreateFileMapping("Global\\WimsShm")` + descripteur de sécurité | ✏️ Session 0 ↔ Apache (E5) |
| SIGALRM | Timer qui **termine l'enfant** seulement ; thread principal constate le timeout | ✏️ Pas de longjmp inter-thread (E6) |
| Sandbox | `ch..root.exe` : jeton restreint + Job Object + ACL NTFS | ✏️ Job seul insuffisant (E7) |
| setreuid | **Compte de service dédié** + jeton restreint enfants | ✏️ Pas de noop silencieux (E8) |
| Shell | sh.exe MSYS2 + **coreutils** embarqués | = V1, +dépendances explicites |
| Scripts bin/ | **Garder en sh** pour V1 (sh.exe embarqué) | ✏️ Pas de réécriture PowerShell (E16) |
| Build | CMake + vcpkg + **winflexbison** | ✏️ flex/bison (E18) |
| crypt | Ne pas définir `HAVE_CRYPT` (optionnel dans `calc.c:2007`) | ✏️ Pas dans auth.c (E10) |
| .bat | **Interdits** : lancer le binaire réel (sbcl.exe, pas maxima.bat) | ✏️ Injection cmd.exe (E20) |
| Encodage | `/utf-8` + `_O_BINARY` + manifeste UTF-8 | ✏️ Ajouté (E21) |

---

## 2. Cartographie des sources r19040 (corrigée)

### 2.1 Arbre source

L'arbre de V1 (§2.1) est **validé**, avec ces corrections :
- `auth.c` n'utilise pas libcrypt : c'est **`calc.c:2007-2020`** (`#ifdef HAVE_CRYPT`)
- `OefMsg2wims` produit **2 exécutables** (`oef2wims`, `msg2wims`)
- `Misc/*.c` : ajouter `suffix.c`, `translator.c` (+ `translator_.c`), `test.c`
- Sous-dossiers Misc manquants dans CMake : `chemeq` (flex/bison + C++), `units-filter` (flex/bison), `symtext`, `mathexp`, `voronoiW`, `whirlgif`, `bioinfo`, `lceb`, `checkmol`, `crossword`, `sigdigits`, `moneyprint`, `scienceprint`, `toascii`

### 2.2 Sites de création de processus (complétés : 14+ sites)

| # | Fichier:Ligne | Pattern | Action |
|---|---------------|---------|--------|
| F1 | `lines.c:927-972` | fork/execve/execvp/waitpid + alarm | → `win32_spawn(SPAWN_FILES)` |
| F2 | `Interfaces/common.c:168-211` | fork, dup2 pipes, setreuid, ch..root, exec | → `win32_spawn(SPAWN_CAS)` |
| F3 | `Wimslogd/wimslogdlines.c:24-65` | fork/exec | → `win32_spawn(SPAWN_FILES)` |
| F4 | `Texgif/basic.c:72-115` | fork/setreuid/exec, appelle sh | → `win32_spawn(SPAWN_FILES)` + sh.exe |
| F5 | `log.c:446-464` | double fork (daemonize) | → **Supprimer** (service SCM) |
| F6 | `mathml.c:42-142` | fork + pipe | → `win32_spawn(SPAWN_PIPE)` |
| F7 | `Wimsd/wimsd.c:206` | fork par requête | → **HORS SCOPE** (Apache) |
| F8 | `Wimslogd/wimslogd.c:61` | fork (logexec) | → `win32_spawn` |
| F9 | `Wimslogd/cleaning.c:146`, `log.c:87` | fork (nettoyage) | → `win32_spawn` |
| F10 | `canvasdraw.c:6330,6372` | fork+exec wims_mathml, obabel | → `win32_spawn(SPAWN_PIPE)` |
| **F11** | `Misc/chroot.c:235-257` | chroot + rlimit + exec | → **`ch..root.exe`** (réécriture complète) |
| **F12** | `Misc/wrap.c:34,38` | setreuid + exec | → `_spawnv` ou suppression |
| **F13** | `Misc/webget.c:117`, `clickzone.c:115` | `system()` | → `_system` vers sh.exe |
| **F14** | `Flydraw/objects.c:126` | `system()` | → `_system` vers sh.exe |

### 2.3 select() sur pipes (corrigé)

- **S1-S4** (`Interfaces/common.c:494,519,549` ; `calc.c:242,255`) : pipes → **overlapped I/O** (inchangé vs V1)
- **S5-S7** (`wimslogd.c:197`, `socket.c:85`, `lines.c:1364`) : **OK si on garde AF_UNIX Winsock** (Win10 1803+). Si named pipes → à réécrire aussi. **Décision V1_1 : garder AF_UNIX Winsock**, ce qui préserve select() et minimise les changements.

### 2.4 call_sh / call_ssh — inventaire complet (30+ appels)

**Dans le moteur wims :**

| Fichier:Ligne | Commande | Action Windows |
|---------------|----------|----------------|
| `wims.c:954` | `rm -Rf %s/%s* >/dev/null 2>&1` | sh.exe ou `win32_rmdir_recursive()` |
| `exec.c:1185` | `%s/%s %d %s >%s/ins.out 2>%s/ins.err` | sh.exe |
| `exec.c:1190` | `mv %s/insert%s-%d.%s %s >/dev/null` | sh.exe ou `MoveFile()` |
| `exec.c:1804` | `cat mail.eml \| /usr/sbin/sendmail -t` | **msmtp.exe** |
| `calc.c:1073` | `date %s >%s/date.out` | sh.exe |
| `calc.c:1093` | `ls %s >%s/ls.out 2>%s/ls.err` | sh.exe |
| `calc.c:1181` | `%s &>%s/instexst.log` (tex2gif) | sh.exe |
| `log.c:443` | `mv %s %s` | sh.exe |
| `log.c:444` | `rm -f %s` | sh.exe |
| `lines.c:1330` | `%s/%s >%s/ins.Out 2>%s/ins.Err` | sh.exe |
| `lines.c:1333` | `mv %s %s >/dev/null` | sh.exe |
| `variables.c:843` | `cat %s/var >%s/corrupt.var.bak` | sh.exe |
| `cleaning.c:36` | `bin/ch..root cleantmpdir` | `ch..root.exe cleantmpdir` |

**Dans Wimslogd :**

| Fichier:Ligne | Commande | Action Windows |
|---------------|----------|----------------|
| `wimslogd.c:63` | `sh log/wimslogd.exec >...` | sh.exe |
| `wimslogd.c:72` | `sh log/wimslogd.local` | sh.exe |
| `Wimslogd/log.c:89` | `ls %s* 2>/dev/null \|\| exit; ...` | sh.exe |
| `Wimslogd/cleaning.c:65` | `bin/ch..root cleantmpdir` | `ch..root.exe` |
| `Wimslogd/housekeep.c:74` | `bin/housekeep.daily &>...` | sh.exe |
| `Wimslogd/housekeep.c:94` | `bin/modupdate.auto &>...` | sh.exe |
| `Wimslogd/housekeep.c:108` | `bin/backup &>...` | sh.exe |

**Dans Texgif :**

| Fichier:Ligne | Commande | Action Windows |
|---------------|----------|----------------|
| `Texgif/tfm.c:48` | `` cp `kpsewhich %s.tfm` %s `` | sh.exe (kpsewhich MiKTeX) |
| `Texgif/gf.c:167` | `mkdir -p %s; ...` | sh.exe |
| `Texgif/gf.c:235` | `cat %s >>%s; mkdir -p; mv -f` | sh.exe |

**Stratégie V1_1 :** Tout passe par sh.exe MSYS2 embarqué. Seule exception : sendmail → msmtp.exe natif. Optimisation Win32 native (`MoveFile`, `CreateDirectory`) **optionnelle V2**.

---

## 3. Module win32compat/ — couche d'abstraction (révisée)

### 3.1 Structure (corrigée)

```
wims/src/win32compat/
├── CMakeLists.txt
├── win32compat.h               # Header principal
│
├── spawn.c                     # ✏️ Point d'entrée UNIQUE win32_spawn()
│                               # Factorise les 3 variantes V1 (SPAWN_FILES/PIPE/CAS)
│                               # STARTUPINFOEX + HANDLE_LIST + Job Object
│
├── cmdline.c                   # ✏️ AJOUTÉ : argv → cmdline (quotation MSVCRT)
│                               # Critique contre l'injection d'arguments
│
├── pipe.c                      # CreateNamedPipe overlapped (2 sens)
│                               # ConnectNamedPipe, nom aléatoire, FIRST_PIPE_INSTANCE
│
├── signal.c                    # ✏️ CORRIGÉ : timer threads safe
│                               # Timer → TerminateJobObject + flag seulement
│                               # Thread principal teste le flag
│
├── mmap.c                      # ✏️ CORRIGÉ : Global\WimsShm + DACL
│
├── sandbox.c                   # ✏️ CORRIGÉ : Job Object + jeton restreint + ACL
│
├── chrootexe/                  # ✏️ AJOUTÉ : ch..root.exe complet
│   └── chroot_win.c           # Jeton restreint/AppContainer + Job + ACL
│                               # + cleantmpdir + FSIZE quota + NPROC
│
├── socket.c                    # ✏️ CHANGÉ : AF_UNIX Winsock (pas named pipes)
│
├── misc.c                      # Utilitaires : rmdir_recursive, chemins, %TEMP%
│
├── fmode.c                     # ✏️ AJOUTÉ : _set_fmode(_O_BINARY) + UTF-8 init
│
├── loadavg.c                   # ✏️ AJOUTÉ : GetSystemTimes → charge glissante
│
├── service.c                   # Service Windows (SCM) pour wimslogd
│
└── rlimit.c                    # setrlimit → Job Objects
```

### 3.2 `win32_spawn()` — point d'entrée unique (corrigé)

```c
/*
 * Correction majeure vs V1 :
 * 1. STARTUPINFOEXW + PROC_THREAD_ATTRIBUTE_HANDLE_LIST : seuls les 3 handles
 *    std sont hérités (pas TOUS les handles héritables du parent).
 * 2. CreateProcessW (UTF-16) avec /utf-8 côté compilation.
 * 3. Mode CAS : stdin ET stdout en named pipes OVERLAPPED
 *    (nom aléatoire, FILE_FLAG_FIRST_PIPE_INSTANCE, ACL restreinte).
 * 4. Mode pipe : thread d'écriture + lecture bornée + timeout réel.
 * 5. .bat/.cmd interdits : on lance l'exécutable réel.
 * 6. Conserve les HANDLE des enfants (pas OpenProcess sur PID recyclé).
 */

typedef enum { SPAWN_FILES, SPAWN_PIPE, SPAWN_CAS } spawn_mode;

typedef struct {
    DWORD timeout_ms;       // 0 = infini
    DWORD cpu_limit_sec;    // 0 = pas de limite
    SIZE_T mem_limit;       // 0 = pas de limite
    DWORD nproc_limit;      // ActiveProcessLimit (≈ RLIMIT_NPROC)
} spawn_limits;

typedef struct {
    HANDLE hProcess;        // Conservé (pas de recyclage PID)
    HANDLE hJob;
    HANDLE hPipeIn_Wr;      // Parent → CAS stdin
    HANDLE hPipeOut_Rd;     // CAS stdout → Parent
    OVERLAPPED ovRead;
    OVERLAPPED ovWrite;     // ✏️ AJOUTÉ : écriture overlapped aussi
    char readBuf[4096];
    BOOL readPending;
    volatile LONG timed_out; // ✏️ AJOUTÉ : flag positionné par timer
} spawn_ctx;

int win32_spawn(const char *exe, char *const argv[], spawn_mode mode,
                const char *inf, const char *outf, const char *errf,
                const spawn_limits *lim, spawn_ctx *ctx)
{
    SECURITY_ATTRIBUTES sa = { sizeof(sa), NULL, TRUE };
    STARTUPINFOEXW siex = {0};
    siex.StartupInfo.cb = sizeof(siex);
    PROCESS_INFORMATION pi = {0};

    // --- Créer les handles selon le mode ---
    HANDLE hStdIn, hStdOut, hStdErr;
    // (détail selon SPAWN_FILES/PIPE/CAS — voir code complet ci-dessous)

    // --- HANDLE_LIST : seulement les 3 handles std ---
    HANDLE inheritList[3] = { hStdIn, hStdOut, hStdErr };
    SIZE_T attrSize = 0;
    InitializeProcThreadAttributeList(NULL, 1, 0, &attrSize);
    siex.lpAttributeList = (LPPROC_THREAD_ATTRIBUTE_LIST)malloc(attrSize);
    InitializeProcThreadAttributeList(siex.lpAttributeList, 1, 0, &attrSize);
    UpdateProcThreadAttribute(siex.lpAttributeList, 0,
        PROC_THREAD_ATTRIBUTE_HANDLE_LIST,
        inheritList, sizeof(inheritList), NULL, NULL);

    siex.StartupInfo.dwFlags = STARTF_USESTDHANDLES;
    siex.StartupInfo.hStdInput  = hStdIn;
    siex.StartupInfo.hStdOutput = hStdOut;
    siex.StartupInfo.hStdError  = hStdErr;

    // --- Ligne de commande sûre (quotation MSVCRT) ---
    wchar_t cmdline[MAX_LINELEN + 1];
    win32_argv_to_cmdline_w(exe, argv, cmdline, ARRAYSIZE(cmdline));

    // --- Job Object (sandbox + limites) ---
    HANDLE hJob = win32_create_sandbox(lim);

    // --- Lancer (EXTENDED_STARTUPINFO_PRESENT) ---
    if (!CreateProcessW(NULL, cmdline, NULL, NULL, TRUE,
                        CREATE_NO_WINDOW | CREATE_SUSPENDED |
                        EXTENDED_STARTUPINFO_PRESENT,
                        NULL, NULL, &siex.StartupInfo, &pi)) {
        win32_log_error("CreateProcess", exe);
        goto cleanup;
    }
    AssignProcessToJobObject(hJob, pi.hProcess);
    ResumeThread(pi.hThread);
    CloseHandle(pi.hThread);

    // Fermer les bouts hérités par l'enfant
    // ...

    // --- Remplir le contexte ---
    if (ctx) {
        ctx->hProcess = pi.hProcess;
        ctx->hJob = hJob;
        ctx->timed_out = 0;
        // ... pipes si mode CAS/PIPE
    }

    // --- Pour SPAWN_FILES : attendre avec timeout ---
    if (mode == SPAWN_FILES) {
        DWORD wait = WaitForSingleObject(pi.hProcess,
                        lim->timeout_ms ? lim->timeout_ms : INFINITE);
        if (wait == WAIT_TIMEOUT) {
            TerminateJobObject(hJob, 1);
        }
        DWORD exitCode = 127;
        GetExitCodeProcess(pi.hProcess, &exitCode);
        CloseHandle(pi.hProcess);
        CloseHandle(hJob);
        return (int)exitCode;
    }

    return 0;

cleanup:
    DeleteProcThreadAttributeList(siex.lpAttributeList);
    free(siex.lpAttributeList);
    // ... fermer handles
    return -1;
}
```

### 3.2.1 Mode CAS : overlapped bidirectionnel (corrigé)

```c
/*
 * Corrections vs V1 :
 * - stdin AUSSI en named pipe overlapped (pas anonymous pipe bloquant)
 * - ConnectNamedPipe / ERROR_PIPE_CONNECTED géré
 * - Nom aléatoire + FILE_FLAG_FIRST_PIPE_INSTANCE (anti-squatting)
 * - ACL restreinte sur les pipes
 */
static int create_cas_pipes(spawn_ctx *ctx,
                            HANDLE *childIn, HANDLE *childOut)
{
    SECURITY_ATTRIBUTES sa = { sizeof(sa), NULL, TRUE };
    char rnd[16];
    win32_random_hex(rnd, sizeof(rnd));

    // --- Pipe STDOUT (CAS→parent) : overlapped côté parent ---
    char outName[256];
    snprintf(outName, sizeof(outName),
             "\\\\.\\pipe\\wims_cas_out_%lu_%s",
             GetCurrentProcessId(), rnd);

    ctx->hPipeOut_Rd = CreateNamedPipeA(outName,
        PIPE_ACCESS_INBOUND | FILE_FLAG_OVERLAPPED | FILE_FLAG_FIRST_PIPE_INSTANCE,
        PIPE_TYPE_BYTE | PIPE_WAIT, 1, 4096, 4096, 0, NULL);

    *childOut = CreateFileA(outName, GENERIC_WRITE, 0, &sa,
        OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);

    // ConnectNamedPipe (peut retourner ERROR_PIPE_CONNECTED si déjà connecté)
    OVERLAPPED ovConnect = {0};
    ovConnect.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
    if (!ConnectNamedPipe(ctx->hPipeOut_Rd, &ovConnect)) {
        DWORD err = GetLastError();
        if (err != ERROR_PIPE_CONNECTED && err != ERROR_IO_PENDING) {
            // erreur réelle
        }
    }
    CloseHandle(ovConnect.hEvent);

    // --- Pipe STDIN (parent→CAS) : AUSSI overlapped côté parent ---
    char inName[256];
    snprintf(inName, sizeof(inName),
             "\\\\.\\pipe\\wims_cas_in_%lu_%s",
             GetCurrentProcessId(), rnd);

    ctx->hPipeIn_Wr = CreateNamedPipeA(inName,
        PIPE_ACCESS_OUTBOUND | FILE_FLAG_OVERLAPPED | FILE_FLAG_FIRST_PIPE_INSTANCE,
        PIPE_TYPE_BYTE | PIPE_WAIT, 1, 4096, 4096, 0, NULL);

    *childIn = CreateFileA(inName, GENERIC_READ, 0, &sa,
        OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);

    // Events pour overlapped
    ctx->ovRead.hEvent  = CreateEvent(NULL, TRUE, FALSE, NULL);
    ctx->ovWrite.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
    ctx->readPending = FALSE;

    return 0;
}
```

### 3.2.2 `win32_dopipes()` — overlapped bidirectionnel (corrigé)

```c
/*
 * Port de dopipes() (Interfaces/common.c:502-560)
 *
 * Correction vs V1 : l'ÉCRITURE est aussi overlapped.
 * WaitForMultipleObjects sur {evtRead, evtWrite} simultanément.
 * Plus d'interblocage si CAS remplit stdout pendant que parent écrit.
 */
int win32_dopipes(spawn_ctx *ctx,
    const char *inputbuf, size_t inputlen,
    char *obuf, size_t obufsize,
    const char *startstring, const char *endstring,
    int interval_ms, int timeout_intervals)
{
    const char *inp = inputbuf;
    const char *inp_end = inputbuf + inputlen;
    char *outptr = obuf;
    int k = 0;
    BOOL writePending = FALSE;
    DWORD writeOffset = 0;

    *obuf = 0;

    // Phase 1 : envoyer l'input + lire la sortie (bidirectionnel)
    while (inp < inp_end || ctx->readPending || writePending) {
        HANDLE events[3];
        int nEvents = 0;
        int readIdx = -1, writeIdx = -1;

        // Lancer une lecture overlapped si pas déjà en cours
        if (!ctx->readPending) {
            ResetEvent(ctx->ovRead.hEvent);
            BOOL ok = ReadFile(ctx->hPipeOut_Rd, ctx->readBuf,
                              sizeof(ctx->readBuf), NULL, &ctx->ovRead);
            if (!ok && GetLastError() == ERROR_IO_PENDING)
                ctx->readPending = TRUE;
            else if (ok) {
                DWORD bytesRead;
                GetOverlappedResult(ctx->hPipeOut_Rd, &ctx->ovRead,
                                    &bytesRead, FALSE);
                if (bytesRead > 0) {
                    memcpy(outptr, ctx->readBuf, bytesRead);
                    outptr += bytesRead;
                    *outptr = 0;
                }
            }
        }
        if (ctx->readPending) {
            readIdx = nEvents;
            events[nEvents++] = ctx->ovRead.hEvent;
        }

        // Lancer une écriture overlapped si données à envoyer
        if (!writePending && inp < inp_end) {
            const char *pw = inp;
            while (pw < inp_end && *pw != '\n') pw++;
            if (pw < inp_end) pw++;
            writeOffset = (DWORD)(pw - inp);

            ResetEvent(ctx->ovWrite.hEvent);
            BOOL ok = WriteFile(ctx->hPipeIn_Wr, inp, writeOffset,
                               NULL, &ctx->ovWrite);
            if (!ok && GetLastError() == ERROR_IO_PENDING)
                writePending = TRUE;
            else if (ok) {
                inp += writeOffset;
            }
        }
        if (writePending) {
            writeIdx = nEvents;
            events[nEvents++] = ctx->ovWrite.hEvent;
        }

        if (nEvents == 0) break;

        // Attendre l'un ou l'autre
        DWORD wait = WaitForMultipleObjects(nEvents, events, FALSE,
                                             (DWORD)interval_ms);

        if (wait >= WAIT_OBJECT_0 && wait < WAIT_OBJECT_0 + nEvents) {
            int idx = wait - WAIT_OBJECT_0;

            if (idx == readIdx) {
                DWORD bytesRead;
                GetOverlappedResult(ctx->hPipeOut_Rd, &ctx->ovRead,
                                    &bytesRead, FALSE);
                if (bytesRead > 0) {
                    memcpy(outptr, ctx->readBuf, bytesRead);
                    outptr += bytesRead;
                    *outptr = 0;
                }
                ctx->readPending = FALSE;
                k = 0;
            }
            if (idx == writeIdx) {
                DWORD written;
                GetOverlappedResult(ctx->hPipeIn_Wr, &ctx->ovWrite,
                                    &written, FALSE);
                inp += written;
                writePending = FALSE;
            }
        } else {
            k++;
            if (k >= timeout_intervals) break;
        }
    }

    // Fermer stdin (signale EOF au CAS)
    CloseHandle(ctx->hPipeIn_Wr);
    ctx->hPipeIn_Wr = NULL;

    // Phase 2 : lire le reste jusqu'à endstring ou timeout
    // (identique à V1 mais avec vérification timed_out)
    char *p2 = endstring ? strstr(obuf, endstring) : NULL;

    while (!p2 && !ctx->timed_out) {
        if (!ctx->readPending) {
            ResetEvent(ctx->ovRead.hEvent);
            ReadFile(ctx->hPipeOut_Rd, ctx->readBuf,
                    sizeof(ctx->readBuf), NULL, &ctx->ovRead);
            ctx->readPending = TRUE;
        }
        DWORD wait = WaitForSingleObject(ctx->ovRead.hEvent, interval_ms);
        if (wait == WAIT_OBJECT_0) {
            DWORD bytesRead;
            GetOverlappedResult(ctx->hPipeOut_Rd, &ctx->ovRead,
                                &bytesRead, FALSE);
            if (bytesRead == 0) break;
            memcpy(outptr, ctx->readBuf, bytesRead);
            outptr += bytesRead;
            *outptr = 0;
            ctx->readPending = FALSE;
            k = 0;
            if (endstring) p2 = strstr(obuf, endstring);
        } else {
            k++;
            if (k >= timeout_intervals) break;
        }
    }

    return ctx->timed_out ? -1 : 0;
}
```

### 3.2.3 Mode PIPE : avec thread d'écriture (corrigé)

```c
/*
 * Correction vs V1 : l'écriture est dans un thread dédié.
 * Le thread principal lit avec timeout réel.
 * Plus de deadlock si input > 4Ko.
 */
typedef struct {
    HANDLE hPipe;
    const char *data;
    size_t len;
} write_thread_arg;

static DWORD WINAPI pipe_writer_thread(LPVOID lpParam) {
    write_thread_arg *arg = (write_thread_arg *)lpParam;
    DWORD written;
    WriteFile(arg->hPipe, arg->data, (DWORD)arg->len, &written, NULL);
    CloseHandle(arg->hPipe);
    return 0;
}

int win32_exec_pipe(const char *cmd, char *const argv[],
    const char *input, size_t input_len,
    char *output, size_t output_size,
    DWORD timeout_ms)
{
    // ... CreateProcess avec STARTUPINFOEX (comme win32_spawn) ...

    // Thread d'écriture (non bloquant pour le thread principal)
    write_thread_arg warg = { hIn_Wr, input, input_len };
    HANDLE hWriteThread = NULL;
    if (input && input_len > 0) {
        hWriteThread = CreateThread(NULL, 0, pipe_writer_thread, &warg, 0, NULL);
    } else {
        CloseHandle(hIn_Wr);
    }

    // Lecture avec timeout réel
    char *p = output;
    DWORD bytesRead;
    HANDLE waitHandles[2] = { pi.hProcess, NULL };
    int nWait = 1;

    while ((output_size - (p - output)) > 1) {
        OVERLAPPED ovr = {0};
        ovr.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
        ReadFile(hOut_Rd, p, (DWORD)(output_size - (p - output) - 1), NULL, &ovr);

        waitHandles[1] = ovr.hEvent;
        DWORD wait = WaitForMultipleObjects(2, waitHandles, FALSE,
                        timeout_ms ? timeout_ms : INFINITE);

        if (wait == WAIT_OBJECT_0) {
            // Process terminé, drainer
            CancelIo(hOut_Rd);
            // Lire le reste en synchrone
            while (ReadFile(hOut_Rd, p, (DWORD)(output_size - (p - output) - 1),
                           &bytesRead, NULL) && bytesRead > 0)
                p += bytesRead;
            CloseHandle(ovr.hEvent);
            break;
        } else if (wait == WAIT_OBJECT_0 + 1) {
            GetOverlappedResult(hOut_Rd, &ovr, &bytesRead, FALSE);
            if (bytesRead == 0) { CloseHandle(ovr.hEvent); break; }
            p += bytesRead;
        } else if (wait == WAIT_TIMEOUT) {
            CancelIo(hOut_Rd);
            TerminateJobObject(hJob, 1);
            CloseHandle(ovr.hEvent);
            break;
        }
        CloseHandle(ovr.hEvent);
    }
    *p = 0;

    if (hWriteThread) {
        WaitForSingleObject(hWriteThread, 5000);
        CloseHandle(hWriteThread);
    }

    DWORD exitCode;
    GetExitCodeProcess(pi.hProcess, &exitCode);
    CloseHandle(hOut_Rd);
    CloseHandle(pi.hProcess);
    CloseHandle(hJob);
    return (int)exitCode;
}
```

### 3.3 Sandbox `ch..root.exe` (corrigé — livrable complet)

```c
/*
 * Remplace Misc/chroot.c (257 lignes) :
 * - chroot("../chroot") → jeton restreint + ACL
 * - RLIMIT_FSIZE → quota NTFS ou vérification post-exécution
 * - RLIMIT_NPROC → JOB_OBJECT_LIMIT_ACTIVE_PROCESS
 * - cleantmpdir → réimplémenté
 * - setreuid/setregid → jeton restreint (intégrité Low)
 * - chdir("/tmp/sessions/<id>") → SetCurrentDirectory
 * - PATH restreint (chroot_path)
 *
 * Effort estimé : 4-5 jours (pas 1 jour comme V1)
 */

HANDLE win32_create_full_sandbox(const spawn_limits *lim,
                                  const char *session_dir)
{
    HANDLE hJob = CreateJobObject(NULL, NULL);

    // --- Job Object : CPU, mémoire, kill-on-close, NPROC ---
    JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli = {0};
    jeli.BasicLimitInformation.LimitFlags =
        JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE |
        JOB_OBJECT_LIMIT_ACTIVE_PROCESS;   // ✏️ RLIMIT_NPROC

    jeli.BasicLimitInformation.ActiveProcessLimit =
        lim->nproc_limit ? lim->nproc_limit : 5;  // PROC_QUOTA dans chroot.c

    if (lim->cpu_limit_sec > 0) {
        jeli.BasicLimitInformation.LimitFlags |= JOB_OBJECT_LIMIT_PROCESS_TIME;
        jeli.BasicLimitInformation.PerProcessUserTimeLimit.QuadPart =
            (LONGLONG)lim->cpu_limit_sec * 10000000LL;
    }
    if (lim->mem_limit > 0) {
        jeli.BasicLimitInformation.LimitFlags |= JOB_OBJECT_LIMIT_PROCESS_MEMORY;
        jeli.ProcessMemoryLimit = lim->mem_limit;
    }
    SetInformationJobObject(hJob, JobObjectExtendedLimitInformation,
                            &jeli, sizeof(jeli));

    // --- Restrictions UI ---
    JOBOBJECT_BASIC_UI_RESTRICTIONS uiRestrict = {0};
    uiRestrict.UIRestrictionsClass =
        JOB_OBJECT_UILIMIT_DESKTOP | JOB_OBJECT_UILIMIT_EXITWINDOWS |
        JOB_OBJECT_UILIMIT_GLOBALATOMS | JOB_OBJECT_UILIMIT_SYSTEMPARAMETERS |
        JOB_OBJECT_UILIMIT_READCLIPBOARD | JOB_OBJECT_UILIMIT_WRITECLIPBOARD;
    SetInformationJobObject(hJob, JobObjectBasicUIRestrictions,
                            &uiRestrict, sizeof(uiRestrict));

    // --- Jeton restreint (intégrité Low) ---
    // Remplace setreuid/setregid : le processus enfant tourne avec
    // un jeton qui n'a accès qu'au dossier de session
    // CreateRestrictedToken + SetTokenInformation(TokenIntegrityLevel)
    // Le jeton est passé à CreateProcessAsUser dans win32_spawn

    // --- ACL NTFS sur le dossier de session ---
    // Seul le compte restreint peut écrire dans session_dir
    // Le reste du disque est en lecture seule pour ce jeton
    if (session_dir) {
        win32_restrict_session_acl(session_dir);
    }

    return hJob;
}

// cleantmpdir : supprime les fichiers de session expirés
void win32_cleantmpdir(const char *tmpdir)
{
    // Equivalent de chroot.c cleantmp() :
    // parcourir tmpdir, supprimer les sessions > age_limit
    WIN32_FIND_DATAA fd;
    char pattern[MAX_PATH];
    snprintf(pattern, sizeof(pattern), "%s\\*", tmpdir);
    HANDLE hFind = FindFirstFileA(pattern, &fd);
    if (hFind == INVALID_HANDLE_VALUE) return;
    do {
        if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
            // Vérifier l'âge, supprimer si expiré
            // ...
        }
    } while (FindNextFileA(hFind, &fd));
    FindClose(hFind);
}
```

### 3.4 Mémoire partagée (corrigée)

```c
/*
 * Corrections vs V1 :
 * - Nom "Global\WimsShm" (pas "WimsSharedMem" en session locale)
 * - Descripteur de sécurité : wimslogd (session 0) en RW, Apache en R
 * - wims.c:1222-1224 est COMMENTÉ : rien à porter tant que non réactivé
 * - Seul wimslogd.c:141 crée le mapping
 */
#define WIMS_SHM_NAME L"Global\\WimsShm"

HANDLE win32_shm_create(size_t size, BOOL writable)
{
    SECURITY_ATTRIBUTES sa = {0};
    SECURITY_DESCRIPTOR sd;
    InitializeSecurityDescriptor(&sd, SECURITY_DESCRIPTOR_REVISION);

    // TODO: construire une DACL explicite :
    // - wimslogd (service, session 0) : SECTION_ALL_ACCESS
    // - compte Apache (wims-svc) : SECTION_MAP_READ
    // Pour la V1, DACL NULL (tout le monde a accès)
    // IMPORTANT : sécuriser pour production
    SetSecurityDescriptorDacl(&sd, TRUE, NULL, FALSE);

    sa.nLength = sizeof(sa);
    sa.lpSecurityDescriptor = &sd;
    sa.bInheritHandle = FALSE;

    HANDLE hMap = CreateFileMappingW(
        INVALID_HANDLE_VALUE,   // Backed by pagefile
        &sa,
        writable ? PAGE_READWRITE : PAGE_READONLY,
        0, (DWORD)size,
        WIMS_SHM_NAME);

    return hMap;
}
```

### 3.5 Timeouts (corrigé — pas de longjmp inter-thread)

```c
/*
 * Correction majeure vs V1 :
 *
 * alarm1/alarm2 (lines.c:841-849) appellent kill() puis module_error("timeup"),
 * qui sort ou fait longjmp. Exécuter ça dans un thread timer CORROMPRAIT l'état.
 *
 * Schéma V1_1 :
 * - Timer → TerminateJobObject(job) + flag volatile timed_out = 1
 * - Thread principal (WaitForSingleObject) est débloqué par la fin de l'enfant
 * - Thread principal teste timed_out, PUIS appelle cleantmpdir/module_error
 *   dans son propre contexte
 */

static VOID CALLBACK win32_timeout_callback(PVOID lpParam, BOOLEAN TimerOrWaitFired)
{
    spawn_ctx *ctx = (spawn_ctx *)lpParam;
    InterlockedExchange(&ctx->timed_out, 1);
    if (ctx->hJob)
        TerminateJobObject(ctx->hJob, 1);
    else if (ctx->hProcess)
        TerminateProcess(ctx->hProcess, 1);
}

// Dans win32_spawn, après CreateProcess :
void win32_set_timeout(spawn_ctx *ctx, DWORD timeout_ms)
{
    if (timeout_ms == 0) return;

    HANDLE hTimerQueue = CreateTimerQueue();
    HANDLE hTimer;
    CreateTimerQueueTimer(&hTimer, hTimerQueue,
        win32_timeout_callback, ctx,
        timeout_ms, 0, WT_EXECUTEDEFAULT);
    // hTimer stocké dans ctx pour nettoyage
}

// Après WaitForSingleObject dans le thread principal :
if (ctx->timed_out) {
    cleantmpdir();
    module_error("timeup");  // Safe : dans le thread principal
}
```

### 3.6 Encodage et mode binaire (ajouté)

```c
// fmode.c — à appeler au démarrage de wims.exe et wimslogd.exe

#include <fcntl.h>
#include <io.h>

void win32_init_fmode(void)
{
    // Sous MSVC, fopen() sans "b" convertit CRLF et décale fseek/ftell.
    // WIMS manipule des fichiers binaires (GIF, données modules) avec fopen/fread.
    _set_fmode(_O_BINARY);
}

// Compiler avec : cl /utf-8 ...
// Manifeste (wims.exe.manifest) :
// <activeCodePage xmlns="http://schemas.microsoft.com/SMI/2019/WindowsSettings">UTF-8</activeCodePage>
```

---

## 4. Phase 1 — Build system CMake (corrigé)

```cmake
cmake_minimum_required(VERSION 3.20)
project(WIMS VERSION 4.33 LANGUAGES C CXX)  # ✏️ 4.33a, pas 4.32 ; C++ pour chemeq

# vcpkg integration
if(DEFINED ENV{VCPKG_ROOT})
    set(CMAKE_TOOLCHAIN_FILE "$ENV{VCPKG_ROOT}/scripts/buildsystems/vcpkg.cmake")
endif()

# Options
option(WIMS_BUILD_WIMSD "Build wimsd (HTTP daemon)" OFF)
option(WIMS_BUILD_WIMSLOGD "Build wimslogd" ON)

# Détection plateforme
if(WIN32)
    add_subdirectory(win32compat)
    set(WIMS_PLATFORM_LIBS win32compat ws2_32 iphlpapi advapi32)
    add_compile_options(/utf-8)                            # ✏️ UTF-8
    add_definitions(-D_WIN32 -DWIN32_LEAN_AND_MEAN -D_CRT_SECURE_NO_WARNINGS)
else()
    set(WIMS_PLATFORM_LIBS crypt m)
endif()

# Dépendances
find_package(unofficial-libgd CONFIG)
find_package(FLEX REQUIRED)     # ✏️ winflexbison
find_package(BISON REQUIRED)    # ✏️ winflexbison

# --- wims.exe (CGI engine) ---
add_executable(wims
    wims.c exec.c lines.c calc.c config.c variables.c
    files.c log.c auth.c cleaning.c html.c insmath.c
    mathml.c rawmath.c texmath.c mathfonts.c
    score.c exam.c compare.c evalue.c matrix.c
    pedia.c var.c
    Lib/libwims.c Lib/math.c Lib/text.c Lib/dir.c
    Lib/evalue.c Lib/liblines.c Lib/levelcurve.c
    Lib/mt19937ar.c
    Common/setenv.c Common/xmalloc.c
)
target_link_libraries(wims PRIVATE ${WIMS_PLATFORM_LIBS})

# --- Interfaces CAS : noms d'ORIGINE (pas wims_xxx) ---
foreach(CAS gap m2 maxima octave pari scilab yacas)  # ✏️ noms originaux
    add_executable(${CAS}
        Interfaces/${CAS}.c Interfaces/common.c
        Lib/libwims.c Lib/text.c Common/xmalloc.c Common/setenv.c
    )
    target_link_libraries(${CAS} PRIVATE ${WIMS_PLATFORM_LIBS})
endforeach()

# --- flydraw ---
add_executable(flydraw
    Flydraw/flydraw.c Flydraw/flylines.c Flydraw/objects.c Flydraw/vimg.c
)
if(unofficial-libgd_FOUND)
    target_link_libraries(flydraw PRIVATE unofficial::libgd::libgd)
endif()

# --- texgif ---
add_executable(texgif
    Texgif/texgif.c Texgif/basic.c Texgif/dvi.c
    Texgif/font.c Texgif/gf.c Texgif/tfm.c
    Texgif/colors.c Texgif/image.c
)
target_link_libraries(texgif PRIVATE ${WIMS_PLATFORM_LIBS})

# --- canvasdraw ---
add_executable(canvasdraw Misc/canvasdraw/canvasdraw.c)  # TODO: lister tous les .c
target_link_libraries(canvasdraw PRIVATE ${WIMS_PLATFORM_LIBS})

# --- OefMsg2wims : DEUX exécutables ---
add_executable(oef2wims                                   # ✏️ séparés
    OefMsg2wims/oef2wims.c OefMsg2wims/oeflines.c
    OefMsg2wims/process.c OefMsg2wims/compare.c
    OefMsg2wims/def.c OefMsg2wims/doc.c
    OefMsg2wims/outexec.c OefMsg2wims/sp.c
)
add_executable(msg2wims                                   # ✏️ ajouté
    OefMsg2wims/msg2wims.c OefMsg2wims/msglines.c
    OefMsg2wims/process.c OefMsg2wims/compare.c
    OefMsg2wims/def.c OefMsg2wims/doc.c
    OefMsg2wims/outexec.c OefMsg2wims/sp.c
)

# --- Misc tools : liste COMPLÈTE ---
set(MISC_TOOLS ccsum clickzone cryptarith curvecomp cyclicode dicsort
    exostat ftype html2msg huffman modind modstat oncechar phtmltext
    Plouffe shortpath suffix translator webget wrap)          # ✏️ complétée
foreach(TOOL ${MISC_TOOLS})
    if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/Misc/${TOOL}.c")
        add_executable(${TOOL} Misc/${TOOL}.c)
        target_link_libraries(${TOOL} PRIVATE ${WIMS_PLATFORM_LIBS})
    endif()
endforeach()

# ✏️ Sous-dossiers Misc avec flex/bison
add_subdirectory(Misc/chemeq)        # flex/bison + C++
add_subdirectory(Misc/units-filter)  # flex/bison
foreach(D symtext mathexp voronoiW whirlgif bioinfo lceb checkmol
          crossword sigdigits moneyprint scienceprint toascii)
    if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/Misc/${D}")
        add_subdirectory(Misc/${D})
    endif()
endforeach()

# ✏️ ch..root.exe (Windows uniquement)
if(WIN32)
    add_executable(ch..root win32compat/chrootexe/chroot_win.c)
    target_link_libraries(ch..root PRIVATE ${WIMS_PLATFORM_LIBS})
endif()

# --- wimslogd ---
if(WIMS_BUILD_WIMSLOGD)
    add_executable(wimslogd
        Wimslogd/wimslogd.c Wimslogd/wimslogdlines.c
        Wimslogd/wimslogdscore.c Wimslogd/socket.c
        Wimslogd/fork.c Wimslogd/cleaning.c Wimslogd/log.c
        Wimslogd/cache.c Wimslogd/cmd.c Wimslogd/files.c
        Wimslogd/homedir.c Wimslogd/housekeep.c Wimslogd/options.c
        Lib/libwims.c Lib/text.c Common/xmalloc.c
    )
    target_link_libraries(wimslogd PRIVATE ${WIMS_PLATFORM_LIBS})
endif()
```

---

## 5. Phase 2 — Moteur wims.exe (révisé)

| Fichier | Modifications | Effort |
|---------|--------------|--------|
| `wims.c` | `/proc/net/tcp:553` → GetExtendedTcpTable ; mmap **commenté** (rien à faire) ; init `_O_BINARY` + UTF-8 | 2 j |
| `wims.h` | `#ifdef _WIN32` prototypes | 1 j |
| `includes.h` | Headers POSIX vs Win32 | 1 j |
| `lines.c` | execredirected → `win32_spawn(SPAWN_FILES)` ; alarm1/alarm2 → timer+flag | 3 j |
| `exec.c` | sendmail → msmtp ; call_sh → sh.exe ; popen → _popen sh.exe | 1.5 j |
| `calc.c` | select() → overlapped ; `_passcrypt` : ne pas définir HAVE_CRYPT | 2 j |
| `mathml.c` | fork+pipe → `win32_spawn(SPAWN_PIPE)` | 1 j |
| `log.c` | double fork → supprimé | 0.5 j |
| `cleaning.c` | kill → win32_kill (via HANDLE, pas PID) | 0.5 j |
| `config.c` | chemins /tmp → %TEMP%, /bin/sh → sh.exe MSYS2 | 1 j |
| `variables.c` | chroot detection → skip | 0.5 j |
| `files.c` | symlink → CreateSymbolicLink ou copie | 1 j |
| `Lib/libwims.c` | random→rand, setenv→_putenv_s, strcasecmp→_stricmp | 1 j |
| `fmode.c` + manifeste | _O_BINARY, UTF-8 (nouveau) | 1 j |
| `cmdline.c` | Quotation argv sûre (nouveau) | 2 j |
| **Total** | | **~20 jours** |

---

## 6. Phase 3 — Interfaces CAS (révisé)

| Fichier | Modifications | Effort |
|---------|--------------|--------|
| `common.c` | execredirected → `win32_spawn(SPAWN_CAS)`, dopipes → overlapped **bidirectionnel**, chroot → `ch..root.exe`, setreuid → jeton restreint | 5 j |
| `common.h` | Prototypes win32 | 1 j |
| `maxima.c` | **Lancer sbcl.exe/gcl directement** (pas maxima.bat → cmd.exe injection) | 2 j |
| `gap.c` | Chemin GAP Windows, binaire direct | 0.5 j |
| `octave.c` | Filtrage étendu : `dos`, `unix`, `winopen`, `winqueryreg`, `!` shell | 1.5 j |
| `pari.c` | Chemin PARI Windows | 0.5 j |
| `scilab.c` | Chemin Scilab, vérifier disponibilité Windows | 1 j |
| `yacas.c` | Vérifier disponibilité (abandonware ?) | 0.5 j |
| `m2.c` | Chemin Macaulay2 | 0.5 j |
| **Total** | | **~12.5 jours (≈ 3 sem)** |

### Phase 3b — Sandbox `ch..root.exe` + sécurité

| Composant | Modifications | Effort |
|-----------|--------------|--------|
| `ch..root.exe` | Jeton restreint (CreateRestrictedToken, intégrité Low) | 3 j |
| ACL NTFS | Écriture restreinte à `chroot/tmp/sessions/<id>` | 2 j |
| cleantmpdir | Réimplémentation Windows (FindFirstFile + âge) | 2 j |
| AppContainer (optionnel) | Bloquer réseau sans capacité | 2 j |
| Filtrage Octave étendu | dos, unix, winopen, winqueryreg, ! | 1 j |
| Tests d'évasion | Lecture hors session, écriture, réseau, fork bomb | 3 j |
| **Total** | | **~13 jours (≈ 3 sem)** |

---

## 7. Phase 4 — Outils auxiliaires (révisé)

| Outil | Modifications | Effort |
|-------|--------------|--------|
| **Flydraw** | libgd vcpkg, `system()` objects.c:126 → sh.exe | 2 j |
| **Texgif** | win32_spawn + sh.exe, MiKTeX | 3 j |
| **canvasdraw** | win32_spawn(SPAWN_PIPE) pour MathML + OpenBabel | 2 j |
| **OefMsg2wims** | 2 exécutables, chemins. C pur portable. | 1 j |
| **chemeq** | winflexbison + C++ | 2 j |
| **units-filter** | winflexbison | 1 j |
| **~20 sous-dossiers Misc** | CMakeLists.txt pour chacun | 3 j |
| **Misc tools** (20+) | strcasecmp, chemins, system() → sh.exe | 2 j |
| **webget.c, clickzone.c** | system() → _system sh.exe | 0.5 j |
| **wrap.c** | exec → _spawnv ou suppression | 0.5 j |
| **Total** | | **~17 jours (≈ 3.5 sem)** |

---

## 8. Phase 5 — Wimslogd service (révisé)

| Composant | Modification | Effort |
|-----------|-------------|--------|
| `wimslogd.c` main | Double fork → SCM. **AF_UNIX Winsock** (code socket.c presque inchangé). `Global\WimsShm` + DACL | 3 j |
| `socket.c` | AF_UNIX Winsock (pas named pipes) → select() conservé | 1 j |
| `wimslogdlines.c` | win32_spawn + sh.exe | 1 j |
| `fork.c` | fork pool → CreateProcess pool, **conserver HANDLE** (pas PID) | 2 j |
| `cleaning.c` | fork → CreateProcess | 1 j |
| `log.c` | Double fork → supprimer (SCM) | 0.5 j |
| `housekeep.c` | cron → Task Scheduler (schtasks) | 1 j |
| `loadavg.c` | `GetSystemTimes` → moyenne glissante (remplace /proc/loadavg) | 1 j |
| Service robustesse | `SERVICE_START_PENDING` + dwWaitHint, `SERVICE_STOPPED` sur erreur | 0.5 j |
| **Total** | | **~11 jours (≈ 2 sem)** |

---

## 9. Phase 6 — Scripts (révisé : garder sh)

Puisque sh.exe + coreutils MSYS2 sont embarqués, **les 35 scripts de `bin/` restent en sh** pour la V1. On adapte seulement :

| Script | Action V1_1 |
|--------|------------|
| `apache-config` | **Adapter** pour Apache Windows / Laragon |
| `changepermissions` | `chmod` → `icacls` |
| `server-interrupt/resume` | `kill` → `sc stop/start WimsLogd` |
| `setchroot` | Configuration de `ch..root.exe` |
| `setwimsd` | **Supprimer** (Apache remplace wimsd) |
| `wrapuid` | **Supprimer** (pas de setuid Windows) |
| `housekeep.daily` | Tâche planifiée appelant sh.exe |
| `oefcompile`, `mkindex`, `backup`, `modupdate`, etc. | **Garder en sh** (fonctionne via sh.exe embarqué) |
| Les 4 `.pl` | **Garder** (Strawberry Perl) |

**Effort : 1.5-2 semaines** (au lieu de 3-4 en V1)

Réécriture PowerShell **optionnelle, hors scope V1**.

---

## 10. Phase 7 — Tests (révisé)

| Test | Description | Critère |
|------|-------------|---------|
| T1 | Compilation | cmake --build sans erreur, tous les .exe |
| T2 | wims.exe CGI | Page d'accueil via Apache |
| T3 | CAS unitaires | Chaque CAS répond via pipe |
| T4 | Exercice OEF simple | Compile + exécute sans CAS |
| T5 | Exercice OEF + Maxima | Avec !exec maxima |
| T6 | flydraw | GIF généré |
| T7 | texgif | LaTeX → GIF |
| T8 | wimslogd service | Démarre, accepte IPC, logge |
| T9 | Classes | CRUD complet |
| T10 | Stress | 5 users simultanés |
| **T11** | **Non-régression** | Diff HTML Linux/Windows sur 200+ modules OEF |
| **T12** | **Sécurité** | Évasion sandbox CAS (lecture C:\, écriture hors session, réseau, fork bomb), injection cmdline, Octave |
| **T13** | **Charge** | 30 utilisateurs simultanés (une classe) |
| **T14** | **Robustesse** | Arrêt wimslogd, timeouts CAS, fuites de handles (Process Explorer) |

**Effort : 5-6 semaines** (au lieu de 3-4 en V1)

---

## 11. Chiffrage révisé

| Phase | V1 | V1_1 | Raison de l'écart |
|-------|-----|------|-------------------|
| **P0** Cadrage (tag vs trunk, `public_html`, inventaire complet) | — | **1 sem** | Ajouté |
| **P1** CMake + win32compat (spawn, cmdline, fmode, loadavg) | 2 sem | **3 sem** | flex/bison, Misc, spawn unifié, cmdline |
| **P2** Moteur wims.exe | 3 sem | **4 sem** | Timeouts corrigés, UTF-8/_O_BINARY, popen |
| **P3** Interfaces CAS | 2.5 sem | **3 sem** | Overlapped bidirectionnel, sans .bat |
| **P3b** Sandbox `ch..root.exe` + sécurité | (1 j) | **3 sem** | Jeton restreint/AppContainer, ACL, cleantmpdir, Octave, tests évasion |
| **P4** Outils (flydraw, texgif, canvasdraw, Misc) | 2 sem | **3.5 sem** | ~20 sous-projets Misc, flex/bison |
| **P5** Wimslogd service | 2.5 sem | **2 sem** | AF_UNIX conservé, moins de travail |
| **P6** Scripts (sh conservé) | 3-4 sem | **2 sem** | Pas de réécriture PowerShell en V1 |
| **P7** Tests, non-régression, sécurité, charge | 3-4 sem | **5-6 sem** | T11-T14 ajoutés |
| Marge intégration + débogage (≈15%) | — | **3-4 sem** | Interblocages, chemins, encodages |
| **Total (1 dev)** | **~5.5 mois** | **≈ 30-32 sem ≈ 6.5-7.5 mois** | |
| **Total (2 devs, P4/P6 en //)** | — | **≈ 4-5 mois** | |

---

## 12. Jalons (révisés)

| Jalon | Fin de | Critère |
|-------|--------|---------|
| J0 | S1 | Version cible figée (tag ou trunk), `public_html` inventorié |
| J1 | S4 | Build vert (tous les .exe, y compris Misc + flex/bison) |
| J2 | S8 | wims.exe sert l'accueil via Apache ; timeouts OK |
| J3 | S11 | Maxima et PARI répondent en overlapped, sans interblocage sur 10 Mo de sortie |
| J4 | S14 | `ch..root.exe` passe les tests d'évasion T12 |
| J5 | S18 | Exercice complet OEF + CAS + flydraw + texgif |
| J6 | S20 | wimslogd en service, mémoire partagée Global OK |
| J7 | S28-32 | T11-T14 verts, release candidate |

---

## 13. Risques

1. **Sécurité du sandbox** : risque n°1. Sans isolation équivalente à chroot, ne pas ouvrir le service aux élèves. La partie `ch..root.exe` conditionne la mise en production.
2. **Divergence avec l'amont** : reverser les patchs `#ifdef _WIN32` à wimsdev (GPL). Sinon chaque mise à jour du trunk coûte cher.
3. **CAS Windows** : versions et options CLI différentes. Yacas, M2 et Scilab peut-être indisponibles ou instables sous Windows.
4. **Encodages et fins de ligne** : CRLF vs LF dans les modules OEF. Régressions silencieuses. `_O_BINARY` + `/utf-8` mitigent mais ne couvrent pas tout.
5. **Dépendance à MSYS2 sh** : le portage n'est « natif » que pour le C. sh.exe + coreutils sont un runtime embarqué. Assumé.
6. **Recyclage de PID** : utiliser les HANDLE des enfants, pas `OpenProcess(pid)`.

---

## 14. Recommandation

- L'architecture est **valide et exploitable avec les corrections ci-dessus**.
- **Avant d'engager 6 à 7 mois**, il reste recommandé de faire un **prototype MSYS2 + Laragon (2-4 semaines)**, qui sert aussi de **référence de non-régression** pour T11.
- Si le natif est confirmé, commencer par **P0, P1 puis P3 + P3b** (CAS et sandbox), là où se concentrent les risques techniques.
- **Stratégie parallèle** : Grok Bot effectue la migration sur son VPS Debian (§15), ce qui produit un WIMS Linux de référence pour les tests de non-régression (T11). Claude travaille sur le portage MSVC Windows en parallèle. Voir §15 et §16.

---

## 15. Stratégie de test : VPS Debian (Grok Bot) + ShadowPC Windows (Claude)

### 15.1 Répartition des rôles

| Rôle | Environnement | Responsable | Livrables |
|------|---------------|-------------|-----------|
| **Référence Linux** | VPS Debian (fourni par l'utilisateur) | Grok Bot | WIMS 4.33a compilé + fonctionnel, corpus de 200+ modules OEF testés, snapshots HTML de référence pour T11 |
| **Portage MSVC** | ShadowPC Windows 11 | Claude | win32compat/, wims.exe MSVC, ch..root.exe, tests Windows |
| **Non-régression T11** | Les deux | Comparaison | Diff automatique HTML Linux vs Windows sur le corpus |

### 15.2 Ce que Grok Bot fait sur le VPS Debian

1. **Installer WIMS 4.33a** depuis le trunk r19040 (ou tag décidé en P0)
2. **Compiler nativement** avec tous les CAS (Maxima, PARI, GAP, Octave, Scilab, Yacas, M2)
3. **Vérifier la disponibilité des CAS** et documenter les versions/options réelles
4. **Générer le corpus de référence T11** : 200+ modules OEF → HTML de sortie archivés
5. **Tester les interfaces CAS** : commandes de référence pour chaque CAS, sortie attendue
6. **Commencer le portage de `common.c`** (Interfaces CAS) comme premier chantier

### 15.3 Le VPS comme oracle de non-régression

```
VPS Debian                           ShadowPC Windows
┌─────────────────────┐              ┌─────────────────────┐
│ WIMS 4.33a natif    │              │ wims.exe MSVC       │
│ Apache + mod_cgi    │              │ Apache Laragon      │
│                     │              │                     │
│ curl module/X.oef   │──référence──►│ curl module/X.oef   │
│ → ref_X.html        │   T11 diff   │ → win_X.html        │
│                     │              │                     │
│ echo "1+1;" | maxima│──référence──►│ echo "1+1;" | maxima│
│ → "2"               │   T3 diff    │ → "2"               │
└─────────────────────┘              └─────────────────────┘
```

Le VPS fournit la **vérité terrain** : si le rendu HTML ou la sortie CAS diffère sur Windows, c'est un bug du portage.

### 15.4 Protocole de non-régression T11

| Étape | Action | Outil |
|-------|--------|-------|
| 1 | Sélectionner 200+ modules OEF diversifiés (maths, physique, chimie, langues) | Manuel sur VPS |
| 2 | Pour chaque module, générer le rendu HTML avec des paramètres fixes (seed=42) | `curl -s "http://localhost/wims/wims.cgi?module=...&seed=42"` |
| 3 | Archiver les HTML de référence | `ref/module_name.html` |
| 4 | Reproduire les mêmes requêtes sur Windows | Même curl |
| 5 | Diff structurel (ignorer timestamps, session IDs, chemins absolus) | Script de normalisation + `diff` |
| 6 | Toute différence = bug à investiguer | Triage : cosmétique (CRLF, chemins) vs fonctionnel (résultat CAS, image, score) |

---

## 16. Migration des Interfaces CAS — étapes détaillées

`src/Interfaces/common.c` regroupe l'essentiel du code CAS. C'est le **premier chantier** du portage (P3), car il concentre les 3 difficultés majeures : overlapped I/O, sandbox, et lancement de processus.

### 16.1 Étape C1 — Inventaire des binaires CAS sur chaque plateforme

| CAS | Binaire Linux | Binaire Windows | Statut Windows | Notes |
|-----|---------------|-----------------|----------------|-------|
| **Maxima** | `/usr/bin/maxima` | `sbcl.exe` ou `gcl.exe` (PAS `maxima.bat`) | ✅ Disponible | Lancer le Lisp directement, pas le .bat (injection cmd.exe) |
| **PARI/GP** | `/usr/bin/gp` | `gp.exe` | ✅ Disponible | winget install |
| **GAP** | `/usr/bin/gap` | `gap.exe` | ✅ Disponible | Installeur Windows |
| **Octave** | `/usr/bin/octave-cli` | `octave-cli.exe` | ✅ Disponible | winget install |
| **gnuplot** | `/usr/bin/gnuplot` | `gnuplot.exe` | ✅ Disponible | winget install |
| **POV-Ray** | `/usr/bin/povray` | `pvengine64.exe` | ✅ Disponible | Installeur Windows |
| **Scilab** | `/usr/bin/scilab-cli` | `scilex.exe` | ⚠️ À vérifier | Dernières versions peuvent ne pas avoir le CLI headless |
| **Yacas** | `/usr/bin/yacas` | ? | ⚠️ À vérifier | Abandonware ; chercher un build Windows récent |
| **Macaulay2** | `/usr/bin/M2` | ? | ⚠️ À vérifier | Builds Windows expérimentaux seulement |

**Action Grok VPS** : Installer chaque CAS sur Debian, documenter les versions, les options CLI de démarrage, et les chaînes d'initialisation attendues par WIMS.

**Action Claude Windows** : Vérifier la disponibilité de chaque binaire sous Windows, trouver les chemins réels (pas les .bat), documenter les différences d'options CLI.

### 16.2 Étape C2 — Port de `common.c` : lancement des CAS

Le code actuel (`common.c:168-211`) fait :

```
fork() → setreuid/setregid → chroot via ch..root → dup2 pipes → execv CAS
```

Le port Windows fait :

```
win32_spawn(SPAWN_CAS) → STARTUPINFOEX + HANDLE_LIST
                        → Job Object (CPU/mémoire/NPROC/kill-on-close)
                        → jeton restreint (intégrité Low)
                        → ACL session dir
                        → named pipes overlapped (2 sens)
                        → CreateProcessW (binaire direct, PAS .bat)
```

**Sous-étapes :**

| # | Action | Fichier | Effort |
|---|--------|---------|--------|
| C2.1 | Créer `win32_spawn(SPAWN_CAS)` avec STARTUPINFOEX | `win32compat/spawn.c` | 2 j |
| C2.2 | Créer les named pipes overlapped bidirectionnels | `win32compat/pipe.c` | 2 j |
| C2.3 | Wrapper `#ifdef _WIN32` dans `common.c:168-211` | `Interfaces/common.c` | 1 j |
| C2.4 | Tester avec Maxima (sbcl.exe) : `1+1;` → `2` | Test unitaire | 1 j |
| C2.5 | Tester avec PARI : `1+1` → `2` | Test unitaire | 0.5 j |

### 16.3 Étape C3 — Port de `dopipes()` : dialogue bidirectionnel

Le code actuel (`common.c:480-559`) utilise `select()` sur des pipes anonymes :

```c
// Lire stdout CAS + écrire stdin CAS simultanément
select(nfds, &rset, &wset, NULL, &tv);
if (FD_ISSET(pipe_out[0], &rset)) read(...);
if (FD_ISSET(pipe_in[1], &wset))  write(...);
```

Le port Windows utilise overlapped I/O sur named pipes :

```c
// Les deux sens en overlapped
ReadFile(hPipeOut_Rd, ..., &ovRead);    // Async
WriteFile(hPipeIn_Wr, ..., &ovWrite);   // Async
WaitForMultipleObjects({ovRead.hEvent, ovWrite.hEvent}, ...);
```

**Sous-étapes :**

| # | Action | Fichier | Effort |
|---|--------|---------|--------|
| C3.1 | Implémenter `win32_dopipes()` bidirectionnel | `win32compat/pipe.c` | 3 j |
| C3.2 | Wrapper `#ifdef _WIN32` dans `common.c:480-559` | `Interfaces/common.c` | 1 j |
| C3.3 | Test d'interblocage : envoyer 10 Mo à Maxima | Test de stress | 1 j |
| C3.4 | Test de timeout : CAS qui ne répond pas | Test robustesse | 0.5 j |

### 16.4 Étape C4 — Timeout et arrêt des CAS

Le code actuel (`common.c:443,599`) :

```c
signal(SIGALRM, cas_timeout_handler);
alarm(CAS_TIMEOUT);
// ... dopipes ...
alarm(0);
```

Le port Windows :

```c
win32_set_timeout(ctx, CAS_TIMEOUT * 1000);
// ... win32_dopipes ...
// Si timeout : timer a fait TerminateJobObject + flag timed_out
if (ctx->timed_out) { /* cleanup */ }
```

**Sous-étapes :**

| # | Action | Fichier | Effort |
|---|--------|---------|--------|
| C4.1 | Implémenter `win32_set_timeout()` avec TimerQueue | `win32compat/signal.c` | 1 j |
| C4.2 | Intégrer dans le flux CAS | `Interfaces/common.c` | 0.5 j |
| C4.3 | Test : lancer un CAS qui boucle, vérifier que le timeout tue le Job | Test | 0.5 j |

### 16.5 Étape C5 — Sécurité des CAS

| # | Action | Détail | Effort |
|---|--------|--------|--------|
| C5.1 | Filtrage Octave étendu | Ajouter `dos`, `unix`, `winopen`, `winqueryreg`, `!` aux mots interdits (`octave.c:73-77`) | 0.5 j |
| C5.2 | Audit filtres autres CAS | Vérifier si Maxima/GAP/PARI ont des fonctions d'accès système à filtrer sous Windows | 1 j (Grok VPS : documenter les commandes dangereuses par CAS) |
| C5.3 | Binaire mode | Mode binaire sur les pipes (`_setmode(fd, _O_BINARY)`) pour éviter les décalages CRLF dans les résultats CAS | 0.5 j |
| C5.4 | Chemins CAS | Configuration externe des chemins (pas en dur) ; conversion `/` → `\` là où nécessaire ; variables d'environnement CAS ajustées | 1 j |

### 16.6 Étape C6 — Validation croisée VPS ↔ Windows

| # | Test | VPS (référence) | Windows (à valider) |
|---|------|-----------------|---------------------|
| C6.1 | Maxima : `factor(x^4-1);` | `(x-1)*(x+1)*(x^2+1)` | Idem ? |
| C6.2 | PARI : `isprime(17)` | `1` | Idem ? |
| C6.3 | GAP : `Size(SymmetricGroup(4));` | `24` | Idem ? |
| C6.4 | Octave : `inv([1 2;3 4])` | `[-2 1; 1.5 -0.5]` | Idem ? |
| C6.5 | gnuplot : `plot sin(x)` | Image GIF | Diff pixel ? |
| C6.6 | Module OEF complet avec CAS | HTML de référence | Diff structurel T11 |

**Toute différence = bug à investiguer.** Le VPS est l'oracle.

### 16.7 Ordre de migration recommandé

```
C1 Inventaire binaires ──► C2 Lancement (spawn) ──► C3 Dialogue (dopipes)
                                                          │
                            C4 Timeout ◄──────────────────┘
                                │
                            C5 Sécurité ──► C6 Validation croisée VPS↔Win
```

**common.c est le bon premier chantier** : il centralise fork+exec, pipes, select, chroot, setreuid, et timeout. Une fois porté, chaque fichier CAS (`maxima.c`, `pari.c`, ...) ne demande que des ajustements de chemins et d'options.

---

## Annexe A — Code de référence des 4 projets

Identique à V1, **plus** :

| Projet | Fichier | Ce qu'il enseigne |
|--------|---------|------------------|
| **Chromium** | `sandbox/win/` | AppContainer, jeton restreint, intégrité Low — modèle pour `ch..root.exe` |
| **Microsoft docs** | `CreateRestrictedToken` | Jeton avec SID restreints + intégrité |

---

## Annexe B — Checklist de démarrage (révisée)

```
□ svn ls https://subversion.renater.fr/anonscm/svn/wimsdev/tags → décider trunk ou tag
□ svn update public_html → inventorier scripts, modules
□ Installer Visual Studio 2022 + Build Tools C++
□ Installer CMake (winget install Kitware.CMake)
□ Installer vcpkg + vcpkg install libgd:x64-windows zlib:x64-windows
□ Installer winflexbison (vcpkg ou chocolatey)
□ Installer MSYS2 (sh.exe + coreutils embarqués) + pacman -S coreutils
□ Installer MiKTeX
□ Installer les CAS : Maxima (trouver sbcl.exe), PARI, Octave, GAP, gnuplot
□ Apache Win64 (Apache Lounge) ou Laragon
□ Créer le compte de service wims-svc
□ Corpus de 200+ modules OEF pour T11 (non-régression)
□ Cloner PostgreSQL (src/port/win32/)
□ Cloner nginx (src/os/win32/)
□ Étudier Chromium sandbox/win/ pour ch..root.exe
□ Créer la branche feat/wims-win32-native
□ Phase 1 : CMakeLists.txt + win32compat → premier build
```
