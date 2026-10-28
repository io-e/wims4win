/* Win32 counterpart of bin/ch..root (src/Misc/chroot.c). GPL v2 or later.
 *
 * Command line (same forms as the Linux wrapper, plus Windows-only options):
 *   ch..root.exe <prog> [args...]     run prog in the sandbox (form used by
 *                                     Interfaces/common.c, WP_ISO_WRAPPER)
 *   ch..root.exe & sh|perl <code>     untrusted !exec sh/perl (calc.c): refused
 *                                     with 127, as the Linux wrapper does when it
 *                                     cannot chroot (no MSYS2 sh in AppContainer)
 *   ch..root.exe cleantmpdir          no chroot tree on Windows: no-op, 0
 *   ch..root.exe --cmd <prog> [args]  same as the first form (older callers)
 *   ch..root.exe --selftest           exit 0 iff the sandbox works here
 *                                     (re-runs itself with --noop: no cmd.exe)
 *   ch..root.exe --grant-rx <dir>...  install step: read+execute for
 *                                     ALL APPLICATION PACKAGES on CAS dirs
 *   ch..root.exe --purge-profiles     delete leftover "wims.chroot.*"
 *                                     AppContainer profiles (older versions)
 *   ch..root.exe --policy <prog>      print the policy <prog> would run under
 *                                     ("appcontainer" or "lowil"), audit aid
 *
 * Policy (D21): every program runs in an AppContainer, except the programs
 * listed with "lowil <name>" in ch..root.conf next to ch..root.exe (shipped
 * with "lowil gap.exe" only: Cygwin cannot create its named objects inside an
 * AppContainer, 0xC0000022). A "lowil" program runs with the same restricted
 * token at LOW integrity (no write to anything not labelled low: profile,
 * HKCU outside AppDataLow, runtime) in a Job with PROC_QUOTA processes,
 * "memory_mb" (default 2048) of committed memory and kill-on-close
 * ("lowil_path <dirs>" is prepended to PATH for them: Cygwin DLLs); only the
 * session dir is labelled low for the run. It is NOT isolated for reads or
 * network, like the Linux chroot. No file or no match = AppContainer.
 * <name> without '\' matches the program's file name, with '\' the full
 * path (case-insensitive).
 *
 * Sandbox, never silently weaker: any failure exits 2 (or 127), no fallback
 * to an unrestricted CreateProcess.
 *  - Restricted token (DisableMaxPrivilege + LUA, Administrators/System/
 *    Network/LocalService SIDs deny-only, every privilege removed).
 *  - AppContainer "wims.chroot.<pid>.<tick>" without capability SIDs: no
 *    network, no HKCU write, no named objects outside the container; files
 *    are readable only where ALL APPLICATION PACKAGES or the container SID
 *    has an ACE (System32, the CAS dirs granted by --grant-rx).
 *  - The session dir (TMPDIR, else tmp_dir) gets a GENERIC_ALL ACE for this
 *    container SID only for the duration of the run; the ACE and the profile
 *    are removed afterwards (no registry/ACL accumulation).
 *  - Job Object around the child: at most PROC_QUOTA processes (fork bomb,
 *    Linux RLIMIT_NPROC=15), killed with the wrapper (KILL_ON_JOB_CLOSE).
 *    CPU/memory limits come from the enclosing wims job (wp_spawn).
 *  - Environment cleaned like the Linux wrapper (env_rm). */
#define WIN32_LEAN_AND_MEAN
#define _WIN32_WINNT 0x0602
#include <windows.h>
#include <userenv.h>
#include <sddl.h>
#include <aclapi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define PROC_QUOTA 15
#define MAX_LOWIL 16

static char lowil[MAX_LOWIL][MAX_PATH]; static int nlowil;
static unsigned long memory_mb=2048;
static char lowil_path[4096];   /* prepended to PATH for lowil runs (Cygwin bin) */

