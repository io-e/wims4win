/*

    jm.evers 2/2010
    No copyrights. All amateur scriblings.
    Example usage:
<html>
 <body>
    <script type="text/javascript">

	function speakTime(){
            var speaktime = document.getElementById('WClock').speakTime();
	    document.getElementById('showtime').innerHTML='<hr><font color=red><b>'+speaktime+'</b></font>' ;}

	function ReadThis(){
	    var reply = document.getElementById('WClock').readApplet();
	    document.getElementById('showtime').innerHTML='<hr>first value is pupils\'s timeanswer<br>second value is initial time<br><font color=red><b>'+reply+'</b></font><hr>';
	}
	
	function increase(t,a){
	    if(t == 0 ){var reply = document.getElementById('WClock').setApplet("H",a);}
	    else
	    if(t == 1 ){var reply = document.getElementById('WClock').setApplet("M",a);}
	    else
	    if(t == 2 ){var reply = document.getElementById('WClock').setApplet("S",a);}
	}
        function reset(){                                                                                                                 
	    document.getElementById('WClock').restart(); 
        }
	
    </script>
    <table>
	<tr>
	    <td>
		<div id="showtime"></div>
		<br>
		<applet id="WClock" name="WClock" code="WClock.class" archive="WClock.jar" width="300" height="300">
		    <param name = "language" value="nl"><!-- default english; optional nl,fr (fr is no good) -->
		    <param name = "user_modify" value="yes"><!-- user sets time : default yes  0,no,1,yes -->
		    <param name = "use_system_time" value="yes"><!-- optional 1,yes,0,no : overrules other time parameters -->
		    <param name = "use_seconds" value="yes"><!-- optional 1,yes,0,no -->
		    <param name = "hours" value="12"><!-- if not set random hour -->
		    <param name = "minutes" value="15"><!-- if not set: will use random minutes -->
		    <param name = "seconds" value="43"><!-- if not set: will random seconds -->
		    <param name = "background_color" value="#dafff6"><!-- optional  -->
		    <param name = "hour_hand_color" value="#007cff"><!-- optional  -->
		    <param name = "minutes_hand_color" value="#00c3ff"><!-- optional  -->
		    <param name = "seconds_hand_color" value="#ff1300"><!-- optional  -->
		    <param name = "numbers_color" value="#ff1300"><!-- optional  -->
		    <param name = "15_min_color" value="#0044ff"><!-- optional  -->
		    <param name = "5_min_color" value="#0044ff"><!-- optional  -->
		    <param name = "1_min_color" value="#0044ff"><!-- optional  -->
		    <param name = "alpha" value="100"><!-- optional  -->
		    <param name = "15_min_type" value="numbers"><!-- optional : diamonds ,lines ,numbers  or 0,1,2-->
		    <param name = "5_min_type" value="0"><!--  optional : diamonds ,lines ,numbers  or 0,1,2-->
		    <param name = "1_min_type" value="lines"><!--  optional : diamonds ,lines ,numbers or 0,1,2-->
		</applet>
		<br>
		<input type="button" name="read_time" value="read time" onclick="javascript:ReadThis();">
		<br>
		<input type="button" name="speak time" value="speak time" onclick="javascript:speakTime();">
		<br>
		<input type="button" name="hours +" value="hours +" onclick="javascript:increase(0,1);">
		<input type="button" name="min +" value="min +" onclick="javascript:increase(1,1);">
		<input type="button" name="sec +" value="sec +" onclick="javascript:increase(2,1);">
		<br>
		<input type="button" name="hours &minus;" value="hours &minus;" onclick="javascript:increase(0,-1);">
		<input type="button" name="min &minus;" value="min &minus;" onclick="javascript:increase(1,-1);">
		<input type="button" name="sec &minus;" value="sec &minus;" onclick="javascript:increase(2,-1);">
	    </td>
	    <td>
		Mouse &amp; Key Controls [if param "user_modify = yes"] 
		<ul>
		    <li>seconds
			<ul>
			    <li>s : +1 sec</li>
			    <li>F3 : +1 sec</li>
			    <li>r : -1 sec</li>
			    <li>F4 : -1 sec</li>
			</ul>
		    </li>
		    <li>minutes
			<ul>
			    <li>F1 : +1 min</li>
			    <li>m : +1 min</li>
			    <li>cursor -&gt; : +1 min</li>
			    <li>cursor &lt;- : -1 min</li>
			    <li>backspace : -1 min</li>
			    <li>pageup : -15 min</li>
			    <li>pagedown : +15 min</li>
			    <li>space : +1 min</li>
			    <li>left mousebutton : +1 min</li>
			    <li>right mousebutton : -1 min</li>
			    <li>scrollwheel : +/- 1 min</li>
			    <li>mouse drag : +1 min</li>
			</ul>
		    </li>
		    <li>hours
			<ul>
			    <li>F2 : +1 hour</li>
			    <li>h : +1 hour</li>
			    <li>F5 : -1 hour</li>
			    <li>- : -1 hour</li>
			    <li>delete : -1 hour</li>
			    <li>1,2,3,4,5,6,7,8,9 : +1,2,3,4,5,6,7,8,9 hours</li>
			</ul>
		    </li>
		</ul>
	    </td>
	</tr>
    </table>
 </body>
</html>

*/

