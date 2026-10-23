/*
    itex2MML  1.3.3
    website : https://golem.ph.utexas.edu/~distler/blog/itex2MML.html
    repository : https://golem.ph.utexas.edu/~distler/code/itexToMML/

    renamed to "wims_mathml"
    Slightly modified for WIMS mathml usage 05/2012
    J.M. Evers, B. Perrin-Riou

*/

#include <cstdio>
#include <string>
#include "wims_mathml.h"

// these C headers are needed to compile on OpenSuSE Linux (and others?) to use atoi/strlen
#include <string.h>
#include <stdlib.h>
#include <sys/time.h>
#include <ctime>


int use_javascript;
char wims_texfontsize[64]; // is set by cmd-line argument "--tex-size" via wims
char wims_mathml_id[64]; // is set by cmd-line argument "--mathml-id" via wims

const int set_javascript(){
    if(use_javascript == 1){
	return 1;
    }
    else
    {
	return 0;
    }
}
const char * read_mathml_id(){
    return wims_mathml_id; // wims_mathml.y will use this in first <mstyle id="read_mathml_id()" ...
    // the whole mathml-string will get this "id" ; does not interfere (?) with "maction" tag, which is on "sub math level"
}

const char * read_fontsize(){ // declared in wims_mathml.h
    // in not given : mathsize="110%"
    if(strlen(wims_texfontsize) == 0){snprintf(wims_texfontsize,sizeof(wims_texfontsize),"%s","110%");}
    return wims_texfontsize; // wims_mathml.y will use this in first <mstyle mathsize="read_fontsize()" ...
}
// count number of substring occurences
int count_substrings( const std::string & str , const std::string & obj ) {
    int n = 0;
    std::string ::size_type position = 0;
    while( (position = obj.find( str, position )) != std::string::npos ){
	n++;
	position += str.size();
    }
    return n;
}

