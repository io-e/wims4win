/*    Copyright (C) 1998-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/* Web exerciser */

#include "wims.h"
enum Name_Modes NAME_MODES;
enum Ro_Names RO_NAMES;
enum _commands COMMANDS;
WORKING_FILE m_file, svar_file, mcache[MAX_MCACHE];
struct VAR_DEF var_def[MAX_VAR_NUM];
char examlogf[MAX_FNAME+1];
char mpboundary[1024];
char *stdinbuf;
struct user_variable user_variable[MAX_VAR_NUM];
char available_lang[MAX_LANGUAGES][4];

struct {
  char *name;
  char *font;
} charname[]={
  {"en","windows-1252"},
  {"fr","windows-1252"},
  {"es","windows-1252"},
  {"cn","gb2312"},
  {"de","windows-1252"},
  {"it","windows-1252"},
  {"nl","windows-1252"},
  {"si","iso-8859-2"},
  {"ar","iso-8859-6"},
  {"tw","big5"},
  {"pt","windows-1252"},
  {"ca","windows-1252"},
  {"pt","windows-1252"},
  {"ru","iso-8859-5"},
  {"ty","iso-8859-13"}
};

#define charname_no (sizeof(charname)/sizeof(charname[0]))

/* left to right or right to left writing */
struct {
  char *name;
  char *dirn;
} dirnname[]={
  {"en","ltr"},
  {"fr","ltr"},
  {"es","ltr"},
  {"cn","ltr"},
  {"de","ltr"},
  {"it","ltr"},
  {"nl","ltr"},
  {"si","ltr"},
  {"ar","rtl"},
  {"tw","ltr"},
  {"pt","ltr"},
  {"ca","ltr"},
  {"pt","ltr"},
  {"ru","ltr"},
  {"ty","ltr"}

};

#define dirnname_no (sizeof(dirnname)/sizeof(dirnname[0]))


#define evalue strevalue
char *robot_session="../tmp/robot";
int robot_access=0,human_access=0;
int user_error_nolog=0;
char *good_agent[]={
  "Mozilla","Netscape","Opera","WIMS",
  "MSIE","Konqueror","Java"
};
#define good_agent_no (sizeof(good_agent)/sizeof(good_agent[0]))

char *bad_agent[]={ /* These are really bad agents: prohibited. */
  "HTTrack","MemoWeb","Teleport","Offline","Wget","eCatch",
  "Powermarks","EmailSiphon", "WebCopier"
};
#define bad_agent_no (sizeof(bad_agent)/sizeof(bad_agent[0]))

/* used for debugging */
int debug=0;

char class_dir[MAX_FNAME+1]; /* directory name of this class */

struct user_variable user_variable[MAX_VAR_NUM];
int user_var_no;

struct VAR_DEF var_def[MAX_VAR_NUM];
int defined_var_total;

/* Destinated to module error messages */
WORKING_FILE m_file,svar_file,mcache[MAX_MCACHE];
int mcachecnt;

/* Limit for any data working files. */
int WORKFILE_LIMIT=2048*1024;

/* whether the user has defined language */
int user_lang=0;

/* for instex grouping */
int instex_cnt=0, getwimstexsize=1;
char instex_src[MAX_LINELEN+1], instex_fname[MAX_LINELEN+1];
char *instex_processor="tex..gif";

/* Cookie management */
char cookiegot[256], cookieset[256], cookieheader[64]="WIMSUSER=";

/* multipart boundary */
char mpboundary[1024];
int  deplen=0; /* length of deposit */

int confset=0; /* set to 1 if setvar for config */

/* Operating mode: default, popup, raw, etc. */

int mode=mode_default;

/* Switch; notice subroutines wherether we are outputing. */
int outputing;

char *home_module="home"; /* name of home module */
extern char **environ;      /* table of environment variables */
int module_defined=0;

        /* directory prefix buffers */
char session_prefix[MAX_FNAME+1], s2_prefix[MAX_FNAME+1], module_prefix[MAX_FNAME+1];

char *stdinbuf;
int sesrandomtab[MAX_SESRANDOM]; /* session random values */
char multiexec_random[64];
int executed_gotos; /* number of goto's executed. to check with GOTO_LIMIT. */
int insert_no; /* number of instex, insplot, insPLOT. */
int output_length; /* length of the output. */
int isexam=0; /* non-zero if request is exam */
int simuxam=0; /* exam is simulation */
int is_multiexec=0; /* for execredirected */
int multiexec_index;
int direct_datafile=0;
int exec_is_module=0;
  /* root directory of modules */
char *module_dir="modules";
int new_session=0; /* =1 if new session created */
int good_httpd=0; /* Whether the http server is intelligent */
/* int internal_sql=0; */ /* for internal sql use */
int direct_exec=0; /* calc routine is exected by exec if 1 */
int print_precision=8; /* precision when printing real numbers */
int session_serial; /* request serial for session control */
int form_access=0; /* identifies form access, for robot identification */
int lastout_file=-1; /* file to hold last output */
char *instex_style=""; /* "": text "$": math "$$": displaymath */
int instex_usedynamic=0; /* always dynamic if 1 */
int wrapexec=0; /* if set to 1, change uid (nobody) to euid (wims).
                 * if set to -1, change euid to uid when exec(). */
int parm_restore=0; /* Restoring saved parameters? */
int exec_wait=1; /* whether to wait for fork return */
int execnt=0; /* count executions */
int readnest; /* nested read count */
int mfilecnt=0; /* count working files */
int forceresume=0; /* force user to resume old request */
int manageable=0; /* whether the connection may be site manager
                   * 0: no; 1: maybe; 2: sure */
