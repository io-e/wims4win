/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2005 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
  
    
    11/2013 rewrite based on SketchEl 1.21: (08/Nov/2008) to include export of SVG.
    
    More recent verions than 1.21 do not improve the Applet or SVG export.
    
    jm.evers
*/  
package WIMSchem;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.net.*;
import java.applet.AppletContext;
import java.text.*;

public class MainApplet extends JApplet implements ComponentListener
{
    MainPanel mainPanel=null;
    long id;
    String zoom_js;
    public static String g_id = "g_SVG_1000000";/* used for SVG zoom in/out */
    public static String svg_id = "SVG_1000000";/* used for SVG zoom in/out */
    /* jm.evers addition to configure applet */
    static String[] TOOLS={"TOOL_CURSOR","TOOL_ROTATOR","TOOL_ERASOR","TOOL_DIALOG","TOOL_EDIT",
    "TOOL_SETATOM","TOOL_SINGLE","TOOL_DOUBLE","TOOL_TRIPLE","TOOL_ZERO",
    "TOOL_INCLINED","TOOL_DECLINED","TOOL_UNKNOWN","TOOL_CHARGE","TOOL_UNDO",
    "TOOL_REDO","TOOL_TEMPLATE","TOOL_CUT","TOOL_COPY","TOOL_PASTE",
    "TOOL_UNSELECT","TOOL_SELECT"};
    static String[] MENUS={"MENU_BLOCK","MENU_SELECT","MENU_TRANSFORM","MENU_ZOOM","MENU_SHOW","MENU_HYDROGEN","MENU_STEREO","MENU_HELP"};
    static int TOOL_COUNT = TOOLS.length;
    static int MENU_COUNT = MENUS.length;
    public static String[] templateURL;
    public static AppletContext applet_context;
    public static String[] myAtoms;
    public static int rotation;
    public static boolean viewC;
    public static boolean viewH;
    public static boolean[] TOOL_SELECTION;
    public static boolean[] MENU_SELECTION;
    public static boolean USER_SELECTION = false;
    public static int GLOBAL_ALPHA = 140; 
    public static int ATOM_SELECT_HTML_COLOR; /* the default Atom click colour for user interaction when selecting atoms */
    public static int BOND_SELECT_HTML_COLOR; /* the default bond click colour for user interaction when selecting atoms */
    public static Color ATOM_SELECT_COLOR;/* the colour for not-by-param-selected-atoms */
    public static Color BOND_SELECT_COLOR;/* the colour for not-by-param-selected-bonds */
    public static int[] ExternalAtomSelection; /* atom line numbers in MDLmol file */
    public static Color SelectedAtomColorArray[];/* atom corresponding colours for line numbers in MDLmol file */
    public static int[] ExternalBondSelection;/* bonding line numbers in MDLmol file */
    public static Color SelectedBondColorArray[];;/* corresponding bonding colours for line numbers in MDLmol file */
    public static int SelectedAtomColorInt[]; /* int color numbers set by params used in svg export */
    public static int SelectedBondColorInt[]; /* colors set by params used in svg export */
    public static boolean ATOM_BUTTONS = true; /* button row with atoms SOUTH */
    public static boolean SUPERUSER_SELECTION = false; /* if true the exported SVG will be coloured according params */
    
    public static String language;
    public void init(){
	ATOM_BUTTONS = getBool("atom_button_row",false); /* if set an extra row of buttons will be shown  */
	USER_SELECTION = getBool("user_selection",false);
	if( USER_SELECTION ){
	    GLOBAL_ALPHA = getInt("color_alpha",140);
	    /* some people forget the importance of alpha...this should be removed */
	    if( GLOBAL_ALPHA > 250 ){GLOBAL_ALPHA = 140;}
	    ATOM_SELECT_COLOR = getColor("default_atom_select_color",GLOBAL_ALPHA,255,0,0);/* RGB */
	    BOND_SELECT_COLOR = getColor("default_bond_select_color",GLOBAL_ALPHA,0,0,255);/* RGB */
	    ATOM_SELECT_HTML_COLOR = RGB2int("default_atom_select_color",0x0000ff);/* int for svg */
	    BOND_SELECT_HTML_COLOR = RGB2int("default_bond_select_color",0x00ff00);/* int for svg */
	}
	zoom_js = "";
	language = getLanguage();
	TOOL_SELECTION = getTools();
	MENU_SELECTION = getMenus();
	ExternalAtomSelection = SetAtomSelection();
	ExternalBondSelection = SetBondSelection();
	rotation = getInt("rotation",0);
	myAtoms = GetMyAtoms(); /* read parm atoms to tailomade atomselection*/
	templateURL = getTemplateURL();/* read params template1...template_n and use these insteadof default ones */
	viewC =  getBool("show_carbon",true);
	viewH = getBool("show_hydrogen",true);
	System.out.println("viewC = "+viewC+"\nviewH = "+viewH);
	getContentPane().setLayout(new BorderLayout());
    	mainPanel=new MainPanel(null,MainPanel.MODE_APPLET,null);
	getContentPane().add(mainPanel,BorderLayout.CENTER);
	addComponentListener(this);
	getFile();/* read param file and load it if param showfile is set */
	repaint(1000);
    }
    public String getAppletInfo() 
    {
    	return "WIMSchem: Applet version of chemistry\nmolecular diagram drawing tool.";
    }

