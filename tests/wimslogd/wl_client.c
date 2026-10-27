/* Minimal wimslogd client for tests: wl_client <sockfile> <command...>
 * Prints the daemon answer. Uses the same IPC as wims (wp_ipc). GPL v2+. */
#include <stdio.h>
#include <string.h>
#include "../../src/Platform/wp_ipc.h"
static char buf[4*1024*1024];
int main(int argc, char **argv)
{
  int i, n=0, r;
  if(argc<3) { fprintf(stderr,"usage: wl_client sockfile cmd...\n"); return 2; }
  for(i=2;i<argc;i++) n+=snprintf(buf+sizeof(int)+n,sizeof(buf)-sizeof(int)-n,"%s%s",i>2?" ":"",argv[i]);
  *(int*)buf=n;
  r=wp_ipc_transact(argv[1],buf,sizeof(buf));
  if(r<(int)sizeof(int)) { fprintf(stderr,"transact failed (%d)\n",r); return 1; }
  fwrite(buf+sizeof(int),1,*(int*)buf,stdout); putchar(10);
  fprintf(stderr,"[%d bytes]\n",*(int*)buf);
  return 0;
}
