/* CGI start-up checks for the native Win32 wims.exe. GPL v2 or later.
 *
 * - chdir() to the directory holding wims.exe (Apache/Laragon may start the
 *   CGI elsewhere; WIMS resolves everything relative to public_html/);
 * - normalise path-like CGI variables from '\' to '/';
 * - verify that the runtime tree produced by a Linux `compile` is present
 *   (html/, msg/, scripts/, ...) and answer "Status: 500" with an explicit
 *   message instead of crashing later on a missing file.
 */
#ifdef _WIN32
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <windows.h>
#include <direct.h>
#include <fcntl.h>
#include <io.h>
#include "wp_compat.h"

void wp_path_slashes(char *s)
{
  if(s) for(; *s; s++) if(*s=='\\') *s='/';
}

static int dir_nonempty(const char *d)
{
  char pat[MAX_PATH]; WIN32_FIND_DATAA fd; HANDLE h; int n=0;
  snprintf(pat,sizeof(pat),"%s/*",d);
  h=FindFirstFileA(pat,&fd); if(h==INVALID_HANDLE_VALUE) return 0;
  do { if(strcmp(fd.cFileName,".") && strcmp(fd.cFileName,"..")) n++; }
  while(n==0 && FindNextFileA(h,&fd));
  FindClose(h); return n>0;
}

static int exists(const char *f)
{
  return GetFileAttributesA(f)!=INVALID_FILE_ATTRIBUTES;
}

void wp_cgi_prepare(void)
{
  static const char *vars[]={"SCRIPT_FILENAME","DOCUMENT_ROOT",
    "CONTEXT_DOCUMENT_ROOT","PATH_TRANSLATED","w_wims_home",NULL};
  static const char *dirs[]={"html","msg","scripts","bases","modules",
    "modules/home","themes",NULL};
  static const char *files[]={"../bin/wimslogd.exe","../log",NULL};
  char exe[MAX_PATH], missing[2048], *p; int i, n=0;
  DWORD l;
  /* All WIMS files are LF text and readfile() checks read()==st_size:
   * the CRT default text mode wrote CRLF (var.stat, var, logs...) and then
   * refused to read them back (sessions lost: cmd=reply became cmd=new). */
  _fmode = _O_BINARY; /* _set_fmode() absent from older mingw-w64 msvcrt import libs (CI) */
  _setmode(0,_O_BINARY); _setmode(1,_O_BINARY); _setmode(2,_O_BINARY);
  l=GetModuleFileNameA(NULL,exe,sizeof(exe));

  if(l>0 && l<sizeof(exe)) {
    wp_path_slashes(exe);
    p=strrchr(exe,'/'); if(p) { *p=0; _chdir(exe); }
  }
  for(i=0; vars[i]; i++) {
    char *v=getenv(vars[i]);
    if(v && strchr(v,'\\')) { char *c=strdup(v); wp_path_slashes(c); setenv(vars[i],c,1); free(c); }
  }
  /* log.c:checklogd() reads SERVER_ADDR unconditionally. */
  if(getenv("SERVER_ADDR")==NULL) setenv("SERVER_ADDR","127.0.0.1",1);

  missing[0]=0;
  for(i=0; dirs[i]; i++) if(!dir_nonempty(dirs[i])) {
    n++; snprintf(missing+strlen(missing),sizeof(missing)-strlen(missing),
      "  public_html/%s/ (missing or empty)\n",dirs[i]);
  }
  for(i=0; files[i]; i++) if(!exists(files[i])) {
    n++; snprintf(missing+strlen(missing),sizeof(missing)-strlen(missing),
      "  %s (missing)\n",files[i]+3);
  }
  if(n==0) return;
  _getcwd(exe,sizeof(exe)); wp_path_slashes(exe);
  printf("Status: 500 WIMS runtime tree incomplete\r\n"
         "Cache-Control: no-cache, no-store, must-revalidate\r\n"
         "Content-type: text/plain\r\n\r\n"
         "WIMS: the runtime tree is incomplete; wims.exe cannot run.\n"
         "public_html = %s\n\nMissing:\n%s\n"
         "Build it on Linux with tools/build-runtime-tree.sh (official\n"
         "wims-4.xx.tgz + ./compile + bin/mkindex) and copy wims-runtime/.\n",
         exe,missing);
  fprintf(stderr,"wims: runtime tree incomplete in %s\n%s",exe,missing);
  fflush(stdout); exit(0);
}
#endif
