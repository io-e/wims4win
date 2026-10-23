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

/* Change root and exec */

#define PROC_QUOTA 15
#define UID_MIN 10000
#define UID_MASK 127
#define CPU_MAX 4096
#define TIME_MASK 16383
#define CLEAN_DELAY 100
#define CLEAN_DELAY2 500000
#define MAX_PARMLEN 16384
#define MAX_OUTLEN  65536
#define chroot_tmp "../chroot/tmp/sessions"
#define chroot_path "/bin:/usr/bin:/usr/local/bin"
#define timestamp "../tmp/log/chroot.stamp"

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>
#include <string.h>
#include <time.h>
#include <signal.h>
#include <utime.h>
#include <sys/time.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/resource.h>

int execuid=15999;
int execgid=15999;
int must=0;
time_t now;

char *env_rm[]={
  "s2_dir", "w_wims_home", "session_base_dir", "trusted_module",
    "session_dir", "wims_server_base", "w_httpd_PWD",
    "w_wims_sesdir", "HTTP_CONNECTION",
    "w_gnuplot_format", "w_insplot_font", "w_ins_anim_limit",
    "w_ins_density", "w_ins_format",
    "texgif_fontdir", "texgif_texheader",
    "w_wims_tmp_debug", "w_insmath_logic", "w_insmath_rawmath",
    "SERVER_ADMIN", "SERVER_ADDR", "SERVER_NAME"
};

#define env_rm_cnt (sizeof(env_rm)/sizeof(env_rm[0]))

char name_sh[32]="/bin/ash.static";
char opt_sh[32]="-c";
char *pre_sh="\
cd $TMPDIR || exit\n\
echo >/dev/null || exit\n\
cd / 2>/dev/null && exit\n\
";

char *name_perl="/usr/bin/perl";
char *opt_perl="-e";
char *pre_perl="\
chdir($ENV{TMPDIR}) || exit;\n\
chdir(\"/\") && exit;\n\
";


/* Remove a tree */
int remove_tree(char *dirname)
{
  DIR *sdir;
  struct dirent *f;
  struct stat dst;

  sdir=opendir(dirname);
  if(sdir==NULL) {   /* Cannot open session directory. */
     return -1;
  }
  while((f=readdir(sdir))!=NULL) {
    char fname[257];
    if(strcmp(".",f->d_name)==0 || strcmp("..",f->d_name)==0) continue;
    snprintf(fname,sizeof(fname),"%s/%s",dirname,f->d_name);
    if(lstat(fname,&dst)) continue;
    if(S_ISDIR(dst.st_mode)) remove_tree(fname);
    else {
      if(remove(fname)<0)
        fprintf(stderr,"ch..root: unable to remove %s. %s\n",fname,strerror(errno));
    }
  }
  closedir(sdir);
  if(rmdir(dirname)<0) {  /* Cannot remove directory. */
     return -1;
  }
  return 0;
}

/* Clean TMP */
void cleantmp(void)
{
  DIR *sdir_base;
  struct dirent *ses;
  struct stat dst;

  if(chdir("../chroot/tmp/sessions")<0) return;
  sdir_base=opendir(".");
  if(sdir_base==NULL) return;
  while((ses=readdir(sdir_base))!=NULL) {
    if(ses->d_name[0]=='.') continue;
    if(lstat(ses->d_name,&dst)) continue;
    if(!S_ISDIR(dst.st_mode)) continue;
    if(dst.st_mtime <= now) {
        if(dst.st_mtime>=now-CLEAN_DELAY) continue;
        if(dst.st_mtime>=now-CLEAN_DELAY2 && (dst.st_mode&S_IRWXO)==0) continue;
    }
    remove_tree(ses->d_name);
  }
}

/* Cleaning */
void cleaning(void)
{
  DIR *sdir_base;
  struct dirent *ses;
  struct stat dst;
  struct utimbuf ub;
  char dbuf[262];

  if(stat(timestamp,&dst)==0 && dst.st_mtime==now) return;
  ub.actime=ub.modtime=now; utime(timestamp,&ub);
  sdir_base=opendir("/proc");
  if(sdir_base==NULL) goto tmpdir;
  while((ses=readdir(sdir_base))!=NULL) {
    if(ses->d_name[0]<'0' || ses->d_name[9]>'9') continue;
    snprintf(dbuf,sizeof(dbuf),"/proc/%s",ses->d_name);
    if(lstat(dbuf,&dst)) continue;
    if(!S_ISDIR(dst.st_mode)) continue;
    if(dst.st_uid<UID_MIN || dst.st_uid>UID_MIN+UID_MASK) continue;
    if(((dst.st_gid-UID_MIN-now)&TIME_MASK)<=CPU_MAX) continue;
    kill(atoi(ses->d_name),SIGKILL);
  }
  closedir(sdir_base);
  tmpdir: return;
}

