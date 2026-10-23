/*
*********************************************************************************
* J.M. Evers 2010								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty.									*
*********************************************************************************
version 0.41
11/2012
corrected html table output

version 0.4
10/12/2010
Added param pointsize to differ size of points from linewidth
Removed &minus;&times from html reply (problems with representation after sending to wims) 
now just ascii + - : x

version 0.3
Added decimals [point object]

version 0.2

Added correct French button text [B. Perrin-Riou]
Added left hand +,-,x,: sign next to the lines [when language=fr ]

version 0.1

Applet for simple adding, subtracting,dividing and multiplying by pupils.
The result can be checked by WIMS.

Output:
document.getElementById('InvulGrid').ReadApplet("0") : itemlist [one item per horizontal line]
document.getElementById('InvulGrid').ReadApplet("1") : javascript confirmbox
document.getElementById('InvulGrid').ReadApplet("2") : html table
document.getElementById('InvulGrid').ReadApplet("3") : latex array/table

Example [same as test.html]
<html>
 <body>
     <script type="text/javascript">
        function ReadThis(){

    	    var wims_reply = document.getElementById('InvulGrid').ReadApplet("0"); 
	    alert("this can send to wims for correction:\n"+wims_reply); 
	    
	    var js_alert_show= document.getElementById('InvulGrid').ReadApplet("1"); 
	    alert("this can be the confirmbox for the pupil:\n"+js_alert_show); 
	    
	    var html_show= document.getElementById('InvulGrid').ReadApplet("2"); 
	    document.getElementById('html_reply').innerHTML="this is a html-table representation<br>"+html_show;
	    
	    var latex_show= document.getElementById('InvulGrid').ReadApplet("3"); 
	    alert("this is an latex array of the answer:\n Usable via !insmath\n"+latex_show);
	}
    </script>. 
    <table>
    <tr>
    <th>
    <applet id="InvulGrid" codebase="dist" archive="InvulGrid.jar"  code="InvulGrid.class"  width="400" height="400">
	<param name="fontsize" value="50"><!-- optional: actual fontsize will be determined according xlines/ylines -- xsize/ysize -->
	<param name="pencolor" value="0,0,255,150"><!-- optional: typing color -->
	<param name="bgcolor" value="255,255,255"><!-- optional: canvas bgcolor;default white ; R,G,B,alpha -->
	<param name="markcolor" value="10,255,10,60"><!-- optional:  active field color -->
	<param name="linecolor" value="200,10,60,150"><!-- optional:  user drawn lines in applet  --> 
	<param name="linewidth" value="6"><!-- linewidth of user drawn lines -->
	<param name="pointsize" value="3"><!-- size of drawn decimal points -->
	
	<param name="xlines" value="12"><!-- default 10 ; number of vertical lines : determines fontsize -->
	<param name="ylines" value="12"><!-- default 10 ; number of horizontal lines  -->
	<param name="showgrid" value="1"><!-- show xlines and ylines as grid in gridcolor -->
	<param name="gridcolor" value="0,0,255,40"><!-- color of grid  -->
	<param name="reverse" value="yes"><!-- default false ; editing from right to left  -->
	
	<param name="language" value="en"><!-- default en ; nl , fr -->
	<!-- optional: is an itemlist, spaces matter .-->
	<!-- Optional horizontal lines: ---|x|-|+|:  -->
	<!-- bug in java plugin's stringtokenizer: first token blancspace is stripped off... -->
	<!-- in applet viewer this works fine -->
	<!-- to produce an space before the first number, add a "," like-->
	<!-- spaces matter  !! -->
	<param name="exercise" value=",   13456789,     567890,      12345,      45678,    2345678,---|-">
	
	<!-- use first row of grid for user scribbling: this will not be send to wims -->
	<param name="scribling_pencolor" value="255,200,0,170"><!-- optional -->
	<!-- if param buttons is empty or not set : only a 'clear all; button will be shown -->
	<param name="buttons" value="+"><!-- user draws lines with +,-,x,&divide; symbols ; do not use too many!  --> 
    </applet>
    <br> 
    <input type="button" name="InvulGrid" value="Read " onclick="javascript:ReadThis(0);"> 
    </th>
    <th>
	<div id="html_reply"></div>
    </th>
    </tr>
    <table>
    </body>
</html>
*/

import java.applet.Applet;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.Color;
import javax.swing.*;
//import java.io.*; not needed, we use javascript to communicate with WIMS

public class InvulGrid extends JApplet implements  ActionListener,KeyListener,MouseListener, MouseMotionListener, MouseWheelListener {
    Font font;
    int fontsize,xsize,ysize,xlines,ylines,xstep,ystep,linewidth,pointsize;
    Color scribling_pencolor,bgcolor,pencolor,linecolor,markcolor,gridcolor,deletecolor,marked_color,exercisecolor,scribling_rowcolor;
    String[][] charArray;
    boolean[][] Marked;
    boolean[][] Editable;
    boolean[] EditableObject;
    int xindex = 0;
    int yindex = 0;
    int objcnt=0;
    Graphics2D backg;
    BufferedImage bg;
    Vector objects = new Vector();
    Vector decimalpoints = new Vector();
    int pointcnt = 0;
    boolean showgrid;
    public String line_plus="line + ";
    public String line_times="line x";
    public String line_min="line -";
    public String line_div="line :";
    public String delete_line="delete line";
    public String clear_all="clear all";
    Container cp;
    int editablecnt = 0;
    int fontheight=10;
    boolean check_segments = false;
    boolean reverse = false;
    boolean left_to_right = false;
    int scribling = 2;
    // true: --- +  false: + ---
    boolean right = true;
    int fontwidth = 10;
    // reverse :if no 'conclusion line' is drawn, we write form left to right
    // if a line is drawn, we write from right to left...

