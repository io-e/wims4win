/* Appletable Plotter

 @Author: Edward A. Lee and Christopher Hylands

 @Version: $Id: PlotApplet.java,v 1.76 2005/07/30 05:30:56 cxh Exp $

 @Copyright (c) 1997-2005 The Regents of the University of California.
 All rights reserved.

 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the
 above copyright notice and the following two paragraphs appear in all
 copies of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY
 */
package ptolemy.plot;

import java.io.*;                                                                                                                          
import java.awt.*;                                                                                                                         
import java.awt.event.*;                                                                                                                   
import java.util.*;                                                                                                                        
import javax.swing.*;                                                                                                                      
import java.net.*;  

//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.MalformedURLException;
//import java.net.URL;
import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import java.applet.*;                                                                                                                      
import java.awt.*;                                                                                                                         
import java.util.*;                                                                                                                        
import java.awt.event.*;                                                                                                                   
import java.lang.Math;


import org.javia.arity.*;

//////////////////////////////////////////////////////////////////////////
//// PlotApplet

/**
 An Applet that can plot data from a URL.
 The URL should be specified using the dataurl applet parameter.
 The formatting commands are included in the file with the
 the data.
 If no URL is given, then a sample plot is generated.

 @author Edward A. Lee, Christopher Hylands, Contributor: Roger Robins
 @version $Id: PlotApplet.java,v 1.76 2005/07/30 05:30:56 cxh Exp $
 @since Ptolemy II 0.2
 @Pt.ProposedRating Yellow (cxh)
 @Pt.AcceptedRating Yellow (cxh)
 @see PlotBox
 @see Plot
 */
public class PlotApplet extends JApplet {
    //     /** Construct a Plot applet */
    //     public PlotApplet() {
    // FIXME: having the mutex causes applets to hang.
    // The mutex was introduced to Work around problem in Java
    // 1.3.1_08 where if we create 6 instances of a Plot applet then
    // navigate forward then back - IE and Navigator hang.
    // However, since we are now operating inside the Swing Event
    // thread, I think this code is unnecessary.
    //         if (_mutex == null) {
    //             _mutex = new Object();
    //         }
    //     }
    
    public static boolean normalexponent=false;
    
    public String ReadApplet(){
                                                                                                                
        String reply="";
	reply=plot().ReadPlotApplet();                                                                                                                 
    return reply;                                                                                                                          
    }

    /** Return a string describing this applet.
     *  @return A string describing the applet.
     */
    public String getAppletInfo() {
        return "PlotApplet " + PlotBox.PTPLOT_RELEASE + ": A data plotter.\n"
                + "By: Edward A. Lee and\n " + "Christopher Hylands\n"
                + "($Id: PlotApplet.java,v 1.76 2005/07/30 05:30:56 cxh Exp $)";
    }

    /** Return information about parameters.
     *  @return A array of arrays giving parameter names, the type,
     *   and the default value or description.
     */
    public String[][] getParameterInfo() {
        String[][] pinfo = {
		{ "data", "String", "My_Data_String as String" },
		{ "function", "String", "function1,function2,function3,function4 ..." }, 
		{ "file", "String", "http://localhost/wims/wims.cgi?session=$session&cmd=getfile&special_parm=My_Data_String_File" },
                { "dataurl", "url", "the URL of the data to plot" },
                { "plotsteps", "integer", "only for data input via function param" },
                { "background", "hexcolor value", "background color" },
                { "foreground", "hexcolor value", "foreground color" },
                { "height", "integer", "100" },
		{ "width", "integer", "100" }, 
		{ "smooth", "integer", "0 or 1 : use with care. default=0" }, 
		{ "names", "String", "names function1,name function2 ... or left blank " }, 
		{ "xmin", "integer", "minimum x-value or left blank for auto detection" }, 
		{ "xmax", "integer", "maximum x-value or left blank for auto detection" }, 
		{ "ymin", "integer", "minimum y-value or left blank for auto detection" }, 
		{ "ymax", "integer", "maximum y-value or left blank for auto detection" }, 
		{ "xlog", "integer", "x-axis is log-scale: 0 or 1: default 0" }, 
		{ "ylog", "integer", "y-axis is log-scale: 0 or 1: default 0" }, 
		{ "color", "integer", " 0 or 1 :default 1" }, 
		{ "xtics", "String", " zero 0, one 1, two 2, pi 3.1428, four 4, five 5 " }, 
		{ "ytics", "String", " thing1 0, thing2 1, thing3 2, three 3, four 4, five 5 " }, 
		{ "xlabel", "String", "label of the x-axis: default x-axis" }, 
		{ "ylabel", "String", "label of the y-axis: default y-axis" }, 
		{ "title", "String", "title of the plot" }, 
		};
        return pinfo;
    }

