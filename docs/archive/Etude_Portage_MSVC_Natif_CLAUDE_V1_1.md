# CLAUDE_V1_1
# Étude — Portage natif MSVC : comment ça marche et qui l'a déjà fait

**Date:** 2026-10-23 (rev 2026-10-23)  
**Objectif:** Détailler la mécanique d'un portage fork/exec → CreateProcess, et lister les projets open source qui l'ont fait pour s'en inspirer.

> **⚠️ ERRATA V1_1** — Corrige les erreurs de la V1, identifiées par relecture croisée (audit Grok) et vérification dans les sources r19040. Voir §8 pour le détail des corrections.

---

## 1. Le problème fondamental

Sur Linux/Unix, WIMS fait ceci pour chaque requête HTTP :

```c
// wimsd.c / common.c — modèle Linux
pid_t pid = fork();          // 1. Copie le processus entier (mémoire, handles, état)
if (pid == 0) {
    // Enfant : hérite TOUT automatiquement
    dup2(pipe_fd, STDOUT);   // 2. Redirige stdout vers un pipe
    execv("/path/to/maxima", args);  // 3. Remplace le binaire par Maxima
}
waitpid(pid, &status, 0);   // 4. Le parent attend la fin
```

**Sur Windows, `fork()` n'existe pas.** Windows crée un processus en une seule étape avec [`CreateProcess()`](https://learn.microsoft.com/en-us/previous-versions/ms811896(v=msdn.10)), qui prend **10 paramètres** et ne copie rien du parent — il faut tout passer explicitement.

```c
// Équivalent Windows natif
STARTUPINFO si = { sizeof(si) };
PROCESS_INFORMATION pi;
si.hStdOutput = hWritePipe;         // Passer le pipe manuellement
si.hStdInput  = hReadPipe;
si.dwFlags    = STARTF_USESTDHANDLES;

CreateProcess(
    "C:\\maxima\\bin\\maxima.exe",   // Exécutable explicite
    cmdline,                          // Arguments
    NULL, NULL,                       // Sécurité
    TRUE,                             // Hériter les handles
    CREATE_NO_WINDOW,                 // Flags
    NULL,                             // Environnement
    NULL,                             // Répertoire
    &si, &pi                          // Structures
);
WaitForSingleObject(pi.hProcess, timeout_ms);
```

### Tableau de correspondance POSIX → Win32

| POSIX (WIMS actuel) | Win32 natif | Complexité |
|---------------------|-------------|------------|
| `fork()` | `CreateProcess()` | 🔴 Réécriture totale |
| `execv()` / `execvp()` | intégré dans `CreateProcess()` | 🔴 |
| `pipe()` | `CreatePipe()` | 🟡 Direct |
| `dup2(fd, STDOUT)` | `SetStdHandle()` + `DuplicateHandle()` | 🟡 |
| `waitpid()` | `WaitForSingleObject()` | 🟢 Direct |
| `select()` sur pipes | ⚠️ **Pas WaitForMultipleObjects** — voir §3.4 | 🔴 |
| `kill(pid, SIGKILL)` | `TerminateProcess()` | 🟢 Direct |
| `signal(SIGALRM)` + `alarm()` | Timer thread ou `CreateTimerQueueTimer()` | 🟠 |
| `setrlimit(RLIMIT_CPU)` | `SetInformationJobObject()` (Job Objects) | 🟠 |
| `chroot()` | ⚠️ Pas un simple noop — voir §3.5 | 🔴 |
| `setreuid()` / `setregid()` | ⚠️ Pas un simple noop — voir §3.5 | 🟠 |
| `mmap()` (pas shmget) | `CreateFileMapping()` + `MapViewOfFile()` | 🟠 |
| `socket(AF_UNIX)` | TCP `127.0.0.1` ou Named Pipes | 🟡 |
| `random()` / `srandom()` | `rand()` / `srand()` ou Mersenne Twister | 🟢 |
| `strcasecmp()` | `_stricmp()` | 🟢 Macro |
| `/proc/net/tcp` | `GetExtendedTcpTable()` (iphlpapi.h) | 🟡 |
| `flock()` / `fcntl()` | `LockFileEx()` | 🟡 |
| `symlink()` | `CreateSymbolicLinkW()` (privilèges requis) | 🟡 |
| `call_sh()` / `popen()` | ⚠️ **Pas cmd.exe** — voir §3.6 | 🟠 |
| `/usr/sbin/sendmail` | SMTP client (blat, PowerShell, msmtp) | 🟠 |
| `libcrypt` (crypt()) | bcrypt.lib ou OpenSSL | 🟡 |

---

## 2. Projets open source qui l'ont fait — leçons à tirer

### 2.1 🏆 PostgreSQL — le modèle de référence

**Le portage le plus documenté et le plus proche de WIMS** (serveur C, fork-per-connection → CreateProcess).

