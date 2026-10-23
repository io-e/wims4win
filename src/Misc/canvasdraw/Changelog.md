# Canvasdraw changes log
## TODO :
- sort out the global/local variable mess...and reorganize headerfile
	 : zooming and grid: scaling needs to be more adaptive! (use constant amount of grid lines)

  2025/10/22: added dashing to cmd parallel (forgotten...)
## 2025 (bpr-bernardi)
  2025/06/28: corrected problems with jsplot and opacity & multistrokeopacity
  2025/06/27: corrected (fill)opacity to colorpalette & userdraw clickfill,color 
  2025/06/21: do vlines as vline, hlines as hline to avoid leaks
  change parseFloat in Math.trunc and ajust x2px y2px
  2025/06/16: modifications (no change, just way to go to CASE)
## 2024
* 23/5/2024: add 'exp' to "to_js_math()"
* 1/5/2024:  command "dashtype" was gone fishing...there must be a much better way of dealing with prefix "dash"...
* 31/4/2024: work in progress: rewrite reset,noreset !
* 28/4/2024: prefix "dash" may be used by all 'dashable' objects
	     which is an overload on 'dashable' syntax !!
	     use commands "noreset" and "dashed" to 'dash' all following objects...until command 'reset'
	     size 200,200
	     xrange -10,10
	     yrange -10,10
	     noreset
	     dashed
	     hline ...
	     vline ...
	     polyline ...
	     rect ...
	     circle ...
	     reset
	     etc etc
