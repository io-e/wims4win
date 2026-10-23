/*
*********************************************************************************
* J.M. Evers 19/11/2009								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatoever								*
*********************************************************************************

    Used as small but fairly powerfull calculator in WIMS
    No memory, no buttons: a single input line.
    Capable of sending the result -via javascript- to WIMS.
    
    Read Arity documentation for capabilities and Copyright/License.
    This little program has no copyrights. Ofcourse.
    
    todo: finish a functionplotter using the Arity-lib. [funcionality triggered by params]

    Example:
    <html>
	<head></head>
	<body>
	    <script language="javascript" type="text/javascript">
		function READTHIS(){
		    var input=document.getElementById("calculator").ReadApplet(){
			if(input.indexOf("error")!=-1){
			alert(input);
		    }
		    else
		    {
			alert("the applet will send:\n"+input);
		    }
		}                                                                                                                                         
	    </script>                                                                                                                                  
	    <center>
	    
	    <applet id="calculator" code="Rekenmachien.class"  archive="Rekenmachien.jar" width="120" height="60">
		<param name="decimals" value="6"> <!-- precision: number of decimals -->
		<!-- defaults should be OK ... but if needed override with these params -->
		<param name="frame_xsize" value="300"> <!-- xsize of popup frame -->
		<param name="frame_ysize" value="240"> <!-- ysize of popup frame -->
		<param name="location_x" value="300"> <!-- x location of popup frame -->
		<param name="location_y" value="300"> <!-- y location of popup frame -->
		<param name="fontsize1" value="10"> <!-- input fontsize -->
		<param name="fontsize2" value="10"> <!-- output / remarks fontsize -->
		<param name="fontsize3" value="16"> <!-- popup frame fontsize -->
		<param name="language" value="en"> <!-- en,nl,fr,de -->
		<param name="pencolor1" value="10,3,2"> <!-- input foreground color -->
		<param name="pencolor2" value="10,3,2"> <!-- input foreground color -->
		<param name="bgcolor1" value="200,250,255"> <!-- input background color -->
		<param name="bgcolor2" value="245,255,255"> <!-- canvas background color -->
	    </applet>
	    <input type="button" name="Send" value="Send" onclick="javascript:READTHIS();">
	</body>
    </html>
*/


import java.awt.*;
import java.applet.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;

public class Rekenmachien extends JApplet {
    GraphWindow graphwindow = new GraphWindow();
    InputWindow inputwindow = new InputWindow();
    private JTextField input; /* need javax.swing JTextField : no tooltips for TextField !! */
    private JTextField output;
    private JTextField remarks;
    boolean visible=false;
    Font myfont1,myfont2,myfont3;
    int font_size1=12,font_size2=12,font_size3=14;
    int frame_x=300,frame_y=240,x0=300,y0=300,rounding=6;
    int R1=225,G1=255,B1=255;
    double precision=0.000001;
    DecimalFormat decformat;
    Color pencolor1,pencolor2,bgcolor1,bgcolor2;
    String rem1="\u2023 Mathematical error";
    String rem2="\u2023 Nonsense Error";
    String rem3="\u2023 calculator";
    String rem4="\u2023 done";
    String rem5="\u2023 ln(e)=1 log(10)=1";
    String rem6="\u2023 Usage [click open/close]";
    String rem7="\u2023 Please wait...";
    String rem8;
    String rem9="\u2023 precision";
    String rem10;
    String rem11="\u2023 You could also erase this text...\n and type in your formula or calculation...\n Press Enter to start calculations.";

    public void init(){
	initparams();
	initfonts();
	inittextareas();
	setLayout(new GridLayout(3, 1));
	initpopups();
	initlisteners();
    }
    
    public void initlisteners(){
	// enter and arrow_down is accepted for starting the calculation in the input field
	input.addKeyListener( new KeyAdapter() {
    	    public void keyPressed(KeyEvent evt) {
		if(evt.getKeyCode() == evt.VK_ENTER  || evt.getKeyCode() == evt.VK_DOWN ){
    		    repaint();
    		}
	    }
	});

	inputwindow.textarea.addKeyListener( new KeyAdapter() {
    	    public void keyPressed(KeyEvent evt) {
		if(evt.getKeyCode() == evt.VK_ENTER  || evt.getKeyCode() == evt.VK_DOWN ){
    		    repaint();
    		}
	    }
	});
	
	// remarks area accepts mouse_click to set the GraphWindow frame visible/invisible
	remarks.addMouseListener( new MouseAdapter(){
	    public void mousePressed(MouseEvent e){
		if(visible){
	    	    inputwindow.setVisible(false);
		    visible=false;
		}
		else
		{
	    	    inputwindow.setVisible(true);
		    visible=true;
		}
	    }
	});    
    }
    
