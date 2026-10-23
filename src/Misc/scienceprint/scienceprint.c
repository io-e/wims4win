/*
*********************************************************************************
* J.M. Evers 3/2012                                                             *
* This is all -very- amateur scriblings... So no copyrights.                    *
* This source code file, and compiled objects derived from it,                  *
* can be used and distributed without restriction, including for commercial use *
* No warrenty whatsoever                                                        *
*********************************************************************************

WIMS usage:
sci_num = !exec scienceprint number,significance,type

number: a number (like 12345 123.45*10^2 123.45E+02)
significance : desired precision (if significance= -1 : "science notation" in name amount of digits)
type (optional args): calc = 0 / html = 1 / latex = 2  / prefix = 3  / mathml = 4

default  : calc   notation : 120000,3   -> 1.20*10^5
type = 0 : calc   notation : 120000,3,0 -> 1.20*10^5
type = 1 : html notation   : 120000,3,1 -> 1.20&times;10<sup>5</sup>
type = 2 : tex notation  : 120000,3,2 -> 1.20 \times 10^{5}
type = 3 : prefix-notation : 120000,3,3 -> 120.0 k
type = 3 : if -24 > prefix > 24         -> type = 1 (html)
type = 4 : MathML notation or KaTeX (e.g. tex notation in 'span' element with: class='wims_katex' zoom enabled)
type = 5 : prefix-notation with words (nano,mega,giga...etc) :120000,3,3 -> 120.0 kilo (same as 3)

multiple conversion: use 'space' between arguments
scienceprint 120000,4 122900,5 120036,6,3 --> 120.0*10^3,122.90*10^3,120.036 k


*********************************************************************************
CHANGELOG
19/2/2021
added compile time support for KaTeX installation (in case 'type = 4': MathML --> KaTeX span element)

16/10/2013
corrected roundoff in case "significance=-1" changed 'factor' from 'int' to 'float'...there must be a better way to do this...
But in numbers > 1000000000000000 there is still a problem, so take care (!)
when there are more than 15 digits in the number (this may vary depending on system / implementations, I guess)
./scienceprint 901234567890123       ,-1,0 --> 9.01234567890123*10^14
./scienceprint 9012345678901234      ,-1,0 --> 9.012345678901236*10^15
./scienceprint 901234567890123*10^70 ,-1,0 --> 9.01234567890123*10^84
./scienceprint 9012345678901234*10^70,-1,0 --> 9.012345678901227*10^85
*********************************************************************************
28/9/2013
minor issue:
Added type = 5 : prefix-notation with words (nano,mega,giga...etc)
small correction in roundoff routine when significance > 6 .... pow(10,7) may give problems when stored in (int) integer
*********************************************************************************
27/9/2013
Correct rounding in stead of truncation...
*********************************************************************************
18/10/2012 :
Added Mathml output
Added option significance=-1
To be used when there is no significance known ; just tries to print the number in science notation
Using the original amount of digits used in "number" argument
!exec scienceprint 123.445000e+23,-1 --> 1.23445000*10^25
*********************************************************************************
12/11/2012
Added support for numbers like  12345*10^12
12345*10^12 --> 12345E12 ---> 1.2345*10^16
*********************************************************************************
20/6/2012
Corrected significance flaw when using prefixes
Simplified routines
24  yotta       Y
21  zetta       Z
18  exa         E
15  peta        P
12  tera        T
9   giga        G
6   mega        M
3   kilo        k
2   hecto       h
1   deca        da
-1  deci        d
-2  centi       c
-3  milli       m
-6  micro       µ
-9  nano        n
-12 pico        p
-15 femto       f
-18 atto        a
-21 zepto       z
-24 yocto       y
*/
#include <stdio.h>
#include <math.h>
#include <string.h>
#include <stdlib.h>
#ifdef KATEX_INSTALLED
/* generate timebased id */
#include <sys/time.h> 
#endif
/*#define MICRO "µ"*/
#define MICRO "\xB5"
#define MAX_CONV 256
#define MAX_STRING 32
#define PREFIX_START -24
#define PREFIX_END 24