int ismhelp=0;  /* 1 if session is in mhelp. */
int getvar_len; /* length of the last-got variable. */
int noout=0; /* if set to 1 then output is skipped */
char tmp_dir[MAX_FNAME+1]; /* temporary directory */
char *bin_dir="bin"; /* directory containing executable scripts and programs. */
char cwdbuf[MAX_FNAME+1]; /* store current working directory */
char var_hacking=0; /* Trying to hack a variable? */
char *tmp_debug="no";
char *tmp_debug_var="";
char *tmp_debug_use_var="";
char ins_alt[MAX_LINELEN+1]; /* dynamic insertion alternative text */
char *devel_modules="close"; /* whether to open devel modules */
int isclassmodule=0; /* 1 if the module is class module */
int isdevelmodule=0; /* development module? */
int setcookie=0; /* 1 if need to set cookie */
int killpid=0; /* pid of process to kill by alarm */
char *mathalign_sup1, *mathalign_sup2; /* see mathalign_base */
int substnest=0; /* nesting level of substit() */
int exodepOK=1;
long int startmtime; /* start time in milliseconds */
long int startmtime2; /* start time in microseconds */
int backslash_insmath=0; /* \(...) substitution? */
char examlogf[MAX_FNAME+1]; /* examlog file name */
char examlogd[MAX_FNAME+1]; /* examlog directory name */
char freeworklogf[MAX_FNAME+1]; /* freeworklog file name */
char freeworklogd[MAX_FNAME+1]; /* freeworklog directory name */
char exam_sheetexo[32]; /* sheet data of an exam */
char loadavg[64];
/* user file variable access control. */
char *var_readable, *var_writable, *var_nr, *var_nw, *var_pfx;
int hostcquota;
int var_noexport; /* do not export variable */

char tmplbuf[MAX_LINELEN+1]; /* for temporary uses not thru subroutines. */

struct tm *now, Now; /* time of request */
time_t nowtime, limtime, limtimex;
char nowstr[32];

/* Resource limits. Capital names are reserved by system. */
int rlimit_cpu=20;  /* cpu time in seconds */
int rlimit_fsize=8388608;/* file size */
int rlimit_as=614457600;/* virtual memory size */
int rlimit_data=2004857600;/* data segment size; maxima requires a lot (must be lower than rlimit_as)*/
int rlimit_stack=2097152;/* stack size */
int rlimit_core=0; /* core dump size */
int rlimit_rss=16777216; /* resident size */
int rlimit_nproc=1024; /* number of processes */
int rlimit_nofile=512; /* number of open files */
int rlimit_memlock=2097152;/* locked-in-memory address space */

char *var_str; /* malloc'ed buffer to hold translated query_string */

/* buffer to hold module's variable definition file, malloc'ed. */
char *var_def_buf;

/* job_identifier is even a reserved variable name */
char job_identifier[32];

/* site manager definition IPv4 IPv6*/
char *manager_site="127.0.0.1 ::1";
int   manager_https=0;

/* sheet and exercise information */
int wims_sheet=0,wims_exo=0;

/* Form method: get or post */
char *default_form_method="post";

/* Je suis maintenant oblige de passer a l'anglais
 * pour la langue de defaut.
 */
char lang[16]="en";
char available_lang[MAX_LANGUAGES][4]={"en","fr"};
int available_lang_no=2;
char pre_language[4]="";
FILE *trace_file;
int trace_indent=0;
char *protocol="http"; /* http or https */

/* check for coordinate input. This will mean that
 * the request is manual, but not robot.
 */
int coord_input=0;

/* These are readonly environment variable names
 * special parm used for special cmds (getins, etc).
 */
char *ro_name[]={
  "cmd",
  "empty",
  "lang",
  "module",
  "session",
  "special_parm",
  "special_parm2",
  "special_parm3",
  "special_parm4",
  "useropts" ,
  "wims_session",
  "wims_subsession",
  "wims_window",
  "worksheet"
};

int RO_NAME_NO=(sizeof(ro_name)/sizeof(ro_name[0]));

int cmd_type;
char *commands[]={
    "intro" , "new" , "renew" , "reply" , "config" , "hint" , "help" ,
      "resume", "next", "getins", "getframe", "getfile", "close", "ref"
};

int CMD_NO=(sizeof(commands)/sizeof(commands[0]));

/* stat=0: saved variables
 * all names starting with wims_priv_ are also internal.
 */
struct internal_name internal_name[]={
  {"accessright", 1}, /* right to access commercial resources */
  {"caller", 1}, /* caller session */
  {"check", 1}, /* for exam check use */
  {"class", 1},
  {"class_examlog", 1},
  {"class_exolog", 1},
  {"class_limit", 1},
  {"class_quota", 1},
  {"class_regpass", 1},
  {"class_user_limit", 1},
  {"classdir", 1},
  {"classname", 1},
  {"devel_modules", 1},
  {"developer", 1},
  {"doc_quota", 1},
  {"doc_regpass", 1},
  {"email", 1},
  {"exo", 0}, /* exercise number */
  {"exoption", 1}, /* exercise option */
  {"exotrymax", 0},
  {"firstname", 1},
  {"forum_limit", 1},
  {"fwexo",0},   /*numero d'exo dans un freework */
  {"fwnumber",0 },   /* numero de freework */
  {"home", 1},
  {"institutionname", 1},
  {"isexam", 0}, /* whether the sheet is an exam sheet */
  {"ismanager", 0},
  {"lastname", 1},
  {"mode", 0}, /* operating mode */
  {"module_start_time", 0},
  {"now", 1}, /* date and time, yyyymmdd.hh:mm:ss */
  {"nowseconds", 1}, /* date and time, seconds since EPOCH */
  {"nr", 1}, /* non-readable variables in user file, words */
  {"nw", 1}, /* non-writable variables in user file, words */
  {"otherclass", 1}, /* Remember other logins */
  {"participate", 1}, /* superclass definition */
  {"prefix", 1}, /* user file prefix */
  {"protocol", 0}, /* http protocol */
  {"rafale", 0}, /* rapidfire request information */
  {"readable", 1}, /* readable variables in user file, words */
  {"realuser", 1}, /* real user for supervisor in gateway */
  {"req_time", 0}, /* time of the request */
  {"sclassdir", 1},
  {"scorereg", 0}, /* score registration flag */
  {"seed", 0},
  {"seed_repeat",0},
  {"seed_score", 0},
  {"seedcnt",0},
  {"seedlastcnt",0},
  {"sequence", 0}, /* sequence number */
  {"sescookie", 1}, /* session cookie */
  {"sesdir", 1},
  {"session_serial", 0}, /* request serial in the session */
  {"session_start_time", 0},
  {"sheet", 0}, /* sheet number */
  {"sup_secure", 1}, /* secure level of supervisor */
  {"superclass", 1}, /* superclass code */
  {"superclass_quota", 1},
  {"supertype", 1}, /* superclass type */
  {"supervise", 1}, /* superclass definition */
  {"supervisor", 1}, /* real name of the supervisor */
  {"supervisormail",1}, /* email of supervisor */
  {"trustfile", 1}, /* trusted files in special adm modules */
  {"useropts", 1}, /* user options */
  {"writable", 1}, /* writable variables in user file, words */
};
int INTERNAL_NAME_NO=(sizeof(internal_name)/sizeof(internal_name[0]));

