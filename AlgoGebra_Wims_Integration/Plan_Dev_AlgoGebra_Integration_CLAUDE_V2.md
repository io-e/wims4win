<!-- CLAUDE_V2 -->
# Planning de développement — Intégration AlgoGebra × WIMS (wims4win)

| Champ | Valeur |
|---|---|
| Document | `AlgoGebra_Wims_Integration/Plan_Dev_AlgoGebra_Integration_CLAUDE_V2.md` |
| Version | CLAUDE_V2 |
| Date | 2026-11-04 |
| Repo | `io-e/wims4win` (public) |
| HEAD de référence | `6ea2c5e` |
| Dépendances | `Handoff_AlgoGebra_JSON_Hook_CLAUDE_V1.md`, `OEF_Variable_Map_CLAUDE_V1.md` |
| Spec consommateur | `Spec_WIMS_Message_Integration_CLAUDE_V1.md` (côté AlgoGebra) |

---

## 0. État des lieux (2026-11-04)

### Portage wims4win — ce qui est fait (phases 1–4)

| Composant | Statut | Commit |
|---|---|---|
| Couche Platform (spawn, IPC, Job Objects) | ✅ | Phases 1–2 |
| CAS interfaces (pari, maxima, gap, octave) | ✅ 24/24 sur PC | `51b54d5` |
| wims.exe CGI compilable + smoke test | ✅ | `4374c68` |
| wimslogd minimal (ping/loadavg, named pipe) | ✅ | `44bde53` |
| crypt DES + BCrypt | ✅ | `920cb53` |
| MathML helper | ✅ | `4b2d4eb` |
| AppContainer sandbox (ch..root.exe) | ✅ selftest OK | `c13459b` |
| Limite CPU Job + watchdog | ✅ | `5f5523b` |
| flydraw.exe + insdraw | ✅ | `1845b46` |
| Runtime tree builder | ✅ | `b29becd` |
| oef2html parity (Linux = Windows) | ✅ | `45a7e96` |

### Issues ouvertes (portage)

| # | Titre | Impact sur AlgoGebra |
|---|---|---|
| #1 | wimslogd complet (scores, housekeeping) | Faible — AlgoGebra n'utilise que cmd=reply |
| #3 | Livrer public_html et modules pour recette | **Bloquant** pour tests end-to-end |
| #6 | Recette 200 modules + charge | Non bloquant pour le hook |
| #7 | Diagnostic Job CPU user-time=0 | Non bloquant (watchdog en filet) |
| #8 | Durcissement sandbox AppContainer | Non bloquant |
| #9 | Remonter bogue calc.c:241 à wimsdev | Non bloquant |

### Ce qui manque pour AlgoGebra

Une seule modification côté WIMS : un hook JSON dans `cmd=reply` quand `special_parm=algogebra_json`. Tout le reste (HttpWimsClient, UI feedback, mode connecté) est côté AlgoGebra.

---

## 1. Répartition des agents

| Agent | Périmètre | Plateforme |
|---|---|---|
| **Claude** | Discovery Windows, hook impl., tests Windows, runtime Windows, review schema | Windows (PC wims4win) |
| **GBot** | Vérification Debian, parity Linux, tests end-to-end côté serveur Debian | Debian (box) — **différé**, sauf blocage ou demande de Claude |
| **Codex** | Client AlgoGebra (HttpWimsClient) | hors périmètre wims4win |

**Principe :** Claude fait tout le travail Windows en premier. GBot n'intervient que si Claude rencontre un blocage nécessitant le runtime Debian, ou pour la validation croisée Linux une fois le hook stabilisé sur Windows.

---

## 2. Tracks de développement

### Track A — Discovery : variables OEF réelles

**Objectif :** confirmer les noms exacts des variables per-answer après un `cmd=reply` sur le runtime Windows.

