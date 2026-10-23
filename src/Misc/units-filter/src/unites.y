%{
/* inclusions, définition */
#include <string.h>
#include <stdio.h>
#include <sstream>
#include <stdlib.h>
#include "unites.h"
#include <math.h>

using namespace std;

  int yylex();
  int yyerror(const char * msg);

  const char * unit_names[BU_LAST]={"m","kg","s","A","K","mol","cd"};

  /**
   * values returned by the lexer are instances of the class yystype
   **/
  class yystype{
  public:
    /* integer property */
    int i;
    /**
     * mutiplicators are mutiprecisions decimals.
     *
     * multip:        multiplicator to apply to the SI monome to get the unit
     * maxmultip:     an accumulator used for mixed units expressions
     * wanted_multip: a desired multiplier when a unit wish is expressed
     **/
    double multip, maxmultip, wanted_multip;
    /* standard or non-standard unit index -- see unites.h */
    uniteSI unite;
    /* vector of powers (a unit is expressed as a monome of bse units)) */
    int base[BU_LAST];
    /* cstr properties */
    char * s, * v;
    /* name of a wanted unit */
    string wanted_unit;
    /* values are mutiprecisions rationals */
    Decimal val;
    /* how many significant numbers? */
    int signif;
    /* percent tolerance */
    int pcent;
    /**
     * the construcror initializes at least cstr properties to avoid
     * dangling links.
     **/
    yystype(){
      s=(char*) NULL;
      v=(char*) NULL;
    }
  };

  /**
   * display to stream operator, useful to debug yystype objects
   **/
  ostream & operator << (ostream & o, yystype data);
  ostream & operator << (ostream & o, yystype data){
    o << "YYSTYPE[" <<"i="<<data.i<<", multip="<<data.multip;
    o<<", maxmultip="<<data.maxmultip<<", wanted_multip="<<data.wanted_multip;
    o<<", unite="<<data.unite<<", base={";
    for (int i=0;i<BU_LAST;i++){
      o<<data.base[i]<<" ";
    }
    o<<"}";
    if(data.s!=NULL){ o<<", s="<<data.s;}else{o<<", s=NULL";}
    if(data.v!=NULL){o<<", v="<<data.v;}else{o<<", v=NULL";}
    o<<", wanted_unit="<<data.wanted_unit;
    o<<", val="<<data.val<<", signif="<<data.signif<<", pcent="<<data.pcent;
    o<<"] ";
    return o;
  }
  
#define YYSTYPE yystype 

 extern FILE * yyin;

/* variables globales */
#define MAXBUF 255
 char buffer[MAXBUF+1], buffer2[MAXBUF+1];
 yystype result;
 Decimal val_decimal; /* a multiprecision rational */
 int val_int, count_signif=0;
 long pos;

 /**
  * SI and non-SI units list
  * source: Handbook of Chemistry & Physics 78, CRC Press 1997, page 1-20
  * full names of the units are in French, with a Latin-1 encoding
  **/
unite_data unites[TU_LAST] ={
/*TUh*/   {"h",      "seconde",   3600.0,     { 0, 0, 1, 0, 0, 0, 0}},
/*TUmin*/ {"min",    "seconde",   60.0,       { 0, 0, 1, 0, 0, 0, 0}},
/*TUm*/   {"m",      "mètre",     1.0,        { 1, 0, 0, 0, 0, 0, 0}},
/*TUg*/   {"g",      "kilogramme",1.0e-3,     { 0, 1, 0, 0, 0, 0, 0}},
/*TUs*/   {"s",      "seconde",   1.0,        { 0, 0, 1, 0, 0, 0, 0}},
/*TUA*/   {"A",      "ampère",    1.0,        { 0, 0, 0, 1, 0, 0, 0}},
/*TUK*/   {"K",      "kelvin",    1.0,        { 0, 0, 0, 0, 1, 0, 0}},
/*TUmol*/ {"mol",    "mol",       1.0,        { 0, 0, 0, 0, 0, 1, 0}},
/*TUcd*/  {"cd",     "candela",   1.0,        { 0, 0, 0, 0, 0, 0, 1}},
/*TUHz*/  {"Hz",     "hetrz",     1.0,        { 0,-1, 0, 0, 0, 0, 0}},
/*TUN*/   {"N",      "newton",    1.0,        { 1, 1,-2, 0, 0, 0, 0}},
/*TUPa*/  {"Pa",     "pascal",    1.0,        {-1, 1,-2, 0, 0, 0, 0}},
/*TUJ*/   {"J",      "joule",     1.0,        { 2, 1,-2, 0, 0, 0, 0}},
/*TUW */  {"W",      "watt",      1.0,        { 2, 1,-3, 0, 0, 0, 0}},
/*TUVA*/  {"VA",     "voltampere",1.0,        { 2, 1,-3, 0, 0, 0, 0}},
/*TUvar*/ {"var",    "var",       1.0,        { 2, 1,-3, 0, 0, 0, 0}},
/*TUC*/   {"C",      "coulomb",   1.0,        { 0, 0, 1, 1, 0, 0, 0}},
/*TUV*/   {"V",      "volt",      1.0,        { 2, 1,-3,-1, 0, 0, 0}},
/*TUohm*/ {"\\Omega","ohm",       1.0,        { 2, 1,-3,-2, 0, 0, 0}},
/*TUS*/   {"S",      "siemens",   1.0,        {-2,-1, 3, 2, 0, 0, 0}},
/*TUF*/   {"F",      "farad",     1.0,        {-2,-1, 4, 2, 0, 0, 0}},
/*TUT*/   {"T",      "tesla",     1.0,        { 0, 1,-2,-1, 0, 0, 0}},
/*TUWb*/  {"Wb",     "weber",     1.0,        { 2, 1,-2,-1, 0, 0, 0}},
/*TUH*/   {"H",      "henry",     1.0,        { 2, 1,-2,-2, 0, 0, 0}},
/*TUlm*/  {"lm",     "lumen",     1.0,        { 0, 0, 0, 0, 0, 0, 1}},
/*TUlx*/  {"lx",     "lux",       1.0,        {-2, 0, 0, 0, 0, 0, 1}},
/*TUBq */ {"Bq",     "becquerel", 1.0,        { 0, 0,-1, 0, 0, 0, 0}},
/*TUGy*/  {"Gy",     "gray",      1.0,        { 2, 0,-2, 0, 0, 0, 0}},
/*TUSv*/  {"Sv",     "sievert",   1.0,        { 2, 0,-2, 0, 0, 0, 0}},
/*TUrad*/ {"rad",    "radian",    1.0,        { 0, 0, 0, 0, 0, 0, 0}},
/*TUsr*/  {"sr" ,    "stéradian", 1.0,        { 0, 0, 0, 0, 0, 0, 0}},
/*TUnull*/{"" ,      "sans unité",1.0,        { 0, 0, 0, 0, 0, 0, 0}},
/*TUda*/  {"°",      "degré",     M_PI/180.0, { 0, 0, 0, 0, 0, 0, 0}},
/*TUma*/  {"'",      "minute",    M_PI/10800, { 0, 0, 0, 0, 0, 0, 0}},
/*TUsa*/  {"''",     "seconde",   M_PI/648000,{ 0, 0, 0, 0, 0, 0, 0}},
/*TUtr*/  {"tr",     "degré",     2*M_PI,     { 0, 0, 0, 0, 0, 0, 0}},
/*TUrpm*/ {"rpm",  "tour/minute", 2*M_PI/60,  { 0, 0, -1, 0, 0, 0, 0}},
/*TUangs*/{"\\o{A}", "angström",  1e-10,      { 1, 0, 0, 0, 0, 0, 0}},
/*TUbarn*/{"b",      "barn",      1e-28,      { 2, 0, 0, 0, 0, 0, 0}},
/*TUare*/ {"a",      "are",       1e2,        { 2, 0, 0, 0, 0, 0, 0}},
/*TUl*/   {"L",      "litre",     1e-3,       { 3, 0, 0, 0, 0, 0, 0}},
/*TUt*/   {"t",      "tonne",     1e3,        { 0, 1, 0, 0, 0, 0, 0}},
/*TUbar*/ {"bar",    "bar",       1e5,        {-1, 1,-2, 0, 0, 0, 0}},
/*TUeV*/  {"eV",     "eV",        1.60218e-19,{ 2, 1,-2, 0, 0, 0, 0}},
/*TUuam*/ {"uma",    "uma",       1.66054e-27,{ 0, 1, 0, 0, 0, 0, 0}}
};

 /**
  * preferred SI units list
  * source: Handbook of Chemistry & Physics 78, CRC Press 1997, page 1-20
  * full names of the units are in French, with a Latin-1 encoding
  **/
unite_data pref_units[] ={
/*TUm*/   {"m",      "mètre",   1.0,     { 1, 0, 0, 0, 0, 0, 0}},
/*   */   {"kg",    "kilogramme",1.0,    { 0, 1, 0, 0, 0, 0, 0}},
/*TUs*/   {"s",      "seconde", 1.0,     { 0, 0, 1, 0, 0, 0, 0}},
/*TUA*/   {"A",      "ampère",  1.0,     { 0, 0, 0, 1, 0, 0, 0}},
/*TUK*/   {"K",      "kelvin",  1.0,     { 0, 0, 0, 0, 1, 0, 0}},
/*TUmol*/ {"mol",    "mol",     1.0,     { 0, 0, 0, 0, 0, 1, 0}},
/*TUcd*/  {"cd",     "candela", 1.0,     { 0, 0, 0, 0, 0, 0, 1}},
/*TUHz*/  {"Hz",     "hetrz",   1.0,     { 0,-1, 0, 0, 0, 0, 0}},
/*TUN*/   {"N",      "newton",  1.0,     { 1, 1,-2, 0, 0, 0, 0}},
/*TUPa*/  {"Pa",     "pascal",  1.0,     {-1, 1,-2, 0, 0, 0, 0}},
/*TUJ*/   {"J",      "joule",   1.0,     { 2, 1,-2, 0, 0, 0, 0}},
/*TUW */  {"W",      "watt",    1.0,     { 2, 1,-3, 0, 0, 0, 0}},
/*TUC*/   {"C",      "coulomb", 1.0,     { 0, 0, 1, 1, 0, 0, 0}},
/*TUV*/   {"V",      "volt",    1.0,     { 2, 1,-3,-1, 0, 0, 0}},
/*TUohm*/ {"ohm",    "ohm",     1.0,     { 2, 1,-3,-2, 0, 0, 0}},
/*TUS*/   {"S",      "siemens", 1.0,     {-2,-1, 3, 2, 0, 0, 0}},
/*TUF*/   {"F",      "farad",   1.0,     {-2,-1, 4, 2, 0, 0, 0}},
/*TUT*/   {"T",      "tesla",   1.0,     { 0, 1,-2,-1, 0, 0, 0}},
/*TUWb*/  {"Wb",     "weber",   1.0,     { 2, 1,-2,-1, 0, 0, 0}},
/*TUH*/   {"H",      "henry",   1.0,     { 2, 1,-2,-2, 0, 0, 0}},
/*TUlm*/  {"lm",     "lumen",   1.0,     { 0, 0, 0, 0, 0, 0, 1}},
/*TUlx*/  {"lx",     "lux",     1.0,     {-2, 0, 0, 0, 0, 0, 1}},
};

int significative(char* text);
/**
 * returns the number of significative digits of a decimal number
 * @param text the decimal number as a cstr
 * @return number of significative nubers; leading zeroes are not taken
 * in account. A true zero is a special case.
 **/
int significative(char* text){
  unsigned int i,j;
  int result=0;
  char *end = text+strlen(text);
  while (end>text && *(end-1)==' '){ // removes the rightmost spaces
    end--;
    *end=0;
  }
  for(i=0;
      i<strlen(text)&&(text[i]=='0'||text[i]=='.'|| text[i]=='+'||text[i]=='-');
      i++){
    //skip leading plus,minus,zeros and the decimal point
  }
  if (i==strlen(text)){ // the number is probably zero
    result=1;
    char * dot = strchr(text,'.');
    if (dot != NULL && *dot == '.') result += (end-dot)-1;
  } else { // the number is not zero
    for(j=i;j<strlen(text) && ((text[j]>='0'&&text[j]<='9') || text[j]=='.');j++){
      if (text[j]!='.') {
	result++;
      } else {
	//skip the decimal point
      }
    }
  }
  return result;
}

%}


