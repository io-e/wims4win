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

/* line input / output / translation routines
 * and error routines
 */
#include <utime.h>
#include <sys/socket.h>
#include <sys/un.h>

#include "wims.h"

int is_class_module=0;

int trusted_module(void);
void phtml_put_base(char *fname,int cache);
void module_error(char msg[]);
void user_error(char msg[]);
void accessfile(char *content, char *type, char *s,...);
int remove_tree(char *p);
void output(char *s,...);
char *_getvar(char *vname);
int checkhost(char *hlist);

char *lastdata;
char lastdatafile[MAX_FNAME+1];
char *datacache[DATAFIELD_LIMIT];
int  nextdatacache;
struct stat ftst;
char lastftest[MAX_FNAME+1];
int lastftype;
char outbuf[8192];
char *outptr;

/* These modules can execute private programs.
 * adm/ modules are always trusted, so need no definition here.
 */
char *trusted_modules="";
/* bit 0: module is not trusted.
  * bit 1: file in wimshome.
  * bit 2: readdef or file in writable directory.
  */
int untrust=0;  /* non-zero if user detrusts the module. */

int error_status=0;
char pidbuf[32];

void delete_pid(void);

/* Internal use only */
void _debug(char *s,...)
{
  va_list vp;
  char buf[MAX_LINELEN+1];

  va_start(vp,s);
  vsnprintf(buf,sizeof(buf),s,vp);
  va_end(vp);
  setvar("debug",buf); module_error("debug");
  exit(1);
}


/* HTTP response header for non-processed CGI interface */
void nph_header(int code)
{
  char *cstr;
  switch(code) {
    case 200: cstr="OK"; break;
    case 301: cstr="Moved Permanently"; break;
    case 302: cstr="Moved Temporarily"; break;
    case 420: cstr="WIMS Nested Error"; break;
    case 450: cstr="WIMS User Error"; break;
    case 500: cstr="WIMS Internal Error"; break;
    case 550: cstr="WIMS Module Error"; break;
    default: cstr="ERROR"; break;
  }
  if(httpd_type!=httpd_wims) printf("Status: %d %s\r\n",code,cstr);
  else {
    char *p, tbuf[256];
    mystrncpy(tbuf,ctime(&nowtime),sizeof(tbuf));
    for(p=tbuf+strlen(tbuf);p>=tbuf && (*(p-1)=='\n' || *(p-1)=='\r'); p--);
    *p=0;
    printf("HTTP/1.0 %d %s\r\n\
Date: %s\r\n\
Connection: close\r\n\
",code,cstr,tbuf);
  }
}

void flushoutput(void)
{
  int l2;
  if(outptr<=outbuf) return;
  l2=outptr-outbuf;
  if(lastout_file!=-1) (void)write(lastout_file,outbuf,l2);
  else fwrite(outbuf,l2,1,stdout);
  outptr=outbuf;
}

void internal_warn(char msg[])
{
  char buf[1024];
  fprintf(stderr,"wims: %s\n%s\n",msg,strerror(errno));
  snprintf(buf,sizeof(buf),"%s: %s\n",nowstr,msg);
  accessfile(buf,"a","%s/internal_error.log",log_dir);
}

/* Internal error: panic and forget about requester. */
void internal_error(char msg[])
{
  if(error_status<2) {
    nph_header(500);
    printf("Cache-Control: no-cache, no-store, must-revalidate\r\n\
Content-type: text/plain\r\n\r\n\r\n\
WIMS panick! %s\n%s\n",msg,strerror(errno));
      error_status=2; internal_warn(msg);
  }
  delete_pid(); exit(1);
}

void tex_nospace(char *p)
{
  char *p1, *p2;
  char buf[MAX_LINELEN+1];
  for(p1=buf,p2=p;*p2 && p1-buf<MAX_LINELEN-4;p2++) {
    if(!isspace(*p2)) {*p1++=*p2;continue;}
    while(isspace(*p2)) p2++;
    if(*(p1-1)=='\\' ||
       (p1>buf && myisalnum(*(p1-1)) && myisalnum(*p2)))
      *p1++=' ';
    *p1++=*p2;
  }
  *p1++=0; memmove(p,buf,p1-buf);
}

void nametoolong(char *p)
{
  char buf[MAX_FNAME+17];
  snprintf(buf,sizeof(buf),"%s...",p);
  force_setvar("wims_error_data",buf);
  module_error("file_name_too_long");
}

off_t ftest_size;

/* A simple front-end of stat(). */
int ftest(char *fname)
{
  if(strcmp(fname,lastftest)==0) return lastftype;
/* if(fname[0]=='/' || fname[0]=='.') fprintf(stderr,"ftest: %s\n",fname); */
  mystrncpy(lastftest,fname,sizeof(lastftest));
  if(stat(fname,&ftst)) return lastftype=-1;
  ftest_size=ftst.st_size;
  if(S_ISREG(ftst.st_mode)) {
    if((ftst.st_mode&(S_IXUSR|S_IXGRP|S_IXOTH))!=0) return lastftype=is_exec;
    else return lastftype=is_file;
  }
  if(S_ISDIR(ftst.st_mode)) return lastftype=is_dir;
  if(S_ISFIFO(ftst.st_mode)) return lastftype=is_fifo;
  if(S_ISSOCK(ftst.st_mode)) return lastftype=is_socket;
  return lastftype=is_unknown;
}

char fnbuf[MAX_FNAME+1];

/* make a filename and check length */
char *mkfname(char buf[], char *s,...)
{
  va_list vp;
  char *p;

  if(buf==NULL) p=fnbuf; else p=buf;
  va_start(vp,s);
  vsnprintf(p,MAX_FNAME,s,vp);
  va_end(vp);
  if(strlen(p)>=MAX_FNAME-1) nametoolong(p);
  return p;
}

void sysmask_trigger(char *s)
{
  char buf[MAX_FNAME+1];
  struct stat st;
  mkfname(buf,"%s/%s",sysmask_trigger_dir,s);
  stat(buf,&st);
}

/* read-in a file into buffer. Use open() and read().
       * Return buffer address which will be malloc'ed if buf=NULL. */