* 1/3/2024: quick fix for halfline
## 2023
* 23/2/2023: minor improvement for sub/sup routine for command family strings (use of space is optional)
* 22/2/2023: snaptopoints may be called multiple times (no limit) And restored MAX_INT to 128 (but it's open for change, anyway)
* 22/2/2023: increase the limit MAX_INT to 256
* 21/2/2023: canvasmultidraw now place object with relative coords (%) instead of absolute (px)
* 13/1/2023: corrected flaw in external images 'copy & drag/drop'
## 2022
* 28/8/2022: added command "newrange xmin,xmax,ymin,ymax" see https://wimsedu.info/?topic=dessiner-des-portions-de-fonctions-sur-un-meme-graphe
* 01/8/2022: getMouse(),latest improvements related to 'reponsiveness' will fatally lockup browsers in the pixel-by-pixel filling...at least in userdraw clickfill
* 19/7/2022: checked and corrected 'dashed' objects like arc,arrowarc,ellipse...
* 16/7/2022: solved long standing issue of dashed arrow(s) and dashed arrow(s)2
* 06/7/2022: clearbutton preserves div's with '<' && '/>' and objects placed with `placed_image_on_canvas(id)` e.g. with `id = placed*`
* 05/7/2022: corrected flaw in 'clearbutton' https://wimsedu.info/?topic=geometrie-interactive-avec-canvasdraw
* 05/7/2022: command 'group' :
	- still some issues with direct clicks on group members (NaN due to competing mouselisteners)
	- removed 'debug newlines'
	- external images -via command 'copy' NOT supported (too much trouble)
	- need to add 'copy' to dragstuff library or introduce another command...
* 26/6/2022: added keyword 'group' : provides mouse-driven movements of all objects after keyword 'group' until keyword 'kill group;
	- may be combined with 'slider' to move or rotate single objects or grouped objects
	- may not be combined with command 'drag xy' (mouse listeners are already occupied...)
	- may be combined with command 'onclick'
	- added objects created by command 'latex' to grouping (todo : external images etc)

* 23/6/2022: corrected drag&drop reply of external images (via command 'copy') when mixed with other external/internal objects...

	 	 #
	 	 size 400,400
	 	 xrange -10,10
	 	 yrange -10,10
	 	 grid 2,2,grey
	 	 linewidth 2
	 	 onclick
	 	 fcircle -6,6,50,red
	 	 onclick
	 	 fcircle -6,-6,50,green
	 	 noreset
	 	 snaptogrid
	 	 drag xy
	 	 centered
	 	 latex 0,0,\frac{12345}{23456} \times \frac{12345}{23456}
	 	 translate 4,4
	 	 html 0,0,<table style='border:solid red 2px'><tr><th>HTML TABLE</th></tr><tr><td><img src="gifs/en.gif" /></tr><tr><td><img src="gifs/nl.gif" /></tr><tr><td><img src="gifs/fr.gif" /></tr><tr><td><img src="gifs/cn.gif" /></tr><tr><td><img src="gifs/it.gif" /></tr></table>
	 	 userdraw arrow2,red
	 	 translate 8,8
	 	 copy 0,0,-1,-1,-1,-1,gifs/nl.gif

* 20/6/2022: Start to make canvasdraw responsive [OB] :
	- reduce size on small screens,
	- use percentage instead of absolute positioning for xml/html/tex
	- convert mouse position coords on shrinked canvas
	- correct mouse coordinates when inside a scrolled wims_scrollable div
	- Replace "Ariel" font references by "Arial"
* 19/6/2022: added function plot to multidraw (https://wimsedu.info/?topic=userinput-function)
	command 'multidraw functions' will -for now- give just 2 inputfields... *TODO*: make configurable, so implement commands like 'multidraw functions5')
	for multiple function input fields, use multiple keywords 'function', e.g. something like:
	multidraw text,line,function,function,function,points
* 27/5/2022: added subsup to userdraw and multidraw 'text' (which is always 'centered' e.g use_offset=4)
* 26/5/2022: corrected replyformat 100
* 23/5/2022: overriding fontfamily and use Helvetica as font for _sub ^sup  strings: all inline numbers are in script-size (80% eg the same as strings+numbers for sub/sup)
* 21/5/2022: using 'scriptsize' imitation on sub / sup
* 20/5/2022: added a sub / sup imitation to the canvas 'string' command family

		size 400,400
		xrange -6,6
		yrange -6,6
		fontfamily 22px Arial

		# use single space for separation in formula...
		# use double space to create new word
		string red,-5,0,H_3 O^+ + OH^\u2212  \u2192 2H_2 O

* 2/4/2022: added timeout fix for centering issue in case of MathJaX (draw_xml())

## 2021
* 14/4/2021: added flydraw command 'range xmin,xmax,ymin,ymax'
* 6/4/2021: corrected 'text objects' (from cmd's text/string) were not added to 'noreset', e.g. keyword 'noreset' was ignored...
added a live example from E3/number/oefgraduation.fr
* 4/4/2021: corrected use of precompiler directive "KATEX_INSTALLED" (was not working...)
* 12/3/2021: add "version 0.5" to canvasdraw include file
* 23/2/2021:
added generic command 'kill argument'
work in progress
corrected wrong alternative command for jscurve
* 21/2/2021:
alternative commands now 'seachable' using js-function look()
* 19/2/2021:
slightly improved dashing of arrows (in dragstuff,userdraw & multidraw)
to test : other objects+dashing !
resetting dashing for the arrow head (head was also dashed...)
see https://wimsedu.info/?topic=dashed-arrows-not-dashed-in-canvasdraw
* 2/2/2021:
added command 'arcarrow|arrowarc', left,right,left/right
added commands 'arc','angle','arcarrow' to onclick and drag&drop
* 17/1/2021:
corrected syntax issue in canvasmultidraw.c signalled by Opera Presto : 'forbidden function user_drawstop(evt) declaration in statement'*/

can not correct centering in case of MathJaX, in commands like

```
####
centered
math 0,0,\frac{1}{222}
####
```

Things get even worse when activating in exec.c the "Safari/MathJax bugfix : refresh display when document ready."
(besides the ridiculous delay !!)
You should really try KaTeX on these systems/browsers !!!

OEF example of the problem:

```
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
\text{m1=wims(mathmlmath \frac{12344}{\pi\pi\pi\pi^{\pi}})}
\text{m2=wims(mathmlmath \frac{\pi\pi\pi\pi^{\pi}}{\alpha})}
\text{m3=wims(mathmlmath \sqrt{\pi\pi\pi\pi^{\pi}})}
\text{m4=wims(mathmlmath \sqrt{\frac{1223}{3344}})}

\statement{
<script type="text/javascript">
 function read_canvas(){
   var fun = eval("read_canvas"+canvas_scripts[0]);
   if( typeof fun === 'function'){ return fun();};
 };
 function read_dragdrop(){
   var fun = eval("read_dragdrop"+canvas_scripts[0]);
   if(typeof fun === 'function'){return fun();};
 };

</script>
<br/>
<span id="m1" onclick="javascript:place_image_on_canvas(this.id)" >\m1</span>
<span id="m2" onclick="javascript:place_image_on_canvas(this.id)" >\m2</span>
<span id="m3" onclick="javascript:place_image_on_canvas(this.id)" >\m3</span>
<span id="m4" onclick="javascript:place_image_on_canvas(this.id)" >\m4</span>

\canvasdraw{400,400}{
xrange -10,10
yrange -10,10
grid 1,1,grey
noreset
# use keyword 'noreset' to avoid using keyword 'centered' before every addition of objects !
centered
fontfamily Bold Italic 42pt Courier
# these images are internal the reply prefix is imagepalette_0, imagepalette_1... !!
imagepalette gifs/ca.gif,gifs/en.gif,gifs/nl.gif,gifs/fr.gif,gifs/cn.gif,gifs/de.gif,gifs/kh.gif,gifs/it.gif
multicolors red,green,blue,lightblue,cyan
multisnaptogrid 1,1,1,1,0
multilinewidth 0,4,0,3,1
multidraw text,arrow,images,points,lines
math -6,6,\frac{1}{\pi}
math -2,2,\frac{1}{\pi}
math 2,-2,\frac{1}{\pi}
math 6,-6,\frac{1}{\pi}
linewidth 3
points red,-6,6,-2,2,2,-2,6,-6
mouse red,22
}


These images are external...The reply id-prefix is "placed_0_'this.id'"
<table><tr>
<td><img src="gifs/ca.gif" onclick='javascript:place_image_on_canvas(this.id);' id="ca" /></td>
<td><img src="gifs/nl.gif" onclick='javascript:place_image_on_canvas(this.id);' id="nl" /></td>
<td><img src="gifs/en.gif" onclick='javascript:place_image_on_canvas(this.id);' id="en" /></td>
<td><img src="gifs/fr.gif" onclick='javascript:place_image_on_canvas(this.id);' id="fr" /></td>
<td><img src="gifs/cn.gif" onclick='javascript:place_image_on_canvas(this.id);' id="cn" /></td>
<td><img src="gifs/it.gif" onclick='javascript:place_image_on_canvas(this.id);' id="it" /></td>
<td><img src="gifs/kh.gif" onclick='javascript:place_image_on_canvas(this.id);' id="kh" /></td>
<td><img src="gifs/de.gif" onclick='javascript:place_image_on_canvas(this.id);' id="de" /></td>
</tr>
</table>
   <input type="button" onclick="alert(read_canvas());" value="read_canvas()" />
   <input type="button" onclick="alert(read_dragdrop());" value="read_dragdrop()" />
}
```

* 15/1/2021:
corrected long standing flaw regarding "centered" on external stuff...tested: bitmap,svg,TeX [on KaTeX]
corrected 'onclick' for 'draw_xml()'

* 14/1/2021:
only on KaTeX enabled wims, a slider title may use TeX produced by command "mathmlmath"

* 13/1/2021:
slider title may use TeX produced by command "mathmlmath"

=> oef example...

```
\title{slider + tex title}
\text{A = \frac{1}{2}}
\text{B = \frac{1}{3}}
\text{C = \frac{1}{4}}
\text{a = \(\frac{1}{2}\)}
\text{b = wims(mathmlmath \\Huge \\frac{1}{3} )}
\text{c = wims(mathmlmath \\Huge \\frac{1}{4} )}
\text{canvas=wims(exec canvasdraw
size 400,400
xrange -10,10
yrange -10,10
fontfamily 38px Arial
opacity 255,40
grid 1,1,grey
strokecolor red
slider -10,10,400,50,x active,\( \frac{1}{2} \)
frect -6,5,-4,-5,red
centered
latex -5,0,\A
killslider
strokecolor blue
slider -10,10,400,50,x active,\b
frect -6,5,-4,-5,blue
centered
latex -5,0,\B
killslider
strokecolor green
slider -10,10,400,50,x active,\c
frect -6,5,-4,-5,green
centered
latex -5,0,\C
killslider)
}
\statement{
canvas = \canvas
}
```

* 8/1/2021:
added affine transformation to Fly text commmand analogue

* 3/1/2021:
added check on 'nan' in case of parametric curve
		size 260,220
		xrange 0,13
		yrange 0,11
		trange 0,2
		curve black,1/t,4*sin(t)


## 2020
* 29/12/2020:
disabled limitation of adding "curve points outside the visual ymin/ymax range" (parametric curves)

* 27/12/2020:
corrected command "parallel" , to be more compatible with flydraw & affine transformations
when a transformation is set  before command parallel...the lines may be set onclick or drag xy!

* 24/12/2020:
solved some issues with curve & affine/rotate...
see [https://wimstest1.di.u-psud.fr/wims/wims.cgi?lang=en&module=adm/doc&cmd=new&job=read&doc=1028&block=transform]()

```
size 200,200
xrange -5,5
yrange -5,5
linewidth 2
trange 0,1
curve blue,2*t,sin(2*pi*t)
affine 0,1,1,2,-1,1
trange 0,1
curve green,2*t,sin(2*pi*t)
```

* 22/12/2020:
command "fillpattern image_url" was gone fishing! `get_image_from_url()` needs to be called after function definition..
* 16/11/2020:
	- added 'affine' to curve evaluation

	```
	size 220,220
	xrange -5,5
	yrange -5,5
	hline 0,0,black
	vline 0,0,black
	rotationcenter 0,0
	slider -2*pi,2*pi,200,28,angle degree active,Rotate
	linewidth 4
	curve blue,sin(x)
	affine 1,-0.2,-0.6,1,0,2
	curve red,sin(x)
	```

* 13/11/2020:
	- added `safe_eval()` to userinput (using inputfields)

* 8/7/2020:
	- command `input x,y,size,readonly,value` gave canvaserror when 'value' was ommitted or a 'space'
	- corrected bug [https://wimsedu.info/?topic=bug-canvasdraw-et-ou-drawinput]()
		```
		case 4: temp = get_string(infile,1); --->  case 4: temp = get_string(infile,3);
		```
* 14/6/2020:
	- corrected bug in multidraw snaptopoints+curvedarrow
* 11/6/2020:
	- added 'touchend' to improve behaviour of touch devices in case 'userdraw' and 'multidraw' [TO TEST]
* 9/6/2020
	- touchend  not supported (eg lifting finger+tapping after dragging to signal some 'corner point')
	- in case of touch devices, use commands 'userinput' to enforce mathematical precise drawing and not finger painting...
	- function calc_lines(): 'multidraw lines' & 'userdraw line,color' returned (xmin:y1)--(xmax:y2)
	    that should have been (x1:y1)---(x2:y2) ...otherwise snaptogrid gives float values !
* 8/6/2020:
	- added command 'noreset' or 'killreset'
* 5/6/2020:
	- solved issue of sync loading of images (`command:html 0,0,<img src="modules/U2/geometry/oefrosoni.it/images/273x273/fig4.png"  />`)

	```
		size 400,400
		xrange -6,6
		yrange -6,6
		fontsize 40
		linewidth 1
		slider -10,10,300,28,angle active degree,
		centered
		html 0,0,<img src="modules/U2/geometry/oefrosoni.it/images/273x273/fig4.png"  />
		killslider
		centered
		html 0,0,<img src="modules/U2/geometry/oefrosoni.it/images/273x273/fig4_ovr.png"  />
	```

	- commands math, katex, latex are identical
	- added compile time checking of 'katex.c' presence:
if present: use KaTeX for all browsers
if not present: use MathJaX (chrome) or native MathML (firefox)
	- TO DO: check if this actually works on non-KaTeX...

* 25/5/2020:
	- removed js-code for rotating (and transforming mouse-coordinates...) Now coordinates are transformed in C
	- slider, solved several issues (removed old slider stuff)
	- animated slider may be combined with manual/mouse slider
	- slider corrected for touch device
* 24/5/2020:
	- add keyword 'animate' to slider

	```
		size 300,300
		xrange -6,6
		yrange -6,6
		grid 1,1,grey
		linewidth 1
		slider 0,2*pi,300,28,angle animate active, no
		opacity 255,30
		ftriangle 2,2,-2,2,0,0,red
		ftriangle -2,2,-2,-2,0,0,blue
		ftriangle -2,-2,2,-2,0,0,green
		ftriangle 2,-2,2,2,0,0,orange
		rotationcenter 0,0
		frect -2,2,2,-2,black
	```


* 22/5/2020:
	- slider things...
* 21/5/2020:
	- minor changes: not yet really satisfied with combinations of xml+dragdrop+slider
	- some doc examples
* 20/5/2020:
	- slider reorganized:
	- restored all listeners on slider.(mousedown ,mousemove and mouseup)
	- update example 'angle': angle and arc are exceptions in case of sliders...they are always active (e.g. not click-activated)
	- added 'onclick=4' : in case of sliders ,no need for click-on-object to active the object--slider (important for objects covering each other)

* 18/5/2020:
	- restored interaction of commands 'arc' and 'angle' with sliders (specially Rotate)
	- drawxml() objects like katex,mathml,latex,html,obabel now interact identical as
    dragstuff library objects with slider...eg. 'first click'=select for sliders, 'second click' unselect sliders
    e.g.: `slidergroup[object_cnt] = obj or slidergroup[object_cnt] = null`
    selected sliders until 'killslider'

    ```
	######################################################
	size 500,500
	xrange -6,6
	yrange -6,6
	grid 1,1,grey
	fontfamily 42px Arial
	opacity 255,60
	centered
	#drag xy
	snaptogrid
	slider -10,10,500,50,angle,R
	slider -10,10,500,50,x,X
	slider -10,10,500,50,y,Y
	# these objects act -if selected- on the above sliders
	katex 3,3,\frac{123}{123^{\pi}}
	centered
	obabel 0,0,smi,-:c1cccc1cc,-xb none -xP100
	filled
	fillcolor blue
	angle 0,0,4,0,0,red
	killslider
	linewidth 4
	slider -10,10,500,50,angle,Rotate rect
	# if selected this object will use only the above slider
	frect -5,5,-4,4,red
	##########################################################
	```

* 15/5/2020:
	- further reduced size of 'plain' grid (like 'grid 1,1,grey')
	- using uniform "slidergroup[object_cnt] = object" to avoid difficulies with reply's of dragstuff/xml [IN PROGRESS]				         __
	- slider appearance changed to :  _____\/______________

	- xml-objects (katex/latex/mathml/html) and slider: onclick will activate slider for object and deactivate after second click

	```
	size 500,500
	xrange -6,6
	yrange -6,6
	grid 1,1,grey
	fontfamily 42px Arial
	strokecolor red
	fillcolor orange
	#rotationcenter 0,0
	slider -12,12,500,50,angle,rotate
	katex 0,0,\frac{1}{2}
	katex 3,3,\frac{1}{3}
	katex -3,3,\frac{9}{3}
	#latex 0,0,\frac{1}{2}
	#latex 3,3,\frac{1}{3}
	#latex -3,3,\frac{9}{3}
	obabel 0,5,smi,-:c1cFccc1cc,-xb none,-xB blue,-xi,-xt,-xa,-xX,-xP100,-h
	html 3,-3,<img src='gifs/en.gif' />
	html 4,-2,<H2>OK?</H2>
	html -4,-2,<input size='4' value='?" />
	```

	- added import of 'TD' and 'TH' elements with an unique 'id' and onclick='javascript:place_image_on_canvas(this.id)' to the canvas

* 13/5/2020:
	- fixed several incompatibilities
	- moved legend_cnt,linegraph_cnt barchar_cnt variables (used by 'grid') to global. reducing the size of default grid

* 12/5/2020:
	- reorganized command copy/copyresized: reply is now js-object, identical to dragstuff library
	- onclick/drag of 'copy images' transparent integration with dragstuff objects onclick/drag and external XML material
	- slider is not supported for flydraw analogues copy/copyresized : use command `html x,y,<img src='gifs/en.gif' />`
	- IOS fixed (thx Olivier Obado)
	- renamed variable 'click_cnt' into 'object_cnt' : it counts all interactive (onclick,dragdrop,slider) objects and results
    in a uniform 'reply' object{}

* 8/5/2020:
	- finished commands 'html','katex','latex','mathml' using draw_xml()
    with 'html' virtually all material can be imported
    affine operations and sliders are supported
	- corrected reply & slider
* 6/5/2020:
	- added imported object to sliders [work in progress]
	- added rotation center to draw_xml(obj); [only for command katex]
* 5/5/2020:
	- changed draw_xml() to object, for importing svg,img,div,p,span,mathml,katex in canvasdraw
	- added transformation matrix to draw_xml(). Preparing for slider manipulation of imported objects
* 3/5/2020: corrected error avoiding multiple calls to 'obabel'
* 1/5/2020: killed 'stderr' default message from 'obabel' : "converted 1 molecule"
* 30/4/2020:
	- added command 'katex x,y,tex-string'
	- thus bypassing cludge between firefox/mathml/mathjax/katex
	- for traditional wims (using MathML) use command 'latex x,y,tex-string' or 'mathml x,y,xml-code'
	- for katex-only wims, use only 'katex'
	- solved 'centered' and 'snaptogrid' issues dealing with drag&drop of generated 'svg/mathml/xml/html/js-code'
	- added link to 'howto katex and wims'

* 21/4/2020:
	- using execvp() to call "obabel" , using $PATH

* 20/4/2020:
	- changed command 'smiles' into 'obabel'
	- now all formats may be used as input
	- obabel 5,5,mol,$module_dir/mymolfile.mol,-xb none,-xB blue,-xi,-xt,-xa,-xX,-xP180,-h
	- obabel -5,-5,smi,-:c1ccFccc1,-xb none,-xB blue,-xi,-xt,-xa,-xX,-xP180,-h
	- added check on 'forbidden argument' to call obabel

* 18/4/2020:
	- added command 'smiles args' , calling -if installed- 'obabel' to produce an SVG image
	- added command 'chemtex' , adding "mhchem.js" for typesetting (needs to come first!)
	- chemtex: added bogus code for mathjax. THIS WILL ONLY WORK ON KATEX ENABLED WIMS !

```
chemtex
size 400,400
xrange -10,10
yrange -10,10
fillcolor green
grid 1,1,grey
drag xy
centered
smiles 5,5,-:c1cFccc1cc,-xb none,-xB blue,-xi,-xt,-xa,-xX,-xP180,-h
rotate 180
drag xy
centered
smiles -5,-5,-:c1cFccc1cc,-xb none,-xB blue,-xi,-xt,-xa,-xX,-xP180,-h
drag xy
rotate -90
centered
smiles -5,5,-:c1cFccc1cc,-xb none,-xB blue,-xi,-xt,-xa,-xX,-xP180,-h
zoom red
drag xy
killrotate
fontfamily 32px Arial
latex 0,0,\frac{1}{2} \ce{H3O+}
```

* 15/3/2020:
	- command latex ; improve code...minor cosmetic things (need to test on android)
	- changed syntax command html : 'html x,y,some-html-code'
	- drag/drop centering for html/latex/mathml/images

```
size 500,500
xrange -10,10
yrange -10,10
grid 1,1,grey
linewidth 3
vline 0,0,green
hline 0,0,green
fontfamily 16px Arial
fillcolor red
strokecolor blue
drag xy
centered
latex 0,0,\frac{12345}{23456} \times \frac{12345}{23456}
drag xy
centered
latex 5,5,\frac{12345}{\frac{12345}{ \frac{12345}{12345}}}
centered
drag xy
html -5,5,<table border=1><tr><td>1234567</td></tr><tr><td>1234567</td></tr><tr><td>1234567</td></tr><tr><td>1234567</td></tr></table>
drag xy
centered
copy 5,-5,-1,-1,-1,-1,gifs/images/skull_and_crossbones50.png
centered
drag xy
html 5,0,<img src=gifs/images/skull_and_crossbones50.png />
```


* 13/3/2020:
	- corrected stupid error in default replyformat
	- added support for touch/android for 'drag_xml()' 'drag_external_images()'
	- unified onclick/drag&drop answer format for MathML,JS_TeX,bitmap-images,html5-canvas-objects
	- compiler warnings (BPR)

* 10/3/2020:
	- command 'rotate angle (deg)' now works for 'draw_xml()' e.g. for commands like latex,mathml,html,video,audio etc
	- corrected drag&drop reply for external things...

* 7/3/2020:
	- command functionlabel is now converted to js-array "functionlabel f(x)=:g(x)=:h(x)=:f'(x)="
	- so multiple funtion inputs may all have different colours and function agruments

* 6/3/2020:
	- added alternative command 'userdraw function,color' , is identical to 'userinput function'
	- added alternative command 'userdraw functions[n],color' , is identical to n-times 'userinput function' (n=1..9)
	- command 'multistrokecolors' or 'multicolors' may be used for setting  function argument/plot colour

* 15/2/2020:
	- corrected flaw in drawing+zooming+multiple jscurves
	- userinput for arrows and polylines (forgotten and corrected)
	- zooming js-curves created by 'userinput function' (or should it be 'userdraw function'?...)
    will adapt to new x/y-ranges after re-clicking the 'draw' or 'OK' button. This is a flaw and not a feature !!

* 13/2/2020:
	- corrected stuff related to 'onclick'
	- corrected combo of clickreply+dragreply

* 11/2/2020:
	- corrected commands 'arc' and 'angle'
	- reset() will not reset the 'use_rotate' eg only 'killrotate' will set 'angle = 0.0'
	- default css_class="none"
	- corrected flaw in command 'input' ('style' is no longer needed...-> css)

* 9/2/2020:
	- restored command 'userinput function|inputfield'
	- 'userdraw text,color' uses `text_inputfield+mouse` and/or `text_inputfield+xy-inputfields`

* 7/2/2020:
	- made a few corrections (Marina)
	- 'userinput inputfield'

* 6/2/2020:
	- added (forgotten) 'userdraw demiline(s),color'
	- restored command 'userinput inputfields|textarea|function'

* 5/2/2020:
	- forgotten rotation/translation for object 'segment'
	- restored 'setlimits' and added 'redraw_jsplot()'
	- added 'jsplot' to 'zoom'

* 4/2/2020:
	- restored copy/copyresized with onclick or drag xy
	- start with affine transformation without using canvas/html5 transform system (e.g. drag&drop&click will work for transformed objects)
    [work in progress]
	- added commands 'linear a,b,c,d' and 'killlinear|killinear' (flydraw compatibility needs to be tested)
	- updated internal doc's; (todo : example scripts)
	- added forgotten userdraw arrow(s)2,color

* 31/1/2020:
	- cleaning: removed unused js-functions
	- work in progress: combined drag/drop external images, draggable copy/resized images and dragstuff objects

* 30/1/2020:
	- userdraw zooming ; marked 'canvas-zoom-buttons' forbidden (eg clicking on zoom will not produce extra userdrawing clicks...)
	- start with cleaning...

* 29/1/2020:
	- minor stuff

* 28/1/2020:
	- restored 'userdraw dotfill...textfill,color|text' (pattern filling)
	- restored 'userdraw inputs'
	- changed 'style' for elements by command 'css some_css' for buttons etc (style="xxxx" --> class="xxxx" )

* 26/1/2020:
	- restored 'userdraw clickfill,color' (the lrest like 'userdraw dotfill,color' not yet ready)

* 25/1/2020:
	- rightmouse click removes last drawn userdraw thing
(including svg/div/p/span/images/inputfields...eg non-canvas elements)
	- restored 'userdraw inputs,color'

* 24/1/2020:
	- restored "userdraw" image(s),arc(s),parallelogram(s)  etc

* 22/1/2020:
	- moving "userdraw" things to separate file, simular to "multidraw"
    work in progress...not all primitives are implemented.
    giving further reduction of resulting javascript filesize

* 17/1/2020
	- zooming : added mouse wheel zoom-in-out
	    : restored panning and reset (x) is now back to original x|y min|max
	    : added basic support for touch devices
	    : TODO adjust zooming params

* 16/1/2020
	- removed unused variables (clang, compiled with: -Weverything)
	- zoom & drag/drop
	- TODO: zoom & other-grid-types ; use mousewheel & tablets?

* 11/1/2020
	- corrected long standing design flaw (zooming would reset dragged objects...)
	- modified command grid (concering zooming)
	- TODO : rethink zooming & panning !!

	```
	##############
	performance/size_reductions:

	U1/logic/oefmap.fr&+cmd=new&defaultlist=free01&+exo=free01&+qnum=1&+scoredelay=&+seedrepeat=0&+qcmlevel=1
		- old canvasdraw version approx 40 kb
		- new canvasdraw version approx 20 kb
		- latest canvasdraw version approx 18 kb

	H5/analysis/gebieden-1.nl&subject=4&level=0&total_exos=3&rounding=-1&usage=2&taal=nl
		- old canvasdraw version approx 83 kb
		- new canvasdraw version approx 38 kb
	##############
	```

	- split large files into fragments:still needs some further shifting/reorganizing
	- multidraw: add draw primitive: crosshair | crosshairs (draw_type no. 25 | 26)
    TODO: arc,angle,ellipse??

	- multidraw: finished fillpatterns

```
size 400,400
xrange -10,10
yrange -10,10
grid 1,1,grey
linewidth  3
snaptogrid
multilinewidth 1,2,3,1,2,3
multifillcolors red,green,blue,brown,cyan,yellow
multistrokecolors red,green,blue,brown,cyan,yellow
#solid, grid, hatch, diamond, dot, none
multifill 1,2,3,4,0
multidraw rects,circles,triangles,poly5,parallelogram,points
```

* 10/1/2020
	- simplified 'fillpattern' for static and dynamic objects
	- multidraw:
    started with implementing fillpatterns for multidraw (only rects/circles for now)
    command: multifill 0,1,2,3,4,5 ... none,fillcolor,grid,hatch,diamond,dot
    image filling not yet ready...
	- corrected flaw in 'ellipses'
	- corrected docs for 'ellipse | ellipses'
	- corrected zoom issue (minimized code increase)

* 8/1/2020
	- added a few other object to "non-html5-canvas" rotation

* 6/1/2020
	- "stringup" now can be "onclick & dragdrop"
eg: stringup == string+angle+mouse_rotation-code
note: no need to reset rotation through 'killrotate'

	```
	onclick
	rotate 45
	string red,0,0,AAAAAA
	killrotate
	string red,4,4,BBBBBB
	```

	is identical with:

	```
	onclick
	stringup red,0,0,45,AAAAAA
	string red,4,4,BBBBBB
	```

	- dragstuff using C-switch
	- code kbytes:

```
 minimal: 		 4 kb
 grid only: 		15 kb
 userdraw only: 	10 kb
 multidraw single:	18 kb
 multidraw all: 	40 kb
 onclick + objects: 	14 kb
 drag xy + objects: 	14 kb
 typical exercise : 	30 kb
```

* 4/1/2020
	- moved dragstuff code : needs to simplify
	- start to move HTML5/Canvas transformation things to canvasdraw.c:
		- e.g. just recalculate the coordinates for objects: so there is no need for recalculating mouse coordinates in case of onclick/drag
		- except for 'text' and ?
		- keep optional mouse coordinate recalculation when using HTML5 transformations...


* 11/2019
	- rewrite of multidraw code and removal of verbatim js-code
	A few notes:
	- command 'inputstyle' will be used for setting 'css class' of the multidraw control table
    for example: inputstyle wimstable
	- when using a single draw primitive , command 'multilable nocontrols' will remove the 'control buttons'
	- when using keyword "duplicates" || "allowdups"  multiple identical points will NOT be removed from the student answer/reply
	- drag&drop math typesetting onto canvasdraw image:
	    - multidraw|userdraw images will only work with Mathml (through !insmath etc) if you use the 'wims modules svn' version of wims_mathml !!
		- e.g. things like !insdraw  \mmlid{123} \frac{1}{\pi}
		- of course the 'id' will only be added to a single call: this will NOT work: \mmlid{1} \frac{1}{\pi} + \mmlid{2} \frac{\pi}{2}
	   - multidraw|userdraw images and MathJaX will not work !
	   - multidraw|userdraw images and native MathML and KaTeX will work
	- drag&drop external material onto canvasdraw image:
		- all objects/content of type 'p','span','div','bitmap image','svg image' with an unique 'id' and
       onclick handler "onclick='javascript:place_image_on_canvas(this.id)'" may be placed onto a canvasdraw image
       when 'userdraw images,boguscolor' or 'multidraw images,...' is declared.
	- currently implemented multidraw primitives:
		*     "point","points",
		*     "circle","circles",
		*     "line","lines",
		*     "segment","segments",
		*     "arrow","arrows",
		*     "triangle","triangles",
		*     "closedpoly",
		*     "text",
		*     "rect","rects",
		*     "poly","polys",
		*     "parallelogram","parallelograms",
		*     "images",
		*     "curvedarrow","curvedarrows",
		*     "curvedarrow2","curvedarrows2"

	- TODO: add a few more draw primitives, like
    "crosshair(s)","ellipse(s)","arc(s) || angle(s)"...


############## REWRITE ################################

added 'angle' to draw_xml()10/4/2020
typo in 'case 12' command 'arc'

* 23/3/2020
added alternative commands for a few 'multidraw' subcommands
typo in doc
* 29/1/2020
stringup may now be set onclick (drag&drop not supported: is implemented in 'canvasdraw.min')
corrected non-univeral path to wims_mathml
* 6/1/2020
forgotten to reset angle=0 in 'killrotate'

## 2019
* 26/12/2019
NO support for dragging and transformation / rotations
generic mouse coordinate transformation ( transform_mouse(x,y,obj) ) usable for 'affine transformation' and 'onclick'
* 25/12/2019

"drag" and "rotate" WILL NOT WORK !!
"onclick" and "rotate" may be combined. (see createxo help "rotate")

adjusted documentation.
string/text using 'xoffset','xyoffset','yoffset' will now use a 2*linewidth marge
22/12/2019

object 10: clicked
object 9 : clicked
object 8 : not clicked
object 7 : not clicked
object 6 : clicked
object 5 : moved from (1:2) -> (2:2)
object 4 : not moved
object 3 : moved from (1:1) -> (3:3)
object 2 : moved from (1:3) -> (3:2)
object 1 : moved from (1:0) -> (2:1)
object 0 : not moved
reply = ["not_moved", "1:1:0:2:1:0", "2:0:1:3:2:0", "3:1:1:3:3:0", "not_moved", "5:1:2:2:2:0",1,0,0,1,1]
for example:

the clicked objects are '0' or '1'
obj_number,xorg[0],yorg[0],xnew[0],ynew[0],new_angle
moved object are items in the javascript reply array...in the same order as their appearance in the fly_script
unmoved object are labelled "not_moved" !
"onclick" and "drag x|y|xy" may now be used together and all data be read using "read_dragdrop()"

[ these are bugs, not features... ]
drag xy & rotate are only compatible if you are using a 'slider'
onclick & rotate are not compatible
impoved mouse selection of text (height of selection box was not ok)
corrected missing font-change when text was set 'onclick' [BPR]

* 21/12/2019
corrected wrong hexcolor conversion [Sophie Lemaire]

* 12/11/2019
if set, multiple identical points will NOT be removed from the student answer/reply
added keyword "duplicates" || "allowdups" for default "multidraw" replyformat
* 9/11/2019
corrected multidraw curvedarrows numeric input [BPR]
* 8/11/2019
correction in multidraw circles : list_uniq(A,B,C) will reserve 'C' for non-numeric answers !

* 2/11/2019
this will break module "H4/logic/logica-1.nl/exos/exo14" (only dutch module, so no serious conflicts)
reply format for "multidraw" in case of "circles" will give radius in x-range [BPR]

* 2/11/2019

```
#######
}
<input type="button" onclick="alert(read_dragdrop());" value="read_dragdrop()" />
<input type="button" onclick="alert(read_canvas());" value="read_canvas()" />


</script>
 find_images();
 };
  };
   };
    img.setAttribute('onclick','javascript:place_image_on_canvas(this.id);');
    idx++;
    img.setAttribute('id','drag_'+idx);
   if(id.indexOf('insert')!= -1){
   var id = img.id;
   var img = images[p];
  for(var p=0;p<len;p++){
  var idx=0;
  var len = images.length;
  var images = document.getElementsByTagName('img');
 /* look for images with 'insert' in the 'id'*/
 function find_images(){
 };
   if(typeof fun === 'function'){return fun();};
   var fun = eval("read_dragdrop"+canvas_scripts[0]);
 function read_dragdrop(){
 };
   if( typeof fun === 'function'){ return fun();};
   var fun = eval("read_canvas"+canvas_scripts[0]);
 function read_canvas(){
<script type="text/javascript">

</table>
 <tr><th> <img src="gifs/en.gif"  id="insert_en" /> </th><th>  <img src="gifs/nl.gif"  id="insert_nl" /> </th><th>  <img src="gifs/cn.gif"  id="insert_cn" />  </th><th>  <img src="gifs/it.gif"  id="insert_it" />  </th></tr>
 <tr><th> \I1   </th><th> \I2 </th><th> \I3 </th><th> \I4 </th></tr>
 <tr><th> \(\M1\) </th><th> \(\M2)</th><th> \(\M3) </th><th> \(\M4\) </th></tr>
 <tr><th> M1   </th><th> M2 </th><th> M3 </th><th> M4 </th></tr>
<table border="1">
}
# multidraw images
userdraw images,red
snaptogrid
centered
grid 2,2,grey,1,1,6,grey
axisnumbering
axis
precision 1
yrange -10,10
xrange -10,10
bgcolor white
\canvasdraw{500,500}{

\statement{

\text{divstyle="display:inline-block;width:auto;text-align: center;"}
\text{I4=<img style="width:50%;height:50%" onclick="javascript:place_image_on_canvas(this.id);" src="modules/data/chemistry/mol2D.fr/images/100_39_0.png" id="I4" />}
\text{I3=<img style="width:50%;height:50%" onclick="javascript:place_image_on_canvas(this.id);" src="modules/data/chemistry/mol2D.fr/images/1002_69_3.png" id="I3" />}
\text{I2=<img style="width:50%;height:50%" onclick="javascript:place_image_on_canvas(this.id);" src="modules/data/chemistry/mol2D.fr/images/1002_57_9.png" id="I2" />}
\text{I1=<img style="width:50%;height:50%" onclick="javascript:place_image_on_canvas(this.id);" src="modules/data/chemistry/mol2D.fr/images/1002_28_4.png" id="I1" />}
\text{M4=\mmlid{M4}\xrightarrow{\rm N_2 \uparrow}}
\text{M3=\mmlid{M3}\xrightarrow{\rm O_2 \uparrow}}
\text{M2=\mmlid{M2}\xrightarrow{\rm H_2 \uparrow}}
\text{M1=\mmlid{M1}\xrightarrow{\text{reaction}}}
##### example OEF...
```

	- 'userdraw images,bogus color' : right mouse click removes item/object under mouse pointer
	- solving issue with size of dynamic created div's in 'userdraw images,color' and 'multidraw images'

* 26/9/2019

multidraw images
userdraw images,red
using command:
including KaTeX or MathJaX js-typesetting, MathML,bitmap, and any other html content,
All 'svg','div','span' elements on page may be used to drop onto canvas,

* 17/9/2019
on touch devices the canvasdraw inputfields could not be used
add attrubute autofocus,true to function 'add_inputs()'

* 8/9/2019
will now accept anything from the exercise page (including MathML,MathJaX,KaTeX typesetting) if included a 'div' with unique id and onclick='javascript:place_image_on_canvas(this.id);'
multidraw images
userdraw images,somecolor

* 5/9/2019
case killslider: replaced 'int p' by loop indicator 'i' [obado]

* 27/8/2019
TEMPORARY ---> all browsers are type 'GECKO' and MathJaX will take care of all others in command 'latex'

in case of KaTeX only use command 'latex x,y,tex_string'
command mathml is only suitable for "GECKO" and "NON-GECKO+MathJaX"
command latex calls 'getMML()' e.g. 'wims_mathml'  in case of "GECKO"

* 26/8/2019
Needs looking into. In mathml.c: katex div: 'display:block;visibility:hidden'./SCHAERSVOORDE/H1/algebra/vergelijkingen-1.nl/exos/exo9:mathml 0,10,10,0,$fun\
setting 'drag_type = -1' to drag KaTeX with left top corner...problems reading width of created div's/span's
WORK IN PROGRESS ( canvasdraw will need to call wims_mathml in case of GECKO browsers!)
to be used for KaTeX
Added command 'latex x,y,tex-string

* 25/8/2019

```
fcircles blue,3,3,1
drag xy
angle 0,0,2,0,0,red
fillpattern dot
arrow 0,0,5,0,7,red
fillcolor blue
slider -pi,pi,250,30,angle degrees,Rotate
opacity 200,100
fillcolor white
rotationcenter 0,0
linewidth 3
precision 1000
yrange -5,5
xrange -5,5
size 300,300
```

- click on 'reset' will deactivate all
- click on objects will activate
- command 'angle x0,y0,x1,y1,angle,color' will always be active in case of a rotation slider.
- drag x|y|xy may be combined with slider(s)
- snapto* will not work !
in slider exercises:

onclick and dragdrop may be combined, but is not really usefull :)
- every onclick object starts with obj.onclick_cnt = 0... when clicked obj.click_cnt = 1
- only the first element of a row is important (obj.click_cnt), which is 0 or 1
in onclick exercises

- obj.click_cnt is the objects sequence number (starting with '0' for the first draggable object in the script)
- only clicked objects have a line in the reply
in dragdrop exercises:

obj.click_cnt+','+obj.xorg[0]+','+obj.yorg[0]+','+current_x+','+current_y+','+current_angle+';\\n
dragdrop reply is now in format:
broken modules adapted

* 15/8/2019

   -- todo: restore slider 'follow_function system'
6) -- todo: restore xy-slider??
6) finished: reply&display precision and reply-format
5) finished: restored slider display 'system'
4) finished: slider rotation with preservation of coordinates e.g. 'mouse dragging' is supported
3) finished: group selection (just by clicking...no 'lasso')
2) finished: real coordinates when using 'drag & manual rotating' objects
1) finished: multiple sliders per object or objects (until 'killslider')
targets:

