package rene.zirkel.graphics;

/* Graphics class supporting EPS export from plots.

Copyright (c) 1998-2000 The Regents of the University of
California.

Modified, completed and extended by R. Grothmann

*/

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;

class EpsFontMetrics extends FontMetrics
{	Font F;

	public EpsFontMetrics (Font f)
	{	super(f); // a dummy font.
		F=f;
	}

	public int stringWidth (String s)
	{	return s.length()*F.getSize()/2;
	}
	
	public int getHeight()
	{	return F.getSize(); 
	}
	
	public int getAscent()
	{	return F.getSize()*4/5; 
	}
	
}

class EpsPoint
{	double x,y;
	public EpsPoint (double xx, double yy)
	{	x=xx; y=yy;
	}
}

public class EPSGraphics {
	
	/////////////////////////////////////////////////////////////////
	//
	//// private variables
	////
	
	private Color _currentColor = Color.black; 
	private Font _currentFont; 
	private double _width, _height;
	int _orientation; 
	private Hashtable _linepattern = new Hashtable(); 
	private OutputStream _out; 
	private StringBuffer _buffer = new StringBuffer(); 
	private Clipboard _clipboard; 
	private double LineWidth=1;
	
	static private String[] _patterns = {
		"[]",
			"[1 1]",
			"[4 4]",
			"[4 4 1 4]",
			"[2 2]",
			"[4 2 1 2 1 2]",
			"[5 3 2 3]",
			"[3 3]",
			"[4 2 1 2 2 2]",
			"[1 2 5 2 1 2 1 2]",
			"[4 1 2 1]",
	}; 

	private int _patternIndex = 0; 

	public static final int PORTRAIT = 0; 
	public static final int LANDSCAPE = 1; 

	private FontMetrics FM;	
	
	public EPSGraphics (OutputStream out, double width, double height, 
		int orientation, boolean clip) {
		_width = width; 
		_height = height; 
		_orientation = orientation; 
		_out = out; 
		_buffer.append("%!PS-Adobe-3.0 EPSF-3.0\n"); 
		_buffer.append("%%Creator: QCircuitBuilder\n"); 
		_buffer.append("%%BoundingBox: 50 50 " + 
			(int)(50+width) + " "+ (int)(50+height) +"\n"); 
		//_buffer.append("%%Orientation: " + (_orientation == PORTRAIT ? "Portrait" : "Landscape"));
		//_buffer.append("%%PageOrientation: " + (_orientation == PORTRAIT ? "Portrait" : "Landscape"));
		_buffer.append("%%Pages: 1\n"); 
		_buffer.append("%%Page: 1 1\n"); 
		_buffer.append("%%LanguageLevel: 2\n"); 
		if (clip) clipRect(0,0,width,height);
		_buffer.append("/Helvetica findfont 10 scalefont setfont\n");
	}	
	
	public void clearRect(int x, int y, int width, int height) 
	{
	}
	
	// Clip
	public void clipRect (double x, double y, double width, double height) {
		EpsPoint start = _convert(x, y); 
		//_fillPattern();
		_buffer.append("newpath " + round(start.x) + " " + round(start.y) + " moveto\n"); 
		_buffer.append("0 " + round(-height) + " rlineto\n"); 
		_buffer.append("" + round(width) + " 0 rlineto\n"); 
		_buffer.append("0 " + round(height) + " rlineto\n"); 
		_buffer.append("" + round(-width) + " 0 rlineto\n"); 
		_buffer.append("closepath clip\n"); 
	}

	private double round (double x)
	{	return Math.round(x*1000.0)/1000.0;
	}
	
