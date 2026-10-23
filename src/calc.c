/*    Copyright (C) 1998-2003 XIAO, Gang of Universite de Nice - Sophia Antipolis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/* This is part of wims source.
 * This file does computations and output.
 */
#include "wims.h"

int _sort_numeric, _sort_nocase;
typedef struct SORT_STRUCT {char *str; double val; int serial;} SORT_STRUCT;
struct SORT_STRUCT sort_sep[MAX_SORT_ITEM+1];

void secure_exec(void)
{
  if((untrust&6)==0) return;
  module_error("Illegal_command");
}

/* internal comparers */
int __sorter(const void *p1, const void *p2)
{
  struct SORT_STRUCT *pp1, *pp2;

  pp1=(struct SORT_STRUCT *) p1; pp2=(struct SORT_STRUCT *) p2;
  if(_sort_numeric) {
    double dd=(pp1->val)-(pp2->val);
    if(dd>0) return 1;
    if(dd<0) return -1;
    return 0;
  }
  if(_sort_nocase) return mystrcmp(pp1->str,pp2->str);
  else return strcmp(pp1->str,pp2->str);
}

int _char_sorter(const void *c1, const void *c2)
{
  if(_sort_nocase) return tolower(*(char *)c1)-tolower(*(char *)c2);
  else return *(char *)c1-*(char *)c2;
}

enum {sort_char, sort_word, sort_item, sort_line, sort_row,
      sort_numeric, sort_nocase, sort_reverse
};
struct {char *name; int type;
} sort_keyw[]={
  {"char",sort_char},
  {"chars",sort_char},
  {"character",sort_char},
  {"characters",sort_char},
  {"word",sort_word},
  {"words",sort_word},
  {"item",sort_item},
  {"items",sort_item},
  {"line",sort_line},
  {"lines",sort_line},
  {"list",sort_item},
  {"numeric",sort_numeric},
  {"numerical",sort_numeric},
  {"nocase",sort_nocase},
  {"ignorecase",sort_nocase},
  {"reverse",sort_reverse},
  {"row",sort_row},
  {"rows",sort_row}
};
#define sort_keyw_no (sizeof(sort_keyw)/sizeof(sort_keyw[0]))

/* string sort */
void calc_sort(char *p)
{
  char *p1, *p2, buf[MAX_LINELEN+1], *csep[MAX_SORT_ITEM+1];
  char fs=0;
  int (*cutfn)(char *p,char *list[],int max);
  int nocase=0, reverse=0, numeric=0, type;
  int i,j,l,t,total;

  cutfn=NULL;
  for(i=0,p1=find_word_start(p);i>=0;p1=find_word_start(p2)) {
    p2=find_word_end(p1);
    if(*p2!=0) *p2++=0;
    for(i=0;i<sort_keyw_no && strcasecmp(p1,sort_keyw[i].name)!=0;i++);
    if(i>=sort_keyw_no) module_error("syntax_error");
    switch(type=sort_keyw[i].type) {
      case sort_nocase: nocase=1; break;
      case sort_reverse: reverse=1; break;
      case sort_numeric: numeric=1; break;
      case sort_char: i=-1; break;
      case sort_word: cutfn=cutwords; fs=' '; i=-1; break;
      case sort_item: cutfn=cutitems; fs=','; i=-1; break;
      case sort_row:
      case sort_line: cutfn=cutlines; fs='\n'; i=-1; break;
    }
  }
  if(p1[0]=='o' && p1[1]=='f' && myisspace(p1[2])) p1=find_word_start(p1+2);
  mystrncpy(buf,p1,sizeof(buf)); substit(buf); *p=0;
  t=0; if(type==sort_row) t=rows2lines(buf);
  _sort_nocase=nocase; _sort_numeric=numeric;
  if(cutfn) {
    char ordbuf[MAX_LINELEN+1];
    total=cutfn(buf,csep,MAX_SORT_ITEM+1);
    if(total<=0 || total>MAX_SORT_ITEM) return;
    for(i=0;i<total;i++) {
      sort_sep[i].str=csep[i];
      if(numeric) sort_sep[i].val=evalue(csep[i]);
    }
    for(i=0;i<total;i++) sort_sep[i].serial=i+1;
    qsort(sort_sep,total,sizeof(sort_sep[0]),__sorter);
    p1=p;p2=ordbuf;ordbuf[0]=0;
    for(i=0;i<total;i++) {
      if(reverse) j=total-1-i; else j=i;
      l=strlen(sort_sep[j].str);
      if(p1-p+l>=MAX_LINELEN-1) module_error("cmd_output_too_long");
      if(p1>p) *p1++=fs;
         memmove(p1,sort_sep[j].str,l+1); p1+=l;
      if(i>0) *p2++=',';
      mystrncpy(p2,int2str(sort_sep[j].serial),
             MAX_LINELEN-(p2-ordbuf)-1);
      p2+=strlen(p2);
    }
    force_setvar("wims_sort_order",ordbuf);
  }
  else { /* case of chars */
    qsort(buf,strlen(buf),1,_char_sorter);
    total=strlen(buf);
    if(reverse) {
      for(i=total-1;i>=0;i--) *(p+total-i-1)=buf[i];
      *(p+total)=0;
    }
    else memmove(p,buf,total+1);
  }
  if(t) lines2rows(p);
}

/* Print a debug output */
void calc_debug(char *p)
{
  secure_exec(); setvar("debug",p); module_error("debug");
}

/* execute a command thru wimslogd */
void calc_daemon(char *p)
{
  if(!trusted_module() || is_class_module) {*p=0; return;}
  _daemoncmd(p);
}

void _get_exec_error(char *errorfname, char *cmdname)
{
  char *p2;
  char obuf[MAX_LINELEN+1];
  int l;
  if(errorfname) accessfile(obuf,"e","%s",errorfname);
  else {
    l=read(mxtab[multiexec_index].pipe_stderr[0],obuf,MAX_LINELEN/4);
    if(l<0 || l>MAX_LINELEN) l=0;
    obuf[l]=0;
  }
  if(obuf[0]) {
    p2=find_word_end(obuf);
    if((manageable<1 || strcasecmp(tmp_debug,"yes")!=0) &&
       !trusted_module() &&
        strncmp(p2," not_INStalled",strlen(" not_INStalled"))==0) {
      *p2=0; setvar("missing_software",obuf);
      snprintf(p2+1,MAX_LINELEN-strlen(obuf)-8,"missing_%s",obuf);
      if(!outputing) user_error(p2+1);
    }
    p2=getvar("wims_exec_error"); if(p2==NULL) p2="";
    snprintf(tmplbuf,sizeof(tmplbuf),"%s\nERROR from %s:\n%s",p2,cmdname,obuf);
    force_setvar("wims_exec_error",tmplbuf);
  }
}

/* execute an external program
 * The output of the external program should be put into
 * a file session_directory/session/cmd.tmp.
 */
void calc_exec(char *p)
{
  int i,j,k;
  char *cmd, *parm, *pp;
  char namebuf[MAX_EXEC_NAME+1], cmdstr[MAX_LINELEN+256], obuf[MAX_LINELEN+1];
  char outfname[MAX_FNAME+1], errorfname[MAX_FNAME+1],
    typefname[MAX_FNAME+1], varfname[MAX_FNAME+1];
  char *abuf[2]={NULL,NULL};
  WORKING_FILE wf;
  if(robot_access || time(0)>=limtimex-1) {
    *p=0;return;
  }
  cmd=find_word_start(p); if(*cmd==0) return; /* No command. */
  parm=find_word_end(cmd);j=parm-cmd;parm=find_word_start(parm);
  if(j>MAX_EXEC_NAME) {
    wrong_command:
    setvar(error_data_string,namebuf); module_error("bad_cmd");
    *p=0; return; /* should not occur */
  }
  memmove(namebuf,cmd,j); namebuf[j]=0;
/* Specifying parent directory in command name is of course
 * prohibited.
 * The module developper cannot start from root, for bin_dir
 * will be prefixed to cmd.
 */
  if(strstr(namebuf,parent_dir_string)!=NULL) {
    setvar(error_data_string,namebuf); module_error("illegal_cmd");
    *p=0; return;
  }
  snprintf(cmdstr,sizeof(cmdstr),"%s/%s",bin_dir,namebuf);
  if(ftest(cmdstr)!=is_exec) goto wrong_command;
  abuf[0]=cmdstr;
  mkfname(outfname,"%s/exec.out",tmp_dir);
  mkfname(errorfname,"%s/exec.err",tmp_dir);
  mkfname(typefname,"%s/exec.type",tmp_dir);
  unlink(typefname);
  mkfname(varfname,"%s/exec.var",tmp_dir);
  unlink(varfname);
  if(!trusted_module()) setenv("untrust","yes",1); else unsetenv("untrust");
  if(strcmp(parm,"about")!=0 && multiexec(namebuf,abuf)) {
    fd_set rset;
    struct timeval t;
    int v1, v2, fd;

    i=strlen(parm);
    fd=mxtab[multiexec_index].pipe_stdin[1];
    (void)write(fd,parm,i);
    (void)write(fd,multiexec_random,strlen(multiexec_random));
    *p=0; i=0; k=MAX_LINELEN; fd=mxtab[multiexec_index].pipe_stdout[0];
    while(k>0) {
      FD_ZERO(&rset); FD_SET(fd,&rset);
      t.tv_sec=0; t.tv_usec=100000; v1=0; v2=300; /* 3 seconds */
      if(select(fd+1,&rset,NULL,NULL,&t)<=0) {
        v1++; if(v1>=v2) break;
      else continue;
      }
      j=read(fd,p+i,k);
      if(j<0 || j>k) break;
      i+=j; k-=j; p[i]=0;
      pp=strstr(p,multiexec_random);
      if(pp) {*pp=0; break;}
    }
    fd=mxtab[multiexec_index].pipe_stderr[0];
    FD_ZERO(&rset); FD_SET(fd,&rset);
    t.tv_sec=0; t.tv_usec=0;
    if(select(fd+1,&rset,NULL,NULL,&t)>0)
      _get_exec_error(NULL,namebuf);
    strip_trailing_spaces(p);
    return;
  }
  exportall(); setenv("wims_exec_parm",parm,1);
  execredirected(cmdstr,NULL,outfname,errorfname,abuf);
  if(open_working_file(&wf,varfname)==0) {
    char *pt, *pv;
    while(wgetline(tmplbuf,MAX_LINELEN, &wf)!=EOF) {
      pt=find_word_start(tmplbuf); pv=strchr(pt,'=');
      if(pv==NULL || pv<=tmplbuf) continue;
      *pv=0; pv=find_word_start(++pv);
      *find_word_end(pt)=0;
      strip_trailing_spaces(pv);
      setvar(pt,pv);
    }
    close_working_file(&wf,0);
  }
  read_tmp_file(p,"exec.out");
  read_tmp_file(obuf,"exec.type");
  if(obuf[0]) k=atoi(obuf); else k=0;
  for(i=2;i<=k && i<256;i++) {
    char nbuf[32];
    snprintf(nbuf,sizeof(nbuf),"exec.out.%d",i);
    read_tmp_file(obuf,nbuf);
    if(obuf[0]) {
      snprintf(nbuf,sizeof(nbuf),"wims_exec_out_%d",i);
      force_setvar(nbuf,obuf);
    }
  }
  strip_trailing_spaces(p);
  _get_exec_error(errorfname,namebuf);
  return;
}

/* execute external program in the module directory.
 * For privileged modules only
 */
void calc_mexec(char *p)
{
  char *public_bin;
  if(robot_access) return;
  if(trusted_module()!=1 || is_class_module) {
    module_error("not_trusted"); *p=0; return;
  }
/* The following is useless, because mexec content is often given
 * by variables
 */
/*    if(strstr(p,PARENT_DIR_STRING)!=NULL) {
     setvar("wims_error_data",p);
     module_error("illegal_fname"); *p=0; return;
    }
*/    public_bin=bin_dir; bin_dir=module_prefix;
  exec_is_module=1;
  calc_exec(p); bin_dir=public_bin;
  exec_is_module=0;
}