char *httpd_vars[]={
  "HTTP_ACCEPT",
  "HTTP_ACCEPT_CHARSET",
  "HTTP_ACCEPT_LANGUAGE",
  "HTTP_COOKIE",
  "HTTP_HOST",
  "HTTP_USER_AGENT",
  "HTTP_X_REQUESTED_WITH",
  "HTTPS",
  "QUERY_STRING",
  "REMOTE_HOST",
  "REMOTE_ADDR",
  "REMOTE_PORT",
  "REQUEST_METHOD",
  "SCRIPT_NAME",
  "SERVER_NAME",
  "SERVER_SOFTWARE",
  "SERVER_PROTOCOL"
};
#define HTTPD_VAR_NO (sizeof(httpd_vars)/sizeof(httpd_vars[0]))

/* security: these variables will not be visible to child processes */
char *unsetvars[]={
  "DOCUMENT_ROOT","SERVER_SIGNATURE","SERVER_SOFTWARE",
      "UNIQUE_ID","HTTP_KEEP_ALIVE","SSL_SESSION_ID"
};
#define unsetvarcnt (sizeof(unsetvars)/sizeof(unsetvars[0]))

int httpd_type=httpd_apache;

char *remote_addr=""; /* storing for performance */
char *remote_host="";

char ref_name[2048], ref_base[2048];

void put_special_page(char *pname);
void useropts(void);

/* Make certain httpd variables readable by modules */
void take_httpd_vars(void)
{
  int i;
  char *p, buf[MAX_NAMELEN+1];
  var_noexport=1;
  for(i=0;i<HTTPD_VAR_NO;i++) {
   snprintf(buf,sizeof(buf),"httpd_%s",httpd_vars[i]);
   if((p=getenv(httpd_vars[i]))!=NULL) setvar(buf,p);
  }
  var_noexport=0;

  for(i=0;i<unsetvarcnt;i++) unsetenv(unsetvars[i]);
   /* IPv4 IPv6*/
  p=getenv("REMOTE_ADDR");if(p!=NULL && (strcmp(p,"127.0.0.1")==0 || strcmp(p,"::1")==0)) human_access=1;
  p=getenv("HTTP_REFERER"); if(p!=NULL && *p!=0) setvar("wims_referer",p);
}

/* cookie2session */
void cookie2session(void)
{
  char cksession[64], psession[32], *ckey, *p;
  char nbuf[MAX_FNAME+1];

  if(mode==mode_popup) return;
  if(cookiegot[0]==0) {
   ckset: cookiegot[0]=0; setcookie=1; return;
  }
  p=getvar("special_parm");
  if(p!=NULL && strcmp(p,"ignorecookie")==0) return;
  mystrncpy(cksession,cookiegot,sizeof(cksession));
  ckey=strchr(cksession,'-');
  if(ckey==NULL) goto ckset; else *ckey++=0;
  p=getvar("wims_session"); if(p==NULL) p="";
  if(strstr(p,"new")!=NULL) goto ckset;
  mystrncpy(psession,p,sizeof(psession));
  p=strchr(psession,'_'); if(p!=NULL) *p=0;
  if(psession[0]!=0) {
    if(strcmp(psession,cksession)==0) return;
    if(session_exists(psession)) goto ckset;
    if(session_exists(cksession)) goto change;
  }
  else {
    if(!session_exists(cksession)) return;
    change:
    p=getenv("HTTPS");
    if(p!=NULL && strcasecmp(p,"on")==0) goto ckset;
    mkfname(nbuf,"%s/%s/var",session_dir,cksession);
    getdef(nbuf,"w_wims_ismanager",tmplbuf);
    if(tmplbuf[0]!=0 && tmplbuf[0]!='0') goto ckset;
    getdef(nbuf,"w_wims_protocol",tmplbuf);
    if(strcasecmp(tmplbuf,"https")==0) goto ckset;
    mkfname(nbuf,"%s/%s/var.stat",session_dir,cksession);
    getdef(nbuf,"wims_user",tmplbuf);
    if(tmplbuf[0]!=0) goto ckset;
    force_setvar(ro_name[ro_session],cksession);
    setsesdir(cksession);
    force_setvar("wims_subsession","");
    session_serial=0;
  }
}

void determine_font(char *l)
{
  int i;

  if(l==NULL || *l==0) return;
  for(i=0;i<charname_no && memcmp(charname[i].name,l,2);i++);
  if(i<charname_no) setvar("wims_main_font",charname[i].font);
}

void determine_dirn(char *l)
{
  int i;

  if(l==NULL || *l==0) return;
  for(i=0;i<dirnname_no && memcmp(dirnname[i].name,l,2);i++);
  if(i<dirnname_no) setvar("wims_main_dirn",dirnname[i].dirn);
}

void predetermine_language(void)
{
  char *p;
  int i,n;

  if(pre_language[0]!=0) p=pre_language;
  else p=getenv("HTTP_ACCEPT_LANGUAGE");
  if(p!=NULL && strlen(p)>=2) {
    for(i=0;i<available_lang_no && memcmp(p,available_lang[i],2)!=0;i++);
    if(i<available_lang_no) goto lend;
  }
  p=getenv("HTTP_USER_AGENT");
  if(p!=NULL && strlen(p)>=5) {
    char *q;
    if((q=strchr(p,'['))!=NULL && islower(*(q+1)) && islower(*(q+2)) && *(q+3)==']') {
        char bb[4];
        bb[0]=*(q+1);bb[1]=*(q+2);bb[2]=0;
        for(i=0;i<available_lang_no && memcmp(bb,available_lang[i],2)!=0;i++);
        if(i<available_lang_no) {
          memmove(lang,bb,2); lang[2]=0;
          goto lend2;
        }
    }
  }
  p=getenv("HTTP_HOST"); if(p==NULL) goto lend2;
  n=strlen(p); if(n<=3 || *(p+n-3)!='.') goto lend2;
  p=p+n-2;
  for(i=0;i<available_lang_no && memcmp(p,available_lang[i],2)!=0;i++);
  if(i<available_lang_no) {
    lend: memmove(lang,p,2); lang[2]=0;
    lend2: determine_font(lang);determine_dirn(lang);
  }
}

