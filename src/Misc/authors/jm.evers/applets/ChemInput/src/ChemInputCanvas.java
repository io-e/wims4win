/*
*********************************************************************************
* J.M. Evers 19/11/2009*
* This is all amateur scriblings... So no copyrights.*
* This source code file, and compiled classes derived from it,*
* can be used and distributed without restriction, including for commercial use*
* No warrenty whatoever*
*********************************************************************************
*/

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

class ChemInputCanvas extends Canvas implements MouseWheelListener,MouseListener, MouseMotionListener,KeyListener{

    ChemInputCanvas(){
        setBackground(ChemInputApplet.bgcolor1);
        addMouseListener(this);
	addKeyListener(this); 
        addMouseMotionListener(this);
	addMouseWheelListener(this);
    }

    public synchronized void paint(Graphics g){
	f = new Font("TimesRoman",Font.BOLD,newsize);
	sf = new Font("TimesRoman",Font.BOLD,newsize - 6);
	fm = g.getFontMetrics(f);
	sfm = g.getFontMetrics(sf);
	fh = fm.getHeight();
	if(ChemInputApplet.once == false){
	    AppletText2Array(ChemInputApplet.applettext);
	    ChemInputApplet.once=true;
	}
	int wmax=getSize().width;
	int hmax = getSize().height;
	y0 = hmax/2;
	g.clearRect(0,0,wmax,hmax);
	int x1=0;
	int y1;
	int x2=getSize().width;
	int y2;
	if(marked !=-1){position=ChemInputApplet.CharPosition[marked];}                                                                                                                        
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
	for(int i=0;i<ChemInputApplet.index;i++){
	    if(ChemInputApplet.CharArray[i] != null){
		if(i == marked){g.setColor(Color.red);g.setFont(f);g.drawString("_",w,y1+fh);}
		else{if(ChemInputApplet.CharArray[i].equals("?")){g.setColor(Color.red);}
		else{g.setColor(pencolor);}}
		
		if(ChemInputApplet.CharPosition[i] == 1){g.setFont(f);g.drawString(ChemInputApplet.CharArray[i],w,y0);}
		else
		if(ChemInputApplet.CharPosition[i] == 0){g.setFont(sf);g.drawString(ChemInputApplet.CharArray[i],w,y0 + fh/2);}
	        else
		if(ChemInputApplet.CharPosition[i] == 2){g.setFont(sf);g.drawString(ChemInputApplet.CharArray[i],w,y0-fh/2);}
		
		w=w+ChemInputApplet.CharWidth[i];
		if(i == ChemInputApplet.index-1 && marked == -1){
		    g.setColor(Color.red);g.setFont(f);g.drawString("_",w,y1+fh/2);
		} 
	    }
	}
    }
    
    public void update(){
	if(ChemInputApplet.index > ChemInputApplet.max_chars - 4){ChemInputApplet.index = ChemInputApplet.max_chars - 4;}
	repaint();
    }

    public String[] ForbiddenKeysChars={"!","~","`","@","#","$","%","&","|","\\","=","/","{","}"};
    public int[] ForbiddenKeys={16,17,18,20,27,39,112,113,114,115,116,117,118,119,120};
    public String[] AcceptedChars={"1","2","3","4","5","6","7","8","9","0","(",")"};

    public void clear(){// niet vergeten applettxt in de arrays !
	position=1;
	marked=-1;
	ChemInputApplet.once=false;
	ChemInputApplet.CharArray = new String[ChemInputApplet.max_chars];
	ChemInputApplet.CharPosition = new int[ChemInputApplet.max_chars];
	ChemInputApplet.CharWidth = new int[ChemInputApplet.max_chars];
	update();
	ChemInputApplet.index=0;
    }
    
