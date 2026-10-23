#include <assert.h>
#include "../../Lib/libwims.h"
#define MAX_INT 128
#define MAX_BUFFER   270000
#define MAX_DRAGSTUFF 28
#define MAX_MULTI_PRIMITIVES 29
/* wims : MAX_FWRITE_SIZE 64*1024=65536 == 2*MAX_BUFFER */
#define MAX_COLOR_STRING 32
#define MAX_SLIDERS  16
/* the commmand / keyword name collection */
typedef enum {
  COMMENT=-2,END,SIZE=1,XRANGE,YRANGE,FONTFAMILY,MATHML,INPUT,TEXTAREA,LINEWIDTH,POLYLINE,POLY,
  SEGMENT=15,LINE,DLINE,RECT,ARC,NEW,STRING,STRINGUP,COPY,COPYRESIZED,TRANSPARENT,POINT,CIRCLE,
  GETPIXEL,SQUARE,ELLIPSE=31,ROTATE,TYPE,QUALITY,CSS,POINTS,TRIANGLE,HLINE,VLINE,GRID,ZOOM,ARROW,
  DARROW,AXIS,FONTSIZE=46,CURVE=50,PLOTSTEPS,TRANGE=53,MOUSE_PRECISION,FLY_TEXT,FLY_TEXTUP,BGIMAGE,
  DASHED,PARALLEL,LATTICE,OPACITY,AXIS_NUMBERING,RAYS,OUTPUT,CLOCK,STYLE,USERDRAW,MOUSE,AUDIO,
  AUDIOOBJECT,STOP,RESTART,FILLCOLOR,CUBE,CLEARBUTTON,ONCLICK,CROSSHAIR,CROSSHAIRS,BLINK,DASHTYPE,
  HTML,CROSSHAIRSIZE,ARROWHEAD,DRAG,HTTP,SLIDER,CLICKTILE=88,CLICKTILE_COLORS,TRANSLATION,
  KILLTRANSLATION,ARROW2,DARROW2,SVGCODE=95,ROUNDRECT,HATCHFILL,PATTERNFILL,DIAMONDFILL,DOTFILL,
  GRIDFILL,IMAGEFILL,FILL,FILLED,XYLOGSCALE,XLOGSCALE,YLOGSCALE,XLOGBASE,INTOOLTIP,REPLYFORMAT,
  VIDEO,BGCOLOR,FLOODFILL,FILLTOBORDER,SETPIXEL,PIXELS,PIXELSIZE,X_AXIS_STRINGS=121,Y_AXIS_STRINGS,
  FONTCOLOR,PIECHART,LEGEND,BARCHART,LINEGRAPH,STROKECOLOR,XLABEL,YLABEL,LEGENDCOLORS,ANIMATE,STATUS,
  SNAPTOGRID,XSNAPTOGRID,YSNAPTOGRID,USERINPUT_XY,SGRAPH=139,YLOGBASE,AFFINE,KILLAFFINE,LEVELCURVE,LEVELSTEP,
  JSMATH,TRACE_JSCURVE,SETLIMITS,JSCURVE,CENTERSTRING,MOUSEX,MOUSEY,MOUSE_DEGREE,MOUSE_DISPLAY,XUNIT,
  YUNIT,KILLSLIDER,ANGLE,USERINPUT_FUNCTION,USERINPUT,HALFLINE,FUNCTION_LABEL,SEGMENTS,ARROWS,
  CIRCLES,ARROWS2,LINES,RECTS,HALFLINES,HLINES,VLINES,ROUNDRECTS,BEZIER,SNAPTOPOINTS,GROUP,
  SNAPTOFUNCTION=175,MULTIDRAW,MULTILINEWIDTH,MULTISTROKECOLORS,MULTISTROKEOPACITY,MULTIFILLCOLORS,
  MULTIFILLOPACITY,MULTIFILL,MULTILABEL,MULTIDASH,MULTISNAPTOGRID,MULTIUSERINPUT,PROTRACTOR,RULER,
  TRIANGLES, POPUP,X_AXIS_STRINGS_UP,CURSOR,XERRORBARS,YERRORBARS, BOXPLOTDATA=196,USERBOXPLOT,
  USERBOXPLOTDATA, ROTATION_CENTER,KILLROTATE,CANVASTYPE, NOXAXIS,NOYAXIS,BOXPLOT,COLORPALETTE,
  FILLALL,XYOFFSET, XOFFSET,YOFFSET,CENTERED,RESETOFFSET,TEXTFILL,FILLPATTERN,NUMBERLINE,ELLIPSES,
  IMAGEPALETTE, CURVEDARROW2,CURVEDARROW,CURVEDARROWS,CURVEDARROWS2,LATEX,ALLOW_DUPLICATES=223,
  LINEAR,KILLLINEAR, OBABEL,CHEMTEX,NORESET,ARCARROW,KILL,RANGE,NEWRANGE,RESET,
  HYPCIRCLES,HYPSEGMENTS,HYPLINES,HYPPOLY,HYPRAYS,STROKEOPACITY,FILLOPACITY,MOEBIUS,KILLMOEBIUS} keyword;