    /* 
	replace the current molecule with the content of the string, which can be any of the formats which WIMSchem
        is able to read out from a file; returns true if successful
    */
    public boolean SetMoleculeNative(String Source) {
	try{
	    Molecule mol=MoleculeStream.readNative(new BufferedReader(new StringReader(Source.toString())));
	    mainPanel.setMolecule(mol);
	    mainPanel.repaint();
	    return true;
        } catch (IOException e) { System.out.println("problems parsing:\n"+Source.toString()+"\n"+e);}
        return false;
    }
    public boolean SetMoleculeMDLMol(String Source){
	try{
	    Molecule mol = MoleculeStream.readMDLMOL(new BufferedReader(new StringReader(Source.toString())));
	    mainPanel.setMolecule(mol);
	    mainPanel.repaint();
	    return true;
        } catch (IOException e) { System.out.println("problems parsing:\n"+Source.toString()+"\n"+e);}
        return false;
    }
    public boolean SetMolecule(String Source, boolean is_molfile) 
    {
    	try
	{
	    Molecule mol;
	    if(is_molfile){
	     mol = MoleculeStream.readMDLMOL(new BufferedReader(new StringReader(Source.toString())));
	    } 
	    else
	    {
	     mol = MoleculeStream.readUnknown(new BufferedReader(new StringReader(Source.toString())));
    	    }
    	    mainPanel.setMolecule(mol);
	    mainPanel.repaint();
	    return true;
	}
	catch (IOException e){ System.out.println("error in applet : setMolecule( String Source ) :\ncan not parse : "+Source.toString());}
	return false;
    }
    
    /* 
	appends the indicated molecular source to the current molecule, 
	in the same way as the paste feature; otherwise works
        the same as SetMolecule
    */
    public boolean AppendMolecule(String Source, boolean is_molfile)
    {
    	try
	{
	    Molecule mol;
	    if(is_molfile){
	     mol = MoleculeStream.readMDLMOL(new BufferedReader(new StringReader(Source.toString())));
	    } 
	    else
	    {
	     mol = MoleculeStream.readUnknown(new BufferedReader(new StringReader(Source.toString())));
    	    }
    	    mainPanel.addMolecule(mol);
	    mainPanel.repaint();
	    return true;
	}
	catch (IOException e) { System.out.println("error in applet : appendMolelcule( String Source ) : "+e.toString());}
	
	return false;
    }
    
    /* 
	return the string representation of the molecule, in WIMSchem native format 
    */
    public String GetMoleculeNative()
    {
    	try
	{
	    StringWriter sw=new StringWriter();
	    BufferedWriter bw=new BufferedWriter(sw);
	    MoleculeStream.writeNative(bw,mainPanel.molData());
	    return sw.toString();
	}
	catch (IOException e) {}
	
	return null;
    }
    
    /* 
	return the string representation of the molecule, in MDL MOL-file format 
    */
    public String GetMoleculeMDLMol()
    {
    	try
	{
	    StringWriter sw = new StringWriter();
	    BufferedWriter bw = new BufferedWriter(sw);
	    MoleculeStream.writeMDLMOL(bw,mainPanel.molData());
	    return sw.toString();
	}
	catch (IOException e) {}
	
	return "error getting MDLMol file from applet";
    }
    public int RGB2int(String p, int d){ /* convert 255,0,255 -> ff00ff*/
	String param = getParameter(p);
	if( param != null && param.length()>0 ){
    	    int k = 0;String hex = "";String tmp="";
    	    StringTokenizer q = new StringTokenizer(param, ",");
    	    int c = q.countTokens();
    	    if( c != 3){System.out.println("use R,G,B for colours ; use param color_alpha for transparency"); return d;}
    	    for( int a = 0; a < 3 ; a++){
		k = Integer.parseInt(q.nextToken(), 10);
		tmp = Integer.toHexString(k);
		while (tmp.length()<2) tmp="0"+tmp; 
		hex = hex+""+tmp;
            }
            return Integer.parseInt(hex,16);
	}
	else
	{
	    return d;
	}
    }

