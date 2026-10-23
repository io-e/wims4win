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

/* WWW multipurpose server, header definitions */

#include "Lib/libwims.h"
#include "config.h"
#include "includes.h"
#include "wimsdef.h"

#define evalue strevalue

/* limit of query_string. */
#define QUERY_STRING_LIMIT 204799
/* maximal length of deposited file */
#define MAX_DEPOSITLEN (8*1024*1024)
/* maximal number of registered variables */
#define MAX_VAR_NUM 512
/* length limit for public_html/bin executable names */
#define MAX_EXEC_NAME 31
/* max level of nested fors */
#define MAX_FOR_LEVEL 12
/* max nested read; to avoid infinite loop */
#define MAX_READNEST 20
/* number of cached working files */
#define MAX_MCACHE 32
/* max sort items. */
#define MAX_SORT_ITEM 5000
/* max randperm() items. Should be compatible to MAX_LINELEN. */
#define MAX_RANDPERM 2000
/* Number of session random numbers */
#define MAX_SESRANDOM 64
/* Limit of output length. This can be configured. */
#define OUTPUT_LENGTH_LIMIT (1024*1024*8)
/* limit of items in value lists */
#define MAX_VALUE_LIST 2048
/* We fix a limit for the number of goto's (or for jumps) executed,
 * in order to avoid infinite loop or DOS attack.
 */
#define GOTO_LIMIT 500000
/* maximal nesting level of substitutions. */
#define SUBST_LIMIT 16
/* Limit for instex, insplot, insPLOT. */
#define INS_LIMIT 500
/* file to hold last output */
#define lastout "last.html"
/* Access policy configuration file */
#define ACCESS_CONF "../log/wims.conf.access"
/* Should not be changed */
#define SESSION_BASE "sessions"
#define S2_BASE "s2"

/* Prefix char for exec functions */
#define exec_prefix_char '!'
/* Prefix char for calc functions */
#define calc_prefix_char '!'
/* Prefix char for goto labels */
#define label_prefix_char ':'
/* Prefix for tags in data files */
#define tag_prefix_char ':'
/* Prefix char for comment lines */
#define comment_prefix_char '#'
/* prefix to user variable names */
#define var_prefix "w_"
/* (user) prefix for mathematical fonts */
#define mathfont_prefix "m_"
/* prefix for internal variables */
#define wims_prefix "wims_"
#define wpflen strlen(wims_prefix)
/* name of variable definition file */
#define default_var_def "var.def"
/* Not configured */
#define error_data_string "wims_error_data"
/* Name of files containing error messages. */
#define user_error_msg_file "msg/user_error.phtml"
#define module_error_msg_file "msg/module_error.msg"
/* directory containing log files */
#define log_dir "../log"
#define class_base "../log/classes"
/* What should indicate that a file name given by the user
 * would allow him to go upstairs in the directory tree?
 * We shouldn't use "../", otherwise the user may just specify
 * ".." as name and go upstairs one step.
 */
#define parent_dir_string ".."
/* This one is stronger. */
#define PARENT_DIR_STRING "/../"
/* kernel daemon socket file. Should match that of wimslogd. */
#define ksockfile "../tmp/log/.wimslogd"
/* this directory should not exist. Do not change the definition! */
#define sysmask_trigger_dir "sysmask/stat/trigger"

typedef struct {
    int lineno; /* line number */
/*    long fpt; */ /* position in the file */
    char varname[MAX_NAMELEN+1];
    double varval, varend, step;
    char *list_pt, *bufpt;
    int from; /* from ... to ... if 0, list if 1 */
} FOR_STACK;

typedef struct {
    char *address;
    short int execcode, varcode;
    int isstart; /* bit 0: start line; bit 1: exec; bit 2: label. bit 3: hard comment. bit 4: soft comment */
    unsigned int llen;
} LINE_STRUCT;

typedef struct {
    char name[MAX_FNAME+1];
    int  l;
    int linepointer;
    char filepath[MAX_FNAME+1];
/* nocache bits: 0. !nocache; 1. hard nocache; 2. isfromcache; 3. isbase; */
    short int for_idx, nocache;
    int linecnt;
    char *textbuf;
    LINE_STRUCT *lines;
    FOR_STACK *for_stack;
} WORKING_FILE;

