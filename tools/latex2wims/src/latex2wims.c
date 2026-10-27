/* latex2wims : application autonome LaTeX (wims.sty) -> document WIMS (doc/ .hd/.def)
   Linux (gcc) et Windows natif (mingw-w64), sans sh, .bat ni cmd.exe à l'exécution.
   Chaîne amont inchangée : modules/adm/latex2wims/latex2wims.pl (perl) et msg2wims ;
   scripts/docu/mkindex est porté en C (mkindex.c). */
#include "util.h"
#include "mkindex.h"
#include "sha256.h"
#include <stdlib.h>
#include <string.h>
#include <time.h>

#define L2W_VERSION "0.1.0"
/* choix provisoires (questions ouvertes d'Arnaud) : modifiables ici ou par option */
#define DEFAULT_MODULE_ZONE "local"      /* modules/local/<nom>.<lang> */
#define DEFAULT_LANG "fr"
#define DEFAULT_URL "http://localhost/wims/wims.cgi"

typedef struct {
  const char *cmd, *wh, *tex, *module, *lang, *title, *author, *email, *url, *out, *perl,
             *log, *corpus, *ref, *from;
  const char *expect[32]; int nexpect;
  int strict, no_index, keep;
} opts;
static opts O;

static void usage(int code) {
  fprintf(code ? stderr : stdout,
"latex2wims " L2W_VERSION " : LaTeX (wims.sty) -> document WIMS\n"
"usage : latex2wims COMMANDE [options]\n"
"  convert  --wims-home DIR --tex F.tex [--out DOSSIER]    arbre module (doc/1/*.def) hors runtime\n"
"  install  --wims-home DIR --tex F.tex [--module local/nom.fr]  convertit dans DIR/public_html/modules/\n"
"  check    --wims-home DIR --module M [--expect TEXTE]...  lit le document via le vrai wims.cgi\n"
"  all      = install + check\n"
"  mkindex  --wims-home DIR [DOSSIER_MODULE]                 portage C de scripts/docu/mkindex\n"
"  hash     DOSSIER                                         empreintes normalisées d'un arbre\n"
"  corpus   --wims-home DIR --corpus DOSSIER --out RAPPORT.tsv [--ref REF.tsv]  parité\n"
"  version\n"
"options : --lang fr --title T --author A --email E --url URL_CGI --perl PERL\n"
"          --log FICHIER --trace --strict (caractère non représentable = erreur)\n"
"          --no-index (convert : sans mkindex)\n"
"codes de sortie : 0 ok, 1 vérification/parité en échec, 2 usage, 3 runtime incomplet,\n"
"                  4 conversion en échec, 5 entrée/sortie\n");
  exit(code);
}

static void parse(int argc, char **argv) {
  int i;
  if (argc < 2) usage(EX_USAGE);
  O.cmd = argv[1]; O.lang = DEFAULT_LANG;
  for (i = 2; i < argc; i++) {
    const char *a = argv[i];
#define ARG(name, field) if (!strcmp(a, name)) { if (++i >= argc) die(EX_USAGE, "%s : valeur manquante", name); O.field = argv[i]; continue; }
    ARG("--wims-home", wh) ARG("--tex", tex) ARG("--module", module) ARG("--lang", lang)
    ARG("--title", title) ARG("--author", author) ARG("--email", email) ARG("--url", url)
    ARG("--out", out) ARG("--perl", perl) ARG("--log", log) ARG("--corpus", corpus) ARG("--ref", ref)
#undef ARG
    if (!strcmp(a, "--expect")) { if (++i >= argc || O.nexpect >= 32) die(EX_USAGE, "--expect"); O.expect[O.nexpect++] = argv[i]; continue; }
    if (!strcmp(a, "--trace")) { g_trace = 1; continue; }
    if (!strcmp(a, "--strict")) { O.strict = 1; continue; }
    if (!strcmp(a, "--no-index")) { O.no_index = 1; continue; }
    if (!strcmp(a, "--keep")) { O.keep = 1; continue; }
    if (!strcmp(a, "-h") || !strcmp(a, "--help")) usage(0);
    if (a[0] != '-' && !O.from) { O.from = a; continue; }
    fprintf(stderr, "latex2wims: option inconnue : %s\n", a); usage(EX_USAGE);
  }
  if (O.log) {
    g_log = fopen(O.log, "ab"); if (!g_log) die(EX_IO, "journal impossible : %s", O.log);
    { time_t t = time(NULL); fprintf(g_log, "=== latex2wims %s %s %s", L2W_VERSION, O.cmd, ctime(&t)); }
  }
}

