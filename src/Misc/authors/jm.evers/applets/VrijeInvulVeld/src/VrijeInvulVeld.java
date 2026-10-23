/*
*********************************************************************************
* J.M. Evers 19/11/2009								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatoever								*
*********************************************************************************

<html>
    <head>test vrije invulveld</head>
    <body>
	<script language="javascript" type="text/javascript">                                                                                      
	    function READTHIS(){                                                                                                                           
		var reply=document.applets[0].ReadApplet(1);
	        alert("the applet will send to wims:\n"+reply);
	    }                                                                                                                                         
	</script>                                                                                                                                  
     <center>
	<applet code="VrijeInvulVeld.class" archive="VrijeInvulVeld.jar" width="640" height="480">
	    <param name="xsize" value="300">
	    <param name="ysize" value="300">
	    <param name="bgcolor" value="255,255,255">
	    <param name="textcolor" value="250,0,0">
	    <param name="drawcolor" value="0,250,10">
	    <param name="penthickness" value="2">
	    <param name="penfontsize" value="18">
	    <param name="penfontfamily" value="Helvetica">
	    <param name="penfontstyle" value="bold">
	    <param name="language" value="nl">
	    <param name="text" value="yes"> <!-- if 0/no no text -->
	    <param name="line" value="yes"> <!-- if 0/no no lines -->
	    <param name="rect" value="0"> <!-- if 0/no no rec -->
	    <param name="ellipse" value="0"> <!-- if 0/no no ellipse -->
	    <param name="arrow" value="0"> <!-- if 0/no no arrows -->
	    <param name="grid" value="yes"> <!-- will produce a grid -->
	    <param name="textlines" value="10"> <!-- number of lines in the applet: strings are bound to these lines leave blank or remove parma-tag to revert to original behaviour of the drawing applet -->
	    <param name="textalign" value="vertical"> <!-- probability tree orientation --> 
	</applet>
	<input type="button" name=".....TEST......" value=".....TEST....." onclick="javascript:READTHIS();">
      </center>
    </body>
</html>
*/

import java.util.*;
import java.awt.*;
import java.applet.*;



/* The applet itself; contains a display, a control panel and a diagram */

