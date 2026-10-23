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

/* This file contains various configuration routines for wims. */

/* Limitations are essentially for security reasons:
 * to curb attacks by resource exhaustion of the server.
 */

#include "wims.h"
/* variables shared with wimslog.c */
#include "commun.h"

/* maximal length of any module's variable definition file. */
int VAR_DEF_LENGTH_LIMIT=50000;
/* Limit for frames in an animation. */
int ANIM_LIMIT=400;


/* Resource limits */
int threshold1=150; /* First load threshold */
int threshold2=300; /* Second load threshold */
int ispriority=0; /* whether the connection is from priority sites */

int rafalvl=10; /* anti-rapidfire severity */

/* Explanation: two real numbers a,b such as * |a+b|>|a-b|*compare_precision
 * will be considered equal in !ifvalue comparisons. */
int DEFAULT_COMPARE_PRECISION=10000;
/* Precision of printing (hence of all evaluations). */
int DEFAULT_PRINT_PRECISION=8;
/* Defaults for instex, insplot, etc. */
char *DEFAULT_INS_FORMAT="gif";
char *DEFAULT_ANIM_FORMAT="gif";
char *DEFAULT_INS_DENSITY="100x100";
char *DEFAULT_INSPLOT_FONT="small";

/* gnuplot intermediate graphics format */
char *gnuplot_format="gif";

/* electronic address of site manager. */
char *site_manager="wims@$httpd_HTTP_HOST";
/* Main configure file, this name cannot be configured! */
char *config_file="../log/wims.conf";
char PATH[512];  /* directory settings */
char *commonpath="/usr/local/bin:/usr/bin:/usr/games:/bin:/usr/bin/X11:/usr/X11R6/bin:/usr/texbin:/usr/local/texbin";
char *gap_command="gap.sh -T -m 64M";
char *maxima_command="maxima -X '--dynamic-space-size 512'";
char *pari_command="gp -f -q -p 150000";
char *octave_command="octave -Hq --no-line-editing --no-window-system --silent";
char *scilab_command="scilab -nwni";
char *QRencode_command="qrencode -o";

/* default languages */
char site_langbuf[]="en fr es it nl ca si de cn";
char *site_languages=site_langbuf;
/* Statistics shows up? */
char *show_stat="yes";
/* Author names show up in search result? */
char *show_author="no";
/* Show user IP address to supervisor */
char *show_ip="yes";
/* busy hours definition */
char *busyhours="";
/* Presentation theme */
char *theme="standard";
/* directory of session files */
char session_dir[MAX_FNAME+1], s2_dir[MAX_FNAME+1];
/* standardised header model for modules' output. Will be stored
 * into the variable $wims_html_header. If the theme contains
 * a file 'head.phtml', html_header will be defined by the address
 * of this file  in subroutine define_html_header
 */
char *html_header="html/header.phtml";
/* standardised about table for modules */
char *module_about_file="html/module_about";
/* title definition page */
char *title_page="title.phtml";
/* standardised header menu bar, to be included in module pages
 * via command !headmenu */
char *header_menu="headmenu.phtml";
char *header_menu_user="headmenu_user.phtml";
char *header_menu_supervisor="headmenu_supervisor.phtml";
/* standardised referer to wims home, to be included in
 * module pages via command !homeref */
char *home_referer="homeref.phtml";
char *home_referer_user="homeref_user.phtml";
char *home_referer_supervisor="homeref_supervisor.phtml";
/* background color in standard reference tables */
char *ref_bgcolor="#676767";
/* standard link color */
char *link_color="#1468A0";
/* visited link color */
char *vlink_color="#1779BA";
/* hover link color */
char *hlink_color="#004077";
/* link color in standard reference tables */
char *ref_menucolor="#FFFFFF";
/* background help button color */
char *ref_button_help_bgcolor="#008f00";
/* background help button color */
char *ref_button_help_color="#FFFFFF";
  /* button background color */
char *ref_button_bgcolor="#666666";
  /* button color */
char *ref_button_color="#FFFFFF";
  /* Default colors for activity types */
char *actcolor="#000000,#CCCCFF,#99CCFF,#9999FF,#6666FF,#FFFFFF,#99CCDD,#AFB31E,#E6A493";
/* module log disabled by default */
int modlog=0;
/* name of the variable definition file for every module.
 * It is this file which defines the presence of the module.
* Better do not make it configurable.
*/
char var_def_file[256];
/* name of variable init file. Processed only when cmd=new or renew. */
char *var_init_file="var.init";
/* name of variable calculation file. Parsed at each call. */
char *main_var_proc_file="var.proc";
/* name of the main (programmable) html file. */
char *html_file="main.phtml";
/* Introduction page name */
char *intro_file="intro.phtml";
/* Reference page */
char *ref_file="wims_ref.phtml";
/* script to process different inserts.
 * The two consecutive points in the name prohibits ordinary
 * user execution of the script.
 */
