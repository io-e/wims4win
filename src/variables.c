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
/* routines to process variables */

#include "wims.h"
char *computed_var_start; /* pointer to read-in var def file */
int session_var_ready=0;
char last_host[32]="";
char *robotcheck="";

char *header_var_name[]={
    "REMOTE_ADDR", "HTTP_REFERER", "QUERY_STRING", "HTTP_USER_AGENT",
     "HTTP_COOKIE"
};
#define HEADER_VAR_NO (sizeof(header_var_name)/sizeof(header_var_name[0]))

struct special_name {char *name; int value;} special_name[]={
  {"MAX_EXAMS",MAX_EXAMS},
  {"MAX_EXOS",MAX_EXOS},
  {"MAX_FREEWORKS",MAX_FREEWORKS},
  {"MAX_LTECHVAR",MAX_LTECHVAR},
  {"MAX_OEFCHOICES",MAX_OEFCHOICES},
  {"MAX_OEFREPLIES",MAX_OEFREPLIES},
  {"MAX_SHEETS",MAX_SHEETS},
  {"MAX_TECHVARVAL",MAX_TECHVARVAL},
  {"MAX_USERFORGRADES",MAX_USERFORGRADES},
  {"MAX_VOTES",MAX_VOTES},
};
int special_name_no=(sizeof(special_name)/sizeof(special_name[0]));
char *var_allow[]={
    "deny" , "init" , "config" ,
      "reply", "any", "help"
};
enum {
    var_allow_deny, var_allow_init, var_allow_config,
      var_allow_reply, var_allow_any, var_allow_help
} VAR_ALLOWS;
#define VAR_ALLOW_NO (sizeof(var_allow)/sizeof(var_allow[0]))

struct internal_name internaldef_name[]={
  {"exotrymax",var_allow_init}
};
int INTERNALDEF_NAME_NO=(sizeof(internaldef_name)/sizeof(internaldef_name[0]));

/* install a temporary directory for the session */
void mktmpdir(char *p)
{
  char *base;
  if(p==NULL || *p==0 || strstr(p,"robot")!=NULL) return;
  if(strstr(tmp_dir,"sessions")!=NULL) return;
  if(ftest("../chroot/tmp/sessions/.chroot")==is_file) base="chroot/tmp";
  else base="tmp";
  mkfname(tmp_dir,"../%s/sessions/%s",base,p);
  remove_tree(tmp_dir); mkdirs(tmp_dir);
  chmod(tmp_dir,S_IRUSR|S_IWUSR|S_IXUSR
       |S_IRGRP|S_IWGRP|S_IXGRP|S_IROTH|S_IWOTH|S_IXOTH);
  setenv("tmp_dir",tmp_dir,1); setenv("TMPDIR",tmp_dir,1);
}

/* Open session variable file */
FILE *fopen_session_var_file(char read_or_write[])
{
  char *nbuf; FILE *f;

  nbuf=mkfname(NULL,"%s/var",session_prefix);
  if(read_or_write[0]=='r') {
    if(open_working_file(&m_file,nbuf)!=0) return NULL;
    mkfname(m_file.name,"session_var");
    return stdin;
  }
  f=fopen(nbuf,read_or_write);
  if(f==NULL) {
/* expired_session */
    internal_error("fopen_session_var_file(): unable to create session variable file.");
  }
  mkfname(m_file.name,"session_var");
  m_file.l=0;
  return f;
}

/* Open a module file, read only, with name checking.
 * returns NULL if error.
 */
int read_module_file(char *fname)
{
  char nbuf[MAX_FNAME+1];

  if(get_cached_file(fname)>=0) return 0;
  if(find_module_file(fname,nbuf,0)) return -1;
  if(open_working_file(&m_file,nbuf)!=0) return -1;
  untrust|=(untrust>>8);
  mystrncpy(m_file.name,fname,sizeof(m_file.name));
  if(strncmp(module_prefix,module_dir,strlen(module_dir))!=0) m_file.nocache|=8;
  return 0;
}
/* list of variables get from the query parms */
int varget[]={ro_module,ro_lang,ro_useropts,ro_worksheet};
#define varget_no (sizeof(varget)/sizeof(varget[0]))

/* set up ref strings according to protocol */
void set_protocol(void)
{
  if(strcmp(protocol,"https")!=0) return;
  if(strncmp(ref_name,"http:",5)==0) {
    string_modify(ref_name,ref_name+4,ref_name+4,"s");
    string_modify(ref_base,ref_base+4,ref_base+4,"s");
    force_setvar("wims_ref_name",ref_name);
  }
}

/* verify class participant connection data */
void classlock(void)
{
  int lvl;
  char *p;

  p=getvar("wims_classlock"); if(p==NULL) lvl=0; else {
    p=find_word_start(p);
    lvl=*p-'0'; if(lvl<0 || lvl>7) lvl=0;
  }
  if(lvl==7) { /* closed */
    p=getvar("wims_user");
    if(p==NULL || strcmp(p,"supervisor")!=0) user_error("class_closed");
  }
  if(lvl==2 || lvl==4 || lvl==6) { /* https */
    p=getenv("HTTPS");
    if(p==NULL || strcasecmp(p,"on")!=0) user_error("need_https");
  }
  if(lvl==3 || lvl>=5) {
    if(strcmp(last_host,remote_addr)!=0) user_error("bad_host");
  }
  if((lvl==1 || lvl>=4) && cookiegot[0]==0) { /* cookie */
    setcookie=1;
    setvar("cookie_module",getvar(ro_name[ro_module]));
    setvar("cookie_cmd",getvar(ro_name[ro_cmd]));
    force_setvar(ro_name[ro_module],home_module);
    force_setvar(ro_name[ro_cmd],commands[cmd_new]);
    setvar("wims_askcookie","yes");
    return;
  }
  else setcookie=0;
}

/* get static session variables */
void get_static_session_var(void)
{
  char *p, *pe, *p2, *p3;
  char sbuf[MAX_FNAME+1], tbuf[MAX_LINELEN+1];
  mystrncpy(sbuf,session_prefix,sizeof(sbuf));
  for(p=sbuf+strlen(sbuf);p>sbuf && *p!='_' && *p!='/'; p--);
  if(p>sbuf && *p=='_') *p=0;
  accessfile(tbuf,"r","%s/var.stat",sbuf);
  p=strrchr(sbuf,'/'); if(p!=NULL) p++; else p=sbuf;
  mktmpdir(p);
  for(p=find_word_start(tbuf);*p;p=find_word_start(pe)) {
    pe=strchr(p,'\n'); if(pe!=NULL) *pe++=0; else pe=p+strlen(p);
    p2=strchr(p,'='); if(p2==NULL) continue;
    *p2++=0; force_setvar(p,p2);
  }
  p=getvar("wims_class"); if(p==NULL || *p==0) return;
  mkfname(class_dir,"%s/%s",class_base,p);
  classlock();
  p3=getvar("wims_class_refcolor");
  if(p3!=NULL && *p3!=0) force_setvar("wims_ref_bgcolor",p3);
  p3=getvar("wims_class_ref_menucolor");
  if(p3!=NULL && *p3!=0) force_setvar("wims_ref_menucolor",p3);
  p3=getvar("wims_class_ref_button_color");
  if(p3!=NULL && *p3!=0) force_setvar("wims_ref_button_color",p3);
  p3=getvar("wims_class_ref_button_bgcolor");
  if(p3!=NULL && *p3!=0) force_setvar("wims_ref_button_bgcolor",p3);
  p3=getvar("wims_class_ref_button_help_bgcolor");
  if(p3!=NULL && *p3!=0) force_setvar("wims_ref_button_help_bgcolor",p3);
  p3=getvar("wims_class_ref_button_help_color");
  if(p3!=NULL && *p3!=0) force_setvar("wims_ref_button_help_color",p3);
  p2=getvar(ro_name[ro_module]);
  if(p2==NULL || strncmp(p2,"classes/",strlen("classes/"))!=0) return;
  mkfname(sbuf,"classes/%s",lang);
  if(strcmp(sbuf,p2)!=0) force_setvar(ro_name[ro_module],sbuf);
}

/* set one static session variable */
void set_static_session_var(char *name, char *val)
{
  char *p;
  char sbuf[MAX_FNAME+1], tbuf[MAX_LINELEN+1];
  mystrncpy(sbuf,session_prefix,sizeof(sbuf));
  for(p=sbuf+strlen(sbuf);p>sbuf && *p!='_' && *p!='/'; p--)
  ;
  if(p>sbuf && *p=='_') *p=0;
  snprintf(sbuf+strlen(sbuf),sizeof(sbuf)-strlen(sbuf),"/var.stat");
  snprintf(tbuf,sizeof(tbuf),"%s=%s",name,val);
  setdef(sbuf,tbuf); setvar(name,val);
}