%token EE
%token DECIMAL
%token INT
%token SPC
%token COLON
%token Uh
%token Umin
%token Um
%token Ug
%token Us
%token UA
%token UK
%token Umol
%token Ucd
%token UHz
%token UN
%token UPa
%token UJ
%token UW
%token UC
%token UVA
%token Uvar
%token UV
%token Uohm
%token US
%token UF
%token UT
%token UWb
%token UH
%token Ulm
%token Ulx
%token UBq
%token UGy
%token USv
%token Urad
%token Usr
%token PUIS
%token PP
%token POINT
%token BARRE
%token Uda
%token Uma
%token Usa
%token Utr
%token Urpm
%token Uangs
%token Ubarn
%token Uare
%token Ul
%token Ut
%token Ubar
%token UeV
%token Uuam
%token Signif
%token PlusminPC

%%

/* the ruleset */

but : valeur_mixte {result = $1;}
| sans_unite {result = $1;}
;

valeur_mixte : valeur spc valeur_mixte {
  int i;
  for(i=0; i < BU_LAST; i++){
    if ($1.base[i] != $3.base[i]) yyerror ("not homogeneous units");
  }
  if ($1.multip <= $3.maxmultip) yyerror ("incorrect multiple units ordering");
  $$.val=$1.val*$1.multip+$3.val*$3.multip;
  $$.multip=1; $$.maxmultip = $1.multip;
  $$.wanted_unit="";
}
| valeur {$$=$1; $$.maxmultip=$1.multip;$$.wanted_unit="";}
| valeur COLON style {
  int i;
  $$=$1; $$.maxmultip=$1.multip;
  for(i=0; i < BU_LAST; i++){
    if ($1.base[i] != $3.base[i]) yyerror ("wanted unit not homogeneous");
  }  
  $$.wanted_multip=$3.multip;
  $$.wanted_unit=$3.s;
}
;

