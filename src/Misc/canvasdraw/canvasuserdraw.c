#include "canvasdraw.h"

void add_js_userdraw(char *draw_type,char *stroke_color,double stroke_opacity,int crosshair_size,int arrow_head,int use_offset, char *css_class,int use_snap,int canvas_type,int use_filled,char * fill_color,double fill_opacity,int line_width,char *font_family){

#define MAX_USERDRAW_PRIMITIVES 59
static char userdraw_primitives[MAX_USERDRAW_PRIMITIVES][32] = {
"point","points", /* 0,1 */
"crosshair","crosshairs",/* 2,3 */
"circle","circles",/* 4,5 */
"segment","segments", /* 6,7 */
"demiline","halfline","demilines","halflines", /* 8,9,10,11 */
"line","lines", /* 12,13 */
"hline","hlines", /* 14,15 */
"vline","vlines", /* 16,17 */
"arrow","arrows", /* 18,19 */
"arrow2","arrows2", /* 20,21 */
"curvedarrow","curvedarrows",/* 22,23 */
"curvedarrow2","curvedarrows2", /* 24,25 */
"freehandline","path","freehandlines","paths", /* 26,27,28,29 */
"poly","polys",/*poly[3]...polys[9] */ /* 30,31 */
"polygon","polygons", /* 32,33 */
"polyline","polylines", /* 34,35 */
"triangle","triangles", /* 36,37 */
"rect","rects",/* 38,39 */
"roundrect","roundrects", /* 40,41*/
"arc","arcs", /* 42,43 */
"text", /* 44 */
"image","images", /* 45 46*/
"input","inputs", /* 47,48*/
"clickfill","gridfill","diamondfill","dotfill","hatchfill","textfill",/* 49,50,51,52,53,54 */
"parallelogram","parallelograms",/* 55,56 */
"function","functions" /* 57,58 is the same as command "userinput function"*/
};

/*1+strlen() of words "point","points",... == 6,7,..*/
//static int userdraw_primitives_length[MAX_USERDRAW_PRIMITIVES] = {6,7,10,11,7,8,8,9,9,9,10,10,5,6,6,7,6,7,6,7,7,8,12,13,13,14,13,5,14,6,5,6,8,9,9,10,9,10,5,6,10,11,4,5,5,6,7,6,7,19,12,8,10,9,14,15};

static int userdraw_primitives_length[MAX_USERDRAW_PRIMITIVES] = {6,7,10,11,7,8,8,9,9,9,10,10,5,6,6,7,6,7,6,7,7,8,12,13,13,14,13,5,14,6,5,6,8,9,9,10,9,10,5,6,10,11,4,5,5,6,7,6,7,10,9,12,8,10,9,14,15,9,10};
int p,n;
int found = 0;
int draw_num = -1;
int equal = -1;;
int polynum = -1;


if(strncmp(draw_type,"functions",9) == 0){
  draw_num = 58; polynum = (int) (draw_type[9]-'0');found = 1;
// fprintf(stdout,"found polys%d<br>",polynum);
}
else {
  if(strncmp(draw_type,"polys",5) == 0){
    draw_num = 31; polynum = (int) (draw_type[5]-'0');found = 1;
// fprintf(stdout,"found polys%d<br>",polynum);
  }
  else {
    if( strncmp(draw_type,"poly",4)  == 0 && ( strstr(draw_type,"polygon") == NULL && strstr(draw_type,"polyline") == NULL ) ){
      draw_num = 30; polynum = (int) (draw_type[4]-'0');found = 1;
//  fprintf(stdout,"found poly%d<br>",polynum);
    }
    else {
      for( p = 0; p < MAX_USERDRAW_PRIMITIVES ; p++ ){
        equal = strncmp(userdraw_primitives[p],draw_type,userdraw_primitives_length[p]);
/* remake array:  static int userdraw_primitives_length[MAX_USERDRAW_PRIMITIVES]*/
/* fprintf(stdout,"%d,",1+strlen(userdraw_primitives[p]));*/
        if( equal == 0 ){
/*    fprintf(stdout,"found %s p=%d<br>",userdraw_primitives[p],p);*/
         draw_num= p;found=1; break;
        }
      }
    }
  }
}
if( found == 0 ){canvas_error("unknown userdraw primitive found...typo?");}

/* begin user_draw() */

/* touchend routine for touch devices */
fprintf(js_include_file,"\
function user_drawstop(evt){\
 if(!user_is_dragging){user_drag(evt);return;};\
 if(user_is_dragging){user_draw(evt);return;};\
};");

fprintf(js_include_file,"function user_draw(evt){\
if(evt.button == 2){remove_last();return;};\
var mouse = getMouse(evt,canvas_userdraw);\
var xy = multisnap_check(mouse.x,mouse.y,use_snap);\
user_is_dragging = false;\
var x = xy[0];var y = xy[1];\
if(x>forbidden_zone[0] && y>forbidden_zone[1]){console.log('drawing in zoom area...');return;};\
");
int temp_reply_format = 22;
switch( draw_num ){
  default: break;
  case 0:temp_reply_format = 8;fprintf(js_include_file,"points(x,y,0,0);");break;
  case 1:temp_reply_format = 8;fprintf(js_include_file,"points(x,y,0,1);");break;
  case 2:temp_reply_format = 8;fprintf(js_include_file,"crosshairs(x,y,0,0);");break;
  case 3:temp_reply_format = 8;fprintf(js_include_file,"crosshairs(x,y,0,1);");break;
  case 4:temp_reply_format = 10;fprintf(js_include_file,"circles(x,y,0,0);");break;
  case 5:temp_reply_format = 10;fprintf(js_include_file,"circles(x,y,0,1);");break;
  case 6:temp_reply_format = 11;fprintf(js_include_file,"segments(x,y,0,0);");break;
  case 7:temp_reply_format = 11;fprintf(js_include_file,"segments(x,y,0,1);");break;
  case 8 ... 9:temp_reply_format = 11;fprintf(js_include_file,"demilines(x,y,0,0);");break;
  case 10 ... 11:temp_reply_format = 11;fprintf(js_include_file,"demilines(x,y,0,1);");break;
  case 12:temp_reply_format = 11;fprintf(js_include_file,"lines(x,y,0,0);");break;
  case 13:temp_reply_format = 11;fprintf(js_include_file,"lines(x,y,0,1);");break;
  case 14:temp_reply_format = 11;fprintf(js_include_file,"hlines(x,y,0,0);");break;
  case 15:temp_reply_format = 11;fprintf(js_include_file,"hlines(x,y,0,1);");break;
  case 16:temp_reply_format = 11;fprintf(js_include_file,"vlines(x,y,0,0);");break;
  case 17:temp_reply_format = 11;fprintf(js_include_file,"vlines(x,y,0,1);");break;
  case 18:temp_reply_format = 11;fprintf(js_include_file,"arrows(x,y,0,0);");break;
  case 19:temp_reply_format = 11;fprintf(js_include_file,"arrows(x,y,0,1);");break;
  case 20:temp_reply_format = 11;fprintf(js_include_file,"arrows2(x,y,0,0);");break;
  case 21:temp_reply_format = 11;fprintf(js_include_file,"arrows2(x,y,0,1);");break;
  case 22:temp_reply_format = 2;fprintf(js_include_file,"curvedarrows(x,y,0,0);");break;
  case 23:temp_reply_format = 2;fprintf(js_include_file,"curvedarrows(x,y,0,1);");break;
  case 24:temp_reply_format = 2;fprintf(js_include_file,"curvedarrows2(x,y,0,0);");break;
  case 25:temp_reply_format = 2;fprintf(js_include_file,"curvedarrows2(x,y,0,1);");break;
  case 26 ... 27:temp_reply_format = 6;fprintf(js_include_file,"paths(x,y,0,0);");break;
  case 28 ... 29:temp_reply_format = 6;fprintf(js_include_file,"paths(x,y,0,0);");break;
  case 30:temp_reply_format = 2;fprintf(js_include_file,"polys(x,y,0,0);");break;
  case 31:temp_reply_format = 2;fprintf(js_include_file,"polys(x,y,0,1);");break;
  case 32:temp_reply_format = 2;fprintf(js_include_file,"polygon(x,y,0,0);");break;
  case 33:temp_reply_format = 2;fprintf(js_include_file,"polygon(x,y,0,1);");break;
  case 34:temp_reply_format = 23;fprintf(js_include_file,"polylines(x,y,0,1);");break;
  case 35:temp_reply_format = 23;fprintf(js_include_file,"polylines(x,y,0,1);");break;
  case 36:temp_reply_format = 2;fprintf(js_include_file,"triangles(x,y,0,0);");break;
  case 37:temp_reply_format = 2;fprintf(js_include_file,"triangles(x,y,0,1);");break;
  case 38:temp_reply_format = 2;fprintf(js_include_file,"rects(x,y,0,0);");break;
  case 39:temp_reply_format = 2;fprintf(js_include_file,"rects(x,y,0,1);");break;
  case 40:temp_reply_format = 2;fprintf(js_include_file,"roundrects(x,y,0,0);");break;
  case 41:temp_reply_format = 2;fprintf(js_include_file,"roundrects(x,y,0,1);");break;
  case 42:temp_reply_format = 25;js_function[JS_FIND_ANGLE] = 1; fprintf(js_include_file,"arcs(x,y,0,0);");break;
  case 43:temp_reply_format = 25;js_function[JS_FIND_ANGLE] = 1; fprintf(js_include_file,"arcs(x,y,0,1);");break;
  case 44:temp_reply_format = 17;js_function[DRAW_SUBSUP] = 1;fprintf(js_include_file,"text(x,y,0,1);");break;
  case 45:temp_reply_format = 17;fprintf(js_include_file,"images(x,y,0,0);");break;
  case 46:temp_reply_format = 17;fprintf(js_include_file,"images(x,y,0,1);");break;
  case 47:temp_reply_format = 27;fprintf(js_include_file,"inputs(x,y,0,0);");break;
  case 48:temp_reply_format = 27;fprintf(js_include_file,"inputs(x,y,0,1);");break;
  case 49 ... 54 :
 /*
 "clickfill 49","gridfill 50","diamondfill 51","dotfill 52","hatchfill 53","textfill 54", 49,50,51,52,53,54
use_filled ...  0:no fill, 1:fill,2=grid?,3=hatch?,4=diamond?,5=dot?,6=image ?
 */
  temp_reply_format = 10;
 fprintf(js_include_file,"clickfill(x,y,0,1);");
 if( draw_num == 49 ){ use_filled = 1;}
 else{
 if( draw_num == 50 ){js_function[DRAW_GRIDFILL] = 1;use_filled = 2;}
 else{
 if( draw_num == 51 ){js_function[DRAW_DIAMONDFILL] = 1;use_filled = 4;}
 else{
 if( draw_num == 52 ){js_function[DRAW_DOTFILL] = 1;use_filled = 5;}
 else{
 if( draw_num == 53 ){js_function[DRAW_HATCHFILL] = 1;use_filled = 3;}
 else{
 if( draw_num == 54 ){js_function[DRAW_TEXTFILL] = 1;use_filled = 7;}}}}}}
 break;

 case 55:temp_reply_format = 2;fprintf(js_include_file,"parallelograms(x,y,0,0);");break;
 case 56:temp_reply_format = 2;fprintf(js_include_file,"parallelograms(x,y,0,1);");break;
 case 57 ... 58:
 // fprintf(stdout,"functions num = %d;",(int) (draw_type[9]-'0'));*/ /* functions4 == 9
 if(reply_format == 0 || reply_format != -1){reply_format = 24;}
 break;

}
fprintf(js_include_file," return;};");

if( reply_format == 0 ){reply_format = temp_reply_format;}

/* begin user_drag()  */
fprintf(js_include_file,"function user_drag(evt){\
var mouse = getMouse(evt,canvas_userdraw);\
if(isTouch){var xy = multisnap_check(mouse.x,mouse.y,use_snap);};\
var x = mouse.x;var y = mouse.y;\
user_is_dragging = true;\
if(x>forbidden_zone[0] && y>forbidden_zone[1]){return;};");
switch( draw_num ){
  case 0:fprintf(js_include_file,"return;");break;
  case 1:fprintf(js_include_file,"return;");break;
  case 2:fprintf(js_include_file,"return;");break;
  case 3:fprintf(js_include_file,"return;");break;
  case 4:fprintf(js_include_file,"circles(x,y,1,0);return;");break;
  case 5:fprintf(js_include_file,"circles(x,y,1,1);return;");break;
  case 6:fprintf(js_include_file,"segments(x,y,1,0);");break;
  case 7:fprintf(js_include_file,"segments(x,y,1,1);return;");break;
  case 8 ... 9:fprintf(js_include_file,"demilines(x,y,1,0);return;");break;
  case 10 ... 11:fprintf(js_include_file,"demilines(x,y,1,1);return;");break;
  case 12:fprintf(js_include_file,"lines(x,y,1,0);return;");break;
  case 13:fprintf(js_include_file,"lines(x,y,1,1);return;");break;
  case 14:fprintf(js_include_file,"hlines(x,y,1,0);return;");break;
  case 15:fprintf(js_include_file,"hlines(x,y,1,1);return;");break;
  case 16:fprintf(js_include_file,"vlines(x,y,1,0);return;");break;
  case 17:fprintf(js_include_file,"vlines(x,y,1,1);return;");break;
  case 18:fprintf(js_include_file,"arrows(x,y,1,0);return;");break;
  case 19:fprintf(js_include_file,"arrows(x,y,1,1);return;");break;
  case 20:fprintf(js_include_file,"arrows2(x,y,1,0);return;");break;
  case 21:fprintf(js_include_file,"arrows2(x,y,1,1);return;");break;
  case 22:fprintf(js_include_file,"curvedarrows(x,y,1,0);return;");break;
  case 23:fprintf(js_include_file,"curvedarrows(x,y,1,1);return;");break;
  case 24:fprintf(js_include_file,"curvedarrows2(x,y,1,0);return;");break;
  case 25:fprintf(js_include_file,"curvedarrows2(x,y,1,1);return;");break;
  case 26 ... 27:fprintf(js_include_file,"paths(x,y,1,0);return;");break;
  case 28 ... 29:fprintf(js_include_file,"paths(x,y,1,0);return;");break;
  case 30:fprintf(js_include_file,"polys(x,y,1,0);return;");break;
  case 31:fprintf(js_include_file,"polys(x,y,1,1);return;");break;
  case 32:fprintf(js_include_file,"polygon(x,y,1,0);return;");break;
  case 33:fprintf(js_include_file,"polygon(x,y,1,1);return;");break;
  case 34:fprintf(js_include_file,"polylines(x,y,1,1);return;");break;
  case 35:fprintf(js_include_file,"polylines(x,y,1,1);return;");break;
  case 36:fprintf(js_include_file,"triangles(x,y,1,0);return;");break;
  case 37:fprintf(js_include_file,"triangles(x,y,1,1);return;");break;
  case 38:fprintf(js_include_file,"rects(x,y,1,0);return;");break;
  case 39:fprintf(js_include_file,"rects(x,y,1,1);return;");break;
  case 40:fprintf(js_include_file,"roundrects(x,y,1,0);return;");break;
  case 41:fprintf(js_include_file,"roundrects(x,y,1,1);return;");break;
  case 42:fprintf(js_include_file,"arcs(x,y,1,0);return;");break;
  case 43:fprintf(js_include_file,"arcs(x,y,1,1);return;");break;
  case 44:fprintf(js_include_file,"return;");break;
  case 45:fprintf(js_include_file,"return;");break;
  case 46:fprintf(js_include_file,"return;");break;
  case 47:fprintf(js_include_file,"return;");break;
  case 48:fprintf(js_include_file,"return;");break;
  case 49:fprintf(js_include_file,"return;");break;
  case 50:fprintf(js_include_file,"return;");break;
  case 51:fprintf(js_include_file,"return;");break;
  case 52:fprintf(js_include_file,"return;");break;
  case 53:fprintf(js_include_file,"return;");break;
  case 54:fprintf(js_include_file,"return;");break;
  case 55:fprintf(js_include_file,"parallelograms(x,y,1,0);return;");break;
  case 56:fprintf(js_include_file,"parallelograms(x,y,1,1);return;");break;
  case 57 ... 58: break;
}

fprintf(js_include_file,"};");

/* add all stuff needed to draw the selected primitives... */
switch( draw_num ){
/* point/points */
  case -1 : break;
  case 0 ... 1:
    fprintf(js_include_file,"\
context_userdraw.fillStyle = \"rgba(%s,%.2f)\";\
function redraw_userdraw(){draw_points();}\
function remove_last(){if( userdraw_x.length > 0 ){userdraw_x.pop();userdraw_y.pop();draw_points();};return;};\
function points(x,y,event_which,num){\
 if(event_which == 1){ return; };\
 if( num == 0 ){\
  userdraw_x[0] = x;\
  userdraw_y[0] = y;\
 }else{\
  userdraw_x.push(x);\
  userdraw_y.push(y);\
 };\
 draw_points();\
};\
function draw_points(){\
 var len = userdraw_x.length;\
 var radius = 2*(context_userdraw.lineWidth);\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 var r = 2*Math.PI;\
 for(var p = 0 ; p < len ; p++ ){\
  context_userdraw.beginPath();\
  context_userdraw.arc(userdraw_x[p],userdraw_y[p],radius,0,r,false);\
  context_userdraw.closePath();context_userdraw.fill();\
 };\
};",stroke_color,stroke_opacity);
    break;
  /* crosshair /crosshairs */
  case 2 ... 3:
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_crosshairs();};\
function remove_last(){if( userdraw_x.length > 0 ){userdraw_x.pop();userdraw_y.pop();draw_crosshairs();};return;};\
function crosshairs(x,y,event_which,num){\
 if(event_which == 1){ return; };\
 if( num == 0 ){\
  userdraw_x[0] = x;\
  userdraw_y[0] = y;\
 }else{\
  userdraw_x.push(x);\
  userdraw_y.push(y);\
 };\
 draw_crosshairs();\
};\
function draw_crosshairs(){\
 var crosshair_size = %d;\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 var x1,x2,y1,y2;\
 var len = userdraw_x.length;\
 for(var p = 0 ; p < len ; p++ ){\
  x1 = userdraw_x[p] - crosshair_size;\
  x2 = userdraw_x[p] + crosshair_size;\
  y1 = userdraw_y[p] - crosshair_size;\
  y2 = userdraw_y[p] + crosshair_size;\
  context_userdraw.beginPath();\
  context_userdraw.moveTo(x1,y1);\
  context_userdraw.lineTo(x2,y2);\
  context_userdraw.closePath();\
  context_userdraw.stroke();\
  context_userdraw.beginPath();\
  context_userdraw.moveTo(x2,y1);\
  context_userdraw.lineTo(x1,y2);\
  context_userdraw.closePath();\
  context_userdraw.stroke();\
 };\
};",crosshair_size);
    break;
 /* circle/circles */
  case 4 ... 5 :
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_circles();};\
function remove_last(){if( userdraw_x.length > 0 ){userdraw_x.pop();userdraw_y.pop();userdraw_radius.pop();draw_circles();};return;};\
function circles(x,y,event_which,num){\
 var last = userdraw_x.length - 1;\
 var xc = userdraw_x[last];\
 var yc = userdraw_y[last];\
 if(event_which == 0){\
  if( xy_cnt == 0 ){\
   if( num  == 0 ){\
    userdraw_x[0]=x;userdraw_y[0]=y;userdraw_radius[0]=4;\
   }\
   else\
   {\
    userdraw_x.push(x);userdraw_y.push(y);userdraw_radius.push(4);\
   };\
  };\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt == 1 ){\
   userdraw_radius[last] = parseInt(Math.sqrt( (x - xc)*(x - xc) + (y - yc)*(y - yc) ));\
  };\
 };\
 if( xy_cnt == 2 ){\
  xy_cnt = 0;\
  if( num == 0 ){\
   userdraw_x = [];userdraw_y = [];\
   userdraw_x[0] = xc;userdraw_y[0] = yc;\
  };\
 };\
 draw_circles();\
};\
function draw_circles(){\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 var len = userdraw_x.length;\
 var r = 2*Math.PI;\
 for(var p = 0 ; p < len ; p++ ){\
  context_userdraw.beginPath();\
  context_userdraw.arc(userdraw_x[p],userdraw_y[p],userdraw_radius[p],0,r,false);\
  context_userdraw.closePath();\
  if(use_filled!=0){context_userdraw.fill();};\
  context_userdraw.stroke();\
 };\
 return;\
};");
    break;

  /* segment/segments */
  case 6 ... 7 :
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_segments();};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<2;p++){userdraw_x.pop();userdraw_y.pop();};draw_segments();};return;};\
function segments(x,y,event_which,num){\
 if(event_which == 0){\
  if( num == 0 && xy_cnt == 0 ){userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(x);userdraw_y.push(y);\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt == 1 ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   draw_segments();\
    userdraw_x.pop();userdraw_y.pop();\
  };\
 };\
 if( xy_cnt == 2 ){\
  xy_cnt = 0;\
  draw_segments();\
 };\
};\
function draw_segments(){\
 var len = userdraw_x.length;\
 if( len%%2 == 0 ){\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  for(var p = 0 ; p < len ; p = p+2 ){\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(userdraw_x[p],userdraw_y[p]);\
   context_userdraw.lineTo(userdraw_x[p+1],userdraw_y[p+1]);\
   context_userdraw.closePath();\
   context_userdraw.stroke();\
  };\
 };\
 return;\
};");

 /* "demiline","demilines","halfline","halflines"  8,9,10,11 */
  case 8 ... 11 :
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_demilines();};\
function find_inf_point(x1,y1,x2,y2){\
 if(x1<x2+2 && x1>x2-2){if(y1<y2){return [x1,y1,x1,ysize];}else{return [x1,0,x1,y1];};};\
 var rc = (y2 - y1)/(x2 - x1);var q = y1 - (x1)*rc;\
 if( x1 < x2 ){ return [x1,y1,xsize,rc*xsize+q];}else{return [x1,y1,0,q];};\
};\
function calc_lines(){\
 var len = userdraw_x.length;\
 var points = find_inf_point(userdraw_x[len-2],userdraw_y[len-2],userdraw_x[len-1],userdraw_y[len-1]);\
 userdraw_x[len-2] = points[0];\
 userdraw_y[len-2] = points[1];\
 userdraw_x[len-1] = points[2];\
 userdraw_y[len-1] = points[3];\
};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<2;p++){userdraw_x.pop();userdraw_y.pop();};draw_demilines();};return;};\
function demilines(x,y,event_which,num){\
 if(event_which == 0){\
  if( num == 0 && xy_cnt == 0 ){userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(x);userdraw_y.push(y);\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt == 1 ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   draw_demilines();\
   userdraw_x.pop();userdraw_y.pop();\
  };\
 };\
 if( xy_cnt == 2 ){\
  xy_cnt = 0;\
  calc_lines();\
  draw_demilines();\
 };\
};\
function draw_demilines(){\
 var len = userdraw_x.length;\
 if( len %%2 == 0 ){\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  for(var p = 0 ; p < len ; p = p+2 ){\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(userdraw_x[p],userdraw_y[p]);\
   context_userdraw.lineTo(userdraw_x[p+1],userdraw_y[p+1]);\
   context_userdraw.closePath();\
   context_userdraw.stroke();\
  };\
 };\
 return;\
};");
    break;
  /* line/lines */
  case 12 ... 13 :
 fprintf(js_include_file,"\
function redraw_userdraw(){calc_lines();draw_lines();};\
function calc_lines(){\
 var marge = 2;var len = userdraw_x.length;\
 var tmp_x = new Array(len);\
 var tmp_y = new Array(len);\
 var pp;\
 for(var p = 0 ; p < len ;p = p+2){\
  pp = p+1;\
  if(userdraw_x[p] < userdraw_x[pp]+marge && userdraw_x[p] > userdraw_x[pp]-marge){\
   tmp_x[p] = userdraw_x[p];tmp_x[pp] = userdraw_x[pp];\
   tmp_y[p] = 0;tmp_y[pp] = ysize;\
  }\
  else\
  {\
   if(userdraw_y[p] < userdraw_y[pp]+marge && userdraw_y[p] > userdraw_y[pp]-marge){\
    tmp_x[p] = 0;tmp_x[pp] = xsize;\
    tmp_y[p] = userdraw_y[p];tmp_y[pp] = userdraw_y[pp];\
   }\
   else\
   {\
    tmp_x[p] = 0;tmp_x[pp] = xsize;\
    tmp_y[p] = userdraw_y[p] - (userdraw_x[p])*(userdraw_y[pp] - userdraw_y[p])/(userdraw_x[pp] - userdraw_x[p]);\
    tmp_y[pp] = userdraw_y[p] + (xsize - userdraw_x[p])*(userdraw_y[pp] - userdraw_y[p])/(userdraw_x[pp] - userdraw_x[p]);\
   };\
  };\
 };\
 return {x:tmp_x,y:tmp_y};\
};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<2;p++){userdraw_x.pop();userdraw_y.pop();};draw_lines();};return;};\
function lines(x,y,event_which,num){\
 if(event_which == 0){\
  if( num == 0 && xy_cnt == 0 ){userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(x);userdraw_y.push(y);\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt == 1 ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   draw_lines();\
   userdraw_x.pop();userdraw_y.pop();\
  };\
 };\
 if( xy_cnt == 2 ){\
  xy_cnt = 0;\
  draw_lines();\
 };\
};\
function draw_lines(){\
 var len = userdraw_x.length;\
 if( len %%2 == 0 ){\
  var xy = calc_lines();\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  for(var p = 0 ; p < len ; p = p+2 ){\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(xy.x[p],xy.y[p]);\
   context_userdraw.lineTo(xy.x[p+1],xy.y[p+1]);\
   context_userdraw.closePath();\
   context_userdraw.stroke();\
  };\
 };\
 return;\
};");
    break;
 /* hline/hlines 14,15 */
  case 14 ... 15:
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_hlines();};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<2;p++){userdraw_x.pop();userdraw_y.pop();};draw_hlines();};return;};\
function hlines(x,y,event_which,num){\
 if(event_which == 0){\
  if( num == 0 ){userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(0);userdraw_y.push(y);\
  userdraw_x.push(xsize);userdraw_y.push(y);\
  draw_hlines();\
 };\
};\
function draw_hlines(){\
 var len = userdraw_x.length;\
 if( len %%2 == 0 ){\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  for(var p = 0 ; p < len ; p = p+2 ){\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(userdraw_x[p],userdraw_y[p]);\
   context_userdraw.lineTo(userdraw_x[p+1],userdraw_y[p+1]);\
   context_userdraw.closePath();\
   context_userdraw.stroke();\
  };\
 };\
 return;\
};");

    break;
 /* vline,vlines  16,17 */
  case 16 ... 17:
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_vlines();};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<2;p++){userdraw_x.pop();userdraw_y.pop();};draw_vlines();};return;};\
function vlines(x,y,event_which,num){\
 if(event_which == 0){\
  if( num == 0 ){userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(x);userdraw_y.push(0);\
  userdraw_x.push(x);userdraw_y.push(ysize);\
  draw_vlines();\
 };\
};\
function draw_vlines(){\
 var len = userdraw_x.length;\
 if( len %%2 == 0 ){\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  for(var p = 0 ; p < len ; p = p+2 ){\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(userdraw_x[p],userdraw_y[p]);\
   context_userdraw.lineTo(userdraw_x[p+1],userdraw_y[p+1]);\
   context_userdraw.closePath();\
   context_userdraw.stroke();\
  };\
 };\
 return;};");

    break;
 /* arrow/arrows */
  case 18 ... 19 :
 fprintf(js_include_file,"\
function redraw_userdraw(){draw_arrows();};\
context_userdraw.fillStyle = \"rgba(%s,%.2f)\";\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<2;p++){userdraw_x.pop();userdraw_y.pop();};draw_arrows();};return;};\
function arrows(x,y,event_which,num){\
 if(event_which == 0){\
  if( num == 0 && xy_cnt == 0 ){userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(x);userdraw_y.push(y);\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt == 1 ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   draw_arrows();\
   userdraw_x.pop();userdraw_y.pop();\
  };\
 };\
 if( xy_cnt == 2 ){\
  xy_cnt = 0;\
  draw_arrows();\
 };\
};\
function draw_arrows(){\
 var arrow_head = %d;\
 var len = userdraw_x.length;\
 var x1,y1,x2,y2,dx,dy,h;\
 if( len%%2 == 0 ){\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  for(var p = 0 ; p < len ; p = p+2 ){\
   x1 = userdraw_x[p];y1 = userdraw_y[p];x2 = userdraw_x[p+1];y2 = userdraw_y[p+1];dx = x2 - x1;dy = y2 - y1;\
   context_userdraw.save();\
   context_userdraw.setLineDash([]);\
   context_userdraw.translate(x2,y2);\
   context_userdraw.rotate(Math.atan2(dy,dx));\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(0,0);\
   context_userdraw.lineTo(-1*arrow_head,-0.5*arrow_head);\
   context_userdraw.lineTo(-1*arrow_head, 0.5*arrow_head);\
   context_userdraw.closePath();\
   context_userdraw.fill();\
   context_userdraw.stroke();\
   context_userdraw.restore();\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(x1,y1);\
   context_userdraw.lineTo(x2,y2);\
   context_userdraw.closePath();\
   context_userdraw.stroke();\
  };\
 };\
 return;\
};",stroke_color,stroke_opacity,arrow_head);
    break;
 /* arrow/arrows */
  case 20 ... 21 :
 fprintf(js_include_file,"\
function redraw_userdraw(){draw_arrows2();};\
context_userdraw.fillStyle = \"rgba(%s,%.2f)\";\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<2;p++){userdraw_x.pop();userdraw_y.pop();};draw_arrows();};return;};\
function arrows2(x,y,event_which,num){\
 if(event_which == 0){\
  if( num == 0 && xy_cnt == 0 ){userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(x);userdraw_y.push(y);\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt == 1 ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   draw_arrows2();\
   userdraw_x.pop();userdraw_y.pop();\
  };\
 };\
 if( xy_cnt == 2 ){\
  xy_cnt = 0;\
  draw_arrows2();\
 };\
};\
function draw_arrows2(){\
 var arrow_head = %d;\
 var len = userdraw_x.length;\
 var x1,y1,x2,y2,dx,dy,h;\
 if( len%%2 == 0 ){\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  for(var p = 0 ; p < len ; p = p+2 ){\
   context_userdraw.save();\
   x1 = userdraw_x[p];y1 = userdraw_y[p];x2 = userdraw_x[p+1];y2 = userdraw_y[p+1];dx = x2 - x1;dy = y2 - y1;\
   context_userdraw.save();\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(x1,y1);\
   context_userdraw.lineTo(x2,y2);\
   context_userdraw.closePath();\
   context_userdraw.stroke();\
   context_userdraw.translate(x2,y2);\
   context_userdraw.rotate(Math.atan2(dy,dx));\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(0,0);\
   context_userdraw.lineTo(-1*arrow_head,-0.5*arrow_head);\
   context_userdraw.lineTo(-1*arrow_head, 0.5*arrow_head);\
   context_userdraw.closePath();\
   context_userdraw.fill();\
   context_userdraw.stroke();\
   context_userdraw.restore();\
   context_userdraw.translate(x1,y1);\
   context_userdraw.rotate(Math.atan2(-dy,-dx));\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(0,0);\
   context_userdraw.lineTo(-1*arrow_head,-0.5*arrow_head);\
   context_userdraw.lineTo(-1*arrow_head, 0.5*arrow_head);\
   context_userdraw.closePath();\
   context_userdraw.fill();\
   context_userdraw.stroke();\
   context_userdraw.restore();\
  };\
 };\
 return;\
};",stroke_color,stroke_opacity,arrow_head);
  break;
 /* curvedarrow/curvedarrows */
    case 22 ... 23:
fprintf(js_include_file,"\
function redraw_userdraw(){draw_curvedarrows();};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<3;p++){userdraw_x.pop();userdraw_y.pop();};draw_curvedarrows();};return;};\
function curvedarrows(x,y,event_which,num){\
 if(event_which == 0){\
  if(num == 0 && xy_cnt == 0){\
   userdraw_x = [];userdraw_y = [];userdraw_x[0] = x;userdraw_y[0] = y;\
  }\
  else\
  {\
   userdraw_x.push(x);userdraw_y.push(y);\
  };\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt < 3 ){userdraw_x.push(x);userdraw_y.push(y);draw_curvedarrows();userdraw_x.pop();userdraw_y.pop();};\
  if( xy_cnt == 3 ){userdraw_x.pop();userdraw_y.pop();userdraw_x.push(x);userdraw_y.push(y);xy_cnt = 0;draw_curvedarrows();};\
 };\
};\
var arrow_head = %d;\
function draw_curvedarrows(){\
 var len = userdraw_x.length;var x1,y1,x2,y2,x3,y3;\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 for(var p = 0 ; p < len ; p = p+3){\
  x1 = userdraw_x[p];x2 = userdraw_x[p+1];x3 = userdraw_x[p+2];y1 = userdraw_y[p];y2 = userdraw_y[p+1];y3 = userdraw_y[p+2];\
  var angle1 = Math.atan2(x3 - x2,y3 - y2) + Math.PI;\
  context_userdraw.beginPath();\
  context_userdraw.moveTo(x1,y1);\
  context_userdraw.quadraticCurveTo(x3,y3,x2,y2);\
  context_userdraw.moveTo(x2 - (arrow_head * Math.sin(angle1 - Math.PI / 6)),y2 - (arrow_head * Math.cos(angle1 - Math.PI / 6)));\
  context_userdraw.lineTo(x2, y2);\
  context_userdraw.lineTo(x2 - (arrow_head * Math.sin(angle1 + Math.PI / 6)),y2 - (arrow_head * Math.cos(angle1 + Math.PI / 6)));\
  context_userdraw.stroke();\
  context_userdraw.closePath();\
 };\
 return;};",arrow_head);
    break;
 /* curvedarrow2/curvedarrows2 */
  case 24 ... 25:
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_curvedarrows2();};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<3;p++){userdraw_x.pop();userdraw_y.pop();};draw_curvedarrows2();};return;};\
function curvedarrows2(x,y,event_which,num){\
 if(event_which == 0){\
  if(num == 0 && xy_cnt == 0){userdraw_x = [];userdraw_y = [];userdraw_x[0] = x;userdraw_y[0] = y;}else{userdraw_x.push(x);userdraw_y.push(y);};xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt < 3 ){userdraw_x.push(x);userdraw_y.push(y);draw_curvedarrows2();userdraw_x.pop();userdraw_y.pop();};\
  if( xy_cnt == 3 ){userdraw_x.pop();userdraw_y.pop();userdraw_x.push(x);userdraw_y.push(y);xy_cnt = 0;draw_curvedarrows2();};\
 };\
};\
var arrow_head = %d;\
function draw_curvedarrows2(){\
 var len = userdraw_x.length;var x1,y1,x2,y2,x3,y3;\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 for(var p = 0 ; p < len ; p = p+3){\
  x1 = userdraw_x[p];x2 = userdraw_x[p+1];x3 = userdraw_x[p+2];y1 = userdraw_y[p];y2 = userdraw_y[p+1];y3 = userdraw_y[p+2];\
  var angle1 = Math.atan2(x3 - x2,y3 - y2) + Math.PI;\
  var angle2 = Math.atan2(x3 - x1,y3 - y1) + Math.PI;\
  context_userdraw.beginPath();\
  context_userdraw.moveTo(x1,y1);\
  context_userdraw.moveTo(x1 - (arrow_head * Math.sin(angle2 - Math.PI / 6)),y1 - (arrow_head * Math.cos(angle2 - Math.PI / 6)));\
  context_userdraw.lineTo(x1, y1);\
  context_userdraw.lineTo(x1 - (arrow_head * Math.sin(angle2 + Math.PI / 6)),y1 - (arrow_head * Math.cos(angle2 + Math.PI / 6)));\
  context_userdraw.moveTo(x1,y1);\
  context_userdraw.quadraticCurveTo(x3,y3,x2,y2);\
  context_userdraw.moveTo(x2 - (arrow_head * Math.sin(angle1 - Math.PI / 6)),y2 - (arrow_head * Math.cos(angle1 - Math.PI / 6)));\
  context_userdraw.lineTo(x2, y2);\
  context_userdraw.lineTo(x2 - (arrow_head * Math.sin(angle1 + Math.PI / 6)),y2 - (arrow_head * Math.cos(angle1 + Math.PI / 6)));\
  context_userdraw.stroke();\
  context_userdraw.closePath();\
 };\
 return;\
};",arrow_head);
    break;

 /* freehandline/freehandlines/path/paths 0,1  26,27,28,29  */
  case 26 ... 29:
    fprintf(js_include_file,"\
 var stop = 1;\
function remove_last(){userdraw_x = [];userdraw_y = []; context_userdraw.clearRect(0,0,xsize,ysize);return;};\
function paths(x,y,event_which,num){\
 if(event_which == 0 && stop == 0 ){stop = 1;return;};\
 if(event_which == 1 && stop == 1 ){return;}; \
 if(event_which == 0 && stop == 1 ){\
 stop = 0;userdraw_x = [];userdraw_y = [];return;};\
 userdraw_x.push(x);userdraw_y.push(y);\
 draw_paths();\
};\
function draw_paths(){\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 var len = userdraw_x.length ;\
 context_userdraw.beginPath();\
 context_userdraw.moveTo(userdraw_x[0],userdraw_y[0]);\
 for(var p = 1 ; p < len ; p++ ){\
  context_userdraw.lineTo(userdraw_x[p],userdraw_y[p]);\
 };\
 if(use_filled!=0){context_userdraw.fill();};\
 context_userdraw.stroke();\
 return;\
};");

    break;

 /* poly/polys poly[3]...polys[9] 30,31 */
  case 30 ... 31:
    fprintf(js_include_file,"\
var polynum = %d;\
function redraw_userdraw(){draw_polys();};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<polynum;p++){userdraw_x.pop();userdraw_y.pop();};draw_polys();};return;};\
function polys(x,y,event_which,num){\
 if(event_which == 0){\
  if(num == 0 && xy_cnt == 0){\
   userdraw_x = [];userdraw_y = [];\
   userdraw_x[0] = x;userdraw_y[0] = y;\
  }\
  else\
  {\
   userdraw_x.push(x);userdraw_y.push(y);\
  };\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt < polynum ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   draw_polys();\
   userdraw_x.pop();userdraw_y.pop();\
  };\
 };\
 if( xy_cnt == polynum ){\
  userdraw_x.pop();userdraw_y.pop();\
  userdraw_x.push(x);userdraw_y.push(y);\
  xy_cnt = 0;\
  draw_polys();\
 };\
};\
function draw_polys(){\
 var len = userdraw_x.length;\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 for(var p = 0 ; p < len ; p = p+polynum){\
  context_userdraw.beginPath();\
  context_userdraw.moveTo(userdraw_x[p],userdraw_y[p]);\
  for( var m = p+1 ;m < p+polynum ; m++){\
  if(typeof(userdraw_x[m]) !== 'undefined' ){context_userdraw.lineTo(userdraw_x[m],userdraw_y[m]);};};\
  context_userdraw.lineTo(userdraw_x[p],userdraw_y[p]);\
  context_userdraw.stroke();\
  context_userdraw.closePath();\
  if(use_filled!=0){context_userdraw.fill();};\
 };\
 return;\
};",polynum);
  break;
    case 32 ... 33:
 /* polygon/polygon  32,33 */
 /* multiple polygons are now working as expected : no problems with duplicated/closing points*/
 fprintf(js_include_file,"\
var poly_closed = [];\
var do_not_pop = 0;\
function redraw_userdraw(){do_not_pop = 1;draw_polygon();};\
function remove_last(){var len = poly_closed.length;if(len > 1 ){var last = poly_closed[len-1]-poly_closed[len-2];for(var p=0;p<last-1;p++){userdraw_x.pop();userdraw_y.pop();};poly_closed.pop();}else{poly_closed = [];userdraw_x = [];userdraw_y = [];};draw_polygon();return;};\
function check_closed(x1,y1,X,Y){\
 var marge=10;\
 var len = X.length-1;\
 for(var p = 0 ; p < len ; p++){\
  if(x1 < X[p] + marge && x1 > X[p] - marge ){\
   if(y1 < Y[p] + marge && y1 > Y[p] - marge ){\
    return 1;\
   };\
  };\
 };\
 return 0;\
};\
\
function polygon(x,y,event_which,num){\
 do_not_pop = 0;\
 if(event_which == 0){\
  if(num == 0 && xy_cnt == 0){\
   userdraw_x = [];userdraw_y = [];\
   userdraw_x[0] = x;userdraw_y[0] = y;\
  }\
  else\
  {\
   userdraw_x.push(x);userdraw_y.push(y);\
  };\
  xy_cnt++;\
  if( xy_cnt > 2 ){\
   if( check_closed(x,y,userdraw_x,userdraw_y) == 1){\
    poly_closed.push(userdraw_x.length - 1);\
    draw_polygon();\
    xy_cnt = 0;\
   };\
  };\
 }\
 else\
 {\
  if( xy_cnt > 0 ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   draw_polygon();\
  };\
 };\
};\
function draw_polygon(){\
 var len = userdraw_x.length;\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 context_userdraw.beginPath();\
 var begin = 0;\
 context_userdraw.moveTo(userdraw_x[0],userdraw_y[0]);\
 for( var p = 0 ; p < len ; p++){\
  if( poly_closed.indexOf(p) != -1 ){\
   context_userdraw.lineTo(userdraw_x[begin],userdraw_y[begin]);\
   begin = p;\
   context_userdraw.closePath();\
   if(use_filled!=0){context_userdraw.fill();};\
   context_userdraw.stroke();\
   context_userdraw.beginPath();\
  }else{ context_userdraw.lineTo(userdraw_x[p],userdraw_y[p]);};\
 };\
 context_userdraw.lineTo(userdraw_x[begin],userdraw_y[begin]);\
 context_userdraw.closePath();\
 if(use_filled!=0){context_userdraw.fill();};\
 context_userdraw.stroke();\
 if(do_not_pop == 0){ userdraw_x.pop();userdraw_y.pop();};\
 return;\
};");
    break;

 /*
 polyline/brokenline 34,35
 there is just a single polyline/brokenline possible !
 */
  case 34 ... 35:
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_polylines();};\
function remove_last(){if( userdraw_x.length > 0 ){userdraw_x.pop();userdraw_y.pop();draw_polylines();};return;};\
function polylines(x,y,event_which,num){\
 if(event_which == 0){\
  if(num == 0  && xy_cnt == 0 ){ userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(x);userdraw_y.push(y);\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt == 1 ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   userdraw_x.pop();userdraw_y.pop();\
  };\
 };\
 if( xy_cnt == 2 ){xy_cnt = 0;};\
 draw_polylines();\
};\
var full_circle = 2*Math.PI;\
function draw_polylines(){\
 var len = userdraw_x.length;\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 context_userdraw.beginPath();\
 context_userdraw.moveTo(userdraw_x[0],userdraw_y[0]);\
 for( var p = 0 ; p < len ; p++){\
  context_userdraw.lineTo(userdraw_x[p],userdraw_y[p]);\
  context_userdraw.arc(userdraw_x[p],userdraw_y[p],2,0,full_circle,false);\
 };\
  context_userdraw.stroke();\
 context_userdraw.closePath();\
 if(use_filled != 0 ){if(use_filled!=0){context_userdraw.fill();};};\
 return;\
};");
    break;

 /* triangle/triangles 36,37 */
  case 36 ... 37:
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_triangles();};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<3;p++){userdraw_x.pop();userdraw_y.pop();};draw_triangles();};return;};\
function triangles(x,y,event_which,num){\
 var last = userdraw_x.length - 1;\
 if(event_which == 0){\
  if(num == 0 && xy_cnt == 0){\
   userdraw_x = [];userdraw_y = [];userdraw_x[0] = x;userdraw_y[0] = y;\
  }\
  else\
  {\
   userdraw_x.push(x);userdraw_y.push(y);\
  };\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt < 3 ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   draw_triangles();\
   userdraw_x.pop();userdraw_y.pop();\
   return;\
  };\
 };\
 if( xy_cnt == 3 ){\
  userdraw_x.pop();userdraw_y.pop();\
  userdraw_x.push(x);userdraw_y.push(y);\
  xy_cnt = 0;\
  draw_triangles();\
 };\
};\
function draw_triangles(){\
 var len = userdraw_x.length - 1;\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 for(var p = 0 ; p < len ; p = p+3){\
  context_userdraw.beginPath();\
  context_userdraw.moveTo(userdraw_x[p],userdraw_y[p]);\
  for( var m = p+1 ;m < p+3 ; m++){\
   context_userdraw.lineTo(userdraw_x[m],userdraw_y[m]);\
  };\
  context_userdraw.lineTo(userdraw_x[p],userdraw_y[p]);\
  context_userdraw.closePath();\
  if(use_filled!=0){context_userdraw.fill();};\
  context_userdraw.stroke();\
 };\
 return;\
};");
    break;

 /* rect/rects 38,39 : NOT USING A PATH !!*/
  case 38 ... 39 :
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_rects();};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<2;p++){userdraw_x.pop();userdraw_y.pop();};draw_rects();};return;};\
function rects(x,y,event_which,num){\
 if(event_which == 0){\
  if( num == 0 && xy_cnt == 0 ){userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(x);userdraw_y.push(y);\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt == 1 ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   draw_rects();\
   userdraw_x.pop();userdraw_y.pop();\
  };\
 };\
 if( xy_cnt == 2 ){\
  xy_cnt = 0;\
  draw_rects();\
 };\
};\
function draw_rects(){\
 var len = userdraw_x.length;\
 if( len %%2 == 0 ){\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  for(var p = 0 ; p < len ; p = p+2 ){\
   context_userdraw.beginPath();\
   context_userdraw.rect(userdraw_x[p],userdraw_y[p],userdraw_x[p+1]-userdraw_x[p],userdraw_y[p+1]-userdraw_y[p]);\
   context_userdraw.closePath();\
   if(use_filled!=0){context_userdraw.fill();};\
   context_userdraw.stroke();\
  };\
 };\
 return;\
};");
    break;
 /* roundrect/roundrects 40,41  : NOT USING A PATH !!*/
  case 40 ... 41:
 fprintf(js_include_file,"\
function redraw_userdraw(){draw_roundrects();};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<2;p++){userdraw_x.pop();userdraw_y.pop();};draw_roundrects();};return;};\
function roundrects(x,y,event_which,num){\
 if(event_which == 0){\
  if( num == 0 && xy_cnt == 0 ){userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(x);userdraw_y.push(y);\
  xy_cnt++;\
 }\
 else\
 {\
  if( xy_cnt == 1 ){\
   userdraw_x.push(x);userdraw_y.push(y);\
   draw_roundrects();\
   userdraw_x.pop();userdraw_y.pop();\
  };\
 };\
 if( xy_cnt == 2 ){\
  xy_cnt = 0;\
  draw_roundrects();\
 };\
};\
function draw_roundrects(){\
 var len = userdraw_x.length;\
 if( len %%2 == 0 ){\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  var x,y,w,h,r;\
  for(var p = 0; p < len; p = p+2){\
   x = userdraw_x[p];y = userdraw_y[p];w = userdraw_x[p+1] - x;h = userdraw_y[p+1] - y;r = parseInt(0.1*w);\
   context_userdraw.beginPath();\
   context_userdraw.moveTo(x + r, y);\
   context_userdraw.lineTo(x + w - r, y);\
   context_userdraw.quadraticCurveTo(x + w, y, x + w, y + r);\
   context_userdraw.lineTo(x + w, y + h - r);\
   context_userdraw.quadraticCurveTo(x + w, y + h, x + w - r, y + h);\
   context_userdraw.lineTo(x + r, y + h);\
   context_userdraw.quadraticCurveTo(x, y + h, x, y + h - r);\
   context_userdraw.lineTo(x, y + r);\
   context_userdraw.quadraticCurveTo(x, y, x + r, y);\
   context_userdraw.closePath();\
   if(use_filled!=0){context_userdraw.fill();};\
   context_userdraw.stroke();\
  };\
 };\
return;\
};");
    break;
 /* arc/arcs  42,43 */
  case 42 ... 43:
 fprintf(js_include_file,"\
function redraw_userdraw(){draw_arcs();};\
var temp_point_x = new Array(3);\
var temp_point_y = new Array(3);\
context_userdraw.fillStyle = context_userdraw.strokeStyle;\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<3;p++){userdraw_x.pop();userdraw_y.pop();userdraw_radius.pop();temp_point_x = [];temp_point_y = [];};draw_arcs();};return;};\
function arcs(x,y,event_which,num){\
 var last = userdraw_x.length - 1;\
 var xc = userdraw_x[last];\
 var yc = userdraw_y[last];\
 if(event_which == 0){\
  if(num == 0 && xy_cnt == 3 ){xy_cnt = 0;userdraw_x = [];userdraw_y = [];};\
  userdraw_x.push(x);userdraw_y.push(y);\
  temp_point_x.push(x);\
  temp_point_y.push(y);\
  draw_points();\
  xy_cnt++;\
 };\
 if( xy_cnt%%3 == 0 ){ draw_arcs();temp_point_x = [];temp_point_y = [];};\
 return;\
};\
function draw_points(){\
 var len = temp_point_x.length;\
 var radius = 4*(context_userdraw.lineWidth);\
 var r = 2*Math.PI;\
 for(var p = 0 ; p < len ; p++ ){\
  context_userdraw.beginPath();\
  context_userdraw.arc(temp_point_x[p],temp_point_y[p],radius,0,r,false);\
  context_userdraw.closePath();\
  context_userdraw.fill();\
 };\
 return;\
};\
function draw_arcs(){\
 var xc,x1,x2,yc,y1,y2,radius,end,start,direction,p1,p2;\
 var len = userdraw_x.length;\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 var idx = 0;\
 for(var p=0; p< len;p = p+3){\
  p1=p+1;p2=p+2;\
  xc = userdraw_x[p1];x1 = userdraw_x[p];x2 = userdraw_x[p2];yc = userdraw_y[p1];y1 = userdraw_y[p];y2 = userdraw_y[p2];\
  radius = distance(x1,y1,xc,yc);\
  start = Math.atan2(y1-yc, x1-xc);\
  end = Math.atan2(y2-yc, x2-xc);\
  direction = ( start > end);\
  userdraw_radius[idx] = Math.abs( end - start );idx++;\
  context_userdraw.beginPath();\
  context_userdraw.moveTo(xc,yc);\
  context_userdraw.arc(xc, yc,radius,start,end,direction);\
  context_userdraw.closePath();\
  context_userdraw.stroke();\
  if(use_filled!=0){context_userdraw.fill();};\
 };\
 return;\
};");
    break;
 /* text : 44 always uses user input field !! */
  case 44:
 fprintf(js_include_file,"\
function redraw_userdraw(){draw_text();};\
var userdraw_text = new Array();\
var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
var inner_html = tooltip_div.innerHTML;\
tooltip_div.innerHTML = inner_html + \"<input class='%s' type='text' size='4' value='' id='userdraw_text_abc'>\";\
context_userdraw.fillStyle = \"rgba(%s,%f)\";\
var userdraw_textheight = 0.3 * (context_userdraw.measureText('M').width);\
function remove_last(){if(userdraw_x.length > 0 ){userdraw_x.pop();userdraw_y.pop();userdraw_text.pop();draw_text();return;};};\
function text(x,y,event_which,num){\
 if(event_which == 1){ return; };\
 if( num == 0 ){\
  userdraw_x[0] = x;\
  userdraw_y[0] = y;\
 }else{\
  userdraw_x.push(x);\
  userdraw_y.push(y);\
 };\
 var txt = document.getElementById('userdraw_text_abc').value;\
 if( ! txt ){userdraw_x.pop();userdraw_y.pop();userdraw_text.pop();return;};\
 txt = conv_to_unicode(txt);userdraw_text.push(txt.replace(/\\\\/g,' '));\
 draw_text();\
};\
function conv_to_unicode(str){return str.replace(/\\u[\\dA-F]{4}/gi,function(match){return String.fromCharCode(parseInt(match.replace(/\\u/g,''), 16));});};\
function draw_text(){\
 var len = userdraw_x.length;\
 var half = 0;\
 var txt;\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 for(var p = 0 ; p < len ; p++ ){\
   txt = userdraw_text[p];\
   if(txt.indexOf('_') > 0 || txt.indexOf('^') > 0 ){draw_subsup(context_userdraw,userdraw_x[p],userdraw_y[p],txt,4);}else{half = 0.5*( context_userdraw.measureText(txt).width );context_userdraw.fillText(txt,userdraw_x[p] - half,userdraw_y[p] + userdraw_textheight);};\
 };\
};",canvas_root_id,css_class,stroke_color,stroke_opacity);
    break;
 /* images 45,46  replyformat is same as 'userdraw text,color' e.g. 17 === x,y,image_id  */
  case 45 ... 46:
    fprintf(js_include_file,"\
 function redraw_userdraw(){draw_images(0);};\
var userdraw_text = new Array();\
var current_id = null;var external_div_cnt=0;\
function remove_last(){draw_images(1);return;};\
function image_adjust(image,x,y){\
 var centered = %d;\
 var w = parseInt(image.width);var h = parseInt(image.height);\
 switch(centered){\
  case 0: return [x,y];break;\
  case 1: return [x,parseInt(y-0.5*h)];break;\
  case 2: return [parseInt(x+0.5*h),y];break;\
  case 3: return [parseInt(x+0.5*h),parseInt(y-0.5*h)];break;\
  case 4: return [parseInt(x-0.5*w),parseInt(y-0.5*h)];break;\
  default: return [x,y];break;\
 };\
};\
place_image_on_canvas = function(id){\
 var thing = document.getElementById(id);\
 var tag = thing.tagName;\
 if(tag == 'SVG'){draw_mathml_svg(thing,id);return;};\
 if(tag == 'DIV' || tag == 'SPAN' || tag == 'P' || tag == 'TD' || tag == 'TH' ){draw_mathml_div(thing,id);return;};\
 var src = thing.src;\
 var image = new Image();\
 image.src = src;\
 image.id = 'placed_'+id;\
 image.width = thing.width;\
 image.height = thing.height;\
 image.onload = function(){ current_id = id; };\
 return;\
};\
function draw_mathml_div(thing,id){\
 var fix_div = document.createElement('DIV');\
 var new_id='placed_'+external_div_cnt+'_'+id;\
 fix_div.setAttribute('id',new_id);\
 var w = parseInt(thing.clientWidth);\
 var h = parseInt(thing.clientHeight);\
 fix_div.innerHTML = thing.innerHTML;\
 fix_div.setAttribute('style','display:none;position;absolute;width:'+w+'px;height:'+h+'px');\
 fix_div.width = w;fix_div.height = h;\
 canvas_div.appendChild(fix_div);\
 current_id = new_id;\
 external_div_cnt++;\
 return;\
};\
function draw_mathml_svg(thing,id){\
 var fix_div = document.createElement('DIV');\
 fix_div.setAttribute('style','display:none;position;relative');\
 canvas_div.appendChild(fix_div);\
 var image = new Image();\
 var svg_string = new XMLSerializer().serializeToString(thing);\
 var dom = self.URL || self.webkitURL || self;\
 var svg = new Blob([svg_string], {type: \"image/svg+xml;charset=utf-8\"});\
 var url = dom.createObjectURL(svg);\
 image.src= url;\
 image.id = 'placed_'+id;\
 image.onload = function(){\
  current_id = image.id;\
  fix_div.innerHTML='<img src='+image.src+' id='+image.id+' alt=\"this should not happen today...!\">';\
 };\
 return;\
};\
function images(x,y,event_which,num){\
 if(event_which == 1){ return;};\
 if( current_id ){\
  if(num == 0 ){ \
   userdraw_x[0] = x;\
   userdraw_y[0] = y;\
   userdraw_text[0] = current_id;\
   external_div_cnt = 0;\
  }else{\
   userdraw_x.push(x);\
   userdraw_y.push(y);\
   userdraw_text.push(current_id);\
  };\
  current_id = null;\
  draw_images(0);\
 };\
};\
function draw_images(remove){\
 var last = userdraw_x.length - 1;\
 if(last < 0 ){return;};\
 var x = userdraw_x[last];\
 var y = userdraw_y[last];\
 var id =  userdraw_text[last];\
 var img = document.getElementById(id);\
 var tag = img.tagName;\
 var xy = image_adjust(img,x,y);\
 if( tag == 'DIV' || tag == 'SPAN' || tag == 'P' || tag == 'TD' || tag == 'TH'){\
  if( remove == 1 ){\
   img.setAttribute('style','display:none');\
   userdraw_x.pop();userdraw_y.pop();userdraw_text.pop();external_div_cnt--;\
  }\
  else\
  {\
   img.setAttribute('style','display:inline-block;position:absolute;top:'+xy[1]+'px;left:'+xy[0]+'px;');\
  };\
  return;\
 }else{\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  if(remove == 1){\
   userdraw_x.pop();userdraw_y.pop();userdraw_text.pop();\
  };\
  var len = userdraw_x.length;\
  for(var p = 0 ; p < len; p++){\
   if( userdraw_text[p] ){\
    img = document.getElementById(userdraw_text[p]);\
    tag = img.tagName;\
    xy = image_adjust(img,userdraw_x[p],userdraw_y[p]);\
    if( tag == 'IMG' || tag == 'SVG' ){\
     context_userdraw.drawImage(img,xy[0],xy[1],img.width,img.height);\
    };\
   };\
  };\
 };\
};",use_offset);
    break;
 /* input/inputs" 46,47
 if(use_offset != 0 ){ center_input('canvas_input'+input_cnt,x,y,style);};\
  inp.addEventListener(\"touchstart\", function(e){this.focus();},false);\
  if( wims_status == \"done\" ){ inp.setAttribute(\"readonly\",\"readonly\");};
 */
  case 47 ... 48:
 /*
 input_cnt keeps track of all inputfields INCLUDING static inputfields
 xy_cnt keeps track of the dynamically added userdraw inputfields
 */
    fprintf(js_include_file,"\
var current_id = null;\
var current_obj = null;\
var inputs_width = 16;\
var inputs_height = 8;\
function redraw_userdraw(){console.log('userdraw inputs not zooming...');};\
if(typeof(input_cnt) !== 'undefined'){var input_cnt = 0;};\
function remove_last(){console.log('input_cnt ='+input_cnt);\
var input = document.getElementById('canvas_input'+input_cnt);try{ canvas_div.removeChild(input);userdraw_x.pop();userdraw_y.pop();xy_cnt--;input_cnt--;}catch(e){console.log('removal failed...'+e);};};\
function check_overlap(x,y,obj){\
 var inputs;var xi,yi;\
 for(var p = 0 ; p < xy_cnt ; p++ ){\
  xi = userdraw_x[p];yi = userdraw_y[p];\
  if( x > xi - 5 && x < xi + inputs_width &&  y > yi-5 && y < yi + inputs_height){\
   try{canvas_div.removeChild(obj);\
    current_obj = null;userdraw_x.pop();userdraw_y.pop();xy_cnt--;input_cnt--;\
   }catch(e){console.log('failed removing overlap: '+e);};\
   return;\
  };\
 };\
 return;\
};\
function inputs(x,y,event_which,num){\
 if( document.getElementById('canvas_input0')){\
  var inputs = document.getElementById('canvas_input0');\
  inputs_width = 5+parseInt(inputs.clientWidth);\
  inputs_height = 5+ parseInt(inputs.clientHeight);\
 };\
 if(event_which == 0){\
  if(num == 0 && xy_cnt == 1 ){try{ canvas_div.removeChild(document.getElementById('canvas_input0'));\
  userdraw_x = [];userdraw_y = [];xy_cnt=0;}catch(e){console.log('removal first input failed...'+e);};};\
  userdraw_x.push(x);userdraw_y.push(y);\
  current_id = 'canvas_input'+input_cnt;\
  current_obj = document.getElementById(current_id);\
  draw_inputs(x,y);\
  if(input_cnt > 0){check_overlap(x,y,current_obj);};\
  input_cnt++;\
  xy_cnt++;\
  if(isTouch){current_obj.addEventListener(\"touchstart\", function(e){this.focus();},false);}else{\
  current_obj.autofocus = true;};\
 };\
 return;\
};\
function draw_inputs(x,y){\
 var input_size = parseInt(context_userdraw.lineWidth);\
 current_obj = document.createElement(\"input\");\
 current_obj.setAttribute(\"id\",current_id);\
 current_obj.setAttribute(\"size\",input_size);\
 var xx = x - 0.5*inputs_width;\
 var yy = y - 0.5*inputs_height;\
 current_obj.setAttribute(\"style\",\"position:absolute;left:\"+xx+\"px;top:\"+yy+\"px;\");\
 canvas_div.appendChild(current_obj);\
 return;\
};");
    break;
