/*
    Sketch Elements: Chemistry molecular diagram drawing tool.
    
    (c) 2008 Dr. Alex M. Clark
    
    Released as GNUware, under the Gnu Public License (GPL)
    
    See www.gnu.org for details.
*/

package WIMSchem;

import java.util.*;
import java.io.*;
import java.text.*;
/*
    A drawing container which allows primitives to be composed, and once complete, rendered into an SVG output stream.
*/

public class SVGBuilder
{
    DecimalFormat df = new DecimalFormat("#.##",new DecimalFormatSymbols(Locale.US));
    public final static int NOCOLOUR=-1;
    public final static int TXTSTYLE_NORMAL=0;
    public final static int TXTSTYLE_BOLD=0x01;
    public final static int TXTSTYLE_ITALIC=0x02;
    public final static int TXTALIGN_CENTRE=0;
    public final static int TXTALIGN_LEFT=1;
    public final static int TXTALIGN_RIGHT=2;
    public String g_id = "g_SVG_1000000";
    public String svg_id = "SVG_1000000";

    private boolean fresh=true;
    private double lowX=0,lowY=0,highX=0,highY=0;
    
    private boolean[] charMask=new boolean[96];

    private final static int ATOM_LINE=1,ATOM_RECT=2,ATOM_OVAL=3,ATOM_PATH=4,ATOM_TEXT=5;
    abstract class Atom {int AtomClass,TypeRef;}

    class LineAtom extends Atom {double X1,Y1,X2,Y2;}
    class LineType {double Thickness; int Colour;}
    
    class RectAtom extends Atom {double X,Y,W,H;}
    class RectType {int EdgeCol,FillCol; double Thickness;}
    
    class OvalAtom extends Atom {double CX,CY,RW,RH;}
    class OvalType {int EdgeCol,FillCol; double Thickness;}
    
    class PathAtom extends Atom {int N; double[] X,Y; boolean[] Ctrl; boolean Closed;}
    class PathType {int EdgeCol,FillCol; double Thickness; boolean HardEdge;}

    class TextAtom extends Atom {double X,Y; String Txt;}
    class TextType {double Sz; int Colour,Style,Align;}
    
    private ArrayList<Atom> atoms=new ArrayList<Atom>();
    private ArrayList<LineType> lineTypes=new ArrayList<LineType>();
    private ArrayList<RectType> rectTypes=new ArrayList<RectType>();
    private ArrayList<OvalType> ovalTypes=new ArrayList<OvalType>();
    private ArrayList<PathType> pathTypes=new ArrayList<PathType>();
    private ArrayList<TextType> textTypes=new ArrayList<TextType>();

    // ------------------------------------------------ public functions ------------------------------------------------

    public SVGBuilder()
    {
    	for (int n=0;n<96;n++) charMask[n]=false;
    }
    
    // query the boundaries of the drawing, post factum
    public double lowX() {return lowX;}
    public double lowY() {return lowY;}
    public double highX() {return highX;}
    public double highY() {return highY;}
    
