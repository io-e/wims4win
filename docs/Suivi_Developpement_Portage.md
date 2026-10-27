# Suivi du développement — Portage natif Windows de WIMS (r19040 / 4.33a)

Dépôt de travail : `/workspace/wims-port` (box Debian 13) — copie : `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-port`.
Architecture de référence : `02_Architecture_Portage.md` (documents consolidés : `README.md`).

---

## Phase 1 — terminée (23/10/2026, après-midi, heure de Paris)

### Résumé
- Import des sources r19040 (Reprise_Wims_Claude\wims, lecture seule) dans un dépôt git local, bits exécutables restaurés.
- Compilation de référence WIMS d'origine sous Debian (ajout dépendance `libgmp-dev` pour `units-filter`; `libwims.a` à copier dans `lib/`).
- Nouvelle couche `src/Platform` : `wims_platform.h` (API portable processus/pipes), `wp_posix.c` (fork/pipe/dup2/execvp/select/waitpid/kill, comportement historique), `wp_common.c` (`wp_strip_cr`), `wp_win32.c` (squelette CreateProcess + STARTUPINFOEX/HANDLE_LIST, PeekNamedPipe, TerminateProcess ; **isolation refusée** `WP_E_NOISOLATION`).
- `src/Interfaces/common.c` refactorisé sur cette API (execredirected, dopipes, kill/alive/wait), comportement Linux inchangé.

### Commits
- `5afd05e` Import WIMS trunk r19040 (4.33a) intact
- `debb65b` Platform: add wims_platform API (POSIX impl + Win32 skeleton)
- `24f38f5` Interfaces: route common.c process/pipe handling through wims_platform
- `0a5207a` Tests: platform unit tests (POSIX + Win32/wine) and pari interface equivalence suite
- `de74127` Restore executable bits lost in the Windows zip transfer

### Tests
- `tests/interfaces/run_equiv.sh` (référence r19040 vs refactorisé, interface pari, gp 2.17.2) : toutes les cas passent — 2+3, factor, gros résultat (fsizelim), commande illégale filtrée, about, faux CAS plain/CRLF/flood 20 Mo, timeout, CAS absent, **multiexec** (session persistante).
- `tests/platform/test_platform.c` : OK en natif Linux et en cross-compilé mingw-w64 sous wine (CRLF supprimés, isolation refusée).

### Limites
- Win32 : squelette seulement (écriture bloquante, pas de Job Object, isolation toujours refusée) ; common.c non cross-compilable.
- Pas de tests 200 modules ni de charge (contrainte box).

---

## Phase 2 — en cours

