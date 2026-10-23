/*
*********************************************************************************
* J.M. Evers 19/11/2009								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatoever								*
*********************************************************************************

Example of usage:

<html>
<head></head>
<body>
    <script language="javascript" type="text/javascript">                                                                                      
	function readthis(){                                                                                                                           
	    var input=document.applets[0].ReadApplet(); 
	    if(input.indexOf(error)!=-1){
		alert(input);
	    }
	    else
	    {                                                                          
		alert("we will send to WIMS:"+input);
	    }
	}                                                                                                                                         
    </script>                                                                                                                                  
    <center>
    <applet code="NumberLine.class" width=600 height=240>
    <!--- no params needed ; defaults are fine --->
	<param name="xmax" value="50">
	<param name="fontsize" value="16">
	<param name="join" value="no">
	<param name="language" value="nl">
	<param name="pencolor" value="0,0,255><!--- input field foreground color --->
	<param name="bgcolor1" value="255,255,255><!--- input field background color --->
	<param name="bgcolor2" value="255:255;255"><!--- canvas color : may conflict with arrow colors...--->
	<param name="exercise" value="an exercise, if not given in a wims phml page">
    </applet>
    <input type="button" name=".....TEST......" value=".....TEST....." onclick="javascript:readthis();">
</body>
</html>

the WIMS reply type format:
 
boolean,"verbatim reply"
true,123-23=100
false,123-23=99
false,error \n multiple = -signs [e.g. 123-23=100-0=100]
false,error \n mathematical error [Arity error exception]

todo: dynamic scaling of xmax/xmin/step [with mouse drag ?/]

Arity usage example: 
fun="sin(2*pi/2)"
fun="(1.2345678-2.345678)/12345678"
try { y=symbols.eval(fun);} catch (SyntaxException e) {System.out.println("error "+e); } 
*/


import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.util.*;
import org.javia.arity.*;


public class GetallenLijn extends Applet implements KeyListener{
    
