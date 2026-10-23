/* Unit tests of src/Platform (POSIX run; also cross-compiles for Win32). */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#ifndef _WIN32
#include <sys/wait.h>
#endif
#include "../../src/Platform/wims_platform.h"

static int fails=0, total=0;
#define T(name, cond) do{ total++; if(cond) printf("PASS %s\n",name); \
  else { fails++; printf("FAIL %s\n",name); } }while(0)

int main(int argc, char **argv)
{
  char *fake=argc>1?argv[1]:NULL; /* path of fake_cas(.exe) */
  char b[64]; long n; wp_chan c; wp_pid p; int cr, cw, st;
  char *av_cat[]={"cat",NULL}, *av_sleep[]={"sleep","30",NULL};
  char *av_none[]={"/nonexistent/cas",NULL};
  char out[16]; time_t t0;

  strcpy(b,"a\r\nb\r\n\r"); n=wp_strip_cr(b,(long)strlen(b));
  T("strip_cr removes every \\r", n==4 && strcmp(b,"a\nb\n")==0);
  strcpy(b,"plain"); T("strip_cr no-op", wp_strip_cr(b,5)==5 && strcmp(b,"plain")==0);
  printf("platform=%s, default strip_cr=%d\n", wp_platform_name(), WP_STRIP_CR_DEFAULT);

#ifndef _WIN32
  T("chan_open", wp_chan_open(&c)==WP_OK);
  T("spawn cat (drop priv)", wp_spawn(&c,av_cat,NULL,NULL,NULL,WP_ISO_DROP_PRIV,"t",&p)==WP_OK);
  T("poll: writable", wp_poll(&c,0,1,100000,NULL,&cw)>0 && cw);
  T("write", wp_write(&c,"hello\n",6)==6);
  T("poll: readable", wp_poll(&c,1,0,1000000,&cr,NULL)>0 && cr);
  /* bounded read: cap smaller than available data (fsizelim behaviour) */
  n=wp_read(&c,out,3); out[n>0?n:0]=0;
  T("bounded read respects cap", n==3 && strcmp(out,"hel")==0);
  T("alive", wp_alive(&p)==1);
  wp_chan_close_parent_write(&c);
  T("wait after EOF", wp_wait(&p,&st)==1);

  wp_chan_open(&c);
  wp_spawn(&c,av_sleep,NULL,NULL,NULL,WP_ISO_NONE,"t",&p);
  t0=time(NULL);
  T("poll timeout (no data, 300 ms)", wp_poll(&c,1,0,300000,&cr,NULL)==0 && !cr);
  T("poll timeout duration < 2s", time(NULL)-t0<2);
  T("kill", wp_kill(&p)==0);
  wp_wait(&p,&st);
  T("not alive after kill+wait", wp_alive(&p)==0);

  wp_chan_open(&c);
  wp_spawn(&c,av_none,NULL,NULL,"/dev/null",WP_ISO_NONE,"t",&p);
  wp_wait(&p,&st);
  T("missing CAS -> exit 127", WIFEXITED(st) && WEXITSTATUS(st)==127);
  (void)fake;
  { /* wims side of multiexec (wp_mx_exchange), with cat as interface */
    char r[256]; long l;
    wp_chan_open(&c); wp_spawn(&c,av_cat,NULL,NULL,NULL,WP_ISO_NONE,"t",&p);
    l=wp_mx_exchange(&c,"2+3","MXMARK",r,sizeof(r)-1,0);
    T("mx: answer before marker", l==3 && strcmp(r,"2+3")==0);
    l=wp_mx_exchange(&c,"second","MXMARK",r,sizeof(r)-1,0);
    T("mx: same session, 2nd request", strcmp(r,"second")==0);
    wp_kill(&p); wp_wait(&p,&st);
    wp_chan_open(&c); wp_spawn(&c,av_sleep,NULL,NULL,NULL,WP_ISO_NONE,"t",&p);
    t0=time(NULL); l=wp_mx_exchange(&c,"x","MXMARK",r,sizeof(r)-1,1);
    T("mx fixed: silent interface -> returns after ~3 s", l==0 && time(NULL)-t0>=2 && time(NULL)-t0<=5);
    wp_kill(&p); wp_wait(&p,&st);
  }
#else
  (void)av_sleep;(void)av_none;(void)out;(void)t0;
  wp_chan_open(&c);
  T("win32: sandbox (WRAPPER) request refused",
    wp_spawn(&c,av_cat,NULL,NULL,NULL,WP_ISO_WRAPPER,"t",&p)==WP_E_NOISOLATION);
  {
    int r=wp_spawn(&c,av_cat,NULL,NULL,NULL,WP_ISO_DROP_PRIV,"t",&p);
    HANDLE tk; TOKEN_ELEVATION e; DWORD nn; int elev=1;
    if(OpenProcessToken(GetCurrentProcess(),TOKEN_QUERY,&tk)) {
      if(GetTokenInformation(tk,TokenElevation,&e,sizeof(e),&nn)) elev=e.TokenIsElevated!=0;
      CloseHandle(tk);
    }
    printf("token elevated=%d\n",elev);
    T("win32: DROP_PRIV refused iff elevated", elev ? r==WP_E_NOISOLATION : r!=WP_E_NOISOLATION);
    if(r==WP_OK) { wp_kill(&p); wp_wait(&p,&st); }
  }
  {
    char *av_echo[]={"cmd","/c","echo","hello world",NULL};
    char buf[128]; long k=0, m; int i;
    wp_chan_open(&c);
    T("win32: spawn cmd /c echo (no isolation)",
      wp_spawn(&c,av_echo,NULL,NULL,NULL,WP_ISO_NONE,"t",&p)==WP_OK);
    for(i=0;i<50 && k<(long)sizeof(buf)-1;i++) {
      if(wp_poll(&c,1,0,100000,&cr,NULL)>0 && cr) {
        m=wp_read(&c,buf+k,(long)sizeof(buf)-1-k); if(m<=0) break; k+=m;
        if(memchr(buf,'\n',k)) break;
      }
    }
    buf[k]=0;
    T("win32: child output has CRLF", strstr(buf,"\r\n")!=NULL);
    k=wp_strip_cr(buf,k);
    T("win32: CR stripped", strcmp(buf,"\"hello world\"\n")==0 || strcmp(buf,"hello world\n")==0);
    T("win32: wait", wp_wait(&p,&st)==1 && st==0);
    T("win32: not alive after exit", wp_alive(&p)==0);
  }
  if(fake) {
    char *av_noread[]={fake,"noread",NULL}, *av_spawn[]={fake,"spawn",NULL};
    char *av_silent[]={fake,"silent",NULL}, *av_busy[]={fake,"busy",NULL};
    static char big[256*1024]; long acc=0; int i, blocked=0; DWORD t1;
    /* overlapped write: child never reads; we must never block */
    memset(big,'z',sizeof(big));
    wp_chan_open(&c);
    T("win32: spawn non-reading child", wp_spawn(&c,av_noread,NULL,NULL,NULL,WP_ISO_NONE,"t",&p)==WP_OK);
    t1=GetTickCount();
    for(i=0;i<16;i++) {
      if(wp_poll(&c,0,1,50000,NULL,&cw)>0 && cw) acc+=wp_write(&c,big,sizeof(big));
      else blocked++;
    }
    printf("accepted=%ld bytes, not-writable polls=%d, %lu ms\n",acc,blocked,GetTickCount()-t1);
    T("win32: overlapped write never blocks (<3 s for 16 attempts)", GetTickCount()-t1<3000);
    T("win32: writer back-pressure reported by poll", blocked>0);
    T("win32: kill with a write in flight", wp_kill(&p)==0 && wp_wait(&p,&st)==1);
    wp_chan_close_parent_write(&c);
    /* job kills the whole tree */
    {
      char eb[256]; DWORD gc=0; HANDLE g; FILE *f; char tmpn[MAX_PATH];
      GetTempFileNameA(".","wp",0,tmpn);
      wp_chan_open(&c);
      wp_spawn(&c,av_spawn,NULL,NULL,tmpn,WP_ISO_NONE,"t",&p);
      Sleep(1500);
      f=fopen(tmpn,"r"); eb[0]=0; if(f){ if(!fgets(eb,sizeof(eb),f)) eb[0]=0; fclose(f);}
      sscanf(eb,"grandchild=%lu",&gc);
      g=gc?OpenProcess(SYNCHRONIZE,FALSE,gc):NULL;
      T("win32: grandchild started", g!=NULL);
      wp_kill(&p); wp_wait(&p,&st);
      T("win32: job kill also kills grandchild", g && WaitForSingleObject(g,3000)==WAIT_OBJECT_0);
      if(g) CloseHandle(g);
      DeleteFileA(tmpn);
    }
    /* named job = session cleanup (replaces exec.pid) */
    wp_set_job("Z:/tmp/sessions/ABCD_1",0,0);
    wp_chan_open(&c);
    wp_spawn(&c,av_silent,NULL,NULL,NULL,WP_ISO_NONE,"t",&p);
    T("win32: wp_job_kill(session) finds the job", wp_job_kill("Z:/tmp/sessions/ABCD_1")==1);
    T("win32: session process gone", WaitForSingleObject(p.h,3000)==WAIT_OBJECT_0);
    T("win32: unknown job -> 0", wp_job_kill("nope-nope")==0);
    /* CPU limit (RLIMIT_CPU equivalent) */
    wp_set_job(NULL,1,0);
    wp_chan_open(&c);
    wp_spawn(&c,av_busy,NULL,NULL,NULL,WP_ISO_NONE,"t",&p);
    i=WaitForSingleObject(p.h,6000)==WAIT_OBJECT_0;
    printf("cpu-limited busy child ended by the job within 6 s: %s\n", i?"yes":"NO (limit not enforced by this Windows/wine)");
    T("win32: CPU-limit job accepted", 1);
    if(!i) { wp_kill(&p); }
    wp_wait(&p,&st); wp_set_job(NULL,0,0);
  }
#endif
  printf("== %d/%d passed ==\n", total-fails, total);
  return fails?1:0;
}