public class VrijeInvulVeld extends Applet {
    Diagram diagram;
    DiagramControls controls;
    DiagramDisplay display;
    MessageArea message;
    EventConsumer current_event_consumer;

// jm.evers a few defaults...i'm no good at this java :(
    static int xsize=640;
    static int ysize=480;
    static int ygrid=50;// default disabled : less than 50 "lines" of text enables the "snap to line"
    static Color bgcolor=new Color(250,250,250);
    static Color textcolor=new Color(255,0,0);
    static Color drawcolor=new Color(150,0,250);
    static float thickness=2.0f;
    static int penfontsize=16;
    static String fontfamily="Helvetica";
    static Font penfont= new Font(fontfamily, Font.BOLD, penfontsize);                                                                         
    static Font messagefont= new Font(fontfamily, Font.BOLD,16);                                                                         
    public String t_line="Line";
    public String t_arrow="Arrow";
    public String t_rectangle="Rectangle";
    public String t_ellipse="Ellipse";
    public String t_text="Text";
    public String t_circle="Circle";
    public String t_destroy="Delete";
    public String t_move="Move";
    static String t_press="Press return to finish the text";
    static boolean textalign=false; // horizontal [snap to y: sort to y] . otherwise vertical [snap to x:sort to x]
    static int textcounter=0;
    static int linecounter=0;
    static boolean original=false;
    static boolean draw_line=true;
    static boolean draw_text=true;
    static boolean draw_arrow=true;
    static boolean draw_ellipse=true;
    static boolean draw_rect=true;

//
    public void init() {
	String param;
	param = getParameter("language");
	if(param != null) {
	    if(param.equalsIgnoreCase("nl")){
		t_line="lijn";
		t_arrow="pijl";
		t_rectangle="rechthoek";
		t_ellipse="ellips";
		t_text="tekst";
		t_circle="cirkel";
		t_destroy="wissen";
		t_move="verplaatsen";
		t_press="Druk op enter als de tekst klaar is";
	    }
	    if(param.equalsIgnoreCase("de")){
		t_line="Linie";
		t_arrow="Pfeil";
		t_rectangle="Rechteck";
		t_ellipse="Ellipse";
		t_text="Text";
		t_circle="Kreis";
		t_destroy="löschen";
		t_move="verlagern";
		t_press="Drücken Sie auf \"Eingabe\" am Ende des Textes";
	    }
	    if(param.equalsIgnoreCase("fr")){
		t_line="droite";
		t_arrow="flèche";
		t_rectangle="rectangle";
		t_ellipse="ellips";
		t_text="texte";
		t_circle="cercle ";
		t_destroy="effacer";
		t_move="déplacer";
		t_press="Press return to finish the text";
	    }
	}
	param = getParameter("line");
	if(param != null){if(param.equalsIgnoreCase("no") || param.equals("0")){draw_line=false;}}
	param = getParameter("rect");
	if(param != null){if(param.equalsIgnoreCase("no") || param.equals("0")){draw_rect=false;}}
	param = getParameter("arrow");
	if(param != null){if(param.equalsIgnoreCase("no") || param.equals("0")){draw_arrow=false;}}
	param = getParameter("text");
	if(param != null){if(param.equalsIgnoreCase("no") || param.equals("0")){draw_text=false;}}
	param = getParameter("ellipse");
	if(param != null){if(param.equalsIgnoreCase("no") || param.equals("0")){draw_ellipse=false;}}
	param = getParameter("penfontsize");
	if(param != null) {penfontsize=Integer.parseInt(param,10); penfont= new Font("Helvetica", Font.BOLD, penfontsize);}
	param = getParameter("penfontfamily");
	if(param != null) {penfont= new Font(param, Font.BOLD, penfontsize);}
	param = getParameter("penfontstyle");
	if(param != null) {
	    if(param.equalsIgnoreCase("bold")){penfont= new Font(param, Font.BOLD , penfontsize);}
	    else if(param.equalsIgnoreCase("italic")){penfont= new Font(param, Font.ITALIC , penfontsize);}
	    else if(param.equalsIgnoreCase("plain")){penfont= new Font(param, Font.PLAIN , penfontsize);}
	}
	param = getParameter("penthickness");
	if(param != null) {thickness=Float.parseFloat(param);}
	param = getParameter("xsize");
	if(param != null) {xsize=Integer.parseInt(param,10);}
	param = getParameter("ysize");
	if(param != null) {ysize=Integer.parseInt(param,10);}
	param = getParameter("textlines");
	if(param != null) {ygrid=Integer.parseInt(param,10);
	    if(ygrid>100){ygrid=100;System.out.println("100 lines max !");}
	    if(ygrid<0){ygrid=10;System.out.println("positive number textlines !");}
	}
	param = getParameter("textalign");
	if(param == null || param.length()==0){original=true;}else{original=false;if(param.equalsIgnoreCase("vertical")){textalign=true;}else{textalign=false;}}
	resize(xsize,ysize);
	param=getParameter("bgcolor"); // Background color of inputfield "input"
	if (param != null && param.length()>0){bgcolor=ReturnColor(param);}
	param=getParameter("textcolor"); // Background color of inputfield "input"
	if (param != null && param.length()>0){textcolor=ReturnColor(param);}
	param=getParameter("drawcolor"); // Background color of inputfield "input"
	if (param != null && param.length()>0){drawcolor=ReturnColor(param);}
	
	diagram = new Diagram();
	diagram.editor = this;
	// no idea how to tell this class that the names are passed on as params in init...
	add(controls = new DiagramControls(this ,t_line,t_arrow,t_rectangle,t_ellipse,t_text,t_circle,t_destroy,t_move));
	add(display = new DiagramDisplay(this ,thickness,textcolor,drawcolor,penfont));
	current_event_consumer = controls;
	
    }

// jm.evers

    public Color ReturnColor(String RGB){
	int R=0;int G=0;int B=0;
	RGB=RGB.replace(':',',');RGB=RGB.replace(';',',');
	StringTokenizer q = new StringTokenizer(RGB, ",");
	String k;int rgb;Color kleur;
	for( int a=0; a<3 ; a++){
	    k=q.nextToken();
	    rgb=Integer.parseInt(k, 10);
	    if(rgb<0){rgb=0;}
	    if(rgb>255){rgb=255;}
	    if(a == 0){R = rgb;}
	    else if(a == 1){G = rgb;}
	    else if(a == 2){B = rgb;}
	}
	kleur=new Color(R,G,B);
	return kleur;
    }
    
