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

 /* This program translates doc format into wims internal data format. */

#define MAX_TITLEN  80

#define MAX_PARM    10
#define MAX_PARAM   1024
#define MAX_DEFINE  1024

#include "../Lib/libwims.h"
#include "msg2wims.h"

struct {
  char *name;
  int serial;
  int type;
} param[MAX_PARAM+32];

char *inbuf;
FILE *inf, *outf;
int inlen=0,outlen=0;
int primserial=1;
char *primitive_dir="docu/primitives";

void internal_warn(char *p)
{}

void get_inf(char *fname)
{
  int i,l;
  inf=fopen(fname,"r");
  if(inf==NULL) bailout(0,0,"");
  fseek(inf,0,SEEK_END); l=ftell(inf); fseek(inf,0,SEEK_SET);
  if(l<=0) bailout(0,0,"");
  inbuf=xmalloc(l+16);
  l=fread(inbuf,1,l,inf);
  if(l<=0) bailout(0,0,"error opening input file");
  else inbuf[l]=0;
  fclose(inf); inlen=l;
  for(i=0;i<l;i++)
    if(inbuf[i]==elsechar || inbuf[i]==endifchar ||
     inbuf[i]==nextchar || inbuf[i]==whilechar) inbuf[i]=' ';
}

void open_outf(char *fname)
{
  outf=fopen(fname,"w");
  if(outf==NULL) bailout(inlen,0,"error opening output file");
}

void process_formula(char *p)
{
  char *p3, bf[MAX_LINELEN+1];

  if(strlen(p)>=MAX_LINELEN)
    bailout(inlen,0,"formula too long");
  while((p3=strstr(p,"&lt;"))!=NULL) memmove(p3," <  ",4);
  while((p3=strstr(p,"&gt;"))!=NULL) memmove(p3," >  ",4);
  for(p3=strchr(p,'\n'); p3!=NULL; p3=strchr(p3,'\n')) *p3=' ';
  snprintf(bf,sizeof(bf),"%s",p);
  if(strchr(bf,'\\')==NULL && strchr(bf,'}')==NULL && strlen(bf)>2) {
    for(p3=strstr(bf,".."); p3!=NULL; p3=strstr(p3,"..")) {
      if(*(p3+2)=='.' || *(p3+2)==',') {
        do p3++; while(*p3=='.'); continue;
      }
      *p3=','; *(p3+1)=' ';
    }
  }
  fprintf(outf,"\n!insmath %s\n",bf);
}

int main(int argc, char *argv[])
{
  char *p, *p1, *p2, *tend;
  sp_error=msg_error;
  string_modify=string_modify2;
  substitute=substit;
  if(argc==2 && strcmp(argv[1],"table")==0) {
    if(verify_order(specialfn, specialfn_no, sizeof(specialfn[0]))) return -1;
    puts("Table orders OK."); return 0;
  }
 /* is defined in public_html/scripts/docu/mkindex and in public_html/modules/adm/doc/var.proc */
  p=getenv("w_msg2wims_primitives"); if(p!=NULL) {
    snprintf(primbuf,sizeof(primbuf),"%s",p);
    for(p=primbuf; *p; p++) if(!isalnum(*p)) *p=' ';
    p=find_word_start(primbuf);
    for(primcnt=0; primcnt<256 && *p; primcnt++, p=find_word_start(p1)) {
      p1=find_word_end(p); if(*p1) *p1++=0;
      primitive[primcnt]=p;
    }
    if(primcnt>0) qsort(primitive,primcnt,sizeof(primitive[0]),_scmp);
  }
  if(argc<3) bailout(0,0,"missing file names");
  p1=argv[1]; p2=argv[2];
  get_inf(p1); open_outf(p2);
  for(p=tend=inbuf;*p;p++) {
    switch(*p) {
      case '$': fputs("&#36;",outf); break;
      case '!': fputs("&#33;",outf); break;
      case ':': fputs("&#58;",outf); break;

      case elsechar: {
        if(primcnt>0) fputs("\n!else\n",outf);
        else fputc(*p,outf);
        break;
      }
      case endifchar: {
        if(primcnt>0) fputs("\n!endif\n",outf);
        else fputc(*p,outf);
        break;
      }
      case nextchar: {
        if(primcnt>0) fputs("\n!next\n",outf);
        else fputc(*p,outf);
        break;
      }
      case whilechar: {
        if(primcnt>0) fputs("\n!endwhile\n",outf);
        else fputc(*p,outf);
        break;
      }
      /* lines begining by > are in blockquote class="wims_citation" */
      case '\n': {
        if(*(p+1)=='>') {
          pre: p++; fputs("\n<blockquote class=\"wims_citation\"><pre>",outf);
          for(p1=strchr(p,'\n'); p1!=NULL && *(p1+1)=='>';
            p1=strchr(++p1,'\n'));
          if(p1!=NULL) *p1++=0; else p1=p+strlen(p);
          for(p2=p;*p2;p2++) {
            if(*p2!='<') fputc(*p2,outf);
            else fputs("&lt;",outf);
          }
          fputs("</pre></blockquote>\n",outf);
          p=p1-1; break;
        }
        /* two successive lines are replaced by an <br> */
        if(*(p+1)!='\n') {fputc(*p,outf);break;}
        p++; fputs("\n<br>\n",outf);
        while(*(p+1)=='\n') p++;
        if(*(p+1)=='>') goto pre;
        break;
      }
      case '<': {
        char *p2;
        if(tend>p || (!isalpha(*(p+1)) && *(p+1)!='!')) {
          fputc(*p,outf); break;
        }
        p2=find_tag_end(p);
        if(!*p2) {msg_error("open_tag"); p2--;}
        tend=p2;
        fputc(*p, outf); break;
      }
      /* interpretation of variables */
      case '\\': {
        char *pe;
        p++;
        if(isalpha(*p)) {
          if(primcnt>0) {
            pe=doccheck(p);
            if(pe>p) {p=pe-1; break;}
          }
          for(pe=p;isalnum(*pe) || *pe=='_';pe++);
          if(*pe=='[') {
            char *pv=find_matching(pe+1,']');
            if(pv!=NULL) {
              char c=*p;
              memmove(p,p+1,pv-p); *pv=')';
              fprintf(outf,"$(m_%c",c); p--; break;
            }
          }
          fprintf(outf,"$m_%c",*p); break;
        }
        switch(*p) {
          case '\\': fputc(*p,outf); break;
          case '{':
          case '(': {
            char *p2, *p3, c;
            if(*p=='{') c='}'; else c=')';
            p++; p2=find_matching(p,c);
            if(c==')') p3=strstr(p,"\\)");
            else p3=strstr(p,"\\}");
            if((p2==NULL && p3!=NULL) ||
               (p2!=NULL && p3!=NULL && p3<p2)) p2=p3+1;
              if(p2==NULL) fputc(*p,outf);
            else {
              *p2=0; if(*(p2-1)=='\\') *(p2-1)=0;
              process_formula(p); p=p2;
            }
            break;
          }
          default: fputc(*p,outf); break;
        }
        break;
      }

      default: fputc(*p,outf); break;
    }
  }
  fputc('\n',outf);
  outlen=ftell(outf);  fclose(outf); bailout(inlen,outlen,"");
  return 0;
}
