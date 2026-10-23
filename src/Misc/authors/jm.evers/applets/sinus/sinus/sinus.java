/*
*********************************************************************************
* J.M. Evers 19/11/2009								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatoever								*
*********************************************************************************
 modified source of "j.-p. Quelen"
 jdk1.2 - double-buffering
---------------------------------------------------------
<html>
    <body>
	<script language="javascript" type="text/javascript">
	    function sendanswer(){
		var data=document.applets[0].ReadApplet();
		var test=data.indexOf("error");
		if(test!=-1){alert("Your answer will not be send...\nthe next objects could not be detected in your drawing...\n\n:"+data); return;}
		else{
		   var answer=data.split(',');
		   alert("technical answer: \n"+answer[0]);
		   alert("unicode answer: \n"+answer[1]);
		   alert("unicode answer: \n"+answer[2]); 
		    <!-- confirm("Send this ?\n"+data);
			if(t==true){
			    data=encodeURIComponent(data);
			    var URL = '$wims_ref_name?session=$session&+moudule=$module&+cmd=reply&+reply$n='+data ;document.location.href = URL
		    -->
		    }
		}
	    }                                                                                                                                          
	</script>
	<p align=center>
	    <applet id="sinus" codebase="build" archive="sinus.jar" code="sinus.class" WIDTH="800" HEIGHT="400" ALIGN="CENTER" MAYSCRIPT>
		<param name="language" value="nl">
		<param name="applettext" value="a string">
		<param name="degrees" value="rad">
	        <param name="functiontype" value="sin">
	        <param name="show_curve" value="yes">
		<param name="start" value="180">
	        <param name="end" value="720">
	        <param name="Xaxis_units" value="yes">
		No java
	    </applet>
	    <br> 
	    <input type="button" onclick="javascript:sendanswer()" name="TEST" value="TEST TEST">
	</p>
    </body>
</html>
---------------------------------------------------------
reply string = degrees+","+radialen+"\u03C0"+","+degrees+"\u00B0";
item 1 = the answer in degrees
item 2 = the unicode view answer in radians
item 3 = the unicode view answer in degrees
---------------------------------------------------------
<param name="language" value="nl/fr/en"> default en
<param name="applettext" value="a string">
<param name="show_curve" value="yes/no">
<param name="type" value="sin/cos"> default sin
<param name="degrees" value="yes/rad/no"> default yes or radians or no
<param name="Xaxis_units" value="yes/no"> default yes using degrees or radians depending on "param degrees"
<param name="start" value="begin value of point P in degrees"> default 0
<param name="end" value="end value of point P in degrees"> default no value

attention: no "try / catch" is applied to the params ! 

*/



import java.awt.event.* ;
import java.awt.* ;
import java.applet.* ;
import geo.* ;

