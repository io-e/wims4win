/* wimslogd, native Windows part (mingw-w64). GPL v2 or later.
 *
 * The upstream daemon (cache, cleaning, cmd, files, homedir, housekeep, log,
 * options, wimslogdlines, wimslogdscore and the globals of wimslogd.c) is
 * compiled unchanged; this file replaces what needs fork() or AF_UNIX:
 *   - main loop: named pipe (wp_ipc, one pipe per runtime tree) instead of
 *     select()/accept() on tmp/log/.wimslogd;
 *   - answer()/output()/sockerror()/sockok() of socket.c;
 *   - fork.c: the fork list holds process handles (wp_pid, Job Object);
 *   - fork() of cleaning(), dispatch_log(), logexec(): the daemon re-executes
 *     itself, "wimslogd.exe --task <name> <arg>", the child runs the same
 *     function body and exits (wl_task=1), as the forked child does on Linux.
 * /proc/loadavg -> wp_loadavg(). */
#include "../../Platform/wp_ipc.h"  /* before wimslogd.h: its "pidfile" macro */
#include "../wimslogd.h"
#include <direct.h>
#include <fcntl.h>
#include <io.h>

extern char pidstr[32];
extern time_t starttime, thismin, lastmin, startmin;
extern int nowday, nowmin, nowhr, startdate, backup_hour, modupdatetime, rshift;
extern int idle_time;
extern char nodeipbuf[];
void getnow(void);
void parms(void);
void local(void);
int verify_tables(void);

int wl_task=0;
static char selfexe[MAX_PATH];

/* ---- fork.c ---------------------------------------------------------- */
#define MAX_FORK 1024
#define MAX_DELAY 1500
static struct { wp_pid p; time_t t; int type; } forklist[MAX_FORK];
int forkcnt;

void addfork_w(wp_pid *p, int type)
{
  if(forkcnt>=MAX_FORK) {
    wp_kill(p); CloseHandle(p->h); if(p->job) CloseHandle(p->job); return;
  }
  forklist[forkcnt].p=*p; forklist[forkcnt].t=time(NULL);
  forklist[forkcnt].type=type; forkcnt++;
}
void addfork(pid_t pid, int type) { (void)pid; (void)type; } /* unused on Win32 */

void forkman(int kz)
{
  int delay=MAX_DELAY, i; time_t now=time(NULL);
  (void)kz;
  if(forkcnt>=MAX_FORK/2) delay=delay/5;
  if(forkcnt*4>=MAX_FORK*3) delay=delay/4;
  for(i=forkcnt-1; i>=0; i--) {
    if(now-forklist[i].t>delay) wp_kill(&forklist[i].p);
    if(wp_alive(&forklist[i].p)) continue;
    CloseHandle(forklist[i].p.h);
    if(forklist[i].p.job) CloseHandle(forklist[i].p.job);
    memmove(forklist+i,forklist+i+1,(forkcnt-1-i)*sizeof(forklist[0]));
    forkcnt--;
  }
}

void wait_children(void)
{
  int i; time_t now;
  do {
    forkman(0); now=time(NULL);
    for(i=0; i<forkcnt; i++)
      if(forklist[i].type && forklist[i].t > now-MAX_DELAY) break;
    if(i>=forkcnt) return;
    Sleep(1000);
  } while(1);
}

void wl_spawn_task(const char *task, int arg, int type)
{
  char a[16], ef[64]; char *argv[5]; wp_chan c; wp_pid p;
  snprintf(a,sizeof(a),"%d",arg);
  argv[0]=selfexe; argv[1]="--task"; argv[2]=(char*)task; argv[3]=a; argv[4]=NULL;
  memset(&c,0,sizeof(c)); fflush(NULL);
  wp_set_job(NULL,MAX_DELAY,0);
  /* stderr per task; NUL if the same task still holds its file open */
  snprintf(ef,sizeof(ef),"tmp/log/wimslogd.%s.err",task);
  if(wp_spawn(&c,argv,"NUL","NUL",ef,WP_ISO_NONE,NULL,&p)!=WP_OK &&
     wp_spawn(&c,argv,"NUL","NUL","NUL",WP_ISO_NONE,NULL,&p)!=WP_OK) {
    wimslogd_error("wl_spawn_task: CreateProcess failed."); return;
  }
  addfork_w(&p,type);
}

/* ---- socket.c -------------------------------------------------------- */
void errorquit(char *p) { wimslogd_error(p); exit(1); }
void opensock(void) {}

static void output_w(wp_ipc_fd fh)
{
  int l, *ip;
  if(answerlen<0) l=strlen(textbuf); else l=answerlen;
  ip=(int *) commbuf; *ip=l;
  (void)wp_ipc_write_msg(fh,commbuf,l+(int)sizeof(int));
  wp_ipc_close(fh);
  if(debugging) {
    if(textbuf[0]=='O') debug("%.2s %d bytes.",textbuf,l);
    else debug("%s",textbuf);
  }
}

void sockerror(int type, char *p,...)
{
  char *estr, buf[MAX_LINELEN+1];
  va_list vp;
  va_start(vp,p); vsnprintf(buf,sizeof(buf),p,vp); va_end(vp);
  if(errno==0) estr=""; else estr=strerror(errno);
  snprintf(textbuf,BUFFERLEN-16,"ERROR %d\n%s\n%s",type,buf,estr);
}

void sockok(char *p) { snprintf(textbuf,BUFFERLEN-16,"OK\n%s",p); }

