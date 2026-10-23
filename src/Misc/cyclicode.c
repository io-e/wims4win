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

 /* This program generates cyclic code from a polynomial
  * defined over a prime field.
  * It does not check whether the polynomial is primitive or irreducible.
  * Accepted parameter: 3 words.
  * Word 1: field characteristics, limited to 2,3,5,7.
  * Word 2: The polynomial coefficients (except the leading one, from
     lower degree to higher)
  * Word 3: The starting status (starting from the first bit).
  */

#include "../Lib/libwims.h"

#define MAX_DEGREE 256
#define MAX_LENGTH 1024000

int ch,degree,length;
char poly[MAX_DEGREE], chain[MAX_DEGREE];

void errorquit(char *msg)
{
  fprintf(stderr,"%s\n",msg); exit(1);
}

int main(int argc, char *argv[])
{
  char *parm, *p1, *p2, c1, c2;
  char pbuf[1024];
  int i,j,k;
  double dt;

  parm=getenv("wims_exec_parm");
  if(parm==NULL || *parm==0) errorquit("no_parameter");
  snprintf(pbuf,sizeof(pbuf),"%s",parm);
  p1=find_word_start(pbuf); p2=find_word_end(p1);
  if(*p2!=0) *p2++=0;
  ch=atoi(p1); p1=find_word_start(p2);
  if(ch!=2 && ch!=3 && ch!=5 && ch!=7) errorquit("bad_characteristics");
  p2=find_word_end(p1); if(*p2!=0) *p2++=0;
  p2=find_word_start(p2); *find_word_end(p2)=0;
  degree=strlen(p1);
  if(degree!=strlen(p2)) errorquit("unequal_degrees");
  if(degree>MAX_DEGREE) errorquit("degree_too_high");
  dt=pow(ch,degree);
  if(dt>=(double) MAX_LENGTH) errorquit("length_overflow");
  length=dt-1-degree;
  for(i=0;i<degree;i++) {
    c1=*(p1+i); c2=*(p2+i);
    if(!isdigit(c1) || c1>=ch+'0') errorquit("bad_polynomial");
    if(!isdigit(c2) || c2>=ch+'0') errorquit("bad_chain");
    poly[i]=ch-(c1-'0'); chain[i]=c2-'0';
  }
  for(i=0;i<length;i++) {
    for(j=k=0;j<degree;k+=poly[j]*chain[j],j++);
    k%=ch; printf("%d",k);
    memmove(chain,chain+1,degree-1); chain[degree-1]=k;
  }
  return 0;
}