    /** Initialize the applet.  Read the applet parameters.

     *  Subclasses that extend this method and call Swing UI methods
     *  should do so in the Swing Event thread by calling
     *  SwingUtilities.invokeAndWait().
     *  Note that some Plot methods will automatically run in the
     *  Swing Event thread, some will not.
     *  For details about SwingUtilities.invokeAndWait(), see
     *  <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/applet.html#thread">The Sun Applet Tutorial</a>
     */

     // jm.evers                                                                                                                           
   public static byte [] loadURL(URL url) throws IOException {
	// jm.evers :handy functions !!! not mine :(
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
	// jm.evers :handy functions !!! not mine :(
	byte[] bytes = loadURL(new URL("file:" + fname));
	return new String(bytes);
    }
    
    public static String load(String fileOrURL) throws IOException {
	// jm.evers :handy functions !!! not mine :(
	try {
	    URL url = new URL(fileOrURL);
	    return new String(loadURL(url));
	    } 
	    catch (Exception e) {return loadFile(fileOrURL);}
    }

    public synchronized void read_from_param(String data){
	StringTokenizer f = new StringTokenizer(data, ";");
	int max=f.countTokens();String line="";
	for(int s=0;s<max;s++){
	    line=f.nextToken();
            plot().read(line);
        }
        showStatus("Done");
	repaint(); 
    }         
    
    public synchronized void read_data_from_url(String data_url){
	try{
	    String data=(load(data_url)).toString();
	    StringTokenizer f = new StringTokenizer(data, ";");
	    int max=f.countTokens();String line="";
	    for(int s=0;s<max;s++){
		line=f.nextToken();
    		plot().read(line);
    	    }
    	    showStatus("Done");
	    repaint(); 
	}
	 catch (Exception e){System.out.println("Could not load data from URL or file : "+data_url);} 
    }

    public synchronized void read_function_from_param(String function,String names,String totaal,int xmin,int xmax,int plotsteps,int smooth){
	String fun;String line="";String fff;
	Symbols symbols=new Symbols();
	double stap=(double) (xmax-xmin)/plotsteps;double org=stap;double y=0.0;double x=0.0;double dy=0.0;
	float diff;
	int s;int max;int tot=0;int n;String legenda="";
	if(names.length()!=0){ n=1; } else { n=0; }
	StringTokenizer ff = new StringTokenizer(function, ",");max=ff.countTokens();
	StringTokenizer nn = new StringTokenizer(names, ",");
	for(s=0;s<max;s++){
	    if(n==0){ totaal=totaal+";dataset:"+s; } else {legenda=nn.nextToken(); totaal=totaal+";dataset:"+legenda; } 
	    fff=ff.nextToken();
	    System.out.println("functie "+s + "="+fff);
	    for(x=xmin; x<xmax; x=x+stap){
		if(smooth==1){
		    diff=(float) Math.abs(y-dy);
		    if(diff>10*stap && stap>0.001){stap=0.5*stap;}else{stap=org;}
		}
		fun=fff.replaceAll("x","("+Double.toString(x)+")");
		try { y=symbols.eval(fun);} catch (SyntaxException e) {System.out.println("error "+e); }
		totaal=totaal+";"+x+","+y;
		tot++;
	    }
	}
	System.out.println("totaal aantal punten is "+tot);
	StringTokenizer f = new StringTokenizer(totaal, ";");max=f.countTokens();
	for(s=0;s<max;s++){
    	    plot().read(f.nextToken());
    	}
    	showStatus("Done");
	repaint(); 
    }                                                                                     