char *readfile(char *fname, char buf[], long int buflen)
{
  int fd, t, st;
  long int l, lc;
  char *bf;
  t=0; if(buf) buf[0]=0;
  st=ftest(fname); if(st!=is_file) {
    if(st==is_exec) { /* refuse to open executable file */
      setvar("executable",fname); module_error("executable");
    }
    return NULL;
  }
  l=ftst.st_size; if(l<0) return NULL;
  if(l>=buflen) {
    if(buflen<MAX_LINELEN) l=buflen-1;
    else {
      if(strncmp(fname,"modules/",strlen("modules/"))==0) {
        setvar(error_data_string,fname); module_error("file_too_long");
      }
      else user_error("cmd_output_too_long");
    }
  }
  fd=open(fname,O_RDONLY); if(fd==-1) return NULL;
  if(buf==NULL) bf=xmalloc(l+8); else {bf=buf;if(l==0) {t=1; l=buflen-1;}}
  lc=read(fd,bf,l); close(fd);
  if(lc<0 || lc>l || (lc!=l && t==0)){
    if(buf==NULL) free(bf); else buf[0]=0; return NULL;
  }
  bf[lc]=0; _tolinux(bf); return bf;
}

/* Get a line in a stored working file.
       * Buffer length is always MAX_LINELEN. */
int wgetline(char buf[], size_t buflen, WORKING_FILE *f)
{
  int i,j; unsigned int n;
  i=f->linepointer; buf[0]=0;
  if(i>=f->linecnt || f->textbuf==NULL) return EOF;
  n=f->lines[i].llen;
  if(n>=buflen) n=buflen-1;
  if(n>0) memmove(buf,f->lines[i].address,n);
  buf[n]=0;
  for(j=i+1;j<f->linecnt && f->lines[j].isstart==0;j++);
  f->l=i; f->linepointer=j;
  if(j>=f->linecnt && n==0) return EOF; else return n;
}

int get_cached_file(char *name)
{
  int i,l,flag;
  l=strlen(name); if(l>=127) return -1;
  if(strncmp(module_prefix,module_dir,strlen(module_dir))!=0) flag=8; else flag=0;
  for(i=0;i<mcachecnt;i++) {
    if(flag==mcache[i].nocache && strcmp(name,mcache[i].name)==0) {
      memmove(&m_file,mcache+i,sizeof(WORKING_FILE));
      m_file.nocache|=4;
      return i;
    }
  }
  return -1;
}

/* Open a work file. Returns 0 if OK. */
int open_working_file(WORKING_FILE *f, char *fname)
{
  char *p, *q;
  void *vp;
  int i,j,k,laststart,lc[LINE_LIMIT];

  f->linecnt=f->linepointer=0;
  f->for_idx=f->nocache=0;
  f->l=-1; f->lines=NULL;
  mystrncpy(f->filepath,fname,MAX_FNAME+1);
  f->textbuf=readfile(fname,NULL,WORKFILE_LIMIT);
  if(f->textbuf==NULL) return -1;
  p=f->textbuf; if(*p) {
    for(i=0,p--; i<LINE_LIMIT-1 && p!=NULL;p=strchr(p,'\n')) {
      if(i>0) *p=0;
      lc[i++]=(++p-f->textbuf);
    }
  }
  else i=0;
  if(i>=LINE_LIMIT-1) module_error("file_too_long");
  if(i==0) {
    f->linecnt=0; f->for_stack=NULL; f->lines=NULL;
    mfilecnt++; return 0;
  }
  lc[i]=lc[i-1]+strlen(f->textbuf+lc[i-1])+1;
  f->textbuf[lc[i]]=0;
  f->linecnt=i; laststart=0;
  f->for_stack=xmalloc(MAX_FOR_LEVEL*sizeof(FOR_STACK)+(i+1)*sizeof(LINE_STRUCT));
  vp=f->for_stack+MAX_FOR_LEVEL; f->lines=vp;
  for(j=0;j<i;j++) {
    f->lines[j].address=p=(f->textbuf)+lc[j];
    f->lines[j].llen=lc[j+1]-lc[j]-1;
    (f->lines[j]).execcode=(f->lines[j]).varcode=-1;
    if(j==0) goto normal;
    q=f->lines[laststart].address+f->lines[laststart].llen-1;
    if(q>=f->textbuf && *q=='\\') {
      f->lines[laststart].llen+=f->lines[j].llen; *q='\n';
      f->lines[j].isstart=f->lines[j].llen=0;
      memmove(q+1,p,lc[j+1]-lc[j]);
    }
    else {
      normal: for(q=p;myislspace(*q);q++);
      f->lines[j].isstart=1; laststart=j; k=0;
      switch(*q) {
                /* isstart: bit 1 = start.
                 * bit 2 = exec
                 * bit 3 (4) = label
                 * bit 4 (8) = hard comment (!!)
                 * bit 5 (16) = soft comment (#) */
        case exec_prefix_char: {
          if(myisalpha(q[1])) f->lines[j].isstart=3;
          else f->lines[j].isstart=9;
          k=1; break;
        }
        case label_prefix_char: {f->lines[j].isstart=5; k=1; break;}
        case comment_prefix_char: {f->lines[j].isstart=17; break;}
        default: {f->lines[j].isstart=1; break;}
      }
      if(k && q>p) {f->lines[j].address=q;f->lines[j].llen-=q-p;}
    }
  }
  f->lines[i].isstart=1; f->lines[i].llen=0;
  f->lines[i].address=(f->textbuf)+lc[i];
  mfilecnt++; return 0;
}

/* close an earlier opened working file */
void close_working_file(WORKING_FILE *f, int cache)
{
  f->linepointer=f->l=0;
  if(cache && untrust==0 && mcachecnt<MAX_MCACHE && (f->nocache&7)==0) {
    memmove(mcache+mcachecnt,f,sizeof(WORKING_FILE));
    mcachecnt++;
  }
  else if((f->nocache&4)==0) {
    if(f->for_stack!=NULL) free(f->for_stack);
    if(f->textbuf!=NULL) free(f->textbuf);
  }
  f->for_stack=NULL; f->textbuf=NULL; f->linecnt=0;
}

void free_mcache(void)
{
  int i;
  untrust=0;
  for(i=mcachecnt-1;i>=0;i--) close_working_file(mcache+i,0);
  mcachecnt=0;
}

void cleantmpdir(void);
void user_error_log(char msg[]);

void nested_error(char msg[])
{
  fprintf(stderr,"\nNested error! %s\n",msg);
  nph_header(420);
  printf("\r\n\r\nWIMS error processing aborted on nested error.\r\n\r\n%s\r\n",msg);
  delete_pid(); exit(1);
}

/* Send an error message to requester and exit.
 * This is for user errors, language-sensitive.
 */
void user_error(char msg[])
{
  char erfname[MAX_FNAME+1];

  if(error_status) nested_error(msg);
  error_status=1;
  mkfname(erfname,"%s.%s",user_error_msg_file,lang);
  if(ftest(erfname)!=is_file) internal_error("user_error(): error message file not found.\n\
Bad installation.");
  force_setvar("wims_user_error",msg);
  if(strcmp(msg,"threshold")!=0) user_error_log(msg);
  memmove(module_prefix,".",2);
  if(lastout_file!=-1 && outputing) {
    flushoutput(); close(lastout_file); lastout_file=-1;
  }
  nph_header(450);
  phtml_put_base(erfname,0);
  if(strcmp(msg,"double_click")!=0) delete_pid();
  else {
    cleantmpdir(); flushlog();
  }
  flushoutput(); exit(0);
}