/* ch..root.conf next to the exe: "lowil <name>", "memory_mb <n>", '#' comments */
static void load_conf(void)
{
  char path[MAX_PATH], line[512], *p, *e; FILE *f; DWORD n;
  n=GetModuleFileNameA(NULL,path,sizeof(path));
  if(!n || n>=sizeof(path)) return;
  p=strrchr(path,'\\'); if(!p) return;
  snprintf(p+1,sizeof(path)-(p+1-path),"ch..root.conf");
  if(!(f=fopen(path,"r"))) return;
  while(fgets(line,sizeof(line),f)) {
    for(p=line; *p==' '||*p=='\t'; p++);
    if(*p=='#') continue;
    for(e=p+strlen(p); e>p && (e[-1]=='\n'||e[-1]=='\r'||e[-1]==' '||e[-1]=='\t'); ) *--e=0;
    if(strncmp(p,"lowil",5)==0 && (p[5]==' '||p[5]=='\t') && nlowil<MAX_LOWIL) {
      for(p+=6; *p==' '||*p=='\t'; p++);
      if(*p) snprintf(lowil[nlowil++],MAX_PATH,"%s",p);
    } else if(strncmp(p,"lowil_path",10)==0 && (p[10]==' '||p[10]=='\t')) {
      for(p+=11; *p==' '||*p=='\t'; p++);
      snprintf(lowil_path,sizeof(lowil_path),"%s",p);
    } else if(strncmp(p,"memory_mb",9)==0) {
      unsigned long v=strtoul(p+9,NULL,10); if(v>=16) memory_mb=v;
    }
  }
  fclose(f);
}

static int is_lowil(const char *exe)
{
  char full[MAX_PATH]; const char *base; int i;
  if(!GetFullPathNameA(exe,sizeof(full),full,NULL)) snprintf(full,sizeof(full),"%s",exe);
  base=strrchr(full,'\\'); base=base?base+1:full;
  { const char *b2=strrchr(base,'/'); if(b2) base=b2+1; }
  for(i=0;i<nlowil;i++)
    if(_stricmp(lowil[i],strchr(lowil[i],'\\')?full:base)==0) return 1;
  return 0;
}

static const char *env_rm[]={
  "s2_dir", "w_wims_home", "session_base_dir", "trusted_module",
  "session_dir", "wims_server_base", "w_httpd_PWD",
  "w_wims_sesdir", "HTTP_CONNECTION",
  "w_gnuplot_format", "w_insplot_font", "w_ins_anim_limit",
  "w_ins_density", "w_ins_format",
  "texgif_fontdir", "texgif_texheader",
  "w_wims_tmp_debug", "w_insmath_logic", "w_insmath_rawmath",
  "SERVER_ADMIN", "SERVER_ADDR", "SERVER_NAME", NULL
};

static void fail(const char *m, DWORD e)
{ fprintf(stderr,"ch..root: %s (err=%lu)\n",m,(unsigned long)e); fflush(stderr); }

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

static PSID make_appcontainer(const WCHAR *wname)
{
  HRESULT hr; PSID sid=NULL;
  hr=CreateAppContainerProfile(wname,wname,wname,NULL,0,&sid);
  if(hr==HRESULT_FROM_WIN32(ERROR_ALREADY_EXISTS)) {
    DeleteAppContainerProfile(wname);
    hr=CreateAppContainerProfile(wname,wname,wname,NULL,0,&sid);
  }
  if(FAILED(hr) || sid==NULL) { fail("CreateAppContainerProfile",(DWORD)hr); return NULL; }
  return sid;
}

/* add (mode=GRANT_ACCESS) or remove (REVOKE_ACCESS) an inheritable ACE */
static int set_ace(const char *dir, PSID sid, DWORD rights, ACCESS_MODE mode)
{
  EXPLICIT_ACCESS_A ea; PACL old=NULL, neu=NULL; PSECURITY_DESCRIPTOR sd=NULL; DWORD e;
  memset(&ea,0,sizeof(ea));
  ea.grfAccessPermissions=rights; ea.grfAccessMode=mode;
  ea.grfInheritance=SUB_CONTAINERS_AND_OBJECTS_INHERIT;
  ea.Trustee.TrusteeForm=TRUSTEE_IS_SID; ea.Trustee.TrusteeType=TRUSTEE_IS_UNKNOWN;
  ea.Trustee.ptstrName=(LPSTR)sid;
  e=GetNamedSecurityInfoA(dir,SE_FILE_OBJECT,DACL_SECURITY_INFORMATION,NULL,NULL,&old,NULL,&sd);
  if(e!=ERROR_SUCCESS) { fail("GetNamedSecurityInfo",e); return -1; }
  e=SetEntriesInAclA(1,&ea,old,&neu);
  if(e!=ERROR_SUCCESS) { fail("SetEntriesInAcl",e); LocalFree(sd); return -1; }
  e=SetNamedSecurityInfoA((LPSTR)dir,SE_FILE_OBJECT,DACL_SECURITY_INFORMATION,NULL,NULL,neu,NULL);
  LocalFree(neu); LocalFree(sd);
  if(e!=ERROR_SUCCESS) { fail("SetNamedSecurityInfo",e); return -1; }
  return 0;
}