char *insdraw_processor="insdraw..processor";
char *insplot_processor="insplot..processor";
char *tex2gif="bin/tex..gif";
char *priority=""; /* IPs of priority sites */
char *texgif_fontdir="w/texgif";
char *texgif_texheader="mathfonts/header";
char *bgcolor="#F6F6F6"; /* page background color */
char *bgimg="";  /* page background image file */
char *pagecss="-theme-";  /* style sheet file */
char *theme_icon="default";  /* icon */
char *usecookie="no";  /* whether to send cookies to anon requests */
char *mail_addrsep=","; /* separator for multiple mail addresses */
char *mail_opt="";  /* !mailto options */
char *mail_hidden="no"; /* option to hide or not user mail in server */
char nodeip[256]="";  /* ip of cluster node if relevant */
char temp_log[MAX_FNAME+1]; /* temp log file name */
 /* 0: low; 1: high; 2: MathML */
int mathalign_base=0;  /* Not use <sup> for middle alignment */
char *disable_zoom="no";  /* yes/no:  default zooming allowed (if enabled via "useropts")  */

int spec_font=0; /* choose of some fonts*/
 /* special font can be configured in log/wims.conf with variable special_font*/
char *special_font="letter-spacing:0.1em;line-height:2.7em;word-spacing:0.8em;font-family:OpenDyslexic3,OpenDyslexic,sans-serif !important;";
int backup_hour=-1; /* Hour for daily backup; -1 means no backup. */
int site_accounting=1; /* whether accounting should be activated */
int examlog_limit=7; /* number of logged exam sessions for each student */
char *site_hostname="";

/* Automatic module update */
char *mu_auto="none";
char *mu_cat="all";
char *mu_delay="3";
char *mu_site="https://wims.univ-cotedazur.fr/download/modules";
char *mu_zone="all";
char *mu_download_site="unice,wimsedu";
char *mu_publish_site="unice";

/* keywords destined to robots */
char *site_keywords="interactive mathematics, interactive math, server side interactivity";
char *site_description="interactive exercises, online calculators and plotters, mathematical recreation and games";

/* class and document authorization */
int class_quota=150;
int superclass_quota=150;
int doc_quota=32;
char *class_regpass="email,email,deny";
char *doc_regpass="";

/* Where to find programs; if your httpd sets /bin:/usr/bin in PATH,
 * this should be OK.
 */
char *sed_prog="sed";
char *tr_prog="tr";
char *awk_prog="awk";

  /* myname */
char *cgi_name="wims.cgi";
char *aliased_cgi="no";
char *aliased_getfile="yes";

  /* Limits for classes and participants */
int class_limit=200; /* limit of number of classes */
int user_limit=5000; /* limit of total number of registered users */
int class_user_limit=300; /* limit of users in one class */
int forum_limit=100;  /* Limit of message boards */
int max_techvar=5; /* default limit number of technical variable */

