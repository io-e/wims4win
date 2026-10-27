/* util.c : voir util.h */
#define _GNU_SOURCE
#include "util.h"
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <fcntl.h>
#ifdef _WIN32
# include <windows.h>
# include <io.h>
# include <process.h>
# include <direct.h>
# define MKDIR(p) _mkdir(p)
#else
# include <unistd.h>
# include <sys/wait.h>
# define MKDIR(p) mkdir(p, 0755)
#endif

int g_trace = 0;
FILE *g_log = NULL;

static void vout(FILE *f, const char *fmt, va_list ap) { vfprintf(f, fmt, ap); fputc('\n', f); fflush(f); }
void msg(const char *fmt, ...) {
  va_list ap; va_start(ap, fmt); vout(stderr, fmt, ap); va_end(ap);
  if (g_log) { va_start(ap, fmt); vout(g_log, fmt, ap); va_end(ap); }
}
void trace(const char *fmt, ...) {
  va_list ap;
  if (!g_trace) return;
  fputs("[trace] ", stderr); va_start(ap, fmt); vout(stderr, fmt, ap); va_end(ap);
  if (g_log) { fputs("[trace] ", g_log); va_start(ap, fmt); vout(g_log, fmt, ap); va_end(ap); }
}
void die(int code, const char *fmt, ...) {
  va_list ap; fputs("latex2wims: ", stderr); va_start(ap, fmt); vout(stderr, fmt, ap); va_end(ap);
  if (g_log) { fputs("latex2wims: ", g_log); va_start(ap, fmt); vout(g_log, fmt, ap); va_end(ap);
    fprintf(g_log, "code de sortie %d\n", code); fclose(g_log); }
  exit(code);
}
char *xstrdup(const char *s) { char *r = strdup(s ? s : ""); if (!r) die(EX_IO, "mémoire"); return r; }
char *xprintf(const char *fmt, ...) {
  va_list ap; int n; char *r;
  va_start(ap, fmt); n = vsnprintf(NULL, 0, fmt, ap); va_end(ap);
  r = malloc(n + 1); if (!r) die(EX_IO, "mémoire");
  va_start(ap, fmt); vsnprintf(r, n + 1, fmt, ap); va_end(ap);
  return r;
}
char *pjoin(const char *a, const char *b) {
  size_t n = strlen(a);
  if (n && (a[n-1] == '/' || a[n-1] == '\\')) return xprintf("%s%s", a, b);
  return xprintf("%s/%s", a, b);
}
void slashify(char *p) { for (; *p; p++) if (*p == '\\') *p = '/'; }
char *abspath(const char *p) {
  char *r;
#ifdef _WIN32
  char buf[4096]; if (!_fullpath(buf, p, sizeof buf)) return xstrdup(p); r = xstrdup(buf);
#else
  r = realpath(p, NULL); if (!r) return xstrdup(p);
#endif
  slashify(r); return r;
}
int is_dir(const char *p) { struct stat s; return stat(p, &s) == 0 && S_ISDIR(s.st_mode); }
int is_file(const char *p) { struct stat s; return stat(p, &s) == 0 && S_ISREG(s.st_mode); }
int newer(const char *a, const char *b) {
  struct stat sa, sb;
  if (stat(a, &sa) != 0) return 0;
  if (stat(b, &sb) != 0) return 1;
#if defined(__linux__)
  if (sa.st_mtim.tv_sec != sb.st_mtim.tv_sec) return sa.st_mtim.tv_sec > sb.st_mtim.tv_sec;
  return sa.st_mtim.tv_nsec > sb.st_mtim.tv_nsec;
#else
  return sa.st_mtime > sb.st_mtime;
#endif
}
int mkdir_p(const char *p) {
  char *t = xstrdup(p), *q; int rc = 0;
  slashify(t);
  for (q = t + 1; *q; q++) if (*q == '/') { *q = 0; if (!is_dir(t) && *t && !(q - t == 2 && t[1] == ':')) MKDIR(t); *q = '/'; }
  if (!is_dir(t) && MKDIR(t) != 0 && !is_dir(t)) rc = -1;
  free(t); return rc;
}
int rm_rf(const char *p) {
  struct stat s; int n, i;
#ifdef _WIN32
  if (stat(p, &s) != 0) return 0;
#else
  if (lstat(p, &s) != 0) return 0;
#endif
  if (S_ISDIR(s.st_mode)) {
    char **l = list_dir(p, 1, &n);
    for (i = 0; i < n; i++) { char *c = pjoin(p, l[i]); rm_rf(c); free(c); }
    free_list(l, n);
    return rmdir(p);
  }
#ifdef _WIN32
  _chmod(p, _S_IREAD | _S_IWRITE);
#endif
  return remove(p);
}
int copy_file(const char *src, const char *dst) {
  FILE *in = fopen(src, "rb"), *out; char buf[65536]; size_t n; int rc = 0;
  if (!in) return -1;
  out = fopen(dst, "wb"); if (!out) { fclose(in); return -1; }
  while ((n = fread(buf, 1, sizeof buf, in)) > 0) if (fwrite(buf, 1, n, out) != n) { rc = -1; break; }
  fclose(in); if (fclose(out) != 0) rc = -1;
  return rc;
}
int copy_tree(const char *src, const char *dst) {
  int n, i, rc = 0; char **l;
  if (mkdir_p(dst) != 0) return -1;
  l = list_dir(src, 1, &n);
  for (i = 0; i < n; i++) {
    char *s = pjoin(src, l[i]), *d = pjoin(dst, l[i]);
    if (is_dir(s)) { if (copy_tree(s, d) != 0) rc = -1; }
    else if (copy_file(s, d) != 0) rc = -1;
    free(s); free(d);
  }
  free_list(l, n); return rc;
}
static int cmpstr(const void *a, const void *b) { return strcmp(*(char *const *)a, *(char *const *)b); }
char **list_dir(const char *p, int hidden, int *n) {
  DIR *d = opendir(p); struct dirent *e; char **l = NULL; int cap = 0;
  *n = 0; if (!d) return NULL;
  while ((e = readdir(d))) {
    if (!strcmp(e->d_name, ".") || !strcmp(e->d_name, "..")) continue;
    if (!hidden && e->d_name[0] == '.') continue;
    if (*n == cap) { cap = cap ? 2 * cap : 32; l = realloc(l, cap * sizeof *l); }
    l[(*n)++] = xstrdup(e->d_name);
  }
  closedir(d);
  if (*n) qsort(l, *n, sizeof *l, cmpstr);
  return l;
}
void free_list(char **l, int n) { int i; for (i = 0; i < n; i++) free(l[i]); free(l); }
char *read_file(const char *p, size_t *len) {
  FILE *f = fopen(p, "rb"); char *b = NULL; size_t cap = 0, n = 0, r;
  if (!f) return NULL;
  do { if (cap - n < 65536) { cap = cap * 2 + 65536; b = realloc(b, cap + 1); }
       r = fread(b + n, 1, cap - n, f); n += r; } while (r > 0);
  fclose(f); b[n] = 0; if (len) *len = n; return b;
}
int write_file(const char *p, const char *data, size_t len) {
  FILE *f = fopen(p, "wb"); if (!f) return -1;
  if (fwrite(data, 1, len, f) != len) { fclose(f); return -1; }
  return fclose(f);
}
int append_file(const char *dst, const char *src) {
  size_t n; char *b = read_file(src, &n); FILE *f; if (!b) return -1;
  f = fopen(dst, "ab"); if (!f) { free(b); return -1; }
  fwrite(b, 1, n, f); fclose(f); free(b); return 0;
}
void set_env(const char *k, const char *v) {
#ifdef _WIN32
  _putenv_s(k, v ? v : "");
#else
  setenv(k, v ? v : "", 1);
#endif
}
void unset_env(const char *k) {
#ifdef _WIN32
  _putenv_s(k, "");
#else
  unsetenv(k);
#endif
}
char *which(const char *prog) {
  const char *path = getenv("PATH"); char *pp, *tok, *save = NULL;
#ifdef _WIN32
  const char *sep = ";";
#else
  const char *sep = ":";
#endif
  if (strchr(prog, '/') || strchr(prog, '\\')) return is_file(prog) ? xstrdup(prog) : NULL;
  if (!path) return NULL;
  pp = xstrdup(path);
  for (tok = strtok_r(pp, sep, &save); tok; tok = strtok_r(NULL, sep, &save)) {
    char *c = pjoin(tok, prog);
    if (is_file(c)) { free(pp); return c; }
#ifdef _WIN32
    { char *e = xprintf("%s.exe", c); free(c); if (is_file(e)) { free(pp); return e; } free(e); }
#else
    free(c);
#endif
  }
  free(pp); return NULL;
}

