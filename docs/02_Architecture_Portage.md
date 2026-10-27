# 02 — Architecture du portage natif Windows de WIMS (document consolidé)

*Document consolidé le 27/10/2026 (heure de Paris). Il fusionne l'architecture de Claude (V1_3), la revue Grok V2, la fusion V3 et l'étude « portage natif MSVC » de Claude.*

## Sources fusionnées

| Source (dans `archive/`) | Auteur | Statut |
|---|---|---|
| `Architecture_Portage_MSVC_WIMS_V3.md` | Grok (fusion Claude V1_2 + Grok V2) | Partie A, intégrale : décisions consolidées |
| `Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md` | Claude | Partie B, intégrale : spécification détaillée (spawn, dopipes, sandbox, SHM, timeouts, CMake, stratégie de test) |
| `Architecture_Portage_MSVC_WIMS_GROK_V2.md` | Grok | Partie C : seule la validation point par point (§0) ; ses §1–14 sont repris par V1_3 et V3 |
| `Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md` | Claude | Partie D, intégrale : correspondances POSIX → Win32, projets de référence (PostgreSQL, Win32-OpenSSH, nginx, Redis), pièges |

## Architecture réellement implémentée (prime sur les parties A à D)

- **Chaîne de build :** cross-compilation **mingw-w64 depuis Debian** (`tools/build-*.sh`), CI GitHub Actions. Le build **MSVC/CMake** décrit en parties A–D **n'est pas implémenté**.
- **Couche portable `src/Platform/` :** `wims_platform.h`, `wp_posix.c` / `wp_win32.c` (CreateProcess, pipes overlapped, Job Objects, watchdog CPU), `wp_mx.c` (multiexec), `wp_alarm.c` (TimerQueue), `wp_cgi_win32.c` (préparation CGI), crypt DES + BCrypt, `sandbox/chroot_win.c` (AppContainer sans capability), `tools/insdraw_win.c`.
- **Frontal web :** Apache (Laragon) en CGI ; `wimsd` n'est pas porté.
- **wimslogd :** named pipe dont le nom est un hash FNV-1a du chemin du runtime (une instance par arbre) ; `wp_daemon_spawn` n'hérite pas du stdout CGI.
- **CAS :** appel direct des binaires, jamais de `.bat` ni de `cmd.exe` ; Maxima via `sbcl.exe --core` ; GAP via `gap.exe` + runtime Cygwin dans le `PATH`.
- **Limite CPU :** `CREATE_SUSPENDED` → affectation au Job → `ResumeThread`, plus watchdog (user+kernel) et délai mural de 2× le budget.
- **Taille des formules :** `log/wims4win.yaml` (`formula.size`, 200 % par défaut, 50–400 %), lu par `mathml.c` ; identique sous Linux et Windows.
- **Linux :** objets identiques octet pour octet hors `_WIN32` (`tools/check-linux-identical.sh`), sauf le correctif argv `--tex-size` voulu par Arnaud.

## Divergences résolues

