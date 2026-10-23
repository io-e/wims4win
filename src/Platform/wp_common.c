/* Platform-independent helpers of wims_platform. GPL v2 or later. */
#include "wims_platform.h"

long wp_strip_cr(char *buf, long len)
{
  long i, j;
  if(buf==NULL || len<=0) return len<0?0:len;
  for(i=j=0;i<len;i++) if(buf[i]!='\r') buf[j++]=buf[i];
  buf[j]=0;
  return j;
}
