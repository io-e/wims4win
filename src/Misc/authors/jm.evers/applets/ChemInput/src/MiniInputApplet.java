/*

*********************************************************************************
* J.M. Evers 19/11/2009*
* This is all amateur scriblings... So no copyrights.*
* This source code file, and compiled classes derived from it,*
* can be used and distributed without restriction, including for commercial use*
* No warrenty whatoever*
*********************************************************************************


Chemistry input applet for WIMS

Slightly based on ideas [and bits of code from...]  http://web.uconn.edu/~cdavid

                                   +     -
A chemical formula input like : H O  + OH  -> 2H O                                                                                        
                                 3              2 
And it's all amateur scriblings, no copyrights

Note: if the jar is not "signed" there will an annoying grey "bar" be visible with the text
"Java Applet Window". [in appletviewer this grey bar will probably be visible,  anyway]

TODO: help & usage in onmouseover?

Example:

<html>
 <body>
    <script type="text/javascript">
	function ReadThis(t){
	    var reply="";
	    if(t == 1){
		reply = document.getElementById('MiniInput').ReadAnswer();
	    }
	    else
	    {
		reply = document.getElementById('MiniInput').ReadLatex();
	    }	
	    alert(reply);
	
	}
    </script>
    <applet id="MiniInput" name="MiniInput" code="MiniInputApplet.class" archive="MiniInput.jar" width="120" height="40">
	<param name="applettext" value="H_{3}O^{+} + OH^{-} \leftrightarrow "><!-- latex question  ; default empty -->
	<param name="fontsize" value="18"><!-- default 18 -->
	<param name="pencolor" value="0,0,255,0.8"><!-- default black  ; R,G,B,alpha -->
	<param name="bgcolor1" value="20,240,240,0.2"><!-- canvas color;default white ; R,G,B,alpha -->
	<param name="bgcolor2" value="0,255,0,0.8"><!-- active typing bar; default green ;R,G,B,alpha -->
    </applet
    <p>
    <input type="button" name="ascii" value="ascii" onclick="javascript:ReadThis(1);">
    <input type="button" name="Latex" value="Latex" onclick="javascript:ReadThis(0);">       
    <ul>
	<li>F1 increase fontsize</li>
	<li>F2 decrease fontsize</li>
	<li>F3 Rightarrow</li>
	<li>F4 LeftRightarrow</li>
	<li>F5 change pencolor</li>
	<li>F6 change color background 1 </li>
	<li>F7 change color background 2 </li>
	<li>F8 delete all text : in case of param "applettext" this will de restored</li>
	<li> -> will give Rightarrow</li>
    </ul>	
 </body>
</html>

See "Schaersvoorde chemistry" WIMS modules for detailed usage 

3/2011  : removed depreciated keyUp()
11/2010 : added \rightleftharpoons arrow type
*/

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.awt.Color;

public class MiniInputApplet extends Applet implements  KeyListener,MouseListener,  MouseMotionListener, MouseWheelListener {

    public void init(){
	//
	String param="";
	param=getParameter("applettext");
	if(param != null && param.length() !=0){ applettext=filter1(param,"param2uni");}

 	param =  getParameter("fontsize");
	if(param != null && param.length() != 0){fontsize =  Integer.parseInt(param);}

	param=getParameter("pencolor");     
        if (param != null && param.length() != 0){pencolor = readColor(param);}
	else pencolor=new Color(0,0,0);                                                                                                                                                           

	param=getParameter("bgcolor1"); // bgcolor canvas    
        if (param != null && param.length() != 0){bgcolor1 = readColor(param);}
	else bgcolor1=new Color(255,255,255);                                                                                                                                                           

	param=getParameter("bgcolor2"); // bgcolor typeline
        if (param != null && param.length() != 0){bgcolor2 = readColor(param);}
	else bgcolor2=new Color(0,255,0);                                                                                                                                                           

	this.setBackground(bgcolor1);
        addMouseListener(this);
	addKeyListener(this); 
        addMouseMotionListener(this);
	addMouseWheelListener(this);


    }
    
