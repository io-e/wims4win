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

/* Check type of a file */

/*************** Customization: change values hereafter ****************/

/* limit of data buffers */
#define buflim 1024*1024*16

/***************** Nothing should need change hereafter *****************/

#include "../Lib/libwims.h"

#define hmsame "\\CC\\Delta\\Gamma\\Lambda\\NN\\Omega\\Phi\\Pi\\Psi\\QQ\\RR\\Sigma\\Xi\\ZZ\\alpha\\beta\\cap\\chi\\cup\\delta\\div\\eta\\exists\\forall\\gamma\\in\\infty\\iota\\kappa\\lambda\\mu\\nabla\\nu\\omega\\pi\\pm\\psi\\rho\\sigma\\subset\\subseteq\\tau\\theta\\times\\varepsilon\\varphi\\xi\\zeta\\"

char fn1[1024]="", fn2[1024]="";
char mathbuf[MAX_LINELEN+1];
char *filebuf;
int filelen=0;
int latex2html=0;
FILE *outf;

struct {
    char *name, *trans;
} backtrans[]={
    {"\\ge\\", " >= "},
    {"\\geq\\", " >= "},
    {"\\le\\", " <= "},
    {"\\leq\\", " <= "},
    {"\\to\\", " -> "},
    {"\\rightarrow\\", " -> "},
    {"\\longrightarrow\\", " --> "},
    {"\\Rightarrow\\", " => "},
    {"\\Longrightarrow\\", " ==> "},
    {"\\Leftrightarrow\\", " <=> "},
    {"\\Longleftrightarrow\\", " <==> "},
    {"\\Longleftarrow\\", " <== "},
};

#define backtransno (sizeof(backtrans)/sizeof(backtrans[0]))

char *find_tag_end(char *p)
{
  char *pp, *old;
  pp=p; if(*pp=='<') pp++;
  for(; *pp && *pp!='>'; pp++) {
    if(*pp=='"') {
      pp=strchr(pp+1,'"');
      if(pp==NULL) {pp=p+strlen(p); break;} else continue;
    }
  }
/* this is probably an syntax error of the page */
  if(*pp==0 && pp>p+2048) {
    old=p; if(*old=='<') old++;
    pp=strchr(old,'>');
    if(pp==NULL) pp=strchr(old,'<');
    if(pp==NULL) pp=find_word_end(find_word_start(old));
  }
  if(*pp=='>') pp++;
  return pp;
}

char *find_tag(char *p, char *tag)
{
  char *pp;
  int len;
  len=strlen(tag);
  for(pp=strchr(p,'<'); pp!=NULL && *pp; pp=strchr(pp+1,'<')) {
    if(strncasecmp(pp+1,tag,len)==0 && !isalnum(*(pp+1+len))) return pp;
  }
  return p+strlen(p);
}

void cutamp(char *p)
{
  char *pp;
  for(pp=strchr(p,'&'); pp; pp=strchr(pp+1,'&')) {
    if(strncmp(pp,"&amp;",5)==0) {
      ovlstrcpy(pp+1,pp+5); continue;
    }
    if(strncmp(pp,"&lt;",4)==0) {
      *pp='<'; ovlstrcpy(pp+1,pp+4); continue;
    }
    if(strncmp(pp,"&gt;",4)==0) {
      *pp='>'; ovlstrcpy(pp+1,pp+4); continue;
    }

  }
}

/* get the file */
void prepare_file(void)
{
  FILE *f;
  long int flen;

  filelen=0;
  f=fopen(fn1,"r"); if(f==NULL) return;
  fseek(f,0,SEEK_END);flen=ftell(f); fseek(f,0,SEEK_SET);
  if(flen>buflim) return;
  filebuf=xmalloc(2*flen+1024);flen=fread(filebuf,1,flen,f);
  fclose(f);
  if(flen<0 || flen>=buflim) flen=0;
  filebuf[flen]=0;
  filelen=flen;
  outf=fopen(fn2,"w"); if(outf==NULL) return;
}

