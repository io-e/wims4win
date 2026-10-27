/* util.h : fichiers, processus, journal — portable Linux (gcc) / Windows (mingw-w64). */
#ifndef L2W_UTIL_H
#define L2W_UTIL_H
#include <stdio.h>
#include <stddef.h>

/* codes de sortie documentés (README) */
#define EX_OK      0  /* succès */
#define EX_CHECK   1  /* vérification wims.cgi ou parité en échec */
#define EX_USAGE   2  /* options invalides, fichier .tex absent */
#define EX_ENV     3  /* runtime incomplet : perl, latex2wims.pl, modèle, msg2wims, wims.cgi */
#define EX_CONV    4  /* conversion en échec : aucun .def, ou caractère non représentable (--strict) */
#define EX_IO      5  /* erreur d'entrée/sortie (écriture, copie) */

extern int g_trace;
extern FILE *g_log;
void msg(const char *fmt, ...);    /* stderr + journal */
void trace(const char *fmt, ...);  /* seulement si --trace */
void die(int code, const char *fmt, ...);

char *xstrdup(const char *);
char *xprintf(const char *fmt, ...);
char *pjoin(const char *a, const char *b);
void slashify(char *p);            /* \ -> / */
char *abspath(const char *p);      /* chemin absolu, séparateurs / */
int is_dir(const char *p);
int is_file(const char *p);
int newer(const char *a, const char *b); /* [ a -nt b ] */
int mkdir_p(const char *p);
int rm_rf(const char *p);
int copy_file(const char *src, const char *dst);
int copy_tree(const char *src, const char *dst);
/* entrées d'un dossier (sans . ni ..), triées octet par octet ; hidden=0 : sans les fichiers cachés */
char **list_dir(const char *p, int hidden, int *n);
void free_list(char **l, int n);
char *read_file(const char *p, size_t *len);
int write_file(const char *p, const char *data, size_t len);
int append_file(const char *dst, const char *src);
void set_env(const char *k, const char *v);
void unset_env(const char *k);
/* lance argv[0] (cherché dans le PATH si sans /), dans cwd, sortie+erreurs ajoutées à outfile
   (NULL = héritées). Retourne le code de sortie, -1 si lancement impossible. */
int run(const char *const *argv, const char *cwd, const char *outfile);
char *which(const char *prog);
#endif
