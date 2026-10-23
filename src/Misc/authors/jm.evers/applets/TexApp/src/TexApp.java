/*

*********************************************************************************
* J.M. Evers 19/11/2009								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatoever								*
*********************************************************************************

ATP library is GNU :see ./atp/gpl.txt

Example:
<html>
    <body bgcolor=#0000ff onload="javascript:SetSize('TexApp');">
        <script type="text/javascript">                                                 
	function remove(t){
	    document.getElementById(t).width = 1;
	    document.getElementById(t).height = 1;	
	}
	                                                       
    	function SetSize(t){                         
	    var size = new Array(2);
	    try{
		size = document.getElementById(t).getPrefferedSize();
		document.getElementById(t).width = size[0];
		document.getElementById(t).height = size[1];
		document.getElementById('showme').innerHTML='<hr><font color=red><b>preffered height = '+size[1]+'<br>preffered width = '+size[0]+'</b></font>';
	    }
	    catch(e){
	    	document.getElementById('showme').innerHTML='<hr><font color=red><b>Your browser does not support automatic resizing...</b></font>';
	    }    
    	
	} 
    	function ReadApplet(t){                         
	    var reply = document.getElementById(t).ReadApplet();
	    document.getElementById('showme').innerHTML='<hr><font color=red><b>'+reply+'</b></font>';
        } 
	</script>
	<div id="showme"></div>
	<table ><tr valign=middle ><th>
	<font color=red>
	this is some text...
	</font>
	</th>
	<th>
	<applet id="TexApp" codebase="dist" archive="TexApp.jar" code="TexApp.class" width="1" height="1"><!-- automatic resizing -->
	    <param name="latex0" value="#ffffff,\frac{4}{7} \cdot,#a0c0f0, \frac{3}{4} \: =\:,input,\:\approx\:,input">  
	    <param name="latex1" value="vspace"><!-- a blanc line : 50% of inputheight -->
	    <param name="latex2" value="#00ccff"><!-- change latex fgcolor --> 
            <param name="latex2" value="\left[ \begin{array}{l} x = \frac{3}{4} \\ y = \frac{5}{6}\end{array}\right. \rightarrow \frac{x}{y} = ,input">
            <param name="latex3" value="\left\{ \begin{array}{l} x = \frac{3}{4} \\ y = \frac{5}{6} \\ z = \frac{\pi}{\sqrt{2}}\end{array}\right. \rightarrow \frac{x}{y} = ,input,#ffffff,\rightarrow z \cdot \frac{x}{y} = ,input">
	    <!-- optional : colors and sizes of latex/inputfields -->
	    <param name="status" value="waiting"><!-- string : wims variable $status  done/waiting -->
	    <param name="latex_bgcolor" value="0,0,255"><!-- rgb or #ffffff default white -->
	    <param name="latex_fgcolor" value="255,0,0"><!-- rgb or #ffffff  default black -->
	    <param name="latex_fontsize" value="18"><!-- rgb  or #ffffff  default black -->
	    <param name="inputfield_width" value="120,34,56,67"><!-- int : default 60 : width in px of inputfields-->
	    <param name="inputfield_height" value="40"><!-- optional int : default fontsize in px : height in px of inputfield-->
	    <param name="inputfield_fontsize" value ="18"><!-- int : default 10 -->
	    <param name="inputfield_bgcolor" value="0,0,255"><!-- rgb or #ffffff  default white -->
	    <param name="inputfield_fgcolor" value="255,0,0"><!-- rgb or #ffffff  default black -->
	    <param name="inputfield_editable" value="1,0,1"><!-- field 1 is editable, field 2 is not_editable... default all editable -->
	    <param name="inputfield_values" value="12,44,,22"><!--  optional for correct answers: field 1 = "12", field 2 = "44" field3= "" field4 = "22" -->
	    NO JAVA
	</applet>
	</th>
	<th>
	<font color=red>
	and this is the rest...</font>
	</th></tr>
	</table>
	<p>
	<font color=white>
	For applet autoscaling, the applet_tag "width" should be less than 10 : width="1"<br>
	The autoscaling SetSize() javascript_function could be called from the htmlpage "body onload".<br>
	Or in WIMS:<br>
	!set wims_html_onload=javascript:SetSize('TexApp');
	<br>
	This will probably not work for Opera browsers...                                                                                  
        <br>                                                                                                                               
        Safari on Windows will incorrectly scale the applet...
	<br>
	If the applet has a width &gt; 10 , no autoscaling is performed.
	</font>
	<br>
	<input type="button" name="ReadApplet()" value="ReadApplet()" onclick="javascript:ReadApplet('TexApp');">
	<input type="button" name="SetSize()" value="SetSize()" onclick="javascript:SetSize('TexApp');">
	<input type="button" name="remove()" value="remove()" onclick="javascript:remove('TexApp');">
    </body>
</html>


*/    

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.StringTokenizer;
import atp.sHotEqn; 

