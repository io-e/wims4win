/* B. Perrin-Riou & J.M.Evers 17/2/2009 
*********************************************************************************
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatsoever 							*
*********************************************************************************

 Example html page:
<html>
    <body>
	<script language="javascript" type="text/javascript">
	    function readthis(){
 		var input=document.getElementById("Clicktile").ReadApplet(); 
 		alert(input);
 	    }
	</script>
	
	<!-- INTERNAL COLOR NAMES FOR PARAMS -->
	<!-- "white","red","green","blue" -->
	<!-- "orange","yellow","purple","lightgreen" -->
	<!-- "lightblue","cyan","brown","salmon","pink,"black" -->
	<applet id="Clicktile" code="Clicktile.class" codebase="." archive="Clicktile.jar" width="500" height="500">
	    
	    <!-- status = done : display non-clickable image -->
	    <param name="status" value="waiting">
	    
	    <!-- choose from the 13 colors -->
	    <param name="point_color" value="white">

	    <!-- use only one at a time : leave others blank -->
	    <param name="point" value="1:1,2:2,3:3,4:4,5:5,6:6,7:7">
	    <param name="segment" value="">
	    <param name="polygon" value="">
	    <param name="line" value="">
	    <!-- segment and line are the same...for now -->
	    <!-- but a line is a segment with 2 points outside xrange/yrange -->
    
	    <!-- for now only usefull in point param: default 5 -->
	    <param name="linewidth" value="20">

	    <!-- if "1" or "yes" all objects including given drawing will be returned -->
	    <param name="return_all_objects" value="0">

	    <!-- en ,de , fr ,nl  default en -->
	    <param name="language" value="fr">
    	    <param name="xrange" value="-11,11">
    	    <param name="yrange" value="-11,11">    
    	    <param name="background_color" value="black">

	    <!-- GIVEN DRAWING  : not more than 13 colors allowed --> 
	    <param name="square1" value="4:1,4:2,4:3,4:4,5:4,6:4,8:4,8:3,8:2,8:1,5:1,6:1,7:1">
	    <!-- limited only by xrange/yrange : coordinates of rectangles e.g. predefined square [teacher] -->
    	    <param name="square1_color" value="blue">
	    <param name="square2" value="-4:1,-4:2,-4:3,-4:4,-5:4,-6:4,-8:4,-8:3,-8:2,-8:1,-5:1,-6:1,-7:1">
	    <!-- limited only by xrange/yrange : coordinates of rectangles e.g. predefined square [teacher] -->
    	    <param name="square2_color" value="yellow">
    	 can add some image (must be transparent)
    	 <param name="image" value="http://...">
    	 <param name="copy" value="0,0"> 
    	 <!-- coordinates in pixels of the left top corner-->
	    NO JAVA INSTALLED ?
	</applet>

	<p>
	<input type="button" name=".....TEST......" value=".....TEST....." onclick="javascript:readthis();">
    </body>
</html>
*/

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.Math;
import java.net.*;

public class Clicktile extends Applet implements Runnable,MouseListener, MouseMotionListener, MouseWheelListener{
    private static final int serialVersionUID = 1;
    int Clicktile[][];int MAX;
    int dx;int dy;Thread thread = null;
    Image canvas;Graphics2D drawing;
    int xmin=-10,xmax=10,ymin=-10,ymax=10;// xmin/xmax ymin/ymax is coordinate system... xrange/yrange
    int xsize,ysize;// xsize,ysize is canvas-size
    int x_rect,y_rect;// x_rect * y_rect is pixel x/y-size of an elementary rectangle
    int Rx,Ry;// Rx * Ry is amount of elementary rectangles on canvas
    int palette[][] = {{255,255,255},{255,0,0},{0,255,0},{0,0,255},{238,154,0},{255,255,0},{160,32,240},{144,238,144},{173,216,230},{0,255,255},{165,24,24},{250,128,114},{255,192,203},{0,0,0}};
    final int max=palette.length;// 14 different colors...arraylength=13
    int[] color = new int[max];// color[0] is background color
    int objects=0;int[][] xcoords;int[][] ycoords;int[] length;
    public String COLORS[] = new String[max];
    int linewidth=1;int TYPE=0;int[] xpoints;int[] ypoints;int point_color=0;
    String language="en";boolean status=true;
    int[] used_colors;int this_color=0;
    Image bg;URL url;int copy_x=0 ; int copy_y=0 ; 
    