CONFIG_DATA main_config[]={
  {"PATH",0, &commonpath},
  {"QRencode_command",2, &QRencode_command},
  {"actcolor",0, &actcolor},
  {"aliased_cgi",0, &aliased_cgi},
  {"aliased_getfile",0, &aliased_getfile},
  {"backup_hour",1, &backup_hour},
  {"busyhours",0, &busyhours},
  {"cgi_name",0, &cgi_name},
  {"class_limit",1, &class_limit},
  {"class_quota",1, &class_quota},
  {"class_regpass",0, &class_regpass},
  {"class_user_limit",1, &class_user_limit},
  {"css",0, &pagecss},
  {"default_anim_format",0, &DEFAULT_ANIM_FORMAT},
  {"default_ins_format",0, &DEFAULT_INS_FORMAT},
  {"default_insplot_font",0, &DEFAULT_INSPLOT_FONT},
  {"default_texposition",1, &mathalign_base},
  {"devel_modules",0, &devel_modules},
  {"doc_quota",1, &doc_quota},
  {"doc_regpass",0, &doc_regpass},
  {"examlog_limit",1, &examlog_limit},
  {"forum_limit",1, &forum_limit},
  {"gap_command",2, &gap_command},
  {"general_log_limit",1, &GEN_LOG_LIMIT},
  {"gnuplot_format",0, &gnuplot_format},
  {"hlink_color",0, &hlink_color},
  {"home_module",0, &home_module},
  {"idle_time",1, &idle_time},
  {"idle_time2",1, &idle_time2},
  {"idle_time3",1, &idle_time3},
  {"insdraw_processor",0, &insdraw_processor},
  {"link_color",0, &link_color},
  {"log_delete",1,&LOG_DELETE},
  {"mail_addrsep",0, &mail_addrsep},
  {"mail_hidden",0, &mail_hidden},
  {"mail_opt",0, &mail_opt},
  {"manager_https",1, &manager_https},
  {"manager_site",0, &manager_site},
  {"max_techvar",1,&max_techvar},
  {"maxima_command",2, &maxima_command},
  {"module_log",1, &modlog},
  {"module_log_limit",1, &MODULE_LOG_LIMIT},
  {"mu_auto",0, &mu_auto},
  {"mu_cat",0, &mu_cat},
  {"mu_delay",0, &mu_delay},
  {"mu_download_site",0, &mu_download_site},
  {"mu_publish_site",0, &mu_publish_site},
  {"mu_site",0, &mu_site},
  {"mu_zone",0, &mu_zone},
  {"octave_command",2, &octave_command},
  {"old_log_files",1, &OLD_LOG_FILES},
  {"page_bgcolor",0, &bgcolor},
  {"page_bgimg",0, &bgimg},
  {"pari_command",2, &pari_command},
  {"priority",0, &priority},
  {"rafale_level",1, &rafalvl},
  {"ref_bgcolor",0, &ref_bgcolor},
  {"ref_button_bgcolor",0, &ref_button_bgcolor},
  {"ref_button_color",0, &ref_button_color},
  {"ref_button_help_bgcolor",0, &ref_button_help_bgcolor},
  {"ref_button_help_color",0, &ref_button_help_color},
  {"ref_menucolor",0, &ref_menucolor},
  {"rlimit_cpu",1, &rlimit_cpu},
  {"scilab_command",2, &scilab_command},
  {"show_author",0, &show_author},
  {"show_ip",0, &show_ip},
  {"show_stat",0, &show_stat},
  {"site_accounting",1, &site_accounting},
  {"site_description",0, &site_description},
  {"site_hostname",0, &site_hostname},
  {"site_keywords",0, &site_keywords},
  {"site_languages",0, &site_languages},
  {"site_manager",0, &site_manager},
  {"superclass_quota",1, &superclass_quota},
  {"texbasesize",1, &texbasesize},
  {"theme",0, &theme},
  {"theme_icon",0, &theme_icon},
  {"threshold1",1, &threshold1},
  {"threshold2",1, &threshold2},
  {"tmp_debug",0, &tmp_debug},
  {"tmp_debug_use_var",0, &tmp_debug_use_var},
  {"tmp_debug_var",0, &tmp_debug_var},
  {"trusted_modules",0, &trusted_modules},
  {"usecookie",0, &usecookie},
  {"user_limit",1, &user_limit},
  {"var_def_length_limit",1, &VAR_DEF_LENGTH_LIMIT},
  {"vlink_color",0, &vlink_color},
  {"workfile_limit",1, &WORKFILE_LIMIT}
};
int MAIN_CONFIG_NO=(sizeof(main_config)/sizeof(main_config[0]));

/* processes the list of languages supported on the site */
void language_list(void)
{
  int i,k;
  char *lbuf[MAX_LANGUAGES];
  calc_tolower(site_languages); items2words(site_languages);
  setvar("wims_site_languages",site_languages);
  i=cutwords(site_languages,lbuf,MAX_LANGUAGES);
  available_lang_no=i;
  for(i=k=0;i<available_lang_no;i++) {
    if(strlen(lbuf[i])==2) memmove(available_lang[k++],lbuf[i],3);
  }
  if(k>0) memmove(lang,available_lang[0],3);
}

char hostname[256];
int html_call=0;

