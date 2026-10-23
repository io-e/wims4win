#include "canvasdraw.h"

void add_js_zoom_buttons(char *stroke_color,double stroke_opacity){
fprintf(js_include_file,"\n/* draw zoom buttons */\
var draw_zoom_buttons = function(){\
 var obj;var canvas_type =%d;\
 if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){\
  obj = document.getElementById(\"wims_canvas%d\"+canvas_type);\
 } else {\
  obj = create_canvas%d(canvas_type,xsize,ysize);\
 };\
 var ctx = obj.getContext(\"2d\");\
 ctx.font =\"20px Arial\";\
 ctx.textAlign = \"right\";\
 ctx.fillStyle=\"rgba(%s,%f)\";\
 ctx.fillText(\"+\",xsize,ysize);\
 ctx.fillText(\"\\u2212\",xsize - 15,ysize-2);\
 ctx.fillText(\"\\u2192\",xsize - 30,ysize-2);\
 ctx.fillText(\"\\u2190\",xsize - 45,ysize-2);\
 ctx.fillText(\"\\u2191\",xsize - 60,ysize-2);\
 ctx.fillText(\"\\u2193\",xsize - 75,ysize-2);\
 ctx.fillText(\"\\u00D7\",xsize - 90,ysize-2);\
 ctx.stroke();\
};draw_zoom_buttons();",BG_CANVAS,canvas_root_id,canvas_root_id,canvas_root_id,stroke_color,stroke_opacity);
}

void add_js_popup(char *getfile_cmd){
  fprintf(stdout,"\n<!-- begin command popup %d -->\n\
<script>\n\
if( wims_status != 'done'){\
 var popup = window.open('','','toolbar=no,scrollbars=yes,menubar=no,location=no,resizable=yes,top=4,left=4,status=no, width = %dpx, height = %dpx');\
 var popupHTML =\"<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN' 'http://www.w3.org/TR/html4/loose.dtd'>\\n<html><head></head>\\n<body><div style='width:%dpx;height:%dpx;position:relative' id='canvas_div%d'></div><div id='tooltip_placeholder_div%d' style='display:block;position:relative;margin-left:auto;margin-right:auto;margin-bottom:4px;'><span id='tooltip_placeholder%d' style='display:none;'></span></div></body></html>\";\
 popup.document.write(popupHTML);\
 var s0 = popup.document.createElement(\"script\");\
 s0.text=\"var wims_status = window.opener.wims_status; var use_dragdrop_reply = window.opener.use_dragdrop_reply;window.opener.canvas_scripts.push('%d');\";\
 popup.document.getElementsByTagName(\"HEAD\")[0].appendChild(s0);\
 var s1 = popup.document.createElement(\"script\");\
 s1.src = \"%s\";\
 popup.document.getElementsByTagName(\"HEAD\")[0].appendChild(s1);\
 popup.document.close();\
 } else {\
  var canvas_div = document.getElementById('canvas_div%d');\
  canvas_div.style.display='block';\
 };\n</script>\n<!-- end command popup %d -->\n",
canvas_root_id,
xsize+40,ysize+40,
xsize,ysize,
canvas_root_id,
canvas_root_id,
canvas_root_id,
canvas_root_id,
getfile_cmd,
canvas_root_id,
canvas_root_id);
}

void add_js_tooltip(char *tooltip_text,char *bgcolor){
  fprintf(stdout,"\n<!-- begin command intooltip %d -->\n\
<script>\n\
var xsize = %d;\
var ysize = %d;\
var tooltip%d_obj_x = 0;\
var tooltip%d_obj_y = 0;\
var tooltip%d_flipflop = 0;\
var tooltip%d_obj = document.getElementById(\"canvas_div%d\");\
tooltip%d_obj.style.display=\"none\";\
tooltip%d_obj.style.position=\"absolute\";\
var tooltip%d_link = document.createElement(\"a\");\
tooltip%d_link.addEventListener(\"mousemove\",tooltip%d_drag,false);\
tooltip%d_link.setAttribute(\"onclick\",\"tooltip%d_show()\");\
tooltip%d_link.innerHTML  = \"%s\";\
var tooltip_placeholder = document.getElementById(\"tooltip_placeholder%d\");\
tooltip_placeholder.style.display=\"block\";\
tooltip_placeholder.style.position=\"absolute\";\
tooltip_placeholder.style.backgroundColor=\"%s\";\
tooltip_placeholder.appendChild(tooltip%d_link);\
function tooltip%d_drag(action){\
 if(!action){ action = event; };\
 if(action.clientX){\
  tooltip%d_obj.style.left = (tooltip%d_mouseX(action) + 10) +\"px\";\
  var ytop = tooltip%d_mouseY(action);\
  if(ytop + ysize < window.innerHeight){\
   tooltip%d_obj.style.top = (ytop - 10) +\"px\";\
  } else {\
   tooltip%d_obj.style.top = parseInt(ytop - 0.8*ysize) +\"px\";\
  };\
 } else {\
  return null;\
 };\
};\
function tooltip%d_mouseX(action){\
 if(action.pageX){\
  return action.pageX;\
 } else {\
  if(action.clientX){\
   return action.clientX + (document.documentElement.scrollLeft ? document.documentElement.scrollLeft : document.body.scrollLeft);\
  } else {\
   return null;\
  };\
 };\
};\
function tooltip%d_mouseY(action){\
 if(action.pageY){\
  return action.pageY;\
 } else {\
  if(action.clientY){\
   return action.clientY + (document.documentElement.scrollTop ? document.documentElement.scrollTop :document.body.scrollTop);\
  } else {\
   return null;\
  };\
 };\
};\
function tooltip%d_show(){\
 if(tooltip%d_flipflop == 0){\
  tooltip%d_obj.style.display = \"block\";\
  tooltip%d_flipflop = 1;\
 } else {\
  tooltip%d_flipflop = 0;\
  tooltip%d_obj.style.display = \"none\";\
 };\
};\n</script>\n<!-- end command intooltip %d -->\n",canvas_root_id,xsize,ysize,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,tooltip_text,canvas_root_id,bgcolor,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id);
}

/*

type = 0 : x-values only [command mousex]
type = 1 : y-values only [command mousey]
type = 2 : (x:y) 	 [command mouse]
type = 3 : degree	 [command mouse_degree]
type = 4 : radian
type = 5 : radius

  var mouse = getMouse(evt,canvas_div);\
  var x = evt.clientX - mouse_canvas_rect.left;\
  var y = evt.clientY - mouse_canvas_rect.top;\

*/
void add_js_mouse(int canvas_cnt,int precision,char *stroke_color,int font_size,double stroke_opacity,int type){
  fprintf(js_include_file,"\n/* command mouse on mouse canvas */\
function use_mouse_coordinates(){\
 var display_type = %d;\
 var canvas_type = %d;\
 var mouse_canvas = create_canvas%d(canvas_type,xsize,ysize);\
 var mouse_context = mouse_canvas.getContext(\"2d\");\
 mouse_canvas.addEventListener(\"mousemove\",show_coordinate%d,false);\
 mouse_canvas.addEventListener(\"touchmove\", function(e){ e.preventDefault();show_coordinate%d(e.changedTouches[0]);},false);\
 var prec = Math.log(%d)/(Math.log(10));\
 function show_coordinate%d(evt){\
  var mouse = getMouse(evt,mouse_canvas);\
  var x = mouse.x;\
  var y = mouse.y;\
  var m_data = \"\";\
  switch(display_type){\
   case 0: m_data = \" \"+(px2x(x)).toFixed(prec)+\" \"+unit_x;break;\
   case 1: m_data = \" \"+(px2y(y)).toFixed(prec)+\" \"+unit_y;break;\
   case 2: m_data = \"(\"+(px2x(x)).toFixed(prec)+\":\"+(px2y(y)).toFixed(prec)+\")\";break;\
   case 3: if(userdraw_radius[0]){ m_data = \" \"+( ( userdraw_radius[0])/(Math.PI/180) ).toFixed(prec)+\" \\u00B0 \";};break;\
   case 4: if(userdraw_radius[0]){ m_data = \" \"+(userdraw_radius[0]).toFixed(prec)+\" rad \";};break;\
   case 5: if( userdraw_x.length > 0 ){var L = userdraw_x.length;m_data = \" R = \"+((xmax - xmin)*(distance(x,y,userdraw_x[L-1],userdraw_y[L-1]))/xsize).toFixed(prec)+\" \"+unit_x;};break;\
   default:break;\
  };\
  var s = parseInt(0.8*%d*(m_data.toString()).length);\
  mouse_context.font = \"%dpx Arial\";\
  mouse_context.fillStyle = \"rgba(%s,%f)\";\
  mouse_context.clearRect(0,0,s,1.2*%d);\
  mouse_context.fillText(m_data,0,%d);\
 };\
};",type,MOUSE_CANVAS,canvas_root_id,canvas_root_id,canvas_root_id,precision,canvas_root_id,font_size,font_size,stroke_color,stroke_opacity,font_size,font_size);
}

/*
Math.sin: angle in radians
x1 = x*cos(a) - y*sin(a)
y1 = y*cos(a) + x*sin(a)
show_display = 0  nothing
show_display = 1 delta X
show_display = 2 delta Y
show_display = 3 delta R radians
show_display = 4 delta degrees
show_display = 5 delta X:Y

*/
void add_slider_display(int precision,int font_size,char *font_color,double stroke_opacity){
  fprintf(js_include_file,"\n/* add_slider_display */\
var slider_prec = Math.log(%d)/(Math.log(10));\
 function show_slider_value(value,use_slider_display){\
 var current_canvas = create_canvas%d(%d,xsize,ysize);\
 var current_context = current_canvas.getContext(\"2d\");\
 current_context.clearRect(0,0,xsize,ysize);\
 var string;\
 switch(use_slider_display){\
  case 0: return;\
  case 1: string = '\\u0394 x = '+value.toFixed(slider_prec)+' '+unit_x;break;\
  case 2: string = '\\u0394 y = '+value.toFixed(slider_prec)+' '+unit_y;break;\
  case 3: string = '\\u2221 = '+value.toFixed(slider_prec)+'\\u03C0 rad';break;\
  case 4: string = '\\u2221 = '+(value*(180/Math.PI)).toFixed(slider_prec)+'\\u00B0';break;\
  case 5: string = 'not implemented';break;\
  default: string = '['+value+']';break;\
 };\
 var s = parseInt(1.2*%d*(string).length);\
 current_context.font = '%dpx Arial';\
 current_context.strokeStyle = 'rgba(%s,%.2f)';\
 current_context.clearRect(0,0,s,1.2*%d);\
 current_context.fillText(string,0,%d);};",precision,canvas_root_id,MOUSE_CANVAS,font_size,font_size,font_color,stroke_opacity,font_size,font_size);
}