/* Microsoft C runtime argv quoting (backslashes before quotes doubled) */
static size_t quote_arg(char *d, size_t cap, const char *a)
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

static int purge_profiles_ex(int stale, int verbose);

static int resolve_exe(const char *name, char *out, size_t cap)
{
  size_t l=strlen(name); DWORD a;
  snprintf(out,cap,"%s",name);
  if(l>=4 && _stricmp(name+l-4,".exe")==0) return 1;
  a=GetFileAttributesA(name);
  if(a!=INVALID_FILE_ATTRIBUTES && !(a&FILE_ATTRIBUTE_DIRECTORY)) return 1;
  snprintf(out,cap,"%s.exe",name);
  return 1;
}

static int launch(HANDLE tok, PSID ac_sid, char *const argv[], const char *cwd, SIZE_T memlim)
{
  static char cmd[32768]; size_t n=0; int i, ok; char exe0[MAX_PATH];
  STARTUPINFOEXA si; PROCESS_INFORMATION pi; SIZE_T asz=0;
  SECURITY_CAPABILITIES sc; LPPROC_THREAD_ATTRIBUTE_LIST al=NULL;
  JOBOBJECT_EXTENDED_LIMIT_INFORMATION li; HANDLE job; DWORD code=1;
  for(i=0;argv[i];i++) {
    const char *a=argv[i];
    if(i==0) { resolve_exe(a,exe0,sizeof(exe0)); a=exe0; }
    if(i) { if(n+1<sizeof(cmd)) cmd[n]=' '; n++; }
    n+=quote_arg(cmd+n,n<sizeof(cmd)?sizeof(cmd)-n:0,a);
  }
  if(n>=sizeof(cmd)) { fail("command line too long",0); return 2; }
  cmd[n]=0;
  job=CreateJobObjectA(NULL,NULL);
  if(!job) { fail("CreateJobObject",GetLastError()); return 2; }
  memset(&li,0,sizeof(li));
  li.BasicLimitInformation.LimitFlags=JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
       |JOB_OBJECT_LIMIT_ACTIVE_PROCESS|JOB_OBJECT_LIMIT_DIE_ON_UNHANDLED_EXCEPTION;
  li.BasicLimitInformation.ActiveProcessLimit=PROC_QUOTA;
  if(memlim) {
    li.BasicLimitInformation.LimitFlags|=JOB_OBJECT_LIMIT_JOB_MEMORY;
    li.JobMemoryLimit=memlim;
  }
  if(!SetInformationJobObject(job,JobObjectExtendedLimitInformation,&li,sizeof(li)))
    { fail("SetInformationJobObject",GetLastError()); CloseHandle(job); return 2; }
  memset(&sc,0,sizeof(sc)); sc.AppContainerSid=ac_sid;
  InitializeProcThreadAttributeList(NULL,1,0,&asz);
  al=(LPPROC_THREAD_ATTRIBUTE_LIST)malloc(asz);
  if(!al || !InitializeProcThreadAttributeList(al,1,0,&asz)) { CloseHandle(job); return 2; }
  if(ac_sid && !UpdateProcThreadAttribute(al,0,PROC_THREAD_ATTRIBUTE_SECURITY_CAPABILITIES,
                            &sc,sizeof(sc),NULL,NULL))
    { fail("UpdateProcThreadAttribute",GetLastError()); DeleteProcThreadAttributeList(al); free(al); CloseHandle(job); return 2; }
  memset(&si,0,sizeof(si)); si.StartupInfo.cb=sizeof(si);
  si.StartupInfo.dwFlags=STARTF_USESTDHANDLES;
  si.StartupInfo.hStdInput=GetStdHandle(STD_INPUT_HANDLE);
  si.StartupInfo.hStdOutput=GetStdHandle(STD_OUTPUT_HANDLE);
  si.StartupInfo.hStdError=GetStdHandle(STD_ERROR_HANDLE);
  si.lpAttributeList=al;
  ok=CreateProcessAsUserA(tok,NULL,cmd,NULL,NULL,TRUE,
                          CREATE_NO_WINDOW|EXTENDED_STARTUPINFO_PRESENT|CREATE_SUSPENDED,
                          NULL,cwd,&si.StartupInfo,&pi);
  DeleteProcThreadAttributeList(al); free(al);
  if(!ok) { fail("CreateProcessAsUser",GetLastError()); CloseHandle(job); return 127; }
  if(!AssignProcessToJobObject(job,pi.hProcess)) {
    fail("AssignProcessToJobObject",GetLastError());
    TerminateProcess(pi.hProcess,127); CloseHandle(pi.hThread); CloseHandle(pi.hProcess);
    CloseHandle(job); return 2;
  }
  ResumeThread(pi.hThread); CloseHandle(pi.hThread);
  WaitForSingleObject(pi.hProcess,INFINITE);
  GetExitCodeProcess(pi.hProcess,&code); CloseHandle(pi.hProcess);
  TerminateJobObject(job,137); CloseHandle(job);  /* leftover grandchildren */
  return (int)code;
}