/* from auth.c */
extern char loadavg[64];
void check_load(int th);
void auth(void);
void checkrafale(void);
void access_check(int isclass);

/* from calc.c */
void calc_tolower(char *p);
void calc_toupper(char *p);
void calc_lookup(char *p);
void calc_randitem(char *p);
void calc_mexec(char *p);
void _calc_exec(char *p, char *arg0, char *arg1, int n);
void calc_detag(char *p);
void calc_html2iso(char *p);
typedef struct  MYFUNCTION {
    char *name;
    int tag;
    void (*routine) (char *p);
} MYFUNCTION;

extern MYFUNCTION calc_routine[];
extern int CALC_FN_NO;

void calc_append(char *p);
void calc_charof(char *p);
void calc_checkallpar(char *p);
void calc_checkhost(char *p);
void calc_columnof(char *p);
void calc_daemon(char *p);
void calc_date(char *p);
void calc_debug(char *p);
void calc_declosing(char *p);
void calc_defof(char *p);
void calc_embraced(char *p);
void calc_evalsubst(char *p);
void calc_exec(char *p);
void calc_fileexists(char *p);
void calc_hex(char *p);
void calc_imgrename(char *p);
void calc_itemof(char *p);
void calc_leveldata(char *p);
void calc_lineof(char *p);
void calc_listfile(char *p);
void calc_listintersect(char *p);
void calc_listunion(char *p);
void calc_listuniq(char *p);
void calc_mailurl(char *p);
void calc_makelist(char *p);
void calc_mathsubst(char *p);
void calc_module(char *p);
void calc_nonempty(char *p);
void calc_pos(char *p);
void calc_product(char *p);
void calc_recordof(char *p);
void calc_recursion(char *p);
void calc_reinput(char *p);
void calc_replace(char *p);
void calc_rowof(char *p);
void calc_rows2lines(char *p);
void calc_select(char *p);
void calc_solve(char *p);
void calc_sort(char *p);
void calc_sum(char *p);
void calc_translate(char *p);
void calc_trim(char *p);
void calc_wordof(char *p);
void calc_values(char *p);

/* from config.c */
void setsesdir(char *p);
extern int threshold2, threshold1, rafalvl, modlog;
extern char session_dir[MAX_FNAME+1], s2_dir[MAX_FNAME+1];
extern char temp_log[MAX_FNAME+1]; /* temp log file name */
extern char *cgi_name;
extern int idle_time, idle_time2, idle_time3, OLD_LOG_FILES, GEN_LOG_LIMIT, MODULE_LOG_LIMIT;
extern int LOG_DELETE;
extern int backup_hour, site_accounting, examlog_limit;
extern int ANIM_LIMIT;
extern char *header_menu, *insplot_processor;
extern char *home_referer, *home_referer_user, *header_menu_supervisor,
   *header_menu_user, *home_referer_supervisor, *title_page, *html_file;
extern char *aliased_cgi, *insdraw_processor, *mail_opt;
extern int mathalign_base;
unsigned long create_job_ident(void);
void module_index(void);
extern char *module_about_file, *main_var_proc_file, *var_init_file, *special_font;
extern char var_def_file[256], *var_def_buf;
void define_html_header(void);
extern char *usecookie, *bgcolor;
extern int spec_font;
extern char *config_file; /* Main configure file, this name cannot be configured! */
extern char *intro_file; /* Introduction page name */
extern char *ref_file; /* Reference page */
typedef struct CONFIG_DATA { char *name; int is_integer; void  *address; } CONFIG_DATA;
extern CONFIG_DATA main_config[];
extern int MAIN_CONFIG_NO;
extern char *modindex[];
extern int MODINDEX_NO;
void main_configure(void);
void set_rlimits(void);
void module_configure(void);
void set_job_ident(void);

/* from compare.c */
int compare(char *p, int numeric, int lvl);
char *bufprep(char *p);

/* from evalue.c and Lib/evalue.c */
double evalue(char *p);
char *substit(char *p);
enum {for_in, for_from};
struct forstruct {
    char var[MAX_NAMELEN+1];
    int type;
    double list[MAX_VALUE_LIST];
    char *pos[MAX_VALUE_LIST];
    double from, to, step;
};
extern struct forstruct forstruct;

int cutfor(char *p, char *bufp);
void float2str(double d, char *p);

/* from exam.c */
void calc_examdep(char *p);
double currexamscore(int sh);
void check_exam(void);