    public void init(){
	String c;
	xsize = getSize().width;ysize = getSize().height;
	c=getParameter("status");
	if(c != null && c.length()>0){
	    if(c.equalsIgnoreCase("waiting")) status = true;
	    else
	    if(c.equalsIgnoreCase("done")) status = false;
	}
	c=getParameter("language");
	if(c != null && c.length()>0){
	    Determine_Color_Names(c); // en: COLOR[0]="white" fr: COLOR[0]="blanc"
	}
	c=getParameter("background_color");
	if(c != null && c.length()>0){
	    color[0]=GetInternalColorCode( c , 0); // 0 is default white background 
	}
	int obj=1;
	c=getParameter("square"+obj+"_color");
	while(c!=null && c.length()>0){ // translate param colors into internal color_array
	    c=getParameter("square"+obj+"_color");
	    if(c != null && c.length()>0){ 
		objects++; color[obj] = GetInternalColorCode( c , 13); // 13 default black
	    }
	    obj++;
	}
	c=getParameter("xrange");
	if(c != null && c.length()>0){Determine_Range( c , "x");}// determine the Xrange of the square ... xmin xmax
	c=getParameter("yrange");
	if(c != null && c.length()>0){Determine_Range( c , "y");}// determine the Yrange of the square ... ymin ymax
	Rx=Math.abs(xmax-xmin);Ry=Math.abs(ymax-ymin);
	MAX=Rx*Ry;
	x_rect=(int)xsize/Rx;y_rect=(int)ysize/Ry; // there are Rx squares in our square : per square x_rect pixels
	Clicktile = new int[MAX][MAX]; // Clicktile[0] does not exist: it is the background of canvas !!
	canvas = createImage(xsize,ysize);drawing = (Graphics2D) canvas.getGraphics();
	xcoords=new int[MAX][MAX];ycoords=new int[MAX][MAX];
	length=new int[MAX]; // how many squares per color
	for(int i=0;i<MAX;i++){// fill array with zero's
	    length[i]=0;
	}
	for(int i=0; i<MAX;i++){
	    for(int ii=0;ii<MAX;ii++){
		xcoords[ii][i]=0;// fill array with zero's
		ycoords[ii][i]=0;// fill array with zero's
	    }
	}	
	for(int x=0;x<MAX;x++){
	    for(int y=0;y<MAX;y++){
		Clicktile[y][x] = color[0];// coloring the background color[0]
	    }
	}
	// get the supplementary colors
	c=getParameter("colors");
	int maxcolors = 0 ; 
	if(c != null && c.length()>0){
	   StringTokenizer q = new StringTokenizer(c, ",");
	   maxcolors=q.countTokens() ;
	}
	int[] tmpcolors=new int[objects + 1 + maxcolors];
	if(c != null && c.length()>0){
	    StringTokenizer q = new StringTokenizer(c, ",");
	    maxcolors=q.countTokens();
	    for( int p = 0 ; p<maxcolors ; p++){
		String  k=q.nextToken();
		tmpcolors[p]=GetInternalColorCode( k, 0 );
	    }
	}
	if(maxcolors>0){this_color=maxcolors;}
	// get the square
	tmpcolors[maxcolors]=color[0];
	for(obj=1 ; obj<=objects ;obj++){
	    c=getParameter("square"+obj);
	    int L=0;int coords=0;
	    if( c!=null && c.length()>0){
		// scheme 4:4,2:2,0:0  scheme 4;4,2;2,0;0  scheme 4@4,2@2,0@0 scheme 4 4,2 2,0 0
 		c=c.replace(';',':');c=c.replace('@',':');c=c.replace(' ',':');
		StringTokenizer q1 = new StringTokenizer(c, ",");
		String k1;String k2;
		int array_x=0;int array_y=0;// array_x & array_y are array cell numbers
		coords=q1.countTokens();L=0;
		int tmp;
		boolean out_of_range;
		try {
		    for(int p=0;p<coords;p++){
			k1=q1.nextToken();
			StringTokenizer q2 = new StringTokenizer(k1, ":");
			out_of_range=false;// jm.evers 27/12/2011 : corrected array-out-of-bound
			for(int s=0;s<2;s++){
			    k2=q2.nextToken();
			    tmp=Integer.parseInt(k2,10);
		    	    if(s==0 && (tmp < xmin || tmp > xmax)){
		    		out_of_range=true;
		    		System.out.println("x-value : "+tmp+" will be ignored !!! it is out of xrange\n");
		    	    }
			    if(s==1 && (tmp < ymin || tmp > ymax)){
		    		out_of_range=true;
		    		System.out.println("y-value : "+tmp+" will be ignored !!! it is out of yrange\n");
		    	    }
		    	    if(!out_of_range){
		    		if(s==0){
				    array_x=(int)((tmp - xmin)*(xmax - xmin)/Rx);
				    xcoords[p][obj]=array_x;
				}
		    		else
		    		{
				    array_y=(int)(Ry + (tmp-ymin)*(ymax-ymin)/(-1*Ry));
				    ycoords[p][obj]=array_y;
				}
			    }
			}
			if(!out_of_range){
			    tmpcolors[obj+maxcolors] = color[obj];
			    // give it the appropriate square_color
			    Clicktile[array_y][array_x]=color[obj];
			    L++;
			}
		    }
		    length[obj]=L;
		} catch (Exception e){System.out.println("there is no square"+obj+"\n"+e);}
	    }
	}
	used_colors = ListUniq(tmpcolors);//list uniq array of internal colors [int]

	c=getParameter("image");
	if (c!=null && c.length()>0) {
	    try {url=new URL(c);}
	    catch (MalformedURLException e) {url=null;}
	    if(url!=null) bg=getImage(url);
	    else bg=null;
	}
	else bg=null;

	c=getParameter("copy");
	if( c != null && c.length() > 0 ){
	    StringTokenizer q = new StringTokenizer(c, ",");
	    for( int p = 0 ; p<2 ; p++){
		String  k=q.nextToken();
		if(p==0){
		    copy_x=Integer.parseInt(k,10);
		}
		if(p==1){
		    copy_y=Integer.parseInt(k,10);
		}
	    }
	}
	
	// get  point_color, point,line or polygon
	c=getParameter("point_color");
	if(c != null && c.length()>0){ point_color=GetInternalColorCode( c , 0);}
	c=getParameter("linewidth"); if( c != null && c.length() > 0 ){ linewidth = Integer.parseInt(c,10); }
	c=getParameter("point"); 
	if( c != null && c.length() > 0 ){ 
	    Retreive_values_from_coordinates( c , 1 );
	}
	else
	{
	    c=getParameter("segment"); 
	    if( c != null && c.length() > 0 ){ 
		Retreive_values_from_coordinates( c , 2 ); 
	    }
	    else
	    {
		c=getParameter("line"); 
		if( c != null && c.length() > 0 ){ 
		    Retreive_values_from_coordinates( c , 3 ); 
		}
		else
		{
		    c=getParameter("polygon"); 
		    if( c != null && c.length() > 0 ){ 
			Retreive_values_from_coordinates( c , 4 ); 
		    }
//		    else
//		    {
//			System.out.println("NO SYMMETRY LINE,SEGMENT,POINT or POLY DEFINED");
//		    }
		}
	    }
	}
	
	addMouseListener(this);
	addMouseMotionListener(this);
	addMouseWheelListener(this);
    }