    public void SwitchFont(int s){
	newsize=newsize + s;
	update();
    }
    public void AppletText2Array(String text){
	ChemInputApplet.once=true;
	if(text.length() != 0){
	    text = ChemInputApplet.replace(text," ","");
	    text = ChemInputApplet.replace(text,"{","");
	    text = ChemInputApplet.replace(text,"\\rightarrow","\u21C4");
	    text = ChemInputApplet.replace(text,"\\leftrightarrow","\u2192");
	    text = ChemInputApplet.replace(text,"\\downarrow","\u2193");
	    int i;
	    for( i=0;i<text.length();i++){
		if( text.charAt(i) == '^'){
		    i++;//remove ^ 
		    while(text.charAt(i) != '}' && i < text.length()){
			ChemInputApplet.CharArray[i] = Character.toString(text.charAt(i));
			ChemInputApplet.CharPosition[i] = 2;
			ChemInputApplet.CharWidth[i] = sfm.charWidth(text.charAt(i));
			i++;
		    }
		}
		else
		if(text.charAt(i) == '_'){
		    i++;// remove _
		    while(text.charAt(i) != '}' && i < text.length()){
			ChemInputApplet.CharArray[i] =  Character.toString(text.charAt(i));
			ChemInputApplet.CharPosition[i] = 0;
			ChemInputApplet.CharWidth[i] = sfm.charWidth(text.charAt(i));
			i++;
		    }
		}
		else
		{
		    ChemInputApplet.CharArray[i] =  Character.toString(text.charAt(i));
		    ChemInputApplet.CharPosition[i] = 1;
		    ChemInputApplet.CharWidth[i] = fm.charWidth(text.charAt(i));
		}
	    }
	    marked = -1;
	    position = 1;
	    ChemInputApplet.index =  i;
	}
    }

