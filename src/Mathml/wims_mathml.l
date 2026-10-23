/*             wims_mathml 1.4.10
 *   wims_mathml.l last modified 6/16/2012
 */

%{
%}
%option noyywrap
%x MATH FINDARG ATTRIBUTELIST BBOLD FRAKTUR CALLIGRAPHIC ROMAN PLAINTEXT INT1 INT2 INT3 MATHENV SVGENV SVGEND HTMLENV HTMLEND  OPTARG ARRAYOPTIONS COLOPTS TEXTOPTARG TEXTOPTARG1 RAISEBOX1 RAISEBOX2
%{
#include <string.h>

#define YYSTYPE char *

#include "y.tab.h"
#include "wims_mathml.h"

#define YY_NEVER_INTERACTIVE 1
#define YY_INPUT(buffer,result,max_size) result = s_wims_mathml_read (buffer, max_size)

static int s_wims_mathml_read (char * buffer, int max_size);

#define yylval wims_mathml_yylval
#define wims_mathml_optarg_depth 100

extern char * yylval;
int wims_mathml_shortcut = 0;
int wims_mathml_lineno = 1;
int wims_mathml_inoptarg[wims_mathml_optarg_depth];
int wims_mathml_optarg_ind = 0;
int wims_mathml_env_start = 0;

int wims_mathml_rowposn = 0;
int wims_mathml_displaymode = 0;

%}
%%
%{ if (wims_mathml_rowposn==1) wims_mathml_rowposn=0;
   if (wims_mathml_rowposn==2) wims_mathml_rowposn=1;
%}

<INITIAL>"$"{1,2}   BEGIN(MATH); wims_mathml_inoptarg[0] = 0; wims_mathml_rowposn=2; if (yyleng==2) { wims_mathml_displaymode=1; return STARTDMATH;}; wims_mathml_displaymode=0; return STARTMATH;
<INITIAL>"\\[" BEGIN(MATH); wims_mathml_inoptarg[0] = 0; wims_mathml_rowposn=2; wims_mathml_displaymode=1; return STARTDMATH;
<INITIAL>EOF    return 0;
<INITIAL>"\n" {wims_mathml_lineno++; yylval=wims_mathml_copy_string(yytext); return CHAR;}
<INITIAL>.     {yylval=wims_mathml_copy_string(yytext); return CHAR;}