void module_error_log(char msg[]);

/* Messages for module errors. English only.
 * This is really rudimentary for the time being.
 */
void module_error(char msg[])
{
  int send=0;
  char *p;
  WORKING_FILE mf;

  if(error_status) nested_error(msg);
  error_status=1; untrust=0;
  module_error_log(msg);
  nph_header(550);
  printf("Server: %s %s (%s)\r\n", SHORTSWNAME,wims_version,LONGSWNAME);
  p=getvar("wims_main_font");
  if(p!=NULL && *p!=0) printf("Content-type: text/plain; charset=%s\r\n\r\n",p);
  else printf("Content-type: text/plain\r\n\r\n");
  p=getvar(ro_name[ro_module]); if(p==NULL) p="???";
  printf("ERROR.\n\nwims has detected an error in the module '%s'.",p);
  if(m_file.l>=0) printf("\n\nIn file '%s', line %d:", m_file.name,m_file.l+1);
  printf(" %s.\n\n",msg);
  if(open_working_file(&mf,mkfname(NULL,"%s.%s",module_error_msg_file,lang))!=0)
    internal_error("module_error(): error message file not found.");
  while(wgetline(tmplbuf,MAX_LINELEN,&mf)!=EOF) {
    if(tmplbuf[0]!=tag_prefix_char) {
      if(send) {substit(tmplbuf); puts(tmplbuf);}
      continue;
    }
    strip_trailing_spaces(tmplbuf);
    if(tmplbuf[1]==0 || strcmp(msg,tmplbuf+1)==0) send=1;
    else send=0;
  }
  close_working_file(&mf,0);
  outptr=outbuf; delete_pid(); exit(1);
}

/* Output kernel routine */
void _output_(char *s)
{
  int l,l2;
  l=strlen(s); output_length+=l;
  if(output_length>=OUTPUT_LENGTH_LIMIT) {
    module_error("output_too_long"); return;
  }
  l2=sizeof(outbuf)-(outptr-outbuf);
  put: if(l<=l2) {
    memmove(outptr,s,l); outptr+=l; return;
  }
  memmove(outptr,s,l2); s+=l2; l-=l2;
  if(lastout_file!=-1) (void)write(lastout_file,outbuf,sizeof(outbuf));
  else fwrite(outbuf,sizeof(outbuf),1,stdout);
  outptr=outbuf; l2=sizeof(outbuf); goto put;
}

/* Output with no var. */
void output0(char buf[])
{
  char *p1, *p2, *ps;
  int dynsave;
  if(backslash_insmath) {
    ps=buf; dynsave=instex_usedynamic;
    for(p1=strstr(buf,"\\("); p1; p1=strstr(p2,"\\(")) {
      p2=find_matching(p1+2,')');
      if(p2==NULL) break;
      if(p1>buf && *(p1-1)=='\\') continue;
      *p1=0; if(*(p2-1)=='\\') *(p2-1)=0; *p2++=0; _output_(ps); ps=p2;
      instex_usedynamic=1; insmath(p1+2);
    }
    if(*ps) _output_(ps);
    instex_usedynamic=dynsave;
  }
  else _output_(buf);
}

/* Output routine */
void output(char *s,...)
{
  va_list vp;
  char buf[4*MAX_LINELEN+1];

  va_start(vp,s);
  vsnprintf(buf,sizeof(buf),s,vp);
  va_end(vp);
  output0(buf);
}

/* read in tmpf in tmp directory, and places in p.
 * Maximal length: MAX_LINELEN.
 */
void read_tmp_file(char *p, const char *fname)
{
  char *name, *pp;
  name=mkfname(NULL,"%s/%s",tmp_dir,fname);
  if(!exec_is_module || !outputing || !direct_exec
     || strcmp(fname,"exec.out")!=0) {
    readfile(name,p,MAX_LINELEN);
    pp=getvar("wims_exec_dollar_double");
    if(pp && strcmp(pp,"yes")==0) {
      for(pp=strchr(p,'$'); pp; pp=strchr(pp+2,'$'))
        string_modify(p,pp,pp+1,"$$");
    }
  }
  else {
    char *s;
    s=readfile(name,NULL,OUTPUT_LENGTH_LIMIT);
    if(s==NULL) {*p=0; return;}
    if(memcmp(s,"Error: ", strlen("Error: "))==0) mystrncpy(p,s,MAX_LINELEN);
    output_length+=strlen(s);
    if(output_length>=OUTPUT_LENGTH_LIMIT) module_error("output_too_long");
    else _output_(s);
    free(s); *p=0;
    chmod(name,S_IRUSR|S_IWUSR);
  }
}

/* verify whether the module is trusted.
 * Returns 1 if yes, 0 if no. -1 for error.
 */
int trusted_module(void)
{
  char *modname, *w, buf[MAX_LINELEN+1];
  int i,n;
  static int _trusted=-1;      /* avoid repeated computations */

  if(untrust&255) return 0;
  if(_trusted>=0) return _trusted;
  modname=getvar(ro_name[ro_module]);
  if(modname==NULL || *modname==0) return 0;
  if(memcmp(modname,"adm/",strlen("adm/"))==0 ||
     memcmp(modname,"classes/",strlen("classes/"))==0 ||
     strcmp(modname,home_module)==0 ||
     memcmp(modname,"help/",strlen("help/"))==0) {
    tr:
    if(memcmp(modname,"classes/",strlen("classes/"))==0)
      is_class_module=1;
    setenv("trusted_module","yes",1);
    return _trusted=1;
  }
  n=wordnum(trusted_modules);
  for(i=0;i<n;i++) {
    w=fnd_word(trusted_modules,i+1,buf);
    if(strcmp(w,modname)==0) goto tr;
  }
  return _trusted=0;
}

/* file should be in the module directory, but
 * it may also be somewhere else.
 * buf[] requires MAX_FNAME+1 length.
 * Returns 0 if found.
 */
