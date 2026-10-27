# latex2wims : document LaTeX (wims.sty) → document WIMS, application autonome

Application en ligne de commande, **hors de l'application WIMS**, qui transforme un
fichier `.tex` (avec `wims.sty`) en module document WIMS (`doc/1/*.hd`, `*.def`,
`.index`), l'installe dans un runtime et le vérifie par le vrai `wims.cgi`.
Mêmes sources C pour Linux (gcc) et Windows (mingw-w64, exécutable natif, sans WSL,
sans `.bat` ni `cmd.exe`).

```
fichier.tex (+ wims.sty, images, css)
 └─ latex2wims (C)  : options, copie du modèle doc.<langue>, UTF-8 → windows-1252
    │                  (diagnostic ligne:colonne), INDEX, doc/1/.def, 0index
    ├─ mkindex (C)  : portage fidèle de scripts/docu/mkindex (parité octet par octet testée)
    │   ├─ perl latex2wims.pl   (amont, NON modifié)   → doc/1/src/*, *.hd
    │   ├─ bin/msg2wims         (amont, déjà porté)    → corps des blocs
    │   └─ perl conversion.pl   (amont)                → nettoyage HTML
    └─ check (C)    : wims.cgi(.exe) du runtime → 200, titre, contenu, MathML, pas d'erreur WIMS
```

## Choix d'architecture (justification)

- **`latex2wims.pl` gardé tel quel** (≈1550 lignes de Perl, expressions régulières sur
  le LaTeX) : le réécrire coûterait cher et créerait une divergence avec l'amont.
  Il est lancé par `perl` : sous Windows le perl MSYS2 du runtime
  (`<wims-home>/other/msys2/usr/bin/perl.exe`). Remarque : `latex2wims.pl` appelle
  lui-même `system("mkdir -p …; mv …")` et `` `cat …` `` ; sous Windows ce sont le
  `sh`/coreutils MSYS2 livrés avec ce perl qui servent, pas `cmd.exe`.
- **`mkindex` réécrit en C** (petit script sh/awk) : plus de dépendance à `sh` dans
  notre chaîne, et parité octet par octet vérifiée contre le script amont.
- **Pilote C** : lancement des processus par `fork/execvp` (Linux) et `CreateProcess`
  (Windows, guillemets selon les règles du CRT), sortie des sous-processus dans
  `doc/latex2wims.log`.
- Graine de hachage perl fixée (`PERL_HASH_SEED=0`, sauf si déjà définie) :
  `latex2wims.pl` calcule `prev`/`next` de certains blocs (sous-sous-sections) en
  parcourant un hachage, sa sortie changeait d'une exécution à l'autre.

## Compilation

```sh
cd tools/latex2wims
make            # build/latex2wims          (Linux, gcc)
make win32      # build/win64/latex2wims.exe (x86_64-w64-mingw32-gcc, statique)
```

## Utilisation

```sh
latex2wims install --wims-home /chemin/wims --tex cours.tex [--module local/cours.fr] \
           [--lang fr] [--title T] [--author A] [--email E] [--url http://hote/wims/wims.cgi]
latex2wims check   --wims-home /chemin/wims --module local/cours.fr [--expect TEXTE]...
latex2wims all     ...                    # install puis check
latex2wims convert --wims-home /chemin/wims --tex cours.tex --out DOSSIER [--no-index]
latex2wims mkindex --wims-home /chemin/wims [DOSSIER_MODULE]
latex2wims hash    DOSSIER [--wims-home W] # empreintes normalisées d'un arbre
latex2wims corpus  --wims-home W --corpus corpus --out rapport.tsv [--ref ref.tsv]
```

Options communes : `--log FICHIER` (journal ajouté, sortie des sous-processus
comprise), `--trace` (chaque commande lancée, son dossier, les chemins trouvés),
`--strict` (un caractère non représentable en windows-1252 est une erreur),
`--perl PERL` (sinon `$L2W_PERL`, puis le perl MSYS2 du runtime, puis le `PATH`).