    public Color readColor(String param){
	param=param.replace(':',',');param=param.replace(';',',');                                                  
        StringTokenizer q = new StringTokenizer(param, ",");                                                        
        String k;float rgb;R1=1.0f;G1=1.0f;B1=1.0f;alpha=0.5f;
	//System.out.println("Color should be in R,G,B,alpha \n <param name=\"pencolor\" value=\"0,0,255,0.8\">");
        for( int a=0; a<4 ; a++){                                                                                   
            k=q.nextToken();                                                                                        
            rgb=Float.parseFloat(k);                                                                         
            if(rgb<0){rgb=0.0f;}                                                                                       
            if(rgb>255){rgb=1.0f;}                                                                                   
            if(a == 0){R1 = (float)rgb/255;}                                                                                   
            else if(a == 1){G1 = (float)rgb/255;}                                                                              
            else if(a == 2){B1 = (float)rgb/255;}                                                                              
            else if(a == 3){alpha = rgb;}                                                                              
        }
	Color a = new Color(R1,G1,B1,alpha);
	return a;                                                                                                           
    }
    
    public boolean processKeyEvent(KeyEvent e, int key){
	return true;
    }
    
    public String ReadLatex(){
	String reply = MakeLaTeX();
	reply=filter1(reply,"uni2latex");
	return reply;	
    }

    public String ReadHtml(){
	String reply = MakeHtml();
	reply=filter1(reply,"uni2html");
	return reply;	
    }

    public String ReadAnswer(){
	String reply = MakeText();
	reply=filter1(reply,"uni2ascii");
	return reply;
    }
    
    public  String replace(String source, String pattern, String replace){
        if ( source!=null && pattern.length()!=0 ){
	    final int len = pattern.length();
	    StringBuffer sb = new StringBuffer();
	    int found = -1;
	    int start = 0;
	    while( (found = source.indexOf(pattern, start) ) != -1) {
		sb.append(source.substring(start, found));
		sb.append(replace);
		start = found + len;
	    }
	    sb.append(source.substring(start));
	    return sb.toString();
        }
        else return "";
    }
   
    public String filter1(String S1 , String k ){
        int p;String r1;String r2;
	String d1[] = new String[]{"\\rightleftharpoons","->","=>","-->","==>","\\rightarrow","<->","<=>","<-->","<==>","\\leftrightarrow","\\downarrow","\\uparrow"};
	
	String d2[] = new String[]{"\u21C4","\u2192","\u2192","\u2192","\u2192","\u2192","\u21C4","\u21C4","\u21C4","\u21C4","\u21C4","\u2193","\u2191"};

	String d3[] = new String[]{" <-> "," -> "," -> "," -> "," -> "," -> "," <-> "," <-> "," <-> "," <-> "," <-> ","(s) ","(g) "};

	String d4[] = new String[]{" \\rightleftharpoons "," \\rightarrow "," \\rightarrow "," \\rightarrow "," \\rightarrow "," \\rightarrow ",
	" \\leftrightarrow "," \\leftrightarrow "," \\leftrightarrow "," \\leftrightarrow "," \\leftrightarrow ",
	" \\downarrow "," \\uparrow "};

	String d5[] = new String[]{"&nbsp;&lt;&minus;&gt;&nbsp;","&nbsp;&minus;&gt;&nbsp;","&nbsp;&minus;&gt;&nbsp;","&nbsp;&minus;&gt;&nbsp;","&nbsp;&minus;&gt;&nbsp;","&nbsp;&minus;&gt;&nbsp;",
	"&nbsp;&minus;&gt;&nbsp;","&nbsp;&gt;&minus;&gt;&nbsp;","&nbsp;&gt;&minus;&gt;&nbsp;","&nbsp;&gt;&minus;&gt;&nbsp;","&nbsp;&gt;&minus;&gt;&nbsp;",
	"<sub>(s)</sub> ","<sub>(g)</sub>"};
	
	int d=d1.length;
	if(k.equals("param2uni")){// from ascii applettext-param to unicode applet
    	    for ( p=0;p<d;p++){                                                                                                                
        	r1=d1[p];                                                                                                                 
        	r2=d2[p];                                                                                                                 
        	S1=replace(S1,r1,r2);                                                                                                          
    	    }
	}
	else
	{
	    if(k.equals("uni2ascii")){// from applet unicode to ascii [wims answer]
    		for ( p=0;p<d;p++){                                                                                                                
        	    r1=d2[p];                                                                                                                 
        	    r2=d3[p];                                                                                                                 
        	    S1=replace(S1,r1,r2);                                                                                                          
    		}
	    }
	    else
	    {
		if(k.equals("uni2latex")){// from applet unicode to latex [wims presentation presentation] 
    		    for ( p=0;p<d;p++){                                                                                                                
        		r1=d2[p];                                                                                                                 
        	        r2=d4[p];                                                                                                                 
        		S1=replace(S1,r1,r2);
		    }                                                                                                         
    		}
		else
		{
		    if(k.equals("uni2html")){// from applet unicode to latex [wims presentation presentation] 
    			for ( p=0;p<d;p++){                                                                                                                
        		    r1=d2[p];                                                                                                                 
        	    	    r2=d5[p];                                                                                                                 
        		    S1=replace(S1,r1,r2);
			}
		    }                                                                                                         
    		}
	    }
	}
        return S1;                                                                                                                         
    }