<MATH>{



"$"{1,2} |
"\\]"           {BEGIN(INITIAL); return ENDMATH;}


"^"            return SUP;
"_"            return SUB;
"{"[\t\r\n ]*"}"   return EMPTYMROW;
"{"            {wims_mathml_rowposn = 2; if(wims_mathml_inoptarg[wims_mathml_optarg_ind] != 0) wims_mathml_inoptarg[wims_mathml_optarg_ind]++; return MROWOPEN;}
"}"            {if(wims_mathml_inoptarg[wims_mathml_optarg_ind] != 0) wims_mathml_inoptarg[wims_mathml_optarg_ind]--; return MROWCLOSE;}

"\\left"       {wims_mathml_rowposn=2; return LEFT;}
"\\right"      {return RIGHT;}

"\\big"   |
"\\bigr"  {return BIG;}
"\\Big"   |
"\\Bigr"  {return BBIG;}
"\\bigg"  |
"\\biggr" {return BIGG;}
"\\Bigg"  |
"\\Biggr" {return BBIGG;}

"\\bigl"  {return BIGL;}
"\\Bigl"  {return BBIGL;}
"\\biggl" {return BIGGL;}
"\\Biggl" {return BBIGGL;}

"\\mathrlap"  {return RLAP;}
"\\mathllap"  {return LLAP;}
"\\mathclap"  {return CLAP;}
"\\rlap" |
"\\llap" |
"\\ulap" |
"\\dlap"               {/* Ignore */ ;}


"&amp;" |
"&"         {wims_mathml_rowposn=2; return COLSEP;}

"\\&"           {yylval=wims_mathml_copy_string("&amp;"); return MI;}

"\\$"           {yylval=wims_mathml_copy_string("$"); return MI;}
"\\%"           {yylval=wims_mathml_copy_string("%"); return MI;}


[a-zA-Z]+       {yylval=wims_mathml_copy_string(yytext); return MI;}

[0-9]{1,3}(,[0-9]{3,3})*(\.[0-9]+)? |
[0-9]*[\.][0-9]+                   |
[0-9]+           {yylval=wims_mathml_copy_string(yytext); return MN;}
"\\infty"   |
"\\infinity"     {yylval=wims_mathml_copy_string("&infin;"); return MN;}

"("   |
"["             {yylval=wims_mathml_copy_string(yytext); return LEFTDELIM;}

")"  {yylval=wims_mathml_copy_string(yytext); return RIGHTDELIM;}
"]"   {if(wims_mathml_inoptarg[wims_mathml_optarg_ind]==1 && wims_mathml_optarg_ind > 0) {wims_mathml_inoptarg[wims_mathml_optarg_ind]=0; wims_mathml_optarg_ind--; return OPTARGCLOSE;} else {yylval=wims_mathml_copy_string(yytext); return RIGHTDELIM;}}

"\\lgroup" |
"\\lpar" {yylval=wims_mathml_copy_string("("); return OTHERDELIM;}

"\\rgroup" |
"\\rpar" {yylval=wims_mathml_copy_string(")"); return OTHERDELIM;}

"\\lbracket" {yylval=wims_mathml_copy_string("["); return OTHERDELIM;}
"\\rbracket" {yylval=wims_mathml_copy_string("]"); return OTHERDELIM;}

"\\lbrace" |
"\\{"		{yylval=wims_mathml_copy_string("{"); return OTHERDELIM;}

"\\rbrace" |
"\\}" 		{yylval=wims_mathml_copy_string("}"); return OTHERDELIM;}


"||" |
"\\||" |
"\\Arrowvert" |
"\\vert\\vert" |
"\\Vert"   {yylval=wims_mathml_copy_string("&DoubleVerticalBar;"); return OTHERDELIM;}


"|" |
"\\|" |
"\\vert" |
"\\arrowvert" {yylval=wims_mathml_copy_string("&VerticalBar;"); return OTHERDELIM;}

"\\lvert" |
"\\bracevert"  {yylval=wims_mathml_copy_string("&VerticalBar;"); return LEFTDELIM;}
"\\rvert"  {yylval=wims_mathml_copy_string("&VerticalBar;"); return RIGHTDELIM;}


"\\setminus"   {yylval=wims_mathml_copy_string("&setminus;"); return MO;}
"\\backslash"   {yylval=wims_mathml_copy_string("&bsol;"); return MO;}
"\\smallsetminus"   {yylval=wims_mathml_copy_string("&#xFE68;"); return MOF;}

"\\sslash" {yylval=wims_mathml_copy_string("&parsl;"); return MO;}

"\\lfloor" |
"\\lceil"  |
"\\lang"   |
"\\langle"      {yylval=wims_mathml_copy2(yytext, ";"); if (yylval != wims_mathml_empty_string) yylval[0]='&'; return LEFTDELIM;}

"\\rfloor" |
"\\rceil"  |
"\\rang"   |
"\\rangle"      {yylval=wims_mathml_copy2(yytext, ";"); if (yylval != wims_mathml_empty_string) yylval[0]='&'; return RIGHTDELIM;}

"/"             {yylval=wims_mathml_copy_string(yytext); return OTHERDELIM;}


"\\uparrow"   |
"\\downarrow" |
"\\updownarrow" {yylval=wims_mathml_copy2(yytext, ";"); if (yylval != wims_mathml_empty_string) yylval[0]='&'; return OTHERDELIM;}

"."             {yylval=wims_mathml_copy_string(yytext); return PERIODDELIM;}

"-"    {if (wims_mathml_rowposn==1) return UNARYMINUS; else {yylval=wims_mathml_copy_string("&minus;"); return MO;}}

"+"    {if (wims_mathml_rowposn==1) return UNARYPLUS; else {yylval=wims_mathml_copy_string("+"); return MO;}}

"\\#" {yylval=wims_mathml_copy_string("#"); return MOL;}


","   |
":"   |
"!"   |
"="   |
"~"   |
";"   |
"?"   |
"#"   |
"`"   |
"*"        {yylval=wims_mathml_copy_string(yytext); return MOL;}

"\\prime"  {yylval=wims_mathml_copy_string("&prime;"); return MOLL;}
"'"        {yylval=wims_mathml_copy_string("&prime;"); return MOL;}
"''"     {yylval=wims_mathml_copy_string("&Prime;"); return MOL;}
"'''"  {yylval=wims_mathml_copy_string("&#8244;"); return MOL;}
"''''"  {yylval=wims_mathml_copy_string("&#8279;"); return MOL;}

"\\alpha"   |
"\\beta" |
"\\gamma" |
"\\delta" |
"\\zeta" |
"\\eta"  |
"\\theta" |
"\\iota"  |
"\\kappa" |
"\\lambda" |
"\\mu"     |
"\\nu"    |
"\\xi"   |
"\\pi"     |
"\\rho"   |
"\\sigma"  |
"\\tau"   |
"\\upsilon" |
"\\chi"  |
"\\psi"  |
"\\omega" |
"\\backepsilon"   |
"\\varkappa" |
"\\varpi" |
"\\varrho" |
"\\varsigma" |
"\\vartheta"      {yylval=wims_mathml_copy2(yytext, ";"); if (yylval != wims_mathml_empty_string) yylval[0]='&'; return MI;}

"\\varepsilon" {yylval=wims_mathml_copy_string("&#x03B5;"); return MI;}
"\\phi"  {yylval=wims_mathml_copy_string("&#x03D5;"); return MI;}
"\\varphi"  {yylval=wims_mathml_copy_string("&#x03C6;"); return MI;}

"\\arccos"  |
"\\arcsin"  |
"\\arctan"  |
"\\arg"     |
"\\Arg"     |
"\\cos"     |
"\\cosh"    |
"\\cot"     |
"\\coth"    |
"\\csc"     |
"\\dim"     |
"\\exp"     |
"\\hom"     |
"\\ker"     |
"\\lg"      |
"\\ln"      |
"\\log"     |
"\\sec"     |
"\\sin"     |
"\\sinh"    |
"\\tan"     |
"\\tanh"      {yylval=wims_mathml_copy_string(yytext+1); return MI;}

"\\det"     |
"\\gcd"     |
"\\inf"     |
"\\lim"     |
"\\liminf"  |
"\\limsup"  |
"\\max"     |
"\\min"     |
"\\Pr"      |
"\\sup"    {yylval=wims_mathml_copy_string(yytext+1); return MIB;}

"\\omicron"  {yylval=wims_mathml_copy_string("&oscr;"); return MI;}

"\\epsilon"  {yylval=wims_mathml_copy_string("&epsi;"); return MI;}

"\\cdot"   {yylval=wims_mathml_copy_string("&sdot;"); return MO;}

"\\Alpha"   |
"\\Beta" |
"\\Delta"  |
"\\Gamma"  |
"\\digamma"  |
"\\Lambda" |
"\\Pi"     |
"\\Phi"    |
"\\Psi"    |
"\\Sigma"  |
"\\Theta"  |
"\\Xi"     |
"\\Zeta" |
"\\Eta"  |
"\\Iota"  |
"\\Kappa" |
"\\Mu"     |
"\\Nu"    |
"\\Rho"   |
"\\Tau"  |
"\\mho"  |
"\\Omega"     {yylval=wims_mathml_copy2(yytext, ";"); if (yylval != wims_mathml_empty_string) yylval[0]='&'; return MI;}

"\\Upsilon" |
"\\Upsi" {yylval=wims_mathml_copy_string("&Upsi;"); return MI;}

"\\iff" {yylval=wims_mathml_copy_string("&iff;"); return MOL;}
"\\longleftarrow" {yylval = wims_mathml_copy_string("&#x27F5;"); return MOL;}
"\\Longleftarrow" {yylval = wims_mathml_copy_string("&xlArr;"); return MOL;}
"\\Longleftrightarrow"  {yylval=wims_mathml_copy_string("&#x27FA;"); return MOL;}
"\\Leftrightarrow"  {yylval=wims_mathml_copy_string("&Leftrightarrow;"); return MOL;}
"\\longleftrightarrow" {yylval=wims_mathml_copy_string("&#x27F7;"); return MOL;}

"\\impliedby" |
"\\Leftarrow"   {yylval=wims_mathml_copy_string("&Leftarrow;"); return MOL;}

"\\implies" |
"\\Rightarrow"   {yylval=wims_mathml_copy_string("&Rightarrow;"); return MOL;}

"\\hookleftarrow" {yylval=wims_mathml_copy_string("&hookleftarrow;"); return MOL;}

"\\embedsin" |
"\\hookrightarrow"  {yylval=wims_mathml_copy_string("&hookrightarrow;"); return MOL;}

"\\longrightarrow"  {yylval=wims_mathml_copy_string("&longrightarrow;"); return MOL;}

"\\Longrightarrow"  {yylval=wims_mathml_copy_string("&#x27F9;"); return MOL;}

"\\leftarrow"   {yylval=wims_mathml_copy_string("&leftarrow;"); return MOL;}

"\\to"    |
"\\rightarrow"  {yylval=wims_mathml_copy_string("&rightarrow;"); return MOL;}

"\\leftrightarrow"  {yylval=wims_mathml_copy_string("&leftrightarrow;"); return MOL;}

"\\longmapsto" {yylval = wims_mathml_copy_string("&#x27FC;"); return MOL;}
"\\Longmapsfrom" {yylval = wims_mathml_copy_string("&#x27FD;"); return MOL;}
"\\Longmapsto" {yylval = wims_mathml_copy_string("&#x27FE;"); return MOL;}
"\\mapsto"  |
"\\map"       {yylval=wims_mathml_copy_string("&map;"); return MOL;}

"\\nearrow" |
"\\nearr"    {yylval=wims_mathml_copy_string("&nearr;"); return MOL;}

"\\nwarrow" |
"\\nwarr"     {yylval=wims_mathml_copy_string("&nwarr;"); return MOL;}

"\\searrow" |
"\\searr"     {yylval=wims_mathml_copy_string("&searr;"); return MOL;}

"\\swarrow" |
"\\swarr"     {yylval=wims_mathml_copy_string("&swarr;"); return MOL;}

"\\neArrow" |
"\\neArr"    {yylval=wims_mathml_copy_string("&neArr;"); return MOL;}

"\\nwArrow" |
"\\nwArr"     {yylval=wims_mathml_copy_string("&nwArr;"); return MOL;}

"\\seArrow" |
"\\seArr"     {yylval=wims_mathml_copy_string("&seArr;"); return MOL;}

"\\swArrow" |
"\\swArr"     {yylval=wims_mathml_copy_string("&swArr;"); return MOL;}

"\\darr"     {yylval=wims_mathml_copy_string("&darr;"); return MOL;}

"\\Downarrow" {yylval=wims_mathml_copy_string("&dArr;"); return MOL;}

"\\uparr"    {yylval=wims_mathml_copy_string("&uarr;"); return MOL;}

"\\Uparrow" {yylval=wims_mathml_copy_string("&uArr;"); return MOL;}

"\\downuparrow" |
"\\duparr" |
"\\updarr"  {yylval=wims_mathml_copy_string("&updownarrow;"); return MOL;}

"\\Updownarrow"  {yylval=wims_mathml_copy_string("&Updownarrow;"); return MOL;}

"\\leftsquigarrow"       {yylval=wims_mathml_copy_string("&#x21DC;"); return MOL;}
"\\rightsquigarrow"      {yylval=wims_mathml_copy_string("&zigrarr;"); return MOL;}
"\\dashleftarrow"        {yylval=wims_mathml_copy_string("&#x290E;"); return MOL;}
"\\dashrightarrow"       {yylval=wims_mathml_copy_string("&#x290F;"); return MOL;}
"\\curvearrowbotright"   {yylval=wims_mathml_copy_string("&#x293B;"); return MOL;}
"\\righttoleftarrow"     {yylval=wims_mathml_copy_string("&#x27F2;"); return MOL;}
"\\lefttorightarrow"     {yylval=wims_mathml_copy_string("&#x27F3;"); return MOL;}
"\\leftrightsquigarrow" |
"\\upuparrows"          |
"\\rightleftarrows"     |
"\\rightrightarrows"    |
"\\curvearrowleft"      |
"\\curvearrowright"     |
"\\downdownarrows"      |
"\\leftarrowtail"       |
"\\rightarrowtail"      |
"\\leftleftarrows"      |
"\\leftrightarrows"     |
"\\Lleftarrow"          |
"\\Rrightarrow"         |
"\\looparrowleft"       |
"\\looparrowright"      |
"\\Lsh"                 |
"\\Rsh"                 |
"\\circlearrowleft"     |
"\\circlearrowright"    |
"\\twoheadleftarrow"    |
"\\twoheadrightarrow"   |
"\\nLeftarrow"          |
"\\nleftarrow"          |
"\\nLeftrightarrow"     |
"\\nleftrightarrow"     |
"\\nRightarrow"         |
"\\nrightarrow"         |
"\\downharpoonleft"     |
"\\downharpoonright"    |
"\\leftrightharpoons"   |
"\\rightleftharpoons"   |
"\\upharpoonleft"       |
"\\upharpoonright"   {yylval=wims_mathml_copy2(yytext, ";"); if (yylval != wims_mathml_empty_string) yylval[0]='&'; return MOL;}

"\\xrightarrow"        |
"\\xleftarrow"         |
"\\xleftrightarrow"    |
"\\xLeftarrow"         |
"\\xRightarrow"        |
"\\xLeftrightarrow"    |
"\\xleftrightharpoons" |
"\\xrightleftharpoons" |
"\\xhookleftarrow"     |
"\\xhookrightarrow"    |
"\\xmapsto" {BEGIN(OPTARG); char * s1 = wims_mathml_copy_string(yytext+2); yylval = wims_mathml_copy3("&", s1, ";"); wims_mathml_free_string(s1); return XARROW;}

"\\dots" |
"\\ldots"  {yylval=wims_mathml_copy_string("&hellip;"); return MI;}
"\\cdots"   {yylval=wims_mathml_copy_string("&ctdot;"); return MI;}
"\\ddots"   {yylval=wims_mathml_copy_string("&dtdot;"); return MI;}
"\\udots"   {yylval=wims_mathml_copy_string("&utdot;"); return MI;}
"\\vdots"   {yylval=wims_mathml_copy_string("&vellip;"); return MI;}
"\\colon"   {yylval=wims_mathml_copy_string("&colon;"); return MOR;}

"\\cup"  |
"\&union;" |
"\\union"          {yylval=wims_mathml_copy_string("&cup;"); return MOL;}

"\\bigcup" |
"\\Union"  |
"\&Union;"      {yylval=wims_mathml_copy_string("&Union;"); return MOB;}

"\\cap"  |
"\\intersection"          {yylval=wims_mathml_copy_string("&cap;"); return MOL;}

"\\bigcap" |
"\\Intersection"       {yylval=wims_mathml_copy_string("&Intersection;"); return MOB;}

"\\in"           {yylval=wims_mathml_copy_string("&Element;"); return MOL;}

 /* For some reason, Firefox does not put in the right spacing around some of these operators
  * unless we ask for it, explicitly.
  */
"\\coloneqq"     {yylval=wims_mathml_copy_string("&colone;"); return MOL;}
"\\Coloneqq"     {yylval=wims_mathml_copy_string("&Colone;"); return MOS;}
"\\coloneq"      {yylval=wims_mathml_copy_string("&#x2236;&#x2212;"); return MOS;}
"\\Coloneq"      {yylval=wims_mathml_copy_string("&#x2237;&#x2212;"); return MOS;}
"\\eqqcolon"     {yylval=wims_mathml_copy_string("&ecolon;"); return MOS;}
"\\Eqqcolon"     {yylval=wims_mathml_copy_string("&#x003d;&#x2237;"); return MOS;}
"\\eqcolon"      {yylval=wims_mathml_copy_string("&#x2239;"); return MOS;}
"\\Eqcolon"      {yylval=wims_mathml_copy_string("&#x2212;&#x2237;"); return MOS;}
"\\colonapprox"  {yylval=wims_mathml_copy_string("&#x2236;&#x2248;"); return MOS;}
"\\Colonapprox"  {yylval=wims_mathml_copy_string("&#x2237;&#x2248;"); return MOS;}
"\\colonsim"     {yylval=wims_mathml_copy_string("&#x2236;&#x223C;"); return MOS;}
"\\Colonsim"     {yylval=wims_mathml_copy_string("&#x2237;&#x223C;"); return MOS;}
"\\dblcolon"     {yylval=wims_mathml_copy_string("&Colon;"); return MOL;}

"\\ast"  |
"\\Cap"  |
"\\Cup"  |
"\\circledast"  |
"\\circledcirc" |
"\\curlyvee"    |
"\\curlywedge"  |
"\\divideontimes" |
"\\dotplus" |
"\\leftthreetimes" |
"\\rightthreetimes" |
"\\veebar" |
"\\gt"   |
"\\lt"   |
"\\approxeq"   |
"\\backsim"   |
"\\backsimeq"   |
"\\barwedge"    |
"\\doublebarwedge" |
"\\subset"  |
"\\subseteq" |
"\\subseteqq" |
"\\subsetneq" |
"\\subsetneqq" |
"\\varsubsetneq" |
"\\varsubsetneqq" |
"\\prec"     |
"\\parallel"  |
"\\nparallel" |
"\\shortparallel" |
"\\nshortparallel" |
"\\perp"  |
"\\eqslantgtr" |
"\\eqslantless" |
"\\gg" |
"\\ggg" |
"\\geq" |
"\\geqq" |
"\\geqslant" |
"\\gneq" |
"\\gneqq" |
"\\gnapprox" |
"\\gnsim" |
"\\gtrapprox" |
"\\ge"  |
"\\le"  |
"\\leq" |
"\\leqq" |
"\\leqslant" |
"\\lessapprox" |
"\\lessdot" |
"\\lesseqgtr" |
"\\lesseqqgtr" |
"\\lessgtr" |
"\\lneq" |
"\\lneqq" |
"\\lnsim" |
"\\lvertneqq" |
"\\gtrsim" |
"\\gtrdot" |
"\\gtreqless" |
"\\gtreqqless" |
"\\gtrless" |
"\\gvertneqq" |
"\\lesssim" |
"\\lnapprox" |
"\\nsubset" |
"\\nsubseteq" |
"\\nsubseteqq" |
"\\notin" |
"\\ni" |
"\\notni" |
"\\nmid" |
"\\nshortmid" |
"\\preceq" |
"\\npreceq" |
"\\ll" |
"\\ngeq" |
"\\ngeqq" |
"\\ngeqslant" |
"\\nleq" |
"\\nleqq" |
"\\nleqslant" |
"\\nless" |
"\\supset" |
"\\supseteq" |
"\\supseteqq" |
"\\supsetneq" |
"\\supsetneqq" |
"\\varsupsetneq" |
"\\varsupsetneqq" |
"\\approx" |
"\\asymp" |
"\\bowtie" |
"\\dashv" |
"\\Vdash" |
"\\vDash" |
"\\VDash" |
"\\vdash" |
"\\Vvdash" |
"\\models" |
"\\sim" |
"\\simeq" |
"\\nsim" |
"\\smile" |
"\\triangle" |
"\\triangledown" |
"\\triangleleft" |
"\\cong" |
"\\succ" |
"\\nsucc" |
"\\ngtr" |
"\\nsupset" |
"\\nsupseteq" |
"\\propto" |
"\\equiv" |
"\\nequiv" |
"\\frown" |
"\\triangleright" |
"\\ncong" |
"\\succeq" |
"\\succapprox" |
"\\succnapprox" |
"\\succcurlyeq" |
"\\succsim" |
"\\succnsim" |
"\\nsucceq" |
"\\nvDash" |
"\\nvdash" |
"\\nVDash" |
"\\amalg" |
"\\pm" |
"\\mp" |
"\\bigcirc" |
"\\wr" |
"\\odot" |
"\\uplus" |
"\\clubsuit" |
"\\spadesuit" |
"\\Diamond" |
"\\diamond" |
"\\sqcup" |
"\\sqcap" |
"\\sqsubset" |
"\\sqsubseteq" |
"\\sqsupset" |
"\\sqsupseteq" |
"\\Subset" |
"\\Supset" |
"\\ltimes" |
"\\div" |
"\\rtimes" |
"\\bot" |
"\\therefore" |
"\\thickapprox" |
"\\thicksim" |
"\\varpropto" |
"\\varnothing" |
"\\flat" |
"\\vee" |
"\\because" |
"\\between" |
"\\Bumpeq" |
"\\bumpeq" |
"\\circeq" |
"\\curlyeqprec" |
"\\curlyeqsucc" |
"\\doteq" |
"\\doteqdot" |
"\\eqcirc" |
"\\fallingdotseq" |
"\\multimap" |
"\\pitchfork" |
"\\precapprox" |
"\\precnapprox" |
"\\preccurlyeq" |
"\\precsim" |
"\\precnsim" |
"\\risingdotseq" |
"\\sharp" |
"\\bullet" |
"\\nexists" |
"\\dagger" |
"\\ddagger" |
"\\not" |
"\\top" |
"\\natural" |
"\\angle" |
"\\measuredangle" |
"\\backprime" |
"\\bigstar" |
"\\blacklozenge" |
"\\lozenge" |
"\\blacksquare" |
"\\blacktriangle" |
"\\blacktriangleleft" |
"\\blacktriangleright" |
"\\blacktriangledown" |
"\\ntriangleleft" |
"\\ntriangleright" |
"\\ntrianglelefteq" |
"\\ntrianglerighteq" |
"\\trianglelefteq" |
"\\trianglerighteq" |
"\\triangleq" |
"\\vartriangleleft" |
"\\vartriangleright" |
"\\forall" |
"\\bigtriangleup" |
"\\bigtriangledown" |
"\\nprec"  {yylval=wims_mathml_copy2(yytext, ";"); if (yylval != wims_mathml_empty_string) yylval[0]='&'; return MOL;}
"\\hrectangle" {yylval = wims_mathml_copy_string("&#x25AD;"); return MI;}
"\\vrectangle" {yylval = wims_mathml_copy_string("&#x25AF;"); return MI;}
"\\aleph" |
"\\beth" |
"\\eth" |
"\\ell" |
"\\hbar" |
"\\Im" |
"\\imath" |
"\\jmath" |
"\\wp" |
"\\Re" {yylval=wims_mathml_copy2(yytext, ";"); if (yylval != wims_mathml_empty_string) yylval[0]='&'; return MI;}

"\\Perp" |
"\\Vbar" {yylval=wims_mathml_copy_string("&Vbar;"); return MOL;}
"\\boxdot" {yylval=wims_mathml_copy_string("&sdotb;"); return MOL;}
"\\Box" |
"\\square" {yylval=wims_mathml_copy_string("&square;"); return MOL;}
"\\emptyset" |
"\\empty" {yylval=wims_mathml_copy_string("&emptyv;"); return MI;}
"\\exists" {yylval=wims_mathml_copy_string("&Exists;"); return MOL;}
"\\circ" {yylval=wims_mathml_copy_string("&SmallCircle;"); return MOL;}
"\\rhd" {yylval=wims_mathml_copy_string("&RightTriangle;"); return MOL;}
"\\lhd" {yylval=wims_mathml_copy_string("&LeftTriangle;"); return MOL;}
"\\lll" {yylval=wims_mathml_copy_string("&Ll;"); return MOL;}
"\\unrhd" {yylval=wims_mathml_copy_string("&RightTriangleEqual;"); return MOL;}
"\\unlhd" {yylval=wims_mathml_copy_string("&LeftTriangleEqual;"); return MOL;}
"\\Del" |
"\\nabla" {yylval=wims_mathml_copy_string("&Del;"); return MOL;}
"\\sphericalangle" {yylval=wims_mathml_copy_string("&angsph;"); return MOL;}

"\\heartsuit" {yylval=wims_mathml_copy_string("&#x2661;"); return MOL;}
"\\diamondsuit" {yylval=wims_mathml_copy_string("&#x2662;"); return MOL;}

"\\partial" {yylval=wims_mathml_copy_string("&PartialD;"); return MOL;}

"\\qed" {yylval=wims_mathml_copy_string("&squf;"); return MO;}

"\\mod" {yylval=wims_mathml_copy_string("mod"); return MOS;}
"\\bmod" {yylval=wims_mathml_copy_string("mod"); return MOS;}
"\\pmod" {return PMOD;}

"\\bottom" {yylval=wims_mathml_copy_string("&perp;"); return MOL;}

"\\neg" {yylval=wims_mathml_copy_string("&not;"); return MOL;}

"\\neq" |
"\\ne"  |
"\\not =" |
"\\not=" {yylval=wims_mathml_copy_string("&ne;"); return MOL;}

"\\not\\subset" {yylval=wims_mathml_copy_string("&#x2284;"); return MOL;}

"\\not\\in" |
"\\not \\in"  {yylval=wims_mathml_copy_string("&notin;"); return MOL;}

"\\shortmid" |
"\\mid"  {yylval=wims_mathml_copy_string("&VerticalBar;"); return MOS;}

"\\int"    |
"\\integral"     {return LARGERINT;}

"\\iint"   |
"\\doubleintegral" {yylval=wims_mathml_copy_string("&Int;"); return MOL;}

"\\iiint"   |
"\\tripleintegral" {yylval=wims_mathml_copy_string("&tint;"); return MOL;}

"\\iiiint"   |
"\\quadrupleintegral" {yylval=wims_mathml_copy_string("&qint;"); return MOL;}

"\\oint"    |
"\\conint"  |
"\\contourintegral"   {yylval=wims_mathml_copy_string("&conint;"); return MOL;}

"\\times"   {yylval=wims_mathml_copy_string("&times;"); return MOL;}
"\\star"   {yylval=wims_mathml_copy_string("&sstarf;"); return MOL;}
"\\circleddash" |
"\\odash"   {yylval=wims_mathml_copy_string("&odash;"); return MOL;}

"\\intercal"   {yylval=wims_mathml_copy_string("&intcal;"); return MOL;}
"\\smallfrown"   {yylval=wims_mathml_copy_string("&sfrown;"); return MOL;}
"\\smallsmile"   {yylval=wims_mathml_copy_string("&ssmile;"); return MOL;}

"\\boxminus" |
"\\minusb"   {yylval=wims_mathml_copy_string("&boxminus;"); return MOL;}
"\\boxplus" |
"\\plusb"   {yylval=wims_mathml_copy_string("&boxplus;"); return MOL;}
"\\boxtimes" |
"\\timesb"   {yylval=wims_mathml_copy_string("&boxtimes;"); return MOL;}

"\\sum"          {yylval=wims_mathml_copy_string("&Sum;"); return MOB;}

"\\prod" |
"\\product"      {yylval=wims_mathml_copy_string("&prod;"); return MOB;}

"\\coprod"  |
"\\coproduct"    {yylval=wims_mathml_copy_string("&coprod;"); return MOB;}

"\\otimes"       {yylval=wims_mathml_copy_string("&otimes;"); return MOL;}
"\\Otimes"       {yylval=wims_mathml_copy_string("&Otimes;"); return MOL;}
"\\bigotimes"     {yylval=wims_mathml_copy_string("&bigotimes;"); return MOL;}

"\\ominus"       {yylval=wims_mathml_copy_string("&ominus;"); return MOL;}
"\\oslash"       {yylval=wims_mathml_copy_string("&osol;"); return MOL;}

"\\oplus"       {yylval=wims_mathml_copy_string("&oplus;"); return MOL;}
"\\Oplus"   |
"\\bigoplus"    {yylval=wims_mathml_copy_string("&bigoplus;"); return MOB;}

"\\bigodot"    {yylval=wims_mathml_copy_string("&bigodot;"); return MOB;}
"\\bigsqcup"    {yylval=wims_mathml_copy_string("&bigsqcup;"); return MOB;}
"\\bigsqcap"    {yylval=wims_mathml_copy_string("&#x2A05;"); return MOB;}
"\\biginterleave" {yylval=wims_mathml_copy_string("&#x2AFC;"); return MOB;}
"\\biguplus"    {yylval=wims_mathml_copy_string("&biguplus;"); return MOB;}

"\\wedge"       {yylval=wims_mathml_copy_string("&wedge;"); return MOL;}
"\\Wedge"  |
"\\bigwedge"       {yylval=wims_mathml_copy_string("&xwedge;"); return MOB;}

"\\Vee"  |
"\\bigvee"       {yylval=wims_mathml_copy_string("&bigvee;"); return MOB;}

"\\invamp"  |
"\\parr"       {yylval=wims_mathml_copy_string("&#8523;"); return MOL;}

"\\frac"         {return FRAC;}
"\\tfrac"         {return TFRAC;}
"\\dfrac"         {return DFRAC;}
"\\sfrac"	{return SFRAC;}
"\\binom" 	{return BINOM;}
"\\choose"	{return BINOM2;}
"\\tbinom"        {return TBINOM;}

"\\tensor"       {return TENSOR;}

"\\multiscripts" {return MULTI;}

"\\overbrace"     {return OVERBRACE;}

"\\underbrace"   {return UNDERBRACE;}

"\\underline"   {return UNDERLINE;}

"\\bar"          {return BAR;}
"\\overline" |
"\\closure" |
"\\widebar"      {return WIDEBAR;}

"\\vec"  { return VEC;}
"\\widevec"   {return WIDEVEC;}
"\\limits"  {/* Ignore  */ ;}

"\\overrightarrow"	  {yylval = wims_mathml_copy_string("&rightarrow;"); 	 return OVERARROW;}
"\\overleftarrow"         {yylval = wims_mathml_copy_string("&leftarrow;"); 	 return OVERARROW;}
"\\overleftrightarrow" 	  {yylval = wims_mathml_copy_string("&leftrightarrow;"); return OVERARROW;}
"\\overrightharpoonup"    {yylval = wims_mathml_copy_string("&#x21C0;"); 	 return OVERARROW;}
"\\overrightharpoondown"  {yylval = wims_mathml_copy_string("&#x21C1;"); 	 return OVERARROW;}

"\\underrightarrow" 	  {yylval = wims_mathml_copy_string("&rightarrow;"); 	 return UNDERARROW;}
"\\underleftarrow" 	  {yylval = wims_mathml_copy_string("&leftarrow;"); 	 return UNDERARROW;}
"\\underleftrightarrow"   {yylval = wims_mathml_copy_string("&leftrightarrow;"); return UNDERARROW;}
"\\underrightharpoonup"   {yylval = wims_mathml_copy_string("&#x21C0;"); 	 return UNDERARROW;}
"\\underrightharpoondown" {yylval = wims_mathml_copy_string("&#x21C1;"); 	 return UNDERARROW;}

"\\dot"       {return DOT;}
"\\ddot"      {return DDOT;}
"\\dddot"      {return DDDOT;}
"\\ddddot"      {return DDDDOT;}
"\\P"  {yylval=wims_mathml_copy_string("&para;"); return MI;}
"\\copyright" {yylval=wims_mathml_copy_string("&#169;"); return MI;}/* copyright*/
"\\deg" {yylval=wims_mathml_copy_string("&#176;"); return MI;}/* not in wims latex: high small round o */
"\\Celcius" {yylval=wims_mathml_copy_string("&#8451;"); return MI;} /* not in wims latex: degree Celcius */
"\\Fahrenheit" {yylval=wims_mathml_copy_string("&#8457;"); return MI;} /* not in wims latex: degree Fahrenheit */
"\\i" {yylval=wims_mathml_copy_string("&#305;"); return MI;} /* dotless i*/
"\\j" {yylval=wims_mathml_copy_string("&#567;"); return MI;} /* dotless j*/
"\\"[~\^\"`'kl=bdruvtoOHct\.] { /* " accents , some are already in other syntax:  \\ddot{i} etc */
yylval = wims_mathml_copy_string(yytext+1);
if ((yyleng > 1) && (yylval != wims_mathml_empty_string)) yylval[yyleng+1]='\0';
return ACCENTS;
}
"\\tilde"       {return TILDE;}
"\\widetilde"   {return WIDETILDE;}


"\\check"       {return CHECK;}
"\\widecheck"   {return WIDECHECK;}


"\\hat"       {return HAT;}
"\\widehat"   {return WIDEHAT;}

"\\underset"     {return UNDER;}

"\\stackrel" |
"\\overset"      {return OVER;}
"\\brack"	{ wims_mathml_shortcut = 0; return BRACK;}
"\\brace"	{ wims_mathml_shortcut = 1; return BRACK;}
"\\over"         {return TEXOVER;}
"\\atop"         {return TEXATOP;}

"\\underoverset"  {return UNDEROVER;}

"\\sqrt"         {BEGIN(OPTARG); return SQRT;}

"\\root"         {return ROOT;}
"\\of"
"\\nonumber"		{yylval=wims_mathml_copy_string("&nbsp;"); return MI;}

"\\space"        {BEGIN(INT1); return SPACE;}
"\\*"      {yylval=wims_mathml_copy_string("&InvisibleTimes;"); return MI;}

"\\text" |
"\\textrm" |
"\\mbox"         {BEGIN(PLAINTEXT); return TEXTBOX;}

"\\statusline"    {BEGIN(PLAINTEXT); return STATLINE;}
"\\tooltip"       {BEGIN(PLAINTEXT); return TOOLTIP;}

"\\toggle"        {return TOGGLE;}

"\\mathraisebox"  {BEGIN(RAISEBOX1); return RAISEBOX;}

"\\fghilight" |
"\\fghighlight"   {BEGIN(ATTRIBUTELIST); return FGHIGHLIGHT;}

"\\bghilight" |
"\\bghighlight"   {BEGIN(ATTRIBUTELIST); return BGHIGHLIGHT;}

"\\special"	{BEGIN(FINDARG); return SPECIAL;}

"\\color"  |
"\\textcolor" {BEGIN(ATTRIBUTELIST); return COLOR;}
"\\bgcolor"        {BEGIN(ATTRIBUTELIST); return BGCOLOR;}

"\\displaystyle"  {wims_mathml_displaymode=1; wims_mathml_rowposn=2; return DISPLAY;}

"\\textstyle"     {wims_mathml_displaymode=0; wims_mathml_rowposn=2; return TEXTSTY;}

"\\textsize"     {return TEXTSIZE;}

"\\scriptsize"     {return SCSIZE;}

"\\scriptscriptsize"     {return SCSCSIZE;}

"\\mathit" |
"\\it" |
"\\emph"      {return ITALICS;}

"\\boldsymbol" |
"\\bf" |
"\\Bf" |
"\\mathbf"       {return BOLD;}
"\\mathrm"     |
"\\rm"  {BEGIN(ROMAN); return RM;}
"\\rm " { return WIMSROMAN;}

"\\mathbb"       {BEGIN(BBOLD); return BB;}

"\\mathfrak" |
"\\mathfr"       {BEGIN(FRAKTUR); return FRAK;}

"\\slash" |
"\\cancel"       {return SLASHED;}

"\\boxed"       {return BOXED;}

"\\mathcal"       {BEGIN(CALLIGRAPHIC); return CAL;}

"\\begin"        {wims_mathml_env_start = 1; BEGIN(MATHENV); return BEGINENV;}
"\\end"        {BEGIN(MATHENV); return ENDENV;}
"\\cases"	{wims_mathml_shortcut = 0;return SHORTARRAY;}
"\\Bmatrix"	{wims_mathml_shortcut = 1;return SHORTARRAY;}
"\\pmatrix"	{wims_mathml_shortcut = 2;return SHORTARRAY;}
"\\vmatrix"	{wims_mathml_shortcut = 3;return SHORTARRAY;}
"\\Vmatrix"	{wims_mathml_shortcut = 4;return SHORTARRAY;}
"\\aligned"	{wims_mathml_shortcut = 5;return SHORTARRAY;}


"\\substack"     {return SUBSTACK;}

"\\array"        {return ARRAY;}

"\\arrayopts"    {return ARRAYOPTS;}

"\\colalign"     {BEGIN(ATTRIBUTELIST); return COLALIGN;}

"\\collayout"   {BEGIN(ATTRIBUTELIST); return COLLAYOUT;}

"\\rowalign"     {BEGIN(ATTRIBUTELIST); return ROWALIGN;}

"\\align"     {BEGIN(ATTRIBUTELIST); return ALIGN;}

"\\equalrows"     {BEGIN(ATTRIBUTELIST); return EQROWS;}

"\\equalcols"     {BEGIN(ATTRIBUTELIST); return EQCOLS;}

"\\rowlines"     {BEGIN(ATTRIBUTELIST); return ROWLINES;}

"\\collines"     {BEGIN(ATTRIBUTELIST); return COLLINES;}

"\\frame"     {BEGIN(ATTRIBUTELIST); return FRAME;}

"\\padding"     {BEGIN(ATTRIBUTELIST); return PADDING;}

"\\rowopts"    {return ROWOPTS;}

"\\cellopts"   {return CELLOPTS;}

"\\rowspan"     {BEGIN(ATTRIBUTELIST); return ROWSPAN;}

"\\colspan"     {BEGIN(ATTRIBUTELIST); return COLSPAN;}


"\\," |
"\\thinspace"   {return THINSPACE;}

"\\:" |
"\\medspace"    {return MEDSPACE;}

"\\;" |
"\\thickspace"  {return THICKSPACE;}

"\\quad"        {return QUAD;}
"\\qquad"       {return QQUAD;}

"\\!" |
"\\negspace"    {return NEGSPACE;}

"\\phantom" |
"\\vphantom"     {return PHANTOM;}

"\\href"        {BEGIN(PLAINTEXT); return HREF;}

"\\operatorname"  {BEGIN(PLAINTEXT); return OPERATORNAME;}
"\\mathop"        {BEGIN(PLAINTEXT); return MATHOP;}
"\\mathbin"       {BEGIN(PLAINTEXT); return MATHBIN;}
"\\mathrel"       {BEGIN(PLAINTEXT); return MATHREL;}
"\\itexnum"       {BEGIN(PLAINTEXT); return ITEXNUM;}

"\\char32" {yylval = wims_mathml_copy_string("&#x2423;"); return MI;}
"\\char44" {yylval = wims_mathml_copy_string("&#44;"); return MI;}
"\\exclam" {yylval = wims_mathml_copy_string("&#x0021;"); return MI;}
"\\octothorpe" {yylval = wims_mathml_copy_string("&#x0023;"); return MI;}
"\\mathdollar" {yylval = wims_mathml_copy_string("&#x0024;"); return MI;}
"\\percent" {yylval = wims_mathml_copy_string("&#x0025;"); return MI;}
"\\ampersand" {yylval = wims_mathml_copy_string("&#x0026;"); return MI;}
"\\lparen" {yylval = wims_mathml_copy_string("&#x0028;"); return LEFTDELIM;}
"\\rparen" {yylval = wims_mathml_copy_string("&#x0029;"); return RIGHTDELIM;}
"\\plus" {yylval = wims_mathml_copy_string("&#x002B;"); return MO;}
"\\period" {yylval = wims_mathml_copy_string("&#x002E;"); return MOP;}
"\\mathslash" {yylval = wims_mathml_copy_string("&#x002F;"); return MO;}
"\\mathcolon" {yylval = wims_mathml_copy_string("&#x003A;"); return MOP;}
"\\semicolon" {yylval = wims_mathml_copy_string("&#x003B;"); return MOP;}
"\\less" {yylval = wims_mathml_copy_string("&#x003C;"); return MO;}
"\\equal" {yylval = wims_mathml_copy_string("&#x003D;"); return MO;}
"\\greater" {yylval = wims_mathml_copy_string("&#x003E;"); return MO;}
"\\question" {yylval = wims_mathml_copy_string("&#x003F;"); return MOP;}
"\\atsign" {yylval = wims_mathml_copy_string("&#x0040;"); return MI;}
"\\sterling" {yylval = wims_mathml_copy_string("&#x00A3;"); return MI;}
"\\yen" {yylval = wims_mathml_copy_string("&#x00A5;"); return MI;}
"\\cdotp" {yylval = wims_mathml_copy_string("&#x00B7;"); return MO;}
"\\matheth" {yylval = wims_mathml_copy_string("&#x00F0;"); return MI;}
"\\Zbar" {yylval = wims_mathml_copy_string("&#x01B5;"); return MI;}
"\\grave" {yylval = wims_mathml_copy_string("&#x0300;"); return MI;}
"\\acute" {yylval = wims_mathml_copy_string("&#x0301;"); return MI;}
"\\overbar" {yylval = wims_mathml_copy_string("&#x0305;"); return MI;}
"\\breve" {yylval = wims_mathml_copy_string("&#x0306;"); return MI;}
"\\ovhook" {yylval = wims_mathml_copy_string("&#x0309;"); return MI;}
"\\ocirc" {yylval = wims_mathml_copy_string("&#x030A;"); return MI;}
"\\candra" {yylval = wims_mathml_copy_string("&#x0310;"); return MI;}
"\\oturnedcomma" {yylval = wims_mathml_copy_string("&#x0312;"); return MI;}
"\\ocommatopright" {yylval = wims_mathml_copy_string("&#x0315;"); return MI;}
"\\droang" {yylval = wims_mathml_copy_string("&#x031A;"); return MI;}
"\\wideutilde" {yylval = wims_mathml_copy_string("&#x0330;"); return MI;}
"\\underbar" {yylval = wims_mathml_copy_string("&#x0331;"); return MI;}
"\\upAlpha" {yylval = wims_mathml_copy_string("&#x0391;"); return MI;}
"\\upBeta" {yylval = wims_mathml_copy_string("&#x0392;"); return MI;}
"\\upGamma" {yylval = wims_mathml_copy_string("&#x0393;"); return MI;}
"\\upDelta" {yylval = wims_mathml_copy_string("&#x0394;"); return MI;}
"\\upEpsilon" {yylval = wims_mathml_copy_string("&#x0395;"); return MI;}
"\\upZeta" {yylval = wims_mathml_copy_string("&#x0396;"); return MI;}
"\\upEta" {yylval = wims_mathml_copy_string("&#x0397;"); return MI;}
"\\upTheta" {yylval = wims_mathml_copy_string("&#x0398;"); return MI;}
"\\upIota" {yylval = wims_mathml_copy_string("&#x0399;"); return MI;}
"\\upKappa" {yylval = wims_mathml_copy_string("&#x039A;"); return MI;}
"\\upLambda" {yylval = wims_mathml_copy_string("&#x039B;"); return MI;}
"\\upMu" {yylval = wims_mathml_copy_string("&#x039C;"); return MI;}
"\\upNu" {yylval = wims_mathml_copy_string("&#x039D;"); return MI;}
"\\upXi" {yylval = wims_mathml_copy_string("&#x039E;"); return MI;}
"\\upOmicron" {yylval = wims_mathml_copy_string("&#x039F;"); return MI;}
"\\upPi" {yylval = wims_mathml_copy_string("&#x03A0;"); return MI;}
"\\upRho" {yylval = wims_mathml_copy_string("&#x03A1;"); return MI;}
"\\upSigma" {yylval = wims_mathml_copy_string("&#x03A3;"); return MI;}
"\\upTau" {yylval = wims_mathml_copy_string("&#x03A4;"); return MI;}
"\\upUpsilon" {yylval = wims_mathml_copy_string("&#x03A5;"); return MI;}
"\\upPhi" {yylval = wims_mathml_copy_string("&#x03A6;"); return MI;}
"\\upChi" {yylval = wims_mathml_copy_string("&#x03A7;"); return MI;}
"\\upPsi" {yylval = wims_mathml_copy_string("&#x03A8;"); return MI;}
"\\upOmega" {yylval = wims_mathml_copy_string("&#x03A9;"); return MI;}
"\\upalpha" {yylval = wims_mathml_copy_string("&#x03B1;"); return MI;}
"\\upbeta" {yylval = wims_mathml_copy_string("&#x03B2;"); return MI;}
"\\upgamma" {yylval = wims_mathml_copy_string("&#x03B3;"); return MI;}
"\\updelta" {yylval = wims_mathml_copy_string("&#x03B4;"); return MI;}
"\\upepsilon" {yylval = wims_mathml_copy_string("&#x03B5;"); return MI;}
"\\upzeta" {yylval = wims_mathml_copy_string("&#x03B6;"); return MI;}
"\\upeta" {yylval = wims_mathml_copy_string("&#x03B7;"); return MI;}
"\\uptheta" {yylval = wims_mathml_copy_string("&#x03B8;"); return MI;}
"\\upiota" {yylval = wims_mathml_copy_string("&#x03B9;"); return MI;}
"\\upkappa" {yylval = wims_mathml_copy_string("&#x03BA;"); return MI;}
"\\uplambda" {yylval = wims_mathml_copy_string("&#x03BB;"); return MI;}
"\\upmu" {yylval = wims_mathml_copy_string("&#x03BC;"); return MI;}
"\\upnu" {yylval = wims_mathml_copy_string("&#x03BD;"); return MI;}
"\\upxi" {yylval = wims_mathml_copy_string("&#x03BE;"); return MI;}
"\\upomicron" {yylval = wims_mathml_copy_string("&#x03BF;"); return MI;}
"\\uppi" {yylval = wims_mathml_copy_string("&#x03C0;"); return MI;}
"\\uprho" {yylval = wims_mathml_copy_string("&#x03C1;"); return MI;}
"\\upvarsigma" {yylval = wims_mathml_copy_string("&#x03C2;"); return MI;}
"\\upsigma" {yylval = wims_mathml_copy_string("&#x03C3;"); return MI;}
"\\uptau" {yylval = wims_mathml_copy_string("&#x03C4;"); return MI;}
"\\upupsilon" {yylval = wims_mathml_copy_string("&#x03C5;"); return MI;}
"\\upvarphi" {yylval = wims_mathml_copy_string("&#x03C6;"); return MI;}
"\\upchi" {yylval = wims_mathml_copy_string("&#x03C7;"); return MI;}
"\\uppsi" {yylval = wims_mathml_copy_string("&#x03C8;"); return MI;}
"\\upomega" {yylval = wims_mathml_copy_string("&#x03C9;"); return MI;}
"\\upvarbeta" {yylval = wims_mathml_copy_string("&#x03D0;"); return MI;}
"\\upvartheta" {yylval = wims_mathml_copy_string("&#x03D1;"); return MI;}
"\\upphi" {yylval = wims_mathml_copy_string("&#x03D5;"); return MI;}
"\\upvarpi" {yylval = wims_mathml_copy_string("&#x03D6;"); return MI;}
"\\upoldKoppa" {yylval = wims_mathml_copy_string("&#x03D8;"); return MI;}
"\\upoldkoppa" {yylval = wims_mathml_copy_string("&#x03D9;"); return MI;}
"\\upStigma" {yylval = wims_mathml_copy_string("&#x03DA;"); return MI;}
"\\upstigma" {yylval = wims_mathml_copy_string("&#x03DB;"); return MI;}
"\\upDigamma" {yylval = wims_mathml_copy_string("&#x03DC;"); return MI;}
"\\updigamma" {yylval = wims_mathml_copy_string("&#x03DD;"); return MI;}
"\\upKoppa" {yylval = wims_mathml_copy_string("&#x03DE;"); return MI;}
"\\upkoppa" {yylval = wims_mathml_copy_string("&#x03DF;"); return MI;}
"\\upSampi" {yylval = wims_mathml_copy_string("&#x03E0;"); return MI;}
"\\upsampi" {yylval = wims_mathml_copy_string("&#x03E1;"); return MI;}
"\\upvarkappa" {yylval = wims_mathml_copy_string("&#x03F0;"); return MI;}
"\\upvarrho" {yylval = wims_mathml_copy_string("&#x03F1;"); return MI;}
"\\upvarTheta" {yylval = wims_mathml_copy_string("&#x03F4;"); return MI;}
"\\upvarepsilon" {yylval = wims_mathml_copy_string("&#x03F5;"); return MI;}
"\\upbackepsilon" {yylval = wims_mathml_copy_string("&#x03F6;"); return MI;}
"\\horizbar" {yylval = wims_mathml_copy_string("&#x2015;"); return MI;}
"\\twolowline" {yylval = wims_mathml_copy_string("&#x2017;"); return MI;}
"\\enleadertwodots" {yylval = wims_mathml_copy_string("&#x2025;"); return MI;}
"\\unicodeellipsis" {yylval = wims_mathml_copy_string("&#x2026;"); return MI;}
"\\dprime" {yylval = wims_mathml_copy_string("&#x2033;"); return MI;}
"\\trprime" {yylval = wims_mathml_copy_string("&#x2034;"); return MI;}
"\\backdprime" {yylval = wims_mathml_copy_string("&#x2036;"); return MI;}
"\\backtrprime" {yylval = wims_mathml_copy_string("&#x2037;"); return MI;}
"\\caretinsert" {yylval = wims_mathml_copy_string("&#x2038;"); return MI;}
"\\Exclam" {yylval = wims_mathml_copy_string("&#x203C;"); return MI;}
"\\tieconcat" {yylval = wims_mathml_copy_string("&#x2040;"); return MO;}
"\\hyphenbullet" {yylval = wims_mathml_copy_string("&#x2043;"); return MI;}
"\\fracslash" {yylval = wims_mathml_copy_string("&#x2044;"); return MO;}
"\\Question" {yylval = wims_mathml_copy_string("&#x2047;"); return MI;}
"\\qprime" {yylval = wims_mathml_copy_string("&#x2057;"); return MI;}
"\\euro" |
"\\Euro" |
"\\EUR" {yylval = wims_mathml_copy_string("&#x20AC;"); return MI;}
"\\leftharpoonaccent" {yylval = wims_mathml_copy_string("&#x20D0;"); return MI;}
"\\rightharpoonaccent" {yylval = wims_mathml_copy_string("&#x20D1;"); return MI;}
"\\vertoverlay" {yylval = wims_mathml_copy_string("&#x20D2;"); return MI;}
"\\enclosecircle" {yylval = wims_mathml_copy_string("&#x20DD;"); return MI;}
"\\enclosesquare" {yylval = wims_mathml_copy_string("&#x20DE;"); return MI;}
"\\enclosediamond" {yylval = wims_mathml_copy_string("&#x20DF;"); return MI;}
"\\enclosetriangle" {yylval = wims_mathml_copy_string("&#x20E4;"); return MI;}
"\\annuity" {yylval = wims_mathml_copy_string("&#x20E7;"); return MI;}
"\\threeunderdot" {yylval = wims_mathml_copy_string("&#x20E8;"); return MI;}
"\\widebridgeabove" {yylval = wims_mathml_copy_string("&#x20E9;"); return MI;}
"\\asteraccent" {yylval = wims_mathml_copy_string("&#x20F0;"); return MI;}
"\\BbbC" {yylval = wims_mathml_copy_string("&#x2102;"); return MI;}
"\\Eulerconst" {yylval = wims_mathml_copy_string("&#x2107;"); return MI;}
"\\mscrg" {yylval = wims_mathml_copy_string("&#x210A;"); return MI;}
"\\mscrH" {yylval = wims_mathml_copy_string("&#x210B;"); return MI;}
"\\mfrakH" {yylval = wims_mathml_copy_string("&#x210C;"); return MI;}
"\\BbbH" {yylval = wims_mathml_copy_string("&#x210D;"); return MI;}
"\\Planckconst" {yylval = wims_mathml_copy_string("&#x210E;"); return MI;}
"\\hslash" {yylval = wims_mathml_copy_string("&#x210F;"); return MI;}
"\\mscrI" {yylval = wims_mathml_copy_string("&#x2110;"); return MI;}
"\\mscrL" {yylval = wims_mathml_copy_string("&#x2112;"); return MI;}
"\\BbbN" {yylval = wims_mathml_copy_string("&#x2115;"); return MI;}
"\\BbbP" {yylval = wims_mathml_copy_string("&#x2119;"); return MI;}
"\\BbbQ" {yylval = wims_mathml_copy_string("&#x211A;"); return MI;}
"\\mscrR" {yylval = wims_mathml_copy_string("&#x211B;"); return MI;}
"\\BbbR" {yylval = wims_mathml_copy_string("&#x211D;"); return MI;}
"\\BbbZ" {yylval = wims_mathml_copy_string("&#x2124;"); return MI;}
"\\mfrakZ" {yylval = wims_mathml_copy_string("&#x2128;"); return MI;}
"\\turnediota" {yylval = wims_mathml_copy_string("&#x2129;"); return MI;}
"\\Angstrom" {yylval = wims_mathml_copy_string("&#x212B;"); return MI;}
"\\mscrB" {yylval = wims_mathml_copy_string("&#x212C;"); return MI;}
"\\mfrakC" {yylval = wims_mathml_copy_string("&#x212D;"); return MI;}
"\\mscre" {yylval = wims_mathml_copy_string("&#x212F;"); return MI;}
"\\mscrE" {yylval = wims_mathml_copy_string("&#x2130;"); return MI;}
"\\mscrF" {yylval = wims_mathml_copy_string("&#x2131;"); return MI;}
"\\Finv" {yylval = wims_mathml_copy_string("&#x2132;"); return MI;}
"\\mscrM" {yylval = wims_mathml_copy_string("&#x2133;"); return MI;}
"\\mscro" {yylval = wims_mathml_copy_string("&#x2134;"); return MI;}
"\\gimel" {yylval = wims_mathml_copy_string("&#x2137;"); return MI;}
"\\daleth" {yylval = wims_mathml_copy_string("&#x2138;"); return MI;}
"\\Bbbpi" {yylval = wims_mathml_copy_string("&#x213C;"); return MI;}
"\\Bbbgamma" {yylval = wims_mathml_copy_string("&#x213D;"); return MI;}
"\\BbbGamma" {yylval = wims_mathml_copy_string("&#x213E;"); return MI;}
"\\BbbPi" {yylval = wims_mathml_copy_string("&#x213F;"); return MI;}
"\\Bbbsum" {yylval = wims_mathml_copy_string("&#x2140;"); return MI;}
"\\Game" {yylval = wims_mathml_copy_string("&#x2141;"); return MI;}
"\\sansLturned" {yylval = wims_mathml_copy_string("&#x2142;"); return MI;}
"\\sansLmirrored" {yylval = wims_mathml_copy_string("&#x2143;"); return MI;}
"\\Yup" {yylval = wims_mathml_copy_string("&#x2144;"); return MI;}
"\\mitBbbD" {yylval = wims_mathml_copy_string("&#x2145;"); return MI;}
"\\mitBbbd" {yylval = wims_mathml_copy_string("&#x2146;"); return MI;}
"\\mitBbbe" {yylval = wims_mathml_copy_string("&#x2147;"); return MI;}
"\\mitBbbi" {yylval = wims_mathml_copy_string("&#x2148;"); return MI;}
"\\mitBbbj" {yylval = wims_mathml_copy_string("&#x2149;"); return MI;}
"\\PropertyLine" {yylval = wims_mathml_copy_string("&#x214A;"); return MI;}
"\\upand" {yylval = wims_mathml_copy_string("&#x214B;"); return MI;}
"\\leftwavearrow" {yylval = wims_mathml_copy_string("&#x219C;"); return MO;}
"\\rightwavearrow" {yylval = wims_mathml_copy_string("&#x219D;"); return MO;}
"\\twoheaduparrow" {yylval = wims_mathml_copy_string("&#x219F;"); return MO;}
"\\twoheaddownarrow" {yylval = wims_mathml_copy_string("&#x21A1;"); return MO;}
"\\mapsfrom" {yylval = wims_mathml_copy_string("&#x21A4;"); return MO;}
"\\mapsup" {yylval = wims_mathml_copy_string("&#x21A5;"); return MO;}
"\\mapsdown" {yylval = wims_mathml_copy_string("&#x21A7;"); return MO;}
"\\updownarrowbar" {yylval = wims_mathml_copy_string("&#x21A8;"); return MO;}
"\\Ldsh" {yylval = wims_mathml_copy_string("&#x21B2;"); return MO;}
"\\Rdsh" {yylval = wims_mathml_copy_string("&#x21B3;"); return MO;}
"\\linefeed" {yylval = wims_mathml_copy_string("&#x21B4;"); return MI;}
"\\carriagereturn" {yylval = wims_mathml_copy_string("&#x21B5;"); return MI;}
"\\barovernorthwestarrow" {yylval = wims_mathml_copy_string("&#x21B8;"); return MI;}
"\\barleftarrowrightarrow" {yylval = wims_mathml_copy_string("&#x21B9;"); return MI;}
"\\acwopencirclearrow" {yylval = wims_mathml_copy_string("&#x21BA;"); return MO;}
"\\cwopencirclearrow" {yylval = wims_mathml_copy_string("&#x21BB;"); return MO;}
"\\leftharpoondown" {yylval = wims_mathml_copy_string("&#x21BD;"); return MO;}
"\\rightharpoonup" {yylval = wims_mathml_copy_string("&#x21C0;"); return MO;}
"\\rightharpoondown" {yylval = wims_mathml_copy_string("&#x21C1;"); return MO;}
"\\updownarrows" {yylval = wims_mathml_copy_string("&#x21C5;"); return MO;}
"\\Nwarrow" {yylval = wims_mathml_copy_string("&#x21D6;"); return MO;}
"\\Nearrow" {yylval = wims_mathml_copy_string("&#x21D7;"); return MO;}
"\\Searrow" {yylval = wims_mathml_copy_string("&#x21D8;"); return MO;}
"\\Swarrow" {yylval = wims_mathml_copy_string("&#x21D9;"); return MO;}
"\\nHuparrow" {yylval = wims_mathml_copy_string("&#x21DE;"); return MO;}
"\\nHdownarrow" {yylval = wims_mathml_copy_string("&#x21DF;"); return MO;}
"\\leftdasharrow" {yylval = wims_mathml_copy_string("&#x21E0;"); return MO;}
"\\updasharrow" {yylval = wims_mathml_copy_string("&#x21E1;"); return MO;}
"\\rightdasharrow" {yylval = wims_mathml_copy_string("&#x21E2;"); return MO;}
"\\downdasharrow" {yylval = wims_mathml_copy_string("&#x21E3;"); return MO;}
"\\barleftarrow" {yylval = wims_mathml_copy_string("&#x21E4;"); return MO;}
"\\rightarrowbar" {yylval = wims_mathml_copy_string("&#x21E5;"); return MO;}
"\\leftwhitearrow" {yylval = wims_mathml_copy_string("&#x21E6;"); return MO;}
"\\upwhitearrow" {yylval = wims_mathml_copy_string("&#x21E7;"); return MO;}
"\\rightwhitearrow" {yylval = wims_mathml_copy_string("&#x21E8;"); return MO;}
"\\downwhitearrow" {yylval = wims_mathml_copy_string("&#x21E9;"); return MO;}
"\\whitearrowupfrombar" {yylval = wims_mathml_copy_string("&#x21EA;"); return MI;}
"\\circleonrightarrow" {yylval = wims_mathml_copy_string("&#x21F4;"); return MO;}
"\\downuparrows" {yylval = wims_mathml_copy_string("&#x21F5;"); return MO;}
"\\rightthreearrows" {yylval = wims_mathml_copy_string("&#x21F6;"); return MO;}
"\\nvleftarrow" {yylval = wims_mathml_copy_string("&#x21F7;"); return MO;}
"\\nvrightarrow" {yylval = wims_mathml_copy_string("&#x21F8;"); return MO;}
"\\nvleftrightarrow" {yylval = wims_mathml_copy_string("&#x21F9;"); return MO;}
"\\nVleftarrow" {yylval = wims_mathml_copy_string("&#x21FA;"); return MO;}
"\\nVrightarrow" {yylval = wims_mathml_copy_string("&#x21FB;"); return MO;}
"\\nVleftrightarrow" {yylval = wims_mathml_copy_string("&#x21FC;"); return MO;}
"\\leftarrowtriangle" {yylval = wims_mathml_copy_string("&#x21FD;"); return MO;}
"\\rightarrowtriangle" {yylval = wims_mathml_copy_string("&#x21FE;"); return MO;}
"\\leftrightarrowtriangle" {yylval = wims_mathml_copy_string("&#x21FF;"); return MO;}
"\\complement" {yylval = wims_mathml_copy_string("&#x2201;"); return MO;}
"\\increment" {yylval = wims_mathml_copy_string("&#x2206;"); return MO;}
"\\smallin" {yylval = wims_mathml_copy_string("&#x220A;"); return MO;}
"\\nni" {yylval = wims_mathml_copy_string("&#x220C;"); return MO;}
"\\smallni" {yylval = wims_mathml_copy_string("&#x220D;"); return MO;}
"\\QED" {yylval = wims_mathml_copy_string("&#x220E;"); return MI;}
"\\minus" {yylval = wims_mathml_copy_string("&#x2212;"); return MO;}
"\\divslash" {yylval = wims_mathml_copy_string("&#x2215;"); return MO;}
"\\vysmwhtcircle" {yylval = wims_mathml_copy_string("&#x2218;"); return MO;}
"\\vysmblkcircle" {yylval = wims_mathml_copy_string("&#x2219;"); return MO;}
"\\cuberoot" {yylval = wims_mathml_copy_string("&#x221B;"); return MI;}
"\\rightangle" {yylval = wims_mathml_copy_string("&#x221F;"); return MI;}
"\\intclockwise" {yylval = wims_mathml_copy_string("&#x2231;"); return MI;}
"\\mathratio" {yylval = wims_mathml_copy_string("&#x2236;"); return MO;}
"\\Colon" {yylval = wims_mathml_copy_string("&#x2237;"); return MO;}
"\\dotminus" {yylval = wims_mathml_copy_string("&#x2238;"); return MO;}
"\\dashcolon" {yylval = wims_mathml_copy_string("&#x2239;"); return MO;}
"\\dotsminusdots" {yylval = wims_mathml_copy_string("&#x223A;"); return MO;}
"\\kernelcontraction" {yylval = wims_mathml_copy_string("&#x223B;"); return MO;}
"\\invlazys" {yylval = wims_mathml_copy_string("&#x223E;"); return MO;}
"\\sinewave" {yylval = wims_mathml_copy_string("&#x223F;"); return MI;}
"\\eqsim" {yylval = wims_mathml_copy_string("&#x2242;"); return MO;}
"\\nsime" {yylval = wims_mathml_copy_string("&#x2244;"); return MO;}
"\\simneqq" {yylval = wims_mathml_copy_string("&#x2246;"); return MO;}
"\\napprox" {yylval = wims_mathml_copy_string("&#x2249;"); return MO;}
"\\approxident" {yylval = wims_mathml_copy_string("&#x224B;"); return MO;}
"\\backcong" {yylval = wims_mathml_copy_string("&#x224C;"); return MO;}
"\\Doteq" {yylval = wims_mathml_copy_string("&#x2251;"); return MO;}
"\\arceq" {yylval = wims_mathml_copy_string("&#x2258;"); return MO;}
"\\wedgeq" {yylval = wims_mathml_copy_string("&#x2259;"); return MO;}
"\\veeeq" {yylval = wims_mathml_copy_string("&#x225A;"); return MO;}
"\\stareq" {yylval = wims_mathml_copy_string("&#x225B;"); return MO;}
"\\eqdef" {yylval = wims_mathml_copy_string("&#x225D;"); return MO;}
"\\measeq" {yylval = wims_mathml_copy_string("&#x225E;"); return MO;}
"\\questeq" {yylval = wims_mathml_copy_string("&#x225F;"); return MO;}
"\\Equiv" {yylval = wims_mathml_copy_string("&#x2263;"); return MO;}
"\\nlessgtr" {yylval = wims_mathml_copy_string("&#x2278;"); return MO;}
"\\ngtrless" {yylval = wims_mathml_copy_string("&#x2279;"); return MO;}
"\\cupleftarrow" {yylval = wims_mathml_copy_string("&#x228C;"); return MO;}
"\\cupdot" {yylval = wims_mathml_copy_string("&#x228D;"); return MO;}
"\\circledequal" {yylval = wims_mathml_copy_string("&#x229C;"); return MO;}
"\\assert" {yylval = wims_mathml_copy_string("&#x22A6;"); return MO;}
"\\nVdash" {yylval = wims_mathml_copy_string("&#x22AE;"); return MO;}
"\\prurel" {yylval = wims_mathml_copy_string("&#x22B0;"); return MO;}
"\\scurel" {yylval = wims_mathml_copy_string("&#x22B1;"); return MO;}
"\\origof" {yylval = wims_mathml_copy_string("&#x22B6;"); return MO;}
"\\imageof" {yylval = wims_mathml_copy_string("&#x22B7;"); return MO;}
"\\hermitmatrix" {yylval = wims_mathml_copy_string("&#x22B9;"); return MO;}
"\\barvee" {yylval = wims_mathml_copy_string("&#x22BD;"); return MO;}
"\\measuredrightangle" {yylval = wims_mathml_copy_string("&#x22BE;"); return MI;}
"\\varlrtriangle" {yylval = wims_mathml_copy_string("&#x22BF;"); return MI;}
"\\smwhtdiamond" {yylval = wims_mathml_copy_string("&#x22C4;"); return MO;}
"\\eqless" {yylval = wims_mathml_copy_string("&#x22DC;"); return MO;}
"\\eqgtr" {yylval = wims_mathml_copy_string("&#x22DD;"); return MO;}
"\\npreccurlyeq" {yylval = wims_mathml_copy_string("&#x22E0;"); return MO;}
"\\nsucccurlyeq" {yylval = wims_mathml_copy_string("&#x22E1;"); return MO;}
"\\nsqsubseteq" {yylval = wims_mathml_copy_string("&#x22E2;"); return MO;}
"\\nsqsupseteq" {yylval = wims_mathml_copy_string("&#x22E3;"); return MO;}
"\\sqsubsetneq" {yylval = wims_mathml_copy_string("&#x22E4;"); return MO;}
"\\sqsupsetneq" {yylval = wims_mathml_copy_string("&#x22E5;"); return MO;}
"\\unicodecdots" {yylval = wims_mathml_copy_string("&#x22EF;"); return MO;}
"\\adots" {yylval = wims_mathml_copy_string("&#x22F0;"); return MO;}
"\\disin" {yylval = wims_mathml_copy_string("&#x22F2;"); return MO;}
"\\longdivision" {yylval = wims_mathml_copy_string("&#x27CC;"); return MI;}
"\\leftouterjoin" {yylval = wims_mathml_copy_string("&#x27D5;"); return MI;}
"\\rightouterjoin" {yylval = wims_mathml_copy_string("&#x27D6;"); return MI;}
"\\fullouterjoin" {yylval = wims_mathml_copy_string("&#x27D7;"); return MI;}
"\\lBrack" {yylval = wims_mathml_copy_string("&#x27E6;"); return LEFTDELIM;}
"\\rBrack" {yylval = wims_mathml_copy_string("&#x27E7;"); return RIGHTDELIM;}
"\\lAngle" {yylval = wims_mathml_copy_string("&#x27EA;"); return LEFTDELIM;}
"\\rAngle" {yylval = wims_mathml_copy_string("&#x27EB;"); return RIGHTDELIM;}
"\\longrightsquigarrow" {yylval = wims_mathml_copy_string("&#x27FF;");return MOL;}
"\\nvtwoheadrightarrow" {yylval = wims_mathml_copy_string("&#x2900;");return MOL;}
"\\nVtwoheadrightarrow" {yylval = wims_mathml_copy_string("&#x2901;");return MOL;}
"\\nvLeftarrow" {yylval = wims_mathml_copy_string("&#x2902;");return MOL;}
"\\nvRightarrow" {yylval = wims_mathml_copy_string("&#x2903;");return MOL;}
"\\nvLeftrightarrow" {yylval = wims_mathml_copy_string("&#x2904;");return MOL;}
"\\twoheadmapsto" {yylval = wims_mathml_copy_string("&#x2905;");return MOL;}
"\\Mapsfrom" {yylval = wims_mathml_copy_string("&#x2906;");return MOL;}
"\\rightdotarrow" {yylval = wims_mathml_copy_string("&#x2911;");return MOL;}
"\\baruparrow" {yylval = wims_mathml_copy_string("&#x2912;");return MOL;}
"\\downarrowbar" {yylval = wims_mathml_copy_string("&#x2913;");return MOL;}
"\\nvrightarrowtail" {yylval = wims_mathml_copy_string("&#x2914;");return MOL;}
"\\nVrightarrowtail" {yylval = wims_mathml_copy_string("&#x2915;");return MOL;}
"\\twoheadrightarrowtail" {yylval = wims_mathml_copy_string("&#x2916;");return MOL;}
"\\nvtwoheadrightarrowtail" {yylval = wims_mathml_copy_string("&#x2917;");return MOL;}
"\\nVtwoheadrightarrowtail" {yylval = wims_mathml_copy_string("&#x2918;");return MOL;}
"\\lefttail" {yylval = wims_mathml_copy_string("&#x2919;");return MOL;}
"\\righttail" {yylval = wims_mathml_copy_string("&#x291A;");return MOL;}
"\\leftdbltail" {yylval = wims_mathml_copy_string("&#x291B;");return MOL;}
"\\rightdbltail" {yylval = wims_mathml_copy_string("&#x291C;");return MOL;}
"\\diamondleftarrow" {yylval = wims_mathml_copy_string("&#x291D;");return MOL;}
"\\rightarrowdiamond" {yylval = wims_mathml_copy_string("&#x291E;");return MOL;}
"\\diamondleftarrowbar" {yylval = wims_mathml_copy_string("&#x291F;");return MOL;}
"\\barrightarrowdiamond" {yylval = wims_mathml_copy_string("&#x2920;");return MOL;}
"\\nwsearrow" {yylval = wims_mathml_copy_string("&#x2921;");return MOL;}
"\\neswarrow" {yylval = wims_mathml_copy_string("&#x2922;");return MOL;}
"\\hknwarrow" {yylval = wims_mathml_copy_string("&#x2923;");return MOL;}
"\\hknearrow" {yylval = wims_mathml_copy_string("&#x2924;");return MOL;}
"\\hksearow" {yylval = wims_mathml_copy_string("&#x2925;");return MOL;}
"\\hkswarow" {yylval = wims_mathml_copy_string("&#x2926;");return MOL;}
"\\tona" {yylval = wims_mathml_copy_string("&#x2927;");return MOL;}
"\\toea" {yylval = wims_mathml_copy_string("&#x2928;");return MOL;}
"\\tosa" {yylval = wims_mathml_copy_string("&#x2929;");return MOL;}
"\\towa" {yylval = wims_mathml_copy_string("&#x292A;");return MOL;}
"\\rdiagovfdiag" {yylval = wims_mathml_copy_string("&#x292B;");return MOL;}
"\\fdiagovrdiag" {yylval = wims_mathml_copy_string("&#x292C;");return MOL;}
"\\seovnearrow" {yylval = wims_mathml_copy_string("&#x292D;");return MOL;}
"\\neovsearrow" {yylval = wims_mathml_copy_string("&#x292E;");return MOL;}
"\\fdiagovnearrow" {yylval = wims_mathml_copy_string("&#x292F;");return MOL;}
"\\rdiagovsearrow" {yylval = wims_mathml_copy_string("&#x2930;");return MOL;}
"\\neovnwarrow" {yylval = wims_mathml_copy_string("&#x2931;");return MOL;}
"\\nwovnearrow" {yylval = wims_mathml_copy_string("&#x2932;");return MOL;}
"\\rightcurvedarrow" {yylval = wims_mathml_copy_string("&#x2933;");return MOL;}
"\\uprightcurvearrow" {yylval = wims_mathml_copy_string("&#x2934;");return MOL;}
"\\downrightcurvedarrow" {yylval = wims_mathml_copy_string("&#x2935;");return MOL;}
"\\leftdowncurvedarrow" {yylval = wims_mathml_copy_string("&#x2936;");return MOL;}
"\\rightdowncurvedarrow" {yylval = wims_mathml_copy_string("&#x2937;");return MOL;}
"\\cwrightarcarrow" {yylval = wims_mathml_copy_string("&#x2938;");return MOL;}
"\\acwleftarcarrow" {yylval = wims_mathml_copy_string("&#x2939;");return MOL;}
"\\acwoverarcarrow" {yylval = wims_mathml_copy_string("&#x293A;");return MOL;}
"\\acwunderarcarrow" {yylval = wims_mathml_copy_string("&#x293B;");return MOL;}
"\\curvearrowrightminus" {yylval = wims_mathml_copy_string("&#x293C;");return MOL;}
"\\curvearrowleftplus" {yylval = wims_mathml_copy_string("&#x293D;");return MOL;}
"\\cwundercurvearrow" {yylval = wims_mathml_copy_string("&#x293E;");return MOL;}
"\\ccwundercurvearrow" {yylval = wims_mathml_copy_string("&#x293F;");return MOL;}
"\\acwcirclearrow" {yylval = wims_mathml_copy_string("&#x2940;");return MOL;}
"\\cwcirclearrow" {yylval = wims_mathml_copy_string("&#x2941;");return MOL;}
"\\rightarrowshortleftarrow" {yylval = wims_mathml_copy_string("&#x2942;");return MOL;}
"\\leftarrowshortrightarrow" {yylval = wims_mathml_copy_string("&#x2943;");return MOL;}
"\\shortrightarrowleftarrow" {yylval = wims_mathml_copy_string("&#x2944;");return MOL;}
"\\rightarrowplus" {yylval = wims_mathml_copy_string("&#x2945;");return MOL;}
"\\leftarrowplus" {yylval = wims_mathml_copy_string("&#x2946;");return MOL;}
"\\rightarrowx" {yylval = wims_mathml_copy_string("&#x2947;");return MOL;}
"\\leftrightarrowcircle" {yylval = wims_mathml_copy_string("&#x2948;");return MOL;}
"\\twoheaduparrowcircle" {yylval = wims_mathml_copy_string("&#x2949;");return MOL;}
"\\leftrightharpoonupdown" {yylval = wims_mathml_copy_string("&#x294A;");return MOL;}
"\\leftrightharpoondownup" {yylval = wims_mathml_copy_string("&#x294B;");return MOL;}
"\\updownharpoonrightleft" {yylval = wims_mathml_copy_string("&#x294C;");return MOL;}
"\\updownharpoonleftright" {yylval = wims_mathml_copy_string("&#x294D;");return MOL;}
"\\leftrightharpoonupup" {yylval = wims_mathml_copy_string("&#x294E;");return MOL;}
"\\updownharpoonrightright" {yylval = wims_mathml_copy_string("&#x294F;");return MOL;}
"\\leftrightharpoondowndown" {yylval = wims_mathml_copy_string("&#x2950;");return MOL;}
"\\updownharpoonleftleft" {yylval = wims_mathml_copy_string("&#x2951;");return MOL;}
"\\barleftharpoonup" {yylval = wims_mathml_copy_string("&#x2952;");return MOL;}
"\\rightharpoonupbar" {yylval = wims_mathml_copy_string("&#x2953;");return MOL;}
"\\barupharpoonright" {yylval = wims_mathml_copy_string("&#x2954;");return MOL;}
"\\lParen" {yylval = wims_mathml_copy_string("&#x2985;"); return LEFTDELIM;}
"\\rParen" {yylval = wims_mathml_copy_string("&#x2986;"); return RIGHTDELIM;}
"\\llparenthesis" {yylval = wims_mathml_copy_string("&#x2987;"); return LEFTDELIM;}
"\\rrparenthesis" {yylval = wims_mathml_copy_string("&#x2988;"); return RIGHTDELIM;}
"\\llangle" {yylval = wims_mathml_copy_string("&#x2989;"); return LEFTDELIM;}
"\\rrangle" {yylval = wims_mathml_copy_string("&#x298A;"); return RIGHTDELIM;}
"\\lbrackubar" {yylval = wims_mathml_copy_string("&#x298B;"); return LEFTDELIM;}
"\\rbrackubar" {yylval = wims_mathml_copy_string("&#x298C;"); return RIGHTDELIM;}
"\\lbrackultick" {yylval = wims_mathml_copy_string("&#x298D;"); return LEFTDELIM;}
"\\rbracklrtick" {yylval = wims_mathml_copy_string("&#x298E;"); return RIGHTDELIM;}
"\\lbracklltick" {yylval = wims_mathml_copy_string("&#x298F;"); return LEFTDELIM;}
"\\rbrackurtick" {yylval = wims_mathml_copy_string("&#x2990;"); return RIGHTDELIM;}
"\\langledot" {yylval = wims_mathml_copy_string("&#x2991;"); return LEFTDELIM;}
"\\rangledot" {yylval = wims_mathml_copy_string("&#x2992;"); return RIGHTDELIM;}
"\\lparenless" {yylval = wims_mathml_copy_string("&#x2993;"); return LEFTDELIM;}
"\\rparengtr" {yylval = wims_mathml_copy_string("&#x2994;"); return RIGHTDELIM;}
"\\Lparengtr" {yylval = wims_mathml_copy_string("&#x2995;"); return LEFTDELIM;}
"\\Rparenless" {yylval = wims_mathml_copy_string("&#x2996;"); return RIGHTDELIM;}
"\\lblkbrbrak" {yylval = wims_mathml_copy_string("&#x2997;"); return LEFTDELIM;}
"\\rblkbrbrak" {yylval = wims_mathml_copy_string("&#x2998;"); return RIGHTDELIM;}
"\\fourvdots" {yylval = wims_mathml_copy_string("&#x2999;"); return OTHERDELIM;}
"\\vzigzag" {yylval = wims_mathml_copy_string("&#x299A;"); return OTHERDELIM;}
"\\measuredangleleft" {yylval = wims_mathml_copy_string("&#x299B;"); return MI;}
"\\rightanglesqr" {yylval = wims_mathml_copy_string("&#x299C;"); return MI;}
"\\rightanglemdot" {yylval = wims_mathml_copy_string("&#x299D;"); return MI;}
"\\angles" {yylval = wims_mathml_copy_string("&#x299E;"); return MI;}
"\\angdnr" {yylval = wims_mathml_copy_string("&#x299F;"); return MI;}
"\\gtlpar" {yylval = wims_mathml_copy_string("&#x29A0;"); return MI;}
"\\sphericalangleup" {yylval = wims_mathml_copy_string("&#x29A1;"); return MI;}
"\\turnangle" {yylval = wims_mathml_copy_string("&#x29A2;"); return MI;}
"\\revangle" {yylval = wims_mathml_copy_string("&#x29A3;"); return MI;}
"\\angleubar" {yylval = wims_mathml_copy_string("&#x29A4;"); return MI;}
"\\revangleubar" {yylval = wims_mathml_copy_string("&#x29A5;"); return MI;}
"\\wideangledown" {yylval = wims_mathml_copy_string("&#x29A6;"); return MI;}
"\\wideangleup" {yylval = wims_mathml_copy_string("&#x29A7;"); return MI;}
"\\measanglerutone" {yylval = wims_mathml_copy_string("&#x29A8;"); return MI;}
"\\measanglelutonw" {yylval = wims_mathml_copy_string("&#x29A9;"); return MI;}
"\\measanglerdtose" {yylval = wims_mathml_copy_string("&#x29AA;"); return MI;}
"\\measangleldtosw" {yylval = wims_mathml_copy_string("&#x29AB;"); return MI;}
"\\measangleurtone" {yylval = wims_mathml_copy_string("&#x29AC;"); return MI;}
"\\measangleultonw" {yylval = wims_mathml_copy_string("&#x29AD;"); return MI;}
"\\measangledrtose" {yylval = wims_mathml_copy_string("&#x29AE;"); return MI;}
"\\measangledltosw" {yylval = wims_mathml_copy_string("&#x29AF;"); return MI;}
"\\revemptyset" {yylval = wims_mathml_copy_string("&#x29B0;"); return MI;}
"\\emptysetobar" {yylval = wims_mathml_copy_string("&#x29B1;"); return MI;}
"\\emptysetocirc" {yylval = wims_mathml_copy_string("&#x29B2;"); return MI;}
"\\emptysetoarr" {yylval = wims_mathml_copy_string("&#x29B3;"); return MI;}
"\\emptysetoarrl" {yylval = wims_mathml_copy_string("&#x29B4;"); return MI;}
"\\circlehbar" {yylval = wims_mathml_copy_string("&#x29B5;"); return MI;}
"\\circledvert" {yylval = wims_mathml_copy_string("&#x29B6;"); return MO;}
"\\circledparallel" {yylval = wims_mathml_copy_string("&#x29B7;"); return MO;}
"\\obslash" {yylval = wims_mathml_copy_string("&#x29B8;"); return MO;}
"\\operp" {yylval = wims_mathml_copy_string("&#x29B9;"); return MO;}
"\\obot" {yylval = wims_mathml_copy_string("&#x29BA;"); return MI;}
"\\olcross" {yylval = wims_mathml_copy_string("&#x29BB;"); return MI;}
"\\odotslashdot" {yylval = wims_mathml_copy_string("&#x29BC;"); return MI;}
"\\uparrowoncircle" {yylval = wims_mathml_copy_string("&#x29BD;"); return MI;}
"\\circledwhitebullet" {yylval = wims_mathml_copy_string("&#x29BE;"); return MI;}
"\\circledbullet" {yylval = wims_mathml_copy_string("&#x29BF;"); return MI;}
"\\olessthan" {yylval = wims_mathml_copy_string("&#x29C0;"); return MO;}
"\\ogreaterthan" {yylval = wims_mathml_copy_string("&#x29C1;"); return MO;}
"\\cirscir" {yylval = wims_mathml_copy_string("&#x29C2;"); return MI;}
"\\cirE" {yylval = wims_mathml_copy_string("&#x29C3;"); return MI;}
"\\boxdiag" {yylval = wims_mathml_copy_string("&#x29C4;"); return MO;}
"\\boxbslash" {yylval = wims_mathml_copy_string("&#x29C5;"); return MO;}
"\\boxast" {yylval = wims_mathml_copy_string("&#x29C6;"); return MO;}
"\\boxcircle" {yylval = wims_mathml_copy_string("&#x29C7;"); return MO;}
"\\boxbox" {yylval = wims_mathml_copy_string("&#x29C8;"); return MO;}
"\\boxonbox" {yylval = wims_mathml_copy_string("&#x29C9;"); return MI;}
"\\triangleodot" {yylval = wims_mathml_copy_string("&#x29CA;"); return MI;}
"\\triangleubar" {yylval = wims_mathml_copy_string("&#x29CB;"); return MI;}
"\\triangles" {yylval = wims_mathml_copy_string("&#x29CC;"); return MI;}
"\\triangleserifs" {yylval = wims_mathml_copy_string("&#x29CD;"); return MI;}
"\\rtriltri" {yylval = wims_mathml_copy_string("&#x29CE;"); return MOL;}
"\\ltrivb" {yylval = wims_mathml_copy_string("&#x29CF;"); return MOL;}
"\\vbrtri" {yylval = wims_mathml_copy_string("&#x29D0;"); return MOL;}
"\\lfbowtie" {yylval = wims_mathml_copy_string("&#x29D1;"); return MOL;}
"\\rfbowtie" {yylval = wims_mathml_copy_string("&#x29D2;"); return MOL;}
"\\fbowtie" {yylval = wims_mathml_copy_string("&#x29D3;"); return MOL;}
"\\lftimes" {yylval = wims_mathml_copy_string("&#x29D4;"); return MOL;}
"\\rftimes" {yylval = wims_mathml_copy_string("&#x29D5;"); return MOL;}
"\\hourglass" {yylval = wims_mathml_copy_string("&#x29D6;"); return MO;}
"\\blackhourglass" {yylval = wims_mathml_copy_string("&#x29D7;"); return MO;}
"\\lvzigzag" {yylval = wims_mathml_copy_string("&#x29D8;"); return LEFTDELIM;}
"\\rvzigzag" {yylval = wims_mathml_copy_string("&#x29D9;"); return RIGHTDELIM;}
"\\Lvzigzag" {yylval = wims_mathml_copy_string("&#x29DA;"); return LEFTDELIM;}
"\\Rvzigzag" {yylval = wims_mathml_copy_string("&#x29DB;"); return RIGHTDELIM;}
"\\iinfin" {yylval = wims_mathml_copy_string("&#x29DC;"); return MI;}
"\\tieinfty" {yylval = wims_mathml_copy_string("&#x29DD;"); return MI;}
"\\nvinfty" {yylval = wims_mathml_copy_string("&#x29DE;"); return MI;}
"\\dualmap" {yylval = wims_mathml_copy_string("&#x29DF;"); return MOL;}
"\\laplac" {yylval = wims_mathml_copy_string("&#x29E0;"); return MI;}
"\\lrtriangleeq" {yylval = wims_mathml_copy_string("&#x29E1;"); return MOL;}
"\\shuffle" {yylval = wims_mathml_copy_string("&#x29E2;"); return MO;}
"\\eparsl" {yylval = wims_mathml_copy_string("&#x29E3;"); return MOL;}
"\\smeparsl" {yylval = wims_mathml_copy_string("&#x29E4;"); return MOL;}
"\\eqvparsl" {yylval = wims_mathml_copy_string("&#x29E5;"); return MOL;}
"\\gleichstark" {yylval = wims_mathml_copy_string("&#x29E6;"); return MOL;}
"\\thermod" {yylval = wims_mathml_copy_string("&#x29E7;"); return MI;}
"\\downtriangleleftblack" {yylval = wims_mathml_copy_string("&#x29E8;"); return MI;}
"\\downtrianglerightblack" {yylval = wims_mathml_copy_string("&#x29E9;"); return MI;}
"\\blackdiamonddownarrow" {yylval = wims_mathml_copy_string("&#x29EA;"); return MI;}
"\\mdlgblklozenge" {yylval = wims_mathml_copy_string("&#x29EB;"); return MO;}
"\\circledownarrow" {yylval = wims_mathml_copy_string("&#x29EC;"); return MI;}
"\\blackcircledownarrow" {yylval = wims_mathml_copy_string("&#x29ED;"); return MI;}
"\\errbarsquare" {yylval = wims_mathml_copy_string("&#x29EE;"); return MI;}
"\\errbarblacksquare" {yylval = wims_mathml_copy_string("&#x29EF;"); return MI;}
"\\errbardiamond" {yylval = wims_mathml_copy_string("&#x29F0;"); return MI;}
"\\errbarblackdiamond" {yylval = wims_mathml_copy_string("&#x29F1;"); return MI;}
"\\errbarcircle" {yylval = wims_mathml_copy_string("&#x29F2;"); return MI;}
"\\errbarblackcircle" {yylval = wims_mathml_copy_string("&#x29F3;"); return MI;}
"\\ruledelayed" {yylval = wims_mathml_copy_string("&#x29F4;"); return MOL;}
"\\dsol" {yylval = wims_mathml_copy_string("&#x29F6;"); return MO;}
"\\rsolbar" {yylval = wims_mathml_copy_string("&#x29F7;"); return MO;}
"\\xsol" {yylval = wims_mathml_copy_string("&#x29F8;"); return MI;}
"\\xbsol" {yylval = wims_mathml_copy_string("&#x29F9;"); return MI;}
"\\handleft" {yylval=wims_mathml_copy_string("&#x261C;"); return MI;}
"\\handright" {yylval=wims_mathml_copy_string("&#x261E;"); return MI;}
"\\handup" {yylval=wims_mathml_copy_string("&#x261D;"); return MI;}
"\\handdown" {yylval=wims_mathml_copy_string("&#x261F;"); return MI;}
"\\OK" {yylval=wims_mathml_copy_string("&#x2611;"); return MI;}
"\\NOK" {yylval=wims_mathml_copy_string("&#x2612;"); return MI;}
"\\poison" {yylval=wims_mathml_copy_string("&#x2620;"); return MI;}
"\\blitz" {yylval=wims_mathml_copy_string("&#x26A1;"); return MI;}
"\\attention" {yylval=wims_mathml_copy_string("&#x26A0;"); return MI;}
"\\nsmile" {yylval=wims_mathml_copy_string("&#x2639;"); return MI;}
"\\vvvert" {yylval=wims_mathml_copy_string("&#x2AFC;"); return OTHERDELIM;}
"\\ointclockwise" |
"\\varointclockwise" {yylval=wims_mathml_copy_string("&#x2A10;"); return MO;}
"\\ointctrclockwise" |
"\\varointctrclockwise" {yylval=wims_mathml_copy_string("&#x2A11;"); return MO;}
"\\sqint" {yylval=wims_mathml_copy_string("&#x2A16;"); return MO;}
"\\fint" {yylval=wims_mathml_copy_string("&#x2AOF;"); return MO;}
"\\diagup"	{yylval=wims_mathml_copy_string("&#x2571;"); return OTHERDELIM;}
"\\diagdown"	{yylval=wims_mathml_copy_string("&#x2572;"); return OTHERDELIM;}
"\\lmoustache" {yylval = wims_mathml_copy_string("&#x23B0;"); return OTHERDELIM;}
"\\rmoustache" {yylval = wims_mathml_copy_string("&#x23B1;"); return OTHERDELIM;}
"\\bigint"    |
"\\bigintegral"     {return BIGINT;}
"\\biggint"    |
"\\biggintegral"     {return BIGGINT;}
"\\bigggint"   |
"\\bigggintegral"    {return BIGGGINT;}
"\\input{"\ {0,5}[0-9]{1,3}\ {0,5}"}{"[0-9]{1,3}\ {0,5}"}{"\ {0,5}[a-z0-9#\ :\-;.*?]{0,250}\ {0,5}"}{"\ {0,5}[a-zA-Z0-9#\ :\-\_\+\=\(\)\[\]\@\~\!\#\$\%\^\&\|\/\,;.*\^\?]{0,250}\ {0,5}"}{"\ {0,3}[0-1]\ {0,3}"}" {yylval=wims_mathml_copy_string(yytext); return INPUT;}
"\\lewis" {yylval=wims_mathml_copy_string(yytext); return LEWIS;}
"\\\\ \\\\"          {wims_mathml_rowposn=2; yylval="\\\\ \\: \\\\";return ROWSEP;}
"\\\\"          {wims_mathml_rowposn=2;return ROWSEP;}
"\\tabularnewline"       {wims_mathml_rowposn=2; return ROWSEP;}
"\\hline"          {yylval=wims_mathml_copy_string(" ");; return MOP; }
"\\cr"          {wims_mathml_rowposn=2; return ROWSEP;}
"\\ "         {return THICKSPACE;}
"\\lbrack"             {yylval=wims_mathml_copy_string("["); return OTHERDELIM;}
"\\rbrack"             {yylval=wims_mathml_copy_string("]"); return OTHERDELIM;}
"\\RR" {yylval=wims_mathml_copy_string("&Ropf;"); return MI;}
"\\QQ" {yylval=wims_mathml_copy_string("&Qopf;"); return MI;}
"\\CC" {yylval=wims_mathml_copy_string("&Copf;"); return MI;}
"\\ZZ" {yylval=wims_mathml_copy_string("&Zopf;"); return MI;}
"\\NN" {yylval=wims_mathml_copy_string("&Nopf;"); return MI;}
"\\size["[0-9]{1,4}"]" {yylval=wims_mathml_copy_string(yytext+6);int len=strlen(yylval);if (yylval != wims_mathml_empty_string) { yylval[len-1]='%'; wims_mathml_env_start = 1; return TMP_FONTSIZE;}}
"\\large" { yylval=wims_mathml_copy_string("120%");wims_mathml_env_start = 1; return TMP_FONTSIZE;}
"\\Large" { yylval=wims_mathml_copy_string("140%");wims_mathml_env_start = 1; return TMP_FONTSIZE;}
"\\huge"  { yylval=wims_mathml_copy_string("200%");wims_mathml_env_start = 1; return TMP_FONTSIZE;}
"\\Huge"  { yylval=wims_mathml_copy_string("250%");wims_mathml_env_start = 1; return TMP_FONTSIZE;}
"\\cline{"\ {0,5}[0-9]{1,3}\ {0,5}-\ {0,5}[0-9]{1,3}\ {0,5}"}"
"\\'(" {yylval=wims_mathml_copy_string("&grave;("); return MI;}
"\\includegraphics"(\[[^\]]*\])?\{[^}]*\};

