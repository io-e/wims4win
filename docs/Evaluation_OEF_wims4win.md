# Évaluation de wims4win pour oef2html (OEF_VALIDATION) — 2026-10-25

## 1. oef2html.py sur le PC avec wims4win
- Copie à nous de `dist/html/oef2html.py` + patch `tools/oef2html-wims4win.patch` (options `--cgi`, `--cgi-prefix wine`, dump `OEF2HTML_DUMP`). Rien modifié dans /workspace/oefc ni D:\ALF_Scripts.
- PC : `Reprise_Wims_Grok\oef-wims4win` (script + échantillon), runtime copié `Reprise_Wims_Grok\wims-runtime-oef` (+ `log/wims.conf` : `default_texposition=2`), oef2wims.exe de D:\ALF_Scripts (exécuté, non modifié).
- Échantillon : 10 fichiers (00001 00006 00013 00076 00347 00352 00403 00509 00726 01012), graine 1.
- Résultat PC natif (Python 3.12, sans Wine, 23 s) : 5 pages ; 00006, 00013, 00076, 00726 **identiques** à la box Linux 4.31b (après normalisation dates/ids de session/ids MathML/taille MathML) ; 00001 identique sauf une image `insdraw` (« Fail ») ; échecs : 00347/00352 too_many_gotos (aussi sous Linux), 01012 bad_index (aussi sous Linux), 00403/00509 `bad_cmd pari` (PARI/GP absent du PC ; Linux : 00403 OK, 00509 too_many_gotos).

### Formule non affichée — causes (deux)
1. **Configuration** : le runtime d'OEF_VALIDATION fixe `default_texposition=2` (MathML) dans `log/wims.conf` ; sans cela (wims4win par défaut, ou son test Wine) WIMS prend la voie « TeX → GIF » : `bin/insdraw..processor` est un script sh + flydraw ELF → rien sous Windows → formule vide. Avec texposition=2, wims_mathml.exe est utilisé : MathML identique à Linux.
2. **Bug wims4win (corrigé)** : CRT en mode texte → fichiers écrits en CRLF ; `readfile()` compare read()/st_size et rejette → `var.stat` perdu (pas de `wims_sesrandom`) → `cmd=reply` relancé en `cmd=new` → pas de correction. Fix : `_set_fmode(_O_BINARY)` dans `wp_cgi_prepare()`.
- Ce n'est pas un écart 4.31b/4.33a. Écart résiduel cosmétique : `mathsize` 120 % (Windows) vs 110 % (Linux), dû à la réécriture Win32 du choix de taille dans mathml.c (N3) ; à aligner.
- Manques Windows pour le corpus : flydraw/insdraw (images `\draw`), PARI/GP (`pari_command`), éventuellement autres CAS.