public class TexApp extends Applet implements Runnable{

    public void init(){
    	setLayout(null);
        Dimension appletSize = getSize();
	if( appletSize.width < 10 ){ resize = true; xsize = 0; ysize = 0; }
	else { resize = false; xsize = appletSize.width; ysize = appletSize.height; }
    	latex_bgcolor = colorParam("latex_bgcolor", Color.white);
	latex_fgcolor = colorParam("latex_fgcolor", Color.black);
	f1 = getInt("latex_fontsize",16);
	f2 = f1-2;
	f3 = f1-4;
	f4 = f1-6;
	// set a few params
	inputwidth = getInputWidth("inputfield_width" , 30);
	fontsize = getInt("inputfield_fontsize", 12);
	inputheight = getInt("inputfield_height",fontsize);
	if(fontsize > inputheight - 4){
	    fontsize = inputheight - 4; if(fontsize < 6 ){ fontsize = 6;}
	}
	myfont = new Font("Utopia", 1, fontsize);
	textfield_bgcolor = colorParam("inputfield_bgcolor", Color.blue);
    	textfield_fgcolor = colorParam("inputfield_fgcolor", Color.black);
	getEditable("inputfield_editable",30);
	getValues("inputfield_values",30);
	Get_Latex();
	setBackground(latex_bgcolor);
    }

    public void run(){
    }
    
    public int getPrefferedWidth(){ 
    // auto resize applet !
    // javascript reading preffered size of applet: use in wims_html_onload
    // only called if width_applet_tag < 10
	return xsize;
    }
    public int getPrefferedHeight(){ 
    // auto resize applet !
    // javascript reading preffered size of applet: use in wims_html_onload
    // only called if width_applet_tag < 10
	return ysize;
    }
    
