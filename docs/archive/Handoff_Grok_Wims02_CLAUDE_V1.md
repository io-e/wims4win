# CLAUDE_V1
# Handoff — Portage WIMS Windows (session 02)

**Date :** 2026-10-23  
**Projet :** `D:\ALF_ZGit\43-wims-paris-saclay_fr\`  
**Objectif :** Portage natif MSVC du serveur WIMS 4.33a (r19040) sur Windows, sans WSL/Docker.

---

## 1. Contexte

WIMS est un serveur éducatif C (fork/exec, pipes, mmap, sockets Unix, chroot) qui ne tourne que sous Linux. Le projet vise un portage natif Windows (MSVC + Win32 API) pour ShadowPC (pas de Docker/WSL2/Hyper-V — pas de virtualisation imbriquée).

Deux agents collaborent :
- **Claude** (ShadowPC Windows) : architecture, documentation, futur portage MSVC
- **Grok Bot** (VPS Debian) : prototype fonctionnel, couche Platform, tests de non-régression

L'utilisateur fournira un **VPS Debian** pour les tests complets.

---

## 2. Dossiers de travail

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

## 3. Documents produits

### Claude (Reprise_Wims_Claude)

| Fichier | Version | Contenu | Commit |
|---------|---------|---------|--------|
| `Audit_Migration_WIMS_Windows_CLAUDE_V2.md` | V2 | Audit initial Windows : pistes Docker/MSYS2/MSVC | `522131e` |
| `Recherche_Projets_Migration_WIMS_CLAUDE_V1_1.md` | V1_1 | Recherche projets open-source, contrainte ShadowPC, recommandation MSYS2+Laragon | `e9c0759` |
| `Etude_Portage_MSVC_Natif_CLAUDE_V1.md` | V1 | Étude initiale MSVC : 4 projets de référence (PostgreSQL, nginx, OpenSSH, Redis) | `2c089e1` |
| `Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md` | V1_1 | Étude corrigée après Grok Review 1 (9 erreurs + 6 oublis fixés) | `98cae01` |
| `Architecture_Portage_MSVC_WIMS_CLAUDE_V1.md` | V1 | Architecture détaillée : win32compat/, CMake, 7 phases | `5f6c706` |
| **`Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md`** | **V1_3** | **Version courante** : toutes corrections Grok intégrées (24 errata), VPS strategy (§15), CAS migration steps (§16), traçabilité des 2 reviews (§0) | `b33b167` |

### Grok (Reprise_Wims_Grok)

| Fichier | Contenu |
|---------|---------|
| `Architecture_Portage_MSVC_WIMS_GROK_V2.md` | Cross-review de l'architecture Claude V1 : 40 points, 24 corrections |
| `Architecture_Portage_MSVC_WIMS_V3.md` | Architecture Grok V3 (sa propre version) |
| `Suivi_Developpement_Portage.md` | **Journal de dev Grok** — Phases 1-2 terminées, code fonctionnel |
| `Audit_Migration_WIMS_Windows_Fusion_v2.md` | Audit fusionné Claude+Grok |

---

## 4. État du code Grok (avance significative)

Grok a un prototype **fonctionnel** dans `Reprise_Wims_Grok/wims-port/` :

### Phase 1 — Terminée ✅

- Import r19040 dans git, compilation de référence Debian
- Couche `src/Platform/` : `wims_platform.h` (API portable), `wp_posix.c`, `wp_win32.c`
- `Interfaces/common.c` refactorisé sur cette API (execredirected, dopipes, kill/alive/wait)
- Tests : suite d'équivalence PARI Linux ↔ refactorisé (tous cas passent)

### Phase 2 — Terminée ✅

- `wp_win32.c` : stdin CAS via **named pipe overlapped** (écriture non bloquante), CAS dans **Job Object nommé** (CPU/mémoire/KILL_ON_JOB_CLOSE), `wp_kill` = TerminateJobObject
- `common.c` cross-compile pour Windows (mingw-w64), `pari.exe` compile sans warning
- **Tests sous wine : 12/12** équivalence Linux ↔ Windows (PARI), 19/19 tests unitaires plateforme
- **Tests Maxima : 7/7** équivalence (integrate, multiexec, timeout, commande illégale)
- **Bug découvert** : `calc.c:241` — `v1=0` réinitialisé → timeout jamais atteint (boucle infinie si CAS muet). Fix conditionnel `WIMS_MX_FIX_TIMEOUT`.
- Derniers commits : `db52741` (HEAD)

### Phase 3 — À faire

- `ch..root.exe` (AppContainer + ACL NTFS) — isolation WRAPPER toujours refusée (`WP_E_NOISOLATION`)
- Port de `lines.c multiexec()` (fork → `wp_spawn`, `mxtab` en `wp_chan`)
- Suppression des `\r` de `exec.err` côté wims
- Vérification limites CPU du Job sur vrai Windows (wine ne les applique pas)
- Autres interfaces : maxima.exe Windows, octave, gap
- wims.cgi compilable sous Windows

---

## 5. État de l'architecture Claude

### Document de référence : `Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md`

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

### Décisions d'architecture clés (validées par les 2 reviews)

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

## 6. Convergence Claude ↔ Grok

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

## 7. Git — branche et commits Claude

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

### Git Grok

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

## 8. Contraintes à respecter

- **ShadowPC** : pas de Docker/WSL2/Hyper-V (pas de virtualisation imbriquée)
- **SVN checkout** (`wims/`) : lecture seule, ne pas modifier, gitignored
- **Git** : ne pas pousser sans demande explicite, toujours créer une branche, commits avec `Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>`
- **Sécurité** : pas de bind 0.0.0.0, sandbox CAS obligatoire avant ouverture aux élèves
- **Fichiers** : versioning `CLAUDE_V<x>` dans nom + contenu, garder les anciennes versions
- **Licence** : WIMS est GPL, prévoir de reverser les `#ifdef _WIN32` à wimsdev

