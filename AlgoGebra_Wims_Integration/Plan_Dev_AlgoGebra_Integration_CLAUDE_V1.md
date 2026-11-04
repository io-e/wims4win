<!-- CLAUDE_V1 -->
# Planning de développement — Intégration AlgoGebra × WIMS (wims4win)

| Champ | Valeur |
|---|---|
| Document | `AlgoGebra_Wims_Integration/Plan_Dev_AlgoGebra_Integration_CLAUDE_V1.md` |
| Version | CLAUDE_V1 |
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
| #1 | wimslogd complet (scores, housekeeping) | Faible — AlgoGebra n'utilise que cmd=reply, pas les scores persistants |
| #3 | Livrer public_html et modules pour recette | **Bloquant** pour tests end-to-end |
| #6 | Recette 200 modules + charge | Non bloquant pour le hook, utile pour la validation |
| #7 | Diagnostic Job CPU user-time=0 | Non bloquant (watchdog mural en filet) |
| #8 | Durcissement sandbox AppContainer | Non bloquant |
| #9 | Remonter bogue calc.c:241 à wimsdev | Non bloquant |

### Ce qui manque pour AlgoGebra

Le handoff (`Handoff_AlgoGebra_JSON_Hook_CLAUDE_V1.md`) définit une seule modification côté WIMS : un hook JSON dans la réponse `cmd=reply` quand `special_parm=algogebra_json`. Tout le reste (HttpWimsClient, UI feedback, mode connecté) est côté AlgoGebra.

---

## 1. Tracks de développement

### Track A — Discovery : variables OEF réelles (prérequis)

**Objectif :** confirmer les noms exacts des variables per-answer après un `cmd=reply`, point de départ de toute implémentation.

| Étape | Description | Livrable | Durée est. |
|---|---|---|---|
| A1 | Lancer `oef2html.py` avec `OEF2HTML_DUMP` sur 00076.oef (numeric) | Dump HTML + session vars | 0.5 j |
| A2 | Inspecter `sessions/<sid>/var` : identifier `sc_reply<N>`, scores per-answer | Variable map vérifiée | 0.5 j |
| A3 | Lire `reply.phtml` et `anscomp.phtml` dans le runtime tree | Point d'injection confirmé | 0.5 j |
| A4 | Répéter sur 00006.oef (menu/choice) pour couvrir les types choice | Complément variable map | 0.5 j |

**Durée totale : ~2 jours**
**Agent :** Grok (accès runtime Debian + Windows)
**Sortie :** `AlgoGebra_Wims_Integration/Discovery_Variable_Dump_CLAUDE_V1.md`

---

### Track B — Hook JSON : implémentation

**Objectif :** émettre le bloc `<!-- ALGOGEBRA_JSON_BEGIN ... ALGOGEBRA_JSON_END -->` dans la réponse `cmd=reply`.

Deux options exclusives (voir Handoff §3.2) :

| | Option A : phtml | Option B : C |
|---|---|---|
| Fichier | `scripts/oef/algogebra_json.phtml` | `src/algogebra.c` |
| Injection | `reply.phtml` (conditional block) | `wims.c` après `main_phtml_put()` |
| Avantage | Accès direct aux variables OEF | Pas de modification des templates upstream |
| Risque | Touche un fichier upstream | Couplage C ↔ nommage OEF |

**Recommandation :** Option A (phtml) sauf si la discovery (Track A) révèle un obstacle.

| Étape | Description | Prérequis | Durée est. |
|---|---|---|---|
| B1 | Créer `algogebra_json.phtml` (itère sur answers, émet JSON) | A1–A2 terminés | 1 j |
| B2 | Injecter le conditional dans `reply.phtml` | A3 terminé | 0.5 j |
| B3 | Intégrer dans `build-runtime-tree.sh` (copie du fichier phtml) | B1 | 0.5 j |
| B4 | Test unitaire : correct answers → grade=1.0, JSON valide | B1+B2 | 0.5 j |
| B5 | Test unitaire : wrong answers → grade<1.0, expected/given corrects | B4 | 0.5 j |
| B6 | Test unitaire : mixed types (menu/choice) | A4 + B4 | 0.5 j |
| B7 | Test de régression : sans `special_parm` → pas de JSON, HTML identique | B4 | 0.5 j |
| B8 | Test parity : `compare_html.py` avec/sans hook | B7 | 0.5 j |

**Durée totale : ~4–5 jours**
**Agent :** Grok
**Sortie :** patch + tests automatisés (extension oef2html.py)

---

### Track C — Validation croisée et encoding

| Étape | Description | Prérequis | Durée est. |
|---|---|---|---|
| C1 | Vérifier l'encoding du JSON (windows-1252 → ASCII + \uNNNN) | B4 | 0.5 j |
| C2 | Tester avec un exercice contenant des accents (feedback français) | C1 | 0.5 j |
| C3 | Valider le JSON schema contre `Spec_WIMS_Message_Integration` §3.2 | B4 | 0.5 j |
| C4 | Vérifier sur Debian (runtime Linux) ET Windows (runtime wims4win) | B8 | 1 j |

**Durée totale : ~2–3 jours**
**Agent :** Grok + Claude (review schema)

---

