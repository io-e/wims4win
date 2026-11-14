<!-- GROK_V1_1 — approved for posting (FR + EN twin), 2026-11-06 -->

Proposition de commentaire pour https://github.com/io-e/wims4win/issues/3

---

## État au 6 novembre 2026 — arbre runtime pour la recette (dont hook JSON)

L’issue demandait une source et un mode de déploiement de `public_html` / modules sous Windows. Depuis la réouverture (commentaire du 27/10 : liens svn:special, helpers, `sh.exe`, `wims.conf`, YAML, index) :

**Source (D15 / D16)**  
SVN trunk `https://subversion.renater.fr/anonscm/svn/wimsdev/trunk/wims` révision **19040** (WIMS 4.33a). Modules initiaux = export SVN, mises à jour ultérieures via `modupdate` (pas de paquets Debian).

**Construction**  
`tools/build-runtime-tree.sh <export-svn-r19040> [workdir]` produit :

- l’arbre Linux de compilation `$workdir/wims` ;
- l’arbre Windows `$workdir/wims-runtime` et l’archive `$workdir/wims-runtime.tgz` (aucun ELF, binaries Windows, `sh.exe` MSYS2, `wims.conf`, `wims4win.yaml`, index `mkindex`, helpers translator/webget/oef2wims, flydraw/insdraw).

Le runtime Grok déjà validé sur le PC (`Reprise_Wims_Grok/wims-runtime-r19040-grok`) couvre les critères listés le 27/10 (accueil fr/en, `adm/doc`, `factor.fr` via webget, `wcalc.fr`, mkindex).

**Hook JSON AlgoGebra (nouveau, branche `feat/algogebra-json-hook`)**  
Le hook n’est plus un patch manuel. `build-runtime-tree.sh` l’installe **toujours** (fichiers vendus dans `tools/algogebra-hook/`) dans l’arbre Linux **et** l’arbre Windows : `public_html/scripts/oef/algogebra_json.phtml` + injection dans `answer.phtml`, avec sauvegarde `answer.phtml.bak_pre_algogebra`. Opt-out : `ALGOGEBRA_HOOK=0`. Install / uninstall documentés dans `docs/09_Hook_JSON_AlgoGebra.md`.

Pour une recette OEF du hook, l’arbre livré suffit plus un module créé depuis `modules/template/oef.fr` (ex. `H6/test/oef2html.fr`) et la compilation de `00006.oef` / `00076.oef` par `oef2wims`. Pas besoin d’un second export SVN.

**Ce qui reste hors de cette avancée (à ne pas fermer l’issue sans accord)**

- Publier ou recopier sur le PC un **nouvel** `wims-runtime.tgz` construit depuis cette branche (volontairement non redéployé aujourd’hui).
- Les modules « complets » au-delà de la base r19040 + ceux déjà récupérés (`factor.fr`, etc.) restent le flux `modupdate` (D16), pas un second installeur.
- La recette 200 modules / charge (issue #6) n’est pas dans le périmètre.

Proposition : laisser l’issue ouverte jusqu’à livraison effective d’un arbre Windows reconstruit depuis `feat/algogebra-json-hook` sur le runtime de recette, puis la fermer.

---

## Status as of 6 November 2026 — recipe runtime tree (including JSON hook)

This issue asked for a source and a Windows deployment path for `public_html` / modules. Since the 27/10 reopen (svn:special links, helpers, `sh.exe`, `wims.conf`, YAML, indexes):

**Source (D15 / D16)**  
SVN trunk `https://subversion.renater.fr/anonscm/svn/wimsdev/trunk/wims` revision **19040** (WIMS 4.33a). Initial modules = SVN export; later updates via `modupdate` (not Debian packages).

**Build**  
`tools/build-runtime-tree.sh <svn-export-r19040> [workdir]` produces:

- Linux compile tree `$workdir/wims`;
- Windows tree `$workdir/wims-runtime` and archive `$workdir/wims-runtime.tgz` (no ELF, Windows binaries, MSYS2 `sh.exe`, `wims.conf`, `wims4win.yaml`, `mkindex`, translator/webget/oef2wims helpers, flydraw/insdraw).

The already-validated Grok runtime on the PC (`Reprise_Wims_Grok/wims-runtime-r19040-grok`) covers the 27/10 checklist (fr/en home, `adm/doc`, `factor.fr` via webget, `wcalc.fr`, mkindex).

**AlgoGebra JSON hook (new, branch `feat/algogebra-json-hook`)**  
The hook is no longer a manual patch. `build-runtime-tree.sh` **always** installs it (vendored under `tools/algogebra-hook/`) into both Linux and Windows trees: `public_html/scripts/oef/algogebra_json.phtml` plus injection into `answer.phtml`, with backup `answer.phtml.bak_pre_algogebra`. Opt-out: `ALGOGEBRA_HOOK=0`. Install/uninstall docs: `docs/09_Hook_JSON_AlgoGebra.md`.

For OEF hook acceptance tests, the delivered tree plus a module from `modules/template/oef.fr` (e.g. `H6/test/oef2html.fr`) and `oef2wims` builds of `00006.oef` / `00076.oef` are enough. No second SVN export is required.

**Still out of scope for closing this issue without agreement**

- Publishing or copying onto the PC a **new** `wims-runtime.tgz` built from this branch (intentionally not redeployed yet).
- “Full” modules beyond the r19040 base + already fetched ones (`factor.fr`, etc.) remain the `modupdate` flow (D16), not a second installer.
- The 200-module / load recipe (issue #6) is out of scope.

Proposal: keep the issue **open** until a Windows tree rebuilt from `feat/algogebra-json-hook` is actually delivered onto the recipe runtime, then close.

---
