// -*- coding: utf-8 -*-
%{
/* inclusions, définition */
#include <string>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <math.h>
#include "chemeq.h"
  //#include <FlexLexer.h>
#include <sstream>

#include "html_table.cc"

  int yylex();
  int chemerror(char * msg);
  int yyerror(const char * msg);

typedef struct{
  int i, d; /* integer numerator, denominator */
  double r;
  AtomeListe * al;
  Molec * m;
  Chemeq * cq;
  Membre * mb;
  char symb[4];
  moltype t;
  std::string s;
} yystype;

#define YYSTYPE yystype

/* variables globales */

 yystype result;
 extern char * yytext;
 extern int position;
 int isequation;

%}

%token Atome
%token SpcPlus
%token Plus
%token Moins
%token Mul
%token Dfleche
%token Fleche
%token Int
%token Charge
%token Frac
%token Real
%token SpcLpar
%token Lpar
%token Rpar
%token Lsq
%token Rsq
%token Haut
%token Spc
%token Sol
%token Liq
%token Gas
%token Aqueous
%token Egal
%token Compose
%token AntiCompose
%token Func
%%

/* les règles */
but : chcompose {
  result=$1;
  isequation=1;
}
| molec {
  result=$1;
  isequation=0;
}
| special_func {
  result=$1;
  isequation=-1;
}
;

chcompose : chc {$$=$1;}
| chcompose spc01 Compose spc01 chc {
  $1.cq->addChemeq($5.cq);
  $$=$1;
}
| chcompose spc01 AntiCompose spc01 chc {
  $1.cq->subChemeq($5.cq);
  $$=$1;
}
;

chc : chemeq cste{
  $$=$1;
  $$.cq->constante($2.s);
  $$.cq->valeur($2.r);
}
| Frac spc01 Mul spc01 chc {
  $$.cq = $5.cq; $$.cq->multiply($1.i,$1.d);
}
| Int spc01 Mul spc01 chc {
  $$.cq = $5.cq; $$.cq->multiply($1.i,1);
}
;

cste : /* rien */ {$$.s = ""; $$.r = MINVAL; /* valeur négative : non défini */}
| SpcLpar cste1 Rpar {$$ = $2;}
;

cste1 : spc01 nombre volt{$$.s = ""; $$.r = $2.r;}
| spc01 id spc01 Egal spc01 nombre volt{$$.s = $2.s; $$.r = $6.r;}
;

nombre : Real {$$.r=$1.r;}
| Int {$$.r=1.0*$1.i;}
| Plus spc01 Real{$$.r=$3.r;}
| Moins spc01 Real{$$.r=-$3.r;}
| Plus spc01 Int{$$.r=1.0*$3.i;}
| Moins spc01 Int{$$.r=-1.0*$3.i;}
;

volt : /* rien */
| spc01 Atome {if ($2.s.compare("V")!=0) chemerror (strdup("only 'V' allowed as unit")); }
;

id : Atome {/* $$.s contient le nom */}
;

chemeq : membre Spc Fleche spc01 membre{
  $$.cq = new Chemeq($1.mb,$5.mb);
}
| membre Spc Dfleche spc01 membre{
  $$.cq = new Chemeq($1.mb,$5.mb,true);
}
;

membre : membre SpcPlus spc01 molecs {
  $$.mb->addMol($4.m);
}
| molecs {
  $$.mb = new Membre;
  $$.mb->push_back($1.m);
}
;

molecs : Int spc01 molec {
  $$ = $3;
  $$.m->nombre($1.i);
}
| Frac spc01 molec {
  $$ = $3;
  $$.m->nombre($1.i,$1.d);
}
| molec{
  $$ = $1;
}
;

spc01 : /*rien*/
| Spc
;

molec : composition0 typage{
  $$.m = new Molec($1.al,0); $$.m->typage($2.t);
}
| composition0 charge typage{
  $$.m = new Molec($1.al,$2.i);$$.m->typage($3.t);
}
;


composition0 : composition {$$.al=$1.al;}
| Lsq spc01 composition spc01 Rsq {$$.al=$3.al; $$.al->sq(1);}
;

typage : /* rien */ {$$.t = aqueous;}
| Aqueous {$$.t = aqueous_explicit;}
| Sol {$$.t = sol;}
| Liq {$$.t = liquid;}
| Gas {$$.t = gas;}
;

charge : Plus {$$.i=1;}
| Haut Plus {$$.i=1;}
| Haut Charge {$$.i=$2.i;}
| Moins{$$.i=-1;}
| Haut  Moins{$$.i=-1;}
;

groupe : Lpar composition Rpar {
  $$=$2;
}
;

atome_general : groupe {
  $$.al = new AtomeListe("",0,0,$1.al);
}
| Atome{
  char buffer[25];
  if ($1.i==-2) { /* ce n'est pas un atome recensé */
    snprintf(buffer,sizeof(buffer),"nonexistent atom : %s", $1.symb);
    chemerror(buffer);
  }
  $$.al = new AtomeListe($1.symb,$1.i);
}
;

composition : atome_general molecularite composition {
  $$ = $1;
  $$.al->setmolecularite($2.i);
  $$.al->setsuivant($3.al);
}
| atome_general molecularite{
  $$.al = $1.al;
  $$.al->setmolecularite($2.i);
}
;