/* The session is probably robot. */
void robot_doubt(void)
{
  char *h, *p;

  p=getvar("special_parm"); h=getvar("module");
  if(p==NULL || h==NULL) {
    bad: user_error("robot_doubt"); return;
  }
  p=find_word_start(p); strip_trailing_spaces(p);
  if(strcmp(p,"wims")!=0 || strcmp(h,home_module)!=0) goto bad;
  set_static_session_var("wims_robotcheck","manual");
}

/* User has changed module within an operation.
 * Probably due to robot access.
 */
void bad_module(void)
{
  char *p;
  p=getvar("wims_user"); if(p==NULL) p="";
  if(*p==0 && strcmp(robotcheck,"manual")!=0) set_static_session_var("wims_robotcheck","robot");
  else setvar("wims_human_access","yes");
  user_error("module_change");
}

/* returns 1 if session directory exists */
int session_exists(char *s)
{
  if(ftest(mkfname(NULL,"../%s/%s/var",SESSION_BASE,s))==is_file) return 1;
  else return 0;
}

/* Check the validity of session number .
 * returns 0 if OK, else -1.
 */
int check_session(void)
{
  char tbuf[MAX_LINELEN+1], vbuf[MAX_LINELEN+1];
  char *p, *pp, *pr;
  int i,m,n,pl,rapid,badmod;
  struct stat st;

  rapid=badmod=0; pr="";
  if(fopen_session_var_file("r")==NULL) return -1;
  if(ftest(s2_prefix)!=is_dir) mkdirs(s2_prefix);
  session_var_ready=1; memmove(&svar_file,&m_file,sizeof(WORKING_FILE));
/* REMOTE_ADDR */
  wgetline(vbuf,MAX_LINELEN,&m_file);
  mystrncpy(last_host,vbuf+strlen("REMOTE_ADDR="),sizeof(last_host));
  m_file.linepointer++; /* now it points to query_string */
  pp=getenv("QUERY_STRING");
  if(pp!=NULL && *pp!=0 && strlen(pp)<=MAX_LINELEN) {
/* we compare the query string with the last one. */
    char *p1, *p2;
    wgetline(tbuf,MAX_LINELEN,&m_file);
    p1=tbuf+strlen("QUERY_STRING=");
    if(strncmp(tbuf,"QUERY_STRING=",strlen("QUERY_STRING="))==0 &&
        strcmp(pp,p1)==0 && strstr(session_prefix,"_test")==NULL) {
/* query string does not change */
      if(ftest(mkfname(NULL,"%s/%s",s2_prefix,lastout))==is_file &&
            ftest_size > 0) {
        uselast:
        putlastout(); delete_pid(); exit(0);
      }
      else {
        if(cmd_type==cmd_new || cmd_type==cmd_renew ||
             cmd_type==cmd_reply || cmd_type==cmd_next) {
          cmd_type=cmd_resume;
          force_setvar(ro_name[ro_cmd],"resume");
          forceresume=1;
        }
      }
    }
/* stop rapidfire requests */
    if((cmd_type==cmd_new || cmd_type==cmd_renew) &&
        strncmp(p1,"session=",strlen("session="))==0 &&
        strncmp(pp,"session=",strlen("session="))==0) {
      p1=strchr(p1,'&'); if(p1==NULL) p1="";
      p2=strchr(pp,'&'); if(p2==NULL) p2=""; pr=p2;
      if(strcmp(p1,p2)==0) rapid=1;
    }
  }
  m_file.linepointer=3;
  wgetline(vbuf,MAX_LINELEN,&m_file); /* stored user_agent */
/*    p=getenv("HTTP_USER_AGENT"); if(p==NULL) p="";
    if(strcmp(vbuf+strlen("HTTP_USER_AGENT="),p)!=0) bad_ident(); */
  m_file.linepointer=HEADER_VAR_NO;
  pl=strlen(var_prefix);i=-1;
  while(wgetline(tbuf,MAX_LINELEN,&m_file)!=EOF) {
    if(tbuf[0]==0) break; /* blank line */
    if(strncmp(tbuf,var_prefix,pl)!=0) break;
    i++;if(i>=RO_NAME_NO) break;
    for(n=0;n<varget_no && varget[n]!=i;n++)
    ;
      if(n>=varget_no) continue;
    m=pl+strlen(ro_name[i]);
    if(tbuf[m]!='=' || tbuf[m+1]==0) continue;
    if(i==ro_module && cmd_type!=cmd_new && cmd_type!=cmd_intro && cmd_type!=cmd_help) {
      char *pp;
      pp=getvar(ro_name[i]);
      if(pp!=NULL && *pp!=0 && strcmp(pp,tbuf+m+1)!=0) badmod=1;
    }
    if(i==ro_lang && !user_lang)
      force_setvar(ro_name[i],tbuf+m+1);
    else setvar(ro_name[i],tbuf+m+1);
  }
/* recover internal variables */
  do
  {
    char *v;
    if(tbuf[0]==0) break;
    v=strchr(tbuf,'=');
    if(v==NULL) break; else *(v++)=0;
    if(strncmp(tbuf,var_prefix,strlen(var_prefix))!=0) setenv(tbuf,v,1);
    else if(tbuf[strlen(var_prefix)]) force_setvar(tbuf+strlen(var_prefix),v);
  }
  while(wgetline(tbuf,MAX_LINELEN,&m_file)!=EOF);
  get_static_session_var();
  robotcheck=getvar("wims_robotcheck"); if(robotcheck==NULL) robotcheck="";
/* form access means manual access. Mark this. */
  if(form_access && strcmp(robotcheck,"manual")!=0) {
    robotcheck="manual";
    set_static_session_var("wims_robotcheck","manual");
  }
  else if(strcmp(robotcheck,"robot")==0) robot_doubt();
  if(badmod) bad_module();
  if(cookiegot[0]!=0) {
    p=getvar("wims_sescookie");
    if(p!=NULL && *p!=0 && strcmp(cookiegot,p)!=0) bad_ident();
  }
  p=getvar("wims_sreferer");
  if(p!=NULL && *p!=0) {
    setenv("HTTP_REFERER",p,1); setvar("httpd_HTTP_REFERER",p);
  }
  if(rapid) {
    int rapidfiredelay;
    char *pw, fnbuf[MAX_FNAME+1];
/* Delay: 10 seconds within worksheets, 1 second otherwise. */
    pw=getvar("wims_developer");
    if(pw!=NULL && *pw!=0) goto delcheckend;
    pw=getvar("wims_user");
    if(pw==NULL || *pw==0 || strcmp(pw,"supervisor")==0) rapidfiredelay=1;
    else {
      pw=strstr(pr,"&+worksheet=");
      if(pw!=NULL && myisdigit(*(pw+strlen("&+worksheet=")))) rapidfiredelay=10;
        else rapidfiredelay=2;
    }
    if(ftest(mkfname(fnbuf,"%s/%s",s2_prefix,lastout))==is_file
        && ftest_size > 0
        && stat(fnbuf,&st) == 0
        && st.st_mtime > nowtime-rapidfiredelay && st.st_mtime <= nowtime)
      goto uselast;
  }
/* set protocol string */
  delcheckend: pp=getvar("wims_protocol");
  if(pp!=NULL && strcmp(pp,"https")==0) {
    protocol="https"; set_protocol();
  }
  useropts(); return 0;
}
/* check whether a session is trapped. */
void trap_check(char *s)
{
  char buf[64];
  char *p;
  time_t t1;

  setvar("wims_session_expired",s);
  accessfile(tmplbuf,"r","../tmp/log/trap.check");
  if(tmplbuf[0]==0) return;
  p=getenv("REMOTE_ADDR");if(p==NULL) return;
  snprintf(buf,sizeof(buf),":%s,%s,",s,p);
  p=strstr(tmplbuf,buf); if(p==NULL) return;
  p+=strlen(buf);*find_word_end(p)=0;
  t1=atoi(p);
  if(t1>nowtime) user_error("trapped");
}

char cars[]="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

void set_cookie(void)
{
  #define keylen 20
  char sesbuf[16], keybuf[keylen+8];
  char *p;

  p=getvar(ro_name[ro_session]); if(p==NULL) return;
  mystrncpy(sesbuf,p,sizeof(sesbuf));
  if(strchr(sesbuf,'_')==NULL) { /* main session */
    int i;
    for(i=0;i<keylen;i++) keybuf[i]=cars[random()%36];
    keybuf[keylen]=0; cookiegot[0]=0;
    snprintf(cookieset,sizeof(cookieset),"%s-%s",sesbuf,keybuf);
    set_static_session_var("wims_sescookie",cookieset);
    setcookie=1;
  }
  else { /* subsession */
    p=getvar("wims_sescookie"); if(p && *p)
    mystrncpy(cookieset,p,sizeof(cookieset));
  }
}

