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

 /* Solves cryptarithms, to be interfaced by wims. */

#define MAX_LINES 64
#define MAX_CHARS 32

#include "../includes.h"

char basetab[MAX_LINES][MAX_CHARS];
int carry[MAX_LINES][MAX_CHARS+8];
char corresp[32],bcorresp[16];
int linecnt,charcnt[MAX_LINES],activelen,maxlen;
int active[MAX_LINES];
int style[MAX_LINES];
int total=0;

char *input;

/* Print the result */
void printresult(char curr[])
{
  int i,j;
  char ch;
  for(i=0;i<linecnt;i++) {
    for(j=0;j<charcnt[i];j++) {
      ch=basetab[i][j];
      if(ch==0) ch=' ';
      else if(ch>='A' && ch<='Z') ch=curr[ch-'A'];
      printf(" %d",ch);
    }
    printf("\n");
  }
  printf("\n");
}

/* returns 1 if OK, 0 if bad. */
int putvalue(char ch, char k, char curr[], char currb[])
{
  int i=ch-'A';
  if(ch==0) {
    if(k>0) return 0; else return 1;
  }
  if((curr[i]!=-1 && curr[i]!=k) || (currb[k]!=0 && currb[k]!=ch))
    return 0;
  curr[i]=k;currb[k]=ch; return 1;
}

/* check that leading number is not 0. Returns 0 if bad. */
int checklead(char curr[])
{
  int i;
  char ch;
  for(i=0;i<linecnt;i++) {
    ch=basetab[i][charcnt[i]-1];
    if(curr[ch-'A']<=0) return 0;
  }
  return 1;
}

/* returns 0 if fail, 1 if OK. */
int colcompute(int c,char curr[],char currb[])
{
  int i,j,k,sum;
  char ch;

  switch(style[1]) {
    case '-':
    case '+':
    case '.': {
      for(j=sum=0;j<linecnt;j++) {
        if(!active[j]) continue;
        ch=basetab[j][c];
        if(ch!=0) {
          if(style[j]!='-') sum+=curr[ch-'A'];
          else sum-=curr[ch-'A'];
        }
      }
      lastline:
      sum+=carry[linecnt-1][c];
      carry[linecnt-1][c+1]=(sum+10000)/10-1000; k=(sum+10000)%10;
      return putvalue(basetab[linecnt-1][c],k,curr,currb);
    }

    case '*': {
      char c1,c2;
      c2=basetab[0][c];
      if(c2!=0) c2=curr[c2-'A'];
      for(i=0;i<c && i<linecnt-3;i++) {
        c1=basetab[1][i];
        if(c2*c1!=0) sum=c2*curr[c1-'A']+carry[2+i][c];
        else sum=carry[2+i][c];
        carry[2+i][c+1]=sum/10;
        if(!putvalue(basetab[2+i][c],sum%10,curr,currb))
          return 0;
      }
      c2=basetab[1][c];
      if(c2!=0) {
        c2=curr[c2-'A'];
        for(i=0;i<=c;i++) {
          c1=basetab[0][i];
          if(c1==0) break;
          sum=c2*curr[c1-'A']+carry[2+c][i];
          carry[2+c][i+1]=sum/10;
          if(!putvalue(basetab[2+c][i],sum%10,curr,currb))
            return 0;
        }
      }
      for(i=sum=0;i<=c && i<linecnt-3;i++) {
          ch=basetab[2+i][c-i];
          if(ch!=0) sum+=curr[ch-'A'];
      }
      goto lastline;
    }

/* short multiplication */
    case '$': {
      char c1,c2;
      for(j=sum=0;j<=c;j++) {
        c1=basetab[0][j]; c2=basetab[1][c-j];
        if(c1==0) break;
        if(c2==0) continue;
        sum+=curr[c1-'A']*curr[c2-'A'];
      }
      goto lastline;
    }

    default: return 0;
  }
}