style : unite {$$=$1;}
;

valeur : decimal spc unite {$$=$3; $$.val=val_decimal; $$.signif=0; $$.pcent=0; $$.v=$1.v;}
| decimal spc unite Signif {
  $$=$3; $$.val=val_decimal; 
  $$.signif=val_int; $$.pcent=0;
}
| decimal spc unite PlusminPC {
  $$=$3; $$.val=val_decimal; 
  $$.pcent=val_int; $$.signif=0;
}
;

sans_unite :decimal {
  int i;
  $$=$1;
  $$.val=val_decimal; 
  for (i=0; i < BU_LAST; i++){$$.base[i]=unites[TUnull].base[i];} 
  $$.multip=1.0; 
  $$.signif=0; $$.pcent=0;
  $$.wanted_unit="";
}
| decimal Signif {
  int i;
  $$=$1;
  $$.val=val_decimal; 
  for (i=0; i < BU_LAST; i++){$$.base[i]=unites[TUnull].base[i];} 
  $$.multip=1.0; 
  $$.signif=val_int; $$.pcent=0;
}
| decimal PlusminPC {
  int i;
  $$=$1;
  $$.val=val_decimal; 
  for (i=0; i < BU_LAST; i++){$$.base[i]=unites[TUnull].base[i];} 
  $$.multip=1.0; 
  $$.signif=0; $$.pcent=val_int;
}
;
 