import java.lang.*;
//import javax.imageio.ImageIO;                                                                                                              
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.applet.Applet;
import java.awt.*;
import java.util.*;
import java.io.*;
import java.awt.Color;
import java.awt.event.*;

public class WClock extends Applet implements  KeyListener, MouseListener, MouseMotionListener, MouseWheelListener{ 

    public Color hour_hand_color,min_hand_color,sec_hand_color,number_color;
    public Color horloge_color,backgroundcolor,ftmin_color,fimin_color,onemin_color;
    int xsize,ysize;
    double center_x,center_y;
    Graphics2D backg;                                                                                                                      
    BufferedImage bg;
    Graphics2D horloge;                                                                                                                      
    BufferedImage ggb;
    double seconds;
    int fifteenminute;
    int fiveminute;
    int oneminute;
    static int diamonds=0;
    static int lines=1;
    static int numbers=2;
    String helptxt;
    int H_in=-1;
    int M_in=-1;
    int S_in=-1;
    int alpha=-1;
    boolean use_seconds;
    boolean user_modify;
    public String[] telwoorden = {"zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen",
    "fourteen","fifteen","sixteen","seventeen","eightteen","nineteen","twenty","twentyone","twentytwo","twentythree",
    "twentyfour","twentyfive","twentysix","twentyseven","twentyeight","twentynine","thirty","thirtyone","thirtytwo",
    "thirtythree","thirtyfour","thirtyfive","thirtysix","thirtyseven","thirtyeight","thirtynine","fourty","fourtyone",
    "fourtytwo","fourtythree","fourtyfour","fourtyfive","fourtysix","fourtyseven","fourtyeight","fourtynine","fifty"
    };
    public String[] mtext = {"It is exactly speak_hours O'clock","It is a quarter past speak_hours",
    "It is speak_minutes minutes past speak_hours"," It is exactly half past speak_hours",
    "It is speak_minutes minutes before half past speak_hours","It is a quarter to speak_hours",
    "It is speak_minutes minutes over half past speak_hours","It is speak_minutes minutes to speak_hours",
    "and speak_seconds seconds"};
    public String [] single_multi_sec = {"second","seconds"};
    public String[] single_multi_min = {"minute","minutes"};
    