/* determine the http reference name of the server */
void determine_ref_name(void)
{
  char *s1, *s2, *p, *pl;
  char buf[MAX_LINELEN+1];
  int len;

  hostname[0]=0;
  s1=getvar("httpd_HTTP_HOST");s2=getvar("httpd_SCRIPT_NAME");
  gethostname(hostname,sizeof(hostname));
  if(hostname[0]==0 && s1 && *s1!=0) mystrncpy(hostname,s1,sizeof(hostname));
  if(s1==NULL || *s1==0) s1=hostname;
  setvar("wims_myhostname",hostname);
  ref_base[0]=0;
  if(s2!=NULL) mystrncpy(buf,s2,sizeof(buf));
  else buf[0]=0;
  if(s1!=NULL && *buf=='/') {
    p=strrchr(buf,'/'); if(p==NULL) p=buf; else *p++=0;
    /* address of type /wims/fr_H2~algebra~oeffraction.fr.html */
    if(strcmp(p,cgi_name)!=0 && strlen(p)>4 &&
       p[2]=='_' && islower(p[0]) && islower(p[1])) {
      pre_language[0]=p[0];pre_language[1]=p[1];pre_language[2]=0;
      p[2]=0; setenv("HTTP_ACCEPT_LANGUAGE",p,1); pl=p; p+=3;
      s2=strrchr(p,'.');
      if(s2!=NULL) {
        *s2=0;
        if(strcasecmp(s2+1,"cgi")!=0) html_call=1;
      }
      s2=p; while((s2=strpbrk(s2,"@~"))!=NULL) *s2='/';
      if(strncmp(p,"doc/",4)==0) {
        char *pd, *pb;
        pd=p+4;
        if(*pd) {
          pb=strchr(pd,'/');
          if(pb) *pb++=0; else pb="main";
          setvar("doc",pd); setvar("block",pb); setvar("job","read");
        }
        setvar(ro_name[ro_module],mkfname(NULL,"adm/doc.%s",pl));
      }
      else setvar(ro_name[ro_module],p);
      module_defined=1;
    }
    p=strchr(buf,'_');
    if(p!=NULL && p>buf+3 && myislower(*(p-1)) && myislower(*(p-2))
         && *(p-3)=='/' && myisalpha(*(p+1))) {
      char *pp;
      for(pp=p+1; myisalnum(*pp); pp++){};
      if(*pp=='/') *(p-3)=0;
    }
    p=getenv("HTTPS");
    if(p!=NULL && strcmp(p,"on")==0)
      snprintf(ref_name,sizeof(ref_name)-2,"https://%s%s/%s",s1,buf,cgi_name);
    else
      snprintf(ref_name,sizeof(ref_name)-2,"http://%s%s/%s",s1,buf,cgi_name);
    mystrncpy(ref_base,ref_name,sizeof(ref_base)-2);
    p=strrchr(ref_base,'/');
    if(p!=NULL) *(p+1)=0;
  }
  else mystrncpy(ref_name,cgi_name,sizeof(ref_name)-1);

    // XSS PROTECTION (remove unwanted chars to avoid a possible XSS hack)
    // Search for a first occurence of forbidden chars
  len = strcspn(ref_name, "<>'\"\\");
    // Give only the ref_name before forbidden chars
  ref_name[len]=0;

  setvar("wims_ref_name",ref_name);
}

      /* Read and interpret wims configuration file. */