    public void componentHidden(ComponentEvent e){}
    public void componentMoved(ComponentEvent e){}
    public void componentResized(ComponentEvent e){}
    public void componentShown(ComponentEvent e){
    	mainPanel.scaleToFit();
	mainPanel.repaint();
    }
    /* 
	jm.evers additions to configuring applet 
    */
    public String getSelected(){
	int num=0;
	String reply="";
	Molecule mol = (mainPanel.editor).molData();
	for (int n=1;n<mol.numAtoms();n++){ 
	    if( (mainPanel.editor).selected[n-1] ){ reply = reply+mol.atomElement(n-1)+"\n";}
	}
	return reply; 
    }
    
    public String getSVG(String type){
	SVGMolecule svgmol;
	if( type.equals("1") ){ /* get the student reply*/
	    try{
		System.out.println("ok : exporting user drawing to SVG");
		svgmol = new SVGMolecule((mainPanel.editor).molData());
	    }catch(Exception e){return e.toString();}
	}
	else
	{
	 /*
	     check if we have a param 'name=file2' : reserved for the correctmolecule in mdlmol --> svg 
	     using javascript:getSVG(2)
	 */
	    type = getString("file2");
	    USER_SELECTION = false; /* disable colours of studentreply in correct answer svg ! */    
	    SUPERUSER_SELECTION = true; /* if appropriate set html colours to applet colours */
	    if( type != null ){ /* ok found param file2 */
		try{
		    String correct_answer = loadAny(type);
		    Molecule answer_mol = MoleculeStream.readMDLMOL(new BufferedReader(new StringReader(correct_answer.toString())));
		    svgmol = new SVGMolecule(answer_mol);
		    System.out.println("ok : exporting file2 to SVG");
		}catch(Exception e){return e.toString();}
	    }
	    else
	    {
		/* use the drawing in the applet for export to svg*/
		try{
		    svgmol = new SVGMolecule((mainPanel.editor).molData());
		    System.out.println("ok : exporting molecule in applet window to SVG\npossibly colouring atoms and bonds\naccording to color params");
		}catch(Exception e){return e.toString();}
	    }
	}
	double zoom_factor = getDouble("zoomfactor",1.00);
	if( zoom_factor != 1.00 ){ /* simple static in/out zoom, no pan */
	    id = System.currentTimeMillis();
	    g_id = ("g_SVG_"+id).toString();
	    svg_id = ("SVG_"+id).toString();
	    zoom_js="<script type=\"text/javascript\">"+
	    "var flip = 0;"+
	    "function SVG_zoom(svg,g,w0,h0){"+
	     "var svg = document.getElementById(svg);"+
	     "var g = document.getElementById(g);"+
	     "var f = "+zoom_factor+";"+
	     "if( flip == 1 ){"+
	      "flip = 0;"+
	      "var w1 = parseInt(w0*f);"+
	      "var h1 = parseInt(h0*f);"+
	      "svg.setAttributeNS(null, 'viewBox', '0 0 '+w1+' '+h1);"+
	      "svg.setAttributeNS(null, 'width',w1);"+
	      "svg.setAttributeNS(null, 'height',h1);"+
	      "g.setAttributeNS(null,'transform','matrix('+f+' 0 0 '+f+' 0 0)');"+
	     "}else{"+
	      "flip = 1;"+
	      "svg.setAttributeNS(null, 'viewBox', '0 0 '+w0+' '+h0);"+
	      "svg.setAttributeNS(null, 'width',w0);"+
	      "svg.setAttributeNS(null, 'height',h0);"+
	      "g.setAttributeNS(null,'transform','matrix(1 0 0 1 0 0)');"+
	      "};};</script>";
	}
	svgmol.draw();
	String reply = "";
	reply = (svgmol + zoom_js).toString();
	return reply.replaceAll("(\\n|\\r|\\  )", " ");
    }
    