    public synchronized void paint(Graphics g){
	f = new Font("TimesRoman",Font.BOLD,fontsize);
	sf = new Font("TimesRoman",Font.BOLD,fontsize-6);
	fm = g.getFontMetrics(f);
	sfm = g.getFontMetrics(sf);
	fh = fm.getHeight();
	if(once == false){
	    AppletText2Array(applettext);
	    once=true;
	}
	int wmax=getSize().width;
	int hmax = getSize().height;
	y0 = (hmax+fh)/2;
	g.clearRect(0,0,wmax,hmax);
	int x1=0;
	int y1;
	int x2=getSize().width;
	int y2;
	if(marked !=-1){position=CharPosition[marked];}                                                                                                                        
        if(position == 2){y1=y0-fh;y2=fh/2;}
        else                                                                                                                               
        if(position == 0){y1=y0;y2=fh/2;}
        else {y1=y0-fh/2;y2=fh/2;}
	if(editing){
	    g.setColor(bgcolor2);
	    g.fillRect(x1,y1,x2,y2);
	}
	g.setColor(pencolor);
	int w=0;
	for(int i=0;i<index;i++){
	    if(CharArray[i] != null){
		if(i == marked){
		    g.setColor(Color.red);g.setFont(f);g.drawString("_",w,y1+fh);
		}
		else
		{
		    if(CharArray[i].equals("?")){
			g.setColor(Color.red);
		    }
		    else
		    {
			g.setColor(pencolor);
		    }
		}
		if(CharPosition[i] == 1){g.setFont(f);g.drawString(CharArray[i],w,y0);}
		else
		if(CharPosition[i] == 0){g.setFont(sf);g.drawString(CharArray[i],w,y0 + fh/2);}
	        else
		if(CharPosition[i] == 2){g.setFont(sf);g.drawString(CharArray[i],w,y0-fh/2);}
		w=w+CharWidth[i];
		if(i == index-1 && marked == -1){
		    g.setColor(Color.red);g.drawString("_",w,y1+fh/2);
		} 
	    }
	}
    }
    
    public void update(){
	if(index > max_chars - 4){index = max_chars - 4;}
	repaint();
    }

