/*             wims_mathml 1.4.10
 *   wims_mathml.h last modified 6/16/2012
 */

#ifndef ITEX2MML_H
#define ITEX2MML_H

#define ITEX2MML_VERSION "1.4.10"

#ifdef __cplusplus
extern "C" {
#endif

  /* Step 1. Parse a buffer with itex source; return value is mathml, or 0 on failure (e.g., parse error).
   */
  const int set_javascript();// wims_mathml.y can read this variable
  const char * read_fontsize(); // wims_mathml.y may use this from wims_mathml.cc to read string "fontsize"
  const char * read_mathml_id(); // wims_mathml.y may use this from wims_mathml.cc to read string "mathml_id"
  extern char * wims_mathml_parse (const char * buffer, unsigned long length);

  /* Step 2. Free the string from Step 1.
   */
  extern void   wims_mathml_free_string (char * str);


  /* Alternatively, to filter generic source and converting embedded equations, use:
   */
  extern int    wims_mathml_filter (const char * buffer, unsigned long length);
  extern int    wims_mathml_html_filter (const char * buffer, unsigned long length);
  extern int    wims_mathml_strict_html_filter (const char * buffer, unsigned long length);
  extern int 	wims_mathml_do_html_filter (const char * buffer, unsigned long length, const int forbid_markup);


  /* To change output methods:
   *
   * Note: If length is 0, then buffer is treated like a string; otherwise only length bytes are written.
   */
  extern void (*wims_mathml_write) (const char * buffer, unsigned long length); /* default writes to stdout */
  extern void (*wims_mathml_write_mathml) (const char * mathml);                /* default calls wims_mathml_write(mathml,0) */
  extern void (*wims_mathml_error) (const char * msg);                          /* default writes to stderr */


  /* Other stuff:
   */
  extern void   wims_mathml_setup (const char * buffer, unsigned long length);

  extern void   wims_mathml_restart ();

  extern char * wims_mathml_copy_string (const char * str);
  extern char * wims_mathml_copy_string_extra (const char * str, unsigned extra);
  extern char * wims_mathml_copy2 (const char * first, const char * second);
  extern char * wims_mathml_copy3 (const char * first, const char * second, const char * third);
  extern char * wims_mathml_copy_escaped (const char * str);

  extern char * wims_mathml_empty_string;

  extern int    wims_mathml_lineno;
  extern int    wims_mathml_shortcut;

  extern int    wims_mathml_rowposn;
  extern int    wims_mathml_displaymode;

  extern char * wims_mathml_remove_char(char * str , char * c);
  extern void replace_str(const char *str, const char *old, const char *newer);
#ifdef __cplusplus
}
#endif

#endif /* ! ITEX2MML_H */