    public void inittextareas(){
	input=new JTextField(font_size1);
	output=new JTextField(font_size2);
	remarks=new JTextField(font_size2);
	input.setForeground(pencolor1);
	output.setForeground(pencolor2);
	remarks.setForeground(pencolor2);
	input.setBackground(bgcolor1);
	output.setBackground(bgcolor2);	    
	remarks.setBackground(bgcolor2);	    
	input.setFont(myfont1);
	output.setFont(myfont2);
	remarks.setFont(myfont2);
	add(input);add(output);add(remarks);
	input.setText("? ");
	output.setText("\u2023 "); 
	remarks.setText(rem6);
	// only a tooltip over the remark inputfield
	remarks.setToolTipText(rem8); 
	input.setCaretPosition(2); 
    }
    
    public void initpopups(){
    	inputwindow.setSize(frame_x,frame_y);
	inputwindow.setLocation(x0,y0);
	inputwindow.textarea.setFont(myfont3);
	inputwindow.textarea.setBackground(bgcolor1);
	inputwindow.panel1.setBackground(bgcolor1);
	graphwindow.setSize(frame_x,frame_y);
	graphwindow.setLocation(x0+20,y0+20);
    }
    public void initfonts(){
    	myfont1 = new Font("Helvetica", Font.BOLD, font_size1);
	myfont2 = new Font("Helvetica", Font.BOLD, font_size2);
	myfont3 = new Font("Helvetica", Font.BOLD, font_size3);
    }