/* ---------- runtime ---------- */
typedef struct { char *wh, *perl, *msg2wims, *l2wpl, *cgi; } runtime;

static char *find_exe(const char *base) {
  char *e = xprintf("%s.exe", base);
  if (is_file(e)) return e;
  free(e); return is_file(base) ? xstrdup(base) : NULL;
}

static runtime rt_open(int need_cgi) {
  runtime r; char *p;
  memset(&r, 0, sizeof r);
  if (!O.wh) die(EX_USAGE, "--wims-home manquant");
  if (!is_dir(O.wh)) die(EX_ENV, "runtime absent : %s", O.wh);
  r.wh = abspath(O.wh);
  r.l2wpl = xprintf("%s/public_html/modules/adm/latex2wims/latex2wims.pl", r.wh);
  if (!is_file(r.l2wpl)) die(EX_ENV, "latex2wims.pl absent : %s", r.l2wpl);
  p = xprintf("%s/bin/msg2wims", r.wh); r.msg2wims = find_exe(p); free(p);
  if (!r.msg2wims) die(EX_ENV, "msg2wims absent dans %s/bin", r.wh);
  /* perl : --perl, $L2W_PERL, perl MSYS2 du runtime, puis PATH */
  if (O.perl) r.perl = is_file(O.perl) ? abspath(O.perl) : xstrdup(O.perl);
  else if (getenv("L2W_PERL") && *getenv("L2W_PERL")) r.perl = xstrdup(getenv("L2W_PERL"));
  else {
    p = xprintf("%s/other/msys2/usr/bin/perl", r.wh); r.perl = find_exe(p); free(p);
    if (!r.perl) r.perl = which("perl");
  }
  if (!r.perl) die(EX_ENV, "perl introuvable (--perl, L2W_PERL, %s/other/msys2/usr/bin, PATH)", r.wh);
  if (need_cgi) {
    p = xprintf("%s/public_html/wims.cgi", r.wh); r.cgi = find_exe(p); free(p);
    if (!r.cgi) { p = xprintf("%s/public_html/wims", r.wh); r.cgi = find_exe(p); free(p); }
    if (!r.cgi) die(EX_ENV, "wims.cgi(.exe) ou wims absent dans %s/public_html", r.wh);
  }
#ifdef _WIN32
  /* perl MSYS2 : ses DLL (msys-2.0.dll...) doivent être dans le PATH */
  { char *d = xstrdup(r.perl), *s = strrchr(d, '/'); if (!s) s = strrchr(d, '\\');
    if (s) { *s = 0; { char *np = xprintf("%s;%s", d, getenv("PATH") ? getenv("PATH") : ""); set_env("PATH", np); free(np); } }
    free(d); }
#endif
  trace("wims_home=%s perl=%s msg2wims=%s cgi=%s", r.wh, r.perl, r.msg2wims, r.cgi ? r.cgi : "-");
  return r;
}

/* ---------- UTF-8 -> windows-1252 ---------- */
static const unsigned short CP1252_HI[32] = { /* 0x80..0x9F */
  0x20AC,0x0081,0x201A,0x0192,0x201E,0x2026,0x2020,0x2021,0x02C6,0x2030,0x0160,0x2039,0x0152,0x008D,0x017D,0x008F,
  0x0090,0x2018,0x2019,0x201C,0x201D,0x2022,0x2013,0x2014,0x02DC,0x2122,0x0161,0x203A,0x0153,0x009D,0x017E,0x0178 };

