
/*
    jm.evers
    2/2010
    no copyrights :all amateur scriblings
    Example usage:
    

<html>
 <body>
    <script type="text/javascript">
	function ReadThis(){
	    var reply = document.getElementById('WClock').readApplet();
	    document.getElementById('showtime').innerHTML='<hr>first value is pupils\'s timeanswer<br>second value is initial time<br><font color=red><b>'+reply+'</b></font><hr>';
	}

	function increase(t,a){
	    if(t == 0 ){var reply = document.getElementById('WDClock').setApplet("H",a);}
	    else
	    if(t == 1 ){var reply = document.getElementById('WDClock').setApplet("M",a);}
	    else
	    if(t == 2 ){var reply = document.getElementById('WDClock').setApplet("S",a);}
	}
	function reset(){
	    document.getElementById('WSClock').restart();
	}
    </script>
    <table>
	<tr>
	    <td>
		<div id="showtime"></div>
		<br>
		<applet id="WDClock" name="WDClock" code="WDClock.class" archive="WClock.jar" width="300" height="60">
		    <param name = "user_modify" value="yes"><!-- user sets time : default yes  0,no,1,yes -->
		    <param name = "fontsize" value = "150"><!-- size will be adjusted to xsize if too big -->
		    <param name = "backgroundcolor" value = "#ffffff"><!-- optional : default white -->
		    <param name = "displaycolor" value = "#ff00a0"><!-- optional : default red -->
		    <param name = "fontname" value = "Times"><!-- optional : default Helvetic -->
		    <param name = "use_system_time" value = "yes"><!-- optional : yes,1,no,0 set the clock to current system time -->
		    <param name = "clock_type" value="24"><!-- optional :default 12 hour clock -->
		    <param name = "hours" value="23"><!-- optional : if not set random value will be used -->
		    <param name = "minutes" value="2"><!-- optional : if not set random value will be used -->
		    <param name = "seconds" value="17"><!-- optional : if not set random value will be used -->
		    <param name = "alpha" value="120"><!-- optional : only for displaycolor -->
		</applet>
		<br>
		<input type="button" name="read_time" value="read time" onclick="javascript:ReadThis();">
		<br>
		Setting time via Javascript :<br>
		<input type="button" name="hours +" value="hours +" onclick="javascript:increase(0,1);">
		<input type="button" name="min +" value="min +" onclick="javascript:increase(1,1);">
		<input type="button" name="sec +" value="sec +" onclick="javascript:increase(2,1);">
		<br>
		<input type="button" name="hours &minus;" value="hours &minus;" onclick="javascript:increase(0,-1);">
		<input type="button" name="min &minus;" value="min &minus;" onclick="javascript:increase(1,-1);">
		<input type="button" name="sec &minus;" value="sec &minus;" onclick="javascript:increase(2,-1);">
		<br>		
	    </td>
	    <td>
		Mouse &amp; Key Controls [if param "user_modify = yes"]
		<ul>
		    <li> mouse over H,M,S display</li>
		    <li> space +1 </li>
		    <li> backspace -1</li>
		    <li> leftmouse +1</li>
		    <li> rightmouse -1</li>
		    <li> scrollmouse +/- 1 </li>
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

public class WDClock extends Applet implements  KeyListener, MouseListener, MouseMotionListener, MouseWheelListener{ 

    int xsize,ysize,H_in=0,M_in=0,S_in=0;
    Graphics2D backg;
    BufferedImage bg;
    Font font;
    int fontsize;
    String fontname;
    int edit=0;
    int clock_type=12;
    int uren,minuten,seconden;
    Color displaycolor,backgroundcolor;
    int alpha = -1;
    boolean user_modify = true;
    
    public void init(){
	String param = getParameter("user_modify");
	if(param != null && param.length() !=0){
	    if(param.equals("0") || param.equalsIgnoreCase("no")){user_modify = false;}
	}
	displaycolor = colorDecodeParam("displaycolor", Color.red);
	backgroundcolor = colorDecodeParam("backgroundcolor", Color.white);
	Dimension appletSize = this.getSize();
    	ysize = appletSize.height;xsize = appletSize.width;    
	bg = new BufferedImage(xsize,ysize, BufferedImage.TYPE_INT_ARGB);                                                         
    	backg = (Graphics2D) bg.getGraphics();
	font = FitFont(backg,"Helvetica",180);
	if(user_modify){
	    addMouseListener(this); 
	    addMouseMotionListener(this);
	    addMouseWheelListener(this);
	    addKeyListener(this);
	}
	getTime();
	uren = H_in;
	minuten = M_in;
	seconden = S_in;
	prepaint();
	//requestFocus();
    }
    
    public void setApplet(String type , int incr){
	if(type.equals("H")){setTime(0,incr);}
	else
	if(type.equals("M")){setTime(1,incr);}
	else
	if(type.equals("S")){setTime(2,incr);}
    }
    
    public Font FitFont(Graphics g , String default_fontname , int default_fontsize){
	fontsize = default_fontsize;
	fontname = default_fontname;
	String param; 
	param = getParameter("clock_type");
	if(param != null){
	    if(param.equals("24")){clock_type = 24;}
	}
	param = getParameter("fontsize");
	if(param != null){
	    try{
		fontsize = Integer.parseInt(param);
	    }catch(Exception e){System.out.println("could not parse fontsize "+param);}
	} 
	param = getParameter("font");
	if(param != null){
	    try{
		fontname = param;
	    }catch(Exception e){System.out.println("could nol load font"+param);}
	}
	font  = new Font(fontname,Font.BOLD,fontsize);
	FontMetrics fm = g.getFontMetrics(font);
	if(fm.stringWidth("00:00:00") > xsize){
	    boolean does_not_fit = true;
	    while(fontsize > 4 && does_not_fit){
		fontsize = fontsize - 1;
		font = new Font(fontname,Font.BOLD,fontsize);
		fm = g.getFontMetrics(font);
		if(fm.stringWidth("00:00:00") < xsize){
		    does_not_fit = false;
		}
	    }
	    System.out.println("xsize ("+xsize+") of applet too small...adjusting fontsize to "+fontsize);
	}
	return font;
    }
    
    public Color MakeTransparent(Color c, int a){
	return new Color(c.getRed(),c.getGreen(),c.getBlue(),a);
    }

    public Color colorDecodeParam(String param, Color c0){
	String c = getParameter(param);
	if( c != null && c.length() !=0){
            try{ c0 = Color.decode(c);}catch(Exception e){System.out.println("don\'t know color "+c+" ... setting to default");}
	}
	c = getParameter("alpha");
	if( c != null && c.length() !=0){
	    try{ alpha = Integer.parseInt(c);
		c0 = MakeTransparent(c0,alpha);
	    }catch(Exception e){System.out.println("could not parse alpha color "+ c );}
	}
	return c0;
    }
    
    public String MakeTimeString(int h , int m , int s){
	String H="";
	String M="";
	String S="";
	if(h>9){H=""+h+"";}else{H="0"+h;}
	if(m>9){M=""+m+"";}else{M="0"+m;}
	if(s>9){S=""+s+"";}else{S="0"+s;}
	return H+":"+M+":"+S;
    }
    public void getTime(){
	String param;boolean readparms=true;
	param = getParameter("use_system_time");
	if(param != null && param.length() !=0){
	    if(param.equals("1") || param.equalsIgnoreCase("yes")){
		Date date = new Date();
	        S_in = date.getSeconds();
		M_in = date.getMinutes();
	        H_in   = date.getHours();
		readparms=false;
	    }
	}
	if(readparms){
	    param = getParameter("hours");
	    if(param != null && param.length() !=0){
		H_in = Integer.parseInt(param);
	    }else{H_in =(int) (Math.round(clock_type*(Math.random())));}
	    
	    param = getParameter("minutes");
	    if(param != null && param.length() !=0){
		M_in = Integer.parseInt(param);
	    }else{M_in =(int) (Math.round(59*(Math.random())));}
	    
	    param = getParameter("seconds");
	    if(param != null && param.length() !=0){
		S_in = Integer.parseInt(param);
	    }else{S_in =(int) (Math.round(59*(Math.random())));}
	    if(H_in > clock_type){H_in =  H_in - clock_type;}	    
	    if(H_in > clock_type || H_in < 0){System.out.println("hours param "+ H_in+" is not correct");}
	    if(M_in > 60 || M_in <0){System.out.println("minutes param "+ M_in+" is not correct");}
	    if(S_in > 60 || S_in <0){System.out.println("seconds param "+ S_in+" is not correct");}
	}
    }

    public String readApplet(){
	return uren+":"+minuten+":"+seconden+","+H_in+":"+M_in+":"+S_in;
    }
	
    public void prepaint(){
	backg.setColor(backgroundcolor);                                                                                                 
	backg.fillRect(0,0,xsize,ysize);
	backg.setFont(font);
	backg.setColor(displaycolor);
	backg.drawString(MakeTimeString(uren,minuten,seconden),0,ysize);
    }

    public void paintComponent(Graphics g){
	Graphics2D g2 = (Graphics2D) g;
    	g2.drawImage(bg,0,0,this);
    }
	
    public void paint(Graphics g){
	update(g);
    }

    public void update(Graphics g){
	prepaint();
    	paintComponent(g);                                                                                                                 
    }

    public void run(){}
    public void start(){}
    public void restart(){
	getTime();
    	uren = H_in;
	minuten = M_in;
	seconden = S_in;
	repaint();
    }
    public void stop(){}

    public void mouseDragged(MouseEvent evt){
    //setTime(60);
    }
    public void mouseMoved(MouseEvent evt){	
	if(evt.getX()>0 && evt.getX()<xsize/3){edit = 0;}
	else
	if(evt.getX()>xsize/3 && evt.getX()<2*xsize/3){edit = 1;}
	else
	if(evt.getX()>2*xsize/3 && evt.getX()<xsize){edit = 2;}
    }
    public void mousePressed(MouseEvent evt){}
    public void mouseReleased(MouseEvent evt){}
    public void mouseEntered(MouseEvent evt){}
    public void mouseExited(MouseEvent evt){}
    public void mouseClicked(MouseEvent evt){
	edit = -1;
	if(evt.getX()>0 && evt.getX()<xsize/3){edit = 0;}
	else
	if(evt.getX()>xsize/3 && evt.getX()<2*xsize/3){edit = 1;}
	else
	if(evt.getX()>2*xsize/3 && evt.getX()<xsize){edit = 2;}
	
	if(edit >-1){
	    if(evt.getButton() == MouseEvent.BUTTON1){
	    setTime(edit,1);
	    }
	    else
	    if(evt.getButton() == MouseEvent.BUTTON3){
		setTime(edit,-1);
	    }
	}
    }
    public void mouseWheelMoved(MouseWheelEvent evt){setTime(edit,-1*(evt.getWheelRotation()));}
    public void keyPressed(java.awt.event.KeyEvent evt){
	int key=evt.getKeyCode();
	if(key == 32){setTime(edit,1);}
	else
	if(key == 8){setTime(edit,1);}
	//System.out.println(key);
    }
    public void keyTyped(KeyEvent evt){}                                                                                                                                                                                                                                               
    public void keyReleased(KeyEvent evt){}

    public void setTime(int e , int incr){
	if(e == 0){uren = uren + incr;} //uren
	else
	if(e == 1){minuten = minuten + incr;} // minuten
	else
	if(e == 2){seconden = seconden + incr;}//seconden
	
	if(seconden <0){seconden = 59;minuten = minuten - 1;}
	if(minuten <0){minuten = 59;uren = uren - 1;}
	if(uren <0){uren = clock_type-1;}
	
	if(seconden > 59){seconden = seconden%60;minuten++;}
	if(minuten > 59){minuten = minuten%60;uren++;}
	if(uren >= clock_type){uren = 0;}
	
	repaint();
    }

}

