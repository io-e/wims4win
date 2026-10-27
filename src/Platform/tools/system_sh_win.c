/* system() for helpers on Windows: "sh -c <cmd>" (MSYS2 sh from PATH,
 * other/msys2/usr/bin) instead of cmd.exe. Linked with -Wl,--wrap=system.
 * GPL v2 or later. */
#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
int __wrap_system(const char *cmd)
{
  static char line[32768]; size_t n=0; const char *p; DWORD code=1;
  STARTUPINFOA si; PROCESS_INFORMATION pi;
  if(cmd==NULL) return 1;
  n=snprintf(line,sizeof(line),"sh -c \"");
  for(p=cmd; *p && n+4<sizeof(line); p++) {
    if(*p=='"' || *p=='\\') line[n++]='\\';
    line[n++]=*p;
  }
  if(n+2>=sizeof(line)) return -1;
  line[n++]='"'; line[n]=0;
  fflush(NULL);
  memset(&si,0,sizeof(si)); si.cb=sizeof(si);
  si.dwFlags=STARTF_USESTDHANDLES;
  si.hStdInput=GetStdHandle(STD_INPUT_HANDLE);
  si.hStdOutput=GetStdHandle(STD_OUTPUT_HANDLE);
  si.hStdError=GetStdHandle(STD_ERROR_HANDLE);
  if(!CreateProcessA(NULL,line,NULL,NULL,TRUE,CREATE_NO_WINDOW,NULL,NULL,&si,&pi)) return -1;
  WaitForSingleObject(pi.hProcess,INFINITE);
  GetExitCodeProcess(pi.hProcess,&code);
  CloseHandle(pi.hThread); CloseHandle(pi.hProcess);
  return (int)code;
}
