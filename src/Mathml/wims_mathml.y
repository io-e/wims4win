/**            wims_mathml 1.4.11
 *   wims_mathml.y last modified 06/22/2020 (r.15175)
 **/

%parse-param {char **ret_str}

%{
  #include <stdio.h>
  #include <string.h>
  #include <stdlib.h>

  #include "wims_mathml.h"

  #define YYSTYPE char *

  #define yytext wims_mathml_yytext
   extern int yylex ();
   extern char * yytext;

  static void wims_mathml_default_error (const char * msg)
  {
    //   if (msg){ fprintf(stderr, "Line: %d Error: %s\n", wims_mathml_lineno, msg);}
  }

  void (*wims_mathml_error) (const char * msg) = wims_mathml_default_error;

  static void yyerror (char ** ret_str, char * s)
  {
  //    char * msg = wims_mathml_copy3 (s, " at token ", yytext);
    if (wims_mathml_error)
      (*wims_mathml_error) ("ERROR");
  //     wims_mathml_free_string (msg);
  }

  /* Note: If length is 0, then buffer is treated like a string; otherwise only length bytes are written.
   */
  static void wims_mathml_default_write (const char * buffer, unsigned long length)
  {
    if (buffer)
    {
      if (length)
        fwrite (buffer, 1, length, stdout);
      else
        fputs (buffer, stdout);
    }
  }

  static void wims_mathml_default_write_mathml (const char * mathml)
  {
    if (wims_mathml_write)
      (*wims_mathml_write) (mathml, 0);
  }

  #ifdef wims_mathml_CAPTURE
    static char * wims_mathml_output_string = "" ;

    const char * wims_mathml_output ()
    {
      char * copy = (char *) malloc((wims_mathml_output_string ? strlen(wims_mathml_output_string) : 0) + 1);
      if (copy)
      {
        if (wims_mathml_output_string)
        {
          strcpy(copy, wims_mathml_output_string);
          if (*wims_mathml_output_string != '\0')
            free(wims_mathml_output_string);
        }
        else
          copy[0] = 0;
        wims_mathml_output_string = "";
      }
      return copy;
    }

    static void wims_mathml_capture (const char * buffer, unsigned long length)
    {
      if (buffer)
      {
        if (length)
        {
          unsigned long first_length = wims_mathml_output_string ? strlen(wims_mathml_output_string) : 0;
          char * copy  = (char *) malloc(first_length + length + 1);
          if (copy)
          {
            if (wims_mathml_output_string)
            {
              strcpy(copy, wims_mathml_output_string);
              if (*wims_mathml_output_string != '\0')
                free(wims_mathml_output_string);
            }
            else
              copy[0] = 0;
            strncat(copy, buffer, length);
            wims_mathml_output_string = copy;
          }
        } else {
          char * copy = wims_mathml_copy2(wims_mathml_output_string, buffer);
          if (*wims_mathml_output_string != '\0')
            free(wims_mathml_output_string);
            wims_mathml_output_string = copy;
        }
      }
    }

    static void wims_mathml_capture_mathml (const char * buffer)
    {
      char * temp = wims_mathml_copy2(wims_mathml_output_string, buffer);
      if (*wims_mathml_output_string != '\0')
        free(wims_mathml_output_string);
      wims_mathml_output_string = temp;
    }
    void (*wims_mathml_write) (const char * buffer, unsigned long length) = wims_mathml_capture;
    void (*wims_mathml_write_mathml) (const char * mathml) = wims_mathml_capture_mathml;
  #else
    void (*wims_mathml_write) (const char * buffer, unsigned long length) = wims_mathml_default_write;
    void (*wims_mathml_write_mathml) (const char * mathml) = wims_mathml_default_write_mathml;
  #endif

  char * wims_mathml_empty_string = "";

  /* Create a copy of a string, adding space for extra chars
   */
  char * wims_mathml_copy_string_extra (const char * str, unsigned extra)
  {
    char * copy = (char *) malloc(extra + (str ? strlen (str) : 0) + 1);
    if (copy)
    {
      if (str)
        strcpy(copy, str);
      else
        copy[0] = 0;
    }
    return copy ? copy : wims_mathml_empty_string;
  }

  /* Create a copy of a string, appending two strings
   */
  char * wims_mathml_copy3 (const char * first, const char * second, const char * third)
  {
    int  first_length =  first ? strlen( first) : 0;
    int second_length = second ? strlen(second) : 0;
    int  third_length =  third ? strlen( third) : 0;

    char * copy = (char *) malloc(first_length + second_length + third_length + 1);

    if (copy)
    {
      if (first)
        strcpy(copy, first);
      else
        copy[0] = 0;

      if (second) strcat(copy, second);
      if ( third) strcat(copy,  third);
    }
    return copy ? copy : wims_mathml_empty_string;
  }

  /**
   * Create a copy of a string, appending 4 strings
   */
  char * wims_mathml_copy5 (const char * first, const char * second, const char * third ,const char * forth,const char * fifth)
  {
    int  first_length =  first ? strlen( first) : 0;
    int second_length = second ? strlen(second) : 0;
    int  third_length =  third ? strlen( third) : 0;
    int  forth_length =  forth ? strlen( forth) : 0;
    int  fifth_length =  fifth ? strlen( fifth) : 0;

    char * copy = (char *) malloc(first_length + second_length + third_length + forth_length + fifth_length + 1);

    if (copy)
    {
      if (first)
        strcpy(copy, first);
      else
        copy[0] = 0;

      if (second) strcat(copy, second);
      if ( third) strcat(copy,  third);
      if ( forth) strcat(copy,  forth);
      if ( fifth) strcat(copy,  fifth);
    }
    return copy ? copy : wims_mathml_empty_string;
  }

  /**
   * Create a copy of a string, appending 6 strings
   */
  char * wims_mathml_copy7 (const char * first, const char * second, const char * third ,const char * forth,const char * fifth , const char * six , const char * seven)
  {
    int  first_length =  first ? strlen( first) : 0;
    int second_length = second ? strlen(second) : 0;
    int  third_length =  third ? strlen( third) : 0;
    int  forth_length =  forth ? strlen( forth) : 0;
    int  fifth_length =  fifth ? strlen( fifth) : 0;
    int  six_length =  six ? strlen( six ) : 0;
    int  seven_length =  seven ? strlen( seven ) : 0;

    char * copy = (char *) malloc(first_length + second_length + third_length + forth_length + fifth_length + six_length + seven_length + 1);

    if (copy)
    {
      if (first)
        strcpy(copy, first);
      else
        copy[0] = 0;

      if (second) strcat(copy, second);
      if ( third) strcat(copy,  third);
      if ( forth) strcat(copy,  forth);
      if ( fifth) strcat(copy,  fifth);
      if ( six) strcat(copy,  six);
      if ( seven) strcat(copy,  seven);
    }
    return copy ? copy : wims_mathml_empty_string;
  }

  /**
   * Create a copy of a string, appending 12 strings
   */
  char * wims_mathml_copy13( const char * A , const char * B , const char * C , const char * D , const char * E , const char * F , const char * G , const char * H,  const char * I,  const char * J,  const char * K,  const char * L,  const char * M){
    int  A_length =  A ? strlen( A ) : 0;
    int  B_length =  B ? strlen( B ) : 0;
    int  C_length =  C ? strlen( C ) : 0;
    int  D_length =  D ? strlen( D ) : 0;
    int  E_length =  E ? strlen( E ) : 0;
    int  F_length =  F ? strlen( F ) : 0;
    int  G_length =  G ? strlen( G ) : 0;
    int  H_length =  H ? strlen( H ) : 0;
    int  I_length =  I ? strlen( I ) : 0;
    int  J_length =  J ? strlen( J ) : 0;
    int  K_length =  K ? strlen( K ) : 0;
    int  L_length =  L ? strlen( L ) : 0;
    int  M_length =  M ? strlen( M ) : 0;

    char * copy = (char *) malloc(A_length + B_length +   C_length +   D_length +   E_length +   F_length +   G_length +   H_length +   I_length +   J_length +   K_length +   L_length +   M_length + 1 );

    if (copy)
    {
      if (A)
        strcpy(copy, A);
      else
        copy[0] = 0;

      if (B) strcat(copy, B);
      if (C) strcat(copy, C);
      if (D) strcat(copy, D);
      if (E) strcat(copy, E);
      if (F) strcat(copy, F);
      if (G) strcat(copy, G);
      if (H) strcat(copy, H);
      if (I) strcat(copy, I);
      if (J) strcat(copy, J);
      if (K) strcat(copy, K);
      if (L) strcat(copy, L);
      if (M) strcat(copy, M);
    }
    return copy ? copy : wims_mathml_empty_string;
  }


  /* Create a copy of a string, appending a second string
   */
  char * wims_mathml_copy2 (const char * first, const char * second)
  {
    return wims_mathml_copy3(first, second, 0);
  }

  /* Create a copy of a string
   */
  char * wims_mathml_copy_string (const char * str)
  {
    return wims_mathml_copy3(str, 0, 0);
  }

  /* Create a copy of a string, escaping unsafe characters for XML
   */
  char * wims_mathml_copy_escaped (const char * str)
  {
    unsigned long length = 0;

    const char * ptr1 = str;

    char * ptr2 = 0;
    char * copy = 0;

    if ( str == 0) return wims_mathml_empty_string;
    if (*str == 0) return wims_mathml_empty_string;

    while (*ptr1)
    {
      switch (*ptr1)
      {
        case '<':  /* &lt;   */
        case '>':  /* &gt;   */
          length += 4;
          break;
        case '&':  /* &amp;  */
          length += 5;
          break;
        case '\'': /* &apos; */
        case '"':  /* &quot; */
        case '-':  /* &#x2d; */
          length += 6;
          break;
        default:
          length += 1;
          break;
      }
      ++ptr1;
    }

    copy = (char *) malloc (length + 1);

    if (copy)
    {
      ptr1 = str;
      ptr2 = copy;

      while (*ptr1)
      {
        switch (*ptr1)
        {
          case '<':
            strcpy (ptr2, "&lt;");
            ptr2 += 4;
            break;
          case '>':
            strcpy (ptr2, "&gt;");
            ptr2 += 4;
            break;
          case '&':  /* &amp;  */
            strcpy (ptr2, "&amp;");
            ptr2 += 5;
            break;
          case '\'': /* &apos; */
            strcpy (ptr2, "&apos;");
            ptr2 += 6;
            break;
          case '"':  /* &quot; */
            strcpy (ptr2, "&quot;");
            ptr2 += 6;
            break;
          case '-':  /* &#x2d; */
            strcpy (ptr2, "&#x2d;");
            ptr2 += 6;
            break;
          default:
            *ptr2++ = *ptr1;
            break;
        }
        ++ptr1;
      }
      *ptr2 = 0;
    }
    return copy ? copy : wims_mathml_empty_string;
  }

  /* Create a hex character reference string corresponding to code
   */
  char * wims_mathml_character_reference (unsigned long int code)
  {
    #define ENTITY_LENGTH 10
    char * entity = (char *) malloc(ENTITY_LENGTH);
    sprintf(entity, "&#x%05lx;", code);
    return entity;
  }

  void wims_mathml_free_string (char * str)
  {
    if (str && str != wims_mathml_empty_string)
      free(str);
  }

%}