* 8/2019 rewriting of slider stuff (reponse to request/question of BPR)

* 10/8 correctedd stupid typo in DRAW_EXTERNAL_IMAGES
and corrected snapping to grid
corrected flaw in multidraw cirle [BPR]
* 3/7/2019
removed 'touchend' (MC)
* 22/6/2019
(changes should be made, not yet fixed)
work on the documentation: add '' ``for emphased words and <code></code>
* 08/06/2019
`userdraw circles,red`
is identical to
`multidraw circles
strokecolor red
multilabel NOCONTROLS`

in multidraw NOCONTROLS and a single object, no need for a 'draw object' button. It's analogue to userdraw.
in multidraw right mouse click will remove last drawn object of the currently selected  draw primitive

* 7/6
added ruler & protractor to new multisnap_check(x,y,use_snap);

```
#<input type='button' onclick='javascript:var fun=eval("clear_draw_area"+canvas_scripts[0]);fun(9,0);' value='REMOVE LAST ARROW ' />
<input type='button' onclick='javascript:var fun=eval("clear_draw_area"+canvas_scripts[0]);fun(24,0);' value='REMOVE LAST CURVEDARROW2 ' />
<input type='button' onclick='javascript:userdraw_primitive=24;multidraw_click_cnt = 0;' value='start drawing curvedarrows2' /> etc etc
<input type='button' onclick='javascript:userdraw_primitive=null' value='STOP DRAWING' />
```
controls for example:

