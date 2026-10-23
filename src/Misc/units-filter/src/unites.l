/* -*- coding: iso-8859-1 -*- */
%option noyywrap
%option c++
     
   #include <stdlib.h>
   #include "unites.h"

spc            [ \t]+

/* les nombres */
digits         [0-9]
int            [+-]?{digits}+
mantisse       {digits}+
ee             [Ee]|\*10^
decimal        {int}([.]{mantisse})
signif         {spc}*\#{digits}+{spc}*
plusminpc      {spc}*~{digits}{digits}?{spc}*
plusminpcbis   {spc}*\+\-{spc}*{digits}{digits}?%{spc}*

/*quelques séparateurs particuliers*/
point          {spc}*\.{spc}*|{spc}*\*{spc}*
barre          {spc}*\/{spc}*
puis           {spc}*^{spc}*
colon          {spc}*:{spc}*

/* les unités de temps hors système */
heure_ou_hecto h
minute         min

/* les unités du SI */
/* Handbook of Chemistry & Physics 78, CRC Press 1997, page 1-20 */
milli_ou_metre m
are            a
gramme         g
seconde        s
ampere         A
kelvin         K
degre_c        °C
mole           mol
candela        cd
hertz          Hz
newton         N
pascal         Pa
joule          J
watt           W
coulomb        C
voltampere     VA
voltamperereactif var
volt           V
ohm            Ohm|ohm
siemens        S
farad          F
tera_ou_tesla  T
weber          Wb
henry          H
lumen          lm
lux            lx
becquerel      Bq
gray           Gy
sievert        Sv
radian         rad
steradian      sr

/* unités annexes, voir page 1-22 */
degre_angle    °
minute_angle   \'
seconde_angle  \'\'
tour           tr
tourminute     rpm
angstrom       °A
barn           b
litre          l|L
tonne          t
bar            bar
ev             eV
uam            u|uma

/* prefixes sauf milli et téra */
/* Handbook of Chemistry & Physics 78, CRC Press 1997, page 1-21 */
prefixe_pur    [yzafpnµcdhkMGPEZY]|da

/* notation des exposants */
puissance      {puis}{int}
carre          ²
cube           ³


%%

{ee}                { pos+=yyleng; yylval.s=strdup(yytext); return EE;}
{decimal}           { val_decimal=Decimal(yytext); yylval.v=strdup(yytext); pos+=yyleng; return DECIMAL;}
{int}               { val_decimal=Decimal(yytext); yylval.v=strdup(yytext); pos+=yyleng; return INT;}
{heure_ou_hecto}    { pos+=yyleng; yylval.s=strdup(yytext); return Uh;}
{minute}            { pos+=yyleng; yylval.s=strdup(yytext); return Umin;}
{milli_ou_metre}    { pos+=yyleng; yylval.s=strdup(yytext); return Um;}
{are}               { pos+=yyleng; yylval.s=strdup(yytext); return Uare;}
{gramme}            { pos+=yyleng; yylval.s=strdup(yytext); return Ug;}
{seconde}           { pos+=yyleng; yylval.s=strdup(yytext); return Us;}
{ampere}            { pos+=yyleng; yylval.s=strdup(yytext); return UA;}
{kelvin}|{degre_c}  { pos+=yyleng; yylval.s=strdup(yytext); return UK;}
{mole}              { pos+=yyleng; yylval.s=strdup(yytext); return Umol;}
{candela}           { pos+=yyleng; yylval.s=strdup(yytext); return Ucd;}
{hertz}             { pos+=yyleng; yylval.s=strdup(yytext); return UHz;}
{newton}            { pos+=yyleng; yylval.s=strdup(yytext); return UN;}
{pascal}            { pos+=yyleng; yylval.s=strdup(yytext); return UPa;}
{joule}             { pos+=yyleng; yylval.s=strdup(yytext); return UJ;}
{watt}              { pos+=yyleng; yylval.s=strdup(yytext); return UW;}
{coulomb}           { pos+=yyleng; yylval.s=strdup(yytext); return UC;}
{voltampere}        { pos+=yyleng; yylval.s=strdup(yytext); return UVA;}
{voltamperereactif} { pos+=yyleng; yylval.s=strdup(yytext); return Uvar;}
{volt}              { pos+=yyleng; yylval.s=strdup(yytext); return UV;}
{ohm}               { pos+=yyleng; yylval.s=strdup(yytext); return Uohm;}
{siemens}           { pos+=yyleng; yylval.s=strdup(yytext); return US;}
{farad}             { pos+=yyleng; yylval.s=strdup(yytext); return UF;}
{tera_ou_tesla}     { pos+=yyleng; yylval.s=strdup(yytext); return UT;}
{weber}             { pos+=yyleng; yylval.s=strdup(yytext); return UWb;}
{henry}             { pos+=yyleng; yylval.s=strdup(yytext); return UH;}
{lumen}             { pos+=yyleng; yylval.s=strdup(yytext); return Ulm;}
{lux}               { pos+=yyleng; yylval.s=strdup(yytext); return Ulx;}
{becquerel}         { pos+=yyleng; yylval.s=strdup(yytext); return UBq;}
{gray}              { pos+=yyleng; yylval.s=strdup(yytext); return UGy;}
{sievert}           { pos+=yyleng; yylval.s=strdup(yytext); return USv;}
{radian}            { pos+=yyleng; yylval.s=strdup(yytext); return Urad;}
{steradian}         { pos+=yyleng; yylval.s=strdup(yytext); return Usr;}
{puissance}         { pos+=yyleng; while (!index("-0123456789", *yytext)) yytext++; val_int=atoi(yytext); return PUIS;}
{carre}             { val_int=2; pos+=yyleng; return PUIS;}
{cube}              { val_int=3; pos+=yyleng; return PUIS;}
{prefixe_pur}       { pos+=yyleng; yylval.s=strdup(yytext); return PP;}
{point}             { pos+=yyleng; return POINT;}
{barre}             { pos+=yyleng; return BARRE;}
{degre_angle}       { pos+=yyleng; yylval.s=strdup(yytext); return Uda;}
{minute_angle}      { pos+=yyleng; yylval.s=strdup(yytext); return Uma;}
{seconde_angle}     { pos+=yyleng; yylval.s=strdup(yytext); return Usa;}
{tour}              { pos+=yyleng; yylval.s=strdup(yytext); return Utr;}
{tourminute}        { pos+=yyleng; yylval.s=strdup(yytext); return Urpm;}
{angstrom}          { pos+=yyleng; yylval.s=strdup(yytext); return Uangs;}
{barn}              { pos+=yyleng; yylval.s=strdup(yytext); return Ubarn;}
{litre}             { pos+=yyleng; yylval.s=strdup(yytext); return Ul;}
{tonne}             { pos+=yyleng; yylval.s=strdup(yytext); return Ut;}
{bar}               { pos+=yyleng; yylval.s=strdup(yytext); return Ubar;}
{ev}                { pos+=yyleng; yylval.s=strdup(yytext); return UeV;}
{uam}               { pos+=yyleng; yylval.s=strdup(yytext); return Uuam;}
{spc}               { pos+=yyleng; return SPC;}
{signif}            { pos+=yyleng; val_int=atoi(strchr(yytext,'#')+1); return Signif;}
{plusminpc}         { pos+=yyleng; val_int=atoi(strchr(yytext,'~')+1); return PlusminPC;}
{plusminpcbis}      { pos+=yyleng; val_int=atoi(strchr(yytext,'+')+2); return PlusminPC;}
{colon}             { pos+=yyleng; return COLON;}

.                    pos+=yyleng;
\n                   pos+=yyleng;