    public synchronized void mouseReleased(MouseEvent evt){}
    public void mouseEntered(MouseEvent evt){requestFocus();}
    public void mouseExited(MouseEvent evt){}
    public void mouseMoved(MouseEvent evt){requestFocus();}
    public void mouseClicked(MouseEvent evt){
	// right click changes color
	if(status){
	    dx = evt.getX()/x_rect;
	    dy = evt.getY()/y_rect;
	    if( evt.getButton() == MouseEvent.BUTTON3 ||  evt.getButton() == MouseEvent.BUTTON2 ){
		this_color++;
		System.out.println("Change color : "+this_color);
		if(this_color > used_colors.length - 1){this_color=0;}
		Clicktile[dy][dx] = used_colors[this_color];
		process_mouse(dx,dy);
	    }
	}
    }
    
    public void mouseWheelMoved(MouseWheelEvent evt){
	if(status){
	    dx = evt.getX()/x_rect;
	    dy = evt.getY()/y_rect;
	    this_color++;
	    if(this_color > used_colors.length - 1){this_color=0;}
	    Clicktile[dy][dx] = used_colors[this_color];
	    process_mouse(dx,dy);
	}
    }
    
    public void mousePressed(MouseEvent evt){
	if(status){
	    dx = evt.getX()/x_rect;
	    dy = evt.getY()/y_rect;
	    process_mouse(dx,dy);
	}
    }