    // atomic drawing options
    public void drawLine(double X1,double Y1,double X2,double Y2,int Colour,double Thickness)
    {
    	updateBounds(X1-Thickness,Y1-Thickness); updateBounds(X1+Thickness,Y1+Thickness);
    	updateBounds(X2-Thickness,Y2-Thickness); updateBounds(X2+Thickness,Y2+Thickness);
	
	LineType type=new LineType();
	type.Thickness=Thickness;
	type.Colour=Colour;

	LineAtom atom=new LineAtom();
	atom.AtomClass=ATOM_LINE;
	atom.X1=X1; atom.Y1=Y1; atom.X2=X2; atom.Y2=Y2;
	atom.TypeRef=registerLineType(type);
	atoms.add(atom);
    }
    public void drawRect(double X,double Y,double W,double H,int EdgeCol,double Thickness,int FillCol)
    {
    	updateBounds(X-Thickness,Y-Thickness); updateBounds(X+W+Thickness,Y+H+Thickness);
	
	RectType type=new RectType();
	type.EdgeCol=EdgeCol;
	type.Thickness=Thickness;
	type.FillCol=FillCol;
	
	RectAtom atom=new RectAtom();
	atom.AtomClass=ATOM_RECT;
	atom.X=X; atom.Y=Y; atom.W=W; atom.H=H;
	atom.TypeRef=registerRectType(type);
	atoms.add(atom);
    }
    public void drawOval(double CX,double CY,double RW,double RH,int EdgeCol,double Thickness,int FillCol)
    {
    	updateBounds(CX-RW-Thickness,CY-RH-Thickness); updateBounds(CX+RW+Thickness,CY+RH+Thickness); 
	
	OvalType type=new OvalType();
	type.EdgeCol=EdgeCol;
	type.Thickness=Thickness;
	type.FillCol=FillCol;
	
	OvalAtom atom=new OvalAtom();
	atom.AtomClass=ATOM_OVAL;
	atom.CX=CX; atom.CY=CY; atom.RW=RW; atom.RH=RH;
	atom.TypeRef=registerOvalType(type);
	atoms.add(atom);
    }
    public void drawPoly(double[] X,double[] Y,int EdgeCol,double Thickness,int FillCol,boolean Closed)
    {
    	PathType type=new PathType();
	type.EdgeCol=EdgeCol;
	type.FillCol=FillCol;
	type.Thickness=Thickness;
	type.HardEdge=true;
    
    	PathAtom atom=new PathAtom();
	atom.AtomClass=ATOM_PATH;
	atom.N=X.length;
	atom.X=new double[atom.N];
	atom.Y=new double[atom.N];
	atom.Ctrl=new boolean[atom.N];
	atom.Closed=Closed;
    	for (int n=0;n<atom.N;n++) 
	{
	    updateBounds(X[n]-Thickness,Y[n]-Thickness); 
	    updateBounds(X[n]+Thickness,Y[n]+Thickness); 
	    atom.X[n]=X[n];
	    atom.Y[n]=Y[n];
	    atom.Ctrl[n]=false;
	}
    	atom.TypeRef=registerPathType(type);
    	atoms.add(atom);
    }
    public void drawCurve(double[] X,double[] Y,boolean[] Ctrl,int EdgeCol,double Thickness,int FillCol,boolean Closed)
    {
    	PathType type=new PathType();
	type.EdgeCol=EdgeCol;
	type.FillCol=FillCol;
	type.Thickness=Thickness;
	type.HardEdge=false;
    
    	PathAtom atom=new PathAtom();
	atom.AtomClass=ATOM_PATH;
	atom.N=X.length;
	atom.X=new double[atom.N];
	atom.Y=new double[atom.N];
	atom.Ctrl=new boolean[atom.N];
	atom.Closed=Closed;
    	for (int n=0;n<atom.N;n++) 
	{
	    // (NOTE: if this is a control point, the boundary could be extended too far, but whatever...)
	    updateBounds(X[n]-Thickness,Y[n]-Thickness); 
	    updateBounds(X[n]+Thickness,Y[n]+Thickness); 
	    atom.X[n]=X[n];
	    atom.Y[n]=Y[n];
	    atom.Ctrl[n]=Ctrl[n];
	}
    	atom.TypeRef=registerPathType(type);
    	atoms.add(atom);
    }
    public void drawText(double X,double Y,String Txt,double Sz,int Colour,int Style,int Align)
    {
    	for (int n=0;n<Txt.length();n++) {int i=Txt.charAt(n); if (i>=32 && i<=127) charMask[i-32]=true;}
    
    	double[] metrics=measureText(Txt,Sz,Style);
	if (Align==TXTALIGN_CENTRE) {updateBounds(X-0.5*metrics[0],Y-metrics[1]); updateBounds(X+0.5*metrics[0],Y+metrics[2]);}
	else if (Align==TXTALIGN_LEFT) {updateBounds(X,Y-metrics[1]); updateBounds(X+metrics[0],Y+metrics[2]);}
	else if (Align==TXTALIGN_RIGHT) {updateBounds(X-metrics[0],Y-metrics[1]); updateBounds(X,Y+metrics[2]);}
	
    	// assumes that HTML tags are not wanted
    	Txt=Txt.replace("&","&amp;");
    	Txt=Txt.replace("<","&lt;");
    	Txt=Txt.replace(">","&gt;");

	TextType type=new TextType();
	type.Sz=Sz;
	type.Colour=Colour;
	type.Style=Style;
	type.Align=Align;
	
	TextAtom atom=new TextAtom();
	atom.AtomClass=ATOM_TEXT;
	atom.X=X; atom.Y=Y; atom.Txt=Txt;
	atom.TypeRef=registerTextType(type);
	atoms.add(atom);
    }
    
