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
#include "../Lib/libwims.h"
#include "sp.h"
void (*sp_error)(char *s);

char *setpre="";

void sp_asis(char *p, int ptype)
{
  fprintf(outf,"%stmp%d=!nosubst %s\n\n",setpre,prepcnt,p);
}

void sp_evalue(char *p, int ptype)
{
  char *p1, *p2, *pe;

  p1=strparchr(p,',');
  if(p1==NULL) {
    fprintf(outf,"%stmp%d=%s\n\n",setpre,prepcnt,p);
    return;
  }
  *p1++=0; fprintf(outf,"%sevaltmp=%s\n\n", setpre,p);
  for(; p1!=NULL; p1=p2){
    p1=find_word_start(p1);
    p2=strparchr(p1,',');
    if(p2!=NULL) *p2++=0;
    pe=strchr(p1,'=');
    if(pe==NULL) continue;
    *pe++=0; *find_word_end(p1)=0;
    if(*p1==0) continue;
    fprintf(outf,"%sevaltmp=!mathsubst %s=(%s) in $evaltmp\n",
          setpre,p1,pe);
  }
  fprintf(outf,"%stmp%d=($evaltmp)\n",setpre,prepcnt);
}

/* simple roots.*/
void sp_solve(char *p, int ptype)
{
  fprintf(outf,"%stmp=!replace internal .. by , in %s \n\n\
!distribute items $tmp into tmp1,tmp2,tmp3\n\
%stmp2=!replace internal = by , in $tmp2\n\
!distribute items $tmp2 into tmp4,tmp5\n\
%stmp%d=!solve $tmp1 for $tmp4 = $tmp5 to $tmp3 \n",
          setpre,p,setpre,setpre,prepcnt);
}

/* use maxima to do formal derivation. */
void sp_diff(char *p, int ptype)
{
  fprintf(outf,"%stmp=!translate ;\";': to $     $ in %s\n\n\
%stmp=!lower $tmp\n\
%stmp%d=!exec maxima diff($tmp);\n",
          setpre,p,setpre,setpre,prepcnt);
}

/* use pari to compute matrix determinant. */
void sp_det(char *p, int ptype)
{
  fprintf(outf,"%stmp=!translate $	$ to ; in %s\n\n\
%stmp%d=!exec pari matdet([$tmp])\n",
          setpre,p,setpre,prepcnt);
}

/* use maxima to do formal integration,
 * but pari for numerical integration. */
void sp_int(char *p, int ptype)
{
  char *s;

  if((s=strchr(p,'='))!=NULL) *s=',';
  if((s=strstr(p,".."))!=NULL) {*s=','; *(s+1)=' ';}
  fprintf(outf,"%stmp=!translate ;\";': to $     $ in %s \n\
%stmp=!lower $tmp\n\
!readproc slib/function/integrate $tmp\n\
%stmp%d=$slib_out\n",
          setpre,p,setpre,setpre,prepcnt);
}

void sp_htmlmath(char *p, int ptype)
{
  fprintf(outf,"%stmp%d=!htmlmath %s\n\n",setpre,prepcnt,p);
}

void sp_teximg(char *p, int ptype)
{
  fprintf(outf,"!readproc %s/teximg.phtml %s \n\
%stmp%d=$ins_url\n", primitive_dir, p, setpre,prepcnt);
}

void sp_texmath(char *p, int ptype)
{
  fprintf(outf,"%stmp%d=!texmath %s\n\n",setpre,prepcnt,p);
}

void sp_maxima(char *p, int ptype)
{
  fprintf(outf,"%st_=!replace internal \\( by ( in %s\n\
%stmp%d=!exec maxima $t_\n\n",setpre,p,setpre,prepcnt);
}

void sp_yacas(char *p, int ptype)
{
  fprintf(outf,"%st_=!replace internal \\( by ( in %s\n\
%stmp%d=!exec yacas $t_\n\n",setpre,p,setpre,prepcnt);
}

void sp_pari(char *p, int ptype)
{
  fprintf(outf,"%st_=!replace internal \\( by ( in %s\n\
%stmp%d=!exec pari $t_\n\n",setpre,p,setpre,prepcnt);
}

void sp_simplify(char *p, int ptype)
{
  fprintf(outf,"%stmp=!translate \";': to $     $ in %s\n\n\
%stmp=!lower $tmp\n\
%stmp%d=!exec maxima fullratsimp($tmp);\n",
          setpre,p,setpre,setpre,prepcnt);
}

void sp_slib(char *p, int ptype)
{
  char *p2;
  p=find_word_start(p);
  for(p2=p;*p2!=0 && !isspace(*p2) && *p2!=',' && *p2!=';';p2++);
  if(*p2!=0 && !isspace(*p2)) *p2=' ';
  fprintf(outf,"!readproc slib/%s \n\
%stmp%d=$slib_out\n",p,setpre,prepcnt);
}