void add_slider(int anim){
  if(anim != 2 ){
    fprintf(js_include_file,"\n/* add_slider aux*/\
if( typeof( slidergroup) !== 'object'){var slidergroup = [];};\
function rotateXY(obj,angle){\
 if( typeof(angle) === 'undefined' ){console.log('rotateXY() angle undefined');return obj;};\
 var len = obj.x.length;\
 if( typeof(len) === 'undefined' ){obj.angle = angle;return obj;};\
 var cos = Math.cos(angle - obj.angle);\
 var sin = Math.sin(angle - obj.angle);\
 obj.angle = angle;\
 var xc,yc;\
 if( obj.use_rotation_center == 0 ){\
  xc = obj.x[0];\
  yc = obj.y[0];\
 }else{\
  xc = obj.rotation_center[0];\
  yc = obj.rotation_center[1];\
 };\
 var x,y;\
 for(var p = 0 ; p < len ; p++ ){\
  x = obj.x[p];\
  y = obj.y[p];\
  obj.x[p] = (cos * (x - xc)) + (sin * (y - yc)) + xc;\
  obj.y[p] = (cos * (y - yc)) - (sin * (x - xc)) + yc;\
 };\
 return obj;\
};\
function rotateARC(obj,angle){\
 obj.h[0] = angle;\
 obj.angle = -1*angle;\
 return obj;\
};\
function slider_show_it(XYR,dx,value,slider_id){\
 if(slidergroup.length != 0){\
  var len = slidergroup.length;var use_xml = false;\
  for(var i = 0;i < len;i++){\
   use_xml = false;var obj = slidergroup[i];\
   if( obj !== null ){\
    if( obj.use_slider[0] != -1 ){\
     var sl = obj.use_slider.length;\
     for(var s = 0 ; s < sl;s++ ){\
      if(obj.use_slider[s] == slider_id ){\
       if( typeof(obj.type) !== 'number' ){use_xml = true;};\
       switch(XYR){\
        case 'X' : obj = move(obj,dx,0);dragdrop_reply[obj.object_cnt].x = obj.x;break;\
        case 'Y' : obj = move(obj,0,dx);dragdrop_reply[obj.object_cnt].y = obj.y;break;\
        case 'R' : if(obj.type == 17 ){obj = rotateARC(obj,value);}else{obj = rotateXY(obj,value);};dragdrop_reply[obj.object_cnt].angle = obj.angle;break;\
        default : break;\
       };\
      };\
     };\
    };\
    if( use_xml ){ draw_xml(obj); }\
    dragstuff.draw();dragstuff.valid = false;\
   };\
  };\
 };\
 return;\
};\
");
  }
  if(anim == 0){fprintf(js_include_file,"\n/* add_slider */\
if( typeof(unit_x) === 'undefined' ){var unit_x = ' ';};\
if( typeof(unit_y) === 'undefined' ){var unit_y = ' ';};\
function slider(XYR,text,slider_id ,width,height,linewidth,fillcolor,strokecolor,opacity,min,max,fontfamily,display){\
 if( wims_status == \"done\" ){return;};var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
 var xcenter = 0.5*width;var rcenter = 0.5*width;fillcolor = 'rgba('+fillcolor+','+opacity[0]+')' || 'rgba(200,200,0,0.2)';\
 strokecolor = 'rgba('+strokecolor+','+opacity[1]+')';var title = document.createElement('SPAN');title.innerHTML = text;\
 var br = document.createElement('BR');tooltip_div.appendChild(br);tooltip_div.appendChild(title);\
 var slider_canvas = document.createElement('canvas');slider_canvas.id = 'canvas'+slider_id;tooltip_div.appendChild(slider_canvas);\
 slider_canvas.width = width;slider_canvas.height = height;var slider_ctx = slider_canvas.getContext(\"2d\");\
 slider_ctx.font = fontfamily;slider_ctx.strokeStyle = strokecolor;\
 slider_ctx.fillStyle = fillcolor;slider_ctx.lineWidth = linewidth;slider_ctx.save();\
 slider_ctx.beginPath();slider_ctx.moveTo(0,height-10);slider_ctx.lineTo(width,height-10);\
 slider_ctx.moveTo(xcenter-10,10);slider_ctx.lineTo(xcenter+10,10);slider_ctx.lineTo(xcenter,height-10);\
 slider_ctx.lineTo(xcenter-10,10);slider_ctx.closePath();slider_ctx.stroke();slider_ctx.fill();\
 slider_canvas.addEventListener( 'mousemove' ,slide_me,false);\
 slider_canvas.addEventListener( 'mouseup' ,stop_slide_me,false);\
 slider_canvas.addEventListener( 'mousedown' ,start_slide_me,false);\
 slider_canvas.addEventListener( 'touchstart', function(e){ e.preventDefault();start_slide_me(e.changedTouches[0]);},false);\
 slider_canvas.addEventListener( 'touchend',   function(e){ e.preventDefault();stop_slide_me(e.changedTouches[0]);},false);\
 slider_canvas.addEventListener( 'touchmove' ,function(e){ e.preventDefault();slide_me(e.changedTouches[0]);},false);\
 var slider_active = false;var value;\
 function stop_slide_me(evt){slider_active = false;};\
 function start_slide_me(evt){slider_active = true;};\
 function slide_me(evt){\
  if( slider_active == false ){return;};\
  var canvas_rect = slider_canvas.getBoundingClientRect();\
  slider_ctx.clearRect(0,0,width,height);\
  var px = parseInt(evt.clientX - canvas_rect.left);\
  slider_ctx.beginPath();slider_ctx.moveTo(0,height-10);slider_ctx.lineTo(width,height-10);\
  slider_ctx.moveTo(px-10,10);slider_ctx.lineTo(px+10,10);slider_ctx.lineTo(px,height-10);\
  slider_ctx.lineTo(px-10,10);slider_ctx.closePath();slider_ctx.stroke();slider_ctx.fill();\
  value = (px - rcenter)/width*(max - min);\
  if( display > 0 ){show_slider_value(value,display);};\
  var dx = px - xcenter;xcenter = px;\
  slider_show_it(XYR,dx,value,slider_id);\
 };\
};",canvas_root_id);
  }
  if( anim == 1){
    fprintf(js_include_file,"\n/* add_anim_slider */\
function animslider(XYR,text,slider_id ,width,height,linewidth,fillcolor,strokecolor,opacity,min,max,fontfamily,display)\
{\
 document.addEventListener('readystatechange',function(event){\
  if(event.target.readyState === 'complete'){\
   var value= 0;\
   var timer = setInterval(function(){\
    value++;slider_show_it(XYR,value,value,slider_id);\
    if( value > width ){clearInterval( timer );}\
   },240);\
  };\
 },false);\
};");
  }

  if( anim == 2 ){/* use mouse in stead of a X|Y slider... external images -via command 'copy' NOT supported !! need to add 'copy' to dragstuff library or introduce another command*/
    fprintf(js_include_file,"\n/* add group move_ */\
if( typeof( slidergroup) !== 'object'){var slidergroup = [];};\
var move_group = function(num){\
 canvas_div.addEventListener( 'mousemove' ,slidegroup_m,false);\
 canvas_div.addEventListener( 'mousedown' ,slidegroup_d,false);\
 canvas_div.addEventListener( 'mouseup'   ,slidegroup_u,false);\
 canvas_div.addEventListener( 'touchstart', function(e){ e.preventDefault();slidegroup_d(e.changedTouches[0]);},false);\
 canvas_div.addEventListener( 'touchend',   function(e){ e.preventDefault();slidegroup_u(e.changedTouches[0]);},false);\
 canvas_div.addEventListener( 'touchmove' ,function(e){ e.preventDefault();slidegroup_m(e.changedTouches[0]);},false);\
 var slide_start = false;\
 function slidegroup_d(e){slide_start = true;};\
 function slidegroup_u(e){slide_start = false;};\
 function slidegroup_m(e){\
  if(!slide_start){return;};\
  var xy;var d_x;var d_y;var len = slidergroup.length;var use_xml = false;var once = 1;\
  for(var i = 0;i < len;i++){\
   use_xml = false;\
   if( typeof(slidergroup[i]) === 'object' ){\
    var obj = slidergroup[i];\
    if( obj.use_slider == num ){\
     if( typeof(obj.type) !== 'number' ){use_xml = true;};\
     xy = getMouse(e,dragstuff.canvas);\
     if( once == 1 ){ d_x = xy.x - obj.x[0];d_y = xy.y - obj.y[0];once = 0;};\
     obj = move(obj,d_x,d_y);\
     if(obj.use_snap != 0 ){var lx = (obj.x).length;for(var t = 0;t < lx;t++){var xy = multisnap_check(obj.x[t],obj.y[t],obj.use_snap);obj.x[t] = xy[0];obj.y[t] = xy[1];};};\
     dragdrop_reply[obj.object_cnt].x = obj.x;dragdrop_reply[obj.object_cnt].y = obj.y;\
     if( use_xml ){obj.onclick = 5;draw_xml(obj);}else{dragstuff.selection = null;dragstuff.draw();dragstuff.valid = false;};\
    };\
   };\
  };\
  dragstuff.valid = true;\
 };\
};");
  }

}
/*      dragdrop_reply[obj.object_cnt] = obj;
*/
/*
adds inputfield for x-value: returns the js-calculated y-value after click on 'OK' button
draws a non-configurable crosshair on this calculated location
*/
void add_calc_y(char *jsmath,int font_size,char *css_class){
  fprintf(js_include_file,"\n/* add_calc_y */\
use_jsmath=1;\
function add_calc_y(){\
 if( wims_status == \"done\" ){return;};\
 var fun = to_js_math(\"%s\");if(fun == null){return;};\
 function eval_jsmath(x){return parseFloat(eval(fun));};\
 var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
 var calc_div = document.createElement('div');\
 calc_div.id = \"calc_div\";\
 tooltip_div.appendChild(calc_div);\
 var label_x = \"x\";var label_y = \"y\";\
 if( typeof(xaxislabel) !== 'undefined' ){label_x = xaxislabel;}\
 if( typeof(yaxislabel) !== 'undefined' ){label_y = yaxislabel;}\
 calc_div.innerHTML=\"<br><span style='font-style:italic;font-size:%dpx'>\"+label_x+\" : <input type='text' size='4' value='' id='calc_input_x' style='%s'>&nbsp;\"+ label_y+\" : <input type='text' size='5' value='' id='calc_output_y' style='%s' readonly><input id='calc_button' type='button' value='OK' onclick=''  style='color:red;background-color:lightblue;'></span> \";\
 var calc_button = document.getElementById(\"calc_button\");\
 calc_button.addEventListener(\"mousedown\",show_it,false);\
 calc_button.addEventListener(\"touchstart\", function(e){ e.preventDefault();show_it(e.changedTouches[0]);},false);\
 function show_it(){\
  var x_value=document.getElementById(\"calc_input_x\").value;\
  var y_value = eval_jsmath(x_value);\
  document.getElementById(\"calc_output_y\").value = y_value;\
  if(isNaN(y_value)){return;};\
  var canvas = create_canvas%d(123,xsize,ysize);\
  var ctx = canvas.getContext(\"2d\");\
  draw_crosshairs(ctx,[x2px(x_value)],[y2px(y_value)],1,5,\"#000000\",1,0,0,0,[0,0]);return;\
 };\
};\
;add_calc_y();",jsmath,canvas_root_id,font_size,css_class,css_class,canvas_root_id);
}
/*
 x-value of the mouse will be used to calculate via javascript the corresponding y-value using the verbatim js-math function
 a configurable crosshair and vertical/horizontal crosshair lines will be drawn
 function is called "use_mouse_coordinates() and thus can not be combined with command 'mouse'
*/
void add_trace_js_mouse(int canvas_cnt, char *stroke_color, char *jsmath,
  int font_size, double stroke_opacity, int line_width,
  int crosshair_size,char *css_class){
    fprintf(js_include_file,"\n/* add_trace_jsmath */\
use_jsmath=1;\
function use_trace_jsmath(){\
if( wims_status == \"done\" ){return;};\
 var label_x = \"x\";var label_y = \"y\";\
 if( typeof(xaxislabel) !== 'undefined' ){label_x = xaxislabel;}\
 if( typeof(yaxislabel) !== 'undefined' ){label_y = yaxislabel;}\
 var trace_canvas = create_canvas%d(%d,xsize,ysize);\
 var trace_context = trace_canvas.getContext(\"2d\");\
 var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
 var trace_div = document.createElement('div');\
 trace_div.id = \"trace_div\";\
 tooltip_div.appendChild(trace_div);\
 trace_div.innerHTML = \"<br><span style='font-style:italic;font-size:%dpx'>\"+label_x+\" : <input type='text' size='4' value='' id='trace_input_x' style='%s'>\"+label_y+\" : <input type='text' size='5' value='' id='trace_input_y' style='%s' readonly></span> \";\
 canvas_div.addEventListener(\"mousemove\",trace,false);\
 canvas_div.addEventListener(\"touchmove\",function(e){ e.preventDefault();trace(e.changedTouches[0]);},false);\
 var fun = to_js_math(\"%s\");if(fun == null){return;};\
 function eval_jsmath(x){return parseFloat(eval(fun));};\
 function trace(evt){\
  var mouse = getMouse(evt,trace_canvas);\
  var x_px = mouse.x;\
  var x = px2x(x_px);\
  var y = eval_jsmath(x);\
  if(isNaN(y)){return;};\
  var y_px = y2px(y);\
  trace_context.clearRect(0,0,xsize,ysize);\
  draw_crosshairs(trace_context,[x_px],[y_px],%d,%d,\"%s\",%f,0,0,0,[0,0]);\
  document.getElementById(\"trace_input_x\").value = x;\
  document.getElementById(\"trace_input_y\").value = y;\
 };\
 return;\
};use_trace_jsmath();",canvas_root_id,canvas_cnt,canvas_root_id,font_size,css_class,css_class,jsmath,line_width,crosshair_size,stroke_color,stroke_opacity);
  }

/*
add a table with 2 textarea's labeled 'x' 'y' ( or 'xlabel' 'ylabel' if defined)
add two buttons: OK and NOK (OK draws; NOK will delete last item pair from userdraw_x / userdraw_y array's
*/
void add_textarea_xy(char *css_class){
  fprintf(js_include_file,"\n/* add_textarea_xy */\
function add_textarea_xy(){\
if( wims_status == \"done\" ){return;};\
var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
var textarea_div = document.createElement('div');\
textarea_div.id = \"textarea_div\";\
tooltip_div.appendChild(textarea_div);\
var label_x = \"x\";var label_y = \"y\";\
if( typeof(xaxislabel) !== 'undefined' ){label_x = xaxislabel;}\
if( typeof(yaxislabel) !== 'undefined' ){label_y = yaxislabel;}\
textarea_div.innerHTML=\"\
<table style=\'border:1px solid black;background-color:#ffffa0\' >\
<tr>\
 <td><input id='textarea_ok_button' type='button' value='OK' onclick='' style='color:red;background-color:lightblue;'></td>\
 <td><input id='textarea_nok_button' type='button' value='NOK' onclick='' style='color:blue;background-color:red;'></td>\
</tr>\
<tr>\
 <td><em>\"+label_x+\"</em></td>\
 <td><em>\"+label_y+\"</em></td>\
</tr>\
<tr>\
 <td><textarea rows='5' cols='2' id='userinput_x' style='%s' ></textarea></td>\
 <td><textarea rows='5' cols='2' id='userinput_y' style='%s' ></textarea></td>\
</tr>\
</table>\";\
var textarea_ok_button = document.getElementById(\"textarea_ok_button\");\
var textarea_nok_button = document.getElementById(\"textarea_nok_button\");\
textarea_ok_button.addEventListener(\"mousedown\",function(e){redraw_userdraw();return;},false);\
textarea_nok_button.addEventListener(\"mousedown\",function(e){remove_last();return;},false);\
return;\
};add_textarea_xy();",canvas_root_id,css_class,css_class);
  }

/*
*/
void add_setlimits(int font_size,char *css_class){
  fprintf(js_include_file,"\n/* add_setlimits */\
function use_setlimits(){\
if( wims_status == \"done\" ){return;};\
var label_x = \"x\";var label_y = \"y\";\
if( typeof(xaxislabel) !== 'undefined' ){label_x = xaxislabel;}\
if( typeof(yaxislabel) !== 'undefined' ){label_y = yaxislabel;}\
var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
var setlim_div = document.createElement('div');\
setlim_div.id = \"setlim_div\";\
tooltip_div.appendChild(setlim_div);\
setlim_div.innerHTML=\"<br><span style='font-style:italic;font-size:%dpx'>\"+label_x+\"min = <input type='text' size='4' value='\"+xmin+\"' id='userinput_xmin' style='%s'> \"+label_x+\"max = <input type='text' size='4' value='\"+xmax+\"' id='userinput_xmax' style='%s'><br>\"+label_y+\"min = <input type='text' size='4' value='\"+ymin+\"' id='userinput_ymin' style='%s'> \"+label_y+\"max = <input type='text' size='4' value='\"+ymax+\"' id='userinput_ymax' style='%s'><br><input id='set_limits' type='button' value='OK' onclick='' style='color:red;background-color:lightblue;'>\";\
var setlimit_button = document.getElementById(\"set_limits\");\
function set_limits(e){\
xmin = safe_eval(document.getElementById('userinput_xmin').value);\
xmax = safe_eval(document.getElementById('userinput_xmax').value);\
ymin = safe_eval(document.getElementById('userinput_ymin').value);\
ymax = safe_eval(document.getElementById('userinput_ymax').value);\
if(xmin > xmax || ymin > ymax){alert(\"your limits are not correct...\");return;}\
try{redraw_jsplot();}catch(e){console.log('redraw_jsplot failed:'+e);};\
try{redraw_grid();}catch(e){console.log('redraw_grid failed:'+e);};\
try{dragstuff.Zoom(xmin,xmax,ymin,ymax)}catch(e){console.log('zooming dragstuff failed:'+e);};return;};\
setlimit_button.addEventListener(\"mousedown\",function(e){set_limits();},false);\
};use_setlimits();",canvas_root_id,font_size,css_class,css_class,css_class,css_class);
}

void add_input_jsfunction(char *css_class,char *input_label,int input_cnt,char *stroke_color,float stroke_opacity,int line_width,int use_dashed,int dashtype0,int dashtype1,int font_size){
  fprintf(js_include_file,"\n/* add_input_jsfunction */\
function clear_jsfunction(canvas_plot_id,input_field){\
 try{\
  var canvas_plot = document.getElementById(\"wims_canvas%d\"+canvas_plot_id);\
  var canvas_plot_ctx = canvas_plot.getContext(\"2d\");\
  if( confirm(\"clear function plot?\") ){\
   canvas_plot_ctx.clearRect(0,0,xsize,ysize);\
   document.getElementById(input_field).value = \"\";\
  };\
  return;\
 }catch(e){alert(e+\"nothing to remove...\");};\
 return;\
};\
function add_input_jsfunction(input_cnt,css_class,input_label,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1){\
 var canvas_plot_id = %d+input_cnt;\
 var label = input_label[input_cnt];\
 if( typeof(multistrokecolors) === 'object'){ stroke_color = multistrokecolors[input_cnt];};\
 var input_field = \"canvas_input\"+input_cnt;\
 var update_button_id = \"update_button\"+input_cnt;\
 var delete_button_id = \"delete_button\"+input_cnt;\
 if( wims_status == \"done\" ){return;};\
 var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
 var input_jsfunction_div = document.createElement('div');\
 input_jsfunction_div.id = \"input_jsfunction_div\"+input_cnt;\
 tooltip_div.appendChild(input_jsfunction_div);\
 input_jsfunction_div.innerHTML+=\"<br><span style='font-style:italic;font-size:%dpx;color:rgb(\"+stroke_color+\")'><b>\"+label+\" <input type='text' size='16' value='' id='\"+input_field+\"' style='\"+css_class+\"'></b><input id='\"+update_button_id+\"' type='button' value='OK' onclick='' style='color:red;background-color:lightblue;'><input id='\"+delete_button_id+\"' type='button' value='NOK' onclick='' style='color:blue;background-color:red;'></span> \";\
 var update_button = document.getElementById(update_button_id);\
 var delete_button = document.getElementById(delete_button_id);\
 update_button.addEventListener(\"mousedown\",function(e){jsplot(canvas_plot_id,[rawmath(document.getElementById(input_field).value)],[line_width],[stroke_color],[stroke_opacity],[use_dashed],dashtype0,dashtype1,0,0,300,0,1);return;},false);\
 delete_button.addEventListener(\"mousedown\",function(e){clear_jsfunction(canvas_plot_id,input_field);return;},false);\
};\
add_input_jsfunction(%d,\"%s\",%s,%d,\"%s\",%.2f,%d,%d,%d);",canvas_root_id,USERDRAW_JSPLOT,canvas_root_id,font_size,input_cnt,css_class,input_label,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1);
}
/*
adds 2 inputfields (x:y) and 'ok' | 'nok' button
these are used for user drawing with inputfields...
function update_me(){\
 var x = safe_eval(document.getElementById('userinput_x').value);\
 var y = safe_eval(document.getElementById('userinput_y').value);\
 userdraw_x.push(x2px(x));userdraw_y.push(y2px(y));\
 return;\
};\
*/
void add_input_xy(int font_size,char *css_class){
  fprintf(js_include_file,"\n/* add_input_xy */\
function add_input_xy(){\
if( wims_status == \"done\" ){return;};\
var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
var input_xy_div = document.createElement('div');\
input_xy_div.id = \"input_xy_div\";\
tooltip_div.appendChild(input_xy_div);\
var label_x = \"x\";var label_y = \"y\";\
input_xy_div.innerHTML=\"<br><span style='font-style:italic;font-size:%dpx'><b>( <input type='text' size='5' value='' id='userinput_x' style='%s'> : <input type='text' size='5' value='' id='userinput_y' style='%s'> )</b><input id='update_button' type='button' value='OK' onclick=''  style='color:red;background-color:lightblue;'><input id='delete_button' type='button' value='NOK' onclick='' style='color:blue;background-color:red;'></span> \";\
var update_button = document.getElementById(\"update_button\");\
var delete_button = document.getElementById(\"delete_button\");\
update_button.addEventListener(\"mousedown\",function(e){update_me();redraw_userdraw();return;},false);\
delete_button.addEventListener(\"mousedown\",function(e){remove_last();return;},false);\
};add_input_xy();",canvas_root_id,font_size,css_class,css_class);
}

/* adds 4 inputfields (x1:y1) --- (x2:y2) and 'ok' + 'nok' button */
void add_input_x1y1x2y2(int font_size,char *css_class){
  fprintf(js_include_file,"\n/* add_input_x1y1x2y2 */\
function add_input_x1y1x2y2(){\
if( wims_status == \"done\" ){return;};\
var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
var input_x1y1x2y2_div = document.createElement('div');\
input_x1y1x2y2_div.id = \"input_x1y1x2y2_div\";\
tooltip_div.appendChild(input_x1y1x2y2_div);\
input_x1y1x2y2_div.innerHTML=\"<br><span style='font-size:%dpx'><b>( <input type='text' size='5' value='' id='userinput_x1' style='%s'> : <input type='text' size='5' value='' id='userinput_y1' style='%s'> ) ----- ( <input type='text' size='5' value='' id='userinput_x2' style='%s'> : <input type='text' size='5' value='' id='userinput_y2' style='%s'> )</b><input id='update_button' type='button' value='OK' onclick='' style='color:red;background-color:lightblue;'><input id='delete_button' type='button' value='NOK' onclick='' style='color:blue;background-color:red;'></span> \";\
var update_button = document.getElementById(\"update_button\");\
var delete_button = document.getElementById(\"delete_button\");\
update_button.addEventListener(\"mousedown\",function(e){redraw_userdraw();return;},false);\
delete_button.addEventListener(\"mousedown\",function(e){remove_last();return;},false);\
};add_input_x1y1x2y2();",canvas_root_id,font_size,css_class,css_class,css_class,css_class);
}

/* adds 3 inputfields Center (x:y) Radius r and 'ok'+'nok' buttons */
void add_input_xyr(int font_size,char *css_class){
  fprintf(js_include_file,"\n/* add_input_xyr */\
function add_input_xyr(){\
if( wims_status == \"done\" ){return;};\
var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
var input_xyr_div = document.createElement('div');\
input_xyr_div.id = \"input_xyr_div\";\
tooltip_div.appendChild(input_xyr_div);\
input_xyr_div.innerHTML=\"<br><span style='font-style:italic;font-size:%dpx'><b>Center : ( <input type='text' size='5' value='' id='userinput_x' style='%s'> : <input type='text' size='5' value='' id='userinput_y' style='%s'> ) Radius : <input type='text' size='5' value='' id='userinput_r' style='%s'></b><input id='update_button' type='button' value='OK' onclick='' style='color:red;background-color:lightblue;'><input id='delete_button' type='button' value='NOK' onclick='' style='color:blue;background-color:red;'></span> \";\
var update_button = document.getElementById(\"update_button\");\
var delete_button = document.getElementById(\"delete_button\");\
update_button.addEventListener(\"mousedown\",function(e){redraw_userdraw();return;},false);\
delete_button.addEventListener(\"mousedown\",function(e){remove_last();return;},false);\
};add_input_xyr();",canvas_root_id,font_size,css_class,css_class,css_class);
}

/* THESE JS-FUNCTIONS COULD BE MADE LESS COPY & PASTE "PROGRAMMING" */

/* draw circle(s) / point(s) via 3 inputfields */
void add_input_circle(int type,int num){
/*
type = 0 : a point ...radius is fixed
type = 1 : a circle ... read inputfield userinput_r
num = 1 : a single point / circle
num = 2 : multiple points / circles
*/
  fprintf(js_include_file,"\n/* add_input_circle */\
function user_redraw(t){\
 var type = %d;\
 var num = %d;\
 var lu = userdraw_x.length;\
 if( t == -1 && lu > 0){userdraw_x.splice(lu-1,1);userdraw_y.splice(lu-1,1);if(type == 1){userdraw_radius.splice(lu-1,1);};context_userdraw.clearRect(0,0,xsize,ysize);draw_circles(context_userdraw,userdraw_x,userdraw_y,userdraw_radius,line_width,stroke_color,stroke_opacity,use_filled,fill_color,fill_opacity,use_dashed,dashtype0,dashtype1);xy_cnt = userdraw_x.length;return;};\
 var add_x = safe_eval( document.getElementById(\"userinput_x\").value );\
 var add_y = safe_eval( document.getElementById(\"userinput_y\").value );\
 if( add_x != null && add_y != null ){if( type == 1 ){var add_r = safe_eval( document.getElementById(\"userinput_r\").value );if( add_r == null ){alert(\"illegal radius input \");return;};if( num == 1 ){userdraw_radius[0] = parseInt(Math.abs(xsize*(add_r)/(xmax - xmin)));}else{userdraw_radius.push( parseInt(Math.abs(xsize*(add_r)/(xmax - xmin))) );};}else{userdraw_radius[lu] = userdraw_radius[0];};if( num == 1 ){userdraw_x[0] = x2px(add_x);userdraw_y[0] = y2px(add_y);xy_cnt=1;}else{userdraw_x.push(x2px(add_x));userdraw_y.push(y2px(add_y));xy_cnt = userdraw_x.length;};context_userdraw.clearRect(0,0,xsize,ysize);draw_circles(context_userdraw,userdraw_x,userdraw_y,userdraw_radius,line_width,stroke_color,stroke_opacity,use_filled,fill_color,fill_opacity,use_dashed,dashtype0,dashtype1);};\
 return;\
};",type,num);
}
/* draw crosshairs via inputfields x/y */
void add_input_crosshair(int num){
  fprintf(js_include_file,"\n/* add_input_crosshair */\
function user_redraw(t){\
 var lu = userdraw_x.length;\
 if( t == -1 && lu > 0){\
  userdraw_x.splice(lu-1,1);\
  userdraw_y.splice(lu-1,1);\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_crosshairs(context_userdraw,userdraw_x,userdraw_y,line_width,crosshair_size,stroke_color,stroke_opacity,0,0,0,[0,0]);\
  return;\
 };\
 var add_x = safe_eval( document.getElementById(\"userinput_x\").value );\
 var add_y = safe_eval( document.getElementById(\"userinput_y\").value );\
 if( add_x != null && add_y != null ){\
  if( %d == 1 ){\
   userdraw_x[0] = x2px(add_x);\
   userdraw_y[0] = y2px(add_y);\
  } else {\
    userdraw_x[lu] = x2px(add_x);\
    userdraw_y[lu] = y2px(add_y);\
    xy_cnt++;\
  };\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_crosshairs(context_userdraw,userdraw_x,userdraw_y,line_width,crosshair_size,stroke_color,stroke_opacity,0,0,0,[0,0]);\
 };\
 return;\
};",num);
}

/* draw arrows via inputfields x/y */
void add_input_arrow(int num){
  fprintf(js_include_file,"\n/* add_input_arrow */\
function user_redraw(t){\
 var lu = userdraw_x.length;\
 if( t == -1 && lu > 1 ){\
  userdraw_x.splice(lu-2,2);\
  userdraw_y.splice(lu-2,2);\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_arrows(context_userdraw,userdraw_x,userdraw_y,arrow_head,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1,type,use_rotate,angle,0,[1,0,0,1,0,0]);\
  return;\
 };\
 var add_x1 = safe_eval( document.getElementById(\"userinput_x1\").value );\
 var add_y1 = safe_eval( document.getElementById(\"userinput_y1\").value );\
 var add_x2 = safe_eval( document.getElementById(\"userinput_x2\").value );\
 var add_y2 = safe_eval( document.getElementById(\"userinput_y2\").value );\
 if( add_x1 != null && add_y1 != null && add_x2 != null && add_y2 != null ){\
  if( %d == 2 ){\
    var s = userdraw_x.length;\
    userdraw_x[lu] = x2px(add_x1);\
    userdraw_y[lu] = y2px(add_y1);\
    userdraw_x[lu+1] = x2px(add_x2);\
    userdraw_y[lu+1] = y2px(add_y2);\
  } else {\
   userdraw_x[0] = x2px(add_x1);\
   userdraw_y[0] = y2px(add_y1);\
   userdraw_x[1] = x2px(add_x2);\
   userdraw_y[1] = y2px(add_y2);\
  };\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_arrows(context_userdraw,userdraw_x,userdraw_y,arrow_head,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1,type,use_rotate,angle,0,[1,0,0,1,0,0]);\
 };\
 return;\
};",num);
}

/* draw line via inputfields x/y */
void add_input_line(int num){
  fprintf(js_include_file,"\n/* line via inputfields  */\
function user_redraw(t){\
 var lu = userdraw_x.length;\
 if( t == -1 && lu > 1){\
  userdraw_x.splice(lu-2,2);\
  userdraw_y.splice(lu-2,2);\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_lines(context_userdraw,userdraw_x,userdraw_y,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1,1,0,0);\
  return;\
 };\
 var add_x1 = safe_eval( document.getElementById(\"userinput_x1\").value );\
 var add_y1 = safe_eval( document.getElementById(\"userinput_y1\").value );\
 var add_x2 = safe_eval( document.getElementById(\"userinput_x2\").value );\
 var add_y2 = safe_eval( document.getElementById(\"userinput_y2\").value );\
 if( add_x1 != null && add_y1 != null && add_x2 != null && add_y2 != null ){\
  if( %d == 2 ){\
    userdraw_x[lu] = x2px(add_x1);\
    userdraw_y[lu] = y2px(add_y1);\
    userdraw_x[lu+1] = x2px(add_x2);\
    userdraw_y[lu+1] = y2px(add_y2);\
  } else {\
   userdraw_x[0] = x2px(add_x1);\
   userdraw_y[0] = y2px(add_y1);\
   userdraw_x[1] = x2px(add_x2);\
   userdraw_y[1] = y2px(add_y2);\
  };\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_lines(context_userdraw,userdraw_x,userdraw_y,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1,1,0,0);\
 };\
 return;\
};",num);
}


/* draw polyline via inputfields x/y */
void add_input_polyline(){
  fprintf(js_include_file,"\n/* polyline_segment via inputfields */\
function user_redraw(t){\
 var lu = userdraw_x.length;\
 cnt = 1;\
 if( t == -1 && lu > 0){\
  userdraw_x.splice(lu-1,1);\
  userdraw_y.splice(lu-1,1);\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_polyline(context_userdraw,userdraw_x,userdraw_y,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1);\
  return;\
 };\
 var add_x = safe_eval( document.getElementById(\"userinput_x\").value );\
 var add_y = safe_eval( document.getElementById(\"userinput_y\").value );\
 if(add_x != null && add_y != null ){\
  userdraw_x.push(x2px(add_x));\
  userdraw_y.push(y2px(add_y));\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_polyline(context_userdraw,userdraw_x,userdraw_y,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1);\
 };\
 return;\
};");
}
/* draw segment(s) via inputfields x/y */
void add_input_segment(int num){
  fprintf(js_include_file,"\n/* add_input_segment */\
function user_redraw(t){\
 var lu = userdraw_x.length;\
 if( t == -1 && lu > 1){\
  userdraw_x.splice(lu-2,2);\
  userdraw_y.splice(lu-2,2);\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_segments(context_userdraw,userdraw_x,userdraw_y,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1);\
  return;\
 };\
 var add_x1 = safe_eval( document.getElementById(\"userinput_x1\").value );\
 var add_y1 = safe_eval( document.getElementById(\"userinput_y1\").value );\
 var add_x2 = safe_eval( document.getElementById(\"userinput_x2\").value );\
 var add_y2 = safe_eval( document.getElementById(\"userinput_y2\").value );\
 if( add_x1 != null && add_y1 != null && add_x2 != null && add_y2 != null ){\
  if( %d == 2 ){\
    var s = userdraw_x.length;\
    userdraw_x[s] = x2px(add_x1);\
    userdraw_y[s] = y2px(add_y1);\
    userdraw_x[s+1] = x2px(add_x2);\
    userdraw_y[s+1] = y2px(add_y2);\
  } else {\
   userdraw_x[0] = x2px(add_x1);\
   userdraw_y[0] = y2px(add_y1);\
   userdraw_x[1] = x2px(add_x2);\
   userdraw_y[1] = y2px(add_y2);\
  };\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_segments(context_userdraw,userdraw_x,userdraw_y,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1);\
 };\
 return;\
};",num);
}

/* draw demilines(s) via inputfields x/y */
void add_input_demiline(int num){
  fprintf(js_include_file,"\n/* add_input_segment */\
function user_redraw(t){\
 var lu = userdraw_x.length;\
 if( t == -1 && lu > 1){\
  userdraw_x.splice(lu-2,2);\
  userdraw_y.splice(lu-2,2);\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_demilines(context_userdraw,userdraw_x,userdraw_y,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1);\
  return;\
 };\
 var add_x1 = safe_eval( document.getElementById(\"userinput_x1\").value );\
 var add_y1 = safe_eval( document.getElementById(\"userinput_y1\").value );\
 var add_x2 = safe_eval( document.getElementById(\"userinput_x2\").value );\
 var add_y2 = safe_eval( document.getElementById(\"userinput_y2\").value );\
 if( add_x1 != null && add_y1 != null && add_x2 != null && add_y2 != null ){\
  if( %d == 2 ){\
    var s = userdraw_x.length;\
    userdraw_x[s] = x2px(add_x1);\
    userdraw_y[s] = y2px(add_y1);\
    userdraw_x[s+1] = x2px(add_x2);\
    userdraw_y[s+1] = y2px(add_y2);\
  } else {\
   userdraw_x[0] = x2px(add_x1);\
   userdraw_y[0] = y2px(add_y1);\
   userdraw_x[1] = x2px(add_x2);\
   userdraw_y[1] = y2px(add_y2);\
  };\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  draw_demilines(context_userdraw,userdraw_x,userdraw_y,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1);\
 };\
 return;\
};",num);
}

void add_clear_button(char *css_class,char *button_text){
/* 25/11/2014 added clearing of reply array
all members will be set to 0 eg reply[0] = 0 , reply[1] = 0 ...
hope this does not interfere with existing work...
*/
/*
5/2016 changed  to 'setAttribute()' because of trouble on Chromium/Safari/IE
10/2016 corrected contex-reset-flaw when using "userdraw text,color" and added inputs to the things we can remove
7/2017 added 'userdraw clickfill,color' to removable things...one user_filling per click

7/2022: preserve all div's with html ('<' && '>') eg mathml,katex,html and all external objects placed with function 'place_image_on_canvas(id)'
...hope this does not interfere with other things
https://wimsedu.info/?topic=geometrie-interactive-avec-canvasdraw
*/
  fprintf(js_include_file,"\n/* add clear button */\
clear_draw_area%d = function(){\
 if(typeof(fill_canvas_no) === 'number'){\
  var chk = document.getElementById('wims_canvas%d'+fill_canvas_no);\
  if( chk ){\
   var fill_ctx = chk.getContext(\"2d\");fill_ctx.clearRect(0,0,xsize,ysize);\
   fill_canvas_no--;userdraw_x.splice(p,1);userdraw_y.splice(p,1);userdraw_radius.splice(p,1);\
   return;\
  };\
 };\
 if( typeof(context_userdraw) === 'object' ){\
  if(document.getElementById(canvas_div.lastChild.id).tagName == 'DIV'){\
   var mathtxt = document.getElementById(canvas_div.lastChild.id).innerHTML;\
   if(mathtxt.indexOf('<') == -1 && mathtxt.indexOf('>') == -1 || canvas_div.lastChild.id.indexOf('placed') != -1){\
    document.getElementById(canvas_div.lastChild.id).innerHTML = null;\
    canvas_div.removeChild(document.getElementById(canvas_div.lastChild.id));\
   };\
  };\
  context_userdraw.clearRect(0,0,xsize,ysize);\
  if( typeof(userdraw_text) !== 'undefined'){ userdraw_text = []; };\
  if( document.getElementById(\"canvas_input0\") ){\
   var p = 0;var inp;\
   while( document.getElementById(\"canvas_input\"+p) ){\
    inp = document.getElementById(\"canvas_input\"+p);\
    canvas_div.removeChild(inp);\
    p++;\
   };\
   input_cnt = 0;start_input_cnt = 0; \
  };\
  userdraw_x = [];userdraw_y = [];userdraw_radius = [];xy_cnt = 0;\
 };\
 if( typeof(external_ctx) === 'object'){\
  external_ctx.clearRect(0,0,xsize,ysize);\
  for(var p = 0 ; p < userdraw_x.length; p++){\
   userdraw_x[p] = null;userdraw_y[p] = null;\
  };\
 };\
 return;\
};\
function add_clear_button(){\
 var tooltip_placeholder_div = document.getElementById(\"tooltip_placeholder_div%d\");\
 var button = document.createElement('input');\
 button.setAttribute(\"type\" , \"button\");\
 button.setAttribute(\"id\" , \"clearbutton%d\");\
 button.setAttribute(\"class\" , \"%s\");\
 button.setAttribute(\"value\" , \"%s\");\
 button.setAttribute(\"onclick\",\"clear_draw_area%d()\");\
 tooltip_placeholder_div.appendChild(button);\
};\
add_clear_button();",canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,css_class,button_text,canvas_root_id);
}

void add_color_palette(char *css_class){
  fprintf(js_include_file,"\n/* add color palette */\
function add_color_palette(){\
 var tooltip_placeholder_div = document.getElementById(\"tooltip_placeholder_div%d\");\
 var clen = palettecolors.length;\
 for( var p = 0 ; p < clen ; p++ ){\
  var button = document.createElement('input');\
  button.setAttribute('type', 'button');\
  button.setAttribute('value', '  ');\
  button.setAttribute('id',palettecolors[p]);\
  button.setAttribute('style', \"%s\");\
  button.setAttribute('style','background-color:rgb('+palettecolors[p]+')');\
  button.setAttribute(\"backgroundColor\",palettecolors[p]);\
  button.addEventListener('mousedown',function(e){var id = this.getAttribute('id');multifillcolors[0]=id;multifillcolors[1]=id;multifillcolors[2]=id;},false);\
  tooltip_placeholder_div.appendChild(button);\
 };\
};\
add_color_palette();\
",canvas_root_id,css_class);
}


struct colors colors[]={
 {"#FF0000","red","255,0,0"},
 {"#00FF00","lime","0,255,0"},
 {"#0000FF","blue","0,0,255"},
 {"#FFA500","orange","255,165,0"},
 {"#E9967A","darksalmon","233,150,122"},
 {"#F0FFFF","azure","240,255,255"},
 {"#CD5C5C","indianred","205,92,92"},
 {"#E0FFFF","lightcyan","224,255,255"},
 {"#FFE4E1","mistyrose","255,228,225"},
 {"#DC143C","crimson","220,20,60"},
 {"#FFC0CB","pink","255,192,203"},
 {"#FFD700","gold","255,215,0"},
 {"#E6E6FA","lavender","230,230,250"},
 {"#808080","gray","128,128,128"},
 {"#F0F8FF","aliceblue","240,248,255"},
 {"#C71585","mediumvioletred","199,21,133"},
 {"#FFF0F5","lavenderblush","255,240,245"},
 {"#DAA520","goldenrod","218,165,32"},
 {"#FFB6C1","lightpink","255,182,193"},
 {"#00FFFF","aqua","0,255,255"},
 {"#FF69B4","hotpink","255,105,180"},
 {"#00FFFF","cyan","0,255,255"},
 {"#FF1493","deeppink","255,20,147"},
 {"#7FFFD4","aquamarine","127,255,212"},
 {"#FA8072","salmon","250,128,114"},
 {"#DEB887","burlywood","222,184,135"},
 {"#DB7093","palevioletred","219,112,147"},
 {"#D2B48C","tan","210,180,140"},
 {"#BDB76B","darkkhaki","189,183,107"},
 {"#B22222","firebrick","178,34,34"},
 {"#FF4500","orangered","255,69,0"},
 {"#8B4513","saddlebrown","139,69,19"},
 {"#FF8C00","darkorange","255,140,0"},
 {"#FFFFE0","lightyellow","255,255,224"},
 {"#FFFF00","yellow","255,255,0"},
 {"#FFFACD","lemonchiffon","255,250,205"},
 {"#F5F5DC","beige","245,245,220"},
 {"#FFEFD5","papayawhip","255,239,213"},
 {"#FAFAD2","lightgoldenrodyellow","250,250,210"},
 {"#FFE4B5","moccasin","255,228,181"},
 {"#B8860B","darkgoldenrod","184,134,11"},
 {"#FFF8DC","cornsilk","255,248,220"},
 {"#FFEBCD","blanchedalmond","255,235,205"},
 {"#FFE4C4","bisque","255,228,196"},
 {"#FFDEAD","navajowhite","255,222,173"},
 {"#F5DEB3","wheat","245,222,179"},
 {"#CD853F","peru","205,133,63"},
 {"#D2691E","chocolate","210,105,30"},
 {"#A0522D","sienna","160,82,45"},
 {"#A52A2A","brown","165,42,42"},
 {"#BC8F8F","rosybrown","188,143,143"},
 {"#F08080","lightcoral","240,128,128"},
 {"#FFA07A","lightsalmon","255,160,122"},
 {"#8B0000","darkred","139,0,0"},
 {"#800000","maroon","128,0,0"},
 {"#FAA460","sandybrown","250,164,96"},
 {"#FF7F50","coral","255,127,80"},
 {"#FF6347","tomato","255,99,71"},
 {"#FFDAB9","peachpuff","255,218,185"},
 {"#EEE8AA","palegoldenrod","238,232,170"},
 {"#F0E68C","khaki","240,230,140"},
 {"#D8BFD8","thistle","216,191,216"},
 {"#DDA0DD","plum","221,160,221"},
 {"#FF00FF","fuchsia","255,0,255"},
 {"#FF00FF","magenta","255,0,255"},
 {"#EE82EE","violet","238,130,238"},
 {"#DA70D6","orchid","218,112,214"},
 {"#BA55D3","mediumorchid","186,85,211"},
 {"#9370DB","mediumpurple","147,112,219"},
 {"#8A2BE2","blueviolet","138,43,226"},
 {"#9400D3","darkviolet","148,0,211"},
 {"#9932CC","darkorchid","153,50,204"},
 {"#8B008B","darkmagenta","139,0,139"},
 {"#800080","purple","128,0,128"},
 {"#4B0082","indigo","75,0,130"},
 {"#483D8B","darkslateblue","72,61,139"},
 {"#6A5ACD","slateblue","106,90,205"},
 {"#7B68EE","mediumslateblue","123,104,238"},
 {"#98FB98","palegreen","152,251,152"},
 {"#ADFF2F","greenyellow","173,255,47"},
 {"#7FFF00","chartreuse","127,255,0"},
 {"#7CFC00","lawngreen","124,252,0"},
 {"#00FF7F","springgreen","0,255,127"},
 {"#00FA9A","mediumspringgreen","0,250,154"},
 {"#90EE90","lightgreen","144,238,144"},
 {"#32CD32","limegreen","50,205,50"},
 {"#3CB371","mediumseagreen","60,179,113"},
 {"#2E8B57","seagreen","46,139,87"},
 {"#228B22","forestgreen","34,139,34"},
 {"#008000","green","0,128,0"},
 {"#006400","darkgreen","0,100,0"},
 {"#9ACD32","yellowgreen","154,205,50"},
 {"#6B8E23","olivedrab","107,142,35"},
 {"#808000","olive","128,128,0"},
 {"#556B2F","darkolivegreen","85,107,47"},
 {"#8FBC8F","darkseagreen","143,188,143"},
 {"#66CDAA","mediumaquamarine","102,205,170"},
 {"#20B2AA","lightseagreen","32,178,170"},
 {"#008B8B","darkcyan","0,139,139"},
 {"#008080","teal","0,128,128"},
 {"#AFEEEE","paleturquoise","175,238,238"},
 {"#40E0D0","turquoise","64,224,208"},
 {"#48D1CC","mediumturquoise","72,209,204"},
 {"#00CED1","darkturquoise","0,206,209"},
 {"#5F9EA0","cadetblue","95,158,160"},
 {"#4682B4","steelblue","70,130,180"},
 {"#B0C4DE","lightsteelblue","176,196,222"},
 {"#B0E0E6","powderblue","176,224,230"},
 {"#ADD8E6","lightblue","173,216,230"},
 {"#87CEEB","skyblue","135,206,235"},
 {"#87CEFA","lightskyblue","135,206,250"},
 {"#00BFFF","deepskyblue","0,191,255"},
 {"#1E90FF","dodgerblue","30,144,255"},
 {"#6495ED","cornflowerblue","100,149,237"},
 {"#4169E1","royalblue","65,105,225"},
 {"#0000CD","mediumblue","0,0,205"},
 {"#00008B","darkblue","0,0,139"},
 {"#000080","navy","0,0,128"},
 {"#191970","midnightblue","25,25,112"},
 {"#DCDCDC","gainsboro","220,220,220"},
 {"#D3D3D3","lightgrey","211,211,211"},
 {"#808080","grey","128,128,128"},
 {"#C0C0C0","silver","192,192,192"},
 {"#A9A9A9","darkgray","169,169,169"},
 {"#778899","lightslategray","119,136,153"},
 {"#708090","slategray","112,128,144"},
 {"#696969","dimgray","105,105,105"},
 {"#2F4F4F","darkslategray","47,79,79"},
 {"#0a0a0a","black","10,10,10"},
 {"#F5FFFA","mintcream","245,255,250"},
 {"#FFFFFF","white","255,255,255"},
 {"#F0FFF0","honeydew","240,255,240"},
 {"#F5F5F5","whitesmoke","245,245,245"},
 {"#F8F8FF","ghostwhite","248,248,255"},
 {"#FFFFF0","ivory","255,255,240"},
 {"#FFFAFA","snow","255,250,250"},
 {"#FFFAF0","floralwhite","255,250,240"},
 {"#FFF5EE","seashell","255,245,238"},
 {"#FDF5E6","oldlace","253,245,230"},
 {"#FAF0E6","linen","250,240,230"},
 {"#FAEBD7","antiquewhite","250,235,215"},
 };
int NUMBER_OF_COLORNAMES=(sizeof(colors)/sizeof(colors[0]));

/*.
  The filling is done in the connex component whih contains (xs,ys)
  of the set of points which have the same color as (xs,ys)
  in the case where border=false and which are in a color different of border
  if not (that is when border is a color).
*/
void add_js_filltoborder(int canvas_type){
  fprintf(js_include_file,"\n/* command filltoborder */\
function filltoborder(xs,ys,border,color,fill_canvas_no,use_special_filling,fill_ctx){\
  let canvas = document.getElementById(\"wims_canvas%d%d\");\
  if (!canvas) return;\
  document.body.style.cursor = 'wait';\
  let ctx = canvas.getContext(\"2d\");\
  xs = parseInt(x2px(xs)); ys = parseInt(y2px(ys));\
  if (xs<0 || xs>=xsize || ys<0 || ys>=ysize)\
    {document.body.style.cursor = 'default'; return;};\
  let ps = xs+ys*xsize;\
  let image = ctx.getImageData(0, 0, xsize, ysize);\
  let iD = image.data;\
  function _getPixelColor(p) {return [iD[p],iD[p+1],iD[p+2],iD[p+3]];}\
  function _clearPixel(p) {for(let q=0; q<4; ++q) iD[p+q]=255;}\
  let _setPixel, _testColor;\
  if (use_special_filling) {\
    function _forcePixel(p,c) {for(let q=0; q<3; ++q) iD[p+q]=c[q]; iD[p+3]=255};\
    let fD = fill_ctx.getImageData(0, 0, xsize, ysize).data;\
    _setPixel = function(p) {_forcePixel(p,[fD[p],fD[p+1],fD[p+2],fD[p+3]])}\
  }\
  else {\
    function _forcePixel(p,c) {for(let q=0; q<3; ++q) iD[p+q]=c[q]; iD[p+3]=c[3]};\
    _setPixel = function(p) {_forcePixel(p,color);}\
  };\
  if(border){\
      _testColor = function(c)\
      {return Math.abs(c[0]-border[0])+Math.abs(c[1]-border[1])+Math.abs(c[2]-border[2])>3;}\
  }\
  else {\
      let cS = _getPixelColor(4*ps);\
      _testColor = function(c) {return c[0]==cS[0] && c[1]==cS[1] && c[2]==cS[2]}\
  }\
  let pad = new Array(xsize*ysize).fill(true);\
  pad[ps]=false;\
  let pStack = [ps];\
  let ct=0;\
  while (pStack.length) {\
    let p = pStack.shift();\
    if(!_testColor(_getPixelColor(4*p))) continue;\
    _setPixel(4*p);\
    ct++;\
    let x = p %% xsize, y = (p - x) / xsize;\
    if(x > 0 && pad[p-1]) {pad[p-1] = false; pStack.push(p-1)};\
    if(x < xsize-1 && pad[p+1]) {pad[p+1] = false; pStack.push(p+1)};\
    if(y > 0 && pad[p-xsize]) {pad[p-xsize] = false; pStack.push(p-xsize)};\
    if(y < ysize-1 && pad[p+xsize]) {pad[p+xsize] = false; pStack.push(p+xsize);};\
  };\
  var fill_canvas = create_canvas%d(fill_canvas_no,xsize,ysize);\
  var fill_canvas_ctx = fill_canvas.getContext(\"2d\");\
  fill_canvas_ctx.clearRect(0,0,xsize,ysize);\
  fill_canvas_ctx.putImageData(image, 0, 0);\
  document.body.style.cursor = 'default';\
};",canvas_root_id,canvas_type,canvas_root_id);
}

void add_js_ruler(double x,double y,double sizex,double sizey,char *font,
char *stroke_color,double stroke_opacity,char *fill_color,double fill_opacity,
int line_width,int dynamic,int use_snap){
 fprintf(js_include_file,"\n/* command ruler */\
 var ruler_data = new Array(3);\
 var ruler%d = function(){\
  var use_snap = %d;\
  var full = 2*Math.PI;\
  var once = true;\
  var canvas = create_canvas%d(3000,xsize,ysize);\
  var ctx = canvas.getContext(\"2d\");\
  var canvas_temp =  document.createElement(\"canvas\");\
  var size_x = xsize*(%f)/(xmax - xmin);\
  var size_y = ysize*(%f)/(ymax - ymin);\
  var dx = xsize/(xmax - xmin);\
  var dy = 0.8*ysize/(ymax - ymin);\
  canvas_temp.width = xsize;\
  canvas_temp.height = ysize;\
  var ctx_temp = canvas_temp.getContext(\"2d\");\
  var xcenter = x2px(%f);\
  var ycenter = y2px(%f);\
  var ruler_x = xcenter;\
  var ruler_y = ycenter;\
  ctx_temp.font = \"%s\";\
  ctx_temp.strokeStyle = \"rgba(%s,%f)\";\
  ctx_temp.fillStyle = \"rgba(%s,%f)\";\
  ctx_temp.lineWidth = %d;\
  ctx_temp.save();\
  if(once){\
   ctx_temp.beginPath();\
   ctx_temp.moveTo(ruler_x,ruler_y);\
   ctx_temp.lineTo(ruler_x+size_x,ruler_y);\
   ctx_temp.lineTo(ruler_x+size_x,ruler_y-size_y);\
   ctx_temp.lineTo(ruler_x,ruler_y-size_y);\
   ctx_temp.lineTo(ruler_x,ruler_y);\
   ctx_temp.closePath();\
   ctx_temp.fill();\
   ctx_temp.stroke();\
   ctx_temp.fillStyle = ctx_temp.strokeStyle;\
   var txtsize;\
   var num = 1;\
   for(var p = dx ; p < size_x ; p = p+dx){\
     txtsize = 0.5*(ctx_temp.measureText(num).width);\
     ctx_temp.fillText(num,ruler_x + p -txtsize,ruler_y - 0.9*dy);\
     num++;\
   };\
   ctx_temp.strokeStyle = \"rgba(0,0,255,0.6)\";\
   ctx_temp.lineWidth = 2;\
   for(var p = 0; p < size_x ; p = p+dx){\
    ctx_temp.beginPath();\
    ctx_temp.moveTo(ruler_x+p,ruler_y);\
    ctx_temp.lineTo(ruler_x+p,ruler_y-0.8*dy);\
    ctx_temp.closePath();\
    ctx_temp.stroke();\
   };\
   ctx_temp.strokeStyle = \"rgba(0,0,255,0.6)\";\
   ctx_temp.lineWidth = 1;\
   for(var p = 0; p < size_x ; p = p+0.5*dx){\
    ctx_temp.beginPath();\
    ctx_temp.moveTo(ruler_x+p,ruler_y);\
    ctx_temp.lineTo(ruler_x+p,ruler_y-0.6*dy);\
    ctx_temp.closePath();\
    ctx_temp.stroke();\
   };\
   ctx_temp.strokeStyle = \"rgba(255,0,0,0.6)\";\
   ctx_temp.lineWidth = 0.5;\
   for(var p = 0; p < size_x ; p = p+0.1*dx){\
    ctx_temp.beginPath();\
    ctx_temp.moveTo(ruler_x+p,ruler_y);\
    ctx_temp.lineTo(ruler_x+p,ruler_y-0.4*dy);\
    ctx_temp.closePath();\
    ctx_temp.stroke();\
   };\
   ctx_temp.drawImage(canvas,ruler_x,ruler_y);\
   once = false;\
  }",canvas_root_id,use_snap,canvas_root_id,sizex,sizey,x,y,font,stroke_color,stroke_opacity,fill_color,fill_opacity,line_width);

 if( dynamic == -1 ){
 fprintf(js_include_file,"\
 ctx.drawImage(canvas_temp,0,0);\
   if(wims_status != \"done\"){\
    canvas_div.addEventListener( 'mouseup'   , ruler_stop,false);\
    canvas_div.addEventListener( 'mousedown' , ruler_start,false);\
    canvas_div.addEventListener( 'mousemove' , ruler_move,false);\
    canvas_div.addEventListener( 'touchstart', function(e){ e.preventDefault();ruler_start(e.changedTouches[0]);},false);\
    canvas_div.addEventListener( 'touchmove', function(e){ e.preventDefault();ruler_move(e.changedTouches[0]);},false);\
    canvas_div.addEventListener( 'touchend', function(e){ e.preventDefault();ruler_stop(e.changedTouches[0]);},false);\
   };\
   function ruler_stop(evt){\
    ruler_data[0] = ruler_x;\
    ruler_data[1] = ruler_y;\
    ruler_data[2] = angle;\
    return;\
   };\
   var ruler_object_cnt = 0;\
   function ruler_start(evt){\
    var mouse = getMouse(evt,canvas);\
    var mouse_y = mouse.y;\
    if( mouse_y > ysize - 20 ){return;};\
    var mouse_x = mouse.x;\
    if( mouse_x > ruler_x - 50 && mouse_x < ruler_x + size_x + 50){\
     if( mouse_y > ruler_y - 50 && mouse_y < ruler_y + size_y + 50){\
      ruler_object_cnt++;\
      ruler_move(evt);\
      return;\
     };\
    }else{ruler_object_cnt = 0; return;};\
   };\
   var angle = 0;\
   function ruler_move(evt){\
    var mouse = getMouse(evt,canvas);\
    switch(ruler_object_cnt){\
     case 1:\
      angle = 0;\
      var xy = multisnap_check(mouse.x,mouse.y,use_snap);\
      ruler_y = xy[1];\
      if( ruler_y > ysize - 20 ){ruler_y = 0.5*ysize;ruler_x = 0.5*xsize;return;};\
      ruler_x = xy[0];\
      ctx.clearRect(0,0,xsize,ysize);\
      ctx.save();\
      ctx.translate(ruler_x - xcenter,ruler_y - ycenter);\
      ctx.drawImage(canvas_temp,0,0);\
      ctx.restore();\
      break;\
     case 2:\
      angle = find_angle(ruler_x,ruler_y,mouse.x,mouse.y);\
      ctx.clearRect(0,0,xsize,ysize);\
      ctx.save();\
      ctx.translate(ruler_x,ruler_y);\
      ctx.rotate(angle);\
      ctx.translate( -1*xcenter, -1*ycenter );\
      ctx.drawImage( canvas_temp,0,0 );\
      ctx.restore();\
      userdraw_radius[0] = 2*Math.PI - angle;\
      break;\
     case 3:ruler_object_cnt = 0;break;\
     default:ruler_stop(evt);break;\
    };\
   };\
  };\
  ruler%d();",canvas_root_id);
 }
 else
 {
  fprintf(js_include_file,"\
   ctx.clearRect(0,0,xsize,ysize);\
   ctx.save();\
   ctx.translate(ruler_x,ruler_y);\
   ctx.rotate(%d*Math.PI/180);\
   ctx.translate( -1*xcenter, -1*ycenter );\
   ctx.drawImage( canvas_temp,0,0 );\
   ctx.restore();\
  };\
  ruler%d();",dynamic,canvas_root_id);
 }
}

void add_js_protractor(int type,double xcenter,double ycenter,int size,char *font,char *stroke_color,double stroke_opacity,char *fill_color,double fill_opacity,int line_width,int use_scale,int dynamic,int use_snap){
/*
use_slider_display = 2 : angle in degrees
use_slider_display = 3 : angle in radians
void add_slider_display(FILE *js_include_file,int canvas_root_id,int precision,int font_size,char *font_color,double stroke_opacity){
*/

  if( type == 1 ){ /* geodriehoek */
 fprintf(js_include_file,"\n/* command protractor type 1 */\
 var protractor_data = new Array(3);\
 var protractor%d = function(){\
  var once = true;\
  var use_snap = %d;\
  var full = 2*Math.PI;\
  var canvas = create_canvas%d(2000,xsize,ysize);\
  var ctx = canvas.getContext(\"2d\");\
  var canvas_temp =  document.createElement(\"canvas\");\
  var size = parseInt(xsize*(%d)/(xmax - xmin));\
  canvas_temp.width = xsize;\
  canvas_temp.height = ysize;\
  var ctx_temp = canvas_temp.getContext(\"2d\");\
  var type = %d;\
  var xcenter = x2px(%f);\
  var ycenter = y2px(%f);\
  var half = 0.5*size;\
  var radius1 = 0.6*half;\
  var radius2 = 0.65*half;\
  var radius3 = 0.7*half;\
  ctx_temp.font = \"%s\";\
  ctx_temp.strokeStyle = \"rgba(%s,%f)\";\
  ctx_temp.fillStyle = \"rgba(%s,%f)\";\
  ctx_temp.lineWidth =%d;\
  var use_scale = %d;\
  if( once ){\
   ctx_temp.clearRect(0,0,canvas_temp.width,canvas_temp.height);\
   ctx_temp.beginPath();\
   ctx_temp.moveTo(xcenter-half,ycenter );\
   ctx_temp.lineTo(xcenter,ycenter-half);\
   ctx_temp.lineTo(xcenter+half,ycenter);\
   ctx_temp.lineTo(xcenter-half,ycenter);\
   ctx_temp.moveTo(xcenter,ycenter );\
   ctx_temp.lineTo(xcenter+0.5*half,ycenter-0.5*half);\
   ctx_temp.moveTo(xcenter,ycenter );\
   ctx_temp.lineTo(xcenter-0.5*half,ycenter-0.5*half);\
   ctx_temp.moveTo(xcenter,ycenter );\
   ctx_temp.lineTo(xcenter,ycenter-half);\
   ctx_temp.closePath();\
   ctx_temp.fill();\
   ctx_temp.stroke();\
   ctx_temp.beginPath();\
   ctx_temp.arc(xcenter,ycenter,radius1,0,Math.PI,false);\
   ctx_temp.closePath();\
   if( use_scale == 1 ){\
    ctx_temp.fillStyle = ctx_temp.strokeStyle;\
    var txtsize;\
    for(var p = 45 ; p < 180;p = p+45){\
     txtsize = 0.5*(ctx_temp.measureText(p).width);\
     ctx_temp.fillText(p,xcenter+0.5*half*Math.cos(p*Math.PI/180) - txtsize,ycenter-0.5*half*Math.sin(p*Math.PI/180));\
    };\
   };\
   for(var p = 10 ; p < 180;p = p+10){\
    ctx_temp.beginPath();\
    ctx_temp.moveTo(xcenter+radius1*Math.cos(p*Math.PI/180),ycenter-radius1*Math.sin(p*Math.PI/180));\
    ctx_temp.lineTo(xcenter+radius3*Math.cos(p*Math.PI/180),ycenter-radius3*Math.sin(p*Math.PI/180));\
    ctx_temp.closePath();\
    ctx_temp.stroke();\
   };\
   for(var p = 0 ; p < 180;p=p+2){\
    if(p%%10 != 0){\
     ctx_temp.beginPath();\
     ctx_temp.moveTo(xcenter+radius1*Math.cos(p*Math.PI/180),ycenter-radius1*Math.sin(p*Math.PI/180));\
     ctx_temp.lineTo(xcenter+radius2*Math.cos(p*Math.PI/180),ycenter-radius2*Math.sin(p*Math.PI/180));\
     ctx_temp.closePath();\
     ctx_temp.stroke();\
    };\
   };\
   ctx_temp.drawImage(canvas,xcenter,ycenter);\
   ctx_temp.save();\
   once = false;\
  };\
  ",canvas_root_id,use_snap,canvas_root_id,size,type,xcenter,ycenter,font,stroke_color,stroke_opacity,fill_color,fill_opacity,line_width,use_scale);
  }
  if( type != 1 ){
 fprintf(js_include_file,"\n/* command protractor type 0 */\
 var protractor_data = new Array(3);\
 var protractor%d = function(){\
  var once = true;\
  var use_snap = %d;\
  var full = 2*Math.PI;\
  var canvas = create_canvas%d(2000,xsize,ysize);\
  var ctx = canvas.getContext(\"2d\");\
  var canvas_temp =  document.createElement(\"canvas\");\
  var size = parseInt(xsize*(%d)/(xmax - xmin));\
  canvas_temp.width = xsize;\
  canvas_temp.height = ysize;\
  var ctx_temp = canvas_temp.getContext(\"2d\");\
  var type = %d;\
  var xcenter = x2px(%f);\
  var ycenter = y2px(%f);\
  var half = 0.5*size;\
  var radius1 = 0.8*half;\
  var radius2 = 0.9*half;\
  var radius3 = half;\
  ctx_temp.font = \"%s\";\
  ctx_temp.strokeStyle = \"rgba(%s,%f)\";\
  ctx_temp.fillStyle = \"rgba(%s,%f)\";\
  ctx_temp.lineWidth =%d;\
  var use_scale = %d;\
  if( once ){\
   ctx_temp.clearRect(0,0,xsize,ysize);\
   ctx_temp.arc(xcenter,ycenter,radius1,0,2*Math.PI,false);\
   ctx_temp.arc(xcenter,ycenter,radius2,0,2*Math.PI,false);\
   ctx_temp.arc(xcenter,ycenter,radius3,0,2*Math.PI,false);\
   ctx_temp.fill();\
   ctx_temp.stroke();\
   if( use_scale == 1 ){\
    ctx_temp.fillStyle = ctx_temp.strokeStyle;\
    var txtsize;\
    for(var p = 0 ; p < 360;p = p+45){\
     txtsize = 0.5*(ctx_temp.measureText(p).width);\
     ctx_temp.fillText(p,xcenter+0.6*half*Math.cos(p*Math.PI/180) - txtsize,ycenter-0.6*half*Math.sin(p*Math.PI/180));\
    };\
   };\
   ctx_temp.strokeStyle = \"rgba(255,0,0,0.4)\";\
   for(var p = 0 ; p < 360;p = p+10){\
    ctx_temp.beginPath();\
    ctx_temp.moveTo(xcenter+radius1*Math.cos(p*Math.PI/180),ycenter-radius1*Math.sin(p*Math.PI/180));\
    ctx_temp.lineTo(xcenter+radius3*Math.cos(p*Math.PI/180),ycenter-radius3*Math.sin(p*Math.PI/180));\
    ctx_temp.closePath();\
    ctx_temp.stroke();\
   };\
   ctx_temp.strokeStyle = \"rgba(0,0,255,0.4)\";\
   for(var p = 0 ; p < 360;p=p+2){\
     ctx_temp.beginPath();\
     ctx_temp.moveTo(xcenter+radius2*Math.cos(p*Math.PI/180),ycenter-radius2*Math.sin(p*Math.PI/180));\
     ctx_temp.lineTo(xcenter+radius3*Math.cos(p*Math.PI/180),ycenter-radius3*Math.sin(p*Math.PI/180));\
     ctx_temp.closePath();\
     ctx_temp.stroke();\
   };\
   ctx_temp.strokeStyle = \"rgba(0,0,0,0.6)\";\
   for(var p = 0 ; p < 360;p=p+45){\
     ctx_temp.beginPath();\
     ctx_temp.moveTo(xcenter,ycenter);\
     ctx_temp.lineTo(xcenter+radius3*Math.cos(p*Math.PI/180),ycenter-radius3*Math.sin(p*Math.PI/180));\
     ctx_temp.closePath();\
     ctx_temp.stroke();\
   };\
   ctx_temp.drawImage(canvas,0,0);\
   ctx_temp.save();\
   once = false;\
  };",canvas_root_id,use_snap,canvas_root_id,size,type,xcenter,ycenter,font,stroke_color,stroke_opacity,fill_color,fill_opacity,line_width,use_scale);
  }
  if( dynamic == -1 ){ /* rotate the protractors */
    fprintf(js_include_file,"\
  var protractor_x = xcenter;\
  var protractor_y = ycenter;\
  ctx.drawImage(canvas_temp,0,0);\
  var angle = 0;\
  if(wims_status != \"done\"){\
   canvas_div.addEventListener( 'mouseup'   , protractor_stop,false);\
   canvas_div.addEventListener( 'mousedown' , protractor_start,false);\
   canvas_div.addEventListener( 'mousemove' , protractor_move,false);\
   canvas_div.addEventListener( 'touchstart', function(e){ e.preventDefault();protractor_start(e.changedTouches[0]);},false);\
   canvas_div.addEventListener( 'touchmove', function(e){ e.preventDefault();protractor_move(e.changedTouches[0]);},false);\
   canvas_div.addEventListener( 'touchend', function(e){ e.preventDefault();protractor_stop(e.changedTouches[0]);},false);\
  };\
  function protractor_stop(evt){\
   protractor_data[0] = protractor_x;\
   protractor_data[1] = protractor_y;\
   protractor_data[2] = angle;\
   return;\
  };\
  var protractor_object_cnt = 0;\
  function protractor_start(evt){\
   var mouse = getMouse(evt,canvas);\
   var mouse_y = mouse.y;\
   if( mouse_y > ysize - 20 ){return;};\
   var mouse_x = mouse.x;\
   if( mouse_x > protractor_x - half && mouse_x < protractor_x + half ){\
    if( mouse_y > protractor_y - half && mouse_y < protractor_y + half ){\
     protractor_object_cnt++;\
     protractor_move(evt);\
     return;\
    };\
   }else{protractor_object_cnt = 0; return;};\
  };\
  function protractor_move(evt){\
   var mouse = getMouse(evt,canvas);\
   switch(protractor_object_cnt){\
    case 1:\
      var xy = multisnap_check(mouse.x,mouse.y,use_snap);\
      mouse.x = xy[0];\
      mouse.y = xy[1];\
      angle = 0;\
      protractor_x = mouse.x;\
      protractor_y = mouse.y;\
      if( protractor_y > ysize - 20 ){protractor_y = 0.5*ysize;protractor_x = 0.5*xsize;return;};\
      ctx.clearRect(0,0,xsize,ysize);\
      ctx.save();\
      ctx.translate(protractor_x - xcenter,protractor_y - ycenter);\
      ctx.drawImage(canvas_temp,0,0);\
      ctx.restore();\
      break;\
    case 2:\
     angle = find_angle(protractor_x,protractor_y,mouse.x,mouse.y);\
     ctx.clearRect(0,0,xsize,ysize);\
     ctx.save();\
     ctx.translate(protractor_x,protractor_y);\
     ctx.rotate(angle);\
     ctx.translate( -1*xcenter, -1*ycenter );\
     ctx.drawImage( canvas_temp,0,0 );\
     ctx.restore();\
     userdraw_radius[0] =2*Math.PI- angle;\
     break;\
    case 3:protractor_object_cnt = 0;break;\
    default:protractor_stop(evt);\
   };\
  };\
 };\
 protractor%d();",canvas_root_id);
  }
  else
  {
 fprintf(js_include_file,"\
  ctx.save();\
  ctx.translate(xcenter,ycenter);\
  ctx.rotate(%d*Math.PI/180);\
  ctx.translate( -1*xcenter, -1*ycenter );\
  ctx.drawImage( canvas_temp,0,0 );\
  ctx.restore();\
 };\
 protractor%d();\
",dynamic,canvas_root_id);
  } /* end dynamic == -1*/
}