static int utf8_next(const unsigned char *s, size_t n, size_t *i, unsigned *cp) {
  unsigned c = s[*i]; int k; unsigned v;
  if (c < 0x80) { *cp = c; (*i)++; return 1; }
  if (c >= 0xC2 && c <= 0xDF) { k = 1; v = c & 0x1F; }
  else if (c >= 0xE0 && c <= 0xEF) { k = 2; v = c & 0x0F; }
  else if (c >= 0xF0 && c <= 0xF4) { k = 3; v = c & 0x07; }
  else return 0;
  { int j; for (j = 1; j <= k; j++) { if (*i + j >= n || (s[*i + j] & 0xC0) != 0x80) return 0; v = v << 6 | (s[*i + j] & 0x3F); } }
  if ((k == 2 && v < 0x800) || (k == 3 && (v < 0x10000 || v > 0x10FFFF)) || (v >= 0xD800 && v <= 0xDFFF)) return 0;
  *i += k + 1; *cp = v; return 1;
}
static int to_cp1252(unsigned cp) {
  int j;
  if (cp < 0x80 || (cp >= 0xA0 && cp <= 0xFF)) return (int)cp;
  for (j = 0; j < 32; j++) if (CP1252_HI[j] == cp) return 0x80 + j;
  return -1;
}
/* 1 si texte UTF-8 valide contenant de l'ASCII étendu */
static int is_utf8_hi(const char *b, size_t n) {
  size_t i = 0; unsigned cp; int hi = 0;
  while (i < n) { if ((unsigned char)b[i] >= 0x80) hi = 1; if (!utf8_next((const unsigned char *)b, n, &i, &cp)) return 0; }
  return hi;
}
/* convertit ; diagnostics « fichier:ligne:colonne » ; retourne le nombre de caractères remplacés par ? */
static int utf8_to_cp1252(const char *name, const char *b, size_t n, char **out, size_t *outn) {
  size_t i = 0, o = 0; unsigned cp; int bad = 0, line = 1, col = 1;
  char *r = malloc(n + 1);
  while (i < n) {
    size_t st = i; int c;
    utf8_next((const unsigned char *)b, n, &i, &cp);
    c = to_cp1252(cp);
    if (c < 0) {
      msg("%s:%d:%d: caractère U+%04X (%.*s) non représentable en windows-1252, remplacé par ?", name, line, col, cp, (int)(i - st), b + st);
      c = '?'; bad++;
    }
    r[o++] = (char)c;
    if (cp == '\n') { line++; col = 1; } else col++;
  }
  r[o] = 0; *out = r; *outn = o; return bad;
}

/* ---------- conversion ---------- */
static char *basename_noext(const char *p) {
  const char *s = strrchr(p, '/'), *s2 = strrchr(p, '\\'); char *r, *e;
  if (s2 > s) s = s2;
  r = xstrdup(s ? s + 1 : p);
  e = strrchr(r, '.'); if (e && !strcmp(e, ".tex")) *e = 0;
  return r;
}
static char *dirname_of(const char *p) {
  char *r = xstrdup(p), *s; slashify(r); s = strrchr(r, '/');
  if (!s) { free(r); return xstrdup("."); }
  if (s == r) s[1] = 0; else *s = 0;
  return r;
}
/* équivalent du sed du script sh : première ligne « \title{...} », jusqu'à la dernière } */
static char *tex_title(const char *b) {
  const char *line = b;
  while (line && *line) {
    const char *nl = strchr(line, '\n'), *p = line, *end = nl ? nl : line + strlen(line);
    while (p < end && (*p == ' ' || *p == '\t')) p++;
    if (end - p > 7 && !strncmp(p, "\\title{", 7)) {
      const char *q = end, *st = p + 7;
      while (q > st && q[-1] != '}') q--;
      if (q > st) return xprintf("%.*s", (int)(q - 1 - st), st);
    }
    line = nl ? nl + 1 : NULL;
  }
  return NULL;
}
static char *replace_lines(const char *b, const char **keys, const char **vals, int nk) {
  size_t cap = strlen(b) + 4096, o = 0; char *r = malloc(cap); const char *line = b;
  while (*line) {
    const char *nl = strchr(line, '\n'); size_t l = nl ? (size_t)(nl - line + 1) : strlen(line); int k, done = 0;
    for (k = 0; k < nk && !done; k++) {
      size_t kl = strlen(keys[k]);
      if (!strncmp(line, keys[k], kl)) {
        char *t = xprintf("%s%s%s", keys[k], vals[k], nl ? "\n" : "");
        size_t tl = strlen(t); if (o + tl + 1 > cap) { cap = (o + tl) * 2; r = realloc(r, cap); }
        memcpy(r + o, t, tl); o += tl; free(t); done = 1;
      }
    }
    if (!done) { if (o + l + 1 > cap) { cap = (o + l) * 2; r = realloc(r, cap); } memcpy(r + o, line, l); o += l; }
    line += l;
  }
  r[o] = 0; return r;
}
static char *enc_value(const char *s) {   /* option en UTF-8 -> cp1252 si besoin */
  size_t n = strlen(s), on; char *o;
  if (!is_utf8_hi(s, n)) return xstrdup(s);
  utf8_to_cp1252("option", s, n, &o, &on); return o;
}

