/* mkindex.c : portage C fidèle de scripts/docu/mkindex (WIMS r19040), hors classes.
   Ordre des fichiers : octet par octet (équivaut à LC_ALL=C pour le script sh). */
#include "mkindex.h"
#include "util.h"
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#ifdef _WIN32
# include <direct.h>
#else
# include <unistd.h>
#endif

/* `grep msgprim allmsgprim | awk -F= '{print $2}'`, lignes jointes par \n */
static char *primitives(const char *wh) {
  char *p = xprintf("%s/public_html/scripts/primitives/allmsgprim", wh);
  char *b = read_file(p, NULL), *line, *res = xstrdup(""); free(p);
  if (!b) return res;
  for (line = b; *line; ) {
    char *nl = strchr(line, '\n'), *e;
    if (nl) *nl = 0;
    if (strstr(line, "msgprim") && (e = strchr(line, '='))) {
      char *f = e + 1, *g = strchr(f, '='), *t;
      size_t n = g ? (size_t)(g - f) : strlen(f);
      t = *res ? xprintf("%s\n%.*s", res, (int)n, f) : xprintf("%.*s", (int)n, f);
      free(res); res = t;
    }
    if (!nl) break; line = nl + 1;
  }
  free(b); return res;
}

/* awk '/^!if/ {exit}; {print}' a > b */
static void head_until_if(const char *a, const char *b) {
  size_t n; char *s = read_file(a, &n), *p; FILE *f = fopen(b, "wb");
  if (!f) { free(s); return; }
  if (s) {
    for (p = s; *p; ) {
      char *nl = strchr(p, '\n'); size_t l = nl ? (size_t)(nl - p) : strlen(p);
      if (!strncmp(p, "!if", 3)) break;
      fwrite(p, 1, l, f); fputc('\n', f);
      if (!nl) break; p = nl + 1;
    }
  }
  fclose(f); free(s);
}

static int ends_with(const char *s, const char *suf) { size_t a = strlen(s), b = strlen(suf); return a >= b && !strcmp(s + a - b, suf); }

static void clean_doc(const char *moddir, const char *def, const char *src, FILE *out) {
  int n, i; char *d = pjoin(moddir, def); char **l = list_dir(d, 0, &n);
  for (i = 0; i < n; i++) {
    if (!ends_with(l[i], ".def")) continue;
    { char *dd = xprintf("%.*s", (int)(strlen(l[i]) - 4), l[i]);
      char *sp = xprintf("%s/%s/%s", moddir, src, dd);
      if (!is_file(sp)) { char *f = pjoin(d, l[i]); fprintf(out, "Removing %s/%s.\n", def, l[i]); remove(f); free(f); }
      free(dd); free(sp); }
  }
  free_list(l, n); free(d);
}

static void clean_src(const char *moddir, const char *def) {
  char *d = xprintf("%s/%s/.src", moddir, def); int n, i; char **l = list_dir(d, 0, &n);
  for (i = 0; i < n; i++) { char *f = pjoin(d, l[i]); if (!is_dir(f)) remove(f); free(f); }
  free_list(l, n);
  rmdir(d);
  free(d);
}

static const char FOOTER[] = "!if $wims_read_parm!=$empty\n  !goto $wims_read_parm\n!endif\n!exit\n\n:content\n";

static void gen_doc(const char *moddir, const char *def, const char *src, const char *wh,
                    const char *perl, const char *msg2wims, const char *logfile) {
  int n, i; char *sd = pjoin(moddir, src); char **l = list_dir(sd, 0, &n);
  char *tmp = pjoin(moddir, "tmp");
  char *conv = xprintf("%s/public_html/scripts/docu/conversion.pl", wh);
  for (i = 0; i < n; i++) {
    const char *f = l[i]; size_t fl = strlen(f);
    char *dd, *dh, *srcf, *deff;
    if (fl >= 3 && !strcmp(f + fl - 2, "hd")) continue;  /* grep -v '.hd$' */
    dd = xprintf("%s/%s/%s.def", moddir, def, f);
    dh = xprintf("%s/%s/%s.hd", moddir, src, f);
    srcf = xprintf("%s/%s", src, f);
    deff = xprintf("%s/%s/%s.def", moddir, def, f);
    if (!is_file(dh)) head_until_if(dd, dh);
    if (!is_file(dd) || newer(xprintf("%s/%s", moddir, srcf), dd)) {
      remove(dd);
      { const char *a[] = { msg2wims, srcf, "tmp", NULL }; run(a, moddir, logfile); }
      { size_t hn; char *h = read_file(dh, &hn); write_file(dd, h ? h : "", h ? hn : 0); free(h); }
      { const char *a[] = { perl, conv, "tmp", NULL }; run(a, moddir, logfile); }
      { FILE *o = fopen(dd, "ab"); if (o) { fputs(FOOTER, o); fclose(o); } }
      append_file(dd, tmp);
    }
    free(dd); free(dh); free(srcf); free(deff);
  }
  remove(tmp);
  free_list(l, n); free(sd); free(tmp); free(conv);
}