void column(int c, char prev[], char prevb[])
{
  char curr[32], currb[16];
  int lreg[MAX_LINES],lfix[MAX_LINES];
  int i,j,icarry;
  char ch;

  memset(lreg,0,sizeof(lreg));
  memset(lfix,0,sizeof(lfix));

  for(i=0;i<linecnt;i++) {
    if(!active[i]) continue;
    ch=basetab[i][c];
    if(ch==0 || prev[ch-'A']!=-1) lfix[i]=1;
  }
  for(icarry=0;;icarry=1) {
    memmove(curr,prev,sizeof(curr));
    memmove(currb,prevb,sizeof(currb));
    for(i=0;i<linecnt;i++) {
      if(!active[i] || lfix[i]) continue;
      for(j=lreg[i]+icarry;j<10 && prevb[j]!=0;j++);
      if(j>=10) {
        for(j=0;j<10 && prevb[j]!=0;j++);
        if(j>=10) return;
        icarry=1;
      }
      else icarry=0;
      lreg[i]=j;
    }
    if(icarry) break;
    for(i=0;i<linecnt;i++) {
      if(!active[i] || lfix[i]) continue;
      if(!putvalue(basetab[i][c],lreg[i],curr,currb)) goto loopend;
    }
    if(!colcompute(c,curr,currb)) continue;
    if(c<activelen-1) column(c+1,curr,currb);
    else {
      for(i=activelen;i<maxlen;i++) {
        if(!colcompute(i,curr,currb)) goto loopend;
      }
      for(i=0;i<linecnt;i++) {
        if(active[i]) continue;
        if(carry[i][maxlen]) goto loopend;
      }
      if(!checklead(curr)) goto loopend;
      total++;
      printresult(curr);
    }
    loopend:
  }
}

int main(int argc, char *argv[])
{
  char *p,*p2,*p3;
  int i;
  input=getenv("wims_exec_parm");
/* nothing to do if no problem */
  if(input==NULL || *input==0) return 0;

  memset(basetab,0,sizeof(basetab));
  memset(corresp,-1,sizeof(corresp));
  memset(bcorresp,0,sizeof(bcorresp));
  memset(carry,0,sizeof(carry));
  memset(active,0,sizeof(active));

  for(p=input+strlen(input);p>input && isspace(*(p-1));p--);
  *p=0;
  activelen=maxlen=0;
  active[0]=active[1]=1;
  for(linecnt=0,p=input;*p!=0 && linecnt<MAX_LINES;p=p3) {
    p2=strchr(p,'\n');
    if(p2==NULL) {
      p2=p+strlen(p);p3=p2;
    }
    else p3=p2+1;
    while(isspace(*p)) p++;
    if(strchr("+-*/=.",*p)!=NULL) {
      style[linecnt]=*p;p++;
      while(isspace(*p)) p++;
    }
    else style[linecnt]='+';
    if(*p3==0) style[linecnt]='=';
    if(linecnt>1) {
     switch(style[1]) {
       case '+':
       case '-': {
        if(*p3!=0) active[linecnt]=1;
        break;
       }

       case '*':
       case '/':
       case '=':
       break;
    }
  }
  while(p2>p && isspace(*(p2-1))) p2--;
  if(p2>p+MAX_CHARS) p2=p+MAX_CHARS;
  *p2=0;
  if(p2<=p) continue;
  for(i=0;i<p2-p;i++) {
    char ch;
    ch=toupper(*(p2-i-1));
/* bad char */
    if(ch<'A' || ch>'Z') return 1;
      basetab[linecnt][i]=ch;
    }
    charcnt[linecnt]=p2-p;
    if(active[linecnt] && p2-p>activelen) activelen=p2-p;
    if(p2-p>maxlen) maxlen=p2-p;
    linecnt++;
  }
/* multiplication */
  if(style[1]=='*') {
    if(linecnt==3) style[1]='$';
    else {
      if(linecnt!=charcnt[1]+3) return 1;
    for(i=3;i<linecnt-1;i++)
      if(charcnt[i]+i-2>maxlen) maxlen=charcnt[i]+i-2;
    }
  }

  column(0,corresp,bcorresp);
  printf("\n!total %d solution(s).\n\n",total);
  return 0;
}
