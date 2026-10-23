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
		reply = document.getElementById('ChemInput').ReadAnswer();
	    }
	    else
	    {
		reply = document.getElementById('ChemInput').ReadLatex();
	    }	
	    alert(reply);
	
	}
    </script>
    <applet id="ChemInput" name="ChemInput" code="ChemInputApplet.class" archive="ChemInput.jar" width="120" height="40">
	<param name="applettext" value="H_{3}O^{+} + OH^{-} \leftrightarrow "><!-- latex question  ; default empty -->
	<param name="lang" value="nl"><!-- default en options nl,fr -->
	<param name="fontsize" value="18"><!-- default 18 -->
	<param name="frame_width" value="420"><!-- initial width of popup window ; is user resizable -->
	<param name="frame_height" value="180"><!-- initial height of popup window ; is user resizable  -->
	<param name="pencolor" value="0,0,255,0.8"><!-- default black  ; R,G,B,alpha -->
	<param name="bgcolor1" value="20,240,240,0.2"><!-- canvas color;default white ; R,G,B,alpha -->
	<param name="bgcolor2" value="0,255,0,0.8"><!-- active typing bar; default green ;R,G,B,alpha -->
	<param name="bgcolor3" value="255,255,255,0.8"><!-- menu color; default white ;R,G,B,alpha -->
	<param name="chembuttons" value="1"><!-- Default 1/yes ; options 0/no ; shows a few arrows-->
	<param name="navbuttons"  value="1"><!-- Default 1/yes ; options 0/no ; shows a few navigation buttons -->
    </applet
    <p>
    <input type="button" name="ascii" value="ascii" onclick="javascript:ReadThis(1);">
    <input type="button" name="Latex" value="Latex" onclick="javascript:ReadThis(0);">       
    
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

public class ChemInputApplet extends Applet implements ActionListener{

    class ChemFrameListener extends WindowAdapter{

        public synchronized void windowClosed(WindowEvent evt){
            launchButton.setLabel(LaunchText);
            launchButton.setEnabled(true);
            chemInput = null;
        }

        ChemFrameListener(){}
    }