// current_obj.setAttribute(\"class\",\"%s\");

 // input.setAttribute(\"style\",\"position:absolute;left:\"+userdraw_x[p]+\"px;top:\"+userdraw_y[p]+\"px;%s\");
 /* gridfill/diamondfill/dotfill/hatchfill/textfill  48,49,50,51,52
  0:no fill, 1:fill,2=grid?,3=hatch?,4=diamond?,5=dot?,6=image?
 if( draw_num == 50 ){js_function[DRAW_GRIDFILL] = 1;use_filled = 2;}
 else{
 if( draw_num == 51 ){js_function[DRAW_DIAMONDFILL] = 1;use_filled = 4;}
 else{
 if( draw_num == 52 ){js_function[DRAW_DOTFILL] = 1;use_filled = 5;}
 else{
 if( draw_num == 53 ){js_function[DRAW_HATCHFILL] = 1;use_filled = 3;}
 else{
 if( draw_num == 54 ){js_function[DRAW_TEXTFILL] = 1;use_filled = 7;}
 else{ use_filled = 1;}}}}}
 */
  case 49 ... 54:
/* handling rare case of clickfill using a pattern instead of a single color...*/
    add_js_filltoborder(canvas_type);
 /*fprintf(stdout,"USE_FILLED = %d<br>",use_filled);*/
    tmp_buffer = my_newmem(MAX_BUFFER);
    switch(use_filled){
      case 1:
        check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"var use_pattern_ctx = false;"));
        break;
      case 2:
        check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"draw_gridfill(%d,0,0,%d,%d,%d,'%s',%d,xsize,ysize,1);var use_pattern_ctx = true;var pattern_ctx = grid_fill_pattern;",CLICKFILL_CANVAS,5*line_width,5*line_width,line_width,stroke_color,(int) (fill_opacity/0.0039215)));
        break;
      case 3:
        check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"draw_hatchfill(%d,0,0,%d,%d,%d,'%s',%d,xsize,ysize,1);var use_pattern_ctx = true;var pattern_ctx = hatch_fill_pattern;",CLICKFILL_CANVAS,5*line_width,5*line_width,line_width,stroke_color,(int) (fill_opacity/0.0039215)));
        break;
      case 4:
        check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"draw_diamondfill(%d,0,0,%d,%d,%d,'%s',%d,xsize,ysize,1);var use_pattern_ctx = true;var pattern_ctx = diamond_fill_pattern;",CLICKFILL_CANVAS,5*line_width,5*line_width,line_width,stroke_color,(int) (fill_opacity/0.0039215)));
        break;
      case 5:
        check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"draw_dotfill(%d,0,0,%d,%d,%d,'%s',%d,xsize,ysize,1);var use_pattern_ctx = true;var pattern_ctx = dot_fill_pattern;",CLICKFILL_CANVAS,5*line_width,5*line_width,line_width,stroke_color,(int) (fill_opacity/0.0039215)));
        break;
      case 6:
        check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"draw_imagefill(%d,0,0,%d,%d,%d,'%s',%d,xsize,ysize,1);var use_pattern_ctx = true;var pattern_ctx = image_fill_pattern;",CLICKFILL_CANVAS,5*line_width,5*line_width,line_width,stroke_color,(int) (fill_opacity/0.0039215)));
        break;
      case 7:
