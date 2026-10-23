/* Minimal native-Windows wimslogd: named-pipe IPC, ping + loadavg.
 * Enough for checklogd() / CGI bring-up. Unported cmds return OK empty.
 * GPL v2 or later. */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <direct.h>
#include <windows.h>
#include "../../Platform/wp_ipc.h"

#define BUFFERLEN (45000+32)
static char commbuf[BUFFERLEN];
static char *textbuf = commbuf + sizeof(int);
static char loadavg[256] = "0.00 0.00 0.00 1/1 1";
static char ipbuf[64] = "127.0.0.1";
static char pidstr[32];

static void handle(wp_ipc_fd fd)
{
  int n, l; char *p1, *p2; char cmd[64]; int c;
  n = wp_ipc_read_msg(fd, commbuf, BUFFERLEN);
  if(n < 0){ wp_ipc_close(fd); return; }
  p1 = textbuf; while(*p1==' '||*p1=='\t') p1++;
  p2 = p1; while(*p2 && *p2!=' ' && *p2!='\t' && *p2!='\n') p2++;
  c = (int)(p2 - p1); if(c > (int)sizeof(cmd)-1) c = (int)sizeof(cmd)-1;
  memcpy(cmd, p1, c); cmd[c] = 0;
  while(*p2==' '||*p2=='\t') p2++;
  wp_loadavg(loadavg, sizeof(loadavg));
  if(strcmp(cmd, "ping") == 0)
    snprintf(textbuf, BUFFERLEN - (int)sizeof(int), "OK\n0 %s\n%s", loadavg, ipbuf);
  else if(strcmp(cmd, "test") == 0)
    snprintf(textbuf, BUFFERLEN - (int)sizeof(int), "OK\n");
  else if(strcmp(cmd, "forkcnt") == 0 || strcmp(cmd, "forklist") == 0 ||
          strcmp(cmd, "forkcount") == 0)
    snprintf(textbuf, BUFFERLEN - (int)sizeof(int), "OK\n0");
  else
    snprintf(textbuf, BUFFERLEN - (int)sizeof(int), "OK\n");
  l = (int)strlen(textbuf); *(int *)commbuf = l;
  (void)wp_ipc_write_msg(fd, commbuf, l + (int)sizeof(int));
  wp_ipc_close(fd);
}

int main(int argc, char **argv)
{
  wp_ipc_server srv; char cwd[512], *p; FILE *f;
  (void)argc; (void)argv;
  if(!_getcwd(cwd, sizeof(cwd))) return 1;
  p = strstr(cwd, "\\public_html");
  if(!p) p = strstr(cwd, "/public_html");
  if(p){ *p = 0; _chdir(cwd); }
  _mkdir("tmp"); _mkdir("tmp/log");
  freopen("tmp/log/wimslogd.out", "w", stdout);
  freopen("tmp/log/wimslogd.err", "w", stderr);
  snprintf(pidstr, sizeof(pidstr), "%lu", (unsigned long)GetCurrentProcessId());
  f = fopen("tmp/log/wimslogd.pid", "w");
  if(f){ fputs(pidstr, f); fclose(f); }
  if(wp_ipc_listen(&srv, "tmp/log/.wimslogd") != 0){
    fprintf(stderr, "wimslogd: listen failed (%lu)\n", GetLastError());
    return 1;
  }
  printf("wimslogd %s started (win-min).\n", pidstr); fflush(stdout);
  for(;;){
    wp_ipc_fd c = wp_ipc_accept(&srv, 50000);
    if(c == WP_IPC_INVALID){ wp_loadavg(loadavg, sizeof(loadavg)); continue; }
    handle(c);
  }
  return 0;
}