    public String ReadApplet(String arg){
	String reply="";
	if(arg.equals("1")){
	    String line_analysis="";
	    int[] text_x=new int[textcounter];
	    int[] text_y=new int[textcounter];
	    int[] line_x1=new int[linecounter];
	    int[] line_y1=new int[linecounter];
	    int[] line_x2=new int[linecounter];
	    int[] line_y2=new int[linecounter];
	    String[] tekst=new String[textcounter];
	    int real_text_length=0;String tmp="";
	    int real_line_length=0;int x1;int x2;int y1;int y2;
    	    for(Enumeration e = diagram.elements(); e.hasMoreElements(); ){                                                                            
        	DiagramItem item = (DiagramItem)e.nextElement();
		if(item.specify_line_x1() != -1){
		    x1=item.specify_line_x1();
		    y1=item.specify_line_y1();
		    x2=item.specify_line_x2();
		    y2=item.specify_line_y2();
		    if(textalign){//alleen horizontale lijnen
			if( x1 != x2 && y1 == y2){// avoid line-dots to be send as lines/branches
			    line_x1[real_line_length] = x1;
			    line_y1[real_line_length] = y1;
			    line_x2[real_line_length] = x2;
			    line_y2[real_line_length] = y2;
			    real_line_length++;
			    System.out.println("een horizontale lijn "+real_line_length);
			}
		    }
		    else
		    {
			if( y1 != y2 && x1 == x2 ){// avoid line-dots to be send as lines/branches
			    line_x1[real_line_length] = x1;
			    line_y1[real_line_length] = y1;
			    line_x2[real_line_length] = x2;
			    line_y2[real_line_length] = y2;
			    real_line_length++;
			    System.out.println("een vertikale lijn "+real_line_length);
			}
		    }
		}
		if(item.specify_text() != null){
		    tmp = item.specify_text();
		    // remove the forgotten '_' and we can't use komma's in the reply...replace them by points ?
		    tmp =replace(tmp,"_"," ");
		    tekst[real_text_length] =replace(tmp,",",".");
		    text_x[real_text_length] = item.specify_text_x();
		    text_y[real_text_length] = item.specify_text_y();
		    real_text_length++;
		}
    	    }
	    
	    boolean nog_meer = true;int i=0;int s=0;int temp_x;int temp_y;String temp_txt;
	    String[] sameline = new String[real_text_length];
	    FontMetrics fm =getFontMetrics(penfont);                                                                                       
            int hoogte = fm.getMaxAscent();
	    if(textalign){//horizonal probability tree
		while (nog_meer) {
    		    nog_meer = false;
    		    for ( i=0; i<real_text_length-1; i++ ){
			if ( text_y[i] < text_y[i+1] ){ sameline[i]="\n";}
        		if ( text_y[i] > text_y[i+1] ){
            		    temp_y = text_y[i]; text_y[i] = text_y[i+1];  text_y[i+1] = temp_y;
            		    temp_x = text_x[i];  text_x[i] = text_x[i+1];  text_x[i+1] = temp_x;
            	    	    temp_txt = tekst[i];  tekst[i] = tekst[i+1];  tekst[i+1] = temp_txt;
            		    nog_meer = true;
			    sameline[i]="\n";
        		}
			if ( text_y[i] == text_y[i+1] ){
			    sameline[i]=" ";
			    if( text_x[i] > text_x[i+1] ){
            			temp_y = text_y[i]; text_y[i] = text_y[i+1];  text_y[i+1] = temp_y;
            	    	        temp_x = text_x[i]; text_x[i] = text_x[i+1];  text_x[i+1] = temp_x;
            		        temp_txt = tekst[i]; tekst[i] = tekst[i+1];  tekst[i+1] = temp_txt;
				nog_meer = true;
			    }
			}
    		    }
		}
	    }
	    else
	    {//vertical probability tree
		while (nog_meer) {
    		    nog_meer = false;
    		    for ( i=0; i<real_text_length-1; i++ ){
			if ( text_x[i] < text_x[i+1] ){ sameline[i]="\n";}
        		if ( text_x[i] > text_x[i+1] ){
            		    temp_y = text_y[i]; text_y[i] = text_y[i+1];  text_y[i+1] = temp_y;
            		    temp_x = text_x[i];  text_x[i] = text_x[i+1];  text_x[i+1] = temp_x;
            	    	    temp_txt = tekst[i];  tekst[i] = tekst[i+1];  tekst[i+1] = temp_txt;
            		    nog_meer = true;
			    sameline[i]="\n";
        		}
			if ( text_x[i] == text_x[i+1] ){
			    sameline[i]=" ";
			    if( text_y[i] > text_y[i+1] ){
            			temp_y = text_y[i]; text_y[i] = text_y[i+1];  text_y[i+1] = temp_y;
            	    	        temp_x = text_x[i]; text_x[i] = text_x[i+1];  text_x[i+1] = temp_x;
            		        temp_txt = tekst[i];  tekst[i] = tekst[i+1];  tekst[i+1] = temp_txt;
				nog_meer = true;
			    }
			}
    		    }
		}
	    
	    }
	    int wacht=0;
	    if(textalign){
		for(i=0;i<real_line_length;i++){
		    y1=line_y1[i];
		    for(s=0;s<real_text_length;s++){
			if(text_y[s]==y1 ||( text_y[s]<y1 && text_y[s]>y1-hoogte)){
			    wacht=s;
			}
		    }
		    if(wacht!=0){
			sameline[wacht]="\n line \n";
			wacht=0;
		    }
		}
	    }
	    else
	    {
		for(i=0;i<real_line_length;i++){
		    x1=line_x1[i];
		    x2=line_x2[i];
		    y1=line_y1[i];
		    y2=line_y2[1];
		    if(x1<x2+hoogte && x1>x2-hoogte){
		    // dus een "bijna vertikale lijn
			for(s=0;s<real_text_length;s++){
			    if(text_y[s]==y1 ||( text_y[s]<y1 && text_y[s]>y1-hoogte)){
			    	wacht=s;
			    }
			}
			if(wacht!=0){
			    sameline[wacht]="\n vertikale line \n";
			    wacht=0;
			}
		    }
		}
	    }

	    for(i=0;i<real_text_length;i++){
		if(sameline[i] == null){sameline[i]="\n";}
		reply=reply+tekst[i]+sameline[i];
	    }
	}
	else
	{
	    //alldata...no special sorting of the text
	    for(Enumeration e = diagram.elements(); e.hasMoreElements(); ){                                                                            
        	DiagramItem item = (DiagramItem)e.nextElement();
		reply=reply+item.describe()+"\n";
	    }
        }

	return reply;
    }
    