    public void mouseDragged(MouseEvent evt){
	if(status){
	    dx = evt.getX()/x_rect;
	    dy = evt.getY()/y_rect;
	    process_mouse(dx,dy);
	}
    }
    public void process_mouse(int dx,int dy){
	if(dx > -1 &&  dy > -1){//array out of bound
	    int n = 0;boolean do_paint=true;
	    if(dx>=0 && dx<= Rx  && dy>=0 && dy<= Ry){
		for(int obj=0;obj<=objects && do_paint ;obj++){
		    for(int p=0;p<length[obj] && do_paint ;p++){
			if(xcoords[p][obj]==dx && ycoords[p][obj]==dy){//System.out.println("CLICKED ON GIVEN DRAWING !!");
			    do_paint=false;
			}	
		    }
		}
	    }
	    if(do_paint){
	        Clicktile[dy][dx] = used_colors[this_color];
	        repaint(); 
	    }
	}
    }
    
    public void paint(Graphics g){
        drawing.setColor(Color.white);
        drawing.fillRect(0,0,xsize,ysize);
        int k;
	for(int xr = 0;xr < Rx;xr++){
	    for(int yr = 0;yr < Ry;yr++){
		k=Clicktile[yr][xr];
		drawing.setColor(new Color(palette[k][0],palette[k][1],palette[k][2])); // this is the student "drawing"
		drawing.fill3DRect(x_rect*xr,y_rect*yr,x_rect,y_rect,true); // use "3D effect"
	    }
	}
	if(TYPE != 0){drawing.setColor(new Color(palette[point_color][0],palette[point_color][1],palette[point_color][2]));}
	if(TYPE == 1){// several points
	    drawing.setStroke(new BasicStroke(linewidth,BasicStroke.JOIN_ROUND,BasicStroke.JOIN_BEVEL));
	    for(int p = 0; p < xpoints.length ; p++){
		drawing.fillOval( (int)(xpoints[p] - 0.5*linewidth) , (int)(ypoints[p] -0.5*linewidth) , linewidth,linewidth );
		//System.out.println("points "+xpoints[p]+":"+ypoints[p]);
	    }
	}
	else if(TYPE == 2){// several lines
	    drawing.setStroke(new BasicStroke(linewidth,BasicStroke.JOIN_ROUND,BasicStroke.JOIN_BEVEL));
	    for( int p = 0 ; p< xpoints.length -1 ; p=p+2){
		drawing.drawLine( xpoints[p] , ypoints[p], xpoints[p+1], ypoints[p+1]);
	    }
	}
	else if(TYPE == 3){//for now the same as TYPE = 2
	    drawing.setStroke(new BasicStroke(linewidth,BasicStroke.JOIN_ROUND,BasicStroke.JOIN_BEVEL));
	    for( int p = 0 ; p< xpoints.length -1 ; p=p+2){
		drawing.drawLine( xpoints[p] , ypoints[p], xpoints[p+1], ypoints[p+1]);
	    }
	}
	else if(TYPE ==  4){
	    int lim=xpoints.length - 1; // improvised polygon...non-filled
	    drawing.setStroke(new BasicStroke(linewidth,BasicStroke.JOIN_ROUND,BasicStroke.JOIN_BEVEL));
	    for(int p = 0 ;  p < lim  ; p++){
		drawing.drawLine(xpoints[p], ypoints[p], xpoints[p+1],ypoints[p+1]);
	    }
	    drawing.drawLine(xpoints[lim], ypoints[lim], xpoints[0],ypoints[0]);
	}
	g.drawImage(canvas,0,0,this); // draw this square on canvas
	// THIS CODE SHOULD BE LAST: paint on top of image
	// jm.evers 3/10/2010
        if(bg!=null) g.drawImage(bg,copy_x,copy_y,this);
     }