spc : /* nothing */
|SPC
;

unite : unite suiv_unit{
  int index;
  $$.unite = TU_LAST; /* unknown unit (not given) */
  strncpy(buffer,$1.s,MAXBUF); 
  strncat(buffer,$2.s,MAXBUF-strlen($1.s)); 
  free($1.s); free($2.s);
  $$.s=strdup(buffer);
  for(index=0; index< BU_LAST; index++){
    $$.base[index] = $1.base[index]+$2.base[index] ; 
  }
  $$.multip = $1.multip*$2.multip;
}
| prim_unit { $$=$1; }
;

suiv_unit : point prim_unit{
  $$=$2; strncpy(buffer,".",MAXBUF); 
  strncat(buffer,$2.s,MAXBUF-1); 
  free($2.s);
  $$.s=strdup(buffer);
}
| BARRE prim_unit{
  int index;
  $$=$2; 
  $$.multip = 1/ $$.multip;
  strncpy(buffer,"/",MAXBUF); 
  strncat(buffer,$2.s,MAXBUF-1); 
  free($2.s);
  $$.s=strdup(buffer);
  for(index=0; index< BU_LAST; index++){
    $$.base[index] *= -1; 
  }
}
;

point : POINT
;

prim_unit1 : 
Um base_unite {
  $$=$2;
  strncpy(buffer,"m",MAXBUF); 
  strncat(buffer,$2.s,MAXBUF-1); 
  free($2.s);
  $$.s = strdup(buffer); $$.multip*=1e-3; 
}
| UT base_unite {
  $$=$2;
  strncpy(buffer,"T",MAXBUF); 
  strncat(buffer,$2.s,MAXBUF-1); 
  free($2.s);
  $$.s = strdup(buffer); $$.multip*=1e12;
}
| Uh base_unite {
  $$=$2;
  strncpy(buffer,"h",MAXBUF); 
  strncat(buffer,$2.s,MAXBUF-1); 
  free($2.s);
  $$.s = strdup(buffer); $$.multip*=1e2;
}
|  prefixe base_unite {
  $$=$2;
  strncpy(buffer, $1.s,MAXBUF); 
  strncat(buffer, $2.s, MAXBUF-strlen($1.s));
  free($1.s); free($2.s);$$.s=strdup(buffer); 
  $$.multip*=$1.multip;
  }
| base_unite {
  $$=$1;
}
;