int find_module_file(char *fname, char buf[], int mode)
{
  char *p, dtest[32];

  fname=find_word_start(fname);
  if(*fname==0) return -1;
/* Name checking: no directory backtracing. */
  if(strstr(fname,parent_dir_string)!=NULL) {
    setvar(error_data_string,fname); module_error("illegal_fname");
    return -1;
  }
  p=strchr(fname,'/'); if(p==NULL || p>fname+10) goto openit;
  memmove(dtest,fname,p-fname); dtest[p-fname]=0;
  if(strcmp(dtest,"datamodule")==0) {
    mkfname(buf,"modules/data%s",p); goto lastopen;
  }
  if(strcmp(dtest,"wimshome")==0 && trusted_module()) {
    mkfname(buf,"%s%s",getvar("wims_home"),p); goto lastopen;
  }
  if(strcmp(dtest,"writable")==0) {
    if(strncmp(p+1,"TEMP_",5)==0 && strchr(p+1,'/')==NULL) {
      mkfname(buf,"%s/%s",tmp_dir,p+1);
    }
    else {
      mkfname(buf,"w/%s/%s",module_prefix,p+1);
    }
    untrust|=4;
  }
  else {
    openit: mkfname(buf,"%s/%s",module_prefix,fname);
  }
  if(mode) return 0;
  if(ftest(buf)!=is_file) {
    if(lastftype==is_exec) {
isexec:
      setvar("executable",fname); module_error("executable");
      return -1;
    }
    if(strncmp(fname,"adm/",4)==0 &&
       (!trusted_module() || is_class_module)) return -1;
    /*
      2026_06_10: added wims_printmodule for datafile to work in
      adm/sheet (cfr. script/oef/printanalysis.proc)
     */
    char *ua;
    ua=getvar("wims_printmodule");
    // do not want to apply the modification for script in script/oef
    if(ua!=NULL && strncmp(ua,"modules/",strlen("modules/"))==0 && strncmp(fname,"oef/",strlen("oef/"))!=0 && strncmp(fname,"gp/",strlen("gp/"))!=0 && strncmp(fname,"slib/",strlen("slib/"))!=0 && strncmp(fname,"canvasimagedir",strlen("canvasimagedir"))!=0 && strncmp(fname,"data/polyedre_off",strlen("data/polyedre_off"))!=0 ) {
      mkfname(buf,"%s/%s",ua,fname);
      goto lastopen;
    }
    mkfname(buf,"scripts/%s",fname);
    lastopen:
    if(mode) return 0;
    if(ftest(buf)!=is_file) {
      if(lastftype==is_exec) goto isexec;
      else return -1;
    }
  }
  return 0;
}

/* check whether a file is user-submitted
 * This is deprecated because of the wimshome/ method.
 */
/* int user_file(char *name) {
    if(name[0]=='/' || name[0]=='.' ||
       strstr(name,"classes/")!=NULL ||
       strstr(name,"forums/")!=NULL ||
       strstr(name,"sessions/")!=NULL ||
       strstr(name,"doc/")!=NULL) return 1; else return 0;
}
 */

/* returns 1 if violation */
int datafile_check(char *name) {
  if((untrust&255)==0) return 0;
  if(strncmp(name,"data/",strlen("data/"))==0) return 0;
  if(strncmp(name,"authors/",strlen("authors/"))==0) return 0;
  if(strncmp(name,"datamodule/",strlen("datamodule/"))==0) return 0;
  return 1;
}

/* returns 0 if success */
void readdatafile(char *name)
{
  char *pp;
  if(strcmp(name,lastdatafile)==0) return;
  lastdata[0]=0; readfile(name,lastdata,WORKFILE_LIMIT);
  mystrncpy(lastdatafile,name,sizeof(lastdatafile));
  datacache[0]=lastdata; nextdatacache=1;
  if(lastdata[0]==tag_string[1]) {
    datacache[1]=lastdata; nextdatacache++;
  }
  pp=strstr(lastdata,tag_string);
  if(pp) datacache[nextdatacache]=pp;
  else datacache[nextdatacache]=lastdata+strlen(lastdata);
}

char *_nextdata(char *p)
{
  char *pp;
  if(!*p) return p;
  pp=strstr(p,tag_string);
  if(pp) return pp;
  else return p+strlen(p);
}

/* datafile structure: number of records.
 * tag=1 if direct access
 */
unsigned int datafile_recordnum(char *p)
{
  char nbuf[MAX_LINELEN+1], *pp;
  int i, t, ret;

  t=untrust; ret=0;
  if(direct_datafile) mystrncpy(nbuf,p,sizeof(nbuf));
  else if(datafile_check(p)!=0 || find_module_file(p,nbuf,0)) goto ret;
  readdatafile(nbuf);
  for(i=nextdatacache, pp=datacache[i]; *pp;) {
    pp=_nextdata(pp+1); i++;
    if(i<DATAFIELD_LIMIT) {
      datacache[i]=pp; nextdatacache=i;
    }
  }
  ret=i-1;
  ret:
  untrust=t;
  return ret;
}

/* datafile structure: find record n, starting from 1 */
char *datafile_fnd_record(char *p, int n, char bf[])
{
  char nbuf[MAX_LINELEN+1], *pp, *p2;
  int i, t;

  bf[0]=0; t=untrust;
  if(n<0) goto ret;
  if(direct_datafile) mystrncpy(nbuf,p,sizeof(nbuf));
  else if(datafile_check(p)!=0 || find_module_file(p,nbuf,0)) goto ret;
  readdatafile(nbuf); if(*lastdata==0) goto ret;
  if(n>nextdatacache) {
    for(i=nextdatacache, pp=datacache[i]; i<n && *pp;) {
      pp=_nextdata(pp+1); i++;
      if(i<DATAFIELD_LIMIT) {
        datacache[i]=pp; nextdatacache=i;
      }
    }
  }
  else pp=datacache[n];
  if(!*pp) goto ret;
  if(n>1 || (n==1 && *pp!=tag_string[1])) pp+=strlen(tag_string);
  else if(n==1) pp+=strlen(tag_string)-1;
  if(n<nextdatacache) p2=datacache[n+1];
  else {
    p2=strstr(pp,tag_string); if(p2==NULL) p2=pp+strlen(pp);
    if(n<DATAFIELD_LIMIT-1 && n==nextdatacache) {
          nextdatacache++; datacache[nextdatacache]=p2;
    }
  }
  if(p2-pp>=MAX_LINELEN) p2=pp+MAX_LINELEN-1;
  if(p2<pp) p2=pp;
  memmove(bf,pp,p2-pp); bf[p2-pp]=0;
  ret:
  untrust=t; return bf;
}

char hex2char(char c1, char c2)
{
  char tbuf[16];
  if(c1<'0' || c1>'f' || c2<'0' || c2>'f') {
invl:
    snprintf(tbuf,sizeof(tbuf),"%%%c%c",c1,c2);
    setvar(error_data_string,tbuf);
    user_error("invalid_char_in_query_string");
  }
  c1=toupper(c1);c2=toupper(c2);
  if(c1>'9' && c1<'A') goto invl;
  if(c2>'9' && c2<'A') goto invl;
  if(c1>'F' || c2>'F') goto invl;
  if(c1>='A') c1=c1-'A'+'9'+1;
  if(c2>='A') c2=c2-'A'+'9'+1;
  return (c1-'0')*16+c2-'0';
}

