# 09 — Hook JSON AlgoGebra : installation et désinstallation

*Document du 6 novembre 2026 (heure de Paris), mis à jour le 14 novembre 2026. Il décrit le hook livré comme pièce du produit wims4win (branche `feat/algogebra-json-hook`), pour Windows et Debian. Le corps du hook est **GROK_V2_1** (corps CLAUDE_V2_1 : F1–F3, et l’échappement `--` → `\\u002d\\u002d` perdu dans V2). Pourquoi V2 : `AlgoGebra_Wims_Integration/Review_Hook_JSON_Windows_CLAUDE_V1.md` ; V2_1 : `Handoff_GBot_Hook_V2_PR10_CLAUDE_V1_3.md` sur `feat/algogebra-wims-integration` (`cb490b7` / `55ef351`).*

Le hook ajoute un bloc JSON dans la page de correction OEF lorsque le client envoie `cmd=reply` **et** `special_parm=algogebra_json`. Sans ce paramètre, WIMS se comporte comme avant. Le chemin de notation n'est pas `adm/raw` ni `job=getresult`. Les champs de réponse restent `replyN` / `choiceN`.

Les fichiers sont versionnés dans `tools/algogebra-hook/` (plus de dépendance à `AlgoGebra_Wims_Integration/runtime_patch`). Licence et provenance : `tools/algogebra-hook/NOTICE.md`.

## Ce que le build installe

`tools/build-runtime-tree.sh` installe **toujours** le hook :

1. dans l'arbre Linux de compilation (`$WD/wims`) ;
2. dans l'arbre Windows empaqueté (`$WD/wims-runtime`).

Il copie `algogebra_json.phtml` vers `public_html/scripts/oef/` et injecte le bloc dans `answer.phtml` (après `!!end of .answer_analysis` / `</div>`, avant `!!presentsol=1 deprecated`). Une sauvegarde `answer.phtml.bak_pre_algogebra` est créée si elle n'existe pas encore.

Pour omettre le hook (cas exceptionnel) :

```bash
ALGOGEBRA_HOOK=0 tools/build-runtime-tree.sh <export-svn-r19040> [workdir]
```

Sans cette variable, l'absence du fichier vendu `tools/algogebra-hook/algogebra_json.phtml` fait échouer le build.

## Debian / Linux — installer sur un arbre déjà construit

```bash
# depuis la racine du dépôt wims4win
bash tools/algogebra-hook/install-algogebra-hook.sh /chemin/vers/arbre-wims
```

Le script est idempotent. Contrôles rapides :

- présence de `public_html/scripts/oef/algogebra_json.phtml` ;
- présence de `GROK_V1 AlgoGebra JSON hook` dans `public_html/scripts/oef/answer.phtml` ;
- présence de `answer.phtml.bak_pre_algogebra`.