prim_unit : prim_unit1 puissance01 {
  int index;
  double r;

  $$.i=$2.i;
  strncpy(buffer, $1.s,MAXBUF);
  if ($2.i!=1){
    strncat(buffer, "^%d",MAXBUF-strlen($1.s)); 
    sprintf(buffer2, buffer, $2.i);
  }
  else strncpy(buffer2,buffer,MAXBUF);
  $$.s=strdup(buffer2); free($1.s);
  for(index=0; index< BU_LAST; index++){
    $$.base[index] = unites[$1.unite].base[index]*$2.i;
  }
  if ($2.i>0){
    for(index=0, r=1; index<$2.i ; index++){
      r = r*$$.multip;
    }
  } else {
    for(index=0, r=1; index>$2.i ; index--){
      r = r* (1/$$.multip);
    }
  }
  $$.multip=r;
}
;

puissance01 : /* nothing */ {$$.i=1;}
| PUIS {$$.i = val_int;}
;

decimal : INT {$$.val=val_decimal;}
| DECIMAL {$$.val=val_decimal;}
| DECIMAL EE INT {val_decimal=Decimal($1.v); int e = atoi($3.v); val_decimal.multiplyByTen(e); $$.val=val_decimal;}
| INT EE INT {val_decimal=atoi($1.v); int e = atoi($3.v); val_decimal.multiplyByTen(e); $$.val=val_decimal;}
;

prefixe : 
PP {$$.multip=1.0; char c=$$.s[0]; 
 switch (c){
 case 'y' : $$.multip = 1e-24; break;
 case 'z' : $$.multip = 1e-21; break;
 case 'a' : $$.multip = 1e-18; break;
 case 'f' : $$.multip = 1e-15; break;
 case 'p' : $$.multip = 1e-12; break;
 case 'n' : $$.multip = 1e-9; break;
 case 'µ' : $$.multip = 1e-6; break;
 case 'c' : $$.multip = 1e-2; break;
 case 'd' : if (!strcmp ($$.s,"da") ) $$.multip = 10.0;
    else $$.multip = 0.1; break;
 case 'h' : $$.multip = 1e2; break;
 case 'k' : $$.multip = 1e3; break;
 case 'M' : $$.multip = 1e6; break;
 case 'G' : $$.multip = 1e9; break;
 case 'P' : $$.multip = 1e15; break;
 case 'E' : $$.multip = 1e18; break;
 case 'Z' : $$.multip = 1e21; break;
 case 'Y': $$.multip = 1e24; break;
 }
}
;

base_unite :  
Uh {$$.unite=TUh; $$.s = strdup("h"); $$.multip=unites[$$.unite].multiplicateur;}
| Umin {$$.unite=TUmin;  $$.multip=unites[$$.unite].multiplicateur;}
| Um {$$.unite=TUm; $$.s = strdup("m"); $$.multip=unites[$$.unite].multiplicateur;}
| Ug {$$.unite=TUg;  $$.multip=unites[$$.unite].multiplicateur;}
| Us {$$.unite=TUs;  $$.multip=unites[$$.unite].multiplicateur;}
| UA { $$.unite = TUA;  $$.multip=unites[$$.unite].multiplicateur;}  
| UK { $$.unite = TUK;  $$.multip=unites[$$.unite].multiplicateur;}  
| Umol { $$.unite = TUmol;  $$.multip=unites[$$.unite].multiplicateur;}  
| Ucd { $$.unite = TUcd;  $$.multip=unites[$$.unite].multiplicateur;}  
| UHz { $$.unite = TUHz;  $$.multip=unites[$$.unite].multiplicateur;}  
| UN { $$.unite = TUN;  $$.multip=unites[$$.unite].multiplicateur;}  
| UPa { $$.unite = TUPa;  $$.multip=unites[$$.unite].multiplicateur;}  
| UJ { $$.unite = TUJ;  $$.multip=unites[$$.unite].multiplicateur;}
| UT { $$.unite = TUT; $$.s = strdup("T"); $$.multip=unites[$$.unite].multiplicateur;}  
| UW { $$.unite = TUW;  $$.multip=unites[$$.unite].multiplicateur;}
| UVA{ $$.unite = TUVA;  $$.multip=unites[$$.unite].multiplicateur;}
| Uvar{ $$.unite = TUvar;  $$.multip=unites[$$.unite].multiplicateur;}
| UC { $$.unite = TUC;  $$.multip=unites[$$.unite].multiplicateur;}  
| UV { $$.unite = TUV;  $$.multip=unites[$$.unite].multiplicateur;}  
| Uohm { $$.unite = TUohm;  $$.multip=unites[$$.unite].multiplicateur;}  
| US { $$.unite = TUS;  $$.multip=unites[$$.unite].multiplicateur;}  
| UF { $$.unite = TUF;  $$.multip=unites[$$.unite].multiplicateur;}  
| UWb { $$.unite = TUWb;  $$.multip=unites[$$.unite].multiplicateur;}  
| UH { $$.unite = TUH;  $$.multip=unites[$$.unite].multiplicateur;}  
| Ulm { $$.unite = TUlm;  $$.multip=unites[$$.unite].multiplicateur;}  
| Ulx { $$.unite = TUlx;  $$.multip=unites[$$.unite].multiplicateur;}  
| UBq { $$.unite = TUBq;  $$.multip=unites[$$.unite].multiplicateur;}  
| UGy { $$.unite = TUGy;  $$.multip=unites[$$.unite].multiplicateur;}  
| USv { $$.unite = TUSv;  $$.multip=unites[$$.unite].multiplicateur;}  
| Urad { $$.unite = TUrad;  $$.multip=unites[$$.unite].multiplicateur;}  
| Usr { $$.unite = TUsr;  $$.multip=unites[$$.unite].multiplicateur;}  
| Uda { $$.unite = TUda;  $$.multip=unites[$$.unite].multiplicateur;} 
| Uma { $$.unite = TUma;  $$.multip=unites[$$.unite].multiplicateur;} 
| Usa { $$.unite = TUsa;  $$.multip=unites[$$.unite].multiplicateur;} 
| Utr { $$.unite = TUtr;  $$.multip=unites[$$.unite].multiplicateur;} 
| Urpm { $$.unite = TUrpm;  $$.multip=unites[$$.unite].multiplicateur;} 
| Uangs { $$.unite = TUangs;  $$.multip=unites[$$.unite].multiplicateur;} 
| Ubarn { $$.unite = TUbarn;  $$.multip=unites[$$.unite].multiplicateur;} 
| Uare { $$.unite = TUare;  $$.multip=unites[$$.unite].multiplicateur;} 
| Ul { $$.unite = TUl;  $$.multip=unites[$$.unite].multiplicateur;} 
| Ut { $$.unite = TUt;  $$.multip=unites[$$.unite].multiplicateur;} 
| Ubar { $$.unite = TUbar;  $$.multip=unites[$$.unite].multiplicateur;} 
| UeV { $$.unite = TUeV;  $$.multip=unites[$$.unite].multiplicateur;} 
| Uuam { $$.unite = TUuam;  $$.multip=unites[$$.unite].multiplicateur;} 
;



