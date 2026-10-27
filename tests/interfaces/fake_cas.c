/* Fake CAS (C version of fake_cas.py, buildable for Linux and Windows):
 * speaks just enough of the gp protocol used by the pari interface.
 *   print("...") -> the string ; quit -> exit ; \e1 -> echo "? <line>" ;
 *   other non-assignment lines -> ECHO:<line>
 * argv[1]: plain | crlf (CRLF line ends, like a Windows CAS)
 *        | flood (20 MB after each echo) | silent (reads, never answers)
 *        | noread (never reads stdin: fills the pipe) | busy (CPU loop)
 *        | spawn (starts a grandchild "fake_cas silent" then behaves as plain:
 *          used to check that killing the job kills the whole tree) */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef _WIN32
#include <unistd.h>
#endif
#ifdef _WIN32
#include <io.h>
#include <fcntl.h>
#include <windows.h>
#endif
int main(int argc, char **argv)
{
  const char *mode=argc>1?argv[1]:"plain", *eol=strcmp(mode,"crlf")==0?"\r\n":"\n";
  char line[65536]; int echo=0;
#ifdef _WIN32
  _setmode(0,_O_BINARY); _setmode(1,_O_BINARY);
  if(strcmp(mode,"spawn")==0) {
    STARTUPINFOA si; PROCESS_INFORMATION pi; char cmd[1024];
    memset(&si,0,sizeof(si)); si.cb=sizeof(si);
    snprintf(cmd,sizeof(cmd),"\"%s\" silent",argv[0]);
    if(CreateProcessA(NULL,cmd,NULL,NULL,FALSE,CREATE_NO_WINDOW,NULL,NULL,&si,&pi)) {
      fprintf(stderr,"grandchild=%lu\n",pi.dwProcessId); fflush(stderr);
      CloseHandle(pi.hThread); CloseHandle(pi.hProcess);
    }
  }
#endif
  /* "write <file>": sandbox write tests without cmd.exe (rc 0 = written) */
  if(strcmp(mode,"write")==0) {
    FILE *f=argc>2?fopen(argv[2],"wb"):NULL;
    if(f==NULL) return 3;
    fputs("ok\n",f); return fclose(f)==0?0:3;
  }
  if(strcmp(mode,"busy")==0) { volatile unsigned long x=0; for(;;) x++; }
  if(strcmp(mode,"noread")==0) for(;;) {
#ifdef _WIN32
    Sleep(1000);
#else
    sleep(1);
#endif
  }
  if(strcmp(mode,"silent")==0) { while(fgets(line,sizeof(line),stdin)); for(;;){
#ifdef _WIN32
    Sleep(1000);
#else
    sleep(1);
#endif
  } }
  while(fgets(line,sizeof(line),stdin)) {
    char *t=line, *e; size_t n=strlen(line);
    if(n && line[n-1]=='\n') line[--n]=0;
    while(*t==' '||*t=='\t') t++;
    if(strcmp(t,"\\e1")==0) { echo=1; continue; }
    if(echo && *t) printf("? %s%s",line,eol);
    if(strcmp(t,"quit")==0) break;
    if(strncmp(line,"print(\"",7)==0 && (e=strrchr(line,'"'))!=NULL && e>line+6
       && strncmp(e,"\")",2)==0) {
      *e=0; printf("%s%s",line+7,eol);
    }
    else if(*t && line[0]!='\\' && strncmp(line,"default",7) && strncmp(line,"alias",5)
            && strncmp(line,"setrand",7) && !strchr(line,'=')) {
      printf("ECHO:%s%s",line,eol);
      if(strcmp(mode,"flood")==0) {
        static char big[1<<20]; int i; memset(big,'x',sizeof(big));
        for(i=0;i<19;i++) fwrite(big,1,sizeof(big),stdout);
        fwrite(big,1,20000000-19*sizeof(big),stdout); fputs(eol,stdout);
      }
    }
    fflush(stdout);
  }
  return 0;
}
