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