/* print a special page */
void put_special_page(char *pname)
{
  determine_font(lang);
  determine_dirn(lang);
  phtml_put_base(mkfname(NULL,"%s.phtml.%s",pname,lang),0);
  write_logs();free(var_str);
}

/* check whether the connection is a site manager. */
void manager_check(void)
{
  char *p, *pp, buf[16];
  struct stat confstat;
  int i;

  manageable=0;
  if(robot_access || *manager_site==0 || checkhost(manager_site)<1)
    goto mend;
  if(manager_https) {
    p=getenv("HTTPS");
    if(p==NULL || strcmp(p,"on")!=0) goto mend;
  }
/* IPv4 IPv6*/
  if(strcmp(remote_addr,"127.0.0.1")==0 || strcmp(remote_addr,"::1")==0) {
    int port, port2;
    char tester[128];
    p=getenv("REMOTE_PORT"); if(p==NULL) goto mend;
    port=atoi(p); if(port<1024 || port>65535) goto mend;
    p=getenv("SERVER_PORT"); if(p==NULL) goto mend;
    port2=atoi(p); if(port2>=10000 || port2<=0) goto mend;
/* this is very non-portable */
    manageable=1;
    accessfile(tmplbuf,"r","/proc/net/tcp");
    snprintf(tester,sizeof(tester)," 0100007F:%04X 0100007F:%04X ",
           port,port2);
    p=strstr(tmplbuf,tester);
    if(p!=NULL) {
      pp=strchr(p,'\n'); if(pp!=NULL) *pp=0;
      if(strlen(p)>=75) {
        p=find_word_start(p+70); *find_word_end(p)=0;
        if(atoi(p)==geteuid()) manageable=2;
      }
    }
  }
  else manageable=1;
  i=stat(config_file,&confstat);
  if(i==0 && manageable>0 && (confstat.st_mode&(S_IRWXO|S_IRWXG))!=0) manageable=-1;
  if(manageable>0 && !trusted_module()) manageable=0;
  if(manageable==1) {
    accessfile(tmplbuf,"r","%s/.manager",session_prefix);
    if(strstr(tmplbuf,"yes")!=NULL) manageable=2;
  }
  if(manageable==1) {
    p=getvar(ro_name[ro_module]);
    if(p!=NULL && strncmp(p,"adm/manage",strlen("adm/manage"))==0) {
      struct stat pstat;
      if(stat("../log/.wimspass",&pstat)==0) {
        if((S_IFMT&pstat.st_mode)!=S_IFREG ||
           ((S_IRWXO|S_IRWXG)&pstat.st_mode)!=0)
          manageable=-2;
      }
    }
  }
  mend:
  mystrncpy(buf,int2str(manageable),sizeof(buf));
  force_setvar("wims_ismanager",buf);
  if(manageable>=2) {
    struct rlimit rlim;
    rlimit_cpu*=10;
    rlim.rlim_cur=rlim.rlim_max=rlimit_cpu;
    setrlimit(RLIMIT_CPU,&rlim);
    mystrncpy(buf,int2str(rlimit_cpu),sizeof(buf));
    setvar("wims_cpu_limit",buf);
    initalarm();
  }
}

/* check for robot access */
void robot_check(void)
{
  char *ua, *p, *ses, *c, *mod;
  int i;

  if(human_access) return;
  mod=getvar(ro_name[ro_module]);
  if(mod!=NULL && strcmp(mod,"adm/raw")==0) return;
  ses=getvar(ro_name[ro_session]);
/* user has valid session; OK */
  if(ses!=NULL && strncmp(ses,robot_session,strlen(robot_session))!=0
     && strchr(ses,'/')==NULL
     && ftest(mkfname(NULL,"%s/%s",s2_dir,ses))==is_dir)
    return;
  ua=getenv("HTTP_USER_AGENT"); if(ua==NULL) ua="";
  ua=find_word_start(ua);
  if(strncasecmp(ua,"Mozilla",strlen("Mozilla"))==0 &&
     (p=strstr(ua,"compatible"))!=NULL)
    ua=find_word_start(find_word_end(p));
  if(*ua) {
    for(i=0;i<good_agent_no
        && strncasecmp(ua,good_agent[i],strlen(good_agent[i]));i++);
    if(i<good_agent_no) return;
    for(i=0;i<bad_agent_no
        && strstr(ua,bad_agent[i])==NULL;i++);
    if(i<bad_agent_no) user_error("trapped");
  }
  force_setvar(ro_name[ro_session],robot_session);
  setsesdir(robot_session);
  c=getvar(ro_name[ro_cmd]);
  robot_access=1;
  if(c!=NULL && strcmp(c,"new") && strcmp(c,"intro")) {
    force_setvar(ro_name[ro_cmd],"robot_error");
    nph_header(450); put_special_page("robot");
    flushoutput(); flushlog(); exit(0);
  }
}