    public String[] ForbiddenKeysChars={"!","~","`","@","#","$","%","&","|","\\","=","/","{","}"};
    public String[] AcceptedChars={"1","2","3","4","5","6","7","8","9","0","(",")"};
    public int[] ForbiddenKeys={16,17,18,20,27,120,154,155};
    public void clear(){// niet vergeten applettxt in de arrays !
	//System.out.println("calling clear");
	position=1;
	marked=-1;
	once=false;
	CharArray = new String[max_chars];
	CharPosition = new int[max_chars];
	CharWidth = new int[max_chars];
	update();
	index=0;
    }
    
    public void SwitchFont(int s){
	fontsize=fontsize + s;
	update();
    }
    public void AppletText2Array(String text){
	once=true;
	if(text.length() != 0){
	    text = replace(text," ","");
	    text = replace(text,"{","");
	    text = replace(text,"\\rightarrow","\u21C4");
	    text = replace(text,"\\leftrightarrow","\u2192");
	    text = replace(text,"\\downarrow","\u2193");
	    int i;
	    for( i=0;i<text.length();i++){
		if( text.charAt(i) == '^'){
		    i++;//remove ^ 
		    while(text.charAt(i) != '}' && i < text.length()){
			CharArray[i] = Character.toString(text.charAt(i));
			CharPosition[i] = 2;
			CharWidth[i] = sfm.charWidth(text.charAt(i));
			i++;
		    }
		}
		else
		if(text.charAt(i) == '_'){
		    i++;// remove _
		    while(text.charAt(i) != '}' && i < text.length()){
			CharArray[i] =  Character.toString(text.charAt(i));
			CharPosition[i] = 0;
			CharWidth[i] = sfm.charWidth(text.charAt(i));
			i++;
		    }
		}
		else
		{
		    CharArray[i] =  Character.toString(text.charAt(i));
		    CharPosition[i] = 1;
		    CharWidth[i] = fm.charWidth(text.charAt(i));
		}
	    }
	    marked = -1;
	    position = 1;
	    index =  i;
	}
    }

    public synchronized void mousePressed(MouseEvent evt){
	marked=-1;
        int x = evt.getX();int y = evt.getY();int p=1;
	if( y < y0 + fh && y > y0 + fh/4 ){ p = 0; } else { if ( y > y0 - 2*fh && y < y0 - fh/4){ p = 2;}}
	int widthprogress=0;
	for(int i=0; i<index;i++){
	    if(x > widthprogress  && x< widthprogress+CharWidth[i]){
		marked=i;
		if(p!=1){ position=p;}else{position=CharPosition[i];}
		if(evt.getButton() == MouseEvent.BUTTON3 ){DeleteMarkedChar(marked);}
		    //if(evt.getButton() == MouseEvent.BUTTON2 || evt.getButton() == MouseEvent.BUTTON3 ){ProcessMarkedKey("?" ,155);}
		repaint();
	    } 
	    widthprogress = widthprogress + CharWidth[i];
	}
    }
    public void mouseWheelMoved(MouseWheelEvent evt){
        position = NewPosition(-1*(evt.getWheelRotation()) , position);
	if(marked!=-1){
	    CharPosition[marked]=position;
	}
	update();
    }

    public synchronized void mouseDragged(MouseEvent evt){}
    public synchronized void mouseReleased(MouseEvent evt){}
    public void mouseEntered(MouseEvent mouseevent){editing=true;repaint();}
    public void mouseExited(MouseEvent mouseevent){editing=false;repaint();}
    public void mouseMoved(MouseEvent mouseevent){}
    public void mouseClicked(MouseEvent mouseevent){}

    public void keyPressed(KeyEvent e){
	if(index < max_chars - 4){
	    //System.out.println("KEYCODE ="+e.getKeyCode());
	    int key = e.getKeyCode();boolean found=false;
	    for(int i=0;i<ForbiddenKeys.length;i++){
		if( key == ForbiddenKeys[i] && found==false){found=true;}
	    }
	    if(found == false){
		String key_char = Character.toString(e.getKeyChar());
		if(marked == -1){ ProcessKey(key_char,key);}else{ProcessMarkedKey(key_char,key);}
		repaint();
	    }
	}else{index=0;} //hahaha
    }
    
