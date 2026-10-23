/* Win32 counterpart of bin/ch..root: launch a CAS under a Restricted Token
 * in an AppContainer, with ACL-restricted access to the session directory.
 * GPL v2 or later.
 *
 * Usage:
 *   ch..root.exe --selftest                 exit 0 if AppContainer works
 *   ch..root.exe --cmd <cmd> [args...]      run cmd under the sandbox
 *                                           (stdin/stdout/stderr inherited)
 *
 * Security model (deliberately weaker than Linux chroot, but never silent):
 *  - RestrictedToken (DisableMaxPrivilege + LuaToken) drops SeDebug /
 *    SeImpersonate / SeTcb / SeLoadDriver / SeTakeOwnership / SeBackup /
 *    SeRestore / SeSecurity / SeSystemEnvironment / SeCreateToken /
 *    SeAssignPrimaryToken / SeIncreaseQuota / SeShutdown / SeTrustedCredManAccess.
 *  - AppContainer named "wims.chroot.<pid>" with NO capability SIDs: no
 *    network, no registry (HKCU write), no clipboard, no named objects.
 *  - The session working directory (TMPDIR or the current directory) gets an
 *    explicit ACE for the AppContainer SID (GENERIC_ALL); everything else
 *    is denied by the AppContainer profile.
 *  - The child is run under that identity via CreateProcessAsUser.
 *
 * If AppContainer / RestrictedToken is unavailable (Windows < 8, missing
 * UserEnv, or CreateAppContainerProfile fails), the program exits with code
 * 2, never falling back to an unrestricted CreateProcess.
 */
#define WIN32_LEAN_AND_MEAN
#define _WIN32_WINNT 0x0602
#include <windows.h>
#include <userenv.h>
#include <sddl.h>
#include <aclapi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#pragma comment(lib, "userenv")
#pragma comment(lib, "advapi32")

static void fail(const char *m, DWORD e)
{ fprintf(stderr,"ch..root: %s (err=%lu)\n",m,(unsigned long)e); fflush(stderr); }

/* Build a Restricted Token that cannot elevate: strip every privilege and
 * apply LUA_TOKEN (no elevation). SIDs to disable are built only when the
 * corresponding CreateWellKnownSid succeeds (wine / older Windows may lack
 * some of them). */
static HANDLE make_restricted(void)
{
  HANDLE cur=NULL, rest=NULL; TOKEN_PRIVILEGES *tp=NULL; DWORD n=0, i, nsa=0;
  WELL_KNOWN_SID_TYPE want[]={
    WinBuiltinAdministratorsSid, WinLocalSystemSid,
    WinNetworkServiceSid, WinLocalServiceSid
  };
  SID_AND_ATTRIBUTES sa[8]; BYTE sidbuf[8][SECURITY_MAX_SID_SIZE]; DWORD sn;
  if(!OpenProcessToken(GetCurrentProcess(),
       TOKEN_DUPLICATE|TOKEN_ASSIGN_PRIMARY|TOKEN_QUERY|TOKEN_ADJUST_DEFAULT
       |TOKEN_ADJUST_SESSIONID,&cur))
    { fail("OpenProcessToken",GetLastError()); return NULL; }
  GetTokenInformation(cur,TokenPrivileges,NULL,0,&n);
  tp=(TOKEN_PRIVILEGES*)malloc(n?n:16); if(!tp) { CloseHandle(cur); return NULL; }
  if(n && !GetTokenInformation(cur,TokenPrivileges,tp,n,&n))
    { fail("GetTokenInformation",GetLastError()); free(tp); CloseHandle(cur); return NULL; }
  for(i=0;i<(n?tp->PrivilegeCount:0);i++)
    tp->Privileges[i].Attributes=SE_PRIVILEGE_REMOVED;
  memset(sa,0,sizeof(sa));
  for(i=0;i<sizeof(want)/sizeof(want[0]);i++) {
    sn=SECURITY_MAX_SID_SIZE;
    if(CreateWellKnownSid(want[i],NULL,sidbuf[nsa],&sn))
      { sa[nsa].Sid=sidbuf[nsa]; nsa++; }
  }
  if(!CreateRestrictedToken(cur,DISABLE_MAX_PRIVILEGE|LUA_TOKEN,
                            nsa,nsa?sa:NULL,
                            n?tp->PrivilegeCount:0,n?tp->Privileges:NULL,
                            0,NULL,&rest))
    { fail("CreateRestrictedToken",GetLastError()); rest=NULL; }
  free(tp); CloseHandle(cur);
  return rest;
}