/* create a session number */
void create_session(void)
{
  long t; char session_str[64],*s;
  char *p, ses_dir_buf[MAX_FNAME+1], sesrandbuf[MAX_LINELEN+1];
  int i;

/* no session is created for robots */
  if(robot_access) return;
  sesrandbuf[0]=0;
/* If session is given in request_string: use it. */
  s=getvar(ro_name[ro_session]); if(s==NULL) goto creat;
  mystrncpy(session_str,s,sizeof(session_str));
  s=strchr(session_str,'.'); if(s!=NULL) *s=0;
  s=session_str;
  if(*s!=0) {
    int i;
    mkfname(ses_dir_buf,"%s/%s",session_dir,s);
    i=ftest(ses_dir_buf);
    if(i<0) {
      trap_check(s);
/* subsession */
      if(strlen(s)>10 && strchr(s,'_')!=NULL) {
        char *tt;
        tt=strrchr(ses_dir_buf,'_'); if(tt!=NULL) *tt=0;
/* parent session gone. */
        if(ftest(ses_dir_buf)<0) goto creat;
        goto creat2;
      }
      else goto creat;
    }
    if(i!=is_dir) {
      trap_check(s);
      remove_tree(ses_dir_buf); goto creat;
    }
    return;
  }
  creat:
  t=create_job_ident();
  for(i=0;i<MAX_SESRANDOM;i++)
    snprintf(sesrandbuf+strlen(sesrandbuf),
            sizeof(sesrandbuf)-strlen(sesrandbuf),
            "%d,",sesrandomtab[i]);
  sesrandbuf[strlen(sesrandbuf)-1]='\n';
  snprintf(session_str,sizeof(session_str),"%c%c%08lX",
          cars[random()%36],cars[random()%36],t);
  creat2:
  force_setvar(ro_name[ro_session],session_str);
  setsesdir(session_str);
/* check whether the environment is created. */
  s=getvar(ro_name[ro_session]);
  if(s==NULL || strcmp(s,session_str))
    internal_error("cannot_create_session_number");
  snprintf(ses_dir_buf,sizeof(ses_dir_buf)-100,"%s/%s",
          session_dir,session_str);
  if(mkdir(ses_dir_buf,S_IRWXU)==-1)
    internal_error("cannot_create_session_directory");
  mkfname(s2_prefix,"%s/%s",s2_dir,session_str);
  if(mkdir(s2_prefix,S_IRWXU)==-1) mkdirs(s2_prefix);
  mystrncpy(session_prefix,ses_dir_buf,sizeof(session_prefix)); create_pid();
  if(strchr(session_str,'_')==NULL) {
    if((s=getenv("HTTP_REFERER"))!=NULL && *s!=0 && strlen(s)<MAX_FNAME-20
        && strstr(s,"wims")==NULL) {
      char *tt;
      tt=getenv("SERVER_NAME");
      if(tt==NULL || *tt==0 || strstr(s,tt)==NULL)
        set_static_session_var("wims_sreferer",s);
    }
    if(sesrandbuf[0]) set_static_session_var("wims_sesrandom",sesrandbuf);
  }
/* determine http protocol name. How to detect? */
  p=getenv("HTTPS"); if(p!=NULL && strcmp(p,"on")==0) {
      protocol="https"; set_protocol();
  }
  force_setvar("wims_protocol",protocol);
  new_session=1; session_serial=0;
  setvar("wims_new_session","yes");
  if(strchr(session_str,'_')!=NULL) get_static_session_var();
  set_cookie();
}

/* Register time of the request. */
void set_req_time(void)
{
  char tstr[64];

  snprintf(tstr,sizeof(tstr),"%04d-%02d-%02d.%02d:%02d:%02d=%lu",
         (now->tm_year)+1900, (now->tm_mon)+1, now->tm_mday,
         now->tm_hour, now->tm_min, now->tm_sec, nowtime);
  force_setvar("wims_req_time",tstr);
  if(cmd_type == cmd_new || cmd_type == cmd_renew)
    force_setvar("wims_module_start_time",tstr);
  if(new_session) force_setvar("wims_session_start_time",tstr);
}
/* choose a seed and put in in wims_seed*/
void seed_time(void)
{
  int r; char buf[64];
  struct timeval t;
  gettimeofday(&t,NULL);
  r=t.tv_usec+t.tv_sec*1000;
  if(r<=0) r=1-r;
  snprintf(buf,sizeof(buf),"%d",r);
  force_setvar("wims_seed",buf);
}

/* set up module_prefix. */
void set_module_prefix(void)
{
  char tbuf[MAX_FNAME+1], mmbuf[MAX_FNAME+1], *p, *pp, *ps;
  int t,ft;
  struct stat st;

  isclassmodule=0;
  p=getvar(ro_name[ro_module]);
  if(p==NULL || *p==0) user_error("no_module_name");
/* security measure: we should not allow users to go back to
 * parent directories.
 */
  if(strstr(p,parent_dir_string)!=NULL) user_error("wrong_module");
  if(strncmp(p,"classes/",strlen("classes/"))==0) isclassmodule=1;
  if(strncmp(p,"devel/",strlen("devel/"))==0) isdevelmodule=1;
  mkfname(module_prefix,"%s/%s",module_dir,p);
/* Now no symbolic link should appear in the module path. */
  mkfname(tbuf,"modules/%s",p);
  for(t=0,ps=pp=strchr(tbuf+strlen("modules/"),'/'); pp;
  *pp='/', ps=pp, pp=strchr(pp+1,'/'), t++) {
  *pp=0; if(lstat(tbuf,&st)) user_error("wrong_module");
  if(t>0 && S_ISLNK(st.st_mode)) {
    if(strcmp(ps,"/local")!=0 ||
          strncmp(tbuf,"modules/home",strlen("modules/home"))==0)
      user_error("wrong_module");
    }
  }
/* Check validity of the module. */
  mkfname(tbuf,"%s/%s",module_prefix,html_file);
  ft=stat(tbuf,&st);
  if(ft!=0 && p[strlen(p)-3]!='.') {
    int i,j;
    char *l;
    l=getvar(ro_name[ro_lang]);
    j=available_lang_no;
    for(i=-1;i<j && ft!=0;i++) {
      if(i<0) mkfname(mmbuf,"%s.%s",p,l);
      else mkfname(mmbuf,"%s.%s",p,available_lang[i]);
      mkfname(module_prefix,"%s/%s",module_dir,mmbuf);
      mkfname(tbuf,"%s/%s",module_prefix,html_file);
      ft=stat(tbuf,&st);
    }
    if(ft==0) force_setvar(ro_name[ro_module],mmbuf);
  }
  if(ft!=0 && !isclassmodule) user_error("wrong_module");
  setenv("module_dir",module_prefix,1); setvar("module_dir",module_prefix);
  module_index();
}

/* set up session_prefix. */
int set_session_prefix(void)
{
  char *p, s[32];

  if(robot_access) {
    mystrncpy(session_prefix,robot_session,sizeof(session_prefix));
    mystrncpy(s2_prefix,robot_session,sizeof(session_prefix));
    return 0;
  }
  p=getvar(ro_name[ro_session]);
  if(p==NULL || *p==0) user_error("no_session");
/* same reason as for modules */
  if (strchr(p,'/')!=NULL || strstr(p,parent_dir_string)!=NULL
     || *find_word_end(p)!=0) user_error("wrong_session");
  mystrncpy(s,p,sizeof(s));
  p=strchr(s,'.'); if(p!=NULL) *p=0;
  mkfname(session_prefix,"%s/%s",session_dir,s);
  p=strstr(session_prefix,"_mhelp"); if(p!=NULL) *p=0;
  if(ftest(session_prefix)!=is_dir) return -1;
  mkfname(s2_prefix,"%s/%s",s2_dir,s);
  setenv("session_dir",session_prefix,1);
  setenv("s2_dir",s2_prefix,1);
  if(ftest(mkfname(NULL,"%s/.trap",s2_prefix))==is_file)
      user_error("trapped");
  return 0;
}