    public void keyTyped(KeyEvent e ) {
    }                                                                                                                                        
                                                                                                                                           
    public void keyReleased(KeyEvent e){
    }     

    public void AppendKey(String key_char, int pos){
	CharPosition[index] = pos;
	CharArray[index] = key_char;
	int w=0;// find length string
	for(int i=0;i<key_char.length();i++){
	    if(pos == 0 || pos == 2){w=w+sfm.charWidth(key_char.charAt(i));}
	    else{w=w+fm.charWidth(key_char.charAt(i));}
	}
	CharWidth[index] = w;
	index++;
    }

    public void InsertKey(String key_char, int pos, boolean need_painting){
	if(marked!=-1){
	    index++;
	    for(int i=index;i>marked;i--){
		CharArray[i]=CharArray[i-1];
		CharPosition[i]=CharPosition[i-1];
		CharWidth[i]=CharWidth[i-1];
	    }
	    CharArray[marked] = key_char;
	    CharPosition[marked] = position;
	    if(position == 0 || position == 2){ CharWidth[marked] =  sfm.charWidth(key_char.charAt(0));}
	    else{CharWidth[marked] =  fm.charWidth(key_char.charAt(0));}
	    marked++;
	}
	if(need_painting){update();}
    }

    public void ProcessMarkedKey(String key_char ,int key){// only called if marked>-1
	if(key == 116){CycleColor(0);}//F5 change pencolor
	else
	if(key == 117){CycleColor(1);}//F6 change bgcolor1
	else
	if(key == 118){CycleColor(2);}//F7 change bgcolor2
	else
	if(key == 119){clear();}//F8 delete all
	else
	if(key == 114){ key_char = "\u2192";InsertKey(key_char,position,false);}//F3 -->
	else
	if(key == 115){ key_char = "\u21C4";InsertKey(key_char,position,false);}//F4 <-->
	else
	if(key == 37 ){LeftRightMarked(-1,false);}// linker pijl
	else
	if(key == 39 ){if(marked==index-1){marked = -1;}else{LeftRightMarked(1,false);}}// rechter pijl
	else
	if( key == 38 || key == 33){UpDownMarked(1,false);}//up
	else
	if( key == 40 || key == 34){UpDownMarked(-1,false);}//down
	else
	if(key == 10 || key == 27){marked = -1;}// esc enter
	if( (key == 8 || key == 127) && index>0 ){DeleteMarkedChar(marked);position=1;marked--;}//backspace & delete on marked 
	else
	if( (key == 8 || key == 127) && index == 0 ){ position=1;marked=-1;CharArray[0]=null; CharPosition[0]=1;CharWidth[0]=0;}//backspace & delete on marked 
	else
	{	    
	    boolean found=false;
	    for(int i=0;i<AcceptedChars.length;i++){
		if(AcceptedChars[i].equals(key_char) || key > 64 && key <91 || key == 45 || key == 61 ){found =true;}
	    }
	    if(found ==true){
		///System.out.println("inserting="+key_char);
		InsertKey(key_char,position,false);
	    }
	}
    }

    
    public void UpDownMarked(int p , boolean need_painting ){
	int i = NewPosition(p , position);
	if(i != position){
	    position = i;
	    CharPosition[marked] = position;
	}
	if(need_painting){update();}
    }

    public void LeftRightMarked(int p , boolean need_painting){
	for(int i=0;i<index;i++){
	    if(i == marked){
		if( p == 1){if( marked <  index - 1){marked++;break;}} else {if(marked>1){marked--; break;}}
	    }
	}
	if(need_painting){update();}
    }
    