    public void init(){
	helptxt="minuten=F1,M,cursor,muis,spatiebalk  uren=F2,H  seconden=F3,S ";
	String param = getParameter("language");
	if(param != null && param.length() == 2){
	    if(param.equalsIgnoreCase("nl")){
		telwoorden = new String[] {"nul","een","twee","drie","vier","vijf","zes","zeven","acht","negen","tien",
		"elf","twaalf","dertien","veertien","vijftien","zestien","zeventien","achttien","negentien",
		"twintig","eenentwintig","tweeentwintig","drieentwintig","vierentwintig","vijfentwintig",
		"zesentwintig ","zevenentwintig"," achtentwintig"," negenentwintig"," dertig",
		"eenendertig","tweeendertig","drieendertig","vierendertig","vijfendertig","zesendertig","zevenendertig",
		"achtendertig","negenendertig","veertig","eennenveertig","tweeenveertig","drieenveertig","vierenveertig",
		"vijfenveertig","zesenveertig","zevenenveertig","achtenveertig","negenenveertig","vijftig"};
	
		mtext = new String[] {"Het is precies speak_hours uur","Het is kwart over speak_hours",
		"Het is speak_minutes minuten over speak_hours","Het is half speak_hours",
		"Het is speak_minutes minuten voor half speak_hours","Het is kwart voor speak_hours",
	        "Het is speak_minutes minuten over half speak_hours","Het is speak_minutes minuten voor speak_hours",
		"en speak_seconds seconden"};
		single_multi_sec = new String[] {"seconde","seconden"};
		single_multi_min = new String[] {"minuut","minuten"};
	    }
	    else
	    {
		System.out.println("Sorry, no support for other languages than English and Dutch");
	    }
	}
	
	param = getParameter("alpha");
	if(param != null && param.length() !=0){
	    try{alpha = Integer.parseInt(param);}catch(Exception e){System.out.println("Can not set alpha color to"+param);}
	}
	param = getParameter("user_modify");
	if(param != null && param.length() !=0){
	    if(param.equals("0") || param.equalsIgnoreCase("no")){user_modify = false;}else{user_modify = true;}
	}
	horloge_color = new Color(255,255,255,0); // do not change : needs to be transparent ... image drawn over number&hand image
	hour_hand_color = new Color(255,0,0,120);
	min_hand_color = new Color(0,255,0,120);
	sec_hand_color = new Color(0,0,255,120);
	number_color = new Color(0,50,255,120);
	backgroundcolor = new Color(255,255,255);
	ftmin_color = new Color(0,10,255,160);
	fimin_color = new Color(0,10,255,160);
	onemin_color = new Color(0,10,255,160);
	// reading params, reverting to default if not set.
	backgroundcolor = colorDecodeParam("background_color",backgroundcolor);
	hour_hand_color = colorDecodeParam("hour_hand_color",hour_hand_color);
	min_hand_color = colorDecodeParam("minutes_hand_color",min_hand_color);
	sec_hand_color = colorDecodeParam("seconds_hand_color",sec_hand_color);
	number_color = colorDecodeParam("numbers_color",number_color);
	ftmin_color = colorDecodeParam("15_min_color",ftmin_color);
	fimin_color = colorDecodeParam("5_min_color",fimin_color);
	onemin_color = colorDecodeParam("1_min_color",onemin_color);
	if(alpha>-1){
	    hour_hand_color = MakeTransparent(hour_hand_color,alpha); 
	    min_hand_color = MakeTransparent(min_hand_color,alpha);
	    sec_hand_color = MakeTransparent(sec_hand_color,alpha);
	}
	fifteenminute = setHorlogeType("15_min_type",2);
	fiveminute = setHorlogeType("5_min_type",0);
	oneminute = setHorlogeType("1_min_type",1);
	Dimension appletSize = this.getSize();                                                                                             
    	ysize = appletSize.height;xsize = appletSize.width;    
        center_x=(double) (xsize/2);center_y=(double) (ysize/2);
	bg = new BufferedImage(xsize,ysize, BufferedImage.TYPE_INT_ARGB);                                                         
    	backg = (Graphics2D) bg.getGraphics();
	ggb = new BufferedImage(xsize,ysize, BufferedImage.TYPE_INT_ARGB);                                                         
    	horloge = (Graphics2D) ggb.getGraphics();
	if(user_modify){
	    addMouseListener(this); 
	    addMouseMotionListener(this);
	    addMouseWheelListener(this);
	    addKeyListener(this);
	}
	prepaint();
	getTime();
	setTime(0);
	//requestFocus();
    }
	
     
   public String speakTime(){
   	int H = (int) ((seconds/3600) % 12);
	int M = (int) ((seconds/60) % 60);
	int S = (int) ((seconds) % 60);
	//System.out.println("H="+H+" M="+M+" S="+S);

	String text_m="";
	String enzoveelseconden="";
	String speak_hours="";
	String speak_minutes="";
	if( M > 15 ){ H++;}
	if( H == 0 || H > 12){ H = 12;}
	speak_hours=telwoorden[H];
	
	if( M <= 15 ){
	    if(M ==  0){
		text_m=mtext[0].replace("speak_hours",speak_hours);
	    }
	    else
	    { 
		if(M == 15){
		    text_m=mtext[1].replace("speak_hours",speak_hours);
		}
		else
		{	
		    speak_minutes=telwoorden[M];
		    text_m=mtext[2].replace("speak_minutes",speak_minutes);
		    text_m=text_m.replace("speak_hours",speak_hours);
		}
	    }
	}
        else
	{
	    if(M <= 30){
		if( M == 30){
		    text_m=mtext[3].replace("speak_hours",speak_hours);
		}
		else
		{
		    speak_minutes=telwoorden[30 - M];
		    text_m=mtext[4].replace("speak_minutes",speak_minutes);
		    text_m=text_m.replace("speak_hours",speak_hours);
		}
	    }
	    else
	    {
		if( M <= 45 ){
		    if(M == 45){
			text_m=mtext[5].replace("speak_hours",speak_hours);
		    }
		    else
		    {
			speak_minutes=telwoorden[M-30];
			text_m=mtext[6].replace("speak_minutes",speak_minutes);
			text_m=text_m.replace("speak_hours",speak_hours);
		    }
		}
		else
		{
		    if( M <= 60 ){
			speak_minutes=telwoorden[60-M];
			text_m=mtext[7].replace("speak_minutes",speak_minutes);
			text_m=text_m.replace("speak_hours",speak_hours);
		    }
		}
	    }
	}
	if( M == 1 || M == 29 || M == 31 || M == 59  ){text_m = text_m.replace(single_multi_min[1],single_multi_min[0]);}
	if( S != 0 && use_seconds){
	    enzoveelseconden=mtext[8].replace("speak_seconds",""+S+"");
	    if( S == 1 ){
		enzoveelseconden=enzoveelseconden.replace(single_multi_sec[1],single_multi_sec[0]);
	    }
	}
	return text_m+"..."+enzoveelseconden;
    }
    
 
   public void setApplet(String type , int incr){
	if(type.equals("H")){setTime((double)(incr*3600));}
	else
	if(type.equals("M")){setTime((double)(incr*60));}
	else
	if(type.equals("S") && use_seconds ){setTime((double)(incr*1));}
    }
    
