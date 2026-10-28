# Documentation du portage wims4win

Depuis le 27/10/2026, les documents de Claude et de Grok sont **fusionnés en documents uniques**. Chaque document indique ses sources et contient une section « Divergences résolues ». Règle de fusion : la vérité est le code et les résultats mesurés.

| Document | Contenu | Sources (dans `archive/`) |
|---|---|---|
| [`01_Audit_Migration.md`](01_Audit_Migration.md) | Audit des composants à porter, état réel | Audits Grok (Fusion v2, complément, v1) + `Audit_Migration_WIMS_Windows_CLAUDE_V2.md` |
| [`02_Architecture_Portage.md`](02_Architecture_Portage.md) | Architecture implémentée, spécification détaillée, étude MSVC | `Architecture_…_V3.md`, `…_CLAUDE_V1_3.md`, `…_GROK_V2.md` (§0), `Etude_Portage_MSVC_Natif_CLAUDE_V1_1.md` |
| [`03_Cahier_Audit_et_Recette.md`](03_Cahier_Audit_et_Recette.md) | Audit du code, recette, évaluation OEF | `Cahier_Audit_…_CLAUDE_V2.md`, `Cahier_Recette_…_CLAUDE_V2_1.md`, `Evaluation_OEF_wims4win.md` |
| [`04_Guide_Installation_et_CGI_Laragon.md`](04_Guide_Installation_et_CGI_Laragon.md) | Installation Windows, CAS, Apache/Laragon | `Guide_Installation_Wims4Win_CLAUDE_V1.md`, `Guide_CGI_Laragon_WIMS_CLAUDE_V1_1.md` |
| [`05_Etat_et_Decisions.yaml`](05_Etat_et_Decisions.yaml) | État, décisions, travaux restants, points à trancher | `State_Wims4Win_CLAUDE_V1.yaml` + Suivi |
| [`06_Recherche_Projets.md`](06_Recherche_Projets.md) | Pistes de migration étudiées | `Recherche_Projets_Migration_WIMS_CLAUDE_V1_1.md` |
| [`07_Handoffs.md`](07_Handoffs.md) | Handoffs Claude ↔ Grok | `HANDOFF_Claude_OEF_Python.md`, `Handoff_Grok_Wims02/03_CLAUDE_V1.md` |
| [`08_Test_Plan_Windows_Migration.md`](08_Test_Plan_Windows_Migration.md) | Test plan (EN) — code modifié pour le portage uniquement, traçabilité D/issues → code → tests | — |
| [`Suivi_Developpement_Portage.md`](Suivi_Developpement_Portage.md) | Journal chronologique du développement | — |
| `wims4win.yaml`, `apache-laragon-wims-runtime.conf` | Fichiers de configuration utilisés par le build | — |
| [`archive/`](archive/) | Originaux Claude et Grok, en lecture seule (historique git conservé) | — |

## Règles d'écriture

- **Claude écrit désormais directement dans ces documents consolidés** (et dans `05_Etat_et_Decisions.yaml`), dans ce dépôt, et non plus dans `Reprise_Wims_Claude` ni dans `docs/archive/`.
- Grok tient `Suivi_Developpement_Portage.md` et pousse sur GitHub.
- Une seule source par sujet : on corrige le document consolidé au lieu de créer une version `_Vn`.
- Les décisions marquées `decided_by: user` ne viennent que d'Arnaud.