### Track D — Issue #3 : runtime tree livrable pour recette

Cette track est un prérequis pour les tests end-to-end AlgoGebra mais pas pour le hook lui-même.

| Étape | Description | Durée est. |
|---|---|---|
| D1 | Documenter la procédure complète de construction du runtime tree (issue #3) | 1 j |
| D2 | Vérifier que `build-runtime-tree.sh` inclut le hook phtml (après B3) | 0.5 j |
| D3 | Installer ≥20 modules OEF représentatifs dans le runtime de test | 1 j |
| D4 | Smoke test Laragon/Apache → accueil + cmd=new + cmd=reply sur 5 exercices | 1 j |

**Durée totale : ~3–4 jours**

---

### Track E — Tests end-to-end (Phase 2 AlgoGebra)

**Prérequis :** Track B terminée + HttpWimsClient côté AlgoGebra (hors périmètre wims4win).

| Étape | Description | Durée est. |
|---|---|---|
| E1 | AlgoGebra desktop → cmd=new → exercice affiché | 1 j |
| E2 | Réponses élève → cmd=reply + special_parm=algogebra_json → JSON extrait | 1 j |
| E3 | Feedback per-answer affiché dans AlgoGebra | 1 j |
| E4 | Test sur Debian (Grok) ET Windows (runtime wims4win) | 1 j |
| E5 | Test avec ≥5 types d'exercices (numeric, function, menu, set, algebraic) | 2 j |

**Durée totale : ~6 jours**
**Agents :** Grok (serveur WIMS) + Codex (client AlgoGebra)

---

## 2. Dépendances et chemin critique

```
Track A (discovery)
  │
  ├──→ Track B (hook impl.)
  │      │
  │      ├──→ Track C (validation encoding + schema)
  │      │
  │      └──→ Track D (runtime livrable) ──→ Track E (end-to-end)
  │                                              ↑
  │                                    HttpWimsClient (AlgoGebra, hors périmètre)
  │
  └──(indépendant)── Issues #1, #6–#9 (portage, non bloquantes)
```

**Chemin critique :** A → B → C + D → E

---

## 3. Calendrier prévisionnel

| Semaine | Dates | Tracks | Jalons |
|---|---|---|---|
| S1 | 04–08 nov. 2026 | **A** (discovery) | Variable map vérifiée, point d'injection confirmé |
| S2 | 11–15 nov. 2026 | **B** (hook impl. B1–B3) | `algogebra_json.phtml` fonctionnel, injecté dans reply.phtml |
| S3 | 18–22 nov. 2026 | **B** (tests B4–B8) + **C** | Tests unitaires passent, encoding validé, schema conforme |
| S4 | 25–29 nov. 2026 | **D** (runtime livrable) | Runtime Windows prêt avec hook + modules de test |
| S5+ | déc. 2026 | **E** (end-to-end) | Intégration AlgoGebra ↔ WIMS validée (dépend d'HttpWimsClient) |

**Estimation totale côté wims4win : ~4 semaines (S1–S4).**
Le Track E dépend de l'avancement côté AlgoGebra (HttpWimsClient).

---

## 4. Risques

| # | Risque | Impact | Mitigation |
|---|---|---|---|
| R1 | Les variables per-answer n'existent pas sous la forme attendue | Bloquant B | Discovery (Track A) en premier ; fallback Option B (C-side) |
| R2 | Le moteur phtml ne supporte pas les structures de contrôle nécessaires | Bloquant B (Option A) | Basculer sur Option B (hook en C) |
| R3 | Encoding windows-1252 corrompt le JSON | Bloquant C | Forcer ASCII + \uNNNN escapes ; ou reconfigurer WIMS en UTF-8 |
| R4 | reply.phtml diffère entre 4.31b et 4.33a | Retravail B2 | Vérifier la version exacte du runtime |
| R5 | HttpWimsClient retardé côté AlgoGebra | Retarde E | Les tests B4–B8 valident le hook indépendamment du client |

---

## 5. Livrables

| # | Fichier | Track | Description |
|---|---|---|---|
| 1 | `Discovery_Variable_Dump_CLAUDE_V1.md` | A | Dump vérifié des variables per-answer |
| 2 | `algogebra_json.phtml` (ou `algogebra.c`) | B | Implémentation du hook |
| 3 | `reply.phtml.patch` (ou `wims.c.patch`) | B | Patch minimal pour l'injection |
| 4 | `test_algogebra_json.py` | B | Tests automatisés (extension oef2html) |
| 5 | Rapport de validation encoding + schema | C | Preuve de conformité |
| 6 | Runtime tree mis à jour avec hook | D | build-runtime-tree.sh patché |
| 7 | Rapport end-to-end | E | Preuve d'intégration AlgoGebra ↔ WIMS |

---

## 6. Affectation des agents

| Agent | Tracks | Responsabilité |
|---|---|---|
| **Grok** | A, B, C, D | Implémentation, tests, runtime — accès Debian + Windows |
| **Claude** | C (review) | Validation JSON schema vs spec |
| **Codex** | E | Client AlgoGebra (HttpWimsClient) |

---

*Fin de `Plan_Dev_AlgoGebra_Integration_CLAUDE_V1.md`.*
