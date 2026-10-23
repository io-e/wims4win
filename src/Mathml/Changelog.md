## 2014-01-04 [GK]
changed the syntax for the parameter ret_str in wims_mathml.y
 to allow a valid compilation with bison-3.0; the compilation with
 previous versions of bison remains still valid. See the bug report
 at [bugs.debian.org](http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=733369)

done :

- array row separators "\\" \cr "\tabularnewline" (...)
- special space "\ " allowed
- Accent replacing by htmlcodes (list is not yet complete...)
- Added "\right." if unbalanced \left( ...
 (preventing errors in mathml)
- added arg --max-mml-size to wims_mathml:
    wims_mathml will now know directly what our max_allowable_buffer_size is
    if the produced XML string is larger it will just send "ERROR"
    mathml() will return 0 ; and wims will do "insmath with gifs"
    note: this is just for a single call to wims_mathml
    note: is your latex is too large, just split it into two or more smaller calls to wims_mathml
    \( latex1 ) \( latex2 ) \( latex3 ) ...  \( latex_n )

- added C-lib headers in wims_mathml.cc to compile on linux (must look into this)

- fixed "empty row" ERROR  in "single collumn array"
    \begin{array}{c} A \\ \\ \\ B \end{array}

- added \bigint \biggint \bigggint without increasing fontsize (that results in loss of horizontal symmetry)
    Drawback (not solved yet...) is that "\left[ \biggint_{0}^{\pi} \right." the "[" will not take the stretched size of "\biggint"
    Incase increase in fontsize, the "[" will stretch...have to see how this works...

- transform `{\\rm }` in `\textrm{ }` in texmath.c (bpr)

- if cal letters do not exist, take the roman one.

- restored converting ">" into "\gt" in wims_mathml.c
  restored converting "<" into "\lt" in wims_mathml.c
- solve f'(x) messing up javacript for clickfill  ( ' -> \' )

- `\input{size}{id}{style}{readonly}`
  size: 0,999 spaces allowed
  id:0,999 spaces allowed -> mathml0 .. mathml999 (prefix mathml is hardcoded : reply1 = document.getElementById("mathml"+p);
   style: no "" allowed ; spaces allowed
   readonly : 0,1  spaces allowed(0 = write ; 1=readonly)
   example : \input{ 5 }{0 }{ color: red;fontcolor:blue;border 1px}{ 1}
- \input added support for hexcolor background-color:#00ff00;
- corrected some flaws with stretchy parenthesis;
  corrected missing \right. procedure in wims_mathml.cc
  * \lpar \rpar usable in createxo for \left( without \right)
  * \lbracket \rbracket usable in createxo for \left[ without \right]
  * \langle \rangle usable in createxo for \left< without \right>

- improved size & appearance of integral sign \int (larger)
    other sizes are bigint biggint bigggint
    Now parenthesis around integral sign are stretchy
    (),[],{},<>
    \lpar\rpar , \lbracket\rbracket, \lbrace\rbrace ,\langle\rangle
- added array with vertical separators (will need testing)
- add dfrac ; set dfrac to be 120% of \frac{}{} ...tune in to your favorite value !
  both frac/dfrac are in displaystyle.
- wims_mathml.l : add \Bf as "dialect" for \bf \boldfont
- add \not =, \not=, \not\in, \not \in
- changed lpar/rpar lbracket/rbracket lbrace/rbrace to OTHERDELIM
- added support for unbalanced \lbracket (etc)
- removed unused code (...)
- removed faulty "
- added \diagup \diagdown
- added \ointclockwise \varointclockwise \sqint \fint"
  more will come
- added extra (non LaTeX) option to \begin{array}{} command.
  now a single  "-" will produce on all rows a "\hline"
  now "-.-.-" will produce a hline under row1,row3,row5
  (the MathML code attribute is:  rowlines="solid none solid none solid")
  The options do not adversely interfere with normal latex (for gif)
  example:

      \begin{array}{c|c:|c-.-.-.} 1 & 2 & 3 \\ 1 & 2 & 3 \\ 1 & 2 & 3 \\ 1 & 2 & 3 \\ 1 & 2 & 3 \\ 1 & 2 & 3 \end{array}

- added lots of (probably useless) unicode symbols matching (ams)latex commands.
  added file UNICODE-2-LATEX.txt to be used as "database" for future reference.
  Note: not all commands are integrated in wims_mathml.l (just a random selection...)
  Note: the 'return' is just globally set ; it may need some fine tuning.
  Note: there is a limit to the amount of rules that can be used in wims_mathml.y
  these are defined in "flexdef.h": we should not pass this "JAMSTATE -32766" limit

- add option in wims_mathml to set the fontsize in mathml from wims (via mathml.c)

- add small script to extract latex commands from "wims_mathml" (just for info)

- add option to use zoom on MathML : to test copy mathml.c.ZOOM to mathml.c
    adm/light is adapted to adjusting size and zoom_option

- add \size[120]{\frac{1}{\sqrt{\pi}}} to set size sub string in mathml
  \size[0 - 999]{any tex string you like}
  for now no spaces are allowed in size argument: like in \size[ 123 ]{}
  We must discuss if we can adapt to known LaTex syntax?

- add alternatives just to avoid mathml-errors (...):
    \begin{align*} \begin{dcases} \begin{alignat} \begin{equation} \begin{eqnarray}
    \emph{} \vphantom
- add { n \choose k } as alternative for \binom{n}{k}
- add \sfrac{1}{3} : 1/3
- add \lmoustache \rmoustache \lgroup \rgroup \Arrowvert \bracevert
- add support for {\rm sin} syntax : \sin \rm{sin} {\rm sin} are equal Roman font
  In MathML Romanfont RM is <MI mathvariant="norma"></MI> tag
    (all other comon functions like cos,tan,log,etc are <MI></MI>)
    Note: every "word" will be in <MI>word</MI> tags
- added 'value' param to \input
- \input{size}{id}{style}{value}{readonly}

- finished error handling
    wims_mathml.y : All errors will flag ERROR to stdout/stderror.
    Wims will switch to insmath_with gifs...
- zooming "font + 100" %  (was "font + 150" %)
- "reinvented" {\rm text}
- removed declaration from <math></math> tag (it is already present in page)
- added config.c / mathml.c variable "disable_zoom" (used -for example- in dynapi3.phtml)
- restored declaration  <math xmlns="http://www.w3.org/1998/Math/MathML"></math> tag
  we have to look into this closer !
- small change in ERROR (no printing to stderror)
- mathml.c : clean exit after ERROR (...)
- wims_mathml.l : removed unused code of mine 4*x -> 4x .This is done by wims.cgi
  will soon try to clean more :  remove all non-wims code (remove all unused itex2mml specific code)
- corrected stupid typo error { n \choose k } : free $3 was not possible (should be $4 !)
- corrected wrong n-th root display \root{3} \of{ 2^{456} }
  the "\of" is now ignored : 2^{456} will now be under root-flag
  other way of writing: \root{3}{2^{456}}
- added
  * "\\longmapsto" {yylval = wims_mathml_copy_string("&#x27FC"); return MOL;}
  * "\\Longmapsfrom" {yylval = wims_mathml_copy_string("&#x27FD"); return MOL;}
  * "\\Longmapsto" {yylval = wims_mathml_copy_string("&#x27FE"); return MOL;}
- 20/9/2012 added
  * "\\large" { yylval=wims_mathml_copy_string("150%");wims_mathml_env_start = 1; return TMP_FONTSIZE;}
  * "\\Large" { yylval=wims_mathml_copy_string("180%");wims_mathml_env_start = 1; return TMP_FONTSIZE;}
  * "\\huge"  { yylval=wims_mathml_copy_string("220%");wims_mathml_env_start = 1; return TMP_FONTSIZE;}
  * "\\Huge"  { yylval=wims_mathml_copy_string("260%");wims_mathml_env_start = 1; return TMP_FONTSIZE;}

## 26/9/2012
added forgotten `^+-=` to recognisable "text value" in inputfield in `\input{size}{id}{style}{value}{readonly}`
  a {value} like `{10^3 - 10^2 = 9*10^2}` will be processed normally.

## 5/11/2012 added "\cline{}" as empty command.
`\cline{2-5}` will now be ignored and not interpreted as "text"
  the horizontal line in matrix can be imitated by \begin{array}{c.c.c.c-c.}
  meaning a horizontal line under the 4-th row
  This non-latex syntax "{c.c.c.c-c.}" will be ignored in math-with-gifs

## 7/11/2012
  replace non-unique id="wims_mathml" by a unique id="wims_mathml..."

## 18/11/2012
  corrected some syntax issues only noticed by older compiler
  corrected compiler warnings for "time_t" etc

## 15/12/2012
  corrected stretchy parenthesis display flaw in \left( { 1 \over 2 } \right)

## 24/10/2013
  added forgotten char "/" to \input recognizable characters for "value" like '1/233'

    \input{size}{id}{style}{value}{readonly}
    \input{10}{0}{background-color:lightblue;color:red;font-size:1.2em}{ 1/234 }{1}
  only interesting if a `pre_filled_in input_field` is needed...
  still problems with chars '>' '<' because they are converted into \lt \gt in wims_mathml.cc

## 22/11/2013
  added quick fix
  \underrightarrow{123456789}
  \overrightarrow{12345678}

## 24/11/2013
   added \underleftarrow \overleftarrow \underleftrightarrow \overleftrightarrow

## 25/11/2013
  in extra arrows: forgotten to add `wims_mathml_free_string($2);`
  TODO: check if these can be harmonized eg made more generic

## 25/11/2013
  Harmonized UNDERARROW OVERARROW ; to add more just

  "\\my_new_under_Arrow" {yylval = wims_mathml_copy_string("&some_unicode_arrow;");  return UNDERARROW;}
  "\\my_new_over_Arrow"  {yylval = wims_mathml_copy_string("&some_unicode_arrow;");  return OVERARROW; }
    added as proof of concept:
    \overrightharpoonup
    \overrightharpoondown
    \underrightharpoonup
    \underrightharpoondown

## 23/12/2013
  - added new environment '\begin{html} any_valid_html_text \end{html}'
  - corrected (W3C) mathml string output for '\input{size}{id}{style}{value}{readonly}'
  - added str_replace() to replace '\lt' and '\gt' from SVG-text or HTML-text into  '<' and '>'
  - corrected SVG environment (trouble with \lt \gt)

We now use the 'W3C correct' annotations for embedding HTML / SVG in XML:

    <semantics><annotation-xml encoding="application/xhtml+xml"> any_valid_html_text </annotation-xml></semantics>
    <semantics><annotation-xml encoding="SVG1.1">      any_valid_svg_text  </annotation-xml></semantics>

  Example:
  my homebrew 'input_pseudo_latex'

    \frac{ \input{2}{0}{color:red}{?}{0} }{ 1234 }
  is equivalent to:

    \frac{ \begin{html}<input size='2' id='mathml0' style='color:red' value='?' />\end{html} }{ 1234 }

  In the html/svg environment, any thing goes, like the square root of an image...

    \sqrt{ \begin{html} <img src="my_image.gif" alt="noalt" /> \end{html} }

  These are all -of course- incompatible with Latex!

## 25/12/2013
  - add 'xmlns="http://www.w3.org/1999/xhtml"' to <input />
  - remove the `<mn></mn>` around `<input /> `: no longer needed
    `\input{5}{0}{color:red}{?}{0}`
    gets translated into:

        <semantics>
            <annotation-xml encoding="application/xhtml+xml">
                <input type="text" xmlns="http://www.w3.org/1999/xhtml" size="5" id="mathml0" value="?" style="color:red" />
            </annotation-xml>
        </semantics>

If MathJax fixes it's bug concering the 'encoding', the input-field will work natively in Chrome+MathJax extension...

## 26/12/2013
  Fixing MathJax inputfield bug by using a 'textarea' instead of a 'input'

  - Corrected 'rowlines' in 'array' environment (using 'none' instead of '')
     MathJax actually shows rowlines when rowlines="" ...

  - Using a 'pre-styled textarea' as inputfield look-alike  : fixing the compatibility / bug issue with MathJaX in Chrome
  `\input{4}{0}{color:red}{1234}{1}`
  gets translated into:

        <semantics>
            <annotation-xml encoding="application/xhtml+xml">
                <textarea xmlns="http://www.w3.org/1999/xhtml" cols="4" rows="1" id="mathml0" style="vertical-align:middle;overflow:hidden;resize:none;color:red"  readonly="readonly">
                    1234
                </textarea>
            </annotation-xml>
        </semantics>

NOTE: revert to inputfields when MathJax is upgraded regarding usage of (X)HTML in MathML.

## 28/12/2013
  Corrected non-stretchy perentheses
  wims_mathml.y: setting all LEFT/RIGHT delimiters to
    <mo stretchy="true">

## 18/1/2014
  added \limit : does nothing

  example: `\int \limit_{123}^{123}` is equal to `\int_{123}^{123}`

  note: in latex \limit will center the text above and/or under the symbol (\int,\wedge etc)

  If a identical behaviour (centered) is needed, use:
    \underset{123}{ \overset{123} \int }

## 9/3/2014
  added \char44 == &#44 (bug 8461)

## 4/3/2015
added `{"\ {0,5}[a-zA-Z0-9#\ :\-\_\+\=\(\)\[\]\@\~\!\#\$\%\^\&\*\/\,;.*\^\?]{0,250}\ {0,5}"}`
to recognizable chars in input

## 5/3/2015
Simplified input "regex"  to `"\\input{"\ {0,5}[0-9]{1,3}\ {0,5}"}{"*.*"}{"*.*"}{"*.*"}{"\ {0,3}[0-1]\ {0,3}"}"`

## 5/3/2015 last commit no good
multiple inputs in a single latex command are not recognized (??)
  revert to 4/3

## 09-06-2015: add textcolor
- size font-size:1.0em; in inputfield

## 26/09/2015
now all mathcal fonts exist.

## 30/09/2015
FOR TESTING: added a span element 'font-size=1em' around the math and set default tex-size=100% in mathml.c

## 30/04/2016
In MathJaX V2.6 the issue with the inputfield in mathml seems to be resolved.
Switching back from "textarea" to "inputfield"

Note+todo: latest html5 options for "input" are not yet implemented.

## 27/10/2016
revert to textarea, because of some bug in Safari.
  Mathjax can not cure this browser error:
  [MathJax issue 1644](https://github.com/mathjax/MathJax/issues/1644)

The textarea will ignore an "enter" (keycode 13) and will be in everyday usage similar to an input-field
  revert to inputfield when Safari (and possibly other Webkit browsers) have better support
  (but this will not happen in the near future, I'm afraid...)
  At the same time "exec.c" is modified: it will activate Mathjax for all non-real-gecko-browsers.

## 22/2/2017
fix for the pari [version > 2.9.0] printtex output "\*"

  pari: printtex(35*x^6 - 8*x + 3) --> 35\*x^6  - 8\*x  + 3
  "\\*"      {yylval=wims_mathml_copy_string("&nbsp;"); return MI;}

## 23/2/2017
 pari printtex " \*" --> &InvisibleTimes;

## 08/05/2017
 add ; in code as &#x

## 15/05/2017
added `_` as non-latex code for array environment.

latex usage:

    \begin{array}{c|c:c|c -._.-.}
    1 & 2 & 3 & 4 \\
    1 & 2 & 3 & 4 \\
    1 & 2 & 3 & 4 \\
    1 & 2 & 3 & 4 \\
    1 & 2 & 3 & 4 \\
    1 & 2 & 3 & 4 \\
    \end{array}

meaning:

    solid vertical lines between columns 1,2
    dashed  vertical line between 2,3
    solid vertical lines between columns 3,4

    solid horizontal line between row 1,2
    no horizontal line between row 2,3
    dashed horizontal line between row 3,4
    no horizontal line between row 4,5
    solid horizontal line between row 5,6

NOTE: the problem is the in mathml all attributes must be known before the actual table can be filled...so all layout information must be derived from the `{c|c:c|c -._.-.}`

    <mtable rowspacing="0.5ex" columnalign="center center center center" columnlines="solid dashed solid" rowlines="solid none dashed none solid none">

To solve this in itex2mml/wims_mathml one must store the mathml strings into ca 6 variables...and print these when the \end{array is found...

## 17/05/2017
a fix for \hline incompatibility between real latex and wims_mathml

`\hline` is now ignored (just an empty string...`\hline` never had a real meaning (ROWSEP) in wims_mathml)
  an array now can use both `\hline` and the "non-latex" syntax for horizontal lines `(-._)`
  the `\hline` will give the correct separation line in math-image-mode.
  `\cr \tabularnewline` are unchanged...giving 'ROWSEP'

note:

    find ~/public_html/modules/[EHU]* -type f -name "*def" -exec grep -H "\\hline" "{}" ";"
will find only one hit: U1/algebra/docsyslin.fr/doc/1/exgauss.def

## 31/5/2017: bug in MathJax
rowlines="" will show rowlines. So we explicitly declare rowlines=" none ".

## 27/6/2017 [jm.evers]
introduced some accents in "math mode"
LaTeX   Description

    \`{o}   grave accent
    \'{o}   acute accent
    \^{o}   circumflex
    \"{o}   umlaut, trema or dieresis
    \H{o}   long Hungarian umlaut (double acute)
    \~{o}   tilde
    \c{c}   cedilla
    \k{a}   ogonek
    \l{}  barred l (l with stroke)
    \={o}   macron accent (a bar over the letter)
    \b{o}   bar under the letter
    \.{o}   dot over the letter
    \d{u}   dot under the letter
    \r{a}   ring over the letter
    \u{o}   breve over the letter
    \v{s}   caron/há?ek ("v") over the letter
    \t{oo}  "tie" (inverted u) over the two letters
    \o{}  slashed o (o with stroke)

there is no way to introduce tex-like accents in the text{} / textrm{} command family.
use something like this in "direct exec"

    \Huge
    \begin{array}{lll}
    \"{O} & \'{O} & \`{O} \\
    \^{O} & \~{O} & \r{O} \\
    \c{O} & \.{O} & \={O} \\
    \k{O} & \b{O} & \d{O} \\
    \O{} & \o{} & \l{} & \\
    \H{O} &  \t{uu} & \textrm{Hall}\"{o} \; \textrm{means Hallo in Danish ? }
    \end{array}

## 29/6/2017 [jm.evers]
added:

  if ((yyleng > 1) && (yylval != wims_mathml_empty_string)) yylval[yyleng+1]='\0';
and removed freeing of `$1` by introducing `*s1` and freeing this pointer after use (see wims_mathml.y)

The problems are clear on Linux GCC :
(clang|bison|flex FreeBSD no (!?) problems...)

MathML: wims_mathml(82765,0x7fff77497000) malloc: *** error for object 0x10e29cb89: pointer being freed was not allocated *** set a breakpoint in malloc_error_break to debug Image:
This solution works, but I do not understand why !!

Found probable cause: the ~ was missing , and was causing the freeing due to another match of \~ (I guess...)

added user info for implemented arrows,symbols and stuff
added Celcius and a few other missing things...

## 1/7/2017
forgotten \u{}

## 11/10/2017
all "e" will be typeset in mathvariant 'normal' as requested:

## 12/10/2017
added lex macro EULER for 'e' (don't know of another trick to implement...)

## 19/10/2017
 delete the modification for EULER

## 30/10/2017
Correcting odd behaviour in Gecko's mathml : not showing an overline in the fancy themes
(plain theme [default] is not affected)
changed the mo-tag "wims_mathml.y" line 1497 (added space)

    WIDEBAR closedTerm {
    $$ = wims_mathml_copy3("<mover>", $2, "<mo>&#x000AF;</mo></mover>");
    wims_mathml_free_string($2);
    };

into

    <mo> &#x000AF; </mo>

## 11/11/2017
added \it as alternative for \mathit or \emph --> ITALICS

## 13/11/2017 - r12112 [obado]
replace the inline style (span element) "font-size:1em" on mathml by a css class, that can be eventually customised (use themes/_css/maths.css).

## 3/7/2018 [BPR]
added \Longleftarow

## 09/04/2018
add changes included in itex2MML v1.4.10 :
[itexToMML revision 64](https://golem.ph.utexas.edu/~distler/code/itexToMML/revision/64)

Add some new large math operators (\bigsqcap and \biginterleave)
from the MathML3 Operator Dictionary.

## 19/4/2019
added '\cancel' as alternative syntax for '\slash'...
see [wimsedu.info](https://wimsedu.info/?topic=utilisation-de-package-latex-specifique/#post-4941)

## 25/8/2019
added (a few things to sync with KaTeX)
\brace,\brack

some array shortcuts : \cases,\aligned,\Bmatrix,\Vmatrix,\vmatrix,\pmatrix

## 15/02/2020
added \not\subset (without spaces)

## 22/06/2020 - r15175 [obado]
replace textarea "rows" attribute by a style="width:" so it will work with all font-families (not only monospaces).
+ move inline style of .wims_mathml textarea to themes/_css/maths.css