/* from exec.c */
void exec_main(char *p);
void exec_set(char *name);
void exec_setseed(char *p);
extern MYFUNCTION exec_routine[];
extern int EXEC_FN_NO;
extern struct distr_cmd { char *name; int (*routine) (char *p, char *list[], int max);
  } distr_cmd[];
extern int distr_cmd_no;
void exec_tail(char *p);
void exec_read(char *p);

/* from files.c */
void secure_exec(void);
void fileappend(char *p);
void filewrite(char *p);

/* from html.c */
void exec_formbar(char *p);
void exec_formbar(char *p);
void exec_formcheckbox(char *p);
void exec_formradio(char *p);
void exec_formselect(char *p);

/* from insmmath.c */
void insmath(char *p);
int instex_ready(char *p, char *n);
char *instex_check_static(char *p);
void calc_instexst(char *p);

/* from lines.c */
void accessfile(char *content, char *type, char *s,...);
int checkhostt(char bf2[]);
extern int untrust;
void module_error(char msg[]);
int trusted_module(void);
int checkhost(char *hlist);
void _getdef(char buf[], char *name, char value[]);
void getdef(char *fname, char *name, char value[]);
void _daemoncmd(char *p);
int datafile_check(char *name);
extern int is_class_module;
int find_module_file(char *fname, char buf[], int mode);
int call_ssh(char *s,...);
int _http2env(char outs[], char ins[]);
void slashsubst(char *p);
void internal_error(char msg[]);
char *readfile(char *fname, char buf[], long int buflen); /* mathfonts.c */
int call_sh(char *s,...);
unsigned int datafile_recordnum(char *p);
char *datafile_fnd_record(char *p, int n, char bf[]);
int execredirected(char *cmdf, char *inf, char *outf, char *errf, char *arg[]);
char *mkfname(char buf[], char *s,...);
extern char *trusted_modules;
enum{is_file, is_dir, is_exec, is_fifo, is_socket, is_unknown};
extern struct mxtab {
    char cmd[MAX_EXEC_NAME+1];
    unsigned int fd1, fd2;
    int pipe_stdin[2], pipe_stdout[2], pipe_stderr[2];
} mxtab[MAX_MULTIEXEC];
int ftest(char *fname);
int multiexec(char *cmd, char **abuf);
int wgetline(char buf[], size_t buflen, WORKING_FILE *f);
int open_working_file(WORKING_FILE *f, char *fname);
void close_working_file(WORKING_FILE *f, int cache);
void read_tmp_file(char *p, const char *fname);
void initalarm(void);
extern int var_noexport; /* do not export variable */

enum Name_Modes {mode_default, mode_popup, mode_raw};
extern enum Name_Modes NAME_MODES;

extern char *manager_site;
void flushoutput(void);
extern char pidbuf[32];
extern char lastdatafile[MAX_FNAME+1];
extern char lastftest[MAX_FNAME+1];
int kerneld(char *p, int bufsize);
void _setdef(char buf[], char *name, char *value);
void output0(char buf[]);
void nph_header(int code);
void delete_pid(void);
void setdef(char *fname, char *name);
void instex_flush(void);
extern char *DEFAULT_INS_FORMAT;
void tex_nospace(char *p);
int get_cached_file(char *name);
extern off_t ftest_size;
void putlastout(void);
void bad_ident(void);
void create_pid(void);
extern struct sockaddr_un sun;
extern char *lastdata;
extern char *outptr;
extern char outbuf[8192];
extern int http2env(char outs[], char ins[]);

/* from log.c */
void user_log(char *p);
void write_logs(void);
void mail_log(char *p);
void flushlog(void);
void post_log(void);
void checklogd(void);

/* from mathfonts.c */
void fix_tex_size(void);
extern int current_tex_size, texbasesize;
void _output_(char *s);
void output(char *s,...);
char *mathfont(char *fontname);
extern int usertexsize;

/* from mathml.c */
int mathml(char *p, int option);

/* from matrix.c */
void rawmatrix(char *p);

/* from pedia.c */
void pedia(char *p);

/* from rawmath.c */
void htmlmath(char *p);
void mathmlmath(char *p);
void tohttpquery(char *p);
void rawmath(char *p);
void texmath(char *p);
extern int rawmath_easy;
extern struct mathname { char *name; int style; char *replace; } mathname[];
extern int mathname_no;