    public void init() {
        super.init();

        // FIXME: having the mutex causes applets to hang.
        //synchronized (_mutex) {
        Runnable doActions = new Runnable() {
            public void run() {
                if (_plot == null) {
                    _plot = newPlot();
                }

                getContentPane().add(plot(), BorderLayout.NORTH);

                // Process the width and height applet parameters
                int width;

                // Process the width and height applet parameters
                int height;
                String widthspec = getParameter("width");

                if (widthspec != null) {
                    width = Integer.parseInt(widthspec);
                } else {
                    width = 400;
                }

                String heightspec = getParameter("height");

                if (heightspec != null) {
                    height = Integer.parseInt(heightspec);
                } else {
                    height = 400;
                }

                _setPlotSize(width, height);
                plot().setButtons(true);

                // Process the background parameter.
                Color background = Color.white;
                String colorspec = getParameter("background");
		

                if (colorspec != null) {
                    background = PlotBox.getColorByName(colorspec);
                }

                setBackground(background);
                plot().setBackground(background);
                getContentPane().setBackground(background);

                // Process the foreground parameter.
                Color foreground = Color.black;
                colorspec = getParameter("foreground");

                if (colorspec != null) {
                    foreground = PlotBox.getColorByName(colorspec);
                }

                setForeground(foreground);
                plot().setForeground(foreground);
                plot().setVisible(true);
		
		//jm.evers
		String normalExponent=getParameter("normalexponent");
		if(normalExponent != null && normalExponent.length()!=0){
		    if(normalExponent.equalsIgnoreCase("yes") || normalExponent.equals("1")){
			normalexponent=true;
		    } 
		}
		
		int finput=0;String data;
		// reading the function param and all other params needed for plotting.
		data=getParameter("function"); 
		if(data!=null && data.length()!=0){
		    finput=1;int xmin;int xmax;int plotsteps=500;double ymin;double ymax;
		    String param;String xlabel;String ylabel;String grid;String title;String kleur="on";String xtics="";String ytics="";
		    String names="";int smooth=0;String xlog="off";String ylog="off";
		    String bargraph="";String binwidth="";String binoffset="";
		    
		    param=getParameter("bargraph");
		    if(param!=null && param.length()!=0){ bargraph=param; }
		    param=getParameter("binwidth");
		    if(param!=null && param.length()!=0){ binwidth=param; }
		    param=getParameter("binoffset");
		    if(param!=null && param.length()!=0){ binoffset=param; }
		    
		    
		    param=getParameter("xmax");
		    if(param!=null && param.length()!=0){ xmax=(int) Integer.parseInt(param, 10); } else { xmax=10; }
		    param=getParameter("xmin");
		    if(param!=null && param.length()!=0){ xmin=(int) Integer.parseInt(param, 10); } else { xmin=-1*xmax; }

		    param=getParameter("ymax");
		    if(param!=null && param.length()!=0){ 
			ymax=(double) Integer.parseInt(param, 10);
			param=getParameter("ymin");
			ymin=(double) Integer.parseInt(param, 10);
			plot().setYRange(ymin,ymax);
		    }
		    param=getParameter("ylog");
		    if(param!=null && param.length()!=0){ if(param.equals("1")){ylog="on";}}
		    param=getParameter("xlog");
		    if(param!=null && param.length()!=0){ if(param.equals("1")){xlog="on";}}
		    param=getParameter("smooth");
		    if(param!=null && param.length()!=0){ if(param.equals("1")){smooth=1;}}
		    param=getParameter("names");
		    if(param!=null && param.length()!=0){ names=param; } else { names=""; }
		    param=getParameter("xlabel");
		    if(param!=null && param.length()!=0){ xlabel=param; } else { xlabel="x-axis"; }
		    param=getParameter("ylabel");
		    if(param!=null && param.length()!=0){ ylabel=param; } else { ylabel="y-axis"; }
		    param=getParameter("grid");
		    if(param.equalsIgnoreCase("off")){ grid="off"; } else { grid="on"; }
		    param=getParameter("title");
		    if(param!=null && param.length()!=0){ title=param; } else { title="WIMS data plotter"; }
		    param=getParameter("color");
		    if(param.equalsIgnoreCase("off")){ kleur="off"; }
		    param=getParameter("xtics");
		    if(param!=null && param.length()!=0){ xtics=param; }
		    param=getParameter("ytics");
		    if(param!=null && param.length()!=0){ ytics=param; }
		    param=getParameter("plotsteps");
		    if(param!=null && param.length()!=0){ plotsteps=(int) Integer.parseInt(param, 10); }

		    String totaal="TitleText: "+title+";xlabel: "+xlabel+";ylabel: "+ylabel+";xlog: "+xlog+";ylog: "+ylog+";grid: "+grid+";color: "+kleur+";xticks:"+xtics+";yticks:"+ytics+";bargraph: "+bargraph+";binwidth: "+binwidth+";binoffset: "+binoffset;
		    read_function_from_param(data , names , totaal, xmin , xmax , plotsteps , smooth);
		}
		
		// reading a string of data in the "plt" format. typical: the string is provided by a wims variable
		data=getParameter("data"); 
		if(data!=null){
		    finput=1;
		    read_from_param(data);
		}
		
		data=getParameter("file");
		if(data!=null && finput==0){
		    finput=1;
		    read_data_from_url(data);
		}
		
                // Process the dataurl parameter.
		// the file is written by wims and stored in the session-dir/getfile/
                String dataurlspec = getParameter("dataurl");

                if (dataurlspec != null && finput==0) {
                    try {
                        showStatus("Reading data");
                        System.out.println("Trying to read data from"+dataurlspec);
                        URL dataurl = new URL(getDocumentBase(), dataurlspec);
                        InputStream in = dataurl.openStream();
                        _read(in);
                        showStatus("Done");
                    } catch (MalformedURLException e) {
                        System.out.println(e.toString());
                    } catch (FileNotFoundException e) {
                        System.out.println("PlotApplet: file not found: " + e);
                    } catch (IOException e) {
                        System.out.println("PlotApplet: error reading input file: " + e);
                    }
                }
            }
        };

        try {
            // NOTE: Using invokeAndWait() here risks causing
            // deadlock.  However, the Sun Tutorial recommends calling
            // invokeAndWait so that the work finishes before returning.
            // if we call invokeLater() then demo/PlotFourierSeries.java
            // has problems.
            SwingUtilities.invokeAndWait(doActions);
        } catch (Exception ex) {
            // Ignore InterruptedException.
            // Other exceptions should not occur.
        }

        //}
    }