/* the js_function[] dcollection : js-code without configurable argumens see canvasutil.c  */
enum {DRAW_GRID, DRAW_CROSSHAIRS,DRAW_GRIDFILL,DRAW_XML,DRAW_ARCS,DRAW_LATTICE,DRAW_INPUTS,
  DRAW_TEXTAREAS,DRAW_HTTP,DRAW_AUDIO,DRAW_VIDEO,DRAW_DOTFILL,DRAW_HATCHFILL,DRAW_IMAGEFILL,
  DRAW_FLOODFILL,DRAW_FILLTOBORDER,DRAW_PIXELS,DRAW_LINES,DRAW_PIECHART,DRAW_CLOCK,DRAW_EXTERNAL_IMAGE,
  DRAW_DIAMONDFILL,DRAW_SGRAPH,DRAW_YLOGSCALE,DRAW_XLOGSCALE,DRAW_XYLOGSCALE,DRAW_CENTERSTRING,
  DRAW_JSFUNCTION,DRAW_BEZIER,DRAW_BOXPLOT,DRAW_JSBOXPLOT,DRAW_NUMBERLINE,JS_FIND_ANGLE,DRAW_TEXTFILL,
  INTERACTIVE,JS_ROTATE_MOUSE,JS_TRANSFORM_MOUSE,DRAW_FILL_PATTERN,JS_ZOOM,DRAG_AND_ZOOM,
  USERDRAW_AND_ZOOM,JS_RAWMATH,JS_LOAD_IMAGE,JS_SAFE_EVAL,JS_PLOT,JS_MATH,DRAW_TEXTS,
  DRAG_EXTERNAL_IMAGE,ADD_USER_INPUTS,JSPLOT_AND_ZOOM,JS_ARROWHEAD,DRAW_SUBSUP,MAX_JS_FUNCTIONS};
/*  end js_function[]  */
/* default canvasses, eg png images xsize x ysize */
enum{EXTERNAL_IMAGE_CANVAS, /* this is created first !!! in order to draw on it */
  BG_CANVAS,STATIC_CANVAS,MOUSE_CANVAS,GRID_CANVAS,DRAG_CANVAS,DRAW_CANVAS,TEXT_CANVAS,CLOCK_CANVAS,ANIMATE_CANVAS,TRACE_CANVAS,
  JSPLOT_CANVAS=100,
  /* JSPLOT_canvas will be increased with every added inputfield...should not conflict with other images*/
  FILL_CANVAS=200,
  /* will be increase with every fill */
  USERDRAW_JSPLOT=300,
  /* USERDRAW_JSPLOT will be increased with every added inputfield...should not conflict with other images*/
  CLICKFILL_CANVAS=400, /* will be increase with every click fill */
  BOXPLOT_CANVAS=500, /* will increase with every new boxplot */
  NUMBERLINE_CANVAS=600,
  MULTIDRAW_CANVAS=1000, /* + >25 draw types... 1001=points*/
  ZOOM_CANVAS=1100,
  STATIC_IMAGE_CANVAS=700};

#ifndef STD_H
#define STD_H

#ifndef bool
#define bool            char
#endif
#ifndef TRUE
#define TRUE            1
#endif
#ifndef FALSE
#define FALSE           0
#endif

#endif
void add_to_buffer(char *tmp); /* add tmp_buffer to the buffer */
void check_string_length(int length);
extern void *tmp_buffer;

