# Architecture détaillée : portage natif MSVC de WIMS — V3 (fusion consolidée)

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

## 0. Validation de la V1_2 de Claude

### 0.1 Verdict global
La V1_2 **reprend quasiment toutes les corrections de la V2 Grok** : 24 errata E1-E24, `win32_spawn` unique, overlapped bidirectionnel, `Global\WimsShm`, timeouts sans longjmp, `ch..root.exe`, AF_UNIX conservé, `_O_BINARY`, `.bat` interdits, flex/bison, sh conservé, chiffrage aligné. Elle ajoute un **inventaire de `call_sh` (30+ appels)**, `Flydraw/objects.c:126` (`system`, vérifié) et un **plan détaillé de migration des Interfaces (§16)**, qui est un bon apport.

En revanche, **la partie Interfaces de calcul contient encore des erreurs de fond**, vérifiées dans `Interfaces/common.c` :
1. **Le mode `multiexec` (sessions CAS persistantes) est ignoré et cassé** par le code proposé.
2. Le **timeout CAS est mal décrit** : il n'y a ni `alarm` ni `SIGALRM` dans `Interfaces/`.
3. **Les CRLF des sorties CAS** ne sont pas traités (`_O_BINARY` ne règle pas ce point).
4. Le **buffer de sortie n'est pas borné** (débordement possible).
5. **GAP est lancé via un script `gap.sh`.**
6. La mémoire partagée est en **DACL NULL**.

**Verdict :** la V1_2 est **exploitable comme base**, après correction de la phase P3 (Interfaces). C'est l'objet de cette V3.

### 0.2 Corrections de la V2 Grok : reprises ou non
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

### 0.3 Tableau de validation de la V1_2 (focus Interfaces de calcul)
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

## 1. Architecture cible (consolidée)
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

## 2. Décisions d'architecture (V3)
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

## 3. win32compat
La structure V1_2 (§3.1) est validée. Ajouts :
- `stdio_bin.c` : `_setmode` sur les 3 flux standard.
- `crlf.c` : `strip_cr()` en place sur un tampon.
- `jobname.c` : Job nommé par session (création et ouverture pour le nettoyage).
- `peek.c` : `win32_poll_readable(HANDLE, timeout_ms)` via `PeekNamedPipe` + attente courte, qui remplace les `select` de `calc.c`.

### 3.1 `win32_dopipes` V3 (spécification, alignée sur `common.c:502-566`)
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

### 3.2 Le reste
- Le mode pipe avec thread d'écriture (V1_2 §3.2.3) est à écrire en entier.
- Les timeouts de `lines.c` (V1_2 §3.5) sont validés.
- `cleantmpdir` (V1_2 §3.3) est à compléter : âge, suppression récursive, `PROC_QUOTA`.

## 4 à 9. Phases P1 à P6
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

## 10. Phase P7 : tests (périmètre réduit)
| Test | Retenu | Commentaire |
|---|---|---|
| T1-T9 | ✅ | Compilation, CGI, CAS unitaires, OEF simple, OEF + Maxima, flydraw, texgif, wimslogd, classes |
| T10 (5 utilisateurs) | ✅ smoke | Fumée de concurrence (sessions, mémoire partagée), pas un test de charge |
| T11 (200 modules en parallèle) | ❌ **Hors périmètre** | Recette |
| T12 sécurité / évasion | ✅ | Indissociable du livrable `ch..root.exe` |
| T13 (30 utilisateurs) | ❌ **Hors périmètre** | Recette |
| T14 robustesse | ✅ | Arrêt de wimslogd, timeouts, CAS tués, fuites de handles |

Effort : **3 semaines** (au lieu de 5-6).

## 11. Chiffrage V3
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

## 12. Jalons
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

## 13. Risques
1. **Sandbox** (jeton/AppContainer) : risque n°1, conditionne l'ouverture aux élèves.
2. **multiexec** : régressions subtiles (sessions CAS persistantes, marqueurs aléatoires).
3. **CAS Windows** : options CLI, sorties CRLF, scripts lanceurs (maxima, gap.sh) ; Yacas, M2, Scilab incertains.
4. **Divergence avec l'amont** : reverser les patchs à wimsdev.
5. **Recette hors développement** (T11/T13) : à planifier séparément, sinon des régressions resteront invisibles jusqu'en production.

## 14. Recommandation
- La V1_2 corrigée par cette V3 est **une base de développement valide**.
- **Commencer par P0, puis P3 (C1 → C3b) et P3b** : les Interfaces de calcul et la sandbox concentrent le risque.
- Un **prototype Cygwin/MSYS + Apache (2-4 semaines)** reste conseillé comme référence fonctionnelle Windows avant d'engager 6-7 mois.

## Annexe : écarts V1_2 → V3 en bref
- **Repris de la V1_2 :** inventaire `call_sh`, `objects.c:126`, plan C1-C6, liste des binaires CAS, Chromium sandbox en référence.
- **Corrigé :** fermeture de stdin (multiexec), tampon non borné, marqueurs, BROKEN_PIPE, `linebyline1` mort, timeout CAS (pas d'alarm), CRLF, `_setmode` stdio, DACL NULL, privilège de `CreateProcessAsUser`, PID → Job nommé, configuration `<prog>_command`, `gap.sh`.
- **Retiré :** T11 (200 modules), T13 (30 utilisateurs) ; le rôle « VPS Debian » n'est pas confirmé.