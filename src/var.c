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

/* Low-level variable management routines. */
#include "wims.h"

#define VARBUF_LEN (256*1024)
#define VARBUF_NUM 16
#define VARNAME_LEN 32768
#define VARNUM_LIMIT 4096
#define vb_internal 1
#define vb_ro 2
#define vb_dirty 4
#define vb_noexport 8
#define vb_hacked 16

char *_varbuf[VARBUF_NUM]; /* main variable value buffer */
char *_varptr;
int _varbufcurr;
char *_varnamebuf, *_varnameptr; /* variable name buffer */

/* wims_ variables that should be exported */
char *exportvars[]={
      "class",
      "deposit",
      "home",
      "inssub",
      "sesdir",
      "session",
      "site_languages",
      "supervisor",
      "supervisormail",
      "tmp_debug",
      "user",
      "version",
};
int exportvarcnt=(sizeof(exportvars)/sizeof(exportvars[0]));

struct vartab {
  char *name, *val;
  unsigned short int vlen;
  char tag,lvl;
} fastvartab[128], mainvartab[VARNUM_LIMIT];

/* int vlenmin[]={0,1,20,45,100,200,400,800,1500,3000,6000}; */
int vlenmax[]={1,32,64,128,256,512,1024,2048,4096,8192,MAX_LINELEN+1};
int freelim[]={1,500,500,300,200,150,100,80,60,50,30};
#define lvlno (sizeof(vlenmax)/sizeof(vlenmax[0]))
#define freevtot (1+500+500+300+200+150+100+80+60+50+30)

int freevars[lvlno], freevstart[lvlno];
int freevcnt=0,bufvcnt=0;
char *freevbuf[freevtot];
int mainvarcnt=0;
void add_hack(char *name);

void get_var_privileges(void)
{
  int sav=untrust;
  untrust=0;
  var_readable=getvar("wims_readable");
  var_writable=getvar("wims_writable");
  var_nr=getvar("wims_nr");
  var_nw=getvar("wims_nw");
  var_pfx=getvar("wims_prefix");
  untrust=sav;
}

/* Returns 1 if rights OK. Else 0. */
int var_checkright(char *name, char *allow, char *deny, char *prefix)
{
  if(strcmp(name,"wims_read_parm")==0) return 1;
  if(allow!=NULL && allow[0]!=0 && wordchr(allow,name)!=NULL) return 1;
  if(deny!=NULL && deny[0]!=0 && wordchr(deny,name)!=NULL) return 0;
  if(prefix!=NULL && prefix[0]!=0) {
   char *p, buf[MAX_NAMELEN+1];
   for(p=name; *p!=0 && *p!='_'; p++);
   if(*p!='_' || p-name>=MAX_NAMELEN) return 0;
   memmove(buf,name,p-name); buf[p-name]=0;
   if(wordchr(prefix,buf)==NULL) return 0;
  }
  return 1;
}

/* initialisation */
void var_init(void)
{
  int i,j;
  memset(freevars,0,sizeof(freevars));
  _varbufcurr=0;
  _varbuf[0]=xmalloc(VARBUF_LEN);
  _varnamebuf=xmalloc(VARNAME_LEN);
  _varptr=_varbuf[0]; _varnameptr=_varnamebuf;
  for(i=j=0;i<lvlno;i++) {freevstart[i]=j; j+=freelim[i];}
  if(j!=freevtot) internal_error("var.c: wrong count freevtot!");
}

void exportall(void)
{
  int i, j, tag; char buf[MAX_NAMELEN+17];
  char *p, *p1, *p2;
  char *noexpword[1024];
  int noexplen[1024];
  int noexpcnt;

  noexpcnt=0;
  p=getvar("wims_noexport"); if(p==NULL) p="";
  for(p1=find_word_start(p),noexpcnt=0;
      *p1 && noexpcnt<1024;
      p1=find_word_start(p2)) {
    p2=find_word_end(p1);
    noexpword[noexpcnt]=p1; noexplen[noexpcnt]=p2-p1;
    noexpcnt++;
  }
  for(i=0;i<mainvarcnt;i++) {
    tag=mainvartab[i].tag;
    if((tag&vb_dirty)==0 || (tag&vb_noexport)!=0) continue;
    p=mainvartab[i].name;
    for(j=0;j<noexpcnt;j++) if(strncmp(p,noexpword[j],noexplen[j])==0) {
      mainvartab[i].tag|=vb_noexport;
      goto lend;
    }
    if(mainvartab[i].tag&vb_hacked) {
      add_hack(p); mainvartab[i].tag&=~vb_hacked;
    }
    snprintf(buf,sizeof(buf),"%s%s",var_prefix,p);
    setenv(buf,mainvartab[i].val,1);
    mainvartab[i].tag&=~vb_dirty;
    lend:  ;
  }
}