    /** Create a new Plot object for the applet.  Derived classes can
     *  redefine this method to return a different type of plot object.
     *  @return A new instance of PlotBox.
     */
    public PlotBox newPlot() {
        return new Plot();
    }

    /** Return the plot object to operate on.
     *  @return The plot object associated with this applet.
     */
    public PlotBox plot() {
        return _plot;
    }

    /** Set the plot object to operate on.
     *  @param plot The plot object to associate with this applet.
     */
    public void setPlot(PlotBox plot) {
        // FIXME: this method is necessary for PxgraphApplet to work
        // properly.  We could modify newPlot() to set _plot, but
        // that would change the newPlot() contract, so we add another method.
        _plot = plot;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Read the specified stream.  Derived classes may override this
     *  to support other file formats.
     *  @param in The input stream.
     *  @exception IOException If the stream cannot be read.
     */
    protected void _read(InputStream in) throws IOException {
        plot().read(in);
    }

    /** Given the size of the applet, set the size of the plot.
     *  Derived classes may override this to allow room for other
     *  widgets below the plot.
     *  @param appletWidth The width of the applet.
     *  @param appletHeight The height of the applet.
     */
    protected void _setPlotSize(int appletWidth, int appletHeight) {
        plot().setSize(appletWidth, appletHeight);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////
    // Work around problem in Java 1.3.1_08 where if we create
    // 6 instances of a Plot applet then navigate forward then
    // back - IE and Navigator hang. (Roger Robins)
    // FIXME: having the mutex causes applets to hang.
    //private static Object _mutex = null;
    // The Plot component we are running.
    private transient PlotBox _plot;

}
