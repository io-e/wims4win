#include "canvasdraw.h"
/* used multidraw primitives : identifier in canvasmultidraw.c is index of this array */
static char multidraw_primitives[MAX_MULTI_PRIMITIVES][32] = {"point","points","circle","circles",
"line","lines","segment","segments",
"arrow","arrows","triangle","triangles",
"closedpoly","text","rect","rects",
"poly","polys","parallelogram","parallelograms",
"images","curvedarrow","curvedarrows","curvedarrow2","curvedarrows2",
"crosshair","crosshairs","function","functions"};
/*size of words "point","points",... == 6,7,..*/
static int multidraw_primitives_length[MAX_MULTI_PRIMITIVES] = {6,7,8,7,6,5,9,8,7,6,10,9,11,5,6,5,6,5,15,14,7,14,13,13,12,10,11,9,10};

void add_js_multidraw(char *draw_types,char *table_css,int use_offset,int no_controls,int crosshair_size,int use_zoom){
  int i=0;int p;int found = 0;
  size_t L0 = 1 + snprintf(NULL,0,"%s",draw_types);
  char *str = my_newmem(L0);snprintf(str,L0,"%s",draw_types);char *array[L0];
  int user_nums[MAX_MULTI_PRIMITIVES]; /* we don't know the number of different draw primitives: so assume MAX */
  int draw_nums[MAX_MULTI_PRIMITIVES];
  for(i=0;i<MAX_MULTI_PRIMITIVES;i++){draw_nums[i] = -1;user_nums[i] = -1;}
/* link the user given primitives to the internal indexes of the draw primitives */
  int equal = -1; i = 0;
  array[i] = strtok(str,",");
  int polynum = -1;
  char *pp;int safe=0;
  while(array[i]!=NULL){
    found = 0;
    if( strstr(array[i],"poly") != NULL ){/* getting the numerical argument from: poly3...polys9 */
      if( strstr(array[i],"closedpoly") == NULL && strstr(array[i],"polygon") == NULL){
        pp = array[i];
          while( *pp ){safe++;
            if(safe > MAX_MULTI_PRIMITIVES){break;}
            if( isdigit(*pp) ){ polynum = atoi(pp);if( strstr( array[i],"polys") != NULL ) {array[i] = "polys";}else{ array[i] = "poly";}break;} else { pp++;}
          }
        }
      }
      for( p = 0 ; p < MAX_MULTI_PRIMITIVES; p++){
  /* multidraw_primitives defined in canvasdraw.h */
        equal = strncmp(multidraw_primitives[p],array[i],multidraw_primitives_length[p]);
        if (equal == 0 ){
/*    fprintf(stdout,"found %s i=%d p=%d<br>",multidraw_primitives[p],i,p);*/
          draw_nums[p]= p;user_nums[p] = i;
          found=1; break;
        }
      }
      if( found == 0 ){canvas_error("unknown multidraw primitive found...typo?");}
      array[++i] = strtok(NULL,",");
    }
/* multilabel is js-array */
  if( no_controls != 1 ){ fprintf(js_include_file,"var inner_html=\"<table class='%s'>\";",table_css); }else{fprintf(js_include_file,"var inner_html = \"\";");}

 /* some default stuff */
  fprintf(js_include_file,"\n/* multidraw  */\
var canvas_userdraw = create_canvas%d(1000,xsize,ysize);var context_userdraw = canvas_userdraw.getContext(\"2d\");\
var multidraw_object_cnt = 0;\
var tooltip_div = document.getElementById(\"tooltip_placeholder_div%d\");\
function multi_snap_check(x,y,snap){switch(snap){case 1:return [snap_to_x(x),snap_to_y(y)];break;case 2:return [snap_to_x(x),y];break;case 3:return [x,snap_to_y(y)];break;case 4:return snap_to_points(x,y);break;default: return [x,y];break;};};\
function coord_split(coord){if(coord.indexOf(':') > 0 ){return coord.split(':');}else{if(coord.indexOf(';') > 0 ){return coord.split(';');}else{if(coord.indexOf(',') > 0 ){return coord.split(',');}else{alert(coord+'-- X : Y ');return;};};};};",canvas_root_id,canvas_root_id);

  if( use_zoom == 1 ){/* only use zoom when command 'zoom color' is given before command 'multidraw' */
    fprintf(js_include_file,"forbidden_zone = [%d,%d];\
function recalculate_multidraw(xmin0,xmax0,ymin0,ymax0){\
function scale_xy(type,xy){var tmp_xmin = xmin;var tmp_xmax = xmax;var tmp_ymin = ymin;var tmp_ymax = ymax;xmin=xmin0;xmax=xmax0;ymin=ymin0;ymax=ymax0;if(type == 1 ){for(var p=0;p<xy.length;p++){xy[p] = px2x(xy[p]);};}else{for(var p=0;p<xy.length;p++){xy[p] = px2y(xy[p]);};};xmin = tmp_xmin;ymin = tmp_ymin;xmax = tmp_xmax;ymax = tmp_ymax;if(type == 1){for(var p=0;p<xy.length;p++){xy[p] = x2px(xy[p]);}}else{for(var p=0;p<xy.length;p++){xy[p] = y2px(xy[p]);};};return xy;};\
function scale_multi_radius(r){for(var p = 0 ; p < r.length;p++ ){r[p] = zoom_xy[0]/xmin*r[p];};return r;};",xsize-115,ysize - 20);
    for(i=0; i < MAX_MULTI_PRIMITIVES ; i++ ){
      switch( draw_nums[i] ){
        case -1	  : break;
        case 0 ... 1   :fprintf(js_include_file,"if( points_x && points_x.length > 0 ){points_x = scale_xy(1,points_x);points_y = scale_xy(1,points_y);draw_points();};");break;
        case 2 ... 3   :fprintf(js_include_file,"if( circles_x && circles_x.length > 0 ){circles_x = scale_xy(1,circles_x);circles_y = scale_xy(1,circles_y);multi_radius = scale_multi_radius(multi_radius);draw_circles();};");break;
        case 4 ... 5   :fprintf(js_include_file,"if( lines_x && lines_x.length > 0 ){lines_x = scale_xy(1,lines_x);lines_y = scale_xy(2,lines_y);draw_lines();};");break;
        case 6 ... 7   :fprintf(js_include_file,"if( segments_x && segments_x.length > 0 ){segments_x = scale_xy(1,segments_x);segments_y = scale_xy(1,segments_y);draw_segments();};");break;
        case 8 ... 9   :fprintf(js_include_file,"if( arrows_x && arrows_x.length > 0 ){arrows_x = scale_xy(1,arrows_x);arrows_y = scale_xy(2,arrows_y);draw_arrows();};");break;
        case 10 ... 11 :fprintf(js_include_file,"if( triangles_x && triangles_x.length > 0 ){triangles_x = scale_xy(1,triangles_x);triangles_y = scale_xy(2,triangles_y);draw_triangles();};");break;
        case 14 ... 15 :fprintf(js_include_file,"if( rects_x && rects_x.length > 0 ){rects_x = scale_xy(1,rects_x);rects_y = scale_xy(2,rects_y);draw_rects();};");break;
        case 12	  :fprintf(js_include_file,"if( closedpoly_x && closedpoly_x.length > 0 ){closedpoly_x = scale_xy(1,closedpoly_x);closedpoly_y = scale_xy(2,closedpoly_y);draw_closedpoly();};");break;
        case 13	  :fprintf(js_include_file,"if( text_x && text_x.length > 0 ){text_x = scale_xy(1,text_x);text_y = scale_xy(2,text_y);draw_text();};");break;
        case 16 ... 17 :fprintf(js_include_file,"if( polys_x && polys_x.length > 0 ){polys_x = scale_xy(1,polys_x);polys_y = scale_xy(2,polys_y);draw_polys();};");break;
        case 18 ... 18 :fprintf(js_include_file,"if( parallelogram_x && parallelogram_x.length > 0 ){parallelogram_x = scale_xy(1,parallelogram_x);parallelogram_y = scale_xy(2,parallelogram_y);draw_parallelogram();};");break;
        case 20	  :fprintf(js_include_file,"if( images_x && images_x.length > 0 ){images_x = scale_xy(1,images_x);images_y = scale_xy(2,images_y);draw_images();};");break;
        case 21 ... 22 :fprintf(js_include_file,"if( curvedarrows_x && curvedarrows_x.length > 0 ){curvedarrows_x = scale_xy(1,curvedarrows_x);curvedarrows_y = scale_xy(2,curvedarrows_y);draw_curvedarrows();};");break;
        case 23 ... 24 :fprintf(js_include_file,"if( curvedarrows2_x && curvedarrows2_x.length > 0 ){curvedarrows2_x = scale_xy(1,curvedarrows2_x);curvedarrows2_y = scale_xy(2,curvedarrows2_y);draw_curvedarrows2();};");break;
        case 25 ... 26 :fprintf(js_include_file,"if( crosshairs_x && crosshairs_x.length > 0 ){crosshairs_x = scale_xy(1,crosshairs_x);crosshairs_y = scale_xy(1,crosshairs_y);draw_crosshairs();};");break;
        case 27:break;
        default	  : break;
      }
    }
    fprintf(js_include_file," return;};");
  }
 /* begin user_draw() */
  fprintf(js_include_file,"function user_draw(evt){\
 if(evt.button == 3){clear_draw_area%d(userdraw_primitive,0);return;};\
 var mouse = getMouse(evt,canvas_userdraw);\
 var x = mouse.x;var y = mouse.y;\
 user_is_dragging = false;\
 if(x>forbidden_zone[0] && y>forbidden_zone[1]){console.log('drawing in zoom area...');return;};\
 switch(userdraw_primitive){",canvas_root_id);
  for(i=0; i < MAX_MULTI_PRIMITIVES ; i++ ){
    switch( draw_nums[i] ){
      case -1: break;
      case 0:fprintf(js_include_file,"case 0: points(x,y,0,0);break;");break;
      case 1:fprintf(js_include_file,"case 1: points(x,y,0,1);break;");break;
      case 2:fprintf(js_include_file,"case 2: circles(x,y,0,0);break;");break;
      case 3:fprintf(js_include_file,"case 3: circles(x,y,0,1);break;");break;
      case 4:fprintf(js_include_file,"case 4: lines(x,y,0,0);break;");break;
      case 5:fprintf(js_include_file,"case 5: lines(x,y,0,1);break;");break;
      case 6:fprintf(js_include_file,"case 6: segments(x,y,0,0);break;");break;
      case 7:fprintf(js_include_file,"case 7: segments(x,y,0,1);break;");break;
      case 8:fprintf(js_include_file,"case 8: arrows(x,y,0,0);break;");break;
      case 9:fprintf(js_include_file,"case 9: arrows(x,y,0,1);break;");break;
      case 10:fprintf(js_include_file,"case 10: triangles(x,y,0,0);break;");break;
      case 11:fprintf(js_include_file,"case 11: triangles(x,y,0,1);break;");break;
      case 12:fprintf(js_include_file,"case 12: closedpoly(x,y,0,0);break;");break;
      case 13:fprintf(js_include_file,"case 13: text(x,y,0,1);break;");break;
      case 14:fprintf(js_include_file,"case 14: rects(x,y,0,0);break;");break;
      case 15:fprintf(js_include_file,"case 15: rects(x,y,0,1);break;");break;
      case 16:fprintf(js_include_file,"case 16: polys(x,y,0,0);break;");break;
      case 17:fprintf(js_include_file,"case 17: polys(x,y,0,1);break;");break;
      case 18:fprintf(js_include_file,"case 18: parallelogram(x,y,0,0);break;");break;
      case 19:fprintf(js_include_file,"case 19: parallelogram(x,y,0,1);break;");break;
      case 20:fprintf(js_include_file,"case 20: images(x,y,0,1);break;");break;
      case 21:fprintf(js_include_file,"case 21: curvedarrows(x,y,0,0);break;");break;
      case 22:fprintf(js_include_file,"case 22: curvedarrows(x,y,0,1);break;");break;
      case 23:fprintf(js_include_file,"case 23: curvedarrows2(x,y,0,0);break;");break;
      case 24:fprintf(js_include_file,"case 24: curvedarrows2(x,y,0,1);break;");break;
      case 25:fprintf(js_include_file,"case 25: crosshairs(x,y,0,0);break;");break;
      case 26:fprintf(js_include_file,"case 26: crosshairs(x,y,0,1);break;");break;
      case 27:fprintf(js_include_file,"case 27:break;");break;
      default: break;
    }
  }
  fprintf(js_include_file,"default:break;};return;};");
 /* end user_draw()*/

/* should we use a fillpattern ? */

 /* begin user_drag() */
  fprintf(js_include_file,"function user_drag(evt){\
 var mouse = getMouse(evt,canvas_userdraw);\
 var x = mouse.x;var y = mouse.y;\
 user_is_dragging = true;\
 if(x>forbidden_zone[0] && y>forbidden_zone[1]){console.log('drawing in zoom area...');return;};\
 switch(userdraw_primitive){");
  for(i=0; i < MAX_MULTI_PRIMITIVES ; i++ ){
    switch( draw_nums[i] ){
      case -1: break;
      case 0:fprintf(js_include_file,"case 0: break;");break;
      case 1:fprintf(js_include_file,"case 1: break;");break;
      case 2:fprintf(js_include_file,"case 2: circles(x,y,1,0);break;");break;
      case 3:fprintf(js_include_file,"case 3: circles(x,y,1,1);break;");break;
      case 4:fprintf(js_include_file,"case 4: lines(x,y,1,0);break;");break;
      case 5:fprintf(js_include_file,"case 5: lines(x,y,1,1);break;");break;
      case 6:fprintf(js_include_file,"case 6: segments(x,y,1,0);break;");break;
      case 7:fprintf(js_include_file,"case 7: segments(x,y,1,1);break;");break;
      case 8:fprintf(js_include_file,"case 8: arrows(x,y,1,0);break;");break;
      case 9:fprintf(js_include_file,"case 9: arrows(x,y,1,1);break;");break;
      case 10:fprintf(js_include_file,"case 10: triangles(x,y,1,0);break;");break;
      case 11:fprintf(js_include_file,"case 11: triangles(x,y,1,1);break;");break;
      case 12:fprintf(js_include_file,"case 12: closedpoly(x,y,1,0);break;");break;
      case 13:fprintf(js_include_file,"case 13: break;");break;
      case 14:fprintf(js_include_file,"case 14: rects(x,y,1,0);break;");break;
      case 15:fprintf(js_include_file,"case 15: rects(x,y,1,1);break;");break;
      case 16:fprintf(js_include_file,"case 16: polys(x,y,1,0);break;");break;
      case 17:fprintf(js_include_file,"case 17: polys(x,y,1,1);break;");break;
      case 18:fprintf(js_include_file,"case 18: parallelogram(x,y,1,0);break;");break;
      case 19:fprintf(js_include_file,"case 19: parallelogram(x,y,1,1);break;");break;
      case 20:fprintf(js_include_file,"case 20: images(x,y,1,1);break;");break;
      case 21:fprintf(js_include_file,"case 21: curvedarrows(x,y,1,0);break;");break;
      case 22:fprintf(js_include_file,"case 22: curvedarrows(x,y,1,1);break;");break;
      case 23:fprintf(js_include_file,"case 23: curvedarrows2(x,y,1,0);break;");break;
      case 24:fprintf(js_include_file,"case 24: curvedarrows2(x,y,1,1);break;");break;
      case 25:fprintf(js_include_file,"case 25: break;");break;
      case 26:fprintf(js_include_file,"case 26: break;");break;
      case 27:fprintf(js_include_file,"case 27: break;");break;
      default:break;
    }
  }
  fprintf(js_include_file,"default:break; };return;};");
 /* end user_drag() */
/* 17/1/2021 corrected syntax issue signalled by Opera Presto : 'forbidden function user_drawstop(evt) declaration in statement'*/
  fprintf(js_include_file,"if(wims_status != \"done\"){\
 canvas_div.addEventListener('mousedown',user_draw,false);\
 canvas_div.addEventListener('mousemove',user_drag,false);\
 canvas_div.addEventListener('touchstart'  , function(e) { e.preventDefault(); user_draw(e.changedTouches[0]);},false);\
 canvas_div.addEventListener('touchmove'  , function(e) { e.preventDefault(); user_drag(e.changedTouches[0]);},false);\
 canvas_div.addEventListener('touchend'  , function(e) { e.preventDefault(); user_drawstop(e.changedTouches[0]);},false);\
 var user_is_dragging = false;\
 var user_drawstop = function(evt){\
  if(!user_is_dragging){user_drag(evt);return;};\
  if(user_is_dragging){user_draw(evt);return;};\
 };\
};");

/* add all stuff needed to draw the selected primitives... */
  int u;
  for(u=0; u < MAX_MULTI_PRIMITIVES ; u++ ){
    i = user_nums[u];
  /*
  u = index of technical draw primitive
  i = index of user defined draw primitive
  */
    switch( draw_nums[u] ){
  /* point/points */
      case -1 : break;
      case 0 ... 1:
        fprintf(js_include_file,"function points(x,y,event_which,num){\
   if(event_which == 1){ return; };\
   var xy = multi_snap_check(x,y,points_snap);\
   if( num == 0 ){\
    points_x[0] = xy[0];\
    points_y[0] = xy[1];\
   } else {\
    points_x.push(xy[0]);\
    points_y.push(xy[1]);\
   };\
   draw_points();\
  };\
  function draw_points(){\
   var radius = 2*(context_points.lineWidth);\
   context_points.clearRect(0,0,xsize,ysize);\
   for(var p = 0 ; p < points_x.length ; p++ ){\
    context_points.beginPath();\
    context_points.arc(points_x[p],points_y[p],radius,0,2*Math.PI,false);\
    context_points.closePath();\
    context_points.fill();\
   };\
  };\
  var canvas_points = create_canvas%d(100%d,xsize,ysize);var context_points = canvas_points.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_points.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  context_points.fillStyle = context_points.strokeStyle;\
  context_points.lineWidth = multilinewidth[%d];if(multilinewidth[%d] %%2 == 1){ context_points.translate(0.5,0.5);};\
  var points_x = new Array();var points_y = new Array();\
  var points_snap = multisnaptogrid[%d];",canvas_root_id,i,i,i,i,i,i,i,i,i,i,i,i);
        if( no_controls != 1){
          fprintf(js_include_file,"\
   inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1 ){inner_html+=\"<td>(<input type='text' size='5' value='' id='input_points_x'>:<input type='text' size='5' value='' id='input_points_y'>)</td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_points_x,input_points_y,null);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
        } else {
          fprintf(js_include_file,"userdraw_primitive = %d;",u);
        }
        break;
  /* circle/circles */
      case 2 ... 3 :
        fprintf(js_include_file,"function circles(x,y,event_which,num){\
   var xy = multi_snap_check(x,y,circles_snap);\
   var last = circles_x.length - 1;\
   var xc = circles_x[last];\
   var yc = circles_y[last];\
   if(event_which == 0){\
    if( multidraw_object_cnt == 0 ){\
     if( num  == 0 ){\
       circles_x[0]=xy[0];circles_y[0]=xy[1];multi_radius[0]=4;\
     } else {\
       circles_x.push(xy[0]);circles_y.push(xy[1]);multi_radius.push(4);\
     };\
    };\
    multidraw_object_cnt++;\
   } else {\
    if( multidraw_object_cnt == 1 ){\
     multi_radius[last] = parseInt(Math.sqrt( (xy[0] - xc)*(xy[0] - xc) + (xy[1] - yc)*(xy[1] - yc) ));\
    };\
   };\
   if( multidraw_object_cnt == 2 ){\
    multidraw_object_cnt = 0;\
    if( num == 0 ){\
     circles_x = [];circles_y = [];\
     circles_x[0] = xc;circles_y[0] = yc;\
    };\
   };\
   draw_circles();\
  };function draw_circles(){\
   context_circles.clearRect(0,0,xsize,ysize);\
   for(var p = 0 ; p < circles_x.length ; p++ ){\
    context_circles.beginPath();\
    context_circles.arc(circles_x[p],circles_y[p],multi_radius[p],0,2*Math.PI,false);\
    context_circles.closePath();\
    context_circles.fill();\
    context_circles.stroke();\
   };\
   return;\
  };var canvas_circles = create_canvas%d(100%d,xsize,ysize);var context_circles = canvas_circles.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_circles.lineWidth = multilinewidth[%d];\
  if(multilinewidth[%d]%%2 == 1){ context_circles.translate(0.5,0.5);};\
  context_circles.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  if(multifill[%d] != 0 ){var my_fill_color=\"rgba(\"+multifillcolors[%d]+\",\"+multifillopacity[%d]+\")\";if( multifill[%d] > 1 ){context_circles.fillStyle = create_Pattern(0,0,parseInt(multifill[%d]),my_fill_color);}else{context_circles.fillStyle = my_fill_color;};}else{context_circles.fillStyle = \"rgba( 255,255,255,0)\";};\
  if(multidash[%d] == 1 ){ if( context_circles.setLineDash ){context_circles.setLineDash([2,4]);}else{if(context_circles.mozDash){context_circles.mozDash = [2,4]};};};\
  var circles_x = new Array();var circles_y = new Array();var multi_radius = new Array();\
  var circles_snap = multisnaptogrid[%d];",canvas_root_id,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i);

    if(no_controls != 1 ){  /* for BPR...*/
      fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1 ){inner_html+=\"<td>M:(<input type='text' size='3' value='' id='input_circles_x'> : <input type='text' size='3' value='' id='input_circles_y'>) R:<input type='text' size='3' value='' id='input_circles_r'></td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_circles_x,input_circles_y,input_circles_r);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
        } else {
          fprintf(js_include_file,"userdraw_primitive = %d;",u);
        }
        break;
  /* line/lines */
      case 4 ... 5 :
        fprintf(js_include_file,"function calc_lines(){\
   var marge = 2;var len = lines_x.length;\
   var tmp_x = new Array(len);\
   var tmp_y = new Array(len);\
   var pp;\
   for(var p = 0 ; p < len ; p = p+2){\
    pp = p+1;\
    if(lines_x[p] < lines_x[pp]+marge && lines_x[p] > lines_x[pp]-marge){\
     tmp_x[p] = lines_x[p];tmp_x[pp] = lines_x[pp];\
     tmp_y[p] = 0;tmp_y[pp] = ysize;\
    } else {\
     if(lines_y[p] < lines_y[pp]+marge && lines_y[p] > lines_y[pp]-marge){\
      tmp_x[p] = 0;tmp_x[pp] = xsize;\
      tmp_y[p] = lines_y[p];tmp_y[pp] = lines_y[pp];\
     } else {\
      tmp_x[p] = 0;tmp_x[pp] = xsize;\
      tmp_y[p] = lines_y[p] - (lines_x[p])*(lines_y[pp] - lines_y[p])/(lines_x[pp] - lines_x[p]);\
      tmp_y[pp] = lines_y[p] + (xsize - lines_x[p])*(lines_y[pp] - lines_y[p])/(lines_x[pp] - lines_x[p]);\
     };\
    };\
   };\
   return {x:tmp_x,y:tmp_y};\
  };function lines(x,y,event_which,num){\
   var xy = multi_snap_check(x,y,lines_snap);\
   if(event_which == 0){\
    if( num == 0 && multidraw_object_cnt == 0 ){lines_x = [];lines_y = [];};\
    lines_x.push(xy[0]);lines_y.push(xy[1]);\
    multidraw_object_cnt++;\
   } else {\
    if( multidraw_object_cnt == 1 ){\
     lines_x.push(xy[0]);lines_y.push(xy[1]);\
     draw_lines();\
     lines_x.pop();lines_y.pop();\
    };\
   };\
   if( multidraw_object_cnt == 2 ){\
    multidraw_object_cnt = 0;\
    draw_lines();\
   };\
  };function draw_lines(){\
   var len = lines_x.length;\
   if( len %%2 == 0 ){\
    var xy = calc_lines();\
    context_lines.clearRect(0,0,xsize,ysize);\
    for(var p = 0 ; p < len ; p = p+2 ){\
     context_lines.beginPath();\
     context_lines.moveTo(xy.x[p],xy.y[p]);\
     context_lines.lineTo(xy.x[p+1],xy.y[p+1]);\
     context_lines.closePath();\
     context_lines.stroke();\
    };\
   };\
   return;\
  };var canvas_lines = create_canvas%d(100%d,xsize,ysize);var context_lines = canvas_lines.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_lines.lineWidth = multilinewidth[%d];if(multilinewidth[%d]%%2 == 1){ context_lines.translate(0.5,0.5);};\
  context_lines.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  if(multidash[%d] == 1 ){ if( context_lines.setLineDash ){context_lines.setLineDash([2,4]);}else{\
  if(context_lines.mozDash){context_lines.mozDash = [2,4]};};};\
  var lines_x = new Array();var lines_y = new Array();var lines_snap = multisnaptogrid[%d];",canvas_root_id,u,i,i,i,i,i,i,i,i,i,i,i,i);

      if( no_controls != 1 ){  /* for BPR...*/
        fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1 ){inner_html+=\"<td>(<input type='text' size='5' value='x1 : y1' id='input_lines_x' style='text-align:center;'>) --- ( <input type='text' size='5' value='x2 : y2' id='input_lines_y' style='text-align:center;'> )</td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_lines_x,input_lines_y,null);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
        fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
  /* segment/segments */
    case 6 ... 7 :
      fprintf(js_include_file,"function segments(x,y,event_which,num){\
   var xy = multi_snap_check(x,y,segments_snap);\
   if(event_which == 0){\
    if( num == 0 && multidraw_object_cnt == 0 ){segments_x = [];segments_y = [];};\
    segments_x.push(xy[0]);segments_y.push(xy[1]);\
    multidraw_object_cnt++;\
   } else {\
    if( multidraw_object_cnt == 1 ){\
     segments_x.push(xy[0]);segments_y.push(xy[1]);\
     draw_segments();\
     segments_x.pop();segments_y.pop();\
    };\
   };\
   if( multidraw_object_cnt == 2 ){\
    multidraw_object_cnt = 0;\
    draw_segments();\
   };\
  };function draw_segments(){\
   var len = segments_x.length;\
   if( len%%2 == 0 ){\
    context_segments.clearRect(0,0,xsize,ysize);\
    for(var p = 0 ; p < len ; p = p+2 ){\
     context_segments.beginPath();\
     context_segments.moveTo(segments_x[p],segments_y[p]);\
     context_segments.lineTo(segments_x[p+1],segments_y[p+1]);\
     context_segments.closePath();\
     context_segments.stroke();\
    };\
   };\
   return;\
  };var canvas_segments = create_canvas%d(100%d,xsize,ysize);var context_segments = canvas_segments.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_segments.lineWidth = multilinewidth[%d];\
  if(multilinewidth[%d]%%2 == 1){ context_segments.translate(0.5,0.5);};\
  context_segments.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  if(multidash[%d] == 1 ){ if( context_segments.setLineDash ){context_segments.setLineDash([2,4]);}\
  else{if(context_segments.mozDash){context_segments.mozDash = [2,4]};};};\
  var segments_x = new Array();var segments_y = new Array();var segments_snap = multisnaptogrid[%d];",
  canvas_root_id,u,i,i,i,i,i,i,i,i,i,i,i,i);

      if( no_controls != 1 ){  /* for BPR...*/
        fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1 ){inner_html+=\"<td>(<input type='text' size='5' value='x1 : y1' id='input_segments_x' style='text-align:center;'>) --- ( <input type='text' size='5' value='x2 : y2' id='input_segments_y' style='text-align:center;'>)</td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_segments_x,input_segments_y,null);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
      fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
  /* arrow/arrows */
    case 8 ... 9 :
      fprintf(js_include_file,"function arrows(x,y,event_which,num){\
   var xy = multi_snap_check(x,y,arrows_snap);\
   if(event_which == 0){\
    if( num == 0 && multidraw_object_cnt == 0 ){arrows_x = [];arrows_y = [];};\
    arrows_x.push(xy[0]);arrows_y.push(xy[1]);\
    multidraw_object_cnt++;\
   } else {\
    if( multidraw_object_cnt == 1 ){\
     arrows_x.push(xy[0]);arrows_y.push(xy[1]);\
     draw_arrows();\
     arrows_x.pop();arrows_y.pop();\
    };\
   };\
   if( multidraw_object_cnt == 2 ){\
    multidraw_object_cnt = 0;\
    draw_arrows();\
   };\
  };function draw_arrows(){\
   var len = arrows_x.length;\
   var x1,y1,x2,y2,dx,dy,h;\
   if( len%%2 == 0 ){\
    context_arrows.clearRect(0,0,xsize,ysize);\
    for(var p = 0 ; p < len ; p = p+2 ){\
     x1 = arrows_x[p];y1 = arrows_y[p];x2 = arrows_x[p+1];y2 = arrows_y[p+1];dx = x2 - x1;dy = y2 - y1;\
     h = Math.sqrt(dx*dx+dy*dy);\
     context_arrows.save();\
     context_arrows.setLineDash([]);\
     context_arrows.translate(x2,y2);\
     context_arrows.rotate(Math.atan2(dy,dx));\
     context_arrows.beginPath();\
     context_arrows.moveTo(0,0);\
     context_arrows.lineTo(-1*arrow_head,-0.5*arrow_head);\
     context_arrows.lineTo(-1*arrow_head, 0.5*arrow_head);\
     context_arrows.closePath();\
     context_arrows.fill();\
     context_arrows.stroke();\
     context_arrows.restore();\
     context_arrows.beginPath();\
     context_arrows.moveTo(x1,y1);\
     context_arrows.lineTo(x2,y2);\
     context_arrows.closePath();\
     context_arrows.stroke();\
    };\
   };\
   return;\
  };var canvas_arrows = create_canvas%d(100%d,xsize,ysize);var context_arrows =  canvas_arrows.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  if(multidash[%d] == 1 ){ if( context_arrows.setLineDash ){context_arrows.setLineDash([2,4]);}else{if(context_arrows.mozDash){context_arrows.mozDash = [2,4]};};};\
  context_arrows.lineWidth = multilinewidth[%d];if(multilinewidth[%d]%%2 == 1){ context_arrows.translate(0.5,0.5);};\
  context_arrows.lineCap = \"round\";\
  context_arrows.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  context_arrows.fillStyle = context_arrows.strokeStyle;\
  var arrows_x = new Array();var arrows_y = new Array();var arrows_snap = multisnaptogrid[%d];",
  canvas_root_id,u,i,i,i,i,i,i,i,i,i,i,i,i);

      if( no_controls != 1 ){  /* for BPR...*/
        fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1){inner_html+=\"<td>(<input type='text' size='5' value='x1 : y1' id='input_arrows_x' style='text-align:center;'><b>) --- (</b> <input type='text' size='5' value='x2 : y2' id='input_arrows_y' style=';text-align:center;'>)</td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_arrows_x,input_arrows_y,null);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
        fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
  /* triangle/triangles */
    case 10 ... 11:
      fprintf(js_include_file,"function triangles(x,y,event_which,num){\
   var xy = multi_snap_check(x,y,triangles_snap);\
   var last = triangles_x.length - 1;\
   if(event_which == 0){\
    if(num == 0 && multidraw_object_cnt == 0){\
     triangles_x = [];triangles_y = [];\
     triangles_x[0] = xy[0];triangles_y[0] = xy[1];\
    } else {\
     triangles_x.push(xy[0]);triangles_y.push(xy[1]);\
    };\
    multidraw_object_cnt++;\
   } else {\
    if( multidraw_object_cnt < 3 ){\
     triangles_x.push(xy[0]);triangles_y.push(xy[1]);\
     draw_triangles();\
     triangles_x.pop();triangles_y.pop();\
    };\
   };\
   if( multidraw_object_cnt == 3 ){\
    triangles_x.pop();triangles_y.pop();\
    triangles_x.push(xy[0]);triangles_y.push(xy[1]);\
    multidraw_object_cnt = 0;\
    draw_triangles();\
   };\
  };function draw_triangles(){\
   var len = triangles_x.length - 1;\
   context_triangles.clearRect(0,0,xsize,ysize);\
   for(var p = 0 ; p < len ; p = p+3){\
    context_triangles.beginPath();\
    context_triangles.moveTo(triangles_x[p],triangles_y[p]);\
    for( var m = p+1 ;m < p+3 ; m++){\
     context_triangles.lineTo(triangles_x[m],triangles_y[m]);\
    };\
    context_triangles.lineTo(triangles_x[p],triangles_y[p]);\
    context_triangles.closePath();\
    context_triangles.fill();\
    context_triangles.stroke();\
   };\
   return;\
  };var canvas_triangles = create_canvas%d(10%d,xsize,ysize);var context_triangles = canvas_triangles.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2);};\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_triangles.lineCap = \"round\";context_triangles.lineWidth = multilinewidth[%d];\
  if(multilinewidth[%d]%%2 == 1){ context_triangles.translate(0.5,0.5);};\
  context_triangles.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  if(multifill[%d] != 0 ){var my_fill_color=\"rgba(\"+multifillcolors[%d]+\",\"+multifillopacity[%d]+\")\";if( multifill[%d] > 1 ){context_triangles.fillStyle = create_Pattern(0,0,parseInt(multifill[%d]),my_fill_color);}else{context_triangles.fillStyle = my_fill_color;};}else{context_triangles.fillStyle = \"rgba( 255,255,255,0)\";};\
  if(multidash[%d] == 1 ){ if( context_triangles.setLineDash ){\
  context_triangles.setLineDash([2,4]);}else{if(context_triangles.mozDash){context_triangles.mozDash = [2,4]};};};\
  var triangles_x = new Array();var triangles_y = new Array();var triangles_snap = multisnaptogrid[%d];",
  canvas_root_id,u,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i);
      if(no_controls != 1 ){  /* for BPR...*/
   fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1 ){inner_html+=\"<td><b>(<input type='text' size='5' value='x1 : y1' id='input_triangles_x'>) -- (<input type='text' size='5' value='x2 : y2' id='input_triangles_y'>) -- (<input type='text' size='5' value='x3 : y3' id='input_triangles_r'>)</b></td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_triangles_x,input_triangles_y,input_triangles_r);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
        fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
  /* closedpoly */
    case 12 :
      fprintf(js_include_file,"function check_closed(x1,y1,X,Y){\
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
  };function closedpoly(x,y,event_which,num){\
   var xy = multi_snap_check(x,y,closedpoly_snap);\
   if(event_which == 0){\
    if(multidraw_object_cnt == 0){\
     closedpoly_x = [];closedpoly_y = [];\
     closedpoly_x[0] = xy[0];closedpoly_y[0] = xy[1];\
    } else {\
     closedpoly_x.push(xy[0]);closedpoly_y.push(xy[1]);\
    };\
    multidraw_object_cnt++;\
    if( multidraw_object_cnt > 2 ){\
     if( check_closed(x,y,closedpoly_x,closedpoly_y) == 1){\
      draw_closedpoly();\
      multidraw_object_cnt = 0;\
     };\
    };\
   } else {\
    if( multidraw_object_cnt > 0 ){\
     closedpoly_x.push(xy[0]);closedpoly_y.push(xy[1]);\
     draw_closedpoly();\
     closedpoly_x.pop();closedpoly_y.pop();\
    };\
   };\
  };function draw_closedpoly(){\
   var len = closedpoly_x.length;\
   context_closedpoly.clearRect(0,0,xsize,ysize);\
   var p = 0;\
   context_closedpoly.beginPath();\
   context_closedpoly.moveTo(closedpoly_x[0],closedpoly_y[0]);\
   for(var p = 1 ; p < len ; p++){\
    context_closedpoly.lineTo(closedpoly_x[p],closedpoly_y[p]);\
   };\
   context_closedpoly.lineTo(closedpoly_x[0],closedpoly_y[0]);\
   context_closedpoly.closePath();\
   context_closedpoly.fill();\
   context_closedpoly.stroke();\
   return;\
  };var canvas_closedpoly = create_canvas%d(10%d,xsize,ysize);\
  var context_closedpoly =  canvas_closedpoly.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_closedpoly.lineCap = \"round\";context_closedpoly.lineWidth = multilinewidth[%d];\
  if(multilinewidth[%d]%%2 == 1){ context_closedpoly.translate(0.5,0.5);};context_closedpoly.lineCap = \"round\";\
  context_closedpoly.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  if(multifill[%d] != 0 ){var my_fill_color=\"rgba(\"+multifillcolors[%d]+\",\"+multifillopacity[%d]+\")\";if( multifill[%d] > 1 ){context_closedpoly.fillStyle = create_Pattern(0,0,parseInt(multifill[%d]),my_fill_color);}else{context_closedpoly.fillStyle = my_fill_color;};}else{context_closedpoly.fillStyle = \"rgba( 255,255,255,0)\";};\
  if(multidash[%d] == 1 ){ if( context_closedpoly.setLineDash ){context_closedpoly.setLineDash([2,4]);}else{\
  if(context_closedpoly.mozDash){context_closedpoly.mozDash = [2,4]};};};\
  var closedpoly_x = new Array();var closedpoly_y = new Array();\
  var closedpoly_snap = multisnaptogrid[%d];",
  canvas_root_id,u,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i);

      if( no_controls != 1 ){  /* for BPR...*/
        fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1){inner_html+=\"<td>(<input type='text' size='5' value='x1:x2:x3:...' id='input_closedpoly_x' style='text-align:center;'><b>) --- (</b> <input type='text' size='5' value='y1:y2:y3:...' id='input_closedpoly_y' style='text-align:center;'>)</td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_closedpoly_x,input_closedpoly_y,null);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
        fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
  /* text : always uses user input field !! */
    case 13:
      fprintf(js_include_file,"function text(x,y,event_which,num){\
   if(event_which == 1){ return; };\
   var xy = multi_snap_check(x,y,text_snap);\
   if( num == 0 ){\
    text_x[0] = xy[0];\
    text_y[0] =xy[1];\
   } else {\
    text_x.push(xy[0]);\
    text_y.push(xy[1]);\
   };\
   draw_text();\
  };function conv_to_unicode(str){\
   return str.replace(/\\u[\\dA-F]{4}/gi,function(match){\
   return String.fromCharCode(parseInt(match.replace(/\\u/g,''), 16));});\
  };function draw_text(){\
   var half = 0;\
   var height = 0.3 * (context_text.measureText('M').width);\
   context_text.clearRect(0,0,xsize,ysize);\
   for(var p = 0 ; p < text_x.length ; p++ ){\
    if( typeof(text_abc[p]) === 'undefined'){\
     var txt = conv_to_unicode(document.getElementById('input_text_r').value);\
     text_abc.push(txt);\
    };\
    half = 0.5*( context_text.measureText(text_abc[p]).width );\
    if(text_abc[p].indexOf('_') > 0 || text_abc[p].indexOf('^') > 0 ){\
    draw_subsup(context_text,text_x[p],text_y[p],text_abc[p],4);}else{context_text.fillText(text_abc[p],text_x[p] - half,text_y[p] + height);};\
   };\
  };var canvas_text = create_canvas%d(10%d,xsize,ysize);\
  var context_text = canvas_text.getContext(\"2d\");\
  context_text.font = multifont_family;\
  context_text.fillStyle = \"rgba(\"+multifont_color+\",\"+multistrokeopacity[%d]+\")\";\
  var text_snap = multisnaptogrid[%d];\
  var text_x = new Array();var text_y = new Array(); var text_abc = new Array();",canvas_root_id,u,i,i);

      if( no_controls != 1 ){  /* for BPR...*/
        fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt=0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td><td><input type='text' size='6' value='' id='input_text_r'>\";if( multiuserinput[%d] == 1){inner_html+=\"(<input type='text' size='2' value='x' id='input_text_x' style='text-align:center;'>:<input type='text' size='2' value='y' id='input_text_y' style='text-align:center;'>)</td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_text_x,input_text_y,input_text_r);' value='OK'></td>\";}else{inner_html+=\"</td>\";};",u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
        fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      fprintf(js_include_file,"inner_html+=\"</tr>\";");
      break;
  /* rect/rects */
    case 14 ... 15 :
      fprintf(js_include_file,"function rects(x,y,event_which,num){\
   var xy = multi_snap_check(x,y,rects_snap);\
   if(event_which == 0){\
    if( num == 0 && multidraw_object_cnt == 0 ){rects_x = [];rects_y = [];};\
    rects_x.push(xy[0]);rects_y.push(xy[1]);\
    multidraw_object_cnt++;\
   } else {\
    if( multidraw_object_cnt == 1 ){\
     rects_x.push(xy[0]);rects_y.push(xy[1]);\
     draw_rects();\
     rects_x.pop();rects_y.pop();\
    };\
   };\
   if( multidraw_object_cnt == 2 ){\
    multidraw_object_cnt = 0;\
    draw_rects();\
   };\
  };function draw_rects(){\
   var len = rects_x.length;\
   if( len %%2 == 0 ){\
    context_rects.clearRect(0,0,xsize,ysize);\
    for(var p = 0 ; p < len ; p = p+2 ){\
     context_rects.beginPath();\
     context_rects.rect(rects_x[p],rects_y[p],rects_x[p+1]-rects_x[p],rects_y[p+1]-rects_y[p]);\
     context_rects.closePath();\
     context_rects.fill();\
     context_rects.stroke();\
    };\
   };\
   return;\
  };var canvas_rects = create_canvas%d(10%d,xsize,ysize);var context_rects = canvas_rects.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_rects.lineWidth = multilinewidth[%d];if(multilinewidth[%d]%%2 == 1){ context_rects.translate(0.5,0.5);};\
  context_rects.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  if(multidash[%d] == 1 ){ if( context_rects.setlineDash ){context_rects.setlineDash([2,4]);}else{\
  if(context_rects.mozDash){context_rects.mozDash = [2,4]};};};\
  if(multifill[%d] != 0 ){var my_fill_color=\"rgba(\"+multifillcolors[%d]+\",\"+multifillopacity[%d]+\")\";if( multifill[%d] > 1 ){context_rects.fillStyle = create_Pattern(0,0,parseInt(multifill[%d]),my_fill_color);}else{context_rects.fillStyle = my_fill_color;};}else{context_rects.fillStyle = \"rgba( 255,255,255,0)\";};\
  var rects_x = new Array();var rects_y = new Array();var rects_snap = multisnaptogrid[%d];",
  canvas_root_id,u,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i);

      if( no_controls != 1 ){
        fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1){inner_html+=\"<td>(<input type='text' size='5' value='x1 : y1' id='input_rects_x' style='text-align:center;'>) --- (<input type='text' size='5' value='x2 : y2' id='input_rects_y' style='text-align:center;'>)</td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_rects_x,input_rects_y,null);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
        fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
  /* poly/polys */
    case 16 ... 17 :
      fprintf(js_include_file,"var polynum = %d;function polys(x,y,event_which,num){\
   var last = polys_x.length - 1;\
   var xy = multi_snap_check(x,y,polys_snap);\
   if(event_which == 0){\
    if(num == 0 && multidraw_object_cnt == 0){\
     polys_x = [];polys_y = [];\
     polys_x[0] = xy[0];polys_y[0] = xy[1];\
    } else {\
     polys_x.push(xy[0]);polys_y.push(xy[1]);\
    };\
    multidraw_object_cnt++;\
   } else {\
    if( multidraw_object_cnt < polynum ){\
     polys_x.push(xy[0]);polys_y.push(xy[1]);\
     draw_polys();\
     polys_x.pop();polys_y.pop();\
    };\
   };\
   if( multidraw_object_cnt == polynum ){\
    polys_x.pop();polys_y.pop();\
    polys_x.push(xy[0]);polys_y.push(xy[1]);\
    multidraw_object_cnt = 0;\
    draw_polys();\
   };\
  };function draw_polys(){\
   var len = polys_x.length - 1;\
   context_polys.clearRect(0,0,xsize,ysize);\
   for(var p = 0 ; p < len ; p = p+polynum){\
    context_polys.beginPath();\
    context_polys.moveTo(polys_x[p],polys_y[p]);\
    for( var m = p+1 ;m < p+polynum ; m++){\
     context_polys.lineTo(polys_x[m],polys_y[m]);\
    };\
    context_polys.lineTo(polys_x[p],polys_y[p]);\
    context_polys.closePath();\
    context_polys.fill();\
    context_polys.stroke();\
   };\
   return;\
  };var canvas_polys = create_canvas%d(10%d,xsize,ysize);var context_polys = canvas_polys.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_polys.lineCap = \"round\";context_polys.lineWidth = multilinewidth[%d];\
  if(multilinewidth[%d]%%2 == 1){ context_polys.translate(0.5,0.5);};\
  context_polys.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  if(multifill[%d] != 0 ){var my_fill_color=\"rgba(\"+multifillcolors[%d]+\",\"+multifillopacity[%d]+\")\";if( multifill[%d] > 1 ){context_polys.fillStyle = create_Pattern(0,0,parseInt(multifill[%d]),my_fill_color);}else{context_polys.fillStyle = my_fill_color;};}else{context_polys.fillStyle = \"rgba( 255,255,255,0)\";};\
  if(multidash[%d] == 1 ){ if( context_polys.setLineDash ){context_polys.setLineDash([2,4]);}\
  else{if(context_polys.mozDash){context_polys.mozDash = [2,4]};};};\
  var polys_x = new Array();var polys_y = new Array();\
  var polys_snap = multisnaptogrid[%d];",
  polynum,canvas_root_id,u,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i);

      if( no_controls != 1 ){
        fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if(multiuserinput[%d] == 1 ){inner_html+=\"<td>(<input type='text' size='8' value='x1,x2...x_n' id='input_polys_x'> ---- <input type='text' size='8' value='y1,y2...y_n' id='input_polys_y'>)</td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_rects_x,input_rects_y,null);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
        fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
  /* parallelogram/parallelograms */
    case 18 ... 19:
      fprintf(js_include_file,"function parallelogram(x,y,event_which,num){\
   var l2 = parallelogram_x.length;\
   var l1 = l2 - 1;var l0 = l2 - 2;\
   var xy = multi_snap_check(x,y,parallelogram_snap);\
   if(event_which == 0){\
    if(multidraw_object_cnt == 0){\
     if(num == 0){parallelogram_x = [];parallelogram_y = [];};\
     parallelogram_x.push(xy[0]);parallelogram_y.push(xy[1]);\
    } else {\
     parallelogram_x.push(xy[0]);parallelogram_y.push(xy[1]);\
     if(multidraw_object_cnt == 2){\
      var xy = multi_snap_check(parallelogram_x[l2] - parallelogram_x[l1] + parallelogram_x[l0],parallelogram_y[l2] - parallelogram_y[l1] + parallelogram_y[l0],parallelogram_snap);\
      parallelogram_x.push(xy[0]);\
      parallelogram_y.push(xy[1]);\
     };\
    };\
    multidraw_object_cnt++;\
   } else {\
    if(multidraw_object_cnt == 1){\
     var xxyy = multi_snap_check(parallelogram_x[l1],parallelogram_y[l1],parallelogram_snap);\
     parallelogram_x.push(xxyy[0]);\
     parallelogram_y.push(xxyy[1]);\
     parallelogram_x.push(xy[0]);\
     parallelogram_y.push(xy[1]);\
     draw_parallelogram();\
     parallelogram_x.pop();parallelogram_y.pop();\
     parallelogram_x.pop();parallelogram_y.pop();\
    } else {\
     if(multidraw_object_cnt == 2){\
      var xxyy = multi_snap_check(parallelogram_x[l2]-parallelogram_x[l1] + parallelogram_x[l0],parallelogram_y[l2]-parallelogram_y[l1] + parallelogram_y[l0],parallelogram_snap);\
      parallelogram_x.push(xy[0]);parallelogram_y.push(xy[1]);\
      parallelogram_x.push(xxyy[0]);\
      parallelogram_y.push(xxyy[1]);\
      draw_parallelogram();\
      parallelogram_x.pop();parallelogram_y.pop();\
      parallelogram_x.pop();parallelogram_y.pop();\
     };\
    };\
   };\
   if( multidraw_object_cnt == 3 ){\
    parallelogram_x.pop();parallelogram_y.pop();\
    var xxyy = multi_snap_check(parallelogram_x[l2]-parallelogram_x[l1] + parallelogram_x[l0],parallelogram_y[l2]-parallelogram_y[l1] + parallelogram_y[l0],parallelogram_snap);\
    parallelogram_x.push(xxyy[0]);\
    parallelogram_y.push(xxyy[1]);\
    parallelogram_x.push(xy[0]);parallelogram_y.push(xy[1]);\
    parallelogram_x.pop();parallelogram_y.pop();\
    multidraw_object_cnt = 0;\
    draw_parallelogram();\
   };\
  };function draw_parallelogram(){\
   var len = parallelogram_x.length-1;\
   context_parallelogram.clearRect(0,0,xsize,ysize);\
   for(var p = 0 ; p < len ; p = p+4){\
    context_parallelogram.beginPath();\
    context_parallelogram.moveTo(parallelogram_x[p],parallelogram_y[p]);\
    for( var m = p+1 ;m < p+4 ; m++){\
     context_parallelogram.lineTo(parallelogram_x[m],parallelogram_y[m]);\
    };\
    context_parallelogram.lineTo(parallelogram_x[p],parallelogram_y[p]);\
    context_parallelogram.closePath();\
    context_parallelogram.fill();\
    context_parallelogram.stroke();\
   };\
   return;\
  };var canvas_parallelogram = create_canvas%d(10%d,xsize,ysize);var context_parallelogram = canvas_parallelogram.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2);};\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2);};\
  context_parallelogram.lineCap = \"round\";context_parallelogram.lineWidth = multilinewidth[%d];\
  if(multilinewidth[%d]%%2 == 1){ context_parallelogram.translate(0.5,0.5);};\
  context_parallelogram.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  if(multifill[%d] != 0 ){var my_fill_color=\"rgba(\"+multifillcolors[%d]+\",\"+multifillopacity[%d]+\")\";if( multifill[%d] > 1 ){context_parallelogram.fillStyle = create_Pattern(0,0,parseInt(multifill[%d]),my_fill_color);}else{context_parallelogram.fillStyle = my_fill_color;};}else{context_parallelogram.fillStyle = \"rgba( 255,255,255,0)\";};\
  if(multidash[%d] == 1 ){ if( context_parallelogram.setLineDash ){context_parallelogram.setLineDash([2,4]);}\
  else{if(context_parallelogram.mozDash){context_parallelogram.mozDash = [2,4]};};};\
  var parallelogram_x = new Array();var parallelogram_y = new Array();var parallelogram_snap = multisnaptogrid[%d];",
  canvas_root_id,u,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i,i);

      if( no_controls != 1 ){
        fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt=0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if(multiuserinput[%d] == 1 ){inner_html+=\"<td>(<input type='text' size='8' value='x1,x2...x_n' id='input_parallelogram_x'> --- <input type='text' size='8' value='y1,y2...y_n' id='input_parallelogram_y'>)</td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_parallelogram_x,input_parallelogram_y,null);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
        fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
  /*
  images : identical ! to userdraw images,bogus_color
  15/1/2021 : corrected long standing flaw regarding "centered" on external stuff
  tested: bitmap,svg,TeX [tested KaTeX]
  */
    case 20:
      fprintf(js_include_file,"var current_id = null;var external_div_cnt=0;\
  function image_adjust(image,x,y){\
   var centered = %d;\
   var w = parseInt(image.width);var h = parseInt(image.height);\
   if(w == 0 || h == 0 ){\
    w = parseInt(window.getComputedStyle(image).width);\
    h = parseInt(window.getComputedStyle(image).height);\
   };\
   switch(centered){\
    case 0: return [x,y];break;\
    case 1: return [x,parseInt(y-0.5*h)];break;\
    case 2: return [parseInt(x+0.5*h),y];break;\
    case 3: return [parseInt(x+0.5*h),parseInt(y-0.5*h)];break;\
    case 4: return [parseInt(x-0.5*w),parseInt(y-0.5*h)];break;\
    default: return [x,y];break;\
   };\
  };\
  const reset_placeable_selection = function(thing){\
    const placeables = document.querySelectorAll('.canvas_placeable');\
    placeables.forEach((placeable) => {\
      placeable.classList.remove('selected');\
    });\
    if(thing){thing.classList.add('selected');}\
  };\
  place_image_on_canvas = function(id){\
    var thing = document.getElementById(id);\
    reset_placeable_selection(thing);\
    var tag = thing.tagName;\
    if(tag == 'SVG'){draw_mathml_svg(thing,id);return;};\
    if(tag == 'DIV' || tag == 'SPAN' || tag == 'P' || tag == 'TD' || tag == 'TH'){draw_mathml_div(thing,id);return;};\
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
   if(num == 1 && current_id){\
    var xy = multi_snap_check(x,y,images_snap);\
    images_x.push(xy[0]);\
    images_y.push(xy[1]);\
    images_id.push(current_id);\
    reset_placeable_selection();\
    current_id = null;\
   };\
   draw_images();\
  };\
  function draw_images(){\
   var xy;var img;var tag;\
   for(var i=0; i<2;i++){\
    for(var p = 0 ; p < images_x.length; p++){\
     if( images_id[p] ){\
      img = document.getElementById(images_id[p]);\
      tag = img.tagName;\
      xy = image_adjust(img,images_x[p],images_y[p]);\
      if( tag != 'IMG' ){\
        xy[0] = xy[0] / xsize * 100;\
        xy[1] = xy[1] / ysize * 100;\
        img.setAttribute('style','display:block;position:absolute;top:'+xy[1]+'%%;left:'+xy[0]+'%%;');\
      } else {\
       context_images.drawImage(img,xy[0],xy[1],img.width,img.height);\
      };\
     };\
    };\
   };\
  };\
  var canvas_images = create_canvas%d(10%d,xsize,ysize);\
  var context_images = canvas_images.getContext(\"2d\");\
  context_images.font = multifont_family;\
  context_images.fillStyle = \"rgba(\"+multifont_color+\",\"+multistrokeopacity[%d]+\")\";\
  var images_snap = multisnaptogrid[%d];\
  var images_x = new Array();var images_y = new Array();\
  var images_id = new Array();",use_offset,canvas_root_id,u,i,i);
        if( no_controls != 1 ){
          fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt=0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( typeof(imagepalette) === 'object' ){\
   inner_html+=\"<td><table class='%s'><tr>\";\
   for(var im=0; im < imagepalette.length; im++){\
   if( im %% 4 == 0 ){ inner_html+=\"</tr><tr>\";};\
   inner_html+=\"<td><img onclick='javascript:place_image_on_canvas(this.id);' src='\"+imagepalette[im]+\"' id='imagepalette_\"+im+\"' alt='none'></td>\";};\
   inner_html+=\"</tr></table></td><td>&nbsp;</td></tr>\";}else{inner_html+=\"<td>&nbsp;</td><td>&nbsp;</td></tr>\";};",u,i,canvas_root_id,u,table_css);
        } else {
          fprintf(js_include_file,"inner_html+=\"</table>\";tooltip_div.innerHTML += inner_html;userdraw_primitive = %d;",u);
        }
      break;
  /* curvedarrow/curvedarrows */
    case 21 ... 22:
      fprintf(js_include_file,"function curvedarrows(x,y,event_which,num){\
   var xy = [x,y];\
   if(event_which == 0){\
     xy = multi_snap_check(x,y,curvedarrows_snap);\
     if(num == 0 && multidraw_object_cnt == 0){curvedarrows_x = [];curvedarrows_y = [];curvedarrows_x[0] = xy[0];curvedarrows_y[0] = xy[1];}\
    else{curvedarrows_x.push(xy[0]);curvedarrows_y.push(xy[1]);};multidraw_object_cnt++;\
   } else {\
    if( multidraw_object_cnt < 3 ){curvedarrows_x.push(xy[0]);curvedarrows_y.push(xy[1]);draw_curvedarrows();curvedarrows_x.pop();curvedarrows_y.pop();};\
    if( multidraw_object_cnt == 3 ){curvedarrows_x.pop();curvedarrows_y.pop();curvedarrows_x.push(xy[0]);curvedarrows_y.push(xy[1]);multidraw_object_cnt = 0;draw_curvedarrows();};\
   };\
  };function draw_curvedarrows(){\
   var len = curvedarrows_x.length;var x1,y1,x2,y2,x3,y3;\
   context_curvedarrows.clearRect(0,0,xsize,ysize);\
   for(var p = 0 ; p < len ; p = p+3){\
    x1 = curvedarrows_x[p];x2 = curvedarrows_x[p+1];x3 = curvedarrows_x[p+2];y1 = curvedarrows_y[p];y2 = curvedarrows_y[p+1];y3 = curvedarrows_y[p+2];\
    var angle1 = Math.atan2(x3 - x2,y3 - y2) + Math.PI;\
    context_curvedarrows.beginPath();\
    context_curvedarrows.moveTo(x1,y1);\
    context_curvedarrows.quadraticCurveTo(x3,y3,x2,y2);\
    context_curvedarrows.moveTo(x2 - (arrow_head * Math.sin(angle1 - Math.PI / 6)),y2 - (arrow_head * Math.cos(angle1 - Math.PI / 6)));\
    context_curvedarrows.lineTo(x2, y2);\
    context_curvedarrows.lineTo(x2 - (arrow_head * Math.sin(angle1 + Math.PI / 6)),y2 - (arrow_head * Math.cos(angle1 + Math.PI / 6)));\
    context_curvedarrows.stroke();\
    context_curvedarrows.closePath();\
   };\
   return;\
  };var canvas_curvedarrows = create_canvas%d(10%d,xsize,ysize);\
  var context_curvedarrows =  canvas_curvedarrows.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_curvedarrows.lineWidth = multilinewidth[%d];\
  if(multilinewidth[%d]%%2 == 1){ context_curvedarrows.translate(0.5,0.5);};\
  context_curvedarrows.lineCap = \"round\";\
  context_curvedarrows.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  context_curvedarrows.fillStyle = context_curvedarrows.strokeStyle;\
  if(multidash[%d] == 1 ){ if( context_curvedarrows.setLineDash ){\
  context_curvedarrows.setLineDash([2,4]);}else{if(context_curvedarrows.mozDash){\
  context_curvedarrows.mozDash = [2,4]};};};\
  var curvedarrows_x = new Array();var curvedarrows_y = new Array();\
  var curvedarrows_snap = multisnaptogrid[%d];",
  canvas_root_id,u,i,i,i,i,i,i,i,i,i,i,i,i);

      if( no_controls != 1 ){
        fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1 ){inner_html+=\"<td><b>(<input type='text' size='5' value='x1 : y1' id='input_curvedarrows_x'>) -- (<input type='text' size='5' value='x2 : y2' id='input_curvedarrows_y'>) -- (<input type='text' size='5' value='x3 : y3' id='input_curvedarrows_r'>)</b></td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_curvedarrows_x,input_curvedarrows_y,input_curvedarrows_r);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
        fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
  /* curvedarrow2/curvedarrows2 */
    case 23 ... 24:
      fprintf(js_include_file,"function curvedarrows2(x,y,event_which,num){\
   var xy=[x,y];\
   if(event_which == 0){\
     xy = multi_snap_check(x,y,curvedarrows2_snap);\
     if(num == 0 && multidraw_object_cnt == 0){curvedarrows2_x = [];curvedarrows2_y = [];curvedarrows2_x[0] = xy[0];curvedarrows2_y[0] = xy[1];}\
    else{curvedarrows2_x.push(xy[0]);curvedarrows2_y.push(xy[1]);};multidraw_object_cnt++;\
   } else {\
   if( multidraw_object_cnt < 3 ){curvedarrows2_x.push(xy[0]);curvedarrows2_y.push(xy[1]);draw_curvedarrows2();curvedarrows2_x.pop();curvedarrows2_y.pop();};\
   if( multidraw_object_cnt == 3 ){curvedarrows2_x.pop();curvedarrows2_y.pop();curvedarrows2_x.push(xy[0]);curvedarrows2_y.push(xy[1]);multidraw_object_cnt = 0;draw_curvedarrows2();};\
   };\
  };function draw_curvedarrows2(){\
   var len = curvedarrows2_x.length;var x1,y1,x2,y2,x3,y3;\
   context_curvedarrows2.clearRect(0,0,xsize,ysize);\
   for(var p = 0 ; p < len ; p = p+3){\
    x1 = curvedarrows2_x[p];x2 = curvedarrows2_x[p+1];x3 = curvedarrows2_x[p+2];y1 = curvedarrows2_y[p];y2 = curvedarrows2_y[p+1];y3 = curvedarrows2_y[p+2];\
    var angle1 = Math.atan2(x3 - x2,y3 - y2) + Math.PI;\
    var angle2 = Math.atan2(x3 - x1,y3 - y1) + Math.PI;\
    context_curvedarrows2.beginPath();\
    context_curvedarrows2.moveTo(x1,y1);\
    context_curvedarrows2.moveTo(x1 - (arrow_head * Math.sin(angle2 - Math.PI / 6)),y1 - (arrow_head * Math.cos(angle2 - Math.PI / 6)));\
    context_curvedarrows2.lineTo(x1, y1);\
    context_curvedarrows2.lineTo(x1 - (arrow_head * Math.sin(angle2 + Math.PI / 6)),y1 - (arrow_head * Math.cos(angle2 + Math.PI / 6)));\
    context_curvedarrows2.moveTo(x1,y1);\
    context_curvedarrows2.quadraticCurveTo(x3,y3,x2,y2);\
    context_curvedarrows2.moveTo(x2 - (arrow_head * Math.sin(angle1 - Math.PI / 6)),y2 - (arrow_head * Math.cos(angle1 - Math.PI / 6)));\
    context_curvedarrows2.lineTo(x2, y2);\
    context_curvedarrows2.lineTo(x2 - (arrow_head * Math.sin(angle1 + Math.PI / 6)),y2 - (arrow_head * Math.cos(angle1 + Math.PI / 6)));\
    context_curvedarrows2.stroke();\
    context_curvedarrows2.closePath();\
   };\
   return;\
  };var canvas_curvedarrows2 = create_canvas%d(10%d,xsize,ysize);\
  var context_curvedarrows2 =  canvas_curvedarrows2.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_curvedarrows2.lineWidth = multilinewidth[%d];\
  if(multilinewidth[%d]%%2 == 1){ context_curvedarrows2.translate(0.5,0.5);};\
  context_curvedarrows2.lineCap = \"round\";\
  context_curvedarrows2.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  context_curvedarrows2.fillStyle = context_curvedarrows2.strokeStyle;\
  if(multidash[%d] == 1 ){ if( context_curvedarrows2.setLineDash ){\
  context_curvedarrows2.setLineDash([2,4]);}else{if(context_curvedarrows2.mozDash){\
  context_curvedarrows2.mozDash = [2,4]};};};\
  var curvedarrows2_x = new Array();var curvedarrows2_y = new Array();\
  var curvedarrows2_snap = multisnaptogrid[%d];",canvas_root_id,u,i,i,i,i,i,i,i,i,i,i,i,i);

      if( no_controls != 1 ){
        fprintf(js_include_file,"inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1 ){inner_html+=\"<td><b>(<input type='text' size='5' value='x1 : y1' id='input_curvedarrows2_x'>) -- (<input type='text' size='5' value='x2 : y2' id='input_curvedarrows2_y'>) -- (<input type='text' size='5' value='x3 : y3' id='input_curvedarrows2_r'>)</b></td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_curvedarrows2_x,input_curvedarrows2_y,input_curvedarrows2_r);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
        fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
    case 25 ... 26:
      fprintf(js_include_file,"function crosshairs(x,y,event_which,num){\
   if(event_which == 1){ return; };\
   var xy = multi_snap_check(x,y,crosshairs_snap);\
   if( num == 0 ){\
    crosshairs_x[0] = xy[0];\
    crosshairs_y[0] = xy[1];\
   } else {\
    crosshairs_x.push(xy[0]);\
    crosshairs_y.push(xy[1]);\
   };\
   draw_crosshairs();\
  };\
  function draw_crosshairs(){\
   var crosshair_size = %d;\
   context_crosshairs.clearRect(0,0,xsize,ysize);\
   var x1,x2,y1,y2;\
   for(var p = 0 ; p < crosshairs_x.length ; p++ ){\
    x1 = crosshairs_x[p] - crosshair_size;\
    x2 = crosshairs_x[p] + crosshair_size;\
    y1 = crosshairs_y[p] - crosshair_size;\
    y2 = crosshairs_y[p] + crosshair_size;\
    context_crosshairs.beginPath();\
    context_crosshairs.moveTo(x1,y1);\
    context_crosshairs.lineTo(x2,y2);\
    context_crosshairs.closePath();\
    context_crosshairs.stroke();\
    context_crosshairs.beginPath();\
    context_crosshairs.moveTo(x2,y1);\
    context_crosshairs.lineTo(x1,y2);\
    context_crosshairs.closePath();\
    context_crosshairs.stroke();\
   };\
  };\
  var canvas_crosshairs = create_canvas%d(100%d,xsize,ysize);var context_crosshairs = canvas_crosshairs.getContext(\"2d\");\
  if( multistrokeopacity[%d] > 1 ){ multistrokeopacity[%d] = (0.0039215*multistrokeopacity[%d]).toFixed(2); };\
  if( multifillopacity[%d] > 1 ){ multifillopacity[%d] =  (0.0039215*multifillopacity[%d]).toFixed(2); };\
  context_crosshairs.strokeStyle = \"rgba(\"+multistrokecolors[%d]+\",\"+multistrokeopacity[%d]+\")\";\
  context_crosshairs.fillStyle = context_crosshairs.strokeStyle;\
  context_crosshairs.lineWidth = multilinewidth[%d];if(multilinewidth[%d] %%2 == 1){ context_crosshairs.translate(0.5,0.5);};\
  var crosshairs_x = new Array();var crosshairs_y = new Array();\
  var crosshairs_snap = multisnaptogrid[%d];",crosshair_size,canvas_root_id,i,i,i,i,i,i,i,i,i,i,i,i);
      if( no_controls != 1){
        fprintf(js_include_file,"\
   inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == 1 ){inner_html+=\"<td>(<input type='text' size='5' value='' id='input_crosshairs_x'>:<input type='text' size='5' value='' id='input_crosshairs_y'>)</td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_crosshairs_x,input_crosshairs_y,null);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);
      } else {
          fprintf(js_include_file,"userdraw_primitive = %d;",u);
      }
      break;
    case 27:break;
  /* einde switch */
      default : break;
    }
  } /* end for loop */

 /* id_z may be used for radius , text etc...e.g. no x-values,y-values, x&y-valuepairs*/
  fprintf(js_include_file,"update_draw_area%d = function(desc,id_x,id_y,id_z){\
  if( desc == 20 ){ draw_images();return;};\
  var x1,x2,x3,y1,y2,y3,z1,r,A,B,C;\
  x1 = id_x.value;y1 = id_y.value;if(id_z != null){z1 = id_z.value;};\
  if( (desc > 3 && desc < 12) || desc == 14 || desc == 15 ){A = coord_split(x1);B = coord_split(y1);if(A.length != 2 || B.length != 2){alert('coordinate mismatch');return;};x1 = x2px(safe_eval(A[0]));y1 = y2px(safe_eval(A[1]));x2 = x2px(safe_eval(B[0]));y2 = y2px(safe_eval(B[1]));if(desc == 10 || desc == 11 ){ C = coord_split(z1);x3 = x2px(safe_eval(C[0]));y3 = y2px(safe_eval(C[1]));};};\
  if( desc < 4 ){x1 = x2px(safe_eval(x1));y1 = y2px( safe_eval(y1));};\
  if( desc > 20 ){A = coord_split(x1);B = coord_split(y1);C = coord_split(z1);\
  if(A.length != 2 || B.length != 2 || C.length != 2){ alert('coordinate mismatch');return;};\
  x1 = x2px(safe_eval(A[0]));y1 = y2px(safe_eval(A[1]));\
  x2 = x2px(safe_eval(B[0]));y2 = y2px(safe_eval(B[1]));\
  x3 = x2px(safe_eval(C[0]));y3 = y2px(safe_eval(C[1]));};\
  switch(desc){",canvas_root_id);
  for(i=0 ; i < MAX_MULTI_PRIMITIVES ; i++ ){
    switch(draw_nums[i]){
      case -1: break;
      case 0: fprintf(js_include_file,"case 0:points(x1,y1,0,0);break;"); break;
      case 1: fprintf(js_include_file,"case 1:points(x1,y1,0,1);break;"); break;
      case 2: fprintf(js_include_file,"case 2:r = scale_x_radius(safe_eval(document.getElementById(id_z.id).value));multi_radius[0] = r;circles_x[0] = x1;circles_y[0] = y1;draw_circles();break;"); break;
      case 3: fprintf(js_include_file,"case 3:r = scale_x_radius(safe_eval(document.getElementById(id_z.id).value));multi_radius.push(r);circles_x.push(x1);circles_y.push(y1);draw_circles();break;"); break;
      case 4: fprintf(js_include_file,"case 4:lines_x[0] = x1;lines_x[1] = x2;lines_y[0] = y1;lines_y[1] = y2;calc_lines();draw_lines();break;"); break;
      case 5: fprintf(js_include_file,"case 5:lines_x.push(x1);lines_x.push(x2);lines_y.push(y1);lines_y.push(y2);calc_lines();draw_lines();break;"); break;
      case 6: fprintf(js_include_file,"case 6:segments_x[0] = x1;segments_x[1] = x2;segments_y[0] = y1;segments_y[1] = y2;draw_segments();break;"); break;
      case 7: fprintf(js_include_file,"case 7:segments_x.push(x1);segments_x.push(x2);segments_y.push(y1);segments_y.push(y2);draw_segments();break;"); break;
      case 8: fprintf(js_include_file,"case 8:arrows_x[0] = x1;arrows_x[1] = x2;arrows_y[0] = y1;arrows_y[1] = y2;draw_arrows();break;"); break;
      case 9: fprintf(js_include_file,"case 9:arrows_x.push(x1);arrows_x.push(x2);arrows_y.push(y1);arrows_y.push(y2);draw_arrows();break;"); break;
      case 10: fprintf(js_include_file,"case 10:triangles_x[0] = x1;triangles_x[1] = x2;triangles_x[2] = x3;triangles_y[0] = y1;triangles_y[1] = y2;triangles_y[2] = y3;draw_triangles();break;"); break;
      case 11: fprintf(js_include_file,"case 11:triangles_x.push(x1);triangles_x.push(x2);triangles_x.push(x3);triangles_y.push(y1);triangles_y.push(y2);triangles_y.push(y3);draw_triangles();break;"); break;
      case 12: fprintf(js_include_file,"case 12:A = coord_split(x1);B = coord_split(y1);var plus_len = A.length;if( plus_len != B.length){alert('mismatch between the number of x-values and  y-values');return;};for(var p = 0 ; p < plus_len ; p++){x1 = x2px(safe_eval(A[p]));y1 = y2px(safe_eval(B[p]));closedpoly_x.push(x1);closedpoly_y.push(y1);};x1 = x2px(safe_eval(A[0]));y1 = y2px(safe_eval(B[0]));closedpoly_x.push(x1);closedpoly_y.push(y1);draw_closedpoly();break;"); break;
      case 13: fprintf(js_include_file,"case 13:text_abc.push( document.getElementById(id_z.id).value);text(x2px(safe_eval(x1)),y2px(safe_eval(y1)),0,1);draw_text();break;"); break;
      case 14: fprintf(js_include_file,"case 14:rects_x[0] = x1;rects_x[1] = x2;rects_y[0] = y1;rects_y[1] = y2;draw_rects();break;"); break;
      case 15: fprintf(js_include_file,"case 15:rects_x.push(x1);rects_x.push(x2);rects_y.push(y1);rects_y.push(y2);draw_rects();break;"); break;
      case 16: fprintf(js_include_file,"case 16:polys_x[0] = x1;polys_x[1] = x2;polys_x[2] = x3;polys_y[0] = y1;polys_y[1] = y2;polys_y[2] = y3;draw_polys();break;"); break;
      case 17: fprintf(js_include_file,"case 17:polys_x.push(x1);polys_x.push(x2);polys_x.push(x3);polys_y.push(y1);polys_y.push(y2);polys_y.push(y3);draw_polys();break;"); break;
      case 18: fprintf(js_include_file,"case 18:parallelogram_x[0] = x1;parallelogram_x[1] = x2;parallelogram_x[2] = x3;parallelogram_y[0] = y1;parallelogram_y[1] = y2;parallelogram_y[2] = y3;draw_parallelogram();break;"); break;
   case 19: fprintf(js_include_file,"case 19:parallelogram_x.push(x1);parallelogram_x.push(x2);parallelogram_x.push(x3);parallelogram_y.push(y1);parallelogram_y.push(y2);parallelogram_y.push(y3);draw_parallelogram();break;"); break;
   case 20: fprintf(js_include_file,"case 20:draw_images();break;"); break;
   case 21: fprintf(js_include_file,"case 21:curvedarrows_x[0] = x1;curvedarrows_x[1] = x2;curvedarrows_x[2] = x3;curvedarrows_y[0] = y1;curvedarrows_y[1] = y2;curvedarrows_y[2] = y3;draw_curvedarrows();break;"); break;
   case 22: fprintf(js_include_file,"case 22:curvedarrows_x.push(x1);curvedarrows_x.push(x2);curvedarrows_x.push(x3);curvedarrows_y.push(y1);curvedarrows_y.push(y2);curvedarrows_y.push(y3);draw_curvedarrows();break;"); break;
   case 23: fprintf(js_include_file,"case 23:curvedarrows2_x[0] = x1;curvedarrows2_x[1] = x2;curvedarrows2_x[2] = x3;curvedarrows2_y[0] = y1;curvedarrows2_y[1] = y2;curvedarrows2_y[2] = y3;draw_curvedarrows2();break;"); break;
   case 24: fprintf(js_include_file,"case 24:curvedarrows2_x.push(x1);curvedarrows2_x.push(x2);curvedarrows2_x.push(x3);curvedarrows2_y.push(y1);curvedarrows2_y.push(y2);curvedarrows2_y.push(y3);draw_curvedarrows2();break;"); break;
   case 25: fprintf(js_include_file,"case 25:crosshairs(x1,y1,0,0);break;"); break;
   case 26: fprintf(js_include_file,"case 26:crosshairs(x1,y1,0,1);break;"); break;
   default:break;
  }
 }
  fprintf(js_include_file,"default: break;};return;};");

 /* begin clear_draw_area()*/
  fprintf(js_include_file,"clear_draw_area%d = function(type){switch(type){",canvas_root_id);
  for(i=0; i < MAX_MULTI_PRIMITIVES ; i++ ){
    switch( draw_nums[i] ){
      case -1: break;
      case 0: fprintf(js_include_file,"case 0:context_points.clearRect(0,0,xsize,ysize);points_x = [];points_y = [];break;");break;
      case 1: fprintf(js_include_file,"case 1:points_x.pop();points_y.pop();draw_points();break;");break;
      case 2: fprintf(js_include_file,"case 2:context_circles.clearRect(0,0,xsize,ysize);circles_x = [];circles_y = []; multi_radius = [];break;");break;
      case 3: fprintf(js_include_file,"case 3:circles_x.pop();circles_y.pop(); multi_radius.pop();draw_circles();break;");break;
      case 4: fprintf(js_include_file,"case 4:context_lines.clearRect(0,0,xsize,ysize);lines_x = [];lines_y = [];break;");break;
      case 5: fprintf(js_include_file,"case 5:lines_x.pop();lines_y.pop();lines_x.pop();lines_y.pop();draw_lines();break;");break;
      case 6: fprintf(js_include_file,"case 6:context_segments.clearRect(0,0,xsize,ysize);segments_x = [];segments_y = [];break;");break;
      case 7: fprintf(js_include_file,"case 7:segments_x.pop();segments_y.pop();segments_x.pop();segments_y.pop();draw_segments();break;");break;
      case 8: fprintf(js_include_file,"case 8:context_arrows.clearRect(0,0,xsize,ysize);arrows_x = [];arrows_y = [];break;");break;
      case 9: fprintf(js_include_file,"case 9:arrows_x.pop();arrows_y.pop();arrows_x.pop();arrows_y.pop();draw_arrows();break;");break;
      case 10:fprintf(js_include_file,"case 10:context_triangles.clearRect(0,0,xsize,ysize); triangles_x = [];triangles_y = [];break;");break;
      case 11:fprintf(js_include_file,"case 11:for(var p=0;p<3;p++){triangles_x.pop();triangles_y.pop();};draw_triangles();break;");break;
      case 12:fprintf(js_include_file,"case 12:context_closedpoly.clearRect(0,0,xsize,ysize);closedpoly_x = [];closedpoly_y = [];break;");break;
      case 13:fprintf(js_include_file,"case 13:context_text.clearRect(0,0,xsize,ysize);text_x.pop();text_y.pop();text_abc.pop();draw_text();break;");break;
      case 14:fprintf(js_include_file,"case 14:context_rects.clearRect(0,0,xsize,ysize);rects_x = [];rects_y = [];break;");break;
      case 15:fprintf(js_include_file,"case 15:rects_x.pop();rects_y.pop();rects_x.pop();rects_y.pop();draw_rects();break;");break;
      case 16:fprintf(js_include_file,"case 16:context_polys.clearRect(0,0,xsize,ysize); polys_x = [];polys_y = [];break;");break;
      case 17:fprintf(js_include_file,"case 17:for(var p=0;p<polynum;p++){polys_x.pop();polys_y.pop();};draw_polys();break;");break;
      case 18:fprintf(js_include_file,"case 18:context_parallelogram.clearRect(0,0,xsize,ysize); parallelogram_x = [];parallelogram_y = [];break;");break;
      case 19:fprintf(js_include_file,"case 19:for(var p = 0; p < 4;p++){ parallelogram_x.pop();parallelogram_y.pop();};draw_parallelogram();break;");break;
      case 20:fprintf(js_include_file,"case 20:context_images.clearRect(0,0,xsize,ysize);var len = images_id.length;if( len == 0 ){return;};var img = document.getElementById(images_id[len-1]);if( img.tagName == 'DIV' ){ img.innerHTML=null;};images_x.pop();images_y.pop();images_id.pop();draw_images();break;");break;
      case 21:fprintf(js_include_file,"case 21:context_curvedarrows.clearRect(0,0,xsize,ysize);curvedarrows_x = [];curvedarrows_y = [];break;");break;
      case 22:fprintf(js_include_file,"case 22:curvedarrows_x.pop();curvedarrows_y.pop();curvedarrows_x.pop();curvedarrows_y.pop();curvedarrows_x.pop();curvedarrows_y.pop();draw_curvedarrows();break;");break;
      case 23:fprintf(js_include_file,"case 23:context_curvedarrows2.clearRect(0,0,xsize,ysize);curvedarrows2_x = [];curvedarrows2_y = [];break;");break;
      case 24:fprintf(js_include_file,"case 24:curvedarrows2_x.pop();curvedarrows2_y.pop();curvedarrows2_x.pop();curvedarrows2_y.pop();curvedarrows2_x.pop();curvedarrows2_y.pop();draw_curvedarrows2();break;");break;
      case 25:fprintf(js_include_file,"case 25:context_crosshairs.clearRect(0,0,xsize,ysize);crosshairs_x = [];crosshairs_y = [];break;");break;
      case 26:fprintf(js_include_file,"case 26:crosshairs_x.pop();crosshairs_y.pop();draw_crosshairs();break;");break;
      default:break;
    }
  }
  fprintf(js_include_file,"};return;};");
 /* end clear_draw_area();*/

 /* add </table> is button controls are needed */
  if( no_controls != 1 ){fprintf(js_include_file,"inner_html+=\"</table>\";tooltip_div.innerHTML = inner_html;"); }
  free(str);
}
/* end 'void add_js_multidraw()' */

/*  if( desc >20 ){A = coord_split(x1);B = coord_split(y1);C = coord_split(z1);if(A.length != 2 || B.length != 2 || C.length != 2 ){alert('coordinate mismatch');return;};x1 = x2px(safe_eval(A[0]));y1 = y2px(safe_eval(A[1]));x2 = x2px(safe_eval(B[0]));y2 = y2px(safe_eval(B[1]));x3 = x2px(safe_eval(C[0]));y3 = y2px(safe_eval(C[1]));};};};\

   inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == '1' ){inner_html+=\"<td><b>(<input type='text' size='5' value='x1 : y1' id='input_curvedarrows2_x'>) -- (<input type='text' size='5' value='x2 : y2' id='input_curvedarrows2_y'>) -- (<input type='text' size='5' value='x3 : y3' id='input_curvedarrows2_r'>)</b></td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_curvedarrows2_x,input_curvedarrows2_y,input_curvedarrows2_r);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);

   inner_html+=\"<tr><td><input type='button' onclick='javascript:userdraw_primitive=%d;multidraw_object_cnt = 0;' value='\"+multilabel[%d]+\"'></td><td><input type='button' onclick='javascript:clear_draw_area%d(%d);' value='delete'></td>\";\
   if( multiuserinput[%d] == '1' ){inner_html+=\"<td><b>(<input type='text' size='5' value='x1 : y1' id='input_curvedarrows_x'>) -- (<input type='text' size='5' value='x2 : y2' id='input_curvedarrows_y'>) -- (<input type='text' size='5' value='x3 : y3' id='input_curvedarrows_r'>)</b></td><td><input type='button' id='canvasdraw_ok_button' onclick='javascript:update_draw_area%d(%d,input_curvedarrows_x,input_curvedarrows_y,input_curvedarrows_r);' value='OK'></td></tr>\";}else{inner_html+=\"</tr>\";};",
   u,i,canvas_root_id,u,i,canvas_root_id,u);

*/