/* check reserved name values in query_string */
void parse_ro_names(void)
{
  int i;
  char *cmd, *p;
  char sesbuf[64];
  create:
  cmd=getvar(ro_name[ro_cmd]);
  if(cmd==NULL || *cmd==0) user_error("no_command");
  for(i=0;i<CMD_NO;i++) if(strcmp(cmd,commands[i])==0) break;
  if(i>=CMD_NO) user_error("bad_command");
  cmd_type=i;
  if(cmd_type == cmd_new) {
    create_session();
    if(set_session_prefix()==0) {
      check_session();
      set_module_prefix();
    }
    else goto redo;
  }
  if (set_session_prefix()==-1 || (cmd_type != cmd_new && check_session())) {
    redo:
    force_setvar(ro_name[ro_cmd],commands[cmd_new]);
    if(strcmp(ro_name[ro_module],home_module)!=0) user_var_no=0;
     goto create;
  }
  if(!new_session) create_pid();
  session_serial++;
  if(robot_access) session_serial=1;
  snprintf(sesbuf,sizeof(sesbuf),"%d",session_serial);
  force_setvar("wims_session_serial",sesbuf);
  p=getvar(ro_name[ro_session]);
  if(p==NULL || *p==0) internal_error("parse_ro_names(): bad session.\n");
  mystrncpy(sesbuf,p,sizeof(sesbuf));
  p=strchr(sesbuf+5,'.');
  if(p!=NULL) *p=0;
  mktmpdir(sesbuf);
  if(!robot_access) {
    setsesdir(sesbuf);
    p=strchr(sesbuf,'_');
    if(p!=NULL) force_setvar("wims_subsession",p);
  }
  snprintf(sesbuf+strlen(sesbuf),sizeof(sesbuf)-strlen(sesbuf),
          ".%d",session_serial);
  force_setvar(ro_name[ro_session],sesbuf);
  if(cmd_type != cmd_new) set_module_prefix();
  set_req_time();
  if(robot_access) check_load(0);
  else {
    if(new_session) auth();
    else {
      p=getvar("wims_user"); if(p==NULL || *p==0) check_load(2);
    }
  }
  if(cmd_type==cmd_help && open_working_file(&m_file,module_about_file)==0)
    var_proc(NULL,0);
}


/* returns positive or 0 if var_def found, otherwise returns -1. */
int var_def_check(char *name)
{
  char *p, nbuf[MAX_NAMELEN+1];
  int i,tt;

  tt=-1;
  for(p=name+strlen(name);p>name && myisdigit(*(p-1));p--)
  ;
  if(*p && *p!='0' && p>name) tt=atoi(p);
  else p=name+strlen(name);
  if(p>name+MAX_NAMELEN) p=name+MAX_NAMELEN;
  memmove(nbuf,name,p-name); nbuf[p-name]=0;
  i=search_list(var_def,defined_var_total,sizeof(var_def[0]),nbuf);
  if(i<0) return -1;
  while(i>0 && tt<var_def[i].beg && strcmp(nbuf,var_def[i-1].name)==0) i--;
  while(i<defined_var_total-1 && tt>var_def[i].end &&
           strcmp(nbuf,var_def[i+1].name)==0) i++;
  if(tt<var_def[i].beg || tt>var_def[i].end) return -1;
  return i;
}

int var_def_name(char *n, int v)
{
  char *q, *r;
  int j;
  if(strlen(n)>=MAX_NAMELEN) module_error("defn_too_long");
  var_def[v].name=n;
  if((strncmp(n,wims_prefix,wpflen)==0 &&
        (strncmp(n,"wims_priv_",strlen("wims_priv_"))==0 ||
        search_list(internal_name,INTERNAL_NAME_NO,
               sizeof(internal_name[0]),n+wpflen)>=0)) ||
          search_list(ro_name,RO_NAME_NO,sizeof(ro_name[0]),n)>=0) {
    setvar("wims_reserved_name",n);
    module_error("name_is_reserved");
  }
  for(q=n;myisalnum(*q) || *q=='_'; q++);
  if(q==n) {
    illegal: setvar("wims_bad_name",n);
    module_error("illegal_name");
  }

  if(*q=='[') {
    *q++=0; r=find_matching(q,']');
    if(r==NULL) goto illegal;
    *r=0; int i;
    for(i=0;i<special_name_no && strcmp(q, special_name[i].name)!=0; i++);
    if(i<special_name_no) j=special_name[i].value; else j=atoi(q);
    if(j<1) j=1;
    if(j>MAX_VAR_NUM) j=MAX_VAR_NUM;
    var_def[v].beg=1; var_def[v].end=j;
    return j;
  }
  if(*q) goto illegal;
  for(r=q; r>n && myisdigit(*(r-1)); r--);
  if(*r && *r!='0' && r>n) {
    var_def[v].beg=var_def[v].end=atoi(r); *r=0;
  }
  else var_def[v].beg=var_def[v].end=-1;
  return 1;
}

int var_def_allow(char *p, int v)
{
  int i;
  for(i=0;i<VAR_ALLOW_NO && strcasecmp(p,var_allow[i])!=0; i++);
  if(i>=VAR_ALLOW_NO) module_error("bad_allow");
  else var_def[v].allow=i;
  return i;
}

int varsort(const void *p1, const void *p2)
{
  int i; const struct VAR_DEF *pp1, *pp2;
  pp1=p1; pp2=p2; i=strcmp(pp1->name,pp2->name);
  if(i) return i; else return pp1->end - pp2->end;
}

/* parse module's variable definitions */
void get_var_defs(void)
{
  int i, j, k, v, add;
  char *p, *p1, *wlist[MAX_VAR_NUM];

  defined_var_total=0;
  if(read_module_file(var_def_file)!=0) return;
  var_def_buf=m_file.textbuf;
  for(m_file.l=v=add=0;v<MAX_VAR_NUM && m_file.l<m_file.linecnt;m_file.l++) {
    if(m_file.lines[m_file.l].isstart!=1) continue;
    p=find_word_start(m_file.lines[m_file.l].address);
    if(*p==0 || *p==comment_prefix_char) continue;  /* empty or comment lines */
    items2words(p);
    if((p1=strchr(p,':'))!=NULL) {  /* new format */
      *p1=' '; i=cutwords(p,wlist,MAX_VAR_NUM); if(i<=1) continue;
      k=var_def_allow(wlist[0],v);
      for(j=1;j<i && v<MAX_VAR_NUM;j++) {
        add+=var_def_name(wlist[j],v); var_def[v].allow=k;
        v++;
      }
    }
    else {
      i=cutwords(p,wlist,3); if(i<2) module_error("too_few_columns");
      add+=var_def_name(wlist[0],v);
      var_def_allow(wlist[1],v);
      var_def[v].defined_in_parm=0;
      v++;
    }
  }
  for(j=0;j<INTERNALDEF_NAME_NO;j++) {
    add+=var_def_name(internaldef_name[j].name,v);
    var_def[v].allow=internaldef_name[j].stat;
    v++;
  }

  if(v>=MAX_VAR_NUM) module_error("too_many_variables");
  defined_var_total=v;
  qsort(var_def,v,sizeof(var_def[0]),varsort);
  p=getvar(ro_name[ro_module]);
  if(p==NULL || strncmp(p,"devel/",6)!=0) return;
  for(v=1;v<defined_var_total;v++) {
    if(strcmp(var_def[v].name,var_def[v-1].name)==0 &&
        var_def[v].beg<=var_def[v-1].end) {
      for(j=0;j<INTERNALDEF_NAME_NO;j++) {
        if(strcmp(internaldef_name[j].name,var_def[v-1].name)==0){
          setvar("wims_reserved_name",internaldef_name[j].name);
          module_error("name_is_reserved");
        }
      }
      setvar("wims_bad_name",var_def[v].name);
      module_error("multiple_declaration");
    }
  }
}

/* returns 1 if hacked, else 0. */
int try_hack(char *var)
{
  int i, al;
  char vbuf[16];
  i=var_def_check(var);
  if(i<0) return 0;
  al=var_def[i].allow;
  if(al != var_allow_any) switch(cmd_type) {
    case cmd_new:
    case cmd_renew: {
      if (al != var_allow_init && al != var_allow_config) return 0;
      else break;
    }
    case cmd_config: {
      if(al != var_allow_config) return 0;
      else break;
    }
    case cmd_reply: {
      if(al != var_allow_reply) return 0;
      else break;
    }
    case cmd_help: {
      if(al != var_allow_help) return 0;
      else break;
    }
    default: return 0;
  }
  snprintf(vbuf,sizeof(vbuf),"%d",(int) irand(21));
  var_hacking=1; setvar(var,vbuf); var_hacking=0;
  return 1;
}

/* set environ variables from last session save
 * The session var file starts with variables which should not
 * be restored. Variables which are restored follow a blank line. */