/* convertit O.tex dans le dossier md (vidé) ; retourne le nombre de blocs .def */
static int do_convert(runtime *r, const char *md, int run_index) {
  char *tex, *texdir, *base, *title, *tpl, *p, *src, *logf; size_t n; char **l; int k, nl, ndef = 0, bad = 0;
  const char *lang = O.lang;
  if (!O.tex) die(EX_USAGE, "--tex manquant");
  if (!is_file(O.tex)) die(EX_USAGE, "fichier absent : %s", O.tex);
  tex = abspath(O.tex); texdir = dirname_of(tex); base = basename_noext(tex);
  src = read_file(tex, &n); if (!src) die(EX_IO, "lecture impossible : %s", tex);
  title = O.title ? xstrdup(O.title) : tex_title(src);
  if (!title || !*title) { free(title); title = xstrdup(base); }
  tpl = xprintf("%s/public_html/modules/template/doc.%s", r->wh, lang);
  if (!is_dir(tpl)) { free(tpl); tpl = xprintf("%s/public_html/modules/template/doc.en", r->wh); }
  if (!is_dir(tpl)) die(EX_ENV, "modèle absent : %s", tpl);
  trace("tex=%s base=%s titre=%s modèle=%s cible=%s", tex, base, title, tpl, md);
  rm_rf(md);
  if (mkdir_p(md) != 0 || copy_tree(tpl, md) != 0) die(EX_IO, "copie du modèle impossible vers %s", md);
  p = pjoin(md, "doc/1"); rm_rf(p); free(p);
  p = pjoin(md, "doc/1/src"); mkdir_p(p); free(p);
  p = pjoin(md, "doc/srctex"); mkdir_p(p); free(p);
  /* sources : .tex + fichiers voisins (images, css), sauf produits LaTeX */
  l = list_dir(texdir, 0, &nl);
  for (k = 0; k < nl; k++) {
    const char *e = strrchr(l[k], '.'); char *s = pjoin(texdir, l[k]), *d;
    if (e && (!strcmp(e, ".aux") || !strcmp(e, ".log") || !strcmp(e, ".pdf") || !strcmp(e, ".out") || !strcmp(e, ".toc"))) { free(s); continue; }
    if (!is_file(s)) { free(s); continue; }
    d = xprintf("%s/doc/srctex/%s", md, l[k]);
    if (copy_file(s, d) != 0) die(EX_IO, "copie impossible : %s", s);
    free(s); free(d);
  }
  free_list(l, nl);
  p = xprintf("%s/doc/srctex/wims.sty", md);
  if (!is_file(p)) { char *s = xprintf("%s/public_html/modules/adm/latex2wims/wims.sty", r->wh); copy_file(s, p); free(s); }
  free(p);
  /* WIMS sert en windows-1252 : convertir un source UTF-8 */
  if (is_utf8_hi(src, n)) {
    char *o; size_t on; char *dst = xprintf("%s/doc/srctex/%s.tex", md, base);
    bad = utf8_to_cp1252(tex, src, n, &o, &on);
    if (write_file(dst, o, on) != 0) die(EX_IO, "écriture impossible : %s", dst);
    msg("source UTF-8 converti en windows-1252 (%d caractère(s) non représentable(s))", bad);
    free(o); free(dst);
    { char *t = enc_value(title); free(title); title = t; }
    if (bad && O.strict) die(EX_CONV, "%d caractère(s) non représentable(s) en windows-1252 (--strict)", bad);
  } else trace("source sans UTF-8 étendu : copié tel quel");
  /* INDEX */
  { char *ip = pjoin(tpl, "INDEX"), *ib = read_file(ip, NULL), *au = enc_value(O.author ? O.author : ""), *em = enc_value(O.email ? O.email : "");
    const char *keys[] = { "title=", "description=", "author=", "address=", "language=" };
    const char *vals[] = { title, "document généré par latex2wims.", au, em, lang };
    char *desc = enc_value(vals[1]); vals[1] = desc;
    if (ib) { char *res = replace_lines(ib, keys, vals, 5), *dst = pjoin(md, "INDEX"); write_file(dst, res, strlen(res)); free(res); free(dst); }
    free(ip); free(ib);
    /* 0index et en-tête doc/1/.def (écrit d'habitude par adm/doc, propreg.proc) */
    p = xprintf("%s/doc/srctex/0index", md); { char *t = xprintf("1 %s\n", base); write_file(p, t, strlen(t)); free(t); } free(p);
    p = xprintf("%s/doc/1/.def", md);
    { char *t = xprintf("tit=%s\nauthor=%s\nemail=%s\ncopyright=\ndocopen=yes\ndoctoc=yes\ndlang=%s\nheader=\ndesc=\n", title, au, em, lang);
      write_file(p, t, strlen(t)); free(t); }
    free(p); free(au); free(em); free(desc); }
  if (!run_index) { msg("arbre préparé sans mkindex : %s", md); return 0; }
  logf = xprintf("%s/doc/latex2wims.log", md);
  remove(logf);
  { FILE *lf = fopen(logf, "ab"); if (!lf) die(EX_IO, "journal %s", logf);
    if (mkindex(md, r->wh, r->perl, r->msg2wims, lf, logf) != 0) msg("mkindex : dossier src absent");
    fclose(lf); }
  /* sous-processus : leur sortie va aussi dans doc/latex2wims.log (relance non nécessaire) */
  p = pjoin(md, "doc/1"); l = list_dir(p, 0, &nl);
  for (k = 0; k < nl; k++) { size_t ll = strlen(l[k]); if (ll > 4 && !strcmp(l[k] + ll - 4, ".def")) ndef++; }
  free_list(l, nl); free(p);
  if (g_log) { fprintf(g_log, "--- %s\n", logf); fflush(g_log); append_file(O.log, logf); }
  if (ndef == 0) {
    char *lb = read_file(logf, NULL); fprintf(stderr, "%s", lb ? lb : ""); free(lb);
    die(EX_CONV, "aucun .def produit, voir %s", logf);
  }
  free(logf); free(tex); free(texdir); free(base); free(title); free(tpl); free(src);
  return ndef;
}