void _calc_exec(char *p, char *arg0, char *arg1, int n)
{
  char *abuf[8];
  char outfname[MAX_FNAME+1], errorfname[MAX_FNAME+1];

  if(robot_access) {*p=0; return;}
  if(!trusted_module() || is_class_module) {
    if(strcasecmp(tmp_debug,"yes")==0)
      accessfile(p,"w","%s/%s.cmd",tmp_dir,arg0);
    abuf[0]="bin/ch..root";
    abuf[1]="&"; abuf[2]=arg0; abuf[3]=p;
    abuf[4]=NULL;
  }
  else {
/* if(strstr(p,PARENT_DIR_STRING)!=NULL) {
         setvar("wims_error_data",p);
         module_error("illegal_fname"); *p=0; return;
     }
*/
      abuf[0]=arg0; abuf[1]=arg1; abuf[n]=p; abuf[n+1]=NULL;
  }
  mkfname(outfname,"%s/exec.out",tmp_dir);
  mkfname(errorfname,"%s/exec.err",tmp_dir);
  wrapexec=1; exportall();
  execredirected(abuf[0],NULL,outfname,errorfname,abuf);
  read_tmp_file(p,"exec.out");
  _get_exec_error(errorfname,arg0);
}

/* call shell. */
void calc_sh(char *p)
{
  _calc_exec(p,"sh","-c",2);
}

/* call perl. */
void calc_perl(char *p)
{
  _calc_exec(p,"perl","-e",2);
}

/* simple evaluation of string */
void calc_evalue(char *p)
{
  double d;
  d=evalue(p); float2str(d,p); return;
}

/* substitute math variables */
void calc_mathsubst(char *p)
{
  char *expr, *val, *nam, *pp;
  char buf[MAX_LINELEN+1];
  expr=wordchr(p,"in"); if(expr==NULL) goto error;
  ovlstrcpy(buf,find_word_start(expr+strlen("in")));substit(buf);
  *expr=0; substit(p);
  val=strchr(p,'=');
  if(val==NULL) {
    error:
    module_error("mathsubst_syntax"); *p=0;return;
  }
  nam=find_word_start(p); *val=0;
  val=find_word_start(val+1);
  for(pp=val+strlen(val)-1;pp>=val && isspace(*pp);*pp--=0);
  *find_word_end(nam)=0;
  if(*nam==0) goto error;
  if(*nam) for(pp=varchr(buf,nam);pp!=NULL;pp=varchr(pp,nam)) {
    string_modify(buf,pp,pp+strlen(nam),"%s",val);
    pp+=strlen(val);
  }
  ovlstrcpy(p,buf);
}

/* substitute and evaluate. */
void calc_evalsubst(char *p)
{
  calc_mathsubst(p);
  calc_evalue(p);
}

/* Nothing needs to be done in the function;
 * substitution is done or not by tag in the structure.
 */
void calc_subst(char *p)
{
}

int _randrep(char *p)
{
  char *rep;
  int i;
  rep=wordchr(p,"repeat"); if(rep==NULL) return 1;
  *rep=0; rep+=strlen("repeat"); i=evalue(rep);
  if(i<1) i=1;
  if(i>MAX_VALUE_LIST) i=MAX_VALUE_LIST;
  return i;
}

/* integer random */
void calc_randint(char *p)
{
  int lbound, ubound, n, i, t;
  char *p1, *parm[2];

  t=_randrep(p); n=cutitems(p,parm,2);
  if(n<=0) {
    snprintf(p,3,"0"); return; /* Random without bound: return 0. */
  }
  lbound=evalue(parm[0]);
/* Missing ubound: random between +-1 and lbound. */
  if(n<=1) {if(lbound<0) ubound=-1; else ubound=1;}
  else ubound=evalue(parm[1]);
  if(lbound>ubound) {i=lbound; lbound=ubound; ubound=i;}
  for(i=0,p1=p;i<t;i++) {
    if(i>0) *p1++=',';
    mystrncpy(p1,int2str((int) irand(ubound-lbound+1)+lbound),
            MAX_LINELEN-(p1-p)-1);
    p1+=strlen(p1);
  }
  return;
}

/* floating random */
void calc_randdouble(char *p)
{
  double lbound, ubound;
  int n, i, t;
  char *parm[2], *p1;

  t=_randrep(p); n=cutitems(p,parm,2);
  if(n<=0) {
    snprintf(p,3,"0"); return; /* Random without bound: return 0. */
  }
  lbound=evalue(parm[0]);
  if(n<=1) ubound=0; /* Missing ubound: random between 0 and lbound. */
  else ubound=evalue(parm[1]);
  for(i=0,p1=p;i<t;i++) {
    float2str(drand(ubound-lbound)+lbound,tmplbuf);
    mystrncpy(p1,tmplbuf,MAX_LINELEN-(p1-p));
    p1+=strlen(p1);
    if(i<t-1 && p1-p<MAX_LINELEN) *p1++=',';
  }
  *p1=0; return;
}

/* Takes randomly a record in a datafile */
void calc_randfile(char *p)
{
  char *pp, n[MAX_FNAME+1];
  int i, j;

  pp=find_word_start(p); *find_word_end(pp)=0;
  mystrncpy(n,pp,sizeof(n));
  i=datafile_recordnum(pp);
  if(i<=0) {  /* zero records */
    *p=0; return;
  }
  j=irand(i); datafile_fnd_record(n,j+1,p);
  return;
}

/* random char, word, line, item in a string */
void calc_randchar(char *p)
{
  p[0]=p[(int) irand(strlen(p))];p[1]=0;
}

void calc_randitem(char *p)
{
  int i;
  i=itemnum(p);
  (void) fnd_item(p,irand(i)+1,tmplbuf);
  mystrncpy(p,tmplbuf,MAX_LINELEN);
}

void calc_randline(char *p)
{
  int i;
  i=linenum(p); fnd_line(p,irand(i)+1,tmplbuf);
  mystrncpy(p,tmplbuf,MAX_LINELEN);
}

void calc_randrow(char *p)
{
  rows2lines(p); calc_randline(p);
}

void calc_randword(char *p)
{
  int i;
  i=wordnum(p); fnd_word(p,irand(i)+1,tmplbuf);
  mystrncpy(p,tmplbuf,MAX_LINELEN);
}

/* random permutation of {1,...,n} */
void calc_randperm(char *p)
{
  int n, i, j, k, t, pt, type;
  double v;
  char buf1[MAX_LINELEN+1],buf2[MAX_LINELEN+1];
  char *list[MAX_RANDPERM];
  int table[MAX_RANDPERM];
  char *p1,*p2,*pp;

  t=0; pt=0; pp=p;
  p1=find_word_start(p); p2=find_word_end(p1);
  if(p2-p1==strlen("even") && strncasecmp(p1,"even",strlen("even"))==0) {
    t=1; pp=p2;
  }
  if(p2-p1==strlen("odd") && strncasecmp(p1,"odd",strlen("odd"))==0) {
    t=-1; pp=p2;
  }

  p1=find_item_end(pp);
  if(*p1==',') {
    type=1; n=cutitems(pp,list,MAX_RANDPERM); v=0;
  }
  else {
    v=evalue(pp); n=v; type=0;
  }
  if(n==1 && !isfinite(v)) {ovlstrcpy(p,pp); goto shorder;}
  if(n<=0) {*p=0; return;}
  if(n==1 && type==0) {
    ovlstrcpy(p,"1");
    shorder: force_setvar("wims_shuffle_order","1"); return;
  }
  if(n>MAX_RANDPERM) n=MAX_RANDPERM;
  for(i=0;i<n;i++) table[i]=i;
/* Uniformity of this algorithm is easy to show by induction. */
  for(i=0;i<n-1;i++) {
    j=irand(n-i)+i; if(j==i) continue;
    k=table[i]; table[i]=table[j]; table[j]=k;
    pt++;
  }
  pt&=1;
  if((t==1 && pt==1) || (t==-1 && pt==0)) {
    k=table[0]; table[0]=table[1]; table[1]=k;
  }
  if(type) {mystrncpy(buf1,list[table[0]],MAX_LINELEN); p1=buf1+strlen(buf1);}
  else buf1[0]=0;
  mystrncpy(buf2,int2str(table[0]+1),MAX_LINELEN); p2=buf2+strlen(buf2);
  for(i=1;i<n;i++) {
    if(type) {
      j=strlen(list[table[i]]);
      if(p1-buf1+j>=MAX_LINELEN-1) module_error("cmd_output_too_long");
      *p1++=','; memmove(p1,list[table[i]],j+1); p1+=j;
    }
    *p2++=',';
    mystrncpy(p2,int2str(table[i]+1), MAX_LINELEN-(p2-buf2)-1);
    p2+=strlen(p2);
  }
  if(type) mystrncpy(p,buf1,MAX_LINELEN);
  else mystrncpy(p,buf2,MAX_LINELEN);
  force_setvar("wims_shuffle_order",buf2);
}

/* Computes number of lines in the parm. */
void calc_linenum(char *p)
{
  int i=linenum(p); mystrncpy(p,int2str(i),MAX_LINELEN);
}

/* Computes number of lines in the parm. */
void calc_rownum(char *p)
{
  int i=rownum(p); mystrncpy(p,int2str(i),MAX_LINELEN);
}

/* Computes number of items in the list p. */
void calc_itemnum(char *p)
{
  int i=itemnum(p); mystrncpy(p,int2str(i),MAX_LINELEN);
}

/* Computes number of records in the datafile p. */
void calc_recordnum(char *p)
{
  int i;
  i=datafile_recordnum(p); mystrncpy(p,int2str(i),MAX_LINELEN);
}

/* Computes number of words in the parm. */
void calc_wordnum(char *p)
{
  int i=wordnum(p); mystrncpy(p,int2str(i),MAX_LINELEN);
}

/* string length */
void calc_lengthof(char *p)
{
  int i;
/* Strip leading and trailing spaces? */
  i=strlen(p); mystrncpy(p,int2str(i),MAX_LINELEN);
}

char *_blockof_one(char *buf,
            unsigned int len,
            char *(fnd_fn)(char *pl, int n, char bbuf[]),
            int i, char bbuf[])
{
  if(i<0 || i>len || (i==0 && fnd_fn!=datafile_fnd_record) ) {
    bbuf[0]=0; return bbuf;
  }
  return fnd_fn(buf,i,bbuf);
}

void _blockof(char *p,
           unsigned int (len_fn)(char *pl),
           char *(fnd_fn)(char *pl, int n, char bbuf[]),
           char *append_char,
           char *msg_str)
{
  int i,j,l,started;
  char *pp, *pe, *po, *pnext;
  char buf[MAX_LINELEN+1], obuf[MAX_LINELEN+1], bbuf[MAX_LINELEN+1];
  pp=wordchr(p,"of");
  if(pp==NULL) {
    setvar(error_data_string,msg_str);
    module_error("no_of"); *p=0; return;
  }
  *pp=0; pp=find_word_start(pp+strlen("of")+1);
  mystrncpy(buf,pp,sizeof(buf)); substit(buf);
  obuf[0]=0; started=0; po=obuf;
  pp=strparstr(p,"to");
  while(*pp!=0 && ((pp>p && !isspace(*(pp-1))) || !isspace(*(pp+2))))
    pp=strparstr(pp+2,"to");
  if(*pp==0) pp=strparstr(p,"..");
  if(*pp!=0) {
    int t=len_fn(buf);
    *pp=0; pe=find_word_start(pp+2);
    i=evalue(p); j=evalue(pe); pnext=buf;
    if(i<0) i=t+i+1;
    if(i<1) i=1;
    if(j<0) j=t+j+1;
    if(j>t) j=t;
    for(; i<=j; i++) {
      if(started==0 || fnd_fn==datafile_fnd_record)
        pp=_blockof_one(buf,t,fnd_fn,i,bbuf);
      else
        pp=_blockof_one(pnext,1,fnd_fn,1,bbuf);
      l=strlen(pp); pnext=fnd_nextpos;
      if(l+(po-obuf)>=MAX_LINELEN-3) {
        too_long: user_error("cmd_output_too_long"); return;
      }
      if(started) {ovlstrcpy(po,append_char); po+=strlen(po);}
      memmove(po,pp,l); po+=l; *po=0; started++;
    }
  }
  else {
    char *p1,*p2,ibuf[MAX_LINELEN+1];
    int t=len_fn(buf);
    mystrncpy(ibuf,p,sizeof(ibuf)); substit(ibuf);
    for(p1=ibuf;*p1;p1=find_word_start(p2)) {
      p2=find_item_end(p1); if(*p2) *p2++=0;
      i=evalue(p1);
      if(i<0) i=t+i+1;
      if(i>t || i<0) continue;
      pp=_blockof_one(buf,t,fnd_fn,i,bbuf);l=strlen(pp);
      if(l+(po-obuf)>=MAX_LINELEN-3) goto too_long;
      if(started) {ovlstrcpy(po,append_char); po+=strlen(po);}
      memmove(po,pp,l); po+=l; *po=0; started++;
    }
  }
  mystrncpy(p,obuf,MAX_LINELEN);
}