void set_vars_from_session(void)
{
  char lbuf[MAX_LINELEN+1];
  int i;
  char *p;

  if(session_var_ready) memmove(&m_file,&svar_file,sizeof(WORKING_FILE));
  else fopen_session_var_file("r");
  /* look for the first blank line. */
  for(i=0;i<m_file.linecnt && (m_file.lines[i].isstart==0 || m_file.lines[i].llen>0);i++);
  for(i++;i<m_file.linecnt && m_file.lines[i].llen==0;i++);
  if(i>=m_file.linecnt) return;
  m_file.linepointer=i;
  if(isdevelmodule && strstr(session_prefix,"_test")==NULL) isdevelmodule=0;
  while(wgetline(lbuf,MAX_LINELEN, &m_file)!=EOF) {
    p=strchr(lbuf,'=');
    if(p==NULL || p<=lbuf || isspace(lbuf[0]) ) {
  /* this time it is corrupted var file */
      call_ssh("cat %s/var >%s/corrupt.var.bak",session_prefix,log_dir);
      internal_error("get_vars_from_session(): corrupt session variable file.");
    }
    *p=0;p++;
    /* Here we suppose that nobody can tamper session variable
     * file, and do not check variable names against module's
     * definition file. Policy under reserve. */
    /* We do not allow override though.
     * Especially because reply variables should
     * be preserved. */
    if(strncmp(lbuf,var_prefix,strlen(var_prefix))!=0)
      setenv(lbuf,p,0);
    else
      if(lbuf[strlen(var_prefix)]!=0 && getvar(lbuf+strlen(var_prefix))==NULL) {
        if(!isdevelmodule || !try_hack(lbuf+strlen(var_prefix))) {
          setvar(lbuf+strlen(var_prefix),p);
        }
      }
  }
  close_working_file(&m_file,0);
}

void start_tracefile(void)
{
  int i;
  putc('\n',trace_file);
  for(i=1; i<=2*trace_indent; i++) putc(' ',trace_file);
  fprintf(trace_file,"%s:",m_file.filepath);
  fflush(trace_file);
  trace_indent++;
}

void stop_tracefile(void)
{
  trace_indent--;
}

/* Initialize environment variables according to module's
 * variable init or calculation file.
 * init is only used when cmd=new or renew.
 * Requires get_var_defs be run first. */
void var_proc(char *fname,int cache)
{
  int  t;
  char *p, tbuf[MAX_LINELEN+1];

  if(fname!=NULL && read_module_file(fname)) return;
  if(untrust&6) get_var_privileges();
  if (trace_file) start_tracefile();
  while(m_file.linepointer<m_file.linecnt) {
    if (trace_file) {fprintf(trace_file," %d",m_file.linepointer+1);
      fflush(trace_file);
    }
    t=m_file.lines[m_file.linepointer].isstart;
    if((t&~2)!=1 || m_file.lines[m_file.linepointer].llen==0) {
      m_file.linepointer++; continue;
    }
    wgetline(tbuf,MAX_LINELEN,&m_file); substnest=0;
    p=find_word_start(tbuf); if(*p==0) continue;
    if((t&2)!=0) exec_main(p+1);
    else exec_set(p);
  }
  if (trace_file) stop_tracefile();
  close_working_file(&m_file,cache);
}

/* Deposit the content of wims_deposit into a file */
void var_deposit(char *p)
{
  char fn[MAX_FNAME+1];
  int l,fd;
  if(!trusted_module()) return;
  if(deplen>0) l=deplen; else {
    while(isspace(*p)) p++;
    l=strlen(p);
  }
  if(l<=0) return;
  if(l>MAX_DEPOSITLEN) l=MAX_DEPOSITLEN; /* silent truncation, should not occur */
  mkfname(fn,"%s/user-deposit",session_prefix);
  fd=creat(fn,S_IRUSR|S_IWUSR|S_IRGRP|S_IROTH); if(fd==-1) return;
  write(fd,p,l); close(fd);
  snprintf(fn,sizeof(fn),"%u",l); setvar("wims_deposit_len",fn);
}

/* Check and set variables passed in query_string */
void set_vars_from_parm(void)
{
  int i,j,al;
  char *s, vbuf[MAX_LINELEN+1];
  if(forceresume) return;
  for(i=0; i<user_var_no; i++) {
    j=var_def_check(user_variable[i].name);
    if(j<0) continue;
    /* check permissions */
    al=var_def[j].allow;
    if(al != var_allow_any) switch(cmd_type) {
      case cmd_new:
      case cmd_renew:
        if (al != var_allow_init && al != var_allow_config) {
violat:        /* setvar(error_data_string,user_variable[i].name);
              user_error("allow_violation"); */
          goto loopend;
        }
      break;

      case cmd_config:
      if(al != var_allow_config) goto violat;
      break;

      case cmd_reply:
        if(al != var_allow_reply) goto violat;
        break;

      case cmd_help:
        if(al != var_allow_help) goto violat;
        break;

      default: goto violat;
    }
    var_def[j].defined_in_parm=1;
    if(strcmp(user_variable[i].name,"wims_deposit")==0) {
      var_deposit(user_variable[i].value); continue;
    }
    mystrncpy(vbuf,user_variable[i].value,sizeof(vbuf));
    if(strchr(vbuf,'$')!=NULL) {
      char *p;
      while((p=strchr(vbuf,'$'))!=NULL)
        string_modify(vbuf,p,p+1,"&#36;");
    }
    s=getvar(user_variable[i].name);
    if(s==NULL || *s==0) setvar(user_variable[i].name, vbuf);
    else {  /* concatenate */
      int k;
      k=strlen(s)+strlen(vbuf);
      if(k>=MAX_LINELEN-2) user_error("string_too_long");
      snprintf(tmplbuf,sizeof(tmplbuf),"%s, %s",s,vbuf);
      setvar(user_variable[i].name, tmplbuf);
    }
    loopend: ;
  }
}

/* parms to be eliminated from module_init_parm */
/* char *init_elim[]={
    "module","cmd","session","lang","worksheet","wims_access","useropts"
};
#define init_elim_no (sizeof(init_elim)/sizeof(init_elim[0]))
*/

void elim_parm(char *str, char *parm)
{
  char *p1, *p2;
  for(p1=strstr(str,parm);p1!=NULL;p1=strstr(p1+1,parm)) {
    if( (p1>str && *(p1-1)!='&') || *(p1+strlen(parm))!='=')
      continue;
    p2=strchr(p1,'&');
    if(p2==NULL) {
      if(p1>str) *(p1-1)=0; else *p1=0;
      return;
    }
    ovlstrcpy(p1,p2+1); p1--;
  }
}

/* eliminate technical definitions form parameter string
 * that is parameters from the list RO_NAMES
 */
void prep_init_parm(char rqv[])
{
  int i;
  char *p;

  for(p=strstr(rqv,"&+"); p!=NULL; p=strstr(++p,"&+"))
    ovlstrcpy(p+1,p+2);
  for(i=0;i<RO_NAME_NO;i++) elim_parm(rqv,ro_name[i]);
  if(strlen(rqv)>=MAX_LINELEN) rqv[0]=0;
  while(rqv[0]=='&') ovlstrcpy(rqv,rqv+1);
  while(rqv[0]!=0 && rqv[strlen(rqv)-1]=='&') rqv[strlen(rqv)-1]=0;
}

