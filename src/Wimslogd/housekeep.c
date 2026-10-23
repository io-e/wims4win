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

/* Daily housekeeping jobs. */

#include "wimslogd.h"
int stringtodays(char * dat)
{
  static int len[13] = {0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
  int y = 1000*(dat[0]-'0')+100*(dat[1]-'0')+10*(dat[2]-'0')+(dat[3]-'0');
  int m = 10*(dat[4]-'0')+(dat[5]-'0');
  int d = 10*(dat[6]-'0')+(dat[7]-'0');
  if (m < 3) {y--; d += 365;}
  return d + len[m] + 365*y + y/4 - y/100 + y/400;
}
void purgefile(char *fname)
{
  char buf[8];
  FILE *f = fopen(fname,"r");
  fread (buf,1,8,f);
  fclose(f);
  int d1 = stringtodays(buf);
  int d2 = stringtodays(nowstr);
  if (d2-d1 > LOG_DELETE)
    unlink(fname);
}

void housekeep(void)
{
  struct stat st;
  char fname[1024], pathname[1024];
  FILE *keeplog;
  static char* lognames[]={
    "access.log","referer.log","session.log","post.log","mail.log",
    "user_error.log","internal_error.log","refuse.log"
  };
  int i,j,cntlogs=sizeof(lognames)/sizeof(char*);
  if(strncmp(keepdate,nowstr,8)==0) return;
  snprintf(fname,sizeof(fname),"%s/keepdate",tmpd);
  keeplog=fopen(fname,"r");
  if(keeplog==NULL) goto dokeep;
  (void)fread(keepdate,8,1,keeplog); keepdate[8]=0; fclose(keeplog);
  if(strncmp(keepdate,nowstr,8)==0) return;
  dokeep:
  keeplog=fopen(fname,"w");
  if(keeplog!=NULL) {
    fwrite(nowstr,8,1,keeplog);fclose(keeplog);
  }
/* delete files older than LOG_DELETE days */
  for (j=0; j < cntlogs; ++j)
  {
    snprintf(pathname,sizeof(pathname),"%s/%s",logd,lognames[j]);
    for(i=OLD_LOG_FILES-1;i>0;i--) {
      snprintf(fname,sizeof(fname),"%s.old%d",pathname,i);
     if(stat(fname,&st)==0) purgefile(fname);
    }
    if(stat(pathname,&st)==0) purgefile(pathname);
  }

  call_ssh(0,"bin/housekeep.daily &>%s/housekeep.log",tmpd);
}

/* module update */
void modupdate(void)
{
  char fname[1024];
  FILE *muplog;

  if(strncmp(mupdate,nowstr,8)==0) return;
  snprintf(fname,sizeof(fname),"%s/mupdate",tmpd);
  muplog=fopen(fname,"r");
  if(muplog==NULL) goto domup;
  (void)fread(mupdate,8,1,muplog); mupdate[8]=0; fclose(muplog);
  if(strncmp(mupdate,nowstr,8)==0) return;
  domup:
  muplog=fopen(fname,"w");
  if(muplog!=NULL) {
    fwrite(nowstr,8,1,muplog);fclose(muplog);
  }
  call_ssh(0,"bin/modupdate.auto &>%s/modupdate.log",tmpd);
}

/* Daily backup. */
void backup(void)
{
  FILE *backlog;

  if(strncmp(backdate,nowstr,8)==0) return;
  backlog=fopen("backup/backdate","r");
  if(backlog==NULL) goto dobackup;
  (void)fread(backdate,8,1,backlog); backdate[8]=0; fclose(backlog);
  if(strncmp(backdate,nowstr,8)==0) return;
  dobackup:
  call_ssh(0,"bin/backup &>%s/backup.log",tmpd);
}