molecularite : /*rien*/{
  $$.i=1;
}
| Int {
  $$=$1;
}
;

special_func : Func {
  $$.s=$1.s;
 }
;

%%
#include "chemlex.cc"

yyFlexLexer lexer;
yyFlexLexer * thelexer = &lexer;

int yylex(){
  return thelexer->yylex();
}

/* le programme lui-même */

inline int chemerror(char * msg){
  fprintf(stderr, "ERROR %s at %d\n ", msg, position);
  exit(1);
}

inline int yyerror(const char * msg){
  printf("ERROR: %s at %d\n", msg, position);
  exit(1);
}

void printVersion(){
  std::cout << "Chemeq version " << VERSION << "\n(c) Georges Khaznadar <georgesk@debian.org>.";
  std::cout << "\nThis program is free, licensed under G.P.L.\n";
}

void printHelp(){
  std::cout << "Usage : chemeq [-m] [-l] [-c] [-w] [-C] [-n] [-e] [-s] [-v] [-h]" << std::endl;
  std::cout << "        default is equivalent to 'chemeq -mlcwCn'" << std::endl;
  std::cout << "        chemeq reads its standard input which must be a standard" << std::endl;
  std::cout << "        chemical equation. It outputs many strings useful for chemical" << std::endl;
  std::cout << "        calculations." << std::endl;
  std::cout << "        As an example you can try the following command :" << std::endl;
  std::cout << "        echo \"Fes -> Fe^2+ + 2 e- (0.44 V)\" | chemeq" << std::endl;
}

#define maxoption 16

void optionadd(char* b, char* allowed, int c);
void optionadd(char* b, char* allowed, int c){
  if (strchr(allowed,c) && !strchr(b,c) && strlen(b) < maxoption+1){
    int l = strlen(b);
    b[l]=c; b[l+1]=0;
  }
  return;
}

int main(int argc, char * argv[]){
  char * optstr = strdup("mMlcCwnesSvh");
  char * envoption=getenv("chemeq_option");
  if (envoption==NULL) envoption=getenv("w_chemeq_option");
  char * envinput =getenv("chemeq_input");
  if (envinput==NULL) envinput=getenv("w_chemeq_input");
  char asked[maxoption+1]="";
  unsigned int i;
  int ch;
  bool nooption=1;
  std::ostringstream w;

  while (-1 != (ch=getopt(argc,argv,optstr))){
    optionadd(asked,optstr,ch);
  }
  if (envoption!=NULL) for(i=0; i<strlen(envoption); i++){
    optionadd(asked, optstr, envoption[i]);
  }
  if (strchr(asked,'h')!=NULL){
    printHelp(); return 0;
  }
  if (strchr(asked,'v')!=NULL){
    printVersion(); return 0;
  }
  if (envinput !=NULL){
    std::istringstream iss(envinput);
    thelexer= new yyFlexLexer (&iss);
  }
  yyparse();

  bool wantedlatex=(strchr(asked,'l')!=NULL);
  if (isequation == 1) {
    for(i=0; i<strlen(asked); i++){
      switch(asked[i]){
      case 'm': nooption=0;
	result.cq->printnorm(std::cout); std::cout << std::endl;
	break;
      case 'M': nooption=0;
	result.cq->printweight(std::cout);
	std::cout <<  std::endl;
	break;
      case 'l': nooption=0;
	std::cout << *result.cq << std::endl;
	break;
      case 'c': nooption=0;
	std::cout << result.cq->equilibre() << std::endl;
	break;
      case 'w': nooption=0;
	result.cq->printNernst(std::cout,w,wantedlatex); std::cout << std::endl;
	std::cout << w.str() << std::endl;
	break;
      case 'n': nooption=0;
	result.cq->normalise();
	result.cq->printnorm(std::cout); std::cout << std::endl;
	break;
      case 'C': nooption=0;
	result.cq->printcount(std::cout); std::cout << std::endl;
	break;
      case 'e': nooption=0;
	result.cq->printelec(std::cout); std::cout << std::endl;
	break;
      case 's': nooption=0;
	result.cq->printspecies(std::cout); std::cout << std::endl;
	break;
      case 'S': nooption=0;
	result.cq->printcoeff(std::cout); std::cout << std::endl;
	break;
      }
    }
    if (nooption){
      result.cq->printnorm(std::cout); std::cout << std::endl;
      std::cout << *result.cq << std::endl;
      std::cout << result.cq->equilibre() << std::endl;
      result.cq->printNernst(std::cout,w); std::cout << std::endl;
      result.cq->printcount(std::cout); std::cout << std::endl;
      result.cq->normalise();
      result.cq->printnorm(std::cout); std::cout << std::endl;
    }
  }
  else { /* ce n'est pas une équation */
    if (isequation == 0) {
      /* c'est une molécule */
      for(i=0; i<strlen(asked); i++){
	switch(asked[i]){
	case 'M': nooption=0;
	  std::cout << result.m->weight();
	  std::cout <<  std::endl;
	  break;
	}
      }
      if (nooption){
	std::cout << *result.m;
      }
    }
    else {
      /* c'est une fonction spéciale */
      if (result.s == "html_table"){
	std::cout << html_table <<  std::endl;
      } else {
	std::cout << "ERROR: no feature named “" << result.s << "”" <<  std::endl;
      }
    }
  }
  return 0;
}
