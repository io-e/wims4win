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

/* This file contains a routine to do housekeeping:
 * it erases obsolete session directories.
 * Regular checkup every 10 minutes or so.
 */
#include "wimslogd.h"

/* internal */
void _cleaning(char *di,int hardcheck)
{
  DIR *sdir_base;
  struct dirent *ses;
  struct stat session_stat;
  char session_name[MAX_LINELEN+1];

  sdir_base=opendir(di);
  if(sdir_base==NULL) return;
  while((ses=readdir(sdir_base))!=NULL) {
    if(ses->d_name[0]=='.') continue;
    snprintf(session_name,sizeof(session_name),"%s/%s",di,ses->d_name);
    if(lstat(session_name,&session_stat)) {
      wimslogd_error("wimslog cleaning(): session stat failure.");
      return;
    }
    if(!S_ISDIR(session_stat.st_mode)) { /* not a directory: remove it. */
      if(remove(session_name)<0) {
        wimslogd_error("wimslogd cleaning(): unable to chase squatter file.");
        return;
      }
    }
    /* remove idle session. */
  else {
    char fbuf[4096],cbuf[MAX_LINELEN+1];
    char *pp;
    /* remove session inactive for more than idle_time seconds */
    if(session_stat.st_mtime<nowtime-idle_time ||
        session_stat.st_mtime>nowtime+anti_time) {
      remove:
      if(remove_tree(session_name)!=0) {
        if(strstr(session_name,"chroot")!=NULL) {
          char tbuf[4096];
          snprintf(tbuf,sizeof(tbuf),"/%s",session_name);
          setenv("tmp_dir",tbuf,1);
          chmod(session_name,
                S_IRUSR|S_IWUSR|S_IXUSR|
                S_IRGRP|S_IWGRP|S_IXGRP|
                S_IROTH|S_IWOTH|S_IXOTH);
          (void)chdir("public_html");
          call_ssh(1,"bin/ch..root cleantmpdir");
          (void)chdir(cwd);
          chmod(session_name,S_IRUSR|S_IWUSR|S_IXUSR);
          fprintf(stderr,"%s\n",tbuf);
        }
        if(remove_tree(session_name)!=0) {
          fprintf(stderr,"Unable to remove session %s: %s.\n",
                 session_name,strerror(errno));
        }
      }
      continue;
    }
    if(hardcheck==2) {
      char dbuf[MAX_FNAME+1];
      struct dirent *s2d;
      struct stat fst;
      int t;
      DIR *s2D;
      snprintf(dbuf,sizeof(dbuf),"%s/%s",sesd,ses->d_name);
      if(ftest(dbuf)!=is_dir) goto remove;
      s2D=opendir(session_name);
      if(sdir_base==NULL) goto remove;
      while((s2d=readdir(s2D))!=NULL) { /* remove individual files */
        snprintf(dbuf,sizeof(dbuf),"%s/%s",session_name,s2d->d_name);
        t=stat(dbuf,&fst);
        if(t==0 && fst.st_mtime<nowtime-INS_DELAY &&
               fst.st_mtime>=nowtime+anti_time) remove(dbuf);
      }
      closedir(s2D);
      continue;
    }
    /* keep sessions with _ in name unless hardcheck */
    if(!hardcheck || strchr(session_name,'_')!=NULL) continue;
    /* keep very new sessions (inactive for less than idle_time3 seconds) */
    if(session_stat.st_mtime>=nowtime-idle_time3 &&
       session_stat.st_mtime<nowtime+anti_time) continue;
    /* for sessions inactive for more than idle_time3 seconds,
       keep session if var.stat file exists and wims_user or wims_developer variable is defined
    */
    snprintf(fbuf,sizeof(fbuf),"%s/var.stat",session_name);
    wlogdaccessfile(fbuf,"r","%s/var.stat",session_name);
    if(fbuf[0]!= 0 && (strstr(fbuf,"wims_user=")!=NULL || strstr(fbuf,"wims_developer=")!=NULL)) continue;
    /* sessions with no var.stat file, chech var file */
    wlogdaccessfile(cbuf,"r","%s/var",session_name);
    if(cbuf[0]==0) goto remove;      /* no var file, remove */
    /* keep manager sessions */
    pp=strstr(cbuf,"\nw_wims_ismanager=");
    if(pp!=NULL) {
      pp+=strlen("\nw_wims_ismanager=");
      if(*pp>'0' && *pp<='9') continue;
    }
    /* remove session inactive for more than idle_time2 seconds */
    if(session_stat.st_mtime<nowtime-idle_time2 ||
        session_stat.st_mtime>nowtime+anti_time) goto remove;
    /* remove unused sessions */
    if(session_stat.st_mtime<nowtime-idle_time3 &&
        strstr(cbuf,"\nwims_new_session=yes\n")!=NULL) goto remove;
         /* remove popup session: 50 sec only. */
    if(session_stat.st_mtime<nowtime-50 &&
        strstr(cbuf,"\nw_wims_mode=popup\n")!=NULL) goto remove;
    }
  }
  closedir(sdir_base);
}

/* Clean obsolete session directories. */
void cleaning(int withmain)
{
  struct stat lastclean_stat;
  pid_t pid;
  char lastclean_name[MAX_FNAME+1];
  FILE *lastclean;
  /* Active only if idle_time>0 */
  if(idle_time<=0) return;
  /* when is last clean? */
  if(lastcleantime>nowtime-300) return;
  mystrncpy(lastclean_name,"tmp/log/lastclean",sizeof(lastclean_name));
  if(stat(lastclean_name,&lastclean_stat)==0 &&
      lastclean_stat.st_mtime>nowtime-300 &&
      lastclean_stat.st_mtime<nowtime+100) return;
  fflush(NULL);
  pid=fork(); if(pid>0) {addfork(pid,0); return;}
  close(commsock);
  if(withmain) _cleaning(sesd,1);
  _cleaning("s2",2);
  _cleaning("tmp/sessions",0);
  _cleaning("chroot/tmp/sessions",0);
  /* touch lastclean file */
  lastclean=fopen(lastclean_name,"w"); fclose(lastclean);
  lastcleantime=nowtime;
  snprintf(lastclean_name,sizeof(lastclean_name),"%s/trap.check",tmpd);
  if(stat(lastclean_name,&lastclean_stat)==0 &&
      (lastclean_stat.st_mtime<nowtime-3600 ||
      lastclean_stat.st_mtime>nowtime+anti_time)) unlink(lastclean_name);
  exit(0);
}
