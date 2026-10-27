# Handoff pour Claude : chaîne Python OEF (validation et génération HTML)

Rédigé par Grok Bot le 25/10/2026 pour Arnaud Lefrancois. Dépôt : `io-e/wims4win` (privé), outils dans `tools/oef-validation/`.

## 1. Objectif
Reprendre et faire évoluer le code Python de la chaîne OEF :
- **`tools/oef-validation/html/oef2html.py`** : produit des pages HTML statiques (énoncé et correction) à partir de `.oef`, via le vrai moteur WIMS. Le moteur est soit le Linux, soit **wims4win `wims.cgi.exe`** sous Windows.
- **`tools/oef-validation/llm/oef_llm_fix.py`** : correction assistée par LLM (Ollama ou API OpenAI-compatible). Il boucle sur « compiler avec `oef2wims-v2.exe` → proposer un correctif ».
- **L'appel du validateur Python depuis `oef2wims-v2.exe`** (`--validator <oef_validate.py> [--python ...]`, code dans `v2/oef2wims_v2.c`) : les diagnostics du validateur sont fusionnés dans `nom.log`/`nom.json`.

## 2. État actuel et résultats mesurés
- **oef2wims (WIMS 4.31b, sources non modifiées), Linux et Windows (mingw)**, sur 1438 fichiers : parité **1438/1438** identique (stdout, stderr, code de retour, `.def`). Même résultat pour `oef2wims-v2.exe` et pour `oef2wims-trace.exe` face à `oef2wims.exe`. Ces mesures ont été refaites le 25/10 depuis le dépôt.
- **Corpus :** 333 fichiers viennent du tarball upstream 4.31b, 5 sont des cas d'erreur et 1100 sont des exercices Eleda convertis par xml2oef. Ces derniers ne sont pas publiés ; voir `corpus/rebuild_corpus.sh`.
- **oef2html sur PC avec wims4win** (natif, échantillon de 10 fichiers, graine 1, 22 s) :
  - 6 pages sont **identiques** à Linux après normalisation : 00001 (image flydraw comprise), 00006, 00013, 00076, 00403 (PARI) et 00726.
  - 4 échecs, **identiques** à Linux : 00347, 00352 et 00509 en too_many_gotos, 01012 en bad_index.
- **Moteur :** WIMS 4.33a porté nativement (wims4win). flydraw est porté (libgd statique, GIF) avec un `insdraw..processor.exe` natif.

## 3. Lancer sur le PC (Windows, sans .bat ni cmd.exe : PowerShell + python.exe)
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

## 4. Décisions d'Arnaud
- Moteur **wims4win 4.33a**, sans rebasage sur 4.31b. La seule exception assumée est le comportement Linux de la taille des formules, qui vient maintenant du YAML.
- Les **58 too_many_gotos** du corpus sont ignorés (« vieilles études »). Ils viennent des exercices Eleda convertis par xml2oef : procédure récursive non traduite dans un `\while`, ou grands entiers affichés en notation e+NN. Voir `docs/Evaluation_OEF_wims4win.md` §3.
- flydraw est porté et PARI/GP est configuré.
- **Dépôt unique** `io-e/wims4win` ; ne rien écrire dans `Reprise_Wims_Claude`.
- **Le validateur d'Arnaud `oef_validate.py` n'est pas publié** dans le dépôt ; il est seulement appelé via `--validator`.
- Pas de `.bat` ni de `cmd.exe` : lancer les exe et `python.exe` directement.

## 5. Limites connues
- **insdraw :** une animation ne donne que sa 1re image, et il n'y a pas de conversion de format. Un GIF est livré sous le nom demandé, faute d'ImageMagick.
- **flydraw :** pas de freetype, donc pas de texte en police TTF.
- **`html/Dockerfile` :** non testé.
- **Encodage :** quand `oef2wims-v2.exe` lance le validateur Python, il faut `PYTHONIOENCODING=utf-8`, sinon les accents sont mal décodés ou provoquent un échec sous une console cp1252. Ce n'est pas encore forcé par v2.
- **Feedback de bonne réponse :** oef2html n'affiche pas encore le feedback d'une bonne réponse (`\feedback` quand la réponse est juste) ; c'est à faire.
- **CAS :** seuls PARI et Maxima sont configurés côté wims4win. Les autres (GAP, Octave…) sont à vérifier exercice par exercice.

## 6. Tâches prioritaires pour Claude
1. **oef2html : affichage du feedback de bonne réponse.** Pour chaque réponse, envoyer la bonne réponse (et pas seulement une réponse fausse) et intégrer le feedback correspondant dans la page de correction. Ajouter des tests sur 00076 et 00726.
2. **v2 + validateur :** définir `PYTHONIOENCODING=utf-8` dans l'environnement du processus fils (`v2/oef2wims_v2.c`). Valider sur `exemples/exemples_v2` (c14_latin1_bom en particulier) sous Windows.
3. **Chaîne unique en Python :** commande `oef-check` en stdlib qui enchaîne oef2wims-v2 (+ validateur), oef_llm_fix (optionnel) puis oef2html, et produit un rapport JSON/HTML par fichier. Fonctionnement Windows natif.
4. **Tests automatisés en Python :** unittest/pytest stdlib sur `html/exemples/sources` et `corpus/erreurs`, avec une référence de sortie normalisée (`compare_html.py`). Les brancher dans `tools/oef-validation/ci.sh`.
5. **oef_llm_fix :** tester sur les cas `exemples_v2` avec `llm/test/mock_ollama.py`, puis avec un vrai modèle. Mesurer le taux de correction et ne jamais écraser le `.oef` sans sauvegarde.
6. **Documentation PC :** mettre à jour `README_HTML.txt` et `README_LLM.txt` pour wims4win (`--cgi`, `wims-runtime-oef`, YAML de taille).
7. **Plus tard :** Dockerfile (le tester ou le retirer), images animées/conversion, aide à la correction xml2oef des 58 too_many_gotos si Arnaud le demande.

## 7. Où trouver quoi
- Outils : `tools/oef-validation/README.md` (build, parités, corpus, licences GPL).
- Évaluation et explications (formule, CRLF, 110/120 %, PARI, flydraw) : `docs/Evaluation_OEF_wims4win.md`.
- Suivi : `docs/Suivi_Developpement_Portage.md`.
- Runtime : `tools/build-runtime-tree.sh` (inclut `default_texposition=2`, le YAML, flydraw et insdraw).