    // slow repainting can occur via update() : using bufferedimage of grid to avoid flickering.
     
    public void init(){
	scribling_pencolor = colorParam("scribling_pencolor", new Color(255,0,0));
	if( (getBool("scribling_row",true)) == true){scribling = 2;}else{scribling = 1;}
	getLanguage("language");
	getButtons();	
	Dimension appletSize = getSize();
	xsize = appletSize.width; 
	ysize = appletSize.height;
	xlines = getInt("xlines",10);
	ylines = getInt("ylines",10);
	xstep = (int) xsize/xlines;
	ystep = (int) ysize/(ylines+1);
	bg = new BufferedImage(xsize,ysize, BufferedImage.TYPE_INT_ARGB);
	backg = (Graphics2D) bg.getGraphics();
	// fitfont kan ylines bepalen !! volgorde is belangrijk
	fontsize = getInt("fontsize",28);
	font = FitFont( backg ,"TimesRoman" , fontsize);
	charArray = new String[xlines+1][ylines+1];
	Marked = new boolean[xlines+1][ylines+1];
	Editable = new boolean[xlines+1][ylines+1];
	EditableObject = new boolean[xlines*ylines];
	
	for(int x = 0; x < xlines+1; x++){
	    for(int y = 0; y < ylines+1 ; y++){
		charArray[x][y]="";
		Marked[x][y]=false;
		Editable[x][y]=true;
	    }
	}
	for(int y = 0 ; y <xlines*ylines;y++){
	    EditableObject[y] = true;
	}
	showgrid = getBool("showgrid",true);
	left_to_right = getBool("reverse",false);
	scribling_rowcolor = colorParam("scribling_rowcolor",new Color(0,255,0,50));
	bgcolor = colorParam("bgcolor",Color.white);
	pencolor = colorParam("pencolor",Color.black);
	linecolor = colorParam("linecolor",Color.red);
	markcolor = colorParam("markcolor",Color.green);
	gridcolor = colorParam("gridcolor",Color.blue);
	exercisecolor = colorParam("exercisecolor",Color.black);
	linewidth = getInt("linewidth",2);
	pointsize = getInt("pointsize",5);
	deletecolor = new Color(255,0,0,120);
	marked_color = markcolor;
	addKeyListener(this); 
        addMouseListener(this);
        addMouseMotionListener(this);
	addMouseWheelListener(this);
	getSegments();
	ReadStringParam();
	prepaint();
	requestFocus();
    }

    public String ReadApplet(String t){
	String[] reply = new String[ylines+1];
	String[] horline = new String[ylines+1];
	StringBuffer tmp;
	int cnt = 0;
	int userline;
	int maxlength=0;
	String op;// operator +,-,/,*
	boolean not_empty = false;
	boolean found_line = false;
	int pnt = -1;
	for( int y = scribling ; y <= ylines ; y++ ){// y = 1 all including scribling row ; y=2 excluding scribling row 
	    tmp = new StringBuffer();
	    found_line = false;
	    op = "";
	    pnt = -1;
	    for(int i = 0 ; i < objcnt ; i++){
		if (objects.elementAt(i) instanceof GridLine){
		    if( ((GridLine)objects.elementAt(i)).isSegment()  == true ){
			userline = (int)(((GridLine)objects.elementAt(i)).getY1()/ystep) - 1;
			if( y == userline ){
			    found_line = true;
			    op = ((GridLine)objects.elementAt(i)).getText();
			    i = objcnt; //exit
			}
		    }
		}
	    }
	    pnt = findPoint(y);
	    for( int x = 0 ; x <= xlines ; x++ ){
		if(x == pnt){
		    tmp.append(".");
		}
		tmp.append(charArray[x][y]);
	    }
	    if(tmp.length() != 0){
		if( tmp.length() > maxlength ){maxlength = tmp.length();}
		not_empty = true;
		reply[cnt] = replace(tmp.toString(), " " , "");
		//System.out.println("added "+reply[cnt] + " by y =  "+y);
		if(found_line == true){horline[cnt] = op;}
		cnt++;
	    }
	
	}
	if(not_empty == false){return "error:empty answer";}

	if( t.equals("3") ){
	    return makeLatex(reply,horline);
	}
	else
	{
	    if( t.equals("2") ){ 
		return makeHtml(reply,horline); 
	    }
	    else
	    {
		if( t.equals("1") ){ 
		    return makeJs(reply,horline,maxlength); 
		}
		else
		{ 
		    if( t.equals("0") ){ 
			return Array2String(reply,",");
		    }
		    else
		    {
			return "error : usage is \n ReadApplet(0) ... wims itemlist\n ReadApplet(3) ... latex representation\n ReadApplet(2) ... html representation\n ReadApplet(1) ... javascript confirmbox";
		    }	
		}
	    }
	}    
    }
    
    public String Array2String(String[] s, String sep) {
	StringBuilder result = new StringBuilder();
	if (s.length > 0) {
    	    result.append(s[0]);
    	    for (int i=1; i < s.length; i++) {
    		if(s[i] != null){
        	    result.append(sep);
        	    result.append(s[i]);
        	}
    	    }
	}
	return result.toString();
    }