void main_configure(void)
{
  int i;
  char *conf_buf,*e,*p,*p2,*p3;
  char buf[MAX_LINELEN+1];

  conf_buf=readfile(config_file,NULL,WORKFILE_LIMIT);
  if(conf_buf==NULL) goto fileend;
  e=conf_buf-1; while(e) {
    p=e+1; e=strchr(p,'\n'); if(e) *e=0;
    p=find_word_start(p);
    if(*p==0 || *p==comment_prefix_char) continue; /* empty or comment line */
    p2=strchr(p,'=');
    if(p2==NULL) continue; /* syntax error */
    *p2=0; *find_word_end(p)=0; p2=find_word_start(p2+1);
    p3=p2+strlen(p2); while(myislspace(p3[-1])) p3--;
    while(p3[-1]=='\\') p3--;
    *p3=0;
    i=search_list(main_config,MAIN_CONFIG_NO,sizeof(main_config[0]),p);
    if(i<0) continue; /* name non-defined */
    if(main_config[i].is_integer==1) {
      int *ip;
      ip=main_config[i].address;
      *ip=atoi(p2); if(*ip<0) *ip=0;
    }
    else {
      char **cp;
      cp=main_config[i].address;
      *cp=p2;
    }
  }
  fileend:
  setenv("session_base_dir",session_dir,1);
  determine_ref_name();
  language_list();
  for(i=0;i<MAIN_CONFIG_NO;i++) {
    char **pp;
    if(main_config[i].is_integer==2) {
      pp=main_config[i].address;
      setenv(main_config[i].name,*pp,1);
    }
  }
        /* check priority */
  if(priority[0] && checkhost(priority)>0) ispriority=1;
  (void)getcwd(cwdbuf,sizeof(cwdbuf)); setvar("httpd_PWD",cwdbuf);
  setenv("wims_server_base",cwdbuf,1);
  p=strrchr(cwdbuf,'/');
  if(p!=NULL && strcmp(p,"/public_html")==0) {
    *p=0; snprintf(PATH,sizeof(PATH),"%s/other/bin:%s",cwdbuf,commonpath);
    force_setvar("wims_home",cwdbuf);
    *p='/';
  }
  else {
    snprintf(PATH,sizeof(PATH),"%s/../other/bin:%s",cwdbuf,commonpath);
    force_setvar("wims_home",cwdbuf);
  }
  setenv("PATH",PATH,1);
  p=getvar("httpd_SERVER_SOFTWARE");
  if(p && (strstr(p,"Apache"))!=NULL && strstr(aliased_getfile,"yes")!=NULL)
    good_httpd=1;
  accessfile(buf,"r","../tmp/log/myip");
  *find_word_end(buf)=0;
  if(buf[0]) {
    snprintf(nodeip,sizeof(nodeip),"%s",buf);
    snprintf(temp_log,sizeof(temp_log),"%s.%s",TEMP_LOG_FILE,nodeip);
    force_setvar("wims_priv_nodeip",nodeip);
  }
  else {
    nodeip[0]=0;
    snprintf(temp_log,sizeof(temp_log),"%s",TEMP_LOG_FILE);
  }
}

struct {
  int rname;
  int *rval;
} resource_table[]={
#ifdef RLIMIT_CPU
  {RLIMIT_CPU, &rlimit_cpu},
#endif
#ifdef RLIMIT_FSIZE
  {RLIMIT_FSIZE, &rlimit_fsize},
#endif
#ifdef RLIMIT_AS
  {RLIMIT_AS, &rlimit_as},
#endif
#ifdef RLIMIT_DATA
  {RLIMIT_DATA, &rlimit_data},
#endif
#ifdef RLIMIT_STACK
  {RLIMIT_STACK, &rlimit_stack},
#endif
#ifdef RLIMIT_CORE
  {RLIMIT_CORE, &rlimit_core},
#endif
#ifdef RLIMIT_RSS
  {RLIMIT_RSS, &rlimit_rss},
#endif
#ifdef RLIMIT_NPROC
  {RLIMIT_NPROC, &rlimit_nproc},
#endif
#ifdef RLIMIT_NOFILE
  {RLIMIT_NOFILE, &rlimit_nofile},
#endif
#ifdef RLIMIT_MEMLOCK
  {RLIMIT_MEMLOCK, &rlimit_memlock}
#endif
};
#define RESOURCE_NO (sizeof(resource_table)/sizeof(resource_table[0]))

/* set system resource limits */
void set_rlimits(void)
{
  int i;
  struct rlimit rlim;

  for(i=0;i<RESOURCE_NO;i++) {
    rlim.rlim_cur=rlim.rlim_max=*(resource_table[i].rval);
    setrlimit(resource_table[i].rname,&rlim);
    if(resource_table[i].rname==RLIMIT_CPU) {
      setvar("wims_cpu_limit",int2str(rlim.rlim_max));
      initalarm();
    }
  }
}