/* Test Must */
int test_must(void)
{
  char *pc;
  if(must) return 1;
  pc=getenv("chroot"); if(pc && strcmp(pc,"must")==0) return 1;
  else return 0;
}

/* MAIN */
int main(int argc,char *argv[])
{
  char *args[1024];
  char parm[MAX_PARMLEN];
  char tmpbuf[256];
  int i,k,uid,t;
  struct stat st;
  struct rlimit lim;
  char *p, *pp;

  if(argc<2) return 0;
  now=time(NULL);
  uid=geteuid();
  t=stat("../chroot/tmp/sessions/.chroot",&st);
  if(uid!=0 || t!=0) {
    if(test_must()) goto abandon;
    args[0]="bin/wrap..exec"; k=1;
  }
  else {
    k=0;
    p=getenv("REMOTE_ADDR");
    if(p && *p) {
      pp=strrchr(p,'.'); if(pp) execuid=(atoi(++pp)&UID_MASK)+UID_MIN;
    }
    getrlimit(RLIMIT_CPU,&lim);
    i=lim.rlim_max; if(i<0) i=0; if(i>=CPU_MAX) i=CPU_MAX-1;
    execgid=((i+now+1)&TIME_MASK)+UID_MIN;
    cleaning();
  }
  if(argc>1 && strcmp(argv[1],"cleantmpdir")==0) {
    if(uid!=0) fprintf(stderr,"ch..root cleantmpdir: uid not changed.\n");
    else cleantmp();
    return 0;
  }
  if(argc>3 && argv[1][0]=='&') {
    if(k) goto abandon;
    if(strcmp(argv[2],"sh")==0) {
      lim.rlim_cur=lim.rlim_max=MAX_OUTLEN;
      setrlimit(RLIMIT_FSIZE,&lim);
      args[0]=name_sh; args[1]=opt_sh;
      snprintf(parm,sizeof(parm),"%s\n%s\n",pre_sh,argv[3]);
      args[2]=parm; args[3]=NULL; must=1;
      goto cont;
    }
    if(strcmp(argv[2],"perl")==0) {
      lim.rlim_cur=lim.rlim_max=MAX_OUTLEN;
      setrlimit(RLIMIT_FSIZE,&lim);
      args[0]=name_perl; args[1]=opt_perl;
      snprintf(parm,sizeof(parm),"%s\n%s\n",pre_perl,argv[3]);
      args[2]=parm; args[3]=NULL; must=1;
      goto cont;
    }
    goto abandon;
  }
  for(i=0;i<1000 && i<argc; i++) args[i+k]=argv[i+1];
  args[i]=NULL;
  cont:
  if(uid!=0) {
    if(test_must()) goto abandon;
    goto ex2;
  }
  if(t!=0) {
    stat("bin",&st); execuid=execgid=st.st_uid;
    if(test_must()) goto abandon;
    goto ex;
  }
  if(chroot("../chroot")==0) {
    (void)chdir("/tmp");
    lim.rlim_cur=lim.rlim_max=PROC_QUOTA;
    setrlimit(RLIMIT_NPROC,&lim);
    setenv("PATH",chroot_path,1);
    p=getenv("w_wims_session");
    if(p && *p) {
      snprintf(tmpbuf,sizeof(tmpbuf),"/tmp/sessions/%s",p);
      p=strchr(tmpbuf,'_'); if(p) *p=0;
      setenv("TMPDIR",tmpbuf,1);
      setenv("tmp_dir",tmpbuf,1);
      p=getenv("w_wims_priv_chroot");
      if(p && strstr(p,"tmpdir")!=NULL)
        (void)chdir(tmpbuf);
    }
  }
  else if(test_must()) goto abandon;
  ex:
  if(setregid(execgid,execgid)<0) goto abandon;
  if(setreuid(execuid,execuid)<0) goto abandon;
  ex2:
  for(i=0;i<env_rm_cnt;i++) unsetenv(env_rm[i]);
  if(strchr(args[0],'/')) execv(args[0],args); else execvp(args[0],args);
  abandon: return 127;
}