%%

#include "uniteslex.cc"
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>

#ifdef RECODE_SUPPORT
  #include <recode.h>
#endif

#include <regex.h>

const char *program_name;
int isUTF8;

/* the program is below */

typedef enum {option_default, option_s, option_o, option_l} optiontype;

yyFlexLexer lexer;
yyFlexLexer * thelexer = &lexer;

int yylex(){
  return thelexer->yylex();
}


/**
 * this function is called when an error is detected. It will print a usable
 * hint about the detected error.
 * @param msg the error message.
 **/
inline int yyerror(const char * msg){
  printf("ERROR at %ld : %s\n", pos, msg);
  exit(1);
}

/**
 * this function can be used to debug new features. It outputs verbose
 * data describing the parsed expression. Messages are in French, with
 * Latin-1 encoding.
 **/
void test_verbeux(){
  int i;
  yyparse();
  if (result.s) {
    if (result.unite == TU_LAST){
      cout << result.s << " " << result.multip*result.val << " SI (équation aux dimensions : ";
      for (i=0; i<BU_LAST; i++){
	printf("%3d", result.base[i]);
      }
      printf(")\n");
    }
    else {
      if (result.i!=1) 
	cout <<  result.s << " (" << result.multip*result.val 
		  << " " << unites[result.unite].nom << ")^{" << result.i 
		  << "}\n";
      else 
	cout << result.s << " " << result.multip*result.val 
		  << " " << unites[result.unite].nom << "\n";
    }
  }
  else fprintf(stderr, "problème : result.s = null\n");
}

/**
 * this function is used to output a normalized string describing the
 * parsed data.
 **
 * here is the format of the output:
 * double int int int int int int int
 * which mean respectively ...
 **
 * value
 * power of the length unit (m)
 * power of the mass unit (kg)
 * power of the time unit (s)
 * power of the current unit (A)
 * power of the temperature unit (K)
 * power of the matter quantity unit (mol)
 * power of the light intensity (cd)
 * number of significative numbers
 * tolerance (as a percentage)
 **/
void sortie_normalisee(){
  int i,s=0,pc=0;

  yyparse();
  // the result will be displaied as a decimal number rather as a fraction
  cout << result.multip*result.val;
  if (count_signif){ // take significative numbers in account
    if (!result.signif) {
      s=significative(result.v);
    } else {
      s=result.signif;
    }
  }
  pc=result.pcent;
  for (i=0; i<BU_LAST; i++){
    printf(" %3d", result.base[i]);
  }
  printf("    %d    %d\n",s,pc);
}