---

## 9. Prochaines étapes

### Priorité 1 — VPS Debian (utilisateur fournit le serveur)

1. Installer WIMS 4.33a nativement avec tous les CAS
2. Générer le corpus T11 (200+ modules OEF → HTML de référence, seed=42)
3. Valider le code Grok (`wims-port/`) sur le VPS (pas seulement la box)

### Priorité 2 — Phase 3 Grok (code)

4. `ch..root.exe` : AppContainer + ACL NTFS (sandbox réel)
5. Port `lines.c multiexec()` → `wp_spawn`
6. Vérifier limites CPU Job Object sur vrai Windows (pas wine)
7. Interfaces : maxima.exe Windows, octave, gap

### Priorité 3 — Convergence Claude ↔ Grok

8. Aligner l'architecture Claude V1_3 sur les choix d'implémentation Grok (`src/Platform/`)
9. Décider : utiliser `wims-port/` comme base unique ou maintenir 2 dépôts
10. Intégrer le bug `calc.c:241` dans le suivi

### Priorité 4 — Portage Windows réel

11. Compiler `pari.exe` sur ShadowPC (MSVC, pas mingw)
12. Tester avec gp.exe natif Windows (pas wine)
13. Avancer vers wims.exe CGI compilable MSVC

---

## 10. Fichiers à lire en priorité

Pour reprendre le travail :

1. **`Architecture_Portage_MSVC_WIMS_CLAUDE_V1_3.md`** — L'architecture de référence (spécification)
2. **`Reprise_Wims_Grok/Suivi_Developpement_Portage.md`** — Le journal de dev Grok (état réel du code)
3. **`Reprise_Wims_Grok/Architecture_Portage_MSVC_WIMS_V3.md`** — L'architecture Grok (sa propre vision)
4. **`Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md`** — L'étude MSVC corrigée (contexte technique)
