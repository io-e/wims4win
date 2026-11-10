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


## 2026-10-27 (après-midi) — Runtime Grok r19040 revalidé sur le PC
- Runtime : `Reprise_Wims_Grok\wims-runtime-r19040-grok` (Laragon non modifié).
- MSYS2 sous `other/msys2/` (racine propre) + perl épinglé, `usr/share/perl5` ajouté (ed2c3f7).
- Helpers Misc mingw (5c7d55e) : webget (WinHTTP), translator, dicsort, modind, oef2wims, msg2wims, etc. (18).
- `factor.fr` installé par webget.exe + tar via sh.exe (vérification `modupdate` en mode check : liste reçue de unice/wimsedu) → 200. `wcalc.fr` → 200.
- `createqcm` : `bad_cmd rmdepot.sh` reproduit puis corrigé (1b58be7) ; Linux identique.
- `mkindex` via sh.exe : 1 338 s, rc=1 (cpp, strfile absents ; `find: environment too large` en fin).
- Retours Claude : GAP 8/8 en relançant (échec de 14 h 56 pendant mkindex, wrapper inchangé depuis le 23/10, donc pas de régression de code) ; limite CPU : le chemin WIMS (job + watchdog) passe < 6 s, le test .NET du harnais attache le job après démarrage et Windows n'applique pas PROCESS_TIME (30 s CPU sans arrêt) ; S2 : harnais sans cmd.exe (27071ff), 3/3.
- oef2html : 6/6 identiques, 4 échecs identiques.