Windows (PowerShell, rien d'autre) :

```powershell
$rt='D:\...\Reprise_Wims_Grok\wims-runtime-r19040-grok'
.\latex2wims.exe all --wims-home $rt --tex D:\...\cours.tex --module local/cours.fr --log l2w.log
```

### Codes de sortie

| code | sens |
|---|---|
| 0 | succès |
| 1 | vérification `wims.cgi` en échec, ou parité de corpus différente |
| 2 | usage : option invalide, `.tex` absent |
| 3 | runtime incomplet : `latex2wims.pl`, `msg2wims`, perl, modèle ou `wims.cgi` absent |
| 4 | conversion : aucun `.def` produit, ou caractère non représentable avec `--strict` |
| 5 | entrée/sortie (copie, écriture) |

### Encodage

WIMS sert en windows-1252. Un source UTF-8 valide contenant des caractères
non-ASCII est converti ; chaque caractère hors windows-1252 est signalé :

```
cours.tex:11:45: caractère U+03B1 (α) non représentable en windows-1252, remplacé par ?
```

Un source déjà en latin-1/windows-1252 (ex. `fichier_test.tex` amont) est copié tel quel.
Le titre, l'auteur, l'adresse et la description de `INDEX` sont aussi écrits en
windows-1252 (l'ancien script laissait la description en UTF-8).

### Vérification `check`

Pour le bloc `main` et chaque bloc de `doc/1/.index`, lance `wims.cgi` (ou `wims`)
du runtime dans `public_html` avec un environnement CGI (`cmd=new&doc=1&block=…`) et
exige : statut 200, `</html>`, pas de « existe pas » ni « WIMS User Error » ; le titre
dans `main` ; chaque `--expect` dans au moins un bloc ; au moins un `<math` si le
source contient des formules (`default_texposition=2` dans `log/wims.conf`).
Message explicite si WIMS se dit surchargé (machine chargée : ajouter
`threshold1=100000` et `threshold2=200000` à `log/wims.conf`).

## Choix provisoires (questions ouvertes)

Faciles à changer (`DEFAULT_MODULE_ZONE`, `DEFAULT_LANG` dans `src/latex2wims.c`, ou options) :
emplacement `modules/local/<nom>.<langue>` ; un document et une langue par module
(`doc/1`) ; pas de PDF (MiKTeX/TeX Live non appelés) ; `bin/mkindex` global non lancé
(le module s'affiche mais n'apparaît dans la recherche qu'après lui).

## Tests

```sh
make test WIMS_HOME=/chemin/runtime     # séquentiel
```

`tests/run_tests.sh` :
1. `tests/mkindex_parity.sh` : pour chaque document du corpus, `mkindex` sh amont puis
   portage C dans le même dossier, `diff -r` (time() de perl figé par
   `tests/perl/L2wFixTime.pm` pour la date `datm`) ;
2. `latex2wims corpus` : conversion du corpus, empreintes comparées à `parity/ref_linux.tsv` ;
3. `latex2wims all` sur chaque document : `wims.cgi` (200, titre, contenu, MathML) ;
4. codes de sortie 1, 2, 3, 4.

Corpus (`corpus/<nom>/<nom>.tex`, un dossier par document car les fichiers voisins
sont copiés) : `exemple`, `fichier_test` (amont, latin-1), `environnements`
(théorèmes, `proof`, plis, listes, `tabular`, `\exercise`, `center`, `quote`,
`latexonly`/`wimsonly`), `sections_maths` (sous-sous-sections, `pmatrix`, `equation`),
`accents_cp1252` (Œ, €, guillemets, caractères hors windows-1252).

### Parité Linux / Windows

Empreinte normalisée d'un arbre (`hash`, `corpus`) : SHA-256 par fichier, fin de
ligne CRLF → LF, `doc/latex2wims.log` ignoré, chemins absolus du module et du runtime
remplacés par `@MOD@`/`@WH@`, ligne `!set datm=` (date) neutralisée. Sous Windows :

```powershell
.\latex2wims.exe corpus --wims-home $rt --corpus ..\corpus --out win\windows.tsv --ref ..\parity\ref_linux.tsv --log win\corpus.log
```

Le rapport TSV donne, par document : statut (`identique`/`DIFFERENT`), nombre de blocs,
nombre de fichiers, empreinte ; `trees/<nom>.sha256` détaille fichier par fichier
(comparer avec `latex2wims hash` côté Linux pour trouver le fichier qui diffère).

## Ancien script

`latex2wims-doc.sh` et `test.sh` (version sh de la même chaîne) restent pour
comparaison ; l'application C les remplace.