    public boolean[] getTools(){ /* jm.evers: configuring toolbar through appletparams  */
     String param;
     boolean[] TOOL_SELECTION = new boolean[TOOL_COUNT];
     for(int p = 0;p < TOOL_COUNT ; p++){ /* adjust if more buttons are added MainPanel */
      param = getParameter(TOOLS[p]);
      TOOL_SELECTION[p] = false;
      if( param != null ){if(param.equalsIgnoreCase("yes") || param.equals("1")){ TOOL_SELECTION[p] = true;} }
     }
      return TOOL_SELECTION;
    }

    public boolean[] getMenus(){ /* jm.evers: configuring menubar through appletparams  */
     String param;
     boolean[] MENU_SELECTION = new boolean[MENU_COUNT];
     for(int p = 0;p < MENU_COUNT ; p++){ /* adjust if more buttons are added MainPanel */
      param = getParameter(MENUS[p]);
      MENU_SELECTION[p] = false;
      if( param != null && param.length() > 0){if(param.equalsIgnoreCase("yes") || param.equals("1")){ MENU_SELECTION[p] = true;} }
     }
      return MENU_SELECTION;
    }
    
    public String[] getTemplateURL(){
	// jm.evers : retreive an unknown amount of templates via params called template1,template2...
	// these templates will be called by Template.java
	String param = getParameter("template1");
	if( param != null && param.length()>0 ){
	    int p=0;
	    while( param!=null && param.length()!=0 ){
		p++; /* unknown amount...let's count */
		param = getParameter("template"+p);
	    }
	    String[] templateURL = new String[p-1];
	    for(int s = 1;s < p; s++){ /* fill the array with url's */
		param = getParameter("template"+s);
		System.out.println("loading template url : "+param);
	        templateURL[s - 1] = param;
	    }
	    return templateURL;
	}
	else
	{
	    return null;
	}
    }
    
    public boolean getFile(){ 
    /*
	jm.evers : try to load a file as string from the URL given in the params
    */
	String showfile = getParameter("showfile");
	if(showfile != null && showfile.length()>0){
	    if(showfile.equalsIgnoreCase("yes") ||  showfile.equals("1") ){
		String filename = getParameter("file");
		if (filename != null && filename.length() > 0){ 
		/* 
		    jm.evers: is param showfile is set, draw to canvas...
		*/
		    String demomol = loadAny(filename);
		    if( demomol.indexOf("V2000") < 1 ){
			SetMolecule(demomol,false);
		    }
		    else
		    {
			SetMolecule(demomol,true);
		    }
		    return true;
		}
	    }
	}
	else
	{
	    System.out.println("not using external file loading");
	}
	return false;
    } 

    public String getString( String p ){
	String param = getParameter(p);
	if( param != null  && param.length()>0){
	    return  param.toString();
        }
        return null; 
    }
    public Boolean getBool(String p , boolean d ){
    /* jm.evers 27/12/2008 */
	String param = getParameter(p);
	if( param != null && param.length()>0 ){
	    param = param.toLowerCase();
	    if(param.equals("1") || param.equalsIgnoreCase("yes") || param.equalsIgnoreCase("true") ){
		return true;
	    }
	    if(param.equals("0") || param.equalsIgnoreCase("no") ||  param.equalsIgnoreCase("false")){
		return false;
	    }
        }
        return d;
    }

    /* 
	routine to read an 'int' from some applet param 'p' 
    */
    public int getInt(String p, int d){/* jm.evers 27/12/2008 */
	String param = getParameter(p);
	if( param != null  && param.length()>0){
	    return Integer.parseInt( param );
        }
        return d; 
        /* 
    	    we don't want to trip over a wrong param...use default
	*/
    }
    /* 
	routine to read a 'double' from some applet param 'p' 
    */
    public double getDouble(String p, double d){/* jm.evers 27/12/2008 */
	String param = getParameter(p);
	if( param != null  && param.length()>0){
	    return Double.parseDouble( param );
        }
        return d; 
        /* 
    	    we don't want to trip over a wrong param...
    	*/
    }
    /* 
	routine to read a color from some applet param 'p' 
    */
    public Color getColor(String p, int alpha, int r0,int g0,int b0 ){
	String param = getParameter(p);
	if( param != null && param.length()>0){
    	    String k;int R1=0,G1=0,B1=0,rgb=0;
    	    StringTokenizer q = new StringTokenizer(param, ",");
    	    int c = q.countTokens();
    	    if( c != 3){System.out.println("use R,G,B for colours ; use param color_alpha for transparency"); return new Color(0,0,255,255);}
    	    for( int a = 0; a < 3 ; a++){
    		k = q.nextToken();
		rgb = Integer.parseInt(k, 10);
		if( rgb < 0 ){ rgb = 0; }
		if( rgb > 255 ){ rgb = 255; }
		if(a == 0){ R1 = rgb;}
		else
		if(a == 1){G1 = rgb;}
		else B1 = rgb;
            }
            return new Color(R1,G1,B1,alpha);
	}
	else
	{
    	    return new Color(r0,g0,b0,alpha);
	}
    }
    
