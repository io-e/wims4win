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

/* This should be a rather independent routine, which are
 * used to interface an outside mathematical encyclopedia. */
#include "wims.h"

void exec_href(char *p);

/* internal encyclopedia version */
/* void pedia_wims(char *p)
{
    char *s, *c, *t, buf[MAX_LINELEN+1];
    char r[MAX_LINELEN+1];

    t="target=wims_ref"; c="module=adm/enc.$module_language";
    s=getvar(ro_name[ro_module]);
    if(s!=NULL && strncmp(s,"adm/enc.",strlen("adm/enc."))==0) {
     c="cmd=reply"; t="";
    }
    else {
     s=getvar(ro_name[ro_session]);
     if(s!=NULL && strstr(s,"_dico")!=NULL) t="";
    }
    setvar("wims_dico_lookup",p);
    snprintf(buf,MAX_LINELEN,"%s %s&job=search&dom=math&search=$wims_dico_lookup %s",
          t, c, p);
    mystrncpy(r,ref_name,MAX_LINELEN);
    if(pedia_address!=NULL && *pedia_address!=0) {
     mystrncpy(ref_name,pedia_address,MAX_LINELEN);
    }
    exec_href(buf);
    mystrncpy(ref_name,r,MAX_LINELEN);
}
*/

     /* this is the (only) public routine in this file. */
void pedia(char *p)
{

    if(*find_word_start(p)==0) return;
    force_setvar("wims_pedia_parm",p);
    phtml_put_base("pedia.phtml",1);
    *p=0;
}
