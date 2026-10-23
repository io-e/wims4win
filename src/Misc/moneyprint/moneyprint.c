/*
*********************************************************************************
* J.M. Evers 3/2012              *
* This is all amateur scriblings... So no copyrights.        *
* This source code file, and compiled objects derived from it,      *
* can be used and distributed without restriction, including for commercial use  *
* No warrenty whatsoever              *
*********************************************************************************
general use:
rounding to 2 decimals (financial math)
tot=!exec moneyprint 0.1,17,123.4,123.99765
tot -> 0.10,17.00,123.40,124.00

2/2012
 modified for general rounding usage; a word 'DECIMALS' can be added to the list of numbers
 tot=!exec moneyprint 2.1,4.123,5  2   // 2 decimals
 tot -> 2.10,4.12,5.00
 tot=!exec moneyprint 2.1,4.123,5  4   // 4 decimals
 tot -> 2.1000,4.1230,5.0000
 tot=!exec moneyprint 2.1,4.123,5
 tot -> 2.10,4.12,5.00 //default value (or old syntax) is 2 decimals

6/2012.
 modified again to support rounding of scientific numbers, like
 tot=!exec moneyprint 1.23456e+06,1.23456*10^6,0.01234e-23 3
 tot -> 1.235e06,1.235e6,0.012e-23

 assumed only powers of 10 [scientific notation]

12/2012
Modified : using only 10^ in powers
(e+8 -> *10^8 ; e-8 -> *10^-8)
Extra syntax error signal (using more than 1 'e')

27/2014
Modified avoiding truncating of numbers like 15.625 --> 16.62
4/2020
Modified to use ";" and "," as separators...preserving the sequence
moneyprint 1,2,3;4,5,6;7,8,9 -> 1.00,2.00,3.00;4.00,5.00,6.00;7.00,8.00,9.00
*/


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#define MAX_DIGITS 32
#define MAX_CONV 32

int main( int argc, char *argv[]){
  if( argc != 2 && argc != 3){
    fprintf(stdout,"error !\nusage:\n!exec moneyprint $your_wims_item_list $precision_word\nexample:\nmoney=!exec moneyprint 1.2,30.1,.4,-.23123456 2\nThe result is a comma separated list: 1.20,30.10,0.40,-0.23\n using 2 decimals\nNote: no calculations are done.\nNo spaces allowed \n");
    exit(0);
  }
  /* test for illegal characters */
  const char *invalid_characters = "\n\"\'!=ABCDFGHIJKLMNOPQRSTUVWYZabcdfghijklmnopqrstuvwyz@#$%&()[]{}:~><?/\\|";
  /* ;+-*^XxEe are allowed : 12.34e+05  12.34e-08 1x10^5 1.234*10^123*/
  char *input;
  input = argv[1];
  while (*input){
    if ( strchr(invalid_characters, *input) ){
      fprintf(stdout,"error !\nfound illegal character \"%c\" in argument\n",*input);
      exit(0);
    }
    input++;
  }
  int DECIMALS;
  if(argv[2] != NULL){
    DECIMALS = atoi(argv[2]);
    if(DECIMALS > MAX_DIGITS){
      fprintf(stdout,"error ! maximum amount of decimals is %d \n",MAX_DIGITS);exit(0);
    }
  }
  else DECIMALS = 2;
  char *ptr;
  char word[MAX_DIGITS];
  char exponent[MAX_DIGITS];
  char number[MAX_DIGITS];
  int cnt = 0;
  int powE = 0;
  int idx1 = 0;
  int idx2 = 0;
  int length= 0;
  int i = 0;
  int pow10 = 0;
  double correction = 1/(pow(10,DECIMALS+6)); /* a reasonable guess...to avoid truncating 15.625 --> 16.63 */
  double ascii_number;
  input = argv[1];
  /* 14/4/2020 replace ';' by ','  BPR */
  int idx3[MAX_CONV];
  for(i = 0; i < strlen(input); i++){
    if(input[i] == ';'){
      input[i] = ','; idx3[cnt] = 1; cnt++;
    }
    else
      if(input[i] == ',' ){ idx3[cnt] = 0; cnt++;}
  }
  char *sep = ",";
  cnt = 1;
  ptr = strtok(input,",");
  while( ptr != NULL){
    if( cnt > MAX_CONV ){
      fprintf(stdout,"ERROR too many (> %d)conversion \n",MAX_CONV);
      exit(0);
    }
    /* next item in input argv[1] */
    strncpy( word, ptr, MAX_DIGITS-1);
    length = strlen(ptr);
    if(length > MAX_DIGITS-1){
      fprintf(stdout,"ERROR string too large\n");
      exit(0);
    }
    /* reset counters */
    powE = 0;
    pow10 = 0;
    idx1 = 0;
    idx2 = 0;
    i = 0;
    for( i = 0; i < length ; i++){
      if(powE > 1 ){
        fprintf(stdout,"ERROR in syntax\n");
        exit(0);
      }
      if( idx1 + idx2  >  MAX_DIGITS-1){
        fprintf(stdout,"ERROR string too large\n");
        exit(0);
      }
      switch( word[i] ){
        case '+' : break; /* do not use 10^+2 */
        case 'e' : powE++;break;
        case 'E' : powE++;break;
        case 'x' : pow10++;break;
        case '*' : pow10++;break;
        case '^' : pow10=5;break;
        default  :
        if( pow10 > 0 ){
          pow10++;
          if( pow10 > 4 ){ /*  *10^ = 4 chars */
            exponent[idx2] = word[i];
            idx2++;
          }
        }
        else
        {
          if(powE > 0){
            exponent[idx2] = word[i]; idx2++;}
          else {
            number[idx1] = word[i]; idx1++;}
        }
        break;
      }
    }
    exponent[idx2] = '\0';
    number[idx1] = '\0';
    ascii_number = atof(number);
    if( ascii_number > 0 )
      ascii_number = ascii_number + correction;
    else
      ascii_number = ascii_number - correction;
    if( powE == 1 || pow10> 0 ){
      if(cnt > 1)
        fprintf( stdout, "%s%.*f*10^%s", sep, DECIMALS, ascii_number, exponent );
      else
        fprintf( stdout, "%.*f*10^%s", DECIMALS, ascii_number, exponent );
    }
    else {
      if(cnt > 1 )
        fprintf( stdout, "%s%.*f", sep, DECIMALS, ascii_number);
      else
        fprintf( stdout, "%.*f", DECIMALS, ascii_number);
    }
    if(idx3[cnt-1] == 1 ) sep = ";"; else sep = ",";
    cnt++;
    ptr = strtok(NULL,",");
  }
  fprintf(stdout,"\n");
  return 0;
}