    /* 
	jm.evers: reading atoms from params 
    */
    public String[] GetMyAtoms(){
        String param = getParameter("atoms");
        if( param != null && param.length()>0){
    	    return String2Array( param );
        }
        else
        {
    	    return null;
        }
    }
    public String getLanguage(){
    /* 
	jm.evers: reading language. default english
    */
     String param = getParameter("language");
     if(param != null && param.length() == 2){return  param.toLowerCase();}
     return "en";
    }
    
/* called via javascript */
    public String ReadAtomSelection(){
     boolean[] S = EditorPane.atomselection;
     String selection="";
     for (int p = 1;p < S.length; p++){ /* starts with 1 */
      if(S[p]){ /* is selected */
       if(selection != ""){ selection = selection +","+p; } else { selection=""+p; }
      }
     }
     return selection;
    }
    public String ReadBondSelection(){
     boolean[] S = EditorPane.bondselection;
      String selection="";
      for (int p=0;p<S.length;p++){
       if(S[p]){ /* is selected */
        if(selection == ""){ selection = ""+p; } else { selection = selection+","+p; }
       }
      }
      return selection;
    }
                

/* jm.evers : routines to load a file direct or via network */

    public String loadAny(String filename){
     String demomol = "";
     try{
        return (load(filename)).toString();
     }catch(Exception e){System.out.println("HMMMM");}
     return demomol;
    }
    public static byte [] loadURL(URL url) throws IOException {
        int bufSize = 1024 * 2;
	byte [] buf = new byte[bufSize];
	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	BufferedInputStream   in   = new BufferedInputStream(url.openStream());
	int n;
	while ((n = in.read(buf)) > 0) {
    	    bout.write(buf, 0, n);
	}
	try 
	{ in.close(); } catch (Exception ignored) { }
	return bout.toByteArray();
    }

    public static String loadFile(String fname) throws IOException {
     try{
	byte[] bytes = loadURL(new URL("file:" + fname));
	return new String(bytes);
     }catch(Exception e ){System.out.println("loadFile error :"+e.toString());}
     return null;
    }
    
    public static String load(String fileOrURL) throws IOException {
	try {
	    URL url = new URL(fileOrURL);
	    return new String(loadURL(url));
	} catch (Exception e) {
	return loadFile(fileOrURL);
	}
    }
   public String[] String2Array(String t){
	/*
	    jm.evers: parsing params into array...fieldseparators are "," ":" ";" " "
	*/
	StringTokenizer q;
	if(t.indexOf(",")!=-1){
	    q = new StringTokenizer(t, ",");
	}
	else
	{
	    if(t.indexOf(":")!=-1){
		q = new StringTokenizer(t, ":");
	    }
	    else
	    {
		if(t.indexOf(";")!=-1){
		    q = new StringTokenizer(t, ";");
		}
		else
		{
		    if(t.indexOf(" ")>2){
			q = new StringTokenizer(t, " ");
		    }
		    else
		    {
			return null;
		    }
		}
	    }
	}
	int max = q.countTokens();if( max > 50 ){ max = 50;}
	String[] tmp = new String[max];String s="";
	for( int p = 0 ; p<max ; p++){
	    s=q.nextToken();
	    if(s.length()==1 || s.length()==2){
		System.out.println("atom = "+s);
		tmp[p]=s;
	    }
	}
	return tmp;
    }
    public void send_to_wims(int type){ 
    /* 
	DO NOT USE THIS FOR SENDING SVG/XML !!!
	THIS WILL GIVE TROUBLE WIH TOO LARGE STRINGS:
	[quote] The requested URL's length exceeds the capacity limit for this server.
	BETTER USE: 
	<form  action="$wims_ref_name?form-data$session" method="post" enctype="multipart/form-data" >
    */
	applet_context = this.getAppletContext();
	String wims_ref_name = getString("wims_ref_name");
        String session = getString("session");
	String module = getString("module");
        String replyargs = getString("replyarguments"); /* reply2=*/
	if( wims_ref_name == null ){
    	    System.out.println("param wims_ref_name is not set POST will not work !!");
	}
	else
	{
	    String student_MDL_molecule = GetMoleculeMDLMol();
	    String student_svg_molecule = getSVG("1");
	    String correct_svg_molecule = getSVG("2");
	    String reply = "";
	    switch(type){
		case 0: reply = student_MDL_molecule;break;
		case 1:	reply = student_svg_molecule+"\n"+student_MDL_molecule;break;
		case 2:	reply = correct_svg_molecule+"\n"+student_svg_molecule+"\n"+student_MDL_molecule;break;
		default: reply = "send_to_wims(type) \ntype=1: MDLmol student drawing\ntype=2: student SVG \\n MDLmol student drawing\ntype=3: answer SVG \\n student SVG \\n MDLmol student drawing ";
	    }
	    try{reply = URLEncoder.encode(reply,"UTF-8"); }catch(Exception e){reply = e.toString();}
	    String back_url_string = wims_ref_name+"?session="+session+"&module="+module+"&cmd=reply&"+replyargs;
	    back_url_string = back_url_string.replaceAll(" ",""); /* no space in url */
	    URL	backurl;
	    try {backurl=new URL(back_url_string+reply);
	        applet_context.showDocument(backurl,"_self");
	    } catch (MalformedURLException e){backurl=null;System.out.println("could not send  to wims "+back_url_string);}
	}
    }
    
