/*
*********************************************************************************
* J.M. Evers 19/11/2009								*
* This is all amateur scriblings... So no copyrights.				*
* This source code file, and compiled classes derived from it,			*
* can be used and distributed without restriction, including for commercial use	*
* No warrenty whatoever								*
*********************************************************************************
  trying to produce an applet from a opensource spreadsheet java application.

    <applet id="SharpTools" archive="dist/SharpTools.jar" code="SharpTools/MainApplet" width="120" height="30">
	<param name="language" value="nl">
	<param name="X_screen_location" value="100"><!-- default 0-->
	<param name="Y_screen_location" value="100"><!-- default 0-->
	<param name="window_width" value="700">
	<param name="window_height" value="400">
	<param name="replyurl" value="http://localhost/wims/wims.cgi?module=$module&session=$sssion&reply$n=">
	
	<param name="reply_type" value="1">
	<!-- 0 = matrix  ["1","2","3";"a","b","c"] note: "" string protection for pari evaluation -->
	<!-- 1 = csv 1,2,3 \n a,b,c  note : no string protection -->
	<!-- 2 = tsv 1 \t 2 \t 3 \n a \t b \t c  note : no string protection -->
	
	<param name="convert_initial_tsv_to_matrix" value="true">
	<!-- append (to the reply) a line with the given/initial tsv-flie as a set in parameter "reply_type" -->
	
	<param name="file" value="test.csv">
	<!-- if a template tsv is needed for the exercise-->
	
	<param name="fontsize" value="16">
	
	<param name="empty_cell" value="null">
	<!-- fill the empty cells with a String : default null --> 
	
	<!-- sizes in pixels these are not really needed, defaults are fine -->
	<param name="COLUMNWIDTH" value="80"> 
	<param name="FIRSTCOLUMNWIDTH" value="40">
	<param name="HISTOGRAMWIDTH"  value="400">
	<param name="HISTOGRAMHEIGHT"  value="400"> 
	<!-- configuring the menu & toolbar -->
	<!-- accepted value's like caseinsensitive : TRUE , 0 , NO , NEE , NON -->
	<!-- all other value will result in FALSE -->
	<param name="COLUMNS" value="5">
	<param name="ROWS" value="5">
	<param name="TOOLBAR_NEW" value="false"> 
	<param name="TOOLBAR_PRINT" value="false">
	<param name="TOOLBAR_UNDO" value="true">
	<param name="TOOLBAR_REDO" value="true">
	<param name="TOOLBAR_CUT" value="true">
	<param name="TOOLBAR_COPY" value="true">
	<param name="TOOLBAR_PASTE" value="true">
	<param name="TOOLBAR_FIND" value="true">
	<param name="TOOLBAR_INSERTROW" value="true">
	<param name="TOOLBAR_INSERTCOLUMN" value="true">
	<param name="TOOLBAR_DELETEROW" value="true">
	<param name="TOOLBAR_DELETECOLUMN" value="true">
	<param name="TOOLBAR_SORTCOLUMN" value="true">
	<param name="TOOLBAR_HISTOGRAM" value="true">
	<param name="TOOLBAR_HELP" value="true">
	<param name="TOOLBAR_FUNCTIONS" value="true">
    </applet>
*/

package SharpTools;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.net.*;
import java.applet.AppletContext;