    // replace compatible with java 1.4  [B. Perrin-Riou]
    public static String replace(String source, String pattern, String replace){
	if (source!=null){
	    final int len = pattern.length();
	    StringBuffer sb = new StringBuffer();
	    int found = -1;
	    int start = 0;
	    while( (found = source.indexOf(pattern, start) ) != -1){
		sb.append(source.substring(start, found));
		sb.append(replace);
		start = found + len;
	    }
	    sb.append(source.substring(start));
	    return sb.toString();
    	}
        else return "";
    }

    public void start() {
    }

    public void stop() {
    }

    public void destroy() {
    }

    public static void main(String argv[]) {
	/* This more-or-less replicates what happens when we  are run as an applet. */
	Frame f = new Frame();
	VrijeInvulVeld d = new VrijeInvulVeld();
	d.init();
	d.start();
	f.add("Center", d);
	f.show();
    }
}
/* end applet */



/* The diagram */

class Diagram extends Vector {
    VrijeInvulVeld editor;
    DiagramItem nearby(int x, int y){
	DiagramItem bestitem = null;
	double bestdist = 6;
	for(Enumeration e = elements(); e.hasMoreElements(); ) {
	    DiagramItem item = (DiagramItem)e.nextElement();
	    double dist = item.distance(x, y);
	    if(dist < bestdist) {
		bestitem = item;
		bestdist = dist;
	    }
	}
	return bestitem;
    }
}


/* The drawing area */

class DiagramDisplay extends Canvas {
    VrijeInvulVeld editor;
    DiagramDisplay(VrijeInvulVeld ed , float thickness,Color textcolor,Color drawcolor, Font penfont){
	editor = ed;
	setBackground(VrijeInvulVeld.bgcolor);
	resize(VrijeInvulVeld.xsize,VrijeInvulVeld.ysize);
    }


    public void paint(Graphics g) {
	Graphics2D g2 = (Graphics2D) g;                                                                                                                    
    	g2.setFont(VrijeInvulVeld.penfont);    
    	g2.setStroke( new BasicStroke(VrijeInvulVeld.thickness)); 
	g2.setColor(VrijeInvulVeld.drawcolor);
	for(Enumeration e = editor.diagram.elements(); e.hasMoreElements(); ) {
	    ((DiagramItem)e.nextElement()).draw(g2);
	}
    }
    

    public boolean mouseDown(Event e, int x, int y){
	if(editor.current_event_consumer.down(x, y)){
	    repaint();
	    return true;
	} 
	else 
	{
	    return false;
	}
    }

    public boolean mouseUp(Event e, int x, int y){
	if(editor.current_event_consumer.up(x, y)){
	    repaint();
	    return true;
	} 
	else 
	{
	    return false;
	}
    }

    public boolean mouseDrag(Event e, int x, int y){
	if(editor.current_event_consumer.drag(x, y)){
	    repaint();
	    return true;
	} 
	else
	{
	    return false;
	}
    }

    public boolean keyDown(Event e, int key){
	if(editor.current_event_consumer.key(key)){
	    repaint();
	    return true;
	} 
	else 
	{
	    return false;
	}
    }
}

/* The control panel and the diagram items do things in response to input */

interface EventConsumer {
    public boolean down(int x, int y);
    public boolean up(int x, int y);
    public boolean drag(int x, int y);
    public boolean key(int key);
    public void yield();
}

/* The control panel */

class DiagramControls extends Panel implements EventConsumer {
    VrijeInvulVeld editor;
    CheckboxGroup buttons;
    Checkbox line, arrow, rect, ellipse, text, destroy, move;
    final int CREATE = 0, DESTROY = 1, MOVE = 2;
    

