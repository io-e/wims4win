# latex2wims-doc : document LaTeX (wims.sty) → document WIMS, hors de l'application

Outil autonome qui reprend la chaîne amont de WIMS sans passer par Modtool
ni par le module `adm/latex2wims` :

```
fichier.tex (+ wims.sty)
  └─ modules/adm/latex2wims/latex2wims.pl   (Perl, amont)  → doc/1/src/*  + *.hd
       └─ scripts/docu/mkindex              (sh, amont)    → msg2wims + conversion.pl
            └─ doc/1/*.def + doc/1/.index                  → servi par le moteur
                 (module de catégorie « document », scripts/docu)
```

Aucune distribution TeX n'est nécessaire : `latex2wims.pl` lit le source
LaTeX lui-même. `wims.sty` sert seulement à compiler le même `.tex` en PDF
(MiKTeX ou TeX Live, facultatif).

## Utilisation

Linux :

```sh
sh tools/latex2wims/latex2wims-doc.sh --wims-home /chemin/wims \
   --tex cours.tex --module local/cours.fr --author "Nom" --email adr@exemple.fr
```

Windows (pas de cmd.exe ni de .bat ; sh.exe et perl MSYS2 du runtime) :

```powershell
$rt='D:\...\wims-runtime-r19040-grok'
$env:PATH="$rt\other\bin;$rt\other\msys2\usr\bin;C:\Windows\System32"
& "$rt\other\msys2\usr\bin\sh.exe" -c "sh /d/.../tools/latex2wims/latex2wims-doc.sh --wims-home /d/.../wims-runtime-r19040-grok --tex /d/.../cours.tex --module local/cours.fr"
```

Le script :
1. copie `modules/template/doc.<langue>` dans `public_html/modules/<module>` (remplacé s'il existe) ;
2. place le `.tex`, les fichiers voisins (images, css) et `wims.sty` dans `doc/srctex/`, écrit `doc/srctex/0index` ;
3. convertit un source UTF-8 en windows-1252 (jeu de caractères de WIMS) ;
4. écrit `doc/1/.def` (titre, auteur), normalement écrit par `adm/doc` ;
5. lance `scripts/docu/mkindex` (qui appelle `latex2wims.pl`, `msg2wims`, `conversion.pl`) ; journal dans `doc/latex2wims.log` ;
6. affiche l'URL : `wims.cgi?module=<module>&cmd=new&lang=fr` ; un bloc : `&doc=1&block=mainS1`.

Le module n'apparaît dans la recherche de WIMS qu'après `bin/mkindex` (facultatif pour l'affichage).

## Test

```sh
sh tools/latex2wims/test.sh /chemin/wims      # Linux : public_html/wims ; Windows : wims.cgi.exe
```

Génère `exemple.tex`, lit trois blocs par le CGI et vérifie : statut 200,
page complète, texte attendu, formules en MathML (`<math`, `mfrac`), puis
supprime le module de test.

Les formules sortent en MathML si `default_texposition=2` est dans
`log/wims.conf` (déjà le cas du runtime wims4win) ; sinon WIMS produit des
images (`instex`).

## Limites

- Commandes LaTeX reconnues : celles de `latex2wims.pl` et de `wims.sty`
  (voir `modules/adm/latex2wims/latex2wimsdoc.pdf`) ; les paquets inconnus sont ignorés.
- `\email{}` absent : simple avertissement. `style.css` absent : avertissement.
- Un seul document (`doc/1`) par module.