char *str_replace ( const char *word, const char *sub_word, const  char *rep_word ){
    if(strlen(word) > MAX_STRING){return NULL;}
    char *part_word = NULL;
    char *new_word = NULL;
    char *old_word = NULL;
    char *head = NULL;
    /* if either sub_word or rep_word is NULL, duplicate word a let caller handle it */
    if ( sub_word == NULL || rep_word == NULL ) return strdup(word);
    new_word = strdup (word);
    head = new_word;
    while ( (part_word = strstr ( head, sub_word ))){
        old_word = new_word;
        new_word = malloc ( strlen ( old_word ) - strlen ( sub_word ) + strlen ( rep_word ) + 1 );
        /*failed to alloc mem, free old word and return NULL */
        if ( new_word == NULL ){
          free (old_word);return NULL;
        }
        memcpy ( new_word, old_word, part_word - old_word );
        memcpy ( new_word + (part_word - old_word), rep_word, strlen ( rep_word ) );
        memcpy ( new_word + (part_word - old_word) + strlen( rep_word ), part_word + strlen ( sub_word ), strlen ( old_word ) - strlen ( sub_word ) - ( part_word - old_word ) );
        memset ( new_word + strlen ( old_word ) - strlen ( sub_word ) + strlen ( rep_word ) , 0, 1 );
        /* move back head right after the last replacement */
        head = new_word + (part_word - old_word) + strlen( rep_word );
        free (old_word);
    }
    return new_word;
}

char *printscience(double value, int sig, int format , int cnt ,int size){
#ifdef KATEX_INSTALLED
/* generate timebased id */
    struct timeval tv;struct timezone tz;gettimeofday(&tv, &tz);
    unsigned int id = (unsigned int) tv.tv_usec;
#endif
    static char *min[] = {"","m",MICRO,"n","p","f","a","z","y"};
    static char *plus[] = {"","k", "M", "G", "T", "P", "E", "Z", "Y" };
    static char *min_word[] = {"","milli","micro","nano","pico","femto","atto","zepto","yocto"};
    static char *plus_word[] = {"","kilo", "mega", "giga", "tera", "peta", "exa", "zetta", "yotta" };
    char *sign = NULL;char *prefix = NULL;float pm;double factor;
    int exponent10 = 0;
    int use_word = 0;if(format == 5){format = 3; use_word = 1;} /* switch to using words in stead of prefix  */
    if(value < 0.0) {pm = -0.5; sign = "-";value = -value;} else {sign = ""; pm = 0.5;}
    if( sig == -1 ){
     /*
     no significance truncations...just science notation 1234 -> 1.234*10^3
     try (!) to use same amount of digits
     */
        sig = size;
        if(format == 3){format = 1;} /* never prefix --> html notation */
    }
    if(value == 0){fprintf(stdout, "%s%.*f", sign, sig-1, value);return NULL;} /* no need to go further */
    if(value>1){
        while(value >= 10){
            value=value / 10.0;
            exponent10++;
            /* need to set a limit to number of while loops ! */
            if(exponent10 > 100){fprintf(stdout,"error : number too big (exponent > 100)\n");return 0;}
        }
    }
    else /* 0 < value < 1 --> exponent10 < 0 */
    {
        while(value < 1){
            value=value*10;
            exponent10--;
            /* need to set a limit to number of while loops ! */
            if(exponent10 <-100){fprintf(stdout,"error : number too small (exponent < -100)\n");return 0;}
        }
    }
    /* 27/9/2013 avoid truncating and do rounding...very expensive */
    factor = pow(10,sig+1);
    value = (round(factor*value + (pm) ))/factor; /* pm = +/- 0.5 */
    if(format == 3 && ((exponent10 < PREFIX_START) || (exponent10 > PREFIX_END))){
        format = 1; /* not in my list of prefixes ; print in html ! */
    }
    sig = sig - 1; /* "%.*f" counts the "." */
    if(cnt > 1){fprintf(stdout,",");}/* more than one conversion to do : make list */
    int idx=0;int exp=0;
    if(exponent10 == 0){format = 6;} /* no need for 2*10^0 */
    if(sig < 0){sig = 0;} /* better be safe than sorry... */
    switch(format){
        case 0: fprintf(stdout, "%s%.*f*10^%d", sign, sig, value, exponent10);break;
        case 1: fprintf(stdout, "%s%.*f&times;10<sup>%d</sup>", sign, sig, value, exponent10);break;
        case 2: fprintf(stdout, "%s%.*f \\times 10^{%d}", sign, sig, value, exponent10);break;
        case 3:
/*
1,1,3 -> 1
10,1,3 -> 1*10^-2 k
100,1,3 -> 1*10^-1 k
1000,1,3 -> 1 k
10000,1,3 -> 1*10^1 k
100000,1,3 -> 1*10^2 k
1000000,1,3 -> 1 M
10000000,1,3 -> 1*10^1 M
100000000,1,3 -> 1*10^2 M
1000000000,1,3 -> 1 G
1,1,3 -> 1
0.1,1,3 -> 1*10^-1
0.01,1,3 -> 1*10^-2
0.001,1,3 -> 1 m
0.0001,1,3 -> 1*10^-1 m
0.00001,1,3 -> 1*10^-2 m
0.000001,1,3 -> 1 µ
0.0000001,1,3-> 1*10^-1 µ
0.00000001,1,3-> 1*10^-2 µ
0.000000001,1,3-> 1 n
*/
        exp = exponent10%3;
        idx = round(exponent10/3);
        if( exponent10 > 0  ){
            if(use_word == 0 ){ prefix = plus[idx]; } else { prefix = plus_word[idx]; }
        }
        else
        {
            if(use_word == 0){ prefix = min[-1*idx]; } else { prefix = min_word[-1*idx]; }
        }
        if( exp == 0){
            fprintf(stdout, "%s%.*f %s",sign, sig, value,prefix);
        }
        else
        {
            fprintf(stdout, "%s%.*f&times;10<sup>%d</sup> %s", sign, sig, value, exp, prefix);
        }
        break;
#ifdef KATEX_INSTALLED
        case 4: fprintf(stdout, "<span onclick='javascript:wims_mathml_zoom(this.id);' id='wims_katex%d' class='wims_katex' use_display='false'>%s%.*f \\times 10^{%d}</span>",id, sign, sig, value, exponent10);break;
#else
        case 4: fprintf(stdout, "<math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"inline\"><mstyle id=\"wims_mathml\" mathsize=\"110%%\"><mn>%s%.*f</mn><mo>&times;</mo><msup><mn>10</mn><mn>%d</mn></msup></mstyle></math>", sign, sig, value, exponent10);break;
#endif
        case 5: break;
        case 6: fprintf(stdout, "%s%.*f",sign,sig,value);break;
        default: break;
    }
    return NULL;
}