    public String makeLatex(String[] reply , String[] horline){
	String latex="";
	String op="";
	String space="";
	boolean fraction = false;
	int w = 0;int thisline=0;
	while ( fraction == false && w <= ylines ){
	    if(reply[w] != null){
		if(reply[w].indexOf("\\") != -1){
		    fraction = true;
		    thisline = w;
		}
	    }
	    w++;
	}
	
	if(fraction == true ){
	    int start = (reply[thisline]).indexOf("/");
	    int end = (reply[thisline]).indexOf("\\");
	    w = end - start - 1;
	    if(w<0){w = reply[thisline].length();}
	    reply[thisline] = replace(reply[thisline],"\\"," & \\diagdown & ");
	    reply[thisline] = replace(reply[thisline],"/"," & \\diagup & ");
	    reply[thisline] = reply[thisline] + " \\\\ ";
	    latex="\\begin{array}{cclcc}"+reply[thisline];
	    thisline++;
	}
	else
	{
	    latex="\\begin{array}{rr}";
	    thisline = 0;
	}
	
	for( int y = thisline ; y < reply.length ; y++ ){
	    if( reply[y] != null){
		if(horline[y] != null){
		    if((horline[y]).equals("\u00D7")){
			op =" \\times ";
		    }
		    else
		    {    
			if((horline[y]).equals("\u00F7")){
			    op =" \\div ";
			}
			else
			{
			    op = horline[y];    
			}
		    }
		    if(fraction == false){
			if(right){
			    latex = latex + " \\underline{" +reply[y]+ "} & "+horline[y] +" \\\\ ";
			}
			else // french
			{
			    latex = latex + horline[y] +" & \\underline{" +reply[y]+ " } \\\\ ";
			}
		    }
		    else
		    {
			if( w > reply[y].length()){
			    for(int z=0; z < w - reply[y].length();z++){
				space = space + " \\: ";
			    }
			}
			latex = latex + "&  & \\underline{" +space+ reply[y]+"} &  & \\\\ ";
		    }
		}
		else
		{
		    if(fraction == false){
			if(right){
			    latex  = latex + " "+ reply[y] + " & "+ " \\\\ ";
			}
			else // french
			{
			    latex  = latex + " & "+ reply[y] + " \\\\ ";
			}
		    }
		    else
		    {
			if( w > reply[y].length()){
			    for(int z=0; z < w - reply[y].length();z++){
				space = space + " \\: ";
			    }
			}
			latex  = latex + "&  & "+ space+ reply[y] +" &  &  \\\\ ";
		    }
		}
	    }
	}
	latex = latex + " \\end{array}";
	return latex;
    }

    public String makeHtml(String[] reply , String[] horline){
	String table = "<table style=\"margin-bottom:1px;margin-top:1px;\"><tr><td style=\"text-align:right\"><table style=\"margin-bottom:1px;margin-top:1px;width:100%\">";
	String tmp="";
	for( int y = 0 ; y < reply.length ; y++ ){
	    if(reply[y] != null){
		if(right){
		    table=table+"<tr><td style=\"text-align:right\">"+reply[y]+"</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td></tr>";
		}
		else // french
		{
		    table=table+"<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;</td><td style=\"text-align:right\">"+reply[y]+"</td></tr>";
		}
		if(horline[y] != null){
		    if(horline[y].equals("\u00D7")) tmp = "x";
		    else
		    if(horline[y].equals("\u00F7")) tmp = ":";
		    else tmp = horline[y];
		    
		    if(right){
			table=table+"</table></td><tr><td><table style=\"margin-bottom:1px;margin-top:1px;width:100%\"><tr><td style=\"width:90%\"><hr></td><td>"+tmp+"</td></tr></table></td></tr><td><table style=\"margin-bottom:1px;margin-top:1px;width:100%\">";
		    }
		    else // french
		    {
			table=table+"</table></td><tr><td><table style=\"margin-bottom:1px;margin-top:1px;width:100%\"><tr><td>"+tmp+"</td><td width=\"90%\"><hr></td></tr></table></td></tr><td><table style=\"margin-bottom:1px;margin-top:1px;width=:100%\">";
		    }
		}
	    }
	}
	table = table+"</table></td></tr></table>";
	return table;
    }
    
    public String makeJs(String[] reply, String[] horline , int maxlength){
	String js="";String space = "";String line = "";String op = "";
	for(int i = 0 ; i < maxlength ; i++){
	    line = line + "-";
	}
	for( int y = 0 ; y < reply.length ; y++ ){
	    if(reply[y] != null){
		if(horline[y] != null){
		    space = "";
		    for(int i = 0 ; i < (maxlength - reply[y].length()) ; i++){
			space = space + " ";
		    }
		    if(right){
			js = js + space+reply[y] + "\n" + line + " " + horline[y] + "\n";
		    }
		    else // french
		    {
			js = js + space+reply[y] + "\n" + horline[y] +"  "+ line + "\n";
		    }
		}
		else // trying to align at the rightside of the alertbox ... somehow this will not work properly
		{
		    space = "";
		    for(int i = 0 ; i < (maxlength - reply[y].length()) ; i++){
			space = space + " ";
		    }
		    js  = js + space + reply[y] + "\n";
		}
	    }
	}
	return js;
    }
    
    public void getSegments(){
	String param = getParameter("segment1");
	if(param != null && param.length() > 3){
	
	    int i = 1;
	    StringTokenizer s;
	    int m=0,x1=0,x2=0,y1=0,y2=0;
	    String colorparam;Color segmentcolor;
	    check_segments = true;
	    while( param != null && param.length() >3 ){
		s = new StringTokenizer(param, ",");
		m = s.countTokens();
		if(m != 4){System.out.println("Expecting 2 points for a linesegment");return;}
		for(int p = 0 ; p < 4 ; p++){
		    if( p == 0 ){ x1 =  Integer.parseInt(s.nextToken());}
		    else
		    if( p == 1 ){ y1 =  Integer.parseInt(s.nextToken());}
		    else
		    if( p == 2 ){ x2 =  Integer.parseInt(s.nextToken());}
		    else
		    if( p == 3 ){ y2 =  Integer.parseInt(s.nextToken());}
		}
		colorparam = "color"+i;
		EditableObject[editablecnt] = false;editablecnt++;
		segmentcolor =  colorParam(colorparam,linecolor);
		drawLine(x1*xstep,(y1+2)*ystep,x2*xstep,(y2+2)*ystep,segmentcolor,"",false,false);
		i++;
		param = getParameter("segment"+i);
	    }
	}
    }


