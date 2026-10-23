%module wims_mathml
%{
#include "wims_mathml.h"
extern const char * wims_mathml_output ();
%}
extern int    wims_mathml_filter (const char * buffer, unsigned long length);
extern int    wims_mathml_html_filter (const char * buffer, unsigned long length);
extern const char * wims_mathml_output ();