int main (int argc, char ** argv)
{
	bool bPrintItex = false;
	bool bRawFilter = false;
	bool bInline    = false;
	bool bDisplay   = false;
	bool bTexString = false;
	bool bStop = false;
	bool bForbidMarkup = false;
	// wims
	int MAX_MML_SIZE = 8192;
	int i_texstring = 0;
	int insize=0;
	using namespace std;
	std::string itex;
	for (int arg = 1; arg < argc; arg++)
		{
			std::string args = argv[arg];

			if (args == "--version" || args == "-v")
				{
					fputs("wims_mathml version " ITEX2MML_VERSION "\n"
					      "See https://golem.ph.utexas.edu/~distler/blog/itex2MML.html for more information.\n", stdout);
					bStop = true;
					break;
				}

			if (args == "--help" || args == "-h"){
			    fputs ("usage: wims_mathml [OPTIONS]\n"
			    "\n"
			    "wims_mathml filters an input text stream (e.g., an XHTML web page) converting itex expressions\n"
			    "to MathML. Inline itex expressions are delimited either side by single dollar symbols ($):\n"
			    "\n"
			    "\t<p>The parameters $\\alpha$ and $\\beta$ in the function $f(x)$ are defined below.</p>\n"
			    "\n"
			    "For normal display of equations, etc., itex expressions can be delimited with double dollar\n"
			    "symbols ($$) either side or by \\[ to the left and \\] to the right:\n"
			    "\n"
			    "\t<p class=\"equation\">\\[\n"
			    "\t\tf(x) = \\alpha x + \\frac{\\beta}{1+|x|}\n"
			    "\t\\]</p>\n"
			    "\n"
			    "wims_mathml Options:\n"
			    "\n"
			    "  --raw-filter    filter input stream, converting equations as found to MathML [stops on error]\n"
			    "  --inline        converts a single itex equation, without any $ symbols, to inline MathML\n"
			    "  --display       converts a single itex equation, without any $ symbols, to display-mode MathML\n"
			    "  --forbid-markup forbid markup (more precisely, the '<' and '>' characters) in itex equations\n"
			    "  --print-itex    used in conjuction with --inline or --display: prints the itex string\n"
			    "\n"
			    "WIMS USAGE:"
			    "  --mathml-id	unique random id for every mathml string\n"
			    "  --tex-size	base fontsize of the mathml string\n"
			    "  --max-mml-size	wims maximum stringlength  \n"
			    "  --tex-string     the actual math string\n"
			    "\n"
			    "For further information, see https://golem.ph.utexas.edu/~distler/blog/itex2MML.html\n", stdout);
			    bStop = true;
			    break;
			}
			if (args == "--print-itex"){
			    bPrintItex = true;
			    bRawFilter = false;
			    continue;
			}
			if (args == "--forbid-markup"){
			    bRawFilter = false;
			    bForbidMarkup = true;
			    continue;
			}
			if (args == "--inline"){
			    bRawFilter = false;
			    bInline    = true;
			    bDisplay   = false;
			    continue;
			}
			if (args == "--display"){
			    bRawFilter = false;
			    bInline    = false;
			    bDisplay   = true;
			    continue;
			}
			if (args == "--raw-filter"){
			    bRawFilter = true;
			    bPrintItex = false;
			    bInline    = false;
			    bDisplay   = false;
			    continue;
			}
			// wims
			if (args == "--use-zoom"){
			    use_javascript = atoi( argv[arg+1] );
			    continue;
			}
			if (args == "--tex-size"){
			    snprintf(wims_texfontsize,sizeof(wims_texfontsize),"%s",argv[arg+1]);
			    continue;
			}
			if (args == "--max-mml-size"){
			    MAX_MML_SIZE = atoi( argv[arg+1]);
			    continue;
			}
			if (args == "--tex-string"){
			    bPrintItex = false;
			    bRawFilter = false;
			    bTexString = true;
			    bDisplay = false;
			    i_texstring = arg+1;
			    break;
			}
		}
	if (bStop) return 0;
	/* every mathml-string will have unique id */
	struct timeval tv;
	struct timezone tz;
	struct tm *tm;
	gettimeofday(&tv, &tz);
	tm=localtime(( const time_t * ) &tv.tv_sec);
	snprintf(wims_mathml_id,sizeof(wims_mathml_id),"wims_mathml%ld",long(tv.tv_usec));

	if(bTexString){ // WIMS modification: reads a 'latex string' from commandline, if arg="--tex-string"
	    char *input;
	    int dollarcnt = 0;
	    input = argv[i_texstring];
	    while(input != NULL){
	         for(int i = 0; i <  strlen(input); i++){
		    switch(input[i]){
			case '<':itex+= "\\lt ";break; // < \lt
			case '>':itex+= "\\gt ";break; // < \gt
			case '$':dollarcnt++;break; // ignore $ we put them directly in itex
			default:itex+=(char) input[i];
        	    }
		}
		i_texstring++;// probably never more than 1 ...
		input = argv[i_texstring];
	    }
	    // common error to forget \right.
	    // this must be done before adding $ around itex
	    int left =  count_substrings("\\left\\" ,itex) + count_substrings("\\left[" ,itex) + count_substrings("\\left(" ,itex) + count_substrings("\\left{" ,itex) + count_substrings("\\left." ,itex) + count_substrings("\\left \\" ,itex) + count_substrings("\\left [" ,itex) + count_substrings("\\left (" ,itex) + count_substrings("\\left {" ,itex) + count_substrings("\\left ." ,itex) ;
	    int right = count_substrings("\\right\\",itex) + count_substrings("\\right]",itex) + count_substrings("\\right)",itex) + count_substrings("\\right}",itex) + count_substrings("\\right.",itex) + count_substrings("\\right \\",itex) + count_substrings("\\right ]",itex) + count_substrings("\\right )",itex) + count_substrings("\\right }",itex) + count_substrings("\\right .",itex) ;
	    if( left != right){
		if( left > right){
		    for(int i = 0 ; i<left-right;i++){
		        itex+=" \\right.";
		    }
		}
		else
		{
		    for(int i = 0 ; i<right-left;i++){
			itex="\\left. "+itex;
		    }
		}
	    }

	    // finish the math mode with appropriate $-signs
	    if( dollarcnt > 2 ){
		bDisplay = true; // use display mode
		itex="$$" + itex;
		itex+="$$";
	    }
	    else
	    {
		bInline = true; // use inline mode
		    itex="$" + itex;
		    itex+="$";
	    }
	}
	else // standard syntax echo  "\frac{1}{2}" | wims_mathml --inline > test.xml
	{ // nothing changed here...
	    #define BUFSIZE 1024
	    char buffer[BUFSIZE];
	    if (bInline)  itex += "$";
	    if (bDisplay) itex += "$$";
	    while (fgets (buffer, BUFSIZE, stdin)) itex += buffer;
	    if (bInline)  itex += "$";
	    if (bDisplay) itex += "$$";
	}

	if (bPrintItex)
		{
			fputs (itex.c_str (), stdout);
			fputs ("\n", stdout);
			fflush (stdout);
		}

	if (!bInline && !bDisplay)
		{
			if (bRawFilter)
				wims_mathml_filter (itex.c_str(), itex.size());
			else
				if (bForbidMarkup)
					wims_mathml_strict_html_filter (itex.c_str(), itex.size());
				else
					wims_mathml_html_filter (itex.c_str(), itex.size());
			return 0;
		}
	char * mathml = wims_mathml_parse (itex.c_str(), itex.size() );

	if (mathml)
		{
		    if(bTexString){ // wims usage
			if(strlen(mathml) > (MAX_MML_SIZE)){
			    fputs("ERROR",stdout);
			}
			else
			{
			    fputs (mathml, stdout);
			}
		    }
		    else
		    {
			fputs (mathml, stdout);
		    }
		    wims_mathml_free_string (mathml);
		    mathml = 0;
		}
	else
		{
		    if(bTexString){ // wims usage
			fprintf(stdout,"ERROR");
		    }
		    else
		    {
			fputs ("wims_mathml: itex parser failed to generate MathML from itex!\n", stderr);
		    }
		}
	return 0;
}