    public int setHorlogeType(String param, int d){
	String c = getParameter(param);
	if(c != null && c.length() !=0){
	    try{
		if(c.equalsIgnoreCase("diamonds") || c.equals("0") ){return diamonds;}
	        else
		if(c.equalsIgnoreCase("lines") || c.equals("1") ){return lines;}
	        else
		if(c.equalsIgnoreCase("numbers") || c.equals("2") ){return numbers;}
	    }catch(Exception e){System.out.println("Can\'t handle "+param+" = "+c);}
	}
	return d;
    }
    
    public Color MakeTransparent(Color c, int a){
	return new Color(c.getRed(),c.getGreen(),c.getBlue(),a);
    }

    public Color colorDecodeParam(String param, Color c0){
	String c = getParameter(param);
	if( c != null && c.length() !=0){
            try{ c0 = Color.decode(c);}catch(Exception e){System.out.println("don\'t know color "+c+" ... setting to default");}
	}    
	return c0;
    }
    
    public void getTime(){
	String param;
	param = getParameter("use_seconds");
	if(param != null && param.length() !=0){
	    if(param.equals("1") || param.equalsIgnoreCase("yes")){use_seconds = true;}else{use_seconds = false;}
	}

	param = getParameter("use_system_time");
	if(param != null && param.length() !=0){
	    if(param.equals("1") || param.equalsIgnoreCase("yes")){
		Date date = new Date();
		if(use_seconds){ S_in = date.getSeconds(); }else{ S_in = 0; }
		M_in = date.getMinutes();
	        H_in   = date.getHours();
		seconds  = (double) (3600*H_in + 60*M_in + S_in);
		return;
	    }
	}
	param = getParameter("hours");
	if(param != null && param.length() !=0){
	    H_in = Integer.parseInt(param);
	}else{H_in =(int) (Math.round(12*(Math.random())));}
	    
	param = getParameter("minutes");
	if(param != null && param.length() !=0){
	    M_in = Integer.parseInt(param);
	}else{M_in =(int) (Math.round(60*(Math.random())));}
	if(use_seconds){
	    param = getParameter("seconds");
	    if(param != null && param.length() !=0){ S_in = Integer.parseInt(param); }else{ S_in = (int) (Math.round(60*(Math.random())));}
	}else{ S_in = 0;}
	seconds  = (double) (3600*H_in + 60*M_in + S_in);
    }