static void index_file(const char *moddir, const char *def) {
  char *d = pjoin(moddir, def), *ix = pjoin(d, ".index"); int n, i; char **l = list_dir(d, 0, &n);
  FILE *o = fopen(ix, "wb");
  if (!o) { free_list(l, n); free(d); free(ix); return; }
  fputc('\n', o);
  for (i = 0; i < n; i++) {
    char *p, *b, *line, *tit = NULL;
    if (!ends_with(l[i], ".def")) continue;
    p = pjoin(d, l[i]); b = read_file(p, NULL); free(p);
    if (!b) continue;
    for (line = b; *line; ) {      /* awk -F= '/titb.*=/ {print $2; exit}' */
      char *nl = strchr(line, '\n'), *t;
      if (nl) *nl = 0;
      if ((t = strstr(line, "titb")) && strchr(t, '=')) {
        char *e = strchr(line, '='), *g = strchr(e + 1, '=');
        tit = xprintf("%.*s", (int)(g ? (size_t)(g - e - 1) : strlen(e + 1)), e + 1);
        break;
      }
      if (!nl) break; line = nl + 1;
    }
    if (tit) { size_t k = strlen(tit); while (k && tit[k-1] == '\n') tit[--k] = 0; }  /* `...` */
    if (tit && *tit) fprintf(o, ":%.*s\n%s\n", (int)(strlen(l[i]) - 4), l[i], tit);
    free(tit); free(b);
  }
  fclose(o); free_list(l, n); free(d); free(ix);
}

static char *trim(char *s) { char *e; while (*s == ' ' || *s == '\t' || *s == '\r') s++; e = s + strlen(s); while (e > s && (e[-1] == ' ' || e[-1] == '\t' || e[-1] == '\r')) *--e = 0; return s; }

static int mk_doc(const char *moddir, const char *l, const char *wh, const char *perl,
                  const char *msg2wims, FILE *out, const char *logfile) {
  char *def = xprintf("doc/%s", l), *src = xprintf("%s/src", def), *p;
  p = pjoin(moddir, def);
  if (!is_dir(p)) { fprintf(out, "Directory %s not found.\n", def); free(def); free(src); def = xstrdup("."); src = xstrdup("./src"); }
  free(p); p = pjoin(moddir, src);
  if (!is_dir(p)) { fprintf(out, "Directory %s not found.\n", src); free(p); free(def); free(src); return -1; }
  free(p);
  /* latex2wims : chaque ligne « sous-dossier fichier » de doc/srctex/0index */
  { char *ix = xprintf("%s/doc/srctex/0index", moddir), *b = read_file(ix, NULL);
    char *sdir = xprintf("%s/doc/srctex", moddir);
    char *pl = xprintf("%s/public_html/modules/adm/latex2wims/latex2wims.pl", wh);
    if (b) {
      char *line = b;
      while (*line) {
        char *nl = strchr(line, '\n'), *t, *w1, *w2, *sp;
        if (!nl) break;                    /* `read` ignore une dernière ligne sans \n */
        *nl = 0; t = trim(line);
        if (*t) {
          char *a1, *a2, *a3;
          w1 = t; sp = strpbrk(t, " \t");
          if (sp) { *sp = 0; w2 = trim(sp + 1); sp = strpbrk(w2, " \t"); if (sp) *sp = 0; } else w2 = "";
          a1 = xprintf("--subdir=%s", w1); a2 = xprintf("--docdir=%s", moddir); a3 = xprintf("%s.tex", w2);
          { const char *a[] = { perl, pl, a1, a2, "--embed=document", a3, NULL }; run(a, sdir, logfile); }
          free(a1); free(a2); free(a3);
        }
        line = nl + 1;
      }
    }
    free(ix); free(b); free(sdir); free(pl); }
  clean_doc(moddir, def, src, out);
  clean_src(moddir, def);
  gen_doc(moddir, def, src, wh, perl, msg2wims, logfile);
  index_file(moddir, def);
  free(def); free(src); return 0;
}

int mkindex(const char *moddir, const char *wh, const char *perl, const char *msg2wims, FILE *out, const char *logfile) {
  char *prim = primitives(wh), *doclist = NULL, *p; int rc = 0;
  set_env("w_msg2wims_primitives", prim); free(prim);
  set_env("wims_home", wh);
  unset_env("wims_exec_parm");   /* lu par latex2wims.pl */
  /* latex2wims.pl calcule prev/next de certains blocs en parcourant un hachage : l'ordre
     aléatoire de perl rend la sortie non reproductible. Graine fixe (sauf si déjà fixée). */
  if (!getenv("PERL_HASH_SEED")) { set_env("PERL_HASH_SEED", "0"); set_env("PERL_PERTURB_KEYS", "0"); }
  p = xprintf("%s/doc/.def", moddir);
  if (is_file(p)) {
    char *vp = xprintf("%s/var.proc", moddir), *b = read_file(vp, NULL), *line;
    if (b) for (line = b; *line; ) {
      char *nl = strchr(line, '\n'); if (nl) *nl = 0;
      if (!strncmp(line, "doclist=", 8)) { char *e = strchr(line + 8, '='); doclist = xprintf("%.*s", (int)(e ? (size_t)(e - line - 8) : strlen(line + 8)), line + 8); break; }
      if (!nl) break; line = nl + 1;
    }
    free(vp); free(b);
  }
  free(p);
  if (!doclist || !*trim(doclist)) { free(doclist); doclist = xstrdup("1"); }
  { char *save = doclist, *tok;
    for (tok = strtok(save, " \t\r"); tok; tok = strtok(NULL, " \t\r")) {
      if (mk_doc(moddir, tok, wh, perl, msg2wims, out, logfile) != 0) { rc = -1; break; }
      fprintf(out, "\n%s done\n", tok);
    } }
  free(doclist); return rc;
}