void getmath(char *p)
{
  char *pt, *pv;
  char *p1, *p2, buf[256];

  mathbuf[0]=0;
  pt=find_word_start(p);
  if(strncmp(pt,"\\begin{displaymath}",
             strlen("\\begin{displaymath}"))==0) {
    pt=strchr(pt,'}')+1;
    pv=strstr(pt,"\\end{displaymath}");
    if(pv==NULL) return;
    goto insmath;
  }
  if(*pt=='%') pt=strchr(pt,'$');
  if(pt==NULL) return;
  if(*pt!='$') return;
  do pt++; while(*pt=='$');
  pv=strchr(pt,'$'); if(pv==NULL) return;
  insmath: if(pv-pt>=MAX_LINELEN-256) return;
  memmove(mathbuf,pt,pv-pt); mathbuf[pv-pt]=0;
  if(strstr(mathbuf,"...\n...")!=NULL) {
    ovlstrcpy(mathbuf,"......"); return;
  }
  cutamp(mathbuf); latex2html=1;
  for(p1=strstr(mathbuf,"\\mathbb");p1;p1=strstr(p1+1,"\\mathbb")) {
    char c,*d;
    p2=find_word_start(p1+strlen("\\mathbb")); c=0;
    if(strchr("NZQRC",*p2)!=NULL) c=*p2;
    else
      if(*p2=='{' && *(p2+2)=='}' && strchr("NZQRC",*(p2+1))!=NULL) {
        c=*(p2+1); p2+=2;
      }
    if(c) {
      p2=find_word_start(++p2);
      if(isalnum(*p2)) d=" "; else d="";
      string_modify(mathbuf,p1,p2,"\\%c%c%s",c,c,d);
    }
  }
  for(p1=strstr(mathbuf,"{\\"); p1; p1=strstr(p1+1,"{\\")) {
    if(p1>mathbuf && isalpha(*(p1-1))) continue;
    for(p2=p1+2; p2<p1+24 && isalpha(*p2); p2++);
    if(*p2!='}' || isalnum(*(p2+1))) continue;
    memmove(buf,p1+1,p2-p1-1); buf[p2-p1-1]='\\'; buf[p2-p1]=0;
    if(strstr(hmsame,buf)==NULL) continue;
    ovlstrcpy(p2,p2+1); ovlstrcpy(p1,p1+1);
  }
  if(strstr(mathbuf,"\\begin{")!=NULL) return;
  for(p1=strchr(mathbuf,'{'); p1; p1=strchr(p1+1,'{')) {
    if((p1>mathbuf && isalpha(*(p1-1))) ||
       !isalnum(*(p1+1)) || *(p1+2)!='}') continue;
    *p1=*(p1+1); ovlstrcpy(p1+1,p1+3);
  }
  if(strchr(mathbuf,'[')!=NULL) {
    char mbuf[MAX_LINELEN+1];
    snprintf(mbuf,sizeof(mbuf),"{%s}",mathbuf);
    ovlstrcpy(mathbuf,mbuf);
  }
/* try to simplify */
  if(strchr(mathbuf,'{')==NULL && strchr(mathbuf,'\\')!=NULL) {
    int i, tt;
    tt=0;
    for(p1=strchr(mathbuf,'\\'); p1; p1=strchr(p1+1,'\\')) {
      for(p2=p1+1;isalpha(*p2);p2++);
      if(p2==p1+1 || p2>p1+24) {tt=1; break;}
      memmove(buf,p1,p2-p1);buf[p2-p1]='\\';buf[p2-p1+1]=0;
      for(i=0;i<backtransno && strcmp(buf,backtrans[i].name)!=0;i++);
      if(i>=backtransno && strstr(hmsame,buf)==NULL) {
        tt=1; break;
      }
    }
    if(tt==0) {
      for(p1=strchr(mathbuf,'\\'); p1; p1=strchr(p1+1,'\\')) {
        for(p2=p1+1;isalpha(*p2);p2++);
        if(p2==p1+1 || p2>p1+24) break;
        memmove(buf,p1,p2-p1);buf[p2-p1]='\\';buf[p2-p1+1]=0;
        for(i=0;i<backtransno && strcmp(buf,backtrans[i].name)!=0;i++);
        if(i<backtransno)
          string_modify(buf,p1,p2,backtrans[i].trans);
        else *p1=' ';
      }
    }
  }
}

void output(void)
{
  char *p, *pp, *p2, *pt;
  char buf[MAX_LINELEN+1];
  p=filebuf;
  restart:
  pp=find_tag(p,"body");
  if(*pp!=0) {
    p=find_tag_end(pp); goto restart;
  }
  pp=find_tag(p,"html");
  if(*pp!=0) {
    p=find_tag_end(pp); goto restart;
  }
  *find_tag(p,"/body")=0; *find_tag(p,"/html")=0;
  for(pp=strstr(p,"\n\n"); pp; pp=strstr(pp+1,"\n\n")) *pp=' ';
  for(pp=strchr(p,'<');pp!=NULL;pp=strchr(find_tag_end(pp),'<')) {
    if(pp>p) {fwrite(p,1,pp-p,outf); p=pp;}
    if(latex2html && strncasecmp(pp,"<br><hr>",8)==0 &&
       *find_word_start(pp+8)==0) break;
    if(strncasecmp(pp+1,"!-- MATH",8)==0) {
      p2=strstr(pp+8,"-->"); if(p2==NULL) continue;
      *p2=0; getmath(pp+9); *p2='-';
      p=p2+3; pt=find_word_start(p);
      if(mathbuf[0] && strncasecmp(pt,"<IMG",4)==0 && isspace(*(pt+4))) {
        p=find_tag_end(pt); pp=pt;
        fprintf(outf,"\\(%s\\)",mathbuf);
      }
      continue;
    }
    if(strncasecmp(pp+1,"a",1)==0 && isspace(*(pp+2))) {
      continue;
    }
    if(strncasecmp(pp+1,"img",3)==0 && isspace(*(pp+4))) {
      p2=find_tag_end(pp);
      if(p2-pp>=MAX_LINELEN-256) continue;
      memmove(buf,pp+1,p2-pp-2); buf[p2-pp-2]=0;
      pt=strstr(buf,"ALT=\""); if(pt==NULL) pt=strstr(buf,"alt=\"");
      if(pt!=NULL) {
        pt+=strlen("ALT=\"");
        getmath(pt); if(mathbuf[0]) {
          fprintf(outf,"\\(%s\\)",mathbuf); p=p2;
        }
      }
    }
  }
  if(pp==NULL) fprintf(outf,"%s",p);
}

int main(int argc, char *argv[])
{
  char *p, *pp;
  char *mod;

  mod=getenv("w_module");
  if(mod!=NULL && strncmp(mod,"adm/",4)!=0 && strcmp(mod,"home")!=0) return 1;
  if(mod==NULL) p=argv[1]; else p=getenv("wims_exec_parm");
  if(p==NULL || *p==0) return 1;
  p=find_word_start(p); pp=find_word_end(p);
  if(pp<=p || pp-p>sizeof(fn1)-1) return 1;
  memmove(fn1,p,pp-p); fn1[pp-p]=0;
  p=find_word_start(pp); pp=find_word_end(p);
  if(pp<=p || pp-p>sizeof(fn2)-1) ovlstrcpy(fn2,fn1);
  else {memmove(fn2,p,pp-p); fn2[pp-p]=0;}
  prepare_file();
  output();
  fclose(outf);
  return 0;
}
