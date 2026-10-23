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

/* Subroutines to process matrices
 * messages: inconsistent_columns
 */
#include "wims.h"

/* Translate error-laden raw matrix input into a standard form. */
void rawmatrix(char *p)
{
  char *p1;
  char lbuf[MAX_LINELEN+1], obuf[MAX_LINELEN+1];
  int n, i, c=0, change=0;

  obuf[0]=0;
  p1=find_word_start(p); if(p1>p) ovlstrcpy(p,p1);
  strip_enclosing_par(p); strip_trailing_spaces(p);
  if(*p==0) return;
  rows2lines(p); n=linenum(p);
  if(itemnum(p)<=1 && wordnum(p)>=2*n) change=1;
  for(i=1;i<=n;i++) {
    fnd_line(p,i,lbuf);
    p1=find_word_start(lbuf); if(p1>lbuf) ovlstrcpy(lbuf,p1);
    strip_enclosing_par(lbuf);
    if(lbuf[0]==0) continue;
    /* Uniformize column separator to comma */
    if(change) words2items(lbuf);
    rawmath(lbuf);
    if(i==1) {
      ovlstrcpy(obuf,lbuf);
      c=itemnum(lbuf);
    }
    else {
      if(strlen(obuf)+strlen(lbuf)>=MAX_LINELEN-1)
        user_error("cmd_output_too_long");
      if(c!=itemnum(lbuf)) {
        force_setvar("wims_warn_rawmatrix","inconsistent_columns");
        return;
      }
      snprintf(obuf+strlen(obuf),sizeof(obuf)-strlen(obuf),
               "\n%s",lbuf);
    }
  }
  ovlstrcpy(p,obuf);
}