/**
 * displays the unit and eventually the tolerance string
 * @param option may be "option_l" to enforce LaTeX encoding
 * @param unit the name of the unit
 * @param tolerance a percentage of tolerance. If it is non-zero, an
 * additional mention will be displayed to express a tolerance.
 **/
void printUnit(optiontype option, string unit, int tolerance){
  char * indexohm;
  char buffer[128];
  char *codedunit;

#ifdef RECODE_SUPPORT
  if(isUTF8) {
    RECODE_OUTER outer = recode_new_outer (true);
    RECODE_REQUEST request = recode_new_request (outer);
    recode_scan_request (request, "latin1..utf8"); 
    codedunit=strdup(recode_string(request,unit.c_str()));
  } else {
    codedunit=strdup(unit.c_str());
  }
#else
  codedunit=strdup(unit.c_str());
#endif

  if (option==option_l){
    strncpy(buffer, codedunit,sizeof(buffer));
    indexohm=strstr(buffer,"ohm");
    if (indexohm){
      strncpy(indexohm, "\\Omega", sizeof(buffer)-strlen("\\Omega")+strlen("ohm"));
    }
    printf(" %s",buffer);
  } else {
    printf(" %s",codedunit);
  }
  if (tolerance){
    if (option==option_l){
      printf(" \\pm %d\\,\\%%",tolerance);
    } else{
      printf(" +-%d%%",tolerance);
    }
  }
  printf("\n");
  free(codedunit);
}

Decimal my_rounded(Decimal r);
/**
 * Uses the rounding method which are taught to students: if the
 * decimal value is 0.5 or more, rounds away from zero; else
 * rounds towards zero
 * @param r the value to round
 * @return the rounded value
 **/
Decimal my_rounded(Decimal r){
  const char * s=r.c_str();
  const char * cur = strchr(s,'.');
  if (cur==NULL) return Decimal(s);
  char trunc[100];
  int l=cur-s; if (l>99) l=99;
  strncpy(trunc, s, l);
  cur++;
  if (*cur=='\0' || *cur-'5' <0) return Decimal(s);
  else { // the mantissa begins by 5, 6, 7, 8 or 9
    if (Decimal(0.0) <= r) return Decimal(s)+Decimal(1.0);
    else return Decimal(s)+Decimal(-1.0);
  }
}

Decimal my_rounded_signif(Decimal r, int s);
/**
 * rounds a number to keep "s" significant numbers.
 * if s == the number of digits before the decimal dot, this function
 * returns the same value as my_rounded(r)
 * @param r the value to round
 * @param s the number of significant digits
 * @return the rounded value
 **/
Decimal my_rounded_signif(Decimal r, int s){
  Decimal result=r;
  result.keepSignificant(s);
  return result;
}

/**
 * displays a value
 * @param option may be "option_l" to enforce LaTeX encoding
 * @param result the value issued by the parser
 * @param s a number to shift the decimal point. s>0 shifts the decimal
 * point to the right; s <0 shifts the decimal point to the left.
 **/
void printValue(optiontype option, yystype result, int s){
  char val[128],exp[128], *i, *j, *indexE;
  char buf[128];
  regex_t regex;
  regmatch_t matches[3];
  int l;
  int vallen;

  Decimal value=result.multip*result.val;
  if (value==0.0){
    printf("0");
    if (s>1){
      printf(".");
      for (int i=1; i<s; i++){
	printf("0");
      }
    }
    return;
  }
  value=my_rounded_signif(value, s);
  if (s<=1){
    snprintf(buf,sizeof(buf),"%1.0e", value.get_double() );
  } else {
    snprintf(buf,sizeof(buf),"%1.*e", s-1,value.get_double() );
  }
  // simplifications
  regcomp(&regex, "^(.*)(e[+-][0-9]+)$", REG_EXTENDED);
  regexec(&regex, buf, 3, matches,0);
  if (strcmp(buf+matches[2].rm_so,"e+00")==0) {
    // removing e+00
    *(buf+matches[2].rm_so)=0;
  }
  l=strlen(buf);
  if (*(buf+matches[2].rm_so+1)=='+'){
    j=buf+matches[2].rm_so+2;
    while (*j=='0') j++;
    for(i=buf+matches[2].rm_so+1; j<=buf+l; i++,j++){
      // erasing +0* after e
      *i=*j;
    }
  }
  if (*(buf+matches[2].rm_so+1)=='-'){
    j=buf+matches[2].rm_so+2;
    while (*j=='0') j++;
    for(i=buf+matches[2].rm_so+2; j<=buf+l; i++,j++){
      // erasing 0* after e-
      *i=*j;
    }
  }
  if (option==option_l){
    indexE=strstr(buf,"e");
    if (indexE){
      vallen=indexE-buf;
      strncpy(val,buf,vallen);
      val[vallen]=0;
      strncpy(exp,indexE+1,sizeof(exp));
      printf("%s\\cdot 10^{%s}\\,",val,exp);
    } else{
      printf("%s\\,",buf);
    }
  } else {
    printf("%s",buf);
  }
}