   public void start(){
      if(thread == null){
         thread = new Thread(this);
         thread.start();
      }
   }

    public void stop(){
	thread =  null;
    }

    public void run(){
	while(thread != null){
	    try{
        	Thread.sleep(100);
            }
            catch (InterruptedException e){
            }
        }
    }
    
    public void update( Graphics g ){
	paint( g );
    }
   
    public String ReadApplet(){// public function to be issued by Javascript...and send to WIMS
	String c=getParameter("return_all_objects");
	boolean return_all=false;
	boolean do_print=true;
	if(c.equalsIgnoreCase("yes") || c.equals("1")){return_all=true;} 
	String reply="";boolean fnd=false;int repx=0;int repy=0;int k;String K="";
	for(int x=0; x<Rx; x++){
	    for(int y=0;y<Ry;y++){
		k=Clicktile[y][x];
		// back to coordinates
		repx=(int)(xmin+x*Rx/(xmax - xmin));
		repy=(int)(Ry + (y - ymin)*(ymax-ymin)/(-1*Ry));
		if(return_all){ // <param name="return_all" value="0">
		    if( k != color[0] ){// no background
			K=COLORS[k];
			if(fnd){ reply=reply+","+K+"@"+repx+":"+repy; }
			else { fnd=true; reply=K+"@"+repx+":"+repy; }
		    }
		}
		else
		{  // <param name="return_all" value="1">
		    do_print=true;
		    for(int obj=0;obj<=objects;obj++){
			for(int p=0;p<length[obj];p++){
			    if(xcoords[p][obj]==x && ycoords[p][obj]==y){// exclude the square from params stored in xcoords[] ycoords[]
				do_print=false;
				//System.out.println("will not print ("+x+":"+y+") with color="+color[k]);
			    }
			}
		    }
		    if(do_print){
			if( k != color[0] ){// no background
			    K=COLORS[k];
			    if(fnd){ reply=reply+","+K+"@"+repx+":"+repy; }
			    else { fnd=true; reply=K+"@"+repx+":"+repy; }
			}
		    }
		}
	    }
	}
	if(reply.length() == 0){reply="ERROR: YOU DID NOT CLICK ANYTHING";}
	return reply;
    }
    // jm.evers 1/2/2011
    // returns only: x-values +"\n"+y_values 
    // e.g. no color information
    public String ReadXY(){// public function to be issued by Javascript...and send to WIMS
	String c=getParameter("return_all_objects");
	boolean return_all=false;
	boolean do_print=true;
	if(c.equalsIgnoreCase("yes") || c.equals("1")){return_all=true;} 
	String reply_x="";String reply_y="";boolean fnd=false;int repx=0;int repy=0;int k;
	for(int x=0; x<Rx; x++){
	    for(int y=0;y<Ry;y++){
		k=Clicktile[y][x];
		// back to coordinates
		repx=(int)(xmin+x*Rx/(xmax - xmin));
		repy=(int)(Ry + (y - ymin)*(ymax-ymin)/(-1*Ry));
		if(return_all){ // <param name="return_all" value="0">
		    if( k != color[0] ){// no background
			if(fnd){ reply_x = reply_x+","+repx; reply_y = reply_y+","+repy; }
			else { fnd=true; reply_x=""+repx;reply_y=""+repy; }
		    }
		}
		else
		{  // <param name="return_all" value="1">
		    do_print=true;
		    for(int obj=0;obj<=objects;obj++){
			for(int p=0;p<length[obj];p++){
			    if(xcoords[p][obj]==x && ycoords[p][obj]==y){// exclude the square from params stored in xcoords[] ycoords[]
				do_print=false;
			    }
			}
		    }
		    if(do_print){
			if( k != color[0] ){// no background
			    if(fnd){ reply_x=reply_x+","+repx;reply_y=reply_y+","+repy; }
			    else { fnd=true; reply_x=""+repx;reply_y=""+repy; }
			}
		    }
		}
	    }
	}
	if(reply_x.length() == 0){reply_x="ERROR: YOU DID NOT CLICK ANYTHING";}
	return reply_x+"\n"+reply_y;
    }
    
