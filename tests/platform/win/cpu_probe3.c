/* N5: diagnose Job CPU limits with GetProcessTimes */
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <stdio.h>
static void ptimes(HANDLE h, const char *tag){
  FILETIME c,e,k,u; ULARGE_INTEGER K,U;
  if(!GetProcessTimes(h,&c,&e,&k,&u)) return;
  K.LowPart=k.dwLowDateTime; K.HighPart=k.dwHighDateTime;
  U.LowPart=u.dwLowDateTime; U.HighPart=u.dwHighDateTime;
  printf("%s kernel=%.3fs user=%.3fs\n", tag, K.QuadPart/1e7, U.QuadPart/1e7);
}
int main(int argc, char **argv){
  long cpu=argc>1?atol(argv[1]):2;
  const char *fake=argc>2?argv[2]:"fake_cas.exe";
  char cmd[512]; JOBOBJECT_EXTENDED_LIMIT_INFORMATION li;
  HANDLE job; PROCESS_INFORMATION pi; STARTUPINFOA si; DWORD t0; BOOL ok;
  printf("mingw PROCESS_TIME flag=0x%lx JOB_TIME=0x%lx\n",
    (unsigned long)JOB_OBJECT_LIMIT_PROCESS_TIME,(unsigned long)JOB_OBJECT_LIMIT_JOB_TIME);
  printf("SDK expected: PROCESS_TIME=0x2 JOB_TIME=0x4\n");
  job=CreateJobObjectA(NULL,NULL);
  memset(&li,0,sizeof(li));
  li.BasicLimitInformation.LimitFlags=JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
    |JOB_OBJECT_LIMIT_PROCESS_TIME|JOB_OBJECT_LIMIT_JOB_TIME;
  li.BasicLimitInformation.PerProcessUserTimeLimit.QuadPart=(LONGLONG)cpu*10000000LL;
  li.BasicLimitInformation.PerJobUserTimeLimit.QuadPart=(LONGLONG)cpu*10000000LL;
  ok=SetInformationJobObject(job,JobObjectExtendedLimitInformation,&li,sizeof(li));
  printf("SetInfo=%d err=%lu\n",ok,GetLastError());
  memset(&si,0,sizeof(si)); si.cb=sizeof(si);
  snprintf(cmd,sizeof(cmd),"\"%s\" busy",fake);
  ok=CreateProcessA(NULL,cmd,NULL,NULL,FALSE,CREATE_SUSPENDED|CREATE_NO_WINDOW,NULL,NULL,&si,&pi);
  printf("Create=%d Assign...",ok);
  ok=AssignProcessToJobObject(job,pi.hProcess);
  printf("%d err=%lu\n",ok,GetLastError());
  ResumeThread(pi.hThread); CloseHandle(pi.hThread);
  t0=GetTickCount();
  for(;;){
    DWORD w=WaitForSingleObject(pi.hProcess,1000);
    ptimes(pi.hProcess,"tick");
    if(w==WAIT_OBJECT_0){ printf("DIED after %lu ms\n",GetTickCount()-t0); break; }
    if(GetTickCount()-t0>20000){ printf("STILL ALIVE 20s\n"); TerminateProcess(pi.hProcess,1); break; }
  }
  CloseHandle(pi.hProcess); CloseHandle(job); return 0;
}