    public String readApplet(){
	int H = (int) ((seconds/3600) % 12);
	int M = (int) ((seconds/60) % 60);
	int S = (int) ((seconds) % 60);
	// user set time , initial time
	return H+":"+M+":"+S+","+H_in+":"+M_in+":"+S_in;
    }
	
    Polygon getDiamond(int seconds,double angle,double forwards,double backwards,double sideways){
	Polygon polygon=new Polygon();
	double leftsin=Math.sin((seconds-angle)*Math.PI/30);
	double leftcos=Math.cos((seconds-angle)*Math.PI/30);
	double middlesin=Math.sin(seconds*Math.PI/30);
	double middlecos=Math.cos(seconds*Math.PI/30);
	double rightsin=Math.sin((seconds+angle)*Math.PI/30);
	double rightcos=Math.cos((seconds+angle)*Math.PI/30);
	polygon.addPoint((int) (center_x + center_x*forwards*middlesin)  , (int) (center_y - center_y*forwards*middlecos));
	polygon.addPoint((int) (center_x + center_x*sideways*leftsin)    , (int) (center_y - center_y*sideways*leftcos));
	polygon.addPoint((int) (center_x + center_x*backwards*middlesin) , (int) (center_y - center_y*backwards*middlecos));
	polygon.addPoint((int) (center_x + center_x*sideways*rightsin)   , (int) (center_y - center_y*sideways*rightcos));
	return polygon;
    }

    void drawNumber(Graphics g,int seconds,int number,double size,double radius){
	Font font=new Font("TimesRoman",Font.PLAIN,(int)(Math.min(center_x,center_y)*size));
	FontMetrics fontmetrics=getFontMetrics(font);
	String string=(new Integer(number)).toString();
	int x=(int) (center_x + center_x*radius*Math.sin(seconds*Math.PI/30));
	int y=(int) (center_y - center_y*radius*Math.cos(seconds*Math.PI/30));
	x-=fontmetrics.stringWidth(string)/2;
	y+=fontmetrics.getAscent()/2;
	g.setColor(number_color);
	g.setFont(font);
	g.drawString(string,x,y);
    }
	
    public void prepaint(){
	backg.setColor(horloge_color);
	backg.fillOval(0,0,xsize,ysize);
	for(int n=0;n<60;n++){
	    if(n%15==0&&fifteenminute==diamonds){
		backg.setColor(ftmin_color);
		backg.fillPolygon(getDiamond(n,.4,1,.8,.9));
	    }
	    else if(n%15==0&&fifteenminute==numbers){
		backg.setColor(ftmin_color);
		drawNumber(backg,n,n==0?12:n/5,.17,.9);
	    }
	    else if(n%15==0&&fifteenminute==lines){
		backg.setColor(ftmin_color);
		drawNumber(backg,n,n==0?12:n/5,.17,.9);
	    }
	    else if(n%5==0&&fiveminute==diamonds){
		backg.setColor(fimin_color);
		backg.fillPolygon(getDiamond(n,.2,.95,.85,.9));
	    }
	    else if(n%5==0&&fiveminute==numbers){
		backg.setColor(fimin_color);
		drawNumber(backg,n,n==0?12:n/5,.1,.9);
	    }
	    else if(n%5==0&&fiveminute==lines){
		backg.setColor(fimin_color);
		drawNumber(backg,n,n==0?12:n/5,.1,.9);
	    }
	    else if(oneminute==lines){
		double sin=Math.sin(n*Math.PI/30);
		double cos=Math.cos(n*Math.PI/30);
		backg.setColor(onemin_color);
		backg.drawLine((int) (center_x + center_x*.88*sin) ,(int) (center_y - center_y*.88*cos) ,(int) (center_x + center_x*.92*sin),(int) (center_y - center_y*.92*cos));
	    }
	    else if(oneminute==numbers){
		backg.setColor(onemin_color);
		drawNumber(backg,n,n,.05,.9);
	    }
	}
    }

    public void prepainthorloge(){
	horloge.setColor(backgroundcolor); 
	horloge.fillRect(0,0,xsize,ysize);
	if(use_seconds){
	    Polygon S = getWijzer((seconds)%60,0.9,0.02,0.02,30);
	    drawWijzer(sec_hand_color,horloge,S);
	}
	Polygon M = getWijzer((seconds/60)%60,0.9,0.04,0.04,30);
	Polygon H = getWijzer(5*((seconds/3600)%12),0.6,0.05,0.05,30);
	drawWijzer(min_hand_color,horloge,M);
	drawWijzer(hour_hand_color,horloge,H);
	drawCenter(Color.black,horloge,(int)(0.05*xsize));
    }