/* Converts back http escaped chars, slight. Does not check buffer length.
 * Returns converted string length.
 */
int _http2env(char outs[], char ins[])
{
  int j,k,l;
  l=strlen(ins);
  for(j=k=0;j<l && !isspace(ins[j]);j++,k++) {
    if(isspace(ins[j])) {  /* skip space characters in query string */
      k--;continue;
    }
    if(ins[j]=='%') {
/* skip Carriage-Return. */
        if(ins[j+1]=='0' && (ins[j+2]=='d' || ins[j+2]=='D')) {
          j+=2; k--; continue;
        }
        outs[k]=hex2char(ins[j+1],ins[j+2]);
        j+=2; continue;
    }
    outs[k]=ins[j];
  }
  outs[k]=0;
  return k;
}

/* Converts back http escaped chars. Does not check buffer length.
 * Returns converted string length.
 */
int http2env(char outs[], char ins[])
{
  int j,k,l;
  l=strlen(ins);
  for(j=k=0;j<l && !isspace(ins[j]);j++,k++) {
    if(isspace(ins[j])) {  /* skip space characters in query string */
      k--;continue;
    }
    if(ins[j]=='%') {
/* skip Carriage-Return. */
      if(ins[j+1]=='0' && (ins[j+2]=='d' || ins[j+2]=='D')) {
        j+=2; k--; continue;
      }
      outs[k]=hex2char(ins[j+1],ins[j+2]);
      j+=2; continue;
    }
    if(ins[j]=='+') {
      outs[k]=' '; continue;
    }
    if(ins[j]=='?' || ins[j]=='&') {
      outs[k]=0; continue;
    }
    outs[k]=ins[j];
  }
  outs[k]=0;
  return k;
}

/* translate a string to http querystring style.
 * '&' is not translated.
 * Buffer p must be at least MAX_LINELEN.
 */
void tohttpquery(char *p)
{
  char trlist[]="	()[]{}+-*^|/\"\'!:;,<>\n";
  char *pp;
  for(pp=p;*pp;pp++) {
    if(*pp==' ') {
      *pp='+'; continue;
    }
    if(strchr(trlist,*pp)==NULL) continue;
    if(*pp=='+' && pp>p && *(pp-1)=='&') continue;
    if(pp>p && *(pp-1)=='\\') {
      ovlstrcpy(pp-1,pp);pp--;continue;
    }
    if(*pp=='\n') {
      string_modify(p,pp,pp+1,"%%0D%%0A");pp+=5;
    }
    else {
        string_modify(p,pp,pp+1,"%%%02X",*pp);pp+=2;
    }
  }
}

/* substitute backslash parameters. Internal use only.
replace in expression all \var by value of m_var variable
if m_var not defined do nothing
principally used in primitive script and insmath.c*/
void slashsubst(char *p)
{
  char *p1, *p2, *pt, *pp, namebuf[128];
  int n;

  n=strlen(mathfont_prefix); memmove(namebuf,mathfont_prefix,n+1);
  for(p1=strchr(p,'\\'); p1!=NULL; p1=strchr(p1,'\\')) {
    p1++; for(p2=p1; myisalnum(*p2) || *p2=='_'; p2++);
    if(p2<=p1 || p2>p1+100) continue;
    memmove(namebuf+n,p1,p2-p1); namebuf[p2-p1+n]=0;
    pt=_getvar(namebuf); if(pt==NULL) continue;
    if(*p2=='[' && (pp=find_matching(p2+1,']'))!=NULL) {
      string_modify(p,pp+1,pp+1,")");
      string_modify(p,p1-1,p1,"$(%s",mathfont_prefix);
    }
    else string_modify(p,p1-1,p1,"$%s",mathfont_prefix);
  }
}

/* two alarm handlers. */
void alarm1(int s)
{
  if(killpid>0 && kill(killpid,SIGKILL)) module_error("timeup");
  killpid=0;
}

void alarm2(int s)
{
  cleantmpdir();
  alarm1(s); module_error("timeup");
}

void finalalarm(void)
{
  time_t curr;
  curr=time(0);
  if(curr>=limtime) alarm2(SIGALRM);
  errno=0;
  if(signal(SIGALRM,alarm2)==SIG_ERR)
    internal_error(strerror(errno));
  alarm(limtime-curr+1);
}

void initalarm(void)
{
  limtimex=nowtime+4*rlimit_cpu/3;
  limtime=limtimex+2; finalalarm();
}

void forkalarm(void)
{
  time_t curr;
  curr=time(0);
  if(curr>=limtimex) {alarm1(SIGALRM); return;}
  if(signal(SIGALRM,alarm1)==SIG_ERR)
    internal_error(strerror(errno));
  alarm(limtimex-curr+1);
}

/* create pid tag */
void create_pid(void)
{
  char buf[MAX_FNAME+1], pbuf[256], obuf[MAX_FNAME+1];
  struct stat dst;
  struct utimbuf ub;

  if(robot_access || *session_prefix==0) return;
  if(cmd_type==cmd_getframe) return;
  mkfname(buf,"%s/.pid",s2_prefix);
/* another process running? */
  if(readfile(buf,pbuf,sizeof(pbuf))!=NULL) {
    mkfname(obuf,"/proc/%s",pbuf);
    if(stat(obuf,&dst)==0) user_error("double_click");
  }
  snprintf(pidbuf,sizeof(pidbuf),"%u",getpid());
  accessfile(pidbuf,"w","%s",buf);
/* Touch session time */
  if(strstr(session_prefix,"sessions/")==NULL) return;
  ub.actime=ub.modtime=nowtime;
  utime(session_prefix,&ub);
  if(strchr(session_prefix,'_')!=NULL) { /* touch parent too */
    char sbuf[MAX_FNAME+1], *p;
    mystrncpy(sbuf,session_prefix,sizeof(sbuf));
    p=strchr(sbuf,'_'); if(p!=NULL) *p=0;
    utime(sbuf,&ub);
  }
}

struct mxtab mxtab[MAX_MULTIEXEC];
int mxno=0;