    DiagramControls( VrijeInvulVeld ed ,String t_line,String t_arrow,String t_rectangle,String t_ellipse,String t_text,String t_circle,String t_destroy,String t_move) {
	editor = ed;
	setBackground(Color.white);
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	Component button;
	setLayout(gridbag);
	buttons = new CheckboxGroup();
	c.gridy = 0;
	c.gridx = 0;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1;
	c.anchor = GridBagConstraints.CENTER;
	if(VrijeInvulVeld.draw_text){
	    button = new DiagramControl(this, CREATE, (new Text()).getClass(), t_text ,  true);
	    gridbag.setConstraints(button, c);add(button);c.gridx++;
	}
	if(VrijeInvulVeld.draw_line){
	    button = new DiagramControl(this, CREATE, (new Line()).getClass(), t_line ,  false);
	    gridbag.setConstraints(button, c);add(button);c.gridx++;
	}
	if(VrijeInvulVeld.draw_arrow){
	    button = new DiagramControl(this, CREATE, (new Arrow()).getClass(), t_arrow ,  false);
	    gridbag.setConstraints(button, c);add(button);c.gridx++;
	}
	if(VrijeInvulVeld.draw_rect){
	    button = new DiagramControl(this, CREATE, (new Rect()).getClass(), t_rectangle, false);
	    gridbag.setConstraints(button, c);add(button);c.gridx++;
	}
	if(VrijeInvulVeld.draw_ellipse){
	    button = new DiagramControl(this, CREATE, (new Ellipse()).getClass(), t_ellipse , false);
	    gridbag.setConstraints(button, c);add(button);c.gridx++;
	}
	button = new DiagramControl(this, MOVE, (new Object()).getClass(), t_move ,    false);
	gridbag.setConstraints(button, c);
	add(button);
	c.gridx++;
	button = new DiagramControl(this, DESTROY, (new Object()).getClass(), t_destroy  , false);
	gridbag.setConstraints(button, c);
	add(button);

    }
    public boolean down(int x, int y) {
	DiagramControl current_button = (DiagramControl)buttons.getCurrent();
	DiagramItem target;
	Diagram diagram = editor.diagram;
	editor.showStatus("");
	switch(current_button.mode) {
	    case CREATE:
	    try {
		Class class_ = current_button.class_;
		target = (DiagramItem)class_.newInstance();
		target.diagram = diagram;
		target.create();
		editor.current_event_consumer = target;
		diagram.addElement(target);
		target.down(x, y);
	    }
	    catch (InstantiationException ex) {}
	    catch (IllegalAccessException ex) {}
	    return true;
	    
	    case DESTROY:
	    target = diagram.nearby(x, y);
	    if(target != null) {diagram.removeElement(target);target.destroy();}
	    return true;
	    
	    case MOVE:
	    target = diagram.nearby(x, y);
	    if(target != null) {target.move();editor.current_event_consumer = target;target.down(x, y);}
	    return false;
	    
	    default:
	    return false;
	}
    }

    public boolean up(int x, int y) {
	return false;
    }

    public boolean drag(int x, int y) {
	return false;
    }

    public boolean key(int key) {
	return false;
    }

    public boolean keyDown(Event e, int key) {
	/* For some reason we get the canvas key presses.  Pass them on XXX */
	return editor.display.keyDown(e, key);
    }

    public void yield() {}
}

/* The buttons in the control panel */

class DiagramControl extends Checkbox {
    int mode;
    Class class_;
    DiagramControl(DiagramControls parent, int _mode, Class _class, String label, boolean state) {
	super(label, parent.buttons, state);
	mode = _mode;
	class_ = _class;
    }
}


/* The diagram items.
 * The methods implemented here are suitable for shapes defined by two
 * points (ends of a line, corners of an ellipse's bounding box).  Other
 * shapes should override.  Maybe I should use a subclass for this, but
 * some of the methods may be useful for other shapes (text could use
 * these methods for moving but not creating; it would be nice to store the 
 * state just by switching the methods).
 */

abstract class DiagramItem implements EventConsumer {
    Diagram diagram;
    int state;
    final int NONE=0, CREATE=1, MOVE=2;
    int x, y, w, h;
    int movex, movey;
    DiagramItem() {
	x = y = w = h = 0;
    }

    DiagramItem(StringTokenizer tok) throws Exception {
	if(tok.countTokens() != 4)
	throw new IllegalArgumentException();
	x = (new Integer(tok.nextToken())).intValue();
	y = (new Integer(tok.nextToken())).intValue();
	w = (new Integer(tok.nextToken())).intValue();
	h = (new Integer(tok.nextToken())).intValue();
    }

    abstract void draw(Graphics g);
    abstract String describe();
    abstract String specify_text();
    abstract int specify_text_x();
    abstract int specify_text_y();
    abstract int specify_line_x1();
    abstract int specify_line_y1();
    abstract int specify_line_x2();
    abstract int specify_line_y2();

    void create() {
	state = CREATE;
    }

    public void yield() {
	diagram.editor.current_event_consumer = diagram.editor.controls;
	state = NONE;
    }

    void destroy() {}

    void move() {
	state = MOVE;
    }

    public boolean down(int _x, int _y) {
	switch(state){
	    case CREATE:
	    x = _x; y = _y;
	    return true;
	    
	    case MOVE:
	    movex = _x; movey = _y;
	    return true;
	}
	return false;
    }

