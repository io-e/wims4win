# Outils de test et de validation OEF (provenance : OEF_VALIDATION)

Ce dossier rassemble la chaîne de test OEF réalisée dans le projet OEF_VALIDATION (Grok Bot, octobre 2026), versionnée dans wims4win. Les sources de référence sont sur la box, dans `/workspace/oefc` (`dist/`, `build.sh`, `parity.sh`, `parity.tsv`, `corpus.map`). Elles ont été recopiées ici sans modification de la logique : seuls les chemins sont devenus paramétrables et les éléments personnels ont été neutralisés.

| Dossier | Contenu |
|---|---|
| `oef2wims/` | Sources **non modifiées** de oef2wims, WIMS 4.31b (`src/`, `NOTE.txt`).<br>Shim MinGW : `shim/oef2wims_win_compat.h`, `oef2wims_win_binmode.c`, `sys/resource.h`, `config_win.h` (le `config.h` Windows).<br>Scripts `build.sh` (Linux + Windows) et `parity.sh`, avec la référence `parity.tsv`.<br>Docs `docs/` (`build.log`, `rapport_compilation.html`, `fichiers_sources_C.md`). |
| `wrapper/` | `oef2wims_log.c` : `oef2wims-log.exe`, qui produit un journal `nom.log`. |
| `trace/` | `oef2wims-trace.exe` : build instrumenté (`oef_trace.c/.h`, `oef_trace.patch` avec 4 hooks `#ifdef OEF_TRACE`, `mksym.py`, `build_trace.sh`, `parity_trace.sh/.tsv`). |
| `v2/` | `oef2wims-v2.exe`, assistant de correction (`oef2wims_v2.c`, `helpers.inc`, `msgs.inc`, `fakepy.c`), avec `build.sh`, `parity_v2.sh/.tsv`. |
| `llm/` | `oef_llm_fix.py` : correction assistée par LLM (Ollama ou API compatible OpenAI), avec `README_LLM.txt` et `test/mock_ollama.py`. |
| `html/` | `oef2html.py` : génération HTML statique d'énoncés et de corrections via le vrai moteur WIMS. Le patch wims4win `--cgi` / `--cgi-prefix` est **appliqué** (source : `tools/oef2html-wims4win.patch`).<br>Aussi : `template/`, `README_HTML.txt`, `build_wims_runtime.sh`, `Dockerfile` (**non testé**), `exemples/`, `compare_html.py` (comparaison normalisée de deux dossiers de sortie). |
| `exemples/` | Cas d'erreur commentés `exemples_v2/` (c01…c17), `exemple_e2.*`, `exemple_trace_e2.log`. |
| `corpus/` | `corpus.map`, `corpus.sha256`, `rebuild_corpus.sh`, `erreurs/` (e1…e5). |
| `LISEZMOI.txt`, `AMELIORATIONS.txt` | Notices d'origine pour l'utilisateur Windows. |
| `ci.sh`, `ci_ref.tsv` | Vérification légère utilisée par la CI. |

**Non inclus :**
- Les exécutables `.exe` et `oef2wims-win.zip`. Ils sont reproductibles par les scripts ci-dessous ; pour les distribuer, il est proposé de créer une GitHub Release (pas encore créée).
- Le tarball upstream.
- `wims-runtime`.
- Les dossiers de travail `build/`, `err/`, `pr/`, `pt/`, `trace/obj`, `v2/pv`.
- Les fichiers `__pycache__`.
- **Le validateur d'Arnaud `oef_validate.py`**, qui n'est pas publié. `oef2wims-v2.exe --validator <chemin>` l'appelle s'il est fourni.

## Licences
- Les sources oef2wims sont celles de WIMS 4.31b, sous **GPL v2 ou ultérieure** (Copyright XIAO Gang et contributeurs WIMS) ; voir `LICENSE` à la racine du dépôt et les en-têtes des fichiers.
- Shim, wrappers, trace, v2, scripts et outils Python : écrits pour ce projet et distribués sous la même licence que wims4win (GPL).

## Prérequis
- Linux (Debian/Ubuntu) : `build-essential`, `mingw-w64` (testé avec `x86_64-w64-mingw32-gcc 14-win32`), `python3` et `patch`. `wine` n'est nécessaire que pour les parités.
- Python : **bibliothèque standard uniquement**, version 3.8 ou plus (testé avec 3.10, 3.12 et 3.13).

## Construire (tout va dans `build/oef-validation/`, modifiable avec `OUT=`)
```bash
bash tools/oef-validation/oef2wims/build.sh      # linux/oef2wims + win/oef2wims.exe
bash tools/oef-validation/v2/build.sh            # win/oef2wims-v2.exe + win/oef2wims-log.exe
bash tools/oef-validation/trace/build_trace.sh   # win/oef2wims-trace.exe
bash tools/oef-validation/ci.sh                  # tout + contrôle sur 37 OEF d'exemple
```

## Corpus de test (1438 fichiers)
Le corpus n'est pas versionné. `corpus.map` indique l'origine de chaque fichier :
- 333 viennent du tarball upstream `wims_4.31b+dfsg1.orig.tar.gz`, de sha256 `b8091a9a16db6fb1264f7bc3a211732fc05562bb20a1b6e7b0963c51c4c13fb9` (archive Debian, non republiée ici) ;
- 5 sont des cas d'erreur, livrés dans `erreurs/` ;
- 1100 sont des exercices Eleda convertis par xml2oef (`eleda:`). Ils ne sont pas publiés et il faut fournir leur dossier.
```bash
sh tools/oef-validation/corpus/rebuild_corpus.sh wims_4.31b+dfsg1.orig.tar.gz corpus_oef [dossier_eleda_oef]
# sortie : "sha256 identiques=1438 differents=0" quand le dossier Eleda est fourni, 338 sinon
```

## Parités (wine requis)
```bash
bash tools/oef-validation/oef2wims/parity.sh corpus_oef   # Linux vs Windows : 1438/1438 SAME
bash tools/oef-validation/v2/parity_v2.sh corpus_oef      # v2 vs oef2wims.exe : 1438/1438 SAME
bash tools/oef-validation/trace/parity_trace.sh corpus_oef  # trace vs oef2wims.exe : 1438/1438 SAME
```
Ces résultats ont été mesurés le 25 octobre 2026 depuis le dépôt. Le build Linux du dépôt donne aussi des sorties identiques au build d'origine sur les 1438 fichiers (stdout, code de retour, `.def`). Les sha256 des `.exe` diffèrent de ceux de `dist/` à cause des chemins de compilation, mais leurs sorties sont identiques.

## oef2html
- **Linux**, arbre WIMS construit par `html/build_wims_runtime.sh` :
  `python3 html/oef2html.py --wims-home wims-runtime --oef2wims build/oef-validation/linux/oef2wims --out html_out exo.oef`
- **Windows**, moteur wims4win. Utiliser le runtime `wims-runtime-oef` (fichier `log/wims.conf` : `default_texposition=2`, plus `pari_command=…/gp.exe -f -q -p 150000` si PARI est installé ; fichier `log/wims4win.yaml` : `formula.size`) :
  `python oef2html.py --wims-home <wims-runtime-oef> --oef2wims oef2wims.exe --cgi <wims-runtime-oef>\public_html\wims.cgi.exe --out out exo.oef`
- Sous Linux avec le moteur Windows : ajouter `--cgi-prefix wine`.
- `python3 html/compare_html.py ref/ out/` compare deux sorties en normalisant les dates, les identifiants de session et les identifiants MathML, la taille MathML et les images.