"\\"[a-zA-Z]+    {yylval=wims_mathml_copy_string(yytext+1); return MOP;}

"\&"[a-zA-Z_][a-zA-Z0-9_]*";" |
"\&#"[0-9]+";" |
"\&#x"[a-fA-F0-9]+";"  {yylval=wims_mathml_copy_string(yytext); return MI;}

"\n"             wims_mathml_lineno++;
[\r\t ]+           ;


.              {return UNKNOWNCHAR;}
}

<ATTRIBUTELIST>"{"[ \t\n\r #a-zA-Z0-9.\-]*"}"  {BEGIN(MATH); yylval=wims_mathml_copy_string(yytext); if (yylval != wims_mathml_empty_string) { yylval[0]='"'; yylval[yyleng-1]='"'; } return ATTRLIST;}


<FINDARG>"{color="*[a-zA-Z]*"}"  {BEGIN(MATH); yylval=wims_mathml_copy_string(yytext+6);int len=strlen(yylval);if (yylval != wims_mathml_empty_string) { yylval[0]='"'; yylval[len-1]='"'; } return ATTRLIST;}


<OPTARG>{
"[" {if(wims_mathml_optarg_ind < wims_mathml_optarg_depth - 1 && wims_mathml_inoptarg[wims_mathml_optarg_ind] !=1 ){wims_mathml_optarg_ind++; wims_mathml_inoptarg[wims_mathml_optarg_ind]=1;}; BEGIN(MATH); return OPTARGOPEN;}
[\n\r\t ]+    ;
.   {BEGIN(MATH); unput(yytext[0]);}
}

<RAISEBOX1>{
\{[\n\r\t ]*- {BEGIN(RAISEBOX2); return NEG;}
\{[\n\r\t ]* {BEGIN(RAISEBOX2);}
}

<RAISEBOX2>{
[^\}]*?\}  {yylval=wims_mathml_copy_string(yytext); if ((yyleng > 1) && (yylval != wims_mathml_empty_string)) yylval[yyleng-1]='\0'; BEGIN(TEXTOPTARG1); return TEXTSTRING;}
}


<TEXTOPTARG1>{
\[[^\]]*?\] {yylval=wims_mathml_copy_string(yytext+1); if ((yyleng > 1) && (yylval != wims_mathml_empty_string)) yylval[yyleng-2]='\0'; BEGIN(TEXTOPTARG); return TEXTSTRING;}
. {BEGIN(MATH); unput(yytext[0]);}
}