    public boolean up(int _x, int _y) {
	switch(state){
	    case CREATE:
	    w = _x - x; h = _y - y;
	    yield();
	    return true;
	    
	    case MOVE:
	    x += (_x - movex);
	    y += (_y - movey);
	    yield();
	    return true;
	}
	return false;
    }

    public boolean drag(int _x, int _y) {
	switch(state){
	    case CREATE:
	    w = _x - x; h = _y - y;
	    return true;
	
	    case MOVE:
	    x += (_x - movex);
	    y += (_y - movey);
	    movex = _x;
	    movey = _y;
	    return true;
	}
	return false;
    }

    public boolean key(int key) {
	return false;
    }

    abstract double distance(int x, int y);
}

class Ellipse extends DiagramItem {
    Ellipse(){super();}
    Ellipse(StringTokenizer tok) throws Exception {super(tok);}
    String describe() {return "ellipse," + x + "," + y + "," + w + "," + h;}
    String specify_text() {return null; }
    int specify_text_x() {return -1; }
    int specify_text_y() {return -1; }
    int specify_line_x1(){return -1;}
    int specify_line_y1(){return -1;}
    int specify_line_x2(){return -1;}
    int specify_line_y2(){return -1;}
    void draw(Graphics g) {
	g.drawOval(x, y, w, h);
    }
    double distance(int _x, int _y) {
	/* Do this better! */
	float ww = w < 1 ? 1 : w, hh = h < 1 ? 1 : h;
	float yscale = ww/hh;
	float xx = _x - (x + ww / 2);
	float yy = (_y - (y + hh / 2)) * yscale;
	double r = Math.sqrt(xx * xx + yy * yy);
	return Math.abs(r - ww / 2);
    }
}

/* The class name Rectangle is already taken :-( */

class Rect extends DiagramItem {
    Rect(){super();}
    Rect(StringTokenizer tok) throws Exception { super(tok);}

    String describe() {return "rectangle," + x + "," + y + "," + (x+w) + "," + (y+h);}
    String specify_text() {return null; }
    int specify_text_x() {return -1; }
    int specify_text_y() {return -1; }
    int specify_line_x1(){return -1;}
    int specify_line_y1(){return -1;}
    int specify_line_x2(){return -1;}
    int specify_line_y2(){return -1;}

    void draw(Graphics g) {
	g.drawLine(x  , y  , x+w, y  );
	g.drawLine(x+w, y  , x+w, y+h);
	g.drawLine(x+w, y+h, x  , y+h);
	g.drawLine(x  , y+h, x  , y  );
    }

    double distance(int _x, int _y) {
	double dtop, dbottom, dleft, dright, dist;
	if(_x < x){
	    dtop = Misc.distance(_x, _y, x, y);
	}
	else 
	{
	    if(_x > x+w){
		dtop = Misc.distance(_x, _y, x+w, y);
	    }
	    else
	    {
		dtop = Math.abs(_y - y);
	    }
	}    
    
	if(_x < x){
	    dbottom = Misc.distance(_x, _y, x, y+h);
	}
	else
	{
	    if(_x > x+w){
		dbottom = Misc.distance(_x, _y, x+w, y+h);
	    }
	    else
	    {
		dbottom = Math.abs(_y - (y+h));
	    }
	}
	
	if(_y < y)
	    dleft = Misc.distance(_x, _y, x, y);
	else if(_y > y+h)
	    dleft = Misc.distance(_x, _y, x, y+h);
	else
	    dleft = Math.abs(_x - x);
    
	if(_y < y)
	    dright = Misc.distance(_x, _y, x+w, y);
	else if(_y > y+h)
	    dright = Misc.distance(_x, _y, x+w, y+h);
	else
	    dright = Math.abs(_x - (x+w));

	dist = dtop;
	if(dbottom < dist){dist = dbottom;}
	if(dleft < dist){dist = dleft;}
	if(dright < dist){dist = dright;}
	
	return dist;
    }
}