char *vaskbuf(int lvl)
{
  char *p;
  if(lvl>=lvlno) {  /* should never occur */
    module_error("Internal_variable_length_overflow"); return NULL;
  }
  if(freevars[lvl]>0) {
    freevars[lvl]--; freevcnt--;
    return freevbuf[freevstart[lvl]+freevars[lvl]];
  }
  if(_varptr+vlenmax[lvl]>=_varbuf[_varbufcurr]+VARBUF_LEN) {
    if(_varbufcurr>=VARBUF_NUM) {
      module_error("variable_buffer_overflow"); return NULL;
    }
    _varbufcurr++; _varptr=_varbuf[_varbufcurr]=xmalloc(VARBUF_LEN);
  }
  bufvcnt++;
  p=_varptr; _varptr+=vlenmax[lvl]; return p;
}

/* returns 1 if free succeeded. */
int vfreebuf(int lvl, char *p)
{
  if(p>=_varptr) return 0;
  if(p+vlenmax[lvl]==_varptr) {_varptr=p; bufvcnt--;return 1;}
  if(lvl<=0 || lvl>=lvlno || freevars[lvl]>=freelim[lvl]) return 0;
  freevcnt++;
  freevbuf[freevstart[lvl]+freevars[lvl]]=p; freevars[lvl]++; return 1;
}

int _char_int(char *vn)
{
  int t; char v;
  v=vn[0]; if(vn[1]) t=64; else t=0;
  if(v>='0' && v<='9') return v-'0'+t;
  else {
    if(v>='A' && v<='Z') return v-'A'+10+t;
    else {
      if(v>='a' && v<='z') return v-'a'+10+26+t;
      else return -1;
    }
  }
}

int var_ins(char *name,int inspoint)
{
  int i, nlen, tag;
  if(mainvarcnt>=VARNUM_LIMIT) {
    module_error("too_many_variable_names"); return -1;
  }
  for(nlen=0;nlen<=MAX_NAMELEN && (myisalnum(name[nlen]) || name[nlen]=='_');nlen++);
  if(nlen<=1 || nlen>MAX_NAMELEN || name[nlen]!=0) return -1;
  if(_varnameptr+nlen+1>_varnamebuf+VARNAME_LEN) {
    module_error("variable_name_buffer_overflow"); return -1;
  }
  tag=0;
  if(search_list(ro_name,RO_NAME_NO,sizeof(ro_name[0]),name)>=0)
    tag|=vb_ro;
  if(nlen<=2 || strncmp(name,mathfont_prefix,strlen(mathfont_prefix))==0 ||
      name[nlen-1]=='_')
    tag|=vb_noexport;
  if(var_noexport) tag|=vb_noexport;
  if(name[0]=='w' && strncmp(name,wims_prefix,wpflen)==0) {
    if(search_list(exportvars,exportvarcnt,sizeof(exportvars[0]),name+wpflen)<0)
      tag|=vb_noexport;
   if(strncmp(name+wpflen,"priv_",strlen("priv_"))==0 ||
        search_list(internal_name,INTERNAL_NAME_NO,sizeof(internal_name[0]),name+wpflen)>=0)
      tag|=vb_internal;
  }
  i=inspoint; if(i>mainvarcnt) i=mainvarcnt; if(i<0) i=0;
  memmove(mainvartab+i+1,mainvartab+i,(mainvarcnt-i)*sizeof(mainvartab[0]));
  mainvarcnt++; nlen++;
  memmove(_varnameptr,name,nlen);
  mainvartab[i].name=_varnameptr; _varnameptr+=nlen;
  mainvartab[i].val=NULL; mainvartab[i].vlen=0;
  mainvartab[i].tag=tag;
  mainvartab[i].lvl=0; return i;
}