## 2. Écart 4.31b → 4.33a utile pour OEF
- Moteur `src/*.c` : ~110 lignes dans 10 fichiers (lines.c 32, calc.c 24, auth.c 18, html.c 13, evalue.c 10, exec 4, log 3, score 3, compare 2, wims 2) ; oef2wims (OefMsg2wims/def.c) 2 lignes ; Lib 4 ; Interfaces 2 ; Wimslogd 6 ; Texgif 158 ; Flydraw 40 ; Misc/authors = données (545 fichiers).
- Scripts OEF (4.31b → 4.32, pas d'arbre public 4.33a hors SVN) : scripts/oef 4 fichiers / 11 lignes (draw.phtml, mkindex, printanalysis.proc, var.proc), anstype/fill.inc 15 lignes ; modules/template/oef.fr identique.
- Patch wims4win (hors Platform/, ~2 400 lignes nouvelles) : 420 lignes dans 15 fichiers ; recouvrement avec le delta amont : 5 fichiers. `patch --dry-run` sur src 4.31b : **1 hunk rejeté sur ~100** (Interfaces/common.c).
- Estimation rebasage wims4win sur 4.31b : 1 à 2 jours (application + 1 conflit + rebuild + rejouer tests Platform/équivalence CAS + parité oef2html sur le corpus).

## 3. too_many_gotos (58) — analyse sans correction
- GOTO_LIMIT=500000 ; les 58 viennent de boucles `\while` de rejet qui ne terminent pas.
- 00352 (graine 1) : `\while{\x2o22 == 1}` dont le corps ne contient que « NON TRADUIT ‹procedures_callnoreturn› : procédure récursive « Déterminer les coefficients » » + recalcul de la condition → aucun nouveau tirage → boucle infinie dès que le 1er tirage est rejeté.
- 00347 (graine 1) : nombres jusqu'à 10^12 en `\integer` ; WIMS les affiche `3.9810717e+09` (8 chiffres significatifs) ; le découpage en chiffres compte e, +, . et perd les zéros → la condition « ≥ p % de zéros » n'est presque jamais satisfaite. (`!randint 3981071705,999999999999` produit même une valeur invalide.)
- Classement heuristique des 58 : ~33 procédure récursive non traduite dans le `\while` (pas de re-tirage) ; ~11 autre bloc non traduit dans le `\while` ; ~8 grands entiers > 1e8 ; ~7 tirage de rejet trop improbable / à examiner (00498, 00608, 00690, 01009, 01116…).
- Correction côté xml2oef : traduire l'appel récursif en re-tirage dans le corps du `\while` ; manipuler les grands entiers en texte (ou via pari) ; borne d'itérations.

## 4. Suite (décisions d'Arnaud, 2026-10-25)
- Pas de rebasage sur 4.31b : wims4win reste en 4.33a. Le commit 8bf7078 de Claude est seulement une doc (`Guide_CGI_Laragon_WIMS_CLAUDE_V1_1.md`) : le vhost Laragon sert notre `wims-runtime`, il n'y a rien à intégrer.
- **PARI/GP** : `gp.exe` présent (`Reprise_Wims_Grok\tests-win\cas\pari`). `log/wims.conf` de `wims-runtime` et `wims-runtime-oef` : `pari_command=D:/ALF_ZGit/43-wims-paris-saclay_fr/Reprise_Wims_Grok/tests-win/cas/pari/gp.exe -f -q -p 150000`. 00403 est OK et **identique à Linux** ; 00509 donne too_many_gotos (comme Linux).
- **flydraw** : `flydraw.exe` est construit par `tools/build-libgd-mingw.sh` (libgd 2.3.3 statique, GIF) puis `tools/build-flydraw-win32.sh`. `insdraw..processor.exe` est natif (`src/Platform/tools/insdraw_win.c`). Corrections moteur :
  - les helpers sans extension sont résolus en `.exe` ;
  - `mv`/`rm` sont exécutés en interne ;
  - `wims_home` et PATH sont corrigés.
  
  00001 est **identique à Linux**, image GIF comprise (sha256 identique).
- Échantillon PC (10 fichiers, graine 1, natif, 22 s) : les 6 pages produites sont identiques à Linux (00001 00006 00013 00076 00403 00726). Les échecs sont les mêmes que sous Linux : 00347, 00352 et 00509 en too_many_gotos, 01012 en bad_index.
- Limites d'insdraw : les animations ne gardent que la 1re image. Il n'y a pas de conversion de format, donc un GIF est livré sous le nom demandé. Il n'y a pas non plus de rendu de texte TTF/freetype.

## 5. Taille des formules (DYS) : `log/wims4win.yaml`
```yaml
formula:
  size: 200%     # 50% à 400%. 200% par défaut ; WIMS amont : 110%
```
- Le fichier est lu par `wims` (mathml.c) avec le chemin relatif `../log/wims4win.yaml`. La forme à plat `formula_size: 200%` est aussi acceptée. En l'absence du fichier ou si la valeur est invalide, la taille est 200 %. Le comportement est le même sous Linux et sous Windows.
- **Origine de l'écart 110 % / 120 %** :
  - Sous Linux, WIMS amont passait `"--tex-size 100%"` comme **un seul** mot argv. wims_mathml ignorait donc l'option et utilisait son défaut interne de 110 %.
  - Le portage Win32 calculait `texsize_list[6]`, soit 120 %, et le passait correctement.
  - Les deux versions passent maintenant `--tex-size <valeur YAML>` en deux mots. Seul ce point change délibérément le binaire Linux, à la demande d'Arnaud.