void rotate(int num,double angle,double center[],int incr);
void transform(int num,int incr);
void moebius(int num,int incr);
void add_read_canvas(int type_reply,int reply_precision);
void add_javascript_function();
void add_drag_code(int canvas_cnt,int use_dragstuff, int stuff[],int reply_format);
void add_trace_js_mouse(int canvas_cnt,char *stroke_color,char *jsmath,int font_size,double stroke_opacity,int line_width,int crosshair_size,char *css_class);
void add_setlimits(int font_size,char *css_class);
void add_safe_eval();
void add_calc_y(char *jsmath,int font_size,char *css_class);
void add_slider(int anim);
void add_slider_display(int precision,int font_size,char *font_color,double stroke_opacity);
void add_xyslider(double v1,double v2,int width,int height,char *use_slider,char *label,int slider_cnt,char *stroke_color,char *fill_color,int line_width,double opacity,char *font_family,char *font_color,int use_slider_display);
void *my_newmem(size_t size);
void canvas_error(char *msg);
char *eval(int xsize,int ysize,char *fun,double xmin,double xmax,double ymin,double ymax,int xsteps,int precision,double rotationcenter[]);
char *eval_parametric(int xsize,int ysize,char *fun1,char* fun2,double xmin,double xmax,double ymin,double ymax, double tmin,double tmax,int plotsteps,int precision,double rotationcenter[]);
char *eval_levelcurve(int xsize,int ysize,char *fun,double xmin,double xmax,double ymin,double ymax,int plotsteps,int precision,double level);
char *getMML(char *tex);
char *getSVGMOL(char *inputtype,char *keys);
char *data2js_array(int data[],int len);
char *double_xy2js_array(double xy[],int len,int decimals);
char *doubledata2js_array(double data[],int len, int decimals);
char *list2js_array(char *list,char *s);
int find_number_of_digits(int i);
int x2px(double x);
int y2px(double y);
char *str_replace(const char *str, const char *old, const char *new);
void add_clear_button(char *css_class,char *button_text);
void add_js_mouse(int canvas_cnt,int precision,char *stroke_color,int font_size,double stroke_opacity,int type);
void add_js_filltoborder(int canvas_type);
void add_js_multidraw(char *draw_types,char *button_style,int use_offset,int use_controls,int crosshair_size,int use_zoom);
extern void add_js_userdraw(char *draw_type,char *stroke_color,double stroke_opacity,int crosshair_size,int arrow_head,int use_offset, char *css_class,int use_snap,int canvas_type,int use_filled,char * fill_color,double fill_opacity,int line_width,char *font_family);
/* these should  be harmonized via switch key !!*/
void add_input_circle(int type,int num);
void add_input_segment(int num);
void add_input_demiline(int num);
void add_input_line(int num);
void add_input_polyline();
void add_input_crosshair(int num);
void add_input_arrow(int num);
void add_input_xy(int font_size,char *css_class);
void add_input_xyr(int font_size,char *css_class);
void add_input_x1y1x2y2(int font_size,char *css_class);
void add_textarea_xy(char *css_class);
void add_js_zoom_buttons(char *stroke_color,double stroke_opacity);
void add_js_tooltip(char *tooltip_text,char *bgcolor);
void add_js_popup(char *getfile_cmd);
void add_input_jsfunction(char *css_class,char *input_label,int input_cnt,char *stroke_color,float stroke_opacity,int line_width,int use_dashed,int dashtype0,int dashtype1,int font_size);
void add_js_protractor(int type,double xcenter,double ycenter,int size,char *font,char *stroke_color,double stroke_opacity,char *fill_color,double fill_opacity,int line_width,int use_scale,int dynamic,int use_snap);
void add_js_ruler(double x,double y,double sizex,double sizey,char *font,char *stroke_color,double stroke_opacity,char *fill_color,double fill_opacity,int line_width,int dynamic,int use_snap);
void add_color_palette(char *css_class);
void add_js_images(int use_offset,int snap);
int count_substring(char* string, char* substring);
extern int NUMBER_OF_COLORNAMES;
extern struct colors { char *hex; char *name; char *rgb; } colors[];
extern int js_function[MAX_JS_FUNCTIONS];
extern int reply_format;
extern int xsize;
extern int ysize;
extern unsigned int canvas_root_id;
extern int user_input_xy;
extern char *css_class;
extern int font_size;
extern char *draw_type;
extern char *function_label;
extern int input_cnt;
extern int use_dashed;
extern int dashtype[];
extern int jsplot_cnt;
extern int linegraph_cnt;
extern int barchart_cnt;
extern int legend_cnt;
extern int use_axis;
extern int use_axis_numbering;

extern FILE *js_include_file;
/* used multidraw primitives : identifier in canvasmultidraw.c is index of this array
static char multidraw_primitives[MAX_MULTI_PRIMITIVES][32] = {"point","points","circle","circles",
"line","lines","segment","segments",
"arrow","arrows","triangle","triangles",
"closedpoly","text","rect","rects",
"poly","polys","parallelogram","parallelograms",
"images","curvedarrow","curvedarrows","curvedarrow2","curvedarrows2",
"crosshair","crosshairs"};
size of words "point","points",... == 6,7,..
static int multidraw_primitives_length[MAX_MULTI_PRIMITIVES] = {7,6,8,7,6,5,9,8,7,6,10,9,11,5,6,5,6,5,15,14,7,14,
13,13,12,10,11};
*/