| # | Étape | Agent | Charge |
|---|---|---|---|
| A1 | `oef2html.py` + `OEF2HTML_DUMP` sur 00076.oef (numeric), runtime `wims-runtime-oef` | Claude | 0.5 j/h |
| A2 | Inspecter `sessions/<sid>/var` : identifier `sc_reply<N>`, scores per-answer | Claude | 0.5 j/h |
| A3 | Lire `reply.phtml` et `anscomp.phtml` dans le runtime tree Windows | Claude | 0.5 j/h |
| A4 | Répéter sur 00006.oef (menu/choice) | Claude | 0.5 j/h |

| | j/h |
|---|---|
| **Claude** | **2** |
| **GBot** | 0 |

**Sortie :** `Discovery_Variable_Dump_CLAUDE_V1.md`

---

### Track B — Hook JSON : implémentation

**Objectif :** émettre `<!-- ALGOGEBRA_JSON_BEGIN ... ALGOGEBRA_JSON_END -->` dans la réponse `cmd=reply`.

Deux options (voir Handoff §3.2) — recommandation : **Option A (phtml)** sauf si Track A révèle un obstacle.

| | Option A : phtml | Option B : C |
|---|---|---|
| Fichier | `scripts/oef/algogebra_json.phtml` | `src/algogebra.c` |
| Injection | `reply.phtml` (conditional block) | `wims.c` après `main_phtml_put()` |
| Avantage | Accès direct aux variables OEF | Ne touche pas les templates upstream |
| Risque | Touche un fichier upstream | Couplage C ↔ nommage OEF |

| # | Étape | Agent | Prérequis | Charge |
|---|---|---|---|---|
| B1 | Créer `algogebra_json.phtml` (itère sur answers, émet JSON) | Claude | A1–A2 | 1 j/h |
| B2 | Injecter le conditional dans `reply.phtml` | Claude | A3 | 0.5 j/h |
| B3 | Intégrer dans `build-runtime-tree.sh` (copie du phtml) | Claude | B1 | 0.5 j/h |
| B4 | Test : correct answers → grade=1.0, JSON valide | Claude | B1+B2 | 0.5 j/h |
| B5 | Test : wrong answers → grade<1.0, expected/given corrects | Claude | B4 | 0.5 j/h |
| B6 | Test : mixed types (menu/choice) | Claude | A4+B4 | 0.5 j/h |
| B7 | Test régression : sans `special_parm` → pas de JSON, HTML identique | Claude | B4 | 0.5 j/h |
| B8 | Test parity : `compare_html.py` avec/sans hook | Claude | B7 | 0.5 j/h |

| | j/h |
|---|---|
| **Claude** | **4.5** |
| **GBot** | 0 |

**Sortie :** `algogebra_json.phtml` + `reply.phtml.patch` + `test_algogebra_json.py`

---

### Track C — Validation encoding et schema

| # | Étape | Agent | Prérequis | Charge |
|---|---|---|---|---|
| C1 | Vérifier encoding JSON (windows-1252 → ASCII + `\uNNNN`) | Claude | B4 | 0.5 j/h |
| C2 | Tester avec exercice à accents (feedback français) | Claude | C1 | 0.5 j/h |
| C3 | Valider JSON schema contre `Spec_WIMS_Message_Integration` §3.2 | Claude | B4 | 0.5 j/h |
| C4 | Vérification croisée Debian (même hook, runtime Linux) | GBot | B8 | 1 j/h |

| | j/h |
|---|---|
| **Claude** | **1.5** |
| **GBot** | **1** (différé) |

---

### Track D — Runtime tree livrable (issue #3)

Prérequis pour les tests end-to-end, pas pour le hook lui-même.

| # | Étape | Agent | Prérequis | Charge |
|---|---|---|---|---|
| D1 | Documenter la procédure complète de construction du runtime tree | Claude | — | 1 j/h |
| D2 | Vérifier que `build-runtime-tree.sh` inclut le hook phtml | Claude | B3 | 0.5 j/h |
| D3 | Installer ≥20 modules OEF représentatifs dans le runtime Windows | Claude | D1 | 1 j/h |
| D4 | Smoke test Laragon/Apache → accueil + cmd=new + cmd=reply sur 5 exercices | Claude | D3 | 1 j/h |