<TEXTOPTARG>{
\[[^\]]*?\] {yylval=wims_mathml_copy_string(yytext+1); if ((yyleng > 1) && (yylval != wims_mathml_empty_string)) yylval[yyleng-2]='\0'; BEGIN(MATH); return TEXTSTRING;}
. {BEGIN(MATH); unput(yytext[0]);}
}

<MATHENV>{
"\{matrix\}" {wims_mathml_env_start = 0; BEGIN(MATH); return MATRIX;}
"\{pmatrix\}" {wims_mathml_env_start = 0; BEGIN(MATH); return PMATRIX;}
"\{bmatrix\}" {wims_mathml_env_start = 0; BEGIN(MATH); return BMATRIX;}
"\{Bmatrix\}" {wims_mathml_env_start = 0; BEGIN(MATH); return BBMATRIX;}
"\{vmatrix\}" {wims_mathml_env_start = 0; BEGIN(MATH); return VMATRIX;}
"\{Vmatrix\}" {wims_mathml_env_start = 0; BEGIN(MATH); return VVMATRIX;}
"\{smallmatrix\}" {wims_mathml_env_start = 0; BEGIN(MATH); return SMALLMATRIX;}
"\{cases\}" |
"\{dcases\}" {wims_mathml_env_start = 0; BEGIN(MATH); return CASES;}
"\{align*\}" |
"\{align\}" |
"\{aligned\}" {wims_mathml_env_start = 0; BEGIN(MATH); return ALIGNED;}
"\{gathered\}" {wims_mathml_env_start = 0; BEGIN(MATH); return GATHERED;}
"\{split\}" {wims_mathml_env_start = 0; BEGIN(MATH); return ALIGNED;}
"\{array\}" |
"\{alignat\}" {if (wims_mathml_env_start == 1) BEGIN(ARRAYOPTIONS); else BEGIN(MATH); return ARRAY;}
"\{svg\}" {wims_mathml_env_start = 0; BEGIN(SVGENV); return SVG;}
"\{html\}" {wims_mathml_env_start = 0; BEGIN(HTMLENV); return HTML;}
"\{equation\}"  {wims_mathml_env_start = 0; BEGIN(MATH); return EQUATION;}
"\{eqnarray\}" {wims_mathml_env_start = 0; BEGIN(MATH); return MATRIX;}

}

