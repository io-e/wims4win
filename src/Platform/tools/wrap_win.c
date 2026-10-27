/* bin/wrap..exec for Windows (Misc/wrap.c): run argv[1..] and return its
 * exit code. There is no setuid on Windows, so there are no ids to reset:
 * wims already runs helpers with its own token (WP_ISO_DROP_PRIV refuses an
 * elevated one). "#!" scripts go through the interpreter found in PATH, and
 * a name without ".exe" is resolved, as in wp_spawn. GPL v2 or later. */
#include <windows.h>
#include <stdio.h>
#include <string.h>
static size_t q(char *d, size_t cap, const char *a)
{
  size_t n=0; const char *p; int bs;
  #define PUT(c) do { if(n+1<cap) d[n]=(c); n++; } while(0)
  if(*a && !strpbrk(a," \t\n\v\"")) { for(p=a;*p;p++) PUT(*p); return n; }
  PUT('"');
  for(p=a;;p++) {
    bs=0; while(*p=='\\') { bs++; p++; }
    if(*p==0) { while(bs--) { PUT('\\'); PUT('\\'); } break; }
    if(*p=='"') { while(bs--) { PUT('\\'); PUT('\\'); } PUT('\\'); PUT('"'); }
    else { while(bs--) PUT('\\'); PUT(*p); }
  }
  PUT('"');
  #undef PUT
  return n;
}
static int interp(const char *f, char *in, size_t cap)
{
  FILE *h=fopen(f,"rb"); char l[256], *p, *e, *b;
  if(!h) return 0;
  if(!fgets(l,sizeof(l),h)) { fclose(h); return 0; }
  fclose(h);
  if(l[0]!='#'||l[1]!='!') return 0;
  p=l+2; while(*p==' ') p++;
  e=p; while(*e && *e!=' ' && *e!='\r' && *e!='\n') e++;
  b=e; while(b>p && b[-1]!='/') b--;
  if((size_t)(e-b)+1>cap) return 0;
  memcpy(in,b,e-b); in[e-b]=0;
  if(strcmp(in,"env")==0) { p=e; while(*p==' ') p++; e=p;
    while(*e && *e!=' ' && *e!='\r' && *e!='\n') e++;
    if((size_t)(e-p)+1>cap) return 0; memcpy(in,p,e-p); in[e-p]=0; }
  return in[0]!=0;
}
int main(int argc, char **argv)
{
  static char cmd[32768]; size_t n=0; int i; char exe[MAX_PATH], in[64];
  STARTUPINFOA si; PROCESS_INFORMATION pi; DWORD code=127, a;
  if(argc<2) return 0;
  snprintf(exe,sizeof(exe),"%s",argv[1]);
  a=GetFileAttributesA(exe);
  if(a==INVALID_FILE_ATTRIBUTES || (a&FILE_ATTRIBUTE_DIRECTORY)) {
    snprintf(exe,sizeof(exe),"%s.exe",argv[1]);
    if(GetFileAttributesA(exe)==INVALID_FILE_ATTRIBUTES) snprintf(exe,sizeof(exe),"%s",argv[1]);
  } else if(interp(exe,in,sizeof(in))) {
    n+=q(cmd+n,sizeof(cmd)-n,in); cmd[n++]=' ';
  }
  n+=q(cmd+n,sizeof(cmd)-n,exe);
  for(i=2;i<argc;i++) { if(n+1<sizeof(cmd)) cmd[n++]=' '; n+=q(cmd+n,n<sizeof(cmd)?sizeof(cmd)-n:0,argv[i]); }
  if(n>=sizeof(cmd)) return 127;
  cmd[n]=0;
  memset(&si,0,sizeof(si)); si.cb=sizeof(si); si.dwFlags=STARTF_USESTDHANDLES;
  si.hStdInput=GetStdHandle(STD_INPUT_HANDLE);
  si.hStdOutput=GetStdHandle(STD_OUTPUT_HANDLE);
  si.hStdError=GetStdHandle(STD_ERROR_HANDLE);
  if(!CreateProcessA(NULL,cmd,NULL,NULL,TRUE,CREATE_NO_WINDOW,NULL,NULL,&si,&pi)) return 127;
  WaitForSingleObject(pi.hProcess,INFINITE);
  GetExitCodeProcess(pi.hProcess,&code);
  CloseHandle(pi.hThread); CloseHandle(pi.hProcess);
  return (int)code;
}