    public void ReadStringParam(){
	String exo = getParameter("exercise");
	if(exo != null && exo.length()>0){
	    StringTokenizer lines = new StringTokenizer(exo, ",");
	    int m=lines.countTokens();
	    boolean found_fraction = false;
	    boolean found_decimal_point = false;
	    if( m < ylines ){
		String lastword="";
		String word="";String tmp="";int xi=0;
		for( int i = 0 ; i < m ; i++){
		    word = lines.nextToken();
		    if(word.indexOf("/") != -1){ found_fraction = true;}
		    if(word.indexOf("---|") != -1){// a special line and should be LAST !!
			// the last word was: charArray[xindex - 1][yindex - 1]
			char[] wordArray = lastword.toCharArray();
			boolean found = false;int start = 0;int end = wordArray.length;
			for(int s = 0; s < end;s++){
			    if(wordArray[s] != ' ' && !found){start = s;found = true;}
			}
			if(right && found_decimal_point){
			    end = end - 1;
			}
			else
			{
			    if(!right && found_decimal_point && start >0 ){start = start - 1; end = end - 1;}
			}
			if(word.indexOf("---|+") != -1){ // ---- +
			    drawLine(start*xstep,(i+2)*ystep,end*xstep,(i+2)*ystep,linecolor,"+",false,true);
			    EditableObject[editablecnt] = false;editablecnt++;
			}
			else
			{
			    if(word.indexOf("---|-") != -1){ // ---- -
				drawLine(start*xstep,(i+2)*ystep,end*xstep,(i+2)*ystep,linecolor,"-",false,true);
				EditableObject[editablecnt] = false;editablecnt++;
			    }
			    else
			    {
				if(word.indexOf("---|x") != -1){ // ---- x
				    drawLine(start*xstep,(i+2)*ystep,end*xstep,(i+2)*ystep,linecolor,"\u00D7",false,true);
				    EditableObject[editablecnt] = false;editablecnt++;
				}
				else
				{
				    if(word.indexOf("---|:") != -1){ // ---- division
					drawLine(start*xstep,(i+2)*ystep,end*xstep,(i+2)*ystep,linecolor,"\u00F7",false,true);
					EditableObject[editablecnt] = false;editablecnt++;
				    }
				    else
				    {
					System.out.println("unknown command : "+word);
				    }
				}
			    }
			}    
		    }
		    else
		    {
			xi=0;
			for( int p = 0 ; p < word.length(); p++){
			    if( p <= xlines ){
				tmp = Character.toString(word.charAt(p));
				if(tmp.equals(".")){
				    drawPoint(xi,i+2,pointsize,false);
				    found_decimal_point = true;
				}
				else
				{
				    charArray[xi][i+2] = tmp;
			    	    //we do not use yindex=1 : space is used for controls...
				    lastword = word;
				    if(found_fraction == true){
					Editable[xi][i+2] = false;
				    }
				    else
				    {
					for(int c = 0 ; c<xlines;c++){ // disable editing of line y=i+2
					    Editable[c][i+2] = false;
					}
				    }
				    xi++;
				}
			    }
			    else
			    {
			    	System.out.println("ERROR : word "+word+" too large for "+xlines+" gridlines...\nin param \"execise\"");
				return;
			    }
			}
		    }
		}
		for(int x = 0 ; x<xlines ; x++){
		    Editable[x][1] = true;
		    charArray[x][1]="";
		}
	    }
	    else
	    {
		System.out.println("ERROR : to many spaces in param \"exercise\"");
		return;
	    }
	}
    }


    public void getLanguage(String s){
	s = getParameter("language");
	if(s != null && s.length()  == 2){
	    if( s.equalsIgnoreCase("nl")){
		line_plus="lijn + ";
		line_times="lijn x";
		line_min="lijn -";
		delete_line="verwijder lijn";
		clear_all="alles wissen";
		line_div="line :";
		right=true;
		return;
	    }
	    else
	    {
		if( s.equalsIgnoreCase("fr")){
		    line_plus="+ à droite ";
		    line_times="droite x";
		    delete_line="effacer droite";
		    clear_all="effacer";
		    line_min="droite -";
		    line_div="droite :";
		    right=false;
		    return;
		}
	    }
	}
    }
    
    
    public Font FitFont(Graphics g , String fontname , int fontsize){
	String maxstring="";
	for(int p = 0 ; p<xlines+4;p++){
	    maxstring=maxstring+"5";
	}
	font  = new Font(fontname,Font.BOLD,fontsize);
	FontMetrics fm = g.getFontMetrics(font);
	if(fm.stringWidth(maxstring) > xsize){
	    boolean does_not_fit = true;
	    while(fontsize > 4 && does_not_fit){
		fontsize = fontsize - 1;
		font = new Font(fontname,Font.BOLD,fontsize);
		fm = g.getFontMetrics(font);
		if(fm.stringWidth(maxstring) < xsize){
		    does_not_fit = false;
		}
	    }
	    System.out.println("xsize ("+xsize+") of applet too small...I've adjusted fontsize to "+fontsize);
	    font  = new Font(fontname,Font.BOLD,fontsize);
	    fm = g.getFontMetrics(font);
	}
	fontheight =(int) (0.6*fm.getHeight());
	if( fontheight > ystep ){ // fonts will overlap !! decrease ygrid...
	    System.out.println("I will decrease number of horizontal lines: fonts will overlap...\nuse more xlines to overcome this.");
	    ystep =  fontheight;
	    ylines = (int) (ysize / fontheight);
	    if(ylines < 2){ylines = 2;}
	}
	fontwidth = fm.stringWidth("+");
	return font;
    }
    