    /*  
	set bonds and atoms via applet param
    */
    public int[] SetAtomSelection(){
	String param = getParameter("select_atoms");
	if(param != null && param.length()>0){
	    String k;int rgb=0;int R1=255;int G1=0;int B1=0;//red
	    param=param.replaceAll(";",",");param=param.replaceAll(":",",");
	    StringTokenizer i = new StringTokenizer(param, ",");
	    int max=i.countTokens();
	    ExternalAtomSelection = new int[max];
	    SelectedAtomColorArray = new Color[max];
	    SelectedAtomColorInt = new int[max];
	    String ColorParam;
	    for(int p=0;p<max;p++){
		ExternalAtomSelection[p] = Integer.parseInt(i.nextToken());
	        // now try to find the color belonging to this selected atom
	        SelectedAtomColorInt[p] = RGB2int("select_atom_color"+ExternalAtomSelection[p]+"",0x00ff00);
	        ColorParam=getParameter("select_atom_color"+ExternalAtomSelection[p]);
		if(ColorParam != null && ColorParam.length()>0){
		    ColorParam=ColorParam.replaceAll(":",",");ColorParam=ColorParam.replace(";",",");
		    StringTokenizer q = new StringTokenizer(ColorParam, ",");
		    for( int a=0; a<3 ; a++){
			k=q.nextToken();
		        rgb=Integer.parseInt(k, 10);
			if(rgb<0){rgb=0;}if(rgb>255){rgb=255;}
		        if(a == 0){R1 = rgb;}
			else
		        {
			    if(a == 1){G1 = rgb;}
			    else{B1 = rgb;}
			}
		    }
		    SelectedAtomColorArray[p] = new  Color(R1,G1,B1,GLOBAL_ALPHA);
		}
	        else
		{
		    SelectedAtomColorArray[p] = ATOM_SELECT_COLOR;
		}
	    }
	    SUPERUSER_SELECTION = true;
	}else{ ExternalAtomSelection = null;}
	return ExternalAtomSelection;
    }

    /*  
	set bonds and atoms via applet param
    */
    public int[] SetBondSelection(){
	String param = getParameter("select_bonds");
	if(param != null && param.length()>0){
	    String k;int rgb=0;int R1=255;int G1=0;int B1=0;String ColorParam;
	    param=param.replaceAll(";",",");param=param.replaceAll(":",",");
	    StringTokenizer i = new StringTokenizer(param, ",");
	    int max=i.countTokens();
	    ExternalBondSelection = new int[max];
	    SelectedBondColorArray = new Color[max];
	    SelectedBondColorInt = new int[max];
	    for(int p=0;p<max;p++){
		ExternalBondSelection[p] = Integer.parseInt(i.nextToken());
	        // now try to find the color belonging to this selected bond 
	        SelectedBondColorInt[p] = RGB2int("select_bond_color"+ExternalBondSelection[p]+"",0x0000ff);
	        ColorParam=getParameter("select_bond_color"+ExternalBondSelection[p]);
		if(ColorParam != null && ColorParam.length()>0){
		    ColorParam=ColorParam.replaceAll(":",",");ColorParam=ColorParam.replace(";",",");
		    StringTokenizer q = new StringTokenizer(ColorParam, ",");
		    for( int a=0; a<3 ; a++){
			k=q.nextToken();
		        rgb=Integer.parseInt(k, 10);
			if(rgb<0){rgb=0;}if(rgb>255){rgb=255;}
		        if(a == 0){R1 = rgb;}
			else
		        {
			    if(a == 1){G1 = rgb;}
			    else{B1 = rgb;}
			}
		    }
		    SelectedBondColorArray[p] = new  Color(R1,G1,B1,GLOBAL_ALPHA);
		}
	        else
		{
		    SelectedBondColorArray[p] = BOND_SELECT_COLOR;
		}
	    }
	    SUPERUSER_SELECTION = true; 
	}else{ ExternalBondSelection = null;}
	return ExternalBondSelection;
    }

