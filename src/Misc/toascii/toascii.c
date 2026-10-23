/*
*********************************************************************************
* J.M. Evers 6/2005								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled objects derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatoever								*
*********************************************************************************
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#define MAX_SIZE 1024

int main ( int argc , char *argv[]){ 
    if( argc < 2 ){
	fprintf(stdout,"error! usage:!exec toascii $your_list_of_items \nReturns a comma separated list of sums of the ascii values of the individual items.\nExample:\ntoascii cat,dog,house\n312,314,548\n");
	exit(0);
    }

    char word[MAX_SIZE];
    int i;
    int length;
    int total = 0;
    int cnt = 0;
    char *ptr;
    char *inp;
    inp = argv[1];
    ptr = (char *) strtok(inp,",");
    while ( ptr != NULL ){
	length = strlen(ptr);
	if( length >= MAX_SIZE){
	    fprintf(stdout,"error! size of word is bigger than %d\n",MAX_SIZE);
	    exit(0);	
	}
	else
	{
	    strncpy( word, ptr, length ) ;
	    total = 0;
	    for( i = 0;i < length ;i++ ){
		total = total + word[i];
	    }
	}
	if( cnt == 0 ){ cnt = 1; fprintf(stdout,"%d",total);}else{ fprintf(stdout,",%d",total); }
	ptr = (char *)  strtok(NULL, ",");
    }
    fprintf(stdout,"\n");
    return 0 ;
}

