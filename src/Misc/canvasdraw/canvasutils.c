#include "canvasdraw.h"
/*
collection of javascript utilities (with no configurable parameters)
*/
void add_javascript_function(){
#ifdef KATEX_INSTALLED
char *mathjax="centered(xml_div,obj);";
#else
char *mathjax="setTimeout(function(){centered(xml_div,obj)},100);";
#endif
int i;
int type;
for(i = 0 ; i < MAX_JS_FUNCTIONS; i++){
/*
fprintf(stdout,"checking js_function[%d] = %d<br>",i,js_function[i]);
*/
  if( js_function[i] == 1){
    switch(i){
      case JS_ARROWHEAD:
/* draw an arrowhead on object ; arrowhead on (x:y) from direction (x0:y0) */
        fprintf(js_include_file,"\n/* draw an arrowhead on object ; arrowhead on (x:y) from direction (x0:y0) */\
function draw_arrowhead(x,y,x0,y0,ctx){\
var angle = Math.atan2(x - x0,y - y0) + Math.PI;\
var pie = Math.PI/6;\
ctx.save();\
ctx.fillStyle=ctx.strokeStyle;\
ctx.beginPath();\
ctx.moveTo(x,y);\
ctx.moveTo(x - (arrow_head * Math.sin(angle - pie)),y - (arrow_head * Math.cos(angle - pie)));\
ctx.lineTo(x,y);\
ctx.lineTo(x - (arrow_head * Math.sin(angle + pie)),y - (arrow_head * Math.cos(angle + pie)));\
ctx.closePath();\
ctx.stroke();\
ctx.fill();\
ctx.restore();\
};");
        break;
      case JS_LOAD_IMAGE:
        fprintf(js_include_file,"\n/* get_image_from_url */\
var image_cnt = 0;\
if( typeof(image_patterns) !== 'object'){var image_patterns = [];};\
var get_image_from_url = function(url){\
 var idx = Math.ceil(1000*(Math.random()));\
 var canvas = create_canvas%d(idx,xsize,ysize);\
 canvas.style.visibility = 'hidden';\
 var ctx = canvas.getContext(\"2d\");\
 var img = new Image();\
 img.src = url;\
 img.onload = function(){\
  var pat = ctx.createPattern(img,\"repeat\");\
  image_patterns.push(pat);\
 };\
};",canvas_root_id);
        break;
      case JS_SAFE_EVAL:
/* to avoid easy js-code injection...but is it a real problem ? */
        fprintf(js_include_file," \n/* add safe eval function */function safe_eval(exp){\
 exp = exp.replace(/pi/g,'3.14159');\
 if(exp.indexOf('^') != -1){\
  exp = exp.replace(/[a-zA-Z]/g,' ');\
  exp = exp.replace(/\\*10\\^-/g,'e-');\
  exp = exp.replace(/\\*10\\^/g,'e+');\
  exp = exp.replace(/10\\^-/g,'1e-');exp = exp.replace(/10\\^/g,'1e+');\
  exp = eval(exp);\
  if(isNaN(exp)){alert(\"invalid input\\ntry just a real number \\ne.g. no calculations...\");return null;}\
  return exp;\
 };\
 var reg = /(?:[a-z$_][a-z0-9$_]*)|(?:[;={}\\[\\]\"'!&<>^\\\\?:])/ig;\
 var valid = true;\
 exp = exp.replace(reg,function($0){\
  if(Math.hasOwnProperty($0)){\
   return \"Math.\"+$0;\
  } else {\
   valid = false;\
  }\
 }\
 );\
 if( !valid ){\
  alert(\"hmmm \"+exp+\" ?\"); exp = null;\
 } else {\
  try{ exp = eval(exp); } catch(e){alert(\"Invalid arithmetic expression\"); exp = null;};\
 };\
 return exp;\
};");
        break;
      case JS_RAWMATH:
        fprintf(js_include_file,"\n/* add_rawmath() */\
function rawmath(i){\
 i=i.toLowerCase();\
 i=i.replace(/\\ /g,\"\");i=i.replace(/\\*\\*/g,\"^\");\
 if(i.indexOf(\"e+\")!=-1){i=i.replace(\"e+\",\"*10^\");};\
 if(i.indexOf(\"e-\")!=-1){i=i.replace(\"e-\",\"*10^-\");};\
 i=i.replace(/\\*\\*/g,\"*\");\
 if(i.charAt(0)==\"*\"){i=i.substring(1,i.length);};\
 var fun=[\"asin\",\"acos\",\"atan\",\"sin\",\"cos\",\"tan\",\"log\",\"ln\",\"pi\",\"e\",\"x\",\"y\"];\
 var cons=[\"pi\",\"e\",\"0\",\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\"];\
 var cntl = 0;var cntr = 0;\
 var len = i.length;\
 for( var p = 0;p < len; p++){\
  if(i.charAt(p) == '('){cntl++;}\
  if(i.charAt(p) == ')'){cntr++;}\
 };\
 if(cntl != cntr){alert(\"unmatched parenthesis !!\");return null;};\
 for(var p = 0; p < 12 ; p++){\
  for(var d = 0; d < 12 ; d++){\
   while(i.indexOf(cons[d]+\"\"+fun[p])!=-1){\
    i = i.replace(cons[d]+\"\"+fun[p],cons[d]+\"*\"+fun[p]);\
   };\
   while(i.indexOf(fun[p]+\"\"+cons[d])!=-1){\
    i = i.replace(fun[p]+\"\"+cons[d],fun[p]+\"*\"+cons[d]);\
   };\
  };\
 };\
 if(i.indexOf(\"(\")!=-1){\
  for(var p = 0;p < 12; p++){\
   if(i.indexOf(cons[p]+\"(\")!=-1){\
    i = i.replace(cons[p]+\"(\",cons[p]+\"*(\");\
   };\
   if(i.indexOf(\")\"+cons[p])!=-1){\
    i = i.replace(\")\"+cons[p],\")*\"+cons[p]);\
   };\
  };\
  i = i.replace(/\\)\\(/g,\")*(\");\
 };\
 return i;\
};");
        break;
      case JS_PLOT:
        fprintf(js_include_file,"\n/* jsplot() */\
var x_anim_points;var y_anim_points;var animation_steps;var animation_funs;\
function jsplot(canvas_type,funs,linewidth,color,opacity,use_dashed,dashtype0,dashtype1,trange,plotsteps,use_parametric,use_animate,origin){\
 var obj = create_canvas%d(canvas_type,xsize,ysize);\
 var ctx = obj.getContext(\"2d\");\
 ctx.clearRect(0,0,xsize,ysize);\
 animation_funs = funs.length;\
 function eval_jsmath(x,func){return parseFloat(eval(func));};\
 if(origin == 0 ){\
  if( typeof(multilinewidth) !== 'undefined' && multilinewidth != null ){ linewidth = multilinewidth;};\
  if( typeof(multistrokecolors) !== 'undefined' && multistrokecolors != null ){ color = multistrokecolors;};\
  if( typeof(multistrokeopacity) !== 'undefined' && multistrokeopacity != null ){ opacity = multistrokeopacity;};\
  if( typeof(multidash) !== 'undefined' && multidash != null ){use_dashed = multidash;};\
 };\
 x_anim_points = [];y_anim_points = [];var idx=0;\
 if( use_parametric == 1){\
  for(var i = 0 ; i < animation_funs; i=i+2){\
   funs[i] = funs[i].replace(/t/g,'x');\
   funs[i+1] = funs[i+1].replace(/t/g,'x');\
   funs[i] = funs[i].replace(/xan/g,'tan');\
   funs[i+1] = funs[i+1].replace(/xan/g,'tan');\
   var fun_x = to_js_math(funs[i]);\
   var fun_y = to_js_math(funs[i+1]);\
   if(fun_x == null || fun_y == null){alert(\"Syntax Error...\\nAttention : try use very precise notation !\\nlike :\\n6*(0.25)^(1.23)\\n1/(sin(5*x))\\n(3*x+4)/(x^(2*pi)) \");return;};\
   try{ parseFloat( eval_jsmath( px2x(0),fun_x ) );}catch(e){alert(\"\\nSyntax Error...\\nAttention : try use very precise notation !\\nlike :\\n6*(0.25)^(1.23)\\n1/(sin(5*x))\\n(3*x+4)/(x^(2*pi))\");return;};\
   ctx.lineWidth = linewidth[i] || linewidth;\
   if(opacity[i]>1){opacity[i]=0.00392*opacity[i];}\
   ctx.strokeStyle = \"rgba(\"+color[i]+\",\"+opacity[i]+\")\";\
   if(use_dashed[i] == \"1\" || use_dashed == \"1\"){if(ctx.setLineDash){ctx.setLineDash([dashtype0,dashtype1]);}else{ctx.mozDash =[dashtype0,dashtype1];}};\
   var y1;var x1;var y2;var x2;\
   ctx.beginPath();\
   var tmin = trange[0];var tmax = trange[1];\
   var step = parseFloat((tmax - tmin)/plotsteps);\
   for(var p = tmin ; p < tmax; p = p+step ){\
    x1 = x2px(parseFloat(eval_jsmath(p,fun_x)));\
    y1 = y2px(parseFloat(eval_jsmath(p,fun_y)));\
    x2 = x2px(parseFloat(eval_jsmath(p+step,fun_x)));\
    y2 = y2px(parseFloat(eval_jsmath(p+step,fun_y)));\
    x_anim_points[idx] = x1;y_anim_points[idx] = y1;idx++;\
     ctx.moveTo(x1,y1);\
     ctx.lineTo(x2,y2);\
     ctx.moveTo(x1,y1);\
     ctx.lineTo(x2,y2);\
   };\
   ctx.closePath();\
   ctx.stroke();\
  };\
  animation_funs = 0.5*animation_funs;\
 }else{\
  for(var i = 0 ; i < animation_funs; i++){\
   var fun = to_js_math(funs[i]);\
   if(fun == null){alert(\"Syntax Error...\\nAttention : try use very precise notation !\\nlike :\\n6*(0.25)^(1.23)\\n1/(sin(5*x))\\n(3*x+4)/(x^(2*pi)) \");return;};\
   try{ parseFloat( eval_jsmath( px2x(0),fun ) );}catch(e){alert(\"\\nSyntax Error...\\nAttention : try use very precise notation !\\nlike :\\n6*(0.25)^(1.23)\\n1/(sin(5*x))\\n(3*x+4)/(x^(2*pi))\");return;};\
   if(use_dashed[i] == \"1\" || use_dashed == \"1\"){if(ctx.setLineDash){ctx.setLineDash([dashtype0,dashtype1]);}else{ctx.mozDash =[dashtype0,dashtype1];}};\
   ctx.lineWidth = linewidth[i] || linewidth;\
   if(opacity[i]>1){opacity[i]=0.00392*opacity[i];}\
   ctx.strokeStyle='rgba('+color[i] +','+opacity[i] +')';\
   var y1;var x1;var y2;var x2;\
   ctx.beginPath();\
   for(var p = 0 ; p<xsize;p++){\
    x1 = px2x(p);\
    y1 = y2px(parseFloat(eval_jsmath(x1,fun)));\
    x2 = px2x(p+1);\
    y2 = y2px(parseFloat(eval_jsmath(x2,fun)));\
    x_anim_points[idx] = p;y_anim_points[idx] = y1;idx++;\
    if(Math.abs(y2-y1) < ysize ){\
     ctx.moveTo(p,y1);\
     ctx.lineTo(p+1,y2);\
    };\
   };\
   ctx.closePath();\
   ctx.stroke();\
  };\
 };\
 if( use_animate == 1 ){animation_steps = idx;animate_this();}\
};",canvas_root_id);
        break;
      case JS_MATH:
/* 5/2024 added exp -> e^ -> (2.718281828459045)^ */
        fprintf(js_include_file,"\n/* to_js_math() */\
function to_js_math(math_fun){\
 if(math_fun == null){return;};\
 var infun=[\"exp\",\"sqrt\",\"^\",\"asin\",\"acos\",\"atan\",\"log\",\"pi\",\"abs\",\"sin\",\"cos\",\"tan\",\"e\"];\
 var outfun=[\"(2.718281828459045)^\",\"Math.sqrt\",\"Math.pow\",\"Math.asin\",\"Math.acos\",\"Math.atan\",\"Math.log\",\"(3.14159265358979)\",\"Math.abs\",\"Math.sin\",\"Math.cos\",\"Math.tan\",\"(2.718281828459045)\"];\
 var len = infun.length;var in_fun;var In_Fun;var out_fun;var w_cnt;\
 for(var p=0 ; p < len ; p++){\
  in_fun = infun[p];   In_Fun = in_fun.toUpperCase();out_fun = outfun[p];w_cnt=0;\
  if(math_fun.indexOf(in_fun) != -1){\
   if(in_fun == \"^\"){\
    var tab = [];var small_trick = \"___small_trick___\";\
    while (math_fun.indexOf(\"(\") != -1){\
     math_fun = math_fun.replace(/(\\([^\\(\\)]*\\))/g, function(m, t){tab.push(t);return (small_trick + (tab.length - 1));});\
     w_cnt++;if(w_cnt>1000){alert(\"hmmm \"+math_fun+\" ?\\nUse command plot for more complex math functions...\");return null;};\
    };\
    tab.push(math_fun);w_cnt = 0;math_fun = small_trick + (tab.length - 1);\
    while (math_fun.indexOf(small_trick) != -1){\
     math_fun = math_fun.replace(new RegExp(small_trick + \"(\\\\d+)\", \"g\"), function(m, d){return tab[d].replace(/(\\w*)\\^(\\w*)/g, out_fun+\"($1,$2)\");});\
     w_cnt++;if(w_cnt>1000){alert(\"hmmm \"+math_fun+\" ?\\nUse command plot for more complex math functions...\");return null;};\
    };\
   } else {\
    while( math_fun.indexOf(in_fun) != -1 ){\
     math_fun = math_fun.replace(in_fun,out_fun);\
     math_fun = math_fun.replace(in_fun,In_Fun);\
     w_cnt++;if(w_cnt>1000){alert(\"hmmm \"+math_fun+\" ?\\nUse command plot for more complex math functions...\");return null;};\
    };\
   };\
  };\
 };\
 for(var p=0 ; p < len ; p++){\
  in_fun = infun[p];In_Fun = in_fun.toUpperCase();w_cnt = 0;\
  if(math_fun.indexOf(In_Fun) != -1 ){\
   while(math_fun.indexOf(In_Fun) != -1){\
    math_fun = math_fun.replace(In_Fun,in_fun);\
    w_cnt++;if(w_cnt>1000){alert(\"hmmm \"+math_fun+\" ?\\nUse command plot for more complex math functions...\");return null;};\
   };\
  };\
 };\
 return math_fun;\
};");
        break;
      case INTERACTIVE:
/* 1/8/2022 NOTE : getMouse() code should run at maximum speed and not be bothered with all kind of fancy tests !!
  1/8/2022 : latest improvements related to 'reponsiveness' will fatally lockup browsers in the pixel-by-pixel filling...at least in userdraw clickfill...*/
        fprintf(js_include_file,"\n/* add mouse support */\
var isTouch = (('ontouchstart' in window) || (navigator.msMaxTouchPoints > 0));\
var getMouse = function(e, element){\
   var mx,my;var offsetX = 0,offsetY = 0;\
   while( ( element = element.offsetParent) ){\
     offsetX += element.offsetLeft;offsetY += element.offsetTop;\
   };\
   /*console.log('===');*/\
   if(isTouch){\
      mx = e.pageX - offsetX; my = e.pageY - offsetY;\
   }else{\
      mx = e.clientX - offsetX + (document.documentElement.scrollLeft ? document.documentElement.scrollLeft:document.body.scrollLeft);\
      my = e.clientY - offsetY + (document.documentElement.scrollTop ? document.documentElement.scrollTop:document.body.scrollTop);\
   };\
   if( typeof(clickfill) === 'function'){ return {x: mx, y: my};}else{\
   /* test if canvas_div is inside a scrollable */\
   const scroll_parent = canvas_div.closest('.wims_scrollable');\
   if (scroll_parent){\
      mx = mx + scroll_parent.scrollLeft;\
      my = my + scroll_parent.scrollTop;\
   }\
   /* If canvas had been resized,, we have to convert these coordinates to original grid*/\
   /*console.log('mx before=' + mx);\
   console.log('xsize='+xsize);\
   console.log('canvas_div.offsetHeight='+canvas_div.offsetWidth);*/\
   mx = (mx * xsize / canvas_div.offsetWidth);\
   /*console.log('mx after='+mx);*/\
   my = (my * ysize / canvas_div.offsetHeight);\
   return {x: mx, y: my};};\
};");
        break;
      case USERDRAW_AND_ZOOM:
        fprintf(js_include_file,"\n/* add zoom support for multidraw */\
function recalculate_userdraw(xmin0,ymin0,xmax0,ymax0){\
 function x_zoom(px){\
  var x = parseFloat(px*(xmax0 - xmin0)/xsize + xmin0);\
  return parseFloat(x*xsize/(xmax - xmin) - xsize*xmin/(xmax - xmin));\
 };\
 function y_zoom(py){\
  var y = parseFloat(ymax0 - py*(ymax0 - ymin0)/ysize);\
  return parseFloat(-1*y*ysize/(ymax - ymin) + ymax*ysize/(ymax - ymin));\
 };\
 function r_zoom(r){\
  return r*(xmin0/xmin);\
 };\
 var len = userdraw_x.length;\
 var x;var y;var r;\
 for(var p=0;p<len;p++){\
  x = userdraw_x[p];\
  y = userdraw_y[p];\
  userdraw_x[p] = x_zoom(x);\
  userdraw_y[p] = y_zoom(y);\
  if( userdraw_radius[p] ){\
   r = userdraw_radius[p];\
   userdraw_radius[p] = r_zoom(r);\
  };\
 };\
};");
        break;

      case DRAG_AND_ZOOM:
        fprintf(js_include_file,"\n/* add zoom support for drag&drop */\
function recalculate_dragdrop(xmin0,ymin0,xmax0,ymax0){\
 function x_zoom(px){\
  var x = parseFloat(px*(xmax0 - xmin0)/xsize + xmin0);\
  return parseFloat(x*xsize/(xmax - xmin) - xsize*xmin/(xmax - xmin));\
 };\
 function y_zoom(py){\
  var y = parseFloat(ymax0 - py*(ymax0 - ymin0)/ysize);\
  return parseFloat(-1*y*ysize/(ymax - ymin) + ymax*ysize/(ymax - ymin));\
 };\
 var len = dragstuff.shapes.length;\
 var shape;var L;var x;var y;\
 for(var p=0;p<len;p++){\
  shape = dragstuff.shapes[p];\
  L = shape.x.length;\
  for(var i=0;i<L;i++){\
   x = shape.x[i];\
   y = shape.y[i];\
   dragstuff.shapes[p].x[i] = x_zoom(x);\
   dragstuff.shapes[p].y[i] = y_zoom(y);\
  };\
 };\
};");
        break;
      case JSPLOT_AND_ZOOM:
        fprintf(js_include_file,"\n/* add zoom support for jscurves */\
function redraw_jsplot(){for(var p=0; p <= all_jsplots;p++){try{var fun = eval('js_plot'+p);fun();}catch(e){};};};");
        break;
      case JS_ZOOM:
        fprintf(js_include_file,"\n/* add zoom support */\
var x_min_start = xmin;var x_max_start = xmax;var y_min_start = ymin;var y_max_start = ymax;\
var zoomcanvas = create_canvas%d(%d,xsize,ysize);\
function redraw_canvas%d(key){\
 var xmin0 = xmin;var xmax0 = xmax;var ymin0 = ymin;var ymax0 = ymax;\
 switch(key){\
  case 0: xmin++;ymin++;xmax--;ymax--;break;\
  case 1: xmin--;ymin--;xmax++;ymax++;break;\
  case 2: xmin--;xmax--;break;\
  case 3: xmin++;xmax++;break;\
  case 4: ymin--;ymax--;break;\
  case 5: ymax++;ymin++;break;\
  case 6: xmin = x_min_start;xmax=x_max_start;ymin=y_min_start;ymax=y_max_start;break;\
  default: break;\
 };\
 if(xmin >= xmax){xmin = x_min_start;xmax = x_max_start;};\
 if(ymin >= ymax){ymax = y_max_start;ymin = y_min_start;};\
 if( typeof(recalculate_multidraw) === 'function'){recalculate_multidraw(xmin0,xmax0,ymin0,ymax0);redraw_userdraw();}\
 if( typeof(recalculate_userdraw) === 'function'){recalculate_userdraw(xmin0,ymin0,xmax0,ymax0);redraw_userdraw();}\
 if( typeof(redraw_grid) === 'function'){redraw_grid();};\
 if( typeof(redraw_jsplot) === 'function'){redraw_jsplot();};\
 if( typeof(recalculate_dragdrop) === 'function'){recalculate_dragdrop(xmin0,ymin0,xmax0,ymax0);dragstuff.valid = false;dragstuff.draw();};\
 return;\
};\
canvas_div.addEventListener(\"mousedown\",check_zoom_or_pan,false);\
canvas_div.addEventListener(\"wheel\",wheel_zoom,false);\
canvas_div.addEventListener(\"touchstart\",function(e){ e.preventDefault();check_zoom_or_pan(e.changedTouches[0]);},false);\
function wheel_zoom(evt){\
 evt.preventDefault();\
 var key = 1;\
 if(evt.deltaY > 0 ){key = 0};\
 redraw_canvas%d(key);\
 return;\
};\
function check_zoom_or_pan(evt){\
 var mouse = getMouse(evt,zoomcanvas);\
 var my = mouse.y;\
 if(my < ysize - 30){ return; };\
 var mx = mouse.x;\
 var key = -1;\
 for(var p = 15 ; p < 106 ; p = p+15){\
  key++;\
  if(mx > xsize - p){\
   redraw_canvas%d(key);\
   return;\
  };\
 };\
 return;\
};",canvas_root_id,ZOOM_CANVAS,canvas_root_id,canvas_root_id,canvas_root_id);
        break;

      case DRAW_FILL_PATTERN:
        fprintf(js_include_file,"\n /* add fill pattern */ \
function create_Pattern(x0,y0,type,color){document.body.style.cursor = 'wait';var idx = Math.ceil(1000*(Math.random()));var canvas = create_canvas%d(idx,xsize,ysize);canvas.style.visibility = 'hidden';var ctx = canvas.getContext(\"2d\");var x;var y;var dx = 6;var dy = 6;ctx.lineWidth = 1;ctx.strokeStyle = color;ctx.fillStyle = color;var pat;\
 switch(type){\
  case 2: for( x = 0 ; x < xsize ; x = x + dx ){ctx.beginPath();ctx.moveTo(x,0);ctx.lineTo(x,ysize);ctx.closePath();ctx.stroke();};for( y = 0 ; y < ysize; y = y + dy ){ctx.beginPath();ctx.moveTo(0,y);ctx.lineTo(xsize,y);ctx.closePath();ctx.stroke();}; pat = ctx.createPattern(canvas,\"no-repeat\");break;\
  case 3: y = ysize;ctx.beginPath();for( x = 0 ; x < xsize ; x = x + dx ){ctx.moveTo(x,0);ctx.lineTo(xsize,y);y = y - dy;};y = 0;for( x = xsize ; x > 0 ; x = x - dx){ctx.moveTo(x,ysize);ctx.lineTo(0,y); y = y + dy;};ctx.closePath();ctx.stroke(); pat = ctx.createPattern(canvas,\"no-repeat\");break;\
  case 4: y = ysize;ctx.beginPath();for( x = 0 ; x < xsize ; x = x + dx ){ctx.moveTo(x,0); ctx.lineTo(xsize,y); y = y - dy;};y = 0;for( x = xsize ; x > 0 ; x = x - dx){ctx.moveTo(x,ysize);ctx.lineTo(0,y);y = y + dy;};y = 0;for( x = 0 ; x < xsize ; x = x + dx ){ctx.moveTo(x,0); ctx.lineTo(0,y); y = y + dy;};y = 0;for( x = 0 ; x < xsize ; x = x + dx ){ctx.moveTo(xsize,y);ctx.lineTo(x,ysize);y = y + dy;};ctx.closePath(); ctx.stroke(); pat = ctx.createPattern(canvas,\"no-repeat\");break;\
  case 5: for( x = 0 ; x < xsize ; x = x + dx ){for( y = 0 ; y < ysize ; y = y + dy ){ctx.beginPath();ctx.arc(x,y,1,0,2*Math.PI,false);ctx.closePath();ctx.fill();};}; pat = ctx.createPattern(canvas,\"no-repeat\");break;\
  case 6: alert('wait for image '+(image_cnt+1)+'...');pat = image_patterns[image_cnt];image_cnt++;break;\
  default: break;\
 };\
 document.body.style.cursor = 'default';return pat;\
};",canvas_root_id);
        break;

      case JS_TRANSFORM_MOUSE:
/* identify mouse coordinates on transformed objects using command affine */
        fprintf(js_include_file,"\n/* translate mouse */\
function transform_mouse(mx,my,obj){\
if( typeof(obj.affine_matrix) === 'undefined' ){console.log('translate_mouse() affine_matrix undefined');{x:mx,y:my};};\
var m = obj.affine_matrix;\
var d = 1/(m[0]*m[3]-m[1]*m[2]);\
var im = [ m[3]*d, -m[1]*d,-m[2]*d,m[0]*d,d*(m[2]*m[5]-m[3]*m[4]),d*(m[1]*m[4]-m[0]*m[5]) ];\
return { x:mx*im[0]+my*im[2]+im[4], y:mx*im[1]+my*im[3]+im[5] };};");
        break;
      case JS_ROTATE_MOUSE:
/*
identify mouse coordinates on rotated STRING (no 14) objects used for command rotate :
faster than transform matrix
*/
        fprintf(js_include_file,"\n/* rotate mouse */\
function rotate_mouse(mx,my,obj){\
 if( typeof(obj.angle) === 'undefined' ){console.log('rotate_mouse() angle undefined');return {x:mx,y:my};};\
 var cos = Math.cos(obj.angle);\
 var sin = Math.sin(obj.angle);\
 var xc = obj.rotation_center[0];\
 var yc = obj.rotation_center[1];\
 var x_r = parseInt((cos * (mx - xc)) + (sin * (my - yc)) + xc);\
 var y_r = parseInt((cos * (my - yc)) - (sin * (mx - xc)) + yc);\
 return {x:x_r,y:y_r};};");
        break;
      case JS_FIND_ANGLE:
        fprintf(js_include_file,"\n/* function find_angle() */ function find_angle(xc,yc,x1,y1){var dx = x1 - xc;var dy = yc - y1;return Math.atan2(dx,dy);};");
        break;
      case DRAG_EXTERNAL_IMAGE:
/*
the external_canvas is already created: it needs to be FIRST in order to do some drawing onto it
reply is object{} and identical to reply from dragstuff library for drag&drop&click
*/
        fprintf(js_include_file,"\n/* drag external images */\
var external_ctx = external_canvas.getContext(\"2d\");\
var external_canvas_rect = external_canvas.getBoundingClientRect();\
canvas_div.addEventListener(\"mousedown\",setxy,false);\
canvas_div.addEventListener(\"mouseup\",dragstop,false);\
canvas_div.addEventListener(\"mousemove\",dragxy,false);\
canvas_div.addEventListener(\"touchstart\",function(e){ e.preventDefault();setxy(e.changedTouches[0]);},false);\
canvas_div.addEventListener(\"touchend\"  ,function(e){ e.preventDefault();dragstop(e.changedTouches[0]);},false);\
canvas_div.addEventListener(\"touchmove\" ,function(e){ e.preventDefault();dragxy(e.changedTouches[0]);},false);\
var selected_image = null;\
if( typeof(ext_drag_images) !== 'object' ){var ext_drag_images = new Array();};\
if( typeof(ext_image_cnt) == 'undefined'){var ext_image_cnt = 0;};\
if( typeof(slidergroup) == 'undefined' ) { var slidergroup = [];};\
function drag_external_image(URL,sx,sy,swidth,sheight,x0,y0,width,height,onclick,object_cnt,use_offset,use_snap,resize){\
 var image = new Image();\
 image.src = URL;\
 slidergroup[object_cnt] = null;\
 image.onload = function(){\
  if( sx < 1 ){ sx = 0; };\
  if( sy < 1 ){ sy = 0; };\
  if( swidth < 1 ){swidth = image.width;};\
  if( sheight < 1 ){sheight = image.height;};\
  if( width < 1 ){width = image.width;};\
  if( height < 1 ){height = image.height;};\
  if( resize == 1 ){\
   if( swidth > image.width ){ swidth = image.width; };\
   if( sheight > image.height){ sheight = image.height;};\
   if( width > image.width ){ width = image.width; };\
   if( height > image.height){ height = image.height;};\
  };\
  var img = new Array(18);\
  img[0] = onclick;img[1] = image;img[2] = sx;img[3] = sy;img[4] = swidth;img[5] = sheight;\
  img[8] = width;img[9] = height;img[10] = object_cnt;img[11] = use_snap;\
  img[6] = x0;img[7] = y0;\
  img[12] = ext_image_cnt;\
  img[13] = img[6];img[14] = img[7];img[15] = 0;img[16] = 0;img[17] = 0;\
  if(use_offset == 4 ){\
   img[16] = 0.5*img[8];\
   img[17] = 0.5*img[9];\
   img[6]= parseInt(img[6] - img[16]);\
   img[7]= parseInt(img[7] - img[17]);\
  };\
  external_ctx.drawImage(img[1],img[2],img[3],img[4],img[5],img[6],img[7],img[8],img[9]);\
  dragdrop_reply[img[10]] = {type:img[0],object_cnt:img[10],x:x0,y:y0,r:0,angle:0,clicked:img[15]};\
  ext_drag_images[ext_image_cnt] = img;\
  ext_image_cnt++;\
 };\
};\
function dragstop(evt){\
 if(selected_image != null){\
  var s_img = ext_drag_images[selected_image];\
  dragdrop_reply[s_img[10]] = {type:s_img[0], object_cnt:s_img[10],x:s_img[6]+s_img[16],y:s_img[7]+s_img[17],r:s_img[8],angle:s_img[15],clicked:s_img[15]};\
 };\
 selected_image = null;return;\
};\
function dragxy(evt){\
 if( selected_image != null ){\
  var s_img = ext_drag_images[selected_image];\
  var mouse = getMouse(evt,external_canvas);\
  var xy = multisnap_check(mouse.x,mouse.y,s_img[11]);\
  xy[0] = parseInt(xy[0] - 0.5*s_img[8]);\
  xy[1] = parseInt(xy[1] - 0.5*s_img[9]);\
  s_img[6] = xy[0];s_img[7] = xy[1];\
  ext_drag_images[selected_image] = s_img;\
  external_ctx.clearRect(0,0,xsize,ysize);\
  for(var i = 0; i < ext_image_cnt ; i++){\
   var img = ext_drag_images[i];\
   external_ctx.drawImage(img[1],img[2],img[3],img[4],img[5],img[6],img[7],img[8],img[9]);\
  };\
 };\
};\
function setxy(evt){\
 if( ! selected_image ){\
  var mouse = getMouse(evt,external_canvas);\
  var xm = mouse.x;\
  var ym = mouse.y;\
  var img;var xmarge;var ymarge;\
  for(var p = 0 ; p < ext_image_cnt ; p++){\
   if( ext_drag_images[p] ){\
    img = ext_drag_images[p];\
    if( img[0] != 0 ){\
     xmarge = 0.5*img[8];ymarge = 0.5*img[9];\
     if( xm > img[6] - xmarge && xm < img[6] + img[8]){\
      if( ym > img[7] - ymarge && ym < img[7] + img[9]){\
       if( img[0] == 1){\
        if( dragdrop_reply[img[10]].clicked == 1 ){\
         dragdrop_reply[img[10]].clicked = 0;external_ctx.strokeStyle = '#ffffff';\
        }\
        else\
        {\
         dragdrop_reply[img[10]].clicked = 1;external_ctx.strokeStyle = '#00ff00';\
        };\
        external_ctx.lineWidth = 2;\
        external_ctx.beginPath();\
        external_ctx.rect(img[6]-2,img[7]-2,img[8]+2,img[9]+2);\
        external_ctx.closePath();\
        external_ctx.stroke();\
        return;\
       }\
       else\
       {\
        img[6] = xm;\
        img[7] = ym;\
        selected_image = p;\
        dragxy(evt);\
        ext_drag_images[p] = img;\
       };\
      };\
     };\
    };\
   };\
  };\
 } else {\
  selected_image = null;\
 };\
};");
        break;
      case DRAW_EXTERNAL_IMAGE:
/*
the external_canvas is already created: it needs to be FIRST in order to do some drawing onto it
only drag_xy !
snaptogrid | xsnaptogrid | ysnaptogrid works
14/5/2019
on heavy styled wims (unice.fr etc problems with mouse?
now uniform method of retreiving mouse coordinates getMouse()
*/
        fprintf(js_include_file,"\n/* draw external images */\
function draw_external_image(URL,sx,sy,swidth,sheight,x0,y0,width,height,use_offset,resize){\
 var external_ctx = image_copy_canvas.getContext(\"2d\");\n\
 var image = new Image();\
 image.src = URL;\
 image.onload = function(){\
  if( sx < 1 ){ sx = 0; };\
  if( sy < 1 ){ sy = 0; };\
  if( swidth < 1 ){swidth = image.width;};\
  if( sheight < 1 ){sheight = image.height;};\
  if( width < 1 ){width = image.width;};\
  if( height < 1 ){height = image.height;};\
  if( resize == 1 ){\
   if( swidth > image.width ){ swidth = image.width; };\
   if( sheight > image.height){ sheight = image.height;};\
   if( width > image.width ){ width = image.width; };\
   if( height > image.height){ height = image.height;};\
  };\
  var img = new Array(9);\
  img[0] = image;img[1] = sx;img[2] = sy;img[3] = swidth;img[4] = sheight;img[5] = x0;img[6] = y0;\
  if(use_offset == 4 ){img[5] = parseInt(img[5] - 0.5*width);img[6] = parseInt(img[6] - 0.5*height);};\
  img[7] = width;img[8] = height;\
  external_ctx.drawImage(img[0],img[1],img[2],img[3],img[4],img[5],img[6],img[7],img[8]);\
 };\
};");
        break;
      case DRAW_BEZIER:
        fprintf(js_include_file,"\n/* draw bezier curve */\
if( typeof(all_fill_patterns) != 'object' ){ var all_fill_patterns = []; };\
function draw_bezier(canvas_type,linewidth,xy_points,fill_color,fill_opacity,stroke_color,stroke_opacity,use_filled,use_dashed,dashtype0,dashtype1,use_rotate,angle){\
 var obj;\
 if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){\
  obj = document.getElementById(\"wims_canvas%d\"+canvas_type);\
 } else {\
  obj = create_canvas%d(canvas_type,xsize,ysize);\
 };\
 var ctx = obj.getContext(\"2d\");\
 ctx.save();\
 ctx.strokeStyle=\"rgba(\"+stroke_color+\",\"+stroke_opacity+\")\";\
 ctx.lineWidth = linewidth;\
 if(use_rotate == 1 ){ctx.rotate(angle*Math.PI/180);};\
 if(use_dashed == 1){if(ctx.setLineDash){ctx.setLineDash([dashtype0,dashtype1]);}else{ctx.mozDash = [dashtype0,dashtype1];};};\
 ctx.beginPath();\
 ctx.moveTo(x2px(xy_points[0]),y2px(xy_points[1]));\
 ctx.bezierCurveTo(x2px(xy_points[2]),y2px(xy_points[3]),x2px(xy_points[4]),y2px(xy_points[5]),x2px(xy_points[6]),y2px(xy_points[7]));\
 var color = \"rgba(\"+fill_color+\",\"+fill_opacity+\")\";\
 if(use_filled > 1 ){ if(! all_fill_patterns[use_filled] ){ var pat = create_Pattern(0,0,use_filled,color); all_fill_patterns[use_filled] = pat;};ctx.fillStyle = all_fill_patterns[use_filled]; } else { ctx.fillStyle = color;};\
 ctx.stroke();\
 ctx.restore();\
};",canvas_root_id,canvas_root_id,canvas_root_id);
        break;
      case DRAW_GRIDFILL:/* not used for userdraw */
        fprintf(js_include_file,"\n/* draw gridfill */\
var grid_fill_pattern;\
function draw_gridfill(canvas_type,x0,y0,dx,dy,linewidth,color,opacity,xsize,ysize,use_userdraw){\
 if( dx == 0 || dy == 0 ){alert(\"increment is zero !!! \");return;};\
 var fc = %d+canvas_type;\
 var obj = create_canvas%d(fc,xsize,ysize);\
 var ctx = obj.getContext('2d');\
 var x,y;\
 ctx.fillStyle='rgba(255,255,255,0.01)';\
 ctx.rect(0,0,xsize,ysize);\
 ctx.fill();\
 ctx.lineWidth = linewidth;\
 ctx.strokeStyle=\"rgba(\"+color+\",0.01)\";\
 for( x = 0 ; x <= xsize ; x = x + dx ){\
  ctx.beginPath();\
  ctx.moveTo(x,0);\
  ctx.lineTo(x,ysize);\
  ctx.closePath();\
  ctx.stroke();\
 };\
 for( y = 0 ; y <= ysize; y = y + dy ){\
  ctx.beginPath();\
  ctx.moveTo(0,y);\
  ctx.lineTo(xsize,y);\
  ctx.closePath();\
  ctx.stroke();\
 };\
 if( use_userdraw ){\
  grid_fill_pattern = ctx;\
 } else {\
  setTimeout(function(){ filltoborder( x0,y0,false,color,canvas_type,true,ctx); },500);};return;\
};",canvas_root_id,canvas_root_id);
        break;
      case DRAW_IMAGEFILL:/* not  used for userdraw */
        fprintf(js_include_file,"\n/* draw imagefill */\
function draw_imagefill(canvas_type,x0,y0,URL,xsize,ysize,use_userdraw,use_scaling){\
 var fc = %d+canvas_type;\
 var obj = create_canvas%d(fc,xsize,ysize);\
 var ctx = obj.getContext('2d');\
 var img = new Image();\
 img.src = URL;\
 obj.style.visibility = 'hidden';\
 img.onload = function(){\
  if( use_scaling == 1 ){\
   ctx.drawImage(img,x0,y0,xsize,ysize);\
  }else{\
   var w0 = img.width;var h0 = img.height;var w;var h;\
   for( w = x0; w < xsize ; w = w + w0 ){\
    for( h = y0; h < ysize; h = h + h0){\
      ctx.drawImage(img,w,h,w0,h0);\
    };\
   };\
  };\
  if( use_userdraw ){\
   image_pattern = ctx;\
  } else {\
   setTimeout(function(){ filltoborder( x0,y0,false,'red',canvas_type,true,ctx); },500);\
  };\
 };\
};",canvas_root_id,canvas_root_id);
        break;
      case DRAW_DOTFILL:/* not  used for userdraw */
        fprintf(js_include_file,"\n/* draw dotfill */\
var dot_fill_pattern;\
function draw_dotfill(canvas_type,x0,y0,dx,dy,radius,color,opacity,xsize,ysize,use_userdraw){\
if( dx == 0 || dy == 0 ){alert(\"increment is zero !!! \");return;};\
var fc = %d+canvas_type;\
 var obj = create_canvas%d(fc,xsize,ysize);\
 var ctx = obj.getContext('2d');\
 var x,y;\
 ctx.fillStyle='rgba(255,255,255,0.01)';\
 ctx.rect(0,0,xsize,ysize);\
 ctx.fill();\
 ctx.fillStyle=\"rgba(\"+color+\",0.01)\";\
 ctx.strokeStyle=\"rgba(\"+color+\",0.01)\";\
 for( x = 0 ; x < xsize ; x = x + dx ){\
  for( y = 0 ; y < ysize ; y = y + dy ){\
   ctx.beginPath();\
   ctx.arc(x,y,radius,0,2*Math.PI,false);\
   ctx.closePath();\
   ctx.fill();\
  };\
 };\
 if( use_userdraw ){\
  dot_fill_pattern = ctx;\
 } else {\
 setTimeout(function(){ filltoborder( x0,y0,false,color,canvas_type,true,ctx); },500);\
 };\
return;\
};",canvas_root_id,canvas_root_id);
        break;
      case DRAW_SUBSUP:/* Uses 'script-size for numbers and for sub & sup strings */
/* use_offset
 0=none;
 1=yoffset
 2=xoffset
 3=xyoffset
 4=centered
*/
        fprintf(js_include_file,"\n/* draw sub sup imitation in canvas */\
function draw_subsup(ctx,x,y,txt,use_offset){\
 var C;var c;var len = txt.length;var baseline = 'middle';var space = 0;ctx.save();var subsup=0;\
 var m = 0.4*(ctx.measureText(txt).width);\
 var h = ctx.measureText('M').width;\
 switch(use_offset){\
  case 0:break;\
  case 1:y=y-h;break;\
  case 2:x=x+h;break;\
  case 3:x=x+h;y=y-h;break;\
  case 4:x=x-m;break;\
  default:break;\
 };\
 var alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';\
 function check_alpha(C){if(alphabet.indexOf(C) != -1){return 1;}else{return 0;};};\
 var S = parseInt(ctx.font) || 10;var s = 0.8*(parseInt(ctx.font)) || 8;\
 ctx.font = S+'px Helvetica';ctx.textBaseline = baseline;\
 for( c = 0 ; c < len ;c++){\
  C = txt.charAt(c);space = 0;\
  if(C == '_'){ctx.font = s+'px Helvetica';subsup = 1;c++;space = 3;baseline='top';C = txt.charAt(c);}\
  else\
  if(C == '^'){ctx.font = s+'px Helvetica';subsup = 1;c++;space = 3;baseline='bottom';C = txt.charAt(c);}\
  else\
  if(C == ' ' ){ctx.font = S+'px Helvetica';if(subsup == 1){space = 0.6*s - (ctx.measureText('M').width);subsup = 0;};baseline = 'middle';}\
  else\
  if( subsup == 1 &&  c == '-'){c = '\\u2212';ctx.font = s+'px Helvetica';}\
  else\
  if( subsup == 1 &&  c == '+'){c = '\\u002B';ctx.font = s+'px Helvetica';}\
  else\
  if( check_alpha(C) == 1 ){ctx.font = S+'px Helvetica';subsup = 0;baseline = 'middle';}\
  else\
  if(C >= '0' && C <= '9'){ctx.font = s+'px Helvetica';}\
  else{ ctx.font = S+'px Helvetica';};\
  ctx.textBaseline = baseline;ctx.fillText(C,x,y);x = space + x + ctx.measureText(C).width;\
 };\
 ctx.stroke();\
 ctx.restore();\
 return;\
};");
        break;
      case DRAW_TEXTFILL:/* not  used for userdraw */
        fprintf(js_include_file,"\n/* draw textfill */\
var text_fill_pattern;\
function draw_textfill(canvas_type,x0,y0,color,fontfamily,xsize,ysize,txt,use_userdraw){\
var fc = %d+canvas_type;\
var obj = create_canvas%d(fc,xsize,ysize);\
var ctx = obj.getContext('2d');\
ctx.font = fontfamily;\
var dx = 2+(ctx.measureText(txt)).width;\
var dy = 2+(ctx.measureText('MI')).width;\
ctx.fillStyle='rgba(255,255,255,0.01)';\
ctx.rect(0,0,xsize,ysize);\
ctx.fill();\
ctx.fillStyle=\"rgba(\"+color+\",0.01)\";\
ctx.strokeStyle=\"rgba(\"+color+\",0.01)\";\
for(var x = 0 ; x <= xsize ; x = x + dx ){\
 for(var y = 0 ; y <= ysize ; y = y + dy ){\
  ctx.fillText(txt, x, y);\
 };\
};\
if( use_userdraw ){\
 text_fill_pattern = ctx;\
}\
else\
{\
 setTimeout(function(){ filltoborder( x0,y0,false,color,canvas_type,true,ctx); },500);\
};\
return;};",canvas_root_id,canvas_root_id);
        break;
      case DRAW_DIAMONDFILL:/* not used for userdraw */
        fprintf(js_include_file,"\n/* draw_diamond fill */\
var diamond_fill_pattern;\
function draw_diamondfill(canvas_type,x0,y0,dx,dy,linewidth,color,stroke_opacity,xsize,ysize,use_userdraw){\
if( dx == 0 || dy == 0 ){alert(\"increment is zero !!! \");return;};\
if (dx < 0) dx = -dx; if (dy < 0) dy = -dy;\
let nx = Math.ceil(xsize/dx), ny = Math.ceil(ysize/dy);\
let i,m = (nx > ny) ? nx : ny;\
var fc = %d+canvas_type;\
var obj = create_canvas%d(fc,xsize,ysize);\
var ctx = obj.getContext('2d');\
ctx.lineWidth = linewidth;\
ctx.fillStyle='rgba(255,255,255,0.01)';\
ctx.rect(0,0,xsize,ysize);\
ctx.fill();\
ctx.strokeStyle=\"rgba(\"+color+\",0.01)\";\
ctx.beginPath();\
  for(i = -2*m ; i<=m ; ++i){\
    ctx.moveTo(0,i*dy);\
    ctx.lineTo(m*dx,(i+m)*dy);\
    ctx.moveTo(0,-i*dy);\
    ctx.lineTo(m*dx,-(i+m)*dy);\
  }\
ctx.closePath();\
ctx.stroke();\
if( use_userdraw ){\
 diamond_fill_pattern = ctx;\
}\
else\
{\
 setTimeout(function(){ filltoborder( x0,y0,false,color,canvas_type,true,ctx); },500);\
};\
return;\
}",canvas_root_id,canvas_root_id);
        break;
      case DRAW_HATCHFILL:/* not used for userdraw */
        fprintf(js_include_file,"\n/* draw hatch fill */\
var hatch_fill_pattern;\
function draw_hatchfill(canvas_type,x0,y0,dx,dy,linewidth,color,stroke_opacity,xsize,ysize,use_userdraw){\
if( dx == 0 || dy == 0 ){alert(\"increment is zero !!! \");return;};\
if (dx < 0) {dx = -dx; dy = -dy;}\
let nx = Math.ceil(xsize/dx), ny = Math.ceil(ysize/Math.abs(dy));\
let i,m = (nx > ny) ? nx : ny;\
var fc = %d+canvas_type;\
var obj = create_canvas%d(fc,xsize,ysize);\
var ctx = obj.getContext('2d');\
ctx.fillStyle='rgba(255,255,255,0.01)';\
ctx.rect(0,0,xsize,ysize);\
ctx.fill();\
ctx.strokeStyle=\"rgba(\"+color+\",0.01)\";\
ctx.lineWidth = linewidth;\
ctx.beginPath();\
  for(i = -2*m ; i<=m ; ++i){\
    ctx.moveTo(0,i*dy);\
    ctx.lineTo(m*dx,(i+m)*dy);\
  }\
ctx.closePath();\
ctx.stroke();\
if( use_userdraw ){\
 hatch_fill_pattern = ctx;\
}\
else\
{\
 setTimeout(function(){ filltoborder( x0,y0,false,color,canvas_type,true,ctx); },500);\
};\
return;};",canvas_root_id,canvas_root_id);
        break;
      case DRAW_LINES:/*  used for js-tracing */
        fprintf(js_include_file,"\n/* draw lines */\
function calc_line(x1,x2,y1,y2){\
 var marge = 2;\
 if(x1 < x2+marge && x1>x2-marge){\
  return [x1,0,x1,ysize];\
 };\
 if(y1 < y2+marge && y1>y2-marge){\
  return [0,y1,xsize,y1];\
 };\
 var Y1 = y1 - (x1)*(y2 - y1)/(x2 - x1);\
 var Y2 = y1 + (xsize - x1)*(y2 - y1)/(x2 - x1);\
 return [0,Y1,xsize,Y2];\
};\
var draw_lines = function(ctx,x_points,y_points,line_width,stroke_color,stroke_opacity,use_dashed,dashtype0,dashtype1,use_rotate,angle){\
 ctx.save();\
 var line = new Array(4);\
 if(use_rotate == 1 ){ctx.rotate(angle*Math.PI/180);}\
 ctx.lineWidth = line_width;\
 ctx.strokeStyle=\"rgba(\"+stroke_color+\",\"+stroke_opacity+\")\";\
 if(use_dashed == 1){if(ctx.setLineDash){ctx.setLineDash([dashtype0,dashtype1]);}else{ctx.mozDash = [dashtype0,dashtype1];};};\
 for(var p = 0 ; p < x_points.length ; p = p+2 ){\
  line = calc_line(x_points[p],x_points[p+1],y_points[p],y_points[p+1]);\
  ctx.beginPath();\
  ctx.moveTo(line[0],line[1]);\
  ctx.lineTo(line[2],line[3]);\
  ctx.closePath();\
  ctx.stroke();\
  }\
  ctx.restore();\
  return;\
 };");
        break;
      case DRAW_CROSSHAIRS:/*  used for tracing */
        fprintf(js_include_file,"\n/* draw crosshairs  */\
var draw_crosshairs = function(ctx,x_points,y_points,line_width,crosshair_size,stroke_color,stroke_opacity,use_rotate,angle){\
 ctx.save();\
 if(use_rotate == 1 ){ctx.rotate(angle*Math.PI/180);}\
 ctx.lineWidth = line_width;\
 ctx.strokeStyle=\"rgba(\"+stroke_color+\",\"+stroke_opacity+\")\";\
 var x1,x2,y1,y2;\
 for(var p = 0 ; p < x_points.length ; p++ ){\
  x1 = x_points[p] - crosshair_size;\
  x2 = x_points[p] + crosshair_size;\
  y1 = y_points[p] - crosshair_size;\
  y2 = y_points[p] + crosshair_size;\
  ctx.beginPath();\
  ctx.moveTo(x1,y1);\
  ctx.lineTo(x2,y2);\
  ctx.closePath();\
  ctx.stroke();\
  ctx.beginPath();\
  ctx.moveTo(x2,y1);\
  ctx.lineTo(x1,y2);\
  ctx.closePath();\
  ctx.stroke();\
 }\
 ctx.restore();\
  return;\
};");
        break;
      case DRAW_VIDEO:/* not  used for userdraw */
        fprintf(js_include_file,"\n/* draw video */\
var draw_video = function(canvas_root_id,x,y,w,h,URL){\
 var canvas_div = document.getElementById(\"canvas_div\"+canvas_root_id);\
 var video_div = document.createElement(\"div\");\
 canvas_div.appendChild(video_div);\
 video_div.style.position = \"absolute\";\
 video_div.style.left = x+\"px\";\
 video_div.style.top = y+\"px\";\
 video_div.style.width = w+\"px\";\
 video_div.style.height = h+\"px\";\
 var video = document.createElement(\"video\");\
 video_div.appendChild(video);\
 video.style.width = w+\"px\";\
 video.style.height = h+\"px\";\
 video.autobuffer = true;\
 video.controls = true;video.autoplay = false;\
 var src = document.createElement(\"source\");\
 src.type = \"video/mp4\";\
 src.src = URL;\
 video.appendChild(src);\
 video.load();\
 return;\
};");
        break;
      case DRAW_AUDIO:/* not used for userdraw */
        fprintf(js_include_file,"\n/* draw audio */\
var draw_audio = function(canvas_root_id,x,y,w,h,loop,visible,URL1,URL2){\
 var canvas_div = document.getElementById(\"canvas_div\"+canvas_root_id);\
 var audio_div = document.createElement(\"div\");\
 canvas_div.appendChild(audio_div);\
 audio_div.style.position = \"absolute\";\
 audio_div.style.left = x+\"px\";\
 audio_div.style.top = y+\"px\";\
 audio_div.style.width = w+\"px\";\
 audio_div.style.height = h+\"px\";\
 var audio = document.createElement(\"audio\");\
 audio_div.appendChild(audio);\
 audio.setAttribute(\"style\",\"width:\"+w+\"px;height:\"+h+\"px\");\
 audio.autobuffer = true;\
 if(visible == 1 ){ audio.controls = true;audio.autoplay = false;}else{ audio.controls = false;audio.autoplay = true;};\
 if(loop == 1 ){ audio.loop = true;}else{ audio.loop = false;};\
 var src1 = document.createElement(\"source\");\
 src1.type = \"audio/ogg\";\
 src1.src = URL1;\
 audio.appendChild(src1);\
 var src2 = document.createElement(\"source\");\
 src2.type = \"audio/mpeg\";\
 src2.src = URL2;\
 audio.appendChild(src2);\
 audio.load();\
 return;\
};");
        break;
      case DRAW_HTTP:/* not  used for userdraw */
        fprintf(js_include_file,"\n/* draw http */\
var draw_http = function(canvas_root_id,x,y,w,h,URL){\
 var canvas_div = document.getElementById(\"canvas_div\"+canvas_root_id);\
 var http_div = document.createElement(\"div\");\
 var iframe = document.createElement(\"iframe\");\
 canvas_div.appendChild(http_div);\
 http_div.appendChild(iframe);\
 iframe.src = URL;\
 iframe.setAttribute(\"width\",w);\
 iframe.setAttribute(\"height\",h);\
 return;\
};");
        break;
      case DRAW_XML:
/*
!! function draw_xml(object)
!! produced by : DRAW_XML
!! used by commands: katex,latex,mathml,html,obabel
!! uses js-functions: calculate_size(xml_div),rad2deg(rad)
!! user interaction: drag,onclick,slider
!! reply function: read_dragdrop()

onclick=0; no interaction
onclick=1: click
onclick=2: drag ==> always centered (use_offset=4)
onclick=3: slider + click on object
onclick=4: slider (always active , keyword 'active' in slider params)
onclick=5: slider + dragdrop
xy:  drag_type = 0;
x:   drag_type = 1;
y:   drag_type = 2;

obj.type: mathml,katex,html,svg,image(bitmap) (attention: for centering image needs to be measured after loading page !!)
*/

        fprintf(js_include_file,"\n/* draw xml */\
function centered(xml_div,obj){\
 var ww = 0.5*(parseFloat(window.getComputedStyle(xml_div).width));\
 var hh = 0.5*(parseFloat(window.getComputedStyle(xml_div).height));\
 obj.offset = [ ww , hh ];\
 /*xml_div.style.left = obj.x[0] - obj.offset[0] +'px';\
 xml_div.style.top  = obj.y[0] - obj.offset[1] +'px';*/\
 xml_div.style.left = (obj.x[0] - obj.offset[0])/xsize*100 +'%%';\
 xml_div.style.top  = (obj.y[0] - obj.offset[1])/ysize*100 +'%%';\
};\
function rad2deg(rad){return rad*180/(Math.PI);};\
if( typeof(slidergroup) !== 'object' ){ var slidergroup = [];};\
function draw_xml(obj){\
 var mouse;var xy = [obj.x[0],obj.y[0]];\
 var xml_div;\
 dragdrop_reply[obj.object_cnt] = {object_cnt:obj.object_cnt,type:obj.onclick,x:obj.x,y:obj.y,r:0,angle:obj.angle,clicked:0};\
 if( obj.once ){\
  xml_div = document.createElement('DIV');\
  canvas_div.appendChild(xml_div);\
  if( obj.type == 'katex' ){ obj.mathml = LoadKaTeX('html',obj.mathml);};\
  xml_div.id = 'xml_div'+obj.id;\
  xml_div.innerHTML = obj.mathml;\
  xml_div.classList.add('canvasdraw_xml');\
  xml_div.style.color = \"rgba(\"+obj.stroke_color+\",\"+obj.stroke_opacity+\")\";\
  xml_div.style.font = obj.fontfamily;\
  if( ! obj.rotation_center){obj.rotation_center = [obj.x[0],obj.y[0]];}else{obj.rotation_center=[x2px(obj.rotation_center[0]) ,y2px(obj.rotation_center[1]) ];};\
  if( obj.use_center == 1 ){if( obj.type == 'image' ){xml_div.addEventListener('load',function(e){centered(xml_div,obj);},true);}else{%s};};\
 } else {\
  xml_div = document.getElementById('xml_div'+obj.id );\
 };\
 if( typeof(xml_div) === 'undefined' ){console.log('[canvasutil] xml_div undefined'); return;};\
 /*xml_div.style.left = parseInt(obj.x[0] - obj.offset[0]) +'px';\
 xml_div.style.top  = parseInt(obj.y[0] - obj.offset[1]) +'px';*/\
 xml_div.style.left = parseInt((obj.x[0] - obj.offset[0])/xsize*100) +'%%';\
 xml_div.style.top  = parseInt((obj.y[0] - obj.offset[1])/ysize*100) +'%%';\
 var back_color = 'rgba('+obj.fill_color+','+obj.fill_opacity+')';\
 var no_color = 'rgba(255,255,255,0)';var dragging = false;\
 if( obj.use_affine != 0 || obj.angle != 0 ){\
  xml_div.style.transform = 'matrix('+obj.transform[0]+','+obj.transform[1]+','+obj.transform[2]+','+obj.transform[3]+','+obj.transform[4]+','+obj.transform[5]+') rotate('+rad2deg(obj.angle)+'deg)';\
 };\
 if( obj.once ){\
  obj.once = false;\
  switch(obj.onclick){\
   case 2:\
    if(isTouch){\
     canvas_div.addEventListener('touchmove' ,function(evt){ evt.preventDefault();if(!dragging){return;};mouse = getMouse(evt.changedTouches[0],xml_div);xy = multisnap_check( mouse.x,mouse.y,obj.use_snap);xml_div.style.left = xy[0] - obj.offset[0] - obj.transform[4] + 'px';xml_div.style.top = xy[1] - obj.offset[1] - obj.transform[5] + 'px';},false);\
     canvas_div.addEventListener('touchend'  ,function(evt){ evt.preventDefault();dragging = false;xml_div.style.backgroundColor = no_color;dragdrop_reply[obj.object_cnt] = {object_cnt:obj.object_cnt,type:obj.onclick,x:xy[0],y:xy[1],r:0,angle:obj.angle,clicked:0};obj.x[0] = xy[0];obj.y[0] = xy[1];},false);\
     xml_div.addEventListener('touchstart',function(evt){evt.preventDefault();if(dragging){dragging = false;xml_div.style.backgroundColor = no_color;}else{dragging = true;xml_div.style.backgroundColor = back_color;};},false);\
     xml_div.addEventListener('touchend'  ,function(evt){ evt.preventDefault();dragging = false;xml_div.style.backgroundColor = no_color;dragdrop_reply[obj.object_cnt] = {object_cnt:obj.object_cnt,type:obj.onclick,x:xy[0],y:xy[1],r:0,angle:obj.angle,clicked:0};obj.x[0] = xy[0];obj.y[0] = xy[1];},false);\
    } else {\
     canvas_div.addEventListener('mousemove',function(evt){if(!dragging){return;};mouse = getMouse(evt,xml_div);xy = multisnap_check( mouse.x,mouse.y,obj.use_snap);xml_div.style.left = xy[0] - obj.offset[0] - obj.transform[4] + 'px';xml_div.style.top = xy[1] - obj.offset[1] - obj.transform[5] + 'px';},false);\
     canvas_div.addEventListener('mouseup',function(evt){dragging = false;xml_div.style.backgroundColor = no_color;dragdrop_reply[obj.object_cnt] = {object_cnt:obj.object_cnt,type:obj.onclick,x:xy[0],y:xy[1],r:0,angle:obj.angle,clicked:0};obj.x[0] = xy[0];obj.y[0] = xy[1];},false);\
     xml_div.addEventListener('mousedown', function(evt){if(dragging){dragging = false;xml_div.style.backgroundColor = no_color;}else{dragging = true;xml_div.style.backgroundColor = back_color;};},false);\
    };\
    break;\
   case 1:\
    if(isTouch){\
     xml_div.addEventListener('touchstart', function(e){ e.preventDefault();if(dragdrop_reply[obj.object_cnt].clicked == 0){ dragdrop_reply[obj.object_cnt].clicked = 1;xml_div.style.backgroundColor = back_color;}else{dragdrop_reply[obj.object_cnt].clicked = 0;xml_div.style.backgroundColor = no_color;};} ,false);\
    } else {\
     xml_div.addEventListener('mousedown'  , function(evt){if(dragdrop_reply[obj.object_cnt].clicked == 0){ dragdrop_reply[obj.object_cnt].clicked = 1;xml_div.style.backgroundColor = back_color;}else{dragdrop_reply[obj.object_cnt].clicked = 0;xml_div.style.backgroundColor = no_color;};} ,false);\
    };\
    break;\
   case 3:\
    if(isTouch){\
     xml_div.addEventListener('touchstart',function(evt){ if(slidergroup[obj.object_cnt] == null ){ slidergroup[obj.object_cnt] = obj;xml_div.style.backgroundColor = back_color;}else{ slidergroup[obj.object_cnt] = null;xml_div.style.backgroundColor = no_color;};} ,false);\
    } else {\
     xml_div.addEventListener('mousedown' ,function(evt){ if(slidergroup[obj.object_cnt] == null ){ slidergroup[obj.object_cnt] = obj;xml_div.style.backgroundColor = back_color;}else{ slidergroup[obj.object_cnt] = null;xml_div.style.backgroundColor = no_color;};} ,false);\
    };\
    break;\
   case 4:slidergroup[obj.object_cnt] = obj;break;\
   case 5:slidergroup[obj.object_cnt] = obj;\
    canvas_div.addEventListener('mousemove',function(evt){if(!dragging){return;};mouse = getMouse(evt,xml_div);xy = multisnap_check( mouse.x,mouse.y,obj.use_snap);xml_div.style.left = xy[0] - obj.offset[0] - obj.transform[4] + 'px';xml_div.style.top = xy[1] - obj.offset[1] - obj.transform[5] + 'px';},false);\
    canvas_div.addEventListener('mouseup',function(evt){dragging = false;xml_div.style.backgroundColor = no_color;dragdrop_reply[obj.object_cnt] = {object_cnt:obj.object_cnt,type:obj.onclick,x:xy[0],y:xy[1],r:0,angle:obj.angle,clicked:0};obj.x[0] = xy[0];obj.y[0] = xy[1];},false);\
    xml_div.addEventListener('mousedown', function(evt){if(dragging){dragging = false;xml_div.style.backgroundColor = no_color;}else{dragging = true;xml_div.style.backgroundColor = back_color;};},false);\
    break;\
   default: break;\
  };\
 };\
 return;\
};",mathjax);

/*
console.log('angle='+obj.angle+'obj.use_center='+obj.use_center+'id='+obj.id+'xml_div.id = '+xml_div.id+' once ='+obj.once+' offset='+ obj.offset+' div x = '+xml_div.style.left+' div y ='+xml_div.style.top+' obj.x='+px2x(obj.x)+'obj.y='+px2y(obj.y));\
*/
        break;
      case DRAW_SGRAPH:
/*
 xstart = given
 ystart = given
 sgraph(canvas_type,precision,xmajor,ymajor,xminor,yminor,majorcolor,minorcolor,fontfamily)
*/
        fprintf(js_include_file,"\n/* draw sgraph */\
var draw_sgraph = function(canvas_type,precision,xmajor,ymajor,xminor,yminor,majorcolor,minorcolor,fontfamily,opacity,font_size){\
 var obj;if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){obj = document.getElementById(\"wims_canvas%d\"+canvas_type);}else{ obj = create_canvas%d(canvas_type,xsize,ysize);};\
 var ctx = obj.getContext(\"2d\");\
 ctx.font = fontfamily;\
 var minor_opacity = 0.8*opacity;\
 ctx.clearRect(0,0,xsize,ysize);\
 var zero_x = 0.1*xsize;\
 var zero_y = 0.9*ysize;\
 var snor_x;var snor_y;\
 if( xstart != xmin){\
  snor_x = 0.1*xsize;\
 }else{\
  snor_x = 0;\
  xstart = xmin;\
 };\
 ctx.strokeStyle = \"rgba(\"+majorcolor+\",\"+opacity+\")\";\
 ctx.lineWidth = 2;\
 ctx.beginPath();\
 ctx.moveTo(xsize,zero_y);\
 ctx.lineTo(zero_x,zero_y);\
 ctx.lineTo(zero_x,0);\
 ctx.stroke();\
 ctx.closePath();\
 ctx.beginPath();\
 ctx.moveTo(zero_x,zero_y);\
 ctx.lineTo(zero_x + 0.25*snor_x,zero_y - 0.1*snor_x);\
 ctx.lineTo(zero_x + 0.5*snor_x,zero_y + 0.1*snor_x);\
 ctx.lineTo(zero_x + 0.75*snor_x,zero_y - 0.1*snor_x);\
 ctx.lineTo(zero_x + snor_x,zero_y);\
 ctx.stroke();\
 ctx.closePath();\
 ctx.beginPath();\
 var num = xstart;\
 var flipflop = 1;\
 var step_x = xmajor*(xsize - zero_x - snor_x)/(xmax - xstart);\
 var txtsize;var txt_marge=step_x - 5;\
 for(var x = zero_x+snor_x ; x < xsize;x = x + step_x){\
  txtsize = ctx.measureText(num).width;\
  if( txtsize > txt_marge ){if( flipflop == 1 ){flipflop = 0;}else{flipflop = 1;};};\
  if( flipflop == 1){\
   ctx.fillText(num,x - 0.5*txtsize,zero_y+font_size);\
  } else {\
   ctx.fillText(num,x - 0.5*txtsize,zero_y+2*font_size);\
  };\
  num = num + xmajor;\
 };\
 ctx.stroke();\
 ctx.closePath();\
 ctx.lineWidth = 1;\
 ctx.beginPath();\
 for(var x = zero_x+snor_x ; x < xsize;x = x + step_x){\
   ctx.moveTo(x,zero_y);\
   ctx.lineTo(x,0);\
 };\
 ctx.stroke();\
 ctx.closePath();\
 if( xminor > 1){\
  ctx.lineWidth = 0.5;\
  ctx.beginPath();\
  ctx.strokeStyle = \"rgba(\"+minorcolor+\",\"+minor_opacity+\")\";\
  var minor_step_x = step_x / xminor;\
  var nx;\
  for(var x = zero_x+snor_x; x < xsize;x = x + step_x){\
    num = 1;\
    for(var p = 1 ; p < xminor ; p++){\
     nx = x + num*minor_step_x;\
     ctx.moveTo(nx,zero_y);\
     ctx.lineTo(nx,0);\
     num++;\
    };\
  };\
  ctx.stroke();\
  ctx.closePath();\
  ctx.beginPath();\
  ctx.lineWidth = 2;\
  ctx.strokeStyle = \"rgba(\"+majorcolor+\",\"+opacity+\")\";\
  for(var x = zero_x+snor_x ; x < xsize;x = x + step_x){\
   ctx.moveTo(x,zero_y);ctx.lineTo(x,zero_y - 12);\
  };\
  for(var x = zero_x+snor_x ; x < xsize;x = x + minor_step_x){\
   ctx.moveTo(x,zero_y);ctx.lineTo(x,zero_y - 6);\
  };\
  ctx.stroke();\
  ctx.closePath();\
  ctx.lineWidth = 0.5;\
 };\
 xmin = xstart - (xmajor*(zero_x+snor_x)/step_x);\
 if( ystart != ymin){\
  snor_y = 0.1*ysize;\
 } else {\
  snor_y = 0;\
  ystart = ymin;\
 };\
 ctx.lineWidth = 2;\
 ctx.strokeStyle = \"rgba(\"+majorcolor+\",\"+opacity+\")\";\
 ctx.beginPath();\
 ctx.moveTo(zero_x,zero_y);\
 ctx.lineTo(zero_x - 0.1*snor_y,zero_y - 0.25*snor_y);\
 ctx.lineTo(zero_x + 0.1*snor_y,zero_y - 0.5*snor_y);\
 ctx.lineTo(zero_x - 0.1*snor_y,zero_y - 0.75*snor_y);\
 ctx.lineTo(zero_x,zero_y - snor_y);\
 ctx.stroke();\
 ctx.closePath();\
 ctx.beginPath();\
 ctx.lineWidth = 1;\
 num = ystart;\
 var step_y = ymajor*(zero_y - snor_y)/(ymax - ystart);\
 for(var y = zero_y - snor_y ; y > 0; y = y - step_y){\
  ctx.moveTo(zero_x,y);\
  ctx.lineTo(xsize,y);\
  ctx.fillText(num,zero_x - ctx.measureText(num+\" \").width,parseInt(y+0.2*font_size));\
  num = num + ymajor;\
 };\
 ctx.stroke();\
 ctx.closePath();\
 if( yminor > 1){\
  ctx.lineWidth = 0.5;\
  ctx.beginPath();\
  ctx.strokeStyle = \"rgba(\"+minorcolor+\",\"+minor_opacity+\")\";\
  var minor_step_y = step_y / yminor;\
  var ny;\
  for(var y = 0 ; y < zero_y - snor_y ;y = y + step_y){\
   num = 1;\
   for(var p = 1 ;p < yminor;p++){\
     ny = y + num*minor_step_y;\
     ctx.moveTo(zero_x,ny);\
     ctx.lineTo(xsize,ny);\
     num++;\
    };\
  };\
  ctx.stroke();\
  ctx.closePath();\
  ctx.lineWidth = 2;\
  ctx.beginPath();\
  ctx.strokeStyle = \"rgba(\"+majorcolor+\",\"+opacity+\")\";\
  for(var y = zero_y - snor_y ; y > 0 ;y = y - step_y){\
   ctx.moveTo(zero_x,y);\
   ctx.lineTo(zero_x+12,y);\
  };\
  for(var y = zero_y - snor_y ; y > 0 ;y = y - minor_step_y){\
   ctx.moveTo(zero_x,y);\
   ctx.lineTo(zero_x+6,y);\
  };\
  ctx.stroke();\
  ctx.closePath();\
 };\
 ymin = ystart - (ymajor*(ysize - zero_y + snor_y)/step_y);\
 if( typeof(legend%d)  !== 'undefined' ){\
  ctx.globalAlpha = 1.0;\
  var y_offset = 2*font_size;\
  var txt;var txt_size;\
  var x_offset = xsize - 2*font_size;\
  var l_length = legend%d.length;var barcolor = new Array();\
  if( typeof(legendcolors%d) !== 'undefined' ){\
   for(var p = 0 ; p < l_length ; p++){\
    barcolor[p] = legendcolors%d[p];\
   };\
  } else {\
   if( barcolor.length == 0 ){\
    for(var p = 0 ; p < l_length ; p++){\
     barcolor[p] = stroke_color;\
    };\
   };\
  };\
  for(var p = 0; p < l_length; p++){\
   ctx.fillStyle = barcolor[p];\
   txt = legend%d[p];\
   txt_size = ctx.measureText(txt).width;\
   ctx.fillText(legend%d[p],x_offset - txt_size, y_offset);\
   y_offset = parseInt(y_offset + 1.5*font_size);\
  };\
 };\
 if( typeof(xaxislabel) !== 'undefined' ){\
   ctx.fillStyle = \'#000000\';\
   var txt_size = ctx.measureText(xaxislabel).width + 4 ;\
   ctx.fillText(xaxislabel,xsize - txt_size, zero_y - 7);\
 };\
 if( typeof(yaxislabel) !== 'undefined'){\
   ctx.save();\
   ctx.fillStyle = \'#000000\';\
   var txt_size = ctx.measureText(yaxislabel).width;\
   ctx.translate(zero_x+8 + font_size,txt_size+font_size);\
   ctx.rotate(-0.5*Math.PI);\
   ctx.fillText(yaxislabel,0,0);\
   ctx.restore();\
 };\
};",canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id);
        break;
      case DRAW_NUMBERLINE:
fprintf(js_include_file,"\n/* draw numberline */\
var draw_numberline = function(canvas_type,use_axis_numbering,x0,x1,xmajor,xminor,y0,y1,linewidth,strokecolor,strokeopacity,fontfamily,fontcolor,precision){\
 var obj = create_canvas%d(canvas_type,xsize,ysize);\
 var ctx = obj.getContext(\"2d\");\
 ctx.lineWidth = linewidth || 1;\
 ctx.strokeStyle = \"rgba(\"+strokecolor+\",\"+strokeopacity+\")\";\
 ctx.font = fontfamily || 'Arial 12px';\
 var fontsize = parseInt(ctx.font);\
 ctx.fillStyle =  \"rgba(\"+fontcolor+\",\"+strokeopacity+\")\";\
 x1 = x2px(x1);\
 x0 = x2px(x0);\
 y0 = y2px(y0);\
 y1 = y2px(y1);\
 var sub_devision = -1;\
 if( xminor%%2 == 0 ){ sub_devision = xminor/2; };\
 var ybase1 = parseInt( y0 + fontsize + 2 );\
 var ybase2 = parseInt( ybase1 + fontsize + 2 );\
 var yh = Math.abs(parseInt( y0 - 0.3*(y0 -y1)));\
 var ys = Math.abs(parseInt( y0 - 0.6*(y0 -y1)));\
 xmajor = x2px(xmajor) - x2px(0);\
 var i;var len;var p;\
 xminor = xmajor / xminor;\
 ctx.beginPath();\
 for(p = x0 ; p < x1 ; p = p + xmajor){\
  ctx.moveTo(p,y0);ctx.lineTo(p,y1);i = 0;\
  for(var s = p ; s < p + xmajor ; s = s + xminor ){\
   ctx.moveTo(s,y0);\
   if( sub_devision == i){ ctx.lineTo(s,ys); } else { ctx.lineTo(s,yh); };\
   i++;\
  };\
 };\
 ctx.moveTo(p,y0);ctx.lineTo(p,y1);\
 ctx.closePath();\
 ctx.stroke();\
 if( use_axis_numbering >-1 ){\
  var str = x_strings[use_axis_numbering];\
  len = str.length;if((len/2+0.5)%%2 == 0){ alert(\"xaxis number unpaired:  text missing ! \");return;};\
  var corr;var x_nums;var x_text;var flipflop = 0;var off = ybase1;\
  ctx.beginPath();\
  if( x_strings_up[use_axis_numbering] == null){\
   for(var p = 0 ; p < len ; p = p+2){\
    var x_nums = x2px(eval(str[p]));\
    var x_text = str[p+1];\
    corr = ctx.measureText(x_text).width;\
    if( corr > xmajor){ if(flipflop == 0 ){flipflop = 1; off = ybase2;}else{flipflop = 0; off = ybase1;};};\
    ctx.fillText(x_text,parseInt(x_nums-0.5*corr),off);\
   };\
  } else {\
   for(var p = 0 ; p < len ; p = p+2){\
    x_nums = x2px(eval(str[p]));\
    x_text = str[p+1];\
    corr = ctx.measureText(x_text).width + ybase1 - fontsize;\
    ctx.save();\
    ctx.translate(x_nums+0.5*fontsize, corr);\
    ctx.rotate(-1.5708);\
    ctx.fillText(x_text,0,0);\
    ctx.restore();\
   };\
  }\
 } else {\
  var corr;var num;var flipflop = 0;var off = ybase1;\
  var prec = parseInt(Math.log(precision)/Math.log(10));\
  for(var p = x0 ; p < x1+xmajor ; p = p+xmajor){\
   num = (px2x(p)).toFixed(prec);\
   corr = ctx.measureText(num).width;\
   if( corr > xmajor){ if(flipflop == 0 ){flipflop = 1; off = ybase2;}else{flipflop = 0; off = ybase1;};};\
   ctx.fillText(num,parseInt(p - 0.5*corr),off);\
  };\
 };\
};",canvas_root_id);
        break;
      case DRAW_GRID:/* not used for userdraw */
        fprintf(js_include_file,"\n/* draw grid */\
var draw_grid%d = function(canvas_type,precision,stroke_opacity,xmajor,ymajor,xminor,yminor,tics_length,line_width,stroke_color,axis_color,font_size,font_family,use_axis,use_axis_numbering,use_dashed,dashtype0,dashtype1,font_color,fill_opacity){\
var obj;if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){obj = document.getElementById(\"wims_canvas%d\"+canvas_type);}else{obj = create_canvas%d(canvas_type,xsize,ysize);};\
var ctx = obj.getContext(\"2d\");ctx.clearRect(0,0,xsize,ysize);\
if(use_dashed == 1){if(ctx.setLineDash){ctx.setLineDash([dashtype0,dashtype1]);}else{ctx.mozDash = [dashtype0,dashtype1];};};\
ctx.save();\
var stroke_color = \"rgba(\"+stroke_color+\",\"+stroke_opacity+\")\";\
ctx.fillStyle = \"rgba(\"+font_color+\",\"+1.0+\")\";\
var axis_color = \"rgba(\"+axis_color+\",\"+stroke_opacity+\")\";\
ctx.font = font_family;\
var barcolor = new Array();\
var xstep = xsize*xmajor/(xmax - xmin);\
var ystep = ysize*ymajor/(ymax - ymin);\
var x2step = xstep / xminor;\
var y2step = ystep / yminor;\
var zero_x = x2px(0);var zero_y = y2px(0);var f_x;var f_y;\
if(xmin < 0 ){ f_x = -1;}else{ f_x = 1;};\
if(ymin < 0 ){ f_y = -1;}else{ f_y = 1;};\
ctx.beginPath();\
ctx.lineWidth = line_width;\
ctx.strokeStyle = stroke_color;\
for(var p = zero_x ; p < xsize; p = p + xstep){\
 ctx.moveTo(p,0);\
 ctx.lineTo(p,ysize);\
};\
for(var p = zero_x ; p > 0; p = p - xstep){\
 ctx.moveTo(p,0);\
 ctx.lineTo(p,ysize);\
};\
for(var p = zero_y ; p < ysize; p = p + ystep){\
 ctx.moveTo(0,p);\
 ctx.lineTo(xsize,p);\
};\
for(var p = zero_y ; p > 0; p = p - ystep){\
 ctx.moveTo(0,p);\
 ctx.lineTo(xsize,p);\
};\
if( typeof(xaxislabel) !== 'undefined' ){\
 ctx.save();\
 ctx.font = \"italic \"+font_size+\"px Arial\";\
 var corr =  parseInt(1.1*ctx.measureText(xaxislabel).width);\
 ctx.fillText(xaxislabel,xsize - corr,zero_y - tics_length - 0.4*font_size);\
 ctx.restore();\
};\
if( typeof(yaxislabel) !== 'undefined' ){\
 ctx.save();\
 ctx.font = \"italic \"+font_size+\"px Arial\";\
 var corr = parseInt(ctx.measureText(yaxislabel).width + font_size);\
 ctx.translate(zero_x+tics_length + font_size,corr);\
 ctx.rotate(-0.5*Math.PI);\
 ctx.fillText(yaxislabel,0,0);\
 ctx.restore();\
};\
ctx.stroke();\
ctx.closePath();",canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id);

        if( use_axis == 1 ){
          fprintf(js_include_file,"\
 ctx.save();\
 ctx.beginPath();\
 ctx.strokeStyle = stroke_color;\
 ctx.lineWidth = 0.6*line_width;\
 for(var p = zero_x ; p < xsize; p = p + x2step){\
  ctx.moveTo(p,0);\
  ctx.lineTo(p,ysize);\
 };\
 for(var p = zero_x ; p > 0; p = p - x2step){\
  ctx.moveTo(p,0);\
  ctx.lineTo(p,ysize);\
 };\
 for(var p = zero_y ; p < ysize; p = p + y2step){\
  ctx.moveTo(0,p);\
  ctx.lineTo(xsize,p);\
 };\
 for(var p = zero_y ; p > 0; p = p - y2step){\
  ctx.moveTo(0,p);\
  ctx.lineTo(xsize,p);\
 };\
 ctx.stroke();\
 ctx.closePath();\
 ctx.beginPath();\
 ctx.lineWidth = 2*line_width;\
 ctx.strokeStyle = axis_color;\
 ctx.moveTo(0,zero_y);\
 ctx.lineTo(xsize,zero_y);\
 ctx.moveTo(zero_x,0);\
 ctx.lineTo(zero_x,ysize);\
 ctx.stroke();\
 ctx.closePath();\
 ctx.lineWidth = line_width+0.5;\
 ctx.beginPath();\
 for(var p = zero_x ; p < xsize; p = p + xstep){\
  ctx.moveTo(p,zero_y-tics_length);\
  ctx.lineTo(p,zero_y+tics_length);\
 };\
 for(var p = zero_x ; p > 0; p = p - xstep){\
  ctx.moveTo(p,zero_y-tics_length);\
  ctx.lineTo(p,zero_y+tics_length);\
 };\
 for(var p = zero_y ; p < ysize; p = p + ystep){\
  ctx.moveTo(zero_x-tics_length,p);\
  ctx.lineTo(zero_x+tics_length,p);\
 };\
 for(var p = zero_y ; p > 0; p = p - ystep){\
  ctx.moveTo(zero_x-tics_length,p);\
  ctx.lineTo(zero_x+tics_length,p);\
 };\
 for(var p = zero_x ; p < xsize; p = p + x2step){\
  ctx.moveTo(p,zero_y-0.5*tics_length);\
  ctx.lineTo(p,zero_y+0.5*tics_length);\
 };\
 for(var p = zero_x ; p > 0; p = p - x2step){\
  ctx.moveTo(p,zero_y-0.5*tics_length);\
  ctx.lineTo(p,zero_y+0.5*tics_length);\
 };\
 for(var p = zero_y ; p < ysize; p = p + y2step){\
  ctx.moveTo(zero_x-0.5*tics_length,p);\
  ctx.lineTo(zero_x+0.5*tics_length,p);\
 };\
 for(var p = zero_y ; p > 0; p = p - y2step){\
  ctx.moveTo(zero_x-0.5*tics_length,p);\
  ctx.lineTo(zero_x+0.5*tics_length,p);\
 };\
 ctx.stroke();\
 ctx.closePath();\
 ctx.restore();");
        }
        if( use_axis_numbering != -1 ){
          fprintf(js_include_file,"\
 ctx.save();\
 ctx.fillColor = axis_color;\
 ctx.strokeStyle = axis_color;\
 ctx.lineWidth = 2*line_width;\
 ctx.font = font_family;\
 var shift = zero_y+2*font_size;var flip=0;var skip=0;var corr;var cnt;var disp_cnt;var prec;\
 if( x_strings[use_axis_numbering] != null ){\
  var str = x_strings[use_axis_numbering];\
  var len = str.length;if((len/2+0.5)%%2 == 0){ alert(\"xaxis number unpaired:  text missing ! \");return;};\
  ctx.beginPath();\
  if( x_strings_up[use_axis_numbering] == null){\
   for(var p = 0 ; p < len ; p = p+2){\
    var x_nums = x2px(eval(str[p]));\
    var x_text = str[p+1];\
    corr = ctx.measureText(x_text).width;\
    skip = 1.2*corr/xstep;\
    if( zero_y+2*font_size > ysize ){shift = ysize - 2*font_size;};\
    if( skip > 1 ){if(flip == 0 ){flip = 1; shift = shift + font_size;}else{flip = 0; shift = shift - font_size;};};\
    ctx.fillText(x_text,parseInt(x_nums-0.5*corr),shift);\
    ctx.moveTo(x_nums,zero_y - tics_length);\
    ctx.lineTo(x_nums,zero_y + tics_length);\
   };\
  } else {\
   for(var p = 0 ; p < len ; p = p+2){\
    var x_nums = x2px(eval(str[p]));\
    var x_text = str[p+1];\
    corr = 2 + tics_length + zero_y + ctx.measureText(x_text).width;\
    if( corr > ysize ){corr = ysize;};\
    ctx.save();\
    ctx.translate(x_nums+0.25*font_size, corr);\
    ctx.rotate(-1.5708);\
    ctx.fillText(x_text,0,0);\
    ctx.restore();\
    ctx.moveTo(x_nums,zero_y - tics_length);\
    ctx.lineTo(x_nums,zero_y + tics_length);\
   };\
  };\
  ctx.closePath();\
 } else {\
  skip = 1;cnt = px2x(zero_x);\
  prec = Math.log(precision)/(Math.log(10));\
  var y_basis;if(f_y == 1){ y_basis = ysize }else{ y_basis = zero_y + 1.4*font_size;};\
  for( var p = zero_x ; p < xsize ; p = p+xstep){\
   if(skip == 0 ){\
    disp_cnt = cnt.toFixed(prec);\
    corr = ctx.measureText(disp_cnt).width;\
    skip = parseInt(1.2*corr/xstep);\
    ctx.fillText(disp_cnt,p-0.5*corr,y_basis);\
   } else {\
    skip--;\
   };\
   cnt = cnt + xmajor;\
  };\
  cnt = px2x(zero_x);skip = 1;\
  for( var p = zero_x ; p > 0 ; p = p-xstep){\
   if(skip == 0 ){\
    disp_cnt = cnt.toFixed(prec);\
    corr = ctx.measureText(disp_cnt).width;\
    skip = parseInt(1.2*corr/xstep);\
    ctx.fillText(disp_cnt,p-0.5*corr,y_basis);\
   } else {\
    skip--;\
   };\
   cnt = cnt - xmajor;\
  };\
 };\
 if( y_strings != null ){\
  var len = y_strings.length;if((len/2+0.5)%%2 == 0){ alert(\"yaxis number unpaired:  text missing ! \");return;};\
  ctx.beginPath();\
  for(var p = 0 ; p < len ; p = p+2){\
   var y_nums = y2px(eval(y_strings[p]));\
   var y_text = y_strings[p+1];\
   corr = 2 + tics_length + ctx.measureText(y_text).width;\
   if( corr > zero_x){corr = parseInt(zero_x+2); }\
   ctx.fillText(y_text,zero_x - corr,y_nums + 0.5*font_size);\
   ctx.moveTo(zero_x - tics_length,y_nums);\
   ctx.lineTo(zero_x + tics_length,y_nums);\
  };\
  ctx.closePath();\
 } else {\
  if(f_x == 1){ corr = 1.5*tics_length; }\
  cnt = px2y(zero_y);skip = 1;\
  for( var p = zero_y ; p < ysize ; p = p+ystep){\
   if(skip == 0 ){\
    skip = parseInt(1.4*font_size/ystep);\
    disp_cnt = cnt.toFixed(prec);\
    if(f_x == -1 ){ corr = parseInt(zero_x - (2 + tics_length + ctx.measureText(disp_cnt).width));};\
    ctx.fillText(disp_cnt,parseInt(corr),parseInt(p+(0.4*font_size)));\
   } else {\
    skip--;\
   };\
   cnt = cnt - ymajor;\
  };\
  corr = 0;cnt = px2y(zero_y);skip = 1;\
  if(f_x == 1){ corr = 1.5*tics_length; }\
  for( var p = zero_y ; p > 0 ; p = p-ystep){\
   if(skip == 0 ){\
    skip = parseInt(1.4*font_size/ystep);\
    disp_cnt = cnt.toFixed(prec);\
    if(f_x == -1 ){corr = parseInt(zero_x - (2 + tics_length + ctx.measureText(disp_cnt).width));};\
    ctx.fillText(disp_cnt,parseInt(corr),parseInt(p+(0.4*font_size)));\
   } else {\
    skip--;\
   };\
   cnt = cnt + ymajor;\
  };\
 };\
 ctx.stroke();\
 ctx.restore();");
        }
        if( legend_cnt != -1 ){
          fprintf(js_include_file,"ctx.save();\
 ctx.globalAlpha = 1.0;\
 ctx.font = \"bold \"+font_size+\"px Arial\";\
 var y_offset = 2*font_size;\
 var txt;var txt_size;\
 var x_offset = xsize - 2*font_size;\
 var l_length = legend0.length;\
 if( typeof(legendcolors0) !== 'undefined' ){\
  for(var p = 0 ; p < l_length ; p++){\
    barcolor[p] = legendcolors0[p];\
  };\
 } else {\
  if( barcolor.length == 0 ){\
   for(var p = 0 ; p < l_length ; p++){\
    barcolor[p] = stroke_color;\
   };\
  };\
 };\
 for(var p = 0; p < l_length; p++){\
  ctx.fillStyle = barcolor[p];\
  txt = legend0[p];\
  txt_size = ctx.measureText(txt).width;\
  ctx.fillText(legend0[p],x_offset - txt_size, y_offset);\
  y_offset = parseInt(y_offset + 1.5*font_size);\
 };\
 ctx.restore();");
        }
        if( barchart_cnt != 0 ){
          fprintf(js_include_file,"ctx.save();\
 var num_barcharts = 0;\
 var bar_name = eval('barchart_0');\
 while( typeof(bar_name) !== 'undefined' ){\
    try{ bar_name = eval('barchart_'+num_barcharts);num_barcharts++;}catch(e){break;};\
 };\
 var bar_width = parseInt(0.8*x2step/(num_barcharts));\
 for(var i=0 ; i< num_barcharts ; i++){\
  bar_name = eval('barchart_'+i);\
  var bar_x = new Array();\
  var bar_y = new Array();\
  var lb = bar_name.length;\
  var idx = 0;\
  var dx = parseInt(0.5*i*bar_width);\
  for( var p = 0 ; p < lb ; p = p + 3 ){\
   bar_x[idx] = x2px(bar_name[p]);\
   bar_y[idx] = y2px(bar_name[p+1]);\
   barcolor[idx] = bar_name[p+2];\
   idx++;\
  };\
  ctx.globalAlpha = fill_opacity;\
  for( var p = 0; p < idx ; p++ ){\
   ctx.beginPath();\
   ctx.strokeStyle = barcolor[p];\
   ctx.fillStyle = barcolor[p];\
   ctx.rect(bar_x[p]-0.4*x2step+dx,bar_y[p],bar_width,zero_y - bar_y[p]);\
   ctx.fill();\
   ctx.stroke();\
   ctx.closePath();\
  };\
 };\
 ctx.restore();");
        }
        if( linegraph_cnt != 0 ){
          fprintf(js_include_file,"ctx.save();\
 ctx.globalAlpha = 1.0;\
 var i = 0;\
 var line_name = eval('linegraph_'+i);\
 while ( typeof(line_name) !== 'undefined' ){\
  ctx.strokeStyle = 'rgba('+line_name[0]+','+stroke_opacity+')';\
  ctx.lineWidth = parseInt(line_name[1]);\
  if(line_name[2] == \"1\"){\
   var d1 = parseInt(line_name[3]);\
   var d2 = parseInt(line_name[4]);\
   if(ctx.setLineDash){ ctx.setLineDash([d1,d2]); } else { ctx.mozDash = [d1,d2];};\
  } else {\
  if(ctx.setLineDash){ctx.setLineDash = null;};if(ctx.mozDash){ctx.mozDash = null;}\
  };\
  var data_x = new Array();var data_y = new Array();\
  var lb = line_name.length;var idx = 0;\
  for( var p = 5 ; p < lb ; p = p + 2 ){\
   data_x[idx] = x2px(line_name[p]);\
   data_y[idx] = y2px(line_name[p+1]);\
   idx++;\
  };\
  for( var p = 0; p < idx ; p++){\
   ctx.beginPath();\
   ctx.moveTo(data_x[p],data_y[p]);\
   ctx.lineTo(data_x[p+1],data_y[p+1]);\
   ctx.stroke();\
   ctx.closePath();\
  };\
  i++;\
  try{ line_name = eval('linegraph_'+i); }catch(e){ break; }\
 };\
 ctx.restore();return;");
        }
        fprintf(js_include_file,"};");
        break;
      case DRAW_PIECHART:
        fprintf(js_include_file,"\n/* draw piecharts */\
if( typeof(all_fill_patterns) != 'object' ){ var all_fill_patterns = []; };\
function draw_piechart(canvas_type,x_center,y_center,radius, data_color_list,fill_opacity,legend_cnt,font_family,use_filled,use_offset){\
 var obj;\
 if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){\
  obj = document.getElementById(\"wims_canvas%d\"+canvas_type);\
 } else {\
  obj = create_canvas%d(canvas_type,xsize,ysize);\
 };\
 var center_text = use_offset || 0;\
 var ld = data_color_list.length;\
 var sum = 0;\
 var idx = 0;\
 var font_size = parseInt(font_family.replace(/[^0-9\\.]+/g, \"\"));\
 var colors = new Array();\
 var data = new Array();\
 for(var p = 0;p < ld; p = p + 2){\
  data[idx] = parseFloat(data_color_list[p]);\
  sum = sum + data[idx];\
  colors[idx] = data_color_list[p+1];\
  idx++;\
 };\
 if( use_filled > 1 ){\
  var i = 2;\
  for(var p = 0 ;  p < idx ; p++){\
   if(i > 5 ){ i = 2; };\
   var pat = create_Pattern(0,0,i,colors[p]); all_fill_patterns[p] = pat;i++;\
  };\
 };\
 var ctx = obj.getContext(\"2d\");\
 ctx.save();\
 var angle;\
 var angle_end = 0;\
 var offset = Math.PI / 2;\
 ctx.globalAlpha = fill_opacity;\
 var angles = [];\
 for(var p=0; p < idx; p++){\
  ctx.beginPath();\
  ctx.moveTo(x_center,y_center);\
  angle = Math.PI * (2 * data[p] / sum);\
  ctx.arc(x_center,y_center, radius, angle_end - offset, angle_end + angle - offset, false);\
  ctx.lineTo(x_center, y_center);\
  if( use_filled > 1 ){ ctx.fillStyle = all_fill_patterns[p]; }else{ ctx.fillStyle = colors[p];};\
  ctx.fill();\
  ctx.closePath();\
  angles.push(angle_end + angle - offset);\
  angle_end  = angle_end + angle;\
 };\
 if(typeof(legend0) !== 'undefined'){\
  var legenda = eval(\"legend\"+legend_cnt);\
  ctx.globalAlpha = 1.0;\
  ctx.font = font_family;\
  var y_offset = font_size;\
  var x_offset = 0;\
  var txt;var txt_size;\
  for(var p = 0; p < idx; p++){\
   ctx.fillStyle = colors[p];\
   txt = legenda[p];\
   txt_size = ctx.measureText(txt).width + 2;\
   if(center_text == 4){\
    ctx.save();\
    ctx.translate(x_center, y_center);\
    ctx.rotate(angles[p]);\
    ctx.fillText(txt,radius-txt_size,0);\
    ctx.restore();\
   } else {\
    if( x_center + radius + txt_size > xsize ){ x_offset =  x_center + radius + txt_size - xsize;} else { x_offset = 0; };\
    ctx.fillText(txt,x_center + radius - x_offset, y_center - radius + y_offset);\
    y_offset = parseInt(y_offset + 1.5*font_size);\
   };\
  };\
 };\
 ctx.restore();\
};",canvas_root_id,canvas_root_id,canvas_root_id);
        break;
      case DRAW_JSBOXPLOT:
        fprintf(js_include_file,"\n/* draw jsboxplots */\
if( typeof(all_fill_patterns) != 'object' ){ var all_fill_patterns = []; };\
function statistics(data){\
 var len = data.length;\
 var min = 10000000;\
 var max = -10000000;\
 var sum = 0;var d;\
 for(var i=0;i<len;i++){\
  d = data[i];\
  if(d < min){min = d;}else{if(d > max){max = d;};};\
  sum+= parseFloat(data[i]);\
 };\
 var mean = parseFloat(sum/len);\
 var variance = 0;\
 for(var i=0;i<len;i++){\
  d = data[i];\
  variance += (d - mean)*(d - mean);\
 };\
 variance = parseFloat(variance / len);\
 var std = Math.sqrt(variance);\
 data.sort(function(a,b){return a - b;});\
 var median;var Q1;var Q3;\
 var half = Math.floor(0.5*len);\
 var q1 = Math.floor(0.25*len);\
 var q3 = Math.floor(0.75*len);\
 var half = Math.floor(0.5*len);\
 if(len %%2 == 1){\
  median = data[half];\
  Q1 = data[q1];\
  Q3 = data[q3];\
 } else {\
  median = (data[half - 1] + data[half] )/2;\
  Q1 = (data[q1 - 1] + data[q1] )/2;\
  Q3 = (data[q3 - 1] + data[q3] )/2;\
 };\
 return [min,Q1,median,Q3,max];\
};");
        break;
      case DRAW_BOXPLOT:
        fprintf(js_include_file,"\n/* draw boxplots */\
if( typeof(all_fill_patterns) != 'object' ){ var all_fill_patterns = []; };\
draw_boxplot = function(canvas_type,xy,hw,cxy,data,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype0,dashtype1){\
 var obj;\
 if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){obj = document.getElementById(\"wims_canvas%d\"+canvas_type);}else{obj = create_canvas%d(canvas_type,xsize,ysize);};\
 var ctx = obj.getContext(\"2d\");\
 ctx.clearRect(0,0,xsize,ysize);\
 ctx.save();\
 ctx.lineWidth = line_width;\
 ctx.strokeStyle =  \"rgba(\"+stroke_color+\",\"+stroke_opacity+\")\";\
 var colors = new Array(2);\
 colors[0] = \"rgba(\"+fill_color+\",\"+fill_opacity+\")\";\
 colors[1] = \"rgba(\"+stroke_color+\",\"+stroke_opacity+\")\";\
 if( use_filled > 1 ){\
   var pat = create_Pattern(0,0,3,colors[0]);\
   all_fill_patterns[0] = pat;\
   pat = create_Pattern(0,0,4,colors[1]);\
   all_fill_patterns[1] = pat;\
 };\
 if(use_dashed == 1){if(ctx.setLineDash){ctx.setLineDash([dashtype0,dashtype1]);}else{if(ctx.mozDash){ ctx.mozDash = [dashtype0,dashtype1];};};};\
 var hh = 0.25*hw;\
 switch(boxplot_source){\
  case 1: if( typeof(jsboxplot_data) === 'undefined'){return;};data = statistics(jsboxplot_data);break;\
  case 2: if( typeof(student_boxplot_data) === 'undefined'){return;};data = statistics(student_boxplot_data);break;\
  case 3: if( typeof(student_boxplot) === 'undefined'){return;};data = student_boxplot;break;\
  default: break;\
 };\
 var min,Q1,median,Q3,max;\
 if(xy == 1 ){\
  min=x2px(data[0]);Q1=x2px(data[1]);median=x2px(data[2]);Q3=x2px(data[3]);max=x2px(data[4]);\
  hh = Math.abs(y2px(hh) - y2px(ystart));\
  hw = Math.abs(y2px(hw) - y2px(ystart));\
  cxy = y2px(cxy);\
  ctx.beginPath();\
  ctx.moveTo(min,cxy);\
  ctx.lineTo(Q1,cxy);\
  ctx.moveTo(Q3,cxy);\
  ctx.lineTo(max,cxy);\
  ctx.moveTo(min,cxy+hh);\
  ctx.lineTo(min,cxy-hh);\
  ctx.moveTo(max,cxy+hh);\
  ctx.lineTo(max,cxy-hh);\
  ctx.closePath();\
  ctx.stroke();\
  ctx.beginPath();\
  ctx.rect(Q1,cxy-2*hh,median-Q1,hw);\
  ctx.closePath();\
  if( use_filled != 0 ){\
   if( use_filled == 1 ) {ctx.fillStyle = colors[0]; }else{ ctx.fillStyle = all_fill_patterns[0] };\
   ctx.fill();\
  };\
  ctx.stroke();\
  ctx.beginPath();\
  ctx.rect(median,cxy-2*hh,Q3-median,hw);\
  ctx.closePath();\
  if( use_filled != 0 ){\
   if( use_filled == 1 ) {ctx.fillStyle = colors[1]; }else{ ctx.fillStyle = all_fill_patterns[1] };\
   ctx.fill();\
  };\
  ctx.stroke();\
 }else{\
  min=y2px(data[0]);Q1=y2px(data[1]);median=y2px(data[2]);Q3=y2px(data[3]);max=y2px(data[4]);\
  hh = Math.abs(x2px(hh) - x2px(xstart));\
  hw = Math.abs(x2px(hw) - x2px(xstart));\
  cxy = x2px(cxy);\
  ctx.beginPath();\
  ctx.moveTo(cxy,min);\
  ctx.lineTo(cxy,Q1);\
  ctx.moveTo(cxy,Q3);\
  ctx.lineTo(cxy,max);\
  ctx.moveTo(cxy + hh,min);\
  ctx.lineTo(cxy - hh,min);\
  ctx.moveTo(cxy + hh,max);\
  ctx.lineTo(cxy - hh,max);\
  ctx.closePath;\
  ctx.stroke();\
  ctx.beginPath();\
  ctx.rect(cxy - 2*hh,Q1,hw,median - Q1);\
  ctx.closePath();\
  if( use_filled != 0 ){\
   if( use_filled == 1 ) {ctx.fillStyle = colors[0]; }else{ ctx.fillStyle = all_fill_patterns[0] };\
   ctx.fill();\
  };\
  ctx.stroke();\
  ctx.beginPath();\
  ctx.rect(cxy - 2*hh,median,hw,Q3 - median);\
  ctx.closePath();\
  if( use_filled != 0 ){\
   if( use_filled == 1 ) {ctx.fillStyle = colors[1]; }else{ ctx.fillStyle = all_fill_patterns[1] };\
   ctx.fill();\
  };\
  ctx.stroke();\
 };\
 ctx.restore();};",canvas_root_id,canvas_root_id,canvas_root_id);
        break;
      case DRAW_CENTERSTRING:
        fprintf(js_include_file,"\n/* draw centerstring */\
var draw_centerstring = function(canvas_type,y,font_family,stroke_color,stroke_opacity,text){\
 var obj;\
 if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){\
  obj = document.getElementById(\"wims_canvas%d\"+canvas_type);\
 } else {\
  obj = create_canvas%d(canvas_type,xsize,ysize);\
 };\
 var ctx = obj.getContext(\"2d\");\
 ctx.save();\
 ctx.clearRect(0,0,xsize,ysize);\
 ctx.font = font_family;\
 ctx.fillStyle = \"rgba(\"+stroke_color+\",\"+stroke_opacity+\")\";\
 var stringwidth = ctx.measureText(text).width;\
 var x = parseInt((xsize - stringwidth)/2);if( x < 0 ){x = 0;};\
 ctx.fillText(text,x,y2px(y));\
 ctx.restore();\
return;\
};",canvas_root_id,canvas_root_id,canvas_root_id);
        break;
      case DRAW_TEXTS:
        fprintf(js_include_file,"\n/* draw text */\
var draw_text = function(canvas_type,x,y,font_size,font_family,stroke_color,stroke_opacity,angle2,text,use_rotate,angle,use_offset){\
 var obj;\
 if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){\
  obj = document.getElementById(\"wims_canvas%d\"+canvas_type);\
 } else {\
  obj = create_canvas%d(canvas_type,xsize,ysize);\
 };\
 var ctx = obj.getContext(\"2d\");\
 if( font_family != 'null' ){\
  ctx.font = font_family;\
 } else {\
  ctx.font = font_size+'px Arial';\
 };\
 if( use_offset == 3 ){if(angle2 < 0 ){ y = y + 0.8*font_size; x = x + (Math.cos(angle2))*font_size; }else{y = y - 0.8*font_size; x = x + (Math.sin(angle2))*font_size;};};\
 if(angle2 == 0 && angle != 0){\
  ctx.save();\
  if(use_rotate == 1 ){\
  ctx.rotate(angle*Math.PI/180);};\
  ctx.restore();\
 };\
 ctx.fillStyle = \"rgba(\"+stroke_color+\",\"+stroke_opacity+\")\";\
 if(angle2 != 0){\
  ctx.save();\
  ctx.translate(x,y);\
  ctx.rotate((360-angle2)*(Math.PI / 180));\
  ctx.fillText(text,0,0);\
  ctx.restore();\
 } else {\
  ctx.fillText(text,x,y);\
 };\
 return;\
};",canvas_root_id,canvas_root_id,canvas_root_id);
        break;
      case DRAW_INPUTS:
/* function center_input may be used for other centering...if not? integrate it in draw_static_inputs...*/
        fprintf(js_include_file,"\n/* draw static input fields */\
if(typeof(input_cnt) !== 'undefined' ){var input_cnt = 0;};\
var draw_static_inputs = function(root_id,num,x,y,size,readonly,css_class,value,use_offset){\
var input = document.createElement(\"input\");\
input.setAttribute(\"id\",\"canvas_input\"+num);\
input.setAttribute(\"style\",\"text-align:center;position:absolute;left:\"+x+\"px;top:\"+y+\"px;\");\
if(css_class != \"none\"){input.setAttribute(\"class\",css_class);};\
input.setAttribute(\"size\",size);\
input.setAttribute(\"value\",value);\
input.addEventListener(\"touchstart\", function(e){this.focus();},false);\
if( readonly == 0 || wims_status == \"done\" ){ input.setAttribute(\"readonly\",\"readonly\");if( wims_status == \"done\" ){input.setAttribute(\"value\",\"\");};};\
canvas_div.appendChild(input);\
if(use_offset != 0 ){ center_input('canvas_input'+num,x,y,css_class);};\
input_cnt++;\
};\
function center_input(id,x,y,css_class){\
 var inp = document.getElementById(id);\
 var pos = inp.getBoundingClientRect();\
 var center_x = parseInt(x - 0.5*(pos.width));\
 var center_y = parseInt(y - 0.5*(pos.height));\
 try{if(css_class != \"none\"){inp.setAttribute(\"class\",css_class);};inp.setAttribute(\"style\",\"text-align:center;position:absolute;left:\"+center_x+\"px;top:\"+center_y+\"px;\");}\
 catch(e){console.log('error :'+e);return;};\
};");
        break;
      case DRAW_TEXTAREAS:
        fprintf(js_include_file,"\n/* draw text area inputfields */\
var draw_textareas = function(root_id,num,x,y,cols,rows,readonly,style,value){\
var canvas_div = document.getElementById(\"canvas_div\"+root_id);\
var textarea = document.createElement(\"textarea\");\
textarea.setAttribute(\"id\",\"canvas_input\"+num);\
textarea.setAttribute(\"style\",\"position:absolute;left:\"+x+\"px;top:\"+y+\"px;\"+style);\
textarea.setAttribute(\"cols\",cols);\
textarea.setAttribute(\"rows\",rows);\
textarea.value = value;\
if( readonly == 0 || wims_status == \"done\" ){ textarea.setAttribute(\"readonly\",\"readonly\");if( wims_status == \"done\" ){textarea.value=\"\";};};\
canvas_div.appendChild(textarea);};");
        break;
      case DRAW_PIXELS:
        fprintf(js_include_file,"\n/* draw pixel */\
var draw_setpixel = function(x,y,color,opacity,pixelsize){\
 var idx = 2000+Math.ceil(1000*(Math.random()));\
 var canvas = create_canvas%d(idx,xsize,ysize);\
 var d = 0.5*pixelsize;\
 var ctx = canvas.getContext(\"2d\");\
 if(pixelsize%%2 == 1){ ctx.translate(0.5,0.5);};\
 ctx.fillStyle = \"rgba(\"+color+\",\"+opacity+\")\";\
 ctx.clearRect(0,0,xsize,ysize);\
 for(var p=0; p<x.length;p++){\
  ctx.fillRect( x2px(x[p]) - d, y2px(y[p]) - d , pixelsize, pixelsize );\
 };\
 ctx.fill();ctx.stroke();\
};",canvas_root_id);
        break;
      case DRAW_CLOCK:
        fprintf(js_include_file,"\n/* command clock */\
var clock_canvas = create_canvas%d(%d,xsize,ysize);\
var clock_ctx = clock_canvas.getContext(\"2d\");\
var clock = function(xc,yc,radius,H,M,S,type,interaction,h_color,m_color,s_color,bg_color,fg_color){\
 clock_ctx.clearRect(xc - radius,yc - radius,2*radius,2*radius);\
 clock_ctx.save();\
 clock_ctx.globalAlpha = clock_bg_opacity;\
 this.type = type || 0;\
 this.interaction = interaction || 0;\
 this.H = H;\
 this.M = M;\
 this.S = S;\
 this.xc = xc || xsize/2;\
 this.yc = yc || ysize/2;\
 this.radius = radius || xsize/4;\
 var font_size = parseInt(0.2*this.radius);\
 this.H_color = h_color || \"black\";\
 this.M_color = m_color || \"black\";\
 this.S_color = s_color || \"black\";\
 this.fg_color = fg_color || \"black\";\
 this.bg_color = bg_color || \"white\";\
 clock_ctx.translate(this.xc,this.yc);\
 clock_ctx.beginPath();\
 clock_ctx.arc(0,0,this.radius,0,2*Math.PI,false);\
 clock_ctx.fillStyle = this.bg_color;\
 clock_ctx.fill();\
 clock_ctx.closePath();\
 clock_ctx.beginPath();\
 clock_ctx.font = font_size+\"px Arial\";\
 clock_ctx.fillStyle = this.fg_color;\
 clock_ctx.textAlign = \"center\";\
 clock_ctx.textBaseline = 'middle';\
 var angle;var x1,y1,x2,y2;\
 var angle_cos;var angle_sin;\
 clock_ctx.globalAlpha = clock_fg_opacity;\
 switch(type){\
 case 0:clock_ctx.beginPath();\
 for(var p = 1; p <= 12 ; p++){\
  angle_cos = this.radius*(Math.cos(p * (Math.PI * 2) / 12));\
  angle_sin = this.radius*(Math.sin(p * (Math.PI * 2) / 12));\
  x1 = 0.8*angle_cos;y1 = 0.8*angle_sin;x2 = angle_cos;y2 = angle_sin;\
  clock_ctx.moveTo(x1,y1);\
  clock_ctx.lineTo(x2,y2);\
 };\
 for(var p = 1; p <= 60 ; p++){\
  angle_cos = this.radius*(Math.cos(p * (Math.PI * 2) / 60));\
  angle_sin = this.radius*(Math.sin(p * (Math.PI * 2) / 60));\
  x1 = 0.9*angle_cos;y1 = 0.9*angle_sin;x2 = angle_cos;y2 = angle_sin;\
  clock_ctx.moveTo(x1,y1);\
  clock_ctx.lineTo(x2,y2);\
 };\
 clock_ctx.closePath();\
 clock_ctx.stroke();\
 break;\
 case 1:\
 for(var p= 1; p <= 12 ; p++){ angle = (p - 3) * (Math.PI * 2) / 12;x1 = 0.9*this.radius*Math.cos(angle);y1 = 0.9*this.radius*Math.sin(angle);clock_ctx.fillText(p, x1, y1);};break;\
 case 2:\
 for(var p= 1; p <= 12 ; p++){ angle = (p - 3) * (Math.PI * 2) / 12;x1 = 0.8*this.radius*Math.cos(angle);y1 = 0.8*this.radius*Math.sin(angle);clock_ctx.fillText(p, x1, y1);};\
 clock_ctx.beginPath();\
 for(var p = 1; p <= 12 ; p++){\
  angle_cos = this.radius*(Math.cos(p * (Math.PI * 2) / 12));\
  angle_sin = this.radius*(Math.sin(p * (Math.PI * 2) / 12));\
  x1 = 0.9*angle_cos;y1 = 0.9*angle_sin;x2 = angle_cos;y2 = angle_sin;\
  clock_ctx.moveTo(x1,y1);\
  clock_ctx.lineTo(x2,y2);\
 };\
 for(var p = 1; p <= 60 ; p++){\
  angle_cos = this.radius*(Math.cos(p * (Math.PI * 2) / 60));\
  angle_sin = this.radius*(Math.sin(p * (Math.PI * 2) / 60));\
  x1 = 0.95*angle_cos;y1 = 0.95*angle_sin;x2 = angle_cos;y2 = angle_sin;\
  clock_ctx.moveTo(x1,y1);\
  clock_ctx.lineTo(x2,y2);\
 };\
 clock_ctx.closePath();\
 clock_ctx.stroke();\
 break;\
 };\
 angle = (this.H - 3 + this.M/60 ) * 2 * Math.PI / 12;\
 clock_ctx.rotate(angle);\
 clock_ctx.beginPath();\
 clock_ctx.moveTo(-3, -2);\
 clock_ctx.lineTo(-3, 2);\
 clock_ctx.lineTo(this.radius * 0.6, 1);\
 clock_ctx.lineTo(this.radius  * 0.6, -1);\
 clock_ctx.fillStyle = this.H_color;\
 clock_ctx.fill();\
 clock_ctx.rotate(-angle);\
 angle = (this.M - 15 + this.S/60) * 2 * Math.PI / 60;\
 clock_ctx.rotate(angle);\
 clock_ctx.beginPath();\
 clock_ctx.moveTo(-3, -2);\
 clock_ctx.lineTo(-3, 2);\
 clock_ctx.lineTo(this.radius  * 0.8, 1);\
 clock_ctx.lineTo(this.radius  * 0.8, -1);\
 clock_ctx.fillStyle = this.M_color;\
 clock_ctx.fill();\
 clock_ctx.rotate(-angle);\
 angle = (this.S - 15) * 2 * Math.PI / 60;\
 clock_ctx.rotate(angle);\
 clock_ctx.beginPath();\
 clock_ctx.moveTo(0,0);\
 clock_ctx.lineTo(this.radius  * 0.9, 1);\
 clock_ctx.lineTo(this.radius  * 0.9, -1);\
 clock_ctx.strokeStyle = this.S_color;\
 clock_ctx.stroke();\
 clock_ctx.restore();\
};",canvas_root_id,CLOCK_CANVAS);
        break;
      case DRAW_LATTICE:
        fprintf(js_include_file,"\n/* draw lattice */\
var draw_lattice = function(canvas_type,line_width,x0,y0,dx1,dy1,dx2,dy2,n1,n2,fill_color,fill_opacity,stroke_color,stroke_opacity,use_rotate,angle,use_filled){\
 var obj;\
 if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){\
  obj = document.getElementById(\"wims_canvas%d\"+canvas_type);\
 } else {\
  obj = create_canvas%d(canvas_type,xsize,ysize);\
 };\
 var ctx = obj.getContext(\"2d\");\
 ctx.save();\
 if(use_rotate == 1 ){ctx.rotate(angle*Math.PI/180);}\
 var color = \"rgba(\"+fill_color+\",\"+fill_opacity+\")\";\
 if(use_filled > 1 ){ if(! all_fill_patterns[use_filled] ){ var pat = create_Pattern(0,0,use_filled,color); all_fill_patterns[use_filled] = pat;};ctx.fillStyle = all_fill_patterns[use_filled]; } else { ctx.fillStyle = color;};\
 ctx.strokeStyle=\"rgba(\"+stroke_color+\",\"+stroke_opacity+\")\";\
 var radius = line_width;\
 var x = 0;\
 var y = 0;\
 var x_step_px = xsize/(xmax-xmin);\
 var y_step_px = ysize/(ymax-ymin);\
 var xv1 = dx1*x_step_px;\
 var yv1 = dy1*y_step_px;\
 var xv2 = dx2*x_step_px;\
 var yv2 = dy2*y_step_px;\
 for(var p = 0; p < n1 ;p++){\
  x = p*xv1 + x0;\
  y = p*yv1 + y0;\
  for(var c = 0; c < n2 ; c++){\
   ctx.beginPath();\
   ctx.arc(x+c*xv2,y+c*yv2,radius,0,2*Math.PI,false);\
   ctx.fill();\
   ctx.stroke();\
   ctx.closePath();\
  };\
 };\
 ctx.restore();\
 return;\
};",canvas_root_id,canvas_root_id,canvas_root_id);
        break;
      case DRAW_XYLOGSCALE:
        fprintf(js_include_file,"\n/* draw xylogscale */\
var draw_grid%d = function(canvas_type,line_width,major_color,minor_color,major_opacity,minor_opacity,font_size,font_family,font_color,use_axis_numbering,precision){\
 var obj;\
 if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){\
  obj = document.getElementById(\"wims_canvas%d\"+canvas_type);\
 } else {\
  obj = create_canvas%d(canvas_type,xsize,ysize);\
 };\
 var ctx = obj.getContext(\"2d\");\
 ctx.clearRect(0,0,xsize,ysize);\
 ctx.save();\
 var xmarge;var ymarge;var x_e;var y_e;var num;var corr;var xtxt;var ytxt;\
 var x_min = Math.log(xmin)/Math.log(xlogbase);\
 var x_max = Math.log(xmax)/Math.log(xlogbase);\
 var y_min = Math.log(ymin)/Math.log(ylogbase);\
 var y_max = Math.log(ymax)/Math.log(ylogbase);\
 if(use_axis_numbering != -1){\
  ctx.font = font_family;\
  xmarge = ctx.measureText(ylogbase+'^'+y_max.toFixed(0)+' ').width;\
  ymarge = parseInt(1.5*font_size);\
  ctx.save();\
  ctx.fillStyle=\"rgba(255,215,0,0.2)\";\
  ctx.rect(0,0,xmarge,ysize);\
  ctx.rect(0,ysize-ymarge,xsize,ysize);\
  ctx.fill();\
  ctx.restore();\
 }else{xmarge = 0;ymarge = 0;};\
 if( typeof(xaxislabel) !== 'undefined' ){\
  ctx.save();\
  ctx.font = \"italic \"+font_size+\"px Arial\";\
  ctx.fillStyle = \"rgba(\"+font_color+\",\"+major_opacity+\")\";\
  corr =  ctx.measureText(xaxislabel).width;\
  ctx.fillText(xaxislabel,xsize - 1.5*corr,ysize - 2*font_size);\
  ctx.restore();\
 };\
 if( typeof(yaxislabel) !== 'undefined' ){\
  ctx.save();\
  ctx.font = \"italic \"+font_size+\"px Arial\";\
  ctx.fillStyle = \"rgba(\"+font_color+\",\"+major_opacity+\")\";\
  corr = ctx.measureText(yaxislabel).width;\
  ctx.translate(xmarge+font_size,corr+font_size);\
  ctx.rotate(-0.5*Math.PI);\
  ctx.fillText(yaxislabel,0,0);\
  ctx.restore();\
 };\
 ctx.fillStyle = \"rgba(\"+font_color+\",\"+major_opacity+\")\";\
 ctx.lineWidth = line_width;\
 for(var p = x_min; p <= x_max ; p++){\
  num = Math.pow(xlogbase,p);\
  for(var i = 1 ; i < xlogbase ; i++){\
   x_e = x2px(i*num);\
   if( i == 1 ){\
    ctx.lineWidth = line_width;\
    ctx.strokeStyle=\"rgba(\"+major_color+\",\"+major_opacity+\")\";\
    if( use_axis_numbering != -1 && p > x_min){\
      xtxt = xlogbase+'^'+p.toFixed(0);\
      corr = 0.5*(ctx.measureText(xtxt).width);\
      ctx.fillText(xtxt,x_e - corr,ysize - 4);\
    };\
   }else{\
    ctx.lineWidth = 0.2*line_width;\
    ctx.strokeStyle=\"rgba(\"+minor_color+\",\"+minor_opacity+\")\";\
   };\
   if( x_e >= xmarge ){\
    ctx.beginPath();\
    ctx.moveTo(x_e,0);\
    ctx.lineTo(x_e,ysize - ymarge);\
    ctx.stroke();\
    ctx.closePath();\
   };\
  };\
 };\
 for(var p = y_min; p <= y_max ; p++){\
  num = Math.pow(ylogbase,p);\
  for(var i = 1 ; i < ylogbase ; i++){\
   y_e = y2px(i*num);\
   if( i == 1 ){\
    ctx.lineWidth = line_width;\
    ctx.strokeStyle=\"rgba(\"+major_color+\",\"+major_opacity+\")\";\
    if( use_axis_numbering != -1 && p > y_min){\
     ctx.fillText(ylogbase+'^'+p.toFixed(0),0,y_e);\
    };\
   }else{\
    ctx.lineWidth = 0.2*line_width;\
    ctx.strokeStyle=\"rgba(\"+minor_color+\",\"+minor_opacity+\")\";\
   };\
   ctx.beginPath();\
   ctx.moveTo(xmarge,y_e);\
   ctx.lineTo(xsize,y_e);\
   ctx.stroke();\
   ctx.closePath();\
  };\
 };\
 ctx.restore();\
};",canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id);
        break;
      case DRAW_XLOGSCALE:
        fprintf(js_include_file,"\n/* draw xlogscale */\
var draw_grid%d = function(canvas_type,line_width,major_color,minor_color,major_opacity,minor_opacity,font_size,font_family,font_color,use_axis_numbering,ymajor,yminor,precision){\
 var obj;\
 if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){\
  obj = document.getElementById(\"wims_canvas%d\"+canvas_type);\
 } else {\
  obj = create_canvas%d(canvas_type,xsize,ysize);\
 };\
 var ctx = obj.getContext(\"2d\");\
 ctx.clearRect(0,0,xsize,ysize);\
 ctx.save();\
 ctx.lineWidth = line_width;\
 var prec = Math.log(precision)/Math.log(10);\
 var x_min = Math.log(xmin)/Math.log(xlogbase);\
 var x_max = Math.log(xmax)/Math.log(xlogbase);\
 var y_min = 0;var y_max = ysize;var x_e;var corr;\
 var xtxt;var ytxt;var num;var xmarge;var ymarge;\
 if(use_axis_numbering != -1){\
  ctx.font = font_family;\
  xmarge = ctx.measureText(ymax.toFixed(prec)+' ').width;\
  ymarge = parseInt(1.5*font_size);\
  ctx.save();\
  ctx.fillStyle=\"rgba(255,215,0,0.2)\";\
  ctx.rect(0,0,xmarge,ysize);\
  ctx.rect(0,ysize-ymarge,xsize,ysize);\
  ctx.fill();\
  ctx.restore();\
 }else{xmarge = 0;ymarge = 0;};\
 if( typeof(xaxislabel) !== 'undefined' ){\
  ctx.save();\
  ctx.font = \"italic \"+font_size+\"px Arial\";\
  ctx.fillStyle = \"rgba(\"+font_color+\",\"+major_opacity+\")\";\
  corr =  ctx.measureText(xaxislabel).width;\
  ctx.fillText(xaxislabel,xsize - 1.5*corr,ysize - 2*font_size);\
  ctx.restore();\
 };\
 if( typeof(yaxislabel) !== 'undefined' ){\
  ctx.save();\
  ctx.font = \"italic \"+font_size+\"px Arial\";\
  ctx.fillStyle = \"rgba(\"+font_color+\",\"+major_opacity+\")\";\
  corr = ctx.measureText(yaxislabel).width;\
  ctx.translate(xmarge+font_size,corr+font_size);\
  ctx.rotate(-0.5*Math.PI);\
  ctx.fillText(yaxislabel,0,0);\
  ctx.restore();\
 };\
 ctx.fillStyle = \"rgba(\"+font_color+\",\"+major_opacity+\")\";\
 ctx.lineWidth = line_width;\
 for(var p = x_min; p <= x_max ; p++){\
  num = Math.pow(xlogbase,p);\
  for(var i = 1 ; i < xlogbase ; i++){\
   x_e = x2px(i*num);\
   if( i == 1 ){\
     ctx.lineWidth = line_width;\
     ctx.strokeStyle=\"rgba(\"+major_color+\",\"+major_opacity+\")\";\
    if( use_axis_numbering != -1 && p > x_min ){\
      xtxt = xlogbase+'^'+p.toFixed(0);\
      corr = 0.5*(ctx.measureText(xtxt).width);\
      ctx.fillText(xtxt,x_e - corr,ysize - 4);\
    };\
   }else{\
    ctx.lineWidth = 0.2*line_width;\
    ctx.strokeStyle=\"rgba(\"+minor_color+\",\"+minor_opacity+\")\";\
   };\
   if( x_e >= xmarge ){\
    ctx.beginPath();\
    ctx.moveTo(x_e,0);\
    ctx.lineTo(x_e,ysize - ymarge);\
    ctx.stroke();\
    ctx.closePath();\
   };\
  };\
 };\
 var stepy = Math.abs(y2px(ymajor) - y2px(0));\
 var minor_step = stepy / yminor;\
 for(var y = 0 ; y < ysize - stepy ; y = y + stepy){\
  ctx.strokeStyle=\"rgba(\"+major_color+\",\"+major_opacity+\")\";\
  ctx.lineWidth = line_width;\
  ctx.beginPath();\
  ctx.moveTo(xmarge,y);\
  ctx.lineTo(xsize,y);\
  ctx.stroke();\
  ctx.closePath();\
  if( use_axis_numbering != -1){\
   ytxt = (px2y(y)).toFixed(prec);\
   ctx.fillText( ytxt,0 ,y + 0.5*font_size );\
  };\
  for(var dy = 1 ; dy < yminor ; dy++){\
   ctx.strokeStyle=\"rgba(\"+minor_color+\",\"+minor_opacity+\")\";\
   ctx.lineWidth = 0.2*line_width;\
   ctx.beginPath();\
   ctx.moveTo(xmarge,y+dy*minor_step);\
   ctx.lineTo(xsize,y+dy*minor_step);\
   ctx.stroke();\
   ctx.closePath();\
  };\
 };\
 ctx.restore();\
};",canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id);
        break;
      case DRAW_YLOGSCALE:
        fprintf(js_include_file,"\n/* draw ylogscale */\
var draw_grid%d = function(canvas_type,line_width,major_color,minor_color,major_opacity,minor_opacity,font_size,font_family,font_color,use_axis_numbering,xmajor,xminor,precision){\
 var obj;\
 if( document.getElementById(\"wims_canvas%d\"+canvas_type) ){\
  obj = document.getElementById(\"wims_canvas%d\"+canvas_type);\
 } else {\
  obj = create_canvas%d(canvas_type,xsize,ysize);\
 };\
 var ctx = obj.getContext(\"2d\");\
 ctx.clearRect(0,0,xsize,ysize);\
 ctx.save();\
 ctx.lineWidth = line_width;\
 var y_min = Math.log(ymin)/Math.log(ylogbase);\
 var y_max = Math.log(ymax)/Math.log(ylogbase);\
 var x_min = 0;var x_max = xsize;var y_s;var y_e;var num;var xmarge;var ymarge;\
 if(use_axis_numbering != -1){\
  ctx.font = font_family;\
  xmarge = ctx.measureText(ylogbase+\"^\"+y_max.toFixed(0)+' ').width;\
  ymarge = 2*font_size;\
  ctx.save();\
  ctx.fillStyle=\"rgba(255,215,0,0.2)\";\
  ctx.rect(0,0,xmarge,ysize);\
  ctx.rect(0,ysize-ymarge,xsize,ysize);\
  ctx.fill();\
  ctx.restore();\
 }else{xmarge = 0;ymarge = 0;};\
 if( typeof(xaxislabel) !== 'undefined' ){\
  ctx.save();\
  ctx.font = \"italic \"+font_size+\"px Arial\";\
  ctx.fillStyle = \"rgba(\"+font_color+\",\"+major_opacity+\")\";\
  corr =  ctx.measureText(xaxislabel).width;\
  ctx.fillText(xaxislabel,xsize - 1.5*corr,ysize - 2*font_size);\
  ctx.restore();\
 };\
 if( typeof(yaxislabel) !== 'undefined' ){\
  ctx.save();\
  ctx.font = \"italic \"+font_size+\"px Arial\";\
  ctx.fillStyle = \"rgba(\"+font_color+\",\"+major_opacity+\")\";\
  corr = ctx.measureText(yaxislabel).width;\
  ctx.translate(xmarge+font_size,corr+font_size);\
  ctx.rotate(-0.5*Math.PI);\
  ctx.fillText(yaxislabel,0,0);\
  ctx.restore();\
 };\
 ctx.fillStyle = \"rgba(\"+font_color+\",\"+major_opacity+\")\";\
 ctx.lineWidth = line_width;\
 for(var p = y_min; p <= y_max ; p++){\
  num = Math.pow(ylogbase,p);\
  for(var i = 1 ; i < ylogbase ; i++){\
   y_e = y2px(i*num);\
   if( i == 1 ){\
    ctx.lineWidth = line_width;\
    ctx.strokeStyle=\"rgba(\"+major_color+\",\"+major_opacity+\")\";\
    if( use_axis_numbering != -1 && p > y_min){\
     ctx.fillText(ylogbase+'^'+p.toFixed(0),0,y_e);\
    };\
   }else{\
    ctx.lineWidth = 0.2*line_width;\
    ctx.strokeStyle=\"rgba(\"+minor_color+\",\"+minor_opacity+\")\";\
   };\
   ctx.beginPath();\
   ctx.moveTo(xmarge,y_e);\
   ctx.lineTo(xsize,y_e);\
   ctx.stroke();\
   ctx.closePath();\
  };\
 };\
 var stepx = Math.abs(x2px(xmajor) - x2px(0));\
 var minor_step = stepx / xminor;\
 var prec = Math.log(precision)/Math.log(10);\
 var xtxt;var corr;var flip = 0;\
 for(var x = stepx ; x < xsize ; x = x + stepx){\
  ctx.strokeStyle=\"rgba(\"+major_color+\",\"+major_opacity+\")\";\
  ctx.lineWidth = line_width;\
  ctx.beginPath();\
  ctx.moveTo(x,ysize-ymarge);\
  ctx.lineTo(x,0);\
  ctx.stroke();\
  ctx.closePath();\
  if( use_axis_numbering != -1){\
   xtxt = (px2x(x)).toFixed(prec);\
   corr = 0.5*(ctx.measureText(xtxt).width);\
   if(flip == 0 ){flip = 1;ctx.fillText( xtxt,x - corr ,ysize - 0.2*font_size );}else{\
   flip = 0;ctx.fillText( xtxt,x - corr ,ysize - 1.2*font_size );};\
  };\
  for(var dx = 1 ; dx < xminor ; dx++){\
   ctx.strokeStyle=\"rgba(\"+minor_color+\",\"+minor_opacity+\")\";\
   ctx.lineWidth = 0.2*line_width;\
   ctx.beginPath();\
   ctx.moveTo(x+dx*minor_step,ysize - ymarge);\
   ctx.lineTo(x+dx*minor_step,0);\
   ctx.stroke();\
   ctx.closePath();\
  };\
 };\
 ctx.restore();\
};",canvas_root_id,canvas_root_id,canvas_root_id,canvas_root_id);
        break;
      case ADD_USER_INPUTS:
        fprintf(js_include_file,"\n/* add_input_xy */\
function add_input_xy(){\
if( wims_status == \"done\" ){return;};\
var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
var input_xy_div = document.createElement('div');\
input_xy_div.id = \"input_xy_div\";\
tooltip_div.appendChild(input_xy_div);",canvas_root_id);

      if(strstr(draw_type,"polyline") != NULL || strstr(draw_type,"point") != NULL || strstr(draw_type,"cross") != NULL ){ /* ( input_x : input_y )*/
        type=1;
        fprintf(js_include_file,"input_xy_div.innerHTML=\"<br><span class='%s'>(<input type='text' size='2' value='' id='userinput_x'> : <input type='text' size='2' value='' id='userinput_y'>)<input id='update_button' type='button' value='OK' onclick='' style='color:red;background-color:lightblue;'><input id='delete_button' type='button' value='NOK' onclick='' style='color:blue;background-color:red;'></span> \";",css_class);
      }else{
        if(strstr(draw_type,"circle") != NULL ){/* M: (input_x :input_y) R:input_r*/
          type=2;
          fprintf(js_include_file,"input_xy_div.innerHTML=\"<br><span class='%s'>M:&nbsp;(<input type='text' size='2' value='' id='userinput_x'> : <input type='text' size='2' value='' id='userinput_y'>) &nbsp;&nbsp;R:&nbsp;<input type='text' size='2' value='' id='userinput_r'><input id='update_button' type='button' value='OK' onclick='' style='color:red;background-color:lightblue;'><input id='delete_button' type='button' value='NOK' onclick='' style='color:blue;background-color:red;'></span> \";",css_class);
        }else{
            type=3;
            if(strstr(draw_type,"segment") != NULL  ||  strstr(draw_type,"line") != NULL  ||  strstr(draw_type,"arrow") != NULL  ){  /* ( input_x1 : input_y1) --- ( input_x2 : input_y2) */
              fprintf(js_include_file,"input_xy_div.innerHTML=\"<br><span class='%s'>(<input type='text' size='2' value='' id='userinput_x1'> : <input type='text' size='2' value='' id='userinput_y1'>) &minus;&minus;&minus; (<input type='text' size='2' value='' id='userinput_x2'> : <input type='text' size='2' value='' id='userinput_y2'>)<input id='update_button' type='button' value='OK' onclick='' style='color:red;background-color:lightblue;'><input id='delete_button' type='button' value='NOK' onclick='' style='color:blue;background-color:red;'></span> \";",css_class);
            }else{
              type=4;
              if(strstr(draw_type,"triang") != NULL ){ /* ( input_x1 : input_y1) --- ( input_x2 : input_y2) --- ( input_x3 : input_y3) */
                fprintf(js_include_file,"input_xy_div.innerHTML=\"<br><span class='%s'>&#8420;&nbsp;&nbsp;<input id='update_button' type='button' value='OK' onclick='' style='color:red;background-color:lightblue;'><input id='delete_button' type='button' value='NOK' onclick='' style='color:blue;background-color:red;'><br>(<input type='text' size='2' value='' id='userinput_x1'> : <input type='text' size='2' value='' id='userinput_y1'>)<br>(<input type='text' size='2' value='' id='userinput_x2'> : <input type='text' size='2' value='' id='userinput_y2'>)<br>(<input type='text' size='2' value='' id='userinput_x3'> : <input type='text' size='2' value='' id='userinput_y3'>)</span> \";",css_class);
              }else{
                type=5;
                if(strstr(draw_type,"text") != NULL ){ /*Text: input_r ( input_x1 : input_y1)  */
                  fprintf(js_include_file,"input_xy_div.innerHTML=\"<br><span class='%s'>(<input type='text' size='2' value='' id='userinput_x'>:<input type='text' size='2' value='' id='userinput_y'>)&nbsp;<input id='update_button' type='button' value='OK' onclick='' style='color:red;background-color:lightblue;'><input id='delete_button' type='button' value='NOK' onclick='' style='color:blue;background-color:red;'></span> \";",css_class);
                }else{canvas_error("command 'userinput' or 'userinput_xy' is not supported for your 'userdraw' type...");
              }
            }
          }
        }
      }
      fprintf(js_include_file,"\
var update_button = document.getElementById(\"update_button\");\
var delete_button = document.getElementById(\"delete_button\");\
update_button.addEventListener(\"mousedown\",function(e){update_me();redraw_userdraw();return;},false);\
delete_button.addEventListener(\"mousedown\",function(e){remove_last();return;},false);\
};add_input_xy();");

      switch(type){
        case 1:
          fprintf(js_include_file,"\n/* add_input_xy */\
function update_me(){\
 var x = safe_eval(document.getElementById('userinput_x').value);\
 var y = safe_eval(document.getElementById('userinput_y').value);\
 userdraw_x.push(x2px(x));userdraw_y.push(y2px(y));\
 return;\
};");break;
        case 2:
          fprintf(js_include_file,"\n/* add_input_xy */\
function update_me(){\
 var x = safe_eval(document.getElementById('userinput_x').value);\
 var y = safe_eval(document.getElementById('userinput_y').value);\
 var r = safe_eval(document.getElementById('userinput_r').value);\
 r = scale_x_radius(r);\
 userdraw_radius.push(r);userdraw_x.push(x2px(x));userdraw_y.push(y2px(y));\
 return;\
};");break;
        case 3:
          fprintf(js_include_file,"\n/* add_input_xy */\
function update_me(){\
 var x,y;\
 for(var p=1; p<3;p++){\
  x = safe_eval(document.getElementById('userinput_x'+p).value);\
  y = safe_eval(document.getElementById('userinput_y'+p).value);\
  userdraw_x.push(x2px(x));userdraw_y.push(y2px(y));\
 };\
 return;\
};");
          break;
        case 4:
          fprintf(js_include_file,"\n/* add_input_xy */\
function update_me(){\
 var x,y;\
 for(var p=1; p<4;p++){\
  x = safe_eval(document.getElementById('userinput_x'+p).value);\
  y = safe_eval(document.getElementById('userinput_y'+p).value);\
  userdraw_x.push(x2px(x));userdraw_y.push(y2px(y));\
 };\
 return;\
};");
          break;
        case 5:
 /* a inputfield with 'id=userdraw_text_abc' is provided by command 'userdraw text,color'*/
          fprintf(js_include_file,"\n/* add_input_xy */\
function update_me(){\
 var x = safe_eval(document.getElementById('userinput_x').value);\
 var y = safe_eval(document.getElementById('userinput_y').value);\
 var txt = conv_to_unicode(document.getElementById('userdraw_text_abc').value);\
 userdraw_x.push(x2px(x));userdraw_y.push(y2px(y));userdraw_text.push(txt.replace(/\\\\/g,' '));\
 return;\
};");
          break;
        default: canvas_error("hmmm, I've no idea...");break;
      }
      break;
      default:break;
      }
    }
  }
  return;
}
