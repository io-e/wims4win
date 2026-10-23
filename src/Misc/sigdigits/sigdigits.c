/*
*********************************************************************************
* J.M. Evers 3/2012								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled objects derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatsoever							*
*********************************************************************************
syntax number_1 number2 number_3 ... number_n

Treats a number as string array : no numerical evaluation !
(pfffff...)

example
shell : ./sigdigits "1.23" "1.230" "1.2300" "1.23e5" "1.23*10^5' "1.2300e5" ".12340000" "01.23*10^5" "1.2.3.4"
wims  : !exec sigdigits 1.23 1.230 1.2300 1.23e5 1.23*10^5 1.2300e5 .12340000 01.23*10^5 1.2.3.4
3,1,2,1,0,1
4,1,3,1,0,1
5,1,4,1,0,1
3,1,2,10,5,1
3,1,2,10,5,1
5,1,4,10,5,1
8,1,8,1,0,1
3,2,2,10,5,0
-1,-1,-1,-1,-1,-1

result is 
1 line per input number
6 items per line:

item 1) real number of significant digits in number_x (eg without leading zeros)

item 2) number of digits left from decimal point or if no decimals: total number of digits (length)

item 3) number of digits right from decimal point

item 4) exponent base (if not present : 1)

item 5) exponent (if not present : 0)

item 6) indication : is the number correctly written ?  1 or 0  ( 000.1 is not correct...)
	scientiffic: [1-9.*] *10^exp : 1.2345*10^5 is OK ... 12.345*10^4 or 0.12345*10^6 is "NOK"
	a*10^b  : 1 <= a <= 9

remarks:
- exponent: any other base will be tolerated : 4*7^5  base=7 exponent=5 -> 1,0,1,0,7,5,1
- if number is 'nonsense' : -1,-1,-1,-1,-1 (1.23.4567  10^1.23.4 ; number will produce NaN in other math software)

ruleset:
120.2		: 4 significant digits
120.2000	: 7 significant digits
0.0000120	: 3 significant digits
.0000120	: 3 significant digits
scientiffic notation:
1.202*10^5	: 4 significant digits
1.20200*10^5	: 6 significant digits


*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#define MAX_DIGITS 64
#define MAX_CONV 64

void append(char* s, char c){
    int len = strlen(s);
    s[len] = c;
    s[len+1] = '\0';
}

int main( int argc , char *argv[]){
    if( argc < 2){
	fprintf(stdout,"syntax error\n");
	exit(0);
    }
    char word[MAX_DIGITS];
    char *input;
    char exp[MAX_DIGITS];
    int cnt,i,ok,length,zeros,sig1,sig2,found_digit,found_point,dec1,pow,found_power,found_multiply,points,base_start,base_end,bracket;
    const char *invalid_characters = "\n\"\',!=ABCDFGHIJKLMNOPQRSTUVWXYZabcdfghijklmnopqrstuvwxyz@#$%&;:~><?/\\|";
    /* Ee +- are allowed : 12.34e+05  12.34e-08  1.234*10^123*/ 
    cnt = 1;
    input = argv[cnt];
    while( input != NULL){
	if(cnt > MAX_CONV){fprintf(stdout,"error : number of conversions exceeds limit of %d\n",MAX_CONV);return 0;}
	length = strlen(input);
        if( length > MAX_DIGITS){
            fprintf(stdout,"error : number is larger than %d digits\n",MAX_DIGITS);
	    return 0;
        }
	/* test for illegal characters */
	while (*input){ 
	    if ( strchr(invalid_characters, *input) ){
		fprintf(stdout,"error : found illegal character in argument \"%s\" \n",input);
		return 0;
	    }
	    input++;
	}
	input = argv[cnt];
	// better way to use strncpy
	strncpy( word, input, MAX_DIGITS - strlen(word)-1);
	// reset
	found_digit = 0;
	found_point = 0;
	found_power = 0;
	bracket = 0;
	found_multiply = 0;
	sig1 = 0; // real significant digits
	sig2 = 0; // integer part [including leading zeros]
	dec1 = 0; // decimal part [including trailing zeros]
	pow = 0; // exponent
	zeros = 0; // leading or trailing zeros
	points = 0; // number of points in number...
	exp[0]='\0';
	base_start = 0;
	base_end = 0;
	ok = 0;
	for( i = length - 1 ; i >= 0 ; i--){ // walk from rightside to left through the 'number'
	    switch( word[i] ){
		case '(' : bracket = 1 ;break; //  10^(-4) -> exp= (-4) : remove bracket from exponent.
		case ')' : bracket = 1 ;break;
		case '{' : bracket = 1 ;break;
		case '}' : bracket = 1 ;break;
		case '[' : bracket = 1 ;break;
		case ']' : bracket = 1 ;break;
		case '^' : base_start = i;found_power++;break;
		case '*' : 
		    found_multiply++;
		    if(found_power == 1){
			base_end = i;
			if(found_point == 1){points--;found_point = 0;}  // point in exponent... 10^4.5 (hmmm)
			pow = length - i;
		        sig1 = 0; // reset counting significant digits and all other stuff
			sig2 = 0;
		        dec1 = 0;
			found_digit = 0;
		        zeros = 0;
		    }
		    break;
		case 'e' : if(found_point == 1){points--;found_point = 0;} found_power++;pow = length - i;sig1 = 0;sig2 = 0;dec1 = 0;found_digit = 0;zeros = 0;found_multiply++;break;
		case 'E' : if(found_point == 1){points--;found_point = 0;} found_power++;pow = length - i;sig1 = 0;sig2 = 0;dec1 = 0;found_digit = 0;zeros = 0;found_multiply++;break;
		case '0' : 
		    if(i == 0){/* last char */ 
			sig1 = sig1 - zeros;
			sig2++;
			if(found_power == 1){zeros++;}
		    }
		    else
		    {		    
			// 1.000*10^5 -> 4 sig
			if( found_point == 0 ){
			    sig1++;
			}
			else
			{
			    sig2++;
			    if( found_digit == 1 ){ sig1++; }
			} 
			zeros++;
		    } 
		    break;
		case '.' : if( i == 0 ){ 
			    sig1 = sig1 - zeros;sig2++; 
			    if(found_power == 1){ zeros++; } 
			   } /* . is last: so assume 0.xxxxx  */
			   dec1 = length - i - pow - 1;
			   found_point = 1;points++; 
			   break;
		case '1' : sig1++;if(found_point == 1){sig2++;} found_digit = 1;zeros = 0; break;
		case '2' : sig1++;if(found_point == 1){sig2++;} found_digit = 1;zeros = 0; break;
		case '3' : sig1++;if(found_point == 1){sig2++;} found_digit = 1;zeros = 0; break;
		case '4' : sig1++;if(found_point == 1){sig2++;} found_digit = 1;zeros = 0; break;
		case '5' : sig1++;if(found_point == 1){sig2++;} found_digit = 1;zeros = 0; break;
		case '6' : sig1++;if(found_point == 1){sig2++;} found_digit = 1;zeros = 0; break;
		case '7' : sig1++;if(found_point == 1){sig2++;} found_digit = 1;zeros = 0; break;
		case '8' : sig1++;if(found_point == 1){sig2++;} found_digit = 1;zeros = 0; break;
		case '9' : sig1++;if(found_point == 1){sig2++;} found_digit = 1;zeros = 0; break;
		default :  break;
	    }
	    if(found_power == 0 && bracket == 0){ append(exp,word[i]); } // maybe a power was used ?
	    bracket = 0;
        }
	
	if( found_power > 1 || found_multiply > 1){ // 2*2 10^5^7
	    fprintf(stdout,"error \n");
	    return 0;
	}

	if( points > 1){ // "nonsense" number 1.23.45  or 1.23*10^1.5
	    fprintf(stdout,"-1,-1,-1,-1,-1,-1\n");
	}
	else
	{
	    // extra check for handling "special cases" 
	    if(found_point == 1 && found_power == 0 && found_multiply == 0 && ( word[1] == '.' || word[0] == '.' ) ){ok = 1;}// just a decimal number 0.1233
	    else
	    if(found_point == 0 && found_power == 0){ sig2 = length;ok = 1; }	// just a number 12345
	    else
	    if(found_point == 0 && found_multiply == 0 && found_power == 1){ sig1 = 0; sig2 = 0 ; dec1 = 0; }	// 10^5
	    else
	    if(found_point == 1 && found_multiply == 0 && found_power == 1){ sig1 = 0; sig2 = 0 ; dec1 = 0; }	// 10^5.1
	    else
	    if(found_point == 0 && found_multiply == 1 && found_power == 1){ dec1 = 0; sig2 = length - zeros - pow;}	// 3*10^5
	        
	    if( found_power == 1){
		// find out if scientiffic number is correctly written ; ok=0 -> ok=1 
		// rule [1-9].[0-9]* x10^exp
		if( word[0] != '0' && word[1] == '.'  ){ //  0.120*10^5 => 1.20*10^4
		    ok = 1;
		}
		else
		{
		    if( (word[0] == '-' || word[0] == '+' ) && word[1] != '0' && word[2] == '.' ){ 
		    	ok = 1; // -1.2*10^5
		    }
		    else
		    {
			if( found_point == 0 && found_multiply == 1 ){
			    if( word[1] == '*' ){ //4*10^5
				ok = 1;
			    }
			    else
			    {
				if( (word[0] == '-' || word[0] == '+' ) && word[2] == '*'){ //-4*10^5
				    ok = 1;
				}
			    }
			}
		    }
		}
		int len = strlen(exp);// reverse appended char array ... 123+ --> +321
	        char exponent[len];
		int w = len - 1;
	        for(i = 0 ; i < len ; i++){
		    exponent[i] = exp[w];
		    w--;
		}
		exponent[len] = '\0';
		if(base_start != 0){ // find the base ( default base = 10 )
		    int c = 0;int s;
		    if(base_end == 0){ // 10^5
			s = 0; 
		    }
		    else
		    {
			s = base_end + 1; // 1.25*10^5
		    } 
		    char c_base[base_start - s]; // assign char array of correct size
		    for(i = s ; i < base_start ; i++){
			c_base[c] = word[i]; 
			c++;
		    }
		    c_base[c] = '\0';
		    fprintf(stdout,"%d,%d,%d,%s,%s,%d\n",sig1,sig2,dec1,c_base,exponent,ok);
		}
		else
		{ // base = 10 : used 4e+5
		    fprintf(stdout,"%d,%d,%d,10,%s,%d\n",sig1,sig2,dec1,exponent,ok);
		}
	    }
	    else
	    { 	// no exponent : base = '1' exponent = '0'
		//several possible correct way of writing...
		if( ok == 0 && (  sig1 == sig2 + dec1 ) ){ ok  = 1; } 
		fprintf(stdout,"%d,%d,%d,1,0,%d\n",sig1,sig2,dec1,ok);
	    }
	}
	cnt++;
	input = argv[cnt];
    }
    return 0;
}