public class MainApplet extends JApplet {
    SharpTools sharptools;
    public static String spreadsheet_from_url;
    public static String empty_cell;
    public static String initial_tsv_matrix;
    public static boolean found_spreadsheet_from_url=false;
    public static boolean inapplet=true;
    public static boolean convert_initial_tsv_to_matrix;
    // instead of a configfile, a tunable paramlist;
    public static int type,fontsize,RECENTFILELIST=8,NUMCONNECTIONS=5,ROWS,COLUMNS,FIRSTCOLUMNWIDTH,COLUMNWIDTH,Xlocation,Ylocation,window_width,window_height,HISTOGRAMWIDTH,HISTOGRAMHEIGHT;
    public static boolean SAVEWINDOW,TOOLBAR_NEW,TOOLBAR_OPEN,TOOLBAR_SAVE,TOOLBAR_PASSWORD,TOOLBAR_PRINT,TOOLBAR_UNDO;
    public static boolean TOOLBAR_REDO,TOOLBAR_CUT,TOOLBAR_COPY,TOOLBAR_PASTE,TOOLBAR_FIND,TOOLBAR_INSERTROW,TOOLBAR_INSERTCOLUMN;
    public static boolean TOOLBAR_DELETEROW,TOOLBAR_DELETECOLUMN,TOOLBAR_SORTCOLUMN,TOOLBAR_HISTOGRAM,TOOLBAR_HELP,TOOLBAR_FUNCTIONS;
    public static String CONNECTION1NAME="",CONNECTION1USERNAME="",CONNECTION1PASSWORD="",CONNECTION1URL="",CONNECTION1DRIVER="";
    public static String RECENTFILE0="",RECENTFILE1="",RECENTFILE2="",RECENTFILE3="",RECENTFILE4="",RECENTFILE5="",RECENTFILE6="",RECENTFILE7="";
    public static String language="en",nee="no",ja ="yes";
    static Font defaultFont;
    public static URL backurl;
    public static String reply_url;
    private Container container;
    private JButton OpenButton;
    private JButton CloseButton;
    ImageIcon OpenIcon=new ImageIcon(getClass().getResource("/images/start.gif"));
    ImageIcon CloseIcon=new ImageIcon(getClass().getResource("/images/close.gif"));
    String Tooltip_open="Open spreadsheet";
    String Tooltip_close="Close spreadsheet";
    public static AppletContext applet_context;
    
    public void init(){
	applet_context = this.getAppletContext();
	container = this.getContentPane();
	container.setLayout(new BorderLayout());
	OpenButton = new JButton(OpenIcon);
	OpenButton.setToolTipText(Tooltip_open);
	CloseButton = new JButton(CloseIcon);
	CloseButton.setToolTipText(Tooltip_close);
	String c;
	c=getParameter("empty_cell");
	if(c!=null && c.length()>0){empty_cell = c;}else{empty_cell = "null";}
	c=getParameter("replyurl");
	if(c!=null && c.length()>0){reply_url = c;}

	c=getParameter("language");
	if(c.equalsIgnoreCase("de") || c.equalsIgnoreCase("nl") || c.equalsIgnoreCase("fr") ){
	    language=c.toLowerCase(); 
	    if(language.equals("nl")){ nee = "nee"; ja = "ja";}
	    else
	    if(language.equals("fr")){ nee = "non"; ja = "oui";}
	    else
	    if(language.equals("de")){ nee = "nein"; ja = "ja";}
	}else{language="en";}
	type = getIntParameter ("reply_type",1);
	convert_initial_tsv_to_matrix= getBoolParameter("convert_initial_tsv_to_matrix",false); 
	TOOLBAR_NEW = getBoolParameter("TOOLBAR_NEW",false);
	TOOLBAR_PRINT = getBoolParameter("TOOLBAR_PRINT",true);
	TOOLBAR_UNDO = getBoolParameter("TOOLBAR_UNDO",true);
	TOOLBAR_REDO = getBoolParameter("TOOLBAR_REDO",true);
	TOOLBAR_CUT = getBoolParameter("TOOLBAR_CUT",true);
	TOOLBAR_COPY = getBoolParameter("TOOLBAR_COPY",true);
	TOOLBAR_PASTE = getBoolParameter("TOOLBAR_PASTE",true);
	TOOLBAR_FIND = getBoolParameter("TOOLBAR_FIND",true);
	TOOLBAR_INSERTROW = getBoolParameter("TOOLBAR_INSERTROW",true);
	TOOLBAR_INSERTCOLUMN = getBoolParameter("TOOLBAR_INSERTCOLUMN",true);
	TOOLBAR_DELETEROW = getBoolParameter("TOOLBAR_DELETEROW",true);
	TOOLBAR_DELETECOLUMN = getBoolParameter("TOOLBAR_DELETECOLUMN",true);
	TOOLBAR_SORTCOLUMN = getBoolParameter("TOOLBAR_SORTCOLUMN",true);
	TOOLBAR_HISTOGRAM = getBoolParameter("TOOLBAR_HISTOGRAM",true);
	TOOLBAR_HELP = getBoolParameter("TOOLBAR_HELP",true);
	TOOLBAR_FUNCTIONS = getBoolParameter("TOOLBAR_FUNCTIONS",true);
	COLUMNS= getIntParameter ("COLUMNS",10);
	ROWS = getIntParameter ("ROWS",10);
	COLUMNWIDTH = getIntParameter ("COLUMNWIDTH",80);
	FIRSTCOLUMNWIDTH = getIntParameter ("FIRSTCOLUMNWIDTH",25);
	Xlocation = getIntParameter ("X_screen_location",0);
	Ylocation = getIntParameter ("Y_screen_location",0);
	window_width = getIntParameter ("window_width",800);
	window_height = getIntParameter ("window_height",600);
	fontsize = getIntParameter ("fontsize",14); 
	defaultFont  = new Font( "Courier", Font.PLAIN, fontsize);
	HISTOGRAMWIDTH = getIntParameter ("HISTOGRAMWIDTH",480);
	HISTOGRAMHEIGHT = getIntParameter ("HISTOGRAMHEIGHT",360);

	sharptools=new SharpTools(inapplet,language);
	sharptools.setVisible(false);
	try_to_open_wims_spreadsheet("file");
	container.add(OpenButton,BorderLayout.EAST);
	container.add(CloseButton,BorderLayout.WEST);
	container.validate();
	OpenButton.addActionListener (new ActionListener(){                                                                              
    	    public void actionPerformed (ActionEvent evt){
		    ShowSpreadSheet(true);
		}                                                                                                                          
	    }
	); 
	CloseButton.addActionListener (new ActionListener(){                                                                              
    	    public void actionPerformed (ActionEvent evt){
		    ShowSpreadSheet(false);
		}                                                                                                                          
	    }
	);
	repaint(1000);
    }
    