%left TEXOVER TEXATOP
%token TMP_FONTSIZE LARGERINT BIGINT BIGGINT BIGGGINT CHAR STARTMATH STARTDMATH ENDMATH MI MIB MN MO SUP SUB MROWOPEN MROWCLOSE LEFT RIGHT BIG BBIG BIGG BBIGG BIGL BBIGL BIGGL BBIGGL DFRAC FRAC TFRAC SFRAC OPERATORNAME MATHOP MATHBIN MATHREL MOP MOL MOLL MOF MOR PERIODDELIM OTHERDELIM LEFTDELIM RIGHTDELIM MOS MOB SQRT ROOT BINOM BINOM2 TBINOM UNDER OVER OVERBRACE UNDERLINE UNDERBRACE UNDEROVER TENSOR MULTI ARRAYALIGN TEX_OPTIONS ARRAY COLSEP ROWSEP ARRAYOPTS COLLAYOUT COLALIGN ROWALIGN ALIGN EQROWS EQCOLS ROWLINES COLLINES FRAME PADDING ATTRLIST ITALICS BOLD BOXED SLASHED RM WIMSROMAN BB ST END BBLOWERCHAR BBUPPERCHAR BBDIGIT CALCHAR FRAKCHAR CAL FRAK CLAP LLAP RLAP ROWOPTS TEXTSIZE SCSIZE SCSCSIZE DISPLAY TEXTSTY TEXTBOX TEXTSTRING XMLSTRING CELLOPTS ROWSPAN COLSPAN THINSPACE MEDSPACE THICKSPACE QUAD QQUAD NEGSPACE PHANTOM HREF UNKNOWNCHAR EMPTYMROW STATLINE TOOLTIP TOGGLE FGHIGHLIGHT BGHIGHLIGHT SPACE INTONE INTTWO INTTHREE BAR WIDEBAR VEC WIDEVEC OVERARROW UNDERARROW HAT WIDEHAT CHECK WIDECHECK TILDE WIDETILDE ACCENTS DOT DDOT DDDOT DDDDOT UNARYMINUS UNARYPLUS BEGINENV ENDENV EQUATION MATRIX PMATRIX BMATRIX BBMATRIX VMATRIX VVMATRIX SVG ENDSVG HTML ENDHTML SMALLMATRIX CASES ALIGNED GATHERED SUBSTACK PMOD RMCHAR COLOR SPECIAL INPUT LEWIS BGCOLOR XARROW OPTARGOPEN OPTARGCLOSE ITEXNUM RAISEBOX NEG BRACK SHORTARRAY

%%

doc:  xmlmmlTermList {/* all processing done in body*/};

xmlmmlTermList:
{/* nothing - do nothing*/}
| char {/* proc done in body*/}
| expression {/* all proc. in body*/}
| xmlmmlTermList char {/* all proc. in body*/}
| xmlmmlTermList expression {/* all proc. in body*/};

char: CHAR {printf("%s", $1);};

expression: STARTMATH ENDMATH {/* empty math group - ignore*/}
| STARTDMATH ENDMATH {/* ditto */}
| STARTMATH compoundTermList ENDMATH {
  char ** r = (char **) ret_str;
  char *s;
  if( set_javascript() == 1){
   s = wims_mathml_copy7("<span class=\"wims_mathml\" onclick=\"javascript:wims_mathml_zoom('",read_mathml_id(),"')\"><math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"inline\"><mstyle id=\"", read_mathml_id() ,"\" mathsize=\"100%%\">", $2, "</mstyle></math></span>");
  }
  else
  {
    s = wims_mathml_copy7("<span class=\"wims_mathml\"><math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"inline\"><mstyle id=\"", read_mathml_id() ,"\" mathsize=\"",read_fontsize(),"\">", $2, "</mstyle></math></span>");
  }
  wims_mathml_free_string($2);
  if (r) {
    (*r) = (s == wims_mathml_empty_string) ? 0 : s;
  }
  else
  {
    if (wims_mathml_write_mathml)
      (*wims_mathml_write_mathml) (s);
    wims_mathml_free_string(s);
  }
}
| STARTDMATH compoundTermList ENDMATH {
  char ** r = (char **) ret_str;
  char *s;
  if( set_javascript() == 1){
   s = wims_mathml_copy7("<span style=\"font-size:1em;\" onclick=\"javascript:wims_mathml_zoom('",read_mathml_id(),"')\"><math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"block\"><mstyle id=\"", read_mathml_id() ,"\" mathsize=\"100%%\">", $2, "</mstyle></math></span>");
  }
  else
  {
    s = wims_mathml_copy7("<span style=\"font-size:1em;\"><math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"block\"><mstyle id=\"", read_mathml_id() ,"\" mathsize=\"",read_fontsize(),"\">", $2, "</mstyle></math></span>");
  }
  wims_mathml_free_string($2);
  if (r) {
    (*r) = (s == wims_mathml_empty_string) ? 0 : s;
  }
  else {
    if (wims_mathml_write_mathml)
      (*wims_mathml_write_mathml) (s);
    wims_mathml_free_string(s);
  }
};