    public void CycleColor(int type){
	if(R1==G1 && G1==B1){R1=0.0f;G1=0.0f;B1=0.0f;}//blue
	R1=R1+0.1f;
	if(R1>1.0){ R1=1.0f; // if red saturated increase green
	    G1=G1+0.1f;
	    if(G1>1.0){G1=1.0f;
		B1=B1+0.1f;
		if(B1>1.0){B1=1.0f;}
	    }
	}
	if(type == 0){pencolor = new Color(R1,G1,B1,(float)1.0);}
	else
	if(type == 1){bgcolor1 = new Color(R1,G1,B1,(float)1.0);this.setBackground(bgcolor1);}
	else { bgcolor2 = new Color(R1,G1,B1,(float)1.0);}
    }


    public void ProcessKey(String key_char ,int key){
	boolean goodkey=true;
	for(int i=0;i<ForbiddenKeysChars.length;i++){ if(key_char.equals(ForbiddenKeysChars[i]) && goodkey){ goodkey = false;} }
	if(goodkey){
	    if( key_char.equals(">") && index>2){if(CharArray[index-1].equals("-")){DeleteMarkedChar(index - 1); key_char = "\u2192";AppendKey(key_char,position);}}
	    else
	    if(key == 112){SwitchFont(6);} // F1 +6
	    else
	    if(key == 113){SwitchFont(-6);} // F2 -6
	    else
	    if(key == 114){ key_char = "\u2192";AppendKey(key_char,position);}//F3 -->
	    else
	    if(key == 115){ key_char = "\u21C4";AppendKey(key_char,position);}//F4 <-->
	    else
	    if(key == 116){CycleColor(0);}//F5 change pencolor
	    else
	    if(key == 117){CycleColor(1);}//F6 change bgcolor1
	    else
	    if(key == 118){CycleColor(2);}//F7 change bgcolor2
	    else
	    if(key == 119){clear();}//F8 delete all
	    else
	    if( key == 33 ){ position = NewPosition(1 , position);}//pageup
	    else
	    if( key == 34 ){ position = NewPosition(-1 , position);}//pagedown
	    else
	    if( key == 36 ){marked = 0;}//home
	    else
	    if( key == 37 ){marked = index-1;}//left arrow
	    else
	    if( key == 39 ){marked = index-1;}//left arrow
	    else
	    if( key == 35 ){marked = index;}//end
	    else
	    if(key == 10 || key == 27){marked = -1;position=1;index++;}// esc enter
	    else
	    if( (key == 127 || key == 8) && index>0){ index--; CharArray[index]=null; CharPosition[index]=1;CharWidth[index]=0;position=CharPosition[index];}
	    else
	    if( (key == 127 || key == 8) && index==0){ position=1;marked=-1; CharArray[0]=null; CharPosition[0]=1;CharWidth[0]=0;}
	    else
	    if( key == 38 || key == 94 ){ position = NewPosition(1 , position);}
	    else
	    if(  key == 40 || key == 95 ){ position = NewPosition(-1 , position);}
	    else{
		CharArray[index]=key_char;
		CharPosition[index]=position;
		if(position == 0 || position == 2){ CharWidth[index]= sfm.charWidth(key_char.charAt(0));}
		else { CharWidth[index]= fm.charWidth(key_char.charAt(0));}
		index++;
	    }
	}
    }

    
    public int NewPosition(int P , int old){
	int n = old + ( P );
	if(n>2){n=2;}
	else 
	if(n<0){n=0;}
	return n;
    }
    
    public void DeleteMarkedChar(int c){
	int m=0;
	for(int i=index-1;i>=0;i--){
	    if( i == c ){
		CharArray=DeleteStringItem(CharArray,i);
		CharPosition=DeleteIntItem(CharPosition,i);
		CharWidth=DeleteIntItem(CharWidth,i);
		CharArray[index-m]=null;
		CharPosition[index-m]=1;
		CharWidth[index-m]=0;
		m++;
	    }
	}
	index=index-m;
    }
    