/* mkindex a des sous-processus : rediriger leur sortie vers le journal du module */
static const char *g_sublog;

static char *module_name(void) {
  char *base;
  if (O.module) {
    if (strstr(O.module, "..") || O.module[0] == '/' || O.module[0] == '\\' || strchr(O.module, ':')) die(EX_USAGE, "module invalide : %s", O.module);
    return xstrdup(O.module);
  }
  if (!O.tex) die(EX_USAGE, "--module ou --tex manquant");
  base = basename_noext(O.tex);
  { char *m = xprintf("%s/%s.%s", DEFAULT_MODULE_ZONE, base, O.lang); free(base); return m; }
}

/* ---------- check : vrai wims.cgi ---------- */
static char *cgi_get(runtime *r, const char *query, size_t *len) {
  char *pub = xprintf("%s/public_html", r->wh), *tmpd = xprintf("%s/tmp", r->wh), *out; char *b;
  mkdir_p(tmpd);
  out = xprintf("%s/l2w-check-%ld.html", tmpd, (long)time(NULL));
  remove(out);
  set_env("REQUEST_METHOD", "GET"); set_env("QUERY_STRING", query);
  set_env("HTTP_USER_AGENT", "Mozilla/5.0 (X11) Firefox/130.0"); set_env("SERVER_NAME", "localhost");
  set_env("SERVER_PORT", "80"); set_env("REMOTE_ADDR", "127.0.0.1"); set_env("SCRIPT_NAME", "/wims/wims.cgi");
  set_env("GATEWAY_INTERFACE", "CGI/1.1"); set_env("wims_home", r->wh);
  { const char *a[] = { r->cgi, NULL }; int rc = run(a, pub, out); trace("wims.cgi %s -> %d", query, rc); }
  b = read_file(out, len);
  remove(out); free(out); free(pub); free(tmpd);
  return b;
}
static int contains(const char *h, const char *n) { return h && strstr(h, n) != NULL; }