    int xmax=100;int xmin=-100;
    int xmax_org=xmax;int xmin_org=xmin;
    double step=3.0D;int x_pix=600;int y_pix=240;int x_nul=300;int y_nul=60;
    int font_size=36;int minmax=200;boolean start= false;
    String op[]={"+","-"};
    Graphics g;int vectors=0;
    boolean join = false;
    boolean conclusion_drawn=false;
    int arrowdistance=10;// moet in verhouding met hoogte...
    private TextField input;
    double fragment[];
    int now=0;
    int kleuren[][]={ {255,0,0},{0,255,0},{0,0,255},{238,154,0},{255,255,0},{160,32,240},{144,238,144},{173,216,230},{0,255,255},{165,24,24},{250,128,114},{255,192,203} };
    // 11 kleuren ... zie changecolor()
    Color arrowcolor;
    Color pencolor;
    int R1=225;int G1=255;int B1=255;Color bgcolor1;
    int R2=255;int G2=255;int B2=255;Color bgcolor2;
    // java 1.4 can not use replaceAll(); 
    String illegalchars[]={"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","y","z","(",")","{","}","[","]","@","#","~","!","$","%","^","&","_",";",">","<","|"};
    // 46 items : prepare();
    String cijfers[]={"0","1","2","3","4","5","6","7","8","9"};
    double multiplication[];
    String param;String title;String exercise="";String inputtext;
    String rem1="Error: wrong answer type (multiple = -signs)";
    String rem2="Mathematical error";
    String rem3="Error: = sign forgotten";
    double tmpsum=1.0D;

    
    public void init(){
	Font myfont = new Font("Helvetica", Font.BOLD, font_size);
	setLayout(new GridLayout(4, 1));
	input=new TextField(40);
	input.setFont(myfont);add(input);setVisible(true);
	arrowcolor=new Color(kleuren[now][0],kleuren[now][1],kleuren[now][2]);
	param=getParameter("language");
	if(param!=null && param.length()>0){
	    if(param.equalsIgnoreCase("nl")){
		rem1="Fout:verkeerd type antwoord (meerdere = -tekens)";
		rem2="Wiskundige fout";
		rem3="Fout:je hebt het = teken vergeten";
	    }
	    if(param.equalsIgnoreCase("de")){
		rem1="Fehler";
		rem2="Mathematik Fehler";
		rem3="Fehler: = Zeichen vergessen";
	    }
	    if(param.equalsIgnoreCase("fr")){
		rem1="Erreur";
		rem2="Erreur mathematique";
		rem3="Erreur: vous avez oublié d'utiliser un = signe";
	    }
	}
	param=getParameter("title");
	if(param!=null){title=param;}else{title="";}
	param=getParameter("xmax");
	if(param!=null && param.length()>0 ){xmax = Integer.parseInt(param, 10);}
//	param=getParameter("xmin");
//	if(param!=null){xmin = Integer.parseInt(param, 10);}
	param=getParameter("fontsize");
	if(param!=null && param.length()>0){font_size = Integer.parseInt(param, 10);}
	param=getParameter("join");
	if(param!=null){if(param.equals("1") || param.equalsIgnoreCase("yes")){join = true;}}
	param=getParameter("exercise");
	if(param!=null && param.length()>0){exercise = param; input.setText(exercise);}
	param=getParameter("pencolor"); // foreground color of inputfield "input"
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
	    pencolor=new Color(R1,G1,B1);
	    input.setForeground(pencolor);
	}

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
	    input.setBackground(bgcolor1);
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
		if(a == 0){R2 = rgb;}
		else if(a == 1){G2 = rgb;}
		else if(a == 2){B2 = rgb;}
	    }
	    bgcolor2=new Color(R2,G2,B2);
	}
	// needs changing !
	xmin=-1*xmax;
	xmax_org=xmax;
	xmin_org=xmin;
	minmax=Math.abs(xmax-xmin);
	if(minmax == 0){xmax=25;xmin=-25;minmax=50;}
	x_pix = getSize().width;
	y_pix = getSize().height;
	y_nul=(int)y_pix/4;
	// xnul is at center for now
	x_nul =(int) ((xmax-xmin)/2)*x_pix/(minmax); // x_nul is positie  op graphics in pixels
	step=(double)x_pix/(minmax);
	input.requestFocus();
	input.addKeyListener(this);
	prepare();
    }
    public void keyPressed(KeyEvent e){}
    public void keyTyped(KeyEvent e ){}
    public void keyReleased(KeyEvent e){ prepare();}
   
    public void prepare(){
	inputtext=input.getText();
	int len=inputtext.length();
	if(len>0){
	    boolean found = false; String last; 
	    inputtext=inputtext.toLowerCase();
	    for(int i=0;i<44;i++){
		inputtext=replace(inputtext,illegalchars[i],"");
	    }
	    inputtext=replace(inputtext,",",".");
	    inputtext=replace(inputtext,":","/");
	    inputtext=replace(inputtext,"*","\u2217");
	    inputtext=replace(inputtext,"x","\u2217");
    	    input.setText(inputtext);
	    len=inputtext.length();
	    last = inputtext.substring(inputtext.length()-1,inputtext.length());
	    for(int i=0;i<10;i++){
		if(last.equals(cijfers[i])){ found = true;}
	    }
	    if( found ){convert();}else{repaint();} 
	}
	input.setCaretPosition(len);
	input.requestFocus();    
    }
    
    public void changecolor(){
	now++;// 11 kleuren, niet meer...
	if( now > 11 ){ now = (int) (11*(Math.random())); }
	arrowcolor=new Color(kleuren[now][0],kleuren[now][1],kleuren[now][2]);
    }
    
    public void convert(){
	// arity lib throws errors +-5-+3------23 etc
	Symbols symbols=new Symbols();
	String S=input.getText();
	for(int s=0;s<10;s++){
	    S=replace(S,"+-","-");
	    S=replace(S,"-+","-");
	    S=replace(S,"--","+");
	    S=replace(S,"++","+");
	}
	S=replace(S,"/","@1/");
	S=replace(S,"+","#+");
	S=replace(S,"-","#-");
	S=replace(S,"==","=");
	S=replace(S,"=","#=#");
	S=replace(S,"\u2217#+","@");	
	S=replace(S,"\u2217#-","@-");	
	S=replace(S,"\u2217","@");
	//System.out.println("S in nu:"+S); 
	int L=S.length();
	if(S != null || L > 0){
	    String tmp1[]=S.split("#");int n=0;int sign=1;double t1=1D;double t2=1D;
	    fragment =new double[L];
	    multiplication = new double[L];
	    int idx;
	    tmpsum=0;
	    for(int i=0;i<tmp1.length;i++){
		if(tmp1[i].length()!=0){
		    if(tmp1[i].indexOf("=")!= -1){
			multiplication[n]=-1;
			fragment[n]=1.23456789;n++;
		    }
		    else
		    {
			if(tmp1[i].indexOf("@")!=-1){
			    String tmp2[]=tmp1[i].split("@");
			    t1=Double.parseDouble(replace(tmp2[0],"+",""));
			    tmp2[1]=replace(tmp2[1],"--","");
			    if(tmp2[1].indexOf("/")!=-1){
				try { 
				    t2=symbols.eval(replace(tmp2[1],"+",""));
				} catch (SyntaxException e) {
				    input.setText("?");System.out.println("error "+e+"\n caused by "+fragment[n]);
				    return; 
				}
			    }
			    else
			    {
				t2=Double.parseDouble(replace(tmp2[1],"+",""));
			    }
			    if(t1<0){sign=-1;}else{sign=1;} // -5*3 -> -1*3 -1*3 -1*3 -1*3 -1*3
			    t1=Math.abs(t1);
			    for(int p=0;p<t1;p++){
				if(n >= L){
				    idx=(int)(t1)+n;
				    L=idx;
				    fragment = GrowArray(fragment,idx,n);
				    multiplication = GrowArray(multiplication,idx,n);
    
				}
				multiplication[n]=1;
				fragment[n]=sign*t2;n++;
			    }
			}
			else
			{
			    multiplication[n] = 0;
			    try { fragment[n]=symbols.eval(tmp1[i]);n++;} catch (SyntaxException e) {input.setText("?");System.out.println("error "+e+"\n caused by "+fragment[n]); } 
			}
		    }
		    if(join){
			tmpsum=tmpsum+(fragment[n-1]);
		    }else{
//			if((Math.abs(fragment[n-1]))>(Math.abs(tmpsum))){tmpsum=Math.abs(fragment[n-1]);}
			if((Math.abs(fragment[n]))>(Math.abs(tmpsum))){tmpsum=Math.abs(fragment[n]);}
		    }
		}
	    }
	    start=true;
	    tmpsum=Math.abs(tmpsum);
	    if(tmpsum>1000){tmpsum=1000;}
	    double tmp;
	    if(tmpsum<100){
		tmp=Math.ceil(tmpsum/10);
		xmax=10*((int)tmp);
	    }
	    else
	    {
		tmp=Math.ceil(tmpsum/100);
		xmax=100*(int)(tmp);
	    }
	    if(xmax == 0){xmax = 10;}
	    xmin=-1*xmax;// let op nog steeds x=0 symmetrisch op x_pix/2
	    repaint();
	}
    }
        
    public void paint(Graphics g){
	step=(double)x_pix/(xmax - xmin);  
	if(step == 0){step=1;}
	now=0;int y=y_nul+vectors+60;double x1=0.0D;double x2=0.0D;double previous=0.0D;
	g.setColor(bgcolor2);g.fillRect(0,y_nul,x_pix,y_pix);
	if(start){
	    for(int p=0;p<fragment.length;p++){
		x2=fragment[p];
	        if(multiplication[p] < 1){changecolor();}else{arrowcolor=new Color(255,30,30);}
		if(x2 != 0.0){
		    if(x2 == 1.23456789){
			y=y+arrowdistance;
		        tekenconclusie( 0, y , x_pix , y, g);
		        y=y+2*arrowdistance;
			previous=x_nul;
		        x2=fragment[p+1];p++;
			conclusion_drawn=true;
		    }
		    if(join){
			if( p == 0){
			    x1=(double) x_nul;
			    x2=x1+(x2*step);
			    previous=x2;
			}
			else
			{
			    x1=previous;
			    if(x1 == 0.0){x1=x_nul;}
			    x2=x1+(x2*step);
			    previous=x2;
			}
		    }
		    else
		    {
			x1=x_nul;
		        x2=x1+(x2*step);
		    }
		    tekenVector( (int)x1, y , (int)x2 , y, conclusion_drawn, g);
		    //System.out.println("x1="+x1+" x2="+x2+" x_nul="+x_nul);
		    y=y+arrowdistance;
		}
	    }
	}
	drawSchaal(g);
	input.requestFocus();
    }
	
    public void drawSchaal(Graphics g){
	Graphics2D g2 = (Graphics2D) g;
	g2.setStroke( new BasicStroke(1.1f) );
	g2.setColor(Color.black);
	int t=0;int sc;
	int sc1=(int)(Math.abs(xmin));
	int sc2=(int)(Math.abs(xmax));
	if(sc1 > sc2){sc=sc1;}else{sc=sc2;}
	int[] i=new int[3];
	int[] s=new int[3];
	s[0]=30;
	s[1]=20;
	s[2]=10;
	if(sc>0 && sc<11){
	    i[0]=5;
	    i[1]=1;
	    i[2]=1;
	}
	else
	{
	    if(sc>10 && sc<51){
		i[0]=10;
		i[1]=5;
		i[2]=1;
	    }
	    else
	    {
		if(sc>50 && sc<101){
		    i[0]=20;
		    i[1]=10;
		    i[2]=2;
		
		}
		else
		{
		    if(sc>100 && sc<501){
			i[0]=50;
		        i[1]=25;
		        i[2]=5;
		    }
		    else
		    { // sc<1000
			i[0]=100;
		        i[1]=50;
			i[2]=25;
		    }
		}
	    }
	}	
	
	if(xmax>0){
	    for(int r=0;r<3;r++){
		for(int p=0;p<xmax;p=p+i[r]){
		    t=(int) ((p*x_pix/(xmax - xmin))  - xmin*x_pix/(xmax - xmin));
		    g2.drawLine(t,y_nul,t,y_nul+s[r]);
		    if(r==0){
			g2.drawString(""+p+"",t-(g.getFontMetrics().stringWidth(" "+p+" "))/2,y_nul+50);
		    }
		}
	    }
	}
	if(xmin<0){
	    for(int r=0;r<3;r++){
		for(int p=0;p>xmin;p=p-i[r]){
		    t=(int) ((p*x_pix/(xmax - xmin))  - xmin*x_pix/(xmax - xmin));
		    g2.drawLine(t,y_nul,t,y_nul+s[r]);
		    if(r==0){
			g2.drawString(""+p+"",t-(g.getFontMetrics().stringWidth(" "+p+" "))/2,y_nul+50);
		    }
		}
	    }
	}
    }

    public void tekenVector(int x1, int y1, int x2, int y2,  boolean con ,Graphics g ){
	double d = Math.atan((float)((double)(y1 - y2) / (double)(x1 - x2)));
	Graphics2D g2 = (Graphics2D) g;
	g2.setColor(arrowcolor);
	if(Math.abs(x2-x1) < step*10){g2.setStroke( new BasicStroke(2.0f) );}else{g2.setStroke( new BasicStroke(3.0f) );}
	if(x1 - x2 < 0){d += 3.1415926535897931D;}
	g2.drawLine(x1, y1, x2, y2);
	g2.drawLine(x2, y2, x2 + (int)(Math.cos(d + 0.6D) * 10D), y2 + (int)(Math.sin(d + 0.6D) * 10D)); 
	g2.drawLine(x2, y2, x2 + (int)(Math.cos(d - 0.6D) * 10D), y2 + (int)(Math.sin(d - 0.6D) * 10D));
	if(con){//we have drawn a dashed conclusion line : no we mark the and of the final ? arrow with a vertical line
	    g2.setStroke( new BasicStroke(4f) );
	    g2.setColor(Color.red);
	    g2.drawLine(x2,0,x2,y2);
	    conclusion_drawn=false;
	}

    } 

    public void tekenconclusie(int x1, int y1, int x2, int y2, Graphics g){
	Graphics2D g2 = (Graphics2D) g;
	g2.setStroke(dashed);
	g2.setColor(Color.black);
	g2.drawLine(x1, y1, x2, y2);
    }
    
    public static int[] Shuffle(int[] S){
	int ll= S.length;
	for (int i=0;i<ll;i++){
	    int r = (int) (Math.random() * (i+1));
	    int swap = S[r];
	    S[r] = S[i];
	    S[i] = swap;
	}  
	return S;  
    } 
     
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
    
    final static float dash1[] = {10.0f};
    
    final static BasicStroke dashed = new BasicStroke(1.0f, 
                                          BasicStroke.CAP_BUTT, 
                                          BasicStroke.JOIN_MITER, 
                                          10.0f, dash1, 0.0f);

    public String ReadApplet(){
	Symbols symbols=new Symbols();
	String reply=input.getText();
	reply=replace(reply,"\u2217","*");
	String con=reply;
	for(int s=0;s<10;s++){
	    con=replace(con,"+-","-");
	    con=replace(con,"-+","-");
	    con=replace(con,"--","+");
	    con=replace(con,"++","+");
	}
	String tmp[]=con.split("=");
	// answer must be: 123 - 23 = 100 ...so a single = sign is expected
	double r1=0.0;double r2=1.0;boolean answer = false;
	if(tmp.length < 2){
	    reply="error \n"+rem3;input.setText(rem3);
	}
	else
	{
	    if(tmp.length > 2){
		reply="error \n"+rem1;input.setText(rem1);
	    }
	    else
	    {
		String tmp0=tmp[0];// trouble with +5-4=+1
		String tmp1=tmp[1];
		if(tmp0.charAt(0)=='+'){tmp0=tmp0.substring(1,tmp0.length());}
		if(tmp1.charAt(0)=='+'){tmp1=tmp1.substring(1,tmp1.length());}
		try { r1=symbols.eval(tmp0);} catch (SyntaxException e) {input.setText(rem2);reply="error \n"+rem2;} 
		try { r2=symbols.eval(tmp1);} catch (SyntaxException e) {input.setText(rem2);reply="error \n"+rem2;}
		if(r1 == r2){ answer = true;}else{ answer = false;}
	    }
	}
	return answer+","+reply;
    }
    
    public double[] GrowArray(double array[], int newlength ,int oldlength){
	double[] grow;
	grow = new double[ newlength ];
	for(int i=0; i < oldlength; i++){
	    grow[i] = array[i];
	    //System.out.println("grow[p]="+grow[i]);
	}
	return grow;
    }    
    public void start(){}
    
    public void stop(){}

    public void run(){}
	
}