/* type=0: ordinary; type=1: multipart/form-data */
void parse_query_string(int len, int type)
{
  int i,j,l,v,cmd_defined;
  int parenth=-1, ll, lb, dlen;
  char *start, *p, *p1, *pt, *b1="";

  cmd_defined=0;
  setvar("wims_subsession","");
  ll=lb=0;
  if(type) {
    ll=strlen(mpboundary);
    start=strstr(var_str,mpboundary);
    if(start==NULL) start=var_str+strlen(var_str);
    if(strstr(var_str,"\r\n\r\n")!=NULL) b1="\r\n\r\n";
    else b1="\n\n";
    lb=strlen(b1);
  }
  else start=var_str;
  for(v=0, p1=start;p1<var_str+len;p1+=l) {
    if(type) {
      char *p2, *p3, *p4, *p5;
      p2=p1+ll; p3=memstr(p2,mpboundary,var_str+len-p2); l=p3-p1;
      p=memstr(p2,b1,var_str+len-p2); if(p>=p3) continue;
      p+=lb;
      if(p3<var_str+len) {
        while(*p3!='\n' && p3>p2) p3--;
        *p3=0;
        p3--; if(*p3=='\r') *p3=0;
      }
      dlen=p3-p;
      p2=memstr(p2,"name=",p3-p2); if(p2>=p3) continue;
      p2+=strlen("name="); if(*p2=='"') p2++;
      for(p3=p2; myisalnum(*p3) || strchr("._",*p3)!=NULL; p3++);
      if(p3==p2) continue;
        if(p3-p2==strlen("wims_deposit") &&
           strncmp(p2,"wims_deposit",p3-p2)==0) {
          p4=memstr(p1,"filename=",p-p1); if(p4<p) {
            p4+=strlen("filename="); if(*p4=='"') {
              p4++; p5=strchr(p4,'"');
              if(p5==NULL || p5-p4>=MAX_FNAME) goto emptyquote;
            }
            else {
              emptyquote:
              for(p5=p4; p5<p && !isspace(*p5) &&
                  strchr(";\"~#*?=,'",*p5)==NULL; p5++);
            }
            if(p5>p4) {
              *p5=0;
              for(p5--;
                  p5>=p4 && !isspace(*p5) && strchr("/\\:",*p5)==NULL;
                  p5--);
              if(p5>=p4) p4=p5+1;
              if(*p4==0) goto noname;
              if(strstr(p4,"..")!=NULL || *p4=='.')
                p4="noname.file";
              setvar("wims_deposit",p4);
            }
            else {
              noname: setvar("wims_deposit","noname.file");
            }
          }
          deplen=dlen;
        }
        *p3=0; l-=p2-p1; p1=p2;
    }
    else {
      p1=find_word_start(p1);
      l=strlen(p1)+1; p=strchr(p1,'=');
      if(p==NULL) p=p1+strlen(p1);
      if(*p==0 && l>1) {
        user_variable[v].name="no_name";
        user_variable[v].value=p1;
        coord_input=1;
        goto nnext;
      }
      *p++=0;
    }
/* empty name or empty value: ignore */
    if(*p1==0 || *p==0) continue;
/* We do not treat names containing '.' */
    for(pt=strchr(p1,'.'); pt; pt=strchr(++pt,'.')) *pt='_';
/* Restrictions on variable names */
    for(pt=p1; myisalnum(*pt) || *pt=='_'; pt++);
    if(*pt) continue;
    if(strcmp(p1,"wims_deposit")!=0) _tolinux(p);
/* This is a restriction:
 * Every parameter must have matching parentheses.
 */
    if(parenth==-1 && strncmp(p1,"freepar_",strlen("freepar_"))!=0
         && strcmp(p1,"wims_deposit")!=0
         && check_parentheses(p,1)) parenth=v;
    if(strcmp(p1,"special_parm")==0 && strcmp(p,"wims")==0)
        human_access=1;
    j=search_list(ro_name,RO_NAME_NO,sizeof(ro_name[0]),p1);
    if(j>=0) {
      if(j==ro_session) {
        p=find_word_start(p); *find_word_end(p)=0;
        if(strlen(p)>MAX_SESSIONLEN) continue;
        if(strcmp(p,robot_session)==0) p="";
        if(strcasecmp(p,"popup")==0) {
          mode=mode_popup;
          force_setvar("wims_mode","popup");
          force_setvar("session","");
          continue;
        }
      }
      if(j==ro_module) module_defined=1;
      if(j==ro_cmd) {
        p=find_word_start(p); *find_word_end(p)=0;
        if(strlen(p)>16) continue;
        cmd_defined=1;
      }
      if(j==ro_lang) {
        if(strlen(p)!=2) continue;
        for(i=0;i<available_lang_no && strcmp(available_lang[i],p)!=0;i++);
        if(i<available_lang_no) {user_lang=1; ovlstrcpy(lang,p);}
        else continue;
      }
/* strip leading and trailing '/'s in module name */
      if(j==ro_module) {
        p=find_word_start(p); *find_word_end(p)=0;
        while(*p=='/') p++;
        while(*p!=0 && *(p+strlen(p)-1)=='/') *(p+strlen(p)-1)=0;
        if(strlen(p)>MAX_MODULELEN) continue;
      }
      setvar(p1,p);
      if(j==ro_session && mode!=mode_popup) {
        char *pp, *pr;
        char buf[1024];
        mystrncpy(buf,p,sizeof(buf));
        if((pp=strchr(buf,'.'))!=NULL) {
          *pp++=0; session_serial=atoi(pp);
          if(session_serial<0) session_serial=0;
        }
        else session_serial=0;
        pp=strchr(buf,'_');
        if(pp!=NULL && (pr=strstr(pp,"_mhelp"))!=NULL) {
          *pr=0; ismhelp=1; lastout_file=-1;
          setvar("wims_inssub","mh");
        }
        force_setvar("wims_session",buf);
        if(pp!=NULL) force_setvar("wims_subsession",pp);
      }
      continue;
    }
    user_variable[v].name=p1;
    user_variable[v].value=p;
nnext:v++; if(v>=MAX_VAR_NUM) user_error("too_many_variables");
  }
  user_var_no=v;
  if(parenth>=0) {
    char buf[16];
    mystrncpy(buf,int2str(user_var_no),sizeof(buf));
    setvar("user_var_no",buf);
    for(i=0;i<user_var_no;i++) {
      snprintf(buf,sizeof(buf),"name%d",i);
      setvar(buf,user_variable[i].name);
      snprintf(buf,sizeof(buf),"value%d",i);
      setvar(buf,user_variable[i].value);
    }
    mystrncpy(buf,int2str(parenth),sizeof(buf));
    setvar("bad_parentheses",buf);
    user_error("unmatched_parentheses");
  }
  p=getenv("SCRIPT_NAME");
  if(p!=NULL && (p=strstr(p,"/getfile/"))!=NULL) {
    p+=strlen("/getfile/");
    force_setvar(ro_name[ro_cmd],commands[cmd_getfile]);
    force_setvar("special_parm",p);
    cmd_defined=1;
  }
  if(module_defined && !cmd_defined) setvar(ro_name[ro_cmd],commands[cmd_intro]);
  robot_check(); cookie2session();
}