static int do_check(runtime *r, const char *mod, const char *title) {
  int fail = 0, nblocks = 0, nmath = 0, k; char *q, *b, *ix, *ib; size_t n;
  const char *blocks[256]; int nb = 0;
  blocks[nb++] = "main";
  ix = xprintf("%s/public_html/modules/%s/doc/1/.index", r->wh, mod);
  ib = read_file(ix, NULL);
  if (!ib) die(EX_CHECK, "module non installé : %s", ix);
  { char *line = ib; while (*line && nb < 256) { char *nl = strchr(line, '\n'); if (nl) *nl = 0;
      if (line[0] == ':' && line[1] && strcmp(line + 1, "main")) blocks[nb++] = line + 1;
      if (!nl) break; line = nl + 1; } }
  for (k = 0; k < nb; k++) {
    int ok = 1; char why[512] = "";
    q = xprintf("module=%s&cmd=new&lang=%s&doc=1&block=%s", mod, O.lang, blocks[k]);
    b = cgi_get(r, q, &n);
    if (!b) { ok = 0; strcpy(why, "pas de sortie"); }
    else {
      char st[128] = ""; sscanf(b, "%127[^\r\n]", st);
      if (!strstr(st, "200") && !strncmp(st, "Status:", 7)) { ok = 0; snprintf(why, sizeof why, "%s", st); }
      if (contains(b, "ne peut plus satisfaire")) { ok = 0; strcat(why, " ; serveur « surchargé » : ajouter threshold1/threshold2 à log/wims.conf"); }
      if (!contains(b, "</html>")) { ok = 0; strcat(why, " ; pas de </html>"); }
      if (contains(b, "existe pas") || contains(b, "WIMS User Error")) { ok = 0; strcat(why, " ; erreur WIMS dans la page"); }
      if (k == 0 && title && !contains(b, title)) { ok = 0; strcat(why, " ; titre absent"); }
      if (contains(b, "<math")) nmath++;
    }
    msg("%s bloc %s%s%s", ok ? "PASS" : "FAIL", blocks[k], *why ? " :" : "", why);
    if (!ok) fail = 1; nblocks++;
    if (b) {
      int e; for (e = 0; e < O.nexpect; e++) if (O.expect[e] && contains(b, O.expect[e])) O.expect[e] = NULL;
    }
    free(b); free(q);
  }
  for (k = 0; k < O.nexpect; k++) if (O.expect[k]) { msg("FAIL texte attendu absent de tous les blocs : %s", O.expect[k]); fail = 1; }
  /* MathML : exigé si le source contient des formules */
  { char *tp = xprintf("%s/public_html/modules/%s/doc/srctex", r->wh, mod); int nl, j, hasmath = 0; char **l = list_dir(tp, 0, &nl);
    for (j = 0; j < nl; j++) { size_t ll = strlen(l[j]); if (ll > 4 && !strcmp(l[j] + ll - 4, ".tex")) {
        char *f = pjoin(tp, l[j]), *s = read_file(f, NULL);
        if (s && (strstr(s, "\\(") || strstr(s, "\\[") || strchr(s, '$'))) hasmath = 1; free(s); free(f); } }
    free_list(l, nl); free(tp);
    if (hasmath && !nmath) { msg("FAIL formules dans le source mais aucun MathML (<math) servi : vérifier default_texposition=2"); fail = 1; }
    else msg("%s MathML : %d bloc(s) sur %d avec <math", hasmath ? "PASS" : "INFO", nmath, nblocks); }
  free(ix); free(ib);
  msg("== check %s : %s (%d blocs) ==", mod, fail ? "ÉCHEC" : "OK", nblocks);
  return fail ? EX_CHECK : EX_OK;
}

static char *installed_title(runtime *r, const char *mod) {
  char *p = xprintf("%s/public_html/modules/%s/doc/1/.def", r->wh, mod), *b = read_file(p, NULL), *t = NULL;
  free(p);
  if (b && !strncmp(b, "tit=", 4)) { char *e = strchr(b, '\n'); t = xprintf("%.*s", (int)(e ? e - b - 4 : (long)strlen(b + 4)), b + 4); }
  free(b); return t;
}

/* ---------- hash : empreintes normalisées ---------- */
/* normalisation : CRLF -> LF ; doc/latex2wims.log ignoré (journal) ; chemins absolus du
   module et du runtime remplacés par @MOD@ et @WH@ ; « !set datm=... » (date) -> @DATE@ */