struct {
  char *name;
  int is_integer;
  void *default_value;
} module_default[]={
  {"anim_format",0, &DEFAULT_ANIM_FORMAT},
  {"gnuplot_format",0, &gnuplot_format},
  {"ins_anim_limit",1, &ANIM_LIMIT},
  {"ins_density",0, &DEFAULT_INS_DENSITY},
  {"ins_format",0, &DEFAULT_INS_FORMAT},
  {"insplot_font",0, &DEFAULT_INSPLOT_FONT},
  {"wims_busyhours",0, &busyhours},
  {"wims_class_limit",1, &class_limit},
  {"wims_class_quota",1, &class_quota},
  {"wims_class_regpass",0, &class_regpass},
  {"wims_class_user_limit",1, &class_user_limit},
  {"wims_compare_precision",1, &DEFAULT_COMPARE_PRECISION},
  {"wims_devel_modules",0, &devel_modules},
  {"wims_doc_quota",1, &doc_quota},
  {"wims_doc_regpass",0, &doc_regpass},
  {"wims_examlog_limit",1, &examlog_limit},
  {"wims_forum_limit",1, &forum_limit},
  {"wims_bgcolor",0, &bgcolor},
  {"wims_bgimg",0, &bgimg},
  {"wims_css",0, &pagecss},
  {"wims_link_color",0, &link_color},
  {"wims_print_precision", 1, &DEFAULT_PRINT_PRECISION},
  {"wims_ref_bgcolor",0, &ref_bgcolor},
  {"wims_ref_button_bgcolor",0, &ref_button_bgcolor},
  {"wims_ref_button_color",0, &ref_button_color},
  {"wims_ref_button_help_bgcolor",0, &ref_button_help_bgcolor},
  {"wims_ref_button_help_color",0, &ref_button_help_color},
  {"wims_ref_menucolor",0, &ref_menucolor},
  {"wims_show_author",0, &show_author},
  {"wims_show_ip",0, &show_ip},
  {"wims_show_stat",0, &show_stat},
  {"wims_site_description",0, &site_description},
  {"wims_site_keywords",0, &site_keywords},
  {"wims_site_manager",0, &site_manager},
  {"wims_texalign",1, &mathalign_base},
  {"wims_texbasesize",1, &texbasesize},
  {"wims_theme",0, &theme},
  {"wims_theme_icon",0, &theme_icon},
  {"wims_tmp_debug",0, &tmp_debug},
  {"wims_usecookie",0, &usecookie},
  {"wims_user_limit",1, &user_limit},
  {"wims_vlink_color",0, &vlink_color},
  {"wims_hlink_color",0, &hlink_color},
  {"wims_superclass_quota", 1, &superclass_quota},
  {"wims_mail_addrsep", 0, &mail_addrsep},
  {"wims_mail_hidden", 0, &mail_hidden}
};
#define MODULE_DEFAULT_NO (sizeof(module_default)/sizeof(module_default[0]))

/* Set defaults for module, and set corresponding variables. */
void module_configure(void)
{
  int i;
  confset=1;
  for(i=0;i<MODULE_DEFAULT_NO;i++) {
    if(module_default[i].is_integer) {
      int j, *jp;
      jp=module_default[i].default_value;
      j=*jp; if(j<0) j=0;
      setvar(module_default[i].name,int2str(j));
    }
    else {
      char **cp;
      cp=module_default[i].default_value;
      setvar(module_default[i].name,*cp);
    }
  }
  setenv("texgif_fontdir",texgif_fontdir,1);
  setenv("texgif_texheader",texgif_texheader,1);
  setvar("wims_texsize","0");
  setvar("disable_zoom","0");
  setvar("disable_mathml","0");
  setvar("force_mathml","0");
  /* if 'force_mathml=yes' , a check should be done on the value of 'disable_mathml'.
     so it's not fool-proof.
  */
  confset=0;
}

char *modindex[]={
  "address",
  "author",
  "category",
  "copyright",
  "data",
  "deprecated",
  "description",
  "description_ca",
  "description_cn",
  "description_de",
  "description_en",
  "description_es",
  "description_fr",
  "description_it",
  "description_nl",
  "description_si",
  "description_tw",
  "domain",
  "help",
  "keywords",
  "keywords_ca",
  "keywords_cn",
  "keywords_de",
  "keywords_en",
  "keywords_es",
  "keywords_fr",
  "keywords_it",
  "keywords_nl",
  "keywords_si",
  "keywords_tw",
  "language",
  "level",
  "maintainer",
  "maintainer_address",
  "mode",
  "require",
  "scoring",
  "title",
  "title_ca",
  "title_cn",
  "title_de",
  "title_en",
  "title_es",
  "title_fr",
  "title_it",
  "title_nl",
  "title_si",
  "title_tw",
  "translation_language",
  "translator",
  "translator_address",
  "translator_address_ca",
  "translator_address_cn",
  "translator_address_de",
  "translator_address_en",
  "translator_address_es",
  "translator_address_fr",
  "translator_address_it",
  "translator_address_nl",
  "translator_address_si",
  "translator_address_tw",
  "translator_ca",
  "translator_cn",
  "translator_de",
  "translator_en",
  "translator_es",
  "translator_fr",
  "translator_it",
  "translator_nl",
  "translator_si",
  "translator_tw",
  "vardef",
  "version",
  "wims_version",
};
int MODINDEX_NO=(sizeof(modindex)/sizeof(modindex[0]));

char *module_special_file[]={
  "intro","help"
};
#define MODSPEC_NO (sizeof(module_special_file)/sizeof(module_special_file[0]))