    public int getInt(String s, int i){
        String s1 = getParameter(s);
        if( s1 != null && s1.length()!=0){
    	    try{
    		i = Integer.parseInt(s1);
    	    }
    	    catch(Exception exception){System.out.println(" can not parse parameter "+s);}
    	}
        return i;
    }

    public boolean getBool(String s, boolean flag){
        String s1 = getParameter(s);
        if(s1 != null){
            if(s1.equals("1") || s1.equalsIgnoreCase("yes") || s1.equalsIgnoreCase("true"))
                return true;
            if(s1.equals("0") || s1.equalsIgnoreCase("no") || s1.equalsIgnoreCase("false"))
                return false;
        }
        return flag;
    }

    public Color colorParam(String s, Color color){
        String s1 = getParameter(s);
        if(s1 != null && s1.length() != 0){
            s1 = replace(s1,":", ",");
            s1 = replace(s1,";", ",");
            if( s1.indexOf(',') > 0 ){
        	StringTokenizer stringtokenizer = new StringTokenizer(s1, ",");
        	int i = stringtokenizer.countTokens();
        	if(i < 3 || i > 4){ return color;}
        	int ai[] = new int[i + 1];
        	for(int j = 0; j < i; j++){
            	    ai[j] = Integer.parseInt(stringtokenizer.nextToken());
            	    if(ai[j] > 255 || ai[j] < 0){ ai[j] = 0;}
        	}
        	if(i == 3){
        	    color = new Color(ai[0], ai[1], ai[2]);
    		}
    		else
    		{
            	    color = new Color(ai[0], ai[1], ai[2], ai[3]);
    		}
	    }
	    else
	    {
		try{ color = Color.decode( s1 );}catch(Exception e){System.out.println("could not parse "+s);}
		return color;
	    }
	}
        return color;
    }

    public void prepaint(){ // all "static" components are painted "once"
        backg.setColor(bgcolor);
        backg.fillRect(0,0,xsize,ysize);
        if(showgrid){ 
    	    backg.setColor(gridcolor); 
	    backg.setStroke( new BasicStroke(1));
    	    for(int x=0; x<xsize;x=x+xstep){
    		    backg.drawLine(x,ystep,x,ysize);
    	    }
    	    for(int y=ystep;y<ysize;y=y+ystep){
    		backg.drawLine(0,y,xsize,y);    		
    	    }
    	}
    	reverse=false;
	backg.setFont(font);
	for( int i = 0 ; i < objcnt; i++){
	    if (objects.elementAt(i) instanceof GridLine){
		backg.setStroke( new BasicStroke(linewidth));
		backg.setColor(((GridLine)objects.elementAt(i)).getColor());
		backg.drawLine(
		    ((GridLine)objects.elementAt(i)).getX1(),
		    ((GridLine)objects.elementAt(i)).getY1(),
		    ((GridLine)objects.elementAt(i)).getX2(),
		    ((GridLine)objects.elementAt(i)).getY2()
		);
		if(right){
		    backg.drawString(
			((GridLine)objects.elementAt(i)).getText(),
			((GridLine)objects.elementAt(i)).getX2()+linewidth,
			((GridLine)objects.elementAt(i)).getY2()
			//((GridLine)objects.elementAt(i)).getY2()+(fontheight-linewidth)/2
		    );
		}
		else
		{
		    backg.drawString(
			((GridLine)objects.elementAt(i)).getText(),
			((GridLine)objects.elementAt(i)).getX1()-linewidth-fontwidth,
			((GridLine)objects.elementAt(i)).getY2()
			//((GridLine)objects.elementAt(i)).getY2()+(fontheight-linewidth)/2
		    );
		}
		if(i == objcnt - 1){
		    reverse= ((GridLine)objects.elementAt(i)).isSegment();
		}
	    }
	}
	for( int i = 0 ;i < pointcnt ; i++){
	    if (decimalpoints.elementAt(i) instanceof DecimalPoint){
		int l = ((DecimalPoint)decimalpoints.elementAt(i)).getD();
		backg.setStroke( new BasicStroke(l));
		backg.setColor(pencolor);
		backg.fillOval(
		    xstep*(((DecimalPoint)decimalpoints.elementAt(i)).getX()) - pointsize,
		    ystep*(1+((DecimalPoint)decimalpoints.elementAt(i)).getY()) - pointsize,
		    l, 
		    l
		);
	    }
	}
	backg.setColor(scribling_rowcolor);
	for(int x = 0 ; x <= xlines ; x++){
	    backg.fillRect(x*xstep,ystep,xstep,ystep); //scribling row
	}

	backg.setColor(exercisecolor);
	for(int x=0;x <= xlines ;x++){
	    for(int y=0;y <= ylines  ;y++){
		if(!Editable[x][y]){
		    backg.drawString(charArray[x][y],x*xstep,(y+1)*ystep);
		}
	    }
	}
    }
    
    public void update(Graphics g){
	prepaint();
	paintComponent(g);
    }
    
    public void paint(Graphics g){
	update(g);
    }