Désinstallation (restaure `answer.phtml` depuis `.bak_pre_algogebra` s'il existe, sinon retire le bloc injecté, puis supprime `algogebra_json.phtml`) :

```bash
bash tools/algogebra-hook/uninstall-algogebra-hook.sh /chemin/vers/arbre-wims
```

Le binaire CGI Linux n'est pas modifié (hook uniquement en phtml). Après installation, un exercice OEF noté avec `cmd=reply&special_parm=algogebra_json` doit contenir un commentaire HTML `ALGOGEBRA_JSON_BEGIN` … `ALGOGEBRA_JSON_END` dont le corps est du JSON dans le charset de la page (`windows-1252` pour le français) — pas forcément de l'ASCII pur (accents dans les libellés, ex. 00045).

## Windows — installer sur un runtime déjà déployé

Ne pas redéployer un arbre complet sur le PC sans accord. Pour activer le hook dans un runtime existant (par exemple `wims-runtime-r19040-grok`) :

```powershell
powershell -File tools\algogebra-hook\install-algogebra-hook.ps1 -RuntimeRoot D:\chemin\vers\wims-runtime
```

Désinstallation (même règle de restauration de `answer.phtml`) :

```powershell
powershell -File tools\algogebra-hook\uninstall-algogebra-hook.ps1 -RuntimeRoot D:\chemin\vers\wims-runtime
```

Aucun fichier `.bat` n'est utilisé. Après un prochain `build-runtime-tree.sh`, l'arbre Windows produit contient déjà le hook : l'installateur PowerShell n'est alors utile que pour un runtime plus ancien.

## Désinstaller sans fichier `.bak`

Si `answer.phtml.bak_pre_algogebra` a été perdu, les scripts `uninstall-*` retirent le bloc délimité par `!! GROK_V1 AlgoGebra JSON hook` … `!endif` juste avant `!!presentsol=1 deprecated`, puis effacent `algogebra_json.phtml`. Vérifier ensuite que `GROK_V1 AlgoGebra JSON hook` a disparu de `answer.phtml`.

## Recette minimale (00006 / 00076) et extension V2 (F1–F3)

Préparer un module OEF (copie de `modules/template/oef.fr` vers `modules/H6/test/oef2html.fr`), compiler les `.oef` avec `bin/oef2wims` (Linux) ou `bin\oef2wims.exe` (Windows), fixer `!setseed 1` dans `var.proc`, puis :

1. `cmd=new` (sans `special_parm`) pour ouvrir la session ;
2. `cmd=reply&special_parm=algogebra_json&reply1=…` — JSON valide, `id` = `reply1`, note 0 ou 1 ;
3. le même `cmd=reply` **sans** `special_parm` — pas de `ALGOGEBRA_JSON_BEGIN` ;
4. renvoyer les valeurs `expected` du bloc comme réponses (même graine, nouvelle session) : `grade` 1.0.

Réponses de référence (graine 1) : 00006 faux `0` / juste `2` ; 00076 faux `1` / juste `3, 5`. Ces deux fumées ne doivent pas changer de résultat par rapport à GROK_V1.

Exercices supplémentaires (sources `tools/oef-validation/html/exemples/sources/`, POST dans le charset de la page, typiquement `windows-1252`) :

| Exercice | Type | Contrôle dans le bloc |
|---|---|---|
| 00004 | `radio` | `expected` est le texte d'option (`blu e arancio` à la graine 1, 2e item), pas `2;blu e verde,blu e arancio,…` ; le rejouer comme `reply1` donne `grade:1` |
| 00008 | `menu` | `expected` est le texte d'option (`falsa` à la graine 1, 2e item), pas `2;vera,falsa` ; le rejouer donne `grade:1` |
| 00045 | `checkbox`, options accentuées | `expected` = textes d'options séparés par des virgules ; le bloc se décode en `windows-1252` ; un POST `windows-1252` donne `grade:1` (un POST UTF-8 donne 0.33, c'est attendu) |
| 00085 | `\steps`, 12 réponses | étape 1 : exactement 2 entrées (`reply1`, `reply3`), aucune avec `given:""` |
| 00021 | 5 réponses, la 5e libre | 4 entrées, `grade` cohérent avec `answers[]` |

Cas `--` (V2_1) : un exercice `case` dont la réponse est `a-->b` doit émettre `\\u002d\\u002d` *à l’intérieur* du commentaire HTML (pas un `--` brut, qui fermerait le commentaire). Après décodage JSON, `given` et `expected` valent `a-->b`. Voir `test_algogebra_json_smoke_CLAUDE_V1_1.py`.

Sur un runtime Debian avec Maxima, inclure aussi au moins un exercice `function` / `formal` / `algexp` (00055, 00058 ou 00072) : V2 les traite comme les « autres types » (`replygood` brut), comme GROK_V1.

Recette manuelle Windows (optionnelle, pas pour la CI) : `python -X utf8 -I AlgoGebra_Wims_Integration/tests/test_algogebra_json_corpus_CLAUDE_V1.py` depuis un checkout de `feat/algogebra-wims-integration`, avec `--sources tools/oef-validation/html/exemples/sources` (le script cible un runtime Windows). Fumée Windows **et** Linux (00006/00076 + cas `--`) : `WIMS_HOME=… OEF2WIMS=… OEF_SOURCES=… python3 -X utf8 -I AlgoGebra_Wims_Integration/tests/test_algogebra_json_smoke_CLAUDE_V1_1.py`.

## Fichiers concernés

| Chemin | Rôle |
|---|---|
| `tools/algogebra-hook/algogebra_json.phtml` | Corps JSON |
| `tools/algogebra-hook/install-algogebra-hook.sh` | Installation Linux |
| `tools/algogebra-hook/uninstall-algogebra-hook.sh` | Désinstallation Linux |
| `tools/algogebra-hook/install-algogebra-hook.ps1` | Installation Windows |
| `tools/algogebra-hook/uninstall-algogebra-hook.ps1` | Désinstallation Windows |
| `tools/algogebra-hook/NOTICE.md` | Provenance et licences GPL / EUPL |
| `tools/build-runtime-tree.sh` | Installation automatique à la construction |