int main( int argc , char *argv[]){

    if( argc < 2){
        fprintf(stdout,"syntax error : number1,significance1,type1 number2,significance2,type2 ... number_n,significance_n,type_n \n");
        return 0;
    }

    double number = 0;
    int significance = 0,type = 0,idx = 0,cnt = 1,size = 0;
    char *input = "\0",*ptr = "\0";

    /* test for illegal characters */
    const char *invalid_characters = "\n\"\'!=ABCDFGHIJKLMNOPQRSTUVWXYZabcdfghijklmnopqrstuvwxyz@#$%&()[]{};:~><?/\\|";
    /* Ee +- are allowed : 12.34e+05  12.34e-08 */

    /* walk through argument 1 to end, and call function scienceprint(a,b,c) */
    input = argv[cnt];
    while( input != NULL ){
        if(cnt > MAX_CONV){fprintf(stdout,"\nerror: number of conversions exceeds limit of %d\n",MAX_CONV);return 0;}
        while (*input){ /* loop through invalid chars. */
            if ( strchr(invalid_characters, *input) ){
                fprintf(stdout,"\nerror : illegal character \"%s\" \n",input);
                return 0;
            }
            input++;
        }
        /* reset input to actual value */
        input = argv[cnt];
        ptr = (char *) strtok(input,",");
        idx = 0;
        type = 0;
        size = 0;
        while( ptr != NULL ){
            switch( idx ){
                case 0:
                        /* size only interesting when 'significance=-1'
                         determine number of digits : 1.23445e+23 -> size = 6
                        */
                        size = strlen(ptr);
                        if( strstr(ptr,".") != NULL){size = size - 1 ;}
                        if( strstr(ptr,"*10^") != NULL){
                            ptr = str_replace(ptr,"*10^","E");
                            if(ptr == NULL){
                                fprintf(stdout,"error : in replacement of 10^ notation\n");
                                return 0;
                            }
                            size = size - 3;
                        }
                        if( strstr(ptr,"E") != NULL){size = size - strlen(strstr(ptr,"E"));}
                        if( strstr(ptr,"e") != NULL){size = size - strlen(strstr(ptr,"e"));}
                        number = atof(ptr);
                        break;
                case 1: significance = atoi(ptr);  break;
                case 2: type = atoi(ptr); if(type < 0 || type > 5 ){type = 0;} break;
                default: break;
            }
            idx++;
            ptr = (char *) strtok(NULL,",");
        }
        /* number and precision are mandatory:  default type=0  */
        if( idx < 2 || idx > 3){fprintf(stdout,"\nsyntax error : number1,significance1,type1 number2,significance2,type2 ... number_n,significance_n,type_n \n");return 0;}
        /* call conversion routine */
        printscience(number, significance, type , cnt , size);
        cnt++;
        input = argv[cnt];
    }
    fprintf(stdout,"\n");
    return 0;
}