    // measures a text string, at a given size; the return value has to be considered approximate;
    // the return array is of the form {width,ascent,descent}
    public double[] measureText(String Txt,double Sz,int Style)
    {
	int w=0;
	for (int n=0;n<Txt.length();n++)
	{
	    int i=Txt.charAt(n)-32;
	    if (i>=0 && i<96) w+=SVGFont.HORIZ_ADV_X[i]; else w+=SVGFont.MISSING_HORZ;
	    
	    if (n<Txt.length()-1)
	    {
	    	int j=Txt.charAt(n+1)-32;
		for (int k=0;k<SVGFont.KERN_K.length;k++) 
		    if ((SVGFont.KERN_G1[k]==i && SVGFont.KERN_G2[k]==j) || (SVGFont.KERN_G1[k]==j && SVGFont.KERN_G2[k]==i))
		    	{w+=SVGFont.KERN_K[k]; break;}
	    }
	}
	double[] ret=new double[3];
	ret[0]=Sz*w/SVGFont.UNITS_PER_EM;
	ret[1]=Sz*SVGFont.ASCENT/SVGFont.UNITS_PER_EM;
	ret[2]=Sz*-SVGFont.DESCENT/SVGFont.UNITS_PER_EM;
	return ret;
    }
        
    // builds the SVG content proper; the output is expected to fit in a box of dimensions (0,0,W,H), which is specified in the parameters;
    // the transformations (OX,OY,SW,SH) exist to facilitate fitting the result into this box; OX and OY are in destination units and are
    // added after scaling, while SW and SH are fractional scaling factors, where 1==unchanged; anisotropic scaling is not recommended
    public void build(PrintWriter Out,int W,int H,double OX,double OY,double SW,double SH)
    {
	if( MainPanel.appletMode ){ 
	    g_id = MainApplet.g_id;
	    svg_id = MainApplet.svg_id;
	    Out.println("<svg onclick=\"javascript:SVG_zoom('"+svg_id+"','"+g_id+"','"+W+"','"+H+"');\" id=\""+svg_id+"\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
	    Out.println(" version=\"1\" x=\"0\" y=\"0\" width=\""+W+"\" height=\""+H+"\" viewBox=\"0 0 "+W+" "+H+"\"><g id=\""+g_id+"\" transform=\"matrix(1 0 0 1 0 0)\">");
	}
	else
	{
    	    Out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
	    Out.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 20010904//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
	    Out.println("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
	    Out.println(" version=\"1\" x=\"0\" y=\"0\" width=\""+W+"\" height=\""+H+"\" viewBox=\"0 0 "+W+" "+H+"\"><g transform=\"matrix(1 0 0 1 0 0)\">");
	}
	Out.println();
	// now write out the font definition
	Out.println("<defs><font id=\""+SVGFont.FONT_FAMILY+"\" horiz-adv-x=\""+SVGFont.FONT_ADV+"\">");
	Out.println("<font-face font-family=\""+SVGFont.FONT_FAMILY+"\" units-per-em=\""+SVGFont.UNITS_PER_EM+"\" "+" panose-1=\""+SVGFont.PANOSE_1+"\" ascent=\""+SVGFont.ASCENT+"\" descent=\""+SVGFont.DESCENT+"\" alphabetic=\"0\"/>");
	Out.println("<missing-glyph horiz-adv-x=\""+SVGFont.MISSING_HORZ+"\" d=\""+SVGFont.MISSING_DATA+"\"/>");
    	for (int n=0;n<96;n++) if (charMask[n])
	{
	    Out.print("<glyph unicode=\""+SVGFont.UNICODE[n]+"\" glyph-name=\""+SVGFont.GLYPH_NAME[n]+"\""+" horiz-adv-x=\""+SVGFont.HORIZ_ADV_X[n]+"\"");
	    
	    if (SVGFont.GLYPH_DATA[n].length()>0) Out.print(" d=\""+SVGFont.GLYPH_DATA[n]+"\"");
	    Out.println("/>");
	}
	for (int n=0;n<SVGFont.KERN_K.length;n++) if (charMask[SVGFont.KERN_G1[n]] && charMask[SVGFont.KERN_G2[n]])
	{
	    Out.println("<hkern g1=\""+SVGFont.KERN_G1[n]+"\" g2=\""+SVGFont.KERN_G2[n]+"\" k=\""+SVGFont.KERN_K[n]+"\"/>");
	}

    	Out.println("</font></defs>");
	Out.println();
		
    	// transform everything
	
	for (int n=0;n<atoms.size();n++)
	{
	    Atom a=atoms.get(n);
	    if (a.AtomClass==ATOM_LINE)
	    {
	    	LineAtom la=(LineAtom)a;
		la.X1=OX+((la.X1-lowX)*SW+lowX); la.Y1=OY+((la.Y1-lowY)*SH+lowY); 
		la.X2=OX+((la.X2-lowX)*SW+lowX); la.Y2=OY+((la.Y2-lowY)*SH+lowY);
	    }
	    else if (a.AtomClass==ATOM_RECT)
	    {
	    	RectAtom ra=(RectAtom)a;
		ra.X=OX+((ra.X-lowX)*SW+lowX); ra.Y=OY+((ra.Y-lowY)*SH+lowY); 
		ra.W=ra.W*SW; ra.H=ra.H*SH;
	    }
	    else if (a.AtomClass==ATOM_OVAL)
	    {
	    	OvalAtom oa=(OvalAtom)a;
		oa.CX=OX+((oa.CX-lowX)*SW+lowX); oa.CY=OY+((oa.CY-lowY)*SH+lowY); 
		oa.RW*=SW; oa.RH*=SH;
	    }
	    else if (a.AtomClass==ATOM_PATH)
	    {
	    	PathAtom pa=(PathAtom)a;
		for (int i=0;i<pa.N;i++) {pa.X[i]=OX+((pa.X[i]-lowX)*SW+lowX); pa.Y[i]=OY+((pa.Y[i]-lowY)*SH+lowY);}
	    }
	    else if (a.AtomClass==ATOM_TEXT)
	    {
	    	TextAtom ta=(TextAtom)a;
		// !! ta.X=OX+(ta.X*SW); ta.Y=OY+(ta.Y*SH); 
		ta.X=OX+((ta.X-lowX)*SW+lowX); ta.Y=OY+((ta.Y-lowY)*SH+lowY); 
	    }
	}
	double swsh=0.5*(SW+SH);
	for (int n=0;n<lineTypes.size();n++) lineTypes.get(n).Thickness*=swsh;
	for (int n=0;n<rectTypes.size();n++) rectTypes.get(n).Thickness*=swsh;
	for (int n=0;n<ovalTypes.size();n++) ovalTypes.get(n).Thickness*=swsh;
	for (int n=0;n<pathTypes.size();n++) pathTypes.get(n).Thickness*=swsh;
	for (int n=0;n<textTypes.size();n++) textTypes.get(n).Sz*=swsh;

	// emit everything, in singlets or in groups
    	int p=0;
	while (p<atoms.size())
	{
	    Atom a=atoms.get(p);
	    int sz=1;
	    if (a.AtomClass!=ATOM_PATH) // (these are not rendered in groups)
	    	for (int n=p+1;n<atoms.size();n++,sz++)
	    {
	    	Atom ax=atoms.get(n);
		if (a.TypeRef!=ax.TypeRef || a.AtomClass!=ax.AtomClass) break;
	    }
	    if (a.AtomClass==ATOM_LINE) {if (sz==1) outputLine1(Out,(LineAtom)a); else outputLineN(Out,(LineAtom)a,p,sz);}
	    else if (a.AtomClass==ATOM_RECT) {if (sz==1) outputRect1(Out,(RectAtom)a); else outputRectN(Out,(RectAtom)a,p,sz);}
	    else if (a.AtomClass==ATOM_OVAL) {if (sz==1) outputOval1(Out,(OvalAtom)a); else outputOvalN(Out,(OvalAtom)a,p,sz);}
	    else if (a.AtomClass==ATOM_PATH) outputPath(Out,(PathAtom)a);
	    else if (a.AtomClass==ATOM_TEXT) {if (sz==1) outputText1(Out,(TextAtom)a); else outputTextN(Out,(TextAtom)a,p,sz);}
	    
	    p+=sz;
	}

	Out.println("</g></svg>");
	Out.flush();
    }
    
    // a conveniently overloaded version which computes the size based on the properties of the drawing itself; the returned value provides
    // the calculated width & height
    public int[] build(PrintWriter Out)
    {
	int w=(int)Math.ceil(highX-lowX)+2,h=(int)Math.ceil(highY-lowY)+2;
	double ox=1-lowX,oy=1-lowY;
	build(Out,w,h,ox,oy,1,1);
    	return new int[]{w,h};
    }
    
    // ------------------------------------------------ private functions ------------------------------------------------
    
    private void updateBounds(double X,double Y)
    {
    	if (fresh) {lowX=highX=X; lowY=highY=Y; fresh=false;}
	lowX=Math.min(lowX,X);
	lowY=Math.min(lowY,Y);
	highX=Math.max(highX,X);
	highY=Math.max(highY,Y);
    }
    
    private int registerLineType(LineType T)
    {
    	for (int n=0;n<lineTypes.size();n++)
	{
	    LineType tx=lineTypes.get(n);
	    if (Util.dblEqual(T.Thickness,tx.Thickness) && T.Colour==tx.Colour) return n;
	}
	lineTypes.add(T);
	return lineTypes.size()-1;
    }
    private int registerRectType(RectType T)
    {
    	for (int n=0;n<rectTypes.size();n++)
	{
	    RectType tx=rectTypes.get(n);
	    if (T.EdgeCol==tx.EdgeCol && Util.dblEqual(T.Thickness,tx.Thickness) && T.FillCol==tx.FillCol) return n;
	}
	rectTypes.add(T);
	return rectTypes.size()-1;
    }
    private int registerOvalType(OvalType T)
    {
    	for (int n=0;n<ovalTypes.size();n++)
	{
	    OvalType tx=ovalTypes.get(n);
	    if (T.EdgeCol==tx.EdgeCol && Util.dblEqual(T.Thickness,tx.Thickness) && T.FillCol==tx.FillCol) return n;
	}
	ovalTypes.add(T);
	return ovalTypes.size()-1;
    }
    private int registerPathType(PathType T)
    {
    	for (int n=0;n<pathTypes.size();n++)
	{
	    PathType tx=pathTypes.get(n);
	    if (T.EdgeCol==tx.EdgeCol && T.FillCol==tx.FillCol && 
	    	Util.dblEqual(T.Thickness,tx.Thickness) && T.HardEdge==tx.HardEdge) return n;
	}
	pathTypes.add(T);
	return pathTypes.size()-1;
    }
    private int registerTextType(TextType T)
    {
    	for (int n=0;n<textTypes.size();n++)
	{
	    TextType tx=textTypes.get(n);
	    if (Util.dblEqual(T.Sz,tx.Sz) && T.Colour==tx.Colour && T.Style==tx.Style && T.Align==tx.Align) return n;
	}
	textTypes.add(T);
	return textTypes.size()-1;
    }
    
    private void outputLine1(PrintWriter Out,LineAtom A)
    {
    	LineType type=lineTypes.get(A.TypeRef);
    	Out.println(
	    "<line x1=\""+df.format(A.X1)+"\" y1=\""+df.format(A.Y1)+"\" x2=\""+df.format(A.X2)+"\" y2=\""+df.format(A.Y2)+"\""+" stroke=\""+Util.colourHTML(type.Colour)+"\" stroke-width=\""+type.Thickness+"\"  stroke-linecap=\"round\"/>");
    }
    private void outputLineN(PrintWriter Out,LineAtom A,int N,int Sz)
    {
    	LineType type=lineTypes.get(A.TypeRef);
    	Out.println("<g stroke=\""+Util.colourHTML(type.Colour)+"\" stroke-width=\""+type.Thickness+"\" stroke-linecap=\"round\">");
	for (int n=0;n<Sz;n++)
	{
	    LineAtom a=n==0 ? A : (LineAtom)atoms.get(N+n);
	    Out.println("<line x1=\""+df.format(a.X1)+"\" y1=\""+df.format(a.Y1)+"\" x2=\""+df.format(a.X2)+"\" y2=\""+df.format(a.Y2)+"\"/>");
	}
	Out.println("</g>");
    }
    private void outputRect1(PrintWriter Out,RectAtom A)
    {
    	RectType type=rectTypes.get(A.TypeRef);
	String edge=type.EdgeCol==NOCOLOUR ? "none" : Util.colourHTML(type.EdgeCol);
	String fill=type.FillCol==NOCOLOUR ? "none" : Util.colourHTML(type.FillCol);
    
    	Out.println("<rect x=\""+df.format(A.X)+"\" y=\""+df.format(A.Y)+"\" width=\""+df.format(A.W)+"\" height=\""+df.format(A.H)+"\""+" stroke=\""+edge+"\" stroke-width=\""+type.Thickness+"\"fill=\""+fill+"\"/>");
    }
    private void outputRectN(PrintWriter Out,RectAtom A,int N,int Sz)
    {
    	RectType type=rectTypes.get(A.TypeRef);
	String edge=type.EdgeCol==NOCOLOUR ? "none" : Util.colourHTML(type.EdgeCol);
	String fill=type.FillCol==NOCOLOUR ? "none" : Util.colourHTML(type.FillCol);

	Out.println("<g stroke=\""+edge+"\" stroke-width=\""+type.Thickness+"\" fill=\""+fill+"\">");
	for (int n=0;n<Sz;n++)
	{
	    RectAtom a=n==0 ? A : (RectAtom)atoms.get(N+n);
	    Out.println("<rect x=\""+df.format(a.X)+"\" y=\""+df.format(a.Y)+"\" width=\""+df.format(a.W)+"\" height=\""+df.format(a.H)+"\"/>");
	}
	Out.println("</g>");
    }
    private void outputOval1(PrintWriter Out,OvalAtom A)
    {
    	OvalType type=ovalTypes.get(A.TypeRef);
	String edge=type.EdgeCol==NOCOLOUR ? "none" : Util.colourHTML(type.EdgeCol);
	String fill=type.FillCol==NOCOLOUR ? "none" : Util.colourHTML(type.FillCol);
    
    	Out.println("<ellipse cx=\""+df.format(A.CX)+"\" cy=\""+df.format(A.CY)+"\" rx=\""+df.format(A.RW)+"\" ry=\""+df.format(A.RH)+"\""+" stroke=\""+edge+"\" stroke-width=\""+type.Thickness+"\" fill=\""+fill+"\"/>");
    }
    private void outputOvalN(PrintWriter Out,OvalAtom A,int N,int Sz)
    {
    	OvalType type=ovalTypes.get(A.TypeRef);
	String edge=type.EdgeCol==NOCOLOUR ? "none" : Util.colourHTML(type.EdgeCol);
	String fill=type.FillCol==NOCOLOUR ? "none" : Util.colourHTML(type.FillCol);

	Out.println("<g stroke=\""+edge+"\" stroke-width=\""+type.Thickness+"\" fill=\""+fill+"\">");
	for (int n=0;n<Sz;n++)
	{
	    OvalAtom a=n==0 ? A : (OvalAtom)atoms.get(N+n);
	    Out.println("<ellipse cx=\""+df.format(a.CX)+"\" cy=\""+df.format(a.CY)+"\" rx=\""+df.format(a.RW)+"\" ry=\""+df.format(a.RH)+"\"/>");
	}
	Out.println("</g>");
    }
    private void outputPath(PrintWriter Out,PathAtom A)
    {
    	PathType type=pathTypes.get(A.TypeRef);
	String edge=type.EdgeCol==NOCOLOUR ? "none" : Util.colourHTML(type.EdgeCol);
	String fill=type.FillCol==NOCOLOUR ? "none" : Util.colourHTML(type.FillCol);
	String join=type.HardEdge ? "miter" : "round",cap=type.HardEdge ? "square" : "round";
    	String shape="M "+A.X[0]+" "+A.Y[0];
    	int n=1;
	while (n<A.N)
	{
	    if (!A.Ctrl[n]) {shape+=" L "+A.X[n]+" "+A.Y[n]; n++;}
	    else if (A.Ctrl[n] && n<A.N-1 && !A.Ctrl[n+1])
	    {
	    	shape+=" Q "+A.X[n]+" "+A.Y[n]+" "+A.X[n+1]+" "+A.Y[n+1];
		n+=2;
	    }
	    else if (A.Ctrl[n] && n<A.N-2 && A.Ctrl[n+1] && !A.Ctrl[n+2])
	    {
	    	shape+=" C "+A.X[n]+" "+A.Y[n]+" "+A.X[n+1]+" "+A.Y[n+1]+" "+A.X[n+2]+" "+A.Y[n+2];
		n+=3;
	    }
	    else n++; // (dunno, so skip)
	}
	if (A.Closed) shape+=" Z";
	
    	Out.println("<path d=\""+shape+"\" stroke=\""+edge+"\" fill=\""+fill+"\" stroke-width=\""+type.Thickness+"\""+" stroke-linejoin=\""+join+"\" stroke-linecap=\""+cap+"\"/>");
    }
    private void outputText1(PrintWriter Out,TextAtom A)
    {
    	TextType type=textTypes.get(A.TypeRef);
	String anchor=type.Align==TXTALIGN_LEFT ? "start" : type.Align==TXTALIGN_RIGHT ? "end" : "middle";

    	// !! don't forget style...

    	Out.println(
	    "<text x=\""+df.format(A.X)+"\" y=\""+df.format(A.Y)+"\" font-family=\"Verdana\" font-size=\""+type.Sz+"\""+" text-anchor=\""+anchor+"\" fill=\""+Util.colourHTML(type.Colour)+"\">"+A.Txt+"</text>");
    }
    private void outputTextN(PrintWriter Out,TextAtom A,int N,int Sz)
    {
    	TextType type=textTypes.get(A.TypeRef);
	String anchor=type.Align==TXTALIGN_LEFT ? "start" : type.Align==TXTALIGN_RIGHT ? "end" : "middle";

    	// !! don't forget style...

	Out.println(
	    "<g font-family=\"Verdana\" font-size=\""+type.Sz+"\""+" text-anchor=\""+anchor+"\" fill=\""+Util.colourHTML(type.Colour)+"\">");
	for (int n=0;n<Sz;n++)
	{
	    TextAtom a=n==0 ? A : (TextAtom)atoms.get(N+n);
	    Out.println("<text x=\""+df.format(a.X)+"\" y=\""+df.format(a.Y)+"\">"+a.Txt+"</text>");
	}
	Out.println("</g>");
    }
}