<ARRAYOPTIONS>{
"[t]" {yylval=wims_mathml_copy_string("axis 1"); return ARRAYALIGN;}
"[c]" {yylval=wims_mathml_copy_string("center"); return ARRAYALIGN;}
"[b]" {yylval=wims_mathml_copy_string("axis -1"); return ARRAYALIGN;}
"\{"  {BEGIN(COLOPTS); return ST;}
[\t\n\r ]+       ;
}

<COLOPTS>{
"\}"  {wims_mathml_env_start = 0; BEGIN(MATH); return END;}
[0-99rlc|.:_\-\ ]{0,99} {
    wims_mathml_env_start=0;
    int p;
    // yyleng is length of last matched token eg "{ c |ccc|c }
    // 8 is max length of tag " center " ; 14 is length " columnalign=\"" 14+1 '\0'
    // {----c|c|c}
    int len = yyleng*8+15;
    char C[len];
    char M[len];
    char H[len];
    memset(C,'\0',len);// fill memory with null
    memset(M,'\0',len);
    memset(H,'\0',len);
    strncat(C," columnalign=\"",14);
    strncat(M," columnlines=\"",14);
    strncat(H," rowlines=\"",11);
    /* {c|c|c|c---}*/
    for( p = 0; p < yyleng ; p++){
	if( strlen(C) < (len - 8) && strlen(M) < (len - 8)){
	/* in case of trouble...array M[len] or C[len] will be too small */
	    switch( yytext[p] ){
	        case 'c' : strncat(C," center ",8);break;
		case 'r' : strncat(C," right ",7);break;
	        case 'l' : strncat(C," left ",6); break;
		case '|' : strncat(M," solid ",7);break;
	        case ':' : strncat(M," dashed ",8);break;
	        case '-' : strncat(H," solid ",7);break;
	        case '_' : strncat(H," dashed ",8);break;
	        case '.' : strncat(H," none ",6);break;
	        case ' ' : break;
		default  : strncat(M," none ",6);strncat(H," none ",6);break;
	    }
	}
    }
     /* 31/5/2017 bug in MathJax :  rowlines="" will show rowlines */
    if( strlen(M) < 16 ){  strncat(M," none ",6); }
    if( strlen(H) < 16 ){  strncat(H," none ",6); }
    strncat(C,"\" ",2);
    strncat(M,"\" ",2);
    strncat(H,"\" ",2);

    yylval=wims_mathml_copy3(C,M,H);
    return TEX_OPTIONS;
  }
}

