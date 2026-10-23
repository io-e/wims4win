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

/* html form simplification routines */
#include "wims.h"

#define MAX_MENU_ITEMS 256

enum {
  FORM_SELECT, FORM_RADIO, FORM_BAR, FORM_CHECKBOX
};

/* Produces <select> and <option>s.
 * Parameter string: name from n1 to n2 [prompt plist]
 * or name, list slist [prompt plist]
 * Using script to do this is too slow.
 */
void _form_menus(char *p,int kind)
{
  char *n, *li, *pp, *val, *p0, *p1, *p2, *pc, *s, *hmode, *name;
  char *vlist[MAX_MENU_ITEMS], *plist[MAX_MENU_ITEMS];
  char nbuf[MAX_LINELEN+1],vbuf[MAX_LINELEN+1],pbuf[MAX_LINELEN+1];
  char buf[256], pfrb[256], pfre[256], input_id[128];
  int i,i1=0,i2,itemcnt,type;
  if(!outputing) return;
  n=find_word_start(p); if(*n==0) return;
  pp=find_word_end(n);
  if(*pp!=0) *(pp++)=0;
  else {
    syntax: module_error("syntax_error"); return;
  }
  li=find_word_start(pp); pp=find_word_end(li);
  if(*pp!=0) *(pp++)=0; else goto syntax;
  itemcnt=0;
  if(strcmp(li,"from")==0) {
    p1=find_word_start(pp);
    p2=wordchr(p1,"to"); if(p2==NULL || p2<=p1) goto syntax;
    *(p2-1)=0; p2=find_word_start(p2+strlen("to"));
    pp=wordchr(p2,"prompt"); if(pp!=NULL) *(pp-1)=0;
    i1=evalue(p1); i2=evalue(p2);
    if(i1<-100000000 || i1>100000000 || i2<-100000000 || i2>100000000)
      goto syntax;
    if(i2<i1) return;
    if(i2>i1+MAX_MENU_ITEMS-1) i2=i1+MAX_MENU_ITEMS-1;
    itemcnt=i2-i1+1;
    type=0;
  }
  else {
    if(strcmp(li,"list")==0) {
      p1=find_word_start(pp);
      pp=wordchr(p1,"prompt"); if(pp!=NULL && pp>p1) *(pp-1)=0;
      ovlstrcpy(vbuf,p1);substit(vbuf);
      itemcnt=cutitems(vbuf,vlist,MAX_MENU_ITEMS);
      type=1;
    }
    else goto syntax;
  }
  for(i=0;i<itemcnt;i++) plist[i]=NULL;
  if(pp!=NULL) {
    p1=find_word_start(pp+strlen("prompt"));
    ovlstrcpy(pbuf,p1);substit(pbuf);
    cutitems(pbuf,plist,MAX_MENU_ITEMS);
  }
  ovlstrcpy(nbuf,n);substit(nbuf);
  if(kind==FORM_SELECT) {
    char *pp;
    s=getvar("wims_ref_class");
    if(s!=NULL && *s!=0 && !isspace(*s)) {
      snprintf(pfrb,sizeof(pfrb),"class=\"%s\"",s);
    } else { pfrb[0]=0;}
    pp=getvar("wims_formselect_switch"); if(pp==NULL) pp="";
    output("<select %s %s name=\"%s\" id=\"%s\">\n",pp,pfrb,nbuf,nbuf);
  }
  if(kind==FORM_BAR) {
    s=getvar("wims_ref_class");
    if(s!=NULL && *s!=0 && !isspace(*s)) {
      snprintf(pfrb,sizeof(pfrb)," <span class=\"%s\">",s); strcpy(pfre,"</span>");
    } else { pfrb[0]=0; pfre[0]=0; }
    output("%s<span style=\"font-weight:bold;\">-</span>&nbsp;",pfrb);
  }
  val=getvar(nbuf);if(val==NULL) val="";

  s=getvar("wims_ref_id");
  if(s!=NULL && *s!=0 && !isspace(*s)) { mystrncpy(input_id,s,sizeof(input_id)); }
  else { mystrncpy(input_id,nbuf,sizeof(input_id));}
  char *ref_class_h = getvar("wims_ref_class");
  char *hmode_h = getvar("wims_html_mode");
  for(i=0;i<itemcnt;i++) {
    if(type==0) {
      snprintf(buf,sizeof(buf),"%d",i+i1);
      p0=buf;
    }
    else p0=vlist[i];
    if(*find_word_start(p0)==0) continue;
    if(plist[i]==NULL) plist[i]=p0;
    if(*val!=0 &&
        (strcmp(p0,val)==0 ||
        ( (kind==FORM_SELECT || kind==FORM_CHECKBOX)
        && itemchr(val,p0)!=NULL))) {
      if(kind==FORM_SELECT) pc=" selected=\"selected\"";
      else pc=" checked=\"checked\"";
    }
    else pc="";
    s=ref_class_h; hmode=hmode_h;
    pfrb[0]=0; pfre[0]=0;
    if(s!=NULL && *s!=0 && !isspace(*s)) {
      if(hmode==NULL || *hmode==0 || isspace(*hmode)) {
        hmode="span";
      }
      snprintf(pfrb, sizeof(pfrb), "<%s class=\"%s\">", hmode, s);
      snprintf(pfre, sizeof(pfre), "</%s>", hmode);
    } else {
      if(hmode!=NULL && *hmode!=0) {
        snprintf(pfrb, sizeof(pfrb), "<%s>", hmode);
        snprintf(pfre, sizeof(pfre), "</%s>", hmode);
      }
    }
    switch(kind) {
      case FORM_RADIO: name="radio"; break;
      case FORM_CHECKBOX: name="checkbox"; break;
      case FORM_BAR: name="radio"; break;
      default: name="";
    }
    switch(kind) {
      case FORM_SELECT:
        output("<option value=\"%s\"%s>%s</option>\n", p0, pc, plist[i]);
        break;

      case FORM_RADIO:
      case FORM_CHECKBOX:
        if (itemcnt > 1) {
          output("%s<input type=\"%s\" name=\"%s\" id=\"%s_%d\" value=\"%s\"%s><label for=\"%s_%d\">%s</label>%s",
           pfrb,name,nbuf,input_id,i+1,p0,pc,input_id,i+1,plist[i],pfre);
          if (i==itemcnt) {
            output("</fieldset>");
          }
        } else {
          output("%s<input type=\"%s\" name=\"%s\" id=\"%s\" value=\"%s\"%s><label for=\"%s\">%s</label>%s",
           pfrb,name,nbuf,input_id,p0,pc,input_id,plist[i],pfre);
        }
        if(i<itemcnt-1 && itemcnt>2 && (hmode==NULL || *hmode==0)) _output_(",");
        _output_("\n");
        break;
      case FORM_BAR:
        output("<input type=\"%s\" name=\"%s\" id=\"%s_%d\" title=\"%s\" value=\"%s\"%s><label style=\"display:none\" for=\"%s_%d\">%s</label>",
           name,nbuf,input_id,i+1,p0,p0,pc,input_id,i+1,p0);
        break;
    }
  }
  setvar("wims_ref_class","");
  setvar("wims_ref_id","");
  setvar("wims_html_mode","");
  if(kind==FORM_SELECT) _output_("</select>");
  if(kind==FORM_BAR) output("&nbsp;<span style=\"font-weight:bold;\">+</span>");
}

void exec_formselect(char *p)
{
  _form_menus(p,FORM_SELECT);
}

void exec_formradio(char *p)
{
  _form_menus(p,FORM_RADIO);
}

void exec_formcheckbox(char *p)
{
  _form_menus(p,FORM_CHECKBOX);
}

void exec_formbar(char *p)
{
  _form_menus(p,FORM_BAR);
}