    public boolean getBoolParameter(String S,boolean def){
	String s = getParameter(S);
	if( s != null && s.length()>0){
	    if(s.equals("0") || s.equalsIgnoreCase("false") || s.equalsIgnoreCase(nee) ){return false;}
	    if(s.equals("1") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase(ja) ){return true;}
	}
	return def;
    }
    
    public int getIntParameter(String S,int def){
	String s = getParameter(S);
	if( s != null && s.length()>0){
	    try {
		 def = Integer.parseInt(s);
	    }catch(Exception e){
		System.out.println("could not parse \"integer\" "+s+" from parameter "+S+"\nWill use default value "+def);
	    }
	}
	return def;
    }

    public void stop(){
	sharptools.setVisible(false);
	
    }
    public void destroy(){
    }
    public void run(){}
    public void start(){}
    
// javascript methods
    public String ReadRawApplet(){//send data in tab-separated format to Javascript
	String AppletReply=sharptools.ReadRawSpreadSheet();
	return AppletReply;
    } 
    public String ReadApplet(){//send data in tab-separated or matrix format to Javascript
	// default [1,2,3;4,5,6]
	String AppletReply=sharptools.ReadSpreadSheet(type);
	return AppletReply;
    }
    
    public void ShowSpreadSheet(boolean v){
	sharptools.setVisible(v);
    }
    
    public void try_to_open_wims_spreadsheet(String S){
	String s = getParameter(S);
	try{
	    spreadsheet_from_url = load(s);
	    if(spreadsheet_from_url != null){ 
		found_spreadsheet_from_url=true;
		sharptools.openFileString(spreadsheet_from_url);
		if(convert_initial_tsv_to_matrix){
		    initial_tsv_matrix = sharptools.ReadSpreadSheet(type);
		}
	    }
	}
	catch(Exception e){System.out.println(e);}
    }

    public static byte [] loadURL(URL url) throws IOException {
	int bufSize = 1024 * 2;
	byte [] buf = new byte[bufSize];
	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	BufferedInputStream   in   = new BufferedInputStream(url.openStream());
	int n;
	while ((n = in.read(buf)) > 0){
	    bout.write(buf, 0, n);
	}
	try { in.close(); } catch (Exception ignored) { }
	return bout.toByteArray();
    }
    
    public static String loadFile(String fname) throws IOException {
    // jm.evers :handy functions !!! not mine :(
	byte[] bytes = loadURL(new URL("file:" + fname));
	return new String(bytes);
    }

    public static String load(String fileOrURL) throws IOException {
    // jm.evers :handy functions !!! not mine :(
	try {
	    URL url = new URL(fileOrURL);
	    return new String(loadURL(url));
	} catch (Exception e) { return loadFile(fileOrURL);}
    }
    
    public String getAppletInfo(){
	return "Spreadsheet program for WIMS.";
    }
    
}