```
curvedarrows2 = 24
curvedarrow2 = 23
curvedarrows = 22
curvedarrow = 21
images = 20
parallelograms = 19
parallelogram = 18
polys[3-9] = 17
poly[3-9] = 16
rects = 15
rect = 14
text = 13
closedspoly = 12
triangles = 11
triangle = 10
arrows = 9
arrow = 8
segments = 7
segment = 6
lines = 5
line = 4
circles = 3
circle = 2
points =1
point = 0
```
The object types are internally represented by the following numbers (makeing typo's will render your exercise null and void)
To activate this feature, use multilabel NOCONTROLS
When you are not content with the default 'multidraw control panel', you can create your own interface, using a few javascript functions to call the drawprimitives, delete things and 'stop drawing' in case you also want to drag&drop stuff...
### from doc's
6/6 added keyword 'NOCONTROLS' to disable all buttons for multidraw

command 'drag xy|x|y' was fallen of the wagon...

don't touch replyformat 29: it is universal and for multidraw  !
todo: modify/change 6,10,11,15,16,17,18,19,20,25,27,31

OEF : replyformat		22,23,28
USERDRAW DEFAULTS: replyformat  2,6,8,10,11,15,16,17,18,19,20,23,24,25,27,29,31
SCHAERSVOORDE : replyformat	2,7,8,21,22,23,24
Starting with unifying the 'replyformat' jungle...without touching existing modules
A single function "multisnap_check(x,y,use_snap)" will return xy[2] array with corrected  coordinates

* 5/6 removed confusing code about 'snapping to something' and unified it in a single variable 'use_snap=0,1,2,3,4' for 'dragstuff library', 'userdraw' and 'multidraw'
* 4/6 modified 'find_angle()' now using Math.atan2(x,y) ... smooth turning 360+ without flipping...see 'protractor'

```
<script>
 resize_svg_mathml(mythings);
 var mythings = [ array_with_image_id's ];
 };
  };
   svg.setAttribute("width", w+'px');
   svg.setAttribute("height", h+'px');
   var h = parseInt(getComputedStyle(div).height);
   var w = parseInt(getComputedStyle(div).width);
   var div = document.getElementById(mythings[p+1]);
   var svg = document.getElementById(mythings[p]);
  for(var p=0; p < len ; lpp){
  var len = mythings.length;
 function resize_svg_mathml( mythings ){
<script type="text/javascript">
To resize and used the svg element, a small piece of javascript is needed..
</svg>
</foreignObject>
<div xmlns="http://www.w3.org/1999/xhtml" style="display:inline-block;width:auto;text-align: center;" > MATHML </div>
<foreignObject width="100%" height="100%">
<svg xmlns="http://www.w3.org/2000/svg" width="1px" height="1px" onclick="javascript:place_image_on_canvas(this.id);" id='my_id'>
```
when the MathML code is embedded in a foreignObject, a div and an svg with size 1px, like in:
On Gecko browsers there is an option to include MathML from the page onto the canvas,
The 'id' and (x;y) coordinates will be returned using read_canvas();
`<img src="somewhere" id="myid" onclick="javascript:place_image_on_canvas(this.id)" alt="hmm" />`
Images (bitmap or svg) present in the exercise page and the img-tag with an unique 'id' and onclick='javascript:place_image_on_canvas(this.id)' can be placed onto the canvas.
(*)

multidraw curvedimages,curvedimages2
multidraw images  (*)
userdraw curvedimages2,color
userdraw curvedimages,color
userdraw curvedimage2,color
userdraw curvedimage,color
userdraw images,boguscolor (*)
using: in canvasdraw.c: int use_snap = 0; /* 0 = none 1=grid : 2=x-grid : 3=y-grid : 4=snap to points ... to finally harmonize the 'snap to grid etc etc */
using multisnap_check(x,y,snap_type)
preparing replyformat 29 (multidraw) as generic reply system (every object has a line, every line is a matrix)
Merged with wimsmodules:


y1,y2,yc,...
x1,x2,xc,...
default replyformat = 2

* 3/6 curvedarrow now has 3 points :

added to multidraw family 'curvedarrow' and 'curvedarrow2'
added to dragstuff library objects 'curvedarrows color,x1,y1,xc,yc,x2,y2,...' and  'curvedarrows2 color,x1,y1,xc,yc,x2,y2,...'
just to be complete...
added to dragstuff library objects 'curvedarrow x1,y1,xc,yc,x2,y2,color' and  'curvedarrow2 x1,y1,xc,yc,x2,y2,color'
added 'userdraw curvedarrow2,color' and 'userdraw curvedarrows2,color'

* 1/6/2019
TODO: add curvedarrow to dragstuff object library
TODO: multidraw curvedarrow
TODO: 'userdraw curvedarrow2,color' and  'userdraw curvedarrows2,color'
* 31/5/2019 added userdraw primitive 'userdraw curvedarrow,color' and  'userdraw curvedarrows,color'
* 15/5/2019 dragging external images now centers the image around the mouse pointer
all other drag&drop things (canvas related and div's (from xml/mathml) seem to work fine, only dragging of images fails
this -however- does not seem to correct the problem om 'styled' wims-servers [BPR].
draw_external images now makes use of the same dragstuff.getMouse()
* 14/5/2019 problems with dragging images...
* 13/5/2019 bugfix command angle|arc combined with command 'slider'...slidervalue is in radians...arc|angle needs degrees
* 11/5/2019 add some examples for OEF [BPR] (work in progress)
* 4/3/2019 corrected fontsize behaviour of command 'text' (always trouble between 'fontsize' and 'fontfamily' commands) [BPR]
* 2/2/2019 corrected behaviour of replyformat 11 etc (dealing with (x1:y1)---<x2:y2) )

```
jsplot red,4*cos(2*x),2*sin(3*x-pi/6),cos(x),3*sin(x),3*cos(x),sin(x),x,x^2
multistrokecolors red,red,blue,blue,green,green,orange,orange
precision 1000
linewidth 1
animate
trange -pi,pi
fillcolor blue
linewidth 4
grid 1,1,grey,4,4,7,black
opacity 100,190
axisnumbering
axis
precision 0
yrange -5,5
xrange -5,5
size 400,400
```
(command animate may only be used once)

* 28/12 multiple animated curves using command jsplot
* 25 --> 26
corrected "snprintf(tmp_buffer,25,"use_mouse_coordinates();\n");add_to_buffer(tmp_buffer);"
* 15/10 BPR
* 12/4 piechart will show 'legend' text inside piechart when using keyword 'centered': use fill opacity to enhance visibility
* 14/3/2018 draw_crosshairs: forgotten ctx.save(); (crosshairs will 'move' with every click...)
* 13/11 live docs
* 12/11 added command 'ellipses'
* 6/11 added few things to "live documentation" work in progress
* 18/10 reoganised string/text keywords 'centered','xoffset','yoffset','xyoffset'
* 8/10 remove duplicates while drawing 2-point objects (circle/line/demiline/segment/rect)

* 4/10 work in progress: adding examples to documentation (the "example:"  flag can be used to extract)

* 2/10 fixed bugs (thx B.Perrin)
* 28/9 bug fix : variable use_axis_numbering was reset by introduction of numberline command...
* 20/9 bug fix in command curve (a parameter was wrongly set, resulting in curve being type=13 (is a circle) insteadof type=9

```
mouse blue,22
precision 1
userinput function
functionlabel f(x)=
strokecolor green
jsplot red,4*cos(2*x),2*sin(3*x-pi/6)
precision 1000
linewidth 1
animate
trange -pi,pi
fillcolor blue
linewidth 4
grid 1,1,grey,2,2,5,black
opacity 100,190
axisnumbering
axis
precision 0
yrange -5,5
xrange -2*pi,2*pi
size 400,400
popup
```

* 17/9 added basic 'point_on_curve' animation (parametric and 'normal' functions)

```
zoom red
numberline -5,5,2,10,-3.5,-3
zoom red
xaxis -4:AAAAA:-2:BBBBB:2:CCCCC:4:DDDDD
strokecolor purple
numberline -5,5,1,8,-2,-1.5
xaxisup -4:AAAA:-2:BBBB:2:CCCC:4:DDDD
strokecolor blue
numberline -5,5,1,5,2,2.5
xaxis -4:AAA:-2:BBB:2:CCC:4:DDD
strokecolor green
numberline -5,5,1,4,4,4.5
xaxisup -4:AA:-2:BB:2:CC:4:DD
strokecolor red
opacity 255,255
grid 1,1,grey
hline 0,0,black
xaxis -4:A:-2:B:2:C:4:D
noyaxis
yrange -5,5
xrange -5,5
size 400,400
```
     adding y-offset when x-values overlap.
     multiple numberlines are allowed using multiple 'xaxis' commmands for labeling several numberlines, including a x-axis label in command grid...
     numberline is mapped to xrange/yrange
* 15/9 added command macro "numberline x0,x1,xmajor,xminor,y0,y1"

     solved long  standing issue with color 0,0,0 (black) as fill border
     added 'fixed' pattern filling to primitives 'boxplot' and 'piechart'
* 6/9  just for completeness: added pattern filling to all fillable userdraw primitives

```
clearbutton removed
userdraw gridfill,red
circle 10,10,100,red
imagefill 0,0,0,gifs/en.gif
# syntax: x,y,scale?,image
plot blue,-x^2+4
plot red,x^2-4
yrange -5,5
xrange -5,5
size 150,150
```

NOTE: special filling (hatch,dot,...,image) should be harmonized to include userdraw and drag&drop library objects !!
26/8 command imagefill x0,y0,URL now works like flydraw should

```
fcircle 2,-3,100,cyan
drag xy
fillpattern gifs/fr.gif
fcircle 2,3,100,green
drag xy
fillpattern gifs/de.gif
fcircle -2,-3,100,red
drag xy
fillpattern gifs/nl.gif
fcircle -2,3,100,blue
drag xy
fillpattern gifs/en.gif
snaptogrid
yrange -5,5
xrange -5,5
size 400,400
```

note: needs an alert for synchrone loading of images (TO DO: closures in fill routines ? )

* 25/8	added commmand 'fillpattern image_url' to use an image as 'ctx.fillStyle()'
* 20/8	added patternfilling to 'userdraw' family

```
zoom red
userdraw dotfill,blue
fcircle 3,-3,160,cyan
fillpattern diamond
fcircle 0,3,160,green
fillpattern hatch
fcircle -3,-3,160,red
fillpattern dot
fcircle -6,3,160,blue
fillpattern grid
opacity 165,160
yrange -5,5
xrange -5,5
size 370,370
```

* 18/8	added command 'fillpattern grid | dot | hatch | diamond'
* 14/8 	added commands 'textfill x,y,color,sometext' and 'userdraw textfill,color,some_text'
* 13/8	updated html docs

```
clearbutton REMOVE
fill 0,5,red
gridfill 4,0,3,3,blue
diamondfill -4,0,6,6,cyan
dotfill 0,0,6,6,green
linewidth 1
userdraw hatchfill,red
plot blue,-x^2+4
plot red,x^2-4
yrange -5,5
xrange -5,5
size 150,150
```

	userdraw clickfill,color (will use only color,no pattern)
	userdraw hatchfill,color
	userdraw dotfill,color
	userdraw gridfill,color
	userdraw diamondfill,color

* 12/8  added to userdraw family (not yet 'cleaned' the js-code)  the commands:
* 11/8  function 'find_angle' was gone fishing in case of command 'protractor'
        added gridfill,hatchfill,diamondfill
example:
     added pattern to command 'userdraw clickfill,color'
     other patterns will -hopefully- follow
* 10/8 premature version of pattern filling ; for now only 'dotfill x,y,dx,dy,color'
     if set the text will not start at given (x:y) but end at (x:y)
* 8/8  added keyword 'yoffset' for use with rotated text strings (command textup or stringup)
* 4/8  multidraw primitive 'text' is now always x-centered on click-coordinates
     reorganized canvasdraw.c : commands in alfabetical order (cleaning & formatting not yet ready)
     (added 'this.use_once')
* 3/8  corrected flaw introduced by keywords xyoffset etc... removed repeated shifting of text-strings in redraw of canvas
     the inputfields will be x/y centered on (x:y)... default was left top corner
     keyword xoffset also active for commands 'input x,y,size,editable,value'  and 'userdraw input(s),color'
* 2/8  added keywords xyoffset,xoffset|centered,resetoffset for easier text-label placing close to 'objects' like crosshairs,points...
* 26/7 multidraw_button_table now centered in tooltip placeholder div element (using style="margin: 0 auto;" )
* 25/7 multidraw command 'text' now will place a 'text string' on canvas via a mouse-click or (if command multiuserinput is set to 1 for 'text') via x/y inputfields
* 24/7 corrected multidraw text error (duplicate text)
* 15/7 added command 'fillall color,x1,y1,x2,y2,x3,y3...xn,yn' for filling multiple speparated areas with the same color
* 14/7 set timeout on clickfill and while waiting set cursor to 'wait'

```
clearbutton REMOVE
userdraw clickfill,purple
colorpalette orange,yellow,red,green,lightgreen,blue,lightblue,cyan
# multifillcolors orange,yellow,red,green,lightgreen,blue,lightblue,cyan
replyformat 10
snaptogrid
grid 1,1,grey
canvastype 4
# use the grid canvas 4 as fill borders
yrange -5,5
xrange -5,5
size 400,400
```
example of a simple clicktile in direct exec:

      this will bind coordinates with color...if the exercise wishes.
      added command 'colorpalette color1,color2...color2' for creation of color-buttons to change the click color.

      to identify the correct color on the correct area...
      When using something like 'replyformat 10' or another format reporting the 'userdraw_radius[]' the color-number will be part of 'read_canvas()'
      the colors will re-cycle when there are more clicks than colors...

* 13/7  'userdraw clickfill,color' may now make use  of command 'multifillcolors color1,color2,color3...color_n'
      fill commands may be used together with 'userdraw clickfill,color'
      (it will still ignore the bordercolor...I just can't make this work... grrr )
      command 'filltoborder x,y,bordercolor,color' is still the same as fill
      command 'fill x,y,color' or 'floodfill c,y,color' may now be used multiple times.

      read_canvas() will return the click-coordinates
      areas can be removed (after confirm 'clear ?' ) with command 'clearbutton some_text'
* 11/7  now 'userdraw clickfill,color' can fill multiple selected area's
      (although this is possible through foreignobject --> blob --> image --> canvas )
      will be located at (1:0) and the mathml will decide the width and height of the drag/click div's (the div's are added to the 'mother' div and not -of c ourse- to the html5 canvas

      ```mathml 1,0,-10000,10000, <span style="font-size:1em;">
      <math xmlns="http://www.w3.org/1998/Math/MathML" display="inline">
       <mstyle id="wims_mathml347806" mathsize="110%">
        <mstyle mathsize="200%">
         <mstyle displaystyle="true">
          <mfrac>
           <mi>?</mi>
           <mrow><mi>sin</mi>
            <mrow>
             <mo stretchy="true">(</mo>
             <mstyle displaystyle="true">
              <mfrac><mi>?</mi><mi>?</mi></mfrac>
             </mstyle>
             <mo stretchy="true">)</mo>
            </mrow>
           </mrow>
          </mfrac>
         </mstyle>
        </mstyle>
       </mstyle>
      </math></span>```

      so the left top corner of
      however, the  values must still be defined (syntax backwards compatibility)

* 8/7   command mathml will use fill_color/fill_opacity when dragged / clicked ; x2,y2 (used for determining the width/height of the embedded mathml div, are no longer used
* 6/7   improved onclick & (standard) reply format
* 5/7   improved drag&drop in case of mathml (or better all xml/html inclusives)
* 4/7   corrected -again?- color cludge in 'barchart'...
* 20/5  added forgotten minimal touch-support for multidraw routines
      drag xy|x|y and [xy]snaptogrid are supproted
      (it's not really dragging , but selecting and moving to a new location by click)
* 12/2  added very primitive dragging to 'mathml'

	```
      line 11 = text_x+";"+text_y+";"+text"\n"
      line 10 = parallelogram_x+";"+parallelogram_y+"\n"
      line 9 = closedpoly_x+";"+closedpoly_y+"\n"
      line 8 = rects_x +";"+rects_y+"\n"
      line 7 = polys[3-9]_x+";"+polys[3-9]_y+"\n"
      line 6 = triangles_x+";"+triangles_y+"\n"
      line 5 = lines_x+";"+lines_y+"\n"
      line 4 = arrows_x+";"+arrows_y+"\n"
      line 3 = segments_x+";"+segments_y+"\n"
      line 2 = circles_x+";"+circlespoint_y+";"+multi_radius+"\n"
      line 1 = points_x+";"+points_y+"\n"
   ```
      The output is always a 11 lines string with fixed sequence.

      multiple parallograms may be used: the coordinates are stored in "line 10" in the reply
      multiple polys[3-9] may be used: poly[3-9] or polys[3-9] has "line 7" in the reply
      removed command poly[s][3-9] and parallelogram[s] from 'triangle' sub-command and used as additional commands.
      command multidraw
* 9/2

## 2017
      the same  canvas :making it possible to do a 'userdraw clickfill,color' & pan & zoom on the single js-curves-canvas
* 30/10 added some options to command jsplot/jscurve: multiple js-curves may now be plot in a single command and on
      But it's still a vary ugly cludge caused by the "Flydraw backwards compatibility"
      then this family&size will be used.
      if command "fontfamily 'size px Family'" was given and the size of "m" is larger then a set/default fontsize
      the context will use this fontsize in Arial...
      if command "fontsize int" is given and it's size is larger than the default fontfamily "12px Arial"

      ```
      ###
      if( font_size > tmp_font_size ){context_userdraw.font = font_size+\"px Arial\";};
      var tmp_font_size = parseInt(context_userdraw.measureText(\"m\").width);\
      ###
      ```
      corrected cludge of fontsize versus fontfamily :
      color was taken from fontcolor... and not from userdraw command... which is color --> strokecolor
* 29/10 corrected "userdraw text,color" :
      `x1,y1,text1 \n x2,y2,text2 \n` etc
      changed the default replyformat for 'userdraw text,color' and 'userdraw input(s),color' to the type:
      (in both cases a right | middle button click will always remove the drawings
      and added to 'clearbutton' the removal of inputfields produced by command "userdraw input(s),color"
* 28/10 corrected ctx-reset flaw in "userdraw text,color" when removing text with the "clearbutton xxx"
* 24/10 command vline | hline now have range -100*xmin | 100*ymax : so on "moderate" zooming there will still be a vline | hline
* 13/10 added keywords "noxaxis" "noyaxis" to "grid" command family
* 8/10  canvasinfo.sh will (after chmod +x) generate also canvasdraw.phtml and move it to "../../../public_html/scripts/help/en"
      so now every userdraw object may be deleted (indiviually) by a right mouse click...middle click still no good
      added 'oncontextmenu="return false;"' to main canvas_div to disable context menu with right click on image
* 7/10  added snapto*** to "userdraw clickfill,color"
* 6/10  moved command "clickfill color" to the userdraw command list e.g. userdraw clickfill,color: clickfill is per default on the drag_canvas (unless you specify an other canvastype)
* 5/10  added support for touch devices
* 30/9  `clear_draw_area()` used by "clearbutton txt"  did not reset the internal click-count-variable "xy_cnt"
* 29/9  corrected errors in replyformat 23 (B. Perrin)
* 26/9  tried to solve the syntax issues with polyline etc
* 11/9  added an option to command clock (and corrected the modulo bug :javascript remainder % is not the same as modulo)
      added some html info
* 10/9  corrected slider display (arguments "display x" and "display y" always showed "x : y" values
* 4/7   using '\t' as command separator (for use within OEF)
*       difference between 'arc' and 'angle': 'arc' may set width+height (ellipse)
* 15/2 corrected filling in command 'arc'
* 4/2  added command 'rotationcenter xc,yc' and keyword 'killrotate'
* 3/1  corrected fontsize flaw in commands text / textup

## 2016
	removed nonexisting 'div' argument 'tabindex=0' (thx to Perrin)

* 29/12 clearbutton was incompatible with chromium etc (revert to button.style = "style_text" when all browsers are supporting this construct)
* 18/9 corrected command 'centerstring' : forgotten clearRect()
* 17/9 corrected command clock: colors are now -really- optional
*       corrected -fixed- interactive boxplot stuff (js-related)
* 15/9 disabled 'crispy-lines' when zooming is activated (repeated translation of context)
*       e.g. producing crispy sharp lines : if(line_width %2 == 1){ctx.translate(0.5,0.5);}
* 12/9 now linewidth 1 == 1px and linewidth 2 == 2px
* 5/9  added command boxplot and jsboxplot
* 3/9  added commands xerrorbars,yerrorbars to dragdrop library
      minor corrections in html-doc for fontsize ; although it will not compensate for 'not-reading-the-docs' !
      corrected flaw in draw_points()
      (the single command 'userdraw type,color) will NOT ZOOM / PAN !!)
* 30/8 multidraw primitives will now zoom / pan (added function redraw_all())
       added several alternatives to html-doc
* 25/8 added command 'cursor some_css_cursor'
* 20/8 added 'rect' and 'rects' to multidraw command
* 10/8 added 'xaxisup' and modified grid extension 'barchart' using the xminor/x2step for the bar-width...
* 2/8   added 'text' to 'multidraw'
* 	when status=done the image will be shown in the page --> canvas_div.style.display='block'
* 28/7  read_canvas and read_dragdrop in popup window
*        no yet suitable for communication back to wims...[work in progress]
*        embedding canvasdraw into separate window xsize x ysize
* 26/7  added keyword 'popup'
* 21/7  added command `triangles color,x1,y1,x2,y2,x3,y3,...`

```
# reply[3]...reply[5] = protractor
# reply[0]...reply[2] = ruler
replyformat 32

protractor 2,-2,6,0,-1,1,1
ruler -4,3,5,1,-1,1
opacity 255,40
fillcolor orange

vline 0,0,black
hline 0,0,black
yrange -5,10
xrange -5,10
size 400,400
```

* 14/7 combinations of ruler & protractor
* 13/7 added command macro `ruler x,y,x-width ,y-height,passive_mode`
	snaptogrid implementes ( xy | x | y )
 	reply format implemented  `(read_canvas())  reply[0] = x;reply[1] = y;reply[2]=angle_in_radians`,
 	value display implemented,
 	left-right rotation with mouse (left click & drag),
 	zooming implemented,
* 12/7	added 'macro' command 'protractor x,y,x-width,type,passive_mode,value_display
* 8/7	revert to normal size of a point at 'linewidth = 1' (e.g. differ from ridiculous small point size of flydraw)
	This last -duplicated- point is removed from the answer array
	only 1 onject can be drawn (only with mouse).The poly is closed when clicking on the first point again (+/- 10px)
* 7/7	added argument 'closedpoly' to command 'multidraw'

	When things are more mature and speed is no longer an issue, the 'single userdraw' command will be replaced by multi*
	And yes, these are horrible long command names.
	multifillopacity,multiuserinput,multilabel,multilinewidth,multisnaptogrid
	multidraw,multidash,multistrokecolors,multifill,multifillcolors,multistrokeopacity,
* 26/6	multi-family of commands are 'finished'
        NOT READY: many issues are still unadressed

	(to increase speed of execution)
	The js-canvas drawfunctions will be different (simpler) compared with the single 'userdraw obj,color' functions
	using buttons to switch drawing primitive...
* 17/6   added experimental 'multidraw', allowing multiple drawing primitives in a single canvas image,

	added command 'snaptofunction some_fun_x,some_fun_y'

		frect -3,3,0,0,red
		slider -5,5,120,10,x function display,some_text
		sliderfunction_x 5*sin(x)

		'sliderfunction_y some_operation_on_y'
		'sliderfunction_x some_operation_on_x'
		added command
		added argument 'function' to the slider objects.
* 28/4	removed point2point...
* 24/4	added experimental command 'point2point x,y,color1,color2,x-relation_1:y-relation_1: ... : x-relation_n_:y-relation_n
	make background color in generated <span> elements adjustable via command 'fillcolor'
	make font color in generated <span> elements adjustable via command 'strokecolor'
	todo:
* 23/4	removed most static settings of fontsize and/or style.
* 22/4	corrected 'static fontsize settings' in some js-user-input stuff, using size set by 'command fontsize int'... to be continued
* 21/4	corrected missing 'ctx.restore()' in case of arrow (gives very strange  rotations when using a slider to rotate only the arrow...)
* 19/4	changing "abs" by "fabs" in case of floating point variables. (BPR)
* 8/4	added forgotten /*<![CDATA[*/ .... /*]]>*/ to script tag
* 25/2	Internet Explorer & "use strict" : changed 'span.style="color:..." into span.setAttribute('style','color:...');

	 (even those not set draggable)
	 note: all external images positions will be present in the 'read_canvas' array
	 the third image position is (-2:-5)
	 the second image position is (6:2) and
	 the first image position is (-5:4) ,
	 0:-5:4,1:6:2,2:-2:-5
	 e.g. expect in  case of 3 external images set draggable, something like
	 read_canvas() will reply "object_number:x:y" of all external images introduced by commands 'copy copyresized'

	- corrected replyformat 20 (only used for external image drag&drop... outside the dragstuff library !!)
	- corrected resetting of 'reply array' in case of zooming... (why did I reset this?)
* 17/2
	- added check on wims_status=done when using 'onclick' (otherwise objects can be 'clicked/dragged' in an answer.phtml)
	- simplified onclick things in external images
	- fixed bug when onclick external images were mixed with static external images
* 16/12 - corrected 'userdraw points,color' erase/redraw bug (using 'clearbutton buttontxt')
* 7/12	- onclick objects may now be of any type (eg external images ,via copy /copyresized or any other canvas 'object')
	 see canvassdraw.html for possible usage and limitations
* 25/11 - added "onclick" to external images (via commands copy / copyresized)
	-again tried to fix copy/copyresized (grrr)
	 and thus reset to "0,0,0"
* 24/11
	- corrected syntax-flaw in js-function draw_rects() ; both stroke_color and fill_color were not correctly defined
          analogue to command 'ellipse'
	- corrected command 'arc' : switched from width/height (width and height may now be different) in pixels to width / heigth in x/y-range
          now "linewidth 1" is almost invisible, just as in 'flydraw'...that is a clear improvement :)
	- synchronised size of point / points with flydraw
          The image may be of type BITMAP and SVG
          (even if there is no external image imported)
* 23/11	- solved issue with drawing onto external images: now a empty 'external_image' canvas is created first / per default...
	  image operations are now performed on a "real canvas" and no longer on a "canvas_div"
* 22/11
	- joined draw_external_images and drag_external_images
	- changed output "function distance()" from integer to floating point. (slightly better in case of snaptopoints)
* 9/11
	- fixed bug of increasing font_size when using multiple 'text' commands [perrin]
	- added "xsnaptogrid,ysnaptogrid,snaptogrid,snaptopoints" to dragging of 'external images'
	- "click_cnt" counter (to identify the object in reply) only incremented when 'onclick' and / or "drag x|y|xy" is set.
	- added "snaptopoints" to userdraw primitives and drag&onclick library
* 6/11	- corrected dragging (dragging was missing...) of closed polygon (triangle etc)
```
userdraw arrow,blue
#userdraw arrows2,blue
arrowhead 10
linewidth 2
snaptopoints 0,0,-6,0,-3,3,0,6,3,3,6,0,3,-3,0,-6,-3,-3
points red,-6,0,-3,3,0,6,3,3,6,0,3,-3,0,-6,-3,-3
point 0,0,blue
linewidth 3
yrange -10,10
xrange -10,10
size 400,400
```
	 only implemented in command "userdraw arrow(s),color"
* 5/11
   - added experimental command "snaptopoints x1,y1,x2,y2,..."
   - added command "bezier color,xs,ys,x1,y1,x2,y2,x3,y3"
   - added alernative to command "polyline" : "path color,x1,y1,x2,y2,...."
   - added command "roundrects color,radius,x1,y1,x2,y2,...."
   - added command "vlines color,x1,y1,x2,y2,...."
   - added command "hlines color,x1,y1,x2,y2,...."
   - added command "demilines color,x1,y1,x2,y2,...." alternative "halflines"
* 3/11   - added commands "rects color,x1,y1,x2,y2,...." and "frects color,x1,y1,x2,y2,...."
* 2/11   - docs
* 1/11
   - corrected font behaviour of Flydraw text command.
        Flydraw command "lines" is equivalent to "polyline"
        "polyline" | "polylines" | "brokenline" | "brokenlines" will produce interconnected segments between the points (not closed)
        "lines" will produce not connected lines through points (x1:y1)--(x2:y) etc
   - syntax changed for command "lines color,x1,y1,x2,y2...x_n-1,y_n-1,x_n,y_n"
   - corrected command "clearbutton some_text" : multiple interactive scripts in web page may have individual 'clear drawing buttons'
   - corrected crosshair_size
	can be set draggable / onclick individually

* 29/10
	- added command "arrows2 color,head px,x_1a,y_1a,x_1b,y_1b,x_2a,y_2a,x_2b,y_2b....x_na,y_na,x_nb,y_nb
	- added 'replyformat 28' to reinput date from 'userdraw circles,color' [perrin]
* 17/10
	- removed empty string error warning in 'char *get_string_argument(); [perrin]
	- removed macro "iscalculation" and replace it by something better...
	- added command "arrows color,x1,y1,x2,y2...x_n,y_n
        note: radius is not in pixels but in xy-coordinate system ! (so dragging / onclick / scaling works )
* 11/10
	- added command "circles color,x1,y1,r1,x2,y2,r2...x_n,y_n,r_n
	like: "0.2345^1.234x" will receive a warning : it should be "(0.2345)^(1.234x)"
	(but they have a small footprint...)
	- added warnings when plot function input has "syntax" issues...well it's more likely that my simple math-parsers are too simple
	- added command segments color,x1,y1,x2,y2....x_n,y_n  	[perrin]
	- added simple javascript "rawmath" to convert user function input (like : 4x --> 4*x)
	 default value 'f(x)='
	- added command 'functionlabel some_string' to label the specific inputfields for function input

* 9/10
	- now multiple functions may be plotted by user via 'userinput function' and read via read_canvas()

```
	  function read_dragdrop(){
	  var script_len = canvas_scripts.length;var dragdrop_reply = "";var found_result = false;for(var p = 0 ; p < script_len ; p++){var fun = eval("read_dragdrop"+canvas_scripts[p]);if(typeof fun === 'function'){var result = fun();if( result && result.length != 0 ){if(script_len == 1 ){ return result;};found_result = true;dragdrop_reply = dragdrop_reply + result +"\n";};};};if( found_result ){return dragdrop_reply;}else{return null;};};
	  <!-- will return all 'drag&drop' or 'onclick' data from all canvas javascript includes -->
	  };
	   };
	    return null;
	   {
	   else
	   }
	    return draw_reply;
	   if( found_result ){
	   };
	    };
	     };
	      draw_reply = draw_reply + result + "\n";
	      found_result = true;
	      if(script_len == 1 ){ return result;};
	     if( result  && result.length != 0){
	     var result = fun();
	    if( typeof fun === 'function'){
	    var fun = eval("read_canvas"+canvas_scripts[p]);
	   for(var p = 0 ; p < script_len ; p++){
	   var found_result = false;
	   var draw_reply = "";
	   var script_len = canvas_scripts.length;
	  function read_canvas(){
	  <!-- will return all 'userdraw' or 'userinput' data from all canvas javascript includes -->
```
	 A special version of 'read_canvas()' and 'read_dragdrop()' should be included in the web page, something like this:

	 In the C-program, the local functions will be called `read_canvas%d()` with '%d' is the random `canvas_root_id`
	 array `canvas_scripts` will keep track of the `canvas_root_id` number of scripts included in the page.
	 The native `read_canvas()` and `read_dragdrop()` are now linked to the `canvas_root_id` and a global

* 1/10	-multiple interactive canvasdraw scripts in a single page may now work correctly in replying the userdrawarrays and the drag&drop data..
* 25/9	-added command 'demiline x1,y1,x2,y2,color` and commands 'userdraw demiline,color' 'userdraw demilines,color'
* 22/9	-corrected missing js-variable declaration in 'userdraw path(s) ' [use strict]
* 20/9	-added userinput function | input | textarea as alternative command + argment to keywords "userinput_function","userinput_textarea" and "userinput_xy"
* 11/9	-tried to improve 'filltoborder x,y,bordercolor,fillcolor'
* 10/9	-added keyword 'userinput_function'
* 9/9	-corrected placement of inputfields 'userdraw inputs,color' (no longer can input fields be placed on top of each other
* 30/8
	- added command 'clearbutton button_text' to remove all user drawings from userdraw_canvas (and empty all userdraw data arrays)
	(although I'm not happy to increase the size of the general part of the include file)
	...sometimes functions expect these array's to be defined [use strict]
	- added userdraw_x = [],yserdraw_y = [],userdraw_radius = [] to global section of javascript include
* 29/8
	- corrected 'display radius,color,fontsize' in case there is no 'userdraw circle,color' defined
	- corrected 'use strict' problems with 'clock'
	display x | y | xy | degree | radius ,color,fontsize

* 28/8
	- added argument 'radius' to mousedisplay (in case of 'userdraw circle,color'
	x_input_1:y_input_:1input_value_1,x_input_2:y_input_:2input_value_2...
	- added 'replyformat 27' : read_canvas() will return array
* 27/8	- corrected flaw [sigalled by 'use strict'] in 'userdraw text,color'
* 26/8	- added command 'userdraw input,color' and 'userdraw inputs,color' [work in progress]
	 type: x display | y display | xy display | angle degree | angle radian
	 display slider values, use:
	 type: x | y | xy | angle
	 slider min,max,width,height,type,string

* 24/8	- reverted to originale command 'display' and modified slider arguments to support a per slider display of it's value in the main canvas window
	 work in progress
* 23/8	- added 'slider | slider_degree | slider_radian' to command "display x|y|xy|degree|slider|slider_degree|slider_radian,color,fontsize"

```
zoom blue
userdraw segment,orange
string black,0.9,0.1,P
fontfamily italic 24px Courier
point 1,0,orange
linewidth 4
angle 0,0,0.3,0,0,green
fillcolor yellow
filled
segment 0,0,1,0,orange
linewidth 2
slider 0,2*pi,200,40,angle degree,use slider to rotate point P
circle 0,0,200,red
fillcolor white
grid 1,1,grey,4,4,10,blue
xaxis 0:0:1:1:2:2:3:3:4:4:5:5:6:6:0.5*pi:\u03c0/2:pi:\u03c0:1.5*pi:3\u03c0/2:2*pi:2\u03c0:3*pi:3\u03c0:4*pi:4\u03c0
fontfamily bold 14px Arial
opacity 200,150
axis
yrange -1,1
xrange -1,7
size 800,200
```
    for example:
    	if the grid does not match the xaxis values:strings there will be extra tics
	- added x/y-tics in command 'grid xmajor,ymajor,color,xminor,yminor,tics-size,axiscolor'  case of command 'xaxis' or 'yaxis'

* 21/8
	- corrected rotation direction in case of 'slider type angle' (anti-clockwise)
	- removed 'animate' from dragstuff library : this should be done with a kind of slider...TO DO !!
	  hope this works for all browsers...tested on FreeBSD Opera/Firefox/Seamonkey/Chrome

* 20/8
	- added 'use strict'; to js-include [just to make sure the js-code is "properly written"
	- slider using 'xy' will start & end by clicking the 'slider rect'
	  'angle' will zoom in/out (using scale_x_radius();) and will use radians for slider values (in contrast to 'arc' which uses width/height in pixels)
	- added command 'angle x,y,width,start,end,color' as alternative to flydraw compatible command 'arc'

* 19/8	- improved response speed of slider (in dragstuff library)
	  The amount of sliders in not limited
	  now a slider may adjust several different objects...until keyword 'killslider' or next 'slider'

* 18/8
	- added keyword 'killslider'
	- for all other object the slider will 'move' / 'rotate' the complete object
	- if slider is used for command 'arc' the angle will be set with the slider
	- javascript:read_dragdrop(); will return an array  'object number:slider value'
* 17/8	- type: xy,x,y,angle : added slider 'rect' for xy movement
* 15/8	- further integration of slider: all draggable objects may have sliders (x/y/angle)
* 14/8
	- adding slider to dragstuff library : work in "progress"
	- adapted 'rotate angle' to be compatible with flydraw... :  -1*angle (Marina)
	- correct imagefill etc : reset() was missing (Marina)
* 5/8	- added replyformat 25/26 : 'userdraw arc(s),color' with degrees or radians
	  STILL NOT 100%  CORRECT !!
* 4/8
	- corrected command 'userdraw arc,color'
	- added precision to the dragdrop library (eg. read_dragdrop() )
* 28/7
	- added precision to the read_canvas() functions.
	- corrected command 'ellipse' : switched from Beziercurve to arc-method. Should now be a true ellipse
	- corrected command 'roundrect' : no inverted roundrect possible
* 27/7	- corrected command 'square' ...
* 26/7
	- corrected a few flaws introduced by internal changing from 'translation' to 'affine'
	- corrected command 'lattice' (hope it's ok now)
	- corrected missing dashing of 'arrow' 'arrow2'
	 to imitate flydraw's filling is too much work ... for both browser js-engine and me ...
remark:  hatchfill/dotfill/diamondfill are not the same as flydraw...I've  not understood the flydraw filling correctly...
	- corrected 'filltoborder' and 'floodfill' ... it should / could work now
	- corrected command 'square x,y,side px,color'
	  Changed several functions: new flaws in javascript may be introduced.
	  the translation_ty being affine_matrix[5]
	  the translation_tx being affine_matrix[4]
	- removed internal variable used by 'translate' and moved to generic 'affine matrix'
	- corrected affine : tx/ty now in x/y-range
* 25/7	Thanks to Marina:

* 29/6
	- added mousex / mousey commands : just show x or y values of mouse coordinates
	- changed command 'barchart' : now "unlimited" barchart commands will be plotted in a single graph.
	- reverted some correction mechanism for grid / axisnumbering when zooming out...need to rething
          'userdraw vlines,color'
          'userdraw vline,color'
          'userdraw hlines,color'
          'userdraw hline,color'
	- added
          Now the sequence is not important. However  both commands should be called at the end of the script
          ('mouse' was only active when 'zoom' function was called before 'mouse')

* 28/6
	- corrected preference behaviour of commands 'zoom' and 'mouse'
	- removed some '\n' from js include code

* 27/6
	- "replyformat 15" will only report inputfields/textareas set 'write' (eg not set 'readonly')
	- added "replyformat 24" read all inputfields/textareas even those set 'readonly'
* 23/6	- added command 'centerstring color,y,text' for placing a x-centered text in the canvas
* 21/6
	- corrected command jsplot : now more complex(...) functions may be used.
	- increased value MAX_INT from 64 -> 128 : this is used for several arrays in C
	- added some info on 'sgraph' (...)
* 18/6
	- solved small issue with removal of userdraw circle
	- solved issue with precision (multiple roundings  x <--> px) in read_canvas() and js-functions x2px(x) / px2x(px):last precision in script is used for reply-precision
	- fix stupid error in 'replyformat 23'
	- zoom in y-direction was gone fishing...
	- if wims_status='done' all 'drawing' related input fields are removed  (eg. userinput_xy etc)
	- added flag to avoid multiple including of safe_eval, usd by several optional things
	  added some alert message when inputstring has unfinished calculations , 'like 123 + 10^5/678'
	- adapted 'safe_eval' to accept numbers like 2.345*10^5 (previous version only accepted 10^5...)
          (barchart and linegraph are closely linked to grid: there can be multiple linegraphs in a single grid...but there can only be 1 grid)

* 17/6
	- modified command 'piechart' and 'legend' : now multiple piecharts with legends may be present in a single canvas image
 	- mouseup() in draglibrary corrected
* 15/6	- added timeout for floodfill and filltoborder: the canvas was not yet 'drawn' in current setup , when floodfill was called...
* 10/6	- added '(xy)snaptogrid' to drag library
	  overide this default behaviour with keyword 'status' or 'nostatus'
* 8/6
	- inputfields/textarea's in canvasimage will be cleared and set 'readonly' when wims status variable is 'done'
	  (in case you want to draw something...but not send the drawing to wims..)
	- added 'replyformat -1' to disable the javascript read_canvas() function
	  added commands 'userdraw arrow2' and 'userdraw arrows2'
* 7/6
	- corrected more inbalance in save() / restore() in 'draw_arrow()'
	- corrected inbalance in save() / restore() causing annoying problems in command 'arc'
	  todo CHECK THE REST !!
	- added forgotten rotate&angle arguments to commands 'string / text'
	- changed command 'grid' : now zooming and axisnumbering is slightly better...but it stays amateur scribling, grrrr...
	- corrected typo "};else{" in javascript  replyformat 7
* 6/6
	- removed randomness from command clock (was annoying)
	    added a lightweight javascipt function to convert math into js-math (only suitable for basic stuff)
	    for plotting curves using clientside calculations: formula is verbatim in page-source
	- added command jscurve color,formula_in_x
* 5/6	- work in progress:
* 2/6    - BPR: replace the dependance with libmatheval to the wims evaluation.
	  allowing all combinations of 'setlimits', 'trace_jsmath' ,'jsmath' , 'userinput_xy+userdraw' , mouse , drag etc
* 1/6
	- added keyword 'setlimits'
	- work in progress: added keyword 'setlimits'
* 31/5	- slightly modified "trace_jsmath some_js_math_function_in_x" and "jsmath some_js_math_function_in_x"
* 30/5	- added commands `trace_jsmath some_js_math_function_in_x` and `jsmath some_js_math_function_in_x`
* 27/5	- javascript variable 'precision' was gone fishing
	  level curve may be set 'onclick' or 'draggable'; every single curve has an unique identifier
* 23/5	- command 'levelcurve' : the quality of the curves is not to be compared with the 'Flydraw levelcurve'
          I'm not sure if this should be implemented in javascript...
* 22/5	- work in progress: added command 'levelcurve'
	  removed function get_double() from canvasdraw.
	  corrected command 'rays color,xc,yc,x1,y1....xn,yn' (it was using pixels instead of x/y-range coordinates)
* 21/5	- added forgotten commande 'dvline' and 'dhline'
	  note: mouse coordinates (for drag / onclick) are not transformed....TODO)
* 20/5	- added command 'affine a,b,c,d,tx,ty' and keyword 'killaffine'  (Marina Cazzola)
	  'snaptogrid'  " will snap to both xmajor and ymajor (or xminor and yminor, if defined)
	  'ysnaptogrid' : will snap to ymajor (or yminor, if defined)
	  'xsnaptogrid' : will snap to xmajor (or xminor, if defined)
* 19/5	- added keywords
	- removed command 'debug'
	    x1,y1,x2,y2,x3,y3,.....x(n-1),y(n-1),xn,yn
	    x1,y1,x2,y2,x2,y2,x3,y3,x3,y3.....,x(n-1),y(n-1),x(n-1),y(n-1),xn,yn --replyformat 23-->
* 18/5	- add replyformat 23 : used for 'userdraw polyline' : filtering out the second click on the same (+/- 5 px) point
	  corrected mixup between userdraw polyline / polygon...
	  if the mouse coordinate display or the student reply -in case of 'userdraw'-  needs a higher precision, just use as last command something like 'precision 10000'

* 16/5	- command 'precision' before ylogscale / xlogscale will set the number of decimals of the x-axis / y -axis (a local variable precision is set)
	  corrected y-axis numbering in case of 'xlogscale'
* 11/5
	- added js function safe_eval() to command 'userinput_xy' to "safely" evaluate math inputs like 10^2 or sqrt(5)
	- corrected x-axis numbering in case of 'ylogscale'
	- corrected problems with ';' in html-text / text-arguments (the ';' is a command separator ... added 22/4)
	- forgotten to clearRect()
	  (just more copy and paste "programming")
* 10/5	- added 'userdraw polygon,color'  'userdraw line,color' and 'userdraw lines,color' to userinput_xy
* 5/5	- added commands 'ylogscale' , 'xlogscale' , 'xylogscale' , 'xlogbase' and 'ylogbase'
* 25/4	- add 'nok' button to remove last item from userdraw arrays
	  add keyword usertextarea_xy (...)
* 24/4	- add userdraw polyline,color
* 23/4	- corrected sgraph
* 22/4	- added semicolon ; as 'new line separator' (Marina Cazzola)
* 21/4	- corrected command grid in case xmin > 0  and / or ymin > 0
	  for use as very (!) primitive 'broken axis graph'
* 20/4	- work in progress: add command `sgraph xstart,ystart,xmajor,ymajor,xminor,yminor,majorgrid_color,minorgrid_color`

```
	  'userdraw arrows,color'
	  'userdraw arrow,color'
	  'userdraw crosshairs,color'
	  'userdraw crosshair,color'
	  'userdraw circles,color'
	  'userdraw circle,color'
	  'userdraw points,color'
	  'userdraw point,color'
	  'userdraw segments,color'
	  'userdraw segment,color'
	  for now: only implemented in
```

* 18/4	- added keyword 'userinput_xy' : if set 2 or 3 inputfields will be shown...the pupil will be able to 'draw' with mouse and/or by giving  a x-value and y-value (or radius-value)
* 14/4   - added 'snaptogrid' to gridfill and dotfill (e.g. userdraw will snap to the grid)
* 11/4	- corrected flaw in snaptogrid : now works correctly for dividers xminor  and yminor (only if they are even dividers e.g. 2,4,6,8 etc)
* 8/4	- added to wims_svn

* 5/4	- corrected flaw in 'normal' userdraw (without snaptogrid)
	  todo: other gridfill commands may use snaptogrid
	  grid 1,1,black,2,2,6,grey  : will snap to all minor grid lines n/2
	  grid 1,1,black : it will snap to all major grid lines 'n'
* 2/4	- added command / keyword 'snaptogrid' : a userdrawing will snap to the given grid...
	  added keyword 'status' to disable this default behaviour (e.g. pupils may draw in canvas when wims $status=done
* 25/3	- userdraw functions only active if wims $status != done
	  canvasdraw now only works in correct wims_environment : e.g. ./canvadraw < fly.txt  will not work anymore
	  No option anymore to gzip the include.
	  The rest is printed in a fairly small buffer and printed to stdout at the end of parsing the fly-script.
* 23/3	- simplified buffer usage : now js-include file in wims-getfile-directoy is standard.

	Probably have to look into str_replace() !
	segfault occured in case "blah" was not present in "temp"  (and no replacement was made) and string length wos big
	On 32/64 bits FreeBSD ; 32/64 bits OpenBSD no problems

	```
	if( strstr(temp,"blah") != 0 ){ temp = str_replace( temp ,"blah","beh");}
    char *temp = getstring(infile,1);
    Changed into:
    char *temp = str_replace( getstring(infile,1),"blah","beh");
	```
* 21/3	- corrected spurious segfault on Gentoo 64 bits (leiden) and OpenSuSE-32 :
* 14/3	- forgotten 'break;'
* 3/3	- synchronized commands "arrow" and "arrow2" with flydraw syntax; e.g. arrow x1,y1,x2,y2,head_in_px,color
	  It works if not too much cropping is required.
	  The commands are still not equal to Flydraw: I'm giving up on this !!
* 28/2
	- tried to correct copy/ropyresized ; using image as background image of div (e.g. no canvas !)
	  We just use the URL of the image...and add some CSS to it (no-repreat / Size / Position) and det the URL to the backgroundImage of the main div
	- an external image copied into a canvas / resized etc... can be set as background of the document.body...
	- an external image copied into a canvas / resized etc... can not be set as CSS backgroundImage of a div... (at least in firefox)
	- there is no way to paint on top of an external image loaded into a canvas...

* 27/2	- solved nasty issue with "drawing on top of external image"  with copy / copyresized
* 19/2	- added command 'animate type' as primitive animation ; only 'point' on curve is implemented
	  Using generic js-functions in wims-pages to read all mathml-inputs (including the ones in a canvas image !)
* 12/2	- removed reading of mathml-inputfield in canvas (only "replyformat 16" will read it)

	  canvas_div.style.background-size --> canvas_div.style.backgroundSize
* 8/2
	- corrected syntax in bgimage:
	- command "bgimage url" will scale to fit background image
* 7/2
	- corrected x-y mixup in case xrange/yrange was forgotten (eg xrange = 0 px ,sizex px)
	- added forgotten filling of an arc ;command 'filledarc'
	  (something like:fontfamily bold italic 24px Courier)
	  the rest of the gridfonts (numbering or x/yaxis text) is determined by the 'fontfamily'
* 6/2
	- added default fontfamily settings to 'legend', 'xlabel' and 'ylabel' ; size may be set by command 'fontsize'
	- add command 'dsegment' as alias for dashed segment
	  eg only active inputfields will be read with read_canvas();
* 5/2	- if inputfield has attribute 'readonly', do not read the value when using javascript:read_canvas();
* 3/2
	- added forgotten 'readonly' attribute to js-function 'draw_inputs()'
	- solved(?) some strange fill issues (pixel manipulation) added demo

          If legendcolors was not set , the default stroke_color will be used in a 'legend'
          for all other legends, we can specify.
          barchart & piechart will use -ofcourse- the same colours for legend
* 1/2
	- add command 'legendcolors color1:color2:color3:...:color_n'
	- command 'legend string1:string2:string3:..:string_n' may now be used with any 'grid' (e.g. not only for barchart/linegraph/piechart)
	- commands 'xlabel' 'ylabel' only depend on command 'grid' (eg without 'axis' and/or 'axisnumbering')
	- added commands 'xlabel some_string' ylabel some_string'
	- corrected tooltip when using images as 'linktext'

	will output reply[0]=x1 reply[1]=y1 reply[2]=x2 reply[3]=y2 ... reply[n-1]=x_n reply[n]=y_n

* 31/1
	- read_canvas() : Added 'replyformat 22' : single array output (just for use in my existing checkfile)
	will output words (x1:y1) (x2:y2) (x3:y3) ... (x_n:y_n)
	- read_canvas() : Added 'replyformat 21' : verbatim coordinate output (just for use in my existing checkfile)
	Command "intooltip tooltip_text" will accept html-text, images and now also Mathml as tooltip_text
	- Now "mouse red,24" will default to font '24px Arial'
	fontfamily [type] [size]px [family]
	was set, the argument fontsize of command "mouse color,fontsize" was ignored...due to the strict syntax in html/js

* 29/1  - Corrected js-fontfamily kludge...when something like "fontfamily italic 14px Arial"
	(On multiple panning/zooming the mouse coordinate display will get 'sluggish' and not very quick in response)
	When 'x' is pressed the page will reload (location.reload();) so the panned/zoomed image or drawing will be reset to it's original state.

* 23/1	Added a 'x' to pan/zoom-buttons (command "zoom color")
	`xaxis -4*pi:-4\u03c0:-3*pi:-3\u03c0:-2*pi:-2\u03c0:-1*pi:-\u03c0:pi/2:\u03c0/2:pi:\u03c0`
	example (unicode allowed; no html)

		yaxis y_value:y_text:y_value:y_text:y_value:y_text....
		xaxis x_value:x_text:x_value:x_text:x_value:x_text....

* 22/1 Corrected commands with "pi"... javascript did not calculate the x-value...

## 2014
(was using keyword 'input' but changed wims_mathml to use a styled textarea as input...)

* 28/12 in case of command 'mathml' , look for 'mathml0' to trigger javascript:read_mathml() if no other ineractiveness was previously defined
* 29/11 copy & copyresized of external images : preparing for drag&drop (outside the drag&drop shape library)
	correction of zoom_buttons (wrong panning in wrong direction...)
* 27/11 improved plotting of asymptotic curves
* 16/11 setting & reading clock (preliminiary: for now just html-buttons for adjusting H,M,S...should be active fields in canvas...)
	custom  colours: clock 0,0,60,4,35,45,1,2,white,white,white,black,yellow
	custom  colours: clock 0,0,60,4,35,45,1,2,,,,yellow,red
	default colours: clock 0,0,60,4,35,45,1,2,[space]
	clock x,y,r(px),H,M,S,type hourglass,interactive [ ,H_color,M_color,S_color,background_color,foreground_color ]
* 15/11 changed basic syntax of command clock : all colours are now optional
	Temporarily removed check on 'valid wims-session' : it is not working for a 'chroot' environment (TODO correct this !!)
	(only when using canvasdraw in a 'chroot' environment , giving : Undefined symbol "_ThreadRuneLocale)
	I can not use the normal implementations found in 'ctypes.h' for this purpose, due to a bug in FreeBSD's C-library
* 14/11 implemented own versions of "tolower()" and "toupper()"...
	clock x,y,r(px),H,M,S,type hourglass,interactive,H_color,M_color,S_color,background_color,foreground_color
* 13/11 Added interactive=2 (multiple choice  / onclick) to select a single clock
	(work in progress)
	Add basic interactivness
	Add argument 'hourglass type' to controle appearance of analogue clock
* 12/11 Multiple clocks may be set interactive ; read_canvas() will read all active clocks in H:M:S
	Preparing for command clock (work in progress)
	(was x/y-range -10,10)
	ymin = 0 ymax = ysize
	xmin = 0 xmax = xsize
* 11/11 If xrange / yrange is not given :
	Todo: check compatibility...I'm not sure it works the same as in 'flydraw'
	"copyresized x1,y2,x2,y2,dx1,dy1,dx2,dy2,filename_URL"	and "copy x,y,x1,y1,x2,y2,filename_URL"
* 08/11 Added commands
	Just use a fairly recent Firefox / Opera / Chrome
	E.g all non supported browsers will be excluded...no intention to offer 'canvas glue' library for IE < 9
	try(document.createElement('canvas');)catch(e){alert("no support");return;}
* 03/11	Added check on canvas-support in function 'create_canvas()';
* 01/11 Added 'userdraw text,color' to reply types
	a confirm will popup asking to delete the selected text.This text will be removed from the "userdraw_txt()" answer array.
	Placing the cursor somewhere on a typed text and hitting "delete/backspace/esc",
	You will need to hit "enter" to add the text to the array "userdraw_txt()" : lightblue background will disappear
	"backspace / delete / esc" will remove typed text.
	While typing the background of every typed char will be lightblue...
	Any string of multiple strings may be placed anywhere on the canvas.
* 30/10 Finished command 'userdraw text,color'
	(it is ofcource possible, but at a very very high price)
	because the mouse coordinates do not follow the transformation matrix if in the proximity of a text-object.
	vertical or rotated text can not be set drag / onclick,
* 29/10 add horizontal text to drag / onclick
* 28/10 preparing for adding all 'text' commands to drag / click library
	Would be nice to have some feedback (...)
	need to find a way to bypass browser differences...using 'String.fromCharCode(evt.keyCode);' is not really a safe method ?
* 27/10 add command "userdraw text,color" work in progress...just a single char, for now (no answer_type,yet)
	(for example to be used for a barchart created by students)
	add commands "userdraw rects,color" "userdraw roundrects,color" for drawing multiple rectangles
	Many different linegraphs may be used in a single plot.
	to be used together with commands 'grid'. May be combined with freestyle x-axis / y0-axis text (commands 'xaxis' 'yaxis')
* 26/10 add command "linegraph x1:y1:x2:y2...x_n:y_n" : may be further controlled by commands 'strokecolor','linewidth','dashed','dashtype'
	to be used together with commands 'grid'. May be combined with freestyle x-axis / y0-axis text (commands 'xaxis' 'yaxis')
	add command "barchart x1:y1:color1:x2:y2:color2...x_n:y_n:color_n" to automate generation of barchart
* 25/10 add command "legend string1:string2:...:string_n" to add an automatically sized legend to a graph
* 24/10 add command "piechart xc,yc,radius,'data+colorlist'"

	add command "fontcolor color" (previously fontcolor=strokecolor) now x/y axis-texts color may be different from axis / xy-tics color
* 22/10 grid: x/y major tics and x/y minor tics now different
	update 'drag curve fill mathml-input' example (selective removal of filled area)
	freestyle x/y-axistext move with panning (e.g stay in visual image)
	to automate filling axis with text insteadof default numbering xmin...xmax
	yaxis num1:string1:num2:string2:num3:string3:num4:string4:....num_n:string-n
	xaxis num1:string1:num2:string2:num3:string3:num4:string4:....num_n:string-n
* 21/10 added commands:
	tried to improve 'axisnumbering' in command 'grid' ( avoiding number overlap )
* 20/10 added testresults flydraw versus canvasdraw.
	added "use_rotate" argument to shape library
* 19/10 ellipse x,y,rx,ry,color is draggable/clickable and will scale/resize on zooming (like all other draggable 'massive' objects)
	multiple instances of tooltip are allowed (as ofcourse is true for multiple canvas)
* 18/10 corrected tooltip to work / integrate in 'real' wims modules
* 15/10 circle x,y,radius(px),color will scale/resize on zooming (like all other draggable 'massive' objects)
* 13/10 corrected hexcolor input
	reading of useractions in canvas is only possible for a single instance (for now)
	(experimental work in progress)
	by using unique names for all potential conflicting functions
* 12/10 added support for multi-canvasses in a single html-page
* 10/10 added missing "userdraw polygone,color"
* 9/10  added parallel to shape_library
	default distance is 20 pix from original 'fill click'
	size of 'measuring rectangle' may be tuned with "clickfillmarge"
* 8/10  added specific removal of a 'click_filled' area.
* 7/10  added arc to shape_library
* 6/10  corrected syntax "fill" == "floodfill" = pixel filling to a border
* 5/10  added missing "userdraw rect,color" "userdraw roundrect,color" "userdraw triangle,color"
* 4/10  added missing "userdraw line,color" "userdraw lines,color"

or search keywords 'html5' 'canvas' om leiden mirror
For live testing: use "directexec" module on Leiden mirror.
For demo: see demo-directory.
For implemented commands: see canvasdraw.html
   (first need to solve  re-draw issues; managing multiple canvasses in drag/click library)
	- Isolate static javascript code  components/functions from C-"programs"
	- Some bitmap operations (like copyresize etc)
	- OEF interface
Unadressed issues at this point :

Opera (>9)
Gecko browsers (Firefox > 17)
Microsoft IE10
Tested on :

(on Leiden mirror and intranet servers only)
It will be used and tested to replace "flydraw" , "GeoGebra" and "flyapplet" in Schaersvoorde modules.
Preliminary version. All javascript code is generated by 'C-program'

* 4/10/2013 First version