int execredirected(char *cmdf, char *inf, char *outf, char *errf, char *arg[])
{
  pid_t pid;
  int status, t;

  if(robot_access) return 0;
  if(time(0)>=limtimex) {
    if(errf!=NULL)
      accessfile("No time left to execute subprograms.\n","w","%s",errf);
    return -100;
  }
  lastdatafile[0]=lastftest[0]=0;
  fflush(NULL); /* flush all output streams before forking
                 * otherwise they will be doubled
                 */
  pid=fork(); if(pid==-1) return -1;
  if(!pid) {      /* child */
    char buf[MAX_LINELEN+1]; int k;
    (void)nice(10);      /* lower priority for children */
    if(is_multiexec) {
      dup2(mxtab[multiexec_index].pipe_stdin[0],0);
      dup2(mxtab[multiexec_index].pipe_stdout[1],1);
      dup2(mxtab[multiexec_index].pipe_stderr[1],2);
    }
    else {
      if(inf!=NULL) (void)freopen(inf,"r",stdin);
      if(outf!=NULL) (void)freopen(outf,"w",stdout);
      if(errf!=NULL) (void)freopen(errf,"w",stderr);
    }
/* This is to patch LinuxPPC uid wrapping
 * for scripts */
    t=0; if(strchr(cmdf,'/')) {
      int tf;
      char tbuf[16];
      tf=open(cmdf,O_RDONLY); (void)read(tf,tbuf,8); close(tf);
      if(memcmp(tbuf+1,"ELF",3)!=0) t=1;
    }
    if(wrapexec==-1) {
      if(setreuid(getuid(),getuid()) || setregid(getgid(),getgid())) _exit(127);
    }
    if(wrapexec==1 || (t==1 && wrapexec==0)) {
      if(setreuid(geteuid(),geteuid()) || setregid(getegid(),getegid())) _exit(127);
    }
    errno=0;
    if(strchr(cmdf,'/')) execve(cmdf,arg,environ);
    else execvp(cmdf,arg);
    snprintf(buf,sizeof(buf),"Failed to execute");
    for(k=0;arg[k];k++) {
      t=strlen(buf);
      snprintf(buf+t,sizeof(buf)-t," %s",arg[k]);
    }
    t=strlen(buf);
    snprintf(buf+t,sizeof(buf)-t,"\n	%s\n",strerror(errno));
    accessfile(buf,"a","%s/exec.fail",tmp_dir);
    exit(127);
  }
  else {      /* parent */
    wrapexec=0; status=0;
    if(exec_wait && !is_multiexec) {
      killpid=pid; forkalarm();
      waitpid(pid,&status,0); killpid=0; finalalarm();
    }
    return WEXITSTATUS(status);
  }
}

/* preparation for resident execution.
 * Returns 1 if already up, otherwise 0.
 */
int multiexec(char *cmd, char **abuf)
{
  char *p;
  int i;

  if(robot_access) return 0;
  if(strstr(tmp_dir,"sessions/")==NULL) return 0;
  if(strstr(tmp_debug,"yes")!=NULL && checkhost(manager_site)>=1)
    setenv("multiexec_debug","yes",1);
  p=getvar("wims_multiexec");
  if(p==NULL || wordchr(p,cmd)==NULL) return 0; /* not allowed */
  if(!multiexec_random[0]) {
    snprintf(multiexec_random, sizeof(multiexec_random),
           "%lX%lX%lX%lX%lX%lX%lX%lX",
           random(),random(),random(),random(),
           random(),random(),random(),random());
    setenv("multiexec_random",multiexec_random,1);
  }
  for(i=0;i<mxno && strcmp(cmd,mxtab[i].cmd)!=0; i++);
  multiexec_index=i;
  if(i==mxno) {
    if(mxno>=MAX_MULTIEXEC) return 0;
    if(pipe(mxtab[i].pipe_stdin)<0) return 0;
    if(pipe(mxtab[i].pipe_stdout)<0) return 0;
    if(pipe(mxtab[i].pipe_stderr)<0) return 0;
    mystrncpy(mxtab[i].cmd,cmd,sizeof(mxtab[i].cmd));
    mxno++;      is_multiexec=1;
    exportall(); setenv("wims_exec_parm",multiexec_random,1);
    execredirected(abuf[0],NULL,NULL,NULL,abuf);
  }
  is_multiexec=0;
  return 1;
}

/* my system(), but with variable parms
 * More secure than system(), and direct fork.
 */
int call_ssh(char *s,...)
{
  va_list vp;
  char buf[MAX_LINELEN+1];
  char *arg[1024];
  char *inf=NULL, *outf=NULL, *errf=NULL;
  char *cmdf, *p, *p2;
  int i, d;

  if(robot_access) return 0;
  va_start(vp,s);
  vsnprintf(buf,sizeof(buf),s,vp);
  va_end(vp);
  p=find_word_start(buf); if(*p==0) return 0;
  cmdf=p;
  for(i=0;*p!=0 && i<1000; p=find_word_start(p2)) {
    switch(*p) {
      case '\'': {
        p++; p2=strchr(p,'\''); if(p2==NULL) p2=p+strlen(p);
        d=0; break;
      }
      case '"': {
        p++; p2=strchr(p,'"'); if(p2==NULL) p2=p+strlen(p);
        d=0; break;
      }
      default: d=1; p2=find_word_end(p); break;
    }
    if(*p2) *p2++=0;
    if(!d) {arg[i++]=p; continue;}
    switch(*p) {
      case '<': inf=++p; break;
      case '>': {
        p++;
        if(*p=='&') {
          merge: p++; errf=outf=p; break;
        }
        else outf=p;
        break;
      }
      case '&': {
        p++;
        if(*p=='>') goto merge;
        else break;
      }
      case '2': {
        if(*(p+1)=='>') {errf=p+2; break;}
      }
      default: arg[i++]=p; break;
    }
  }
  arg[i]=NULL;
  return execredirected(cmdf,inf,outf,errf,arg);
}

/* Read/write to a file with variable parms to print filename */
void accessfile(char *content, char *type, char *s,...)
{
  va_list vp;
  char buf[MAX_FNAME+1];
  int fd;

  if(robot_access) return;
  va_start(vp,s);
  vsnprintf(buf,sizeof(buf),s,vp);
  va_end(vp);
  if(strlen(buf)>=MAX_FNAME-1) nametoolong(buf);
  switch(*type) {
    case 'r': readfile(buf,content,MAX_LINELEN); return;
    case 'e': readfile(buf,content,MAX_LINELEN/4); return; /* limited read */
    /*case 'w': fd=creat(buf,S_IRUSR|S_IWUSR|S_IRGRP|S_IROTH); break;*/
    case 'w': fd=creat(buf,S_IRUSR|S_IWUSR|S_IRGRP); break;
    case 'a': fd=open(buf,O_WRONLY|O_CREAT|O_APPEND,S_IRUSR|S_IWUSR|S_IRGRP|S_IROTH); break;
    default : return;
  }
  lastdatafile[0]=lastftest[0]=0;
  if(fd==-1) return;
  (void)write(fd,content,strlen(content)); close(fd);
}

/* system(), but with variable parms
 * Uses sh to execute command.
 */