<SVGENV>{
"\\end\{svg" {
    yylval = wims_mathml_copy_string(yytext);
    replace_str(yylval,"\\lt ","<");
    replace_str(yylval,"\\gt ",">");
    yyleng = strlen(yylval);
    yylval[yyleng-8]=0; BEGIN(SVGEND); return XMLSTRING;}
(.|\n) {yymore();}
}

<SVGEND>{
"\}" {BEGIN(MATH); return ENDSVG;}
}

<HTMLENV>{
"\\end\{html" {
    yylval = wims_mathml_copy_string(yytext);
    replace_str(yylval,"\\lt ","<");
    replace_str(yylval,"\\gt ",">");
    yyleng = strlen(yylval);
    yylval[yyleng-9]=0; BEGIN(HTMLEND); return XMLSTRING;}
(.|\n) {yymore();}
}

<HTMLEND>{
"\}" {BEGIN(MATH); return ENDHTML;}
}

<ROMAN>{
"\{"  {return ST;}
"\}"  {BEGIN(MATH); return END;}
[a-zA-Z]   {yylval=wims_mathml_copy_string(yytext); return RMCHAR;}
[\t\n\r ]+       ;
.           {return yytext[0];}
}

<BBOLD>{
"\{"  {return ST;}
"\}"  {BEGIN(MATH); return END;}
[a-z] {yylval=wims_mathml_copy_string(yytext); return BBLOWERCHAR;}
[A-Z]   {yylval=wims_mathml_copy_string(yytext); return BBUPPERCHAR;}
[0-9]   {yylval=wims_mathml_copy_string(yytext); return BBDIGIT;}
[\t\n\r ]+       ;
.           {return yytext[0];}
}

