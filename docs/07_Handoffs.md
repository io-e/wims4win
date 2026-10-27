# 07 — Handoffs Claude ↔ Grok (regroupés)

*Regroupement du 27/10/2026 (heure de Paris). Les handoffs sont des instantanés : leur état « à faire » est historique. L'état courant est dans `05_Etat_et_Decisions.yaml`.*

| Source (dans `archive/`) | Sens | Date | Partie |
|---|---|---|---|
| `HANDOFF_Claude_OEF_Python.md` | Grok → Claude | 25/10/2026 | A — **actif** : chaîne Python OEF (oef2html, validation) |
| `Handoff_Grok_Wims03_CLAUDE_V1.md` | Claude → Grok | 23/10/2026 | B — phase 4 (réalisée) |
| `Handoff_Grok_Wims02_CLAUDE_V1.md` | Claude → Grok | 23/10/2026 | C — phases 1–3 (réalisées) |

## Divergences résolues
- Les « priorités VPS » des parties B et C sont caduques (box Debian = référence ; issue #5 fermée).
- Le « dépôt unique » (H9) est appliqué : `io-e/wims4win`. Les documents de Claude y sont désormais intégrés dans `docs/` (consolidés), et non plus dans `Reprise_Wims_Claude`.


---

## Partie A — Handoff Grok → Claude : chaîne Python OEF

> Source : `archive/HANDOFF_Claude_OEF_Python.md`

## Handoff pour Claude : chaîne Python OEF (validation et génération HTML)

Rédigé par Grok Bot le 25/10/2026 pour Arnaud Lefrancois. Dépôt : `io-e/wims4win` (privé), outils dans `tools/oef-validation/`.

### 1. Objectif
Reprendre et faire évoluer le code Python de la chaîne OEF :
- **`tools/oef-validation/html/oef2html.py`** : produit des pages HTML statiques (énoncé et correction) à partir de `.oef`, via le vrai moteur WIMS. Le moteur est soit le Linux, soit **wims4win `wims.cgi.exe`** sous Windows.
- **`tools/oef-validation/llm/oef_llm_fix.py`** : correction assistée par LLM (Ollama ou API OpenAI-compatible). Il boucle sur « compiler avec `oef2wims-v2.exe` → proposer un correctif ».
- **L'appel du validateur Python depuis `oef2wims-v2.exe`** (`--validator <oef_validate.py> [--python ...]`, code dans `v2/oef2wims_v2.c`) : les diagnostics du validateur sont fusionnés dans `nom.log`/`nom.json`.

### 2. État actuel et résultats mesurés
- **oef2wims (WIMS 4.31b, sources non modifiées), Linux et Windows (mingw)**, sur 1438 fichiers : parité **1438/1438** identique (stdout, stderr, code de retour, `.def`). Même résultat pour `oef2wims-v2.exe` et pour `oef2wims-trace.exe` face à `oef2wims.exe`. Ces mesures ont été refaites le 25/10 depuis le dépôt.
- **Corpus :** 333 fichiers viennent du tarball upstream 4.31b, 5 sont des cas d'erreur et 1100 sont des exercices Eleda convertis par xml2oef. Ces derniers ne sont pas publiés ; voir `corpus/rebuild_corpus.sh`.
- **oef2html sur PC avec wims4win** (natif, échantillon de 10 fichiers, graine 1, 22 s) :
  - 6 pages sont **identiques** à Linux après normalisation : 00001 (image flydraw comprise), 00006, 00013, 00076, 00403 (PARI) et 00726.
  - 4 échecs, **identiques** à Linux : 00347, 00352 et 00509 en too_many_gotos, 01012 en bad_index.
- **Moteur :** WIMS 4.33a porté nativement (wims4win). flydraw est porté (libgd statique, GIF) avec un `insdraw..processor.exe` natif.

### 3. Lancer sur le PC (Windows, sans .bat ni cmd.exe : PowerShell + python.exe)
- **Runtime :** `D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef` (copie de `wims-runtime`, servi par Laragon).
  - `log\wims.conf` (fins de ligne LF) : `default_texposition=2` (MathML, obligatoire sinon la formule est vide) ; `pari_command=D:/ALF_ZGit/43-wims-paris-saclay_fr/Reprise_Wims_Grok/tests-win/cas/pari/gp.exe -f -q -p 150000`.
  - `log\wims4win.yaml` : `formula:` puis `size: 200%`. Plage de 50 à 400 % ; les grandes formules sont voulues pour les élèves DYS. C'est lu par wims, sous Linux comme sous Windows.
- **Commande :**
  ```powershell
  $RT='D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Grok\wims-runtime-oef'
  python oef2html.py --wims-home $RT --oef2wims <...>\oef2wims.exe --cgi "$RT\public_html\wims.cgi.exe" --out out --json D:\mes_oef\*.oef
  ```
  `OEF2HTML_DUMP=<dossier>` vide les réponses CGI brutes, ce qui sert au débogage.
- **Comparaison** avec une référence Linux : `python compare_html.py ref out`.
- **Copies existantes sur le PC :** `Reprise_Wims_Grok\oef-wims4win\` (oef2html, sample, out_pc2 et out_pc3).

### 4. Décisions d'Arnaud
- Moteur **wims4win 4.33a**, sans rebasage sur 4.31b. La seule exception assumée est le comportement Linux de la taille des formules, qui vient maintenant du YAML.
- Les **58 too_many_gotos** du corpus sont ignorés (« vieilles études »). Ils viennent des exercices Eleda convertis par xml2oef : procédure récursive non traduite dans un `\while`, ou grands entiers affichés en notation e+NN. Voir `docs/Evaluation_OEF_wims4win.md` §3.
- flydraw est porté et PARI/GP est configuré.
- **Dépôt unique** `io-e/wims4win` ; ne rien écrire dans `Reprise_Wims_Claude`.
- **Le validateur d'Arnaud `oef_validate.py` n'est pas publié** dans le dépôt ; il est seulement appelé via `--validator`.
- Pas de `.bat` ni de `cmd.exe` : lancer les exe et `python.exe` directement.

### 5. Limites connues
- **insdraw :** une animation ne donne que sa 1re image, et il n'y a pas de conversion de format. Un GIF est livré sous le nom demandé, faute d'ImageMagick.
- **flydraw :** pas de freetype, donc pas de texte en police TTF.
- **`html/Dockerfile` :** non testé.
- **Encodage :** quand `oef2wims-v2.exe` lance le validateur Python, il faut `PYTHONIOENCODING=utf-8`, sinon les accents sont mal décodés ou provoquent un échec sous une console cp1252. Ce n'est pas encore forcé par v2.
- **Feedback de bonne réponse :** oef2html n'affiche pas encore le feedback d'une bonne réponse (`\feedback` quand la réponse est juste) ; c'est à faire.
- **CAS :** seuls PARI et Maxima sont configurés côté wims4win. Les autres (GAP, Octave…) sont à vérifier exercice par exercice.

### 6. Tâches prioritaires pour Claude
1. **oef2html : affichage du feedback de bonne réponse.** Pour chaque réponse, envoyer la bonne réponse (et pas seulement une réponse fausse) et intégrer le feedback correspondant dans la page de correction. Ajouter des tests sur 00076 et 00726.
2. **v2 + validateur :** définir `PYTHONIOENCODING=utf-8` dans l'environnement du processus fils (`v2/oef2wims_v2.c`). Valider sur `exemples/exemples_v2` (c14_latin1_bom en particulier) sous Windows.
3. **Chaîne unique en Python :** commande `oef-check` en stdlib qui enchaîne oef2wims-v2 (+ validateur), oef_llm_fix (optionnel) puis oef2html, et produit un rapport JSON/HTML par fichier. Fonctionnement Windows natif.
4. **Tests automatisés en Python :** unittest/pytest stdlib sur `html/exemples/sources` et `corpus/erreurs`, avec une référence de sortie normalisée (`compare_html.py`). Les brancher dans `tools/oef-validation/ci.sh`.
5. **oef_llm_fix :** tester sur les cas `exemples_v2` avec `llm/test/mock_ollama.py`, puis avec un vrai modèle. Mesurer le taux de correction et ne jamais écraser le `.oef` sans sauvegarde.
6. **Documentation PC :** mettre à jour `README_HTML.txt` et `README_LLM.txt` pour wims4win (`--cgi`, `wims-runtime-oef`, YAML de taille).
7. **Plus tard :** Dockerfile (le tester ou le retirer), images animées/conversion, aide à la correction xml2oef des 58 too_many_gotos si Arnaud le demande.

### 7. Où trouver quoi
- Outils : `tools/oef-validation/README.md` (build, parités, corpus, licences GPL).
- Évaluation et explications (formule, CRLF, 110/120 %, PARI, flydraw) : `docs/Evaluation_OEF_wims4win.md`.
- Suivi : `docs/Suivi_Developpement_Portage.md`.
- Runtime : `tools/build-runtime-tree.sh` (inclut `default_texposition=2`, le YAML, flydraw et insdraw).


---

## Partie B — Handoff Claude → Grok, session 03

> Source : `archive/Handoff_Grok_Wims03_CLAUDE_V1.md`

## Handoff → Grok Bot — Session 03 (Phase 4)

**Date :** 2026-10-23
**De :** Claude (ShadowPC)
**Pour :** Grok Bot (VPS Debian)
**Repo :** `Reprise_Wims_Grok/wims-port/` — HEAD `4374c68`
**Audit :** `Cahier_Audit_Portage_WIMS_CLAUDE_V1_1.md`
**Recette :** `Cahier_Recette_Portage_WIMS_CLAUDE_V1_1.md`

---

### 1. Bilan Phase 3 — Validé ✅

Tout le plan G1-G6 est livré. Résumé :

| Task | Commit | Résultat |
|------|--------|----------|
| G2 — lines.c multiexec sur Platform | `1cd0534` | ✅ Linux byte-identical |
| G1 — ch..root.exe sandbox | `c13459b` | ✅ AppContainer selftest OK sur ShadowPC |
| G3 — CRLF exec.err | `f81abf1` | ✅ |
| G5 — 7 interfaces .exe | `e13ec24` | ✅ Compilation. Octave filtrage étendu. |
| G6 — wims.exe 814Ko | `4374c68` | ✅ Link OK. Panique attendue (wimslogd absent). |
| G4 — Job Object natif | tests-win | ✅ 22/22, mémoire OK. **❌ CPU limit non appliqué.** |

**Score tests : ~91 exécutés sur ~170 (53%).**

---

### 2. Décision H9 — Dépôt unique

**`wims-port/` est le dépôt unique.** Claude produit specs + audit, Grok produit le code.
Pas de fusion à faire, pas de second dépôt C.

---

### 3. Phase 4 — Actions prioritaires

#### Ordre recommandé (par impact sur le démarrage de wims.exe) :

| # | Action | Bloqueur | Effort estimé | Réf recette |
|---|--------|:--------:|:-------------:|:-----------:|
| **N2** | **Port wimslogd sans fork** | **wims.exe ne démarre pas** | 1-2 semaines | P4.1-P4.4 |
| **N1** | **crypt → BCrypt (Windows) / crypt_r (Linux)** | **Auth bloquée** | 3-5 jours | P4.5-P4.9 |
| **N11** | **Éliminer fork() résiduel dans wims.exe** | **Crash Windows** | 2-3 jours | — |
| N6 | Installer CAS Windows (sbcl, gap, octave-cli) | Tests CAS | 1-2 jours | T-CAS M/G/O |
| N3 | MathML helper sans fork | Fallback GIF = dégradé | 3-5 jours | P4.10-P4.11 |
| N5 | CPU limit diagnostic | Non bloquant (mitigé) | 1-2 jours | P4.12-P4.14 |
| N4 | Vérifier réseau sandbox | Devrait déjà marcher | 0.5 jour | S3 |

#### Détails techniques par action :

##### N2 — wimslogd

wimslogd est un daemon lancé par fork() dans le code original. Sous Windows :
- **Option A :** le porter en service Windows (plus propre, mais complexe)
- **Option B :** le lancer comme processus séparé avant wims.exe (plus simple)

Préférence : **Option B d'abord** (prototype rapide), Option A en Phase 5 si nécessaire.

Le daemon utilise AF_UNIX (socket) pour l'IPC avec wims.cgi. Architecture V1_3 §5 prévoit
AF_UNIX Winsock (Windows 10 1803+). Vérifier que `select()` sur AF_UNIX Winsock fonctionne.

Tests : P4.1 compile, P4.2 démarre, P4.3 wims.exe ne panique plus, P4.4 logs écrits.

##### N1 — crypt → BCrypt

Le stub actuel refuse toute authentification (fail-closed — correct en sécurité).
Remplacement :
- **Windows :** `BCryptGenerateSymmetricKey` / `BCryptHashData` (pas de dépendance externe)
- **Linux :** `crypt()` standard (déjà en place, pas de changement)
- **Compatibilité cross :** les hash stockés dans les fichiers de classes doivent être les mêmes.
  Problème : crypt() Linux utilise DES/MD5/SHA-512 selon le préfixe du salt. BCrypt Windows
  ne fait pas la même chose. **Solution proposée :** utiliser un hash portable (SHA-256 ou
  SHA-512 avec salt, implémentation identique des deux côtés). Attention à la rétrocompatibilité
  avec les classes existantes.

Tests : P4.5-P4.9 dans le cahier de recette.

##### N11 — fork() résiduel

Le Suivi mentionne un `fork()` encore frappé dans le chemin de démarrage wims.exe.
Identifier l'appel exact (`grep -rn 'fork()' src/wims/`), le remplacer par `wp_spawn`
ou une alternative Windows, et s'assurer que le chemin Linux reste byte-identical.

##### N6 — CAS Windows

Installer sur ShadowPC :
- **Maxima :** chercher `sbcl.exe` dans MSYS2 (`pacman -S mingw-w64-ucrt-x86_64-maxima`)
  ou l'installeur Maxima Windows. **Ne PAS utiliser maxima.bat** (injection cmd.exe).
- **GAP :** installeur Windows officiel → `gap.exe`
- **Octave :** `winget install GNU.Octave` → `octave-cli.exe`
- Puis relancer `run_equiv_wine.sh` adapté en natif pour chaque CAS.

##### N5 — CPU limit diagnostic

Pistes à explorer dans l'ordre :
1. Ajouter `GetProcessTimes()` dans le test : le compteur UserTime avance-t-il ?
2. `QueryInformationJobObject()` : le limit est-il bien vu par le kernel ?
3. Recompiler le test CPU avec MSVC (`cl.exe`) au lieu de mingw : layout struct identique ?
4. Tester avec `PerJobUserTimeLimit` au lieu de `PerProcessUserTimeLimit`

---

### 4. Tests évasion sandbox (S1-S9)

Le selftest passe, mais les vrais tests d'évasion manquent. Le Suivi mentionne
`test_sandbox_evasion.ps1` (esquisse) mais il n'est pas dans le repo.

**Action Grok :** écrire et commiter les tests S1-S9 du cahier de recette §8.1 :

```
S1 — Lecture C:\Windows\system32\... via un CAS sandboxé → refusé
S2 — Écriture hors session dir → refusé
S3 — Connexion TCP (ping/curl) → refusé (AppContainer sans NET capability)
S4 — fork bomb (100 enfants) → Job limit atteint
S5 — Injection cmdline ("; calc.exe") → pas d'exécution
S6 — Octave system("cmd /c dir") → filtré
S7 — Octave dos/winqueryreg/winopen → filtré
S8 — Maxima (:lisp (ext:run-program "cmd")) → filtré ou sandboxé
S9 — Écriture registre HKCU → refusé (AppContainer)
```

Commiter les tests comme `tests/platform/test_sandbox_evasion.sh` ou `.ps1`.
Exécuter sur ShadowPC et reporter les résultats.

---

### 5. VPS Debian (rappel — bloqueur pour T-INT, T-OEF)

Le VPS n'est pas encore provisionné. Les actions H1-H3 du handoff 02 restent en attente :
1. Installer WIMS 4.33a avec tous les CAS
2. Générer corpus T11 (200+ OEF, seed=42)
3. Valider `wims-port/` sur le VPS

**Grok peut avancer N2/N1/N11 sur sa box sans le VPS.**

---

### 6. Git — état actuel

```
Repo : Reprise_Wims_Grok/wims-port/
Branche : main (ou master)
HEAD : 4374c68

Phase 3 commits (6) :
b9670a6 Platform: wp_spawn_mx, wp_pipe_open, wp_fd_read
1cd0534 G2: lines.c multiexec() on platform layer
c13459b G1: ch..root.exe (AppContainer + RestrictedToken + session ACL)
f81abf1 G3: strip CR from exec.err
e13ec24 G5: cross-build 7 interfaces; octave illegal+=dos/winqueryreg/winopen
4374c68 G6: wims.exe cross-builds (posix shims, alarm, crypt stub)
```

Binaires de test sur ShadowPC : `Reprise_Wims_Grok/tests-win/` (22/22 pass, CPU fail, mémoire pass, selftest pass).

---

### 7. Contraintes (rappel)

- **ShadowPC** : pas de Docker/WSL2/Hyper-V
- **SVN checkout** (`wims/`) dans `Reprise_Wims_Claude/` : lecture seule, gitignored
- **Git** : ne pas pousser sans demande, commits avec `Co-Authored-By:`
- **Sécurité** : sandbox obligatoire avant ouverture aux élèves, crypt fail-closed correct
- **Licence** : GPL, reverser `#ifdef _WIN32` à wimsdev
- **Fichiers tests-win/** : Grok peut y déposer des binaires de test. Ne rien toucher d'autre sur ShadowPC.
- **Dépôt unique** : `wims-port/` (décision H9)

---

### 8. Fichiers à lire en priorité

1. **Ce document** — instructions Phase 4
2. **`Cahier_Recette_Portage_WIMS_CLAUDE_V1_1.md`** — tests à exécuter (170 tests, 91 faits)
3. **`Cahier_Audit_Portage_WIMS_CLAUDE_V1_1.md`** — verdict Phase 1-3 + lacunes L7-L11
4. **`Suivi_Developpement_Portage.md`** — ton journal (à mettre à jour)
5. **`Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md`** — spec de référence

---

### 9. Réponse attendue

Lance N2 (wimslogd) en premier — c'est le plus gros bloqueur. N1 (crypt) et N11 (fork résiduel)
peuvent suivre dans la foulée car ils touchent des parties différentes du code.

Pour les tests sandbox (S1-S9), tu peux les faire tourner sur ShadowPC en les déposant dans
`tests-win/`. Je les exécuterai si tu préfères.

Continue ?


---

## Partie C — Handoff Claude → Grok, session 02

> Source : `archive/Handoff_Grok_Wims02_CLAUDE_V1.md`

## Handoff — Portage WIMS Windows (session 02)

**Date :** 2026-10-23  
**Projet :** `D:\ALF_ZGit\43-wims-paris-saclay_fr\`  
**Objectif :** Portage natif MSVC du serveur WIMS 4.33a (r19040) sur Windows, sans WSL/Docker.

---

### 1. Contexte

WIMS est un serveur éducatif C (fork/exec, pipes, mmap, sockets Unix, chroot) qui ne tourne que sous Linux. Le projet vise un portage natif Windows (MSVC + Win32 API) pour ShadowPC (pas de Docker/WSL2/Hyper-V — pas de virtualisation imbriquée).

Deux agents collaborent :
- **Claude** (ShadowPC Windows) : architecture, documentation, futur portage MSVC
- **Grok Bot** (VPS Debian) : prototype fonctionnel, couche Platform, tests de non-régression

L'utilisateur fournira un **VPS Debian** pour les tests complets.

---

### 2. Dossiers de travail

```
D:\ALF_ZGit\43-wims-paris-saclay_fr\
├── Reprise_Wims_Claude\              # Travail Claude
│   ├── wims\                         # SVN checkout r19040 (lecture seule, gitignored)
│   ├── .git\                         # Branche feat/wims-windows-migration
│   └── *.md                          # Documents (voir §3)
│
└── Reprise_Wims_Grok\                # Travail Grok
    ├── wims-port\                    # Dépôt git Grok (code fonctionnel)
    └── *.md                          # Documents Grok
```

---

### 3. Documents produits

#### Claude (Reprise_Wims_Claude)

| Fichier | Version | Contenu | Commit |
|---------|---------|---------|--------|
| `Audit_Migration_WIMS_Windows_CLAUDE_V2.md` | V2 | Audit initial Windows : pistes Docker/MSYS2/MSVC | `522131e` |
| `Recherche_Projets_Migration_WIMS_CLAUDE_V1_1.md` | V1_1 | Recherche projets open-source, contrainte ShadowPC, recommandation MSYS2+Laragon | `e9c0759` |
| `Etude_Portage_MSVC_Natif_CLAUDE_V1.md` | V1 | Étude initiale MSVC : 4 projets de référence (PostgreSQL, nginx, OpenSSH, Redis) | `2c089e1` |
| `Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md` | V1_1 | Étude corrigée après Grok Review 1 (9 erreurs + 6 oublis fixés) | `98cae01` |
| `Architecture_Portage_MSVC_WIMS_CLAUDE_V1.md` | V1 | Architecture détaillée : win32compat/, CMake, 7 phases | `5f6c706` |
| **`Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md`** | **V1_3** | **Version courante** : toutes corrections Grok intégrées (24 errata), VPS strategy (§15), CAS migration steps (§16), traçabilité des 2 reviews (§0) | `b33b167` |

#### Grok (Reprise_Wims_Grok)

| Fichier | Contenu |
|---------|---------|
| `Architecture_Portage_MSVC_WIMS_GROK_V2.md` | Cross-review de l'architecture Claude V1 : 40 points, 24 corrections |
| `Architecture_Portage_MSVC_WIMS_V3.md` | Architecture Grok V3 (sa propre version) |
| `Suivi_Developpement_Portage.md` | **Journal de dev Grok** — Phases 1-2 terminées, code fonctionnel |
| `Audit_Migration_WIMS_Windows_Fusion_v2.md` | Audit fusionné Claude+Grok |

---

### 4. État du code Grok (avance significative)

Grok a un prototype **fonctionnel** dans `Reprise_Wims_Grok/wims-port/` :

#### Phase 1 — Terminée ✅

- Import r19040 dans git, compilation de référence Debian
- Couche `src/Platform/` : `wims_platform.h` (API portable), `wp_posix.c`, `wp_win32.c`
- `Interfaces/common.c` refactorisé sur cette API (execredirected, dopipes, kill/alive/wait)
- Tests : suite d'équivalence PARI Linux ↔ refactorisé (tous cas passent)

#### Phase 2 — Terminée ✅

- `wp_win32.c` : stdin CAS via **named pipe overlapped** (écriture non bloquante), CAS dans **Job Object nommé** (CPU/mémoire/KILL_ON_JOB_CLOSE), `wp_kill` = TerminateJobObject
- `common.c` cross-compile pour Windows (mingw-w64), `pari.exe` compile sans warning
- **Tests sous wine : 12/12** équivalence Linux ↔ Windows (PARI), 19/19 tests unitaires plateforme
- **Tests Maxima : 7/7** équivalence (integrate, multiexec, timeout, commande illégale)
- **Bug découvert** : `calc.c:241` — `v1=0` réinitialisé → timeout jamais atteint (boucle infinie si CAS muet). Fix conditionnel `WIMS_MX_FIX_TIMEOUT`.
- Derniers commits : `db52741` (HEAD)

#### Phase 3 — À faire

- `ch..root.exe` (AppContainer + ACL NTFS) — isolation WRAPPER toujours refusée (`WP_E_NOISOLATION`)
- Port de `lines.c multiexec()` (fork → `wp_spawn`, `mxtab` en `wp_chan`)
- Suppression des `\r` de `exec.err` côté wims
- Vérification limites CPU du Job sur vrai Windows (wine ne les applique pas)
- Autres interfaces : maxima.exe Windows, octave, gap
- wims.cgi compilable sous Windows

---

### 5. État de l'architecture Claude

#### Document de référence : `Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md`

**Sections clés :**

| Section | Contenu |
|---------|---------|
| §0 | Traçabilité complète des 2 reviews Grok (15 + 24 points, tous résolus) |
| §1 | Architecture cible Linux→Windows (schéma, décisions) |
| §2 | Cartographie : 14 sites fork, 7 select(), 30+ call_sh |
| §3 | Module `win32compat/` : spawn.c (STARTUPINFOEX + HANDLE_LIST), pipe.c (overlapped bidirectionnel), signal.c (timer safe), mmap.c (Global\WimsShm), sandbox.c + chrootexe/ (jeton restreint), service.c, fmode.c, loadavg.c, cmdline.c |
| §4 | CMakeLists.txt complet (winflexbison, 2 OefMsg2wims, ~20 Misc subdirs) |
| §5-8 | Phases 2-5 avec fichiers/efforts révisés |
| §9 | Scripts : garder sh (MSYS2 embarqué), pas de réécriture PowerShell V1 |
| §10 | Tests T1-T14 (non-régression, sécurité, charge 30 users) |
| §11 | Chiffrage : 6,5-7,5 mois (1 dev), 4-5 mois (2 devs) |
| §15 | **Stratégie VPS** : Grok = oracle Linux, Claude = portage MSVC, diff T11 |
| §16 | **Migration CAS en 6 étapes** : C1 inventaire → C2 spawn → C3 dopipes → C4 timeout → C5 sécurité → C6 validation croisée |

#### Décisions d'architecture clés (validées par les 2 reviews)

| Décision | Choix | Raison |
|----------|-------|--------|
| wimsd | Supprimé → Apache CGI | Inutile, fork-per-request |
| fork+exec | `win32_spawn()` unique (STARTUPINFOEX) | Pas de fuite de handles |
| Pipes CAS | Overlapped bidirectionnel | Pas de deadlock |
| IPC wimslogd | AF_UNIX Winsock (Win10 1803+) | Préserve select() |
| Shared memory | `Global\WimsShm` + DACL | Cross-session (service ↔ Apache) |
| SIGALRM | Timer → flag + TerminateJobObject | Pas de longjmp inter-thread |
| Sandbox | `ch..root.exe` : jeton restreint + Job + ACL | Job seul insuffisant |
| Shell | sh.exe MSYS2 + coreutils | 30+ commandes bash |
| .bat | Interdits → binaire direct | Injection cmd.exe |
| Encodage | `/utf-8` + `_O_BINARY` + manifeste | CRLF/fseek |

---

### 6. Convergence Claude ↔ Grok

| Aspect | Claude | Grok | Convergent ? |
|--------|--------|------|-------------|
| API processus | `win32_spawn()` 3 modes | `wp_spawn()` + `wp_chan` | ✅ Même pattern (STARTUPINFOEX + Job Object) |
| Pipes overlapped | §3.2.1-3.2.2 (code détaillé) | `wp_win32.c` (code fonctionnel sous wine) | ✅ Grok a le code testé |
| Sandbox | `ch..root.exe` spécifié (§3.3) | `WP_E_NOISOLATION` (pas encore implémenté) | ⚠️ À faire en Phase 3 |
| dopipes | `win32_dopipes()` bidirectionnel (§3.2.2) | Intégré dans `wp_exchange()` | ✅ Même approche |
| Timeout | Timer + flag (§3.5) | Job Object PerProcessUserTimeLimit | ✅ Complémentaires |
| multiexec | Documenté dans §2 | Porté (`wp_mx_exchange`) + bug calc.c:241 trouvé | ✅ Grok avance |
| Noms exe CAS | Binaire direct, pas .bat (§16.1) | gp.exe MSYS2 testé | ✅ Aligné |
| Scripts | Garder sh.exe (§9) | Non abordé | — |
| Tests | T1-T14 spécifiés (§10) | 12/12 + 19/19 + 7/7 fonctionnels | ✅ Grok a la base de tests |

**Recommandation pour la session suivante :** s'appuyer sur le code Grok (`wims-port/`) comme base de travail, l'architecture Claude V1_3 comme spécification de référence.

---

### 7. Git — branche et commits Claude

```
Repo : D:\ALF_ZGit\43-wims-paris-saclay_fr\Reprise_Wims_Claude\
Branche : feat/wims-windows-migration (10 commits)

b33b167 Add full traceability of both Grok cross-reviews (V1_3)
36a4e29 Add VPS testing strategy and detailed CAS migration steps (V1_2)
70bf308 Fix architecture document after Grok V2 cross-review (24 corrections)
5f6c706 Add detailed architecture document for native MSVC port of WIMS
98cae01 Fix 9 errors in MSVC porting study after cross-review with sources
2c089e1 Add detailed native MSVC porting study with open-source precedents
e9c0759 Update migration research for ShadowPC (no Docker/WSL2/Hyper-V)
2ef81b3 Add open-source project research for WIMS Windows migration
522131e Consolidated WIMS Windows migration audit (Claude + Grok)
a299db2 Add WIMS 4.32 Windows migration audit

.gitignore exclut : wims/ (SVN checkout), * - Copie* (fichiers Windows)
```

#### Git Grok

```
Repo : Reprise_Wims_Grok/wims-port/
HEAD : db52741 (Tests: maxima interface equivalence)

Commits principaux :
5afd05e Import WIMS trunk r19040
debb65b Platform: wims_platform API (POSIX + Win32 skeleton)
24f38f5 Interfaces: common.c through wims_platform
0a5207a Tests: platform unit tests + pari equivalence
68377ef Platform Win32: overlapped stdin, Job Objects, named per session
3ae53c2 multiexec on platform layer + calc.c:241 bug doc
eeac3ee Tests: Linux<->Windows(wine) equivalence suite
db52741 Tests: maxima interface equivalence
```

---

### 8. Contraintes à respecter

- **ShadowPC** : pas de Docker/WSL2/Hyper-V (pas de virtualisation imbriquée)
- **SVN checkout** (`wims/`) : lecture seule, ne pas modifier, gitignored
- **Git** : ne pas pousser sans demande explicite, toujours créer une branche, commits avec `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>`
- **Sécurité** : pas de bind 0.0.0.0, sandbox CAS obligatoire avant ouverture aux élèves
- **Fichiers** : versioning `CLAUDE_V<x>` dans nom + contenu, garder les anciennes versions
- **Licence** : WIMS est GPL, prévoir de reverser les `#ifdef _WIN32` à wimsdev

---

### 9. Prochaines étapes

#### Priorité 1 — VPS Debian (utilisateur fournit le serveur)

1. Installer WIMS 4.33a nativement avec tous les CAS
2. Générer le corpus T11 (200+ modules OEF → HTML de référence, seed=42)
3. Valider le code Grok (`wims-port/`) sur le VPS (pas seulement la box)

#### Priorité 2 — Phase 3 Grok (code)

4. `ch..root.exe` : AppContainer + ACL NTFS (sandbox réel)
5. Port `lines.c multiexec()` → `wp_spawn`
6. Vérifier limites CPU Job Object sur vrai Windows (pas wine)
7. Interfaces : maxima.exe Windows, octave, gap

#### Priorité 3 — Convergence Claude ↔ Grok

8. Aligner l'architecture Claude V1_3 sur les choix d'implémentation Grok (`src/Platform/`)
9. Décider : utiliser `wims-port/` comme base unique ou maintenir 2 dépôts
10. Intégrer le bug `calc.c:241` dans le suivi

#### Priorité 4 — Portage Windows réel

11. Compiler `pari.exe` sur ShadowPC (MSVC, pas mingw)
12. Tester avec gp.exe natif Windows (pas wine)
13. Avancer vers wims.exe CGI compilable MSVC

---

### 10. Fichiers à lire en priorité

Pour reprendre le travail :

1. **`Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md`** — L'architecture de référence (spécification)
2. **`Reprise_Wims_Grok/Suivi_Developpement_Portage.md`** — Le journal de dev Grok (état réel du code)
3. **`Reprise_Wims_Grok/Architecture_Portage_MSVC_WIMS_V3.md`** — L'architecture Grok (sa propre vision)
4. **`Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md`** — L'étude MSVC corrigée (contexte technique)
