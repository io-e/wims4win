# NOTICE — hook JSON AlgoGebra (wims4win)

## Provenance

| Fichier | Auteur | Origine |
|---|---|---|
| `algogebra_json.phtml` | Grok Bot, 2026-11-05/06, pour wims4win | Fichier **nouveau**, écrit pour émettre le JSON de notation. Il n'est pas extrait du client AlgoGebra ni d'un module OEF amont. |
| Injection dans `scripts/oef/answer.phtml` | Grok Bot | Patch minimal (bloc `!if $special_parm=algogebra_json` après `.answer_analysis`). Le fichier de base reste le modèle OEF de WIMS (SVN r19040). |
| `install-algogebra-hook.sh` / `.ps1`, `uninstall-algogebra-hook.sh` / `.ps1` | Grok Bot | Outils wims4win. |

L'appel `!read oef/algogebra_json.phtml` ne s'exécute que si le client envoie `cmd=reply` **et** `special_parm=algogebra_json`. Sans ce paramètre, le comportement de `answer.phtml` est inchangé.

## Licence (GPL-2, comme WIMS)

WIMS est sous GNU GPL version 2 (`LICENSE` à la racine de ce dépôt). Ces fichiers font partie de wims4win et sont publiés sous **GNU GPL v2**, afin de rester combinables avec les modèles `scripts/oef/*.phtml` de WIMS.

Une copie de la GPL-2 se trouve dans `LICENSE`.

## Rapport avec le client AlgoGebra (EUPL)

Le client AlgoGebra (dépôt `io-e/AlgoGebra`, EUPL) **consomme uniquement** le bloc JSON transporté en HTTP (`<!-- ALGOGEBRA_JSON_BEGIN … ALGOGEBRA_JSON_END -->`). Il n'est pas compilé avec WIMS, n'est pas recopié dans cet arbre, et n'est pas une œuvre dérivée de ces modèles phtml.

Aucun fichier EUPL n'est incorporé ici. Il n'y a donc pas de combinaison de licences dans le runtime WIMS : le hook est GPL-2, le client reste EUPL de son côté.

Si une version ultérieure devait mélanger du code EUPL dans l'arbre WIMS, il faudrait une analyse spécifique (l'EUPL 1.2 prévoit une compatibilité déclarée avec la GPL-2 lors d'une combinaison). Ce n'est pas le cas de ce hook.
