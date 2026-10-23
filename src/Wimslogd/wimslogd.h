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

/* student score management, definitions */

#include <utime.h>
#include <sys/socket.h>
#include <sys/un.h>
#include "../Lib/libwims.h"

/* how many seconds in a minute? */
#define MINLENGTH 47
/* maximal running minutes, in order to avoid leaking */
#define MAX_MIN 300
/* queue of socket requests */
#define SOCKET_QUEUE 32
/* socket buffer length */
#define BUFFERLEN (MAX_LINELEN+sizeof(scoreresult)*MAX_CLASSEXOS)
/* number of log lines */
#define MAX_LOGLINES (102400)
/* cuttime buffer, for all exams in a class */
#define CTBUFLEN 4096
/* text buffer length in a sheet cache */
#define SHEETBUFLEN 8192
/* number of cached classes */
/* Storage requirement: about MAX_CLASSCACHE*(MAX_CLASSEXOS*10+CTBUFLEN) bytes. */
#define MAX_CLASSCACHE 20
/* number of cached sheets */
/* Storage requirement: about MAX_SHEETCACHE*SHEETBUFLEN bytes. */
#define MAX_SHEETCACHE 32
/* Refreshment rate for class caches, in seconds */
#define CLASSCACHE_DELAY 100
/* Refreshment rate for sheet caches, in seconds */
#define SHEETCACHE_DELAY 50
/* Maximal debug file length. Debug will be activated once the debug file exists. */
#define MAX_DEBUGLENGTH 1000000
/* delay before ins files are erased. In seconds */
#define INS_DELAY 500

#define evalue strevalue
#define tmpd "tmp/log"
#define classd "log/classes"
#define sesd "sessions"
#define modd "public_html/modules"
#define logd "log"
#define pidfile "tmp/log/wimslogd.pid"
#define sockfile "tmp/log/.wimslogd"
#define debugfile "tmp/log/wimslogd.debug"

/* from cache.c */
typedef struct exodata {
    int active;
    float weight, require;
} exodata; /* General information of an exercise. Size: 10 bytes. */

typedef struct sheetdata {
    unsigned short int start, indstart, exocnt, techcnt, techval, techoffset;
} sheetdata; /* General information of a sheet. Size: xx bytes. */

extern struct classdata {
    char name[MAX_CLASSLEN+1];
    char sclass[MAX_CLASSLEN+1];
    char techs[MAX_LINELEN];
    time_t start, last, modif;
    int sheetcnt, exocnt, examcnt, examstart, access;
    struct exodata exos[MAX_CLASSEXOS];
    char ctbuf[CTBUFLEN];
    short int ctptr[MAX_EXOS];
    struct sheetdata exam, sheets[MAX_SHEETS+1];
} classdata[MAX_CLASSCACHE];
int search_data(void *list, int items, size_t item_size, unsigned short int t);
struct classdata *getclasscache(char *cl);
extern char opt_class[MAX_CLASSLEN+1];
extern char **environ;
extern int classcaches, sheetcaches;
void cleancache(void);

/* from cleaning.c */
void cleaning(int withmain); /* Clean obsolete session directories. */

/* from cmd.c */
extern struct cmdlist {
    char *name;
    void (*routine) (char *p);
} cmdlist[];
extern int cmdcnt;

/* from homedir.c */
void homedir(void);

/* from housekeep.c */
void backup(void);
void housekeep(void);
void modupdate(void); /* module update */

/* from options.c */
extern int options(void);
extern char opt_user[MAX_FNAME+1];
void cmd(void);

/* from wimslogdscore.c */
void cmd_getscore(char *p);
void cmd_scorelog(char *p);

/* from socket.c */
void sockerror(int type, char *p,...);
void opensock(void);
void answer(int fh);

/* from wimslogd.c */
extern char cwd[MAX_FNAME+1];
extern int cwdtype;
enum {dir_home, dir_class, dir_session, dir_module};
extern char qbuf[MAX_LINELEN+1]; /* quota buffer */
extern char ipbuf[64];
extern char loadavg[MAX_LINELEN+1]; /* dans wims.h extern char loadavg[64];*/
extern time_t nowtime, lastcleantime;
extern int idle_time, idle_time2, idle_time3, anti_time;
extern char keepdate[32];
extern char nowstr[64];
extern char mupdate[32], backdate[32];
extern int GEN_LOG_LIMIT, MODULE_LOG_LIMIT, OLD_LOG_FILES, LOG_DELETE;

/* from fork.c */
extern int forkcnt;
void addfork(pid_t pid, int type);
void forkman(int kz);
void wait_children(void);
void dispatch_log(void);

/*from files.c */
void readfile(char *fname, char buf[], long int buflen); /* read the content of a file */
/* datafile structure: number of records.
 * tag=1 if direct access
 */
unsigned int datafile_recordnum(char *p);
char *datafile_fnd_record(char *p, int n, char bf[]); /* datafile structure: find record n, starting from 1 */
int ftest(char *fname);
enum{is_file, is_dir, is_exec, is_fifo, is_socket, is_unknown};

/* from wimslogdlines.c */
void wimslogd_error(char *msg);
extern int commsock, answerlen, debugging;
extern char commbuf[BUFFERLEN+1];
#define textbuf (commbuf+sizeof(int))
void debug(char *p,...);
void my_debug(char *p,...);
extern char *textptr;
void wlogdaccessfile(char *content, char *type, char *s,...);
int call_ssh(int wait,char *s,...);
int call_sh(int wait,char *s,...);
extern int exec_wait;
void getdef(char *fname, char *name, char value[]);