    public void paintComponent(Graphics g){
	Graphics2D g2 = (Graphics2D) g;
    	g2.drawImage(ggb,0,0,this);
    	g2.drawImage(bg,0,0,this);
    }
	
    public void paint(Graphics g){
	update(g);
    }

    public void update(Graphics g){
	prepainthorloge();                                                                                                        
    	paintComponent(g);                                                                                                                 
    }

    Polygon getWijzer(double time,double forwards,double backwards,double sideways,int div){
	Polygon polygon=new Polygon();
	double sin = Math.sin(time*Math.PI/div); 
	double cos = Math.cos(time*Math.PI/div);
	polygon.addPoint((int) (center_x + center_x*forwards*sin)  ,(int) (center_y - center_y*forwards*cos));
	polygon.addPoint((int) (center_x + center_x*sideways*cos)  ,(int) (center_y + center_y*sideways*sin));
	polygon.addPoint((int) (center_x - center_x*backwards*sin) ,(int) (center_y + center_y*backwards*cos));
	polygon.addPoint((int) (center_x - center_x*sideways*cos)  ,(int) (center_y - center_y*sideways*sin));
	return polygon;
    }
	
    void drawCenter(Color c,Graphics g,int w){
	    g.setColor(c);
	    g.fillOval((int) (center_x-w/2),(int) (center_y-w/2),w,w);
    }

    void drawWijzer(Color c,Graphics g,Polygon polygon){
	g.setColor(c);
	g.fillPolygon(polygon);
	g.drawLine(polygon.xpoints[0],polygon.ypoints[0],polygon.xpoints[2],polygon.ypoints[2]);
	g.drawLine(polygon.xpoints[1],polygon.ypoints[1],polygon.xpoints[2],polygon.ypoints[2]);
	g.drawLine(polygon.xpoints[3],polygon.ypoints[3],polygon.xpoints[2],polygon.ypoints[2]);
	g.drawLine(polygon.xpoints[0],polygon.ypoints[0],polygon.xpoints[1],polygon.ypoints[1]);
	g.drawLine(polygon.xpoints[0],polygon.ypoints[0],polygon.xpoints[3],polygon.ypoints[3]);
    }


    public void run(){}
    public void start(){}
    public void stop(){}
    public void restart(){getTime();repaint();}

    public void mouseDragged(MouseEvent evt){setTime(60);}
    public void mouseMoved(MouseEvent evt){}
    public void mousePressed(MouseEvent evt){}
    public void mouseReleased(MouseEvent evt){}
    public void mouseEntered(MouseEvent evt){}
    public void mouseExited(MouseEvent evt){}
    public void mouseClicked(MouseEvent evt){if(evt.getButton() == MouseEvent.BUTTON1){setTime(60);}else{setTime(-60);}}
    public void mouseWheelMoved(MouseWheelEvent evt){setTime(-60*(evt.getWheelRotation()));}
    public void keyPressed(java.awt.event.KeyEvent evt){
	int key=evt.getKeyCode();
	if(key == 37 || key == 8 ){setTime(59*60);}// left arrow
	else
	if(key == 39 || key ==32 ){setTime(60);}// right arrow
	else
	if(key == 33){setTime(3*900);}//page down
	else
	if(key == 34){setTime(900);}//page up
	else
	if(key > 48 && key < 58){setTime(3600*(key-48));}//1,2,3,4,5,6,7,8,9	
	else
	if(use_seconds && (key == 83 || key == 114) ){setTime(1);}// s , F3
	else
	if(use_seconds && (key == 82 || key == 115) ){setTime(59);}// r , F4
	else
	if(key == 77 || key == 112 ){setTime(60);}// m , F1
	else
	if(key == 85 || key == 113 || key == 72){setTime(3600);}// u , h , F2
	else
	if(key == 116  || key == 127){setTime(11*3600);}// F5 ,del
	//System.out.println(key);
    }
    public void keyTyped(KeyEvent evt){}                                                                                                                                                                                                                                               
    public void keyReleased(KeyEvent evt){}

    public void setTime(double incr){
	seconds = seconds + incr;
	repaint();
    }

}