class Line extends DiagramItem {
    Line() {super();}
    Line(StringTokenizer tok) throws Exception {super(tok);}
    void draw(Graphics g) {
	Graphics2D g2 = (Graphics2D) g;
	if(!VrijeInvulVeld.original){
	    int dy=(int) VrijeInvulVeld.ysize / VrijeInvulVeld.ygrid;
	    int dx=(int) VrijeInvulVeld.xsize / VrijeInvulVeld.ygrid;
	    if(state != 2){
		y=dy*((int) Math.round(y/dy));
		h=dy*((int) Math.round(h/dy));
	    }
	    if(state == 1 || state == 2){
		g2.setColor(VrijeInvulVeld.textcolor);
		g2.setStroke( new BasicStroke(1.0f));
		if(VrijeInvulVeld.textalign){
		    for(int p=0;p<VrijeInvulVeld.ysize;p=p+dy){
			g2.drawLine(0,p,VrijeInvulVeld.xsize,p);
		    }
		}
		else
		{
		    for(int p=0;p<VrijeInvulVeld.xsize;p=p+dx){
			g2.drawLine(p,0,p,VrijeInvulVeld.ysize);
		    }	    
		}
	    }
	}
	g2.setColor(VrijeInvulVeld.drawcolor);
	g2.setStroke( new BasicStroke(VrijeInvulVeld.thickness));
	g2.drawLine(x, y, x+w, y+h);
	VrijeInvulVeld.linecounter++;
    }
    String describe() {return "line," + x + "," + y + "," + (x+w) + "," + (y+h);}
    String specify_text() {return null; }
    int specify_text_x(){return -1; }
    int specify_text_y(){return -1; }
    int specify_line_x1(){return x;}
    int specify_line_y1(){return y;}
    int specify_line_x2(){return (x+w);}
    int specify_line_y2(){return (y+h);}

    double distance(int _x, int _y) {
	if(w == 0 && h == 0)
	return Misc.distance(x, y, _x, _y);
	/* Set origin to end of line */
	_x -= x;
	_y -= y;
	/* Find line length and unit vector along line */
	double len = Math.sqrt(w*w + h*h);
	double u = w / len;
	double v = h / len;
	/* Find nearest point on line using dot product */
	double r = _x * u + _y * v;
	if(r < 0)
	    return Misc.distance(0, 0, _x, _y);
	if(r > len)
	    return Misc.distance(w, h, _x, _y);
	    return Misc.distance(r * u, r * v, _x, _y);
    }
}

class Arrow extends Line {
    Arrow(){super();}
    Arrow(StringTokenizer tok) throws Exception { super(tok);}
    void draw(Graphics g) {
	/* Draw a line */
	Graphics2D g2 = (Graphics2D) g;
	if(!VrijeInvulVeld.original){
	    int dy=(int) VrijeInvulVeld.ysize / VrijeInvulVeld.ygrid;
	    int dx=(int) VrijeInvulVeld.xsize / VrijeInvulVeld.ygrid;
	    if( state != 2){
		y=dy*((int) Math.round(y/dy));
		h=dy*((int) Math.round(h/dy));
	    }
	    if(state == 1 || state == 2){
		g2.setColor(VrijeInvulVeld.textcolor);
		g2.setStroke( new BasicStroke(1.0f));
		if(VrijeInvulVeld.textalign){
		    for(int p=0;p<VrijeInvulVeld.ysize;p=p+dy){
			g2.drawLine(0,p,VrijeInvulVeld.xsize,p);
		    }
		}
		else
		{
		    for(int p=0;p<VrijeInvulVeld.xsize;p=p+dx){
			g2.drawLine(p,0,p,VrijeInvulVeld.ysize);
		    }	    
		}
	    }
	}
	    
	VrijeInvulVeld.linecounter++;
	g2.setColor(VrijeInvulVeld.drawcolor);
	g2.setStroke( new BasicStroke(VrijeInvulVeld.thickness));
	g2.drawLine(x, y, x+w, y+h);
	if(w == 0 && h == 0){return;}
	
	/* Find unit vector along line */
	double len = Math.sqrt(w*w + h*h);
	double u = w / len;
	double v = h / len;

	/* Draw arrow head */
	double _x = x + w - 5 * u;
	double _y = y + h - 5 * v;
	g2.drawLine(x+w, y+h, (int)(_x - 5 * v + 0.5), (int)(_y + 5 * u + 0.5));
	g2.drawLine(x+w, y+h, (int)(_x + 5 * v + 0.5), (int)(_y - 5 * u + 0.5));
    }
        
    String describe() {return "arrow," + x + "," + y + "," + (x+w) + "," +(y+h);}
    String specify_text() {return null; }
    int specify_text_x() {return -1; }
    int specify_text_y() {return -1; }
    int specify_line_x1(){return x;}
    int specify_line_y1(){return y;}
    int specify_line_x2(){return (x+w);}
    int specify_line_y2(){return (y+h);}

}

class Text extends DiagramItem {
    String text;
    StringBuffer buffer;
    Text() {
	x = y = 0;
	buffer = new StringBuffer();
	text = buffer.toString().concat("_");
    }

    Text(StringTokenizer tok) throws Exception {
	if(tok.countTokens() < 1){throw new IllegalArgumentException();}
	    x = (new Integer(tok.nextToken())).intValue();
	    y = (new Integer(tok.nextToken())).intValue();
	    text = tok.nextToken("\n\r").trim();

    }

    void create() {
	super.create();
	diagram.editor.controls.disable();
	//diagram.editor.showStatus("Press return to finish string");
	diagram.editor.showStatus(VrijeInvulVeld.t_press);
	VrijeInvulVeld.textcounter++;
    }