    public void initparams(){
        String param="";
	param=getParameter("language");
	if(param!=null && param.length()>0){
	    if(param.equalsIgnoreCase("nl")){
		rem1="\u2023 Wiskundige fout";
		rem2="\u2023 Onzinnige invoer";
		rem3="\u2023 rekenmachine";
		rem4="\u2023 klaar";
		rem5="\u2023 ln(e)=1 log(10)=1";
		rem6="\u2023 info [klik open & dicht]";
		rem7="\u2023 even geduld...";
		rem9="\u2023 precisie";
		rem11="\u2023 Je mag deze tekst ook wissen...\n en dit venster gebruiken voor de berekening \n en druk \"enter\" om je berekening te starten";
	    } 
	    else
	    {
		if(param.equalsIgnoreCase("de")){
		    rem1="\u2023 Mathematik Fehler";
		    rem2="\u2023 Nonsense Fehler";
		    rem3="\u2023 Taschenrechner";
		    rem4="\u2023 fertig";
		    rem5="\u2023 ln(e)=1 log(10)=1";
		    rem6="\u2023 info [click open & close]";
		    rem7="\u2023 einen moment...";
		    rem9="\u2023 precision";
		    rem11="\u2023 You could also erase this text...\n and type in your formula or calculation...\n Press Enter to start calculations.";
		}
		else
		{
		    if(param.equalsIgnoreCase("fr")){
			rem1="\u2023 Erreur mathematique";
			rem2="\u2023 Nonsense Erreur";
			rem3="\u2023 calculateur";
			rem4="\u2023 fini";
			rem5="\u2023 ln(e)=1 log(10)=1";
			rem6="\u2023 info [click open & close]";
			rem7="\u2023 Please wait...";
			rem9="\u2023 precision";
			rem11="\u2023 You could also erase this text...\n and type in your formula or calculation...\n Press Enter to start calculations.";
		    }
		}
	    }
	}

	// display precision 
	param=getParameter("decimals");
	if(param!=null && param.length()>0){
	    rounding =Integer.parseInt(param, 10);
	    precision=(double) 1/(Math.pow(10,rounding));
	}
	String numformat="#.";
	for(int n=0;n<rounding;n++){
	    numformat=numformat+"#";
	}
	decformat = new DecimalFormat(numformat);

	// very slow java tooltip text: multiline through HTML
	rem8="<html><body bgcolor=white><ul><li>\u2023"+rem9+": \u00B1 "+precision+"</li><li>6*(123/4+1234/5)/(7+9)</li><li>(123^(4/3)+5)/6</li><li>sqrt(123/4)/5</li><li>sqrt[3](1234)</li><li>sin(2*pi)</li><li>sinh(pi)</li><li>log(123)/log(10)</li><li>ln(e)=log(e)=1</li></ul>";
	rem10=rem9+": \u00B1 "+precision+"\n\u2023 sqrt[3](1234) \n\u2023 sin(2*pi) \n\u2023 sinh(pi) \n\u2023 log(123)/log(10) [ ln(e) == log(e) = 1 ]\n"+rem11;
	// adjust the GraphWindow frame...
	param=getParameter("frame_xsize");
	if(param!=null && param.length()>0){frame_x = Integer.parseInt(param, 10);}
	param=getParameter("frame_ysize");
	if(param!=null && param.length()>0){frame_y = Integer.parseInt(param, 10);}
	param=getParameter("location_x");
	if(param!=null && param.length()>0){x0 = Integer.parseInt(param, 10);}
	param=getParameter("location_y");
	if(param!=null && param.length()>0){y0 = Integer.parseInt(param, 10);}
	// adjust the fonts
	param=getParameter("fontsize1");
	if(param!=null && param.length()>0){font_size1 = Integer.parseInt(param, 10);}
	param=getParameter("fontsize2");
	if(param!=null && param.length()>0){font_size2 = Integer.parseInt(param, 10);}
	param=getParameter("fontsize3");
	if(param!=null && param.length()>0){font_size3 = Integer.parseInt(param, 10);}
	// adjust the pen colours
	param=getParameter("pencolor1"); // foreground color of inputfield "input"
	if (param != null && param.length()>0){
	    param=param.replace(':',',');param=param.replace(';',',');
	    StringTokenizer q = new StringTokenizer(param, ",");
	    String k;int rgb;
	    for( int a=0; a<3 ; a++){
		k=q.nextToken();
		rgb=Integer.parseInt(k, 10);
		if(rgb<0){rgb=0;}
		if(rgb>255){rgb=255;}
		if(a == 0){R1 = rgb;}
		else if(a == 1){G1 = rgb;}
		else if(a == 2){B1 = rgb;}
	    }
	    pencolor1=new Color(R1,G1,B1);
	}
	param=getParameter("pencolor2"); // foreground color of inputfield "input"
	if (param != null && param.length()>0){
	    param=param.replace(':',',');param=param.replace(';',',');
	    StringTokenizer q = new StringTokenizer(param, ",");
	    String k;int rgb;
	    for( int a=0; a<3 ; a++){
		k=q.nextToken();
		rgb=Integer.parseInt(k, 10);
		if(rgb<0){rgb=0;}
		if(rgb>255){rgb=255;}
		if(a == 0){R1 = rgb;}
		else if(a == 1){G1 = rgb;}
		else if(a == 2){B1 = rgb;}
	    }
	    pencolor2=new Color(R1,G1,B1);
	}
	// adjust the background colours
	param=getParameter("bgcolor1"); // Background color of inputfield "input"
	if (param != null && param.length()>0){
	    param=param.replace(':',',');param=param.replace(';',',');
	    StringTokenizer q = new StringTokenizer(param, ",");
	    String k;int rgb;
	    for( int a=0; a<3 ; a++){
		k=q.nextToken();
		rgb=Integer.parseInt(k, 10);
		if(rgb<0){rgb=0;}
		if(rgb>255){rgb=255;}
		if(a == 0){R1 = rgb;}
		else if(a == 1){G1 = rgb;}
		else if(a == 2){B1 = rgb;}
	    }
	    bgcolor1=new Color(R1,G1,B1);
	}
	param=getParameter("bgcolor2"); // Background color of canvas
	if (param != null && param.length()>0){
	    param=param.replace(':',',');param=param.replace(';',',');
	    StringTokenizer q = new StringTokenizer(param, ",");
	    String k;int rgb;
	    for( int a=0; a<3 ; a++){
		k=q.nextToken();
		rgb=Integer.parseInt(k, 10);
		if(rgb<0){rgb=0;}
		if(rgb>255){rgb=255;}
		if(a == 0){R1 = rgb;}
		else if(a == 1){G1 = rgb;}
		else if(a == 2){B1 = rgb;}
	    }
	    bgcolor2=new Color(R1,G1,B1);
	}    
    } 
    
    // plot a function
    public void plot(String S){
	remarks.setText("\u2023 function plot"); /* not really fine ; will need some work... */
	input.setText("? ");
	output.setText("\u2023 ?");
	if(S.indexOf("=") != -1){
	    S=S.substring(S.indexOf("=")+1);
	}
	//System.out.println(S);
	graphwindow.plotfunction(S);
	graphwindow.setVisible(true);
    }