    public void init(){
	//
	String param="";
	param =  getParameter("lang");
	if(param != null  && param.length() == 2 ){
	    if(param.equalsIgnoreCase("nl")){
		More="Groter lettertype";
		Less="Kleiner lettertype";
		Clear="Wissen";
		Solid="vast";
		Gas="gas";
		Up="naar boven";
		Down="naar beneden";
		Precip="neerslag";
		Liquid="opgelost";
		LaunchText="Start Chemie Applet";
		Chemarrows="Chemie";
		Color="Kleur";
		Text="Tekst";
		CloseText="Sluit venster";
		Title="Schaersvoorde Chemie Invoer Applet";
		Insert="Invoegen";
		Left="naar links";
		Right="naar rechts";
	    }
	    else
	    { 
		if(param.equalsIgnoreCase("fr")){
		    More="larger font";
		    Less="smaller font";
		    Clear="Effacer";
		    Solid="solide";
		    Gas="gaz";
		    Up="Up";
		    Down="Down";
		    Precip="precipitation";
		    Liquid="liquide";
		    LaunchText="Start Chemie Applet";
		    Chemarrows="Chimie";
		    Color="Couleur";
		    Text="Texte";
		    CloseText="Fermer";
		    Title="WIMS Chimie Input Applet";
		    Insert="Inserer";
		    Left="Gauche";
		    Right="Droite`";
		}                                                                                                                        
	    }
	}
	param=getParameter("applettext");
	if(param != null && param.length() !=0){ applettext=filter1(param,"param2uni");}

	param =  getParameter("chembuttons");
	if(param != null && param.length() != 0){ if(param.equalsIgnoreCase("no") || param.equals("0")){chembuttons = false; }}

	param =  getParameter("navbuttons");
	if(param != null && param.length() != 0){ if(param.equalsIgnoreCase("no") || param.equals("0")){navbuttons = false; }}
	
 	param =  getParameter("fontsize");
	if(param != null && param.length() != 0){fontsize =  Integer.parseInt(param);}

	param =  getParameter("frame_width");
	if(param != null && param.length() != 0){frame_x =  Integer.parseInt(param);}

	param =  getParameter("frame_height");
	if(param != null && param.length() != 0){frame_y =  Integer.parseInt(param);}

	param=getParameter("pencolor");     
        if (param != null && param.length() != 0){pencolor = readColor(param);}
	else pencolor=new Color(0,0,0);                                                                                                                                                           

	param=getParameter("bgcolor1"); // bgcolor canvas    
        if (param != null && param.length() != 0){bgcolor1 = readColor(param);}
	else bgcolor1=new Color(255,255,255);                                                                                                                                                           

	param=getParameter("bgcolor2"); // bgcolor typeline
        if (param != null && param.length() != 0){bgcolor2 = readColor(param);}
	else bgcolor2=new Color(0,255,0);                                                                                                                                                           

	param=getParameter("bgcolor3"); //bgcolor menu and button
        if (param != null && param.length() != 0){bgcolor3 = readColor(param);}
	else bgcolor1=new Color(255,255,255);                                                                                                                                                           

	this.setBackground(bgcolor3);
        setLayout(new BorderLayout());
        launchButton = new Button(LaunchText);
        add("Center", launchButton);
        launchButton.addActionListener(this);
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
    
    public synchronized void actionPerformed(ActionEvent evt){
        if(chemInput == null){
            launchButton.setLabel(CloseText);
            chemInput = new ChemInputFrame();
            chemInput.addWindowListener(new ChemFrameListener());
        } 
	else
        {
            launchButton.setLabel(LaunchText);
            launchButton.setEnabled(false);
            chemInput.dispose();
        }
    }

    public synchronized void destroy(){
	once=false;
	CharArray = new String[max_chars];// no idea why this is the only way to remove the content of arrays...
	CharPosition = new int[max_chars];
	CharWidth = new int[max_chars];
        if(chemInput != null){
            chemInput.dispose();
            chemInput = null;
        }
    }

    public String ReadLatex(){
	String reply = ChemInputCanvas.MakeLaTeX();
	reply=filter1(reply,"uni2latex");
	return reply;	
    }

    public String ReadHtml(){
	String reply = ChemInputCanvas.MakeHtml();
	reply=filter1(reply,"uni2html");
	return reply;	
    }

    public String ReadAnswer(){
	String reply = ChemInputCanvas.MakeText();
	reply=filter1(reply,"uni2ascii");
	return reply;
    }
    
    public static String replace(String source, String pattern, String replace){
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



    public ChemInputApplet(){}

	ChemInputFrame chemInput;
	Button launchButton;
	float R1,G1,B1,alpha;
	public static boolean chembuttons = true;
	public static boolean navbuttons = true;
	public static boolean once = false;
	public static int fontsize = 18;
	public static int frame_x=420;
	public static int frame_y=180;
	public static String applettext="";
	public static String Insert="Insert";
	public static String Text="Text";
	public static String Quit="Exit";
	public static String Color="Color";
	public static String Chemarrows="Chemistry";
	public static String More="larger font";
	public static String Less="smaller font";
	public static String Solid="solid";
	public static String Gas="gas";
	public static String Clear="Clear";
	public static String Arrow1="\u2192";
	public static String Arrow2="\u21C4";
	public static String Up="Up";
	public static String Down="Down";
	public static String Left="Left";
	public static String Right="Right";
	public static String Precip="Precipitate";
	public static String Liquid="liquid";
	public static String LaunchText="Launch Chemistry Applet";
	public static String CloseText="Close Applet";	
	public static String error1="error empty answer ?";
	public static String Title="WIMS chemistry input applet";
	public static Color bgcolor1;
	public static Color pencolor;
	public static Color bgcolor2;
	public static Color bgcolor3;
	public static int max_chars = 200;
	public static String[] CharArray = new String[max_chars];
	public static int[] CharPosition = new int[max_chars];
	public static int[] CharWidth = new int[max_chars];
	public static int index=0;
}