    public Color MakeTransparent(Color color, int i){
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), i);
    }

    public Color DecodeColor(String s1 , Color color){
	try{ color = Color.decode( s1 );}catch(Exception e){System.out.println("could not parse "+s1);}
	return color;
    }
    
    public Color colorParam(String s, Color color){
        String s1 = getParameter(s);
        if(s1 != null && s1.length() != 0){
            s1 = s1.replaceAll(":", ",");
            s1 = s1.replaceAll(";", ",");
	    if( s1.indexOf(',') > 0 ){
        	StringTokenizer stringtokenizer = new StringTokenizer(s1, ",");
        	int i = stringtokenizer.countTokens();
        	if(i < 3 || i > 4){ return color;}
        	int ai[] = new int[i + 1];
        	for(int j = 0; j < i; j++){
            	    ai[j] = Integer.parseInt(stringtokenizer.nextToken());
            	    if(ai[j] > 255 || ai[j] < 0){ ai[j] = 0;}
        	}
		if(i == 3)
            	    color = new Color(ai[0], ai[1], ai[2]);
        	else
            	    color = new Color(ai[0], ai[1], ai[2], ai[3]);
    	    }
	    else
	    {
		try{ color = Color.decode( s1 );}catch(Exception e){System.out.println("could not parse "+s);}
		return color;
	    }
	}
        return color;
    }

    public void getValues(String s, int inp){
        String s1 = getParameter(s);
        values = new String[inp];
        for(int i = 0; i < inp; i++){ values[i] = " ";}
	if( s1 != null && s1.length() != 0){
	    s1 = s1.replaceAll(":", ",");
	    s1 = s1.replaceAll(";", ",");
	    s1 = s1.replaceAll(",", " ,");
	    StringTokenizer stringtokenizer = new StringTokenizer(s1, ",");
	    int j = stringtokenizer.countTokens();
    	    for(int k = 0; k < Math.min(j,inp) ; k++){
        	values[k] = stringtokenizer.nextToken();
	    }
	}
    }

    public boolean getStatus(){
        String s1 = getParameter("status");
	if( s1 != null && s1.length() != 0){
	    if( s1.equals("done")){
		return true;
	    }
	}
	return false;
    }

    public void getEditable(String s , int inp){
        editable = new boolean[inp];
	if( getStatus() ){ // status = done : no editing allowed
    	    for(int i = 0; i < inp; i++){ editable[i] = false ;}
	}
	else
	{
    	    String s1 = getParameter(s);
    	    for(int i = 0; i < inp; i++){ editable[i] = true;}
	    if(s1 != null){
    		s1 = s1.replaceAll(":", ",");
    	        s1 = s1.replaceAll(";", ",");
    		StringTokenizer stringtokenizer = new StringTokenizer(s1, ",");
    	        int j = stringtokenizer.countTokens();
		for(int k = 0; k < Math.min(inp,j); k++)
    		{
		    try{
        		String s2 = stringtokenizer.nextToken();
    	    	        if(s2.equals("0") || s2.equalsIgnoreCase("no"))
            	    	    editable[k] = false;
        		else
            		    editable[k] = true;
		    }
		    catch(Exception e){}
    		}
	    }
	}
    }

    public int[] getInputWidth(String s , int inp){
        inputwidth = new int[inp];
	String s1 = getParameter(s);
	for(int i = 0; i < inp; i++){ inputwidth[i] = DEFAULT_WIDTH;}
	if(s1 != null){
    	    s1 = s1.replaceAll(":", ",");
    	    s1 = s1.replaceAll(";", ",");
    	    StringTokenizer stringtokenizer = new StringTokenizer(s1, ",");
    	    int j = stringtokenizer.countTokens();
	    for(int k = 0; k < Math.min(inp,j); k++){
		try{ inputwidth[k] = (int) Integer.parseInt(stringtokenizer.nextToken());}
		    catch(Exception e){ System.out.println(" can not parse inteter parameter "+s);}
	    }
	}
	return inputwidth;
    }

    public int getInt(String s, int i){
        String s1 = getParameter(s);
	if( s1 != null && s1.length()!=0){
    	    try{
        	i = Integer.parseInt(s1);
    	    }
    	    catch(Exception exception){
        	System.out.println(" can not parse parameter "+s);
    	    }
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
    
    public String[] GrowStringArray(String[] array, int newlength){
        String[] grow = new String[ newlength ];                                                                                                    
        for(int i = 0; i < array.length; i++){
    	    grow[i] = array[i];
        }                                                                                                                                  
        return grow;                                                                                                                       
    }   

    public int[] GrowIntArray(int[] array, int newlength){
        int[] grow = new int[ newlength ];                                                                                                    
        for(int i = 0; i < array.length; i++){
    	    grow[i] = array[i];
        }
        return grow;
    }   

    public BufferedImage[] GrowImageArray(BufferedImage[] array, int newlength){
        BufferedImage[] grow = new BufferedImage[ newlength ];  
        for(int i = 0; i < array.length; i++){
    	    grow[i] = array[i];
        }
        return grow;
    }   
    
    public TextField[] GrowTextFieldArray(TextField[] array, int newlength){
        TextField[] grow = new TextField[ newlength ];                                                                                                    
        for(int i = 0; i < array.length; i++){
    	    grow[i] = array[i];
        }

	if(newlength > 29){
	    getEditable("inputfield_editable",newlength);
	    getValues("inputfield_values",newlength);
	}
        return grow;
    }
    public void Get_Latex(){
	int linecnt = 0;
	String r = getParameter("latex"+linecnt);
	if(r == null || r.length() == 0){
	    System.out.println("can not read parameter \"latex\"");
	    return;
	}
	int max_icons=6; // just reasonable numbers: will be increased if nessecary
	int max_inputs=6;
	icon = new BufferedImage[max_icons];
	Xlatex = new int[max_icons];
	Ylatex = new int[max_icons];
	inputfield = new TextField[max_inputs];
	int Xinput[] = new int[max_inputs]; 
	int icons = 0;
	Xlatex[0] = 0;
	Ylatex[0] = 0;
	int tmp_xsize = 0;
	int tmp_ysize = 0;
	inputs = 0;
	int actualsize[] = new int[2];
	actualsize[0] = inputwidth[0];
	actualsize[1] = inputheight;
	String tmp;
	int max_tmp_ysize = 0;
	int min_tmp_ysize = 0;
	int current_xsize = 0;
	int token_cnt=0;
	int line_icons=0;
	int line_inputs=0;
	while( r != null && r.length() != 0 ){
	    if( icons >= max_icons){
		icon = GrowImageArray(icon, icons+1);
		Xlatex = GrowIntArray(Xlatex, icons+1);
		Ylatex = GrowIntArray(Ylatex, icons+1);
		max_icons = icons;
	    }
	    current_xsize=0;
	    if(r.indexOf("input") != -1){ 
		// images and inputfields on the same horizontal line 
		// syntax \frac{2}{6}=,input
		StringTokenizer stringtokenizer_r = new StringTokenizer(r, ",");
		max_tmp_ysize = 0;
		min_tmp_ysize = inputheight;
		line_icons=icons;
		line_inputs=inputs;
		token_cnt =  stringtokenizer_r.countTokens();
		for(int k = 0 ; k < token_cnt; k++){
		    tmp = stringtokenizer_r.nextToken();
		    if(tmp.equals("input")){
			if(inputs >= max_inputs){
			    inputfield = GrowTextFieldArray(inputfield, inputs+1);
			    Xinput = GrowIntArray(Xinput,inputs+1);
			    max_inputs = inputs;
			    if(max_inputs>29){System.out.println("hardcoded maximum of 30 inputfields !");}
			}
			Xinput[inputs] = current_xsize;
			current_xsize = current_xsize + inputwidth[inputs];
			inputs++;
		    }
		    else
		    {
			if(tmp.indexOf("#") == 0){// #a0a0e5 colorcode <param name="latex3" value="\pi,#a0f0a0,\alpha,#ff00ff,\beta">
			    latex_fgcolor = DecodeColor(tmp,latex_fgcolor);
			}
			else
			{ // processing latex images
			    if( icons >= max_icons){
				icon = GrowImageArray(icon, icons+1);
			        Xlatex = GrowIntArray(Xlatex, icons+1);
				Ylatex = GrowIntArray(Ylatex, icons+1);
			        max_icons = icons;
			    }
			    sHotEqn shoteqn = new sHotEqn();
			    shoteqn.setFontsizes(f1,f2,f3,f4);
			    shoteqn.setEquation(tmp);
			    icon[icons] = shoteqn.mkImage(latex_bgcolor, latex_fgcolor,400,400, false, 1, 1, false);
			    // make an image 400x400 : should be enough...
			    actualsize = shoteqn.getActualSize();
			    // find borders and remap
			    icon[icons] = shoteqn.mkImage(latex_bgcolor, latex_fgcolor, actualsize[0], actualsize[1], false, 1, 1, false);
			    Xlatex[icons] = current_xsize;
			    Ylatex[icons] = actualsize[1];
			    current_xsize = current_xsize + actualsize[0];
			    icons++;
			    if(actualsize[1] > max_tmp_ysize){max_tmp_ysize = actualsize[1];}
			    if(actualsize[1] < min_tmp_ysize){min_tmp_ysize = actualsize[1];}
			}
		    }
		}
		// trying to center around the line max_tmp_ysize/2
		for( int i = line_icons  ; i < icons ;i++){
		    Ylatex[i] = tmp_ysize + Math.abs((max_tmp_ysize - Ylatex[i])/2);
		}
		int corr = Math.abs((max_tmp_ysize - min_tmp_ysize)/2);
		for( int i = line_inputs ; i < inputs ; i++){
		    inputfield[i] = new TextField(values[i],inputwidth[i]);
		    inputfield[i].setFont(myfont);
		    inputfield[i].setBackground(textfield_bgcolor);
		    inputfield[i].setForeground(textfield_fgcolor);
		    inputfield[i].setEditable(editable[i]);
		    inputfield[i].setBounds(Xinput[i],tmp_ysize+corr-5,inputwidth[i],min_tmp_ysize+5);
		    inputfield[i].setVisible(true);
		    this.add(inputfield[i]);
		}
		if(current_xsize > tmp_xsize){ tmp_xsize = current_xsize;}
		tmp_ysize = tmp_ysize + max_tmp_ysize;
	    }
	    else // no inputfield defined ; just lateximages
	    {
		if(r.equals("vspace")){
		    tmp_ysize = (int) (tmp_ysize + inputheight/2);		
		}
		else
		{
		    if(r.indexOf("#") == 0){// #a0a0e5 colorcode for a new latex_fgcolor <param name="latex4" value="#ff00ff">
			latex_fgcolor = DecodeColor(r,latex_fgcolor);
		    }
		    else
		    { // this will have a single color...latex_fgcolor...it's too much trouble to alter :(
			sHotEqn shoteqn = new sHotEqn();
		        shoteqn.setFontsizes(f1,f2,f3,f4);
			shoteqn.setEquation(r);
		        icon[icons] = shoteqn.mkImage(latex_bgcolor, latex_fgcolor,400,400, false, 1, 1, false);
			// make an image 400x400 : should be enough...
		        actualsize = shoteqn.getActualSize();
			// find borders and remap
		        icon[icons] = shoteqn.mkImage(latex_bgcolor, latex_fgcolor, actualsize[0], actualsize[1], false, 1, 1, false);
			Xlatex[icons] = 0;
			Ylatex[icons] = tmp_ysize;
			if(actualsize[0] > tmp_xsize){ tmp_xsize = actualsize[0];}
			tmp_ysize = tmp_ysize + actualsize[1];
			icons++;
		    }
		}
	    }
	    linecnt++;
	    r = getParameter("latex"+linecnt);
	}
	if(resize){ // width / height are less then 10 px
	    xsize = tmp_xsize; ysize = tmp_ysize;
	    System.out.println("resizing appletwindow to "+tmp_xsize+"x"+tmp_ysize);
	}
	else
	{ // applet param width / height are set to "real values"
	    if(tmp_xsize > xsize){System.out.println("ERROR: applet window is too small...increase appletparam \"width \" to "+tmp_xsize+" px");xsize = tmp_xsize;}
	    if(tmp_ysize > ysize){System.out.println("ERROR: applet window is too small...increase appletparam \"height\" to "+tmp_ysize+" px");ysize = tmp_ysize;}
	}
    }
    
    public void paint(Graphics g)
    {
	update(g);
    }

    public void update(Graphics g)
    {
	for(int p = 0 ; p < icon.length ; p++)
	{
	    g.drawImage(icon[p],Xlatex[p],Ylatex[p],this);
	}
    }

    public String ReadApplet(){
	System.out.println("ReadApplet()\n");
	String reply="";
	if(inputs > 0){
	    try{
	    reply = inputfield[0].getText();
	    for(int i = 1 ; i < inputs ; i++)
	    {
		reply = reply + "," + inputfield[i].getText();
	    }
	    }catch(Exception e){System.out.println("ReadApplet error :"+e);}
	}
	else
	{
	    reply = "error no inputfields defined via params...";
	}
	return reply;
    }


    public void start(){}

    public void stop(){}

    public void destroy(){}

    TextField inputfield[];
    public int[] prefferedsize;
    public int inputs,Xlatex[],embed,Ylatex[],inputheight=0,xsize,ysize,fontsize,f1,f2,f3,f4;
    public Color textfield_bgcolor,textfield_fgcolor,latex_bgcolor,latex_fgcolor;
    public String Latex[],values[];;    
    boolean editable[],resize;
    public sHotEqn HSD;
    BufferedImage icon[];
    Font myfont;
    int[] inputwidth;
    int DEFAULT_WIDTH = 60;
}