char *fast_getvar(char *vname)
{
  int n;

  n=_char_int(vname); if(n<0) return "";
  if(fastvartab[n].tag&vb_hacked) {
    add_hack(vname); fastvartab[n].tag&=~vb_hacked;
  }
  getvar_len=fastvartab[n].vlen;
  return fastvartab[n].val;
}

int _setvar_(struct vartab *vtb, char *vval)
{
  int l,lvl;
  char *p;
  if(vval) l=strlen(vval); else l=0;
  if(l>MAX_LINELEN) return 1;
  lvl=vtb->lvl;
  if(l==0) {
    if(lvl>0) {
      vfreebuf(lvl,vtb->val); vtb->tag|=vb_dirty;
      if(var_hacking) vtb->tag|=vb_hacked; else vtb->tag&=~vb_hacked;
      vtb->lvl=0; vtb->vlen=0;
    }
    if(vval) vtb->val=""; else vtb->val=NULL;
   return 0;
  }
  if(l>=vlenmax[lvl]) {
    vfreebuf(lvl,vtb->val);
    do lvl++; while(l>=vlenmax[lvl]);
    vtb->lvl=lvl;
    vtb->val=p=vaskbuf(lvl);
    }
  else p=vtb->val;
  vtb->vlen=l; vtb->tag|=vb_dirty;
  if(var_hacking) vtb->tag|=vb_hacked; else vtb->tag&=~vb_hacked;
  memmove(p,vval,++l); return 0;
}

int fast_setvar(char *vname, char *vval)
{
  int n;

  n=_char_int(vname); if(n<0) return 1;
  return _setvar_(fastvartab+n,vval);
}

/* internal constant */
int setvar_force=0;

/* Set a user variable. Now it no longer uses environment.
 * Returns 0 if OK. */
int setvar(char *vname, char *vvalue)
{
  int i,overwrite,tag;

  if(vname[0]==0) return 1;
  if((vname[1]==0 || (vname[1]=='_' && vname[2]==0)))
    return fast_setvar(vname,vvalue);
  i=search_list(mainvartab,mainvarcnt,sizeof(mainvartab[0]),vname);
  if(i<0) i=var_ins(vname,~i);
  if(i<0) return 1; /* error */
  overwrite=1; tag=mainvartab[i].tag;
  if(setvar_force==0 && confset==0) {
/* user file has no right to modify wims variables. */
    if((untrust&6)!=0) {
      if(var_checkright(vname,var_writable,var_nw,var_pfx)==0)
        return 1;
      if(strncmp(vname,wims_prefix,wpflen)==0 &&
          strcmp(vname,"wims_read_parm")!=0) return 1;
    }
    if((tag&vb_ro)!=0) overwrite=0;
    else if((tag&vb_internal)!=0 && !trusted_module()) return 1;
  }
  if(!overwrite && mainvartab[i].val!=NULL) return 1;
  _setvar_(mainvartab+i,vvalue);
  if(vname[0]=='w') {
    if(strcmp(vname,"wims_print_precision")==0) {
      int a=atoi(vvalue);
      if(a>0 && a<100) print_precision=a;
    }
    if(strcmp(vname,"wims_backslash_insmath")==0) {
      if(strcasecmp(vvalue,"yes")==0) backslash_insmath=1;
      else backslash_insmath=0;
    }
  }
  if (trace_file && wordchr(tmp_debug_var,vname)) {
    fprintf(trace_file,"\n%s='%s'\n",vname,vvalue);
  }
  return 0;
}

int force_setvar(char *vname,char *vvalue)
{
  int i;
  setvar_force=1; i=setvar(vname,vvalue); setvar_force=0; return i;
}

void unsetvar(char *vname)
{
  int i;
  if(vname[0]!=0 && (vname[1]==0 || (vname[1]=='_' && vname[2]==0))) {
    fast_setvar(vname,NULL); return;
  }
  i=search_list(mainvartab,mainvarcnt,sizeof(mainvartab[0]),vname);
  if(i>=0) _setvar_(mainvartab+i,NULL);
}