public class sinus extends java.applet.Applet implements MouseListener, MouseMotionListener{
    Image img ;Graphics g ;Graphics g1 ;
    Repere R, R1 ;
    pt O,P,Px,Py,Pcosx,endpoint1,endpoint2,pp1,pp2;
    PointSurDroite O1;
    Cercle C, Cenr ;
    ArcDeCercle IM ;
    Droite xas, yas ,hline;
    PointSurCercle M ;
    Vecteur Endpoint;
    Segment straal,OPx,OPy,value,proj ;
    CourbeDeFonction functie ; sin sfunctie ;cos cfunctie;
    int aNp, periodes = 0 ;double ap = 1.0, pi2 ;int X, Y, Xret, Yret ;int modepaint = -1 ;
    double degrees;double absdegrees=0;int use_degrees=1;double start=0;double end=0;int type=0;
    int xz;int yz;double scale=7.0;double minscale=7.0;int use_end=0;int units=1;int curve=1;
    double yP;String rem1="Only point P [on the circle] can be rotated...";String rem2="zooming out...";String rem3="\u03C0 radians";
    String rem4="the angle = ";String rem5=" The unit circle";
    
    
    public void init(){
	String param;
	param=getParameter("language");if(param!=null && param.length()>0){
	    if(param.equalsIgnoreCase("nl")){
		rem1="Alleen het punt P [op de cirkel] kan worden bewogen...";
		rem2="uitzoomen...";
		rem3="\u03C0 radialen";
		rem4="de hoek = ";
		rem5=" De eenheids cirkel";
	    }
	    if(param.equalsIgnoreCase("fr")){
		rem1="Seulement point P [sur le cercle] peut être tourné ...";
		rem2="zoomer en plan général...";
		rem3="\u03C0 radians";
	        rem4="angle = ";
		rem5=" Cercle unité";
	    }
	}
	param=getParameter("functiontype");if(param!=null && param.length()>0){if(param.equalsIgnoreCase("cos")){type=1;}}
	param=getParameter("show_curve");if(param!=null && param.length()>0){if(param.equalsIgnoreCase("no")){curve=0;scale=4;minscale=4;}}
	param=getParameter("Xaxis_units") ;if(param!=null && param.length()>0){if(param.equalsIgnoreCase("no" )){units=0;}}
        param=getParameter("degrees");
	if(param!=null && param.length()>0){
	    if(param.equalsIgnoreCase("no")){
		use_degrees=0;
	    }
	    else
	    {	
		if(param.equalsIgnoreCase("radians") || param.equalsIgnoreCase("rad") ){use_degrees=2;}
	    }
	}
        param=getParameter("start");if(param!=null && param.length()>0){start=(double) Integer.parseInt(param, 10);  }
        param=getParameter("end");if(param!=null && param.length()>0){end=(double) Integer.parseInt(param, 10); use_end=1;}	
	setBackground(Color.white);modepaint = 1 ;
	periodes = 0 ;
	pi2 = Math.PI * 2.0;
	img = createImage(getSize().width, getSize().height);g = img.getGraphics();
	addMouseMotionListener(this);addMouseListener(this);
	xz=getSize().width/2;yz=getSize().height/2;
	R   = new Repere(xz,yz, getSize().width, getSize().height,(double)(getSize().height / 2 - 20),(double)(getSize().height / 2 - 20));
	R1  = new Repere(xz,yz, getSize().width, getSize().height,getSize().width/scale,getSize().width/scale);
	xas = new Droite(0.0, 1.0, 0.0, "", R1);
	yas = new Droite(1.0, 0.0, 0.0, "", R1);
	O1  = new PointSurDroite(xz,yz, xas, "O", R1);
	O   = new PointSurDroite(xz,yz, xas, "O", R1);
	C   = new Cercle(O1, 1.0, "", R1);
	Cenr= new Cercle(O1, 1.0, "", R1);
	M   = new PointSurCercle(start*pi2/360, C, "P", R1);
	straal= new Segment(O1.x,O1.y,M.x,M.y,"",R1);
	Px  = new point(M.x,0, "P'", R1);
	Py  = new point(0,M.y, "P'", R1);
	proj= new Segment(Px.x,Px.y,M.x,M.y,"",R1);
	OPx= new Segment(O1.x,O1.y,M.x,0,"cos",R1);
	OPy= new Segment(O1.x,O1.y,0,M.y,"sin",R1);
	IM = new ArcDeCercle(0.0, 0.0, 1.0, 0.0, 1.0, "", R1);
	if(type==0){
	    P = new pt(O1.x+1.0,M.y, "", R1);
	    sfunctie = new sin();
	    functie = new CourbeDeFonction(sfunctie, "", R1);;
	}
	else
	{
	    P = new pt(O1.y + 1.0, M.y, "", R1);
	    cfunctie = new cos();
	    functie = new CourbeDeFonction(cfunctie, "", R1);
	}
	
	hline=new Droite(O1.x,O1.y,M.x,M.y,"",R1);
	if(use_end==1){
    	    endpoint2 = new point(end*pi2/360,0.0,"",R1);
    	    endpoint1 = new point(end*pi2/360,getSize().height,"",R1);
	    Endpoint = new Vecteur(endpoint1,endpoint2, "end",R1);
	}
	Pcosx  = new point(P.x,0, "P''", R1);
	value= new Segment(P.x,P.y,P.x,0,"",R1);
    }
    
    