    public void paintComponent(Graphics g){
	Graphics2D g2 = (Graphics2D) g;
	g2.drawImage(bg,0,0,this);
	g2.setFont(font);
	reverse=false;
	for( int i = 0 ; i < objcnt; i++){
	    if (objects.elementAt(i) instanceof GridLine){
		if( ((GridLine)objects.elementAt(i)).getEdit() ){
		    backg.setStroke( new BasicStroke(linewidth));
		    backg.setColor(((GridLine)objects.elementAt(i)).getColor());
		    backg.drawLine(
			((GridLine)objects.elementAt(i)).getX1(),
		        ((GridLine)objects.elementAt(i)).getY1(),
			((GridLine)objects.elementAt(i)).getX2(),
		        ((GridLine)objects.elementAt(i)).getY2()
		    );
		    if(right){
			backg.drawString(
			    ((GridLine)objects.elementAt(i)).getText(),
		    	    ((GridLine)objects.elementAt(i)).getX2()+linewidth, // ---- +
			    ((GridLine)objects.elementAt(i)).getY2()
			    //((GridLine)objects.elementAt(i)).getY2()+(fontheight-linewidth)/2
			);
		    }
		    else //french
		    {
			backg.drawString(
			    ((GridLine)objects.elementAt(i)).getText(),
		    	    ((GridLine)objects.elementAt(i)).getX1()-linewidth-fontwidth, // + ---- 
			    ((GridLine)objects.elementAt(i)).getY2()+(fontheight-linewidth)/2
			);
		    
		    }
		}
		if(i == objcnt - 1){
		    reverse= ((GridLine)objects.elementAt(i)).isSegment();
		}
	    }
	}
	for( int i = 0 ; i < pointcnt; i++){
	    if (decimalpoints.elementAt(i) instanceof DecimalPoint){
		int l = ((DecimalPoint)decimalpoints.elementAt(i)).getD();
		backg.setStroke( new BasicStroke(l));
		backg.setColor(pencolor);
		backg.fillOval(
		    xstep*(((DecimalPoint)decimalpoints.elementAt(i)).getX()) - pointsize,
		    ystep*(1+((DecimalPoint)decimalpoints.elementAt(i)).getY()) - pointsize,
		    l, 
		    l
		);
	    }
	}
	for(int y=1;y <= ylines  ;y++){ // start from scribling row
	    for(int x=0;x <= xlines ;x++){
		if(Marked[x][y]){
		    g2.setColor(marked_color);
		    g2.fillRect(x*xstep,y*ystep,xstep,ystep);
		    Marked[x][y]=false;
		}
		if(Editable[x][y]){
		    if( y == 1){ // scribling row
			g2.setColor(scribling_pencolor);
		    }
		    else
		    {
			g2.setColor(pencolor);
		    }
		    g2.drawString(charArray[x][y],x*xstep,(y+1)*ystep);
		}
	    }
	}
	
    }
    
    public synchronized void mousePressed(MouseEvent evt){
        int x = evt.getX();int y = evt.getY();
        boolean xfound=false,yfound=false;
        for(int p=0 ; p < xsize ; p = p + xstep){
    	    if(x > p && x < p + xstep ){
    		xindex = (int) x/xstep;
    		xfound=true;
    	    }
        }
        for(int p=ystep ; p < ysize ; p = p + ystep){
    	    if(y > p && y < p + ystep ){
    		yindex = (int) y/ystep ;
    		yfound=true;
    	    }
        }
        if(xfound && yfound){
    	    Marked[xindex][yindex] = true;
	    repaint();
	}
    }
    public void mouseWheelMoved(MouseWheelEvent evt){
        int d = evt.getWheelRotation();
        if(d < 0){
	    yindex--;
	    if(yindex < 1){yindex =  1;}
	    Marked[xindex][yindex] = true;
        }
        else
        {
	    yindex++;
	    if(yindex > ylines ){yindex = ylines ;}
	    Marked[xindex][yindex] = true;
        }
        repaint();
    }

    public synchronized void mouseDragged(MouseEvent evt){
        int x =(int) (evt.getX())/xstep;
        int y =(int) (evt.getY())/ystep;
        if(x>=0 && y>=1 && x<=xlines && y<=ylines){
    	    if(Editable[x][y]){ // only delete if editable 
    		Marked[x][y] = true;
    		charArray[x][y]="";
    		deleteDecimalPoint(x,y);
    		marked_color=deletecolor;
    		xindex = x;
    		yindex = y;
    		repaint();
    	    }
        }
    }
    
    public synchronized void mouseReleased(MouseEvent evt){}
    public void mouseEntered(MouseEvent evt){requestFocus();}
    public void mouseExited(MouseEvent evt){}
    public void mouseMoved(MouseEvent evt){requestFocus();}
    public void mouseClicked(MouseEvent evt){
        int x =(int) (evt.getX())/xstep;
        int y =(int) (evt.getY())/ystep;
        if(x>=0 && y>=0 && x<=xlines && y<=ylines){
    	    if(Editable[x][y]){
    		xindex = x;yindex = y;
    		Marked[xindex][yindex] = true;
    		repaint();
    	    }
        }
    }
    public int[] ForbiddenKeys={16,17,18,19,20,27,112,113,114,115,116,117,118,119,120,145,154,155};

    public void keyPressed(KeyEvent e){
	int key = e.getKeyCode();
	//System.out.println("key="+key);
	for(int i = 0 ; i < 18 ; i++){
	    if(key == ForbiddenKeys[i]){return;}
	}
	ProcessKey( key, Character.toString(e.getKeyChar()) );
	repaint();
    }
    
    public void keyTyped(KeyEvent e ) {}
    public void keyReleased(KeyEvent e){}