    public int GetInternalColorCode(String c , int defaultcode){
	int colorcode=defaultcode;
    	if(c.equalsIgnoreCase("white")) colorcode=0;
	else 
	if(c.equalsIgnoreCase("red")) colorcode=1;
	else 
	if(c.equalsIgnoreCase("green")) colorcode=2;
	else 
	if(c.equalsIgnoreCase("blue")) colorcode=3;
	else 
	if(c.equalsIgnoreCase("orange")) colorcode=4;
	else
	if(c.equalsIgnoreCase("yellow")) colorcode=5;
	else
	if(c.equalsIgnoreCase("purple")) colorcode=6;
	else
	if(c.equalsIgnoreCase("lightgreen")) colorcode=7;
	else
	if(c.equalsIgnoreCase("lightblue")) colorcode=8;
	else
	if(c.equalsIgnoreCase("cyan")) colorcode=9;
	else
	if(c.equalsIgnoreCase("brown")) colorcode=10;
	else
	if(c.equalsIgnoreCase("salmon")) colorcode=11;
	else
	if(c.equalsIgnoreCase("pink")) colorcode=12;
	else
	if(c.equalsIgnoreCase("black")) colorcode=13;
	
	return colorcode;
    }
    
    public int X_ConvertToInternalCoordinates(int x){
	int X = (int)(x_rect*(x - xmin)*(xmax - xmin)/Rx);
	return X;
    }
    
    public int Y_ConvertToInternalCoordinates(int y){
	int Y = (int)(y_rect*(Ry + (y - ymin)*(ymax - ymin)/(-1*Ry)));
	return Y;
    }
    
    public void Retreive_values_from_coordinates( String c , int type){
	// typical value="-5:5,-4;4,-3:3"
	TYPE=type;
	c=c.replace(';',':');c=c.replace('@',':');c=c.replace(' ',':');
	StringTokenizer q1 = new StringTokenizer(c, ",");
	String k1;String k2;
	int m0=q1.countTokens();
	xpoints=new int[m0];
	ypoints=new int[m0];
	int tmp;int m1;boolean flipflop=true;
	for(int p=0;p<m0;p++){
	    k1=q1.nextToken();
	    StringTokenizer q2 = new StringTokenizer(k1, ":");
	    m1 = q2.countTokens();
	    for(int s=0; s<m1 ; s++){
		k2=q2.nextToken();
		if(flipflop){
		    try{ tmp = Integer.parseInt(k2,10); xpoints[p] = X_ConvertToInternalCoordinates(tmp); }
		    catch(Exception e){System.out.println("ERROR\n"+e);}
		    flipflop=false;
		}
		else
		{
		    try{ tmp = Integer.parseInt(k2,10); ypoints[p] = Y_ConvertToInternalCoordinates(tmp); }
		    catch(Exception e){System.out.println("ERROR\n"+e);}
		    flipflop=true;
		}
	    }
	}


    }
    