/* run argv in a fresh container; session dir granted for the run only */
static int sandbox_run(char *const argv[], const char *dir)
{
  HANDLE tok; PSID sid; WCHAR wname[96]; int r, granted=0;
  _snwprintf(wname,96,L"wims.chroot.%lu.%lu",GetCurrentProcessId(),GetTickCount());
  wname[95]=0;
  purge_profiles_ex(1,0);
  tok=make_restricted(); if(!tok) return 2;
  sid=make_appcontainer(wname); if(!sid) { CloseHandle(tok); return 2; }
  if(dir && *dir && GetFileAttributesA(dir)!=INVALID_FILE_ATTRIBUTES)
    granted=set_ace(dir,sid,GENERIC_ALL,GRANT_ACCESS)==0;
  /* cwd = session dir (the Linux wrapper chdirs into the chroot): the
   * caller's cwd (public_html) is not readable from the container */
  r=launch(tok,sid,argv,granted?dir:NULL,0);
  if(granted) set_ace(dir,sid,GENERIC_ALL,REVOKE_ACCESS);
  FreeSid(sid); CloseHandle(tok);
  DeleteAppContainerProfile(wname);
  return r;
}

/* low (or, with low=0, no) mandatory label on dir, inherited (OI|CI) */
static int set_low_label(const char *dir, int low)
{
  PSECURITY_DESCRIPTOR sd=NULL; PACL sacl=NULL; BOOL pres=FALSE, def=FALSE; DWORD e;
  ACL empty;
  if(low) {
    if(!ConvertStringSecurityDescriptorToSecurityDescriptorA("S:(ML;OICI;NW;;;LW)",
         SDDL_REVISION_1,&sd,NULL)) { fail("label SDDL",GetLastError()); return -1; }
    GetSecurityDescriptorSacl(sd,&pres,&sacl,&def);
  } else {
    InitializeAcl(&empty,sizeof(empty),ACL_REVISION); sacl=&empty;
  }
  e=SetNamedSecurityInfoA((LPSTR)dir,SE_FILE_OBJECT,LABEL_SECURITY_INFORMATION,NULL,NULL,NULL,sacl);
  if(sd) LocalFree(sd);
  if(e!=ERROR_SUCCESS) { fail("SetNamedSecurityInfo(label)",e); return -1; }
  return 0;
}

static PSID current_user_sid(BYTE *buf, DWORD cap)
{
  HANDLE t; DWORD n=0; PSID r=NULL;
  if(!OpenProcessToken(GetCurrentProcess(),TOKEN_QUERY,&t)) return NULL;
  if(GetTokenInformation(t,TokenUser,buf,cap,&n)) r=((TOKEN_USER*)buf)->User.Sid;
  CloseHandle(t); return r;
}

/* D21 "lowil" policy: restricted token at low integrity + Job (processes,
 * memory, kill-on-close), no AppContainer; session dir labelled low for the
 * run, TEMP/TMP pointed at it (the profile temp is not writable at low IL) */
