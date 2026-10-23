#include "canvasdraw.h"

/* What reply format in read_canvas();

note: if userdraw is combined with inputfields...every "userdraw" based answer will append "\n" and inputfield.value()
1 = x1,x2,x3,x4....x_n
    y1,y2,y3,y4....y_n

    x/y in pixels

2 = x1,x2,x3,x4....x_n
    y1,y2,y3,y4....y_n
    x/y in xrange / yrange coordinate system

3 = x1,x2,x3,x4....x_n
    y1,y2,y3,y4....y_n
    r1,r2,r3,r4....r_n

    x/y in pixels
    r in pixels

4 = x1,x2,x3,x4....x_n
    y1,y2,y3,y4....y_n
    r1,r2,r3,r4....r_n

    x/y in xrange / yrange coordinate system
    r in pixels

5 = Ax1,Ax2,Ax3,Ax4....Ax_n
    Ay1,Ay2,Ay3,Ay4....Ay_n
    Bx1,Bx2,Bx3,Bx4....Bx_n
    By1,By2,By3,By4....By_n
    Cx1,Cx2,Cx3,Cx4....Cx_n
    Cy1,Cy2,Cy3,Cy4....Cy_n
    ....
    Zx1,Zx2,Zx3,Zx4....Zx_n
    Zy1,Zy2,Zy3,Zy4....Zy_n

    x/y in pixels

6 = Ax1,Ax2,Ax3,Ax4....Ax_n
    Ay1,Ay2,Ay3,Ay4....Ay_n
    Bx1,Bx2,Bx3,Bx4....Bx_n
    By1,By2,By3,By4....By_n
    Cx1,Cx2,Cx3,Cx4....Cx_n
    Cy1,Cy2,Cy3,Cy4....Cy_n
    ....
    Zx1,Zx2,Zx3,Zx4....Zx_n
    Zy1,Zy2,Zy3,Zy4....Zy_n

    x/y in xrange / yrange coordinate system

7 = x1:y1,x2:y2,x3:y3,x4:y4...x_n:y_n

    x/y in pixels

8 = x1:y1,x2:y2,x3:y3,x4:y4...x_n:y_n

    x/y in xrange / yrange coordinate system

9 = x1:y1:r1,x2:y2:r2,x3:y3:r3,x4:y4:r3...x_n:y_n:r_n

    x/y in pixels

10 = x1 ; y1 ;r1 \n x2;y2;r2 \n x3;y3;r3 \n ...x_n:y_n:r_n \n

    x/y in xrange / yrange coordinate system
    r is userdraw_radius

11 = Ax1,Ay1,Ax2,Ay2
     Bx1,By1,Bx2,By2
     Cx1,Cy1,Cx2,Cy2
     Dx1,Dy1,Dx2,Dy2
     ......
     Zx1,Zy1,Zx2,Zy2

    x/y in xrange / yrange coordinate system

12 = Ax1,Ay1,Ax2,Ay2
     Bx1,By1,Bx2,By2
     Cx1,Cy1,Cx2,Cy2
     Dx1,Dy1,Dx2,Dy2
     ......
     Zx1,Zy1,Zx2,Zy2

    x/y in pixels

13 = removed
14 = Ax1:Ay1:Ax2:Ay2,Bx1:By1:Bx2:By2....Zx1:Zy1:Zx2:Zy2
    x/y in pixels
15 = reply from inputfields,textareas
    reply1,reply2,reply3,...,reply_n
    only fields set write (e.g. will not read readonly inputfield values.

16 = read mathml inputfields only

17 = read userdraw text only (x1,y1,text1\nx2,y2,text2..\n.x_n,y_n,text_n
 when ready: calculate size_t of string via snprintf(NULL,0,"blah blah...");

18 = read clock(s): H1:M1:S1,H2:M2:S2,...H_n:M_n:S_n
19 = return clicked object number (analogue to shape-library onclick)
20 = return x/y-data in x-range/y-range of all ''draggable`` images
21 = return verbatim coordinates (x1:y1) (x2:y2)...(x_n:y_n)
22 = array: x1,y1,x2,y2,x3,y3,x4,y4...x_n,y_n
    x/y in xrange / yrange coordinate system
23 = answertype for a polyline: remove multiple occurences due to reclick on a point to create next polyline segment
24 = read all inputfield values: even those set <code>readonly</code>
25 = return all userdrawn arcs in degrees:
26 = return all userdrawn arcs in radians:
27 = return (only) userdraw inputfields array: x1,y1,text1 \n x2,y2,text2...
28 = x1,y1,r1,x2,y2,r2...x_n,y_n,r_n
    x/y/r in xrange / yrange coordinate system: may be used to reinput into command
    circles color,x1,y1,r1,x2,y2,r2...x_n,y_n,r_n
    will not return anything else (e.g. no inputfields, text etc)
29 = mulidraw read:

*/