    public void paint(Graphics g1){
	Font f = new Font("Arial", Font.PLAIN, 10);g.setFont(f);g.setColor(getBackground());g.fillRect(0, 0, R1.XMAX, R1.YMAX);
	double Ma = M.a + pi2 *(double)(periodes);degrees=Ma*(360/(pi2));double Maa = M.a ;aNp = Math.abs(periodes);
	if((M.a < 0.0) &&(periodes > 0)){ aNp -- ;Maa = Maa + pi2 ;} else { if((M.a > 0.0) &&(periodes < 0)){ aNp -- ;Maa = Maa - pi2 ;}}
	g.setColor(Color.gray);
	for(int i = 0 ; i < aNp ; i ++){ Cenr.Cercle(0.0, 0.0, 1.0 + R1.Abs(i + R1.X0));Cenr.trace(g); }
	IM.trace(g); IM.ArcDeCercle(0.0, 0.0, 1.0 + R1.Abs(aNp + R1.X0), 0.0, Maa);
	g.setColor(Color.gray);	xas.trace(g);yas.trace(g);
	g.setColor(Color.green);
	straal.Segment(O1,M);straal.trace(g);
	if(type==0){
	    g.setColor(Color.red);
	    Py.point(0,M.y);Py.trace(g);Py.traceNom(g);
	    OPy.Segment(O1,Py);OPy.trace(g);
	    proj.Segment(M,Py);proj.trace(g);
	}
	else
	{
	    g.setColor(Color.red);
	    Px.point(M.x,0);Px.trace(g);Px.traceNom(g);
	    OPx.Segment(O1,Px);OPx.trace(g);
	    g.setColor(Color.green);
	    proj.Segment(M,Px);proj.trace(g);
	}
	if(curve==1){
	    if(units==1 && aNp<10){
	        g.setColor(Color.gray);
		if(use_degrees==1){
		    for(int i=1; i<(aNp+4);i++){
			pp1=new point(i*pi2,0.0,(i*360)+"\u00B0",R1);
		        pp2=new point(-1.0*i*pi2,0.0,"-"+(i*360)+"\u00B0",R1);
			pp1.trace(g);pp2.trace(g);	    
		    }	    
		}
		else
		{
		    for(int i=1; i<(aNp+4);i++){
			pp1=new point(i*pi2,0.0,2*i+"\u03C0",R1);
			pp2=new point(-1.0*i*pi2,0.0,"-"+2*i+"\u03C0",R1);
			pp1.trace(g);pp2.trace(g);	    
		    }
		}
	    }
	    g.setColor(Color.red);
    	    if(type==1){
		yP=(double) Math.cos(O1.x + Ma);
		P.point(O1.x + Ma, yP);
	    }
	    else
	    {
		yP=(double) Math.sin(O1.x + Ma);
		P.point(O1.x + Ma, yP);
		hline.Droite(P,M);hline.trace(g);
	    }
	    Pcosx.point(P.x,0);Pcosx.trace(g);
	    value.Segment(Pcosx,P);value.trace(g);
	    if(use_end==1){g.setColor(Color.green);Endpoint.trace(g);}
	    g.setColor(Color.orange);
	    functie.trace(g);
	}
	g.setColor(Color.black);
	g.drawString(rem5,getSize().width/2,20);
	if(use_degrees==1){g.drawString(rem4+(double)(Math.round(10*degrees))/10 +"\u00B0",getSize().width/2, getSize().height - 30);}
	if(use_degrees==2){g.drawString(rem4+(double)(Math.round(1000*degrees/180))/1000 +rem3,getSize().width/2, getSize().height - 30);}
	if(absdegrees>180){g.drawString(rem2,getSize().width/2, getSize().height - 50);}
	C.trace(g);
	R1.cadre(g);
	P.trace(g);P.traceNom(g);
	if(modepaint==2){
	    g.setColor(Color.red);
	    g.drawString(rem1,getSize().width/2, getSize().height - 40);
	}
	if(modepaint==0){g.setColor(Color.red);}
	M.trace(g);
	g1.drawImage(img, 0, 0, this);
  }

  public void mousePressed(MouseEvent e){
    e.consume();
    if( M.zone(e.getX(), e.getY()) ){ 
	modepaint = 0 ;
	X = -1 ;
	repaint();
    }
    else
    {
	modepaint=2;
    }
  }

    public void update(Graphics g1){paint(g1);}

  public void mouseDragged(MouseEvent e)
  { e.consume();
    M.bouge(e.getX(), e.getY());
    if(M.a * ap < 0.0){
	if(ap > 2.0){
	    periodes ++ ;
	}
	else 
        {
	    if(ap < -2.0){
		periodes -- ;
	    }
	}
    }
    ap = M.a ;
    if(curve==1){
	absdegrees=Math.abs(degrees);
	if(absdegrees>180){scale=(absdegrees/180)*minscale;}
	R1.Repere(xz,yz, getSize().width, getSize().height,getSize().width/scale,getSize().width/scale);
    }
    O1.bouge(e.getX(), e.getY());
    repaint();
  }

  public void mouseReleased(MouseEvent e)
  { e.consume();
    M.deplace = O1.deplace = false ; modepaint = 1 ;
    repaint();
  }

  public void mouseMoved(MouseEvent e){
    e.consume();
    if(modepaint == 1){ 
	if( M.zone(e.getX(), e.getY())){ 
	    this.X = e.getX() + 2 ; this.Y = e.getY() + 2 ;
	    M.deplace = false ;
	}
	Xret = e.getX(); Yret = e.getY();
	repaint();
    }
  }

  public void mouseClicked(MouseEvent e) { }
  public void mouseEntered(MouseEvent e) { }
  public void mouseExited(MouseEvent e) { }

  public String getAppletInfo()
  { return "Written by J.P. Quelen\nModified for WIMS \n J.M. Evers 3/2008" ; }

    protected class sin extends Fonction{
	public boolean defini(double x){ return true ; }
	public double Image(double x){ return Math.sin(x); }
    }

    protected class cos extends Fonction{
	public boolean defini(double x){ return true ; }
	public double Image(double x){ return Math.cos(x); }
    }
    
    // reading the degrees value of the applet by javascript
    public String ReadApplet(){
	double radialen=(Math.round(100.0*degrees/180.0))/100.0;
	double deg=Math.round(degrees);
	String reply=degrees+","+rem4+radialen+rem3+","+rem4+deg+"\u00B0";
	return reply;
    }
    
    public void start(){}
    public void stop(){}
    public void destroy(){}
}