/* Get a variable's value. */
char *_getvar(char *vname)
{
  char *val;
  int i;

  if((untrust&4)!=0 || vname[0]==0) return ""; /* called from !readdef */
  if(vname[1]==0 || (vname[1]=='_' && vname[2]==0)) return fast_getvar(vname);
  if((untrust&6)!=0) {
    if(var_checkright(vname,var_readable,var_nr,var_pfx)==0)
      return "";
  }
  i=search_list(mainvartab,mainvarcnt,sizeof(mainvartab[0]),vname);
  if(i<0) val=NULL; else {
    if(mainvartab[i].tag&vb_hacked) {
      add_hack(vname); mainvartab[i].tag&=~vb_hacked;
    }
    val=mainvartab[i].val; getvar_len=mainvartab[i].vlen;
  }
  if(vname[0]=='w' && strcmp(vname,"wims_incremental")==0) {
    static char buf[32];
    if(val==NULL) i=0;
    else i=atoi(val)+1;
    mystrncpy(buf,int2str(i),sizeof(buf));
    force_setvar(vname,buf); getvar_len=strlen(buf); val=buf;
  }
  return val;
}

char *getvar(char *vname)
{
  char *val;
  getvar_len=0; val=_getvar(vname);
  if(val==NULL && memcmp(vname,mathfont_prefix,strlen(mathfont_prefix))==0) {// mathfont_prefix m_ is add
    val=mathfont(vname);
    if(val) getvar_len=strlen(val); else getvar_len=0;
  }
  if (trace_file && wordchr(tmp_debug_use_var,vname)) {
    fprintf(trace_file,"\nuse %s:'%s'\n",vname,val);
  }
  return val;
}

/* Search variables with numeric subscripts, from beg to end.
 * Store result to pbuf. Returns number of variables found.
 */
int varsuite(char *stem, int beg, int end, char *pbuf[], int pbuflen)
{
  int i,t,l,v;
  i=search_list(mainvartab,mainvarcnt,sizeof(mainvartab[0]),stem);
  if(i<0) i=~i;
  for(t=0,l=strlen(stem);i<mainvarcnt && t<pbuflen;i++){
    if(strncmp(mainvartab[i].name,stem,l)!=0) break;
    v=atoi(mainvartab[i].name+l); if(v<beg || v>end) continue;
    if(mainvartab[i].val==NULL || mainvartab[i].val[0]==0) continue;
    pbuf[t++]=mainvartab[i].name;
  }
  return t;
}

/* output debug information */
void debug_output(void)
{
  long int endmtime2, time1;
  struct timeval tv;
  struct rusage us;

  if(noout || robot_access || strcasecmp(tmp_debug,"yes")!=0 ||
     checkhost(manager_site)<1) return;
  if(instex_cnt>0) instex_flush();
  if(gettimeofday(&tv,NULL)) endmtime2=0;
  else endmtime2=((tv.tv_sec%1000)*1000000+tv.tv_usec);
  endmtime2=(endmtime2-startmtime2)/100;
  time1=0;
  if(getrusage(RUSAGE_SELF,&us)==0) {
    time1+=us.ru_utime.tv_sec*1000+us.ru_utime.tv_usec/1000;
    time1+=us.ru_stime.tv_sec*1000+us.ru_stime.tv_usec/1000;
  }
  if(getrusage(RUSAGE_CHILDREN,&us)==0) {
    time1+=us.ru_utime.tv_sec*1000+us.ru_utime.tv_usec/1000;
    time1+=us.ru_stime.tv_sec*1000+us.ru_stime.tv_usec/1000;
  }
  snprintf(tmplbuf,sizeof(tmplbuf),"%d,%d,%d,%.4f,%.2f,%d,%d,%d",
        (int)(_varptr-_varbuf[_varbufcurr])+_varbufcurr*VARBUF_LEN,
        bufvcnt-freevcnt,freevcnt,
        (double) endmtime2/10000, (double) time1/1000,
        mcachecnt,mfilecnt,execnt);
  setvar("wims_debug_info",tmplbuf);
  lastout_file=-1; phtml_put_base("debug.phtml",0);
}

/* Add to list of hacked variables */
void add_hack(char *name)
{
  char buf[MAX_LINELEN+1];
  char *p;
  p=getvar("wims_hacked_variables"); if(p==NULL) buf[0]=0;
  else snprintf(buf,sizeof(buf),"%s",p);
  p=buf+strlen(buf);
  if(p-buf >= sizeof(buf)-strlen(name)-4) return; /* too long */
  if(p>buf) snprintf(p,sizeof(buf)-(p-buf),", %s",name);
  else snprintf(buf,sizeof(buf),"%s",name);
  setvar("wims_hacked_variables",buf);
}