/* retain initializing parameters, for use in user references */
void set_init_parm(void)
{
  char *rq, rqv[MAX_LINELEN*2+2], *u, *sh, *seedr, *exotrymax;
  char *shname, *dirname;
  int public_sheet, freework_sheet;
  int len;

  if(isexam) return;
  /* reset sheet number and exo number for a sheet */
  force_setvar("wims_sheet",""); force_setvar("wims_exo","");
  /* reset freework number and exo number for a freework */
  force_setvar("wims_fwnumber",""); force_setvar("wims_fwexo","");
  rq=getenv("QUERY_STRING");
  if(rq==NULL || *rq==0) {
    empty:
    setvar("module_init_parm",""); return;
  }
  if(strlen(rq)>=MAX_LINELEN*2) goto empty;
  _http2env(rqv,rq); prep_init_parm(rqv);
  /*XSS PROTECTION see also config.c*/
  len=strcspn(rqv, "<>'\"\\"); rqv[len]=0;
  setvar("module_init_parm",rqv); public_sheet=0; freework_sheet=0;
/* seedrepeat=1 or an integer
  must be in the url and seedrepeat
  in the var.def of the module to be used */
  seedr=getvar("seedrepeat");
  force_setvar("wims_seed_repeat",seedr);
  exotrymax=getvar("exotrymax");
  force_setvar("wims_exotrymax",exotrymax);
/* now determine the sheet number for user */
  sh=getvar(ro_name[ro_worksheet]); if(sh==NULL) return;
  if(*sh=='P') {public_sheet=1; sh++;}  /* for public sheet file is in session */
  if(*sh=='F') {     /* for freework */
    freework_sheet=1; sh++;
    shname="Wfreework";
    dirname="freeworks";
  }
  else {   /* for sheet */
    shname="sheet";
    dirname="sheets";
  }
  u=getvar("wims_user"); if(u==NULL) u="";
  if(sh!=NULL && *sh!=0) {
    char buf[MAX_LINELEN+1],ubuf[32], nbuf[1024], *c, *m;
    char *p1,*p2,*p3,*p4,*p5;
    int i,j,sheet;
    sheet=atoi(sh); if(sheet<=0 || sheet>MAX_SHEETS) return;
    m=getvar(ro_name[ro_module]);
    if(m==NULL) internal_error("set_init_parm(): module name disapears.");
    if(*u==0) public_sheet=1;
    if(!public_sheet) {
    /* make filename for data on a freework or a sheet in a class */
      c=getvar("wims_class"); if(c==NULL) c="";
      snprintf(nbuf,sizeof(nbuf),"%s/%s/.%s%d",
               class_dir,dirname,shname,sheet);
    }
    else {
      /* make filename for data in case of a public sheet */
      char bf[MAX_LINELEN+1];
      int i;
      accessfile(bf,"r","%s/.sheets",session_prefix);
      if(bf[0]==0) return;
      for(i=1, p1=bf;i<sheet;i++,p1=p2) {
        p2=strchr(p1,'\n');
        if(p2!=NULL) *p2++=0; else p2=p1+strlen(p1);
      }
      p2=strchr(p1,'\n'); if(p2) *p2=0;
      snprintf(nbuf,sizeof(nbuf),"bases/sheet/%s.def",p1);
    }
    /* now nbuf is the filename for data in any case of ressource
      (sheet / public_sheet / freework) */
    if(readfile(nbuf,buf,sizeof(buf))==NULL) return;
    /* buf is the data of ressources. nbuf no more needed */
    for(p1=strstr(buf,"&+");p1!=NULL;p1=strstr(++p1,"&+"))    /* delete the &+ in buf */
      ovlstrcpy(p1+1,p1+2);
    /* in case of use an exercice of the class instead of a public exercise */
    if(strncmp(m,"classes/",strlen("classes/"))==0) {
      m="classes/";
      for(p1=strstr(buf,":classes/");p1;p1=strstr(p1+1,":classes/")) {
        if(p1==buf || *(p1-1)=='\n') {
          p1+=strlen(":classes/");
          p2=find_word_end(p1); if(p2>p1 && *p2=='\n') ovlstrcpy(p1,p2);
        }
      }
    }
    snprintf(nbuf,sizeof(nbuf),":%s\n%s\n",m,rqv);  /* rqv : execution parameters for the module ; m is the name of exercise module */
    p1=strstr(buf,nbuf);
    /* this loop searches exercise parameters in buf
      calculate num of the exercise in the ressources (sheet /public_sheet / freework)
    */
    while(p1>buf && *(p1-1)!='\n') p1=strstr(p1+1,nbuf);
    if(p1!=NULL) {
      p2=strchr(buf,':');
      while(p2>buf && *(p2-1)!='\n') p2=strchr(p2+1,':');
      for(i=1;p2!=NULL && p2<p1;i++) {
        p2=strchr(p2+1,':');
        while(p2>buf && *(p2-1)!='\n') p2=strchr(p2+1,':');
      }
      if(p2==NULL) return; /* error which should not occur */
      snprintf(ubuf,sizeof(ubuf),"%d",i);
      /* look for dependency information */
      /* pas utile pour freework mais ne pose pas de probleme tant que les fichiers .wfreework$i ne contiennent
       que des record avec moins de 7 lignes (pour ne pas activer la fonction de dependance
       de score  */
      for(j=0, p3=strchr(p1+strlen(nbuf),'\n');
      j<3 && p3 && *(p3+1)!=':';
      j++, p3=strchr(p3+1,'\n'));
      if(j>=3 && p3!=NULL && *(p3+1)!=':') {
        p3++; p4=strchr(p3,'\n');
        if(p4) {
          *p4++=0;
          if(*p4!=':') { /* options */
            p5=strchr(p4,'\n'); if(p5) *p5=0;
            force_setvar("wims_exoption",p4);
          }
        }
        p3=find_word_start(p3); strip_trailing_spaces(p3);
/* non-empty dependency information */
        if(*p3 && !public_sheet) {
          exodepOK=depcheck(sh,i,p3);
          if(!exodepOK) setvar("wims_exodep","pending");
        }
      }
      if(public_sheet) {
        char bf[32];
        snprintf(bf,16,"P%s",sh);
        force_setvar("wims_sheet",bf);
      }
      else {
        if(freework_sheet) {
          /* if an exo is already registered, user cannot launch the same exercise an other time */
          snprintf(nbuf,sizeof(nbuf),"%s/log/classes/%s/freeworksdata/%s/work/%s-wimsexo/%s",
               getvar("wims_home"),getvar("wims_class"),sh,u,ubuf);
          if(ftest(mkfname(NULL,"%s",nbuf))!=is_file) {
            force_setvar("wims_fwnumber",sh);
            force_setvar("wims_fwexo",ubuf);
          }
        }
        else {
          force_setvar("wims_sheet",sh);
          force_setvar("wims_exo",ubuf);
          wims_sheet=sheet; wims_exo=i;
        }
      }
    }
  }
}

/* user with class: whether exercise is registered
 * Returns 1 if got, 0 otherwise. */
int get_parmreg(void)
{
  char *p, *cl, *u, nbuf[MAX_FNAME+1];
  struct stat st;

  u=getvar("wims_user"); cl=getvar("wims_class");
  if(u==NULL || cl==NULL || *u==0 || *cl==0 || strcmp(u,"supervisor")==0
       || wims_sheet<=0 || wims_exo<=0) return 0;
  mkfname(nbuf,"%s/.parmreg/%s.%d.%d", class_dir,u,wims_sheet,wims_exo);
  p=getvar("wims_scorereg");
  if(p!=NULL && strcmp(p,"suspend")==0) {
    unlink(nbuf); return 0;
  }
  if(stat(nbuf,&st)) return 0;
  /* latency is 10 min. */
  if(st.st_mtime<nowtime-600 || st.st_mtime > nowtime) {
     unlink(nbuf); return 0;
  }
  if(open_working_file(&m_file,nbuf)!=0) return 0;
  mkfname(m_file.name,"parmreg/%s.%d.%d",u,wims_sheet,wims_exo);
  while(wgetline(tmplbuf,MAX_LINELEN, &m_file)!=EOF) {
    p=strchr(tmplbuf,'=');
    if(p==NULL || p<=tmplbuf || isspace(tmplbuf[0]) )
      /* this time it is corrupted var file */
      internal_error("get_parmreg(): corrupt parmreg file.");
    *p=0;p++;
    if(strncmp(tmplbuf,var_prefix,strlen(var_prefix))!=0) setenv(tmplbuf,p,1);
    else if(tmplbuf[strlen(var_prefix)]!=0) force_setvar(tmplbuf+strlen(var_prefix),p);
  }
  parm_restore=1;
  close_working_file(&m_file,0); return 1;
}