/* parse special commands */
void special_cmds(void)
{
  char *c, *p;
  int i;
  long int l=-1;

  c=getvar(ro_name[ro_cmd]);
  if(c==NULL || *c==0) {  /* no module name nor command */
    setvar(ro_name[ro_module],home_module);
    setvar(ro_name[ro_cmd],commands[cmd_new]);
    return;
  }
  for(i=0;i<CMD_NO && strcmp(commands[i],c)!=0; i++);
  switch(i) {
    case cmd_intro: {
      set_module_prefix();
      default_form_method="get";
      if(ftest(mkfname(NULL,"%s/%s",module_prefix,intro_file))<0) {
        force_setvar(ro_name[ro_cmd],commands[cmd_new]);
        return;
      }
      p=getvar("wims_session");
      if(p!=NULL && *p!=0) {
        if(set_session_prefix()==0) check_session();
        else {
          trap_check(p);
          if(strchr(p,'_')!=NULL && strchr(p,'/')==NULL) {
            get_static_session_var();
          }
        }
      }
/* determine http protocol name. How to detect? */
      p=getenv("HTTPS");
      if(p!=NULL && strcmp(p,"on")==0) {
        protocol="https"; set_protocol();
      }
      force_setvar("wims_protocol",protocol);
      determine_font(lang);
      determine_dirn(lang);
      main_phtml_put(intro_file); debug_output();
      introend: write_logs();free(var_str);
      delete_pid(); exit(0);
    }
    case cmd_ref: {
      set_module_prefix();
      default_form_method="get";
      p=getvar("wims_session");
      if(p!=NULL && *p!=0) {
        if(set_session_prefix()==0) check_session();
        else trap_check(p);
      }
      determine_font(lang);
      determine_dirn(lang);
      main_phtml_put(ref_file); goto introend;
    }
    case cmd_getins: {
      c=getvar(ro_name[ro_special_parm]);
      if(c==NULL || *c==0) {
        user_error_nolog=1; user_error("no_insnum");
      }
      if(*c=='/' || strstr(c,"..")!=NULL) goto badins;
      set_session_prefix();
      if(strstr(session_prefix,"robot")!=NULL) exit(0);
      l=filelength("%s/%s",s2_prefix,c);
      if(l<0) {
        badins: user_error_nolog=1; user_error("bad_insnum");
      }
      {
        char *fmt;
        fmt=strchr(c,'.');
        if(fmt==NULL) {
            user_error_nolog=1; user_error("bad_insnum");
        }
        else fmt++;

        nph_header(200);
/* insert format problem; bricolage */
        printf("Content-type: image/%s\r\n\
Content-length: %ld\r\n\r\n",fmt,l);
        catfile(stdout,"%s/%s",s2_prefix,c); exit(0);
      }
    }
    case cmd_getfile: {
      char fname[MAX_FNAME+1];
      c=getvar(ro_name[ro_special_parm]);
      if(c==NULL || *c==0) {
        user_error_nolog=1; user_error("no_insnum");
      }
      if(*c=='/' || strstr(c,"..")!=NULL) goto badfile;

      set_session_prefix();
      if(strstr(session_prefix,"robot")!=NULL) exit(0);
      mkfname(fname,"%s/getfile/%s",session_prefix,c);
      l=filelength("%s",fname);
      if(l<0 && strchr(session_prefix,'_')!=NULL) {
        char *pt;
        mystrncpy(fname,session_prefix,sizeof(fname));
        pt=strrchr(fname,'_'); if(pt) *pt=0;
        snprintf(fname+(pt-fname),sizeof(fname)-(pt-fname),
              "/getfile/%s",c);
        l=filelength("%s",fname);
      }
      if(l<0) {
        badfile: user_error_nolog=1; user_error("bad_insnum");
      }
      if(l>512*1024) {
        struct rlimit rlim;
        rlimit_cpu*=l/(10*1024);
        rlim.rlim_cur=rlim.rlim_max=rlimit_cpu;
          initalarm();
      }
      {
        char *p1;
        char mime[MAX_LINELEN+1];
        for(p1=c+strlen(c);p1>c && isalpha(*(p1-1)); p1--);
        ovlstrcpy(mime,"application/octet-stream");
        if(p1>c && *(p1-1)=='.') {
          setvar("translator_unknown",mime);
          setvar("dictionary","bases/sys/mime");
          snprintf(mime,sizeof(mime),"translator %s",p1);
          calc_exec(mime);
        }
        nph_header(200);
        printf("Content-type: %s\r\n\
Content-length: %ld\r\n\r\n",mime,l);
        catfile(stdout,"%s",fname); exit(0);
      }
    }
    case cmd_close: {
      char *p, b2[32]; int w;
      char nbuf[MAX_FNAME+1], vbuf[MAX_LINELEN+1];
      p=getvar(ro_name[ro_session]);
      if(p==NULL || strlen(p)<10 ||
         strchr(p,'/')!=NULL) return;
      mystrncpy(b2,p,sizeof(b2));
      p=strchr(b2,'.'); if(p!=NULL) *p=0;
      mkfname(nbuf,"%s/%s/var.stat",session_dir,b2);
      getdef(nbuf,"wims_caller",vbuf);
      if(vbuf[0]!=0) force_setvar(ro_name[ro_session],vbuf);
      w=wrapexec; wrapexec=1;
      call_sh("rm -Rf %s/%s* %s/%s* >/dev/null 2>&1",session_dir,b2,s2_dir,b2);
      wrapexec=w; cookiegot[0]=0;
      force_setvar(ro_name[ro_cmd],"new");
    }
    default: return;
  }
}

/* This is run only when manually invoking the program.
 * Verifies the orderedness of various list tables.
 */
int verify_tables(void)
{
    if(verify_order(calc_routine,CALC_FN_NO,sizeof(calc_routine[0]))) return -1;
    if(verify_order(exec_routine,EXEC_FN_NO,sizeof(exec_routine[0]))) return -1;
    if(verify_order(main_config,MAIN_CONFIG_NO,sizeof(main_config[0]))) return -1;
    if(verify_order(mathname,mathname_no,sizeof(mathname[0]))) return -1;
    if(verify_order(hmname,hmname_no,sizeof(hmname[0]))) return -1;
    if(verify_order(ro_name,RO_NAME_NO,sizeof(ro_name[0]))) return -1;
    if(verify_order(distr_cmd,distr_cmd_no,sizeof(distr_cmd[0]))) return -1;
    if(verify_order(internal_name,INTERNAL_NAME_NO,
                sizeof(internal_name[0]))) return -1;
    if(verify_order(tmathfn,tmathfn_no,sizeof(tmathfn[0]))) return -1;
    if(verify_order(tmathvar,tmathvar_no,sizeof(tmathvar[0]))) return -1;
    if(verify_order(modindex,MODINDEX_NO,sizeof(modindex[0]))) return -1;
    if(verify_order(exportvars,exportvarcnt,sizeof(exportvars[0]))) return -1;
    if(evaltab_verify()) return -1;
    if(textab_verify()) return -1;
    return 0;
}