    public int[] DeleteIntItem(int[] array1, int del){
	int m=0;
	for(int i=0;i<array1.length-1;i++){
	    if(i >= del){
		m=i+1;
		array1[i]=array1[m];
	    }
	}
	return array1;
    }
    public String[] DeleteStringItem(String[] array1, int del){
	int m=0;
	for(int i=0;i<array1.length-1;i++){
	    if(i >= del){
		m=i+1;
		array1[i]=array1[m];
	    }
	}
	return array1;
    }

    public void ShowHelp(){
	System.out.println("laat helptext zien");
    }

    public  String MakeText(){
	String Text = "";//homebrew replace("","","") can't handle stringbuffer.toString()
	for(int i=0;i<max_chars;i++){
	    if(CharArray[i] != null){
		if(CharPosition[i] == 1){Text=Text+CharArray[i];}
		else
		if(CharPosition[i] == 2 ){Text=Text+"^" ;
		    while(CharPosition[i] == 2  && CharArray[i]!=null){
		        Text=Text+CharArray[i] ;
			i++;
		    }
		    Text=Text+" ";
		    i=i-1;
		}
		else
		if(CharPosition[i] == 0 ){
		    while(CharPosition[i] == 0 && CharArray[i]!=null){
			Text=Text+CharArray[i] ;
			i++;
		    }
		    Text=Text+" ";
		    i=i-1;
		}
	    }
	}
	
	for(int p=1;p<10;p++){// Mn02 => MnO2 
	    Text=replace(Text,"0"+p,"O"+p);
	}
	return Text;
    }
    
    public  String MakeLaTeX(){
	StringBuffer LaTeX = new StringBuffer();
	for(int i=0;i<max_chars;i++){
	    if(CharArray[i] != null){
		if(CharPosition[i] == 1){LaTeX.append(CharArray[i]);}
		else
		if(CharPosition[i] == 2 ){LaTeX.append("^{") ;
		    while(CharPosition[i] == 2  && CharArray[i]!=null){
		        LaTeX.append(CharArray[i]) ;
			i++;
		    }
		    i=i-1;
		    LaTeX.append("}");
		}
		else
		if(CharPosition[i] == 0 ){LaTeX.append("_{");
		    while(CharPosition[i] == 0  && CharArray[i]!=null){
		    LaTeX.append(CharArray[i]) ;
			i++;
		    }
		    i=i-1;
		    LaTeX.append("}");
		}
	    }
	}
	return LaTeX.toString();
    }

    public  String MakeHtml(){
	StringBuffer Html = new StringBuffer();
	for(int i=0;i<max_chars;i++){
	    if(CharArray[i] != null){
		if(CharPosition[i] == 1){Html.append(CharArray[i]);}
		else
		if(CharPosition[i] == 2 ){Html.append("<sup><font size=\"-1\">") ;
		    while(CharPosition[i] == 2  && CharArray[i]!=null){
		        Html.append(CharArray[i]) ;
			i++;
		    }
		    i=i-1;
		    Html.append("</font></sup>");
		}
		else
		if(CharPosition[i] == 0 ){Html.append("<sub><font size=\"-1\">");
		    while(CharPosition[i] == 0  && CharArray[i]!=null){
		    Html.append(CharArray[i]) ;
			i++;
		    }
		    i=i-1;
		    Html.append("</font></sub>");
		}
	    }
	}
	return Html.toString();
    }

    public  int position=1;// 0,1,2 last edit
    public int marked=-1;
    public  int y0;
    public  FontMetrics sfm;
    public  FontMetrics fm;
    public  Font f;
    public  Font sf;
    public  int fh; //font height
    float R1,G1,B1,alpha;
    public  boolean once = false;
    boolean editing = false;
    public  int fontsize = 18;
    public  String applettext="";
    public  Color bgcolor1;
    public  Color pencolor;
    public  Color bgcolor2;
    public  int max_chars = 200;
    public  String[] CharArray = new String[max_chars];
    public  int[] CharPosition = new int[max_chars];
    public  int[] CharWidth = new int[max_chars];
    public  int index=0;
}

///////////////////////////////////////////////////