	/** Draw a line, using the current color, between the points
	(x1, y1)
	* and (x2, y2) in this graphics context's coordinate
	system.
	* @param x1 the x coordinate of the first point.
	* @param y1 the y coordinate of the first point.
	* @param x2 the x coordinate of the second point.
	* @param y2 the y coordinate of the second point.
	*/
	public void drawLine(double x1, double y1, double x2, double y2) 
	{	EpsPoint start = _convert(x1, y1); 
		EpsPoint end = _convert(x2, y2); 
		_buffer.append("newpath " + round(start.x) + " " + round(start.y) + " moveto\n"); 
		_buffer.append("" + round(end.x) + " " + round(end.y) + " lineto\n"); 
		_buffer.append("stroke\n"); 
	}
	
	/** Draw a closed polygon defined by arrays of x and y
	coordinates.
	* Each pair of (x, y) coordinates defines a vertex. The
	third argument
	* gives the number of vertices. If the arrays are not long
	enough to
	* define this many vertices, or if the third argument is
	less than three,
	* then nothing is drawn.
	* @param xPoints An array of x coordinates.
	* @param yPoints An array of y coordinates.
	* @param nPoints The total number of vertices.
	*/
	public void drawPolygon(double xPoints[], double yPoints[], int
		nPoints) 
	{	if (!_polygon(xPoints, yPoints, nPoints)) {
			return; 
		} else {
			_buffer.append("closepath stroke\n"); 
		}
	}
	
	/** Draw an oval bounded by the specified rectangle with the
	current color.
	* @param x The x coordinate of the upper left corner
	* @param y The y coordinate of the upper left corner
	* @param width The width of the oval to be filled.
	* @param height The height of the oval to be filled.
	*/
	// FIXME: Currently, this ignores the fourth argument and
	// draws a circle with diameter given by the third argument.
	public void drawOval (double x, double y, double width, double height) 
	{
		double radius = width/2.0; 
		_buffer.append("newpath " + _convertX(x+radius) + " " + 
			_convertY(y+radius) + " " + 
			round(radius) + " 0 360 arc closepath stroke\n"); 
	}
	
	public void drawRect(double x, double y, double width, double height) {
		EpsPoint start = _convert(x, y); 
		_buffer.append("newpath " + round(start.x) + " " + round(start.y) + " moveto\n"); 
		_buffer.append("0 " + round(-height) + " rlineto\n"); 
		_buffer.append("" + round(width) + " 0 rlineto\n"); 
		_buffer.append("0 " + round(height) + " rlineto\n"); 
		_buffer.append("" + round(-width) + " 0 rlineto\n"); 
		_buffer.append("closepath stroke\n"); 
	}
	
	public void drawRoundRect(double x, double y, double width, double
		height,
		int arcWidth, int arcHeight) {
	}
	
	public void drawString(java.text.AttributedCharacterIterator
		iterator,
		int x, int y) {
	}
	
	public void drawString(String str, double x, double y) 
	{
		FontMetrics fm=getFontMetrics();
		EpsPoint start = _convert(x, y); 
		_buffer.append("" + start.x + " " + start.y + " moveto\n"); 
		_buffer.append("(" + str + ") show\n"); 
	}
	
	public void drawArc (double x, double y, double width, double height,
		double startAngle, double arcAngle) 
	{	double radius = width/2.0; 
		_buffer.append("newpath " +
			_convertX(x+radius) + " " + 
			_convertY(y+radius) + " " + 
			round(radius) + " " + round(startAngle) + " " + " " +
			round(startAngle+arcAngle) + " arc stroke\n"); 
	}
	
	public void fillArc (double x, double y, double width, double height,
		double startAngle, double arcAngle) 
	{	double radius = width/2.0;
		_buffer.append("newpath " + 
			_convertX(x+radius) + " " + _convertY(y+radius) + " " +
			" moveto " + 
			_convertX(x+radius) + " " + _convertY(y+radius) + " " + 
			radius + " " + round(startAngle) + " " +
			round(startAngle+arcAngle) + 
			" arc closepath fill\n"); 
	}
	