| | j/h |
|---|---|
| **Claude** | **3.5** |
| **GBot** | 0 |

---

### Track E — Tests end-to-end (Phase 2 AlgoGebra)

**Prérequis :** Track B terminée + HttpWimsClient côté AlgoGebra (Codex).

| # | Étape | Agent | Charge |
|---|---|---|---|
| E1 | AlgoGebra desktop → cmd=new → exercice affiché (Windows) | Claude | 1 j/h |
| E2 | cmd=reply + special_parm=algogebra_json → JSON extrait (Windows) | Claude | 1 j/h |
| E3 | Feedback per-answer affiché dans AlgoGebra | Claude | 0.5 j/h |
| E4 | Test ≥5 types d'exercices (numeric, function, menu, set, algebraic) | Claude | 1.5 j/h |
| E5 | Même parcours sur Debian | GBot | 2 j/h |

| | j/h |
|---|---|
| **Claude** | **4** |
| **GBot** | **2** (différé) |

---

## 3. Récapitulatif des charges

| Track | Description | Claude (j/h) | GBot (j/h) | Total |
|---|---|---|---|---|
| A | Discovery variables OEF | 2 | — | 2 |
| B | Hook JSON impl. + tests | 4.5 | — | 4.5 |
| C | Validation encoding/schema | 1.5 | 1 | 2.5 |
| D | Runtime tree livrable | 3.5 | — | 3.5 |
| E | End-to-end AlgoGebra | 4 | 2 | 6 |
| | **Total** | **15.5 j/h** | **3 j/h** | **18.5 j/h** |

GBot : 3 j/h, **différés**, déclenchés par Claude quand le hook Windows est stabilisé.

---

## 4. Dépendances et chemin critique

```
Track A (discovery, 2 j/h)
  │
  └──→ Track B (hook impl., 4.5 j/h)
         │
         ├──→ Track C (validation, 1.5 j/h Claude + 1 j/h GBot différé)
         │
         └──→ Track D (runtime, 3.5 j/h) ──→ Track E (e2e, 4+2 j/h)
                                                  ↑
                                        HttpWimsClient (Codex, hors périmètre)
```

**Chemin critique Claude :** A (2) → B (4.5) → D (3.5) → E (4) = **14 j/h**

---

## 5. Risques

| # | Risque | Impact | Mitigation |
|---|---|---|---|
| R1 | Variables per-answer absentes sous la forme attendue | Bloquant B | Discovery (A) en premier ; fallback Option B (C-side) |
| R2 | Moteur phtml ne supporte pas les boucles nécessaires | Bloquant B (Option A) | Basculer sur Option B (hook en C) |
| R3 | Encoding windows-1252 corrompt le JSON | Bloquant C | ASCII + `\uNNNN` ; ou reconfigurer WIMS en UTF-8 |
| R4 | reply.phtml diffère entre 4.31b et 4.33a | Retravail B2 | Vérifier version exacte du runtime |
| R5 | HttpWimsClient retardé côté AlgoGebra | Retarde E | Tests B4–B8 valident le hook indépendamment |
| R6 | Blocage Windows nécessitant GBot | Retarde B | Escalade immédiate vers GBot sur la box Debian |

---

## 6. Livrables

| # | Fichier | Track | Agent |
|---|---|---|---|
| 1 | `Discovery_Variable_Dump_CLAUDE_V1.md` | A | Claude |
| 2 | `algogebra_json.phtml` (ou `algogebra.c`) | B | Claude |
| 3 | `reply.phtml.patch` (ou `wims.c.patch`) | B | Claude |
| 4 | `test_algogebra_json.py` | B | Claude |
| 5 | Rapport validation encoding + schema | C | Claude + GBot |
| 6 | Runtime tree mis à jour avec hook | D | Claude |
| 7 | Rapport end-to-end | E | Claude + GBot |

---

*Fin de `Plan_Dev_AlgoGebra_Integration_CLAUDE_V2.md`.*