/* read and treat module's INDEX file */
void module_index(void)
{
  char buf[MAX_NAMELEN+1], ind_buf[MAX_LINELEN+1];
  char *e, *p, *p2, *ft;
  double v1, v2;
  int i;
  long indf_len;

  memmove(var_def_file,default_var_def,strlen(default_var_def)+1);
  ft=readfile(mkfname(NULL,"%s/INDEX",module_prefix),
          ind_buf,sizeof(ind_buf));
  if(ft==NULL) ft=readfile(mkfname(NULL,"%s/index",module_prefix),
                     ind_buf,sizeof(ind_buf));
  if(ft==NULL) return;
  e=ind_buf-1; indf_len=strlen(ind_buf);
  for( ; e<ind_buf+indf_len && e!=NULL;){
    p=e+1; e=strchr(p,'\n'); if(e!=NULL) *e=0;
    strip_trailing_spaces(p); p=find_word_start(p);
    if(*p==0 || *p==comment_prefix_char) continue; /* empty or comment line */
    p2=strchr(p,'=');
    if(p2==NULL) continue; /* syntax error */
    *p2=0; *find_word_end(p)=0; p2=find_word_start(p2+1);
    i=search_list(modindex,MODINDEX_NO,sizeof(modindex[0]),p);
    if(i<0) continue; /* name not in list */
    snprintf(buf,sizeof(buf),"module_%s",p);
    setvar(buf,p2);
  }
  if(mode!=mode_default) {
    char *s;
    s=getvar("module_mode");
    if(s==NULL) {
      nomode: mode=mode_default; force_setvar("wims_mode","");
    }
    else switch(mode) {
      case mode_popup: {
        if(strstr(s,"popup")==NULL) goto nomode;
        else break;
      }
      case mode_raw: {
        if(strstr(s,"raw")==NULL) goto nomode;
        else break;
      }
      default: break;
    }
  }
/* test for existence of some special files in module's directory */
  for(i=0;i<MODSPEC_NO;i++) {
    if(ftest(mkfname(NULL,"%s/%s.phtml",module_prefix,module_special_file[i]))
      ==is_file) {
      snprintf(buf,sizeof(buf),"module_has_%s",module_special_file[i]);
      setvar(buf,"yes");
    }
  }
  p=getvar("module_language"); if(p==NULL || *p==0) setvar("module_language","en");
  setvar("module_has_about","yes"); /* now we have default about.phtml */
/* Set var_def */
  p=getvar("module_vardef");
  if(p!=NULL && *p!=0) mystrncpy(var_def_file,p,sizeof(var_def_file));
  p=getvar("module_wims_version"); if(p!=NULL && *p!=0) {
    v1=atof(p); v2=atof(wims_version);
    if(isfinite(v1) && isfinite(v2) &&
        (v1>v2 || (v1==v2 && strcmp(find_word_start(p),wims_version)>0))) {
      setvar("module_wims_version",p);
      setvar("wims_version",wims_version);
      module_error("antidated_version");
    }
  }
  if(isdevelmodule) {
    p=getvar("module_scoring");
    if(p==NULL || strcasecmp(p,"yes")!=0) isdevelmodule=0;
  }
}

/* Set up a unique job identity as a 4-bytes integer */
unsigned long create_job_ident(void)
{
/* unsigned long it; // 64-bit compatibility: take away _long_ */
  unsigned long it;
  int i;
/* Is this enough to guarantee uniqueness? */

  it=(nowtime<<16)+(getpid()&0xffff);
/* The encryption is very simple. */
  it=it^0x5a3c9671;
  for(i=0;i<MAX_SESRANDOM;i++) sesrandomtab[i]=random();
    /* ensure that the result will be a 4-bytes integer gives random problem on 32 bits
    return it;*/
  return it & 0xffffffff;
}

/* Setup a job identifier */
void set_job_ident(void)
{
  unsigned long l,r;

  l=create_job_ident();
  /* the last 4 bits always make 0xA. A bug of glibc random()? */
  r=random()>>4;
  snprintf(job_identifier,sizeof(job_identifier),"%lX%08lX",r,l);
  var_noexport=1;
  setvar("job_identifier",job_identifier);
  var_noexport=0;
}