    // the actual calculation starts here:
    public void calculate(String S){
	S=replace(S,"?","");
	S=RawMath(S);
	if(S.length()>1){
	    String remark="";
	    S=replace(S,",",".");
	    S=replace(S,"**","^");
	    S=S.toLowerCase();
	    S=replace(S,"sqr(","sqrt(");/* common error */
	    if(S.indexOf("sqrt[") != -1){S=sqrtn(S);} /* sqrt[3](4) = 4^(1/3) ... */
	    if(S.indexOf("log[") != -1){S=logn(S);} /* log[3](4) = log(4)/log(3) ... */
	    for(int s=0;s<3;s++){/* arity lib throws errors +-5-+3------23 etc */
		S=replace(S,"+-","-");
	        S=replace(S,"-+","-");
		S=replace(S,"--","+");
	        S=replace(S,"++","+");
	    }
	    if(S != null){
		if(S.indexOf("x")!=-1 || S.indexOf("y")!=-1 ){ /* function to plot */
		    plot(S);
		}
		else
		{ double result=0.0D;/* double is needed for Arity libs */
		    Symbols symbols=new Symbols(); /* Arity */
		    String ex[]=S.split("=");
		    String out="";
	    	    try { 
		    result=symbols.eval(ex[ex.length-1]); /* try to call arity  for evaluation */
		    if(result == Double.POSITIVE_INFINITY){out="\u221E";}
		    else
		    if(result == Double.NEGATIVE_INFINITY){out="-\u221E";}
		    else
		    if(result == Double.NaN){out="NaN (Not A Number)";}
		    else
		    if(result == Math.PI){out="-\u03C0";}
		    else
		    if(result == Math.E){out="e";}
		    else
		    out=truncate(result);
				
		    output.setText("\u2023 "+out);remarks.setText(rem4+remark);
		    } catch (SyntaxException e) {output.setText("\u2023 ?");remarks.setText(rem1+remark);}
		}
	    }
	    else
	    {
		output.setText(rem2);
	        remarks.setText(rem4+remark);
	    }        
	}
	else
	{
	    output.setText("\u2023 ");
	    remarks.setText(rem6);
	}
	input.setText("? ");
	input.setCaretPosition(2); /* first 2 positions are: ? [space] */
    }

    public String RawMath(String i){
	i=i.toLowerCase();
	i=i.replace(" ","");
	i=i.replace("**","^");
	if(i.indexOf("e+")!=-1){i=i.replace("e+","*10^");}
	if(i.indexOf("e-")!=-1){i=i.replace("e-","*10^-");}
	i=i.replace("**","*");
	String[] fun={"sin","cos","tan","log","ln","pi","e"};
	String[] cons={"pi","e","0","1","2","3","4","5","6","7","8","9"};
	for(int p=0;p<fun.length;p++){
	    for( int d=0;d<cons.length;d++){
		while(i.indexOf(cons[d]+""+fun[p])!=-1){
		    i=i.replace(cons[d]+""+fun[p],cons[d]+"*"+fun[p]);
		}
		while(i.indexOf(fun[p]+""+cons[d])!=-1){
		    i=i.replace(fun[p]+""+cons[d],fun[p]+"*"+cons[d]);
		}
	    }
	}
	if(i.indexOf("(")!=-1){
	    for(int p=0;p<cons.length;p++){
		if(i.indexOf(cons[p]+"(")!=-1){
		    i=i.replace(cons[p]+"(",cons[p]+"*(");
		}
		if(i.indexOf(")"+cons[p])!=-1){
		    i=i.replace(")"+cons[p],")*"+cons[p]);
		}
	    }
	    i=i.replace(")(",")*(");
	}
	if(i.indexOf("e")!=-1){
	    while(i.indexOf("e")!=-1){
		i=i.replace("e","2.718281828459045");
	    }
	}
	if(i.indexOf("pi")!=-1){
	    while(i.indexOf("pi")!=-1){
		i=i.replace("pi","3.141592653589793");
	    }
	}

	if(i.indexOf("log(")!=-1 || i.indexOf("ln(")!=-1){i=ConvertLog(i);}
	return i;
    }
    
    public String ConvertLog(String t){
	int begin=t.indexOf("log(");
	if(begin==-1){t=replace(t,"ln","log");return t;}
	int tot;int Got;int Wait;int End;char chr;int s;
	String value;String L;String R;
	while (begin >= 0){
	    begin=t.indexOf("log(");
	    tot=t.length();
	    Wait=0;Got=0;End=0;
	    for(s=begin+3;s<tot;s++){
		if(End==0){
		    chr=t.charAt(s);
		    if(chr=='('){Wait=Wait+1;}
		    if(chr==')'){Got=Got+1;}
		    if(Got!=0){if(Wait==Got){End=s;}}
		}
	    }
	    //System.out.println("begin="+begin+" end="+End);
	    
	    value=t.substring(begin+4,End);
	    L="log("+value+")";R="(@("+value+")/(@(10)))";
	    t=replace(t,L,R);
	}
	t=replace(t,"@","log");
	t=replace(t,"ln","log");
	//System.out.println("corrected log() -> log10() "+t);
	return t;
    }