## 2026-10-27 (soir) — latex2wims, CI, sandbox #8, wimslogd #1, helpers Misc
- latex2wims (`feat/latex2wims-doc`) : 53b65e1 poussé ; 0449ac3 : `check` compte un symbole isolé (`\alpha`, image `mathfonts/`) comme formule rendue. Linux 11/11 (runtime de test avec `threshold1/2` et `default_texposition=2`). PC (`latex2wims_GROK`) : exemple OK, parité corpus 5/5 identique à Linux, 4 documents via `wims.cgi.exe` OK, codes 2/3/4 OK.
- CI : les 3 runs signalés étaient déjà en success ; `timeout-minutes` ajoutés (031d2a4) ; runs suivants en success.
- wimslogd (#1, 2258c89) : démon amont complet compilé mingw ; tube nommé ; tâches `fork()` relancées en `wimslogd.exe --task` dans un Job Object ; pile 32 Mo (tampons MAX_FILELEN de 2 Mo sur la pile). PC : getscore, record, readfile, cleaning, dispatch_log OK. Linux identique 13/13.
- Sandbox (#8, 0c0fce7) : `ch..root.exe` à la syntaxe amont, AppContainer par exécution nettoyé, Job 15 processus, `--selftest` sans cmd.exe, `--grant-rx`, `--purge-profiles`. PC : 17/17 (S1–S5, S9) ; factor.fr via pari sandboxé OK.
- Misc (af76b4c) : les 17 programmes restants + `wrap..exec` natif ; testés via `bin/` sur le PC.
- Risque noté : `wims.exe` a la pile Windows par défaut (1 Mo) alors que le code a des tampons de 2 Mo sur la pile (vu dans wimslogd) ; à vérifier.

## 2026-10-28 — Charset, thème CSS, runtime #3 (mkindex/oefcompile/modupdate), PLT-02
- **Charset (`Â°`)** : la cause n'est pas `wims.c`. WIMS 4.33a r19040 envoie `charset=windows-1252` pour fr, sous Linux comme sous Windows (vérifié sur le runtime Linux de la box), et les modules amont sont en ISO-8859-1. Les sources de `local/oef2html_web.fr` (et `INDEX` de `local/exemple.fr`) étaient en UTF-8. Changer `charname[]` aurait cassé tous les modules amont, donc aucun changement moteur. Correction : `tools/modules/convert-module-cp1252.ps1` (d36f377), qui convertit en windows-1252 les fichiers UTF-8 seulement, avec des entités `&#N;` hors cp1252. Appliqué sur le PC, sauvegarde dans `Reprise_Wims_Grok\backup-modules-utf8-20261028`. Résultats : 42/42 exercices en 200, 0 séquence `Â°` ; via Laragon, 11qcm contient 5 « ° » en octet 0xB0. Laragon pointe déjà sur le runtime Grok, sans `AddDefaultCharset` (lecture seule). À noter : les sources `D:\ALF_ZGit\03-oef2html_Web\src\oef\fr` sont en UTF-8 et devront être converties à chaque recompilation.
- **Thème** : un export SVN n'a ni `themes/*/css.css`, ni `gifs/themes/*/icon-min.css`, ni icônes wimsedu1/2. Ce sont des produits de release (mkcss.pl, gifs/themes/mkindex, avec java/yuicompressor et ImageMagick). `tools/gen-theme-assets.sh` (2475db1) lance les scripts amont inchangés avec des shims java/magick/identify (Pillow) et est appelé par `build-runtime-tree.sh`. Déployé sur le PC : css.css et icon-min.css en 200 via Laragon (icon-min.css était en 404). L'ancien css.css fait à la main est sauvegardé. La jonction `gifs/themes/standard` n'existe pas sous Linux et n'est pas ajoutée au build (laissée en place sur le PC, sans effet).
- **mkindex** (ae4be3d) : « environment too large » venait de `export mlist slist glist` (limite Windows de 32 767 caractères d'environnement). `tools/patches/mkindex-wims4win.patch` passe les listes à modind via des fichiers (`*_file`), et `modind.c` les lit sous `_WIN32` (Linux identique). `cpp` vient de gcc 16.2.0-1 MSYS2 (cpp.exe + cc1.exe ; isl et mpc épinglés). `strfile` est sauté par le script quand il est absent, comme en amont (fortune n'a pas de binaire Windows). Résultat PC : rc=0 en ~27 min, préprocesseur qcm OK. pdflatex absent : PDF de bases/doc/1028 non régénérés (P2). Le stage `build/win32` était périmé : il faut relancer `build-win32.sh` avant `build-misc-win32.sh`.
- **oefcompile** : `bin/oefcompile test U1/physics/oefelecmag.fr` donne 13/13, 0 échec.
- **modupdate** de bout en bout sur un module : `installone()` de `bin/modupdate` (webget WinHTTP, tar, mv), avec le harnais `Reprise_Wims_Grok\tools\mu_one.sh`, sur U1/physics/oefelecmag.fr : « Successful », page en 200.
- **PLT-02** (d4d7892) : `cmd /c echo` remplacé par `fake_cas echo/errline` ; 22/22 sur ShadowPC et sous Linux.
- Vérification Linux : check-linux-identical 14/14, plus Misc/modind.c IDENTICAL.

## 2026-10-28 (après-midi)

- **YAML-02** : `mathml.c` borne désormais la taille à 50..400 % au lieu d'ignorer la valeur. PC : 900 % → 400 %, 30 % → 50 %, abc → 200 %. Le test de Claude lisait adm/doc, qui ne contient aucune formule.
- **Pile de wims.exe** : lien avec `--stack,8388608` (8 Mo, comme Linux) ; `-fstack-usage` montre des trames jusqu'à ~287 Ko, alors que mingw réservait 2 Mo.
- **ch..root** : `ensure_profile_env()` reconstruit USERPROFILE, LOCALAPPDATA, APPDATA, TEMP et TMP. Sous Apache (environnement minimal), CreateProcessAsUser échouait avec l'erreur 203 et tous les CAS étaient refusés. `--cmd & sh` est maintenant refusé explicitement (127).
- **Suite sandbox** `tests/platform/test-sandbox-grok.ps1` : 26/26 sur ShadowPC. Les 8 échecs de la suite Claude venaient de `param([string[]]$Args)` (variable automatique PowerShell) et du nom localisé de S-1-15-2-1. Réponse détaillée dans `tests-win_ALF\11_Handoff_Grok_to_Claude_GROK_V1.md`.
- **P0** : SBX-S5b, SBX-20, CGI-06 et MSYS-05 PASS. IFC-06 FAIL pour GAP (Cygwin ne peut pas créer ses objets nommés dans un AppContainer) ; décision à prendre. `--grant-rx` appliqué sur `tests-win\cas\gap\runtime`.
- **Image 404 `wims.gif?cmd=getins`** : le CGI sert bien l'image ; seul le vhost ne mappe que `/wims/wims.cgi`. Les lignes ScriptAliasMatch sont dans `docs/apache-laragon-wims-runtime.conf`, à appliquer par Arnaud.

## 2026-10-28 (fin d'après-midi) — D21, GAP hors AppContainer

- Décision D21 (option B d'Arnaud) : `ch..root.exe` lit `public_html/bin/ch..root.conf`. Les programmes listés en `lowil` (livré : `gap.exe` seulement) tournent avec le jeton restreint au niveau d'intégrité bas, dans un Job (15 processus, 2048 Mo, kill-on-close), cwd/TEMP = session. Tous les autres restent en AppContainer. `--policy <prog>` affiche la politique.
- Pour étiqueter la session en intégrité basse, il faut WRITE_OWNER, que l'ACL « Modification » ne donne pas : le propriétaire se l'ajoute le temps de l'exécution, puis l'étiquette et l'ACE sont retirées.
- `lowil_path` ajoute le dossier de cygwin1.dll au PATH : Apache ne le fournit pas, et la configuration Laragon n'est pas modifiée.
- Runtime PC : `log/wims.conf` reçoit `gap_command=…\runtime\opt\gap-4.16.1\gap.exe -T -n`, et `ch..root.conf` reçoit `lowil_path`. Sauvegardes dans `backup-exe-20261028c`.
- Résultats : SBX-21 20/20, suite sandbox 26/26, IFC-06 GAP via Laragon (Factorial(10)=3628800, |S5|=120), 0 cmd.exe. check-linux-identical 14/14.
- Compromis : GAP n'est isolé ni en lecture ni en réseau (même modèle que le chroot Linux).

## 2026-11-04 — Octave et Maxima dans le runtime PC

- Installations portables sous `Reprise_Wims_Grok\tests-win\cas`, sans installateur système :
  - Maxima 5.49.0 : extrait de l'installateur NSIS avec 7-Zip, sans vtk, gnuplot ni clisp, soit 295 Mo. `share/info` a été conservé, car Maxima en a besoin.
  - Octave 11.3.0 : zip officiel GNU.
  - `--grant-rx` a été appliqué sur les deux dossiers.
- `log\wims.conf` (sauvegarde dans `backup-exe-20261104`) :
  - `maxima_command` : `sbcl.exe --core …\maxima.core --noinform --dynamic-space-size 512 --end-runtime-options --eval (cl-user::run) --end-toplevel-options`, sans .bat ni cmd.exe ;
  - `octave_command` : `octave-cli.exe -Hq --no-line-editing --no-window-system --silent` ;
  - les anciennes lignes (C:\maxima-5.49.0 et Octave dans AppData) sont commentées « superseded ».
- Maxima fonctionne en AppContainer. SBX-S8 PASS ; IFC-06 Maxima PASS par wims.cgi.exe direct, avec un environnement minimal de type Apache.
- Octave échoue en AppContainer : `GetFinalPathNameByHandleW(VOLUME_NAME_DOS)` renvoie ERROR_ACCESS_DENIED (vérifié avec un petit exécutable de test ; VOLUME_NAME_NT fonctionne). Octave rattache alors ses fonctions .m au cwd. SBX-S6, SBX-S7 et IFC-06 Octave restent BLOQUÉS ; décision d'Arnaud attendue. Octave n'est pas ajouté à la liste lowil.
- Suites sandbox : 26/26 et 20/20.
- CGI-08 : Apache ne tournait pas (connexion refusée), test à relancer après démarrage.

## 2026-11-04 (suite) — D22, Octave sous la politique lowil

- Arnaud décide de faire tourner Octave comme GAP (D22). `lowil octave-cli.exe` est ajouté à `ch..root.conf`, dans le dépôt et sur le PC (sauvegarde dans `backup-exe-20261104`). `lowil_path` n'est pas nécessaire, car les DLL d'Octave sont à côté de l'exécutable.
- `--policy` : gap et octave-cli renvoient lowil ; gp, maxima, sbcl et fake_cas renvoient appcontainer.
- La suite lowil passe à 22/22 avec D22-01 et D22-02 :
  - Octave exécute det, ispc et strjoin ;
  - fopen dans le profil ou dans log renvoie -1 ; dans la session, l'écriture réussit.
- La suite sandbox reste à 26/26.
- Tests par wims.cgi.exe direct, avec un environnement de type Apache :
  - IFC-06 Octave : -2, 0.5 et a-b ;
  - SBX-S6 et SBX-S7 : system, dos, winqueryreg et __w2mpth__ sont refusés par l'interface ; aucun cmd.exe ni PING.EXE n'est lancé.
  - En contournant l'interface, `system("echo x > profil")` répond « Accès refusé ».
  - `unix()` n'est pas dans illegal[], mais c'est un no-op sous Windows.
- Gate P0 : 52/52.
- Apache est toujours arrêté : CGI-08 et les tests Laragon d'IFC-06 restent NON EXÉCUTÉS.
- check-linux-identical : 14/14.

## 2026-11-05 — `unix()` interdit dans l'interface Octave

- `unix` ajouté à `illegal[]` de `src/Interfaces/octave.c` (même mécanisme que `system`/`dos` : `find_illegal` remplace la 2ᵉ lettre par J → `uJix`).
- Vérifié sous Linux : rebuild de `Interfaces/octave` OK ; unit test `system`→`sJstem`, `unix`→`uJix`, `myunix` non modifié.
- Windows : `octave.exe` reconstruit (mingw), sauvegardé dans `backup-exe-20261105`, déployé dans le runtime PC.
- PC : IFC-06 Octave PASS (-2, 0.5, a-b) ; SBX-S6 (`system` et `unix` refusés) ; SBX-S7 (`dos`/`winqueryreg`) ; suites 26/26 et 23/23 (D22-03).
- Apache toujours arrêté : CGI-08 et IFC-06 via Laragon NON EXÉCUTÉS.
- Gate P0 : 52/52 inchangé.

## 2026-11-06 — Laragon : CGI-08 et IFC-06 Maxima/Octave

- WIMS_RT pointe vers le runtime frère `wims-runtime-r19040-grok-hook-20261106` (sans toucher à la config Laragon).
- Apache n'écoutait pas au démarrage (Laragon et MySQL oui, httpd non ; unclean shutdown dans error.log). `httpd.exe` relancé ; config Syntax OK ; vhost déjà avec les 4 ScriptAliasMatch.
- Config CAS du sibling déjà alignée (maxima/octave/gap_command, `lowil gap.exe` + `lowil octave-cli.exe`, octave.exe avec `unix` dans illegal[]).
- **IFC-06 via Laragon** `tool/directexec` : Maxima PASS (`(x-1)*(x+1)*(x^2+1)`, `sin(x)-x*cos(x)`) ; Octave PASS (`-2`, `0.5`, `a-b`) ; 0 cmd.exe.
- **CGI-08 via Laragon** : image flydraw → `/wims/wims.gif?cmd=getins…` → 200 `image/gif` GIF87a 839 o.
- Totaux plan 08 : 141 tests, 116 PASS, 24 NOT RUN, 0 BLOCKED, 1 FAIL (P2). Gate P0 toujours 52/52.

## 2026-11-06 (suite) — HLP-16, OEF-03/04, HLP-17, JOB-06, HLP-10

Runtime Laragon : `wims-runtime-r19040-grok-hook-20261106` (sans édition de la config Laragon).

- **HLP-16** PASS : `bin/run-mkindex.sh` → `EXIT:0` / Start-Process ExitCode=0 (`C:\Users\Shadow\mkindex5.log`, démarré 13:19, fini 13:46 heure de Paris). Cause du rc=2 : `find.exe` de System32 devant le `find` MSYS (« Fichier introuvable - *.bin »). Correctif : `export PATH="/usr/bin:/bin:$PATH"` en tête de `bin/mkindex` (patch `tools/patches/mkindex-wims4win.patch` + runtime). Avec PATH : 0× Fichier introuvable. Bénin : pas de `strfile` ; `modules/data/images` absent.
- **OEF-03** PASS : `module=local/exemple.fr&cmd=new&lang=fr` → 200, page ~12810 o.
- **OEF-04** PASS : `local/oef2html_web.fr` — 42 exo, new 42/42 status 200, reply 42/42 status 200, pas de WIMS User Error (le nom de module doit inclure `.fr`).
- **HLP-17** PASS : `./bin/oefcompile test local/oef2html_web.fr` → 42 sources, 42 compiled, Total failure: 0.
- **JOB-06** PASS : `rlimit_cpu=3` puis PARI lourd `sum(n=1,8*10^7,n%97)` via tool/directexec → ~3,3 s, message « PARI/GP n'a pas pu exécuter votre code » ; factor léger OK après restore. `log/wims.conf` restauré (0 ligne `rlimit`).
- **HLP-10** PASS (triage) : `crossword.exe wordlist` rc=0 ; « Cannot open bcd » = mauvais argv. Laragon exo=crossword → 200. Pas de correctif code.
- Totaux plan 08 (§7 réconcilié avec chaque ligne) : **138** tests, **114 PASS**, **24 NOT RUN**, **0 BLOCKED**, **0 FAIL** (P0 51/51, P1 57/9, P2 6/15).
- Dépôt : docs + patch PATH mkindex poussés sur `master` ; pas de branche code hors patch.

## 2026-11-06 (soir) — Audit des totaux §7 du plan 08

Arnaud a demandé de tracer 141/P0 52 (`0bb1553`) → 138/P0 51 (`1b47d97`) malgré 6 PASS.
Diff scripté des lignes §4 : **aucune** ID ajoutée/retirée/renommée ; **aucune** priorité
changée ; seulement les 6 statuts HLP-16/17, JOB-06, OEF-03/04, HLP-10. Les totaux 141
venaient de 3 ID fantômes (**ENC-05**, **BLD-04**, **HLP-18**) comptées dans §7 depuis
`b885a1f` sans jamais avoir de ligne §4. Aucune ligne légitime perdue. Totaux corrects :
**138** / **114 PASS** / **24 NOT RUN** / **0 FAIL** (P0 **51/51**, P1 57/9, P2 6/15).

## 2026-11-06 (soir) — Batch P1 restants + P2 cheap

Runtime Laragon : `wims-runtime-r19040-grok-hook-20261106`.

- **PATH-01** PASS : copie `rt with space` (live intact) ; SMK-01/02/03 OK (factor.fr greffé seulement dans la copie).
- **SBX-18** PASS : `fake_cas env` + ch..root ; PR https://github.com/io-e/wims4win/pull/11 (non fusionnée).
- **JOB-04** PASS : Job mem 64 Mo, alloc 200→3 / 32→0.
- **JOB-05** PASS : `wp_job_kill` via test_platform 22/22.
- **HLP-09** PASS : module local `canvasdraw_hlp09.fr`, JS canvasdraw servi en getfile.
- **LOGD-12** PASS (scripts sh) ; démon laissé tourner, keepdate effacé — collecte overnight dans `tests-win_ALF/LOGD-12_15_overnight.txt`.
- **LOGD-15** PASS (sibling) : cause = `HAVE_CRYPT` absent du config.h Win32 → `_passcrypt` no-op ; PR #12 ; exercice élève + Notes superviseur.
- **LIN-03** WAIVED → YAML-02.
- **SEC-02** WAITING : script `tests-win_ALF/SEC-02_pipe_acl_steps.ps1` ; 2e utilisateur Windows à fournir par Arnaud.
- P2 cheap : **PLT-02**, **HLP-11** (partiel), **PATH-02**, **YAML-03**, **CGI-07** PASS ; **SBX-14** skip (Arnaud : pas de purge) ; autres P2 différés avec motif.
- Incident `wims.cgi.exe` : **accident extensionless `wims.cgi` (pas AV)**. Créé 14:52:56 Europe/Paris ; exe restauré 14:59:28 ; AH02811 14:57–14:58. Defender 0.
- Totaux §7 (script) : **126/138 PASS**, 0 FAIL, P0 **51/51** (LOGD-15 PASS sur sibling).

### 2026-11-06 — sibling adm/class (décision propriétaire)

- Nouveau runtime sibling (live intact) :
  `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-r19040-grok-hook-class-20261106`
  = copie du hook live + modules `adm/class` complets (1537 fichiers) depuis `wims-runtime-r19040-grok`.
- Laragon `WIMS_RT` **non modifié** : toujours `…/hook-20261106/public_html`.
- Smokes CGI directs (env Apache-like) sur le sibling :
  - `adm/class/classes` → Status **200**, titre Virtual classes
  - AlgoGebra hook H6/test/oef2html.fr 00006/00076 → **8/8** (`hook_smoke_8.csv`)
  - CAS : `tool/algebra/factor.fr` greffé dans le sibling seulement → factor `2^64+1` = `67280421310721`
  - `adm/class/userscore` → 200 (page scores) ; login enseignant/élève 9001 encore en échec passcheck
- Ligne de bascule proposée (non appliquée) :
  `Define WIMS_RT "D:/ALF_ZGit/43-wims-paris-saclay_fr/Reprise_Wims_Grok/wims-runtime-r19040-grok-hook-class-20261106/public_html"`
- PR #11 laissée ouverte (fusion par Arnaud).


### 2026-11-06 — fix HAVE_CRYPT (login classe)

- Cause login 9001 `bad_password` : `src/Platform/win32/config.h` sans `HAVE_CRYPT` → `_passcrypt` vide alors que `wp_descrypt.c` est déjà linké.
- Correctif : `#define HAVE_CRYPT 1` ; `tools/check-linux-identical.sh` IDENTICAL pour `calc.c`.
- PR https://github.com/io-e/wims4win/pull/12 (non fusionnée). Déployé **uniquement** sur sibling `hook-class-20261106` (backup `Bak/cgi-before-have-crypt-20261106`). Live Laragon inchangé.
- LOGD-15 : élève `anonymous` feuille 1 exo exi1 → `score/anonymous1` ; superviseur page **Notes** 200.

## 2026-11-09 — LOGD-12 overnight collection (PC online)

Scheduled Nov 7 09:00 Europe/Paris collect found ShadowPC offline; re-run today 16:10 Europe/Paris (PowerShell only, no .bat/cmd.exe; live runtime untouched).

- Confirmed Laragon `WIMS_RT` → `wims-runtime-r19040-grok-hook-20261106/public_html` (read-only).
- Logs under runtime `tmp/log/` (sibling of `public_html`):
  - `housekeep.log` 789 B, mtime **2026-11-06 14:52:45** (manual MSYS sh proof; missing `log/stat/*` messages)
  - `modupdate.log` 50 B, mtime **2026-11-06 14:52:55** (« Waiting… OK. »)
  - `wimslogd.out` 53 B, mtime 2026-11-06 10:16:45 — only `wimslogd 12944 started at 20261106.10:16:45 (win32).`
  - `keepdate` absent (cleared 2026-11-06); bak `keepdate.bak-logd12` = `20261106`
  - `lastclean` mtime 2026-11-06 16:04:27; `mupdate` = `20261106`
  - `exec.fail` (mtime 2026-11-06 10:21:24): daemon failed to spawn `bin/housekeep.daily` and `bin/modupdate.auto` (error 0) earlier that morning
- **No Nov 7 / 8 / 9 overnight housekeep or modupdate** from wimslogd.
- `wimslogd` not running (stale pid file 12944). Laragon process up; ambient `cmd.exe` child of laragon.exe (started 2026-11-09 11:33) — not used by this collect or by the MSYS sh housekeep proof.
- **LOGD-12** remains **PASS** on the 2026-11-06 MSYS sh evidence; overnight daemon path did not add new runs.
- Totaux §7 (script, one row = one test) : **126/138 PASS**, 0 FAIL, P0 **51/51**, NOT RUN 10, WAIVED 1 (LIN-03), WAITING 1 (SEC-02).

## 2026-11-09 — wimslogd restart + housekeep/modupdate spawn retest

Owner OK (t90u): restart daemon on live `wims-runtime-r19040-grok-hook-20261106` only; trigger daily jobs via daemon path.

- Pre: stale pid 12944; `exec.fail` already had two «Failed to execute … error 0» from 2026-11-06 10:21; logs from manual MSYS sh only.
- Start 16:20:04 Europe/Paris: removed stale pid; `Start-Process bin\wimslogd.exe -WorkingDirectory <runtime root> -WindowStyle Hidden`; PATH put bundled `other\msys2\usr\bin\sh.exe` first. PID **5132**, `wimslogd.out`: `wimslogd 5132 started at 20261109.16:20:04 (win32).`
- Daemon path (not hand-run scripts): `housekeep()` / `modupdate()` via `thismin` ticks (`MINLENGTH` 47 + random `rshift`).
  - **16:22:08** housekeep: `keepdate=20261109`; `housekeep.log` truncated to **0 B**; `exec.fail` += housekeep.daily error 0.
  - **16:29:12** modupdate: `mupdate=20261109`; `modupdate.log` **0 B**; `exec.fail` += modupdate.auto error 0.
- PowerShell CreateFile repro: two `GENERIC_WRITE` opens on same path with `FILE_SHARE_READ` only → 2nd handle INVALID, **Win32 32 ERROR_SHARING_VIOLATION**; with `FILE_SHARE_READ|FILE_SHARE_WRITE` both OK.
- **Root cause:** `housekeep.c` `call_ssh(0,"bin/… &>%s/….log",tmpd)` → `outf==errf`; `wp_win32.c` `open_redirect` share mode too tight for stdout+stderr same file.
- **Proposed fix (not applied / not committed):** open once + `DuplicateHandle` for the second std handle when paths match (preferred), or add `FILE_SHARE_WRITE` in `open_redirect`.
- Daemon **left running** (PID 5132, healthy). No Laragon/config/code changes. Ambient Laragon `cmd.exe` unchanged; collect used PowerShell only.
- Totaux §7 inchangés : **126/138 PASS**, P0 51/51.

## 2026-11-09 — fix win32 `&>` shared redirect (LOGD-12 daemon spawn)

- **Debian ?** Non. Chemin Linux : `src/Wimslogd/wimslogdlines.c` `#else` → `fork` + `freopen(outf)` puis `freopen(errf)` (même chemin OK). Repro box : child_status=0, log écrit. Pas de `CreateFile` / partage de handles.
- **Fix Windows** (branche `fix/win32-wimslogd-shared-redirect`, PR ouverte, fusion par Arnaud) :
  - `wp_spawn` : si `outf` et `errf` même chemin → un `open_redirect` + `DuplicateHandle` pour stderr
  - `open_redirect` écriture : `FILE_SHARE_READ|FILE_SHARE_WRITE` (ceinture)
  - `execredirected` / `wp_spawn` : conserver le vrai `GetLastError` avant `CloseHandle`
- Build : `tools/build-wimslogd-win32.sh` → `wimslogd.exe` ; `tests/platform` 22/22 Linux ; `test_platform.exe` rebuild mingw.
- Vérif sibling PC prévue : `wims-runtime-r19040-grok-hook-logd-20261109` (live PID 5132 intact).

### Sibling verify (logd-20261109)

- Copie live → `wims-runtime-r19040-grok-hook-logd-20261109` (ask-before-overwrite: n'existait pas).
- `wimslogd.exe` corrigé déployé **uniquement** dans le sibling (bak `.bak-before-fix`). Live Laragon / PID **5132** intact.
- Sibling daemon PID **23708** démarré 16:44:28 Europe/Paris (pipe hashé par arbre → pas de collision).
- **housekeep OK** ~16:46 : `keepdate=20261109`, `housekeep.log` 809 o, **pas** de `exec.fail`.
- modupdate pas encore observé (PC offline ~16:53) ; même `call_ssh &>` que housekeep.
- CI PR #13 : Linux Platform / mingw interfaces / OEF tools = SUCCESS.
- À la reconnexion : confirmer modupdate + arrêter PID 23708 (pas 5132).

### Sibling modupdate confirm (2026-11-10 01:05–01:22 Europe/Paris)

- PC back online after reboot: neither daemon running (stale pids 23708 / 5132).
- Sibling: restarted fixed `wimslogd` (PID 18880 then 2068); `mupdate` reset to `20261101`.
- Early morning: `modupdatetime` (random 0–350) can block modupdate; one restart re-rolled the gate.
- **modupdate PASS 01:21:50–01:22:00:** `mupdate=20261110`, `modupdate.log` 50 B (« Waiting for running processes to terminate… OK. »), **pas de `exec.fail`**.
- housekeep also re-ran for 20261110 → log 809 B (spawn OK).
- Sibling daemon **PID 2068 stopped** (path `…hook-logd-20261109…` only).
- Live: restarted **old** binary as PID **11056** (hash ≠ fixed); Laragon/config untouched.