/* from hmname.c */
extern struct hmname { char *name; char *replace; char *replacem;} hmname[];
extern int hmname_no;

/* from score.c */
void calc_getscorelast(char *p);
void calc_getscore(char *p);
void calc_getscoremean(char *p);
void calc_getscoreremain(char *p);
void calc_getscorerequire(char *p);
void calc_getscoreweight(char *p);
void calc_getscorelast(char *p);
void calc_getscoretry(char *p);
void calc_getscorealltries(char *p);
void calc_getscoremaxexotry(char *p);
void calc_getscorebest(char *p);
void calc_getscorelevel(char *p);
void calc_getscorepercent(char *p);
void calc_getscorestatus(char *p);
void calc_examscore(char *p);
void calc_getseedlast(char *p);
void calc_getseedscorelast(char *p);
void calc_getseedlastcnt(char *p);
void calc_getseedscores(char *p);
void calc_getsheetstatus(char *p);
void exam_currscore(int esh);
extern double exam_scoredata[MAX_EXAMS];
int exam_depcheck(char *deps, int exam);
int getscorestatus(char *classe, int sheet);
int depcheck(char *sh, int exo, char *deps);
int gettrycheck (char *class, char *user, int sh, int ex);
char* getseedscore (char *class, char *user, int sh, int ex);
extern char rscore_class[MAX_CLASSLEN+1];
extern char rscore_user[MAX_NAMELEN+1];

/* from test.c */
void exec_test(char *p);

/* from texmath.c */
void _replace_matrix ( char *p , char *s_mat1, char *s_mat2);
extern struct tmathfn {
  char *name; int expind; char *left, *right; void (*routine) (char *p);
} tmathfn[];
extern int tmathfn_no;
extern struct tmathvar { char *name, *tex; } tmathvar[];
extern int tmathvar_no;

/* from var.c */
int force_setvar(char *vname,char *vvalue);
char *getvar(char *name);
int setvar(char *vname, char *vval);
void debug_output(void);
void exportall(void);
void unsetvar(char *vname);
void get_var_privileges(void);
char *_getvar(char *vname);
int varsuite(char *stem, int beg, int end, char *pbuf[], int pbuflen);
void lessrafale(void);
extern char *exportvars[]; /* wims_ variables that should be exported */
extern int exportvarcnt;

/* from variables.c */
int var_def_check(char *name);
void var_proc(char *fname,int cache);
void phtml_put(char *fname,int cache);
void phtml_put_base(char *fname,int cache);
void set_module_prefix(void);
extern int ispriority;
extern char *sed_prog, *tr_prog, *tex2gif;
void get_static_session_var(void);
void parse_ro_names(void);
int var_def_name(char *n, int v);
void set_init_parm(void);
int session_exists(char *s);
int set_session_prefix(void);
int check_session(void);
void trap_check(char *s);
void set_protocol(void);
void main_phtml_put(char *mname);
void var_init(void); /* initialisation */
void set_variables(void);
void save_session_vars(void);
void seed_time(void);

/* from wims.c */
void user_error(char msg[]);
extern int robot_access, wrapexec, good_httpd, multiexec_index, outputing;
extern int rlimit_cpu, new_session, hostcquota, manageable, cmd_type, exec_is_module;
extern int available_lang_no, WORKFILE_LIMIT, isclassmodule, mode, module_defined;
extern char lang[16];
extern char *remote_addr;
extern time_t nowtime, limtime, limtimex;
extern char *ro_name[];
extern char *mathalign_sup1, *mathalign_sup2; /* see mathalign_base */
extern char *module_dir;
extern char tmplbuf[MAX_LINELEN+1]; /* for temporary uses not thru subroutines. */
extern char cwdbuf[MAX_FNAME+1]; /* store current working directory */
extern char ins_alt[MAX_LINELEN+1]; /* dynamic insertion alternative text */
extern char *bin_dir; /* directory containing executable scripts and programs. */
extern char *tmp_debug, *tmp_debug_var, *tmp_debug_use_var;
extern char *instex_style;
extern char session_prefix[MAX_FNAME+1], s2_prefix[MAX_FNAME+1], module_prefix[MAX_FNAME+1];
extern char multiexec_random[64];
extern char class_dir[MAX_FNAME+1]; /* directory name of this class */
extern char *devel_modules, *robot_session;
extern int manager_https, confset, rlimit_fsize, rlimit_as, rlimit_data, rlimit_stack, rlimit_core;
extern int rlimit_rss, rlimit_nproc, rlimit_nofile, rlimit_memlock;
extern int sesrandomtab[MAX_SESRANDOM]; /* session random values */
extern char job_identifier[32];
extern int print_precision, substnest, getvar_len;
extern char pre_language[4];
extern FILE * trace_file;
extern int trace_indent;
/* the following list must be in the same order as *ro_name in wims.c */
enum Ro_Names{
    ro_cmd, ro_empty, ro_lang, ro_module, ro_session, ro_special_parm,
      ro_special_parm2, ro_special_parm3, ro_special_parm4, ro_useropts, ro_wsession,
      ro_subsession, ro_win, ro_worksheet
};
extern enum Ro_Names RO_NAMES;
extern char *home_module;
extern char class_dir[MAX_FNAME+1]; /* directory name of this class */
extern char available_lang[MAX_LANGUAGES][4];