    public synchronized void mousePressed(MouseEvent evt){
	marked=-1;
        int x = evt.getX();int y = evt.getY();int p=1;
	if( y >  y0 - 2*fh && y <  y0 + 2*fh ){
	    if( y < y0 + fh && y > y0 + fh/4 ){ p = 0; } else { if ( y > y0 - 2*fh && y < y0 - fh/4){ p = 2;}}
	    int widthprogress=0;
	    for(int i=0; i<ChemInputApplet.index;i++){
		if(x > widthprogress  && x< widthprogress+ChemInputApplet.CharWidth[i]){
		    marked=i;
		    if(p!=1){ position=p;}else{position=ChemInputApplet.CharPosition[i];}
		    if(evt.getButton() == MouseEvent.BUTTON3 ){DeleteMarkedChar(marked);}
		    //if(evt.getButton() == MouseEvent.BUTTON2 || evt.getButton() == MouseEvent.BUTTON3 ){ProcessMarkedKey("?" ,155);}
		    repaint();
		} 
		widthprogress = widthprogress + ChemInputApplet.CharWidth[i];
	    }
	}
    }
    public void mouseWheelMoved(MouseWheelEvent evt){
        position = NewPosition(-1*(evt.getWheelRotation()) , position);
	if(marked!=-1){
	    ChemInputApplet.CharPosition[marked]=position;
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
	if(ChemInputApplet.index < ChemInputApplet.max_chars - 4){
	    //System.out.println("KEYCODE ="+e.getKeyCode());
	    int key = e.getKeyCode();
	    String key_char = Character.toString(e.getKeyChar());
	    if(marked == -1){ ProcessKey(key_char,key);}else{ProcessMarkedKey(key_char,key);}
	    repaint();
	}else{ChemInputApplet.index=0;} //hhh
    }
    
    public void keyTyped(KeyEvent e ) {
    }                                                                                                                                        
                                                                                                                                           
    public void keyReleased(KeyEvent e){
    }     

    public void AppendKey(String key_char, int pos){
	ChemInputApplet.CharPosition[ChemInputApplet.index] = pos;
	ChemInputApplet.CharArray[ChemInputApplet.index] = key_char;
	int w=0;// find length string
	for(int i=0;i<key_char.length();i++){
	    if(pos == 0 || pos == 2){w=w+sfm.charWidth(key_char.charAt(i));}
	    else{w=w+fm.charWidth(key_char.charAt(i));}
	}
	if(pos == 0 || pos == 2){ ChemInputApplet.CharWidth[ChemInputApplet.index] = w;}
	else { ChemInputApplet.CharWidth[ChemInputApplet.index] = w;}
	ChemInputApplet.index++;
    }

    public void InsertKey(String key_char, int pos, boolean need_painting){
	if(marked!=-1){
	    ChemInputApplet.index++;
	    for(int i=ChemInputApplet.index;i>marked;i--){
		ChemInputApplet.CharArray[i]=ChemInputApplet.CharArray[i-1];
		ChemInputApplet.CharPosition[i]=ChemInputApplet.CharPosition[i-1];
		ChemInputApplet.CharWidth[i]=ChemInputApplet.CharWidth[i-1];
	    }
	    ChemInputApplet.CharArray[marked] = key_char;
	    ChemInputApplet.CharPosition[marked] = position;
	    if(position == 0 || position == 2){ ChemInputApplet.CharWidth[marked] =  sfm.charWidth(key_char.charAt(0));}
	    else{ChemInputApplet.CharWidth[marked] =  fm.charWidth(key_char.charAt(0));}
	marked++;
	}
	if(need_painting){update();}
    }
    
    public void ProcessMarkedKey(String key_char ,int key){// only called if marked>-1
	if(key == 37 ){LeftRightMarked(-1,false);}// linker pijl
	else
	if(key == 39 ){if(marked==ChemInputApplet.index-1){marked = -1;}else{LeftRightMarked(1,false);}}// rechter pijl
	else
	if( key == 38 || key == 33){UpDownMarked(1,false);}//up
	else
	if( key == 40 || key == 34){UpDownMarked(-1,false);}//down
	else
	if(key == 10 || key == 27){marked = -1;}// esc enter
	if( (key == 8 || key == 127) && ChemInputApplet.index>0 ){DeleteMarkedChar(marked);position=1;marked--;}//backspace & delete on marked 
	else
	if( (key == 8 || key == 127) && ChemInputApplet.index == 0 ){ position=1;marked=-1;ChemInputApplet.CharArray[0]=null; ChemInputApplet.CharPosition[0]=1;ChemInputApplet.CharWidth[0]=0;}//backspace & delete on marked 
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

    public void AppendSymbol(String s, int pos){// VIA MUISKLIK + TOETSENBORD
	if(marked != -1){InsertKey(s,pos,false);marked = -1;}else{AppendKey(s,pos);}
	update();
    }
    
    public void UpDownMarked(int p , boolean need_painting ){
	int i = NewPosition(p , position);
	if(i != position){
	    position = i;
	    ChemInputApplet.CharPosition[marked] = position;
	}
	if(need_painting){update();}
    }

    public void LeftRightMarked(int p , boolean need_painting){
	for(int i=0;i<ChemInputApplet.index;i++){
	    if(i == marked){
		if( p == 1){ if(i<ChemInputApplet.index-1){i++;marked=i;}} else {if(i>0){marked=i-1;}}
	    }
	}
	if(need_painting){update();}
    }


    public void ProcessKey(String key_char ,int key){
	boolean goodkey=true;
	for(int i=0;i<ForbiddenKeysChars.length;i++){ if(key_char.equals(ForbiddenKeysChars[i]) && goodkey){ goodkey = false;} }
	for(int i=0;i<ForbiddenKeys.length;i++){ if(key == ForbiddenKeys[i] && goodkey){ goodkey = false;} }
	if(goodkey){
	    if( key == 33 ){ position = NewPosition(1 , position);}//pageup
	    else
	    if( key == 34 ){ position = NewPosition(-1 , position);}//pagedown
	    else
	    if( key == 36 ){marked = 0;}//home
	    else
	    if( key == 37 ){marked = ChemInputApplet.index-1;}//left arrow
	    else
	    if( key == 35 ){marked = ChemInputApplet.index-1;}//end
	    else
	    if( (key == 127 || key == 8) && ChemInputApplet.index>0){ChemInputApplet.index--; ChemInputApplet.CharArray[ChemInputApplet.index]=null; ChemInputApplet.CharPosition[ChemInputApplet.index]=1;ChemInputApplet.CharWidth[ChemInputApplet.index]=0;position=ChemInputApplet.CharPosition[ChemInputApplet.index];}
	    else
	    if( (key == 127 || key == 8) && ChemInputApplet.index==0){ position=1;marked=-1; ChemInputApplet.CharArray[0]=null; ChemInputApplet.CharPosition[0]=1;ChemInputApplet.CharWidth[0]=0;}
	    else
	    if( key == 38 || key == 94 ){ position = NewPosition(1 , position);}
	    else
	    if(  key == 40 || key == 95 ){ position = NewPosition(-1 , position);}
	    else{
		ChemInputApplet.CharArray[ChemInputApplet.index]=key_char;
		ChemInputApplet.CharPosition[ChemInputApplet.index]=position;
		if(position == 0 || position == 2){ChemInputApplet.CharWidth[ChemInputApplet.index]=  sfm.charWidth(key_char.charAt(0)); } else { ChemInputApplet.CharWidth[ChemInputApplet.index]= fm.charWidth(key_char.charAt(0));}
		ChemInputApplet.index++;
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
	for(int i=ChemInputApplet.index-1;i>=0;i--){
	    if( i == c ){
		ChemInputApplet.CharArray=DeleteStringItem(ChemInputApplet.CharArray,i);
		ChemInputApplet.CharPosition=DeleteIntItem(ChemInputApplet.CharPosition,i);
		ChemInputApplet.CharWidth=DeleteIntItem(ChemInputApplet.CharWidth,i);
		ChemInputApplet.CharArray[ChemInputApplet.index-m]=null;
		m++;
	    }
	}
	ChemInputApplet.index=ChemInputApplet.index-m;
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

    public static String MakeText(){
	String Text = "";//homebrew replace("","","") can't handle stringbuffer.toString()
	for(int i=0;i<ChemInputApplet.max_chars;i++){
	    if(ChemInputApplet.CharArray[i] != null){
		if(ChemInputApplet.CharPosition[i] == 1){Text=Text+ChemInputApplet.CharArray[i];}
		else
		if(ChemInputApplet.CharPosition[i] == 2 ){Text=Text+"^" ;
		    while(ChemInputApplet.CharPosition[i] == 2  && ChemInputApplet.CharArray[i]!=null){
		        Text=Text+ChemInputApplet.CharArray[i] ;
			i++;
		    }
		    Text=Text+" ";
		    i=i-1;
		}
		else
		if(ChemInputApplet.CharPosition[i] == 0 ){
		    while(ChemInputApplet.CharPosition[i] == 0 && ChemInputApplet.CharArray[i]!=null){
			Text=Text+ChemInputApplet.CharArray[i] ;
			i++;
		    }
		    Text=Text+" ";
		    i=i-1;
		}
	    }
	}
	
	for(int p=1;p<10;p++){// Mn02 => MnO2 
	    Text=ChemInputApplet.replace(Text,"0"+p,"O"+p);
	}
	return Text;
    }
    
    public static String MakeLaTeX(){
	StringBuffer LaTeX = new StringBuffer();
	for(int i=0;i<ChemInputApplet.max_chars;i++){
	    if(ChemInputApplet.CharArray[i] != null){
		if(ChemInputApplet.CharPosition[i] == 1){LaTeX.append(ChemInputApplet.CharArray[i]);}
		else
		if(ChemInputApplet.CharPosition[i] == 2 ){LaTeX.append("^{") ;
		    while(ChemInputApplet.CharPosition[i] == 2  && ChemInputApplet.CharArray[i]!=null){
		        LaTeX.append(ChemInputApplet.CharArray[i]) ;
			i++;
		    }
		    i=i-1;
		    LaTeX.append("}");
		}
		else
		if(ChemInputApplet.CharPosition[i] == 0 ){LaTeX.append("_{");
		    while(ChemInputApplet.CharPosition[i] == 0  && ChemInputApplet.CharArray[i]!=null){
		    LaTeX.append(ChemInputApplet.CharArray[i]) ;
			i++;
		    }
		    i=i-1;
		    LaTeX.append("}");
		}
	    }
	}
	return LaTeX.toString();
    }

    public static String MakeHtml(){
	StringBuffer Html = new StringBuffer();
	for(int i=0;i<ChemInputApplet.max_chars;i++){
	    if(ChemInputApplet.CharArray[i] != null){
		if(ChemInputApplet.CharPosition[i] == 1){Html.append(ChemInputApplet.CharArray[i]);}
		else
		if(ChemInputApplet.CharPosition[i] == 2 ){Html.append("<sup><font size=\"-1\">") ;
		    while(ChemInputApplet.CharPosition[i] == 2  && ChemInputApplet.CharArray[i]!=null){
		        Html.append(ChemInputApplet.CharArray[i]) ;
			i++;
		    }
		    i=i-1;
		    Html.append("</font></sup>");
		}
		else
		if(ChemInputApplet.CharPosition[i] == 0 ){Html.append("<sub><font size=\"-1\">");
		    while(ChemInputApplet.CharPosition[i] == 0  && ChemInputApplet.CharArray[i]!=null){
		    Html.append(ChemInputApplet.CharArray[i]) ;
			i++;
		    }
		    i=i-1;
		    Html.append("</font></sub>");
		}
	    }
	}
	return Html.toString();
    }

    
    public static int position=1;// 0,1,2 last edit
    public int marked=-1;
    public int newsize=ChemInputApplet.fontsize;
    public static int y0;
    public static FontMetrics fm;
    public static FontMetrics sfm;
    public static Font f;
    public static Font sf;// small
    public static int fh; //font height
    boolean editing=false;
    Color pencolor = ChemInputApplet.pencolor;
    Color bgcolor1 = ChemInputApplet.bgcolor1;
    Color bgcolor2 = ChemInputApplet.bgcolor2;
    Color bgcolor3 = ChemInputApplet.bgcolor3;
}