/*var draw_textfill = function(canvas_type,x0,y0,color,fontfamily,xsize,ysize,txt,use_userdraw)*/
        check_string_length(snprintf(tmp_buffer,MAX_BUFFER,"draw_textfill(%d,0,0,'%s','%s',xsize,ysize,userdraw_text_string,1);var use_pattern_ctx = true;var pattern_ctx = text_fill_pattern;",CLICKFILL_CANVAS,stroke_color,font_family));
        break;
      }
      add_to_buffer(tmp_buffer);

      fprintf(js_include_file,"function redraw_userdraw(){console.log('userdraw fill not zooming...');return;};\
function remove_last(){var last = fill_canvas_no;\
console.log('last = '+last);\
if(last > %d){\
var last_canvas = document.getElementById('wims_canvas%d'+last);\
if( last_canvas == null ){fill_canvas_no--;return;};\
var last_ctx = last_canvas.getContext('2d');\
last_ctx.clearRect(0,0,xsize,ysize);\
canvas_div.removeChild(last_canvas);\
userdraw_x.pop();userdraw_y.pop();\
fill_canvas_no--;\
};return;};\
if( typeof(fill_canvas_no) === 'undefined' ){ var fill_canvas_no = %d; };\
function clickfill(x,y,event_which,num){\
 var opacity = %d;\
 var color = [%s,opacity];\
 if( typeof(multifillcolors) === 'object'){\
  var numx = userdraw_x.length;\
  var numc = multifillcolors.length-1;var num = numx%%numc;\
  if( ! multifillcolors[num] ){alert('first select a colour...');return;};\
  var tc = (multifillcolors[num]).split(',');\
  color = [ tc[0],tc[1],tc[2],opacity ];\
  if( typeof( palettecolors ) === 'object'){\
   var idx = palettecolors.indexOf( multifillcolors[num]);\
   userdraw_radius.push(idx);\
  }else{ userdraw_radius.push(num);};\
 }\
 else\
 {\
  userdraw_radius.push(0);\
 };\
 document.body.style.cursor = 'wait';\
 if( use_pattern_ctx ){\
  setTimeout(function(){ filltoborder( px2x(x),px2y(y),false,color,fill_canvas_no,true,pattern_ctx);},500);\
 }\
 else\
 {\
  setTimeout(function(){ filltoborder(px2x(x),px2y(y),false,color,fill_canvas_no,false,null);},500);\
 };\
 userdraw_x.push(x);\
 userdraw_y.push(y);\
 fill_canvas_no++;\
 document.body.style.cursor = 'default';\
 return;\
};",CLICKFILL_CANVAS,canvas_root_id,CLICKFILL_CANVAS,(int)(255*fill_opacity),stroke_color);
/*(int) (stroke_opacity/0.0039215),(int) (stroke_opacity/0.0039215) */
    break;
 /* parallelogram/parallelograms */
  case 55 ... 56:
    fprintf(js_include_file,"\
function redraw_userdraw(){draw_parallelograms();};\
function remove_last(){if( userdraw_x.length > 0 ){for(var p=0;p<4;p++){userdraw_x.pop();userdraw_y.pop();};draw_parallelograms();};return;};\
function parallelograms(x,y,event_which,num){\
 var l2 = userdraw_x.length;\
 var l1 = l2 - 1;var l0 = l2 - 2;\
 var xxyy;\
 if(event_which == 0){\
  if(xy_cnt == 0){\
   if(num == 0){userdraw_x = [];userdraw_y = [];};\
   userdraw_x.push(x);userdraw_y.push(y);\
  }\
  else\
  {\
   userdraw_x.push(x);userdraw_y.push(y);\
   if(xy_cnt == 2){\
    var xy = multisnap_check(userdraw_x[l2] - userdraw_x[l1] + userdraw_x[l0],userdraw_y[l2] - userdraw_y[l1] + userdraw_y[l0],use_snap);\
    userdraw_x.push(xy[0]);\
    userdraw_y.push(xy[1]);\
   };\
  };\
  xy_cnt++;\
 }\
 else\
 {\
  if(xy_cnt == 1){\
   var xxyy = multisnap_check(userdraw_x[l1],userdraw_y[l1],use_snap);\
   userdraw_x.push(xxyy[0]);\
   userdraw_y.push(xxyy[1]);\
   userdraw_x.push(x);\
   userdraw_y.push(y);\
   draw_parallelograms();\
   userdraw_x.pop();userdraw_y.pop();\
   userdraw_x.pop();userdraw_y.pop();\
  }\
  else\
  {\
   if(xy_cnt == 2){\
    xxyy = multisnap_check(userdraw_x[l2]-userdraw_x[l1] + userdraw_x[l0],userdraw_y[l2]-userdraw_y[l1] + userdraw_y[l0],use_snap);\
    userdraw_x.push(x);userdraw_y.push(y);\
    userdraw_x.push(xxyy[0]);\
    userdraw_y.push(xxyy[1]);\
    draw_parallelograms();\
    userdraw_x.pop();userdraw_y.pop();\
    userdraw_x.pop();userdraw_y.pop();\
   };\
  };\
 };\
 if( xy_cnt == 3 ){\
  userdraw_x.pop();userdraw_y.pop();\
  xxyy = multisnap_check(userdraw_x[l2]-userdraw_x[l1] + userdraw_x[l0],userdraw_y[l2]-userdraw_y[l1] + userdraw_y[l0],use_snap);\
  userdraw_x.push(xxyy[0]);userdraw_y.push(xxyy[1]);\
  userdraw_x.push(x);userdraw_y.push(y);\
  userdraw_x.pop();userdraw_y.pop();\
  xy_cnt = 0;\
  draw_parallelograms();\
 };\
};\
function draw_parallelograms(){\
 var len = userdraw_x.length-1;\
 context_userdraw.clearRect(0,0,xsize,ysize);\
 for(var p = 0 ; p < len ; p = p+4){\
  context_userdraw.beginPath();\
  context_userdraw.moveTo(userdraw_x[p],userdraw_y[p]);\
  for( var m = p+1 ;m < p+4 ; m++){\
   context_userdraw.lineTo(userdraw_x[m],userdraw_y[m]);\
  };\
  context_userdraw.lineTo(userdraw_x[p],userdraw_y[p]);\
  context_userdraw.closePath();\
  if(use_filled!=0){context_userdraw.fill();};\
  context_userdraw.stroke();\
 };\
 return;};");
    break;

  case 57 ... 58:
    if(js_function[DRAW_JSFUNCTION] != 1 ){
      js_function[JS_RAWMATH] = 1;js_function[DRAW_JSFUNCTION] = 1;
      js_function[JS_MATH] = 1;js_function[JS_PLOT] = 1;
      if(reply_format == 0 || reply_format != -1){reply_format = 24;}/* read canvas_input values */
      if(polynum == -1 ){polynum = 1; } /* functions5 --> polynum = 5 */
      for(n=0;n<polynum;n++){
        add_input_jsfunction(css_class,function_label,input_cnt,stroke_color,stroke_opacity,
        line_width,use_dashed,dashtype[0],dashtype[1],font_size);
        input_cnt++;
        jsplot_cnt++;
      }
    }
    fprintf(js_include_file,"if(typeof(all_jsplots) !== 'number'){var all_jsplots;};all_jsplots = %d;function redraw_userdraw(){redraw_jsplot();return;};",jsplot_cnt);
    break;

  default:canvas_error("unknown drawtype?? this should not happen...");break;
  }/* einde switch */

}/* einde add_js_userdraw */