### 23/10/2026 17:45 (heure de Paris) — étape 1 : common.c + pari.c compilés pour Windows
Travail fait (commité à 17:50, voir étape 2) :
- `src/Platform/win32/config.h` (config.h écrit à la main pour Windows), `src/Platform/wp_compat.h/.c` : `setenv/unsetenv` (`_putenv_s`), `mkdir`, `lstat`, conflit `filelength`, stdio en binaire (sinon le CRT transforme `\n` en `\r\n`), `gettimeofday` (MSVC). `random/srandom` déjà fournis par libwims (Mersenne Twister).
- `includes.h` / `Interfaces/common.h` : en-têtes POSIX conditionnels (`sys/resource.h`, `sys/wait.h`).
- `common.c` : sous Win32, `addpid/rmpid` (exec.pid) remplacés par un **Job Object nommé** d'après `tmp_dir` ; limites CPU/mémoire via `wims_rlimit_cpu` / `wims_rlimit_mem`.
- `wp_win32.c` : stdin du CAS via named pipe **overlapped** (écriture jamais bloquante), CAS lancé suspendu puis assigné au Job (KILL_ON_JOB_CLOSE, limites), `wp_kill` = TerminateJobObject (tue l'arbre), `wp_job_kill(nom)`. Isolation : DROP_PRIV accepté seulement si le jeton n'est pas élevé (pas de setuid sous Windows), WRAPPER (ch..root) toujours refusé.
- `tools/build-win32.sh` (cross-build mingw-w64) → `pari.exe` compile sans avertissement ; `tools/check-linux-identical.sh` : objets Linux **identiques octet pour octet** (common.c, pari.c, Lib/*, calc.c, lines.c) ; seul wp_posix.o change (2 fonctions no-op ajoutées, jamais appelées par le code existant).
- PARI/GP Windows : site officiel pari.math.u-bordeaux.fr injoignable (box et PC) → gp.exe 2.19.0 du paquet MSYS2 ucrt64 (build Windows natif). `pari.exe` + `gp.exe` sous wine : `2+3` → `5` ✅.
- En cours : suite d'équivalence Linux ↔ Windows/wine (`tests/interfaces/run_equiv_wine.sh`, faux CAS en C `fake_cas.c`).

Bloqueurs : site PARI officiel indisponible (contourné par MSYS2).
Prochaines étapes : résultats de la suite wine, commits, multiexec côté wims (calc.c), puis Maxima / ch..root.exe si le temps le permet.

### 23/10/2026 17:51 (heure de Paris) — étapes 1 à 3 terminées et commitées

Commits :
- `641bf5e` Win32 build: hand-written config.h, libc compat, conditional POSIX headers, mingw-w64 cross-build script
- `68377ef` Platform Win32: overlapped stdin writes, CAS in Job Objects (named per session, CPU/mem limits, tree kill) replacing exec.pid; DROP_PRIV only when not elevated; common.c builds for Windows
- `3ae53c2` multiexec (calc.c:229-258) on the platform layer: wp_mx_exchange; documents bug calc.c:241, fix only with WIMS_MX_FIX_TIMEOUT
- `eeac3ee` Tests: Linux<->Windows(wine) equivalence suite, C fake CAS, Win32 job/overlapped/isolation tests, multiexec legacy-bug demo, Linux byte-identity checker

Résultats des tests :
- **Linux octet pour octet** (`tools/check-linux-identical.sh de74127`) : common.c, pari.c, Lib/*, calc.c, lines.c identiques ; seul wp_posix.o diffère (fonctions ajoutées non appelées par le chemin Linux).
- **Équivalence Linux ↔ Windows/wine** (`run_equiv_wine.sh`, pari.exe + gp.exe 2.19.0 vs pari Linux + gp 2.17.2) : **12/12** — 2+3, factor, 2^200, gros résultat (fsizelim), commande illégale filtrée (secure mode), about, faux CAS plain / CRLF (Windows sans \r = Linux) / flood 20 Mo, timeout CAS muet, CAS absent, **multiexec** (session persistante a=5 → a+1=6…).
- **Unitaires plateforme** : Linux 18/18 (dont wp_mx_exchange) ; Win32 sous wine 19/19 — sandbox refusé, DROP_PRIV refusé si jeton élevé, écriture overlapped non bloquante avec contre-pression, kill avec écriture en vol, **Job tue aussi le petit-enfant**, wp_job_kill(session), CPU-limit accepté.
- **Bogue calc.c:241** démontré (`test_mx_legacy.sh`) : `v1=0` réinitialisé à chaque tour → timeout jamais atteint (attente infinie si l'interface se tait) ; en plus read()==0 (EOF) → boucle active ; et le commentaire « 3 seconds » vaut en réalité 300×100 ms = 30 s. Version corrigée (`-DWIMS_MX_FIX_TIMEOUT`) : retour après 3 s. Chemin Linux par défaut **non modifié** (ancien code conservé sous `#if`).

Ce qui marche sous Windows (wine) : interface pari.exe complète (simple, about, multiexec, timeouts, fsizelim, CRLF), Job Objects (arbre, nommage par session).

Limites / bloqueurs :
- Limite CPU du Job (JOB_OBJECT_LIMIT_PROCESS_TIME) **non appliquée par wine** : à vérifier sur un vrai Windows.
- stderr du CAS hérité (non pipé) : gp.exe y écrit des CRLF → à supprimer côté wims à la lecture de exec.err (phase 3).
- Côté wims, `lines.c multiexec()` (lancement des sessions) est encore fork() : le bloc calc.c est prêt pour la couche Platform mais wims.cgi n'est pas encore compilable sous Windows (phase 3).
- PARI officiel injoignable : gp.exe MSYS2 utilisé.

Prochaines étapes : Maxima (installation en cours sur la box) et tests d'équivalence ; puis prototype ch..root.exe si le temps le permet.

### 23/10/2026 17:53 (heure de Paris) — étape 4 et clôture de la phase 2
- Maxima 5.x installé sur la box (apt). `tests/interfaces/run_equiv_maxima.sh` (r19040 vs refactorisé) : **7/7** — 2+3, factor, integrate, commande illégale neutralisée, about, timeout, multiexec (a:5 → a+1=6, expand).
- Commit `db52741` Tests: maxima interface equivalence (r19040 vs refactored), incl. multiexec.
- **Prototype ch..root.exe (AppContainer / jeton restreint) : reporté en phase 3.** L'isolation WRAPPER reste refusée sous Windows (`WP_E_NOISOLATION`).
- Copie `Reprise_Wims_Grok\wims-port` mise à jour par `git fetch` d'un bundle + `merge --ff-only` (non destructif) : HEAD = `db52741`.

Prochaines étapes (phase 3) : ch..root.exe (AppContainer + ACL NTFS), port de lines.c multiexec() (fork → wp_spawn, mxtab en wp_chan), suppression des \r de exec.err côté wims, vérification des limites CPU du Job sur un vrai Windows 10/11, autres interfaces (maxima.exe Windows, octave, gap…), puis wims.cgi.

---

## Phase 3 — en cours / livrée partiellement

### 23/10/2026 18:27 (heure de Paris) — G2, G1, G3, G5, G6, G4

#### G2 — multiexec() de lines.c sur Platform ✅
- `wp_spawn_mx`, `wp_pipe_open`, `wp_fd_read` ; `mxtab` devient `wp_chan`+`wp_pid`+`err_*` sous `_WIN32` / `WIMS_MX_PLATFORM`.
- `execredirected` (one-shot) aussi porté sur la couche Platform sous ces mêmes options.
- Chemin Linux par défaut **identique octet pour octet** (calc.c, lines.c, common.c, auth.c, wims.c…).
- Commits : `b9670a6`, `1cd0534`.

#### G1 — ch..root.exe ✅ (sous réserve tests d'évasion manuels)
- `src/Platform/sandbox/chroot_win.c` → `ch..root.exe` : RestrictedToken (DISABLE_MAX_PRIVILEGE|LUA_TOKEN) + AppContainer sans capacités + ACE GENERIC_ALL sur TMPDIR.
- `--selftest` : **OK sur le PC Windows réel** ; sous wine → E_NOTIMPL (refus correct → `WP_E_NOISOLATION`).
- WRAPPER accepté seulement si `ch..root.exe --selftest` réussit (`wp_sandbox_available`).
- Esquisse d'évasion : `tests/platform/test_sandbox_evasion.ps1` (écriture hors session, ping).
- Commit : `c13459b`.

#### G3 — retrait des \\r dans exec.err ✅
- `_get_exec_error` (calc.c) et `readabout` (common.c) : `wp_strip_cr` sous `_WIN32` uniquement.
- Commit : `f81abf1`.

#### G4 — Job Objects sur PC Windows réel ⚠️
Dossier neuf `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\tests-win` (rien d'autre touché).
| Test | Résultat |
|------|----------|
| test_platform.exe (22/22) | ✅ overlapped, spawn_mx, **kill d'arborescence (petit-enfant)**, job nommé |
| Limite **mémoire** 32 Mo | ✅ memhog tué |
| Limite **CPU** 1 s (PROCESS_TIME et JOB_TIME) | ❌ processus busy encore vivant après 30 s (SetInformation/Assign OK, flags 0x2002) |
| ch..root.exe --selftest | ✅ AppContainer OK |

Bloqueur G4 : la limite CPU des Job Objects ne s'applique pas sur cette machine (Windows 10/11 + binaire mingw). La limite mémoire et le kill d'arbre fonctionnent. Contre-mesure déjà en place : timeout de l'interface (`dopipes` / `wp_kill` → TerminateJobObject). À approfondir (comptabilité GetProcessTimes, intervalle d'échantillonnage, éventuel bug mingw/headers).

#### G5 — autres interfaces CAS ✅ (compilation)
- `pari maxima gap octave m2 scilab yacas` → `.exe` via `tools/build-win32.sh`.
- Octave : `dos`, `winqueryreg`, `winopen`, `perl`, `python`, `python3` ajoutés à `illegal[]` **sous `_WIN32` seulement** (Linux inchangé).
- gap : `gap.exe` par défaut sous Windows ; m2 : `"nul"` à la place de `/dev/null`.
- Pas de binaires CAS Windows testés (hors pari/gp déjà validé en phase 2).
- Commit : `e13ec24`.

#### G6 — wims.cgi / wims.exe compilable mingw-w64 ✅ (lien + smoke)
- 37 objets compilés + lien statique → `build/wims-win/wims.exe` (~814 Ko).
- Shims : alarm (TimerQueue), kill/getppid/getrusage/pipe/nanosleep/symlink, setrlimit no-op, crypt **fail-closed**, mathml → fallback GIF (fork non porté).
- Smoke wine : démarre, panique attendue « Unable to bring up wimslogd » (daemon hors phase 3) ; un `fork()` résiduel encore frappé dans ce chemin.
- Commit : `4374c68`. Script : `tools/build-wims-win32.sh`.

### Hors périmètre (rappel)
H1–H3 (VPS) non traités. Pas de tests 200 modules ni charge.

### Prochaines étapes
1. Diagnostiquer la non-application de JOB_OBJECT_LIMIT_*_TIME sur le PC.
2. Jouer `test_sandbox_evasion.ps1` sur le PC ; durcir ch..root si besoin.
3. Porter le démarrage de wimslogd (plus de fork caché) ; remplacer crypt stub par OpenSSL/BCrypt.
4. MathML helper sans fork ; autres démons ; installer les CAS Windows et rejouer l'équivalence.

---

## Phase 4 — 23/10/2026 18:53 (heure de Paris)

Décision **H9** : dépôt unique `wims-port` (confirmé).

### N2 — wimslogd sans fork ✅
- IPC : `wp_ipc` — POSIX AF_UNIX inchangé ; Win32 **named pipe** `\\.\pipe\wims-*`.
- Daemon minimal `Wimslogd/win/wimslogd_win.c` : ping + loadavg via `GetSystemTimes`, pas de fork.
- `bringuplogd` → `wp_daemon_spawn` (CreateProcess détaché).
- **wine** : CGI `module=adm/doc&lang=en` → **Status: 200** (14 Ko HTML).
- **PC** : idem → **PASS CGI Status 200** (après création de `sessions/`).
- mmap/shm : toujours commenté dans les sources d'origine ; CreateFileMapping reporté (non requis pour le ping).
- Commit : `44bde53`.

### N1 — crypt ✅
- DES crypt (musl, compat hashes existants) : vecteurs `password/ab` → `abJnggxhB/yWI`, etc.
- Helper BCrypt PBKDF2-SHA256 pour nouveau format `$wims$v1$`.
- Tests 8/8 sous wine **et sur le PC**.
- Commit : `920cb53`.

### N6 — CAS Windows ✅ (23/10/2026 ~20:00 heure de Paris)
- **GAP 4.16.1** : exécutable direct `C:\Users\Shadow\AppData\Local\GAP-4.16.1\runtime\opt\gap-4.16.1\gap.exe` (pas de `.bat`) ; junction `tests-win\cas\gap` ; `PATH` += `runtime\bin` (Cygwin). `gap_command=<junction>\...\gap.exe -T -n`.
- **Maxima / Octave / PARI** : OK ; maxima via `maxima.bat`+SBCL (`MAXIMA_PREFIX`).
- Suite PC `run_equiv_pc.ps1` : **24/24** (gap+maxima+octave × calc, factor, large, illegal, about, timeout, absent, multiexec). Écarts about : versions Linux 4.14/5.47/9.4 vs Win 4.16.1/5.49.0/11.3.
- Commit : `51b54d5`. Guide : `tools/install-cas-msys2.md`.


### N3 — MathML ✅ (23/10/2026 ~19:05 heure de Paris)
- `mathml.c` Win32 : `wp_spawn` + lecture stdout (plus de fallback GIF forcé). Chemin Linux (`fork`/`pipe`/`execv`) **identique** à r19040.
- Helper `wims_mathml.exe` (flex/bison + mingw-g++) : `tools/build-wims-mathml-win32.sh`.
- Smoke PC `tests-win/test_mathml_spawn.exe` : **PASS** (`x^2+y^2` → MathML). Wine idem.
- Commit : `4b2d4eb`.

### N5 — limite CPU ✅ corrigé (23/10/2026 ~19:00 heure de Paris)
- Diagnostic antérieur : constantes mingw = SDK ; sur Win11 le busy loop n'accumulait pas de user-time → la seule limite Job ne tirait pas.
- **Correctif** (commit `5f5523b`) :
  - `CreateProcess(CREATE_SUSPENDED)` → Job avec `JOB_OBJECT_LIMIT_PROCESS_TIME` **et** `JOB_OBJECT_LIMIT_JOB_TIME` → `AssignProcessToJobObject` → `ResumeThread` (systématique dans `wp_spawn` / `wp_spawn_mx`).
  - Budget par défaut = `rlimit_cpu` WIMS (**20 s**) via `wp_set_job` / env `wims_rlimit_cpu` (`config.c` Win32 + `Interfaces/common.c`).
  - **Filet** : thread watchdog lit `JOBOBJECT_BASIC_ACCOUNTING_INFORMATION` (user+kernel) et `TerminateJobObject` si dépassement, ou après 2× le budget en temps mural (quand l'accounting reste à 0).
- **PC** `tests-win` : `test_platform.exe` → **22/22**, busy child tué sous 6 s (« ended by job/watchdog within 6 s: yes »).
- Linux : `check-linux-identical.sh` OK (chemins `#ifdef _WIN32` seulement).

### N4 — réseau AppContainer ✅
- `ch..root.exe --cmd ping …` → rc=1 (bloqué) sur le PC.

### Linux
- `lines.c` / `log.c` : identiques octet pour octet (chemin par défaut).

### Bloqueurs / suite
1. ~~N6 CAS~~ gap/maxima/octave/pari OK sur PC (`51b54d5`, equiv 24/24).
2. ~~N3 MathML~~ résolu (`4b2d4eb`).
3. wimslogd complet (scores, housekeeping) au-delà du min-ping.
4. ~~N5 CPU~~ résolu par watchdog + limites au spawn (`5f5523b`).

---

## GitHub — projet **wims4win** (23/10/2026 ~20:30 heure de Paris)

- Dépôt privé : https://github.com/io-e/wims4win (renommé depuis `io-e/wims-port`).
- Commits scaffolding : `16c0232` LICENSE/gitignore, `39af5bc` docs/, `021d9e5` README, `9324e26` CI, `197acab` rename wims4win.
- Issues `port-windows` #1–#9 (travaux restants).
- Remote `origin` = `https://github.com/io-e/wims4win.git` (box + copie PC).

## 2026-10-23 — Maxima : sbcl.exe direct (issue #2)
- `maxima_command` = `C:\maxima-5.49.0\bin\sbcl.exe --core …\binary-sbcl\maxima.core --noinform --dynamic-space-size 2000 --end-runtime-options --eval '(cl-user::run)' --end-toplevel-options` — plus de maxima.bat ni de cmd.exe.
- `src/Interfaces/maxima.c` (`#ifdef _WIN32` uniquement) : dérive `SBCL_HOME=<prefix>/bin` et `MAXIMA_PREFIX=<prefix>` du chemin de sbcl.exe s'ils ne sont pas définis ; l'enfant hérite de l'environnement Windows complet. Build Linux identique (check-linux-identical : IDENTICAL).
- tests-win (env MAXIMA_PREFIX/SBCL_HOME vidés) : 24/24 PASS, dont maxima calc/factor/large/illegal/about/timeout/absent/multiexec.

## 2026-10-23 — Arbre runtime complet + wims.exe en CGI (PC)
- Arbre runtime : `wims-4.32.tgz` officiel (SourceSup, SHA-256 d26b643c…; Renater refuse le TLS depuis la box, téléchargé sur le PC puis copié) → `./compile --mathjax --geogebra` (Shtooka ignoré : downloadcenter.wimsedu.info injoignable) → `bin/mkindex`. Procédure : `tools/build-runtime-tree.sh`. Paquet Windows sans aucun ELF, liens symboliques déréférencés (≈260 : html/themes, scripts/themes, alias anstype…), exe Windows placés : `public_html/wims.exe` + `wims.cgi.exe`, `bin/wimslogd.exe`, `bin/wims_mathml.exe`, `public_html/bin/ch..root.exe` + interfaces (gap, maxima, octave, pari, m2, scilab, yacas).
- Copié sur le PC dans le dossier neuf `Reprise_Wims_Grok\wims-runtime` (config Laragon non modifiée).
- Cause exacte du 0xC0000005 (wine + winedbg + addr2line) : `log.c:521 checklogd()` → `memcmp(getenv("SERVER_ADDR"),"10.",3)` avec SERVER_ADDR absent. Corrigé (garde `_WIN32` + SERVER_ADDR=127.0.0.1 par défaut).
- `wp_cgi_prepare()` (Platform/wp_cgi_win32.c) : chdir vers le dossier de l'exe, `\`→`/` dans SCRIPT_FILENAME/DOCUMENT_ROOT/PATH_TRANSLATED…, contrôle html/ msg/ scripts/ bases/ modules/ themes/ bin/wimslogd.exe log/ → `Status: 500 WIMS runtime tree incomplete` + liste des manquants.
- Deux bugs trouvés pendant le smoke test : (1) liens symboliques perdus → page d'accueil tronquée (themes/standard/visitor.phtml introuvable) ; corrigé par `tar -h`. (2) tube nommé global `\\.\pipe\wims-wimslogd` : wims-runtime parlait au wimslogd de tests-win ; nom = hash FNV-1a du chemin absolu. `wp_daemon_spawn` n'hérite plus du stdout CGI (Apache aurait attendu EOF tant que wimslogd vit).
- Smoke PC (Process .NET, stdout lu jusqu'à EOF, comme Apache) : accueil fr 200 OK 26 803 o `</html>` présent (634 ms, wimslogd démarré à la volée) ; `lang=en` 200 OK ; arbre vide → 500 explicite. Linux : objets identiques (wims.c, log.c, lines.c).
- Config Apache/Laragon proposée (non appliquée) : `docs/apache-laragon-wims-runtime.conf`.
- Limites : arbre 4.32 + binaire 4.33a ; modules hors distribution (ex. tool/algebra/factor.fr → 450 « module absent ») à installer via modupdate/wimsget ; Shtooka absent ; mot de passe webmaster vide (log/.wimspass) à définir.

## 2026-10-25 — Reprise OEF_VALIDATION : évaluation wims4win pour oef2html
- oef2html.py (copie + patch `--cgi`) tourne nativement sur le PC avec wims.cgi.exe (runtime `wims-runtime-oef`) : 4/5 pages identiques à Linux 4.31b, la 5e diffère par une image insdraw.
- Formule absente = `default_texposition=2` manquant (voie GIF/flydraw inopérante sous Windows) ; correction absente = bug CRLF (mode texte CRT) corrigé : binaire global.
- Écart 4.31b→4.33a moteur ≈110 lignes ; rebasage : 1 hunk en conflit / ~100 ; 1–2 j.
- too_many_gotos : boucles de rejet sans re-tirage (procédures récursives non traduites par xml2oef), grands entiers en notation e+NN.
- Détails : docs/03_Cahier_Audit_et_Recette.md, partie C (original : docs/archive/Evaluation_OEF_wims4win.md).

## 2026-10-25 — Décisions d'Arnaud : PARI, flydraw, taille des formules
- On garde 4.33a (pas de rebasage). Le commit 8bf7078 de Claude ne contient qu'une doc Laragon, qui sert notre `wims-runtime`.
- PARI/GP : `pari_command` est configuré dans `wims-runtime` et `wims-runtime-oef`. 00403 est identique à Linux ; 00509 donne too_many_gotos, comme sous Linux.
- flydraw.exe : libgd statique en mingw. `insdraw..processor.exe` est natif. Les helpers sont résolus en `.exe`, `mv`/`rm` tournent en interne, et `wims_home` et PATH sont corrigés. 00001 est identique à Linux, image GIF comprise.
- `log/wims4win.yaml` règle `formula.size`, 200 % par défaut, avec le même comportement sous Linux et Windows. L'écart 110/120 % venait d'un bug argv amont, expliqué dans 03_Cahier_Audit_et_Recette.md, partie C §5.
- `build-runtime-tree.sh` ajoute `default_texposition=2`, le yaml, flydraw et le processeur insdraw.
- Échantillon PC de 10 fichiers : les 6 pages produites sont identiques à Linux et les 4 échecs sont les mêmes que sous Linux.
- Les 58 too_many_gotos ne sont pas traités (décision d'Arnaud).
- Reste à faire :
  - animations ou conversion d'images dans insdraw (pas d'ImageMagick) ;
  - texte freetype dans flydraw ;
  - autres CAS ;
  - Laragon/Apache arrêté sur le PC au moment du test (CGI validé en direct : 200).

## 2026-10-25 — Outils OEF dans le dépôt et handoff pour Claude
- Ajout de `tools/oef-validation/`, chaîne de test issue d'OEF_VALIDATION :
  - oef2wims 4.31b (Linux + mingw) ;
  - log, trace, v2 ;
  - correcteur LLM ;
  - oef2html avec `--cgi` appliqué ;
  - `corpus.map`, sha256 et script de reconstruction.
- Chemins paramétrés. Chemins, modèles et ports personnels neutralisés.
- Non inclus : `.exe`, zip, tarball, wims-runtime, dossiers de travail, `__pycache__`, `oef_validate.py` et corpus Eleda.
- Vérifié depuis le dépôt : parités Linux/Windows, v2 et trace à 1438/1438 ; build Linux identique à celui d'origine ; oef2html identique à Linux (Linux 3/3, wine 2/2).
- Le corpus se reconstruit depuis le tarball : 338/338 fichiers (333 upstream + 5 cas d'erreur), et 1438/1438 si l'on fournit le dossier Eleda.
- CI : nouveau job `oef-validation` (`ci.sh`, 37 OEF de référence).
- Handoff pour Claude : `docs/07_Handoffs.md`, partie A (original : `docs/archive/HANDOFF_Claude_OEF_Python.md`), avec une copie dans `Reprise_Wims_Grok`.
- Laragon (vhost `wims_paris_saclay_fr.test`, qui sert `wims-runtime`) : `wims.cgi` répond 200.

## 2026-10-27 — Documents de Claude intégrés puis fusionnés
- Commit de Claude `769bab0` (11 documents dans `docs/claude/`) poussé sur `master`.
- Fusion en documents uniques dans `docs/` : `01_Audit_Migration.md`, `02_Architecture_Portage.md`, `03_Cahier_Audit_et_Recette.md`, `04_Guide_Installation_et_CGI_Laragon.md`, `05_Etat_et_Decisions.yaml`, `06_Recherche_Projets.md`, `07_Handoffs.md` ; index `docs/README.md`.
- Originaux Claude et Grok déplacés par `git mv` dans `docs/archive/` (historique conservé).
- Chaque document indique ses sources et une section « Divergences résolues » (le code et les mesures font foi) ; les questions non tranchées sont dans `05_Etat_et_Decisions.yaml` (`open_questions`).
- Claude écrit désormais directement dans les documents consolidés.
- Laragon démarré par Arnaud : `wims.cgi` → 200 ; `tool/algebra/factor.fr` → 450 (module non installé, issue #3).
- Aucune modification de code.

## 2026-10-27 — Décisions Q1–Q6 d'Arnaud et runtime r19040
- Décisions D15–D20 inscrites dans `05_Etat_et_Decisions.yaml` ; 01/02/04 mis à jour (MSVC/CMake en historique seulement). Run CI 40493026828 annulé.
- `tools/build-runtime-tree.sh` : construit depuis un `svn export -r 19040` ; restaure les ~225 liens svn:special qu'un export Windows transforme en fichiers « link … » (sinon accueil tronqué), les bits d'exécution, supprime `.wims_maintainer_mode`, `make` séquentiel ; provenance dans `log/wims4win-provenance.txt` ; `PARI_COMMAND`.
- `tools/fetch-msys2-sh.sh` + `tools/msys2/msys2-pins.txt` : 43 paquets MSYS2 épinglés (sha256), licences dans `other/MSYS2-LICENSES.txt`.
- Mesures sur le runtime Grok (box, déployé puis retiré du PC) : accueil fr 200 (26 700 car., `</html>`), en 200, adm/doc 200, adm/class/classes 200 ; parité oef2html sur l'échantillon de 10 : 6/6 pages identiques au run PC de référence, 4 échecs identiques (00347, 00352, 00509 too_many_gotos ; 01012 bad_index).
- Échecs constatés :
  - `tool/algebra/factor.fr` → 450 : ce module n'est pas dans trunk r19040 ; il faut `modupdate`, qui appelle `public_html/bin/webget` (helper C Linux, pas encore porté).
  - `tool/wcalc.fr` → 550 : helper `translator` (C, Misc) non porté sous Windows.
  - `bin/mkindex` via sh.exe : la racine MSYS2 tombe sur le runtime, donc `pwd` = `/` et `wims_home` = `//` ; `perl` absent du lot MSYS2. Le montage `bind` de `/bin` corrige les shebangs mais pas `wims_home`. À revoir : placer MSYS2 sous `other/msys2/` (racine distincte) et ajouter perl.
- Runtime de Claude (`wims-port\wims-runtime-r19040`, `install-runtime-r19040.ps1`, 8fbccc0) contrôlé : accueil 200 mais tronqué (2 727 car., pas de `</html>`), factor.fr 450, wcalc 550 ; pas de `log/wims.conf` (donc ni `default_texposition=2` ni `pari_command`), ni `wims4win.yaml`, ni sh.exe/coreutils, ni flydraw/insdraw, ni index mkindex.