void config_defaults(void)
{
    int i;
    for(i=0;i<MAIN_CONFIG_NO;i++) {
      if((1&main_config[i].is_integer)==1) {
          int *pi = (int*)main_config[i].address;
          printf("DF_%s=%d\n",main_config[i].name, *pi);
        }
      else {
            char **ps = (char**)main_config[i].address;
          printf("DF_%s=%s\n",main_config[i].name,*ps);
        }
    }
}

/* get and set useroptions */
void useropts(void)
{
    char *p;
    setvar("lang",lang);
    p=getvar("useropts");
    if(p==NULL || *p==0) p=getvar("wims_useropts");
    if(p!=NULL && *p!=0) {
      if(myisdigit(p[0])) {
        usertexsize=p[0]-'0';
        /* fourth digit is for special fonts*/
        if(p[1]!=0) { mathalign_base=p[1]-'0'; }
      }
      if(myisdigit(p[3]) && p[3]!=0){ spec_font=p[3]-'0';}
    }
    if(mathalign_base==1) {
        mathalign_sup1="<sup>"; mathalign_sup2="</sup>";
    } else mathalign_sup1=mathalign_sup2="";
}

/* popup module help */
void mhelp(void)
{
    char *p, buf[MAX_LINELEN+1];
    main_phtml_put(""); buf[0]=0;
    if(cmd_type!=cmd_help) {
      phtml_put_base("closemhelp.phtml",0);
    }
    else {
      phtml_put_base("mhelpheader.phtml",0);
      p=getvar("special_parm");
      if(p!=NULL && strcmp(p,"about")==0)
        phtml_put("about.phtml",0);
      else phtml_put("help.phtml",0);
      phtml_put_base("mhelptail.phtml",0);
      exec_tail(buf);
    }
}

#define READSTDIN_WINDOW 4096

void readstdin(int len)
{
    int ll, l1, lt, lr;
    int cpulim;

    cpulim=rlimit_cpu; rlimit_cpu=3;
    lr=len; l1=0;
    while(lr>0) {
      nowtime=time(0); initalarm();
      ll=lr; if(ll>READSTDIN_WINDOW) ll=READSTDIN_WINDOW;
      lt=fread(stdinbuf+l1,1,ll,stdin);
      if(lt!=ll) user_error("parm_too_long");
      lr-=ll; l1+=ll;
    }
    if(l1!=len) user_error("parm_too_long");
    stdinbuf[len]=0; rlimit_cpu=cpulim;
}

/* input: p=QUERY_STRING. output: parameter length. */
/* Netscape puts form content into /tmp. */
int formdata(char *p)
{
    char *pp;
    int inlen;
    char *ctype;
    inlen=0; ctype=getenv("CONTENT_TYPE");
    if(ctype==NULL || strstr(ctype,"multipart/form-data")==NULL
       || (p=strstr(ctype,"boundary="))==NULL) {
      bad: stdinbuf=""; return 0;
    }
    pp=getenv("CONTENT_LENGTH");
    if(pp==NULL) goto bad;
    inlen=atoi(pp); if(inlen<=10) goto bad;
    if(inlen>=MAX_DEPOSITLEN) user_error("parm_too_long");
    stdinbuf=xmalloc(inlen+1); readstdin(inlen);
    p+=strlen("boundary=");
    for(pp=p;myisalnum(*pp) || *pp=='-'; pp++);
    if(pp-p<sizeof(mpboundary)-2) {
      memmove(mpboundary,p,pp-p); mpboundary[pp-p]=0;
    }
/* empty data */
    if(strstr(stdinbuf,mpboundary)==NULL || strstr(stdinbuf,"name=")==NULL) {
      free(stdinbuf); goto bad;
    }
    form_access=1; post_log();
    return inlen;
}

/* get the content of POST */
void getpost(void)
{
    int ll;
    char *pp;
    pp=getenv("CONTENT_LENGTH");
    if(pp==NULL || (ll=atoi(pp))<=0) {
      stdinbuf=xmalloc(16); stdinbuf[0]=0;
    }
    else {
      if(ll>QUERY_STRING_LIMIT) user_error("parm_too_long");
      stdinbuf=xmalloc(ll+16); readstdin(ll);
      if(ll>0) {
          setenv("QUERY_STRING",stdinbuf,1);
          form_access=1; post_log();
      }
    }
}

void buffer_init(void)
{
    struct timeval tv;

    mcachecnt=readnest=0;
    mpboundary[0]=cookiegot[0]=cookieset[0]=cwdbuf[0]=0;
    rscore_class[0]=rscore_user[0]=multiexec_random[0]=0;
    lastftest[0]=0;
    lastdatafile[0]=0; lastdata=xmalloc(WORKFILE_LIMIT);
    outptr=outbuf;
    instex_src[0]=instex_fname[0]=module_prefix[0]=0;
    examlogf[0]=examlogd[0]=exam_sheetexo[0]=0;
    freeworklogf[0]=freeworklogd[0]=0;
    stdinbuf=NULL;
    mkfname(tmp_dir,"../tmp/forall");
    mkfname(session_dir,"../%s",SESSION_BASE);
    mkfname(s2_dir,"../%s",S2_BASE);
    if(gettimeofday(&tv,NULL)) startmtime=startmtime2=0;
    else {
      startmtime=((tv.tv_sec%10000)*1000+tv.tv_usec/1000);
      startmtime2=(tv.tv_sec%1000)*1000000+tv.tv_usec;
    }
}

/**
 * runs the test suite, to check whether the documented commands of modtool
 * behave properly.
 * @param prefix path to the proc file to process
 * @param p      name of the proc file to proceed
 * @param vars a list of var names to print.
 **/
void test_suite(char *prefix, char *p, char *vars){
  char *v, *nextv;
  mystrncpy(module_prefix, prefix, sizeof(module_prefix));
  exec_read(p);
  nextv=vars;
  while (*nextv!='\0'){
    v=find_word_start(nextv);
    nextv=find_word_end(v);
    if (*nextv != '\0'){
      *nextv++='\0';
    }
    v=getvar(v);
    if(v) puts(v); else putchar('\n');
  }
}