/* pick lines */
void calc_lineof(char *p)
{
  _blockof(p,linenum,fnd_line,"\n","line");
}

/* pick rows */
void calc_rowof(char *p)
{
  char *cc, tbuf[MAX_LINELEN+1]; /* called by substit(); no right to use tmplbuf */
  mystrncpy(tbuf,p,sizeof(tbuf)); substit(tbuf);
  if(strchr(tbuf,'\n')==NULL && strchr(tbuf,';')!=NULL) cc=";";
  else cc="\n";
  _blockof(p,rownum,fnd_row,cc,"row");
}

/* pick items */
void calc_itemof(char *p)
{
  _blockof(p,itemnum,fnd_item,", ","item");
}

/* pick records in datafile */
void calc_recordof(char *p)
{
  _blockof(p,datafile_recordnum,datafile_fnd_record,"\n:","record");
}

/* pick words */
void calc_wordof(char *p)
{
  _blockof(p,wordnum,fnd_word," ","word");
}

/* pick characters */
void calc_charof(char *p)
{
  _blockof(p,charnum,fnd_char,"","char");
}

char *append_obj[]={
  "item","line","word","semicolon","colon"
};
char apch_list[]={',','\n',' ',';',':'};
#define append_obj_no (sizeof(append_obj)/sizeof(append_obj[0]))

/* append object to string */
void calc_append(char *p)
{
  char append_char, *p1,*p2,*p3,*p4;
  int i,l1,l2;
  p1=find_word_start(p);p2=find_word_end(p1);
  if(*p2!=0) *p2++=0;
  for(i=0;i<append_obj_no && strcmp(p1,append_obj[i])!=0;i++);
  if(i>=append_obj_no) {
    synterr:
    module_error("append_syntax");
    *p=0; return;
  }
  append_char=apch_list[i];
  p3=wordchr(p2,"to");
  if(p3==NULL) goto synterr;
  for(p4=p3-1;p4>p2 && isspace(*(p4-1));p4--);
  if(p4<=p2) goto synterr;
  *p4=0;p3=find_word_start(p3+strlen("to"));
  memmove(tmplbuf,p2,p4-p2); tmplbuf[p4-p2]=0; ovlstrcpy(p,p3);
  substit(tmplbuf);substit(p);
  l1=strlen(p); l2=strlen(tmplbuf);
  if(l1+l2>=MAX_LINELEN-1) user_error("cmd_output_too_long");
  p3=find_word_start(p); p4=p+l1;
  if(*p3) *p4++=append_char;
  memmove(p4,tmplbuf,l2); p4[l2]=0;
}

/* character translation */
void calc_translate(char *p)
{
  int i, internal;
  char *q[3];
  char bf[3][MAX_LINELEN+1];
  char fn[3][MAX_FNAME+1], *arglist[8];

  q[0]=find_word_start(p); internal=0;
  if(strncasecmp(q[0],"internal",strlen("internal"))==0 &&
     isspace(*(q[0]+strlen("internal")))) {
    q[0]=find_word_start(q[0]+strlen("internal"));
    internal=1;
  }
  q[1]=wordchr(q[0],"to"); q[2]=wordchr(q[0],"in");
  if(q[1]==NULL || q[2]==NULL) {
    module_error("tr_syntax"); *p=0; return;
  }
  *q[1]=0; *q[2]=0;
  q[1]=find_word_start(q[1]+strlen("to"));
  q[2]=find_word_start(q[2]+strlen("in"));
  for(i=0;i<3;i++) {
    strip_trailing_spaces(q[i]);
    mystrncpy(bf[i],q[i],sizeof(bf[i]));substit(bf[i]);
  }
  if(bf[0][0]==0) goto bailout;
  if(internal || strstr(tmp_dir,"sessions")==NULL || (strpbrk(bf[0],"\\[-")==NULL &&
     strpbrk(bf[1],"\\[-")==NULL)) { /* direct internal translation */
    char *pp;
    if(strlen(bf[1])<strlen(bf[0])) bf[0][strlen(bf[1])]=0;
    for(pp=strpbrk(bf[2],bf[0]);pp!=NULL && *pp!=0 && pp<bf[2]+MAX_LINELEN;
       pp=strpbrk(pp+1,bf[0])) {
      for(i=0;bf[0][i]!=*pp && bf[0][i]!=0;i++);
      if(bf[0][i]!=0) *pp=bf[1][i];
    }
    bailout: mystrncpy(p,bf[2],MAX_LINELEN);
    return;
  }
  mkfname(fn[0],"%s/tr.in",tmp_dir);
  mkfname(fn[1],"%s/tr.out",tmp_dir);
  accessfile(bf[2],"w",fn[0]);
  arglist[0]=tr_prog; arglist[1]=bf[0]; arglist[2]=bf[1];
  arglist[3]=NULL; exportall();
  execredirected(tr_prog,fn[0],fn[1],"/dev/null",arglist);
  read_tmp_file(p,"tr.out");
  strip_trailing_spaces(p);
}

/* internal common routine for positionof */
void _pos(char *hay, char *stitch, int style, char *out,
       char *(fnd_obj)(char *p, int n, char bf[]),
       unsigned int (objnum)(char *p))
{
  int i,n;
  char buf[MAX_LINELEN+1];
  char *po;
  n=objnum(hay);
  po=out+strlen(out);
  for(i=1;i<=n;i++) {
    fnd_obj(hay,i,buf);
    if(strcmp(buf,stitch)!=0) continue;
    if(po-out>MAX_LINELEN-12) return;
    if(po>out) *po++=',';
    po+=snprintf(po,(size_t)(MAX_LINELEN-(po-out)),"%d",i);
  }
}

/* return positions of searched for objects */
void calc_pos(char *p)
{
  char buf[2][MAX_LINELEN+1];
  char *p1, *p2;
  int  style;

  p1=find_word_start(p); p2=wordchr(p1,"in");
  if(p2==NULL) module_error("syntax_error");
  *p2=0;p2=find_word_start(p2+strlen("in"));
  strip_trailing_spaces(p1);
  ovlstrcpy(buf[0],p1);*find_word_end(buf[0])=0; style=0;
  if(strcmp(buf[0],"word")==0) style=1;
  else {
    if(strcmp(buf[0],"item")==0) style=2;
    else {
      if(strcmp(buf[0],"line")==0) style=3;
      else {
        if(strcmp(buf[0],"char")==0) style=4;
        else {
        if(strcmp(buf[0],"row")==0) style=5;
        }
      }
    }
  }
  if(style>0) p1=find_word_start(find_word_end(p1));
  ovlstrcpy(buf[0],p1); ovlstrcpy(buf[1],p2);
  substit(buf[0]); substit(buf[1]); *p=0;
  switch(style) {
    case 0: {  /* string */
      char *pp, *po=p;
      int i;
      for(pp=strstr(buf[1],buf[0]);pp!=NULL;pp=strstr(pp+1,buf[0])) {
        i=pp-buf[1];
        if(po-p>MAX_LINELEN-12) return;
        if(po>p) *po++=',';
        po+=snprintf(po,(size_t)(MAX_LINELEN-(po-p)),"%d",i);
      }
      return;
    }
    case 1: { /* word */
      _pos(buf[1],buf[0],style,p,fnd_word,wordnum);
      return;
    }
    case 2: { /* item */
      _pos(buf[1],buf[0],style,p,fnd_item,itemnum);
      return;
    }
    case 3: { /* line */
      _pos(buf[1],buf[0],style,p,fnd_line,linenum);
      return;
    }
    case 4: { /* char */
      _pos(buf[1],buf[0],style,p,fnd_char,charnum);
      return;
    }
    case 5: { /* row */
      _pos(buf[1],buf[0],style,p,fnd_row,rownum);
      return;
    }
  }
}

/* internal routine for calc_replace. */
void _obj_replace(char *orig, char *by, char *in, int num,
            char separator, char *result,
            char *(fnd_obj)(char *p, int n, char bf[]),
            unsigned int (objnum)(char *p),
            char *(objchr)(char *p,char *w))
{
  int i;
  char *p1, *p2;

  ovlstrcpy(result,in);
  if(num!=0) {
    num=objnum(in); i=evalue(orig);
    if(i==0) module_error("bad_index");
    if(i<0) i=num+i+1;
    if(i>num || i<1) return;
    if(separator==0) {  /* char */
      result[i-1]=by[0]; return;
    }
    fnd_obj(result,i,orig); p1=fnd_position;
    if(i<num) {
      fnd_obj(result,i+1,orig); p2=fnd_position;
    }
    else p2=result+strlen(result);
    if(p1==NULL || p2==NULL) internal_error("_obj_replace() error.");
    if(i<num) {
      i=strlen(by); by[i++]=separator;by[i]=0;
    }
    string_modify(result,p1,p2,"%s",by);
  }
  else {
    if(separator==0) {
      if(orig[0]==0 || by[0]==0) return;
      for(p1=strchr(result,orig[0]);p1!=NULL;p1=strchr(p1+1,orig[0]))
        *p1=by[0];
      return;
    }
    if(strlen(orig)+strlen(by)==0) return;
    for(p1=objchr(result,orig);p1!=NULL;p1=objchr(p1+strlen(by)+1,orig)) {
         string_modify(result,p1,p1+strlen(orig),"%s",by);
    }
  }
}

/* replacement */
void calc_replace(char *p)
{
  int i,style,num,internal;
  char *q[3], *pp;
  char bf[4][MAX_LINELEN+17];
  char fn[3][MAX_FNAME+1], *arglist[8];
  char regexp_char='/';

  style=num=0;
  q[0]=find_word_start(p); internal=0;
  if(strncasecmp(q[0],"internal",strlen("internal"))==0 &&
     isspace(*(q[0]+strlen("internal")))) {
    q[0]=find_word_start(q[0]+strlen("internal"));
    internal=1;
  }
  q[1]=wordchr(q[0],"by"); q[2]=wordchr(q[0],"in");
  if(q[1]==NULL || q[2]==NULL) {
    module_error("replace_syntax"); *p=0; return;
  }
  *q[1]=0; *q[2]=0;
  q[1]=find_word_start(q[1]+strlen("by"));
  q[2]=find_word_start(q[2]+strlen("in"));
  mystrncpy(bf[0],q[0],sizeof(bf[0]));
  pp=find_word_end(bf[0]); if(*pp) *(pp++)=0;
  if(strcmp(bf[0],"word")==0) style=1;
  else {
    if(strcmp(bf[0],"item")==0) style=2;
    else {
      if(strcmp(bf[0],"line")==0) style=3;
      else {
        if(strcmp(bf[0],"char")==0) style=4;
      }
    }
  }
  if(style>0) {
    q[0]=find_word_start(find_word_end(q[0]));
    mystrncpy(bf[0],q[0],sizeof(bf[0]));
    pp=find_word_end(bf[0]); if(*pp) *(pp++)=0;
    if(strcmp(bf[0],"number")==0) {
      num=1; q[0]=find_word_start(pp);
    }
  }
  for(i=0;i<3;i++) {
    strip_trailing_spaces(q[i]);
    mystrncpy(bf[i],q[i],sizeof(bf[i]));
    substit(bf[i]);
  }
  if(bf[0][0]==0) {mystrncpy(p,bf[2],MAX_LINELEN); return;}
  switch(style) {
    case 1: { /* word */
      _obj_replace(bf[0],bf[1],bf[2],num,' ',p,
              fnd_word,wordnum,wordchr);
      return;
    }
    case 2: { /* item */
      _obj_replace(bf[0],bf[1],bf[2],num,',',p,
              fnd_item,itemnum,itemchr);
      return;
    }
    case 3: { /* line */
      _obj_replace(bf[0],bf[1],bf[2],num,'\n',p,
              fnd_line,linenum,linechr);
     return;
    }
    case 4: { /* char */
      if(bf[1][0]==0) bf[1][0]=' ';
      if(bf[0][0]==0) return;
      _obj_replace(bf[0],bf[1],bf[2],num,0,p,
              fnd_char,charnum,charchr);
      return;
    }
    default: break;
  }
  if(internal || strstr(tmp_dir,"sessions")==NULL || (strpbrk(bf[0],"\\[^.*$")==NULL &&
       strpbrk(bf[1],"\\[^.*$")==NULL)) {
/* No regexp, direct replace */
    char *pp;
    for(pp=strstr(bf[2],bf[0]);pp<bf[2]+MAX_LINELEN && pp!=NULL;
       pp=strstr(pp+strlen(bf[1]),bf[0])) {
      string_modify(bf[2],pp,pp+strlen(bf[0]),"%s",bf[1]);
    }
    mystrncpy(p,bf[2],MAX_LINELEN);return;
  }
  mkfname(fn[0],"%s/sed.in",tmp_dir);
  mkfname(fn[1],"%s/sed.out",tmp_dir);
  accessfile(bf[2],"w",fn[0]);
  snprintf(bf[3],sizeof(bf[3]),"s%c%s%c%s%cg",
          regexp_char,bf[0],regexp_char,bf[1],regexp_char);
  arglist[0]=sed_prog; arglist[1]=bf[3]; arglist[2]=NULL;
  execredirected(sed_prog,fn[0],fn[1],"/dev/null",arglist);
  read_tmp_file(p,"sed.out");
  strip_trailing_spaces(p);
}