static void hash_tree(const char *root, const char *rel, FILE *o, sha256_ctx *tree, int *nfiles, const char *wh) {
  char *d = rel[0] ? pjoin(root, rel) : xstrdup(root); int n, i; char **l = list_dir(d, 1, &n);
  for (i = 0; i < n; i++) {
    char *r2 = rel[0] ? xprintf("%s/%s", rel, l[i]) : xstrdup(l[i]), *full = pjoin(root, r2);
    if (is_dir(full)) hash_tree(root, r2, o, tree, nfiles, wh);
    else if (strcmp(r2, "doc/latex2wims.log")) {
      size_t len, j, k = 0; char *b = read_file(full, &len), hex[65]; sha256_ctx c;
      const char *pats[2]; const char *reps[2] = { "@MOD@", "@WH@" };
      pats[0] = root; pats[1] = wh;
      sha256_init(&c);
      for (j = 0; b && j < len; j++) {
        int m, hit = 0;
        for (m = 0; m < 2; m++) if (pats[m] && *pats[m]) { size_t pl = strlen(pats[m]); if (j + pl <= len && !memcmp(b + j, pats[m], pl)) { sha256_update(&c, reps[m], strlen(reps[m])); j += pl - 1; hit = 1; break; } }
        if (hit) continue;
        if ((j == 0 || b[j-1] == '\n') && j + 10 <= len && !memcmp(b + j, "!set datm=", 10)) {  /* date de génération */
          sha256_update(&c, "!set datm=@DATE@", 16); j += 10; while (j < len && b[j] != '\n' && b[j] != '\r') j++; j--; continue; }
        if (b[j] == '\r' && j + 1 < len && b[j + 1] == '\n') continue;
        sha256_update(&c, b + j, 1);
      }
      (void)k;
      sha256_hex(&c, hex);
      fprintf(o, "%s  %s\n", hex, r2);
      sha256_update(tree, hex, 64); sha256_update(tree, r2, strlen(r2)); sha256_update(tree, "\n", 1);
      (*nfiles)++; free(b);
    }
    free(r2); free(full);
  }
  free_list(l, n); free(d);
}
static void tree_hash(const char *dir, const char *wh, FILE *o, char hex[65], int *nfiles) {
  sha256_ctx t; char *root = abspath(dir); FILE *nul = o ? o : NULL;
  sha256_init(&t); *nfiles = 0;
  if (!nul) { nul = tmpfile(); }
  hash_tree(root, "", nul, &t, nfiles, wh);
  if (!o) fclose(nul);
  sha256_hex(&t, hex); free(root);
}

/* ---------- corpus : parité ---------- */
static int do_corpus(void) {
  runtime r = rt_open(0); int n, i, nok = 0, ndiff = 0, nfail = 0, nsame = 0; char **l; FILE *rep; char *outdir, *trees;
  char *refb = NULL;
  if (!O.corpus || !O.out) die(EX_USAGE, "corpus : --corpus DOSSIER et --out RAPPORT.tsv requis");
  if (O.ref) { refb = read_file(O.ref, NULL); if (!refb) die(EX_USAGE, "référence illisible : %s", O.ref); }
  outdir = dirname_of(O.out); mkdir_p(outdir); { char *a = abspath(outdir); free(outdir); outdir = a; } trees = xprintf("%s/trees", outdir); mkdir_p(trees);
  rep = fopen(O.out, "wb"); if (!rep) die(EX_IO, "rapport : %s", O.out);
#ifdef _WIN32
  fprintf(rep, "# latex2wims %s corpus windows\n", L2W_VERSION);
#else
  fprintf(rep, "# latex2wims %s corpus linux\n", L2W_VERSION);
#endif
  fprintf(rep, "#fichier\tstatut\tblocs\tfichiers\tempreinte_arbre\n");
  l = list_dir(O.corpus, 0, &n);
  for (i = 0; i < n; i++) {
    /* corpus/<nom>/<nom>.tex : un dossier par document (les fichiers voisins sont copiés) */
    char *tex, *base, *md, hex[65]; int nb, nf; const char *st = "ok";
    tex = xprintf("%s/%s/%s.tex", O.corpus, l[i], l[i]);
    if (!is_file(tex)) { free(tex); continue; }
    base = xstrdup(l[i]); md = pjoin(trees, base);
    O.tex = tex; O.title = NULL;
    msg("--- %s", l[i]);
    nb = do_convert(&r, md, 1);
    tree_hash(md, r.wh, NULL, hex, &nf);
    { char *hp = xprintf("%s/%s.sha256", trees, base); FILE *h = fopen(hp, "wb"); if (h) { int x; char hx[65]; tree_hash(md, r.wh, h, hx, &x); fclose(h); } free(hp); }
    if (refb) {
      char *key = xprintf("\n%s\t", l[i]), *f = strstr(refb, key);
      if (!f) { st = "absent_ref"; ndiff++; }
      else { char *t = strrchr(xprintf("%.*s", (int)(strchr(f + 1, '\n') ? strchr(f + 1, '\n') - f - 1 : (long)strlen(f + 1)), f + 1), '\t');
             if (t && !strcmp(t + 1, hex)) { st = "identique"; nsame++; } else { st = "DIFFERENT"; ndiff++; } }
      free(key);
    }
    nok++;
    fprintf(rep, "%s\t%s\t%d\t%d\t%s\n", l[i], st, nb, nf, hex);
    msg("%s : %d blocs, %d fichiers, %s %s", l[i], nb, nf, hex, st);
    free(tex); free(base); free(md);
  }
  fclose(rep); free_list(l, n);
  msg("== corpus : %d fichier(s) convertis, %d échec(s)%s ==", nok, nfail,
      refb ? xprintf(", parité : %d identique(s), %d différent(s)", nsame, ndiff) : "");
  free(refb);
  return ndiff ? EX_CHECK : EX_OK;
}