    /* 
	return molecule data from current molecule in panel
	returns 5 lines with  
	line 1 : molweight  	: eg 1 decimal, eg 123.4
	line 2 : plain formula  : eg C5H4O2
	line 3 : html formula 	: eg C<sub>5</sub>H<sub>4</sub>O<sub>2</sub>
	line 4 : plain formula  : eg C5H4O2 (this was in the old wimschem a faulty smiles interpretation...)
	line 5 : latex formula  : eg C_{5}H_{4}O_{2}
    */
    public String ReadApplet(){ 
	try{
	    StringWriter sw = new StringWriter();
	    BufferedWriter bw = new BufferedWriter(sw);
	    MoleculeStream.writeNative(bw,mainPanel.molData());
	    String nativemol = sw.toString();
	    nativemol = nativemol.replaceAll(" ","");
	    return moleculeData(nativemol);
	}catch(Exception e){return e.toString();}
    }
    
    public String moleculeData(String S){
	int N=Molecule.ELEMENTS.length;
	double[] WEIGHTS={0.0,1.00794,4.002602,6.941,9.01218,10.811,12.011,14.00674,15.9994,18.998403,20.1797,
	22.989768,24.305,26.981539,28.0855,30.973762,32.066,35.4527,39.948,39.0983,40.078,
	44.95591,47.88,50.9415,51.9961,54.93805,55.847,58.9332,58.6934,63.546,65.39,
	69.723,72.61,74.92159,78.96,79.904,83.8,85.4678,87.62,88.90585,91.224,
	92.90638,95.94,97.9072,101.07,102.9055,106.42,107.8682,112.411,114.818,118.71,
	121.760,127.6,126.90447,131.29,132.90543,137.327,138.9055,140.115,140.90765,144.24,
	144.9127,150.36,151.965,157.25,158.92534,162.50,164.93032,167.26,168.93421,173.04,
	174.967,178.49,180.9479,183.84,186.207,190.23,192.22,195.08,196.96654,200.59,
	204.3833,207.2,208.98037,208.9824,209.9871,222.0176,223.0197,226.0254,227.0278,232.0381,
	231.03588,238.0289,237.048,244.0642,243.0614,247.0703,247.0703,251.0796,252.083,257.0951,
	258.1,259.1009,262.11};	
	
	String[] s=S.split("\n");String[] atom;int s1=s[0].indexOf('(');int s2=s[0].indexOf(',');int s3=s[0].indexOf(')');
	int n_s=Integer.parseInt(s[0].substring(s1+1,s2).trim());int b_s=Integer.parseInt(s[0].substring(s2+1,s3).trim());
	String[] E_s=new String[n_s];int[] H_s=new int[n_s];int[] C_s=new int[n_s];int[] R_s=new int[n_s];
	int[] this_A=new int[N];int[] this_C=new int[N];int[] this_R=new int[N];int[] this_H=new int[N];
	String plainformula="";String htmlformula="";String latexformula="";
	String R_plain="";String R_html="";String R_latex="";
	String C_plain="";String C_html="";String C_latex="";
	String sign;
	double weight_s=0.0D;int r=0;
	
	/* loop only through the atom part of the file-string */
	for( int p=0; p<n_s ;p++){
	    atom=s[p+1].split("[\\=\\,\\;]"); /* first line is mimetype */
	    E_s[p]=atom[0];r=0;
	    for(int n=0;n<N;n++){
		if(atom[0].equals(Molecule.ELEMENTS[n])){
		    weight_s=(double)(weight_s + WEIGHTS[n]);
		    this_A[n]++;r=n;n=N;
		}
	    }
	    C_s[p]=Integer.parseInt(atom[3]);
	    this_C[r]=C_s[p];
	    R_s[p]=Integer.parseInt(atom[4]);
	    this_R[p]=R_s[p];
	    if( atom[5].indexOf("i")!=-1){
		atom[5]=atom[5].replaceAll("i","");H_s[p]=Integer.parseInt(atom[5]);this_H[r]=H_s[p];this_A[1]=this_A[1]+H_s[p];//hydrogen
	    }
	    else
	    {
		atom[5]=atom[5].replaceAll("e","");H_s[p]=Integer.parseInt(atom[5]);this_H[r]=H_s[p];this_A[1]=this_A[1]+H_s[p];//hydrogen
	    }	    
	    weight_s=(double)(weight_s + H_s[p]);
	}

	if(this_A[6]!=0){
	    if(this_R[6] != 0){R_plain="\u2022";R_latex="^{^{\\cdot}}";R_html="<sup>&cdot;</sup>";}
	    if(this_C[6] != 0){
		if(this_C[6]>0){sign="+";}else{sign="-";this_C[6]=(int) (Math.abs(this_C[6]));}
		C_plain=this_C[6]+sign;C_latex="^{"+this_C[6]+sign+"}";C_html="<sup><small>"+this_C[6]+sign+"</small></sup>";
	    }
	    if(this_A[6] == 1 ){/* C1 is not done:  CH4 */
		plainformula="C"+C_plain+R_plain+" ";
	        htmlformula="<span style=\"fontsize:110%\" ><tt>C"+C_html+R_html+"</tt></span>";
		latexformula="\\Bf{C"+C_latex+R_latex+"}";
	    }
	    else
	    {//C2H5OH
		plainformula="C"+C_plain+R_plain+this_A[6];
		htmlformula="<span style=\"fontsize:110%\" ><tt>C"+C_html+R_html+"</tt></span><sub><small>"+this_A[6]+"</small></sub>";
		latexformula="\\Bf{C"+C_latex+R_latex+"}_{"+this_A[6]+"}";
	    }
	}
	// the rest
	for(int p = 0; p<N; p++){
	    if( p == 6 ){ p = 7;} /* we already covered carbon */
	    if( this_A[p] != 0 ){
		R_plain="";R_html="";R_latex="";C_plain="";C_html="";C_latex="";sign="";
		if( this_R[p] != 0 ){ R_plain=this_R[p]+"\u2022";R_latex="^{^{"+this_R[p]+"\\cdot}}";R_html="<sup><sup>"+this_R[p]+"&cdot;</sup></sup>";}
		if( this_C[p] != 0 ){ if( this_C[p]>0 ){ sign="+"; } else { sign="-"; this_C[p]=-1*this_C[p];}C_plain=this_C[p]+sign;C_latex="^{"+this_C[p]+sign+"}";C_html="<sup><small>"+this_C[p]+sign+"</small></sup>";}
		if( this_A[p] == 1 ){
		    plainformula=plainformula+" "+Molecule.ELEMENTS[p]+""+C_plain+""+R_plain;
		    htmlformula=htmlformula+" <span style=\"fontsize:110%\" ><tt>"+Molecule.ELEMENTS[p]+C_html+R_html+"</tt></span>";
		    latexformula=latexformula+"\\,\\Bf{"+Molecule.ELEMENTS[p]+C_latex+R_latex+"}";
		}
		else
		{
		    plainformula=plainformula+" "+Molecule.ELEMENTS[p]+""+C_plain+""+R_plain+""+this_A[p];
		    htmlformula=htmlformula+" <span style=\"fontsize:110%\" ><tt>"+Molecule.ELEMENTS[p]+C_html+R_html+"</tt></span><sub><small>"+this_A[p]+"</small></sub>";
		    latexformula=latexformula+"\\,\\Bf{"+Molecule.ELEMENTS[p]+C_latex+R_latex+"}_{"+this_A[p]+"}";
		}
	    }
	}
	DecimalFormat df = new DecimalFormat("#.#",new DecimalFormatSymbols(Locale.US));
	/* 
	    the second 'plainformula' was (the no good) previous smiles-formula ; 
	    added just to satisfy my checkfiles ... 
	*/
	return df.format(weight_s)+"\n"+plainformula+"\n"+htmlformula+"\n"+plainformula+"\n"+latexformula;
    }
}