/* Create (or reopen) a named AppContainer and return its SID. */
static PSID make_appcontainer(const char *name, HANDLE *out_token)
{
  HRESULT hr; PSID sid=NULL; WCHAR wname[128]; HANDLE tok=*out_token;
  MultiByteToWideChar(CP_UTF8,0,name,-1,wname,128);
  hr=CreateAppContainerProfile(wname,wname,wname,NULL,0,&sid);
  if(hr==HRESULT_FROM_WIN32(ERROR_ALREADY_EXISTS)) {
    /* reopen: delete + recreate keeps the same SID for a given name */
    DeleteAppContainerProfile(wname);
    hr=CreateAppContainerProfile(wname,wname,wname,NULL,0,&sid);
  }
  if(FAILED(hr) || sid==NULL) {
    fail("CreateAppContainerProfile",(DWORD)hr); return NULL;
  }
  (void)tok; (void)out_token;
  return sid;
}

/* Grant GENERIC_ALL on dir to the AppContainer SID (session sandbox). */
static int grant_session(const char *dir, PSID sid)
{
  EXPLICIT_ACCESS_A ea; PACL old=NULL, neu=NULL; PSECURITY_DESCRIPTOR sd=NULL;
  DWORD e;
  memset(&ea,0,sizeof(ea));
  ea.grfAccessPermissions=GENERIC_ALL;
  ea.grfAccessMode=GRANT_ACCESS;
  ea.grfInheritance=SUB_CONTAINERS_AND_OBJECTS_INHERIT;
  ea.Trustee.TrusteeForm=TRUSTEE_IS_SID;
  ea.Trustee.TrusteeType=TRUSTEE_IS_USER;
  ea.Trustee.ptstrName=(LPSTR)sid;
  e=GetNamedSecurityInfoA(dir,SE_FILE_OBJECT,DACL_SECURITY_INFORMATION,
                          NULL,NULL,&old,NULL,&sd);
  if(e!=ERROR_SUCCESS) { fail("GetNamedSecurityInfo",e); return -1; }
  e=SetEntriesInAclA(1,&ea,old,&neu);
  if(e!=ERROR_SUCCESS) { fail("SetEntriesInAcl",e); LocalFree(sd); return -1; }
  e=SetNamedSecurityInfoA((LPSTR)dir,SE_FILE_OBJECT,DACL_SECURITY_INFORMATION,
                          NULL,NULL,neu,NULL);
  LocalFree(neu); LocalFree(sd);
  if(e!=ERROR_SUCCESS) { fail("SetNamedSecurityInfo",e); return -1; }
  return 0;
}