compoundTermList: compoundTerm {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| compoundTermList compoundTerm {
  $$ = wims_mathml_copy2($1, $2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

compoundTerm: mob SUB closedTerm SUP closedTerm {
  if (wims_mathml_displaymode == 1) {
    char * s1 = wims_mathml_copy3("<munderover>", $1, " ");
    char * s2 = wims_mathml_copy3($3, " ", $5);
    $$ = wims_mathml_copy3(s1, s2, "</munderover>");
    wims_mathml_free_string(s1);
    wims_mathml_free_string(s2);
  }
  else {
    char * s1 = wims_mathml_copy3("<msubsup>", $1, " ");
    char * s2 = wims_mathml_copy3($3, " ", $5);
    $$ = wims_mathml_copy3(s1, s2, "</msubsup>");
    wims_mathml_free_string(s1);
    wims_mathml_free_string(s2);
  }
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
}
| mob SUB closedTerm {
  if (wims_mathml_displaymode == 1) {
    char * s1 = wims_mathml_copy3("<munder>", $1, " ");
    $$ = wims_mathml_copy3(s1, $3, "</munder>");
    wims_mathml_free_string(s1);
  }
  else {
    char * s1 = wims_mathml_copy3("<msub>", $1, " ");
    $$ = wims_mathml_copy3(s1, $3, "</msub>");
    wims_mathml_free_string(s1);
  }
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
}
| mob SUP closedTerm SUB closedTerm {
  if (wims_mathml_displaymode == 1) {
    char * s1 = wims_mathml_copy3("<munderover>", $1, " ");
    char * s2 = wims_mathml_copy3($5, " ", $3);
    $$ = wims_mathml_copy3(s1, s2, "</munderover>");
    wims_mathml_free_string(s1);
    wims_mathml_free_string(s2);
  }
  else {
    char * s1 = wims_mathml_copy3("<msubsup>", $1, " ");
    char * s2 = wims_mathml_copy3($5, " ", $3);
    $$ = wims_mathml_copy3(s1, s2, "</msubsup>");
    wims_mathml_free_string(s1);
    wims_mathml_free_string(s2);
  }
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
}
| mob SUP closedTerm {
  if (wims_mathml_displaymode == 1) {
    char * s1 = wims_mathml_copy3("<mover>", $1, " ");
    $$ = wims_mathml_copy3(s1, $3, "</mover>");
    wims_mathml_free_string(s1);
  }
  else {
    char * s1 = wims_mathml_copy3("<msup>", $1, " ");
    $$ = wims_mathml_copy3(s1, $3, "</msup>");
    wims_mathml_free_string(s1);
  }
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
}
|mib SUB closedTerm SUP closedTerm {
  if (wims_mathml_displaymode == 1) {
    char * s1 = wims_mathml_copy3("<munderover>", $1, " ");
    char * s2 = wims_mathml_copy3($3, " ", $5);
    $$ = wims_mathml_copy3(s1, s2, "</munderover>");
    wims_mathml_free_string(s1);
    wims_mathml_free_string(s2);
  }
  else {
    char * s1 = wims_mathml_copy3("<msubsup>", $1, " ");
    char * s2 = wims_mathml_copy3($3, " ", $5);
    $$ = wims_mathml_copy3(s1, s2, "</msubsup>");
    wims_mathml_free_string(s1);
    wims_mathml_free_string(s2);
  }
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
}
| mib SUB closedTerm {
  if (wims_mathml_displaymode == 1) {
    char * s1 = wims_mathml_copy3("<munder>", $1, " ");
    $$ = wims_mathml_copy3(s1, $3, "</munder>");
    wims_mathml_free_string(s1);
  }
  else {
    char * s1 = wims_mathml_copy3("<msub>", $1, " ");
    $$ = wims_mathml_copy3(s1, $3, "</msub>");
    wims_mathml_free_string(s1);
  }
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
}
| mib SUP closedTerm SUB closedTerm {
  if (wims_mathml_displaymode == 1) {
    char * s1 = wims_mathml_copy3("<munderover>", $1, " ");
    char * s2 = wims_mathml_copy3($5, " ", $3);
    $$ = wims_mathml_copy3(s1, s2, "</munderover>");
    wims_mathml_free_string(s1);
    wims_mathml_free_string(s2);
  }
  else {
    char * s1 = wims_mathml_copy3("<msubsup>", $1, " ");
    char * s2 = wims_mathml_copy3($5, " ", $3);
    $$ = wims_mathml_copy3(s1, s2, "</msubsup>");
    wims_mathml_free_string(s1);
    wims_mathml_free_string(s2);
  }
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
}
| mib SUP closedTerm {
  if (wims_mathml_displaymode == 1) {
    char * s1 = wims_mathml_copy3("<mover>", $1, " ");
    $$ = wims_mathml_copy3(s1, $3, "</mover>");
    wims_mathml_free_string(s1);
  }
  else {
    char * s1 = wims_mathml_copy3("<msup>", $1, " ");
    $$ = wims_mathml_copy3(s1, $3, "</msup>");
    wims_mathml_free_string(s1);
  }
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
}
| closedTerm SUB closedTerm SUP closedTerm {
  char * s1 = wims_mathml_copy3("<msubsup>", $1, " ");
  char * s2 = wims_mathml_copy3($3, " ", $5);
  $$ = wims_mathml_copy3(s1, s2, "</msubsup>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
}
| closedTerm SUP closedTerm SUB closedTerm {
  char * s1 = wims_mathml_copy3("<msubsup>", $1, " ");
  char * s2 = wims_mathml_copy3($5, " ", $3);
  $$ = wims_mathml_copy3(s1, s2, "</msubsup>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
}
| closedTerm SUB closedTerm {
  char * s1 = wims_mathml_copy3("<msub>", $1, " ");
  $$ = wims_mathml_copy3(s1, $3, "</msub>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
}
| closedTerm SUP closedTerm {
  char * s1 = wims_mathml_copy3("<msup>", $1, " ");
  $$ = wims_mathml_copy3(s1, $3, "</msup>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
}
| SUB closedTerm {
  $$ = wims_mathml_copy3("<msub><mo></mo>", $2, "</msub>");
  wims_mathml_free_string($2);
}
| SUP closedTerm {
  $$ = wims_mathml_copy3("<msup><mo></mo>", $2, "</msup>");
  wims_mathml_free_string($2);
}
| TMP_FONTSIZE compoundTerm {
  $$ = wims_mathml_copy5("<mstyle mathsize=\"",$1,"\">",$2,"</mstyle>");
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
}
| closedTerm {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
};


closedTerm: array
| shortarray
| unaryminus
| unaryplus
| mib
| mi {
  $$ = wims_mathml_copy3("<mi>", $1, "</mi>");
  wims_mathml_free_string($1);
}
| mn {
  $$ = wims_mathml_copy3("<mn>", $1, "</mn>");
  wims_mathml_free_string($1);
}
| mo
| tensor
| multi
| mfrac
| binom
| msqrt
| mroot
| raisebox
| munder
| mover
| bar
| vec
| overarrow
| underarrow
| hat
| dot
| ddot
| dddot
| ddddot
| check
| tilde
| accents
| moverbrace
| munderbrace
| munderline
| munderover
| emptymrow
| mathclap
| mathllap
| mathrlap
| displaystyle
| textstyle
| textsize
| scriptsize
| scriptscriptsize
| italics
| bold
| roman
| rmchars
| bbold
| frak
| slashed
| boxed
| cal
| space
| textstring
| thinspace
| medspace
| thickspace
| quad
| qquad
| negspace
| phantom
| href
| statusline
| tooltip
| toggle
| fghighlight
| bghighlight
| color
| special
| input
| lewis
| bigint
| texover
| texatop
| brack
| MROWOPEN closedTerm MROWCLOSE {
  $$ = wims_mathml_copy_string($2);
  wims_mathml_free_string($2);
}
| MROWOPEN compoundTermList MROWCLOSE {
  $$ = wims_mathml_copy3("<mrow>", $2, "</mrow>");
  wims_mathml_free_string($2);
}
| left compoundTermList right {
  char * s1 = wims_mathml_copy3("<mrow>", $1, $2);
  $$ = wims_mathml_copy3(s1, $3, "</mrow>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
}
| mathenv
| substack
| pmod
| unrecognized;

left: LEFT LEFTDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo stretchy=\"true\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| LEFT OTHERDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo stretchy=\"true\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| LEFT PERIODDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy_string("");
  wims_mathml_free_string($2);
};

right: RIGHT RIGHTDELIM {
  $$ = wims_mathml_copy3("<mo stretchy=\"true\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| RIGHT OTHERDELIM {
  $$ = wims_mathml_copy3("<mo stretchy=\"true\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| RIGHT PERIODDELIM {
  $$ = wims_mathml_copy_string("");
  wims_mathml_free_string($2);
};


bigint:  BIGGGINT {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy_string("<mtable columnalign=\"center\" rowspacing=\"0.5ex\"><mtr><mtd><mo symmetric=\"true\"  stretchy=\"true\"  maxsize=\"600%\" minsize=\"400%\">&Integral;</mo></mtd></mtr></mtable>");
}
| BIGGINT {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy_string("<mtable columnalign=\"center\" rowspacing=\"0.5ex\"><mtr><mtd><mo symmetric=\"true\"  stretchy=\"true\"  maxsize=\"400%\" minsize=\"30%\">&Integral;</mo></mtd></mtr></mtable>");
}
| BIGINT {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy_string("<mtable columnalign=\"center\" rowspacing=\"0.5ex\"><mtr><mtd><mo symmetric=\"true\"  stretchy=\"true\"  maxsize=\"300%\" minsize=\"200%\">&Integral;</mo></mtd></mtr></mtable>");
}
| LARGERINT {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy_string("<mtable columnalign=\"center\" rowspacing=\"0.5ex\"><mtr><mtd><mo symmetric=\"true\"  stretchy=\"true\"  maxsize=\"260%\" minsize=\"160%\">&Integral;</mo></mtd></mtr></mtable>");
};

bigdelim: BIG LEFTDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"1.2em\" minsize=\"1.2em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BIG RIGHTDELIM {
  $$ = wims_mathml_copy3("<mo maxsize=\"1.2em\" minsize=\"1.2em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BIG OTHERDELIM {
  $$ = wims_mathml_copy3("<mo maxsize=\"1.2em\" minsize=\"1.2em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BBIG LEFTDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"1.8em\" minsize=\"1.8em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BBIG RIGHTDELIM {
  $$ = wims_mathml_copy3("<mo maxsize=\"1.8em\" minsize=\"1.8em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BBIG OTHERDELIM {
  $$ = wims_mathml_copy3("<mo maxsize=\"1.8em\" minsize=\"1.8em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BIGG LEFTDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"2.4em\" minsize=\"2.4em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BIGG RIGHTDELIM {
  $$ = wims_mathml_copy3("<mo maxsize=\"2.4em\" minsize=\"2.4em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BIGG OTHERDELIM {
  $$ = wims_mathml_copy3("<mo maxsize=\"2.4em\" minsize=\"2.4em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BBIGG LEFTDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"3em\" minsize=\"3em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BBIGG RIGHTDELIM {
  $$ = wims_mathml_copy3("<mo maxsize=\"3em\" minsize=\"3em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BBIGG OTHERDELIM {
  $$ = wims_mathml_copy3("<mo maxsize=\"3em\" minsize=\"3em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
|BIGL LEFTDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"1.2em\" minsize=\"1.2em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BIGL OTHERDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"1.2em\" minsize=\"1.2em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BBIGL LEFTDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"1.8em\" minsize=\"1.8em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BBIGL OTHERDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"1.8em\" minsize=\"1.8em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BIGGL LEFTDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"2.4em\" minsize=\"2.4em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BIGGL OTHERDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"2.4em\" minsize=\"2.4em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BBIGGL LEFTDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"3em\" minsize=\"3em\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| BBIGGL OTHERDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo maxsize=\"3em\" minsize=\"3em\">", $2, "</mo>");
  wims_mathml_free_string($2);
};

unrecognized: UNKNOWNCHAR {
  $$ = wims_mathml_copy_string("<merror><mtext>Unknown character</mtext></merror>");
};

unaryminus: UNARYMINUS {
  $$ = wims_mathml_copy_string("<mo lspace=\"verythinmathspace\" rspace=\"0em\">&minus;</mo>");
};

unaryplus: UNARYPLUS {
  $$ = wims_mathml_copy_string("<mo lspace=\"verythinmathspace\" rspace=\"0em\">+</mo>");
};

mi: MI;

mib: MIB {
  wims_mathml_rowposn=2;
  $$ = wims_mathml_copy3("<mi>", $1, "</mi>");
  wims_mathml_free_string($1);
};

mn: MN
| ITEXNUM TEXTSTRING {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy_string($2);
  wims_mathml_free_string($2);
};

mob: MOB {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo lspace=\"thinmathspace\" rspace=\"thinmathspace\">", $1, "</mo>");
  wims_mathml_free_string($1);
};

mo: mob
| bigdelim
| MO {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo>", $1, "</mo>");
  wims_mathml_free_string($1);
}
| MOL {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo>", $1, "</mo>");
  wims_mathml_free_string($1);
}
| MOLL {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mstyle scriptlevel=\"0\"><mo>", $1, "</mo></mstyle>");
  wims_mathml_free_string($1);
}
| RIGHTDELIM {
  $$ = wims_mathml_copy3("<mo stretchy=\"false\">", $1, "</mo>");
  wims_mathml_free_string($1);
}
| LEFTDELIM {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo stretchy=\"false\">", $1, "</mo>");
  wims_mathml_free_string($1);
}
| OTHERDELIM {
  $$ = wims_mathml_copy3("<mo stretchy=\"false\">", $1, "</mo>");
  wims_mathml_free_string($1);
}
| MOF {
  $$ = wims_mathml_copy3("<mo stretchy=\"false\">", $1, "</mo>");
  wims_mathml_free_string($1);
}
| PERIODDELIM {
  $$ = wims_mathml_copy3("<mo>", $1, "</mo>");
  wims_mathml_free_string($1);
}
| MOS {
  wims_mathml_rowposn=2;
  $$ = wims_mathml_copy3("<mo lspace=\"mediummathspace\" rspace=\"mediummathspace\">", $1, "</mo>");
  wims_mathml_free_string($1);
}
| MOP {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo lspace=\"0em\" rspace=\"thinmathspace\">", $1, "</mo>");
  wims_mathml_free_string($1);
}
| MOR {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo lspace=\"verythinmathspace\">", $1, "</mo>");
  wims_mathml_free_string($1);
}
| OPERATORNAME TEXTSTRING {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo lspace=\"0em\" rspace=\"thinmathspace\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| MATHOP TEXTSTRING {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo lspace=\"thinmathspace\" rspace=\"thinmathspace\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| MATHBIN TEXTSTRING {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo lspace=\"mediummathspace\" rspace=\"mediummathspace\">", $2, "</mo>");
  wims_mathml_free_string($2);
}
| MATHREL TEXTSTRING {
  wims_mathml_rowposn = 2;
  $$ = wims_mathml_copy3("<mo lspace=\"thickmathspace\" rspace=\"thickmathspace\">", $2, "</mo>");
  wims_mathml_free_string($2);
};

space: SPACE ST INTONE END ST INTTWO END ST INTTHREE END {
  char * s1 = wims_mathml_copy3("<mspace height=\"", $3, "ex\" depth=\"");
  char * s2 = wims_mathml_copy3($6, "ex\" width=\"", $9);
  $$ = wims_mathml_copy3(s1, s2, "em\"></mspace>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string($3);
  wims_mathml_free_string($6);
  wims_mathml_free_string($9);
};

statusline: STATLINE TEXTSTRING closedTerm {
  char * s1 = wims_mathml_copy3("<maction actiontype=\"statusline\">", $3, "<mtext>");
  $$ = wims_mathml_copy3(s1, $2, "</mtext></maction>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

tooltip: TOOLTIP TEXTSTRING closedTerm {
  char * s1 = wims_mathml_copy3("<maction actiontype=\"tooltip\">", $3, "<mtext>");
  $$ = wims_mathml_copy3(s1, $2, "</mtext></maction>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

toggle: TOGGLE closedTerm closedTerm {
  char * s1 = wims_mathml_copy3("<maction actiontype=\"toggle\" selection=\"2\">", $2, " ");
  $$ = wims_mathml_copy3(s1, $3, "</maction>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

fghighlight: FGHIGHLIGHT ATTRLIST closedTerm {
  char * s1 = wims_mathml_copy3("<maction actiontype=\"highlight\" other=\"color=", $2, "\">");
  $$ = wims_mathml_copy3(s1, $3, "</maction>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

bghighlight: BGHIGHLIGHT ATTRLIST closedTerm {
  char * s1 = wims_mathml_copy3("<maction actiontype=\"highlight\" other=\"background=", $2, "\">");
  $$ = wims_mathml_copy3(s1, $3, "</maction>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

color: COLOR ATTRLIST compoundTermList {
  char * s1 = wims_mathml_copy3("<mstyle mathcolor=", $2, ">");
  $$ = wims_mathml_copy3(s1, $3, "</mstyle>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
}
| BGCOLOR ATTRLIST compoundTermList {
  char * s1 = wims_mathml_copy3("<mstyle mathbackground=", $2, ">");
  $$ = wims_mathml_copy3(s1, $3, "</mstyle>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};


input: INPUT {
  int len = strlen(yylval);
  char size[len];
  char id[len];
  char style[len];
  char value[len];
  char readonly[len];
  memset(size,'\0',len);
  memset(id,'\0',len);
  memset(value,'\0',len);
  memset(style,'\0',len);
  memset(readonly,'\0',len);
  int found_left = 0;
  int found_right = 0;
  int cnt=0;
  char tmp[2];
  int i;
  for(i = 0 ; i < len ; i++){
    if( yylval[i] == '{' ){
      found_left=1;found_right=0;
    } else {
      if( yylval[i] == '}' ){
        cnt++;found_left=0;found_right=1;
      } else {
        if(found_left == 1 && found_right == 0){
          tmp[0] = yylval[i];
          tmp[1] = '\0';
          switch (cnt){
            case 0 : strncat(size,tmp,1);break;
            case 1 : strncat(id,tmp,1);break;
            case 2 : strncat(style,tmp,1);break;
            case 3 : strncat(value,tmp,1);break;
            case 4 : strncat(readonly,tmp,1);break;
            default : break;
          }
        }
      }
    }
  }
  char * s1 = "<semantics><annotation-xml encoding=\"application/xhtml+xml\"><textarea xmlns=\"http://www.w3.org/1999/xhtml\"  onkeydown=\"return (event.keyCode!=13);\"";
  $1 = wims_mathml_copy_string(id);
  s1 = wims_mathml_copy5(s1, " rows=\"1\" id=\"mathml",$1,"\"",0);
  $1 = wims_mathml_copy_string(size);
  s1 = wims_mathml_copy3(s1," style=\"width:",$1);
  $1 = wims_mathml_copy_string(style);
  s1 = wims_mathml_copy5(s1,"em;",$1,"\"",0);
  if( strstr(readonly,"1") != NULL){
    s1 = wims_mathml_copy2(s1," readonly=\"readonly\">");
  } else {
    s1 = wims_mathml_copy2(s1,">");
  }
  $1 = wims_mathml_copy_string(value);
  s1 = wims_mathml_copy3(s1,$1,"</textarea></annotation-xml></semantics>");
  $$ = wims_mathml_copy_string(s1);
  wims_mathml_free_string(s1);
  wims_mathml_free_string($1);
};

lewis: LEWIS closedTerm closedTerm closedTerm closedTerm closedTerm closedTerm closedTerm closedTerm closedTerm {
  char *s1 = wims_mathml_copy13("<mtable columnspacing=\"0.3ex\" columnalign=\"left\" columnlines=\"none\" rowlines=\"none\" rowalign=\"center\" displaystyle=\"true\"><mtr><mtd><mstyle mathsize=\"70%\"><munderover><mrow mathsize=\"110%\">",$3,"</mrow><mphantom></mphantom><mrow mathsize=\"110%\">",$4,"</mrow></munderover></mstyle></mtd><mtd><munderover>",$2,"<mrow>",$10,$9,"</mrow><mrow>",$5,$6,"</mrow></munderover></mtd><mtd><mstyle mathsize=\"70%\"><munderover><mrow mathsize=\"110%\">");
  $$ = wims_mathml_copy5 (s1,$8,"</mrow><mphantom></mphantom><mrow mathsize=\"110%\">",$7,
"</mrow></munderover></mstyle></mtd></mtr></mtable>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
  wims_mathml_free_string($4);
  wims_mathml_free_string($5);
  wims_mathml_free_string($6);
  wims_mathml_free_string($7);
  wims_mathml_free_string($8);
  wims_mathml_free_string($9);
  wims_mathml_free_string($10);
};

special: SPECIAL ATTRLIST compoundTermList {
  char * s1 = wims_mathml_copy3("<mstyle mathcolor=", $2, ">");
  $$ = wims_mathml_copy3(s1, $3, "</mstyle>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

mathrlap: RLAP closedTerm {
  $$ = wims_mathml_copy3("<mpadded width=\"0\">", $2, "</mpadded>");
  wims_mathml_free_string($2);
};

mathllap: LLAP closedTerm {
  $$ = wims_mathml_copy3("<mpadded width=\"0\" lspace=\"-100%width\">", $2, "</mpadded>");
  wims_mathml_free_string($2);
};

mathclap: CLAP closedTerm {
  $$ = wims_mathml_copy3("<mpadded width=\"0\" lspace=\"-50%width\">", $2, "</mpadded>");
  wims_mathml_free_string($2);
};

textstring: TEXTBOX TEXTSTRING {
  $$ = wims_mathml_copy3("<mtext>", $2, "</mtext>");
  wims_mathml_free_string($2);
};

displaystyle: DISPLAY compoundTermList {
  $$ = wims_mathml_copy3("<mstyle displaystyle=\"true\">", $2, "</mstyle>");
  wims_mathml_free_string($2);
};

textstyle: TEXTSTY compoundTermList {
  $$ = wims_mathml_copy3("<mstyle displaystyle=\"false\">", $2, "</mstyle>");
  wims_mathml_free_string($2);
};

textsize: TEXTSIZE compoundTermList {
  $$ = wims_mathml_copy3("<mstyle scriptlevel=\"0\">", $2, "</mstyle>");
  wims_mathml_free_string($2);
};

scriptsize: SCSIZE compoundTermList {
  $$ = wims_mathml_copy3("<mstyle scriptlevel=\"1\">", $2, "</mstyle>");
  wims_mathml_free_string($2);
};

scriptscriptsize: SCSCSIZE compoundTermList {
  $$ = wims_mathml_copy3("<mstyle scriptlevel=\"2\">", $2, "</mstyle>");
  wims_mathml_free_string($2);
};

italics: ITALICS closedTerm {
  $$ = wims_mathml_copy3("<mstyle mathvariant=\"italic\">", $2, "</mstyle>");
  wims_mathml_free_string($2);
};


slashed: SLASHED closedTerm {
  $$ = wims_mathml_copy3("<menclose notation=\"updiagonalstrike\">", $2, "</menclose>");
  wims_mathml_free_string($2);
};

boxed: BOXED closedTerm {
  $$ = wims_mathml_copy3("<menclose notation=\"box\">", $2, "</menclose>");
  wims_mathml_free_string($2);
};

bold: BOLD closedTerm {
  $$ = wims_mathml_copy3("<mstyle mathvariant=\"bold\">", $2, "</mstyle>");
  wims_mathml_free_string($2);
};

roman: RM ST rmchars END {
  $$ = wims_mathml_copy3("<mi mathvariant=\"normal\">", $3, "</mi>");
  wims_mathml_free_string($3);
};

roman: WIMSROMAN closedTerm{
  $$ = wims_mathml_copy3("<mi mathvariant=\"normal\">", $2, "</mi>");
  wims_mathml_free_string($2);
};

rmchars: RMCHAR {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| rmchars RMCHAR {
  $$ = wims_mathml_copy2($1, $2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

bbold: BB ST bbchars END {
  $$ = wims_mathml_copy3("<mi>", $3, "</mi>");
  wims_mathml_free_string($3);
};

bbchars: bbchar {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| bbchars bbchar {
  $$ = wims_mathml_copy2($1, $2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

bbchar: BBLOWERCHAR {
  $$ = wims_mathml_copy3("&", $1, "opf;");
  wims_mathml_free_string($1);
}
| BBUPPERCHAR {
  $$ = wims_mathml_copy3("&", $1, "opf;");
  wims_mathml_free_string($1);
}
| BBDIGIT {
  /* Blackboard digits 0-9 correspond to Unicode characters 0x1D7D8-0x1D7E1 */
  char * end = $1 + 1;
  int code = 0x1D7D8 + strtoul($1, &end, 10);
  $$ = wims_mathml_character_reference(code);
  wims_mathml_free_string($1);
};

frak: FRAK ST frakletters END {
  $$ = wims_mathml_copy3("<mi>", $3, "</mi>");
  wims_mathml_free_string($3);
};

frakletters: frakletter {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| frakletters frakletter {
  $$ = wims_mathml_copy2($1, $2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

frakletter: FRAKCHAR {
  $$ = wims_mathml_copy3("&", $1, "fr;");
  wims_mathml_free_string($1);
};

cal: CAL ST calletters END {
  $$ = wims_mathml_copy3("<mi>", $3, "</mi>");
  wims_mathml_free_string($3);
};

calletters: calletter {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| calletters calletter {
  $$ = wims_mathml_copy2($1, $2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

calletter: CALCHAR {
  $$ = wims_mathml_copy3("&",$1,"scr;");
  wims_mathml_free_string($1);
};

thinspace: THINSPACE {
  $$ = wims_mathml_copy_string("<mspace width=\"thinmathspace\"></mspace>");
};

medspace: MEDSPACE {
  $$ = wims_mathml_copy_string("<mspace width=\"mediummathspace\"></mspace>");
};

thickspace: THICKSPACE {
  $$ = wims_mathml_copy_string("<mspace width=\"thickmathspace\"></mspace>");
};

quad: QUAD {
  $$ = wims_mathml_copy_string("<mspace width=\"1em\"></mspace>");
};

qquad: QQUAD {
  $$ = wims_mathml_copy_string("<mspace width=\"2em\"></mspace>");
};

negspace: NEGSPACE {
  $$ = wims_mathml_copy_string("<mspace width=\"-0.1667 em\"></mspace>");
};

phantom: PHANTOM closedTerm {
  $$ = wims_mathml_copy3("<mphantom>", $2, "</mphantom>");
  wims_mathml_free_string($2);
};

href: HREF TEXTSTRING closedTerm {
  char * s1 = wims_mathml_copy3("<mrow href=\"", $2, "\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:type=\"simple\" xlink:href=\"");
  char * s2 = wims_mathml_copy3(s1, $2, "\">");
  $$ = wims_mathml_copy3(s2, $3, "</mrow>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

tensor: TENSOR closedTerm MROWOPEN subsupList MROWCLOSE {
  char * s1 = wims_mathml_copy3("<mmultiscripts>", $2, $4);
  $$ = wims_mathml_copy2(s1, "</mmultiscripts>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($4);
}
| TENSOR closedTerm subsupList {
  char * s1 = wims_mathml_copy3("<mmultiscripts>", $2, $3);
  $$ = wims_mathml_copy2(s1, "</mmultiscripts>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

multi: MULTI MROWOPEN subsupList MROWCLOSE closedTerm MROWOPEN subsupList MROWCLOSE {
  char * s1 = wims_mathml_copy3("<mmultiscripts>", $5, $7);
  char * s2 = wims_mathml_copy3("<mprescripts></mprescripts>", $3, "</mmultiscripts>");
  $$ = wims_mathml_copy2(s1, s2);
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
  wims_mathml_free_string($7);
}
| MULTI MROWOPEN subsupList MROWCLOSE closedTerm EMPTYMROW {
  char * s1 = wims_mathml_copy2("<mmultiscripts>", $5);
  char * s2 = wims_mathml_copy3("<mprescripts></mprescripts>", $3, "</mmultiscripts>");
  $$ = wims_mathml_copy2(s1, s2);
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
}
| MULTI EMPTYMROW closedTerm MROWOPEN subsupList MROWCLOSE {
  char * s1 = wims_mathml_copy3("<mmultiscripts>", $3, $5);
  $$ = wims_mathml_copy2(s1, "</mmultiscripts>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
};

subsupList: subsupTerm {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| subsupList subsupTerm {
  $$ = wims_mathml_copy3($1, " ", $2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

subsupTerm: SUB closedTerm SUP closedTerm {
  $$ = wims_mathml_copy3($2, " ", $4);
  wims_mathml_free_string($2);
  wims_mathml_free_string($4);
}
| SUB closedTerm {
  $$ = wims_mathml_copy2($2, " <none></none>");
  wims_mathml_free_string($2);
}
| SUP closedTerm {
  $$ = wims_mathml_copy2("<none></none> ", $2);
  wims_mathml_free_string($2);
}
| SUB SUP closedTerm {
  $$ = wims_mathml_copy2("<none></none> ", $3);
  wims_mathml_free_string($3);
};

mfrac: DFRAC closedTerm closedTerm {
  char * s1 = wims_mathml_copy3("<mstyle displaystyle=\"true\" fontsize=\"120%\"><mfrac>", $2, $3);
  $$ = wims_mathml_copy2(s1, "</mfrac></mstyle>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
}
| FRAC closedTerm closedTerm {
  char * s1 = wims_mathml_copy3("<mstyle displaystyle=\"true\"><mfrac>", $2, $3);
  $$ = wims_mathml_copy2(s1, "</mfrac></mstyle>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
}
| TFRAC closedTerm closedTerm {
  char * s1 = wims_mathml_copy3("<mstyle displaystyle=\"false\"><mfrac>", $2, $3);
  $$ = wims_mathml_copy2(s1, "</mfrac></mstyle>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
}
| SFRAC closedTerm closedTerm{
  $$ = wims_mathml_copy5("<mstyle scriptlevel=\"1\"><msup><mrow/>",$2,"</msup><msub><mo lspace=\"verythinmathspace\" stretchy=\"true\">&#x2571;</mo>",$3,"</msub></mstyle>");
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};


pmod: PMOD closedTerm {
  $$ = wims_mathml_copy3( "<mo lspace=\"mediummathspace\">(</mo><mo rspace=\"thinmathspace\">mod</mo>", $2, "<mo rspace=\"mediummathspace\">)</mo>");
  wims_mathml_free_string($2);
};

brack:
MROWOPEN compoundTermList BRACK compoundTermList MROWCLOSE {
 char *s1;
 switch( wims_mathml_shortcut ){
  case 0: s1 = wims_mathml_copy3("<mrow><mo>[</mo><mfrac linethickness=\"0\">", $2, $4);
    $$ = wims_mathml_copy2(s1,"</mfrac><mo>]</mo></mrow>");break;
  case 1: s1 = wims_mathml_copy3("<mrow><mo>{</mo><mfrac linethickness=\"0\">", $2, $4);
    $$ = wims_mathml_copy2(s1,"</mfrac><mo>}</mo></mrow>");
          break;
  default: break;
  }
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($4);
};


texover: MROWOPEN compoundTermList BINOM2 compoundTermList MROWCLOSE {
  char * s1 = wims_mathml_copy3("<mrow><mo>(</mo><mfrac linethickness=\"0\">", $2, $4);
  $$ = wims_mathml_copy2(s1,"</mfrac><mo>)</mo></mrow>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($4);
}
| MROWOPEN compoundTermList TEXOVER compoundTermList MROWCLOSE {
  char * s1 = wims_mathml_copy3("<mstyle displaystyle=\"true\"><mfrac><mrow>", $2, "</mrow><mrow>");
  $$ = wims_mathml_copy3(s1, $4, "</mrow></mfrac></mstyle>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($4);
}
| left compoundTermList TEXOVER compoundTermList right {
  char * s1 = wims_mathml_copy3("<mrow>", $1, "<mstyle displaystyle=\"true\"><mfrac><mrow>");
  char * s2 = wims_mathml_copy3($2, "</mrow><mrow>", $4);
  char * s3 = wims_mathml_copy3("</mrow></mfrac></mstyle>", $5, "</mrow>");
  $$ = wims_mathml_copy3(s1, s2, s3);
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string(s3);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($4);
  wims_mathml_free_string($5);
};

texatop: MROWOPEN compoundTermList TEXATOP compoundTermList MROWCLOSE {
  char * s1 = wims_mathml_copy3("<mfrac linethickness=\"0\"><mrow>", $2, "</mrow><mrow>");
  $$ = wims_mathml_copy3(s1, $4, "</mrow></mfrac>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($4);
}
| left compoundTermList TEXATOP compoundTermList right {
  char * s1 = wims_mathml_copy3("<mrow>", $1, "<mfrac linethickness=\"0\"><mrow>");
  char * s2 = wims_mathml_copy3($2, "</mrow><mrow>", $4);
  char * s3 = wims_mathml_copy3("</mrow></mfrac>", $5, "</mrow>");
  $$ = wims_mathml_copy3(s1, s2, s3);
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string(s3);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($4);
  wims_mathml_free_string($5);
};

binom: BINOM closedTerm closedTerm {
  char * s1 = wims_mathml_copy3("<mrow><mo>(</mo><mfrac linethickness=\"0\">", $2, $3);
  $$ = wims_mathml_copy2(s1, "</mfrac><mo>)</mo></mrow>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
}
| TBINOM closedTerm closedTerm {
  char * s1 = wims_mathml_copy3("<mrow><mo>(</mo><mstyle displaystyle=\"false\"><mfrac linethickness=\"0\">", $2, $3);
  $$ = wims_mathml_copy2(s1, "</mfrac></mstyle><mo>)</mo></mrow>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

munderbrace: UNDERBRACE closedTerm {
  $$ = wims_mathml_copy3("<munder>", $2, "<mo>&UnderBrace;</mo></munder>");
  wims_mathml_free_string($2);
};

munderline: UNDERLINE closedTerm {
  $$ = wims_mathml_copy3("<munder>", $2, "<mo>&#x00332;</mo></munder>");
  wims_mathml_free_string($2);
};

moverbrace: OVERBRACE closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo>&OverBrace;</mo></mover>");
  wims_mathml_free_string($2);
};

bar: BAR closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo stretchy=\"false\">&#x000AF;</mo></mover>");
  wims_mathml_free_string($2);
}
| WIDEBAR closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo> &#x000AF; </mo></mover>");
  wims_mathml_free_string($2);
};

vec: VEC closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo stretchy=\"false\">&RightVector;</mo></mover>");
  wims_mathml_free_string($2);
}
| WIDEVEC closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo>&RightVector;</mo></mover>");
  wims_mathml_free_string($2);
};

overarrow: OVERARROW closedTerm {
  $$ = wims_mathml_copy5("<mover>", $2, "<mo stretchy=\"true\">", $1 ,"</mo></mover>");
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

underarrow: UNDERARROW closedTerm {
  $$ = wims_mathml_copy5("<munder>", $2, "<mo stretchy=\"true\">", $1 ,"</mo></munder>");
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

dot: DOT closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo>&dot;</mo></mover>");
  wims_mathml_free_string($2);
};

ddot: DDOT closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo>&Dot;</mo></mover>");
  wims_mathml_free_string($2);
};

dddot: DDDOT closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo>&tdot;</mo></mover>");
  wims_mathml_free_string($2);
};

ddddot: DDDDOT closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo>&DotDot;</mo></mover>");
  wims_mathml_free_string($2);
};

accents: ACCENTS closedTerm {
/*
 see https://en.wikibooks.org/wiki/LaTeX/Special_Characters#Escaped_codes
 jm.evers 29/6/2017
*/
 int pos=1;char *s1 = " ";
 if( strstr($1,"\"") != NULL ){s1 = wims_mathml_copy_string( "&uml;");}
 else
 if( strstr($1,"'") != NULL ){s1 = wims_mathml_copy_string( "&acute;");}
 else
 if( strstr($1,"`") != NULL ){s1 = wims_mathml_copy_string( "&grave;");}
 else
 if( strstr($1,"~") != NULL ){s1 = wims_mathml_copy_string( "&tilde;");}
 else
 if( strstr($1,".") != NULL ){s1 = wims_mathml_copy_string( "&dot;");}
 else
 if( strstr($1,"r") != NULL ){s1 = wims_mathml_copy_string( "&ring;");}
 else
 if( strstr($1,"c") != NULL ){s1 = wims_mathml_copy_string( "&cedil;");pos = 2;}
 else
 if( strstr($1,"=") != NULL ){s1 = wims_mathml_copy_string( "&macr;");}
 else
 if( strstr($1,"k") != NULL ){s1 = wims_mathml_copy_string( "&ogon;");pos = 2;}
 else
 if( strstr($1,"^") != NULL ){s1 = wims_mathml_copy_string( "&#x0005E;");}
 else
 if( strstr($1,"b") != NULL ){s1 = wims_mathml_copy_string( "&macr;");pos = 2;}
 else
 if( strstr($1,"v") != NULL ){s1 = wims_mathml_copy_string( "&#x2c7;");}
 else
 if( strstr($1,"d") != NULL ){s1 = wims_mathml_copy_string( "&dot;");pos = 2;}
 else
 if( strstr($1,"l") != NULL ){s1 = wims_mathml_copy_string( "&#x142;");pos = 3;}
 else
 if( strstr($1,"o") != NULL ){s1 = wims_mathml_copy_string( "&oslash;");pos = 3;}
 else
 if( strstr($1,"O") != NULL ){s1 = wims_mathml_copy_string( "&Oslash;");pos = 3;}
 else
 if( strstr($1,"H") != NULL ){s1 = wims_mathml_copy_string( "&#733;");}
 else
 if( strstr($1,"t") != NULL ){s1 = wims_mathml_copy_string( "&#8256;");}
 else
 if( strstr($1,"u") != NULL ){s1 = wims_mathml_copy_string( "&breve;");}

 if( pos == 1 ){ $$ = wims_mathml_copy5("<mover>",$2,"<mo stretchy=\"false\">",s1,"</mo></mover>");}
 else
 if( pos == 2 ){ $$ = wims_mathml_copy5("<munder>",$2,"<mo stretchy=\"false\">",s1,"</mo></munder>");}
 else
 if( pos == 3 ){ $$ = wims_mathml_copy3("<mo stretchy=\"false\">",s1,"</mo>");}
 wims_mathml_free_string(s1);
 wims_mathml_free_string($2);
}
tilde: TILDE closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo stretchy=\"false\">&tilde;</mo></mover>");
  wims_mathml_free_string($2);
}
| WIDETILDE closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo>&tilde;</mo></mover>");
  wims_mathml_free_string($2);
};

check: CHECK closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo stretchy=\"false\">&#x2c7;</mo></mover>");
  wims_mathml_free_string($2);
}
| WIDECHECK closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo>&#x2c7;</mo></mover>");
  wims_mathml_free_string($2);
};

hat: HAT closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo stretchy=\"false\">&#x5E;</mo></mover>");
  wims_mathml_free_string($2);
}
| WIDEHAT closedTerm {
  $$ = wims_mathml_copy3("<mover>", $2, "<mo>&#x5E;</mo></mover>");
  wims_mathml_free_string($2);
};

msqrt: SQRT closedTerm {
  $$ = wims_mathml_copy3("<msqrt>", $2, "</msqrt>");
  wims_mathml_free_string($2);
};

mroot: SQRT OPTARGOPEN compoundTermList OPTARGCLOSE closedTerm {
  char * s1 = wims_mathml_copy3("<mroot>", $5, $3);
  $$ = wims_mathml_copy2(s1, "</mroot>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
}
| ROOT closedTerm closedTerm {
  char * s1 = wims_mathml_copy3("<mroot>", $3, $2);
  $$ = wims_mathml_copy2(s1, "</mroot>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

raisebox: RAISEBOX TEXTSTRING TEXTSTRING TEXTSTRING closedTerm {
  char * s1 = wims_mathml_copy3("<mpadded voffset=\"", $2, "\" height=\"");
  char * s2 = wims_mathml_copy3(s1, $3, "\" depth=\"");
  char * s3 = wims_mathml_copy3(s2, $4, "\">");
  $$ = wims_mathml_copy3(s3, $5, "</mpadded>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string(s3);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
  wims_mathml_free_string($4);
  wims_mathml_free_string($5);
}
| RAISEBOX NEG TEXTSTRING TEXTSTRING TEXTSTRING closedTerm {
  char * s1 = wims_mathml_copy3("<mpadded voffset=\"-", $3, "\" height=\"");
  char * s2 = wims_mathml_copy3(s1, $4, "\" depth=\"");
  char * s3 = wims_mathml_copy3(s2, $5, "\">");
  $$ = wims_mathml_copy3(s3, $6, "</mpadded>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string(s3);
  wims_mathml_free_string($3);
  wims_mathml_free_string($4);
  wims_mathml_free_string($5);
  wims_mathml_free_string($6);
}
| RAISEBOX TEXTSTRING TEXTSTRING closedTerm {
  char * s1 = wims_mathml_copy3("<mpadded voffset=\"", $2, "\" height=\"");
  char * s2 = wims_mathml_copy3(s1, $3, "\" depth=\"depth\">");
  $$ = wims_mathml_copy3(s2, $4, "</mpadded>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
  wims_mathml_free_string($4);
}
| RAISEBOX NEG TEXTSTRING TEXTSTRING closedTerm {
  char * s1 = wims_mathml_copy3("<mpadded voffset=\"-", $3, "\" height=\"");
  char * s2 = wims_mathml_copy3(s1, $4, "\" depth=\"+");
  char * s3 = wims_mathml_copy3(s2, $3, "\">");
  $$ = wims_mathml_copy3(s3, $5, "</mpadded>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string(s3);
  wims_mathml_free_string($3);
  wims_mathml_free_string($4);
  wims_mathml_free_string($5);
}
| RAISEBOX TEXTSTRING closedTerm {
  char * s1 = wims_mathml_copy3("<mpadded voffset=\"", $2, "\" height=\"+");
  char * s2 = wims_mathml_copy3(s1, $2, "\" depth=\"depth\">");
  $$ = wims_mathml_copy3(s2, $3, "</mpadded>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
}
| RAISEBOX NEG TEXTSTRING closedTerm {
  char * s1 = wims_mathml_copy3("<mpadded voffset=\"-", $3, "\" height=\"0pt\" depth=\"+");
  char * s2 = wims_mathml_copy3(s1, $3, "\">");
  $$ = wims_mathml_copy3(s2, $4, "</mpadded>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string($3);
  wims_mathml_free_string($4);
};

munder: XARROW OPTARGOPEN compoundTermList OPTARGCLOSE EMPTYMROW {
  char * s1 = wims_mathml_copy3("<munder><mo>", $1, "</mo><mrow>");
  $$ = wims_mathml_copy3(s1, $3, "</mrow></munder>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
}
| UNDER closedTerm closedTerm {
  char * s1 = wims_mathml_copy3("<munder>", $3, $2);
  $$ = wims_mathml_copy2(s1, "</munder>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

mover: XARROW closedTerm {
  char * s1 = wims_mathml_copy3("<mover><mo>", $1, "</mo>");
  $$ =  wims_mathml_copy3(s1, $2, "</mover>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
}
| OVER closedTerm closedTerm {
  char * s1 = wims_mathml_copy3("<mover>", $3, $2);
  $$ = wims_mathml_copy2(s1, "</mover>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
};

munderover: XARROW OPTARGOPEN compoundTermList OPTARGCLOSE closedTerm {
  char * s1 = wims_mathml_copy3("<munderover><mo>", $1, "</mo><mrow>");
  char * s2 = wims_mathml_copy3(s1, $3, "</mrow>");
  $$ = wims_mathml_copy3(s2, $5, "</munderover>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
}
| UNDEROVER closedTerm closedTerm closedTerm {
  char * s1 = wims_mathml_copy3("<munderover>", $4, $2);
  $$ = wims_mathml_copy3(s1, $3, "</munderover>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
  wims_mathml_free_string($3);
  wims_mathml_free_string($4);
};

emptymrow: EMPTYMROW {
  $$ = wims_mathml_copy_string("<mrow></mrow>");
};

mathenv: BEGINENV MATRIX tableRowList ENDENV MATRIX {
  $$ = wims_mathml_copy3("<mrow><mtable rowspacing=\"0.5ex\">", $3, "</mtable></mrow>");
  wims_mathml_free_string($3);
}
|  BEGINENV EQUATION tableRowList ENDENV EQUATION {
  $$ = wims_mathml_copy3("<mrow><mtable rowspacing=\"0.8ex\">", $3, "</mtable></mrow>");
  wims_mathml_free_string($3);
}
|  BEGINENV GATHERED tableRowList ENDENV GATHERED {
  $$ = wims_mathml_copy3("<mrow><mtable rowspacing=\"1.0ex\">", $3, "</mtable></mrow>");
  wims_mathml_free_string($3);
}
| BEGINENV PMATRIX tableRowList ENDENV PMATRIX {
  $$ = wims_mathml_copy3("<mrow><mo>(</mo><mrow><mtable rowspacing=\"0.5ex\">", $3, "</mtable></mrow><mo>)</mo></mrow>");
  wims_mathml_free_string($3);
}
| BEGINENV BMATRIX tableRowList ENDENV BMATRIX {
  $$ = wims_mathml_copy3("<mrow><mo>[</mo><mrow><mtable rowspacing=\"0.5ex\">", $3, "</mtable></mrow><mo>]</mo></mrow>");
  wims_mathml_free_string($3);
}
| BEGINENV VMATRIX tableRowList ENDENV VMATRIX {
  $$ = wims_mathml_copy3("<mrow><mo>&VerticalBar;</mo><mrow><mtable rowspacing=\"0.5ex\">", $3, "</mtable></mrow><mo>&VerticalBar;</mo></mrow>");
  wims_mathml_free_string($3);
}
| BEGINENV BBMATRIX tableRowList ENDENV BBMATRIX {
  $$ = wims_mathml_copy3("<mrow><mo>{</mo><mrow><mtable rowspacing=\"0.5ex\">", $3, "</mtable></mrow><mo>}</mo></mrow>");
  wims_mathml_free_string($3);
}
| BEGINENV VVMATRIX tableRowList ENDENV VVMATRIX {
  $$ = wims_mathml_copy3("<mrow><mo>&DoubleVerticalBar;</mo><mrow><mtable rowspacing=\"0.5ex\">", $3, "</mtable></mrow><mo>&DoubleVerticalBar;</mo></mrow>");
  wims_mathml_free_string($3);
}
| BEGINENV SMALLMATRIX tableRowList ENDENV SMALLMATRIX {
  $$ = wims_mathml_copy3("<mstyle scriptlevel=\"2\"><mrow><mtable rowspacing=\"0.5ex\">", $3, "</mtable></mrow></mstyle>");
  wims_mathml_free_string($3);
}
| BEGINENV CASES tableRowList ENDENV CASES {
  $$ = wims_mathml_copy3("<mrow><mo>{</mo><mrow><mtable columnalign=\"left left\">", $3, "</mtable></mrow></mrow>");
  wims_mathml_free_string($3);
}
| BEGINENV ALIGNED tableRowList ENDENV ALIGNED {
  $$ = wims_mathml_copy3("<mrow><mtable columnalign=\"right left right left right left right left right left\" columnspacing=\"0em\">", $3, "</mtable></mrow>");
  wims_mathml_free_string($3);
}
| BEGINENV ARRAY ARRAYALIGN ST TEX_OPTIONS END tableRowList ENDENV ARRAY {
  $$= wims_mathml_copy7("<mtable rowspacing=\"0.5ex\" align=\"", $3 ,"\"", $5 ," >",$7,"</mtable>");
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
  wims_mathml_free_string($7);
}
| BEGINENV ARRAY ST TEX_OPTIONS END tableRowList ENDENV ARRAY {
    $$= wims_mathml_copy5("<mtable rowspacing=\"0.5ex\" ",$4," >", $6 ,"</mtable>");
    wims_mathml_free_string($4);
    wims_mathml_free_string($6);
}
| BEGINENV SVG XMLSTRING ENDSVG {
  $$ = wims_mathml_copy3("<semantics><annotation-xml encoding=\"SVG1.1\">", $3, "</annotation-xml></semantics>");
  wims_mathml_free_string($3);
}
| BEGINENV SVG ENDSVG {
  $$ = wims_mathml_copy_string(" ");
}
| BEGINENV HTML XMLSTRING ENDHTML {
  $$ = wims_mathml_copy3("<semantics><annotation-xml encoding=\"application/xhtml+xml\">", $3, "</annotation-xml></semantics>");
  wims_mathml_free_string($3);
}
| BEGINENV HTML ENDHTML {
  $$ = wims_mathml_copy_string(" ");
};

substack: SUBSTACK MROWOPEN tableRowList MROWCLOSE {
  $$ = wims_mathml_copy3("<mrow><mtable columnalign=\"center\" rowspacing=\"0.5ex\">", $3, "</mtable></mrow>");
  wims_mathml_free_string($3);
};

shortarray: SHORTARRAY MROWOPEN tableRowList MROWCLOSE {
char *s1;char *s2;
/*
0 = \cases{}.
1 = \Bmatrix{}
2 = \pmatrix{}
3 = \vmatrix{}
4 = \Vmatrix{}
5 = \aligned{}
*/
switch(wims_mathml_shortcut){
 case 0: s1 = wims_mathml_copy_string("<mo stretchy=\"true\">{</mo>");s2=wims_mathml_copy_string("<mo></mo>");break;
 case 1: s1 = wims_mathml_copy_string("<mo stretchy=\"true\">{</mo>");s2=wims_mathml_copy_string("<mo stretchy=\"true\">}</mo>");break;
 case 2: s1 = wims_mathml_copy_string("<mo stretchy=\"true\">(</mo>");s2=wims_mathml_copy_string("<mo stretchy=\"true\">)</mo>");break;
 case 3: s1 = wims_mathml_copy_string("<mo stretchy=\"true\">&vert;</mo>");s2=wims_mathml_copy_string("<mo stretchy=\"true\">&vert;</mo>");break;
 case 4: s1 = wims_mathml_copy_string("<mo stretchy=\"true\">&Vert;</mo>");s2=wims_mathml_copy_string("<mo stretchy=\"true\">&Vert;</mo>");break;
 case 5: s1 = wims_mathml_copy_string(" ");s2=wims_mathml_copy_string(" ");break;
 default: s1 = wims_mathml_copy_string(" ");s2 = wims_mathml_copy_string(" "); break;
}
  $$ = wims_mathml_copy5( s1 ,"<mrow><mtable>", $3, "</mtable></mrow>", s2);
  wims_mathml_free_string($3);
  wims_mathml_free_string(s1);
  wims_mathml_free_string(s2);
};


array: ARRAY MROWOPEN tableRowList MROWCLOSE {
  $$ = wims_mathml_copy3("<mrow><mtable>", $3, "</mtable></mrow>");
  wims_mathml_free_string($3);
}
| ARRAY MROWOPEN ARRAYOPTS MROWOPEN arrayopts MROWCLOSE tableRowList MROWCLOSE {
  char * s1 = wims_mathml_copy3("<mrow><mtable ", $5, ">");
  $$ = wims_mathml_copy3(s1, $7, "</mtable></mrow>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($5);
  wims_mathml_free_string($7);
};

arrayopts: anarrayopt {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| arrayopts anarrayopt {
  $$ = wims_mathml_copy3($1, " ", $2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

anarrayopt: collayout {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| colalign {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| rowalign {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| align {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| eqrows {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| eqcols {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| rowlines {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| collines {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| frame {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| padding {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
};

collayout: COLLAYOUT ATTRLIST {
  $$ = wims_mathml_copy2("columnalign=", $2);
  wims_mathml_free_string($2);
};

colalign: COLALIGN ATTRLIST {
  $$ = wims_mathml_copy2("columnalign=", $2);
  wims_mathml_free_string($2);
};

rowalign: ROWALIGN ATTRLIST {
  $$ = wims_mathml_copy2("rowalign=", $2);
  wims_mathml_free_string($2);
};

align: ALIGN ATTRLIST {
  $$ = wims_mathml_copy2("align=", $2);
  wims_mathml_free_string($2);
};

eqrows: EQROWS ATTRLIST {
  $$ = wims_mathml_copy2("equalrows=", $2);
  wims_mathml_free_string($2);
};

eqcols: EQCOLS ATTRLIST {
  $$ = wims_mathml_copy2("equalcolumns=", $2);
  wims_mathml_free_string($2);
};

rowlines: ROWLINES ATTRLIST {
  $$ = wims_mathml_copy2("rowlines=", $2);
  wims_mathml_free_string($2);
};

collines: COLLINES ATTRLIST {
  $$ = wims_mathml_copy2("columnlines=", $2);
  wims_mathml_free_string($2);
};

frame: FRAME ATTRLIST {
  $$ = wims_mathml_copy2("frame=", $2);
  wims_mathml_free_string($2);
};

padding: PADDING ATTRLIST {
  char * s1 = wims_mathml_copy3("rowspacing=", $2, " columnspacing=");
  $$ = wims_mathml_copy2(s1, $2);
  wims_mathml_free_string(s1);
  wims_mathml_free_string($2);
};

tableRowList: tableRow {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| tableRowList ROWSEP tableRow {
  $$ = wims_mathml_copy3($1, " ", $3);
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
};

tableRow: simpleTableRow {
  $$ = wims_mathml_copy3("<mtr>", $1, "</mtr>");
  wims_mathml_free_string($1);
}
| optsTableRow {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
};

simpleTableRow: tableCell {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| simpleTableRow COLSEP tableCell {
  $$ = wims_mathml_copy3($1, " ", $3);
  wims_mathml_free_string($1);
  wims_mathml_free_string($3);
};

optsTableRow: ROWOPTS MROWOPEN rowopts MROWCLOSE simpleTableRow {
  char * s1 = wims_mathml_copy3("<mtr ", $3, ">");
  $$ = wims_mathml_copy3(s1, $5, "</mtr>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
};

rowopts: arowopt {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| rowopts arowopt {
  $$ = wims_mathml_copy3($1, " ", $2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

arowopt: colalign {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| rowalign {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
};

tableCell:   {
  $$ = wims_mathml_copy_string("<mtd></mtd>");
}
| compoundTermList {
  $$ = wims_mathml_copy3("<mtd>", $1, "</mtd>");
  wims_mathml_free_string($1);
}
| CELLOPTS MROWOPEN cellopts MROWCLOSE compoundTermList {
  char * s1 = wims_mathml_copy3("<mtd ", $3, ">");
  $$ = wims_mathml_copy3(s1, $5, "</mtd>");
  wims_mathml_free_string(s1);
  wims_mathml_free_string($3);
  wims_mathml_free_string($5);
};

cellopts: acellopt {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| cellopts acellopt {
  $$ = wims_mathml_copy3($1, " ", $2);
  wims_mathml_free_string($1);
  wims_mathml_free_string($2);
};

acellopt: colalign {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| rowalign {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| rowspan {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
}
| colspan {
  $$ = wims_mathml_copy_string($1);
  wims_mathml_free_string($1);
};

rowspan: ROWSPAN ATTRLIST {
  $$ = wims_mathml_copy2("rowspan=", $2);
  wims_mathml_free_string($2);
};

colspan: COLSPAN ATTRLIST {
  $$ = wims_mathml_copy2("columnspan=", $2);
  wims_mathml_free_string($2);
};

%%

char * wims_mathml_parse (const char * buffer, unsigned long length)
{
  char * mathml = 0;

  int result;

  wims_mathml_setup (buffer, length);
  wims_mathml_restart ();

  result = wims_mathml_yyparse (&mathml);

  if (result && mathml) /* shouldn't happen? */
    {
      wims_mathml_free_string (mathml);
      mathml = 0;
    }
  return mathml;
}

int wims_mathml_filter (const char * buffer, unsigned long length)
{
  wims_mathml_setup (buffer, length);
  wims_mathml_restart ();

  return wims_mathml_yyparse (0);
}

#define ITEX_DELIMITER_DOLLAR 0
#define ITEX_DELIMITER_DOUBLE 1
#define ITEX_DELIMITER_SQUARE 2

static char * wims_mathml_last_error = 0;

static void wims_mathml_keep_error (const char * msg)
{
  if (wims_mathml_last_error)
    {
      wims_mathml_free_string (wims_mathml_last_error);
      wims_mathml_last_error = 0;
    }
  wims_mathml_last_error = wims_mathml_copy_escaped (msg);
}

int wims_mathml_html_filter (const char * buffer, unsigned long length)
{
 return wims_mathml_do_html_filter (buffer, length, 0);
}

int wims_mathml_strict_html_filter (const char * buffer, unsigned long length)
{
  return wims_mathml_do_html_filter (buffer, length, 1);
}

int wims_mathml_do_html_filter (const char * buffer, unsigned long length, const int forbid_markup)
{
  int result = 0;

  int type = 0;
  int skip = 0;
  int match = 0;

  const char * ptr1 = buffer;
  const char * ptr2 = 0;

  const char * end = buffer + length;

  char * mathml = 0;

  void (*save_error_fn) (const char * msg) = wims_mathml_error;

  wims_mathml_error = wims_mathml_keep_error;

  _until_math:
  ptr2 = ptr1;

  while (ptr2 < end)
  {
    if (*ptr2 == '$') break;
    if ((*ptr2 == '\\') && (ptr2 + 1 < end))
    {
      if (*(ptr2+1) == '[') break;
    }
    ++ptr2;
  }
  if (wims_mathml_write && ptr2 > ptr1)
    (*wims_mathml_write) (ptr1, ptr2 - ptr1);

  if (ptr2 == end) goto _finish;

  _until_html:
  ptr1 = ptr2;

  if (ptr2 + 1 < end)
  {
    if ((*ptr2 == '\\') && (*(ptr2+1) == '['))
    {
      type = ITEX_DELIMITER_SQUARE;
      ptr2 += 2;
    }
    else if ((*ptr2 == '$') && (*(ptr2+1) == '$'))
    {
      type = ITEX_DELIMITER_DOUBLE;
      ptr2 += 2;
    }
    else
    {
      type = ITEX_DELIMITER_DOLLAR;
      ptr2 += 2;
    }
  }
  else goto _finish;

  skip = 0;
  match = 0;

  while (ptr2 < end)
  {
    switch (*ptr2)
    {
    case '<':
    case '>':
      if (forbid_markup == 1) skip = 1;
      break;

    case '\\':
      if (ptr2 + 1 < end)
      {
        if (*(ptr2 + 1) == '[')
        {
          skip = 1;
        }
        else if (*(ptr2 + 1) == ']')
        {
          if (type == ITEX_DELIMITER_SQUARE)
          {
            ptr2 += 2;
            match = 1;
          }
          else
          {
            skip = 1;
          }
        }
      }
      break;

    case '$':
      if (type == ITEX_DELIMITER_SQUARE)
      {
        skip = 1;
      }
      else if (ptr2 + 1 < end)
      {
        if (*(ptr2 + 1) == '$')
        {
          if (type == ITEX_DELIMITER_DOLLAR)
          {
            ptr2++;
            match = 1;
          }
          else
          {
            ptr2 += 2;
            match = 1;
          }
        }
        else
        {
          if (type == ITEX_DELIMITER_DOLLAR)
          {
            ptr2++;
            match = 1;
          }
          else
          {
            skip = 1;
          }
        }
      }
      else
      {
        if (type == ITEX_DELIMITER_DOLLAR)
        {
          ptr2++;
          match = 1;
        }
        else
        {
          skip = 1;
        }
      }
      break;

    default:
      break;
    }
    if (skip || match) break;

    ++ptr2;
  }
  if (skip)
  {
    if (type == ITEX_DELIMITER_DOLLAR)
    {
      if (wims_mathml_write)
        (*wims_mathml_write) (ptr1, 1);
      ptr1++;
    }
    else
    {
      if (wims_mathml_write)
        (*wims_mathml_write) (ptr1, 2);
      ptr1 += 2;
    }
    goto _until_math;
  }
  if (match)
  {
    mathml = wims_mathml_parse (ptr1, ptr2 - ptr1);
    if (mathml)
    {
      if (wims_mathml_write_mathml)
        (*wims_mathml_write_mathml) (mathml);
      wims_mathml_free_string (mathml);
      mathml = 0;
    }
    else
    {
      ++result;
      if (wims_mathml_write)
      {
        if (type == ITEX_DELIMITER_DOLLAR)
          (*wims_mathml_write) ("<span style=\"font-size:1em;\"><math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"inline\"><merror><mtext>", 0);
        else
          (*wims_mathml_write) ("<span style=\"font-size:1em;\"><math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"block\"><merror><mtext>", 0);
          (*wims_mathml_write) (wims_mathml_last_error, 0);
          (*wims_mathml_write) ("</mtext></merror></math></span>", 0);
        }
    }
    ptr1 = ptr2;

    goto _until_math;
  }
  if (wims_mathml_write)
    (*wims_mathml_write) (ptr1, ptr2 - ptr1);

  _finish:
  if (wims_mathml_last_error)
  {
    wims_mathml_free_string (wims_mathml_last_error);
    wims_mathml_last_error = 0;
  }
  wims_mathml_error = save_error_fn;

  return result;
}


void replace_str(const char *str, const char *old, const char *new){
/* http://creativeandcritical.net/str-replace-c/ */
    char *ret, *r;
    const char *p, *q;
    size_t oldlen = strlen(old);
    size_t count, retlen, newlen = strlen(new);

    if (oldlen != newlen){
  for (count = 0, p = str; (q = strstr(p, old)) != NULL; p = q + oldlen){
      count++;
      retlen = p - str + strlen(p) + count * (newlen - oldlen);
  }
    }
    else
    {
  retlen = strlen(str);
    }

    if ((ret = malloc(retlen + 1)) == NULL){
  ret = NULL;
    }
    else
    {
  for (r = ret, p = str; (q = strstr(p, old)) != NULL; p = q + oldlen) {
      size_t l = q - p;
      memcpy(r, p, l);
      r += l;
      memcpy(r, new, newlen);
      r += newlen;
  }
  strcpy(r, p);
    }
    yylval = ret;
}