void sp_draw(char *p, int ptype)
{
  char *p2;
  p2=strchr(p,'	'); if(p2==NULL) p2=strchr(p,'\n');
  if(p2==NULL) return;
  *p2++=0;
  fprintf(outf,"!readproc %s/draw.phtml %s \\\n%s \n\
%stmp%d=$ins_url\n", primitive_dir, p, p2, setpre,prepcnt);
}

void sp_drawtikz(char *p, int ptype)
{
  char *p2;
  p2=strchr(p,'	'); if(p2==NULL) p2=strchr(p,'\n');
  if(p2==NULL) return;
  *p2++=0;
  fprintf(outf,"!readproc %s/drawtikz.phtml %s \\\n%s \n\
%stmp%d=$ins_tikz\n", primitive_dir, p, p2, setpre,prepcnt);
}

void sp_canvasdraw(char *p, int ptype)
{
  char *p2;
  p2=strchr(p,'	'); if(p2==NULL) p2=strchr(p,'\n');
  if(p2==NULL) return;
  *p2++=0;
  fprintf(outf,"!readproc %s/canvasdraw.phtml %s \\\n%s \n\
%stmp%d=$canvasdraw_out\n", primitive_dir, p, p2, setpre,prepcnt);
}

void sp_shuffle(char *p, int ptype)
{
  fprintf(outf,"%stmp%d=!shuffle %s\n\n",setpre,prepcnt,p);
}

void sp_positionof(char *p, int ptype)
{
  char *p1;
  p1=strparchr(p,','); if(p1==NULL) {
    fprintf(outf,"%stmp%d=%s\n\n",setpre,prepcnt,p); return;
  }
  *p1++=0;
  fprintf(outf,"%stmp%d=!positionof item %s in %s\n\n",setpre,prepcnt,p, p1);
}

void sp_random(char *p, int ptype)
{
  char *pr, *p2, buf[MAX_LINELEN+1];
  snprintf(buf,sizeof(buf),"%s",p);
  for(p2=buf; *p2 && p2-buf<MAX_LINELEN; p2++) {
    if(*p2=='(') {
      p2=find_matching(p2+1,')'); continue;
    }
    if(*p2==',' || (*p2=='.' && *(p2+1)=='.')) break;
  }
  if(*p2==',') pr="randitem";
  else {
    if(*p2=='.') {
      *p2=','; *(p2+1)=' ';
    }
    if(ptype==pt_int) pr="randint"; else pr="random";
  }
  fprintf(outf,"%stmp%d=!%s %s\n\n",setpre,prepcnt,pr,buf);
}

void sp_pickone(char *p, int ptype)
{
  sp_random(p,pt_int);
}

void sp_item(char *p, int ptype)
{
  char *pp, *p2, buf[MAX_LINELEN+1];
  pp=strparchr(p,',');
  if(pp==NULL) pp=""; else *pp++=0;
  strncpy(buf,p,MAX_LINELEN);buf[MAX_LINELEN]=0;
  strip_enclosing_par(buf);
  if((p2=strstr(buf,".."))!=NULL) string_modify(buf,p2,p2+2," to ");
  fprintf(outf,"%stmp%d=!item %s of %s\n\n",setpre,prepcnt,buf,pp);
}

void sp_items(char *p, int ptype)
{
  fprintf(outf,"%stmp%d=!itemcnt %s\n\n",setpre,prepcnt,p);
}

void sp_randitem(char *p, int ptype)
{
  fprintf(outf,"%stmp=!nonempty items %s\n\n\
%stmp%d=!randitem $tmp\n",setpre,p,setpre,prepcnt);
}

void sp_column(char *p, int ptype)
{
  char *pp, *p2, buf[MAX_LINELEN+1];
  pp=strparchr(p,',');
  if(pp==NULL) pp=""; else *pp++=0;
  strncpy(buf,p,MAX_LINELEN);buf[MAX_LINELEN]=0;
  strip_enclosing_par(buf);
  if((p2=strstr(buf,".."))!=NULL) string_modify(buf,p2,p2+2," to ");
  fprintf(outf,"%stmp=!translate internal $	$ to ; in %s\n\n\
%stmp=!column %s of $tmp\n\
%stmp%d=!translate internal $\\\n$ to ; in $tmp\n",
          setpre,pp,setpre, buf, setpre, prepcnt);
}

void sp_row(char *p, int ptype)
{
  char *pp, *p2, buf[MAX_LINELEN+1];
  pp=strparchr(p,',');
  if(pp==NULL) pp=""; else *pp++=0;
  strncpy(buf,p,MAX_LINELEN);buf[MAX_LINELEN]=0;
  strip_enclosing_par(buf);
  fprintf(outf,"%stmp=!translate internal $	$ to ; in %s\n\n",
        setpre,pp);
  if(strstr(buf,"column")!=NULL) {
    fprintf(outf,"%stmp%d=!select $tmp where %s\n\n",setpre,prepcnt,buf);
    return;
  }
  while((p2=strstr(buf,".."))!=NULL)
    string_modify(buf,p2,p2+2," to ");
  fprintf(outf,"%stmp=!row %s of $tmp\n\
%stmp%d=!translate internal $\\\n$ to ; in $tmp\n",
          setpre, buf, setpre, prepcnt);
}