    public void Determine_Range(String c , String type){
	c=c.replace(':',',');c=c.replace(';',',');
	StringTokenizer q = new StringTokenizer(c, ",");
	String k;
	for(int p=0;p<2;p++){
	    k=q.nextToken();
	    if(p==0){
		if(type.equals("x")){ xmin = Integer.parseInt(k,10);}
		else { ymin = Integer.parseInt(k,10);}
	    }
	    else
	    {
		if(type.equals("x")){ xmax = Integer.parseInt(k,10);}
		else { ymax = Integer.parseInt(k,10);}
	    }
        }
    }
    
    public void Determine_Color_Names(String lang){
	if(lang.equalsIgnoreCase("nl")){ 
	    COLORS[0]="wit";COLORS[1]="rood";COLORS[2]="groen";COLORS[3]="blauw";
	    COLORS[4]="oranje";COLORS[5]="geel";COLORS[6]="paars";COLORS[7]="licht groen";
	    COLORS[8]="licht blauw";COLORS[9]="cyaan";COLORS[10]="bruiwn";COLORS[11]="zalmroze";
	    COLORS[12]="roze";COLORS[13]="zwart";// max = 13 
	}
	else
	{ // TO DO : TRANSLATION ?
	
	    if(lang.equalsIgnoreCase("fr")){
		COLORS[0]="blanc";COLORS[1]="rouge";COLORS[2]="vert";COLORS[3]="bleu";
		COLORS[4]="orange";COLORS[5]="jaune";COLORS[6]="violet";COLORS[7]="vert pâle";
		COLORS[8]="bleu clair";COLORS[9]="cyan";COLORS[10]="marron";COLORS[11]="saumon";
		COLORS[12]="rose";COLORS[13]="noir";
	    }
	    else
	    {
		if(lang.equalsIgnoreCase("de")){
		    COLORS[0]="weiß";COLORS[1]="rot";COLORS[2]="grun";COLORS[3]="blau";
		    COLORS[4]="orange";COLORS[5]="gelb";COLORS[6]="violett";COLORS[7]="hellgrün";
		    COLORS[8]="hellblau";COLORS[9]="cyan";COLORS[10]="braun";COLORS[11]="Lachsfarbig";
		    COLORS[12]="rosa";COLORS[13]="schwartz";
		}
		else
		{
		    COLORS[0]="white";COLORS[1]="red";COLORS[2]="green";COLORS[3]="blue";
		    COLORS[4]="orange";COLORS[5]="yellow";COLORS[6]="purple";COLORS[7]="lightgreen";
		    COLORS[8]="lightblue";COLORS[9]="cyan";COLORS[10]="brown";COLORS[11]="salmon";
		    COLORS[12]="pink";COLORS[13]="black";
		}
	    }	
	}
    }
    // a listuniq on colors used in several square1...square_n    
    public int[] ListUniq(int[] array){
	Arrays.sort(array);
	int k = 0;
        for (int i = 0; i < array.length; i++){
	    if (i > 0 && array[i] == array[i -1] )
	    continue;
	    array[k++] = array[i];
	}
	int [] unique = new int[k];
        System.arraycopy(array, 0, unique, 0, k);
	return unique;
    }
}


/*
0 "white" {255,255,255}
1 "red" {255,0,0}
2 "green" {0,255,0}
3 "blue" {0,0,255}
4 "orange" {238,154,0}
5 "yellow" {255,255,0}
6 "purple" {160,32,240}
7 "lightgreen" {144,238,144}
8 "lightblue" {173,216,230}
9 "cyan" {0,255,255}
10 "brown" {165,24,24}
11 "salmon" {250,128,114}
12 "pink" {255,192,203}
*/

//{255,255,255},{255,0,0},{0,255,0},{0,0,255},{238,154,0},{255,255,0},{160,32,240},{144,238,144},{173,216,230},{0,255,255},{165,24,24},{250,128,114},{255,192,203}