<CALLIGRAPHIC>{
"\{"  {return ST;}
"\}"  {BEGIN(MATH); return END;}
[a-zA-Z]   {yylval=wims_mathml_copy_string(yytext); return CALCHAR;}
[\t\n\r ]+       ;
.           {return yytext[0];}
}

<FRAKTUR>{
"\{"  {return ST;}
"\}"  {BEGIN(MATH); return END;}
[a-zA-Z]   {yylval=wims_mathml_copy_string(yytext); return FRAKCHAR;}
[\t\n\r ]+       ;
.           {return yytext[0];}
}

<PLAINTEXT>{
\{[^\}]*?\}  {yylval=wims_mathml_copy_string(yytext+1); if ((yyleng > 1) && (yylval != wims_mathml_empty_string)) yylval[yyleng-2]='\0'; BEGIN(MATH); return TEXTSTRING;}
}

<INT1>{
"\{"      {return ST;}
"\}"      {BEGIN(INT2); return END;}
[0-9]+    {yylval=wims_mathml_copy_string_extra(yytext, 1); if (yyleng && (yylval != wims_mathml_empty_string)) { yylval[yyleng]=yylval[yyleng-1]; yylval[yyleng-1]='.'; yylval[yyleng+1]='\0'; } return INTONE;}
[\t\n\r ]+   ;
.         {return yytext[0];}
}