/* transforms a string to hexadecimal code, upper-case */
void calc_hex(char *p)
{
  unsigned char *p1, orig[MAX_LINELEN+1];
  char *p2;
  char *hex="0123456789ABCDEF";

  mystrncpy((char*)orig,p,MAX_LINELEN);
  for(p1=orig, p2=p; *p1 && p2-p<MAX_LINELEN-2; p1++) {
    *p2++=hex[(*p1>>4)&15]; *p2++=hex[*p1&15];
  }
  *p2=0;
}

/* transforms to lower/upper cases */
void calc_tolower(char *p)
{
  char *pp;
  for(pp=p;*pp;pp++) *pp=tolower(*pp);
}

void calc_toupper(char *p)
{
  char *pp;
  for(pp=p;*pp;pp++) *pp=toupper(*pp);
}

/* strip leading and trailing spaces */
void calc_trim(char *p)
{
  char *s;
  s=find_word_start(p);
  if(s>p) memmove(p,s,MAX_LINELEN-(s-p)+1);
  strip_trailing_spaces(p);
}

/* output date. Uses Linux 'date' utility. */
void calc_date(char *p)
{
  char *p1, *p2, *p3;
  if(!trusted_module() || is_class_module) {
    if(strstr(p,"..")!=NULL) return;
    for(p1=find_word_start(p); *p1; p1=find_word_start(p2)) {
      p2=find_word_end(p1);
      while(*p1=='\'' || *p1=='"') p1++;
      if(*p1!='-') continue;
      for(p3=p1+1;p3<p2;p3++) if(strchr("rs",*p3)!=NULL) return;
    }
  }
  wrapexec=1;
  call_ssh("date %s >%s/date.out 2>/dev/null",p,tmp_dir);
  read_tmp_file(p,"date.out");
}

/* ls, or dir */
void calc_listfile(char *p)
{
  char *pp;

/* only for trusted modules */
  if(!trusted_module() || is_class_module) {
    module_error("not_trusted"); *p=0; return;
  }
/* security measures. */
  for(pp=p;*pp;pp++) if(isspace(*pp) || *pp==';') *pp=' ';
  if(strstr(p,parent_dir_string)!=NULL) {
    setvar(error_data_string,p);
    module_error("illegal_fname"); return;
  }
  wrapexec=1;
  call_sh("ls %s >%s/ls.out 2>%s/ls.err",
         p,tmp_dir,tmp_dir);
  read_tmp_file(p,"ls.out");
}

void calc_fileexists(char *p)
{
  char *pp, *fname;
/* security measures. */
  for(pp=p;*pp;pp++) if(isspace(*pp) || *pp==';') *pp=' ';
  if(strstr(p,parent_dir_string)!=NULL) {
    setvar(error_data_string,p);
    module_error("illegal_fname"); return mystrncpy(p,"no",MAX_LINELEN);
  }
  if(strncmp(p,"wimshome/",9)==0){
    if(robot_access || !trusted_module() || is_class_module){
      module_error("illegal_fname"); return mystrncpy(p,"no",MAX_LINELEN);
    }
    else
    {
      fname=strchr(p,'/');
      if(ftest(mkfname(NULL,"%s/%s",getvar("wims_home"),fname))==is_file)
        return mystrncpy(p,"yes",MAX_LINELEN);
    }
  }
  if(ftest(mkfname(NULL,"%s",p))==is_file) return mystrncpy(p,"yes",MAX_LINELEN);
  else return mystrncpy(p,"no",MAX_LINELEN);
}

/* instex static: static tex inserts */
void calc_instexst(char *p)
{
  char nbuf[MAX_FNAME+1], bufc[MAX_LINELEN+1];
  char buf2[1024], altbuf[1024], buf[MAX_LINELEN+1], urlbuf[MAX_LINELEN+1];
  char *b, *at, *al, *md1, *md2;
  int t, border, vspace;
  struct stat st,stc;
  char *p1, *p2, *p3, *ppp, *pt;

  if(robot_access) {*p=0; return;}
  p1=find_word_start(p); p2=find_word_end(p1);
  p3=p2-4; vspace=0;
  fix_tex_size();
  t=untrust; untrust=0;
  if(find_module_file(m_file.name,bufc,0)) module_error(m_file.name);
  else stat(bufc,&stc);
  untrust=t;
  if(*p3=='.' && (memcmp(p3+1,"gif",3)==0 || memcmp(p3+1,"png",3)==0)) {
    char mbuf[MAX_LINELEN+1];
    if(*p2!=0) *p2++=0;
    p2=find_word_start(p2);
    ovlstrcpy(mbuf,p1); substit(mbuf);
    if(strstr(mbuf,parent_dir_string)!=NULL) {
      setvar(error_data_string,mbuf);
      module_error("illegal_fname"); return;
    }
    mkfname(nbuf,"%s/%s",module_prefix,mbuf);
  }
  else {
    ppp=getvar(ro_name[ro_module]);
    if(ppp==NULL) internal_error("calc_instexst(): module name vanishes.");
    p2=p1;
    mkfname(nbuf,"w/instex/%d/%s/%s_%d.gif",
           current_tex_size,ppp,m_file.name,m_file.l);
  }
  snprintf(urlbuf,sizeof(urlbuf),"%s%s?%X",ref_base,nbuf,
          (unsigned short int) stc.st_mtime);
  mystrncpy(buf,nbuf,sizeof(buf));
  if((ppp=strrchr(buf,'/'))!=NULL) {
    *ppp=0;mkdirs(buf);
  }
  b=getvar("ins_border");
  at=getvar("ins_attr");
  al=getvar("ins_align");
  if(at==NULL) at="";
  if(al==NULL) al="";
  al=find_word_start(al);
  if(*al!=0) snprintf(buf2,sizeof(buf2),"vertical-align:%s",al); else buf2[0]=0;
  if(b==NULL || *b==0) border=0;
  else border=atoi(b);
  if(border<0) border=0;
  if(border>100) border=100;
  if(instex_ready(p2,urlbuf)) goto prt;
  if(stat(nbuf,&st)!=0 || st.st_mtime<stc.st_mtime || st.st_size<45) {
    setenv("texgif_style",instex_style,1);
    setenv("texgif_src",p2,1);
    setenv("texgif_outfile",nbuf,1);
    setenv("texgif_tmpdir",tmp_dir,1); exportall();
    wrapexec=0; call_ssh("%s &>%s/instexst.log",tex2gif,tmp_dir);
    setenv("instexst_src","",1);
  }
  prt: md1=md2="";
  if(strcasecmp(al,"middle")==0) {
   md1=mathalign_sup1; md2=mathalign_sup2;
   vspace=5;
  }
  if(ins_alt[0]==0) mystrncpy(ins_alt,p2,sizeof(ins_alt));
  if(strchr(ins_alt,'"')!=NULL || strlen(ins_alt)>256) ins_alt[0]=0;
  pt=getvar("wims_ins_alt"); if(pt==NULL) pt="";
  if(strcmp(pt,"empty")==0) altbuf[0]=0;
  else
    if(ins_alt[0] && strcmp(pt,"none")!=0)
      snprintf(altbuf,sizeof(altbuf)," alt=\"\"");
    else
      snprintf(altbuf,sizeof(altbuf)," alt=\"%s\"",ins_alt);
  snprintf(p,MAX_LINELEN,"%s<img src=\"%s\" style=\"border:solid;border-width:%dpx;margin-bottom:%dpx;%s\" %s %s>%s",
        md1,urlbuf,border,vspace,buf2,at,altbuf,md2);
  setvar("ins_attr",""); ins_alt[0]=0;
  setvar("ins_url",urlbuf);
}

/* extract non-empty lines or items */
void calc_nonempty(char *p)
{
  int type, i, cnt;
  char *p1, *p2, buf[MAX_LINELEN+1], out[MAX_LINELEN+1];
  p1=find_word_start(p); p2=find_word_end(p1);
  if(*p2) *p2++=0; else {
    *p=0; return;
  }
  type=0;
  if(strcasecmp(p1,"item")==0 || strcasecmp(p1,"items")==0) type=1;
  if(type==0 && (strcasecmp(p1,"line")==0 || strcasecmp(p1,"lines")==0))
    type=2;
  if(type==0 && (strcasecmp(p1,"row")==0 || strcasecmp(p1,"rows")==0))
    type=3;
  if(type==0) module_error("syntax_error");
  out[0]=out[1]=0;
  switch(type) {
    case 1: { /* items */
      cnt=itemnum(p2);
      for(i=1; i<=cnt; i++) {
        fnd_item(p2,i,buf);
        if(*find_word_start(buf)) {
          strcat(out,",");strcat(out,buf);
        }
      }
      break;
    }
    case 2: { /* lines */
      lines: cnt=linenum(p2);
      for(i=1; i<=cnt; i++) {
        fnd_line(p2,i,buf);
        if(*find_word_start(buf)) {
          strcat(out,"\n");strcat(out,buf);
        }
      }
      break;
    }
    case 3: { /* rows */
      int t=rows2lines(p2);
      if(t==0) goto lines;
      cnt=linenum(p2);
      for(i=1; i<=cnt; i++) {
        fnd_line(p2,i,buf);
        if(*find_word_start(buf)) {
          strcat(out,";");strcat(out,buf);
        }
      }
      break;
    }
    default: break;
  }
  ovlstrcpy(p,out+1);
}

/* returns a list with unique items */
void calc_listuniq(char *p)
{
  int i,n;
  char lout[MAX_LINELEN+2], *ll;
  char *cut[MAX_LIST];
  lout[0]=lout[1]=0;
  n=cutitems(p,cut,MAX_LIST); for(i=0;i<n;i++) {
    ll=cut[i];
    if(*ll && itemchr(lout,ll)==NULL &&
        strlen(lout)+strlen(ll)<MAX_LINELEN-2) {
      strcat(lout,",");strcat(lout,ll);
    }
  }
  ovlstrcpy(p,lout+1);
}

/* returns intersection of 2 lists */
void calc_listintersect(char *p)
{
  char l1[MAX_LINELEN+1],l2[MAX_LINELEN+1];
  char *cut[MAX_LIST],*cut1[MAX_LIST],*cuts[MAX_LIST];
  char *pp, *ll;
  int i,j,k,n,m,s=0;
  pp=wordchr(p,"and");
  if(pp==NULL) module_error("syntax_error");
  *pp=0;ovlstrcpy(l1,p); ovlstrcpy(l2,pp+strlen("and"));
  substit(l1); substit(l2);
  n=cutitems(l2,cut,MAX_LIST); if(n<=0) {*p=0; return;}
  m=cutitems(l1,cut1,MAX_LIST);
  for(i=0;i<n;i++) {
    ll=cut[i];
    for(k=0;k<m;k++)
      if(!strcmp(cut1[k],ll)) break;
    if (k==m) continue;
    for(j=0;j<s;j++)
      if(!strcmp(cuts[j],ll)) break;
    if (j==s) cuts[s++]=ll;
  }
  p[0]=0;
  for(i=0;i<s;i++){
    if(i) strcat(p,",");
    if(strlen(p)+strlen(cuts[i])<MAX_LINELEN-2)
    strcat(p,cuts[i]);
  }
}

