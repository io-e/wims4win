/*27/7/2013 version  0.01
"Inspired" by FLY program: http://martin.gleeson.com/fly
*********************************************************************************
* J.M. Evers 7/2013        *
* This is all just amateur scriblings... So no copyrights.      *
* This source code file, and compiled objects derived from it,      *
* can be used and distributed without restriction, including for commercial use  *
* No warrenty whatsoever      *
*********************************************************************************
*/
#define VERSION    "0.5"

#define _GNU_SOURCE
#include <string.h>

#include "canvasdraw.h"
void *tmp_buffer;
FILE  *js_include_file;
/******************************************************************************
**  Internal Functions
******************************************************************************/
void  add_to_buffer(char *tmp); /* add tmp_buffer to the buffer */
void  sync_input(FILE *infile);/* proceed with inputfile */
void  reset();/* reset some global variables like "use_filled", "use_dashed" */
int  get_token(FILE *infile); /* read next char until EOL*/
char *fillcolor;
int  x2px(double x);
int  y2px(double y);

double   px2x(int x);
double   px2y(int y);
double  get_real(FILE *infile,int last); /* read a value; calculation and symbols allowed */
char  *str_replace ( const char *word, const char *sub_word, const char *rep_word );
char  *get_color(FILE *infile,int last); /* read hex-color or colorname -> hex */
char  *get_string(FILE *infile,int last); /* get the string at the end of a command */
char  *get_string_argument(FILE *infile,int last); /* the same, but with "comma" as  separator */
int hypgeodaux(double *q, double* res, int full);
char   *convert_hex2rgb(char *hexcolor);
void  make_js_include(int canvas_root_id);
void  check_string_length(int length);/* checks if the length of string argument of command is correct */
FILE   *js_include_file;
FILE  *get_file(int *line_number, char **filename);
FILE   *infile;    /* will be stdin */
/******************************************************************************
** global
******************************************************************************/
int finished = FALSE;/* main variable for signalling the end of the fly-script ; if finished = 1 ; write to stdout or canvasz */
int line_number = 1;/* used in canvas_error() ; keep track of line number in canvasdraw/fly - script */
/* set some variables to avoid trouble (NaN) in case of syntax and other usage errors */
int xsize = 320;
int ysize = 320;
double xmin = 0.0;
double xmax = 320.0;
double ymin = 0.0;
double ymax = 320.0;
double tmax = 0;
double tmin = 0;
/* flag to indicate parsing of line status */
int done = FALSE;
int type; /* eg command number */
int onclick = 0;
/*
0 = noninteractive ;
1 = onclick ;
2 = draggable ;
3 = click+slideable ;
4 = slideable without click
5 = draggable + slideable
*/
char *slider_type="0";
char *my_sliders = "[-1]";
int use_slider = -1;
int last_slider = 0;
double double_data[MAX_INT+1];
int int_data[MAX_INT+1];
int dragstuff[MAX_DRAGSTUFF];
int js_function[MAX_JS_FUNCTIONS]; /* javascript functions include objects on demand basis: only once per object type */
double affine_matrix[] = {1,0,0,1,0,0};
double moebius_matrix[]={0,0,0};
int use_affine = FALSE;
int use_rotate = FALSE;
int use_moebius = FALSE;
int use_filled = 0; /* 0:no fill, 1:fill,2=grid?,3=hatch?,4=diamond?,5=dot?,6=image? */
int use_dashed = FALSE; /* dashing not natively supported in firefox, for now... */
double angle = 0.0;
char buffer[MAX_BUFFER];/* contains js-functions with arguments ... all other basic code is directly printed into js-include file */
char *getfile_cmd = "";
int reply_format = 0;
unsigned int canvas_root_id;
char *css_class;
int font_size;
char *draw_type;
int jsplot_cnt = -1;     /* keepint track on the curve identity */
int input_cnt = 0;
int dashtype[2] = { 4 , 4 }; /* just line_px and space_px: may have more arguments...if needed in future */
char *function_label = "[\"f(x)=\",\"g(x)=\",\"h(x)=\"]";
int drag_type = -1;/* 0,1,2: xy,x,y */
int use_offset = 0;/* use_offset only for text shape objects... 0=none;1=yoffset;2=xoffset;3=xyoffset;4=centered*/
int linegraph_cnt = 0; /* identifier for command 'linegraph' ; multiple line graphs may be plotted in a single plot*/
int barchart_cnt = 0; /* identifier for command 'barchart' ; multiple charts may be plotted in a single plot*/
int legend_cnt = -1; /* to allow multiple legends to be used, for multiple piecharts etc  */
int use_axis = FALSE;
int use_axis_numbering = -1;
int no_reset = FALSE;

/******************************************************************************
** Main Program
******************************************************************************/
int main(int argc, char *argv[]){
  /* need unique id for every call to canvasdraw: rand(); is too slow...will result in many identical id's */
  struct timeval tv;struct timezone tz;gettimeofday(&tv, &tz);
  canvas_root_id = (unsigned int) tv.tv_usec;
  infile = stdin;/* read flyscript via stdin */
  int i,c;
  for(i=0;i<MAX_DRAGSTUFF;i++){dragstuff[i] = 0;}
  for(i=0;i<MAX_INT;i++){int_data[i]=0;double_data[i]=0;}
  int use_snap = 0; /* 0 = none 1=grid: 2=x-grid: 3=y-grid: 4=snap to points */
  int print_drag_params_only_once = FALSE;/* avoid multiple useless identical lines about javascript precision and use_dragdrop */
  int line_width = 2;
  int decimals = 2;
  int use_dragstuff = 0;
  int precision = 100; /* 10 = 1;100=2;1000=3 decimal display for mouse coordinates or grid coordinate.May be redefined before every object */
  int use_userdraw = 0; /* 0=none,1=userdraw,2=multidraw flag to indicate user interaction */
  int use_tooltip = -1; /* 1= tooltip 2= popup window*/
  int use_parametric = FALSE;/* will be reset after parametric plotting */
  char *tooltip_text = "Click here";
  char *temp = ""; /* */
  char *bgcolor = "";/* used for background of canvas_div ; default is tranparent */
  char *stroke_color = "255,0,0";
  char *fill_color = "255,255,255";
  char *font_family = "12px Arial"; /* commands xaxistext,yaxistext,legend,text/textup/string/stringup may us this */
  char *font_color = "#00000";
  draw_type = "points";
  char *fly_font = "normal";
  css_class = "none";
  char *flytext = "";
  int canvas_type = DRAG_CANVAS; /* to use a specific canvas  for filling etc */
  int pixelsize = 1;
  int fill_cnt = 0;
  int use_zoom = 0;
  font_size = 12;/* this may lead to problems when using something like <code>fontfamily Italic 24px Arial</code> the ''fontsize`` value is not substituted into fontfamily !! */
  int fly_font_size = 12; /*fly_font_size is relative to this... */
  for(i=0;i<MAX_JS_FUNCTIONS;i++){js_function[i]=0;}
  int arrow_head = 8; /* size in px needed for arrow based  userdraw:  "userdraw arrow,color" */
  int crosshair_size = 5; /* size in px*/
  int level_step = 1; /* resolution in pixels of level curves */
  int plot_steps = 250;/* the js-arrays with x_data_points and y_data_points will have size 250 each: use with care !!! use jscurve when precise plots are required  */
  int found_size_command = 0; /* 1 = found size ; 2 = found xrange; 3 = found yrange: just to flag an error message */
  int object_cnt = 0; /*counter to identify the "onclick" ojects ; 0 is first object set onclick: reply[object_cnt]=1 when clicked ; otherwise reply[object_cnt]=0 ; object_cnt is only increased when another object is set  again */
  int clock_cnt = 0; /* counts the amount of clocks used -> unique object clock%d */
  int boxplot_cnt = 0;
  int drawxml_cnt = 0;
  int numberline_cnt = 0;
  int snap_to_points_cnt = 0;
  int reply_precision = 100; /* used for precision of student answers / drawings */
  char *rotation_center = "null";/* needs to be removed... but is used for canvas CTX based rotation*/
  double rotationcenter[] = {0,0}; /* use for recalculating x/y values on rotation() */
  int use_animate = 0; /* used for jscurve / js parametric  */
  int use_input_xy = 0; /* 1= input fields 2= textarea 3=calc y value*/
  size_t string_length = 0; /* measure the size of the user input fly-string */
  double stroke_opacity = 0.95; /* use some opacity as default */
  double fill_opacity = 0.5;/* use some opacity as default */
  char *URL = "http://localhost/images";
  memset(buffer,'\0',MAX_BUFFER);
  void *tmp_buffer = "";
  double res[10];
  /* default writing a unzipped js-include file into wims getfile directory */
  char *w_wims_session = getenv("w_wims_session");
  if( w_wims_session == NULL || *w_wims_session == 0 ){canvas_error("Hmmm, your wims environment does not exist...\nCanvasdraw should be used within wims.");}
  int L0=strlen(w_wims_session) + 21;
  char *getfile_dir = my_newmem(L0); /* create memory to fit string precisely */
  snprintf(getfile_dir,L0, "../sessions/%s/getfile",w_wims_session);/* string will fit precisely  */
  mode_t process_mask = umask(0); /* check if file exists */
  int result = mkdir(getfile_dir, S_IRWXU | S_IRWXG | S_IRWXO);
  if( result == 0 || errno == EEXIST ){
    umask(process_mask); /* be sure to set correct permission */
    char *w_session = getenv("w_session");
    if( w_session == NULL || *w_session == 0 ){canvas_error("Hmmm, your wims environment does not exist...\nCanvasdraw should be used within wims.");}
    int L1 = (int) (strlen(w_session)) + find_number_of_digits(canvas_root_id) + 48;
    getfile_cmd = my_newmem(L1); /* create memory to fit string precisely */
    snprintf(getfile_cmd,L1,"wims.cgi?session=%s&cmd=getfile&special_parm=%d.js",w_session,canvas_root_id);/* extension ".gz" is MANDATORY for webserver */
    /* write the include tag to html page:<script src="wims.cgi?session=%s&cmd=getfile&special_parm=11223344_js"></script> */
    /* now write file into getfile dir*/
    char *w_wims_home = getenv("w_wims_home"); /* "/home/users/wims": we need absolute path for location */
    if( w_wims_home == NULL || *w_wims_home == 0 ){canvas_error("Hmmm, your wims environment does not exist...\nCanvasdraw should be used within wims.");}
    int L2 = (int) (strlen(w_wims_home)) + (int) (strlen(w_wims_session)) + find_number_of_digits(canvas_root_id) + 23;
    char *location = my_newmem(L2); /* create memory to fit string precisely */
    snprintf(location,L2,"%s/sessions/%s/getfile/%d.js",w_wims_home,w_wims_session,canvas_root_id);/*absolute path */
    js_include_file = fopen(location,"w");/* open the file location for writing */
    /* check on opening...if nogood: mount readonly? disk full? permissions not set correctly? */
    if(js_include_file == NULL){ canvas_error("SHOULD NOT HAPPEN: could not write to javascript include file...check your system logfiles !" );}

/* ----------------------------------------------------- */

/*  while more lines to process */

    while(!finished){
      if(line_number>1 && found_size_command == 0 && use_tooltip != 2 ){canvas_error("command \"size xsize,ysize\" needs to come first ! ");}
      type = get_token(infile);
      done = FALSE;
  /*
  @ canvasdraw
  @ Canvasdraw will try use the same basic syntax structure as flydraw
  @ General syntax <ul><li>The transparency of all objects can be controlled by command <a href="#opacity">opacity [0-255],[0,255]</a></li><li>Line width of any object can be controlled by command <a href="#linewidth">linewidth int</a></li><li>Any may be dashed by using keyword <a href="#dashed">dashed</a> before the object command.<br> the dashing type can be controled by command <a href="#dashtype">dashtype int,int</a>please note: dashing may have different spacing depending on the angle of the line<br>see https://wimsedu.info/?topic=dashed-arrows-not-dashed-in-canvasdraw</li><li>A fillable object can be set fillable by starting the object command with an ''f`` (like ''frect``,''fcircle``,''ftriangle`` ...) or by using the keyword <a href="#filled">filled</a> before the object command.<br>Non-solid filling (grid,hatch,diamond,dot,text) is provided using command <a href="#fillpattern">fillpattern a_pattern</a><br>note: do not use a <b>f</b> with this non-solid pattern filling.<br>For <a href="#filltoborder">filltoborder x0,y0,bordercolor,color</a> (fill a region around x0,y0 with color until a border of color borderolor is encountered) or <a href="#filltoborder">fill x0,y0,color</a> type filling (fill a region around x0,y0 with color until a border is encountered), there are non-solid pattern fill analogues:<ul><li><a href="#gridfill">gridfill x,y,dx,dy,color</a></li><li><a href="#hatchfill">hatchfill x,y,dx,dy,color</a></li><li><a href="#diamondfill">diamondfill x,y,dx,dy,color</a></li><li><a href="#dotfill">dotfill x,y,dx,dy,color</a></li><li><a href="#textfill">textfill x,y,color,sometext_or_char</a></li></ul></li><li>All draggable objects may have a <a href="#slider">slider</a> for translation / rotation; several objects may be translated / rotated by a single slider</li><li>A draggable object can be set draggable by a preceding command <a href="#drag">drag x/y/xy</a><br>The translation can be read by javascript:read_dragdrop();The replyformat is: object_number : x-orig : y-orig : x-drag : y-drag<br>The x-orig/y-orig will be returned in maximum precision (javascript float)...<br>the x-drag/y-drag will be returned in defined ''precision`` number of decimals<br>Multiple objects may be set draggable / clickable (no limit)<br>not all flydraw objects may be dragged / clicked<br>Only draggable / clickable objects will be scaled on <a href="#zoom">zoom</a> and will be translated in case of panning.</li><li>A ''onclick object`` can be set ''clickable`` by the preceding keyword <a href="#onclick">onclick</a><br>Not all flydraw objects can be set clickable</li><li><b>Remarks using a '';`` as command separator</b>. Commands with only numeric or color arguments may be using a '';`` as command separator (instead of a new line). Commands with a string argument may not use a '';`` as command separator.<br>These exceptions are not really straight forward... so keep this in mind.</li><li>Almost every <a href="#userdraw">userdraw object,color</a> or <a href="#multidraw">multidraw</a> command ''family`` may be combined with keywords <a href="#snaptogrid">"snaptogrid | xsnaptogrid | ysnaptogrid | snaptofunction</a> or command <code>snaptopoints x1,y1,x2,y2,...</code></li><li>Every draggable | onclick object may be combined with keywords <a href="#snaptogrid">snaptogrid | xsnaptogrid | ysnaptogrid | snaptofunction</a> or command <code>snaptopoints x1,y1,x2,y2,...</code></li><li>Almost every command for a single object has a multiple objects counterpart: <ul>general syntax rule:<li><code>object x1,y1,...,color</code></li><li><code>objects color,x1,y1,...</code></li></ul><li>All inputfields or textareas generated, can be styled individually using command <a href="#css">css some_css</a>: the fontsize used for labeling these elements can be controlled by command <a href="#fontsize">fontsize int</a> command <code>fontfamily</code> is <b>not</b> active for these elements.</li></ul>
  @ If needed multiple interactive scripts (*) may be used in a single webpage.<br>A function <code>read_canvas()</code> and / or <code>read_dragdrop()</code> can read all interactive userdata from these images.<br>The global array <code>canvas_scripts</code>will contain all unique random "canvas_root_id" of the included scripts.<br>The included local javascript "read" functions ''read_canvas%d()`` and ''read_dragdrop%d()`` will have this ''%d = canvas_root_id``<br>e.g. canvas_scripts[0] will be the random id of the first script in the page and will thus provide a function<br><code>fun = eval("read_canvas"+canvas_scripts[0])</code> to read user based drawings / inputfield in this first image.<br>The read_dragdrop is analogue.<br>If the default reply formatting is not suitable, use command <a href='#replyformat'>replyformat</a> to format the replies for an individual canvas script,<br>To read all user interactions from all included canvas scripts, use something like:<br><code>function read_all_canvas_images(){<br>&nbsp;var script_len = canvas_scripts.length;<br>&nbsp;var draw_reply = "";<br>&nbsp;var found_result = false;<br>&nbsp;for(var p = 0 ; p < script_len ; p++){<br>&nbsp;&nbsp;var fun = eval("read_canvas"+canvas_scripts[p]);<br>&nbsp;&nbsp;if( typeof fun === 'function'){<br>&nbsp;&nbsp;&nbsp;var result = fun();<br>&nbsp;&nbsp;&nbsp;if( result&nbsp;&nbsp;&& result.length != 0){<br>&nbsp;&nbsp;&nbsp;&nbsp;if(script_len == 1 ){.return result;};<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;found_result = true;<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;draw_reply = draw_reply + result + "newline";<br>&nbsp;&nbsp;&nbsp;&nbsp;};<br>&nbsp;&nbsp;&nbsp;};<br>&nbsp;&nbsp;};<br>&nbsp;if( found_result ){return draw_reply;}else{return null;};<br>};</code> <br>(*) Note: the speed advantage over <em>canvas-do-it-all</em> libraries is reduced to zero, when multiple canvasdraw scripts are present in a single page... For example a typical canvasdraw script is between 5 and 40 kB...a large library like JSXgraph is approx 600 kB. In these cases it would be much faster to load a static general HTML5 canvas javascript draw library and parse it multiple raw fly instructions.
  @ Canvasdraw can be used to paint a html5 bitmap image by generating a tailor-made javascript include file: providing only the js-functionality needed to perform the job, thus ensuring a minimal strain on the client browser (unlike some popular ''canvas-do-it-all`` libraries, who have proven to be not suitable for low-end computers found in schools...)
  @ You can check the javascript reply format in the wims tool <a href="http://localhost/wims/wims.cgi?lang=en&module=tool/directexec">direct exec</a>
  @ For usage within OEF (without anstype ''draw``), something like this (a popup function plotter) will work:<br><code>\\text{popup_grapher=wims(exec canvasdraw <br>popup<br>size 400,400<br>xrange -10,10<br>yrange -10,10<br>axis<br>axisnumbering<br>opacity 100,100<br>grid 2,2,grey,2,2,6,black<br>snaptogrid<br>linewidth 2<br>jsplot red,5*sin(1/x)<br>strokecolor green<br>functionlabel f(x)=<br>userinput function<br>mouse blue,22<br>)<br>}<br>\\statement{<br>\\popup_grapher<br>}</code>.
  @ Be aware that older browsers will probably not work correctly. no effort has been undertaken to add glue code for older browsers !! In any case it is not wise to use older browsers...not just for canvasdraw.
  @ Be aware that combining several different objects and interactivity may lead to problems.
  @ If you find flaws, errors or other incompatibilities -not those mentioned in this document- send <a href='mailto:jm.evers-at-schaersvoorde.nl'>me</a> an email with screenshots and the generated javascript include file.
  @ There is limited support for touch devices: touchstart, touchmove and touchend in commands <a href="#userdraw">userdraw primitives </a>, <a href="#multidraw">multidraw primitives </a>, <a href="#protractor">protractor</a>, <a href="#ruler">ruler</a> and probably a few others... Only single finger gestures are supported (for now). The use of a special pen is advised for interactive drawing on touch devices. For more accurate user-interaction (numeric, eg keyboard driven drawings) with canvasdraw on touch devices: use the command family <a href="#userinput_xy">userinput</a>.
  */
      switch(type){
        case END:finished = 1;done = TRUE;break;
        case 0:sync_input(infile);break;
        case AFFINE:
  /*
  @ affine a,b,c,d,tx,ty
  @ defines a transformation matrix for subsequent objects
  @ use keyword <a href='#killaffine'>killaffine</a> to end the transformation...the next objects will be drawn in the original x/y-range
  @ a: Scales the drawings horizontally
  @ b: Skews the drawings horizontally
  @ c: Skews the drawings vertically
  @ d: Scales the drawings vertically
  @ tx: Moves the drawings horizontally in xrange coordinate system
  @ ty: Moves the drawings vertically in yrange coordinate system
  @ the data precision may be set by preceding command ''precision int``
  @ <b>note</b>: not all affine operations on canvasdraw objects will be identical to flydraw's behaviour. Make sure to check !
  @%affine%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%#use larger linewith to improve selecting a draggable object !%linewidth 4%drag xy%frect 0,5,3,3,red%affine cos(pi/4),sin(pi/4),-cos(pi/2),sin(pi/2),-5,-5%drag xy%frect 0,5,3,3,red
  @%affine+latex%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%strokecolor blue%fontfamily 42px Arial%centered%drag xy%latex 5,5,\\frac{123}{\\pi^{234}}%affine cos(pi/4),sin(pi/4),-cos(pi/2),sin(pi/2),-5,-5%drag xy%latex 5,5,\\frac{123}{\\pi^{234}}
  */
          for(i = 0 ; i<6;i++){
            switch(i){
              case 0: affine_matrix[0] = get_real(infile,0);break;
              case 1: affine_matrix[1] = get_real(infile,0);break;
              case 2: affine_matrix[2] = get_real(infile,0);break;
              case 3: affine_matrix[3] = get_real(infile,0);break;
              case 4: affine_matrix[4] = get_real(infile,0);break;
              case 5: affine_matrix[5] = get_real(infile,1);
              use_affine = TRUE;
              break;
              default: break;
            }
          }
          break;
        case ALLOW_DUPLICATES:
  /*
  @ duplicates || allowdups
  @ keyword (no arguments)
  @ only useful in case of a <a href="#multidraw">multidraw</a> student reply.
  @ only useful in default <a href="#replyformat">replyformat</a> (eg in case of a not specified replyformat).
  @ if set, duplicate (x:y) coordinates will not be removed from the student reply.
  @ technical: a javascript variable "allow_duplicate_answer = 1;" is declared.
  @ default for command multidraw is: removal of duplicates.
  */
          fprintf(js_include_file,"var allow_duplicate_answers = 1;");
          break;
        case ANGLE:
  /*
  @ angle xc,yc,width,start_angle,end_angle,color
  @ width is in x-range
  @ angles are in degrees
  @ not compatible with ''flydraw``
  @ will zoom in/out
  @ may be set onclick or drag&amp;drop
  @ if angle size is controlled by command <a href='#slider'>slider</a>, use radians to set limits of slider
  @ ''angle`` and <a href="#arc">arc</a>  are exceptions in case of sliders...they are always active (e.g. not click-activated)
  @%angle%size 400,400%xrange -10,10%yrange -10,10%filled%fillcolor orange%angle 0,0,4,10,135,blue
  @%angle_slider%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 3%strokecolor blue%slider -2*pi,2*pi,260,28,angle active degree,blue arrow%arrow 0,0,8,0,8,blue%fillpattern diamond%angle 0,0,2,0,0,blue%killslider%strokecolor red%slider -2*pi,2*pi,260,28,angle active radian,red arrow%arrow 0,1,8,1,8,red%fillpattern dot%angle 0,1,2,0,0,red
  */
          for(i=0;i<7;i++){
            switch(i){
              case 0:double_data[0] = get_real(infile,0);break; /* x-values */
              case 1:double_data[1] = get_real(infile,0);
              if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
              if(use_affine == TRUE ){ transform(2,2);}
              break; /* y-values */
              case 2:double_data[2] = get_real(infile,0);break; /* width x-range ! */
              case 3:double_data[3] = 0.0174532925*(get_real(infile,0) - angle);break; /* start angle in degrees -> radians  */
              case 4:double_data[4] = 0.0174532925*(get_real(infile,0) - angle);break; /* end angle in degrees -> radians */
              case 5:stroke_color = get_color(infile,1);/* name or hex color */
              if( use_slider != -1 ){ onclick = 3; }/* always active in case of slider */
              decimals = find_number_of_digits(precision);
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,17,[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[0],decimals,double_data[1],decimals,double_data[1],decimals,double_data[2],decimals,double_data[2],decimals,double_data[3],decimals,double_data[4],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
              add_to_buffer(tmp_buffer);
              dragstuff[17] = 1;
              if(use_dragstuff == 0 ){ use_dragstuff = 1; }
              if(onclick != 0){object_cnt++;}/* object_cnt++;*/
              break;
            }
          }
          break;
        case ANIMATE:
  /*
  @ animate
  @ keyword
  @ if set, it will animate a point on a curve
  @ all other canvas object or group of objects (like lines,circles,rects,points...images,svg,latex,mathml etc)<br>may be animated using command <a href='#slider'>slider</a> with keyword 'anim'
  @ the animated point is a filled rectangle ; adjust color with command <code>fillcolor colorname/hexnumber</code>
  @ use linewidth to adjust size of the points
  @ will animate a point on -only- the next <a href='#jsplot'>jsplot/jscurve command</a>. Only a single call to <code>animate</code> is allowed...in case of multiple <code>animate</code> keywords, only the last one is valid
  @ only usable in combination with command <a href='#jsplot'>jsplot</a> (normal functions or parametric)
  @ moves repeatedly from <a href='#xrange'>xmin to xmax</a> or in case of a parametric function from <a href='#trange'>tmin to tmax</a>
  @ use commands <a href='#multilinewidth'>multilinewidth</a>, <a href='#multistrokecolor'>multistrokecolor</a> etc in case of multiple animated functions.<br>use multiple functions as argument in a single call to <a href='#jsplot'>jsplot color,fun1,fun2,fun3...fun_n</a>
  @%animate_1%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%precision 100%linewidth 4%fillcolor red%animate%trange -2*pi,2*pi%linewidth 1%opacity 255,50%canvastype 100%fill 1.2,1.2,red%canvastype 101%fill -1.2,-1.2,blue%jsplot blue,7*cos(x),5*sin(2*x)
  @%animate_2%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%precision 100%linewidth 4%fillcolor red%animate%trange -2*pi,2*pi%linewidth 1%opacity 255,50%canvastype 100%fill 1.2,1.2,red%canvastype 101%fill -1.2,-1.2,blue%multistrokecolors blue,blue,green,green,orange,orange%multilinewidth 2,2,3,3,1,1%jsplot blue,7*cos(x),5*sin(2*x),9*sin(x),5*cos(x),x^2,x
  */
          use_animate++;
          if( use_animate == 1 ){
            fprintf(js_include_file,"\nvar trace_canvas  = create_canvas%d(%d,xsize,ysize);\
              var trace_ctx = trace_canvas.getContext('2d');\
              trace_ctx.fillStyle = 'rgba(%s,%f)';\
              trace_ctx.strokeStyle = 'rgba(%s,%f)';\
              trace_ctx.lineWidth = %d;var anim_pos = 0;\n\
              function animate_this(){\
              var sync;\
              var synchrone = Math.floor(animation_steps/animation_funs);\
              trace_ctx.clearRect(0,0,xsize,ysize);\
              for(var p=0; p<animation_funs;p++){\
               sync = p*synchrone;\
               trace_ctx.fillRect(x_anim_points[sync+anim_pos]-%d, y_anim_points[sync+anim_pos]-%d,%d,%d);\
              };\
              setTimeout(function(){\
               requestAnimationFrame(animate_this);  anim_pos++;}, 50\
              );\
              if(anim_pos >= animation_steps){anim_pos = 0;};\
              };",canvas_root_id,ANIMATE_CANVAS,fill_color,fill_opacity,stroke_color,stroke_opacity,line_width,line_width,line_width,2*line_width,2*line_width);
          } else {
            canvas_error("animate can only be used once<br>multiple curves may be animated using something like:<br>jsplot red,sin(x),cos(x),x^2,sin(2*x)");
          }
          break;
        case ARC:
  /*
  @ arc xc,yc,x-width,y-height,start_angle,end_angle,color
  @ may be set ''onclick`` or ''drag xy``
  @ compatible with ''flydraw``
  @ attention: width &amp; height in x/y-range
  @ for arrow hats on an arc, see  command <a href='#arcarrow'>arcarrow or arrowarc</a>
  @ better use command <a href='#angle'>angle</a> for use with a <a href='#slider'>slider</a>
  @%arc%size 400,400%xrange -10,10%yrange -10,10%arc 0,0,4,4,10,135,red
  @%arc_filled%size 400,400%xrange -10,10%yrange -10,10%opacity 255,60%filled%fillcolor green%arc 0,0,4,4,10,135,red
  */
          if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
          for(i=0;i<7;i++){
            switch(i){
              case 0:double_data[0] = get_real(infile,0);break; /* x-values */
              case 1:double_data[1] = get_real(infile,0);
              if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
              if(use_affine == TRUE ){ transform(4,2);}
              break; /* y-values */
              case 2:double_data[2] = get_real(infile,0);break; /* width x-range no pixels ! */
              case 3:double_data[3] = get_real(infile,0);
              break; /* height y-range no pixels ! */
              case 4:double_data[4] = get_real(infile,0) - angle ;break; /* start angle in degrees */
              case 5:double_data[5] = get_real(infile,0) - angle;break; /* end angle in degrees */
              case 6:stroke_color = get_color(infile,1);/* name or hex color */
        /* in Shape library:
      x[0] = x[1] = xc = double_data[0]
      y[0] = y[1] = yc = double_data[1]
      w[0] = width = double_data[2]
      w[1] = height = double_data[3]
      h[0] = start_angle = double_data[4]
      h[1] = end_angle = double_data[5]
        */
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                decimals = find_number_of_digits(precision);

                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,12,[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[0],decimals,double_data[1],decimals,double_data[1],decimals,double_data[2],decimals,double_data[3],decimals,double_data[4],decimals,double_data[5],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);reset();
                dragstuff[12] = 1;
                if(onclick != 0){object_cnt++;}
                if(use_dragstuff == 0 ){ use_dragstuff = 1; }
              break;
            }
          }
          break;
        case ARCARROW:
  /*
  @ arrowarc xc,yc,x-width,y-height,start_angle,end_angle,color,type
  @ alternative: arcarrow
  @ uses same syntax as <a href='#arc'>arc</a>
  @ for arrow hat: type = 1 : right<br>type = 2 : left<br>type = 3 : left&amp;right
  @ if the default arrow hat/head is not satisfactory , the size of the arrow may be set with command <a href='#arrowhead'>arrowhead</a>
  @ no other arrow types are implemented...yet
  @ may be set draggable or onclick
  @ attention: when width ad height are very different, the arrow hat is not drawn correctly. This is a flaw and not a feature...(for now: too much calculations to correct)
  @%arcarrow%size 400,400%xrange -10,10%yrange -10,10%noreset%onclick%arcarrow 0,0,7,7,50,275,blue,3%arcarrow 0,0,8,8,50,275,red,2%arcarrow 0,0,9,9,50,275,green,1
  */
          for(i=0;i<8;i++){
            switch(i){
              case 0:double_data[0] = get_real(infile,0);break; /* x-values */
              case 1:double_data[1] = get_real(infile,0);
                if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(4,2);}
                break; /* y-values */
              case 2:double_data[2] = get_real(infile,0);break; /* width x-range no pixels ! */
              case 3:double_data[3] = get_real(infile,0);
                break; /* height y-range no pixels ! */
              case 4:double_data[4] = get_real(infile,0) - angle ;break; /* start angle in degrees */
              case 5:double_data[5] = get_real(infile,0) - angle;break; /* end angle in degrees */
              case 6:stroke_color = get_color(infile,0);/* name or hex color */
                break;
              case 7:int_data[0] = (int) get_real(infile,1);
                switch(int_data[0]){
                  case 1: int_data[1] = 24;break; /* right */
                  case 2: int_data[1] = 25;break; /* left */
                  case 3: int_data[1] = 26;break; /* left&right */
                  default:int_data[1] = 24;break;
                }
              if(int_data[0] == 1 ){int_data[1] = 24;}
              if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
              decimals = find_number_of_digits(precision);

              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"var arrow_head = %d;dragstuff.addShape(new Shape(%d,%d,%d,%d,%d,[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",arrow_head,drag_type,object_cnt,onclick,use_snap,int_data[1],decimals,double_data[0],decimals,double_data[0],decimals,double_data[1],decimals,double_data[1],decimals,double_data[2],decimals,double_data[3],decimals,double_data[4],decimals,double_data[5],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
              add_to_buffer(tmp_buffer);
              dragstuff[int_data[1]] = 1;
              if(onclick != 0){object_cnt++;}
              if(use_dragstuff == 0 ){ use_dragstuff = 1; }
              js_function[JS_ARROWHEAD] = 1;
              reset();
            }
          }
          break;
        case ARROW:
  /*
  @ arrow x1,y1,x2,y2,h,color
  @ alternative: vector
  @ draw a single headed arrow / vector from (x1:y1) to (x2:y2)<br>with arrowhead size h in px and in color ''color``
  @ use command <code>linewidth int</code> to adjust thickness of the arrow
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%arrow_drag%size 400,400%xrange -10,10%yrange -10,10%cursor move%linewidth 2%drag xy%arrow 0,0,4,3,8,blue%drag xy%arrow 0,0,-4,3,8,green%drag xy%arrow 0,0,4,-3,8,orange%drag xy%arrow 0,0,-4,-3,8,cyan
  @%arrow_click%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%%onclick%arrow 0,0,4,4,8,blue%onclick%arrow 0,0,-4,5,8,green%onclick%arrow 0,0,4,-6,8,orange%onclick%arrow 0,0,-4,-2,8,cyan
  */
          for(i=0;i<6;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x */
              case 1: double_data[1] = get_real(infile,0);break; /* y */
              case 2: double_data[2] = get_real(infile,0);break; /* x */
              case 3: double_data[3] = get_real(infile,0);break; /* y */
              case 4: arrow_head = (int) get_real(infile,0);break;/* h */
              case 5: stroke_color = get_color(infile,1);/* name or hex color */
                if(use_rotate == TRUE ){rotate(4,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(4,2);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,8,[%.*f,%.*f],[%.*f,%.*f],[%d,%d],[%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[1],decimals,double_data[3],arrow_head,arrow_head,arrow_head,arrow_head,line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                if(onclick != 0){object_cnt++;}
          /* object_cnt++;*/
                add_to_buffer(tmp_buffer);reset();
                dragstuff[8] = 1;
                if(use_dragstuff == 0 ){ use_dragstuff = 1; }
              break;
            }
          }
          break;
        case ARROWS:
  /*
  @ arrows color,head (px),x1,y1,x2,y2...x_n,y_n
  @ alternative: vectors
  @ draw single headed arrows / vectors from (x1:y1) to (x2:y2) ... (x3:y3) to (x4:y4) etc ... in color 'color'
  @ use command <code>linewidth int</code> to adjust thickness of the arrow
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually
  @%arrows_click%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%onclick%arrows red,8,0,0,4,3,0,0,2,4,0,0,-2,4,0,0,-3,-4,0,0,3,-2%
  @%arrows_drag%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%drag xy%arrows red,8,0,0,4,3,0,0,2,4,0,0,-2,4,0,0,-3,-4,0,0,3,-2%
  @%arrows_drag_slider%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%drag xy%# Click arrow(s) to activate %slider 0,2*pi,300,30,angle degrees,Rotate%slider -5,5*pi,300,30,x display,move in x-direction%slider -10,10*pi,300,30,y display,move in y-direction%arrows red,8,0,0,4,3,0,0,2,4,0,0,-2,4,0,0,-3,-4,0,0,3,-2%
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          arrow_head = (int) get_real(infile,0);/* h */
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            } else {
              double_data[i] = get_real(infile,1); /* y */
            }
          i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if( use_affine == TRUE ){ transform(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          decimals = find_number_of_digits(precision);
          for(c = 0 ; c+3 < i ; c = c+4){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,8,[%.*f,%.*f],[%.*f,%.*f],[%d,%d],[%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+2],decimals,double_data[c+1],decimals,double_data[c+3],arrow_head,arrow_head,arrow_head,arrow_head,line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}/* object_cnt++; */
          }
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          dragstuff[8] = 1;
          reset();
          break;
        case ARROW2:
  /*
  @ arrow2 x1,y1,x2,y2,h,color
  @ draw a double headed arrow/vector from (x1:y1) to (x2:y2)<br>with arrowhead size h in px and in color ''color``
  @ use command <code>arrowhead int</code> to adjust the arrow head size
  @ use command <code>linewidth int</code> to adjust thickness of the arrow
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%arrow2%size 400,400%xrange -10,10%yrange -10,10%drag xy%arrow2 0,0,4,3,8,blue%
  */
          for(i=0;i<6;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x */
              case 1: double_data[1] = get_real(infile,0);break; /* y */
              case 2: double_data[2] = get_real(infile,0);break; /* x */
              case 3: double_data[3] = get_real(infile,0);break; /* y */
              case 4: arrow_head = (int) get_real(infile,0);break;/* h */
              case 5: stroke_color = get_color(infile,1);/* name or hex color */
                if(use_rotate == TRUE ){rotate(4,angle,rotationcenter,2);}
                if( use_affine == 1 ){ transform(4,2);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,10,[%.*f,%.*f],[%.*f,%.*f],[%d,%d],[%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[1],decimals,double_data[3],arrow_head,arrow_head,arrow_head,arrow_head,line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}/* object_cnt++;*/
                if(use_dragstuff == 0 ){ use_dragstuff = 1; }
                dragstuff[10] = 1;
                reset();
                break;
            }
          }
          break;
        case ARROWS2:
  /*
  @ arrows2 color,head (px),x1,y1,x2,y2...x_n,y_n
  @ draw double headed arrows / vectors from (x1:y1) to (x2:y2) ... (x3:y3) to (x4:y4) etc ... in color ''color``
  @ use command <code>linewidth int</code> to adjust thickness of the arrows
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually
  @%arrows2%size 400,400%xrange -10,10%yrange -10,10%onclick%arrows2 red,8,0,0,4,3,1,1,2,4,2,2,-2,4,3,3,-3,-4,0,0,3,-2%
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          arrow_head = (int) get_real(infile,0);/* h */
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            } else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if( use_affine == 1 ){ transform(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */

          decimals = find_number_of_digits(precision);
          for(c = 0 ; c+3 < i ; c = c+4){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,10,[%.*f,%.*f],[%.*f,%.*f],[%d,%d],[%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+2],decimals,double_data[c+1],decimals,double_data[c+3],arrow_head,arrow_head,arrow_head,arrow_head,line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}/* object_cnt++; */
          }
          dragstuff[10] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case ARROWHEAD:
  /*
  @ arrowhead int
  @ default 8 (pixels)
  */
          arrow_head = (int) (get_real(infile,1));
          break;
        case AUDIO:
  /*
  @ audio x,y,w,h,loop,visible,audiofile location
  @ x,y: left top corner of audio element (in xrange / yrange)
  @ w,y: width and height in pixels
  @ loop: 0 or 1 ( 1 = loop audio fragment)
  @ visible: 0 or 1 (1 = show controls)
  @ audio format may be in *.mp3 or *.ogg
  @ If you are using *.mp3: be aware that FireFox will not (never) play this ! (Pattented format)
  @ if you are using *.ogg: be aware that Microsoft based systems not support it natively
  @ To avoid problems supply both types (mp3 and ogg) of audiofiles.<br>the program will use both as source tag
  */
          js_function[DRAW_AUDIO] = 1;
          for(i=0;i<7;i++){
            switch(i){
              case 0: int_data[0] = x2px(get_real(infile,0)); break; /* x in x/y-range coord system -> pixel */
              case 1: int_data[1] = y2px(get_real(infile,0)); break; /* y in x/y-range coord system  -> pixel */
              case 2: int_data[2] = (int) (get_real(infile,0)); break; /* pixel width */
              case 3: int_data[3] = (int) (get_real(infile,0)); break; /* height pixel height */
              case 4: int_data[4] = (int) (get_real(infile,0)); if(int_data[4] != TRUE){int_data[4] = FALSE;} break; /* loop boolean */
              case 5: int_data[5] = (int) (get_real(infile,0)); if(int_data[5] != TRUE){int_data[5] = FALSE;} break; /* visible boolean */
              case 6:
                temp = get_string(infile,1);
                if( strstr(temp,".mp3") != 0 ){ temp = str_replace(temp,".mp3","");}
                if( strstr(temp,".ogg") != 0 ){ temp = str_replace(temp,".ogg","");}
                tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_audio(%d,%d,%d,%d,%d,%d,%d,\"%s.ogg\",\"%s.mp3\");\n", canvas_root_id, int_data[0],int_data[1],int_data[2],int_data[3],int_data[4],int_data[5],temp,temp));
                add_to_buffer(tmp_buffer);
                break;
              default:break;
            }
          }
          reset();
          break;
        case AXIS_NUMBERING:
  /*
  @ axisnumbering
  @ keyword (no arguments required)
  @ for special numbering of x-axis or y-axis see grid related commands <a href="#axis">axis</a> <a href="#xaxis">xaxis</a>, <a href="#xaxisup">xaxisup</a>, <a href="#noxaxis">noxaxis</a>, <a href="#yaxis">yaxis</a>, <a href="#yaxisup">yaxisup</a>, <a href="#noyaxis">noyaxis</a>
  @ to be used before command grid (see <a href="#grid">command grid</a>)
  */
          use_axis_numbering++;
          break;
        case AXIS:
  /*
  @ axis
  @ keyword (no arguments required)
  @ to be used before command grid (see <a href="#grid">command grid</a>)

  */
          use_axis = TRUE;
          break;
        case BARCHART:
  /*
  @ barchart x_1:y_1:color_1:x_2:y_2:color_2:...x_n:y_n:color_n
  @ may <b>only</b> to be used together with command <a href='#grid'>grid</a>
  @ can be used together with freestyle x-axis/y-axis texts: see commands <a href='#xaxis'>xaxis</a>,<a href='#xaxisup'>xaxisup</a> and <a href='#yaxis'>yaxis</a>
  @ use command <a href='#legend'>legend</a> to provide an optional legend in right-top-corner
  @ multiple barchart command may be used in a single script
  @ also see command <a href='#piechart'>piechart</a>
  @ note: your arguments are not checked by canvasdraw: use your javascript console in case of trouble...
  @%barchart%size 400,400%xrange -1,10%yrange -2,14%legend legend Z:legend A:this is B:C:D:E:F:G:H:X%legendcolors green:red:orange:lightblue:cyan:gold:purple:darkred:yellow:lightgreen%xaxis 0:Z:1:A:2:B:3:C:4:D:5:E:6:F:7:G:8:H:9:X%noyaxis%precision 1%fontfamily bold 15px Arial%grid 1,1,white%barchart 0:5.5:green:2:5.5:red:4:6.5:orange:6:8:lightblue:8:11:cyan:1:5.5:gold:3:9:purple:5:4:darkred:7:7:yellow:9:1:lightgreen%mouse red,14
  */
          temp = get_string(infile,1);
          if( strstr( temp,":" ) != 0 ){ temp = str_replace(temp,":","\",\""); }
          fprintf(js_include_file,"var barchart_%d = [\"%s\"];",barchart_cnt,temp);
          barchart_cnt++;
          reset();
          break;
        case BEZIER:
  /*
  @ bezier color,x_start,y_start,x_first,y_first,x_second,y_second,x_end,y_end
  @ draw a bezier curve between points, starting from (x_start:y_start)
  @ can <b>not</b> be dragged or set onclick
  */
          js_function[DRAW_BEZIER] = 1;
          decimals = find_number_of_digits(precision);
          for(i = 0 ; i < 9; i++){
            switch(i){
              case 0: stroke_color = get_color(infile,0);break;
              case 1: double_data[0] = get_real(infile,0);break;/* start x */
              case 2: double_data[1] = get_real(infile,0);break;/* start y */
              case 3: double_data[2] = get_real(infile,0);break;/*The x-coordinate of the first Bézier control point */
              case 4: double_data[3] = get_real(infile,0);break;/*The y-coordinate of the first Bézier control point */
              case 5: double_data[4] = get_real(infile,0);break;/*The x-coordinate of the second Bézier control point */
              case 6: double_data[5] = get_real(infile,0);break;/*The y-coordinate of the second Bézier control point */
              case 7: double_data[6] = get_real(infile,0);break;/*The x-coordinate of the Bézier end point */
              case 8: double_data[7] = get_real(infile,1);/*The y-coordinate of the Bézier end point */
                if(use_rotate == TRUE ){rotate(8,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(2,5);}
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"draw_bezier(%d,%d,[%f,%f,%f,%f,%f,%f,%f,%f],\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.2f);",STATIC_CANVAS,line_width,double_data[0],double_data[1],double_data[2],double_data[3],double_data[4],double_data[5],double_data[6],double_data[7],fill_color,fill_opacity,stroke_color,stroke_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle));
                add_to_buffer(tmp_buffer);
                break;
              default: break;
            }
          }
          reset();
          break;
        case BGCOLOR:
  /*
  @ bgcolor colorname or #hex
  @ use this color as background of the "div" containing the canvas(es)
  @%bgcolor%size 400,400%xrange -10,10%yrange -10,10%bgcolor lightblue
  */
  /* [255,255,255]*/
          bgcolor = get_string(infile,1);
          if(strstr(bgcolor,"#") == NULL){ /* convert colorname -> #ff00ff */
            int found = 0;
            for( i = 0; i < NUMBER_OF_COLORNAMES ; i++ ){
              if( strcmp( colors[i].name , bgcolor ) == 0 ){
                bgcolor = colors[i].hex;
                found = 1;
              break;
              }
            }
            if(found == 0){canvas_error("your bgcolor is not in my rgb.txt data list: use hexcolor...something like #a0ffc4");}
          }
          fprintf(js_include_file,"/* set background color of canvas div */\ncanvas_div.style.backgroundColor = \"%s\";canvas_div.style.opacity = %f;\n",bgcolor,fill_opacity);
          break;
        case BGIMAGE:
  /*
  @ bgimage image_location
  @ use an image as background; technical: we use the background of ''canvas_div``
  @ the background image will be resized to match "width = xsize" and "height = ysize"
  @%bgimage%size 400,400%xrange -10,10%yrange -10,10%bgimage https://wims.unice.fr/wims/gifs/en.gif
  */
          URL = get_string(infile,1);
          fprintf(js_include_file,"/* set background image to canvas div */\ncanvas_div.style.backgroundImage = \"url(%s)\";canvas_div.style.backgroundSize = \"%dpx %dpx\";\n",URL,xsize,ysize);
          break;
        case BLINK:
  /*
  @ blink time(seconds)
  @ NOT IMPLEMETED -YET
  */
          break;
        case BOXPLOT:
  /*
  @ boxplot x_or_y,box-height_or_box-width,position,min,Q1,median,Q3,max
  @ example:<br><code>xrange 0,300<br>yrange 0,10<br>boxplot x,4,8,120,160,170,220,245</code><br>meaning: create a boxplot in x-direction, with height 4 (in yrange) and centered around line y=8
  @ example:<br><code>xrange 0,10<br>yrange 0,300<br>boxplot y,4,8,120,160,170,220,245</code><br>meaning: create a boxplot in y-direction, with width 4 (in xrange) and centered around line x=8
  @ use command <a href='#filled'>filled</a> to fill the box<br><b>note:</b> the strokecolor is used for filling Q1, the fillcolor is used for filling Q3
  @ use command <a href='#fillpattern'>fillpattern some_pattern</a> to use a (diamond for Q1, hatch for Q3) pattern.
  @ use command <a href='#opacity'>opacity</a> to adjust fill_opacity of stroke and fill colors
  @ use command <a href='#legend'>legend</a> to automatically create a legend <br>unicode allowed in legend<br>use command <a href='#fontfamily'>fontfamily</a> to set the font of the legend.
  @ there is no limit to the number of boxplots used.
  @ can <b>not</b> be set draggable and <a href='#onclick'>onclick</a> is not ready yet
  @ use keyword <a href="#userboxplot">userboxplot</a> before command boxplot, if a pupil must draw a boxplot (using his own min,Q1,median,Q3,max data)
  @ use keyword <a href="#userboxplotdata">userboxplotdata</a> before command boxplot, if a pupil must generate the data by some means.
  @ use command <a href="#boxplotdata">boxplotdata</a> when the boxplot should be drawn from wims-generated raw statistical date
  @%boxplot_1%size 400,400%xrange 0,300%yrange 0,10%opacity 120,50%filled%fillcolor orange%strokecolor blue%linewidth 2%boxplot x,4,8,120,160,170,220,245
  @%boxplot_2%size 400,400%xrange 0,10%yrange 0,300%opacity 120,50%filled%fillcolor orange%strokecolor blue%linewidth 2%boxplot y,4,8,120,160,170,220,245
  @%boxplot_3%size 400,400%xrange 0,100%yrange 0,10%fillpattern hatch%linewidth 3%fillcolor red%strokecolor green%boxplot x,1,2,4,14,27,39,66%strokecolor blue%boxplot x,1,4,15,45,50,66,87%strokecolor red%boxplot x,1,6,45,70,80,90,100%strokecolor orange%boxplot x,1,8,28,38,48,56,77%mouse red,16
  */
          js_function[DRAW_BOXPLOT] = 1;
          for(i=0;i<8;i++){
            switch(i){
              case 0: temp = get_string_argument(infile,0);
                if( strstr(temp,"x") != 0){int_data[0] = 1;}else{int_data[0] = 0;} break; /* x or y */
              case 1: double_data[0] = get_real(infile,0);break;/* height | width  */
              case 2:
                if( js_function[DRAW_JSBOXPLOT] == 0 ){
                  double_data[1] = get_real(infile,0);
                  fprintf(js_include_file,"var boxplot_source = 0;\n");/* we use given min,Q1,median,Q3,max */
                }
                else {
                  double_data[1] = get_real(infile,1);
                  double_data[2] = 1;
                  double_data[3] = 1;
                  double_data[4] = 1;
                  double_data[5] = 1;
                  double_data[6] = 1;
                  double_data[7] = 1;
                  i=8;
                }
                break;/* center value x or y */
              case 3: double_data[2] = get_real(infile,0); break;/* min */
              case 4: double_data[3] = get_real(infile,0); break;/* Q1 */
              case 5: double_data[4] = get_real(infile,0); break;/* median */
              case 6: double_data[5] = get_real(infile,0); break;/* Q3 */
              case 7: double_data[6] = get_real(infile,1); break;/* max */
              default:break;
            }
          }
          decimals = find_number_of_digits(precision);
          /*function draw_boxplot(canvas_type,xy,hw,cxy,data,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype0,dashtype1)*/
          tmp_buffer=my_newmem(MAX_BUFFER);
          check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"draw_boxplot(%d,%d,%.*f,%.*f,[%.*f,%.*f,%.*f,%.*f,%.*f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d);\n",BOXPLOT_CANVAS+boxplot_cnt,int_data[0],decimals,double_data[0],decimals,double_data[1],decimals,double_data[2],decimals,double_data[3],decimals,double_data[4],decimals,double_data[5],decimals,double_data[6],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1]));
          add_to_buffer(tmp_buffer);
          boxplot_cnt++;
          reset();
          break;
        case BOXPLOTDATA:
  /*
  @ boxplotdata some_data
  @ 'some_data' are a list of numbers separated by a comma "," (items)
  @ only be used before command <code>boxplot</code>: the command <a href="#boxplot">boxplot</a> will provide the boxplot drawing of the data.
  @ xrange 0,100<br>yrange 0,10<br>boxplotdata 11,22,13,15,23,43,12,12,14,2,45,32,44,13,21,24,13,19,35,21,24,23<br>boxplot x,4,5
  @ note: wims will not check your data input | format. use js-error console to debug any problems.
  @ a javascript function <code>statistics()</code> will parse the data and calculate the values [min,Q1,median,Q3,max] and hand them to the boxplot draw function.
  @ only a single call to <code>boxplotdata</code> can be made. If multiple boxplots should be present in a single canvas, then use multiple calls to command <a href='#boxplot'>boxplot</a>
  @%boxplotdata%size 400,400%xrange 0,100%yrange 0,10%strokecolor orange%fillpattern hatch%linewidth 3%strokecolor green%boxplotdata 11,22,13,15,23,43,12,12,14,2,45,32,44,13,21,24,13,19,35,21,24,23%boxplot x,2,2%mouse red,16
  */
          js_function[DRAW_JSBOXPLOT] = 1;
          js_function[DRAW_BOXPLOT] = 1;
          fprintf(js_include_file,"var boxplot_source = 1;var jsboxplot_data = [%s];\n",get_string(infile,1));
          break;
        case CANVASTYPE:
          canvas_type = (int) (get_real(infile,1));
  /*
  @ canvastype TYPE
  @ for now only useful before commands filltoborder / floodfill / clickfill etc operations<br>Only the images of this TYPE will be scanned and filled
  @ default value of TYPE is DRAG_CANVAS e.g. 5 (all clickable / draggable object are in this canvas)
  @ use another TYPE, if you know what you are doing...
  @ other possible canvasses (e.g. transparent PNG pictures, xsize &times; ysize on top of each other)<ul><li>EXTERNAL_IMAGE_CANVAS 0</li><li>BG_CANVAS   1</li><li> STATIC_CANVAS  2</li><li> MOUSE_CANVAS  3</li><li> GRID_CANVAS  4</li><li> DRAG_CANVAS  5</li><li> DRAW_CANVAS  6</li><li> TEXT_CANVAS  7</li><li> CLOCK_CANVAS  8</li><li> ANIMATE_CANVAS  9</li><li> TRACE_CANVAS  10</li><li>BOXPLOT_CANVAS 11</li><li> JSPLOT_CANVAS  100, will increase with every call</li><li> FILL_CANVAS  200, will increase with every call</li><li> USERDRAW_JSPLOT 300, will increase with every call</li><li>CLICKFILL_CANVAS 400, will increase with every call/click</li><li>BOXPLOT_CANVAS 500, will increase with every call</li></ul>
  */
          break;
        case CENTERED:
          use_offset = 4;
   /*
  @ centered
  @ keyword ; to place the text centered (in width and height) on the text coordinates(x:y)
  @ may be used for text exactly centered on its (x;y)
  @ use <a href="#fontfamily">fontfamily</a> for setting the font
  @ may be active for commands <a href="#text">text</a> and <a href="#string">string</a> (e.g. objects in the ''drag/drop/onclick-library``)
  @%centered%size 400,400%xrange -10,10%yrange -10,10%fontfamily 12pt Arial%string blue,-9,-9,no offset%point -9,-9,red%centered%string blue,-6,-6,centered%point -6,-6,red%xoffset%string blue,-3,-3,xoffset%point -3,-3,red%yoffset%string blue,0,0,yoffset%point 0,0,red%xyoffset%string blue,3,3,xyoffset%point 3,3,red%resetoffset%string blue,6,6,resetoffset%point 6,6,red
  */
          break;
        case CENTERSTRING:
  /*
  @ centerstring color,y-value,the text string
  @ title color,y-value,the text string
  @ draw a string centered on the canvas at y = y-value
  @ can not be set ''onclick`` or ''drag xy`` (...)
  @ unicode supported: <code>centerstring red,5,\\u2232</code>
  @ use a command like <code>fontfamily italic 24pt Arial</code> to set fonts on browser that support font change
  @%centerstring%size 400,400%xrange -10,10%yrange -10,10%bgcolor lightblue%fontfamily italic 22pt Courier%centerstring blue,7,the center
  */
          js_function[DRAW_CENTERSTRING] = 1;
          for(i=0;i<3;i++){
            switch(i){
              case 0: stroke_color = get_color(infile,0);break;/* name or hex color */
              case 1: double_data[0] = get_real(infile,0);break; /* y in xrange*/
              case 2: temp = get_string_argument(infile,1);
                /* draw_text = function(canvas_type,y,font_family,stroke_color,stroke_opacity,text) */
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_centerstring(%d,%.*f,\"%s\",\"%s\",%.2f,\"%s\");\n",canvas_root_id,decimals,double_data[0],font_family,stroke_color,stroke_opacity,temp));
                add_to_buffer(tmp_buffer);
                break;
              default:break;
            }
          }
          break;
        case CHEMTEX:
  /*
  chemtex
  keyword...needs to be the first command in the script (even before the ''size`` command)
  only for KaTeX enabled typesetting !
  will include 80kB large js-library for chemisty typesetting
  using <a href="http://85.148.206.56/wims/download/katex.for.wims.tar.gz">KaTeX</a> : <code>katex x,y,\\ce{ chemistry tex code} like : \\ce{ Hg^2+ ->[I-] HgI2 ->[I-] [Hg^{II}I4]^2- }</code>
  using MathJaX : <code>latex x,y,\\ce{ chemistry tex code} like : \\ce{ Hg^2+ ->[I-] HgI2 ->[I-] [Hg^{II}I4]^2- }</code>
  see https://mhchem.github.io/MathJax-mhchem/
  %chemtex_katex_mathjax%chemtex%size 400,400%xrange -10,10%yrange -10,10%snaptogrid%fontfamily 22px Arial%strokecolor red%drag xy%centered%latex 0,8,\\ce{Hg^2+}%drag xy%centered%latex 0,4,\\ce{\\xrightarrow{\\text{I}^{-}}}%drag xy%centered%latex 0,0,\\ce{HgI2}%centered%drag xy%latex 0,-4,\\ce{\\xrightarrow{\\text{I}^{-}}}%drag xy%centered%latex 0,-8,\\ce{[Hg^{II}I4]^2-}
  */
          found_size_command = 1;
          fprintf(stdout,"\n<script src=\"scripts/js/KaTeX/mhchem.js\" defer></script>\n");
          break;

        case CIRCLE:
  /*
  @ circle xc,yc,width (2*r in pixels),color
  @ use command <code>fcircle xc,yc,d,color</code>
  @ alternative: disk
  @ use command <code>fillcolor color</code> to set the fillcolor
  @ may be set <a href='#drag'>draggable</a> / <a href='#onclick'>onclick</a>
  @ will shrink / expand on zoom out / zoom in
  @%circle%size 400,400%xrange -10,10%yrange -10,10%filled%fillcolor lightblue%opacity 255,50%drag xy%circle 0,0,60,red%zoom red
  */
          for(i=0;i<4;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x */
              case 1: double_data[1] = get_real(infile,0);break; /* y */
              case 2: double_data[2] = px2x((get_real(infile,0))/2) - px2x(0);break; /* for zoom in/out: radius in 'dx' xrange*/
              case 3: stroke_color = get_color(infile,1);/* name or hex color */
                if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
                if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(2,2);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,13,[%.*f],[%.*f],[%.3f],[%.3f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[1],double_data[2],double_data[2],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}/* object_cnt++;*/
                dragstuff[13] = 1;
                if(use_dragstuff == 0 ){ use_dragstuff = 1; }
                reset();
                break;
              default : break;
            }
          }
          break;
        case CIRCLES:
  /*
  @ circles color,xc1,yc1,r1,xc2,yc2,r2...xc_n,yc_n,r_n
  @ <b>attention</b> r = radius in x-range (!)
  @ use keyword <code>filled</code> or command <code>fcircles</code> to produce solid circles
  @ alternative: disks
  @ use command <code>fillcolor color</code> to set the fillcolor
  @ may be set <a href='#drag'>draggable</a> / <a href='#onclick'>onclick</a> (individually)
  @ will shrink / expand on zoom out / zoom in
  @%circles_drag%size 400,400%xrange -10,10%yrange -10,10%filled%fillcolor lightblue%opacity 255,50%drag xy%circles blue,0,0,2,2,2,3,-3,-3,3,3,3,4,3,-4,2%zoom red
  @%circles_onclick%size 400,400%xrange -10,10%yrange -10,10%filled%fillcolor lightblue%opacity 255,50%onclick%circles blue,0,0,2,2,2,3,-3,-3,3,3,3,4,3,-4,2%zoom red
  @%circles_drag_slider%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%drag xy%# Click circles(s) to activate%opacity 200,50%fillcolor orange%rotationcenter 2,3%slider 0,2*pi,300,30,angle degrees,Rotate%slider -5,5*pi,300,30,x display,move in x-direction%slider -10,10*pi,300,30,y display,move in y-direction%fcircles blue,0,0,0.5,2,2,1,-3,-3,1.5,3,3,0.5,3,-4,0.5
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
          i=1;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            switch (i%3){
              case 1:double_data[i-1] = get_real(infile,0);break; /* x */
              case 2:double_data[i-1] = get_real(infile,0);break; /* y */
              case 0:double_data[i-1] = get_real(infile,1);break; /* r */
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,3);}
          if(use_affine == TRUE ){ transform(i-1,3);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */

          decimals = find_number_of_digits(precision);
          for(c = 0 ; c < i-1 ; c = c+3){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,13,[%.*f],[%.*f],[%.3f],[%.3f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+1],double_data[c+2],double_data[c+2],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}/* object_cnt++; */
          }
          reset();
          dragstuff[13] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case CLEARBUTTON:
  /*
  @ clearbutton value
  @ alternative: delete
  @ alternative: erase
  @ adds a button to clear the <a href="#userdraw">userdraw</a> canvas with text ''value``
  @ <b>attention</b> command <code>clearbutton</code> is incompatible with <a href="#multidraw">multidraw</a> based drawings<br>(in <code>multidraw</code> there is always a remove_object_button for every draw primitive)
  @ normally <a href="#userdraw">userdraw</a> primitives have the option to use middle/right mouse button on<br> a point of the object to remove this specific object...this clear button will remove all drawings
  @ uses the tooltip placeholder div element: may not be used with command <code>intooltip</code>
  @ use command <a href="#css">css</a> to style the button...
  @ the clearbutton will have id="canvas_scripts[%d]" ; starting with %d=0 for the first script<br>to change the style of all ''clearbutton`` of all included canvasdraw scripts, use something like<br><code>if(document.getElementById("clearbutton"+canvas_scripts[0])){<br>&nbsp;var p = 0;<br>&nbsp;while(document.getElementById("clearbutton"+canvas_scripts[p])){<br>&nbsp;&nbsp;document.getElementById("clearbutton"+canvas_scripts[p]).className="some_class_name";<br>&nbsp;&nbsp;&lt;!&minus;&minus;</code> or <code>document.getElementById("clearbutton"+canvas_scripts[p]).setAttribute("style","some_style"); &minus;&minus;&gt;<br>&nbsp;&nbsp;p++;<br>&nbsp;};<br>};</code>
  @%clearbutton%size 400,400%xrange -10,10%yrange -10,10%filled%fillcolor lightblue%opacity 255,50%userdraw circles,red%clearbutton Remove All
  */
          if(reply_format == 29){/* eg multidraw is selected */
  // canvas_error("command clearbutton incompatible with multidraw...only suitable for userdraw");
          }
          add_clear_button(css_class,get_string(infile,1));
          break;
        case CLOCK:
  /*
  @ clock x,y,r(px),H,M,S,type hourglass,interactive [ ,H_color,M_color,S_color,background_color,foreground_color ]
  @ use command <code>opacity stroke-opacity,fill-opacity</code> to adjust foreground (stroke) and background (fill) transparency
  @ type hourglass:<br>type = 0: only segments<br>type = 1: only numbers<br>type = 2: numbers and segments
  @ colors are optional: if not defined, default values will be used<br>default colors: clock 0,0,60,4,35,45,1,2<br>custom colors: clock 0,0,60,4,35,45,1,2,,,,yellow,red<br>custom colors: clock 0,0,60,4,35,45,1,2,white,green,blue,black,yellow
  @ if you don't want a seconds hand (or minutes...), just make it invisible by using the background color of the hourglass...
  @ interactive <ul><li>0: not interactive, just clock(s)</li><li>1: function read_canvas() will read all active clocks in H:M:S format<br>The active clock(s) can be adjusted by pupils</li><li>2: function read_canvas() will return the clicked clock <br>(like multiplechoice; first clock in script in nr. 0 )</li><li>3: no prefab buttons...create your own buttons (or other means) to make the clock(s) adjustable by javascript function set_clock(num,type,diff)<br>wherein: num = clock id (starts with 0) ; type = 1 (hours) ; type = 2 (minutes) ; type = 3 (seconds) <br>and diff = the increment of 'type' (positive or negative)</li></ul>
  @ canvasdraw will not check validity of colornames...the javascript console is your best friend
  @ no combinations with other reply_types allowed, for now
  @ if interactive is set to ''1``, 6 buttons per clock will be displayed for adjusting a clock (H+ M+ S+ H- M- S-)<br> set_clock(clock_id,type,incr) <br>first clock has clock_id=0 ; type: H=1,M=2,S=3 ; incr: increment integer
  @ note: if you need multiple -interactive- clocks on a webpage, use multiple ''clock`` commands in a single script<br>and <i>not multiple canvas scripts</i> in a single page
  @ note: clocks will not zoom or pan, when using command <a href='#zoom'>zoom</a>
  @%clock_1%size 400,400%xrange -10,10%yrange -10,10%clock 0,0,120,4,35,45,0,0,red,green,blue,lightgrey,black
  @%clock_2%size 400,400%xrange -10,10%yrange -10,10%clock 0,0,120,4,35,45,1,1,red,green,blue,lightgrey,black
  @%clock_3%size 400,400%xrange -10,10%yrange -10,10%clock -5,0,80,4,35,45,2,2,red,green,blue,lightgrey,black%clock 5,0,80,3,15,65,2,2,red,green,blue,lightgrey,black
  @%clock_4%size 400,400%xrange -10,10%yrange -10,10%clock 0,0,120,4,35,45,0,0,red,green,blue,lightgrey,black
  @%clock_5%size 400,400%xrange -10,10%yrange -10,10%clock 0,0,120,4,35,45,1,1,red,green,blue,lightgrey,black
  @%clock_6%size 400,400%xrange -10,10%yrange -10,10%clock -5,0,80,4,35,45,2,2,red,green,blue,lightgrey,black%clock 5,0,80,8,55,15,2,2,red,green,blue,lightgrey,black
  @%clock_7%size 400,400%xrange -10,10%yrange -10,10%clock 0,0,120,4,35,45,2,0,red,green,blue,lightgrey,black
  */
          js_function[DRAW_CLOCK] = 1;
          js_function[INTERACTIVE] = 1;

  /*    var clock = function(xc,yc,radius,H,M,S,h_color,m_color,s_color,bg_color,fg_color) */
          for(i=0;i<9;i++){
            switch(i){
              case 0: int_data[0] = x2px(get_real(infile,0)); break; /* xc */
              case 1: int_data[1] = y2px(get_real(infile,0)); break; /* yc */
              case 2: int_data[2] = get_real(infile,0);break;/* radius in px */
              case 3: int_data[3] = get_real(infile,0);break;/* hours */
              case 4: int_data[4] = get_real(infile,0);break;/* minutes */
              case 5: int_data[5] = get_real(infile,0);break;/* seconds */
              case 6: int_data[6] = get_real(infile,0);if(int_data[6] < 0 || int_data[6] > 2){canvas_error("hourglass can be 0,1 or 2");}break;/* type hourglass */
              case 7: int_data[7] = (int)(get_real(infile,1));/* interactive 0,1,2*/
                switch(int_data[7]){
                  case 0:break;
                  case 1:
                    if(clock_cnt == 0){
                      if( reply_format == 0 ){
                        reply_format = 18; /* user sets clock */
                  /*
                     tmp_buffer=my_newmem(MAX_BUFFER);
                     check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "set_clock = function(num,type,diff){var name = eval(\"clocks\"+num);switch(type){case 1:name.H = parseInt(name.H+diff);break;case 2:name.M = parseInt(name.M+diff);break;case 3:name.S = parseInt(name.S+diff);break;default: break;};name = clock(name.xc,name.yc,name.radius,name.H,name.M,name.S,name.type,name.interaction,name.H_color,name.M_color,name.S_color,name.bg_color,name.fg_color);};\n"));
                     add_to_buffer(tmp_buffer);
                 */
                        fprintf(js_include_file,"set_clock = function(num,type,diff){if(wims_status == \"done\"){return;};var name = eval(\"clocks\"+num);switch(type){case 1:name.H = parseInt(name.H+diff);break;case 2:name.M = parseInt(name.M+diff);break;case 3:name.S = parseInt(name.S+diff);break;default: break;};name = new clock(name.xc,name.yc,name.radius,name.H,name.M,name.S,name.type,name.interaction,name.H_color,name.M_color,name.S_color,name.bg_color,name.fg_color);};\n");
                      }
                      else {
                        canvas_error("interactive clock may not be used together with other reply_types...");
                      }
                    }
                    fprintf(stdout,"<p style=\"text-align:center\"><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,1,1)\" value=\"H+\"><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,2,1)\" value=\"M+\"><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,3,1)\" value=\"S+\"><br><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,1,-1)\" value=\"H&minus;\"><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,2,-1)\" value=\"M&minus;\"><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,3,-1)\" value=\"S&minus;\"></p>",css_class,clock_cnt,css_class,clock_cnt,css_class,clock_cnt,css_class,clock_cnt,css_class,clock_cnt,css_class,clock_cnt);
                    break;
                  case 3:
                    if(clock_cnt == 0){
                      if( reply_format == 0 ){
                        reply_format = 18; /* user sets clock */
                        fprintf(js_include_file,"set_clock = function(num,type,diff){if(wims_status == \"done\"){return;};var name = eval(\"clocks\"+num);switch(type){case 1:name.H = parseInt(name.H+diff);break;case 2:name.M = parseInt(name.M+diff);break;case 3:name.S = parseInt(name.S+diff);break;default: break;};name = new clock(name.xc,name.yc,name.radius,name.H,name.M,name.S,name.type,1,name.H_color,name.M_color,name.S_color,name.bg_color,name.fg_color);};\n");
                       }
                      else {
                        canvas_error("interactive clock may not be used together with other reply_types...");
                      }
                    }
          /*
          fprintf(stdout,"<p style=\"text-align:center\"><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,1,1)\" value=\"H+\"><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,2,1)\" value=\"M+\"><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,3,1)\" value=\"S+\"><br><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,1,-1)\" value=\"H&minus;\"><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,2,-1)\" value=\"M&minus;\"><input class=\"%s\" type=\"button\" onclick=\"javascript:set_clock(%d,3,-1)\" value=\"S&minus;\"></p>",css_class,clock_cnt,css_class,clock_cnt,css_class,clock_cnt,css_class,clock_cnt,css_class,clock_cnt,css_class,clock_cnt);
         */
                  break;
                  case 2:
                    if( reply_format == 0 ){
                      reply_format = 19; /* "onclick */
                      js_function[INTERACTIVE] = 1;
                      fprintf(js_include_file,"\n/* begin onclick handler for clocks */\nvar reply = new Array();canvas_div.addEventListener( 'mousedown', user_click,false);\n\nfunction user_click(evt){if(evt.button == 1){var canvas_rect = clock_canvas.getBoundingClientRect();var x = evt.clientX - canvas_rect.left;var y = evt.clientY - canvas_rect.top;var p = 0;var name;var t = true;while(t){try{name = eval('clocks'+p);if( x < name.xc + name.radius && x > name.xc - name.radius ){if( y < name.yc + name.radius && y > name.yc - name.radius ){reply[0] = p;name = new clock(name.xc,name.yc,name.radius,name.H,name.M,name.S,name.type,name.interaction,name.H_color,name.M_color,name.S_color,\"lightblue\",name.fg_color);};}else{clock_ctx.clearRect(name.xc-name.radius,name.yc-name.radius,name.xc+name.radius,name.yc+name.radius);name = new clock(name.xc,name.yc,name.radius,name.H,name.M,name.S,name.type,name.interaction,name.H_color,name.M_color,name.S_color,name.bg_color,name.fg_color);};p++;}catch(e){t=false;};};};};\n");
                    }
                    else {
                      if( reply_format != 19){
                        canvas_error("clickable clock(s) may not be used together with other reply_types...");
                      }
                    }
                    break;
                  default: canvas_error("interactive must be set 0,1 or 2");break;
                }
                break;
              case 8:
                if(clock_cnt == 0 ){ /* set opacity's just once .... it should be a argument to clock(), for now it's OK */
                  fprintf(js_include_file,"var clock_bg_opacity = %.2f;var clock_fg_opacity = %.2f;",fill_opacity,stroke_opacity);
                }
                temp = get_string(infile,3);/* optional colors, like: ,,red,,blue*/
                if( strstr( temp,",") != 0 ){ temp = str_replace(temp,",","\",\""); }
                else{
                /* h_color,m_color,s_color,bg_color,fg_color */
                  temp = ",black\",\"black\",\"black\",\"white\",\"black";}
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "clocks%d = new clock(%d,%d,%d,%d,%d,%d,%d,%d,\"%s\");\n",clock_cnt,int_data[0],int_data[1],int_data[2],int_data[3],int_data[4],int_data[5],int_data[6],int_data[7],temp));
                add_to_buffer(tmp_buffer);
                fprintf(js_include_file,"var clocks%d;",clock_cnt);
                clock_cnt++;
                break;
              default:break;
            }
          }
          break;
        case COLORPALETTE:
  /*
  @ colorpalette color_name_1,color_name_2,...,color_name_8
  @ opacity will be the same for all colors and is set by command <a href="#opacity">opacity [0-255],[0-255]</a>
  @ can be used with command <a href='#userdraw'>userdraw clickfill,color</a> when more than one fillcolor is wanted.<br>in that case use for example <a href='#replyformat'>replyformat 10</a> ... reply=x1:y1:color1,x2:y2:color2...<br>the pupil can choose from the given colors by clicking small coloured buttons.<br> the click coordinates and corresponding fillcolor will be stored in read_canvas()...when using the appropriate replyformat.<br>the first color of the palette is color=0
  @ make sure to include the ''remove button`` by using command <a href='#clearbutton'>clearbutton some_text</a><br/>please note: right mouse click will alse remove last added item...
  @%colorpalette%size 400,400%xrange -10,10%yrange -10,10%linewidth 3%circles blue,0,0,4,1,1,6,3,3,3,-3,-3,5%opacity 255,100%colorpalette red,green,yellow,blue%userdraw clickfill,green%clearbutton REMOVE
  */
          if( use_tooltip == 1 ){canvas_error("command 'colorpalette' is incompatible with command 'intooltip tip_text'");}
          fprintf(js_include_file,"var multifillcolors = [];var palettecolors = [");
          while( ! done ){
            temp = get_color(infile,1);
            fprintf(js_include_file,"\"%s,%d\",",temp,(int) (255*fill_opacity));
          }
          fprintf(js_include_file,"];");/* add black to avoid trouble with dangling comma... */
          add_color_palette(css_class);
          break;

        case COMMENT:
          sync_input(infile);
          break;

        case COPY:
  /*
  @ copy x,y,x1,y1,x2,y2,[filename URL]
  @ The image may be "bitmap" or "SVG"
  @ Insert the region from (x1,y1) to (x2,y2) (in pixels) of [filename] to (x,y) in x/y-range
  @ If x1=y1=x2=y2=-1, the whole [filename URL] is copied.
  @ [filename] is the URL of the image
  @ <em>TODO:move special image functions to generic 'dragstuff' library</em>
  @ URL is normal URL of network reachable image file location
  @ if command <a href="#drag">drag x/y/xy</a> is set before command ''copy``, the images will be draggable<br>javascript function read_canvas(); will return the x/y coordinate data in xrange/yrange of all -including non draggable- images<br>the command drag is only valid for the next image<br>draggable / non-draggable images may be mixed<br>may be used together with preceding keywords ''snaptogrid``, ''xsnaptogrid``, ''ysnaptogrid`` or <code>snaptopoints x1,y1,x2,y2...</code>.
  @ if keyword <a href="#onclick">onclick</a> is set before command ''copy`` the image(s) is clickable (marked with a green rectangle around the image)<br>use 'read_dragdrop' to get the number of the clicked image(s)<br>use command 'clearbutton some_text' to reset the reply/click array.<br>example: 4 images; student clicked on image 2 and 3: reply = 0,1,1,0<br>after clicking the clear button: reply = 0,0,0,0<br>May be mixed with commands ''drag x|y|xy`` (use javascript read_canvas to get the new coordinates
  @ ''onclick`` for external images may be mixed with canvas generated stuff (like lines,curves, embeded XML etc)
  @ you may draw / userdraw / drag other stuff on top of an "imported" image
  @ the use of a slider is not possible: if needed, use command <a href='#html'>html x,y,&lt;img src=my_image.svg /&gt; </a>
  @ use keyword <a href='#centered'>centered</a> before command ''copy`` to place image center at given coordinates.
  @%copy_onclick%size 400,400%xrange -10,10%yrange -10,10%onclick%copy -5,5,-1,-1,-1,-1,gifs/fr.gif%onclick%copy 5,5,-1,-1,-1,-1,gifs/en.gif%onclick%copy 5,-5,-1,-1,-1,-1,gifs/it.gif%onclick%copy -5,-5,-1,-1,-1,-1,gifs/cn.gif
  @%copy_drag_xy%size 400,400%xrange -10,10%yrange -10,10%# attention: left mouse click on the image will activate dragging...%# keep left mouse button pressed while moving the image !%drag xy%copy -5,5,-1,-1,-1,-1,gifs/fr.gif%drag xy%copy 5,5,-1,-1,-1,-1,gifs/en.gif%drag xy%copy 5,-5,-1,-1,-1,-1,gifs/it.gif%drag xy%copy -5,-5,-1,-1,-1,-1,gifs/cn.gif
  @%copy_drag_xy_snaptogrid%size 400,400%xrange -10,10%yrange -10,10%grid 2,2,grey%# attention: left mouse click on the image will activate dragging...%# keep left mouse button pressed while moving the image !%drag xy%# a function read_canvas_images() %copy -6,6,-1,-1,-1,-1,gifs/fr.gif%snaptogrid%drag xy%copy 6,6,-1,-1,-1,-1,gifs/en.gif%snaptogrid%drag xy%copy 6,-6,-1,-1,-1,-1,gifs/it.gif%snaptogrid%drag xy%copy -6,-6,-1,-1,-1,-1,gifs/cn.gif
  */
          for(i = 0 ; i<7;i++){
            switch(i){
              case 0: double_data[0]=get_real(infile,0);break; /* x left top corner in x/y range  */
              case 1: double_data[1]=get_real(infile,0);break; /* y left top corner in x/y range */
              case 2: int_data[2]=(int)(get_real(infile,0));break;/* x1 in px of external image */
              case 3: int_data[3]=(int)(get_real(infile,0));break;/* y1 in px of external image */
              case 4: int_data[4]=(int)(get_real(infile,0));break;/* x2 --> width  */
              case 5: int_data[5]=(int)(get_real(infile,0)) ;break;/* y2 --> height */
              case 6: URL = get_string(infile,1);
                if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
                if(use_affine == TRUE ){transform(2,2);}
                int_data[0] = x2px(double_data[0]);
                int_data[1] = y2px(double_data[1]);
                int_data[6] = int_data[4] - int_data[2];/* swidth & width (if not scaling )*/
                int_data[7] = int_data[5] - int_data[3];/* sheight & height (if not scaling )*/
                if( onclick == 0 ){ /* no mouse needed static image copy  */
                  if(js_function[DRAW_EXTERNAL_IMAGE] == 0){/* create canvas just once */
                    fprintf(js_include_file,"var image_copy_canvas = create_canvas%d(%d,xsize,ysize);",canvas_root_id,STATIC_IMAGE_CANVAS);
                    js_function[DRAW_EXTERNAL_IMAGE] = 1;
                  }
                  tmp_buffer=my_newmem(MAX_BUFFER);
                  check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_external_image(\"%s\",%d,%d,%d,%d,%d,%d,%d,%d,%d,%d);\n",URL,int_data[2],int_data[3],int_data[6],int_data[7],int_data[0],int_data[1],int_data[6],int_data[7],use_offset,int_data[10]));
                }
                else /* onclick or drag & drop external copy images */
                {
                  js_function[DRAG_EXTERNAL_IMAGE] = 1;
                  tmp_buffer=my_newmem(MAX_BUFFER);
                  check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "drag_external_image(\"%s\",%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d);\n",URL,int_data[2],int_data[3],int_data[6],int_data[7],int_data[0],int_data[1],int_data[6],int_data[7],onclick,object_cnt,use_offset,use_snap,int_data[10]));
                }
                add_to_buffer(tmp_buffer);
                object_cnt++;
                break;
              default: break;
            }
          }
          reset();
          break;
/*
HTML5 specs:
context.drawImage(img,sx,sy,swidth,sheight,x,y,width,height);
img   Specifies the image, canvas, or video element to use
sx   The x coordinate where to start clipping: x1 = int_data[0]
sy   The y coordinate where to start clipping: x2 = int_data[1]
swidth   The width of the clipped image: int_data[2] - int_data[0]
sheight The height of the clipped image: int_data[3] - int_data[1]
x   The x coordinate where to place the image on the canvas: dx1 = int_data[4]
y   The y coordinate where to place the image on the canvas: dy1 = int_data[5]
width   The width of the image to use (stretch or reduce the image): dx2 - dx1 = int_data[6]
height   The height of the image to use (stretch or reduce the image): dy2 - dy1 = int_data[7]
*/
        case COPYRESIZED:
  /*
  @ copyresized x1,y2,x2,y2,dx1,dy1,dx2,dy2,image_file_url
  @ The image may be any "bitmap" or "SVG"
  @ Insert the region from (x1,y1) to (x2,y2) (in pixels) of [ filename], <br>possibly resized,<br>to the region of (dx1,dy1) to (dx2,dy2) in x/y-range
  @ (dx1:dy1) must be left top corner; (dx2:dy2) must be right bottom corner of inserted image
  @ If x1=y1=x2=y2=-1, the whole [filename / URL ] is copied and resized.
  @ URL is normal URL of network reachable image file location<br>(as seen from public_html-root or network reachable 'http://some_server/my_images/test.gif'<br>(eg no special wims paths are searched !!)
  @ if command <a href="#drag">drag x/y/xy</a> is set before command ''copy``, the images will be draggable<br>javascript function read_canvas(); will return the x/y coordinate data in xrange/yrange of all -including non draggable- images<br>the command drag is only valid for the next image<br>draggable / non-draggable images may be mixed<br>may be used together with preceding keywords ''snaptogrid``,''xsnaptogrid``,''ysnaptogrid`` or <code>snaptopoints x1,y1,x2,y2...</code>
  @ if keyword <a href="#onclick">onclick</a> is set before command ''copy`` the image(s) is clickable (marked with a green rectangle around the image)<br>use ''read_dragdrop`` to get the number of the clicked image(s)<br>use command 'clearbutton some_text' to reset the reply/click array.<br>example: 4 images; student clicked on image 2 and 3: reply = 0,1,1,0<br>after clicking the clear button: reply = 0,0,0,0<br>May be mixed with commands ''drag x|y|xy`` (use javascript read_canvas to get the new coordinates
  @ ''onclick`` for external images may be mixed with canvas generated stuff (like lines,curves etc)
  @ you may draw / userdraw / drag stuff on top of an "imported" image
  @ when set draggable, there will be special function 'read_canvas_images()'<br>now dragging external images may be combined with 'read_canvas()' from <a href='#userdraw'>userdraw</a> or <a href='#multidraw'>multidraw</a><br>set command <a href='#precision'>precision</a> before command ''copy``
  @ use keyword <a href='#centered'>centered</a> before command 'copyresized' to place image center at given coordinates.
  @ <em>TODO:move special image functions to generic 'dragstuff' library</em>
  */
          for(i = 0 ; i<9;i++){
           switch(i){
            case 0: int_data[0] = (int)(get_real(infile,0));break; /* x1 */
            case 1: int_data[1] = (int)(get_real(infile,0));break; /* y1 */
            case 2: int_data[2] = (int)(get_real(infile,0));break;/* x2 */
            case 3: int_data[3] = (int)(get_real(infile,0));break;/* y2 */
            case 4: int_data[4] = x2px(get_real(infile,0));break;/* dx1 */
            case 5: int_data[5] = y2px(get_real(infile,0));break;/* dy1 */
            case 6: int_data[6] = x2px(get_real(infile,0));break;/* dx2 */
            case 7: int_data[7] = y2px(get_real(infile,0));break;/* dy2 */
            case 8: URL = get_string(infile,1);
              if( int_data[1] == -1 ){ int_data[10] = 1; }else{int_data[10] = 0; }/* resized / not resized */
          /* flag error when wrong diagonal:  copyresized -1,-1,-1,-1,0,0,7,7,testfig.gif */
              if( int_data[7] < int_data[5] || int_data[6] < int_data[4]){
                canvas_error("in copyresized,  use:<br>left top corner (dx1:dy1) and right bottom corner (dx2:dy2) ! ");
              }
              int_data[2] = abs(int_data[2] - int_data[0]);/* swidth */
              int_data[3] = abs(int_data[3] - int_data[1]);/* sheight */
              int_data[6] = abs(int_data[6] - int_data[4]);/* width */
              int_data[7] = abs(int_data[7] - int_data[5]);/* height */
              if( onclick == 0 ){ /* no mouse needed static image copy  */
                if(js_function[DRAW_EXTERNAL_IMAGE] == 0){
                  fprintf(js_include_file,"var image_copy_canvas = create_canvas%d(%d,xsize,ysize);",canvas_root_id,STATIC_IMAGE_CANVAS);
                  js_function[DRAW_EXTERNAL_IMAGE] = 1;
                }
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_external_image(\"%s\",%d,%d,%d,%d,%d,%d,%d,%d,%d,%d);\n",URL,int_data[0],int_data[1],int_data[2],int_data[3],int_data[4],int_data[5],int_data[6],int_data[7],use_offset,int_data[10]));
              }
              else /* onclick or drag & drop external copy images */
              {
                js_function[DRAG_EXTERNAL_IMAGE] = 1;
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "drag_external_image(\"%s\",%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d);\n",URL,int_data[0],int_data[1],int_data[2],int_data[3],int_data[4],int_data[5],int_data[6],int_data[7],onclick,object_cnt,use_offset,use_snap,int_data[10]));
                object_cnt++;
              }
              add_to_buffer(tmp_buffer);
              break;
            default: break;
            }
          }
          reset();
          break;

        case CROSSHAIR:
  /*
  @ crosshair x,y,color
  @ draw a single crosshair point at (x;y) in color ''color``
  @ use command <code>crosshairsize int</code> and / or <code>linewidth int</code> to adjust
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%crosshair%size 400,400%xrange -10,10%yrange -10,10%opacity 255,255%linewidth 2%onclick%crosshair 0,0,red%linewidth 1%onclick%crosshair 1,1,blue%linewidth 3%onclick%crosshair 3,3,green%linewidth 4%xrosshair 4,4,orange
  */
          for(i=0;i<3;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x */
              case 1: double_data[1] = get_real(infile,0);break; /* y */
              case 2: stroke_color = get_color(infile,1);/* name or hex color */
                if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(2,2);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,7,[%.*f],[%.*f],[%d],[%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[1],crosshair_size,crosshair_size,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,0,0,0,use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}
          /* object_cnt++ */
                reset();
                dragstuff[7] = 1;
                break;
              default:break;
            }
          }
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;

        case CROSSHAIRS:
  /*
  @ crosshairs color,x1,y1,x2,y2,...,x_n,y_n
  @ draw multiple crosshair points at given coordinates in color ''color``
  @ use command <code>crosshairsize int</code> and / or <code>linewidth int</code> to adjust
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually (!)
  @%crosshairs_1%size 400,400%xrange -10,10%yrange -10,10%opacity 255,255%snaptogrid%linewidth 2%drag xy%crosshairs red,0,0,1,1,2,2,3,3%drag x%crosshairs blue,0,1,1,2,2,3,3,4
  @%crosshairs_2%size 400,400%xrange -10,10%yrange -10,10%opacity 255,255%linewidth 2%onclick%crosshairs red,0,0,1,1,2,2,3,3%onclick%crosshairs blue,0,1,1,2,2,3,3,4
*/
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if(use_moebius == TRUE ){ moebius(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          decimals = find_number_of_digits(precision);
          for(c=0 ; c < i-1 ; c = c+2){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,7,[%.*f],[%.*f],[%d],[%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+1],crosshair_size,crosshair_size,line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,1,0,0,0,use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}/* object_cnt++; */
          }
          dragstuff[7] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case CROSSHAIRSIZE:
  /*
  @ crosshairsize int
  @ default 8 (px)
  */
          crosshair_size = (int) (get_real(infile,1));
          break;
        case CSS:
  /*
  @ css css_class
  @ may be used before any ''style-able`` html object (like inputfields or buttons) or some html objects that are generated by some canvasdraw commands
  @ in case of <a href="#multidraw">multidraw</a> this command must be a table css class, for example "wimstable"
  @%css%size 400,400%xrange -10,10%yrange -10,10%css wims_button_help%input 0,0,10,1,Hello
  */
          css_class = get_string(infile,1);
          break;
        case CURSOR:
  /*
  @ cursor some CSS cursor_style
  @ alternative: pointer
  @ style can be any valid CSS property value
  @ choose from these types:<br>alias,all-scroll,auto,cell,context-menu,col-resize,copy,crosshair,default,e-resize,<br>ew-resize,grab,grabbing,help,move,n-resize,ne-resize,nesw-resize,ns-resize,nw-resize,<br>nwse-resize,no-drop,none,not-allowed,pointer,progress,row-resize,s-resize,se-resize,<br>sw-resize,text,url(myBall.cur),auto,vertical-text,w-resize,wait,zoom-in,zoom-out,initial
  @ note: wims will not check the validity of your cursor declaration
  @%cursor_css%size 400,400%xrange -10,10%yrange -10,10%cursor move%linewidth 3%drag xy%opacity 200,75%fcircles blue,-5,5,3,-4,-2,6,0,0,5,3,4,2,4,-5,4
  */
          fprintf(js_include_file,"canvas_div%d.style.cursor = \"%s\";",canvas_root_id,get_string(infile,1));
         break;
        case CURVE:
  /*
  @ curve color,formula(x)
  @ alternative: plot
  @ use command <a href="#trange">trange</a> in parametric functions before <b>every</b> command curve / plot <code>trange -pi,pi<br>curve color,formula1(t),formula2(t)</code><br>A next parametric curve will only be correctly plot when trange is set again !<br>this is a design flaw and not a feature...
  @ use command <a href="#precision">precision</a> to increase the number of digits of the plotted points
  @ use command <a href="#plotsteps">plotsteps</a> to increase / decrease the amount of plotted points (default 150)
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @ if you need a plot beyond xrange / yrange, use <a href="#jsplot">jsplot</a> (command ''curve`` will only calculate points within the xrange)
  @%curve%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%xlabel x-axis%ylabel y-axis%precision 1%grid 2,2,grey,2,2,6,grey%precision 1000%curve red,4*sqrt(x)%curve green,2*sqrt(abs(x))%curve blue,3*1/sqrt(x)%curve orange,4*sin(4/x)%dashed%curve red,4*cos(x)
  */
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */

          if( use_parametric == TRUE ){ /* parametric color,fun1(t),fun2(t)*/
            use_parametric = FALSE;
            stroke_color = get_color(infile,0);
            char *fun1 = get_string_argument(infile,0);
            char *fun2 = get_string_argument(infile,1);
            if( strlen(fun1) == 0 || strlen(fun2) == 0 ){canvas_error("parametric functions are NOT OK !");}
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,9,%s,[%d],[%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,eval_parametric(xsize,ysize,fun1,fun2,xmin-10,xmax+10,ymin-10,ymax+10,tmin,tmax,plot_steps,precision,rotationcenter),2*line_width,2*line_width,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
          }
          else{
            stroke_color = get_color(infile,0);
            char *fun1 = get_string_argument(infile,1);
            if( strlen(fun1) == 0 ){canvas_error("function is NOT OK !");}
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,9,%s,[%d],[%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,eval(xsize,ysize,fun1,xmin-10,xmax+10,ymin-10,ymax+10,plot_steps,precision,rotationcenter),line_width,line_width,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
          }
          if(onclick != 0){object_cnt++;}/* object_cnt++; */
          dragstuff[9] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case CURVEDARROW:
    /*
  @ curvedarrow x1,y1,xc,yc,x2,y2,color
  @ draw a single headed curved arrow from (x1:y1) in direction of (xc:yc) to point (x2:y2)<br> note: the curve will <b>not go through</b> point (xc:yc)
  @ use command <a href='#arrowhead'>arrowhead</a> to set the size of the arrow head.
  @ use command <code>linewidth int</code> to adjust thickness of the arrow
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%curvedarrow_drag%size 400,400%xrange -10,10%yrange -10,10%cursor move%linewidth 2%drag xy%curvedarrow -5,0,0,10,5,0,blue
  @%curvedarrow_click%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%%onclick%curvedarrow -5,0,0,-10,5,0,blue%onclick%curvedarrow -8,0,0,5,8,3,green

h[0] = arrowhead
h[1] = type: 1 = single 2=double arrow
function Shape(object_cnt,onclick,direction,type,x,y,w,h,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype0,dashtype1,use_rotate,angle,text,font_size,font_family,use_slider,rotation_center,use_offset)
    */
          for(i=0;i<7;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x1 */
              case 1: double_data[1] = get_real(infile,0);break; /* y1 */
              case 2: double_data[2] = get_real(infile,0);break; /* xc */
              case 3: double_data[3] = get_real(infile,0);break; /* yc */
              case 4: double_data[4] = get_real(infile,0);break; /* y3 */
              case 5: double_data[5] = get_real(infile,0);break; /* y3 */
              case 6: stroke_color = get_color(infile,1);/* name or hex color */
                if(use_rotate == TRUE ){rotate(6,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(6,2);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,21,[%.*f,%.*f,%.*f],[%.*f,%.*f,%.*f],[%d,%d],[%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[4],decimals,double_data[1],decimals,double_data[3],decimals,double_data[5],arrow_head,arrow_head,arrow_head,1,line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}/* object_cnt++;*/
                reset();
                break;
            }
          }
          dragstuff[21] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case CURVEDARROW2:
    /*
  @ curvedarrow2 x1,y1,xc,yc,x2,y2,color
  @ draw a double headed curved arrow from (x1:y1) in direction of (xc:yc) to point (x2:y2)<br> note: the curve will <b>not go through</b> point (xc:yc)
  @ use command <a href='#arrowhead'>arrowhead</a> to set the size of the arrow head.
  @ use command <code>linewidth int</code> to adjust thickness of the arrow
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%curvedarrow_drag%size 400,400%xrange -10,10%yrange -10,10%cursor move%linewidth 2%drag xy%curvedarrow2 -5,0,0,10,5,0,blue
  @%curvedarrow_click%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%%onclick%curvedarrow2 -5,0,0,-10,5,0,blue%onclick%curvedarrow -8,0,0,5,8,3,green

h[0] = arrowhead
h[1] = type: 1 = single 2=double arrow
function Shape(object_cnt,onclick,direction,type,x,y,w,h,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype0,dashtype1,use_rotate,angle,text,font_size,font_family,use_slider,rotation_center,use_offset)
    */
          for(i=0;i<7;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x1 */
              case 1: double_data[1] = get_real(infile,0);break; /* y1 */
              case 2: double_data[2] = get_real(infile,0);break; /* xc */
              case 3: double_data[3] = get_real(infile,0);break; /* yc */
              case 4: double_data[4] = get_real(infile,0);break; /* y3 */
              case 5: double_data[5] = get_real(infile,0);break; /* y3 */
              case 6: stroke_color = get_color(infile,1);/* name or hex color */
              if(use_rotate == TRUE ){rotate(6,angle,rotationcenter,2);}
              if(use_affine == TRUE ){ transform(6,2);}
              if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
              decimals = find_number_of_digits(precision);
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,21,[%.*f,%.*f,%.*f],[%.*f,%.*f,%.*f],[%d,%d],[%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[4],decimals,double_data[1],decimals,double_data[3],decimals,double_data[5],arrow_head,arrow_head,arrow_head,2,line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
              add_to_buffer(tmp_buffer);
              if(onclick != 0){object_cnt++;}/* object_cnt++;*/
              dragstuff[21] = 1;
              if(use_dragstuff == 0 ){ use_dragstuff = 1; }
              reset();
              break;
            }
          }
          break;
        case CURVEDARROWS:
    /*
  @ curvedarrows color,x1,y1,xc,yc,x2,y2,...,x_(n-1),y_(n-1),xc,yc,x_n,y_n
  @ draw curved arrows from (x1:y1) in direction of (xc:yc) to point (x2:y2), etc<br> note: the curve will <b>not go through</b> point (xc:yc)
  @ use command <a href='#arrowhead'>arrowhead</a> to set the size of the arrow head.
  @ use command <code>linewidth int</code> to adjust thickness of the arrow
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%curvedarrows_drag%size 400,400%xrange -10,10%yrange -10,10%cursor move%linewidth 2%drag xy%curvedarrows red,-8,0,0,8,8,0,-5,5,0,-10,6,3
  @%curvedarrows_click%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%%onclick%curvedarrows red,-8,0,0,8,8,0,-5,5,0,-10,6,3

h[0] = arrowhead
h[1] = type: 1 = single 2=double arrow
function Shape(object_cnt,onclick,direction,type,x,y,w,h,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype0,dashtype1,use_rotate,angle,text,font_size,font_family,use_slider,rotation_center,use_offset)
    */
          stroke_color = get_color(infile,0);/* name or hex color */
          i = 0;
          decimals = find_number_of_digits(precision);
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          while( ! done ){
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            double_data[0] = get_real(infile,0); /* x1 */
            double_data[1] = get_real(infile,0); /* y1 */
            double_data[2] = get_real(infile,0); /* xc */
            double_data[3] = get_real(infile,0); /* yc */
            double_data[4] = get_real(infile,0); /* x3 */
            double_data[5] = get_real(infile,1); /* y3 */
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,21,[%.*f,%.*f,%.*f],[%.*f,%.*f,%.*f],[%d,%d],[%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[4],decimals,double_data[1],decimals,double_data[3],decimals,double_data[5],arrow_head,arrow_head,arrow_head,1,line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
            i = i + 6;
          }
          if(use_rotate == TRUE ){rotate(i-6,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-6,2);}
          dragstuff[21] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case CURVEDARROWS2:
    /*
  @ curvedarrows2 color,x1,y1,xc,yc,x2,y2,...x_(n-1),y_(n-1),xc,yc,x_n,y_n
  @ draw double headed curved arrows from (x1:y1) in direction of (xc:yc) to point (x2:y2), etc. <br> note: the curve will <b>not go through</b> point (xc:yc)
  @ use command <a href='#arrowhead'>arrowhead</a> to set the size of the arrow head.
  @ use command <code>linewidth int</code> to adjust thickness of the arrow
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%curvedarrows2_drag%size 400,400%xrange -10,10%yrange -10,10%cursor move%linewidth 2%drag xy%curvedarrows2 red,-8,0,0,8,8,0,-5,5,0,-10,6,3
  @%curvedarrows2_click%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%%onclick%curvedarrow -5,0,0,-10,5,0,blue%onclick%curvedarrows2 red,-8,0,0,8,8,0,-5,5,0,-10,6,3

h[0] = arrowhead
h[1] = type: 1 = single 2=double arrow
function Shape(object_cnt,onclick,direction,type,x,y,w,h,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype0,dashtype1,use_rotate,angle,text,font_size,font_family,use_slider,rotation_center,use_offset)
    */
          stroke_color = get_color(infile,0);/* name or hex color */
          i = 0;
          decimals = find_number_of_digits(precision);
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          while( ! done ){
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            double_data[0] = get_real(infile,0); /* x1 */
            double_data[1] = get_real(infile,0); /* y1 */
            double_data[2] = get_real(infile,0); /* xc */
            double_data[3] = get_real(infile,0); /* yc */
            double_data[4] = get_real(infile,0); /* x3 */
            double_data[5] = get_real(infile,1); /* y3 */
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,21,[%.*f,%.*f,%.*f],[%.*f,%.*f,%.*f],[%d,%d],[%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[4],decimals,double_data[1],decimals,double_data[3],decimals,double_data[5],arrow_head,arrow_head,arrow_head,2,line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
            i = i + 6;
          }
          if(use_rotate == TRUE ){rotate(i-6,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-6,2);}
          dragstuff[21] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case DASHED:
  /*
  @ dashed
  @ keyword (no arguments required)
  @ next object will be drawn with a dashed line
  @ change dashing scheme by using command <a href="#dashtype">dashtype</a>
  @%dashed%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%line -5,-5,-5,5,red%dashtype 1,1%dline -4,-5,-4,5,green%dashtype 2,2%dline -3,-5,-3,5,blue%dashtype 3,3%dline 0,-5,0,5,orange%dashtype 4,4%dline 3,-5,3,5,brown
  */
          use_dashed = TRUE;
          break;
        case DASHTYPE:
  /*
  @ dashtype line_width_px,space_width_px
  @ every indiviual object may have its own dashtype, if needed...
  @ When keyword <a href='#dashed'>dashed</a> is set, the objects will be drawn with this dashtype
  @ default value <code>dashtype 2,2</code> e.g. 2px line and 2px space
  @ HTML5 canvas specification supports more arguments (dashing schemes) ... but not all modern browsers are yet capable
  @%dashtype%size 400,400%xrange -10,10%yrange -10,10%dashtype 1,1%dhline 0,9,red%dashtype 2,2%dhline 0,8,red%dashtype 4,4%dhline 0,7,red%dashtype 6,6%dhline 0,6,red%dashtype 8,8%dhline 0,5,red%dashtype 10,10%dhline 0,4,red%dashtype 1,2%dhline 0,3,red%dashtype 2,4%dhline 0,2,red%dashtype 3,6%dhline 0,1,red%dashtype 4,8%dhline 0,0,red%linewidth 2%dashtype 1,1%dhline 0,-9,red%dashtype 2,2%dhline 0,-8,red%dashtype 4,4%dhline 0,-7,red%dashtype 6,6%dhline 0,-6,red%dashtype 8,8%dhline 0,-5,red%dashtype 10,10%dhline 0,-4,red%dashtype 1,2%dhline 0,-3,red%dashtype 2,4%dhline 0,-2,red%dashtype 4,8%dhline 0,-1,red
  */
          for(i=0;i<2;i++){
            switch(i){
              case 0 : dashtype[0] = (int) line_width*( get_real(infile,0)) ; break;
              case 1 : dashtype[1] = (int) line_width*( get_real(infile,1)) ; break;
            }
          }
          break;
        case DIAMONDFILL:
  /*
  @ diamondfill x0,y0,dx,dy,color
  @ x0,y0 in xrange / yrange
  @ distances dx,dy in pixels
  @ there is also a command <a href="#userdraw">userdraw diamondfill,color</a>
  @%diamondfill%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%circles red,-4,0,6,4,0,6%linewidth 1%diamondfill 0,0,5,8,blue%diamondfill 0,7,8,8,lightgreen
  */
          js_function[DRAW_DIAMONDFILL] = 1;
          if(js_function[DRAW_FILLTOBORDER] != 1 ){/* use only once */
            js_function[DRAW_FILLTOBORDER] = 1;
            add_js_filltoborder(canvas_type);
          }
          decimals = find_number_of_digits(precision);
          for(i=0;i<5;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0); break; /* x */
              case 1: double_data[1] = get_real(infile,0); break; /* y  */
              case 2: int_data[0] = (int) (get_real(infile,0)); break; /* dx pixel */
              case 3: int_data[1] = (int) (get_real(infile,0)); break; /* dy pixel*/
              case 4: stroke_color = get_color(infile,1);
                if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(2,2);}
              /* draw_diamondfill(ctx,x0,y0,dx,dy,linewidth,color,opacity,xsize,ysize) */
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_diamondfill(%d,%.*f,%.*f,%d,%d,%d,\"%s\",%.2f,%d,%d);\n",STATIC_CANVAS+fill_cnt,decimals,double_data[0],decimals,double_data[1],int_data[0],int_data[1],line_width,stroke_color,stroke_opacity,xsize,ysize));
                add_to_buffer(tmp_buffer);
                fill_cnt++;
                reset();
                break;
              default:break;
            }
          }
          break;
        case DOTFILL:
  /*
  @ dotfill x0,y0,dx,dy,color
  @ x0,y0 in xrange / yrange
  @ distances dx,dy in pixels
  @ radius of dots is linewidth
  @ there is also a command <a href="#userdraw">userdraw dotfill,color</a>
  @%dotfill%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%circles red,-4,0,6,4,0,6%dotfill 0,0,5,8,blue%dotfill 0,7,8,8,lightgreen
  */
          js_function[DRAW_DOTFILL] = 1;
          if(js_function[DRAW_FILLTOBORDER] != 1 ){/* use only once */
            js_function[DRAW_FILLTOBORDER] = 1;
            add_js_filltoborder(canvas_type);
          }
          decimals = find_number_of_digits(precision);
          for(i=0;i<5;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0); break; /* x in px */
              case 1: double_data[1] = get_real(infile,0); break; /* y in py */
              case 2: int_data[0] = (int) (get_real(infile,0)); break; /* dx pixel */
              case 3: int_data[1] = (int) (get_real(infile,0)); break; /* dy pixel*/
              case 4: stroke_color = get_color(infile,1);
                if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(2,2);}
        /* draw_dotfill(ctx,x0,y0,dx,dy,radius,color,opacity,xsize,ysize) */
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_dotfill(%d,%.*f,%.*f,%d,%d,%d,\"%s\",%.2f,%d,%d);\n",FILL_CANVAS+fill_cnt,decimals,double_data[0],decimals,double_data[1],int_data[0],int_data[1],line_width,stroke_color,stroke_opacity,xsize,ysize));
                add_to_buffer(tmp_buffer);
                fill_cnt++;
                reset();
                break;
              default:break;
            }
          }
          break;
        case DRAG:
  /*
  @ drag [x][y][xy]
  @ the next object will be draggable in x / y / xy direction
  @ the displacement can be read by <code>javascript:read_dragdrop();</code>
  @ the precision (default 2 decimals) in the student reply may be set with command <a href="#precision">precision</a>.<br>Use this 'precision' command before this command 'drag x|y|xy' !
  @ <a href='#onclick'>onclick</a> and ''drag x|y|xy`` may be combined (for different objects: a single object can either be onclick or drag, not both )
  @ ''multi_objects`` will be numbered in the given x/y-sequence (example: points red,0,0,1,1,2,2,3,3: point (0:0) is object_number 1)
  @ <b>attention</b>: static objects and ''onclick/drag`` objects of the same type (like point,circle,etc) with the same coordinates (e.g. objects that overlap) will give problems in the ''recognition algorithm``) in this example<br> <code>linewidth 4<br>point 0,0,red<br>drag xy<br>point 0,0,blue</code><br>the red point will not be recognised as draggable ! in the example<br><code>linewidth 4<br>drag xy<br>point 0,0,red<br>drag xy<br>point 0,0,blue</code><br>both points will be recognised
  @ the answer is: drag_or_onclick_object_number : Xorg : Yorg : Xnew : Ynew<br>wherein object_number is the sequence number of the draggable &amp; onclick objects in your script.<br>Only draggable & onclick objects will have an object_number (e.g things like point,crosshair,line,segment,circle,rect,triangle...etc)
  @ use keyword <a href='#snaptogrid'>snaptogrid</a>, <a href='#xsnaptogrid'>xsnaptogrid</a>, <a href='#ysnaptogrid'>ysnaptogrid</a> or command <a href='#snaptopoints'>snaptopoints x1,y1,x2,y2,...</a> to switch from free to discrete movement
  @ in case of external images (commands copy / copyresized) the external image can be set draggable ; always xy. <br>The function javascript;read_canvas() will return the xy-coordinates of all images.
  @%drag_x%size 400,400%xrange -10,10%yrange -10,10%filled%fillcolor lightblue%opacity 200,40%drag x%linewidth 2%circles blue,-5,0,3,0,0,2,5,0,4,0,4,3,0,-3,4
  @%drag_y%size 400,400%xrange -10,10%yrange -10,10%filled%fillcolor lightblue%opacity 200,40%drag y%linewidth 2%circles blue,-5,0,3,0,0,2,5,0,4,0,4,3,0,-3,4
  @%drag_xy%size 400,400%xrange -10,10%yrange -10,10%filled%fillcolor lightblue%opacity 200,40%drag xy%linewidth 2%circles blue,-5,0,3,0,0,2,5,0,4,0,4,3,0,-3,4
  */
          temp = get_string(infile,1);
          if(strstr(temp,"xy") != NULL ){
            drag_type = 0;
          } else {
            if(strstr(temp,"x") != NULL ){
              drag_type = 1;
            }
            else { drag_type = 2;}
          }
      /* assuming all drag&drop coordinates the same precision: so set only once */
          if( print_drag_params_only_once == FALSE ){
            fprintf(js_include_file,"dragdrop_precision = %d;use_dragdrop_reply = true;",precision);
            print_drag_params_only_once = TRUE;
          }
          onclick = 2;
      /* if(use_userdraw == TRUE ){canvas_error("\"drag & drop\" may not be combined with \"userdraw\" or \"pan and zoom\" \n");} */
          use_dragstuff = 2;
          js_function[INTERACTIVE] = 1;
          break;
        case ELLIPSE:
  /*
  @ ellipse xc,yc,width_x,height_y,color
  @ ellipses with center xc/yc and width/height in x/y-range etc (this differs from flydraw syntax!)
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @ will shrink / expand on zoom out / zoom in
  @%ellipse%size 400,400%xrange -10,10%yrange -10,10%filled%fillcolor orange%opacity 200,40%linewidth 3%drag xy%ellipse 0,0,6,4,green%zoom blue
  */
          for(i=0;i<5;i++){
            switch(i){
              case 0:double_data[0] = get_real(infile,0);break; /* x-values */
              case 1:double_data[1] = get_real(infile,0);break; /* y-values */
              case 2:double_data[2] = get_real(infile,0);break; /* rx -> px */
              case 3:double_data[3] = get_real(infile,0);break; /* ry -> px */
              case 4:stroke_color = get_color(infile,1);/* name or hex color */
                if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,4);}
                if(use_affine == TRUE ){ transform(2,4);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,3,[%.*f],[%.*f],[%.*f],[%.*f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[1],decimals,double_data[2],decimals,double_data[3],line_width,stroke_color,stroke_opacity,stroke_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}/* object_cnt++; */
                dragstuff[3] = 1;
                if(use_dragstuff == 0 ){ use_dragstuff = 1; }
                reset();
              break;
            }
          }
          break;
        case ELLIPSES:
  /*
  @ ellipses color,xc1,yc1,width_x1,height_y1,xc2,yc2,width_x2,height_y2,xc3,yc3,width_x3,height_y3,...
  @ ellipses with center and height in x/y-range etc (this differs from flydraw syntax!)
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @ will shrink / expand on zoom out / zoom in
  @%ellipses%size 400,400%xrange -10,10%yrange -10,10%filled%fillcolor orange%opacity 200,40%linewidth 3%onclick%ellipses red,-3,0,2,4,0,0,4,2,3,0,6,2
  */

          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
          i=1;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            switch (i%4){
              case 1:double_data[i-1] = get_real(infile,0);break; /* x */
              case 2:double_data[i-1] = get_real(infile,0);break; /* y */
              case 3:double_data[i-1] = get_real(infile,0);break; /* rx */
              case 0:double_data[i-1] = get_real(infile,1);break; /* ry */
              default: break;
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,4);}
          if(use_affine == TRUE ){ transform(i-1,4);}
          decimals = find_number_of_digits(precision);
          for(c = 0 ; c+3 < i ; c = c+4){
            check_string_length(string_length);
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,3,[%.*f],[%.*f],[%.*f],[%.*f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+1],decimals,double_data[c+2],decimals,double_data[c+3],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;} /* object_cnt++; */
          }
          reset();
          dragstuff[3] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case FILLALL:
  /*
  @ fillall color,x1,y1,x2,y2...x_n,y_n
  @ fill all the region containing points (x1:y1),(x2:y2)...(x_n:y_n) with color 'color'
  @ any color (object) in the <a href="#canvastype">canvastype</a> will act as border to the bucket fill
  @ use this command after all boundary objects are declared.
  @ Use command 'userdraw clickfill,color' for user click driven flood fill.
  @ use command <a href="#canvastype">canvastype </a> to fill another canvas (default should be fine: DRAG_CANVAS = 5)
  @ note: the fill-family of commands are very (client) cpu intensive operations!<br>filling is done pixel by pixel e.g. image size of 400x400 uses 160000 pixels: each pixel contains 4 data (R,G,B,Opacity) = 640000 data.<br>on every data a few operations / comparisons are done...<br>So have pity on your students CPU..
  @%fillall%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%vlines black,-5,0,-4,0,3,0&hlines black,-5,0,-5,4,-5,-2&%circles green,0,0,2,3,3,5,-5,-5,3%opacity 240,50%fillall blue,1,1,8,8,-8,-8
  */
          decimals = find_number_of_digits(precision);
          fill_color=get_color(infile,0); /* how nice: now the color comes first...*/
          i=0;
          if(js_function[DRAW_FILLTOBORDER] != 1 ){/* use only once */
            js_function[DRAW_FILLTOBORDER] = 1;
            add_js_filltoborder(canvas_type);
          }
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "setTimeout(function(){filltoborder(%.*f,%.*f,false,[%s,%d],%d,false,null);},1000);\n",decimals,double_data[i-1],decimals,double_data[i],fill_color,(int) (fill_opacity/0.0039215),FILL_CANVAS+fill_cnt));
              add_to_buffer(tmp_buffer);
              fill_cnt++;
            }
            i++;
          }
          break;
        case FILLED:
  /*
  @ filled
  @ keyword (no arguments required)
  @ the next ''fillable`` object (only the next !) will be filled
  @ use command <a href="#fillcolor">fillcolor color</a> to set fillcolor
  @ use <a href="#fillpattern">fillpattern</a> for non-solid color filling.
  @ use command <code>opacity 0-255,0-255</code> to set stroke and fill-opacity
  @ use command <a href='#fill'>fill x,y,color</a> or <a href="#floodfill">floodfill x,y,color</a> to fill the space around (x;y) with color <br>pixel operation implemented in javascript: use with care !
  */
          use_filled = 1;
          break;
        case FILLCOLOR:
  /*
  @ fillcolor colorname or #hex
  @ set the color: mainly used for command 'userdraw obj,stroke_color'
  @ or in forms as polygon, rects and so on (add filled it the form is not already filled, so if you use rect, need to put filled, if you use frect, no need).
  @ see <a href="#fillpattern">fillpattern</a> for non-solid color filling.
  */
          fillcolor = get_color(infile,1);
          fill_color = fillcolor;
          break;
        case FILLPATTERN:
  /*
  @ fillpattern grid | hatch | diamond | dot | image_url
  @ alternative: settile image_url
  @ use a pattern as fillstyle
  @ suitable for all fillable object including the <a href="#userdraw">userdraw objects' family</a>
  @ note: do not use the ''f`` for a fillable object : this is used exclusively for solid color filling.
  @ the fillcolor is set by the object command, for example:<br><code>size 370,370<br>xrange -5,5<br>yrange -5,5<br>opacity 165,150<br>fillpattern grid<br>circle -6,3,160,blue<br>fillpattern dot<br>circle -3,-3,160,red<br>fillpattern hatch<br>circle 0,3,160,green<br>fillpattern diamond<br>circle 3,-3,160,cyan<br>userdraw dotfill,blue<br>zoom red</code>
  @ the pattern dimensions are hardcoded (linewidth, radius,dx,dy are fixed)
  @ the pattern color is set by command <a href='#fillcolor'>fillcolor</a> and <a href='#opacity'>opacity</a>
  @ see <a href="#fillcolor">fillcolor</a> for solid color filling.
  @ when using an image-url, make sure it contains an ''/`` in the filename...''fillpattern &#36;module_dir/gifs/test.jpg`` will fill the next fillable object with this image.|<br>the argument to html5 canvas routine 'createPattern(img,argument)' is set to ''repeat`` e.g. if the image is smaller then the canvas, multiple copies will be used to fill the area ( e.g. ctx.fillStyle() = pattern)<br>for example:<br><code>size 150,150<br>xrange -5,5<br>yrange -5,5<br>drag xy<br>fillpattern gifs/en.gif<br>circle 0,0,100,red<br>fillpattern gifs/nl.gif<br>drag xy<br>circle -3,2,100,green<br>fillpattern gifs/cn.gif<br>drag xy<br>circle 3,2,100,green</code>
  @ fillpattern is also active for <a href="#userdraw">userdraw object,color</a>...<br>the userdraw family a has also ''clickfill type`` (e.g. an object gets filled between boundaries, when clicked) commands like:<br>'userdraw dotfill,color'<br>'userdraw hatchfill,color' etc
  @%fillpattern_1%size 400,400%xrange -5,5%yrange -5,5%opacity 165,150%fillpattern grid%circle -6,3,160,blue%fillpattern dot%circle -3,-3,160,red%fillpattern hatch%circle 0,3,160,green%fillpattern diamond%circle 3,-3,160,cyan%zoom red
  @%fillpattern_2%size 400,400%xrange -10,10%yrange -10,10%linewidth 3%fillcolor green%fillpattern hatch%#fillpattern dot,diamond,grid,imageurl%userdraw circle,red
  */
          temp = get_string(infile,1);
          use_filled = 0;
          js_function[DRAW_FILL_PATTERN] = 1;
          if( strstr(temp,"grid") != 0 ){ use_filled = 2;}
          else{
            if( strstr(temp,"hatch") != 0 ){ use_filled = 3;}
            else{
              if( strstr(temp,"diamond") != 0 ){ use_filled = 4;}
              else{
                if( strstr(temp,"dot") != 0 ){ use_filled = 5;}
                else{
                  if( strstr(temp,"/") != 0 ){ /* get_image_from_url() needs to be called after function definition...*/
                    use_filled = 6;js_function[JS_LOAD_IMAGE] = 1;
                    tmp_buffer=my_newmem(MAX_BUFFER);
                    check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "get_image_from_url(\"%s\");",temp));
                    add_to_buffer(tmp_buffer);
                  }
                }
              }
            }
          }
          if( use_filled == 0 ){canvas_error("fillpattern unknown or typo...choose grid,hatch,diamond of dot...");}
          break;
        case FILLTOBORDER:
  /*
  @ filltoborder x,y,bordercolor,color
  @ fill the region of point (x:y) with color 'color'
  @ any other color than bordercolor will not act as border to the bucket fill
  @ the filling is done in the connex component whih contains (x,y) of the set of points which are in a color different of bordercolor
  @ use this command after all boundary objects are declared.
  @ it can be useful to increase strokeopacity or linewidth
  @ use command <a href="#canvastype">canvastype </a> to fill another canvas (default should be fine: DRAG_CANVAS = 5)
  @ note: filltoborder is a very (client) cpu intensive operation!<br>filling is done pixel by pixel e.g. image size of 400x400 uses 160000 pixels: each pixel contains 4 data (R,G,B,Opacity) = 640000 data.<br>on every data a few operations / comparisons are done...<br>So have pity on your students CPU..
  @ maybe used together with command <a href="#userdraw">userdraw clickfill,color</a>
  @%filltoborder%size 400,400%xrange -10,10%yrange -10,10%canvastype 100%linewidth 2%precision 1000%jsplot blue,5*sin(x)%opacity 200,50%filltoborder 6,6,blue,green%filltoborder 6,-6,blue,red
  */
          for(i=0 ;i < 4 ; i++){
            switch(i){
              case 0:double_data[0] = get_real(infile,0);break;
              case 1:double_data[1] = get_real(infile,0);break;
              case 2:bgcolor = get_color(infile,0);break;
              case 3:fill_color = get_color(infile,1);
          if(js_function[DRAW_FILLTOBORDER] != 1 ){/* use only once */
            js_function[DRAW_FILLTOBORDER] = 1;
            add_js_filltoborder(canvas_type);
          }
          decimals = find_number_of_digits(precision);
         /* we need to set a timeout: the canvas is not yet draw in memory? when floodfill is called directly... */
          tmp_buffer=my_newmem(MAX_BUFFER);
          check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "setTimeout(function(){filltoborder(%.*f,%.*f,[%s,%d],[%s,%d],%d,false,null)},1000);\n",decimals,double_data[0],decimals,double_data[1],bgcolor,(int) (fill_opacity/0.0039215),fill_color,(int) (fill_opacity/0.0039215),FILL_CANVAS+fill_cnt));
          add_to_buffer(tmp_buffer);
          fill_cnt++;
          reset();
          break;
              default:break;
            }
          }
          break;
        case FLOODFILL:
  /*
  @ floodfill x,y,color
  @ alternative: fill
  @ fill the region of point (x:y) with color 'color'
  @ any other color or size of picture (borders of picture) will act as border to the bucket fill
  @ use this command after all boundary objects are declared.
  @ Use command <code>userdraw clickfill,color</code> for user click driven flood fill.
  @ use command <a href="#canvastype">canvastype </a> to fill another canvas (default should be fine: DRAG_CANVAS = 5)
  @ note: floodfill is a very (client) cpu intensive operation!<br>filling is done pixel by pixel e.g. image size of 400x400 uses 160000 pixels: each pixel contains 4 data (R,G,B,Opacity) = 640000 data.<br>on every data a few operations / comparisons are done...<br>So have pity on your students CPU..
  @%floodfill%size 400,400%xrange -10,10%yrange -10,10%canvastype 100%linewidth 2%precision 1000%jsplot blue,5*sin(x)%opacity 200,50%floodfill 6,6,blue%floodfill 6,-6,red
  */
          for(i=0 ;i < 4 ; i++){
            switch(i){
              case 0:double_data[0] = get_real(infile,0);break;
              case 1:double_data[1] = get_real(infile,0);break;
              case 2:fill_color = get_color(infile,1);
                if(js_function[DRAW_FILLTOBORDER] != 1 ){/* use only once */
                  js_function[DRAW_FILLTOBORDER] = 1;
                  add_js_filltoborder(canvas_type);
                }
                decimals = find_number_of_digits(precision);
         /* we need to set a timeout: the canvas is not yet draw in memory? when floodfill is called directly... */
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "setTimeout(function(){filltoborder(%.*f,%.*f,false,[%s,%d],%d,false,null);},1000);\n",decimals,double_data[0],decimals,double_data[1],fill_color,(int) (fill_opacity/0.0039215),FILL_CANVAS+fill_cnt));
                add_to_buffer(tmp_buffer);
                fill_cnt++;
                break;
            default:break;
            }
          }
          reset();
          break;
        case FONTCOLOR:
  /*
  @ fontcolor color
  @ color: hexcolor or colorname
  @ default: black
  @ use command <a href="#fontfamily">fontfamily</a> to deviate from default font type
  @%fontcolor%size 400,400%xrange -10,10%yrange -10,10%fontcolor red%#note: use command fontfamily to change size and shape%axis%axisnumbering%grid 2,2,grey,2,2,4,grey
  */
          font_color = get_color(infile,1);
          break;
        case FONTFAMILY:
  /*
  @ fontfamily font_description
  @ set the font family; for browsers that support it
  @ font_description: Arial, Courier, Helvetica etc
  @ in case commands <code>string color,x,y,the string</code>, <code>stringup color,x,y,rotation,the string</code>, ''fontfamily`` can be something like:<code>fontfamily italic 34pt Arial</code>. Use correct syntax: ''font style``, ''font size pt``, ''fontfamily``
  @%fontfamily%size 400,400%xrange -10,10%yrange -10,10  %fontfamily Bold 10pt Arial%string blue,-9,9,10 pt Arial%fontfamily Italic 20pt Arial%string blue,0,9,20 pt Arial%fontfamily Bold 10pt Helvetica%string blue,-9,5,10 pt Helvetica%fontfamily Italic 20pt Helvetica%string blue,0,5,20 pt Helvetica %fontfamily Bold 10pt Courier%string blue,-9,0,10 pt Courier%fontfamily Italic 20pt Courier%string blue,0,0,20 pt Courier%fontfamily Bold 10pt Fixed%string blue,-9,-5,10 pt Fixed%fontfamily Italic 20pt Fixed%string blue,0,-5,20 pt Fixed %fontfamily Bold 10pt Times%string blue,-9,-9,10 pt Times%fontfamily Italic 20pt Times%string blue,0,-9,20 pt Times

  */
          font_family = get_string(infile,1);
          break;
        case FONTSIZE:
  /*
  @ fontsize font_size
  @ default value 12
  @ note: for some macros (like ''grid | legend | xaxistext | xlabel`` etc) sometimes command <a href="#fontfamily">fontfamily</a> can be used for some specific font-setting<br>this is however not always very straight forward... so just try and see what happens
  */
          font_size = (int) (get_real(infile,1));
          break;
        case FUNCTION_LABEL:
  /*
  @ functionlabel label_1:label_2:label_3...
  @ alternative: functionlabels
  @ default value ''f(x)=:g(x)=:h(x)=:i(x)=:j(x)=:k(x)=:m(x)=:n(x)=``
  @ no mathml allowed (just ascii string)
  @ use command <a href='#fontsize'>fontsize int</a> to adjust the size
  @ use command <a href='#strokecolor'>strokecolor colorname</a> to adjust the labels (individually, if needed)
  @ if needed, use before every command <a href='#userinput'>userinput function | inputfield | textarea</a>
  @ no limit in amount of inputfields for userbased function plotting
  @%function_label%size 400,400%xrange -5,5%yrange -5,5%precision 0%axis%axisnumbering%opacity 100,190%grid 1,1,grey,2,2,5,black%linewidth 3%linewidth 1%precision 1000%functionlabels F(x)=:H(x)=:Yield(x)=%strokecolor green%userinput function%strokecolor red%userinput function%strokecolor blue%userinput function
  */
          temp = get_string_argument(infile,1);
          function_label = list2js_array(temp,":");
          break;
        case GRID:/* xmajor,ymajor,gridcolor [,xminor,yminor,tick length (px), axis/tickscolor]*/
  /*
  @ grid step_x,step_y,gridcolor
  @ if keywords <a href="#axis">axis</a> or <a href="#axisnumbering">axisnumbering</a> are set, use: <code>grid step_x,step_y,major_color,minor_x,minor_y,tics height in px,axis_color</code> minor x step = step_x / minor_x
  @ in that case, use command <a href="#fontcolor">fontcolor</a>, <a href="#fontsize">fontsize</a> and / or <a href="#fontfamily">fontfamily</a> to adjust font; defaults: black,12,Arial
  @ if xmin > 0 and/or ymin > 0 and zooming / panning is not active: be aware that the x/y-axis numbering and x/y major/minor tic marks will not be visual as they are placed under the x-axis and left to the y-axis (in Quadrant II and IV)
  @ can <b>not</b> be set <a href="#onclick">onclick</a> or <a href="#drag">drag xy</a>
  @ use commands <a href="#xlabel">xlabel some_string</a> and/or <a href="#ylabel">ylabel some_string</a> to label axis; use command ''fontsize`` to adjust size: the font family is non-configurable 'italic your_fontsize px Arial' !
  @ see commands <a href="#xaxis">xaxis or xaxistext</a>, <a href="#yaxis">yaxis or yaxistext</a> to set tailormade values on axis (the used font is set by command <a href="#fontfamily">fontfamily</a>; default '12px Arial')
  @ see command <a href="#legend">legend</a> to set a legend for the graph; use command <a href="#fontsize">fontsize</a> to adjust size (the font family is non-configurable 'bold your_fontsize px Arial')
  @%grid%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%zoom red
  @%grid_axis%size 400,400%xrange -10,10%yrange -10,10%axis%grid 1,1,grey,2,2,6,black%zoom red
  @%grid_axis_axisnumbering%size 400,400%xrange -10,10%yrange -10,10%fontfamily Italic 14px Courier%axis%axisnumbering%precision 0%grid 1,1,grey,2,2,6,black%zoom red
  */
          if( js_function[DRAW_YLOGSCALE] == 1 ){canvas_error("only one grid type is allowed...");}
          js_function[DRAW_GRID] = 1;
          for(i=0;i<4;i++){
            switch(i){
              case 0:double_data[0] = get_real(infile,0);break;/* xmajor */
              case 1:double_data[1] = get_real(infile,0);break;/* ymajor */
              case 2:
              if( use_axis == TRUE ){
                stroke_color = get_color(infile,0);
                done = FALSE;
                int_data[0] = (int) (get_real(infile,0));/* xminor */
                int_data[1] = (int) (get_real(infile,0));/* yminor */
                int_data[2] = (int) (get_real(infile,0));/* tic_length */
                fill_color = get_color(infile,1); /* used as axis_color*/
              }
              else {
              int_data[0] = 1;
              int_data[1] = 1;
              stroke_color = get_color(infile,1);
              fill_color = stroke_color;
              }
              if( double_data[0] <= 0 ||  double_data[1] <= 0 ||  int_data[0] <= 0 ||  int_data[1] <= 0 ){canvas_error("major or minor ticks must be positive !");}
              /* set snap_x snap_y values in pixels */
              fprintf(js_include_file,"snap_x = %f;snap_y = %f;",double_data[0] / int_data[0],double_data[1] / int_data[1]);
              fprintf(js_include_file,"\n/* add grid */function redraw_grid(){draw_grid%d(%d,%d,%.2f,%.*f,%.*f,%d,%d,%d,%d,\"%s\",\"%s\",%d,\"%s\",%d,%d,%d,%d,%d,\"%s\",%.2f);return;};",canvas_root_id,GRID_CANVAS,precision,stroke_opacity,decimals,double_data[0],decimals,double_data[1],int_data[0],int_data[1],int_data[2],line_width,stroke_color,fill_color,font_size,font_family,use_axis,use_axis_numbering,use_dashed,dashtype[0],dashtype[1],font_color,fill_opacity);
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "redraw_grid();\n"));
              add_to_buffer(tmp_buffer);
              break;
            }
          }
          reset();
          break;
        case GRIDFILL:
  /*
  @ gridfill x0,y0,dx,dy,color
  @ x0,y0 in xrange / yrange
  @ distances dx,dy in pixels
  @ there is also a command <a href="#userdraw">userdraw gridfill,color</a>
  @%gridfill%size 400,400%xrange -10,10%yrange -10,10%canvastype 100%linewidth 2%precision 1000%jsplot blue,5*sin(x)%opacity 200,50%gridfill 6,6,10,10,blue%gridfill 6,-6,6,6,red

  */
          js_function[DRAW_GRIDFILL] = 1;
          decimals = find_number_of_digits(precision);
          if(js_function[DRAW_FILLTOBORDER] != 1 ){/* use only once */
            js_function[DRAW_FILLTOBORDER] = 1;
            add_js_filltoborder(canvas_type);
          }
          for(i=0;i<5;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0); break; /* x  */
              case 1: double_data[1] = get_real(infile,0); break; /* y  */
              case 2: int_data[0] = (int) (get_real(infile,0)); break; /* dx pixel */
              case 3: int_data[1] = (int) (get_real(infile,0)); break; /* dy pixel*/
              case 4: stroke_color = get_color(infile,1);

              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_gridfill(%d,%.*f,%.*f,%d,%d,%d,\"%s\",%.2f,%d,%d);\n",STATIC_CANVAS+fill_cnt,decimals,double_data[0],decimals,double_data[1],int_data[0],int_data[1],line_width,stroke_color,stroke_opacity,xsize,ysize));
              add_to_buffer(tmp_buffer);
              fill_cnt++;
              reset();
              break;
              default:break;
            }
          }
          break;
        case GROUP:
  /*
  @ group
  @ keyword
  @ work in 'progress'
  @ all objects(*) after the command and until <a href="#kill">kill group</a> or <a href="#killslider">killslider</a> may be moved together with mouse moverments<br> (*) for now all real canvas objects and latex / xml ; but no images (work in progress)
  @ may be combined with slider driven movements or drag & drop
  @%group%%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%precision 100%linewidth 4%rotationcenter 0,0%slider -2*pi,2*pi,300,40,angle active,rotate%snaptogrid%ftriangle 0,-4,-2,-2,4,--5,blue%fillcolor red%fcircle -4.4,4.4,40,blue%kill slider%slider -20,20,300,40,x active,x-slide%ftriangle -2,0,0,4,2,0,green%fcircle 4.4,4.5,40,red%kill slider%slider -20,20,300,40,y active,y-slide%fcircle 4,-4,40,green%linewidth 4%fcircle -4,-4,40,orange%kill slider%group%fcircle -4.4,4.5,10,blue%fcircle -5.4,4.5,10,blue%fcircle -6.4,4.5,10,blue%fcircle -4.4,8,16,green%fcircle -5.4,8,16,green%fcircle -6.4,8,16,green%mouse red,22%zoom red
  */
          use_slider++;
          add_slider(2);
          no_reset = TRUE;
          int c = 0;
          for(i=last_slider;i<=use_slider;i++){ int_data[c] = i; c++; }
          my_sliders = data2js_array(int_data,use_slider - last_slider+1);
          if( precision == 0 ){precision = 100;}
          onclick = 5;
          use_dragstuff = 2;
          drag_type = 0;
          js_function[INTERACTIVE] = 1;
          fprintf(js_include_file,"var slider%d;dragdrop_precision = %d;use_dragdrop_reply = true;",use_slider,precision);
          tmp_buffer=my_newmem(MAX_BUFFER);
          check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "slider%d = new move_group(%d);\n",use_slider,use_slider));
          add_to_buffer(tmp_buffer);
          break;
        case HALFLINE:
  /*
  @ demiline x1,y1,x2,y2,color
  @ alternative: halfline
  @ draws a halfline starting in (x1:y1) and through (x2:y2) in color 'color' (colorname or hex)
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%halfline%size 400,400%xrange -10,10%yrange -10,10%halfline -5,5,0,0,red%halfline -5,-5,0,0,blue
  */
          for(i=0; i<13;i++){double_data[i]=0;}
          for(i=0;i<5;i++){
            switch(i){
              case 0: double_data[0]= get_real(infile,0);break; /* x-values */
              case 1: double_data[1]= get_real(infile,0);break; /* y-values */
              case 2: double_data[10]= get_real(infile,0);break; /* x-values */
              case 3: double_data[11]= get_real(infile,0);break; /* y-values */
              case 4: stroke_color=get_color(infile,1);/* name or hex color */
                if(double_data[0] == double_data[10]){ /* vertical halfline */
                  if(double_data[1] < double_data[11]){
                    double_data[3] = ymax + 1000;
                  }
                  else {
                    double_data[3] = ymin - 1000;
                  }
                  double_data[2] = double_data[0];
                } else { /* horizontal halfline*/
                  if( double_data[1] == double_data[11] ){
                    if( double_data[0] < double_data[10] ){
                      double_data[2] = xmax + 1000; /* halfline to the right */
                    }
                    else {
                      double_data[2] = xmin - 1000; /* halfline to the left */
                    }
                    double_data[3] = double_data[1];
                  }
                else {
          /* any other halfline */
          /* slope */
                  double_data[12] = (double_data[11] - double_data[1])/(double_data[10] - double_data[0]);
                /* const */
                  double_data[13] = double_data[1] - double_data[12]*double_data[0];
                  if( double_data[0] < double_data[10] ){
                    double_data[2] = double_data[2] + 1000;
                  }
                  else {
                    double_data[2] = double_data[2] - 1000;
                  }
                  double_data[3] = double_data[12]*double_data[2] + double_data[13];
                }
              }
              if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
              if(use_rotate == TRUE ){rotate(4,angle,rotationcenter,2);}
              if(use_affine == TRUE ){ transform(4,2);}
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,18,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[1],decimals,double_data[3],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
              add_to_buffer(tmp_buffer);
              if(onclick != 0){object_cnt++;}
        /* object_cnt++; */
              reset();
              break;
            }
          }
          dragstuff[18] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case HALFLINES:
  /*
  @ demilines color,x1,y1,x2,y2,....
  @ alternative: halflines
  @ draws halflines starting in (x1:y1) and through (x2:y2) in color 'color' (colorname or hex) etc
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> indiviually
  @%halflines%size 400,400%xrange -10,10%yrange -10,10%halflines red,-5,5,0,0,-5,-5,0,0
  */
          stroke_color=get_color(infile,0);
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            } else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          decimals = find_number_of_digits(precision);
          for(c = 0 ; c+3 < i ; c = c+4){
            if( double_data[c] == double_data[c+2] ){ /* vertical line*/
              if(double_data[c+1] < double_data[c+3]){ /* upright halfline */
                double_data[c+3] = ymax + 1000;
              }
              else {
                double_data[c+3] = ymin - 1000;/* descending halfline */
              }
            }
            else {
              if( double_data[c+1] == double_data[c+3] ){ /* horizontal line */
                if(double_data[c] < double_data[c+2] ){ /* halfline to the right */
                  double_data[c+2] = xmax+100;
                }
                else {
                  double_data[c+2] = xmin-1000; /* halfline to the right */
                }
              }
              else {
      /* m */
                double m = (double_data[c+3] - double_data[c+1]) /(double_data[c+2] - double_data[c]);
      /* q */
                double q = double_data[c+1] - ((double_data[c+3] - double_data[c+1]) /(double_data[c+2] - double_data[c]))*double_data[c];
                if(double_data[c] < double_data[c+2]){ /* to the right */
                  double_data[c+2] = xmax+1000; /* 1000 is needed for dragging...otherwise it is just segment */
                  double_data[c+3] = (m)*(double_data[c+2])+(q);
                  }
                  else { /* to the left */
                    double_data[c+2] = xmin - 1000;
                    double_data[c+3] = (m)*(double_data[c+2])+(q);
                  }
                }
              }
              if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
              if(use_affine == TRUE ){ transform(i-1,2);}
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,18,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+2],decimals,double_data[c+1],decimals,double_data[c+3],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
              add_to_buffer(tmp_buffer);
              if(onclick != 0){object_cnt++;}/* object_cnt++; */
            }
          reset();
          dragstuff[18] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case HATCHFILL:
  /*
  @ hatchfill x0,y0,dx,dy,color
  @ x0,y0 in xrange / yrange
  @ distances dx,dy in pixels
  @ there is also a command <a href="#userdraw">userdraw hatchfill,color</a>
  @%hatchfill%size 400,400%xrange -10,10%yrange -10,10%canvastype 100%linewidth 2%precision 1000%jsplot blue,5*sin(x)%opacity 200,50%hatchfill 6,6,10,10,blue%hatchfill 6,-6,6,6,red
  */
          js_function[DRAW_HATCHFILL] = 1;
          if(js_function[DRAW_FILLTOBORDER] != 1 ){/* use only once */
            js_function[DRAW_FILLTOBORDER] = 1;
            add_js_filltoborder(canvas_type);
          }
          decimals = find_number_of_digits(precision);
          for(i=0;i<5;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0); break; /* x */
              case 1: double_data[1] = get_real(infile,0); break; /* y  */
              case 2: int_data[0] = (int) (get_real(infile,0)); break; /* dx pixel */
              case 3: int_data[1] = (int) (get_real(infile,0)); break; /* dy pixel*/
              case 4: stroke_color = get_color(infile,1);
                if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(2,2);}
                /* draw_hatchfill(ctx,x0,y0,dx,dy,linewidth,color,opacity,xsize,ysize) */
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_hatchfill(%d,%.*f,%.*f,%d,%d,%d,\"%s\",%.2f,%d,%d);\n",STATIC_CANVAS+fill_cnt,decimals,double_data[0],decimals,double_data[1],int_data[0],int_data[1],line_width,stroke_color,stroke_opacity,xsize,ysize));
                add_to_buffer(tmp_buffer);
                fill_cnt++;
                reset();
                break;
              default:break;
            }
          }
          break;
        case HLINE:
  /*
  @ hline x,y,color
  @ alternative: horizontalline
  @ draw a horizontal line through point (x:y) in color 'color'
  @ or use command <a href='#curve'>curve color,formula</a> to draw the line (uses more points to draw the line; is however better draggable)
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%hline%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%hline 0,0,red%dhline 0,5,blue
  */
          for(i=0;i<3;i++) {
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x-values */
              case 1: double_data[1] = get_real(infile,0);break; /* y-values */
              case 2: stroke_color = get_color(infile,1);/* name or hex color */
                double_data[3] = double_data[1];
                if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(2,2);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,100*xmin,decimals,100*xmax,decimals,double_data[1],decimals,double_data[3],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}/* object_cnt++; */
                dragstuff[4] = 1;
                if(use_dragstuff == 0 ){ use_dragstuff = 1; }
                reset();
                break;
            }
          }
          break;
        case HLINES:
  /*
  @ hlines color,x1,y1,x2,y2,...
  @ alternative: horizontallines
  @ draw horizontal lines through points (x1:y1)...(xn:yn) in color 'color'
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually
  @%hlines%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%hlines red,0,0,0,5,0,-5
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
                double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */

          decimals = find_number_of_digits(precision);
          for(c = 0 ; c < i-1 ; c = c+2){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,100*xmin,decimals,100*xmax,decimals,double_data[c+1],decimals,double_data[c+1],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}/* object_cnt++; */
          }
          reset();
          dragstuff[4] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case HTTP:
  /*
  @ http x1,y1,x2,y2,http://some_adress.com
  @ an active html-page will be displayed in an "iframe" rectangle left top (x1:y1), right bottom (x2:y2)
  @ do not use interactivity (or mouse) if the mouse needs to be active in the iframe
  @ can <b>not</b> be ''set onclick`` or ''drag xy``
  @%http%size 400,400%xrange -10,10%yrange -10,10%http 0,10,10,0,http://wims.unice.fr%opacity 200,50%drag xy%fcircle 0,0,100,green
  */
          js_function[DRAW_HTTP] = 1;
          for(i=0;i<5;i++){
            switch(i){
              case 0: int_data[0]=x2px(get_real(infile,0));break; /* x in x/y-range coord system -> pixel width */
              case 1: int_data[1]=y2px(get_real(infile,0));break; /* y in x/y-range coord system  -> pixel height */
              case 2: int_data[2]=x2px(get_real(infile,0)) - int_data[0];break; /* width in x/y-range coord system -> pixel width */
              case 3: int_data[3]=y2px(get_real(infile,0)) - int_data[1];break; /* height in x/y-range coord system  -> pixel height */
              case 4: decimals = find_number_of_digits(precision);
                temp = get_string(infile,1);
                if(strstr(temp,"\"") != 0 ){ temp = str_replace(temp,"\"","'");}
                tmp_buffer = my_newmem(string_length+2);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_http(%d,%d,%d,%d,%d,\"%s\");\n",canvas_root_id,int_data[0],int_data[1],int_data[2],int_data[3],temp));
                add_to_buffer(tmp_buffer);
               break;
            }
          }
          reset();
          break;
        case HTML:
  /*
  @ html x1,y1,html_string
  @ all tags are allowed, html code using inputfields could be read using your own javascript code. Do not use ids like 'canvas_input0' etc.
  @ can be set <a href='#onclick'>onclick</a>  and <a href='#drag'>drag&amp;drop</a>
  @ command <a href='#affine'>affine</a> will produce CSS3 matrix transformations
  @ command <a href='#rotate'>rotate</a> will rotate the object
  @ use keyword <a href='#centered'>centered</a> to center the html object on (x1:y1)
  @ note: using drag&amp;drop for all external P,SPAN,DIV,IMG,SVG-images onto a canvasdraw element, use ''onclick=javascript:place_image_on_canvas(this.id)``
  @ note: sub &amp; sup are supported in command family <a href='#string'>string</a>, e.g. real internal canvas objects !
  @%html-text%size 500,500%xrange -10,10%yrange -10,10%drag xy%centered%fillcolor lightblue%html 2,2,<h1 style='color:red'>DRAG ME</h1> %opacity 200,50%drag xy%fcircle 0,0,100,green
  @%html-image-slider%size 500,500%xrange -10,10%yrange -10,10%fontsize 42%rotationcenter 0,0%centered%html 0,0,<img src="http://85.148.206.56/gifs/can1.gif">%slider 0,pi,400,40,angle active degree, %centered%html 0,0,<img src="http://85.148.206.56/gifs/can2.gif">
  */
          js_function[DRAW_XML] = 1;
          for(i=0;i<5;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break;
              case 1: double_data[1] = get_real(infile,0);break;
              case 4: decimals = find_number_of_digits(precision);
                if(use_affine == TRUE ){ transform(2,2);}/* needs double_data[] */
                if( use_offset != 0 || drag_type != -1 ){int_data[2] = 1;}else{int_data[2] = 0;} /* only centered or not-centered */
                int_data[0] = x2px(double_data[0]);/* needs px */
                int_data[1] = y2px(double_data[1]);
                if( use_slider != -1 && onclick == 0){ onclick = 5; }/* no drag&onclick but slideable */
                if( use_slider != -1 && drag_type != -1){ onclick = 5; }
                temp = get_string(infile,1);
                if( strstr(temp,"\"") != 0 ){ temp = str_replace(temp,"\"","\\\""); }
                if( strstr(temp,"<img ")!= 0){URL="image";}else{URL="html";}
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "var draw_xml%d = {id:%d,type:'%s',x:[%d],y:[%d],mathml:\"%s\",drag_type:%d,onclick:%d,object_cnt:%d,stroke_color:\"%s\",stroke_opacity:%.2f,fill_color:\"%s\",fill_opacity:%.2f,use_center:%d,use_snap:%d,angle:%f,fontfamily:\"%s\",transform:%s,use_affine:%d,offset:[0,0],use_slider:%s,rotation_center:%s,once:true};slidergroup[%d] = null;draw_xml(draw_xml%d);\n",drawxml_cnt,drawxml_cnt,URL,int_data[0],int_data[1],temp,drag_type,onclick,object_cnt,stroke_color,stroke_opacity,fill_color,fill_opacity,int_data[2],use_snap,angle,font_family,doubledata2js_array(affine_matrix,6,decimals),use_affine,my_sliders,rotation_center,object_cnt,drawxml_cnt));
                add_to_buffer(tmp_buffer);
                if(onclick != 0 ){object_cnt++;}
                drawxml_cnt++;/* keeps track on imported img,div,p,span,mathml,svg */
                break;
              default:break;
            }
          }
          reset();
          break;
        case IMAGEFILL:
  /*
  @ imagefill x,y,scaling to xsize &times; ysize?,image_url
  @ The next suitable <b>filled object</b> will be filled with "image_url" tiled
  @ scaling to xsize &times; ysize ? ... 1 = yes 0 = no
  @ After pattern filling, the fill-color should be reset !
  @ wims getins / image from class directory: imagefill 80,80,my_image.gif
  @ normal url: imagefill 80,80,0,&#36;module_dir/gifs/my_image.gif
  @ normal url: imagefill 80,80,1,http://adres/a/b/c/my_image.jpg
  @ if dx,dy is larger than the image, the whole image will be background to the next object.
  @%imagefill_tile%size 400,400%xrange -10,10%yrange -10,10%linewidth 3%circles blue,0,0,5,3,2,5%imagefill 1.5,1.5,0,gifs/en.gif%imagefill -5,5,0,gifs/logo/wimsedu.png
  @%imagefill_scale%size 400,400%xrange -10,10%yrange -10,10%linewidth 3%circles blue,0,0,5,3,2,5%imagefill 1.5,1.5,1,gifs/en.gif%imagefill -5,5,1,gifs/logo/wimsedu.png
  */
          js_function[DRAW_IMAGEFILL] = 1;
          if(js_function[DRAW_FILLTOBORDER] != 1 ){/* use only once */
            js_function[DRAW_FILLTOBORDER] = 1;
            add_js_filltoborder(canvas_type);
          }
          for(i=0 ;i < 4 ; i++){
            switch(i){
              case 0:int_data[0] = (int) (get_real(infile,0));break;
              case 1:int_data[1] = (int) (get_real(infile,0));break;
              case 2:int_data[2] = (int) (get_real(infile,0));break; /* 0 | 1 */
              case 3: URL = get_string_argument(infile,1);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_imagefill(%d,%d,%d,\"%s\",%d,%d,%d,%d);\n",STATIC_CANVAS+fill_cnt,int_data[0],int_data[1],URL,xsize,ysize,use_userdraw,int_data[2]));
                add_to_buffer(tmp_buffer);
                fill_cnt++;
              break;
            }
          }
          reset();
          break;
        case IMAGEPALETTE:
  /*
  @ imagepalette image1,image2,image3,...
  @ if used before and together with command <a href='#multidraw'>multidraw images,..,..., etc</a> the image will be presented in a small table in the ''control panel``.
  @%imagepalette%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%copy 0,0,-1,-1,-1,-1,gifs/images/skull_and_crossbones50.png%fontcolor green%fontfamily Bold 42pt Arial%imagepalette gifs/ca.gif,gifs/en.gif,gifs/nl.gif,gifs/fr.gif,gifs/cn.gif,gifs/de.gif,gifs/kh.gif,gifs/it.gif%multiuserinput 0,0,1%css color:blue;%multisnaptogrid 1,1,1%multilinewidth 0,4,0%# attention: use unicode text input without the slash %# \u222D ---> u222D at least will sometimes work%# otherwise cut&past unicode symbols into inputfield...%multilabel TEXT,REACTION ARROW,FLAGS,STOP DRAWING%multidraw text,arrow,images
   */
          temp = get_string(infile,1);
          temp = str_replace(temp,",","\",\"");
          if( use_tooltip == 1 ){canvas_error("command 'imagepalette' is incompatible with command 'intooltip tip_text',as they use the same div-element ");}
          fprintf(js_include_file,"\nvar current_id;var imagepalette = [\" %s \"];\n",temp);
          break;
        case INPUT:
  /*
  @ input x,y,size,editable,value
  @ to set inputfield "readonly", use editable = 0
  @ if no preset 'value' is needed...use a 'space' as last item argument
  @ only active inputfields (editable = 1) will be read with read_canvas();
  @ if ''&#36;status=done`` (e.g. in answer.phtml) the inputfield will be cleared and set readonly<br>override this by keyword <a href="#status">status</a>
  @ may be further controlled by <a href="#css">css</a>
  @ if mathml inputfields are present and / or some userdraw is performed, these data will <b>not</b> be send as well (javascript:read_canvas();)
  @ use keyword <a href='#xoffset'>xoffset | centered</a> if the inputfield should be centered on (x:y)<br> default is the left top corner is (x:y)
  @ if the student must place an inputfield(s) somewhere on the canvas, use command <a href="#userdraw">userdraw input,color</a> or make use of a command like <a href="#userdraw">userdraw text,color</a>
  @%input%size 400,400%xrange -10,10%yrange -10,10%linewidth 6%point 1,2,red%input 1,2,5,1, ?%point -5,5,red%input -5,5,5,1, ?%point 6,-5,red%input 6,-5,5,1, ?%point -5,-8,red%input -5,-8,5,1, ?
  */
          js_function[DRAW_INPUTS] = 1;
          for(i = 0 ; i<5;i++){
            switch(i){
              case 0: int_data[0]=x2px(get_real(infile,0));break;/* x in px */
              case 1: int_data[1]=y2px(get_real(infile,0));break;/* y in px */
              case 2: int_data[2]=abs( (int)(get_real(infile,0)));break; /* size */
              case 3: if( get_real(infile,1) >0){int_data[3] = 1;}else{int_data[3] = 0;};break; /* readonly */
              case 4: temp = get_string(infile,3);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_static_inputs(%d,%d,%d,%d,%d,%d,\"%s\",\"%s\",%d);\n",canvas_root_id,input_cnt,int_data[0],int_data[1],int_data[2],int_data[3],css_class,temp,use_offset));
                add_to_buffer(tmp_buffer);
                input_cnt++;break;
              default: break;
            }
          }
          if(reply_format == 0 ){reply_format = 15;}
          reset();
          break;
        case INTOOLTIP:
  /*
  @ intooltip link_text
  @ link_text is a single line (span-element)
  @ link_text may also be an image URL ''http://some_server/images/my_image.png`` or ''&#36;module_dir/gifs/my_image.jpg``
  @ link_text may contain HTML markup
  @ the canvas will be displayed in a tooltip on ''link_text``
  @ the canvas is default transparent: use command <a href="#bgcolor">bgcolor color</a> to adjust background-color, the link text will also be shown with this 'bgcolor'.
  @ many ''userinput stuff`` will use the tooltip_placeholder_div element...only one is defined in the wims-page<br>and are therefore these commands are mutually exclusive.<br>keep this in mind...
  @%intooltip%size 400,400%xrange -10,10%yrange -10,10%fontfamily Bold 42pt Courier%string black,0,0,Hello World%intooltip <span style="background-color:black;color:white;font-style:bold;font-size:48pt;">CLICK <br>HERE</span>
  */
          if(use_input_xy != FALSE ){canvas_error("intooltip can not be combined with userinput_xy or other commands using the tooltip-div...see documentation");}
          if( use_tooltip == 1 ){ canvas_error("command 'intooltip' cannot be combined with command 'popup'...");}
          tooltip_text = get_string(infile,1);
          if(strstr(tooltip_text,"\"") != 0 ){ tooltip_text = str_replace(tooltip_text,"\"","'"); }
          use_tooltip = 1;
          break;
        case JSCURVE:
  /*
  @ jscurve color,formula1(x),formula2(x),formula3(x),...
  @ alternative: jsplot
  @ your function will be plotted by the javascript engine of the client browser
  @ if <a href='trange'>trange</a> is defined, the two functions will be plotted parametric<br><b>note</b>: use <i>x</i> as variable...and not <i>t</i>. Use keyword <a href='#animate'>animate</a> to animate a point on the curve
  @ use only basic math in your curve: <code>sqrt,^,asin,acos,atan,log,pi,abs,sin,cos,tan,e</code>
  @ use parenthesis and rawmath: use 2*x instead of 2x ; use 2^(sin(x))...etc etc (use error console to debug any errors...)
  @ <b>attention</b>: last ''precision`` command in the canvasdraw script determines the calculation precision of the javascript curve plot !
  @ no validity check is done by wims.
  @ zooming & panning are implemented:<br>use command ''zoom color`` for mouse driven zooming<br>or use keyword 'setlimits' for inputfields setting xmin/xmax, ymin/ymax
  @ zooming & panning is better than for curves produced by command <a href="#curve">curve color,formula</a> because for avery change in x/y-range the curve is recalculated in javascript
  @ zooming & panning in case of userbased functionplot: reclick the OK button to re-plot curve onto the resized grid
  @ use keyword <a href='animate'>animate</a> for animating a point on the curve
  @ use command ''trace_jscurve formula(x)`` for tracing
  @ use command ''jsmath formula(x)`` for calculating and displaying indiviual points on the curve
  @ can <b>not</b> be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> (yet)
  @ commands plotjump / plotstep are not active for ''jscurve``
  @ every command jscurve will produce a new canvas (canvastype 111,112,113...) for this one curve.
  @ plotting multiple js-curves on the same canvas (for example if you want to use 'userdraw clickfill,color' on <a href="#canvastype">canvastype</a> number 111, use:<br> <code>jscurve red,fun1(x),fun2(x)...fun_n(x)</code>, you must specify individual multistrokecolors &amp; multistrokeopacity &amp; multilinewidth for these multiple js-curves to use different colors. Otherwise all curves will be the same color... Use commands like: <a href="#multistrokecolors">multistrokecolors</a>, <a href="#multilinewidth">multilinewidth</a>, <a href="#multidash">multidash</a>, <a href="#multistrokeopacity">multistroke</a>, <b>color</b> given for the command <code>jscurve color,formulas(x)</code> will not be used in that case... but the color argument must still be given in any case (otherwise syntax error...)
  @%jscurve%size 400,400%xrange -10,10%yrange -10,10%multistrokecolors red,green,blue,orange%multilinewidth 1,2,3%multistrokeopacity 0.5,0.8,1.0%jscurve red,sin(x),1/sin(x),sin(x^2)
  */
          jsplot_cnt++;/* -1 --> 0 */
          stroke_color = get_color(infile,0);
          js_function[JS_MATH] = 1;
          js_function[JS_PLOT] = 1;
          if( tmin != 0 && tmax !=0){use_parametric = TRUE;}
          temp = get_string(infile,1);
          temp = str_replace(temp,",","\",\"");
          tmp_buffer=my_newmem(MAX_BUFFER);
          check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "var js_plot%d = function(){jsplot(%d,[\"%s\"],[%d],[\"%s\"],[%.2f],[%d],%d,%d,[%f,%f],%d,%d,%d,0);};js_plot%d();",jsplot_cnt,JSPLOT_CANVAS+jsplot_cnt,temp,line_width,stroke_color,stroke_opacity,use_dashed,dashtype[0],dashtype[1],tmin,tmax,plot_steps,use_parametric,use_animate,jsplot_cnt));
          add_to_buffer(tmp_buffer);
          fprintf(js_include_file,"if(typeof(all_jsplots) !== 'number'){var all_jsplots;};all_jsplots = %d;",jsplot_cnt);

       /* we need to create multiple canvasses, so we may zoom and pan ?? */
          break;
        case JSMATH:
  /*
  @ jsmath some_math_function
  @ will calculate an y-value from a userinput x-value and draws a crosshair on these coordinates.
  @ default labels ''x`` and ''y``; the commands ''xlabel some_x_axis_name`` and ''ylabel some_y_axis_name`` will set the label for the input fields
  @ use command 'css some_css' for styling the display fields. Use command 'fontsize int' to size the labels ''x`` and ''y``
  @ the client browser will convert your math function to javascript math.<br>use parenthesis and rawmath: use 2*x instead of 2x etc etc<br>no check is done on the validity of your function and/or syntax<br>use error console to debug any errors...
  @ be aware that the formula's of the plotted function(s) can be found in the page javascript source
  @%jsmath%size 400,400%xrange -10,10%yrange -10,10%jsplot blue,sin(x^2)%jsmath sin(x^2)
  */
          js_function[DRAW_CROSSHAIRS] = 1;
          js_function[JS_MATH] = 1;
          add_calc_y(get_string(infile,1),font_size,css_class);
          break;
        case KILL:
  /*
  @ kill arguments
  @ arguments may be: affine linear translation rotation slider offset  reset
  @ for documentation see: killaffine,killlinear,killtranslation...
  @ multiple arguments are allowed (although not checked for validity...)
  */
          temp = get_string(infile,1);
          if(strstr(temp,"affine") != 0 ){use_affine = FALSE;affine_matrix[0] = 1.0;affine_matrix[1] = 0.0;affine_matrix[2] = 0.0;affine_matrix[3] = 1.0;affine_matrix[4] = 0.0;affine_matrix[5] = 0.0;}
          if(strstr(temp,"linear") != 0 ){affine_matrix[0] = 1.0;affine_matrix[1] = 0.0;affine_matrix[2] = 0.0;affine_matrix[3] = 1.0;}
          if(strstr(temp,"translation") != 0 || strstr(temp,"translate") != 0 ){affine_matrix[4] = 0.0;affine_matrix[5] = 0.0;}
          if(strstr(temp,"rotation") != 0 || strstr(temp,"rotate") != 0 ){use_rotate = FALSE;angle = 0.0;rotation_center="null";}
          if(strstr(temp,"slider") != 0 ){slider_type = "0";my_sliders = "[-1]";last_slider = use_slider+1;}
          if(strstr(temp,"moebius") != 0 ){use_moebius = FALSE; moebius_matrix[0] = 0.0;moebius_matrix[1] = 0.0;moebius_matrix[2] = 0.0;}
          if(strstr(temp,"group") != 0 ){onclick = 0;drag_type = -1;slider_type = "0";my_sliders = "[-1]";last_slider = use_slider+1;use_slider = -1;reset();}
          if(strstr(temp,"reset") != 0 ){if(no_reset == FALSE){no_reset = TRUE;}else{no_reset = FALSE;reset();}}
          if(strstr(temp,"offset") != 0 ){use_offset = 0;}
          break;
        case KILLAFFINE:
  /*
  @ killaffine
  @ keyword: resets the transformation matrix to 1,0,0,1,0,0
  @ note: any active linear transformation will also be reset: tx=0, ty=0
  */
          use_affine = FALSE;
          affine_matrix[0] = 1.0;
          affine_matrix[1] = 0.0;
          affine_matrix[2] = 0.0;
          affine_matrix[3] = 1.0;
          affine_matrix[4] = 0.0;
          affine_matrix[5] = 0.0;
          break;
        case KILLLINEAR:
  /*
  @ killlinear
  @ keyword: resets the transformation matrix to 1,0,0,1,tx,ty
  @ note:any active transformation or rotation will not be killed (tx,ty remain active)
  */
          affine_matrix[0] = 1.0;
          affine_matrix[1] = 0.0;
          affine_matrix[2] = 0.0;
          affine_matrix[3] = 1.0;
          break;
        case KILLMOEBIUS:
  /*
  @ killmoebius
  @ keyword: resets the moebius vector to 0,0,0
  */
          use_moebius = FALSE;
          moebius_matrix[0] = 0.0;
          moebius_matrix[1] = 0.0;
          moebius_matrix[2] = 0.0;
          break;
        case KILLROTATE:
  /*
  @ killrotate
  @ will set the rotation angle to 0.
  @ will also reset the command <a href="#rotationcenter">rotationcenter</a> to the first (x;y) of the next rotatable/slidable object(s) <br>eg a following rotate command will have the first object point as rotation center
  @ if not set, the rotation center will remain unchanged
  @ note:any active transformation or linear will not be killed (e.g an active transformation matrix remains active)
  @%killrotate%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%rotationcenter 0,0%# slider is active for all following objects until 'killslider'%slider -10,10,300,40,angle degree,Rotate%drag xy%# after dragging the object must be clicked to re-activate the slider%linewidth 4%arrow 0,0,6,0,9,blue%%# reset rotation center to the first (x,y) of the next object/arrow...%killrotate%drag xy%# after dragging the object must be clicked to re-activate the slider%arrow 0,0,6,1,9,red
  */
          use_rotate = FALSE;
          angle = 0.0;
          rotation_center="null";
          break;
        case KILLSLIDER:
  /*
  @ killslider
  @ keyword (no arguments required)
  @ ends grouping of object under a previously defined slider
  */
          slider_type = "0";
          my_sliders = "[-1]";
          last_slider = use_slider+1;
          break;
        case KILLTRANSLATION:
  /*
  @ killtranslation
  @ alternative: killtranslate
  @ note: a active linear or affine transformation will not be 100% reset...only tx=0,ty=0
  @ resets the translation matrix a,b,c,d,tx,ty to a,b,c,d,0,0
  */
          affine_matrix[4] = 0.0;
          affine_matrix[5] = 0.0;
          break;
        case LATEX:
  /*
  @ latex x,y,tex string
  @ alternative: math
  @ note: <b>for a single greek letter</b> ,please be smart and use a command like <a href='#string'>string</a> along with <b>unicode</b> !! <br>possibly together with command <a href="#xyoffset">xoffset, yoffset or xyoffset</a><br> See <a target='new' href='https://en.wikipedia.org/wiki/Mathematical_operators_and_symbols_in_Unicode'>https://en.wikipedia.org/wiki/Mathematical_operators_and_symbols_in_Unicode</a><br> See <a target='new' href='https://en.wikipedia.org/wiki/Greek_script_in_Unicode'>https://en.wikipedia.org/wiki/Greek_script_in_Unicode</a>
  @ you may also use command <a href="#mathml">mathml</a> for xml strings generated with wims commmand ''mathmlmath`` (will not work on KaTeX enabled WIMS)
  @ transformation commands <a href='#affine'>affine</a>, <a href='#translation'>translation</a> and <a href='#rotate'>rotate</a> are supported.(onclick and drag will work)
  @ can be set onclick: <code>javascript:read_dragdrop();</code> will return click numbers of mathml-objects<br>if 4 clickable object are drawn, the reply could be 1,0,1,0 ... meaning clicked on the first and third object
  @ can be set draggable:<code>javascript:read_dragdrop();</code> will return all coordinates in the same order as the canvas script: unmoved object will have their original coordinates...
  @ can be moved/rotated with command <a href='#slider'>slider</a>
  @ snaptogrid is supported
  @ when clicked, the color of the 'div background' of the 'mathobject' will be determined by the <a href="#fillcolor">fillcolor</a> and <a href="#opacity">opacity</a> settings
  @ userdraw may be combined with 'latex' ; the js-function 'read_canvas()' will contain the coordinates of the drawing.
  @ userdraw may be combined; the read_canvas() will contain the drawing.
  @ draggable or onclick 'external images' from command <a href='#copyresized'>copy or copyresized</a> and all objects from commands <a href='#html'>html</a> or <a href='#obabel'>obabel</a> can be combined with drag and/or onclick mathml
  @ other drag objects (circles/rects etc) are supported, but read_dragdrop() will probably be difficult to interpret...
  @ if inputfields are incorporated in mathml (with id's: id='mathml0',id='mathml1',...id='mathml_n')<br>the user_input values will be read by <code>javascript:read_mathml();</code>. <b>attention</b>: if after this mathml-input object other user-interactions are included, these will read mathml too using "read_canvas();"
  @ If other inputfields (command input / command textarea) or userdraw are performed, the function read_canvas() will not read mathml. Use some generic function to read it....
  @ use keyword <a href='#centered'>centered</a> to center the katex div object on (x1:y1) <br>this may not work as expected for MathJaX [TO BE TESTED]
  @ note: if you want to include external TeX via drag&amp;drop onto a canvasdraw element, use \\mmlid{integer} in the tex-command:''!insmath \\mmlid{1}\\frac{1}{\pi}``<br> (if your wims_mathml does not support it...use <a href="http://85.148.206.56/wims/download/Mathml.tar.gz">this version...</a>)
  @ note: the same is true for all external P,SPAN,DIV,IMG,SVG-images via drag&amp;drop onto a canvasdraw element, use ''onclick=javascript:place_image_on_canvas(this.id)``
  @%latex_drag%size 400,400%xrange -10,10%yrange -10,10%grid 2,2,grey%strokecolor red%drag xy%centered%latex -6,5,\\frac{1}{2}+ \\frac{\\pi}{2}%strokecolor blue%drag xy%centered%latex -3,5,\\frac{1}{3}+ \\frac{\\pi}{3}%strokecolor green%drag xy%centered%latex 0,5,\\frac{1}{4}+ \\frac{\\pi}{4}%strokecolor orange%drag xy%fontfamily 26px Times%centered%latex 3,5,\\frac{1}{5}+ \\frac{\\pi}{6}
  @%latex%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 3%vline 0,0,green%hline 0,0,green%fontfamily 16px Arial%fillcolor orange%strokecolor blue%linewidth 2%slider 0,4*pi,400,40,angle degree active, %centered%latex 0,0,\\frac{12345}{23456} \\times \\frac{12345}{23456}%translate 4,4%centered%rotate 180%drag xy%html 0,0,<table><tr><th>HTML TABLE</th></tr><tr><th><a href="https://wimsedu.info/">WIMS EDU </a> </th></tr><tr><td><img src="gifs/en.gif"></tr><tr><td><img src="gifs/nl.gif"></tr><tr><td><img src="gifs/fr.gif"></tr><tr><td><img src="gifs/cn.gif"></tr><tr><td><img src="gifs/it.gif"></tr></table>%userdraw arrow2,red
  */
          js_function[DRAW_XML] = 1;
          for(i=0;i<3;i++){
            switch(i){
              case 0: double_data[0]=get_real(infile,0);break; /* x in x/y-range coord system -> pixel width */
              case 1: double_data[1]=get_real(infile,0);break; /* y in x/y-range coord system  -> pixel height */
              case 2: decimals = find_number_of_digits(precision);
                temp = get_string(infile,1);
                if(use_affine == TRUE ){ transform(2,2);}/* slider will use css-rotate transformation */
                if( use_offset != 0 || drag_type != -1 ){int_data[2] = 1;}else{int_data[2] = 0;} /* only centered or not-centered */
                int_data[0] = x2px(double_data[0]);
                int_data[1] = y2px(double_data[1]);
                if( use_slider != -1 && onclick == 0 ){ onclick = 5;}
                if( use_slider != -1 && drag_type != -1){ onclick = 5; }
#ifdef KATEX_INSTALLED
                if( strstr(temp,"\\") != 0 ){ temp = str_replace(temp,"\\","\\\\"); }
                if( strstr(temp,"\"") != 0 ){ temp = str_replace(temp,"\"","'"); }
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "var draw_xml%d = {id:%d,type:'katex',x:[%d],y:[%d],mathml:\"%s\",drag_type:%d,onclick:%d,object_cnt:%d,stroke_color:\"%s\",stroke_opacity:%.2f,fill_color:\"%s\",fill_opacity:%.2f,use_center:%d,use_snap:%d,angle:%f,fontfamily:\"%s\",transform:%s,use_affine:%d,offset:[0,0],use_slider:%s,rotation_center:%s,once:true};draw_xml(draw_xml%d);\n",drawxml_cnt,drawxml_cnt,int_data[0],int_data[1],temp,drag_type,onclick,object_cnt,stroke_color,stroke_opacity,fill_color,fill_opacity,int_data[2],use_snap,angle,font_family,doubledata2js_array(affine_matrix,6,decimals),use_affine,my_sliders,rotation_center,drawxml_cnt));
#else
                temp = getMML(temp);/* generate MathML for Firefox or MathJaX */
                if( strstr(temp,"\"") != 0 ){ temp = str_replace(temp,"\"","'"); }
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "var draw_xml%d = {id:%d,type:'mathml',x:[%d],y:[%d],mathml:\"%s\",drag_type:%d,onclick:%d,object_cnt:%d,stroke_color:\"%s\",stroke_opacity:%.2f,fill_color:\"%s\",fill_opacity:%.2f,use_center:%d,use_snap:%d,angle:%f,fontfamily:\"%s\",transform:%s,use_affine:%d,offset:[0,0],use_slider:%s,rotation_center:%s,once:true};draw_xml(draw_xml%d);\n",drawxml_cnt,drawxml_cnt,int_data[0],int_data[1],temp,drag_type,onclick,object_cnt,stroke_color,stroke_opacity,fill_color,fill_opacity,int_data[2],use_snap,angle,font_family,doubledata2js_array(affine_matrix,6,decimals),use_affine,my_sliders,rotation_center,drawxml_cnt));
#endif
                add_to_buffer(tmp_buffer);
                if(onclick != 0 ){object_cnt++;}
                drawxml_cnt++;/* keeps track on imported img,div,p,span,mathml,svg */
                break;
              default:break;
              }
          }
          reset();
          break;
        case LATTICE:
  /*
  @ lattice x0,y0,xv1,yv1,xv2,yv2,n1,n2,color
  @ can <b>not</b> be set ''onclick`` or ''drag xy``
  @%lattice%size 400,400%xrange -10,10%yrange -10,10%fillcolor red%linewidth 2%lattice -10,-10,0,1,1,1,10,10,red%fillcolor blue%lattice 10,-10,0,1,-1,1,10,10,blue
  */
          js_function[DRAW_LATTICE] = 1;
          for( i = 0; i<9; i++){
            switch(i){
              case 0: int_data[0] = x2px(get_real(infile,0));break; /* x0-values  -> x-pixels*/
              case 1: int_data[1] = y2px(get_real(infile,0));break; /* y0-values  -> y-pixels*/
              case 2: int_data[2] = (int) (get_real(infile,0));break; /* x1-values  -> x-pixels*/
              case 3: int_data[3] = (int) -1*(get_real(infile,0));break; /* y1-values  -> y-pixels*/
              case 4: int_data[4] = (int) (get_real(infile,0));break; /* x2-values  -> x-pixels*/
              case 5: int_data[5] = (int) -1*(get_real(infile,0));break; /* y2-values  -> y-pixels*/
              case 6: int_data[6] = (int) (get_real(infile,0));break; /* n1-values */
              case 7: int_data[7] = (int) (get_real(infile,0));break; /* n2-values */
              case 8: stroke_color=get_color(infile,1);
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_lattice(%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,\"%s\",%.2f,\"%s\",%.2f,%d,%.2f,%d); ",STATIC_CANVAS,line_width,int_data[0],int_data[1],int_data[2],int_data[3],int_data[4],int_data[5],int_data[6],int_data[7],fill_color,fill_opacity,stroke_color,stroke_opacity,use_rotate,angle,use_filled));
                add_to_buffer(tmp_buffer);break;
              default:break;
            }
          }
          reset();
          break;
        case LINEAR:
  /*
  @ linear a,b,c,d
  @ defines a transformation matrix for subsequent objects
  @ use keyword <a href='#killlinear'>killlinear</a> to end the transformation...the next objects will be drawn in the original x/y-range
  @ a: Scales the drawings horizontally
  @ b: Skews the drawings horizontally
  @ c: Skews the drawings vertically
  @ d: Scales the drawings vertically
  @ the data precision may be set by preceding command ''precision int``
  @ note: any active translation (tx,ty) is not changed
  @%linear%size 400,400%xrange -10,10%yrange -10,10%opacity 255,255%fcircle 5,5,40,blue%linear 0.2,0,0,0.2%fcircle 5,5,40,green
  */
          for(i = 0 ; i<4;i++){
            switch(i){
              case 0: affine_matrix[0] = get_real(infile,0);break;
              case 1: affine_matrix[1] = get_real(infile,0);break;
              case 2: affine_matrix[2] = get_real(infile,0);break;
              case 3: affine_matrix[3] = get_real(infile,1);
                affine_matrix[4] = 0;affine_matrix[5] = 0;
                use_affine = TRUE;
                break;
              default: break;
            }
          }
          reset();
         break;
        case LINE:
  /*
  @ line x1,y1,x2,y2,color
  @ draw a line through points (x1:y1)--(x2:y2) in color ''color``
  @ or use command ''curve color,formula`` to draw the line (uses more points to draw the line; is however better draggable)
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%line%size 400,400%xrange -10,10%yrange -10,10%line 0,1,2,-1,green
  */
          for(i=0;i<5;i++){
            switch(i){
              case 0: double_data[10]= get_real(infile,0);break; /* x-values */
              case 1: double_data[11]= get_real(infile,0);break; /* y-values */
              case 2: double_data[12]= get_real(infile,0);break; /* x-values */
              case 3: double_data[13]= get_real(infile,0);break; /* y-values */
              case 4: stroke_color=get_color(infile,1);/* name or hex color */
                if( double_data[10] == double_data[12] ){ /* vertical line*/
                  double_data[1] = xmin;
                  double_data[3] = ymax;
                  double_data[0] = double_data[10];
                  double_data[2] = double_data[10];
                }
                else{
                  if( double_data[11] == double_data[13] ){ /* horizontal line */
                    double_data[1] = double_data[11];
                    double_data[3] = double_data[11];
                    double_data[0] = ymin;
                    double_data[2] = xmax;
                }
                else {
      /* m */
                  double_data[5] = (double_data[13] - double_data[11]) /(double_data[12] - double_data[10]);
      /* q */
                  double_data[6] = double_data[11] - ((double_data[13] - double_data[11]) /(double_data[12] - double_data[10]))*double_data[10];
      /*xmin,m*xmin+q,xmax,m*xmax+q*/
                  double_data[1] = (double_data[5])*(xmin)+(double_data[6]);
                  double_data[3] = (double_data[5])*(xmax)+(double_data[6]);
                  double_data[0] = xmin;
                  double_data[2] = xmax;
                }
              }
              if(use_rotate == TRUE ){rotate(4,angle,rotationcenter,2);}
              if(use_affine == TRUE ){ transform(4,2);}
              if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
              decimals = find_number_of_digits(precision);
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[1],decimals,double_data[3],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
              add_to_buffer(tmp_buffer);
              if(onclick != 0){object_cnt++;}
        /* object_cnt++;*/
              reset();
              break;
            }
          }
          dragstuff[4] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case LINES:
  /*
  @ lines color,x1,y1,x2,y2...x_n-1,y_n-1,x_n,y_n
  @ draw multiple lines through points (x1:y1)--(x2:y2) ...(x_n-1:y_n-1)--(x_n:y_n) in color 'color'
  @ or use multiple commands ''curve color,formula`` or ''jscurve color,formule`` to draw the line <br>(uses more points to draw the line; is however better draggable)
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%lines%size 400,400%xrange -10,10%yrange -10,10%lines green,0,1,1,3,0,0,1,3,0,0,-2,1
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          decimals = find_number_of_digits(precision);
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          for(c = 0 ; c+3 < i ; c = c+4){
            if(double_data[c] == double_data[c+2] ){ /* vertical line*/
              double_data[c+1] = xmin;
              double_data[c+3] = ymax;
              double_data[c+2] = double_data[c];
            }
            else {
              if( double_data[c+1] == double_data[c+3] ){ /* horizontal line */
                double_data[c+3] = double_data[c+1];
                double_data[c] = ymin;
                double_data[c+2] = xmax;
              }
              else {
      /* m */
                double m = (double_data[c+3] - double_data[c+1]) /(double_data[c+2] - double_data[c]);
      /* q */
                double q = double_data[c+1] - ((double_data[c+3] - double_data[c+1]) /(double_data[c+2] - double_data[c]))*double_data[c];
      /*xmin,m*xmin+q,xmax,m*xmax+q*/
                double_data[c+1] = (m)*(xmin)+(q);
                double_data[c+3] = (m)*(xmax)+(q);
                double_data[c] = xmin;
                double_data[c+2] = xmax;
              }
            }
            if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
            if(use_affine == TRUE ){ transform(i-1,2);}
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+2],decimals,double_data[c+1],decimals,double_data[c+3],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
    /* object_cnt++; */
          }
          dragstuff[4] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case LINEWIDTH:
  /*
  @ linewidth int
  @ default 1
  @%linewidth%size 400,400%xrange -10,10%yrange -10,10%linewidth 1%line -5,-5,-5,5,red%linewidth 2%line -4,-5,-4,5,green%linewidth 3%line -3,-5,-3,5,blue%linewidth 4%line -2,-5,-2,5,orange%linewidth 1%line -1,-5,-1,5,brown%linewidth 5%line 1,-5,1,5,cyan%linewidth 6%line 3,-5,3,5,purple%linewidth 7%line 5,-5,5,5,black
  */
          line_width = (int) (get_real(infile,1));
          break;
        case LEVELSTEP:
          level_step = (int) (get_real(infile,1));
          if (level_step < 1) level_step=1;
          if (level_step > 16) level_step=16;
          break;
        case LEVELCURVE:
  /*
  @ levelcurve color,expression in x/y,l1,l2,...
  @ draws very primitive level curves for expression, with levels l1,l2,l3,...,l_n
  @ the quality is <b>not to be compared</b> with the Flydraw levelcurve. <br>(choose flydraw if you want quality...)
  @ every individual level curve may be set 'onclick / drag xy' <br>e.g. every single level curve (l1,l2,l3...l_n) has a unique identifier
  @ note: the arrays for holding the javascript data are limited in size
  @ note: reduce image size if javascript data arrays get overloaded<br>(command 'plotsteps int' will not control the data size of the plot...)
  @%levelcurve%size 400,400%xrange -10,10%yrange -10,10%levelcurve red,x*y,1,2,3,4
  */
          fill_color = get_color(infile,0);
          char *fun1 = get_string_argument(infile,0);
          if( strlen(fun1) == 0 ){canvas_error("function is NOT OK !");}
          i = 0;
          done = FALSE;
          while( !done ){
            double_data[i] = get_real(infile,1);
            i++;
          }
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          for(c = 0 ; c < i; c++){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,16,%s,[%d],[%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,eval_levelcurve(xsize,ysize,fun1,xmin,xmax,ymin,ymax,level_step,precision,double_data[c]),line_width,line_width,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
           /* object_cnt++; */
          }
          dragstuff[16] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case LEGEND:
  /*
  @ legend string1:string2:string3....string_n
  @ will be used to create a legend for a graph
  @ also see command <a href='#piechart'>piechart</a>
  @ will use the same colors per default as used in the graphs; use command <a href='#legendcolors'>legendcolors</a> to override the default
  @ use command <a href="#fontsize">fontsize</a> to adjust. (command ''fontfamily`` is not active for command ''legend``)
  @%legend%size 400,400%xrange -10,10%yrange -10,10%bgcolor white%fontsize 16%legend legend 1:legend 2:legend 3:legend 4:this is legend 5:legend 6:another legend abc%legendcolors red:green:blue%grid 1,1,white%# note: command "grid" is mandatory%# just set grid invisible if not wanted
  */
          temp = get_string(infile,1);
          if( strstr( temp,":") != 0 ){ temp = str_replace(temp,":","\",\""); }
          legend_cnt++; /* attention: starts with -1: it will be used in piechart etc */
          fprintf(js_include_file,"var legend%d = [\"%s\"];",legend_cnt,temp);
          break;
        case LEGENDCOLORS:
  /*
  @ legendcolors color1:color2:color3:...:color_n
  @ will be used to color a legend: use this command after the legend command ! e.g. <code>legend test1:test2:test3<br>legendcolors blue:red:orange</code>.
  @ make sure the number of colors match the number of legend items
  @ command ''legend`` in case of ''piechart`` and ''barchart`` will use these colors per default (no need to specify ''legendcolors``)
  @%legendcolors%size 400,400%xrange -10,10%yrange -10,10%fontsize 18%legend legend 1:legend 2:legend 3%legendcolors red:green:blue%grid 1,1,grey
  */
          if(legend_cnt == -1){canvas_error("use command \"legend\" before command \"legendcolors\" ! ");}
          temp = get_string(infile,1);
          if( strstr( temp,":") != 0 ){ temp = str_replace(temp,":","\",\""); }
          fprintf(js_include_file,"var legendcolors%d = [\"%s\"];",legend_cnt,temp);
          break;
        case LINEGRAPH: /* scheme: var linegraph_0 = [ 'stroke_color','line_width','use_dashed', 'dashtype0','dashtype1','x1','y1',...,'x_n','y_n'];*/
  /*
  @ linegraph x1:y1:x2:y2...x_n:y_n
  @ will plot your data in a graph
  @ may <b>only</b> to be used together with command <a href='#grid'>grid</a>
  @ can be used together with freestyle x-axis/y-axis texts: see commands <a href='#xaxis'>xaxis</a>,<a href='#xaxisup'>xaxisup</a> and <a href='#yaxis'>yaxis</a>
  @ use command <a href='#legend'>legend</a> to provide an optional legend in right-top-corner
  @ also see command <a href='#piechart'>piechart</a>
  @ multiple linegraphs may be used in a single plot
  @ note: your arguments are not checked by canvasdraw: use your javascript console in case of trouble...
  @ <ul><li>use command <a href='#strokecolor'>strokecolor</a> before a command ''linegraph`` to set the color of this graph</li><li>use command <a href='#linewidth'>linewidth</a> before command ''linegraph`` to set linewidth of this graph</li><li>use keyword <a href='#dashed'>dashed</a> before command ''linegraph`` to set dashing of the graph</li><li>if dashing is set, use command <a href='#dashtype'>dashtype</a> before command ''linegraph`` to set the type of dashing of the (individual) graph</li></ul>
  @%linegraph%size 400,400%xrange -10,10%yrange -10,10%strokecolor red%linegraph -10:1:-5:-1:3:3:10:-5%strokecolor blue%linegraph -10:-1:-5:1:3:-3:10:5%dashed%strokecolor green%linegraph -10:2:-5:-2:3:4:10:2%grid 1,1,grey
  */
          temp = get_string(infile,1);
          if( strstr( temp,":") != 0 ){ temp = str_replace(temp,":","\",\""); }
          fprintf(js_include_file,"var linegraph_%d = [\"%s\",\"%d\",\"%d\",\"%d\",\"%d\",\"%s\"];",linegraph_cnt,stroke_color,line_width,use_dashed,dashtype[0],dashtype[1],temp);
          linegraph_cnt++;
          reset();
          break;
        case MATHML:
  /*
  @ mathml x1,y1,mathml_string
  @ this command is special for GECKO browsers, and it makes use of Native Mathml
  @ For general use with all browsers, use command <a href='#latex'>latex</a>
  @ can be set <a href='onclick'>onclick</a>  and <a href='drag'>drag&amp;drop</a> <br>Note: dragging is fairly primitive dragging of the div-element, and is not done using the <em>dragstuff library</em>
  @ command <a href='#affine'>affine</a> will produce CSS3 matrix transformations
  @ command <a href='#rotate'>rotate</a> will rotate the object
  @ the mathml object is centered at (x1:y1)
  @ the ''mathml_string`` can be produced using WIMS commands like ''texmath`` followed by ''mathmlmath``... or write correct TeX and use only ''mathmlmath``
  @ mathml will be displayed in a rectangle left top (x1:y1)
  @ can be set onclick <code>javascript:read_dragdrop();</code> will return click numbers of mathml-objects; if 4 clickable object are drawn, the reply could be 1,0,1,0 ... meaning clicked on the first and third object
  @ can be set draggable: <code>javascript:read_dragdrop()</code> will return all coordinates in same order as the canvas script: unmoved objects will have their original coordinates...
  @ snaptogrid is supported...snaptopoints will work, but use with care... due to the primitive dragging. Technically: the dragstuff library is not used... the mathml is embedded in a new div element and not in the html5-canvas.
  @ when clicked, the mathml object will be drawn in red color; the div background color will be determined by the <a href="#fillcolor">fillcolor</a> and <a href="#opacity">opacity</a> settings.
  @ userdraw may be combined with 'mathml' ; the read_canvas() will contain the drawing.
  @ draggable or onclick 'external images' from command <a href='#copyresized'>copy or copyresized</a> can be combined with drag and/or onclick mathml
  @ other drag objects (circles/rects etc) are supported, but read_dragdrop() will probably be difficult to interpret...
  @ if inputfields are incorporated in mathml (with id's: id='mathml0',id='mathml1',...id='mathml_n')<br>the user_input values will be read by javascript:read_mathml();<br><b>attention</b>: if after this mathml-input object other user-interactions are included, these will read mathml too using "read_canvas();"
  @ If other inputfields (command input / command textarea) or userdraw is performed, the function read_canvas() will not read mathml. Use some generic function to read it....
  @ use keyword <a href='#centered'>centered</a> to center the mathml/xml object on (x1:y1)
  @%mathml_onclick%size 400,400%xrange -10,10%yrange -10,10%opacity 255,70%onclick%strokecolor red%fillcolor green%mathml -5,5,<span style="font-size:1em;"><math xmlns="http://www.w3.org/1998/Math/MathML" display="inline"><mstyle id="wims_mathml366290"><mrow><mo stretchy="true">[</mo><mtable rowspacing="0.5ex" columnalign=" left " columnlines=" none " rowlines=" none " ><mtr><mtd><mi>f</mi><mo stretchy="false">(</mo><mi>x</mi><mo stretchy="false">)</mo><mo>=</mo><mstyle displaystyle="true"><mfrac><mn>1</mn><mn>2</mn></mfrac></mstyle><mo>&sdot;</mo><msup><mi>x</mi> <mn>2</mn></msup></mtd></mtr> <mtr><mtd><mi>g</mi><mo stretchy="false">(</mo><mi>x</mi><mo stretchy="false">)</mo><mo>=</mo><msqrt><mstyle displaystyle="true"><mfrac><mn>1</mn><mrow><msup><mi>x</mi> <mn>2</mn></msup></mrow></mfrac></mstyle></msqrt></mtd></mtr></mtable><mo stretchy="true">]</mo></mrow></mstyle></math></span>%onclick%strokecolor blue%mathml 5,5,<span style="font-size:1em;"><math xmlns="http://www.w3.org/1998/Math/MathML" display="inline"><mstyle id="wims_mathml580175" ><mrow><mo stretchy="true">[</mo><mtable rowspacing="0.5ex" columnalign=" left " columnlines=" none " rowlines="none"><mtr><mtd><mi>f</mi><mo stretchy="false">(</mo><mi>x</mi><mo stretchy="false">)</mo><mo>=</mo><mstyle displaystyle="true"><mfrac><mn>1</mn><mrow><mi>sin</mi><mrow><mo stretchy="true">(</mo><msup><mi>x</mi> <mn>2</mn></msup><mo stretchy="true">)</mo></mrow></mrow></mfrac></mstyle></mtd></mtr> <mtr><mtd><mi>g</mi><mo stretchy="false">(</mo><mi>x</mi><mo stretchy="false">)</mo><mo>=</mo><msqrt><mrow><mi>sin</mi><mrow><mo stretchy="true">(</mo><msup><mi>x</mi> <mn>2</mn></msup><mo stretchy="true">)</mo></mrow></mrow></msqrt></mtd></mtr></mtable><mo stretchy="true">]</mo></mrow></mstyle></math></span>
  @%mathml_drag%size 400,400%xrange -10,10%yrange -10,10%drag xy%strokecolor red%mathml -5,5,<span style="font-size:1em;"><math xmlns="http://www.w3.org/1998/Math/MathML" display="inline"><mstyle id="wims_mathml366290"><mrow><mo stretchy="true">[</mo><mtable rowspacing="0.5ex" columnalign=" left " columnlines=" none " rowlines=" none "><mtr><mtd><mi>f</mi><mo stretchy="false">(</mo><mi>x</mi><mo stretchy="false">)</mo><mo>=</mo><mstyle displaystyle="true"><mfrac><mn>1</mn><mn>2</mn></mfrac></mstyle><mo>&sdot;</mo><msup><mi>x</mi> <mn>2</mn></msup></mtd></mtr> <mtr><mtd><mi>g</mi><mo stretchy="false">(</mo><mi>x</mi><mo stretchy="false">)</mo><mo>=</mo><msqrt><mstyle displaystyle="true"><mfrac><mn>1</mn><mrow><msup><mi>x</mi> <mn>2</mn></msup></mrow></mfrac></mstyle></msqrt></mtd></mtr></mtable><mo stretchy="true">]</mo></mrow></mstyle></math></span>%drag xy%strokecolor blue%mathml 5,5,<span style="font-size:1em;"><math xmlns="http://www.w3.org/1998/Math/MathML" display="inline"><mstyle id="wims_mathml580175" ><mrow><mo stretchy="true">[</mo><mtable rowspacing="0.5ex" columnalign=" left " columnlines=" none " rowlines=" none "><mtr><mtd><mi>f</mi><mo stretchy="false">(</mo><mi>x</mi><mo stretchy="false">)</mo><mo>=</mo><mstyle displaystyle="true"><mfrac><mn>1</mn><mrow><mi>sin</mi><mrow><mo stretchy="true">(</mo><msup><mi>x</mi> <mn>2</mn></msup><mo stretchy="true">)</mo></mrow></mrow></mfrac></mstyle></mtd></mtr> <mtr><mtd><mi>g</mi><mo stretchy="false">(</mo><mi>x</mi><mo stretchy="false">)</mo><mo>=</mo><msqrt><mrow><mi>sin</mi><mrow><mo stretchy="true">(</mo><msup><mi>x</mi> <mn>2</mn></msup><mo stretchy="true">)</mo></mrow></mrow></msqrt></mtd></mtr></mtable><mo stretchy="true">]</mo></mrow></mstyle></math></span>%#click left top corner...then drag...
  */
          js_function[DRAW_XML] = 1;
          for(i=0;i<3;i++){
            switch(i){
              case 0: double_data[0]=get_real(infile,0);break; /* x in x/y-range coord system -> pixel width */
              case 1: double_data[1]=get_real(infile,0);break; /* y in x/y-range coord system  -> pixel height */
              case 2: decimals = find_number_of_digits(precision);
                if(use_affine == TRUE ){ transform(2,2);}/* needs double_data[] */
                if( use_offset != 0 || drag_type != -1 ){int_data[2] = 1;}else{int_data[2] = 0;} /* only centered or not-centered */
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                int_data[0] = x2px(double_data[0]);/* needs px */
                int_data[1] = y2px(double_data[1]);
                if( use_slider != -1 && onclick == 0 ){ onclick = 3;}/* no drag&onclick but slideable */
                if( use_slider != -1 && drag_type != -1){ onclick = 5; }
                temp = get_string(infile,1);
                if( strstr(temp,"\"") != 0 ){ temp = str_replace(temp,"\"","'"); }
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "var draw_xml%d = {id:%d,type:'mathml',x:[%d],y:[%d],mathml:\"%s\",drag_type:%d,onclick:%d,object_cnt:%d,stroke_color:\"%s\",stroke_opacity:%.2f,fill_color:\"%s\",fill_opacity:%.2f,use_center:%d,use_snap:%d,angle:%f,fontfamily:\"%s\",transform:%s,use_affine:%d,offset:[0,0],use_slider:%d,rotation_center:%s,once:true};draw_xml(draw_xml%d);slidergroup[%d] = null;\n",drawxml_cnt,drawxml_cnt,int_data[0],int_data[1],temp,drag_type,onclick,object_cnt,stroke_color,stroke_opacity,fill_color,fill_opacity,int_data[2],use_snap,angle,font_family,doubledata2js_array(affine_matrix,6,decimals),use_affine,use_slider,rotation_center,drawxml_cnt,object_cnt));
                add_to_buffer(tmp_buffer);
                if(onclick != 0 ){object_cnt++;}
                drawxml_cnt++;/* keeps track on imported img,div,p,span,mathml,svg */
          /*
            in case inputs are present, trigger adding the read_mathml()
            if no other reply_format is defined
            note: all other reply types will include a reading of elements with id='mathml'+p)
          */
                if(strstr(temp,"mathml0") != NULL){ if(reply_format == 0 ){reply_format = 16;}} /* no other reply type is defined */
                break;
              default:break;
            }
          }
          reset();
          break;
        case MOEBIUS:
 /*
  @ moebius a,b,angle
  @ defines a Moebius transformation in Poincaré disk for subsequent objects of type hyperbolic as hypsegments, hyppolygon, hyptriangle,hyplines. In complex coordinates z -> exp(i \pi/180*theta)(z+m)/(conj(m)z+1) with m=a+ib of module <1, theta in degree.
  @ It make sense for objects in the disk of radius 1 and center 0,0 but no check is done.
  @ use keyword <a href='#killmoebius'>killmoebius</a> to end the transformation...
  @%moebius%size 400,400%xrange -1,1%yrange -1,1%hline 0,0, black%vline 0,0, black%circles black,0,0,1%hyplines blue,0.6,0,0.5,0.6,0,0,0.5,0.6%moebius 0.5,-0.2,0%hyplines skyblue,0.6,0,0.5,0.6,0,0,0.5,0.6%killmoebius%hyppolygon green,-0.6,0,-0.5,0.6,0,0,-0.5,-0.6%moebius -0.5,-0.2,90%hyppolygon%lightgreen,-0.6,0,-0.5,0.6,0,0,-0.5,-0.6%killmoebius%hyprays black,0,0,0.6,-0.4,0.5,-0.6,0,0,0.5,0.4%moebius -0.5,-0.2,120%hyprays grey,0,0,0.6,-0.4,0.5,-0.6,0,0,0.5,0.4%killmoebius%hyptriangle 0.6,0.3,0.5,0.4,0.7,0.5,purple%moebius 0.1,0.2,-90%hyptriangle 0.6,0.3,0.5,0.4,0.7,0.5,purple
*/
          for(i = 0 ; i<3;i++){
            switch(i){
              case 0: moebius_matrix[0] = get_real(infile,0);break;
              case 1: moebius_matrix[1] = get_real(infile,0);break;
              case 2: moebius_matrix[2] = get_real(infile,1);
              use_moebius = TRUE;
              break;
              default: break;
            }
          }
          break;
        case MOUSE:
  /*
  @ mouse color,fontsize
  @ will display the cursor (x:y) coordinates in ''color`` and ''fontsize`` using default fontfamily Arial
  @ note: use command ''mouse`` at the end of your script code (the same is true for command ''zoom``)
  @%mouse%size 400,400%xrange -10,10%yrange -10,10%mouse red,22
  */
          stroke_color = get_color(infile,0);
          font_size = (int) (get_real(infile,1));
          tmp_buffer = my_newmem(26);
          snprintf(tmp_buffer,26,"use_mouse_coordinates();\n");add_to_buffer(tmp_buffer);
          add_js_mouse(MOUSE_CANVAS,precision,stroke_color,font_size,stroke_opacity,2);
          js_function[INTERACTIVE] = 1;
          break;
        case MOUSE_DEGREE:
  /*
  @ mouse_degree color,fontsize
  @ will display the angle in degrees between x-axis, (0:0) and the cursor (x:y) in 'color' and 'font size'<br> using a fontfamily Arial
  @ The angle is positive in QI and QIII and the angle value is negative in QII and QIV
  @ note: use command 'mouse' at the end of your script code (the same is true for command 'zoom')
  @%mouse_degree%size 400,400%xrange -10,10%yrange -10,10%userdraw arc,blue%precision 100000%mouse_degree red,22
  */
          stroke_color = get_color(infile,0);
          font_size = (int) (get_real(infile,1));
          tmp_buffer = my_newmem(26);
          snprintf(tmp_buffer,26,"use_mouse_coordinates();\n");add_to_buffer(tmp_buffer);
          add_js_mouse(MOUSE_CANVAS,precision,stroke_color,font_size,stroke_opacity,3);
          js_function[JS_FIND_ANGLE] = 1;
          js_function[INTERACTIVE] = 1;
          break;
        case MOUSE_DISPLAY:
  /*
  @ display TYPE,color,fontsize
  @ TYPE may be x | y | xy | degree | radian | radius
  @ will display the mouse cursor coordinates as x-only,y-only,(x:y), the radius of a circle (this only in case 'userdraw circle(s),color') or the angle in degrees or radians for commands <code>userdraw arc,color</code> or protractor, ruler (if set dynamic).
  @ use commands ''xunit`` and / or ''yunit`` to add the units to the mouse values. The ''degree | radian`` will always have the appropriate symbol).
  @ just like commands ''mouse``, ''mousex``, ''mousey``, ''mouse_degree``... only other name
  @%display_x%size 400,400%xrange -10,10%yrange -10,10%xunit \\u212B%display x,red,22
  @%display_y%size 400,400%xrange -10,10%yrange -10,10%yunit seconds%display y,red,22
  @%display_xy%size 400,400%xrange -10,10%yrange -10,10%xunit centimetre%yunit seconds%display xy,red,22%userdraw segments,blue
  @%display_deg%size 400,400%xrange -10,10%yrange -10,10%display degree,red,22%fillcolor orange%opacity 200,50%userdraw arc,blue
  @%display_rad%size 400,400%xrange -10,10%yrange -10,10%display radian,red,22%fillcolor orange%opacity 200,50%userdraw arc,blue
  @%display_radius%size 400,400%xrange -10,10%yrange -10,10%xunit cm%xunit \\u212b%display radius,red,22%userdraw circle,blue
  */
          temp = get_string_argument(infile,0);
          if( strstr(temp,"xy") != NULL ){
            int_data[0] = 2;
          } else {
            if( strstr(temp,"y") != NULL ){
              int_data[0] = 1;
            }else{
              if( strstr(temp,"x") != NULL ){
                int_data[0] = 0;
              }else{
                if(strstr(temp,"degree") != NULL){
                int_data[0] = 3;
                js_function[JS_FIND_ANGLE] = 1;
                }else{
                  if(strstr(temp,"radian") != NULL){
                    int_data[0] = 4;
                    js_function[JS_FIND_ANGLE] = 1;
                  }else{
                    if(strstr(temp,"radius") != NULL){
                      int_data[0] = 5;
                    }else{
                      int_data[0] = 2;
                    }
                  }
                }
              }
            }
          }
          stroke_color = get_color(infile,0);
          font_size = (int) (get_real(infile,1));
          tmp_buffer = my_newmem(26);
          snprintf(tmp_buffer,26,"use_mouse_coordinates();\n");add_to_buffer(tmp_buffer);
          add_js_mouse(MOUSE_CANVAS,precision,stroke_color,font_size,stroke_opacity,int_data[0]);
          js_function[INTERACTIVE] = 1;
          break;
        case MOUSE_PRECISION:
  /*
  @ precision int
  @ 1 = no decimals ; 10 = 1 decimal ; 100 = 2 decimals etc
  @ may be used / changed before every object
  @ In case of user interaction (like ''userdraw`` or ''multidraw``), this value will be used to determine the amount of decimals in the reply / answer
  @%precision%size 400,400%xrange -10,10%yrange -10,10%precision 1%userdraw segment,red
  */
          precision = (int) (get_real(infile,1));
          if(precision < 1 ){precision = 1;};
          break;
        case MOUSEX:
  /*
  @ mousex color,fontsize
  @ will display the cursor x-coordinate in ''color`` and ''font size`` using the fontfamily Arial.
  @ note: use command ''mouse`` at the end of your script code (the same is true for command ''zoom``).
  */
          stroke_color = get_color(infile,0);
          font_size = (int) (get_real(infile,1));
          tmp_buffer = my_newmem(26);
          snprintf(tmp_buffer,26,"use_mouse_coordinates();\n");add_to_buffer(tmp_buffer);
          add_js_mouse(MOUSE_CANVAS,precision,stroke_color,font_size,stroke_opacity,0);
          js_function[INTERACTIVE] = 1;
          break;
        case MOUSEY:
  /*
  @ mousey color,fontsize
  @ will display the cursor y-coordinate in ''color`` and ''font size`` using default fontfamily Arial.
  @ note: use command ''mouse`` at the end of your script code (the same is true for command ''zoom``).

  */
          stroke_color = get_color(infile,0);
          font_size = (int) (get_real(infile,1));
          tmp_buffer = my_newmem(26);
          snprintf(tmp_buffer,26,"use_mouse_coordinates();\n");add_to_buffer(tmp_buffer);
          add_js_mouse(MOUSE_CANVAS,precision,stroke_color,font_size,stroke_opacity,1);
          js_function[INTERACTIVE] = 1;
          break;
        case MULTIDASH:
  /*
  @ multidash 0,1,1
  @ meaning draw objects no. 2 (circle) and 3 (segments), in the list of command like <code>multifill points,circle,segments</code>, are dashed
  @ use before command <a href='#multidraw'>multidraw</a>
  @ if not set all objects will be set ''not dashed``... unless a generic keyword ''dashed`` was given before command ''multidraw``
  @ the dash-type is not -yet- adjustable <br>(e.g. command <code>dashtype line_px,space_px</code> will give no control over multidraw objects)
  @ wims will <b>not</b> check if the number of 0 or 1's matches the amount of draw primitives...
  @ always use the same sequence as is used for ''multidraw``
  */
          if( use_tooltip == 1 ){canvas_error("command 'multidraw' is incompatible with command 'intooltip tip_text'");}
          temp = get_string(infile,1);
          temp = str_replace(temp,",","\",\"");
          fprintf(js_include_file,"var multidash = [\"%s\"];",temp);
          reset();/* if command 'dashed' was given...reset to not-dashed */
          break;
        case MULTIDRAW:
  /*
  @ multidraw obj_type_1,obj_type_2...obj_type_11
  @ for simple single object user drawings you could also use command <a href="#userdraw">userdraw</a>
  @ implemented obj_types:<ul><li>point | points</li><li>circle | circles</li><li>line | lines</li><li>segment | segments</li><li>arrow | arrows (use command 'arrowhead int' for size (default value 8 pixels))</li><li>curvedarrow | curvedarrows</li><li>rect | rects</li><li>closedpoly<br><b>only one</b> closedpolygon may be drawn.The number of ''corner points`` is not preset (e.g. not limited, freestyle), the polygon is closed when clicking on the first point again..(+/- 10px)</li><li>triangle | triangles</li><li>parallelogram | parallelograms</li><li>poly[3-9] | polys[3-9] draw 3...9 point polygone(s): polys3 is of course triangles</li><li>images</li><li>crosshair | crosshairs</li><li>function <br>for more function user input fields, use it multiple times<br>for 4 inputfields use : multidraw function,function,function,function</li></ul>
  @ additionally objects may be user labelled, using obj_type ''text``...<br>in this case allways a text input field and if <a href='#multiuserinput'> multiuserinput=1 </a> also (x:y) inputfields will be added to the page.<br>use commands ''fontfamily`` and ''fontcolor`` to adjust (command ''multistrokeopacity`` may be set to adjust text opacity)<br>note: text is always centered on the mouse-click or user-input coordinates !<br>note: no keyboard listeners are used
  @ it makes no sense using something like ''multidraw point,points`` ... <br>something like "multidraw polys4,polys7" will only result in drawing a ''4 point polygone`` and not a ''7 point polygone``: this is a design flaw and not a feature...
  @ note: mouselisteners are only active if "&#36;status != done " (eg only drawing in an active/non-finished exercise) <br> to overrule use command/keyword "status" (no arguments required)
  @ buttons for changing the obj_type (and in case of ''multiuserinput``, some inputfields and buttons) <br>will be present in the reserved div ''tooltip_div`` and can be styled using command 'css some_css'
  @ the button label will be default the ''object primitive name`` (like ''point``, ''circles``).<br>If you want a different label (e.g. an other language), use command ''multilabel``<br>for example in dutch: <br><code>multilabel cirkel,lijnstuk,punten,STOP<br>multidraw circle,segment,points</code><br>(see command <a href='#multilabel'>multilabel</a> for more details)
  @ a right mouse button click will remove the last drawn object of the selected drawing type. All other type of objects are not removed
  @ multidraw is incompatible with command ''tooltip`` (the reserved div_area is used for the multidraw control buttons)
  @ all ''multidraw`` drawings will scale on zooming.<br>this in contrast to the command <a href="#userdraw">userdraw</a>.
  @ wims will <b>not</b> check the amount or validity of your command arguments ! <br>( use javascript console to debug any typo's )
  @ a local function <code>read_canvas%d</code> will read all userbased drawings.<br>The output is always a 16 lines string with fixed sequence.<br>line 1 = points_x+";"+points_y+"\\n"<br>line 2 = circles_x+";"+circles_y+";"+multi_radius+"\\n"<br>line 3 = segments_x+";"+segments_y+"\\n"<br>line 4 = arrows_x+";"+arrows_y+"\\n"<br>line 5 = lines_x+";"+lines_y+"\\n"<br>line 6 = triangles_x+";"+triangles_y+"\\n"<br>line 7 = polys[3-9]_x+";"+polys[3-9]_y+"\\n"<br>line 8 = rects_x +";"+rects_y+"\\n"<br>line 9 = closedpoly_x+";"+closedpoly_y+"\\n"<br>line 10 = parallelogram_x+";"+parallelogram_y"\\n"<br>line 11 = text_x+";"+text_y+";"+text"\\n"<br>line 12 = image_x+";"+image_y+";"+image_id<br>line 13 = curvedarrows_x +";"+ curvedarrows_y +"\\n"<br>line 14 = curvedarrows2_x +";"+ curvedarrows2_y +"\\n"<br>line 15 = crosshairs_x +";"+ crosshairs_y +"\\n"<br>line 16 = userdraw_x +";"+userdraw_y + "\\n" note: this is for single ''userdraw object,color`` and ''replyformat 29``<br>line 17 = userdraw_x +";"+userdraw_y +";"+userdraw_radius + "\\n" note: this is for single ''userdraw object,color`` and ''replyformat 29``<br>The x/y-data are in x/y-coordinate system and display precision may be set by a previous command ''precision 0 | 10 | 100 | 1000...``<br>In case of circles the radius is -for the time being- rounded to pixels<br><b>use the wims "direct exec" tool to see the format of the reply</b>
  @ It is best to prepare / format the student reply in clientside javascript.<br>However in ''wims`` language you could use something like this<br>for example you are interested in the polys5 drawings of a pupil (the pupil may draw multiple poly5 objects...)<br>note: the reply for 2 poly5's is: x11,x12,x13,x14,x15,x21,x22,x23,x24,x25 ; y11,y12,y13,y14,y15,y21,y22,y23,y24,y25<br>rep = !line 7 of reply <br>rep = !translate ';' to '\\n' in &#36;rep <br>pts = 5 # 5 points for polygon <br>x_rep = !line 1 of &#36;rep <br>y_rep = !line 2 of &#36;rep <br>tot = !itemcnt &#36;x_rep <br>num_poly = &#36;[&#36;tot/&#36;pts] <br>idx = 0 <br>!for p=1 to &#36;num_poly <br>&nbsp;!for s=1 to &#36;pts <br>&nbsp;&nbsp;!increase idx <br>&nbsp;&nbsp;X = !item &#36;idx of &#36;x_rep <br>&nbsp;&nbsp;Y = !item &#36;idx of &#36;y_rep <br>&nbsp;&nbsp;# do some checking <br>&nbsp;!next s <br>!next p <br>
  @ <b>attention</b>: for command argument ''closedpoly``, only one polygone can be drawn. The last point (e.g. the point clicked near the first point) of the array is removed.
  @ technical: all 10 ''draw primitives`` + ''text`` will have their own -transparent- PNG bitmap canvas. <br>So for example there can be a points_canvas entirely separated from a line_canvas.<br>This to avoid the need for a complete redraw when something is drawn to the canvas...(eg only the object_type_canvas is redrawn), this in contrast too many very slow do-it-all HTML5 canvas javascript libraries.<br>The mouselisteners are attached to the canvas-div element.
  @ a special object type is ''images``.<br>if used together with <a href='#imagepalette'>imagepalette</a> a image table will be integrated in the 'control section' of multidraw (set <code>multiuserinput 1</code> for ''images``) if not used with <a href='#imagepalette'>imagepalette</a>, provide the images or div's (&lt;img&gt; tag with bitmap or SVG or anything in a div element) somewhere on the html exercise page, with an onclick handler like:<br><code>&lt;img src='gifs/images/dog.svg' onclick='javascript:place_image_on_canvas(this.id);' id="ext_image_1" /&gt;<br>&lt;img src='gifs/fish.png' onclick='javascript:place_image_on_canvas(this.id);' id="another" /&gt;</code><br>etc ... when activating the multidraw ''image`` button, the images can be selected<br> (left mouse button/onclick) and placed on the canvas...left mouse click.<br>using div's will enable you -amongst other content- to add math typesetting from the exercise page onto the canvas.
  @ When you are not content with the default ''multidraw control panel``, you can create your own interface, using a few javascript functions to call the drawprimitives, delete things and ''stop drawing`` in case you also want to drag&drop stuff...</br>To activate this feature, use <a href='#multilabel'>multilabel NOCONTROLS</a><br>The object types are internally represented by the following numbers (making typos will render your exercise null and void)<br>point = 0<br>points =1<br>circle = 2<br>circles = 3<br>line = 4<br>lines = 5<br>segment = 6<br>segments = 7<br>arrow = 8<br>arrows = 9<br>triangle = 10<br>triangles = 11<br>closedspoly = 12<br>text = 13<br>rect = 14<br>rects = 15<br>poly[3-9] = 16<br>polys[3-9] = 17<br>parallelogram = 18<br>parallelograms = 19<br>images  = 20<br>curvedarrow = 21<br>curvedarrows = 22<br>curvedarrow2 = 23<br>curvedarrows2 = 24<br>crosshair = 25<br>crosshairs = 26 <br>controls for example:<br><code>&lt;input type='button' onclick='javascript:userdraw_primitive=null' value='STOP DRAWING' /&gt;<br>&lt;input type='button' onclick='javascript:userdraw_primitive=24;multidraw_object_cnt = 0;' value='start drawing curvedarrows2' /&gt; <br>&lt;input type='button' onclick='javascript:var fun=eval("clear_draw_area"+canvas_scripts[0]);fun(24,0);' value='REMOVE LAST CURVEDARROW ' /&gt; </code><br> If using multiple canvas scripts in a single page, loop through the canvas_scripts[n] <br>note: if using NOCONTROLS and just a single draw primitive (for example, just: 'multidraw circles'), the object may be drawn directly. (analogue to 'userdraw circles,color')<br>And since a right mouse button click will always remove the last drawn object of the current object type, there is no need for a special "remove button"
  @%multidraw_function%size 400,400%xrange -10,10%yrange -10,10%fontfamily Italic 22px Helvetica%axis%axisnumbering%precision 0%grid 2,2,grey,1,1,5,black%multicolors red,green,blue%fontcolor orange%multilinewidth 1,2,3%multidraw text,function,function,function,line
  @%multidraw%size 400,400%xrange -10,10%yrange -10,10%multidash 1,0%multilinewidth 1,2%multistrokecolors red,blue%multisnaptogrid 1,1%multilabel LINES,CIRCLES,STOP DRAWING%multidraw lines,circles
  @%multidraw_images%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%drag xy%# use special function to read the drag coordinates%copy 0,0,-1,-1,-1,-1,gifs/images/skull_and_crossbones50.png%fontcolor green%fontfamily Bold 42pt Arial%imagepalette gifs/ca.gif,gifs/en.gif,gifs/nl.gif,gifs/fr.gif,gifs/cn.gif,gifs/de.gif,gifs/kh.gif,gifs/it.gif%multiuserinput 0,0,1%css color:blue;%multisnaptogrid 1,1,1%multilinewidth 0,4,0%# attention: use unicode text input without the slash %# \u222D ---> u222D at least will sometimes work%# otherwise cut&past unicode symbols into inputfield...%multilabel TEXT,REACTION ARROW,FLAGS,STOP DRAWING%multidraw text,arrow,images
  @%multidraw_demo%size 800,800%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%css color:blue;%fontfamily Italic 42pt Arial%precision 1%opacity 200,50%snaptogrid%linewidth 3%filled%multistrokecolors red,green,blue,orange,yellow,purple,black,cyan,red,green,blue,orange,green,purple,black,cyan%multifillcolors red,green,blue,orange,yellow,purple,black,cyan,red,green,blue,orange,brown,purple%imagepalette gifs/ca.gif,gifs/en.gif,gifs/nl.gif,gifs/fr.gif,gifs/cn.gif,gifs/de.gif,gifs/kh.gif,gifs/it.gif%multidraw closedpoly,segments,rect,parallelogram,triangles,poly5,points,lines,arrows,circles,text,curvedarrows,curvedarrows2,images
  @%multidraw_NOCONTROLS%size 400,400%xrange -10,10%yrange -10,10%grid 2,2,grey%linewidth 3%strokecolor green%fillcolor blue%filled%opacity 255,60%multilabel NOCONTROLS%multidraw circles%# RIGHT MOUSE CLICK REMOVES LAST OBJECT
  */
          js_function[INTERACTIVE] = 1;
          if(js_function[JS_ZOOM] == 1){use_zoom = 1;} /* use noisy zoom_code, when command zoom is given before command 'multidraw' */
          if( use_tooltip == 1 ){canvas_error("command 'multidraw' is incompatible with command 'intooltip tip_text'");}
          if( use_userdraw == 1 ){canvas_error("Only one userdraw primitive may be used in command 'userdraw' use command 'multidraw' for this...");}
          use_userdraw = 2;
          temp = get_string(infile,1);
          fprintf(js_include_file,"\
            var MMP = %d;\
            if( typeof(multisnaptogrid) == 'undefined' && multisnaptogrid == null){var multisnaptogrid = new Array(MMP);for(var i=0;i<MMP;i++){multisnaptogrid[i] = %d;};};\
            if( typeof(multistrokecolors) === 'undefined' && multistrokecolors == null){ var multistrokecolors = new Array(MMP);for(var i=0;i<MMP;i++){multistrokecolors[i] = '%s';};};\
            if( typeof(multifillcolors) === 'undefined' && multifillcolors == null ){var multifillcolors = new Array(MMP);for(var i=0;i<MMP;i++){multifillcolors[i] = '%s';};};\
            if( typeof(multistrokeopacity) === 'undefined' && multistrokeopacity == null){var multistrokeopacity = new Array(MMP);for(var i=0;i<MMP;i++){multistrokeopacity[i] = %.2f;};};\
            if( typeof(multifillopacity) === 'undefined' &&  multifillopacity == null){var multifillopacity = new Array(MMP);for(var i=0;i<MMP;i++){multifillopacity[i] = %.2f;};};\
            if( typeof(multilinewidth) === 'undefined' && multilinewidth == null){var multilinewidth = new Array(MMP);for(var i=0;i<MMP;i++){multilinewidth[i] = %d;};};\
            if( typeof(multifill) === 'undefined' && multifill == null){var multifill = new Array(MMP);for(var i=0;i<MMP;i++){multifill[i] = %d;};};\
            if( typeof(multidash) === 'undefined' && multidash == null){var multidash = new Array(MMP);for(var i=0;i<MMP;i++){multidash[i] = %d;};};\
            if( typeof(multilabel) === 'undefined' && multilabel == null){var multilabel = [\"%s\",\"stop drawing\"];};\
            if( typeof(multiuserinput) === 'undefined' && multiuserinput == null){var multiuserinput = new Array(MMP);for(var i=0;i<MMP;i++){multiuserinput[i] = '0';};};\
            var arrow_head = %d;var multifont_color = '%s';var multifont_family = '%s';var forbidden_zone = [xsize+2,ysize+2];",
            MAX_MULTI_PRIMITIVES,
            use_snap,
            stroke_color,
            fill_color,
            stroke_opacity,
            fill_opacity,
            line_width,
            use_filled,
            use_dashed,
            str_replace(temp,",","\",\""),
            arrow_head,font_color,font_family);
          add_js_multidraw(temp,css_class,use_offset,int_data[MAX_MULTI_PRIMITIVES+1],crosshair_size,use_zoom);
            /* no_controls == int_data[MAX_MULTI_PRIMITIVES+1] */
          reply_precision = precision;
          if(strstr(temp,"text") != NULL){
            js_function[JS_SAFE_EVAL] = 1;
            js_function[DRAW_SUBSUP] = 1;
          }
          if(strstr(temp,"function") != NULL){
            int pp, funs = count_substring(temp, "function");
            js_function[JS_SAFE_EVAL] = 1;
            js_function[JS_RAWMATH] = 1;
            js_function[DRAW_JSFUNCTION] = 1;
            js_function[JS_MATH] = 1;
            js_function[JS_PLOT] = 1;
            for(pp = 0 ; pp< funs ;pp++){
              add_input_jsfunction(css_class,function_label,input_cnt,stroke_color,stroke_opacity,line_width,use_dashed,dashtype[0],dashtype[1],font_size);
              input_cnt++;
              jsplot_cnt++;
            }
            fprintf(js_include_file,"if(typeof(all_jsplots) !== 'number'){var all_jsplots;};all_jsplots = %d;function redraw_userdraw(){redraw_jsplot();return;};",jsplot_cnt);
          }
      /* the canvasses range from 1000 ... 1008 */
          if( reply_format == 0){reply_format = 29;}
          reset();/* if command 'filled' / 'dashed' was given...reset all */
          break;
        case MULTILABEL:
  /*
  @ multilabel button_label_1,button_label_2,...,button_label_8,'stop drawing text'
  @ use before command <a href='#multidraw'>multidraw</a>
  @ if not set all labels (e.g. the value of input type 'button') will be set by the english names for the draw_primitives (like 'point','circle'...)
  @ the ''stop drawing`` button text <b>must</b> be the last item on the ''multilabel`` -list <br>for example:<br><code>multilabel punten,lijnen,Stop met Tekenen<br>multidraw points,lines</code>
  @ all buttons can be ''styled`` by using command <code>css</code><br><b>note:</b>If you want to add some CSS style to the buttons...<br>the id's of the ''draw buttons`` are their english command argument<br>(e.g. id="canvasdraw_points" for the draw points button).<br>the id of the ''stop drawing`` button is "canvasdraw_stop_drawing".<br>the id of the "OK" button is ''canvasdraw_ok_button``
  @ wims will not check the amount or validity of your input
  @ always use the same sequence as is used for ''multidraw``
  @ if you don't want the controls, and want to write your own interface, set <code>multilabel NOCONTROLS</code>
  */
          if( use_tooltip == 1 ){canvas_error("command 'multidraw' is incompatible with command 'intooltip tip_text'");}
          temp = get_string(infile,1);
          if( strcasestr(temp,"NOCONTROLS") ){
            int_data[MAX_MULTI_PRIMITIVES+1] = 1; /*int_data[25] = 1 --> 'no_controls = 1' in canvasmacro.c */
            fprintf(js_include_file,"var multilabel = null;");
          }
          else {
            int_data[MAX_MULTI_PRIMITIVES+1] = 0; /* so DO use control buttons etc */
            temp = str_replace(temp,",","\",\"");
            fprintf(js_include_file,"var multilabel = [\"%s\"];",temp);
          }
          break;
        case MULTILINEWIDTH:
  /*
  @ multilinewidth linewidth_1,linewidth_2,...,linewidth_8
  @ use before command <a href='#multidraw'>multidraw</a>
  @ if not set all line widths will be set by a previous command ''linewidth int``
  @ use these up to 7 different line widths for the draw primitives used by command <code>multidraw obj_type_1,obj_type_2...obj_type_7</code>
  @ wims will <b>not</b> check if the number of 0 or 1's matches the amount of draw primitives...
  @ always use the same sequence as is used for ''multidraw``
  */
          if( use_tooltip == 1 ){canvas_error("command 'multidraw' is incompatible with command 'intooltip tip_text'");}
          temp = get_string(infile,1);
          temp = str_replace(temp,",","\",\"");
          fprintf(js_include_file,"var multilinewidth = [\"%s\"];",temp);
          break;
        case MULTIFILL:
  /*
  @ multifill 0,0,1,0,1,0,0
  @ meaning draw objects no. 3 and 5, in the list of command ''multifill``, are filled (if the object is fillable...and not a line,segment,arrow or point...)
  @ using a fillpattern: multifill 0,1,2,5,3,4<br>meaning: first object is not filled...second object is solid color filled...2=grid | 3=hatch | 4=diamond | 5=dot
  @ use before command <a href='#multidraw'>multidraw</a>
  @ if not set all objects -except point|points- will be set ''not filled``... unless a command <code>filled</code> was given before command <code>multifill</code>
  @ only suitable for draw_primitives like ''circle | circles``, ''triangle | triangles``, ''rect | rects``, ''poly[3-9] | polys[3-9]`` and ''polygon``
  @ wims will <b>not</b> check if the number of 0 or 1's matches the amount of draw primitives...
  @ always use the same sequence as is used for ''multidraw``
  */
          if( use_tooltip == 1 ){canvas_error("command 'multidraw' is incompatible with command 'intooltip tip_text'");}
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_MULTI_PRIMITIVES){canvas_error("too many multidraw primitives...read the documentation...");}
            int_data[i] = (int)(get_real(infile,1)); /* 0,1,2,3,4,5 */
            if(int_data[i] < 0 || int_data[i] > 5 ){canvas_error("the only possible multifill arguments are: 0,1,2,3,4,5 ");}
            if(int_data[i] > 1 ){use_filled = 2;js_function[DRAW_FILL_PATTERN] = 1;}/* switch to trigger pattern filling */
            i++;
          }
          fprintf(js_include_file,"var multifill = %s ;",data2js_array(int_data,i-1));
          break;
        case MULTIFILLCOLORS:
  /*
  @ multifillcolors color_name_1,color_name_2,...,color_name_8
  @ use before command <a href='#multidraw'>multidraw</a>
  @ if not set all fillcolors (for circle | triangle | poly[3-9] | closedpoly ) will be ''stroke_color``, ''fill_opacity``
  @ use these up to 6 colors for the draw primitives used by command <code>multidraw obj_type_1,obj_type_2...obj_type_n</code>
  @ wims will <b>not</b> check if the number of colors matches the amount of draw primitives...
  @ always use the same sequence as is used for ''multidraw``
  @ can also be used with command <a href='#userdraw'>userdraw clickfill,color</a> when more than one fillcolor is wanted.<br>in that case use for example <a href='#replyformat'>replyformat 10</a> ... reply=x1:y1:color1,x2:y2:color2...<br>the colors will restart at the first color, when there are more fill-clicks than multi-fill-colors<br>if more control over the used colors is wanted, see command <a href='#colorpalette'>colorpalette color1,color2...</a>
  */
          if( use_tooltip == 1 ){canvas_error("command 'multidraw' is incompatible with command 'intooltip tip_text'");}
          fprintf(js_include_file,"var multifillcolors = [");
          while( ! done ){
            temp = get_color(infile,1);
            fprintf(js_include_file,"\"%s\",",temp);
          }
          fprintf(js_include_file,"\"0,0,0\"];");/* add black to avoid trouble with dangling comma... */
          break;
        case MULTIFILLOPACITY:
  /*
  @ multifillopacity fill_opacity_1,fill_opacity_2,...,fill_opacity_8
  @ float values 0 - 1 or integer values 0 - 255
  @ use before command <a href='#multidraw'>multidraw</a>
  @ if not set all fill opacity_ will be set by previous command <code>opacity int,int</code> and keyword ''filled``
  @ use these up to 7 different stroke opacities for the draw primitives used by command <code>multidraw obj_type_1,obj_type_2...obj_type_y</code>
  @ wims will not check the amount or validity of your input
  @ always use the same sequence as is used for ''multidraw``
  */
          if( use_tooltip == 1 ){canvas_error("command 'multidraw' is incompatible with command 'intooltip tip_text'");}
          temp = get_string(infile,1);
          temp = str_replace(temp,",","\",\"");
          fprintf(js_include_file,"var multifillopacity = [\"%s\"];",temp);
          break;
        case MULTISNAPTOGRID:
  /*
  @ multisnaptogrid 0,1,1
  @ alternative: multisnap
  @ meaning draw objects no. 2 (circle) and 3 (segments), in the list of command like <code>multifill points,circle,segments</code>, will snap to the xy-grid (default 1 in x/y-coordinate system: see command <a href='#snaptogrid'>snaptogrid</a>)
  @ freehand drawing...specify precision for reply: all objects snap to grid <code>multisnaptogrid 1,1,1,...</code>
  @ only the xy-values snap_to_grid: all objects snap to grid <code>multisnaptogrid 1,1,1,...</code>
  @ only the x-values snap_to_grid: all objects snap to x-grid <code>multisnaptogrid 2,2,2,...</code>
  @ only the y-values snap_to_grid: all objects snap to y-grid <code>multisnaptogrid 3,3,3,...</code>
  @ if <a href='#snaptopoints'>snaptopoints</a> is defined: all objects snap to points <code>multisnaptogrid 4,4,4,...</code> <br><b>make sure to define the points to snap on...</b> use command <a href='#snaptopoints'>snaptopoints</a>
  @ <code>multisnaptogrid 0,1,2,3,4<br>multidraw text,arrow,line,circle,image</code><br>''text`` is free hand, ''arrow`` is snap to grid, ''line`` is snap to x-grid, ''circle`` is snap to y-grid, ''image`` is snap to points defined by command <a href='#snaptopoints'>snaptopoints</a>
  @ use before command <a href='#multidraw'>multidraw</a>
  @ attention: if not set all objects will be set ''no snap``... unless a generic command ''snaptogrid`` was given before command ''multidraw``
  @ commands <a href='#xsnaptogrid'>xsnaptogrid</a>, <a href='#ysnaptogrid'>ysnaptogrid</a>, <a href='#snaptofunction'>snaptofunction</a> are <b>not</b> supported amd only functional for command <a href='#userdraw'>userdraw</a>
  @ always use the same sequence as is used for ''multidraw``
  @ wims will <b>not</b> check if the number of 0 or 1's matches the amount of draw primitives...
  */
          if( use_tooltip == 1 ){canvas_error("command 'multidraw' is incompatible with command 'intooltip tip_text'");}
          temp = get_string(infile,1);
          fprintf(js_include_file,"var multisnaptogrid = [%s];",temp);
          reset();/* if command 'dashed' was given...reset to not-dashed */
          break;
        case MULTISTROKECOLORS:
  /*
  @ multistrokecolors color_name_1,color_name_2,...,color_name_8
  @ use before command <a href='#multidraw'>multidraw</a>
  @ if not set all colors will be ''stroke_color``, ''stroke_opacity``
  @ use these up to 6 colors for the draw primitives used by command <code>multidraw obj_type_1,obj_type_2...obj_type_7</code>
  @ wims will <b>not</b> check if the number of colors matches the amount of draw primitives...
  @ always use the same sequence as is used for ''multidraw``
  */
          if( use_tooltip == 1 ){canvas_error("command 'multidraw' is incompatible with command 'intooltip tip_text'");}
          fprintf(js_include_file,"var multistrokecolors = [");
          while( ! done ){
            temp = get_color(infile,1);
            fprintf(js_include_file,"\"%s\",",temp);
          }
          fprintf(js_include_file,"\"0,0,0\"];");/* add black to avoid trouble with dangling comma... */
          break;
        case MULTISTROKEOPACITY:
  /*
  @ multistrokeopacity stroke_opacity_1,stroke_opacity_2,...,stroke_opacity_7
  @ float values 0 - 1 or integer values 0 - 255
  @ use before command <a href='#multidraw'>multidraw</a>
  @ if not set all stroke opacity_ will be set by previous command <code>opacity int,int</code>
  @ use these up to 7 different stroke opacities for the draw primitives used by command <code>multidraw obj_type_1,obj_type_2...obj_type_7</code>
  @ wims will not check the amount or validity of your input
  @ always use the same sequence as is used for ''multidraw``
  */
          if( use_tooltip == 1){canvas_error("command 'multidraw' is incompatible with command 'intooltip tip_text'");}
          temp = get_string(infile,1);
          temp = str_replace(temp,",","\",\"");
          fprintf(js_include_file,"var multistrokeopacity = [\"%s\"];",temp);
          break;
        case MULTIUSERINPUT:
  /*
  @ multiuserinput 0,1,1,0
  @ alternative: multiinput
  @ meaning, when the command ''multidraw`` is used <code>multidraw circles,points,lines,triangles</code><br>objects ''points`` and ''lines`` may additionally be ''drawn`` by direct input (inputfields)<br>all other objects must be drawn with a mouse
  @ in case of circle | circles a third inputfield for Radius (R) is added. The radius must be in the x/y coordinate system (x-range) and <b>not</b> in pixels...students don't think in pixels.<br>note: R-values will not snap-to-grid
  @ in case of line(s) | segment(s) | arrow(s) the user should write <b>x1:y1</b> in the first inputfield and <b>x2:y2</b> in the second.<br>These ''hints`` are pre-filled into the input field.<br>Other coordinate delimiters are '';`` and '',`` e.g. <b>x1;y1</b> or <b>x1,y1</b>.<br>An error message (alert box) will popup when things are not correctly...
  @ in case of a triangle | poly3, three inputfields are provided.
  @ in case of ''text`` and ''multiuserinput=1, 3`` inputfields will be shown: ''x,y,text``
  @ in case of ''text`` and ''multiuserinput=0, 1`` inputfield will be shown: text ... a mouse click will place the text on the canvas.
  @ may be styled using command <a href="#css">css</a>
  @ an additional button ''stop drawing`` may be used to combine userbased drawings with ''drag&and;drop`` or ''onclick`` elements
  @ when exercise if finished (status=done) the buttons will not be shown.<br>To override this default behaviour use command / keyword ''status``
  @ use before command <a href='#multidraw'>multidraw</a>
  @ always use the same sequence as is used for ''multidraw``
  */
      /* simple rawmath and input check */
          js_function[JS_SAFE_EVAL] = 1;
          temp = get_string(infile,1);
          temp = str_replace(temp,",","\",\"");
          fprintf(js_include_file,"var multiuserinput = [\"%s\"];",temp);
          break;
        case NORESET:
  /*
  @ noreset
  @ alternative: killreset
  @ keyword
  @ may come in handy if canvas script code is generated using loops
  @ if used the following properties will remain to be valid<br><ul><li>filled</li><li>dash settings</li><li>onclick or drag settings</li><li>centering or offset</li></ul>
  @ if used again, these properies will be reset to the default values and normal behaviour is continued (e.g. the above properties will be reset after 'use' on a canvas object)
  @ etc etc
  @ commands <a href='#slider'>slider</a>, <a href='#linear'>linear</a>, <a href='#rotate'>rotate</a>, <a href='#translate'>translate</a>, <a href='#affine'>affine</a> are always active until the 'kill' commands are given: <br><a href='#killlinear'>killlinear</a>, <a href='#killrotate'>killrotate</a>, <a href='#killtranslate'>killtranslate</a> and <a href='#killaffine'>killaffine</a>
  @ commands like 'opacity', 'linewidth', 'fontsize', 'fontfamily' are only changed when redefined again
  @%noreset%size 400,400%xrange -10,10%yrange -10,10%noreset%drag xy%linewidth 4%filled%rect -4,4,-2,2,blue%triangle -3,-3,0,0,1,-5,green%circle 0,0,50,orange%noreset%rect 2,4,4,2,blue
  @%noreset_oefgraduation%size 800,100%xrange 0,11%yrange -1,1%hline 0,0,black%parallel 0,0,0,0.5,1,0,11,blue%noreset%onclick%centered%text black,1,0.6,large,20%text black,2,0.6,large,30%text black,3,0.6,large,40%text black,4,0.6,large,50%text black,5,0.6,large,60%text black,6,0.6,large,70%text black,7,0.6,large,80%text black,8,0.6,large,90%text black,9,0.6,large,100%text black,10,0.6,large,110
  */
          if(no_reset == FALSE){no_reset = TRUE;}else{no_reset = FALSE;reset();}
          break;
        case NOXAXIS:
  /*
  @ noxaxis
  @ keyword
  @ if set, the automatic x-axis numbering will be ignored
  @ use command <a href="#axis">axis</a> to have a visual x/y-axis lines (see command <a href="#grid">grid</a>)
  @ to be used before command grid (see <a href="#grid">command grid</a>)
  */
          fprintf(js_include_file,"x_strings = {};x_strings_up = [];\n");
          use_axis_numbering = -1;
          break;
        case NOYAXIS:
  /*
  @ noyaxis
  @ keyword
  @ if set, the automatic y-axis numbering will be ignored
  @ use command <a href="#axis">axis</a> to have a visual x/y-axis lines (see command <a href="#grid">grid</a>)
  @ to be used before command grid (see <a href="#grid">command grid</a>)
  */
          fprintf(js_include_file,"y_strings = {};\n");
          break;
        case NUMBERLINE:
  /*
  @ numberline x0,x1,xmajor,xminor,y0,y1
  @ numberline is using xrange/yrange system for all dimensions
  @ multiple numberlines are allowed ; combinations with command <a href='#grid'>grid</a> is allowed; multiple commands <a href='#xaxis'>xaxis numbering</a> are allowed
  @ x0 is start x-value in xrange
  @ x1 is end x-value in xrange
  @ xmajor is step for major division
  @ xminor is divisor of xmajor; using small (30% of major tick) tick marks: this behaviour is ''hardcoded``
  @ is xminor is an even divisor, an extra tickmark (60% of major tick) is added to the numberline: this behaviour is ''hardcoded``
  @ y0 is bottom of numberline; y1 endpoint of major tics
  @ use command <a href="#linewidth">linewidth</a> to control appearance
  @ use <a href="#strokecolor">strokecolor</a> and <a href="#opacity">opacity</a> to controle measure line
  @ for all ticks linewidth and color / opacity are identical.
  @ if command <a href="#xaxis">xaxis</a> or <a href="#xaxisup">xaxisup</a> is not defined, the labeling will be on major ticks: x0...x1
  @ use <a href="#fontfamily">fontfamily</a> and <a href="#fontcolor">fontcolor</a> to control fonts settings
  @ may be used together with <a href="#userdraw">userdraw</a>, <a href="#multidraw">multidraw</a> and <a href="#drag">user drag</a> command family for the extra object drawn onto the numberline
  @ <a href="#snaptogrid">snaptogrid, snaptopoints etc</a> and <a href="#zoom">zooming and panning</a> is supported
  @ onclick and dragging of the numberline are not -yet- supported
  @ note: in case of multiple numberlines, make sure the numberline without special x-axis numbering (e.g. ranging from xmin to xmax) comes first !
  @%numberline%size 400,400%xrange -10,10%yrange -10,10%precision 1%strokecolor black%numberline -8,8,1,6,-4,-3.5%strokecolor red%xaxis -4:AA:-2:BB:2:CC:4:DD%numberline -8,8,1,2,4,4.5%strokecolor green%xaxisup -4:AAA:-2:BBB:2:CCC:4:DDD%numberline -8,8,1,3,2,2.5%strokecolor blue%xaxis -4:AAAA:-2:BBBB:2:CCCC:4:DDDD%numberline -8,8,1,4,0,0.5%strokecolor brown%xaxis -4:AAAAA:-2:BBBBB:2:CCCCC:4:DDDDD%numberline -8,8,1,5,-2,-1.5%zoom red
  */
          js_function[DRAW_NUMBERLINE] = 1;
          for(i=0;i<6;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break;/* xmin */
              case 1: double_data[1] = get_real(infile,0);break;/* xmax */
              case 2: double_data[2] = get_real(infile,0);break;/* xmajor */
              case 3: double_data[3] = get_real(infile,0);break;/* xminor */
              case 4: double_data[4] = get_real(infile,0);break;/* ymin */
              case 5: double_data[5] = get_real(infile,1);/* ymax */
            /*
            var draw_numberline%d = function(canvas_type,xmin,xmax,xmajor,xminor,ymin,ymax,linewidth,strokecolor,strokeopacity,fontfamily,fontcolor);
            */
                fprintf(js_include_file,"snap_x = %f;snap_y = %f;",double_data[2] / double_data[3],double_data[5] - double_data[4] );
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "\ndraw_numberline(%d,%d,%f,%f,%f,%f,%f,%f,%d,\"%s\",%f,\"%s\",\"%s\",%d);  ",NUMBERLINE_CANVAS+numberline_cnt,use_axis_numbering,double_data[0],double_data[1],double_data[2],double_data[3],double_data[4],double_data[5],line_width,stroke_color,stroke_opacity,font_family,font_color,precision));
                add_to_buffer(tmp_buffer);
                numberline_cnt++;
                break;
              default:break;
            }
          }
          reset();
          break;
        case OBABEL:
  /*
  @ obabel x,y,type input,molecule smiles-code or file location, extra arguments,extra arguments,...
  @ will call the ''obabel`` program, if installed.
  @ output will be an svg file
  @ see documentation of obabel for special keys
  @ command <a href='#affine'>affine</a> will produce CSS3 matrix transformations
  @ command <a href='#rotate'>rotate</a> will rotate the object
  @ can be set onclick: <code>javascript:read_dragdrop();</code> will return click numbers of mathml-objects<br>if 4 clickable object are drawn, the reply could be 1,0,1,0 ... meaning clicked on the first and third object
  @ can be set draggable: <code>javascript:read_dragdrop();</code> will return all coordinates in the same order as the canvas script: unmoved object will have their original coordinates...
  @ snaptogrid is supported...snaptopoints will work, but use with care...due to the primitive dragging<br>technically: the dragstuff library is not used...the mathml is embedded in a new div element and not in the html5-canvas
  @ external files may be loaded if they are present on the server or in the modules <br>for example:<br> obabel 0,0,mol,&#36;module_dir/caffeine.mol,-P100,-xb none
  @%obabel_smi%size 400,400%xrange -10,10%yrange -10,10%fillcolor green%drag xy%centered%obabel 0,0,smi,-:c1cFccc1cc,-xb none,-xB blue,-xi,-xt,-xa,-xX,-xP180,-h
  */
          js_function[DRAW_XML] = 1;
          for(i=0;i<4;i++){
            switch(i){
              case 0: double_data[0]=get_real(infile,0);break; /* x in x/y-range coord system -> pixel width */
              case 1: double_data[1]=get_real(infile,0);break; /* y in x/y-range coord system  -> pixel height */
              case 2: URL = get_string_argument(infile,0);break;
              case 3:
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                temp = getSVGMOL(URL,get_string(infile,1));
                decimals = find_number_of_digits(precision);
                if(use_affine == TRUE ){ transform(2,2);}
                if( use_offset != 0 || drag_type != -1 ){int_data[2] = 1;}else{int_data[2] = 0;} /* only centered or not-centered */
                int_data[0] = x2px(double_data[0]);
                int_data[1] = y2px(double_data[1]);
                if( use_slider != -1 && onclick == 0 ){ onclick = 3;}/* no drag&onclick but slideable */
                if( use_slider != -1 && drag_type > 0 ){ onclick = 5;}/* slider+drag*/
                if( strstr(temp,"\"") != 0 ){ temp = str_replace(temp,"\"","'"); }
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "var draw_xml%d = {id:%d,type:'svg',x:[%d],y:[%d],mathml:\"%s\",drag_type:%d,onclick:%d,object_cnt:%d,stroke_color:\"%s\",stroke_opacity:%.2f,fill_color:\"%s\",fill_opacity:%.2f,use_center:%d,use_snap:%d,angle:%f,fontfamily:\"%s\",transform:%s,use_affine:%d,offset:[0,0],use_slider:%s,rotation_center:%s,once:true};slidergroup[%d] = null;draw_xml(draw_xml%d);\n",drawxml_cnt,drawxml_cnt,int_data[0],int_data[1],temp,drag_type,onclick,object_cnt,stroke_color,stroke_opacity,fill_color,fill_opacity,int_data[2],use_snap,angle,font_family,doubledata2js_array(affine_matrix,6,decimals),use_affine,my_sliders,rotation_center,object_cnt,drawxml_cnt));
                add_to_buffer(tmp_buffer);
                if(onclick != 0 ){object_cnt++;}
                drawxml_cnt++;/* keeps track on imported img,div,p,span,mathml,svg */
                break;
              default:break;
            }
          }
          reset();
          break;
        case OPACITY:
  /*
  @ opacity [0-255],[0-255]
  @ opacity [0.0 - 1.0],[0.0 - 1.0]
  @ alternative: transparent
  @ first item is stroke opacity, second is fill opacity
  @%opacity%size 400,400%xrange -10,10%yrange -10,10%opacity 255,0%fcircle -10,0,100,blue%opacity 250,50%fcircle -5,0,100,blue%opacity 200,100%fcircle 0,0,100,blue%opacity 150,150%fcircle 5,0,100,blue%opacity 100,200%fcircle 10,0,100,blue
  */
          for(i = 0 ; i<2;i++){
            switch(i){
              case 0: double_data[0]= get_real(infile,0);break;
              case 1: double_data[1]= get_real(infile,1);break;
              default: break;
            }
           }
          if( double_data[0] > 255 ||  double_data[1] > 255  || double_data[0] < 0 || double_data[1] < 0 ){ canvas_error("opacity [0 - 255], [0 - 255] ");}/* typo or non-RGB ? */
          if( double_data[0] > 1 ){ stroke_opacity = (double) (0.0039215*double_data[0]); }else{ stroke_opacity = double_data[0];} /* 0.0 - 1.0 */
          if( double_data[1] > 1 ){ fill_opacity = (double) (0.0039215*double_data[1]); }else{ fill_opacity = double_data[1];} /* 0.0 - 1.0 */
          break;
        case STROKEOPACITY:
  /*
  @ strokeopacity [0-255]
  @ strokeopacity [0.0 - 1.0]
  @%strokeopacity%size 400,400%xrange -10,10%yrange -10,10%fillopacity 120%strokeopacity 255%circle -10,0,100,blue%strokeopacity 100%circle -5,0,100,blue%strokeopacity 50%circle 0,0,100,blue
  */
          double_data[0]= get_real(infile,1);
          if( double_data[0] > 255 || double_data[0] < 0 ){ canvas_error("opacity [0 - 255] ");}/* typo or non-RGB ? */
          if( double_data[0] > 1 ){ stroke_opacity = (double) (0.0039215*double_data[0]); }else{ stroke_opacity = double_data[0];} /* 0.0 - 1.0 */
          break;
        case FILLOPACITY:
  /*
  @ fillopacity [0-255]
  @ fillopacity [0.0 - 1.0]
  @%fillopacity%size 400,400%xrange -10,10%yrange -10,10%strokeopacity 100%%fillopacity 0%fcircle -10,0,100,blue%fillopacity 50%fcircle -5,0,100,blue%fillopacity 100%fcircle 0,0,100,blue%fillopacity 150%fcircle 5,0,100,blue%fillopacity 200%fcircle 10,0,100,blue
  */
          double_data[0]= get_real(infile,1);
          if( double_data[0] > 255 || double_data[0] < 0 ){ canvas_error("fillopacity [0 - 255] ");}/* typo or non-RGB ? */
          if( double_data[0] > 1 ){ fill_opacity = (double) (0.0039215*double_data[0]); }else{ fill_opacity = double_data[0];} /* 0.0 - 1.0 */
          break;
        case ONCLICK:
  /*
  @ onclick
  @ keyword (no arguments required)
  @ if the next object is clicked, its ''object onclick_or_drag sequence number`` in fly script is returned by <code>javascript:read_canvas();</code>
  @ onclick sequence numbering starts at ''0``, e.g. if there are 6 objects set onclick, the first onclick object will have id-number ''0``, the last id-number ''5``
  @ line based objects will show an increase in line width<br>font based objects will show the text in ''bold`` when clicked.
  @ the click zone (accuracy) is determined by 2&times; the line width of the object
  @ onclick and <a href="#drag">drag x|y|xy</a> may be combined in a single flyscript (although a single object can <b>not</b> be onclick and draggable at the same time...)
  @ note: not all objects may be set onclick
  @%onclick%size 400,400%xrange -10,10%yrange -10,10%opacity 255,60%linewidth 3%onclick%fcircles blue,-3,3,1,1,2,2,3,1,1%onclick%ftriangles red,-4,-4,-4,0,-3,-2,0,0,4,0,2,-4%onclick%frects green,-4,4,-2,2,1,-1,3,-4
  */
          fprintf(js_include_file,"use_dragdrop_reply = true;");
          onclick = 1;
          use_dragstuff=2;
          js_function[INTERACTIVE] = 1;
          break;
        case PARALLEL:
  /*
  @ parallel x1,y1,x2,y2,dx,dy,n,[colorname or #hexcolor]
  @ affine transformations should be identical to flydraw
  @ in case of <a href='#rotate'>rotation</a>  or <a href='#affine'>affine transformation </a>, command parallel will produce <em>n</em> individual segments, and these may be set ''onclick`` or ''drag xy`` individually.
  @ in case of <em>no rotation or transformations</em> the lines can not be set ''onclick`` or ''drag xy``.
  @ note: a large number of parallel lines (large <em>n</em>) may result in a canvasdraw error (...simplify your script...it produces too many lines...)
  @%parallel_click%size 400,400%xrange -10,10%yrange -10,10%parallel -5,5,-4,-5,0.25,0,40,red%rotate 45%onclick%parallel -5,5,-4,-5,0.25,0,40,blue
  @%parallel%size 400,400%xrange -10,10%yrange -10,10%parallel -5,5,-4,-5,0.25,0,40,red
  */
          decimals = find_number_of_digits(precision);
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          if( use_rotate == TRUE || use_affine == TRUE ){
            for( i = 0;i < 8; i++ ){
              switch(i){
                case 0: double_data[0] = get_real(infile,0);break; /* x1-values */
                case 1: double_data[1] = get_real(infile,0);break; /* y1-values */
                case 2: double_data[2] = get_real(infile,0);break; /* x2-values */
                case 3: double_data[3] = get_real(infile,0);break; /* y2-values */
                case 4: double_data[4] = get_real(infile,0);break; /* xv  */
                case 5: double_data[5] = get_real(infile,0);break; /* yv  */
                case 6: int_data[0] = (int) (get_real(infile,0));break; /* n  */
                case 7: stroke_color=get_color(infile,1);break;/* name or hex color */
                default: break;
              }
            }
            double_data[6] = double_data[0]; /* x1 */
            double_data[7] = double_data[1]; /* y1 */
            double_data[8] = double_data[2]; /* x2 */
            double_data[9] = double_data[3]; /* y2 */
            for(i = 0 ; i < int_data[0] ; i++){
              double_data[0] = double_data[6] + i*double_data[4];
              double_data[1] = double_data[7] + i*double_data[5];
              double_data[2] = double_data[8] + i*double_data[4];
              double_data[3] = double_data[9] + i*double_data[5];
              if(use_rotate == TRUE ){ rotate(4,angle,rotationcenter,2);}
              if(use_affine == TRUE ){ transform(4,2);}
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[1],decimals,double_data[3],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
              add_to_buffer(tmp_buffer);
              if(onclick != 0){object_cnt++;}
            }
            dragstuff[4] = 1;
          }
          else  /* use the old parallel version: calculations in javascript */
          {
            for( i = 0;i < 8; i++ ){
              switch(i){
                case 0: double_data[0] = get_real(infile,0);break; /* x1-values */
                case 1: double_data[1] = get_real(infile,0);break; /* y1-values */
                case 2: double_data[2] = get_real(infile,0);break; /* x2-values */
                case 3: double_data[3] = get_real(infile,0);break; /* y2-values */
                case 4: double_data[4] = xmin + get_real(infile,0);break; /* xv  */
                case 5: double_data[5] = ymax + get_real(infile,0);break; /* yv  */
                case 6: int_data[0] = (int) (get_real(infile,0));break; /* n  */
                case 7: stroke_color=get_color(infile,1);/* name or hex color */
                  tmp_buffer=my_newmem(MAX_BUFFER);
                  check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,11,[%.*f,%.*f,%.*f],[%.*f,%.*f,%.*f],[%d,%d,%d],[%d,%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[4],decimals,double_data[1],decimals,double_data[3],decimals,double_data[5],int_data[0],int_data[0],int_data[0],int_data[0],int_data[0],int_data[0],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                  add_to_buffer(tmp_buffer);
                  if(onclick != 0){object_cnt++;}
                  break;
                default: break;
              }
              dragstuff[11] = 1;
            }
          }
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case PLOTSTEPS:
      /*
  @ plotsteps a_number
  @ default 250
  @ only used for commands <a href="#curve">curve / plot</a>
  @ use with care !
      */
          plot_steps = (int) (get_real(infile,1));
          break;
        case POINT:
  /*
  @ point x,y,color
  @ draw a single point at (x;y) in color 'color'
  @ use command <code>linewidth int</code> to adjust size
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @ will not resize on zooming (command <code>circle x,y,r,color</code> will resize on zooming)
  @ attention: in case of command <a href="#rotate">rotate angle</a> a point has rotation center (0:0) in x/y-range
  @%point%size 400,400%xrange -10,10%yrange -10,10%opacity 255,255%linewidth 1%onclick%point 0,0,red%linewidth 2%onclick%point 1,1,blue%linewidth 3%onclick%point 3,3,green%linewidth 4%point 4,4,orange
  */
          for(i=0;i<3;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x */
              case 1: double_data[1] = get_real(infile,0);break; /* y */
              case 2: stroke_color = get_color(infile,1);/* name or hex color */
                if(use_rotate == TRUE ){rotate(2,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(2,2);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,2,[%.*f],[%.*f],[%.2f],[%d],%.2f,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[1],1.5*line_width,line_width,1.5*line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,1,0,0,0,use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                /* object_cnt++; */
                if(onclick != 0){object_cnt++;}
                break;
              default: break;
            }
          }
          dragstuff[2] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case POINTS:
  /*
  @ points color,x1,y1,x2,y2,...,x_n,y_n
  @ draw multiple points at given coordinates in color 'color'
  @ use command <code>linewidth int</code> to adjust size
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually (!)
  @ attention: in case of command <a href="#rotate">rotate angle</a> the points have rotation center (0:0) in x/y-range
  @%points_1%size 400,400%xrange -10,10%yrange -10,10%opacity 255,255%snaptogrid%linewidth 1%drag xy%points red,0,0,1,1,2,2,3,3%drag x%points blue,0,1,1,2,2,3,3,4
  @%points_2%size 400,400%xrange -10,10%yrange -10,10%opacity 255,255%linewidth 1%onclick%points red,0,0,1,1,2,2,3,3%onclick%points blue,0,1,1,2,2,3,3,4
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          decimals = find_number_of_digits(precision);
          for(c = 0 ; c < i-1 ; c = c+2){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,2,[%.*f],[%.*f],[%.2f],[%d],%.2f,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+1],1.5*line_width,line_width,1.5*line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,1,0,0,0,use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            /* object_cnt++; */
            if(onclick != 0){object_cnt++;}
          }
          dragstuff[2] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case POLY:
  /*
  @ poly color,x1,y1,x2,y2...x_n,y_n
  @ polygon color,x1,y1,x2,y2...x_n,y_n
  @ draw closed polygon
  @ use command ''fpoly`` to fill it or use keyword <a href='#filled'>filled</a>
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%polygon_1%size 400,400%xrange -10,10%yrange -10,10%opacity 255,25%fillcolor orange%filled%linewidth 2%drag xy%snaptogrid%poly blue,0,0,1,3,3,1,2,4,-1,3
  @%polygon_2%size 400,400%xrange -10,10%yrange -10,10%opacity 255,25%fillcolor orange%filled%linewidth 1%onclick%poly green,0,0,1,3,3,1,2,4,-1,3
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          i=0;
          c=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            for( c = 0 ; c < 2; c++){
              if(c == 0 ){
                double_data[i] = get_real(infile,0);
                i++;
              }
              else {
                double_data[i] = get_real(infile,1);
                i++;
              }
            }
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */

      /* draw path:  closed & optional filled */
          decimals = find_number_of_digits(precision);
          tmp_buffer=my_newmem(MAX_BUFFER);
          check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,5,%s,[0],[0],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,double_xy2js_array(double_data,i,decimals),line_width,stroke_color,stroke_opacity,stroke_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
          add_to_buffer(tmp_buffer);
          if(onclick != 0){object_cnt++;}
          reset();
          dragstuff[5] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case POLYLINE:
  /*
  @ polyline color,x1,y1,x2,y2...x_n,y_n
  @ brokenline color,x1,y1,x2,y2...x_n,y_n
  @ path color,x1,y1,x2,y2...x_n,y_n
  @ remark: there is <b>no</b> command polylines | brokenlines | paths ... just use multiple commands <code>polyline, x1,y1,x2,y2...x_n,y_n</code>
  @ remark: there are commands <code>userdraw path(s),color</code> and <code>userdraw polyline,color</code>... these are two entirely different things ! the path(s) userdraw commands may be used for freehand drawing(s)<br>the polyline userdraw command is analogue to this polyline|brokenline command
  @ the command interconnects the points in the given order with a line (canvasdraw will not close the drawing: use command <a href="#poly">polygon</a> for this)
  @ use command <a href='#segments'>segments</a> for a series of segments. These may be clicked/dragged individually
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%polyline_1%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%drag xy%snaptogrid%polyline blue,0,0,1,3,3,1,2,4,-1,3
  @%polyline_2%size 400,400%xrange -10,10%yrange -10,10%linewidth 1%onclick%polyline green,0,0,1,3,3,1,2,4,-1,3
  */

          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          i=0;
          c=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            for( c = 0 ; c < 2; c++){
              if(c == 0 ){
                double_data[i] = get_real(infile,0);
                i++;
              }
              else {
                double_data[i] = get_real(infile,1);
                i++;
              }
            }
          }

          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */

      /* draw path: not closed & not filled */
          decimals = find_number_of_digits(precision);
          tmp_buffer=my_newmem(MAX_BUFFER);
          check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,%s,[0],[0],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,double_xy2js_array(double_data,i,decimals),line_width,stroke_color,stroke_opacity,stroke_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
          add_to_buffer(tmp_buffer);
          if(onclick != 0){object_cnt++;}
          /* object_cnt++;*/
          reset();
          dragstuff[4] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case POPUP:
      /*
  @ popup
  @ keyword (no arguments)
  @ if fly-script starts with keyword ''popup``, the canvas image will be exclusively in a popup window (xsize px &times; ysize px)
  @ if keyword ''popup`` is used after command <code>size xsize,ysize</code> the canvas will also be displayed in a popup window with size ''xsize &times; ysize``
  @ the popup window will be embedded into the page as a normal image, when ''status=done``; override with keyword <a href="#status">nostatus</a>
  @ to access the read_canvas and read_dragdrop functions in a popup window, use:<br> <code> function read_all(){<br> if( typeof popup !== 'undefined' ){<br> var fun1 = popup['read_dragdrop'+canvas_scripts[0]];<br>var fun2 = popup['read_canvas'+canvas_scripts[0]];<br> popup.close();<br> return "dragdrop="+fun1()+"\\ncanvas="+fun2();<br> };</code>
  @ to set a canvasdraw produced <a href="#clock">clock</a> or multiple clocks...use something like: <code>popup.set_clock(clock_id,type,diff);</code> as js-function for a button (or something else) in your document page.<br>where in <b>clock_id</b> starts with 0 for the first clock<br><b>type</b> is 1 for Hours,2 for Minutes and 3 for Seconds<br><b>diff</b> is the increment (positive or negative) per click
  @%popup%popup%size 400,400%xrange -2*pi,2*pi%yrange -5,5%precision 0%axis%axisnumbering%opacity 100,190%grid 1,1,grey,2,2,5,black%linewidth 4%fillcolor blue%trange -pi,pi%animate%linewidth 1%precision 1000%jsplot red,4*cos(2*x),2*sin(3*x-pi/6)%strokecolor green%functionlabel H(x)=%userinput function
      */
          use_tooltip = 2;
          break;
        case PROTRACTOR:
  /*
  @ protractor x,y,x_width,type,mode,use_a_scale
  @ x,y are the initial location
  @ x_width: give the width in x-coordinate system (e.g. not in pixels !)
  @ type = 1: a triangle range 0 - 180<br>type = 2: a circle shape 0 - 360
  @ mode: use -1 to set the protractor interactive (mouse movement of protractor)<br>use mode = '0&deg; - 360&deg;' to set the protractor with a static angle of some value
  @ if the value of the user_rotation angle is to be shown...use command <a href='#display'>display degree,color,fontsize</a><a href='#display'>display radian,color,fontsize</a>
  @ use_scale = 1: the protractor will have some scale values printed; use_scale=0 to disable
  @ the rotating direction of the mouse around the protractor determines the clockwise/ counter clockwise rotation of the protractor...
  @ commands ''stroke_color | fill_color | linewidth | opacity | font_family`` will determine the looks of the protractor.
  @ default replyformat: reply[0] = x;reply[1] = y;reply[2] = angle_in_radians<br>use command ''precision`` to set the reply precision.
  @ if combined with a ruler, use replyformat = 32
  @ command <code>snap_to_grid</code> may be used to assist the pupil at placing the protractor
  @ when using command ''zoom``, pay <b>attention</b> to the size and symmetry of your canvas<br>...to avoid a partial image, locate the start position near the center of the visual canvas<br>technical: the actual ''protractor`` is just a static generated image in a new canvas-memory<br>This image is only generated once, and a copy of its bitmap is translated & rotated onto the visible canvas.<br>That is the reason for the ''high-speed dragging and rotating``.<br>I've limited its size to xsize &times; ysize e.g. the same size as the visual canvas...
  @ only one protractor allowed (for the time being)
  @ usage: first left click on the protractor will activate dragging;<br>a second left click will activate rotating (just move mouse around)<br>a third click will freeze this position and the x/y-coordinate and angle in radians will be stored in reply(3)<br>a next click will restart this sequence...
  @%protractor%size 400,400%xrange -5,10%yrange -5,10%hline 0,0,black%vline 0,0,black%fillcolor orange%opacity 255,40%protractor 2,-2,6,0,-1,1,1%mouse red,22
  */
          for( i = 0;i < 6; i++ ){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x-center */
              case 1: double_data[1] = get_real(infile,0);break; /* y-center */
              case 2: double_data[2] = get_real(infile,0);break; /* x-width */
              case 3: int_data[0] = (int)(get_real(infile,0));break; /* type: 1==triangle 2 == circle */
              case 4: int_data[1] = (int)(get_real(infile,0));break; /* passive mode == 0; active mode == -1 */
              case 5: int_data[2] = (int)(get_real(infile,1)); /* use scale */
                decimals = find_number_of_digits(precision);
                if( int_data[1] < 0 ){ js_function[JS_FIND_ANGLE] = 1;}
                add_js_protractor(int_data[0],double_data[0], double_data[1], double_data[2],font_family,stroke_color,stroke_opacity,fill_color,fill_opacity,line_width,int_data[2],int_data[1],use_snap);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, ";protractor%d(); ",canvas_root_id));
                add_to_buffer(tmp_buffer);
                reply_precision = precision;
        /* no reply from protractor if non-interactive */
                if( reply_format == 0 && int_data[1] == -1 ){reply_format = 30;}
                js_function[INTERACTIVE] = 1;
                break;
              default: break;
            }
          }
          break;
        case PIXELS:
  /*
  @ pixels color,x1,y1,x2,y2,x3,y3...
  @ draw rectangular "points" with diameter 1 pixel
  @ pixels can <b>not</b> be dragged or clicked
  @ "pixelsize = 1" may be changed by command <code>pixelsize int</code>
  @%pixels%size 400,400%opacity 255,255%pixelsize 5%pixels red,1,1,2,2,3,3,4,4,5,5,10,10,20,20,30,30,40,40,50,50,60,60,70,70,80,80,90,90,100,100,120,120,140,140,160,160,180,180,200,200,240,240,280,280,320,320,360,360,400,400%#NOTE pixelsize=5...otherwise you will not see them clearly...
  */
          js_function[DRAW_PIXELS] = 1;
          stroke_color=get_color(infile,0);
          i=0;
          c=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            for( c = 0 ; c < 2; c++){
              if(c == 0 ){
                double_data[i] = get_real(infile,0);
                i++;
              }
              else {
                double_data[i] = get_real(infile,1);
              }
            }
        }
        if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
        if(use_affine == TRUE ){ transform(i-1,2);}
        decimals = find_number_of_digits(precision);
        /*  *double_xy2js_array(double xy[],int len,int decimals) */
        tmp_buffer=my_newmem(MAX_BUFFER);
        check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_setpixel(%s,\"%s\",%.2f,%d);\n",double_xy2js_array(double_data,i,decimals),stroke_color,stroke_opacity,pixelsize));
        add_to_buffer(tmp_buffer);
        reset();
        break;
        case PIXELSIZE:
  /*
  @ pixelsize int
  @ in case you want to deviate from default pixelsize = 1(...)
  @ pixelsize 100 is of course a filled rectangle 100px &times; 100px
  */
          pixelsize = (int) get_real(infile,1);
          break;

        case PIECHART:
  /*
  @ piechart xc,yc,radius,'data+colorlist'
  @ (xc: yc) center of circle diagram in xrange/yrange
  @ radius in pixels
  @ data+color list: a colon separated list of raw data and corresponding colors<br>canvasdraw will not check validity of colornames...<br>in case of trouble look into javascript debugging of your browser
  @ example data+colorlist: 32:red:65:green:23:black:43:orange:43:yellow:14:white
  @ the number of colors must match the number of data.
  @ if defined <a href='#fillpattern'>fillpattern some_pattern</a> then the pie pieces will be filled with the respective color and a fill pattern...<br>the pattern is cycled from the 4 pattern primitives: grid,hatch,diamond,dot,grid,hatch,diamond,dot,...
  @ use command <a href='#opacity'>opacity</a> to adjust fill_opacity of colors
  @ use command <a href='#legend'>legend</a> to automatically create a legend using the same colors as pie segments; unicode allowed in legend; expect javascript trouble if the amount of ''pie-slices``, ''pie-colors``, ''pie-legend-titles`` do not match, a javascript console is your best friend...<br>use command ''fontfamily`` to set the font of the legend.
  @ use command <a href='centered'>centered</a> to place <a href='#legend'>legend</a> text inside the piechart. The text is using the same color as the pie segment: use (fill) opacity to enhance visibility.
  @%piechart_1%size 300,200%xrange -10,10%yrange -10,10%legend cars:motorcycles:bicycles:trikes%opacity 255,120%piechart -5,0,75,22:red:8:blue:63:green:7:purple%
  @%piechart_2%size 200,200%xrange -10,10%yrange -10,10%fontfamily 16px Arial%centered%legend cars:motorcycles:bicycles:trikes%opacity 255,60%piechart 0,0,100,22:red:8:blue:63:green:7:purple
  */
          js_function[DRAW_PIECHART] = 1;
          for(i=0;i<5;i++){
            switch(i){
              case 0: int_data[0] = x2px(get_real(infile,0)); break; /* x */
              case 1: int_data[1] = y2px(get_real(infile,0)); break; /* y  */
              case 2: int_data[2] = (int)(get_real(infile,1));break;/* radius*/
              case 3: temp = get_string(infile,1);
                if( strstr( temp, ":" ) != 0 ){ temp = str_replace(temp,":","\",\"");}
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_piechart(%d,%d,%d,%d,[\"%s\"],%.2f,%d,\"%s\",%d,%d);\n",PIECHART,int_data[0],int_data[1],int_data[2],temp,fill_opacity,legend_cnt,font_family,use_filled,use_offset));
                add_to_buffer(tmp_buffer);
                break;
              default:break;
            }
          }
          reset();
          break;
        case RANGE:
  /*
  @ range xmin,xmax,ymin,ymax
  @ if not given: 0,xsize,0,ysize (eg in pixels)
  */
          for(i = 0 ; i<4; i++){
            switch(i){
              case 0: xmin = get_real(infile,0);break;
              case 1: xmax = get_real(infile,1);break;
              case 2: ymin = get_real(infile,0);break;
              case 3: ymax = get_real(infile,1);break;
              default: break;
            }
          }
          if(xmin >= xmax){canvas_error(" xrange is not OK: xmin &lt; xmax !");}
          if(ymin >= ymax){canvas_error(" yrange is not OK: ymin &lt; ymax !");}
          fprintf(js_include_file,"var xmin = %f;var xmax = %f;var ymin = %f;var ymax = %f;",xmin,xmax,ymin,ymax);
          found_size_command = found_size_command+2;
          break;
        case RAYS:
  /*
  @ rays color,xc,yc,x1,y1,x2,y2,x3,y3...x_n,y_n
  @ draw rays in color 'color' and center (xc:yc)
  @ may be set draggable or onclick (every individual ray)
  @%rays_onclick%size 400,400%xrange -10,10%yrange -10,10%onclick%rays blue,0,0,3,9,-3,5,-4,0,4,-9,7,9,-8,1,-1,-9
  @%rays_drag_xy%size 400,400%xrange -10,10%yrange -10,10%drag xy%rays blue,0,0,3,9,-3,5,-4,0,4,-9,7,9,-8,1,-1,-9
  */
          stroke_color=get_color(infile,0);
          fill_color = stroke_color;
          double_data[0] = get_real(infile,0);/* xc */
          double_data[1] = get_real(infile,0);/* yc */
          i=2;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("in command rays too many points / rays in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            fprintf(js_include_file,"/* double_data[%d] = %f */\n",i,double_data[i]);
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if( i%2 != 0 ){canvas_error("in command rays: unpaired x or y value");}
          decimals = find_number_of_digits(precision);
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          for(c=2; c<i;c = c+2){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[c],decimals,double_data[1],decimals,double_data[c+1],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
    /* object_cnt++; */
            if(onclick != 0){object_cnt++;}
          }
          reset();
          dragstuff[4] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case RECT:
  /*
  @ rect x1,y1,x2,y2,color
  @ use command <code>frect x1,y1,x2,y2,color</code> for a filled rectangle
  @ use command/keyword <a href='#filled'>filled</a> before command <code>rect x1,y1,x2,y2,color</code>
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @ note: internally a rect is defined with 4 points. So when performing some affine transformation, other than translation, it will result in some morphing of the rectangle !<br>this is a deviation of flydraw's rect&amp;affine
  @%rect%size 400,400%xrange -10,10%yrange -10,10%rect 0,0,4,-4,green%rect 0,5,4,1,red
  */
          for(i=0;i<5;i++){
            switch(i){
              case 0:double_data[0] = get_real(infile,0);break; /* x-values */
              case 1:double_data[1] = get_real(infile,0);break; /* y-values */
              case 2:double_data[4] = get_real(infile,0);break; /* x-values */
              case 3:double_data[5] = get_real(infile,0);break; /* y-values */
              case 4:stroke_color = get_color(infile,1);/* name or hex color */
                decimals = find_number_of_digits(precision);
                double_data[2] = double_data[4];
                double_data[3] = double_data[1];
                double_data[6] = double_data[0];
                double_data[7] = double_data[5];
          /* using closed PATH (type=5) instead of ctx.rect (type=1)!!!
              0,1   2,3
          6,7      4,5
          x = [0,2,4,6]
          y = [1,3,5,7]
          */
                if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
                if(use_rotate == TRUE ){rotate(8,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(8,2);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,5,[%.*f,%.*f,%.*f,%.*f],[%.*f,%.*f,%.*f,%.*f],[%d],[%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[4],decimals,double_data[6],decimals,double_data[1],decimals,double_data[3],decimals,double_data[5],decimals,double_data[7],line_width,line_width,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}
                /* object_cnt++; */
                reset();
                break;
              }
            }
          dragstuff[5] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case RECTS:
  /*
  @ rects color,x1,y1,x2,y2,.....
  @ use command <code>frect color,x1,y1,x2,y2,.....</code> for a filled rectangle
  @ use command/keyword <a href='#filled'>filled</a> before command <code>rects color,x1,y1,x2,y2,....</code>
  @ use command <code>fillcolor color</code> before ''frects`` to set the fill color.
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually
  @%rects%size 400,400%xrange -10,10%yrange -10,10%rects red,0,0,4,-4,0,5,4,1
  */
      /* using closed PATH (type=5) in stead of ctx.rect (type=1)!!!
      0,1   2,3 .....8,9  10,11.....
  6,7      4,5 .....14,15  12,13.....
  x = [0,2,4,6,8,10,12,14...]
  y = [1,3,5,7,9,11,13,15...]
      */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */

          decimals = find_number_of_digits(precision);
          for(c = 0 ; c+3 < i ; c = c+4){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,5,[%.*f,%.*f,%.*f,%.*f],[%.*f,%.*f,%.*f,%.*f],[%d],[%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+2],decimals,double_data[c+2],decimals,double_data[c],decimals,double_data[c+1],decimals,double_data[c+1],decimals,double_data[c+3],decimals,double_data[c+3],line_width,line_width,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
    /* object_cnt++; */
          }
          dragstuff[5] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          if(use_rotate == TRUE ){
            rotate(i-1,angle,rotationcenter,2);
          }
          reset();
          break;
        case REPLYFORMAT:
  /*
  @ replyformat number
  @ use number=-1 to deactivate the js-functions read_canvas() and read_dragdrop()
  @ default values should be fine !
  @ use command <code>precision [0,1,10,100,1000,10000...]</code> before command ''replyformat`` to set the desired number of decimals in the student reply / drawing
  @ the last value for ''precision int`` will be used to calculate the reply coordinates, if needed (read_canvas();)
  @ choose<ul><li>replyformat 1: <code>x1,x2,x3,x4....x_n<br>y1,y2,y3,y4....y_n</code> x/y in pixels</li><li>replyformat 2: <code> x1,x2,x3,x4....x_n<br> y1,y2,y3,y4....y_n</code> x/y in xrange / yrange coordinate system</li><li>replyformat 3: <code> x1,x2,x3,x4....x_n<br> y1,y2,y3,y4....y_n<br> r1,r2,r3,r4....r_n</code> x/y in pixels, r in pixels</li><li>replyformat 4: <code> x1,x2,x3,x4....x_n<br> y1,y2,y3,y4....y_n<br> r1,r2,r3,r4....r_n</code> x/y in xrange / yrange coordinate system, r in pixels</li><li>replyformat 5: <code> Ax1,Ax2,Ax3,Ax4....Ax_n<br> Ay1,Ay2,Ay3,Ay4....Ay_n<br> Bx1,Bx2,Bx3,Bx4....Bx_n<br> By1,By2,By3,By4....By_n<br> Cx1,Cx2,Cx3,Cx4....Cx_n<br>  Cy1,Cy2,Cy3,Cy4....Cy_n<br>  ....<br>  Zx1,Zx2,Zx3,Zx4....Zx_n<br> Zy1,Zy2,Zy3,Zy4....Zy_n</code> x/y in pixels</li><li>replyformat 6: <code> Ax1,Ax2,Ax3,Ax4....Ax_n<br> Ay1,Ay2,Ay3,Ay4....Ay_n<br> Bx1,Bx2,Bx3,Bx4....Bx_n<br> By1,By2,By3,By4....By_n<br> Cx1,Cx2,Cx3,Cx4....Cx_n<br> Cy1,Cy2,Cy3,Cy4....Cy_n<br> ....<br> Zx1,Zx2,Zx3,Zx4....Zx_n<br> Zy1,Zy2,Zy3,Zy4....Zy_n</code> x/y in xrange / yrange coordinate system</li><li>replyformat 7: <code> x1:y1,x2:y2,x3:y3,x4:y4...x_n:y_n</code> x/y in pixels</li><li>replyformat 8: <code> x1:y1,x2:y2,x3:y3,x4:y4...x_n:y_n</code> x/y in xrange / yrange coordinate system</li><li>replyformat 9: <code> x1:y1:r1,x2:y2:r2,x3:y3:r3,x4:y4:r3...x_n:y_n:r_n</code> x/y in pixels</li><li>replyformat 10: <code> x1:y1:r1,x2:y2:r2,x3:y3:r3,x4:y4:r3...x_n:y_n:r_n</code> x/y in xrange / yrange coordinate system</li><li>replyformat 11: <code> Ax1,Ay1,Ax2,Ay2<br>Bx1,By1,Bx2,By2<br> Cx1,Cy1,Cx2,Cy2<br> Dx1,Dy1,Dx2,Dy2<br> ......<br> Zx1,Zy1,Zx2,Zy2</code> x/y in xrange / yrange coordinate system</li><li>replyformat 12: <code> Ax1,Ay1,Ax2,Ay2<br> Bx1,By1,Bx2,By2<br>Cx1,Cy1,Cx2,Cy2<br> Dx1,Dy1,Dx2,Dy2<br> ......<br> Zx1,Zy1,Zx2,Zy2</code> x/y in pixels</li><li>replyformat 13: <code> Ax1:Ay1:Ax2:Ay2,Bx1:By1:Bx2:By2,Cx1:Cy1:Cx2:Cy2,Dx1:Dy1:Dx2:Dy2, ... ,Zx1:Zy1:Zx2:Zy2</code> x/y in xrange / yrange coordinate system</li><li>replyformat 14: <code> Ax1:Ay1:Ax2:Ay2,Bx1:By1:Bx2:By2....Zx1:Zy1:Zx2:Zy2</code> x/y in pixels</li><li>replyformat 15: reply from inputfields,textareas <code>reply1,reply2,reply3,...,reply_n</code></li><li>replyformat 16: mathml input fields</li><li>replyformat 17: read ''userdraw text,color`` only <code>x1,y1,text1 \\n x2,y2,text2...\\n...x_n,y_n,text_n </code> x/y-values are in xrange/yrange</li><li>replyformat 18: read_canvas() will read all interactive clocks in <code>H1:M1:S1,H2:M2:S2...Hn:Mn:Sn</code></li><li>replyformat 19: read_canvas() will return the object number of marked / clicked object (clock), analogue to (shape library) onclick command</li><li>replyformat 20: read_canvas() will reply "object_number:x:y" of external images: object_number of the first draggable external image in the fly-script starts with 0, e.g. expect something like 0:-5:4,1:6:2,2:-2:-5, the first image position is (-5:4), the second image position is (6:2) and the third image position is (-2:-5)   <li>replyformat 21: <code> (x1:y1) (x2:y2) ... (x_n:y_n)</code> verbatim coordinate return</li><li>replyformat 22: returns an array .... <code>reply[0]=x1 reply[1]=y1 reply[2]=x2 reply[3]=y2 ... reply[n-1]=x_n reply[n]=y_n</code> x/y in xrange / yrange coordinate system</li><li>replyformat 23: can only be used for drawtype ''polyline``. A typical click sequence in drawtype polyline is x1,y1,x2,y2,x2,y2,x3,y3,x3,y3.....,x(n-1),y(n-1),x(n-1),y(n-1),xn,yn --replyformat 23 gives <code>x1,y1,x2,y2,x3,y3,.....x(n-1),y(n-1),xn,yn</code>; multiple occurences will be filtered out. The reply will be in x-y-range (xreply \\n yreply)</li><li>replyformat 24: read all inputfield values: even those set ''readonly``</li><li>replyformat 25: <code> angle1,angle2;...;angle_n</code> will return the radius (one or many) of the user drawn circle segment in degrees</li><li>replyformat 26: <code> rad1,rad2,...rad_n</code> will return the radius (one or many) of the user drawn circle segment in radians</li><li>replyformat 27: return (only) userdraw inputfields <code>x1,y1,text1<br> x2,y2,text2...<br>...x_n,y_n,textn</code></li><li>replyformat 28: <code> x1,y1,r1,x2,y2,r2...x_n,y_n,r_n</code> x / y / r in xrange / yrange coordinate system: may be used to reinput into command <code>circles color,x1,y1,r1,x2,y2,r2...x_n,y_n,r_n</code> will not return anything else (e.g. no inputfields, text etc)</li><li>replyformat 34: a special for OEF and dragging external images -included via commands <a href='#copy'>copy</a> or <a href='#copyresized'>copyresized</a> there will be an extra function <code>read_canvas_images()</code> for reading the coordinates of the images.<br>for now this is a unique function, e.g. there is no ''canvas id`` linked to it. (TO DO !!! 18/5/2019)</li></ul>
  @ special replyformat = 100 ; will access to the raw javascript object data...use: read_dragdrop([property,property,...])<br>for example  properies like 'clicked','text', 'angle' , 'x'
  */
          reply_format = (int) get_real(infile,1);
          reply_precision = precision;
          break;
        case ROUNDRECT:
  /*
  @ roundrect x1,y1,x2,y2,radius in px,color
  @ use command <code>froundrect x1,y1,x2,y2,radius,color</code> for a filled rectangle
  @ use command/keyword <a href='#filled'>filled</a> before command <code>roundrect x1,y1,x2,y2,radius,color</code>
  @ fillcolor will be identical to ''color``
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%roundrect%size 400,400%xrange -10,10%yrange -10,10%roundrect 0,0,4,-4,20,green%roundrect 0,5,4,1,10,red
  */
          for(i=0;i<6;i++){
            switch(i){
              case 0:double_data[0] = get_real(infile,0);break; /* x-values */
              case 1:double_data[1] = get_real(infile,0);break; /* y-values */
              case 2:double_data[2] = get_real(infile,0);break; /* x-values */
              case 3:double_data[3] = get_real(infile,0);break; /* y-values */
              case 4:int_data[0] = (int) (get_real(infile,0));break; /* radius value in pixels */
              case 5:stroke_color = get_color(infile,1);/* name or hex color */
                if(use_rotate == TRUE ){rotate(4,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(2,4);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
      /* ensure no inverted roundrect is produced... */
                if( double_data[0] > double_data[2] ){double_data[4] = double_data[0];double_data[0] = double_data[2];double_data[2] = double_data[4];}
                if( double_data[3] > double_data[1] ){double_data[4] = double_data[1];double_data[1] = double_data[3];double_data[3] = double_data[4];}
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,6,[%.*f,%.*f],[%.*f,%.*f],[%d,%d],[%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[1],decimals,double_data[3],int_data[0],int_data[0],int_data[0],int_data[0],line_width,stroke_color,stroke_opacity,stroke_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}
      /* object_cnt++;*/
                reset();
                break;
            }
          }
          dragstuff[6] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case ROUNDRECTS:
  /*
  @ roundrects color,radius in px,x1,y1,x2,y2,x3,y3,x4,y4,....
  @ for filled roundrects use command/keyword <a href='#filled'>filled</a> before command
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually
  @%roundrects%size 400,400%xrange -10,10%yrange -10,10%roundrects blue,5,0,0,4,-4,5,4,1,2
  */

          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          int_data[0] = (int) (get_real(infile,0)); /* radius value in pixels */
          if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          decimals = find_number_of_digits(precision);
          for(c = 0 ; c+3 < i ; c = c+4){
    /* ensure no inverted roundrect is produced... */
            if( double_data[c] > double_data[c+2] ){double_data[c+4] = double_data[c];double_data[c] = double_data[c+2];double_data[c+2] = double_data[c+4];}
            if( double_data[c+3] > double_data[c+1] ){double_data[c+4] = double_data[c+1];double_data[c+1] = double_data[c+3];double_data[c+3] = double_data[c+4];}
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,6,[%.*f,%.*f],[%.*f,%.*f],[%d,%d],[%d,%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+2],decimals,double_data[c+1],decimals,double_data[c+3],int_data[0],int_data[0],int_data[0],int_data[0],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
    /* object_cnt++; */
          }
          dragstuff[6] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case RULER:
  /*
  @ ruler x,y,x-width,y-height,mode
  @ x,y are the initial location
  @ x-width, y-height are the ruler dimensions width &amp; height in xy-coordinate system
  @ the ruler scale is by definition the x-scale, set by command ''xrange``. For example: a ruler x-width of 6 will have a scale ranging from 0 to 6
  @ mode: use -1 to set the ruler interactive (eg mouse movement of ruler; drag &amp; rotate)<br>use mode = '0&deg; - 360&deg;' to set the ruler with a static angle of some value
  @ if combined with a protractor, use replyformat = 32
  @ only one ruler allowed (for the time being)
  @ when using command ''zoom``, pay <b>attention</b> to the size and symmetry of your canvas to avoid a partial image, locate the start position near the center of the visual canvas; technical: the actual ''ruler`` is just a static generated image in a new canvas-memory. This image is only generated once, and a copy of its bitmap is translated & rotated onto the visible canvas. That is the reason for the ''high-speed dragging and rotating``. I have limited its size to xsize &times; ysize e.g. the same size as the visual canvas...
  @ usage: first left click on the ruler will activate dragging; a second left click will activate rotating (just move mouse around), a third click will freeze this position and the x/y-coordinate and angle in radians will be stored in reply(3), a next click will restart this sequence...
  @%ruler_interactive%size 800,400%xrange -10,10%yrange -10,10%strokecolor blue%opacity 200,50%filled%fillcolor lightgrey%ruler -9,0,10,2,-1%display degree,blue,22
  @%ruler_set_degree_45%size 800,400%xrange -10,10%yrange -10,10%strokecolor blue%opacity 200,50%filled%fillcolor lightgrey%ruler 0,0,10,2,45
  @%ruler_set_degree_125%size 800,400%xrange -10,10%yrange -10,10%strokecolor blue%opacity 200,50%filled%fillcolor lightgrey%ruler 0,0,10,2,125
  */
          for( i = 0;i < 5; i++ ){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x-center */
              case 1: double_data[1] = get_real(infile,0);break; /* y-center */
              case 2: double_data[2] = get_real(infile,0);break; /* x-width */
              case 3: double_data[3] = get_real(infile,0);break; /* y-width */
              case 4: int_data[0] = (int)(get_real(infile,1)); /* passive mode */
                decimals = find_number_of_digits(precision);
                if( int_data[0] < 0 ){
                  js_function[JS_FIND_ANGLE] = 1;
                }
                add_js_ruler(double_data[0],double_data[1],double_data[2],double_data[3], font_family,stroke_color,stroke_opacity,fill_color,fill_opacity,line_width,int_data[0],use_snap);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, ";ruler%d(); ",canvas_root_id));
                add_to_buffer(tmp_buffer);
                reply_precision = precision;
                /* no reply from ruler if non-interactive */
                if( reply_format == 0 && int_data[0] == -1 ){reply_format = 31;}
                break;
              default: break;
            }
          }
          break;
        case RESET:
  /*
  @ reset
  @ keyword
  @ disables the effects of the <a href="noreset">noreset</a> command
  @ so the next objects will not be <a href="#filled">filled</a> and <a href="#dashed">dashed</a>
  */
          no_reset = FALSE; reset();
          break;
        case RESETOFFSET:
  /*
  @ resetoffset
  @ keyword ; use to restore text placement on the canvas to the real (x;y) coordinates of the left bottom corner of the text
  @ may be active for commands <a href="#text">text</a> and <a href="#string">string</a> (e.g. objects in the drag/drop/onclick-librariy
  */
          use_offset = 0;
          break;
        case ROTATE:
  /*
  @ rotate rotation_angle
  @ angle in degrees
  @ (only) the next object will be rotated is given angle
  @ positive values rotate counter clockwise
  @ attention: all objects will be rotated around their first point...<br><code>rotate 45<br>triangle 1,1,5,1,3,4,red</code><br>will rotate 45 degrees around point (1:1)
  @ if another rotation center is needed, use command <a href="#rotationcenter">rotationcenter xc,yc</a>.<br>to reset this rotationcenter, use keyword <a href="#killrotate">killrotate</a>
  @ attention: rotate will mess up the interactivity of the rotated object <br>e.g. if combined with command <a href="#drag">drag xy</a> or keyword <a href="#onclick">onclick</a>: the mouse recognises the original -unrotated- coordinates of the object
  @%rotate_1%size 400,400%xrange -10,10%yrange -10,10%fpoly yellow,0,0,4,3,2,5%rotate 45%fpoly violet,0,0,4,3,2,5%killrotate%rotate 90%fpoly violet,0,0,4,3,2,5%
  */
          use_rotate = TRUE;
          angle = -1*(get_real(infile,1));/* -1: to be compatible with Flydraw... */
          break;
        case ROTATION_CENTER:
  /*
  @ rotationcenter x_center,y_center
  @ define an rotation center in your x/y-coordinate system
  @ wims will not check the validity of your input; use javascript console to debug any erors
  @ if not defined a rotation will be around the first point of an object
  @ to be used before command <a href="#rotate">rotate</a>
  @ use <a href="#killrotate">killrotate</a> to reset to 'null' <br>(eg. the rotationcenter of the next object(s) will be the first (x;y) coordinate of the object(s))
  @ all other commands will use this rotation center, unless a <a href="#killrotation">killrotation</a> is given
  @%rotationcenter%size 400,400%xrange -5,10%yrange -5,10%circles green,3,3,4.25%rotationcenter 3,3%opacity 255,80%fpoly yellow,0,0,4,3,2,5%rotate 45%fpoly violet,0,0,4,3,2,5%rotate 90%fpoly lightblue,0,0,4,3,2,5%rotate 135%fpoly blue,0,0,4,3,2,5%rotate 180%fpoly orange,0,0,4,3,2,5%rotate 225%fpoly green,0,0,4,3,2,5%rotate 270%fpoly cyan,0,0,4,3,2,5%rotate 315%fpoly purple,0,0,4,3,2,5%linewidth 3%point 3,3,red%mouse red,22
  @%rotationcenter_slider%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%fillcolor black%strokecolor yellow%rotationcenter 0,0%fontsize 28%slider 0,2*pi,400,30,angle degrees active, rotate...%plot red,5*sin(x)%filled%linewidth 1%opacity 150,50%fillcolor orange%angle 0,0,pi,0,0,blue
  */
          for( i = 0;i < 2; i++ ){
            switch(i){
              case 0: rotationcenter[0] = get_real(infile,0);break; /* x-center */
              case 1: rotationcenter[1] = get_real(infile,1);break; /* y-center */
            }
            string_length = 1 + snprintf(NULL,0,"[%f,%f ]",rotationcenter[0],rotationcenter[1]);

            rotation_center = my_newmem(string_length);
            snprintf(rotation_center,string_length,"[%f,%f]",rotationcenter[0],rotationcenter[1]);
          }
          break;
        case SIZE:
      /*
  @ size width,height
  @ set canvas size in pixels
  @ mandatory first command (can only be preceded by keyword <a href="#popup">popup</a>)
  @ if xrange and/or yrange is not given the range will be set to pixels:<br>xrange 0,xsize yrange 0,ysize<br>note: lower left corner is origin (0:0) !!! this in contrast to flydraw
      */
          found_size_command = 1;
          /* using fabs: however "xsize == int": so "xsize = abs( (int) get_real(infile,0))" would be the idea... */
          xsize = (int)(fabs(round(get_real(infile,0)))); /* just to be sure that sizes > 0 */
          ysize = (int)(fabs(round(get_real(infile,1))));
          /* sometimes we want xrange / yrange to be in pixels...without telling x/y-range */
          xmin = 0;xmax = xsize;
          ymin = 0;ymax = ysize;

/*
 The sequence in which stuff is finally printed is important !!
*/
          fprintf(stdout,"\n\
          <script>\n\
          /*<![CDATA[*/\n\
          if( typeof(wims_status) === 'undefined' ){ var wims_status = \"$status\";};\
          if( typeof(use_dragdrop_reply) === 'undefined' ){ var use_dragdrop_reply = false;};\
          if( typeof(canvas_scripts) === 'undefined' ){ var canvas_scripts = new Array();};\
          canvas_scripts.push(\"%d\");\n/*]]>*/\n</script>\n\
          ",canvas_root_id);

          /* style=\"display:block;position:relative;margin-left:auto;margin-right:auto;margin-bottom:4px;\" */
          if( use_tooltip != 2){
            fprintf(stdout,"<!-- canvasdraw div -->\n\
            <div tabindex=\"0\" id=\"canvas_div%d\" style=\"max-width:%dpx;width:%dpx;height:%dpx\" class=\"canvas_wrapper\" oncontextmenu=\"return false;\">\
             <canvas class=\"canvas_placeholder\" width=\"%d\" height=\"%d\"></canvas>\
            </div>\n\
            <!-- tooltip and input placeholder -->\n\
            <div id=\"tooltip_placeholder_div%d\" style=\"text-align:center\">\
             <span id=\"tooltip_placeholder%d\" class=\"tooltip_placeholder\"></span></div>\
            <!-- include actual object code via include file -->\n\
            <script id=\"canvas_script%d\" src=\"%s\" defer></script>\n",canvas_root_id,xsize,xsize,ysize,xsize,ysize,canvas_root_id,canvas_root_id,canvas_root_id,getfile_cmd);
          } else {
/*
set canvas_div invisible and do not include placeholder in main html page:
the js-include will also be in a popup window...to be shown when wims &#36;status = done
*/
            fprintf(stdout,"<!-- canvasdraw div invisible -->\n\
            <div tabindex=\"0\" id=\"canvas_div%d\" class=\"canvas_wrapper\" style=\"display:none;max-width:%dpx;width:%dpx;height:%dpx;\">\
             <canvas class=\"canvas_placeholder\" width=\"%d\" height=\"%d\"></canvas>\
            </div>\n\
            <div id=\"tooltip_placeholder_div%d\" style=\"display:none;position:relative;margin-left:auto;margin-right:auto;margin-bottom:4px;\">\
             <span id=\"tooltip_placeholder%d\" class=\"tooltip_placeholder\"></span></div>\
            <!-- include actual object code via include file -->\n\
            <script id=\"canvas_script%d\" src=\"%s\"></script>\n",canvas_root_id,xsize,xsize,ysize,xsize,ysize,canvas_root_id,canvas_root_id,canvas_root_id,getfile_cmd);
          }

/* these must be global...it is all really very poor javascript:( */
fprintf(js_include_file,"\n/* begin generated javascript include for canvasdraw version %s */\n\
\"use strict\";\n\
/* these variables and functions must be global */\
var read_dragdrop%d;\
var read_canvas_images;\
var read_canvas%d;\
var set_clock;\
var clear_draw_area%d;\
var update_draw_area%d;\
var place_image_on_canvas;\
var draw_boxplot;\
var redraw_all%d;\
var userdraw_primitive;\
var wims_canvas_function%d = function(){\n/* common used stuff */\
var userdraw_x = [];var userdraw_y = [];var userdraw_radius = [];\
var xsize = %d;\
var ysize = %d;\
var precision = 100;\
var canvas_div = document.getElementById(\"canvas_div%d\");\
var create_canvas%d = function(canvas_type,size_x,size_y){var cnv;if(document.getElementById(\"wims_canvas%d\"+canvas_type)){ cnv = document.getElementById(\"wims_canvas%d\"+canvas_type);}else{try{ cnv = document.createElement(\"canvas\"); }catch(e){alert(\"Your browser does not support HTML5 CANVAS:GET FIREFOX !\");return;};canvas_div.appendChild(cnv);};cnv.width = size_x;cnv.height = size_y;cnv.style.top = 0;cnv.style.left = 0;cnv.style.position = \"absolute\";cnv.id = \"wims_canvas%d\"+canvas_type;return cnv;};\
function findPosX(i){ var obj = i;var curleft = 0;if(obj.offsetParent){while(1){curleft += obj.offsetLeft;if(!obj.offsetParent){break;};obj = obj.offsetParent;};}else{if(obj.x){curleft += obj.x;};};return curleft;};function findPosY(i){var obj = i;var curtop = 0;if(obj.offsetParent){while(1){curtop += obj.offsetTop;if(!obj.offsetParent){break;};obj = obj.offsetParent;};}else{if(obj.y){curtop += obj.y;};};return curtop;};\
function x2px(x){if(use_xlogscale == 0 ){let res=parseFloat((x-xmin)/(xmax - xmin)*xsize);return (res==xsize)?xsize-1:res;}else{let x_max = Math.log(xmax)/Math.log(xlogbase);let x_min = Math.log(xmin)/Math.log(xlogbase);let x_in = Math.log(x)/Math.log(xlogbase);let res=(x_in-x_min)/(x_max - x_min)*xsize; return (res==xsize)?xsize-1:res;};};\
function px2x(px){if(use_xlogscale == 0 ){return px*(xmax - xmin)/xsize + xmin;}else{let x_max = Math.log(xmax)/Math.log(xlogbase);let x_min = Math.log(xmin)/Math.log(xlogbase);let x_out = x_min +px*(x_max - x_min)/xsize;return Math.pow(xlogbase,x_out);};};\
function px2y(py){if(use_ylogscale == 0 ){return ymax - py*(ymax - ymin)/ysize;}else{let y_max = Math.log(ymax)/Math.log(ylogbase);let y_min = Math.log(ymin)/Math.log(ylogbase);let y_out = y_max +py*(y_min - y_max)/ysize;return Math.pow(ylogbase,y_out);};};\
function y2px(y){if(use_ylogscale == 0){let res= parseFloat((ymax-y)/(ymax - ymin)*ysize);return (res==ysize)?ysize-1:res;}else{var y_max = Math.log(ymax)/Math.log(ylogbase);var y_min = Math.log(ymin)/Math.log(ylogbase);var y_in = Math.log(y)/Math.log(ylogbase);return (y_max - y_in)*ysize/(y_max - y_min);};};\
function scale_x_radius(rx){return rx*xsize/(xmax - xmin);};\
function scale_y_radius(ry){return ry*ysize/(ymax - ymin);};\
function distance(x1,y1,x2,y2){return Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) );};\
function distance_to_line (r,q,x,y){var c = (y) - (-1/r)*(x);var xs = r*(c - q)/(r*r+1);var ys = (r)*(xs)+(q);return parseInt(Math.sqrt( (xs-x)*(xs-x) + (ys-y)*(ys-y) ));};\
function move(obj,dx,dy){for(var p = 0 ; p < obj.x.length; p++){obj.x[p] = obj.x[p] + dx;obj.y[p] = obj.y[p] + dy;};return obj;};\
var snap_x = 1;var snap_y = 1;\
function snap_to_x(x){return x2px(snap_x*(Math.round((px2x(x))/snap_x)));};\
function snap_to_y(y){return y2px(snap_y*(Math.round((px2y(y))/snap_y)));};\
function multisnap_check(x,y,snap){switch(snap){case 1:return [snap_to_x(x),snap_to_y(y)];break;case 2:return [snap_to_x(x),y];break;case 3:return [x,snap_to_y(y)];break;case 4:return snap_to_points(x,y);break;case 5: return snap_to_fun(x,y);break;default: return [x,y];break;};};\
var xlogbase = 10;\
var ylogbase = 10;\
var use_xlogscale = 0;\
var use_ylogscale = 0;\
var x_strings = {};var x_strings_up = [];\
var y_strings = null;\
var use_jsmath = 0;\
var xstart = 0;\
var ystart = 0;\
var unit_x=\" \";\
var unit_y=\" \";\
var dragdrop_reply = [];\
var external_canvas = create_canvas%d(%d,xsize,ysize);",VERSION,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,xsize,ysize,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,EXTERNAL_IMAGE_CANVAS);
/* var xstart,ystart are normally 0 : in case of sgraph they have the 'jump' in the graph */
          break;
        case SEGMENT:
  /*
  @ segment x1,y1,x2,y2,color
  @ alternative: seg
  @ draw a line segment between points (x1:y1)--(x2:y2) in color ''color``
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%segment_onclick%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%onclick%segment 1,1,-9,3,green
  @%segment_drag_y%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%drag y%segment 1,1,-9,3,green
  */
          for(i=0;i<5;i++) {
            switch(i){
              case 0: double_data[0]= get_real(infile,0);break; /* x1-values */
              case 1: double_data[1]= get_real(infile,0);break; /* y1-values */
              case 2: double_data[2]= get_real(infile,0);break; /* x2-values */
              case 3: double_data[3]= get_real(infile,0);break; /* y2-values */
              case 4: stroke_color=get_color(infile,1);/* name or hex color */
                if(use_rotate == TRUE ){rotate(4,angle,rotationcenter,2);}
                if(use_affine == TRUE ){ transform(4,2);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                decimals = find_number_of_digits(precision);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,double_data[1],decimals,double_data[3],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}
                /* object_cnt++; */
                reset();
                break;
              default: break;
            }
          }
          dragstuff[4] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case SEGMENTS:
  /*
  @ segments color,x1,y1,x2,y2,...,x_n,y_n
  @ alternative: segs
  @ draw multiple segments between points (x1:y1)--(x2:y2).....and... (x_n-1:y_n-1)--(x_n:y_n) in color ''color``
  @ use command ''linewidth int`` to adjust size
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually (!)
  @%segments_onclick%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%onclick%segments green,1,1,3,3,0,0,-3,3,1,1,4,-1,-5,5,-3,-1
  @%segments_drag_y%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%drag y%segments green,1,1,3,3,0,0,-3,3,1,1,4,-1,-5,5,-3,-1
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          decimals = find_number_of_digits(precision);
          for(c = 0 ; c+3 < i ; c = c+4){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+2],decimals,double_data[c+1],decimals,double_data[c+3],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
    /* object_cnt++;*/
          }
          reset();
          dragstuff[4] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case SETLIMITS:
  /*
  @ setlimits
  @ keyword: if set, it will produce 4 inputfields for ''xmin,xmax,ymin,ymax`` and an ''ok`` button
  @ may be used for inputfield based zooming / panning
  @ may be styled using command <a href="#css">css</a>
  @ use commands <a href="#xlabel">xlabel / ylabel</a> to change text from xmin to ''xlabel`` etc
  @ note: the input value will not be checked on validity
  @%setlimits%size 400,400%xrange -10,10%yrange -10,10%precision 1%xlabel T%ylabel H%axis%axisnumbering%grid 2,2,grey,2,2,5,grey%precision 100%multistrokecolors red,green,blue,orange%multilinewidth 1,1,2,2%multistrokeopacity 0.6,0.7,0.8,0.9%jsplot red,1/x,-1,x,1/(x-3),1/(x+3)%setlimits
  */
          js_function[JS_SAFE_EVAL] = 1;
          js_function[JSPLOT_AND_ZOOM] = 1;
          add_setlimits(font_size,css_class);
          done = TRUE;
          break;
        case SETPIXEL:
  /*
  @ setpixel x,y,color
  @ A rectangular "point" with diameter 1 pixel centered at (x:y) in xrange / yrange
  @ pixels can <b>not</b> be dragged or clicked
  @ "pixelsize = 1" may be changed by command <code>pixelsize int</code>
  @%setpixel%size 400,400%xrange -10,10%yrange -10,10%setpixel 1,1,red%pixelsize 2%setpixel 2,2,green%pixelsize 3%setpixel 3,3,blue%
  */
          js_function[DRAW_PIXELS] = 1;
          for(i=0;i<3;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0); break; /* x */
              case 1: double_data[1] = get_real(infile,0); break; /* y */
              case 2: stroke_color = get_color(infile,1);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_setpixel([%f],[%f],\"%s\",%.2f,%d);\n",double_data[0],double_data[1],stroke_color,stroke_opacity,pixelsize));
                add_to_buffer(tmp_buffer);
                break;
              default:break;
            }
          }
          reset();
          break;
        case SLIDER:
  /*
  @ slider start_value,end_value,width px,height px,type,label
  @ type may be: ''x,y,angle``
  @ if a slider value display is desired, use for argument ''type``: ''x display``, ''y display``, ''angle radian``, ''angle degree``
  @ is the slider is used for animation, add keyword ''anim`` or ''animate`` to ''type``; for now only one animated slider may be used...
  @ if a unit (or something like that...) for x/y-value display is needed, use commands ''xunit`` and / or ''yunit``
  @ if the translation should be performed using a function, use for type: ''x function``, ''y function``<br>use commands ''sliderfunction_x`` and/or ''sliderfunction_y`` before the slider command to define the functions. Example:<code>sliderfunction_x x^2<br>sliderfunction_y y^2<br>slider -5,5,100,100,xy function,Some_Text<br>...some stuff to slide<br>killslider<br>sliderfunction_x x^2-2<br>slider -15,15,100,10,x function,Some_Other_Text<br>...more stuff to slide<br>killslider... etc</code>
  @ use command ''slider`` before draggable/clickable objects.
  @ drag and drop may be combined with rotation slider<br>for example an arrow rotated by a slider may be placed anywhere (drag&drop)<br><code>size 300,300<br>xrange -5,5<br>yrange -5,5<br>grid 1,1,grey<br>linewidth 3<br>drag xy<br>fillcolor orange<br>strokecolor blue<br>slider 0,2*pi,250,30,angle degrees,Rotate arrow<br>arrow 2,2,5,5,8,red</code><br>note: except a combination 'drag' and 'slider' for command 'latex, katex, mathml, html, obabel'
  @ no slider for a math function, these can be traced using command ''trace_jscurve some_function_in_x``
  @ a slider will affect all draggable objects after the ''slider`` command...<br>and can be used to group translate / rotate several objects...<br>until a next ''slider`` or keyword ''killslider``
  @ amount of sliders is not limited.
  @ a slider can not be set ''snaptogrid`` or other ''snapto*`` : you may always use 'drag xy' in combination with the slider objects
  @ <code>javascript:read_dragdrop();</code> will return an array with ''object_number:slider_value``
  @ every draggable object may have its own slider (no limit in amount of sliders)
  @ label: some slider text. <br>Note: on <a target='new' href='https://katex.org'>KaTeX</a> enabled wims, TeX produced by wims command ''mathmlmath``, is allowed.
  @ use fillcolor for slider controlkey
  @ use strokecolor for slider bar
  @ use fontfamily / fontcolor to set used fonts
  @ use opacity (only fill opacity will be used) to set transparency
  @ the slider canvas will be added to the ''tooltip div``: so incompatible with command tooltip ; setlimits etc
  @%slider_x_y_angle%%size 300,300%xrange -5,5%yrange -5,5%grid 1,1,grey%linewidth 3%fillcolor orange%strokecolor blue%slider 0,2*pi,300,30,angle degrees,Rotate arrow%arrow 0,0,4.5,0,8,red%killslider%opacity 200,100%slider -2,2,300,30,x ,move blue rectangle%frect -4,4,1,-1,blue%killslider%linewidth 2%slider -2,2,300,30,y,move green rectangle%frect -4,4,1,-1,green
  @%slider%%size 300,300%xrange -6,6%yrange -6,6%grid 1,1,grey%linewidth 1%slider 0,2*pi,300,28,angle degree, name%fillcolor lightgreen%opacity 255,30%ftriangle 2,2,-2,2,0,0,red%ftriangle -2,2,-2,-2,0,0,blue%ftriangle -2,-2,2,-2,0,0,green%ftriangle 2,-2,2,2,0,0,orange%rotationcenter 0,0%frect -2,2,2,-2,black
  @%slider_animate%%size 300,300%xrange -6,6%yrange -6,6%grid 1,1,grey%linewidth 1%slider 0,2*pi,300,28,angle anim , name%fillcolor lightgreen%opacity 255,30%ftriangle 2,2,-2,2,0,0,red%ftriangle -2,2,-2,-2,0,0,blue%ftriangle -2,-2,2,-2,0,0,green%ftriangle 2,-2,2,2,0,0,orange%rotationcenter 0,0%frect -2,2,2,-2,black
  @%slider_html_image%%size 400,400%xrange -6,6%yrange -6,6%bgcolor white%grid 1,1,grey%linewidth 1%slider 0,2*pi,300,28,angle degree,%centered%html 0,0,<img src="gifs/domains/sciences/flasks.svg" width="100px" height="100px">
  */
          js_function[INTERACTIVE] = 1;
          int_data[2] = 0; /* --> show_display = 0; */
          for(i=0; i<6 ; i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* start value */
              case 1: double_data[1] = get_real(infile,0);break; /* end value */
              case 2: int_data[0] = (int)(get_real(infile,0));break; /* width */
              case 3: int_data[1] = (int)(get_real(infile,0));break; /* height */
              case 4: temp = get_string_argument(infile,0); /* type: x,y,angle */
                if( strstr(temp,"displ")!=0 ||  strstr(temp,"deg")!=0 || strstr(temp,"rad")!=0 ){int_data[5] = 1; }else{int_data[5] = 0;}
                if(strstr(temp,"anim")!= 0){int_data[3] = 1;}else{int_data[3] = 0;}
                if(strstr(temp,"angle")!= 0){slider_type="R";if( strstr(temp,"rad")!= 0){int_data[2] = 3;}if( strstr(temp,"deg")!= 0){int_data[2] = 4;}}
                else
                  if(strstr(temp,"xy") != 0){slider_type = "XY";if( strstr(temp,"disp")!= 0){int_data[2] = 5;}}
                    else
                      if(strstr(temp,"x") != 0){slider_type = "X";if( strstr(temp,"disp")!= 0){int_data[2] = 1;}}
                      else
                        if(strstr(temp,"y") != 0){slider_type = "Y";if( strstr(temp,"disp")!= 0){int_data[2] = 2;}}
                          else
                            canvas_error("slider can be of type: x,y,angle,fun_x:fun_y");
                  break;
                case 5: temp = get_string_argument(infile,1); if( strstr(temp,"\"") != 0 ){ temp = str_replace(temp,"\""," ");}
        /* slider label : in case of latex/mathmlmath we need to remove the extra " */
              break;
            }
          }
          if(use_slider == -1 ){ /* add once */
            add_slider(int_data[3]);
            if(int_data[5] == 1 ){add_slider_display(precision,font_size,font_color,stroke_opacity);}
          }
          use_slider++;
          if(int_data[3] == 0){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"slider%d = new slider(\"%s\",\"%s\",%d,%d,%d,%d,\"%s\",\"%s\",[%f,%f],%f,%f,'%s',%d);\n",use_slider,slider_type,temp,use_slider,int_data[0],int_data[1],line_width,fill_color,stroke_color,fill_opacity,stroke_opacity,double_data[0],double_data[1],font_family,int_data[2]));
          }else{

            add_slider(int_data[3]);/* use only once !! */
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"slider%d = new animslider(\"%s\",\"%s\",%d,%d,%d,%d,\"%s\",\"%s\",[%f,%f],%f,%f,'%s',%d);\n",use_slider,slider_type,temp,use_slider,int_data[0],int_data[1],line_width,fill_color,stroke_color,fill_opacity,stroke_opacity,double_data[0],double_data[1],font_family,int_data[2]));
          }
          add_to_buffer(tmp_buffer);
          fprintf(js_include_file,"var slider%d;",use_slider);
          use_dragstuff = 2;
//      dragstuff[22] = 1; /* dragstuff switch no 22 is a slider...*/
          js_function[INTERACTIVE] = 1; /* no need to click on object to use slider... */
      //js_function[JS_ROTATE_MOUSE] = 1;
          c = 0;
          for(i=last_slider;i<=use_slider;i++){
            int_data[c] = i; c++;
          }
          my_sliders = data2js_array(int_data,use_slider - last_slider+1);
          break;
        case SGRAPH:
  /*
  @ sgraph xstart,ystart,xmajor,ymajor,xminor,yminor,majorgrid_color,minorgrid_color
  @ primitive implementation of a ''broken scale`` graph...
  @ not very versatile: only usable in combination with userdraw <br>eg no other objects will obey this "coordinate system"<br>if you want to place an object into this coordinate system, be aware that 10% or 20% of xsize and/or ysize is ''lost``.<br>Use these "formulas" to recalculate the virtual coordinates:<br>factor=0.8 in case xstart != xmin (or ystart != ymin)<br>factor=0.9 in case xstart = xmin (or ystart = ymin)<br>px_x_point = ((factor*xsize)/(xmax - xstart))*(x_point - xmax)+xsize<br>x_recalculated = px*(xmax - xmin)/xsize + $xmin<br>px_y_point = -1*factor*y_point*ysize/(ymax - ystart) + ymax*factor*ysize/(ymax - ystart)<br>y_recalculated = ymax - py*(ymax - ymin)/ysize<br>
  @%sgraph%size 400,400%xrange 0,10000%yrange 0,100%sgraph 9000,50,100,10,4,4,grey,blue%userinput_xy%linewidth 2%userdraw segments,red%precision 0%mouse blue,22
  */
            js_function[DRAW_SGRAPH] = 1;
            for(i = 0 ; i < 8 ;i++){
              switch(i){
                case 0:double_data[0] = get_real(infile,0);break;
                case 1:double_data[1] = get_real(infile,0);break;
                case 2:double_data[2] = get_real(infile,0);break;
                case 3:double_data[3] = get_real(infile,0);break;
                case 4:int_data[0] = (int)(get_real(infile,0));break;
                case 5:int_data[1] = (int)(get_real(infile,0));break;
                case 6:stroke_color = get_color(infile,0);break;
                case 7:font_color = get_color(infile,1);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "xstart = %f;\n ystart = %f;\ndraw_sgraph(%d,%d,%f,%f,%d,%d,\"%s\",\"%s\",\"%s\",%f,%d);\n", double_data[0],double_data[1],GRID_CANVAS,precision,double_data[2],double_data[3],int_data[0],int_data[1],stroke_color,font_color,font_family,stroke_opacity,font_size));
                add_to_buffer(tmp_buffer);
                break;
                default:break;
              }
            }
      /* sgraph(canvas_type,precision,xmajor,ymajor,xminor,yminor,majorcolor,minorcolor,fontfamily,opacity)*/
          break;
        case SNAPTOFUNCTION:
  /*
  @ snaptofunction some_function_in_x,some_funtion_in_y
  @ alternative: snaptofun
  @ the next object will snap to the calculated values
  @ note: snaptofun is probably not really useful feature...
  @ if you want only modification of y-values,just use: <code>snaptofunction x,5*sin(1/y)</code>
  @ if you want only modification of x-values,just use: <code>snaptofunction 5*sin(1/x),y</code>
  @ for now only one instance of ''snaptofunction`` is allowed
  @ use rawmath on your functions: no validity checking is done by wims !
  @ note: switching x and y coordinates? <code>snaptofunction y,x</code>
  @%snaptofunction_1%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%precision 100%snaptofunction x,5*sin(x)%linewidth 3%crosshairsize 6%userdraw crosshairs,red%linewidth 2%curve blue,5*sin(x)%xunit = x-value%display x,blue,22
  @%snaptofunction_2%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%precision 100%snaptofunction y^2-9,y%#snaptofunction y^2-9,abs(y)%linewidth 3%crosshairsize 6%userdraw crosshairs,red%linewidth 2%curve blue,sqrt(x+9)%curve blue,-1*sqrt(x+9)%yunit = y-value%display y,blue,22
  */
          temp = get_string_argument(infile,0);
          use_snap = 2;
          use_snap = 5;
          js_function[JS_MATH] = 1;
          fprintf(js_include_file,"var snap_fun = {x:to_js_math('%s'),y:to_js_math('%s')};function snap_to_fun(px,py){ var x = px2x(px); var y = px2y(py); return [ x2px(eval(snap_fun.x)) , y2px(eval(snap_fun.y)) ];};",temp,get_string(infile,1));
          break;
        case SNAPTOPOINTS:
  /*
  @ snaptopoints x1,y1,x2,y2,x3,y3....
  @ a userdraw object will snap to these points.
  @ the array size (e.g. the number of points) of command ''snaptopoints`` is limited by constant MAX_INT (canvasdraw.h)<br>this command may be repeated multiple times (no limit) to add points
  @ a draggable object (use command ''drag x|y|xy``) will snap to the closed of these points when dragged (mouseup)
  @ other options: use keyword ''snaptogrid``, ''xsnaptogrid`` or ''ysnaptogrid``
  @%snaptopoints_1%size 400,400%xrange -5,5%yrange -5,5%snaptopoints -1,-3,-1,-2,-1,0,-1,1,-1,2,-1,3,1,-3,1,-2,1,-1,1,0,1,1,1,2,1,3%linewidth 2%points red,-1,-3,-1,-2,-1,0,-1,1,-1,2,-1,3,1,-3,1,-2,1,-1,1,0,1,1,1,2,1,3%userdraw arrows,red
  @%snaptopoints_2%size 400,400%xrange -5,5%yrange -5,5%snaptopoints -1,-3,-1,-2,-1,0,-1,1,-1,2,-1,3,1,-3,1,-2,1,-1,1,0,1,1,1,2,1,3%linewidth 3%%points blue,-1,-3,-1,-2,-1,0,-1,1,-1,2,-1,3,1,-3,1,-2,1,-1,1,0,1,1,1,2,1,3%drag xy%ftriangle -4,0,-2,3,0,0,red
  */
          i = 0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          decimals = find_number_of_digits(precision);
      /* NEED AN EXTRA COUNTER VARIABLE FOR MORE THAN 2 CALLS */
          fprintf(js_include_file,"if( temp_push_array === 'undefined' ){var temp_push_array;};if( points_to_snap_on === 'undefined' ){var points_to_snap_on;};var temp_push_array = [%s];\n",double_xy2js_array(double_data,i,decimals));
          if( snap_to_points_cnt == 0 ){
            fprintf(js_include_file,"points_to_snap_on = temp_push_array;function find_min_diff(x,y,X,Y){var diff = 100000000;var chk;var idx = 0;var p = 0;while(X[p] != null){chk = distance(x,y,X[p],Y[p]);if( chk  < diff ){ diff = chk; idx = p;};p++;};return idx;};function snap_to_points(x,y){x = px2x(x); y = px2y(y);var xpoints = points_to_snap_on[0];var ypoints = points_to_snap_on[1];var idx = find_min_diff(x,y,xpoints,ypoints);x = xpoints[idx];y = ypoints[idx];return [x2px(x),y2px(y)];};");
          }else{
            fprintf(js_include_file,"points_to_snap_on[0].push.apply(points_to_snap_on[0],temp_push_array[0]);points_to_snap_on[1].push.apply(points_to_snap_on[1],temp_push_array[1]);");
          }
          snap_to_points_cnt = 1; /* do not repeat including the js-functions...just once*/
          use_snap = 4;
          break;
        case SNAPTOGRID:
  /*
  @ snaptogrid
  @ keyword (no arguments required)
  @ a draggable object (use command ''drag x|y|xy``) will snap to the given grid when dragged (mouseup)
  @ in case of userdraw the drawn points will snap to xmajor / ymajor grid
  @ if no grid is defined, points will snap to every integer xrange/yrange value. (eg snap_x=1,snap_y=1)
  @ if you do not want a visible grid, but you only want a ''snaptogrid`` with some value...define this grid with opacity 0.
  @ if xminor / yminor is defined,(use keyword ''axis`` to activate the minor steps) the drawing will snap to xminor and yminor<br>use only even dividers in x/y-minor...for example <code>snaptogrid<br>axis<br>grid 2,1,grey,4,4,7,red</code> will snap on x=0, x=0.5, x=1, x=1.5 .... will snap on y=0, y=0.25 y=0.5 y=0.75 ...
  @%snaptogrid_1%size 400,400%xrange -5,5%yrange -5,5%axis%axisnumbering%precision 1%grid 1,1,grey,2,2,6,grey%linewidth 2%snaptogrid%userdraw crosshairs,blue%mouse red,22
  @%snaptogrid_2%size 400,400%xrange -5,5%yrange -5,5%axis%axisnumbering%precision 1%grid 1,1,grey,4,1,6,grey%linewidth 1%snaptogrid%userdraw crosshairs,blue%mouse red,22
  */
          use_snap = 1;
          break;
        case SQUARE:
  /*
  @ square x,y,side (px),color
  @ draw a square with left top corner (x:y) with side ''side`` in color 'color'
  @ use command <code>fsquare x,y,side,color</code> for a filled square
  @ use command/keyword <a href='#filled'>filled</a> before command <code>square x,y,side,color</code>
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%square%size 400,400%xrange -10,10%yrange -10,10%linewidth 3%filled%fillcolor blue%square 0,0,120,green
  */
          for(i=0;i<4;i++){
            switch(i){
            case 0:double_data[0] = get_real(infile,0);break; /* x1-values */
            case 1:double_data[1] = get_real(infile,0);break; /* y1-values */
            case 2:double_data[2] = get_real(infile,0);break; /* width in px */
            case 3:stroke_color = get_color(infile,1);/* name or hex color */
              decimals = find_number_of_digits(precision);
              double_data[3] = double_data[0] + (xmax - xmin)*double_data[2]/xsize;
              double_data[4] = double_data[1] + -1*(ymax - ymin)*double_data[2]/ysize;
              if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,1,[%.*f,%.*f,%.*f,%.*f],[%.*f,%.*f,%.*f,%.*f],[%d],[%d],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[3],decimals,double_data[3],decimals,double_data[0],decimals,double_data[1],decimals,double_data[1],decimals,double_data[4],decimals,double_data[4],line_width,line_width,line_width,stroke_color,stroke_opacity,stroke_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
              add_to_buffer(tmp_buffer);
              if(onclick != 0){object_cnt++;}/* object_cnt++; */
              reset();break;
            default: break;
            }
          }
          dragstuff[1] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case STATUS:
  /*
  @ status
  @ keyword
  @ alernative: nostatus
  @ used to override the effects of ''status=done`` in wims (answer.phtml)
  @ affects ''readonly`` in inputfields / textareas in canvasimage and all userdraw based commands
  @ e.g.: if keyword ''status`` is set, the pupil will be able to modify the canvas when the ''wims &#36;status variable`` is set to ''done``
  */

          fprintf(js_include_file,"\nwims_status=\"waiting\";\n");
          break;
        case STRING:
  /*
  @ string color,x,y,the text string
  @ may be set ''onclick`` or ''drag xy``
  @ note: when set ''onclick``, use an extra command ''fontsize`` (default: fontsize=12) to adjust the size of the clicked text-string<br>note: a clicked text string will be hardcoded : fontsize+10 in the font family courier
  @ unicode supported: <code>string red,0,0,\\u2232</code><br> See <a target='new' href='https://en.wikipedia.org/wiki/Mathematical_operators_and_symbols_in_Unicode'>https://en.wikipedia.org/wiki/Mathematical_operators_and_symbols_in_Unicode</a><br> See <a  target='new' href='https://en.wikipedia.org/wiki/Greek_script_in_Unicode'>https://en.wikipedia.org/wiki/Greek_script_in_Unicode</a>
  @ use a command like <code>fontfamily italic 24px Arial</code> to set fonts on browser that support font change
  @ super / sub script is supported, using '<b>_</b>' and '<b>^</b>' <br>The font family for the sub/sup string will be Helvetica e.g. your font family settings will be ignored <br>to force end the subscript/supscript, use an extra space...see example:
  @%string_sup_sub%size 400,400%xrange -6,6%yrange -6,6%fontfamily 22px Arial%# you can use a single space for separation in formula...%# use double space to create new word%string blue,-5,3,H_3O^+ + OH^-  \\u2192 2H_2O%# you can use a single space for separation in formula...%string red,-5,-3,H_3 O^+ + OH^\\u2212  \\u2192 2H_2 O%
  @%string%size 400,400%xrange -10,10%yrange -10,10%fontfamily 14px Arial%crosshair -3,-3,red%crosshair 3,3,blue%string red,-3,-3,Hello World%fontfamily Italic 18px Arial%string red,3,3,Hello World%fontfamily 22pt STIX%string black,-10,8,\\u03B1 \\u03B2 \\u03B3 \\u03B4 \\u03B5 \\u03B6 \\u03B7 \\u03B8 \\u03B9 \\u03BA \\u03BB \\u03BC \\u03BD \\u03BE \\u03BF
  */
          if( use_rotate == TRUE  ){js_function[JS_ROTATE_MOUSE] = 1; }
          dragstuff[14] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          for(i=0;i<5;i++){
            switch(i){
              case 0: stroke_color = get_color(infile,0);break;/* name or hex color */
              case 1: double_data[0] = get_real(infile,0);break; /* x in xrange*/
              case 2: double_data[1] = get_real(infile,0);break; /* y in yrange*/
              case 3: decimals = find_number_of_digits(precision);
                if(use_affine == TRUE ){ transform(2,2);}
        /* rotate is done by HTML5/CANVAS context rotation */
                temp = get_string_argument(infile,1);
                if(strstr(temp,"_") != NULL || strstr(temp,"^") != NULL){js_function[DRAW_SUBSUP] = 1;}
                decimals = find_number_of_digits(precision);
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,14,[%.*f],[%.*f],[0],[0],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[1],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,0,0,0,use_rotate,angle,temp,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}
      //onclick = 0;
      //use_offset = 0;
                reset();
                break;
              default:break;
            }
          }
          break;
        case STRINGUP:
  /*
  @ stringup color,x,y,rotation_degrees,the text string
  @ may be set ''onclick`` or ''drag xy``
  @ note: when set ''onclick``, use an extra command ''fontsize`` (default: fontsize=12) to adjust the size of the clicked text-string<br>note: a clicked text string will be hardcoded : fontsize+10 in the font family courier
  @ unicode supported: <code>stringup red,0,0,45,\\u2232</code>
  @ use a command like <code>fontfamily bold 34px Courier</code> to set fonts on browser that support font change
  @ you could use keyword <a href='#yoffset'>yoffset</a> to -sometimes- do a small correction of text placement under/above a point (e.g. text &amp; point have thesame coordinates)
  @ note: no need to ''killrotate`` after ''stringup``<br><code>onclick<br>rotate 45<br>string red,0,0,AAAAAA<br>killrotate<br>string red,4,4,BBBBBB</code><br>is identical with:<br><code>onclick<br>stringup red,0,0,45,AAAAAA<br>string red,4,4,BBBBBB</code>
  @ super / sub script is supported, using '<b>_</b>' and '<b>^</b>' <br>to end the subscript/supscript, use an extra space...see example:
  @%stringup_sub_sup%size 400,400%xrange -6,6%yrange -6,6%fontfamily 22px Arial%# use single space for separation in formula...%# use double space to create new word%stringup red,-5,0,45,H_3 O^+ + OH^\\u2212  \\u2192 2H_2 O%
  @%stringup%size 400,400%xrange -10,10%yrange -10,10%fontsize 24%fontfamily 14px Arial%crosshair -3,0,red%crosshair 3,0,blue%onclick%stringup red,-3,0,-90,Hello World%drag xy%stringup red,-3,0,-45,Hello World%stringup red,-3,0,45,Hello World%stringup red,-3,0,90,Hello World%stringup blue,3,0,-90,Hello World%stringup blue,3,0,-45,Hello World%stringup blue,3,0,45,Hello World%stringup blue,3,0,90,Hello World

  */
      /* html5 canvas rotation is only used for text objects  */
          use_rotate = TRUE ;
          dragstuff[14] = 1;
          js_function[JS_ROTATE_MOUSE] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }

          for(i=0;i<6;i++){
            switch(i){
              case 0: stroke_color = get_color(infile,0);break;/* name or hex color */
              case 1: double_data[0] = get_real(infile,0);break; /* x */
              case 2: double_data[1] = get_real(infile,0);break; /* y */
              case 3: angle = get_real(infile,0);break;/* rotation */
              case 4: decimals = find_number_of_digits(precision);
                temp = get_string_argument(infile,1);
                if(strstr(temp,"_") != NULL || strstr(temp,"^") != NULL){js_function[DRAW_SUBSUP] = 1;}
                decimals = find_number_of_digits(precision);
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,14,[%.*f],[%.*f],[0],[0],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[1],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,0,0,0,use_rotate,angle,temp,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}
                //onclick = 0;
                //use_offset = 0;
                reset();
                break;
              default:break;
            }
          }
          use_rotate = FALSE;
          break;
        case STYLE:
  /*
  @ highlight color,opacity,linewidth
  @ NOT IMPLEMENTED
  @ use command ''onclick``: when the object receives a userclick it will increase its linewidth
  */
         break;
        case STROKECOLOR:
  /*
  @ strokecolor colorname or #hex
  @ to be used for commands that do not supply a color argument (like command ''linegraph``)
  */
          stroke_color = get_color(infile,1);
          break;
        case FLY_TEXT:
  /*
  @ text fontcolor,x,y,font,text_string
  @ font may be described by keywords: giant,huge,normal,small,tiny
  @ use command ''fontsize`` to increase base fontsize for these keywords
  @ may be set ''onclick`` or ''drag xy``
  @ backwards compatible with flydraw
  @ unicode supported: text red,0,0,huge,\\u2232
  @ special: use '_' and '^' to imitate html sup/sub, like H_3O^+ + OH^\\u22i2  \\u2192 2H_2 O
  @ much better to use command <a href='#string'>string</a> combined with <a href='#fontfamily'>fontfamily</a> for a more fine grained control over html5 canvas text element
  @ super / sub script is supported, using '<b>_</b>' and '<b>^</b>' <br>to end the subscript/supscript, use an extra space...see <a href='#string'>string </a> command
  @ Avoid mixing old flydraw commands ''text``, ''textup`` with new canvasdraw commands ''string``, ''stringup``. If the fontfamily was set completely like <code>fontfamily italic 24px Arial</code>. In that case reset ''fontfamily`` to something lke ''fontfamily Arial`` before the old flydraw commands.
  @%text%size 400,400%xrange -10,10%yrange -10,10%fontsize 14%onclick%drag xy%text green,-4,-4,small,Hello World%drag xy%text red,-4,-2,large,Hello World%drag xy%text blue,-4,0,huge,Hello World%drag xy%text green,-4,3,giant,Hello World%drag xy
  */
        for(i = 0; i < 5 ;i++){
          switch(i){
            case 0: stroke_color = get_color(infile,0);break;/* font_color == stroke_color name or hex color */
            case 1: double_data[0] = get_real(infile,0);break; /* x */
            case 2: double_data[1] = get_real(infile,0);break; /* y */
            case 3: fly_font = get_string_argument(infile,0);
              if(strcmp(fly_font,"giant") == 0){
                fly_font_size = (int)(font_size + 24);
              }
              else {
                if(strcmp(fly_font,"huge") == 0){
                  fly_font_size = (int)(font_size + 14);
                }
                else {
                  if(strcmp(fly_font,"large") == 0){
                    fly_font_size = (int)(font_size + 6);
                  }
                  else {
                    if(strcmp(fly_font,"small") == 0){
                      fly_font_size = (int)(font_size - 4);
                      if(fly_font_size<0){fly_font_size = 8;}
                    }
                  }
                }
              }
              break;
            case 4:
              temp = get_string_argument(infile,1);
              if(strstr(temp,"_") != NULL || strstr(temp,"^") != NULL){js_function[DRAW_SUBSUP] = 1;}
              decimals = find_number_of_digits(precision);
              if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
              if(use_affine == TRUE ){ transform(2,2);}
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,14,[%.*f],[%.*f],[0],[0],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%f,\"%s\",%d,%s,%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[1],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,0,0,0,use_rotate,angle,temp,fly_font_size,"null",my_sliders,rotation_center,use_offset));
              add_to_buffer(tmp_buffer);
              if(onclick != 0){object_cnt++;}
              reset();
              break;
            default:break;
            }
          }
          dragstuff[14] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case TEXTAREA:
  /*
  @ textarea x,y,cols,rows,readonly,value
  @ may be further controlled by <a href="#css">css</a>
  @ if ''&#36;status=done`` (e.g. in answer.phtml) the inputfield will be cleared and set readonly. Override this by keyword <a href="#status">status</a>.
  @ if mathml inputfields are present and / or some userdraw is performed, these data will <b>not</b> be send as well (<code>javascript:read_canvas();</code>)
  @ keyword ''xoffset | centered`` is not active for command ''textarea``
  @%textarea%size 400,400%xrange -10,10%yrange -10,10%css color:red;background-color:lightblue;font-size:14px;text-align:center%textarea -3,-2,6,3,1,?%css color:blue;background-color:yellow;font-size:14px;text-align:center%textarea 0,-2,8,2,1,?
  */
          js_function[DRAW_TEXTAREAS] = 1;
          for(i = 0 ; i<6;i++){
            switch(i){
              case 0: int_data[0]=x2px(get_real(infile,0));break; /* x in px */
              case 1: int_data[1]=y2px(get_real(infile,0));break; /* y in px */
              case 2: int_data[2]=abs( (int)(get_real(infile,0)));break;/* cols */
              case 3: int_data[3]=abs( (int)(get_real(infile,0)));break;/* rows */
              case 4: if( get_real(infile,1) >0){int_data[4] = 1;}else{int_data[3] = 0;};break; /* readonly */
              case 5: temp = get_string_argument(infile,1);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_textareas(%d,%d,%d,%d,%d,%d,%d,\"%s\",\"%s\");\n",canvas_root_id,input_cnt,int_data[0],int_data[1],int_data[2],int_data[3],int_data[4],css_class,temp));
                add_to_buffer(tmp_buffer);
                input_cnt++;break;
              default: break;
            }
          }
          if(reply_format == 0 ){reply_format = 15;}
          reset();
          break;
        case TEXTFILL:
  /*
  @ textfill x0,y0,color,some_text
  @ x0,y0 in xrange / yrange
  @ color will be used for the font color
  @ use command <a href="#fontfamily">fontfamily</a> to set font type and size
  @ there is also a command <a href="#userdraw">userdraw textfill,color,some_text</a>
  @%textfill%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%fontfamily 24pt Arial%circles red,0,0,3,3,3,6%textfill 4,4,blue,HELLO WORLD
  */

          js_function[DRAW_TEXTFILL] = 1;
          if(js_function[DRAW_FILLTOBORDER] != 1 ){/* use only once */
            js_function[DRAW_FILLTOBORDER] = 1;
            add_js_filltoborder(canvas_type);
          }
          decimals = find_number_of_digits(precision);
          for(i=0;i<4;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0); break; /* x in px */
              case 1: double_data[1] = get_real(infile,0); break; /* y in py */
              case 2: font_color = get_color(infile,0); break;
              case 3: temp = get_string(infile,1);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_textfill(%d,%*.f,%*.f,'%s','%s',%d,%d,'%s',false); ",FILL_CANVAS+fill_cnt,decimals,double_data[0],decimals,double_data[1],font_color,font_family,xsize,ysize,temp));
                add_to_buffer(tmp_buffer);
                fill_cnt++;
                break;
              default:break;
            }
          }
          reset();
          break;
        case FLY_TEXTUP:
  /*
  @ textup fontcolor,x,y,font,text_string
  @ can <b>not</b> be set ''onclick`` or ''drag xy`` (because of translaton matrix...mouse incompatible)
  @ font may be described by keywords: giant,huge,normal,small,tiny
  @ use command ''fontsize`` to increase base fontsize for the keywords
  @ backwards compatible with flydraw
  @ unicode supported: textup red,0,0,huge,\\u2232
  @ use command ''stringup`` and ''fontfamily`` for a more fine grained control over html5 canvas text element
  @ Avoid mixing old flydraw commands ''text``, ''textup`` with new canvasdraw commands ''string``; ''stringup``. If the fontfamily was set completely like <code>fontfamily italic 24px Arial</code>. In that case reset ''fontfamily`` to something like ''fontfamily Arial`` before the old flydraw commands.
  */
          js_function[DRAW_TEXTS] = 1;
          for(i = 0; i<5 ;i++){
            switch(i){
              case 0: font_color = get_color(infile,0);break;/* name or hex color */
              case 1: int_data[0] = x2px(get_real(infile,0));break; /* x */
              case 2: int_data[1] = y2px(get_real(infile,0));break; /* y */
              case 3: fly_font = get_string_argument(infile,0);
                if(strcmp(fly_font,"giant") == 0){
                  fly_font_size = (int)(font_size + 24);
                }
                else {
                  if(strcmp(fly_font,"huge") == 0){
                    fly_font_size = (int)(font_size + 14);
                  }
                  else {
                    if(strcmp(fly_font,"large") == 0){
                      fly_font_size = (int)(font_size + 6);
                    }
                    else {
                      if(strcmp(fly_font,"small") == 0){
                        fly_font_size = (int)(font_size - 4);
                        if(fly_font_size<0){fly_font_size = 8;}
                      }
                    }
                  }
                }
                break;
              case 4:
                decimals = find_number_of_digits(precision);
                temp = get_string_argument(infile,1);
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_text(%d,%d,%d,%d,\"%s\",\"%s\",%.2f,90,\"%s\",%d,%.2f,%d);\n",STATIC_CANVAS,int_data[0],int_data[1],fly_font_size,"null",font_color,stroke_opacity,temp,use_rotate,angle,use_offset));
                add_to_buffer(tmp_buffer);
                reset();
        //use_offset = 0;
                break;
              default:break;
            }
          }
          break;
        case TRACE_JSCURVE:
  /*
  @ trace_jscurve some_math_function
  @ will use a crosshair to trace the jsmath curve
  @ two inputfields will display the current x/y-values (numerical evaluation by javascript)
  @ default labels ''x`` and ''y``; use commands ''xlabel some_x_axis_name`` and ''ylabel some_y_axis_name`` to customize the labels for the input fields
  @ use commands fontsize and css to format the fonts for labels and inputfields.
  @ use commands ''linewidth, strokecolor, crosshairsize`` to adjust the corsshair.
  @ the client browser will convert your math function to javascript math.<br>use parenthesis and rawmath: use 2*x instead of 2x etc etc no check is done on the validity of your function and/or syntax (use error console to debug any errors...)
  @ be aware that the formulas of the plotted function(s) can be found in the page javascript source
  @%trace_jscurve%size 400,400%xrange -10,10%yrange -10,10%precision 0%axis%axisnumbering%grid 2,2,grey,2,2,5,gray%recision 100%css color:blue;%linewidth 4%crosshairsize 8%trace_jscurve 5*sin(0.1*x^2)%linewidth 1%jsplot red,5*sin(0.1*x^2)%#only one curve can be traced
  */
          js_function[INTERACTIVE] = 1;
          js_function[DRAW_CROSSHAIRS] = 1;
          js_function[DRAW_LINES] = 1;
          js_function[JS_MATH] = 1;
          add_trace_js_mouse(TRACE_CANVAS,stroke_color,get_string(infile,1),font_size,stroke_opacity,line_width,crosshair_size,css_class);
          break;
        case TRANGE:
  /*
  @ trange tmin,tmax
  @ alternative: ranget
  @ default -2,2
  */
          use_parametric = TRUE;
          for(i = 0 ; i<2; i++){
            switch(i){
              case 0: tmin = get_real(infile,0);break;
              case 1: tmax = get_real(infile,1);break;
              default: break;
            }
          }
          if(tmin >= tmax ){canvas_error(" trange is not OK: tmin &lt; tmax!\n");}
          break;
        case TRANSLATION:
  /*
  @ translation tx,ty
  @ alternative: translate
  @ will translate the next objects tx in xrange and ty in yrange
  @ use command ''killtranstation`` to end the command
  @%translation%size 400,400%xrange -10,10%yrange -10,10%linewidth 1%fillpattern grid%triangle -6,6,8,6,5,1,blue%translation 2,2%ftriangle -6,6,8,6,5,1,red
  */
          for(i = 0 ; i<2;i++){
            switch(i){
              case 0: affine_matrix[4] = get_real(infile,0);break;
              case 1: affine_matrix[5] = get_real(infile,1);
                use_affine = TRUE;
      /* the other values affine_matrix[0..3] remain untouched*/
                break;
              default: break;
            }
          }
          break;
        case TRIANGLE:
  /*
  @ triangle x1,y1,x2,y2,x3,y3,color
  @ use ftriangle or keyword <a href='#filled'>filled</a> for a solid triangle
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%triangle%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%opacity 250,150%drag xy%triangle 0,0,-4,4,6,8,red%drag xy%ftriangle 0,0,-4,-4,6,-8,red%fillpattern grid%drag xy%ftriangle -6,6,8,6,5,1,blue
  */
          for(i=0;i<7;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x */
              case 1: double_data[1] = get_real(infile,0);break; /* y */
              case 2: double_data[2] = get_real(infile,0);break; /* x */
              case 3: double_data[3] = get_real(infile,0);break; /* y */
              case 4: double_data[4] = get_real(infile,0);break; /* x */
              case 5: double_data[5] = get_real(infile,0);break; /* y */
              case 6: stroke_color = get_color(infile,1);/* name or hex color */
                decimals = find_number_of_digits(precision);
                if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
                if( use_rotate == TRUE ){ rotate(6,angle,rotationcenter,2);}
                if( use_affine == TRUE ){ transform(6,2);}
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,5,%s,[0],[0],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,double_xy2js_array(double_data,6,decimals),line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}
          /* object_cnt++;*/
                reset();
                break;
              default: break;
            }
          }
          dragstuff[5] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case TRIANGLES:
  /*
  @ triangles color,x1,y1,x2,y2,x3,y3,...
  @ use ftriangles or keyword <a href='#filled'>filled</a> for solid triangles
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually (!)
  @%triangles%size 400,400%xrange -10,10%yrange -10,10%linewidth 3%onclick%triangles red,0,0,-4,4,6,8,0,0,-4,-4,6,-8,-6,6,8,6,5,1%# the same as 3 calls to command triangle
  */
          stroke_color = get_color(infile,0);/* name or hex color */
          if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
          i = 0;
          decimals = find_number_of_digits(precision);
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          while( ! done ){
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            double_data[0] = get_real(infile,0); /* x1 */
            double_data[1] = get_real(infile,0); /* y1 */
            double_data[2] = get_real(infile,0); /* x2 */
            double_data[3] = get_real(infile,0); /* y2 */
            double_data[4] = get_real(infile,0); /* x3 */
            double_data[5] = get_real(infile,1); /* y3 */
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,5,%s,[0],[0],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,double_xy2js_array(double_data,6,decimals),line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
            i = i + 6;
          }
          reset();
          dragstuff[5] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case USERBOXPLOT:
  /*
  @ userboxplot
  @ keyword, no arguments
  @ use before command <a href="#boxplot">boxplot x_or_y,box-height_or_box-width,x_or_y-position</a>
  @ if set, the student will have to calculate "min,Q1,median,Q3,max" and feed these data into the ''draw_boxplot`` function
  @ for example: put the canvas-script into a html element with id='boxplot' and set style='display:none'<br>define a variable called ''student_boxplot`` and fill it with the 5 student-data (from inputfields or something)<br><code>var student_boxplot = new Array(5)<br>function show_boxplot(){<br>student_boxplot[0] = min;<br>student_boxplot[1] = Q1;<br>student_boxplot[2] = median;<br>student_boxplot[3] = Q3;<br>student_boxplot[4] = max;<br>document.getElementById('boxplot').style.display = "block";<br>draw_boxplot(12345,1,2.00,5.00,[0,0,0,0,0],4,"0,0,255",0.78,"255,165,0",0.60,1,0,1,1);<br>};</code><br>In the canvas-script the function draw_boxplot has the following arguments:<br>draw_boxplot=function(canvas_type,xy,hw,cxy,data,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype0,dashtype1)
  */
          js_function[DRAW_BOXPLOT] = 1;
          fprintf(js_include_file,"var boxplot_source = 3;\n");
          js_function[DRAW_JSBOXPLOT] = 2;
          break;
        case USERBOXPLOTDATA:
  /*
  @ userboxplotdata
  @ keyword, no arguments
  @ use before command <a href="#boxplot">boxplot x_or_y,box-height_or_box-width,x_or_y-position</a>
  @ if set, the student will have to generate some statistical data. These data should be put in a named array ''student_boxplot_data``
  @ ''min,Q1,median,Q3,max`` are calculated by a js-function and the 'draw_boxplot' function will draw a boxplot.
  @ see command <a href="#userboxplot">userboxplot</a> for calling 'draw_boxplot()'
  */
          js_function[DRAW_BOXPLOT] = 1;
          fprintf(js_include_file,"var boxplot_source = 2;\n");
          js_function[DRAW_JSBOXPLOT] = 1;
          break;
        case USERDRAW:
  /*
  @ userdraw object_type,color
  @ only a single object_type is allowed.
  @ right mouse click will remove last drawn object.
  @ for multiple different 'userdraw' objects in an exercise, use command <a href="#multidraw">multidraw</a>
  @ implemented object_type: <ul><li>point</li><li>points</li><li>crosshair</li><li>crosshairs</li><li>line</li><li>lines</li><li>vline</li><li>vlines</li><li>hline</li><li>hlines</li><li>demiline</li><li>demilines</li><li>segment</li><li>segments</li><li>polyline | brokenline</li><li>circle</li><li>circles</li><li>arrow</li><li>arrow2 (double arrow)</li><li>arrows</li><li>arrows2 (double arrows)</li><li>curvedarrow</li><li>curvedarrows</li><li>curvedarrow2</li><li>curvedarrows2</li><li>triangle</li><li>polygon</li><li>poly[3-9] (e.g poly3 ... poly7...poly9</li><li>rect</li><li>roundrect</li><li>rects</li><li>roundrects</li><li>freehandline | path</li><li>freehandlines | paths</li><li>clickfill: fill the clicked area with color<br>multiple areas may be selected <br>multiple colors may be provided using commands <a href='#colorpalette'>colorpalette color1,color2,color3,...</a> use <a href='#replyformat'>replyformat 10</a> for checking the user click color ... reply=x1:y1:color1,x2:y2:color2...<br>attention: this will <b>not</b> work for pattern filling, because the pattern image is only generated once and after creation can not be changed !<br>the opacity of this image on a separate canvas is set to 0.01 and not 0 (!!)...in the ''fill algorithm`` the opacity of the matching pixels is set to 1</li><li>dotfill: fill the clicked area with a dot pattern; use command linewidth to change dot size</li><li>diamondfill: fill the clicked area with a diamond pattern</li><li>hatchfill: fill the clicked area with a hatch pattern</li><li>gridfill: fill the clicked area with a grid pattern</li><li>textfill: fill the clicked area with a repeating string<br>userdraw textfill,blue,some_text<br>use command <a href="#fontfamily">fontfamily</a> to adjust text style and size</li><li>''clickfill | pattern filling`` in general:<br>the clicks may be set <a href="#snaptogrid">snaptogrid</a><br>can be used together with command <a href="#floodfill">floodfill or fill</a><br><b>always</b> use together with command <a href="#clearbutton">clearbutton some_text</a> for removal of all click_colored areas<br>the function read_canvas() will return the click coordinates in the sequence of the user clicks<br>use command <a href="#canvastype">canvastype</a> to fill another canvas (default should be fine: DRAG_CANVAS = 5)</li><li>text <br>an inputfield is provided, unicode allowed. The text is drawn a the mouse click, or if used with command ''userinput inputfield`` also at the given x/y-coordonates</li><li>arc</li><li>arcs</li><li>image<br>only a single "image" of every supported type(*) may be added to canvas window from the surrounding html page.<br>the image should have an 'id' and an onclick handler.<br>(*) supported types are ''svg``,''bitmap``,''p-element``,''div-element`` and ''mathml/tex-code`` with ''\\mmlid{int}``.</li><li>images</li><li>input<br>place a single inputfield on ''canvas`` <br>use commands 'css' for css styling: use command ''linewidth`` for adjusting the input field size (default 1)</li><li>inputs<br>place multiple inputfield: placing inputfields on top of each other is not possible</li><li>function : identical to <a href="#userinput">userinput function</a></li></ul>
  @ note: mouselisteners are only active if ''&#36;status != done`` (eg only drawing in an active/non-finished exercise) <br> to overrule use command/keyword ''status`` (no arguments required)
  @ note: object_type text: Any string or multiple strings may be placed anywhere on the canvas.<br>Use command ''fontfamily`` to set font
  @ note: object_type polygone: Will be finished (the object is closed) when clicked on the first point of the polygone again.
  @ note: all objects will be removed -after a javascript confirm box- when clicked on an object point with middle or right mouse button (e.g. event.button != 1: all buttons but left)
  @ use a prefix <a href='#filled'>filled</a> or ''f`` to set fillable objects filled. (fcircles,filledcircles etc)<br> in case of ''fillpattern`` do not use the ''f`` prefix !
  @ for non solid filling, use command <a href="#fillpattern">fillpattern grid,hatch,diamond,dot</a>
  @ use <a href='#opacity'>opacity int,int</a> and <a href='#fillcolor'>fillcolor color</a> to trigger coloured filling of fillable objects
  @ use command ''dashed`` and/or ''dashtype int,int`` to trigger dashing
  @ use command ''replyformat int`` to control / adjust output formatting of javascript function read_canvas(); (the defaults should be fine...)
  @ may be combined with onclick or drag xy of other components of flyscript objects (although not very useful...)
  @ may be combined with keyword <a href='#userinput_xy'>userinput_xy</a>
  @ may be combined width the <a href='#snaptogrid'>snaptogrid snaptopoints </a> etc, to simplify the checking of the student reply
  @ the cursor may be appropriately styled using command <a href='cursor'>cursor</a>
  @ note: when zooming / panning after a drawing, the drawing will NOT be zoomed / panned...this is a "design" flaw and not a feature <br>To avoid trouble do not use zooming / panning together width userdraw.!<br>use command <a href="#multidraw">multidraw</a> is this is a problem for you...
  @ note: the default replyformat for ''userdraw input(s),color`` used format x1;y1;text1 \n x2;y2;test2 \n x_n;y_n;text_n (e.g. it is not a comma separated array...use ''direct exec`` to test)
  @ note: a special case is ''userdraw image,boguscolor``. Images (bitmap or svg or div) present in the exercise page and the img/svg/div-tag with an unique 'id' and <code>onclick='javascript:place_image_on_canvas(this.id)'</code> can be placed onto the canvas.<br>The ''id`` and (x;y) coordinates will be returned using read_canvas();<br> native MathML, MathJax or KaTeX typesetting may be included in div's.(experiments; wims_modules svn version only!)
  @ note: command <br><code>userdraw function,color</code> is identical to acombination of <code>strokecolor color</code> and <code>userinput function</code><br>
  @ note: commands :<br><code>multicolors red,green,blue<br>multilabel f(x)=:g(x)=:h(x)=<br>userdraw functions3,color</code><br>is identical to commands :<br><code>functionlabel f(x)=:p(x)=:w(x)=<br>strokecolor red<br>userinput function <br>strokecolor green<br>userinput function <br>strokecolor blue<br>userinput function</code>
  @%userdraw_canvastype_a%size 400,400%xrange -10,10%yrange -10,10%linewidth 3%grid 2,2,grey%replyformat 10%colorpalette orange,yellow,red,green,lightgreen,blue,lightblue,cyan%canvastype 4%userdraw clickfill,blue%clearbutton REMOVE LAST RECTANGLE
  @%userdraw_canvastype_b%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%canvastype 4%snaptogrid%replyformat 10%userdraw dotfill,blue%clearbutton REMOVE LAST RECTANGLE
  @%userdraw_rect%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%opacity 200,50%userdraw rect,green
  @%userdraw_rects%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%opacity 200,50%userdraw rects,green
  @%userdraw_frect%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%opacity 200,50%userdraw frect,green
  @%userdraw_frects%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%filled%opacity 200,50%userdraw rects,green
  @%userdraw_roundrect%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%filled%opacity 200,50%userdraw roundrect,green
  @%userdraw_roundrects%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%filled%opacity 200,50%userdraw roundrects,green
  @%userdraw_froundrect%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%filled%opacity 200,50%userdraw roundrect,green
  @%userdraw_froundrects%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%filled%opacity 200,50%userdraw roundrects,green
  @%userdraw_line%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw line,green
  @%userdraw_lines%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw lines,green
  @%userdraw_vline%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw vline,green
  @%userdraw_vlines%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw vlines,green
  @%userdraw_hline%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw hline,green
  @%userdraw_hlines%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw hlines,green
  @%userdraw_demiline%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw demiline,green
  @%userdraw_demilines%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw demilines,green
  @%userdraw_arc%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%opacity 200,50%userdraw arc,green
  @%userdraw_arcs%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%opacity 200,50%userdraw arcs,green
  @%userdraw_point%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw point,green
  @%userdraw_points%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw points,green
  @%userdraw_arrow%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw arrow,green
  @%userdraw_arrows%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw arrows,green
  @%userdraw_arrow2%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw arrow2,green
  @%userdraw_arrows2%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw arrows2,green
  @%userdraw_curvedarrow%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%precision 1%linewidth 3%userdraw curvedarrow,red%clearbutton REMOVE ALL ARROWS
  @%userdraw_curvedarrows%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%precision 1%linewidth 3%userdraw curvedarrows,red%clearbutton REMOVE ALL ARROWS
  @%userdraw_curvedarrow2%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%precision 1%linewidth 3%userdraw curvedarrow2,red%clearbutton REMOVE ALL ARROWS
  @%userdraw_curvedarrows2%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%precision 1%linewidth 3%userdraw curvedarrows2,red%clearbutton REMOVE ALL ARROWS
  @%userdraw_crosshair%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw crosshair,green
  @%userdraw_crosshairs%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw crosshairs,green
  @%userdraw_circle%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%filled%opacity 200,50%userdraw circle,green
  @%userdraw_circles%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%filled%opacity 200,50%userdraw circles,green
  @%userdraw_segment%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw segment,green
  @%userdraw_segments%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw segments,green
  @%userdraw_line%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw line,green
  @%userdraw_lines%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw lines,green
  @%userdraw_triangle%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%filled%opacity 200,50%userdraw triangle,green
  @%userdraw_poly5%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw poly5,green
  @%userdraw_filled_poly5%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%opacity 200,50%filled%userdraw poly5,green
  @%userdraw_poly7%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%opacity 200,50%userdraw poly7,green
  @%userdraw_filled_poly7%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%opacity 200,50%filled%userdraw poly7,green
  @%userdraw_polyline%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw polyline,green
  @%userdraw_freehandline%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw freehandline,green
  @%userdraw_filled_freehandline%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%fillcolor orange%opacity 200,50%filled%userdraw freehandline,green
  @%userdraw_freehandlines%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%opacity 200,50%userdraw freehandlines,green
  @%userdraw_input%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%userdraw input,green
  @%userdraw_inputs%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%userdraw inputs,green
  @%userdraw_text%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%fontfamily 42px Courier%userdraw text,green
  @%userdraw_function%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 1%grid 2,2,grey,2,2,5,grey%multicolors orange,red,green,blue,cyan%precision 1000%userdraw functions3,red%jscurve blue,x^2,-x^2,5*cos(x)%precision 1%mouse red,22
  @%userdraw_clickfill_colorpalette%size 400,400%xrange -10,10%yrange -10,10%linewidth 3%circles blue,0,0,4,1,1,6,3,3,3,-3,-3,5%opacity 255,120%colorpalette red,green,yellow,blue%userdraw clickfill,green
  @%userdraw_clickfill_1%size 400,400%xrange -10,10%yrange -10,10%linewidth 3%circles blue,0,0,4,1,1,6,3,3,3,-3,-3,5%opacity 255,120%userdraw clickfill,green
  @%userdraw_clickfill_2%size 400,400%xrange -10,10%yrange -10,10%circles blue,0,0,2,1,1,5,5,5,4,-5,5,6,5,-5,6%userdraw hatchfill,red%#userdraw dotfill,red%#userdraw diamondfill,red%#userdraw gridfill,red
  @%userdraw_clickfill_2%size 400,400%xrange -10,10%yrange -10,10%bgcolor white%# to get nice click coordinates take invisible ''grid`` and use ''snaptogrid`` %grid 1,1,white%snaptogrid%circles blue,0,0,2,1,1,5,5,5,4,-5,5,6,5,-5,6%userdraw gridfill,red
  */
          if( use_userdraw != 0 ){ /* only one object type may be drawn*/
            canvas_error("Only one userdraw primitive may be used in command 'userdraw' use command 'multidraw' for this...");
          }
          js_function[INTERACTIVE] = 1;
          draw_type = get_string_argument(infile,0);
          if( strcmp(draw_type,"textfill") == 0){
            fprintf(js_include_file,"var userdraw_text_string = \"%s\";", get_string(infile,1));
          }
          else {
            if( strcmp(draw_type,"imagefill") == 0){
              fprintf(js_include_file,"var userdraw_image_url = \"%s\";", get_string(infile,1));
            }
            else {
              stroke_color = get_color(infile,1);
            }
          }
          if( strcmp(draw_type,"clickfill") == 0){use_filled = 1;fill_color = stroke_color;}
          reply_precision = precision;
          use_userdraw = 1;
          fprintf(js_include_file,"\n\
/* begin userdraw */\
userdraw_x = new Array();userdraw_y = new Array();userdraw_radius = new Array();\
var forbidden_zone=[xsize+1,ysize+1];var xy_cnt = 0;\
var canvas_userdraw = create_canvas%d(%d,xsize,ysize);\
var context_userdraw = canvas_userdraw.getContext(\"2d\");\
var use_dashed = %d;\
var use_snap = %d;if(use_dashed == 1){if( context_userdraw.setLineDash ){context_userdraw.setLineDash([%d,%d]);}else{if(context_userdraw.mozDash){context_userdraw.mozDash = [%d,%d];};};};\
context_userdraw.lineWidth = %d;var use_filled = %d;\
context_userdraw.strokeStyle =  \"rgba(%s,%.2f)\";\
context_userdraw.font = \"%s\";\
var user_is_dragging = false;\
if(wims_status != \"done\"){\
canvas_div.addEventListener(\"mousedown\" ,user_draw,false);\
canvas_div.addEventListener(\"mousemove\" ,user_drag,false);\
canvas_div.addEventListener(\"touchstart\",function(e){ e.preventDefault();user_draw(e.changedTouches[0]);},false);\
canvas_div.addEventListener(\"touchmove\" ,function(e){ e.preventDefault();user_drag(e.changedTouches[0]);},false);\
canvas_div.addEventListener(\"touchend\" ,function(e){ e.preventDefault();user_drawstop(e.changedTouches[0]);},false);\
};",canvas_root_id,DRAW_CANVAS,use_dashed,use_snap,dashtype[0],dashtype[1],dashtype[0],dashtype[1],line_width,use_filled,stroke_color,stroke_opacity,font_family);

          if( use_filled == 0 ){
            fprintf(js_include_file,"context_userdraw.fillStyle = \"rgba(255,255,255.0)\";");
          }
          else {
            if( use_filled == 1 ){
              fprintf(js_include_file,"context_userdraw.fillStyle = \"rgba(%s,%.2f)\";",fill_color,fill_opacity);
            }
            else {
              js_function[DRAW_FILL_PATTERN] = 1;
              fprintf(js_include_file,"context_userdraw.fillStyle = create_Pattern(0,0,%d,[%s]);\n",use_filled,fill_color);
            }
          }
          add_js_userdraw(draw_type,stroke_color,stroke_opacity,crosshair_size,arrow_head,use_offset,css_class,use_snap,canvas_type,use_filled,fill_color,fill_opacity,line_width,font_family);
          reset();
          break;
        case USERINPUT:
  /*
  @ userinput function inputfield
  @ alternative: userinput_function
  @ alternative: userinput_xy
  @ ''inputfield`` is only usable in combination with some ''userdraw draw_type``
  @ note: the input fields are not cleared after the object is drawn...be aware of multiple idential drawings (many clicks on the ''ok`` button)
  @ ''userinput function`` may be used any time (e.g. without userdraw)
  @ multiple ''userinput function`` commands may be used.
  @ use command <code>functionlabel some_string</code> to define the inputfield text: default value "f(x)="
  @ use command <code>strokecolor some_color</code> to adjust the plot / functionlabel color
  @ use command <code>css some_css</code> to adjust the inputfields
  @ use command <code>fontsize int</code> to adjust the label fonts. (default 12px)
  @ the user input for the function will be corrected by a simple ''rawmath`` implementation...<br>an error message will be shown if javascript can not interpret the user input
  @%userinput_function%size 400,400%xrange -10,10%yrange -10,10%functionlabel your function g(x)=%axis%axisnumbering%xlabel x-axis%ylabel y-axis%grid 2,2,grey,3,3,5,grey%css color:blue;text-align:center%userinput function%# note: number of function inputs not limited
  @%userinput_points%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%# adding 2 inputfields for x and y%userinput inputfield%userdraw points,blue
  @%userinput_arrows%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%#adding 4 inputfields for (x1;y1)---(x2;y2)%userinput inputfieldd%userdraw arrows,blue
  @%userinput_combined%size 400,400%xrange -10,10%yrange -10,10%functionlabel your function g(x)=%axis%axisnumbering%xlabel x-axis%ylabel y-axis%precision 0%grid 2,2,grey,3,3,5,grey%css color:blue;text-align:center%precision 1000%strokecolor red%opacity 255,255%userinput function%# note: number of function inputs not limited%userdraw line,blue
  */
          temp = get_string_argument(infile,1);
          if(strstr(temp,"function") != 0  || strstr(temp,"curve") != 0  || strstr(temp,"plot") != 0 ){
            if( js_function[DRAW_JSFUNCTION] != 1 ){
              js_function[JS_RAWMATH] = 1;
              js_function[DRAW_JSFUNCTION] = 1;
              if(reply_format == 0){reply_format = 24;}/* read canvas_input values */
              add_input_jsfunction(css_class,function_label,input_cnt,stroke_color,stroke_opacity,line_width,use_dashed,dashtype[0],dashtype[1],font_size);
              input_cnt++;
            }
            else {
        /* no need to add DRAW_JSFUNCTION, just call it with the parameters */
            fprintf(js_include_file,"add_input_jsfunction(%d,\"%s\",%s,%d,\"%s\",\"%.2f\",%d,%d,%d,%d);\n",input_cnt,css_class,function_label,line_width,stroke_color,stroke_opacity,use_dashed,dashtype[0],dashtype[1],font_size);
            input_cnt++;
            }
            js_function[JS_MATH] = 1;
            js_function[JS_PLOT] = 1;
          }
          else {
            if(strstr(temp,"inputfield") != 0 ){
              js_function[JS_SAFE_EVAL] = 1;
              js_function[ADD_USER_INPUTS] = 1;
            }
            else {
              canvas_error("userinput argument may be \"function\" or \"inputfield\"");
            }
          }
          break;
        case USERINPUT_XY:
  /*
  @ userinput_xy
  @ keyword (no arguments required)
  @ to be used in combination with command "userdraw object_type,color"
  @ if set two (or three) input fields are added to the document<br>(one for x-values, one for y-values and in case of drawing circle one for radius-values)
  @ the student may use this as correction for (x:y) on a drawing (or to draw without mouse, using just the coordinates)
  @ math input is allowed (e.g something like: 1+3,2*6,1/3,sqrt(3), sin(pi/4),10^-2,log(2)...)<br>eval function is ''protected`` against code injection.
  @ can <b>not</b> be combined with command ''intooltip tiptext`` <br>note: the ''tooltip div element`` is used for placing inputfields
  @ user drawings will not zoom on zooming (or pan on panning)
  @ use command ''css some_css`` to adjust the inputarea.
  @ use command ''fontsize int`` to adjust the text labels (if needed)
  @%userinput_xy%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%precision 0%grid 2,2,grey,3,3,6,black%# provides inputfields for (x1:y1)---(x2:y2)%userinput_xy%linewidth 2%precision 1000%userdraw lines,blue
  */
      /* add simple eval check to avoid code injection with unprotected eval(string) */
          js_function[JS_SAFE_EVAL] = 1;
          js_function[ADD_USER_INPUTS] = 1;
          break;
        case USERINPUT_FUNCTION:
  /*
  @ userinput_function
  @ alternative: userinput
  @ keyword (no arguments required)
  @ if set, a inputfield will be added to the page
  @ repeat keyword for more function input fields
  @ the userinput value will be plotted in the canvas
  @ this value may be read with <code>read_canvas()</code>. <br>for do it yourself js-scripters: If this is the first inputfield in the script, its id is canvas_input0
  @ use before this command ''userinput_function``,<br>commands like ''css some_css``, ''xlabel some_description``, ''opacity int,int``, ''linewidth int``, ''dashed`` and ''dashtype int,int`` to modify
  @ fontsize can be set using command ''fontsize int``
  @ incompatible with command ''intooltip link_text_or_image``: it uses the tooltip div for adding the inputfield
  @%userinput_function%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%xlabel x-axis%ylabel y-axis%precision 0%grid 2,2,grey,2,2,5,grey%precision 1000%linewidth 2%# first inputfield%css color:blue;text-align:center;font-family:Italic;%strokecolor blue%functionlabel g(x)=:p(x)=:k(x)=%userinput function%# second inputfield%css color:green;text-align:center;font-family:Italic;%strokecolor green%userinput function%# third inputfield%css color:purple;text-align:center;font-family:Italic;%strokecolor purple%userinput function%# no limit in number of function inputfields
  */
          if( js_function[DRAW_JSFUNCTION] != 1 ){
            js_function[DRAW_JSFUNCTION] = 1;
            js_function[JS_RAWMATH] = 1;
            if(reply_format == 0){reply_format = 24;}/* read canvas_input values */
            add_input_jsfunction(css_class,function_label,input_cnt,stroke_color,stroke_opacity,line_width,use_dashed,dashtype[0],dashtype[1],font_size);
            input_cnt++;
          }
          else {
        /* no need to add DRAW_JSFUNCTION, just call it with the parameters */
          fprintf(js_include_file,"add_input_jsfunction(%d,\"%s\",%s,%d,\"%s\",\"%.2f\",%d,%d,%d,%d);\n",input_cnt,css_class,function_label,line_width,stroke_color,stroke_opacity,use_dashed,dashtype[0],dashtype[1],font_size);
          input_cnt++;
          }
          js_function[JS_MATH] = 1;
          js_function[JS_PLOT] = 1;
          break;
        case VLINE:
  /*
  @ vline x,y,color
  @ alternative: verticalline
  @ draw a vertical line through point (x:y) in color 'color'
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%vline%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%onclick%vline 0,0,red%onclick%vline 1,0,orange%onclick%vline 2,0,blue%onclick%vline 3,0,green
  */
          for(i=0;i<3;i++) {
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* x-values */
              case 1: double_data[1] = get_real(infile,0);break; /* y-values */
              case 2: stroke_color=get_color(infile,1);/* name or hex color */
                double_data[2] = double_data[0];
                decimals = find_number_of_digits(precision);
                if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
                    tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[0],[0],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[2],decimals,100*ymin,decimals,100*ymax,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
                add_to_buffer(tmp_buffer);
                if(onclick != 0){object_cnt++;}
      /* object_cnt++; */
                reset();
               break;
            }
          }
          dragstuff[4] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case VLINES:
  /*
  @ vlines color,x1,y1,x2,y2....
  @ alternative: verticallines
  @ draw vertical lines through points (x1:y1),(x2:y2)... in color 'color'
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually
  @%vlines%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%onclick%vlines red,1,0,2,0,3,0,4,0
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          decimals = find_number_of_digits(precision);
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          for(c = 0 ; c < i-1 ; c = c+2){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[0],[0],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c],decimals,100*ymin,decimals,100*ymax,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
            /* object_cnt++; */
          }
          reset();
          dragstuff[4] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case VIDEO:
  /*
  @ video x,y,w,h,videofile location
  @ x,y: left top corner of audio element (in xrange / yrange)
  @ w,y: width and height in pixels
  @ video format may be in *.mp4 (todo: other formats)
  @%video%size 400,400%xrange -10,10%yrange -10,10%opacity 200,100%frect -9,9,6,-6,green%video -5,5,200,200,http://techslides.com/demos/sample-videos/small.mp4
  */
          js_function[DRAW_VIDEO] = 1;
          for(i=0;i<5;i++){
            switch(i){
              case 0: int_data[0] = x2px(get_real(infile,0)); break; /* x in x/y-range coord system -> pixel */
              case 1: int_data[1] = y2px(get_real(infile,0)); break; /* y in x/y-range coord system -> pixel */
              case 2: int_data[2] = (int) (get_real(infile,0)); break; /* pixel width */
              case 3: int_data[3] = (int) (get_real(infile,0)); break; /* height pixel height */
              case 4: temp = get_string(infile,1);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_video(%d,%d,%d,%d,%d,\"%s\");\n",canvas_root_id,int_data[0],int_data[1],int_data[2],int_data[3],temp));
                add_to_buffer(tmp_buffer);
                break;
              default:break;
            }
          }
          reset();
          break;
        case X_AXIS_STRINGS:
  /*
  @ xaxis num1:string1:num2:string2:num3:string3:num4:string4:....num_n:string_n
  @ alternative: xaxistext
  @ usable for commands <a href="#numberline">numberline</a> and <a href="#grid">grid</a> or combinations thereof
  @ use these x-axis num1...num_n values instead of default xmin...xmax
  @ in case of command ''grid``. there is no need to use keyword <a href="#axisnumbering">axisnumbering</a>
  @ use command <a href="#axis">axis</a> to have visual x/y-axis lines (see command <a href="#grid">grid</a>
  @ use command ''fontcolor``, ''fontfamily`` to adjust font <br>defaults: black,12,Arial<br>note: command ''fontsize`` is not active for this command.(''fontsize`` can be used for the <a href="#legend">legend</a> in a <a href="#grid">grid</a>)
  @ a javascript error message will flag non-matching value:name pairs
  @ if the ''x-axis words`` are too big and will overlap, a simple alternating offset will be applied
  @ to be used before command grid (see <a href="#grid">command grid</a>)
  @ ''xmajor`` steps should be synchronised with numbers eg. ''1`` in the next example <code>grid 1,100,grey,1,4,6,grey</code>
  @%xaxistext%size 800,200%xrange -1,13%yrange -5,10%axis%xaxistext 1:january:2:february:3:march:4:april:5:may:6:june:7:july:8:august:9:september:10:october:11:november:12:december%grid 1,4,grey,1,2,10,red
  */
          use_axis_numbering++;
          temp = get_string(infile,1);
          if( strstr(temp,":") != 0 ){ temp = str_replace(temp,":","\",\"");}
          if( strstr(temp,"pi") != 0 ){ temp = str_replace(temp,"pi","(3.1415927)");}/* we need to replace pi for javascript y-value*/
          fprintf(js_include_file,"x_strings[%d] = [\"%s\"];x_strings_up[%d] = null;",use_axis_numbering,temp,use_axis_numbering);
          break;
        case X_AXIS_STRINGS_UP:
  /*
  @ xaxisup num1:string1:num2:string2:num3:string3:num4:string4:....num_n:string_n
  @ alternative: xaxistextup
  @ the text will be rotated 90&deg; up
  @ no need to use keyword <a href="#axisnumbering">axisnumbering</a>
  @ use command <a href="#axis">axis</a> to have visual x/y-axis lines (see command <a href="#grid">grid</a>
  @ use these x-axis num1...num_n values instead of default xmin...xmax
  @ use command ''fontcolor``, <a href="#fontfamily">fontfamily</a> to adjust font <br>defaults: black,12,Arial<br>note: command ''fontsize`` is not active for this command.(''fontsize`` can be used for the <a href="#legend">legend</a> in a <a href="#grid">grid</a>)
  @ a javascript error message will flag non-matching value:name pairs
  @ if the ''x-axis words`` are too big, they will overlap the graph<br> (in this case the text will start from ysize upwards)
  @ to be used before command grid (see <a href="#grid">command grid</a>)
  @''xmajor`` steps should be synchronised with numbers eg. "1" in the next example <code>grid 1,100,grey,1,4,6,grey</code>
  @%xaxistextup%size 800,300%xrange -1,13%yrange -10,10%fontfamily Italic 18pt Courier%axis%xaxistextup 1:january:2:february:3:march:4:april:5:may:6:june:7:july:8:august:9:september:10:october:11:november:12:december%grid 1,4,grey,1,2,10,red
  */
          use_axis_numbering++;
          temp = get_string(infile,1);
          if( strstr(temp,":") != 0 ){ temp = str_replace(temp,":","\",\"");}
          if( strstr(temp,"pi") != 0 ){ temp = str_replace(temp,"pi","(3.1415927)");}/* we need to replace pi for javascript y-value*/
          fprintf(js_include_file,"x_strings_up[%d] = 1;x_strings[%d] = [\"%s\"];",use_axis_numbering,use_axis_numbering,temp);
          break;
        case XERRORBARS:
  /*
  @ xerrorbars color,E1,E2,x1,y1,x2,y2,...,x_n,y_n
  @ draw multiple points with x-errorbars E1 (error value left from point) and E2 (error value right from point) at given coordinates in color 'color'
  @ the errors E1 and E2 values are in xrange.
  @ use command ''linewidth int`` to adust size
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually (!)
  @%xerrorbars%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%drag xy%xerrorbars red,0.8,1.3,0,0,1,1,2,3,3,2,4,5,5,2,6,1,-1,-2,-2,0,-3,2,-4,4,-5,-1

  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
                double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          decimals = find_number_of_digits(precision);
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          for(c = 2 ; c < i-1 ; c = c+2){
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,20,[%.*f],[%.*f],[%.2f],[%.2f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+1],double_data[0],double_data[1],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,1,0,0,0,use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            /* object_cnt++; */
            if(onclick != 0){object_cnt++;}
          }
          reset();
          dragstuff[20] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case NEWRANGE:
  /*
  @ newrange xmin,xmax,ymin,ymax
  @ objects defined after command will make use of this new range
  @ https://wimsedu.info/?topic=dessiner-des-portions-de-fonctions-sur-un-meme-graphe
  */
          for(i = 0 ; i<4; i++){
            switch(i){
              case 0: xmin = get_real(infile,0);break;
              case 1: xmax = get_real(infile,0);break;
              case 2: ymin = get_real(infile,0);break;
              case 3: ymax = get_real(infile,1);break;
              default: break;
            }
          }
          tmp_buffer=my_newmem(MAX_BUFFER);
          check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "\n\nxmin = %f;xmax = %f;ymin = %f;ymax = %f;\n\n",xmin,xmax,ymin,ymax));
          add_to_buffer(tmp_buffer);
          break;
        case XRANGE:
  /*
  @ xrange xmin,xmax
  @ alternative: rangex
  @ if not given: 0,xsize (eg in pixels)
  */
          for(i = 0 ; i<2; i++){
            switch(i){
              case 0: xmin = get_real(infile,0);break;
              case 1: xmax = get_real(infile,1);break;
              default: break;
            }
          }
          if(xmin >= xmax){canvas_error(" xrange is not OK: xmin &lt; xmax !");}
          fprintf(js_include_file,"var xmin = %f;var xmax = %f;",xmin,xmax);
          found_size_command++;
          break;
        case XSNAPTOGRID:
  /*
  @ xsnaptogrid
  @ keyword (no arguments required)
  @ a draggable object (use command ''drag x|y|xy``) will snap to the given x-grid values when dragged (mouseup)
  @ in case of userdraw the drawn points will snap to xmajor grid
  @ if no grid is defined, points will snap to every integer xrange value. (eg snap_x=1)
  @ if you do not want a visible grid, but you only want a ''snaptogrid`` with some value...define this grid with opacity 0.
  @ if xminor is defined (use keyword ''axis`` to activate xminor), the drawing will snap to xminor <br>use only even dividers in x-minor...for example<br><code>xsnaptogrid<br>axis<br>grid 2,1,grey,4,4,7,red</code><br> will snap on x=0, x=0.5, x=1, x=1.5 ....<br> will snap on y=0, y=0.25 y=0.5 y=0.75 ...<br>
  @%xsnaptogrid_1%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 2%xsnaptogrid%userdraw segments,red%precision 1%display x,red,12
  @%xsnaptogrid_2%size 400,400%xrange -10,10%yrange -10,10%grid 1,1,grey%linewidth 3%drag x%points red,0,0,0,0,0,0,0,0,0,0
*/
          use_snap = 2;
          break;
        case XOFFSET:
  /*
  @ xoffset
  @ keyword ; to place the text centered above the text coordinates(x:y) ...
  @ may be used for points or other things requiring centered labels
  @ use <a href="#fontfamily">fontfamily</a> for setting the font
  @ may be active for commands <a href="#text">text</a> and <a href="#string">string</a> (e.g. objects in the drag/drop/onclick-library)
  @%xoffset%size 400,400%xrange -10,10%yrange -10,10%fontfamily 12pt Arial%string blue,-9,-9,no offset%point -9,-9,red%centered%string blue,-6,-6,centered%point -6,-6,red%xoffset%string blue,-3,-3,xoffset%point -3,-3,red%yoffset%string blue,0,0,yoffset%point 0,0,red%xyoffset%string blue,3,3,xyoffset%point 3,3,red%resetoffset%string blue,6,6,resetoffset%point 6,6,red
  */
          use_offset = 2;
          break;
        case XYOFFSET:
  /*
  @ xyoffset
  @ keyword ; to place the text (x:y) to (x+dx:y+dy)... dx/dy are dependent on fontsize/fontfamily
  @ may be used for points or other things requiring labels
  @ use <a href="#fontfamily">fontfamily</a> for setting the font
  @ only active for commands <a href="#text">text</a> and <a href="#string">string</a> (e.g. objects in the drag/drop/onclick-librariy
  @ in case of inputfields the inputfield will be centered x and y on its coordinates.<br>for example:<br>inputs 1,1,10,? <br>point 1,1,red <br> the point will be completely invisible<br>note: keyword ''xyoffset`` will also provide centering if used with <a href='#userdraw'>input(s),color</a>
  @%xyoffset%size 400,400%xrange -10,10%yrange -10,10%fontfamily 12pt Arial%string blue,-9,-9,no offset%point -9,-9,red%centered%string blue,-6,-6,centered%point -6,-6,red%xoffset%string blue,-3,-3,xoffset%point -3,-3,red%yoffset%string blue,0,0,yoffset%point 0,0,red%xyoffset%string blue,3,3,xyoffset%point 3,3,red%resetoffset%string blue,6,6,resetoffset%point 6,6,red
  */
          use_offset = 3;
          break;
        case XUNIT:
  /*
  @ xunit some_unit_for_x-values
  @ unicode allowed (no html code)
  @ use together with command <a href="#display">display or mouse</a>
  @ will display the cursor x-coordinate in ''unit``
  @%xunit%size 400,400%xrange -10,10%yrange -10,10%xunit cm \\u00B2%grid 2,2,grey%linewidth 2%userdraw segments,blue%display x,blue,18
  */
          fprintf(js_include_file,"unit_x = \"%s\";",get_string(infile,1));
          break;
        case XLABEL:
  /*
  @ xlabel some_string
  @ will be used to create a label for the x-axis (label is in quadrant I)
  @ can only be used together with command ''grid``<br>not depending on keywords ''axis`` and ''axisnumbering``
  @ font setting: italic Courier, fontsize will be slightly larger (fontsize + 4)<br>use command ''fontsize`` to adjust.<br>(command ''fontfamily`` is not active for this command)
  @ use <a href="#ylabel">ylabel</a>
  @%xlabel%size 400,400%xrange -10,10%yrange -10,10%axis%axisnumbering%xlabel cm\\u00B2 %ylabel v\\u00B2 %precision 1%grid 2,2,grey,2,2,5,grey
  */
          temp = get_string(infile,1);
          fprintf(js_include_file,"var xaxislabel = \"%s\";",temp);
          break;
        case XLOGBASE:
  /*
  @ xlogbase number
  @ sets the logbase number for the x-axis
  @ default value 10
  @ use together with commands xlogscale / xylogscale
  */
          fprintf(js_include_file,"xlogbase=%d;",(int)(get_real(infile,1)));
          break;
        case XLOGSCALE:
  /*
  @ xlogscale ymajor,yminor,majorcolor,minorcolor
  @ the x/y-range are set using commands <code>xrange xmin,xmax</code> and <code>yrange ymin,ymax</code>
  @ ymajor is the major step on the y-axis; yminor is the divisor for the y-step
  @ the linewidth is set using command ''linewidth int``
  @ the opacity of major / minor grid lines is set by command <a href='#opacity'>opacity</a>
  @ default logbase number = 10 ... when needed, set the logbase number with command ''xlogbase number``
  @ the x/y- axis numbering is triggered by keyword ''axisnumbering``<ul><li>use command ''precision`` before ''xlogscale`` command to set the precision (decimals) of the axis numbering</li><li>use commands ''xlabel some_text`` and/or ''ylabel some_text`` for text on axis: use command ''fontsize int`` to set the fontsize (default 12px)</li><li>use command ''fontfamily fnt_family_string`` to set the fonts for axis-numbering</li><li>use command ''fontcolor`` to set the color</li></ul>
  @ note: the complete canvas will be used for the ''log paper``
  @ note: userdrawings are done in the log paper, e.g. javascript:read_canvas() will return the real values
  @ note: command ''mouse color,fontsize`` will show the real values in the logpaper.<br>\
  @ note: when using something like ''xrange 0.0001,0.01``...combined with commands <a href='#mouse'>mouse</a> and/or <a href='#userdraw'>userdraw</a>...<br> make sure the <a href='#precision'>precision</a> is set accordingly
  @ note: in case of userdraw, the use of keyword <a href='#userinput_xy'>userinput_xy</a> may be handy !
  @ <b>attention</b>: keyword ''snaptogrid`` may not lead to the desired result...
  @ <b>attention</b>: do not use command ''zoom``
  @%xlogscale%size 400,400%xrange 10,50000%yrange -5,5%xlabel x-axis%ylabel y-axis%xlogscale 10,1,black,grey%display x,red,22
  */
          use_axis_numbering++;if(use_axis_numbering > 1){use_axis_numbering = 1;}
          if( js_function[DRAW_GRID] == 1 ){canvas_error("only one type of grid is allowed...");}
          js_function[DRAW_XLOGSCALE] = 1;
          for(i=0;i<4;i++){
            switch(i){
              case 0: double_data[0] = get_real(infile,0);break; /* xmajor */
              case 1: int_data[0] = (int) (get_real(infile,0));break; /* xminor */
              case 2: stroke_color = get_color(infile,0); break;
              case 3: fill_color = get_color(infile,1);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_grid%d(%d,%d,\"%s\",\"%s\",%.2f,%.2f,%d,\"%s\",\"%s\",%d,%f,%d,%d); ",canvas_root_id,GRID_CANVAS,line_width,stroke_color,fill_color,stroke_opacity,fill_opacity,font_size,font_family,font_color,use_axis_numbering,double_data[0],int_data[0],precision));
                fprintf(js_include_file,"use_xlogscale=1;snap_y = %f;snap_x = xlogbase;",double_data[0]/int_data[0]);
                add_to_buffer(tmp_buffer);
                break;
              default:break;
            }
          }
          break;
        case XYLOGSCALE:
  /*
  @ xylogscale majorcolor,minorcolor
  @ the x/y-range are set using commands ''xrange xmin,xmax`` and ''yrange ymin,ymax``
  @ the linewidth is set using command ''linewidth int``
  @ the opacity of major / minor grid lines is set by command ''opacity [0-255],[0-255]``
  @ default logbase number = 10 ... when needed, set the logbase number with command ''xlogbase number`` and/or ''ylogbase number``
  @ the x/y- axis numbering is triggered by keyword ''axisnumbering``<ul><li>use commands ''xlabel some_text`` and/or ''ylabel some_text`` for text on axis: use command ''fontsize int`` to set the fontsize (default 12px)</li><li>use command ''fontfamily fnt_family_string`` to set the fonts for axis-numbering</li><li>use command ''fontcolor`` to set the color</li></ul>
  @ note: the complete canvas will be used for the ''log paper``
  @ note: userdrawings are done in the log paper, e.g. javascript:read_canvas() will return the real values
  @ note: command ''mouse color,fontsize`` will show the real values in the logpaper.<br>\
  @ note: when using something like ''yrange 0.0001,0.01``...combined with commands ''mouse color,fontsize`` and/or ''userdraw type,color``...<br> make sure the precision is set accordingly (eg command ''precision 10000``)
  @ note: in case of userdraw, the use of keyword ''userinput_xy`` may be handy !
  @ <b>attention</b>: keyword ''snaptogrid`` may not lead to the desired result...
  @ <b>attention</b>: do not use command ''zoom``
  @%xylogscale%size 400,400%xrange 10,50000%yrange 10,50000%xlabel x-axis%ylabel y-axis%xylogscale black,grey%display xy,red,22
  */
          use_axis_numbering++;if(use_axis_numbering > 1){use_axis_numbering = 1;}
          if( js_function[DRAW_GRID] == 1 ){canvas_error("only one type of grid is allowed...");}
          js_function[DRAW_XYLOGSCALE] = 1;
          for(i=0;i<2;i++){
            switch(i){
              case 0: stroke_color = get_color(infile,0); break;
              case 1: fill_color = get_color(infile,1);
                tmp_buffer=my_newmem(MAX_BUFFER);
                check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_grid%d(%d,%d,\"%s\",\"%s\",%.2f,%.2f,%d,\"%s\",\"%s\",%d,%d); ",canvas_root_id,GRID_CANVAS,line_width,stroke_color,fill_color,stroke_opacity,fill_opacity,font_size,font_family,font_color,use_axis_numbering,precision));
                fprintf(js_include_file,"use_xlogscale=1;use_ylogscale=1;snap_x = xlogbase;snap_y = ylogbase;");
                add_to_buffer(tmp_buffer);
                break;
              default:break;
            }
          }
          break;
        case Y_AXIS_STRINGS:
  /*
  @ yaxis num1:string1:num2:string2:num3:string3:num4:string4:....num_n:string_n
  @ alternative: yaxistext
  @ use command ''fontcolor``, ''fontfamily`` to adjust font <br>defaults: black,12,Arial<br> note: command ''fontsize`` is not active for this command.(''fontsize`` can be used for the <a href="#legend">legend</a> in a <a href="#grid">grid</a>)
  @ no need to use keyword <a href="#axisnumbering">axisnumbering</a>
  @ use command <a href="#axis">axis</a> to have visual x/y-axis lines (see command <a href="#grid">grid</a>
  @ use these y-axis num1...num_n values instead of default ymin...ymax
  @ a javascript error message will flag non-matching value:name pairs
  @ to be used before command grid (see <a href="#grid">command grid</a>)
  @%yaxistext%size 400,400%yrange 0,13%xrange -100,500%axis%yaxis 1:january:2:february:3:march:5:may:6:june:7:july:8:august:9:september:10:october:11:november:12:december%#'ymajor' steps should be synchronised with numbers eg. "1" in this example%grid 100,1,grey,4,1,6,grey
  */
          temp = get_string(infile,1);
          if( strstr(temp,":") != 0 ){ temp = str_replace(temp,":","\",\"");}
          if( strstr(temp,"pi") != 0 ){ temp = str_replace(temp,"pi","(3.1415927)");}/* we need to replace pi for javascript y-value*/
          fprintf(js_include_file,"y_strings = [\"%s\"];\n ",temp);
          use_axis_numbering++;
          break;
        case YERRORBARS:
  /*
  @ yerrorbars color,E1,E2,x1,y1,x2,y2,...,x_n,y_n
  @ draw multiple points with y-errorbars E1 (error value under point) and E2 (error value above point) at given coordinates in color 'color'
  @ the errors E1 and E2 values are in yrange.
  @ use command ''linewidth int`` to adust size
  @ may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually (!)
  @%yerrorbars%size 400,400%xrange -10,10%yrange -10,10%linewidth 2%onclick%yerrorbars red,0.8,1.3,0,0,1,1,2,3,3,2,4,5,5,2,6,1,-1,-2,-2,0,-3,2,-4,4,-5,-1
  */
      stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
      fill_color = stroke_color;
      i=0;
      while( ! done ){     /* get next item until EOL*/
        if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
        if(i%2 == 0 ){
            double_data[i] = get_real(infile,0); /* x */
        }
        else
        {
            double_data[i] = get_real(infile,1); /* y */
        }
        i++;
      }
      if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
      for(c = 2 ; c < i-1 ; c = c+2){
        tmp_buffer=my_newmem(MAX_BUFFER);
        check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "dragstuff.addShape(new Shape(%d,%d,%d,%d,19,[%.*f],[%.*f],[%.2f],[%.2f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+1],double_data[0],double_data[1],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,1,0,0,0,use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
        add_to_buffer(tmp_buffer);
    /* object_cnt++; */
        if(onclick != 0){object_cnt++;}
      }
      decimals = find_number_of_digits(precision);
      reset();
      dragstuff[19] = 1;
      if(use_dragstuff == 0 ){ use_dragstuff = 1;}
      break;
        case YOFFSET:
  /*
  @ yoffset
  @ keyword; to place the text centered above the text coordinates(x:y) ...
  @ may be used for points or other things requiring centered labels
  @ use <a href="#fontfamily">fontfamily</a> for setting the font
  @ may be active for commands <a href="#text">text</a> and <a href="#string">string</a> (e.g. objects in the drag/drop/onclick-library)
  @%yoffset%size 400,400%xrange -10,10%yrange -10,10%fontfamily 12pt Arial%string blue,-9,-9,no offset%point -9,-9,red%centered%string blue,-6,-6,centered%point -6,-6,red%xoffset%string blue,-3,-3,xoffset%point -3,-3,red%yoffset%string blue,0,0,yoffset%point 0,0,red%xyoffset%string blue,3,3,xyoffset%point 3,3,red%resetoffset%string blue,6,6,resetoffset%point 6,6,red
  */
          use_offset = 1;
          break;
        case YRANGE:
  /*
  @ yrange ymin,ymax
  @ alternative: rangey
  @ if not given 0,ysize (eg in pixels)
  */
          for(i = 0 ; i<2; i++){
            switch(i){
              case 0: ymin = get_real(infile,0);break;
              case 1: ymax = get_real(infile,1);break;
              default: break;
            }
          }
          if(ymin >= ymax){canvas_error(" yrange is not OK: ymin &lt; ymax !<br>");}
          fprintf(js_include_file,"var ymin = %f;var ymax = %f;",ymin,ymax);
          found_size_command++;
          break;
        case YSNAPTOGRID:
  /*
  @ ysnaptogrid
  @ keyword (no arguments required)
  @ a draggable object (use command ''drag x|y|xy``) will snap to the given y-grid values when dragged (mouseup)
  @ in case of userdraw the drawn points will snap to ymajor grid
  @ if no grid is defined, points will snap to every integer yrange value. (eg snap_y=1)
  @ if you do not want a visible grid, but you only want a ''snaptogrid`` with some value...define this grid with opacity 0.
  @ if yminor is defined (use keyword ''axis`` to activate yminor), the drawing will snap to yminor <br>use only even dividers in y-minor...for example<br><code>ysnaptogrid<br>axis<br>grid 2,1,grey,4,4,7,red</code><br> will snap on x=0, x=0.5, x=1, x=1.5 ....<br> will snap on y=0, y=0.25 y=0.5 y=0.75 ...<br>
  @%ysnaptogrid_1%size 400,400%xrange -10,10%yrange -10,10%ysnaptogrid%grid 1,1,grey%linewidth 2%userdraw crosshairs,blue%css font-size:8px;color:blue%clearbutton delete all crosshairs
  @%ysnaptogrid_2%size 400,400%xrange -10,10%yrange -10,10%ysnaptogrid%grid 1,1,grey%linewidth 3%drag y%points red,0,0,0,0,0,0,0,0,0,0
  */
          use_snap = 3;
          break;
        case YLABEL:
  /*
  @ ylabel some_string
  @ will be used to create a (vertical) label for the y-axis (label is in quadrant I)
  @ can only be used together with command <a href="#grid">grid</a><br>not depending on keywords ''axis`` and ''axisnumbering``
  @ font setting: italic Courier, fontsize will be slightly larger (fontsize + 4)<br>use command ''fontsize`` to adjust (command ''fontsize`` is not active for this command)
  @ use <a href="#xlabel">xlabel</a>
  @%ylabel%size 400,400%xrange -10,10%yrange -10,10%fontsize 8%axis%axisnumbering%precision 1%xlabel x-axis%ylabel y-axis%grid 1,1,grey,2,2,2,red
  */
          temp = get_string(infile,1);
          fprintf(js_include_file,"var yaxislabel = \"%s\";",temp);
          break;
        case YLOGBASE:
  /*
  @ ylogbase number
  @ sets the logbase number for the y-axis
  @ default value 10
  @ use together with commands ylogscale / xylogscale
  */
          fprintf(js_include_file,"ylogbase=%d;",(int)(get_real(infile,1)));
          break;
        case YLOGSCALE:
  /*
  @ ylogscale xmajor,xminor,majorcolor,minorcolor
  @ the x/y-range are set using commands ''xrange xmin,xmax`` and ''yrange ymin,ymax``
  @ xmajor is the major step on the x-axis; xminor is the divisor for the x-step
  @ the linewidth is set using command ''linewidth int``
  @ the opacity of major / minor grid lines is set by command ''opacity [0-255],[0-255]``
  @ default logbase number = 10 ... when needed, set the logbase number with command ''ylogbase number``
  @ the x/y- axis numbering is triggered by keyword ''axisnumbering``<ul><li>use command ''precision`` before ''ylogscale`` command to set the precision (decimals) of the axis numbering</li><li>use commands ''xlabel some_text`` and/or ''ylabel some_text`` for text on axis: use command ''fontsize int`` to set the fontsize (default 12px)</li><li>use command ''fontfamily fnt_family_string`` to set the fonts for axis-numbering</li><li>use command ''fontcolor`` to set the color</li></ul>
  @ note: the complete canvas will be used for the ''log paper``
  @ note: userdrawings are done in the log paper, e.g. javascript:read_canvas() will return the real values
  @ note: command ''mouse color,fontsize`` will show the real values in the logpaper.<br>\
  @ note: when using something like ''yrange 0.0001,0.01``...combined with commands ''mouse color,fontsize`` and/or ''userdraw type,color``...<br> make sure the precision is set accordingly (eg command ''precision 10000``)
  @ note: in case of userdraw, the use of keyword ''userinput_xy`` may be handy !
  @ <b>attention</b>: do not use command ''zoom``
  @ <b>attention</b>: keyword ''snaptogrid`` may not lead to the desired result...
  */
        use_axis_numbering++;if(use_axis_numbering > 1){use_axis_numbering = 1;}
        if( js_function[DRAW_GRID] == 1 ){canvas_error("only one type of grid is allowed...");}
        js_function[DRAW_YLOGSCALE] = 1;
        for(i=0;i<4;i++){
          switch(i){
            case 0: double_data[0] = get_real(infile,0);break; /* xmajor */
            case 1: int_data[0] = (int) (get_real(infile,0));break; /* xminor */
            case 2: stroke_color = get_color(infile,0); break;
            case 3: fill_color = get_color(infile,1);
              tmp_buffer=my_newmem(MAX_BUFFER);
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER, "draw_grid%d(%d,%d,\"%s\",\"%s\",%.2f,%.2f,%d,\"%s\",\"%s\",%d,%f,%d,%d); ",canvas_root_id,GRID_CANVAS,line_width,stroke_color,fill_color,stroke_opacity,fill_opacity,font_size,font_family,font_color,use_axis_numbering,double_data[0],int_data[0],precision));
              fprintf(js_include_file,"use_ylogscale=1;snap_x = %f;snap_y = ylogbase;",double_data[0]/int_data[0]);
              add_to_buffer(tmp_buffer);
               break;
            default:break;
            }
          }
          break;
        case YUNIT:
  /*
  @ yunit some_unit_for_y-values
  @ unicode allowed (no html code)
  @ use together with command mousey
  @ will display the cursor y-coordinate in ''unit``
  */
          fprintf(js_include_file,"unit_y = \"%s\";",get_string(infile,1));
          break;
        case HYPSEGMENTS:
  /*
  @ hypsegments color,x1,y1,x2,y2,...,x_n,y_n
  @ draw multiple hyperbolic segments in the Poincaré disk between points (x1:y1)--(x2:y2).....and... (x_n-1:y_n-1)--(x_n:y_n) in color ''color``
  @ use command ''linewidth int`` to adjust size may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a> individually (!)
  @%hypsegments%size 400,400%xrange -1,1%yrange -1,1%circles black,0,0,1%linewidth 2%hypsegments green,0.1,-0.3,0.6,0.7,0.3,0,-0.8,0.6,0.6,0.5,-0.5,0.6
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
              if(i%2 == 0 ){
                double_data[i] = get_real(infile,0); /* x */
              }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if(use_moebius == TRUE ){ moebius(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          decimals = find_number_of_digits(precision);
          for(c = 0 ; c+3 < i ; c = c+4){
            tmp_buffer=my_newmem(MAX_BUFFER);
            if (hypgeodaux(double_data+c,res,0)){
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,12,[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,res[0],decimals,res[0],decimals,res[1],decimals,res[1],decimals,res[2],decimals,res[3],decimals,res[4],decimals,res[5],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            }
            else {
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+2],decimals,double_data[c+1],decimals,double_data[c+3],line_width,stroke_color,stroke_opacity,stroke_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            }
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
          }
          reset();
          dragstuff[4] = 1;dragstuff[12] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case HYPLINES:
  /*
  @ hyplines color,x1,y1,x2,y2...x_n-1,y_n-1,x_n,y_n
  @ draw multiple hyperbolic lines in the Poincaré disk through points (x1:y1)--(x2:y2) ...(x_n-1:y_n-1)--(x_n:y_n) in color 'color' may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
  @%hyplines%size 400,400%xrange -1,1%yrange -1,1%circles black,0,0,1%hyplines green,0,0.1,0.1,0.3,0.2,0.3,0.1,0.3,0,0.5,-0.5,0.4
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if(use_moebius == TRUE ){ moebius(i-1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
          decimals = find_number_of_digits(precision);
          for(c = 0 ; c+3 < i ; c = c+4){
            tmp_buffer=my_newmem(MAX_BUFFER);
            if (hypgeodaux(double_data+c,res,1)){
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,12,[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,res[0],decimals,res[0],decimals,res[1],decimals,res[1],decimals,res[2],decimals,res[3],decimals,res[4],decimals,res[5],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));}
            else {
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,res[0],decimals,res[2],decimals,res[1],decimals,res[3],line_width,stroke_color,stroke_opacity,fill_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            }
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}
          }
          reset();
          dragstuff[4] = 1;dragstuff[12] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case HYPPOLY:
    /*
    @ hyppolygon color,x1,y1,x2,y2...x_n-1,y_n-1,x_n,y_n
    @ draw hyperbolic polygon in the Poincaré disk through points (x1:y1)--(x2:y2) ...(x_n-1:y_n-1)--(x_n:y_n) -- (x1:y1)  in color 'color' may be set <a href="#drag">draggable</a> / <a href="#onclick">onclick</a>
    @ option fhyppolygon only for convex polygon for the moment
    @%hyppolygon%size 400,400%xrange -1,1%yrange -1,1%circles black,0,0,1%hyppolygon green,0,0,0.5,0.3,0.8,-0.1,0.4,-0.5
    */
          stroke_color=get_color(infile,0);
          fill_color = stroke_color;
          i=0;
          while( ! done ){     /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            i++;
          }
          /* il faut rajouter le premier point à la fin*/
          if( i%2 != 0 ){canvas_error("in command hyppolygon: unpaired x or y value");}
          if( i > MAX_INT - 1 ){canvas_error("too many points in argument: repeat command multiple times to fit");}
          double_data[i]=double_data[0]; double_data[i+1]=double_data[1];
          if(use_rotate == TRUE ){rotate(i+1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){transform(i+1,2);}
          if(use_moebius == TRUE ){ moebius(i+1,2);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          decimals = find_number_of_digits(precision);

          char tmp[100];
          char* x=my_newmem(MAX_BUFFER);strcpy(x,"[");
          char* y=my_newmem(MAX_BUFFER);strcpy(y,"[");
          char* w=my_newmem(MAX_BUFFER);strcpy(w,"[");
          char* h=my_newmem(MAX_BUFFER);strcpy(h,"[");
          tmp_buffer=my_newmem(MAX_BUFFER);
          for(c = 0 ; c < i-1 ; c = c+2)
            if (hypgeodaux(double_data+c,res,0)) {
              sprintf(tmp,"%.*f,",decimals,res[0]);
              strcat(x,tmp);
              sprintf(tmp,"%.*f,",decimals,res[1]);
              strcat(y,tmp);
              strcat(w,"0,");
              sprintf(tmp,"[%.*f,%.*f,%.*f],", decimals,res[2],decimals,res[4],decimals,res[5]);
              strcat(h,tmp);
            }
            else {
              sprintf(tmp,"%.*f,",decimals,double_data[c]);
              strcat(x,tmp);
              sprintf(tmp,"%.*f,",decimals,double_data[c+1]);
              strcat(y,tmp);
              strcat(w,"1,");
              sprintf(tmp,"[%.*f,%.*f],",decimals,double_data[c+2],decimals,double_data[c+3]);
              strcat(h,tmp);
            }
          x[strlen(x)-1]=0;strcat(x,"]");
          y[strlen(y)-1]=0;strcat(y,"]");
          w[strlen(w)-1]=0;strcat(w,"]");
          h[strlen(h)-1]=0;strcat(h,"]");
          check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,27,%s,%s,%s,%s,%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,x,y,w,h,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
          free(x);free(y);free(w);free(h);
          dragstuff[27]=1;
          add_to_buffer(tmp_buffer);
          if(onclick != 0){object_cnt++;}
          dragstuff[4] = 1; dragstuff[12] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          reset();
          break;
        case HYPRAYS:
  /*
  @ hyprays color,xc,yc,x1,y1,x2,y2,x3,y3...x_n,y_n
  @ draw hyperbolic rays in the Poincaré disk in color 'color' and center (xc:yc) may be set draggable or onclick (every individual ray)
  @%hyprays%size 400,400%xrange -1,1%yrange -1,1%circles black,0,0,1%onclick%hyprays blue,0.5,0.5,0.3,0.9,-0.3,0.5,-0.4,0,0.4,-0.9,-0.8,0.1,-0.1,-0.9
  */
  /*
   hyprays_onclick%size 400,400%xrange -1,1%yrange -1,1%circles black,0,0,1%onclick%hyprays blue,0.5,0.5,0.3,0.9,-0.3,0.5,-0.4,0,0.4,-0.9,-0.8,0.1,-0.1,-0.9
   hyprays_drag_xy%size 400,400%xrange -1,1%yrange -1,1%circles black,0,0,1%drag xy%hyprays blue,0,0,0.3,0.9,-0.3,0.5,-0.4,0,0.4,-0.9,-0.8,0.1,-0.1,-0.9
  */
          stroke_color=get_color(infile,0);
          if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
          double_data[0] = get_real(infile,0);/* xc */
          double_data[1] = get_real(infile,0);/* yc */
          i=2;
          while( ! done ){ /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("in command rays too many points / rays in argument: repeat command multiple times to fit");}
            if(i%2 == 0 ){
              double_data[i] = get_real(infile,0); /* x */
            }
            else {
              double_data[i] = get_real(infile,1); /* y */
            }
            fprintf(js_include_file,"/* double_data[%d] = %f */\n",i,double_data[i]);
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,2);}
          if(use_affine == TRUE ){ transform(i-1,2);}
          if(use_moebius == TRUE ){ moebius(i-1,2);}
          if( i%2 != 0 ){canvas_error("in command rays: unpaired x or y value");}
          decimals = find_number_of_digits(precision);
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          for(c=2; c<i;c = c+2){
            tmp_buffer=my_newmem(MAX_BUFFER);
            double_data[2]=double_data[c];double_data[3]=double_data[c+1];
            if (hypgeodaux(double_data,res,0)){
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,12,[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],[%.*f,%.*f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,res[0],decimals,res[0],decimals,res[1],decimals,res[1],decimals,res[2],decimals,res[3],decimals,res[4],decimals,res[5],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            }
            else {
              check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,4,[%.*f,%.*f],[%.*f,%.*f],[30,30],[30,30],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[0],decimals,double_data[c],decimals,double_data[1],decimals,double_data[c+1],line_width,stroke_color,stroke_opacity,fill_color,stroke_opacity,0,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            }
            add_to_buffer(tmp_buffer);
            /* object_cnt++; */
            if(onclick != 0){object_cnt++;}
          }
          reset();
          dragstuff[4] = 1; dragstuff[12] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case HYPCIRCLES:
        /*
  @ hypcircles color,xc1,yc1,r1,xc2,yc2,r2...xc_n,yc_n,r_n
  @ draw hyperbolic circles in Poincaré disk
  @ <b>attention</b> r = radius in x-range (!)
  @ use keyword <code>filled</code> or command <code>fhypcircles</code> to produce solid circles
  @ use command <code>fillcolor color</code> to set the fillcolor
  @%hypcircles%size 400,400%xrange -1,1%yrange -1,1%filled%fillcolor lightblue%opacity 255,50%hypcircles blue,0,0,1,0.4,0.4,3,0.2,0.6,6
  */
  /*
  hypcircles_drag%size 400,400%xrange -1,1%yrange -1,1%filled%fillcolor lightblue%opacity 255,50%drag xy%hypcircles blue,0,0,2,0.2,0.2,3,0.2,0.2,5%zoom red
  hypcircles_onclick%size 400,400%xrange -1,1%yrange -1,1%filled%fillcolor lightblue%opacity 255,50%onclick%hypcircles blue,0,0,2,0.2,0.2,3,0.2,0.2,5%zoom red
  hypcircles_drag_slider%size 400,400%xrange -1,1%yrange -1,1%linewidth 2%drag xy%# Click circles(s) to activate%opacity 200,50%fillcolor orange%rotationcenter 2,3%slider 0,2*pi,300,30,angle degrees,Rotate%slider -5,5*pi,300,30,x display,move in x-direction%slider -10,10*pi,300,30,y display,move in y-direction%hypcircles blue,0,0,2,0.2,0.2,3,0.2,0.2,5
  */
          stroke_color=get_color(infile,0); /* how nice: now the color comes first...*/
          if(fillcolor) {fill_color=fillcolor;} else {fill_color=stroke_color;}
          i=1;
          while( ! done ){ /* get next item until EOL*/
            if(i > MAX_INT - 1){canvas_error("too many points in argument: repeat command multiple times to fit");}
              switch (i%3){
                case 1:double_data[i-1] = get_real(infile,0);break; /* x */
                case 2:double_data[i-1] = get_real(infile,0);break; /* y */
                case 0:double_data[i-1] = get_real(infile,1);break; /* r */
              }
            i++;
          }
          if(use_rotate == TRUE ){rotate(i-1,angle,rotationcenter,3);}
          if(use_affine == TRUE ){ transform(i-1,3);}
          if(use_moebius == TRUE ){ moebius(i-1,3);}
          if( use_slider != -1 && onclick == 0){ onclick = 3; }/* no drag&onclick but slideable */
          decimals = find_number_of_digits(precision);
          for(c = 0 ; c < i-1 ; c = c+3){
            double Z2=double_data[c]*double_data[c]+double_data[c+1]*double_data[c+1];//[Z]^2
            double R=tanh(double_data[c+2]/2);double R2=R*R;
            double den=1-R2*Z2; double XY=(1-R2)/den;
            double_data[c]=XY*double_data[c];
            double_data[c+1]=XY*double_data[c+1];
            double_data[c+2]=(1-Z2)/den*R;
            tmp_buffer=my_newmem(MAX_BUFFER);
            check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"dragstuff.addShape(new Shape(%d,%d,%d,%d,13,[%.*f],[%.*f],[%.3f],[%.3f],%d,\"%s\",%.2f,\"%s\",%.2f,%d,%d,%d,%d,%d,%.1f,\"%s\",%d,\"%s\",%s,%s,%d));\n",drag_type,object_cnt,onclick,use_snap,decimals,double_data[c],decimals,double_data[c+1],double_data[c+2],double_data[c+2],line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype[0],dashtype[1],use_rotate,angle,flytext,font_size,font_family,my_sliders,rotation_center,use_offset));
            add_to_buffer(tmp_buffer);
            if(onclick != 0){object_cnt++;}/* object_cnt++; */
          }
          reset();
          dragstuff[13] = 1;
          if(use_dragstuff == 0 ){ use_dragstuff = 1; }
          break;
        case ZOOM:
  /*
  @ zoom button_color
  @ introduce a very small ''controlpanel`` at the lower right corner (font size of the panel is fixed to: 22px Arial)
  @ giving six 15&times;15px ''active`` rectangle areas<br>(''&times;,&darr;,&uarr;,&larr;,&rarr;,&minus;,+``) for zooming and/or panning of the image
  @ a mouse wheel is active for in/out zooming. Drag panning is not supported (this will conflict with many ''userdraw`` or ''multidraw`` primitives)
  @ the ''controlpanel`` is not active for a ''userdraw`` mousedown (but it can interfere with some ''userdraw`` types)
  @ the ''&times;`` symbol will reset to your original xmax/xmin ymax/ymin values.
  @ choose an appropriate color, so the small ''&times;,&darr;,&uarr;,&larr;,&rarr;,&minus;,+`` are clearly visible
  @ command ''opacity`` may be used to set stroke_opacity of buttons
  @ note: on zooming, text will not increase / decrease the font size (todo??)
  @ note: adding ''zooming`` will increase the size of the javascript include with approx. 11 kb
  */
         js_function[INTERACTIVE] = 1;
          js_function[JS_ZOOM] = 1;
          if( use_userdraw == 1 ){
            js_function[USERDRAW_AND_ZOOM] = 1;
            fprintf(js_include_file,"forbidden_zone = [%d,%d];",xsize-115,ysize - 20);
          }
          if(jsplot_cnt != -1){ js_function[JSPLOT_AND_ZOOM] = 1;}
          stroke_color = get_color(infile,1);
          /* we use BG_CANVAS (0) */
          add_js_zoom_buttons(stroke_color,stroke_opacity);
          done = TRUE;
          break;

/* ready */
        default:sync_input(infile);
        break;
      }
    }
  /* we are done parsing script file */
    if(use_dragstuff != 0){
  /* add the 20kb drag code: nearly always used ... use_dragstuff==1: no-mouse ! */
      add_drag_code(DRAG_CANVAS,use_dragstuff,dragstuff,reply_format);
      if(js_function[JS_ZOOM] == 1){
        js_function[DRAG_AND_ZOOM] = 1;
      }
  }

  /* check if xrange / yrange was set explicit ... or use xmin=0 xmax=xsize ymin=0 ymax=ysize: Quadrant I */
    if( found_size_command == 1 ){
      fprintf(js_include_file,"var xmin = 0;var xmax = %d;var ymin = 0;var ymax = %d",xsize,ysize);
    }
    else
    {
      if( found_size_command != 3 ){
        canvas_error("Please specify size first and then both xrange and yrange ...");
      }
    }

  /* if needed, add generic draw functions (grid / xml etc) to buffer: these are no draggable/clickable shapes / objects ! */
    add_javascript_function();
   /* add read_canvas() etc functions if needed */
    if( reply_format > 0 ){ add_read_canvas(reply_format,reply_precision);}
  /* no zoom, just add buffer */
    fprintf(js_include_file,"\n/* add buffer */\n%s};\n/* end wims_canvas_function */\nwims_canvas_function%d();\n",buffer,canvas_root_id);
/* done writing the javascript include file */
    fclose(js_include_file);
    }

/* if using a tooltip, this should always be printed to the *.phtml file, so stdout */
    if( use_tooltip > 0 ){
      if( use_tooltip == 1 ){
        add_js_tooltip(tooltip_text,bgcolor);
      }
      else
      {
      if( use_tooltip == 2 ){
        add_js_popup(getfile_cmd);
      }
    }
  }
  exit(EXIT_SUCCESS);
}
/* end main() */

/******************************************************************************
**
**  sync_input
**
**  synchronises input line - reads to end of line, leaving file pointer
**  at first character of next line.
**
**  Used by:
**  main program - error handling.
**
******************************************************************************/
void sync_input(FILE *infile)
{
  int c = 0;

  if( c == '\n' || c == ';' ) return;
  while( ( (c=getc(infile)) != EOF ) && (c != '\n') && (c != '\r') && (c != ';')) ;
  if( c == EOF ) finished = 1;
  if( c == '\n' || c == '\r' || c == ';') line_number++;
  return;
}

/******************************************************************************/
void transform(int num,int incr){
/*.
only "double_data[]" is used for transformations !!
*/
 int i;int ii;double x,y;
 for(i=0;i<num;i = i+incr){
  ii = i+1;
  x = double_data[i]*affine_matrix[0] + double_data[ii]*affine_matrix[1]+affine_matrix[4];
  y = double_data[i]*affine_matrix[2] + double_data[ii]*affine_matrix[3]+affine_matrix[5];
 /*
  printf("(%f:%f) &rarr; (%f:%f)<br>",double_data[i],double_data[ii],x,y);
 */
  double_data[i] = x;
  double_data[ii] = y;
 }
}

void rotate(int num,double angle,double center[],int incr){
 int i;int ii;double rad = angle * 0.0174532925199;
 double c = cos(rad);
 double s = sin(rad);
 double x,y;
 for(i=0;i<num;i = i+incr){
  ii = i+1;
  x = c*(double_data[i]-center[0]) + s*(double_data[ii] - center[1] ) + center[0];
  y = c*(double_data[ii]-center[1]) - s*(double_data[i] - center[0] ) + center[1];
  double_data[i] = x;
  double_data[ii] = y;
 // printf("(x:y) - (%f:%f)<br>",x,y);
 }
}

void moebius(int num,int incr){
  int i;int ii;double x,y,den;
  double rad = moebius_matrix[2] * 0.0174532925199;
  double c = cos(rad); double s = sin(rad);
  for(i=0;i<num;i = i+incr){
    ii = i+1;
    double den=((moebius_matrix[0]*moebius_matrix[0] + moebius_matrix[1]*moebius_matrix[1])*double_data[i]*double_data[i] + 2*moebius_matrix[0]*double_data[i] + ((moebius_matrix[0]*moebius_matrix[0] + moebius_matrix[1]*moebius_matrix[1])*double_data[ii]*double_data[ii] + 2*moebius_matrix[1]*double_data[ii] + 1));
    x=(moebius_matrix[0]*double_data[i]*double_data[i] + (moebius_matrix[0]*moebius_matrix[0] + (-moebius_matrix[1]*moebius_matrix[1] + 1))*double_data[i] + (moebius_matrix[0]*double_data[ii]*double_data[ii] + 2*moebius_matrix[1]*moebius_matrix[0]*double_data[ii] + moebius_matrix[0]))/den;
    y=(moebius_matrix[1]*double_data[i]*double_data[i] + 2*moebius_matrix[1]*moebius_matrix[0]*double_data[i] + (moebius_matrix[1]*double_data[ii]*double_data[ii] + (-moebius_matrix[0]*moebius_matrix[0] + (moebius_matrix[1]*moebius_matrix[1] + 1))*double_data[ii] + moebius_matrix[1]))/den;
    double_data[i] = c*x-s*y;
    double_data[ii]= s*x+c*y;
  }
}
/* not used: see transform()
void translate(int num){
 int i;int ii;
 double x,y;
 for(i=0;i<num;i = i+2){
  ii = i+1;
  x = double_data[i] + affine_matrix[4];
  y = double_data[ii] + affine_matrix[5];
  double_data[i] = x;
  double_data[ii] = y;
 }
}
*/
char *str_replace(const char *str, const char *old, const char *new){
  if(strlen(str) > MAX_BUFFER){canvas_error("string argument too big");}
  char *ret, *r;
  const char *p, *q;
  size_t oldlen = strlen(old);
  size_t count = 0;
  size_t retlen = 0;
  size_t newlen = strlen(new);
  if (oldlen != newlen){
    for (count = 0, p = str; (q = strstr(p, old)) != NULL; p = q + oldlen){
      count++;
      retlen = p - str + strlen(p) + count * (newlen - oldlen);
    }
  }
  else {
    retlen = strlen(str);
  }
  if ((ret = malloc(retlen + 1)) == NULL){
    ret = NULL;
    canvas_error("string argument is NULL");
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
  return ret;
}

/******************************************************************************/

char *get_color(FILE *infile , int last){
  int c,i = 0,is_hex = 0;
  char temp[MAX_COLOR_STRING], *string;
  const char *not_allowed = "0123456789";
  while(( (c=getc(infile)) != EOF ) && ( c != '\n') && ( c != ',' ) && ( c != ';' )  && ( c != '\t' ) ){
    if( i > MAX_COLOR_STRING ){ canvas_error("colour string is too big ... ? ");}
    if( c == '#' ){
      is_hex = 1;
    }
    if( c != ' '){
      if( is_hex == 0 ){if(strchr(not_allowed,c) != 0){canvas_error("found something like a number...but is should have been a colour or #hex color number...<br>Do not use R,G,B !!! ");}}
      temp[i]=tolower(c);
      i++;
    }
  }
  if( ( c == '\n' || c == EOF || c == ';' || c == '\t' ) && last == 0){canvas_error("expecting more arguments in command");}
  if( c == '\n' || c == ';'  || c == '\t' ){ done = TRUE; line_number++; }
  if( c == EOF ){finished = 1;}
  if( finished == 1 && last != 1 ){ canvas_error("expected more arguments");}
  temp[i]='\0';
  if( strlen(temp) == 0 ){ canvas_error("expected a colorname or hexnumber, but found nothing !!");}
  if( is_hex == 1 ){
    char red[3], green[3], blue[3];
    red[0]   = toupper(temp[1]); red[1]   = toupper(temp[2]); red[2]   = '\0';
    green[0] = toupper(temp[3]); green[1] = toupper(temp[4]); green[2] = '\0';
    blue[0]  = toupper(temp[5]); blue[1]  = toupper(temp[6]); blue[2]  = '\0';
    int r = (int) strtol(red,   NULL, 16);
    int g = (int) strtol(green, NULL, 16);
    int b = (int) strtol(blue,  NULL, 16);
    int L0 = 1+snprintf(NULL,0,"%d,%d,%d",r,g,b);
    string = my_newmem(L0);
    snprintf(string,L0,"%d,%d,%d",r,g,b);
    return string;
  }
  else
  {
    string = (char *)my_newmem(sizeof(temp));
    snprintf(string,sizeof(temp),"%s",temp);
    for( i = 0; i < NUMBER_OF_COLORNAMES ; i++ ){
      if( strcmp( colors[i].name , string ) == 0 ){
        return colors[i].rgb;
      }
    }
    canvas_error("I was expecting a color name or hexnumber...but found nothing.");
  }
  return "0,0,255";
}

char *get_string(FILE *infile,int last){ /* last = 0: more arguments ; last=1 final argument */
  int c,i=0;
  char  temp[MAX_BUFFER], *string;
  while(( (c=getc(infile)) != EOF ) && ( c != '\n') && ( c != '\t') ){
    temp[i]=c;
    i++;
    if(i > MAX_BUFFER){ canvas_error("string size too big...repeat command to fit string");break;}
  }
  if( ( c == '\n' ||  c == '\t'  || c == EOF ) && last == 0){
    canvas_error("expecting more arguments in command");}
  if( c == '\n' ||  c == '\t') { done = TRUE; line_number++; }
  if( c == EOF ) {finished = 1;}
  temp[i]='\0';
  if( strlen(temp) == 0 && last != 3 ){
    canvas_error("expected a word or string, but found nothing!");}
  string=(char *)my_newmem(strlen(temp));
  snprintf(string,sizeof(temp),"%s",temp);
  return string;
}

char *get_string_argument(FILE *infile,int last){  /* last = 0: more arguments ; last=1 final argument */
  int c,i=0;
  char temp[MAX_BUFFER], *string;
  while(( (c=getc(infile)) != EOF ) && ( c != '\n') && ( c != '\t') && ( c != ',')){
    temp[i]=c;
    i++;
    if(i > MAX_BUFFER){ canvas_error("string size too big...will cut it off");break;}
  }
  if( ( c == '\n' || c == EOF) && last == 0){canvas_error("expecting more arguments in command");}
  if( c == '\n' || c == '\t' ) { line_number++; }
  if( c == EOF ) {finished = 1;}
  if( finished == 1 && last == 0 ){ canvas_error("expected more arguments");}
  temp[i]='\0';
/*
  17.10.2014 removed (question Perrin)
  may cause some unwanted effects...
  if( strlen(temp) == 0 ){ canvas_error("expected a word or string (without comma), but found nothing !!");}
*/
  string=(char *)my_newmem(sizeof(temp));
  snprintf(string,sizeof(temp),"%s",temp);
  done = TRUE;
  return string;
}

double get_real(FILE *infile, int last){ /* accept anything that looks like an number ?  last = 0: more arguments ; last=1 final argument */
  int c,i=0,found_calc = 0;
  double y;
  char tmp[MAX_INT];
  /*
   these things are 'allowed functions': *,^,+,-,/,(,),e,arc,cos,tan,pi,log,ln,sqrt,abs
   but there should be a better way to avoid segfaults !
  */
  const char *allowed = "earcostanpilogqb*+-/^()";/* assuming these are allowed stuff in a 'number'*/
  const char *not_allowed = "#dfhjkmuvwxyz{}[]%&~!$";/* avoid segmentation faults in a "atof()" and "wims eval" */
  while(( (c=getc(infile)) != EOF ) && ( c != ',') && (c != '\n') && (c != '\t') && ( c != ';')){
    if( c != ' ' ){
      if( i == 0 &&  c == '+' ){
        continue;
      }
      else
      {
        c = tolower(c);
        if( strchr(not_allowed,c) != 0 ){canvas_error("found a character not associated with a number...");}
        if( strchr(allowed,c) != 0 ){found_calc = 1;}/* hand the string over to wims eval() */
        tmp[i] = c;
        i++;
      }
    }
    if( i > MAX_INT - 1){canvas_error("number too large");}
  }
  if( ( c == '\n' || c == EOF || c == ';' || c == '\t' ) && last == 0){canvas_error("expecting more arguments in command");}
  if( c == '\n' || c == ';' || c == '\t' ){ done = TRUE; line_number++; }
  if( c == EOF ){done = TRUE ; finished = 1;}
  tmp[i]='\0';
  if( strlen(tmp) == 0 ){canvas_error("expected a number, but found nothing !!");}
  if( found_calc == 1 ){ /* use wims eval to calculate 2*pi/3 */
    void *f = eval_create(tmp);
    assert(f);if( f == NULL ){canvas_error("I'm having trouble parsing your \"expression\" ") ;}
    y = eval_x(f, 1);
    /* if function is bogus; y = 1: so no core dumps */
    eval_destroy(f);
  }
  else
  {
   y = atof(tmp);
  }
  return y;
}
void canvas_error(char *msg){
  fprintf(stdout,"\n</script><hr><span style=\"color:red\">FATAL syntax error: line %d: %s</span><hr>",line_number,msg);
  finished = 1;
  exit(EXIT_SUCCESS);
}
/* convert x/y coordinates to pixel */
int x2px(double x){
  int res=(x-xmin)/(xmax-xmin)*xsize;
  return (res==xsize)?xsize-1:res;
}
int y2px(double y){
  int res=(ymax-y)/(ymax-ymin)*ysize;
  return (res==ysize)?ysize-1:res;
}
double px2x(int x){
  return (x*(xmax - xmin)/xsize + xmin);
}
double px2y(int y){
  return (y*(ymax - ymin)/ysize + ymin);
}
void add_to_buffer(char *tmp){
//fprintf(stdout,"tmp = %s<br>buffer = %s<br>",tmp,buffer);
  if( tmp == NULL || tmp == 0 ){ canvas_error("nothing to add_to_buffer()...");}
  /*  do we have enough space left in buffer[MAX_BUFFER] ? */
  int space_left = (int) (sizeof(buffer) - strlen(buffer));
  if( space_left > strlen(tmp)){
    strncat(buffer,tmp,space_left - 1);/* add safely "tmp" to the string buffer */
  }
  else
  {
    canvas_error("your memory buffer is too big<br>simplify your script...it produces too many lines ");
  }
  free(tmp);
  return;
}
void reset(){
  if(no_reset == FALSE){ /* 8/5/2020 */
    use_filled = FALSE;
    use_dashed = FALSE;
    if(onclick != 4 ){onclick = 0;} /* slider param 'active'... onclick=4 */
    drag_type = -1;
    use_offset = 0;
    fillcolor = FALSE;
  }
}
char *getMML(char *tex){
  int my_pipe[2];pid_t pid;
  if(pipe(my_pipe)){canvas_error("mathml(): pipe() failure.\n");}
  pid = fork();
  if (pid == (pid_t) 0){
    char *argv[]={"wims_mathml","--use-zoom","0","--tex-size 100%","--max-mml-size","1024","--tex-string",tex,NULL};
    close(my_pipe[0]);dup2(my_pipe[1], 1);dup2(my_pipe[1], 2);close(my_pipe[1]);
    execv("../bin/wims_mathml",argv);canvas_error("could not execute wims_mathml\n");
  }
  else
  {
    if (pid < (pid_t) 0){
      close(my_pipe[0]);close(my_pipe[1]);canvas_error("mathml(): fork() failure.\n");
    }
    int status;FILE *stream;close(my_pipe[1]);stream = fdopen (my_pipe[0], "r");
    char buffer[MAX_BUFFER+1];memset(buffer,'\0',MAX_BUFFER);
    fgets(buffer, MAX_BUFFER, stream);
    int L0 = 1 + snprintf(NULL,0,"%s", buffer);
    tex = my_newmem(L0);memset(tex,'\0',L0);
    snprintf(tex,L0,"%s",buffer);
    fclose (stream);close(my_pipe[0]);waitpid(pid, &status, 0);
  }
  return tex;
}

char *getSVGMOL(char *inputtype,char *keys){
  int idx;
  char *forbidden[] = {"-O","-H","-z","-L","-o","-m"};
  char *argv[1+strlen(keys)];
  argv[0] = "obabel"; argv[1] = "-i"; argv[2] = inputtype;
  idx = 3;int i;
  char *ptr = strtok(keys,",");
  while(ptr != NULL ){
    for(i = 0 ; i < 6; i++ ){if( strncmp(ptr,forbidden[i],2) == 0 ){return "NOT ALLOWED ARGUMENT";}}
    argv[idx] = ptr; idx++;
    if(idx > 18){canvas_error("too many arguments for obabel....see docs<br>");}
    ptr = strtok(NULL,",");
  }
  /* last arguments; no 'javascript', only 'svg to STDOUT' and 'NULL' */
  argv[idx] = "-xj";argv[idx+1] = "-o"; argv[idx+2] = "svg";argv[idx+3] = NULL;
  int link[2];
  pid_t pid;
  char buffer[MAX_BUFFER+1];
  memset(buffer,'\0',MAX_BUFFER);
  if (pipe(link)==-1){canvas_error("pipe");}
  if ((pid = fork()) == -1){canvas_error("fork");}

  char *svgmol = "error";
  int string_length = 0;

  if(pid == 0) {
    dup2 (link[1], STDOUT_FILENO);
    dup2(link[0],  STDERR_FILENO);/* remove annoying messages '1 molecule converted' */
    close(link[0]);
    close(link[1]);
    execvp("obabel",argv);
  } else {
    close(link[1]);
    read(link[0],buffer, sizeof(buffer));
    close(link[0]);
    /* need to remover newline from svg-string on freebsd */
    char *pch = strstr(buffer, "\n");
    while(pch != NULL){
      strncpy(pch, " ", 1);
      pch = strstr(buffer, "\n");
    }
    string_length = 1 + snprintf(NULL,0,"%s",buffer);
    svgmol= my_newmem(string_length);
    snprintf(svgmol,string_length,"%s",buffer);
    wait(NULL);
  }
  return svgmol;
}
/* GNU libmatheval library for evaluating mathematical functions. */
char *eval(int xsize,int ysize,char *fun,double xmin,double xmax,double ymin,double ymax,int plotsteps,int precision,double rotationcenter[]){
  void *f;
  double x;
  double y;
  double xorg;
  int xv;
  int i = 0;
  int xstep =(int)(xsize/plotsteps);
  if( xstep == 0 ){xstep = 1;}
  double a = (xmax - xmin)/xsize;
  f = eval_create(fun);
  assert (f);
  if( f == NULL ){canvas_error("I'm having trouble parsing your \"expression\" ") ;}
  /* we supply the true x/y values...draw_curve() will convert these (x:y) to pixels : used for pan/scale */
  double xydata[MAX_BUFFER+1];/* hmmm */
  int lim_ymin =(int)( ymin - 4*fabs(ymin));/* 19-4-2015 replacing "abs" by "fabs"*/
  int lim_ymax =(int)( ymax + 4*fabs(ymax));/* 19-4-2015 replacing "abs" by "fabs"*/
  double c = 1.0;double s = 1.0;
  if( use_rotate == TRUE ){s = sin(angle*0.0174533);c = cos(angle*0.0174533);}
  for ( xv = 0 ;xv < xsize ; xv = xv+xstep ){
    x = (double) (xv*a + xmin);
    xorg = x;
    if( i < MAX_BUFFER - 2){
      y = eval_x(f, x);
      if(y < lim_ymax && y > lim_ymin ){
        if( use_affine == TRUE ){
          x = x*affine_matrix[0] + y*affine_matrix[1]+affine_matrix[4];
          y = xorg*affine_matrix[2] + y*affine_matrix[3]+affine_matrix[5];
        }
        if( use_rotate == TRUE){
          x = (c * (x - rotationcenter[0])) + (s * (y - rotationcenter[1])) + rotationcenter[0];
          y = (c * (y - rotationcenter[1])) - (s * (xorg - rotationcenter[0])) + rotationcenter[1];
        }
        xydata[i++] = x;
        xydata[i++] = y;
      }
    }
    else
    {
      canvas_error("\nYour curve plotting produces too many data \n Use less plotsteps or some other means to reduce the amount of data... ");
    }
  }
  eval_destroy(f);
  return double_xy2js_array(xydata,i,find_number_of_digits(precision));
}
static int filtre (int a, int b, int c, int d){
  return ((a>0)||(b>0)||(c>0)||(d>0))&&((a<0)||(b<0)||(c<0)||(d<0));
}
/* plot a levelcurve
  modified 08/2025 bernardi */
char *eval_levelcurve(int xsize,int ysize,char *fun,double xmin,double xmax,double ymin,double ymax,int levelstep,int precision,double level){
  void *f = eval_create(fun);
  if( f == NULL ){canvas_error("I'm having trouble parsing your \"expression\" ") ;}
  int ptx=xsize/levelstep, pty=ysize/levelstep;
  double dx = (xmax - xmin)/ptx;
  double dy = (ymax - ymin)/pty;
  int sgns[(ptx+1)*(pty+1)];
  double x, y, diff, xydata[MAX_BUFFER+1];
  int i, j, k;
  for (j = 0; j <= ptx; ++j)
    for (k = 0; k <= pty; ++k){
      x = xmin + j*dx;
      y = ymin + k*dy;
      diff = eval_x_y(f,x,y) - level;
      sgns[k+j*(pty+1)] = (diff > 0) ? 1 : -(diff < 0);
    };
  i = 0;
  for (j = 0; j < ptx; ++j)
    for (k = 0; k < pty; ++k){
      int b = k+j*(pty+1);
      if(filtre(sgns[b],sgns[b+1],sgns[b+pty+1],sgns[b+pty+2])){
        if (i >= MAX_BUFFER)
          canvas_error("\nYour curve plotting produces too many data \n Use less plotsteps, decrease image size...\nor some other means to reduce the amount of data... ");
        xydata[i++] = xmin+j*dx;
        xydata[i++] = ymin+k*dy;
      }
    }
  eval_destroy(f);
  return double_xy2js_array(xydata,i,find_number_of_digits(precision));
}

/* plot parametric function */
char *eval_parametric(int xsize,int ysize,char *fun1,char* fun2,double xmin,
  double xmax,double ymin,double ymax,
  double tmin,double tmax,int plotsteps,int precision,double rotationcenter[]){
  void *fx;
  void *fy;
  double t;
  int i = 0;
  double tstep = (tmax-tmin)/plotsteps;
  if( tstep == 0 ){canvas_error("zero step for t variable : reduce plotsteps or inrease trange");}
  fx = eval_create(fun1);
  fy = eval_create(fun2);
  assert(fx);
  assert(fy);
  if( fx == NULL || fy == NULL ){canvas_error("I'm having trouble parsing your \"expression\" ") ;}
    /* we supply the true x/y values...draw_curve() will convert these (x:y) to pixels : used for pan/scale */
 double xydata[MAX_BUFFER+1];/* hmmm */
 double x; /* real x-values */
 double y; /* real y-values */
 double xorg;
 /*
 29/12/2020
 disabled to try and synchronise curve+affine behaviour in complex scripts produced by "elec, tool circuit" (BPR)
    int lim_ymin =(int)( ymin - 4*fabs(ymin));
    int lim_ymax =(int)( ymax + 4*fabs(ymax));
    */
  double c = 1.0;double s = 1.0;
  if( use_rotate == TRUE ){s = sin(angle*0.0174533);c = cos(angle*0.0174533);}
  for( t = tmin ;t <= tmax ; t = t + tstep ){
    if( i < MAX_BUFFER - 2 ){
      y = eval_t(fy, t);
  /*    if(y > lim_ymin && y < lim_ymax){*/
      x = eval_t(fx, t);
      if( x == x){ /* no NaN */
        xorg = x;
        if( use_affine == TRUE ){
          x = x*affine_matrix[0] + y*affine_matrix[1]+affine_matrix[4];
          y = xorg*affine_matrix[2] + y*affine_matrix[3]+affine_matrix[5];
        }
        if( use_rotate == TRUE){
          x = (c * (x - rotationcenter[0])) + (s * (y - rotationcenter[1])) + rotationcenter[0];
          y = (c * (y - rotationcenter[1])) - (s * (xorg - rotationcenter[0])) + rotationcenter[1];
        }
        xydata[i++] = x;
        xydata[i++] = y;
      }
  /*    } */
    }
    else
    {
      canvas_error("\nYour curve plotting produces too many data \n Use less plotsteps or some other means to reduce the amount of data... ");
    }
  }
  eval_destroy(fx);
  eval_destroy(fy);
  return double_xy2js_array(xydata,i,find_number_of_digits(precision));
}

char *double_xy2js_array(double xy[],int len,int decimals){
 /*
    1,2,3,4,5,6,7,8 --> [1,3,5,7],[2,4,6,8]
    int xy[] is already checked for errors or overflow in "get_real()"
    just to be sure we double check the size of "temp"
*/
  char temp[2*MAX_BUFFER], *string;
  char *tmp = my_newmem(16);/* <= 9999999999999999  */
  memset(temp,'\0',2*MAX_BUFFER);/* clear memory */
  int i;int space_left;
  temp[0] = '[';/* start js-array */
  for(i = 0; i < len; i = i + 2){ /*  x_points[] */
    if(i == len - 2){sprintf(tmp, "%.*f",decimals, xy[i]);}
    else {sprintf(tmp, "%.*f,",decimals,xy[i]);}
    space_left = (int) (sizeof(temp) - strlen(temp) - strlen(tmp) - 1);
    if( space_left > 0 ){ strncat(temp,tmp,space_left - 1);}
    else{
      canvas_error("can not parse integer to js-array:\nYour curve plotting produces too many data \nreduce your image size or plotsteps ");}
    }
    strncat(temp,"],[",4); /* close js x_values array and start new */
    for(i = 1; i < len;i = i + 2){ /* y_points */
      if(i == len - 1){ sprintf(tmp, "%.*f",decimals,xy[i]);}else{sprintf(tmp, "%.*f,",decimals,xy[i]);}
      space_left = (int) (sizeof(temp) - strlen(temp) - strlen(tmp) - 1);
      if( space_left > 0 ){ strncat(temp,tmp,space_left - 1);}else{canvas_error("can not parse integer to js-array:\nYour curve plotting produces too many data \nreduce your image size or plotsteps");}
    }
    strncat(temp,"]",2);
    string=(char *)my_newmem(sizeof(temp));
    snprintf(string,sizeof(temp),"%s",temp);
    return string;
}

char *list2js_array(char *list, char *s){/* abc:defg:hjiuy:qwer --> ["abc","defg","hjiuy","qwer"] */
#define MAX_ARG 128
  if( strlen(list)> MAX_ARG - 1){canvas_error("argument is too large (&gt; 128)");}
  char tmp[MAX_ARG];
  size_t p = 0;
  tmp[0] = '[';
  tmp[1] = '\"';
  size_t t = 2;
  while(list[p] != '\0'){
    if( list[p] == s[0] ){
      tmp[t++]='\"';tmp[t++]=',';tmp[t++]='\"';
    }
    else
    {
      tmp[t++] = list[p];
    }
    p++;
  }
  tmp[t++]='\"';tmp[t++]=']';tmp[t++]= '\0';
  char *js_array = (char *)my_newmem(sizeof(tmp));
  snprintf(js_array,sizeof(tmp),"%s",tmp);
  return js_array;
}

char *xy2js_array(int xy[],int len){
 /*
    1,2,3,4,5,6,7,8 --> [1,3,5,7],[2,4,6,8]
    int xy[] is already checked for errors or overflow in "get_real()"
    just to be sure we double check the size of "temp"
*/
  char temp[MAX_BUFFER], *string;
  char *tmp = my_newmem(16);/* <= 9999999999999999  */
  memset(temp,'\0',MAX_BUFFER);/* clear memory */
  int i;int space_left;
  temp[0] = '[';/* start js-array */
  for(i = 0; i < len;i = i + 2){ /*  x_points[] */
    if(i == len - 2){sprintf(tmp, "%d", xy[i]);}else{sprintf(tmp, "%d,", xy[i]);}
    space_left = (int) (sizeof(temp) - strlen(temp) - strlen(tmp) - 1);
    if( space_left > 0 ){ strncat(temp,tmp,space_left - 1);}
    else {
    canvas_error("can not parse integer to js-array:\nYour curve plotting produces too many data \nreduce image size or plotsteps ");}
  }
  strncat(temp,"],[",4); /* close js x_values array and start new */
  for(i = 1; i < len;i = i + 2){ /* y_points */
    if(i == len - 1){ sprintf(tmp, "%d", xy[i]);}else{sprintf(tmp, "%d,", xy[i]);}
    space_left = (int) (sizeof(temp) - strlen(temp) - strlen(tmp) - 1);
    if( space_left > 0 ){ strncat(temp,tmp,space_left - 1);}
    else{canvas_error("can not parse integer to js-array:\nYour curve plotting produces too many data\nreduce image size or plotsteps \n");}
  }
  strncat(temp,"]",2);
  string=(char *)my_newmem(sizeof(temp));
  snprintf(string,sizeof(temp),"%s",temp);
  return string;
}

char *data2js_array(int data[],int len){
 /*
    1,2,3,4,5,6,7,8 --> [1,2,3,4,5,6,7,8]
    int data[] is already checked for errors or overflow in "get_real()"
    just to be sure we double check the size of "temp"
*/
  char temp[MAX_BUFFER], *string;
  char *tmp = my_newmem(16);/* <= 9999999999999999  */
  memset(temp,'\0',MAX_BUFFER);/* clear memory */
  int i;int space_left;
  temp[0] = '[';/* start js-array */
  for(i = 0; i < len; i++){
    if(i == len - 1){sprintf(tmp, "%d", data[i]);}else{sprintf(tmp, "%d,", data[i]);}
    space_left = (int) (sizeof(temp) - strlen(temp) - strlen(tmp) - 1);
    if( space_left > 0 ){ strncat(temp,tmp,space_left - 1);}
    else{
    canvas_error("can not parse integer to js-array:\nYour curve plotting produces too many data \nreduce image size or plotsteps ");
    }
  }
  strncat(temp,"]",2);
  string=(char *)my_newmem(sizeof(temp));
  snprintf(string,sizeof(temp),"%s",temp);
  return string;
}

char *doubledata2js_array(double data[],int len, int decimals){
 /*
    1.4355,2.345353,3.3455 --> [1.44,2.35,3.35]
    double data[] is already checked for errors or overflow in "get_real()"
    just to be sure we double check the size of "temp"
*/
  char temp[MAX_BUFFER], *string;
  char *tmp = my_newmem(16);/* <= 9999999999999999  */
  memset(temp,'\0',MAX_BUFFER);/* clear memory */
  int i;int space_left;
  temp[0] = '[';/* start js-array */
  for(i = 0; i < len; i++){
    if(i == len - 1){sprintf(tmp, "%.*f",decimals,data[i]);}
    else{sprintf(tmp, "%.*f,",decimals,data[i]);}
    space_left = (int) (sizeof(temp) - strlen(temp) - strlen(tmp) - 1);
    if( space_left > 0 ){ strncat(temp,tmp,space_left - 1);}
    else{
      canvas_error("can not parse integer to js-array:\nYour curve plotting produces too many data \nreduce image size or plotsteps ");}
  }
  strncat(temp,"]",2);
  string=(char *)my_newmem(sizeof(temp));
  snprintf(string,sizeof(temp),"%s",temp);
  return string;
}

void *my_newmem(size_t size){
  void  *p;
  if((p = malloc(size +1)) == NULL){canvas_error("canvasdraw: ran out of memory\n");}
  return p;
}

int find_number_of_digits(int i){
  if(i < 0 ){ i = -1*i;}
  int digits = 0;
  while ( i > 0){
    digits++;
    i = i/10;
  }
  return digits;
}

int count_substring(char* string, char* substring) {
  int i, j, l1, l2;
  int count = 0;
  l1 = strlen(string);
  l2 = strlen(substring);
  for(i = 0; i < l1 - l2 + 1; i++) {
    if(strstr(string + i, substring) == string + i) {
      count++;
      i = i + l2 -1;
    }
  }
  return count;
}

void check_string_length(int L){
  if( L > MAX_BUFFER-1){
    canvas_error("problem with your arguments to command...");
  }
  return;
}
/* useful in hyp commands: determine if the hypsegment is an arc or a line */
/* give a point on the arc */
int hypgeodaux(double *q, double* ress, int full){
  double alpha,beta,gamma,r,cx,cy,a1,a2,a3,tmp,
    nx = -q[0]*q[2]*q[2]+(q[0]*q[0]+q[1]*q[1]+1)*q[2]-q[0]*q[3]*q[3]-q[0],
    ny = -q[1]*q[2]*q[2]-q[1]*q[3]*q[3]+(q[0]*q[0]+q[1]*q[1]+1)*q[3]-q[1],
    dy = -2*q[1]*q[2]+2*q[0]*q[3];
  if (dy*dy*1e4 <= nx*nx+ny*ny){
    if(full){
      if (q[1]*q[1]+q[0]*q[0] > q[2]*q[2]+q[3]*q[3])
        gamma = atan2(q[1],q[0]);
      else
        gamma = atan2(q[3],q[2]);
      ress[0]=cos(gamma); ress[1]=sin(gamma); ress[2]=-cos(gamma); ress[3]=-sin(gamma);
    }
    else
      {int i;for(i=0;i<4;++i) ress[i]=q[i];}
    ress[6]=(q[0]+q[2])/2;
    ress[7]=(q[1]+q[3])/2;
    return 0;}
  cx = ny/dy; cy=-nx/dy;
  r = sqrt(cx*cx+cy*cy-1);
  if(full)
    {alpha=atan(1/r); beta = atan2(cy,cx);a1=M_PI+beta-alpha;a2=M_PI+beta+alpha;}
  else
    {a1 = atan2(q[1]-cy, q[0]-cx);a2 = atan2(q[3]-cy, q[2]-cx);
      if (fabs(a2-a1)>M_PI){if(a1>a2) a1-=2*M_PI; else a2-=2*M_PI;}}
  a3 = (a1+a2)/2;
  ress[0]=cx;
  ress[1]=cy;
  ress[2]=2*r;
  ress[3]=2*r;
  ress[4]=360+180/M_PI*a1;
  ress[5]=360+180/M_PI*a2;
  ress[6]=cx+r*cos(a3);
  ress[7]=cy+r*sin(a3);
  return 1;
}

int get_token(FILE *infile){
  int left,milieu,delta,right,c,i=0;
  char temp[MAX_INT], *input_type;
  enum{rien,usefilled,usedashed};
  static struct commande {char *nom; int res; int special;} table[]=
  {
{"#",COMMENT,0},
{"affine",AFFINE,0},
{"allowdups",ALLOW_DUPLICATES,0},
{"angle",ANGLE,0},
{"animate",ANIMATE,0},
{"arc",ARC,0},
{"arcarrow",ARCARROW,0},
{"arrow",ARROW,0},
{"arrow2",ARROW2,0},
{"arrowarc",ARCARROW,0},
{"arrowhead",ARROWHEAD,0},
{"arrows",ARROWS,0},
{"arrows2",ARROWS2,0},
{"audio",AUDIO,0},
{"audioobject",AUDIOOBJECT,0},
{"axis",AXIS,0},
{"axisnumbering",AXIS_NUMBERING,0},
{"axisnumbers",AXIS_NUMBERING,0},
{"backgroundimage",BGIMAGE,0},
{"barchart",BARCHART,0},
{"bezier",BEZIER,0},
{"bgcolor",BGCOLOR,0},
{"bgimage",BGIMAGE,0},
{"blink",BLINK,0},
{"boxplot",BOXPLOT,0},
{"boxplotdata",BOXPLOTDATA,0},
{"brokenline",POLYLINE,0},
{"canvastype",CANVASTYPE,0},
{"centered",CENTERED,0},
{"centerstring",CENTERSTRING,0},
{"chemtex",CHEMTEX,0},
{"circle",CIRCLE,0},
{"circles",CIRCLES,0},
{"clearbutton",CLEARBUTTON,0},
{"clicktile",CLICKTILE,0},
{"clicktile_colors",CLICKTILE_COLORS,0},
{"clock",CLOCK,0},
{"colorpalette",COLORPALETTE,0},
{"colors",MULTISTROKECOLORS,0},
{"copy",COPY,0},
{"copyresized",COPYRESIZED,0},
{"crosshair",CROSSHAIR,0},
{"crosshairs",CROSSHAIRS,0},
{"crosshairsize",CROSSHAIRSIZE,0},
{"css",CSS,0},
{"cursor",CURSOR,0},
{"curve",CURVE,0},
{"curvedarrow",CURVEDARROW,0},
{"curvedarrow2",CURVEDARROW2,0},
{"curvedarrows",CURVEDARROWS,0},
{"curvedarrows2",CURVEDARROWS2,0},
{"darrow",ARROW,usedashed},
{"darrow2",ARROW2,usedashed},
{"dashed",DASHED,0},
{"dashtype",DASHTYPE,0},
{"dcurve",CURVE,usedashed},
{"delete",CLEARBUTTON,0},
{"demiline",HALFLINE,0},
{"demilines",HALFLINES,0},
{"dhline",HLINE,usedashed},
{"diafill",DIAMONDFILL,0},
{"diamondfill",DIAMONDFILL,0},
{"disk",CIRCLE,usefilled},
{"disks",CIRCLES,usefilled},
{"display",MOUSE_DISPLAY,0},
{"dline",LINE,usedashed},
{"dotfill",DOTFILL,0},
{"dplot",CURVE,usedashed},
{"dpolyline",POLYLINE,usedashed},
{"drag",DRAG,0},
{"dsegment",SEGMENT,usedashed},
{"dsegments",SEGMENTS,usedashed},
{"duplicates",ALLOW_DUPLICATES,0},
{"dvline",VLINE,usedashed},
{"ellipse",ELLIPSE,0},
{"ellipses",ELLIPSES,0},
{"end",END,0},
{"erase",CLEARBUTTON,0},
{"farc",ARC,usefilled},
{"fcircle",CIRCLE,usefilled},
{"fcircles",CIRCLES,usefilled},
{"fellipse",ELLIPSE,usefilled},
{"fhypcircles",HYPCIRCLES,usefilled},
{"fhyppolygon",HYPPOLY,usefilled},
{"fill",FLOODFILL,0},
{"fillall",FILLALL,0},
{"fillcolor",FILLCOLOR,0},
{"fillcolors",MULTIFILLCOLORS,0},
{"filled",FILLED,0},
{"filledarc",ARC,usefilled},
{"filledpoly",POLY,usefilled},
{"filledpolygon",POLY,usefilled},
{"fillopacity",FILLOPACITY,0},
{"fillpattern",FILLPATTERN,0},
{"filltoborder",FILLTOBORDER,0},
{"floodfill",FLOODFILL,0},
{"fontcolor",FONTCOLOR,0},
{"fontfamily",FONTFAMILY,0},
{"fontsize",FONTSIZE,0},
{"fpoly",POLY,usefilled},
{"fpolygon",POLY,usefilled},
{"frect",RECT,usefilled},
{"frectangle",RECT,usefilled},
{"frects",RECTS,usefilled},
{"froundrect",ROUNDRECT,usefilled},
{"froundrects",ROUNDRECTS,usefilled},
{"fsquare",SQUARE,usefilled},
{"fsquares",RECTS,usefilled},
{"ftriangle",TRIANGLE,usefilled},
{"ftriangles",TRIANGLES,usefilled},
{"functionlabel",FUNCTION_LABEL,0},
{"functionlabels",FUNCTION_LABEL,0},
{"grid",GRID,0},
{"gridfill",GRIDFILL,0},
{"group",GROUP,0},
{"halfline",HALFLINE,0},
{"halflines",HALFLINES,0},
{"hatchfill",HATCHFILL,0},
{"highlight",STYLE,0},
{"hline",HLINE,0},
{"hlines",HLINES,0},
{"horizontalline",HLINE,0},
{"horizontallines",HLINES,0},
{"html",HTML,0},
{"http",HTTP,0},
{"hypcircles",HYPCIRCLES,0},
{"hyplines",HYPLINES,0},
{"hyppolygon",HYPPOLY,0},
{"hyprays",HYPRAYS,0},
{"hypsegments",HYPSEGMENTS,0},
{"imagefill",IMAGEFILL,0},
{"imagepalette",IMAGEPALETTE,0},
{"input",INPUT,0},
{"intooltip",INTOOLTIP,0},
{"jscurve",JSCURVE,0},
{"jsmath",JSMATH,0},
{"jsplot",JSCURVE,0},
{"katex",LATEX,0},
{"kill",KILL,0},
{"killaffine",KILLAFFINE,0},
{"killgroup",KILLSLIDER,0},
{"killinear",KILLLINEAR,0},
{"killlinear",KILLLINEAR,0},
{"killmoebius",KILLMOEBIUS,0},
{"killreset",NORESET,0},
{"killrotate",KILLROTATE,0},
{"killslider",KILLSLIDER,0},
{"killtranslate",KILLTRANSLATION,0},
{"killtranslation",KILLTRANSLATION,0},
{"latex",LATEX,0},
{"lattice",LATTICE,0},
{"legend",LEGEND,0},
{"legendcolors",LEGENDCOLORS,0},
{"levelcurve",LEVELCURVE,0},
{"levelstep",LEVELSTEP,0},
{"line",LINE,0},
{"linear",LINEAR,0},
{"linegraph",LINEGRAPH,0},
{"lines",LINES,0},
{"linewidth",LINEWIDTH,0},
{"linewidths",MULTILINEWIDTH,0},
{"math",LATEX,0},
{"mathml",MATHML,0},
{"moebius",MOEBIUS,0},
{"mouse",MOUSE,0},
{"mousedegree",MOUSE_DEGREE,0},
{"mouseprecision",MOUSE_PRECISION,0},
{"mousex",MOUSEX,0},
{"mousey",MOUSEY,0},
{"multicolors",MULTISTROKECOLORS,0},
{"multidash",MULTIDASH,0},
{"multidraw",MULTIDRAW,0},
{"multifill",MULTIFILL,0},
{"multifillcolors",MULTIFILLCOLORS,0},
{"multifillopacity",MULTIFILLOPACITY,0},
{"multiinput",MULTIUSERINPUT,0},
{"multilabel",MULTILABEL,0},
{"multilinewidth",MULTILINEWIDTH,0},
{"multisnap",MULTISNAPTOGRID,0},
{"multisnaptogrid",MULTISNAPTOGRID,0},
{"multistrokecolors",MULTISTROKECOLORS,0},
{"multistrokeopacity",MULTISTROKEOPACITY,0},
{"multiuserinput",MULTIUSERINPUT,0},
{"newrange",NEWRANGE,0},
{"noreset",NORESET,0},
{"nostatus",STATUS,0},
{"noxaxis",NOXAXIS,0},
{"noyaxis",NOYAXIS,0},
{"numberline",NUMBERLINE,0},
{"obabel",OBABEL,0},
{"onclick",ONCLICK,0},
{"opacity",OPACITY,0},
{"parallel",PARALLEL,0},
{"path",POLYLINE,0},
{"patternfill",PATTERNFILL,0},
{"piechart",PIECHART,0},
{"pixels",PIXELS,0},
{"pixelsize",PIXELSIZE,0},
{"plot",CURVE,0},
{"plotstep",PLOTSTEPS,0},
{"plotsteps",PLOTSTEPS,0},
{"point",POINT,0},
{"pointer",CURSOR,0},
{"points",POINTS,0},
{"poly",POLY,0},
{"polygon",POLY,0},
{"polyline",POLYLINE,0},
{"popup",POPUP,0},
{"precision",MOUSE_PRECISION,0},
{"protractor",PROTRACTOR,0},
{"range",RANGE,0},
{"ranget",TRANGE,0},
{"rangex",XRANGE,0},
{"rangey",YRANGE,0},
{"rays",RAYS,0},
{"rect",RECT,0},
{"rectangle",RECT,0},
{"rects",RECTS,0},
{"replyformat",REPLYFORMAT,0},
{"reset",RESET,0},
{"resetoffset",RESETOFFSET,0},
{"rotate",ROTATE,0},
{"rotationcenter",ROTATION_CENTER,0},
{"roundrect",ROUNDRECT,0},
{"roundrectangle",ROUNDRECT,0},
{"roundrects",ROUNDRECTS,0},
{"ruler",RULER,0},
{"seg",SEGMENT,0},
{"segment",SEGMENT,0},
{"segments",SEGMENTS,0},
{"segs",SEGMENTS,0},
{"setlimits",SETLIMITS,0},
{"setpixel",SETPIXEL,0},
{"settile",FILLPATTERN,0},
{"sgraph",SGRAPH,0},
{"size",SIZE,0},
{"slider",SLIDER,0},
{"snaptofun",SNAPTOFUNCTION,0},
{"snaptofunction",SNAPTOFUNCTION,0},
{"snaptogrid",SNAPTOGRID,0},
{"snaptopoints",SNAPTOPOINTS,0},
{"square",SQUARE,0},
{"status",STATUS,0},
{"string",STRING,0},
{"stringup",STRINGUP,0},
{"strokecolor",STROKECOLOR,0},
{"strokecolors",MULTISTROKECOLORS,0},
{"strokeopacity",STROKEOPACITY,0},
{"style",STYLE,0},
{"text",FLY_TEXT,0},
{"textarea",TEXTAREA,0},
{"textfill",TEXTFILL,0},
{"textup",FLY_TEXTUP,0},
{"title",CENTERSTRING,0},
{"trace_jscurve",TRACE_JSCURVE,0},
{"trange",TRANGE,0},
{"translate",TRANSLATION,0},
{"translation",TRANSLATION,0},
{"transparent",OPACITY,0},
{"triangle",TRIANGLE,0},
{"triangles",TRIANGLES,0},
{"tsteps",PLOTSTEPS,0},
{"userboxplot",USERBOXPLOT,0},
{"userboxplotdata",USERBOXPLOT,0},
{"userdraw",USERDRAW,0},
{"userinput",USERINPUT,0},
{"userinput_function",USERINPUT_FUNCTION,0},
{"userinput_xy",USERINPUT_XY,0},
{"vector",ARROW,0},
{"vectors",ARROWS,0},
{"verticalline",VLINE,0},
{"verticallines",VLINES,0},
{"video",VIDEO,0},
{"vline",VLINE,0},
{"vlines",VLINES,0},
{"xaxis",X_AXIS_STRINGS,0},
{"xaxistext",X_AXIS_STRINGS,0},
{"xaxistextup",X_AXIS_STRINGS_UP,0},
{"xaxisup",X_AXIS_STRINGS_UP,0},
{"xerrorbars",XERRORBARS,0},
{"xlabel",XLABEL,0},
{"xlogbase",XLOGBASE,0},
{"xlogscale",XLOGSCALE,0},
{"xoffset",XOFFSET,0},
{"xrange",XRANGE,0},
{"xsnaptogrid",XSNAPTOGRID,0},
{"xunit",XUNIT,0},
{"xylogscale",XYLOGSCALE,0},
{"xyoffset",XYOFFSET,0},
{"yaxis",Y_AXIS_STRINGS,0},
{"yaxistext",Y_AXIS_STRINGS,0},
{"yerrorbars",YERRORBARS,0},
{"ylabel",YLABEL,0},
{"ylogbase",YLOGBASE,0},
{"ylogscale",YLOGSCALE,0},
{"yoffset",YOFFSET,0},
{"yrange",YRANGE,0},
{"ysnaptogrid",YSNAPTOGRID,0},
{"yunit",YUNIT,0},
{"zoom",ZOOM,0}
  };

  while(((c = getc(infile)) != EOF)&&(c!='\n')&&(c!=',')&&(c!='=')&&(c!='\r')&&(c!='\t')){
   if( i == 0 && (c == ' ') ){ continue; /* white spaces or tabs allowed before first command identifier */
   }else{
    if( c == ' ' ){
      break;
    }else{
     temp[i] = c;
     if(i > MAX_INT - 2){canvas_error("command string too long !");}
     i++;
    }
   }
   if(temp[0] == '#'){ break; }
  }
  if (c == '\n' || c == '\r' || c == '\t' ){  line_number++; }
  if (c == EOF) {finished=1;return 0;}

  temp[i]='\0';
  if(strstr(temp,"dash") !=0 && strlen(temp) > 4 && strstr(temp,"dashtype") == 0 ){
  /* 4/2024 adapt to Flydraw's dashing syntax for all objects with prefix "dash" */
      use_dashed = TRUE;int idx=0;int p;
      for( p = 4; p < strlen(temp); p++){temp[idx] = temp[p];idx++;}
      input_type = (char*)my_newmem(idx); snprintf(input_type,idx+1,"%s",temp);
  }else{
      input_type=(char*)my_newmem(strlen(temp));
      snprintf(input_type,sizeof(temp),"%s",temp);
  }
/*  fprintf(stdout,"input_type = %s no_reset = %d <br>",input_type,no_reset);*/

  left=0; right=sizeof(table)/sizeof(struct commande);
  while (left<right){
    milieu = (left+right)/2;
    delta = strcmp(input_type, table[milieu].nom);
    if (delta == 0){
       free(input_type);
       if (table[milieu].special==usefilled) use_filled=TRUE;
       if (table[milieu].special==usedashed) use_dashed=TRUE;
       return table[milieu].res;
    }
    if (delta<0) right=milieu; else left=milieu+1;
  }
  free(input_type);
  ungetc(c,infile);
  return 0;
}