/* define the variable $wims_html_header */
void define_html_header(void)
{
  char *sp, *cp, *ladirection, *mp, *s2, *wversion;
  char buf[MAX_LINELEN+1];
  int buf_length = 0;
  int noc;
  noc=0;
  cp=getvar("wims_expire"); if(cp!=NULL) goto css;
  if(!robot_access && cmd_type==cmd_intro && isclassmodule) {
    sp=getvar("special_parm"); if(sp==NULL) sp="";
    if(strcmp(sp,".nocache.")==0) {
      force_setvar("special_parm",""); noc=1;
    }
    if(!noc) {
      mp=getvar(ro_name[ro_module]);
      if(mp!=NULL && strncmp(mp,"devel/",strlen("devel/"))==0) noc=1;
    }
  }
  if(noc==1){
    _output_("Cache-Control: no-cache, no-store, must-revalidate\r\n");
  }
  css:
  setvar("wims_CSS","");
  /* wims_css peut avoir pour valeur "---" ou "-theme-" au niveau du site
    + la valeur "class" si une classe décide d'utiliser son propre css à la place du theme
  */
  cp=getvar("wims_css");
  ladirection=getvar("wims_main_dirn");
  if (strcmp(ladirection,"ltr")==0) ladirection="";

  if(!robot_access && cp!=NULL && *cp!=0 && strstr(cp,"---")==NULL) {
    char *nbuf;
    cp=find_word_start(cp);

    if(strchr(cp,'/')==NULL) {
      char *pc, *th, *ti; int class_style=0;
      pc=getvar("wims_class");
      wversion=getvar("wims_version");
      if(wversion==NULL || *wversion==0) wversion="0.0";

      /* When a class choose its own css instead of theme css */
      if(pc!=NULL && *pc!=0 && strcmp(cp,"class")==0) {
        nbuf=mkfname(NULL,"%s/%s/css",class_base,pc);
        th=getvar("class_theme");ti=getvar("class_theme_icon") ;
        if(th==NULL || *th==0) th=getvar("wims_theme");
        if(ti==NULL || *ti==0) ti=getvar("wims_theme_icon");

        // Check content of "nbuf" file. (if file is too heavy (>45Ko), it will raise a user_error)
        if( readfile(nbuf,tmplbuf,sizeof(tmplbuf)) ){
          // file exist
          //snprintf(buf,sizeof(buf),"<style>%s</style>",tmplbuf);
          // The content will be added by scripts/adm/css.phtml
          class_style = 1;
        }
      }

      if (!class_style) {
        th=getvar("wims_theme");
        /* when wims_css is everything but '---' */
        if(strcmp(cp,"---")!=0 && strchr(th,'.')==NULL) {
          if(th==NULL || *th==0) th="standard";
          nbuf=mkfname(NULL,"html/themes/%s/css.css",th);

          // Check if theme css file exists (use access from unistd.h)
          if( access( nbuf, F_OK ) != -1 ) {
            buf_length = snprintf(buf,sizeof(buf),"<link href=\"%s?ver=%s\" rel=\"stylesheet\">", nbuf, wversion);
          }
        }
      }
      /* icon theme */
      ti=getvar("wims_theme_icon");
      if(ti==NULL || *ti==0) ti="default";
      snprintf(buf+buf_length,sizeof(buf)-buf_length,"<link href=\"gifs/themes/%s/icon-min.css?ver=%s\" rel=\"stylesheet\">", ti, wversion);

      setvar("wims_CSS",buf);

      /* case of theme */
      if (th) {
        s2=getvar("module_title");
        if(s2!=NULL && *s2!=0) {
          char nbuf[MAX_LINELEN+1];
          mystrncpy(nbuf,s2,sizeof(nbuf)); calc_detag(nbuf); setvar("module_title2",nbuf);
        };
        nbuf=mkfname(NULL,"html/themes/%s/htmlheader.phtml",th) ;
        if (readfile(nbuf,tmplbuf,sizeof(tmplbuf))) html_header=nbuf ;
      }
    }
  }
  if(readfile(html_header,buf,sizeof(buf)))
  setvar("wims_html_header",buf);
}

/* Set Session Dir */
void setsesdir(char *d)
{
  char buf[MAX_FNAME+1];
  if(strstr(d,parent_dir_string)!=NULL) {
    force_setvar("wims_session",robot_session);
    mystrncpy(buf,robot_session,sizeof(buf));
    mkfname(session_dir,"../tmp");
    mkfname(s2_dir,"../tmp");
  }
  else {
    force_setvar("wims_session",d);
    snprintf(buf,sizeof(buf),"%s/%s",SESSION_BASE,d);
    mkfname(session_dir,"../%s",SESSION_BASE);
/* mkfname(s2_dir,"../%s",S2_BASE); */
  }
  force_setvar("wims_sesdir",buf);
}