1. **MSVC + CMake (A–D) contre mingw-w64 + scripts sh (code) :** le code fait foi ; la partie CMake reste une cible documentaire. Voir « Points à trancher » dans `README.md`.
2. **IPC wimslogd :** V1_3/V3 conservaient AF_UNIX ; le code utilise un named pipe (accepté par l'audit Claude V2).
3. **`sh.exe` MSYS2 embarqué pour `call_sh` :** prévu en A–B ; non embarqué à ce jour, les commandes `mv`/`rm` sont internes. Les autres appels `call_sh` restent à traiter (point ouvert).
4. **Stratégie de test VPS (partie B §15) :** la box Debian de Grok sert de référence Linux ; pas de VPS séparé (issue #5 fermée). Tests parallèles sur 200 modules et charge à 30 utilisateurs hors périmètre.
5. **Chiffrage (6–7 mois en V3) :** caduc ; voir `05_Etat_et_Decisions.yaml`.


---

## Partie A — Architecture V3 (fusion consolidée)

> Source : `archive/Architecture_Portage_MSVC_WIMS_V3.md`

## Architecture détaillée : portage natif MSVC de WIMS — V3 (fusion consolidée)

**Date :** 23 octobre 2026, 16 h 30 (heure de Paris)
**Bases :** `Reprise_Wims_Claude\Architecture_Portage_MSVC_WIMS_CLAUDE_V1_2.md` (1 380 lignes, lue en entier) et `Reprise_Wims_Grok\Architecture_Portage_MSVC_WIMS_V2.md`.
**Vérification :** checkout `Reprise_Wims_Claude\wims`, `trunk/wims` **r19040**, fichier `version` = **4.33a**.
**Périmètre :** portage du serveur WIMS sur Windows natif (MSVC + Win32), avec `sh.exe`/coreutils MSYS2 embarqués pour `call_sh` et les scripts.
**Hors périmètre (décision de l'utilisateur) :**
- les tests de non-régression parallèles sur 200 modules (ex-T11) ;
- le test de charge à 30 utilisateurs (ex-T13).

Ces deux campagnes relèvent de la recette, pas du développement.

**Estimation V3 :** **27 à 30 semaines ≈ 6 à 7 mois** (1 développeur) ; **≈ 4 à 4,5 mois** (2 développeurs). Voir §11.

---

### 0. Validation de la V1_2 de Claude

#### 0.1 Verdict global
La V1_2 **reprend quasiment toutes les corrections de la V2 Grok** : 24 errata E1-E24, `win32_spawn` unique, overlapped bidirectionnel, `Global\WimsShm`, timeouts sans longjmp, `ch..root.exe`, AF_UNIX conservé, `_O_BINARY`, `.bat` interdits, flex/bison, sh conservé, chiffrage aligné. Elle ajoute un **inventaire de `call_sh` (30+ appels)**, `Flydraw/objects.c:126` (`system`, vérifié) et un **plan détaillé de migration des Interfaces (§16)**, qui est un bon apport.

En revanche, **la partie Interfaces de calcul contient encore des erreurs de fond**, vérifiées dans `Interfaces/common.c` :
1. **Le mode `multiexec` (sessions CAS persistantes) est ignoré et cassé** par le code proposé.
2. Le **timeout CAS est mal décrit** : il n'y a ni `alarm` ni `SIGALRM` dans `Interfaces/`.
3. **Les CRLF des sorties CAS** ne sont pas traités (`_O_BINARY` ne règle pas ce point).
4. Le **buffer de sortie n'est pas borné** (débordement possible).
5. **GAP est lancé via un script `gap.sh`.**
6. La mémoire partagée est en **DACL NULL**.

**Verdict :** la V1_2 est **exploitable comme base**, après correction de la phase P3 (Interfaces). C'est l'objet de cette V3.

#### 0.2 Corrections de la V2 Grok : reprises ou non
| Correction V2 | Dans V1_2 ? | Commentaire |
|---|---|---|
| HANDLE_LIST (STARTUPINFOEX) | ✅ E2, §3.2 | OK |
| Overlapped dans les 2 sens | ✅ E3, §3.2.1-3.2.2 | OK, mais voir V1 à V5 ci-dessous |
| Mode pipe : thread d'écriture + timeout | ✅ E4, §3.2.3 | Code laissé en « … » : à écrire |
| `Global\WimsShm` + descripteur de sécurité | ⚠️ Partiel | Nom corrigé, mais **DACL NULL** (§3.4), donc accès total pour tous |
| Timer = TerminateJob + drapeau | ✅ E6, §3.5 | OK pour `lines.c` |
| Sandbox jeton restreint + ACL + `ch..root.exe` | ✅ E7, E15, §3.3 | `CreateRestrictedToken` / `CreateProcessAsUser` seulement en commentaire ; le droit `SeAssignPrimaryTokenPrivilege` n'est pas traité |
| setreuid → compte de service + jeton | ✅ E8 | OK |
| mmap de `wims.c` commenté | ✅ E9 | OK |
| crypt dans `calc.c` / `HAVE_CRYPT` | ✅ E10 | OK |
| 2 exécutables OefMsg2wims, noms d'origine des CAS | ✅ E11, E12 | OK |
| Sites fork/exec/system complétés | ✅ E13 (+ `objects.c:126`) | OK, mais **`multiexec` (`lines.c` mxtab) manque** |
| AF_UNIX Winsock conservé | ✅ E14 | OK |
| Scripts gardés en sh | ✅ E16 | OK |
| flex/bison, Misc complet | ✅ E18, E19 | OK |
| `.bat` interdits (Maxima) | ✅ E20 | OK, mais **`gap.sh` oublié** |
| `_O_BINARY`, `/utf-8`, manifeste | ⚠️ Partiel (E21) | `_set_fmode` **n'affecte pas stdin/stdout/stderr déjà ouverts** : il manque `_setmode(_fileno(stdout),_O_BINARY)` (sortie CGI, GIF de flydraw vers stdout `flydraw.c:80`) |
| ConnectNamedPipe, nom aléatoire | ✅ E22 | OK |
| PID → HANDLE | ⚠️ Partiel (E24) | Les **fichiers de PID** (`common.c:142-156` `addpid`/`rmpid`, `exec.pid` `:375`) et `cleaning.c:31` `kill(atoi(...))` restent fondés sur des PID |
| Filtrage Octave étendu | ✅ C5.1 | OK |
| loadavg, service robuste | ✅ §8 | OK |

#### 0.3 Tableau de validation de la V1_2 (focus Interfaces de calcul)
Légende : ✅ validé ; ✏️ corrigé ; ➕ ajouté ; ❌ erreur.

| # | Point V1_2 | Verdict | Justification (fichier:ligne, r19040) |
|---|---|---|---|
| V1 | §3.2.2 `win32_dopipes` ferme stdin après la phase 1 | ❌ → ✏️ | **`multiexec`** : `common.c:578-594` boucle `rep:` (putparm, dopipes, readresult, `getstdin`, `goto rep`). Le **même processus CAS** sert plusieurs requêtes successives. Fermer stdin tue la session. Il ne faut fermer stdin qu'en mode non-multiexec (où `quitstring` est envoyé, `:580-582`) |
| V2 | Chaîne wims → interface (multiexec) absente | ➕ | `calc.c:229-258` : wims garde des pipes persistants `mxtab[].pipe_stdin/stdout/stderr` vers le programme d'interface, écrit `parm` + `multiexec_random`, lit jusqu'au marqueur (`select` `:242`), puis sonde stderr (`select` timeout 0 `:255`). C'est un **deuxième niveau de pipes à porter**. Sur Windows, `PeekNamedPipe` suffit (sondage, pas d'overlapped) |
| V3 | §3.2.2 copie sans borne (`memcpy(outptr, …)`) | ❌ → ✏️ | L'original borne à `fsizelim` (`common.c:533,551`). Le paramètre `obufsize` de Claude n'est jamais utilisé : **débordement de tampon** |
| V4 | §3.2.2 phase 2 : arrêt sur `endstring` n'importe où | ✏️ | L'original cherche `endstring` **après `startstring`** (`common.c:541-544,560-564`) ; sinon l'écho de l'entrée peut fausser la détection |
| V5 | §3.2.2 phase 2 : ReadFile en échec suivi de `readPending=TRUE` | ✏️ | Si le CAS est mort (`ERROR_BROKEN_PIPE`), on attend tout le timeout. Il faut tester l'erreur et sortir immédiatement (équivalent de `l<=0`, `:552`) |
| V6 | §16.3 « select sur pipes, `common.c:480-559` » | ✏️ | **`common.c:455-498` (`linebyline1`, fifo) n'est pas compilé** (« not used », `#ifdef linebyline1`). Seul `dopipes` (`:502-566`) est actif. S1 (`:494`) est du code mort : rien à porter |
| V7 | §16.4 « `common.c:443,599` : `signal(SIGALRM)` + `alarm` » | ❌ | Il n'y a **aucun `alarm`/`SIGALRM` dans `Interfaces/`**. `:443` et `:599` sont des `kill(mxpid,SIGKILL)`. Le timeout CAS est le **compteur d'intervalles de `dopipes`** (`interval=20000 µs`, `timeout=300` : 6 s de silence, `:505-506`). Le délai global vient du **parent** (`lines.c` `forkalarm`, qui tue l'interface) |
| V8 | Arrêt du CAS quand l'interface est tuée | ➕ | Sous Linux, le CAS orphelin est tué via les fichiers de PID et le nettoyage. Sous Windows : **l'interface est lancée dans un Job (par wims) et le CAS hérite du Job**. `TerminateJobObject` tue toute l'arborescence. Prévoir les Jobs imbriqués (Windows 8+) puisque `ch..root.exe` crée le sien |
| V9 | §6.2 V1 `wims.conf [cas_paths]` | ✏️ | Mécanisme **déjà existant** : la variable d'environnement `<prog>_command` (`common.c:370-372`) surcharge `nameofcmd`. Il suffit de la renseigner dans la configuration Windows, sans nouveau format |
| V10 | Lancement GAP | ➕ ❌ | `gap.c:37` `nameofcmd="gap.sh -T -n"` : c'est un **script shell**. Sous Windows, il faut soit lancer le binaire GAP (`gap.exe` + arguments du script), soit passer par sh.exe |
| V11 | Lignes de commande des autres CAS | ✅ ➕ | `maxima.c:33` `maxima` (script sous Linux aussi → sbcl.exe + `--core`), `pari.c:39` `gp -f -q`, `octave.c:37` `octave -Hf --no-line-editing`, `scilab.c:36` `scilab -nw -ns`, `yacas.c:36` `yacas -p`, `m2.c:33` `M2`. Les options Windows sont à vérifier une par une (C1) |
| V12 | C5.3 « `_setmode(fd,_O_BINARY)` sur les pipes évite les CRLF » | ❌ → ✏️ | Le mode binaire côté **parent** n'empêche pas un CAS Windows (CRT en mode texte) **d'écrire `\r\n`**. Il faut **supprimer les `\r`** dans la sortie lue (`dopipes`/`readresult`) **avant** la recherche de `startstring`/`endstring` et la sortie HTML. Côté entrée, envoyer `\n` (accepté par gp, sbcl, octave). C'est un point de compatibilité, pas de sécurité |
| V13 | `exec.pid` et `addpid`/`rmpid` (`common.c:142-156,216,375`) | ➕ | Les fichiers de PID servent à `cleaning.c:31` (`kill(atoi(p1),SIGKILL)`). Sous Windows, PID recyclé : il faut un **Job nommé par session** (`Local\wims_<session>`) que le nettoyage ouvre et termine |
| V14 | Filtrage Octave `octave.c:73-77` + extension | ✅ | Validé, avec les ajouts `dos`, `unix`, `winopen`, `winqueryreg`, `!`. ➕ Vérifier aussi `system` sous PARI (`system()`, `extern()`), Maxima (`system`, `:lisp`) et GAP (`Exec`, `Process`). Ces fonctions existent sous Linux aussi ; c'est la **sandbox qui les neutralise** |
| V15 | §3.4 DACL NULL sur `Global\WimsShm` | ❌ | Accès total pour tout processus, y compris les CAS sandboxés. Il faut une DACL explicite dès le départ (SDDL : `D:(A;;GA;;;SY)(A;;GR;;;<SID wims-svc>)`) |
| V16 | §3.3 jeton restreint « passé à `CreateProcessAsUser` » | ✏️ | `CreateProcessAsUser` exige `SeAssignPrimaryTokenPrivilege` (le compte de service Apache ne l'a pas par défaut). Alternative sans privilège : **`CreateProcess` avec `PROC_THREAD_ATTRIBUTE_SECURITY_CAPABILITIES` (AppContainer)**, ou un jeton dérivé du sien via `CreateRestrictedToken` + `CreateProcessAsUser`, autorisé pour un jeton restreint du même utilisateur. À valider par un prototype |
| V17 | §3.6 `_set_fmode` | ✏️ | Ajouter `_setmode(_fileno(stdin/stdout/stderr), _O_BINARY)` dans `wims.exe`, flydraw, texgif et les interfaces |
| V18 | §2.4 inventaire `call_sh` (30+) | ✅ | Échantillon vérifié (`exec.c:1804` sendmail ; `objects.c:126` `system`) |
| V19 | §15 « VPS Debian de Grok Bot » | ⚠️ | Je n'ai **pas de VPS Debian fourni** à ce jour ; mon environnement est une machine Linux de travail. Il peut servir de référence pour les **sorties CAS unitaires (C6)**. Le corpus de 200 modules (T11) est hors périmètre |
| V20 | §10 T11 (200 modules) et T13 (30 utilisateurs) | ✏️ Retirés | Décision de l'utilisateur : hors développement. Chiffrage ajusté (§11) |
| V21 | Bogue existant `calc.c:241-244` | ➕ | `v1=0` est réinitialisé à chaque tour de boucle : le timeout de 3 s du multiexec **ne se déclenche jamais** (attente bornée seulement par `k`). À corriger au passage, sous Linux comme sous Windows, et à signaler à l'amont |

---

### 1. Architecture cible (consolidée)
```
Navigateur ──HTTP──► Apache Win64 (mod_cgi), service, compte « wims-svc »
                  │ CreateProcess
                  ▼
   wims.exe (MSVC /utf-8 ; _setmode stdio binaire)
     ├─ IPC wimslogd : AF_UNIX Winsock (select conservé)
     ├─ multiexec : pipes anonymes persistants vers les interfaces + PeekNamedPipe
     ├─ win32_spawn() : STARTUPINFOEX + HANDLE_LIST + Job (nommé par session)
     └─ call_sh → sh.exe MSYS2 + coreutils
          │
          ▼
   Interfaces (pari.exe, maxima.exe, gap.exe, octave.exe, …), noms d'origine
     └─ ch..root.exe (AppContainer ou jeton restreint + Job imbriqué + ACL session)
          └─ CAS : gp.exe / sbcl.exe --core maxima.core / gap.exe / octave-cli.exe
             ↔ named pipes overlapped 2 sens ; suppression des \r ; sortie bornée (fsizelim)
wimslogd.exe : service Windows ; Global\WimsShm (DACL explicite) ; loadavg ; tâches
```

### 2. Décisions d'architecture (V3)
Celles de la V1_2 (§1.3) sont **validées**, avec ces ajouts :

| Décision | V3 |
|---|---|
| multiexec | Porter `mxtab` (`lines.c`) et la boucle `rep:` (`common.c:578-594`) ; stdin **jamais fermé** en multiexec |
| Sondage wims → interface | `PeekNamedPipe` (pipes anonymes) au lieu de `select` (`calc.c:242,255`) |
| Fins de ligne CAS | Suppression des `\r` en lecture, avant la recherche des marqueurs |
| Tampons | Borne `fsizelim` conservée |
| Configuration CAS | Variables `<prog>_command` existantes (`common.c:370`) |
| Scripts lanceurs (`maxima`, `gap.sh`) | Remplacés par l'appel direct du binaire + arguments (ou sh.exe si inévitable) |
| Arbre de processus | Job nommé par session ; le nettoyage termine le Job (et non un PID) |
| Mémoire partagée | DACL explicite (SDDL) dès la V1 |
| Jeton enfant | Prototype AppContainer (sans privilège) en priorité ; sinon jeton restreint |

### 3. win32compat
La structure V1_2 (§3.1) est validée. Ajouts :
- `stdio_bin.c` : `_setmode` sur les 3 flux standard.
- `crlf.c` : `strip_cr()` en place sur un tampon.
- `jobname.c` : Job nommé par session (création et ouverture pour le nettoyage).
- `peek.c` : `win32_poll_readable(HANDLE, timeout_ms)` via `PeekNamedPipe` + attente courte, qui remplace les `select` de `calc.c`.

#### 3.1 `win32_dopipes` V3 (spécification, alignée sur `common.c:502-566`)
```c
/* Entrées : inputbuf..inputptr ; sortie obuf borné à fsizelim ; startstring/endstring.
 * Boucle 1 (tant qu'il reste à écrire) : overlapped R + W, interval 20 ms, timeout 300 intervalles.
 *   - écriture ligne par ligne (comme :524-530)
 *   - lecture : l = min(dispo, fsizelim-(outptr-obuf)-1) ; si erreur/BROKEN_PIPE -> stop
 * strip_cr(obuf) après chaque lecture (avant les strstr).
 * Boucle 2 : lire jusqu'à endstring APRÈS startstring (comme :540-565) ou timeout ou EOF.
 * NE PAS fermer stdin : en multiexec, run() reboucle (goto rep) ; hors multiexec,
 *   quitstring est déjà dans l'entrée (:580-582) et le CAS se termine seul.
 * timed_out (Job terminé par wims) -> retour immédiat. */
```

#### 3.2 Le reste
- Le mode pipe avec thread d'écriture (V1_2 §3.2.3) est à écrire en entier.
- Les timeouts de `lines.c` (V1_2 §3.5) sont validés.
- `cleantmpdir` (V1_2 §3.3) est à compléter : âge, suppression récursive, `PROC_QUOTA`.

### 4 à 9. Phases P1 à P6
- **P1, P2, P4, P5 et P6 de la V1_2** sont validées, avec les ajouts :
  - **P2** : `calc.c` multiexec → `PeekNamedPipe` (+1 j) ; `_setmode` stdio (+0,5 j) ; correction du bogue `calc.c:241` (0,5 j).
  - **P4** : flydraw stdout binaire (inclus).
- **P3 Interfaces (révisée) :**

| Étape | Contenu | Effort |
|---|---|---|
| C1 | Inventaire des binaires et options Windows (gp, sbcl + maxima.core, gap.exe sans gap.sh, octave-cli, scilab, yacas, M2) ; variables `<prog>_command` | 2 j |
| C2 | `win32_spawn(SPAWN_CAS)` via `ch..root.exe`, Job imbriqué, nommé par session | 3 j |
| C3 | `win32_dopipes` V3 (bornes, CRLF, marqueurs, BROKEN_PIPE, sans fermeture de stdin) | 4 j |
| C3b | **multiexec** : boucle `rep:`/`getstdin` (`common.c:126-140,578-594`) + `mxtab` côté wims | 3 j |
| C4 | Timeouts : intervalles dopipes + Job terminé par le parent ; nettoyage par Job nommé (remplace `exec.pid`) | 2 j |
| C5 | Filtrage Octave étendu + revue des fonctions système PARI/Maxima/GAP | 1,5 j |
| C6 | Tests unitaires CAS (1+1, factor, isprime, inv) comparés à une référence Linux, sorties 10 Mo, CAS bloqué, CAS tué | 2 j |
| **Total P3** | | **17,5 j ≈ 3,5 sem** |

- **P3b sandbox :** celle de la V1_2 (13 j), plus le prototype AppContainer vs jeton restreint (2 j) et la DACL de la mémoire partagée (0,5 j) : **≈ 3,5 sem**.

### 10. Phase P7 : tests (périmètre réduit)
| Test | Retenu | Commentaire |
|---|---|---|
| T1-T9 | ✅ | Compilation, CGI, CAS unitaires, OEF simple, OEF + Maxima, flydraw, texgif, wimslogd, classes |
| T10 (5 utilisateurs) | ✅ smoke | Fumée de concurrence (sessions, mémoire partagée), pas un test de charge |
| T11 (200 modules en parallèle) | ❌ **Hors périmètre** | Recette |
| T12 sécurité / évasion | ✅ | Indissociable du livrable `ch..root.exe` |
| T13 (30 utilisateurs) | ❌ **Hors périmètre** | Recette |
| T14 robustesse | ✅ | Arrêt de wimslogd, timeouts, CAS tués, fuites de handles |

Effort : **3 semaines** (au lieu de 5-6).

### 11. Chiffrage V3
| Phase | V1_2 Claude | **V3** | Raison |
|---|---|---|---|
| P0 Cadrage (tag vs trunk, `public_html`, inventaire) | 1 sem | **1 sem** | = |
| P1 CMake + win32compat | 3 sem | **3 sem** | + stdio_bin, crlf, peek, jobname (petits) |
| P2 Moteur wims.exe | 4 sem | **4,5 sem** | multiexec côté wims, `_setmode`, bogue `calc.c:241` |
| P3 Interfaces CAS | 3 sem | **3,5 sem** | multiexec, CRLF, bornes, gap.sh, Job imbriqué |
| P3b Sandbox + sécurité | 3 sem | **3,5 sem** | Prototype AppContainer/jeton, DACL |
| P4 Outils | 3,5 sem | **3,5 sem** | = |
| P5 wimslogd | 2 sem | **2 sem** | = |
| P6 Scripts (sh) | 2 sem | **2 sem** | = |
| P7 Tests (sans T11/T13) | 5-6 sem | **3 sem** | Décision de l'utilisateur |
| Marge intégration ≈ 15 % | 3-4 sem | **3,5 sem** | |
| **Total (1 développeur)** | 30-32 sem | **≈ 29,5 sem → fourchette 27-30 sem ≈ 6-7 mois** | |
| **Total (2 développeurs)** | 4-5 mois | **≈ 4-4,5 mois** | P4/P6 en parallèle de P2/P3 |

### 12. Jalons
| Jalon | Fin de | Critère |
|---|---|---|
| J0 | S1 | Version figée, `public_html` inventorié |
| J1 | S4 | Build vert |
| J2 | S8 | Accueil via Apache, timeouts `lines.c` OK |
| J3 | S12 | PARI et Maxima OK en **mode simple et multiexec**, CRLF filtrés, 10 Mo sans interblocage |
| J4 | S15 | `ch..root.exe` passe T12 |
| J5 | S19 | Exercice OEF complet (CAS, flydraw, texgif) |
| J6 | S21 | wimslogd en service, mémoire partagée Global avec DACL |
| J7 | S27-30 | T1-T10, T12, T14 verts : livraison pour recette (T11/T13 en recette) |

### 13. Risques
1. **Sandbox** (jeton/AppContainer) : risque n°1, conditionne l'ouverture aux élèves.
2. **multiexec** : régressions subtiles (sessions CAS persistantes, marqueurs aléatoires).
3. **CAS Windows** : options CLI, sorties CRLF, scripts lanceurs (maxima, gap.sh) ; Yacas, M2, Scilab incertains.
4. **Divergence avec l'amont** : reverser les patchs à wimsdev.
5. **Recette hors développement** (T11/T13) : à planifier séparément, sinon des régressions resteront invisibles jusqu'en production.

### 14. Recommandation
- La V1_2 corrigée par cette V3 est **une base de développement valide**.
- **Commencer par P0, puis P3 (C1 → C3b) et P3b** : les Interfaces de calcul et la sandbox concentrent le risque.
- Un **prototype Cygwin/MSYS + Apache (2-4 semaines)** reste conseillé comme référence fonctionnelle Windows avant d'engager 6-7 mois.

### Annexe : écarts V1_2 → V3 en bref
- **Repris de la V1_2 :** inventaire `call_sh`, `objects.c:126`, plan C1-C6, liste des binaires CAS, Chromium sandbox en référence.
- **Corrigé :** fermeture de stdin (multiexec), tampon non borné, marqueurs, BROKEN_PIPE, `linebyline1` mort, timeout CAS (pas d'alarm), CRLF, `_setmode` stdio, DACL NULL, privilège de `CreateProcessAsUser`, PID → Job nommé, configuration `<prog>_command`, `gap.sh`.
- **Retiré :** T11 (200 modules), T13 (30 utilisateurs) ; le rôle « VPS Debian » n'est pas confirmé.


---

## Partie B — Architecture détaillée de Claude (V1_3)

> Source : `archive/Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md`

## Architecture détaillée — Portage natif MSVC de WIMS 4.33a (r19040)

**Date:** 2026-10-23 (rév. V1_3 2026-10-23)  
**Scope:** Portage complet du serveur WIMS sur Windows natif (MSVC + Win32 API)  
**Estimation révisée :** 6 – 7,5 mois (1 dev), 4 – 5 mois (2 devs)  
**Prérequis :** Étude V1_1 (`Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md`)  
**Base :** Architecture V1 (`Architecture_Portage_MSVC_WIMS_CLAUDE_V1.md`) corrigée après 2 cross-reviews Grok  
**Sources :** trunk `wims` **r19040**, fichier `version` = **4.33a** (pas 4.32)

#### Historique des reviews

| Review | Document Grok | Points | Corrections appliquées dans |
|--------|---------------|--------|-----------------------------|
| **Review 1** (étude MSVC) | Commentaires intégrés dans l'audit | 9 erreurs + 6 oublis | `Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md` (commit `98cae01`) |
| **Review 2** (architecture) | `Architecture_Portage_MSVC_WIMS_GROK_V2.md` | 40 points (24 corrections) | Ce document V1_3 (§0 errata) |

---

### 0. Traçabilité des cross-reviews Grok

#### 0.1 Review 1 — Étude MSVC (9 erreurs + 6 oublis)

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

#### 0.2 Review 2 — Architecture V1 (40 points, 24 corrections)

Grok a validé l'architecture V1 point par point contre les sources r19040 dans `Architecture_Portage_MSVC_WIMS_GROK_V2.md`. **Toutes les corrections ont été intégrées** dans cette V1_3.

#### 0.3 Tableau des corrections de la Review 2

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

### 1. Vue d'ensemble de l'architecture cible

#### 1.1 Architecture Linux actuelle

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

#### 1.2 Architecture Windows cible (corrigée)

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

#### 1.3 Décisions d'architecture (révisées)

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

### 2. Cartographie des sources r19040 (corrigée)

#### 2.1 Arbre source

L'arbre de V1 (§2.1) est **validé**, avec ces corrections :
- `auth.c` n'utilise pas libcrypt : c'est **`calc.c:2007-2020`** (`#ifdef HAVE_CRYPT`)
- `OefMsg2wims` produit **2 exécutables** (`oef2wims`, `msg2wims`)
- `Misc/*.c` : ajouter `suffix.c`, `translator.c` (+ `translator_.c`), `test.c`
- Sous-dossiers Misc manquants dans CMake : `chemeq` (flex/bison + C++), `units-filter` (flex/bison), `symtext`, `mathexp`, `voronoiW`, `whirlgif`, `bioinfo`, `lceb`, `checkmol`, `crossword`, `sigdigits`, `moneyprint`, `scienceprint`, `toascii`

#### 2.2 Sites de création de processus (complétés : 14+ sites)

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

#### 2.3 select() sur pipes (corrigé)

- **S1-S4** (`Interfaces/common.c:494,519,549` ; `calc.c:242,255`) : pipes → **overlapped I/O** (inchangé vs V1)
- **S5-S7** (`wimslogd.c:197`, `socket.c:85`, `lines.c:1364`) : **OK si on garde AF_UNIX Winsock** (Win10 1803+). Si named pipes → à réécrire aussi. **Décision V1_1 : garder AF_UNIX Winsock**, ce qui préserve select() et minimise les changements.

#### 2.4 call_sh / call_ssh — inventaire complet (30+ appels)

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

### 3. Module win32compat/ — couche d'abstraction (révisée)

#### 3.1 Structure (corrigée)

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

#### 3.2 `win32_spawn()` — point d'entrée unique (corrigé)

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

#### 3.2.1 Mode CAS : overlapped bidirectionnel (corrigé)

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

#### 3.2.2 `win32_dopipes()` — overlapped bidirectionnel (corrigé)

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

#### 3.2.3 Mode PIPE : avec thread d'écriture (corrigé)

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

#### 3.3 Sandbox `ch..root.exe` (corrigé — livrable complet)

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

#### 3.4 Mémoire partagée (corrigée)

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

#### 3.5 Timeouts (corrigé — pas de longjmp inter-thread)

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

#### 3.6 Encodage et mode binaire (ajouté)

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

### 4. Phase 1 — Build system CMake (corrigé)

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

### 5. Phase 2 — Moteur wims.exe (révisé)

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

### 6. Phase 3 — Interfaces CAS (révisé)

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

#### Phase 3b — Sandbox `ch..root.exe` + sécurité

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

### 7. Phase 4 — Outils auxiliaires (révisé)

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

### 8. Phase 5 — Wimslogd service (révisé)

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

### 9. Phase 6 — Scripts (révisé : garder sh)

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

### 10. Phase 7 — Tests (révisé)

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

### 11. Chiffrage révisé

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

### 12. Jalons (révisés)

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

### 13. Risques

1. **Sécurité du sandbox** : risque n°1. Sans isolation équivalente à chroot, ne pas ouvrir le service aux élèves. La partie `ch..root.exe` conditionne la mise en production.
2. **Divergence avec l'amont** : reverser les patchs `#ifdef _WIN32` à wimsdev (GPL). Sinon chaque mise à jour du trunk coûte cher.
3. **CAS Windows** : versions et options CLI différentes. Yacas, M2 et Scilab peut-être indisponibles ou instables sous Windows.
4. **Encodages et fins de ligne** : CRLF vs LF dans les modules OEF. Régressions silencieuses. `_O_BINARY` + `/utf-8` mitigent mais ne couvrent pas tout.
5. **Dépendance à MSYS2 sh** : le portage n'est « natif » que pour le C. sh.exe + coreutils sont un runtime embarqué. Assumé.
6. **Recyclage de PID** : utiliser les HANDLE des enfants, pas `OpenProcess(pid)`.

---

### 14. Recommandation

- L'architecture est **valide et exploitable avec les corrections ci-dessus**.
- **Avant d'engager 6 à 7 mois**, il reste recommandé de faire un **prototype MSYS2 + Laragon (2-4 semaines)**, qui sert aussi de **référence de non-régression** pour T11.
- Si le natif est confirmé, commencer par **P0, P1 puis P3 + P3b** (CAS et sandbox), là où se concentrent les risques techniques.
- **Stratégie parallèle** : Grok Bot effectue la migration sur son VPS Debian (§15), ce qui produit un WIMS Linux de référence pour les tests de non-régression (T11). Claude travaille sur le portage MSVC Windows en parallèle. Voir §15 et §16.

---

### 15. Stratégie de test : VPS Debian (Grok Bot) + ShadowPC Windows (Claude)

#### 15.1 Répartition des rôles

| Rôle | Environnement | Responsable | Livrables |
|------|---------------|-------------|-----------|
| **Référence Linux** | VPS Debian (fourni par l'utilisateur) | Grok Bot | WIMS 4.33a compilé + fonctionnel, corpus de 200+ modules OEF testés, snapshots HTML de référence pour T11 |
| **Portage MSVC** | ShadowPC Windows 11 | Claude | win32compat/, wims.exe MSVC, ch..root.exe, tests Windows |
| **Non-régression T11** | Les deux | Comparaison | Diff automatique HTML Linux vs Windows sur le corpus |

#### 15.2 Ce que Grok Bot fait sur le VPS Debian

1. **Installer WIMS 4.33a** depuis le trunk r19040 (ou tag décidé en P0)
2. **Compiler nativement** avec tous les CAS (Maxima, PARI, GAP, Octave, Scilab, Yacas, M2)
3. **Vérifier la disponibilité des CAS** et documenter les versions/options réelles
4. **Générer le corpus de référence T11** : 200+ modules OEF → HTML de sortie archivés
5. **Tester les interfaces CAS** : commandes de référence pour chaque CAS, sortie attendue
6. **Commencer le portage de `common.c`** (Interfaces CAS) comme premier chantier

#### 15.3 Le VPS comme oracle de non-régression

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

#### 15.4 Protocole de non-régression T11

| Étape | Action | Outil |
|-------|--------|-------|
| 1 | Sélectionner 200+ modules OEF diversifiés (maths, physique, chimie, langues) | Manuel sur VPS |
| 2 | Pour chaque module, générer le rendu HTML avec des paramètres fixes (seed=42) | `curl -s "http://localhost/wims/wims.cgi?module=...&seed=42"` |
| 3 | Archiver les HTML de référence | `ref/module_name.html` |
| 4 | Reproduire les mêmes requêtes sur Windows | Même curl |
| 5 | Diff structurel (ignorer timestamps, session IDs, chemins absolus) | Script de normalisation + `diff` |
| 6 | Toute différence = bug à investiguer | Triage : cosmétique (CRLF, chemins) vs fonctionnel (résultat CAS, image, score) |

---

### 16. Migration des Interfaces CAS — étapes détaillées

`src/Interfaces/common.c` regroupe l'essentiel du code CAS. C'est le **premier chantier** du portage (P3), car il concentre les 3 difficultés majeures : overlapped I/O, sandbox, et lancement de processus.

#### 16.1 Étape C1 — Inventaire des binaires CAS sur chaque plateforme

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

#### 16.2 Étape C2 — Port de `common.c` : lancement des CAS

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

#### 16.3 Étape C3 — Port de `dopipes()` : dialogue bidirectionnel

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

#### 16.4 Étape C4 — Timeout et arrêt des CAS

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

#### 16.5 Étape C5 — Sécurité des CAS

| # | Action | Détail | Effort |
|---|--------|--------|--------|
| C5.1 | Filtrage Octave étendu | Ajouter `dos`, `unix`, `winopen`, `winqueryreg`, `!` aux mots interdits (`octave.c:73-77`) | 0.5 j |
| C5.2 | Audit filtres autres CAS | Vérifier si Maxima/GAP/PARI ont des fonctions d'accès système à filtrer sous Windows | 1 j (Grok VPS : documenter les commandes dangereuses par CAS) |
| C5.3 | Binaire mode | Mode binaire sur les pipes (`_setmode(fd, _O_BINARY)`) pour éviter les décalages CRLF dans les résultats CAS | 0.5 j |
| C5.4 | Chemins CAS | Configuration externe des chemins (pas en dur) ; conversion `/` → `\` là où nécessaire ; variables d'environnement CAS ajustées | 1 j |

#### 16.6 Étape C6 — Validation croisée VPS ↔ Windows

| # | Test | VPS (référence) | Windows (à valider) |
|---|------|-----------------|---------------------|
| C6.1 | Maxima : `factor(x^4-1);` | `(x-1)*(x+1)*(x^2+1)` | Idem ? |
| C6.2 | PARI : `isprime(17)` | `1` | Idem ? |
| C6.3 | GAP : `Size(SymmetricGroup(4));` | `24` | Idem ? |
| C6.4 | Octave : `inv([1 2;3 4])` | `[-2 1; 1.5 -0.5]` | Idem ? |
| C6.5 | gnuplot : `plot sin(x)` | Image GIF | Diff pixel ? |
| C6.6 | Module OEF complet avec CAS | HTML de référence | Diff structurel T11 |

**Toute différence = bug à investiguer.** Le VPS est l'oracle.

#### 16.7 Ordre de migration recommandé

```
C1 Inventaire binaires ──► C2 Lancement (spawn) ──► C3 Dialogue (dopipes)
                                                          │
                            C4 Timeout ◄──────────────────┘
                                │
                            C5 Sécurité ──► C6 Validation croisée VPS↔Win
```

**common.c est le bon premier chantier** : il centralise fork+exec, pipes, select, chroot, setreuid, et timeout. Une fois porté, chaque fichier CAS (`maxima.c`, `pari.c`, ...) ne demande que des ajustements de chemins et d'options.

---

### Annexe A — Code de référence des 4 projets

Identique à V1, **plus** :

| Projet | Fichier | Ce qu'il enseigne |
|--------|---------|------------------|
| **Chromium** | `sandbox/win/` | AppContainer, jeton restreint, intégrité Low — modèle pour `ch..root.exe` |
| **Microsoft docs** | `CreateRestrictedToken` | Jeton avec SID restreints + intégrité |

---

### Annexe B — Checklist de démarrage (révisée)

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


---

## Partie C — Validation point par point de l'architecture Claude V1 (Grok V2, §0)

> Source : `archive/Architecture_Portage_MSVC_WIMS_GROK_V2.md` — §1–14 repris par les parties A et B

### 0. Validation de l'architecture Claude V1

#### 0.1 Verdict global
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

#### 0.2 Tableau de validation point par point
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

#### 0.3 Remarques préalables
- **Version :** r19040 = 4.33a. Avant de démarrer, lancer `svn ls https://subversion.renater.fr/anonscm/svn/wimsdev/tags`, puis décider : porter le trunk (recommandé, puisque le portage sera reversé) ou un tag 4.32.
- **`public_html` est vide dans le checkout** : les scripts `public_html/scripts`, les modules et le contenu ne sont pas analysés. Faire un `svn update`, puis inventorier.

---


---

## Partie D — Étude du portage natif MSVC (Claude V1_1)

> Source : `archive/Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md`

## Étude — Portage natif MSVC : comment ça marche et qui l'a déjà fait

**Date:** 2026-10-23 (rev 2026-10-23)  
**Objectif:** Détailler la mécanique d'un portage fork/exec → CreateProcess, et lister les projets open source qui l'ont fait pour s'en inspirer.

> **⚠️ ERRATA V1_1** — Corrige les erreurs de la V1, identifiées par relecture croisée (audit Grok) et vérification dans les sources r19040. Voir §8 pour le détail des corrections.

---

### 1. Le problème fondamental

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

#### Tableau de correspondance POSIX → Win32

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

### 2. Projets open source qui l'ont fait — leçons à tirer

#### 2.1 🏆 PostgreSQL — le modèle de référence

**Le portage le plus documenté et le plus proche de WIMS** (serveur C, fork-per-connection → CreateProcess).

- **Repo :** [github.com/postgres/postgres](https://github.com/postgres/postgres) → `src/port/` et `src/backend/postmaster/`
- **Historique :** Portage commencé en 2002, livré en PostgreSQL 8.0 (2005). [Présentation OSCON 2005](https://wiki.postgresql.org/images/4/46/OSCON2005-PortingToWindows.pdf).
- **[Mailing-list : fork/exec patch](https://www.postgrespro.com/list/thread-id/1997277)** — les patches `CreateProcess` de Claudio Natoli (2003-2004).

##### Architecture du portage PostgreSQL

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

##### La technique clé : "re-exec self"

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

##### Leçons pour WIMS

| Leçon PostgreSQL | Application WIMS |
|------------------|-----------------|
| Répertoire `src/port/` séparé | Créer `src/win32compat/` |
| `#ifdef WIN32` partout dans le code existant | Idem — ne pas toucher au code Linux |
| `kill()` → wrapper qui appelle `TerminateProcess` | `src/win32compat/kill.c` |
| Signaux POSIX émulés via threads | `src/win32compat/signal.c` |
| Shared memory via `CreateFileMapping` | Pour le `mmap()` de wimslogd |
| **Re-exec self** pour remplacer fork | Non nécessaire si on utilise Apache CGI (voir §3.7) |

---

#### 2.2 🥈 Win32-OpenSSH — couche POSIX complète

**Port officiel Microsoft d'OpenSSH (daemon sshd, fork-per-session).**

- **Repo :** [github.com/PowerShell/openssh-portable](https://github.com/PowerShell/openssh-portable) → `contrib/win32/openssh/`
- **Releases :** Jusqu'à 10.0.0.0 (oct 2025)
- **[Wiki design](https://github.com/PowerShell/Win32-OpenSSH/wiki)**

##### Architecture

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

##### Leçons pour WIMS

| Leçon OpenSSH | Application WIMS |
|---------------|-----------------|
| `contrib/win32/` = tout le code Windows séparé | Garder les .c Windows à part |
| `openbsd-compat/` = fonctions manquantes | `posix-compat/` pour WIMS |
| `posix_spawn()` utilisé quand possible | Alternative à fork+exec pour les CAS |

---

#### 2.3 🥉 nginx — architecture sans fork

**nginx ne fork PAS les workers. Il les crée avec CreateProcess dès le départ.**

- **Source :** [`src/os/win32/`](https://nginx.googlesource.com/nginx/+/refs/heads/master/src/os/win32/) — 39 fichiers
- **Approche :** Le code Linux utilise `fork()` pour les workers. Le code Windows utilise `CreateProcess()` + arguments CLI pour que chaque worker charge sa propre config.

##### Les fichiers clés de src/os/win32/

| Fichier | Rôle | Pertinent pour WIMS ? |
|---------|------|----------------------|
| `ngx_process.c` | `CreateProcess(CREATE_NO_WINDOW)` | ✅ Modèle pour lancer les CAS |
| `ngx_process_cycle.c` | Boucle master/worker | ✅ Si wimsd est conservé |
| `ngx_shmem.c` | `CreateFileMapping` / `MapViewOfFile` | ✅ Pour mmap WIMS |
| `ngx_socket.c` | Winsock wrappers | ✅ Pour wimslogd sockets |
| `ngx_service.c` | Service Windows (SCM) | ✅ Pour wimsd/wimslogd |
| `ngx_time.c` | Timers Windows | ✅ Pour alarm/SIGALRM |

##### La technique clé : "workers autonomes"

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

#### 2.4 Redis on Windows — mmap et shared memory

**Microsoft a porté Redis sur Windows (2013-2019). Le fork() servait au snapshot mémoire (BGSAVE).**

- **Repo :** [github.com/microsoftarchive/redis](https://github.com/microsoftarchive/redis) (archivé)
- **Couche :** `src/Win32_Interop/`

##### Technique : Copy-on-Write applicatif

- **v2.8 :** `CreateFileMapping()` (memory-mapped file) — le dataset Redis est dans un fichier mappé
- **v3.0 :** Utilise le pagefile système au lieu d'un fichier nommé

##### Pertinence pour WIMS

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

### 3. Points critiques — erreurs corrigées et pièges

#### 3.1 execredirected() existe en 4 versions distinctes

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

#### 3.2 Mémoire partagée : c'est mmap, pas shmget

**Erreur V1 :** Le header de compatibilité proposait des wrappers pour `shmget/shmat/shmctl` (System V IPC).

**Réalité dans les sources r19040 :**

```c
// wimslogd.c:141 — c'est mmap(), pas shmget()
shmptr = mmap(0, SHM_SIZE, PROT_READ|PROT_WRITE, MAP_SHARED, mfd, 0);

// wims.c:1224 — idem, en lecture seule
// shmptr = mmap(0, SHM_SIZE, PROT_READ, MAP_SHARED, mfd, 0);
```

WIMS utilise `mmap()` sur un fichier partagé, pas les fonctions System V. Le remplacement correct est `CreateFileMapping()` + `MapViewOfFile()` (voir §2.4).

#### 3.3 wimsd : ne pas le porter — utiliser Apache CGI

**Erreur V1 :** Proposait de porter `wimsd` (le daemon HTTP maison) avec la technique "re-exec self" de PostgreSQL.

**Correction :** C'est du travail inutile. `wimsd` n'est qu'un mini-serveur HTTP qui dispatche vers `wims.exe`. Apache fait exactement la même chose en CGI. Sur Windows :

- **Laragon** (déjà installé) fournit Apache natif avec `mod_cgi`
- Apache lance `wims.exe` via `ScriptAlias` — c'est un `CreateProcess` natif
- Pas besoin de porter le fork-per-request de wimsd

**→ Supprimer wimsd du scope. Utiliser Apache CGI.**

#### 3.4 select() sur pipes : le piège Windows

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

#### 3.5 Sécurité : chroot/setreuid comme noop = danger silencieux

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

#### 3.6 Shell : cmd.exe ne marche pas

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

#### 3.7 Composants oubliés en V1

| Composant | Fichier(s) | Problème | Impact |
|-----------|------------|----------|--------|
| **wimslogd** | `src/Wimslogd/wimslogd.c` | Daemon complet : double fork, sockets Unix, mmap, timer, nettoyage. Sa propre `execredirected()` et `call_sh()`. | 🔴 Portage majeur. Alternative : réécrire en service Windows. |
| **Texgif** | `src/Texgif/texgif.c`, `basic.c`, `gf.c`, `tfm.c` | Sa propre `execredirected()`. `call_sh()` avec `cp`, `mkdir -p`, `kpsewhich`. Chemins `/tmp`. | 🟠 Compiler avec MSVC + MiKTeX. Adapter les chemins. |
| **canvasdraw + OpenBabel** | `src/Misc/canvasdraw/canvasdraw.c:6330,6372` | 2 `fork()` directs : un pour MathML (pipe), un pour OpenBabel (pipe). | 🟠 Remplacer par CreateProcess. OpenBabel existe sur Windows. |
| **libcrypt** | `configure:3895`, `config.hin:62` | `HAVE_LIBCRYPT` — utilisé pour `crypt()` (hash de mots de passe). | 🟡 Remplacer par `bcrypt.lib` (Win32) ou OpenSSL. |
| **sendmail** | `exec.c:1804` | `call_sh("cat mail.eml \| /usr/sbin/sendmail -t")` | 🟠 Remplacer par msmtp (Windows natif) ou PowerShell `Send-MailMessage`. |
| **Filtrage sécurité Octave** | scripts bin/ et Interfaces | Octave doit être sandboxé pour empêcher l'exécution de code arbitraire via `system()`. | 🟠 Job Objects + filtrage des commandes Octave autorisées. |

#### 3.8 Confusion MSYS2 en V1

**Erreur V1 :** Confondait "MSYS2 MinGW" (compilateur natif, PAS de fork) avec "MSYS2 MSYS" (runtime Cygwin-like, fork émulé via `msys-2.0.dll`).

**Clarification :**

| Mode MSYS2 | Shell | Compilateur | fork() | Binaires dépendent de... |
|------------|-------|-------------|--------|-------------------------|
| **MSYS** | bash MSYS2 | gcc (msys2) | ✅ Émulé (lent) | msys-2.0.dll |
| **MINGW64** | bash MSYS2 | gcc (mingw-w64) | ❌ N'existe pas | Rien (natif Win32) |
| **MSVC** | PowerShell/cmd | cl.exe (MSVC) | ❌ N'existe pas | Rien (natif Win32) |

Pour un portage MSVC natif, fork n'existe pas. Chaque `fork()` doit être remplacé par `CreateProcess()`. Le mode MSYS (avec fork émulé) est une **autre piste** (piste C du document de recherche), pas la piste MSVC.

---

### 4. La fonction centrale portée correctement

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

### 5. Le header de compatibilité (corrigé)

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

### 6. Effort révisé

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

### 7. Recommandation révisée

#### Étudier ces dépôts dans cet ordre :

1. **PostgreSQL** (`src/port/win32/` + `src/backend/postmaster/postmaster.c`) — le cas le plus proche de WIMS
2. **nginx** (`src/os/win32/`) — architecture la plus propre (39 fichiers)
3. **Win32-OpenSSH** (`contrib/win32/openssh/`) — couche POSIX la plus complète
4. **Redis Windows** (`src/Win32_Interop/`) — uniquement pour mmap → CreateFileMapping

#### Le portage natif vaut-il le coup pour WIMS ?

**Non pour un usage local.** Le ratio est clair :

| Approche | Effort | Résultat | Usage cible |
|----------|--------|----------|-------------|
| MSYS2 + Laragon | 2-3 semaines | WIMS fonctionnel, fork lent | 1-5 utilisateurs locaux |
| Portage MSVC natif | 4,5-6,5 mois | WIMS natif, performant | 30+ élèves simultanés |

**Recommandation : prototyper d'abord sous MSYS2 (piste C) avec Apache Laragon en CGI.** Si la performance est insuffisante, le code source patché pour MSYS2 servira de base au portage MSVC (les `#ifdef` sont les mêmes).

---

### 8. Errata V1 → V1_1

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

### Références

- [UNIX Application Migration Guide (Microsoft)](https://learn.microsoft.com/en-us/previous-versions/ms811896(v=msdn.10))
- [PostgreSQL OSCON 2005 : Porting to Windows (PDF)](https://wiki.postgresql.org/images/4/46/OSCON2005-PortingToWindows.pdf)
- [PostgreSQL fork/exec patch thread](https://www.postgrespro.com/list/thread-id/1997277)
- [Win32-OpenSSH wiki](https://github.com/PowerShell/Win32-OpenSSH/wiki)
- [nginx win32 source](https://nginx.googlesource.com/nginx/+/refs/heads/master/src/os/win32/)
- [Redis Windows (archived)](https://github.com/microsoftarchive/redis)
- ["A fork() in the road" — Microsoft Research (2019)](https://www.microsoft.com/en-us/research/uploads/prod/2019/04/fork-hotos19.pdf)