- **Repo :** [github.com/postgres/postgres](https://github.com/postgres/postgres) → `src/port/` et `src/backend/postmaster/`
- **Historique :** Portage commencé en 2002, livré en PostgreSQL 8.0 (2005). [Présentation OSCON 2005](https://wiki.postgresql.org/images/4/46/OSCON2005-PortingToWindows.pdf).
- **[Mailing-list : fork/exec patch](https://www.postgrespro.com/list/thread-id/1997277)** — les patches `CreateProcess` de Claudio Natoli (2003-2004).

#### Architecture du portage PostgreSQL

```
postgres/
├── src/
│   ├── port/               ← COUCHE DE COMPATIBILITÉ
│   │   ├── win32/
│   │   │   ├── signal.c    ← Émulation POSIX signals
│   │   │   ├── timer.c     ← Émulation alarm/setitimer
│   │   │   ├── socket.c    ← Winsock wrappers
│   │   │   └── security.c  ← Remplace setuid/chroot
│   │   ├── pipe.c          ← CreatePipe wrapper
│   │   ├── exec.c          ← find_my_exec, CreateProcess
│   │   ├── kill.c          ← kill() → TerminateProcess
│   │   └── open.c          ← open() differences
│   ├── backend/
│   │   └── postmaster/
│   │       └── postmaster.c  ← fork() remplacé par CreateProcess
│   └── include/
│       └── port/
│           └── win32.h     ← Macros de compatibilité
```

#### La technique clé : "re-exec self"

PostgreSQL ne peut pas `fork()` (copier l'état mémoire). À la place :

1. Le parent **sérialise son état** dans une zone mémoire partagée ou des paramètres CLI
2. `CreateProcess` relance **le même exécutable** avec `--fork backend <shared_mem_handle>`
3. L'enfant **désérialise l'état** et reprend le travail

```c
// Pseudo-code simplifié de postmaster.c (PostgreSQL Windows)
#ifdef WIN32
// Parent
STARTUPINFO si;
snprintf(cmdline, "postgres.exe --fork=backend %d", shmem_handle);
CreateProcess(NULL, cmdline, ..., &si, &pi);

// Enfant (même exe, détecte --fork)
if (argc > 1 && strncmp(argv[1], "--fork=", 7) == 0) {
    restore_backend_state(atoi(argv[1] + 7));
    BackendRun();
}
#else
// Linux
pid = fork();
if (pid == 0) BackendRun();
#endif
```

#### Leçons pour WIMS

| Leçon PostgreSQL | Application WIMS |
|------------------|-----------------|
| Répertoire `src/port/` séparé | Créer `src/win32compat/` |
| `#ifdef WIN32` partout dans le code existant | Idem — ne pas toucher au code Linux |
| `kill()` → wrapper qui appelle `TerminateProcess` | `src/win32compat/kill.c` |
| Signaux POSIX émulés via threads | `src/win32compat/signal.c` |
| Shared memory via `CreateFileMapping` | Pour le `mmap()` de wimslogd |
| **Re-exec self** pour remplacer fork | Non nécessaire si on utilise Apache CGI (voir §3.7) |

---

### 2.2 🥈 Win32-OpenSSH — couche POSIX complète

**Port officiel Microsoft d'OpenSSH (daemon sshd, fork-per-session).**

- **Repo :** [github.com/PowerShell/openssh-portable](https://github.com/PowerShell/openssh-portable) → `contrib/win32/openssh/`
- **Releases :** Jusqu'à 10.0.0.0 (oct 2025)
- **[Wiki design](https://github.com/PowerShell/Win32-OpenSSH/wiki)**

#### Architecture

```
openssh-portable/
├── contrib/
│   └── win32/
│       └── openssh/         ← COUCHE WIN32 COMPLÈTE
├── openbsd-compat/          ← Fonctions OpenBSD absentes d'autres OS
│   ├── bsd-signal.c
│   ├── setproctitle.c
│   └── ...
└── (code OpenSSH original, avec #ifdef WIN32)
```

#### Leçons pour WIMS

| Leçon OpenSSH | Application WIMS |
|---------------|-----------------|
| `contrib/win32/` = tout le code Windows séparé | Garder les .c Windows à part |
| `openbsd-compat/` = fonctions manquantes | `posix-compat/` pour WIMS |
| `posix_spawn()` utilisé quand possible | Alternative à fork+exec pour les CAS |

---

### 2.3 🥉 nginx — architecture sans fork

**nginx ne fork PAS les workers. Il les crée avec CreateProcess dès le départ.**

- **Source :** [`src/os/win32/`](https://nginx.googlesource.com/nginx/+/refs/heads/master/src/os/win32/) — 39 fichiers
- **Approche :** Le code Linux utilise `fork()` pour les workers. Le code Windows utilise `CreateProcess()` + arguments CLI pour que chaque worker charge sa propre config.

#### Les fichiers clés de src/os/win32/

| Fichier | Rôle | Pertinent pour WIMS ? |
|---------|------|----------------------|
| `ngx_process.c` | `CreateProcess(CREATE_NO_WINDOW)` | ✅ Modèle pour lancer les CAS |
| `ngx_process_cycle.c` | Boucle master/worker | ✅ Si wimsd est conservé |
| `ngx_shmem.c` | `CreateFileMapping` / `MapViewOfFile` | ✅ Pour mmap WIMS |
| `ngx_socket.c` | Winsock wrappers | ✅ Pour wimslogd sockets |
| `ngx_service.c` | Service Windows (SCM) | ✅ Pour wimsd/wimslogd |
| `ngx_time.c` | Timers Windows | ✅ Pour alarm/SIGALRM |

#### La technique clé : "workers autonomes"

```c
// ngx_process.c (simplifié)
ngx_pid_t ngx_execute(ngx_cycle_t *cycle, ngx_exec_ctx_t *ctx) {
    STARTUPINFO         si = { sizeof(STARTUPINFO) };
    PROCESS_INFORMATION pi;
    si.dwFlags = STARTF_USESHOWWINDOW;
    si.wShowWindow = SW_HIDE;
    CreateProcess(NULL, ctx->cmdline, NULL, NULL,
                  FALSE, CREATE_NO_WINDOW, NULL, NULL, &si, &pi);
    CloseHandle(pi.hThread);
    return pi.dwProcessId;
}
```

---

### 2.4 Redis on Windows — mmap et shared memory

**Microsoft a porté Redis sur Windows (2013-2019). Le fork() servait au snapshot mémoire (BGSAVE).**

- **Repo :** [github.com/microsoftarchive/redis](https://github.com/microsoftarchive/redis) (archivé)
- **Couche :** `src/Win32_Interop/`

#### Technique : Copy-on-Write applicatif

- **v2.8 :** `CreateFileMapping()` (memory-mapped file) — le dataset Redis est dans un fichier mappé
- **v3.0 :** Utilise le pagefile système au lieu d'un fichier nommé

#### Pertinence pour WIMS

Directement pertinent : WIMS utilise `mmap()` (pas `shmget` comme écrit par erreur en V1) dans `wimslogd.c:141` et `wims.c:1224`. Le remplacement est :

```c
// Linux (wimslogd.c:141)
shmptr = mmap(0, SHM_SIZE, PROT_READ|PROT_WRITE, MAP_SHARED, mfd, 0);

// Windows
HANDLE hMapFile = CreateFileMapping(
    (HANDLE)_get_osfhandle(mfd),  // Le même fd de fichier
    NULL,
    PAGE_READWRITE,
    0, SHM_SIZE,
    NULL
);
shmptr = MapViewOfFile(hMapFile, FILE_MAP_ALL_ACCESS, 0, 0, SHM_SIZE);
```

---

## 3. Points critiques — erreurs corrigées et pièges

### 3.1 execredirected() existe en 4 versions distinctes

La V1 ne portait qu'une seule version. En réalité, le code r19040 contient **4 implémentations indépendantes** :

| # | Fichier | Lignes | Contexte | Particularités |
|---|---------|--------|----------|----------------|
| 1 | `src/lines.c` | 912-970 | **Moteur wims principal** | Redirige via fichiers (`inf`/`outf`/`errf`) OU pipes. `setrlimit`. Timeout `alarm`. |
| 2 | `src/Interfaces/common.c` | 168-226 | **Interfaces CAS** (Maxima, GAP, etc.) | Pipes bidirectionnels. `chroot` conditionnel. `setreuid/setregid`. |
| 3 | `src/Wimslogd/wimslogdlines.c` | 24-123 | **Daemon wimslogd** | Variante de lines.c avec `call_sh(wait,...)`. |
| 4 | `src/Texgif/basic.c` | 72-115 | **Texgif** (TeX → GIF) | Variante simplifiée, pas de chroot/setreuid. |

**En plus**, il y a des `fork()` directs (sans passer par `execredirected`) dans **6 fichiers** :

| Fichier | Ligne(s) | Usage |
|---------|----------|-------|
| `src/log.c` | 446, 448 | Double fork pour daemoniser l'envoi de logs |
| `src/mathml.c` | 42 | Fork + pipe pour `wims_mathml` |
| `src/Wimsd/wimsd.c` | 206 | Fork par requête HTTP entrante |
| `src/Wimslogd/wimslogd.c` | 61 | Fork pour tâches de nettoyage |
| `src/Wimslogd/cleaning.c` | 146 | Fork pour nettoyage de sessions |
| `src/Misc/canvasdraw/canvasdraw.c` | 6330, 6372 | Fork + pipe pour MathML et OpenBabel |

**→ Chacun de ces 10 sites fork doit être porté individuellement.** C'est l'erreur principale de la V1 qui n'en portait qu'un.

### 3.2 Mémoire partagée : c'est mmap, pas shmget

**Erreur V1 :** Le header de compatibilité proposait des wrappers pour `shmget/shmat/shmctl` (System V IPC).

**Réalité dans les sources r19040 :**

```c
// wimslogd.c:141 — c'est mmap(), pas shmget()
shmptr = mmap(0, SHM_SIZE, PROT_READ|PROT_WRITE, MAP_SHARED, mfd, 0);

// wims.c:1224 — idem, en lecture seule
// shmptr = mmap(0, SHM_SIZE, PROT_READ, MAP_SHARED, mfd, 0);
```

WIMS utilise `mmap()` sur un fichier partagé, pas les fonctions System V. Le remplacement correct est `CreateFileMapping()` + `MapViewOfFile()` (voir §2.4).

### 3.3 wimsd : ne pas le porter — utiliser Apache CGI

**Erreur V1 :** Proposait de porter `wimsd` (le daemon HTTP maison) avec la technique "re-exec self" de PostgreSQL.

**Correction :** C'est du travail inutile. `wimsd` n'est qu'un mini-serveur HTTP qui dispatche vers `wims.exe`. Apache fait exactement la même chose en CGI. Sur Windows :

- **Laragon** (déjà installé) fournit Apache natif avec `mod_cgi`
- Apache lance `wims.exe` via `ScriptAlias` — c'est un `CreateProcess` natif
- Pas besoin de porter le fork-per-request de wimsd

**→ Supprimer wimsd du scope. Utiliser Apache CGI.**

### 3.4 select() sur pipes : le piège Windows

**Erreur V1 :** Le tableau de correspondance disait `select()` → `WaitForMultipleObjects()`.

**Réalité :** Sur Windows, **aucune des deux ne fonctionne sur des pipes anonymes** :

- `select()` (Winsock) ne fonctionne **que sur des sockets**, pas sur des handles de pipes
- `WaitForMultipleObjects()` ne fonctionne **pas non plus** sur des anonymous pipes (ils n'ont pas d'état signalé)

Or WIMS utilise `select()` sur des pipes anonymes (`pipe_out[0]`, `pipe_in[1]`) dans `Interfaces/common.c:494,519,549` pour la communication bidirectionnelle avec les CAS.

**Solutions possibles (par ordre de préférence) :**

| Solution | Complexité | Risque de deadlock |
|----------|-----------|-------------------|
| **1. Overlapped I/O** : Créer les pipes avec `CreateNamedPipe(FILE_FLAG_OVERLAPPED)` au lieu de `CreatePipe()`. Utiliser `ReadFileEx/WriteFileEx` + `WaitForMultipleObjects` sur les events OVERLAPPED. | 🟠 Moyen | Faible |
| **2. Threads lecteur/écrivain** : Un thread dédié lit le pipe en mode bloquant et signale un event quand des données arrivent. Le thread principal fait `WaitForMultipleObjects` sur ces events. | 🟡 Simple | Moyen |
| **3. Named pipes** : Remplacer les anonymous pipes par des named pipes (`\\.\pipe\wims_cas_NNN`), qui supportent le mode overlapped nativement. | 🟠 Moyen | Faible |

**C'est le PostgreSQL `src/port/pipe.c` qui montre la bonne approche** (overlapped + events).

### 3.5 Sécurité : chroot/setreuid comme noop = danger silencieux

**Erreur V1 :** Le header proposait :

```c
#define chroot(x)      (0)   // Noop
#define setreuid(r,e)  (0)   // Noop
#define setregid(r,e)  (0)   // Noop
```

**Problème :** Cela supprime silencieusement l'isolation de sécurité sans aucun avertissement. Les CAS (Maxima, Octave, etc.) s'exécutent avec les mêmes permissions que le serveur WIMS et sans sandbox. Un exercice OEF malveillant pourrait exécuter du code arbitraire sur la machine.

**Correction :** Au minimum :

```c
#ifdef _WIN32
#define chroot(x) do { \
    fprintf(stderr, "WARNING: chroot() not available on Windows. " \
                    "CAS processes run WITHOUT sandbox.\n"); \
} while(0)

#define setreuid(r,e) do { \
    fprintf(stderr, "WARNING: setreuid() not available on Windows. " \
                    "Process runs with full user privileges.\n"); \
} while(0)
```

**Idéalement :** Utiliser les Job Objects avec des restrictions :

```c
// win32compat/sandbox.c — sandbox minimal via Job Objects
HANDLE create_cas_sandbox(void) {
    HANDLE hJob = CreateJobObject(NULL, NULL);
    JOBOBJECT_BASIC_UI_RESTRICTIONS uiRestrict = {0};
    uiRestrict.UIRestrictionsClass =
        JOB_OBJECT_UILIMIT_DESKTOP |          // Pas d'accès au desktop
        JOB_OBJECT_UILIMIT_EXITWINDOWS |      // Pas de shutdown
        JOB_OBJECT_UILIMIT_GLOBALATOMS |      // Pas de global atoms
        JOB_OBJECT_UILIMIT_SYSTEMPARAMETERS;  // Pas de modif système
    SetInformationJobObject(hJob, JobObjectBasicUIRestrictions,
                            &uiRestrict, sizeof(uiRestrict));
    // + limites CPU/mémoire (voir §1)
    return hJob;
}
```

### 3.6 Shell : cmd.exe ne marche pas

**Erreur V1 :** Le header proposait `#define DEFAULT_SHELL "cmd.exe"`.

**Réalité :** `call_sh()` dans WIMS (lines.c:1100, Texgif/basic.c:103, etc.) envoie des commandes en **syntaxe bash** :

```c
// Exemples réels dans les sources r19040 :
call_sh("rm -Rf %s/%s* >/dev/null 2>&1", session_dir, b2);        // wims.c:954
call_sh("cat %s/mail.eml | /usr/sbin/sendmail -t", tmp_dir);       // exec.c:1804
call_sh("cp `kpsewhich %s.tfm` %s", fname, tmpdir);                // Texgif/tfm.c:48
call_sh("mkdir -p %s\n...", dir);                                   // Texgif/gf.c:167
call_sh("ls %s* 2>/dev/null >/dev/null || exit;\n...", path);       // Wimslogd/log.c:89
```

`cmd.exe` ne comprend rien de tout ça (`rm`, pipes `|`, backticks, `2>/dev/null`, etc.).

**Solutions :**

| Solution | Avantage | Inconvénient |
|----------|----------|-------------|
| **1. Garder bash MSYS2** : `DEFAULT_SHELL = "C:/msys64/usr/bin/sh.exe"` | Zéro réécriture des commandes | Dépendance MSYS2 (même en build MSVC) |
| **2. Réécrire en PowerShell** | 100% natif | Énorme : il y a ~50 appels `call_sh()` + 36 scripts `bin/` |
| **3. Hybride** : bash MSYS2 pour call_sh(), PowerShell pour les scripts d'admin | Pragmatique | Deux shells à maintenir |

**Recommandation : solution 1** (bash MSYS2). Même avec un build MSVC, embarquer `sh.exe` de MSYS2 (~5 Mo) est le choix pragmatique. C'est ce que Git for Windows fait.

### 3.7 Composants oubliés en V1

| Composant | Fichier(s) | Problème | Impact |
|-----------|------------|----------|--------|
| **wimslogd** | `src/Wimslogd/wimslogd.c` | Daemon complet : double fork, sockets Unix, mmap, timer, nettoyage. Sa propre `execredirected()` et `call_sh()`. | 🔴 Portage majeur. Alternative : réécrire en service Windows. |
| **Texgif** | `src/Texgif/texgif.c`, `basic.c`, `gf.c`, `tfm.c` | Sa propre `execredirected()`. `call_sh()` avec `cp`, `mkdir -p`, `kpsewhich`. Chemins `/tmp`. | 🟠 Compiler avec MSVC + MiKTeX. Adapter les chemins. |
| **canvasdraw + OpenBabel** | `src/Misc/canvasdraw/canvasdraw.c:6330,6372` | 2 `fork()` directs : un pour MathML (pipe), un pour OpenBabel (pipe). | 🟠 Remplacer par CreateProcess. OpenBabel existe sur Windows. |
| **libcrypt** | `configure:3895`, `config.hin:62` | `HAVE_LIBCRYPT` — utilisé pour `crypt()` (hash de mots de passe). | 🟡 Remplacer par `bcrypt.lib` (Win32) ou OpenSSL. |
| **sendmail** | `exec.c:1804` | `call_sh("cat mail.eml \| /usr/sbin/sendmail -t")` | 🟠 Remplacer par msmtp (Windows natif) ou PowerShell `Send-MailMessage`. |
| **Filtrage sécurité Octave** | scripts bin/ et Interfaces | Octave doit être sandboxé pour empêcher l'exécution de code arbitraire via `system()`. | 🟠 Job Objects + filtrage des commandes Octave autorisées. |

### 3.8 Confusion MSYS2 en V1

**Erreur V1 :** Confondait "MSYS2 MinGW" (compilateur natif, PAS de fork) avec "MSYS2 MSYS" (runtime Cygwin-like, fork émulé via `msys-2.0.dll`).

**Clarification :**

| Mode MSYS2 | Shell | Compilateur | fork() | Binaires dépendent de... |
|------------|-------|-------------|--------|-------------------------|
| **MSYS** | bash MSYS2 | gcc (msys2) | ✅ Émulé (lent) | msys-2.0.dll |
| **MINGW64** | bash MSYS2 | gcc (mingw-w64) | ❌ N'existe pas | Rien (natif Win32) |
| **MSVC** | PowerShell/cmd | cl.exe (MSVC) | ❌ N'existe pas | Rien (natif Win32) |

Pour un portage MSVC natif, fork n'existe pas. Chaque `fork()` doit être remplacé par `CreateProcess()`. Le mode MSYS (avec fork émulé) est une **autre piste** (piste C du document de recherche), pas la piste MSVC.

---

## 4. La fonction centrale portée correctement

Voici le portage de la version `Interfaces/common.c` (la plus complexe, avec pipes bidirectionnels et sandbox) :

```c
// AVANT — Interfaces/common.c:168 (Linux)
int execredirected(char *cmdf, char *inf, char *outf, char *errf, char *args)
{
    pid_t pid;
    // ...
    pid = fork();
    if (!pid) { /* child */
        if (!inf)  { dup2(pipe_in[0], 0);  close(pipe_in[1]);  }
        else       freopen(inf, "r", stdin);
        if (!outf) { dup2(pipe_out[1], 1); close(pipe_out[0]); }
        else       freopen(outf, "w", stdout);
        if (errf)  freopen(errf, "w", stderr);
        // chroot conditionnel
        if (stat("../chroot/tmp/sessions/.chroot", &st) == 0 || must_chroot)
            { arg[0] = ch_root; i = 1; }
        else
            { if (setreuid(getuid(),getuid()) || setregid(getgid(),getgid())) _exit(127); }
        arg[i++] = cmdf;
        // ... parse args ...
        execv(arg[0], arg);
        exit(127);
    }
    else { /* parent */
        addpid(pid); mxpid = pid;
    }
    return 0;
}
```

```c
// APRÈS — win32compat/process.c (Windows)
#ifdef _WIN32
int execredirected_win32(char *cmdf, char *inf, char *outf, char *errf,
                         char *args, DWORD timeout_ms, DWORD cpu_limit_sec,
                         SIZE_T mem_limit)
{
    SECURITY_ATTRIBUTES sa = { sizeof(sa), NULL, TRUE };
    HANDLE hChildStdIn_Rd = NULL, hChildStdIn_Wr = NULL;
    HANDLE hChildStdOut_Rd = NULL, hChildStdOut_Wr = NULL;
    HANDLE hChildStdErr_Wr = NULL;
    STARTUPINFOA si = { sizeof(si) };
    PROCESS_INFORMATION pi = {0};

    // --- 1. Créer les pipes (ou ouvrir les fichiers) ---
    if (!inf) {
        CreatePipe(&hChildStdIn_Rd, &hChildStdIn_Wr, &sa, 0);
        SetHandleInformation(hChildStdIn_Wr, HANDLE_FLAG_INHERIT, 0);
    } else {
        hChildStdIn_Rd = CreateFileA(inf, GENERIC_READ, 0, &sa,
                                     OPEN_EXISTING, 0, NULL);
    }

    if (!outf) {
        // *** Overlapped pipe pour pouvoir select() dessus ***
        char pipeName[256];
        snprintf(pipeName, sizeof(pipeName),
                 "\\\\.\\pipe\\wims_out_%lu_%lu",
                 GetCurrentProcessId(), GetCurrentThreadId());
        hChildStdOut_Rd = CreateNamedPipeA(pipeName,
            PIPE_ACCESS_INBOUND | FILE_FLAG_OVERLAPPED,
            PIPE_TYPE_BYTE | PIPE_WAIT, 1, 4096, 4096, 0, NULL);
        hChildStdOut_Wr = CreateFileA(pipeName, GENERIC_WRITE, 0, &sa,
                                      OPEN_EXISTING, 0, NULL);
    } else {
        hChildStdOut_Wr = CreateFileA(outf, GENERIC_WRITE, 0, &sa,
                                      CREATE_ALWAYS, 0, NULL);
    }

    if (errf) {
        hChildStdErr_Wr = CreateFileA(errf, GENERIC_WRITE, 0, &sa,
                                      CREATE_ALWAYS, 0, NULL);
    } else {
        hChildStdErr_Wr = hChildStdOut_Wr; // stderr → stdout
    }

    // --- 2. STARTUPINFO ---
    si.dwFlags = STARTF_USESTDHANDLES;
    si.hStdInput  = hChildStdIn_Rd;
    si.hStdOutput = hChildStdOut_Wr;
    si.hStdError  = hChildStdErr_Wr;

    // --- 3. Construire la ligne de commande ---
    char cmdline[MAX_LINELEN + 1];
    snprintf(cmdline, sizeof(cmdline), "%s %s", cmdf, args ? args : "");

    // --- 4. Job Object (sandbox + limites CPU/mémoire) ---
    HANDLE hJob = CreateJobObject(NULL, NULL);
    if (cpu_limit_sec > 0 || mem_limit > 0) {
        JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli = {0};
        if (cpu_limit_sec > 0) {
            jeli.BasicLimitInformation.LimitFlags |= JOB_OBJECT_LIMIT_PROCESS_TIME;
            jeli.BasicLimitInformation.PerProcessUserTimeLimit.QuadPart =
                (LONGLONG)cpu_limit_sec * 10000000LL;
        }
        if (mem_limit > 0) {
            jeli.BasicLimitInformation.LimitFlags |= JOB_OBJECT_LIMIT_PROCESS_MEMORY;
            jeli.ProcessMemoryLimit = mem_limit;
        }
        // *** Kill-on-close : si le parent crash, l'enfant meurt aussi ***
        jeli.BasicLimitInformation.LimitFlags |= JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
        SetInformationJobObject(hJob, JobObjectExtendedLimitInformation,
                                &jeli, sizeof(jeli));
    }
    // *** Sandbox UI minimal ***
    JOBOBJECT_BASIC_UI_RESTRICTIONS uiRestrict = {0};
    uiRestrict.UIRestrictionsClass =
        JOB_OBJECT_UILIMIT_DESKTOP |
        JOB_OBJECT_UILIMIT_EXITWINDOWS |
        JOB_OBJECT_UILIMIT_GLOBALATOMS;
    SetInformationJobObject(hJob, JobObjectBasicUIRestrictions,
                            &uiRestrict, sizeof(uiRestrict));

    // --- 5. Lancer le processus ---
    if (!CreateProcessA(NULL, cmdline, NULL, NULL, TRUE,
                        CREATE_NO_WINDOW | CREATE_SUSPENDED,
                        NULL, NULL, &si, &pi)) {
        fprintf(stderr, "CreateProcess failed for: %s (error %lu)\n",
                cmdf, GetLastError());
        // cleanup...
        return -1;
    }
    AssignProcessToJobObject(hJob, pi.hProcess);
    ResumeThread(pi.hThread);

    // Fermer les bouts hérités par l'enfant
    CloseHandle(hChildStdIn_Rd);
    CloseHandle(hChildStdOut_Wr);
    if (errf && hChildStdErr_Wr) CloseHandle(hChildStdErr_Wr);

    // --- 6. I/O avec le CAS (overlapped sur le pipe de sortie) ---
    // Écriture input
    if (!inf && hChildStdIn_Wr) {
        DWORD written;
        WriteFile(hChildStdIn_Wr, inputbuf, inputptr - inputbuf, &written, NULL);
        CloseHandle(hChildStdIn_Wr);
    }

    // Lecture output (overlapped pour éviter le deadlock)
    if (!outf && hChildStdOut_Rd) {
        OVERLAPPED ov = {0};
        ov.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
        char buf[4096];
        DWORD bytesRead;
        char *outptr = obuf;

        while (1) {
            BOOL ok = ReadFile(hChildStdOut_Rd, buf, sizeof(buf), &bytesRead, &ov);
            if (!ok && GetLastError() == ERROR_IO_PENDING) {
                // Attendre avec timeout
                DWORD wait = WaitForSingleObject(ov.hEvent, timeout_ms);
                if (wait == WAIT_TIMEOUT) {
                    CancelIo(hChildStdOut_Rd);
                    TerminateProcess(pi.hProcess, 1);
                    break;
                }
                GetOverlappedResult(hChildStdOut_Rd, &ov, &bytesRead, FALSE);
            }
            if (bytesRead == 0) break;
            memcpy(outptr, buf, bytesRead);
            outptr += bytesRead;
            ResetEvent(ov.hEvent);
        }
        *outptr = 0;
        CloseHandle(ov.hEvent);
        CloseHandle(hChildStdOut_Rd);
    }

    // --- 7. Attendre la fin ---
    WaitForSingleObject(pi.hProcess, timeout_ms);
    DWORD exitCode;
    GetExitCodeProcess(pi.hProcess, &exitCode);

    // --- 8. Cleanup ---
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    CloseHandle(hJob);

    return (int)exitCode;
}
#endif // _WIN32
```

**Différences clés avec la V1 :**

- Named pipes overlapped au lieu de anonymous pipes (pour le select)
- Job Object avec `KILL_ON_JOB_CLOSE` (orphan protection)
- Sandbox UI restrictions (au lieu d'un simple noop)
- Overlapped `ReadFile` + `WaitForSingleObject` + `CancelIo` pour le timeout (au lieu d'un `read()` bloquant)
- Pas de `cmd.exe` — la commande est lancée directement

---

## 5. Le header de compatibilité (corrigé)

```c
// win32compat.h — CLAUDE_V1_1
#ifdef _WIN32

#include <windows.h>
#include <io.h>
#include <process.h>

// Types
typedef DWORD pid_t;

// Fonctions simples (macros)
#define strcasecmp  _stricmp
#define strncasecmp _strnicmp
#define random()    rand()
#define srandom(x)  srand(x)
#define unlink(x)   _unlink(x)
#define umask(x)    _umask(x)

// Chemins
#define PATH_SEP     '\\'
#define DEFAULT_TMP  win32_get_temp_dir()

// *** Shell : PAS cmd.exe — garder bash MSYS2 ***
#define DEFAULT_SHELL "C:/msys64/usr/bin/sh.exe"

// Fonctions remplacées (implémentées dans win32compat/*.c)
int    win32_pipe(int pipefd[2]);
pid_t  win32_fork_exec(const char *cmd, char *const argv[],
                        HANDLE hStdIn, HANDLE hStdOut, HANDLE hStdErr);
int    win32_kill(pid_t pid, int sig);
int    win32_waitpid(pid_t pid, int *status, int options);
void   win32_alarm(unsigned int seconds, void (*handler)(int));
int    win32_setrlimit(int resource, unsigned long limit);
char*  win32_get_temp_dir(void);

// mmap → CreateFileMapping (PAS shmget — WIMS utilise mmap)
void*  win32_mmap(void *addr, size_t length, int prot, int flags,
                  int fd, off_t offset);
int    win32_munmap(void *addr, size_t length);

// *** Sécurité : PAS des noops silencieux ***
#define chroot(x) \
    (fprintf(stderr, "SECURITY WARNING: chroot(%s) skipped on Windows. " \
                     "Process runs without filesystem sandbox.\n", (x)), 0)

#define setreuid(r,e) \
    (fprintf(stderr, "WARNING: setreuid() not available on Windows.\n"), 0)

#define setregid(r,e) \
    (fprintf(stderr, "WARNING: setregid() not available on Windows.\n"), 0)

#define setsid() (0)

#endif // _WIN32
```

---

## 6. Effort révisé

L'estimation V1 (~3 mois) était trop optimiste. Après vérification des sources :

| Composant | Effort V1 | Effort V1_1 (corrigé) | Raison |
|-----------|-----------|----------------------|--------|
| `execredirected()` (4 versions + 6 fork directs) | 2 semaines | **4-5 semaines** | 4 implémentations distinctes, pas 1. Pipes overlapped. |
| wimsd → Apache CGI | 2 semaines | **0** (supprimé du scope) | Apache le remplace. |
| wimslogd | Non compté | **2-3 semaines** | Oublié en V1. Daemon complet. |
| Texgif | Non compté | **1 semaine** | Oublié en V1. Sa propre execredirected + call_sh. |
| canvasdraw (OpenBabel + MathML) | Non compté | **1 semaine** | Oublié en V1. 2 fork directs. |
| signal/alarm | 3 jours | 3 jours | Inchangé. |
| kill wrapper | 1 jour | 1 jour | Inchangé. |
| Shared memory (mmap, pas shmget) | 1 semaine | 1 semaine | Type corrigé, effort similaire. |
| Socket Unix → TCP | 3 jours | 3 jours | Inchangé. |
| setrlimit / Job Objects | 2 jours | 2 jours | Inchangé. |
| Build system → CMake | 1 semaine | 1 semaine | Inchangé. |
| Sécurité (sandbox réel, pas noop) | Non compté | **1 semaine** | Noop supprimé. |
| sendmail → msmtp/SMTP | Non compté | **2 jours** | Oublié en V1. |
| libcrypt → bcrypt/OpenSSL | Non compté | **2 jours** | Oublié en V1. |
| call_sh() : bash MSYS2 + chemins | Non compté | **1 semaine** | Adapter ~50 appels. |
| 36 scripts shell → PowerShell | 3-4 semaines | 3-4 semaines | Inchangé. |
| Tests intégration | Non compté | **3-4 semaines** | Non compté en V1. |
| **TOTAL** | **~3 mois** | **~4,5 à 6,5 mois** | |

---

## 7. Recommandation révisée

### Étudier ces dépôts dans cet ordre :

1. **PostgreSQL** (`src/port/win32/` + `src/backend/postmaster/postmaster.c`) — le cas le plus proche de WIMS
2. **nginx** (`src/os/win32/`) — architecture la plus propre (39 fichiers)
3. **Win32-OpenSSH** (`contrib/win32/openssh/`) — couche POSIX la plus complète
4. **Redis Windows** (`src/Win32_Interop/`) — uniquement pour mmap → CreateFileMapping

### Le portage natif vaut-il le coup pour WIMS ?

**Non pour un usage local.** Le ratio est clair :

| Approche | Effort | Résultat | Usage cible |
|----------|--------|----------|-------------|
| MSYS2 + Laragon | 2-3 semaines | WIMS fonctionnel, fork lent | 1-5 utilisateurs locaux |
| Portage MSVC natif | 4,5-6,5 mois | WIMS natif, performant | 30+ élèves simultanés |

**Recommandation : prototyper d'abord sous MSYS2 (piste C) avec Apache Laragon en CGI.** Si la performance est insuffisante, le code source patché pour MSYS2 servira de base au portage MSVC (les `#ifdef` sont les mêmes).

---

## 8. Errata V1 → V1_1

| # | Erreur V1 | Correction V1_1 | Source |
|---|-----------|-----------------|--------|
| E1 | Shared memory = shmget/shmat | mmap() (wimslogd.c:141, wims.c:1224) | Sources r19040 |
| E2 | select() → WaitForMultipleObjects | Ne marche pas sur anonymous pipes. Overlapped I/O ou threads. | Win32 API doc |
| E3 | DEFAULT_SHELL = "cmd.exe" | call_sh() envoie du bash. Garder sh.exe MSYS2. | lines.c:1100, exec.c:1804, etc. |
| E4 | chroot/setreuid = noop silencieux | Danger : supprime l'isolation. Au minimum warning, idéalement Job Objects. | Audit sécurité |
| E5 | Une seule execredirected() portée | 4 versions + 6 fork directs = 10 sites à porter | Sources r19040 |
| E6 | wimsd porté | Inutile — Apache CGI le remplace | Architecture WIMS |
| E7 | wimslogd, Texgif, canvasdraw, libcrypt, sendmail oubliés | Ajoutés en §3.7 | Sources r19040 |
| E8 | MSYS2 MinGW confondu avec runtime Cygwin | Clarifié en §3.8 | Documentation MSYS2 |
| E9 | Effort ~3 mois | 4,5-6,5 mois (tests inclus) | Somme détaillée §6 |

---

## Références

- [UNIX Application Migration Guide (Microsoft)](https://learn.microsoft.com/en-us/previous-versions/ms811896(v=msdn.10))
- [PostgreSQL OSCON 2005 : Porting to Windows (PDF)](https://wiki.postgresql.org/images/4/46/OSCON2005-PortingToWindows.pdf)
- [PostgreSQL fork/exec patch thread](https://www.postgrespro.com/list/thread-id/1997277)
- [Win32-OpenSSH wiki](https://github.com/PowerShell/Win32-OpenSSH/wiki)
- [nginx win32 source](https://nginx.googlesource.com/nginx/+/refs/heads/master/src/os/win32/)
- [Redis Windows (archived)](https://github.com/microsoftarchive/redis)
- ["A fork() in the road" — Microsoft Research (2019)](https://www.microsoft.com/en-us/research/uploads/prod/2019/04/fork-hotos19.pdf)
