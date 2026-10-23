/* wims side of multiexec (calc.c:229-258) on the platform layer. GPL v2+.
 *
 * Sends "<parm><marker>" to a persistent CAS interface and reads its stdout
 * until the marker comes back (or MAX bytes / error / timeout).
 *
 * fixed==0 reproduces the historical loop of calc.c EXACTLY, bugs included:
 *  BUG calc.c:241: "v1=0" is reset on every iteration, so the 3-second
 *    silence timeout (v2=300 x 100 ms) can never fire: if the interface
 *    stays silent without closing its stdout, wims waits forever (until its
 *    own RLIMIT_CPU/alarm kills it).
 *  BUG (same loop): read()==0 (EOF, interface died) is not treated as an
 *    end: select() reports readable again at once -> busy loop at 100% CPU.
 *  Also: the comment says "3 seconds" but v2=300 polls of 100 ms = 30 s.
 * fixed==1: v1 initialised once, v2=30 (the documented 3 s of cumulated
 *    silence) and EOF ends the exchange. Only used when WIMS_MX_FIX_TIMEOUT is defined.
 * Returns the number of bytes stored in p (p is 0-terminated, marker cut). */
#include <string.h>
#include "wims_platform.h"

long wp_mx_exchange(wp_chan *c, const char *parm, const char *marker,
                    char *p, long cap, int fixed)
{
  long i=0, k=cap, j; int v1=0, v2=fixed?30:300, cr; char *pp;
  (void)wp_write(c,parm,(long)strlen(parm));
  (void)wp_write(c,marker,(long)strlen(marker));
  *p=0;
  while(k>0) {
    if(!fixed) { v1=0; v2=300; }          /* historical reset (calc.c:241) */
    if(wp_poll(c,1,0,100000,&cr,NULL)<=0) {
      v1++; if(v1>=v2) break;
      else continue;
    }
    j=wp_read(c,p+i,k);
    if(j<0 || j>k) break;
    if(fixed && j==0) break;              /* EOF: interface is gone */
    i+=j; k-=j; p[i]=0;
    pp=strstr(p,marker);
    if(pp) {*pp=0; i=pp-p; break;}
  }
  return i;
}
