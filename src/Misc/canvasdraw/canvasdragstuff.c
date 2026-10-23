#include "canvasdraw.h"

void add_drag_code(int canvas_cnt,int use_dragstuff, int dragstuff[],int reply_format){
/* in drag& drop / onclick library:
 use_dragstuff == 1 : no mouse code
    obj_type = 1 == rect / rects
    obj_type = 2 == point / points (do not scale with zoom)
    obj_type = 3 == ellipse
    obj_type = 4 == polyline / segment / segments /line / vline / hline
    obj_type = 5 == closed path (polygon)
    obj_type = 6 == roundrect /roundrects
    obj_type = 7 == crosshair / crosshairs
    obj_type = 8 == arrow / arrows
    obj_type = 9 == curve
    obj_type = 10== arrow2 / arrows2
    obj_type = 11== parallel (no drag or onclick)
    obj_type = 12== arc : radius is in pixels , so no zooming in/out
    obj_type = 13== circle / circles (will scale on zoom)
    obj_type = 14== text (will not scale or pan on zoom)
    obj_type = 15== animated point on curve
    obj_type = 16== pixels
    obj_type = 17== new arc [command angle] :radius in x-range, so will scale on zooming in/out
    obj_type = 18== halfline
    obj_type = 19== yerrorbars
    obj_type = 20== xerrorbars
    obj_type = 21== curvedarrow
    obj_type = 22== use a slider (...)
    obj_type = 23== image
    obj_type = 24== arrowarc right
    obj_type = 25== arrowarc left
    obj_type = 26== arrowarc left/right
arc
x[0] = x[1] = xc = double_data[0]
y[0] = y[1] = yc = double_data[1]
w[0] = width = int_data[0]
w[1] = height = int_data[1]
h[0] = start_angle = double_data[2]
h[1] = end_angle = double_data[3]

use_offset only for text shape objects...
 0=none;
 1=yoffset
 2=xoffset
 3=xyoffset
 4=centered

int onclick = 0; 0 = noninteractive ; 1 = onclick ; 2 = draggable
type= object type

verwijderd: direction : dragging in xy=0 , in x=1 in y=2 vervangen dooor use_snap en multisnap_check(x,y,use_snap)
use_snap: 0 = none 1=grid : 2=x-grid : 3=y-grid : 4=snap to points
obj.angle == RADIANS!
use_filled 0=no fill, 1=fill,2=grid,3=hatch,4=diamond,5=dot,6=image

rotate+translate+linear: is done in C-functions...e.g. original coordinates are transformed
NO NEED for javascript rotations in dragstuff...except for ANGLE
*/
  int i;
  fprintf(js_include_file,"\n/* complete drag_drop_onclick shape library */\
  if( typeof(dragdrop_precision) == 'undefined' ){var dragdrop_precision = 100;};\
  function Shape(drag_type,object_cnt,onclick,use_snap,type,x,y,w,h,line_width,stroke_color,stroke_opacity,fill_color,fill_opacity,use_filled,use_dashed,dashtype0,dashtype1,use_rotate,angle,text,font_size,font_family,use_slider,rotation_center,use_offset){\
  this.drag_type = drag_type || 0;\
  this.stroke_opacity = stroke_opacity || 1.0;\
  this.stroke_color = \"rgba(\"+stroke_color+\",\"+stroke_opacity+\")\" || '#FF0000';\
  this.fill_opacity = fill_opacity || 1.0;\
  this.fill_color = \"rgba(\"+fill_color+\",\"+fill_opacity+\")\" || '#FF0000';\
  this.use_filled = use_filled || 0;\
  if( this.use_filled > 1 ){ this.fill_pattern = create_Pattern(0,0,this.use_filled,this.fill_color);};\
  this.use_slider = use_slider;\
  this.text = text || 0;\
  this.font_size = font_size || 12;\
  if( this.font_size == 12 ){ this.font_size = parseInt(font_family); };\
  this.font_family = font_family || this.font_size+'px Arial';\
  this.org_font_family = this.font_family;\
  this.textwidth = 0;\
  this.textheight = 0;\
  if(type == 14 ){ this.use_rotate = use_rotate;this.angle=angle*Math.PI/180;}else{this.angle=0;this.use_rotate=0;};\
  this.object_cnt = object_cnt;\
  this.onclick = onclick;\
  this.use_snap = use_snap || 0;\
  this.type = type || 1;\
  this.xorg = x;this.yorg = y;\
  this.use_once = true;\
  this.x = [x.length];this.y = [x.length];this.w = [x.length];this.h = [x.length];\
  for(var p=0;p<x.length;p++){this.x[p] = x2px(x[p]-xstart);this.y[p] = y2px(y[p]-ystart);if( p > w.length){this.w[p] = w[0];this.h[p] = h[0];}else{this.w[p] = w[p];this.h[p] = h[p];};};\
  if( rotation_center != null ){this.use_rotation_center = 1;this.rotation_center = [x2px(rotation_center[0]),y2px(rotation_center[1])];}else{this.use_rotation_center = 0;this.rotation_center = [this.x[0],this.y[0]];};\
  this.line_width = line_width || 2;\
  this.org_line_width = line_width || 2;\
  this.use_filled = use_filled || 0;\
  this.use_dashed = use_dashed || 0;\
  this.dashtype0 = dashtype0 || 4;\
  this.dashtype1 = dashtype1 || 4;\
  this.use_offset = use_offset || 0;\
  this.clicked = 0;\
  dragdrop_reply[object_cnt] = {object_cnt:this.object_cnt,type:onclick,x:this.x[0],y:this.y[0],r:this.w[0],angle:this.angle,clicked:0};\
  if(this.use_slider != -1 ){slidergroup[this.object_cnt] = this;};\
  };");

// dragdrop_reply[object_cnt] = {object_cnt:this.object_cnt,type:onclick,x:this.x[0],y:this.y[0],r:this.w[0],angle:this.angle,clicked:0};

  if( dragstuff[21] == 1 ){
    fprintf(js_include_file,"\n/* add curvedarrow... is this really the place for it??? */\
  function drawCurvedArrow(ctx,x1,y1,x3,y3,x2,y2,arrowhead,type){ctx.save();var angle1 = Math.atan2(x3 - x2,y3 - y2) + Math.PI;ctx.beginPath();if(type == 2){var angle2 =Math.atan2(x3 - x1,y3 - y1) + Math.PI;ctx.moveTo(x1,y1);ctx.moveTo(x1 - (arrowhead * Math.sin(angle2 - Math.PI / 6)),y1 - (arrowhead * Math.cos(angle2 - Math.PI / 6)));ctx.lineTo(x1, y1);ctx.lineTo(x1 - (arrowhead * Math.sin(angle2 + Math.PI / 6)),y1 - (arrowhead * Math.cos(angle2 + Math.PI / 6)));};ctx.moveTo(x1,y1);ctx.quadraticCurveTo(x3,y3,x2,y2);ctx.moveTo(x2 - (arrowhead * Math.sin(angle1 - Math.PI / 6)),y2 - (arrowhead * Math.cos(angle1 - Math.PI / 6)));ctx.lineTo(x2, y2);ctx.lineTo(x2 - (arrowhead * Math.sin(angle1 + Math.PI / 6)),y2 - (arrowhead * Math.cos(angle1 + Math.PI / 6)));ctx.stroke();ctx.closePath();ctx.restore();};");
  }

  fprintf(js_include_file,"\n/* add basic drawstuff,but don't go here if nothing has changed... */\
Shape.prototype.draw = function(ctx){\
 if(dragstuff.valid === true){return;};\
 ctx.lineWidth = this.line_width;ctx.strokeStyle = this.stroke_color;\
 if( this.use_filled > 1 ){ ctx.fillStyle = this.fill_pattern; }else{ ctx.fillStyle = this.fill_color; };\
 ctx.lineJoin = \"round\";ctx.save();\
 if(this.use_rotate == 1){if(this.use_rotation_center == 0 ){ this.rotation_center[0] = this.x[0];this.rotation_center[1] = this.y[0];};ctx.translate(this.rotation_center[0],this.rotation_center[1]);ctx.rotate(this.angle);ctx.translate(-1*(this.rotation_center[0]),-1*(this.rotation_center[1]));};\
 ctx.beginPath();\
 switch(this.type){default:break;");

  for( i=0; i < MAX_DRAGSTUFF; i++){
    if( dragstuff[i] == 1 ){
      switch(i){
        case 1: fprintf(js_include_file,"case 1: for(var p = 0 ; p < this.x.length;p = p+4){ctx.rect(this.x[p], this.y[p], this.x[p+1]-this.x[p], this.y[p+2] - this.y[p]);};break;");break;
        case 2: fprintf(js_include_file,"case 2: ctx.arc(this.x[0],this.y[0],0.5*this.w[0],0,2*Math.PI,false);break;");break;
        case 3: fprintf(js_include_file,"case 3: ctx.save();var w = 0.5*(scale_x_radius(this.w[0]));var h = 0.5*(scale_y_radius(this.h[0]));ctx.scale(1,h/w);ctx.beginPath();ctx.arc(this.x[0], w/h*this.y[0], w, 0, 2 * Math.PI);if(this.use_filled == 1){ ctx.fillStyle = this.fill_color; ctx.fill(); };ctx.closePath();if(this.use_dashed == 1 ){if( ctx.setLineDash ){ ctx.setLineDash([this.dashtype0,this.dashtype1]);}else{ ctx.mozDash = [this.dashtype0,this.dashtype1];};};ctx.stroke();ctx.restore();break;");break;
        case 4: fprintf(js_include_file,"case 4: for(var p = 0; p < this.x.length - 1;p++){ctx.moveTo(this.x[p], this.y[p]);ctx.lineTo(this.x[p+1],this.y[p+1]);};break;");break;
        case 5: fprintf(js_include_file,"case 5: ctx.moveTo(this.x[0],this.y[0]);for(var p = 1; p < this.x.length;p++){ctx.lineTo(this.x[p],this.y[p]);};ctx.lineTo(this.x[0],this.y[0]);break;");break;
        case 6: fprintf(js_include_file,"case 6: var w = this.x[1] - this.x[0];var h = this.y[1] - this.y[0];var r = this.w[0];ctx.beginPath();ctx.moveTo(this.x[0] + r, this.y[0]);ctx.lineTo(this.x[0] + w - r, this.y[0]);ctx.quadraticCurveTo(this.x[0] + w, this.y[0], this.x[0] + w, this.y[0] + r);ctx.lineTo(this.x[0] + w, this.y[0] + h - r);ctx.quadraticCurveTo(this.x[0] + w, this.y[0] + h, this.x[0] + w - r, this.y[0] + h);ctx.lineTo(this.x[0] + r, this.y[0] + h);ctx.quadraticCurveTo(this.x[0], this.y[0] + h, this.x[0], this.y[0] + h - r);ctx.lineTo(this.x[0], this.y[0] + r);ctx.quadraticCurveTo(this.x[0], this.y[0], this.x[0] + r, this.y[0]);ctx.closePath();break;");break;
        case 7: fprintf(js_include_file,"case 7: ctx.moveTo(this.x[0]-this.w[0],this.y[0]-this.h[0]);ctx.lineTo(this.x[0]+this.w[0],this.y[0]+this.h[0]);ctx.moveTo(this.x[0]-this.w[0],this.y[0]+this.h[0]);ctx.lineTo(this.x[0]+this.w[0],this.y[0]-this.h[0]);break;");break;
        case 8: fprintf(js_include_file,"case 8: var x1,x2,y1,y2,dx,dy,h;var len = this.x.length;var arrow_head = this.w[0];ctx.save();for(var p = 0 ; p < len ; p = p+2 ){x1 = this.x[p];y1 = this.y[p];x2 = this.x[p+1];y2 = this.y[p+1];dx = x2 - x1;dy = y2 - y1;h = Math.sqrt(dx*dx+dy*dy);ctx.save();ctx.setLineDash([]);ctx.translate(x2,y2);ctx.rotate(Math.atan2(dy,dx));ctx.beginPath();ctx.moveTo(0,0);ctx.lineTo(-1*arrow_head,-0.5*arrow_head);ctx.lineTo(-1*arrow_head, 0.5*arrow_head);ctx.closePath();ctx.stroke();ctx.fill();};ctx.restore();for(var p = 0 ; p < len ; p = p+2 ){x1 = this.x[p];y1 = this.y[p];x2 = this.x[p+1];y2 = this.y[p+1];ctx.beginPath();ctx.moveTo(x1,y1);ctx.lineTo(x2,y2);};break;");
        case 9: fprintf(js_include_file,"case 9: ctx.moveTo(this.x[0], this.y[0]);for(var p = 1; p < this.x.length - 1;p++){if( Math.abs(this.y[p] - this.y[p-1]) < ysize && Math.abs(this.y[p+1] - this.y[p]) < ysize ){ctx.lineTo(this.x[p],this.y[p]);}else{ctx.moveTo(this.x[p],this.y[p]);};};break;");break;
        case 10: fprintf(js_include_file,"case 10: var x1,x2,y1,y2,dx,dy,h;var len = this.x.length;var arrow_head = this.w[0];ctx.save();for(var p = 0 ; p < len ; p = p+2 ){x1 = this.x[p];y1 = this.y[p];x2 = this.x[p+1];y2 = this.y[p+1];dx = x2 - x1;dy = y2 - y1;h = Math.sqrt(dx*dx+dy*dy);ctx.save();ctx.setLineDash([]);ctx.translate(x2,y2);ctx.rotate(Math.atan2(dy,dx));ctx.beginPath();ctx.moveTo(0,0);ctx.lineTo(-1*arrow_head,-0.5*arrow_head);ctx.lineTo(-1*arrow_head, 0.5*arrow_head);ctx.closePath();ctx.stroke();ctx.fill();};ctx.restore();for(var p = 0 ; p < len ; p = p+2 ){x1 = this.x[p];y1 = this.y[p];x2 = this.x[p+1];y2 = this.y[p+1];dx = x2 - x1;dy = y2 - y1;h = Math.sqrt(dx*dx+dy*dy);ctx.save();ctx.setLineDash([]);ctx.translate(x1,y1);ctx.rotate(Math.atan2(dy,dx));ctx.beginPath();ctx.moveTo(0,0);ctx.lineTo(arrow_head,0.5*arrow_head);ctx.lineTo(arrow_head, -0.5*arrow_head);ctx.closePath();ctx.stroke();ctx.fill();};ctx.restore();for(var p = 0 ; p < len ; p = p+2 ){x1 = this.x[p];y1 = this.y[p];x2 = this.x[p+1];y2 = this.y[p+1];ctx.beginPath();ctx.moveTo(x1,y1);ctx.lineTo(x2,y2);};break;");break;
        case 11: fprintf(js_include_file,"case 11:if(this.use_dashed == 1 ){if( ctx.setLineDash ){ ctx.setLineDash([this.dashtype0,this.dashtype1]);}else{ ctx.mozDash = [this.dashtype0,this.dashtype1];};};var x1 = this.x[0];var y1 = this.y[0];var x2 = this.x[1];var y2 = this.y[1];var dx = this.x[2];var dy = this.y[2];var n  = this.w[0];for(var p = 0 ; p < n ; p++ ){ctx.beginPath();ctx.moveTo(x1,y1);ctx.lineTo(x2,y2);ctx.stroke();x1 = x1 + dx;y1 = y1 + dy;x2 = x2 + dx;y2 = y2 + dy;ctx.closePath();};break;");break;
        case 12: fprintf(js_include_file,"case 12: ctx.save();var start;var end;if(this.h[0] < this.h[1]){start = this.h[0];end = this.h[1]}else{start = this.h[1];end = this.h[0];};start=(360-start)*Math.PI/180;end=(360-end)*Math.PI/180;var w = 0.5*(scale_x_radius(this.w[0]));var wh = 0.5*(scale_y_radius(this.w[1]));ctx.scale(1,wh/w);ctx.arc(this.x[0],this.y[0]*w/wh,w,start,end,true);if(this.use_dashed == 1 ){if( ctx.setLineDash ){ ctx.setLineDash([this.dashtype0,this.dashtype1]);}else{ ctx.mozDash = [this.dashtype0,this.dashtype1];};};ctx.stroke();if(this.use_filled == 1){ ctx.fillStyle = this.fill_color;ctx.lineTo(this.x[0],this.y[0]);ctx.fill(); };ctx.restore();break;");break;
        case 13: fprintf(js_include_file,"case 13: for(var p = 0; p < this.x.length; p++){ ctx.arc(this.x[p],this.y[p],scale_x_radius(this.w[p]),0,2*Math.PI,false);};break;");break;
        case 14: fprintf(js_include_file,"case 14: if( this.font_family == null ){ ctx.font = this.font_size+\"px Arial\";}else{ctx.font = this.font_family ;};if( this.use_once == true ){this.marge = 2*this.line_width;this.textwidth = ctx.measureText(this.text).width;this.textheight = ctx.measureText('W').width;};if(this.text.indexOf('_')>1 || this.text.indexOf('^')>1){draw_subsup(ctx,this.x[0],this.y[0],this.text,this.use_offset);}else{if( this.use_offset > 0 && this.use_once == true ){this.use_once = false;if(this.use_offset == 1){this.y[0] = this.y[0] - this.marge;}else{if(this.use_offset == 2){this.x[0] = this.x[0] + this.marge;}else{if(this.use_offset == 3){this.y[0] = this.y[0] - this.marge;this.x[0] = this.x[0] + this.marge;}else{if(this.use_offset == 4){this.y[0] = parseInt(this.y[0] + 0.25*(this.textheight));this.x[0] = parseInt(this.x[0] - 0.5*(this.textwidth));};};};};};ctx.fillText(this.text,this.x[0],this.y[0]);};break;");break;
        case 15: fprintf(js_include_file,"case 15: break;");break;
        case 16: fprintf(js_include_file,"case 16: for(var p = 0; p < this.x.length;p++){ctx.fillRect( this.x[p], this.y[p],this.line_width,this.line_width );};break;");break;
        case 17: fprintf(js_include_file,"case 17: ctx.save();var start;var end;if(this.h[0] < this.h[1]){start = this.h[0];end = this.h[1]}else{start = this.h[1];end = this.h[0];};start=2*Math.PI-start;end=2*Math.PI-end;var r = scale_x_radius(this.w[0]);ctx.arc(this.x[0], this.y[0], r,start,end,true);if(this.use_filled){ctx.lineTo(this.x[0],this.y[0]);ctx.fill();};ctx.restore();break;");break;
        case 18: fprintf(js_include_file,"case 18: for(var p=0 ; p < this.x.length ;p=p+2){ctx.moveTo(this.x[p], this.y[p]);ctx.lineTo(this.x[p+1],this.y[p+1]);};break;");break;
        case 19: fprintf(js_include_file,"case 19: ctx.arc(this.x[0],this.y[0],this.line_width,0,2*Math.PI,false);var E1 = y2px(px2y(this.y[0]) - this.h[0]);var E2 = y2px(px2y(this.y[0]) + this.w[0]);ctx.moveTo(this.x[0],E1);ctx.lineTo(this.x[0],E2);ctx.moveTo(this.x[0] - 2*(this.line_width),E1);ctx.lineTo(this.x[0] + 2*(this.line_width),E1);ctx.moveTo(this.x[0] - 2*(this.line_width),E2);ctx.lineTo(this.x[0] + 2*(this.line_width),E2);break;");break;
        case 20: fprintf(js_include_file,"case 20: ctx.arc(this.x[0],this.y[0],this.line_width,0,2*Math.PI,false);var E1 = x2px(px2x(this.x[0]) - this.w[0]);var E2 = x2px(px2x(this.x[0]) + this.h[0]);ctx.moveTo(E1,this.y[0]);ctx.lineTo(E2,this.y[0]);ctx.moveTo(E1,this.y[0]-2*(this.line_width));ctx.lineTo(E1,this.y[0]+2*(this.line_width));ctx.moveTo(E2,this.y[0]-2*(this.line_width));ctx.lineTo(E2,this.y[0]+2*(this.line_width));break;");break;
        case 21: fprintf(js_include_file,"case 21: drawCurvedArrow(ctx,this.x[0],this.y[0],this.x[1],this.y[1],this.x[2],this.y[2],this.h[0],this.h[1]);break;");break;
        case 22: fprintf(js_include_file,"case 22: break;");break;
        case 24: fprintf(js_include_file,"case 24: ctx.save();var start;var end;if(this.h[0] < this.h[1]){start = this.h[0];end = this.h[1]}else{start = this.h[1];end = this.h[0];};start=(360-start)*Math.PI/180;end=(360-end)*Math.PI/180;var w = 0.5*(scale_x_radius(this.w[0]));var wh = 0.5*(scale_y_radius(this.w[1]));ctx.scale(1,wh/w);var f = w/wh;ctx.arc(this.x[0],this.y[0]*f,w,start,end,true); if(this.use_dashed == 1 ){if( ctx.setLineDash ){ ctx.setLineDash([this.dashtype0,this.dashtype1]);}else{ ctx.mozDash = [this.dashtype0,this.dashtype1];};};ctx.stroke();var xh = this.x[0]+ w*Math.cos(start);var yh = f*(this.y[0]+ wh*Math.sin(start));var xh0 = this.x[0]+ w*Math.cos(start+0.1);var yh0 = f*(this.y[0]+ wh*Math.sin(start+0.1));draw_arrowhead(xh,yh,xh0,yh0,ctx);ctx.restore();break;");break;
        case 25: fprintf(js_include_file,"case 25: ctx.save();var start;var end;if(this.h[0] < this.h[1]){start = this.h[0];end = this.h[1]}else{start = this.h[1];end = this.h[0];};start=(360-start)*Math.PI/180;end=(360-end)*Math.PI/180;var w = 0.5*(scale_x_radius(this.w[0]));var wh = 0.5*(scale_y_radius(this.w[1]));ctx.scale(1,wh/w);var f = w/wh;ctx.arc(this.x[0],this.y[0]*f,w,start,end,true); if(this.use_dashed == 1 ){if( ctx.setLineDash ){ ctx.setLineDash([this.dashtype0,this.dashtype1]);}else{ ctx.mozDash = [this.dashtype0,this.dashtype1];};};ctx.stroke();var xh = this.x[0]+ w*Math.cos(end);var yh = f*(this.y[0]+ wh*Math.sin(end));var xh0 = this.x[0]+ wh*Math.cos(end-0.1);var yh0 =  f*(this.y[0]+ wh*Math.sin(end-0.1));draw_arrowhead(xh,yh,xh0,yh0,ctx);ctx.restore();break;");break;
        case 26: fprintf(js_include_file,"case 26: ctx.save();var start;var end;if(this.h[0] < this.h[1]){start = this.h[0];end = this.h[1]}else{start = this.h[1];end = this.h[0];};start=(360-start)*Math.PI/180;end=(360-end)*Math.PI/180;var w = 0.5*(scale_x_radius(this.w[0]));var wh = 0.5*(scale_y_radius(this.w[1]));ctx.scale(1,wh/w);var f = w/wh;ctx.arc(this.x[0],this.y[0]*f,w,start,end,true); if(this.use_dashed == 1 ){if( ctx.setLineDash ){ ctx.setLineDash([this.dashtype0,this.dashtype1]);}else{ ctx.mozDash = [this.dashtype0,this.dashtype1];};};ctx.stroke();var xh = this.x[0]+ w*Math.cos(start);var yh = f*(this.y[0]+ wh*Math.sin(start));var xh0 = this.x[0]+ w*Math.cos(start+0.1);var yh0 = f*(this.y[0]+ wh*Math.sin(start+0.1));draw_arrowhead(xh,yh,xh0,yh0,ctx);xh = this.x[0]+ w*Math.cos(end);yh = f*(this.y[0]+ wh*Math.sin(end));xh0 = this.x[0]+ wh*Math.cos(end-0.1);yh0 = f*(this.y[0]+ wh*Math.sin(end-0.1));draw_arrowhead(xh,yh,xh0,yh0,ctx);ctx.restore();break;");break;
        case 27: fprintf(js_include_file,"case 27: for(var p = 0; p < this.x.length;p++)if(this.w[p]==1){ctx.moveTo(this.x[p],this.y[p]);ctx.lineTo(x2px(this.h[p][0]-xstart),y2px(this.h[p][1]-ystart));}else{var s1=this.h[p][1]; var s2=this.h[p][2]; var sens=s1<s2; if(sens){sens=s2<s1+180;}else{sens=s2<s1-180;};var r=scale_x_radius(this.h[p][0])/2;ctx.arc(this.x[p],this.y[p],r,-s1*Math.PI/180,-s2*Math.PI/180,sens);};break;");break;
        default: break;
      }
    }
  }

  fprintf(js_include_file,"};\
 if(this.use_filled > 0){ ctx.fill();}\
 if(this.use_dashed == 1 ){if( ctx.setLineDash ){ ctx.setLineDash([this.dashtype0,this.dashtype1]);}else{ ctx.mozDash = [this.dashtype0,this.dashtype1];};};\
 ctx.stroke();ctx.restore();\
};");

  if( use_dragstuff != 1 ){
    fprintf(js_include_file,"\n/* add mouse -- object recognition stuff */ Shape.prototype.contains = function(mx, my){");
    /* only rotate text (type=14) with canvas-own-transformation !! */
    if( dragstuff[14] == 1){ fprintf(js_include_file,"if( this.type == 14 ){if( this.angle != 0 ){ var m_rot = rotate_mouse(mx,my,this);mx = m_rot.x;my = m_rot.y;};};");}
    fprintf(js_include_file," var marge = 2*this.org_line_width;\
      switch(this.type){default:break;");
    for( i=0; i < MAX_DRAGSTUFF; i++){
      if( dragstuff[i] == 1 ){
      switch(i){
        case 1: fprintf(js_include_file,"case 1: for(var p = 0 ; p < this.x.length; p++ ){if( mx < this.x[p] + marge && mx > this.x[p] - marge ){if( my < this.y[p]+marge && my > this.y[p] - marge ){return p;};};};break;");break;
        case 2: fprintf(js_include_file,"case 2: for(var p = 0 ; p < this.x.length; p++ ){if( Math.abs(distance(this.x[p],this.y[p],mx,my) ) < this.w[p] + 4*marge ){return p;break;};};break;");break;
        case 3: fprintf(js_include_file,"case 3: for(var p = 0 ; p < this.x.length; p++ ){if( Math.abs(distance(this.x[p],this.y[p],mx,my) ) < scale_x_radius(this.w[p]) + marge ){return p;break;};};break;");break;
        case 4: fprintf(js_include_file,"case 4: var diff;var q;var r;for(var p = 0 ; p < this.x.length-1; p = p+2 ){if( ((this.x[p+1] - this.x[p]) != 0) && ((this.y[p+1]-this.y[p]) != 0) ){r = (this.y[p+1]-this.y[p])/(this.x[p+1]-this.x[p]);q = this.y[p] - (r)*(this.x[p]);diff = distance_to_line(r,q,mx,my);}else{if( (this.y[p+1]-this.y[p]) != 0 ){diff = Math.abs(this.x[p] - mx);}else{diff = Math.abs(this.y[p] - my);};};if( diff < marge ){ return p;};}; break;");break;
        case 5: fprintf(js_include_file,"case 5: marge = 2*marge;for(var p = 0 ; p < this.x.length; p++ ){if( mx < this.x[p] + marge && mx > this.x[p] - marge ){if( my < this.y[p]+marge && my > this.y[p] - marge ){return p;};};};break;");break;
        case 6: fprintf(js_include_file,"case 6: marge = 0.5*this.w[0];for(var p = 0 ; p < this.x.length; p++ ){if( mx < this.x[p] + marge && mx > this.x[p] - marge ){if( my < this.y[p]+marge && my > this.y[p] - marge ){return p;};};};break;");break;
        case 7: fprintf(js_include_file,"case 7: for(var p = 0 ; p < this.x.length; p++ ){if( (this.x[p] - this.w[p] <= mx) && (this.x[p] + this.w[p] >= mx) && (this.y[p] - this.h[p] <= my) && (this.y[p] + this.h[p] >= my) ){return p;};};break;");break;
        case 8: fprintf(js_include_file,"case 8: var diff;var q;var r;for(var p = 0 ; p < this.x.length-1; p = p+2 ){if( ((this.x[p+1] - this.x[p]) != 0) && ((this.y[p+1]-this.y[p]) != 0) ){r = (this.y[p+1]-this.y[p])/(this.x[p+1]-this.x[p]);q = this.y[p] - (r)*(this.x[p]);diff = distance_to_line(r,q,mx,my);}else{if( (this.y[p+1]-this.y[p]) != 0 ){diff = Math.abs(this.x[p] - mx);}else{diff = Math.abs(this.y[p] - my);};};if( diff < marge ){ return p;};}; break;");break;
        case 9: fprintf(js_include_file,"case 9: for(var p = 0 ; p < this.x.length; p++ ){if( (this.x[p] - this.line_width <= mx) && (this.x[p] + this.w[p] + this.line_width >= mx) && (this.y[p] - this.line_width <= my) && (this.y[p] + this.h[p] +this.line_width >= my) ){return p;};};break;");break;
        case 10: fprintf(js_include_file,"case 10: var diff;var q;var r;for(var p = 0 ; p < this.x.length-1; p = p+2 ){if( ((this.x[p+1] - this.x[p]) != 0) && ((this.y[p+1]-this.y[p]) != 0) ){r = (this.y[p+1]-this.y[p])/(this.x[p+1]-this.x[p]);q = this.y[p] - (r)*(this.x[p]);diff = distance_to_line(r,q,mx,my);}else{if( (this.y[p+1]-this.y[p]) != 0 ){diff = Math.abs(this.x[p] - mx);}else{diff = Math.abs(this.y[p] - my);};};if( diff < marge ){ return p;};}; break;");break;
        case 11: fprintf(js_include_file,"case 11: break;");break;
        case 12: fprintf(js_include_file,"case 12: var radius = 0.5*(scale_x_radius(this.w[0]));for(var p = 0 ; p < this.x.length; p++ ){var d = Math.abs(distance(this.x[p],this.y[p],mx,my));if( d < radius + marge && d > radius - marge ){return p;break;};};break;");break;
        case 13: fprintf(js_include_file,"case 13: for(var p = 0 ; p < this.x.length; p++ ){if( Math.abs(distance(this.x[p],this.y[p],mx,my) ) < scale_x_radius(this.w[p]) + marge ){return p;break;};};break;");break;
        case 14: fprintf(js_include_file,"case 14: for(var p = 0 ; p < this.x.length; p++ ){if( my < this.y[p] && my > this.y[p] - this.textheight ){if( mx < this.x[p] + this.textwidth && mx > this.x[p] ){ return p;break;};};};break;");break;
        case 15: fprintf(js_include_file,"case 15: break;");break;
        case 16: fprintf(js_include_file,"case 16: break;");break;
        case 17: fprintf(js_include_file,"case 17: var radius = scale_x_radius(this.w[0]);var d = Math.abs(distance(this.x[0],this.y[0],mx,my));if( d < radius + marge && d > radius - marge ){return 0;break;};break;");break;
        case 18: fprintf(js_include_file,"case 18: var diff;var q;var r;if(((this.x[1] - this.x[0]) != 0) && ((this.y[1]-this.y[0]) != 0)){r = (this.y[1]-this.y[0])/(this.x[1]-this.x[0]);q = this.y[0] - (r)*(this.x[0]);diff = distance_to_line(r,q,mx,my);}else{if((this.y[1]-this.y[0])!= 0){diff = Math.abs(this.x[0] - mx);}else{diff = Math.abs(this.y[0] - my);};};if( diff < marge ){ return 0;};break;");break;
        case 19: fprintf(js_include_file,"case 19: marge = 2*marge;for(var p = 0 ; p < this.x.length; p++ ){if( mx < this.x[p] + marge && mx > this.x[p] - marge ){if( my < this.y[p]+marge && my > this.y[p] - marge ){return p;};};};break;");break;
        case 20: fprintf(js_include_file,"case 20: for(var p = 0 ; p < this.x.length; p++ ){if( (this.x[p] - this.line_width <= mx) && (this.x[p] + this.w[p] + this.line_width >= mx) && (this.y[p] - this.line_width <= my) &&(this.y[p] + this.h[p] +this.line_width >= my) ){return p;};};break;");break;
        case 21: fprintf(js_include_file,"case 21: marge = 2*marge;for(var p = 0 ; p < this.x.length; p++ ){if( mx < this.x[p] + marge && mx > this.x[p] - marge ){if( my < this.y[p]+marge && my > this.y[p] - marge ){return p;};};};break;");break;
        case 22: fprintf(js_include_file,"case 22: break;");break;
        case 24: fprintf(js_include_file,"case 24: var radius = 0.5*(scale_x_radius(this.w[0]));for(var p = 0 ; p < this.x.length; p++ ){var d = Math.abs(distance(this.x[p],this.y[p],mx,my));if( d < radius + marge && d > radius - marge ){return p;break;};};break;");break;
        case 25: fprintf(js_include_file,"case 25: var radius = 0.5*(scale_x_radius(this.w[0]));for(var p = 0 ; p < this.x.length; p++ ){var d = Math.abs(distance(this.x[p],this.y[p],mx,my));if( d < radius + marge && d > radius - marge ){return p;break;};};break;");break;
        case 26: fprintf(js_include_file,"case 26: var radius = 0.5*(scale_x_radius(this.w[0]));for(var p = 0 ; p < this.x.length; p++ ){var d = Math.abs(distance(this.x[p],this.y[p],mx,my));if( d < radius + marge && d > radius - marge ){return p;break;};};break;");break;
        default: break;
      }
    }
  }
  fprintf(js_include_file,"};return -1;};\
 var obj = create_canvas%d(%d,xsize,ysize);\
 var container_div = document.getElementById(\"canvas_div%d\");\
 function CanvasState(canvas,container_div){\
  this.canvas = canvas;this.width = canvas.width;this.height = canvas.height;var ctx = canvas.getContext(\"2d\");this.ctx = ctx;this.valid = false;this.shapes = [];this.moved = [];this.dragging = false;this.selection = null;var myState = this;\
  container_div.addEventListener( 'mouseup'    , mouseup,false);\
  container_div.addEventListener( 'mousemove'  , mousemove,false);\
  container_div.addEventListener( 'mousedown'  , mousedown,false);\
  container_div.addEventListener('touchstart'  , function(e) { e.preventDefault(); mousedown(e.changedTouches[0]);},false);\
  container_div.addEventListener( 'touchmove'  , function(e) { e.preventDefault(); mousemove(e.changedTouches[0]);},false);\
  container_div.addEventListener( 'touchend'   , function(e) { e.preventDefault(); mouseup(  e.changedTouches[0]);},false);\
  function mousedown(e){\
  if( wims_status == \"done\"){return null;};\
  var mouse = getMouse(e,canvas);var mx = mouse.x;var my = mouse.y;\
  var shapes = myState.shapes;\
  var l = shapes.length;\
  var chk = -1;\
  for(var i=0;i<l;i++){\
  chk = shapes[i].contains(mx, my);\
  if ( chk != -1 ){\
    myState.chk = chk;\
    myState.selection = shapes[i];\
    myState.valid = false;\
    switch(shapes[i].onclick){\
    case 0:myState.dragging = false;break;\
    case 1:if( dragdrop_reply[myState.selection.object_cnt].clicked != 1 ){\
     myState.selection.line_width = 2*myState.selection.org_line_width;\
     myState.selection.clicked = 1;\
     myState.selection.font_family = parseInt(myState.selection.font_size+10)+\"px Courier\";\
     dragdrop_reply[myState.selection.object_cnt].clicked = 1;}else{dragdrop_reply[myState.selection.object_cnt].clicked = 0;\
     myState.selection.clicked = 0;myState.selection.font_family = myState.selection.org_font_family;myState.selection.line_width = myState.selection.org_line_width;};\
     myState.dragging = false;myState.draw();myState.selection = null;break;\
  case 2: myState.dragging = true;break;\
  case 3: break;\
  case 4: break;\
  case 5: myState.dragging = true;break;\
  default:break;};myState.valid = true;return;};};myState.valid = true;return;};\
  function mouseup(e){if(myState.selection != null ){if(myState.selection.onclick == 2 ){var dx,dy;var mouse = getMouse(e,canvas);var xy = multisnap_check(mouse.x,mouse.y,myState.selection.use_snap);switch(myState.selection.drag_type){case 0: dx = xy[0] - myState.selection.x[myState.chk];dy = xy[1] - myState.selection.y[myState.chk];break;case 1: dx = xy[0] - myState.selection.x[myState.chk];dy = 0;break;case 2: dx = 0;dy = xy[1] - myState.selection.y[myState.chk];break;default:break;};myState.selection = move(myState.selection,dx,dy);dragdrop_reply[myState.selection.object_cnt] = {object_cnt:myState.selection.object_cnt,type:myState.selection.onclick,x:myState.selection.x[0],y:myState.selection.y[0],r:myState.selection.w[0],angle:myState.selection.angle};myState.valid = false;myState.draw();myState.valid = true;};};myState.selection = null;myState.dragging = false;};\
  function mousemove(e){if(myState.dragging && e.button == 0 ){var dx,dy;var mouse = getMouse(e,canvas);var xy = multisnap_check(mouse.x,mouse.y,myState.selection.use_snap);switch(myState.selection.drag_type){case 0: dx = xy[0] - myState.selection.x[myState.chk];dy = xy[1] - myState.selection.y[myState.chk];break;case 1: dx = xy[0] - myState.selection.x[myState.chk];dy = 0;break;case 2: dx = 0;dy = xy[1] - myState.selection.y[myState.chk];break;default:break;};myState.selection = move(myState.selection,dx,dy);};myState.valid = false;};this.interval = 30;setInterval(function() { myState.draw(); }, myState.interval);\
 };\
 CanvasState.prototype.addShape = function(shape){this.shapes.push(shape);this.valid = false;};\
 CanvasState.prototype.clear = function(){this.ctx.clearRect(0, 0, this.width, this.height);};\
 CanvasState.prototype.draw = function(){if(this.valid == false ){var shapes = this.shapes;this.clear();var l = shapes.length;var shape;for(var i = 0; i < l; i++){shape = shapes[i];shape.draw(this.ctx);};this.valid = true;};};var dragstuff = new CanvasState(obj,container_div);\
 ",canvas_root_id,DRAG_CANVAS,canvas_root_id);

 /*
 dragdrop + onclick for 'dragstuff', command 'copy' and external 'xml/mathml/javascript objects'
 only the FIRST x,y value-pair is used for the reply !!
 */
  if(reply_format == 100){/* this reply is raw javascript giving ALL object/properties */
    fprintf(js_include_file,"\n/* add reply stuff */\
    CanvasState.prototype.read_dragdrop = function(props){\
    if( wims_status == 'done' || use_dragdrop_reply == -1 ){return null;};\
    if( typeof(props) !== 'object' ){var props = ['clicked','text','angle'];};\
    var s = props.length;\
    var obj = dragstuff.shapes;\
    var total = '';var rep = '';\
    for(var pr = 0 ; pr < s ; pr++){\
     var prop = props[pr];\
     Object.getOwnPropertyNames(obj).forEach(\
      function(val, idx, array){ var tmp = obj[val];\
       Object.getOwnPropertyNames(tmp).forEach(\
        function(val, idx, array){\
         if(val == prop){rep = rep+':'+val+'='+tmp[val]+'\\t';}\
        }\
       );\
       total = total+tmp['object_cnt']+rep+'\\n';rep='';\
      }\
     );\
    };\
    return total;\
   };read_dragdrop%d = dragstuff.read_dragdrop;",canvas_root_id);
    } else  {
  /* dragdrop will only report the first x/y eg x[0]:y[0]:radius[0]:angle[0]*/
      fprintf(js_include_file,"\n/* add reply stuff */\
   function set_precision(val){val = (Math.round(dragdrop_precision*val))/dragdrop_precision;if(Number.isNaN(val) ){return 'NaN';}else{return val;};};\
   CanvasState.prototype.read_dragdrop = function(){\
    if( wims_status == \"done\" || use_dragdrop_reply == -1 ){return null;};\
    var len = dragdrop_reply.length;\
    var idx,x,y;\
    if(dragdrop_precision == 0 ){dragdrop_precision = 100;};\
    var total = new Array();\
    for(var p = 0 ; p < len ; p++){\
     var obj = dragdrop_reply[p];\
     idx = obj.object_cnt;\
     if( typeof(idx) === 'number' ){\
      if(obj.type == 1){\
       total[idx] = obj.clicked;\
      }else{\
       if(obj.type !=0 ){\
        if( typeof(obj.x) === 'number' ){x = obj.x;}else{x = obj.x[0];};\
        if( typeof(obj.y) === 'number' ){y = obj.y;}else{y = obj.y[0];};\
        total[idx] = set_precision(px2x(x))+':'+set_precision(px2y(y))+':'+obj.r+':'+set_precision(obj.angle);\
       };\
      };\
     };\
    };\
    return total;\
   };read_dragdrop%d = dragstuff.read_dragdrop;",canvas_root_id);
    }
  }
  else /* use_dragstuff == 1 : no mouse code ! */
  {
    fprintf(js_include_file,"\n/* no mouse actions needed */\
  var container_div = document.getElementById(\"canvas_div%d\");\
  function CanvasState(canvas,container_div){this.canvas = canvas;this.width = canvas.width;this.height = canvas.height;var ctx = canvas.getContext(\"2d\");this.ctx = ctx;this.shapes = [];this.moved = [];this.dragging = false;this.selection = null;this.draw();};\
  CanvasState.prototype.addShape = function(shape){this.shapes.push(shape);this.draw();};\
  CanvasState.prototype.clear = function(){this.ctx.clearRect(0, 0, this.width, this.height);};\
  CanvasState.prototype.draw = function(){var shapes = this.shapes;this.clear();var l = shapes.length;var shape;for(var i = 0; i < l; i++){shape = shapes[i];shape.draw(this.ctx);};};var obj = create_canvas%d(%d,xsize,ysize);\
  var dragstuff = new CanvasState(obj,container_div);",canvas_root_id,canvas_root_id,DRAG_CANVAS);
    if(reply_format != -1 ){ fprintf(js_include_file,"\n/* add reply stuff */read_dragdrop%d = dragstuff.read_dragdrop;\n",canvas_root_id);}
  }
}