/*
SCHAERSVOORDE: replyformat 2,7,8,21,22,23,24
USERDRAW DEFAULTS: 2,6,8,10,11,15,16,17,18,19,20,23,24,25,27,29,31
OEF: 22,23,28

TO BE REMOVED: 1,3,4,5,9,12,13,14,21
*/
void add_read_canvas(int type_reply,int reply_precision){
/* just 1 reply type allowed...except for format 34 !!!  */
  fprintf(js_include_file,"\
\n/* set_reply_precision() */\
function set_reply_precision(){\
 var len = userdraw_x.length;\
 var prec = %d;\
 for(var p = 0 ; p < len ; p++ ){\
  userdraw_x[p] = (Math.round(prec*userdraw_x[p]))/prec;\
  userdraw_y[p] = (Math.round(prec*userdraw_y[p]))/prec;\
 };\
 len = userdraw_radius.length;\
 if( len > 0 ){\
  for(var p = 0 ; p < len ; p++ ){\
   userdraw_radius[p] = (Math.round(prec*userdraw_radius[p]))/prec;\
  };\
 };\
};",reply_precision);

  switch(type_reply){
/*
answers may have:
x-values,y-values,r-values,input-fields,mathml-inputfields,text-typed answers
*/
    case 1:
     fprintf(js_include_file,"\
\n/* function 1 read_canvas%d() */\
read_canvas%d = function(){\
 if( userdraw_x.length == 0){return;}\
 return userdraw_x+\"\\n\"+userdraw_y;\
};\
/* end function 1 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 2: fprintf(js_include_file,"\
\n/* function 2 read_canvas%d() */\
read_canvas%d = function(){\
 if( userdraw_x.length == 0){return;}\
 var reply_x = new Array();var reply_y = new Array();\
 var prec = %d;\
 var len = userdraw_x.length;\
 for(var p = 0;p < len ; p++){\
  reply_x[p] = (Math.round(prec*(px2x(userdraw_x[p]))))/prec;\
  reply_y[p] = (Math.round(prec*(px2y(userdraw_y[p]))))/prec;\
 };\
 return reply_x+\"\\n\"+reply_y;\
};\
/* end function 2 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 3: fprintf(js_include_file,"\
\n/* function 3 read_canvas%d() */\
read_canvas%d = function(){\
 if( userdraw_x.length == 0){return;}\
 return userdraw_x+\"\\n\"+userdraw_y+\"\\n\"+userdraw_radius;\
};\
/* end function 3 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 4: fprintf(js_include_file,"\
\n/* function 4 read_canvas%d() */\
read_canvas%d = function(){\
 var prec = %d;\
 var reply_x = new Array();var reply_y = new Array();\
 var len = userdraw_x.length;\
 for(var p = 0 ; p>len ;p++){\
  reply_x[p] = (Math.round(prec*(px2x(userdraw_x[p]))))/prec;\
  reply_y[p] = (Math.round(prec*(px2y(userdraw_y[p]))))/prec;\
 };\
 return reply_x+\"\\n\"+reply_y+\"\\n\"+userdraw_radius;\
};\
/* end function 4 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 5: fprintf(js_include_file,"\
\n/* function 5 read_canvas%d() */\
read_canvas%d = function(){\
 set_reply_precision();\
 var p = 0;\
 var reply = \"\";\
 for(p = 0; p < userdraw_x.length;p++){\
  if(userdraw_x[p] != null ){\
   reply = reply + userdraw_x[p]+\"\\n\"+userdraw_y[p]+\"\\n\";\
  };\
 };\
 return reply;\
};\
/* end function 5 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 6: fprintf(js_include_file,"\
\n/* function 6 read_canvas%d() */\
read_canvas%d = function(){\
 var reply = \"\";\
 var tmp_x = new Array();\
 var tmp_y = new Array();\
 var prec = %d;\
 for(var p = 0 ; p < userdraw_x.length; p++){\
  tmp_x = userdraw_x[p];\
  tmp_y = userdraw_y[p];\
  if(tmp_x != null){\
   for(var i = 0 ; i < tmp_x.length ; i++){\
    tmp_x[i] = (Math.round(prec*(px2x(tmp_x[i]))))/prec;\
    tmp_y[i] = (Math.round(prec*(px2y(tmp_y[i]))))/prec;\
   };\
   reply = reply + tmp_x + \"\\n\" + tmp_y +\"\\n\";\
  };\
 };\
 return reply;\
};\
/* end function 6 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 7: fprintf(js_include_file,"\
\n/* function 7 read_canvas%d() */\
read_canvas%d = function(){\
 set_reply_precision();\
 var reply = new Array();\
 var len = userdraw_x.length;\
 for(var p=0; p< len ;p++){\
  reply[p] = userdraw_x[p] +\":\" + userdraw_y[p];\
 };\
 return reply;\
};\
/* end function 7 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 8: fprintf(js_include_file,"\
\n/* function 8 read_canvas%d() */\
read_canvas%d = function(){\
 var reply = new Array();\
 var prec = %d;\
 var len = userdraw_x.length;\
 for(var p=0 ; p < len; p++){\
  reply[p] = (Math.round(prec*(px2x(userdraw_x[p]))))/prec +\":\" + (Math.round(prec*(px2y(userdraw_y[p]))))/prec;\
 };\
 return reply;\
};\
/* end function 8 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 9: fprintf(js_include_file,"\
\n/* function 9 read_canvas%d() */\
read_canvas%d = function(){\
 set_reply_precision();\
 var reply = new Array();\
 var len = userdraw_x.length;\
 for(var p=0;p<len;p++){\
  reply[p] = userdraw_x[p] +\":\" + userdraw_y[p] + \":\" + userdraw_radius[p];\
  p++;\
 };\
 return reply;\
};\
/* end function 9 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 10: fprintf(js_include_file,"\
\n/* function 10 read_canvas%d() */\
read_canvas%d = function(){\
 var len = userdraw_x.length;\
 var prec = %d;\
 var reply = \"\";\
 for(var p=0; p<len;p++){\
  reply = reply + (Math.round(prec*(px2x(userdraw_x[p]))))/prec +\";\" + (Math.round(prec*(px2y(userdraw_y[p]))))/prec + \";\" + (Math.round(prec*userdraw_radius[p]))/prec + \"\\n\";\
 };\
 return reply;\
};\
/* end function 10 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 11: fprintf(js_include_file,"\
\n/* function 11 read_canvas%d() */\
read_canvas%d = function(){\
 var reply = \"\";\
 var p = 0;\
 var prec = %d;\
 var len = userdraw_x.length;\
 for(var p=0;p<len;p=p+2){\
  reply = reply + (Math.round(prec*(px2x(userdraw_x[p]))))/prec +\",\" + (Math.round(prec*(px2y(userdraw_y[p]))))/prec +\",\" + (Math.round(prec*(px2x(userdraw_x[p+1]))))/prec +\",\" + (Math.round(prec*(px2y(userdraw_y[p+1]))))/prec +\"\\n\" ;\
 };\
 return reply;\
};\
/* end function 11 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 12: fprintf(js_include_file,"\
\n/* function 12 read_canvas%d() */\
read_canvas%d = function(){\
 set_reply_precision();\
 var reply = \"\";\
 var len = userdraw_x.length;\
 for(var p=0;p<len;p = p+2){\
  reply = reply + userdraw_x[p] +\",\" + userdraw_y[p] +\",\" + userdraw_x[p+1] +\",\" + userdraw_y[p+1] +\"\\n\" ;\
 };\
 return reply;\
};\
/* end function 12 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 13: canvas_error("replyformat 13 has been removed...");
      break;
    case 14:
      canvas_error("replyformat 14 has been removed");
      break;
/*
read_canvas%d = function(){\
 set_reply_precision();\
 var reply = new Array();\
 var p = 0;var i = 0;\
 while(userdraw_x[p+1]){\
  reply[i] = userdraw_x[p] +\":\" + userdraw_y[p] +\":\" + userdraw_x[p+1] +\":\" + userdraw_y[p+1];\
  p = p+2;i++;\
 };\
 return reply;\
};\
*/
      break;
    case 15: fprintf(js_include_file,"\
\n/* function 15  read_canvas%d() */\
read_canvas%d = function(){\
 var input_reply = new Array();\
 var p = 0;\
 if( document.getElementById(\"canvas_input0\")){\
  var t = 0;\
  while(document.getElementById(\"canvas_input\"+t)){\
   if( ! document.getElementById(\"canvas_input\"+t).getAttribute(\"readonly\")){\
    input_reply[p] = document.getElementById(\"canvas_input\"+t).value;\
    p++;\
   };\
   t++;\
  };\
 };\
 return input_reply;\
};\
/* end function 15 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 16: fprintf(js_include_file,"\
\n/* function 16 read_mathml() */\
function read_mathml(){\
 var reply = new Array();\
 var p = 0;\
 if( document.getElementById(\"mathml0\")){\
  while(document.getElementById(\"mathml\"+p)){\
   reply[p] = document.getElementById(\"mathml\"+p).value;\
   p++;\
  };\
 };\
return reply;\
};\
this.read_mathml = read_mathml;\n\
/* end function 16 read_mathml() */");
      break;
    case 17:  fprintf(js_include_file,"\
\n/* function 17 read_canvas%d() */\
read_canvas%d = function(){\
 var len = userdraw_x.length;\
 if( len == 0){return;}\
 var rep = px2x(userdraw_x[0])+\",\"+px2y(userdraw_y[0])+\",\"+userdraw_text[0];\
 for(var p = 1 ; p < len ; p++){\
  rep = rep + \"\\n\" + px2x(userdraw_x[p]) + \",\" + px2y(userdraw_y[p]) + \",\" + userdraw_text[p];\
 };\
 return rep;\
};\
/* end function 17 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 18: fprintf(js_include_file,"\
\n/* javascript has no real modulo function */\
function mod(n, m){\
 var m = parseInt(((n %% m) + m) %% m);\
 return m;\
};\
\n/* function 18 read_canvas%d() */\
read_canvas%d = function(){\
 var p = 0;\
 var reply = new Array();\
 var name;\
 var t = true;\
 var h;var m;var s;\
 while(t){\
  try{\
   name = eval('clocks'+p);\
   h = name.H;m = name.M;s = name.S;\
   h = mod((h+m/60+s/3600),12);m = mod((m + s/60),60);s = mod(s,60);\
   reply[p] = h+\":\"+m+\":\"+s;\
   p++;\
  }catch(e){t=false;};\
 };\
 if( p == 0 ){return;}\
 return reply;\
};\
/* end function 18 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 19: fprintf(js_include_file,"\
\n/* function 19 read_canvas%d() */\
read_canvas%d = function(){\
 return reply[0];\
};\
/* end function 19 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 20: fprintf(js_include_file,"\
\n/* function 20 read_canvas%d() */\
read_canvas%d = function(){\
 var prec = %d;\
 var len  = ext_drag_images.length;\
 var reply = new Array(len);\
 for(var p = 0 ; p < len ; p++){\
  var img = ext_drag_images[p];\
  reply[p] = p+\":\"+(Math.round(prec*(px2x(img[6]))))/prec+\":\"+(Math.round(prec*(px2y(img[7]))))/prec;\
 };\
 return reply;\
};\
/* end function 20 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 21: fprintf(js_include_file,"\
\n/* function 21 read_canvas%d() */\
read_canvas%d = function(){\
 var len = userdraw_x.length;if(len == 0){return;}\
 var reply_coord = new Array();var p = 0;\
 var prec = %d;\
 for(var p = 0; p < len ; p++){\
  reply_coord[p] = \"(\"+(Math.round(prec*(px2x(userdraw_x[p]))))/prec+\":\"+(Math.round(prec*(px2y(userdraw_y[p]))))/prec+\")\";\
 };\
 return reply_coord;\
};\
/* end function 21 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 22: fprintf(js_include_file,"\
\n/* function 22 read_canvas%d() */\
read_canvas%d = function(){\
 var reply = new Array();\
 var lu = userdraw_x.length;\
 if(lu == 0){return;};\
 var idx = 0;\
 var prec = %d;\
 for(var p = 0 ; p < lu ; p++){\
  reply[idx] = (Math.round(prec*(px2x(userdraw_x[p]))))/prec;idx++;\
  reply[idx] = (Math.round(prec*(px2y(userdraw_y[p]))))/prec;idx++;\
 };\
 return reply;\
};\
/* end function 22 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 23: fprintf(js_include_file,"\
\n/* function 23 read_canvas%d() default 5 px marge */\
read_canvas%d = function(){\
 if( userdraw_x.length < 2){return;}\
 var lu = userdraw_x.length;\
 if( lu != userdraw_y.length ){return;}\
 var reply_x = new Array();var reply_y = new Array();\
 var marge = 5;var p = 0;\
 var prec = %d;\
 for(var i = 0; i < lu - 1 ; i++ ){\
  if( Math.abs(userdraw_x[i] - userdraw_x[i+1]) || Math.abs(userdraw_y[i] - userdraw_y[i+1])){\
   reply_x[p] = (Math.round(prec*(px2x(userdraw_x[i]))))/prec;reply_y[p] = (Math.round(prec*(px2y(userdraw_y[i]))))/prec;\
   if( isNaN(reply_x[p]) || isNaN(reply_y[p]) ){return; };\
   p++;\
  };\
  reply_x[p] = (Math.round(prec*(px2x(userdraw_x[lu-1]))))/prec;reply_y[p] = (Math.round(prec*(px2y(userdraw_y[lu-1]))))/prec;\
 };\
 return reply_x+\"\\n\"+reply_y;\
};\
/* end function 23 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 24: fprintf(js_include_file,"\n\
/* function 24  read_canvas%d() */\
read_canvas%d = function(){\
 var input_reply = new Array();\
 var p = 0;\
 if( document.getElementById(\"canvas_input0\")){\
  while(document.getElementById(\"canvas_input\"+p)){\
    input_reply[p] = document.getElementById(\"canvas_input\"+p).value;\
    p++;\
  };\
  return input_reply;\
 };\
};\
/* end function 24 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 25:
      fprintf(js_include_file,"\n/* function 25 read_canvas%d(): angle(s) in degrees */\
read_canvas%d = function(){\
 if( userdraw_radius.length < 1){return;}\
 var lu = userdraw_radius.length;\
 var prec = %d;\
 var angle_reply = new Array(lu);\
 for(var p = 0 ; p < lu ; p++){\
  angle_reply[p] = (Math.round(prec*180*(userdraw_radius[p])/Math.PI))/prec;\
 };\
 return angle_reply;\
};\
/* end function 25 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 26:
      fprintf(js_include_file,"\n/* function 26 read_canvas%d(): angle(s) in radians */\
read_canvas%d = function(){\
 if( userdraw_radius.length < 1){return;}\
 var lu = userdraw_radius.length;\
 var prec = %d;\
 var angle_reply = new Array(lu);\
 for(var p = 0 ; p < lu ; p++){\
  angle_reply[p] = (Math.round(prec*(userdraw_radius[p])))/prec;\
 };\
 return angle_reply;\
};\
/* end function 26 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 27:
      fprintf(js_include_file,"\n/* function 27 read_canvas%d(): inputfield(s) location and their values: */\
read_canvas%d = function(){\
 var lu = userdraw_x.length;\
 if( lu < 1){return;};\
 set_reply_precision();\
 var prec = %d;var rep = \"\";\
 for(var p = 0 ; p < lu ; p++){\
   rep = rep + (Math.round(prec*(px2x(userdraw_x[p]))))/prec+\";\"+(Math.round(prec*(px2y(userdraw_y[p]))))/prec+\";\"+ document.getElementById(\"canvas_input\"+p).value + \"\\n\";\
 };\
 return rep;\
};\
/* end function 27 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 28:
      fprintf(js_include_file,"\n/* function 28 read_canvas%d() */\
read_canvas%d = function(){\
 var prec = %d;var reply = new Array();var p = 0;var idx = 0;\
 while(userdraw_x[p]){\
  reply[idx] = (Math.round(prec*(px2x(userdraw_x[p]))))/prec;\
  idx++;\
  reply[idx] = (Math.round(prec*(px2y(userdraw_y[p]))))/prec;\
  idx++;\
  reply[idx] = (Math.round(prec*(px2x(userdraw_radius[p]) - px2x(0))))/prec;\
  idx++;\
  p++;\
 };\
 return reply;\
};\
/* end function 28 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 29:
      fprintf(js_include_file,"\n/* function 29 [multidraw]read_canvas%d() */\
function x_precision(array_x){\
 var len = array_x.length;\
 var x_array = new Array(len);\
 var prec = %d;\
 for(var p = 0 ; p < len ; p++ ){\
  x_array[p] = (Math.round(prec*(px2x(array_x[p]))))/prec;\
 };\
 return x_array;\
};\
function y_precision(array_y){\
 var len = array_y.length;\
 var y_array = new Array(len);\
 var prec = %d;\
 for(var p = 0 ; p < len ; p++ ){\
  y_array[p] = (Math.round(prec*(px2y(array_y[p]))))/prec;\
 };\
 return y_array;\
};\
function radius_to_x(array_r){\
 var len = array_r.length;\
 var ff = (xmax - xmin)/xsize;\
 var r_array = new Array(len);\
 var prec = %d;\
 for(var p = 0 ; p < len ; p++ ){\
  r_array[p] = (Math.round(prec*(ff*(array_r[p]))))/prec;\
 };\
 return r_array;\
};\
read_canvas%d = function(){\
 function uniq_fast(arr){\
  var seen = {};\
  var out = [];\
  var len = arr.length;\
  var j = 0;\
  for(var i = 0; i < len; i++){\
   var item = arr[i];\
   if(seen[item] !== 1) {\
    seen[item] = 1;out[j++] = item;\
   };\
  };\
  return out;\
 };\
 function list_unique(arr1,arr2,arr3){\
  if( typeof( allow_duplicate_answers ) === 'number' ){ return [ x_precision(arr1),y_precision(arr2),arr3 ];};\
  var len1 = arr1.length;\
  if(len1 != arr2.length){return;};\
  var sum = [];var R1=[];var R2=[];\
  arr1 = x_precision(arr1);\
  arr2 = y_precision(arr2);\
  if(arr3 == null){\
   for(var p=0;p<len1;p++){ sum[p] = arr1[p]+'#'+arr2[p]; };\
   sum = uniq_fast(sum);var len2=sum.length;\
   for(var p=0;p<len2;p++){\
    var tmp = (sum[p]).split('#');\
    R1[p] = tmp[0];\
    R2[p] = tmp[1];\
   };\
   return [R1,R2];\
  }else{\
   var R3 = [];\
   for(var p=0;p<len1;p++){ sum[p] = arr1[p]+'#'+arr2[p]+'#'+arr3[p];};\
   sum = uniq_fast(sum);var len2=sum.length;\
   for(var p=0;p<len2;p++){\
    var tmp = (sum[p]).split('#');\
    R1[p] = tmp[0];\
    R2[p] = tmp[1];\
    R3[p] = tmp[2];\
   };\
   return [R1,R2,R3];\
  };\
 };\
 var reply=\" \";\
 if(  typeof(points_x) === 'object' && points_x.length > 0 ){var xyz = list_unique(points_x,points_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\";}else{ reply = reply + \"\\n\"; };\
 if(  typeof(circles_x) === 'object' && circles_x.length > 0 ){var xyz = list_unique(circles_x,circles_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\";\"+radius_to_x(multi_radius)+\"\\n\"; }else{ reply = reply + \"\\n\"; };\
 if(  typeof(segments_x) === 'object' && segments_x.length > 0 ){ var xyz = list_unique(segments_x,segments_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\";}else{ reply = reply + \"\\n\"; };\
 if(  typeof(arrows_x) === 'object' && arrows_x.length > 0 ){var xyz = list_unique(arrows_x,arrows_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\"; }else{ reply = reply + \"\\n\"; };\
 if(  typeof(lines_x) === 'object' && lines_x.length > 0 ){ var xyz = list_unique(lines_x,lines_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\"; }else{ reply = reply + \"\\n\"; };\
 if(  typeof(triangles_x) === 'object' && triangles_x.length > 0){var xyz = list_unique(triangles_x,triangles_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\"; }else{ reply = reply + \"\\n\"; };\
 if(  typeof(polys_x) === 'object' && polys_x.length > 0){ var xyz = list_unique(polys_x,polys_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\"; }else{ reply = reply + \"\\n\"; };\
 if(  typeof(rects_x) === 'object' && rects_x.length > 0 ){var xyz = list_unique(rects_x,rects_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\"; }else{ reply = reply + \"\\n\"; };\
 if(  typeof(closedpoly_x) === 'object' && closedpoly_x.length > 0){ closedpoly_x.pop();closedpoly_y.pop();var xyz = list_unique(closedpoly_x,closedpoly_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\";}else{ reply = reply + \"\\n\"; };\
 if(  typeof(parallelogram_x) === 'object' && parallelogram_x.length > 0){var xyz = list_unique(parallelogram_x,parallelogram_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\"; }else{ reply = reply + \"\\n\"; };\
 if(  typeof(text_x) === 'object' && text_x.length > 0){var xyz = list_unique(text_x,text_y,text_abc);reply = reply + xyz[0] +\";\"+xyz[1]+\";\"+xyz[2]+\"\\n\";}else{ reply = reply + \"\\n\"; };\
 if(  typeof(images_x) === 'object' && images_x.length > 0){var xyz = list_unique(images_x,images_y,images_id);reply = reply + xyz[0] +\";\"+xyz[1]+\";\"+xyz[2]+\"\\n\";}else{ reply = reply + \"\\n\"; };\
 if(  typeof(curvedarrows_x) === 'object' && curvedarrows_x.length > 0){var xyz = list_unique(curvedarrows_x,curvedarrows_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\"; }else{ reply = reply + \"\\n\";};\
 if(  typeof(curvedarrows2_x) === 'object' && curvedarrows2_x.length > 0){var xyz = list_unique(curvedarrows2_x,curvedarrows2_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\"; }else{ reply = reply + \"\\n\";};\
 if(  typeof(crosshairs_x) === 'object' && crosshairs_x.length > 0 ){var xyz = list_unique(crosshairs_x,crosshairs_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\";}else{ reply = reply + \"\\n\"; };\
 if(  typeof(userdraw_x) === 'object' && userdraw_radius.x> 0){var xyz = list_unique(userdraw_x,userdraw_y,null);reply = reply + xyz[0] +\";\"+xyz[1]+\"\\n\"; return reply;;};\
 if(  typeof(userdraw_radius) === 'object' && userdraw_radius.length > 0){var xyz = list_unique(userdraw_x,userdraw_y,userdraw_radius);reply = reply + xyz[0] +\";\"+xyz[1]+\";\"+xyz[2]+\"\\n\"; return reply;;};\
 if( document.getElementById(\"canvas_input0\")){var t = 0;\
  while(document.getElementById(\"canvas_input\"+t)){\
   if( ! document.getElementById(\"canvas_input\"+t).getAttribute(\"readonly\")){\
    reply = reply+document.getElementById(\"canvas_input\"+t).value + \";\";\
   };\
   t++;\
  };\
 };\
 return reply;\
};\
/* end function 29 read_canvas%d() */",canvas_root_id,reply_precision,reply_precision,reply_precision,canvas_root_id,canvas_root_id);
      break;
    case 30:
      fprintf(js_include_file,"\n/* function 30 read_canvas%d() */\
read_canvas%d = function(){\
 var reply = new Array(3);\
 var prec = %d;\
 reply[0] = (Math.round(prec*(px2x(protractor_data[0]))))/prec;\
 reply[1] = (Math.round(prec*(px2y(protractor_data[1]))))/prec;\
 reply[2] = (Math.round(prec*(protractor_data[2])))/prec;\
 return reply;\
};\
/* end function 30 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 31:
      fprintf(js_include_file,"\n/* function 31 read_canvas%d() */\
read_canvas%d = function(){\
 var reply = new Array(3);\
 var prec = %d;\
 reply[0] = (Math.round(prec*(px2x(ruler_data[0]))))/prec;\
 reply[1] = (Math.round(prec*(px2y(ruler_data[1]))))/prec;\
 reply[2] = (Math.round(prec*(ruler_data[2])))/prec;\
 return reply;\
};\
/* end function 31 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 32:
      fprintf(js_include_file,"\n/* function 32 read_canvas%d() */\
read_canvas%d = function(){\
 var reply = new Array(6);\
 var prec = %d;\
 reply[0] = (Math.round(prec*(px2x(ruler_data[0]))))/prec;\
 reply[1] = (Math.round(prec*(px2y(ruler_data[1]))))/prec;\
 reply[2] = (Math.round(prec*(ruler_data[2])))/prec;\
 reply[3] = (Math.round(prec*(px2x(protractor_data[0]))))/prec;\
 reply[4] = (Math.round(prec*(px2y(protractor_data[1]))))/prec;\
 reply[5] = (Math.round(prec*(protractor_data[2])))/prec;\
 return reply;\
};\
/* end function 32 read_canvas%d() */",canvas_root_id,canvas_root_id,reply_precision,canvas_root_id);
      break;
    case 33:
      fprintf(js_include_file,"\n/* function 33 read_canvas%d() */\
read_canvas%d = function(){\
 var reply = userdraw_x+'\\n'+userdraw_y;\
 return reply;\
};\
/* end function 32 read_canvas%d() */",canvas_root_id,canvas_root_id,canvas_root_id);
      break;
    case 34: fprintf(js_include_file,"\
\n/* special OEF function (replyformat 34) read_canvas_images() \\n note: only suitable for reading a single canvas in exercise page */\
var read_canvas_images = function(){\
 var prec = %d;\
 var len  = ext_drag_images.length;\
 var reply = new Array(len);\
 for(var p = 0 ; p < len ; p++){\
    var img = ext_drag_images[p];\
    reply[p] = p+\":\"+(Math.round(prec*(px2x(img[6]))))/prec+\":\"+(Math.round(prec*(px2y(img[7]))))/prec;\
 };\
 return reply;\
};\
/* end function 20 read_canvas_images() */",reply_precision);
      break;
    case 100:break; /* debug js-object format: see dragstuff.c */
    default: canvas_error("hmmm unknown replyformat...");break;
}
 return;
}