    public void ProcessKey(int key,String c){
	    if(key == 46 || key == 44){if(Editable[xindex][yindex] == true){if(reverse){drawPoint(xindex+1,yindex,pointsize,true);}else{drawPoint(xindex,yindex,pointsize,true);}Marked[xindex][yindex] = true;}}
	    else
	    if(key == 32){ // space
		xindex++;;
		if( xindex > xlines - 1){
		    xindex = 0;
		    yindex++;
		    if(yindex > ylines ){yindex = ylines;}	
		}
		Marked[xindex][yindex] = true;
	    }
	    else
	    if(key == 10){ // enter -> newline : starts under the first char of previous line
		int[] word = findLastWordLength();
		yindex++;
		if(reverse && left_to_right == false ){xindex = word[2];}else{xindex = word[1];}
		if(yindex > ylines  ){yindex = ylines;}
		Marked[xindex][yindex] = true;
	    }
	    else
	    if(key == 37){ // leftarrow
		xindex--;
		if(xindex < 0){xindex = xlines;
		    yindex--;
		    if(yindex < 1){yindex = 1;}// not smaller than 1 : control button space...
		}
		Marked[xindex][yindex] = true;
	    }
	    else
	    if(key == 39){ // rightarrow
		xindex++;
		if( xindex > xlines - 1){
		    xindex = 0;
		    yindex++;
		    if(yindex > ylines ){yindex = ylines;}	
		}
		Marked[xindex][yindex] = true;
	    }
	    else
	    if( key == 36){// home;
		yindex = 1;
		xindex = 1;
		Marked[xindex][yindex] = true;
	    }
	    else	    
	    if( key == 35){// end;
		yindex = ylines ;
		Marked[xindex][yindex] = true;
	    }
	    else	    
	    if( key == 38 || key == 33){// arrow up
		yindex--;
		if(yindex < 1){yindex = 1;}
		Marked[xindex][yindex] = true;
	    }
	    else
	    if( key == 40 || key == 34 ){// arrow down
		yindex++;
		if(yindex > ylines ){yindex = ylines ;}
		Marked[xindex][yindex] = true;
	    }	    
	    else
	    if( (key == 8 ) && Editable[xindex][yindex]){//backspace
		charArray[xindex][yindex] = "";
		deleteDecimalPoint(xindex,yindex);
		xindex--;
		if(xindex < 0){
		    xindex = xlines - 1;
		    yindex--;
		    if(yindex < 1){ yindex = 1;}
		}
		Marked[xindex][yindex] = true;
	    }
	    else
	    if( ( key == 127 )  && Editable[xindex][yindex]){// delete
		charArray[xindex][yindex] = "";
		Marked[xindex][yindex] = true;
		deleteDecimalPoint(xindex,yindex);
	    }
	    else
	    {
		if(Editable[xindex][yindex]){
		    charArray[xindex][yindex] = c;
		    if(reverse && left_to_right == false ){
			if(xindex < xlines ){
			    xindex = xindex - 1;
			    if (xindex<0 ){
				int[] pos = findLastWordLength();
			        xindex = pos[2];
				yindex++; 
				if( yindex > ylines ){ yindex = ylines;}
			    }
			}
		    }
		    else
		    {
			if(xindex < xlines ){
			    xindex = xindex + 1;
			    if (xindex>=xlines ){
				int[] pos = findLastWordLength();
			        xindex = pos[1];
				yindex++; 
				if( yindex > ylines ){ yindex = ylines;}
			    }
			}
		    }	
		    Marked[xindex][yindex] = true;
		}
		
	    }
	    marked_color = markcolor;
    }

    public void Clear(){
	for(int x = 0; x < xlines+1; x++){
	    for(int y = 0; y < ylines+1 ; y++){
		if(Editable[x][y]){ // only clear if editable
		    charArray[x][y]="";
		    Marked[x][y]=false;
		}
	    }
	}
	repaint();
    }

