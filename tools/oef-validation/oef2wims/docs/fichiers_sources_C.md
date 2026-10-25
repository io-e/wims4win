# Sources C de oef2wims (WIMS 4.31b) — guide pour un portage Python

Liste dérivée de la compilation réelle : règle `oef2wims` du Makefile (`src/OefMsg2wims/Makefile.in` : `oefO=oeflines.o compare.o outexec.o process.o sp.o oef2wims.o` + `-lwims` = objets de `src/Lib`) et dépendances `gcc -MM` (en-têtes système exclus). Copies inchangées dans `sources_oef2wims/`.

> Note : le lien prend toute la bibliothèque libwims ; tous ses fichiers ne sont pas forcément sollicités par oef2wims (à l'exécution, oef2wims ne fait qu'une traduction de texte, sans évaluer de mathématiques).

## Programme oef2wims (src/OefMsg2wims/)
| Fichier | Lignes | Rôle |
|---|---|---|
| OefMsg2wims/oef2wims.c | 445 | `main()`, table des directives OEF (\title, \answer…), lecture du .oef, découpage en directives, écriture du .def |
| OefMsg2wims/oef2wims.h | 91 | Déclarations communes (types de paramètres, struct param, prototypes) |
| OefMsg2wims/process.c | 708 | Traitement de chaque directive `p_*` (statement, answer, choice, real, if/while/for…) → lignes !set du .def |
| OefMsg2wims/oeflines.c | 249 | Utilitaires : `oef_error`, substitution des variables \x, `checkparentheses`, `find_matching2` |
| OefMsg2wims/outexec.c | 232 | Traduction des commandes exécutables (`out_exec`, if/for, formules) |
| OefMsg2wims/compare.c | 137 | Vérification syntaxique des conditions de comparaison (`check_compare`) |
| OefMsg2wims/sp.c | 384 | Fonctions spéciales (evalue, solve, diff, maxima, pari, texmath, …) |
| OefMsg2wims/sp.h | 45 | Table des fonctions spéciales |

## Bibliothèque libwims (src/Lib/)
| Fichier | Lignes | Rôle |
|---|---|---|
| Lib/libwims.h | 244 | En-tête de la bibliothèque (prototypes, macros) |
| Lib/libwims.c | 32 | Point d'agrégation de la bibliothèque |
| Lib/liblines.c | 922 | Manipulation de chaînes/lignes : `find_word_start`, `find_matching`, `search_list`, `verify_order`, `xmalloc`, `filelength`… |
| Lib/text.c | 429 | Fonctions de traitement de texte WIMS |
| Lib/evalue.c | 596 | Évaluateur d'expressions numériques |
| Lib/math.c | 92 | Fonctions mathématiques auxiliaires |
| Lib/levelcurve.c | 129 | Courbes de niveau (lié, non utilisé par la traduction) |
| Lib/mt19937ar.c | 170 | Générateur aléatoire Mersenne Twister |
| Lib/dir.c | 60 | Création de répertoires (`mkdirs`) |

## En-têtes généraux (src/)
| Fichier | Lignes | Rôle |
|---|---|---|
| includes.h | 91 | Inclusions système communes |
| wimsdef.h | 120 | Constantes WIMS (tailles de buffers…) |
| config.h | 301 | **Généré** par autoheader+configure (absent de l'archive) |

## Ordre d'appel (pour le portage)
1. `main(argc, argv)` (oef2wims.c) — `oef2wims table` → `verify_order` sur les tables puis « Table orders OK. ». Sinon : sortie = argv[2] ou nom.oef → nom.def ; lit `$oef2wims_mdef`.
2. `printf("%s..", fichier)` puis `getinp()` : lecture du fichier (≤ 199 998 octets, sinon erreur).
3. `checkparentheses(inpbuf,1)` (oeflines.c).
4. Test d'ouverture du fichier de sortie, puis initialisation de param[1..5] (imagedir, confparm1-4).
5. `process()` (oef2wims.c) : repère chaque `\nom{…}{…}`, cherche la directive par `search_list`, découpe les paramètres (`find_matching`) et les range dans define[].
6. `output()` : écrit l'en-tête du .def puis `_out(type)` par catégorie, qui appelle les `p_*` de process.c ; ceux-ci appellent `subst/repsubst`, `out_exec`, `check_compare`, fonctions `sp_*`.
7. `printf(" -> %s\n", sortie)`, code 0. Toute erreur : `oef_error()` → « ERROR … » sur stdout, `exit(1)`.