/* Launch argv under the restricted AppContainer token. Inherit std handles. */
static int launch(HANDLE tok, PSID ac_sid, char *const argv[])
{
  char cmd[32768]; size_t n=0; int i, ok;
  STARTUPINFOEXA si; PROCESS_INFORMATION pi; SIZE_T asz=0;
  SECURITY_CAPABILITIES sc; LPPROC_THREAD_ATTRIBUTE_LIST al=NULL;
  for(i=0;argv[i];i++) {
    if(i) { if(n+1<sizeof(cmd)) cmd[n]=' '; n++; }
    /* simple quoting: wrap if spaces */
    if(strpbrk(argv[i]," \t\"")) {
      if(n+1<sizeof(cmd)) cmd[n++]='"';
      for(const char *p=argv[i];*p && n+1<sizeof(cmd);p++) {
        if(*p=='"') { if(n+2<sizeof(cmd)) { cmd[n++]='\\'; cmd[n++]='"'; } }
        else cmd[n++]=*p;
      }
      if(n+1<sizeof(cmd)) cmd[n++]='"';
    } else for(const char *p=argv[i];*p && n+1<sizeof(cmd);p++) cmd[n++]=*p;
  }
  if(n>=sizeof(cmd)) return 2;
  cmd[n]=0;
  memset(&sc,0,sizeof(sc)); sc.AppContainerSid=ac_sid;
  InitializeProcThreadAttributeList(NULL,1,0,&asz);
  al=(LPPROC_THREAD_ATTRIBUTE_LIST)malloc(asz);
  if(!al || !InitializeProcThreadAttributeList(al,1,0,&asz)) return 2;
  UpdateProcThreadAttribute(al,0,PROC_THREAD_ATTRIBUTE_SECURITY_CAPABILITIES,
                            &sc,sizeof(sc),NULL,NULL);
  memset(&si,0,sizeof(si)); si.StartupInfo.cb=sizeof(si);
  si.StartupInfo.dwFlags=STARTF_USESTDHANDLES;
  si.StartupInfo.hStdInput=GetStdHandle(STD_INPUT_HANDLE);
  si.StartupInfo.hStdOutput=GetStdHandle(STD_OUTPUT_HANDLE);
  si.StartupInfo.hStdError=GetStdHandle(STD_ERROR_HANDLE);
  si.lpAttributeList=al;
  ok=CreateProcessAsUserA(tok,NULL,cmd,NULL,NULL,TRUE,
                          CREATE_NO_WINDOW|EXTENDED_STARTUPINFO_PRESENT,
                          NULL,NULL,&si.StartupInfo,&pi);
  DeleteProcThreadAttributeList(al); free(al);
  if(!ok) { fail("CreateProcessAsUser",GetLastError()); return 2; }
  WaitForSingleObject(pi.hProcess,INFINITE);
  { DWORD code=1; GetExitCodeProcess(pi.hProcess,&code);
    CloseHandle(pi.hThread); CloseHandle(pi.hProcess); return (int)code; }
}

static int selftest(void)
{
  HANDLE tok; PSID sid; char name[64]; char *av[]={"cmd.exe","/c","exit","0",NULL};
  int r;
  snprintf(name,sizeof(name),"wims.chroot.selftest.%lu",GetCurrentProcessId());
  tok=make_restricted(); if(!tok) return 2;
  sid=make_appcontainer(name,&tok); if(!sid) { CloseHandle(tok); return 2; }
  r=launch(tok,sid,av);
  FreeSid(sid); CloseHandle(tok);
  DeleteAppContainerProfile(L"wims.chroot.selftest"); /* best effort */
  return r==0?0:2;
}

int main(int argc, char **argv)
{
  HANDLE tok; PSID sid; char name[64]; char *const *av; const char *dir; int r;
  if(argc>=2 && strcmp(argv[1],"--selftest")==0) return selftest();
  if(argc<3 || strcmp(argv[1],"--cmd")!=0) {
    fprintf(stderr,"usage: ch..root.exe --selftest | --cmd <cmd> [args...]\n");
    return 2;
  }
  av=argv+2;
  snprintf(name,sizeof(name),"wims.chroot.%lu",GetCurrentProcessId());
  tok=make_restricted(); if(!tok) return 2;
  sid=make_appcontainer(name,&tok); if(!sid) { CloseHandle(tok); return 2; }
  dir=getenv("TMPDIR"); if(!dir || !*dir) dir=".";
  if(grant_session(dir,sid)<0) {
    /* non-fatal on a missing dir; child will simply not see it */
  }
  r=launch(tok,sid,av);
  FreeSid(sid); CloseHandle(tok);
  return r;
}