void sp_rows(char *p, int ptype)
{
  fprintf(outf,"%stmp=!translate internal $	$ to ; in %s\n\n\
%stmp%d=!rowcnt $tmp\n",setpre,p,setpre,prepcnt);
}

void sp_randrow(char *p, int ptype)
{
  fprintf(outf,"%stmp=!translate internal $	$ to ; in %s\n\n\
%stmp=!nonempty rows $tmp\n\
%stmp=!randrow $tmp\n\
%stmp%d=!translate internal $\\\n$ to ; in $tmp\n",
          setpre,p,setpre,setpre,setpre,prepcnt);
}

void sp_mathexp_cut(char *p, int ptype)
{
  char *p2;
  p2=find_word_end(find_word_start(p)); if(isspace(*p2)) *p2++=0;
  p2=find_word_start(p2);
  fprintf(outf,"%stmp%d=!exec mathexp cut %s\\\n%s\n\n",
        setpre,prepcnt,p,p2);
}

void sp_wims(char *p, int ptype)
{
  p=find_word_start(p);
  if(!isalpha(*p) || strncasecmp(p,"ins",3)==0)
    fprintf(outf,"%stmp%d=%s\n\n",setpre,prepcnt,p);
  else
    fprintf(outf,"%stmp%d=!%s\n\n",setpre,prepcnt,p);
}

struct specialfn specialfn[]={
  {"asis",  sp_asis},
  {"canvasdraw", sp_canvasdraw},
  {"column", sp_column},
  {"deriv", sp_diff},
  {"derivative", sp_diff},
  {"det", sp_det},
  {"determinant", sp_det},
  {"diff",  sp_diff},
  {"draw",  sp_draw},
  {"drawtikz", sp_drawtikz},
  {"evaluate", sp_evalue},
  {"evalue", sp_evalue},
  {"htmlmath", sp_htmlmath},
  {"int",  sp_int},
  {"integral", sp_int},
  {"integrate", sp_int},
  {"item",  sp_item},
  {"items",  sp_items},
  {"mathexp_cut", sp_mathexp_cut},
  {"maxima", sp_maxima},
  {"pari",  sp_pari},
  {"pickone", sp_pickone},
  {"position", sp_positionof},
  {"positionof", sp_positionof},
  {"randint", sp_pickone},
  {"randitem", sp_randitem},
  {"random", sp_random},
  {"randomitem", sp_randitem},
  {"randomrow", sp_randrow},
  {"randrow", sp_randrow},
  {"row",  sp_row},
  {"rows",  sp_rows},
  {"shuffle", sp_shuffle},
  {"simplify", sp_simplify},
  {"slib",  sp_slib},
  {"solve",  sp_solve},
  {"teximg", sp_teximg},
  {"texmath", sp_texmath},
  {"wims",  sp_wims},
  {"yacas", sp_yacas}
};
int specialfn_no=(sizeof(specialfn)/sizeof(specialfn[0]));

/* This routine treats special functions */
void parmprep(char *p,int ptype)
{
  char *pp, *p2, *pn, namebuf[32], buf[MAX_LINELEN+1];
  int i;

  while((pp=strchr(p,'\n'))!=NULL) *pp='	';
  for(pp=p;*pp && pp-p<MAX_LINELEN;pp++) {
/* function names */
    if(isalpha(*pp)) {
      for(i=0;i<30 && (isalnum(pp[i]) || pp[i]=='_');i++) namebuf[i]=pp[i];
      namebuf[i]=0; p2=find_word_start(pp+i);
      if((pp>p && isalnum(*(pp-1))) || *p2!='(') {
/* if(*p2=='\\' && *(p2+1)=='(') ovlstrcpy(p2,p2+1); */
        pp=p2-1; continue;
      }
      pn=pp; pp=p2+1; p2=find_matching(pp,')');
      if(p2==NULL) {
        sp_error("unmatched_parentheses");
        pp=p2; continue;
      }
      i=search_list(specialfn,specialfn_no,sizeof(specialfn[0]),namebuf);
      if(i<0) {
        pp--; continue;
      }
      *p2=0;
      snprintf(buf,sizeof(buf),"%s",pp);
      if(specialfn[i].processor!=sp_asis) parmprep(buf,ptype);
      specialfn[i].processor(buf,ptype);
      string_modify(p, pn, p2+1, "$(tmp%d)",prepcnt); prepcnt++;
      pp=pn+6;
    }
  }
}