int main(int argc, char **argv) {
  parse(argc, argv);
  (void)g_sublog;
  if (!strcmp(O.cmd, "version")) { printf("latex2wims %s\n", L2W_VERSION); return 0; }
  if (!strcmp(O.cmd, "convert")) {
    runtime r = rt_open(0); char *b = O.tex ? basename_noext(O.tex) : NULL;
    char *out = O.out ? xstrdup(O.out) : xprintf("%s.%s", b ? b : "doc", O.lang); int nb;
    char *md;
    mkdir_p(out); md = abspath(out);
    nb = do_convert(&r, md, !O.no_index);
    if (!O.no_index) msg("arbre : %s (%d blocs, journal doc/latex2wims.log)", md, nb);
    return 0;
  }
  if (!strcmp(O.cmd, "install") || !strcmp(O.cmd, "all")) {
    runtime r = rt_open(!strcmp(O.cmd, "all")); char *mod = module_name();
    char *md = xprintf("%s/public_html/modules/%s", r.wh, mod); int nb = do_convert(&r, md, 1);
    msg("module : %s (%d blocs, journal doc/latex2wims.log)", mod, nb);
    printf("URL : %s?module=%s&cmd=new&lang=%s\n", O.url ? O.url : DEFAULT_URL, mod, O.lang); fflush(stdout);
    if (!strcmp(O.cmd, "all")) {
      char *t = installed_title(&r, mod); int rc = do_check(&r, mod, t);
      if (g_log) fclose(g_log);
      return rc;
    }
    return 0;
  }
  if (!strcmp(O.cmd, "check")) {
    runtime r = rt_open(1); char *mod = module_name(); char *t = installed_title(&r, mod);
    int rc = do_check(&r, mod, t);
    if (g_log) fclose(g_log);
    return rc;
  }
  if (!strcmp(O.cmd, "mkindex")) {
    runtime r = rt_open(0); char *md = abspath(O.from ? O.from : ".");
    int rc = mkindex(md, r.wh, r.perl, r.msg2wims, stdout, NULL);
    return rc ? EX_CONV : 0;
  }
  if (!strcmp(O.cmd, "hash")) {
    char hex[65]; int nf; if (!O.from) die(EX_USAGE, "hash DOSSIER");
    if (!is_dir(O.from)) die(EX_USAGE, "dossier absent : %s", O.from);
    tree_hash(O.from, O.wh ? abspath(O.wh) : NULL, stdout, hex, &nf);
    printf("%s  ARBRE (%d fichiers)\n", hex, nf); return 0;
  }
  if (!strcmp(O.cmd, "corpus")) return do_corpus();
  fprintf(stderr, "latex2wims: commande inconnue : %s\n", O.cmd); usage(EX_USAGE);
  return EX_USAGE;
}