	public void fillChord (double x, double y, double width, double height,
		double startAngle, double arcAngle) 
	{	double radius = width/2.0;
		_buffer.append("newpath " + 
			_convertX(x+radius) + " " + _convertY(y+radius) + " " + 
			round(radius) + " " + round(startAngle) + " " +
			round(startAngle+arcAngle) + 
			" arc fill\n"); 
	}
	
	public void fillPolygon(double xPoints[], double yPoints[], int
		nPoints) 
	{	if (!_polygon(xPoints, yPoints, nPoints)) {
			return; 
		} else {
			_buffer.append("closepath fill\n"); 
		}
	}
	
	/** Fill an oval bounded by the specified rectangle with the
	current color.
	* @param x The x coordinate of the upper left corner
	* @param y The y coordinate of the upper left corner
	* @param width The width of the oval to be filled.
	* @param height The height of the oval to be filled.
	*/
	// FIXME: Currently, this ignores the fourth argument and draws a circle
	// with diameter given by the third argument.
	public void fillOval (double x, double y, double width, double height) 
	{
		double radius = width/2.0; 
		_buffer.append("newpath " + _convertX(x+radius) + " " + 
			_convertY(y+radius) + " " + 
			radius + " 0 360 arc closepath fill\n"); 
	}
	
	/** Fill the specified rectangle and draw a thin outline
	around it.
	* The left and right edges of the rectangle are at x and x
	+ width - 1.
	* The top and bottom edges are at y and y + height - 1.
	* The resulting rectangle covers an area width pixels wide
	by
	* height pixels tall. The rectangle is filled using the
	* brightness of the current color to set the level of gray.
	* @param x The x coordinate of the top left corner.
	* @param y The y coordinate of the top left corner.
	* @param width The width of the rectangle.
	* @param height The height of the rectangle.
	*/
	public void fillRect(double x, double y, double width, double height) {
		EpsPoint start = _convert(x, y); 
		//_fillPattern();
		_buffer.append("newpath " + start.x + " " + start.y + " moveto\n"); 
		_buffer.append("0 " + round(-height) + " rlineto\n"); 
		_buffer.append("" + round(width) + " 0 rlineto\n"); 
		_buffer.append("0 " + round(height) + " rlineto\n"); 
		_buffer.append("" + round(-width) + " 0 rlineto\n"); 
		_buffer.append("closepath gsave fill grestore\n"); 
		_buffer.append("0.5 setlinewidth 0 setgray [] 0 setdash stroke\n"); 
		// reset the gray scale to black
		_buffer.append(round(LineWidth)+" setlinewidth\n"); 
	}
	
	public void fillRoundRect(double x, double y, double width, double
		height,
		int arcWidth, int arcHeight) {
	}
	
	public Shape getClip() {
		return null; 
	}
	
	public Rectangle getClipBounds() {
		return null; 
	}
	
	public Color getColor() {
		return _currentColor; 
	}
	
	public Font getFont() {
		return _currentFont; 
	}
	
	public FontMetrics getFontMetrics (Font f)
	{	if (FM==null) FM=new EpsFontMetrics(new Font("dialog",Font.PLAIN,20)); 
		return FM; 
	}
	public FontMetrics getFontMetrics ()
	{	return getFontMetrics(_currentFont);
	}
	
	public void setFont (Font font) {
		int size = font.getSize(); 
		boolean bold = font.isBold(); 
		if (bold) {
			_buffer.append("/Helvetica-Bold findfont\n"); 
		} else {
			_buffer.append("/Helvetica findfont\n"); 
		}
		_buffer.append("" + size + " scalefont setfont\n"); 
		_currentFont = font;
		FM=new EpsFontMetrics(font); 
	}
	
	public void setClip(Shape clip) {
	}
	
	public void setClip(int x, int y, int width, int height) {
	}
	