    public void yield() {
	super.yield();
	diagram.editor.controls.enable();
	text = buffer.toString();
    }

    String describe() {
	return "text," + x + "," + y + "," + text;
    }
    String specify_text() {return text; }
    int specify_text_x() {return x; }
    int specify_text_y() {return y; }
    int specify_line_x1(){return -1;}
    int specify_line_y1(){return -1;}
    int specify_line_x2(){return -1;}
    int specify_line_y2(){return -1;}
    

    void draw(Graphics g) {
    //jm.evers
	if(!VrijeInvulVeld.original){
	    Graphics2D g2 = (Graphics2D) g;
	    int dy=(int) VrijeInvulVeld.ysize / VrijeInvulVeld.ygrid;
	    int dx=(int) VrijeInvulVeld.xsize / VrijeInvulVeld.ygrid;
	    if(state == 1 || state == 2){//show a temp grid lines
		g2.setColor(VrijeInvulVeld.drawcolor);
		g2.setStroke( new BasicStroke(1.0f));
		if(VrijeInvulVeld.textalign){for(int p=0;p<VrijeInvulVeld.ysize;p=p+dy){g2.drawLine(0,p,VrijeInvulVeld.xsize,p);}}
		else{for(int p=0;p<VrijeInvulVeld.xsize;p=p+dx){g2.drawLine(p,0,p,VrijeInvulVeld.ysize);}}
		g2.setFont(VrijeInvulVeld.penfont);     
	    }
	    else
	    {
		g2.setFont(VrijeInvulVeld.penfont);    
	    }
	    if(state != 2){ // not while dragging
		if(VrijeInvulVeld.ygrid<50 && VrijeInvulVeld.ygrid>1){
		    if(VrijeInvulVeld.textalign){// then snap to the nearest vertical line...
	    		y=dy*((int) (Math.round(y/dy)));
			if(y<dy){y=dy;}
	    		if(y>VrijeInvulVeld.ysize-dy){y=VrijeInvulVeld.ysize-dy;}
		    }
		    else
		    {//snap to the nearest horizontal line...
	    		x=dx*((int) (Math.round(x/dx)));
			if(x<dx){x=dx;}
	    		if(x>VrijeInvulVeld.xsize-dx){x=VrijeInvulVeld.xsize-dx;}
		    }
		}
	    }
	    g2.setColor(VrijeInvulVeld.textcolor);
	    g2.setStroke( new BasicStroke(VrijeInvulVeld.thickness));
	    g2.drawString(text, x, y);
	    g2.setColor(VrijeInvulVeld.drawcolor);
	}
	else
	{
	    g.drawString(text, x, y);
	}
    }
    double distance(int _x, int _y) {
	FontMetrics fm = diagram.editor.display.getGraphics().getFontMetrics();
	int m = y - (fm.getAscent() / 2);
	if(_x < x){return Misc.distance(x, m, _x, _y);}
	int r = x + fm.stringWidth(text);
	if(_x > r){return Misc.distance(r, m, _x, _y);}
	return Math.abs(_y - m);
    }

    public boolean down(int _x, int _y) {
	switch(state) {
	    case CREATE:
	    x = _x; y = _y;
	    return true;
	    
	    case MOVE:
	    return super.down(_x, _y);
	}
	return false;
    }

    public boolean up(int _x, int _y) {
	switch(state) {
	    case CREATE:
	    x = _x; y = _y;
	    return true;
	    
	    case MOVE:
	    return super.up(_x, _y);    
	}
	return false;
    }

    public boolean drag(int _x, int _y) {
	switch(state) {
	    case CREATE:
	    x = _x; y = _y;
	    return true;
	    
	    case MOVE:
	    return super.drag(_x, _y);
	}
	return false;
    }

    public boolean key(int key) {
	char c[] = new char[1];
	switch(state) {
	    case CREATE:
	    switch(key) {
		case 10:/* LF */

		case 13:/* CR */
		yield();
		if(text.equals("")){
		    diagram.removeElement(this);
		    destroy();
		    diagram.editor.showStatus("String was empty");
		}
		else
		    diagram.editor.showStatus("");
		    return true;
		
		case 8:/* BS */
		case 127:/* DEL */
		int l = buffer.length();
	    if(l > 0)
		buffer.setLength(l - 1);
		break;
    
		default:
		buffer.append((char)key);
		break;
	    }
	    text = buffer.toString().concat("_");
	    return true;
	}
	return false;
    }
}

/* An area for displaying messages */

class MessageArea extends Canvas {
    String text = "";
    void setText(String s) {
	text = s;
	repaint();
    }

    public void paint(Graphics g) {
	setBackground(VrijeInvulVeld.bgcolor);
	//g.setFont(VrijeInvulVeld.messagefont);    
	g.drawString(text,5,20);
    }
}


/* Handy functions */

class Misc {
    static double distance(double x1, double y1, double x2, double y2) {
	return Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }
}