/* returns union of 2 lists */
void calc_listunion(char *p)
{
  char l1[MAX_LINELEN+1],l2[MAX_LINELEN+1];
  char *cut[MAX_LIST],*cut1[MAX_LIST],*cuts[MAX_LIST];
  char *pp, *ll;
  int i,j,k,n,m,s=0;
  pp=wordchr(p,"and");
  if(pp==NULL) module_error("syntax_error");
  *pp=0;ovlstrcpy(l1,p); ovlstrcpy(l2,pp+strlen("and"));
  substit(l1); substit(l2);
  n=cutitems(l2,cut,MAX_LIST);
  m=cutitems(l1,cut1,MAX_LIST);
  for(i=0;i<n;i++) {
    ll=cut[i];
    for(j=0;j<s;j++)
      if(!strcmp(cuts[j],ll)) break;
    if (j==s) cuts[s++]=ll;
  }
  for(k=0;k<m;k++) {
    ll=cut1[k];
    for(j=0;j<s;j++)
      if(!strcmp(cuts[j],ll)) break;
    if(j==s) cuts[s++]=ll;
  }
  p[0]=0;
  for(i=0;i<s;i++){
    if(i) strcat(p,",");
    if(strlen(p)+strlen(cuts[i])<MAX_LINELEN-2)
    strcat(p,cuts[i]);
  }
}

/* returns items of list2 not in list1 */
void calc_listcomplement(char *p)
{
  char l1[MAX_LINELEN+1],l2[MAX_LINELEN+1];
  char *cut[MAX_LIST],*cut1[MAX_LIST],*cuts[MAX_LIST];
  char *pp, *ll;
  int i,j,k,n,m,s=0;
  pp=wordchr(p,"in");
  if(pp==NULL) module_error("syntax_error");
  *pp=0;ovlstrcpy(l1,p); ovlstrcpy(l2,pp+strlen("in"));
  substit(l1); substit(l2);
  n=cutitems(l2,cut,MAX_LIST); if(n<=0) {*p=0; return;}
  m=cutitems(l1,cut1,MAX_LIST);
  for(i=0;i<n;i++) {
    ll=cut[i];
    for(j=0;j<s;j++)
      if(!strcmp(cuts[j],ll)) break;
    for(k=0;k<m;k++)
      if(!strcmp(cut1[k],ll)) break;
    if(j==s && k==m) cuts[s++]=ll;
  }
  p[0]=0;
  for(i=0;i<s;i++){
    if(i) strcat(p,",");
    if(strlen(p)+strlen(cuts[i])<MAX_LINELEN-2)
    strcat(p,cuts[i]);
  }
}

/* Consult a dictionary to translate a string */
/*
void calc_dictionary(char *p)
{
  char *pp;
  char name[MAX_LINELEN+1], str[MAX_LINELEN+1];
  int t;
  pp=wordchr(p,"for");
  if(pp==NULL || pp<=p) module_error("syntax_error");
  *(find_word_end(pp-1))=0;
  ovlstrcpy(name,p); substit(name);
  pp=find_word_start(pp+strlen("for")); t=0;
  if(memcmp(pp,"word",strlen("word"))==0 &&
     (isspace(*(pp+strlen("word"))) ||
   (*(pp+strlen("word"))=='s' && isspace(*(pp+strlen("words")))))) {
   pp=find_word_start(pp+strlen("words")); t=1;
  }
  if(memcmp(pp,"line",strlen("line"))==0 &&
     (isspace(*(pp+strlen("line"))) ||
   (*(pp+strlen("line"))=='s' && isspace(*(pp+strlen("lines")))))) {
   pp=find_word_start(pp+strlen("lines")); t=1;
  }
  if(memcmp(pp,"list",strlen("list"))==0 && isspace(*(pp+strlen("list")))) {
   pp=find_word_start(pp+strlen("list"));
  }
  if(memcmp(pp,"items",strlen("items"))==0 && isspace(*(pp+strlen("items")))) {
   pp=find_word_start(pp+strlen("items"));
  }
  if(memcmp(pp,"item",strlen("item"))==0 && isspace(*(pp+strlen("item")))) {
   pp=find_word_start(pp+strlen("item"));
  }
  ovlstrcpy(str,pp); substit(str);
  switch(t) {
   case 1: {
       calc_words2items(str); break;
   }
   case 2: {
         calc_lines2items(str); break;
     }
     default: break;
    }
}
*/

void calc_module(char *p)
{
  char *p1,*p2, ind_buf[MAX_LINELEN+1], buf[MAX_FNAME+1];
  char *tp;

  p1=find_word_start(p); p2=find_word_end(p1);
  if(*p2!=0) *p2++=0;
  p2=find_word_start(p2); *find_word_end(p2)=0;
  if(*p1==0) {empty: *p=0; return;}
  if(*p2==0) {
    snprintf(buf,sizeof(buf),"module_%s",p1);
    p1=getvar(buf); if(p1==NULL) p1="";
    mystrncpy(p,p1,MAX_LINELEN); return;
  }
  mkfname(buf,"%s/%s/INDEX",module_dir,p2);
  tp=readfile(buf,ind_buf,MAX_LINELEN);
  if(tp==NULL) {
    mkfname(buf,"%s/%s/index",module_dir,p2);
    tp=readfile(buf,ind_buf,MAX_LINELEN);
  }
  if(tp==NULL && p2[strlen(p2)-3]!='.') {
    mkfname(buf,"%s/%s.%s/INDEX",module_dir,p2,lang);
    tp=readfile(buf,ind_buf,MAX_LINELEN);
    if(tp==NULL) {
      mkfname(buf,"%s/%s.%s/index",module_dir,p2,lang);
      tp=readfile(buf,ind_buf,MAX_LINELEN);
    }
    if(tp==NULL) {
      int i;
      for(i=0;i<available_lang_no;i++) {
        mkfname(buf,"%s/%s.%s/INDEX",module_dir,p2,available_lang[i]);
        tp=readfile(buf,ind_buf,MAX_LINELEN); if(tp) break;
        mkfname(buf,"%s/%s.%s/index",module_dir,p2,
              available_lang[i]);
        tp=readfile(buf,ind_buf,MAX_LINELEN); if(tp) break;
      }
    }
  }
  if(tp==NULL) goto empty; /* module not found */
  _getdef(ind_buf,p1,p);
}

/* strip enclosing parentheses */
void calc_declosing(char *p)
{
  strip_enclosing_par(p);
}
/* return no if the parenthesis do not match */
void calc_checkallpar(char *p)
{
  if(check_parentheses(p,1)==0)
    ovlstrcpy(p, "yes");
  else ovlstrcpy(p, "no");
}
/* remove html tag, very rudimentary.
  by default, everything is detagged
  one can choose what to detag by the parameter wims_parm_detag
*/
void calc_detag(char *p)
{
  char *p1, *p2, *s, tag[100];
  int n=0;

  s=getvar("wims_parm_detag");
  if(s==NULL || *s == 0)
    for(p1=strchr(p,'<'); p1!=NULL; p1=strchr(p1,'<'))
      {
        p2=strchr(p1,'>');
        if(p2==NULL) p1++; else ovlstrcpy(p1,p2+1);
      }
  else
    for(s=find_word_start(s); *s; s=find_word_start(s+n))
    {
      n = find_word_end(s) - s; if (n > 96) n = 96;
      tag[0]='<';
      mystrncpy(tag+1, s, n+1); tag[n+1]=0;
      for(p1=strstr(p,tag); p1!=NULL; p1=strstr(p1,tag))
      {
        p2=strchr(p1,'>');
        if(p2==NULL) p1++; else ovlstrcpy(p1,p2+1);
      }
      tag[1]='/';
      mystrncpy(tag+2, s, n+1); tag[n+2]='>'; tag[n+3]=0;
      for(p1=strstr(p,tag); p1!=NULL; p1=strstr(p1,tag))
        ovlstrcpy(p1,p1+strlen(tag));
    }
}

/* replace html symbols by their iso translation */
void calc_html2iso(char *p)
{
  int st, ns;
  unsigned char buffer[MAX_LINELEN], *s = buffer;
  inithtmltrans();
  strcpy((char *)buffer, p);
  while (*s)
    if (*s == '&'){
        unsigned char *p1 = ++s;
        for (st = 0; (ns = htmltrans[abs(st)][*s]); st = ns)
           s++;
        if (st > 0 && *s == ';')
          {char *r=htmlsymbs[st][1]; strcpy((char *)p, r); s++; p+=strlen(r);}
        else
          {*p++='&'; s=p1;}
      }
    else *p++=*s++;
  *p = 0;
}

/* prepare a string to be inserted into a form input or textarea as is */
void calc_reinput(char *p)
{
  char *p1;
  for(p1=strchr(p,'&'); p1!=NULL; p1=strchr(p1,'&')) {
    p1++; string_modify(p,p1,p1,"amp;");
  }
  for(p1=strchr(p,'<'); p1!=NULL; p1=strchr(++p1,'<'))
    string_modify(p,p1,p1+1,"&lt;");
}

/* get a definition from a file. Trusted modules only. */
void calc_defof(char *p)
{
  char *p1;
  char fbuf[MAX_FNAME+1], nbuf[MAX_LINELEN+1], tbuf[MAX_LINELEN+1];

  secure_exec();
  p1=wordchr(p,"in"); if(p1==NULL) module_error("syntax_error");
  *p1=0; p1=find_word_start(p1+strlen("in"));
  mystrncpy(nbuf,p,sizeof(nbuf));  mystrncpy(tbuf,p1,sizeof(tbuf));
  substit(nbuf); substit(tbuf);
  p1=find_word_start(tbuf); *find_word_end(p1)=0;
  if(find_module_file(p1,fbuf,0)) {*p=0; return;}
  p1=find_word_start(nbuf); strip_trailing_spaces(p1);
  getdef(fbuf,p1,p);
}

/* check host */
void calc_checkhost(char *p)
{
  int t;
  if(robot_access || !trusted_module()) ovlstrcpy(p,"0");
  else {
    t=checkhost(p); mystrncpy(p,int2str(t),MAX_LINELEN);
  }
}

#define MAX_COLUMNS 256

/* A rudimentary database facility */
void calc_select(char *p)
{
  char *p1, *p2, *p3, *bufp, c, sep;
  char buf1[MAX_LINELEN+1], buf2[MAX_LINELEN+1];
  char buf[MAX_LINELEN+1];
  int i,j,lmax;
  p2=wordchr(p,"where"); if(p2==NULL) module_error("syntax_error");
  *p2=0; p2+=strlen("where");
  p2=find_word_start(p2); strip_trailing_spaces(p2);
  p1=find_word_start(p) ; strip_trailing_spaces(p1);
  if(*p1==0 || *p2==0) module_error("syntax_error");
  ovlstrcpy(buf1,p1); ovlstrcpy(buf2,p2); sep='\n';
/* buf1: data; buf2: condition. */
  substit(buf1);
  if(strstr(buf2,"column")!=NULL) { /* matrix */
    lmax=0; if(rows2lines(buf1)) sep=';';
    for(p1=strstr(buf2,"column"); p1; p1=strstr(p1+1,"column")) {
      if(p1>buf2 && isalnum(*(p1-1))) continue;
      p2=find_word_start(p1+strlen("column"));
      for(p3=p2; myisdigit(*p3); p3++);
      if(p3==p2) continue;
      c=*p3; *p3=0; i=atoi(p2); *p3=c;
      if(i<=0 || i>MAX_COLUMNS) continue;
      if(i>lmax) lmax=i;
      string_modify(buf2,p1,p3,"$(wims_select_col_%d)",i);
    }
    buf[0]=0; bufp=buf;
    for(p1=buf1; *p1; p1=p2) {
      char ibuf[MAX_LINELEN+1], nbuf[MAX_NAMELEN+1];
      p2=strchr(p1,'\n');
      if(p2==NULL) p2=p1+strlen(p1); else *p2++=0;
      if(*find_word_start(p1)==0) continue;
      for(j=1;j<=lmax;j++) {
        snprintf(nbuf,sizeof(nbuf),"wims_select_col_%d",j);
        fnd_item(p1,j,ibuf); force_setvar(nbuf,ibuf);
      }
      ovlstrcpy(ibuf,buf2); if(compare(ibuf,0,0)) {
        snprintf(bufp,MAX_LINELEN-(bufp-buf),"%s%c",p1,sep);
        bufp+=strlen(bufp);
      }
    }
  }
  else { /* datafile */
   module_error("syntax_error");
  }
  if(buf[0]!=0) buf[strlen(buf)-1]=0;
  mystrncpy(p,buf,MAX_LINELEN);
}