#ifdef _WIN32
/* guillemets selon les règles de CommandLineToArgvW / CRT */
static void quote_arg(char **cl, size_t *len, size_t *cap, const char *a) {
  #define PUT(c) do { if (*len + 2 > *cap) { *cap = *cap * 2 + 256; *cl = realloc(*cl, *cap); } (*cl)[(*len)++] = (c); } while (0)
  int need = !*a || strpbrk(a, " \t\"") != NULL; const char *p; size_t bs;
  if (*len) PUT(' ');
  if (!need) { for (p = a; *p; p++) PUT(*p); (*cl)[*len] = 0; return; }
  PUT('"');
  for (p = a; ; p++) {
    bs = 0; while (*p == '\\') { bs++; p++; }
    if (!*p) { while (bs--) { PUT('\\'); PUT('\\'); } break; }
    if (*p == '"') { bs = bs * 2 + 1; while (bs--) PUT('\\'); PUT('"'); }
    else { while (bs--) PUT('\\'); PUT(*p); }
  }
  PUT('"'); (*cl)[*len] = 0;
  #undef PUT
}
#endif

int run(const char *const *argv, const char *cwd, const char *outfile) {
  int i;
  if (g_trace) {
    char *s = xstrdup(""); for (i = 0; argv[i]; i++) { char *t = xprintf("%s%s'%s'", s, i ? " " : "", argv[i]); free(s); s = t; }
    trace("exec (cwd=%s) %s%s%s", cwd ? cwd : ".", s, outfile ? " >> " : "", outfile ? outfile : ""); free(s);
  }
  fflush(NULL);
#ifdef _WIN32
  {
    char *cl = NULL; size_t len = 0, cap = 0; char *exe = which(argv[0]);
    STARTUPINFOA si; PROCESS_INFORMATION pi; SECURITY_ATTRIBUTES sa; HANDLE h = NULL; DWORD code = 1;
    if (!exe) { msg("lancement impossible : %s introuvable", argv[0]); return -1; }
    for (i = 0; argv[i]; i++) quote_arg(&cl, &len, &cap, argv[i]);
    memset(&si, 0, sizeof si); si.cb = sizeof si;
    if (outfile) {
      sa.nLength = sizeof sa; sa.lpSecurityDescriptor = NULL; sa.bInheritHandle = TRUE;
      h = CreateFileA(outfile, FILE_APPEND_DATA, FILE_SHARE_READ | FILE_SHARE_WRITE, &sa, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
      if (h == INVALID_HANDLE_VALUE) { free(exe); free(cl); return -1; }
      si.dwFlags = STARTF_USESTDHANDLES; si.hStdInput = GetStdHandle(STD_INPUT_HANDLE);
      si.hStdOutput = h; si.hStdError = h;
    }
    if (!CreateProcessA(exe, cl, NULL, NULL, TRUE, CREATE_NO_WINDOW, NULL, cwd, &si, &pi)) {
      msg("lancement impossible : %s (erreur Windows %lu)", exe, (unsigned long)GetLastError());
      if (h) CloseHandle(h); free(exe); free(cl); return -1;
    }
    WaitForSingleObject(pi.hProcess, INFINITE);
    GetExitCodeProcess(pi.hProcess, &code);
    CloseHandle(pi.hProcess); CloseHandle(pi.hThread); if (h) CloseHandle(h);
    free(exe); free(cl); return (int)code;
  }
#else
  {
    pid_t pid = fork(); int st;
    if (pid < 0) return -1;
    if (pid == 0) {
      if (cwd && chdir(cwd) != 0) _exit(127);
      if (outfile) { int fd = open(outfile, O_WRONLY | O_CREAT | O_APPEND, 0644); if (fd < 0) _exit(127); dup2(fd, 1); dup2(fd, 2); close(fd); }
      execvp(argv[0], (char *const *)argv); _exit(127);
    }
    if (waitpid(pid, &st, 0) < 0) return -1;
    if (WIFEXITED(st) && WEXITSTATUS(st) == 127) { msg("lancement impossible (ou code 127) : %s", argv[0]); return -1; }
    if (WIFEXITED(st)) return WEXITSTATUS(st);
    return 128 + (WIFSIGNALED(st) ? WTERMSIG(st) : 0);
  }
#endif
}
