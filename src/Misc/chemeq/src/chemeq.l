/* -*- coding: utf-8 -*- */
%option noyywrap
%option c++
     
   #include <stdlib.h>
   #include "chemeq.h"

   char * ind;
   int position = 0;

   #ifndef size_t
   #define size_t int
   #endif     

/* les nombres */
digits         [0-9]
spc            [ \t]+
int            {digits}+
frac           {int}{spc}?[/]{spc}?{int}
mantisse       {digits}+
real           -?{int}([.]{mantisse})?([Ee][+-]?{int})?
eol            \n.*

/* la flèche */
fleche         [-]+>

/* la double flèche */
dfleche        <[-]+>

/* fonction spéciale : toute chaîne préfixée par "func:" */
func          func:.*

%%
 /* les règles */

 /* pour les atomes */
Uu[a-z]|[A-Z][a-z]? { /* éléments Uux : transuraniens après le N° 109
                           les autres sont les éléments ordinaires */
    int i = 0;
    position += strlen(yytext);
    while (lesatomes[i].Zed != 0 && strcmp(yytext, lesatomes[i].symb)) i++;
    if (lesatomes[i].Zed == 0){
      yylval.i=-2;      /* -2 est le n° pour les éléments non définis */
      strncpy(yylval.symb, yytext,3);
    }
    else {
      yylval.i=i;
      strcpy(yylval.symb, lesatomes[i].symb);
    }
    yylval.s = yytext;
    return Atome;
  }

e      {position++; yylval.i=-1; strcpy(yylval.symb, "e");return Atome;}


\(     { position++; return Lpar;}
\)     { position++; return Rpar;}

{spc}+\( { position += strlen(yytext); return SpcLpar;}
\[       { position ++; return Lsq;}
\]       { position ++; return Rsq;}
{int}  { position += strlen(yytext); yylval.i = atoi(yytext); return Int; }
{real} { position += strlen(yytext); yylval.r = atof(yytext); return Real;}
{frac} { position += strlen(yytext); ind = index(yytext,'/'); *ind=0; 
         yylval.i = atoi(yytext); yylval.d = atoi(ind+1); 
         return Frac;
       }
\+{int}  { position += strlen(yytext); yylval.i = atoi(yytext+1); 
           return Charge;
         }
\+[\+]+  { position += strlen(yytext); 
           yylval.i = strlen(yytext); return Charge;
         }
\-[\-]+  { position += strlen(yytext); yylval.i = -strlen(yytext); 
           return Charge;
         }
{int}\+  { position += strlen(yytext); yytext[strlen(yytext)]=0; 
           yylval.i = atoi(yytext); return Charge;
         }
{int}\-  { position += strlen(yytext); yytext[strlen(yytext)]=0; 
           yylval.i = -atoi(yytext); return Charge;
         }
{spc}+\+ {position += strlen(yytext); return SpcPlus;}
\-       {position += strlen(yytext); return Moins;}
\+       {position ++; return Plus;} 
\*       {position ++; return Mul;}
{dfleche} {position += strlen(yytext); return Dfleche;}
{fleche} {position += strlen(yytext); return Fleche;}
{spc}    {position += strlen(yytext); return Spc;}
\^       {position ++; return Haut;}
_\(s\)|_s|s     {position += strlen(yytext); return Sol;}
_\(l\)|_l|l     {position += strlen(yytext); return Liq;}
_\(g\)|_g|g     {position += strlen(yytext); return Gas;}
_\(aq\)|_ag|aq   {position += strlen(yytext); return Aqueous;}
=        {position ++; return Egal;}
#        {position ++; return Compose;}
~        {position+=2; return AntiCompose;}
{func}   {position += strlen(yytext); yytext[strlen(yytext)]=0; 
  yylval.s = yytext+strlen("func:"); return Func;}
{eol}    {/* rien c'est la fin des entrées */}
.|\n     {position += strlen(yytext); /* rien */}