/* Extract a column from a matrix */
void calc_columnof(char *p)
{
  char *p1, *p2, *bufp, sep;
  char buf1[MAX_LINELEN+1], buf2[MAX_LINELEN+1];
  char buf[MAX_LINELEN+1];
  p2=wordchr(p,"of"); if(p2==NULL) module_error("syntax_error");
  *p2=0; p2+=strlen("of");
  p2=find_word_start(p2); strip_trailing_spaces(p2);
  p1=find_word_start(p) ; strip_trailing_spaces(p1);
  if(*p1==0) module_error("syntax_error");
  if(*p2==0) {*p=0; return;}
  ovlstrcpy(buf1,p1); ovlstrcpy(buf2,p2); sep='\n';
/* buf1: number(s); buf2: matrix. */
  substit(buf1); substit(buf2);
  if(rows2lines(buf2)) sep=';';
  if(strchr(buf1,',')==NULL && wordchr(buf1,"to")==NULL
     && strstr(buf1,"..")==NULL) sep=',';
  buf[0]=0; bufp=buf;
  for(p1=buf2; *p1; p1=p2) {
    char ibuf[MAX_LINELEN+1];
    p2=strchr(p1,'\n');
    if(p2==NULL) p2=p1+strlen(p1); else *p2++=0;
    snprintf(ibuf,sizeof(ibuf),"%s of %s",buf1,p1);
    calc_itemof(ibuf);
    snprintf(bufp,MAX_LINELEN-(bufp-buf),"%s%c",ibuf,sep);
    bufp+=strlen(bufp);
  }
  if(buf[0]!=0) buf[strlen(buf)-1]=0;
  mystrncpy(p,buf,MAX_LINELEN);
}

/* find roots of a function or equation in a given zone */
void calc_solve(char *p)
{
  char *pp, *fp, *forp;
  char buf[MAX_LINELEN+1], vbuf[MAX_LINELEN+1];
  double v, dd, start, stop, step, old, v1, v2, v3, d1, d3;
  int i, pos;

  forp=wordchr(p,"for");
  if(forp==NULL) { syntax: module_error("syntax_error"); *p=0; return; }
  *forp=0; forp+=strlen("for");
  fp=find_word_start(p); strip_trailing_spaces(fp);
  if(*fp==0) goto syntax;
  i=cutfor(forp,NULL); if(i<0 || forstruct.type==for_in) goto syntax;
  if(i>0) {*p=0; return;}
  start=forstruct.from; stop=forstruct.to; forp=forstruct.var;
  mystrncpy(buf,fp,sizeof(buf)); substitute(buf);
  *p=0; pp=strchr(buf,'='); if(pp!=NULL) {
    if(strlen(buf)>=MAX_LINELEN-16) return;
    strcat(buf,")");
    string_modify(buf,pp,pp+1,"-(");
  }
  i=0; for(fp=varchr(buf,forp); fp!=NULL; fp=varchr(fp,forp)) {
    string_modify(buf,fp,fp+strlen(forp),EV_X); fp+=strlen(EV_X); i++;
  }
  if(i==0 || start==stop) return;
  evalue_compile(buf); pos=eval_getpos(EV_X);
  if(start>stop) {dd=start; start=stop; stop=dd;}
  step=(stop-start)/100; if(step==0) return;
  pp=p; old=0;
  for(v=start; v<=stop; v+=step, old=dd) {
    eval_setval(pos,v);
    dd=checked_eval(buf);
    if(v==start) continue;
    if(!isfinite(old) || !isfinite(dd) || (old>0 && dd>0) || (old<0 && dd<0))
      continue;
    if(dd==0 && v<stop) continue;
    v1=v-step; v2=v; d1=old;
    for(i=0;i<30;i++) {
      v3=(v1+v2)/2; eval_setval(pos,v3);
      d3=checked_eval(buf);
      if(!isfinite(d3)) goto next;
      if((d1>0 && d3>0) || (d1<0 && d3<0)) {d1=d3; v1=v3;}
      else {v2=v3;}
    }
    float2str(v3,vbuf); if(pp-p+strlen(vbuf)<MAX_LINELEN-1) {
      if(pp>p) *pp++=',';
      ovlstrcpy(pp,vbuf);
      pp+=strlen(pp);
    }
    else break;
    next: ;
  }
}

/* type: 1=values, 2=sum, 3=product, 4=recursion, 5=subst */
void _values(char *p, int type)
{
  char *pp, *fp, *forp;
  char vbuf[MAX_LINELEN+1], buf[MAX_LINELEN+1], fbuf[MAX_LINELEN+1], tbuf[MAX_LINELEN+1];
  char *f[64];
  double v, dd, start, stop, step, v0;
  int i, k, fcnt, pos, posr;

  forp=wordchr(p,"for");
  if(forp==NULL) { syntax: module_error("syntax_error"); *p=0; return; }
  *forp=0; forp+=strlen("for"); forp=find_word_start(forp);
  fp=find_word_start(p); strip_trailing_spaces(fp);
  if(*fp==0) goto syntax;
  if(type<5) i=cutfor(forp,NULL); else i=cutfor(forp,tbuf);
  if(i<0) goto syntax;
  if(i>0) {*p=0; return;}
  start=forstruct.from; stop=forstruct.to; step=forstruct.step;
  forp=forstruct.var;
  mystrncpy(buf,fp,sizeof(buf)); substitute(buf);
  for(fp=varchr(buf,forp); fp!=NULL; fp=varchr(fp,forp)) {
    string_modify(buf,fp,fp+strlen(forp),EV_X); fp+=strlen(EV_X);
  }
  for(fp=varchr(buf,"last"); fp!=NULL; fp=varchr(fp,"last")) {
    string_modify(buf,fp,fp+strlen("last"),EV_S); fp+=strlen(EV_S);
  }
  fcnt=pos=posr=0;
  if(type==5) goto skip;
  fcnt=itemnum(buf); if(fcnt>64) fcnt=64; pp=fbuf;
  for(k=0; k<fcnt; k++) {
    fnd_item(buf,k+1,vbuf); evalue_compile(vbuf);
    if(pp-fbuf+strlen(vbuf)<MAX_LINELEN-1) {
      f[k]=pp; ovlstrcpy(pp,vbuf); pp+=strlen(pp)+1;
    }
    else f[k]="";
  }
  pos=eval_getpos(EV_X); posr=eval_getpos(EV_S);
  skip:
  if(step==0) step=1;/* if(step<0) step=-step;
  if(stop<start) {dd=start; start=stop; stop=dd;} */
  *p=0; v0=0;
  switch(type) {
    case 4:
    case 1: {
      pp=getvar("recursion_start");
      if(pp==NULL || *pp==0) v0=0;
      else {
        v0=evalue(pp); if(!isfinite(v0)) return;
      }
      break;
    }
    case 2: v0=0; break;
    case 3: v0=1; break;
    case 5: break;
    }
    pp=p;
    if(type==5) {
      char *ps, *pt, buf2[MAX_LINELEN+1];
      int l,ln;
      *p=0; l=strlen(buf); if(l>=MAX_LINELEN) return;
      for(i=0,v=start; i<MAX_VALUE_LIST && v*step<=stop*step; v+=step, i++) {
        if(forstruct.type==for_from) {
          float2str(v,vbuf); ps=vbuf;
        }
        else ps=forstruct.pos[i];
        ovlstrcpy(buf2,buf); l=strlen(ps);ln=strlen(EV_X);
        for(pt=varchr(buf2,EV_X);pt!=NULL;pt=varchr(pt+l,EV_X))
          string_modify(buf2,pt,pt+ln,"%s",ps);
        if(pp-p+strlen(buf2)>=MAX_LINELEN-1) return;
        if(pp>p) *pp++=',';
        ovlstrcpy(pp,buf2);
        pp+=strlen(pp);
      }
      return;
    }
    for(i=0,v=start; i<MAX_VALUE_LIST && v*step<=stop*step; v+=step, i++) {
      if(forstruct.type==for_from) eval_setval(pos,v);
      else eval_setval(pos,forstruct.list[i]);
      eval_setval(posr,v0);
      for(k=0; k<fcnt; k++) {
        dd=checked_eval(f[k]);
        switch(type) {
          case 1: { /* values */
            float2str(dd,vbuf);
            if(pp-p+strlen(vbuf)<MAX_LINELEN-1) {
              if(pp>p) *pp++=',';
              ovlstrcpy(pp,vbuf);
              pp+=strlen(pp);
            }
            v0=dd; break;
         }
         case 2: { /* sum */
          v0=v0+dd; break;
        }
        case 3: { /* product */
          v0=v0*dd; break;
        }
        case 4: { /* recursion */
          v0=dd; break;
        }
      }
    }
  }
  if(type!=1) float2str(v0,p);
}

/* cut a function into values */
void calc_values(char *p)
{ _values(p,1); }

/* compute sum */
void calc_sum(char *p)
{ _values(p,2); }

/* compute product */
void calc_product(char *p)
{ _values(p,3); }

/* simple recursion */
void calc_recursion(char *p)
{ _values(p,4); }

/* List substitution */
void calc_makelist(char *p)
{ _values(p,5); }

/* level curve data */
void calc_leveldata(char *p)
{
  leveldata ld;
  char *sizep, *rangep, *fp, *levelp, *stepp;
  char *pp,*p2,fbuf[MAX_LINELEN+1],buf[MAX_LINELEN+1];
  double d[4];
  int i;

  sizep=wordchr(p,"size");
  rangep=wordchr(p,"ranges");
  fp=wordchr(p,"function");
  levelp=wordchr(p,"levels");
  stepp=wordchr(p,"step");
  if(sizep==NULL || rangep==NULL || fp==NULL) {
    syntax: module_error("syntax_error"); *p=0; return;
  }
  *sizep=0; sizep+=strlen("size");
  *rangep=0; rangep+=strlen("ranges");
  *fp=0; fp+=strlen("function");
  if(levelp!=NULL) {*levelp=0; levelp+=strlen("levels");}
  else levelp="0";
  if(stepp!=NULL) {*stepp=0; stepp+=strlen("step");}
  else stepp="0";
  mystrncpy(fbuf,fp,sizeof(fbuf)); substitute(fbuf);
  ld.fn=fbuf;
  ld.xname="x"; ld.yname="y"; ld.grain=evalue(stepp);
  mystrncpy(buf,sizep,sizeof(buf)); substitute(buf);
  for(i=0,pp=buf;i<2;i++,pp=p2) {
    if(*pp==0) goto syntax;
    p2=find_item_end(pp); if(*p2) *p2++=0;
    d[i]=evalue(pp);
  }
  ld.xsize=d[0]; ld.ysize=d[1];
  mystrncpy(buf,rangep,sizeof(buf)); substitute(buf);
  for(i=0,pp=buf;i<4;i++,pp=p2) {
    if(*pp==0) goto syntax;
    p2=find_item_end(pp); if(*p2) *p2++=0;
    d[i]=evalue(pp);
  }
  ld.xrange[0]=d[0]; ld.xrange[1]=d[1]; ld.yrange[0]=d[3]; ld.yrange[1]=d[2];
  mystrncpy(buf,levelp,sizeof(buf)); substitute(buf);
  ld.levelcnt=itemnum(buf); if(ld.levelcnt>LEVEL_LIM) ld.levelcnt=LEVEL_LIM;
  for(i=0,pp=buf;i<ld.levelcnt;i++,pp=p2) {
    if(*pp==0) goto syntax;
    p2=find_item_end(pp); if(*p2) *p2++=0;
    ld.levels[i]=evalue(pp);
  }
  levelcurve(&ld);
  for(i=0, pp=p; i<ld.datacnt && pp<p+MAX_LINELEN-16; i++) {
    float2str(ld.xdata[i],buf);
    if(pp-p+strlen(buf)<MAX_LINELEN-1) {
      if(pp>p) *pp++=';';
      ovlstrcpy(pp,buf); pp+=strlen(pp);
    }
    float2str(ld.ydata[i],buf);
    if(pp-p+strlen(buf)<MAX_LINELEN-1) {
      if(pp>p) *pp++=',';
      ovlstrcpy(pp,buf); pp+=strlen(pp);
    }
  }
}

/* internal routine with no security check */
void _lookup(char *p, char *fname)
{
  char buf1[MAX_LINELEN+1];
  char *p1, *p2, *mbuf;

  mbuf=readfile(fname,NULL,WORKFILE_LIMIT);
  if(mbuf==NULL) {abort: *p=0; return;}
  p1=find_word_start(p); strip_trailing_spaces(p1);
  snprintf(buf1,sizeof(buf1),"%s:",p1); substit(buf1);
  for(p1=strstr(mbuf,buf1);
  p1!=NULL && p1>mbuf && *(p1-1)!='\n';
  p1=strstr(p1+1,buf1));
  if(p1==NULL) {free(mbuf); goto abort;}
  p1+=strlen(buf1);
  for(p2=strchr(p1,'\n'); p2!=NULL; p2=strchr(p2+1,'\n')) {
    if(p2>p1 && *(p2-1)=='\\') {*(p2-1)=' '; continue;}
    else break;
  }
  if(p2==NULL) p2=p1+strlen(p1); else *p2=0;
  mystrncpy(p,p1,MAX_LINELEN); free(mbuf);
}