enum _commands {
    cmd_intro, cmd_new, cmd_renew, cmd_reply, cmd_config, cmd_hint, cmd_help,
      cmd_resume, cmd_next, cmd_getins, cmd_getframe, cmd_getfile, cmd_close,
      cmd_ref
};
extern enum _commands COMMANDS;
extern char tmp_dir[MAX_FNAME+1]; /* temporary directory */

extern char ref_name[2048], ref_base[2048];

struct user_variable {
    char *name;
    char *value;
};
extern struct user_variable user_variable[MAX_VAR_NUM];

extern int user_var_no;
/* Destinated to module error messages */
extern WORKING_FILE m_file;
extern WORKING_FILE svar_file;
extern WORKING_FILE mcache[MAX_MCACHE];

extern int isexam, direct_datafile;
extern char *unsetvars[], *commands[];
extern char exam_sheetexo[32]; /* sheet data of an exam */
extern char nowstr[32];
extern int CMD_NO;
extern int simuxam, parm_restore, exodepOK, user_error_nolog;
extern char examlogd[MAX_FNAME+1]; /* examlog directory name */
extern char examlogf[MAX_FNAME+1]; /* examlog file name */
extern char freeworklogd[MAX_FNAME+1]; /* freeworklog directory name */
extern char freeworklogf[MAX_FNAME+1]; /* freeworklog file name */
extern char mpboundary[1024];
extern char *stdinbuf;
extern char **environ;      /* table of environment variables */
extern char cookiegot[256], cookieheader[64];
extern int executed_gotos; /* number of goto's executed. to check with GOTO_LIMIT. */
extern int noout, execnt, direct_exec, ismhelp;
extern int readnest; /* nested read count */;
extern char *default_form_method;
extern int insert_no, lastout_file, instex_cnt;
extern char instex_src[MAX_LINELEN+1], instex_fname[MAX_LINELEN+1];
extern char *instex_processor;
extern int instex_usedynamic, httpd_type, mcachecnt, mfilecnt;
extern char *mathalign_sup1, *mathalign_sup2; /* see mathalign_base */
enum {httpd_apache, httpd_wims};
extern int output_length; /* length of the output. */
extern int backslash_insmath, killpid, is_multiexec, exec_wait, getwimstexsize;
extern char *remote_host;
extern char *var_readable, *var_writable, *var_nr, *var_nw, *var_pfx;
extern int RO_NAME_NO, INTERNAL_NAME_NO, INTERNALDEF_NAME_NO;
extern struct internal_name {
    char *name; int stat;
} internal_name[];
extern char var_hacking;
extern long int startmtime2;
extern int setcookie, forceresume, user_lang, form_access, session_serial;
extern char *protocol;
void useropts(void);
extern char cookiegot[256], cookieset[256];
extern struct tm *now; /* time of request */
extern int isdevelmodule, defined_var_total, deplen, wims_sheet, wims_exo;
struct VAR_DEF {
    char *name; short int beg,end;
    char allow, log_num, defined_in_parm, unused_padding;
};
extern struct VAR_DEF var_def[MAX_VAR_NUM];


#if !HAVE_SETENV
int setenv(const char *name, const char *value, int overwrite);
void unsetenv(const char *name);
#endif