int call_sh(char *s,...)
{
  va_list vp;
  char buf[MAX_LINELEN+1];
  char *abuf[8];

  if(robot_access) return 0;
  va_start(vp,s);
  vsnprintf(buf,sizeof(buf),s,vp);
  va_end(vp);
  abuf[0]="sh"; abuf[1]="-c"; abuf[2]=buf; abuf[3]=NULL;
  return execredirected(abuf[0],NULL,NULL,NULL,abuf);
}

void _getdef(char buf[], char *name, char value[])
{
  char *p1, *p2, *p3, *p4;

  if(*name==0) goto nothing;      /* this would create segfault. */
  for(p1=strstr(buf,name); p1!=NULL; p1=strstr(p1+1,name)) {
    p2=find_word_start(p1+strlen(name));
    if((p1>buf && !isspace(*(p1-1))) || *p2!='=') continue;
    p3=p1; while(p3>buf && *(p3-1)!='\n') p3--;
    p3=find_word_start(p3);
    if(p3<p1 && *p3!='!') continue;
    if(p3<p1) {
      p3++; p4=find_word_end(p3);
      if(find_word_start(p4)!=p1) continue;
      if(p4-p3!=3 || (strncmp(p3,"set",3)!=0 &&
           strncmp(p3,"let",3)!=0 &&
           strncmp(p3,"def",3)!=0)) {
        if(p4-p3!=6 || strncmp(p3,"define",6)!=0) continue;
      }
    }
    p2++;p3=strchr(p2,'\n'); if(p3==NULL) p3=p2+strlen(p2);
    p2=find_word_start(p2);
    if(p2>p3) goto nothing;
    if(p3-p2>=MAX_LINELEN) user_error("cmd_output_too_long");
    memmove(value,p2,p3-p2); value[p3-p2]=0;
    strip_trailing_spaces(value); return;
  }
nothing:
  value[0]=0; return;
}

/* Get variable definition from a file.
 * Result stored in buffer value of length MAX_LINELEN.
 */
void getdef(char *fname, char *name, char value[])
{
  char buf[MAX_LINELEN+1], tbuf[MAX_LINELEN+1], nbuf[MAX_NAMELEN+1];
  char *p1, *p2;

  value[0]=0; if(readfile(fname,buf,sizeof(buf)-16)==NULL) return;
  mystrncpy(value,name,MAX_LINELEN);
  for(p1=value; *p1; p1=p2) {
    while(*p1 && !myisalnum(*p1) && *p1!='_') p1++;
    if(*p1==0) break;
    for(p2=p1; myisalnum(*p2) || *p2=='_'; p2++);
    if(p2-p1>MAX_NAMELEN) continue;
    memmove(nbuf,p1,p2-p1); nbuf[p2-p1]=0;
    _getdef(buf,nbuf,tbuf);
    string_modify(value,p1,p2,"%s",tbuf);
    p2=p1+strlen(tbuf);
  }
}

int _setdef_changed;

void _setdef(char buf[], char *name, char *value)
{
  char *p1, *p2, *p3;
  int n;

  for(p1=strstr(buf,name); p1!=NULL; p1=strstr(p1+1,name)) {
    p2=find_word_start(p1+strlen(name));
    if((p1>buf && !isspace(*(p1-1))) || *p2!='=') continue;
    p3=p1; while(p3>buf && *(p3-1)==' ') p3--;
    if(p3>buf && *(p3-1)!='\n') continue;
    p2++;p3=strchr(p2,'\n'); if(p3==NULL) p3=p2+strlen(p2);
    if(strlen(value)!=p3-p2 || strncmp(value,p2,p3-p2)!=0) {
      string_modify(buf,p2,p3,"%s",value);
      _setdef_changed++;
    }
    return;
  }
  n=strlen(buf);
  if(n>0 && buf[n-1]!='\n')
    snprintf(buf+n,MAX_LINELEN-n,"\n%s=%s\n",name,value);
  else
    snprintf(buf+n,MAX_LINELEN-n,"%s=%s\n",name,value);
  _setdef_changed++;
}

/* Set variable definition to a file. */
void setdef(char *fname, char *name)
{
  char buf[MAX_LINELEN+1];
  char *p1, *p2, *p3;

  _setdef_changed=0;
  if(strchr(name,'=')==NULL) return;
  for(p1=name;*p1;p1++) {
    if(isspace(*p1) && *p1!=' ' && *p1!='\n') *p1=' ';
    if(*p1==' ') {
      for(p2=p1+1; isspace(*p2) && *p2!='\n'; p2++);
      if(p2>p1+1) ovlstrcpy(p1+1,p2);
      p2=p1+1; if(*p2=='=' || *p2=='\n') ovlstrcpy(p1,p2);
    }
  }
  if(readfile(fname,buf,sizeof(buf))==NULL) buf[0]=0;
  for(p1=find_word_start(name); p1!=NULL; p1=p2) {
    p2=strchr(p1,'\n'); if(p2!=NULL) *p2++=0;
    p1=find_word_start(p1);
    p3=strchr(p1,'='); if(p3==NULL) continue;
    *p3++=0; p3=find_word_start(p3);
    _setdef(buf,p1,p3);
  }
  if(_setdef_changed) accessfile(buf,"w","%s",fname);
}

/* check whether connecting host is part of given list.
 * Returns 0 if no, 1 if yes.
 */
int checkhost(char *hlist)
{
  char buf[MAX_LINELEN+1];
  char lbuf[1024], hbuf1[256], hbuf2[256];
  char *p1, *p2, *pb, *pe, *pp;

  if(*remote_addr==0) return 0;
  snprintf(hbuf1,sizeof(hbuf1),"+%s+",remote_addr);
  if(*remote_host!=0) {
    snprintf(hbuf2,sizeof(hbuf2),"+%s+",remote_host);
    for(p1=hbuf2; *p1; p1++) *p1=tolower(*p1);
  }
  else hbuf2[0]=0;
  mystrncpy(buf,find_word_start(hlist),sizeof(buf)); strip_trailing_spaces(buf);
  for(p1=buf; *p1; p1++) {
    *p1=tolower(*p1);
    if(!myisalnum(*p1) && strchr(".-_",*p1)==NULL) *p1=' ';
  }
  if(strcmp(buf,"all")==0) return 1;      /* all is all */
  for(p1=find_word_start(buf); *p1; p1=find_word_start(p2)) {
    p2=find_word_end(p1); if(*p2) *p2++=0;
    if(p2-p1<3) continue;
    if(myisalnum(*p1)) pb="+"; else pb="";
    if(myisalnum(*(p2-1))) pe="+"; else pe="";
    snprintf(lbuf,sizeof(lbuf),"%s%s%s",pb,p1,pe);
    for(pp=p1; *pp && (myisdigit(*pp) || *pp=='.'); pp++);
    if(*pp) pp=hbuf2;      /* host name */
    else pp=hbuf1;         /* ip number */
    if(strstr(pp,lbuf)!=NULL) return 1;      /* found */
  }
  return 0;
}