/* lookup a definition in a definition file */
void calc_lookup(char *p)
{
  char buf2[MAX_LINELEN+1], buf3[MAX_FNAME+1];
  char *p2;

  p2=wordchr(p,"in"); if(p2==NULL) {abort: *p=0; return;}
  *p2=0;p2=find_word_start(p2+2);
  mystrncpy(buf2,p2,sizeof(buf2)); substit(buf2);
  *find_word_end(buf2)=0;
  if(strstr(buf2,parent_dir_string)!=NULL) {
    force_setvar("wims_error_data",buf2); module_error("illegal_cmd");
  }
  if(strncmp(buf2,"bases/",strlen("bases/"))!=0) {
    if(datafile_check(buf2)!=0 || find_module_file(buf2,buf3,0)!=0)
      goto abort;
    _lookup(p,buf3);
  }
  else _lookup(p,buf2);
}

/* Hide name of a file. Only in module directory or in gifs/ */
void calc_rename(char *p)
{
  char buf1[MAX_LINELEN+1], buf2[MAX_LINELEN+1];
  char *p1, *ext, *s;
  int t;

  if(robot_access || strstr(p,"getfile")!=NULL) return;
  p1=find_word_start(p); *find_word_end(p1)=0;
  if(strncmp(p1,ref_name,strlen(ref_name))==0) p1+=strlen(ref_name);
  if(p1>p) ovlstrcpy(p,p1);
  if(strstr(p,parent_dir_string)!=NULL ||
     strncmp(p,"modules/adm/",strlen("modules/adm/"))==0) {
    badfile: force_setvar("wims_error_data",p); module_error("illegal_cmd");
  }
  if(strncmp(p,module_dir,strlen(module_dir))!=0 &&
     strncmp(p,"modules/data/",strlen("modules/data/"))!=0 &&
     strncmp(p,"scripts/data/",strlen("scripts/data/"))!=0 &&
     strncmp(p,"gifs",strlen("gifs"))!=0) goto badfile;
  mkfname(buf1,"%s/getfile",session_prefix); mkdirs(buf1);
  mkfname(buf1,"%s/.rename",session_prefix);
  mystrncpy(buf2,p,sizeof(buf2)); _lookup(buf2,buf1);
  if(buf2[0]!=0) { /* already */
    mystrncpy(p,buf2,MAX_LINELEN); return;
  }
  if(cwdbuf[0]==0) return;
  p1=p+strlen(p)-1;
  while(p1>p && isalnum(*p1)) p1--;
  if(p1>p && *p1=='.') ext=p1; else ext="";
  rerand: t=random();
  mkfname(buf1,"%s/%s",cwdbuf,p);
  mkfname(buf2,"%s/getfile/rename-%u%s",session_prefix,t,ext);
  if(ftest(buf2)>=0) goto rerand;
  (void) symlink(buf1,buf2);
  s=getvar("wims_session"); if(s==NULL) return;
  if(good_httpd) snprintf(buf1,sizeof(buf1),
                   "getfile/rename-%u%s?session=%s", t,ext,s);
  else snprintf(buf1,sizeof(buf1),"%s?cmd=getfile&+session=%s&+special_parm=rename-%u%s",
            ref_name, s, t,ext);
  snprintf(buf2,sizeof(buf2),"%s:%s\n",p,buf1);
  accessfile(buf2,"a","%s/.rename",session_prefix);
  mystrncpy(p,buf1,MAX_LINELEN);
}

/* Pick up and translate imgrename(...) within a string */
void calc_imgrename(char *p)
{
  char buf[MAX_LINELEN+1], buf2[MAX_LINELEN+1];
  char *p1, *p2, *p3, *p4;

  for(p1=varchr(p,"imgrename"); p1!=NULL; p1=varchr(p1,"imgrename")) {
    p2=find_word_start(p1+strlen("imgrename"));
    if(*p2!='(') {p1=p2; continue;}
    p2++; p3=find_matching(p2,')');
    if(*p3!=')') {p1=p2-1; continue;}
    p2=find_word_start(p2); p4=find_word_end(p2);
     memmove(buf,p2,p4-p2); buf[p4-p2]=0;
    calc_rename(buf); *p3=0;
    snprintf(buf2,sizeof(buf2),"<img src=\"%s\"%s alt=\"\">",buf, p4);
    *p3=')'; p3++;
    string_modify(p,p1,p3,"%s",buf2);
      p1+=strlen(buf2);
  }
}

/* internal use only */
void calc_unhttp(char *p)
{
  _http2env(tmplbuf,p); mystrncpy(p,tmplbuf,MAX_LINELEN);
}

/*
method BDSi not maintained in linux:
...0 gives the number of DES rounds (2 here) _...0smiw
come back to "Nv", basic DES
*/
void _passcrypt(char *p, char *salted_hash)
{
#ifdef HAVE_CRYPT
  char* saltstr=salted_hash ? salted_hash: "Nv";
  char *p1, *p2, *pp, *s, buf[MAX_LINELEN+1];
  buf[0]=0; pp=buf;
  for(p1=find_word_start(p);*p1;p1=find_word_start(p2)) {
    p2=find_word_end(p1); if(*p2) *p2++=0;
    pp=pp+strlen(pp);
    if(pp>buf) s=" "; else s="";
    if(*p1=='*')
      snprintf(pp,MAX_LINELEN-(pp-buf),"%s%s",s,p1);
    else
      snprintf(pp,MAX_LINELEN-(pp-buf),"%s*%s",s,crypt(p1,saltstr));
  }
  ovlstrcpy(p,buf);
#endif
}

void calc_passcrypt(char *p)
{
  _passcrypt(p, NULL);
}
/*
 syntax: passcheck nocrypt and list of words as *cryptwords
 check if the passcrypt of nocrypt is amongst the list of words
*/
void calc_passcheck(char *p)
{
  char cl[MAX_LINELEN+1],sh[MAX_LINELEN+1];
  char *qq, *pp=wordchr(p,"and");
  if(pp==NULL) module_error("syntax_error");
  *pp=0;ovlstrcpy(cl,p); ovlstrcpy(sh,pp+strlen("and"));
  substit(cl); substit(sh);
  for (pp = sh;*pp; pp=qq){
    while (*pp && *pp != '*') ++pp;
    /* delete the star */
    pp++;
    if(!*pp) break;
    for (qq = pp; *qq && *qq != ' '; ++qq);
    if (*qq) *qq++ = 0;
    _passcrypt(cl, pp);
    if (!strcmp(cl+1, pp))
    {ovlstrcpy(p, "yes"); return;}
  }
  ovlstrcpy(p, "no");
}

void exec_readproc(char *p);

/* crypted mail interface */
void calc_mailurl(char *p)
{
  char *p0, buf[MAX_LINELEN+1];

  if(robot_access) {*p=0; return;}
  snprintf(buf,sizeof(buf),"mailurl.proc %s",p);
  exec_readproc(buf);
  p0=getvar("mailurl_"); if(p0==NULL) p0="";
  mystrncpy(p,p0,MAX_LINELEN);
}

/* get option word in a string */
void calc_getopt(char *p)
{
  char *p1, *p2, *p3, *p4;
  char buf1[MAX_LINELEN+1], buf2[MAX_LINELEN+1];

  p1=wordchr(p,"in"); if(p1==NULL) module_error("syntax error");
  *p1=0; p1=find_word_start(p1+3);
  mystrncpy(buf1,p,MAX_LINELEN); mystrncpy(buf2,p1,MAX_LINELEN);
  substitute(buf1); substitute(buf2);
  p1=find_word_start(buf1); *find_word_end(p1)=0;
  for(p2=buf2;*p2;p2++) {
    if(myisspace(*p2)) *p2=' ';
    if(*p2=='=') *p2='	';
  }
  *p=0;
  p2=wordchr(buf2,p1); if(p2==NULL) return;
  for(p3=find_word_end(p2);myisspace(*p3);p3++) {
    if(*p3=='	') {
      p3=find_word_start(p3);
      switch(*p3) {
      case '"': {
        p4=strchr(p3+1,'"');
        goto tested;
      }
      case '(': {
        p4=find_matching(p3+1,')');
        goto tested;
      }
      case '[': {
        p4=find_matching(p3+1,']');
        goto tested;
      }
      case '{': {
        p4=find_matching(p3+1,'}');
        tested:
        if(p4) {
          p3++; *p4=0; break;
        }
        else goto nomatch;
      }
      default: {
        nomatch:
        *find_word_end(p3)=0;
        }
      }
      mystrncpy(p,p3,MAX_LINELEN);
      return;
    }
  }
  *find_word_end(p2)=0;
  mystrncpy(p,p2,MAX_LINELEN);
}

/* internal */
static void _embraced(char buf[], void (app)(char *p))
{
  char *p1, *p2, buf2[MAX_LINELEN+1];
  for(p1=strchr(buf,'{'); p1; p1=strchr(p1,'{')) {
    p2=find_matching(p1+1,'}');
    if(p2==NULL) module_error("unmatched_parentheses");
    *p2=0; mystrncpy(buf2,p1+1,sizeof(buf2)); app(buf2);
    string_modify(buf,p1,p2+1,buf2);
  }
}

static void _embraced_tree(char buf[])
{
  char *p1, *p2, buf2[MAX_LINELEN+1];
  int nb, index;
  for (p1=strchr(buf,'{'); p1; p1=strchr(buf,'{')){
    p2=find_matching(p1+1,'}');
    if(p2==NULL) module_error("unmatched_parentheses");
    *p2=0; nb=itemnum(p1+1); *p2='}'; index=(random()%nb)+1;
    for (p1=strchr(buf,'{'); p1; p1=strchr(p1,'{')){
      p2=find_matching(p1+1,'}');
      if(p2==NULL) module_error("unmatched_parentheses");
      *p2=0; fnd_item(p1+1,index,buf2);
      string_modify(buf,p1,p2+1,buf2);
      p1+=strlen(buf2);
    }
  }
}

/* embraced operations */
void calc_embraced(char *p)
{
  char *p1, *p2, buf[MAX_LINELEN+1];

  p1=find_word_start(p); p2=find_word_end(p1);
  if(*p2==0) {*p=0; return;}
  *p2++=0; p2=find_word_start(p2);
  mystrncpy(buf,p2,sizeof(buf)); substit(buf);
  if(p1>p) ovlstrcpy(p,p1);
  substit(p);
  if(strcmp(p,"randitem")==0) {_embraced(buf,calc_randitem); goto end;}
  if(strcmp(p,"randrow")==0) {_embraced(buf,calc_randrow); goto end;}
  if(strcmp(p,"linkedranditem")==0) {_embraced_tree(buf); goto end;}
  if(strcmp(p,"extract")==0) {
    p1=strchr(buf,'{');
    if(p1!=NULL) {
      p2=find_matching(++p1,'}');
      if(p2!=NULL) {
        memmove(buf,p1,p2-p1); buf[p2-p1]=0;
      }
      else buf[0]=0;
    }
    else buf[0]=0;
    goto end;
  }
  if(strcmp(p,"delete")==0) {
    for(p1=strchr(buf,'{'); p1; p1=strchr(p1,'{')) {
      p2=find_matching(p1+1,'}');
      if(p2) ovlstrcpy(p1,p2+1); else p1++;
    }
    goto end;
  }
  module_error("syntax_error");

  end:
  mystrncpy(p,buf,MAX_LINELEN);
}

void calc_rows2lines(char *p)
{ rows2lines(p); }

void calc_lines2rows(char *p)
{ lines2rows(p); }

/* check whether datamodules exist */
void calc_checkdata(char *p)
{
  char *p1, *p2, buf[MAX_LINELEN+1], nbuf[MAX_FNAME+1];
  struct stat st;

  memmove(p,"yes",4);
  p1=getvar("module_data");
  if(p1==NULL || *p1==0) return;
  snprintf(buf,sizeof(buf),"%s",p1);
  for(p2=buf; *p2; p2++) if(*p2==',' || *p2==';') *p2=' ';
  if(strstr(buf,"..")!=NULL) {
    snprintf(p,MAX_LINELEN,"badly_defined_data_module");
    return;
  }
  for(p1=find_word_start(buf); *p1; p1=find_word_start(p2)) {
    p2=find_word_end(p1); if(*p2) *p2++=0;
    snprintf(nbuf,sizeof(nbuf),"%s/%s/INDEX",module_dir,p1);
    if(stat(nbuf,&st)<0) {
      snprintf(nbuf,sizeof(nbuf),"%s/%s/index",module_dir,p1);
      if(stat(nbuf,&st)<0) {
        snprintf(p,MAX_LINELEN,"%s",p1);
        return;
      }
    }
  }
}