    public int[] findLastWordLength(){
	int[] word = new int[4];
	int cnt;int thisline=0;int max=0;
	for(int y = 0 ; y < ylines ; y++){
	    cnt=0;
	    for(int x=0 ; x < xlines ; x++){
		if(charArray[x][y] != ""){cnt++;}
	    }
	    if(cnt>0){
		thisline = y;
		if( cnt > max ){ max = cnt;}
	    }
	}
	int begin=0;
	int end=xlines;
	for(int x = 0 ; x<xlines;x++){ // from left to word
	    if(charArray[x][thisline] != "" ){begin = x;x=xlines;}
	}
	for(int x = xlines - 1  ; x >= 0;x--){//from right to word
	    if(charArray[x][thisline] != "" ){ end = x; x=-1;}
	}
	word[0] = thisline;
	word[1] = begin;
	word[2] = end;
	word[3] = max;
	return word;
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

    public void deleteAllLines(){
	int s = objcnt - 1;
    	for( int i = s ; i >=0; i--){
	    if(EditableObject[i]){
    		objects.removeElementAt(i);
    		objcnt--;
	    }
	}
	repaint();
    }

    public void deleteLine(){
	if(objcnt <= 0){objcnt = 0; return;}
	if( (EditableObject[objcnt - 1]) == true){
	    try{
		objcnt--;
		objects.removeElementAt(objcnt);
		repaint();
	    }
	    catch(Exception e){System.out.println("can not remove line ... objcnt = "+objcnt+"\nerror : "+e);}
	}
    }

    public void deleteAllPoints(){
	int m = pointcnt - 1;
    	for( int i = m ; i >=0; i--){
    	    if( (((DecimalPoint)decimalpoints.elementAt(i)).getEditable()) == true ){ 
    		decimalpoints.removeElementAt(i);
    		pointcnt--;
    	    }
	}
	repaint();
    }
    
    public int findPoint(int y){ // find the x- location of a decimal point at line y : only 1 point allowed...
	for(int i = 0 ; i<pointcnt;i++){
	    if( (DecimalPoint)decimalpoints.elementAt(i) != null ){
		if( (((DecimalPoint)decimalpoints.elementAt(i)).getY()) == y ){
		    return (int) (((DecimalPoint)decimalpoints.elementAt(i)).getX());
		}
	    }
	}
	return -1;
    }
    
    public void deleteDecimalPoint(int x,int y){
    	for( int i = 0 ; i < pointcnt; i++){
    	    if(
    		(((DecimalPoint)decimalpoints.elementAt(i)).getX()) == x 
		&& 
		(((DecimalPoint)decimalpoints.elementAt(i)).getY()) == y
		&&
		(((DecimalPoint)decimalpoints.elementAt(i)).getEditable()) == true
	    ){
		try{
		    decimalpoints.removeElementAt(i);
		    pointcnt--;
		    repaint();
		    return;
		}catch(Exception e){System.out.println("poincnt "+pointcnt+"\terror="+e);}
	    }
	}
    }
    
    public void destroy() {}
    
    public void start(){}

    public void drawPoint(int x, int y, int pointsize,boolean can_be_edited){
	try{
	    decimalpoints.add(new DecimalPoint(x,y,pointsize,can_be_edited));
	    pointcnt++;
	}
	catch(Exception e){System.out.println(e);}
    }
    
    public void drawLine(int x1,int y1,int x2,int y2,Color c,String s, boolean edit , boolean is_segment){
	try{
	    objects.add(new GridLine(x1,y1,x2,y2,c,s,edit,is_segment));
	    objcnt++;
	}
	catch(Exception e){System.out.println(e);}
    }
    
    public void actionPerformed(ActionEvent e){
	String command = e.getActionCommand();
	if(command.equals(clear_all)){
	    Clear();
	    deleteAllLines();
	    deleteAllPoints();
	}
	else
	{
	    if(command.equals(line_plus)){
		int[] word = findLastWordLength();
		if(word[0] > 1){
		    drawLine(word[1]*xstep,(word[0]+1)*ystep,(word[2]+1)*xstep,(word[0]+1)*ystep,linecolor,"+",true,true);//+
		}
	    }
	    else
	    {
		if(command.equals(line_min)){
		    int[] word = findLastWordLength();
		    if(word[0] > 1){
			drawLine(word[1]*xstep,(word[0]+1)*ystep,(word[2]+1)*xstep,(word[0]+1)*ystep,linecolor,"-",true,true);//+
		    }
		}
		else
		{
		    if(command.equals(line_div)){
			int[] word = findLastWordLength();
			if(word[0] > 1){
			    drawLine(word[1]*xstep,(word[0]+1)*ystep,(word[2]+1)*xstep,(word[0]+1)*ystep,linecolor,"\u00F7",true,true);//+
			}
		    }
		    else
		    {
			if(command.equals(line_times)){
			    int[] word = findLastWordLength();
			    if(word[0] > 1){
				drawLine(word[1]*xstep,(word[0]+1)*ystep,(word[2]+1)*xstep,(word[0]+1)*ystep,linecolor,"\u00D7",true,true);
			    }
			}
			else
			{
			    if(command.equals(delete_line)){
				deleteLine();
			    }
			}
		    }
		}
	    }
	}
	repaint();
    }
    
    public void getButtons(){
	String param = getParameter("buttons");
	Panel panel;
	if( param != null && param.length()>0){
	    panel = InvulGridPanel(param);
	}
	else
	{
	    panel = InvulGridPanel("none");
	}
	cp = getContentPane();
	cp.add(panel);
    }

    public Panel InvulGridPanel(String param){
	Panel panel = new Panel();
	setLayout(new FlowLayout());
	Button s;
	if(!param.equals("none")){
    	    StringTokenizer label = new StringTokenizer(param, ",");
    	    int m = label.countTokens();String operator="";
    	    String txt="";
    	    for(int i = 0; i < m ;i++){
		operator = label.nextToken();
		if(operator.equals("+")) txt = line_plus;
		else
		if(operator.equals("-")) txt = line_min;
		else
		if(operator.equals(":")) txt = line_div;
		else
		if(operator.equals("x")) txt = line_times;
		s = new Button(txt);
		s.addActionListener(this);
		add(s);
	    }
	    s = new Button(delete_line);
	    s.addActionListener(this);
	    add(s);
	}
	s = new Button(clear_all);
	s.addActionListener(this);
	add(s);
	return panel;
    }

    public static void main(String[] args) {
	run(new InvulGrid(), 300, 300);
  }

    public static void run(JApplet applet, int width, int height) {
	JFrame frame = new JFrame();
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.getContentPane().add(applet);
	applet.init();
	applet.start();
	frame.setVisible(true);
    }


}


class DecimalPoint {
    int x,y;
    int linewidth;
    boolean editable;
    
    public DecimalPoint(int x,int y, int linewidth,boolean editable){
	this.x = x;
	this.y = y;
	this.linewidth = linewidth;
	this.editable = editable;
    }
    
    public int getX(){return x;}
    public int getY(){return y;}
    public int getD(){return linewidth;}
    public boolean getEditable(){return editable;}
}

class GridLine {
    int x1,y1,x2,y2;
    Color color;
    String txt;
    boolean edit;
    boolean segment;

    public GridLine(int x1, int y1 , int x2 , int y2, Color color, String txt,boolean edit, boolean segment){
	this.x1 = x1;
	this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
        this.txt = txt;
        this.edit = edit;
        this.segment = segment;
   }
   public int getX1() { return x1; }
   public int getY1() { return y1; }
   public int getX2() { return x2; }
   public int getY2() { return y2; }
   public Color getColor(){ return color;}
   public String getText() { return txt;}
   public boolean getEdit() { return edit;}
   public boolean isSegment() { return segment;}
}