static int lowil_run(char *const argv[], const char *dir)
{
  HANDLE tok; PSID low=NULL, user=NULL; TOKEN_MANDATORY_LABEL ml; int r, labelled=0;
  BYTE ubuf[SECURITY_MAX_SID_SIZE+sizeof(TOKEN_USER)];
  tok=make_restricted(); if(!tok) return 2;
  if(!ConvertStringSidToSidA("S-1-16-4096",&low)) { fail("low IL SID",GetLastError()); CloseHandle(tok); return 2; }
  memset(&ml,0,sizeof(ml)); ml.Label.Sid=low; ml.Label.Attributes=SE_GROUP_INTEGRITY;
  if(!SetTokenInformation(tok,TokenIntegrityLevel,&ml,sizeof(ml)+GetLengthSid(low)))
    { fail("SetTokenInformation(low IL)",GetLastError()); LocalFree(low); CloseHandle(tok); return 2; }
  LocalFree(low);
  if(*lowil_path) {
    static char np[32768]; char *op=getenv("PATH");
    snprintf(np,sizeof(np),"%s;%s",lowil_path,op?op:"");
    SetEnvironmentVariableA("PATH",np);
  }
  if(dir && *dir && GetFileAttributesA(dir)!=INVALID_FILE_ATTRIBUTES) {
    /* relabelling needs WRITE_OWNER, which "Modify" ACLs do not give: the
     * owner (WRITE_DAC) adds it for itself for the run */
    user=current_user_sid(ubuf,sizeof(ubuf));
    if(user) set_ace(dir,user,WRITE_OWNER,GRANT_ACCESS);
    if(set_low_label(dir,1)) { if(user) set_ace(dir,user,WRITE_OWNER,REVOKE_ACCESS); CloseHandle(tok); return 2; }
    labelled=1;
    SetEnvironmentVariableA("TEMP",dir); SetEnvironmentVariableA("TMP",dir);
  }
  r=launch(tok,NULL,argv,labelled?dir:NULL,(SIZE_T)memory_mb<<20);
  if(labelled) { set_low_label(dir,0); if(user) set_ace(dir,user,WRITE_OWNER,REVOKE_ACCESS); }
  CloseHandle(tok);
  return r;
}

static int grant_rx(int n, char **dirs)
{
  PSID all=NULL; int i, rc=0;
  if(!ConvertStringSidToSidA("S-1-15-2-1",&all)) { fail("ALL APPLICATION PACKAGES SID",GetLastError()); return 1; }
  for(i=0;i<n;i++) {
    if(set_ace(dirs[i],all,GENERIC_READ|GENERIC_EXECUTE,GRANT_ACCESS)) rc=1;
    else printf("granted RX to ALL APPLICATION PACKAGES: %s\n",dirs[i]);
  }
  LocalFree(all); return rc;
}

/* stale=1: only profiles "wims.chroot.<pid>.<tick>" whose wrapper process is
 * gone (killed with its job by wims before it could clean up) */
static int purge_profiles_ex(int stale, int verbose);
static int purge_profiles(void) { return purge_profiles_ex(0,1); }

static int pid_alive(unsigned long pid)
{
  HANDLE h=OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION,FALSE,pid); DWORD c=0;
  if(!h) return GetLastError()==ERROR_ACCESS_DENIED;
  if(!GetExitCodeProcess(h,&c)) c=STILL_ACTIVE;
  CloseHandle(h); return c==STILL_ACTIVE;
}

static int purge_profiles_ex(int stale, int verbose)
{
  HKEY k; DWORD i=0, n; char sub[256]; WCHAR mon[256]; int cnt=0;
  const char *key="Software\\Classes\\Local Settings\\Software\\Microsoft\\Windows\\CurrentVersion\\AppContainer\\Mappings";
  static WCHAR names[4096][96]; int nn=0, j;
  if(RegOpenKeyExA(HKEY_CURRENT_USER,key,0,KEY_READ,&k)!=ERROR_SUCCESS) return 0;
  for(i=0;;i++) {
    HKEY s2; DWORD sz=sizeof(mon); n=sizeof(sub);
    if(RegEnumKeyExA(k,i,sub,&n,NULL,NULL,NULL,NULL)!=ERROR_SUCCESS) break;
    if(RegOpenKeyExA(k,sub,0,KEY_READ,&s2)!=ERROR_SUCCESS) continue;
    if(RegQueryValueExW(s2,L"Moniker",NULL,NULL,(BYTE*)mon,&sz)==ERROR_SUCCESS
       && wcsncmp(mon,L"wims.chroot.",12)==0 && nn<4096
       && (!stale || (wcsncmp(mon,L"wims.chroot.selftest",20)!=0
                      && !pid_alive(wcstoul(mon+12,NULL,10))))) {
      mon[95]=0; wcscpy(names[nn++],mon);
    }
    RegCloseKey(s2);
  }
  RegCloseKey(k);
  for(j=0;j<nn;j++) if(SUCCEEDED(DeleteAppContainerProfile(names[j]))) cnt++;
  if(verbose) printf("deleted %d wims.chroot.* AppContainer profile(s)\n",cnt);
  return 0;
}