    public void paint(Graphics g){
    	String S=input.getText();
	String SS=inputwindow.textarea.getText();
	if(SS.length() > S.length()){S=SS;inputwindow.textarea.setText("");}
	calculate(S);
    }

    public static String replace(String source, String pattern, String replace){ /* java 1.4 lacks decent replace() */
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
        else
	{
	    return "";
	}
    }

    public String ReadApplet(){ /* interface for sending the answer through javascript to WIMS */
	String reply=output.getText();
	String tmp[]=reply.split("\u2023");
	if( tmp[1].length() == 0){
	    remarks.setText(rem2); 
	    return "error";
	}
	else
	{
	    return " "+tmp[1];
	}
    }

    private String sqrtn(String t){/* function to replace sqrt[g](n) by n^(1/g) */
	int begin=t.indexOf("sqrt[");
	String org=t;
	int tot;int Got;int Wait;int End;char chr;int s;int restart;
	String value;String exp;String val;String L;String R;int cnt=0;
	while (begin >= 0){
	    cnt++;if(cnt>1000){remarks.setText("sqrt[]() syntax error");}
	    tot=t.length();Wait=0;Got=0;End=0;
	    for(s=begin+4;s<tot;s++){
		if(End==0){
		    chr=t.charAt(s);
		    if(chr=='['){Wait=Wait+1;}
		    if(chr==']'){Got=Got+1;}
		    if(Got!=0){if(Wait==Got){End=s;}}
		}
	    }
	    restart=End;    
	    exp=t.substring(begin+5,End);
	    Wait=0;Got=0;End=0;
	    for(s=restart;s<tot;s++){
		if(End==0){
		    chr=t.charAt(s);
		    if(chr=='('){Wait=Wait+1;}
		    if(chr==')'){Got=Got+1;}
		    if(Got!=0){if(Wait==Got){End=s;}}
		}
	    }
	    val=t.substring(restart+2,End);
	    R="("+val+")^(1/("+exp+"))";L="sqrt["+exp+"]("+val+")";
	    t=replace(t,L,R);
	    begin=t.indexOf("sqrt[");
	}
	return t;
    }

    private String logn(String t){/* function to replace sqrt[g](n) by n^(1/g) */
	int begin=t.indexOf("log[");
	String org=t;
	int tot;int Got;int Wait;int End;char chr;int s;int restart;
	String value;String exp;String val;String L;String R;int cnt=0;
	while (begin >= 0){
	    cnt++;if(cnt>1000){remarks.setText("log[]() syntax error");}
	    tot=t.length();Wait=0;Got=0;End=0;
	    for(s=begin+3;s<tot;s++){
		if(End==0){
		    chr=t.charAt(s);
		    if(chr=='['){Wait=Wait+1;}
		    if(chr==']'){Got=Got+1;}
		    if(Got!=0){if(Wait==Got){End=s;}}
		}
	    }
	    restart=End;    
	    exp=t.substring(begin+4,End);
	    Wait=0;Got=0;End=0;
	    for(s=restart;s<tot;s++){
		if(End==0){
		    chr=t.charAt(s);
		    if(chr=='('){Wait=Wait+1;}
		    if(chr==')'){Got=Got+1;}
		    if(Got!=0){if(Wait==Got){End=s;}}
		}
	    }
	    val=t.substring(restart+2,End);
	    R="(log("+val+"))/(log("+exp+"))";L="log["+exp+"]("+val+")";
	    t=replace(t,L,R);
	    begin=t.indexOf("log[");
	}
	return t;
    }

    public String getAppletInfo(){
	return "Java Calculator\n \u2023 using \"Arity\" library\n \u2023 normal arithmetics \n \u2023 sin(),cos(),tan(),sinh(),etc() \n \u2023 sqrt() sqrt[n]() log() \n \u2023 javascript communication [WIMS] \n \u2023 precision adjustable [\u00B1 "+precision+"]\nJ.M. Evers 10/2008";
    }

    public String truncate(double t){
	String r = decformat.format(precision*(Math.round(t/precision)));	
	return r;    
    }
    
    public void start(){}
    
    public void stop(){}

    public void run(){}
    

}