static void answer_w(wp_ipc_fd fh)
{
  int t, l;
  t=wp_ipc_read_msg(fh,commbuf,BUFFERLEN-(int)sizeof(int));
  l=*(int *)commbuf;
  if(t<(int)sizeof(int) || l<=0 || l>=BUFFERLEN-(int)sizeof(int)) {
    sockerror(3,"Daemon socket read error."); goto end;
  }
  textbuf[l]=0; textptr=textbuf;
  answerlen=-1;
  if(debugging) debug("> %s",textbuf);
  if(options()>=0) cmd();
  end: (void)chdir(cwd); cwdtype=dir_home; output_w(fh);
}

void answer(int fh) { (void)fh; }

/* ---- main ------------------------------------------------------------ */
static void to_root(void)
{
  char *p; size_t l;
  if(getcwd(cwd,sizeof(cwd))==NULL) { fprintf(stderr,"wimslogd: getcwd() failure.\n"); exit(1); }
  for(p=cwd; *p; p++) if(*p=='\\') *p='/';
  l=strlen(cwd);
  if(l>=12 && strcmp(cwd+l-12,"/public_html")==0) {
    cwd[l-12]=0;
    if(chdir(cwd)<0) { fprintf(stderr,"wimslogd: Unable to change directory.\n"); exit(1); }
  }
}

static int run_task(char *task, int arg)
{
  wl_task=1;
  getnow();
  wlogdaccessfile(qbuf,"r","log/cquota/lim.host");
  if(strcmp(task,"cleaning")==0) cleaning(arg);
  else if(strcmp(task,"dispatch_log")==0) dispatch_log();
  else if(strcmp(task,"logexec")==0) logexec();
  return 0;
}

int main(int argc, char *argv[])
{
  wp_ipc_server srv; struct stat st; FILE *f; char buf[MAX_LINELEN+1];
  DWORD l;
  _fmode=_O_BINARY; _setmode(0,_O_BINARY); _setmode(1,_O_BINARY); _setmode(2,_O_BINARY);
  l=GetModuleFileNameA(NULL,selfexe,sizeof(selfexe));
  if(l==0 || l>=sizeof(selfexe)) snprintf(selfexe,sizeof(selfexe),"%s",argv[0]);
  forkcnt=0; exec_wait=1; classcaches=0;
  init_random();
  parms();
  to_root();
  if(argc>=3 && strcmp(argv[1],"--task")==0) return run_task(argv[2],argc>3?atoi(argv[3]):0);
  if(argc>=2 && strcmp(argv[1],"--check")==0) return verify_tables()?1:0;
  _mkdir("tmp"); _mkdir("tmp/log");
  (void)freopen("NUL","r",stdin);
  (void)freopen("tmp/log/wimslogd.out","w",stdout);
  (void)freopen("tmp/log/wimslogd.err","w",stderr);
  verify_tables();
  modupdatetime=(double) random()*350/RAND_MAX;
  rshift=(double) random()*MINLENGTH/RAND_MAX;
  snprintf(pidstr,sizeof(pidstr),"%lu",(unsigned long)GetCurrentProcessId());
  f=fopen(pidfile,"w"); if(f) { fputs(pidstr,f); fclose(f); }
  if(wp_ipc_listen(&srv,sockfile)!=0) {
    fprintf(stderr,"wimslogd: listen failed (%lu)\n",GetLastError()); return 1;
  }
  getnow(); printf("wimslogd %s started at %s (win32).\n",pidstr,nowstr);
  startdate=nowday; fflush(NULL);
  starttime=nowtime; startmin=lastmin=thismin;
  wlogdaccessfile(qbuf,"r","log/cquota/lim.host");
  wlogdaccessfile(buf,"r","log/myip"); mystrncpy(ipbuf,find_word_start(buf),sizeof(ipbuf));
  if(ipbuf[0]==0) snprintf(ipbuf,sizeof(ipbuf),"127.0.0.1");
  do {
    int n;
    if(stat(debugfile,&st)==0 && st.st_size<MAX_DEBUGLENGTH) debugging=1;
    else debugging=0;
    wp_loadavg(loadavg,MAX_LINELEN);
    for(n=0; n<100; n++) {
      wp_ipc_fd c=wp_ipc_accept(&srv,50000);
      if(c==WP_IPC_INVALID) { forkman(0); continue; }
      answer_w(c);
    }
    forkman(1);
    getnow();
    if(thismin==lastmin) continue;
    if(nowday!=startdate) { wait_children(); return 0; } /* daily refreshment */
    lastmin=thismin;
    wlogdaccessfile(buf,"r",pidfile); strip_trailing_spaces(buf);
    if(strcmp(buf,pidstr)!=0) { wait_children(); return 0; } /* wrong pid */
    wlogdaccessfile(qbuf,"r","log/cquota/lim.host");
    wlogdaccessfile(buf,"r","log/myip"); mystrncpy(ipbuf,find_word_start(buf),sizeof(ipbuf));
    if(ipbuf[0]==0) snprintf(ipbuf,sizeof(ipbuf),"127.0.0.1");
    cleancache();
    if((thismin%127)==6) homedir();
    wlogdaccessfile(buf,"r","tmp/log/wimslogd.relax");
    if(strstr(buf,"yes")==NULL) {
      dispatch_log();
      if((thismin%2)==1) local();
      cleaning(1);
      if((thismin%5)==0 && nowmin>15) housekeep();
      if(nowhr*60+nowmin>modupdatetime && (thismin%17)==11) modupdate();
      if(backup_hour>0 && backup_hour<23 && (thismin%17)==3 && nowhr>=backup_hour)
        backup();
      fflush(NULL);
      logexec();
    }
    else if((thismin%9)==0) cleaning(0);
  } while(1);
  return 0;
}