/* Apache (mod_cgi) gives CGIs a minimal environment (PATH, SystemRoot...):
 * without USERPROFILE/LOCALAPPDATA, CreateProcessAsUser of an AppContainer
 * process fails with ERROR_ENVVAR_NOT_FOUND (203), so every CAS was refused
 * under Laragon while direct runs worked. Rebuild the per-user variables
 * from the process token when they are missing. */
static void ensure_profile_env(void)
{
  char prof[MAX_PATH], b[MAX_PATH+32]; DWORD n=sizeof(prof); HANDLE tk;
  if(GetEnvironmentVariableA("USERPROFILE",prof,sizeof(prof))==0) {
    if(!OpenProcessToken(GetCurrentProcess(),TOKEN_QUERY,&tk)) return;
    if(!GetUserProfileDirectoryA(tk,prof,&n)) { CloseHandle(tk); return; }
    CloseHandle(tk); SetEnvironmentVariableA("USERPROFILE",prof);
  }
  if(GetEnvironmentVariableA("LOCALAPPDATA",b,sizeof(b))==0)
    { snprintf(b,sizeof(b),"%s\\AppData\\Local",prof); SetEnvironmentVariableA("LOCALAPPDATA",b); }
  if(GetEnvironmentVariableA("APPDATA",b,sizeof(b))==0)
    { snprintf(b,sizeof(b),"%s\\AppData\\Roaming",prof); SetEnvironmentVariableA("APPDATA",b); }
  snprintf(b,sizeof(b),"%s\\AppData\\Local\\Temp",prof);
  { char t[8]; if(GetEnvironmentVariableA("TEMP",t,sizeof(t))==0) SetEnvironmentVariableA("TEMP",b);
    if(GetEnvironmentVariableA("TMP",t,sizeof(t))==0) SetEnvironmentVariableA("TMP",b); }
}

int main(int argc, char **argv)
{
  const char *dir; int i;
  if(argc<2) return 0;
  ensure_profile_env();
  if(strcmp(argv[1],"--noop")==0) return 0;
  if(strcmp(argv[1],"--selftest")==0) {
    char self[MAX_PATH]; char *av[3];
    if(!GetModuleFileNameA(NULL,self,sizeof(self))) return 2;
    av[0]=self; av[1]="--noop"; av[2]=NULL;
    return sandbox_run(av,NULL)==0?0:2;
  }
  if(strcmp(argv[1],"--grant-rx")==0) return grant_rx(argc-2,argv+2);
  if(strcmp(argv[1],"--purge-profiles")==0) return purge_profiles();
  if(strcmp(argv[1],"cleantmpdir")==0) return 0;
  load_conf();
  if(strcmp(argv[1],"--policy")==0) {
    char e[MAX_PATH]; if(argc<3) return 2;
    resolve_exe(argv[2],e,sizeof(e));
    printf("%s\n",is_lowil(e)?"lowil":"appcontainer"); return 0;
  }
  i=1; if(strcmp(argv[1],"--cmd")==0) i=2;
  if(i>=argc) { fprintf(stderr,"usage: ch..root.exe <prog> [args...] | --selftest | --grant-rx <dir>...\n"); return 2; }
  /* "& sh|perl <code>" (also after --cmd): untrusted shell, refused */
  if(argv[i][0]=='&') {
    fprintf(stderr,"ch..root: untrusted %s refused on Windows.\n",i+1<argc?argv[i+1]:"exec");
    return 127;
  }
  dir=getenv("TMPDIR"); if(!dir || !*dir) dir=getenv("tmp_dir");
  { static char d[MAX_PATH];
    /* absolute, since the child runs in it: $TMPDIR/x must stay valid */
    if(dir && GetFullPathNameA(dir,sizeof(d),d,NULL)>0) {
      dir=d; SetEnvironmentVariableA("TMPDIR",d); SetEnvironmentVariableA("tmp_dir",d);
    } else dir=NULL; }
  { int k; for(k=0; env_rm[k]; k++) SetEnvironmentVariableA(env_rm[k],NULL); }
  { char e[MAX_PATH]; resolve_exe(argv[i],e,sizeof(e));
    if(is_lowil(e)) return lowil_run(argv+i,dir); }
  return sandbox_run(argv+i,dir);
}