<INT2>{
"\{"      {return ST;}
"\}"      {BEGIN(INT3); return END;}
[0-9]+    {yylval=wims_mathml_copy_string_extra(yytext, 1); if (yyleng && (yylval != wims_mathml_empty_string)) { yylval[yyleng]=yylval[yyleng-1]; yylval[yyleng-1]='.'; yylval[yyleng+1]='\0'; } return INTTWO;}
[\t\n\r ]+   ;
.         {return yytext[0];}
}

<INT3>{
"\{"      {return ST;}
"\}"      {BEGIN(MATH); return END;}
[0-9]+    {yylval=wims_mathml_copy_string_extra(yytext, 1); if (yyleng && (yylval != wims_mathml_empty_string)) { yylval[yyleng]=yylval[yyleng-1]; yylval[yyleng-1]='.'; yylval[yyleng+1]='\0'; } return INTTHREE;}
[\t\n\r ]+   ;
.         {return yytext[0];}
}



%%

void wims_mathml_restart ()
{
	BEGIN(INITIAL);
}

static const char *  s_wims_mathml_buffer = 0;
static unsigned long s_wims_mathml_length = 0;

void wims_mathml_setup (const char * buffer, unsigned long length)
{
	yy_flush_buffer( YY_CURRENT_BUFFER );

	s_wims_mathml_buffer = buffer;
	s_wims_mathml_length = length;
}

static int s_wims_mathml_read (char * buffer, int max_size)
{
	int result = 0;

	if (max_size < s_wims_mathml_length)
	{
		result = max_size;

		memcpy (buffer, s_wims_mathml_buffer, max_size);

		s_wims_mathml_buffer += max_size;
		s_wims_mathml_length -= max_size;
	}
	else
	{
		result = s_wims_mathml_length;

		memcpy (buffer, s_wims_mathml_buffer, s_wims_mathml_length);

		s_wims_mathml_buffer = 0;
		s_wims_mathml_length = 0;
	}
	return result;
}

