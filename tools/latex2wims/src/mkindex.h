#ifndef L2W_MKINDEX_H
#define L2W_MKINDEX_H
#include <stdio.h>
/* Portage C de public_html/scripts/docu/mkindex (cas hors classe) :
   moddir = dossier du module (le « pwd » du script sh), wh = wims_home,
   perl = interpréteur pour latex2wims.pl et conversion.pl, out = sortie « écho » du sh,
   logfile = sortie des sous-processus (NULL : héritée). Retourne 0, ou -1 si src absent. */
int mkindex(const char *moddir, const char *wh, const char *perl, const char *msg2wims, FILE *out, const char *logfile);
#endif
