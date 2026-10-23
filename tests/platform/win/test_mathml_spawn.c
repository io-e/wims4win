/* Smoke: spawn wims_mathml.exe the same way mathml.c Win32 does. */
#include <stdio.h>
#include <string.h>
#include "../../../src/Platform/wims_platform.h"
int main(void){
  wp_chan c; wp_pid pid; char buf[8192]; long n,tot=0; int st=0;
  char *av[]={"wims_mathml.exe","--use-zoom","0","--tex-size","100%",
              "--max-mml-size","8000","--tex-string","x^2+y^2",NULL};
  if(wp_chan_open(&c)!=WP_OK){ puts("FAIL chan"); return 1; }
  if(wp_spawn(&c,av,"NUL",NULL,"NUL",WP_ISO_NONE,"wims_mathml",&pid)!=WP_OK){
    puts("FAIL spawn"); return 1;
  }
  CloseHandle(c.out_w); c.out_w=INVALID_HANDLE_VALUE;
  wp_chan_close_parent_write(&c);
  CloseHandle(c.in_r);
  memset(buf,0,sizeof(buf));
  while((n=wp_read(&c,buf+tot,(long)sizeof(buf)-1-tot))>0){ tot+=n; buf[tot]=0; }
  wp_strip_cr(buf,tot);
  CloseHandle(c.out_r); if(c.wov.hEvent) CloseHandle(c.wov.hEvent);
  wp_wait(&pid,&st);
  printf("out=%s\nstatus=%d\n",buf,st);
  if(strstr(buf,"MathML")||strstr(buf,"math")){ puts("PASS mathml spawn"); return 0; }
  puts("FAIL no MathML in output"); return 1;
}