/* reset seed according to the values of
  wims_seed_repeat and wims_seed_score
*/
/* for non registered users or suspended score,
  save some variables wims_seedcnt and seedlastcnt in sessions/xxx/var
*/
void reset_seed (void) {
  char *p, *seedr, buf[64], *sh, *ex, *tseed, *seedcnt, *seedlastcnt;
  char *mp=getvar("module");
  sh=getvar("wims_sheet"); ex=getvar("wims_exo");
  seedr=getvar("wims_seed_repeat");
  /* number of seeds during the sessions only used for unregistered */
  seedcnt=getvar("wims_seedcnt");
  if(seedcnt==NULL || *seedcnt==0) {
    force_setvar("wims_seedcnt","0");
    seedcnt=getvar("wims_seedcnt");
  }
  /* number of tries with the actual seed only used for unregistered
    or score suspended */
  seedlastcnt=getvar("wims_seedlastcnt");
  if(seedlastcnt==NULL || *seedlastcnt==0) {
    force_setvar("wims_seedlastcnt","0");
    seedlastcnt=getvar("wims_seedlastcnt");
  }
  if(seedr!=NULL && atoi(seedr)>0) { /* seedrepeat>1 */
    p=getvar("wims_seed_score");
    if(evalue(p)>=10){
      seed_time(); force_setvar("wims_seed_score",NULL);
      snprintf(buf,sizeof(buf), "%d", atoi(seedcnt)+1);
      force_setvar("wims_seedcnt",buf);
      seedcnt=getvar("wims_seedcnt");
      force_setvar("wims_seedlastcnt","0");
      seedlastcnt=getvar("wims_seedlastcnt");
    }
    else { /* score < 10 */
      /* is score suspended by the student ? */
      char *screg=getvar("wims_scorereg");
      /* is score suspended by the supervisor ? */
      int scregs=0;
      if(sh!=NULL && *sh!=0)
        scregs=getscorestatus(getvar("wims_class"),atoi(sh));
      if(sh!=NULL && *sh!=0 && ex!=NULL && *ex!=0
          && (screg==NULL || strcmp(screg,"suspend")!=0)
          && (scregs!=0)){
        /* the case of a supervisor looking at the sheet is not treated */
        tseed=getseedscore(getvar("wims_class"), getvar("wims_user"), atoi(sh), atoi(ex));
        if( tseed!=0 ) {
          mystrncpy(buf, tseed,sizeof(buf));
          force_setvar("wims_seed",buf);
        }
        else seed_time();
      }
      else { /* non registered or score suspended
          and seedrepeat>1 and score <10 */
        if (cmd_type==cmd_new ||cmd_type==cmd_renew) {
          snprintf(buf,sizeof(buf),"%d",atoi(seedlastcnt)+1);
          force_setvar("wims_seedlastcnt",buf);
          seedlastcnt=getvar("wims_seedlastcnt");
          if(atoi(seedlastcnt) >= atoi(seedr)){
            seed_time();
            if(strcmp(mp,home_module)!=0 && cmd_type!=cmd_intro){
              snprintf(buf,sizeof(buf),"%d",0);
              force_setvar("wims_seedlastcnt",buf);
              seedlastcnt=getvar("wims_seedlastcnt");
              snprintf(buf,sizeof(buf),"%d",atoi(seedcnt)+1);
              force_setvar("wims_seedcnt",buf);
              seedcnt=getvar("wims_seedcnt");
            }
            else {
              force_setvar("wims_seedcnt","0");
              force_setvar("wims_seedlastcnt","0");
            }
          }
        }
      }
    }
  }
  else {// no seedrepeat
    seed_time();
    if (cmd_type==cmd_reply && strcmp(mp,home_module)!=0){
      snprintf(buf,sizeof(buf),"%d",atoi(seedcnt)+1);
      force_setvar("wims_seedcnt",buf);
      seedcnt=getvar("wims_seedcnt");
      force_setvar("wims_seedlastcnt","0");
    }
    else {
      if (strcmp(mp,home_module)==0 || cmd_type==cmd_intro) {
        force_setvar("wims_seedcnt","0");
        force_setvar("wims_seedlastcnt","0");
      }
    }
  }
  p=getvar("wims_seed");
  if(p!=NULL && (cmd_type==cmd_new || cmd_type==cmd_renew || cmd_type==cmd_next)){
    mystrncpy(buf,p,sizeof(buf)); setenv("tmp_seed",buf,1);
    exec_setseed(buf);
  }
}

/* set environment variables */
void set_variables(void)
{
  outputing=0; readnest=0;
  get_var_defs();
  set_vars_from_parm();
  if(cmd_type != cmd_new && cmd_type != cmd_renew) {
    set_vars_from_session();
    if(cmd_type==cmd_next) reset_seed();
  }
  else {
    set_init_parm();
    reset_seed();
    if(wims_sheet>0 && get_parmreg()) {
      cmd_type=cmd_resume; force_setvar("cmd","resume");
      var_proc(main_var_proc_file,0); return;
    }
    checkrafale();
    var_proc(var_init_file,0);
  }
/* check_var_bounds(); */
  var_proc(main_var_proc_file,0);
}

/* Output a phtml file. */
void phtml_put(char *fname,int cache)
{
  int t;
  char tbuf[MAX_LINELEN+1];
  char *s;
  outputing=1;
     /* File not found; we give empty output, but no error message. */
  if(fname!=NULL && read_module_file(fname)!=0) return;
  if(untrust&6) get_var_privileges();
  if (trace_file) start_tracefile();
  while(m_file.linepointer<m_file.linecnt) {
    if (trace_file) {
      fprintf(trace_file," %d",m_file.linepointer+1);
      fflush(trace_file);
    }
    t=m_file.lines[m_file.linepointer].isstart;
    if((t&~18)!=1) {m_file.linepointer++; continue;}
    wgetline(tbuf,MAX_LINELEN,&m_file); substnest=0;
    if((t&2)!=0) {exec_main(tbuf+1); continue;}
    substit(tbuf);
    s=getvar("wims_parm_detag");
    if(s!=NULL && *s != 0) calc_detag(tbuf);
    output0(tbuf); _output_("\n");
  }
  if (trace_file) stop_tracefile();
  close_working_file(&m_file,cache);
}

/* output a file in base html directory. Internal use only. */
void phtml_put_base(char *fname,int cache)
{
  WORKING_FILE save;
  char modsave[MAX_FNAME+1];
  memmove(&save,&m_file,sizeof(WORKING_FILE));
  mystrncpy(modsave,module_prefix,sizeof(modsave));
  ovlstrcpy(module_prefix,"html");
  phtml_put(fname,cache);
  mystrncpy(module_prefix,modsave,sizeof(module_prefix));
  memmove(&m_file,&save,sizeof(WORKING_FILE));
}

/* Read main.phtml, process it, and write to stdout. */
void main_phtml_put(char *mname)
{
  char *p, buf[1024], txbuf[256], dirnbuf[256], bgbuf[256], spfbuf[256], ebuf[256];
  char *bcolor, *refcolor, *bg, *tx, *dirn, *vlink, *link, *hlink, *spf ;
  define_html_header(); readnest=0;
  nph_header(200);
  p=getvar("wims_backslash_insmath");
  if(p!=NULL && strcasecmp(p,"yes")==0) backslash_insmath=1;
  p=getvar("wims_expire");
  if(p!=NULL) p=strstr(p,"no-cache");
  if(p!=NULL) _output_("Cache-Control: no-cache, no-store, must-revalidate\r\n");
/* expiration date */
  if(mode==mode_default) {
    if(!robot_access && (cmd_type!=cmd_intro || p!=NULL)) {
      char *expir="Sat, 26 Jul 1990 05:00:00 GMT";
      snprintf(ebuf,sizeof(ebuf),"%s",expir);
    }
    else {
/* expiration in 10 days for robot access or intro page. */
      time_t t=nowtime+(long) 10*24*3600;
      { struct tm *tt = localtime(&t);
        strftime(ebuf,sizeof(ebuf),"%a, %d %b %Y %T GMT", tt);
      }
    }
    strip_trailing_spaces(ebuf);
    output("Expires:%s\r\n",ebuf);
  }

  output("Server: %s %s (%s)\n", SHORTSWNAME,wims_version,LONGSWNAME);
  if(!robot_access && strcasecmp(usecookie,"yes")==0 && setcookie
       && mode!=mode_popup) {
    if(cookieset[0]==0) {
      p=getvar("wims_sescookie");
      if(p!=NULL && *p!=0) mystrncpy(cookieset,p,sizeof(cookieset));
    }
     output("Set-Cookie: %s%s;SameSite=Strict; path=/\r\n",cookieheader,cookieset);
  }
  p=getvar("wims_main_font");
  if(p!=NULL && *p!=0) output("Content-type: text/html; charset=%s\r\n\r\n",p);
  else _output_("Content-type: text/html\r\n\r\n");

  bcolor=getvar("wims_bgcolor");
  if(bcolor==NULL || *bcolor==0) bcolor=bgcolor;

  link=getvar("wims_link_color");
  if(link==NULL || *link==0) link="blue";

  vlink=getvar("wims_vlink_color");
  if(vlink==NULL || *vlink==0) vlink="blue";

  hlink=getvar("wims_hlink_color");
  if(hlink==NULL || *hlink==0) vlink="red";

  refcolor=getvar("wims_ref_bgcolor");
  if(refcolor==NULL || *refcolor==0) {
    setvar("wims_ref_bgcolor","white");
    refcolor="white";
  }

  bg=getvar("wims_bgimg"); bgbuf[0]=0;
  if(bg!=NULL && *bg!=0 && strchr(bg,'\"')==NULL) {
    if(strchr(bg,'/')==NULL)
      snprintf(bgbuf,sizeof(bgbuf),"background-image: url(\"gifs/bg/%s\");",bg);
     else
      snprintf(bgbuf,sizeof(bgbuf),"background-image: url(\"%s\");",bg);
  }
  setvar("wims_bodyimg",bgbuf);
  tx=getvar("wims_textcolor");
  if(tx!=NULL && *tx!=0 && strchr(tx,'\"')==NULL) {
    snprintf(txbuf,sizeof(txbuf),"color:%s;",tx);
  }
  else txbuf[0]=0;
   /* special fonts defined in special_font -should also take the value specialfont in wims.conf */
  if(spec_font==1){
    spf=getvar("wims_specialfont");
    if(spf!=NULL && *spf!=0) { snprintf(spfbuf,sizeof(spfbuf),"%s",spf);}
    else { snprintf(spfbuf,sizeof(spfbuf),"%s",special_font); }
    setvar("wims_special_font",spfbuf);
  }
  dirn=getvar("wims_main_dirn"); /* "rtl" for arabic writing ; on pourrait laisser vide pour les autres ? */
  if(dirn!=NULL && *dirn!=0 && strchr(dirn,'\"')==NULL) {
    snprintf(dirnbuf,sizeof(dirnbuf),"dir=\"%s\"",dirn);
  }
  else dirnbuf[0]=0;
  if(mode==mode_popup) {
    snprintf(buf,sizeof(buf), "class=\"main_body mode_popup\" %s", dirnbuf);
  }else{
    snprintf(buf,sizeof(buf), "class=\"main_body\" %s",dirnbuf);
  }
  setvar("wims_htmlbody",buf);
  phtml_put(mname,0);
}

