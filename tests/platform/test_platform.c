/* Unit tests of src/Platform (POSIX run; also cross-compiles for Win32). */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include "../../src/Platform/wims_platform.h"

static int fails=0, total=0;
#define T(name, cond) do{ total++; if(cond) printf("PASS %s\n",name); \
  else { fails++; printf("FAIL %s\n",name); } }while(0)

int main(void)
{
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
#else
  (void)c;(void)p;(void)cr;(void)cw;(void)st;(void)av_cat;(void)av_sleep;(void)av_none;(void)out;(void)t0;
  wp_chan_open(&c);
  T("win32: isolation request refused",
    wp_spawn(&c,av_cat,NULL,NULL,NULL,WP_ISO_WRAPPER,"t",&p)==WP_E_NOISOLATION);
  T("win32: drop-priv request refused",
    wp_spawn(&c,av_cat,NULL,NULL,NULL,WP_ISO_DROP_PRIV,"t",&p)==WP_E_NOISOLATION);
  {
    char *av_echo[]={"cmd","/c","echo","hello world",NULL};
    char buf[128]; long k=0, m; int i;
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
#endif
  printf("== %d/%d passed ==\n", total-fails, total);
  return fails?1:0;
}
