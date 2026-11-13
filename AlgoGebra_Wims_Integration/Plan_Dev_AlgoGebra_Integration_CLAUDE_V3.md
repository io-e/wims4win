<!-- CLAUDE_V3 -->
# Planning de développement — Intégration AlgoGebra × WIMS (wims4win) — re-baseline

| Champ | Valeur |
|---|---|
| Document | `AlgoGebra_Wims_Integration/Plan_Dev_AlgoGebra_Integration_CLAUDE_V3.md` |
| Version | CLAUDE_V3 (remplace V2 : les tracks B et D ont été livrées entre-temps) |
| Date | 2026-11-13 |
| Repo | `io-e/wims4win` (public) |
| Branches | `feat/algogebra-json-hook` (GBot, PR #10, HEAD `00eaa91`) · `feat/algogebra-wims-integration` (Claude, HEAD `4ae4d86` + ce commit) |
| Référence | `Review_Hook_JSON_Windows_CLAUDE_V1.md` (constats F1–F6), `Discovery_Variable_Dump_GROK_V1.md`, `docs/09_Hook_JSON_AlgoGebra.md`, `docs/handoff_Wims4Claude_GBOT_v1.md` |

---

## 0. Ce qui a changé depuis le plan V2 (04/11)

| Track V2 | Prévu | Réalisé | Par qui | Où |
|---|---|---|---|---|
| A — Discovery | Claude 2 j/h | **Fait** 05/11 : chaîne `Main.phtml → var.proc → answer.phtml`, variables `m_sc_reply<N>`, `diareply<N>`, injection après `answer_analysis` | Claude | `Discovery_Variable_Dump_GROK_V1.md`, `OEF_Variable_Map_GROK_V1.md` |
| B — Hook JSON | Claude 4.5 j/h | **Fait** 05–06/11 : `algogebra_json.phtml` (Option A) + injection `answer.phtml` + smoke 00006/00076 ; puis **vendu** par GBot dans `tools/algogebra-hook/` et câblé dans `build-runtime-tree.sh` (PR #10) | Claude puis GBot | `runtime_patch/`, `tools/algogebra-hook/` |
| C — Validation | Claude 1.5 + GBot 1 | **Partiel** : Debian HTTP 8/8 (GBot) ; schéma conforme à la spec §3.2 ; **revue corpus Windows 13/11 (Claude)** : 6 constats, V2 du hook proposée | GBot + Claude | `tests/out_corpus_*_CLAUDE_V1.md`, `Review_Hook_JSON_Windows_CLAUDE_V1.md` |
| D — Runtime livrable | Claude 3.5 j/h | **Fait** par GBot : sibling `wims-runtime-r19040-grok-hook-20261106` (= Laragon live), hook installé au build | GBot | `docs/09`, handoff §4 |
| E — End-to-end | Claude 4 + GBot 2 | **Non commencé** côté client : WA-9 (HttpWimsClient, Codex) débloqué, non livré | Codex | AlgoGebra `codex/wims-wa9-client` |

Le chemin critique n'est plus côté wims4win : il est **PR #10 (fusion) → WA-9 (client) → E2E**.

---

## 1. Constats de la revue Windows (13/11) à traiter avant la fusion de PR #10

Détail et preuves : `Review_Hook_JSON_Windows_CLAUDE_V1.md`. Résumé :

| # | Constat | Gravité | Correctif proposé | Qui |
|---|---|---|---|---|
| F1 | `expected` = `<index>;<liste d'options>` pour menu/radio/checkbox (et `;options` pour clickfill) : ni affichable ni rejouable — le formulaire poste le **texte** de l'option | Haute | `algogebra_json_CLAUDE_V2.phtml` : `$(replyGood<N>)` pour la famille menu, `!line 1 of !rows2lines` pour la famille fill/texte (idiome des `anstype/`) | Claude (fait, testé), à reprendre dans PR #10 par GBot |
| F2 | Réponses à `replygood` vide (point libre, aucune ligne d'analyse) listées comme fausses, score 0 | Moyenne | V2 : omises | idem |
| F3 | Exercices à étapes (`\steps`) : les 12 réponses listées alors que 2 sont notées | Moyenne | V2 : `r<N> isitemof $gotsteps` comme `answer.phtml` | idem |
| F4 | Pages d'erreur (`bad_exo`, `bad_answer`, `empty_data`) : aucun bloc JSON ; le client conclut « hook non installé » (`NO_JSON_BLOCK`) | Moyenne | Spec §6 : état `INVALID_ANSWER` quand `200` + `wims_msg alert` + pas de bloc (pas de nouveau champ JSON, décision propriétaire) | Codex / spec (GROK_V1_2) |
| F5 | Séries `qnum>1` : `grade=0` sur les questions intermédiaires (`module_score` non défini avant la fin) | Basse si le client reste en `qnum=1` | Spec : imposer `qnum=1` ; sinon V3 du hook avec note provisoire `$allgot/$alltot` | spec ; hook si demandé |
| F6 | Encodage : le bloc n'est **pas** ASCII (textes d'options accentués) ; il est dans le charset de la page (`windows-1252`). Et le POST du client doit être encodé dans ce charset, pas en UTF-8 (spec §5.2 l'indique à l'envers) | Haute côté client | Spec §3.2/§5.2 à corriger ; hook inchangé (le client décode déjà par `Content-Type`) | Codex / spec |

---

## 2. Travail restant — charges en jours/homme

### Côté wims4win (hook)

| # | Tâche | Qui | Charge |
|---|---|---|---|
| H1 | Reprendre V2 (`expected`, omissions F2/F3) dans `tools/algogebra-hook/algogebra_json.phtml` de PR #10, bump `GROK_V1 → V2`, mettre à jour `docs/09` (§ recette : menu/radio/checkbox) | GBot | 0.5 j/h |
| H2 | Rejouer la recette Debian (00006/00076 + un menu, un checkbox, un `\steps`) avec V2 ; preuves | GBot | 0.5 j/h |
| H3 | Ajouter `tests/test_algogebra_json_corpus_CLAUDE_V1.py` à la CI ? **Non** (nécessite un runtime Windows) : le garder comme recette manuelle, documenter la commande dans `docs/09` | GBot | 0.25 j/h |
| H4 | PR #10 : merge `master` (M1), CI verte (M2), check `ALGOGEBRA_HOOK=0` (M3) — déjà listés dans la checklist GROK_V1 | GBot | 0.5 j/h |
| | **Sous-total wims4win** | | **1.75 j/h** |

### Côté spec / client (AlgoGebra, hors wims4win)

| # | Tâche | Qui | Charge |
|---|---|---|---|
| S1 | Spec `GROK_V1_1 → V1_2` : §3.2 (bloc dans le charset de la page ; `expected` = texte d'option), §5.2 (POST encodé dans le charset de la réponse `cmd=new`), §6 (`INVALID_ANSWER`), §3.1 (`qnum=1` obligatoire) | Claude (rédaction) → Arnaud | 0.5 j/h |
| S2 | WA-9 HttpWimsClient : encodage POST, extraction, états d'erreur, fixtures réelles (`tests/out_corpus_*_CLAUDE_V1.json` fournit 10 blocs réels wrong/right) | Codex | 2 j/h |
| S3 | E2E Windows (AlgoGebra desktop ↔ runtime wims4win) sur 5 types : numeric, menu, checkbox, clickfill, function (nécessite Maxima sur le runtime) | Claude | 2 j/h |
| S4 | E2E Debian | GBot | 1 j/h |
| | **Sous-total client / E2E** | | **5.5 j/h** |

### Récapitulatif

| Agent | Charge restante |
|---|---|
| Claude | 2.5 j/h (S1, S3) |
| GBot | 2.75 j/h (H1–H4, S4) |
| Codex | 2 j/h (S2) |
| **Total** | **7.25 j/h** |

Dépendances : H1 → H2 → H4 (fusion PR #10) → S2 → S3/S4. S1 peut partir tout de suite.

---

## 3. Règles de coordination (rappel, handoff GBot §8)

- Pas de commit sur `feat/algogebra-json-hook` par Claude : les correctifs V2 sont livrés sur `feat/algogebra-wims-integration` + cette note ; GBot les reprend dans PR #10.
- Aucune modification des runtimes `Reprise_Wims_Grok` (live Laragon = `hook-20261106`). Les tests Claude utilisent `wims-runtime-oef` (hook GROK_V1 restauré à l'identique après l'essai V2, sha256 vérifié).
- Pas de commentaire GitHub ni de fusion sans Arnaud.

---

## 4. Limites du banc Windows (`wims-runtime-oef`)

| Exercice | Cause | Impact sur la revue |
|---|---|---|
| 00055, 00058, 00072 (`function`, `formal`) | `bad_exo` : pas de Maxima sur ce runtime (PARI seul) ; 10 s de timeout | Types CAS non couverts sur Windows ; à rejouer sur le sibling Laragon (Maxima OK) ou Debian |
| 00027 | `canvasdraw` : `bad_cmd` (helper absent du runtime) | Hors périmètre hook |
| 00352 | `too_many_gotos` (décision Arnaud 25/10 : non traité) | Hors périmètre hook |

---

*Fin de `Plan_Dev_AlgoGebra_Integration_CLAUDE_V3.md`.*