void _write_var(char *name, FILE *varf,int user,int skip)
{
  char *s, buf[MAX_NAMELEN+9], nbf[MAX_NAMELEN+9];

  if((user&2)!=0) {
    snprintf(nbf,sizeof(nbf),"%s%s",wims_prefix,name); name=nbf;
  }
  if((user&1)!=0) {
    snprintf(buf,sizeof(buf),"%s%s",var_prefix,name); s=_getvar(name);
  }
  else {
    mystrncpy(buf,name,sizeof(buf)); s=getenv(name);
  }
  if(s==NULL) s="";
  if(skip && *s==0) return;
  fprintf(varf,"%s=",buf);
  if(strchr(s,'\n')==NULL) fputs(s,varf);
  else
    while(*s) {
      if(*s=='\n') fputc('\\',varf);
       fputc(*s,varf); s++;
    }
  fputc('\n',varf);
}

void _write_vars(FILE *varf)
{
  int i,j,k;
  char nbuf[MAX_NAMELEN+1];
  for(i=0;i<defined_var_total;i++) {
    if(var_def[i].beg>=0) {
      if(var_def[i].end<=var_def[i].beg) {
        snprintf(nbuf,sizeof(nbuf),"%s%d",var_def[i].name,var_def[i].beg);
        _write_var(nbuf,varf,1,1);
      }
      else {
        char *pbuf[MAX_VAR_NUM];
        j=varsuite(var_def[i].name,var_def[i].beg,var_def[i].end,pbuf,MAX_VAR_NUM);
        for(k=0;k<j;k++) _write_var(pbuf[k],varf,1,1);
      }
    }
    else _write_var(var_def[i].name,varf,1,1);
  }
}

/* save exercise parm for registered users */
void save_parmreg(void)
{
  char *p, *u, *sh, *ex, nbuf[MAX_FNAME+1], dbuf[MAX_FNAME+1];
  int s;
  FILE *varf;

  if(cmd_type!=cmd_reply && cmd_type!=cmd_new && cmd_type!=cmd_renew)
    return;
  u=getvar("wims_user");
  if(class_dir[0]==0 || *u==0) return;
  sh=getvar("wims_sheet"); ex=getvar("wims_exo");
  if(sh==NULL || ex==NULL || *sh==0 || *ex==0) return;
  mkfname(nbuf,"%s/.parmreg/%s.%s.%s", class_dir,u,sh,ex);
  if(cmd_type==cmd_reply) {
    unlink(nbuf); return;
  }
  p=getvar("wims_scorereg");
  if(p!=NULL && strcmp(p,"suspend")==0) return;
/* these two lines must be before random factor
 * in order to set variable wims_scoring */
  s=atoi(sh); if(getscorestatus(getvar("wims_class"),s)==0) return;
  if(strcmp(u,"supervisor")==0) return;
/* 0.2 is random factor to trigger exercise parm register */
/*    return;*/
 // if((double) random()<(double) RAND_MAX*0.2){ }
  return;
  mkfname(dbuf,"%s/.parmreg",class_dir);
  mkdirs(dbuf); varf=fopen(nbuf,"w"); if(varf==NULL) return;
  _write_var("module_init_parm",varf,1,0);
  _write_var("wims_sheet",varf,1,1);
  _write_var("wims_exo",varf,1,1);
  _write_var("wims_scoring",varf,1,1);
  _write_var("wims_seed",varf,1,1);
  _write_vars(varf);
  fclose(varf);
}

void exolog(char *fname)
{
  FILE *varf, *varg;
  int c;
  char logftmp[MAX_FNAME+1], *sess;

/* add by FG to put w_module_score at the top of the file */
  if ((getenv("w_module_score"))!=NULL )
  {
    sess=getvar("wims_session");
    mkfname(logftmp,"%s/%s/.tmp%lu",s2_dir,sess,nowtime);
    rename(fname,logftmp);
    varf=fopen(fname,"a");
    if(varf!=NULL) {
      _write_var("module_score",varf,1,1);
      varg=fopen(logftmp,"r");
      if(varg!=NULL) {
        while ( (c=getc (varg)) != EOF) putc(c,varf);
        fclose(varg);
        }
        fclose(varf);
        remove(logftmp);
     }
  }
/* end of add */
  varf=fopen(fname,"a");
  if(varf!=NULL) {
/* The first 4 lines should not be modified */
    fprintf(varf,"\n:\n");
    _write_var("QUERY_STRING",varf,0,0);
    _write_var("module",varf,1,0);
    _write_var("cmd",varf,1,0);
    _write_var("module_init_parm",varf,1,0);
    _write_var("wims_scoring",varf,1,1);
    _write_var("module_score",varf,1,1);
    fprintf(varf,"w_wims_checktime1=%lu\n\
w_wims_checktime2=%s\n",nowtime,nowstr);
    _write_var("wims_seed",varf,1,1);
    _write_vars(varf);
    fclose(varf);
  }
}

/* save variables to session var file */
void save_session_vars(void)
{
  int i;
  char *mp, *sh, *ex, *ll;
  FILE *varf;
/* light pages don't need saved variables. ? */
/*    if(varchr(module_prefix,"light")!=NULL) return;*/
  if(robot_access) return;
/* no variable saving if cmd=help? */
  if(cmd_type==cmd_help) return;
  lastdatafile[0]=lastftest[0]=0;
  lessrafale();
  mp=getvar("wims_nextmodule");
  if(mp!=NULL && *mp!=0) {
    mp=getvar("module");
    if(strcmp(mp,home_module)==0)
      force_setvar("module",getvar("wims_nextmodule"));
  }
  varf=fopen_session_var_file("w");
  for(i=0;i<HEADER_VAR_NO;i++) _write_var(header_var_name[i],varf,0,0);
  for(i=0;i<RO_NAME_NO;i++) _write_var(ro_name[i],varf,1,0);
  for(i=0;i<INTERNAL_NAME_NO;i++) {
    if(internal_name[i].stat==0)
      _write_var(internal_name[i].name,varf,3,0);
  }
  _write_var("password",varf,0,1);
  save_parmreg();
  if(new_session) fprintf(varf,"wims_new_session=yes\n");
  fprintf(varf,"\n");
  _write_var("module_init_parm",varf,1,0);
  _write_var("wims_sheet",varf,1,1);
  _write_var("wims_exo",varf,1,1);
  _write_var("wims_scoring",varf,1,1);
  _write_vars(varf);
  fclose(varf);
  if(cmd_type==cmd_new || cmd_type==cmd_renew || cmd_type==cmd_reply || cmd_type==cmd_next) {
    ll=getvar("wims_class_examlog");
    if(ll!=NULL && *ll!=0) i=atoi(ll); else i=examlog_limit;
    if(strstr(session_prefix,"_exam")!=NULL && examlogf[0]!=0 &&
        simuxam==0 && i>0) {
      mkdirs(examlogd); exolog(examlogf);
    }
    else {
      ll=getvar("wims_class_exolog");
      sh=getvar("wims_sheet"); ex=getvar("wims_exo");
      if(ll!=NULL && atoi(ll)>0) {
        char buf[MAX_FNAME+1];
        if(sh!=NULL && *sh!=0 && ex!=NULL && *ex!=0){
          mkfname(buf,"%s/exolog.%s.%s",session_prefix,sh,ex);
        }
        else { mkfname(buf,"%s/exolog",session_prefix);}
        if(cmd_type==cmd_new || cmd_type==cmd_renew) unlink(buf);
        exolog(buf);
      }
      /* for auto log of exercise in freework */
      if(freeworklogf[0]!=0) {
        char buf[MAX_FNAME+1],*sess,c;
        FILE *varg;
        mkdirs(freeworklogd);
        sess=getvar("wims_session");
        mkfname(buf,"%s/%s/exolog",session_dir,sess);
        varg=fopen(buf,"r");
        if(varg!=NULL) {
          varf=fopen(freeworklogf,"w");
          while ( (c=getc (varg)) != EOF)
            putc (c,varf);
          fclose(varg);
          fclose(varf);
        }
      }
    }
  }
  if(var_def_buf) free(var_def_buf);
}
