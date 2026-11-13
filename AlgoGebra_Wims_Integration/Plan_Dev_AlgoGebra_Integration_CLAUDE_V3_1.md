<!-- CLAUDE_V3_1 -->
# Planning de développement — Intégration AlgoGebra × WIMS (wims4win) — re-baseline + décisions du 13/11

| Champ | Valeur |
|---|---|
| Document | `AlgoGebra_Wims_Integration/Plan_Dev_AlgoGebra_Integration_CLAUDE_V3_1.md` |
| Version | CLAUDE_V3_1 (V3 + décisions d'Arnaud du 13/11 et livraison S1) |
| Date | 2026-11-13 |
| Repo | `io-e/wims4win` (public) |
| Branches | `feat/algogebra-json-hook` (GBot, PR #10, HEAD `00eaa91`) · `feat/algogebra-wims-integration` (Claude, HEAD `bece517` + ce commit) |
| Référence | `Review_Hook_JSON_Windows_CLAUDE_V1.md` (F1–F6), `Handoff_GBot_Hook_V2_PR10_CLAUDE_V1.md`, `spec_draft/Spec_WIMS_Message_Integration_GROK_V1_2_DRAFT_CLAUDE_V1.md`, `docs/09_Hook_JSON_AlgoGebra.md`, `docs/handoff_Wims4Claude_GBOT_v1.md` |

---

## 0. Décisions d'Arnaud (13/11)

| # | Question | Décision | Conséquence |
|---|---|---|---|
| D1 | Reprendre V2 dans PR #10 ? | **Oui, par note seulement.** GBot copie `runtime_patch/algogebra_json_CLAUDE_V2.phtml` dans `tools/algogebra-hook/`, bump du marqueur, recette `docs/09` étendue (menu, checkbox, 00085), rejeu Debian avant fusion. Claude ne touche pas `feat/algogebra-json-hook`, pas de commentaire GitHub, pas de modification du runtime Laragon. | `Handoff_GBot_Hook_V2_PR10_CLAUDE_V1.md` rédigé (ce commit). H1–H4 inchangées, côté GBot. |
| D2 | Spec GROK_V1_2 ? | **Oui, rédiger maintenant, ne pas publier.** Quatre points seulement : §3.2 (`expected` = texte posté ; bloc dans le charset de la page), §5.2 (POST encodé dans le charset de la réponse `cmd=new`), §6 (`INVALID_ANSWER` sur `200` + pas de bloc + `wims_msg alert` ; `NO_JSON_BLOCK` sinon ; pas de nouveau champ JSON), §3.1 (`qnum` reste 1, séries hors périmètre). Arnaud décide si le brouillon devient la spec ; rien n'est commité dans `io-e/AlgoGebra`. | **S1 livrée** : `spec_draft/Spec_WIMS_Message_Integration_GROK_V1_2_DRAFT_CLAUDE_V1.md` (texte intégral V1_1 + modifications balisées **[V1_2]**), copie de référence V1_1 et diff unifié à côté. |
| D3 | `function` / `formal` non couverts sur Windows ? | **Accepter l'écart, ne pas bloquer PR #10.** Limite du banc (`wims-runtime-oef` = PARI seul), pas un bug du hook. Rejouer plus tard sur Debian ou sur le sibling Laragon qui a Maxima. Ne pas installer Maxima sur `wims-runtime-oef`, ne pas éditer `wims-runtime-r19040-grok-hook-20261106`. | S3 (E2E Windows) perd le type `function` : 4 types au lieu de 5 ; la couverture CAS passe à S4 (Debian, GBot). |

---

## 1. Ce qui a changé depuis le plan V2 (04/11)

| Track V2 | Prévu | Réalisé | Par qui | Où |
|---|---|---|---|---|
| A — Discovery | Claude 2 j/h | **Fait** 05/11 | Claude | `Discovery_Variable_Dump_GROK_V1.md`, `OEF_Variable_Map_GROK_V1.md` |
| B — Hook JSON | Claude 4.5 j/h | **Fait** 05–06/11, vendu par GBot dans `tools/algogebra-hook/` (PR #10) ; **V2 du corps** proposée 13/11 | Claude puis GBot | `runtime_patch/`, `tools/algogebra-hook/` |
| C — Validation | Claude 1.5 + GBot 1 | **Partiel** : Debian HTTP 8/8 (GBot) ; revue corpus Windows 13/11 (Claude) : 6 constats, V2 validée 10/10 | GBot + Claude | `tests/out_corpus_*`, `Review_Hook_JSON_Windows_CLAUDE_V1.md` |
| D — Runtime livrable | Claude 3.5 j/h | **Fait** par GBot : sibling `wims-runtime-r19040-grok-hook-20261106` | GBot | `docs/09`, handoff §4 |
| E — End-to-end | Claude 4 + GBot 2 | **Non commencé** côté client (WA-9, Codex) | Codex | AlgoGebra `codex/wims-wa9-client` |

Chemin critique : **PR #10 (V2 + fusion) → WA-9 (client, sur spec V1_2) → E2E**.

---

## 2. Constats de la revue Windows (13/11) — état

| # | Constat | Gravité | Correctif | État |
|---|---|---|---|---|
| F1 | `expected` = `<index>;<options>` pour menu/radio/checkbox/clickfill | Haute | hook V2 | fait (Claude), à reprendre dans PR #10 (GBot, D1) |
| F2 | réponses à `replygood` vide listées comme fausses | Moyenne | hook V2 : omises | idem |
| F3 | `\steps` : toutes les réponses listées | Moyenne | hook V2 : filtre `gotsteps` | idem |
| F4 | pages d'erreur sans bloc → `NO_JSON_BLOCK` à tort | Moyenne | spec §6 `INVALID_ANSWER` | **rédigé** dans le brouillon V1_2 (D2), à approuver |
| F5 | `qnum>1` : `grade=0` intermédiaire | Basse | spec §3.1 `qnum` jamais envoyé | **rédigé** (D2) |
| F6 | bloc non ASCII ; POST à encoder dans le charset de la page | Haute (client) | spec §3.2 / §5.2 | **rédigé** (D2) ; implémentation Codex (S2) |

---

## 3. Travail restant — charges en jours/homme

### Côté wims4win (hook) — GBot

| # | Tâche | Charge |
|---|---|---|
| H1 | Reprendre V2 dans `tools/algogebra-hook/algogebra_json.phtml` (PR #10), bump marqueur, `docs/09` : recette étendue (00004 radio, 00008 menu, 00045 checkbox, 00085 steps, 00021 point libre) — détail dans `Handoff_GBot_Hook_V2_PR10_CLAUDE_V1.md` | 0.5 j/h |
| H2 | Rejeu Debian (00006/00076 + les 5 ci-dessus + un `function` puisque Maxima est disponible) ; preuves | 0.5 j/h |
| H3 | Documenter le script corpus comme recette manuelle dans `docs/09` (pas en CI : runtime Windows requis) | 0.25 j/h |
| H4 | PR #10 : merge `master`, CI verte, check `ALGOGEBRA_HOOK=0`, M4 (V2 adoptée) | 0.5 j/h |
| | **Sous-total** | **1.75 j/h** |

### Côté spec / client / E2E

| # | Tâche | Qui | Charge | État |
|---|---|---|---|---|
| S1 | Brouillon spec GROK_V1_2 (§3.1, §3.2, §5.2, §6) | Claude | 0.5 j/h | **fait** (ce commit) ; décision de publication : Arnaud |
| S1b | Si approuvé : commit dans `io-e/AlgoGebra` `02-Cdc/`, archivage V1_1, mise à jour des renvois (Cdc WI-20..24) | Claude, sur demande explicite | 0.25 j/h | en attente |
| S2 | WA-9 `HttpWimsClient` sur spec V1_2 : charset du POST = charset de `cmd=new`, `WimsSession.charset`, extraction + `INVALID_ANSWER`, fixtures = 10 blocs réels de `tests/out_corpus_CLAUDE_V2_hook_CLAUDE_V1.json` | Codex | 2 j/h | à lancer après S1 approuvée |
| S3 | E2E Windows (AlgoGebra desktop ↔ `wims-runtime-oef` + hook V2) sur **4 types** : numeric, menu, checkbox, clickfill (`function` retiré, D3) | Claude | 1.5 j/h | après S2 |
| S4 | E2E Debian, y compris `function` / `formal` (Maxima) | GBot | 1.25 j/h | après H4, S2 |
| | **Sous-total** | | **5.5 j/h** (dont 0.5 fait) |

### Récapitulatif

| Agent | Restant | Fait depuis V3 |
|---|---|---|
| Claude | 1.75 j/h (S1b, S3) | 0.5 (S1) |
| GBot | 3 j/h (H1–H4, S4) | — |
| Codex | 2 j/h (S2) | — |
| **Total restant** | **6.75 j/h** | |

Dépendances : S1 (fait) → décision Arnaud → S1b, S2. H1 → H2 → H4. S2 → S3 ; H4 + S2 → S4.

---

## 4. Règles de coordination (rappel, handoff GBot §8 + décisions D1/D3)

- Aucun commit de Claude sur `feat/algogebra-json-hook` ; V2 et notes livrées sur `feat/algogebra-wims-integration`.
- Aucune modification des runtimes `Reprise_Wims_Grok` (live Laragon = `hook-20261106`), pas de Maxima sur `wims-runtime-oef`.
- Pas de commentaire GitHub, pas de fusion, pas de push, pas de commit dans `io-e/AlgoGebra` sans demande explicite d'Arnaud.

---

## 5. Limites du banc Windows (`wims-runtime-oef`) — acceptées (D3)

| Exercice | Cause | Traitement |
|---|---|---|
| 00055, 00058, 00072 (`function`, `formal`) | `bad_exo` : PARI seul, pas de Maxima | rejeu en S4 (Debian) ou sur le sibling Laragon avec Maxima, après fusion |
| 00027 | `canvasdraw` : `bad_cmd` | hors périmètre hook |
| 00352 | `too_many_gotos` (décision 25/10) | hors périmètre hook |

---

*Fin de `Plan_Dev_AlgoGebra_Integration_CLAUDE_V3_1.md`.*