int main(int argc, char *argv[], char *envp[])
{
    char *p, homebuf[MAX_FNAME+1], lbuf[32], buf[64];
    int inlen=0;
/*    int mfd; */

    class_dir[0]=0;
    substitute=substit; buffer_init(); var_init();
/* WIMS internal locale is always C. */
    setenv("LANG","C",1); umask(022);
    setenv("LANGUAGE","us",1);
    setenv("LC_ALL","C",1);
    if(argc>1) {
      if(strcasecmp(argv[1],"table")==0) {
          if(verify_tables()) internal_error("Table disorder.");
          else printf("Table orders OK.\n");
          return 0;
      }
      if(strcasecmp(argv[1],"version")==0) {
          printf("%s",wims_version); return 0;
      }
      if(strcasecmp(argv[1],"defaults")==0) {
          config_defaults(); return 0;
      }
      if(strcasecmp(argv[1],"test")==0) {
/* launches a test suite */
        if (argc < 5) {
          printf("Not enough arguments to launch a test; usage:\n");
          printf("wims test path_to_file name_of_proc_file 'var1 var2 ...'\n\n");
          printf("the file at path_to_file/name_of_proc_file will be evaluated by wims for\nexec commands, then the values of var1, var2, ... will be printed\nin the standard output, one per line.\n");
          return 1;
        }
        test_suite(argv[2], argv[3], argv[4]);
        return 0;
      }
    }
    p=getenv("SERVER_SOFTWARE"); if(p!=NULL && strcasecmp(p,"WIMS")==0)
      httpd_type=httpd_wims;
    p=getenv("REMOTE_ADDR"); if(p!=NULL) remote_addr=p;
    p=getenv("REMOTE_HOST"); if(p!=NULL) remote_host=p;
    nowtime=time(0); now=localtime(&nowtime);
/* nowtime contains the seconds elapsed from EPOCH (1970)
   but tm_year is set to current year-1900.
 */
    memmove(&Now, now, sizeof(Now)); now=&Now;
    snprintf(nowstr,sizeof(nowstr),"%04d%02d%02d.%02d:%02d:%02d",
           (now->tm_year)+1900,(now->tm_mon)+1,now->tm_mday,
           now->tm_hour,now->tm_min,now->tm_sec);
    p=getenv("QUERY_STRING");
    if(p==NULL || *p==0) getpost();
    else if(strncmp(p,"form-data",9)==0) inlen=formdata(p);

    force_setvar("wims_now",nowstr);
    snprintf(lbuf,sizeof(lbuf),"%lu",nowtime);
    force_setvar("wims_nowseconds",lbuf);
    nowtime=time(0);
    initalarm();

    executed_gotos=insert_no=output_length=0; ins_alt[0]=0;
    setvar("empty","");       /* lock this variable */
    setvar("wims_version",wims_version);
    setvar("wims_version_date",wims_version_date);
    setvar("wims_main_font","utf-8");
    take_httpd_vars();

    main_configure();
    checklogd();
/* mfd=shm_open(SHM_NAME,O_RDONLY,-1);
 * if(mfd==-1) internal_error("Unable to find shared memory.");
 * shmptr=mmap(0,SHM_SIZE,PROT_READ,MAP_SHARED,mfd,0);
 * if(shmptr==MAP_FAILED) internal_error("Shared memory failure.");
 */
    getppid(); /* this is the first sysmask trigger, must be after checklogd() */
    predetermine_language();
    /* modify a few rlimits for 64-bit processors */
    if (sizeof(long) == 8) {
      rlimit_as*=2;    /* virtual memory size */
      rlimit_data*=2;  /* data segment size; maxima requires a lot */
      rlimit_stack*=2; /* stack size */
    }
    set_rlimits();
    /*init_random();*/
    seed_time();
    p=getvar("wims_seed");
    mystrncpy(buf,p,sizeof(buf));
    exec_setseed(buf);
    module_configure();
    set_job_ident();
    m_file.name[0]=0;m_file.linecnt=m_file.linepointer=0;
    p=getenv("QUERY_STRING");
    if(p==NULL || strlen(p)==0) {
      setvar("lang",lang);
      snprintf(homebuf,sizeof(homebuf),"module=%s",home_module);
      p=homebuf;
    }
    if(strlen(p)>=QUERY_STRING_LIMIT) user_error("parm_too_long");
    if(mpboundary[0]==0) {
      var_str=xmalloc(strlen(p)+2);
      parse_query_string(http2env(var_str,p),0);
    }
    else {
      var_str=stdinbuf;
      parse_query_string(inlen,1);
    }
    if(ismhelp) {
      p=getvar(ro_name[ro_cmd]);
      if(p==NULL || (strcmp(p,"help")!=0 && strcmp(p,"getins")!=0)) {
          mhelp(); goto outgo;
      }
    }
    check_exam();
    useropts();
    special_cmds();
    parse_ro_names();
    manager_check();
    access_check(0);
/* it is really for developing in wims, so no use in general */
  if (1==1) {
    if(strstr(tmp_debug,"yes")!=NULL && checkhost(manager_site)>=1)
      trace_file = fopen(mkfname(NULL,"%s/%s",tmp_dir,"trace.txt"),"a");
  }
  set_variables();
  determine_font(getvar("module_language"));
  determine_dirn(getvar("module_language"));
  if(!robot_access && session_prefix[0]!=0 && cmd_type!=cmd_help && !ismhelp)
    lastout_file=creat(mkfname(NULL,"%s/%s",s2_prefix,lastout),
                 S_IRUSR|S_IWUSR);
  p=getvar("module_score");
  if(p!=NULL) force_setvar("wims_seed_score",p);
  p=getvar("module_category");
  if(p==NULL || strstr(p,"tool")==NULL) default_form_method="get";
  if(noout) {
    write_logs(); save_session_vars();
    goto outgo;
  }
  if(ismhelp) {
    mhelp();
  }
  else {
    main_phtml_put(html_file);
    if(lastout_file!=-1) {
      flushoutput(); close(lastout_file); putlastout();
    }
    write_logs(); save_session_vars();
  }
  outgo:
  debug_output();
  if (trace_file)  { fclose(trace_file); trace_file=NULL; }
  if(var_str!=stdinbuf) free(var_str);
  delete_pid();
  if(mode!=mode_popup && trusted_module()) {
    p=getvar("wims_mode");
    if(p!=NULL && strcmp(p,"popup")==0) mode=mode_popup;
  }
  if(mode==mode_popup && insert_no==0) {
    p=getvar("wims_mode");
    if(p!=NULL && strcmp(p,"popup")==0) {
      remove_tree(session_prefix);

      remove_tree(s2_prefix);
    }
  }
  return 0;
}