/**
 * parses the input and displays parsed data as a text
 * @param option may be "option_l" to enforce LaTeX enconding
 **
 * the output is formated as follows: <value unit>
 * where value is a number expressing the desired value, taking in account
 * the desired significant digits, and unit is a SI unit or some custom unit,
 * optionnally followed by "+- pcent %", pcent being the tolerance.
 **/
void sortie_texte(optiontype option){
  int i,j,
    s=0, pc=0,
    notfirst=0,
    trouve=0,
    nb_pref=sizeof(pref_units)/sizeof(pref_units[0]);

  yyparse();
  if (!result.signif) {
    // the number of significant digits must be computed from the given value
    s=significative(result.v);
  } else {
    // the number of significant digits has been declared
    s=result.signif;
  }
  pc=result.pcent;
  // rounds the value taking in account the number of significant digits
  if (result.wanted_unit.length()>0){
    result.multip /= result.wanted_multip;
  }else{ // set the multiplier to 1 if the input unit has been declared
    if (result.s && strlen(result.s)>0){
      result.multip = 1;
    }
  }
  // print the value
  printValue(option, result, s);
  // if a custom unit is wanted, use it
  if (result.wanted_unit.length()>0){
    printUnit(option, result.wanted_unit, pc);
    return;
  }
  // else print the given unit if it is declared
  if (result.s && strlen(result.s)>0){
    printUnit(option, result.s, pc);
    return;
  }
  // else search a preferred unit
  for (i=0; i<nb_pref; i++){
    trouve=(result.base[0]==pref_units[i].base[0]);
    for (j=1; j<BU_LAST; j++){
      trouve &= (result.base[j]==pref_units[i].base[j]);
    }
    if (trouve){
      printUnit(option, pref_units[i].sym, pc);
      return;
    }
  }
  // test whether there will be a visible unit
  int visible_unit=0;
  for (i=0; i<BU_LAST; i++){
    if (result.base[i]!=0) visible_unit=1;
  }
  if (result.wanted_unit.length()>0) visible_unit=1;
  if(visible_unit!=0){ // display a unit only if it has some sense!
    for (i=0; i<BU_LAST; i++){
      if (result.base[i]!=0){
	if (notfirst) printf("."); else printf(" ");
	printf("%s",unit_names[i]);
	notfirst=1;
	if (result.base[i]!=1){
	  if (option==option_l){
	    printf("^{%d}",result.base[i]);
	  } else {
	    printf("^%d",result.base[i]);
	  }
	}
      }
    }
    printUnit(option, "", pc);
  }
}

/**
 * main function
 * @param argc the number of arguments of the program's call
 * @param argv the vector of arguments
 * @return the exit code of the program
 **/
int main(int argc, char * argv[]){
  const char * optstr = "osl";
  char * envoption=getenv("units_option");
  // environmental option take precedence on command-line options

  optiontype option=option_default;
  
  int ch;
  while (-1 != (ch=getopt(argc,argv,optstr))){
    switch(ch){
    case 's': 
      option=option_s;
      break;
    case 'o':
      option=option_o;
      break;
    case 'l':
      option=option_l;
      break;
    default: 
      break;
    }
  }
  // environmental option take precedence on command-line options
  if (envoption && strncmp(envoption, "s", 2)==0) option=option_s;
  if (envoption && strncmp(envoption, "o", 2)==0) option=option_o;
  if (envoption && strncmp(envoption, "l", 2)==0) option=option_l;
#ifdef RECODE_SUPPORT
  // check whether we are in a UTF8 environment
  char *lc_all=getenv("LC_ALL");
  isUTF8 = (lc_all!= NULL && strstr(lc_all,"UTF-8"));
#endif
  program_name = argv[0];


switch(option){
  case option_s: 
    count_signif=1;
    sortie_normalisee();
    return 0;
    break;
  case option_o:
  case option_l:
    sortie_texte(option);
    return 0;
    break;
  default: 
    break;
  }
  // disabled; use only for debug purpose
  /*test_verbeux();*/
  sortie_normalisee();
  return 0;
}