void calc_getexoindex(char *p){
  char *c, *p1, *p2, *p3, *p4;
  char nbuf[MAX_LINELEN+1], fname[MAX_FNAME+1], buf[MAX_LINELEN+1],
    fcontent[MAX_LINELEN+1];
  int i, sheet, start=1, stop=MAX_SHEETS;
  c=getvar("wims_class"); if(c==NULL) return;
  p1=strstr(p, "module="); if(p1==NULL) return;
  p1=find_word_start(p1+strlen("module="));
  p2=find_word_end(p1); *p2=0;
  p4=strstr(p2+1, " sheet=");
  if (p4!=NULL) {*p4=0;start=stop=evalue(p4+strlen(" sheet="));}
  p2=strstr(p2+1, "param="); if(p2==NULL) return;
  p2=find_word_start(p2+strlen("param="));
  p3=find_word_end(p2);*p3=0;

  snprintf(fname,sizeof(fname),"%s/%s/sheets/.severity",class_base,c);
  readfile(fname,fcontent,MAX_LINELEN);
  snprintf(nbuf,sizeof(nbuf),":%s\n%s\n",p1,p2); *p=0;
  for (sheet=start; sheet <= stop; ++sheet){
    snprintf(fname,sizeof(fname),"%s/%s/sheets/.sheet%d",class_base,c,sheet);
    if(readfile(fname,buf,MAX_LINELEN)==NULL) break;
    p1=strstr(buf,nbuf);
    while(p1>buf && *(p1-1)!='\n') p1=strstr(p1+1,nbuf);
    if(p1!=NULL) {
      p2=strchr(buf,':');
      while(p2>buf && *(p2-1)!='\n') p2=strchr(p2+1,':');
      for(i=1;p2!=NULL && p2<p1;i++) {
        p2=strchr(p2+1,':');
        while(p2>buf && *(p2-1)!='\n') p2=strchr(p2+1,':');
      }
      if(p2==NULL) return;
      fnd_line(fcontent,sheet+1,buf);
      if(!buf[0]) getdef("bases/sys/define.conf","DF_SEVERITY",buf);
      snprintf(p,20,"%d/%d,%s",sheet,i,buf);
      break;
    }
  }
}

/* tag!=0 if we don't want automatic substit(). */
MYFUNCTION calc_routine[]={
      {"TeXmath", 0, texmath},
      {"add",  1, calc_sum},
      {"append", 1, calc_append},
      {"call",   0, calc_exec},
      {"char",  1, calc_charof},
      {"charcnt", 0,      calc_lengthof},
      {"charcount", 0,      calc_lengthof},
      {"charno", 0,      calc_lengthof},
      {"charnum", 0,      calc_lengthof},
      {"chars",  1, calc_charof},
      {"checkallpar",0,calc_checkallpar},
      {"checkdata", 0, calc_checkdata},
      {"checkdatamodule",0, calc_checkdata},
      {"checkhost", 0, calc_checkhost},
      {"column", 1, calc_columnof},
      {"columns", 1, calc_columnof},
      {"daemon", 0, calc_daemon},
      {"date",  0, calc_date},
      {"deaccent", 0, deaccent},
      {"debug",  0, calc_debug},
      {"declosing", 0, calc_declosing},
      {"definitionof", 1, calc_defof},
      {"defof",  1, calc_defof},
      {"detag",  0, calc_detag},
/*      {"dictionary", 1, calc_dictionary}, */
      {"dir",  0, calc_listfile},
      {"embraced", 1, calc_embraced},
      {"encyclo", 0, pedia},
      {"encyclopedia", 0, pedia},
      {"eval",  0, calc_evalue},
      {"evalsubst", 1, calc_evalsubst},
      {"evalsubstit", 1, calc_evalsubst},
      {"evalsubstitute",1, calc_evalsubst},
      {"evalue", 0, calc_evalue},
      {"evaluesubst", 1, calc_evalsubst},
      {"evaluesubstit", 1, calc_evalsubst},
      {"evaluesubstitute",1, calc_evalsubst},
      {"examdep", 0, calc_examdep},
      {"examscore", 0, calc_examscore},
      {"exec",   0, calc_exec},
      {"execute", 0, calc_exec},
      {"fileexists", 0, calc_fileexists},
      {"filelist", 0, calc_listfile},
      {"filexists", 0, calc_fileexists},
      {"getdef", 1, calc_defof},
      {"getexoindex", 0, calc_getexoindex},
      {"getopt", 1, calc_getopt},
      {"getscore", 0, calc_getscore},
      {"getscorealltries", 0, calc_getscorealltries},
      {"getscorebest", 0, calc_getscorebest},
      {"getscorelast", 0, calc_getscorelast},
      {"getscorelevel", 0, calc_getscorelevel},
      {"getscoremaxexotry",0,calc_getscoremaxexotry},
      {"getscoremean",0, calc_getscoremean},
      {"getscorepercent",0, calc_getscorepercent},
      {"getscorequality",0, calc_getscoremean},
      {"getscoreremain",0, calc_getscoreremain},
      {"getscorerequire",0, calc_getscorerequire},
      {"getscorestatus",0, calc_getscorestatus},
      {"getscoretry", 0, calc_getscoretry},
      {"getscoreweight",0, calc_getscoreweight},
      {"getseedlast", 0, calc_getseedlast},
      {"getseedlastcnt", 0, calc_getseedlastcnt},
      {"getseedscorelast", 0, calc_getseedscorelast},
      {"getseedscores", 0, calc_getseedscores},
      {"getsheetstatus", 0, calc_getsheetstatus},
      {"hex",  0, calc_hex},
      {"html2iso", 0, calc_html2iso},
      {"htmlmath", 0, htmlmath},
      {"httpquery", 0, tohttpquery},
      {"imgrename", 0, calc_imgrename},
      {"instexst", 1, calc_instexst},
      {"instexstatic", 1, calc_instexst},
      {"item",  1, calc_itemof},
      {"itemcnt", 0,      calc_itemnum},
      {"itemcount", 0,      calc_itemnum},
      {"itemno", 0,      calc_itemnum},
      {"itemnum", 0,      calc_itemnum},
      {"items",  1, calc_itemof},
      {"items2lines", 0,      items2lines},
      {"items2words", 0,      items2words},
      {"itemstolines", 0,      items2lines},
      {"itemstowords", 0,      items2words},
      {"lengthof", 0,      calc_lengthof},
      {"leveldata", 1, calc_leveldata},
      {"levelpoints", 1, calc_leveldata},
      {"line",  1, calc_lineof},
      {"linecnt", 0,      calc_linenum},
      {"linecount", 0,      calc_linenum},
      {"lineno", 0,      calc_linenum},
      {"linenum", 0,      calc_linenum},
      {"lines",  1, calc_lineof},
      {"lines2items", 0,      lines2items},
      {"lines2list", 0,      lines2items},
      {"lines2rows", 0,      calc_lines2rows},
      {"lines2words", 0,      lines2words},
      {"linestoitems", 0,      lines2items},
      {"linestolist", 0,      lines2items},
      {"linestowords", 0,      lines2words},
      {"list2lines", 0,      items2lines},
      {"list2words", 0,      items2words},
      {"listcomplement",1, calc_listcomplement},
      {"listfile", 0, calc_listfile},
      {"listfiles", 0, calc_listfile},
      {"listintersect", 1, calc_listintersect},
      {"listintersection",1, calc_listintersect},
      {"listtolines", 0,      items2lines},
      {"listtowords", 0,      items2words},
      {"listunion", 1, calc_listunion},
      {"listuniq", 0, calc_listuniq},
      {"listunique", 0, calc_listuniq},
      {"listvar", 0, mathvarlist},
      {"lookup", 1, calc_lookup},
      {"lower",  0, calc_tolower},
      {"lowercase", 0, calc_tolower},
      {"ls",  0, calc_listfile},
      {"mailurl", 0, calc_mailurl},
      {"makelist", 1, calc_makelist},
      {"math2html", 0, htmlmath},
      {"math2mathml", 0,mathmlmath},
      {"math2tex", 0, texmath},
      {"mathmlmath", 0, mathmlmath},
      {"mathsubst", 1, calc_mathsubst},
      {"mathsubstit", 1, calc_mathsubst},
      {"mathsubstitute",1, calc_mathsubst},
      {"mexec",  0, calc_mexec},
      {"module", 0, calc_module},
      {"multiply", 1, calc_product},
      {"non_empty", 0, calc_nonempty},
      {"nonempty", 0, calc_nonempty},
      {"nospace", 0, nospace},
      {"nosubst", 1, calc_subst},
      {"nosubstit", 1, calc_subst},
      {"nosubstitute", 1, calc_subst},
      {"passcheck", 1, calc_passcheck},
      {"passcrypt", 0, calc_passcrypt},
      {"pedia",  0, pedia},
      {"perl",  0, calc_perl},
      {"position", 1, calc_pos},
      {"positionof", 1, calc_pos},
      {"positions", 1, calc_pos},
      {"prod",  1, calc_product},
      {"product", 1, calc_product},
      {"randchar", 0, calc_randchar},
      {"randdouble", 0, calc_randdouble},
      {"randfile", 0, calc_randfile},
      {"randfloat", 0, calc_randdouble},
      {"randint",  0, calc_randint},
      {"randitem", 0, calc_randitem},
      {"randline", 0, calc_randline},
      {"random", 0, calc_randdouble},
      {"randperm", 0, calc_randperm},
      {"randpermute", 0, calc_randperm},
      {"randreal", 0, calc_randdouble},
      {"randrecord", 0, calc_randfile},
      {"randrow", 0, calc_randrow},
      {"randword", 0, calc_randword},
      {"rawmath", 0, rawmath},
      {"rawmatrix", 0, rawmatrix},
      {"reaccent", 0, reaccent},
      {"record", 1, calc_recordof},
      {"recordcnt", 0, calc_recordnum},
      {"recordcount", 0, calc_recordnum},
      {"recordno", 0, calc_recordnum},
      {"recordnum", 0, calc_recordnum},
      {"records", 1, calc_recordof},
      {"recursion", 1, calc_recursion},
      {"reinput", 0, calc_reinput},
      {"rename", 0, calc_rename},
      {"replace", 1, calc_replace},
      {"rootof", 1, calc_solve},
      {"row",  1, calc_rowof},
      {"rowcnt", 0,      calc_rownum},
      {"rowcount", 0,      calc_rownum},
      {"rowno",  0,      calc_rownum},
      {"rownum", 0,      calc_rownum},
      {"rows",  1, calc_rowof},
      {"rows2lines", 0, calc_rows2lines},
      {"run",   0, calc_exec},
      {"select", 1, calc_select},
      {"sh",  0, calc_sh},
      {"shuffle", 0, calc_randperm},
      {"singlespace", 0, singlespace},
      {"slashsubst", 0, slashsubst},
      {"solve", 1, calc_solve},
      {"sort", 1, calc_sort},
/*      {"sql", 0, calc_sql}, */
      {"staticinstex", 1, calc_instexst},
      {"stinstex", 1, calc_instexst},
      {"subst", 0, calc_subst},
      {"substit", 0, calc_subst},
      {"substitute", 0, calc_subst},
      {"sum", 1, calc_sum},
      {"system", 0, calc_sh},
      {"texmath", 0, texmath},
      {"text", 1, text},
      {"tohex", 0, calc_hex},
      {"tolower", 0, calc_tolower},
      {"toupper", 0, calc_toupper},
      {"translate", 1, calc_translate},
      {"trim", 0, calc_trim},
      {"unhttp", 0, calc_unhttp},
      {"upper", 0, calc_toupper},
      {"uppercase", 0, calc_toupper},
      {"values", 1, calc_values},
      {"varlist", 0, mathvarlist},
      {"word", 1, calc_wordof},
      {"wordcnt", 0, calc_wordnum},
      {"wordcount", 0,calc_wordnum},
      {"wordno", 0, calc_wordnum},
      {"wordnum", 0, calc_wordnum},
      {"words", 1, calc_wordof},
      {"words2items", 0, words2items},
      {"words2lines", 0, words2lines},
      {"words2list", 0, words2items},
      {"wordstoitems", 0,words2items},
      {"wordstolines", 0,words2lines},
      {"wordstolist", 0, words2items}
};
int CALC_FN_NO=(sizeof(calc_routine)/sizeof(calc_routine[0]));