	/** Set the current color.
	* @param c The desired current color.
	*/
	public void setColor(Color c) {
		_buffer.append(c.getRed()/255.0); 
		_buffer.append(" "); 
		_buffer.append(c.getGreen()/255.0); 
		
		_buffer.append(" "); 
		_buffer.append(c.getBlue()/255.0); 
		
		_buffer.append(" setrgbcolor\n"); 
		// _buffer.append("[] 0 setdash\n");
		// _buffer.append("1 setlinewidth\n");
		
		_currentColor = c; 
	}

	public void setLineWidth (double w)
	{	_buffer.append(round(w)+" setlinewidth\n");
		LineWidth=w;
	}
	
	public void setDash (double a, double b)
	{	_buffer.append("["+round(a)+" "+round(b)+" ] 0 setdash\n");
	}
	
	public void clearDash ()
	{	_buffer.append("[ ] 0 setdash\n");
	}
	
	public void setPaintMode() {
	}
	
	public void setXORMode(Color c1) {
	}
	
	/**
	* Issue the PostScript showpage command, then write and
	flush the output.
	*/
	public void showpage (String name) {
		try {
			//_buffer.append("showpage\n"); 
			_buffer.append("%%EOF"); 
			
			PrintWriter output = new PrintWriter(
				new java.io.FileWriter(name)); 
			
			output.println(_buffer.toString()); 
			output.flush(); 
			
		}
		catch(Exception e) {	e.printStackTrace(); }
	}
	
	/**
	* Issue the PostScript showpage command, then write and
	flush the output.
	*/
	public void close ()
		throws IOException 
	{
			_buffer.append("showpage\n"); 
			_buffer.append("%%EOF"); 
	
			PrintWriter output=new PrintWriter(_out);
			
			output.println(_buffer.toString()); 
			output.flush(); 
			
	}
	
	/////////////////////////////////////////////////////////////////
	//
	//// private methods
	////
	
	// Convert the screen coordinate system to that of postscript.
	private EpsPoint _convert (double x, double y) {
		return new EpsPoint(round(x+50),round(_height+50 - y)); 
	}
	
	private double _convertX (double x) 
	{	return round(x+50);
	}
	private double _convertY (double y) 
	{	return round(_height+50-y);
	}

	// Draw a closed polygon defined by arrays of x and y coordinates.
	// Return false if arguments are misformed.
	private boolean _polygon(double xPoints[], double yPoints[], int
		nPoints) {
		if (nPoints < 3 || xPoints.length < nPoints
			|| yPoints.length < nPoints) return false; 
		EpsPoint start = _convert(xPoints[0], yPoints[0]); 
		_buffer.append("newpath " + round(start.x) + " " + round(start.y) + " moveto\n"); 
		for (int i = 1; i < nPoints; i++) {
			EpsPoint vertex = _convert(xPoints[i], yPoints[i]); 
			_buffer.append("" + round(vertex.x) + " " + round(vertex.y) + " lineto\n"); 
		}
		return true; 
	}
	
	// Issue a command to set the fill pattern.
	// Currently, this is a gray level that is a function of the color.
	private void _fillPattern() {
		// FIXME: We probably want a fill pattern rather than
		// just a gray scale.
		int red = _currentColor.getRed(); 
		int green = _currentColor.getGreen(); 
		int blue = _currentColor.getBlue(); 
		// Scaling constants so that fully saturated R, G, or B appear
		// different.
		double bluescale = 0.6; // darkest
		double redscale = 0.8; 
		double greenscale = 1.0; // lightest
		double fullscale =
			Math.sqrt(255.0*255.0*(bluescale*bluescale
			+ redscale*redscale + greenscale*greenscale)); 
		double graylevel =
			Math.sqrt((double)(red*red*redscale*redscale
			+ blue*blue*bluescale*bluescale
			+ green*green*greenscale*greenscale))/fullscale; 
		_buffer.append("" + graylevel + " setgray\n"); 
		// NOTE -- for debugging, output color spec in comments
		_buffer.append("rgb: " + red + " " +
			green + " " + blue +"\n"); 
	}

}