/* return 1 if a word of bf2 is a substring of host
 * and if time constraints are verified
 * Like checkhost, but check first time then host.
 * The content of bf2[] is destroyed.
 * it is possible to put a joker in the times
 */
int checkhostt(char bf2[])
{
  char *p1, *p2, *p3;
  int i,maxremain=1;
  /* compare with starting time */
  for(p1=strchr(bf2,'>'); p1!=NULL; p1=strchr(p1+1,'>')) {
    if(p1>bf2 && !isspace(*(p1-1))) continue;
    p3=find_word_start(++p1); p2=find_word_end(p3);
    if(p2-p3!=14) continue;
    p3[8]='.'; p3[11]=':'; if(*p2) *p2++=0;
    for (i=0; i<14; i++) if(p3[i]=='*') p3[i]=nowstr[i];
    if(strncmp(nowstr,p3,14)<0) return 0;
    ovlstrcpy(p1-1,p2); p1-=2;
  }
  /* compare with ending time */
  for(p1=strchr(bf2,'<'); p1!=NULL; p1=strchr(p1+1,'<')) {
    if(p1>bf2 && !isspace(*(p1-1))) continue;
    p3=find_word_start(++p1); p2=find_word_end(p3);
    if(p2-p3!=14) continue;
    p3[8]='.'; p3[11]=':'; if(*p2) *p2++=0;
    for (i=0; i<14; i++) if(!isdigit(p3[i])) p3[i]=nowstr[i];
    if(strncmp(nowstr,p3,14)>0) return 0;
/* added to calculate max time in exam */
    if (isexam) {
       struct tm maxtime;
       char tmp[8];
       unsigned long time;
       snprintf(tmp,sizeof(tmp),"%c%c%c%c",p3[0],p3[1],p3[2],p3[3]);
       maxtime.tm_year=atoi(tmp)-1900;
       snprintf(tmp,sizeof(tmp),"%c%c",p3[4],p3[5]);
       maxtime.tm_mon=atoi(tmp)-1;
       snprintf(tmp,sizeof(tmp),"%c%c",p3[6],p3[7]);
       maxtime.tm_mday=atoi(tmp);
       snprintf(tmp,sizeof(tmp),"%c%c",p3[9],p3[10]);
       maxtime.tm_hour=atoi(tmp);
       snprintf(tmp,sizeof(tmp),"%c%c",p3[12],p3[13]);
       maxtime.tm_min=atoi(tmp);
       maxtime.tm_sec=0;
       maxtime.tm_isdst=-1;
       time=mktime(&maxtime);
       if (time<nowtime) maxremain=0; else maxremain=nowtime-time;
   }
/* end add */
    ovlstrcpy(p1-1,p2); p1-=2;
  }
  p1=find_word_start(bf2); if(*p1==0) return maxremain;
  if (checkhost(p1)) return maxremain; else return 0;
}

/* bad identification */
void bad_ident(void)
{
  if(cookiegot[0]!=0) {
  }
  user_error("bad_ident");
}

void instex_flush(void)
{
  char *p;
  setenv("texgif_style","",1);
  setenv("texgif_tmpdir",tmp_dir,1);
  setenv("texgif_src",instex_src,1);
  setenv("texgif_outfile",instex_fname,1);
  unsetenv("w_instex_color");
  getwimstexsize=0; fix_tex_size(); getwimstexsize=1;
  for(p=instex_fname;*p;p++) if(*p=='\n') *p=' ';
  wrapexec=0; call_ssh("%s/%s >%s/ins.Out 2>%s/ins.Err",
                 bin_dir,instex_processor,
                 tmp_dir,tmp_dir);
  call_ssh("mv %s %s >/dev/null 2>/dev/null", instex_fname,s2_prefix);
  instex_src[0]=instex_fname[0]=0; instex_cnt=0;
}

/* put last.phtml */
void putlastout(void)
{
  int t;
  if(instex_cnt>0) instex_flush();
  t=catfile(stdout,"%s/%s",s2_prefix,lastout);
  if(t==0) printf("Content-type: text/plain\r\n\r\n");
}

struct sockaddr_un sun;

/* returns >=0 if OK. */

int kerneld(char *p, int bufsize)
{
  int sock, s, t, t1, l, *ip;
  struct timeval tv;
  fd_set rset;
  sock=socket(PF_UNIX,SOCK_STREAM,0);
  s=connect(sock,(const struct sockaddr *)&sun,sizeof(sun));
  if(s) {bad: close(sock); return -1;}
  ip=(int *) p;
  l=strlen(p+sizeof(int)); *ip=l;
  s=write(sock,p,l+sizeof(int)); if(s!=l+sizeof(int)) goto bad;
  for(t=0, l=bufsize-1; t<l+sizeof(int);) {
    tv.tv_sec=2; tv.tv_usec=0;
    FD_ZERO(&rset); FD_SET(sock,&rset);
    if(select(sock+1,&rset,NULL,NULL,&tv)<=0) goto bad;
    t1=read(sock,p+t,l+sizeof(int)-t);
    if(t1+t<sizeof(int)) goto bad;
    if (t1 < 0) goto bad;
    l=*ip; if(l<=0) goto bad;
    if(l>=bufsize-sizeof(int)-4) user_error("cmd_output_too_long");
    t+=t1;
  }
  p[l+sizeof(int)]=0;
  close(sock);
  return l+sizeof(int);
}

void _daemoncmd(char *p)
{
  char buf[MAX_LINELEN+1+sizeof(int)];
  char *p1, *p2, *p3;
  mystrncpy(buf+sizeof(int),p,sizeof(buf)-sizeof(int));
  if(kerneld(buf,sizeof(buf))<0)
    internal_error("Daemon communication error.");
  p1=find_word_start(buf+sizeof(int));
  p2=find_word_end(p1); if(*p2) *p2++=0;
  if(strcmp(p1,"OK")==0) {
    mystrncpy(p,p2,MAX_LINELEN); return;
  }
  p1=find_word_start(p2); p2=find_word_end(p1); if(*p2) *p2++=0;
  p2=find_word_start(p2); p3=find_word_end(p2);
  if(*p3) {
    *p3++=0; p3=find_word_start(p3); strip_trailing_spaces(p3);
    setvar("wims_error_data",p3);
  }
  switch(*p1) {
    case '1': user_error(p2);
    case '2': module_error(p2);
    case '3':
    default: internal_error(p2);
  }
  *p=0;
}
